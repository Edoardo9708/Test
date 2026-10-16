package it.edo.codecprobe;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.view.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.concurrent.*;
import static android.app.Activity.RESULT_OK;
import static it.edo.codecprobe.MainActivity.*;
import static it.edo.codecprobe.HomeRequests.*;

/**
* HDR diagnostic, TXT exports and safe log cleanup.
*/
final class HomeDiagnostics {
    private final HomeController home;
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    final ArrayDeque<String> recent = new ArrayDeque<>();

    HomeDiagnostics(HomeController home) {
        this.home = home;
    }

    void bind() {
        home.screen.diagnostics.saveLog.setOnClickListener((v)->exportLog());
        home.screen.diagnostics.saveMetadata.setOnClickListener((v)->exportMetadata());
        home.screen.diagnostics.hdrTest.setOnClickListener((v)->startHdrTest());
        home.screen.diagnostics.saveHdr.setOnClickListener((v)->exportLog(HdrProbe.logFile(home.activity), "CodecProbe-HDR-log-"));
        home.screen.diagnostics.clearLogs.setOnClickListener((v)->clearAllLogs());
        home.screen.diagnostics.toggle.setOnClickListener((v)->{
            boolean show = home.screen.diagnostics.logView.getVisibility() != View.VISIBLE;
            home.screen.diagnostics.logView.setVisibility(show ? View.VISIBLE : View.GONE);
            home.screen.diagnostics.toggle.setText(show ? "Nascondi ultimi messaggi" : "Mostra ultimi messaggi");
        });
    }

    void updateClearLogsButton() {
        boolean blocked = home.state.exportingDiagnostics || home.state.pendingLog != null || home.state.choosingOrigins;
        home.screen.diagnostics.setBusy(home.state.busy, home.state.clearingLogs, blocked);
        home.screen.diagnostics.clearLogs.setEnabled(!home.state.busy && !home.state.clearingLogs && !blocked);
    }

    private boolean canStartAction() {
        if (home.state.destroyed || home.state.busy || ConversionService.snapshot().running || home.state.clearingLogs || home.state.exportingDiagnostics || home.state.pendingLog != null || home.state.choosingOrigins) {
            home.toast("Completa prima l’operazione in corso");
            return false;
        }
        return true;
    }

    void clearAllLogs() {
        if (home.state.busy || ConversionService.snapshot().running || home.state.clearingLogs || home.state.exportingDiagnostics || home.state.pendingLog != null || home.state.choosingOrigins) {
            home.toast("Completa prima l’operazione in corso");
            return;
        }
        home.state.clearingLogs = true;
        home.conversion.setBusy(home.state.busy);
        worker.execute(()->{
            final java.util.ArrayList<String> failures = new java.util.ArrayList<>();
            final int[] removed = {0};
            boolean idle = ConversionService.clearLogsIfIdle(()->{
                synchronized (FolderPickerActivity.class) {
                    File[] logs = {VideoConverter.logFile(home.activity), ConversionService.batchLogFile(home.activity), HdrProbe.logFile(home.activity), X265Probe.logFile(home.activity), FolderPickerActivity.logFile(home.activity)};
                    for (File file : logs) if (file.exists()) {
                        if (file.delete()) removed[0]++; else failures.add(file.getName());
                    }
                    File[] snapshots = home.activity.getCacheDir().listFiles((f)->f.getName().endsWith(".txt") && (f.getName().startsWith("log-export-") || f.getName().startsWith("metadata-export-")));
                    if (snapshots != null) for (File file : snapshots) if (file.delete()) removed[0]++; else failures.add(file.getName());
                }
            });
            home.activity.runOnUiThread(()->{
                home.state.clearingLogs = false;
                if (home.state.destroyed) return;
                if (idle) {
                    synchronized (recent) {
                        recent.clear();
                    }
                    home.screen.diagnostics.logView.setText("");
                }
                home.conversion.setBusy(home.state.busy);
                home.toast(!idle ? "Conversione avviata: log mantenuti" : failures.isEmpty() ? "Log cancellati · " + removed[0] + " file" : "Alcuni log non sono stati cancellati: " + String.join(", ", failures));
            });
        });
    }

