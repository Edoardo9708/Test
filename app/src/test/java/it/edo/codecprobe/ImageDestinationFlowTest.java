package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import java.io.File;
import java.nio.file.Files;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageDestinationFlowTest {
    @Before public void reset(){var context=RuntimeEnvironment.getApplication();for(String prefs:new String[]{"image-workspace","image-settings"})context.getSharedPreferences(prefs,0).edit().clear().commit();ShadowContentResolver.registerProviderInternal("destinations",new MediaDestinationsTest.Directories());ImageMetadataCache.memory.clear();}
    private static void ready(ImageController images)throws Exception{
        long end=System.currentTimeMillis()+5000;
        while((!images.destinations.ready()||images.state.reading)&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}
        Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(images.destinations.ready());assertFalse(images.state.reading);
    }
    @Test public void photosUseSameFolderFlowAndFreezeTheCorrectPerFileDestinations()throws Exception{
        File one=File.createTempFile("photo-one-",".jpg"),two=File.createTempFile("photo-two-",".jpg");Files.write(one.toPath(),ImageWorkspaceTest.fixture());Files.write(two.toPath(),ImageWorkspaceTest.fixture());
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            ImageController images=controller.get().images;assertEquals(View.VISIBLE,images.screen.destinations.body.getVisibility());assertFalse(images.screen.convert.isEnabled());
            images.state.inputs.add(Uri.fromFile(one).toString());images.state.inputs.add(Uri.fromFile(two).toString());images.setFolder(MediaDestinationsTest.A);MediaDestinationsTest.grant(controller.get(),MediaDestinationsTest.A);images.open();ready(images);
            assertTrue(images.screen.convert.isEnabled());images.state.selected=1;images.open();ready(images);((PhotoDestinationsView)images.screen.destinations).currentScope.performClick();((PhotoDestinationsView)images.screen.destinations).exception.performClick();images.screen.destinations.pickFolder.performClick();assertFalse(images.screen.convert.isEnabled());
            controller.recreate();images=controller.get().images;assertTrue(images.destinations.choosing());
            controller.get().onActivityResult(MediaDestinationsController.FILE,Activity.RESULT_OK,new Intent().setData(Uri.parse(MediaDestinationsTest.B)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));ready(images);
            assertEquals(java.util.Arrays.asList(MediaDestinationsTest.A,MediaDestinationsTest.B),images.destinations.snapshot());
            images.state.reading=true;images.state.info=null;images.render();assertTrue(images.screen.convert.isEnabled());
            images.screen.convert.performClick();Intent command=null;long end=System.currentTimeMillis()+5000;
            while(command==null&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();command=Shadows.shadowOf(controller.get()).getNextStartedService();Thread.sleep(5);}
            assertNotNull(command);assertEquals(ImageConversionService.START,command.getAction());assertNull(command.getStringArrayListExtra("inputs"));assertNull(command.getStringArrayListExtra("folders"));
            String queueId=command.getStringExtra("queueId");assertNotNull(queueId);ImageQueueStore.Queue queued=ImageQueueStore.read(controller.get(),queueId);
            assertEquals(java.util.Arrays.asList(MediaDestinationsTest.A,MediaDestinationsTest.B),queued.folders);assertEquals(2,queued.inputs.size());ImageQueueStore.discard(controller.get(),queueId);assertEquals(65,command.getIntExtra("quality",-1));assertFalse(images.screen.destinations.pickFolder.isEnabled());
        }finally{controller.pause().stop().destroy();one.delete();two.delete();reset();}
    }
}
