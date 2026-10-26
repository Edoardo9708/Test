package it.edo.codecprobe;

import android.os.Bundle;
import android.widget.LinearLayout;
import java.util.LinkedHashMap;

/** Navigation state only. Compose owns the chooser, headers and navigation buttons. */
final class ImageSteps {
    final LinkedHashMap<String,LinearLayout> bodies=new LinkedHashMap<>();
    boolean previousEnabled,closed;
    String previousHint="";
    Runnable changed=()->{};
    private String current;
    ImageSteps(Bundle saved){
        String restored=saved==null?null:saved.getString("imageStep");
        current=valid(restored)?restored:null;
    }
    private static boolean valid(String title){
        return ImageScreen.FILES.equals(title)||ImageScreen.COMPRESSION.equals(title)
            ||ImageScreen.HEIC.equals(title)||ImageScreen.DETAILS.equals(title);
    }
    LinearLayout panel(AppUi ui,String title){
        LinearLayout body=new LinearLayout(ui.activity);body.setOrientation(LinearLayout.VERTICAL);
        bodies.put(title,body);return body;
    }
    static LinearLayout group(AppUi ui,LinearLayout parent){return PhotoStyle.group(ui,parent,16);}
    void focusCard(String title){
        String next=valid(title)?title:null;
        if(java.util.Objects.equals(current,next))return;
        current=next;if(!closed)changed.run();
    }
    void home(){focusCard(null);}
    String current(){return current;}
    void save(Bundle out){out.putString("imageStep",current);}
}
