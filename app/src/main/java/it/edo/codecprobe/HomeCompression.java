package it.edo.codecprobe;

import java.util.Locale;
import android.os.*;
import android.view.*;
import android.widget.*;

/**
* Compression controls and effective preset preview.
*/
final class HomeCompression {
    private final HomeController home;

    HomeCompression(HomeController home) {
        this.home = home;
    }

    void bind() {
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener(){

            public void onProgressChanged(SeekBar bar, int value, boolean user) {
                updateQualityHelp();
                updateTemporalHelp();
            }

            public void onStartTrackingTouch(SeekBar bar) {
            }

            public void onStopTrackingTouch(SeekBar bar) {
            }
        };
        home.screen.compression.crfSlider.setOnSeekBarChangeListener(listener);
        home.screen.compression.presetSlider.setOnSeekBarChangeListener(listener);
        home.screen.compression.bframeSwitch.setOnCheckedChangeListener((v,c)->updateTemporalHelp());
        home.screen.compression.lookaheadSwitch.setOnCheckedChangeListener((v,c)->updateTemporalHelp());
        updateQualityHelp();
        updateTemporalHelp();
    }

    void save(Bundle out) {
        out.putInt("crfValue", selectedCrf());
        out.putInt("presetValue", home.screen.compression.presetSlider.getProgress());
        out.putInt("cpu", home.screen.compression.cpuMode.getSelectedItemPosition());
        out.putBoolean("bframes", home.screen.compression.bframeSwitch.isChecked());
        out.putBoolean("lookahead", home.screen.compression.lookaheadSwitch.isChecked());
    }

    void updateTemporalHelp() {
        if (home.screen.compression.temporalHelp == null) return;
        home.screen.compression.presetValue.setText("Preset · " + AppSettings.PRESETS[home.screen.compression.presetSlider.getProgress()]);
        try {
            int[] values = EncoderPresetPreview.read(home.screen.compression.presetSlider.getProgress(), home.screen.compression.bframeSwitch.isChecked(), home.screen.compression.lookaheadSwitch.isChecked());
            home.screen.compression.bframeSwitch.setText("B-frame · " + values[0]);
            home.screen.compression.lookaheadSwitch.setText("Lookahead · " + values[1]);
            home.screen.compression.cuTreeLabel.setText("CU-tree · " + (values[2] != 0 ? "ON" : "OFF"));
            home.screen.compression.aqModeLabel.setText("AQ mode · " + values[3]);
            home.screen.compression.aqStrengthLabel.setText(String.format(Locale.ITALY, "AQ strength · %.2f", values[4] / 1000.0));
            home.screen.compression.temporalHelp.setVisibility(View.GONE);
        } catch (RuntimeException | LinkageError e) {
            home.screen.compression.temporalHelp.setVisibility(View.VISIBLE);
            home.screen.compression.temporalHelp.setText("Parametri x265 non disponibili: " + e.getMessage());
        }
    }

    int selectedCrf() {
        return home.screen.compression.crfSlider.getProgress();
    }

    void updateQualityHelp() {
        if (home.screen.compression.qualityHelp == null) return;
        int n = selectedCrf();
        home.screen.compression.qualityButtons.selectFromCrf(n);
        home.screen.compression.qualityHelp.setText(n < 0 || n > 51 ? "Inserisci un numero intero da 0 a 51." : "CRF · " + n);
    }

    void applyDefaults(AppSettings defaults) {
        home.screen.compression.presetSlider.setProgress(defaults.preset);
        home.screen.compression.crfSlider.setProgress(defaults.crf);
        home.screen.compression.cpuMode.setSelection(defaults.cpu);
        home.screen.compression.bframeSwitch.setChecked(defaults.bframes);
        home.screen.compression.lookaheadSwitch.setChecked(defaults.lookahead);
        updateTemporalHelp();
        updateQualityHelp();
        home.state.settingsRevision = defaults.revision;
    }
}
