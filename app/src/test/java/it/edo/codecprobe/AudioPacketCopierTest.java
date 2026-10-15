package it.edo.codecprobe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AudioPacketCopierTest {
    static final class Packets implements AudioPacketCopier.Source {
        final long[] times;final byte[][] bytes;int index;
        int track=2,readAdjustment;boolean protectedAudio;
        Packets(long... times){this.times=times;bytes=new byte[times.length][];
            for(int n=0;n<times.length;n++)bytes[n]=new byte[]{(byte)n,3,8,9};}
        public int trackIndex(){return index<times.length?track:-1;}
        public long size(){return index<times.length?bytes[index].length:-1;}
        public long timeUs(){return index<times.length?times[index]:-1;}
        public boolean encrypted(){return protectedAudio;}
        public int read(ByteBuffer buffer){buffer.put(bytes[index]);return bytes[index].length+readAdjustment;}
        public void advance(){index++;}
    }
    @Test public void negativePrerollZeroAndPositivePacketsKeepEveryByteAndTimestamp()throws Exception {
        Packets source=new Packets(-161133,-114694,-68254,-21814,24625,71065);
        List<Long> written=new ArrayList<>();List<byte[]> output=new ArrayList<>();List<String> log=new ArrayList<>();
        AudioPacketCopier.copy(source,2,(buffer,size,time)->{
            written.add(time);byte[] bytes=new byte[size];buffer.get(bytes);output.add(bytes);
        },()->{},log::add);
        assertEquals(Arrays.asList(-161133L,-114694L,-68254L,-21814L,24625L,71065L),written);
        for(int n=0;n<source.bytes.length;n++)assertArrayEquals(source.bytes[n],output.get(n));
        assertTrue(log.stream().anyMatch(line->line.contains("negative_pts=4")));
        assertEquals(6,log.stream().filter(line->line.startsWith("DIAGNOSTICA PRIMI TIMESTAMP")).count());
    }
    @Test public void minusOneIsAValidTimestampWhenSampleExists()throws Exception {
        Packets source=new Packets(-40000,-1,0,20000);List<Long> written=new ArrayList<>();
        AudioPacketCopier.copy(source,2,(b,n,t)->written.add(t),()->{},s->{});
        assertEquals(Arrays.asList(-40000L,-1L,0L,20000L),written);
    }
    @Test public void positiveStartingTimeIsNotRebased()throws Exception {
        List<Long> written=new ArrayList<>();
        AudioPacketCopier.copy(new Packets(80000,120000),2,(b,n,t)->written.add(t),()->{},s->{});
        assertEquals(Arrays.asList(80000L,120000L),written);
    }
    @Test public void diagnosticsAreBoundedButCopyContinues()throws Exception {
        long[] times=new long[100];for(int n=0;n<times.length;n++)times[n]=n*20000L-40000;
        List<String> log=new ArrayList<>();List<Long> written=new ArrayList<>();
        AudioPacketCopier.copy(new Packets(times),2,(b,n,t)->written.add(t),()->{},log::add);
        assertEquals(100,written.size());
        assertEquals(12,log.stream().filter(line->line.startsWith("DIAGNOSTICA PRIMI TIMESTAMP")).count());
    }
    @Test public void emptyAndMalformedSourcesAreErrorsInsteadOfSilentAudioLoss()throws Exception {
        assertFailure(new Packets(),"realmente senza pacchetti");
        assertFailure(new Packets(-10,-10),"non crescente");
        assertFailure(new Packets(10,0),"non crescente");
        Packets protectedAudio=new Packets(0);protectedAudio.protectedAudio=true;assertFailure(protectedAudio,"protetto");
        Packets incomplete=new Packets(-20);incomplete.readAdjustment=-1;assertFailure(incomplete,"incompleta");
        Packets wrongTrack=new Packets(0);wrongTrack.track=3;assertFailure(wrongTrack,"traccia diversa");
        Packets excessive=new Packets(0);excessive.bytes[0]=new byte[AudioPacketCopier.MAX_PACKET_BYTES+1];assertFailure(excessive,"Dimensione");
    }
    private void assertFailure(Packets source,String expected)throws Exception {
        try{AudioPacketCopier.copy(source,2,(b,n,t)->{},()->{},s->{});fail("Expected "+expected);}
        catch(IOException error){assertTrue(error.toString(),error.getMessage().contains(expected));}
    }
    @Test public void cancellationStopsBeforeReadingNextPacket()throws Exception {
        Packets source=new Packets(-20,-10,0);List<Long> written=new ArrayList<>();
        try{
            AudioPacketCopier.copy(source,2,(b,n,t)->written.add(t),()->{
                if(!written.isEmpty())throw new InterruptedException("cancelled");
            },s->{});fail();
        }catch(InterruptedException expected){}
        assertEquals(List.of(-20L),written);assertEquals(1,source.index);
    }
    @Test public void unsupportedMuxerReportsTheOriginalNegativeTimestamp()throws Exception {
        try{
            AudioPacketCopier.copy(new Packets(-12345,0),2,(b,n,t)->{throw new IllegalArgumentException("unsupported");},()->{},s->{});fail();
        }catch(IOException error){assertTrue(error.getMessage().contains("pts_us=-12345"));assertTrue(error.getMessage().contains("nessun timestamp modificato"));}
    }
}
