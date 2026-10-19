package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import java.util.*;
import java.util.concurrent.*;

/** Snapshot and preflight before starting the foreground image queue. */
final class ImageActions {
    private final ImageController images;private final ExecutorService worker=Executors.newSingleThreadExecutor();private boolean closed;
    ImageActions(ImageController images){this.images=images;}
    void start(){
        if(images.busy()||images.state.inputs.isEmpty())return;ImageOptions options=images.state.options;
        if(options.folder==null){images.toast("Scegli una cartella di destinazione");images.screen.sections.focusCard(ImageScreen.FILES);return;}
        ArrayList<String> inputs=new ArrayList<>(images.state.inputs);images.state.starting=true;images.state.startingAt=0;images.state.previousRun=ImageConversionService.snapshot().startedAt;images.render();
        worker.execute(()->{
            String failure=null;try{OutputDocuments.names(images.activity,Uri.parse(options.folder));}catch(Exception e){failure=e.getMessage();}
            String error=failure;images.activity.runOnUiThread(()->{
                if(closed)return;if(error!=null){images.state.starting=false;images.state.notice="Cartella non accessibile: "+error;images.render();images.toast(images.state.notice);return;}
                Intent request=new Intent(images.activity,ImageConversionService.class).setAction(ImageConversionService.START).putStringArrayListExtra("inputs",inputs);options.put(request);
                try{images.state.startingAt=android.os.SystemClock.elapsedRealtime();images.activity.startForegroundService(request);images.screen.sections.focusCard(ImageScreen.HEIC);images.render();}
                catch(RuntimeException e){images.state.starting=false;images.state.notice="Avvio immagini non riuscito: "+e.getMessage();images.render();images.toast(images.state.notice);}
            });
        });
    }
    void cancel(){ImageRunState run=ImageConversionService.snapshot();if(!run.running||run.cancelling)return;new AlertDialog.Builder(images.activity).setTitle("Interrompere la coda immagini?").setMessage("La fase di codifica in corso deve terminare prima dell’annullamento. I file già salvati restano disponibili.").setPositiveButton("Interrompi",(d,w)->images.activity.startService(new Intent(images.activity,ImageConversionService.class).setAction(ImageConversionService.CANCEL).putExtra("startedAt",run.startedAt))).setNegativeButton("Continua",null).show();}
    void close(){closed=true;worker.shutdownNow();}
}
