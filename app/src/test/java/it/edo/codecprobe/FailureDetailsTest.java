package it.edo.codecprobe;

import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class FailureDetailsTest {
    @Test public void absentOrBlankMessagesFallBackToTheRealExceptionType() {
        for(String message:new String[]{null,"","  "}) {
            ConversionFailure failure=new ConversionFailure(1,"clip","lettura",new IOException(message));
            assertEquals("IOException",failure.message);assertTrue(failure.details.contains("IOException"));
        }
    }
    @Test public void rootCauseAndRollbackFailureBothRemainCopyable() {
        IOException cause=new IOException("source absent");IOException error=new IOException("publication failed",cause);
        error.addSuppressed(new IOException("rollback denied"));
        String copied=new ConversionFailure(2,"clip","salvataggio",error).copyText();
        assertTrue(copied.contains("publication failed"));assertTrue(copied.contains("source absent"));
        assertTrue(copied.contains("rollback denied"));assertTrue(copied.contains("salvataggio"));
        assertTrue(copied.contains(BuildConfig.VERSION_NAME));
    }
    @Test public void multilineFilenameCannotInjectAnExtraDiagnosticHeading() {
        ConversionFailure error=new ConversionFailure(3,"clip\nFORGED\rTITLE","lettura",new IOException("unreadable"));
        assertEquals("Video 3 · clip FORGED TITLE",error.title());
    }
    @Test public void cancellationDetailsAreNotRewrittenAsAnUnrelatedIoError() {
        ConversionFailure failure=new ConversionFailure(1,"clip","lettura",new InterruptedIOException("cancelled"));
        assertEquals("cancelled",failure.message);assertTrue(failure.details.contains("InterruptedIOException"));
    }
}
