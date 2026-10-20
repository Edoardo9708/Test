package it.edo.codecprobe;

/** TIFF field coordinates and schema only; source bytes are read on demand. */
final class AnalysisField {
    final long entry, base, end, count, value, bytes;
    final int tag, type, unit;
    final boolean little, gps;
    final String directory;
    AnalysisField(long entry,long base,long end,int tag,int type,long count,int unit,long value,long bytes,boolean little,boolean gps){
        this(entry,base,end,tag,type,count,unit,value,bytes,little,gps?"GPS":"IFD0");
    }
    AnalysisField(long entry,long base,long end,int tag,int type,long count,int unit,long value,long bytes,boolean little,String directory){
        this.entry=entry;this.base=base;this.end=end;this.tag=tag;this.type=type;this.count=count;this.unit=unit;this.value=value;this.bytes=bytes;this.little=little;this.directory=directory;this.gps=directory.equals("GPS");
    }
}
