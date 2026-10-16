package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.view.View;
import android.widget.*;
import java.util.List;

/** Per-file failure actions and readable, copyable error dialog. No log scanning. */
final class HomeFailureReport {
    private final HomeController home;
    private String rendered = "";
    private List<ConversionFailure> renderedFailures = java.util.Collections.emptyList();
    private List<String> renderedOutputs = java.util.Collections.emptyList();
    AlertDialog dialog;

    HomeFailureReport(HomeController home) { this.home = home; }

    void render(ConversionService.State state) {
        HomeConversionSection views = home.screen.conversion;
        boolean structured = !state.failures.isEmpty() || !state.results.isEmpty();
        views.sizeReport.setVisibility(structured ? View.GONE : View.VISIBLE);
        views.failureReport.setVisibility(structured ? View.VISIBLE : View.GONE);
        if (!structured) { views.failureReport.removeAllViews(); rendered = ""; renderedFailures = java.util.Collections.emptyList(); renderedOutputs=java.util.Collections.emptyList(); return; }
        if (rendered.equals(state.sizeReport) && renderedFailures.equals(state.failures) && renderedOutputs.equals(state.outputUris)) return;
        rendered = state.sizeReport;
        renderedFailures = new java.util.ArrayList<>(state.failures);
        renderedOutputs = new java.util.ArrayList<>(state.outputUris);
        views.failureReport.removeAllViews();
        for (String paragraph : state.sizeReport.split("\n\n")) {
            ConversionFailure found = null;
            for (ConversionFailure failure : state.failures)
                if (paragraph.startsWith(failure.title() + "\nConversione fallita")) { found = failure; break; }
            LinearLayout row = home.ui.row(views.failureReport);
            row.setPadding(0, home.ui.dp(4), 0, home.ui.dp(8));
            TextView body = home.ui.text(row, paragraph, 16);
            body.setTextIsSelectable(true);
            body.setLineSpacing(home.ui.dp(4), 1);
            body.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            for(int n=0;n<state.items.size()&&n<state.outputUris.size();n++){
                CompressionSummary.Item item=state.items.get(n);
                String title="Video "+item.number+" · "+item.name.replace('\n',' ').replace('\r',' ');
                if(item.status==CompressionSummary.Status.COMPLETE&&!state.outputUris.get(n).isEmpty()&&paragraph.startsWith(title+"\n")){
                    final android.net.Uri output=android.net.Uri.parse(state.outputUris.get(n));
                    Button open=home.ui.button(row,"Apri");
                    open.setLayoutParams(new LinearLayout.LayoutParams(home.ui.dp(88),-2));
                    open.setContentDescription("Apri risultato: "+title);
                    open.setOnClickListener(v->home.openResult(output));break;
                }
            }
            if (found != null) {
                final ConversionFailure failure = found;
                Button button = new Button(home.activity);
                home.ui.styleButton(button, false);
                home.ui.danger(button);
                button.setText("!");
                button.setTextSize(22);
                button.setPadding(0, 0, 0, 0);
                button.setMinHeight(home.ui.dp(48));button.setMinimumHeight(home.ui.dp(48));
                button.setContentDescription("Mostra errore: " + failure.title());
                row.addView(button, new LinearLayout.LayoutParams(home.ui.dp(48), home.ui.dp(48)));
                button.setOnClickListener(v -> show(failure));
            }
        }
    }

    void show(ConversionFailure failure) {
        close();
        AppUi ui = home.ui;
        LinearLayout content = new LinearLayout(home.activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(12));
        TextView name = ui.text(content, failure.title(), 17);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        ui.text(content, "MESSAGGIO", 12);
        TextView message = ui.text(content, failure.message, 16);
        message.setTextIsSelectable(true);
        ui.text(content, "ULTIMO STATO COMUNICATO", 12);
        ui.text(content, failure.lastState, 15);
        Button toggle = ui.button(content, "Mostra dettagli tecnici ▾");
        TextView details = ui.text(content, failure.details, 13);
        details.setTextColor(ui.ink);
        details.setTypeface(android.graphics.Typeface.MONOSPACE);
        details.setTextIsSelectable(true);
        details.setVisibility(View.GONE);
        toggle.setOnClickListener(v -> {
            boolean open = details.getVisibility() != View.VISIBLE;
            details.setVisibility(open ? View.VISIBLE : View.GONE);
            toggle.setText(open ? "Nascondi dettagli tecnici ▴" : "Mostra dettagli tecnici ▾");
        });
        ScrollView scroll = new ScrollView(home.activity);
        scroll.addView(content);
        dialog = new AlertDialog.Builder(home.activity).setTitle("Conversione fallita")
            .setView(scroll).setNegativeButton("Chiudi", null).setPositiveButton("Copia tutto", null).create();
        dialog.show();
        // Keep the popup open after copying; technical details are copied even while collapsed.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            ClipboardManager clipboard = home.activity.getSystemService(ClipboardManager.class);
            clipboard.setPrimaryClip(ClipData.newPlainText("Errore conversione", failure.copyText()));
            home.toast("Errore e dettagli copiati");
        });
    }

    void close() {
        if (dialog != null) { dialog.dismiss(); dialog = null; }
    }
}
