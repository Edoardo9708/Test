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
    static String name(String original){return name(original,false,"",false);}
    static String name(String original,boolean motion){return name(original,motion,"",false);}
    static String name(String original,boolean motion,String addition,boolean prefix){
        String value=original.replace('/','_').replace('\\','_').replace('\n','_').replace('\r','_').trim();int dot=value.lastIndexOf('.');if(dot>0)value=value.substring(0,dot);if(value.isEmpty())value="Foto";
        String extra=addition==null?"":addition.replace('/','_').replace('\\','_').replace('\n','_').replace('\r','_').trim();if(extra.length()>80)extra=extra.substring(0,80);
        String marker=motion&&!value.endsWith("MP")?"_MP":"";int baseLimit=Math.max(1,140-extra.length()-marker.length());if(value.length()>baseLimit)value=value.substring(0,baseLimit);
        return (prefix?extra+value:value+extra)+marker+".heic";
    }
    static String unique(String wanted,Set<String> existing){Set<String> normalized=new HashSet<>();for(String value:existing)normalized.add(value.toLowerCase(Locale.ROOT));return choose(wanted,normalized);}
    private static String choose(String wanted,Set<String> existing){String base=wanted.substring(0,wanted.length()-5);String suffix=base.endsWith("_MP")?"_MP":base.endsWith("MP")?"MP":"";String stem=base.substring(0,base.length()-suffix.length());for(int n=1;n<100000;n++){String candidate=n==1?wanted:stem+" ("+n+")"+suffix+".heic";if(!existing.contains(candidate.toLowerCase(Locale.ROOT)))return candidate;}throw new IllegalStateException("Troppi nomi HEIC uguali");}
    /** Read only top-level headers: no pixels or video payload enter Java memory. */
    static boolean hasMotionVideo(File source)throws IOException{
        try(RandomAccessFile file=new RandomAccessFile(source,"r")){
            long end=file.length(),at=0;if(end<8)throw new IOException("HEIC vuoto o troncato");int boxes=0;boolean motion=false;
            while(at<end){
                if(end-at<8||++boxes>65536)throw new IOException("Box HEIC non valido");file.seek(at);
                long size=Integer.toUnsignedLong(file.readInt());int type=file.readInt();int header=8;
                if(size==1){if(end-at<16)throw new IOException("Header HEIC esteso troncato");size=file.readLong();header=16;}
                if(size<header||size>end-at)throw new IOException("Dimensione box HEIC non valida");
                if(type==0x6d707664){if(motion||at+size!=end)throw new IOException("Posizione video HEIC non valida");motion=true;}
                at+=size;
            }
            return motion;
        }
    }
    /** One directory listing per destination per run; SAF createDocument creates a new document. */
    static final class Names {
        private final Map<String,Set<String>> folders=new HashMap<>();
        String reserve(Context c,Uri tree,String wanted)throws IOException{
            String key=OutputDocuments.key(c,tree);
            Set<String> names=folders.get(key);
            if(names==null){names=OutputDocuments.names(c,tree);folders.put(key,names);}
            String chosen=choose(wanted,names);names.add(chosen.toLowerCase(Locale.ROOT));return chosen;
        }
    }
    static Uri save(Context c,Uri tree,File source,String original,BooleanSupplier cancelled)throws IOException{
        return save(c,tree,source,original,cancelled,new Names());
    }
    static Uri save(Context c,Uri tree,File source,String original,BooleanSupplier cancelled,Names names)throws IOException{
        return save(c,tree,source,original,"",false,cancelled,names);
    }
    static Uri save(Context c,Uri tree,File source,String original,String addition,boolean prefix,BooleanSupplier cancelled,Names names)throws IOException{
        Uri document=null;
        try{
            ImageConverter.check(cancelled);String chosen=names.reserve(c,tree,name(original,hasMotionVideo(source),addition,prefix));ImageConverter.check(cancelled);
            document=DocumentsContract.createDocument(c.getContentResolver(),DocumentsContract.buildDocumentUriUsingTree(tree,OutputDocuments.id(c,tree)),"image/heic",chosen);if(document==null)throw new IOException("Creazione HEIC fallita");
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
