package it.edo.codecprobe;

import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.util.function.*;

/** One bounded source at a time. No output URI, decoding, encoding or conversion journal. */
final class ImageMetadataCheck {
    static ImageRunState.Result inspect(Context c,Uri input,String name,BooleanSupplier cancelled,Consumer<String> stage)throws IOException{
        File source=File.createTempFile("image-source-",".tmp",c.getCacheDir());
        try{
            stage.accept("Lettura JPEG · verifica metadati senza codec");long bytes=0;
            try(InputStream in=c.getContentResolver().openInputStream(input);OutputStream out=new FileOutputStream(source)){
                if(in==null)throw new IOException("JPEG non accessibile");byte[] buffer=new byte[65536];int n;
                while((n=in.read(buffer))!=-1){ImageConverter.check(cancelled);bytes+=n;if(bytes>64L*1024*1024)throw new IOException("JPEG oltre il limite di 64 MiB per la conversione");out.write(buffer,0,n);}
            }
            ImageConverter.check(cancelled);String details;
            try{details=ImageNative.inspect(source.getAbsolutePath());}
            catch(LinkageError error){throw new IOException("Motore verifica metadati non caricabile",error);}
            ImageConverter.check(cancelled);
            return new ImageRunState.Result(name,bytes,0,null,"Metadati compatibili · pixel non verificati",ImageRunState.Outcome.CHECKED,details);
        }finally{source.delete();}
    }
    private ImageMetadataCheck(){}
}