    void exportMetadata() {
        if (!canStartAction()) return;
        if (home.state.input == null) {
            home.toast("Seleziona prima il video da analizzare");
            return;
        }
        if (!OriginalMedia.hasLocationPermission(home.activity) || !OriginalMedia.hasVideoPermission(home.activity)) {
            java.util.ArrayList<String> permissions = new java.util.ArrayList<>();
            permissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION);
            java.util.Collections.addAll(permissions, OriginalMedia.videoPermissions());
            home.activity.requestPermissions(permissions.toArray(new String[0]), 8);
            return;
        }
        final Uri selected = home.state.input;
        final String name = home.state.sourceName;
        home.state.exportingDiagnostics = true;
        updateClearLogsButton();
        home.toast("Leggo solo i blocchi dei metadati, senza copiare il video…");
        worker.execute(()->{
            File snapshot = null;
            try {
                Uri original = OriginalMedia.resolve(home.activity, selected);
                snapshot = File.createTempFile("metadata-export-", ".txt", home.activity.getCacheDir());
                try (final ParcelFileDescriptor fd = home.activity.getContentResolver().openFileDescriptor(original, "r");) {
                    if (fd == null) throw new IOException("Video originale non disponibile");
                    try (final ParcelFileDescriptor.AutoCloseInputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd);
                        final Writer out = new OutputStreamWriter(new FileOutputStream(snapshot), StandardCharsets.UTF_8);) {
                        out.write("Versione app: " + BuildConfig.VERSION_NAME + "\nFile: " + name + "\n\n");
                        MetadataDump.write(in.getChannel(), out, ()->home.state.destroyed);
                    }
                }
                final File result = snapshot;
                home.activity.runOnUiThread(()->{
                    if (home.state.destroyed) {
                        result.delete();
                        return;
                    }
                    home.state.pendingLog = result.getAbsolutePath();
                    home.state.exportingDiagnostics = false;
                    updateClearLogsButton();
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE);
                    i.putExtra(Intent.EXTRA_TITLE, "v" + BuildConfig.VERSION_NAME + "-CodecProbe-metadati-" + System.currentTimeMillis() + ".txt");
                    home.activity.startActivityForResult(i, SAVE_LOG);
                });
            } catch (Exception e) {
                if (snapshot != null) snapshot.delete();
                home.activity.runOnUiThread(()->{
                    if (!home.state.destroyed) {
                        home.state.exportingDiagnostics = false;
                        updateClearLogsButton();
                        home.toast("Esportazione metadati non riuscita: " + e.getMessage());
                    }
                });
            }
        });
    }

    void startHdrTest() {
        if (!canStartAction()) return;
        if (home.state.clearingLogs) {
            home.toast("Attendi la cancellazione dei log");
            return;
        }
        if (home.state.input == null) {
            home.toast("Seleziona un video HDR HEVC");
            return;
        }
        if (home.state.busy || ConversionService.snapshot().running) return;
        final Uri selected = home.state.input;
        final int crf = home.compression.selectedCrf();
        home.screen.sections.focusCard(CARD_CONVERSION);
        home.state.hdrTesting = true;
        if (home.state.selectedInputs.size() > 1) home.toast("Prova HDR sul video visualizzato: " + home.state.sourceName);
        home.state.cancelled.set(false);
        home.conversion.setBusy(true);
        home.screen.conversion.status.setText("Test HDR: resta su questa schermata…");
        recent.clear();
        home.screen.diagnostics.logView.setText("");
        home.screen.diagnostics.logView.setVisibility(View.VISIBLE);
        worker.execute(()->{
            String result;
            try {
                result = new HdrProbe(home.activity.getApplicationContext(), ()->home.state.destroyed || home.state.cancelled.get(), (line)->{
                    synchronized (recent) {
                        recent.addLast(line);
                        while (recent.size() > 14) recent.removeFirst();
                    }
                    home.activity.runOnUiThread(()->{
                        if (!home.state.destroyed) {
                            synchronized (recent) {
                                home.screen.diagnostics.logView.setText(String.join("\n", recent));
                            }
                        }
                    });
                }).run(selected, crf);
            } catch (Exception | LinkageError e) {
                result = "Test HDR non completato: " + e.getMessage() + "\nEsporta il log HDR.";
            }
            final String message = result;
            home.activity.runOnUiThread(()->{
                home.state.hdrTesting = false;
                if (!home.state.destroyed) {
                    home.conversion.setBusy(false);
                    home.screen.conversion.status.setText(message);
                }
            });
        });
    }

    void exportLog() {
        File conversion = ConversionService.logFile(home.activity);
        exportLog(conversion.isFile() ? conversion : FolderPickerActivity.logFile(home.activity), "CodecProbe-log-");
    }

    void copy(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[65536];
        int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
    }

    void exportLog(File source, String prefix) {
        if (!canStartAction()) return;
        if (!source.isFile()) {
            home.toast("Nessun log di conversione disponibile");
            return;
        }
        home.state.exportingDiagnostics = true;
        updateClearLogsButton();
        worker.execute(()->{
            try {
                File snapshot = File.createTempFile("log-export-", ".txt", home.activity.getCacheDir());
                try (final InputStream in = new FileInputStream(source);
                    final OutputStream out = new FileOutputStream(snapshot);) {
                    if(prefix.equals("CodecProbe-log-"))CompactLog.export(in,out);else copy(in,out);
                    File access = FolderPickerActivity.logFile(home.activity);
                    if (prefix.equals("CodecProbe-log-") && access.isFile() && !access.equals(source)) {
                        out.write("\n\n*** DIAGNOSTICA CARTELLE E SELETTORE ANDROID ***\n".getBytes(StandardCharsets.UTF_8));
                        try (final InputStream extra = new FileInputStream(access);) {
                            copy(extra, out);
                        }
                    }
                }
                home.activity.runOnUiThread(()->{
                    if (home.state.destroyed) {
                        snapshot.delete();
                        return;
                    }
                    home.state.pendingLog = snapshot.getAbsolutePath();
                    home.state.exportingDiagnostics = false;
                    updateClearLogsButton();
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE);
                    i.putExtra(Intent.EXTRA_TITLE, prefix + System.currentTimeMillis() + ".txt");
                    home.activity.startActivityForResult(i, SAVE_LOG);
                });
            } catch (Exception e) {
                home.activity.runOnUiThread(()->{
                    if (!home.state.destroyed) {
                        home.state.exportingDiagnostics = false;
                        updateClearLogsButton();
                        home.toast("Errore: " + e.getMessage());
                    }
                });
            }
        });
    }

    void onLogResult(int request, int code, Intent data) {
        if (code != RESULT_OK || data == null || data.getData() == null) {
            if (home.state.pendingLog != null) {
                new File(home.state.pendingLog).delete();
                home.state.pendingLog = null;
                updateClearLogsButton();
            }
            return;
        }
        if (home.state.pendingLog == null) return;
        Uri uri = data.getData();
        final File snapshot = new File(home.state.pendingLog);
        home.state.pendingLog = null;
        home.state.exportingDiagnostics = true;
        updateClearLogsButton();
        worker.execute(()->{
            try (final InputStream in = new FileInputStream(snapshot);
                final OutputStream out = home.activity.getContentResolver().openOutputStream(uri, "w");) {
                if (out == null) throw new IOException("Destinazione non disponibile");
                copy(in, out);
                home.activity.runOnUiThread(()->{
                    if (!home.state.destroyed) home.toast("File TXT salvato");
                });
            } catch (Exception e) {
                home.activity.runOnUiThread(()->{
                    if (!home.state.destroyed) home.toast("Errore esportazione log: " + e.getMessage());
                });
            } finally {
                snapshot.delete();
                home.activity.runOnUiThread(()->{
                    home.state.exportingDiagnostics = false;
                    if (!home.state.destroyed) updateClearLogsButton();
                });
            }
        });
    }
}
