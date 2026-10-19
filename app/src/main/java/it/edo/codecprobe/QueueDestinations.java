package it.edo.codecprobe;

import android.content.Intent;
import java.util.*;

/** Immutable per-file destinations at the service boundary; supports previous single-folder requests. */
final class QueueDestinations {
    static ArrayList<String> read(Intent request,int count,String fallback) {
        ArrayList<String> values=request.getStringArrayListExtra("folders");
        if(values==null)values=new ArrayList<>(Collections.nCopies(count,fallback));
        if(values.size()!=count)throw new IllegalArgumentException("Piano destinazioni incompleto");
        for(String value:values)if(value==null||value.isEmpty())throw new IllegalArgumentException("Cartella di destinazione mancante");
        return new ArrayList<>(values);
    }
    private QueueDestinations(){}
}
