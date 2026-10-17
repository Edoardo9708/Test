package it.edo.codecprobe;

import android.os.Bundle;

/** Video-specific accordion labels; view construction is shared with Audio. */
final class HomeSections extends AppSections {
    HomeSections(AppUi ui,Bundle saved,boolean active){super(ui,saved,active?MainActivity.CARD_CONVERSION:null,HomeSections::description);}
    private static String description(String title){
        if(title.startsWith("01"))return "Video selezionati, anteprima e cartelle";
        if(title.startsWith("02"))return "Qualità, preset e uso della CPU";
        if(title.startsWith("03"))return "Avanzamento, sistema e risultati";
        return "Log, metadati e prove HDR";
    }
}
