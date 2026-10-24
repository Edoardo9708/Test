package it.edo.codecprobe;

import android.widget.*;

/** Image values mapped onto the same sliders and shortcut buttons used by video. */
final class ImageQualityControls {
    final SeekBar quality;final ChoiceSlider preset,tune;final TextView value;Runnable changed;private final QualityButtons shortcuts;private boolean tracking;
    ImageQualityControls(AppUi ui,LinearLayout parent,Runnable changed){
        this.changed=changed;
        if(ui.photos){LinearLayout format=PhotoStyle.group(ui,parent,12);ui.text(format,"Formato di uscita",13);ui.text(format,"HEIC · dimensioni originali",16);parent=PhotoStyle.group(ui,parent,28);}
        LinearLayout qualityPanel=parent;
        value=ui.parameterRow(parent,"Qualità","Valori da 40 a 95. Un valore maggiore tende a conservare più dettagli e occupare più spazio. La conversione è con perdita.");
        quality=new SeekBar(ui.activity);quality.setMax(55);ui.styleSlider(quality);parent.addView(quality,new LinearLayout.LayoutParams(-1,ui.dp(48)));
        shortcuts=new QualityButtons(ui,new int[]{50,55,60},new String[]{"50","55","60"},"qualità",q->quality.setProgress(q-40));parent.addView(shortcuts,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout presetPanel=ui.photos?PhotoStyle.group(ui,(LinearLayout)qualityPanel.getParent(),28):parent;
        preset=new ChoiceSlider(ui,presetPanel,"Preset",new String[]{"fast","medium","slow"},"Il preset più lento prova più alternative e richiede più tempo.",()->this.changed.run());
        LinearLayout advanced=ui.photos?PhotoStyle.group(ui,(LinearLayout)qualityPanel.getParent(),28):parent;
        LinearLayout tunePanel=advanced;
        if(ui.photos){Button more=ui.button(advanced,"Opzioni avanzate");tunePanel=new LinearLayout(ui.activity);tunePanel.setOrientation(LinearLayout.VERTICAL);advanced.addView(tunePanel);tunePanel.setVisibility(android.view.View.GONE);final LinearLayout body=tunePanel;more.setOnClickListener(v->PhotoStyle.visibility(body,body.getVisibility()!=android.view.View.VISIBLE));}
        tune=new ChoiceSlider(ui,tunePanel,"Tune",new String[]{"standard","ssim","psnr"},"Standard usa il comportamento psicovisivo predefinito di x265; SSIM e PSNR sono opzioni di confronto.",()->this.changed.run());
        ui.text(presetPanel,"Dimensioni originali, 8 bit e YCbCr 4:2:0. Qualità e preset sono indipendenti.",13);
        quality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){show();if(!tracking)ImageQualityControls.this.changed.run();}public void onStartTrackingTouch(SeekBar b){tracking=true;}public void onStopTrackingTouch(SeekBar b){tracking=false;ImageQualityControls.this.changed.run();}});
    }
    void set(ImageOptions options){quality.setProgress(options.quality-40);preset.select(options.preset);tune.select(options.tune);show();}
    int value(){return quality.getProgress()+40;}
    String preset(){return preset.value();}
    String tune(){return tune.value();}
    private void show(){value.setText("Qualità · "+value()+"/100 · CRF "+((100-value())/2.0));shortcuts.selectFromCrf(value());}
    void enabled(boolean enabled){quality.setEnabled(enabled);preset.slider.setEnabled(enabled);tune.slider.setEnabled(enabled);shortcuts.setEnabled(enabled);}
}
