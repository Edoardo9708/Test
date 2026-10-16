package it.edo.codecprobe;

import android.content.Context;
import android.content.Intent;
import java.io.IOException;
import java.util.HexFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class SamsungSefReportTest {
    @Test public void fullDiagnosticSurvivesFailureCopyAndSavedSessionWithoutRecentLog()throws Exception{
        byte[] bytes=HexFormat.of().parseHex("00000054736566640000510c1400000053616d73756e675f436170747572655f496e666f53637265656e207265636f7264696e67534546486b000000010000000000510c2c0000002c0000001800000053454654");
        // Structurally consistent, but the known capture name has an unexpected 0BA1 type.
        bytes[10]=(byte)0xa1;bytes[11]=0x0b;bytes[66]=(byte)0xa1;bytes[67]=0x0b;
        IOException diagnostic;
        try{SamsungSef.validate(bytes,8000);throw new AssertionError("Unsupported record accepted");}
        catch(IOException expected){diagnostic=expected;}
        Context context=RuntimeEnvironment.getApplication();Intent request=BatchRetryTest.queue(context);
        ConversionService.State state=SavedSessionTest.sample(request);
        ConversionFailure failure=new ConversionFailure(3,state.names.get(2),"Analisi video 3 / 3",diagnostic);
        assertTrue(failure.copyText().contains("Samsung_Capture_Info"));assertTrue(failure.copyText().contains("0x0BA1 (2977)"));
        assertTrue(failure.copyText().contains("record=8008"));assertTrue(failure.copyText().contains("SHA-256 payload"));
        assertTrue(failure.copyText().contains("Primi 16 byte"));assertTrue(failure.copyText().contains("tipo inatteso"));
        state.failures.set(0,failure);state.log="";
        try{
            SavedSession.save(context,request,state);SavedSession saved=SavedSession.load(context);
            assertEquals(failure.copyText(),saved.state.failures.get(0).copyText());
            assertEquals("",saved.state.log);
        }finally{SavedSession.clear(context);}
    }
}
