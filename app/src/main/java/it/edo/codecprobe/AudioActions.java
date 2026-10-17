package it.edo.codecprobe;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import java.io.*;
import java.util.ArrayList;

/** Commands and log export belong to Audio, independently of video actions. */
final class AudioActions {
    static final int EXPORT=103;
    private final AudioController audio;
    AudioActions(AudioController audio){this.audio=audio;}
    void start(){
        if(audio.state.readingNames||audio.state.readingLyrics){audio.toast("Attendi la lettura dei nomi di brani e testi");return;}
        if(audio.state.inputs.isEmpty()||audio.state.folder==null){audio.toast("Seleziona almeno un FLAC e una cartella audio");return;}
        if(audio.state.lyricsInputs.size()>1000){audio.toast("Seleziona al massimo 1000 testi .lrc per questa coda");return;}
        if(audio.state.inputs.size()>500){audio.toast("Per questa prova seleziona al massimo 500 brani");return;}
        if(ConversionService.snapshot().running||AudioConversionService.snapshot().running){audio.toast("Una conversione è già in corso");return;}
        new AlertDialog.Builder(audio.activity).setTitle("Prova FLAC → Opus VBR")
            .setMessage("Tag e copertine verranno copiati e verificati. Per i testi esterni vengono usati solo i .lrc selezionati e abbinati per nome. Gli originali restano disponibili.\n\nConvertire "+audio.state.inputs.size()+" brani a "+audio.state.bitrate+" kbit/s?")
            .setNegativeButton("Indietro",null).setPositiveButton("Converti",(dialog,which)->launch()).show();
    }
    private void launch(){
        if(Build.VERSION.SDK_INT>=33&&audio.activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            audio.activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},120);
        Intent command=new Intent(audio.activity,AudioConversionService.class).setAction(AudioConversionService.START)
            .putStringArrayListExtra("inputs",new ArrayList<>(audio.state.inputs)).putStringArrayListExtra("names",new ArrayList<>(audio.state.names))
            .putStringArrayListExtra("lyricsInputs",new ArrayList<>(audio.state.lyricsInputs)).putStringArrayListExtra("lyricsNames",new ArrayList<>(audio.state.lyricsNames))
            .putExtra("folder",audio.state.folder).putExtra("bitrate",audio.state.bitrate);
        try{audio.activity.startForegroundService(command);audio.screen.sections.focusCard(AudioScreen.CONVERSION);}
        catch(RuntimeException error){audio.toast("Avvio audio fallito: "+error.getMessage());}
    }
    void cancel(){long expected=AudioConversionService.snapshot().startedAt;new AlertDialog.Builder(audio.activity).setTitle("Interrompere la coda audio?")
        .setMessage("Il brano in corso non viene salvato. I risultati già completati restano nella cartella scelta.")
        .setNegativeButton("Continua",null).setPositiveButton("Interrompi",(dialog,which)->audio.activity.startService(new Intent(audio.activity,AudioConversionService.class).setAction(AudioConversionService.CANCEL).putExtra("startedAt",expected))).show();}
    void export(){
        if(!AudioLog.file(audio.activity).isFile()){audio.toast("Nessun log audio da esportare");return;}
        audio.activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_TITLE,"v"+BuildConfig.VERSION_NAME+"-Audio-log-"+System.currentTimeMillis()+".txt"),EXPORT);
    }
    void result(int request,int code,Intent data){
        if(request!=EXPORT||code!=Activity.RESULT_OK||data==null||data.getData()==null)return;Uri destination=data.getData();
        audio.selection.worker.execute(()->{
            String message;
            try(InputStream in=new FileInputStream(AudioLog.file(audio.activity));OutputStream out=audio.activity.getContentResolver().openOutputStream(destination,"w")){
                if(out==null)throw new IOException("Destinazione non scrivibile");byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);message="Log audio salvato";
            }catch(Exception error){message="Esportazione log fallita: "+error.getMessage();}
            String done=message;audio.activity.runOnUiThread(()->{if(!audio.state.destroyed)audio.toast(done);});
        });
    }
}
