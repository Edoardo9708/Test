package it.edo.codecprobe;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageResponsivenessTest {
    @Before public void reset(){RuntimeEnvironment.getApplication().getSharedPreferences("image-workspace",0).edit().clear().commit();}
    @Test public void unchangedLargeCarouselDoesNotReadHiddenNamesOrReplaceButtonBackgrounds(){
        var activity=Robolectric.buildActivity(Activity.class).setup();try{
            SelectedItemView view=new SelectedItemView(new AppUi(activity.get()),new LinearLayout(activity.get()),"Foto","Nessuna","Dettagli");
            List<String> names=new AbstractList<String>(){
                public int size(){return 8500;}
                public String get(int index){assertTrue("Hidden names must not be traversed by the carousel",index<100);return "Foto "+index;}
            };
            view.render(names,Collections.emptyList(),0,true,n->{},100);
            Button chip=view.chips.get(0);Drawable background=chip.getBackground();
            view.render(names,Collections.emptyList(),0,true,n->{},100);
            assertSame(chip,view.chips.get(0));assertSame(background,chip.getBackground());
            assertEquals(101,view.navigator.getChildCount());assertTrue(view.number.getText().toString().contains("8500"));
            Drawable untouched=view.chips.get(2).getBackground();view.update(1,true,false);
            assertSame(untouched,view.chips.get(2).getBackground());assertTrue(view.chips.get(1).isSelected());assertFalse(chip.isSelected());view.close();
        }finally{activity.pause().stop().destroy();}
    }
    @Test public void progressTicksLeaveTheSelectionUntouchedAndReuseReportRows()throws Exception{
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();
        var field=ImageConversionService.class.getDeclaredField("state");field.setAccessible(true);Object old=field.get(null);
        try{
            ImageController images=controller.get().images;
            for(int i=0;i<8500;i++){images.state.inputs.add("content://photos/"+i);images.state.names.add("Foto "+i);}
            ImageRunState run=new ImageRunState();run.running=true;run.total=8500;run.current=200;run.message="Codifica";field.set(null,run);images.render();
            images.screen.sections.focusCard(ImageScreen.HEIC);ImageResultList.Item retained=images.report.results.item(198);PhotoUiState presentation=images.compose.getState();
            images.state.notice="untouched workspace";run.message="Verifica";images.refresh();
            assertEquals("untouched workspace",images.state.notice);assertSame(presentation,images.compose.getState());
            assertSame(retained,images.report.results.item(198));assertTrue(images.report.results.item(199).text.contains("Verifica"));
            run.current=201;images.refresh();assertSame(retained,images.report.results.item(198));assertTrue(images.report.results.item(200).text.contains("Foto 201"));
        }finally{field.set(null,old);controller.pause().stop().destroy();}
    }
    @Test public void cachedPersistenceKeepsAllFilesAndDetectsIndividualDestinationEdits(){
        var context=RuntimeEnvironment.getApplication();ImageState state=new ImageState();state.options=ImageOptions.load(context);state.bulk=true;
        for(int i=0;i<8500;i++){state.inputs.add("content://photos/"+i);state.names.add("Foto "+i);}
        ImageDraftStore.save(context,state);ImageDraftStore.Selection previous=state.savedSelection;
        state.selected=123;state.options=new ImageOptions(50,"slow","standard",null,true,false,state.options.revision);ImageDraftStore.save(context,state);
        assertSame(previous,state.savedSelection);ImageState restored=ImageDraftStore.load(context);assertEquals(8500,restored.inputs.size());assertEquals(123,restored.selected);assertEquals(50,restored.options.quality);
        state.destinations.plan.setFolder(5000,"content://folders/output");ImageDraftStore.save(context,state);assertNotSame(previous,state.savedSelection);
        restored=ImageDraftStore.load(context);assertEquals("content://folders/output",restored.destinations.plan.files.get(5000).folder);assertEquals(8500,restored.names.size());
        previous=state.savedSelection;state.names.set(8499,"Renamed.jpg");ImageDraftStore.save(context,state);assertNotSame(previous,state.savedSelection);assertEquals("Renamed.jpg",ImageDraftStore.load(context).names.get(8499));
    }
    @Test public void manualSelectionReturnsWhilePermissionWorkIsPending()throws Exception{
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=controller.get().images;
            android.content.ClipData clips=android.content.ClipData.newRawUri("photos",android.net.Uri.parse("content://photos/0"));
            for(int i=1;i<125;i++)clips.addItem(new android.content.ClipData.Item(android.net.Uri.parse("content://photos/"+i)));
            android.content.Intent picked=new android.content.Intent();picked.setClipData(clips);
            images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,picked);
            assertTrue(images.state.importing);assertEquals(125,images.state.inputs.size());assertFalse(images.screen.convert.isEnabled());
            java.util.concurrent.atomic.AtomicBoolean responsive=new java.util.concurrent.atomic.AtomicBoolean();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->responsive.set(true));
            long deadline=System.currentTimeMillis()+5000;
            while(images.state.importing&&System.currentTimeMillis()<deadline){Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();Thread.sleep(5);}
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(responsive.get());assertFalse(images.state.importing);assertEquals(125,images.state.inputs.size());
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void reusedResultButtonsTargetTheNewFileAfterTheWindowMoves(){
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=controller.get().images;ImageRunState run=new ImageRunState();run.total=5000;run.current=1000;run.running=true;
            for(int i=0;i<1000;i++)run.add(new ImageRunState.Result("Foto "+i,300,200,"content://saved/"+i,"OK",ImageRunState.Outcome.SAVED,""));
            images.report.render(run);View row=ImageVirtualReportTest.row(images,48,null);run.current=1001;images.report.render(run);assertSame(row,ImageVirtualReportTest.row(images,48,row));
            Button button=find(row,"Apri risultato:");assertNotNull(button);button.performClick();assertEquals("content://saved/999",Shadows.shadowOf(controller.get()).getNextStartedActivity().getDataString());
        }finally{controller.pause().stop().destroy();}
    }
    private static String text(View view){StringBuilder out=new StringBuilder();if(view instanceof TextView)out.append(((TextView)view).getText());if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)out.append(text(((ViewGroup)view).getChildAt(i)));return out.toString();}
    private static Button find(View view,String prefix){if(view instanceof Button&&view.getVisibility()==View.VISIBLE&&view.getContentDescription()!=null&&view.getContentDescription().toString().startsWith(prefix))return (Button)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){Button b=find(((ViewGroup)view).getChildAt(i),prefix);if(b!=null)return b;}return null;}
}
