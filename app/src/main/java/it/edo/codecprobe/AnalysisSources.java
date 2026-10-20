package it.edo.codecprobe;

import android.content.Context;
import org.json.*;
import java.util.*;

/** Selection references only. Listing never opens a media source or starts an inspector. */
final class AnalysisSources {
    static final class Entry {
        final String uri,name,workspace,known;
        Entry(String uri,String name,String workspace,String known){this.uri=uri;this.name=name;this.workspace=workspace;this.known=known;}
    }
    private static List<Entry> videos=Collections.emptyList();
    static synchronized void video(List<String> uris,List<String> names){
        List<Entry> next=new ArrayList<>();for(int i=0;i<uris.size();i++)next.add(new Entry(uris.get(i),i<names.size()?names.get(i):"Video","Video",""));videos=next;
    }
    static synchronized List<Entry> list(Context context){
        List<Entry> out=new ArrayList<>();
        for(Entry e:videos){VideoMetadataCache.Entry cached=VideoMetadataCache.get(e.uri);out.add(new Entry(e.uri,e.name,e.workspace,cached==null?"":cached.quick));}
        var audio=context.getSharedPreferences("audio-workspace",0);
        try{JSONArray rows=new JSONArray(audio.getString("selection","[]"));for(int i=0;i<Math.min(rows.length(),100);i++){JSONObject row=rows.getJSONObject(i);out.add(new Entry(row.getString("uri"),row.optString("name","Audio"),"Audio",""));}}catch(JSONException ignored){}
        var image=context.getSharedPreferences("image-workspace",0);
        try{JSONArray rows=new JSONArray(image.getString("selection","[]")),names=new JSONArray(image.getString("names","[]"));for(int i=0;i<Math.min(rows.length(),100);i++){String uri=rows.getString(i);ImageMetadataCache.Entry cached=ImageMetadataCache.memory.get(uri);String known=cached==null||cached.info==null?"":cached.info.width+" × "+cached.info.height+" pixel";out.add(new Entry(uri,names.optString(i,"Immagine"),"Immagini",known));}}catch(JSONException ignored){}
        return Collections.unmodifiableList(out);
    }
    private AnalysisSources(){}
}
