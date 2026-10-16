package it.edo.codecprobe;

import android.content.*;
import android.net.Uri;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class SavedSessionTest {
    static ConversionService.State sample(Intent request){
        ConversionService.State s=new ConversionService.State();
        s.hasSession=true;s.recoverable=true;s.sessionId="saved-test-session";
        s.inputs=request.getStringArrayListExtra("inputs");s.names=request.getStringArrayListExtra("names");s.folders=request.getStringArrayListExtra("folders");s.batchTotal=s.inputs.size();
        s.batchIndex=2;s.filePercent=42;s.doneFrames=100;s.totalFrames=238;s.done=3333333;s.total=7933333;s.elapsedBefore=45000;
        s.items.add(new CompressionSummary.Item(1,s.names.get(0),CompressionSummary.Status.COMPLETE,1000,500));
        s.items.add(new CompressionSummary.Item(2,s.names.get(1),CompressionSummary.Status.IN_PROGRESS,-1,-1));
        s.outputUris.add("content://saved/first");s.outputUris.add("");
        s.succeeded=1;s.result=Uri.parse(s.outputUris.get(0));s.results.add(s.result);
        if(s.inputs.size()==3){
            s.items.add(new CompressionSummary.Item(3,s.names.get(2),CompressionSummary.Status.FAILED,-1,-1));s.outputUris.add("");s.failed=1;
            s.failures.add(new ConversionFailure(3,s.names.get(2),"Verifica audio",new IOException("Errore completo",new IOException("Causa interna"))));
        }
        s.sizeReport=CompressionSummary.render(s.items);return s;
    }

    @Test public void roundTripPreservesReportErrorsProgressOptionsAndPerFileUris()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Intent request=BatchRetryTest.queue(context);
        request.putExtra("namingAddition","converted-").putExtra("namingPrefix",false);
        ConversionService.State original=sample(request);SavedSession.save(context,request,original);
        SavedSession saved=SavedSession.load(context);assertNotNull(saved);ConversionService.State s=saved.state;
        assertTrue(s.recoverable);assertFalse(s.running);assertFalse(s.paused);assertEquals(original.sessionId,s.sessionId);
        assertEquals(original.sizeReport,s.sizeReport);assertEquals(original.outputUris,s.outputUris);assertEquals(original.failures.get(0).copyText(),s.failures.get(0).copyText());
        assertEquals(42,s.filePercent);assertEquals(100,s.doneFrames);assertEquals(3333333,s.done);assertEquals(45000,s.elapsedBefore);assertEquals(0,s.startedAt);
        assertEquals(17,s.requestedCrf);assertEquals("veryslow",s.requestedPreset);assertEquals(2,s.requestedCpu);
        Intent resume=saved.resumeRequest();assertArrayEquals(new boolean[]{true,false,true},resume.getBooleanArrayExtra("finishedFiles"));
        assertEquals(original.sessionId,resume.getStringExtra(SavedSession.RESTORE));assertEquals(request.getStringArrayListExtra("outputNames"),resume.getStringArrayListExtra("requestedOutputNames"));
        assertEquals("converted-",resume.getStringExtra("namingAddition"));assertFalse(resume.getBooleanExtra("namingPrefix",true));assertFalse(resume.getBooleanExtra("bframes",true));
        SavedSession.clear(context);assertNull(SavedSession.load(context));
    }

    @Test public void interruptedAtomicWriteKeepsPreviousSessionAndCompletedQueueOnlyRestoresReport()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Intent request=BatchRetryTest.queue(context);ConversionService.State s=sample(request);
        s.items.set(1,new CompressionSummary.Item(2,s.names.get(1),CompressionSummary.Status.NO_SAVING,100,150));s.recoverable=false;
        SavedSession.save(context,request,s);
        AtomicFile file=new AtomicFile(new File(context.getFilesDir(),"conversion-session.json"));
        FileOutputStream interrupted=file.startWrite();interrupted.write("partial".getBytes(java.nio.charset.StandardCharsets.UTF_8));file.failWrite(interrupted);
        SavedSession saved=SavedSession.load(context);assertFalse(saved.state.recoverable);assertFalse(saved.hasPending());assertEquals(1,saved.state.succeeded);assertEquals(1,saved.state.failed);assertEquals(1,saved.state.notSaved);
        assertTrue(saved.state.sizeReport.contains("Nessun risparmio"));assertEquals(s.outputUris,saved.state.outputUris);
    }

    @Test public void invalidSessionIsReportedAndPreservedForDiagnosis()throws Exception{
        Context context=RuntimeEnvironment.getApplication();File file=new File(context.getFilesDir(),"conversion-session.json");
        try(OutputStream out=new FileOutputStream(file)){out.write("invalid json".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        try{SavedSession.load(context);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("non leggibile"));}
        assertTrue(file.exists());
    }
}
