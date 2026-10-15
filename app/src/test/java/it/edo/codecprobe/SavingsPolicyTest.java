package it.edo.codecprobe;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SavingsPolicyTest {
    @Test public void onlyStrictlySmallerKnownFilesPassTheEnabledGate(){
        assertEquals(SavingsPolicy.Decision.SAVE,SavingsPolicy.decide(true,100,99));
        assertEquals(SavingsPolicy.Decision.NO_SAVING,SavingsPolicy.decide(true,100,100));
        assertEquals(SavingsPolicy.Decision.NO_SAVING,SavingsPolicy.decide(true,100,101));
        assertEquals(SavingsPolicy.Decision.UNKNOWN_SIZE,SavingsPolicy.decide(true,-1,99));
        assertEquals(SavingsPolicy.Decision.UNKNOWN_SIZE,SavingsPolicy.decide(true,100,0));
        assertEquals(SavingsPolicy.Decision.SAVE,SavingsPolicy.decide(false,-1,200));
        assertEquals(SavingsPolicy.Decision.SAVE,SavingsPolicy.decide(true,Long.MAX_VALUE,Long.MAX_VALUE-1));
    }
    @Test public void unsavedCandidatesAreExcludedFromActualSavingsTotals(){
        List<CompressionSummary.Item> items=List.of(
            new CompressionSummary.Item(1,"saved",CompressionSummary.Status.COMPLETE,10000000,5000000),
            new CompressionSummary.Item(2,"larger",CompressionSummary.Status.NO_SAVING,10000000,11000000),
            new CompressionSummary.Item(3,"unknown",CompressionSummary.Status.SIZE_UNKNOWN,-1,9000000));
        String report=CompressionSummary.render(items);
        assertTrue(report.contains("Nessun risparmio · risultato non salvato"));
        assertTrue(report.contains("Confronto dimensioni non disponibile"));
        String total=report.substring(report.indexOf("TOTALE ·"));
        assertTrue(total.contains("10,00 MB → 5,00 MB"));assertFalse(total.contains("11,00"));assertFalse(total.contains("9,00"));
    }
}
