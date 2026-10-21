package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageLargeQueueTest {
    @Before public void reset(){var c=RuntimeEnvironment.getApplication();c.getSharedPreferences("image-workspace",0).edit().clear().commit();ImageMetadataCache.memory.clear();new File(c.getFilesDir(),ImageFailureStore.FILE).delete();}
    @Test public void fiveThousandOutcomesKeepOnlyOneHundredDetailsAndExactTotals(){
        ImageRunState run=new ImageRunState();run.total=5000;
        for(int i=0;i<5000;i++)run.add(new ImageRunState.Result("Foto "+i,3000000,2000000,"content://saved/"+i,"Salvato",ImageRunState.Outcome.SAVED,"Dettagli"));
        assertEquals(5000,run.completed);assertEquals(5000,run.saved);assertEquals(100,run.results.size());assertEquals(4900,run.firstResult());
        assertNull(run.resultAt(4899));assertEquals("Foto 4900",run.resultAt(4900).name);
        assertEquals(15000000000L,run.inputSavedBytes);assertEquals(10000000000L,run.outputSavedBytes);
        ImageRunState copy=new ImageRunState(run);assertEquals(100,copy.results.size());assertEquals(run.inputSavedBytes,copy.inputSavedBytes);
        String huge="x".repeat(100000);run.add(new ImageRunState.Result("errore",0,0,null,"Errore",ImageRunState.Outcome.FAILED,huge));
        assertTrue(run.results.get(99).details.length()<17000);assertEquals(1,run.failed);assertEquals(5000,run.saved);
    }
    @Test public void carouselShowsAllSmallQueuesAndOnlyOneHundredPlusEllipsisForLargeQueues(){
        var controller=Robolectric.buildActivity(Activity.class).setup();try{
            AppUi ui=new AppUi(controller.get());SelectedItemView view=new SelectedItemView(ui,ui.newPage(),"Foto","Nessuna","Dettagli");
            ArrayList<String> names=new ArrayList<>();for(int i=0;i<5000;i++)names.add("Foto "+i);
            view.render(names.subList(0,100),Collections.emptyList(),0,true,n->{},100);assertEquals(100,view.chips.size());assertEquals(100,view.navigator.getChildCount());
            view.render(names,Collections.emptyList(),0,true,n->{},100);assertEquals(100,view.chips.size());assertEquals(101,view.navigator.getChildCount());
            assertEquals("…",((TextView)view.navigator.getChildAt(100)).getText().toString());assertTrue(view.hint.getText().toString().contains("5000"));view.close();
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void reportCentersCurrentPhotoAndStillIncludesTotalsAfterEviction(){
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageRunState run=new ImageRunState();run.total=5000;run.current=1000;run.running=true;run.message="Verifica";
            ArrayList<String> names=new ArrayList<>();for(int i=0;i<5000;i++)names.add("Nome "+(i+1));run.queueNames=Collections.unmodifiableList(names);
            for(int i=0;i<999;i++)run.add(new ImageRunState.Result(names.get(i),3000000,2000000,"content://saved/"+i,"Salvato",ImageRunState.Outcome.SAVED,"OK"));
            var images=controller.get().images;images.report.render(run);assertEquals(100,images.screen.workReport.rows.getChildCount());
            assertTrue(text(images.screen.workReport.rows.getChildAt(49)).contains("Foto 1000 · Nome 1000"));assertTrue(text(images.screen.workReport.rows.getChildAt(49)).contains("IN CORSO"));
            assertTrue(text(images.screen.workReport.rows.getChildAt(50)).contains("In attesa"));assertTrue(images.screen.workReport.summary.getText().toString().contains("Salvate 999"));
            assertEquals(950,run.windowStart());run.current=5000;assertEquals(4900,run.windowStart());run.current=1;assertEquals(0,run.windowStart());
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void fullQueueIsOnDiskAndTheIntentContainsOnlyItsIdentifier()throws Exception{
        var context=RuntimeEnvironment.getApplication();ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>(),folders=new ArrayList<>();
        for(int i=0;i<5000;i++){inputs.add("content://photos/"+i+"/"+"a".repeat(120));names.add("Foto "+i);folders.add("content://dirs/tree/out");}
        String id=ImageQueueStore.write(context,inputs,names,folders);
        try{
            ImageQueueStore.Queue read=ImageQueueStore.read(context,id);assertEquals(inputs,read.inputs);assertEquals(names,read.names);assertEquals(folders,read.folders);
            Intent request=new Intent().putExtra("queueId",id);Parcel p=Parcel.obtain();try{request.writeToParcel(p,0);assertTrue(p.dataSize()<4096);}finally{p.recycle();}
            inputs.add("content://too-many");folders.add("content://dirs/tree/out");try{ImageQueueStore.write(context,inputs,names,folders);fail();}catch(IOException expected){}
            try{ImageQueueStore.read(context,"../bad");fail();}catch(IOException expected){}
        }finally{ImageQueueStore.discard(context,id);}
    }
    @Test public void failureArchiveSurvivesHistoryEvictionAndContainsRetryInputs()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageOptions options=ImageOptions.load(context);ImageRunState run=new ImageRunState();
        for(int i=0;i<125;i++){
            ImageRunState.Result result=new ImageRunState.Result("Fallita "+i,0,0,null,"Errore "+i,ImageRunState.Outcome.FAILED,"Dettaglio "+i);
            ImageFailureStore.append(context,77,i+1,"content://source/"+i,"content://folder/out",options,result);run.add(result);
        }
        assertEquals(100,run.results.size());assertEquals(125,run.failed);assertNull(run.resultAt(0));
        java.util.List<String> lines=Files.readAllLines(new File(context.getFilesDir(),ImageFailureStore.FILE).toPath(),StandardCharsets.UTF_8);assertEquals(125,lines.size());
        JSONObject first=new JSONObject(lines.get(0));assertEquals("content://source/0",first.getString("input"));assertEquals("content://folder/out",first.getString("folder"));assertEquals("Dettaglio 0",first.getString("details"));assertEquals(options.quality,first.getInt("quality"));
        ImageConversionService.clear();assertTrue(new File(context.getFilesDir(),ImageFailureStore.FILE).isFile());
    }
    @Test public void serviceContinuesAfterMoreThanOneHundredFailuresAndPreservesEveryError()throws Exception{
        var context=RuntimeEnvironment.getApplication();
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("destinations",new MediaDestinationsTest.Directories());
        var stateField=ImageConversionService.class.getDeclaredField("state");stateField.setAccessible(true);Object previous=stateField.get(null);stateField.set(null,new ImageRunState());
        var controller=Robolectric.buildService(ImageConversionService.class).create();
        ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>(),folders=new ArrayList<>();
        for(int i=0;i<125;i++){inputs.add("file:///missing-large-queue-"+i+".jpg");names.add("Foto "+i);folders.add(MediaDestinationsTest.A);}
        String id=ImageQueueStore.write(context,inputs,names,folders);
        try{
            Intent request=new Intent().setAction(ImageConversionService.START).putExtra("queueId",id);ImageOptions.load(context).put(request);
            controller.get().onStartCommand(request,0,1);
            long end=System.currentTimeMillis()+15000;
            while(ImageConversionService.snapshot().running&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10);}
            ImageRunState run=ImageConversionService.snapshot();assertFalse(run.message,run.running);assertEquals(125,run.completed);assertEquals(125,run.failed);assertEquals(100,run.results.size());
            assertEquals(125,Files.readAllLines(new File(context.getFilesDir(),ImageFailureStore.FILE).toPath()).size());
            String log=new String(Files.readAllBytes(new File(context.getFilesDir(),ImageConversionService.LOG).toPath()),StandardCharsets.UTF_8);assertTrue(log.contains("missing-large-queue-0.jpg"));assertTrue(log.contains("missing-large-queue-124.jpg"));assertTrue(run.log.length()<=24000);assertFalse(ImageQueueStore.file(context,id).exists());
        }finally{controller.destroy();stateField.set(null,previous);ImageQueueStore.discard(context,id);}
    }
    @Test public void largeDraftRestoresAllFiveThousandReferences(){
        var context=RuntimeEnvironment.getApplication();ImageState state=new ImageState();state.options=ImageOptions.load(context);
        for(int i=0;i<5000;i++){state.inputs.add("content://photo/"+i);state.names.add("Foto "+i);}
        ImageDraftStore.save(context,state);ImageState restored=ImageDraftStore.load(context);assertEquals(state.inputs,restored.inputs);assertEquals(state.names,restored.names);
    }
    @Test public void diskLogKeepsEarlyErrorsBeyondTheOldTwoMibRotationLimit()throws Exception{
        var field=ImageConversionService.class.getDeclaredField("state");field.setAccessible(true);Object previous=field.get(null);field.set(null,new ImageRunState());
        var controller=Robolectric.buildService(ImageConversionService.class).create();
        try{
            var write=ImageConversionService.class.getDeclaredMethod("write",String.class);write.setAccessible(true);
            write.invoke(controller.get(),"FIRST_ERROR\n"+"x".repeat(2*1024*1024));write.invoke(controller.get(),"LAST_ERROR");
            String disk=new String(Files.readAllBytes(new File(controller.get().getFilesDir(),ImageConversionService.LOG).toPath()),StandardCharsets.UTF_8);assertTrue(disk.contains("FIRST_ERROR"));assertTrue(disk.contains("LAST_ERROR"));assertTrue(ImageConversionService.snapshot().log.length()<=24000);
        }finally{controller.destroy();field.set(null,previous);}
    }
    private static String text(View view){StringBuilder s=new StringBuilder();if(view instanceof TextView)s.append(((TextView)view).getText());if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)s.append(text(((ViewGroup)view).getChildAt(i)));return s.toString();}
}
