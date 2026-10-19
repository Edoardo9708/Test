package it.edo.codecprobe;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** SAF publication after verification. Creates a new name; removes partial documents on failure. */
final class AudioOutput {
    static String name(String original){
        String name=original.replace('/', '_').replace('\\','_').replace('\n','_').replace('\r','_').trim();
        int dot=name.lastIndexOf('.');if(dot>0)name=name.substring(0,dot);
        if(name.isEmpty())name="Audio";
        if(name.length()>140)name=name.substring(0,140);
        return name+".opus";
    }
    static String unique(String wanted,Set<String> existing){return unique(wanted,existing,false);}
    static String unique(String wanted,Set<String> existing,boolean lyrics){
        if(!contains(existing,wanted)&&(!lyrics||!contains(existing,AudioLyrics.stem(wanted)+".lrc")))return wanted;
        String base=wanted.substring(0,wanted.length()-5);
        for(int n=2;n<100000;n++){String choice=base+" ("+n+").opus";if(!contains(existing,choice)&&(!lyrics||!contains(existing,AudioLyrics.stem(choice)+".lrc")))return choice;}
        throw new IllegalStateException("Troppi file omonimi nella destinazione");
    }
    private static boolean contains(Set<String> names,String name){for(String existing:names)if(existing.equalsIgnoreCase(name))return true;return false;}
    static Uri save(Context c,Uri tree,File source,String original,BooleanSupplier cancelled)throws IOException{
        return save(c,tree,source,original,cancelled,null,text->{});
    }
    static Uri save(Context c,Uri tree,File source,String original,BooleanSupplier cancelled,byte[] lyrics,java.util.function.Consumer<String> log)throws IOException{
        Uri document=null,lyricDocument=null;
        try{
            String id=DocumentsContract.getTreeDocumentId(tree);
            Set<String> names=new HashSet<>();
            Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,id);
            try(Cursor rows=c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
                if(rows==null)throw new IOException("Impossibile leggere la cartella di destinazione");
                while(rows.moveToNext())names.add(rows.getString(0));
            }
            if(cancelled.getAsBoolean())throw new IOException("Annullato");
            String chosen=unique(name(original),names,lyrics!=null);
            document=DocumentsContract.createDocument(c.getContentResolver(),DocumentsContract.buildDocumentUriUsingTree(tree,id),"audio/ogg",chosen);
            if(document==null)throw new IOException("Creazione documento Opus fallita");
            long written=0;
            try(InputStream in=new FileInputStream(source);OutputStream out=c.getContentResolver().openOutputStream(document,"w")){
                if(out==null)throw new IOException("Documento non scrivibile");
                byte[] buffer=new byte[65536];int n;
                while((n=in.read(buffer))!=-1){if(cancelled.getAsBoolean())throw new IOException("Annullato");out.write(buffer,0,n);written+=n;}
                out.flush();
            }
            if(cancelled.getAsBoolean())throw new IOException("Annullato");
            if(written!=source.length())throw new IOException("Copia output incompleta");
            long size=MediaFileSizes.read(c,document);if(size>=0&&size!=written)throw new IOException("Dimensione del documento salvato diversa dall’output");
            if(lyrics!=null){
                String actual=displayName(c,document);String lyricName=AudioLyrics.stem(actual)+".lrc";
                if(contains(names,lyricName))throw new IOException("Esiste già un testo con il nome finale "+lyricName+": nessun file sovrascritto");
                lyricDocument=DocumentsContract.createDocument(c.getContentResolver(),DocumentsContract.buildDocumentUriUsingTree(tree,id),"application/octet-stream",lyricName);
                if(lyricDocument==null)throw new IOException("Creazione testo .lrc fallita");
                if(!lyricName.equals(displayName(c,lyricDocument)))throw new IOException("Il provider ha cambiato il nome del testo .lrc: impossibile garantire l’abbinamento");
                try(OutputStream out=c.getContentResolver().openOutputStream(lyricDocument,"w")){
                    if(out==null)throw new IOException("Testo .lrc non scrivibile");
                    if(cancelled.getAsBoolean())throw new IOException("Annullato");out.write(lyrics);out.flush();
                }
                byte[] actualLyrics=AudioLyrics.read(c,lyricDocument.toString(),cancelled);
                if(!Arrays.equals(lyrics,actualLyrics))throw new IOException("Verifica .lrc fallita: byte diversi dall’originale");
                log.accept("OK TESTO ESTERNO: "+lyricName+" · "+lyrics.length+" byte · SHA-256="+AudioLyrics.hash(lyrics)+"\nCopia riletta e identica; tempi, BOM e codifica invariati. URI: "+lyricDocument);
            }
            if(cancelled.getAsBoolean())throw new IOException("Annullato");
            return document;
        }catch(Exception error){
            rollback(c,lyricDocument,error);rollback(c,document,error);
            if(error instanceof IOException)throw (IOException)error;
            throw new IOException("Salvataggio Opus fallito: "+error.getMessage(),error);
        }
    }
    private static String displayName(Context c,Uri uri)throws IOException{
        try(Cursor rows=c.getContentResolver().query(uri,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
            if(rows!=null&&rows.moveToFirst()&&!rows.isNull(0))return rows.getString(0);
        }throw new IOException("Nome effettivo del documento non leggibile");
    }
    private static void rollback(Context c,Uri uri,Exception error){
        if(uri==null)return;
        try{if(!DocumentsContract.deleteDocument(c.getContentResolver(),uri))error.addSuppressed(new IOException("Eliminazione output parziale non riuscita: "+uri));}
        catch(Exception cleanup){error.addSuppressed(cleanup);}
    }
    private AudioOutput(){}
}
