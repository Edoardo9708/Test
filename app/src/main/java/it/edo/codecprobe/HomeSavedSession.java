package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import java.io.IOException;

/** Durable stop/recovery UI. Saving is owned by the service, never by the Activity lifecycle. */
final class HomeSavedSession {
    static final int PERMISSION=13;
    private final HomeController home;
    AlertDialog dialog;
    private Intent pending;

    HomeSavedSession(HomeController home){this.home=home;}

    void bind(){home.screen.conversion.savedSession.setOnClickListener(v->confirm());}

    void render(ConversionService.State state){
        home.screen.conversion.savedSession.setVisibility(state.running||state.recoverable?View.VISIBLE:View.GONE);
        home.screen.conversion.savedSession.setText(state.running?"Salva e interrompi":"Riprendi sessione salvata");
        home.screen.conversion.savedSession.setEnabled(!home.conversion.outputCheck.checking&&!home.state.metadataLoading
            &&!home.state.clearingLogs&&!home.state.exportingDiagnostics&&!home.state.choosingOrigins&&!state.cancelling&&!state.skipping);
        home.screen.conversion.sessionHint.setVisibility(state.running||state.recoverable?View.VISIBLE:View.GONE);
        home.screen.conversion.sessionHint.setText(state.running
            ?"La coda viene salvata anche automaticamente. Con Salva e interrompi puoi chiudere l’app: alla ripresa il solo video interrotto ricomincerà dall’inizio."
            :"Report e risultati sono conservati. L’avanzamento mostrato è quello salvato; il video interrotto ricomincerà dall’inizio.");
    }

    void confirm(){
        if(dialog!=null||home.state.destroyed)return;
        ConversionService.State state=ConversionService.snapshot();
        if(state.running){
            if(state.cancelling||state.skipping)return;
            dialog=new AlertDialog.Builder(home.activity).setTitle("Salvare e interrompere?")
                .setMessage("Conservo coda, parametri, report e risultati già completati. Interrompo il video attuale ed elimino i suoi temporanei.\n\nAlla riapertura potrai riprendere la coda; il video interrotto ripartirà dall’inizio. Attendi il messaggio Sessione salvata prima di chiudere l’app.")
                .setNegativeButton("Continua",null).setPositiveButton("Salva e interrompi",(d,w)->{
                    ConversionService.State current=ConversionService.snapshot();
                    if(!current.running||current.startedAt!=state.startedAt)return;
                    home.activity.startService(new Intent(home.activity,ConversionService.class).setAction(ConversionService.SAVE_STOP)
                        .putExtra(MainActivity.CANCEL_SESSION,state.startedAt));
                }).create();
        }else if(state.recoverable){
            dialog=new AlertDialog.Builder(home.activity).setTitle("Riprendere la sessione salvata?")
                .setMessage("Video totali: "+state.batchTotal+"\nGià riusciti: "+state.succeeded+"\nFalliti già registrati: "+state.failed+
                    "\n\nConservo il report e salto tutti i video già conclusi. Ricontrollo i nomi nella destinazione. Il video interrotto riparte dall’inizio, con i parametri salvati.")
                .setNegativeButton("Non ora",null).setPositiveButton("Riprendi",(d,w)->resume()).create();
        }else return;
        dialog.setOnDismissListener(d->dialog=null);dialog.show();
    }

    void resume(){
        if(home.state.destroyed||home.state.busy||ConversionService.snapshot().running)return;
        if(!OriginalMedia.hasLocationPermission(home.activity)){
            home.activity.requestPermissions(new String[]{android.Manifest.permission.ACCESS_MEDIA_LOCATION},PERMISSION);return;
        }
        if(!OriginalMedia.hasVideoPermission(home.activity)){
            home.activity.requestPermissions(OriginalMedia.videoPermissions(),PERMISSION);return;
        }
        try{
            SavedSession saved=SavedSession.load(home.activity);
            if(saved==null||!saved.state.recoverable){home.toast("Nessuna sessione da riprendere");return;}
            home.conversion.outputCheck.begin(saved.resumeRequest());
        }catch(IOException error){home.toast(error.getMessage());}
    }

    void confirmReplacement(Intent task){
        if(dialog!=null)return;
        pending=task;
        dialog=new AlertDialog.Builder(home.activity).setTitle("Sostituire la sessione salvata?")
            .setMessage("È presente una coda da riprendere. Avviandone una nuova, sostituisci la sessione e il suo report. I video già salvati restano nella loro cartella.")
            .setNegativeButton("Conserva sessione",null).setPositiveButton("Avvia nuova coda",(d,w)->{
                Intent next=pending;pending=null;
                if(next!=null)home.conversion.outputCheck.begin(next.putExtra("replaceSavedSession",true));
            }).create();
        dialog.setOnDismissListener(d->{dialog=null;pending=null;});dialog.show();
    }

    void onPermissions(int[] results){
        if(results.length==0){home.toast("Ripresa non avviata: permessi mancanti");return;}
        for(int result:results)if(result!=android.content.pm.PackageManager.PERMISSION_GRANTED){home.toast("Ripresa non avviata: permessi mancanti");return;}
        resume();
    }

    void close(){if(dialog!=null){dialog.dismiss();dialog=null;}pending=null;}
}
