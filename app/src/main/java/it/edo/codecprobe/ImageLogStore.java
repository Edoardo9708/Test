package it.edo.codecprobe;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Serializes complete events and local snapshots; slow document providers never hold the lock. */
final class ImageLogStore {
    private static final Object LOCK=new Object();
    static void reset(Context context)throws IOException{
        synchronized(LOCK){try(FileOutputStream out=new FileOutputStream(source(context,ImageConversionService.LOG),false)){out.getFD().sync();}}
    }
    static void append(Context context,String filename,String text)throws IOException{
        synchronized(LOCK){try(FileOutputStream out=new FileOutputStream(source(context,filename),true)){out.write(text.getBytes(StandardCharsets.UTF_8));out.getFD().sync();}}
    }
    static boolean clear(Context context){synchronized(LOCK){File file=source(context,ImageConversionService.LOG);return !file.exists()||file.delete();}}
    static File snapshot(Context context,String filename)throws IOException{
        synchronized(LOCK){
            File snapshot=File.createTempFile("image-log-snapshot-",".tmp",context.getCacheDir());boolean complete=false;
            try{
                try(InputStream in=new FileInputStream(source(context,filename));FileOutputStream out=new FileOutputStream(snapshot)){
                    copy(in,out);out.getFD().sync();
                }
                complete=true;return snapshot;
            }finally{if(!complete)snapshot.delete();}
        }
    }
    static File restoredSnapshot(Context context,String name){
        if(name==null||!name.matches("image-log-snapshot-[0-9]+\\.tmp"))return null;
        File file=new File(context.getCacheDir(),name);return file.isFile()?file:null;
    }
    static void copy(InputStream in,OutputStream out)throws IOException{
        byte[] buffer=new byte[65536];int count;
        while((count=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Esportazione interrotta");out.write(buffer,0,count);}
    }
    private static File source(Context context,String filename){
        if(!ImageConversionService.LOG.equals(filename)&&!ImageFailureStore.FILE.equals(filename))throw new IllegalArgumentException("Archivio immagini non valido");
        return new File(context.getFilesDir(),filename);
    }
    private ImageLogStore(){}
}
