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

    static byte[] exportedSef(String report)throws Exception{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        java.util.regex.Matcher chunks=java.util.regex.Pattern.compile("(?m)^SEF BASE64 ([0-9]+)=([A-Za-z0-9+/=]+)$").matcher(report);
        while(chunks.find()){
            assertEquals(bytes.size(),Integer.parseInt(chunks.group(1)));
            bytes.write(Base64.getDecoder().decode(chunks.group(2)));
        }
        return bytes.toByteArray();
    }
    @Test public void unknownEditorRecordsExportEveryByteAndCompleteJson()throws Exception{
        String json="{\"NDE_VERSION\":\"2.0\",\"data\":\""+"é\\\"".repeat(400)+"END_OF_COMPLETE_JSON\"}";
        byte[] binary=new byte[2501];new Random(8241).nextBytes(binary);
        byte[] sef=SamsungSefMotionFixtures.fixture(107,
            new SamsungSefMotionFixtures.Item(0x0ba1,"VideoEditor_Re_Edit_Data",json.getBytes(StandardCharsets.UTF_8)),
            new SamsungSefMotionFixtures.Item(0x0ba1,"Unknown_Binary_Metadata",binary));
        String report=report(join(box("mdat","DO_NOT_EXPORT_VIDEO".getBytes(StandardCharsets.UTF_8)),sef));
        assertArrayEquals(sef,exportedSef(report));
        assertTrue(report.contains("SHA256="+Mp4MetadataInventory.digest(ByteBuffer.wrap(sef))));
        assertTrue(report.contains("SEF TESTO COMPLETO="+Mp4MetadataInventory.escape(json)));
        assertTrue(report.contains("END_OF_COMPLETE_JSON"));assertFalse(report.contains("DO_NOT_EXPORT_VIDEO"));
        assertTrue(report.contains("*** FINE SEF BLOCCO COMPLETO ***"));
    }
    @Test public void malformedSefStillExportsExactBytesAndLargeBlocksHaveExplicitLimit()throws Exception{
        byte[] malformed=box("sefd",new byte[800]);String report=report(malformed);
        assertArrayEquals(malformed,exportedSef(report));assertTrue(report.contains("SEF NON INTERPRETABILE"));
        byte[] large=box("sefd",new byte[SamsungSef.MAX_BYTES]);String limited=report(large);
        assertEquals(0,exportedSef(limited).length);assertTrue(limited.contains("blocco completo non esportato"));
        assertFalse(limited.contains("*** SEF BLOCCO COMPLETO"));
    }
    @Test public void completeSefExportRemainsCancellableBetweenChunks()throws Exception{
        Path path=Files.createTempFile("sef-export-cancel-",".mp4");StringWriter out=new StringWriter();
        try{
            Files.write(path,box("sefd",new byte[3000]));
            try(FileChannel file=FileChannel.open(path)){
                try{Mp4MetadataInventory.write(file,out,()->out.toString().contains("SEF BASE64 0="));fail();}
                catch(InterruptedIOException expected){}
            }
            assertTrue(out.toString().contains("SEF BASE64 0="));assertFalse(out.toString().contains("*** FINE SEF BLOCCO COMPLETO ***"));
        }finally{Files.deleteIfExists(path);}
    }

    @Test public void quickTimeMetaWithoutFullBoxFlagsCannotHideFollowingSef()throws Exception{
        byte[] editor=SamsungSefMotionFixtures.fixture(107,new SamsungSefMotionFixtures.Item(0x0ba1,"VideoEditor_Re_Edit_Data","{\"NDE_VERSION\":\"2.0\",\"payload\":\""+"A".repeat(1400)+"\"}"));
        byte[] meta=box("meta",join(box("hdlr",new byte[24]),box("keys",new byte[8])));
        String report=report(join(box("ftyp",new byte[8]),box("mdat",new byte[64]),box("moov",meta),editor));
        assertArrayEquals(editor,exportedSef(report));assertTrue(report.contains("META QUICKTIME"));
        assertTrue(report.contains("root/moov[1]/meta[1]/hdlr[1]"));
        assertFalse(report.contains("NON INTERPRETABILE"));
    }
    @Test public void malformedNestedMetadataStillLeavesCompleteRootSefAvailable()throws Exception{
        byte[] editor=SamsungSefMotionFixtures.fixture(107,new SamsungSefMotionFixtures.Item(0x0ba1,"VideoEditor_Re_Edit_Data","{\"unknown\":true}"));
        byte[] malformedMeta=box("meta",join(new byte[4],ByteBuffer.allocate(8).putInt(1000).put("hdlr".getBytes(StandardCharsets.US_ASCII)).array()));
        Path path=Files.createTempFile("sef-before-malformed-meta-",".mp4");StringWriter out=new StringWriter();
        try{
            Files.write(path,join(box("moov",malformedMeta),editor));
            try(FileChannel file=FileChannel.open(path)){
                try{Mp4MetadataInventory.write(file,out,()->false);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("Dimensione blocco non valida"));}
            }
            assertArrayEquals(editor,exportedSef(out.toString()));
            assertTrue(out.toString().contains("*** FINE SEF BLOCCO COMPLETO ***"));
        }finally{Files.deleteIfExists(path);}
    }
}
