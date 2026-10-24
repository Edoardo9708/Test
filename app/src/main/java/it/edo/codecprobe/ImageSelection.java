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
    void log(){images.logExport.start(false);}
    void errors(){images.logExport.start(true);}
    void export(){
        if(images.state.info==null||images.state.reading||images.busy()||pendingInput!=null)return;
        pendingInput=images.state.inputs.get(images.state.selected);
        images.activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"v"+BuildConfig.VERSION_NAME+"-foto-inventario-"+System.currentTimeMillis()+".json"),REPORT);
    }
    void result(int request,int code,Intent data){
        if(images.destinations.result(request,code,data))return;
        if(request==REPORT){String input=pendingInput;pendingInput=null;if(code==Activity.RESULT_OK&&data!=null&&data.getData()!=null&&input!=null)images.export(Uri.parse(input),data.getData());return;}
        if(request==ERRORS||request==LOG){images.logExport.result(request,code,data);return;}
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
        java.util.HashSet<String> existing=new java.util.HashSet<>(images.state.inputs);values.removeAll(existing);
        if(values.isEmpty()){images.toast("Le foto scelte sono già nella selezione.");return;}
        images.acceptSelection(new java.util.ArrayList<>(combined),new java.util.ArrayList<>(values));
    }
}
