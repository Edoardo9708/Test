package it.edo.codecprobe;

import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.View;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class HomeResetTest {
    private ConversionService.State before;
    private Intent beforeRetry;
    private Field stored;
    @Before public void before()throws Exception{
        before=ConversionService.snapshot();beforeRetry=ConversionService.retryRequest(RuntimeEnvironment.getApplication());
        stored=ConversionService.class.getDeclaredField("state");stored.setAccessible(true);
        stored.set(null,new ConversionService.State());
    }
    @After public void after()throws Exception{
        stored.set(null,before);ConversionService.rememberFailures(RuntimeEnvironment.getApplication(),beforeRetry);VideoMetadataCache.clear();
    }
    @Test public void clearRemovesSelectionAndReportWithoutDeletingMediaSettingsOrLogAndSurvivesRecreation()throws Exception{
        Context context=RuntimeEnvironment.getApplication();
        File original=File.createTempFile("keep-original-",".mp4",context.getCacheDir());
        File converted=File.createTempFile("keep-output-",".mp4",context.getCacheDir());
        Files.write(original.toPath(),new byte[]{1,2,3});Files.write(converted.toPath(),new byte[]{4,5});
        File log=ConversionService.batchLogFile(context);byte[] oldLog=log.exists()?Files.readAllBytes(log.toPath()):null;
        Files.write(log.toPath(),"Detailed log kept for export".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String input=Uri.fromFile(original).toString();
        ConversionService.State completed=new ConversionService.State();completed.hasSession=true;completed.batchIndex=2;completed.batchTotal=2;completed.filePercent=100;completed.succeeded=1;completed.failed=1;
        completed.inputs.add(input);completed.names.add("clip.mp4");completed.folders.add("content://dirs/tree/zero");completed.result=Uri.fromFile(converted);completed.results.add(completed.result);
        completed.failures.add(new ConversionFailure(2,"failed.mp4","Analisi",new IOException("Example error")));
        completed.sizeReport=CompressionSummary.render(List.of(new CompressionSummary.Item(1,"clip.mp4",CompressionSummary.Status.COMPLETE,3,2),new CompressionSummary.Item(2,"failed.mp4",CompressionSummary.Status.FAILED,-1,-1)));
        stored.set(null,completed);ConversionService.rememberFailures(context,BatchRetry.select(BatchRetryTest.queue(context),List.of(1)));
        VideoMetadataCache.permissions("false:false");VideoMetadataCache.clear();
        VideoMetadataCache.Entry entry=new VideoMetadataCache.Entry("clip.mp4","Video · 3 byte",new InputInspector.Report("video","metadata"));
        VideoMetadataCache.put(input,entry,VideoMetadataCache.epoch());
        Bundle saved=new Bundle();saved.putString("input",input);saved.putStringArrayList("inputs",new ArrayList<>(List.of(input)));saved.putStringArrayList("names",new ArrayList<>(List.of("clip.mp4")));
        var controller=Robolectric.buildActivity(MainActivity.class).create(saved).start().resume();
        try{
            HomeController home=controller.get().home;home.conversion.renderSession();
            home.screen.compression.crfSlider.setProgress(27);home.screen.compression.presetSlider.setProgress(3);
            Map<String,?> settings=new HashMap<>(context.getSharedPreferences("startup-settings",0).getAll());
            long epoch=VideoMetadataCache.epoch();int generation=home.state.metadataGeneration;
            home.screen.files.videoPreview.setAlpha(0.3f);
            assertTrue(home.screen.files.clearSelection.isEnabled());assertEquals(View.VISIBLE,home.screen.conversion.sizeReportPanel.getVisibility());
            home.screen.files.clearSelection.performClick();
            assertTrue(home.state.selectedInputs.isEmpty());assertTrue(home.state.selectedNames.isEmpty());assertNull(home.state.input);assertNull(home.state.resultUri);
            assertTrue(home.state.originFolders.isEmpty());assertTrue(home.state.destinationPlan.files.isEmpty());assertFalse(home.state.originsReady);
            assertTrue(home.state.metadataGeneration>generation);assertEquals(0,VideoMetadataCache.size());assertFalse(VideoMetadataCache.put(input,entry,epoch));
            assertEquals(1f,home.screen.files.videoPreview.getAlpha(),0f);assertEquals(0,home.screen.files.videoNavigator.getChildCount());assertEquals(0,home.screen.files.inputDetails.getChildCount());
            assertEquals("Nessun video selezionato",home.screen.files.videoLabel.getText().toString());assertEquals(View.GONE,home.screen.files.videoDetails.getVisibility());
            assertEquals(0,home.screen.conversion.progress.getProgress());assertEquals("Nessuna coda avviata",home.screen.conversion.queueLabel.getText().toString());
            assertEquals(View.GONE,home.screen.conversion.sizeReportPanel.getVisibility());assertEquals("",home.screen.conversion.sizeReport.getText().toString());assertEquals(0,home.screen.conversion.failureReport.getChildCount());
            assertEquals(View.GONE,home.screen.conversion.openVideo.getVisibility());assertEquals(View.GONE,home.screen.conversion.retryFailed.getVisibility());assertFalse(home.screen.files.clearSelection.isEnabled());
            assertEquals(27,home.screen.compression.crfSlider.getProgress());assertEquals(3,home.screen.compression.presetSlider.getProgress());assertEquals(settings,context.getSharedPreferences("startup-settings",0).getAll());
            assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(original.toPath()));assertArrayEquals(new byte[]{4,5},Files.readAllBytes(converted.toPath()));
            assertEquals("Detailed log kept for export",new String(Files.readAllBytes(log.toPath()),java.nio.charset.StandardCharsets.UTF_8));
            assertFalse(ConversionService.snapshot().hasSession);assertTrue(ConversionService.snapshot().failures.isEmpty());assertNull(BatchRetry.load(context));assertNull(ConversionService.retryRequest(context));
            home.conversion.renderSession();assertNull(home.state.input);
            Bundle empty=new Bundle();controller.saveInstanceState(empty).pause().stop().destroy();
            controller=Robolectric.buildActivity(MainActivity.class).create(empty).start().resume();
            home=controller.get().home;Shadows.shadowOf(Looper.getMainLooper()).idle();home.conversion.renderSession();
            assertTrue(home.state.selectedInputs.isEmpty());assertNull(home.state.resultUri);assertEquals(View.GONE,home.screen.conversion.sizeReportPanel.getVisibility());
            home.screen.files.pickVideo.performClick();assertEquals(Intent.ACTION_OPEN_DOCUMENT,Shadows.shadowOf(controller.get()).getNextStartedActivityForResult().intent.getAction());
        }finally{
            controller.pause().stop().destroy();original.delete();converted.delete();if(oldLog==null)log.delete();else Files.write(log.toPath(),oldLog);
        }
    }
    @Test public void activeOrPausedQueueCannotBeClearedEvenByCallingTheActionDirectly()throws Exception{
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;ConversionService.State active=new ConversionService.State();active.running=true;active.hasSession=true;active.paused=true;active.pauseRequested=true;active.sizeReport="Report to keep";stored.set(null,active);
            home.conversion.setBusy(true);assertFalse(home.screen.files.clearSelection.isEnabled());home.reset.clear();
            assertFalse(ConversionService.clearSessionIfIdle(controller.get()));assertTrue(ConversionService.snapshot().running);assertEquals("Report to keep",ConversionService.snapshot().sizeReport);
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void metadataReadCannotBeClearedUntilItFinishes(){
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;home.state.selectedInputs.add("content://input/pending");home.state.metadataLoading=true;
            home.reset.update(false);assertFalse(home.screen.files.clearSelection.isEnabled());home.reset.clear();assertEquals(1,home.state.selectedInputs.size());
            home.state.metadataLoading=false;
        }finally{controller.pause().stop().destroy();}
    }
}
