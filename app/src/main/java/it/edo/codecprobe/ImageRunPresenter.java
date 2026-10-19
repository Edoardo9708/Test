package it.edo.codecprobe;

import android.view.View;
import android.view.WindowManager;

/** Render the immutable service snapshot; no queue or file mutations. */
final class ImageRunPresenter {
    static void render(ImageController images,ImageRunState run){
        ImageScreen s=images.screen;boolean busy=images.busy();s.convert.setEnabled(!busy&&!images.state.inputs.isEmpty()&&images.state.options.folder!=null);s.cancel.setEnabled(run.running&&!run.cancelling);
        s.progress.setVisibility(run.running||images.state.starting?View.VISIBLE:View.GONE);s.progress.setIndeterminate(true);
        s.runStatus.setText(images.state.starting?"Preparazione della coda…":run.message.isEmpty()?"Scegli foto, cartella e qualità.":(run.total>0?"Foto "+run.current+" / "+run.total+" · completate "+run.results.size()+"\n"+run.name+"\n":"")+run.message);
        s.parameters.setText((run.running&&run.options!=null?run.options:images.state.options).description());
        images.report.render(run);s.log.setText(run.log.isEmpty()?"Nessun log immagini":run.log);
        if(images.visible()&&run.running&&run.options!=null&&run.options.keepScreenOn)images.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else images.activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private ImageRunPresenter(){}
}
