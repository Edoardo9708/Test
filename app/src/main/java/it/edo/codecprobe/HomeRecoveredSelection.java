package it.edo.codecprobe;

import android.content.Intent;
import android.net.Uri;
import java.util.ArrayList;

/** Explicit recovery action: restore the selection first, then load its missing metadata. */
final class HomeRecoveredSelection {
    private final HomeController home;
    HomeRecoveredSelection(HomeController home){this.home=home;}

    void restore(Intent request){
        if(home.state.destroyed)return;
        ArrayList<String> inputs=request.getStringArrayListExtra("inputs"),names=request.getStringArrayListExtra("names"),folders=request.getStringArrayListExtra("folders");
        String id=request.getStringExtra(SavedSession.RESTORE);
        if(id==null||inputs==null||names==null||folders==null||inputs.isEmpty()||inputs.size()!=names.size()||inputs.size()!=folders.size())return;
        if(home.state.metadataLoading)home.selection.stopMetadataLoading();
        ++home.state.transitionGeneration;
        home.screen.files.videoPreview.animate().cancel();home.screen.files.videoPreview.setAlpha(1);
        home.state.selectionSession=id;
        home.state.selectedInputs.clear();home.state.selectedInputs.addAll(inputs);
        home.state.selectedNames.clear();home.state.selectedNames.addAll(names);
        home.state.selectedVideoIndex=0;home.state.input=Uri.parse(inputs.get(0));home.state.sourceName=names.get(0);
        home.state.originFolders.clear();home.state.originFolders.addAll(folders);
        home.state.destinationNotes.clear();
        ArrayList<String> notes=request.getStringArrayListExtra("destinationNotes");if(notes!=null)home.state.destinationNotes.addAll(notes);
        home.state.destinationPlan.reset(inputs.size(),null);
        boolean[] origins=request.getBooleanArrayExtra("individualOrigins");ArrayList<String> individual=request.getStringArrayListExtra("individualFolders");
        if(origins==null||origins.length!=inputs.size()||individual==null||individual.size()!=inputs.size()){
            origins=new boolean[inputs.size()];individual=new ArrayList<>(folders);
        }
        home.state.destinationPlan.restore(origins,individual,request.getBooleanExtra("applyAllDestinations",false),
            request.getBooleanExtra("allOrigin",true),request.getStringExtra("allFolder"));
        home.destinations.refreshLabels();home.destinations.updateDestinationPanel();
        FolderPickerActivity.record(home.activity,"*** RIPRISTINO ELENCO DELLA SESSIONE ***\nVideo: "+inputs.size()+"\n1. Ripristino nomi e destinazioni salvati.\n2. Leggo in background soltanto i metadati mancanti.\nLa conversione usa la richiesta di ripresa già confermata.");
        home.selection.inspectInput();
    }
}
