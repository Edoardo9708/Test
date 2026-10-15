package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;

/** Pause/resume commands and confirmation tied to one session and one file. */
final class HomePlaybackControls {
    private final HomeController home;
    AlertDialog confirmation;
    boolean paused;

    HomePlaybackControls(HomeController home) { this.home = home; }

    void bind() {
        home.screen.conversion.pause.setOnClickListener(v -> {
            ConversionService.State state = ConversionService.snapshot();
            if (!state.running || state.cancelling || state.skipping) return;
            send(state.pauseRequested ? ConversionService.RESUME : ConversionService.PAUSE, state);
        });
        home.screen.conversion.skip.setOnClickListener(v -> confirmSkip());
    }

    void render(ConversionService.State state) {
        paused = state.paused;
        home.screen.conversion.playbackActions.setVisibility(state.running ? View.VISIBLE : View.GONE);
        home.screen.conversion.pause.setText(state.pauseRequested ? "Riprendi" : "Pausa");
        home.screen.conversion.pause.setEnabled(state.running && !state.cancelling && !state.skipping);
        home.screen.conversion.skip.setEnabled(state.running && state.fileActive && !state.cancelling && !state.skipping);
    }

    void confirmSkip() {
        ConversionService.State state = ConversionService.snapshot();
        if (!state.running || !state.fileActive || state.cancelling || state.skipping || confirmation != null && confirmation.isShowing()) return;
        confirmation = new AlertDialog.Builder(home.activity).setTitle("Saltare questo video?")
            .setMessage("Video " + state.batchIndex + " · " + state.source
                + "\n\nElimino i temporanei di questo file e l’eventuale risultato ancora incompleto. L’originale resta intatto. Poi passo al video successivo."
                + (state.pauseRequested ? "\n\nLa coda resterà in pausa anche sul prossimo video." : ""))
            .setNegativeButton("Continua", null).setPositiveButton("Salta video", (d,w) -> {
                ConversionService.State current = ConversionService.snapshot();
                if (!current.running || current.startedAt != state.startedAt || current.batchIndex != state.batchIndex || !current.fileActive) {
                    home.toast("Questo video è già terminato.");return;
                }
                send(ConversionService.SKIP, state);
            }).create();
        confirmation.setOnDismissListener(d -> confirmation = null);
        confirmation.show();
    }

    private void send(String action, ConversionService.State state) {
        home.activity.startService(new Intent(home.activity, ConversionService.class).setAction(action)
            .putExtra(MainActivity.CANCEL_SESSION, state.startedAt).putExtra("fileNumber", state.batchIndex));
    }

    void close() { if (confirmation != null) { confirmation.dismiss(); confirmation = null; } }
}
