package it.edo.codecprobe;

import android.os.Bundle;
import android.widget.*;

/** Image view composition only; file and engine actions belong to separate coordinators. */
final class ImageScreen {
    static final String FILES="01  FOTO E DESTINAZIONE",COMPRESSION="02  COMPRESSIONE IMMAGINI",HEIC="03  CONVERSIONE IMMAGINI",DETAILS="DIAGNOSTICA";
    final AppUi ui;final LinearLayout page;final AppSections sections;final AppNavigation navigation;final ImageQualityControls quality;
    final Button settings,select,importFolder,retryFailures,clear,reload,remove,export,chooseFolder,convert,cancel,metadata,exportLog,exportErrors,clearLog,toggleLog;
    final CheckBox smaller,screenOn;final ProgressBar progress;
    final TextView position,name,summary,selectionSummary,details,status,folder,parameters,runStatus,log;
    final WorkReportView workReport;
    final ConversionDashboard dashboard;
    final SelectedItemView selectedItem;
    final MediaDestinationsView destinations;
    ImageScreen(AppUi ui,Bundle saved){
        this.ui=ui;page=ui.newPage();settings=AppHeader.add(ui,page,"Converti immagini","JPEG → HEIC · x265 · v"+BuildConfig.VERSION_NAME);
        sections=new AppSections(ui,saved,null,title->title.equals(FILES)?"Foto selezionate e cartella di destinazione":title.equals(COMPRESSION)?"Qualità e velocità di compressione":title.equals(HEIC)?"Avanzamento e risultati":"Metadati originali, report e log");
        LinearLayout files=sections.card(page,FILES),actions=ui.row(files);select=ui.halfButton(actions,"Aggiungi JPEG",true);clear=ui.halfButton(actions,"Svuota tutto",false);
        LinearLayout imports=ui.row(files);importFolder=ui.halfButton(imports,"Importa cartella JPEG",true);retryFailures=ui.halfButton(imports,"Carica fallite",false);
        selectionSummary=ui.text(files,"",14);
        selectedItem=new SelectedItemView(ui,files,"Foto","Nessuna foto selezionata","Dettagli foto");position=selectedItem.number;name=selectedItem.name;summary=selectedItem.summary;
        metadata=selectedItem.details;remove=selectedItem.remove;
        details=ui.text(selectedItem.detailsBody,"Seleziona una foto per leggere i metadati.",15);details.setTextIsSelectable(true);details.setVisibility(android.view.View.GONE);
        reload=ui.button(files,"Rileggi metadati");
        status=ui.text(files,"Scegli le foto. Gli originali restano invariati.",14);destinations=new MediaDestinationsView(ui,files,MediaKind.IMAGE);chooseFolder=destinations.pickFolder;folder=destinations.folderLabel;
        ui.text(destinations.body,"Il nome originale viene mantenuto con estensione .heic. In caso di omonimi viene aggiunto un numero; gli originali sono conservati.",13);
        LinearLayout compression=sections.card(page,COMPRESSION);quality=new ImageQualityControls(ui,compression,()->{});
        LinearLayout output=sections.card(page,HEIC),buttons=ui.row(output);convert=ui.halfButton(buttons,"Converti e salva",true);cancel=ui.halfButton(buttons,"Annulla",false);ui.danger(cancel);
        ConversionOptionsView options=new ConversionOptionsView(ui,output,false,true);smaller=options.smaller;screenOn=options.screenOn;
        dashboard=new ConversionDashboard(ui,output,new String[]{},new String[]{"Solo se più piccolo","Qualità","CRF","Preset","Tune","Formato","Dimensioni"});progress=dashboard.progress;runStatus=dashboard.status;
        parameters=ui.text(output,"",14);parameters.setVisibility(android.view.View.GONE);workReport=new WorkReportView(ui,output);
        LinearLayout diagnostic=sections.card(page,DETAILS),exports=ui.row(diagnostic);exportLog=ui.halfButton(exports,"Esporta log",false);export=ui.halfButton(exports,"Metadati foto · JSON",false);
        exportErrors=ui.button(diagnostic,"Esporta archivio errori");
        ui.text(diagnostic,"Il report include i blocchi metadati originali, che possono contenere posizione GPS e altri dati personali. Non include i pixel della foto.",13);
        ui.parameterRow(diagnostic,"Dettagli del motore immagini","JPEG YCbCr a 8 bit, lati 64–16384 pixel, massimo 48 megapixel e file di 64 MiB. Decodifica JPEG e verifica HEIC con FFmpeg incluso; contenitore libheif, codifica x265. Nessun ridimensionamento, rotazione dei pixel o normalizzazione. Conversione con perdita. EXIF/XMP/ICC standard conservati e riletti; i blocchi APP/COM originali e il SEF foto supportato sono conservati anche in un archivio interno. Le funzioni dell’editor Samsung non sono garantite. Altri APP/COM proprietari restano archiviati senza interpretazione. CMYK/RGB, scala di grigi, HDR/gain map, Motion Photo, MPF, XMP esteso e SEF non supportato vengono rifiutati.\n\nSelezione manuale fino a 5.000 foto. Importazione automatica della cartella JPEG, senza sottocartelle: nessuna selezione manuale dei lotti. Il registro su disco mantiene un solo esito per originale; originale e risultato già presenti vengono verificati con SHA-256 prima di evitare una nuova codifica. «Carica fallite» riporta in selezione gli ultimi fallimenti del registro, con le destinazioni conservate e le impostazioni di compressione correnti. Carosello: prime 100 e puntini. Risultati: finestra di 100 righe; la corrente è al 50º posto quando ci sono abbastanza elementi prima e dopo. Totali cumulativi su tutta la coda. Log completo dell’ultima esecuzione su disco, anteprima di 24.000 caratteri in RAM. Archivio errori persistente ed esportabile, separato dal report. La lettura dei metadati di anteprima non blocca l’avvio; ogni foto viene controllata integralmente prima della codifica.\n\nLa coda continua uscendo dalla pagina. Se Android termina il processo non riparte automaticamente. Interrompi agisce alla fine della fase nativa in corso; non interrompe x265 a metà fotogramma.");
        LogPanelView logPanel=new LogPanelView(ui,diagnostic,"Cancella log immagini","Cancella il log immagini nell’app. Le foto, il resoconto e i TXT già esportati restano salvati.");clearLog=logPanel.clear;toggleLog=logPanel.toggle;log=logPanel.log;
        navigation=new AppNavigation(ui,AppNavigation.Workspace.IMAGES);ui.install(page,navigation);
    }
}
