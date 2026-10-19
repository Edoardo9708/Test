package it.edo.codecprobe;

import android.content.*;
import android.net.Uri;
import java.util.*;

/** Photo results adapted to the shared work report, without controlling conversion. */
final class ImageWorkReport {
    private final ImageController images;
    private List<ImageRunState.Result> rendered;
    private boolean renderedRunning;
    private final ResultDialog popup;

    ImageWorkReport(ImageController images) { this.images = images;popup=new ResultDialog(images.screen.ui,images::toast); }

    void render(ImageRunState run) {
        WorkReportView view = images.screen.workReport;
        view.title.setText(run.running ? "REPORT LAVORI · IN CORSO" : "REPORT LAVORI");
        view.panel.setVisibility(run.results.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);view.summary.setVisibility(android.view.View.GONE);
        if (rendered != null && rendered.equals(run.results)&&renderedRunning==run.running) return;
        rendered = new ArrayList<>(run.results);renderedRunning=run.running;
        view.rows.removeAllViews();
        AppUi ui = images.screen.ui;
        long before = 0, after = 0;
        int saved = 0, failed = 0, skipped = 0, cancelled = 0;
        for (int n = 0; n < run.results.size(); n++) {
            ImageRunState.Result result = run.results.get(n);
            String status;
            switch (result.outcome) {
                case SAVED:
                    saved++; before += result.inputBytes; after += result.outputBytes;
                    status = CompressionSummary.size(result.inputBytes) + " → " + CompressionSummary.size(result.outputBytes)
                        + " · " + CompressionSummary.change(result.inputBytes, result.outputBytes);
                    break;
                case FAILED: failed++; status = "Conversione fallita · nessun nuovo file completato"; break;
                case NOT_SMALLER: skipped++; status = "Nessun risparmio · risultato non salvato"; break;
                default: cancelled++; status = "Conversione annullata";
            }
            String title = "Foto " + (n + 1) + " · " + result.name.replace('\n', ' ').replace('\r', ' ');
            ResultReportRow.add(ui,view.rows,title+"\n"+status,
                result.outcome==ImageRunState.Outcome.SAVED&&result.uri!=null?()->open(result.uri):null,
                result.outcome==ImageRunState.Outcome.FAILED?()->show(result):null);
        }
        view.summary.setText(run.results.isEmpty() ? "Nessuna conversione eseguita." :
            "Salvate " + saved + " · fallite " + failed + " · senza risparmio " + skipped + " · annullate " + cancelled
            + (saved == 0 ? "" : "\n" + CompressionSummary.size(before) + " → " + CompressionSummary.size(after)
            + " · " + CompressionSummary.change(before, after)
            + "\n" + (before >= after ? "Spazio risparmiato: " : "Spazio aggiuntivo: ") + CompressionSummary.size(Math.abs(before - after)))
            + "\nTotale calcolato sui file salvati, inclusi i metadati. MB = 1.000.000 byte.");
        if(!run.results.isEmpty())ResultReportRow.add(ui,view.rows,(run.running?"TOTALE PARZIALE · FOTO RIUSCITE\n":"TOTALE · FOTO RIUSCITE\n")+view.summary.getText(),null,null);
    }

    private void open(String uri) {
        try {
            images.activity.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), "image/heic")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (ActivityNotFoundException error) { images.toast("Non è installato un visualizzatore per questo file HEIC");
        } catch (SecurityException error) { images.toast("Accesso al file non più disponibile"); }
    }

    private void show(ImageRunState.Result result) {
        popup.show("Conversione fallita",result.name,result.message,"",result.details,result.name+"\n"+result.message+"\n\n"+result.details);
    }
    void close(){popup.close();}
}
