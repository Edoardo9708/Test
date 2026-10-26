package it.edo.codecprobe;

import android.os.Bundle;
import java.util.*;

/** Photo destination presentation. Values only, never command widgets. */
final class PhotoDestinationState {
    boolean general;
    String currentLabel="",allLabel="",allocation="",exceptionLabel="",folder="";
    boolean exception,editorVisible,origin,destination,chooseVisible;
    private List<Integer> lastGroup=Collections.emptyList();
    PhotoDestinationState(Bundle saved){general=saved==null||saved.getBoolean("photoGeneralScope",true);}

    void update(DestinationPlan plan,int count,int selected,List<Integer> indices){
        if(!lastGroup.equals(indices)){
            if(indices.size()>1)general=false;
            lastGroup=new ArrayList<>(indices);
        }
        int exceptions=plan.exceptionCount(),own=0;
        for(int index:indices)if(plan.files.get(index).exception)own++;
        currentLabel=indices.size()>1?"File correnti · "+indices.size():"File corrente";
        allLabel="Tutti · "+(count-exceptions);
        allocation=exceptions+" file con destinazione propria · "+(count-exceptions)+" con destinazione generale";
        exception=!indices.isEmpty()&&own==indices.size();
        exceptionLabel=own>0&&own<indices.size()?"Eccezioni miste · attiva per tutti i selezionati":"Gestisci come eccezione";
        editorVisible=general||own>0;
        DestinationPlan.Choice choice=general||count==0?plan.all:plan.effective(selected);
        boolean mixed=false;
        if(!general)for(int index:indices){
            DestinationPlan.Choice other=plan.effective(index);
            mixed|=other.origin!=choice.origin||!Objects.equals(other.folder,choice.folder);
        }
        origin=!mixed&&choice.origin;destination=!mixed&&!choice.origin;
        folder=mixed?"Destinazioni diverse nella selezione":choice.origin?"Ogni foto nella propria cartella di origine":friendly(choice.folder);
        chooseVisible=mixed||!choice.origin;
    }
    private static String friendly(String value){
        if(value==null||value.isEmpty())return "Nessuna cartella scelta";
        String segment=android.net.Uri.parse(value).getLastPathSegment();
        return segment==null?value:segment.replace("primary:","Memoria interna / ");
    }
}
