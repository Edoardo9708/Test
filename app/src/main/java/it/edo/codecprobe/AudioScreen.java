package it.edo.codecprobe;

import android.os.Bundle;
import android.widget.*;

/** Audio views use the same header, cards, buttons, sliders and navigation as Video. */
final class AudioScreen {
    static final String FILES="01  FILE AUDIO E DESTINAZIONE",COMPRESSION="02  COMPRESSIONE AUDIO",CONVERSION="03  CONVERSIONE AUDIO";
    final AppSections sections;final AppNavigation navigation;
    final Button settings,pick,clear,chooseFolder;final TextView selected,names,folder,bitrateLabel,status;final SeekBar bitrate;
    AudioScreen(AppUi ui,Bundle saved,AudioState state){
        LinearLayout page=ui.newPage();settings=AppHeader.add(ui,page,"Converti audio","Audio · Opus · v"+BuildConfig.VERSION_NAME);
        sections=new AppSections(ui,saved,null,title->title.equals(FILES)?"Brani selezionati e cartella audio":title.equals(COMPRESSION)?"Impostazioni audio indipendenti":"Stato del modulo audio");
        LinearLayout files=sections.card(page,FILES),buttons=ui.row(files);
        pick=ui.halfButton(buttons,"Seleziona audio",true);clear=ui.halfButton(buttons,"Svuota selezione",false);
        selected=ui.text(files,"Nessun brano selezionato",18);names=ui.text(files,"",14);
        ui.divider(files);chooseFolder=ui.button(files,"Scegli cartella audio");folder=ui.text(files,"Nessuna cartella scelta",14);
        LinearLayout compression=sections.card(page,COMPRESSION);
        ui.parameterRow(compression,"Opus · VBR","Formato previsto per il modulo audio. Questa versione prepara la selezione e le impostazioni; la codifica non è ancora disponibile.");
        bitrateLabel=ui.text(compression,"",18);bitrate=new SeekBar(ui.activity);bitrate.setMax(24);ui.styleSlider(bitrate);
        compression.addView(bitrate,new LinearLayout.LayoutParams(-1,-2));bitrate.setProgress((state.bitrate-64)/8);
        ui.text(compression,"Bitrate previsto da 64 a 256 kbit/s. Valore iniziale: 128 kbit/s.",13);
        LinearLayout conversion=sections.card(page,CONVERSION);
        status=ui.text(conversion,"Selezione dei brani e destinazione disponibili. La conversione audio sarà disponibile nel prossimo aggiornamento.",15);
        navigation=new AppNavigation(ui,true);ui.install(page,navigation);
    }
}
