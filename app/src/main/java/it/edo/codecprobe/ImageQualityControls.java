package it.edo.codecprobe;

import android.widget.*;

/** Shared image quality controls for workspace and dedicated settings page. */
final class ImageQualityControls {
    final SeekBar quality;final Spinner preset,tune;final TextView value;Runnable changed;private final java.util.ArrayList<Button> shortcuts=new java.util.ArrayList<>();
    ImageQualityControls(AppUi ui,LinearLayout parent,Runnable changed){
        this.changed=changed;value=ui.text(parent,"",20);quality=new SeekBar(ui.activity);quality.setMax(55);ui.styleSlider(quality);parent.addView(quality,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout choices=ui.row(parent);for(int q:new int[]{60,65,70}){Button b=ui.halfButton(choices,Integer.toString(q),false);shortcuts.add(b);b.setOnClickListener(v->quality.setProgress(q-40));}
        ui.text(parent,"Qualità · 40–95 · compressione con perdita. Un valore maggiore tende a conservare più dettagli e occupare più spazio.",13);
        preset=spinner(ui,parent,"Preset",new String[]{"fast","medium","slow"});tune=spinner(ui,parent,"Tune",new String[]{"standard","ssim","psnr"});
        ui.text(parent,"Standard usa il comportamento psicovisivo predefinito di x265; SSIM e PSNR sono opzioni di confronto. Il preset più lento richiede più tempo. Dimensioni originali, 8 bit e YCbCr 4:2:0.",13);
        quality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){show();ImageQualityControls.this.changed.run();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        android.widget.AdapterView.OnItemSelectedListener listener=new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int at,long id){ImageQualityControls.this.changed.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}};preset.setOnItemSelectedListener(listener);tune.setOnItemSelectedListener(listener);
    }
    private Spinner spinner(AppUi ui,LinearLayout parent,String title,String[] values){ui.text(parent,title,15);Spinner spinner=new Spinner(ui.activity);ArrayAdapter<String> adapter=new ArrayAdapter<>(ui.activity,android.R.layout.simple_spinner_dropdown_item,values);spinner.setAdapter(adapter);spinner.setMinimumHeight(ui.dp(52));parent.addView(spinner,new LinearLayout.LayoutParams(-1,-2));return spinner;}
    void set(ImageOptions options){quality.setProgress(options.quality-40);preset.setSelection(options.preset.equals("fast")?0:options.preset.equals("medium")?1:2);tune.setSelection(options.tune.equals("standard")?0:options.tune.equals("ssim")?1:2);show();}
    int value(){return quality.getProgress()+40;}
    String preset(){return new String[]{"fast","medium","slow"}[Math.max(0,preset.getSelectedItemPosition())];}
    String tune(){return new String[]{"standard","ssim","psnr"}[Math.max(0,tune.getSelectedItemPosition())];}
    private void show(){value.setText("Qualità · "+value()+"/100 · CRF "+((100-value())/2.0));for(int i=0;i<shortcuts.size();i++)shortcuts.get(i).setSelected(value()==new int[]{60,65,70}[i]);}
    void enabled(boolean enabled){quality.setEnabled(enabled);preset.setEnabled(enabled);tune.setEnabled(enabled);for(Button b:shortcuts)b.setEnabled(enabled);}
}
