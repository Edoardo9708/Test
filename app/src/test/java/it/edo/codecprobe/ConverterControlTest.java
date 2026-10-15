package it.edo.codecprobe;

import android.net.Uri;
import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ConverterControlTest {
    private VideoConverter converter(AtomicBoolean cancel) {
        return new VideoConverter(RuntimeEnvironment.getApplication(),cancel,new VideoConverter.Listener(){
            public void log(String text){}public void progress(int p,String text){}public void timeline(long d,long t){}
        });
    }
    @Test public void sdrAndHdrCancellationCallbacksUseTheSamePauseGate()throws Exception{
        AtomicBoolean cancel=new AtomicBoolean();CountDownLatch paused=new CountDownLatch(1);
        ConversionControl gate=new ConversionControl(cancel,android.os.SystemClock::elapsedRealtime,p->{if(p)paused.countDown();});
        VideoConverter converter=converter(cancel);converter.control=gate;
        HdrTranscoder hdr=new HdrTranscoder(converter::isCancelled,null,null);
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            gate.beginFile(1);gate.pause(true);Future<Boolean> check=worker.submit(hdr::isCancelled);
            assertTrue(paused.await(3,TimeUnit.SECONDS));assertFalse(check.isDone());
            assertTrue(gate.skip(1));assertTrue(check.get(3,TimeUnit.SECONDS));assertTrue(converter.isCancelled());
        }finally{gate.cancel();worker.shutdownNow();}
    }
    @Test public void skipBeforeOpeningInputDeletesWorkingFileAndNeverCreatesOutput()throws Exception{
        AtomicBoolean cancel=new AtomicBoolean();ConversionControl gate=new ConversionControl(cancel,()->0L,p->{});
        VideoConverter converter=converter(cancel);converter.control=gate;gate.beginFile(1);assertTrue(gate.skip(1));
        File working=new File(RuntimeEnvironment.getApplication().getCacheDir(),"conversion-working.mp4");
        Files.write(working.toPath(),new byte[]{1,2,3});
        try {
            converter.convert(Uri.parse("content://missing/input"),Uri.parse("content://missing/folder"),23,"clip.mp4",5,1,true,true);
            fail("Must stop before opening the provider");
        }catch(InterruptedException expected){assertFalse(working.exists());}
        assertFalse(cancel.get()); // File skip is independent of queue cancellation.
    }
    @Test public void cleanupReportsAnUndeletableTemporaryAndStillDeletesOtherFiles()throws Exception{
        File root=Files.createTempDirectory("cleanup-test").toFile();
        File blocked=new File(root,"nonempty");assertTrue(blocked.mkdir());File child=new File(blocked,"child");Files.write(child.toPath(),new byte[]{1});
        File removable=new File(root,"removable");Files.write(removable.toPath(),new byte[]{1});
        try{ConversionCleanup.temporaryFiles(blocked,removable);fail("Must report failed cleanup");}
        catch(ConversionCleanup.Failed expected){assertTrue(expected.getMessage().contains("nonempty"));assertFalse(removable.exists());}
        finally{child.delete();blocked.delete();root.delete();}
    }
}
