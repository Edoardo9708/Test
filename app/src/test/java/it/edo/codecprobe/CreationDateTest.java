package it.edo.codecprobe;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CreationDateTest {
    @Test public void onlyMissingAndZero1904DatesUseAndroidFallback()throws Exception {
        assertTrue(CreationDatePolicy.needsFallback(0,null));
        assertTrue(CreationDatePolicy.needsFallback(0,"19040101T000000.000Z"));
        assertFalse(CreationDatePolicy.needsFallback(1234,null));
        assertFalse(CreationDatePolicy.needsFallback(0,"20241012T120000.000Z"));
        assertFalse(CreationDatePolicy.needsFallback(0,"19651012T120000.000Z"));
        assertFalse(CreationDatePolicy.needsFallback(0,"unknown format"));
        assertEquals(0,CreationDatePolicy.fromAddedSeconds(0));
        assertEquals(0,CreationDatePolicy.fromAddedSeconds(-1));
        assertEquals(3782844800L,CreationDatePolicy.fromAddedSeconds(1700000000));
        assertEquals("2023-11-14T22:13:20Z",CreationDatePolicy.displayMp4(3782844800L));
        try{CreationDatePolicy.fromAddedSeconds(Long.MAX_VALUE);fail();}catch(IOException expected){}
    }
    static byte[] box(String name,byte[] body){ByteBuffer b=ByteBuffer.allocate(body.length+8);b.putInt(b.capacity()).put(name.getBytes(StandardCharsets.ISO_8859_1)).put(body);return b.array();}
    static byte[] join(byte[]... parts)throws Exception {ByteArrayOutputStream bytes=new ByteArrayOutputStream();for(byte[] part:parts)bytes.write(part);return bytes.toByteArray();}
    static byte[] header(String type,int version,long created,long changed){
        byte[] body=new byte[type.equals("mdhd")?36:112];body[0]=(byte)version;
        ByteBuffer b=ByteBuffer.wrap(body);if(version==1){b.putLong(4,created);b.putLong(12,changed);}else{b.putInt(4,(int)created);b.putInt(8,(int)changed);}
        if(type.equals("mdhd"))b.putShort(version==1?32:20,(short)0x15c7);
        return box(type,body);
    }
    static byte[] movie(int version,long created,long changed)throws Exception {
        byte[] handler=new byte[24];System.arraycopy("vide".getBytes(StandardCharsets.US_ASCII),0,handler,8,4);
        return join(box("ftyp",new byte[16]),box("mdat",new byte[]{1,3,5,7}),box("moov",join(
            header("mvhd",version,created,changed),box("trak",join(header("tkhd",version,created,changed),
                box("mdia",join(header("mdhd",version,created,changed),box("hdlr",handler))))),
            box("udta",box("name","original title".getBytes(StandardCharsets.UTF_8))))));
    }
    static List<Long> field(byte[] file,String type,int index){
        List<Long> out=new ArrayList<>();fields(file,0,file.length,type,index,out);return out;
    }
    static void fields(byte[] bytes,int start,int end,String type,int index,List<Long> out){
        ByteBuffer b=ByteBuffer.wrap(bytes);
        for(int at=start;at<end;){int size=b.getInt(at);String name=new String(bytes,at+4,4,StandardCharsets.ISO_8859_1);
            if(name.equals(type)){int version=bytes[at+8]&255,width=version==1?8:4,offset=at+12+index*width;out.add(width==8?b.getLong(offset):Integer.toUnsignedLong(b.getInt(offset)));}
            if(Set.of("moov","trak","mdia").contains(name))fields(bytes,at+8,at+size,type,index,out);
            at+=size;
        }
    }
    @Test public void roundTripFillsOnlyMissingCreationDatesAndPreservesMediaAndOtherMetadata()throws Exception {
        Path input=Files.createTempFile("date-source-",".mp4"),output=Files.createTempFile("date-result-",".mp4");
        long fallback=CreationDatePolicy.fromAddedSeconds(1700000000);
        try{
            for(int version:new int[]{0,1})for(long original:new long[]{0,1234}){
                Files.write(input,movie(version,original,77));byte[] encoded=movie(version,99,100);Files.write(output,encoded);
                try(FileChannel src=FileChannel.open(input)){
                    assertEquals(original,Mp4Metadata.creationTime(src,()->false));
                    Mp4Metadata.copy(src,output.toFile(),()->false,s->{},fallback);
                }
                byte[] result=Files.readAllBytes(output);
                assertArrayEquals(Arrays.copyOf(encoded,36),Arrays.copyOf(result,36));
                assertTrue(new String(result,StandardCharsets.ISO_8859_1).contains("original title"));
                for(String type:List.of("mvhd","tkhd","mdhd")){
                    assertEquals(List.of(original==0?fallback:original),field(result,type,0));
                    assertEquals(List.of(77L),field(result,type,1));
                }
                try(FileChannel file=FileChannel.open(output)){assertEquals(original==0?fallback:original,Mp4Metadata.creationTime(file,()->false));}
            }
        }finally{Files.deleteIfExists(input);Files.deleteIfExists(output);}
    }
    @Test public void absentAndroidDateLeavesZeroUntouched()throws Exception {
        Path input=Files.createTempFile("date-source-",".mp4"),output=Files.createTempFile("date-result-",".mp4");
        try{
            Files.write(input,movie(0,0,0));Files.write(output,movie(0,99,100));
            try(FileChannel src=FileChannel.open(input)){Mp4Metadata.copy(src,output.toFile(),()->false,s->{},0);}
            assertEquals(List.of(0L),field(Files.readAllBytes(output),"mvhd",0));
        }finally{Files.deleteIfExists(input);Files.deleteIfExists(output);}
    }
}
