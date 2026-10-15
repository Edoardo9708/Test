package it.edo.codecprobe;

import android.content.Context;
import android.content.Intent;
import java.util.*;
import org.json.*;

/** Stores only actual failures, with their original destinations, names and encoder options. */
final class BatchRetry {
    private static final String[] ARRAYS={"inputs","names","folders","destinationNotes","individualFolders","requestedOutputNames"};
    private static final String[] BOOLS={"bframes","lookahead","onlySmaller","applyAllDestinations","allOrigin","namingPrefix"};
    private static final String[] STRINGS={"allFolder","namingAddition"};
    static Intent select(Intent source,List<Integer> indices){
        if(indices.isEmpty())return null;
        ArrayList<String> inputs=source.getStringArrayListExtra("inputs");
        if(inputs==null)throw new IllegalArgumentException("Coda senza input");
        Set<Integer> seen=new HashSet<>();
        for(int index:indices)if(index<0||index>=inputs.size()||!seen.add(index))throw new IllegalArgumentException("Indice fallito non valido");
        Intent retry=new Intent(source);
        for(String key:ARRAYS){
            ArrayList<String> values=source.getStringArrayListExtra(key);
            if(key.equals("requestedOutputNames")){
                ArrayList<String> approved=source.getStringArrayListExtra("outputNames");if(approved!=null)values=approved;
            }
            if(values==null){retry.removeExtra(key);continue;}
            if(values.size()!=inputs.size()){
                if(key.equals("inputs")||key.equals("names")||key.equals("folders"))throw new IllegalArgumentException("Coda incoerente: "+key);
                retry.removeExtra(key);continue;
            }
            ArrayList<String> subset=new ArrayList<>();for(int index:indices)subset.add(values.get(index));
            retry.putStringArrayListExtra(key,subset);
        }
        boolean[] origins=source.getBooleanArrayExtra("individualOrigins");
        if(origins!=null&&origins.length==inputs.size()){
            boolean[] subset=new boolean[indices.size()];for(int n=0;n<subset.length;n++)subset[n]=origins[indices.get(n)];
            retry.putExtra("individualOrigins",subset);
        }else retry.removeExtra("individualOrigins");
        retry.removeExtra("outputNames");retry.removeExtra("omitted");
        retry.putExtra("retryFailures",true);
        return retry;
    }
    static boolean save(Context context,Intent request){
        try{
            android.content.SharedPreferences.Editor editor=context.getSharedPreferences("failed-conversion-retry",0).edit();
            if(request==null)editor.remove("request");else editor.putString("request",encode(request).toString());
            return editor.commit();
        }catch(JSONException|RuntimeException error){return false;}
    }
    static void clear(Context context){
        context.getSharedPreferences("failed-conversion-retry",0).edit().remove("request").apply();
    }
    static Intent load(Context context){
        try{String saved=context.getSharedPreferences("failed-conversion-retry",0).getString("request",null);
            return saved==null?null:decode(context,new JSONObject(saved));
        }catch(JSONException|RuntimeException error){return null;}
    }
    private static JSONObject encode(Intent request)throws JSONException {
        JSONObject json=new JSONObject();json.put("schema",1);
        for(String key:ARRAYS){ArrayList<String> values=request.getStringArrayListExtra(key);if(values!=null)json.put(key,new JSONArray(values));}
        boolean[] origins=request.getBooleanArrayExtra("individualOrigins");
        if(origins!=null){JSONArray array=new JSONArray();for(boolean origin:origins)array.put(origin);json.put("individualOrigins",array);}
        for(String key:BOOLS)json.put(key,request.getBooleanExtra(key,key.equals("bframes")||key.equals("lookahead")||key.equals("allOrigin")||key.equals("namingPrefix")));
        for(String key:STRINGS)if(request.getStringExtra(key)!=null)json.put(key,request.getStringExtra(key));
        json.put("crf",request.getIntExtra("crf",23));json.put("preset",request.getIntExtra("preset",6));json.put("cpu",request.getIntExtra("cpu",1));
        return json;
    }
    private static Intent decode(Context context,JSONObject json)throws JSONException {
        if(json.getInt("schema")!=1)throw new JSONException("Unsupported retry schema");
        int size=json.getJSONArray("inputs").length();if(size<1||size>BatchProgress.MAX_FILES)throw new JSONException("Invalid retry count");
        Intent task=new Intent(context,ConversionService.class).setAction(ConversionService.START).putExtra("retryFailures",true);
        for(String key:ARRAYS)if(json.has(key)){
            JSONArray array=json.getJSONArray(key);if(array.length()!=size)throw new JSONException("Invalid array: "+key);
            ArrayList<String> values=new ArrayList<>();for(int n=0;n<size;n++)values.add(array.getString(n));task.putStringArrayListExtra(key,values);
        }
        if(!task.hasExtra("names")||!task.hasExtra("folders"))throw new JSONException("Missing retry files");
        if(json.has("individualOrigins")){
            JSONArray array=json.getJSONArray("individualOrigins");if(array.length()!=size)throw new JSONException("Invalid origin choices");
            boolean[] values=new boolean[size];for(int n=0;n<size;n++)values[n]=array.getBoolean(n);task.putExtra("individualOrigins",values);
        }
        for(String key:BOOLS)task.putExtra(key,json.getBoolean(key));
        for(String key:STRINGS)if(json.has(key))task.putExtra(key,json.getString(key));
        for(String key:new String[]{"crf","preset","cpu"})task.putExtra(key,json.getInt(key));
        return task;
    }
    private BatchRetry(){}
}
