package it.edo.codecprobe;

import android.content.*;
import android.net.Uri;
import android.os.Looper;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageSessionServiceTest {
    Context context;String id;ImageRunState before;File source,output;
    @Before public void setup()throws Exception{
        context=RuntimeEnvironment.getApplication();before=ImageConversionService.snapshot();install(new ImageRunState());ImageSessionStore.discard(context);context.deleteDatabase(ImageProgressJournal.DATABASE);
        source=File.createTempFile("image-source-test-",".jpg",context.getCacheDir());output=File.createTempFile("image-output-test-",".heic",context.getCacheDir());Files.write(source.toPath(),new byte[]{1,2,3});Files.write(output.toPath(),new byte[]{4,5});
    }
    @After public void cleanup()throws Exception{ImageSessionStore.discard(context);if(id!=null)ImageQueueStore.discard(context,id);source.delete();output.delete();install(before);}
    private static void install(ImageRunState state)throws Exception{var field=ImageConversionService.class.getDeclaredField("state");field.setAccessible(true);field.set(null,state);}
    private static void drain(ImageConversionService service)throws Exception{var field=ImageConversionService.class.getDeclaredField("worker");field.setAccessible(true);((ExecutorService)field.get(service)).submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();}
    private ImageRunState prepare()throws Exception{
        ImageRunState run=new ImageRunState();run.total=2;run.options=new ImageOptions(50,"slow","standard","content://dest/tree/root",true,false,0);
        String input=Uri.fromFile(source).toString();id=ImageQueueStore.write(context,List.of("content://must-not-be-reopened/first",input),List.of("first.jpg","last.jpg"),Collections.nCopies(2,run.options.folder));
        run.add(new ImageRunState.Result("first.jpg",300,100,"content://saved/first","OK",ImageRunState.Outcome.SAVED,""));ImageSessionStore.save(context,id,run);
        try(ImageProgressJournal journal=new ImageProgressJournal(context)){journal.record(input,run.options.folder,run.options,new ImageRunState.Result("last.jpg",source.length(),output.length(),Uri.fromFile(output).toString(),"OK",ImageRunState.Outcome.SAVED,"",ImageFingerprint.file(source,()->false),ImageFingerprint.file(output,()->false)));}
        return run;
    }
    @Test public void freshServiceResumesAfterCursorPreservesTotalsAndAppendsLog()throws Exception{
        prepare();ImageLogStore.append(context,ImageConversionService.LOG,"previous-details\n");install(new ImageRunState());
        var controller=Robolectric.buildService(ImageConversionService.class).create();try{
            controller.get().onStartCommand(ImageSessionStore.load(context).request(context),0,1);drain(controller.get());
            ImageRunState run=ImageConversionService.snapshot();assertFalse(run.running);assertEquals(2,run.completed);assertEquals(1,run.saved);assertEquals(1,run.alreadyDone);assertEquals(201,run.inputSavedBytes-run.outputSavedBytes);assertFalse(ImageSessionStore.exists(context));assertFalse(ImageQueueStore.file(context,id).exists());
            String log=new String(Files.readAllBytes(new File(context.getFilesDir(),ImageConversionService.LOG).toPath()),java.nio.charset.StandardCharsets.UTF_8);assertTrue(log.startsWith("previous-details"));assertTrue(log.contains("RIPRESA CODA"));assertFalse(log.contains("must-not-be-reopened"));
        }finally{controller.destroy();}
    }
    @Test public void fatalLogFailureKeepsQueueAndPreviousCursorForRetry()throws Exception{
        prepare();File log=new File(context.getFilesDir(),ImageConversionService.LOG);log.delete();assertTrue(log.mkdir());
        var controller=Robolectric.buildService(ImageConversionService.class).create();try{
            controller.get().onStartCommand(ImageSessionStore.load(context).request(context),0,1);drain(controller.get());assertFalse(ImageConversionService.snapshot().running);assertTrue(ImageConversionService.snapshot().message.contains("Coda conservata"));assertTrue(ImageQueueStore.file(context,id).exists());assertEquals(1,ImageSessionStore.load(context).run.completed);
        }finally{controller.destroy();log.delete();}
    }
}
