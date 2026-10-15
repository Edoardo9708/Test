package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** One isolated result flow shared by every folder chooser; never relaunches onResume. */
public final class FolderPickerActivity extends Activity {
    static final String ERROR="folderPickerError";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private boolean launched,processing;
    private Intent response;
    static File logFile(Context c){return new File(c.getFilesDir(),"folder-access-last.txt");}
    static synchronized void record(Context c,String text){
        try{File f=logFile(c);if(f.length()>1024*1024)try(FileOutputStream clear=new FileOutputStream(f)){clear.write("*** LOG PRECEDENTE RIDOTTO ***\n".getBytes(StandardCharsets.UTF_8));}
            try(FileOutputStream out=new FileOutputStream(f,true)){out.write(("["+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",java.util.Locale.ITALY).format(new java.util.Date())+"] "+text+"\n").getBytes(StandardCharsets.UTF_8));}
        }catch(IOException e){android.util.Log.e("FolderPicker","Scrittura log cartelle",e);}
    }
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);launched=saved!=null&&saved.getBoolean("launched");
        TextView message=new TextView(this);message.setText("Autorizzazione cartella\n\nScegli la cartella, poi premi Usa questa cartella e Consenti.\n\nAttendo la risposta del selettore Android…");message.setTextSize(18);message.setPadding(32,48,32,32);setContentView(message);
        record(this,"*** SELETTORE CARTELLA · v"+BuildConfig.VERSION_NAME+" ***\nActivity creata · ripristino="+(saved!=null)+" già_aperto="+launched);
        if(saved!=null){response=saved.getParcelable("response");if(response!=null){processResponse(response);return;}}
        if(!launched){launched=true;
            // The official minimal SAF request, without initial-location hints or task flags.
            Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            try{record(this,"1. APERTURA · ACTION_OPEN_DOCUMENT_TREE · nessun percorso iniziale · gestore="+picker.resolveActivity(getPackageManager()));startActivityForResult(picker,1);}
            catch(RuntimeException e){fail("Impossibile aprire il selettore: "+e);}
        }
    }
    @Override protected void onActivityResult(int request,int code,Intent data){
        super.onActivityResult(request,code,data);if(request!=1||processing)return;
        record(this,"2. RISPOSTA ANDROID · result="+code+" uri="+(data==null?null:data.getData())+" flags="+(data==null?0:data.getFlags()));
        if(code!=RESULT_OK||data==null||data.getData()==null){setResult(RESULT_CANCELED);record(this,"*** FINE SELETTORE · Android non ha restituito una cartella ***");finish();return;}
        response=new Intent(data);processResponse(response);
    }
    private void processResponse(Intent data){
        if(processing)return;
        processing=true;final Uri uri=data.getData();final int flags=data.getFlags();
        worker.execute(()->{
            try{
                if((flags&Intent.FLAG_GRANT_READ_URI_PERMISSION)==0||(flags&Intent.FLAG_GRANT_WRITE_URI_PERMISSION)==0)throw new SecurityException("Il selettore non ha concesso lettura e scrittura");
                record(this,"3. BEGIN · salvataggio permanente del permesso · "+uri);
                getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                record(this,"3. OK · permesso permanente salvato");
                Intent answer=new Intent(data);runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){setResult(RESULT_OK,answer);record(this,"4. OK · restituisco la cartella all’app e chiudo il selettore intermedio");finish();}});
            }catch(Exception e){runOnUiThread(()->fail("Permesso cartella non salvato: "+e));}
        });
    }
    private void fail(String message){record(this,"*** ERRORE SELETTORE ***\n"+message);setResult(RESULT_CANCELED,new Intent().putExtra(ERROR,message));finish();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putBoolean("launched",launched);if(response!=null)b.putParcelable("response",response);}
    @Override protected void onResume(){super.onResume();record(this,"EVENTO onResume · selettore_aperto="+launched+" risposta_in_elaborazione="+processing);}
    @Override protected void onPause(){record(this,"EVENTO onPause · in attesa del selettore o app in background");super.onPause();}
    @Override protected void onDestroy(){worker.shutdown();super.onDestroy();}
}
