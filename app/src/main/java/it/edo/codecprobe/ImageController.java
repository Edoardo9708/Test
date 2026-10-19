package it.edo.codecprobe;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Coordinates the image workspace; slow provider/metadata reads never run on the UI thread. */
final class ImageController {
    final ImageActivity activity;final ImageState state;final ImageScreen screen;final ImageSelection selection;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private Future<?> analysis,exportTask;private int generation;private boolean destroyed;
    ImageController(ImageActivity activity,Bundle saved){
        this.activity=activity;state=ImageDraftStore.load(activity);screen=new ImageScreen(new AppUi(activity),saved);selection=new ImageSelection(this,saved);
        screen.select.setOnClickListener(v->selection.pick());screen.clear.setOnClickListener(v->{if(state.exporting)return;state.inputs.clear();state.selected=0;open();});
        screen.previous.setOnClickListener(v->move(-1));screen.next.setOnClickListener(v->move(1));
        screen.remove.setOnClickListener(v->{if(state.exporting||state.inputs.isEmpty())return;state.inputs.remove(state.selected);state.clamp();open();});
        screen.export.setOnClickListener(v->selection.export());open();
    }
    void move(int delta){if(state.exporting||state.inputs.isEmpty())return;state.selected=Math.max(0,Math.min(state.inputs.size()-1,state.selected+delta));open();}
    void persist(){ImageDraftStore.save(activity,state);}
    void save(Bundle out){persist();screen.sections.save(out);selection.save(out);}
    void open(){
        int current=++generation;if(analysis!=null)analysis.cancel(true);
        state.clamp();state.info=null;state.notice="";state.name="";state.reading=!state.inputs.isEmpty();persist();render();if(!state.reading)return;
        String value=state.inputs.get(state.selected);
        analysis=worker.submit(()->{
            String name=name(Uri.parse(value));JpegInventory.Info info=null;String error="";
            try(InputStream in=activity.getContentResolver().openInputStream(Uri.parse(value))){if(in==null)throw new IOException("Impossibile leggere la foto.");info=JpegInventory.read(in);}
            catch(IOException|RuntimeException e){error=message(e);}
            JpegInventory.Info result=info;String failure=error;
            activity.runOnUiThread(()->{if(destroyed||current!=generation)return;state.name=name;state.info=result;state.notice=failure;state.reading=false;render();});
        });
    }
    private String name(Uri uri){
        try(Cursor row=activity.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(row!=null&&row.moveToFirst()&&!row.isNull(0))return row.getString(0);
        }catch(RuntimeException ignored){}
        return "Foto JPEG";
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
                    try(Writer writer=new OutputStreamWriter(out,java.nio.charset.StandardCharsets.UTF_8)){ImageReport.write(writer,name(source),info);}
                }
            }catch(IOException|RuntimeException e){result="Esportazione non completata: "+message(e);}
            String notice=result;activity.runOnUiThread(()->{if(destroyed)return;state.exporting=false;state.notice=notice;render();toast(notice);});
        });
    }
    void render(){
        state.clamp();int count=state.inputs.size();boolean any=count>0,busy=state.exporting;
        screen.select.setEnabled(!busy);screen.clear.setEnabled(any&&!busy);screen.remove.setEnabled(any&&!busy);
        screen.previous.setEnabled(state.selected>0&&!busy);screen.next.setEnabled(state.selected+1<count&&!busy);
        screen.previous.setVisibility(any?android.view.View.VISIBLE:android.view.View.GONE);screen.next.setVisibility(any?android.view.View.VISIBLE:android.view.View.GONE);screen.remove.setVisibility(any?android.view.View.VISIBLE:android.view.View.GONE);
        screen.position.setText(any?"Foto "+(state.selected+1)+" di "+count:"Nessuna foto selezionata");screen.name.setText(state.name);
        screen.export.setEnabled(state.info!=null&&!state.reading&&!busy);
        JpegInventory.Info info=state.info;
        screen.summary.setText(info==null?"":String.format(Locale.ITALY,"%d × %d · %.2f MB · JPEG %d bit · %d componenti",info.width,info.height,info.bytes/1000000.0,info.precision,info.components));
        screen.status.setText(state.reading?"Analisi della foto…":!state.notice.isEmpty()?state.notice:info!=null?"Inventario completato. Il controllo dei pixel e la conversione HEIC non sono ancora disponibili.":"Scegli le foto da analizzare. Gli originali restano invariati.");
        if(info==null){screen.details.setText(state.reading?"Lettura metadati…":state.notice.isEmpty()?"Seleziona una foto per leggere i metadati.":"Analisi non disponibile: "+state.notice);return;}
        LinkedHashMap<String,Integer> counts=new LinkedHashMap<>();for(JpegInventory.Block b:info.blocks)counts.put(b.kind,counts.getOrDefault(b.kind,0)+1);
        StringBuilder detail=new StringBuilder();for(Map.Entry<String,Integer> e:counts.entrySet())detail.append(e.getKey()).append(": ").append(e.getValue()).append(" blocchi\n");
        if(counts.isEmpty())detail.append("Nessun blocco APP o commento presente.\n");
        detail.append("\nMetadati inventariati: ").append(info.metadataBytes()).append(" byte");
        if(info.count("EXIF")>0)detail.append("\nEXIF presente: dati originali inclusi nel report. Data, GPS e orientamento non ancora interpretati.");
        if(info.count("ICC")>0)detail.append(info.iccComplete?"\nSequenza dei blocchi ICC completa (profilo non validato).":"\nAttenzione: sequenza ICC incompleta o duplicata.");
        if(info.trailingBytes>0)detail.append("\nAttenzione: ").append(info.trailingBytes).append(" byte dopo la fine JPEG. Potrebbero contenere altre immagini o dati Motion Photo: tipo da verificare. Il report contiene conteggio e hash, non questi byte.");
        detail.append("\n\nSHA-256 originale\n").append(info.sha256);screen.details.setText(detail);
    }
    void toast(String value){Toast.makeText(activity,value,Toast.LENGTH_LONG).show();}
    void destroy(){destroyed=true;++generation;if(analysis!=null)analysis.cancel(true);if(exportTask!=null)exportTask.cancel(true);worker.shutdownNow();screen.sections.closed=true;}
    private static String message(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
}
