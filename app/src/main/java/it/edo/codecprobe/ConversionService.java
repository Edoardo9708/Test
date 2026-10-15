package it.edo.codecprobe;

import android.app.*;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns conversion lifetime; Activity only observes and controls it. */
public final class ConversionService extends Service {
    static final String START="it.edo.codecprobe.START",CANCEL="it.edo.codecprobe.CANCEL";
    private static final Object LOCK=new Object();
    private static final State state=new State();
    private static Runnable observer;
    static volatile boolean uiVisible;
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
        boolean hasSession,running,originMode,applyAllDestinations,allOrigin=true;
        String allFolder="";
        boolean[] individualOrigins=new boolean[0];
        ArrayList<String> individualFolders=new ArrayList<>();
        int percent,filePercent,batchIndex,batchTotal,succeeded,failed,doneFrames,totalFrames=-1;
        ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>(),folders=new ArrayList<>();
        ArrayList<Uri> results=new ArrayList<>();
        int bframeCount=-1,lookaheadCount=-1,cuTree=-1,aqMode=-1,aqStrength=-1;
        long done,total;
        String message="",log="",source="",sizeReport="";
        Uri result,input,folder;
        State(){}
        State(State s){sizeReport=s.sizeReport;applyAllDestinations=s.applyAllDestinations;allOrigin=s.allOrigin;allFolder=s.allFolder;individualOrigins=s.individualOrigins.clone();individualFolders=new ArrayList<>(s.individualFolders);originMode=s.originMode;inputs=new ArrayList<>(s.inputs);names=new ArrayList<>(s.names);folders=new ArrayList<>(s.folders);results=new ArrayList<>(s.results);filePercent=s.filePercent;batchIndex=s.batchIndex;batchTotal=s.batchTotal;succeeded=s.succeeded;failed=s.failed;hasSession=s.hasSession;running=s.running;bframeCount=s.bframeCount;lookaheadCount=s.lookaheadCount;cuTree=s.cuTree;aqMode=s.aqMode;aqStrength=s.aqStrength;percent=s.percent;doneFrames=s.doneFrames;totalFrames=s.totalFrames;done=s.done;total=s.total;message=s.message;log=s.log;source=s.source;result=s.result;input=s.input;folder=s.folder;}
    }
    static boolean clearLogsIfIdle(Runnable clear){synchronized(LOCK){if(state.running)return false;clear.run();state.log="";return true;}}
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
        Notification.Builder n=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_notification_video)
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
        ArrayList<String> inputs=intent.getStringArrayListExtra("inputs"),names=intent.getStringArrayListExtra("names"),folders=intent.getStringArrayListExtra("folders");
        if(inputs==null||names==null||folders==null||inputs.isEmpty()||inputs.size()>BatchProgress.MAX_FILES||names.size()!=inputs.size()||folders.size()!=inputs.size()){
            stopSelf();return START_NOT_STICKY;
        }
        final ArrayList<String> queueInputs=new ArrayList<>(inputs),queueNames=new ArrayList<>(names),queueFolders=new ArrayList<>(folders);
        final AppSettings naming=new AppSettings(this);
        final ArrayList<CompressionSummary.Item> sizeResults=new ArrayList<>();
        for(int n=0;n<queueInputs.size();n++)sizeResults.add(new CompressionSummary.Item(n+1,queueNames.get(n),CompressionSummary.Status.SKIPPED,-1,-1));
        final ArrayList<String> destinationNotes=intent.getStringArrayListExtra("destinationNotes");
        Uri input=Uri.parse(inputs.get(0)),folder=Uri.parse(folders.get(0));String source=names.get(0);
        int crf=intent.getIntExtra("crf",23),preset=intent.getIntExtra("preset",6),cpu=intent.getIntExtra("cpu",1);
        boolean bframes=intent.getBooleanExtra("bframes",true),lookahead=intent.getBooleanExtra("lookahead",true);
        synchronized(LOCK){state.individualOrigins=intent.getBooleanArrayExtra("individualOrigins");if(state.individualOrigins==null)state.individualOrigins=new boolean[queueInputs.size()];
            ArrayList<String> requestedChoices=intent.getStringArrayListExtra("individualFolders");state.individualFolders=requestedChoices==null?new ArrayList<>(queueFolders):new ArrayList<>(requestedChoices);
            state.applyAllDestinations=intent.getBooleanExtra("applyAllDestinations",false);state.allOrigin=intent.getBooleanExtra("allOrigin",true);state.allFolder=intent.getStringExtra("allFolder");
            state.originMode=intent.getBooleanExtra("originMode",false);state.inputs=queueInputs;state.names=queueNames;state.folders=queueFolders;state.results.clear();state.sizeReport="";state.batchIndex=0;state.batchTotal=queueInputs.size();state.succeeded=0;state.failed=0;state.filePercent=0;state.hasSession=true;state.running=true;state.bframeCount=-1;state.lookaheadCount=-1;state.cuTree=-1;state.aqMode=-1;state.aqStrength=-1;state.percent=0;state.doneFrames=0;state.totalFrames=-1;state.done=0;state.total=0;state.result=null;state.log="";state.message="Analisi video…";state.source=source;state.input=input;state.folder=folder;}
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
        cancel.set(false);timedOut=false;tail.clear();publish(true);
        worker.execute(()->{
            try(BufferedWriter batchLog=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(batchLogFile(this)),java.nio.charset.StandardCharsets.UTF_8),65536)) {
                writeBatch(batchLog,"*** INIZIO CODA · v"+BuildConfig.VERSION_NAME+" ***\nVideo selezionati: "+queueInputs.size()+"\n1. Analisi del file e metadati\n2. Codifica x265 CRF e verifica completa\n3. Salvataggio in un nuovo file; poi video successivo\nParametri: CRF="+crf+" preset="+AppSettings.PRESETS[Math.max(0,Math.min(8,preset))]+" CPU="+cpu+" B-frame="+bframes+" Lookahead="+lookahead+"\nLa percentuale totale assegna lo stesso peso a ogni video; non è una stima del tempo.");
                for(int index=0;index<queueInputs.size()&&!cancel.get();index++){
                    final int current=index;
                    synchronized(LOCK){state.batchIndex=index+1;state.filePercent=0;state.percent=BatchProgress.percent(index,0,queueInputs.size());state.doneFrames=0;state.totalFrames=-1;state.done=0;state.total=0;state.source=queueNames.get(index);state.input=Uri.parse(queueInputs.get(index));state.folder=Uri.parse(queueFolders.get(index));state.message="Analisi video "+(index+1)+" / "+queueInputs.size();state.bframeCount=-1;}
                    writeBatch(batchLog,"\n*** INIZIO VIDEO "+(index+1)+" / "+queueInputs.size()+" ***\nNome: "+queueNames.get(index)+"\nInput: "+queueInputs.get(index)+"\nCartella: "+queueFolders.get(index)+(destinationNotes!=null&&destinationNotes.size()==queueInputs.size()?"\nScelta destinazione: "+destinationNotes.get(index):""));publish(true);
                    try{
                        long originalBytes=readFileSize(Uri.parse(queueInputs.get(index)));
                        writeBatch(batchLog,"CONFRONTO DIMENSIONI · input_byte="+originalBytes+" · "+CompressionSummary.size(originalBytes));
                        requireWritableFolder(Uri.parse(queueFolders.get(index)));
                        VideoConverter converter=new VideoConverter(getApplicationContext(),cancel,new VideoConverter.Listener(){
                            public void encoderSettings(int b,int l,int tree,int aq,int strength){synchronized(LOCK){state.bframeCount=b;state.lookaheadCount=l;state.cuTree=tree;state.aqMode=aq;state.aqStrength=strength;}publish(true);}
                            public String runtimeState(){return "UI_visibile="+uiVisible+" wake_lock_attivo="+(wake!=null&&wake.isHeld());}
                            public String cancellationReason(){return timedOut?"Limite Android del servizio mediaProcessing raggiunto":"Annullamento dell’intera coda richiesto dall’utente o arresto del servizio";}
                            public void log(String line){writeBatch(batchLog,line);}
                            public void progress(int percent,String text){if(timedOut||cancel.get())return;synchronized(LOCK){state.filePercent=percent;state.percent=BatchProgress.percent(current,percent,queueInputs.size());state.message=text;}publish(false);}
                            public void frames(int done,int total){synchronized(LOCK){state.doneFrames=done;state.totalFrames=total;}publish(false);}
                            public void timeline(long done,long total){synchronized(LOCK){state.done=done;state.total=total;}publish(false);}
                        },naming);
                        Uri result=converter.convert(Uri.parse(queueInputs.get(index)),Uri.parse(queueFolders.get(index)),crf,queueNames.get(index),preset,cpu,bframes,lookahead);
                        long outputBytes=readFileSize(result);
                        sizeResults.set(index,new CompressionSummary.Item(index+1,queueNames.get(index),CompressionSummary.Status.COMPLETE,originalBytes,outputBytes));
                        synchronized(LOCK){state.result=result;state.results.add(result);state.succeeded++;}
                        writeBatch(batchLog,"*** RISULTATO DIMENSIONI ***\n"+CompressionSummary.row(sizeResults.get(index))+"\nInput byte: "+originalBytes+"\nOutput byte: "+outputBytes);
                        writeBatch(batchLog,"*** VIDEO COMPLETATO ***\nOutput: "+result);
                    }catch(Exception e){
                        if(cancel.get()){sizeResults.set(index,new CompressionSummary.Item(index+1,queueNames.get(index),CompressionSummary.Status.CANCELLED,-1,-1));writeBatch(batchLog,"*** VIDEO INTERROTTO ***\n"+e);break;}
                        sizeResults.set(index,new CompressionSummary.Item(index+1,queueNames.get(index),CompressionSummary.Status.FAILED,-1,-1));
                        synchronized(LOCK){state.failed++;}
                        writeBatch(batchLog,"*** VIDEO FALLITO · CONTINUO CON IL SUCCESSIVO ***\nNome: "+queueNames.get(index)+"\nErrore: "+e);
                    }
                    synchronized(LOCK){state.percent=BatchProgress.percent(index+1,0,queueInputs.size());state.filePercent=100;}batchLog.flush();publish(true);
                }
                synchronized(LOCK){int remaining=state.batchTotal-state.succeeded-state.failed;state.message=(cancel.get()?"Coda interrotta":"Coda terminata")+" · riusciti "+state.succeeded+" · falliti "+state.failed+" · non completati "+remaining;}
                synchronized(LOCK){state.sizeReport=CompressionSummary.render(sizeResults);}
                writeBatch(batchLog,"\n*** RESOCONTO DIMENSIONI ***\n"+snapshot().sizeReport);
                writeBatch(batchLog,"\n*** FINE CODA ***\n"+snapshot().message+"\nI file completati restano salvati. Esporta questo TXT per tutti i dettagli.");
            }catch(Exception e){
                synchronized(LOCK){state.message="Coda interrotta: "+e.getMessage();}
            }finally {
                main.post(()->{
                    synchronized(LOCK){state.sizeReport=CompressionSummary.render(sizeResults);state.running=false;}
                    releaseWake();stopForeground(STOP_FOREGROUND_REMOVE);publish(true);
                    if(!timedOut)notifyState(snapshot());
                    stopSelf();
                });
            }
        });
        return START_NOT_STICKY;
    }
    private long readFileSize(Uri uri){
        // Prefer the actual finalized descriptor: some providers expose a stale SIZE column.
        try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r")){if(fd!=null){long bytes=fd.getStatSize();if(bytes>0)return bytes;}}catch(Exception ignored){}
        try(android.database.Cursor row=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.SIZE},null,null,null)){
            if(row!=null&&row.moveToFirst()&&!row.isNull(0)){long bytes=row.getLong(0);if(bytes>0)return bytes;}
        }catch(Exception ignored){}
        return -1;
    }
    private void requireWritableFolder(Uri folder)throws IOException{
        try{
            String id=android.provider.DocumentsContract.isDocumentUri(this,folder)?android.provider.DocumentsContract.getDocumentId(folder):android.provider.DocumentsContract.getTreeDocumentId(folder);
            Uri document=android.provider.DocumentsContract.buildDocumentUriUsingTree(folder,id);
            try(android.database.Cursor row=getContentResolver().query(document,new String[]{android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,android.provider.DocumentsContract.Document.COLUMN_FLAGS},null,null,null)){
                if(row==null||!row.moveToFirst()||!android.provider.DocumentsContract.Document.MIME_TYPE_DIR.equals(row.getString(0))||(row.getInt(1)&android.provider.DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE)==0)
                    throw new IOException("Cartella non scrivibile. Sceglila nuovamente nella home o nelle impostazioni.");
            }
        }catch(SecurityException|IllegalArgumentException e){throw new IOException("Accesso alla cartella non disponibile: selezionala nuovamente. "+e.getMessage(),e);}
    }
    static File batchLogFile(android.content.Context c){return new File(c.getFilesDir(),"conversion-batch-last.txt");}
    static File logFile(android.content.Context c){File f=batchLogFile(c);return f.isFile()?f:VideoConverter.logFile(c);}
    private void writeBatch(BufferedWriter writer,String line){
        try{writer.write(line);writer.newLine();if(line.contains("***"))writer.flush();}
        catch(IOException e){throw new java.io.UncheckedIOException("Scrittura log della coda fallita",e);}
        synchronized(LOCK){tail.addLast(line);while(tail.size()>14)tail.removeFirst();state.log=String.join("\n",tail);}
        publish(line.contains("***")||line.contains("SESSION END"));
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
