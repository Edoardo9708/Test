package it.edo.codecprobe;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** One durable queue, not encoder memory. Atomic replacement preserves the previous checkpoint. */
final class SavedSession {
    static final String RESTORE="restoreSession", ID="savedSessionId";
    final Intent request;
    final ConversionService.State state;

    SavedSession(Intent request, ConversionService.State state) {
        this.request=new Intent(request);this.state=new ConversionService.State(state);
    }

    static boolean terminal(CompressionSummary.Status status) {
        return status!=CompressionSummary.Status.IN_PROGRESS && status!=CompressionSummary.Status.SKIPPED
            && status!=CompressionSummary.Status.CANCELLED;
    }

    boolean hasPending() {
        for(CompressionSummary.Item item:state.items)if(!terminal(item.status))return true;
        return false;
    }

    Intent resumeRequest() {
        Intent task=new Intent(request).putExtra(RESTORE,state.sessionId);
        task.putStringArrayListExtra("requestedOutputNames",request.getStringArrayListExtra("outputNames"));
        boolean[] finished=new boolean[state.items.size()];
        for(int n=0;n<finished.length;n++)finished[n]=terminal(state.items.get(n).status);
        task.putExtra("finishedFiles",finished);
        return task;
    }

    private static AtomicFile file(Context context) {
        return new AtomicFile(new File(context.getFilesDir(),"conversion-session.json"));
    }

    static synchronized void clear(Context context) { file(context).delete(); }

    static synchronized void save(Context context,Intent request,ConversionService.State state)throws IOException {
        FileOutputStream stream=null;
        AtomicFile target=file(context);
        try {
            byte[] bytes=encode(request,state).toString().getBytes(StandardCharsets.UTF_8);
            stream=target.startWrite();stream.write(bytes);target.finishWrite(stream);
        }catch(JSONException|RuntimeException error){
            if(stream!=null)target.failWrite(stream);
            throw new IOException("Salvataggio sessione non riuscito",error);
        }catch(IOException error){if(stream!=null)target.failWrite(stream);throw error;}
    }

    static synchronized SavedSession load(Context context)throws IOException {
        AtomicFile target=file(context);
        if(!target.getBaseFile().exists()&&!new File(target.getBaseFile()+".bak").exists())return null;
        try(InputStream input=target.openRead();ByteArrayOutputStream data=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int count;
            while((count=input.read(buffer))!=-1){if(data.size()+count>8*1024*1024)throw new IOException("Sessione troppo grande");data.write(buffer,0,count);}
            return decode(context,new JSONObject(data.toString(StandardCharsets.UTF_8.name())));
        }catch(JSONException|RuntimeException error){throw new IOException("Sessione salvata non leggibile",error);}
    }

    private static JSONObject encode(Intent request,ConversionService.State s)throws JSONException {
        JSONObject json=new JSONObject().put("schema",1).put("id",s.sessionId)
            .put("request",BatchRetry.encode(request)).put("recoverable",s.recoverable)
            .put("outputNames",strings(request.getStringArrayListExtra("outputNames")))
            .put("elapsed",elapsed(s,SystemClock.elapsedRealtime()))
            .put("message",s.message).put("index",s.batchIndex).put("filePercent",s.filePercent)
            .put("frames",s.doneFrames).put("totalFrames",s.totalFrames).put("done",s.done).put("total",s.total)
            .put("bframes",s.bframeCount).put("lookahead",s.lookaheadCount).put("cuTree",s.cuTree)
            .put("aq",s.aqMode).put("aqStrength",s.aqStrength).put("memoryProtection",s.memoryProtection);
        boolean[] omitted=request.getBooleanArrayExtra("omitted");JSONArray omissions=new JSONArray();
        for(int n=0;n<s.inputs.size();n++)omissions.put(omitted!=null&&omitted[n]);json.put("omitted",omissions);
        JSONArray items=new JSONArray();
        for(CompressionSummary.Item item:s.items)items.put(new JSONObject().put("status",item.status.name()).put("before",item.before).put("after",item.after));
        json.put("items",items).put("outputs",strings(s.outputUris));
        JSONArray failures=new JSONArray();
        for(ConversionFailure f:s.failures)failures.put(new JSONObject().put("number",f.number).put("message",f.message).put("lastState",f.lastState).put("details",f.details));
        return json.put("failures",failures);
    }

    static long elapsed(ConversionService.State s,long now) {
        return SessionClock.millis(s,now);
    }

    private static JSONArray strings(List<String> strings) { return new JSONArray(strings==null?Collections.emptyList():strings); }

