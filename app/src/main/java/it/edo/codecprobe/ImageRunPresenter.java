package it.edo.codecprobe;

import android.view.View;
import android.view.WindowManager;
import java.util.Locale;

/** Render the immutable service snapshot; no queue or file mutations. */
final class ImageRunPresenter {
    static void render(ImageController images,ImageRunState run){
        ImageScreen s=images.screen;boolean busy=images.busy();s.convert.setEnabled(!busy&&!images.state.inputs.isEmpty()&&images.state.options.folder!=null);s.cancel.setEnabled(run.running&&!run.cancelling);
        s.progress.setVisibility(run.running||images.state.starting?View.VISIBLE:View.GONE);s.progress.setIndeterminate(true);
        s.runStatus.setText(images.state.starting?"Preparazione della coda…":run.message.isEmpty()?"Scegli foto, cartella e qualità.":(run.total>0?"Foto "+run.current+" / "+run.total+" · completate "+run.results.size()+"\n"+run.name+"\n":"")+run.message);
        s.parameters.setText((run.running&&run.options!=null?run.options:images.state.options).description());
        StringBuilder report=new StringBuilder();long input=0,output=0;int saved=0,failed=0,skipped=0;
        for(ImageRunState.Result r:run.results){if(r.outcome==ImageRunState.Outcome.SAVED){input+=r.inputBytes;output+=r.outputBytes;saved++;}else if(r.outcome==ImageRunState.Outcome.FAILED)failed++;else if(r.outcome==ImageRunState.Outcome.NOT_SMALLER)skipped++;
            report.append(r.name).append("\n").append(r.message);if(r.outcome==ImageRunState.Outcome.SAVED)report.append(String.format(Locale.ITALY," · %.2f → %.2f MB",r.inputBytes/1000000.0,r.outputBytes/1000000.0));report.append("\n\n");}
        if(!run.results.isEmpty())report.insert(0,String.format(Locale.ITALY,"Salvate %d · fallite %d · senza risparmio %d\nRisparmio sui file salvati: %.2f MB\n\n",saved,failed,skipped,(input-output)/1000000.0));
        s.report.setText(report.length()==0?"Nessuna conversione eseguita.":report);s.log.setText(run.log.isEmpty()?"Nessun log immagini":run.log);
        if(images.visible()&&run.running&&run.options!=null&&run.options.keepScreenOn)images.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else images.activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private ImageRunPresenter(){}
}
