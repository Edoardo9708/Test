package it.edo.codecprobe;

import android.os.Bundle;
import android.widget.*;

/** Image view composition only; file and engine actions belong to separate coordinators. */
final class ImageScreen {
    static final String FILES="01  FOTO E DESTINAZIONE",COMPRESSION="02  COMPRESSIONE IMMAGINI",HEIC="03  AVANZAMENTO IMMAGINI",DETAILS="DIAGNOSTICA";
    final ImageWorkspaceLayout layout;
    final AppUi ui;final LinearLayout page;final ImageSteps sections;final AppNavigation navigation;final ImageQualityControls quality;
    final Button settings,select,importFolder,retryFailures,clear,reload,remove,export,chooseFolder,checkMetadata,convert,cancel,pause,resumeSession,discardSession,metadata,exportLog,exportErrors,clearLog,toggleLog;
    final CheckBox smaller,screenOn;final ProgressBar progress;
    final TextView position,name,summary,selectionSummary,details,status,folder,parameters,runStatus,log;
    final WorkReportView workReport;
    final ConversionDashboard dashboard;
    final SelectedItemView selectedItem;
    final MediaDestinationsView destinations;
    ImageScreen(AppUi ui,Bundle saved){
        this.ui=ui;page=ui.newPage();settings=AppHeader.add(ui,page,"Converti immagini","JPEG → HEIC · x265 · v"+BuildConfig.VERSION_NAME);
        settings.setText("");settings.setContentDescription("Impostazioni immagini");settings.setPadding(ui.dp(10),ui.dp(10),ui.dp(10),ui.dp(10));
        settings.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)));
        android.graphics.drawable.Drawable settingsIcon=ui.activity.getDrawable(R.drawable.trial_settings).mutate();settingsIcon.setTint(ui.accent);settingsIcon.setBounds(0,0,ui.dp(24),ui.dp(24));settings.setCompoundDrawables(settingsIcon,null,null,null);
        sections=new ImageSteps(ui,page,saved);
        LinearLayout files=sections.card(page,FILES),importsPanel=ImageSteps.group(ui,files),actions=ui.row(importsPanel);
        select=ui.halfButton(actions,"Aggiungi\nfile",true);importFolder=ui.halfButton(actions,"Leggi\ncartella",true);clear=ui.halfButton(actions,"Svuota",false);
        LinearLayout photoPanel=ImageSteps.group(ui,files);
        selectionSummary=ui.text(photoPanel,"",14);
        selectedItem=new SelectedItemView(ui,photoPanel,"Foto","Nessuna foto selezionata","Dettagli foto",true);position=selectedItem.number;name=selectedItem.name;summary=selectedItem.summary;
        metadata=selectedItem.details;remove=selectedItem.remove;
        details=ui.text(selectedItem.detailsBody,"Seleziona una foto per leggere i metadati.",15);details.setTextIsSelectable(true);details.setVisibility(android.view.View.GONE);
        ((android.view.ViewGroup)remove.getParent()).removeView(remove);((android.view.ViewGroup)metadata.getParent()).removeView(metadata);
        LinearLayout photoActions=ui.row(photoPanel);photoActions.addView(remove,new LinearLayout.LayoutParams(0,-2,1));
        reload=ui.halfButton(photoActions,"Rileggi",false);photoActions.addView(metadata,new LinearLayout.LayoutParams(0,-2,1));remove.setText("Rimuovi");metadata.setText("Dettagli");
        ((android.view.ViewGroup)selectedItem.detailsBody.getParent()).removeView(selectedItem.detailsBody);photoPanel.addView(selectedItem.detailsBody);
        status=ui.text(photoPanel,"Scegli le foto. Gli originali restano invariati.",14);
        LinearLayout savingPanel=ImageSteps.group(ui,files);ui.text(savingPanel,"Salvataggio",21).setAccessibilityHeading(true);
        destinations=new MediaDestinationsView(ui,savingPanel,MediaKind.IMAGE);chooseFolder=destinations.pickFolder;folder=destinations.folderLabel;
        ui.text(destinations.body,"Il nome originale viene mantenuto con estensione .heic. In caso di omonimi viene aggiunto un numero; gli originali sono conservati.",13);
        ConversionOptionsView options=new ConversionOptionsView(ui,savingPanel,false,true);smaller=options.smaller;screenOn=options.screenOn;
        LinearLayout compression=sections.card(page,COMPRESSION);quality=new ImageQualityControls(ui,compression,()->{});
        LinearLayout output=sections.card(page,HEIC);
        checkMetadata=ui.button(output,"Verifica compatibilità dei metadati");retryFailures=ui.button(output,"Carica foto fallite");
        ui.text(output,"La verifica legge i metadati senza convertire. I pixel saranno verificati durante la conversione.",13);
        LinearLayout buttons=ui.row(output);convert=ui.halfButton(buttons,"Converti e salva",true);cancel=ui.halfButton(buttons,"Annulla",false);ui.danger(cancel);pause=ui.button(output,"Pausa");
        LinearLayout recovery=ui.row(output);resumeSession=ui.halfButton(recovery,"Riprendi coda interrotta",true);discardSession=ui.halfButton(recovery,"Scarta coda interrotta",false);
        ui.text(output,"Pausa completa la foto in corso, poi attende. Riprendi continua dalla foto successiva, senza ricreare la coda.",13);

        dashboard=new ConversionDashboard(ui,output,new String[]{},new String[]{"Solo se più piccolo","Qualità","CRF","Preset","Tune","Formato","Dimensioni"});progress=dashboard.progress;runStatus=dashboard.status;
        parameters=ui.text(output,"",14);parameters.setVisibility(android.view.View.GONE);workReport=new WorkReportView(ui,output);
        LinearLayout diagnostic=sections.card(page,DETAILS),exports=ui.row(diagnostic);exportLog=ui.halfButton(exports,"Esporta log",false);export=ui.halfButton(exports,"Metadati foto · JSON",false);
        exportErrors=ui.button(diagnostic,"Esporta archivio errori");
        ui.text(diagnostic,"Puoi esportare log e archivio errori anche durante la conversione. Viene salvata una copia degli eventi già registrati al momento della richiesta; la registrazione prosegue mentre scegli la destinazione. Il file in corso potrebbe non avere ancora un esito nel log.",13);
        ui.text(diagnostic,"Il report include i blocchi metadati originali, che possono contenere posizione GPS e altri dati personali. Non include i pixel della foto.",13);
        ui.parameterRow(diagnostic,"Dettagli del motore immagini","JPEG YCbCr a 8 bit, lati 64–16384 pixel, massimo 48 megapixel e file di 64 MiB. Decodifica JPEG e verifica HEIC con FFmpeg incluso; contenitore libheif, codifica x265. Nessun ridimensionamento, rotazione dei pixel o normalizzazione. Conversione con perdita. EXIF/ICC standard conservati e riletti; XMP conservato o adattato per Motion Photo, con pacchetto originale archiviato; i blocchi APP/COM originali e il SEF foto supportato sono conservati anche in un archivio interno. Le funzioni dell’editor Samsung non sono garantite. Altri APP/COM proprietari restano archiviati senza interpretazione. Foto principali CMYK/RGB o in scala di grigi, HDR/MPF diversi dalla mappa Apple v1 riconosciuta, XMP esteso e SEF non supportato vengono rifiutati. Mappa HDR Apple v1: immagine ausiliaria monocromatica HEVC lossless, XMP originale e collegamento auxl/auxC; pixel della mappa riletti e confrontati esattamente. JPEG ausiliario originale conservato anche nell’archivio. Resa HDR da verificare sul dispositivo. APP11/JUMBF C2PA riconosciuto: manifesto originale archiviato, firma non verificata; nessuna certificazione C2PA del nuovo HEIC. Motion Photo JPEG con video MP4 SEF: filmato originale copiato senza ricodifica nel box HEIC mpvd finale; XMP aggiornato e archivio CPJPEG2/3 senza duplicazione del video. SEF massimo 52 MiB complessivi, video Motion Photo massimo 48 MiB, nel limite file di 64 MiB. Per code oltre 4 MiB (video escluso), archivio UUID CPJPEG3 separato e verificato, senza copia nell’item metadati HEIF. Prefisso SEF tutto zero ammesso fino a 64 KiB; buchi interni e sovrapposizioni rifiutati. Playback da verificare nella galleria del dispositivo. EXIF orientamento 0: nessuna rotazione applicata, valore originale conservato.\n\nSelezione manuale fino a 5.000 foto. Importazione automatica della cartella JPEG, senza sottocartelle: nessuna selezione manuale dei lotti. Il registro su disco mantiene un solo esito per originale; originale e risultato già presenti vengono verificati con SHA-256 prima di evitare una nuova codifica. «Carica fallite» riporta in selezione gli ultimi fallimenti del registro, con le destinazioni conservate e le impostazioni di compressione correnti. Carosello: prime 100 e puntini. Risultati: finestra di 100 righe; la corrente è al 50º posto quando ci sono abbastanza elementi prima e dopo. Totali cumulativi su tutta la coda. Log completo dell’ultima esecuzione su disco, anteprima di 24.000 caratteri in RAM. Archivio errori persistente ed esportabile, separato dal report. La lettura dei metadati di anteprima non blocca l’avvio; ogni foto viene controllata integralmente prima della codifica.\n\nLa coda continua uscendo dalla pagina. Se Android termina il processo, «Riprendi coda interrotta» recupera impostazioni, posizione e totali dall’ultimo salvataggio. I dettagli precedenti restano nel log. Interrompi agisce alla fine della fase nativa in corso; non interrompe x265 a metà fotogramma.");
        LogPanelView logPanel=new LogPanelView(ui,diagnostic,"Cancella log immagini","Cancella il log immagini nell’app. Le foto, il resoconto e i TXT già esportati restano salvati.");clearLog=logPanel.clear;toggleLog=logPanel.toggle;log=logPanel.log;
        Button diagnosticButton=ui.button(output,"Diagnostica e log");diagnosticButton.setOnClickListener(v->sections.focusCard(DETAILS));
        TrialIcons.set(ui,select,R.drawable.trial_add,true);TrialIcons.set(ui,importFolder,R.drawable.trial_folder,true);TrialIcons.set(ui,clear,R.drawable.trial_bin,false);
        for(Button button:new Button[]{select,importFolder,clear}){
            android.graphics.drawable.Drawable icon=button.getCompoundDrawablesRelative()[0];button.setCompoundDrawablesRelative(null,icon,null,null);
            button.setTextSize(13);button.setMinHeight(ui.dp(84));button.setMinimumHeight(ui.dp(84));button.setPadding(ui.dp(4),ui.dp(10),ui.dp(4),ui.dp(8));
        }
        navigation=new AppNavigation(ui,AppNavigation.Workspace.IMAGES,true);layout=new ImageWorkspaceLayout(ui,page,navigation,sections);
        Runnable navigate=sections.changed;sections.changed=()->{navigate.run();selectedItem.thumbnails.active(FILES.equals(sections.current()));};
    }
}
