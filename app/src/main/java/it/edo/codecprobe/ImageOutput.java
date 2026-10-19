package it.edo.codecprobe;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Publish a new HEIC, then re-read every saved byte. Roll back a partial/changed document. */
final class ImageOutput {
    static String name(String original){String value=original.replace('/','_').replace('\\','_').replace('\n','_').replace('\r','_').trim();int dot=value.lastIndexOf('.');if(dot>0)value=value.substring(0,dot);if(value.isEmpty())value="Foto";if(value.length()>140)value=value.substring(0,140);return value+".heic";}
    static String unique(String wanted,Set<String> existing){String base=wanted.substring(0,wanted.length()-5);for(int n=1;n<100000;n++){String candidate=n==1?wanted:base+" ("+n+").heic";boolean found=false;for(String value:existing)if(candidate.equalsIgnoreCase(value)){found=true;break;}if(!found)return candidate;}throw new IllegalStateException("Troppi nomi HEIC uguali");}
    static Uri save(Context c,Uri tree,File source,String original,BooleanSupplier cancelled)throws IOException{
        Uri document=null;
        try{
            Set<String> names=OutputDocuments.names(c,tree);String chosen=unique(name(original),names);ImageConverter.check(cancelled);
            document=DocumentsContract.createDocument(c.getContentResolver(),DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree)),"image/heic",chosen);if(document==null)throw new IOException("Creazione HEIC fallita");
            MessageDigest expected=JpegInventory.digest();long written=0;
            try(InputStream in=new FileInputStream(source);OutputStream out=c.getContentResolver().openOutputStream(document,"w")){
                if(out==null)throw new IOException("HEIC non scrivibile");byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){ImageConverter.check(cancelled);out.write(buffer,0,n);expected.update(buffer,0,n);written+=n;}
            }
            MessageDigest actual=JpegInventory.digest();long read=0;try(InputStream in=c.getContentResolver().openInputStream(document)){
                if(in==null)throw new IOException("HEIC salvato non rileggibile");byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){ImageConverter.check(cancelled);actual.update(buffer,0,n);read+=n;if(read>written)throw new IOException("HEIC salvato più lungo dell’output");}
            }
            ImageConverter.check(cancelled);if(written!=source.length()||read!=written||!Arrays.equals(expected.digest(),actual.digest()))throw new IOException("HEIC salvato diverso dall’output verificato");return document;
        }catch(Exception error){if(document!=null)try{if(!DocumentsContract.deleteDocument(c.getContentResolver(),document))error.addSuppressed(new IOException("Eliminazione HEIC parziale fallita"));}catch(Exception cleanup){error.addSuppressed(cleanup);}if(error instanceof IOException)throw(IOException)error;throw new IOException("Salvataggio HEIC fallito: "+error.getMessage(),error);}
    }
    private ImageOutput(){}
}
