package it.edo.codecprobe;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class OutputPlanTest {
    @Test public void existingNamesAndQueueReservationsShareNamespace() {
        OutputPlan plan=new OutputPlan(4);
        plan.directory("camera",Set.of("Clip.mp4","clip (2).MP4"));
        plan.directory("movies",Set.of());
        assertTrue(plan.conflicts("camera","CLIP.MP4"));
        assertEquals("clip (3).mp4",plan.suggestion("camera","clip.mp4"));
        plan.accept("camera","clip (3).mp4");
        assertTrue(plan.conflicts("camera","CLIP (3).MP4"));
        assertFalse(plan.conflicts("movies","clip (3).mp4"));
        plan.skip();plan.accept("movies","clip.mp4");
        assertTrue(plan.omitted[1]);assertFalse(plan.omitted[0]);
        assertEquals(Arrays.asList("clip (3).mp4","","clip.mp4"),plan.names);
        // A later provider read must not discard earlier queue reservations.
        plan.directory("camera",Set.of());assertTrue(plan.conflicts("camera","clip (3).mp4"));
    }
    @Test public void manualNamesKeepMp4AndRejectPathsAndOversizedNames() {
        assertEquals("nuovo.mp4",OutputPlan.manual(" nuovo "));
        assertEquals("nuovo.MP4",OutputPlan.manual("nuovo.MP4"));
        for(String name:new String[]{"",".mp4","../video","a\\b","a\nb","a:b","à".repeat(121)}) {
            try {OutputPlan.manual(name);fail(name);}catch(IllegalArgumentException expected){}
        }
    }
    @Test public void omittedFilesAreReportedWithoutSavingsOrFailure() {
        String report=CompressionSummary.render(List.of(new CompressionSummary.Item(1,"clip.mp4",CompressionSummary.Status.OMITTED,100,50)));
        assertTrue(report.contains("Ignorato su richiesta"));assertFalse(report.contains("50,0%"));assertFalse(report.contains("fallita"));
    }
}
