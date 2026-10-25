package it.edo.codecprobe;

import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.util.function.*;

/** Private staging → bundled engine → verified HEIC → SAF publication. */
final class ImageConverter {
    static ImageRunState.Result convert(Context c,Uri input,String name,ImageOptions options,BooleanSupplier cancelled,Consumer<String> stage)throws IOException{
        return convert(c,input,name,options,cancelled,stage,new ImageOutput.Names());
    }
    static ImageRunState.Result convert(Context c,Uri input,String name,ImageOptions options,BooleanSupplier cancelled,Consumer<String> stage,ImageOutput.Names names)throws IOException{
        File source=File.createTempFile("image-source-",".tmp",c.getCacheDir()),output=null;
        try{
            output=File.createTempFile("image-heic-",".tmp",c.getCacheDir());stage.accept("Lettura JPEG originale");
            java.security.MessageDigest sourceDigest=JpegInventory.digest();
            long bytes=0;try(InputStream in=c.getContentResolver().openInputStream(input);OutputStream out=new FileOutputStream(source)){
                if(in==null)throw new IOException("JPEG non accessibile");byte[] buffer=new byte[65536];int n;
                while((n=in.read(buffer))!=-1){check(cancelled);bytes+=n;if(bytes>64L*1024*1024)throw new IOException("JPEG oltre il limite di 64 MiB per la conversione");out.write(buffer,0,n);sourceDigest.update(buffer,0,n);}
            }
            String sourceSha=ImageFingerprint.hex(sourceDigest);
            check(cancelled);String details;
            try{details=ImageNative.convert(source.getAbsolutePath(),output.getAbsolutePath(),options.quality,options.preset,options.tune,text->{check(cancelled);stage.accept(text);});}
            catch(LinkageError error){throw new IOException("Motore immagini incluso non caricabile",error);}
            check(cancelled);if(output.length()==0)throw new IOException("Output HEIC vuoto");
            return publish(c,source,output,name,options,details,sourceSha,cancelled,stage,names);
        }finally{source.delete();if(output!=null)output.delete();}
    }
    /** Select and publish the verified HEIC or the unchanged staged JPEG. */
    static ImageRunState.Result publish(Context c,File source,File output,String name,ImageOptions options,String details,String sourceSha,BooleanSupplier cancelled,Consumer<String> stage,ImageOutput.Names names)throws IOException{
        check(cancelled);long bytes=source.length();
        if(options.onlySmaller&&output.length()>=bytes){
            if(!options.copyOriginalWhenLarger)return new ImageRunState.Result(name,bytes,output.length(),null,"Non salvato: nessun risparmio",ImageRunState.Outcome.NOT_SMALLER,details,sourceSha,"");
            stage.accept("Copia e verifica JPEG originale");
            Uri saved=ImageOutput.copyOriginal(c,Uri.parse(options.folder),source,name,cancelled,names);
            return new ImageRunState.Result(name,bytes,bytes,saved.toString(),"Originale copiato: HEIC non più piccolo",ImageRunState.Outcome.COPIED_ORIGINAL,details+"\nHEIC non salvato ("+output.length()+" byte). JPEG originale copiato integralmente e riletto con verifica SHA-256.",sourceSha,sourceSha,"image/jpeg");
        }
        String outputSha=ImageFingerprint.file(output,cancelled);
        stage.accept("Salvataggio HEIC verificato");Uri saved=ImageOutput.save(c,Uri.parse(options.folder),output,name,cancelled,names);
        return new ImageRunState.Result(name,bytes,output.length(),saved.toString(),"Salvato",ImageRunState.Outcome.SAVED,details,sourceSha,outputSha);
    }
    static void check(BooleanSupplier cancelled)throws IOException{if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Conversione immagini annullata");}
    private ImageConverter(){}
}
