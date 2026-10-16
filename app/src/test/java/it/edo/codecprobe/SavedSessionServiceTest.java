package it.edo.codecprobe;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.Looper;
import android.provider.DocumentsContract;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class SavedSessionServiceTest {
    public static class Directory extends ContentProvider {
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        final List<String> requested=Collections.synchronizedList(new ArrayList<>());
        volatile boolean fail;
        public boolean onCreate(){return true;}
        public Cursor query(Uri uri,String[] columns,String selection,String[] args,String sort){
            requested.add(uri.toString());entered.countDown();
            if(fail)return null;
            try{if(!release.await(4,TimeUnit.SECONDS))throw new IllegalStateException("Provider not released");}
            catch(InterruptedException error){Thread.currentThread().interrupt();throw new IllegalStateException(error);}
            MatrixCursor rows=new MatrixCursor(columns);rows.addRow(new Object[]{DocumentsContract.Document.MIME_TYPE_DIR,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE});return rows;
        }
        public String getType(Uri uri){return null;}public Uri insert(Uri uri,ContentValues values){return null;}
        public int update(Uri uri,ContentValues values,String where,String[] args){return 0;}public int delete(Uri uri,String where,String[] args){return 0;}
    }
    private static void install(ConversionService.State state)throws Exception{Field field=ConversionService.class.getDeclaredField("state");field.setAccessible(true);field.set(null,state);}
    private static void drain(ConversionService service)throws Exception{
        Field field=ConversionService.class.getDeclaredField("worker");field.setAccessible(true);
        ((ExecutorService)field.get(service)).submit(()->{}).get(6,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Test public void durableStopAndFreshProcessRecoverySkipCompletedFileAndKeepWholeReport()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Intent request=BatchRetry.select(BatchRetryTest.queue(context),List.of(0,1));
        request.putStringArrayListExtra("outputNames",new ArrayList<>(List.of("custom-zero.mp4","custom-one.mp4"))).putExtra("omitted",new boolean[2]);
        ConversionService.State original=SavedSessionTest.sample(request),before=ConversionService.snapshot();
        SavedSession.save(context,request,original);install(new ConversionService.State());
        Directory directory=new Directory();ShadowContentResolver.registerProviderInternal("dirs",directory);
        var controller=Robolectric.buildService(ConversionService.class).create();ConversionService service=controller.get();
        try{
            assertNull(ConversionService.restoreSession(context));assertTrue(ConversionService.snapshot().recoverable);assertEquals(42,ConversionService.snapshot().filePercent);
            service.onStartCommand(SavedSession.load(context).resumeRequest(),0,1);assertTrue(directory.entered.await(4,TimeUnit.SECONDS));
            long session=ConversionService.snapshot().startedAt;
            service.onStartCommand(new Intent(context,ConversionService.class).setAction(ConversionService.SAVE_STOP).putExtra(MainActivity.CANCEL_SESSION,session+1),0,2);assertFalse(ConversionService.snapshot().savingSession);
            service.onStartCommand(new Intent(context,ConversionService.class).setAction(ConversionService.SAVE_STOP).putExtra(MainActivity.CANCEL_SESSION,session),0,3);
            directory.release.countDown();drain(service);
            SavedSession stopped=SavedSession.load(context);assertTrue(stopped.state.recoverable);assertFalse(ConversionService.snapshot().running);
            assertEquals(1,stopped.state.succeeded);assertEquals(0,stopped.state.failed);assertEquals("content://saved/first",stopped.state.outputUris.get(0));
            assertTrue(directory.requested.stream().noneMatch(uri->uri.contains("zero")));
            controller.destroy();install(new ConversionService.State());
            assertNull(ConversionService.restoreSession(context));assertEquals(stopped.state.sizeReport,ConversionService.snapshot().sizeReport);
            directory.fail=true;
            var next=Robolectric.buildService(ConversionService.class).create();
            try{
                next.get().onStartCommand(SavedSession.load(context).resumeRequest(),0,1);drain(next.get());
                SavedSession finished=SavedSession.load(context);assertFalse(finished.state.recoverable);assertEquals(1,finished.state.succeeded);assertEquals(1,finished.state.failed);
                assertTrue(finished.state.sizeReport.contains("1,00 kB → 0,50 kB"));assertEquals(2,finished.state.failures.get(0).number);
                String log=new String(java.nio.file.Files.readAllBytes(ConversionService.batchLogFile(context).toPath()),java.nio.charset.StandardCharsets.UTF_8);
                assertEquals(2,log.split("RIPRESA SESSIONE SALVATA",-1).length-1);
                assertFalse(log.contains("*** INIZIO VIDEO 1 / 2 ***"));
            }finally{next.destroy();}
            install(new ConversionService.State());ConversionService.restoreSession(context);assertFalse(ConversionService.snapshot().recoverable);assertEquals(1,ConversionService.snapshot().succeeded);
            assertTrue(ConversionService.clearSessionIfIdle(context));assertNull(SavedSession.load(context));
        }finally{directory.release.countDown();controller.destroy();install(before);SavedSession.clear(context);}
    }
}
