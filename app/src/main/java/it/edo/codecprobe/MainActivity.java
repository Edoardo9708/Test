package it.edo.codecprobe;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.ClipboardManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Range;
import android.view.Surface;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private static final String MIME = "video/hevc";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView output;
    private Button run;
    private Button software;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private File exportFile;
    private volatile boolean destroyed;
    private final StringBuilder report = new StringBuilder();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = Math.round(16 * getResources().getDisplayMetrics().density);
        layout.setPadding(p, p, p, p);
        layout.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(p + bars.left, p + bars.top, p + bars.right, p + bars.bottom);
            } else {
                v.setPadding(p + insets.getSystemWindowInsetLeft(), p + insets.getSystemWindowInsetTop(),
                        p + insets.getSystemWindowInsetRight(), p + insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        TextView title = new TextView(this);
        title.setText("Test codec video · v0.6");
        title.setTextSize(24);
        layout.addView(title);
        TextView description = new TextView(this);
        description.setText("Test API Android oppure codifica software di un filmato sintetico Full HD.");
        layout.addView(description);
        run = new Button(this);
        run.setText("Esegui test");
        layout.addView(run);
        software = new Button(this);
        software.setText("Test x265 software · Full HD / CRF");
        layout.addView(software);
        Button cancel = new Button(this);
        cancel.setText("Interrompi test x265");
        layout.addView(cancel);
        cancel.setOnClickListener(v -> {
            cancelled.set(true);
            Toast.makeText(this, "Interruzione alla fine della chiamata corrente", Toast.LENGTH_SHORT).show();
        });
        Button export = new Button(this);
        export.setText("Esporta MP4 di prova");
        layout.addView(export);
        export.setOnClickListener(v -> {
            if (!run.isEnabled()) {
                Toast.makeText(this, "Attendi la fine del test", Toast.LENGTH_SHORT).show();
                return;
            }
            exportFile = X265Probe.outputFile(this, 32);
            if (!exportFile.isFile()) exportFile = X265Probe.outputFile(this, 24);
            if (!exportFile.isFile()) {
                Toast.makeText(this, "Nessun MP4 disponibile", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/mp4");
            intent.putExtra(Intent.EXTRA_TITLE, exportFile.getName());
            startActivityForResult(intent, 7);
        });
        Button saveLog = new Button(this);
        saveLog.setText("Salva log completo (.txt)");
        layout.addView(saveLog);
        saveLog.setOnClickListener(v -> {
            if (!run.isEnabled()) {
                Toast.makeText(this, "Attendi la fine del test per salvare il log completo", Toast.LENGTH_SHORT).show();
                return;
            }
            final String snapshot = output.getText().toString();
            // Freeze the text before opening the picker; a later test cannot replace it.
            saveLog.setEnabled(false);
            worker.execute(() -> {
                try {
                    java.nio.file.Files.write(new File(getCacheDir(), "log-export.txt").toPath(),
                            snapshot.getBytes(StandardCharsets.UTF_8));
                    runOnUiThread(() -> {
                        if (destroyed) return;
                        saveLog.setEnabled(true);
                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TITLE, "CodecProbe-log-"+System.currentTimeMillis()+".txt");
                        startActivityForResult(intent, 8);
                    });
                } catch (Exception error) {
                    runOnUiThread(() -> {
                        if (destroyed) return;
                        saveLog.setEnabled(true);
                        Toast.makeText(this, "Errore salvataggio log: "+detail(error), Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
        Button copy = new Button(this);
        copy.setText("Copia risultati");
        layout.addView(copy);
        ScrollView scroll = new ScrollView(this);
        output = new TextView(this);
        output.setTextSize(14);
        output.setTextIsSelectable(true);
        scroll.addView(output);
        layout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(layout);
        layout.requestApplyInsets();
        output.setText("Premi Esegui test per iniziare.");
        File previous = X265Probe.logFile(this);
        if (previous.isFile()) {
            try {
                output.setText("Ultimo log x265 salvato:\n"
                        + new String(java.nio.file.Files.readAllBytes(previous.toPath()), StandardCharsets.UTF_8));
            } catch (Exception error) { output.setText("Errore recupero log: " + detail(error)); }
        }
        software.setOnClickListener(v -> {
            run.setEnabled(false);
            software.setEnabled(false);
            cancelled.set(false);
            report.setLength(0);
            output.setText("Test x265 in corso…");
            worker.execute(() -> {
                try { X265Probe.run(getApplicationContext(), cancelled, this::line); }
                finally {
                    runOnUiThread(() -> {
                        if (!destroyed) { run.setEnabled(true); software.setEnabled(true); }
                    });
                }
            });
        });
        copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Test codec", output.getText()));
            Toast.makeText(this, "Risultati copiati", Toast.LENGTH_SHORT).show();
        });
        run.setOnClickListener(v -> {
            run.setEnabled(false);
            software.setEnabled(false);
            report.setLength(0);
            output.setText("Test in corso…");
            worker.execute(() -> {
                try { testAll(); }
                catch (Exception e) { line("ERRORE generale: " + detail(e)); }
                finally {
                    line("\nTest terminato. OK = chiamata riuscita, non conversione verificata.");
                    runOnUiThread(() -> { if (!destroyed) { run.setEnabled(true); software.setEnabled(true); } });
                }
            });
        });
    }

    private void line(String text) {
        report.append(text).append('\n');
        String snapshot = report.toString();
        runOnUiThread(() -> { if (!destroyed) output.setText(snapshot); });
    }

    private void testAll() {
        line("\n*** INIZIO TEST API ANDROID ***");
        line("STEP 1: elenco encoder HEVC\n• STEP 2: leggo limiti e scelgo un profilo\n• STEP 3: provo le modalità compatibili.");
        line("LEGENDA: supporto dichiarato = capacità riportate da Android\n• OK = chiamata terminata senza errori. Questo test non invia fotogrammi.");
        line(Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE
                + " · API " + Build.VERSION.SDK_INT);
        line("Versione 0.6 · profilo preferito: 1280×720 a 30 fps. Fallback dai limiti dichiarati.");
        line("Input Surface, 4 Mbit/s nelle modalità bitrate (adattato al range).");
        int found = 0;
        for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
            if (!info.isEncoder() || info.isAlias()) continue;
            boolean hevc = false;
            for (String type : info.getSupportedTypes()) if (MIME.equalsIgnoreCase(type)) hevc = true;
            if (!hevc) continue;
            found++;
            line("\n*** STEP 2 · ANALISI ENCODER ***\nEncoder: " + info.getName() + " · hardware=" + info.isHardwareAccelerated()
                    + " · software=" + info.isSoftwareOnly());
            try { testEncoder(info); }
            catch (Exception e) { line("ERRORE lettura capacità: " + detail(e)); }
            if (destroyed) return;
        }
        if (found == 0) line("NON SUPPORTATO: nessun encoder H.265 disponibile.");
        else line("\nEncoder H.265 trovati: " + found);
    }

    private void testEncoder(MediaCodecInfo info) {
        MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(MIME);
        boolean surface = false;
        for (int color : caps.colorFormats)
            if (color == MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface) surface = true;
        MediaCodecInfo.VideoCapabilities video = caps.getVideoCapabilities();
        boolean size = video.areSizeAndRateSupported(1280, 720, 30);
        line((surface && size ? "OK" : "NON SUPPORTATO") + ": profilo di prova (Surface="
                + surface + ", 720p30=" + size + ")");
        line("Limiti dichiarati: larghezza=" + video.getSupportedWidths()
                + ", altezza=" + video.getSupportedHeights()
                + ", fps globali=" + video.getSupportedFrameRates());
        line("Allineamento: larghezza=" + video.getWidthAlignment()
                + ", altezza=" + video.getHeightAlignment()
                + " · bitrate=" + video.getBitrateRange());
        Profile selected = selectProfile(video);
        if (selected != null) {
            line("Profilo scelto: " + selected.width + "×" + selected.height
                    + " a " + selected.fps + " fps"
                    + (size ? " · preferito" : " · fallback"));
        } else {
            line("NON SUPPORTATO: nessuna combinazione trovata nella ricerca limitata; nessuna prova reale.");
        }
        MediaCodecInfo.EncoderCapabilities enc = caps.getEncoderCapabilities();
        if (enc.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ))
            line("CQ: range qualità dichiarato=" + enc.getQualityRange());
        int[] modes = {MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ};
        String[] names = {"VBR · bitrate variabile", "CBR · bitrate costante", "CQ · qualità costante"};
        for (int i = 0; i < modes.length; i++) {
            int mode = modes[i];
            String name = names[i];
            boolean supported = enc.isBitrateModeSupported(mode);
            line((supported ? "OK" : "NON SUPPORTATO") + ": supporto dichiarato " + name);
            if (!supported || !surface || selected == null) continue;
            MediaFormat format = MediaFormat.createVideoFormat(MIME, selected.width, selected.height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setFloat(MediaFormat.KEY_FRAME_RATE, selected.fps);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, mode);
            if (mode == MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ) {
                Range<Integer> quality = enc.getQualityRange();
                int value = (int) (quality.getLower() + ((long) quality.getUpper() - quality.getLower()) / 2);
                format.setInteger(MediaFormat.KEY_QUALITY, value);
                line("CQ: range=" + quality + ", qualità richiesta=" + value);
            } else {
                int bitrate = caps.getVideoCapabilities().getBitrateRange().clamp(4_000_000);
                format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
                line("Bitrate richiesto=" + bitrate + " bit/s");
            }
            line("Supporto del formato completo dichiarato=" + caps.isFormatSupported(format));
            probe(info.getName(), name, format);
            if (destroyed) return;
        }
    }

    private static final class Profile {
        final int width, height;
        final float fps;
        Profile(int width, int height, float fps) {
            this.width = width;
            this.height = height;
            this.fps = fps;
        }
    }

    private Profile selectProfile(MediaCodecInfo.VideoCapabilities video) {
        Profile selected = null;
        int[][] sizes = {{1280, 720}, {1920, 1080}, {640, 480}, {640, 360},
                {320, 240}, {256, 256}, {176, 144}};
        // Report all candidates, even after a compatible one is found.
        for (int[] candidate : sizes) {
            int w = candidate[0], h = candidate[1];
            if (!video.isSizeSupported(w, h)) {
                line("Dimensioni " + w + "×" + h + ": NON SUPPORTATO");
                continue;
            }
            Range<Double> rates = video.getSupportedFrameRatesFor(w, h);
            line("Dimensioni " + w + "×" + h + ": OK · fps=" + rates);
            Profile profile = compatibleProfile(video, w, h, rates);
            if (selected == null && profile != null) selected = profile;
        }
        if (selected != null) return selected;
        // Search aligned dimensions inside the published ranges, bounded to 1920×1080.
        Range<Integer> widths = video.getSupportedWidths();
        int alignW = video.getWidthAlignment();
        int alignH = video.getHeightAlignment();
        int firstW = alignUp(widths.getLower(), alignW);
        int lastW = Math.min(widths.getUpper(), 1920);
        for (int w = firstW; w <= lastW; w += alignW) {
            if (destroyed) return null;
            Range<Integer> heights;
            try { heights = video.getSupportedHeightsFor(w); }
            catch (IllegalArgumentException e) { continue; }
            int firstH = alignUp(heights.getLower(), alignH);
            int lastH = Math.min(heights.getUpper(), 1080);
            // Check a small set of valid heights for each width, no unbounded probing.
            int[] candidates = {firstH, (lastH / alignH) * alignH,
                    (heights.clamp(720) / alignH) * alignH};
            for (int h : candidates) {
                if (h < firstH || h > lastH || !video.isSizeSupported(w, h)) continue;
                Profile profile = compatibleProfile(video, w, h,
                        video.getSupportedFrameRatesFor(w, h));
                if (profile != null) return profile;
            }
        }
        return null;
    }

    private static int alignUp(int value, int alignment) {
        return (int) (((long) value + alignment - 1) / alignment * alignment);
    }

    private static Profile compatibleProfile(MediaCodecInfo.VideoCapabilities video,
            int width, int height, Range<Double> rates) {
        // Positive rates only; global ranges alone do not validate a specific size.
        double low = Math.max(1.0, rates.getLower());
        double high = Math.min(60.0, rates.getUpper());
        if (low > high) return null;
        float fps = (float) Math.max(low, Math.min(30.0, high));
        if (!rates.contains((double) fps) || !video.areSizeAndRateSupported(width, height, fps))
            return null;
        return new Profile(width, height, fps);
    }

    private void probe(String codecName, String modeName, MediaFormat format) {
        MediaCodec codec = null;
        Surface surface = null;
        boolean started = false;
        String phase = "createByCodecName";
        line("\n*** STEP 3 · PROVA API: " + modeName + " ***");
        line("Creo encoder\n• applico formato\n• preparo superficie di ingresso\n• avvio\n• arresto\n• libero risorse.");
        try {
            line("INIZIO: " + phase + " · creo l'encoder");
            codec = MediaCodec.createByCodecName(codecName);
            line("OK: " + phase);
            phase = "configure";
            line("INIZIO: " + phase + " · applico il formato");
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            line("OK: " + phase);
            phase = "createInputSurface";
            line("INIZIO: " + phase + " · preparo ingresso fotogrammi");
            surface = codec.createInputSurface();
            line("OK: " + phase);
            phase = "start";
            line("INIZIO: " + phase + " · avvio encoder");
            codec.start();
            started = true;
            line("OK: " + phase);
            phase = "stop";
            line("INIZIO: " + phase + " · arresto encoder");
            codec.stop();
            started = false;
            line("OK: " + phase);
        } catch (Exception e) {
            line("ERRORE: " + phase + " · " + detail(e));
        } finally {
            if (codec != null) {
                if (started) {
                    try { codec.stop(); }
                    catch (Exception e) { line("ERRORE: stop di pulizia · " + detail(e)); }
                }
                try { codec.release(); line("OK: release encoder"); }
                catch (Exception e) { line("ERRORE: release · " + detail(e)); }
            }
            if (surface != null) {
                try { surface.release(); }
                catch (Exception e) { line("ERRORE: release Surface · " + detail(e)); }
            }
        }
    }

    private static String detail(Exception e) {
        String text = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
        if (e instanceof MediaCodec.CodecException) {
            MediaCodec.CodecException ce = (MediaCodec.CodecException) e;
            text += " · " + ce.getDiagnosticInfo() + " · recuperabile=" + ce.isRecoverable()
                    + " · temporaneo=" + ce.isTransient();
        }
        return text;
    }

    @Override protected void onDestroy() {
        destroyed = true;
        cancelled.set(true);
        worker.shutdown();
        super.onDestroy();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if ((request != 7 && request != 8) || result != RESULT_OK || data == null || data.getData() == null) return;
        final File source = request == 8 ? new File(getCacheDir(), "log-export.txt") : exportFile;
        if (source == null) return;
        worker.execute(() -> {
            try (FileInputStream input = new FileInputStream(source);
                 OutputStream target = getContentResolver().openOutputStream(data.getData())) {
                if (target == null) throw new IllegalStateException("Destinazione non disponibile");
                byte[] bytes = new byte[8192];
                int count;
                while ((count = input.read(bytes)) != -1) target.write(bytes, 0, count);
                runOnUiThread(() -> {
                    if (!destroyed) Toast.makeText(this, request == 8 ? "Log TXT salvato" : "MP4 esportato", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (!destroyed) Toast.makeText(this, "Errore esportazione: " + detail(error), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
