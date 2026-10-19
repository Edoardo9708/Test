package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.widget.*;
import java.util.*;

/** Photo results adapted to the shared work report, without controlling conversion. */
final class ImageWorkReport {
    private final ImageController images;
    private List<ImageRunState.Result> rendered;
    private AlertDialog dialog;

    ImageWorkReport(ImageController images) { this.images = images; }

    void render(ImageRunState run) {
        WorkReportView view = images.screen.workReport;
        view.title.setText(run.running ? "REPORT LAVORI · IN CORSO" : "REPORT LAVORI");
        if (rendered != null && rendered.equals(run.results)) return;
        rendered = new ArrayList<>(run.results);
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
            LinearLayout row = WorkReportView.row(ui, view.rows);
            TextView body = WorkReportView.body(ui, row, title + "\n" + status);
            body.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout actions = new LinearLayout(ui.activity);
            actions.setOrientation(LinearLayout.VERTICAL);
            row.addView(actions);
            if (result.outcome == ImageRunState.Outcome.SAVED && result.uri != null)
                WorkReportView.action(ui, actions, "Apri", "Apri risultato: " + title, () -> open(result.uri));
            WorkReportView.action(ui, actions, result.outcome == ImageRunState.Outcome.FAILED ? "Errore" : "Dettagli",
                "Mostra dettagli: " + title, () -> show(result));
        }
        view.summary.setText(run.results.isEmpty() ? "Nessuna conversione eseguita." :
            "Salvate " + saved + " · fallite " + failed + " · senza risparmio " + skipped + " · annullate " + cancelled
            + (saved == 0 ? "" : "\n" + CompressionSummary.size(before) + " → " + CompressionSummary.size(after)
            + " · " + CompressionSummary.change(before, after)
            + "\n" + (before >= after ? "Spazio risparmiato: " : "Spazio aggiuntivo: ") + CompressionSummary.size(Math.abs(before - after)))
            + "\nTotale calcolato sui file salvati, inclusi i metadati. MB = 1.000.000 byte.");
    }

    private void open(String uri) {
        try {
            images.activity.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), "image/heic")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (ActivityNotFoundException error) { images.toast("Non è installato un visualizzatore per questo file HEIC");
        } catch (SecurityException error) { images.toast("Accesso al file non più disponibile"); }
    }

    private void show(ImageRunState.Result result) {
        close();
        String text = result.name + "\n" + result.message + "\n\n" + result.details;
        TextView body = WorkReportView.body(images.screen.ui, new LinearLayout(images.activity), text);
        body.setPadding(images.screen.ui.dp(20), images.screen.ui.dp(12), images.screen.ui.dp(20), images.screen.ui.dp(12));
        ((LinearLayout) body.getParent()).removeView(body);
        ScrollView scroll = new ScrollView(images.activity); scroll.addView(body);
        dialog = new AlertDialog.Builder(images.activity).setTitle("Esito immagine").setView(scroll)
            .setNegativeButton("Chiudi", null).setPositiveButton("Copia tutto", null).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            images.activity.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Esito immagine", text));
            images.toast("Dettagli copiati");
        });
    }

    void close() { if (dialog != null) { dialog.dismiss(); dialog = null; } }
}
