package it.edo.codecprobe;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.*;

/** Audio SAF actions and lightweight filename reads; no video permissions or metadata workers. */
final class AudioSelection {
    static final int FILES=101,FOLDER=102,LYRICS=104;
    private final AudioController audio;
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    AudioSelection(AudioController audio){this.audio=audio;}
    void pick(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        audio.activity.startActivityForResult(intent,FILES);
    }
    void pickLyrics(){audio.activity.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE)
        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true).putExtra(Intent.EXTRA_TITLE,"Seleziona i testi .lrc").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),LYRICS);}
    void clearLyrics(){++audio.state.lyricsGeneration;audio.state.readingLyrics=false;audio.state.lyricsInputs.clear();audio.state.lyricsNames.clear();audio.save();audio.render();}
    void folder(){audio.activity.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),FOLDER);}
    void result(int request,int code,Intent data){
        if(audio.state.destroyed||code!=android.app.Activity.RESULT_OK||data==null)return;
        if(request==FOLDER){
            Uri uri=data.getData();if(uri==null)return;
            int grants=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if(grants!=(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION)){audio.toast("Per la cartella audio servono accesso in lettura e scrittura");return;}
            try{audio.activity.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}
            catch(SecurityException error){audio.toast("La cartella non concede un’autorizzazione persistente: scegline un’altra");return;}
            audio.state.folder=uri.toString();audio.save();audio.render();return;
        }
        if(request==LYRICS){readLyrics(data);return;}
        if(request!=FILES)return;
        LinkedHashSet<String> uris=new LinkedHashSet<>();
        if(data.getClipData()!=null)for(int n=0;n<data.getClipData().getItemCount();n++){Uri uri=data.getClipData().getItemAt(n).getUri();if(uri!=null)uris.add(uri.toString());}
        else if(data.getData()!=null)uris.add(data.getData().toString());
        if(uris.isEmpty())return;
        ++audio.state.selectionGeneration;audio.state.readingNames=true;audio.state.inputs.clear();audio.state.names.clear();boolean temporary=false;
        for(String value:uris){
            Uri uri=Uri.parse(value);audio.state.inputs.add(value);audio.state.names.add("Brano "+audio.state.inputs.size());
            try{audio.activity.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}
            catch(SecurityException error){temporary=true;}
        }
        audio.save();audio.render();if(temporary)audio.toast("Alcuni file concedono solo accesso temporaneo: potrebbe servire riselezionarli dopo la chiusura");
        readNames();
    }
    private void readLyrics(Intent data){
        LinkedHashSet<String> values=new LinkedHashSet<>();
        if(data.getClipData()!=null)for(int n=0;n<data.getClipData().getItemCount();n++)values.add(data.getClipData().getItemAt(n).getUri().toString());
        else if(data.getData()!=null)values.add(data.getData().toString());
        if(values.isEmpty())return;int generation=++audio.state.lyricsGeneration;audio.state.readingLyrics=true;audio.render();
        worker.execute(()->{
            ArrayList<String> uris=new ArrayList<>(),names=new ArrayList<>();int invalid=0;boolean temporary=false;
            for(String value:values){
                if(audio.state.destroyed||generation!=audio.state.lyricsGeneration||Thread.currentThread().isInterrupted())return;
                String name="";Uri uri=Uri.parse(value);
                try(Cursor rows=audio.activity.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(rows!=null&&rows.moveToFirst()&&!rows.isNull(0))name=rows.getString(0);}catch(RuntimeException ignored){}
                if(!AudioLyrics.isLyrics(name)){invalid++;continue;}
                try{audio.activity.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException error){temporary=true;}
                uris.add(value);names.add(name);
            }
            int rejected=invalid;boolean transientGrant=temporary;
            audio.activity.runOnUiThread(()->{
                if(audio.state.destroyed||generation!=audio.state.lyricsGeneration)return;
                audio.state.readingLyrics=false;
                if(!uris.isEmpty()){audio.state.lyricsInputs.clear();audio.state.lyricsInputs.addAll(uris);audio.state.lyricsNames.clear();audio.state.lyricsNames.addAll(names);}
                audio.save();audio.render();if(rejected>0)audio.toast(rejected+" file senza estensione .lrc esclusi dalla selezione");
                if(transientGrant)audio.toast("Alcuni testi concedono accesso temporaneo: potrebbe servire riselezionarli dopo la chiusura");
            });
        });
    }
    private void readNames(){
        final int generation=audio.state.selectionGeneration;final ArrayList<String> inputs=new ArrayList<>(audio.state.inputs);
        worker.execute(()->{
            ArrayList<String> names=new ArrayList<>();
            for(int n=0;n<inputs.size();n++){
                if(audio.state.destroyed||generation!=audio.state.selectionGeneration||Thread.currentThread().isInterrupted())return;
                String name="Brano "+(n+1);
                try(Cursor row=audio.activity.getContentResolver().query(Uri.parse(inputs.get(n)),new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
                    if(row!=null&&row.moveToFirst()&&!row.isNull(0))name=row.getString(0);
                }catch(RuntimeException ignored){}
                names.add(name);
            }
            audio.activity.runOnUiThread(()->{
                if(audio.state.destroyed||generation!=audio.state.selectionGeneration)return;
                audio.state.readingNames=false;audio.state.names.clear();audio.state.names.addAll(names);audio.save();audio.render();
            });
        });
    }
    void clear(){++audio.state.selectionGeneration;audio.state.readingNames=false;audio.state.inputs.clear();audio.state.names.clear();audio.save();audio.render();}
    void close(){worker.shutdownNow();}
}
