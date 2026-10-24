package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Looper;
import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageCarouselCacheTest {
    @Before public void reset() {
        ImageMetadataCache.memory.clear();
        RuntimeEnvironment.getApplication().getSharedPreferences("image-workspace",0).edit().clear().commit();
    }
    private static void settle(ImageController images) throws Exception {
        long deadline=System.currentTimeMillis()+5000;
        while(images.state.reading){Shadows.shadowOf(Looper.getMainLooper()).idle();if(System.currentTimeMillis()>deadline)fail("Metadata read timed out");Thread.sleep(5);}
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
    private static void select(ImageController images,int index) throws Exception {
        images.screen.selectedItem.chips.get(index).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));settle(images);
        assertEquals(index,images.state.selected);
        assertTrue(images.screen.selectedItem.chips.get(index).isSelected());
    }
    @Test public void carouselReusesPhotoMetadataAcrossNavigationAndActivityRecreationUntilReload() throws Exception {
        File first=File.createTempFile("first-photo-",".jpg"),second=File.createTempFile("second-photo-",".jpg");
        Files.write(first.toPath(),ImageWorkspaceTest.fixture());Files.write(second.toPath(),ImageWorkspaceTest.fixture());
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            Intent picked=new Intent();ClipData clips=ClipData.newRawUri("photos",Uri.fromFile(first));clips.addItem(new ClipData.Item(Uri.fromFile(second)));picked.setClipData(clips);
            ImageController images=controller.get().images;images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,picked);settle(images);
            JpegInventory.Info original=images.state.info;assertNotNull(original);
            images.screen.metadata.performClick();assertEquals(android.view.View.VISIBLE,images.screen.selectedItem.detailsBody.getVisibility());
            images.screen.selectedItem.chips.get(1).performClick();images.screen.selectedItem.chips.get(0).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertEquals(0,images.state.selected);
            Files.write(first.toPath(),new byte[]{1,2,3});
            select(images,1);assertNotNull(images.state.info);
            select(images,0);assertSame(original,images.state.info);
            controller.recreate();images=controller.get().images;settle(images);
            assertSame(original,images.state.info);assertEquals(2,images.screen.selectedItem.chips.size());
            images.screen.reload.performClick();settle(images);
            assertNull(images.state.info);assertTrue(images.state.notice.contains("Non è un JPEG"));
            images.screen.clear.performClick();org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(0,ImageMetadataCache.memory.size());
            assertEquals(0,images.screen.selectedItem.chips.size());assertTrue(first.exists());
        }finally{controller.pause().stop().destroy();first.delete();second.delete();reset();}
    }
}
