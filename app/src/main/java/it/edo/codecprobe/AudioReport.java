package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.widget.*;

/** Per-brano report actions; no queue mutation or encoder control. */
final class AudioReport {
    static void render(AudioController audio,AudioRunState state){
        AppUi ui=audio.screen.ui;LinearLayout report=audio.screen.report;report.removeAllViews();
        if(state.results.isEmpty()){ui.text(report,"Nessun brano completato",14);return;}
        for(int n=0;n<state.results.size();n++){
            AudioRunState.Result result=state.results.get(n);LinearLayout row=ui.row(report);
            TextView text=ui.text(row,(n+1)+". "+result.name+"\n"+(result.uri!=null?AudioRunState.sizes(result.inputBytes,result.outputBytes):result.outcome==AudioRunState.Outcome.CANCELLED?"Interrotto":result.outcome==AudioRunState.Outcome.NOT_SAVED?result.error:"Conversione fallita"),14);
            text.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
            Button action=new Button(audio.activity);ui.styleButton(action,false);action.setText(result.uri!=null?"Apri":result.outcome==AudioRunState.Outcome.FAILED?"Errore":"Dettagli");row.addView(action,new LinearLayout.LayoutParams(ui.dp(92),-2));
            action.setOnClickListener(v->{
                if(result.uri!=null){
                    try{audio.activity.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(result.uri),"audio/ogg").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}
                    catch(ActivityNotFoundException error){audio.toast("Non è installato un lettore per questo file Opus");}
                    catch(SecurityException error){audio.toast("Accesso al file non più disponibile: "+error.getMessage());}
                }else new AlertDialog.Builder(audio.activity).setTitle(result.outcome==AudioRunState.Outcome.FAILED?"Errore audio":"Esito audio").setMessage(result.error)
                    .setNeutralButton("Copia tutto",(dialog,which)->{audio.activity.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Esito audio",result.error));audio.toast("Dettagli copiati");})
                    .setPositiveButton("Chiudi",null).show();
            });
        }
    }
    private AudioReport(){}
}
