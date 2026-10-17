package it.edo.codecprobe;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Explicit SAF sidecars. Matching is exact by stem, preferring the original document's parent. */
final class AudioLyrics {
    static final int LIMIT=2*1024*1024;
    static String stem(String name){int dot=name.lastIndexOf('.');return dot>0?name.substring(0,dot):name;}
    static boolean isLyrics(String name){return name.toLowerCase(Locale.ROOT).endsWith(".lrc");}
    private static String parent(String value){
        try{Uri uri=Uri.parse(value);String id=DocumentsContract.getDocumentId(uri);int slash=id.lastIndexOf('/');return uri.getAuthority()+":"+(slash>=0?id.substring(0,slash):id);}
        catch(IllegalArgumentException error){return "";}
    }
    static String match(String input,String name,List<String> uris,List<String> names)throws IOException{
        ArrayList<String> candidates=new ArrayList<>(),sameFolder=new ArrayList<>();String parent=parent(input);
        for(int n=0;n<uris.size();n++)if(isLyrics(names.get(n))&&stem(name).equalsIgnoreCase(stem(names.get(n)))){
            candidates.add(uris.get(n));if(!parent.isEmpty()&&parent.equals(parent(uris.get(n))))sameFolder.add(uris.get(n));
        }
        if(sameFolder.size()==1)return sameFolder.get(0);
        if(candidates.size()==1)return candidates.get(0);
        if(candidates.size()>1)throw new IOException("Più testi .lrc corrispondono a "+name+". Seleziona il testo corretto per evitare abbinamenti ambigui.");
        return "";
    }
    static byte[] read(Context context,String value,BooleanSupplier cancelled)throws IOException{
        if(value==null||value.isEmpty())return null;
        try(InputStream in=context.getContentResolver().openInputStream(Uri.parse(value));ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
            if(in==null)throw new IOException("Testo .lrc non leggibile");byte[] buffer=new byte[16384];int n;
            while((n=in.read(buffer))!=-1){if(cancelled.getAsBoolean())throw new IOException("Annullato");if(bytes.size()+n>LIMIT)throw new IOException("Testo .lrc oltre 2 MiB: nessun dato scartato");bytes.write(buffer,0,n);}
            return bytes.toByteArray();
        }
    }
    static String hash(byte[] bytes){
        try{byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder out=new StringBuilder();for(byte b:hash)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private AudioLyrics(){}
}
