package it.edo.codecprobe;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.SystemClock;
import java.io.*;
import java.util.Locale;

/** Low-rate measurements on the existing conversion worker; no timer thread or pixel storage. */
final class ConversionResources {
    private final Context context;
    private long sampledAt=-1;
    ConversionResources(Context context){this.context=context;}
    String periodic(){long now=SystemClock.elapsedRealtime();return sampledAt<0||now-sampledAt>=60000?capture("periodico"):"";}
    String capture(String phase){
        sampledAt=SystemClock.elapsedRealtime();
        try{
            Debug.MemoryInfo memory=new Debug.MemoryInfo();Debug.getMemoryInfo(memory);
            ActivityManager.MemoryInfo system=new ActivityManager.MemoryInfo();context.getSystemService(ActivityManager.class).getMemoryInfo(system);
            Runtime runtime=Runtime.getRuntime();
            return String.format(Locale.ITALY,"RISORSE · %s · pid=%d · thread_processo=%s\nPSS processo=%.1f MiB · heap Java usato=%.1f MiB · heap allocato dal runtime C/C++=%.1f MiB\nRAM disponibile sistema=%.1f MiB · lowMemory=%s\nPSS include quote condivise; i due heap non comprendono tutte le allocazioni del processo.",phase,android.os.Process.myPid(),threads(),memory.getTotalPss()/1024.0,(runtime.totalMemory()-runtime.freeMemory())/1048576.0,Debug.getNativeHeapAllocatedSize()/1048576.0,system.availMem/1048576.0,system.lowMemory);
        }catch(RuntimeException|LinkageError error){return "RISORSE · "+phase+" · lettura non disponibile: "+error.getClass().getSimpleName();}
    }
    private static String threads(){
        try(BufferedReader input=new BufferedReader(new FileReader("/proc/self/status"))){String line;while((line=input.readLine())!=null)if(line.startsWith("Threads:"))return line.substring(8).trim();}
        catch(IOException|SecurityException ignored){}
        return "non disponibile";
    }
}
