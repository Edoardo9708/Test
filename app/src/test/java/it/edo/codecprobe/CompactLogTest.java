package it.edo.codecprobe;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CompactLogTest {
    @Test public void repeatedSuccessesAreSummarizedButWarningsErrorsAndUnknownMessagesSurvive(){
        List<String> messages=new ArrayList<>();CompactLog log=new CompactLog(messages::add);
        log.accept("*** STEP 2/3 · CODIFICA ***",0);
        for(int i=0;i<100000;i++){
            log.accept("BEGIN x265_encoder_encode frame="+i+" pts_us="+(i*33333L),i);
            log.accept("END x265_encoder_encode rc=1 nals=1 elapsed_ms=3",i);
            log.accept("VERIFY frame="+i+" pts_us="+(i*33333L),i);
        }
        log.accept("Nuova diagnostica non ancora conosciuta",100001);
        log.accept("*** ERRORE · CONVERSIONE INTERROTTA ***",100002);
        log.accept("java.io.IOException: Example\n\tat example.Source.run(Source.java:12)",100003);
        log.accept("SESSION END cancelled=false",100004);log.finish();
        String result=String.join("\n",messages);
        assertTrue(messages.size()<100);assertTrue(result.length()<30000);
        assertTrue(result.contains("frame=99999"));assertTrue(result.contains("ULTIMI EVENTI DETTAGLIATI"));assertTrue(result.contains("RIEPILOGO EVENTI RIPETITIVI"));
        assertTrue(result.contains("Nuova diagnostica non ancora conosciuta"));assertTrue(result.contains("Source.java:12"));assertTrue(result.contains("SESSION END"));
    }
    @Test public void negativeCodecResultsAreNeverSampledAway(){
        List<String> messages=new ArrayList<>();CompactLog log=new CompactLog(messages::add);
        for(int i=0;i<100;i++)log.accept("END x265_encoder_encode rc=0 nals=0",0);
        log.accept("END x265_encoder_encode rc=-1 nals=0",0);
        assertTrue(String.join("\n",messages).contains("rc=-1"));
    }
    @Test public void legacyExportKeepsEveryFileAndErrorAndHandlesTimestampRestart()throws Exception{
        ByteArrayOutputStream old=new ByteArrayOutputStream();
        for(int file=1;file<=8;file++){
            old.write(("*** INIZIO VIDEO "+file+" / 8 ***\nNome: video"+file+".mp4\n").getBytes(StandardCharsets.UTF_8));
            for(int frame=0;frame<10000;frame++)old.write(("["+(frame*10)+" ms] BEGIN conversione YUV frame="+frame+" pts_us="+frame+"\n").getBytes(StandardCharsets.UTF_8));
            old.write("[100001 ms] BEGIN muxer.stop\n[100002 ms] OK muxer.stop\n".getBytes(StandardCharsets.UTF_8));
            if(file==8)old.write("[100003 ms] *** ERRORE ***\njava.io.IOException: moov assente\n\tat Mp4Metadata.roots(Mp4Metadata.java:86)\n".getBytes(StandardCharsets.UTF_8));
        }
        byte[] original=old.toByteArray();ByteArrayOutputStream output=new ByteArrayOutputStream();CompactLog.export(new ByteArrayInputStream(original),output);
        String text=output.toString(StandardCharsets.UTF_8.name());
        assertTrue(output.size()<original.length/20);
        for(int file=1;file<=8;file++){assertTrue(text.contains("Nome: video"+file+".mp4"));assertTrue(text.contains("INIZIO VIDEO "+file+" / 8"));}
        assertTrue(text.contains("moov assente"));assertTrue(text.contains("Mp4Metadata.java:86"));assertTrue(text.contains("BEGIN muxer.stop"));assertTrue(text.contains("OK muxer.stop"));
        assertEquals(8,text.split("frame=0 pts_us=0",-1).length-1);
    }
    @Test public void hdrMultilineAndAbruptEndAreFlushedAndLargeSamplesAreBounded()throws Exception{
        List<String> messages=new ArrayList<>();CompactLog log=new CompactLog(messages::add);
        StringBuilder large=new StringBuilder();for(int i=0;i<20000;i++)large.append('x');
        for(int i=0;i<1000;i++)log.accept("FRAME HDR · numero="+i+"\nMetadati originali: "+large,0);
        log.finish();String text=String.join("\n",messages);
        assertTrue(text.contains("numero=999"));assertTrue(text.contains("campione abbreviato"));assertTrue(text.length()<50000);
        ByteArrayOutputStream out=new ByteArrayOutputStream();CompactLog.export(new ByteArrayInputStream("[1 ms] VERIFY frame=1\n[2 ms] VERIFY frame=2\n[3 ms] VERIFY frame=3".getBytes(StandardCharsets.UTF_8)),out);
        assertTrue(out.toString("UTF-8").contains("frame=3"));
    }
    @Test public void exportUsesTheRealStreamInsteadOfAssumingAvailableIsItsLength()throws Exception{
        byte[] data="*** PARAMETRI ***\nCRF=23\njava.io.IOException: error".getBytes(StandardCharsets.UTF_8);
        InputStream input=new ByteArrayInputStream(data){public int available(){return 0;}};
        ByteArrayOutputStream output=new ByteArrayOutputStream();CompactLog.export(input,output);
        assertTrue(output.toString("UTF-8").contains("CRF=23"));assertTrue(output.toString("UTF-8").contains("IOException: error"));
    }
    @Test public void repeatedP010FormatsAndHdrVerificationAreCompactedToo()throws Exception{
        ByteArrayOutputStream verbose=new ByteArrayOutputStream();
        for(int i=0;i<10000;i++){
            verbose.write(("["+i+" ms] FORMATO P010 · {width=1920, height=1080, color-format=54, timeUs="+i+"}\n").getBytes(StandardCharsets.UTF_8));
            verbose.write(("["+i+" ms] OK VERIFY HDR SEI · frame="+i+" · pts_us="+i+" · payload originali identici (SHA-256)\n").getBytes(StandardCharsets.UTF_8));
        }
        verbose.write("*** ERRORE ***\njava.io.IOException: retained HDR error\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output=new ByteArrayOutputStream();CompactLog.export(new ByteArrayInputStream(verbose.toByteArray()),output);
        String text=output.toString("UTF-8");assertTrue(output.size()<verbose.size()/20);
        assertTrue(text.contains("FORMATO P010"));assertTrue(text.contains("frame=9999"));assertTrue(text.contains("retained HDR error"));
    }
}
