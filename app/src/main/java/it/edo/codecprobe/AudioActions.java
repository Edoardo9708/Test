package it.edo.codecprobe;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

/** Validates an immutable queue before launch; all provider reads run off the UI thread. */
final class AudioActions {
    static final int EXPORT=103;
    private final AudioController audio;private AlertDialog confirmation;
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    AudioActions(AudioController audio){this.audio=audio;}
    void start(){
        if(audio.busy()||ConversionService.snapshot().running){audio.toast("Una conversione o un’operazione è già in corso");return;}
        if(audio.state.readingNames||audio.state.readingLyrics){audio.toast("Attendi la lettura dei nomi di brani e testi");return;}
        if(audio.state.inputs.isEmpty()||audio.state.folder==null){audio.toast("Seleziona almeno un brano e una cartella audio");return;}
        if(audio.state.inputs.size()>500||audio.state.lyricsInputs.size()>1000){audio.toast("Seleziona al massimo 500 brani e 1000 testi per coda");return;}
        handler.removeCallbacksAndMessages(null);
        final ArrayList<String> inputs=new ArrayList<>(audio.state.inputs),names=new ArrayList<>(audio.state.names),lyrics=new ArrayList<>(audio.state.lyricsInputs),lyricNames=new ArrayList<>(audio.state.lyricsNames);
        final String folder=audio.state.folder;final int bitrate=audio.state.bitrate;final boolean onlySmaller=audio.state.onlySmaller;
        audio.state.preparing=true;audio.state.notice="";audio.renderRun();
        audio.selection.worker.execute(()->{
            String error=null;int matched=0;HashSet<String> used=new HashSet<>();
            try{
                OutputDocuments.names(audio.activity,Uri.parse(folder));
                for(int n=0;n<inputs.size();n++){
                    if(audio.state.destroyed||Thread.currentThread().isInterrupted())return;
                    AudioMediaDecoder.kind(names.get(n));
                    String uri=AudioLyrics.match(inputs.get(n),names.get(n),lyrics,lyricNames);if(!uri.isEmpty()){matched++;used.add(uri);}
                }
            }catch(Exception problem){error=problem.getMessage();}
            final String failure=error;final int matches=matched,unused=lyrics.size()-used.size();
            audio.activity.runOnUiThread(()->{
                if(audio.state.destroyed)return;
                if(failure!=null){audio.state.preparing=false;audio.state.notice="Avvio non riuscito: "+failure;audio.renderRun();audio.toast(audio.state.notice);return;}
                Runnable launch=()->launch(inputs,names,lyrics,lyricNames,folder,bitrate,onlySmaller);
                if(!lyrics.isEmpty()&&(matches<inputs.size()||unused>0)){
                    confirmation=new AlertDialog.Builder(audio.activity).setTitle("Controlla i testi esterni")
                        .setMessage(matches+" / "+inputs.size()+" brani hanno un .lrc abbinato.\n"+(inputs.size()-matches)+" brani verranno convertiti senza testo esterno.\n"+unused+" testi selezionati non sono usati.\n\nContinuare?")
                        .setNegativeButton("Indietro",(d,w)->ready()).setPositiveButton("Converti",(d,w)->launch.run()).setOnCancelListener(d->ready()).create();confirmation.show();
                }else launch.run();
            });
        });
    }
    private void ready(){audio.state.preparing=false;if(!audio.state.destroyed)audio.renderRun();}
    private void launch(ArrayList<String> inputs,ArrayList<String> names,ArrayList<String> lyrics,ArrayList<String> lyricNames,String folder,int bitrate,boolean onlySmaller){
        if(audio.state.destroyed)return;
        if(ConversionService.snapshot().running||AudioConversionService.snapshot().running){ready();audio.toast("Una conversione è già in corso");return;}
        if(Build.VERSION.SDK_INT>=33&&audio.activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            audio.activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},120);
        Intent command=new Intent(audio.activity,AudioConversionService.class).setAction(AudioConversionService.START)
            .putStringArrayListExtra("inputs",inputs).putStringArrayListExtra("names",names).putStringArrayListExtra("lyricsInputs",lyrics).putStringArrayListExtra("lyricsNames",lyricNames)
            .putExtra("folder",folder).putExtra("bitrate",bitrate).putExtra("onlySmaller",onlySmaller);
        try{audio.activity.startForegroundService(command);audio.screen.sections.focusCard(AudioScreen.CONVERSION);
            handler.postDelayed(()->{if(!audio.state.destroyed&&audio.state.preparing&&!AudioConversionService.snapshot().running){audio.state.notice="Avvio audio non confermato: "+AudioConversionService.snapshot().message;ready();}},5000);
        }
        catch(RuntimeException error){audio.state.notice="Avvio audio fallito: "+error.getMessage();audio.toast(audio.state.notice);ready();}
    }
    void retry(){
        if(audio.busy()||ConversionService.snapshot().running)return;
        ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>();
        for(AudioRunState.Result result:AudioConversionService.snapshot().results)if(result.outcome==AudioRunState.Outcome.FAILED&&result.input!=null){inputs.add(result.input);names.add(result.name);}
        if(inputs.isEmpty()){audio.toast("Nessun brano fallito da riprovare");return;}
        ++audio.state.selectionGeneration;audio.state.inputs.clear();audio.state.inputs.addAll(inputs);audio.state.names.clear();audio.state.names.addAll(names);audio.state.selectedIndex=0;audio.save();audio.render();start();
    }
    void cancel(){AudioRunState run=AudioConversionService.snapshot();if(!run.running||run.cancelling)return;long expected=run.startedAt;
        confirmation=new AlertDialog.Builder(audio.activity).setTitle("Interrompere la coda audio?")
            .setMessage("Il brano in corso non viene salvato. I risultati già completati restano nella cartella scelta.")
            .setNegativeButton("Continua",null).setPositiveButton("Interrompi",(dialog,which)->audio.activity.startService(new Intent(audio.activity,AudioConversionService.class).setAction(AudioConversionService.CANCEL).putExtra("startedAt",expected))).create();confirmation.show();
    }
    void clearLog(){
        if(audio.busy())return;
        confirmation=new AlertDialog.Builder(audio.activity).setTitle("Cancellare il log audio?").setMessage("I file audio e il resoconto restano conservati.")
            .setNegativeButton("Indietro",null).setPositiveButton("Cancella",(d,w)->{
                File file=AudioLog.file(audio.activity);if(file.exists()&&!file.delete()){audio.toast("Impossibile cancellare il log audio");return;}
                AudioConversionService.clearLog();audio.renderRun();
            }).create();confirmation.show();
    }
    void export(){
        if(audio.busy())return;
        if(!AudioLog.file(audio.activity).isFile()){audio.toast("Nessun log audio da esportare");return;}
        audio.activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_TITLE,"v"+BuildConfig.VERSION_NAME+"-Audio-log-"+System.currentTimeMillis()+".txt"),EXPORT);
    }
    void result(int request,int code,Intent data){
        if(request!=EXPORT||code!=Activity.RESULT_OK||data==null||data.getData()==null||audio.state.destroyed)return;Uri destination=data.getData();
        if(audio.busy()){audio.toast("Attendi la fine dell’operazione prima di esportare");return;}
        audio.state.exporting=true;audio.renderRun();
        audio.selection.worker.execute(()->{
            String message;
            try(InputStream in=new FileInputStream(AudioLog.file(audio.activity));OutputStream out=audio.activity.getContentResolver().openOutputStream(destination,"w")){
                if(out==null)throw new IOException("Destinazione non scrivibile");byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){if(audio.state.destroyed||Thread.currentThread().isInterrupted())throw new IOException("Esportazione annullata");out.write(buffer,0,n);}message="Log audio salvato";
            }catch(Exception error){message="Esportazione log fallita: "+error.getMessage();}
            String done=message;audio.activity.runOnUiThread(()->{if(!audio.state.destroyed){audio.state.exporting=false;audio.renderRun();audio.toast(done);}});
        });
    }
    void close(){handler.removeCallbacksAndMessages(null);if(confirmation!=null)confirmation.dismiss();}
}
