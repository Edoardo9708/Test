package it.edo.codecprobe;

import android.content.Intent;
import android.net.Uri;
import static android.app.Activity.RESULT_OK;
import static it.edo.codecprobe.HomeRequests.*;

/**
* Per-video destinations and Android folder permission requests.
*/
final class HomeDestinations {
    private final HomeController home;
    OriginDirectoryPanel originPanel;

    HomeDestinations(HomeController home) {
        this.home = home;
    }

    void bind() {
        originPanel = new OriginDirectoryPanel(home.activity, home.screen.destinations.originRows, new OriginDirectoryPanel.Listener(){

            public void authorize(String parent, String representative, boolean origin, String requestedFolder) {
                requestDirectoryAccess(parent, representative, origin, requestedFolder);
            }

            public void changed(boolean ready, java.util.ArrayList<String> destinations, java.util.ArrayList<String> notes) {
                home.state.originsReady = ready;
                home.state.originFolders.clear();
                home.state.originFolders.addAll(destinations);
                home.state.destinationNotes.clear();
                home.state.destinationNotes.addAll(notes);
                home.conversion.setBusy(home.state.busy);
            }

            public void error(String message) {
                FolderPickerActivity.record(home.activity, "ERRORE · verifica cartella · " + message);
                home.toast("Cartella: " + message);
            }
        }, home.ui.ink, home.ui.dark);
        home.screen.destinations.pickFolder.setOnClickListener((v)->{
            if (home.state.selectedInputs.isEmpty()) {
                home.toast("Seleziona prima i video");
                return;
            }
            launchFolderPicker(PICK_FOLDER, home.state.selectedInputs.get(home.state.selectedVideoIndex));
        });
        home.screen.destinations.pickAllFolder.setOnClickListener((v)->launchFolderPicker(PICK_ALL_FOLDER, null));
        home.screen.destinations.videoOrigin.setOnCheckedChangeListener((v,checked)->{
            if (!home.state.updatingDestinationUi && !home.state.selectedInputs.isEmpty()) {
                home.state.destinationPlan.files.get(home.state.selectedVideoIndex).origin = checked;
                destinationsChanged();
            }
        });
        home.screen.destinations.applyAllDestinations.setOnCheckedChangeListener((v,checked)->{
            if (!home.state.updatingDestinationUi) {
                home.state.destinationPlan.applyAll = checked;
                destinationsChanged();
            }
        });
        home.screen.destinations.allOrigin.setOnCheckedChangeListener((v,checked)->{
            if (!home.state.updatingDestinationUi) {
                home.state.destinationPlan.all.origin = checked;
                destinationsChanged();
            }
        });
    }

    String folderPath(String value) {
        if (value == null || value.isEmpty()) return "Nessuna cartella scelta";
        Uri uri = Uri.parse(value);
        return Uri.decode(uri.getLastPathSegment());
    }

    void updateDestinationPanel() {
        originPanel.setInputs(home.state.selectedInputs, home.state.selectedNames, home.state.destinationPlan.effectiveOrigins(), home.state.destinationPlan.effectiveFolders());
    }

    void destinationsChanged() {
        home.state.originsReady = false;
        refreshLabels();
        updateDestinationPanel();
        originPanel.refresh();
        home.conversion.setBusy(home.state.busy);
        FolderPickerActivity.record(home.activity, "PIANO DESTINAZIONI · override_coda=" + home.state.destinationPlan.applyAll + " · video_visualizzato=" + (home.state.selectedVideoIndex + 1));
    }

    void launchFolderPicker(int request, String source) {
        if (home.state.busy || home.state.choosingOrigins) return;
        home.state.pendingOriginSource = source;
        home.state.choosingOrigins = true;
        home.conversion.setBusy(home.state.busy);
        FolderPickerActivity.record(home.activity, "RICHIESTA CARTELLA · tipo=" + request + " · video=" + source);
        try {
            home.activity.startActivityForResult(new Intent(home.activity, FolderPickerActivity.class), request);
        } catch (RuntimeException e) {
            cancelOrigins();
            home.toast("Selettore cartelle non avviato: " + e.getMessage());
            FolderPickerActivity.record(home.activity, "ERRORE apertura · " + e);
        }
    }

