package it.edo.codecprobe;

/** Process-local metadata only. No Activity, View, decoder or video buffers retained. */
final class VideoMetadataCache {
    static final int LIMIT=100;
    static final class Entry {
        final String name,quick;
        final InputInspector.Report report;
        Entry(String name,String quick,InputInspector.Report report){this.name=name;this.quick=quick;this.report=report;}
    }
    private static final MetadataMemoryCache<Entry> entries = new MetadataMemoryCache<>(LIMIT, LIMIT);
    private static String permissions="";
    static synchronized void permissions(String value){if(!permissions.equals(value)){entries.clear();permissions=value;}}
    static Entry get(String uri){return entries.get(uri);}
    static long epoch(){return entries.epoch();}
    static boolean put(String uri,Entry entry,long expected){return entries.put(uri,entry,1,expected);}
    static void clear(){entries.clear();}
    static int size(){return entries.size();}
    private VideoMetadataCache(){}
}
