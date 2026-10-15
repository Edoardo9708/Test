package it.edo.codecprobe;

import android.media.*;
import android.os.SystemClock;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.function.BooleanSupplier;

/** Full Main10 path. The caller owns MP4/audio/GPS/export; this class owns only HDR video. */
@android.annotation.TargetApi(33)
final class HdrTranscoder {
    interface Sink {
        void log(String value);void headers(byte[] bytes);void packet(byte[] bytes,long pts,boolean key);
        void frame(long pts)throws IOException;void settings(int[] values);
    }
    private final BooleanSupplier cancelled;private final Sink sink;private final ColorVui color;
    private HdrMetadataLedger ledger;
    private native long nativeOpen(int w,int h,int fps,int crf,int preset,int cpu,int cores,boolean bframes,boolean lookahead,int p,int t,int m,int flags);
    private native void nativeFrame(long handle,ByteBuffer pixels,long pts);
    private native void nativeFinish(long handle);
    private native void nativeClose(long handle);
    private native int[] nativeEffectiveSettings(long handle);
    HdrTranscoder(BooleanSupplier cancelled,Sink sink,ColorVui color){this.cancelled=cancelled;this.sink=sink;this.color=color;}
    public boolean isCancelled(){return cancelled.getAsBoolean();}
    public void onLog(String value){sink.log(value);}
    public void onHeaders(byte[] data){HdrHevcInfo.requireMain10(data);sink.headers(HdrSei.replace(data,Collections.emptyList()));}
    public void onPacket(byte[] data,long pts,boolean key){try{sink.packet(ledger.packet(data,pts),pts,key);}catch(IOException e){throw new IllegalStateException(e);}}
    private void check()throws IOException{if(isCancelled())throw new IOException("Conversione HDR annullata");}
    void run(MediaExtractor extractor,MediaFormat format,File metadataIndex,int w,int h,int fps,int crf,int preset,int cpu,boolean bframes,boolean lookahead)throws Exception{
        if(!"video/hevc".equals(format.getString(MediaFormat.KEY_MIME))||color.primaries!=9||color.matrix!=9||(color.transfer!=16&&color.transfer!=18))
            throw new IOException("HDR supportato: HEVC Main10, BT.2020 non costante, PQ oppure HLG; Dolby Vision non supportato");
        onLog("*** STEP 2/3 · CONVERSIONE HDR COMPLETA A 10 BIT ***");
        onLog("1. Leggo i SEI HDR dai pacchetti originali.\n2. Decodifico in P010 senza passare a 8 bit.\n3. Codifico con x265 Main10 CRF e i parametri selezionati.\n4. Reinserisco i payload HDR originali sullo stesso timestamp.\n5. Confronterò SHA-256 di ogni gruppo di metadati nel file MP4.\nLegenda: frame = fotogramma; pts_us = posizione originale in microsecondi; campioni_precisi = campioni che usano anche i 2 bit aggiuntivi.\nHDR: assembly disabilitato nella libreria a 10 bit; la velocità può essere inferiore a SDR.");
        HdrHevcInfo.requireMain10(HdrCodec.csds(format).toArray(new byte[0][]));
        System.loadLibrary("x265hdrprobe");
        MediaCodec decoder=null;long handle=0;
        try(HdrMetadataLedger index=new HdrMetadataLedger(metadataIndex,HdrCodec.metadata(format))){
            ledger=index;decoder=HdrCodec.open(format,cancelled,this::onLog);
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();boolean inputEnd=false,outputEnd=false;int frames=0;long last=SystemClock.elapsedRealtime(),lastPts=-1;
            ByteBuffer pixels=ByteBuffer.allocateDirect(Math.multiplyExact(Math.multiplyExact(w,h),3));
            while(!outputEnd){
                check();if(SystemClock.elapsedRealtime()-last>60000)throw new IOException("Decoder HDR senza progresso per 60 secondi");
                if(!inputEnd){int i=decoder.dequeueInputBuffer(1000);if(i>=0){ByteBuffer b=decoder.getInputBuffer(i);if(b==null)throw new IOException("Buffer HDR assente");b.clear();
                    long size=extractor.getSampleSize();if(size>b.remaining()||size>32*1024*1024)throw new IOException("Campione HDR troppo grande");
                    int n=extractor.readSampleData(b,0);
                    if(n<0){decoder.queueInputBuffer(i,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;onLog("OK decoder HDR input EOS");}
                    else {if((extractor.getSampleFlags()&(MediaExtractor.SAMPLE_FLAG_ENCRYPTED|MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME))!=0)throw new IOException("Campione HDR protetto/parziale non supportato");
                        byte[] original=new byte[n];ByteBuffer copy=b.duplicate();copy.position(0);copy.limit(n);copy.get(original);long pts=extractor.getSampleTime();index.queued(pts,original);
                        decoder.queueInputBuffer(i,0,n,pts,0);extractor.advance();}last=SystemClock.elapsedRealtime();
                }}
                int i=decoder.dequeueOutputBuffer(info,1000);
                if(i==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){onLog("FORMATO P010 · "+decoder.getOutputFormat());last=SystemClock.elapsedRealtime();}
                else if(i>=0)try{
                    if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){
                        if(info.presentationTimeUs<=lastPts)throw new IOException("Timestamp HDR decodificati non crescenti");lastPts=info.presentationTimeUs;
                        HdrCodec.check(decoder.getOutputFormat(i),color);
                        try(Image image=decoder.getOutputImage(i)){
                            long precise=HdrCodec.pack(image,pixels,w,h);
                            if(handle==0){handle=nativeOpen(w,h,fps,crf,preset,cpu,Runtime.getRuntime().availableProcessors(),bframes,lookahead,color.primaries,color.transfer,color.matrix,color.flags());sink.settings(nativeEffectiveSettings(handle));}
                            String metadata=index.beforeEncode(info.presentationTimeUs);sink.frame(info.presentationTimeUs);
                            onLog("FRAME HDR · numero="+(++frames)+" · pts_us="+info.presentationTimeUs+" · campioni_precisi="+precise+"\nMetadati originali: "+metadata);
                            nativeFrame(handle,pixels,info.presentationTimeUs);
                        }
                    }
                    outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;last=SystemClock.elapsedRealtime();
                }finally{decoder.releaseOutputBuffer(i,false);}
            }
            if(handle==0)throw new IOException("Nessun fotogramma HDR decodificato");
            onLog("*** RECUPERO FOTOGRAMMI HDR IN ATTESA ***");nativeFinish(handle);index.finish();onLog("OK COPIA SEI HDR · "+index.summary());
        }finally{
            if(handle!=0)nativeClose(handle);ledger=null;
            if(decoder!=null){try{decoder.stop();}catch(RuntimeException e){onLog("AVVISO decoder HDR stop: "+e);}decoder.release();}
        }
    }
    static void verifyMetadata(File mp4,File metadataIndex,ColorVui original,BooleanSupplier cancelled,java.util.function.Consumer<String> log)throws Exception{
        log.accept("*** VERIFICA INTEGRALE METADATI HDR NELL’MP4 ***");
        MediaExtractor extractor=new MediaExtractor();
        try{
            extractor.setDataSource(mp4.getAbsolutePath());int selected=-1;for(int i=0;i<extractor.getTrackCount();i++)if("video/hevc".equals(extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME))){selected=i;break;}
            if(selected<0)throw new IOException("Traccia HDR assente");MediaFormat f=extractor.getTrackFormat(selected);
            byte[][] csds=HdrCodec.csds(f).toArray(new byte[0][]);HdrHevcInfo.requireMain10(csds);
            if(!ColorVui.read("video/hevc",csds).same(original))throw new IOException("VUI HDR modificato nel file finale");
            extractor.selectTrack(selected);
            try(HdrMetadataLedger.Verifier verifier=new HdrMetadataLedger.Verifier(metadataIndex,HdrCodec.metadata(f))){
                ByteBuffer buffer=ByteBuffer.allocateDirect(1024*1024);int count=0;
                while(extractor.getSampleTime()>=0){
                    if(cancelled.getAsBoolean())throw new IOException("Verifica HDR annullata");long size=extractor.getSampleSize();if(size<0||size>32*1024*1024)throw new IOException("Dimensione campione HDR non valida");if(size>buffer.capacity())buffer=ByteBuffer.allocateDirect((int)size);
                    buffer.clear();int n=extractor.readSampleData(buffer,0);if(n<0)break;byte[] data=new byte[n];buffer.position(0);buffer.limit(n);buffer.get(data);verifier.sample(extractor.getSampleTime(),data);
                    log.accept("OK VERIFY HDR SEI · frame="+(++count)+" · pts_us="+extractor.getSampleTime()+" · payload originali identici (SHA-256)");extractor.advance();
                }
                int verified=verifier.finish();log.accept("OK VERIFICA HDR COMPLETA · "+verified+" fotogrammi · nessun payload HDR perso, aggiunto o modificato");
            }
        }finally{extractor.release();}
    }
}
