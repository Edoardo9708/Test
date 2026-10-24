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
    final TextView previousHint;
    Runnable changed=()->{};
    private String current;

    ImageSteps(AppUi ui,LinearLayout page,Bundle saved){
        super(ui,null,null,title->"");this.ui=ui;
        current=saved==null?null:saved.getString("imageStep");
        chooser=new LinearLayout(ui.activity);chooser.setOrientation(LinearLayout.VERTICAL);page.addView(chooser,new LinearLayout.LayoutParams(-1,-2));
        ui.text(chooser,"Da dove cominciamo?",23).setAccessibilityHeading(true);
        ui.text(chooser,"Puoi tornare qui senza perdere il nuovo lavoro.",14);
        newWork=ui.button(chooser,"Nuovo lavoro");
        previousWork=ui.button(chooser,"Riprendi sessione");
        PhotoStyle.joined(ui,newWork,0,true);PhotoStyle.joined(ui,previousWork,2,true);
        newWork.setMinHeight(ui.dp(104));previousWork.setMinHeight(ui.dp(104));
        previousHint=ui.text(chooser,"",13);
        newWork.setOnClickListener(v->focusCard(ImageScreen.FILES));
        previousWork.setOnClickListener(v->focusCard(ImageScreen.HEIC));
    }
    static LinearLayout group(AppUi ui,LinearLayout parent){
        return PhotoStyle.group(ui,parent,16);
    }
    @Override LinearLayout card(LinearLayout page,String title){
        LinearLayout panel=new LinearLayout(ui.activity);panel.setOrientation(LinearLayout.VERTICAL);
        page.addView(panel,new LinearLayout.LayoutParams(-1,-2));panels.put(title,panel);
        Button heading=new Button(ui.activity);ui.styleButton(heading,false);
        heading.setText(title.equals(ImageScreen.FILES)?"01  Prepara il lavoro\nNuovo lavoro":title.equals(ImageScreen.COMPRESSION)?"02  Compressione":title.equals(ImageScreen.HEIC)?"03  Avanzamento":title);heading.setTextSize(14);heading.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);
        heading.setPadding(0,0,0,ui.dp(6));heading.setBackground(null);heading.setClickable(false);
        TrialIcons.set(ui,heading,title.equals(ImageScreen.FILES)?R.drawable.trial_photo:title.equals(ImageScreen.COMPRESSION)?R.drawable.trial_sliders:title.equals(ImageScreen.HEIC)?R.drawable.trial_play:R.drawable.trial_info,false);
        heading.setAccessibilityHeading(true);panel.addView(heading,new LinearLayout.LayoutParams(-1,-2));headers.put(title,heading);
        LinearLayout body=new LinearLayout(ui.activity);body.setOrientation(LinearLayout.VERTICAL);
        panel.addView(body,new LinearLayout.LayoutParams(-1,-2));bodies.put(title,body);

        render();return body;
    }
    @Override void setCardExpanded(String title,boolean expanded){if(expanded)focusCard(title);}
    @Override void focusCard(String title){
        current=panels.containsKey(title)?title:null;render();if(current!=null&&panels.get(current).isAttachedToWindow()&&android.animation.ValueAnimator.areAnimatorsEnabled()){LinearLayout panel=panels.get(current);panel.setAlpha(0);panel.animate().alpha(1).setDuration(180).start();}changed.run();
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
