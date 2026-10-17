package it.edo.codecprobe;

import java.io.IOException;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

public final class SamsungSefTest {
    // Exact 84-byte sefd box from the uploaded Samsung screen recording. No media or personal data.
    static byte[] screen(){return HexFormat.of().parseHex("00000054736566640000510c1400000053616d73756e675f436170747572655f496e666f53637265656e207265636f7264696e67534546486b000000010000000000510c2c0000002c0000001800000053454654");}
    static final class Item {
        final int type;final String name;final byte[] payload;
        Item(int type,String name,String payload){this(type,name,payload.getBytes(java.nio.charset.StandardCharsets.US_ASCII));}
        Item(int type,String name,byte[] payload){this.type=type;this.name=name;this.payload=payload;}
    }
    static byte[] fixture(int version,Item... items){
        int length=8+20+12*items.length;for(Item item:items)length+=8+item.name.length()+item.payload.length;
        ByteBuffer out=ByteBuffer.allocate(length);out.putInt(length).put("sefd".getBytes(java.nio.charset.StandardCharsets.US_ASCII));out.order(ByteOrder.LITTLE_ENDIAN);
        int[] offsets=new int[items.length],sizes=new int[items.length];
        for(int n=0;n<items.length;n++){
            Item item=items[n];offsets[n]=out.position();byte[] name=item.name.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            out.putShort((short)0).putShort((short)item.type).putInt(name.length).put(name).put(item.payload);sizes[n]=out.position()-offsets[n];
        }
        int directory=out.position();out.put("SEFH".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(version).putInt(items.length);
        for(int n=0;n<items.length;n++)out.putShort((short)0).putShort((short)items[n].type).putInt(directory-offsets[n]).putInt(sizes[n]);
        out.putInt(12+12*items.length).put("SEFT".getBytes(java.nio.charset.StandardCharsets.US_ASCII));return out.array();
    }
    static byte[] descriptive(){return fixture(106,new Item(0x0a01,"Image_UTC_Data","1745758399000"),
        new Item(0x0aa1,"MCC_Data","222"),new Item(0x0e41,"Video_Edited_UTC_Offset","+0200"),
        new Item(0x0c61,"Camera_Capture_Mode_Info","1"),new Item(0x0910,"Front_Cam_Selfie_Info","Front_Cam_Selfie_Info"),
        new Item(0x08e0,"Panorama_Shot_Info","PanoramaShot"),new Item(0x09e0,"Burst_Shot_Info","489489125"),
        new Item(0xa050,"Jpeg360_2D_Info","Jpeg3602D"));}
    // Reconstructed exactly from v0.57's complete 116-byte diagnostic (no media data needed).
    static byte[] slowMotion(){return fixture(105,new Item(0x7ffe,"Invalid_Data",new byte[13]),new Item(0x0890,"SlowMotion_Data","0:1637:4"));}
    private static String failure(byte[] b)throws Exception{try{SamsungSef.validate(b,123456);}catch(IOException expected){return expected.getMessage();}throw new AssertionError("Invalid SEF accepted");}
    private static void reject(byte[] b)throws Exception{try{SamsungSef.validate(b);}catch(IOException expected){return;}throw new AssertionError("Invalid SEF accepted");}
    public static void main(String[] args)throws Exception{
        byte[] original=screen(),copy=original.clone();String result=SamsungSef.validate(original);
        if(!result.contains("107")||!result.contains("Samsung_Capture_Info=1")||!Arrays.equals(original,copy))throw new AssertionError(result);
        byte[] corrupt=original.clone();corrupt[56]=108;reject(corrupt);
        corrupt=original.clone();ByteBuffer.wrap(corrupt).order(ByteOrder.LITTLE_ENDIAN).putInt(68,45);reject(corrupt);
        corrupt=original.clone();corrupt[36]=0;reject(corrupt);
        corrupt=original.clone();corrupt[10]=0;reject(corrupt);
        corrupt=original.clone();corrupt[16]='X';reject(corrupt);
        corrupt=original.clone();corrupt[64]=1;reject(corrupt);
        corrupt=original.clone();ByteBuffer.wrap(corrupt).order(ByteOrder.LITTLE_ENDIAN).putInt(72,45);reject(corrupt);
        byte[] slow=slowMotion(),slowCopy=slow.clone();String slowReport=SamsungSef.validate(slow);
        if(slow.length!=116||!Arrays.equals(slow,slowCopy)||!slowReport.contains("2 copiabili · 0 bloccati")||!slowReport.contains("velocità=1/8"))throw new AssertionError(slowReport);
        SamsungSefDirectory parsed=SamsungSefDirectory.parse(slow);
        String[] hashes={"dd46c3eebb1884ff3b5258c0a2fc9398e560a29e0780d4b53869b6254aa46a96","049b5e2408d9f0a63e7b51377da44897064f47fdc7188a4358b72b92c9842488"};
        for(int i=0;i<2;i++){SamsungSefDirectory.Record r=parsed.records.get(i);byte[] payload=Arrays.copyOfRange(r.bytes,r.payload,r.end);String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(payload));if(!hash.equals(hashes[i]))throw new AssertionError("Diagnostic payload hash mismatch");}

        if(parsed.start!=72||parsed.records.get(0).payload!=28||parsed.records.get(1).payload!=64)throw new AssertionError("Fixture does not match reported offsets");
        SamsungSef.validate(fixture(105,new Item(0x0890,"SlowMotion_Data","0:100:1*100:300:2*300:1637:4")));
        SamsungSef.validate(fixture(105,new Item(0x0890,"SlowMotion_Data","0:100:31")));
        for(String value:new String[]{"","0:0:4","5:4:4","-1:3:4","0:10:0","0:10:32","0:10:4*9:20:4","0:10:4*","0:10:4\u0000","0:10:4:5","0:10:x","9223372036854775808:10:4","0:9223372036854775807:4"})
            reject(fixture(105,new Item(0x0890,"SlowMotion_Data",value)));
        for(int length:new int[]{0,4097})reject(fixture(105,new Item(0x7ffe,"Invalid_Data",new byte[length])));
        for(int i=0;i<13;i++){byte[] invalid=new byte[13];invalid[i]=1;reject(fixture(105,new Item(0x7ffe,"Invalid_Data",invalid)));}
        reject(fixture(105,new Item(0x7fff,"Invalid_Data",new byte[13])));
        reject(fixture(105,new Item(0x0891,"SlowMotion_Data","0:1637:4")));
        byte[] descriptive=descriptive(),untouched=descriptive.clone();String approved=SamsungSef.validate(descriptive);
        if(!approved.contains("8 copiabili · 0 bloccati")||!Arrays.equals(descriptive,untouched))throw new AssertionError(approved);
        for(int version:new int[]{103,105,106,107})SamsungSef.validate(fixture(version,new Item(0x0aa1,"MCC_Data","222")));
        reject(fixture(108,new Item(0x0aa1,"MCC_Data","222")));
        reject(fixture(107,new Item(0x0aa1,"MCC_Data","222\u0000")));
        reject(fixture(107,new Item(0x0a01,"Image_UTC_Data","9223372036854775808")));
        reject(fixture(107,new Item(0x0e41,"Video_Edited_UTC_Offset","+1460")));
        reject(fixture(107,new Item(0x0e41,"Video_Edited_UTC_Offset","+1401")));
        String diagnostic=failure(fixture(107,new Item(0x0ba1,"Original_Path_Hash_Key",new byte[]{0,1,2,3}),
            new Item(0x0ba1,"PhotoEditor_Re_Edit_Data","{\"originalPath\":\"/data/sec/photoeditor/source.mp4\"}"),
            new Item(0x0aa1,"MCC_Data","222"),new Item(0x7fff,"Future_Record","future")));
        for(String expected:new String[]{"0x0BA1 (2977)","Original_Path_Hash_Key","PhotoEditor_Re_Edit_Data","Future_Record",
            "1 copiabili · 3 bloccati","Offset assoluto nel file","SHA-256 payload","00 01 02 03","\\x00\\x01\\x02\\x03","possibile JSON","Motivo del blocco"})
            if(!diagnostic.contains(expected))throw new AssertionError("Missing diagnostic: "+expected+"\n"+diagnostic);
        String mismatch=failure(fixture(107,new Item(0x0ba1,"MCC_Data","222")));
        if(!mismatch.contains("tipo inatteso")||!mismatch.contains("0x0AA1"))throw new AssertionError(mismatch);
        String variant=failure(fixture(107,new Item(0x0c51,"Samsung_Capture_Info","different capture")));
        if(!variant.contains("variante supportata"))throw new AssertionError(variant);
        byte[] huge=new byte[1024*1024];Arrays.fill(huge,(byte)'Z');
        String bounded=failure(fixture(107,new Item(0x0ba1,"Original_Path_Hash_Key",huge)));
        if(bounded.length()>10000||!bounded.contains("1048576 byte")||!bounded.contains("Ultimi 64 byte"))throw new AssertionError("Unbounded or incomplete diagnostics");
        Item[] many=new Item[100];for(int n=0;n<many.length;n++)many[n]=new Item(0x0ba1,"deco_text_bitmap",new byte[]{0,1});
        String all=failure(fixture(107,many));
        if(all.length()>35000||!all.contains("0 copiabili · 100 bloccati")||!all.contains("primi 64 record")||!all.contains("Altri 96 record"))throw new AssertionError("Record diagnostics not bounded");
        // Invalid size/offset data must never escape as an unchecked bounds exception.
        Random random=new Random(55);for(int n=0;n<500;n++){
            byte[] fuzz=original.clone();int at=random.nextInt(fuzz.length);fuzz[at]^=(byte)(1+random.nextInt(255));
            try{SamsungSef.validate(fuzz);}catch(IOException expected){}
        }
        if(args.length>0){byte[] real=Files.readAllBytes(Path.of(args[0]));if(!Arrays.equals(real,original))throw new AssertionError("Fixture differs from actual file");SamsungSef.validate(real);}
        System.out.println("OK Samsung SEF: exact screen fixture, 8 additional descriptive schemas, standard slow-motion/zero-template schemas, four directory versions, full record inventory, type/name/variant diagnostics, bounded binary/JSON previews, relocation and mutation checks");
    }
}
