package it.edo.codecprobe;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.ImageFormat;
import android.media.*;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Bounded compatibility probe only. Does not export a converted video or drop HDR metadata silently. */
@android.annotation.TargetApi(33)
final class HdrProbe {
    private static final int MAX_FRAMES=6;
    private final Context context;private final BooleanSupplier cancelled;private final Consumer<String> listener;
    private final long start=SystemClock.elapsedRealtime();private Writer log;private RandomAccessFile elementary;
    private final List<Packet> packets=new ArrayList<>();private final List<Long> inputs=new ArrayList<>();
    private HdrMetadataLedger metadata;
    private File metadataFile;
    private byte[] headers;private ColorVui originalColor;private int width,height;private boolean decoderStarted;
    private static final class Packet {long at,pts;int size;Packet(long a,long p,int n){at=a;pts=p;size=n;}}
    private native long nativeOpen(int width,int height,int crf,int p,int t,int m,int flags);
    private native void nativeFrame(long handle,ByteBuffer pixels,long pts);
    private native void nativeFinish(long handle);
    private native void nativeClose(long handle);
    HdrProbe(Context c,BooleanSupplier cancel,Consumer<String> listener){context=c;cancelled=cancel;this.listener=listener;}
    static File logFile(Context c){return new File(c.getFilesDir(),"hdr-last.txt");}
    public boolean isCancelled(){return cancelled.getAsBoolean();}
    private void check()throws IOException{if(isCancelled())throw new IOException("Prova HDR annullata");}
    public void onLog(String value){String line="["+(SystemClock.elapsedRealtime()-start)+" ms] "+value;try{log.write(line+"\n");log.flush();}catch(IOException e){throw new IllegalStateException("Scrittura log HDR fallita",e);}listener.accept(line);}
    public void onHeaders(byte[] bytes){
        bytes=HdrSei.replace(bytes,Collections.emptyList());
        onLog("*** VERIFICA HEADER REALI x265 ***");onLog("OK "+HdrHevcInfo.requireMain10(bytes));
        ColorVui encoded=ColorVui.read("video/hevc",bytes);onLog("COLORE HEVC GENERATO: "+encoded);
        if(!encoded.same(originalColor))throw new IllegalStateException("Colore HDR generato diverso dall’originale");
        headers=bytes.clone();try{elementary.write(bytes);}catch(IOException e){throw new IllegalStateException(e);}
    }
    public void onPacket(byte[] bytes,long pts,boolean key){
        try{bytes=metadata.packet(bytes,pts);}catch(IOException e){throw new IllegalStateException(e);}
        if(bytes.length==0||bytes.length>16*1024*1024||packets.size()>=MAX_FRAMES)throw new IllegalStateException("Pacchetto HDR fuori limite");
        try{packets.add(new Packet(elementary.getFilePointer(),pts,bytes.length));elementary.write(bytes);}catch(IOException e){throw new IllegalStateException(e);}
        onLog("PACCHETTO HDR · indice="+packets.size()+" · pts_us="+pts+" · byte="+bytes.length+" · key="+key);
    }
    String run(Uri source,int crf)throws Exception{
        File file=new File(context.getCacheDir(),"hdr-diagnostic.hevc");
        try(Writer writer=new OutputStreamWriter(new FileOutputStream(logFile(context)),StandardCharsets.UTF_8);RandomAccessFile output=new RandomAccessFile(file,"rw")){
            log=writer;elementary=output;output.setLength(0);
            try{
                onLog("*** INIZIO TEST HDR · v"+BuildConfig.VERSION_NAME+" ***");
                onLog("1. Leggo HEVC Main10 e dichiarazioni colore.\n2. Cerco decoder con uscita P010.\n3. Estraggo al massimo 6 fotogrammi a 10 bit.\n4. Ricodifico con x265 Main10 separato.\n5. Decodifico il campione e verifico fotogrammi e timestamp.");
                onLog("TEST DI COMPATIBILITÀ: nessun MP4 finale, nessuna copia audio, nessuna validazione di qualità visiva.\nHDR10/HDR10+: payload originali copiati per timestamp e verificati tramite SHA-256.\nIl flusso SDR e la sua libreria a 8 bit non vengono modificati.");
                if(Build.VERSION.SDK_INT<33)throw new IOException("Questa prova richiede Android 13/API 33 per richiedere P010 al codec");
                System.loadLibrary("x265hdrprobe");onLog("OK caricamento libreria HDR separata");
                MediaExtractor extractor=new MediaExtractor();
                try{
                    extractor.setDataSource(context,source,null);MediaFormat format=null;int track=-1;
                    for(int i=0;i<extractor.getTrackCount();i++){MediaFormat f=extractor.getTrackFormat(i);if(f.getString(MediaFormat.KEY_MIME)!=null&&f.getString(MediaFormat.KEY_MIME).startsWith("video/")){if(track>=0)throw new IOException("Per la prova serve una sola traccia video");track=i;format=f;}}
                    if(format==null||!"video/hevc".equals(format.getString(MediaFormat.KEY_MIME)))throw new IOException("Prima prova HDR: seleziona un video HEVC Main10 PQ o HLG");
                    onLog("*** ANALISI HDR ORIGINALE ***\n"+format);
                    List<byte[]> csd=new ArrayList<>();for(String key:format.getKeys())if(key.startsWith("csd-")){ByteBuffer b=format.getByteBuffer(key).duplicate();byte[] data=new byte[b.remaining()];b.get(data);csd.add(data);onLog(key+" · byte="+data.length);}
                    onLog("SPS ORIGINALE: "+HdrHevcInfo.requireMain10(csd.toArray(new byte[0][])));
                    originalColor=ColorVui.read("video/hevc",csd.toArray(new byte[0][]));
                    try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(source,"r")){
                        if(fd==null)throw new IOException("Originale assente");try(ParcelFileDescriptor.AutoCloseInputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){originalColor=ContainerColor.check(in.getChannel(),originalColor,this::onLog);}
                    }
                    onLog("COLORE HDR RISOLTO: "+originalColor);
                    if(originalColor.transfer!=16&&originalColor.transfer!=18)throw new IOException("Il video non dichiara PQ o HLG");
                    if(originalColor.primaries!=9||originalColor.matrix!=9)throw new IOException("Prima prova HDR limitata a BT.2020 con matrice non costante (9/9)");
                    dumpMetadata(format,"INPUT");width=NumberValues.intExact(format.getNumber(MediaFormat.KEY_WIDTH),"width");height=NumberValues.intExact(format.getNumber(MediaFormat.KEY_HEIGHT),"height");
                    if(width<16||height<16||width>3840||height>3840||(long)width*height>3840L*2160||width%2!=0||height%2!=0)throw new IOException("Prova HDR: dimensioni pari fino a 3840×2160, anche verticale");
                    extractor.selectTrack(track);format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010);format.setInteger(MediaFormat.KEY_ROTATION,0);
                    MediaCodec decoder=openDecoder(format);
                    metadataFile=new File(context.getCacheDir(),"hdr-diagnostic-metadata.bin");
                    try(HdrMetadataLedger index=new HdrMetadataLedger(metadataFile,HdrCodec.metadata(format))){
                        metadata=index;decodeSample(extractor,decoder,crf);
                    }finally{metadata=null;if(decoderStarted)try{decoder.stop();}catch(RuntimeException ignored){}decoder.release();}
                }finally{extractor.release();}
                if(packets.size()!=inputs.size()||headers==null)throw new IOException("Campione HEVC incompleto");
                verifyMetadata();verify();
                String message="OK PROVA HDR · "+inputs.size()+" fotogrammi reali · P010 → x265 Main10 → decoder verificato";
                onLog(message);onLog("*** FINE TEST HDR ***\nPayload HDR originali verificati; il campione diagnostico temporaneo viene eliminato. La conversione completa usa il percorso HDR separato.");return message;
            }catch(Exception|LinkageError e){onLog("*** ERRORE TEST HDR ***\n"+e);StringWriter stack=new StringWriter();e.printStackTrace(new PrintWriter(stack));onLog(stack.toString());throw e;}
        }finally{file.delete();if(metadataFile!=null)metadataFile.delete();}
    }
    private MediaCodec openDecoder(MediaFormat format)throws IOException{
        MediaCodecList list=new MediaCodecList(MediaCodecList.ALL_CODECS);List<String> candidates=new ArrayList<>();
        for(MediaCodecInfo info:list.getCodecInfos())if(!info.isEncoder()&&Arrays.asList(info.getSupportedTypes()).contains("video/hevc")){
            try{MediaCodecInfo.CodecCapabilities caps=info.getCapabilitiesForType("video/hevc");boolean p010=false;for(int color:caps.colorFormats)if(color==54)p010=true;
                onLog("DECODER "+info.getName()+" · hardware="+info.isHardwareAccelerated()+" · P010_dichiarato="+p010);
                if(p010&&caps.isFormatSupported(format))candidates.add(info.getName());
            }catch(RuntimeException e){onLog("DECODER non interrogabile: "+info.getName()+" · "+e.getClass().getSimpleName());}
        }
        for(String name:candidates){check();MediaCodec decoder=null;try{decoder=MediaCodec.createByCodecName(name);decoder.configure(format,null,null,0);decoder.start();decoderStarted=true;onLog("OK DECODER P010 · "+name);return decoder;}catch(IOException|RuntimeException e){onLog("DECODER FALLITO · "+name+" · "+e);if(decoder!=null)decoder.release();}}
        throw new IOException("Nessun decoder espone P010 accessibile alla CPU per questo formato. Nessuna conversione a 8 bit eseguita.");
    }
    private void dumpMetadata(MediaFormat format,String where){
        if(!format.containsKey(MediaFormat.KEY_HDR_STATIC_INFO)&&!format.containsKey(MediaFormat.KEY_HDR10_PLUS_INFO))onLog("METADATI HDR "+where+": non esposti dal codec in questo formato; questo non prova che il file ne sia privo.");
        for(String key:new String[]{MediaFormat.KEY_HDR_STATIC_INFO,MediaFormat.KEY_HDR10_PLUS_INFO})if(format.containsKey(key))try{ByteBuffer b=format.getByteBuffer(key).duplicate();byte[] bytes=new byte[b.remaining()];b.get(bytes);onLog("METADATI HDR "+where+" · "+key+" · byte="+bytes.length+" · HEX="+ContainerColor.hex(Arrays.copyOf(bytes,Math.min(bytes.length,4096)))+(bytes.length>4096?" · primi 4096 byte":""));}catch(RuntimeException e){onLog("Metadati HDR non leggibili: "+key);}
    }
    private void decodeSample(MediaExtractor extractor,MediaCodec decoder,int crf)throws Exception{
        MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();boolean inputEnd=false,outputEnd=false;long handle=0,last=SystemClock.elapsedRealtime();ByteBuffer pixels=null;
        try{
            while(inputs.size()<MAX_FRAMES&&!outputEnd){check();if(SystemClock.elapsedRealtime()-last>30000)throw new IOException("Timeout decoder HDR: nessun progresso per 30 secondi");
                if(!inputEnd){int index=decoder.dequeueInputBuffer(10000);if(index>=0){ByteBuffer b=decoder.getInputBuffer(index);if(b==null)throw new IOException("Buffer decoder HDR assente");b.clear();long sample=extractor.getSampleSize();if(sample>b.remaining())throw new IOException("Campione HDR troppo grande per il decoder");int n=extractor.readSampleData(b,0);if(n<0){decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;}else{
                        if((extractor.getSampleFlags()&(MediaExtractor.SAMPLE_FLAG_ENCRYPTED|MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME))!=0)throw new IOException("Campione HDR protetto/parziale");
                        byte[] original=new byte[n];ByteBuffer copy=b.duplicate();copy.position(0);copy.limit(n);copy.get(original);metadata.queued(extractor.getSampleTime(),original);
                        decoder.queueInputBuffer(index,0,n,extractor.getSampleTime(),0);extractor.advance();
                    }}}
                int index=decoder.dequeueOutputBuffer(info,10000);
                if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){MediaFormat output=decoder.getOutputFormat();onLog("FORMATO DECODER HDR: "+output);dumpMetadata(output,"DECODER");last=SystemClock.elapsedRealtime();}
                else if(index>=0)try{
                    if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0)try(Image image=decoder.getOutputImage(index)){
                        if(image==null||image.getFormat()!=ImageFormat.YCBCR_P010)throw new IOException("Il decoder non ha restituito Image P010: formato="+(image==null?"null":image.getFormat()));
                        Rect crop=image.getCropRect();if(handle==0){width=crop.width();height=crop.height();pixels=ByteBuffer.allocateDirect(Math.multiplyExact(Math.multiplyExact(width,height),3));handle=nativeOpen(width,height,crf,originalColor.primaries,originalColor.transfer,originalColor.matrix,originalColor.flags());}
                        if(crop.width()!=width||crop.height()!=height)throw new IOException("Cambio dimensioni HDR");
                        Image.Plane[] p=image.getPlanes();ByteBuffer[] planes=new ByteBuffer[p.length];int[] rows=new int[p.length],strides=new int[p.length];for(int i=0;i<p.length;i++){planes[i]=p[i].getBuffer();rows[i]=p[i].getRowStride();strides[i]=p[i].getPixelStride();}
                        onLog("FOTOGRAMMA P010 · indice="+(inputs.size()+1)+" · pts_us="+info.presentationTimeUs+" · crop="+crop+" · row_stride="+Arrays.toString(rows)+" · pixel_stride="+Arrays.toString(strides));
                        long precise=HdrP010Packer.pack(planes,rows,strides,crop.left,crop.top,width,height,pixels);
                        onLog("OK PACK 10 BIT · byte="+pixels.remaining()+" · campioni con bit bassi utilizzati="+precise+" · nessuna riduzione a 8 bit");
                        MediaFormat frameFormat=decoder.getOutputFormat(index);checkDecodedFormat(frameFormat);dumpMetadata(frameFormat,"FRAME "+(inputs.size()+1));
                        onLog("SEI ORIGINALI · "+metadata.beforeEncode(info.presentationTimeUs));
                        inputs.add(info.presentationTimeUs);nativeFrame(handle,pixels,info.presentationTimeUs);
                    }
                    outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;last=SystemClock.elapsedRealtime();
                }finally{decoder.releaseOutputBuffer(index,false);}
            }
            if(handle==0)throw new IOException("Nessun fotogramma HDR decodificato");onLog("*** FLUSH ENCODER HDR ***");nativeFinish(handle);
        }finally{if(handle!=0)nativeClose(handle);}
    }
    private void checkDecodedFormat(MediaFormat f)throws IOException{
        int expectedTransfer=originalColor.transfer==16?MediaFormat.COLOR_TRANSFER_ST2084:MediaFormat.COLOR_TRANSFER_HLG;
        int expectedRange=originalColor.fullRange?MediaFormat.COLOR_RANGE_FULL:MediaFormat.COLOR_RANGE_LIMITED;
        int transfer=f.containsKey(MediaFormat.KEY_COLOR_TRANSFER)?NumberValues.intExact(f.getNumber(MediaFormat.KEY_COLOR_TRANSFER),"color-transfer"):-1;
        int standard=f.containsKey(MediaFormat.KEY_COLOR_STANDARD)?NumberValues.intExact(f.getNumber(MediaFormat.KEY_COLOR_STANDARD),"color-standard"):-1;
        int range=f.containsKey(MediaFormat.KEY_COLOR_RANGE)?NumberValues.intExact(f.getNumber(MediaFormat.KEY_COLOR_RANGE),"color-range"):-1;
        if(transfer!=expectedTransfer||standard!=MediaFormat.COLOR_STANDARD_BT2020||range!=expectedRange)throw new IOException("Colore del buffer HDR non verificabile o modificato: standard="+standard+" transfer="+transfer+" range="+range+" attesi=6/"+expectedTransfer+"/"+expectedRange);
    }
    private void verifyMetadata()throws IOException{
        onLog("*** CONFRONTO PAYLOAD HDR DEL CAMPIONE ***");
        try(HdrMetadataLedger.Verifier verifier=new HdrMetadataLedger.Verifier(metadataFile,Collections.emptyList())){
            for(Packet p:packets){check();byte[] data=new byte[p.size];elementary.seek(p.at);elementary.readFully(data);verifier.sample(p.pts,data);onLog("OK VERIFY SEI HDR · pts_us="+p.pts+" · payload identici (SHA-256)");}
            onLog("OK METADATI HDR · "+verifier.finish()+" fotogrammi verificati");
        }
    }
    private void verify()throws Exception{
        onLog("*** VERIFICA DEL CAMPIONE HDR GENERATO ***");MediaFormat f=MediaFormat.createVideoFormat("video/hevc",width,height);f.setByteBuffer("csd-0",ByteBuffer.wrap(headers));f.setInteger(MediaFormat.KEY_COLOR_FORMAT,54);f.setInteger(MediaFormat.KEY_PROFILE,MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10);
        int max=0;for(Packet p:packets)max=Math.max(max,p.size);f.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,max+65536);
        MediaCodec decoder=openDecoder(f);int fed=0,decoded=0;boolean queuedEnd=false,end=false;MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();long last=SystemClock.elapsedRealtime();
        try{
            while(!end){check();if(SystemClock.elapsedRealtime()-last>30000)throw new IOException("Timeout verifica HDR");
                if(!queuedEnd){int index=decoder.dequeueInputBuffer(10000);if(index>=0){if(fed==packets.size()){decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);queuedEnd=true;}else{Packet p=packets.get(fed++);ByteBuffer b=decoder.getInputBuffer(index);if(b==null||b.capacity()<p.size)throw new IOException("Buffer verifica HDR insufficiente");b.clear();b.limit(p.size);long offset=p.at;while(b.hasRemaining()){int n=elementary.getChannel().read(b,offset);if(n<=0)throw new EOFException();offset+=n;}decoder.queueInputBuffer(index,0,p.size,p.pts,0);}}}
                int index=decoder.dequeueOutputBuffer(info,10000);
                if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){onLog("FORMATO VERIFICA HDR: "+decoder.getOutputFormat());last=SystemClock.elapsedRealtime();}
                else if(index>=0)try{
                    if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){try(Image image=decoder.getOutputImage(index)){if(image==null||image.getFormat()!=54)throw new IOException("Verifica HDR non restituisce P010");checkDecodedFormat(decoder.getOutputFormat(index));if(decoded>=inputs.size()||Math.abs(info.presentationTimeUs-inputs.get(decoded))>1000)throw new IOException("Timestamp HDR modificato");decoded++;onLog("OK DECODE HDR · frame="+decoded+" · pts_us="+info.presentationTimeUs+" · formato=P010");}}
                    end=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;last=SystemClock.elapsedRealtime();
                }finally{decoder.releaseOutputBuffer(index,false);}
            }
            if(decoded!=inputs.size())throw new IOException("Fotogrammi HDR verificati="+decoded+" attesi="+inputs.size());
        }finally{try{decoder.stop();}catch(RuntimeException ignored){}decoder.release();}
    }
}

