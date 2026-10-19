package it.edo.codecprobe;

import org.junit.Test;
import static org.junit.Assert.*;

public class MetadataMemoryCacheTest {
    @Test public void byteBudgetEvictsLeastRecentlyUsedMetadata() {
        MetadataMemoryCache<String> cache=new MetadataMemoryCache<>(100,10);
        long epoch=cache.epoch();cache.put("a","first",4,epoch);cache.put("b","second",4,epoch);
        cache.get("a");cache.put("c","third",4,epoch);
        assertEquals(8,cache.bytes());assertEquals("first",cache.get("a"));assertNull(cache.get("b"));
        assertFalse(cache.put("huge","oversized",11,epoch));assertEquals(2,cache.size());
    }
    @Test public void reloadAndClearRejectInFlightOldReads() {
        MetadataMemoryCache<String> cache=new MetadataMemoryCache<>(2,100);
        long old=cache.epoch();cache.put("photo","before",1,old);cache.remove("photo");
        assertFalse(cache.put("photo","stale",1,old));
        long current=cache.epoch();cache.put("photo","after",1,current);cache.clear();
        assertFalse(cache.put("photo","late",1,current));assertEquals(0,cache.bytes());assertEquals(0,cache.size());
    }
}
