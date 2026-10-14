package it.edo.codecprobe;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import java.lang.reflect.Field;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

/** Reads declared metadata without decoding or modifying the original file. */
final class InputInspector {
    private InputInspector() {}
    static final class Report {final String summary,details;Report(String s,String d){summary=s;details=d;}String full(){return summary+"\n\nMETADATI LEGGIBILI DA ANDROID\n"+details;}}
    static String describe(Context context,Uri uri){return inspect(context,uri,()->false).full();}
    static String describe(Context context,Uri uri,BooleanSupplier cancelled){return inspect(context,uri,cancelled).full();}
    static Report inspect(Context context,Uri uri,BooleanSupplier cancelled) {
        StringBuilder info=new StringBuilder(),details=new StringBuilder();
        long bytes=-1;
        try(Cursor c=context.getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)) {
            if(c!=null&&c.moveToFirst()&&!c.isNull(0))bytes=c.getLong(0);
        }catch(Exception ignored){}
        if(bytes<0)try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(uri,"r")) {
            if(fd!=null)bytes=fd.getStatSize();
        }catch(Exception ignored){}
        info.append("Dimensione: ").append(bytes>=0?String.format(Locale.ITALY,"%.2f MB · %,d byte",bytes/1000000.0,bytes):"non disponibile dal provider");
        String container="non dichiarato";
        String duration=null,rotation=null;
        MediaMetadataRetriever retriever=new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context,uri);
            String mime=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            if(mime!=null)container=containerName(mime);
            duration=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            rotation=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            for(Field field:MediaMetadataRetriever.class.getFields())if(field.getName().startsWith("METADATA_KEY_"))try{
                String value=retriever.extractMetadata(field.getInt(null));if(value!=null)details.append(field.getName().substring(13)).append(": ").append(value).append("\n");
            }catch(Exception ignored){}
        }catch(Exception ignored){}finally{try{retriever.release();}catch(Exception ignored){}}
        info.append("\nContenitore: ").append(container);
        if(duration!=null)try{info.append(String.format(Locale.ITALY,"\nDurata: %.2f s",Long.parseLong(duration)/1000.0));}catch(NumberFormatException ignored){}
        if(rotation!=null)info.append(" · rotazione ").append(rotation).append("°");
        MediaExtractor extractor=new MediaExtractor();int videos=0,audios=0;
        long sampleTotal=0;boolean counted=false;
        try {
            extractor.setDataSource(context,uri,null);
            for(int i=0;i<extractor.getTrackCount();i++) {
                MediaFormat f=extractor.getTrackFormat(i);String mime=f.getString(MediaFormat.KEY_MIME);
                if(mime==null)continue;
                details.append("\nTRACCIA ").append(i).append(" · ").append(mime).append("\n");
                for(String key:new TreeSet<>(f.getKeys())) {
                    details.append(key).append(" = ");
                    try {int type=f.getValueTypeForKey(key);
                        if(type==MediaFormat.TYPE_STRING)details.append(f.getString(key));
                        else if(type==MediaFormat.TYPE_BYTE_BUFFER){ByteBuffer data=f.getByteBuffer(key);details.append(data==null?"null":data.remaining()+" byte di dati binari");}
                        else details.append(f.getNumber(key));
                    }catch(RuntimeException error){details.append("non leggibile");}
                    details.append("\n");
                }
                if(mime.startsWith("video/")) {
                    videos++;info.append("\n\nVideo ").append(videos).append(": ").append(codecName(mime));
                    int w=integer(f,MediaFormat.KEY_WIDTH,-1),h=integer(f,MediaFormat.KEY_HEIGHT,-1);
                    if(w>0&&h>0)info.append("\nRisoluzione: ").append(w).append(" × ").append(h);
                    Number fps=null;try{fps=f.getNumber(MediaFormat.KEY_FRAME_RATE);}catch(RuntimeException ignored){}
                    info.append("\nFPS dichiarati: ").append(fps!=null?String.format(Locale.ITALY,"%.3f",fps.doubleValue()):"non disponibili");
                    int transfer=integer(f,MediaFormat.KEY_COLOR_TRANSFER,-1);
                    String hdr=transfer==6?"HDR · PQ (non supportato)":transfer==7?"HDR · HLG (non supportato)":transfer==3||transfer==1?"SDR":f.containsKey("hdr-static-info")?"metadati HDR presenti; trasferimento non dichiarato":"non determinabile dai metadati";
                    info.append("\nGamma dinamica: ").append(hdr);
                    int profile=integer(f,MediaFormat.KEY_PROFILE,-1);
                    String depth="non dichiarata";
                    if(mime.equals("video/hevc")) {if(profile==1)depth="8 bit (profilo Main)";else if(profile==2||profile==4096||profile==8192)depth="10 bit (non supportato)";}
                    if(mime.equals("video/avc")&&(profile==16||profile==32||profile==64))depth="profilo avanzato non supportato";
                    info.append("\nProfondità: ").append(depth);
                    int bitrate=integer(f,MediaFormat.KEY_BIT_RATE,-1);if(bitrate>0)info.append(String.format(Locale.ITALY,"\nBitrate video dichiarato: %.2f Mbit/s",bitrate/1000000.0));
                    if(transfer==6||transfer==7)info.append("\nCompatibilità: conversione HDR non ancora disponibile.");
                }else if(mime.startsWith("audio/")) {
                    audios++;info.append("\n\nAudio ").append(audios).append(": ").append(codecName(mime));
                    int channels=integer(f,MediaFormat.KEY_CHANNEL_COUNT,-1),hz=integer(f,MediaFormat.KEY_SAMPLE_RATE,-1);
                    if(channels>0)info.append("\nCanali: ").append(channels);if(hz>0)info.append(" · ").append(hz).append(" Hz");
                    info.append(mime.equals("audio/mp4a-latm")?"\nAAC: verrà copiato senza ricodifica.":"\nQuesto codec audio non è ancora supportato per la conversione.");
                }
            }
            long[] sizes=new long[extractor.getTrackCount()];
            for(int i=0;i<sizes.length;i++)extractor.selectTrack(i);
            while(extractor.getSampleTrackIndex()>=0){
                if(cancelled.getAsBoolean())return new Report("Analisi annullata","");
                int track=extractor.getSampleTrackIndex();long size=extractor.getSampleSize();
                if(size<0)throw new IllegalStateException("Dimensione campione non disponibile");
                sizes[track]=Math.addExact(sizes[track],size);sampleTotal=Math.addExact(sampleTotal,size);if(!extractor.advance())break;
            }
            info.append("\n\nDIMENSIONI DELLE TRACCE (DATI COMPRESSI)");
            for(int i=0;i<sizes.length;i++)info.append("\nTraccia ").append(i).append(" · ").append(codecName(extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME))).append(": ").append(sizeText(sizes[i]));
            counted=true;
            if(videos==0)info.append("\nNessuna traccia video rilevata.");
            if(audios==0)info.append("\n\nAudio: assente.");
            if(videos>1||audios>1)info.append("\nPiù tracce dello stesso tipo: non ancora supportato.");
        }catch(Exception e){info.append("\nAnalisi tracce non completata: ").append(e.getMessage());}
        finally{extractor.release();}
        info.append("\n\nInformazioni dichiarate dal file; fps variabili e dati mancanti verranno verificati durante la conversione.");
        if(counted&&bytes>=sampleTotal)info.append("\nStruttura, metadati, padding e altri dati fuori dai campioni: ").append(sizeText(bytes-sampleTotal));
        if(container.startsWith("MP4")||container.equals("video/quicktime"))try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(uri,"r")){
            if(fd!=null)try(FileInputStream in=new FileInputStream(fd.getFileDescriptor())) {
                Mp4Boxes.Sizes boxes=Mp4Boxes.read(in.getChannel());
                info.append("\nStruttura moov (indici + metadati tecnici): ").append(sizeText(boxes.moov));
                info.append("\nBlocchi descrittivi udta/meta: ").append(sizeText(boxes.tags)).append(" (inclusi nel moov se interni)");
            }
        }catch(Exception e){info.append("\nDimensione dei blocchi metadati: non disponibile per questo file/provider.");}
        details.append("\nQuesti sono tutti i campi esposti dalle API Android. Tag proprietari o blocchi sconosciuti possono non essere decodificati. La conversione conserva risoluzione, tempi, orientamento e colori; i tag descrittivi dell’originale non sono copiati automaticamente.");
        return new Report(info.toString(),details.toString());
    }
    private static String sizeText(long bytes){return String.format(Locale.ITALY,"%.3f MB · %,d byte",bytes/1000000.0,bytes);}
    private static int integer(MediaFormat f,String key,int fallback){try{return f.containsKey(key)?f.getInteger(key):fallback;}catch(RuntimeException e){return fallback;}}
    private static String containerName(String mime){switch(mime==null?"sconosciuto":mime){case "video/mp4":return "MP4 ("+mime+")";case "video/x-matroska":return "Matroska / MKV";case "video/webm":return "WebM";case "video/3gpp":return "3GP";default:return mime;}}
    private static String codecName(String mime){switch(mime==null?"sconosciuto":mime){case "video/hevc":return "H.265 / HEVC";case "video/avc":return "H.264 / AVC";case "video/av01":return "AV1";case "video/x-vnd.on2.vp9":return "VP9";case "video/x-vnd.on2.vp8":return "VP8";case "audio/mp4a-latm":return "AAC";case "audio/mpeg":return "MP3";case "audio/opus":return "Opus";case "audio/vorbis":return "Vorbis";default:return mime;}}
}
