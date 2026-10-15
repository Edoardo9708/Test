package it.edo.codecprobe;

import android.content.*;
import android.os.Looper;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class FailedQueueServiceTest {
    @Test public void actualFailureIsPersistedButExplicitlyIgnoredVideoIsExcluded()throws Exception {
        ServiceController<ConversionService> controller=Robolectric.buildService(ConversionService.class).create();
        ConversionService service=controller.get();
        ConversionService.State before=ConversionService.snapshot();
        Intent beforeRetry=ConversionService.retryRequest(service);
        try{
            Intent task=BatchRetry.select(BatchRetryTest.queue(service),List.of(0,1));
            task.putExtra("omitted",new boolean[]{true,false});
            task.putStringArrayListExtra("outputNames",new ArrayList<>(List.of("zero.mp4","one.mp4")));
            // This URI has no writable directory provider, so the second file fails before any codec runs.
            service.onStartCommand(task,0,1);
            Field field=ConversionService.class.getDeclaredField("worker");field.setAccessible(true);
            ((ExecutorService)field.get(service)).submit(()->{}).get(5,TimeUnit.SECONDS);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            ConversionService.State state=ConversionService.snapshot();assertFalse(state.running);assertEquals(1,state.ignored);assertEquals(1,state.failed);assertEquals(0,state.notSaved);
            assertEquals(1,state.failures.size());assertEquals(2,state.failures.get(0).number);
            assertTrue(state.failures.get(0).details.contains("IOException"));assertFalse(state.failures.get(0).message.isEmpty());
            assertTrue(java.nio.file.Files.readString(ConversionService.batchLogFile(service).toPath()).contains(state.failures.get(0).copyText()));
            Intent retry=BatchRetry.load(service);assertNotNull(retry);assertEquals(List.of("content://input/1"),retry.getStringArrayListExtra("inputs"));
            assertEquals(List.of("content://dirs/tree/one"),retry.getStringArrayListExtra("folders"));
            assertEquals(List.of("one.mp4"),retry.getStringArrayListExtra("requestedOutputNames"));
        }finally{
            controller.destroy();
            Field stored=ConversionService.class.getDeclaredField("state");stored.setAccessible(true);Object actual=stored.get(null);
            for(Field value:ConversionService.State.class.getDeclaredFields()){value.setAccessible(true);value.set(actual,value.get(before));}
            ConversionService.rememberFailures(service,beforeRetry);
        }
    }
}
