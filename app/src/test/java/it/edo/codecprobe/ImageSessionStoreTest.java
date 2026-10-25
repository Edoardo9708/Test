package it.edo.codecprobe;

import android.content.*;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class ImageSessionStoreTest {
    Context context;String id;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();ImageSessionStore.discard(context);}
    @After public void cleanup(){ImageSessionStore.discard(context);if(id!=null)ImageQueueStore.discard(context,id);}
    private ImageRunState sample()throws Exception{
        ImageRunState run=new ImageRunState();run.options=new ImageOptions(50,"medium","ssim","content://dest/tree/root",false,true,0);run.total=125;run.elapsedBefore=1234;
        ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>(),folders=new ArrayList<>();for(int i=0;i<125;i++){inputs.add("content://photos/"+i);names.add("photo"+i+".jpg");folders.add(run.options.folder);}
        id=ImageQueueStore.write(context,inputs,names,folders);return run;
    }
    @Test public void freshLoadKeepsQueueOptionsCursorAndCumulativeTotalsWithoutGrowingWithDetails()throws Exception{
        ImageRunState run=sample();for(int i=0;i<120;i++)run.add(new ImageRunState.Result("photo"+i,3000,1000,"content://out/"+i,"OK",ImageRunState.Outcome.SAVED,"x".repeat(20000)));
        run.add(new ImageRunState.Result("failed",0,0,null,"err",ImageRunState.Outcome.FAILED,""));ImageSessionStore.save(context,id,run);
        ImageSessionStore.Session restored=ImageSessionStore.load(context);assertEquals(id,restored.queueId);assertEquals(121,restored.run.completed);assertEquals(120,restored.run.saved);assertEquals(1,restored.run.failed);assertEquals(240000,restored.run.inputSavedBytes-restored.run.outputSavedBytes);assertEquals(1234,restored.run.elapsedBefore);
        assertEquals(125,ImageQueueStore.read(context,id).inputs.size());assertTrue(restored.run.results.isEmpty());assertTrue(new File(context.getFilesDir(),"image-session.json").length()<2048);
        Intent request=restored.request(context);ImageOptions options=ImageOptions.from(request);assertEquals(50,options.quality);assertEquals("medium",options.preset);assertEquals("ssim",options.tune);assertFalse(options.onlySmaller);assertTrue(options.keepScreenOn);assertTrue(request.getBooleanExtra("restore",false));assertEquals(id,request.getStringExtra("queueId"));
    }
    @Test public void interruptedAtomicWriteKeepsPreviousCheckpointAndExplicitDiscardRemovesQueue()throws Exception{
        ImageRunState run=sample();ImageSessionStore.save(context,id,run);
        AtomicFile file=new AtomicFile(new File(context.getFilesDir(),"image-session.json"));FileOutputStream incomplete=file.startWrite();incomplete.write("broken".getBytes(java.nio.charset.StandardCharsets.UTF_8));incomplete.close();
        assertEquals(0,ImageSessionStore.load(context).run.completed);assertTrue(ImageSessionStore.exists(context));
        ImageSessionStore.discard(context);assertFalse(ImageSessionStore.exists(context));assertFalse(ImageQueueStore.file(context,id).exists());
    }
    @Test public void missingQueueIsReportedWithoutSilentlyDroppingTheSession()throws Exception{
        ImageRunState run=sample();ImageSessionStore.save(context,id,run);ImageQueueStore.discard(context,id);
        assertThrows(IOException.class,()->ImageSessionStore.load(context));assertTrue(ImageSessionStore.exists(context));
    }
    @Test public void copiedCountAndOptionSurviveQueueRecovery()throws Exception{
        ImageRunState run=sample();run.options=new ImageOptions(50,"medium","ssim",run.options.folder,true,true,true,0);
        run.add(new ImageRunState.Result("foto.jpg",100,100,"content://out/copy","Copied",ImageRunState.Outcome.COPIED_ORIGINAL,""));
        ImageSessionStore.save(context,id,run);var restored=ImageSessionStore.load(context);
        assertEquals(1,restored.run.copied);assertEquals(0,restored.run.saved);assertEquals(1,restored.run.completed);
        assertEquals(100,restored.run.outputSavedBytes);assertTrue(restored.run.options.copyOriginalWhenLarger);
        assertTrue(ImageOptions.from(restored.request(context)).copyOriginalWhenLarger);
    }
    @Test public void sessionsFromBeforeOriginalCopyOptionRemainReadable()throws Exception{
        ImageRunState run=sample();ImageSessionStore.save(context,id,run);
        File checkpoint=new File(context.getFilesDir(),"image-session.json");
        org.json.JSONObject legacy=new org.json.JSONObject(new String(java.nio.file.Files.readAllBytes(checkpoint.toPath()),java.nio.charset.StandardCharsets.UTF_8));
        legacy.remove("copied");legacy.remove("copyOriginalWhenLarger");java.nio.file.Files.write(checkpoint.toPath(),legacy.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var restored=ImageSessionStore.load(context);assertNotNull(restored);assertEquals(0,restored.run.copied);assertFalse(restored.run.options.copyOriginalWhenLarger);assertEquals(125,restored.run.total);
    }
}
