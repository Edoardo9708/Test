package it.edo.codecprobe;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.view.WindowManager;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
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
public class ImagePauseTest {
    private Field stateField;private Object previous;
    @Before public void reset()throws Exception{
        var context=RuntimeEnvironment.getApplication();context.getSharedPreferences("image-workspace",0).edit().clear().commit();context.deleteDatabase(ImageProgressJournal.DATABASE);
        stateField=ImageConversionService.class.getDeclaredField("state");stateField.setAccessible(true);previous=stateField.get(null);stateField.set(null,new ImageRunState());
        ShadowContentResolver.registerProviderInternal("destinations",new MediaDestinationsTest.Directories());
    }
    @After public void restore()throws Exception{stateField.set(null,previous);}
    public static final class BlockingPhotos extends MediaDestinationsTest.Directories {
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        volatile int queries;
        @Override public Cursor query(Uri uri,String[] columns,String selection,String[] args,String sort){
            queries++;
            if("one".equals(uri.getLastPathSegment())){entered.countDown();try{if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Photo test timed out");}catch(InterruptedException error){Thread.currentThread().interrupt();throw new IllegalStateException(error);}}
            return super.query(uri,columns,selection,args,sort);
        }
        @Override public android.os.ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{throw new FileNotFoundException("Synthetic unsupported photo");}
    }
    @Test public void pauseFinishesTheCurrentPhotoAndResumeKeepsQueueAndCounters()throws Exception{
        BlockingPhotos photos=new BlockingPhotos();ShadowContentResolver.registerProviderInternal("pausephotos",photos);
        var controller=Robolectric.buildService(ImageConversionService.class).create();String id=queue();
        try{
            start(controller.get(),id);assertTrue(photos.entered.await(5,TimeUnit.SECONDS));long run=ImageConversionService.snapshot().startedAt;
            command(controller.get(),ImageConversionService.PAUSE,run);assertTrue(ImageConversionService.snapshot().pauseRequested);assertFalse(ImageConversionService.snapshot().paused);
            command(controller.get(),ImageConversionService.RESUME,run-1);assertTrue("Old notification must not resume a new run",ImageConversionService.snapshot().pauseRequested);
            photos.release.countDown();await(()->ImageConversionService.snapshot().paused);
            ImageRunState paused=ImageConversionService.snapshot();assertTrue(paused.running);assertEquals(1,paused.completed);assertEquals(1,paused.failed);assertEquals(1,photos.queries);assertTrue(ImageQueueStore.file(controller.get(),id).isFile());assertNull(field(controller.get(),"wake"));
            var notification=ImageConversionService.class.getDeclaredMethod("notification");notification.setAccessible(true);Notification notice=(Notification)notification.invoke(controller.get());assertEquals("Riprendi",notice.actions[0].title.toString());
            await(()->ImageConversionService.snapshot().log.contains("PAUSA IMMAGINI"));File copy=ImageLogStore.snapshot(controller.get(),ImageConversionService.LOG);try{assertTrue(read(copy).contains("PAUSA IMMAGINI"));}finally{copy.delete();}
            command(controller.get(),ImageConversionService.RESUME,run);await(()->!ImageConversionService.snapshot().running);
            ImageRunState ended=ImageConversionService.snapshot();assertEquals(3,ended.completed);assertEquals(3,ended.failed);assertEquals(3,photos.queries);assertFalse(ended.paused);assertFalse(ended.pauseRequested);assertFalse(ImageQueueStore.file(controller.get(),id).exists());assertNull(field(controller.get(),"wake"));
            String log=read(new File(controller.get().getFilesDir(),ImageConversionService.LOG));assertTrue(log.contains("FINE PAUSA IMMAGINI"));assertTrue(log.contains("ripresa della stessa coda"));
        }finally{photos.release.countDown();command(controller.get(),ImageConversionService.CANCEL,ImageConversionService.snapshot().startedAt);await(()->!ImageConversionService.snapshot().running);controller.destroy();ImageQueueStore.discard(RuntimeEnvironment.getApplication(),id);}
    }
    @Test public void cancellingWhilePausedWakesTheWorkerAndDoesNotStartAnotherPhoto()throws Exception{
        BlockingPhotos photos=new BlockingPhotos();ShadowContentResolver.registerProviderInternal("pausephotos",photos);
        var controller=Robolectric.buildService(ImageConversionService.class).create();String id=queue();
        try{
            start(controller.get(),id);assertTrue(photos.entered.await(5,TimeUnit.SECONDS));long run=ImageConversionService.snapshot().startedAt;
            command(controller.get(),ImageConversionService.PAUSE,run);photos.release.countDown();await(()->ImageConversionService.snapshot().paused);
            command(controller.get(),ImageConversionService.CANCEL,run);await(()->!ImageConversionService.snapshot().running);
            ImageRunState ended=ImageConversionService.snapshot();assertEquals(1,ended.completed);assertEquals(1,photos.queries);assertFalse(ended.paused);assertFalse(ended.pauseRequested);assertFalse(ImageQueueStore.file(controller.get(),id).exists());assertNull(field(controller.get(),"wake"));
        }finally{photos.release.countDown();command(controller.get(),ImageConversionService.CANCEL,ImageConversionService.snapshot().startedAt);await(()->!ImageConversionService.snapshot().running);controller.destroy();ImageQueueStore.discard(RuntimeEnvironment.getApplication(),id);}
    }
    @Test public void resumeCanWithdrawAPauseBeforeTheCurrentPhotoFinishes()throws Exception{
        BlockingPhotos photos=new BlockingPhotos();ShadowContentResolver.registerProviderInternal("pausephotos",photos);
        var controller=Robolectric.buildService(ImageConversionService.class).create();String id=queue();
        try{
            start(controller.get(),id);assertTrue(photos.entered.await(5,TimeUnit.SECONDS));long run=ImageConversionService.snapshot().startedAt;
            command(controller.get(),ImageConversionService.PAUSE,run);command(controller.get(),ImageConversionService.RESUME,run);photos.release.countDown();await(()->!ImageConversionService.snapshot().running);
            assertEquals(3,ImageConversionService.snapshot().completed);assertFalse(read(new File(controller.get().getFilesDir(),ImageConversionService.LOG)).contains("PAUSA IMMAGINI"));
        }finally{photos.release.countDown();command(controller.get(),ImageConversionService.CANCEL,ImageConversionService.snapshot().startedAt);await(()->!ImageConversionService.snapshot().running);controller.destroy();ImageQueueStore.discard(RuntimeEnvironment.getApplication(),id);}
    }
    @Test public void destroyingAPausedServiceDoesNotLeaveItsWorkerWaiting()throws Exception{
        BlockingPhotos photos=new BlockingPhotos();ShadowContentResolver.registerProviderInternal("pausephotos",photos);
        var controller=Robolectric.buildService(ImageConversionService.class).create();String id=queue();boolean destroyed=false;
        try{
            start(controller.get(),id);assertTrue(photos.entered.await(5,TimeUnit.SECONDS));command(controller.get(),ImageConversionService.PAUSE,ImageConversionService.snapshot().startedAt);photos.release.countDown();await(()->ImageConversionService.snapshot().paused);
            controller.destroy();destroyed=true;await(()->!ImageConversionService.snapshot().running);assertEquals(1,ImageConversionService.snapshot().completed);assertNull(field(controller.get(),"wake"));
        }finally{photos.release.countDown();if(!destroyed){command(controller.get(),ImageConversionService.CANCEL,ImageConversionService.snapshot().startedAt);await(()->!ImageConversionService.snapshot().running);controller.destroy();}ImageQueueStore.discard(RuntimeEnvironment.getApplication(),id);}
    }
    @Test public void screenAndNotificationCommandsAreBoundToTheActiveQueue()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageLogStore.reset(context);ImageLogStore.append(context,ImageConversionService.LOG,"EVENTO\n\n");
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            ImageController images=controller.get().images;images.screen.sections.focusCard(ImageScreen.HEIC);assertFalse(images.screen.pause.isEnabled());
            ImageRunState run=new ImageRunState();run.running=true;run.total=10;run.startedAt=12345;run.options=ImageOptions.load(context);stateField.set(null,run);images.render();
            assertTrue(images.screen.pause.isEnabled());assertEquals("Pausa",images.screen.pause.getText().toString());images.screen.pause.performClick();Intent pause=Shadows.shadowOf(controller.get()).getNextStartedService();assertEquals(ImageConversionService.PAUSE,pause.getAction());assertEquals(12345,pause.getLongExtra("startedAt",-1));
            run.pauseRequested=true;images.render();assertEquals("Riprendi",images.screen.pause.getText().toString());assertTrue(images.screen.runStatus.getText().toString().contains("Pausa richiesta"));
            run.paused=true;images.render();assertTrue(images.screen.runStatus.getText().toString().contains("in pausa"));assertFalse(images.screen.progress.isIndeterminate());assertTrue(images.screen.exportLog.isEnabled());assertEquals(0,controller.get().getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            controller.recreate();images=controller.get().images;assertEquals("Riprendi",images.screen.pause.getText().toString());images.screen.pause.performClick();Intent resume=Shadows.shadowOf(controller.get()).getNextStartedService();assertEquals(ImageConversionService.RESUME,resume.getAction());assertEquals(12345,resume.getLongExtra("startedAt",-1));
            run.cancelling=true;images.render();assertFalse(images.screen.pause.isEnabled());assertTrue(images.screen.runStatus.getText().toString().contains("Annullamento"));
        }finally{controller.pause().stop().destroy();}
    }
    private static String queue()throws Exception{return ImageQueueStore.write(RuntimeEnvironment.getApplication(),Arrays.asList("content://pausephotos/one","content://pausephotos/two","content://pausephotos/three"),Arrays.asList("Uno","Due","Tre"),Arrays.asList(MediaDestinationsTest.A,MediaDestinationsTest.A,MediaDestinationsTest.A));}
    private static void start(ImageConversionService service,String id){Intent request=new Intent().setAction(ImageConversionService.START).putExtra("queueId",id);ImageOptions.load(service).put(request);service.onStartCommand(request,0,1);}
    private static void command(ImageConversionService service,String action,long run){service.onStartCommand(new Intent().setAction(action).putExtra("startedAt",run),0,2);}
    private static Object field(Object object,String name)throws Exception{Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);}
    private static String read(File file)throws IOException{return new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);}
    private static void await(BooleanSupplier done)throws Exception{long deadline=System.currentTimeMillis()+10000;while(!done.getAsBoolean()&&System.currentTimeMillis()<deadline){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue("Photo worker did not reach the expected state",done.getAsBoolean());}
}
