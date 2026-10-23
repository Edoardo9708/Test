package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import java.util.*;
import java.util.concurrent.*;

/** Snapshot and preflight before starting the foreground image queue. */
final class ImageActions {
    private final ImageController images;private final ExecutorService worker=Executors.newSingleThreadExecutor();private boolean closed;private AlertDialog logConfirmation,cancelConfirmation,discardConfirmation;
    ImageActions(ImageController images){this.images=images;}
    void start(){start(false);}
    void start(boolean metadataOnly){
        if(images.busy()||images.state.inputs.isEmpty())return;
        if(ImageSessionStore.exists(images.activity)){images.toast("Riprendi o scarta prima la coda interrotta nella sezione avanzamento.");images.screen.sections.focusCard(ImageScreen.HEIC);return;}
        ImageOptions options=images.state.options;
        if(!metadataOnly&&!images.destinations.ready()){images.toast("Autorizza tutte le cartelle di destinazione");images.screen.sections.focusCard(ImageScreen.FILES);return;}
        final boolean bulk=images.state.bulk;
        images.stopPreviewReads();
        ArrayList<String> inputs=new ArrayList<>(images.state.inputs),folders=metadataOnly?new ArrayList<>(Collections.nCopies(inputs.size(),"")):images.destinations.snapshot(),names=new ArrayList<>(images.state.names);images.state.starting=true;images.state.startingAt=0;images.state.previousRun=ImageConversionService.snapshot().startedAt;images.render();
        worker.execute(()->{
            String failure=null,id=null;try{
                if(!metadataOnly)for(String folder:new HashSet<>(folders))OutputDocuments.names(images.activity,Uri.parse(folder));
                id=metadataOnly?ImageQueueStore.writeInspection(images.activity,inputs,names,bulk):ImageQueueStore.write(images.activity,inputs,names,folders,bulk);
                ImageRunState initial=new ImageRunState();initial.options=options;initial.metadataOnly=metadataOnly;initial.total=inputs.size();ImageSessionStore.save(images.activity,id,initial);
            }catch(Exception e){if(id!=null)ImageQueueStore.discard(images.activity,id);failure=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
            String error=failure,queueId=id;images.activity.runOnUiThread(()->{
                if(closed){return;}if(error!=null){images.state.starting=false;images.state.notice="Preparazione coda non riuscita: "+error;images.render();images.toast(images.state.notice);return;}
                Intent request=new Intent(images.activity,ImageConversionService.class).setAction(ImageConversionService.START).putExtra("queueId",queueId).putExtra("metadataOnly",metadataOnly);options.put(request);
                try{images.state.startingAt=android.os.SystemClock.elapsedRealtime();images.activity.startForegroundService(request);images.screen.sections.focusCard(ImageScreen.HEIC);images.render();}
                catch(RuntimeException e){images.state.starting=false;images.state.notice="Avvio immagini non riuscito: "+e.getMessage();images.render();images.toast(images.state.notice);}
            });
        });
    }
    void resumeSession(){
        if(images.busy())return;
        images.stopPreviewReads();images.state.starting=true;images.state.startingAt=0;images.state.previousRun=ImageConversionService.snapshot().startedAt;images.render();
        worker.execute(()->{
            Intent request=null;String failure=null;
            try{ImageSessionStore.Session saved=ImageSessionStore.load(images.activity);if(saved==null)throw new java.io.IOException("Nessuna coda da riprendere");request=saved.request(images.activity);}
            catch(Exception error){failure=error.getMessage();}
            Intent ready=request;String problem=failure;
            images.activity.runOnUiThread(()->{
                if(closed)return;
                if(ready==null){images.state.starting=false;images.state.notice="Ripresa non riuscita: "+problem;images.render();images.toast(images.state.notice);return;}
                try{images.state.startingAt=android.os.SystemClock.elapsedRealtime();images.activity.startForegroundService(ready);images.screen.sections.focusCard(ImageScreen.HEIC);images.render();}
                catch(RuntimeException error){images.state.starting=false;images.state.notice="Ripresa non riuscita: "+error.getMessage();images.render();images.toast(images.state.notice);}
            });
        });
    }
    void discardSession(){
        if(images.busy()||!ImageSessionStore.exists(images.activity))return;
        discardConfirmation=new AlertDialog.Builder(images.activity).setTitle("Scartare la coda interrotta?").setMessage("Verrà eliminato il punto di ripresa. Le foto già salvate, il registro e il log restano disponibili.").setNegativeButton("Indietro",null).setPositiveButton("Scarta",(d,w)->{
            if(images.busy())return;images.state.importing=true;images.render();
            worker.execute(()->{ImageSessionStore.discard(images.activity);images.activity.runOnUiThread(()->{if(closed)return;images.state.importing=false;images.render();});});
        }).create();discardConfirmation.show();
    }
    void pause(){ImageRunState run=ImageConversionService.snapshot();if(!run.running||run.cancelling)return;images.activity.startService(new Intent(images.activity,ImageConversionService.class).setAction(run.pauseRequested||run.paused?ImageConversionService.RESUME:ImageConversionService.PAUSE).putExtra("startedAt",run.startedAt));}
    void cancel(){ImageRunState run=ImageConversionService.snapshot();if(!run.running||run.cancelling)return;cancelConfirmation=new AlertDialog.Builder(images.activity).setTitle(run.metadataOnly?"Annullare la verifica?":"Annullare la conversione?").setMessage(run.metadataOnly?"La verifica verrà interrotta. Gli esiti già raccolti restano nel log.":"La fase di codifica in corso deve terminare prima dell’annullamento. I file già salvati restano disponibili.").setPositiveButton("Sì, annulla",(d,w)->images.activity.startService(new Intent(images.activity,ImageConversionService.class).setAction(ImageConversionService.CANCEL).putExtra("startedAt",run.startedAt))).setNegativeButton("Continua",null).create();cancelConfirmation.show();}
    void clearLog(){
        if(images.busy())return;
        logConfirmation=new AlertDialog.Builder(images.activity).setTitle("Cancellare il log immagini?").setMessage("Le foto e il resoconto restano conservati.")
            .setNegativeButton("Indietro",null).setPositiveButton("Cancella",(d,w)->{if(!ImageLogStore.clear(images.activity)){images.toast("Impossibile cancellare il log immagini");return;}ImageConversionService.clearLog();images.render();}).create();logConfirmation.show();
    }
    void close(){closed=true;if(discardConfirmation!=null)discardConfirmation.dismiss();if(cancelConfirmation!=null)cancelConfirmation.dismiss();if(logConfirmation!=null)logConfirmation.dismiss();worker.shutdownNow();}
}
