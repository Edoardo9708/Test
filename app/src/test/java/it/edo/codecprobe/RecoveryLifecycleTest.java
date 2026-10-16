package it.edo.codecprobe;

import android.content.*;
import android.os.*;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class RecoveryLifecycleTest {
    private static Field stateField()throws Exception{Field field=ConversionService.class.getDeclaredField("state");field.setAccessible(true);return field;}
    private static void drain(ConversionService service)throws Exception{
        Field field=ConversionService.class.getDeclaredField("worker");field.setAccessible(true);ExecutorService worker=(ExecutorService)field.get(service);
        if(worker.isShutdown())assertTrue(worker.awaitTermination(5,TimeUnit.SECONDS));else worker.submit(()->{}).get(5,TimeUnit.SECONDS);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
    @Test public void restoredBundleStartsEmptyWithoutMetadataScanAndBothTimersUseAccumulatedTime()throws Exception{
        Context context=RuntimeEnvironment.getApplication();ConversionService.State before=ConversionService.snapshot();
        Intent request=BatchRetryTest.queue(context);SavedSession.save(context,request,SavedSessionTest.sample(request));stateField().set(null,new ConversionService.State());
        Bundle fromDeadProcess=new Bundle();fromDeadProcess.putString("homeProcess","another-process");fromDeadProcess.putString("input","content://input/0");
        fromDeadProcess.putStringArrayList("inputs",request.getStringArrayListExtra("inputs"));fromDeadProcess.putStringArrayList("names",request.getStringArrayListExtra("names"));
        var activity=Robolectric.buildActivity(MainActivity.class).create(fromDeadProcess).start().resume().visible();
        try{
            HomeController home=activity.get().home;
            assertNull(home.state.input);assertTrue(home.state.selectedInputs.isEmpty());assertFalse(home.state.metadataLoading);assertNull(home.selection.metadataDialog);
            assertTrue(home.screen.conversion.sizeReport.getText().toString().contains("0,50 kB"));assertTrue(ConversionService.snapshot().recoverable);
            home.handler.removeCallbacks(home.system.sample);
            ConversionService.State saved=ConversionService.snapshot();home.conversion.renderConversionDashboard(saved);String initial=home.screen.conversion.elapsedLabel.getText().toString();
            home.system.sample.run();assertEquals(initial,home.screen.conversion.elapsedLabel.getText().toString());assertEquals("00:00:45",initial);
            ShadowSystemClock.advanceBy(Duration.ofSeconds(10));
            ConversionService.State running=new ConversionService.State(saved);running.startedAt=SystemClock.elapsedRealtime()-5000;running.running=true;stateField().set(null,running);
            home.conversion.renderConversionDashboard(running);assertEquals("00:00:50",home.screen.conversion.elapsedLabel.getText().toString());
            home.system.sample.run();assertEquals("00:00:50",home.screen.conversion.elapsedLabel.getText().toString());
            running.endedAt=SystemClock.elapsedRealtime();running.running=false;ShadowSystemClock.advanceBy(Duration.ofHours(2));
            home.system.sample.run();assertEquals("00:00:50",home.screen.conversion.elapsedLabel.getText().toString());
            assertNull(home.selection.metadataDialog);assertTrue(home.state.selectedInputs.isEmpty());
        }finally{activity.pause().stop().destroy();stateField().set(null,before);SavedSession.clear(context);}
    }

    @Test public void destroyedWorkerRetainsLeaseUntilCleanupAndRepeatedStartsCannotOverlap()throws Exception{
        Context context=RuntimeEnvironment.getApplication();ConversionService.State before=ConversionService.snapshot();
        Intent request=BatchRetry.select(BatchRetryTest.queue(context),List.of(0,1));
        request.putStringArrayListExtra("outputNames",new ArrayList<>(List.of("first.mp4","second.mp4"))).putExtra("omitted",new boolean[2]);
        ConversionService.State original=SavedSessionTest.sample(request);original.bframeCount=8;original.lookaheadCount=40;original.aqMode=3;original.aqStrength=1000;
        SavedSession.save(context,request,original);stateField().set(null,new ConversionService.State());
        SavedSessionServiceTest.Directory directory=new SavedSessionServiceTest.Directory();ShadowContentResolver.registerProviderInternal("dirs",directory);
        var old=Robolectric.buildService(ConversionService.class).create();var other=Robolectric.buildService(ConversionService.class).create();
        try{
            old.get().onStartCommand(SavedSession.load(context).resumeRequest(),0,1);assertTrue(directory.entered.await(3,TimeUnit.SECONDS));
            ConversionService.State active=ConversionService.snapshot();assertEquals(0,active.doneFrames);assertEquals(0,active.filePercent);assertEquals(-1,active.lookaheadCount);assertEquals(-1,active.aqMode);assertEquals(-1,active.aqStrength);
            assertEquals(1,active.succeeded);assertEquals(45000,active.elapsedBefore);
            old.get().onStartCommand(SavedSession.load(context).resumeRequest(),0,2);assertEquals(1,directory.requested.size());
            old.destroy();other.get().onStartCommand(SavedSession.load(context).resumeRequest(),0,3);
            assertEquals(active.sessionId,ConversionService.snapshot().sessionId);assertEquals(1,directory.requested.size());assertTrue(ConversionService.snapshot().running);
            directory.release.countDown();drain(old.get());assertFalse(ConversionService.snapshot().running);assertTrue(SavedSession.load(context).state.recoverable);
            other.destroy();directory.fail=true;
            var next=Robolectric.buildService(ConversionService.class).create();
            try{
                next.get().onStartCommand(SavedSession.load(context).resumeRequest(),0,1);drain(next.get());
                assertEquals(1,ConversionService.snapshot().succeeded);assertEquals(1,ConversionService.snapshot().failed);assertFalse(ConversionService.snapshot().running);
                String log=new String(java.nio.file.Files.readAllBytes(ConversionService.batchLogFile(context).toPath()),java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(log.contains("RISORSE ·"));assertTrue(log.contains("dopo rilascio risorse"));
            }finally{next.destroy();}
        }finally{directory.release.countDown();drain(old.get());old.destroy();other.destroy();stateField().set(null,before);SavedSession.clear(context);}
    }
}
