package it.edo.codecprobe;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Coordinates the image workspace; slow provider/metadata reads never run on the UI thread. */
final class ImageController {
    private static final String[] PRESETS={"fast","medium","slow"},TUNES={"standard","ssim","psnr"};
    final ImageActivity activity;final ImageState state;final ImageScreen screen;final ImageSelection selection;final ImageActions actions;final ImageLogExport logExport;final ImageWorkReport report;final MediaDestinationsController destinations;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ExecutorService namesWorker=Executors.newSingleThreadExecutor();
    private Future<?> analysis,exportTask,namesRead,folderTask;private int namesGeneration;private int generation;private boolean destroyed,visible,applying;
    private boolean renderedBusy;
    PhotoComposeHost compose;
    boolean detailsOpen;
    final LinkedHashSet<String> selectedPhotos=new LinkedHashSet<>();
    private final LinkedHashSet<String> pendingNames=new LinkedHashSet<>();
    private android.app.AlertDialog selectionConfirmation;
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable tick=new Runnable(){public void run(){if(!visible||destroyed)return;if(screen.dashboard.cpu.isShown())screen.dashboard.monitor.sample(android.os.SystemClock.elapsedRealtime());refresh();main.postDelayed(this,500);}};
    ImageController(ImageActivity activity,Bundle saved){
        this.activity=activity;state=ImageDraftStore.load(activity);screen=new ImageScreen(new AppUi(activity,true),saved);report=new ImageWorkReport(this);selection=new ImageSelection(this,saved);
        destinations=new MediaDestinationsController(new MediaDestinationsController.Host(){
            public AppUi ui(){return screen.ui;}public java.util.List<String> inputs(){return state.inputs;}public java.util.List<String> names(){return state.names;}public int selected(){return state.selected;}
            public java.util.List<Integer> selectedIndices(){return selectionIndices();}
            public DestinationDraft draft(){return state.destinations;}public String defaultFolder(){return state.options.folder;}public boolean busy(){return ImageController.this.busy();}
            public void persist(){ImageController.this.persist();}public void changed(){render();}public void toast(String text){ImageController.this.toast(text);}
        },saved);actions=new ImageActions(this);logExport=new ImageLogExport(this,saved);showOptions();
        if(saved!=null){ArrayList<String> selected=saved.getStringArrayList("selectedPhotos");if(selected!=null)selectedPhotos.addAll(selected);detailsOpen=saved.getBoolean("photoDetailsOpen");}
        screen.retryFailures.setOnClickListener(v->loadFailures());
        screen.export.setOnClickListener(v->selection.export());screen.exportLog.setOnClickListener(v->selection.log());screen.exportErrors.setOnClickListener(v->selection.errors());screen.clearLog.setOnClickListener(v->actions.clearLog());
        screen.checkMetadata.setOnClickListener(v->actions.start(true));
        screen.convert.setOnClickListener(v->actions.start());screen.cancel.setOnClickListener(v->actions.cancel());screen.pause.setOnClickListener(v->actions.pause());screen.resumeSession.setOnClickListener(v->actions.resumeSession());screen.discardSession.setOnClickListener(v->actions.discardSession());
        screen.screenOn.setOnCheckedChangeListener((button,checked)->screenOnEdited(checked));
        screen.sections.changed=this::render;
        open();
    }
    void settings(){
        android.content.Intent intent=new android.content.Intent(activity,ImageSettingsActivity.class);
        state.destinations.sync(state.inputs,state.options.folder);
        String folder=state.inputs.isEmpty()?state.options.folder:state.destinations.plan.effective(state.selected).origin?null:state.destinations.plan.effective(state.selected).folder;
        state.options.withFolder(folder).put(intent);activity.startActivity(intent);
    }
    void next(){String current=screen.sections.current();if(ImageScreen.FILES.equals(current))screen.sections.focusCard(ImageScreen.COMPRESSION);else if(ImageScreen.COMPRESSION.equals(current))screen.sections.focusCard(ImageScreen.HEIC);}
    void back(){
        String current=screen.sections.current();
        if(current==null){activity.finish();return;}
        if(ImageScreen.COMPRESSION.equals(current))screen.sections.focusCard(ImageScreen.FILES);
        else if(ImageScreen.HEIC.equals(current))screen.sections.focusCard(ImageScreen.COMPRESSION);
        else if(ImageScreen.DETAILS.equals(current))screen.sections.focusCard(ImageScreen.HEIC);
        else screen.sections.home();
    }
    void navigate(Class<? extends android.app.Activity> target){
        activity.startActivity(new android.content.Intent(activity,target).addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP));
        activity.finish();AppNavigation.fade(activity);
    }
    void toggleDetails(){if(state.inputs.isEmpty())return;detailsOpen=!detailsOpen;render();}
    void quality(int value){if(busy())return;ImageOptions old=state.options;setOptions(new ImageOptions(Math.max(40,Math.min(95,value)),old.preset,old.tune,old.folder,old.onlySmaller,old.keepScreenOn,old.revision,old.nameAddition,old.namePrefix));}
    void preset(int index){if(busy()||index<0||index>=PRESETS.length)return;ImageOptions old=state.options;setOptions(new ImageOptions(old.quality,PRESETS[index],old.tune,old.folder,old.onlySmaller,old.keepScreenOn,old.revision,old.nameAddition,old.namePrefix));}
    void tune(int index){if(busy()||index<0||index>=TUNES.length)return;ImageOptions old=state.options;setOptions(new ImageOptions(old.quality,old.preset,TUNES[index],old.folder,old.onlySmaller,old.keepScreenOn,old.revision,old.nameAddition,old.namePrefix));}
    void smaller(boolean value){if(busy())return;ImageOptions old=state.options;setOptions(new ImageOptions(old.quality,old.preset,old.tune,old.folder,value,old.keepScreenOn,old.revision,old.nameAddition,old.namePrefix));}
    void nameAddition(String value){if(busy())return;setOptions(state.options.withNameAddition(value));ImageOptions.saveNaming(activity,state.options.nameAddition,state.options.namePrefix);}
    void namePrefix(boolean value){if(busy())return;setOptions(state.options.withNamePrefix(value));ImageOptions.saveNaming(activity,state.options.nameAddition,state.options.namePrefix);}
    private void setOptions(ImageOptions value){state.options=value;persist();render();}
    boolean busy(){return state.importing||state.exporting||logExport!=null&&logExport.busy()||state.starting||ImageConversionService.snapshot().running||destinations!=null&&destinations.choosing();}
    boolean visible(){return visible;}
    void select(int index){
        if(busy()||index<0||index>=state.inputs.size())return;
        if(!selectedPhotos.isEmpty()){toggleSelection(index);return;}
        if(index==state.selected)return;
        state.selected=index;open();
    }
    private void stopSelectedRead(){++generation;if(analysis!=null)analysis.cancel(true);state.reading=false;}
    void stopPreviewReads(){
        stopSelectedRead();++namesGeneration;if(namesRead!=null)namesRead.cancel(true);
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

                LinkedHashMap<String,String> combined=new LinkedHashMap<>();
                for(int i=0;i<state.inputs.size();i++)combined.put(state.inputs.get(i),state.names.get(i));
                for(int i=0;i<result.inputs.size();i++)combined.putIfAbsent(result.inputs.get(i),result.names.get(i));
                if(combined.size()>ImageQueueStore.BULK_LIMIT){state.notice="La coda può contenere al massimo "+ImageQueueStore.BULK_LIMIT+" foto.";render();return;}
                HashSet<String> existing=new HashSet<>(state.inputs);
                state.inputs.clear();state.inputs.addAll(combined.keySet());state.names.clear();state.names.addAll(combined.values());state.clamp();state.bulk=true;state.selectionLabel=state.inputs.size()+" · "+label;
                state.destinations.sync(state.inputs,state.options.folder);
                HashMap<String,Integer> indices=new HashMap<>();for(int i=0;i<state.inputs.size();i++)indices.put(state.inputs.get(i),i);
                for(int i=0;i<result.inputs.size();i++)if(label.startsWith("Foto fallite")&&!existing.contains(result.inputs.get(i))&&!result.folders.get(i).isEmpty())state.destinations.plan.setFolder(indices.get(result.inputs.get(i)),result.folders.get(i));
                open();
            });
        });
    }
    void acceptSelection(ArrayList<String> combined,ArrayList<String> added){
        stopPreviewReads();
        HashMap<String,String> names=new HashMap<>();for(int n=0;n<state.inputs.size();n++)names.put(state.inputs.get(n),state.names.get(n));
        state.names.clear();state.inputs.clear();state.inputs.addAll(combined);for(String input:combined)state.names.add(names.getOrDefault(input,"Foto "+(state.names.size()+1)));state.clamp();state.destinations.sync(state.inputs,state.options.folder);
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
                if(destroyed)return;state.importing=false;open();readNames(added);
                if(temporaryAccess)toast("Alcune foto concedono solo accesso temporaneo: potrebbe servire riselezionarle dopo la chiusura.");
            });
        });
    }
    void readNames(){readNames(new ArrayList<>(state.inputs));}
    private void readNames(List<String> requested){
        int token=++namesGeneration;if(namesRead!=null)namesRead.cancel(true);
        pendingNames.addAll(requested);pendingNames.retainAll(new HashSet<>(state.inputs));
        ArrayList<String> inputs=new ArrayList<>(state.inputs),targets=new ArrayList<>(pendingNames);
        namesRead=namesWorker.submit(()->{
            HashMap<String,String> names=new HashMap<>();
            for(String value:targets){if(Thread.currentThread().isInterrupted())return;names.put(value,ImageFiles.name(activity,Uri.parse(value)));}
            activity.runOnUiThread(()->{if(destroyed||token!=namesGeneration||!inputs.equals(state.inputs))return;for(int n=0;n<inputs.size();n++){String name=names.get(inputs.get(n));if(name!=null)state.names.set(n,name);}pendingNames.removeAll(names.keySet());persist();render();});
        });
    }
    void screenOnEdited(boolean checked){
        if(applying)return;ImageOptions old=state.options;state.options=new ImageOptions(old.quality,old.preset,old.tune,old.folder,old.onlySmaller,checked,old.revision,old.nameAddition,old.namePrefix);persist();render();
    }
    void setFolder(String folder){state.destinations.sync(state.inputs,state.options.folder);state.destinations.plan.enableExceptions();state.destinations.plan.all.origin=folder==null||folder.isEmpty();state.destinations.plan.all.folder=folder==null?"":folder;ImageOptions o=state.options;state.options=new ImageOptions(o.quality,o.preset,o.tune,folder,o.onlySmaller,o.keepScreenOn,o.revision,o.nameAddition,o.namePrefix);persist();render();}
    void showOptions(){applying=true;try{screen.screenOn.setChecked(state.options.keepScreenOn);}finally{applying=false;}}
    void resume(){visible=true;if(compose!=null)compose.onVisible(true);screen.dashboard.monitor.reset();destinations.refresh();main.removeCallbacks(tick);tick.run();}
    void pause(){visible=false;if(compose!=null)compose.onVisible(false);main.removeCallbacks(tick);activity.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
    void refresh(){ImageRunState run=ImageConversionService.snapshot();if(state.starting&&state.startingAt>0&&(run.startedAt!=state.previousRun||android.os.SystemClock.elapsedRealtime()-state.startingAt>5000)){state.starting=false;if(run.startedAt==state.previousRun)state.notice=run.message.isEmpty()?"La conversione non è partita: riprova.":run.message;}
        ImageOptions defaults=ImageOptions.load(activity);if(!busy()&&state.options.revision!=defaults.revision){state.options=defaults;showOptions();persist();render();return;}
        // Progress ticks must not rebuild the selection, metadata or destination plan.
        if(renderedBusy!=busy())render();else ImageRunPresenter.renderLive(this,run);
    }
    void persist(){ImageDraftStore.save(activity,state);}
    void save(Bundle out){out.putBoolean("photoGeneralScope",destinations.photo.general);out.putBoolean("photoDetailsOpen",detailsOpen);out.putStringArrayList("selectedPhotos",new ArrayList<>(selectedPhotos));persist();screen.sections.save(out);selection.save(out);logExport.save(out);destinations.save(out);}
    void open(){
        int current=++generation;if(analysis!=null)analysis.cancel(true);
        state.clamp();
        String value=state.inputs.isEmpty()?null:state.inputs.get(state.selected);
        ImageMetadataCache.Entry cached=value==null?null:ImageMetadataCache.memory.get(value);
        if(cached!=null){showInput(cached);return;}
        state.info=null;state.notice="";state.name="";state.reading=value!=null;persist();render();if(!state.reading)return;
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
        state.clamp();if(!selectedPhotos.isEmpty())selectedPhotos.retainAll(new HashSet<>(state.inputs));
        boolean busy=busy();renderedBusy=busy;
        if(state.inputs.isEmpty())detailsOpen=false;
        screen.retryFailures.setEnabled(!busy&&activity.getDatabasePath(ImageProgressJournal.DATABASE).isFile());
        screen.export.setEnabled(state.info!=null&&!state.reading&&!busy);
        screen.exportLog.setEnabled(logExport.available(ImageConversionService.LOG));
        screen.clearLog.setEnabled(!busy&&new File(activity.getFilesDir(),ImageConversionService.LOG).isFile());
        screen.exportErrors.setEnabled(logExport.available(ImageFailureStore.FILE));
        destinations.render();
        ImageRunPresenter.render(this,ImageConversionService.snapshot());
        if(compose!=null)compose.refresh();
    }
    ArrayList<Integer> selectionIndices(){
        ArrayList<Integer> result=new ArrayList<>();
        if(selectedPhotos.isEmpty()){if(!state.inputs.isEmpty())result.add(state.selected);return result;}
        for(int n=0;n<state.inputs.size();n++)if(selectedPhotos.contains(state.inputs.get(n)))result.add(n);return result;
    }
    private ArrayList<String> selectionSources(){ArrayList<String> result=new ArrayList<>();for(int n:selectionIndices())result.add(state.inputs.get(n));return result;}
    void finishSelection(){
        if(busy()||selectedPhotos.isEmpty())return;
        selectedPhotos.clear();open();
    }
    void toggleSelection(int index){
        if(busy()||index<0||index>=state.inputs.size())return;
        stopSelectedRead();String input=state.inputs.get(index);
        if(!selectedPhotos.remove(input)){selectedPhotos.add(input);state.selected=index;}
        if(selectedPhotos.size()==1)state.selected=state.inputs.indexOf(selectedPhotos.iterator().next());
        if(selectedPhotos.size()<=1)open();else render();
    }
    void reloadSelection(){
        if(busy()||state.inputs.isEmpty())return;
        ArrayList<String> targets=selectionSources();for(String input:targets)ImageMetadataCache.memory.remove(input);

        if(targets.size()==1){open();return;}
        stopSelectedRead();state.importing=true;state.notice="Rilettura di "+targets.size()+" foto…";render();
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
    void confirmRemoval(boolean all){
        if(busy()||state.inputs.isEmpty())return;
        ArrayList<String> targets=all?new ArrayList<>(state.inputs):selectionSources();
        selectionConfirmation=new android.app.AlertDialog.Builder(activity)
            .setTitle(all?"Svuotare la selezione?":"Rimuovere "+targets.size()+" foto dalla selezione?")
            .setMessage("Le foto originali restano sul dispositivo.")
            .setNegativeButton("Annulla",null).setPositiveButton(all?"Svuota":"Rimuovi",(d,w)->{
                if(busy())return;stopPreviewReads();
                HashSet<String> removed=new HashSet<>(targets);
                for(int n=state.inputs.size()-1;n>=0;n--)if(removed.contains(state.inputs.get(n))){ImageMetadataCache.memory.remove(state.inputs.remove(n));state.names.remove(n);}
                selectedPhotos.removeAll(removed);
                if(all){ImageMetadataCache.memory.clear();state.bulk=false;state.selectionLabel="";ImageConversionService.clear();}
                state.clamp();open();
            }).create();selectionConfirmation.show();
    }
    void toast(String value){Toast.makeText(activity,value,Toast.LENGTH_LONG).show();}
    void destroy(){pause();if(compose!=null)compose.close();if(selectionConfirmation!=null)selectionConfirmation.dismiss();destroyed=true;++generation;if(analysis!=null)analysis.cancel(true);if(exportTask!=null)exportTask.cancel(true);if(namesRead!=null)namesRead.cancel(true);if(folderTask!=null)folderTask.cancel(true);worker.shutdownNow();namesWorker.shutdownNow();screen.dashboard.monitor.close();destinations.close();report.close();actions.close();logExport.close();screen.sections.closed=true;}
    private static String message(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
}
