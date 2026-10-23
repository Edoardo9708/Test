package it.edo.codecprobe;

import android.content.Context;
import org.json.*;

final class ImageDraftStore {
    static ImageState load(Context context){
        ImageState state=new ImageState();var prefs=context.getSharedPreferences("image-workspace",0);
        state.bulk=prefs.getBoolean("bulk",false);state.selectionLabel=prefs.getString("selectionLabel","");
        try{JSONArray rows=new JSONArray(prefs.getString("selection","[]"));if(rows.length()>(state.bulk?ImageQueueStore.BULK_LIMIT:ImageQueueStore.LIMIT))throw new JSONException("selection limit");for(int i=0;i<rows.length();i++)state.inputs.add(rows.getString(i));}
        catch(JSONException e){state.inputs.clear();}
        try{JSONArray names=new JSONArray(prefs.getString("names","[]"));for(int i=0;i<Math.min(names.length(),state.inputs.size());i++)state.names.add(names.getString(i));}catch(JSONException ignored){state.names.clear();}
        state.selected=prefs.getInt("selected",0);state.clamp();ImageOptions defaults=ImageOptions.load(context);
        try{state.options=new ImageOptions(prefs.getInt("quality",defaults.quality),prefs.getString("preset",defaults.preset),prefs.getString("tune",defaults.tune),prefs.contains("hasOptions")?prefs.getString("folder",null):defaults.folder,prefs.getBoolean("smaller",defaults.onlySmaller),prefs.getBoolean("screenOn",defaults.keepScreenOn),prefs.getLong("revision",defaults.revision));}catch(IllegalArgumentException e){state.options=defaults;}
        state.destinations.load(prefs,state.inputs,state.options.folder);
        if(!ImageConversionService.snapshot().running&&state.options.revision!=defaults.revision){state.options=defaults;}return state;
    }
    static final class Selection {
        private final java.util.ArrayList<String> inputs,names;
        private final DestinationPlan.Snapshot destinations;
        Selection(ImageState state){inputs=new java.util.ArrayList<>(state.inputs);names=new java.util.ArrayList<>(state.names);destinations=new DestinationPlan.Snapshot(state.destinations.plan);}
        boolean matches(ImageState state){return inputs.equals(state.inputs)&&names.equals(state.names)&&destinations.matches(state.destinations.plan);}
    }
    static void save(Context context,ImageState state){
        ImageOptions o=state.options;state.destinations.sync(state.inputs,o==null?null:o.folder);
        var edit=context.getSharedPreferences("image-workspace",0).edit().putBoolean("bulk",state.bulk).putString("selectionLabel",state.selectionLabel).putInt("selected",state.selected);
        if(state.savedSelection==null||!state.savedSelection.matches(state)){
            edit.putString("selection",new JSONArray(state.inputs).toString()).putString("names",new JSONArray(state.names).toString());
            state.destinations.save(edit,state.inputs,o==null?null:o.folder);state.savedSelection=new Selection(state);
        }
        if(o!=null)edit.putBoolean("hasOptions",true).putInt("quality",o.quality).putString("preset",o.preset).putString("tune",o.tune).putString("folder",o.folder).putBoolean("smaller",o.onlySmaller).putBoolean("screenOn",o.keepScreenOn).putLong("revision",o.revision);
        edit.apply();
    }
    private ImageDraftStore(){}
}
