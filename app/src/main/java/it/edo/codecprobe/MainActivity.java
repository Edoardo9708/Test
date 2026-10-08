package it.edo.codecprobe;

import android.app.Activity;
import android.content.ClipData;
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

public final class MainActivity extends Activity {
    private static final String MIME = "video/hevc";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView output;
    private Button run;
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
        title.setText("Test API video · H.265");
        title.setTextSize(24);
        layout.addView(title);
        TextView description = new TextView(this);
        description.setText("Verifica supporto, configurazione e avvio. Non converte ancora video.");
        layout.addView(description);
        run = new Button(this);
        run.setText("Esegui test");
        layout.addView(run);
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
        copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Test codec", output.getText()));
            Toast.makeText(this, "Risultati copiati", Toast.LENGTH_SHORT).show();
        });
        run.setOnClickListener(v -> {
            run.setEnabled(false);
            report.setLength(0);
            output.setText("Test in corso…");
            worker.execute(() -> {
                try { testAll(); }
                catch (Exception e) { line("ERRORE generale: " + detail(e)); }
                finally {
                    line("\nTest terminato. OK = chiamata riuscita, non conversione verificata.");
                    runOnUiThread(() -> { if (!destroyed) run.setEnabled(true); });
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
        line(Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE
                + " · API " + Build.VERSION.SDK_INT);
        line("Profilo di prova: 1280×720, 30 fps, input Surface, 4 Mbit/s (adattato al range).");
        int found = 0;
        for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
            if (!info.isEncoder() || info.isAlias()) continue;
            boolean hevc = false;
            for (String type : info.getSupportedTypes()) if (MIME.equalsIgnoreCase(type)) hevc = true;
            if (!hevc) continue;
            found++;
            line("\nEncoder: " + info.getName() + " · hardware=" + info.isHardwareAccelerated()
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
        boolean size = caps.getVideoCapabilities().areSizeAndRateSupported(1280, 720, 30);
        line((surface && size ? "OK" : "NON SUPPORTATO") + ": profilo di prova (Surface="
                + surface + ", 720p30=" + size + ")");
        MediaCodecInfo.EncoderCapabilities enc = caps.getEncoderCapabilities();
        int[] modes = {MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ};
        String[] names = {"VBR · bitrate variabile", "CBR · bitrate costante", "CQ · qualità costante"};
        for (int i = 0; i < modes.length; i++) {
            int mode = modes[i];
            String name = names[i];
            boolean supported = enc.isBitrateModeSupported(mode);
            line((supported ? "OK" : "NON SUPPORTATO") + ": supporto dichiarato " + name);
            if (!supported || !surface || !size) continue;
            MediaFormat format = MediaFormat.createVideoFormat(MIME, 1280, 720);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
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
            probe(info.getName(), name, format);
            if (destroyed) return;
        }
    }

    private void probe(String codecName, String modeName, MediaFormat format) {
        MediaCodec codec = null;
        Surface surface = null;
        boolean started = false;
        String phase = "createByCodecName";
        line("Prova reale: " + modeName);
        try {
            codec = MediaCodec.createByCodecName(codecName);
            line("OK: " + phase);
            phase = "configure";
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            line("OK: " + phase);
            phase = "createInputSurface";
            surface = codec.createInputSurface();
            line("OK: " + phase);
            phase = "start";
            codec.start();
            started = true;
            line("OK: " + phase);
            phase = "stop";
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
        worker.shutdown();
        super.onDestroy();
    }
}
