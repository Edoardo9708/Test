package it.edo.codecprobe;

import android.os.Bundle;
import android.widget.*;

/** Image view composition only; file and engine actions belong to separate coordinators. */
final class ImageScreen {
    static final String FILES="01  FOTO E DESTINAZIONE",COMPRESSION="02  COMPRESSIONE IMMAGINI",HEIC="03  CONVERSIONE IMMAGINI",DETAILS="DIAGNOSTICA";
    final AppUi ui;final LinearLayout page;final AppSections sections;final AppNavigation navigation;final ImageQualityControls quality;
    final Button settings,select,clear,reload,remove,export,chooseFolder,convert,cancel,metadata,exportLog;
    final CheckBox smaller,screenOn;final ProgressBar progress;
    final TextView position,name,summary,details,status,folder,parameters,runStatus,log;
    final WorkReportView workReport;
    final SelectedItemView selectedItem;
    ImageScreen(AppUi ui,Bundle saved){
        this.ui=ui;page=ui.newPage();settings=AppHeader.add(ui,page,"Converti immagini","JPEG → HEIC · x265 · v"+BuildConfig.VERSION_NAME);
        sections=new AppSections(ui,saved,null,title->title.equals(FILES)?"Foto selezionate e cartella di destinazione":title.equals(COMPRESSION)?"Qualità e velocità di compressione":title.equals(HEIC)?"Avanzamento e risultati":"Metadati originali, report e log");
        LinearLayout files=sections.card(page,FILES),actions=ui.row(files);select=ui.halfButton(actions,"Seleziona JPEG",true);clear=ui.halfButton(actions,"Svuota tutto",false);
        selectedItem=new SelectedItemView(ui,files,"Foto","Nessuna foto selezionata","Dettagli foto");position=selectedItem.number;name=selectedItem.name;summary=selectedItem.summary;
        metadata=selectedItem.details;remove=ui.button(files,"Rimuovi foto");
        details=ui.text(selectedItem.detailsBody,"Seleziona una foto per leggere i metadati.",15);details.setTextIsSelectable(true);
        reload=ui.button(files,"Rileggi metadati");
        status=ui.text(files,"Scegli le foto. Gli originali restano invariati.",14);ui.divider(files);chooseFolder=ui.button(files,"Scegli cartella di destinazione");folder=ui.text(files,"Nessuna cartella scelta",14);
        ui.text(files,"Il nome originale viene mantenuto con estensione .heic. In caso di omonimi viene aggiunto un numero; gli originali sono conservati.",13);
        LinearLayout compression=sections.card(page,COMPRESSION);quality=new ImageQualityControls(ui,compression,()->{});smaller=ImageSettingsScreen.check(ui,compression,"Salva solo se più piccolo",true);screenOn=ImageSettingsScreen.check(ui,compression,"Mantieni schermo acceso",false);
        LinearLayout output=sections.card(page,HEIC),buttons=ui.row(output);convert=ui.halfButton(buttons,"Converti in HEIC",true);cancel=ui.halfButton(buttons,"Interrompi",false);ui.danger(cancel);
        progress=new ProgressBar(ui.activity);output.addView(progress,new LinearLayout.LayoutParams(-1,ui.dp(36)));progress.setVisibility(android.view.View.GONE);
        runStatus=ui.text(output,"Scegli foto, cartella e qualità.",14);ui.sectionHeading(output,"PARAMETRI IN USO");parameters=ui.text(output,"",14);workReport=new WorkReportView(ui,output);
        LinearLayout diagnostic=sections.card(page,DETAILS);export=ui.button(diagnostic,"Esporta inventario JSON");
        ui.text(diagnostic,"Il report include i blocchi metadati originali, che possono contenere posizione GPS e altri dati personali. Non include i pixel della foto.",13);
        ui.parameterRow(diagnostic,"Dettagli del motore immagini","JPEG YCbCr a 8 bit, lati 64–16384 pixel, massimo 48 megapixel e file di 64 MiB. Decodifica JPEG e verifica HEIC con FFmpeg incluso; contenitore libheif, codifica x265. Nessun ridimensionamento, rotazione dei pixel o normalizzazione. Conversione con perdita. EXIF/XMP/ICC standard conservati e riletti; i blocchi APP/COM originali e il SEF foto supportato sono conservati anche in un archivio interno. Le funzioni dell’editor Samsung non sono garantite. Altri APP/COM proprietari restano archiviati senza interpretazione. CMYK/RGB, scala di grigi, HDR/gain map, Motion Photo, MPF, XMP esteso e SEF non supportato vengono rifiutati.\n\nLa coda continua uscendo dalla pagina. Se Android termina il processo non riparte automaticamente. Interrompi agisce alla fine della fase nativa in corso; non interrompe x265 a metà fotogramma.");
        exportLog=ui.button(diagnostic,"Esporta log immagini TXT");log=ui.text(diagnostic,"Nessun log immagini",13);log.setMaxLines(14);log.setTextIsSelectable(true);
        navigation=new AppNavigation(ui,AppNavigation.Workspace.IMAGES);ui.install(page,navigation);
    }
}
