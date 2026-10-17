package it.edo.codecprobe;

import java.util.ArrayList;

/** Audio draft only. No references to video selections, reports or saved conversion sessions. */
final class AudioState {
    final ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>();
    String folder;
    int bitrate=128;
    volatile boolean destroyed;
    int selectionGeneration;
}
