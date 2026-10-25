package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;

/** Build and explicitly save image defaults. Does not mutate a queued conversion snapshot. */
final class ImageSettingsScreen {
    final Activity activity;final AppUi ui;final LinearLayout page;final ImageQualityControls quality;final CheckBox smaller,screenOn,copyOriginal;final TextView folderValue;final Button save,back,chooseFolder,removeFolder,copy;
    String folder;
    ImageSettingsScreen(Activity activity,Bundle saved){
        this.activity=activity;ui=new AppUi(activity);SettingsPage frame=new SettingsPage(ui,"Impostazioni immagini","HEIC · H.265 · v"+BuildConfig.VERSION_NAME);page=frame.page;back=frame.back;ImageOptions initial=ImageOptions.load(activity);
        if(saved!=null)initial=new ImageOptions(saved.getInt("quality",65),saved.getString("preset","slow"),saved.getString("tune","standard"),saved.getString("folder"),saved.getBoolean("smaller",true),saved.getBoolean("screenOn",false),saved.getBoolean("copyOriginalWhenLarger",false),0);
        LinearLayout dest=frame.section("CARTELLA PREDEFINITA");folder=initial.folder;folderValue=ui.text(dest,"",16);showFolder();chooseFolder=ui.button(dest,"Scegli cartella predefinita");chooseFolder.setOnClickListener(v->activity.startActivityForResult(new Intent(activity,FolderPickerActivity.class),10));removeFolder=ui.button(dest,"Rimuovi cartella predefinita");removeFolder.setOnClickListener(v->{folder=null;showFolder();});
        LinearLayout compression=frame.section("QUALITÀ PREDEFINITA");quality=new ImageQualityControls(ui,compression,()->{});quality.set(initial);
        LinearLayout options=frame.section("OPZIONI PREDEFINITE");smaller=check(ui,options,"Salva solo se più piccolo",initial.onlySmaller);screenOn=check(ui,options,"Mantieni schermo acceso",initial.keepScreenOn);
        copyOriginal=check(ui,options,"Copia l’originale se l’HEIC non è più piccolo",initial.copyOriginalWhenLarger);copyOriginal.setEnabled(initial.onlySmaller);smaller.setOnCheckedChangeListener((button,checked)->copyOriginal.setEnabled(checked));
        ui.text(options,"Il confronto delle dimensioni include tutti i metadati conservati. Le modifiche salvate valgono dalla prossima coda.",13);
        SettingsPage.Actions actions=frame.actions();copy=actions.copy;copy.setOnClickListener(v->{try{ImageOptions current=ImageOptions.from(activity.getIntent());quality.set(current);folder=current.folder;smaller.setChecked(current.onlySmaller);screenOn.setChecked(current.keepScreenOn);copyOriginal.setChecked(current.copyOriginalWhenLarger);showFolder();}catch(IllegalArgumentException ignored){}});
        save=actions.save;save.setOnClickListener(v->{value().save(activity);Toast.makeText(activity,"Impostazioni immagini salvate",Toast.LENGTH_SHORT).show();activity.finish();});ui.install(page);
    }
    static CheckBox check(AppUi ui,LinearLayout parent,String text,boolean selected){return SettingsPage.check(ui,parent,text,selected);}
    ImageOptions value(){return new ImageOptions(quality.value(),quality.preset(),quality.tune(),folder,smaller.isChecked(),screenOn.isChecked(),copyOriginal.isChecked(),0);}
    private void showFolder(){String text="Nessuna cartella predefinita";if(folder!=null)try{text=android.provider.DocumentsContract.getTreeDocumentId(Uri.parse(folder));}catch(IllegalArgumentException e){text=folder;}folderValue.setText(text);}
    void saveState(Bundle out){ImageOptions value=value();out.putInt("quality",value.quality);out.putString("preset",value.preset);out.putString("tune",value.tune);out.putString("folder",folder);out.putBoolean("smaller",value.onlySmaller);out.putBoolean("screenOn",value.keepScreenOn);out.putBoolean("copyOriginalWhenLarger",value.copyOriginalWhenLarger);}
    void result(int request,int code,Intent data){
        if(request!=10||code!=Activity.RESULT_OK||data==null||data.getData()==null)return;
        try{int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);if(flags!=(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))throw new SecurityException("Servono lettura e scrittura");activity.getContentResolver().takePersistableUriPermission(data.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);folder=data.getData().toString();showFolder();}
        catch(SecurityException e){Toast.makeText(activity,"Cartella non salvata: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
}
