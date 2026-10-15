package it.edo.codecprobe;

import java.util.LinkedHashMap;
import java.util.Map;

/** Process-local metadata only. No Activity, View, decoder or video buffers retained. */
final class VideoMetadataCache {
    static final int LIMIT=100;
    static final class Entry {
        final String name,quick;
        final InputInspector.Report report;
        Entry(String name,String quick,InputInspector.Report report){this.name=name;this.quick=quick;this.report=report;}
    }
    private static final Map<String,Entry> entries=new LinkedHashMap<String,Entry>(16,0.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,VideoMetadataCache.Entry> eldest){return size()>LIMIT;}
    };
    private static String permissions="";
    private static long epoch;
    static synchronized void permissions(String value){if(!permissions.equals(value)){entries.clear();permissions=value;epoch++;}}
    static synchronized Entry get(String uri){return entries.get(uri);}
    static synchronized long epoch(){return epoch;}
    static synchronized boolean put(String uri,Entry entry,long expected){if(expected!=epoch)return false;entries.put(uri,entry);return true;}
    static synchronized void clear(){entries.clear();epoch++;}
    static synchronized int size(){return entries.size();}
    private VideoMetadataCache(){}
}