    private static SavedSession decode(Context context,JSONObject json)throws JSONException {
        if(json.getInt("schema")!=1)throw new JSONException("Versione sessione non supportata");
        Intent task=BatchRetry.decode(context,json.getJSONObject("request"));task.removeExtra("retryFailures");
        ArrayList<String> inputs=task.getStringArrayListExtra("inputs"),names=task.getStringArrayListExtra("names");
        int count=inputs.size();
        JSONArray approved=json.getJSONArray("outputNames"),omissions=json.getJSONArray("omitted"),items=json.getJSONArray("items"),outputs=json.getJSONArray("outputs");
        if((approved.length()!=0&&approved.length()!=count)||omissions.length()!=count||items.length()!=count||outputs.length()!=count)throw new JSONException("Elenco sessione incoerente");
        if(approved.length()>0){ArrayList<String> values=new ArrayList<>();for(int n=0;n<count;n++)values.add(approved.getString(n));task.putStringArrayListExtra("outputNames",values);}
        boolean[] omitted=new boolean[count];for(int n=0;n<count;n++)omitted[n]=omissions.getBoolean(n);task.putExtra("omitted",omitted);
        ConversionService.State s=new ConversionService.State();
        s.sessionId=json.getString("id");if(s.sessionId.isEmpty())throw new JSONException("Sessione senza identificativo");
        s.hasSession=true;s.inputs=inputs;s.names=names;s.folders=task.getStringArrayListExtra("folders");s.batchTotal=count;
        s.requestedCrf=task.getIntExtra("crf",23);s.requestedCpu=task.getIntExtra("cpu",1);
        int preset=task.getIntExtra("preset",6);if(preset<0||preset>=AppSettings.PRESETS.length||s.requestedCrf<0||s.requestedCrf>51||s.requestedCpu<0||s.requestedCpu>2)throw new JSONException("Parametri sessione non validi");
        s.requestedPreset=AppSettings.PRESETS[preset];s.onlySmaller=task.getBooleanExtra("onlySmaller",false);
        s.individualOrigins=task.getBooleanArrayExtra("individualOrigins");if(s.individualOrigins==null)s.individualOrigins=new boolean[count];
        s.individualFolders=task.getStringArrayListExtra("individualFolders");if(s.individualFolders==null)s.individualFolders=new ArrayList<>(s.folders);
        s.applyAllDestinations=task.getBooleanExtra("applyAllDestinations",false);s.allOrigin=task.getBooleanExtra("allOrigin",true);s.allFolder=task.getStringExtra("allFolder");
        for(int n=0;n<count;n++){
            JSONObject item=items.getJSONObject(n);CompressionSummary.Status status=CompressionSummary.Status.valueOf(item.getString("status"));
            s.items.add(new CompressionSummary.Item(n+1,names.get(n),status,item.getLong("before"),item.getLong("after")));
            String uri=outputs.getString(n);s.outputUris.add(uri);
            switch(status){case COMPLETE:if(uri.isEmpty())throw new JSONException("Risultato senza URI");s.succeeded++;s.result=Uri.parse(uri);s.results.add(s.result);break;
                case FAILED:s.failed++;break;case OMITTED:s.ignored++;break;case USER_SKIPPED:s.skipped++;break;
                case NO_SAVING:case SIZE_UNKNOWN:s.notSaved++;break;default:break;}
        }
        JSONArray failures=json.getJSONArray("failures");
        for(int n=0;n<failures.length();n++){JSONObject f=failures.getJSONObject(n);int number=f.getInt("number");if(number<1||number>count)throw new JSONException("Errore senza video");
            s.failures.add(new ConversionFailure(number,names.get(number-1),f.getString("message"),f.getString("lastState"),f.getString("details")));}
        s.elapsedBefore=Math.max(0,json.getLong("elapsed"));s.batchIndex=json.getInt("index");
        if(s.batchIndex<0||s.batchIndex>count)throw new JSONException("Indice sessione non valido");
        int current=Math.max(0,s.batchIndex-1);s.source=names.get(current);s.input=Uri.parse(inputs.get(current));s.folder=Uri.parse(s.folders.get(current));
        s.filePercent=json.getInt("filePercent");s.doneFrames=json.getInt("frames");s.totalFrames=json.getInt("totalFrames");s.done=json.getLong("done");s.total=json.getLong("total");
        s.bframeCount=json.getInt("bframes");s.lookaheadCount=json.getInt("lookahead");s.cuTree=json.getInt("cuTree");s.aqMode=json.getInt("aq");s.aqStrength=json.getInt("aqStrength");
        s.memoryProtection=json.optBoolean("memoryProtection",false);
        s.message=json.getString("message");s.sizeReport=CompressionSummary.render(s.items);
        SavedSession saved=new SavedSession(task,s);s.recoverable=json.getBoolean("recoverable")&&saved.hasPending();
        if(s.recoverable)s.message="Sessione salvata · i video completati restano acquisiti. Il video interrotto ripartirà dall’inizio.";
        return new SavedSession(task,s);
    }
}
