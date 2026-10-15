package it.edo.codecprobe;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import java.util.Locale;
import android.net.Uri;
import android.os.*;
import android.view.*;
import java.io.*;
import static it.edo.codecprobe.MainActivity.*;

/**
* Conversion commands, cancellation confirmation and service rendering.
*/
final class HomeConversion {
    private final HomeController home;
    android.app.AlertDialog cancelConfirmation;

    HomeConversion(HomeController home) {
        this.home = home;
    }
    final Runnable sessionObserver = this::renderSession;

    void bind() {
        home.screen.conversion.convert.setOnClickListener((v)->startConversion());
        home.screen.conversion.stop.setOnClickListener((v)->confirmCancellation(-1));
        home.screen.conversion.openVideo.setOnClickListener((v)->showResults());
        home.screen.conversion.keepScreenOn.setOnCheckedChangeListener((v,c)->{
            AppSettings.setKeepScreenOn(home.activity, c);
            updateScreenAwake();
        });
    }

    void showResults() {
        ConversionService.State current = ConversionService.snapshot();
        if (current.results.size() > 1) {
            String[] labels = new String[current.results.size()];
            for (int n = 0; n < labels.length; n++) labels[n] = home.displayName(current.results.get(n));
            new android.app.AlertDialog.Builder(home.activity).setTitle("Video completati").setItems(labels, (d,n)->home.openResult(current.results.get(n))).show();
        } else if (home.state.resultUri != null) home.openResult(home.state.resultUri);
    }

    void handleCancellationIntent(Intent intent) {
        if (intent == null || !CONFIRM_CANCEL.equals(intent.getAction())) return;
        long expected = intent.getLongExtra(CANCEL_SESSION, -1);
        intent.setAction(null);
        intent.removeExtra(CANCEL_SESSION);
        if (expected < 0) return;
        if (home.state.metadataLoading) home.selection.stopMetadataLoading();
        home.screen.sections.focusCard(CARD_CONVERSION);
        confirmCancellation(expected);
    }

    void confirmCancellation(long expectedSession) {
        if (home.state.destroyed || cancelConfirmation != null && cancelConfirmation.isShowing()) return;
        final boolean hdr = home.state.hdrTesting;
        final ConversionService.State current = ConversionService.snapshot();
        if (expectedSession >= 0 && (!current.running || current.startedAt != expectedSession)) {
            home.toast("Questa richiesta non riguarda più una conversione attiva.");
            return;
        }
        if (!hdr && !current.running) return;
        final long session = current.startedAt;
        cancelConfirmation = new android.app.AlertDialog.Builder(home.activity).setTitle(hdr ? "Annullare la prova HDR?" : "Annullare la conversione?").setMessage(hdr ? "Vuoi interrompere la prova HDR in corso?" : "Verranno interrotti il file attuale e i video ancora in attesa. I video già completati restano salvati.\n\nLa conversione continua finché non confermi.").setNegativeButton("Continua", null).setPositiveButton("Sì, annulla", (dialog,which)->{
            if (hdr) {
                if (!home.state.hdrTesting) return;
                home.state.cancelled.set(true);
                home.screen.conversion.status.setText("Annullamento prova HDR…");
            } else {
                ConversionService.State active = ConversionService.snapshot();
                if (!active.running || active.startedAt != session) {
                    home.toast("La conversione è già terminata.");
                    return;
                }
                home.activity.startService(new Intent(home.activity, ConversionService.class).setAction(ConversionService.CANCEL).putExtra(CANCEL_SESSION, session));
                home.screen.conversion.status.setText("Annullamento della coda…");
            }
            FolderPickerActivity.record(home.activity, "*** ANNULLAMENTO CONFERMATO ***\nTipo: " + (hdr ? "prova HDR" : "coda conversione"));
            home.screen.conversion.stop.setEnabled(false);
        }).create();
        cancelConfirmation.setOnDismissListener((dialog)->cancelConfirmation = null);
        cancelConfirmation.show();
    }

    void setBusy(boolean value) {
        home.state.busy = value;
        updateScreenAwake();
        if (home.destinations.originPanel != null) home.destinations.originPanel.setBusy(value || home.state.choosingOrigins);
        boolean hasInputs = !home.state.selectedInputs.isEmpty();
        home.screen.files.setBusy(value, home.state.choosingOrigins, home.state.metadataLoading, hasInputs);
        home.screen.destinations.setBusy(value, home.state.choosingOrigins, hasInputs, home.state.destinationPlan.applyAll);
        home.screen.compression.setBusy(value);
        home.screen.conversion.convert.setEnabled(!value && !home.state.clearingLogs && !home.state.choosingOrigins && home.state.originsReady && hasInputs);
        home.screen.conversion.stop.setEnabled(value);
        home.diagnostics.updateClearLogsButton();
    }

