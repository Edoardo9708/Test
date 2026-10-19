package it.edo.codecprobe;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import java.util.Locale;
import android.provider.DocumentsContract;
import android.net.Uri;
import android.widget.*;

/** Audio lifecycle and rendering; selection and worker actions stay in their own modules. */
final class AudioController {
    final AudioActivity activity;final AudioState state;final AudioScreen screen;final AudioSelection selection;final AudioActions actions;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean visible;private int reportCount=-1;private long reportSession=-1;
    private final Runnable tick=new Runnable(){public void run(){if(!visible||state.destroyed)return;renderRun();handler.postDelayed(this,500);}};
    AudioController(AudioActivity activity,Bundle saved){
        this.activity=activity;state=AudioDraftStore.load(activity);
        screen=new AudioScreen(new AppUi(activity),AudioDraftStore.expansion(activity,saved),state);
        selection=new AudioSelection(this);actions=new AudioActions(this);
        screen.pickLyrics.setOnClickListener(v->selection.pickLyrics());screen.clearLyrics.setOnClickListener(v->selection.clearLyrics());
        screen.lyricsToggle.setOnClickListener(v->{state.lyricsExpanded=!state.lyricsExpanded;save();render();});
        screen.details.setOnClickListener(v->screen.inputDetails.setVisibility(screen.inputDetails.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));screen.remove.setOnClickListener(v->selection.remove());
        screen.start.setOnClickListener(v->actions.start());screen.cancel.setOnClickListener(v->actions.cancel());screen.retry.setOnClickListener(v->actions.retry());
        screen.exportLog.setOnClickListener(v->actions.export());screen.clearLog.setOnClickListener(v->actions.clearLog());
        screen.toggleLog.setOnClickListener(v->{boolean show=screen.log.getVisibility()!=View.VISIBLE;screen.log.setVisibility(show?View.VISIBLE:View.GONE);screen.toggleLog.setText(show?"Nascondi ultimi messaggi":"Mostra ultimi messaggi");});
        screen.pick.setOnClickListener(v->selection.pick());screen.clear.setOnClickListener(v->reset());
        screen.chooseFolder.setOnClickListener(v->selection.folder());screen.settings.setOnClickListener(v->screen.sections.focusCard(AudioScreen.COMPRESSION));
        screen.onlySmaller.setOnCheckedChangeListener((b,value)->{state.onlySmaller=value;save();});
        screen.keepScreenOn.setOnCheckedChangeListener((b,value)->{state.keepScreenOn=value;save();renderRun();});
        screen.bitrate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar slider,int progress,boolean fromUser){state.bitrate=64+progress*8;save();render();}
            public void onStartTrackingTouch(SeekBar slider){}public void onStopTrackingTouch(SeekBar slider){}
        });
        render();selection.restore();if(AudioConversionService.snapshot().running)screen.sections.focusCard(AudioScreen.CONVERSION);
    }
    boolean busy(){return state.preparing||state.exporting||AudioConversionService.snapshot().running;}
    void render(){
        state.notice="";
        int count=state.inputs.size();screen.selected.setText(count==0?"Nessun brano selezionato":(state.selectedIndex+1)+" / "+count+" brani"+(state.readingNames?" · lettura nomi…":""));
        selection.renderFiles();
        String folder="Nessuna cartella audio scelta";
        if(state.folder!=null)try{folder=DocumentsContract.getTreeDocumentId(Uri.parse(state.folder));}catch(IllegalArgumentException error){folder=state.folder;}
        int matched=0,ambiguous=0;java.util.Set<String> used=new java.util.HashSet<>();
        if(!state.readingNames&&!state.readingLyrics)for(int n=0;n<count;n++)try{String uri=AudioLyrics.match(state.inputs.get(n),state.names.get(n),state.lyricsInputs,state.lyricsNames);if(!uri.isEmpty()){matched++;used.add(uri);}}catch(java.io.IOException error){ambiguous++;}
        screen.lyrics.setText(state.readingLyrics?"Lettura nomi dei testi…":state.lyricsInputs.isEmpty()?"Nessun testo .lrc selezionato":matched+" / "+count+" brani con testo abbinato · "+(state.lyricsInputs.size()-used.size())+" testi non abbinati"+(ambiguous>0?" · "+ambiguous+" abbinamenti ambigui":""));
        screen.lyricsPanel.setVisibility(state.lyricsExpanded?View.VISIBLE:View.GONE);screen.lyricsToggle.setText((state.lyricsExpanded?"▾ ":"▸ ")+"Testi esterni .lrc · opzionali");
        screen.folder.setText(folder);screen.bitrateLabel.setText("Bitrate · "+state.bitrate+" kbit/s");
        int[] values={96,128,160};for(int n=0;n<values.length;n++)screen.presets.get(n).setSelected(state.bitrate==values[n]);renderRun();
    }
    void renderRun(){
        AudioRunState run=AudioConversionService.snapshot();if(run.running)state.preparing=false;boolean active=run.running,blocked=active||state.preparing||state.exporting;
        screen.pick.setEnabled(!blocked);screen.chooseFolder.setEnabled(!blocked);screen.clear.setEnabled(!blocked&&(!state.inputs.isEmpty()||!state.lyricsInputs.isEmpty()||run.total>0||state.readingLyrics||state.readingNames));screen.bitrate.setEnabled(!blocked);for(Button preset:screen.presets)preset.setEnabled(!blocked);
        screen.onlySmaller.setEnabled(!blocked);screen.settings.setEnabled(!blocked);screen.remove.setEnabled(!blocked&&!state.readingNames);
        for(int n=0;n<screen.navigator.getChildCount();n++)screen.navigator.getChildAt(n).setEnabled(!blocked);
        screen.pickLyrics.setEnabled(!blocked);screen.clearLyrics.setEnabled(!blocked&&(!state.lyricsInputs.isEmpty()||state.readingLyrics));
        screen.start.setEnabled(!blocked&&!state.readingLyrics&&!state.readingNames&&!ConversionService.snapshot().running&&!state.inputs.isEmpty()&&state.folder!=null);
        screen.cancel.setEnabled(active&&!run.cancelling);screen.clearLog.setEnabled(!blocked&&AudioLog.file(activity).isFile());screen.exportLog.setEnabled(!blocked&&AudioLog.file(activity).isFile());
        screen.status.setText(state.preparing?"Verifica di cartella e testi prima dell’avvio…":!active&&!state.notice.isEmpty()?state.notice:run.message);
        screen.summary.setText(run.summary());screen.totalProgress.setText(run.total>0?run.queuePercent()+"%":"—");screen.current.setText(run.name);screen.percent.setText(run.total>0?run.percent+"%":"0%");screen.progress.setProgress(run.percent);
        screen.seconds.setText(run.rate>0?String.format(Locale.ROOT,"%.1f / %s s",run.doneSamples/(double)run.rate,run.totalSamples>0?String.format(Locale.ROOT,"%.1f",run.totalSamples/(double)run.rate):"—"):"—");
        long now=active?SystemClock.elapsedRealtime():run.endedAt;screen.elapsed.setText(time(run.startedAt>0?Math.max(0,now-run.startedAt):0));
        screen.parameters.setText(run.total>0?"Opus · VBR · "+run.bitrate+" kbit/s · output 48 kHz\nSalva solo se più piccolo: "+(run.onlySmaller?"sì":"no"):"—");
        screen.log.setText(run.log.isEmpty()?"Nessun log audio":run.log);screen.savings.setText(run.total>0?run.savings():"");
        screen.retry.setVisibility(!active&&run.count(AudioRunState.Outcome.FAILED)>0?View.VISIBLE:View.GONE);screen.retry.setEnabled(!blocked&&!ConversionService.snapshot().running&&!state.readingLyrics&&!state.readingNames);
        if(reportSession!=run.sessionId||reportCount!=run.results.size()){AudioReport.render(this,run);reportCount=run.results.size();reportSession=run.sessionId;}
        keepAwake(active);
    }
    void reset(){if(busy()){toast("Completa prima l’operazione in corso");return;}AudioConversionService.clear();selection.clearLyrics();selection.clear();screen.sections.focusCard(AudioScreen.FILES);toast("Selezione e resoconto svuotati · file e log conservati");}
    static String time(long millis){long seconds=millis/1000;return String.format(Locale.ROOT,"%02d:%02d:%02d",seconds/3600,(seconds/60)%60,seconds%60);}
    private void keepAwake(boolean running){if(visible&&running&&state.keepScreenOn)activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
    void resume(){if(visible)return;visible=true;handler.post(tick);}
    void save(){AudioDraftStore.save(activity,state);}
    void pause(){visible=false;handler.removeCallbacks(tick);keepAwake(false);save();AudioDraftStore.saveExpansion(activity,screen.sections);}
    void destroy(){visible=false;handler.removeCallbacks(tick);keepAwake(false);state.destroyed=true;screen.sections.closed=true;actions.close();selection.close();}
    void toast(String message){Toast.makeText(activity,message,Toast.LENGTH_LONG).show();}
}
