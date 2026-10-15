package it.edo.codecprobe;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Cross-language integration: actual reordered x265 Main10 packets + Samsung HDR10+ fixtures. */
public final class HdrNativeMetadataCheck {
    public static void main(String[] args)throws Exception{
        if(args.length>0){
            List<HdrSei.Message> before=HdrSei.read(Files.readAllBytes(Path.of("hdr10-preserved.hevc"))),after=HdrSei.read(Files.readAllBytes(Path.of("hdr10-remuxed.hevc")));
            HdrMetadataTest.require(before.size()==after.size()&&Arrays.equals(HdrSei.digest(before),HdrSei.digest(after)));
            System.out.println("OK MP4 remux: every HDR SEI payload byte preserved");return;
        }
        File index=new File("hdr10-native-metadata.bin");
        List<byte[]> result=new ArrayList<>();List<Long> pts=new ArrayList<>();
        try(HdrMetadataLedger ledger=new HdrMetadataLedger(index,Collections.emptyList());DataInputStream nativeData=new DataInputStream(new FileInputStream("hdr10-bframes.hevc"));BufferedReader nativeIndex=Files.newBufferedReader(Path.of("hdr10-bframes.index"));OutputStream out=new FileOutputStream("hdr10-preserved.hevc")){
            for(int i=0;i<72;i++){ledger.queued(i*41708L,HdrMetadataTest.au(HdrMetadataTest.fixture(i)));ledger.beforeEncode(i*41708L);}
            String line;while((line=nativeIndex.readLine())!=null){String[] fields=line.split(" ");int length=Integer.parseInt(fields[1]);byte[] packet=new byte[length];nativeData.readFully(packet);
                if(fields[0].equals("H")){HdrHevcInfo.requireMain10(packet);out.write(HdrSei.replace(packet,Collections.emptyList()));}
                else {long time=Long.parseLong(fields[0]);byte[] preserved=ledger.packet(packet,time);result.add(preserved);pts.add(time);out.write(preserved);}
            }
            ledger.finish();HdrMetadataTest.require(nativeData.read()==-1&&result.size()==72);
        }
        try(HdrMetadataLedger.Verifier verifier=new HdrMetadataLedger.Verifier(index,Collections.emptyList())){for(int i=0;i<result.size();i++)verifier.sample(pts.get(i),result.get(i));HdrMetadataTest.require(verifier.finish()==72);}
        Files.delete(index.toPath());System.out.println("OK real x265 Main10: HDR10+ per PTS survives B-frame reorder; 72 exact SHA-256 matches");
    }
}
