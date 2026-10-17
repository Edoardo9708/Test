package it.edo.codecprobe;

import android.os.Bundle;
import android.widget.*;

/** Audio views share Video's cards, visual components and dashboard layout. */
final class AudioScreen {
    static final String FILES="01  FILE AUDIO E DESTINAZIONE",COMPRESSION="02  COMPRESSIONE AUDIO",CONVERSION="03  CONVERSIONE AUDIO";
    final AppUi ui;final AppSections sections;final AppNavigation navigation;
    final Button settings,pick,clear,chooseFolder,start,cancel,exportLog,clearReport,pickLyrics,clearLyrics;
    final TextView selected,names,folder,bitrateLabel,status,summary,current,percent,samples,seconds,elapsed,parameters,log,lyrics;
    final java.util.ArrayList<Button> presets=new java.util.ArrayList<>();
    final SeekBar bitrate;final ProgressBar progress;final LinearLayout report;
    AudioScreen(AppUi ui,Bundle saved,AudioState state){
        this.ui=ui;LinearLayout page=ui.newPage();settings=AppHeader.add(ui,page,"Converti audio","Audio · Opus · v"+BuildConfig.VERSION_NAME+" · prova preliminare");
        sections=new AppSections(ui,saved,null,title->title.equals(FILES)?"Brani selezionati e cartella audio":title.equals(COMPRESSION)?"Impostazioni audio indipendenti":"Progresso, report e log audio");
        LinearLayout files=sections.card(page,FILES),buttons=ui.row(files);
        pick=ui.halfButton(buttons,"Seleziona audio",true);clear=ui.halfButton(buttons,"Svuota selezione",false);
        selected=ui.text(files,"Nessun brano selezionato",18);names=ui.text(files,"",14);
        LinearLayout lyricActions=ui.row(files);pickLyrics=ui.halfButton(lyricActions,"Seleziona testi .lrc",false);clearLyrics=ui.halfButton(lyricActions,"Svuota testi",false);
        lyrics=ui.text(files,"",14);
        ui.text(files,"Per i testi esterni seleziona anche i file .lrc con lo stesso nome del FLAC. Vengono abbinati e copiati accanto all’Opus, senza cambiare testo, tempi o codifica.",13);
        ui.divider(files);chooseFolder=ui.button(files,"Scegli cartella audio");folder=ui.text(files,"Nessuna cartella scelta",14);
        LinearLayout compression=sections.card(page,COMPRESSION);
        ui.parameterRow(compression,"Opus · VBR","libopus 1.6.1 + libopusenc 0.3 incluse nell’app. VBR libero, complessità 10. Il bitrate indica un obiettivo medio, non una dimensione garantita. Opus è lossy: non mantiene esattamente il PCM del FLAC.");
        bitrateLabel=ui.text(compression,"",18);bitrate=new SeekBar(ui.activity);bitrate.setMax(24);ui.styleSlider(bitrate);
        compression.addView(bitrate,new LinearLayout.LayoutParams(-1,-2));bitrate.setProgress((state.bitrate-64)/8);
        ui.text(compression,"Scegli il bitrate · 64–256 kbit/s",13);
        LinearLayout presetRow=ui.row(compression);
        for(int value:new int[]{96,128,160}){Button b=ui.halfButton(presetRow,value+" kbit/s",false);presets.add(b);b.setOnClickListener(v->bitrate.setProgress((value-64)/8));}
        ui.parameterRow(compression,"FLAC mono / stereo → .opus","Input FLAC nativo da 8 a 192 kHz, fino a 32 bit; conversione a float, senza normalizzazione. libopusenc ricampiona quando serve. Output Ogg Opus a 48 kHz, con lo stesso numero di canali. Altri formati e audio multicanale richiederanno un passaggio successivo.");
        ui.text(compression,"PROVA DEL MOTORE\nTag e copertine vengono copiati e verificati. I testi .lrc esterni richiedono la selezione dedicata sopra. Conserva gli originali durante queste prove. La coda audio non è ancora ripristinabile dopo la chiusura del processo.",14);
        LinearLayout conversion=sections.card(page,CONVERSION),actions=ui.row(conversion);
        start=ui.halfButton(actions,"Converti audio",true);cancel=ui.halfButton(actions,"Annulla…",false);ui.danger(cancel);
        LinearLayout tools=ui.row(conversion);exportLog=ui.halfButton(tools,"Salva log TXT",false);clearReport=ui.halfButton(tools,"Svuota report",false);
        status=ui.text(conversion,"",16);
        ui.sectionHeading(conversion,"SOMMARIO");summary=ui.dashboardRow(conversion,"Brani e risultati","—");elapsed=ui.dashboardRow(conversion,"Tempo trascorso","—");
        ui.sectionHeading(conversion,"PROGRESSO");current=ui.dashboardRow(conversion,"File attuale","—");percent=ui.dashboardRow(conversion,"Avanzamento file","—");
        progress=new ProgressBar(ui.activity,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(ui.accent));conversion.addView(progress,new LinearLayout.LayoutParams(-1,ui.dp(12)));
        samples=ui.dashboardRow(conversion,"Campioni / canale","—");seconds=ui.dashboardRow(conversion,"Secondi convertiti / totali","—");
        ui.sectionHeading(conversion,"PARAMETRI DELLA CONVERSIONE");parameters=ui.text(conversion,"—",14);
        ui.sectionHeading(conversion,"REPORT LAVORI");report=new LinearLayout(ui.activity);report.setOrientation(LinearLayout.VERTICAL);conversion.addView(report,new LinearLayout.LayoutParams(-1,-2));
        ui.sectionHeading(conversion,"ULTIMI PASSAGGI DEL LOG");log=ui.text(conversion,"Nessun log audio",13);log.setTextIsSelectable(true);
        navigation=new AppNavigation(ui,true);ui.install(page,navigation);
    }
}
