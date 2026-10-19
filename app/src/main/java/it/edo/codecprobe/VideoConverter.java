package it.edo.codecprobe;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.*;
import android.net.Uri;
import android.os.SystemClock;
import android.os.ParcelFileDescriptor;
import java.util.ArrayList;
import java.util.List;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

final class VideoConverter {
    interface Listener { void log(String text); void progress(int percent, String text); void timeline(long doneUs,long totalUs); default void frames(int done,int total){} default void encoderSettings(int b,int l,int tree,int aq,int strength){} default void memoryProtection(boolean active){} default String runtimeState(){return "stato servizio non disponibile";} default String cancellationReason(){return "Annullamento richiesto";} }
    private native long nativeOpen(int w,int h,int fpsMilli,int crf,int standard,int transfer,int range,int preset,int cpuMode,int cores,boolean bframes,boolean lookahead,int primaries,int colorTransfer,int matrix,int colorFlags);
    private native void nativeFrame(long handle,ByteBuffer i420,long pts);
    private native void nativeFinish(long handle);
    private native void nativeClose(long handle);
    private native int[] nativeEffectiveSettings(long handle);
    private long perfWall,perfCpu;private int perfPackets;
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
    private ColorVui sourceColor;
    private int preset,cpuMode;
    private boolean bframes,lookahead;
    private DataOutputStream inputTimestamps;
    private File timestampFile,hdrMetadataFile;
    private long fallbackCreationSeconds;
    private VideoDateAudit.Snapshot originalDates;
    private boolean hdr;
    private long durationUs;
    private MediaFormat audioFormat;
    private int originalAudio=-1;
    private int totalFrames=-1;
    private static final class MetadataTrack {final int input;final MediaFormat format;int output;MetadataTrack(int i,MediaFormat f){input=i;format=f;}}
    private final List<MetadataTrack> metadataTracks=new ArrayList<>();
    private AppSettings namingDefaults;
    VideoConverter(Context c,AtomicBoolean cancel,Listener listener) {context=c;this.cancel=cancel;this.listener=listener;}
    VideoConverter(Context c,AtomicBoolean cancel,Listener listener,AppSettings naming) {this(c,cancel,listener);namingDefaults=naming;}
    ConversionControl control;
    public boolean isCancelled() {return cancel.get() || control != null && control.checkpoint();}
    private boolean stopping() {return cancel.get() || control != null && control.stopping();}
    private long activeNow() {return SystemClock.elapsedRealtime() - (control == null ? 0 : control.pausedMillis());}
    private void checkCancel() throws InterruptedException {if(isCancelled()) throw new InterruptedException("Conversione interrotta su richiesta");}
    private void finishFile() throws InterruptedException {
        checkCancel();
        if(control != null && !control.finishFile())throw new InterruptedException("Conversione interrotta prima del completamento");
    }
    private final CompactLog compactLog=new CompactLog(this::writeLog);
    public void onLog(String message) {compactLog.accept(message,SystemClock.elapsedRealtime()-start);}
    private void writeLog(String message) {
        String line="["+ (SystemClock.elapsedRealtime()-start)+" ms] "+message;
        if(message.startsWith("***")) line="\n━━━━━━━━━━━━━━━━━━━━━━━━\n"+line+"\n━━━━━━━━━━━━━━━━━━━━━━━━";
        try { log.write((line+"\n").getBytes(StandardCharsets.UTF_8)); log.flush(); }
        catch(IOException e) {throw new IllegalStateException("Impossibile salvare il log",e);}
        listener.log(line);
    }
    public void onHeaders(byte[] headers) {
        if(hdr)onLog("OK "+HdrHevcInfo.requireMain10(headers));
        ColorVui encodedColor=ColorVui.read("video/hevc",headers);
        onLog("COLORE USCITA x265: "+encodedColor);
        if(!sourceColor.same(encodedColor))throw new IllegalStateException("Dichiarazione colore x265 diversa dall’originale: conversione interrotta");
        onLog("OK COLORE: VUI originale e VUI x265 identici");
        onLog("BEGIN muxer.addTrack · creo la traccia HEVC");
        MediaFormat f=MediaFormat.createVideoFormat("video/hevc",width,height);
        f.setByteBuffer("csd-0",ByteBuffer.wrap(headers));
        f.setFloat(MediaFormat.KEY_FRAME_RATE,fps);
        if(hdr)f.setInteger(MediaFormat.KEY_PROFILE,MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10);
        f.setInteger(MediaFormat.KEY_COLOR_STANDARD,standard);
        f.setInteger(MediaFormat.KEY_COLOR_TRANSFER,transfer);
        f.setInteger(MediaFormat.KEY_COLOR_RANGE,range);
        videoTrack=muxer.addTrack(f);
        if(audioFormat!=null) audioTrack=muxer.addTrack(audioFormat);
        for(MetadataTrack track:metadataTracks)track.output=muxer.addTrack(track.format);
        onLog("BEGIN muxer.start · avvio contenitore MP4");
        muxer.start();muxing=true;
        onLog("OK muxer.start videoTrack="+videoTrack+" audioTrack="+audioTrack);
    }
    public void onPacket(byte[] data,long pts,boolean key) {
        if(pts<0 || (!bframes && pts<=lastPacket)) throw new IllegalStateException("Timestamp HEVC non valido");
        MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
        info.set(0,data.length,pts,key?MediaCodec.BUFFER_FLAG_KEY_FRAME:0);
        onLog("BEGIN scrittura MP4 packet="+(packets+1)+" pts_us="+pts);
        muxer.writeSampleData(videoTrack,ByteBuffer.wrap(data),info);
        lastPacket=Math.max(lastPacket,pts);packets++;
        samplePerformance();
        listener.frames(packets,totalFrames);
        onLog("OK scrittura MP4 packet="+packets+" bytes="+data.length+" key="+key);
    }
    private void samplePerformance(){
        long now=SystemClock.elapsedRealtime();if(perfWall!=0&&now-perfWall<5000)return;
        long cpu=android.os.Process.getElapsedCpuTime();int cores=Runtime.getRuntime().availableProcessors();
        if(perfWall!=0){
            double seconds=(now-perfWall)/1000.0;
            String state=listener.runtimeState();
            try{
                android.os.PowerManager pm=context.getSystemService(android.os.PowerManager.class);
                android.content.Intent battery=context.registerReceiver(null,new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
                String temp=battery!=null&&battery.hasExtra(android.os.BatteryManager.EXTRA_TEMPERATURE)?String.valueOf(battery.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE,0)/10.0):"non disponibile";
                android.app.ActivityManager.RunningAppProcessInfo info=new android.app.ActivityManager.RunningAppProcessInfo();android.app.ActivityManager.getMyMemoryState(info);
                state+=" schermo_attivo="+pm.isInteractive()+" risparmio_energia="+pm.isPowerSaveMode()+" esente_ottimizzazione_batteria="+pm.isIgnoringBatteryOptimizations(context.getPackageName())+" stato_termico="+pm.getCurrentThermalStatus()+" temperatura_batteria_C="+temp+" importanza_processo="+info.importance+" priorita_thread_conversione="+android.os.Process.getThreadPriority(android.os.Process.myTid());
            }catch(RuntimeException e){state+=" lettura_stato_non_disponibile="+e.getClass().getSimpleName();}
            onLog(String.format(java.util.Locale.US,"PRESTAZIONI · finestra_s=%.1f · fps_codifica=%.2f · CPU_app_percento=%.1f · core=%d\n%s",seconds,(packets-perfPackets)/seconds,100.0*(cpu-perfCpu)/(now-perfWall)/cores,cores,state));
        }
        perfWall=now;perfCpu=cpu;perfPackets=packets;
    }
    static int integer(MediaFormat f,String key,int fallback) {
        return f.containsKey(key)?NumberValues.intExact(f.getNumber(key),key):fallback;
    }
    private void rejectHdr(MediaFormat f) {
        int t=integer(f,MediaFormat.KEY_COLOR_TRANSFER,0);
        if(t==6 || t==7) throw new IllegalArgumentException("Dichiarazione HDR rilevata nel percorso SDR: conversione interrotta per evitare una riduzione a 8 bit.");
    }
    String plannedOutputName;
    boolean onlySmaller;
    long originalByteSize=-1;
    Uri convert(Uri input,Uri folder,int crf,String sourceName,int preset,int cpuMode,boolean bframes,boolean lookahead) throws Exception {
        final AppSettings naming=namingDefaults!=null?namingDefaults:new AppSettings(context);
        this.bframes=bframes;this.lookahead=lookahead;
        this.cpuMode=cpuMode;
        this.preset=preset;
        File temporary=new File(context.getCacheDir(),"conversion-working.mp4");
        Uri destination=null;
        Throwable pendingFailure=null;
        try(FileOutputStream fileLog=new FileOutputStream(logFile(context),false)) {
            log=fileLog;
            try {
                checkCancel();
                if(plannedOutputName!=null)OutputDocuments.requireFree(context,folder,plannedOutputName);
                onLog("*** INIZIO CONVERSIONE v"+BuildConfig.VERSION_NAME+" ***");
                onLog("LOG COMPATTO: primi due esempi per tipo, poi riepiloghi ogni circa 15 secondi. Errori e ultimi 24 eventi dettagliati conservati; la verifica continua a controllare tutti i fotogrammi.");
                onLog("PERCORSO:\n1. Analizzo video e audio\n2. Decodifico i fotogrammi\n3. Ricodifico con x265 CRF\n4. Copio audio AAC\n5. Verifico il video\n6. Salvo nella cartella scelta");
                onLog("QUALITÀ: CRF="+crf+" · valore minore = più dettagli, file generalmente più grande. Il risultato dipende dal contenuto.");
                onLog("INPUT: "+sourceName+"\nPROCESSO 64 bit="+android.os.Process.is64Bit());
                onLog("*** STEP 1 · APERTURA ORIGINALE E POSIZIONE GPS ***");
                onLog("1. Controllo il permesso per la posizione registrata nel file.\n2. Risolvo il video selezionato allo stesso elemento MediaStore.\n3. Richiedo i byte originali senza oscuramento.\n4. Leggo il GPS e lo confronterò con il risultato.");
                onLog("GPS PERMESSI: posizione="+OriginalMedia.hasLocationPermission(context)+"; lettura video="+OriginalMedia.hasVideoPermission(context));
                onLog("GPS PROVIDER SELEZIONATO: "+input.getAuthority());
                input=OriginalMedia.resolve(context,input);
                originalByteSize=MediaFileSizes.read(context,input);
                onLog("GPS ACCESSO: permesso concesso; requireOriginal="+OriginalMedia.requiresOriginal(input));
                if(!OriginalMedia.requiresOriginal(input))onLog("GPS AVVISO: provider esterno a MediaStore; non posso garantire che il provider consegni coordinate non oscurate.");
                final String sourceGps=OriginalMedia.location(context,input);
                onLog("GPS ORIGINALE: "+OriginalMedia.display(sourceGps));
                onLog("*** INFORMAZIONI FILE IN INPUT ***");
                onLog(InputInspector.describe(context,input,this::isCancelled));
                onLog("*** CONTROLLO CONSERVAZIONE METADATI ***");
                sourceMetadata(input,null);
                onLog("L’analisi del colore sceglierà la libreria CPU separata: SDR a 8 bit oppure HDR Main10.");
                timestampFile=new File(context.getCacheDir(),"conversion-timestamps.bin");
                try(DataOutputStream timestamps=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(timestampFile)))) {
                    inputTimestamps=timestamps;decodeAndEncode(input,temporary,crf);
                }
                inputTimestamps=null;
                checkCancel();
                listener.progress(93,"Conservazione metadati originali…");
                sourceMetadata(input,temporary);
                onLog("*** VERIFICA POSIZIONE GPS DEL VIDEO CONVERTITO ***");
                verifyGps(sourceGps,OriginalMedia.location(temporary),"MP4 temporaneo");
                onLog("*** STEP 5 · VERIFICA VIDEO COMPLETO ***");
                listener.progress(94,"Verifica del video...");
                verify(temporary);
                if(hdr)HdrTranscoder.verifyMetadata(temporary,hdrMetadataFile,sourceColor,this::isCancelled,this::onLog);
                if(originalAudio>=0)verifyCopiedTrack(input,temporary,originalAudio,audioTrack);
                for(MetadataTrack track:metadataTracks)verifyCopiedTrack(input,temporary,track.input,track.output);
                checkCancel();
                long candidateBytes=temporary.length();
                SavingsPolicy.Decision saving=SavingsPolicy.decide(onlySmaller,originalByteSize,candidateBytes);
                onLog("*** CONTROLLO RISPARMIO PRIMA DEL SALVATAGGIO ***\nSalva solo se più piccolo="+onlySmaller+"\nOriginale byte="+originalByteSize+"\nConvertito completo byte="+candidateBytes+"\nEsito="+saving);
                if(saving!=SavingsPolicy.Decision.SAVE){finishFile();throw new SavingsPolicy.NotSaved(saving,originalByteSize,candidateBytes);}
                onLog("*** STEP 6 · SALVATAGGIO NELLA CARTELLA SCELTA ***");
                listener.progress(98,"Salvataggio MP4...");
                String requested=plannedOutputName!=null?plannedOutputName:OutputName.base(sourceName,naming.addition,naming.prefix);
                java.util.Set<String> occupied=new java.util.HashSet<>();
                String folderId=DocumentsContract.isDocumentUri(context,folder)?DocumentsContract.getDocumentId(folder):DocumentsContract.getTreeDocumentId(folder);
                Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(folder,folderId);
                try(android.database.Cursor cursor=context.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
                    if(cursor==null)throw new IOException("Impossibile controllare i nomi nella cartella di destinazione");
                    while(cursor.moveToNext()){checkCancel();if(!cursor.isNull(0))occupied.add(cursor.getString(0).toLowerCase(java.util.Locale.ROOT));}
                }
                if(plannedOutputName!=null && occupied.contains(requested.toLowerCase(java.util.Locale.ROOT)))throw new IOException("Il nome scelto è stato occupato dopo il controllo iniziale: "+requested+". Nessun file sovrascritto; scegli un altro nome e riprova.");
                String name=plannedOutputName!=null?requested:OutputName.unique(requested,occupied);
                onLog("NOME OUTPUT · posizione="+(naming.prefix?"prefisso":"suffisso")+" · testo="+naming.addition+"\nNome richiesto: "+requested+"\nNome scelto senza sovrascrittura: "+name);
                Uri parent=DocumentsContract.buildDocumentUriUsingTree(folder,folderId);
                checkCancel();
                destination=DocumentsContract.createDocument(context.getContentResolver(),parent,"video/mp4",name);
                if(destination==null) throw new IOException("Impossibile creare il file nella cartella");
                if(destination.equals(input)){destination=null;throw new IOException("Il provider ha restituito l’originale come destinazione: scrittura rifiutata");}
                try(android.database.Cursor cursor=context.getContentResolver().query(destination,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())onLog("NOME FINALE DEL PROVIDER: "+cursor.getString(0));}
                try(InputStream in=new FileInputStream(temporary);
                    OutputStream out=context.getContentResolver().openOutputStream(destination,"w")) {
                    if(out==null) throw new IOException("Destinazione non scrivibile");
                    byte[] buffer=new byte[262144];int n;
                    while((n=in.read(buffer))!=-1) {checkCancel();out.write(buffer,0,n);}
                }
                onLog("*** VERIFICA GPS DEL FILE SALVATO ***");
                // Read the created document directly: it may not be indexed yet.
                // ACCESS_MEDIA_LOCATION is already granted; no redacted fallback is used for the source.
                verifyGps(sourceGps,OriginalMedia.location(context,destination),"file salvato");
                VideoDateAudit.verify(originalDates,VideoDateAudit.saved(context,destination,this::isCancelled,this::onLog),fallbackCreationSeconds,"FILE SALVATO",this::onLog);
                finishFile();
                onLog("OK SALVATAGGIO nome="+name+" bytes="+temporary.length());
                onLog("*** CONVERSIONE RIUSCITA ***");
                onLog("SUMMARY frames="+frames+" packets="+packets+" elapsed_ms="+(SystemClock.elapsedRealtime()-start));
                listener.progress(100,"Completato: "+name);
                return destination;
            } catch(SavingsPolicy.NotSaved skipped) {
                pendingFailure=skipped;
                onLog("*** RISULTATO NON SALVATO ***\n"+skipped.getMessage()+"\nNessun documento di destinazione creato; elimino solo il temporaneo di questa conversione.");
                listener.progress(100,skipped.getMessage());throw skipped;
            } catch(Exception | LinkageError e) {
                pendingFailure=e;
                if(destination!=null) {
                    try {if(!DocumentsContract.deleteDocument(context.getContentResolver(),destination))throw new IOException("Il provider ha rifiutato la cancellazione");}
                    catch(Exception cleanup) {
                        ConversionCleanup.Failed problem=new ConversionCleanup.Failed("Impossibile eliminare il risultato incompleto: "+destination,cleanup);
                        problem.addSuppressed(e);pendingFailure=problem;
                        onLog("*** ERRORE PULIZIA FILE INCOMPLETO ***\n"+problem+"\nControlla la cartella di destinazione.");
                        throw problem;
                    }
                }
                StringWriter stack=new StringWriter();e.printStackTrace(new PrintWriter(stack));
                onLog(stopping()?"*** CONVERSIONE INTERROTTA SU RICHIESTA ***":"*** ERRORE · CONVERSIONE INTERROTTA ***");
                if(stopping())onLog("MOTIVO: "+listener.cancellationReason());
                onLog(stack.toString());
                if(e instanceof Exception) throw (Exception)e;
                throw new IllegalStateException("Caricamento codec fallito",e);
            } finally {
                onLog("SESSION END cancelled="+cancel.get()+" skipped="+(control!=null&&control.skipped()));
                try {ConversionCleanup.temporaryFiles(timestampFile,hdrMetadataFile,temporary);}
                catch(ConversionCleanup.Failed cleanup) {
                    if(pendingFailure!=null)cleanup.addSuppressed(pendingFailure);
                    onLog("*** ERRORE PULIZIA TEMPORANEI ***\n"+cleanup);throw cleanup;
                }
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
                    audioFormat=f;inputAudio=i;originalAudio=i;
                } else if(mime!=null&&mime.startsWith("application/")){
                    metadataTracks.add(new MetadataTrack(i,f));onLog("Traccia metadati temporizzati da conservare: "+mime);
                } else throw new IllegalArgumentException("Traccia "+mime+" non supportata: conversione interrotta per evitare la perdita dei metadati");
            }
            if(video==null) throw new IllegalArgumentException("Nessuna traccia video");
            int profile=integer(video,MediaFormat.KEY_PROFILE,0);
            String inputMime=video.getString(MediaFormat.KEY_MIME);
            java.util.List<byte[]> csds=new java.util.ArrayList<>();
            for(String key:video.getKeys())if(key.startsWith("csd-")){ByteBuffer b=video.getByteBuffer(key).duplicate();byte[] bytes=new byte[b.remaining()];b.get(bytes);csds.add(bytes);}
            onLog("*** DIAGNOSTICA COLORE ORIGINALE ***");
            onLog("1. Esporto le intestazioni originali del codec (CSD).\n2. Leggo la descrizione colore nel flusso SPS/VUI.\n3. Leggo il blocco colr dell’MP4.\n4. Elenco ogni differenza prima dell’eventuale arresto.\nI fotogrammi non sono inclusi in questa esportazione.");
            for(int i=0;i<csds.size();i++){
                byte[] data=csds.get(i);int shown=Math.min(data.length,512);
                onLog("CSD intestazione #"+i+" · MIME="+inputMime+" · byte="+data.length+"\nHEX: "+ContainerColor.hex(java.util.Arrays.copyOf(data,shown))+(shown<data.length?" · troncato ai primi 512 byte":" · completo"));
            }
            sourceColor=ColorVui.read(inputMime,csds.toArray(new byte[0][]));
            onLog("*** COLORE NEL FLUSSO VIDEO · SPS/VUI ***");
            onLog("Primarie: "+sourceColor.primaries+"\nTrasferimento: "+sourceColor.transfer+"\nMatrice: "+sourceColor.matrix+"\nRange: "+(sourceColor.fullRange?"completo":"limitato")+"\nSegnale video presente: "+sourceColor.signalPresent+"\nDescrizione colore presente: "+sourceColor.descriptionPresent+"\nFormato video: "+sourceColor.videoFormat+"\nCodice 2: non specificato. Se un campo non è presente, il valore mostrato deriva dalle regole SPS, non da una dichiarazione esplicita.");
            try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(uri,"r")){
                if(fd==null)throw new IOException("Originale assente durante il controllo colore");
                try(ParcelFileDescriptor.AutoCloseInputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){sourceColor=ContainerColor.check(in.getChannel(),sourceColor,this::onLog);}
            }
            onLog("*** CONSERVAZIONE DICHIARAZIONE COLORE ***");
            onLog("COLORE FLUSSO DA CONSERVARE (SPS/VUI): "+sourceColor+" · colr conservato separatamente nel contenitore; nessuna traduzione dell’enum SDR Android");
            hdr=sourceColor.transfer==16||sourceColor.transfer==18;
            if(hdr){
                if(!"video/hevc".equals(inputMime))throw new IOException("Percorso HDR: richiesto HEVC Main10");
                onLog("OK "+HdrHevcInfo.requireMain10(csds.toArray(new byte[0][])));
            }else{
                rejectHdr(video);
                if(("video/hevc".equals(inputMime)&&profile!=0&&profile!=MediaCodecInfo.CodecProfileLevel.HEVCProfileMain)
                        ||("video/avc".equals(inputMime)&&(profile==16||profile==32||profile==64)))
                    throw new IllegalArgumentException("Profilo SDR non supportato: richiesti 8 bit.");
            }
            width=integer(video,MediaFormat.KEY_WIDTH,0);height=integer(video,MediaFormat.KEY_HEIGHT,0);
            if(width<16||height<16||width>3840||height>3840||(long)width*height>3840L*2160||width%2!=0||height%2!=0)
                throw new IllegalArgumentException("Supportati video con dimensioni pari, fino a 3840×2160 (anche verticali)");
            fps=30;
            if(video.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                fps=video.getNumber(MediaFormat.KEY_FRAME_RATE).floatValue();
            } else onLog("AVVISO: fps non dichiarati; uso 30 per x265, conservando i timestamp originali.");
            if(!Float.isFinite(fps)||fps<1||fps>240) throw new IllegalArgumentException("Frequenza video non supportata: "+fps);
            long duration=video.containsKey(MediaFormat.KEY_DURATION)?NumberValues.longExact(video.getNumber(MediaFormat.KEY_DURATION),MediaFormat.KEY_DURATION):0;
            durationUs=duration;
            totalFrames=integer(video,"accurate-framecount",integer(video,"frame-count",-1));
            onLog("FOTOGRAMMI TOTALI: "+totalFrames+" · campi numerici letti tramite Number, compatibile con Integer e Long");
            listener.frames(0,totalFrames);
            int rotation=integer(video,MediaFormat.KEY_ROTATION,0);
            standard=hdr?6:integer(video,MediaFormat.KEY_COLOR_STANDARD,height>576?1:2);
            transfer=hdr?(sourceColor.transfer==16?6:7):integer(video,MediaFormat.KEY_COLOR_TRANSFER,3);
            range=hdr?(sourceColor.fullRange?1:2):integer(video,MediaFormat.KEY_COLOR_RANGE,2);
            onLog("FORMAT dimensioni="+width+"x"+height+" fps_nominali="+fps+" duration_us="+duration+" rotazione="+rotation);
            onLog("AUDIO: "+(audioFormat==null?"assente":"AAC, copia senza ricodifica"));
            muxer=new MediaMuxer(output.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            if(rotation==90||rotation==180||rotation==270) muxer.setOrientationHint(rotation);
            video.setInteger(MediaFormat.KEY_ROTATION,0);
            extractor.selectTrack(selected);
            if(hdr){
                hdrMetadataFile=new File(context.getCacheDir(),"conversion-hdr-metadata.bin");
                HdrTranscoder.Sink sink=new HdrTranscoder.Sink(){
                    public void log(String value){onLog(value);}
                    public void headers(byte[] bytes){onHeaders(bytes);}
                    public void packet(byte[] bytes,long pts,boolean key){onPacket(bytes,pts,key);}
                    public void frame(long pts)throws IOException{
                        inputTimestamps.writeLong(pts);frames++;
                        int percent=durationUs>0?(int)Math.min(89,Math.max(0,pts)*89.0/durationUs):0;
                        listener.timeline(Math.max(0,lastPacket),durationUs);listener.progress(percent,"Codifica HDR: "+frames+" fotogrammi · "+percent+"%");
                    }
                    public void settings(int[] values){reportEncoderSettings(values);}
                };
                applyMemoryProtection();
                new HdrTranscoder(this::isCancelled,sink,sourceColor).run(extractor,video,hdrMetadataFile,width,height,Math.round(fps*1000),crf,preset,this.cpuMode,this.bframes,this.lookahead);
                listener.frames(frames,frames);listener.timeline(durationUs,durationUs);
            }else{
                onLog("BEGIN loadLibrary x265probe · percorso SDR a 8 bit");
                System.loadLibrary("x265probe");
                onLog("OK libreria SDR · CPU ARM64, O3, assembly NEON");
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
                boolean inputEnd=false,outputEnd=false;long lastActivity=activeNow();
                MediaFormat decoded=video;ByteBuffer pixels=null;
                while(!outputEnd) {
                    checkCancel();
                    if(activeNow()-lastActivity>60000) throw new IOException("Decoder bloccato: nessun progresso per 60 secondi");
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
                            lastActivity=activeNow();
                        }
                    }
                    int index=decoder.dequeueOutputBuffer(info,1000);
                    if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        decoded=decoder.getOutputFormat();rejectHdr(decoded);onLog("DECODE FORMAT "+decoded);
                        lastActivity=activeNow();
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
                                        applyMemoryProtection();
                                        handle=nativeOpen(width,height,Math.round(fps*1000),crf,standard,transfer,range,preset,this.cpuMode,Runtime.getRuntime().availableProcessors(),this.bframes,this.lookahead,sourceColor.primaries,sourceColor.transfer,sourceColor.matrix,sourceColor.flags());
                                        int[] temporal=nativeEffectiveSettings(handle);
                                        reportEncoderSettings(temporal);
                                        onLog("PRESTAZIONI: ogni circa 5 secondi vedrai fps di codifica e CPU media normalizzata sui core; il log continua anche a schermata nascosta.\nStato termico Android: 0=regolare, 1=lieve, 2=moderato, 3=elevato, 4=critico, 5=emergenza, 6=spegnimento.\nLa temperatura riportata è della batteria, non della CPU.");
                                        pixels=ByteBuffer.allocateDirect(width*height*3/2);
                                    }
                                    if(crop.width()!=width||crop.height()!=height) throw new IOException("Cambio risoluzione durante il video non supportato");
                                    onLog("BEGIN conversione YUV frame="+frames+" pts_us="+info.presentationTimeUs);
                                    packImage(image,pixels,width,height);
                                    onLog("OK conversione YUV · invio a x265");
                                    inputTimestamps.writeLong(info.presentationTimeUs);
                                    nativeFrame(handle,pixels,info.presentationTimeUs);frames++;
    
                                    int percent=duration>0?(int)Math.min(89,Math.max(0,info.presentationTimeUs)*89.0/duration):0;
                                    listener.timeline(duration>0?Math.min(duration,Math.max(0,lastPacket)):Math.max(0,lastPacket),duration);
                                    listener.progress(percent,"Codifica: "+frames+" fotogrammi · "+percent+"%");
                                }
                            }
                            outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                        } finally {decoder.releaseOutputBuffer(index,false);}
                        lastActivity=activeNow();
                    }
                }
                if(handle==0) throw new IOException("Nessun fotogramma decodificato");
                nativeFinish(handle);
                listener.frames(frames,frames);
                listener.timeline(durationUs>0?durationUs:Math.max(0,lastPacket),durationUs);
                onLog("BEGIN nativeClose");nativeClose(handle);handle=0;onLog("OK nativeClose");
            }
            if(frames!=packets) throw new IOException("Frame e pacchetti diversi: "+frames+"/"+packets);
            if(inputAudio>=0) {listener.progress(91,"Copia audio...");copyAudio(uri,inputAudio);}
            for(MetadataTrack track:metadataTracks)copyMetadataTrack(uri,track);
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
    private void applyMemoryProtection(){
        boolean active=MemoryProtection.required(width,height);
        listener.memoryProtection(active);
        if(!active)return;
        onLog("*** PROTEZIONE MEMORIA 4K ATTIVA ***\nDimensioni effettive: "+width+"×"+height
            +"\nScelte richieste: CPU="+cpuMode+" · B-frame="+bframes+" · lookahead esteso="+lookahead
            +"\n"+MemoryProtection.description());
        cpuMode=0;bframes=true;lookahead=false;
    }
    private void reportEncoderSettings(int[] values){
        if(values==null||values.length!=5)throw new IllegalStateException("Parametri encoder non disponibili");
        listener.encoderSettings(values[0],values[1],values[2],values[3],values[4]);
        onLog("*** PARAMETRI EFFETTIVI ***\nProfondità: "+(hdr?"10 bit HDR":"8 bit SDR")+"\nB-frame: "+values[0]+"\nLookahead: "+values[1]+" fotogrammi\nCU-tree: "+values[2]+"\nAQ mode: "+values[3]+"\nAQ strength: "+(values[4]/1000.0));
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
        onLog("1. Controllo se il pacchetto esiste, indipendentemente dal segno del timestamp.\n2. Copio tutti i pacchetti, anche prima dello zero, senza cambiare i tempi.\n3. Stampo i primi 12 timestamp e il conteggio dei pacchetti negativi.\nLegenda: sample = pacchetto AAC; pts_us = tempo originale in microsecondi; negativo = pacchetto prima dello zero temporale. La fine viene riconosciuta dall’assenza di campioni, non dal timestamp.");
        MediaExtractor e=new MediaExtractor();
        try {
            e.setDataSource(context,uri,null);e.selectTrack(track);
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            AudioPacketCopier.copy(new AudioPacketCopier.Source(){
                public int trackIndex(){return e.getSampleTrackIndex();}
                public long size(){return e.getSampleSize();}
                public long timeUs(){return e.getSampleTime();}
                public boolean encrypted(){return (e.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_ENCRYPTED)!=0;}
                public int read(ByteBuffer buffer){return e.readSampleData(buffer,0);}
                public void advance(){e.advance();}
            },track,(buffer,size,pts)->{
                info.set(0,size,pts,0);muxer.writeSampleData(audioTrack,buffer,info);
            },this::checkCancel,this::onLog);
        } finally {e.release();}
    }
    private void verifyCopiedTrack(Uri source,File target,int inputTrack,int outputTrack)throws Exception {
        MediaExtractor original=new MediaExtractor(),result=new MediaExtractor();
        try{
            original.setDataSource(context,source,null);result.setDataSource(target.getAbsolutePath());original.selectTrack(inputTrack);result.selectTrack(outputTrack);int count=0;
            onLog("*** VERIFICA TRACCIA COPIATA ***\nInput track="+inputTrack+" · output track="+outputTrack+"\nConfronto tutti i pacchetti: contenuto identico e differenza timestamp massima 1000 microsecondi per arrotondamenti MP4.");
            ByteBuffer in=ByteBuffer.allocateDirect(1024*1024),out=ByteBuffer.allocateDirect(1024*1024);
            while(original.getSampleTrackIndex()>=0){
                checkCancel();long n=original.getSampleSize();if(n<0||n>16*1024*1024||result.getSampleSize()!=n)throw new IOException("Dimensione campioni copiati diversa");
                long sourcePts=original.getSampleTime(),savedPts=result.getSampleTime();
                if(count<12)onLog("VERIFY TIMESTAMP TRACCIA · campione="+(count+1)+" · originale_us="+sourcePts+" · salvato_us="+savedPts);
                if(Math.abs(sourcePts-savedPts)>1000)throw new IOException("Timestamp audio/metadati copiati diverso · campione="+(count+1)+" · originale_us="+sourcePts+" · salvato_us="+savedPts);
                if(n>in.capacity()){in=ByteBuffer.allocateDirect((int)n);out=ByteBuffer.allocateDirect((int)n);}
                in.clear();out.clear();int a=original.readSampleData(in,0),b=result.readSampleData(out,0);if(a!=b||a<0)throw new IOException("Campione copiato assente");
                in.position(0);in.limit(a);out.position(0);out.limit(b);if(!in.equals(out))throw new IOException("Contenuto audio/metadati copiati diverso");
                count++;original.advance();result.advance();
            }
            if(result.getSampleTrackIndex()>=0)throw new IOException("Campioni aggiuntivi nella traccia copiata");
            onLog("OK VERIFY traccia copiata="+inputTrack+" samples="+count+" contenuto e timestamp conservati");
        }finally{original.release();result.release();}
    }
    private void verifyGps(String original,String result,String stage)throws IOException {
        onLog("GPS CONVERTITO ("+stage+"): "+OriginalMedia.display(result));
        if(!java.util.Objects.equals(original,result))throw new IOException("GPS DIVERSO O PERSO nel "+stage+": salvataggio annullato");
        if(original!=null)onLog("GPS ESITO: OK · coordinate identiche all’originale · "+stage);
        else onLog("GPS ESITO: NON VERIFICABILE · coordinate non esposte in origine; nessuna posizione inventata. I blocchi descrittivi sono verificati byte per byte.");
    }
    private void sourceMetadata(Uri uri,File target)throws Exception {
        ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(uri,"r");
        if(fd==null)throw new IOException("File originale non disponibile per conservare i metadati");
        try(ParcelFileDescriptor.AutoCloseInputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){
            if(target==null){
                long created=Mp4Metadata.creationTime(in.getChannel(),this::isCancelled);
                fallbackCreationSeconds=InputCreationDate.read(context,uri,created,this::onLog);
                originalDates=VideoDateAudit.source(context,uri,created,this::onLog);
                VideoDateAudit.describe("ORIGINALE",originalDates,this::onLog);
                onLog("OK struttura MP4/MOV compatibile con copia dei metadati");
            }else{
                onLog("*** CONTROLLO MP4 TEMPORANEO PRIMA DEI METADATI ***\nByte temporaneo="+target.length()+" · fotogrammi inviati="+frames+" · pacchetti scritti="+packets);
                Mp4Metadata.copy(in.getChannel(),target,this::isCancelled,this::onLog,fallbackCreationSeconds);
                InputCreationDate.verify(target,fallbackCreationSeconds,this::onLog);
                VideoDateAudit.verify(originalDates,VideoDateAudit.file(target,this::isCancelled),fallbackCreationSeconds,"MP4 TEMPORANEO",this::onLog);
            }
        }
    }
    private void copyMetadataTrack(Uri uri,MetadataTrack track)throws Exception {
        onLog("*** COPIA TRACCIA METADATI TEMPORIZZATI ***");
        MediaExtractor e=new MediaExtractor();
        try {
            e.setDataSource(context,uri,null);e.selectTrack(track.input);ByteBuffer buffer=ByteBuffer.allocateDirect(1024*1024);
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();int count=0;
            while(e.getSampleTrackIndex()>=0){
                checkCancel();long size=e.getSampleSize();if(size<0||size>16*1024*1024)throw new IOException("Dimensione metadati temporizzati non supportata");
                if(size>buffer.capacity())buffer=ByteBuffer.allocateDirect((int)size);
                if((e.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_ENCRYPTED)!=0)throw new IOException("Metadati protetti non copiabili");
                buffer.clear();int n=e.readSampleData(buffer,0);if(n<0)break;
                info.set(0,n,e.getSampleTime(),(e.getSampleFlags()&MediaExtractor.SAMPLE_FLAG_SYNC)!=0?MediaCodec.BUFFER_FLAG_KEY_FRAME:0);
                muxer.writeSampleData(track.output,buffer,info);count++;onLog("OK metadati sample="+count+" pts_us="+info.presentationTimeUs+" bytes="+n);if(!e.advance())break;
            }
            if(count==0)throw new IOException("Traccia metadati vuota: impossibile garantire la conservazione");
        }finally{e.release();}
    }
    private void verify(File file) throws Exception {
        try(DataInputStream expected=new DataInputStream(new BufferedInputStream(new FileInputStream(timestampFile)))) {
            verifyFrames(file,expected);
        }
    }
    private void verifyFrames(File file,DataInputStream expected) throws Exception {
        MediaExtractor e=new MediaExtractor();MediaCodec d=null;boolean running=false;
        try {
            e.setDataSource(file.getAbsolutePath());int track=-1;
            for(int i=0;i<e.getTrackCount();i++) if("video/hevc".equals(e.getTrackFormat(i).getString(MediaFormat.KEY_MIME))) {track=i;break;}
            if(track<0) throw new IOException("Traccia HEVC mancante");
            MediaFormat format=e.getTrackFormat(track);
            if(integer(format,MediaFormat.KEY_WIDTH,0)!=width||integer(format,MediaFormat.KEY_HEIGHT,0)!=height) throw new IOException("Risoluzione MP4 errata");
            if(e.getTrackCount()!=(audioFormat==null?1:2)+metadataTracks.size()) throw new IOException("Conteggio tracce errato");
            ByteBuffer outputCsd=format.getByteBuffer("csd-0");
            if(outputCsd==null)throw new IOException("SPS HEVC mancante nella verifica finale");
            byte[] outputHeaders=new byte[outputCsd.remaining()];outputCsd.duplicate().get(outputHeaders);
            if(!sourceColor.same(ColorVui.read("video/hevc",outputHeaders)))throw new IOException("VUI del file finale diverso dal colore originale");
            onLog("OK COLORE FILE FINALE · SPS/VUI verificato dopo la copia del colr originale");
            e.selectTrack(track);
            if(hdr){
                final int[] verified={0};
                SoftwareHevcDecoder.decode(e,format,width,height,sourceColor,this::isCancelled,this::onLog,null,(pixels,pts,precise)->{
                    checkCancel();long originalPts=expected.readLong();
                    if(Math.abs(pts-originalPts)>1000)throw new IOException("Timestamp MP4 diverso dall’originale: atteso="+originalPts+" ottenuto="+pts);
                    onLog("VERIFY HDR SOFTWARE frame="+(++verified[0])+" pts_us="+pts);
                });
                if(verified[0]!=frames||expected.read()!=-1)throw new IOException("Verifica HDR software fallita: "+verified[0]+"/"+frames);
                onLog("OK VERIFY HDR SOFTWARE decoded="+verified[0]+"/"+frames+" · 10 bit · timestamp originali verificati (tolleranza 1 ms)");
                return;
            }
            else {d=MediaCodec.createDecoderByType("video/hevc");d.configure(format,null,null,0);d.start();}
            running=true;
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();boolean inEnd=false,outEnd=false;int count=0;
            long lastActivity=activeNow(),lastPts=-1;
            while(!outEnd) {
                checkCancel();
                if(activeNow()-lastActivity>60000)throw new IOException("Timeout verifica decoder");
                if(!inEnd) {
                    int i=d.dequeueInputBuffer(1000);
                    if(i>=0) {
                        ByteBuffer b=d.getInputBuffer(i);if(b==null)throw new IOException("Buffer verifica assente");
                        b.clear();int n=e.readSampleData(b,0);
                        if(n<0){d.queueInputBuffer(i,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inEnd=true;}
                        else {d.queueInputBuffer(i,0,n,e.getSampleTime(),0);e.advance();}
                        lastActivity=activeNow();
                    }
                }
                int i=d.dequeueOutputBuffer(info,1000);
                if(i>=0) {
                    if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0) {
                        if(info.presentationTimeUs<=lastPts)throw new IOException("Timestamp verifica non crescente");
                        long originalPts=expected.readLong();
                        if(Math.abs(info.presentationTimeUs-originalPts)>1000)throw new IOException("Timestamp MP4 diverso dall’originale: atteso="+originalPts+" ottenuto="+info.presentationTimeUs);
                        lastPts=info.presentationTimeUs;count++;
                        onLog("VERIFY frame="+count+" pts_us="+lastPts);
                    }
                    outEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;d.releaseOutputBuffer(i,false);
                    lastActivity=activeNow();
                }
            }
            if(count!=frames)throw new IOException("Verifica fallita: "+count+"/"+frames);
            if(expected.read()!=-1)throw new IOException("Timestamp originali avanzati dopo verifica");
            onLog("OK VERIFY timestamp originali verificati (tolleranza 1 ms); B-frame="+bframes+" lookahead="+lookahead);
            onLog("OK VERIFY decoded="+count+"/"+frames+" dimensioni="+width+"x"+height);
        } finally {
            if(d!=null){try{if(running)d.stop();}catch(Exception ignored){}d.release();}
            e.release();
        }
    }
}







