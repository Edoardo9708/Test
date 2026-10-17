package it.edo.codecprobe;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.Locale;
import android.provider.DocumentsContract;
import android.net.Uri;
import android.widget.*;

/** Audio lifecycle owner. Video state, service and settings are never imported or reset. */
final class AudioController {
    final AudioActivity activity;final AudioState state;final AudioScreen screen;final AudioSelection selection;final AudioActions actions;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean visible;private int reportCount=-1;
    private final Runnable tick=new Runnable(){public void run(){if(!visible||state.destroyed)return;renderRun();handler.postDelayed(this,500);}};
    AudioController(AudioActivity activity,Bundle saved){
        this.activity=activity;state=AudioDraftStore.load(activity);
        screen=new AudioScreen(new AppUi(activity),AudioDraftStore.expansion(activity,saved),state);
        selection=new AudioSelection(this);actions=new AudioActions(this);
        screen.start.setOnClickListener(v->actions.start());screen.cancel.setOnClickListener(v->actions.cancel());
        screen.exportLog.setOnClickListener(v->actions.export());screen.clearReport.setOnClickListener(v->{AudioConversionService.clear();reportCount=-1;renderRun();});
        screen.pick.setOnClickListener(v->selection.pick());screen.clear.setOnClickListener(v->selection.clear());
        screen.chooseFolder.setOnClickListener(v->selection.folder());screen.settings.setOnClickListener(v->screen.sections.focusCard(AudioScreen.COMPRESSION));
        screen.bitrate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar slider,int progress,boolean fromUser){state.bitrate=64+progress*8;save();render();}
            public void onStartTrackingTouch(SeekBar slider){}public void onStopTrackingTouch(SeekBar slider){}
        });
        render();
    }
    void render(){
        int count=state.inputs.size();screen.selected.setText(count==0?"Nessun brano selezionato":count+" brani selezionati"+(state.readingNames?" · lettura nomi…":""));
        StringBuilder names=new StringBuilder();for(int n=0;n<Math.min(10,count);n++){if(n>0)names.append('\n');names.append(n+1).append(". ").append(state.names.get(n));}
        if(count>10)names.append("\n… e altri ").append(count-10).append(" brani selezionati");screen.names.setText(names);screen.clear.setEnabled(count>0);
        String folder="Nessuna cartella audio scelta";
        if(state.folder!=null)try{folder=DocumentsContract.getTreeDocumentId(Uri.parse(state.folder));}catch(IllegalArgumentException error){folder=state.folder;}
        screen.folder.setText(folder);renderRun();screen.bitrateLabel.setText("Bitrate audio · "+state.bitrate+" kbit/s · VBR");
    }
    void renderRun(){
        AudioRunState run=AudioConversionService.snapshot();
        boolean active=run.running;
        screen.pick.setEnabled(!active);screen.chooseFolder.setEnabled(!active);screen.clear.setEnabled(!active&&!state.inputs.isEmpty());screen.bitrate.setEnabled(!active);for(Button preset:screen.presets)preset.setEnabled(!active);
        screen.start.setEnabled(!active&&!state.readingNames&&!ConversionService.snapshot().running&&!state.inputs.isEmpty()&&state.folder!=null);
        screen.cancel.setEnabled(active&&!run.cancelling);screen.clearReport.setEnabled(!active&&!run.results.isEmpty());
        screen.exportLog.setEnabled(AudioLog.file(activity).isFile());screen.status.setText(run.message);
        int success=0;for(AudioRunState.Result r:run.results)if(r.uri!=null)success++;
        screen.summary.setText(run.current+" / "+run.total+" · riusciti "+success+" · interrotti/falliti "+(run.results.size()-success));
        screen.current.setText(run.name);screen.percent.setText(run.total>0?run.percent+"%":"—");screen.progress.setProgress(run.percent);
        screen.samples.setText(run.rate>0?run.doneSamples+" / "+(run.totalSamples>0?Long.toString(run.totalSamples):"non dichiarati"):"—");
        screen.seconds.setText(run.rate>0?String.format(Locale.ROOT,"%.1f / %s s",run.doneSamples/(double)run.rate,run.totalSamples>0?String.format(Locale.ROOT,"%.1f",run.totalSamples/(double)run.rate):"—"):"—");
        long now=run.running?SystemClock.elapsedRealtime():run.endedAt;
        screen.elapsed.setText(run.startedAt>0?((Math.max(0,now-run.startedAt))/1000)+" s":"—");
        screen.parameters.setText(run.total>0?"Opus · VBR libero · "+run.bitrate+" kbit/s\nComplessità 10 · output 48 kHz\nInput: "+(run.rate>0?run.rate+" Hz · "+run.channels+" canali":"analisi in attesa"):"—");
        screen.log.setText(run.log.isEmpty()?"Nessun log audio":run.log);
        if(reportCount!=run.results.size()){AudioReport.render(this,run);reportCount=run.results.size();}
    }
    void resume(){if(visible)return;visible=true;handler.post(tick);}
    void save(){AudioDraftStore.save(activity,state);}
    void pause(){visible=false;handler.removeCallbacks(tick);save();AudioDraftStore.saveExpansion(activity,screen.sections);}
    void destroy(){visible=false;handler.removeCallbacks(tick);state.destroyed=true;screen.sections.closed=true;selection.close();}
    void toast(String message){Toast.makeText(activity,message,Toast.LENGTH_LONG).show();}
}
