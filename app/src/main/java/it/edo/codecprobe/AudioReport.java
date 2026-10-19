package it.edo.codecprobe;

import android.content.*;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;

/** Audio results supply data/actions to the same rows and popup used by video. */
final class AudioReport {
    static void render(AudioController audio,AudioRunState state){
        AppUi ui=audio.screen.ui;LinearLayout report=audio.screen.report;report.removeAllViews();
        audio.screen.workReport.panel.setVisibility(state.results.isEmpty()?View.GONE:View.VISIBLE);
        audio.screen.savings.setVisibility(View.GONE);
        long before=0,after=0;boolean known=true;int saved=0;
        for(int n=0;n<state.results.size();n++){
            AudioRunState.Result result=state.results.get(n);String title="Brano "+(n+1)+" · "+result.name.replace('\n',' ').replace('\r',' ');String status;
            if(result.outcome==AudioRunState.Outcome.SAVED){saved++;known&=result.inputBytes>0;before+=Math.max(0,result.inputBytes);after+=result.outputBytes;status=CompressionSummary.size(result.inputBytes)+" → "+CompressionSummary.size(result.outputBytes)+" · "+CompressionSummary.change(result.inputBytes,result.outputBytes);}
            else if(result.outcome==AudioRunState.Outcome.CANCELLED)status="Conversione annullata";
            else if(result.outcome==AudioRunState.Outcome.NOT_SAVED)status=result.error;
            else status="Conversione fallita · nessun nuovo file completato";
            ResultReportRow.add(ui,report,title+"\n"+status,
                result.outcome==AudioRunState.Outcome.SAVED&&result.uri!=null?()->open(audio,result.uri):null,
                result.outcome==AudioRunState.Outcome.FAILED?()->audio.resultDialog.show("Conversione fallita",title,result.message,result.lastState,result.details,result.error==null?title:result.error):null);
        }
        if(!state.results.isEmpty()){
            String total=saved==0?"Nessun file salvato":known?CompressionSummary.size(before)+" → "+CompressionSummary.size(after)+" · "+CompressionSummary.change(before,after)+"\nSpazio "+(before>=after?"risparmiato: ":"aggiuntivo: ")+CompressionSummary.size(Math.abs(before-after)):"Confronto dimensioni non disponibile · output "+CompressionSummary.size(after);
            ResultReportRow.add(ui,report,(state.running?"TOTALE PARZIALE · AUDIO RIUSCITI\n":"TOTALE · AUDIO RIUSCITI\n")+total+"\nDimensioni del file completo, inclusi i metadati. MB = 1.000.000 byte.",null,null);
        }
    }
    private static void open(AudioController audio,String uri){
        try{audio.activity.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri),"audio/ogg").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}
        catch(ActivityNotFoundException error){audio.toast("Non è installato un lettore per questo file Opus");}
        catch(SecurityException error){audio.toast("Accesso al file non più disponibile: "+error.getMessage());}
    }
    private AudioReport(){}
}
