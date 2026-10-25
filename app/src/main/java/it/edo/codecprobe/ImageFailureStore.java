package it.edo.codecprobe;

import android.content.Context;
import java.io.*;
import org.json.*;

/** Append-only failure archive on private storage. Independent of the bounded on-screen history. */
final class ImageFailureStore {
    static final String FILE="image-failures.jsonl";
    static void append(Context context,long run,int index,String input,String folder,ImageOptions options,ImageRunState.Result result)throws IOException{
        append(context,run,index,input,folder,options,result,false);
    }
    static void append(Context context,long run,int index,String input,String folder,ImageOptions options,ImageRunState.Result result,boolean metadataOnly)throws IOException{
        try{
            JSONObject row=new JSONObject().put("timestamp",System.currentTimeMillis()).put("run",run).put("index",index)
                .put("input",input).put("name",result.name).put("folder",folder).put("message",result.message).put("details",result.details)
                .put("operation",metadataOnly?"metadata-check":"conversion").put("quality",options.quality).put("preset",options.preset).put("tune",options.tune).put("onlySmaller",options.onlySmaller).put("copyOriginalWhenLarger",options.copyOriginalWhenLarger);
            ImageLogStore.append(context,FILE,row+"\n");
        }catch(JSONException error){throw new IOException("Errore non archiviato",error);}
    }
    private ImageFailureStore(){}
}
