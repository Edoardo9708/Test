package it.edo.codecprobe;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Audio worker, notification and output lifetime. No dependency on the video session model. */
public final class AudioConversionService extends Service {
    static final String START="it.edo.codecprobe.AUDIO_START",CANCEL="it.edo.codecprobe.AUDIO_CANCEL";
    private static final Object LOCK=new Object();
    private static AudioRunState state=new AudioRunState();
    private static final int NOTICE=20;
    private static final String CHANNEL="audio-conversion";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancelled=new AtomicBoolean();
    private final Handler main=new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wake;
    private long token,lastNotify,lastProgress,lastProgressLog;
    private volatile boolean destroyed;
    static AudioRunState snapshot(){synchronized(LOCK){return new AudioRunState(state);}}
    static void clear(){synchronized(LOCK){if(!state.running)state=new AudioRunState();}}
    @Override public void onCreate(){
        super.onCreate();getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Conversione audio",NotificationManager.IMPORTANCE_LOW));
    }
    @Override public int onStartCommand(Intent request,int flags,int startId){
        if(request==null){stopSelf(startId);return START_NOT_STICKY;}
        if(CANCEL.equals(request.getAction())){
            synchronized(LOCK){if(state.running&&request.getLongExtra("startedAt",-1)==state.startedAt){cancelled.set(true);state.cancelling=true;state.message="Annullamento audio e pulizia temporanei…";}}
            if(token==0)stopSelf(startId);return START_NOT_STICKY;
        }
        if(!START.equals(request.getAction())||snapshot().running||token!=0)return START_NOT_STICKY;
        ArrayList<String> inputs=request.getStringArrayListExtra("inputs"),names=request.getStringArrayListExtra("names");
        String folder=request.getStringExtra("folder");int bitrate=request.getIntExtra("bitrate",128);
        if(inputs==null||names==null||inputs.isEmpty()||inputs.size()>500||inputs.size()!=names.size()||folder==null||bitrate<64||bitrate>256){stopSelf(startId);return START_NOT_STICKY;}
        token=MediaConversionGate.INSTANCE.acquire();
        if(token==0){synchronized(LOCK){state.message="È già attiva una conversione audio o video: attendi la fine prima di avviare l’audio.";}stopSelf(startId);return START_NOT_STICKY;}
        final ArrayList<String> queue=new ArrayList<>(inputs),labels=new ArrayList<>(names);
        final Uri destination=Uri.parse(folder);
        synchronized(LOCK){state=new AudioRunState();state.running=true;state.total=queue.size();state.bitrate=bitrate;state.startedAt=SystemClock.elapsedRealtime();state.message="Avvio motore audio…";}
        cancelled.set(false);destroyed=false;
        try{
            startForeground(NOTICE,notification(),Build.VERSION.SDK_INT>=35?ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING:ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CodecProbe:audio");wake.acquire(6*60*60*1000L);
        }catch(RuntimeException error){
            synchronized(LOCK){state.running=false;state.endedAt=SystemClock.elapsedRealtime();state.message="Avvio audio fallito: "+error.getMessage();}
            releaseWake();stopForeground(STOP_FOREGROUND_REMOVE);MediaConversionGate.INSTANCE.release(token);token=0;stopSelf(startId);return START_NOT_STICKY;
        }
        worker.execute(()->run(queue,labels,destination,bitrate,startId));return START_NOT_STICKY;
    }
    private void run(ArrayList<String> inputs,ArrayList<String> names,Uri destination,int bitrate,int startId){
        try(AudioLog log=new AudioLog(this)){
            try{
            write(log,"*** INIZIO SESSIONE AUDIO · v"+BuildConfig.VERSION_NAME+" ***\n1. Decodifica FLAC con libFLAC\n2. Codifica CPU con libopus + libopusenc\n3. Verifica decodifica completa del file Opus\n4. Salvataggio nella cartella scelta\nBUILD PRELIMINARE: tag, copertine e LRC non vengono copiati. Originali invariati. Nessun ripristino audio dopo chiusura del processo.");
            // A previous process may have died with a private temporary file. No user output is removed.
            File[] stale=getCacheDir().listFiles((dir,name)->name.startsWith("audio-opus-")&&name.endsWith(".tmp"));
            if(stale!=null)for(File file:stale)if(!file.delete())write(log,"AVVISO: temporaneo precedente non eliminato: "+file.getName());
            AudioCodec.load();write(log,"OK: loadLibrary audioopus");
            for(int n=0;n<inputs.size()&&!cancelled.get();n++){
                final int number=n+1;final String name=names.get(n);final Uri input=Uri.parse(inputs.get(n));
                synchronized(LOCK){state.current=number;state.name=name;state.percent=0;state.doneSamples=0;state.totalSamples=0;state.channels=0;state.rate=0;state.message="Analisi FLAC "+number+" / "+inputs.size();}
                lastProgress=0;lastProgressLog=0;
                write(log,"*** INIZIO BRANO "+number+" / "+inputs.size()+" ***\nNome: "+name+"\nInput: "+input+"\nDestinazione: "+destination);
                File temp=null;
                long before=MediaFileSizes.read(this,input);
                try{
                    temp=File.createTempFile("audio-opus-",".tmp",getCacheDir());
                    long duration;
                    try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(input,"r")){
                        if(fd==null)throw new IOException("File input non leggibile");
                        duration=AudioCodec.encode(fd.getFd(),temp.getAbsolutePath(),bitrate*1000,new AudioCodec.Listener(){
                            public boolean progress(long samples,long total,int rate,int channels){
                                long now=SystemClock.elapsedRealtime();
                                if(now-lastProgress>=250||samples==0||samples==total){
                                    synchronized(LOCK){state.doneSamples=samples;state.totalSamples=total;state.rate=rate;state.channels=channels;state.percent=total>0?(int)Math.min(100,samples*100/total):0;state.message="Codifica FLAC → Opus VBR";}
                                    lastProgress=now;notifyProgress();
                                }
                                if(now-lastProgressLog>=5000){writeUnchecked(log,"AVANZAMENTO: campioni="+samples+"/"+total+" · secondi="+String.format(Locale.ROOT,"%.1f",samples/(double)rate));lastProgressLog=now;}
                                return !cancelled.get();
                            }
                            public void log(String text){writeUnchecked(log,text);}
                        });
                    }
                    checkCancelled();message("Verifica decodifica Opus…");write(log,"*** 3. VERIFICA OUTPUT ***\nDecodifica completa con il decoder Android. Controllo canali e durata PCM, tolleranza 80 ms per pre-skip/fine stream.");
                    AudioVerifier.verify(temp,duration,snapshot().channels,cancelled::get);
                    checkCancelled();write(log,"OK: file Opus decodificato · durata attesa="+duration+" µs");
                    message("Salvataggio del file Opus…");write(log,"*** 4. SALVATAGGIO ***\nIn caso di omonimia creo un nome con suffisso numerico. Non sovrascrivo file esistenti.");
                    Uri result=AudioOutput.save(this,destination,temp,name,cancelled::get);
                    synchronized(LOCK){state.results.add(new AudioRunState.Result(name,result.toString(),before,temp.length(),null));state.percent=100;}
                    write(log,"*** BRANO RIUSCITO ***\n"+name+"\n"+AudioRunState.sizes(before,temp.length())+"\nFile salvato: "+result);
                }catch(Exception error){
                    String details=failure(number,name,snapshot().message,error);
                    synchronized(LOCK){state.results.add(new AudioRunState.Result(name,null,before,0,details));}
                    write(log,"*** "+(cancelled.get()?"BRANO INTERROTTO":"ERRORE BRANO · CONTINUO LA CODA")+" ***\n"+details);
                }finally{if(temp!=null&&temp.exists()&&!temp.delete())write(log,"AVVISO: impossibile eliminare temporaneo "+temp.getName());}
            }
            synchronized(LOCK){state.message=cancelled.get()?"Coda audio interrotta · risultati completati conservati":"Coda audio terminata";}
            write(log,"*** FINE SESSIONE AUDIO ***\n"+snapshot().message);
            }catch(Exception|LinkageError error){
                String details=failure(0,"Sessione audio",snapshot().message,error);
                synchronized(LOCK){state.message="Errore sessione audio: "+error.getMessage();}
                write(log,"*** ERRORE SESSIONE AUDIO ***\n"+details);
            }
        }catch(Exception error){synchronized(LOCK){state.message="Errore sessione audio: "+error.getMessage();state.log=state.log+"\n"+failure(0,"Sessione audio",state.message,error);}}
        finally{
            // JNI has returned and every per-file try/finally completed before releasing the shared lease.
            main.post(()->{
                synchronized(LOCK){state.running=false;state.cancelling=false;state.endedAt=SystemClock.elapsedRealtime();}
                releaseWake();stopForeground(STOP_FOREGROUND_REMOVE);
                MediaConversionGate.INSTANCE.release(token);token=0;stopSelf(startId);
            });
        }
    }
    private void message(String text){synchronized(LOCK){state.message=text;}notifyProgress();}
    private void checkCancelled()throws IOException{if(cancelled.get())throw new IOException("Annullato");}
    private void write(AudioLog log,String text)throws IOException{log.write(text);synchronized(LOCK){state.log=log.tail();}}
    private void writeUnchecked(AudioLog log,String text){try{write(log,text);}catch(IOException error){throw new UncheckedIOException(error);}}
    private static String failure(int number,String name,String step,Throwable error){
        StringWriter stack=new StringWriter();error.printStackTrace(new PrintWriter(stack));
        return "ERRORE AUDIO · v"+BuildConfig.VERSION_NAME+"\nBrano "+number+" · "+name+"\n\nMESSAGGIO\n"+error.getMessage()+"\n\nULTIMO STATO\n"+step+"\n\nDETTAGLI TECNICI\n"+stack;
    }
    private Notification notification(){
        AudioRunState s=snapshot();Intent open=new Intent(this,AudioActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content=PendingIntent.getActivity(this,20,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_notification_audio).setContentTitle("Audio · "+s.current+" / "+s.total)
            .setContentText(s.message).setSubText(s.name).setContentIntent(content).setOngoing(s.running).setOnlyAlertOnce(true).setProgress(100,s.percent,s.totalSamples==0).build();
    }
    private void notifyProgress(){
        long now=SystemClock.elapsedRealtime();if(now-lastNotify<1000||destroyed||cancelled.get())return;lastNotify=now;
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
        getSystemService(NotificationManager.class).notify(NOTICE,notification());
    }
    private synchronized void releaseWake(){if(wake!=null&&wake.isHeld())wake.release();}
    @Override public void onTimeout(int startId,int type){cancelled.set(true);message("Limite Android raggiunto: interruzione audio e pulizia…");releaseWake();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
    @Override public void onDestroy(){destroyed=true;cancelled.set(true);releaseWake();worker.shutdown();super.onDestroy();}
    @Override public IBinder onBind(Intent request){return null;}
}
