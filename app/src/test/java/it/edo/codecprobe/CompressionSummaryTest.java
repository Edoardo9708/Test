package it.edo.codecprobe;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class CompressionSummaryTest {
    private CompressionSummary.Item ok(int number,long before,long after){
        return new CompressionSummary.Item(number,"Video.mp4",CompressionSummary.Status.COMPLETE,before,after);
    }
    @Test public void halfSizeIsMinusFifty(){
        String report=CompressionSummary.render(Arrays.asList(ok(1,10000000,5000000)));
        assertTrue(report.contains("10,00 MB → 5,00 MB · -50,0%"));
        assertTrue(report.contains("Spazio risparmiato: 5,00 MB"));
    }
    @Test public void growthIsPositiveAndUsesAdditionalSpace(){
        String report=CompressionSummary.render(Arrays.asList(ok(1,10000000,12000000)));
        assertTrue(report.contains("+20,0%"));assertTrue(report.contains("Spazio aggiuntivo: 2,00 MB"));
        assertFalse(report.contains("Spazio risparmiato:"));
    }
    @Test public void totalsUseBytesNotAverageOfPercentages(){
        String report=CompressionSummary.render(Arrays.asList(ok(1,10000000,5000000),ok(2,100000000,90000000),
            new CompressionSummary.Item(3,"fallito",CompressionSummary.Status.FAILED,900000000,-1)));
        assertTrue(report.contains("110,00 MB → 95,00 MB · -13,6%"));
        assertTrue(report.contains("Conversione fallita"));assertTrue(report.contains("Spazio risparmiato: 15,00 MB"));
    }
    @Test public void unknownSizeIsExcludedAndReported(){
        String report=CompressionSummary.render(Arrays.asList(ok(1,-1,2000000),ok(2,10000000,5000000)));
        assertTrue(report.contains("dimensione non disponibile"));assertTrue(report.contains("variazione non calcolabile"));
        assertTrue(report.contains("Totale calcolato su 1 di 2"));
    }
    @Test public void zeroInputDoesNotDivideByZero(){
        assertEquals("variazione non calcolabile",CompressionSummary.change(0,100));
        assertEquals("0,0%",CompressionSummary.change(100,100));
    }
    @Test public void cancelledAndSkippedHaveNoInventedSavings(){
        String report=CompressionSummary.render(Arrays.asList(
            new CompressionSummary.Item(1,"a",CompressionSummary.Status.CANCELLED,-1,-1),
            new CompressionSummary.Item(2,"b",CompressionSummary.Status.SKIPPED,-1,-1)));
        assertTrue(report.contains("Conversione annullata"));assertTrue(report.contains("Non convertito"));
        assertTrue(report.contains("Confronto dimensioni non disponibile"));assertFalse(report.contains("-100"));
    }
    @Test public void runningReportIncludesFinishedVideoAndDistinguishesPendingFiles(){
        String report=CompressionSummary.render(Arrays.asList(ok(1,10000000,5000000),
            new CompressionSummary.Item(2,"secondo",CompressionSummary.Status.IN_PROGRESS,-1,-1),
            new CompressionSummary.Item(3,"terzo",CompressionSummary.Status.SKIPPED,-1,-1)),true);
        assertTrue(report.contains("10,00 MB → 5,00 MB · -50,0%"));
        assertTrue(report.contains("Video 2 · secondo\nIn corso"));
        assertTrue(report.contains("Video 3 · terzo\nIn attesa"));
        assertTrue(report.contains("TOTALE PARZIALE"));assertTrue(report.contains("Spazio risparmiato: 5,00 MB"));
        assertFalse(report.contains("Non convertito"));
    }
    @Test public void finalReportDoesNotLeaveFilesRunningOrWaiting(){
        String report=CompressionSummary.render(Arrays.asList(ok(1,10000000,5000000),
            new CompressionSummary.Item(2,"secondo",CompressionSummary.Status.IN_PROGRESS,-1,-1),
            new CompressionSummary.Item(3,"terzo",CompressionSummary.Status.SKIPPED,-1,-1)));
        assertFalse(report.contains("In corso"));assertFalse(report.contains("In attesa"));assertFalse(report.contains("TOTALE PARZIALE"));
        assertTrue(report.contains("Non completato"));assertTrue(report.contains("Non convertito"));
    }
    @Test public void initialReportWaitsForActualCompletedOutput(){
        String report=CompressionSummary.render(Arrays.asList(new CompressionSummary.Item(1,"primo",CompressionSummary.Status.IN_PROGRESS,-1,-1)),true);
        assertTrue(report.contains("In attesa del primo video completato"));assertFalse(report.contains("Spazio risparmiato:"));
    }
}
