package it.edo.codecprobe;

import android.widget.*;

/** Image values mapped onto the same sliders and shortcut buttons used by video. */
final class ImageQualityControls {
    final SeekBar quality;final ChoiceSlider preset,tune;final TextView value;Runnable changed;private final QualityButtons shortcuts;
    ImageQualityControls(AppUi ui,LinearLayout parent,Runnable changed){
        this.changed=changed;value=ui.parameterRow(parent,"Qualità","Valori da 40 a 95. Un valore maggiore tende a conservare più dettagli e occupare più spazio. La conversione è con perdita.");
        quality=new SeekBar(ui.activity);quality.setMax(55);ui.styleSlider(quality);parent.addView(quality,new LinearLayout.LayoutParams(-1,ui.dp(48)));
        shortcuts=new QualityButtons(ui,new int[]{60,65,70},new String[]{"60","65","70"},"qualità",q->quality.setProgress(q-40));parent.addView(shortcuts,new LinearLayout.LayoutParams(-1,-2));
        preset=new ChoiceSlider(ui,parent,"Preset",new String[]{"fast","medium","slow"},"Il preset più lento prova più alternative e richiede più tempo.",()->this.changed.run());
        tune=new ChoiceSlider(ui,parent,"Tune",new String[]{"standard","ssim","psnr"},"Standard usa il comportamento psicovisivo predefinito di x265; SSIM e PSNR sono opzioni di confronto.",()->this.changed.run());
        ui.text(parent,"Dimensioni originali, 8 bit e YCbCr 4:2:0. Qualità e preset sono indipendenti.",13);
        quality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){show();ImageQualityControls.this.changed.run();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
    }
    void set(ImageOptions options){quality.setProgress(options.quality-40);preset.select(options.preset);tune.select(options.tune);show();}
    int value(){return quality.getProgress()+40;}
    String preset(){return preset.value();}
    String tune(){return tune.value();}
    private void show(){value.setText("Qualità · "+value()+"/100 · CRF "+((100-value())/2.0));shortcuts.selectFromCrf(value());}
    void enabled(boolean enabled){quality.setEnabled(enabled);preset.slider.setEnabled(enabled);tune.slider.setEnabled(enabled);shortcuts.setEnabled(enabled);}
}
