package it.edo.codecprobe;

import android.media.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.function.BooleanSupplier;

/** Full Main10 path. The caller owns MP4/audio/GPS/export; this class owns only HDR video. */
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
        onLog("1. Leggo i SEI HDR dai pacchetti originali.\n2. Decodifico con HEVC software incluso nell’APK, a 10 bit.\n3. Codifico con x265 Main10 CRF e i parametri selezionati.\n4. Reinserisco i payload HDR originali sullo stesso timestamp.\n5. Confronterò SHA-256 di ogni gruppo di metadati nel file MP4.\nLegenda: frame = fotogramma; pts_us = posizione originale in microsecondi; campioni_precisi = campioni che usano anche i 2 bit aggiuntivi.\nHDR: assembly disabilitato nella libreria a 10 bit; la velocità può essere inferiore a SDR.");
        HdrHevcInfo.requireMain10(HdrCodec.csds(format).toArray(new byte[0][]));
        System.loadLibrary("x265hdrprobe");
        final long[] handle={0};final int[] frames={0};
        try(HdrMetadataLedger index=new HdrMetadataLedger(metadataIndex,HdrCodec.metadata(format))){
            ledger=index;
            SoftwareHevcDecoder.decode(extractor,format,w,h,color,cancelled,this::onLog,index::queued,(pixels,pts,precise)->{
                check();
                if(handle[0]==0){handle[0]=nativeOpen(w,h,fps,crf,preset,cpu,Runtime.getRuntime().availableProcessors(),bframes,lookahead,color.primaries,color.transfer,color.matrix,color.flags());sink.settings(nativeEffectiveSettings(handle[0]));}
                String metadata=index.beforeEncode(pts);sink.frame(pts);
                onLog("FRAME HDR · numero="+(++frames[0])+" · pts_us="+pts+" · campioni_precisi="+precise+"\nMetadati originali: "+metadata);
                nativeFrame(handle[0],pixels,pts);
            });
            if(handle[0]==0)throw new IOException("Nessun fotogramma HDR decodificato");
            onLog("*** RECUPERO FOTOGRAMMI HDR IN ATTESA ***");nativeFinish(handle[0]);index.finish();onLog("OK COPIA SEI HDR · "+index.summary());
        }finally{
            if(handle[0]!=0)nativeClose(handle[0]);ledger=null;
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
