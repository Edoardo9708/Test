package it.edo.codecprobe;

import android.content.Context;
import android.content.Intent;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Atomic cursor and cumulative totals. The immutable queue and full logs stay in separate files. */
final class ImageSessionStore {
    private static AtomicFile file(Context c){return new AtomicFile(new File(c.getFilesDir(),"image-session.json"));}
    static boolean exists(Context c){AtomicFile f=file(c);return f.getBaseFile().exists()||new File(f.getBaseFile()+".bak").exists();}
    static final class Session {
        final String queueId;final ImageRunState run;
        Session(String queueId,ImageRunState run){this.queueId=queueId;this.run=run;}
        Intent request(Context c){Intent intent=new Intent(c,ImageConversionService.class).setAction(ImageConversionService.START).putExtra("queueId",queueId).putExtra("metadataOnly",run.metadataOnly).putExtra("restore",true);run.options.put(intent);return intent;}
    }
    static synchronized void save(Context c,String queueId,ImageRunState run)throws IOException{
        if(queueId==null)throw new IOException("Coda non disponibile per il recupero");
        ImageQueueStore.file(c,queueId);
        JSONObject json=new JSONObject();ImageOptions o=run.options;
        try{
            json.put("version",1).put("queue",queueId).put("metadataOnly",run.metadataOnly);
            json.put("quality",o.quality).put("preset",o.preset).put("tune",o.tune).put("folder",o.folder==null?JSONObject.NULL:o.folder).put("onlySmaller",o.onlySmaller).put("screenOn",o.keepScreenOn);
            json.put("completed",run.completed).put("total",run.total).put("saved",run.saved).put("failed",run.failed).put("skipped",run.skipped).put("cancelled",run.cancelledCount).put("alreadyDone",run.alreadyDone).put("checked",run.checked).put("inputBytes",run.inputSavedBytes).put("outputBytes",run.outputSavedBytes);
            json.put("elapsed",run.elapsedBefore+(run.startedAt>0?Math.max(0,android.os.SystemClock.elapsedRealtime()-run.startedAt):0));
        }catch(JSONException error){throw new IOException("Salvataggio sessione immagini non riuscito",error);}
        AtomicFile target=file(c);FileOutputStream out=null;
        try{out=target.startWrite();out.write(json.toString().getBytes(StandardCharsets.UTF_8));target.finishWrite(out);}
        catch(IOException|RuntimeException error){if(out!=null)target.failWrite(out);throw new IOException("Salvataggio sessione immagini non riuscito",error);}
    }
    static synchronized Session load(Context c)throws IOException{
        if(!exists(c))return null;
        try(InputStream in=file(c).openRead()){
            ByteArrayOutputStream data=new ByteArrayOutputStream();byte[] buffer=new byte[2048];int count;
            while((count=in.read(buffer))!=-1){if(data.size()+count>16384)throw new IOException("Sessione immagini troppo grande");data.write(buffer,0,count);}
            JSONObject json=new JSONObject(data.toString(StandardCharsets.UTF_8.name()));if(json.getInt("version")!=1)throw new IOException("Versione sessione immagini non supportata");
            String queueId=json.getString("queue");if(!ImageQueueStore.file(c,queueId).isFile())throw new IOException("Elenco della sessione immagini mancante");
            ImageRunState run=new ImageRunState();run.metadataOnly=json.getBoolean("metadataOnly");run.options=new ImageOptions(json.getInt("quality"),json.getString("preset"),json.getString("tune"),json.isNull("folder")?null:json.getString("folder"),json.getBoolean("onlySmaller"),json.getBoolean("screenOn"),0);
            run.completed=json.getInt("completed");run.total=json.getInt("total");run.current=run.completed;run.saved=json.getInt("saved");run.failed=json.getInt("failed");run.skipped=json.getInt("skipped");run.cancelledCount=json.getInt("cancelled");run.alreadyDone=json.getInt("alreadyDone");run.checked=json.getInt("checked");run.inputSavedBytes=json.getLong("inputBytes");run.outputSavedBytes=json.getLong("outputBytes");run.elapsedBefore=json.getLong("elapsed");
            if(run.total<1||run.total>ImageQueueStore.BULK_LIMIT||run.completed<0||run.completed>run.total||run.saved<0||run.failed<0||run.skipped<0||run.cancelledCount<0||run.alreadyDone<0||run.checked<0||run.inputSavedBytes<0||run.outputSavedBytes<0||run.elapsedBefore<0||(long)run.saved+run.failed+run.skipped+run.cancelledCount+run.alreadyDone+run.checked!=run.completed)throw new IOException("Contatori sessione immagini non validi");
            return new Session(queueId,run);
        }catch(JSONException|IllegalArgumentException error){throw new IOException("Sessione immagini non leggibile",error);}
    }
    static synchronized void clear(Context c,String queueId){file(c).delete();ImageQueueStore.discard(c,queueId);}
    static synchronized void discard(Context c){try{Session session=load(c);if(session!=null)ImageQueueStore.discard(c,session.queueId);}catch(IOException ignored){}file(c).delete();}
    private ImageSessionStore(){}
}
