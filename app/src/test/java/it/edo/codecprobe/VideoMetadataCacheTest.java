package it.edo.codecprobe;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.LinkedHashMap;
import java.util.Collections;

public class VideoMetadataCacheTest {
    private VideoMetadataCache.Entry entry(String name){
        return new VideoMetadataCache.Entry(name,"Video: 1 · Audio: 0",new InputInspector.Report("riepilogo","dettagli"));
    }
    @Test public void reusedEntryDoesNotLoseDetails(){
        VideoMetadataCache.clear();VideoMetadataCache.permissions("read");
        VideoMetadataCache.Entry original=entry("primo.mp4");
        assertTrue(VideoMetadataCache.put("one",original,VideoMetadataCache.epoch()));
        assertSame(original,VideoMetadataCache.get("one"));
        assertTrue(VideoMetadataCache.get("one").report.details.contains("dettagli"));
    }
    @Test public void permissionChangeInvalidatesGpsAndRejectsLateResult(){
        VideoMetadataCache.clear();VideoMetadataCache.permissions("redacted");long old=VideoMetadataCache.epoch();
        VideoMetadataCache.put("one",entry("one"),old);VideoMetadataCache.permissions("original");
        assertNull(VideoMetadataCache.get("one"));
        assertFalse(VideoMetadataCache.put("one",entry("stale"),old));
        assertEquals(0,VideoMetadataCache.size());
    }
    @Test public void explicitReloadRejectsOlderWorker(){
        VideoMetadataCache.clear();long old=VideoMetadataCache.epoch();VideoMetadataCache.clear();
        assertFalse(VideoMetadataCache.put("one",entry("stale"),old));
    }
    @Test public void boundedCacheRetainsRecentlyViewedEntry(){
        VideoMetadataCache.clear();long epoch=VideoMetadataCache.epoch();
        for(int n=0;n<100;n++)VideoMetadataCache.put("v"+n,entry("name"),epoch);
        VideoMetadataCache.get("v0");VideoMetadataCache.put("new",entry("name"),epoch);
        assertEquals(100,VideoMetadataCache.size());assertNotNull(VideoMetadataCache.get("v0"));assertNull(VideoMetadataCache.get("v1"));
    }
    @Test public void reportCopiesMapsAndPreservesErrorStatus(){
        LinkedHashMap<String,String> source=new LinkedHashMap<>();source.put("VIDEO","errore");
        InputInspector.Report report=new InputInspector.Report(source,Collections.emptyMap(),true);
        source.clear();assertEquals("errore",report.summarySections.get("VIDEO"));assertTrue(report.readError);
    }
}
