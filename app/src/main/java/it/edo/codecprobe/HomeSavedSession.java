package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.*;
import java.io.IOException;

/** Durable stop/recovery UI. Saving is owned by the service, never by the Activity lifecycle. */
final class HomeSavedSession {
    static final int PERMISSION=13;
    private final HomeController home;
    AlertDialog dialog;
    private Intent pending;
    Intent pendingResume;

    HomeSavedSession(HomeController home){this.home=home;}

    void bind(){home.screen.conversion.savedSession.setOnClickListener(v->confirm());}

    void render(ConversionService.State state){
        home.screen.conversion.savedSession.setVisibility(state.running||state.recoverable?View.VISIBLE:View.GONE);
        home.screen.conversion.savedSession.setText(state.running?"Salva e interrompi":"Riprendi sessione salvata");
        home.screen.conversion.savedSession.setEnabled(!home.conversion.outputCheck.checking&&(!home.state.metadataLoading||home.selection.metadataBackground)
            &&!home.state.clearingLogs&&!home.state.exportingDiagnostics&&!home.state.choosingOrigins&&!state.cancelling&&!state.skipping);
        home.screen.conversion.sessionHint.setVisibility(state.running||state.recoverable?View.VISIBLE:View.GONE);
        home.screen.conversion.sessionHint.setText(state.running
            ?"La coda viene salvata anche automaticamente. Con Salva e interrompi puoi chiudere l’app: alla ripresa il solo video interrotto ricomincerà dall’inizio."
            :"Report e risultati sono conservati. Il progresso del file interrotto è azzerato: ricomincerà dall’inizio.");
    }

    void confirm(){
        if(dialog!=null||pendingResume!=null||home.state.destroyed)return;
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
            try{
                SavedSession saved=SavedSession.load(home.activity);
                if(saved==null||!saved.state.recoverable||!saved.state.sessionId.equals(state.sessionId)){home.toast("La sessione salvata è cambiata: riprova.");return;}
                Intent original=saved.resumeRequest(),current=ResumeParameters.current(home);
                LinearLayout content=new LinearLayout(home.activity);content.setOrientation(LinearLayout.VERTICAL);
                content.setPadding(home.ui.dp(20),home.ui.dp(8),home.ui.dp(20),home.ui.dp(12));
                home.ui.text(content,"Video totali: "+state.batchTotal+" · già riusciti: "+state.succeeded+
                    "\nReport e destinazioni restano conservati. Il video interrotto riparte dall’inizio; la scelta vale per tutti i video ancora da elaborare.",15);
                RadioGroup choices=new RadioGroup(home.activity);choices.setOrientation(RadioGroup.VERTICAL);content.addView(choices);
                RadioButton old=option(choices,"Usa i parametri salvati\n"+ResumeParameters.describe(original));
                RadioButton updated=option(choices,"Usa quelli attuali della scheda 2\n"+ResumeParameters.describe(current));
                choices.check(old.getId());
                ScrollView scroll=new ScrollView(home.activity);scroll.addView(content);
                dialog=new AlertDialog.Builder(home.activity).setTitle("Parametri per la ripresa").setView(scroll)
                    .setNegativeButton("Non ora",null).setPositiveButton("Riprendi",(d,w)->{
                        pendingResume=ResumeParameters.choose(original,current,choices.getCheckedRadioButtonId()==updated.getId());
                        resume();
                    }).create();
            }catch(IOException error){home.toast(error.getMessage());return;}
        }else return;
        dialog.setOnDismissListener(d->dialog=null);dialog.show();
    }

    void resume(){
        if(pendingResume==null||home.state.destroyed||home.state.busy||ConversionService.snapshot().running)return;
        if(!OriginalMedia.hasLocationPermission(home.activity)){
            home.activity.requestPermissions(new String[]{android.Manifest.permission.ACCESS_MEDIA_LOCATION},PERMISSION);return;
        }
        if(!OriginalMedia.hasVideoPermission(home.activity)){
            home.activity.requestPermissions(OriginalMedia.videoPermissions(),PERMISSION);return;
        }
        try{
            SavedSession saved=SavedSession.load(home.activity);
            if(saved==null||!saved.state.recoverable||!saved.state.sessionId.equals(pendingResume.getStringExtra(SavedSession.RESTORE))){pendingResume=null;home.toast("Nessuna sessione corrispondente da riprendere");return;}
            Intent request=pendingResume;pendingResume=null;
            FolderPickerActivity.record(home.activity,"*** PARAMETRI SCELTI PER LA RIPRESA ***\nScelta: "+request.getStringExtra("resumeParameterChoice")+"\n"+ResumeParameters.describe(request));
            home.conversion.outputCheck.begin(request);
        }catch(IOException error){pendingResume=null;home.toast(error.getMessage());}
    }

    private RadioButton option(RadioGroup group,String text){
        RadioButton button=new RadioButton(home.activity);button.setId(View.generateViewId());
        button.setText(text);button.setTextColor(home.ui.ink);button.setTextSize(15);button.setSingleLine(false);
        button.setPadding(home.ui.dp(8),home.ui.dp(14),home.ui.dp(8),home.ui.dp(14));
        group.addView(button,new RadioGroup.LayoutParams(-1,-2));return button;
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
        if(results.length==0){pendingResume=null;home.toast("Ripresa non avviata: permessi mancanti");return;}
        for(int result:results)if(result!=android.content.pm.PackageManager.PERMISSION_GRANTED){pendingResume=null;home.toast("Ripresa non avviata: permessi mancanti");return;}
        resume();
    }

    void close(){if(dialog!=null){dialog.dismiss();dialog=null;}pending=null;pendingResume=null;}
}
