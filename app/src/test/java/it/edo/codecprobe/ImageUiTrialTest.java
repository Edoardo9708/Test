package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageUiTrialTest {
    final ArrayList<File> files=new ArrayList<>();
    @Before public void reset(){
        var c=RuntimeEnvironment.getApplication();
        for(String pref:new String[]{"image-workspace","image-settings"})c.getSharedPreferences(pref,0).edit().clear().commit();
        ImageMetadataCache.memory.clear();
    }
    @After public void cleanup(){for(File file:files)file.delete();reset();}
    private String photo(String name)throws Exception{
        File file=File.createTempFile(name,".jpg");Files.write(file.toPath(),ImageWorkspaceTest.fixture());files.add(file);return Uri.fromFile(file).toString();
    }
    static void settle(ImageController images)throws Exception{
        long end=System.currentTimeMillis()+10000;
        while((images.state.reading||images.state.importing)&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}
        Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(images.state.reading);assertFalse(images.state.importing);
    }
    @Test public void navigationRetainsDraftAndQualityAndHeadersCannotCollapse()throws Exception{
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;assertNull(images.screen.sections.current());
            images.screen.sections.newWork.performClick();assertEquals(ImageScreen.FILES,images.screen.sections.current());
            String input=photo("draft");images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.parse(input)));settle(images);
            images.screen.layout.forward.performClick();images.screen.quality.quality.setProgress(30);
            int quality=images.screen.quality.value();images.screen.layout.back.performClick();
            assertEquals(ImageScreen.FILES,images.screen.sections.current());images.screen.layout.back.performClick();
            assertNull(images.screen.sections.current());assertEquals(Collections.singletonList(input),images.state.inputs);
            images.screen.sections.newWork.performClick();images.screen.layout.forward.performClick();
            assertEquals(quality,images.screen.quality.value());images.screen.sections.headers.get(ImageScreen.COMPRESSION).performClick();
            assertEquals(View.VISIBLE,images.screen.sections.bodies.get(ImageScreen.COMPRESSION).getVisibility());
            activity.recreate();assertEquals(ImageScreen.COMPRESSION,activity.get().images.screen.sections.current());
        }finally{activity.pause().stop().destroy();}
    }
    @Test public void longPressSelectionUsesGroupAndRequiresConfirmationBeforeRemoval()throws Exception{
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;for(int n=0;n<3;n++)images.state.inputs.add(photo("selection"+n));images.state.clamp();images.open();settle(images);
            images.screen.selectedItem.chips.get(0).performLongClick();images.screen.selectedItem.chips.get(2).performClick();
            assertEquals(Arrays.asList(0,2),images.selectionIndices());assertEquals(2,images.selectedPhotos.size());
            assertTrue(images.screen.name.getText().toString().contains("2 foto"));images.screen.metadata.performClick();
            assertEquals(View.VISIBLE,images.screen.selectedItem.detailsBody.getVisibility());
            images.screen.remove.performClick();assertEquals(3,images.state.inputs.size());
            ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_NEGATIVE).performClick();assertEquals(3,images.state.inputs.size());
            images.screen.remove.performClick();ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();settle(images);
            assertEquals(1,images.state.inputs.size());for(File file:files)assertTrue(file.exists());
            images.screen.clear.performClick();assertEquals(1,images.state.inputs.size());
            ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(images.state.inputs.isEmpty());
        }finally{activity.pause().stop().destroy();}
    }
    @Test public void folderImportIsAdditiveAndKeepsExistingDestinationEvenForDuplicates()throws Exception{
        ShadowContentResolver.registerProviderInternal("folderphotos",new ImageFolderImportTest.Folder());
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;String old=photo("existing");images.state.inputs.add(old);images.state.inputs.add("content://folderphotos/tree/camera/document/photo0");images.state.clamp();
            images.state.destinations.sync(images.state.inputs,null);images.state.destinations.plan.setFolder(0,"content://saved/tree/custom");
            images.importFolder(Uri.parse(ImageFolderImportTest.TREE));settle(images);
            assertTrue(images.state.inputs.contains(old));assertEquals(new HashSet<>(images.state.inputs).size(),images.state.inputs.size());
            assertEquals("content://saved/tree/custom",images.state.destinations.plan.files.get(0).folder);
            int before=images.state.inputs.size();images.importFolder(Uri.parse(ImageFolderImportTest.TREE));settle(images);assertEquals(before,images.state.inputs.size());
        }finally{activity.pause().stop().destroy();}
    }
    @Test public void folderPickerFreezesSelectedGroupAcrossRecreation()throws Exception{
        ShadowContentResolver.registerProviderInternal("destinations",new MediaDestinationsTest.Directories());
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;for(int n=0;n<3;n++)images.state.inputs.add(photo("destination"+n));images.state.clamp();images.setFolder(MediaDestinationsTest.A);images.open();settle(images);
            images.toggleSelection(0);images.toggleSelection(2);images.screen.destinations.pickFolder.performClick();activity.recreate();images=activity.get().images;
            images.selection.result(MediaDestinationsController.FILE,Activity.RESULT_OK,new Intent().setData(Uri.parse(MediaDestinationsTest.B)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            assertEquals(MediaDestinationsTest.B,images.state.destinations.plan.files.get(0).folder);
            assertEquals(MediaDestinationsTest.A,images.state.destinations.plan.files.get(1).folder);
            assertEquals(MediaDestinationsTest.B,images.state.destinations.plan.files.get(2).folder);
        }finally{activity.pause().stop().destroy();}
    }
    @Test public void carouselTilesHaveFixedSizeAndFooterLeavesScrollableSpace(){
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;images.state.inputs.add("content://preview/one");images.state.clamp();images.render();images.screen.sections.newWork.performClick();
            var layout=images.screen.layout;int w=images.screen.ui.dp(360),h=images.screen.ui.dp(800);
            layout.root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));layout.root.layout(0,0,w,h);
            assertEquals(images.screen.ui.dp(64),images.screen.selectedItem.chips.get(0).getLayoutParams().width);
            assertTrue(layout.scroll.getPaddingBottom()>=layout.footer.getMeasuredHeight());assertTrue(layout.forward.isShown());assertTrue(layout.back.isShown());
        }finally{activity.pause().stop().destroy();}
    }
}
