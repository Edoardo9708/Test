package it.edo.codecprobe;

import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.security.MessageDigest;
import java.util.function.BooleanSupplier;

/** Fixed-buffer fingerprints, never images in memory. */
final class ImageFingerprint {
    static String hex(MessageDigest digest){StringBuilder out=new StringBuilder(64);for(byte b:digest.digest()){out.append(Character.forDigit((b&255)>>>4,16));out.append(Character.forDigit(b&15,16));}return out.toString();}
    static String read(InputStream in,long limit,BooleanSupplier cancelled)throws IOException{
        if(in==null)throw new IOException("File non leggibile");MessageDigest digest=JpegInventory.digest();byte[] buffer=new byte[65536];long bytes=0;int n;
        while((n=in.read(buffer))!=-1){ImageConverter.check(cancelled);bytes+=n;if(bytes>limit)throw new IOException("Dimensione file cambiata");digest.update(buffer,0,n);}ImageConverter.check(cancelled);return hex(digest);
    }
    static String uri(Context c,String uri,long limit,BooleanSupplier cancelled)throws IOException{try(InputStream in=c.getContentResolver().openInputStream(Uri.parse(uri))){return read(in,limit,cancelled);}}
    static String file(File file,BooleanSupplier cancelled)throws IOException{try(InputStream in=new FileInputStream(file)){return read(in,file.length(),cancelled);}}
    private ImageFingerprint(){}
}
