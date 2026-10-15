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
    private static Intent lastRetry;
    private static boolean retryLoaded;
    static Intent retryRequest(android.content.Context context){synchronized(LOCK){
        if(state.running)return null;
        if(!retryLoaded){lastRetry=BatchRetry.load(context);retryLoaded=true;}
        return lastRetry==null?null:new Intent(lastRetry);
    }}
    static void rememberFailures(android.content.Context context,Intent task){
        synchronized(LOCK){lastRetry=task==null?null:new Intent(task);retryLoaded=true;}
        if(!BatchRetry.save(context,task))FolderPickerActivity.record(context,"*** AVVISO RIPROVA FALLITI ***\nSalvataggio su disco non riuscito: elenco disponibile solo finché il processo resta aperto.");
    }
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
        boolean hasSession,running,originMode,applyAllDestinations,allOrigin=true,onlySmaller;
        String allFolder="";
        boolean[] individualOrigins=new boolean[0];
        ArrayList<String> individualFolders=new ArrayList<>();
        int percent,filePercent,batchIndex,batchTotal,succeeded,failed,ignored,notSaved,doneFrames,totalFrames=-1;
        ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>(),folders=new ArrayList<>();
        ArrayList<Uri> results=new ArrayList<>();
        ArrayList<ConversionFailure> failures=new ArrayList<>();
        int bframeCount=-1,lookaheadCount=-1,cuTree=-1,aqMode=-1,aqStrength=-1;
        long done,total,startedAt,endedAt;
        int requestedCrf=-1,requestedCpu=-1;
        String requestedPreset="—";
        String message="",log="",source="",sizeReport="";
        Uri result,input,folder;
        State(){}
        State(State s){failures=new ArrayList<>(s.failures);startedAt=s.startedAt;endedAt=s.endedAt;requestedCrf=s.requestedCrf;requestedCpu=s.requestedCpu;requestedPreset=s.requestedPreset;sizeReport=s.sizeReport;applyAllDestinations=s.applyAllDestinations;allOrigin=s.allOrigin;allFolder=s.allFolder;individualOrigins=s.individualOrigins.clone();individualFolders=new ArrayList<>(s.individualFolders);originMode=s.originMode;inputs=new ArrayList<>(s.inputs);names=new ArrayList<>(s.names);folders=new ArrayList<>(s.folders);results=new ArrayList<>(s.results);filePercent=s.filePercent;batchIndex=s.batchIndex;batchTotal=s.batchTotal;succeeded=s.succeeded;failed=s.failed;ignored=s.ignored;notSaved=s.notSaved;onlySmaller=s.onlySmaller;hasSession=s.hasSession;running=s.running;bframeCount=s.bframeCount;lookaheadCount=s.lookaheadCount;cuTree=s.cuTree;aqMode=s.aqMode;aqStrength=s.aqStrength;percent=s.percent;doneFrames=s.doneFrames;totalFrames=s.totalFrames;done=s.done;total=s.total;message=s.message;log=s.log;source=s.source;result=s.result;input=s.input;folder=s.folder;}
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
    private void notifyState(State s){failures=new ArrayList<>(s.failures);
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        getSystemService(NotificationManager.class).notify(NOTICE,notification(s));
    }
    private Notification notification(State s){
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder n=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_notification_video)
            .setContentTitle(s.running?"Video "+Math.max(1,s.batchIndex)+"/"+s.batchTotal+" · "+s.filePercent+"%":"Conversione video")
            .setContentText(s.message).setStyle(new Notification.BigTextStyle().bigText(s.message+"\n"+s.source))
            .setContentIntent(open).setOnlyAlertOnce(true).setOngoing(s.running);
        if(s.running){
            n.setProgress(100,s.filePercent,false);
            PendingIntent stop=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class).setAction(MainActivity.CONFIRM_CANCEL).putExtra(MainActivity.CANCEL_SESSION,s.startedAt).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            n.addAction(new Notification.Action.Builder(null,"Annulla…",stop).build());
        }else n.setAutoCancel(true);
        return n.build();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        if(CANCEL.equals(intent.getAction())) {
            long expected=intent.getLongExtra(MainActivity.CANCEL_SESSION,-1);State current=snapshot();if(expected>=0&&(!current.running||current.startedAt!=expected))return START_NOT_STICKY;
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
        final Intent originalRequest=new Intent(intent);
        final ArrayList<Integer> failedIndices=new ArrayList<>();
        final boolean onlySmaller=intent.getBooleanExtra("onlySmaller",false);
        final ArrayList<String> outputNames=intent.getStringArrayListExtra("outputNames");
        final boolean[] omitted=intent.getBooleanArrayExtra("omitted");
        if(outputNames!=null&&(outputNames.size()!=inputs.size()||omitted==null||omitted.length!=inputs.size())){stopSelf();return START_NOT_STICKY;}
        final ArrayList<CompressionSummary.Item> sizeResults=new ArrayList<>();
        for(int n=0;n<queueInputs.size();n++)sizeResults.add(new CompressionSummary.Item(n+1,queueNames.get(n),CompressionSummary.Status.SKIPPED,-1,-1));
        final ArrayList<String> destinationNotes=intent.getStringArrayListExtra("destinationNotes");
        Uri input=Uri.parse(inputs.get(0)),folder=Uri.parse(folders.get(0));String source=names.get(0);
        int crf=intent.getIntExtra("crf",23),preset=intent.getIntExtra("preset",6),cpu=intent.getIntExtra("cpu",1);
        boolean bframes=intent.getBooleanExtra("bframes",true),lookahead=intent.getBooleanExtra("lookahead",true);
        synchronized(LOCK){state.individualOrigins=intent.getBooleanArrayExtra("individualOrigins");if(state.individualOrigins==null)state.individualOrigins=new boolean[queueInputs.size()];
            ArrayList<String> requestedChoices=intent.getStringArrayListExtra("individualFolders");state.individualFolders=requestedChoices==null?new ArrayList<>(queueFolders):new ArrayList<>(requestedChoices);
            state.applyAllDestinations=intent.getBooleanExtra("applyAllDestinations",false);state.allOrigin=intent.getBooleanExtra("allOrigin",true);state.allFolder=intent.getStringExtra("allFolder");
            state.startedAt=SystemClock.elapsedRealtime();state.endedAt=0;state.requestedCrf=crf;state.requestedCpu=cpu;state.requestedPreset=AppSettings.PRESETS[Math.max(0,Math.min(8,preset))];state.originMode=intent.getBooleanExtra("originMode",false);state.inputs=queueInputs;state.names=queueNames;state.folders=queueFolders;state.results.clear();state.failures.clear();state.sizeReport=CompressionSummary.render(sizeResults,true);state.batchIndex=0;state.batchTotal=queueInputs.size();state.succeeded=0;state.failed=0;state.ignored=0;state.notSaved=0;state.onlySmaller=onlySmaller;state.filePercent=0;state.hasSession=true;state.running=true;state.bframeCount=-1;state.lookaheadCount=-1;state.cuTree=-1;state.aqMode=-1;state.aqStrength=-1;state.percent=0;state.doneFrames=0;state.totalFrames=-1;state.done=0;state.total=0;state.result=null;state.log="";state.message="Analisi video…";state.source=source;state.input=input;state.folder=folder;}
        try {
            int type=Build.VERSION.SDK_INT>=35?ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING:ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
            startForeground(NOTICE,notification(snapshot()),type);
            PowerManager pm=getSystemService(PowerManager.class);
            wake=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CodecProbe:conversion");
            wake.acquire(6*60*60*1000L);
        }catch(RuntimeException e){
            synchronized(LOCK){state.endedAt=SystemClock.elapsedRealtime();state.running=false;state.message="Impossibile avviare il servizio: "+e.getMessage();}
            releaseWake();publish(true);stopSelf();return START_NOT_STICKY;
        }
        cancel.set(false);timedOut=false;tail.clear();publish(true);
        worker.execute(()->{
            try(BufferedWriter batchLog=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(batchLogFile(this)),java.nio.charset.StandardCharsets.UTF_8),65536)) {
                rememberFailures(this,null);
                writeBatch(batchLog,"*** INIZIO CODA · v"+BuildConfig.VERSION_NAME+" ***\nVideo selezionati: "+queueInputs.size()+"\n1. Analisi del file e metadati\n2. Codifica x265 CRF e verifica completa\n3. Salvataggio in un nuovo file; poi video successivo\nParametri: CRF="+crf+" preset="+AppSettings.PRESETS[Math.max(0,Math.min(8,preset))]+" CPU="+cpu+" B-frame="+bframes+" Lookahead="+lookahead+"\nSalva solo se più piccolo="+onlySmaller+" · riprova_falliti="+intent.getBooleanExtra("retryFailures",false)+"\nLa percentuale totale assegna lo stesso peso a ogni video; non è una stima del tempo.");
                for(int index=0;index<queueInputs.size()&&!cancel.get();index++){
                    if(omitted!=null && omitted[index]){
                        sizeResults.set(index,new CompressionSummary.Item(index+1,queueNames.get(index),CompressionSummary.Status.OMITTED,-1,-1));
                        synchronized(LOCK){state.ignored++;state.batchIndex=index+1;state.sizeReport=CompressionSummary.render(sizeResults,true);}
                        writeBatch(batchLog,"*** VIDEO IGNORATO SU RICHIESTA ***\nNome: "+queueNames.get(index)+"\nIl file esistente resta invariato; nessuna codifica eseguita.");publish(true);continue;
                    }
                    final int current=index;
                    sizeResults.set(index,new CompressionSummary.Item(index+1,queueNames.get(index),CompressionSummary.Status.IN_PROGRESS,-1,-1));
                    synchronized(LOCK){state.sizeReport=CompressionSummary.render(sizeResults,true);state.batchIndex=index+1;state.filePercent=0;state.percent=BatchProgress.percent(index,0,queueInputs.size());state.doneFrames=0;state.totalFrames=-1;state.done=0;state.total=0;state.source=queueNames.get(index);state.input=Uri.parse(queueInputs.get(index));state.folder=Uri.parse(queueFolders.get(index));state.message="Analisi video "+(index+1)+" / "+queueInputs.size();state.bframeCount=-1;}
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
                        converter.onlySmaller=onlySmaller;
                        converter.plannedOutputName=outputNames==null?null:outputNames.get(index);
                        writeBatch(batchLog,"*** CONTROLLO NOME OUTPUT ***\nNome approvato: "+converter.plannedOutputName);
                        Uri result=converter.convert(Uri.parse(queueInputs.get(index)),Uri.parse(queueFolders.get(index)),crf,queueNames.get(index),preset,cpu,bframes,lookahead);
                        originalBytes=converter.originalByteSize>0?converter.originalByteSize:originalBytes;
                        long outputBytes=readFileSize(result);
                        sizeResults.set(index,new CompressionSummary.Item(index+1,queueNames.get(index),CompressionSummary.Status.COMPLETE,originalBytes,outputBytes));
                        synchronized(LOCK){state.result=result;state.results.add(result);state.succeeded++;state.sizeReport=CompressionSummary.render(sizeResults,true);}
                        writeBatch(batchLog,"*** RISULTATO DIMENSIONI ***\n"+CompressionSummary.row(sizeResults.get(index))+"\nInput byte: "+originalBytes+"\nOutput byte: "+outputBytes);
                        writeBatch(batchLog,"*** VIDEO COMPLETATO ***\nOutput: "+result);
                    }catch(SavingsPolicy.NotSaved skipped){
                        CompressionSummary.Status status=skipped.decision==SavingsPolicy.Decision.NO_SAVING?CompressionSummary.Status.NO_SAVING:CompressionSummary.Status.SIZE_UNKNOWN;
                        sizeResults.set(index,new CompressionSummary.Item(index+1,queueNames.get(index),status,skipped.original,skipped.converted));
                        synchronized(LOCK){state.notSaved++;state.filePercent=100;state.sizeReport=CompressionSummary.render(sizeResults,true);}
                        writeBatch(batchLog,"*** VIDEO ELABORATO SENZA SALVATAGGIO ***\n"+CompressionSummary.row(sizeResults.get(index))+"\nNon è un errore; escluso da Riprova i falliti.");
                    }catch(Exception e){
                        if(cancel.get()){sizeResults.set(index,new CompressionSummary.Item(index+1,queueNames.get(index),CompressionSummary.Status.CANCELLED,-1,-1));synchronized(LOCK){state.sizeReport=CompressionSummary.render(sizeResults,true);}writeBatch(batchLog,"*** VIDEO INTERROTTO ***\n"+e);break;}
                        sizeResults.set(index,new CompressionSummary.Item(index+1,queueNames.get(index),CompressionSummary.Status.FAILED,-1,-1));
                        failedIndices.add(index);rememberFailures(this,BatchRetry.select(originalRequest,failedIndices));
                        writeBatch(batchLog,"*** RIPROVA FALLITI AGGIORNATO ***\nVideo eleggibili: "+failedIndices.size()+" · parametri, cartelle e nomi originali conservati.");
                        ConversionFailure failure=new ConversionFailure(index+1,queueNames.get(index),snapshot().message,e);
                        synchronized(LOCK){state.failures.add(failure);state.failed++;state.sizeReport=CompressionSummary.render(sizeResults,true);}
                        writeBatch(batchLog,"*** VIDEO FALLITO · CONTINUO CON IL SUCCESSIVO ***\nNome: "+queueNames.get(index)+"\n"+failure.copyText());
                    }
                    synchronized(LOCK){state.percent=BatchProgress.percent(index+1,0,queueInputs.size());if(sizeResults.get(index).status==CompressionSummary.Status.COMPLETE)state.filePercent=100;}batchLog.flush();publish(true);
                }
                synchronized(LOCK){int remaining=state.batchTotal-state.succeeded-state.failed-state.ignored-state.notSaved;state.message=(cancel.get()?"Coda interrotta":"Coda terminata")+" · riusciti "+state.succeeded+" · falliti "+state.failed+" · ignorati "+state.ignored+" · non salvati "+state.notSaved+" · non completati "+remaining;}
                synchronized(LOCK){state.sizeReport=CompressionSummary.render(sizeResults);}
                writeBatch(batchLog,"\n*** RESOCONTO DIMENSIONI ***\n"+snapshot().sizeReport);
                writeBatch(batchLog,"\n*** FINE CODA ***\n"+snapshot().message+"\nTempo trascorso: "+DashboardMetrics.elapsed(snapshot().startedAt,0,SystemClock.elapsedRealtime())+"\nI file completati restano salvati. Esporta questo TXT per tutti i dettagli.");
            }catch(Exception e){
                synchronized(LOCK){state.message="Coda interrotta: "+e.getMessage();}
            }finally {
                main.post(()->{
                    synchronized(LOCK){state.sizeReport=CompressionSummary.render(sizeResults);state.endedAt=SystemClock.elapsedRealtime();state.running=false;}
                    releaseWake();stopForeground(STOP_FOREGROUND_REMOVE);publish(true);
                    if(!timedOut)notifyState(snapshot());
                    stopSelf();
                });
            }
        });
        return START_NOT_STICKY;
    }
    private long readFileSize(Uri uri){return MediaFileSizes.read(this,uri);}
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

