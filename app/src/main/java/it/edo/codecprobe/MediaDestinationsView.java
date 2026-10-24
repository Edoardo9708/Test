package it.edo.codecprobe;

import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;

/**
* Destination layout and rendering rules shared by all three workspaces.
*/
class MediaDestinationsView {
    final LinearLayout body;
    final MediaKind kind;
    boolean updating;
    CheckBox videoOrigin;
    CheckBox allOrigin;
    CheckBox applyAllDestinations;
    Button pickFolder;
    Button pickAllFolder;
    TextView videoDestinationTitle;
    TextView videoDestinationHelp;
    TextView allFolderLabel;
    TextView folderLabel;
    LinearLayout originRows;

    MediaDestinationsView(AppUi ui, LinearLayout parent, MediaKind kind) {
        this(ui,parent,kind,true);
    }
    MediaDestinationsView(AppUi ui,LinearLayout parent,MediaKind kind,boolean compose){
        this.kind=kind;
        LinearLayout files = new LinearLayout(ui.activity);
        files.setOrientation(LinearLayout.VERTICAL);
        parent.addView(files, new LinearLayout.LayoutParams(-1, -2));
        body = files;
        if(!compose)return;
        ui.divider(files);
        videoDestinationTitle = ui.text(files, "DESTINAZIONE DEL FILE", 16);
        videoDestinationTitle.setTypeface(null, Typeface.BOLD);
        videoDestinationHelp = ui.text(files, "Vale solo per il file visualizzato.", 14);
        videoOrigin = ui.destinationCheck(files, "Salva nella cartella di origine");
        pickFolder = ui.button(files, "Scegli cartella per "+kind.thisItem+"…");
        folderLabel = ui.text(files, "Nessuna cartella scelta", 14);
        ui.divider(files);
        ui.text(files, "DESTINAZIONE PER TUTTA LA CODA", 16).setTypeface(null, Typeface.BOLD);
        applyAllDestinations = ui.destinationCheck(files, "Applica a "+kind.allItems);
        ui.text(files, "Questa regola sostituisce temporaneamente le scelte individuali. Togliendo la spunta, le ritrovi come prima.", 14);
        allOrigin = ui.destinationCheck(files, "Salva nella cartella di origine");
        pickAllFolder = ui.button(files, "Scegli una cartella per "+kind.allItems+"…");
        allFolderLabel = ui.text(files, "Nessuna cartella scelta", 14);
        ui.text(files, "Origine: ciascun file nella propria directory. Cartella scelta: la stessa per tutta la coda.", 13);
        ui.divider(files);
        ui.text(files, "CARTELLE UTILIZZATE", 16).setTypeface(null, Typeface.BOLD);
        ui.text(files, "Destinazioni attive per tutta la coda. Verde: accesso verificato. Rosso: cartella da scegliere o autorizzare.", 14);
        originRows = new LinearLayout(ui.activity);
        originRows.setOrientation(LinearLayout.VERTICAL);
        files.addView(originRows);
    }

    void render(DestinationPlan plan,int count,int selected,boolean busy,boolean choosing) {
        boolean any=count>0;body.setVisibility(any?View.VISIBLE:View.GONE);updating=true;
        try {
            videoDestinationTitle.setText(any?"DESTINAZIONE · "+kind.title.toUpperCase(java.util.Locale.ITALY)+" "+(selected+1)+" DI "+count:"DESTINAZIONE DEL FILE");
            videoDestinationHelp.setText(plan.applyAll?"È attiva la regola della coda. La scelta individuale qui sotto viene conservata.":"Questa scelta riguarda solo il file visualizzato.");
            applyAllDestinations.setChecked(plan.applyAll);allOrigin.setChecked(plan.all.origin);
            allFolderLabel.setText(plan.all.origin?"Ogni file nella propria directory di origine":path(plan.all.folder));
            if(any){DestinationPlan.Choice choice=plan.files.get(selected);videoOrigin.setChecked(choice.origin);folderLabel.setText(choice.origin?"Directory di origine del file selezionato":path(choice.folder));}
        } finally { updating=false; }
        setBusy(busy,choosing,any,plan.applyAll);
    }
    private static String path(String value){return value==null||value.isEmpty()?"Nessuna cartella scelta":android.net.Uri.decode(android.net.Uri.parse(value).getLastPathSegment());}

    void setBusy(boolean busy, boolean choosing, boolean hasInputs, boolean applyAll) {
        boolean editable = !busy && !choosing;
        videoOrigin.setEnabled(editable && hasInputs && !applyAll);
        pickFolder.setEnabled(editable && hasInputs && !applyAll);
        applyAllDestinations.setEnabled(editable && hasInputs);
        allOrigin.setEnabled(editable && applyAll);
        pickAllFolder.setEnabled(editable && applyAll);
    }
}
