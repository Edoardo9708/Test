package it.edo.codecprobe;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class SavingsBoundaryTest {
    @Test public void unknownAndExtremeSizesCannotBecomeFalseSavings() {
        for(long unknown:new long[]{Long.MIN_VALUE,-1,0}) {
            assertEquals(SavingsPolicy.Decision.UNKNOWN_SIZE,SavingsPolicy.decide(true,unknown,1));
            assertEquals(SavingsPolicy.Decision.UNKNOWN_SIZE,SavingsPolicy.decide(true,1,unknown));
        }
        assertEquals(SavingsPolicy.Decision.NO_SAVING,SavingsPolicy.decide(true,Long.MAX_VALUE,Long.MAX_VALUE));
        assertEquals(SavingsPolicy.Decision.NO_SAVING,SavingsPolicy.decide(true,1,Long.MAX_VALUE));
    }
    @Test public void notSavedReasonsKeepTheirCategoryAndOriginalMeasurements() {
        for(SavingsPolicy.Decision reason:List.of(SavingsPolicy.Decision.NO_SAVING,SavingsPolicy.Decision.UNKNOWN_SIZE)) {
            SavingsPolicy.NotSaved error=new SavingsPolicy.NotSaved(reason,-1,Long.MAX_VALUE);
            assertSame(reason,error.decision);assertEquals(-1,error.original);assertEquals(Long.MAX_VALUE,error.converted);
            assertFalse(error.getMessage().isBlank());
        }
    }
    @Test public void overflowingTotalsAreUnavailableRatherThanNegativeOrInvented() {
        String report=CompressionSummary.render(List.of(
            new CompressionSummary.Item(1,"one",CompressionSummary.Status.COMPLETE,Long.MAX_VALUE,1),
            new CompressionSummary.Item(2,"two",CompressionSummary.Status.COMPLETE,1,1)));
        String total=report.substring(report.indexOf("TOTALE ·"));
        assertTrue(total.contains("Confronto dimensioni non disponibile"));assertFalse(total.contains("Spazio risparmiato"));
        assertEquals("",CompressionSummary.render(List.of()));
    }
}
