package it.edo.codecprobe;

import android.content.SharedPreferences;
import java.util.*;
import org.json.*;

/** Persists choices by source URI; removal/reordering never assigns another file's folder. */
final class DestinationDraft {
    final DestinationPlan plan=new DestinationPlan();
    private final ArrayList<String> sources=new ArrayList<>();

    void sync(List<String> inputs,String defaults) {
        if(sources.equals(inputs)&&plan.files.size()==inputs.size())return;
        Map<String,DestinationPlan.Choice> previous=new HashMap<>();
        for(int n=0;n<Math.min(sources.size(),plan.files.size());n++)previous.put(sources.get(n),plan.files.get(n));
        boolean override=plan.applyAll,origin=plan.all.origin;String folder=plan.all.folder;
        boolean existing=!sources.isEmpty();
        plan.reset(inputs.size(),defaults);
        for(int n=0;n<inputs.size();n++){
            DestinationPlan.Choice choice=previous.get(inputs.get(n));
            if(choice!=null){plan.files.get(n).origin=choice.origin;plan.files.get(n).folder=choice.folder;}
        }
        if(existing){plan.applyAll=override;plan.all.origin=origin;plan.all.folder=folder;}
        sources.clear();sources.addAll(inputs);
    }
    void reset(List<String> inputs,String defaults){sources.clear();plan.reset(0,defaults);sync(inputs,defaults);}
    void defaults(List<String> inputs,String folder){reset(inputs,folder);}
    void load(SharedPreferences prefs,List<String> inputs,String defaults){
        reset(inputs,defaults);
        try{
            JSONObject data=new JSONObject(prefs.getString("destinations","{}"));
            JSONArray rows=data.getJSONArray("files");if(rows.length()!=inputs.size())return;
            boolean[] origins=new boolean[rows.length()];ArrayList<String> folders=new ArrayList<>();
            for(int n=0;n<rows.length();n++){JSONObject row=rows.getJSONObject(n);if(!inputs.get(n).equals(row.getString("input")))return;origins[n]=row.getBoolean("origin");folders.add(row.getString("folder"));}
            plan.restore(origins,folders,data.getBoolean("applyAll"),data.getBoolean("allOrigin"),data.getString("allFolder"));
        }catch(JSONException ignored){/* Old drafts migrate to their previous single output folder. */}
    }
    void save(SharedPreferences.Editor prefs,List<String> inputs,String defaults){
        sync(inputs,defaults);JSONArray rows=new JSONArray();
        try{
            for(int n=0;n<inputs.size();n++){DestinationPlan.Choice choice=plan.files.get(n);rows.put(new JSONObject().put("input",inputs.get(n)).put("origin",choice.origin).put("folder",choice.folder));}
            prefs.putString("destinations",new JSONObject().put("files",rows).put("applyAll",plan.applyAll).put("allOrigin",plan.all.origin).put("allFolder",plan.all.folder).toString());
        }catch(JSONException error){throw new IllegalStateException("Destinazioni non serializzabili",error);}
    }
}
