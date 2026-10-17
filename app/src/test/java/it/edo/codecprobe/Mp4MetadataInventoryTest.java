package it.edo.codecprobe;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class Mp4MetadataInventoryTest {
    static byte[] join(byte[]... values)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();for(byte[] value:values)out.write(value);return out.toByteArray();}
    static byte[] box(String type,byte[] payload){ByteBuffer bytes=ByteBuffer.allocate(payload.length+8);bytes.putInt(bytes.capacity()).put(type.getBytes(StandardCharsets.ISO_8859_1)).put(payload);return bytes.array();}
    static byte[] date(int version,long seconds){ByteBuffer bytes=ByteBuffer.allocate(32);bytes.put(0,(byte)version);if(version==1){bytes.putLong(4,seconds).putLong(12,seconds+5);}else{bytes.putInt(4,(int)seconds).putInt(8,(int)(seconds+5));}return box("mvhd",bytes.array());}
    static String report(byte[] bytes)throws Exception{
        Path path=Files.createTempFile("metadata-inventory-",".mp4");
        try{Files.write(path,bytes);try(FileChannel file=FileChannel.open(path)){StringWriter out=new StringWriter();Mp4MetadataInventory.write(file,out,()->false);return out.toString();}}
        finally{Files.deleteIfExists(path);}
    }
    @Test public void datesVersionZeroAndOneAndNestedTextAreReadable()throws Exception{
        for(int version:new int[]{0,1}){
            byte[] value=join(ByteBuffer.allocate(8).putInt(1).putInt(0).array(),"2026-10-17T10:00:00+02:00".getBytes(StandardCharsets.UTF_8));
            byte[] metadata=box("meta",join(new byte[4],box("keys",join(ByteBuffer.allocate(8).putInt(0).putInt(1).array(),box("mdta","com.apple.quicktime.creationdate".getBytes(StandardCharsets.UTF_8)))),box("ilst",box("©day",box("data",value)))));
            String text=report(join(box("ftyp",new byte[8]),box("moov",join(date(version,3900000000L),box("udta",metadata))),box("mdat","NOT_VIDEO_EXPORT".getBytes())));
            assertTrue(text.contains("CREAZIONE · secondi dal 1904=3900000000"));assertTrue(text.contains("MODIFICA · secondi dal 1904=3900000005"));
            assertTrue(text.contains("versione="+version));assertTrue(text.contains("com.apple.quicktime.creationdate"));assertTrue(text.contains("VALORE UTF-8=2026-10-17T10:00:00+02:00"));
            assertTrue(text.contains("SHA256 PAYLOAD="));assertFalse(text.contains("NOT_VIDEO_EXPORT"));
        }
    }
    @Test public void opaqueMetadataHashSurvivesRelocationAndPreviewIsBounded()throws Exception{
        byte[] value=new byte[1024];Arrays.fill(value,(byte)'X');
        String first=report(box("uuid",value));String second=report(join(box("free",new byte[32]),box("uuid",value)));
        String expected=Mp4MetadataInventory.digest(ByteBuffer.wrap(value));assertTrue(first.contains(expected));assertTrue(second.contains(expected));
        assertTrue(first.contains("primi 256 / 1024 byte"));assertTrue(first.length()<2500);
        ByteBuffer buffer=ByteBuffer.wrap(value);buffer.position(10);int position=buffer.position();Mp4MetadataInventory.digest(buffer);assertEquals(position,buffer.position());
    }
    @Test public void sparseMediaIsSkippedAndMalformedOrCancelledReportsFailClearly()throws Exception{
        Path path=Files.createTempFile("metadata-large-mdat-",".mp4");
        try(RandomAccessFile file=new RandomAccessFile(path.toFile(),"rw")){
            file.setLength(64L*1024*1024);file.writeInt(0);file.writeBytes("mdat");
            StringWriter out=new StringWriter();Mp4MetadataInventory.write(file.getChannel(),out,()->false);
            assertTrue(out.toString().contains("CONTENUTO MULTIMEDIALE SALTATO"));assertTrue(out.toString().length()<800);
            try{Mp4MetadataInventory.write(file.getChannel(),new StringWriter(),()->true);fail();}catch(InterruptedIOException expected){}
        }finally{Files.deleteIfExists(path);}
        try{report(ByteBuffer.allocate(8).putInt(1000).put("udta".getBytes()).array());fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("Dimensione blocco non valida"));}
    }
}
