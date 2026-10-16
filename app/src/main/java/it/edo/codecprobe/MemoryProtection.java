package it.edo.codecprobe;

/** Per-file UHD policy; never modifies the user's queue or saved defaults. */
final class MemoryProtection {
    static boolean required(int width,int height){return width>=3840||height>=3840;}
    static String cpuLabel(boolean active,int requested){
        if(active)return "Limited · protezione memoria 4K";
        String[] modes={"Limited","Auto","Max"};
        return requested>=0&&requested<modes.length?modes[requested]:"—";
    }
    static String description(){return "CPU Limitata: 2 frame thread, 2 worker\nB-frame: 4\nLookahead: 5 fotogrammi\nCU-tree: OFF\nCRF, preset e AQ: quelli selezionati\nProfilo a consumo ridotto; nessun tetto RAM garantito.";}
    private MemoryProtection(){}
}
