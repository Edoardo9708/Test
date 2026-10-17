package it.edo.codecprobe;

import android.content.Context;
import android.os.SystemClock;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;

/** Worker-owned bounded TXT with a short UI tail. No packet/sample-by-sample logging. */
final class AudioLog implements Closeable {
    static final int LIMIT=2*1024*1024;
    private final OutputStream output;
    private final long start=SystemClock.elapsedRealtime();
    private final ArrayDeque<String> tail=new ArrayDeque<>();
    private int bytes;
    private boolean capped;
    static File file(Context c){return new File(c.getFilesDir(),"audio-conversion-last.txt");}
    AudioLog(Context c)throws IOException{output=new BufferedOutputStream(new FileOutputStream(file(c)),16384);}
    void write(String message)throws IOException{
        String line="["+(SystemClock.elapsedRealtime()-start)+" ms] "+message+"\n";
        if(line.length()>12000)line=line.substring(0,12000)+"\n[Dettaglio abbreviato]\n";
        byte[] data=line.getBytes(StandardCharsets.UTF_8);
        if(!capped&&bytes+data.length>LIMIT){capped=true;output.write("\n*** LIMITE LOG RAGGIUNTO · ulteriori righe solo nel riepilogo a schermo ***\n".getBytes(StandardCharsets.UTF_8));}
        if(!capped){output.write(data);bytes+=data.length;output.flush();}
        tail.addLast(line);while(tail.size()>12)tail.removeFirst();
    }
    String tail(){return String.join("\n",tail);}
    public void close()throws IOException{output.close();}
}
