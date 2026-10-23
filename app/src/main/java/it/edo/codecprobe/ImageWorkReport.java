package it.edo.codecprobe;

import android.content.*;
import android.net.Uri;
import java.util.*;

/** Photo results adapted to the shared work report, without controlling conversion. */
final class ImageWorkReport {
    private final ImageController images;
    private List<ImageRunState.Result> rendered;
    private boolean renderedRunning,renderedPaused,renderedPauseRequested,renderedMetadataOnly;
    private String renderedMessage="";
    private int renderedCurrent=-1,renderedTotal=-1;
    private final ResultDialog popup;
    private final ArrayList<ResultReportRow.Reusable> rows=new ArrayList<>();

    ImageWorkReport(ImageController images) { this.images = images;popup=new ResultDialog(images.screen.ui,images::toast); }

    void render(ImageRunState run) {
        WorkReportView view = images.screen.workReport;
        UiText.set(view.title,run.metadataOnly?"VERIFICA METADATI"+(run.paused?" · IN PAUSA":run.running?" · IN CORSO":""):run.paused ? "REPORT LAVORI · IN PAUSA" : run.running ? "REPORT LAVORI · IN CORSO" : "REPORT LAVORI");
        view.panel.setVisibility(run.total==0&&run.completed==0?android.view.View.GONE:android.view.View.VISIBLE);
        view.summary.setVisibility(android.view.View.VISIBLE);
        if(rendered!=null&&rendered.equals(run.results)&&renderedMetadataOnly==run.metadataOnly&&renderedRunning==run.running&&renderedCurrent==run.current&&renderedTotal==run.total&&renderedPaused==run.paused&&renderedPauseRequested==run.pauseRequested&&renderedMessage.equals(run.message))return;
        renderedMetadataOnly=run.metadataOnly;rendered=new ArrayList<>(run.results);renderedRunning=run.running;renderedCurrent=run.current;renderedTotal=run.total;renderedPaused=run.paused;renderedPauseRequested=run.pauseRequested;renderedMessage=run.message;
        AppUi ui=images.screen.ui;
        boolean large=run.total>ImageQueueStore.VISIBLE;
        int start=large?run.windowStart():0,end=large?Math.min(run.total,start+ImageQueueStore.VISIBLE):run.completed;
        int count=end-start;
        while(rows.size()>count){ResultReportRow.Reusable removed=rows.remove(rows.size()-1);view.rows.removeView(removed.row);}
        while(rows.size()<count)rows.add(new ResultReportRow.Reusable(ui,view.rows));
        for(int n=start;n<end;n++){
            ResultReportRow.Reusable row=rows.get(n-start);
            ImageRunState.Result result=run.resultAt(n);
            if(result==null){
                String name=n<run.queueNames.size()?run.queueNames.get(n):"Foto "+(n+1);
                String status=run.running&&n==run.current-1?"IN CORSO · "+ImageConversionService.displayMessage(run):n<run.completed?"Completata · dettagli nel log":run.running?"In attesa":run.cancelling?"Non elaborata":"Non elaborata";
                row.bind("Foto "+(n+1)+" · "+name.replace('\n',' ').replace('\r',' ')+"\n"+status,null,null,false);continue;
            }
            String status;
            switch(result.outcome){
                case SAVED:status=CompressionSummary.size(result.inputBytes)+" → "+CompressionSummary.size(result.outputBytes)+" · "+CompressionSummary.change(result.inputBytes,result.outputBytes);break;
                case ALREADY_DONE:status="Già convertita · originale e HEIC verificati · nessuna ricodifica";break;
                case CHECKED:status=result.message;break;
                case FAILED:status=run.metadataOnly?"Da verificare · "+result.message:"Conversione fallita · nessun nuovo file completato";break;
                case NOT_SMALLER:status="Nessun risparmio · risultato non salvato";break;
                default:status=run.metadataOnly?"Verifica annullata":"Conversione annullata";
            }
            String title="Foto "+(n+1)+" · "+result.name.replace('\n',' ').replace('\r',' ');
            row.bind(title+"\n"+status,
                (result.outcome==ImageRunState.Outcome.SAVED||result.outcome==ImageRunState.Outcome.ALREADY_DONE)&&result.uri!=null?()->open(result.uri):null,
                (result.outcome==ImageRunState.Outcome.FAILED||result.outcome==ImageRunState.Outcome.CHECKED)?()->show(result,run.metadataOnly):null,result.outcome==ImageRunState.Outcome.CHECKED);
        }
        long before=run.inputSavedBytes,after=run.outputSavedBytes;
        view.summary.setText(run.metadataOnly?"Compatibili: "+run.checked+" · da verificare: "+run.failed+" · annullate: "+run.cancelledCount+"\nNessuna decodifica o codifica. Esito limitato ai metadati; integrità dei pixel e formato colore saranno verificati in conversione. Il primo blocco non supportato viene segnalato per ciascuna foto.\nDettagli completi nel log; problemi conservati nell’archivio errori. Righe mostrate: massimo 100.":run.completed==0?"Nessuna conversione eseguita.":
            "Salvate "+run.saved+" · fallite "+run.failed+" · senza risparmio "+run.skipped+" · annullate "+run.cancelledCount+(run.alreadyDone>0?" · già presenti "+run.alreadyDone:"")
            +(run.saved+run.alreadyDone==0?"":"\n"+CompressionSummary.size(before)+" → "+CompressionSummary.size(after)+" · "+CompressionSummary.change(before,after)
            +"\n"+(before>=after?"Spazio risparmiato: ":"Spazio aggiuntivo: ")+CompressionSummary.size(Math.abs(before-after)))
            +"\nTotale sulle foto salvate e su quelle già presenti verificate, compresi i risultati usciti dalla finestra. MB = 1.000.000 byte."
            +(large?"\nRighe "+(start+1)+"–"+end+" di "+run.total+". Dettagli completi nel log; tutti i fallimenti nell’archivio errori.":""));
    }

    private void open(String uri) {
        try {
            images.activity.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), "image/heic")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (ActivityNotFoundException error) { images.toast("Non è installato un visualizzatore per questo file HEIC");
        } catch (SecurityException error) { images.toast("Accesso al file non più disponibile"); }
    }

    private void show(ImageRunState.Result result,boolean metadataOnly) {
        popup.show(metadataOnly?"Verifica metadati":"Conversione fallita",result.name,result.message,"",result.details,result.name+"\n"+result.message+"\n\n"+result.details);
    }
    void close(){popup.close();}
}
