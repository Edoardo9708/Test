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
    private String previewKey="";
    private static final MetadataMemoryCache<AudioInputInfo.Info> previews=new MetadataMemoryCache<>(500,4L*1024*1024);
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    AudioSelection(AudioController audio){this.audio=audio;}
    void restore(){if(audio.state.readingNames)readNames();if(audio.state.readingLyrics)readLyricNames();}
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
        if(audio.state.destroyed||audio.state.preparing||AudioConversionService.snapshot().running||code!=android.app.Activity.RESULT_OK||data==null)return;
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
        if(uris.size()>500){audio.toast("Seleziona al massimo 500 brani per coda");return;}
        audio.screen.selectedItem.cancelTransition();previews.clear();++audio.state.selectionGeneration;audio.state.selectedIndex=0;audio.state.notice="";audio.state.readingNames=true;audio.state.inputs.clear();audio.state.names.clear();boolean temporary=false;
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
        if(data.getClipData()!=null)for(int n=0;n<data.getClipData().getItemCount();n++){Uri uri=data.getClipData().getItemAt(n).getUri();if(uri!=null)values.add(uri.toString());}
        else if(data.getData()!=null)values.add(data.getData().toString());
        if(values.isEmpty())return;
        if(values.size()>1000){audio.toast("Seleziona al massimo 1000 testi .lrc per coda");return;}
        ++audio.state.lyricsGeneration;audio.state.readingLyrics=true;
        audio.state.lyricsInputs.clear();audio.state.lyricsNames.clear();
        for(String value:values){audio.state.lyricsInputs.add(value);audio.state.lyricsNames.add("Testo");}
        audio.save();audio.render();readLyricNames();
    }
    private void readLyricNames(){
        final int generation=audio.state.lyricsGeneration;
        final ArrayList<String> values=new ArrayList<>(audio.state.lyricsInputs);
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
                audio.state.lyricsInputs.clear();audio.state.lyricsInputs.addAll(uris);audio.state.lyricsNames.clear();audio.state.lyricsNames.addAll(names);
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
    void clear(){audio.screen.selectedItem.cancelTransition();previews.clear();++audio.state.selectionGeneration;audio.state.selectedIndex=0;audio.state.notice="";audio.state.readingNames=false;audio.state.inputs.clear();audio.state.names.clear();audio.save();audio.render();}
    void remove(){
        if(audio.busy()||audio.state.inputs.isEmpty())return;
        audio.screen.selectedItem.cancelTransition();++audio.state.selectionGeneration;audio.state.inputs.remove(audio.state.selectedIndex);audio.state.names.remove(audio.state.selectedIndex);
        audio.state.selectedIndex=Math.max(0,Math.min(audio.state.selectedIndex,audio.state.inputs.size()-1));audio.save();audio.render();
    }
    void reload(){
        if(audio.busy()||audio.state.inputs.isEmpty())return;
        int index=audio.state.selectedIndex;
        previews.remove(audio.state.inputs.get(index)+"\n"+audio.state.names.get(index));previewKey="";audio.render();
    }
    void renderFiles(){
        AudioState state=audio.state;AudioScreen screen=audio.screen;int count=state.inputs.size();
        state.selectedIndex=Math.max(0,Math.min(state.selectedIndex,count-1));
        screen.selectedItem.render(state.names,java.util.Collections.emptyList(),state.selectedIndex,!audio.busy(),index->{
            if(audio.busy())return;
            if(index==state.selectedIndex){screen.selectedItem.cancelTransition();return;}
            String selectedUri=state.inputs.get(index);
            screen.selectedItem.transition(()->{if(audio.busy()||index>=state.inputs.size()||!selectedUri.equals(state.inputs.get(index)))return;state.selectedIndex=index;screen.selectedItem.setExpanded(false);audio.save();audio.render();screen.selectedItem.update(index,true,true);});
        });
        screen.names.setText(count==0?"Nessun brano selezionato":state.names.get(state.selectedIndex));
        screen.details.setVisibility(count==0?android.view.View.GONE:android.view.View.VISIBLE);screen.remove.setVisibility(count==0?android.view.View.GONE:android.view.View.VISIBLE);
        if(count==0){previewKey="";screen.inputInfo.setText("Seleziona uno o più brani");screen.selectedItem.setExpanded(false);return;}
        String uri=state.inputs.get(state.selectedIndex),name=state.names.get(state.selectedIndex);String selectedKey=uri+"\n"+name;AudioInputInfo.Info cached=previews.get(selectedKey);
        if(cached!=null){previewKey=selectedKey;screen.inputInfo.setText(cached.summary);screen.inputDetails.setText(cached.details);return;}
        if(selectedKey.equals(previewKey))return;previewKey=selectedKey;screen.inputInfo.setText(state.readingNames?"Lettura nomi…":"Lettura informazioni audio…");screen.inputDetails.setText("Lettura informazioni audio…");
        if(state.readingNames){previewKey="";return;}
        final long cacheEpoch=previews.epoch();
        worker.execute(()->{AudioInputInfo.Info info=AudioInputInfo.read(audio.activity,uri,name);audio.activity.runOnUiThread(()->{
            if(state.destroyed)return;previews.put(selectedKey,info,4096L+2L*(info.summary.length()+info.details.length()),cacheEpoch);if(cacheEpoch==previews.epoch()&&selectedKey.equals(previewKey)){screen.inputInfo.setText(info.summary);screen.inputDetails.setText(info.details);}
        });});
    }
    void close(){audio.screen.selectedItem.close();worker.shutdownNow();}
}
