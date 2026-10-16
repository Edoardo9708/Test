package it.edo.codecprobe;

import android.view.View;
import android.widget.TextView;

/** Clears the editable queue and finished session, without touching media or saved settings. */
final class HomeReset {
    private final HomeController home;
    HomeReset(HomeController home) { this.home = home; }

    void bind() { home.screen.files.clearSelection.setOnClickListener(v -> clear()); }

    private boolean blocked(boolean busy) {
        return busy || home.state.destroyed || home.state.hdrTesting || home.state.metadataLoading
            || home.state.choosingOrigins || home.state.clearingLogs || home.state.exportingDiagnostics
            || home.state.pendingLog != null || home.conversion.retry.pending != null;
    }

    void update(boolean busy) {
        ConversionService.State session = ConversionService.snapshot();
        boolean populated = !home.state.selectedInputs.isEmpty() || home.state.resultUri != null
            || session.hasSession || ConversionService.retryRequest(home.activity) != null;
        home.screen.files.clearSelection.setEnabled(populated && !blocked(busy) && !session.running);
    }

    void clear() {
        if(blocked(home.state.busy) || home.conversion.outputCheck.checking
            || !ConversionService.clearSessionIfIdle(home.activity)) {
            home.toast("Completa prima l’operazione in corso"); return;
        }
        home.conversion.failures.close();
        home.conversion.playback.close();
        home.conversion.savedSession.close();
        home.conversion.retry.close();
        if(home.conversion.cancelConfirmation != null)home.conversion.cancelConfirmation.dismiss();
        ++home.state.metadataGeneration;
        ++home.state.transitionGeneration;
        home.screen.files.videoPreview.animate().cancel();
        home.screen.files.videoPreview.setAlpha(1);
        VideoMetadataCache.clear();
        home.state.input = null;
        home.state.resultUri = null;
        home.state.sourceName = "video";
        home.state.selectedVideoIndex = 0;
        home.state.selectedInputs.clear();
        home.state.selectedNames.clear();
        home.state.originFolders.clear();
        home.state.destinationNotes.clear();
        home.state.originsReady = false;
        home.state.destinationPlan.reset(0, home.state.folder == null ? null : home.state.folder.toString());
        home.destinations.cancelOrigins();
        home.screen.files.videoDetails.setVisibility(View.GONE);
        home.screen.files.details.setText("▸ Dettagli video");
        home.selection.rebuildVideoCarousel();
        home.screen.files.videoCarousel.scrollTo(0, 0);
        home.selection.showCachedInput();
        home.destinations.refreshLabels();
        home.destinations.updateDestinationPanel();
        emptyDashboard();
        home.conversion.setBusy(false);
        home.screen.sections.focusCard("01  FILE E DESTINAZIONE");
        home.toast("Selezione e resoconto svuotati · file e log conservati");
    }

    private void emptyDashboard() {
        home.conversion.renderSizeReport(ConversionService.snapshot());
        home.conversion.renderConversionDashboard(ConversionService.snapshot());
        HomeConversionSection views = home.screen.conversion;
        views.sizeReport.setText("");
        views.queueLabel.setText("Nessuna coda avviata");
        views.currentFile.setText("Nessun file in conversione");
        views.currentFile.setContentDescription("Nessun file in conversione");
        views.timeline.setText("—");
        views.status.setText("Scegli video, cartella e qualità.");
        for(TextView value : new TextView[]{views.activeOnlySmaller, views.activePreset, views.activeCrf,
            views.activeCpu, views.activeBframes, views.activeLookahead, views.activeTree, views.activeAq, views.activeStrength})value.setText("—");
    }
}
