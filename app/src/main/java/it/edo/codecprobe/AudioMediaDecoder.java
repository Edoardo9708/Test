package it.edo.codecprobe;

import android.content.Context;
import android.media.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import java.io.*;
import java.nio.ByteBuffer;

/** MP3/M4A decoder pump. One synchronous worker owns decoder, extractor and native Opus handle. */
final class AudioMediaDecoder {
    static final int MP3=1,M4A=2;
    static int kind(String name)throws IOException{
        String lower=name.toLowerCase(java.util.Locale.ROOT);
        if(lower.endsWith(".flac"))return 0;
        if(lower.endsWith(".mp3"))return MP3;
        if(lower.endsWith(".m4a"))return M4A;
        throw new IOException("Formato input non supportato: seleziona FLAC, MP3 oppure M4A");
    }
    static long encode(Context context,Uri input,File output,int kind,int bitrate,AudioCodec.Listener listener)throws IOException{
        MediaExtractor extractor=new MediaExtractor();MediaCodec decoder=null;boolean started=false;long handle=0;
        try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(input,"r")){
            if(fd==null)throw new IOException("Input audio non leggibile");
            listener.log("*** 1. LETTURA MP3 / M4A ***\nDecodifica: MediaExtractor + MediaCodec Android.\nCodifica: libopus + libopusenc CPU. Nessun encoder Android.\nMetadati: TagLib 2.3.2; campi ID3/MP4 tradotti in commenti Opus, copertine senza ricompressione. I campi non traducibili interrompono il salvataggio.");
            // Android's decoder handles encoder-delay/encoder-padding from the original format.
            // Do not apply a second manual trim and do not remove those keys.
            extractor.setDataSource(context,input,null);int track=-1;MediaFormat format=null;
            for(int n=0;n<extractor.getTrackCount();n++){
                MediaFormat candidate=extractor.getTrackFormat(n);String mime=candidate.getString(MediaFormat.KEY_MIME);
                if(mime!=null&&mime.startsWith("audio/")){
                    if(track>=0)throw new IOException("Input con piu tracce audio: selezione della traccia non ancora disponibile");
                    track=n;format=candidate;
                }
            }
            if(track<0||format==null)throw new IOException("Nessuna traccia audio leggibile");
            String mime=format.getString(MediaFormat.KEY_MIME);
            if(kind==MP3&&!"audio/mpeg".equals(mime))throw new IOException("Il file .mp3 non contiene audio MPEG Layer III");
            if(kind==M4A&&!"audio/mp4a-latm".equals(mime)&&!"audio/alac".equals(mime))throw new IOException("M4A: codec non supportato: "+mime);
            int inputChannels=format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),inputRate=format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            if(inputChannels<1||inputChannels>2||inputRate<8000||inputRate>192000)throw new IOException("Supportati solo input mono/stereo da 8 a 192 kHz");
            long expectedUs=format.containsKey(MediaFormat.KEY_DURATION)?format.getLong(MediaFormat.KEY_DURATION):0;
            int delay=format.containsKey(MediaFormat.KEY_ENCODER_DELAY)?format.getInteger(MediaFormat.KEY_ENCODER_DELAY):0;
            int padding=format.containsKey(MediaFormat.KEY_ENCODER_PADDING)?format.getInteger(MediaFormat.KEY_ENCODER_PADDING):0;
            listener.log("INPUT: "+format+"\nGapless dichiarato: delay="+delay+" · padding="+padding+" campioni/canale, affidati al decoder Android.\nMP3/AAC sono gia lossy: una nuova codifica puo ridurre la qualita e non garantisce un file piu piccolo.");
            if(!listener.progress(0,expectedUs*inputRate/1000000L,inputRate,inputChannels))throw new IOException("Annullato");
            extractor.selectTrack(track);decoder=MediaCodec.createDecoderByType(mime);
            listener.log("DECODER SCELTO: "+decoder.getName());decoder.configure(format,null,null,0);decoder.start();started=true;
            boolean inputEnd=false,outputEnd=false;long lastData=SystemClock.elapsedRealtime(),frames=0;int channels=0,rate=0,encoding=AudioFormat.ENCODING_PCM_16BIT;
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();float[] pcm=new float[8192];
            while(!outputEnd){
                if(!listener.progress(frames,expectedUs* (rate>0?rate:inputRate)/1000000L,rate>0?rate:inputRate,channels>0?channels:inputChannels))throw new IOException("Annullato");
                if(SystemClock.elapsedRealtime()-lastData>15000)throw new IOException("Decoder audio senza avanzamento per 15 secondi");
                if(!inputEnd){int slot=decoder.dequeueInputBuffer(10000);if(slot>=0){
                    ByteBuffer buffer=decoder.getInputBuffer(slot);if(buffer==null)throw new IOException("Buffer input decoder assente");buffer.clear();
                    int size=extractor.readSampleData(buffer,0);
                    if(size<0){decoder.queueInputBuffer(slot,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;}
                    else{
                        if((extractor.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_ENCRYPTED)!=0)throw new IOException("Audio protetto DRM non supportato");
                        if(size>buffer.capacity())throw new IOException("Pacchetto audio oltre la capacita del decoder");
                        decoder.queueInputBuffer(slot,0,size,extractor.getSampleTime(),0);extractor.advance();
                    }
                }}
                int slot=decoder.dequeueOutputBuffer(info,10000);
                if(slot==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                    MediaFormat out=decoder.getOutputFormat();int nextChannels=out.getInteger(MediaFormat.KEY_CHANNEL_COUNT),nextRate=out.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    if(nextChannels<1||nextChannels>2||nextRate<8000||nextRate>192000)throw new IOException("Formato PCM decoder non supportato: "+out);
                    if(handle!=0&&(nextChannels!=channels||nextRate!=rate))throw new IOException("Formato PCM cambiato durante il brano");
                    channels=nextChannels;rate=nextRate;encoding=out.containsKey(MediaFormat.KEY_PCM_ENCODING)?out.getInteger(MediaFormat.KEY_PCM_ENCODING):AudioFormat.ENCODING_PCM_16BIT;AudioPcm.bytes(encoding);
                    listener.log("PCM DECODER: "+out);
                    if(handle==0){
                        handle=AudioCodec.openPcm(fd.getFd(),kind,output.getAbsolutePath(),rate,channels,bitrate,listener);
                        if(handle==0)throw new IOException("Encoder Opus non aperto");
                        listener.log("*** 2. CODIFICA OPUS VBR ***\nBitrate="+bitrate+" bit/s · VBR libero · complexity=10.\nPCM a piccoli blocchi; nessuna normalizzazione. libopusenc ricampiona a 48 kHz.\nDa qui: campioni per canale elaborati/totali stimati, secondi e percentuale. Log limitato a un aggiornamento ogni 5 secondi.");
                    }
                }else if(slot>=0){
                    try{
                        if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){
                            if(handle==0)throw new IOException("PCM ricevuto prima del formato output");
                            ByteBuffer buffer=decoder.getOutputBuffer(slot);if(buffer==null||info.offset<0||info.size<0||(long)info.offset+info.size>buffer.capacity())throw new IOException("Buffer PCM non valido");
                            int width=AudioPcm.bytes(encoding);if(info.size%(width*channels)!=0)throw new IOException("Campioni PCM non allineati");
                            buffer.position(info.offset);buffer.limit(info.offset+info.size);
                            while(buffer.hasRemaining()){
                                if(!listener.progress(frames,expectedUs*rate/1000000L,rate,channels))throw new IOException("Annullato");
                                int count=Math.min(pcm.length,buffer.remaining()/width);count-=count%channels;
                                AudioPcm.read(buffer,encoding,pcm,count);AudioCodec.writePcm(handle,pcm,count);frames+=count/channels;
                            }
                            lastData=SystemClock.elapsedRealtime();
                        }
                        outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                    }finally{decoder.releaseOutputBuffer(slot,false);}
                }
            }
            if(handle==0||frames==0)throw new IOException("Nessun campione audio decodificato");
            long duration=frames*1000000L/rate;
            // MP3 duration may be estimated without a VBR index. Report it, never truncate to it.
            listener.log("DECODIFICA TERMINATA: campioni/canale="+frames+" · durata PCM="+duration+" us · durata contenitore="+expectedUs+" us (puo essere stimata).");
            if(!listener.progress(frames,frames,rate,channels))throw new IOException("Annullato");
            long encodedUs=AudioCodec.finishPcm(handle,listener);
            if(encodedUs!=duration)throw new IOException("Numero campioni inviati a Opus incoerente");
            listener.log("OK: drain Opus, parametri riletti (bitrate="+bitrate+", VBR=1, constrained=0, complexity=10) e tag/copertine riletti e confrontati byte per byte con i campi tradotti da TagLib.");
            return duration;
        }catch(RuntimeException error){throw new IOException("Decodifica MP3/M4A: "+error.getMessage(),error);}
        finally{
            try{if(handle!=0)AudioCodec.closePcm(handle);}
            finally{try{if(decoder!=null){try{if(started)decoder.stop();}finally{decoder.release();}}}finally{extractor.release();}}
        }
    }
    private AudioMediaDecoder(){}
}
