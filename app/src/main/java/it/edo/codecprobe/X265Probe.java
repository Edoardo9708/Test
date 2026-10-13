package it.edo.codecprobe;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

final class X265Probe {
    private static native void encode(Session session, int crf);
    static File logFile(Context context) { return new File(context.getFilesDir(), "x265-last.log"); }
    static File outputFile(Context context, int crf) { return new File(context.getFilesDir(), "x265-crf"+crf+".mp4"); }

    static void run(Context context, AtomicBoolean cancelled, Consumer<String> listener) {
        try (Session session = new Session(context, cancelled, listener)) {
            session.onLog("SESSION BEGIN v0.5 "+Build.MANUFACTURER+" "+Build.MODEL
                    +" Android="+Build.VERSION.RELEASE+" ABI="+Arrays.toString(Build.SUPPORTED_ABIS));
            session.onLog("*** INIZIO TEST X265 SOFTWARE ***");
            session.onLog("PROCESSO: is64Bit="+android.os.Process.is64Bit());
            session.onLog("CONFRONTO: stesso input sintetico, 1920x1080, 30 fotogrammi, CRF 24/32, due thread. Compilazione nativa ottimizzata O3; assembly manuale ancora disattivato per isolare questa modifica.");
            session.onLog("PERCORSO:\n1. carico la libreria\nPer ogni CRF:\n2. preparo il file\n3. configuro x265\n4. preparo la traccia\n5. codifico\n6. recupero i fotogrammi in attesa\n7. chiudo MP4\n8. verifico.");
            session.onLog("LEGENDA GENERALE: [N ms] = tempo trascorso da inizio test\n• BEGIN/INIZIO = operazione in corso\n• OK/END = chiamata terminata\n• ERROR = errore. Una chiamata iniziata senza esito indica dove indagare.");
            session.onLog("NOTA: 30 fotogrammi sintetici = 1 secondo di video. La verifica controlla la decodifica, non misura la qualità visiva.");
            session.onLog("*** STEP 1 · CARICAMENTO LIBRERIA ***");
            session.onLog("Carico x265 per eseguire la codifica sulla CPU del telefono.");
            session.onLog("BEGIN loadLibrary x265probe");
            System.loadLibrary("x265probe");
            session.onLog("OK loadLibrary; JNI bridge → x265 C++ → CPU");
            for (int oldCrf : new int[]{24,32}) {
                File old = outputFile(context,oldCrf);
                if (old.exists() && !old.delete()) throw new IllegalStateException("Cannot remove old MP4");
            }
            for (int crf : new int[]{24,32}) {
                if(cancelled.get()) break;
                session.output = outputFile(context,crf);
                session.onLog("*** INIZIO PROCEDURA PER CODIFICA ***");
                session.onLog("*** STEP 2 · PREPARAZIONE FILE MP4 ***");
                session.onLog("Creo il contenitore del video. La riga seguente indica CRF richiesto e nome del file.");
                session.onLog("BEGIN MP4 crf="+crf+" file="+session.output.getName());
                session.muxer = new MediaMuxer(session.output.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                session.packets=0;
                try {
                    encode(session,crf);
                    session.finishMuxer();
                    session.onLog("OK MP4 bytes="+session.output.length());
                    verify(session.output,session);
                } catch (Throwable error) {
                    session.onLog("ERROR "+stack(error));
                    session.finishMuxer();
                    if(!session.output.delete()) session.onLog("WARN partial MP4 could not be deleted");
                    break;
                }
            }
            session.onLog("*** FINE SESSIONE · consultare gli esiti delle singole prove ***");
            session.onLog("SESSION END cancelled="+cancelled.get()
                    +"; synthetic input only, no visual-quality validation; ARM assembly disabled");
        } catch (Throwable error) {
            String message = "ERROR x265 session: "+stack(error);
            try (FileOutputStream log = new FileOutputStream(logFile(context),true)) {
                log.write((message+"\n").getBytes(StandardCharsets.UTF_8));
            } catch (Exception ignored) { Log.e("CodecProbe","Unable to append failure",ignored); }
            listener.accept(message);
        }
    }

    private static String stack(Throwable error) {
        StringWriter text=new StringWriter();
        error.printStackTrace(new PrintWriter(text));
        return text.toString();
    }

    // Callbacks are invoked synchronously on the worker thread.
    private static final class Session implements AutoCloseable {
        final AtomicBoolean cancelled;
        final Consumer<String> listener;
        final FileOutputStream log;
        final long started=SystemClock.elapsedRealtime();
        MediaMuxer muxer;
        boolean muxing;
        int track, packets;
        File output;
        Session(Context context, AtomicBoolean cancelled, Consumer<String> listener) throws Exception {
            this.cancelled=cancelled; this.listener=listener;
            log=new FileOutputStream(logFile(context),false);
        }
        public void onLog(String message) {
            String line="["+ (SystemClock.elapsedRealtime()-started)+" ms] "+message;
            if(message.startsWith("***")) line="\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"+line+"\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
            if(message.startsWith("ERROR")) line="\n!!! ERRORE · PROCEDURA INTERROTTA !!!\n"+line;
            try {
                log.write((line+"\n").getBytes(StandardCharsets.UTF_8));
                log.flush();
            } catch(Exception error) { throw new IllegalStateException("Cannot persist diagnostic log",error); }
            Log.i("CodecProbe",line);
            listener.accept(line);
        }
        public boolean isCancelled() { return cancelled.get(); }
        public void onHeaders(byte[] bytes) {
            onLog("*** STEP 4 · PREPARAZIONE TRACCIA VIDEO ***");
            onLog("Inserisco i parametri HEVC nel file e avvio la scrittura. headers = byte delle intestazioni; track = indice della traccia.");
            onLog("BEGIN muxer.addTrack headers="+bytes.length);
            MediaFormat format=MediaFormat.createVideoFormat("video/hevc",1920,1080);
            format.setByteBuffer("csd-0",ByteBuffer.wrap(bytes));
            format.setInteger(MediaFormat.KEY_FRAME_RATE,30);
            track=muxer.addTrack(format);
            onLog("OK muxer.addTrack track="+track);
            onLog("BEGIN muxer.start");
            muxer.start(); muxing=true;
            onLog("OK muxer.start track="+track);
        }
        public void onPacket(byte[] bytes,long pts,boolean key) {
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            info.set(0,bytes.length,pts,key ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0);
            onLog("INIZIO scrittura pacchetto MP4 numero="+(packets+1));
            muxer.writeSampleData(track,ByteBuffer.wrap(bytes),info);
            packets++;
            onLog("OK muxer.writeSampleData packet="+packets+" pts_us="+pts+" bytes="+bytes.length);
        }
        void finishMuxer() {
            if(muxer==null) return;
            onLog("*** STEP 7 · CHIUSURA FILE MP4 ***");
            onLog("Finalizzo il file e libero le risorse. Seguirà la verifica di lettura.");
            try { if(muxing) { onLog("BEGIN muxer.stop"); muxer.stop(); onLog("OK muxer.stop"); } }
            finally { onLog("BEGIN muxer.release"); muxer.release(); muxer=null; muxing=false; onLog("OK muxer.release"); }
        }
        @Override public void close() throws Exception {
            try { finishMuxer(); } finally { log.close(); }
        }
    }

    private static void verify(File file, Session s) throws Exception {
        s.onLog("*** STEP 8 · VERIFICA VIDEO SALVATO ***");
        s.onLog("Riapro MP4 e decodifico tutti i 30 fotogrammi. La lettura può usare hardware; la codifica precedente usa x265 sulla CPU.");
        s.onLog("BEGIN VERIFY extract+decode all frames; decoder may use hardware (encoding did not)");
        MediaExtractor extractor=new MediaExtractor();
        MediaCodec decoder=null;
        boolean started=false;
        try {
            s.onLog("INIZIO extractor.setDataSource · apro il file salvato");
            extractor.setDataSource(file.getAbsolutePath());
            if(extractor.getTrackCount()!=1) throw new IllegalStateException("Expected one video track");
            MediaFormat format=extractor.getTrackFormat(0);
            s.onLog("EXTRACT format="+format);
            if(!"video/hevc".equals(format.getString(MediaFormat.KEY_MIME))
                    || format.getInteger(MediaFormat.KEY_WIDTH)!=1920
                    || format.getInteger(MediaFormat.KEY_HEIGHT)!=1080)
                throw new IllegalStateException("Unexpected codec or size");
            extractor.selectTrack(0);
            decoder=MediaCodec.createDecoderByType("video/hevc");
            s.onLog("BEGIN decoder.configure name="+decoder.getName());
            decoder.configure(format,null,null,0);
            s.onLog("OK decoder.configure");
            s.onLog("BEGIN decoder.start");
            decoder.start(); started=true;
            s.onLog("OK decoder.start");
            s.onLog("*** INIZIO SEQUENZA FOTOGRAMMI DECODIFICATI ***");
            s.onLog("LEGENDA: output = totale fotogrammi letti\n• pts_us = posizione nel video in microsecondi (1000000 = 1 secondo)\n• bytes = dimensione del buffer decodificato, incluso eventuale padding.");
            s.onLog("LEGENDA: flags è una maschera:\n• 1 fotogramma chiave\n• 2 configurazione codec\n• 4 fine flusso (EOS). Un buffer EOS vuoto non conta come fotogramma.");
            s.onLog("EXTRACT/outputFormat = dettagli tecnici del formato; samples = pacchetti inviati al decoder.");
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            boolean inputEnd=false, outputEnd=false;
            int input=0, output=0;
            long deadline=SystemClock.elapsedRealtime()+60_000;
            while(!outputEnd) {
                if(s.isCancelled()) throw new IllegalStateException("CANCELLED during verification");
                if(SystemClock.elapsedRealtime()>deadline) throw new IllegalStateException("Decoder timeout 60s");
                if(!inputEnd) {
                    int index=decoder.dequeueInputBuffer(10_000);
                    if(index>=0) {
                        ByteBuffer buffer=decoder.getInputBuffer(index);
                        if(buffer==null) throw new IllegalStateException("Null decoder input");
                        buffer.clear();
                        int size=extractor.readSampleData(buffer,0);
                        if(size<0) {
                            decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputEnd=true; s.onLog("OK decoder input EOS samples="+input);
                        } else {
                            long pts=extractor.getSampleTime();
                            decoder.queueInputBuffer(index,0,size,pts,0);
                            input++; extractor.advance();
                        }
                    }
                }
                int index=decoder.dequeueOutputBuffer(info,10_000);
                if(index>=0) {
                    if(info.size>0 && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0) output++;
                    outputEnd=(info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                    s.onLog("DECODE output="+output+" pts_us="+info.presentationTimeUs+" bytes="+info.size+" flags="+info.flags);
                    decoder.releaseOutputBuffer(index,false);
                } else if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    s.onLog("DECODE outputFormat="+decoder.getOutputFormat());
                }
            }
            if(input!=30 || output!=30) throw new IllegalStateException("Count mismatch input="+input+" decoded="+output);
            s.onLog("OK VERIFY decoded=30/30 Full HD; MP4="+file.getName());
            s.onLog("*** VERIFICA RIUSCITA · 30/30 FOTOGRAMMI FULL HD ***");
        } finally {
            if(decoder!=null) {
                try { if(started) decoder.stop(); } finally { decoder.release(); }
            }
            extractor.release();
        }
    }
}
