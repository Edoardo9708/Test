package it.edo.codecprobe;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.*;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

final class VideoConverter {
    interface Listener { void log(String text); void progress(int percent, String text); void timeline(long doneUs,long totalUs); }
    private native long nativeOpen(int w,int h,int fpsMilli,int crf,int standard,int transfer,int range,int preset);
    private native void nativeFrame(long handle,ByteBuffer i420,long pts);
    private native void nativeFinish(long handle);
    private native void nativeClose(long handle);
    static File logFile(Context c) { return new File(c.getFilesDir(),"conversion-last.txt"); }
    private final Context context;
    private final AtomicBoolean cancel;
    private final Listener listener;
    private final long start=SystemClock.elapsedRealtime();
    private FileOutputStream log;
    private MediaMuxer muxer;
    private boolean muxing;
    private int videoTrack=-1,audioTrack=-1,width,height,frames,packets,standard,transfer,range;
    private long lastPacket=-1;
    private float fps;
    private int preset;
    private long durationUs;
    private MediaFormat audioFormat;
    VideoConverter(Context c,AtomicBoolean cancel,Listener listener) {context=c;this.cancel=cancel;this.listener=listener;}
    public boolean isCancelled() {return cancel.get();}
    private void checkCancel() throws InterruptedException {if(cancel.get()) throw new InterruptedException("Conversione annullata");}
    public void onLog(String message) {
        String line="["+ (SystemClock.elapsedRealtime()-start)+" ms] "+message;
        if(message.startsWith("***")) line="\n━━━━━━━━━━━━━━━━━━━━━━━━\n"+line+"\n━━━━━━━━━━━━━━━━━━━━━━━━";
        try { log.write((line+"\n").getBytes(StandardCharsets.UTF_8)); log.flush(); }
        catch(IOException e) {throw new IllegalStateException("Impossibile salvare il log",e);}
        listener.log(line);
    }
    public void onHeaders(byte[] headers) {
        onLog("BEGIN muxer.addTrack · creo la traccia HEVC");
        MediaFormat f=MediaFormat.createVideoFormat("video/hevc",width,height);
        f.setByteBuffer("csd-0",ByteBuffer.wrap(headers));
        f.setFloat(MediaFormat.KEY_FRAME_RATE,fps);
        f.setInteger(MediaFormat.KEY_COLOR_STANDARD,standard);
        f.setInteger(MediaFormat.KEY_COLOR_TRANSFER,transfer);
        f.setInteger(MediaFormat.KEY_COLOR_RANGE,range);
        videoTrack=muxer.addTrack(f);
        if(audioFormat!=null) audioTrack=muxer.addTrack(audioFormat);
        onLog("BEGIN muxer.start · avvio contenitore MP4");
        muxer.start();muxing=true;
        onLog("OK muxer.start videoTrack="+videoTrack+" audioTrack="+audioTrack);
    }
    public void onPacket(byte[] data,long pts,boolean key) {
        if(pts<0 || pts<=lastPacket) throw new IllegalStateException("Timestamp HEVC non crescente");
        MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
        info.set(0,data.length,pts,key?MediaCodec.BUFFER_FLAG_KEY_FRAME:0);
        onLog("BEGIN scrittura MP4 packet="+(packets+1)+" pts_us="+pts);
        muxer.writeSampleData(videoTrack,ByteBuffer.wrap(data),info);
        lastPacket=pts;packets++;
        onLog("OK scrittura MP4 packet="+packets+" bytes="+data.length+" key="+key);
    }
    static int integer(MediaFormat f,String key,int fallback) {
        return f.containsKey(key)?f.getInteger(key):fallback;
    }
    private void rejectHdr(MediaFormat f) {
        int t=integer(f,MediaFormat.KEY_COLOR_TRANSFER,0);
        if(t==6 || t==7) throw new IllegalArgumentException("Video HDR non ancora supportato: usa un video SDR a 8 bit.");
    }
    Uri convert(Uri input,Uri folder,int crf,String sourceName,int preset) throws Exception {
        this.preset=preset;
        File temporary=new File(context.getCacheDir(),"conversion-working.mp4");
        Uri destination=null;
        try(FileOutputStream fileLog=new FileOutputStream(logFile(context),false)) {
            log=fileLog;
            try {
                onLog("*** INIZIO CONVERSIONE v0.8 ***");
                onLog("PERCORSO:\n1. Analizzo video e audio\n2. Decodifico i fotogrammi\n3. Ricodifico con x265 CRF\n4. Copio audio AAC\n5. Verifico il video\n6. Salvo nella cartella scelta");
                onLog("QUALITÀ: CRF="+crf+" · valore minore = più dettagli, file generalmente più grande. Il risultato dipende dal contenuto.");
                onLog("INPUT: "+sourceName+"\nPROCESSO 64 bit="+android.os.Process.is64Bit());
                onLog("BEGIN loadLibrary x265probe");
                System.loadLibrary("x265probe");
                onLog("OK loadLibrary · CPU ARM64, O3, assembly NEON; preset selezionato indice="+preset+", due thread.");
                decodeAndEncode(input,temporary,crf);
                checkCancel();
                onLog("*** STEP 5 · VERIFICA VIDEO COMPLETO ***");
                listener.progress(94,"Verifica del video...");
                verify(temporary);
                checkCancel();
                onLog("*** STEP 6 · SALVATAGGIO NELLA CARTELLA SCELTA ***");
                listener.progress(98,"Salvataggio MP4...");
                String stem=sourceName.replaceFirst("\\.[^.]+$","").replaceAll("[^\\p{L}\\p{N}._ -]","_");
                if(stem.length()>70) stem=stem.substring(0,70);
                String name=stem+"-HEVC-CRF"+crf+"-"+System.currentTimeMillis()+".mp4";
                Uri parent=DocumentsContract.buildDocumentUriUsingTree(folder,DocumentsContract.getTreeDocumentId(folder));
                destination=DocumentsContract.createDocument(context.getContentResolver(),parent,"video/mp4",name);
                if(destination==null) throw new IOException("Impossibile creare il file nella cartella");
                try(InputStream in=new FileInputStream(temporary);
                    OutputStream out=context.getContentResolver().openOutputStream(destination,"w")) {
                    if(out==null) throw new IOException("Destinazione non scrivibile");
                    byte[] buffer=new byte[262144];int n;
                    while((n=in.read(buffer))!=-1) {checkCancel();out.write(buffer,0,n);}
                }
                onLog("OK SALVATAGGIO nome="+name+" bytes="+temporary.length());
                onLog("*** CONVERSIONE RIUSCITA ***");
                onLog("SUMMARY frames="+frames+" packets="+packets+" elapsed_ms="+(SystemClock.elapsedRealtime()-start));
                listener.progress(100,"Completato: "+name);
                return destination;
            } catch(Exception | LinkageError e) {
                if(destination!=null) {
                    try {DocumentsContract.deleteDocument(context.getContentResolver(),destination);}
                    catch(Exception cleanup) {onLog("AVVISO: impossibile eliminare il file incompleto: "+cleanup);}
                }
                StringWriter stack=new StringWriter();e.printStackTrace(new PrintWriter(stack));
                onLog(cancel.get()?"*** CONVERSIONE ANNULLATA ***":"*** ERRORE · CONVERSIONE INTERROTTA ***");
                onLog(stack.toString());
                if(e instanceof Exception) throw (Exception)e;
                throw new IllegalStateException("Caricamento codec fallito",e);
            } finally {
                onLog("SESSION END cancelled="+cancel.get());
                if(temporary.exists()&&!temporary.delete()) onLog("AVVISO: file temporaneo non eliminato");
            }
        }
    }
    private void decodeAndEncode(Uri uri,File output,int crf) throws Exception {
        MediaExtractor extractor=new MediaExtractor();
        MediaCodec decoder=null;
        boolean decoderStarted=false;
        long handle=0;
        int inputAudio=-1;
        try {
            onLog("*** STEP 1 · ANALISI DEL FILE ***");
            extractor.setDataSource(context,uri,null);
            MediaFormat video=null;int selected=-1;
            for(int i=0;i<extractor.getTrackCount();i++) {
                MediaFormat f=extractor.getTrackFormat(i);
                onLog("TRACK "+i+" "+f);
                String mime=f.getString(MediaFormat.KEY_MIME);
                if(mime!=null&&mime.startsWith("video/")) {
                    if(video!=null) throw new IllegalArgumentException("File con più tracce video non ancora supportato");
                    video=f;selected=i;
                } else if(mime!=null&&mime.startsWith("audio/")) {
                    if(audioFormat!=null) throw new IllegalArgumentException("Più tracce audio non ancora supportate");
                    if(!"audio/mp4a-latm".equals(mime)) throw new IllegalArgumentException("Audio "+mime+" non ancora supportato: per questa versione serve AAC.");
                    audioFormat=f;inputAudio=i;
                } else onLog("AVVISO: traccia non audio/video esclusa");
            }
            if(video==null) throw new IllegalArgumentException("Nessuna traccia video");
            rejectHdr(video);
            int profile=integer(video,MediaFormat.KEY_PROFILE,0);
            String inputMime=video.getString(MediaFormat.KEY_MIME);
            if(("video/hevc".equals(inputMime)&&profile!=0&&profile!=MediaCodecInfo.CodecProfileLevel.HEVCProfileMain)
                    ||("video/avc".equals(inputMime)&&(profile==16||profile==32||profile==64)))
                throw new IllegalArgumentException("Profilo video non supportato: questa versione richiede SDR a 8 bit.");
            width=video.getInteger(MediaFormat.KEY_WIDTH);height=video.getInteger(MediaFormat.KEY_HEIGHT);
            if(width<16||height<16||width>3840||height>3840||(long)width*height>3840L*2160||width%2!=0||height%2!=0)
                throw new IllegalArgumentException("Supportati video con dimensioni pari, fino a 3840×2160 (anche verticali)");
            fps=30;
            if(video.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                try {fps=video.getFloat(MediaFormat.KEY_FRAME_RATE);}
                catch(ClassCastException e) {fps=video.getInteger(MediaFormat.KEY_FRAME_RATE);}
            } else onLog("AVVISO: fps non dichiarati; uso 30 per x265, conservando i timestamp originali.");
            if(!Float.isFinite(fps)||fps<1||fps>240) throw new IllegalArgumentException("Frequenza video non supportata: "+fps);
            long duration=video.containsKey(MediaFormat.KEY_DURATION)?video.getLong(MediaFormat.KEY_DURATION):0;
            durationUs=duration;
            int rotation=integer(video,MediaFormat.KEY_ROTATION,0);
            standard=integer(video,MediaFormat.KEY_COLOR_STANDARD,height>576?1:2);
            transfer=integer(video,MediaFormat.KEY_COLOR_TRANSFER,3);
            range=integer(video,MediaFormat.KEY_COLOR_RANGE,2);
            onLog("FORMAT dimensioni="+width+"x"+height+" fps_nominali="+fps+" duration_us="+duration+" rotazione="+rotation);
            onLog("AUDIO: "+(audioFormat==null?"assente":"AAC, copia senza ricodifica"));
            muxer=new MediaMuxer(output.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            if(rotation==90||rotation==180||rotation==270) muxer.setOrientationHint(rotation);
            video.setInteger(MediaFormat.KEY_ROTATION,0);
            video.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            extractor.selectTrack(selected);
            String mime=video.getString(MediaFormat.KEY_MIME);
            decoder=MediaCodec.createDecoderByType(mime);
            onLog("*** STEP 2/3 · DECODIFICA E CODIFICA ***");
            onLog("BEGIN decoder.configure nome="+decoder.getName()+"; il decoder può usare hardware, x265 codifica sulla CPU.");
            decoder.configure(video,null,null,0);decoder.start();decoderStarted=true;
            onLog("OK decoder.start");
            onLog("LEGENDA:\n• frame: indice del fotogramma\n• pts_us: posizione nel video, in microsecondi\n• rc: risultato x265 (0 = uscita in attesa, positivo = frame pronto)\n• nals: blocchi HEVC\n• elapsed_ms: durata chiamata\n• packet/bytes: numero e dimensione dei pacchetti MP4");
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            boolean inputEnd=false,outputEnd=false;long lastActivity=SystemClock.elapsedRealtime();
            MediaFormat decoded=video;ByteBuffer pixels=null;
            while(!outputEnd) {
                checkCancel();
                if(SystemClock.elapsedRealtime()-lastActivity>60000) throw new IOException("Decoder bloccato: nessun progresso per 60 secondi");
                if(!inputEnd) {
                    int index=decoder.dequeueInputBuffer(1000);
                    if(index>=0) {
                        ByteBuffer b=decoder.getInputBuffer(index);
                        if(b==null) throw new IOException("Buffer decoder assente");
                        b.clear();long sampleSize=extractor.getSampleSize();
                        if(sampleSize>b.capacity()) throw new IOException("Pacchetto video troppo grande per decoder");
                        int size=extractor.readSampleData(b,0);
                        if(size<0) {decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;onLog("OK decoder input EOS");}
                        else {
                            if((extractor.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_ENCRYPTED)!=0) throw new IOException("Video protetto non supportato");
                            decoder.queueInputBuffer(index,0,size,extractor.getSampleTime(),0);extractor.advance();
                        }
                        lastActivity=SystemClock.elapsedRealtime();
                    }
                }
                int index=decoder.dequeueOutputBuffer(info,1000);
                if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    decoded=decoder.getOutputFormat();rejectHdr(decoded);onLog("DECODE FORMAT "+decoded);
                    lastActivity=SystemClock.elapsedRealtime();
                } else if(index>=0) {
                    try {
                        if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0) {
                            try(Image image=decoder.getOutputImage(index)) {
                                if(image==null||image.getFormat()!=ImageFormat.YUV_420_888)
                                    throw new IOException("Il decoder non fornisce fotogrammi YUV420 a 8 bit: formato non supportato");
                                Rect crop=image.getCropRect();
                                if(handle==0) {
                                    width=crop.width();height=crop.height();
                                    if(width%2!=0||height%2!=0||crop.left%2!=0||crop.top%2!=0) throw new IOException("Ritaglio YUV non allineato");
                                    standard=integer(decoded,MediaFormat.KEY_COLOR_STANDARD,standard);
                                    transfer=integer(decoded,MediaFormat.KEY_COLOR_TRANSFER,transfer);
                                    range=integer(decoded,MediaFormat.KEY_COLOR_RANGE,range);
                                    if(standard==0) standard=height>576?1:2;
                                    if(transfer==0) transfer=3;
                                    if(range==0) range=2;
                                    onLog("YUV crop="+crop+" standard="+standard+" transfer="+transfer+" range="+range+" (default SDR per valori non dichiarati)");
                                    handle=nativeOpen(width,height,Math.round(fps*1000),crf,standard,transfer,range,preset);
                                    pixels=ByteBuffer.allocateDirect(width*height*3/2);
                                }
                                if(crop.width()!=width||crop.height()!=height) throw new IOException("Cambio risoluzione durante il video non supportato");
                                onLog("BEGIN conversione YUV frame="+frames+" pts_us="+info.presentationTimeUs);
                                packImage(image,pixels,width,height);
                                onLog("OK conversione YUV · invio a x265");
                                nativeFrame(handle,pixels,info.presentationTimeUs);frames++;
                                int percent=duration>0?(int)Math.min(89,Math.max(0,info.presentationTimeUs)*89.0/duration):0;
                                listener.timeline(duration>0?Math.min(duration,Math.max(0,lastPacket)):Math.max(0,lastPacket),duration);
                                listener.progress(percent,"Codifica: "+frames+" fotogrammi · "+percent+"%");
                            }
                        }
                        outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                    } finally {decoder.releaseOutputBuffer(index,false);}
                    lastActivity=SystemClock.elapsedRealtime();
                }
            }
            if(handle==0) throw new IOException("Nessun fotogramma decodificato");
            nativeFinish(handle);
            listener.timeline(durationUs>0?durationUs:Math.max(0,lastPacket),durationUs);
            onLog("BEGIN nativeClose");nativeClose(handle);handle=0;onLog("OK nativeClose");
            if(frames!=packets) throw new IOException("Frame e pacchetti diversi: "+frames+"/"+packets);
            if(inputAudio>=0) {listener.progress(91,"Copia audio...");copyAudio(uri,inputAudio);}
            onLog("BEGIN muxer.stop");muxer.stop();muxing=false;onLog("OK muxer.stop");
        } finally {
            if(handle!=0) nativeClose(handle);
            if(decoder!=null) {
                try {if(decoderStarted)decoder.stop();} catch(Exception e){onLog("AVVISO decoder.stop: "+e);}
                decoder.release();
            }
            extractor.release();
            if(muxer!=null) {
                try {if(muxing)muxer.stop();} catch(Exception e){onLog("AVVISO muxer.stop: "+e);}
                muxer.release();muxer=null;muxing=false;
            }
        }
    }
    static void packImage(Image image,ByteBuffer target,int w,int h) throws IOException {
        Rect c=image.getCropRect();
        Image.Plane[] planes=image.getPlanes();
        if(planes.length!=3||c.left%2!=0||c.top%2!=0) throw new IOException("Piani YUV non supportati");
        target.clear();
        for(int p=0;p<3;p++) {
            int shift=p==0?0:1,rows=h>>shift,cols=w>>shift;
            Image.Plane plane=planes[p];
            ByteBuffer source=plane.getBuffer().duplicate();
            YuvPacker.copyPlane(source,plane.getRowStride(),plane.getPixelStride(),c.left>>shift,c.top>>shift,cols,rows,target);
        }
        target.flip();
    }
    private void copyAudio(Uri uri,int track) throws Exception {
        onLog("*** STEP 4 · COPIA AUDIO AAC ***");
        onLog("LEGENDA audio: sample = pacchetto AAC; pts_us = timestamp originale, conservato per sincronizzazione.");
        MediaExtractor e=new MediaExtractor();
        try {
            e.setDataSource(context,uri,null);e.selectTrack(track);
            ByteBuffer buffer=ByteBuffer.allocateDirect(1024*1024);int count=0;long last=-1;
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            while(e.getSampleTime()>=0) {
                checkCancel();long size=e.getSampleSize();
                if(size>16*1024*1024) throw new IOException("Pacchetto AAC eccessivo");
                if(size>buffer.capacity()) buffer=ByteBuffer.allocateDirect((int)size);
                buffer.clear();int n=e.readSampleData(buffer,0);if(n<0)break;
                if((e.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_ENCRYPTED)!=0) throw new IOException("Audio protetto non supportato");
                long pts=e.getSampleTime();
                if(pts<=last) throw new IOException("Timestamp audio non crescente");
                info.set(0,n,pts,0);muxer.writeSampleData(audioTrack,buffer,info);
                onLog("OK audio sample="+(++count)+" pts_us="+pts+" bytes="+n);
                last=pts;e.advance();
            }
            if(count==0) throw new IOException("Traccia audio vuota");
            onLog("OK copia audio samples="+count);
        } finally {e.release();}
    }
    private void verify(File file) throws Exception {
        MediaExtractor e=new MediaExtractor();MediaCodec d=null;boolean running=false;
        try {
            e.setDataSource(file.getAbsolutePath());int track=-1;
            for(int i=0;i<e.getTrackCount();i++) if("video/hevc".equals(e.getTrackFormat(i).getString(MediaFormat.KEY_MIME))) {track=i;break;}
            if(track<0) throw new IOException("Traccia HEVC mancante");
            MediaFormat format=e.getTrackFormat(track);
            if(format.getInteger(MediaFormat.KEY_WIDTH)!=width||format.getInteger(MediaFormat.KEY_HEIGHT)!=height) throw new IOException("Risoluzione MP4 errata");
            if(e.getTrackCount()!=(audioFormat==null?1:2)) throw new IOException("Conteggio tracce errato");
            e.selectTrack(track);d=MediaCodec.createDecoderByType("video/hevc");d.configure(format,null,null,0);d.start();running=true;
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();boolean inEnd=false,outEnd=false;int count=0;
            long lastActivity=SystemClock.elapsedRealtime(),lastPts=-1;
            while(!outEnd) {
                checkCancel();
                if(SystemClock.elapsedRealtime()-lastActivity>60000)throw new IOException("Timeout verifica decoder");
                if(!inEnd) {
                    int i=d.dequeueInputBuffer(1000);
                    if(i>=0) {
                        ByteBuffer b=d.getInputBuffer(i);if(b==null)throw new IOException("Buffer verifica assente");
                        b.clear();int n=e.readSampleData(b,0);
                        if(n<0){d.queueInputBuffer(i,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inEnd=true;}
                        else {d.queueInputBuffer(i,0,n,e.getSampleTime(),0);e.advance();}
                        lastActivity=SystemClock.elapsedRealtime();
                    }
                }
                int i=d.dequeueOutputBuffer(info,1000);
                if(i>=0) {
                    if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0) {
                        if(info.presentationTimeUs<=lastPts)throw new IOException("Timestamp verifica non crescente");
                        lastPts=info.presentationTimeUs;count++;
                        onLog("VERIFY frame="+count+" pts_us="+lastPts);
                    }
                    outEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;d.releaseOutputBuffer(i,false);
                    lastActivity=SystemClock.elapsedRealtime();
                }
            }
            if(count!=frames)throw new IOException("Verifica fallita: "+count+"/"+frames);
            onLog("OK VERIFY decoded="+count+"/"+frames+" dimensioni="+width+"x"+height);
        } finally {
            if(d!=null){try{if(running)d.stop();}catch(Exception ignored){}d.release();}
            e.release();
        }
    }
}
