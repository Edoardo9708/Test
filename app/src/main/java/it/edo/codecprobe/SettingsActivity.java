package it.edo.codecprobe;

import android.app.Activity;
import android.os.Bundle;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputFilter;
import android.view.*;
import android.widget.*;

/** Explicitly saved defaults; edits never change a running conversion. */
public final class SettingsActivity extends Activity {
    private LinearLayout page;
    private String defaultFolder;
    private TextView folderValue;
    private int ink,accent,bg,surface;
    private SeekBar preset,crf;
    private RadioGroup cpu,position;
    private Switch bframes,lookahead;
    private CheckBox keepScreenOn;
    private EditText addition;
    private TextView presetValue,crfValue,preview;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        boolean dark=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        bg=Color.parseColor(dark?"#10151D":"#F2F5FA");ink=Color.parseColor(dark?"#EDF3FC":"#172236");accent=Color.parseColor(dark?"#99C1FF":"#215CBD");
        surface=Color.parseColor(dark?"#1B2330":"#FFFFFF");
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        ScrollView scroll=new ScrollView(this);page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setBackgroundColor(bg);scroll.addView(page);
        int pad=dp(20);page.setPadding(pad,pad,pad,pad);
        page.setOnApplyWindowInsetsListener((v,insets)->{if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(pad+b.left,pad+b.top,pad+b.right,pad+b.bottom);}else v.setPadding(pad,pad+insets.getSystemWindowInsetTop(),pad,pad+insets.getSystemWindowInsetBottom());return insets;});
        label("Impostazioni",28);button("← Torna alla conversione",this::finish);
        label("CARTELLA PREDEFINITA",16);
        defaultFolder=state!=null?state.getString("defaultFolder"):new AppSettings(this).defaultFolder;
        folderValue=label("",16);updateFolder();
        button("Scegli cartella predefinita",()->{android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION|android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION|android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,10);});
        button("Rimuovi cartella predefinita",()->{defaultFolder=null;updateFolder();});
        label("Usata all’apertura dell’app. Nella home puoi scegliere una destinazione diversa per la coda corrente.",13);
        label("NOME FILE",16);
        addition=new EditText(this);addition.setSingleLine();addition.setTextColor(ink);addition.setTextSize(18);addition.setHint("Testo da aggiungere");addition.setHintTextColor(ink);addition.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});page.addView(addition);
        position=radios("All’inizio","Alla fine");
        preview=label("",16);
        label("Il nome originale viene mantenuto; l’output è MP4. Se esiste già, aggiungo un numero e creo un nuovo file.",13);
        label("VALORI ALL’AVVIO",16);
        presetValue=label("Preset",20);preset=slider(8,()->presetValue.setText("Preset · "+AppSettings.PRESETS[preset.getProgress()]));
        crfValue=label("CRF",20);crf=slider(51,()->crfValue.setText("CRF · "+crf.getProgress()));
        LinearLayout notable=new LinearLayout(this);page.addView(notable);for(int n:new int[]{18,23,28}){Button b=new Button(this);b.setAllCaps(false);b.setText("CRF "+n);b.setOnClickListener(v->crf.setProgress(n));notable.addView(b,new LinearLayout.LayoutParams(0,-2,1));}
        label("CPU",20);cpu=radios("Limited","Auto","Max");
        bframes=toggle("B-frame");lookahead=toggle("Lookahead");
        label("AQ mode, AQ strength e CU-tree seguono il preset e i selettori.",13);
        keepScreenOn=new CheckBox(this);keepScreenOn.setText("Mantieni schermo acceso");keepScreenOn.setTextColor(ink);keepScreenOn.setTextSize(17);keepScreenOn.setMinHeight(dp(48));keepScreenOn.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));page.addView(keepScreenOn);
        AppSettings saved=new AppSettings(this);keepScreenOn.setChecked(saved.keepScreenOn);preset.setProgress(saved.preset);crf.setProgress(saved.crf);cpu.check(cpu.getChildAt(saved.cpu).getId());bframes.setChecked(saved.bframes);lookahead.setChecked(saved.lookahead);addition.setText(saved.addition);position.check(position.getChildAt(saved.prefix?0:1).getId());
        presetValue.setText("Preset · "+AppSettings.PRESETS[saved.preset]);crfValue.setText("CRF · "+saved.crf);
        addition.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){updatePreview();}public void afterTextChanged(Editable e){}});
        position.setOnCheckedChangeListener((g,id)->updatePreview());updatePreview();
        button("Usa la configurazione della schermata conversione",()->{
            preset.setProgress(getIntent().getIntExtra("preset",saved.preset));crf.setProgress(getIntent().getIntExtra("crf",saved.crf));int choice=Math.max(0,Math.min(2,getIntent().getIntExtra("cpu",saved.cpu)));cpu.check(cpu.getChildAt(choice).getId());bframes.setChecked(getIntent().getBooleanExtra("bframes",saved.bframes));lookahead.setChecked(getIntent().getBooleanExtra("lookahead",saved.lookahead));
        });
        button("Salva impostazioni",()->{AppSettings.setDefaultFolder(this,defaultFolder);AppSettings.setKeepScreenOn(this,keepScreenOn.isChecked());AppSettings.save(this,crf.getProgress(),preset.getProgress(),selected(cpu),bframes.isChecked(),lookahead.isChecked(),addition.getText().toString(),selected(position)==0);Toast.makeText(this,"Valori predefiniti salvati",Toast.LENGTH_SHORT).show();finish();});
        setContentView(scroll);page.requestApplyInsets();
        if(android.os.Build.VERSION.SDK_INT>=30)getWindow().getInsetsController().setSystemBarsAppearance(dark?0:WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        else getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    private void updateFolder(){folderValue.setText(defaultFolder==null?"Nessuna cartella predefinita":android.net.Uri.decode(android.net.Uri.parse(defaultFolder).getLastPathSegment()));}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("defaultFolder",defaultFolder);}
    @Override protected void onActivityResult(int request,int code,android.content.Intent data){
        super.onActivityResult(request,code,data);
        if(request!=10||code!=RESULT_OK||data==null||data.getData()==null)return;
        try{int flags=data.getFlags()&(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION|android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);if((flags&(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION|android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION))!=(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION|android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION))throw new SecurityException("Lettura e scrittura non concesse");getContentResolver().takePersistableUriPermission(data.getData(),android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION|android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);defaultFolder=data.getData().toString();updateFolder();}
        catch(SecurityException e){Toast.makeText(this,"Cartella non salvata: serve accesso permanente in scrittura. "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void updatePreview(){preview.setText("Anteprima: "+OutputName.base(getIntent().getStringExtra("sourceName")!=null?getIntent().getStringExtra("sourceName"):"Video.mp4",addition.getText().toString(),selected(position)==0));}
    private int selected(RadioGroup g){for(int i=0;i<g.getChildCount();i++)if(g.getChildAt(i).getId()==g.getCheckedRadioButtonId())return i;return 0;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(ink);t.setPadding(0,dp(10),0,dp(6));if(size>=20)t.setTypeface(null,Typeface.BOLD);page.addView(t);return t;}
    private void button(String s,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setSingleLine(false);b.setTextSize(16);b.setTextColor(accent);b.setMinHeight(dp(52));b.setPadding(dp(14),dp(10),dp(14),dp(10));android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();shape.setColor(surface);shape.setCornerRadius(dp(14));b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(accent),shape,null));page.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(v->action.run());}
    private RadioGroup radios(String... values){RadioGroup g=new RadioGroup(this);g.setOrientation(LinearLayout.HORIZONTAL);page.addView(g);for(String value:values){RadioButton b=new RadioButton(this);b.setId(View.generateViewId());b.setText(value);b.setTextColor(ink);b.setTextSize(16);b.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));g.addView(b,new LinearLayout.LayoutParams(0,-2,1));}return g;}
    private Switch toggle(String s){Switch t=new Switch(this);t.setText(s);t.setTextSize(20);t.setTextColor(ink);t.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));t.setPadding(0,dp(12),0,dp(12));page.addView(t);return t;}
    private SeekBar slider(int max,Runnable update){SeekBar s=new SeekBar(this);s.setMax(max);s.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));s.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));s.setMinHeight(dp(48));page.addView(s,new LinearLayout.LayoutParams(-1,-2));s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int n,boolean user){update.run();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});return s;}
}


