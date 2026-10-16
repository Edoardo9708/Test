package it.edo.codecprobe;

import android.content.pm.PackageManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static it.edo.codecprobe.MainActivity.*;
import static it.edo.codecprobe.HomeRequests.*;

/**
* Lifecycle coordinator only. Each section/action has one owner.
*/
final class HomeController {
    final MainActivity activity;
    final HomeState state = new HomeState();
    final Handler handler = new Handler(Looper.getMainLooper());
    final AppUi ui;
    final HomeScreen screen;
    final HomeSelection selection;
    final HomeDestinations destinations;
    final HomeCompression compression;
    final HomeConversion conversion;
    final HomeDiagnostics diagnostics;
    final HomeSystemMonitor system;
    final HomeReset reset;

    HomeController(MainActivity activity, Bundle saved) {
        this.activity = activity;
        AppSettings defaults = new AppSettings(activity);
        state.settingsRevision = defaults.revision;
        ui = new AppUi(activity);
        screen = new HomeScreen(ui, defaults, saved, ConversionService.snapshot().running);
        selection = new HomeSelection(this);
        destinations = new HomeDestinations(this);
        compression = new HomeCompression(this);
        conversion = new HomeConversion(this);
        diagnostics = new HomeDiagnostics(this);
        system = new HomeSystemMonitor(this);
        reset = new HomeReset(this);
        selection.bind();
        destinations.bind();
        compression.bind();
        conversion.bind();
        diagnostics.bind();
        reset.bind();
        screen.preferences.setOnClickListener((v)->openSettings());
        HomeSavedState.restore(this, saved, defaults);
        if (saved != null && saved.getBoolean("videoDetailsExpanded")) {
            screen.files.videoDetails.setVisibility(View.VISIBLE);
            screen.files.details.setText("▾ Dettagli video");
        }
        if (saved != null && saved.getBoolean("logExpanded")) {
            screen.diagnostics.logView.setVisibility(View.VISIBLE);
            screen.diagnostics.toggle.setText("Nascondi ultimi messaggi");
        }
        destinations.updateDestinationPanel();
        destinations.refreshLabels();
        selection.inspectInput();
        if (state.resultUri != null) screen.conversion.openVideo.setVisibility(View.VISIBLE);
        restoreLogTail();
        if (CONFIRM_CANCEL.equals(activity.getIntent().getAction())) handler.post(()->conversion.handleCancellationIntent(activity.getIntent()));
    }

    void openSettings() {
        activity.startActivity(new Intent(activity, SettingsActivity.class).putExtra("preset", screen.compression.presetSlider.getProgress()).putExtra("crf", compression.selectedCrf()).putExtra("cpu", screen.compression.cpuMode.getSelectedItemPosition()).putExtra("bframes", screen.compression.bframeSwitch.isChecked()).putExtra("lookahead", screen.compression.lookaheadSwitch.isChecked()).putExtra("sourceName", state.sourceName));
    }

    void onActivityResult(int request, int code, Intent data) {
        if (request == PICK_FOLDER || request == PICK_ALL_FOLDER || request == PICK_ORIGIN) destinations.onFolderResult(request, code, data); else if (request == PICK_VIDEO) selection.onVideoResult(request, code, data); else if (request == SAVE_LOG) diagnostics.onLogResult(request, code, data);
    }

    void onPermissions(int request, String[] permissions, int[] results) {
        if(request==HomeRetry.PERMISSION){conversion.retry.onPermissions(results);return;}
        final HomeController home = this;
        if (request == 9) {
            home.destinations.updateDestinationPanel();
            home.destinations.originPanel.refresh();
            if (!OriginalMedia.hasVideoPermission(home.activity)) home.toast("Android può nascondere i percorsi: usa Autorizza per associare le cartelle manualmente.");
            return;
        }
        if (request == 8) {
            if (OriginalMedia.hasLocationPermission(home.activity) && OriginalMedia.hasVideoPermission(home.activity)) home.diagnostics.exportMetadata(); else home.toast("Per esportare i metadati originali, consenti posizione dei file e lettura del video selezionato.");
        }
        if (request == 5) {
            if (OriginalMedia.hasLocationPermission(home.activity)) {
                home.selection.inspectInput();
                home.conversion.startConversion();
            } else {
                home.screen.conversion.status.setText("Conversione non avviata: permesso posizione dei file negato.");
                home.toast("Per conservare il GPS, abilita il permesso nelle impostazioni dell’app e riprova.");
            }
        }
        if (request == 6) {
            if (OriginalMedia.hasVideoPermission(home.activity)) {
                home.selection.inspectInput();
                home.conversion.startConversion();
            } else {
                home.screen.conversion.status.setText("Conversione non avviata: accesso ai video negato.");
                home.toast("Consenti la lettura del video originale nelle impostazioni dell’app.");
            }
        }
        if (request == 4) {
            if (results.length == 0 || results[0] != PackageManager.PERMISSION_GRANTED) home.toast("Notifiche disattivate: puoi abilitarle nelle impostazioni Android.");
            home.conversion.startConversion();
        }
    }

