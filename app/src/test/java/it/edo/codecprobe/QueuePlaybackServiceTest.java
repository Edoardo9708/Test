package it.edo.codecprobe;

import android.app.*;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class QueuePlaybackServiceTest {
    public static class Directory extends ContentProvider {
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        public boolean onCreate(){return true;}
        public Cursor query(Uri u,String[] p,String s,String[] a,String o){
            entered.countDown();
            try{if(!release.await(3,TimeUnit.SECONDS))throw new IllegalStateException("Test did not release provider");}
            catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
            MatrixCursor c=new MatrixCursor(p);
            c.addRow(new Object[]{DocumentsContract.Document.MIME_TYPE_DIR,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE});return c;
        }
        public String getType(Uri u){return null;}public Uri insert(Uri u,ContentValues v){return null;}
        public int update(Uri u,ContentValues v,String s,String[] a){return 0;}public int delete(Uri u,String s,String[] a){return 0;}
    }
    private void await(BooleanSupplier condition)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
        while(!condition.getAsBoolean()&&System.nanoTime()<end)Thread.sleep(10);
        assertTrue("Worker did not reach expected state",condition.getAsBoolean());
    }
    private Intent command(ConversionService service,String action,long session,int file){
        return new Intent(service,ConversionService.class).setAction(action).putExtra(MainActivity.CANCEL_SESSION,session).putExtra("fileNumber",file);
    }
    @Test public void pauseSkipResumeKeepsNotificationAndExcludesSkippedFileFromRetry()throws Exception{
        Directory directory=new Directory();ShadowContentResolver.registerProviderInternal("dirs",directory);
        var controller=Robolectric.buildService(ConversionService.class).create();ConversionService service=controller.get();
        ConversionService.State before=ConversionService.snapshot();Intent beforeRetry=ConversionService.retryRequest(service);
        try{
            Intent task=BatchRetry.select(BatchRetryTest.queue(service),java.util.List.of(0,1));
            service.onStartCommand(task,0,1);assertTrue(directory.entered.await(3,TimeUnit.SECONDS));
            long session=ConversionService.snapshot().startedAt;
            service.onStartCommand(command(service,ConversionService.PAUSE,session+1,1),0,2);assertFalse(ConversionService.snapshot().pauseRequested);
            service.onStartCommand(command(service,ConversionService.PAUSE,session,1),0,3);directory.release.countDown();
            await(()->ConversionService.snapshot().paused);
            Field wake=ConversionService.class.getDeclaredField("wake");wake.setAccessible(true);assertFalse(((PowerManager.WakeLock)wake.get(service)).isHeld());
            Method make=ConversionService.class.getDeclaredMethod("notification",ConversionService.State.class);make.setAccessible(true);
            Notification notification=(Notification)make.invoke(service,ConversionService.snapshot());
            assertTrue((notification.flags&Notification.FLAG_ONGOING_EVENT)!=0);assertEquals("Conversione in pausa",notification.extras.getCharSequence(Notification.EXTRA_TITLE));
            assertEquals("Riprendi",notification.actions[1].title);assertTrue(notification.actions[1].actionIntent.isService());
            Intent resume=Shadows.shadowOf(notification.actions[1].actionIntent).getSavedIntent();assertEquals(ConversionService.RESUME,resume.getAction());
            service.onStartCommand(command(service,ConversionService.SKIP,session,2),0,4);assertFalse(ConversionService.snapshot().skipping);
            service.onStartCommand(command(service,ConversionService.SKIP,session,1),0,5);
            await(()->{var state=ConversionService.snapshot();return state.batchIndex==2&&state.paused;});
            assertEquals(1,ConversionService.snapshot().skipped);assertEquals(0,ConversionService.snapshot().failed);assertTrue(ConversionService.snapshot().failures.isEmpty());assertNull(BatchRetry.load(service));
            service.onStartCommand(command(service,ConversionService.SKIP,session,1),0,6);assertFalse(ConversionService.snapshot().skipping);
            service.onStartCommand(resume,0,7);
            Field worker=ConversionService.class.getDeclaredField("worker");worker.setAccessible(true);((ExecutorService)worker.get(service)).submit(()->{}).get(5,TimeUnit.SECONDS);
            Shadows.shadowOf(Looper.getMainLooper()).idle();ConversionService.State done=ConversionService.snapshot();
            assertFalse(done.running);assertEquals(1,done.skipped);assertEquals(1,done.failed);assertTrue(done.sizeReport.contains("Saltato dall’utente"));
            assertEquals(java.util.List.of("content://input/1"),BatchRetry.load(service).getStringArrayListExtra("inputs"));
            assertFalse(((PowerManager.WakeLock)wake.get(service)).isHeld());
        }finally{
            directory.release.countDown();
            Field workerField=ConversionService.class.getDeclaredField("worker");workerField.setAccessible(true);
            ExecutorService executor=(ExecutorService)workerField.get(service);controller.destroy();
            assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));Shadows.shadowOf(Looper.getMainLooper()).idle();
            Field stored=ConversionService.class.getDeclaredField("state");stored.setAccessible(true);Object actual=stored.get(null);
            for(Field field:ConversionService.State.class.getDeclaredFields()){field.setAccessible(true);field.set(actual,field.get(before));}
            ConversionService.rememberFailures(service,beforeRetry);
        }
    }
}
