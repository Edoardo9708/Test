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
    static String name(String original,boolean motion){String value=name(original);return motion&&!value.endsWith("MP.heic")?value.substring(0,value.length()-5)+"_MP.heic":value;}
    static String unique(String wanted,Set<String> existing){Set<String> normalized=new HashSet<>();for(String value:existing)normalized.add(value.toLowerCase(Locale.ROOT));return choose(wanted,normalized);}
    private static String choose(String wanted,Set<String> existing){int dot=wanted.lastIndexOf('.');String extension=dot>0?wanted.substring(dot):"",base=dot>0?wanted.substring(0,dot):wanted;String suffix=extension.equalsIgnoreCase(".heic")?(base.endsWith("_MP")?"_MP":base.endsWith("MP")?"MP":""):"";String stem=base.substring(0,base.length()-suffix.length());for(int n=1;n<100000;n++){String candidate=n==1?wanted:stem+" ("+n+")"+suffix+extension;if(!existing.contains(candidate.toLowerCase(Locale.ROOT)))return candidate;}throw new IllegalStateException("Troppi nomi immagine uguali");}
    static String originalName(String original){String value=original.replace('/','_').replace('\\','_').replace('\n','_').replace('\r','_').trim();if(value.isEmpty())return "Foto.jpg";int dot=value.lastIndexOf('.');String extension=dot>0?value.substring(dot):".jpg",stem=dot>0?value.substring(0,dot):value;if(stem.length()>140)stem=stem.substring(0,140);return stem+extension;}
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
        return publish(c,tree,source,name(original,hasMotionVideo(source)),"image/heic","HEIC",cancelled,names);
    }
    static Uri copyOriginal(Context c,Uri tree,File source,String original,BooleanSupplier cancelled,Names names)throws IOException{
        return publish(c,tree,source,originalName(original),"image/jpeg","JPEG originale",cancelled,names);
    }
    private static Uri publish(Context c,Uri tree,File source,String wanted,String mime,String label,BooleanSupplier cancelled,Names names)throws IOException{
        Uri document=null;
        try{
            ImageConverter.check(cancelled);String chosen=names.reserve(c,tree,wanted);ImageConverter.check(cancelled);
            document=DocumentsContract.createDocument(c.getContentResolver(),DocumentsContract.buildDocumentUriUsingTree(tree,OutputDocuments.id(c,tree)),mime,chosen);if(document==null)throw new IOException("Creazione "+label+" fallita");
            MessageDigest expected=JpegInventory.digest();long written=0;
            try(InputStream in=new FileInputStream(source);OutputStream out=c.getContentResolver().openOutputStream(document,"w")){
                if(out==null)throw new IOException(label+" non scrivibile");byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){ImageConverter.check(cancelled);out.write(buffer,0,n);expected.update(buffer,0,n);written+=n;}
            }
            MessageDigest actual=JpegInventory.digest();long read=0;try(InputStream in=c.getContentResolver().openInputStream(document)){
                if(in==null)throw new IOException(label+" salvato non rileggibile");byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){ImageConverter.check(cancelled);actual.update(buffer,0,n);read+=n;if(read>written)throw new IOException(label+" salvato più lungo dell’output");}
            }
            ImageConverter.check(cancelled);if(written!=source.length()||read!=written||!Arrays.equals(expected.digest(),actual.digest()))throw new IOException(label+" salvato diverso dall’output verificato");return document;
        }catch(Exception error){if(document!=null)try{if(!DocumentsContract.deleteDocument(c.getContentResolver(),document))error.addSuppressed(new IOException("Eliminazione "+label+" parziale fallita"));}catch(Exception cleanup){error.addSuppressed(cleanup);}if(error instanceof IOException)throw(IOException)error;throw new IOException("Salvataggio "+label+" fallito: "+error.getMessage(),error);}
    }
    private ImageOutput(){}
}
