package it.edo.codecprobe;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Real diagnostic payload regression + malformed variants + complete MP4 relocation. */
public final class SamsungSefMotionTest {
    @org.junit.Test public void completeLogPayloadsAndRelocation()throws Exception{main(new String[0]);}
    private static void reject(int type,String name,String text)throws Exception{
        try{SamsungSef.validate(SamsungSefMotionFixtures.fixture(106,new SamsungSefMotionFixtures.Item(type,name,text)));}
        catch(IOException expected){return;}throw new AssertionError("Accepted malformed "+name+": "+text);
    }
    public static void main(String[] args)throws Exception{
        Path input=Files.createTempFile("sef-motion-source-",".mp4"),output=Files.createTempFile("sef-motion-target-",".mp4");
        try{
            for(String[] sample:SamsungSefMotionFixtures.RECORDS){
                byte[] payload=HexFormat.of().parseHex(sample[2]);String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
                if(!hash.equals(sample[3]))throw new AssertionError("Diagnostic SHA mismatch");
                byte[] sef=SamsungSefMotionFixtures.fixture(106,new SamsungSefMotionFixtures.Item(Integer.parseInt(sample[0],16),sample[1],payload));
                String report=SamsungSef.validate(sef);if(!report.contains("1 copiabili · 0 bloccati"))throw new AssertionError(report);
                Files.write(input,SamsungSefMotionFixtures.join(SamsungSefMotionFixtures.content(true),sef));byte[] before=SamsungSefMotionFixtures.content(false);Files.write(output,before);
                try(FileChannel source=FileChannel.open(input)){Mp4Metadata.copy(source,output.toFile(),()->false,s->{});}
                byte[] after=Files.readAllBytes(output);
                if(!Arrays.equals(sef,Arrays.copyOfRange(after,after.length-sef.length,after.length)))throw new AssertionError("Relocated SEF changed: "+sample[1]);
                if(after.length-sef.length==SamsungSefMotionFixtures.content(true).length)throw new AssertionError("Fixture did not relocate");
                for(int i=0;i<37;i++)if(after[i]!=before[i])throw new AssertionError("Encoded media changed during metadata copy");
            }
            for(int length:new int[]{1,12,13,14,26,4096})SamsungSef.validate(SamsungSefMotionFixtures.fixture(105,new SamsungSefMotionFixtures.Item(0x7ffe,"Invalid_Data",new byte[length])));
            for(String name:new String[]{"Super_SlowMotion_Data","Super_SlowMotion_Edit_Data"}){
                int type=name.endsWith("Edit_Data")?0x0b03:0x0b00;
                for(String value:new String[]{"0:100:50:1","0:100:100:9!0*100:200:150:2!1"})SamsungSef.validate(SamsungSefMotionFixtures.fixture(106,new SamsungSefMotionFixtures.Item(type,name,value)));
                for(String value:new String[]{"","0:0:0:1!1","1:10:0:1!1","0:10:11:1!1","0:10:2:0!1","0:10:2:10!1","0:10:2:1!","0:10:2:1!1!2","0:10:2:1*9:20:10:1","0:2147483648:2:1!1","0:10:2:1\u0000"})reject(type,name,value);
            }
            for(int type:new int[]{0x0b00,0x0890})SamsungSef.validate(SamsungSefMotionFixtures.fixture(106,new SamsungSefMotionFixtures.Item(type,"Super_SlowMotion_Data","0:100:50:1!1")));
            reject(0x0ba1,"Super_SlowMotion_Data","0:100:50:1!1");
            String bgm="1:Theme:themedata/bgm/theme.ogg:true:100:false:null";
            for(int type:new int[]{0x0b01,0x0890})SamsungSef.validate(SamsungSefMotionFixtures.fixture(106,new SamsungSefMotionFixtures.Item(type,"Super_SlowMotion_BGM",bgm)));
            for(String value:new String[]{bgm.replace("themedata/bgm","/sdcard"),bgm.replace("bgm/","../"),bgm.replace("true","yes"),bgm.replace(":100:",":101:"),bgm.replace(":null",":original.mp4"),bgm+":extra"})reject(0x0b01,"Super_SlowMotion_BGM",value);
            reject(0x0b04,"Super_SlowMotion_Deflickering_On","true");reject(0x0b02,"Super_SlowMotion_Deflickering_Info","F");
            // A mixed directory must survive validation without changing record bytes/order.
            byte[] mixed=SamsungSefMotionFixtures.fixture(106,new SamsungSefMotionFixtures.Item(0x0b04,"Super_SlowMotion_Deflickering_On","F"),new SamsungSefMotionFixtures.Item(0x0890,"Super_SlowMotion_BGM",bgm),new SamsungSefMotionFixtures.Item(0x0b03,"Super_SlowMotion_Edit_Data","0:100:50:1!1"),new SamsungSefMotionFixtures.Item(0x0890,"Super_SlowMotion_Data","0:100:50:1!1"));
            byte[] before=mixed.clone();if(!SamsungSef.validate(mixed).contains("4 copiabili · 0 bloccati")||!Arrays.equals(before,mixed))throw new AssertionError("Mixed directory changed");
            System.out.println("OK Samsung motion: "+SamsungSefMotionFixtures.RECORDS.length+" complete log payloads/SHA-256, MP4 relocation with unchanged SEF/media, super-motion/legacy aliases/flags/BGM, bounded zero-padding and malformed variants");
        }finally{Files.deleteIfExists(input);Files.deleteIfExists(output);}
    }
}
