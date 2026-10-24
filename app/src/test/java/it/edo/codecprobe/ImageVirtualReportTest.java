package it.edo.codecprobe;

import android.app.Activity;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageVirtualReportTest {
    static View row(ImageController images,int position,View recycled){ListView list=images.report.results.list;return list.getAdapter().getView(position,recycled,list);}
    private static void layout(ImageResultList view){view.list.measure(View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY));view.list.layout(0,0,640,640);}
    @Test public void oneHundredResultsUseOnlyAVisibleSubsetOfViewsAndRecycleTheirActions(){
        var controller=Robolectric.buildActivity(Activity.class).setup();try{
            AppUi ui=new AppUi(controller.get());LinearLayout root=new LinearLayout(controller.get());root.setOrientation(LinearLayout.VERTICAL);controller.get().setContentView(root);
            ImageResultList view=new ImageResultList(ui,root);ArrayList<ImageResultList.Item> items=new ArrayList<>();int[] clicked={-1};
            for(int i=0;i<100;i++){int id=i;items.add(new ImageResultList.Item(i,"Photo "+i,"","Photo "+i+"\nSaved",null,false,()->clicked[0]=id,null,false));}
            view.submit(items,49);layout(view);Shadows.shadowOf(Looper.getMainLooper()).idle();layout(view);
            assertEquals(100,view.list.getAdapter().getCount());assertTrue(view.list.getChildCount()>0);assertTrue("Off-screen rows must not be attached",view.list.getChildCount()<20);
            View recycled=view.list.getAdapter().getView(0,null,view.list);View rebound=view.list.getAdapter().getView(99,recycled,view.list);assertSame(recycled,rebound);
            ((Button)((LinearLayout)rebound).getChildAt(1)).performClick();assertEquals(99,clicked[0]);
            ImageResultList.Item retained=items.get(50);ArrayList<ImageResultList.Item> shifted=new ArrayList<>(items.subList(1,100));shifted.add(new ImageResultList.Item(100,"Last","","Last",null,false,null,null,false));
            view.submit(shifted,50);layout(view);assertSame(retained,view.item(50));assertTrue(view.list.getChildCount()<20);assertEquals(100,view.size());
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void hiddenProgressAndLogAreNotRewrittenByTheTimer()throws Exception{
        var c=RuntimeEnvironment.getApplication();c.getSharedPreferences("image-workspace",0).edit().clear().commit();var field=ImageConversionService.class.getDeclaredField("state");field.setAccessible(true);Object previous=field.get(null);
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            var images=controller.get().images;ImageRunState run=new ImageRunState();run.running=true;run.total=8500;run.current=100;run.log="first";field.set(null,run);images.render();
            images.screen.sections.focusCard(ImageScreen.FILES);String original=images.screen.dashboard.summary.getText().toString();images.screen.log.setText("hidden");run.current=500;run.log="new log";images.refresh();
            assertEquals(original,images.screen.dashboard.summary.getText().toString());assertEquals("hidden",images.screen.log.getText().toString());
            images.screen.sections.focusCard(ImageScreen.HEIC);images.refresh();assertTrue(images.screen.dashboard.summary.getText().toString().contains("500 / 8500"));
            images.screen.sections.focusCard(ImageScreen.DETAILS);images.screen.toggleLog.performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));images.refresh();assertEquals("new log",images.screen.log.getText().toString());
        }finally{field.set(null,previous);controller.pause().stop().destroy();}
    }
    @Test public void visibleLogPreviewIsBoundedAndContainsTheNewestEvent(){
        StringBuilder log=new StringBuilder();for(int i=0;i<100;i++)log.append("event ").append(i).append('\n');String preview=ImageLogPreview.text(log.toString());assertTrue(preview.contains("event 99"));assertFalse(preview.contains("event 0\n"));assertTrue(preview.length()<=4000);
        assertEquals(4000,ImageLogPreview.text("x".repeat(100000)).length());assertEquals("",ImageLogPreview.text(""));
    }
}
