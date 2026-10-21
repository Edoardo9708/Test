package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import java.util.LinkedHashSet;

/** SAF selection and export-picker state; no pixel decoding or encoder calls. */
final class ImageSelection {
    static final int FILES=201,REPORT=202,LOG=203,FOLDER=204,ERRORS=205,SOURCE_FOLDER=206;
    private final ImageController images;private String pendingInput;
    ImageSelection(ImageController images,Bundle saved){this.images=images;pendingInput=saved==null?null:saved.getString("imageReportSource");}
    void save(Bundle out){out.putString("imageReportSource",pendingInput);}
    void pick(){if(images.busy())return;images.activity.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/jpeg").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),FILES);}
    void sourceFolder(){if(!images.busy())images.activity.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION),SOURCE_FOLDER);}
    void folder(){if(!images.busy())images.activity.startActivityForResult(new Intent(images.activity,FolderPickerActivity.class),FOLDER);}
    void log(){if(!images.busy())images.activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"v"+BuildConfig.VERSION_NAME+"-immagini-log-"+System.currentTimeMillis()+".txt"),LOG);}
    void errors(){if(!images.busy())images.activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/x-ndjson").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"CodecProbe-errori-immagini.jsonl"),ERRORS);}
    void export(){
        if(images.state.info==null||images.state.reading||images.busy()||pendingInput!=null)return;
        pendingInput=images.state.inputs.get(images.state.selected);
        images.activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"v"+BuildConfig.VERSION_NAME+"-foto-inventario-"+System.currentTimeMillis()+".json"),REPORT);
    }
    void result(int request,int code,Intent data){
        if(images.destinations.result(request,code,data))return;
        if(request==REPORT){String input=pendingInput;pendingInput=null;if(code==Activity.RESULT_OK&&data!=null&&data.getData()!=null&&input!=null)images.export(Uri.parse(input),data.getData());return;}
        if(request==ERRORS){if(code==Activity.RESULT_OK&&data!=null&&data.getData()!=null)images.exportLog(data.getData(),ImageFailureStore.FILE);return;}
        if(request==LOG){if(code==Activity.RESULT_OK&&data!=null&&data.getData()!=null)images.exportLog(data.getData());return;}
        if(code!=Activity.RESULT_OK||data==null||images.busy())return;
        if(request==SOURCE_FOLDER){
            if(data.getData()==null)return;
            try{int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if((flags&Intent.FLAG_GRANT_READ_URI_PERMISSION)==0)throw new SecurityException("Serve accesso in lettura");
                if((flags&Intent.FLAG_GRANT_WRITE_URI_PERMISSION)!=0)images.activity.getContentResolver().takePersistableUriPermission(data.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                else images.activity.getContentResolver().takePersistableUriPermission(data.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION);
                images.importFolder(data.getData());
            }catch(SecurityException error){images.toast("Cartella non importata: "+error.getMessage());}return;
        }
        if(request==FOLDER){if(data.getData()==null)return;try{int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);if(flags!=(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))throw new SecurityException("Servono lettura e scrittura");images.activity.getContentResolver().takePersistableUriPermission(data.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);images.setFolder(data.getData().toString());}catch(SecurityException e){images.toast("Cartella non salvata: "+e.getMessage());}return;}
        if(request!=FILES)return;
        LinkedHashSet<String> values=new LinkedHashSet<>();
        if(data.getClipData()!=null)for(int i=0;i<data.getClipData().getItemCount();i++){Uri uri=data.getClipData().getItemAt(i).getUri();if(uri!=null)values.add(uri.toString());}
        else if(data.getData()!=null)values.add(data.getData().toString());
        if(values.isEmpty())return;
        LinkedHashSet<String> combined=new LinkedHashSet<>(images.state.inputs);combined.addAll(values);
        int limit=images.state.bulk?ImageQueueStore.BULK_LIMIT:ImageQueueStore.LIMIT;
        if(combined.size()>limit){images.toast("La coda può contenere al massimo "+limit+" foto.");return;}
        boolean temporary=false;
        for(String value:values)try{images.activity.getContentResolver().takePersistableUriPermission(Uri.parse(value),Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException e){temporary=true;}
        images.screen.selectedItem.cancelTransition();ImageMetadataCache.memory.clear();images.state.names.clear();images.state.inputs.clear();images.state.inputs.addAll(combined);images.state.selected=0;images.state.destinations.sync(images.state.inputs,images.state.options.folder);images.open();images.readNames();
        if(temporary)images.toast("Alcune foto concedono solo accesso temporaneo: potrebbe servire riselezionarle dopo la chiusura.");
    }
}
