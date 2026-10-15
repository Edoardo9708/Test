package it.edo.codecprobe;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import java.io.*;
import java.util.function.Consumer;

/** Reads the original Android date once; never updates the original or DATE_ADDED. */
final class InputCreationDate {
    static long read(Context context,Uri input,long movieCreation,Consumer<String> log)throws Exception {
        log.accept("*** DATA DI CREAZIONE DEL NUOVO VIDEO ***\n1. Controllo la data interna originale.\n2. Solo se manca o vale zero/1904, leggo DATE_ADDED di Android.\n3. Scrivo quella data nel nuovo MP4 e la verifico. DATE_ADDED indica l’aggiunta ad Android, spesso il download, non una data di ripresa certa.");
        log.accept("DATA HEADER ORIGINALE · secondi_epoca_MP4="+movieCreation+" · UTC="+CreationDatePolicy.displayMp4(movieCreation));
        if(movieCreation>0){log.accept("DATA SCELTA · data interna originale conservata; nessuna sostituzione con la data di download");return 0;}
        String embedded;
        try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(input,"r")){
            if(fd==null)throw new IOException("File non disponibile durante la lettura della data");
            MediaMetadataRetriever reader=new MediaMetadataRetriever();
            try{reader.setDataSource(fd.getFileDescriptor());embedded=reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);}
            finally{reader.release();}
        }
        return fromAndroid(context,input,movieCreation,embedded,log);
    }
    static long fromAndroid(Context context,Uri input,long movieCreation,String embedded,Consumer<String> log)throws IOException {
        log.accept("DATA METADATI ORIGINALE · valore="+embedded+" · stato="+CreationDatePolicy.embedded(embedded));
        if(!CreationDatePolicy.needsFallback(movieCreation,embedded)){
            log.accept("DATA SCELTA · metadati originali conservati; data presente o formato non riconosciuto, nessuna sostituzione automatica");return 0;
        }
        if(!MediaStore.AUTHORITY.equals(input.getAuthority())){
            log.accept("DATA ANDROID NON DISPONIBILE · questo provider non espone un elemento MediaStore; nessuna data inventata");return 0;
        }
        Long added=null;
        try(Cursor row=context.getContentResolver().query(input,new String[]{MediaStore.MediaColumns.DATE_ADDED},null,null,null)){
            if(row!=null&&row.moveToFirst()&&!row.isNull(0))added=row.getLong(0);
        }catch(RuntimeException error){log.accept("DATA ANDROID NON LEGGIBILE · "+error);return 0;}
        long fallback=CreationDatePolicy.fromAddedSeconds(added==null?0:added);
        if(fallback==0){log.accept("DATA ANDROID ASSENTE · DATE_ADDED="+added+"; nessuna data inventata");return 0;}
        log.accept("DATA SCELTA · provenienza=Android MediaStore.DATE_ADDED dell’originale\nSecondi Unix="+added+
                "\nData UTC="+CreationDatePolicy.displayMp4(fallback)+"\nUserò questa data solo nei campi di creazione MP4 mancanti; non è una data di ripresa recuperata.");
        return fallback;
    }
    static void verify(File target,long fallback,Consumer<String> log)throws Exception {
        if(fallback==0)return;
        try(FileInputStream file=new FileInputStream(target)){
            long actual=Mp4Metadata.creationTime(file.getChannel(),()->false);
            if(actual!=fallback)throw new IOException("Data di creazione MP4 diversa da DATE_ADDED: attesa="+fallback+" letta="+actual);
        }
        MediaMetadataRetriever reader=new MediaMetadataRetriever();
        String exposed;
        try{reader.setDataSource(target.getAbsolutePath());exposed=reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);}
        finally{reader.release();}
        java.time.Instant time=CreationDatePolicy.parse(exposed);
        if(time==null||time.getEpochSecond()!=fallback-CreationDatePolicy.MP4_EPOCH_SECONDS)
            throw new IOException("La data esposta da Android non coincide con DATE_ADDED: attesa="+CreationDatePolicy.displayMp4(fallback)+" letta="+exposed);
        log.accept("OK VERIFICA DATA · header MP4 e lettore Android concordano: "+exposed+" · provenienza=DATE_ADDED originale");
    }
    private InputCreationDate() {}
}
