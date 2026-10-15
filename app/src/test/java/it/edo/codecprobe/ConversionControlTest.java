package it.edo.codecprobe;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConversionControlTest {
    @Test public void pauseWaitsAndExcludesTimeFromDecoderWatchdog() throws Exception {
        AtomicLong clock=new AtomicLong(1000);
        CountDownLatch entered=new CountDownLatch(1);
        List<Boolean> events=Collections.synchronizedList(new ArrayList<>());
        ConversionControl gate=new ConversionControl(new AtomicBoolean(),clock::get,p->{events.add(p);if(p)entered.countDown();});
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            gate.beginFile(1);gate.pause(true);
            Future<Boolean> result=worker.submit(gate::checkpoint);
            assertTrue(entered.await(3,TimeUnit.SECONDS));assertFalse(result.isDone());
            clock.set(121000);assertEquals(120000,gate.pausedMillis());
            assertEquals(1000,clock.get()-gate.pausedMillis());
            gate.pause(false);assertFalse(result.get(3,TimeUnit.SECONDS));
            assertEquals(List.of(true,false),events);assertEquals(120000,gate.pausedMillis());
            clock.set(122000);assertEquals(2000,clock.get()-gate.pausedMillis());
        }finally{gate.cancel();worker.shutdownNow();}
    }
    @Test public void skippingWhilePausedKeepsNextFilePausedAndRejectsOldFileCommands() throws Exception {
        BlockingQueue<Boolean> events=new LinkedBlockingQueue<>();
        ConversionControl gate=new ConversionControl(new AtomicBoolean(),()->0L,events::add);
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            gate.beginFile(1);gate.pause(true);Future<Boolean> first=worker.submit(gate::checkpoint);
            assertEquals(Boolean.TRUE,events.poll(3,TimeUnit.SECONDS));assertTrue(gate.skip(1));
            assertTrue(first.get(3,TimeUnit.SECONDS));assertEquals(Boolean.FALSE,events.poll(3,TimeUnit.SECONDS));
            assertTrue(gate.pauseRequested());gate.endFile();gate.beginFile(2);
            Future<Boolean> next=worker.submit(gate::checkpoint);assertEquals(Boolean.TRUE,events.poll(3,TimeUnit.SECONDS));
            assertFalse(gate.skip(1));assertFalse(next.isDone());gate.pause(false);
            assertFalse(next.get(3,TimeUnit.SECONDS));assertEquals(Boolean.FALSE,events.poll(3,TimeUnit.SECONDS));
            assertTrue(gate.finishFile());assertFalse(gate.skip(2));
        }finally{gate.cancel();worker.shutdownNow();}
    }
    @Test public void cancellationWakesPausedWorkerAndCannotCommitAFile() throws Exception {
        CountDownLatch entered=new CountDownLatch(1);AtomicBoolean cancel=new AtomicBoolean();
        ConversionControl gate=new ConversionControl(cancel,()->0L,p->{if(p)entered.countDown();});
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            gate.beginFile(1);gate.pause(true);Future<Boolean> done=worker.submit(gate::finishFile);
            assertTrue(entered.await(3,TimeUnit.SECONDS));gate.cancel();
            assertFalse(done.get(3,TimeUnit.SECONDS));assertTrue(cancel.get());assertFalse(gate.skip(1));
        }finally{gate.cancel();worker.shutdownNow();}
    }
    @Test public void resumeCommandDoesNotWaitForWorkerAcknowledgementLogging() throws Exception {
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        ConversionControl gate=new ConversionControl(new AtomicBoolean(),()->0L,p->{
            if(p){entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}}
        });
        ExecutorService worker=Executors.newSingleThreadExecutor(),commands=Executors.newSingleThreadExecutor();
        try {
            gate.beginFile(1);gate.pause(true);Future<Boolean> done=worker.submit(gate::checkpoint);
            assertTrue(entered.await(3,TimeUnit.SECONDS));
            commands.submit(()->gate.pause(false)).get(3,TimeUnit.SECONDS);
            assertFalse(done.isDone());release.countDown();assertFalse(done.get(3,TimeUnit.SECONDS));
        }finally{release.countDown();gate.cancel();worker.shutdownNow();commands.shutdownNow();}
    }
}
