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

/** Foreground photo queue. Owns engine lease, wake lock and lifetime of every temporary output. */
public final class ImageConversionService extends Service {
    static final String START="it.edo.codecprobe.IMAGE_START",CANCEL="it.edo.codecprobe.IMAGE_CANCEL",PAUSE="it.edo.codecprobe.IMAGE_PAUSE",RESUME="it.edo.codecprobe.IMAGE_RESUME";
    static final String LOG="image-conversion.log";
    private static final Object LOCK=new Object();private static ImageRunState state=new ImageRunState();
    private static final String CHANNEL="image-conversion";private static final int NOTICE=30;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final AtomicBoolean cancelled=new AtomicBoolean();
    private final ConversionControl control=new ConversionControl(cancelled,SystemClock::elapsedRealtime,this::pauseChanged);
    private long token;private PowerManager.WakeLock wake;private volatile boolean destroyed;private volatile int latestStartId;
    static ImageRunState snapshot(){synchronized(LOCK){return new ImageRunState(state);}}
    static void clear(){synchronized(LOCK){if(!state.running){String log=state.log;state=new ImageRunState();state.log=log;}}}
    static void clearLog(){synchronized(LOCK){if(!state.running)state.log="";}}
    @Override public void onCreate(){super.onCreate();getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Conversione immagini",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent request,int flags,int startId){
        latestStartId=startId;
        if(request==null){stopSelf(startId);return START_NOT_STICKY;}
        if(PAUSE.equals(request.getAction())||RESUME.equals(request.getAction())){
            boolean pause=PAUSE.equals(request.getAction());
            synchronized(LOCK){if(!state.running||state.cancelling||request.getLongExtra("startedAt",-1)!=state.startedAt){if(token==0)stopSelf(startId);return START_NOT_STICKY;}state.pauseRequested=pause;}
            control.pause(pause);notifyProgress();return START_NOT_STICKY;
        }
        if(CANCEL.equals(request.getAction())){
            boolean accepted;
            synchronized(LOCK){accepted=state.running&&request.getLongExtra("startedAt",-1)==state.startedAt;if(accepted){state.cancelling=true;state.pauseRequested=false;state.message="Annullamento dopo la fase di codifica in corso…";}}
            if(accepted){control.cancel();notifyProgress();}if(token==0)stopSelf(startId);return START_NOT_STICKY;
        }
        if(!START.equals(request.getAction())||token!=0||snapshot().running)return START_NOT_STICKY;
        String queueId=request.getStringExtra("queueId");
        ArrayList<String> values=request.getStringArrayListExtra("inputs");ImageOptions options;
        final ArrayList<String> legacyFolders;
        try{
            options=ImageOptions.from(request);
            if(queueId!=null){if(!ImageQueueStore.file(this,queueId).isFile())throw new IOException("Coda non disponibile");legacyFolders=null;}
            else{if(values==null||values.isEmpty()||values.size()>100)throw new IllegalArgumentException();legacyFolders=QueueDestinations.read(request,values.size(),options.folder);}
        }catch(IOException|RuntimeException e){ImageQueueStore.discard(this,queueId);stopSelf(startId);return START_NOT_STICKY;}
        token=MediaConversionGate.INSTANCE.acquire();if(token==0){ImageQueueStore.discard(this,queueId);synchronized(LOCK){state.message="È già attiva una conversione: attendi il completamento.";}stopSelf(startId);return START_NOT_STICKY;}
        ArrayList<String> queue=values==null?null:new ArrayList<>(values);cancelled.set(false);control.pause(false);
        synchronized(LOCK){state=new ImageRunState();state.running=true;state.total=queue==null?0:queue.size();state.startedAt=SystemClock.elapsedRealtime();state.options=options;state.message="Avvio conversione immagini…";}
        try{
            startForeground(NOTICE,notification(),Build.VERSION.SDK_INT>=35?ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING:ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            acquireWake();
            worker.execute(()->run(queueId,queue,legacyFolders,options,startId));
        }catch(RuntimeException error){ImageQueueStore.discard(this,queueId);synchronized(LOCK){state.running=false;state.endedAt=SystemClock.elapsedRealtime();state.message="Avvio immagini fallito: "+error.getMessage();}release();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(latestStartId);}
        return START_NOT_STICKY;
    }
    private void run(String queueId,ArrayList<String> legacyQueue,ArrayList<String> legacyFolders,ImageOptions options,int startId){
        try(ImageProgressJournal journal=new ImageProgressJournal(this)){
            ImageQueueStore.Queue job;
            if(queueId!=null)job=ImageQueueStore.read(this,queueId);
            else{job=new ImageQueueStore.Queue();job.inputs.addAll(legacyQueue);job.folders.addAll(legacyFolders);for(int i=0;i<legacyQueue.size();i++)job.names.add("Foto "+(i+1));}
            ArrayList<String> queue=job.inputs,folders=job.folders;
            synchronized(LOCK){state.total=queue.size();state.queueNames=Collections.unmodifiableList(new ArrayList<>(job.names));}
            // New run log: never rotate away an error halfway through a large batch.
            ImageLogStore.reset(this);
            write("INIZIO IMMAGINI · v"+BuildConfig.VERSION_NAME+"\n"+options.description()+"\nEXIF/XMP/ICC standard; APP/COM e SEF foto supportato conservati anche nell’archivio interno originale. Funzioni proprietarie Samsung non garantite. Originali invariati.");
            for(String folder:new HashSet<>(folders))OutputDocuments.names(this,Uri.parse(folder));
            File[] stale=getCacheDir().listFiles((d,n)->(n.startsWith("image-source-")||n.startsWith("image-heic-"))&&n.endsWith(".tmp"));if(stale!=null)for(File f:stale)f.delete();
            for(int i=0;i<queue.size();i++){
                if(control.checkpoint())break;Uri uri=Uri.parse(queue.get(i));String name=ImageFiles.name(this,uri);
                synchronized(LOCK){state.current=i+1;state.name=name;state.message="Preparazione JPEG";}notifyProgress();
                ImageRunState.Result result;
                ImageOptions fileOptions=options.withFolder(folders.get(i));
                try{
                    stage("Controllo registro e originale");result=journal.reusable(this,queue.get(i),name,fileOptions,cancelled::get);
                    if(result==null)result=ImageConverter.convert(this,uri,name,fileOptions,cancelled::get,this::stage);
                }
                catch(Exception e){result=new ImageRunState.Result(name,0,0,null,e.getMessage()==null?e.getClass().getSimpleName():e.getMessage(),cancelled.get()?ImageRunState.Outcome.CANCELLED:ImageRunState.Outcome.FAILED,android.util.Log.getStackTraceString(e));}
                synchronized(LOCK){state.add(result);}
                journal.record(queue.get(i),folders.get(i),fileOptions,result);
                if(result.outcome==ImageRunState.Outcome.FAILED)ImageFailureStore.append(this,state.startedAt,i+1,queue.get(i),folders.get(i),options,result);
                // The local result still owns the full details; only state.add keeps a bounded preview.
                write(name+" · "+result.outcome+" · "+result.message+"\n"+result.details);
                notifyProgress();
            }
            synchronized(LOCK){state.message=cancelled.get()?"Coda immagini interrotta. I file già salvati restano disponibili.":"Coda immagini terminata.";}
        }catch(Exception error){synchronized(LOCK){state.message="Conversione immagini interrotta: "+error.getMessage();}try{write(android.util.Log.getStackTraceString(error));}catch(IOException ignored){}}
        finally{ImageQueueStore.discard(this,queueId);synchronized(LOCK){state.running=false;state.cancelling=false;state.paused=false;state.pauseRequested=false;state.endedAt=SystemClock.elapsedRealtime();}release();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(latestStartId);}
    }
    private void stage(String text){synchronized(LOCK){if(!state.cancelling)state.message=text;}notifyProgress();}
    private void write(String text)throws IOException{
        ImageLogStore.append(this,LOG,text+"\n\n");
        synchronized(LOCK){state.log+=text+"\n\n";if(state.log.length()>24000)state.log=state.log.substring(state.log.length()-24000);}
    }
    static String displayMessage(ImageRunState run){
        if(run.cancelling)return "Annullamento della coda immagini…";
        if(run.paused)return "Coda immagini in pausa · premi Riprendi per continuare";
        if(run.pauseRequested)return "Pausa richiesta · completo la foto in corso…";
        return run.message;
    }
    private void pauseChanged(boolean paused){
        if(paused)releaseWake();else if(!destroyed&&!cancelled.get()&&!control.pauseRequested())acquireWake();
        synchronized(LOCK){state.paused=paused;}
        try{write(paused?"PAUSA IMMAGINI · foto completate: "+snapshot().completed+" · nessuna nuova foto avviata":"FINE PAUSA IMMAGINI · "+(cancelled.get()?"annullamento":"ripresa della stessa coda"));}
        catch(IOException error){throw new UncheckedIOException(error);}
        notifyProgress();
    }
    private Notification notification(){ImageRunState run=snapshot();Intent open=new Intent(this,ImageActivity.class);PendingIntent page=PendingIntent.getActivities(this,NOTICE,new Intent[]{new Intent(this,MainActivity.class),open},PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent cancel=new Intent(this,ImageConversionService.class).setAction(CANCEL).putExtra("startedAt",run.startedAt);PendingIntent stop=PendingIntent.getService(this,NOTICE,cancel,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder notice=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_upload).setContentTitle(run.paused?"Immagini in pausa · "+run.completed+" / "+run.total:"Immagini · "+run.current+" / "+run.total).setContentText(displayMessage(run)).setContentIntent(page).setOngoing(true).setOnlyAlertOnce(true);
        if(run.running&&!run.cancelling){boolean resume=run.pauseRequested||run.paused;Intent command=new Intent(this,ImageConversionService.class).setAction(resume?RESUME:PAUSE).putExtra("startedAt",run.startedAt);PendingIntent action=PendingIntent.getService(this,resume?32:31,command,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);notice.addAction(new Notification.Action.Builder(null,resume?"Riprendi":"Pausa",action).build());}
        return notice.addAction(new Notification.Action.Builder(null,"Interrompi",stop).build()).build();}
    private synchronized void acquireWake(){if(wake==null)wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CodecProbe:images");if(!wake.isHeld())wake.acquire(6*60*60*1000L);}
    private void notifyProgress(){if(!destroyed)getSystemService(NotificationManager.class).notify(NOTICE,notification());}
    private synchronized void releaseWake(){if(wake!=null&&wake.isHeld())wake.release();wake=null;}
    private synchronized void release(){releaseWake();if(token!=0){MediaConversionGate.INSTANCE.release(token);token=0;}}
    @Override public void onTimeout(int startId,int type){control.cancel();synchronized(LOCK){state.cancelling=true;state.pauseRequested=false;}stage("Tempo massimo Android raggiunto: annullamento dopo la codifica in corso…");releaseWake();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(startId);}
    @Override public void onDestroy(){destroyed=true;control.cancel();worker.shutdown();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
