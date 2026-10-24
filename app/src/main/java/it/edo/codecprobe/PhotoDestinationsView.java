package it.edo.codecprobe;

import android.view.View;
import android.widget.*;
import java.util.*;
import java.util.function.Consumer;

/** General destination plus explicit per-file exceptions; permissions form a separate group. */
final class PhotoDestinationsView extends MediaDestinationsView {
    final Button currentScope,allScope,originChoice,targetChoice;
    final CheckBox exception;
    final TextView allocation,inherited;
    final LinearLayout editor,policies;
    boolean general=true;
    Runnable changed=()->{};
    Consumer<Boolean> exceptionEdited=value->{},originEdited=value->{};
    private final AppUi ui;
    private List<Integer> lastGroup=Collections.emptyList();
    PhotoDestinationsView(AppUi ui,LinearLayout parent){
        super(ui,parent,MediaKind.IMAGE,false);this.ui=ui;
        LinearLayout scope=PhotoStyle.group(ui,body,0);ui.text(scope,"Cosa elaborare",13);
        LinearLayout choices=ui.row(scope);currentScope=ui.halfButton(choices,"File corrente",false);allScope=ui.halfButton(choices,"Tutti",false);
        currentScope.setOnClickListener(v->{general=false;changed.run();});allScope.setOnClickListener(v->{general=true;changed.run();});
        allocation=ui.text(scope,"",12);
        LinearLayout where=PhotoStyle.group(ui,body,4);ui.text(where,"Dove salvare",13);
        exception=ui.destinationCheck(where,"Gestisci come eccezione");exception.setTextSize(13);
        exception.setOnCheckedChangeListener((v,value)->{if(!updating)exceptionEdited.accept(value);});
        inherited=ui.text(where,"Segue la destinazione generale.",12);
        editor=new LinearLayout(ui.activity);editor.setOrientation(LinearLayout.VERTICAL);where.addView(editor);
        LinearLayout locations=ui.row(editor);originChoice=ui.halfButton(locations,"Origine",false);targetChoice=ui.halfButton(locations,"Altra cartella",false);
        originChoice.setOnClickListener(v->originEdited.accept(true));targetChoice.setOnClickListener(v->originEdited.accept(false));
        pickFolder=ui.button(editor,"Scegli cartella…");folderLabel=ui.text(editor,"",12);folderLabel.setTextIsSelectable(true);
        // Existing controller contracts are retained for other workspaces and saved picker requests.
        pickAllFolder=new Button(ui.activity);videoOrigin=new CheckBox(ui.activity);allOrigin=new CheckBox(ui.activity);applyAllDestinations=new CheckBox(ui.activity);
        videoDestinationTitle=new TextView(ui.activity);videoDestinationHelp=new TextView(ui.activity);allFolderLabel=new TextView(ui.activity);
        policies=PhotoStyle.group(ui,body,4);
        PhotoStyle.heading(ui,body,"Autorizzazioni cartelle",R.drawable.trial_folder);
        originRows=new LinearLayout(ui.activity);originRows.setOrientation(LinearLayout.VERTICAL);body.addView(originRows);
    }
    void renderGroup(DestinationPlan plan,int count,int selected,List<Integer> indices,boolean busy,boolean choosing){
        if(indices.size()>1&&!lastGroup.equals(indices))general=false;lastGroup=new ArrayList<>(indices);
        updating=true;
        try{
            int exceptions=plan.exceptionCount(),own=0;for(int n:indices)if(plan.files.get(n).exception)own++;
            currentScope.setText((indices.size()>1?"File correnti · "+indices.size():"File corrente")+" · "+exceptions+" eccezioni");allScope.setText("Tutti · "+(count-exceptions));
            currentScope.setSelected(!general);allScope.setSelected(general);
            allocation.setText(exceptions+" file con destinazione propria · "+(count-exceptions)+" con destinazione generale");
            exception.setChecked(!indices.isEmpty()&&own==indices.size());exception.setText(own>0&&own<indices.size()?"Eccezioni miste · attiva per tutti i selezionati":"Gestisci come eccezione");
            PhotoStyle.visibility(exception,!general);PhotoStyle.visibility(inherited,!general&&own==0);PhotoStyle.visibility(editor,general||own>0);
            DestinationPlan.Choice choice=general||count==0?plan.all:plan.effective(selected);
            boolean mixed=false;for(int n:indices)if(!general){DestinationPlan.Choice other=plan.effective(n);mixed|=other.origin!=choice.origin||!Objects.equals(other.folder,choice.folder);}
            originChoice.setSelected(!mixed&&choice.origin);targetChoice.setSelected(!mixed&&!choice.origin);
            folderLabel.setText(mixed?"Destinazioni diverse nella selezione":choice.origin?"Ogni foto nella propria cartella di origine":friendly(choice.folder));
            PhotoStyle.visibility(pickFolder,mixed||!choice.origin);
            boolean enabled=!busy&&!choosing;for(Button button:new Button[]{currentScope,allScope,originChoice,targetChoice,pickFolder})button.setEnabled(enabled&&(general||count>0));exception.setEnabled(enabled&&count>0);
        }finally{updating=false;}
    }
    static String friendly(String value){if(value==null||value.isEmpty())return "Nessuna cartella scelta";String segment=android.net.Uri.parse(value).getLastPathSegment();return segment==null?value:segment.replace("primary:","Memoria interna / ");}
}
