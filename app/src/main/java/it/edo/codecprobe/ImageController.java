package it.edo.codecprobe;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Coordinates the image workspace; slow provider/metadata reads never run on the UI thread. */
final class ImageController {
    final ImageActivity activity;final ImageState state;final ImageScreen screen;final ImageSelection selection;final ImageActions actions;final ImageWorkReport report;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private Future<?> analysis,exportTask;private int generation;private boolean destroyed,visible,applying;
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable tick=new Runnable(){public void run(){if(!visible||destroyed)return;refresh();main.postDelayed(this,500);}};
    ImageController(ImageActivity activity,Bundle saved){
        this.activity=activity;state=ImageDraftStore.load(activity);screen=new ImageScreen(new AppUi(activity),saved);report=new ImageWorkReport(this);selection=new ImageSelection(this,saved);actions=new ImageActions(this);showOptions();
        screen.select.setOnClickListener(v->selection.pick());screen.clear.setOnClickListener(v->{if(busy())return;state.inputs.clear();state.selected=0;ImageConversionService.clear();open();});
        screen.previous.setOnClickListener(v->move(-1));screen.next.setOnClickListener(v->move(1));
        screen.remove.setOnClickListener(v->{if(busy()||state.inputs.isEmpty())return;state.inputs.remove(state.selected);state.clamp();open();});
        screen.export.setOnClickListener(v->selection.export());screen.metadata.setOnClickListener(v->screen.sections.focusCard(ImageScreen.DETAILS));screen.exportLog.setOnClickListener(v->selection.log());screen.chooseFolder.setOnClickListener(v->selection.folder());
        screen.convert.setOnClickListener(v->actions.start());screen.cancel.setOnClickListener(v->actions.cancel());
        screen.quality.changed=this::optionsEdited;screen.smaller.setOnCheckedChangeListener((b,checked)->optionsEdited());screen.screenOn.setOnCheckedChangeListener((b,checked)->optionsEdited());
        screen.settings.setOnClickListener(v->{android.content.Intent intent=new android.content.Intent(activity,ImageSettingsActivity.class);state.options.put(intent);activity.startActivity(intent);});open();
    }
    boolean busy(){return state.exporting||state.starting||ImageConversionService.snapshot().running;}
    boolean visible(){return visible;}
    void move(int delta){if(busy()||state.inputs.isEmpty())return;state.selected=Math.max(0,Math.min(state.inputs.size()-1,state.selected+delta));open();}
    void optionsEdited(){if(applying||busy())return;ImageOptions old=state.options;state.options=new ImageOptions(screen.quality.value(),screen.quality.preset(),screen.quality.tune(),old.folder,screen.smaller.isChecked(),screen.screenOn.isChecked(),old.revision);persist();render();}
    void setFolder(String folder){ImageOptions o=state.options;state.options=new ImageOptions(o.quality,o.preset,o.tune,folder,o.onlySmaller,o.keepScreenOn,o.revision);persist();render();}
    void showOptions(){applying=true;try{screen.quality.set(state.options);screen.smaller.setChecked(state.options.onlySmaller);screen.screenOn.setChecked(state.options.keepScreenOn);}finally{applying=false;}}
    void resume(){visible=true;main.removeCallbacks(tick);tick.run();}
    void pause(){visible=false;main.removeCallbacks(tick);activity.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
    void refresh(){ImageRunState run=ImageConversionService.snapshot();if(state.starting&&state.startingAt>0&&(run.startedAt!=state.previousRun||android.os.SystemClock.elapsedRealtime()-state.startingAt>5000)){state.starting=false;if(run.startedAt==state.previousRun)state.notice=run.message.isEmpty()?"La conversione non è partita: riprova.":run.message;}
        ImageOptions defaults=ImageOptions.load(activity);if(!busy()&&state.options.revision!=defaults.revision){state.options=defaults;showOptions();persist();}render();}
    void persist(){ImageDraftStore.save(activity,state);}
    void save(Bundle out){persist();screen.sections.save(out);selection.save(out);}
    void open(){
        int current=++generation;if(analysis!=null)analysis.cancel(true);
        state.clamp();state.info=null;state.notice="";state.name="";state.reading=!state.inputs.isEmpty();persist();render();if(!state.reading)return;
        String value=state.inputs.get(state.selected);
        analysis=worker.submit(()->{
            String name=ImageFiles.name(activity,Uri.parse(value));JpegInventory.Info info=null;String error="";
            try(InputStream in=activity.getContentResolver().openInputStream(Uri.parse(value))){if(in==null)throw new IOException("Impossibile leggere la foto.");info=JpegInventory.read(in);}
            catch(IOException|RuntimeException e){error=message(e);}
            JpegInventory.Info result=info;String failure=error;
            activity.runOnUiThread(()->{if(destroyed||current!=generation)return;state.name=name;state.info=result;state.notice=failure;state.reading=false;render();});
        });
    }
    void export(Uri source,Uri output){
        if(state.exporting||destroyed)return;state.exporting=true;state.notice="Esportazione inventario…";render();
        exportTask=worker.submit(()->{
            String result="Inventario esportato.";
            try{
                JpegInventory.Info info;
                try(InputStream in=activity.getContentResolver().openInputStream(source)){if(in==null)throw new IOException("Foto non più accessibile.");info=JpegInventory.read(in);}
                // Fully read the source before opening the report destination. Never accept the source URI as output.
                if(source.equals(output))throw new IOException("La destinazione coincide con la foto originale.");
                try(OutputStream out=activity.getContentResolver().openOutputStream(output,"wt")){
                    if(out==null)throw new IOException("Destinazione non scrivibile.");
                    try(Writer writer=new OutputStreamWriter(out,java.nio.charset.StandardCharsets.UTF_8)){ImageReport.write(writer,ImageFiles.name(activity,source),info);}
                }
            }catch(IOException|RuntimeException e){result="Esportazione non completata: "+message(e);}
            String notice=result;activity.runOnUiThread(()->{if(destroyed)return;state.exporting=false;state.notice=notice;render();toast(notice);});
        });
    }
    void exportLog(Uri output){
        if(busy()||destroyed)return;state.exporting=true;render();exportTask=worker.submit(()->{String notice="Log immagini esportato.";try(InputStream in=new FileInputStream(new File(activity.getFilesDir(),ImageConversionService.LOG));OutputStream out=activity.getContentResolver().openOutputStream(output,"wt")){if(out==null)throw new IOException("Destinazione non scrivibile");byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();out.write(buffer,0,n);}}catch(IOException|RuntimeException e){notice="Esportazione log non completata: "+message(e);}String result=notice;activity.runOnUiThread(()->{if(destroyed)return;state.exporting=false;state.notice=result;render();toast(result);});});
    }
    void render(){
        state.clamp();int count=state.inputs.size();boolean any=count>0,busy=busy();
        screen.select.setEnabled(!busy);screen.clear.setEnabled(any&&!busy);screen.remove.setEnabled(any&&!busy);
        screen.previous.setEnabled(state.selected>0&&!busy);screen.next.setEnabled(state.selected+1<count&&!busy);
        screen.previous.setVisibility(any?android.view.View.VISIBLE:android.view.View.GONE);screen.next.setVisibility(any?android.view.View.VISIBLE:android.view.View.GONE);screen.remove.setVisibility(any?android.view.View.VISIBLE:android.view.View.GONE);
        screen.position.setText(any?"Foto "+(state.selected+1)+" di "+count:"Nessuna foto selezionata");screen.name.setText(state.name);
        screen.export.setEnabled(state.info!=null&&!state.reading&&!busy);
        screen.metadata.setEnabled(state.info!=null);screen.chooseFolder.setEnabled(!busy);screen.quality.enabled(!busy);screen.smaller.setEnabled(!busy);screen.screenOn.setEnabled(!busy);screen.exportLog.setEnabled(!busy&&new File(activity.getFilesDir(),ImageConversionService.LOG).isFile());
        String folder=state.options.folder;try{if(folder!=null)folder=android.provider.DocumentsContract.getTreeDocumentId(Uri.parse(folder));}catch(IllegalArgumentException ignored){}screen.folder.setText(folder==null?"Nessuna cartella scelta":folder);
        ImageRunPresenter.render(this,ImageConversionService.snapshot());
        JpegInventory.Info info=state.info;
        screen.summary.setText(info==null?"":String.format(Locale.ITALY,"%d × %d · %.2f MB · JPEG %d bit · %d componenti",info.width,info.height,info.bytes/1000000.0,info.precision,info.components));
        screen.status.setText(state.reading?"Analisi della foto…":!state.notice.isEmpty()?state.notice:info!=null?"JPEG analizzato. La compatibilità completa verrà controllata prima della codifica.":"Scegli le foto. Gli originali restano invariati.");
        if(info==null){screen.details.setText(state.reading?"Lettura metadati…":state.notice.isEmpty()?"Seleziona una foto per leggere i metadati.":"Analisi non disponibile: "+state.notice);return;}
        LinkedHashMap<String,Integer> counts=new LinkedHashMap<>();for(JpegInventory.Block b:info.blocks)counts.put(b.kind,counts.getOrDefault(b.kind,0)+1);
        StringBuilder detail=new StringBuilder();for(Map.Entry<String,Integer> e:counts.entrySet())detail.append(e.getKey()).append(": ").append(e.getValue()).append(" blocchi\n");
        if(counts.isEmpty())detail.append("Nessun blocco APP o commento presente.\n");
        detail.append("\nMetadati inventariati: ").append(info.metadataBytes()).append(" byte");
        if(info.count("EXIF")>0)detail.append("\nEXIF presente: i dati originali sono inclusi nel report. In conversione l’orientamento viene applicato al contenitore; EXIF viene conservato integralmente.");
        if(info.count("ICC")>0)detail.append(info.iccComplete?"\nSequenza dei blocchi ICC completa (profilo non validato).":"\nAttenzione: sequenza ICC incompleta o duplicata.");
        if(info.trailingBytes>0)detail.append("\nAttenzione: ").append(info.trailingBytes).append(" byte dopo la fine JPEG. Potrebbero contenere altre immagini o dati Motion Photo: tipo da verificare. Il report contiene conteggio e hash, non questi byte.");
        detail.append("\n\nSHA-256 originale\n").append(info.sha256);screen.details.setText(detail);
    }
    void toast(String value){Toast.makeText(activity,value,Toast.LENGTH_LONG).show();}
    void destroy(){pause();destroyed=true;++generation;if(analysis!=null)analysis.cancel(true);if(exportTask!=null)exportTask.cancel(true);worker.shutdownNow();report.close();actions.close();screen.sections.closed=true;}
    private static String message(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
}
