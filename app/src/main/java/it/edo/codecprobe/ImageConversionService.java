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
    static final String START="it.edo.codecprobe.IMAGE_START",CANCEL="it.edo.codecprobe.IMAGE_CANCEL";
    static final String LOG="image-conversion.log";
    private static final Object LOCK=new Object();private static ImageRunState state=new ImageRunState();
    private static final String CHANNEL="image-conversion";private static final int NOTICE=30;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final AtomicBoolean cancelled=new AtomicBoolean();
    private long token;private PowerManager.WakeLock wake;private volatile boolean destroyed;
    static ImageRunState snapshot(){synchronized(LOCK){return new ImageRunState(state);}}
    static void clear(){synchronized(LOCK){if(!state.running){String log=state.log;state=new ImageRunState();state.log=log;}}}
    @Override public void onCreate(){super.onCreate();getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Conversione immagini",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent request,int flags,int startId){
        if(request==null){stopSelf(startId);return START_NOT_STICKY;}
        if(CANCEL.equals(request.getAction())){synchronized(LOCK){if(state.running&&request.getLongExtra("startedAt",-1)==state.startedAt){cancelled.set(true);state.cancelling=true;state.message="Annullamento dopo la fase di codifica in corso…";}}if(token==0)stopSelf(startId);return START_NOT_STICKY;}
        if(!START.equals(request.getAction())||token!=0||snapshot().running)return START_NOT_STICKY;
        ArrayList<String> values=request.getStringArrayListExtra("inputs");ImageOptions options;
        try{options=ImageOptions.from(request);if(options.folder==null||values==null||values.isEmpty()||values.size()>100)throw new IllegalArgumentException();}
        catch(RuntimeException e){stopSelf(startId);return START_NOT_STICKY;}
        token=MediaConversionGate.INSTANCE.acquire();if(token==0){synchronized(LOCK){state.message="È già attiva una conversione: attendi il completamento.";}stopSelf(startId);return START_NOT_STICKY;}
        ArrayList<String> queue=new ArrayList<>(values);cancelled.set(false);
        synchronized(LOCK){state=new ImageRunState();state.running=true;state.total=queue.size();state.startedAt=SystemClock.elapsedRealtime();state.options=options;state.message="Avvio conversione immagini…";}
        try{
            startForeground(NOTICE,notification(),Build.VERSION.SDK_INT>=35?ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING:ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CodecProbe:images");wake.acquire(6*60*60*1000L);
            worker.execute(()->run(queue,options,startId));
        }catch(RuntimeException error){synchronized(LOCK){state.running=false;state.message="Avvio immagini fallito: "+error.getMessage();}release();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(startId);}
        return START_NOT_STICKY;
    }
    private void run(ArrayList<String> queue,ImageOptions options,int startId){
        try{
            write("INIZIO IMMAGINI · v"+BuildConfig.VERSION_NAME+"\n"+options.description()+"\nEXIF/XMP/ICC standard; APP/COM e SEF foto supportato conservati anche nell’archivio interno originale. Funzioni proprietarie Samsung non garantite. Originali invariati.");
            OutputDocuments.names(this,Uri.parse(options.folder));
            File[] stale=getCacheDir().listFiles((d,n)->(n.startsWith("image-source-")||n.startsWith("image-heic-"))&&n.endsWith(".tmp"));if(stale!=null)for(File f:stale)f.delete();
            for(int i=0;i<queue.size();i++){
                if(cancelled.get())break;Uri uri=Uri.parse(queue.get(i));String name=ImageFiles.name(this,uri);
                synchronized(LOCK){state.current=i+1;state.name=name;state.message="Preparazione JPEG";}notifyProgress();
                ImageRunState.Result result;
                try{result=ImageConverter.convert(this,uri,name,options,cancelled::get,this::stage);}
                catch(Exception e){result=new ImageRunState.Result(name,0,0,null,e.getMessage()==null?e.getClass().getSimpleName():e.getMessage(),cancelled.get()?ImageRunState.Outcome.CANCELLED:ImageRunState.Outcome.FAILED,android.util.Log.getStackTraceString(e));}
                synchronized(LOCK){state.results.add(result);}write(name+" · "+result.outcome+" · "+result.message+"\n"+result.details);notifyProgress();
            }
            synchronized(LOCK){state.message=cancelled.get()?"Coda immagini interrotta. I file già salvati restano disponibili.":"Coda immagini terminata.";}
        }catch(Exception error){synchronized(LOCK){state.message="Conversione immagini interrotta: "+error.getMessage();}try{write(android.util.Log.getStackTraceString(error));}catch(IOException ignored){}}
        finally{synchronized(LOCK){state.running=false;state.cancelling=false;}release();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(startId);}
    }
    private void stage(String text){synchronized(LOCK){if(!state.cancelling)state.message=text;}notifyProgress();}
    private void write(String text)throws IOException{
        File file=new File(getFilesDir(),LOG);if(file.length()>2*1024*1024)try(OutputStream out=new FileOutputStream(file,false)){out.write("Log immagini precedente ruotato al limite di 2 MiB.\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        try(OutputStream out=new FileOutputStream(file,true)){out.write((text+"\n\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        synchronized(LOCK){state.log+=text+"\n\n";if(state.log.length()>24000)state.log=state.log.substring(state.log.length()-24000);}
    }
    private Notification notification(){ImageRunState run=snapshot();Intent open=new Intent(this,ImageActivity.class);PendingIntent page=PendingIntent.getActivities(this,NOTICE,new Intent[]{new Intent(this,MainActivity.class),open},PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent cancel=new Intent(this,ImageConversionService.class).setAction(CANCEL).putExtra("startedAt",run.startedAt);PendingIntent stop=PendingIntent.getService(this,NOTICE,cancel,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_upload).setContentTitle("Immagini · "+run.current+" / "+run.total).setContentText(run.message).setContentIntent(page).setOngoing(true).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(null,"Interrompi",stop).build()).build();}
    private void notifyProgress(){if(!destroyed)getSystemService(NotificationManager.class).notify(NOTICE,notification());}
    private synchronized void release(){if(wake!=null&&wake.isHeld())wake.release();wake=null;if(token!=0){MediaConversionGate.INSTANCE.release(token);token=0;}}
    @Override public void onTimeout(int startId,int type){cancelled.set(true);stage("Tempo massimo Android raggiunto: annullamento dopo la codifica in corso…");if(wake!=null&&wake.isHeld())wake.release();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(startId);}
    @Override public void onDestroy(){destroyed=true;cancelled.set(true);worker.shutdown();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
