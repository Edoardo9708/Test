package it.edo.codecprobe;

import android.app.*;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.*;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns conversion lifetime; Activity only observes and controls it. */
public final class ConversionService extends Service {
    static final String START="it.edo.codecprobe.START",CANCEL="it.edo.codecprobe.CANCEL";
    private static final Object LOCK=new Object();
    private static final State state=new State();
    private static Runnable observer;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancel=new AtomicBoolean();
    private final ArrayDeque<String> tail=new ArrayDeque<>();
    private PowerManager.WakeLock wake;
    private volatile boolean timedOut;
    private long lastPublish,lastNotification;
    private static final int NOTICE=10;
    private static final String CHANNEL="conversion";
    static final class State {
        boolean hasSession,running;
        int percent,doneFrames,totalFrames=-1;
        long done,total;
        String message="",log="",source="";
        Uri result,input,folder;
        State(){}
        State(State s){hasSession=s.hasSession;running=s.running;percent=s.percent;doneFrames=s.doneFrames;totalFrames=s.totalFrames;done=s.done;total=s.total;message=s.message;log=s.log;source=s.source;result=s.result;input=s.input;folder=s.folder;}
    }
    static State snapshot(){synchronized(LOCK){return new State(state);}}
    static void observe(Runnable listener){synchronized(LOCK){observer=listener;}}
    static void detach(Runnable listener){synchronized(LOCK){if(observer==listener)observer=null;}}
    private void publish(boolean force){
        long now=SystemClock.elapsedRealtime();
        if(!force&&now-lastPublish<250)return;
        lastPublish=now;
        main.post(()->{
            Runnable listener; synchronized(LOCK){listener=observer;}
            if(listener!=null)listener.run();
            State s=snapshot();
            if(!timedOut&&s.running&&(force||SystemClock.elapsedRealtime()-lastNotification>1000)) {
                lastNotification=SystemClock.elapsedRealtime();
                notifyState(s);
            }
        });
    }
    @Override public void onCreate(){
        super.onCreate();
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Conversione video",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Avanzamento e annullamento della conversione");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    private void notifyState(State s){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        getSystemService(NotificationManager.class).notify(NOTICE,notification(s));
    }
    private Notification notification(State s){
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder n=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(s.running?"Conversione video · "+s.percent+"%":"Conversione video")
            .setContentText(s.message).setStyle(new Notification.BigTextStyle().bigText(s.message+"\n"+s.source))
            .setContentIntent(open).setOnlyAlertOnce(true).setOngoing(s.running);
        if(s.running){
            n.setProgress(100,s.percent,false);
            PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,ConversionService.class).setAction(CANCEL),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            n.addAction(new Notification.Action.Builder(null,"Annulla",stop).build());
        }else n.setAutoCancel(true);
        return n.build();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        if(CANCEL.equals(intent.getAction())) {
            cancel.set(true);
            synchronized(LOCK){if(state.running)state.message="Annullamento della chiamata corrente…";}
            publish(true);if(!snapshot().running)stopSelf();return START_NOT_STICKY;
        }
        if(!START.equals(intent.getAction())||snapshot().running)return START_NOT_STICKY;
        Uri input=Uri.parse(intent.getStringExtra("input")),folder=Uri.parse(intent.getStringExtra("folder"));
        String source=intent.getStringExtra("name");
        int crf=intent.getIntExtra("crf",23),preset=intent.getIntExtra("preset",6),cpu=intent.getIntExtra("cpu",1);
        boolean bframes=intent.getBooleanExtra("bframes",true),lookahead=intent.getBooleanExtra("lookahead",true);
        synchronized(LOCK){state.hasSession=true;state.running=true;state.percent=0;state.doneFrames=0;state.totalFrames=-1;state.done=0;state.total=0;state.result=null;state.log="";state.message="Analisi video…";state.source=source;state.input=input;state.folder=folder;}
        try {
            int type=Build.VERSION.SDK_INT>=35?ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING:ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
            startForeground(NOTICE,notification(snapshot()),type);
            PowerManager pm=getSystemService(PowerManager.class);
            wake=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CodecProbe:conversion");
            wake.acquire(6*60*60*1000L);
        }catch(RuntimeException e){
            synchronized(LOCK){state.running=false;state.message="Impossibile avviare il servizio: "+e.getMessage();}
            releaseWake();publish(true);stopSelf();return START_NOT_STICKY;
        }
        cancel.set(false);publish(true);
        worker.execute(()->{
            try {
                VideoConverter converter=new VideoConverter(getApplicationContext(),cancel,new VideoConverter.Listener(){
                    public String cancellationReason(){return timedOut?"Limite Android del servizio mediaProcessing raggiunto":"Annullamento richiesto dall’utente o arresto del servizio";}
                    public void log(String s){
                        synchronized(LOCK){tail.addLast(s);while(tail.size()>14)tail.removeFirst();state.log=String.join("\n",tail);}
                        publish(s.contains("***")||s.contains("SESSION END"));
                    }
                    public void progress(int percent,String text){if(timedOut||cancel.get())return;synchronized(LOCK){state.percent=percent;state.message=text;}publish(false);}
                    public void frames(int done,int total){synchronized(LOCK){state.doneFrames=done;state.totalFrames=total;}publish(false);}
                    public void timeline(long done,long total){synchronized(LOCK){state.done=done;state.total=total;}publish(false);}
                });
                Uri result=converter.convert(input,folder,crf,source,preset,cpu,bframes,lookahead);
                synchronized(LOCK){state.result=result;}
            }catch(Exception e){
                if(!timedOut)synchronized(LOCK){state.message=cancel.get()?"Conversione annullata.":"Conversione fallita: "+e.getMessage()+"\nEsporta il log per i dettagli.";}
            }finally {
                main.post(()->{
                    synchronized(LOCK){state.running=false;}
                    releaseWake();stopForeground(STOP_FOREGROUND_REMOVE);publish(true);
                    if(!timedOut)notifyState(snapshot());
                    stopSelf();
                });
            }
        });
        return START_NOT_STICKY;
    }
    @Override public void onTimeout(int startId,int type){
        timedOut=true;cancel.set(true);
        synchronized(LOCK){state.message="Conversione interrotta: raggiunto il limite Android del servizio in background.";}
        releaseWake();stopForeground(STOP_FOREGROUND_REMOVE);publish(true);stopSelf();
    }
    private void releaseWake(){if(wake!=null&&wake.isHeld())wake.release();}
    @Override public void onDestroy(){cancel.set(true);releaseWake();worker.shutdown();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
