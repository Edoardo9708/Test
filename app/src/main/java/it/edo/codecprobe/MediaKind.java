package it.edo.codecprobe;

/** Labels only; all media use the same destination views and state transitions. */
enum MediaKind {
    VIDEO("Video", "questo video", "tutti i video"),
    AUDIO("Brano", "questo brano", "tutti i brani"),
    IMAGE("Foto", "questa foto", "tutte le foto");
    final String title, thisItem, allItems;
    MediaKind(String title, String thisItem, String allItems) {
        this.title=title;this.thisItem=thisItem;this.allItems=allItems;
    }
}
