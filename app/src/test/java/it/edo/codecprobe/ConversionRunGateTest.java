package it.edo.codecprobe;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConversionRunGateTest {
    @Test public void concurrentStartsHaveOneOwnerAndOldOwnerCannotReleaseItsSuccessor()throws Exception{
        ConversionRunGate gate=new ConversionRunGate();ExecutorService workers=Executors.newFixedThreadPool(8);
        CountDownLatch ready=new CountDownLatch(8),start=new CountDownLatch(1),attempted=new CountDownLatch(8),release=new CountDownLatch(1);
        AtomicInteger entered=new AtomicInteger();
        try{
            for(int n=0;n<8;n++)workers.submit(()->{
                ready.countDown();try{start.await();long token=gate.acquire();if(token!=0)entered.incrementAndGet();attempted.countDown();
                    if(token!=0){release.await();gate.release(token);}}
                catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
            });
            assertTrue(ready.await(3,TimeUnit.SECONDS));start.countDown();assertTrue(attempted.await(3,TimeUnit.SECONDS));assertEquals(1,entered.get());assertEquals(0,gate.acquire());
            release.countDown();workers.shutdown();assertTrue(workers.awaitTermination(3,TimeUnit.SECONDS));
            long first=gate.acquire();assertTrue(first>0);gate.release(first);long next=gate.acquire();assertTrue(next>first);
            gate.release(first);assertTrue(gate.owns(next));assertFalse(gate.owns(first));gate.release(next);assertTrue(gate.acquire()>next);
        }finally{start.countDown();release.countDown();workers.shutdownNow();}
    }
}
