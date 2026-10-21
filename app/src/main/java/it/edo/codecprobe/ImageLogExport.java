package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import java.io.*;
import java.util.concurrent.*;

/** Freeze on click, then export the closed snapshot without holding up the conversion log. */
final class ImageLogExport {
    private final ImageController images;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private File pending;private int pendingRequest;private boolean working,closed;
    ImageLogExport(ImageController images,Bundle saved){
        this.images=images;
        if(saved!=null){pending=ImageLogStore.restoredSnapshot(images.activity,saved.getString("imageLogSnapshot"));pendingRequest=saved.getInt("imageLogSnapshotRequest");}
    }
    boolean busy(){return working||pending!=null;}
    boolean available(String filename){return !closed&&!busy()&&!images.state.exporting&&new File(images.activity.getFilesDir(),filename).isFile();}
    void save(Bundle out){if(pending!=null){out.putString("imageLogSnapshot",pending.getName());out.putInt("imageLogSnapshotRequest",pendingRequest);}}
    void start(boolean errors){
        String filename=errors?ImageFailureStore.FILE:ImageConversionService.LOG;
        if(!available(filename))return;
        boolean partial=ImageConversionService.snapshot().running;long capturedAt=System.currentTimeMillis();
        working=true;images.state.notice="Preparazione copia del log…";images.render();
        worker.execute(()->{
            File snapshot=null;String failure=null;
            try{snapshot=ImageLogStore.snapshot(images.activity,filename);}catch(IOException|RuntimeException error){failure=message(error);}
            File ready=snapshot;String problem=failure;
            images.activity.runOnUiThread(()->{
                if(closed){if(ready!=null)ready.delete();return;}
                working=false;
                if(problem!=null){images.state.notice="Copia del log non riuscita: "+problem;images.render();images.toast(images.state.notice);return;}
                pending=ready;pendingRequest=errors?ImageSelection.ERRORS:ImageSelection.LOG;
                String title="v"+BuildConfig.VERSION_NAME+(errors?"-immagini-errori-":"-immagini-log-")+(partial?"parziale-":"")+capturedAt+(errors?".jsonl":".txt");
                images.state.notice="Copia del log pronta. Scegli dove salvarla.";images.render();
                try{images.activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(errors?"application/x-ndjson":"text/plain").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,title),pendingRequest);}
                catch(RuntimeException error){discard();images.state.notice="Esportazione non avviata: "+message(error);images.render();images.toast(images.state.notice);}
            });
        });
    }
    void result(int request,int code,Intent data){
        if(pending==null||pendingRequest!=request)return;
        File snapshot=pending;pending=null;pendingRequest=0;
        if(code!=Activity.RESULT_OK||data==null||data.getData()==null){snapshot.delete();images.state.notice="Esportazione log annullata.";images.render();return;}
        Uri output=data.getData();working=true;images.state.notice="Esportazione copia del log…";images.render();
        worker.execute(()->{
            String notice="Copia del log immagini esportata.";
            try(InputStream in=new FileInputStream(snapshot);OutputStream out=images.activity.getContentResolver().openOutputStream(output,"wt")){
                if(out==null)throw new IOException("Destinazione non scrivibile");ImageLogStore.copy(in,out);
            }catch(IOException|RuntimeException error){notice="Esportazione log non completata: "+message(error);}
            finally{snapshot.delete();}
            String result=notice;images.activity.runOnUiThread(()->{if(closed)return;working=false;images.state.notice=result;images.render();images.toast(result);});
        });
    }
    private void discard(){if(pending!=null)pending.delete();pending=null;pendingRequest=0;}
    void close(){closed=true;worker.shutdownNow();if(!images.activity.isChangingConfigurations())discard();}
    private static String message(Exception error){return error.getMessage()==null?error.getClass().getSimpleName():error.getMessage();}
}
