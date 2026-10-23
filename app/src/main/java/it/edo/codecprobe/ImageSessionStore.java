package it.edo.codecprobe;

import android.content.Context;
import android.content.Intent;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Atomic cursor and cumulative totals. Queue entries and full logs stay in separate files. */
final class ImageSessionStore {
    private static final int VERSION=1, MAX_BYTES=16384;

    static final class Session {
        final String queueId;
        final ImageRunState run;

        Session(String queueId,ImageRunState run){this.queueId=queueId;this.run=run;}

        Intent request(Context context){
            Intent intent=new Intent(context,ImageConversionService.class)
                .setAction(ImageConversionService.START)
                .putExtra("queueId",queueId)
                .putExtra("metadataOnly",run.metadataOnly)
                .putExtra("restore",true);
            run.options.put(intent);
            return intent;
        }
    }

    private static AtomicFile file(Context context){
        return new AtomicFile(new File(context.getFilesDir(),"image-session.json"));
    }

    static boolean exists(Context context){
        AtomicFile target=file(context);
        return target.getBaseFile().exists()||new File(target.getBaseFile()+".bak").exists();
    }

    static synchronized void save(Context context,String queueId,ImageRunState run)throws IOException{
        if(queueId==null)throw new IOException("Coda non disponibile per il recupero");
        ImageQueueStore.file(context,queueId);
        byte[] bytes;
        try{bytes=encode(queueId,run).toString().getBytes(StandardCharsets.UTF_8);}
        catch(JSONException error){throw new IOException("Salvataggio sessione immagini non riuscito",error);}
        AtomicFile target=file(context);
        FileOutputStream out=null;
        try{
            out=target.startWrite();
            out.write(bytes);
            target.finishWrite(out);
        }catch(IOException|RuntimeException error){
            if(out!=null)target.failWrite(out);
            throw new IOException("Salvataggio sessione immagini non riuscito",error);
        }
    }

    static synchronized Session load(Context context)throws IOException{
        if(!exists(context))return null;
        try(InputStream in=file(context).openRead();ByteArrayOutputStream data=new ByteArrayOutputStream()){
            byte[] buffer=new byte[2048];int count;
            while((count=in.read(buffer))!=-1){
                if(data.size()+count>MAX_BYTES)throw new IOException("Sessione immagini troppo grande");
                data.write(buffer,0,count);
            }
            JSONObject json=new JSONObject(data.toString(StandardCharsets.UTF_8.name()));
            if(json.getInt("version")!=VERSION)throw new IOException("Versione sessione immagini non supportata");
            String queueId=json.getString("queue");
            if(!ImageQueueStore.file(context,queueId).isFile())throw new IOException("Elenco della sessione immagini mancante");
            ImageRunState run=decode(json);
            validate(run);
            return new Session(queueId,run);
        }catch(JSONException|IllegalArgumentException error){
            throw new IOException("Sessione immagini non leggibile",error);
        }
    }

    /** Called only when the queue finishes or the user explicitly cancels it. */
    static synchronized void clear(Context context,String queueId){
        file(context).delete();
        ImageQueueStore.discard(context,queueId);
    }

    /** A broken checkpoint can also be discarded explicitly, without touching output files. */
    static synchronized void discard(Context context){
        try{
            Session session=load(context);
            if(session!=null)ImageQueueStore.discard(context,session.queueId);
        }catch(IOException ignored){}
        file(context).delete();
    }

    private static JSONObject encode(String queueId,ImageRunState run)throws JSONException{
        ImageOptions options=run.options;
        long elapsed=run.elapsedBefore+(run.startedAt>0?Math.max(0,android.os.SystemClock.elapsedRealtime()-run.startedAt):0);
        return new JSONObject().put("version",VERSION).put("queue",queueId)
            .put("metadataOnly",run.metadataOnly)
            .put("quality",options.quality).put("preset",options.preset).put("tune",options.tune)
            .put("folder",options.folder==null?JSONObject.NULL:options.folder)
            .put("onlySmaller",options.onlySmaller).put("screenOn",options.keepScreenOn)
            .put("completed",run.completed).put("total",run.total)
            .put("saved",run.saved).put("failed",run.failed).put("skipped",run.skipped)
            .put("cancelled",run.cancelledCount).put("alreadyDone",run.alreadyDone).put("checked",run.checked)
            .put("inputBytes",run.inputSavedBytes).put("outputBytes",run.outputSavedBytes).put("elapsed",elapsed);
    }

    private static ImageRunState decode(JSONObject json)throws JSONException{
        ImageRunState run=new ImageRunState();
        run.metadataOnly=json.getBoolean("metadataOnly");
        run.options=new ImageOptions(json.getInt("quality"),json.getString("preset"),json.getString("tune"),
            json.isNull("folder")?null:json.getString("folder"),json.getBoolean("onlySmaller"),json.getBoolean("screenOn"),0);
        run.completed=json.getInt("completed");run.total=json.getInt("total");run.current=run.completed;
        run.saved=json.getInt("saved");run.failed=json.getInt("failed");run.skipped=json.getInt("skipped");
        run.cancelledCount=json.getInt("cancelled");run.alreadyDone=json.getInt("alreadyDone");run.checked=json.getInt("checked");
        run.inputSavedBytes=json.getLong("inputBytes");run.outputSavedBytes=json.getLong("outputBytes");run.elapsedBefore=json.getLong("elapsed");
        return run;
    }

    private static void validate(ImageRunState run)throws IOException{
        long outcomes=(long)run.saved+run.failed+run.skipped+run.cancelledCount+run.alreadyDone+run.checked;
        boolean valid=run.total>=1&&run.total<=ImageQueueStore.BULK_LIMIT&&run.completed>=0&&run.completed<=run.total
            &&run.saved>=0&&run.failed>=0&&run.skipped>=0&&run.cancelledCount>=0&&run.alreadyDone>=0&&run.checked>=0
            &&run.inputSavedBytes>=0&&run.outputSavedBytes>=0&&run.elapsedBefore>=0&&outcomes==run.completed;
        if(!valid)throw new IOException("Contatori sessione immagini non validi");
    }

    private ImageSessionStore(){}
}
