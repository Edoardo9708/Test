package it.edo.codecprobe;

import android.content.*;
import android.os.*;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowToast;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class RecoveryPresentationTest {
    private Field field()throws Exception{Field f=ConversionService.class.getDeclaredField("state");f.setAccessible(true);return f;}

    @Test public void pendingRecoveryHidesFileMetricsWithoutLosingReportOrJournal()throws Exception{
        Context context=RuntimeEnvironment.getApplication();ConversionService.State before=ConversionService.snapshot();
        Intent request=BatchRetryTest.queue(context);ConversionService.State saved=SavedSessionTest.sample(request);
        saved.bframeCount=4;saved.lookaheadCount=5;saved.cuTree=0;saved.aqMode=2;saved.aqStrength=1000;saved.memoryProtection=true;
        SavedSession.save(context,request,saved);field().set(null,new ConversionService.State());
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;assertTrue(home.state.selectedInputs.isEmpty());assertNull(home.selection.metadataDialog);
            assertEquals("0%",home.screen.conversion.percentLabel.getText().toString());assertEquals(0,home.screen.conversion.progress.getProgress());
            assertEquals("—",home.screen.conversion.frameLabel.getText().toString());assertEquals("—",home.screen.conversion.timeline.getText().toString());
            assertEquals("—",home.screen.conversion.activeCpu.getText().toString());assertEquals("—",home.screen.conversion.activeCrf.getText().toString());
            assertEquals("—",home.screen.conversion.activeBframes.getText().toString());assertEquals("—",home.screen.conversion.activeLookahead.getText().toString());
            assertEquals("Nessun file in conversione",home.screen.conversion.currentFile.getText().toString());
            assertEquals("00:00:45",home.screen.conversion.elapsedLabel.getText().toString());assertTrue(home.screen.conversion.sizeReport.getText().toString().contains("0,50 kB"));
            SavedSession untouched=SavedSession.load(context);assertEquals(42,untouched.state.filePercent);assertEquals(100,untouched.state.doneFrames);assertEquals(saved.outputUris,untouched.state.outputUris);
            ConversionService.State active=new ConversionService.State(untouched.state);active.running=true;active.filePercent=7;active.doneFrames=17;
            home.conversion.renderConversionDashboard(active);assertEquals("7%",home.screen.conversion.percentLabel.getText().toString());
            assertTrue(home.screen.conversion.frameLabel.getText().toString().startsWith("17 /"));
        }finally{controller.pause().stop().destroy();field().set(null,before);SavedSession.clear(context);}
    }

    @Test public void explicitResumeRestoresNamesImmediatelyAndUsesOneBackgroundReaderAcrossRecreation()throws Exception{
        Context context=RuntimeEnvironment.getApplication();ConversionService.State before=ConversionService.snapshot();
        Intent request=BatchRetryTest.queue(context);SavedSession.save(context,request,SavedSessionTest.sample(request));field().set(null,new ConversionService.State());
        VideoMetadataCache.clear();var controller=Robolectric.buildActivity(MainActivity.class).setup();
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);Bundle bundle=new Bundle();
        try{
            HomeController home=controller.get().home;assertTrue(home.state.selectedInputs.isEmpty());assertFalse(home.state.metadataLoading);
            home.selection.worker.submit(()->{entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});
            assertTrue(entered.await(2,TimeUnit.SECONDS));Intent resume=SavedSession.load(context).resumeRequest();
            home.conversion.launchPrepared(resume);
            assertEquals(request.getStringArrayListExtra("inputs"),home.state.selectedInputs);assertEquals(request.getStringArrayListExtra("names"),home.state.selectedNames);
            assertEquals(request.getStringArrayListExtra("folders"),home.state.destinationPlan.individualFolders());
            assertArrayEquals(request.getBooleanArrayExtra("individualOrigins"),home.state.destinationPlan.individualOrigins());
            assertEquals("saved-test-session",home.state.selectionSession);
            assertTrue(home.state.metadataLoading);assertTrue(home.selection.metadataBackground);assertNull(home.selection.metadataDialog);
            assertEquals("Rileggo i metadati in background",ShadowToast.getTextOfLatestToast());assertEquals(3,home.screen.files.videoChips.size());
            assertTrue(home.screen.conversion.savedSession.isEnabled());
            int generation=home.state.metadataGeneration;home.selection.inspectInput();assertEquals(generation,home.state.metadataGeneration);
            home.screen.files.videoChips.get(1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertEquals(1,home.state.selectedVideoIndex);
            controller.saveInstanceState(bundle);
        }finally{release.countDown();controller.pause().stop().destroy();}
        var recreated=Robolectric.buildActivity(MainActivity.class).create(bundle).start().resume();
        try{
            HomeController home=recreated.get().home;assertEquals(3,home.state.selectedInputs.size());assertEquals("saved-test-session",home.state.selectionSession);
            assertNull(home.selection.metadataDialog);
            home.selection.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(home.state.metadataLoading);assertTrue(VideoMetadataCache.size()>0);assertNull(home.selection.metadataDialog);
            int generation=home.state.metadataGeneration;home.selection.inspectInput();assertEquals(generation,home.state.metadataGeneration);
            Bundle dead=new Bundle(bundle);dead.putString("homeProcess","dead-process");Bundle clean=HomeSavedState.inThisProcess(dead);
            assertNull(clean.getStringArrayList("inputs"));assertNull(clean.getString("selectionSession"));
        }finally{recreated.pause().stop().destroy();field().set(null,before);SavedSession.clear(context);VideoMetadataCache.clear();}
    }
}
