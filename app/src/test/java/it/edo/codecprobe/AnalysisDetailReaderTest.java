package it.edo.codecprobe;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AnalysisDetailReaderTest {
    static byte[] orientation(boolean little,int value){
        ByteBuffer b=ByteBuffer.allocate(26).order(little?ByteOrder.LITTLE_ENDIAN:ByteOrder.BIG_ENDIAN);
        b.put((byte)(little?'I':'M')).put((byte)(little?'I':'M')).putShort((short)42).putInt(8).putShort((short)1);
        b.putShort((short)0x112).putShort((short)3).putInt(1).putShort((short)value).putShort((short)0).putInt(0);return b.array();
    }
    static AnalysisNode field(AnalysisReader r,boolean external)throws Exception{
        for(AnalysisNode n:ExifAnalysisParser.read(r,new AnalysisNode("EXIF","",0,r.size,"exif",0)))if(n.field!=null&&(external?n.offset!=n.field.entry:n.offset==n.field.entry))return n;
        throw new AssertionError("Field missing");
    }
    interface Work{void run(AnalysisReader r)throws Exception;}
    static void with(byte[] bytes,Work work)throws Exception{
        Path path=Files.createTempFile("analysis-detail-",".bin");try{Files.write(path,bytes);try(RandomAccessFile file=new RandomAccessFile(path.toFile(),"r")){work.run(new AnalysisReader(file.getChannel()));}}finally{Files.delete(path);}
    }
    @Test public void littleAndBigEndianAreExplainedUsingActualBytes()throws Exception{
        for(boolean little:new boolean[]{true,false})with(orientation(little,6),r->{
            AnalysisDetail d=AnalysisDetailReader.read(r,field(r,false));
            assertEquals(little?"12 01":"01 12",d.parts.get(0).hex);assertEquals(2,d.parts.get(0).size);
            assertTrue(d.parts.get(1).meaning.contains("SHORT"));assertTrue(d.parts.get(2).meaning.contains("1 × 2 = 2"));
            assertEquals(2,d.parts.get(3).size);assertEquals(2,d.parts.get(4).size);
            assertTrue(d.result.contains("6 → rotazione di 90° in senso orario"));assertTrue(d.preview().startsWith("6 →"));assertTrue(d.alternatives.contains("6 = rotazione di 90° in senso orario  [PRESENTE NEL FILE]"));
        });
    }
    @Test public void everyOrientationHasAConcreteMeaningAndUnknownValuesAreNotGuessed()throws Exception{
        for(int value=1;value<=8;value++){final int v=value;with(orientation(true,value),r->{AnalysisDetail d=AnalysisDetailReader.read(r,field(r,false));assertTrue(d.alternatives.contains(v+" = "));assertFalse(d.result.contains("valore non riconosciuto"));});}
        with(orientation(true,99),r->assertTrue(AnalysisDetailReader.read(r,field(r,false)).result.contains("valore non riconosciuto")));
    }
    @Test public void externalAsciiFollowsTheOffsetAndPreservesItsTagIdentity()throws Exception{
        ByteBuffer b=ByteBuffer.allocate(34).order(ByteOrder.LITTLE_ENDIAN);b.put((byte)'I').put((byte)'I').putShort((short)42).putInt(8).putShort((short)1);
        b.putShort((short)0x10f).putShort((short)2).putInt(8).putInt(26).putInt(0).put(new byte[]{'S','A','M','S','U','N','G',0});
        with(b.array(),r->{AnalysisNode entry=field(r,false),value=field(r,true);assertEquals(entry.field.entry,value.field.entry);assertEquals(entry.field.value,value.field.value);
            AnalysisDetail d=AnalysisDetailReader.read(r,entry);assertTrue(d.parts.get(3).meaning.contains("0 + offset 26 = posizione assoluta 26"));assertEquals(26,d.parts.get(4).offset);assertEquals(8,d.parts.get(4).size);assertTrue(d.result.contains("SAMSUNG\\0"));
            assertTrue(AnalysisDetailReader.read(r,value).result.contains("SAMSUNG\\0"));
        });
    }
    @Test public void changedDirectoryHeaderIsRejectedInsteadOfShowingCachedClaims()throws Exception{
        Path path=Files.createTempFile("changed-field-",".tiff");try{Files.write(path,orientation(true,6));try(RandomAccessFile file=new RandomAccessFile(path.toFile(),"rw")){
            AnalysisReader r=new AnalysisReader(file.getChannel());AnalysisNode n=field(r,false);file.seek(10);file.write(new byte[]{0x10,1});
            try{AnalysisDetailReader.read(r,n);fail("Changed tag accepted");}catch(IOException expected){assertTrue(expected.getMessage().contains("cambiata"));}
        }}finally{Files.delete(path);}
    }
    @Test public void boxAnatomySeparatesAsciiIdAndBigEndianLength()throws Exception{
        byte[] bytes=FileAnalysisChecks.box("free",new byte[600]);with(bytes,r->{
            AnalysisNode n=new AnalysisNode("free","",0,bytes.length,"",8);AnalysisDetail d=AnalysisDetailReader.read(r,n);
            assertEquals("00 00 02 60",d.parts.get(0).hex);assertEquals("66 72 65 65",d.parts.get(1).hex);assertTrue(d.result.contains("8 di intestazione e 600 di contenuto"));assertTrue(d.parts.get(2).meaning.contains("Mostrati 64 di 600"));
        });
    }
    @Test public void jpegLengthExcludesMarkerAndIncludesLengthField()throws Exception{
        byte[] segment={(byte)255,(byte)225,0,16,'E','x','i','f',0,0,'I','I',42,0,8,0,0,0};
        with(segment,r->{AnalysisDetail d=AnalysisDetailReader.read(r,new AnalysisNode("APP1 · EXIF","",0,18,"exif",10));
            assertEquals("FF E1",d.parts.get(0).hex);assertEquals("00 10",d.parts.get(1).hex);assertTrue(d.parts.get(1).meaning.contains("Include i 2 byte"));assertTrue(d.parts.get(2).meaning.contains("Exif\\0\\0"));
        });
    }
    @Test public void unknownTiffTypeDoesNotInventAnExternalDataSize()throws Exception{
        byte[] bytes=orientation(true,6);ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putShort(12,(short)99);
        with(bytes,r->{AnalysisDetail d=AnalysisDetailReader.read(r,field(r,false));assertTrue(d.result.contains("Tipo non supportato"));assertEquals(4,d.parts.size());});
    }
    @Test public void truncatedSelectedRangeIsRejected()throws Exception{
        with(new byte[4],r->{try{AnalysisDetailReader.read(r,new AnalysisNode("free","",0,12,"",8));fail("Truncated node accepted");}catch(IOException expected){}});
    }
}
