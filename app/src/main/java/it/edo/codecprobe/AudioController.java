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
    final AudioActivity activity;final AudioState state;final AudioScreen screen;final AudioSelection selection;final AudioActions actions;final MediaDestinationsController destinations;final ResultDialog resultDialog;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean visible,reportRunning;private int reportCount=-1;private long reportSession=-1;
    private final Runnable tick=new Runnable(){public void run(){if(!visible||state.destroyed)return;screen.dashboard.monitor.sample(SystemClock.elapsedRealtime());renderRun();handler.postDelayed(this,500);}};
    AudioController(AudioActivity activity,Bundle saved){
        this.activity=activity;state=AudioDraftStore.load(activity,!AudioConversionService.snapshot().running);
        screen=new AudioScreen(new AppUi(activity),AudioDraftStore.expansion(activity,saved),state);
        resultDialog=new ResultDialog(screen.ui,this::toast);selection=new AudioSelection(this);
        destinations=new MediaDestinationsController(new MediaDestinationsController.Host(){
            public AppUi ui(){return screen.ui;}public java.util.List<String> inputs(){return state.inputs;}public java.util.List<String> names(){return state.names;}public int selected(){return state.selectedIndex;}
            public DestinationDraft draft(){return state.destinations;}public String defaultFolder(){return state.folder;}public boolean busy(){return AudioController.this.busy();}
            public void persist(){save();}public void changed(){renderRun();}public void toast(String text){AudioController.this.toast(text);}
        },screen.destinations,MediaKind.AUDIO,saved);actions=new AudioActions(this);
        screen.reload.setOnClickListener(v->selection.reload());
        screen.pickLyrics.setOnClickListener(v->selection.pickLyrics());screen.clearLyrics.setOnClickListener(v->selection.clearLyrics());
        screen.lyricsToggle.setOnClickListener(v->{state.lyricsExpanded=!state.lyricsExpanded;save();render();});
        screen.remove.setOnClickListener(v->selection.remove());
        screen.start.setOnClickListener(v->actions.start());screen.cancel.setOnClickListener(v->actions.cancel());screen.retry.setOnClickListener(v->actions.retry());
        screen.exportLog.setOnClickListener(v->actions.export());screen.clearLog.setOnClickListener(v->actions.clearLog());

        screen.pick.setOnClickListener(v->selection.pick());screen.clear.setOnClickListener(v->reset());
        screen.settings.setOnClickListener(v->openSettings());
        screen.onlySmaller.setOnCheckedChangeListener((b,value)->{state.onlySmaller=value;save();});
        screen.keepScreenOn.setOnCheckedChangeListener((b,value)->{state.keepScreenOn=value;save();renderRun();});
        screen.bitrate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar slider,int progress,boolean fromUser){state.bitrate=64+progress*8;save();render();}
            public void onStartTrackingTouch(SeekBar slider){}public void onStopTrackingTouch(SeekBar slider){}
        });
        render();selection.restore();if(AudioConversionService.snapshot().running)screen.sections.focusCard(AudioScreen.CONVERSION);
    }
    boolean busy(){return state.preparing||state.exporting||AudioConversionService.snapshot().running||destinations!=null&&destinations.choosing();}
    void render(){
        state.notice="";
        int count=state.inputs.size();screen.selected.setText(count==0?"Nessun brano selezionato":(state.selectedIndex+1)+" / "+count+" brani"+(state.readingNames?" · lettura nomi…":""));
        selection.renderFiles();
        int matched=0,ambiguous=0;java.util.Set<String> used=new java.util.HashSet<>();
        if(!state.readingNames&&!state.readingLyrics)for(int n=0;n<count;n++)try{String uri=AudioLyrics.match(state.inputs.get(n),state.names.get(n),state.lyricsInputs,state.lyricsNames);if(!uri.isEmpty()){matched++;used.add(uri);}}catch(java.io.IOException error){ambiguous++;}
        screen.lyrics.setText(state.readingLyrics?"Lettura nomi dei testi…":state.lyricsInputs.isEmpty()?"Nessun testo .lrc selezionato":matched+" / "+count+" brani con testo abbinato · "+(state.lyricsInputs.size()-used.size())+" testi non abbinati"+(ambiguous>0?" · "+ambiguous+" abbinamenti ambigui":""));
        screen.lyricsPanel.setVisibility(state.lyricsExpanded?View.VISIBLE:View.GONE);screen.lyricsToggle.setText((state.lyricsExpanded?"▾ ":"▸ ")+"Testi esterni .lrc · opzionali");
        screen.bitrateLabel.setText("Bitrate · "+state.bitrate+" kbit/s");
        int[] values={96,128,160};for(int n=0;n<values.length;n++)screen.presets.get(n).setSelected(state.bitrate==values[n]);renderRun();
    }
    void renderRun(){
        AudioRunState run=AudioConversionService.snapshot();if(!run.running&&!state.preparing&&!state.exporting&&refreshDefaults())return;if(run.running)state.preparing=false;boolean active=run.running,blocked=active||state.preparing||state.exporting||destinations.choosing();destinations.render();
        screen.reload.setVisibility(state.inputs.isEmpty()?View.GONE:View.VISIBLE);screen.reload.setEnabled(!blocked&&!state.readingNames);
        screen.pick.setEnabled(!blocked);screen.clear.setEnabled(!blocked&&(!state.inputs.isEmpty()||!state.lyricsInputs.isEmpty()||run.total>0||state.readingLyrics||state.readingNames));screen.bitrate.setEnabled(!blocked);for(Button preset:screen.presets)preset.setEnabled(!blocked);
        screen.onlySmaller.setEnabled(!blocked);screen.remove.setEnabled(!blocked&&!state.readingNames);
        for(int n=0;n<screen.navigator.getChildCount();n++)screen.navigator.getChildAt(n).setEnabled(!blocked);
        screen.pickLyrics.setEnabled(!blocked);screen.clearLyrics.setEnabled(!blocked&&(!state.lyricsInputs.isEmpty()||state.readingLyrics));
        screen.start.setEnabled(!blocked&&!state.readingLyrics&&!state.readingNames&&!ConversionService.snapshot().running&&!state.inputs.isEmpty()&&destinations.ready());
        screen.cancel.setEnabled(active&&!run.cancelling);screen.clearLog.setEnabled(!blocked&&AudioLog.file(activity).isFile());screen.exportLog.setEnabled(!blocked&&AudioLog.file(activity).isFile());
        screen.status.setText(state.preparing?"Verifica di cartella e testi prima dell’avvio…":!active&&!state.notice.isEmpty()?state.notice:run.message);
        screen.summary.setText(run.summary());screen.totalProgress.setText(run.total>0?run.queuePercent()+"%":"—");screen.current.setText(run.name);screen.percent.setText(run.total>0?run.percent+"%":"0%");screen.progress.setProgress(run.percent);
        screen.seconds.setText(run.rate>0?String.format(Locale.ROOT,"%.1f / %s s",run.doneSamples/(double)run.rate,run.totalSamples>0?String.format(Locale.ROOT,"%.1f",run.totalSamples/(double)run.rate):"—"):"—");
        long now=active?SystemClock.elapsedRealtime():run.endedAt;screen.elapsed.setText(time(run.startedAt>0?Math.max(0,now-run.startedAt):0));
        screen.parameters.setText(run.total>0?"Opus · VBR · "+run.bitrate+" kbit/s · output 48 kHz\nSalva solo se più piccolo: "+(run.onlySmaller?"sì":"no"):"—");
        screen.dashboard.parameter("Solo se più piccolo",run.total>0?(run.onlySmaller?"ON":"OFF"):"—");screen.dashboard.parameter("Bitrate",run.total>0?run.bitrate+" kbit/s":"—");screen.dashboard.parameter("Modalità",run.total>0?"VBR":"—");screen.dashboard.parameter("Frequenza output",run.total>0?"48 kHz":"—");
        screen.log.setText(run.log.isEmpty()?"Nessun log audio":run.log);screen.savings.setText(run.total>0?run.savings():"");screen.savings.setVisibility(View.GONE);screen.workReport.title.setText(run.running?"REPORT LAVORI · IN CORSO":"REPORT LAVORI");
        screen.retry.setVisibility(!active&&run.count(AudioRunState.Outcome.FAILED)>0?View.VISIBLE:View.GONE);screen.retry.setEnabled(!blocked&&!ConversionService.snapshot().running&&!state.readingLyrics&&!state.readingNames);
        if(reportSession!=run.sessionId||reportCount!=run.results.size()||reportRunning!=run.running){AudioReport.render(this,run);reportCount=run.results.size();reportSession=run.sessionId;reportRunning=run.running;}
        keepAwake(active);
    }
    void openSettings(){
        state.destinations.sync(state.inputs,state.folder);String currentFolder=state.inputs.isEmpty()?state.folder:state.destinations.plan.effective(state.selectedIndex).origin?null:state.destinations.plan.effective(state.selectedIndex).folder;
        activity.startActivity(new android.content.Intent(activity,SettingsActivity.class).putExtra(AudioSettingsScreen.AUDIO,true)
            .putExtra("audioBitrate",state.bitrate).putExtra("audioFolder",currentFolder).putExtra("audioSmaller",state.onlySmaller).putExtra("audioScreen",state.keepScreenOn));
    }
    private boolean refreshDefaults(){
        AudioSettings defaults=new AudioSettings(activity);if(defaults.revision==state.settingsRevision)return false;
        defaults.apply(state);screen.bitrate.setProgress((state.bitrate-64)/8);screen.onlySmaller.setChecked(state.onlySmaller);screen.keepScreenOn.setChecked(state.keepScreenOn);save();render();return true;
    }
    void reset(){if(busy()){toast("Completa prima l’operazione in corso");return;}AudioConversionService.clear();selection.clearLyrics();selection.clear();screen.sections.focusCard(AudioScreen.FILES);toast("Selezione e resoconto svuotati · file e log conservati");}
    static String time(long millis){return SessionClock.duration(millis);}
    private void keepAwake(boolean running){if(visible&&running&&state.keepScreenOn)activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
    void resume(){if(visible)return;visible=true;screen.dashboard.monitor.reset();destinations.refresh();handler.post(tick);}
    void save(){AudioDraftStore.save(activity,state);}
    void pause(){visible=false;handler.removeCallbacks(tick);keepAwake(false);save();AudioDraftStore.saveExpansion(activity,screen.sections);}
    void destroy(){visible=false;handler.removeCallbacks(tick);keepAwake(false);state.destroyed=true;screen.dashboard.monitor.close();screen.sections.closed=true;destinations.close();resultDialog.close();actions.close();selection.close();}
    void toast(String message){Toast.makeText(activity,message,Toast.LENGTH_LONG).show();}
}
