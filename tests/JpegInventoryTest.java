package it.edo.codecprobe;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Streaming parser regression including metadata between progressive scans and appended payloads. */
public final class JpegInventoryTest {
    static byte[] bytes(String value){return value.getBytes(StandardCharsets.ISO_8859_1);}
    static void segment(ByteArrayOutputStream out,int marker,byte[] data)throws IOException{out.write(255);out.write(marker);out.write((data.length+2)>>>8);out.write(data.length+2);out.write(data);}
    static ByteArrayOutputStream start()throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(bytes("\377\330"));return out;}
    static void frame(ByteArrayOutputStream out)throws IOException{segment(out,0xc2,new byte[]{8,0,32,0,48,1,1,0x11,0});}
    static void scan(ByteArrayOutputStream out)throws IOException{segment(out,0xda,new byte[]{1,1,0,0,63,0});out.write(bytes("\023\377\000\024\377\320\025"));}
    public static byte[] fixture()throws IOException{
        ByteArrayOutputStream out=start();segment(out,0xe1,bytes("Exif\0\0original\0GPS"));segment(out,0xe2,bytes("ICC_PROFILE\0\002\002second"));segment(out,0xe7,bytes("custom\0opaque"));frame(out);scan(out);
        segment(out,0xfe,bytes("between scans"));segment(out,0xe2,bytes("ICC_PROFILE\0\001\002first"));scan(out);out.write(bytes("\377\331appended movie bytes"));return out.toByteArray();
    }
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static JpegInventory.Info read(byte[] data)throws IOException{return JpegInventory.read(new ByteArrayInputStream(data));}
    static void rejects(byte[] data)throws IOException{try{read(data);throw new AssertionError("Malformed JPEG accepted");}catch(IOException expected){}}
    public static void main(String[] args)throws Exception{
        byte[] source=fixture();JpegInventory.Info info=read(source);
        check(info.width==48&&info.height==32&&info.precision==8&&info.components==1&&info.scans==2,"Frame/scans");
        check(info.bytes==source.length&&info.trailingBytes==20,"Full file and trailing sizes");
        check(info.sha256.equals(JpegInventory.hex(JpegInventory.digest().digest(source))),"Full-file SHA");
        check(info.trailingSha256.equals(JpegInventory.hex(JpegInventory.digest().digest(bytes("appended movie bytes")))),"Trailing SHA");
        check(info.blocks.size()==5&&info.count("EXIF")==1&&info.count("ICC")==2&&info.iccComplete,"All metadata, including inter-scan segments");
        for(JpegInventory.Block block:info.blocks){byte[] payload=Base64.getDecoder().decode(block.base64());check(Arrays.equals(payload,Arrays.copyOfRange(source,(int)block.offset,(int)block.offset+block.size())),"Original payload offset/bytes");check(block.sha256.equals(JpegInventory.hex(JpegInventory.digest().digest(payload))),"Payload SHA");}
        byte[] duplicate=source.clone();int offset=(int)info.blocks.get(4).offset;duplicate[offset+12]=2;check(!read(duplicate).iccComplete,"Duplicate ICC part");duplicate[offset+13]=3;check(!read(duplicate).iccComplete,"Mismatched ICC count");
        for(int i=0;i<source.length-20;i++)rejects(Arrays.copyOf(source,i));
        rejects(bytes("not an image"));ByteArrayOutputStream noScan=start();frame(noScan);noScan.write(bytes("\377\331"));rejects(noScan.toByteArray());
        ByteArrayOutputStream huge=start();byte[] payload=new byte[65533];for(int i=0;i<257;i++)segment(huge,0xe1,payload);rejects(huge.toByteArray());
        Thread.currentThread().interrupt();try{read(source);throw new AssertionError("Cancellation ignored");}catch(InterruptedIOException expected){}finally{Thread.interrupted();}
        if(args.length>0){try(InputStream input=Files.newInputStream(Path.of(args[0]))){JpegInventory.Info real=JpegInventory.read(input);System.out.println("Real JPEG: "+real.width+"x"+real.height+", "+real.bytes+" bytes, metadata="+real.blocks.size()+", EXIF="+real.count("EXIF")+", XMP="+real.count("XMP")+", ICC="+real.count("ICC")+", trailing="+real.trailingBytes+", SHA256="+real.sha256);}}
        System.out.println("OK JPEG inventory: streaming scans, original metadata bytes/hashes, ICC sequencing, trailing data, truncation, limits and cancellation");
    }
}
