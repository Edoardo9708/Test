package it.edo.codecprobe;

import android.widget.*;
import java.util.Arrays;

/** A preset slider binds the standard slider appearance to a list of actual values. */
final class ChoiceSlider {
    final SeekBar slider;
    private final String[] values;
    private final String title;
    private final TextView label;
    ChoiceSlider(AppUi ui,LinearLayout parent,String title,String[] values,String help,Runnable changed){
        this.title=title;this.values=values.clone();label=ui.parameterRow(parent,title,help);slider=new SeekBar(ui.activity);slider.setMax(values.length-1);ui.styleSlider(slider);parent.addView(slider,new LinearLayout.LayoutParams(-1,ui.dp(48)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int position,boolean user){show();changed.run();}public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}});show();
    }
    void select(String value){int index=Arrays.asList(values).indexOf(value);if(index<0)throw new IllegalArgumentException("Scelta non valida: "+value);slider.setProgress(index);show();}
    String value(){return values[slider.getProgress()];}
    private void show(){label.setText(title+" · "+value());}
}
