package it.edo.codecprobe;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import java.io.*;
import java.time.*;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Separates dates stored in the MP4 from dates exposed by Android. Never writes to MediaStore. */
final class VideoDateAudit {
    static final class Index {
        final Long taken,added,modified;final String status;
        Index(Long taken,Long added,Long modified,String status){this.taken=taken;this.added=added;this.modified=modified;this.status=status;}
    }
    static final class Snapshot {
        final long creation;final String extracted;final Long duration;final Index index;
        Snapshot(long creation,String extracted,Long duration,Index index){this.creation=creation;this.extracted=extracted;this.duration=duration;this.index=index;}
    }
    private static final Index PRIVATE=new Index(null,null,null,"file temporaneo privato · nessun indice Android");

    static Snapshot source(Context context,Uri uri,long creation,Consumer<String> log)throws Exception {
        try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(uri,"r")){
            if(fd==null)throw new IOException("Originale non disponibile per la verifica date");
            MediaMetadataRetriever reader=new MediaMetadataRetriever();
            try{reader.setDataSource(fd.getFileDescriptor());return extracted(reader,creation,readIndex(context,uri,log));}
            finally{reader.release();}
        }
    }
    static Snapshot file(File file,BooleanSupplier cancel)throws Exception {
        long creation;
        try(FileInputStream in=new FileInputStream(file)){creation=Mp4Metadata.creationTime(in.getChannel(),cancel);}
        MediaMetadataRetriever reader=new MediaMetadataRetriever();
        try{reader.setDataSource(file.getAbsolutePath());return extracted(reader,creation,PRIVATE);}
        finally{reader.release();}
    }
    static Snapshot saved(Context context,Uri uri,BooleanSupplier cancel,Consumer<String> log)throws Exception {
        try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(uri,"r")){
            if(fd==null)throw new IOException("Risultato non disponibile per la verifica date");
            // AutoCloseInputStream owns this descriptor; close it after the retriever has finished.
            try(ParcelFileDescriptor.AutoCloseInputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){
                long creation=Mp4Metadata.creationTime(in.getChannel(),cancel);
                MediaMetadataRetriever reader=new MediaMetadataRetriever();
                try{reader.setDataSource(fd.getFileDescriptor());return extracted(reader,creation,readIndex(context,uri,log));}
                finally{reader.release();}
            }
        }
    }
    private static Snapshot extracted(MediaMetadataRetriever reader,long creation,Index index){
        String duration=reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);Long millis=null;
        try{if(duration!=null)millis=Long.valueOf(duration);}catch(NumberFormatException ignored){}
        return new Snapshot(creation,reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE),millis,index);
    }
    static Index readIndex(Context context,Uri uri,Consumer<String> log){
        try{
            Uri media=uri;
            if(!MediaStore.AUTHORITY.equals(uri.getAuthority())){
                String authority=uri.getAuthority();
                if(!DocumentsContract.isDocumentUri(context,uri)||!("com.android.providers.media.documents".equals(authority)||"com.android.externalstorage.documents".equals(authority)))
                    return new Index(null,null,null,"provider senza equivalente MediaStore");
                media=MediaStore.getMediaUri(context,uri);
            }
            if(media==null)return new Index(null,null,null,"risultato non ancora indicizzato · confronto non disponibile");
            String[] fields={MediaStore.MediaColumns.DATE_TAKEN,MediaStore.MediaColumns.DATE_ADDED,MediaStore.MediaColumns.DATE_MODIFIED};
            try(Cursor rows=context.getContentResolver().query(media,fields,null,null,null)){
                if(rows==null||!rows.moveToFirst())return new Index(null,null,null,"elemento non ancora indicizzato · confronto non disponibile");
                return new Index(value(rows,fields[0]),value(rows,fields[1]),value(rows,fields[2]),"indice letto");
            }
        }catch(RuntimeException error){
            log.accept("AVVISO DATE · indice Android non leggibile: "+error.getClass().getSimpleName()+": "+error.getMessage());
            return new Index(null,null,null,"indice non accessibile · confronto non disponibile");
        }
    }
    private static Long value(Cursor rows,String field){int at=rows.getColumnIndex(field);return at<0||rows.isNull(at)?null:rows.getLong(at);}

    static void describe(String stage,Snapshot value,Consumer<String> log){
        Instant parsed=CreationDatePolicy.parse(value.extracted);
        log.accept("*** DIAGNOSTICA DATE · "+stage+" ***\nFuso del dispositivo="+ZoneId.systemDefault()+
            "\nHeader mvhd · secondi dal 1904="+value.creation+" · UTC="+CreationDatePolicy.displayMp4(value.creation)+
            "\nLettore Android METADATA_KEY_DATE="+value.extracted+" · "+instant(parsed)+
            "\nDurata video ms="+value.duration+
            "\nMediaStore="+value.index.status+
            "\nDATE_TAKEN · millisecondi Unix="+value.index.taken+" · "+millis(value.index.taken)+
            "\nDATE_ADDED · secondi Unix="+value.index.added+" · "+seconds(value.index.added)+
            "\nDATE_MODIFIED · secondi Unix="+value.index.modified+" · "+seconds(value.index.modified));
        if(parsed!=null&&positive(value.index.taken)){
            long difference=value.index.taken-parsed.toEpochMilli();
            log.accept("CONFRONTO FONTI DEL FILE · DATE_TAKEN meno data interna="+difference+" ms ("+(difference/1000.0)+" s). Valori diversi non indicano automaticamente un errore di copia.");
        }
    }
    static void verify(Snapshot original,Snapshot result,long fallback,String stage,Consumer<String> log)throws IOException {
        describe(stage,result,log);
        long expectedHeader=original.creation==0&&fallback>0?fallback:original.creation;
        if(result.creation!=expectedHeader)throw new IOException("DATA HEADER DIVERSA · "+stage+" · attesa="+expectedHeader+" · letta="+result.creation);
        Instant expected=fallback>0?Instant.ofEpochSecond(fallback-CreationDatePolicy.MP4_EPOCH_SECONDS):CreationDatePolicy.parse(original.extracted);
        Instant actual=CreationDatePolicy.parse(result.extracted);
        if(expected!=null){
            if(!expected.equals(actual))throw new IOException("DATA LETTA DA ANDROID DIVERSA · "+stage+"\nOriginale/attesa="+expected+"\nConvertito="+result.extracted+
                "\nScarto ms="+(actual==null?"non calcolabile":Duration.between(expected,actual).toMillis()));
            log.accept("OK DATE INTERNE · "+stage+" · header e data esposta dal lettore Android conservati · scarto=0 ms");
        }else if(Objects.equals(original.extracted,result.extracted)){
            log.accept("DATE INTERNE · header e valore grezzo del lettore identici; data assente/non interpretabile, nessuna certezza sulla visualizzazione in Galleria");
        }else log.accept("AVVISO DATE · valore del lettore cambiato ma data originale non interpretabile; confronto temporale non verificabile");
        if(result.index==PRIVATE)return;
        if(positive(original.index.taken)&&positive(result.index.taken)){
            long difference=result.index.taken-original.index.taken;
            log.accept((difference==0?"OK":"AVVISO")+" DATA GALLERIA · confronto MediaStore.DATE_TAKEN · scarto output meno input="+difference+" ms ("+(difference/1000.0)+" s)"+
                (difference==0?"":"\nDate interne controllate separatamente. La data indicizzata differisce; nessuna correzione arbitraria di minuti/ore applicata."));
        }else log.accept("DATA GALLERIA NON VERIFICATA · DATE_TAKEN originale o risultato assente/non accessibile; l’indicizzazione può non essere conclusa. Non equivale a esito OK.");
    }
    private static boolean positive(Long value){return value!=null&&value>0;}
    private static String instant(Instant value){return value==null?"non disponibile/non interpretabile":"UTC="+value+" · locale="+value.atZone(ZoneId.systemDefault());}
    private static String millis(Long value){try{return value==null?"assente":instant(Instant.ofEpochMilli(value));}catch(DateTimeException error){return "fuori intervallo";}}
    private static String seconds(Long value){try{return value==null?"assente":instant(Instant.ofEpochSecond(value));}catch(DateTimeException error){return "fuori intervallo";}}
    private VideoDateAudit(){}
}
