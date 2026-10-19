package it.edo.codecprobe;

import android.os.*;
import android.view.*;
import android.widget.*;

/**
* View construction only; selection and reset have separate controllers.
*/
final class HomeFilesSection {
    final SelectedItemView selectedItem;
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
    final java.util.ArrayList<Button> videoChips;

    HomeFilesSection(AppUi ui, LinearLayout files, AppSettings defaults, Bundle state) {
        LinearLayout actions=ui.row(files);
        pickVideo=ui.halfButton(actions,"Seleziona video",true);
        clearSelection=ui.halfButton(actions,"Svuota tutto",false);
        clearSelection.setContentDescription("Svuota selezione, avanzamento e resoconto senza eliminare i video");
        clearSelection.setEnabled(false);
        selectedItem = new SelectedItemView(ui, files, "Video", "Nessun video selezionato", "Dettagli video");
        videoNumber = selectedItem.number;
        videoCarousel = selectedItem.carousel;
        videoNavigator = selectedItem.navigator;
        videoChips = selectedItem.chips;
        navHint = selectedItem.hint;
        videoPreview = selectedItem.preview;
        videoLabel = selectedItem.name;
        inputInfo = selectedItem.summary;
        details = selectedItem.details;
        videoDetails = selectedItem.detailsBody;
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
