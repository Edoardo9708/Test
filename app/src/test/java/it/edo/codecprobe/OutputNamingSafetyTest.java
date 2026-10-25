package it.edo.codecprobe;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class OutputNamingSafetyTest {
    @Test public void emptyDirectoryAndEmptyQueueDoNotInventCollisions() {
        OutputPlan plan=new OutputPlan(0);plan.directory("empty",Set.of());
        assertFalse(plan.conflicts("empty","clip.mp4"));assertEquals("clip.mp4",plan.suggestion("empty","clip.mp4"));
        assertTrue(plan.names.isEmpty());assertEquals(0,plan.omitted.length);
    }
    @Test public void reservationsDoNotMutateTheProviderListingOrAnotherDirectory() {
        Set<String> listing=new HashSet<>(Set.of("CLIP.MP4"));OutputPlan plan=new OutputPlan(2);
        plan.directory("one",listing);plan.directory("two",listing);listing.clear();
        plan.accept("one","clip (2).mp4");
        assertTrue(plan.conflicts("one","clip.mp4"));assertTrue(listing.isEmpty());
        assertFalse(plan.conflicts("two","clip (2).mp4"));assertEquals("clip (3).mp4",plan.suggestion("one","clip.mp4"));
    }
    @Test public void firstAvailableSuffixIsChosenWithoutModifyingReservations() {
        Set<String> occupied=Set.of("film.mp4","film (2).mp4","film (4).mp4");
        assertEquals("film (3).mp4",OutputName.unique("film.mp4",occupied));
        assertEquals(3,occupied.size());assertEquals("film (3).mp4",OutputName.unique("film.mp4",occupied));
    }
    @Test public void collisionComparisonDoesNotDependOnTurkishDefaultLocale() {
        Locale before=Locale.getDefault();
        try { Locale.setDefault(Locale.forLanguageTag("tr-TR"));OutputPlan plan=new OutputPlan(1);
            plan.directory("one",Set.of("INDICE.MP4"));assertTrue(plan.conflicts("one","indice.mp4"));
            assertEquals("indice (2).mp4",plan.suggestion("one","indice.mp4"));
        } finally { Locale.setDefault(before); }
    }
    @Test public void manualUtf8LimitAccepts240BytesAndRejects242Bytes() {
        String boundary="à".repeat(118)+".mp4";assertEquals(240,boundary.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(boundary,OutputPlan.manual(boundary));
        assertThrows(IllegalArgumentException.class,()->OutputPlan.manual("à".repeat(119)+".mp4"));
    }
    @Test public void generatedNamesKeepWholeUnicodeCodePointsAndRemoveTraversal() {
        String result=OutputName.base("🌈".repeat(100)+".mov","../",true);
        assertTrue(result.endsWith(".mp4"));assertTrue(result.getBytes(StandardCharsets.UTF_8).length<=224);
        assertFalse(result.contains("/"));assertFalse(result.contains("\\"));
        assertEquals(result,new String(result.getBytes(StandardCharsets.UTF_8),StandardCharsets.UTF_8));
    }
    @Test public void rejectedManualNameCannotConsumeAQueueSlot() {
        OutputPlan plan=new OutputPlan(1);plan.directory("one",Set.of());
        assertThrows(IllegalArgumentException.class,()->plan.accept("one",OutputPlan.manual("../clip")));
        assertTrue(plan.names.isEmpty());assertFalse(plan.conflicts("one","clip.mp4"));assertFalse(plan.omitted[0]);
    }
}
