package it.edo.codecprobe;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SamsungSefEditorTest {
    private static byte[] record(String name,byte[] value){return SamsungSefMotionFixtures.fixture(107,new SamsungSefMotionFixtures.Item(0x0ba1,name,value));}
    private static byte[] editor(String value){return record("VideoEditor_Re_Edit_Data",value.getBytes(StandardCharsets.UTF_8));}
    private static void reject(byte[] sef)throws Exception{try{SamsungSef.validate(sef);fail("Unverified editor schema accepted");}catch(IOException expected){}}
    @Test public void observedVersionsCopyWithoutChangingProjectOrReferences()throws Exception{
        Path source=Files.createTempFile("editor-source-",".mp4"),target=Files.createTempFile("editor-target-",".mp4");
        try{
            for(String project:SamsungSefEditorFixtures.PROJECTS){
                byte[] sef=SamsungSefMotionFixtures.fixture(107,
                    new SamsungSefMotionFixtures.Item(0x0ba1,"Original_Path_Hash_Key","b".repeat(64)+"/12345"),
                    new SamsungSefMotionFixtures.Item(0x0ba1,"VideoEditor_Re_Edit_Data",project.getBytes(StandardCharsets.UTF_8)));
                byte[] original=sef.clone();assertTrue(SamsungSef.validate(sef).contains("2 copiabili · 0 bloccati"));assertArrayEquals(original,sef);
                Files.write(source,SamsungSefMotionFixtures.join(SamsungSefMotionFixtures.content(true),sef));
                byte[] media=SamsungSefMotionFixtures.content(false);Files.write(target,media);
                try(FileChannel input=FileChannel.open(source)){Mp4Metadata.copy(input,target.toFile(),()->false,s->{});}
                byte[] output=Files.readAllBytes(target);
                assertArrayEquals(original,Arrays.copyOfRange(output,output.length-sef.length,output.length));
                assertArrayEquals(Arrays.copyOf(media,37),Arrays.copyOf(output,37));
            }
        }finally{Files.deleteIfExists(source);Files.deleteIfExists(target);}
    }
    @Test public void unexpectedFieldsVersionsReferencesAndInvalidJsonRemainBlocked()throws Exception{
        String v2=SamsungSefEditorFixtures.PROJECTS[0],v1=SamsungSefEditorFixtures.PROJECTS[2];
        for(String invalid:new String[]{
            v2.replace("\"NDE_VERSION\":\"2.0\"","\"NDE_VERSION\":\"3.0\""),
            v2.replace("\"ALLOW_REEDIT\":true","\"ALLOW_REEDIT\":1"),
            v2.replace("\"ALLOW_REEDIT\":true","\"ALLOW_REEDIT\":true,\"ALLOW_REEDIT\":false"),
            v2.replace("\"NDE_VERSION\"","\"OFFSET_IN_OUTPUT\":123,\"NDE_VERSION\""),
            v2.replace("_12345.mp4","_54321.mp4"),
            v2.replace("/data/sec/photoeditor/0/","/storage/unknown/"),
            v2.replace("\"BIT_DEPTH\":10","\"BIT_DEPTH\":9"),
            v2.replace("\"DURATION\":30356","\"DURATION\":1"),
            v2.replace("\"LAYERS_INFO\":[","\"LAYERS_INFO\":[{},"),
            v2.replace("\"LAYER_ID\":0","\"LAYER_ID\":1e100"),
            v2+"{}",v2.substring(0,v2.length()-1),"{}","null","[]",
            v1.replace("\"deltaRoi\":1.0012407302856445","\"deltaRoi\":NaN"),
            v1.replace("\"ORIGINAL_FILE_PATH\":\"","\"ORIGINAL_FILE_PATH\":\"!"),
            v1.replace("\"resolution\":[576,1024]","\"resolution\":[0,1024]")
        }){assertFalse("Fixture mutation did not apply",invalid.equals(v1)||invalid.equals(v2));reject(editor(invalid));}
        reject(record("VideoEditor_Re_Edit_Data",new byte[]{(byte)0xc3,0x28}));
        reject(record("VideoEditor_Re_Edit_Data",new byte[65537]));
        reject(SamsungSefMotionFixtures.fixture(107,new SamsungSefMotionFixtures.Item(0x0ba2,"VideoEditor_Re_Edit_Data",v2)));
        reject(record("PhotoEditor_Re_Edit_Data",v2.getBytes(StandardCharsets.UTF_8)));
    }
    @Test public void originalKeyIsAnExternalReferenceNotAnOutputHash()throws Exception{
        String valid="a".repeat(64)+"/12345";assertTrue(SamsungSef.validate(record("Original_Path_Hash_Key",valid.getBytes(StandardCharsets.US_ASCII))).contains("1 copiabili · 0 bloccati"));
        for(String invalid:new String[]{"a".repeat(63)+"/12345","A".repeat(64)+"/12345",valid+"\u0000","a".repeat(64)+"/0","a".repeat(64)+"/9999999999999999999",valid+"/offset"})reject(record("Original_Path_Hash_Key",invalid.getBytes(StandardCharsets.US_ASCII)));
    }
}