    void requestDirectoryAccess(String parent, String representative, boolean origin, String requestedFolder) {
        if (home.state.busy || home.state.choosingOrigins) return;
        if (!origin && (requestedFolder == null || requestedFolder.isEmpty())) {
            launchFolderPicker(home.state.destinationPlan.applyAll ? PICK_ALL_FOLDER : PICK_FOLDER, representative);
            return;
        }
        home.state.pendingOriginParent = parent;
        home.state.pendingFixedFolder = requestedFolder;
        home.state.pendingAuthorizationOrigin = origin;
        if (origin && parent == null) {
            new android.app.AlertDialog.Builder(home.activity).setTitle("Origine non rilevata").setMessage("Scegli la directory che contiene " + home.displayName(Uri.parse(representative)) + ". Verificherò anche gli altri video presenti nella stessa cartella.").setPositiveButton("Scegli cartella", (d,w)->launchFolderPicker(PICK_ORIGIN, representative)).setNegativeButton("Annulla", (d,w)->cancelOrigins()).show();
        } else launchFolderPicker(PICK_ORIGIN, representative);
    }

    void cancelOrigins() {
        home.state.choosingOrigins = false;
        home.state.pendingOriginParent = null;
        home.state.pendingOriginSource = null;
        home.state.pendingFixedFolder = null;
        home.state.pendingAuthorizationOrigin = false;
        home.conversion.setBusy(home.state.busy);
    }

    void refreshLabels() {
        home.screen.destinations.body.setVisibility(home.state.selectedInputs.isEmpty() ? View.GONE : View.VISIBLE);
        home.screen.files.videoLabel.setText(home.state.input == null ? "Nessun video selezionato" : home.state.sourceName);
        home.selection.updateVideoSelection(false);
        home.state.updatingDestinationUi = true;
        home.screen.destinations.applyAllDestinations.setChecked(home.state.destinationPlan.applyAll);
        home.screen.destinations.allOrigin.setChecked(home.state.destinationPlan.all.origin);
        home.screen.destinations.allFolderLabel.setText(home.state.destinationPlan.all.origin ? "Ogni video nella propria directory di origine" : folderPath(home.state.destinationPlan.all.folder));
        boolean selected = !home.state.selectedInputs.isEmpty() && home.state.selectedVideoIndex < home.state.destinationPlan.files.size();
        home.screen.destinations.videoDestinationTitle.setText(selected ? "DESTINAZIONE · VIDEO " + (home.state.selectedVideoIndex + 1) + " DI " + home.state.selectedInputs.size() : "DESTINAZIONE DEL VIDEO");
        home.screen.destinations.videoDestinationHelp.setText(home.state.destinationPlan.applyAll ? "È attiva la regola della coda. La scelta individuale qui sotto viene conservata." : "Questa scelta riguarda solo il video visualizzato.");
        home.screen.destinations.videoOrigin.setChecked(selected && home.state.destinationPlan.files.get(home.state.selectedVideoIndex).origin);
        home.screen.destinations.folderLabel.setText(selected ? (home.state.destinationPlan.files.get(home.state.selectedVideoIndex).origin ? "Directory di origine del video selezionato" : folderPath(home.state.destinationPlan.files.get(home.state.selectedVideoIndex).folder)) : "Seleziona prima un video");
        home.state.updatingDestinationUi = false;
    }

    void onFolderResult(int request, int code, Intent data) {
        if (request == PICK_FOLDER || request == PICK_ALL_FOLDER || request == PICK_ORIGIN) {
            String source = home.state.pendingOriginSource;
            String parent = home.state.pendingOriginParent;
            String requested = home.state.pendingFixedFolder;
            boolean sourceFolder = home.state.pendingAuthorizationOrigin;
            FolderPickerActivity.record(home.activity, "RISPOSTA ALLA HOME · tipo=" + request + " result=" + code + " uri=" + (data == null ? null : data.getData()));
            cancelOrigins();
            String error = data == null ? null : data.getStringExtra(FolderPickerActivity.ERROR);
            if (code != RESULT_OK || data == null || data.getData() == null) {
                home.toast(error != null ? error : "Il selettore Android non ha restituito una cartella. Ricontrollo i permessi già presenti.");
                originPanel.refresh();
                return;
            }
            Uri chosen = data.getData();
            if (request == PICK_ORIGIN) {
                if (sourceFolder && source != null) originPanel.accept(parent, source, chosen); else if (requested != null && !requested.isEmpty()) originPanel.acceptFixed(parent, requested, chosen); else originPanel.refresh();
                return;
            }
            if (request == PICK_ALL_FOLDER) home.state.destinationPlan.setAllFolder(chosen.toString()); else {
                int index = home.state.selectedInputs.indexOf(source);
                if (index < 0) {
                    home.toast("Il video della richiesta non è più nella coda");
                    return;
                }
                home.state.destinationPlan.setFolder(index, chosen.toString());
            }
            home.state.folder = chosen;
            home.activity.getPreferences(0).edit().putString("folder", chosen.toString()).apply();
            destinationsChanged();
            return;
        }
    }
}
