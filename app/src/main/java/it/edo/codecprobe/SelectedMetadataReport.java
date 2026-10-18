package it.edo.codecprobe;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Fresh read-only batch report. No cache, decoder, encoder or full-file copy. */
final class SelectedMetadataReport {
    interface Reader {void write(Writer out)throws Exception;}
    static void write(Context context,List<Uri> inputs,List<String> names,Writer out,BooleanSupplier cancel,java.util.function.IntConsumer progress)throws Exception{
        if(inputs.size()!=names.size())throw new IllegalArgumentException("Elenco metadati incoerente");
        out.write("*** RAPPORTO METADATI · v"+BuildConfig.VERSION_NAME+" ***\nFile selezionati="+inputs.size()+"\nFuso dispositivo="+ZoneId.systemDefault()+"\n");
        out.write("Lettura nuova dei file, senza conversione. Proprietà Android e tracce esposte dalle API; inventario dei blocchi MP4/MOV con date, identificatori, dimensioni, hash e anteprime; i blocchi Samsung SEF sono esportati integralmente in BASE64 entro il limite esplicito di 4 MiB per file. I dati proprietari non documentati restano binari, non vengono interpretati per supposizione. Campioni audio/video e SEI dentro tutti i fotogrammi non sono analizzati. I limiti o le sezioni non leggibili sono indicati esplicitamente.\n");
        for(int n=0;n<inputs.size();n++){
            check(cancel);progress.accept(n+1);
            out.write("\n============================================================\n*** FILE "+(n+1)+" / "+inputs.size()+" · "+safe(names.get(n))+" ***\nURI selezionato="+inputs.get(n)+"\n");
            final Uri selected=inputs.get(n);
            section(out,"DOCUMENTO SELEZIONATO · CAMPI DEL PROVIDER",cancel,writer->provider(context,selected,writer,cancel));
            Uri original;
            try{original=OriginalMedia.resolve(context,selected);}
            catch(Exception error){check(cancel);out.write("ERRORE ACCESSO ORIGINALE · "+safe(error.toString())+"\nFile non analizzato; continuo con il successivo.\n");continue;}
            out.write("URI originale="+original+"\n");
            if(!selected.equals(original))section(out,"INDICE ANDROID · CAMPI MEDIASTORE",cancel,writer->provider(context,original,writer,cancel));
            section(out,"PROPRIETÀ DEL FILE · LETTORE ANDROID",cancel,writer->retriever(context,original,writer,cancel));
            section(out,"TRACCE VIDEO, AUDIO E METADATI",cancel,writer->tracks(context,original,writer,cancel));
            section(out,"STRUTTURA E METADATI INTERNI MP4/MOV",cancel,writer->{
                try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(original,"r")){
                    if(fd==null)throw new IOException("File non disponibile");
                    try(ParcelFileDescriptor.AutoCloseInputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){
                        Mp4MetadataInventory.write(in.getChannel(),writer,cancel);
                    }
                }
            });
            out.write("*** FINE FILE "+(n+1)+" ***\n");out.flush();
        }
        check(cancel);out.write("\n*** FINE RAPPORTO METADATI ***\n");
    }
    static void section(Writer out,String title,BooleanSupplier cancel,Reader reader)throws Exception{
        check(cancel);out.write("\n*** "+title+" ***\n");
        try{reader.write(out);}catch(InterruptedIOException error){throw error;}
        catch(Exception error){check(cancel);out.write("SEZIONE NON COMPLETA · "+safe(error.toString())+"\nLe altre sezioni/file vengono comunque analizzati.\n");}
    }
    private static void provider(Context context,Uri uri,Writer out,BooleanSupplier cancel)throws Exception{
        try(Cursor rows=context.getContentResolver().query(uri,null,null,null,null)){
            if(rows==null||!rows.moveToFirst()){out.write("NON DISPONIBILE · nessuna riga dal provider; non prova l’assenza dei metadati\n");return;}
            TreeMap<String,Integer> columns=new TreeMap<>();for(int at=0;at<rows.getColumnCount();at++)columns.put(rows.getColumnName(at),at);
            int count=0;for(Map.Entry<String,Integer> entry:columns.entrySet()){
                check(cancel);if(++count>256){out.write("LIMITE · prime 256 colonne; altre omesse\n");break;}
                int at=entry.getValue();String name=entry.getKey(),value;
                if(rows.isNull(at))value="[null]";
                else if(rows.getType(at)==Cursor.FIELD_TYPE_BLOB)value="[BLOB del provider · contenuto non esportato]";
                else value=safe(rows.getString(at));
                out.write(name+"="+value);
                if(!rows.isNull(at)&&(name.equals(MediaStore.MediaColumns.DATE_TAKEN)||name.equals(MediaStore.MediaColumns.DATE_ADDED)||name.equals(MediaStore.MediaColumns.DATE_MODIFIED))){
                    try{long raw=rows.getLong(at);boolean millis=name.equals(MediaStore.MediaColumns.DATE_TAKEN);Instant time=millis?Instant.ofEpochMilli(raw):Instant.ofEpochSecond(raw);
                        out.write(" · "+(millis?"millisecondi":"secondi")+" Unix · UTC="+time+" · locale="+time.atZone(ZoneId.systemDefault()));
                    }catch(RuntimeException error){out.write(" · data non interpretabile");}
                }
                out.write("\n");
            }
        }
    }
    private static void retriever(Context context,Uri uri,Writer out,BooleanSupplier cancel)throws Exception{
        MediaMetadataRetriever reader=new MediaMetadataRetriever();
        try{
            reader.setDataSource(context,uri);
            TreeMap<String,Field> fields=new TreeMap<>();for(Field field:MediaMetadataRetriever.class.getFields())if(field.getName().startsWith("METADATA_KEY_"))fields.put(field.getName(),field);
            for(Map.Entry<String,Field> field:fields.entrySet()){
                check(cancel);int id=field.getValue().getInt(null);String value;
                try{value=reader.extractMetadata(id);}catch(RuntimeException error){out.write(field.getKey()+" · id="+id+" · NON LEGGIBILE="+safe(error.toString())+"\n");continue;}
                out.write(field.getKey()+" · id="+id+" · valore="+(value==null?"[non esposto]":safe(value))+"\n");
                if(field.getKey().equals("METADATA_KEY_DATE")){
                    Instant time=CreationDatePolicy.parse(value);out.write("DATA INTERPRETATA="+(time==null?"non disponibile/non interpretabile":"UTC="+time+" · locale="+time.atZone(ZoneId.systemDefault()))+"\n");
                }
            }
        }finally{reader.release();}
    }
    private static void tracks(Context context,Uri uri,Writer out,BooleanSupplier cancel)throws Exception{
        MediaExtractor extractor=new MediaExtractor();
        try{
            extractor.setDataSource(context,uri,null);out.write("Numero tracce="+extractor.getTrackCount()+"\n");
            for(int n=0;n<extractor.getTrackCount();n++){
                check(cancel);MediaFormat format=extractor.getTrackFormat(n);out.write("\nTRACCIA id="+n+"\n");
                for(String key:new TreeSet<>(format.getKeys())){
                    check(cancel);out.write(key+"=");
                    try{
                        int type=format.getValueTypeForKey(key);
                        if(type==MediaFormat.TYPE_BYTE_BUFFER){
                            ByteBuffer bytes=format.getByteBuffer(key);
                            if(bytes==null)out.write("[null]");
                            else {ByteBuffer copy=bytes.duplicate();int size=copy.remaining();byte[] head=new byte[Math.min(size,64)];copy.get(head);
                                out.write(size+" byte · SHA256="+(size<=4*1024*1024?Mp4MetadataInventory.digest(bytes):"non calcolato · oltre 4 MiB")+" · primi "+head.length+" byte HEX="+Mp4MetadataInventory.hex(head));}
                        }else if(type==MediaFormat.TYPE_STRING)out.write(safe(format.getString(key)));
                        else out.write(String.valueOf(format.getNumber(key)));
                    }catch(RuntimeException error){out.write("NON LEGGIBILE · "+safe(error.toString()));}
                    out.write("\n");
                }
            }
        }finally{extractor.release();}
    }
    private static String safe(String value){if(value==null)return "null";String result=Mp4MetadataInventory.escape(value.length()>4096?value.substring(0,4096):value);return result+(value.length()>4096?" [testo troncato a 4096 caratteri]":"");}
    private static void check(BooleanSupplier cancel)throws InterruptedIOException{if(cancel.getAsBoolean())throw new InterruptedIOException("Esportazione metadati annullata");}
    private SelectedMetadataReport(){}
}
