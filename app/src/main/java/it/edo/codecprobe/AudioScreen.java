package it.edo.codecprobe;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;

/** Audio-specific controls follow the same four-section flow as Video. No file I/O here. */
final class AudioScreen {
    static final String FILES="01  FILE AUDIO E DESTINAZIONE",COMPRESSION="02  COMPRESSIONE AUDIO",CONVERSION="03  CONVERSIONE AUDIO",DIAGNOSTICS="DIAGNOSTICA";
    final AppUi ui;final AppSections sections;final AppNavigation navigation;final LinearLayout page;
    final Button settings,pick,clear,chooseFolder,start,cancel,exportLog,clearLog,pickLyrics,clearLyrics,remove,details,lyricsToggle,retry,toggleLog;
    final TextView selected,names,folder,bitrateLabel,status,summary,current,percent,seconds,elapsed,parameters,log,lyrics,inputInfo,inputDetails,totalProgress,savings;
    final java.util.ArrayList<Button> presets=new java.util.ArrayList<>();
    final SeekBar bitrate;final ProgressBar progress;final LinearLayout report,navigator,lyricsPanel;
    final HorizontalScrollView carousel;final CheckBox onlySmaller,keepScreenOn;
    AudioScreen(AppUi ui,Bundle saved,AudioState state){
        this.ui=ui;page=ui.newPage();settings=AppHeader.add(ui,page,"Converti audio","Opus · VBR · v"+BuildConfig.VERSION_NAME);settings.setText("Qualità audio");
        sections=new AppSections(ui,saved,null,title->title.equals(FILES)?"Brani, testi opzionali e destinazione":title.equals(COMPRESSION)?"Qualità e bitrate":title.equals(CONVERSION)?"Avanzamento e risultati":"Esportazione log e informazioni sul motore audio");
        LinearLayout files=sections.card(page,FILES),buttons=ui.row(files);
        pick=ui.halfButton(buttons,"Seleziona audio",true);clear=ui.halfButton(buttons,"Svuota tutto",false);clear.setContentDescription("Svuota selezione e resoconto senza eliminare audio o log");
        selected=ui.text(files,"Nessun brano selezionato",15);
        carousel=new HorizontalScrollView(ui.activity);navigator=new LinearLayout(ui.activity);carousel.addView(navigator,new HorizontalScrollView.LayoutParams(-2,-2));files.addView(carousel,new LinearLayout.LayoutParams(-1,-2));
        names=ui.text(files,"Nessun brano selezionato",22);names.setTypeface(null,Typeface.BOLD);names.setMaxLines(3);names.setEllipsize(android.text.TextUtils.TruncateAt.END);
        inputInfo=ui.text(files,"Seleziona uno o più brani",14);
        LinearLayout fileActions=ui.row(files);details=ui.halfButton(fileActions,"Dettagli brano",false);remove=ui.halfButton(fileActions,"Rimuovi brano",false);
        inputDetails=ui.text(files,"",14);inputDetails.setVisibility(View.GONE);inputDetails.setTextIsSelectable(true);
        lyricsToggle=ui.button(files,"▸ Testi esterni .lrc · opzionali");lyrics=ui.text(files,"",14);
        lyricsPanel=new LinearLayout(ui.activity);lyricsPanel.setOrientation(LinearLayout.VERTICAL);files.addView(lyricsPanel);lyricsPanel.setVisibility(state.lyricsExpanded?View.VISIBLE:View.GONE);
        LinearLayout lyricActions=ui.row(lyricsPanel);pickLyrics=ui.halfButton(lyricActions,"Seleziona testi .lrc",false);clearLyrics=ui.halfButton(lyricActions,"Svuota testi",false);
        ui.text(lyricsPanel,"Seleziona i .lrc con lo stesso nome dei brani: saranno copiati accanto all’Opus. La selezione dei soli audio non autorizza l’accesso ai file vicini.",13);
        ui.divider(files);chooseFolder=ui.button(files,"Scegli cartella di destinazione");folder=ui.text(files,"Nessuna cartella scelta",14);
        ui.text(files,"Il nome originale viene mantenuto con estensione .opus. Se esiste già, aggiungo un numero anche al testo abbinato. Gli originali restano conservati.",13);
        LinearLayout compression=sections.card(page,COMPRESSION);
        ui.parameterRow(compression,"Opus · bitrate variabile (VBR)","Il bitrate è un obiettivo medio: aumentandolo si dedica più spazio all’audio. La dimensione finale dipende dal contenuto. Opus usa compressione con perdita; la conversione non conserva esattamente i campioni originali.");
        bitrateLabel=ui.text(compression,"",20);bitrate=new SeekBar(ui.activity);bitrate.setMax(24);ui.styleSlider(bitrate);compression.addView(bitrate,new LinearLayout.LayoutParams(-1,-2));bitrate.setProgress((state.bitrate-64)/8);
        ui.text(compression,"Bitrate · 64–256 kbit/s",13);LinearLayout presetRow=ui.row(compression);
        for(int value:new int[]{96,128,160}){Button b=ui.halfButton(presetRow,value+" kbit/s",false);presets.add(b);b.setOnClickListener(v->bitrate.setProgress((value-64)/8));}
        ui.text(compression,"FLAC, MP3 e M4A → Opus. Ricodificare MP3 o AAC può ridurre la qualità senza risparmiare spazio. Tag e copertine vengono copiati e verificati.",14);
        LinearLayout conversion=sections.card(page,CONVERSION),actions=ui.row(conversion);
        start=ui.halfButton(actions,"Converti e salva",true);cancel=ui.halfButton(actions,"Annulla",false);ui.danger(cancel);
        keepScreenOn=check(ui,conversion,"Mantieni schermo acceso",state.keepScreenOn);
        onlySmaller=check(ui,conversion,"Salva solo se più piccolo",state.onlySmaller);
        ui.sectionHeading(conversion,"SOMMARIO");summary=ui.text(conversion,"Nessuna coda avviata",16);totalProgress=ui.dashboardRow(conversion,"Avanzamento coda","—");elapsed=ui.dashboardRow(conversion,"Tempo trascorso","00:00:00");
        ui.sectionHeading(conversion,"PROGRESSO · FILE ATTUALE");current=ui.text(conversion,"Nessun file in conversione",16);current.setTypeface(null,Typeface.BOLD);percent=ui.text(conversion,"0%",28);
        progress=new ProgressBar(ui.activity,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(ui.accent));progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(ui.border));conversion.addView(progress,new LinearLayout.LayoutParams(-1,ui.dp(12)));
        seconds=ui.dashboardRow(conversion,"Secondi di audio","—");
        status=ui.text(conversion,"Scegli audio, cartella e qualità.",14);ui.sectionHeading(conversion,"PARAMETRI IN USO");parameters=ui.text(conversion,"—",14);
        retry=ui.button(conversion,"Riprova i falliti");retry.setVisibility(View.GONE);
        ui.sectionHeading(conversion,"REPORT LAVORI");savings=ui.text(conversion,"",16);report=new LinearLayout(ui.activity);report.setOrientation(LinearLayout.VERTICAL);conversion.addView(report,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout diagnostics=sections.card(page,DIAGNOSTICS),tools=ui.row(diagnostics);
        exportLog=ui.halfButton(tools,"Esporta log TXT",false);clearLog=ui.halfButton(tools,"Cancella log audio",false);ui.danger(clearLog);
        toggleLog=ui.button(diagnostics,"Mostra ultimi messaggi");log=ui.text(diagnostics,"Nessun log audio",13);log.setTextIsSelectable(true);log.setMaxLines(16);log.setVisibility(View.GONE);
        ui.parameterRow(diagnostics,"Dettagli del motore audio","FLAC: libFLAC incluso, input da 8 a 192 kHz fino a 32 bit. MP3/M4A: decoder Android; ALAC dipende dal dispositivo. Opus: libopus 1.6.1 e libopusenc 0.3 incluse, VBR libero, complessità 10, output a 48 kHz senza normalizzazione. Verifica finale con il decoder Android. File DRM esclusi.\n\nLa coda continua uscendo dalla schermata, ma non riparte automaticamente se Android termina il processo. La selezione e le impostazioni vengono ricordate.\n\nSalva solo se più piccolo: confronto dopo codifica e verifica, inclusi tag e copertine. Se non c’è risparmio o la dimensione originale non è disponibile, il risultato non viene salvato. I conteggi in byte riguardano i file audio; i .lrc sono copiati invariati.\n\nLa percentuale di coda assegna lo stesso peso a ogni brano: non è una stima del tempo rimanente.");
        navigation=new AppNavigation(ui,true);ui.install(page,navigation);
    }
    private static CheckBox check(AppUi ui,LinearLayout parent,String label,boolean selected){CheckBox c=new CheckBox(ui.activity);c.setText(label);c.setTextColor(ui.ink);c.setTextSize(17);c.setMinHeight(ui.dp(48));c.setButtonTintList(android.content.res.ColorStateList.valueOf(ui.accent));c.setChecked(selected);parent.addView(c);return c;}
}
