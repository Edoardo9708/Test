package it.edo.codecprobe;

import java.util.ArrayList;

/** Independent image draft. No views, Activity references or running encoder state. */
final class ImageState {
    final ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>();
    final DestinationDraft destinations=new DestinationDraft();
    ImageDraftStore.Selection savedSelection;
    int selected;String name="",notice="";JpegInventory.Info info;
    boolean reading,exporting,starting,importing,bulk;String selectionLabel="";long startingAt,previousRun;ImageOptions options;
    void clamp(){while(names.size()<inputs.size())names.add("Foto "+(names.size()+1));while(names.size()>inputs.size())names.remove(names.size()-1);selected=Math.max(0,Math.min(selected,inputs.size()-1));}
}
