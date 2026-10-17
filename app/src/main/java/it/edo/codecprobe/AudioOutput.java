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
    static String unique(String wanted,Set<String> existing){
        if(!existing.contains(wanted))return wanted;
        String base=wanted.substring(0,wanted.length()-5);
        for(int n=2;n<100000;n++){String choice=base+" ("+n+").opus";if(!existing.contains(choice))return choice;}
        throw new IllegalStateException("Troppi file omonimi nella destinazione");
    }
    static Uri save(Context c,Uri tree,File source,String original,BooleanSupplier cancelled)throws IOException{
        Uri document=null;
        try{
            String id=DocumentsContract.getTreeDocumentId(tree);
            Set<String> names=new HashSet<>();
            Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,id);
            try(Cursor rows=c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
                if(rows==null)throw new IOException("Impossibile leggere la cartella di destinazione");
                while(rows.moveToNext())names.add(rows.getString(0));
            }
            if(cancelled.getAsBoolean())throw new IOException("Annullato");
            String chosen=unique(name(original),names);
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
            return document;
        }catch(Exception error){
            if(document!=null)try{if(!DocumentsContract.deleteDocument(c.getContentResolver(),document))error.addSuppressed(new IOException("Eliminazione output parziale non riuscita: "+document));}catch(Exception cleanup){error.addSuppressed(cleanup);}
            if(error instanceof IOException)throw (IOException)error;
            throw new IOException("Salvataggio Opus fallito: "+error.getMessage(),error);
        }
    }
    private AudioOutput(){}
}
