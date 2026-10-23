package it.edo.codecprobe;

import android.view.View;
import android.view.WindowManager;

/** Render the immutable service snapshot; no queue or file mutations. */
final class ImageRunPresenter {
    static void render(ImageController images,ImageRunState run){
        ImageScreen s=images.screen;boolean busy=images.busy();s.checkMetadata.setEnabled(!busy&&!images.state.inputs.isEmpty());s.convert.setEnabled(!busy&&!images.state.inputs.isEmpty()&&images.destinations.ready());s.cancel.setEnabled(run.running&&!run.cancelling);s.pause.setEnabled(run.running&&!run.cancelling);UiText.set(s.pause,run.pauseRequested||run.paused?"Riprendi":"Pausa");
        s.progress.setVisibility(View.VISIBLE);s.progress.setIndeterminate(run.running&&!run.paused||images.state.starting);if(!run.running||run.paused)s.progress.setProgress(0);UiText.set(s.dashboard.percent,"—");
        UiText.set(s.runStatus,images.state.starting?"Preparazione della coda…":ImageConversionService.displayMessage(run).isEmpty()?"Scegli foto, cartelle e qualità.":ImageConversionService.displayMessage(run));
        UiText.set(s.dashboard.summary,run.metadataOnly?"VERIFICA METADATI · "+run.completed+" / "+run.total+"\nCompatibili: "+run.checked+" · Da verificare: "+run.failed+" · Annullate: "+run.cancelledCount+"\nPixel non verificati · nessuna conversione":run.total>0?"Foto "+run.current+" / "+run.total+" · completate "+run.completed+"\nRiuscite: "+run.saved+" · Fallite: "+run.failed+"\nGià presenti: "+run.alreadyDone+" · Senza risparmio: "+run.skipped+" · Annullate: "+run.cancelledCount:"Nessuna coda avviata");UiText.set(s.dashboard.current,run.name.isEmpty()?(run.metadataOnly?"Nessuna foto in verifica":"Nessun file in conversione"):run.name);UiText.set(s.dashboard.totalProgress,run.total>0?run.completed+" / "+run.total+" completate":"—");
        UiText.set(s.dashboard.elapsed,run.startedAt>0?SessionClock.duration(Math.max(0,(run.endedAt>0?run.endedAt:android.os.SystemClock.elapsedRealtime())-run.startedAt)):"00:00:00");
        ImageOptions used=run.metadataOnly?null:run.options;s.dashboard.parameter("Solo se più piccolo",used==null?"—":used.onlySmaller?"ON":"OFF");s.dashboard.parameter("Qualità",used==null?"—":used.quality+"/100");s.dashboard.parameter("CRF",used==null?"—":Double.toString((100-used.quality)/2.0));s.dashboard.parameter("Preset",used==null?"—":used.preset);s.dashboard.parameter("Tune",used==null?"—":used.tune);s.dashboard.parameter("Formato",used==null?"—":"HEIC · YCbCr 4:2:0 · 8 bit");s.dashboard.parameter("Dimensioni",used==null?"—":"Originali");
        UiText.set(s.parameters,(run.running&&run.options!=null?run.options:images.state.options).description());
        images.report.render(run);UiText.set(s.log,run.log.isEmpty()?"Nessun log immagini":run.log);
        if(images.visible()&&run.running&&!run.paused&&images.state.options.keepScreenOn)images.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else images.activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private ImageRunPresenter(){}
}
