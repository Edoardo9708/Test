package it.edo.codecprobe;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;
import static android.app.Activity.RESULT_OK;
import static it.edo.codecprobe.HomeRequests.*;

/**
* Video selection, cached metadata and carousel transitions.
*/
final class HomeSelection {
    private final HomeController home;
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    android.app.AlertDialog metadataDialog;
    boolean metadataBackground;
    ProgressBar metadataProgress;
    TextView metadataMessage;

    HomeSelection(HomeController home) {
        this.home = home;
    }

    void bind() {
        home.screen.files.pickVideo.setOnClickListener((v)->pickVideos());
        home.screen.files.reloadMetadata.setOnClickListener((v)->{
            VideoMetadataCache.clear();
            inspectInput();
        });
        home.screen.files.details.setOnClickListener((v)->{
            boolean show = home.screen.files.videoDetails.getVisibility() != View.VISIBLE;
            home.screen.files.videoDetails.setVisibility(show ? View.VISIBLE : View.GONE);
            home.screen.files.details.setText(show ? "▾ Dettagli video" : "▸ Dettagli video");
            if (show) loadInputDetails();
        });
    }

    void pickVideos() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("video/*").addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        home.activity.startActivityForResult(intent, PICK_VIDEO);
    }

    void inspectInput() {
        if (home.state.destroyed) return;
        VideoMetadataCache.permissions(OriginalMedia.hasLocationPermission(home.activity) + ":" + OriginalMedia.hasVideoPermission(home.activity));
        rebuildVideoCarousel();
        if (home.state.input == null) {
            home.screen.files.inputInfo.setText("Seleziona uno o più video");
            return;
        }
        boolean missing = false;
        for (String uri : home.state.selectedInputs) if (VideoMetadataCache.get(uri) == null) {
            missing = true;
            break;
        }
        showCachedInput();
        if (missing && !home.state.metadataLoading) preloadMetadata(!home.state.selectionSession.isEmpty());
    }

    void showCachedInput() {
        home.screen.files.inputSummary.removeAllViews();
        home.screen.files.inputDetails.removeAllViews();
        if (home.state.input == null) {
            home.screen.files.inputInfo.setText("Seleziona uno o più video");
            return;
        }
        VideoMetadataCache.Entry entry = VideoMetadataCache.get(home.state.input.toString());
        if (entry == null) {
            home.screen.files.inputInfo.setText(home.state.metadataLoading && metadataBackground ? "Lettura metadati in background…" : "Metadati non ancora letti · premi Rileggi metadati");
            return;
        }
        home.state.sourceName = entry.name;
        home.screen.files.videoLabel.setText(home.state.sourceName);
        home.screen.files.inputInfo.setText(entry.quick + (entry.report.readError ? "\nLettura incompleta · apri i dettagli per l’errore" : ""));
        if (home.screen.files.videoDetails.getVisibility() == View.VISIBLE) loadInputDetails();
    }

    void loadInputDetails() {
        if (home.state.input == null) return;
        VideoMetadataCache.Entry entry = VideoMetadataCache.get(home.state.input.toString());
        if (entry == null) return;
        home.ui.renderInfoSections(home.screen.files.inputSummary, entry.report.summarySections);
        home.ui.renderInfoSections(home.screen.files.inputDetails, entry.report.detailSections);
    }

    void preloadMetadata() { preloadMetadata(false); }

    void preloadMetadata(boolean background) {
        if (home.state.destroyed || home.state.metadataLoading) return;
        final int generation = ++home.state.metadataGeneration;
        home.state.metadataLoading = true;
        home.screen.files.reloadMetadata.setEnabled(false);
        final java.util.ArrayList<String> uris = new java.util.ArrayList<>(home.state.selectedInputs);
        final long cacheEpoch = VideoMetadataCache.epoch();
        final java.util.ArrayList<String> names = new java.util.ArrayList<>(home.state.selectedNames);
        metadataBackground = background;
        if (background) { home.toast("Rileggo i metadati in background"); showCachedInput(); }
        else showMetadataDialog(uris.size());
        FolderPickerActivity.record(home.activity, "*** INIZIO LETTURA METADATI DELLA CODA ***\n1. Leggo nome e riepilogo.\n2. Analizzo tracce e metadati.\n3. Conservo il risultato in memoria.\nVideo selezionati: " + uris.size());
        worker.execute(()->{
            int failures = 0;
            for (int index = 0; index < uris.size(); index++) {
                if (home.state.destroyed || generation != home.state.metadataGeneration) return;
                final int position = index;
                final String uri = uris.get(index);
                home.activity.runOnUiThread(()->{
                    if (!home.state.destroyed && generation == home.state.metadataGeneration && !background) {
                        metadataMessage.setText("Video " + (position + 1) + " di " + uris.size() + "\n" + names.get(position));
                        metadataProgress.setProgress(position);
                    }
                });
                VideoMetadataCache.Entry entry = VideoMetadataCache.get(uri);
                if (entry == null) {
                    long begin = SystemClock.elapsedRealtime();
                    String name = home.displayName(Uri.parse(uri));
                    FolderPickerActivity.record(home.activity, "BEGIN METADATI · video=" + (index + 1) + "/" + uris.size() + " · " + name);
                    try {
                        String quick = InputInspector.quickSummary(home.activity, Uri.parse(uri));
                        InputInspector.Report report = InputInspector.inspect(home.activity, Uri.parse(uri), ()->home.state.destroyed || generation != home.state.metadataGeneration);
                        entry = new VideoMetadataCache.Entry(name, quick, report);
                    } catch (Exception e) {
                        entry = new VideoMetadataCache.Entry(name, "Informazioni non disponibili", new InputInspector.Report(java.util.Collections.singletonMap("ERRORE LETTURA", String.valueOf(e)), java.util.Collections.emptyMap(), true));
                    }
                    if (home.state.destroyed || generation != home.state.metadataGeneration) return;
                    if (!VideoMetadataCache.put(uri, entry, cacheEpoch)) {
                        home.activity.runOnUiThread(()->{
                            if (!home.state.destroyed && generation == home.state.metadataGeneration) {
                                stopMetadataLoading();
                                inspectInput();
                            }
                        });
                        return;
                    }
                    FolderPickerActivity.record(home.activity, "END METADATI · video=" + (index + 1) + " · esito=" + (entry.report.readError ? "INCOMPLETO" : "OK") + " · tempo_ms=" + (SystemClock.elapsedRealtime() - begin));
                }
                if (entry.report.readError) failures++;
                final String name = entry.name;
                home.activity.runOnUiThread(()->{
                    if (!home.state.destroyed && generation == home.state.metadataGeneration) {
                        home.state.selectedNames.set(position, name);
                        if (!background) metadataProgress.setProgress(position + 1);
                        if (background && home.state.input != null && home.state.input.toString().equals(uri)) showCachedInput();
                    }
                });
            }
            final int errors = failures;
            home.activity.runOnUiThread(()->{
                if (home.state.destroyed || generation != home.state.metadataGeneration) return;
                home.state.metadataLoading = false;
                if (metadataDialog != null) metadataDialog.dismiss();
                metadataDialog = null;
                metadataBackground = false;
                home.screen.files.reloadMetadata.setEnabled(true);
                home.state.sourceName = home.state.selectedNames.get(home.state.selectedVideoIndex);
                rebuildVideoCarousel();
                home.destinations.refreshLabels();
                showCachedInput();
                home.destinations.updateDestinationPanel();
                home.destinations.originPanel.refresh();
                home.conversion.setBusy(home.state.busy);
                FolderPickerActivity.record(home.activity, "*** FINE LETTURA METADATI ***\nVideo: " + uris.size() + " · letture incomplete: " + errors);
                if (errors > 0) home.toast(errors + " video con lettura incompleta: apri le schede contrassegnate con !");
            });
        });
    }

    private void showMetadataDialog(int count) {
        LinearLayout panel = new LinearLayout(home.activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(home.ui.dp(24), home.ui.dp(12), home.ui.dp(24), home.ui.dp(12));
        metadataMessage = new TextView(home.activity);
        metadataMessage.setText("Preparo la lettura di " + count + " video…");
        metadataMessage.setTextSize(16);
        metadataMessage.setTextColor(home.ui.ink);
        panel.addView(metadataMessage);
        metadataProgress = new ProgressBar(home.activity, null, android.R.attr.progressBarStyleHorizontal);
        metadataProgress.setProgressTintList(ColorStateList.valueOf(home.ui.accent));
        metadataProgress.setMax(count);
        panel.addView(metadataProgress, new LinearLayout.LayoutParams(-1, home.ui.dp(24)));
        TextView note = new TextView(home.activity);
        note.setTextColor(home.ui.muted);
        note.setText("Leggo formato, tracce, dimensioni e metadati. I video non vengono decodificati. I risultati restano in memoria.");
        note.setPadding(0, home.ui.dp(12), 0, 0);
        panel.addView(note);
        TextView heading = new TextView(home.activity);
        heading.setText("Lettura metadati dei video");
        heading.setTextColor(home.ui.ink);
        heading.setTextSize(21);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(home.ui.dp(24), home.ui.dp(20), home.ui.dp(24), home.ui.dp(8));
        metadataDialog = new android.app.AlertDialog.Builder(home.activity).setCustomTitle(heading).setView(panel).setNegativeButton("Interrompi", (d,w)->stopMetadataLoading()).create();
        metadataDialog.setCanceledOnTouchOutside(false);
        metadataDialog.setOnCancelListener((d)->stopMetadataLoading());
        metadataDialog.show();
        metadataDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(home.ui.accent);
        metadataDialog.getWindow().setBackgroundDrawable(home.ui.shape(home.ui.surface, 20));
        metadataDialog.getWindow().setDimAmount(0.65F);
    }

    void stopMetadataLoading() {
        ++home.state.metadataGeneration;
        home.state.metadataLoading = false;
        metadataBackground = false;
        if (metadataDialog != null) {
            metadataDialog.dismiss();
            metadataDialog = null;
        }
        home.screen.files.reloadMetadata.setEnabled(true);
        rebuildVideoCarousel();
        home.destinations.refreshLabels();
        showCachedInput();
        home.destinations.updateDestinationPanel();
        home.destinations.originPanel.refresh();
        FolderPickerActivity.record(home.activity, "*** LETTURA METADATI INTERROTTA ***\nI risultati già letti restano in memoria.");
    }

    void rebuildVideoCarousel() {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        java.util.ArrayList<Boolean> errors = new java.util.ArrayList<>();
        for (int n = 0; n < home.state.selectedInputs.size(); n++) {
            VideoMetadataCache.Entry entry = VideoMetadataCache.get(home.state.selectedInputs.get(n));
            String name = entry == null ? home.state.selectedNames.get(n) : entry.name;
            home.state.selectedNames.set(n, name);
            names.add(name); errors.add(entry != null && entry.report.readError);
        }
        home.screen.files.selectedItem.render(names, errors, home.state.selectedVideoIndex,
            !home.state.metadataLoading || metadataBackground, this::selectVideo);
    }

    void updateVideoSelection(boolean reveal) {
        home.screen.files.selectedItem.update(home.state.selectedVideoIndex,
            !home.state.metadataLoading || metadataBackground, reveal);
    }

    void selectVideo(int index) {
        if (index < 0 || index >= home.state.selectedInputs.size() || home.state.metadataLoading && !metadataBackground) return;
        final int generation = ++home.state.transitionGeneration;
        home.screen.files.videoPreview.animate().cancel();
        if (index == home.state.selectedVideoIndex) {
            home.screen.files.videoPreview.setAlpha(1);
            return;
        }
        home.screen.files.videoPreview.animate().alpha(0).setDuration(90).withEndAction(()->{
            if (home.state.destroyed || generation != home.state.transitionGeneration) return;
            home.state.selectedVideoIndex = index;
            home.state.input = Uri.parse(home.state.selectedInputs.get(index));
            home.state.sourceName = home.state.selectedNames.get(index);
            home.destinations.refreshLabels();
            showCachedInput();
            updateVideoSelection(true);
            home.screen.files.videoPreview.animate().alpha(1).setDuration(140).withEndAction(null).start();
        }).start();
    }

    void onVideoResult(int request, int code, Intent data) {
        if (code != RESULT_OK || data == null) return;
        if (request == PICK_VIDEO) {
            java.util.LinkedHashSet<Uri> chosen = new java.util.LinkedHashSet<>();
            if (data.getClipData() != null) for (int n = 0; n < data.getClipData().getItemCount(); n++) chosen.add(data.getClipData().getItemAt(n).getUri());
            if (data.getData() != null) chosen.add(data.getData());
            if (chosen.isEmpty()) return;
            if (chosen.size() > BatchProgress.MAX_FILES) {
                home.toast("Seleziona al massimo " + BatchProgress.MAX_FILES + " video per coda");
                return;
            }
            ++home.state.metadataGeneration;
            home.state.metadataLoading = false;
            metadataBackground = false;
            VideoMetadataCache.clear();
            ++home.state.transitionGeneration;
            home.screen.files.videoPreview.animate().cancel();
            home.screen.files.videoPreview.setAlpha(1);
            home.state.selectionSession = "";
            home.state.selectedInputs.clear();
            home.state.selectedNames.clear();
            home.state.originFolders.clear();
            home.destinations.cancelOrigins();
            home.state.selectedVideoIndex = 0;
            for (Uri chosenUri : chosen) {
                try {
                    home.activity.getContentResolver().takePersistableUriPermission(chosenUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                }
                home.state.selectedInputs.add(chosenUri.toString());
                home.state.selectedNames.add(home.displayName(chosenUri));
            }
            home.state.input = Uri.parse(home.state.selectedInputs.get(0));
            home.state.sourceName = home.state.selectedNames.get(0);
            String defaultFolder = new AppSettings(home.activity).defaultFolder;
            home.state.destinationPlan.reset(home.state.selectedInputs.size(), defaultFolder != null ? defaultFolder : home.state.folder == null ? null : home.state.folder.toString());
            home.destinations.destinationsChanged();
            inspectInput();
            return;
        }
    }
}
