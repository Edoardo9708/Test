package it.edo.codecprobe;

import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;

/**
* View construction only; actions are bound by HomeCompression.
*/
final class HomeCompressionSection {
    CpuModeButtons cpuMode;
    QualityButtons qualityButtons;
    SeekBar presetSlider;
    SeekBar crfSlider;
    Switch bframeSwitch;
    Switch lookaheadSwitch;
    TextView presetValue;
    TextView qualityHelp;
    TextView temporalHelp;
    TextView cuTreeLabel;
    TextView aqModeLabel;
    TextView aqStrengthLabel;
    final java.util.List<Button> crfButtons = new java.util.ArrayList<>();

    HomeCompressionSection(AppUi ui, LinearLayout settings, AppSettings defaults, Bundle state) {
        presetValue = ui.parameterRow(settings, "Preset", "Il preset regola velocità e compressione. I preset lenti provano più alternative; osserva B-frame, lookahead e AQ mentre sposti lo slider.");
        presetSlider = new SeekBar(ui.activity);
        presetSlider.setMax(8);
        ui.styleSlider(presetSlider);
        settings.addView(presetSlider, new LinearLayout.LayoutParams(-1, ui.dp(48)));
        presetSlider.setProgress(state == null ? defaults.preset : state.getInt("presetValue", defaults.preset));
        qualityHelp = ui.parameterRow(settings, "CRF", "Valori da 0 a 51. Un numero più basso conserva generalmente più dettagli e produce file più grandi. CRF 0 non è lossless. Lo slider permette di scegliere ogni valore intero. I pulsanti Alta, Media e Ridotta selezionano rispettivamente CRF 18, 23 e 28. Sono indicazioni relative: la qualità percepita dipende anche dal video.");
        ui.text(settings, "Scegli la qualità manualmente", 14);
        crfSlider = new SeekBar(ui.activity);
        crfSlider.setMax(51);
        ui.styleSlider(crfSlider);
        settings.addView(crfSlider, new LinearLayout.LayoutParams(-1, ui.dp(48)));
        crfSlider.setProgress(state == null ? defaults.crf : state.getInt("crfValue", defaults.crf));
        ui.text(settings, "Scegli una qualità predefinita", 14);
        qualityButtons = new QualityButtons(ui, (n)->crfSlider.setProgress(n));
        settings.addView(qualityButtons, new LinearLayout.LayoutParams(-1, -2));
        crfButtons.addAll(qualityButtons.buttons);
        bframeSwitch = new Switch(ui.activity);
        bframeSwitch.setText("B-frame");
        bframeSwitch.setTextColor(ui.ink);
        bframeSwitch.setTextSize(20);
        bframeSwitch.setTypeface(null, Typeface.BOLD);
        bframeSwitch.setPadding(0, ui.dp(12), 0, ui.dp(12));
        ui.switchRow(settings, bframeSwitch, "B-frame", "Fotogrammi che possono usare riferimenti precedenti e successivi. Il numero visualizzato è il massimo consecutivo; x265 può usarne meno in base alla scena. OFF imposta il massimo a zero.");
        bframeSwitch.setChecked(state == null ? defaults.bframes : state.getBoolean("bframes", defaults.bframes));
        lookaheadSwitch = new Switch(ui.activity);
        lookaheadSwitch.setText("Lookahead esteso");
        lookaheadSwitch.setTextColor(ui.ink);
        lookaheadSwitch.setTextSize(20);
        lookaheadSwitch.setTypeface(null, Typeface.BOLD);
        lookaheadSwitch.setPadding(0, ui.dp(12), 0, ui.dp(12));
        ui.switchRow(settings, lookaheadSwitch, "Lookahead", "Numero di fotogrammi futuri analizzati per pianificare la codifica. ON usa il preset; OFF disattiva l’analisi estesa e CU-tree, mantenendo l’eventuale buffer minimo dei B-frame.");
        lookaheadSwitch.setChecked(state == null ? defaults.lookahead : state.getBoolean("lookahead", defaults.lookahead));
        temporalHelp = ui.text(settings, "", 13);
        temporalHelp.setVisibility(View.GONE);
        cuTreeLabel = ui.parameterRow(settings, "CU-tree", "Usa il lookahead per valutare quanto una zona verrà riutilizzata nei fotogrammi successivi e distribuire i bit. Segue il preset; viene disattivato con Lookahead OFF.");
        aqModeLabel = ui.parameterRow(settings, "AQ mode", "Adaptive Quantization: distribuisce la compressione tra le zone dello stesso fotogramma. 0: disattivata. 1: AQ basata sulla variabilità. 2: auto-variance. 3: auto-variance con attenzione alle zone scure. 4: auto-variance con informazioni sui bordi. Qui segue il preset x265.");
        aqStrengthLabel = ui.parameterRow(settings, "AQ strength", "Intensità dell’Adaptive Quantization. Segue il valore del preset; 0 disattiva AQ. Un valore maggiore non garantisce qualità migliore o file più piccoli.");
        ui.parameterRow(settings, "CPU", "Limited limita il numero di thread. Auto lascia decidere x265. Max aumenta il parallelismo in base ai core disponibili. Non garantisce il 100% di utilizzo: dipende dal video, dal preset e dai limiti del telefono.");
        cpuMode = new CpuModeButtons(ui.activity, ui.accent, ui.bg, ui.surface, ui.border);
        LinearLayout.LayoutParams cpuLayout = new LinearLayout.LayoutParams(-1, -2);
        cpuLayout.setMargins(0, ui.dp(6), 0, ui.dp(6));
        settings.addView(cpuMode, cpuLayout);
        cpuMode.setSelection(state == null ? defaults.cpu : state.getInt("cpu", defaults.cpu));
    }

    void setBusy(boolean busy) {
        presetSlider.setEnabled(!busy);
        crfSlider.setEnabled(!busy);
        cpuMode.setEnabled(!busy);
        bframeSwitch.setEnabled(!busy);
        lookaheadSwitch.setEnabled(!busy);
        for (Button b : crfButtons) b.setEnabled(!busy);
    }
}
