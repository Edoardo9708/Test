package it.edo.codecprobe;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputFilter;
import android.view.*;
import android.widget.*;

/**
* Explicitly saved defaults; edits never change a running conversion.
*/
public final class SettingsActivity extends Activity {
    AudioSettingsScreen audioScreen;
    private LinearLayout page;
    private AppUi ui;
    private String defaultFolder;
    private TextView folderValue;
    private int ink;
    private int accent;
    private int bg;
    private int surface;
    private int border;
    private SeekBar preset;
    private SeekBar crf;
    private QualityButtons qualityButtons;
    private RadioGroup position;
    private CpuModeButtons cpu;
    private Switch bframes;
    private Switch lookahead;
    private CheckBox keepScreenOn,onlySmaller;
    private EditText addition;
    private TextView presetValue;
    private TextView crfValue;
    private TextView preview;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        if(getIntent().getBooleanExtra(AudioSettingsScreen.AUDIO,false)){audioScreen=new AudioSettingsScreen(this,state);return;}
        ui = new AppUi(this);
        bg = ui.bg;
        ink = ui.ink;
        accent = ui.accent;
        surface = ui.surface;
        border = ui.border;
        SettingsPage frame=new SettingsPage(ui,"Impostazioni video","HEVC · x265 · v"+BuildConfig.VERSION_NAME);
        LinearLayout root=frame.page;
        page=frame.section("CARTELLA PREDEFINITA");
        defaultFolder = state != null ? state.getString("defaultFolder") : new AppSettings(this).defaultFolder;
        folderValue = label("", 16);
        updateFolder();
        button("Scegli cartella predefinita", ()->startActivityForResult(new android.content.Intent(this, FolderPickerActivity.class), 10));
        button("Rimuovi cartella predefinita", ()->{
            defaultFolder = null;
            updateFolder();
        });
        label("Usata all’apertura dell’app. Nella home puoi scegliere una destinazione diversa per la coda corrente.", 13);
        page = frame.section("NOME FILE");
        addition = new EditText(this);
        addition.setSingleLine();
        addition.setTextColor(ink);
        addition.setTextSize(18);
        addition.setHint("Testo da aggiungere");
        addition.setHintTextColor(ui.muted);
        addition.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
        addition.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
        page.addView(addition);
        position = radios("All’inizio", "Alla fine");
        preview = label("", 16);
        label("Il nome originale viene mantenuto; l’output è MP4. Se esiste già, aggiungo un numero e creo un nuovo file.", 13);
        page = frame.section("QUALITÀ PREDEFINITA");
        presetValue = label("Preset", 20);
        preset = slider(8, ()->presetValue.setText("Preset · " + AppSettings.PRESETS[preset.getProgress()]));
        crfValue = label("CRF", 20);
        label("Scegli la qualità manualmente", 14);
        crf = slider(51, ()->{
            crfValue.setText("CRF · " + crf.getProgress());
            if (qualityButtons != null) qualityButtons.selectFromCrf(crf.getProgress());
        });
        label("Scegli una qualità predefinita", 14);
        qualityButtons = new QualityButtons(ui, (n)->crf.setProgress(n));
        page.addView(qualityButtons, new LinearLayout.LayoutParams(-1, -2));
        label("CPU", 20);
        cpu = new CpuModeButtons(this, accent, bg, surface, border);
        page.addView(cpu, new LinearLayout.LayoutParams(-1, -2));
        bframes = toggle("B-frame");
        lookahead = toggle("Lookahead");
        label("AQ mode, AQ strength e CU-tree seguono il preset e i selettori.", 13);
        page=frame.section("OPZIONI PREDEFINITE");
        onlySmaller=SettingsPage.check(ui,page,"Salva solo se più piccolo",false);
        keepScreenOn=SettingsPage.check(ui,page,"Mantieni schermo acceso",false);
        ui.text(page,"Le modifiche salvate valgono dalla prossima coda; non cambiano una conversione già avviata.",13);
        AppSettings saved = new AppSettings(this);
        keepScreenOn.setChecked(state==null?saved.keepScreenOn:state.getBoolean("settingScreen",saved.keepScreenOn));
        onlySmaller.setChecked(state==null?saved.onlySmaller:state.getBoolean("settingSmaller",saved.onlySmaller));
        preset.setProgress(state==null?saved.preset:state.getInt("settingPreset",saved.preset));
        crf.setProgress(state==null?saved.crf:state.getInt("settingCrf",saved.crf));
        cpu.setSelection(state==null?saved.cpu:state.getInt("settingCpu",saved.cpu));
        bframes.setChecked(state==null?saved.bframes:state.getBoolean("settingBframes",saved.bframes));
        lookahead.setChecked(state==null?saved.lookahead:state.getBoolean("settingLookahead",saved.lookahead));
        addition.setText(state==null?saved.addition:state.getString("settingAddition",saved.addition));
        position.check(position.getChildAt((state==null?saved.prefix:state.getBoolean("settingPrefix",saved.prefix)) ? 0 : 1).getId());
        presetValue.setText("Preset · " + AppSettings.PRESETS[preset.getProgress()]);
        crfValue.setText("CRF · " + crf.getProgress());
        addition.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence s, int a, int c, int f) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
                updatePreview();
            }

            public void afterTextChanged(Editable e) {
            }
        });
        position.setOnCheckedChangeListener((g,id)->updatePreview());
        updatePreview();
        page = root;
        SettingsPage.Actions actions=frame.actions();
        actions.copy.setOnClickListener(v->{
            preset.setProgress(getIntent().getIntExtra("preset", saved.preset));
            crf.setProgress(getIntent().getIntExtra("crf", saved.crf));
            int choice = Math.max(0, Math.min(2, getIntent().getIntExtra("cpu", saved.cpu)));
            cpu.setSelection(choice);
            bframes.setChecked(getIntent().getBooleanExtra("bframes", saved.bframes));
            lookahead.setChecked(getIntent().getBooleanExtra("lookahead", saved.lookahead));
            if(getIntent().hasExtra("folder")){defaultFolder=getIntent().getStringExtra("folder");updateFolder();}
            keepScreenOn.setChecked(getIntent().getBooleanExtra("keepScreenOn",saved.keepScreenOn));
            onlySmaller.setChecked(getIntent().getBooleanExtra("onlySmaller",saved.onlySmaller));
        });
        actions.save.setOnClickListener(v->{
            AppSettings.setDefaultFolder(this, defaultFolder);
            AppSettings.setKeepScreenOn(this, keepScreenOn.isChecked());
            AppSettings.setOnlySmaller(this,onlySmaller.isChecked());
            AppSettings.save(this, crf.getProgress(), preset.getProgress(), cpu.getSelectedItemPosition(), bframes.isChecked(), lookahead.isChecked(), addition.getText().toString(), selected(position) == 0);
            Toast.makeText(this, "Valori predefiniti salvati", Toast.LENGTH_SHORT).show();
            finish();
        });
        ui.install(root);
    }

    private void updateFolder() {
        folderValue.setText(defaultFolder == null ? "Nessuna cartella predefinita" : android.net.Uri.decode(android.net.Uri.parse(defaultFolder).getLastPathSegment()));
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        if(audioScreen!=null){audioScreen.saveState(b);return;}
        b.putString("defaultFolder", defaultFolder);
        b.putInt("settingPreset",preset.getProgress());b.putInt("settingCrf",crf.getProgress());b.putInt("settingCpu",cpu.getSelectedItemPosition());b.putBoolean("settingBframes",bframes.isChecked());b.putBoolean("settingLookahead",lookahead.isChecked());b.putBoolean("settingScreen",keepScreenOn.isChecked());b.putBoolean("settingSmaller",onlySmaller.isChecked());b.putString("settingAddition",addition.getText().toString());b.putBoolean("settingPrefix",selected(position)==0);
    }

    @Override
    protected void onActivityResult(int request, int code, android.content.Intent data) {
        super.onActivityResult(request, code, data);
        if(audioScreen!=null){audioScreen.result(request,code,data);return;}
        if (request != 10) return;
        if (code != RESULT_OK || data == null || data.getData() == null) {
            String error = data == null ? null : data.getStringExtra(FolderPickerActivity.ERROR);
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            int flags = data.getFlags() & (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if ((flags & (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)) != (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)) throw new SecurityException("Lettura e scrittura non concesse");
            getContentResolver().takePersistableUriPermission(data.getData(), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            defaultFolder = data.getData().toString();
            updateFolder();
        } catch (SecurityException e) {
            Toast.makeText(this, "Cartella non salvata: serve accesso permanente in scrittura. " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updatePreview() {
        preview.setText("Anteprima: " + OutputName.base(getIntent().getStringExtra("sourceName") != null ? getIntent().getStringExtra("sourceName") : "Video.mp4", addition.getText().toString(), selected(position) == 0));
    }

    private int selected(RadioGroup g) {
        for (int i = 0; i < g.getChildCount(); i++) if (g.getChildAt(i).getId() == g.getCheckedRadioButtonId()) return i;
        return 0;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private TextView label(String s, int size) {
        TextView t=ui.text(page,s,size);if(size>=20)t.setTypeface(null,Typeface.BOLD);return t;
    }

    private void button(String text, Runnable action) {
        Button b = ui.button(page, text);
        if (text.equals("Salva impostazioni")) ui.styleButton(b, true);
        b.setOnClickListener((v)->action.run());
    }

    private RadioGroup radios(String... values) {
        RadioGroup g = new RadioGroup(this);
        g.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(g);
        for (String value : values) {
            RadioButton b = new RadioButton(this);
            b.setId(View.generateViewId());
            b.setText(value);
            b.setTextColor(ink);
            b.setTextSize(16);
            b.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));
            g.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
        }
        return g;
    }

    private Switch toggle(String s) {
        Switch t = new Switch(this);
        t.setText(s);
        t.setTextSize(20);
        t.setTextColor(ink);
        ui.styleSwitch(t);
        t.setPadding(0, dp(12), 0, dp(12));
        page.addView(t);
        return t;
    }

    private SeekBar slider(int max, Runnable update) {
        SeekBar s = new SeekBar(this);
        s.setMax(max);
        ui.styleSlider(s);
        page.addView(s, new LinearLayout.LayoutParams(-1, -2));
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            public void onProgressChanged(SeekBar b, int n, boolean user) {
                update.run();
            }

            public void onStartTrackingTouch(SeekBar b) {
            }

            public void onStopTrackingTouch(SeekBar b) {
            }
        });
        return s;
    }
}
