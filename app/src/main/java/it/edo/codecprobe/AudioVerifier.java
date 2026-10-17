package it.edo.codecprobe;

import android.media.*;
import android.os.SystemClock;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.function.BooleanSupplier;

/** Reads and decodes the produced Ogg Opus, independently of the native encoder. */
final class AudioVerifier {
    static void verify(File file,long expectedUs,int expectedChannels,BooleanSupplier cancelled)throws IOException{
        MediaExtractor extractor=new MediaExtractor();MediaCodec decoder=null;boolean started=false;
        try{
            extractor.setDataSource(file.getAbsolutePath());int track=-1;MediaFormat format=null;
            for(int n=0;n<extractor.getTrackCount();n++){MediaFormat f=extractor.getTrackFormat(n);if("audio/opus".equals(f.getString(MediaFormat.KEY_MIME))){track=n;format=f;break;}}
            if(track<0||format==null)throw new IOException("Verifica: nessuna traccia Opus leggibile");
            if(format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)!=expectedChannels)throw new IOException("Verifica: canali output diversi dall’input");
            extractor.selectTrack(track);decoder=MediaCodec.createDecoderByType("audio/opus");decoder.configure(format,null,null,0);decoder.start();started=true;
            boolean inputEnd=false,outputEnd=false;long lastData=SystemClock.elapsedRealtime(),pcmFrames=0;int channels=expectedChannels,rate=48000,bytesPerSample=2;
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            while(!outputEnd){
                if(cancelled.getAsBoolean())throw new IOException("Annullato");
                if(SystemClock.elapsedRealtime()-lastData>15000)throw new IOException("Verifica: decoder audio senza avanzamento per 15 secondi");
                if(!inputEnd){int slot=decoder.dequeueInputBuffer(10000);if(slot>=0){
                    ByteBuffer buffer=decoder.getInputBuffer(slot);if(buffer==null)throw new IOException("Verifica: buffer input decoder assente");
                    int size=extractor.readSampleData(buffer,0);
                    if(size<0){decoder.queueInputBuffer(slot,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;}
                    else{decoder.queueInputBuffer(slot,0,size,extractor.getSampleTime(),0);extractor.advance();}
                }}
                int slot=decoder.dequeueOutputBuffer(info,10000);
                if(slot==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                    MediaFormat out=decoder.getOutputFormat();channels=out.getInteger(MediaFormat.KEY_CHANNEL_COUNT);rate=out.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    if(out.containsKey(MediaFormat.KEY_PCM_ENCODING))bytesPerSample=out.getInteger(MediaFormat.KEY_PCM_ENCODING)==AudioFormat.ENCODING_PCM_FLOAT?4:2;
                    if(channels!=expectedChannels||rate!=48000)throw new IOException("Verifica: formato PCM inatteso "+out);
                }else if(slot>=0){
                    if(info.size>0){pcmFrames+=info.size/(channels*bytesPerSample);lastData=SystemClock.elapsedRealtime();}
                    outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;decoder.releaseOutputBuffer(slot,false);
                }
            }
            long decodedUs=pcmFrames*1000000L/rate;
            // 80 ms covers decoder pre-skip/end trimming differences across Android devices.
            if(pcmFrames<=0||Math.abs(decodedUs-expectedUs)>80000)throw new IOException("Verifica durata PCM: attesi "+expectedUs+" µs, decodificati "+decodedUs+" µs");
        }catch(RuntimeException error){throw new IOException("Verifica Opus: "+error.getMessage(),error);}
        finally{try{if(decoder!=null){try{if(started)decoder.stop();}finally{decoder.release();}}}finally{extractor.release();}}
    }
    private AudioVerifier(){}
}