    void restoreLogTail() {
        final HomeController home = this;
        File old = ConversionService.logFile(home.activity);
        if (old.isFile()) home.diagnostics.worker.execute(()->{
            try (final RandomAccessFile f = new RandomAccessFile(old, "r");) {
                int size = (int)Math.min(6000, f.length());
                byte[] bytes = new byte[size];
                f.seek(f.length() - size);
                f.readFully(bytes);
                String tail = new String(bytes, StandardCharsets.UTF_8);
                home.activity.runOnUiThread(()->{
                    if (!home.state.destroyed && !home.state.busy) home.screen.diagnostics.logView.setText(tail);
                });
            } catch (IOException ignored) {
            }
        });
    }

    void resume() {
        final HomeController home = this;
        home.state.conversionScreenVisible = true;
        AppSettings saved = new AppSettings(home.activity);
        home.screen.conversion.keepScreenOn.setChecked(saved.keepScreenOn);
        if (!java.util.Objects.equals(home.state.loadedDefault, saved.defaultFolder) && !ConversionService.snapshot().running) {
            home.state.loadedDefault = saved.defaultFolder;
            if (home.state.loadedDefault != null) {
                home.state.folder = Uri.parse(home.state.loadedDefault);
            }
            home.destinations.refreshLabels();
        }
        if (saved.revision != home.state.settingsRevision && !ConversionService.snapshot().running) home.compression.applyDefaults(saved);
        ConversionService.uiVisible = true;
        home.system.previousWall = 0;
        home.handler.post(home.system.sample);
        ConversionService.observe(home.conversion.sessionObserver);
        home.conversion.renderSession();
        if (home.destinations.originPanel != null && !ConversionService.snapshot().running) home.destinations.originPanel.refresh();
        home.conversion.updateScreenAwake();
    }

    void pause() {
        final HomeController home = this;
        home.state.conversionScreenVisible = false;
        home.conversion.updateScreenAwake();
        ConversionService.uiVisible = false;
        home.handler.removeCallbacks(home.system.sample);
        ConversionService.detach(home.conversion.sessionObserver);
    }

    void destroy() {
        final HomeController home = this;
        home.state.cancelled.set(true);
        home.state.conversionScreenVisible = false;
        home.conversion.updateScreenAwake();
        home.state.destroyed = true;
        home.conversion.outputCheck.close();
        home.conversion.retry.close();
        home.conversion.failures.close();
        home.conversion.playback.close();
        if(home.diagnostics.logFormatDialog!=null)home.diagnostics.logFormatDialog.dismiss();
        home.screen.sections.closed = true;
        home.handler.removeCallbacksAndMessages(null);
        home.screen.files.videoPreview.animate().cancel();
        if (home.destinations.originPanel != null) home.destinations.originPanel.close();
        ConversionService.detach(home.conversion.sessionObserver);
        ++home.state.metadataGeneration;
        ++home.state.transitionGeneration;
        if (home.selection.metadataDialog != null) {
            home.selection.metadataDialog.dismiss();
            home.selection.metadataDialog = null;
        }
        if (home.conversion.cancelConfirmation != null) {
            home.conversion.cancelConfirmation.dismiss();
            home.conversion.cancelConfirmation = null;
        }
        home.selection.worker.shutdownNow();
        home.system.worker.shutdownNow();
        home.diagnostics.worker.shutdown();
    }

    void toast(String s) {
        final HomeController home = this;
        Toast.makeText(home.activity, s, Toast.LENGTH_LONG).show();
    }

    String displayName(Uri uri) {
        final HomeController home = this;
        try (final Cursor c = home.activity.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getString(0);
        } catch (Exception ignored) {
        }
        return "video";
    }

    void openResult(Uri uri) {
        final HomeController home = this;
        try {
            home.activity.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (Exception e) {
            home.toast("Nessun lettore video disponibile");
        }
    }
}
