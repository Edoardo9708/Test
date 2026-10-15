package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.concurrent.*;

/** Resolves filename collisions before any expensive encoding starts. */
final class HomeOutputCheck {
    private final HomeController home;
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    boolean checking;
    AlertDialog dialog;
    private Intent task;
    private ArrayList<String> inputs,folders;
    private OutputPlan plan;
    private AppSettings naming;
    HomeOutputCheck(HomeController home) { this.home=home; }

    void begin(Intent task) {
        if(checking)return;
        checking=true;this.task=task;
        inputs=task.getStringArrayListExtra("names");folders=task.getStringArrayListExtra("folders");
        plan=new OutputPlan(inputs.size());naming=new AppSettings(home.activity);
        home.conversion.setBusy(true);
        home.screen.conversion.stop.setEnabled(false);
        home.screen.sections.focusCard(MainActivity.CARD_CONVERSION);
        FolderPickerActivity.record(home.activity,"*** INIZIO CONTROLLO NOMI OUTPUT ***\nControllo file esistenti e nomi duplicati nella coda prima della codifica.");
        next();
    }

    private void next() {
        if(!checking||home.state.destroyed)return;
        int index=plan.names.size();
        if(index==inputs.size()) {
            task.putStringArrayListExtra("outputNames",new ArrayList<>(plan.names)).putExtra("omitted",plan.omitted);
            checking=false;
            home.conversion.launchPrepared(task);
            return;
        }
        home.screen.conversion.status.setText("Controllo nomi di destinazione · "+(index+1)+" / "+inputs.size());
        worker.execute(()->{
            try {
                Uri folder=Uri.parse(folders.get(index));
                String key=OutputDocuments.key(home.activity,folder);
                if(!plan.containsDirectory(key))plan.directory(key,OutputDocuments.names(home.activity,folder));
                String requested=OutputName.base(inputs.get(index),naming.addition,naming.prefix);
                home.handler.post(()->{
                    if(!checking||home.state.destroyed)return;
                    if(plan.conflicts(key,requested))showConflict(index,key,requested);
                    else {plan.accept(key,requested);next();}
                });
            } catch(Exception error) {
                home.handler.post(()->{if(checking&&!home.state.destroyed){
                    abort();home.screen.conversion.status.setText("Controllo nomi non riuscito: "+error.getMessage());
                    FolderPickerActivity.record(home.activity,"*** CONTROLLO NOMI FALLITO ***\n"+error);
                }});
            }
        });
    }

    void showConflict(int index,String key,String requested) {
        LinearLayout content=new LinearLayout(home.activity);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(home.ui.dp(24),home.ui.dp(8),home.ui.dp(24),home.ui.dp(8));
        EditText name=new EditText(home.activity);name.setSingleLine(true);name.setTextColor(home.ui.ink);
        name.setText(plan.suggestion(key,requested));name.setSelectAllOnFocus(true);
        name.setHint("Nome del nuovo video (.mp4)");name.setContentDescription("Nome del nuovo video");content.addView(name);
        dialog=new AlertDialog.Builder(home.activity).setTitle("File già presente")
            .setMessage("Video "+(index+1)+" / "+inputs.size()+" · "+inputs.get(index)+
                "\n\nIl nome “"+requested+"” è già presente nella destinazione o previsto per un altro video della coda.\n\nIgnora questo video oppure modifica il nome per convertirlo di nuovo. Il file esistente resta invariato.")
            .setView(content).setNegativeButton("Annulla coda",(d,w)->abort())
            .setNeutralButton("Ignora video",(d,w)->{
                FolderPickerActivity.record(home.activity,"*** VIDEO IGNORATO ***\nInput: "+inputs.get(index)+"\nNome già presente: "+requested);
                plan.skip();dialog=null;next();
            }).setPositiveButton("Converti con nuovo nome",null).create();
        dialog.setOnCancelListener(d->abort());dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try {
                String chosen=OutputPlan.manual(name.getText().toString());
                if(plan.conflicts(key,chosen)){name.setError("Questo nome è già utilizzato. Scegline un altro.");return;}
                FolderPickerActivity.record(home.activity,"*** NOME OUTPUT MODIFICATO ***\nPrima: "+requested+"\nScelto: "+chosen);
                plan.accept(key,chosen);dialog.dismiss();dialog=null;next();
            } catch(IllegalArgumentException error) { name.setError(error.getMessage()); }
        });
    }

    private void abort() {
        checking=false;dialog=null;
        home.conversion.setBusy(false);
        home.screen.conversion.status.setText("Avvio annullato · nessuna conversione iniziata");
    }
    void close() {checking=false;if(dialog!=null){dialog.dismiss();dialog=null;}worker.shutdownNow();}
}
