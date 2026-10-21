package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowActivity;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageLiveLogTest {
    private Field stateField;private Object previous;
    @Before public void reset()throws Exception{
        var context=RuntimeEnvironment.getApplication();context.getSharedPreferences("image-workspace",0).edit().clear().commit();
        stateField=ImageConversionService.class.getDeclaredField("state");stateField.setAccessible(true);previous=stateField.get(null);stateField.set(null,new ImageRunState());
        ImageLogStore.reset(context);new File(context.getFilesDir(),ImageFailureStore.FILE).delete();
    }
    @After public void restore()throws Exception{stateField.set(null,previous);}

    @Test public void runningQueueExportsTheClickSnapshotAndShowsAllCumulativeCounts()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageRunState run=new ImageRunState();run.running=true;run.total=8500;run.current=600;
        for(int i=0;i<599;i++){
            ImageRunState.Outcome outcome=i<400?ImageRunState.Outcome.SAVED:i<590?ImageRunState.Outcome.FAILED:i<598?ImageRunState.Outcome.NOT_SMALLER:ImageRunState.Outcome.ALREADY_DONE;
            run.add(new ImageRunState.Result("Foto "+i,3000,2000,null,"Esito",outcome,""));
        }
        stateField.set(null,run);ImageLogStore.append(context,ImageConversionService.LOG,"PRIMA\n\n");
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();File exported=File.createTempFile("partial-export-",".txt");
        try{
            ImageController images=controller.get().images;assertTrue(images.screen.exportLog.isEnabled());assertFalse(images.screen.clearLog.isEnabled());
            String counts=images.screen.dashboard.summary.getText().toString();assertTrue(counts,counts.contains("Foto 600 / 8500"));assertTrue(counts,counts.contains("Riuscite: 400 · Fallite: 190"));assertTrue(counts,counts.contains("Già presenti: 1 · Senza risparmio: 8 · Annullate: 0"));assertEquals(100,run.results.size());
            images.screen.exportLog.performClick();ShadowActivity.IntentForResult picker=awaitPicker(controller.get());
            assertEquals(ImageSelection.LOG,picker.requestCode);assertEquals(Intent.ACTION_CREATE_DOCUMENT,picker.intent.getAction());assertTrue(picker.intent.getStringExtra(Intent.EXTRA_TITLE).contains("parziale"));assertFalse(images.screen.exportLog.isEnabled());
            ImageLogStore.append(context,ImageConversionService.LOG,"DOPO IL CLICK\n\n");
            images.selection.result(ImageSelection.LOG,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(exported)));awaitExport(images);
            assertEquals("PRIMA\n\n",read(exported));assertEquals("PRIMA\n\nDOPO IL CLICK\n\n",read(new File(context.getFilesDir(),ImageConversionService.LOG)));
            assertTrue(ImageConversionService.snapshot().running);assertTrue(images.screen.exportLog.isEnabled());assertFalse(images.screen.clearLog.isEnabled());assertEquals(0,snapshots(context).length);
        }finally{controller.pause().stop().destroy();exported.delete();}
    }

    @Test public void pickerRecreationAndCancellationKeepTheSourceAndDeleteTheSnapshot()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageLogStore.append(context,ImageConversionService.LOG,"ORIGINALE\n\n");
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();File output=File.createTempFile("recreated-export-",".txt");
        try{
            controller.get().images.selection.log();awaitPicker(controller.get());assertEquals(1,snapshots(context).length);
            controller.recreate();ImageController restored=controller.get().images;assertTrue(restored.logExport.busy());
            ImageLogStore.reset(context);ImageLogStore.append(context,ImageConversionService.LOG,"NUOVA CODA\n\n");
            restored.selection.result(ImageSelection.LOG,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(output)));awaitExport(restored);assertEquals("ORIGINALE\n\n",read(output));assertEquals(0,snapshots(context).length);
            restored.selection.log();awaitPicker(controller.get());File snapshot=snapshots(context)[0];
            restored.selection.result(ImageSelection.LOG,Activity.RESULT_CANCELED,null);assertFalse(snapshot.exists());assertFalse(restored.logExport.busy());assertEquals("NUOVA CODA\n\n",read(new File(context.getFilesDir(),ImageConversionService.LOG)));
        }finally{controller.pause().stop().destroy();output.delete();}
    }

    @Test public void failedExportCleansUpAndAllowsRetryWhileTheQueueRuns()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageRunState run=new ImageRunState();run.running=true;run.total=1;stateField.set(null,run);ImageLogStore.append(context,ImageConversionService.LOG,"EVENTO\n\n");
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            ImageController images=controller.get().images;images.selection.log();awaitPicker(controller.get());
            images.selection.result(ImageSelection.LOG,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(context.getCacheDir())));awaitExport(images);
            assertTrue(images.state.notice,images.state.notice.contains("non completata"));assertEquals(0,snapshots(context).length);assertTrue(images.screen.exportLog.isEnabled());assertTrue(ImageConversionService.snapshot().running);
        }finally{controller.pause().stop().destroy();}
    }

    @Test public void concurrentEventsProduceCompletePrefixSnapshotsWithoutLosingAnyEvent()throws Exception{
        var context=RuntimeEnvironment.getApplication();ExecutorService writer=Executors.newSingleThreadExecutor();List<File> copies=new ArrayList<>();
        try{
            ImageLogStore.append(context,ImageConversionService.LOG,"INIZIO\n\n");
            Future<?> writing=writer.submit(()->{try{for(int i=0;i<80;i++)ImageLogStore.append(context,ImageConversionService.LOG,"EVENTO "+i+" "+"x".repeat(10000)+"\n\n");}catch(IOException error){throw new UncheckedIOException(error);}});
            for(int i=0;i<8;i++)copies.add(ImageLogStore.snapshot(context,ImageConversionService.LOG));writing.get(10,TimeUnit.SECONDS);
            String complete=read(new File(context.getFilesDir(),ImageConversionService.LOG));
            for(File snapshot:copies){String text=read(snapshot);assertTrue(complete.startsWith(text));assertTrue(text.endsWith("\n\n"));}
            assertEquals(81,complete.split("\n\n").length);assertTrue(complete.contains("EVENTO 79 "));
        }finally{writer.shutdownNow();for(File file:copies)file.delete();}
    }

    @Test public void slowDestinationDoesNotHoldTheEventWriterLock()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageLogStore.append(context,ImageConversionService.LOG,"INIZIO\n\n");File snapshot=ImageLogStore.snapshot(context,ImageConversionService.LOG);
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);ExecutorService workers=Executors.newFixedThreadPool(2);
        try{
            Future<?> export=workers.submit(()->{try(InputStream in=new FileInputStream(snapshot)){ImageLogStore.copy(in,new OutputStream(){public void write(int value){}public void write(byte[] bytes,int offset,int length)throws IOException{entered.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new IOException("Timeout destinazione");}catch(InterruptedException error){Thread.currentThread().interrupt();throw new InterruptedIOException();}}});}catch(IOException error){throw new UncheckedIOException(error);}});
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            workers.submit(()->{try{ImageLogStore.append(context,ImageConversionService.LOG,"DURANTE EXPORT\n\n");}catch(IOException error){throw new UncheckedIOException(error);}}).get(2,TimeUnit.SECONDS);
            assertFalse(export.isDone());assertTrue(read(new File(context.getFilesDir(),ImageConversionService.LOG)).contains("DURANTE EXPORT"));release.countDown();export.get(5,TimeUnit.SECONDS);
        }finally{release.countDown();workers.shutdownNow();snapshot.delete();}
    }

    @Test public void runningFailureArchiveExportContainsOnlyWholeJsonRecords()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageRunState run=new ImageRunState();run.running=true;run.total=2;stateField.set(null,run);
        ImageOptions options=ImageOptions.load(context);ImageRunState.Result failure=new ImageRunState.Result("Errore.jpg",0,0,null,"Non supportata",ImageRunState.Outcome.FAILED,"stack\ntrace");
        ImageFailureStore.append(context,1,1,"content://photo/1","content://folder/out",options,failure);
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();File output=File.createTempFile("partial-errors-",".jsonl");
        try{
            ImageController images=controller.get().images;assertTrue(images.screen.exportErrors.isEnabled());images.selection.errors();ShadowActivity.IntentForResult picker=awaitPicker(controller.get());assertEquals(ImageSelection.ERRORS,picker.requestCode);
            ImageFailureStore.append(context,1,2,"content://photo/2","content://folder/out",options,failure);
            images.selection.result(ImageSelection.ERRORS,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(output)));awaitExport(images);
            List<String> lines=Files.readAllLines(output.toPath(),StandardCharsets.UTF_8);assertEquals(1,lines.size());assertEquals("content://photo/1",new JSONObject(lines.get(0)).getString("input"));assertEquals(2,Files.readAllLines(new File(context.getFilesDir(),ImageFailureStore.FILE).toPath()).size());
        }finally{controller.pause().stop().destroy();output.delete();}
    }

    private static ShadowActivity.IntentForResult awaitPicker(Activity activity)throws Exception{
        long deadline=System.currentTimeMillis()+5000;
        while(System.currentTimeMillis()<deadline){Shadows.shadowOf(Looper.getMainLooper()).idle();ShadowActivity.IntentForResult picker=Shadows.shadowOf(activity).getNextStartedActivityForResult();if(picker!=null)return picker;Thread.sleep(5);}
        throw new AssertionError("Snapshot picker did not open");
    }
    private static void awaitExport(ImageController images)throws Exception{
        long deadline=System.currentTimeMillis()+5000;
        while(images.logExport.busy()&&System.currentTimeMillis()<deadline){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse("Export did not finish",images.logExport.busy());
    }
    private static String read(File file)throws IOException{return new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);}
    private static File[] snapshots(android.content.Context context){return context.getCacheDir().listFiles((directory,name)->name.startsWith("image-log-snapshot-"));}
}
