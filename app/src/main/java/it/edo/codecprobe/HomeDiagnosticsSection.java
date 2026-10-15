package it.edo.codecprobe;

import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;

/**
* View construction only; actions are bound by HomeDiagnostics.
*/
final class HomeDiagnosticsSection {
    Button saveLog;
    Button saveMetadata;
    Button hdrTest;
    Button saveHdr;
    Button clearLogs;
    Button toggle;
    TextView logView;

    HomeDiagnosticsSection(AppUi ui, LinearLayout logs, AppSettings defaults, Bundle state) {
        LinearLayout exports = ui.row(logs);
        saveLog = ui.halfButton(exports, "Esporta log · TXT", false);
        saveMetadata = ui.halfButton(exports, "Metadati · TXT", false);
        ui.sectionHeading(logs, "PROVE HDR");
        LinearLayout hdr = ui.row(logs);
        hdrTest = ui.halfButton(hdr, "Test HDR · 10 bit", false);
        saveHdr = ui.halfButton(hdr, "Log HDR · TXT", false);
        ui.text(logs, "La prova HDR analizza sei fotogrammi del video visualizzato.", 13);
        ui.divider(logs);
        clearLogs = ui.button(logs, "Cancella tutti i log");
        ui.danger(clearLogs);
        ui.text(logs, "Cancella i log nell’app. I TXT già esportati restano salvati.", 13);
        toggle = ui.button(logs, "Mostra ultimi messaggi");
        logView = ui.text(logs, "", 12);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setTextIsSelectable(true);
        logView.setMaxLines(16);
        logView.setVisibility(View.GONE);
    }

    void setBusy(boolean busy, boolean clearing, boolean exporting) {
        hdrTest.setEnabled(!busy && !clearing && !exporting);
        saveHdr.setEnabled(!busy && !clearing && !exporting);
        saveLog.setEnabled(!busy && !clearing && !exporting);
        saveMetadata.setEnabled(!busy && !clearing && !exporting);
    }
}
