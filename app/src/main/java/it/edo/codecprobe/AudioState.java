package it.edo.codecprobe;

import java.util.ArrayList;

/** Audio draft only. No references to video selections, reports or saved conversion sessions. */
final class AudioState {
    final ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>();
    final ArrayList<String> lyricsInputs=new ArrayList<>(),lyricsNames=new ArrayList<>();
    String folder;
    int bitrate=128,selectedIndex;
    boolean onlySmaller=true,keepScreenOn,lyricsExpanded,preparing,exporting;
    String notice="";
    volatile boolean destroyed;
    volatile int selectionGeneration;
    boolean readingNames,readingLyrics;
    volatile int lyricsGeneration;
}
