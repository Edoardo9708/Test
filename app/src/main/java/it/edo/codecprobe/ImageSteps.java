package it.edo.codecprobe;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.util.LinkedHashMap;

/** Retains every step's controls; navigation changes visibility, never the draft. */
final class ImageSteps extends AppSections {
    private final AppUi ui;
    private final LinkedHashMap<String,LinearLayout> panels=new LinkedHashMap<>();
    final LinearLayout chooser;
    final Button newWork,previousWork;
    Runnable changed=()->{};
    private String current;

    ImageSteps(AppUi ui,LinearLayout page,Bundle saved){
        super(ui,null,null,title->"");this.ui=ui;
        current=saved==null?null:saved.getString("imageStep");
        chooser=group(ui,page);
        ui.text(chooser,"Da dove cominciamo?",23).setAccessibilityHeading(true);
        ui.text(chooser,"Puoi tornare qui senza perdere il nuovo lavoro.",14);
        newWork=ui.button(chooser,"Nuovo lavoro");ui.styleButton(newWork,true);
        previousWork=ui.button(chooser,"Riprendi sessione");
        newWork.setOnClickListener(v->focusCard(ImageScreen.FILES));
        previousWork.setOnClickListener(v->focusCard(ImageScreen.HEIC));
    }
    static LinearLayout group(AppUi ui,LinearLayout parent){
        LinearLayout panel=new LinearLayout(ui.activity);panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(ui.dp(16),ui.dp(12),ui.dp(16),ui.dp(16));
        int tint=ui.dark?0xE6293548:0xE6E8EFFC;
        panel.setBackground(ui.shape(tint,26));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,ui.dp(16),0,0);
        parent.addView(panel,lp);return panel;
    }
    @Override LinearLayout card(LinearLayout page,String title){
        LinearLayout panel=group(ui,page);panels.put(title,panel);
        Button heading=new Button(ui.activity);ui.styleButton(heading,false);
        heading.setText(title);heading.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);
        heading.setPadding(0,0,0,ui.dp(6));heading.setBackground(null);heading.setClickable(false);
        TrialIcons.set(ui,heading,title.equals(ImageScreen.FILES)?R.drawable.trial_photo:title.equals(ImageScreen.COMPRESSION)?R.drawable.trial_sliders:title.equals(ImageScreen.HEIC)?R.drawable.trial_play:R.drawable.trial_info,false);
        heading.setAccessibilityHeading(true);panel.addView(heading,new LinearLayout.LayoutParams(-1,-2));headers.put(title,heading);
        LinearLayout body=new LinearLayout(ui.activity);body.setOrientation(LinearLayout.VERTICAL);
        panel.addView(body,new LinearLayout.LayoutParams(-1,-2));bodies.put(title,body);
        render();return body;
    }
    @Override void setCardExpanded(String title,boolean expanded){if(expanded)focusCard(title);}
    @Override void focusCard(String title){
        current=panels.containsKey(title)?title:null;render();changed.run();
    }
    void home(){current=null;render();changed.run();}
    String current(){return current;}
    private void render(){
        chooser.setVisibility(current==null?View.VISIBLE:View.GONE);
        for(String title:panels.keySet()){
            boolean selected=title.equals(current);panels.get(title).setVisibility(selected?View.VISIBLE:View.GONE);
            bodies.get(title).setVisibility(selected?View.VISIBLE:View.GONE);
        }
    }
    @Override void save(Bundle out){out.putString("imageStep",current);}
}
