package it.edo.codecprobe;

import android.view.View;
import android.view.WindowManager;

/** Render the immutable service snapshot; no queue or file mutations. */
final class ImageRunPresenter {
    static void render(ImageController images,ImageRunState run){
        ImageScreen s=images.screen;boolean busy=images.busy();s.convert.setEnabled(!busy&&!images.state.inputs.isEmpty()&&images.destinations.ready());s.cancel.setEnabled(run.running&&!run.cancelling);
        s.progress.setVisibility(View.VISIBLE);s.progress.setIndeterminate(run.running||images.state.starting);if(!run.running)s.progress.setProgress(0);s.dashboard.percent.setText("—");
        s.runStatus.setText(images.state.starting?"Preparazione della coda…":run.message.isEmpty()?"Scegli foto, cartelle e qualità.":run.message);
        s.dashboard.summary.setText(run.total>0?"Foto "+run.current+" / "+run.total+" · completate "+run.results.size():"Nessuna coda avviata");s.dashboard.current.setText(run.name.isEmpty()?"Nessun file in conversione":run.name);s.dashboard.totalProgress.setText(run.total>0?run.results.size()+" / "+run.total+" completate":"—");
        s.dashboard.elapsed.setText(run.startedAt>0?SessionClock.duration(Math.max(0,(run.endedAt>0?run.endedAt:android.os.SystemClock.elapsedRealtime())-run.startedAt)):"00:00:00");
        ImageOptions used=run.options;s.dashboard.parameter("Solo se più piccolo",used==null?"—":used.onlySmaller?"ON":"OFF");s.dashboard.parameter("Qualità",used==null?"—":used.quality+"/100");s.dashboard.parameter("CRF",used==null?"—":Double.toString((100-used.quality)/2.0));s.dashboard.parameter("Preset",used==null?"—":used.preset);s.dashboard.parameter("Tune",used==null?"—":used.tune);s.dashboard.parameter("Formato",used==null?"—":"HEIC · YCbCr 4:2:0 · 8 bit");s.dashboard.parameter("Dimensioni",used==null?"—":"Originali");
        s.parameters.setText((run.running&&run.options!=null?run.options:images.state.options).description());
        images.report.render(run);s.log.setText(run.log.isEmpty()?"Nessun log immagini":run.log);
        if(images.visible()&&run.running&&images.state.options.keepScreenOn)images.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else images.activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private ImageRunPresenter(){}
}
