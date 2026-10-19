package it.edo.codecprobe;

import java.util.ArrayList;

/** Independent image draft. No views, Activity references or running encoder state. */
final class ImageState {
    final ArrayList<String> inputs=new ArrayList<>();
    int selected;String name="",notice="";JpegInventory.Info info;
    boolean reading,exporting,starting;long startingAt,previousRun;ImageOptions options;
    void clamp(){selected=Math.max(0,Math.min(selected,inputs.size()-1));}
}
