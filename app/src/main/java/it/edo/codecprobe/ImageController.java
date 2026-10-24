package it.edo.codecprobe;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Coordinates the image workspace; slow provider/metadata reads never run on the UI thread. */
final class ImageController {
    final ImageActivity activity;final ImageState state;final ImageScreen screen;final ImageSelection selection;final ImageActions actions;final ImageLogExport logExport;final ImageWorkReport report;final MediaDestinationsController destinations;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ExecutorService namesWorker=Executors.newSingleThreadExecutor();
    private Future<?> analysis,exportTask,namesRead,folderTask;private int namesGeneration;private int generation;private boolean destroyed,visible,applying;
    private boolean renderedBusy;
    final LinkedHashSet<String> selectedPhotos=new LinkedHashSet<>();
    private android.app.AlertDialog selectionConfirmation;
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable tick=new Runnable(){public void run(){if(!visible||destroyed)return;if(screen.dashboard.cpu.isShown())screen.dashboard.monitor.sample(android.os.SystemClock.elapsedRealtime());refresh();main.postDelayed(this,500);}};
    ImageController(ImageActivity activity,Bundle saved){
        this.activity=activity;state=ImageDraftStore.load(activity);screen=new ImageScreen(new AppUi(activity),saved);report=new ImageWorkReport(this);selection=new ImageSelection(this,saved);
        destinations=new MediaDestinationsController(new MediaDestinationsController.Host(){
            public AppUi ui(){return screen.ui;}public java.util.List<String> inputs(){return state.inputs;}public java.util.List<String> names(){return state.names;}public int selected(){return state.selected;}
            public java.util.List<Integer> selectedIndices(){return selectionIndices();}
            public DestinationDraft draft(){return state.destinations;}public String defaultFolder(){return state.options.folder;}public boolean busy(){return ImageController.this.busy();}
            public void persist(){ImageController.this.persist();}public void changed(){render();}public void toast(String text){ImageController.this.toast(text);}
        },screen.destinations,MediaKind.IMAGE,saved);actions=new ImageActions(this);logExport=new ImageLogExport(this,saved);showOptions();
        screen.importFolder.setOnClickListener(v->selection.sourceFolder());screen.retryFailures.setOnClickListener(v->loadFailures());
        screen.select.setOnClickListener(v->selection.pick());screen.clear.setOnClickListener(v->confirmRemoval(true));
        screen.remove.setOnClickListener(v->confirmRemoval(false));
        screen.selectedItem.longSelected=this::toggleSelection;
        if(saved!=null){ArrayList<String> selected=saved.getStringArrayList("selectedPhotos");if(selected!=null)selectedPhotos.addAll(selected);}
        screen.reload.setOnClickListener(v->reloadSelection());
        screen.export.setOnClickListener(v->selection.export());screen.exportLog.setOnClickListener(v->selection.log());screen.exportErrors.setOnClickListener(v->selection.errors());screen.clearLog.setOnClickListener(v->actions.clearLog());
        screen.checkMetadata.setOnClickListener(v->actions.start(true));
        screen.convert.setOnClickListener(v->actions.start());screen.cancel.setOnClickListener(v->actions.cancel());screen.pause.setOnClickListener(v->actions.pause());screen.resumeSession.setOnClickListener(v->actions.resumeSession());screen.discardSession.setOnClickListener(v->actions.discardSession());
        screen.quality.changed=this::optionsEdited;screen.smaller.setOnCheckedChangeListener((b,checked)->optionsEdited());screen.screenOn.setOnCheckedChangeListener((b,checked)->screenOnEdited(checked));
        screen.settings.setOnClickListener(v->{android.content.Intent intent=new android.content.Intent(activity,ImageSettingsActivity.class);state.destinations.sync(state.inputs,state.options.folder);String currentFolder=state.inputs.isEmpty()?state.options.folder:state.destinations.plan.effective(state.selected).origin?null:state.destinations.plan.effective(state.selected).folder;state.options.withFolder(currentFolder).put(intent);activity.startActivity(intent);});open();
    }
    boolean busy(){return state.importing||state.exporting||logExport!=null&&logExport.busy()||state.starting||ImageConversionService.snapshot().running||destinations!=null&&destinations.choosing();}
    boolean visible(){return visible;}
    void select(int index){
        if(busy()||index<0||index>=state.inputs.size())return;
        if(!selectedPhotos.isEmpty()){toggleSelection(index);return;}
        if(index==state.selected){screen.selectedItem.cancelTransition();return;}
        String uri=state.inputs.get(index);
        screen.selectedItem.transition(()->{
            if(busy()||index>=state.inputs.size()||!uri.equals(state.inputs.get(index)))return;
            state.selected=index;open();screen.selectedItem.update(index,true,true);
        });
    }
    void stopPreviewReads(){
        ++generation;++namesGeneration;
        if(analysis!=null)analysis.cancel(true);if(namesRead!=null)namesRead.cancel(true);
        state.reading=false;
    }
    void importFolder(Uri tree){
        if(busy())return;String folder=state.options.folder==null||state.options.folder.isEmpty()?tree.toString():state.options.folder;
        importQueue(()->ImageFolderReader.read(activity,tree,folder),"JPEG importati dalla cartella (senza sottocartelle)");
    }
    void loadFailures(){
        if(busy())return;
        importQueue(()->{try(ImageProgressJournal journal=new ImageProgressJournal(activity)){return journal.failures();}},"Foto fallite dal registro · impostazioni di compressione correnti");
    }
    interface QueueImport {ImageQueueStore.Queue read()throws Exception;}
    private void importQueue(QueueImport source,String label){
        stopPreviewReads();state.importing=true;state.notice="Lettura dell’elenco foto…";render();
        folderTask=worker.submit(()->{
            ImageQueueStore.Queue queue=null;String failure=null;
            try{queue=source.read();}catch(Exception error){failure=message(error);}
            ImageQueueStore.Queue result=queue;String problem=failure;
            activity.runOnUiThread(()->{
                if(destroyed)return;state.importing=false;
                if(problem!=null||result==null||result.inputs.isEmpty()){state.notice=problem==null?"Nessun JPEG o fallimento da importare.":"Importazione non riuscita: "+problem;render();return;}
                screen.selectedItem.cancelTransition();ImageMetadataCache.memory.clear();
                LinkedHashMap<String,String> combined=new LinkedHashMap<>();
                for(int i=0;i<state.inputs.size();i++)combined.put(state.inputs.get(i),state.names.get(i));
                for(int i=0;i<result.inputs.size();i++)combined.putIfAbsent(result.inputs.get(i),result.names.get(i));
                if(combined.size()>ImageQueueStore.BULK_LIMIT){state.notice="La coda può contenere al massimo "+ImageQueueStore.BULK_LIMIT+" foto.";render();return;}
                HashSet<String> existing=new HashSet<>(state.inputs);
                state.inputs.clear();state.inputs.addAll(combined.keySet());state.names.clear();state.names.addAll(combined.values());state.clamp();state.bulk=true;state.selectionLabel=state.inputs.size()+" · "+label;
                state.destinations.sync(state.inputs,state.options.folder);
                HashMap<String,Integer> indices=new HashMap<>();for(int i=0;i<state.inputs.size();i++)indices.put(state.inputs.get(i),i);
                for(int i=0;i<result.inputs.size();i++)if(!existing.contains(result.inputs.get(i))&&!result.folders.get(i).isEmpty())state.destinations.plan.setFolder(indices.get(result.inputs.get(i)),result.folders.get(i));
                open();
            });
        });
    }
    void acceptSelection(ArrayList<String> combined,ArrayList<String> added){
        stopPreviewReads();screen.selectedItem.cancelTransition();ImageMetadataCache.memory.clear();
        state.names.clear();state.inputs.clear();state.inputs.addAll(combined);state.selected=0;state.clamp();state.destinations.sync(state.inputs,state.options.folder);
        state.importing=true;state.reading=true;state.info=null;state.name="";state.notice="Acquisizione dell’accesso alle foto…";persist();render();
        folderTask=worker.submit(()->{
            boolean temporary=false;
            for(String value:added){
                if(Thread.currentThread().isInterrupted())return;
                try{activity.getContentResolver().takePersistableUriPermission(Uri.parse(value),android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);}
                catch(SecurityException error){temporary=true;}
            }
            boolean temporaryAccess=temporary;
            activity.runOnUiThread(()->{
                if(destroyed)return;state.importing=false;open();readNames();
                if(temporaryAccess)toast("Alcune foto concedono solo accesso temporaneo: potrebbe servire riselezionarle dopo la chiusura.");
            });
        });
    }
    void readNames(){
        int token=++namesGeneration;if(namesRead!=null)namesRead.cancel(true);
        ArrayList<String> inputs=new ArrayList<>(state.inputs);
        namesRead=namesWorker.submit(()->{
            ArrayList<String> names=new ArrayList<>();
            for(String value:inputs){if(Thread.currentThread().isInterrupted())return;names.add(ImageFiles.name(activity,Uri.parse(value)));}
            activity.runOnUiThread(()->{if(destroyed||token!=namesGeneration||!inputs.equals(state.inputs))return;state.names.clear();state.names.addAll(names);persist();render();});
        });
    }
    void screenOnEdited(boolean checked){
        if(applying)return;ImageOptions old=state.options;state.options=new ImageOptions(old.quality,old.preset,old.tune,old.folder,old.onlySmaller,checked,old.revision);persist();render();
    }
    void optionsEdited(){if(applying||busy())return;ImageOptions old=state.options;state.options=new ImageOptions(screen.quality.value(),screen.quality.preset(),screen.quality.tune(),old.folder,screen.smaller.isChecked(),screen.screenOn.isChecked(),old.revision);persist();render();}
    void setFolder(String folder){state.destinations.defaults(state.inputs,folder);ImageOptions o=state.options;state.options=new ImageOptions(o.quality,o.preset,o.tune,folder,o.onlySmaller,o.keepScreenOn,o.revision);persist();render();}
    void showOptions(){applying=true;try{screen.quality.set(state.options);screen.smaller.setChecked(state.options.onlySmaller);screen.screenOn.setChecked(state.options.keepScreenOn);}finally{applying=false;}}
    void resume(){visible=true;screen.selectedItem.thumbnails.active(ImageScreen.FILES.equals(screen.sections.current()));screen.dashboard.monitor.reset();destinations.refresh();main.removeCallbacks(tick);tick.run();}
    void pause(){visible=false;screen.selectedItem.thumbnails.active(false);main.removeCallbacks(tick);activity.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
    void refresh(){ImageRunState run=ImageConversionService.snapshot();if(state.starting&&state.startingAt>0&&(run.startedAt!=state.previousRun||android.os.SystemClock.elapsedRealtime()-state.startingAt>5000)){state.starting=false;if(run.startedAt==state.previousRun)state.notice=run.message.isEmpty()?"La conversione non è partita: riprova.":run.message;}
        ImageOptions defaults=ImageOptions.load(activity);if(!busy()&&state.options.revision!=defaults.revision){state.options=defaults;showOptions();persist();render();return;}
        // Progress ticks must not rebuild the selection, metadata or destination plan.
        if(renderedBusy!=busy())render();else ImageRunPresenter.renderLive(this,run);
    }
    void persist(){ImageDraftStore.save(activity,state);}
    void save(Bundle out){out.putStringArrayList("selectedPhotos",new ArrayList<>(selectedPhotos));persist();screen.sections.save(out);selection.save(out);logExport.save(out);destinations.save(out);}
    void open(){
        int current=++generation;if(analysis!=null)analysis.cancel(true);
        state.clamp();state.info=null;state.notice="";state.name="";state.reading=!state.inputs.isEmpty();persist();render();if(!state.reading)return;
        String value=state.inputs.get(state.selected);
        ImageMetadataCache.Entry cached=ImageMetadataCache.memory.get(value);
        if(cached!=null){showInput(cached);return;}
        final long cacheEpoch=ImageMetadataCache.memory.epoch();
        analysis=worker.submit(()->{
            String name=ImageFiles.name(activity,Uri.parse(value));JpegInventory.Info info=null;String error="";
            try(InputStream in=activity.getContentResolver().openInputStream(Uri.parse(value))){if(in==null)throw new IOException("Impossibile leggere la foto.");info=JpegInventory.read(in);}
            catch(IOException|RuntimeException e){error=message(e);}
            ImageMetadataCache.Entry result=new ImageMetadataCache.Entry(name,info,error);
            if(Thread.currentThread().isInterrupted())return;
            ImageMetadataCache.memory.put(value,result,result.weight(),cacheEpoch);
            activity.runOnUiThread(()->{if(destroyed||current!=generation)return;showInput(result);});
        });
    }
    private void showInput(ImageMetadataCache.Entry entry){
        state.name=entry.name;state.info=entry.info;state.notice=entry.error;state.reading=false;
        state.names.set(state.selected,entry.name);persist();render();
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
    void render(){
        state.clamp();if(!selectedPhotos.isEmpty())selectedPhotos.retainAll(new HashSet<>(state.inputs));int count=state.inputs.size();boolean any=count>0,busy=busy();renderedBusy=busy;
        screen.importFolder.setEnabled(!busy);screen.retryFailures.setEnabled(!busy&&activity.getDatabasePath(ImageProgressJournal.DATABASE).isFile());
        screen.selectionSummary.setText(state.selectionLabel);screen.selectionSummary.setVisibility(state.selectionLabel.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);
        screen.select.setEnabled(!busy);screen.clear.setEnabled(any&&!busy);screen.remove.setEnabled(any&&!busy);
        screen.selectedItem.render(state.names,Collections.emptyList(),state.selected,!busy,this::select,ImageQueueStore.VISIBLE);
        screen.selectedItem.selectedIndices(new HashSet<>(selectionIndicesForHighlight()));
        screen.selectedItem.thumbnails.bind(state.inputs);
        if(!any)screen.selectedItem.setExpanded(false);
        screen.remove.setVisibility(any?android.view.View.VISIBLE:android.view.View.GONE);
        screen.reload.setVisibility(any?android.view.View.VISIBLE:android.view.View.GONE);screen.reload.setEnabled(any&&!busy&&!state.reading);
        screen.metadata.setVisibility(any?android.view.View.VISIBLE:android.view.View.GONE);screen.name.setText(state.name.isEmpty()?(any?state.names.get(state.selected):"Nessuna foto selezionata"):state.name);
        screen.export.setEnabled(state.info!=null&&!state.reading&&!busy);
        screen.metadata.setEnabled(state.info!=null);screen.quality.enabled(!busy);screen.smaller.setEnabled(!busy);screen.exportLog.setEnabled(logExport.available(ImageConversionService.LOG));screen.clearLog.setEnabled(!busy&&new File(activity.getFilesDir(),ImageConversionService.LOG).isFile());
        screen.exportErrors.setEnabled(logExport.available(ImageFailureStore.FILE));
        destinations.render();
        ImageRunPresenter.render(this,ImageConversionService.snapshot());
        if(selectedPhotos.size()>1){
            ArrayList<String> selectedNames=new ArrayList<>();for(int index:selectionIndices())selectedNames.add(state.names.get(index));selectedNames.sort(String.CASE_INSENSITIVE_ORDER);
            screen.name.setText(selectedPhotos.size()+" foto selezionate");screen.summary.setText("Rimuovi, rileggi e destinazione agiscono sulla selezione.");screen.metadata.setEnabled(true);
            screen.selectedItem.showMetadata(Collections.singletonMap("FILE SELEZIONATI",String.join("\n",selectedNames)),Collections.emptyMap());
            screen.selectedItem.number.setText(selectedPhotos.size()+" selezionate · "+count+" in coda");return;
        }
        JpegInventory.Info info=state.info;
        screen.summary.setText(info==null?"":String.format(Locale.ITALY,"%d × %d · %.2f MB · JPEG %d bit · %d componenti",info.width,info.height,info.bytes/1000000.0,info.precision,info.components));
        screen.status.setText(state.importing?state.notice:state.reading?"Analisi della foto…":!state.notice.isEmpty()?state.notice:info!=null?"JPEG analizzato. La compatibilità completa verrà controllata prima della codifica.":"Scegli le foto. Gli originali restano invariati.");
        if(info==null){screen.selectedItem.showMetadata(Collections.singletonMap("LETTURA",state.reading?"Lettura metadati…":state.notice.isEmpty()?"Seleziona una foto per leggere i metadati.":state.notice),Collections.emptyMap());screen.details.setText(state.reading?"Lettura metadati…":state.notice.isEmpty()?"Seleziona una foto per leggere i metadati.":"Analisi non disponibile: "+state.notice);return;}
        LinkedHashMap<String,Integer> counts=new LinkedHashMap<>();for(JpegInventory.Block b:info.blocks)counts.put(b.kind,counts.getOrDefault(b.kind,0)+1);
        StringBuilder detail=new StringBuilder();for(Map.Entry<String,Integer> e:counts.entrySet())detail.append(e.getKey()).append(": ").append(e.getValue()).append(" blocchi\n");
        if(counts.isEmpty())detail.append("Nessun blocco APP o commento presente.\n");
        detail.append("\nMetadati inventariati: ").append(info.metadataBytes()).append(" byte");
        if(info.count("EXIF")>0)detail.append("\nEXIF presente: i dati originali sono inclusi nel report. In conversione l’orientamento viene applicato al contenitore; EXIF viene conservato integralmente.");
        if(info.count("ICC")>0)detail.append(info.iccComplete?"\nSequenza dei blocchi ICC completa (profilo non validato).":"\nAttenzione: sequenza ICC incompleta o duplicata.");
        if(info.trailingBytes>0)detail.append("\nAttenzione: ").append(info.trailingBytes).append(" byte dopo la fine JPEG. Potrebbero contenere altre immagini o dati Motion Photo: tipo da verificare. Il report contiene conteggio e hash, non questi byte.");
        detail.append("\n\nSHA-256 originale\n").append(info.sha256);screen.details.setText(detail);screen.selectedItem.showMetadata(Collections.singletonMap("IMMAGINE",screen.summary.getText().toString()),Collections.singletonMap("METADATI ORIGINALI",detail.toString()));
    }
    ArrayList<Integer> selectionIndices(){
        ArrayList<Integer> result=new ArrayList<>();
        if(selectedPhotos.isEmpty()){if(!state.inputs.isEmpty())result.add(state.selected);return result;}
        for(int n=0;n<state.inputs.size();n++)if(selectedPhotos.contains(state.inputs.get(n)))result.add(n);return result;
    }
    private ArrayList<Integer> selectionIndicesForHighlight(){return selectedPhotos.isEmpty()?new ArrayList<>():selectionIndices();}
    private ArrayList<String> selectionSources(){ArrayList<String> result=new ArrayList<>();for(int n:selectionIndices())result.add(state.inputs.get(n));return result;}
    void toggleSelection(int index){
        if(busy()||index<0||index>=state.inputs.size())return;
        screen.selectedItem.cancelTransition();stopPreviewReads();String input=state.inputs.get(index);
        if(!selectedPhotos.remove(input)){selectedPhotos.add(input);state.selected=index;}
        if(selectedPhotos.size()==1)state.selected=state.inputs.indexOf(selectedPhotos.iterator().next());
        if(selectedPhotos.size()<=1)open();else render();
    }
    private void reloadSelection(){
        if(busy()||state.inputs.isEmpty())return;
        ArrayList<String> targets=selectionSources();for(String input:targets)ImageMetadataCache.memory.remove(input);
        screen.selectedItem.thumbnails.invalidate(targets);
        if(targets.size()==1){open();return;}
        stopPreviewReads();state.importing=true;state.notice="Rilettura di "+targets.size()+" foto…";render();
        final long epoch=ImageMetadataCache.memory.epoch();
        analysis=worker.submit(()->{
            int failures=0;
            for(String input:targets){
                if(Thread.currentThread().isInterrupted())return;
                String name=ImageFiles.name(activity,Uri.parse(input));JpegInventory.Info info=null;String error="";
                try(InputStream in=activity.getContentResolver().openInputStream(Uri.parse(input))){if(in==null)throw new IOException("Foto non accessibile");info=JpegInventory.read(in);}
                catch(IOException|RuntimeException e){error=message(e);failures++;}
                ImageMetadataCache.Entry entry=new ImageMetadataCache.Entry(name,info,error);ImageMetadataCache.memory.put(input,entry,entry.weight(),epoch);
            }
            int failed=failures;activity.runOnUiThread(()->{if(destroyed)return;state.importing=false;open();if(failed>0){state.notice=failed+" foto non lette. Apri i dettagli dei singoli file per l’errore.";render();}});
        });
    }
    private void confirmRemoval(boolean all){
        if(busy()||state.inputs.isEmpty())return;
        ArrayList<String> targets=all?new ArrayList<>(state.inputs):selectionSources();
        selectionConfirmation=new android.app.AlertDialog.Builder(activity)
            .setTitle(all?"Svuotare la selezione?":"Rimuovere "+targets.size()+" foto dalla selezione?")
            .setMessage("Le foto originali restano sul dispositivo.")
            .setNegativeButton("Annulla",null).setPositiveButton(all?"Svuota":"Rimuovi",(d,w)->{
                if(busy())return;screen.selectedItem.cancelTransition();stopPreviewReads();
                HashSet<String> removed=new HashSet<>(targets);
                for(int n=state.inputs.size()-1;n>=0;n--)if(removed.contains(state.inputs.get(n))){ImageMetadataCache.memory.remove(state.inputs.remove(n));state.names.remove(n);}
                selectedPhotos.removeAll(removed);screen.selectedItem.thumbnails.invalidate(targets);
                if(all){ImageMetadataCache.memory.clear();state.bulk=false;state.selectionLabel="";ImageConversionService.clear();}
                state.clamp();open();
            }).create();selectionConfirmation.show();
    }
    void toast(String value){Toast.makeText(activity,value,Toast.LENGTH_LONG).show();}
    void destroy(){pause();if(selectionConfirmation!=null)selectionConfirmation.dismiss();destroyed=true;++generation;if(analysis!=null)analysis.cancel(true);if(exportTask!=null)exportTask.cancel(true);if(namesRead!=null)namesRead.cancel(true);if(folderTask!=null)folderTask.cancel(true);screen.selectedItem.close();worker.shutdownNow();namesWorker.shutdownNow();screen.dashboard.monitor.close();destinations.close();report.close();actions.close();logExport.close();screen.sections.closed=true;}
    private static String message(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
}
