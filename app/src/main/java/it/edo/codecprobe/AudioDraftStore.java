package it.edo.codecprobe;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import org.json.*;

/** Separate audio preference file; this draft is not an encoder checkpoint. */
final class AudioDraftStore {
    private static SharedPreferences preferences(Context context){return context.getSharedPreferences("audio-workspace",Context.MODE_PRIVATE);}
    static AudioState load(Context context){return load(context,true);}
    static AudioState load(Context context,boolean applyDefaults){
        AudioState state=new AudioState();SharedPreferences prefs=preferences(context);
        state.bitrate=64+(Math.max(64,Math.min(256,prefs.getInt("bitrate",128)))-64)/8*8;state.folder=prefs.getString("folder",null);
        try{
            JSONArray rows=new JSONArray(prefs.getString("selection","[]"));
            for(int n=0;n<rows.length();n++){JSONObject row=rows.getJSONObject(n);state.inputs.add(row.getString("uri"));state.names.add(row.getString("name"));}
        }catch(JSONException error){state.inputs.clear();state.names.clear();}
        try{
            JSONArray rows=new JSONArray(prefs.getString("lyrics","[]"));
            for(int n=0;n<rows.length();n++){JSONObject row=rows.getJSONObject(n);state.lyricsInputs.add(row.getString("uri"));state.lyricsNames.add(row.getString("name"));}
        }catch(JSONException error){state.lyricsInputs.clear();state.lyricsNames.clear();}
        state.selectedIndex=Math.max(0,Math.min(state.inputs.size()-1,prefs.getInt("selectedIndex",0)));
        state.onlySmaller=prefs.getBoolean("onlySmaller",true);state.keepScreenOn=prefs.getBoolean("keepScreenOn",false);state.lyricsExpanded=prefs.getBoolean("lyricsExpanded",false);
        state.readingNames=prefs.getBoolean("readingNames",false)&&!state.inputs.isEmpty();state.readingLyrics=prefs.getBoolean("readingLyrics",false)&&!state.lyricsInputs.isEmpty();
        state.settingsRevision=prefs.getLong("settingsRevision",0);
        AudioSettings defaults=new AudioSettings(context);if(applyDefaults&&defaults.revision!=state.settingsRevision)defaults.apply(state);
        return state;
    }
    static void save(Context context,AudioState state){
        JSONArray rows=new JSONArray(),lyrics=new JSONArray();
        try{for(int n=0;n<state.inputs.size();n++)rows.put(new JSONObject().put("uri",state.inputs.get(n)).put("name",state.names.get(n)));}
        catch(JSONException error){throw new IllegalStateException("Bozza audio non serializzabile",error);}
        try{for(int n=0;n<state.lyricsInputs.size();n++)lyrics.put(new JSONObject().put("uri",state.lyricsInputs.get(n)).put("name",state.lyricsNames.get(n)));}
        catch(JSONException error){throw new IllegalStateException("Testi audio non serializzabili",error);}
        preferences(context).edit().putLong("settingsRevision",state.settingsRevision).putInt("selectedIndex",state.selectedIndex).putBoolean("onlySmaller",state.onlySmaller).putBoolean("keepScreenOn",state.keepScreenOn).putBoolean("lyricsExpanded",state.lyricsExpanded).putBoolean("readingNames",state.readingNames).putBoolean("readingLyrics",state.readingLyrics).putString("lyrics",lyrics.toString()).putInt("bitrate",state.bitrate).putString("folder",state.folder).putString("selection",rows.toString()).apply();
    }
    static Bundle expansion(Context context,Bundle saved){
        if(saved!=null)return saved;
        Bundle result=new Bundle(),cards=new Bundle();SharedPreferences prefs=preferences(context);
        for(String title:new String[]{AudioScreen.FILES,AudioScreen.COMPRESSION,AudioScreen.CONVERSION,AudioScreen.DIAGNOSTICS})if(prefs.contains(title))cards.putBoolean(title,prefs.getBoolean(title,false));
        result.putBundle("cardExpansion",cards);return result;
    }
    static void saveExpansion(Context context,AppSections sections){
        SharedPreferences.Editor edit=preferences(context).edit();
        for(String title:sections.bodies.keySet())edit.putBoolean(title,sections.bodies.get(title).getVisibility()==android.view.View.VISIBLE);edit.apply();
    }
    private AudioDraftStore(){}
}
