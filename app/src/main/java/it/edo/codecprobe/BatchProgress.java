package it.edo.codecprobe;

/** Queue progress assigns equal weight to each file; it is not an ETA. */
final class BatchProgress {
    static final int MAX_FILES=100;
    static int percent(int completed,int currentPercent,int count){
        if(count<=0)return 0;
        long units=(long)Math.max(0,Math.min(count,completed))*100+Math.max(0,Math.min(100,currentPercent));
        return (int)Math.min(100,units/count);
    }
}
