package it.edo.codecprobe;

import android.content.*;
import android.net.Uri;
import android.os.Looper;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageMetadataCheckTest {
    private Field stateField;private Object previous;
    @Before public void reset()throws Exception{
        var c=RuntimeEnvironment.getApplication();c.getSharedPreferences("image-workspace",0).edit().clear().commit();c.deleteDatabase(ImageProgressJournal.DATABASE);
        stateField=ImageConversionService.class.getDeclaredField("state");stateField.setAccessible(true);previous=stateField.get(null);stateField.set(null,new ImageRunState());
    }
    @After public void restore()throws Exception{stateField.set(null,previous);}
    @Test public void inspectionQueueNeedsNoDestinationAndKeepsLargeSelection()throws Exception{
        var c=RuntimeEnvironment.getApplication();List<String> inputs=Collections.nCopies(8500,"content://photos/source");
        String id=ImageQueueStore.writeInspection(c,inputs,Collections.emptyList(),true);
        try{var q=ImageQueueStore.read(c,id);assertTrue(q.metadataOnly);assertEquals(8500,q.inputs.size());assertEquals("",q.folders.get(8499));}
        finally{ImageQueueStore.discard(c,id);}
        try{ImageQueueStore.write(c,Arrays.asList("content://photos/source"),Collections.emptyList(),Arrays.asList(""));fail("Conversion still requires a destination");}catch(IOException expected){}
    }
    @Test public void compatibilityButtonStartsWithoutFolderAndUsesDiskQueue()throws Exception{
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();String id=null;
        try{
            var images=controller.get().images;images.state.inputs.clear();images.state.names.clear();images.state.inputs.add("content://missing/photo");images.state.names.add("photo.jpg");images.state.options=images.state.options.withFolder(null);images.render();
            assertFalse(images.screen.convert.isEnabled());assertTrue(images.screen.checkMetadata.isEnabled());images.screen.checkMetadata.performClick();
            Intent[] request={null};await(()->{if(request[0]==null)request[0]=Shadows.shadowOf(controller.get()).getNextStartedService();return request[0]!=null;});
            assertEquals(ImageConversionService.START,request[0].getAction());assertTrue(request[0].getBooleanExtra("metadataOnly",false));id=request[0].getStringExtra("queueId");assertTrue(ImageQueueStore.read(controller.get(),id).metadataOnly);
        }finally{ImageQueueStore.discard(controller.get(),id);controller.pause().stop().destroy();}
    }
    @Test public void checkPausesResumesAndArchivesProblemsWithoutTouchingConversionJournal()throws Exception{
        var c=RuntimeEnvironment.getApplication();ImagePauseTest.BlockingPhotos photos=new ImagePauseTest.BlockingPhotos();ShadowContentResolver.registerProviderInternal("pausephotos",photos);
        String id=ImageQueueStore.writeInspection(c,Arrays.asList("content://pausephotos/one","content://pausephotos/two"),Arrays.asList("one.jpg","two.jpg"),false);
        var controller=Robolectric.buildService(ImageConversionService.class).create();
        try{
            Intent start=new Intent().setAction(ImageConversionService.START).putExtra("queueId",id).putExtra("metadataOnly",true);ImageOptions.load(c).withFolder(null).put(start);controller.get().onStartCommand(start,0,1);
            assertTrue(photos.entered.await(5,TimeUnit.SECONDS));long token=ImageConversionService.snapshot().startedAt;
            controller.get().onStartCommand(new Intent().setAction(ImageConversionService.PAUSE).putExtra("startedAt",token),0,2);photos.release.countDown();await(()->ImageConversionService.snapshot().paused);
            assertTrue(ImageConversionService.snapshot().metadataOnly);assertEquals(1,ImageConversionService.snapshot().completed);assertFalse(c.getDatabasePath(ImageProgressJournal.DATABASE).exists());
            File copy=ImageLogStore.snapshot(c,ImageConversionService.LOG);try{assertTrue(read(copy).contains("INIZIO VERIFICA METADATI"));}finally{copy.delete();}
            controller.get().onStartCommand(new Intent().setAction(ImageConversionService.RESUME).putExtra("startedAt",token),0,3);await(()->!ImageConversionService.snapshot().running);
            var run=ImageConversionService.snapshot();assertEquals(2,run.failed);assertEquals(0,run.saved);assertEquals(0,run.checked);assertFalse(c.getDatabasePath(ImageProgressJournal.DATABASE).exists());assertTrue(read(new File(c.getFilesDir(),ImageFailureStore.FILE)).contains("metadata-check"));assertFalse(ImageQueueStore.file(c,id).exists());
            File[] tmp=c.getCacheDir().listFiles((d,n)->n.startsWith("image-source-")||n.startsWith("image-heic-"));assertNotNull(tmp);assertEquals(0,tmp.length);
        }finally{photos.release.countDown();controller.get().onStartCommand(new Intent().setAction(ImageConversionService.CANCEL).putExtra("startedAt",ImageConversionService.snapshot().startedAt),0,4);await(()->!ImageConversionService.snapshot().running);controller.destroy();ImageQueueStore.discard(c,id);}
    }
    @Test public void checkedCountersStaySeparateAndSurviveBoundedResultWindow()throws Exception{
        ImageRunState state=new ImageRunState();state.metadataOnly=true;state.current=8500;state.total=8500;
        for(int n=0;n<8500;n++)state.add(new ImageRunState.Result("photo",100,0,null,"Metadati compatibili",ImageRunState.Outcome.CHECKED,"Pixel non verificati"));
        ImageRunState copy=new ImageRunState(state);assertTrue(copy.metadataOnly);assertEquals(8500,copy.checked);assertEquals(0,copy.saved);assertEquals(0,copy.cancelledCount);assertEquals(0,copy.inputSavedBytes);assertEquals(100,copy.results.size());
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{stateField.set(null,copy);controller.get().images.render();assertTrue(controller.get().images.screen.dashboard.summary.getText().toString().contains("Compatibili: 8500"));assertFalse(controller.get().images.screen.dashboard.summary.getText().toString().contains("Riuscite:"));}finally{controller.pause().stop().destroy();}
    }
    private static String read(File file)throws IOException{return new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);}
    private static void await(BooleanSupplier done)throws Exception{long deadline=System.currentTimeMillis()+10000;while(!done.getAsBoolean()&&System.currentTimeMillis()<deadline){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue("Metadata check did not reach the expected state",done.getAsBoolean());}
}
