package it.edo.codecprobe;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

public final class HdrMetadataTest {
    static void require(boolean value){if(!value)throw new AssertionError();}
    static void fails(Runnable task){try{task.run();}catch(IllegalArgumentException e){return;}throw new AssertionError("Expected rejection");}
    static byte[] hex(String value){return HexFormat.of().parseHex(value);}
    static List<HdrSei.Message> fixture(int index){
        // Actual 64-byte T.35 payloads from the Samsung P010 diagnostic, kept uninterpreted.
        String[] payloads={"b5003c0001040140000c810d4c497040e200bee4080000289bf05003fcc801919004125809b2d014a2f8250b189bf00040781364d37bee5cd35ddfb9aecbc340","b5003c0001040140000c815d244eb640e200c0e4080000289bc85003fcc801919004125809b2d014a2f8250b189bc800407c13a4d37bee5cd35ddfb9aecbc340"};
        List<HdrSei.Message> out=new ArrayList<>();if(index==0){byte[] mastering=hex("84d03e8033c286c41d4c0bb83d134042000003e800000032");out.add(new HdrSei.Message(137,mastering));out.add(new HdrSei.Message(144,hex("03e800c8")));}out.add(new HdrSei.Message(4,hex(payloads[index%2])));return out;
    }
    static byte[] au(List<HdrSei.Message> values){return HdrSei.annexB(Arrays.asList(HdrSei.encode(values),new byte[]{2,1,(byte)128}));}
    public static void main(String[] args)throws Exception{
        List<HdrSei.Message> values=fixture(0);byte[] packet=au(values);require(Arrays.equals(HdrSei.digest(values),HdrSei.digest(HdrSei.read(packet))));
        // Length-prefixed access units, both NAL start-code lengths, and emulation prevention.
        List<byte[]> nals=HdrSei.nals(packet);
        for(int size=1;size<=4;size++){ByteArrayOutputStream out=new ByteArrayOutputStream();for(byte[] n:nals){for(int b=size-1;b>=0;b--)out.write((n.length>>>(8*b))&255);out.write(n);}require(Arrays.equals(HdrSei.digest(values),HdrSei.digest(HdrSei.read(out.toByteArray()))));}
        byte[] escaped=new byte[300];for(int i=0;i<escaped.length;i++)escaped[i]=(byte)(i%4);HdrSei.Message large=new HdrSei.Message(4,Arrays.copyOf(fixture(1).get(0).payload,400));System.arraycopy(escaped,0,large.payload,70,300);
        require(Arrays.equals(HdrSei.digest(Collections.singletonList(large)),HdrSei.digest(HdrSei.read(au(Collections.singletonList(large))))));
        require(Arrays.equals(HdrSei.digest(fixture(1)),HdrSei.digest(HdrSei.read(HdrSei.replace(packet,fixture(1))))));
        fails(()->HdrSei.read(au(Collections.singletonList(new HdrSei.Message(5,new byte[16])))));
        fails(()->HdrSei.read(au(Collections.singletonList(new HdrSei.Message(4,new byte[16])))));
        fails(()->HdrSei.read(new byte[]{0,0,0,5,78,1}));
        byte[] ambiguous=new byte[260];ambiguous[2]=1;ambiguous[4]=78;ambiguous[5]=1;fails(()->HdrSei.nals(ambiguous));
        fails(()->HdrSei.merge(fixture(0),fixture(1)));
        for(int cut=0;cut<8;cut++){final byte[] truncated=Arrays.copyOf(packet,packet.length-8-cut);fails(()->HdrSei.read(truncated));}
        File index=Files.createTempFile("hdr-ledger",".bin").toFile();
        try{
            try(HdrMetadataLedger ledger=new HdrMetadataLedger(index,Collections.emptyList())){
                int[] order={0,3,1,2};for(int i:order)ledger.queued(i*33333L,au(fixture(i)));for(int i=0;i<4;i++)ledger.beforeEncode(i*33333L);
                for(int i:order){byte[] result=ledger.packet(new byte[]{0,0,1,2,1,(byte)128},i*33333L);require(Arrays.equals(HdrSei.digest(fixture(i)),HdrSei.digest(HdrSei.read(result))));}ledger.finish();
            }
            try(HdrMetadataLedger.Verifier verifier=new HdrMetadataLedger.Verifier(index,Collections.emptyList())){for(int i:new int[]{0,3,1,2})verifier.sample(i*33333L+10,au(fixture(i)));require(verifier.finish()==4);}
            try(HdrMetadataLedger.Verifier verifier=new HdrMetadataLedger.Verifier(index,Collections.emptyList())){try{verifier.sample(0,au(fixture(1)));throw new AssertionError("Lost metadata accepted");}catch(IOException expected){require(expected.getMessage().contains("METADATI HDR"));}}
            try(HdrMetadataLedger.Verifier verifier=new HdrMetadataLedger.Verifier(index,Collections.emptyList())){verifier.sample(0,au(fixture(0)));try{verifier.finish();throw new AssertionError("Missing frames accepted");}catch(IOException expected){}}
        }finally{Files.delete(index.toPath());}
        index=Files.createTempFile("hdr-csd",".bin").toFile();
        try{
            List<HdrSei.Message> staticHeaders=new ArrayList<>(fixture(0).subList(0,2)),dynamic=fixture(1);
            try(HdrMetadataLedger ledger=new HdrMetadataLedger(index,staticHeaders)){ledger.queued(0,au(dynamic));ledger.beforeEncode(0);ledger.packet(new byte[]{0,0,1,2,1,(byte)128},0);ledger.finish();}
            try(HdrMetadataLedger.Verifier verifier=new HdrMetadataLedger.Verifier(index,staticHeaders)){verifier.sample(0,au(dynamic));require(verifier.finish()==1);}
        }finally{Files.delete(index.toPath());}
        System.out.println("OK HDR SEI: raw payloads, length prefixes, escaping, rejected unsupported/truncated SEI, B-frame timestamp joins, tampering/missing frames");
    }
}
