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
    @Test public void copiedOriginalHasOwnCountAndOpensAsJpeg(){
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=controller.get().images;ImageRunState run=new ImageRunState();
            run.add(new ImageRunState.Result("Originale.jpg",3000000,3000000,"content://saved/original","Originale copiato",ImageRunState.Outcome.COPIED_ORIGINAL,"","a".repeat(64),"a".repeat(64),"image/jpeg"));
            images.report.render(run);assertEquals(0,run.saved);assertEquals(1,run.copied);
            String summary=images.screen.workReport.summary.getText().toString();assertTrue(summary,summary.contains("originali copiati 1"));assertTrue(summary,summary.contains("Spazio risparmiato: "+CompressionSummary.size(0)));
            find(ImageVirtualReportTest.row(images,0,null),"Apri risultato:").performClick();
            Intent opened=Shadows.shadowOf(controller.get()).getNextStartedActivity();assertEquals("image/jpeg",opened.getType());assertEquals("content://saved/original",opened.getDataString());
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void savedOutputOpensWithReadPermissionAndFailuresHaveCopyableDetails() {
        var controller = Robolectric.buildActivity(ImageActivity.class).setup();
        try {
            ImageController images = controller.get().images;
            ImageRunState run = new ImageRunState();
            run.add(new ImageRunState.Result("Foto.jpg", 3000000, 2000000, "content://saved/photo", "Salvato", ImageRunState.Outcome.SAVED, "Metadati verificati"));
            run.add(new ImageRunState.Result("Errore.jpg", 9000000, 0, null, "JPEG non supportato", ImageRunState.Outcome.FAILED, "Dettaglio tecnico"));
            run.add(new ImageRunState.Result("Grande.jpg", 1000000, 2000000, null, "Nessun risparmio", ImageRunState.Outcome.NOT_SMALLER, "Non salvato"));
            run.add(new ImageRunState.Result("Annullata.jpg", 5000000, 0, null, "Annullata", ImageRunState.Outcome.CANCELLED, ""));
            images.report.render(run);
            WorkReportView report = images.screen.workReport;
            String summary = report.summary.getText().toString();
            assertTrue(summary.contains("Salvate 1 · fallite 1 · senza risparmio 1 · annullate 1"));
            assertTrue(summary.contains("3,00 MB → 2,00 MB"));
            assertTrue(summary.contains("Spazio risparmiato: 1,00 MB"));
            find(ImageVirtualReportTest.row(images,0,null), "Apri risultato:").performClick();
            Intent opened = Shadows.shadowOf(controller.get()).getNextStartedActivity();
            assertEquals("image/heic", opened.getType());
            assertEquals("content://saved/photo", opened.getDataString());
            assertNotEquals(0, opened.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            find(ImageVirtualReportTest.row(images,1,null), "Mostra errore: Foto 2").performClick();
            AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            String copied = controller.get().getSystemService(ClipboardManager.class).getPrimaryClip().getItemAt(0).getText().toString();
            assertTrue(copied.contains("JPEG non supportato"));
            assertTrue(copied.contains("Dettaglio tecnico"));
            assertTrue(dialog.isShowing());
            View firstRow = ImageVirtualReportTest.row(images,0,null);
            images.report.render(new ImageRunState(run));
            assertSame(firstRow, ImageVirtualReportTest.row(images,0,firstRow));
            images.report.render(new ImageRunState());
            assertEquals(0, images.report.results.size());
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