    void updateScreenAwake() {
        boolean enabled = home.state.conversionScreenVisible && home.state.busy && home.screen.conversion.keepScreenOn != null && home.screen.conversion.keepScreenOn.isChecked();
        if (enabled) home.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); else home.activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    void startConversion() {
        if (home.state.clearingLogs) {
            home.toast("Attendi la cancellazione dei log");
            return;
        }
        if (home.state.input == null) {
            home.toast("Seleziona uno o più video");
            return;
        }
        if (!OriginalMedia.hasLocationPermission(home.activity)) {
            new android.app.AlertDialog.Builder(home.activity).setTitle("Conserva la posizione del video").setMessage("Android può nascondere le coordinate salvate nei video. Il permesso serve a leggerle e copiarle: non rileva la tua posizione attuale. Senza il permesso la conversione non parte, per evitare di perdere il GPS.").setPositiveButton("Continua", (d,w)->home.activity.requestPermissions(new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION}, 5)).setNegativeButton("Annulla", null).show();
            return;
        }
        if (!OriginalMedia.hasVideoPermission(home.activity)) {
            new android.app.AlertDialog.Builder(home.activity).setTitle("Accesso al video originale").setMessage("Per aprire il video originale senza oscurare il GPS, Android richiede anche l’accesso ai video. Se scegli solo alcuni video, includi quello che vuoi convertire. Il permesso per la posizione, già concesso, non basta per leggere il file tramite MediaStore.").setPositiveButton("Continua", (d,w)->home.activity.requestPermissions(OriginalMedia.videoPermissions(), 6)).setNegativeButton("Annulla", null).show();
            return;
        }
        if (home.state.choosingOrigins) {
            home.toast("Completa prima la scelta delle cartelle di origine");
            return;
        }
        if (!home.state.originsReady || home.state.originFolders.size() != home.state.selectedInputs.size() || home.state.originFolders.contains(null)) {
            home.toast("Completa le scelte e autorizza tutte le cartelle rosse prima di avviare la coda");
            return;
        }
        java.util.ArrayList<String> destinations = new java.util.ArrayList<>();
        destinations.addAll(home.state.originFolders);
        final int crf = home.compression.selectedCrf();
        final int preset = home.screen.compression.presetSlider.getProgress();
        final int cpu = home.screen.compression.cpuMode.getSelectedItemPosition();
        if (Build.VERSION.SDK_INT >= 33 && !home.state.notificationsRequested && home.activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            home.state.notificationsRequested = true;
            home.activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 4);
            return;
        }
        Intent task = new Intent(home.activity, ConversionService.class).setAction(ConversionService.START).putStringArrayListExtra("inputs", new java.util.ArrayList<>(home.state.selectedInputs)).putStringArrayListExtra("names", new java.util.ArrayList<>(home.state.selectedNames)).putStringArrayListExtra("folders", destinations).putStringArrayListExtra("destinationNotes", new java.util.ArrayList<>(home.state.destinationNotes)).putExtra("individualOrigins", home.state.destinationPlan.individualOrigins()).putStringArrayListExtra("individualFolders", home.state.destinationPlan.individualFolders()).putExtra("applyAllDestinations", home.state.destinationPlan.applyAll).putExtra("allOrigin", home.state.destinationPlan.all.origin).putExtra("allFolder", home.state.destinationPlan.all.folder).putExtra("crf", crf).putExtra("preset", preset).putExtra("cpu", cpu).putExtra("bframes", home.screen.compression.bframeSwitch.isChecked()).putExtra("lookahead", home.screen.compression.lookaheadSwitch.isChecked());
        try {
            home.activity.startForegroundService(task);
            home.screen.sections.focusCard(CARD_CONVERSION);
            setBusy(true);
            home.screen.conversion.status.setText("Avvio conversione…");
        } catch (RuntimeException e) {
            setBusy(false);
            home.screen.conversion.status.setText("Avvio non riuscito: " + e.getMessage());
        }
    }

    void renderSession() {
        if (home.state.destroyed || home.state.hdrTesting) return;
        ConversionService.State s = ConversionService.snapshot();
        if (!s.hasSession) return;
        renderSizeReport(s);
        renderConversionDashboard(s);
        home.state.resultUri = s.result;
        home.screen.conversion.openVideo.setVisibility(home.state.resultUri == null ? View.GONE : View.VISIBLE);
        if (home.state.input == null) {
            home.state.selectedInputs.addAll(s.inputs);
            home.state.selectedNames.addAll(s.names);
            home.state.input = Uri.parse(home.state.selectedInputs.get(0));
            home.state.folder = s.folder;
            home.state.destinationPlan.reset(home.state.selectedInputs.size(), null);
            home.state.destinationPlan.restore(s.individualOrigins, s.individualFolders, s.applyAllDestinations, s.allOrigin, s.allFolder);
            home.state.sourceName = home.state.selectedNames.get(0);
            home.state.selectedVideoIndex = 0;
            home.destinations.updateDestinationPanel();
            home.destinations.refreshLabels();
            home.selection.inspectInput();
        }
    }

    void renderSizeReport(ConversionService.State s) {
        boolean show = !s.sizeReport.isEmpty();
        home.screen.conversion.sizeReportPanel.setVisibility(show ? View.VISIBLE : View.GONE);
        home.screen.conversion.sizeReportTitle.setText(s.running ? "RESOCONTO PARZIALE" : "RESOCONTO CONVERSIONE");
        if (show && !s.sizeReport.contentEquals(home.screen.conversion.sizeReport.getText())) home.screen.conversion.sizeReport.setText(s.sizeReport);
    }

    void renderConversionDashboard(ConversionService.State s) {
        int pending = Math.max(0, s.batchTotal - (s.running ? Math.max(s.batchIndex, s.succeeded + s.failed) : s.succeeded + s.failed));
        home.screen.conversion.queueLabel.setText("Video " + s.batchIndex + " / " + s.batchTotal + "\nRiusciti: " + s.succeeded + " · Falliti: " + s.failed + "\n" + (s.running ? "In attesa: " : "Non completati: ") + pending);
        home.screen.conversion.elapsedLabel.setText(DashboardMetrics.elapsed(s.startedAt, s.endedAt, SystemClock.elapsedRealtime()));
        home.screen.conversion.currentFile.setText(s.source.isEmpty() ? "In attesa del file" : s.source);
        home.screen.conversion.currentFile.setContentDescription("File attuale: " + s.source);
        home.screen.conversion.frameLabel.setText(s.doneFrames + " / " + (s.totalFrames > 0 ? s.totalFrames : "—"));
        setBusy(s.running);
        home.screen.conversion.progress.setProgress(s.filePercent);
        home.screen.conversion.percentLabel.setText(s.filePercent + "%");
        home.screen.conversion.status.setText(s.message);
        home.screen.diagnostics.logView.setText(s.log);
        home.screen.conversion.timeline.setText(String.format(Locale.ITALY, "%.1f s convertiti\n%s", s.done / 1000000.0, s.total > 0 ? String.format(Locale.ITALY, "%.1f s rimanenti", Math.max(0, s.total - s.done) / 1000000.0) : "durata totale non disponibile"));
        home.screen.conversion.activePreset.setText(s.requestedPreset);
        home.screen.conversion.activeCrf.setText(s.requestedCrf >= 0 ? Integer.toString(s.requestedCrf) : "—");
        String[] cpuModes = {"Limited", "Auto", "Max"};
        home.screen.conversion.activeCpu.setText(s.requestedCpu >= 0 && s.requestedCpu < cpuModes.length ? cpuModes[s.requestedCpu] : "—");
        boolean opened = s.bframeCount >= 0;
        home.screen.conversion.activeBframes.setText(opened ? Integer.toString(s.bframeCount) : "in attesa");
        home.screen.conversion.activeLookahead.setText(opened ? Integer.toString(s.lookaheadCount) : "in attesa");
        home.screen.conversion.activeTree.setText(opened ? (s.cuTree != 0 ? "ON" : "OFF") : "in attesa");
        home.screen.conversion.activeAq.setText(opened ? Integer.toString(s.aqMode) : "in attesa");
        home.screen.conversion.activeStrength.setText(opened ? String.format(Locale.ITALY, "%.2f", s.aqStrength / 1000.0) : "in attesa");
    }
}
