package it.edo.codecprobe;

import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;

/**
* View construction only; selection and reset have separate controllers.
*/
final class HomeFilesSection {
    Button pickVideo;
    Button clearSelection;
    Button reloadMetadata;
    Button details;
    TextView videoNumber;
    TextView videoLabel;
    TextView inputInfo;
    TextView navHint;
    HorizontalScrollView videoCarousel;
    LinearLayout videoNavigator;
    LinearLayout videoPreview;
    LinearLayout inputSummary;
    LinearLayout inputDetails;
    LinearLayout videoDetails;
    final java.util.ArrayList<Button> videoChips = new java.util.ArrayList<>();

    HomeFilesSection(AppUi ui, LinearLayout files, AppSettings defaults, Bundle state) {
        LinearLayout actions=ui.row(files);
        pickVideo=ui.halfButton(actions,"Seleziona video",true);
        clearSelection=ui.halfButton(actions,"Svuota tutto",false);
        clearSelection.setContentDescription("Svuota selezione, avanzamento e resoconto senza eliminare i video");
        clearSelection.setEnabled(false);
        videoNumber = ui.text(files, "", 15);
        videoCarousel = new HorizontalScrollView(ui.activity);
        videoCarousel.setHorizontalScrollBarEnabled(true);
        videoCarousel.setFillViewport(false);
        videoNavigator = new LinearLayout(ui.activity);
        videoNavigator.setGravity(Gravity.CENTER_VERTICAL);
        videoCarousel.addView(videoNavigator, new HorizontalScrollView.LayoutParams(-2, -2));
        files.addView(videoCarousel, new LinearLayout.LayoutParams(-1, -2));
        navHint = ui.text(files, "Scorri la barra e tocca un video per mostrarne i dettagli.", 13);
        videoPreview = new LinearLayout(ui.activity);
        videoPreview.setOrientation(LinearLayout.VERTICAL);
        files.addView(videoPreview, new LinearLayout.LayoutParams(-1, -2));
        videoLabel = ui.text(videoPreview, "Nessun video selezionato", 23);
        videoLabel.setTypeface(null, Typeface.BOLD);
        videoLabel.setMaxLines(3);
        videoLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        inputInfo = ui.text(videoPreview, "Seleziona un video", 15);
        inputInfo.setLineSpacing(ui.dp(3), 1);
        details = ui.button(videoPreview, "▸ Dettagli video");
        videoDetails = new LinearLayout(ui.activity);
        videoDetails.setOrientation(LinearLayout.VERTICAL);
        videoPreview.addView(videoDetails);
        videoDetails.setVisibility(View.GONE);
        inputSummary = new LinearLayout(ui.activity);
        inputSummary.setOrientation(LinearLayout.VERTICAL);
        videoDetails.addView(inputSummary);
        inputDetails = new LinearLayout(ui.activity);
        inputDetails.setOrientation(LinearLayout.VERTICAL);
        videoDetails.addView(inputDetails);
        reloadMetadata = ui.button(files, "Rileggi metadati");
    }

    void setBusy(boolean busy, boolean choosing, boolean loading, boolean hasInputs) {
        details.setVisibility(hasInputs ? View.VISIBLE : View.GONE);
        reloadMetadata.setVisibility(hasInputs ? View.VISIBLE : View.GONE);
        pickVideo.setEnabled(!busy && !choosing);
        reloadMetadata.setEnabled(!busy && !choosing && !loading);
    }
}
