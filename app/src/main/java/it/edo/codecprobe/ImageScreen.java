package it.edo.codecprobe;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

/** Image view composition only; all file access and actions belong to the controller. */
final class ImageScreen {
    static final String FILES="01  FOTO",DETAILS="02  METADATI",HEIC="03  OUTPUT HEIC";
    final AppUi ui;final LinearLayout page;final AppSections sections;final AppNavigation navigation;
    final Button select,clear,previous,next,remove,export;
    final TextView position,name,summary,details,status;
    ImageScreen(AppUi ui,Bundle saved){
        this.ui=ui;page=ui.newPage();
        Button settings=AppHeader.add(ui,page,"Immagini","JPEG · analisi metadati · v"+BuildConfig.VERSION_NAME);settings.setVisibility(View.GONE);
        sections=new AppSections(ui,saved,null,title->title.equals(FILES)?"Selezione e dimensioni delle foto":title.equals(DETAILS)?"EXIF, XMP, profilo colore e altri dati":"Stato della conversione JPEG → HEIC");
        LinearLayout files=sections.card(page,FILES),actions=ui.row(files);
        select=ui.halfButton(actions,"Seleziona JPEG",true);clear=ui.halfButton(actions,"Svuota tutto",false);
        position=ui.text(files,"Nessuna foto selezionata",14);LinearLayout move=ui.row(files);
        previous=ui.halfButton(move,"Precedente",false);next=ui.halfButton(move,"Successiva",false);
        name=ui.text(files,"",20);summary=ui.text(files,"",14);remove=ui.button(files,"Rimuovi foto");status=ui.text(files,"Scegli le foto da analizzare. Gli originali restano invariati.",14);
        LinearLayout metadata=sections.card(page,DETAILS);details=ui.text(metadata,"Seleziona una foto per leggere i metadati.",14);details.setTextIsSelectable(true);
        export=ui.button(metadata,"Esporta inventario JSON");
        ui.text(metadata,"Il report include i blocchi metadati originali, che possono contenere posizione GPS e altri dati personali. Non include i pixel della foto.",13);
        LinearLayout output=sections.card(page,HEIC);
        ui.text(output,"Conversione HEIC non ancora disponibile",18);
        ui.text(output,"In questa versione puoi analizzare i JPEG ed esportare i metadati. Il salvataggio delle foto in HEIC sarà disponibile dopo le verifiche su qualità, colore e dati conservati.",14);
        navigation=new AppNavigation(ui,AppNavigation.Workspace.IMAGES);ui.install(page,navigation);
    }
}
