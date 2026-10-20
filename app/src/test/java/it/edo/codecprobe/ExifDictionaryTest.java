package it.edo.codecprobe;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real TIFF fixtures exercise directory context, schema guards and actual-value explanations. */
public class ExifDictionaryTest {
    interface Check {void run(AnalysisNode node,AnalysisDetail detail)throws Exception;}
    static void single(String directory,int tag,int type,int count,byte[] data,boolean little,Check check)throws Exception{
        boolean nested=directory.equals("GPS")||directory.equals("Interop");
        int entry=nested?28:10,end=entry+16;
        ByteBuffer b=ByteBuffer.allocate(end+(data.length>4?data.length:0)).order(little?ByteOrder.LITTLE_ENDIAN:ByteOrder.BIG_ENDIAN);
        b.put((byte)(little?'I':'M')).put((byte)(little?'I':'M')).putShort((short)42).putInt(8).putShort((short)1);
        if(nested){b.putShort((short)(directory.equals("GPS")?0x8825:0xa005)).putShort((short)4).putInt(1).putInt(26).putInt(0).putShort((short)1);}
        b.putShort((short)tag).putShort((short)type).putInt(count);
        if(data.length>4)b.putInt(end);else{b.put(data);while(b.position()<entry+12)b.put((byte)0);}
        b.putInt(0);if(data.length>4)b.put(data);
        AnalysisDetailReaderTest.with(b.array(),r->{
            AnalysisNode selected=null;
            for(AnalysisNode n:ExifAnalysisParser.read(r,new AnalysisNode("EXIF","",0,r.size,"exif",0)))
                if(n.field!=null&&n.field.tag==tag&&n.field.directory.equals(directory)&&n.offset==n.field.entry){selected=n;break;}
            assertNotNull(selected);check.run(selected,AnalysisDetailReader.read(r,selected));
        });
    }
    static byte[] shorts(boolean little,int... values){ByteBuffer b=ByteBuffer.allocate(values.length*2).order(little?ByteOrder.LITTLE_ENDIAN:ByteOrder.BIG_ENDIAN);for(int n:values)b.putShort((short)n);return b.array();}
    static byte[] fractions(boolean little,int... values){ByteBuffer b=ByteBuffer.allocate(values.length*4).order(little?ByteOrder.LITTLE_ENDIAN:ByteOrder.BIG_ENDIAN);for(int n:values)b.putInt(n);return b.array();}
    @Test public void standardCoverageAndDirectoryNamespacesAreExplicit(){
        assertEquals(165,ExifTagDictionary.size());
        assertEquals("GPSLatitudeRef",ExifTagDictionary.find(1,"GPS").technical);
        assertEquals("InteropIndex",ExifTagDictionary.find(1,"Interop").technical);
        assertNull(ExifTagDictionary.find(1,"IFD0"));
        assertNull(ExifTagDictionary.find(0x112,"GPS"));
        assertNull(ExifTagDictionary.find(0xffff,"EXIF"));
        for(int id=0;id<32;id++){assertNotNull(ExifTagDictionary.find(id,"GPS"));assertFalse(ExifTagDictionary.find(id,"GPS").meaning.isEmpty());}
        assertTrue(ExifTagDictionary.find(0x9287,"EXIF").meaning.contains("non è ancora decodificato"));
        assertTrue(ExifTagDictionary.find(0xa40d,"EXIF").meaning.contains("Due codici"));
    }
    @Test public void everyIdInTheReferenceSnapshotHasAnItalianExplanation()throws Exception{
        try(java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/exif-standard-ids.txt")),StandardCharsets.UTF_8))){
        for(String line;(line=reader.readLine())!=null;){
            String[] cells=line.split(" ");int id=Integer.parseInt(cells[0],16);
            ExifTagDictionary.Entry e=ExifTagDictionary.find(id,"EXIF");assertNotNull(line,e);
            assertEquals(line,cells[1],e.technical);assertTrue(line,e.meaning.length()>20);assertFalse(line,e.name.isEmpty());
        }}
    }
    @Test public void sameNumericIdGetsTheMeaningOfItsOwnDirectory()throws Exception{
        single("GPS",1,2,2,new byte[]{'S',0},true,(n,d)->{assertEquals("Emisfero della latitudine",AnalysisLabels.of(n).name);assertTrue(d.preview().contains("S → sud"));assertTrue(d.alternatives.contains("N = nord"));});
        single("Interop",1,2,4,new byte[]{'R','9','8',0},false,(n,d)->{assertEquals("Profilo di interoperabilità",AnalysisLabels.of(n).name);assertTrue(d.introduction.contains("R98"));assertFalse(d.alternatives.contains("nord"));});
        single("GPS",0x112,3,1,shorts(true,6),true,(n,d)->{assertEquals("Tag EXIF non tradotto",AnalysisLabels.of(n).name);assertFalse(d.result.contains("rotazione"));});
    }
    @Test public void exposureAndSignedEvHaveUnitsAndZeroDenominatorsStayVisible()throws Exception{
        for(boolean little:new boolean[]{true,false}){
            single("IFD0",0x829a,5,1,fractions(little,1,100),little,(n,d)->{assertTrue(d.result.contains("1/100"));assertTrue(d.preview().endsWith(" s."));assertTrue(d.introduction.contains("Tempo di esposizione"));assertEquals(8,d.parts.get(4).size);});
            single("IFD0",0x9204,10,1,fractions(little,-1,3),little,(n,d)->{assertTrue(d.preview().contains("-0.33333333 EV"));});
        }
        single("IFD0",0x829a,5,1,fractions(true,1,0),true,(n,d)->{assertTrue(d.result.contains("divisione non definita"));assertTrue(d.preview().contains("denominatore zero"));});
        single("IFD0",0x9201,10,1,fractions(true,7,1),true,(n,d)->assertTrue(d.preview().contains("0.0078125000 s")));
    }
    @Test public void accelerationUsesMilligalRatherThanTreatingRawValueAsSiUnits()throws Exception{
        single("IFD0",0x9404,5,1,fractions(true,100000,1),true,(n,d)->{assertTrue(d.preview().contains("mGal = 1.0000000 m/s²"));});
    }
    @Test public void enumValuesAreHighlightedButWrongSchemasAndUnknownCodesAreNotGuessed()throws Exception{
        single("IFD0",0xa403,3,1,shorts(true,1),true,(n,d)->{assertTrue(d.preview().contains("1 → manuale"));assertTrue(d.alternatives.contains("1 = manuale  [PRESENTE NEL FILE]"));});
        single("IFD0",0xa403,3,1,shorts(true,99),true,(n,d)->assertTrue(d.preview().contains("valore non riconosciuto")));
        single("IFD0",0xa403,3,2,shorts(true,1,0),true,(n,d)->assertTrue(d.preview().contains("tipo o quantità")));
        single("GPS",1,2,2,new byte[]{'S','X'},true,(n,d)->assertTrue(d.preview().contains("terminatore nullo")));
    }
    @Test public void flashBitsAndCoordinatesDescribeActualComponents()throws Exception{
        single("IFD0",0x9209,3,1,shorts(false,0x59),false,(n,d)->{assertTrue(d.preview().contains("flash scattato"));assertTrue(d.preview().contains("automatica"));assertTrue(d.preview().contains("occhi rossi attiva"));assertTrue(d.alternatives.contains("Bit 3–4"));});
        single("IFD0",0xa214,3,2,shorts(true,120,240),true,(n,d)->{assertTrue(d.preview().contains("x orizzontale = 120 px"));assertTrue(d.preview().contains("y verticale = 240 px"));});
        single("IFD0",0x9214,3,4,shorts(true,120,240,50,80),true,(n,d)->assertTrue(d.preview().contains("larghezza = 50 px; altezza = 80 px")));
        single("GPS",2,5,3,fractions(true,45,1,30,1,0,1),true,(n,d)->{assertTrue(d.preview().contains("45.500000°"));assertTrue(d.preview().contains("Segno da leggere"));});
    }
    @Test public void knownUndefinedLayoutsAreDecodedAndOpaquePayloadsRemainExplicit()throws Exception{
        single("IFD0",0x9000,7,4,new byte[]{'0','2','3','2'},true,(n,d)->assertTrue(d.preview().contains("versione 2.32")));
        single("IFD0",0xa300,7,1,new byte[]{3},true,(n,d)->assertTrue(d.preview().contains("fotocamera digitale")));
        single("IFD0",0x9101,7,4,new byte[]{1,2,3,0},true,(n,d)->assertTrue(d.preview().contains("1 = Y, 2 = Cb, 3 = Cr, 0 = assente")));
        byte[] comment=new byte[]{65,83,67,73,73,0,0,0,'c','i','a','o',0};
        single("IFD0",0x9286,7,comment.length,comment,true,(n,d)->assertTrue(d.preview().contains("ciao\\0")));
        single("IFD0",0x8828,7,4,new byte[]{1,2,3,4},true,(n,d)->{assertTrue(d.introduction.contains("non viene decodificata"));assertTrue(d.result.contains("UNDEFINED"));assertFalse(d.result.contains("VALORE TROVATO"));});
        single("IFD0",0xa40d,3,2,shorts(true,4,2),true,(n,d)->assertTrue(d.preview().contains("differenze estreme; impostazioni: 2 → diverse")));
    }
}
