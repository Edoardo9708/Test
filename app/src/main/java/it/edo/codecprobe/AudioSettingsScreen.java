package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;

/** Dedicated audio settings page hosted by SettingsActivity, with explicit save/back semantics. */
final class AudioSettingsScreen {
    static final String AUDIO="audioSettings";
    final Activity activity;final AppUi ui;final LinearLayout page;
    final SeekBar bitrate;final CheckBox onlySmaller,keepScreenOn;final TextView bitrateValue,folderValue;
    final Button save,copyCurrent,back,chooseFolder,removeFolder;
    final java.util.ArrayList<Button> presets=new java.util.ArrayList<>();String folder;
    AudioSettingsScreen(Activity activity,Bundle state){
        this.activity=activity;ui=new AppUi(activity);SettingsPage frame=new SettingsPage(ui,"Impostazioni audio","Opus · v"+BuildConfig.VERSION_NAME);page=frame.page;back=frame.back;AudioSettings defaults=new AudioSettings(activity);
        LinearLayout destination=frame.section("CARTELLA PREDEFINITA");
        folder=state!=null?state.getString("audioFolder"):defaults.folder;folderValue=ui.text(destination,"",16);showFolder();
        chooseFolder=ui.button(destination,"Scegli cartella predefinita");chooseFolder.setOnClickListener(v->activity.startActivityForResult(new Intent(activity,FolderPickerActivity.class),10));
        removeFolder=ui.button(destination,"Rimuovi cartella predefinita");removeFolder.setOnClickListener(v->{folder=null;showFolder();});
        ui.text(destination,"Usata per le nuove code audio. Nella schermata conversione puoi scegliere una destinazione diversa.",13);
        LinearLayout quality=frame.section("QUALITÀ PREDEFINITA");
        bitrateValue=ui.text(quality,"",20);bitrate=new SeekBar(activity);bitrate.setMax(24);ui.styleSlider(bitrate);quality.addView(bitrate,new LinearLayout.LayoutParams(-1,-2));
        ui.text(quality,"Bitrate · 64–256 kbit/s · VBR",13);
        QualityButtons shortcuts=new QualityButtons(ui,new int[]{96,128,160},new String[]{"96 kbit/s","128 kbit/s","160 kbit/s"},"bitrate",value->bitrate.setProgress((value-64)/8));quality.addView(shortcuts,new LinearLayout.LayoutParams(-1,-2));presets.addAll(shortcuts.buttons);
        bitrate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int value,boolean user){showBitrate();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        bitrate.setProgress((AudioSettings.normalize(state!=null?state.getInt("audioBitrate",defaults.bitrate):defaults.bitrate)-64)/8);showBitrate();
        ui.text(quality,"Un bitrate maggiore dedica più spazio all’audio. Opus usa compressione con perdita; il bitrate è un obiettivo medio.",13);
        LinearLayout options=frame.section("OPZIONI PREDEFINITE");
        onlySmaller=check(options,"Salva solo se più piccolo",state!=null?state.getBoolean("audioSmaller",defaults.onlySmaller):defaults.onlySmaller);
        keepScreenOn=check(options,"Mantieni schermo acceso",state!=null?state.getBoolean("audioScreen",defaults.keepScreenOn):defaults.keepScreenOn);
        ui.text(options,"Le modifiche vengono applicate dopo il salvataggio. Se una conversione è in corso, valgono dalla coda successiva.",13);
        SettingsPage.Actions actions=frame.actions();copyCurrent=actions.copy;copyCurrent.setOnClickListener(v->{
            Intent current=activity.getIntent();bitrate.setProgress((AudioSettings.normalize(current.getIntExtra("audioBitrate",defaults.bitrate))-64)/8);
            folder=current.hasExtra("audioFolder")?current.getStringExtra("audioFolder"):defaults.folder;showFolder();
            onlySmaller.setChecked(current.getBooleanExtra("audioSmaller",defaults.onlySmaller));keepScreenOn.setChecked(current.getBooleanExtra("audioScreen",defaults.keepScreenOn));
        });
        save=actions.save;save.setOnClickListener(v->{AudioSettings.save(activity,value(),folder,onlySmaller.isChecked(),keepScreenOn.isChecked());Toast.makeText(activity,"Impostazioni audio salvate",Toast.LENGTH_SHORT).show();activity.finish();});
        ui.install(page);
    }
    int value(){return 64+bitrate.getProgress()*8;}
    private void showBitrate(){bitrateValue.setText("Bitrate · "+value()+" kbit/s");int[] values={96,128,160};for(int n=0;n<values.length;n++)presets.get(n).setSelected(value()==values[n]);}
    private void showFolder(){String text="Nessuna cartella predefinita";if(folder!=null)try{text=android.provider.DocumentsContract.getTreeDocumentId(Uri.parse(folder));}catch(IllegalArgumentException error){text=folder;}folderValue.setText(text);}
    private CheckBox check(LinearLayout parent,String label,boolean selected){return SettingsPage.check(ui,parent,label,selected);}
    void saveState(Bundle out){out.putString("audioFolder",folder);out.putInt("audioBitrate",value());out.putBoolean("audioSmaller",onlySmaller.isChecked());out.putBoolean("audioScreen",keepScreenOn.isChecked());}
    void result(int request,int code,Intent data){
        if(request!=10)return;
        if(code!=Activity.RESULT_OK||data==null||data.getData()==null){String error=data==null?null:data.getStringExtra(FolderPickerActivity.ERROR);if(error!=null)Toast.makeText(activity,error,Toast.LENGTH_LONG).show();return;}
        try{
            int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if(flags!=(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))throw new SecurityException("Servono lettura e scrittura");
            activity.getContentResolver().takePersistableUriPermission(data.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);folder=data.getData().toString();showFolder();
        }catch(SecurityException error){Toast.makeText(activity,"Cartella non salvata: "+error.getMessage(),Toast.LENGTH_LONG).show();}
    }
}
