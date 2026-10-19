package it.edo.codecprobe;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import java.util.Locale;

/** Lightweight preview only; the conversion engine remains the authority on supported input. */
final class AudioInputInfo {
    static final class Info {final String summary,details;Info(String s,String d){summary=s;details=d;}}
    static Info read(Context context,String value,String name){
        Uri uri=Uri.parse(value);long bytes=MediaFileSizes.read(context,uri);StringBuilder summary=new StringBuilder(bytes>0?String.format(Locale.ROOT,"%.2f MB",bytes/1000000.0):"Dimensione non disponibile");
        StringBuilder details=new StringBuilder("Output: "+AudioOutput.name(name));
        MediaMetadataRetriever reader=new MediaMetadataRetriever();
        try{
            reader.setDataSource(context,uri);String ms=reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if(ms!=null){long seconds=Long.parseLong(ms)/1000;summary.append(String.format(Locale.ROOT," · %d:%02d",seconds/60,seconds%60));}
            for(int key:new int[]{MediaMetadataRetriever.METADATA_KEY_TITLE,MediaMetadataRetriever.METADATA_KEY_ARTIST,MediaMetadataRetriever.METADATA_KEY_ALBUM}){
                String text=reader.extractMetadata(key);if(text!=null&&!text.isEmpty())details.append('\n').append(key==MediaMetadataRetriever.METADATA_KEY_TITLE?"Titolo: ":key==MediaMetadataRetriever.METADATA_KEY_ARTIST?"Artista: ":"Album: ").append(text);
            }
        }catch(Exception ignored){details.append("\nTag non disponibili nell’anteprima; saranno controllati durante la conversione.");}
        finally{try{reader.release();}catch(Exception ignored){}}
        MediaExtractor extractor=new MediaExtractor();
        try{
            extractor.setDataSource(context,uri,null);
            for(int n=0;n<extractor.getTrackCount();n++){
                MediaFormat f=extractor.getTrackFormat(n);String mime=f.getString(MediaFormat.KEY_MIME);if(mime==null||!mime.startsWith("audio/"))continue;
                summary.append(" · ").append(mime.substring(6).toUpperCase(Locale.ROOT));
                if(f.containsKey(MediaFormat.KEY_SAMPLE_RATE))details.append("\nFrequenza: ").append(f.getInteger(MediaFormat.KEY_SAMPLE_RATE)).append(" Hz");
                if(f.containsKey(MediaFormat.KEY_CHANNEL_COUNT))details.append("\nCanali: ").append(f.getInteger(MediaFormat.KEY_CHANNEL_COUNT));break;
            }
        }catch(Exception ignored){details.append("\nFormato da verificare all’avvio della conversione.");}
        finally{extractor.release();}
        return new Info(summary.toString(),details.toString());
    }
    private AudioInputInfo(){}
}
