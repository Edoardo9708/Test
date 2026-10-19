package it.edo.codecprobe;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;

/** Audio-specific controls follow the same four-section flow as Video. No file I/O here. */
final class AudioScreen {
    static final String FILES="01  FILE AUDIO E DESTINAZIONE",COMPRESSION="02  COMPRESSIONE AUDIO",CONVERSION="03  CONVERSIONE AUDIO",DIAGNOSTICS="DIAGNOSTICA";
    final AppUi ui;final AppSections sections;final AppNavigation navigation;final LinearLayout page;
    final Button settings,pick,clear,chooseFolder,start,cancel,exportLog,clearLog,pickLyrics,clearLyrics,remove,details,reload,lyricsToggle,retry,toggleLog;
    final TextView selected,names,folder,bitrateLabel,status,summary,current,percent,seconds,elapsed,parameters,log,lyrics,inputInfo,inputDetails,totalProgress,savings;
    final java.util.ArrayList<Button> presets=new java.util.ArrayList<>();
    final SeekBar bitrate;final ProgressBar progress;final LinearLayout report,navigator,lyricsPanel;
    final WorkReportView workReport;
    final ConversionDashboard dashboard;
    final SelectedItemView selectedItem;
    final MediaDestinationsView destinations;
    final HorizontalScrollView carousel;final CheckBox onlySmaller,keepScreenOn;
    AudioScreen(AppUi ui,Bundle saved,AudioState state){
        this.ui=ui;page=ui.newPage();settings=AppHeader.add(ui,page,"Converti audio","Opus · VBR · v"+BuildConfig.VERSION_NAME);
        sections=new AppSections(ui,saved,null,title->title.equals(FILES)?"Brani, testi opzionali e destinazione":title.equals(COMPRESSION)?"Qualità e bitrate":title.equals(CONVERSION)?"Avanzamento e risultati":"Esportazione log e informazioni sul motore audio");
        LinearLayout files=sections.card(page,FILES),buttons=ui.row(files);
        pick=ui.halfButton(buttons,"Seleziona audio",true);clear=ui.halfButton(buttons,"Svuota tutto",false);clear.setContentDescription("Svuota selezione e resoconto senza eliminare audio o log");
        selectedItem=new SelectedItemView(ui,files,"Brano","Nessun brano selezionato","Dettagli brano");
        selected=selectedItem.number;carousel=selectedItem.carousel;navigator=selectedItem.navigator;
        names=selectedItem.name;inputInfo=selectedItem.summary;
        details=selectedItem.details;remove=selectedItem.remove;
        inputDetails=ui.text(selectedItem.detailsBody,"",15);inputDetails.setTextIsSelectable(true);inputDetails.setVisibility(View.GONE);
        reload=ui.button(files,"Rileggi metadati");
        lyricsToggle=ui.button(files,"▸ Testi esterni .lrc · opzionali");lyrics=ui.text(files,"",14);
        lyricsPanel=new LinearLayout(ui.activity);lyricsPanel.setOrientation(LinearLayout.VERTICAL);files.addView(lyricsPanel);lyricsPanel.setVisibility(state.lyricsExpanded?View.VISIBLE:View.GONE);
        LinearLayout lyricActions=ui.row(lyricsPanel);pickLyrics=ui.halfButton(lyricActions,"Seleziona testi .lrc",false);clearLyrics=ui.halfButton(lyricActions,"Svuota testi",false);
        ui.text(lyricsPanel,"Seleziona i .lrc con lo stesso nome dei brani: saranno copiati accanto all’Opus. La selezione dei soli audio non autorizza l’accesso ai file vicini.",13);
        destinations=new MediaDestinationsView(ui,files,MediaKind.AUDIO);chooseFolder=destinations.pickFolder;folder=destinations.folderLabel;
        ui.text(destinations.body,"Il nome originale viene mantenuto con estensione .opus. Se esiste già, aggiungo un numero anche al testo abbinato. Gli originali restano conservati.",13);
        LinearLayout compression=sections.card(page,COMPRESSION);
        ui.parameterRow(compression,"Opus · bitrate variabile (VBR)","Il bitrate è un obiettivo medio: aumentandolo si dedica più spazio all’audio. La dimensione finale dipende dal contenuto. Opus usa compressione con perdita; la conversione non conserva esattamente i campioni originali.");
        bitrateLabel=ui.text(compression,"",20);bitrate=new SeekBar(ui.activity);bitrate.setMax(24);ui.styleSlider(bitrate);compression.addView(bitrate,new LinearLayout.LayoutParams(-1,-2));bitrate.setProgress((state.bitrate-64)/8);
        ui.text(compression,"Bitrate · 64–256 kbit/s",13);
        QualityButtons shortcuts=new QualityButtons(ui,new int[]{96,128,160},new String[]{"96 kbit/s","128 kbit/s","160 kbit/s"},"bitrate",value->bitrate.setProgress((value-64)/8));compression.addView(shortcuts,new LinearLayout.LayoutParams(-1,-2));presets.addAll(shortcuts.buttons);
        ui.text(compression,"FLAC, MP3 e M4A → Opus. Ricodificare MP3 o AAC può ridurre la qualità senza risparmiare spazio. Tag e copertine vengono copiati e verificati.",14);
        LinearLayout conversion=sections.card(page,CONVERSION),actions=ui.row(conversion);
        start=ui.halfButton(actions,"Converti e salva",true);cancel=ui.halfButton(actions,"Annulla",false);ui.danger(cancel);
        ConversionOptionsView options=new ConversionOptionsView(ui,conversion,state.keepScreenOn,state.onlySmaller);keepScreenOn=options.screenOn;onlySmaller=options.smaller;
        dashboard=new ConversionDashboard(ui,conversion,new String[]{"Secondi di audio"},new String[]{"Solo se più piccolo","Bitrate","Modalità","Frequenza output"});
        summary=dashboard.summary;totalProgress=dashboard.totalProgress;elapsed=dashboard.elapsed;current=dashboard.current;percent=dashboard.percent;progress=dashboard.progress;seconds=dashboard.measures.get("Secondi di audio");status=dashboard.status;
        parameters=ui.text(conversion,"—",14);parameters.setVisibility(View.GONE);
        retry=ui.button(conversion,"Riprova i falliti");retry.setVisibility(View.GONE);
        workReport=new WorkReportView(ui,conversion);savings=workReport.summary;report=workReport.rows;
        LinearLayout diagnostics=sections.card(page,DIAGNOSTICS);
        exportLog=ui.button(diagnostics,"Esporta log");
        ui.parameterRow(diagnostics,"Dettagli del motore audio","FLAC: libFLAC incluso, input da 8 a 192 kHz fino a 32 bit. MP3/M4A: decoder Android; ALAC dipende dal dispositivo. Opus: libopus 1.6.1 e libopusenc 0.3 incluse, VBR libero, complessità 10, output a 48 kHz senza normalizzazione. Verifica finale con il decoder Android. File DRM esclusi.\n\nLa coda continua uscendo dalla schermata, ma non riparte automaticamente se Android termina il processo. La selezione e le impostazioni vengono ricordate.\n\nSalva solo se più piccolo: confronto dopo codifica e verifica, inclusi tag e copertine. Se non c’è risparmio o la dimensione originale non è disponibile, il risultato non viene salvato. I conteggi in byte riguardano i file audio; i .lrc sono copiati invariati.\n\nLa percentuale di coda assegna lo stesso peso a ogni brano: non è una stima del tempo rimanente.");
        LogPanelView logPanel=new LogPanelView(ui,diagnostics,"Cancella log audio","Cancella il log audio nell’app. I file audio, il resoconto e i TXT già esportati restano salvati.");clearLog=logPanel.clear;toggleLog=logPanel.toggle;log=logPanel.log;
        navigation=new AppNavigation(ui,true);ui.install(page,navigation);
    }
}
