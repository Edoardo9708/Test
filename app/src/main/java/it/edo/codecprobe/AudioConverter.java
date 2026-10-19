package it.edo.codecprobe;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;

/** One audio→Opus job. Owns the temporary file and all per-file resources, never the queue/UI. */
final class AudioConverter {
    interface Listener {
        boolean cancelled();
        void progress(long samples,long total,int rate,int channels);
        void stage(String message);
        void log(String text);
    }
    static AudioRunState.Result convert(Context context,Uri input,Uri destination,String name,int bitrate,String lyricsUri,Listener listener)throws IOException{
        return convert(context,input,destination,name,bitrate,lyricsUri,false,listener);
    }
    static AudioRunState.Result convert(Context context,Uri input,Uri destination,String name,int bitrate,String lyricsUri,boolean onlySmaller,Listener listener)throws IOException{
        File temp=null;
        try{
            listener.log("*** LETTURA TESTO ESTERNO ***\n"+(lyricsUri.isEmpty()?"Nessun .lrc selezionato e abbinato a questo brano. Eventuali file non autorizzati non sono ricercabili automaticamente.":"Testo abbinato: "+lyricsUri));
            listener.stage("Lettura testo esterno…");
            byte[] lyrics=AudioLyrics.read(context,lyricsUri,listener::cancelled);
            if(lyrics!=null)listener.log("TESTO ORIGINALE: "+lyrics.length+" byte · SHA-256="+AudioLyrics.hash(lyrics));
            listener.stage("Analisi audio e metadati…");
            long before=MediaFileSizes.read(context,input);
            temp=File.createTempFile("audio-opus-",".tmp",context.getCacheDir());
            long duration;int[] channelCount={0};
            AudioCodec.Listener callback=new AudioCodec.Listener(){
                public boolean progress(long samples,long total,int rate,int channels){channelCount[0]=channels;listener.progress(samples,total,rate,channels);return !listener.cancelled();}
                public void log(String text){listener.log(text);}
            };
            int kind=AudioMediaDecoder.kind(name);
            if(kind!=0)duration=AudioMediaDecoder.encode(context,input,temp,kind,bitrate*1000,callback);
            else try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(input,"r")){
                if(fd==null)throw new IOException("File input non leggibile");
                duration=AudioCodec.encode(fd.getFd(),temp.getAbsolutePath(),bitrate*1000,callback);
            }
            checkCancelled(listener);listener.stage("Verifica decodifica Opus…");
            listener.log("*** 3. VERIFICA OUTPUT ***\nDecodifica completa con il decoder Android. Controllo canali e durata PCM, tolleranza 80 ms per pre-skip/fine stream.");
            AudioVerifier.verify(temp,duration,channelCount[0],listener::cancelled);
            checkCancelled(listener);listener.log("OK: file Opus decodificato · durata attesa="+duration+" µs");
            SavingsPolicy.Decision saving=SavingsPolicy.decide(onlySmaller,before,temp.length());
            listener.log("CONTROLLO RISPARMIO AUDIO: originale="+before+" · convertito="+temp.length()+" · solo più piccolo="+onlySmaller+" · "+saving);
            if(saving!=SavingsPolicy.Decision.SAVE)return new AudioRunState.Result(name,input.toString(),null,before,temp.length(),saving==SavingsPolicy.Decision.NO_SAVING?"Nessun risparmio · risultato non salvato":"Dimensione originale non disponibile · risultato non salvato",AudioRunState.Outcome.NOT_SAVED);
            listener.stage("Salvataggio del file Opus…");listener.log("*** 4. SALVATAGGIO ***\nIn caso di omonimia creo un nome con suffisso numerico. Non sovrascrivo file esistenti.");
            Uri result=AudioOutput.save(context,destination,temp,name,listener::cancelled,lyrics,listener::log);
            // Publication is the commit point: do not recheck cancellation after saving a valid result.
            return new AudioRunState.Result(name,input.toString(),result.toString(),before,temp.length(),null,AudioRunState.Outcome.SAVED);
        }finally{
            if(temp!=null&&temp.exists()&&!temp.delete()){
                // A diagnostic write must never turn an already published file into a false failure.
                try{listener.log("AVVISO: impossibile eliminare temporaneo "+temp.getName());}catch(RuntimeException ignored){}
            }
        }
    }
    private static void checkCancelled(Listener listener)throws IOException{if(listener.cancelled())throw new IOException("Annullato");}
    private AudioConverter(){}
}
