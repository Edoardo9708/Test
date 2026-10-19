package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.*;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageWorkReportTest {
    @Test public void savedOutputOpensWithReadPermissionAndFailuresHaveCopyableDetails() {
        var controller = Robolectric.buildActivity(ImageActivity.class).setup();
        try {
            ImageController images = controller.get().images;
            ImageRunState run = new ImageRunState();
            run.results.add(new ImageRunState.Result("Foto.jpg", 3000000, 2000000, "content://saved/photo", "Salvato", ImageRunState.Outcome.SAVED, "Metadati verificati"));
            run.results.add(new ImageRunState.Result("Errore.jpg", 9000000, 0, null, "JPEG non supportato", ImageRunState.Outcome.FAILED, "Dettaglio tecnico"));
            run.results.add(new ImageRunState.Result("Grande.jpg", 1000000, 2000000, null, "Nessun risparmio", ImageRunState.Outcome.NOT_SMALLER, "Non salvato"));
            run.results.add(new ImageRunState.Result("Annullata.jpg", 5000000, 0, null, "Annullata", ImageRunState.Outcome.CANCELLED, ""));
            images.report.render(run);
            WorkReportView report = images.screen.workReport;
            String summary = report.summary.getText().toString();
            assertTrue(summary.contains("Salvate 1 · fallite 1 · senza risparmio 1 · annullate 1"));
            assertTrue(summary.contains("3,00 MB → 2,00 MB"));
            assertTrue(summary.contains("Spazio risparmiato: 1,00 MB"));
            find(report.rows, "Apri risultato:").performClick();
            Intent opened = Shadows.shadowOf(controller.get()).getNextStartedActivity();
            assertEquals("image/heic", opened.getType());
            assertEquals("content://saved/photo", opened.getDataString());
            assertNotEquals(0, opened.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            find(report.rows, "Mostra dettagli: Foto 2").performClick();
            AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            String copied = controller.get().getSystemService(ClipboardManager.class).getPrimaryClip().getItemAt(0).getText().toString();
            assertTrue(copied.contains("JPEG non supportato"));
            assertTrue(copied.contains("Dettaglio tecnico"));
            assertTrue(dialog.isShowing());
            View firstRow = report.rows.getChildAt(0);
            images.report.render(new ImageRunState(run));
            assertSame(firstRow, report.rows.getChildAt(0));
            images.report.render(new ImageRunState());
            assertEquals(0, report.rows.getChildCount());
            assertEquals("Nessuna conversione eseguita.", report.summary.getText().toString());
        } finally { controller.pause().stop().destroy(); }
    }

    private static View find(View view, String prefix) {
        if (view.getContentDescription() != null && view.getContentDescription().toString().startsWith(prefix)) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), prefix);
                if (found != null) return found;
            }
        }
        return null;
    }
}
