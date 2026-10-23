package it.edo.codecprobe;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.io.*;
import java.util.function.BooleanSupplier;

/** One compact latest outcome per original. No metadata payloads, images or log text in this DB. */
final class ImageProgressJournal extends SQLiteOpenHelper {
    static final String DATABASE="image-progress.db";
    ImageProgressJournal(Context context){super(context,DATABASE,null,2);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE progress (uri TEXT PRIMARY KEY, name TEXT NOT NULL, folder TEXT NOT NULL, settings TEXT NOT NULL, status TEXT NOT NULL, source_sha TEXT NOT NULL, output_uri TEXT NOT NULL, output_sha TEXT NOT NULL, input_bytes INTEGER NOT NULL, output_bytes INTEGER NOT NULL, updated INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX progress_status ON progress(status)");
        createInspectionTable(db);
    }
    private static void createInspectionTable(SQLiteDatabase db){db.execSQL("CREATE TABLE inspection_failures (uri TEXT PRIMARY KEY, name TEXT NOT NULL, updated INTEGER NOT NULL)");}
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){if(oldVersion==1&&newVersion==2)createInspectionTable(db);else throw new IllegalStateException("Versione registro immagini non supportata");}
    void inspection(String uri,ImageRunState.Result result){
        if(result.outcome==ImageRunState.Outcome.CANCELLED)return;
        if(result.outcome==ImageRunState.Outcome.FAILED){ContentValues row=new ContentValues();row.put("uri",uri);row.put("name",result.name);row.put("updated",System.currentTimeMillis());getWritableDatabase().replaceOrThrow("inspection_failures",null,row);}
        else getWritableDatabase().delete("inspection_failures","uri=?",new String[]{uri});
    }
    static String settings(ImageOptions options){return "heic-v1|"+options.quality+"|"+options.preset+"|"+options.tune+"|"+options.onlySmaller+"|"+options.folder;}
    void record(String uri,String folder,ImageOptions options,ImageRunState.Result result){
        if(result.outcome==ImageRunState.Outcome.CHECKED||result.outcome==ImageRunState.Outcome.CANCELLED||result.outcome==ImageRunState.Outcome.ALREADY_DONE)return;
        ContentValues row=new ContentValues();row.put("uri",uri);row.put("name",result.name);row.put("folder",folder);row.put("settings",settings(options));row.put("status",result.outcome.name());row.put("source_sha",result.sourceSha);row.put("output_uri",result.uri==null?"":result.uri);row.put("output_sha",result.outputSha);row.put("input_bytes",result.inputBytes);row.put("output_bytes",result.outputBytes);row.put("updated",System.currentTimeMillis());
        getWritableDatabase().replaceOrThrow("progress",null,row);
        if(result.outcome==ImageRunState.Outcome.SAVED||result.outcome==ImageRunState.Outcome.NOT_SMALLER)getWritableDatabase().delete("inspection_failures","uri=?",new String[]{uri});
    }
    ImageRunState.Result reusable(Context c,String input,String name,ImageOptions options,BooleanSupplier cancelled)throws IOException{
        String status,sourceSha,outputUri,outputSha;long before,after;
        try(Cursor row=getReadableDatabase().query("progress",null,"uri=? AND settings=?",new String[]{input,settings(options)},null,null,null)){
            if(!row.moveToFirst())return null;status=row.getString(row.getColumnIndexOrThrow("status"));
            if(!status.equals("SAVED")&&!status.equals("NOT_SMALLER"))return null;
            sourceSha=row.getString(row.getColumnIndexOrThrow("source_sha"));outputUri=row.getString(row.getColumnIndexOrThrow("output_uri"));outputSha=row.getString(row.getColumnIndexOrThrow("output_sha"));before=row.getLong(row.getColumnIndexOrThrow("input_bytes"));after=row.getLong(row.getColumnIndexOrThrow("output_bytes"));
        }
        if(sourceSha.length()!=64)return null;
        try{
            if(!sourceSha.equals(ImageFingerprint.uri(c,input,before,cancelled)))return null;
            if(status.equals("NOT_SMALLER"))return new ImageRunState.Result(name,before,after,null,"Già verificata: nessun risparmio",ImageRunState.Outcome.NOT_SMALLER,"Originale invariato e impostazioni uguali al registro.",sourceSha,"");
            if(outputUri.isEmpty()||outputSha.length()!=64||!outputSha.equals(ImageFingerprint.uri(c,outputUri,after,cancelled)))return null;
            return new ImageRunState.Result(name,before,after,outputUri,"Già convertita e verificata",ImageRunState.Outcome.ALREADY_DONE,"SHA-256 dell’originale e dell’HEIC confrontati con il registro; nessuna ricodifica.",sourceSha,outputSha);
        }catch(InterruptedIOException cancelledRead){throw cancelledRead;}catch(IOException|RuntimeException changed){return null;}
    }
    ImageQueueStore.Queue failures(){
        ImageQueueStore.Queue queue=new ImageQueueStore.Queue();
        try(Cursor rows=getReadableDatabase().rawQuery("SELECT uri,name,folder,updated FROM progress WHERE status='FAILED' UNION ALL SELECT uri,name,'' AS folder,updated FROM inspection_failures WHERE uri NOT IN (SELECT uri FROM progress WHERE status='FAILED') ORDER BY updated ASC",null)){
            while(rows.moveToNext()){
                if(Thread.currentThread().isInterrupted())throw new IllegalStateException("Lettura falliti interrotta");
                if(queue.inputs.size()>=ImageQueueStore.BULK_LIMIT)throw new IllegalStateException("Troppi falliti per una singola coda");
                queue.inputs.add(rows.getString(0));queue.names.add(rows.getString(1));queue.folders.add(rows.getString(2));
            }
        }return queue;
    }
}
