package it.edo.codecprobe;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConversionControlBoundaryTest {
    @Test public void cancellationBeforeTheFirstFileNeverEntersPauseOrAcceptsSkip() {
        AtomicBoolean cancelled=new AtomicBoolean();ConversionControl gate=new ConversionControl(cancelled,()->0L,p->fail("No worker was paused"));
        gate.pause(true);gate.cancel();assertTrue(gate.checkpoint());assertFalse(gate.finishFile());assertFalse(gate.skip(1));
    }
    @Test public void completedFileRejectsLateSkipWithoutCancellingTheNextFile() {
        ConversionControl gate=new ConversionControl(new AtomicBoolean(),()->0L,p->{});
        assertFalse(gate.skip(1));gate.beginFile(1);assertTrue(gate.finishFile());assertFalse(gate.skip(1));
        gate.endFile();gate.beginFile(2);assertFalse(gate.skip(1));assertFalse(gate.stopping());assertTrue(gate.finishFile());
    }
    @Test public void interruptingAPausedWorkerCancelsAndRestoresItsInterruptStatus()throws Exception {
        CountDownLatch paused=new CountDownLatch(1);AtomicBoolean cancellation=new AtomicBoolean();
        ConversionControl gate=new ConversionControl(cancellation,()->0L,p->{if(p)paused.countDown();});
        CompletableFuture<Boolean> outcome=new CompletableFuture<>();
        Thread worker=new Thread(()->{try{boolean stopped=gate.checkpoint();outcome.complete(stopped&&Thread.currentThread().isInterrupted());}catch(Throwable error){outcome.completeExceptionally(error);}},"test-paused-worker");
        gate.beginFile(1);gate.pause(true);
        try { worker.start();assertTrue(paused.await(3,TimeUnit.SECONDS));worker.interrupt();assertTrue(outcome.get(3,TimeUnit.SECONDS));assertTrue(cancellation.get()); }
        finally { gate.cancel();worker.interrupt();worker.join(3000);assertFalse("Worker leaked",worker.isAlive()); }
    }
}
