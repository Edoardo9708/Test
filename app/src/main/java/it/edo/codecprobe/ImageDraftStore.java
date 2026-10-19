package it.edo.codecprobe;

import android.content.Context;
import org.json.*;

final class ImageDraftStore {
    static ImageState load(Context context){
        ImageState state=new ImageState();var prefs=context.getSharedPreferences("image-workspace",0);
        try{JSONArray rows=new JSONArray(prefs.getString("selection","[]"));if(rows.length()>100)throw new JSONException("selection limit");for(int i=0;i<rows.length();i++)state.inputs.add(rows.getString(i));}
        catch(JSONException e){state.inputs.clear();}
        state.selected=prefs.getInt("selected",0);state.clamp();return state;
    }
    static void save(Context context,ImageState state){context.getSharedPreferences("image-workspace",0).edit().putString("selection",new JSONArray(state.inputs).toString()).putInt("selected",state.selected).apply();}
    private ImageDraftStore(){}
}
