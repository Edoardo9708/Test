package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainActivity extends Activity {
    private static final int PICK_VIDEO=1,PICK_FOLDER=2,SAVE_LOG=3;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancelled=new AtomicBoolean();
    private final ArrayDeque<String> recent=new ArrayDeque<>();
    private Uri input,folder,resultUri;
    private String sourceName="video";
    private Button pickVideo,pickFolder,convert,stop,saveLog,openVideo;
    private TextView videoLabel,folderLabel,qualityHelp,status,logView;
    private Spinner quality;
    private ProgressBar progress;
    private volatile boolean destroyed,busy;
    private long lastUi;
    private String pendingLog;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll=new ScrollView(this);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(16*getResources().getDisplayMetrics().density);
        page.setPadding(pad,pad,pad,pad);scroll.addView(page);
        page.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(pad+b.left,pad+b.top,pad+b.right,pad+b.bottom);
            } else v.setPadding(pad+insets.getSystemWindowInsetLeft(),pad+insets.getSystemWindowInsetTop(),pad+insets.getSystemWindowInsetRight(),pad+insets.getSystemWindowInsetBottom());
            return insets;
        });
        text(page,"Converti video · v0.7",24);
        text(page,"Video HEVC (H.265) con qualità CRF. Conserva risoluzione, tempi e orientamento. Audio AAC copiato senza ricodifica.",15);
        text(page,"Prima versione: video SDR a 8 bit, fino a 4K. Per iniziare scegli un filmato breve. Tieni aperta l’app durante la conversione.",14);
        pickVideo=button(page,"1. Seleziona video");
        videoLabel=text(page,"Nessun video selezionato",14);
        pickFolder=button(page,"2. Scegli cartella di destinazione");
        folderLabel=text(page,"Nessuna cartella selezionata",14);
        text(page,"3. Qualità del video",18);
        quality=new Spinner(this);
        String[] values=new String[52];
        for(int i=0;i<=51;i++)values[i]="CRF "+i+" · "+qualityName(i);
        quality.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));
        page.addView(quality);
        qualityHelp=text(page,"",14);
        quality.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> parent){}
            public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){
                qualityHelp.setText("CRF "+position+": "+qualityName(position)+".\nNumero più basso → più dettagli e file generalmente più grande.\nNumero più alto → più compressione e maggiori perdite visive.\nNon è una percentuale: qualità e dimensioni dipendono dal video."+ (position==0?"\nCRF 0 non è la modalità senza perdita.":""));
            }
        });
        quality.setSelection(state==null?23:state.getInt("crf",23));
        convert=button(page,"Converti e salva MP4");
        stop=button(page,"Annulla conversione");stop.setVisibility(View.GONE);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);page.addView(progress);
        status=text(page,"Scegli video, cartella e qualità.",15);
        openVideo=button(page,"Apri video convertito");openVideo.setVisibility(View.GONE);
        saveLog=button(page,"Salva log completo (.txt)");
        text(page,"Ultimi messaggi · il TXT contiene il log completo",14);
        logView=text(page,"",12);logView.setTextIsSelectable(true);
        logView.setMaxLines(16);
        setContentView(scroll);page.requestApplyInsets();
        String savedFolder=getPreferences(0).getString("folder",null);
        if(savedFolder!=null)folder=Uri.parse(savedFolder);
        if(state!=null){
            if(state.getString("input")!=null)input=Uri.parse(state.getString("input"));
            sourceName=state.getString("sourceName","video");
            pendingLog=state.getString("pendingLog");
            if(state.getString("result")!=null)resultUri=Uri.parse(state.getString("result"));
        }
        refreshLabels();
        if(resultUri!=null)openVideo.setVisibility(View.VISIBLE);
        File old=VideoConverter.logFile(this);
        if(old.isFile())worker.execute(()->{
            try(RandomAccessFile f=new RandomAccessFile(old,"r")){
                int size=(int)Math.min(6000,f.length());byte[] bytes=new byte[size];f.seek(f.length()-size);f.readFully(bytes);
                String tail=new String(bytes,StandardCharsets.UTF_8);
                runOnUiThread(()->{if(!destroyed&&!busy)logView.setText(tail);});
            }catch(IOException ignored){}
        });
        pickVideo.setOnClickListener(v->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("video/*");i.addCategory(Intent.CATEGORY_OPENABLE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i,PICK_VIDEO);
        });
        pickFolder.setOnClickListener(v->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i,PICK_FOLDER);
        });
        convert.setOnClickListener(v->startConversion());
        stop.setOnClickListener(v->{cancelled.set(true);stop.setEnabled(false);status.setText("Annullamento alla fine della chiamata corrente...");});
        openVideo.setOnClickListener(v->{
            if(resultUri==null)return;
            try{startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(resultUri,"video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}
            catch(Exception e){toast("Nessun lettore video disponibile");}
        });
        saveLog.setOnClickListener(v->exportLog());
    }
    private static String qualityName(int crf){
        if(crf==0)return "Qualità massima CRF, non senza perdita";
        if(crf<=17)return "Qualità molto alta · file più grandi";
        if(crf<=22)return "Qualità alta";
        if(crf<=27)return "Compromesso qualità / dimensioni";
        if(crf<=32)return "Più compressione · dettagli ridotti";
        if(crf<=40)return "Qualità bassa · perdite evidenti";
        return "Compressione molto forte · qualità molto bassa";
    }
    private TextView text(LinearLayout p,String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);p.addView(t);return t;}
    private Button button(LinearLayout p,String s){Button b=new Button(this);b.setText(s);p.addView(b);return b;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void refreshLabels(){
        videoLabel.setText(input==null?"Nessun video selezionato":sourceName);
        folderLabel.setText(folder==null?"Nessuna cartella selezionata":Uri.decode(folder.getLastPathSegment()));
    }
    private void setBusy(boolean value){
        busy=value;pickVideo.setEnabled(!value);pickFolder.setEnabled(!value);quality.setEnabled(!value);
        convert.setEnabled(!value);saveLog.setEnabled(!value);stop.setVisibility(value?View.VISIBLE:View.GONE);stop.setEnabled(value);
        if(value)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void startConversion(){
        if(input==null||folder==null){toast("Seleziona il video e la cartella di destinazione");return;}
        final Uri source=input,dest=folder;final String name=sourceName;final int crf=quality.getSelectedItemPosition();
        cancelled.set(false);recent.clear();lastUi=0;logView.setText("");progress.setProgress(0);resultUri=null;openVideo.setVisibility(View.GONE);
        setBusy(true);status.setText("Analisi video...");
        worker.execute(()->{
            try{
                VideoConverter converter=new VideoConverter(getApplicationContext(),cancelled,new VideoConverter.Listener(){
                    public void log(String s){
                        recent.addLast(s);while(recent.size()>14)recent.removeFirst();
                        long now=SystemClock.elapsedRealtime();
                        if(now-lastUi>250||s.contains("***")||s.contains("SESSION END")){
                            lastUi=now;String snapshot=String.join("\n",recent);
                            runOnUiThread(()->{if(!destroyed)logView.setText(snapshot);});
                        }
                    }
                    public void progress(int percent,String s){runOnUiThread(()->{if(!destroyed&&!cancelled.get()){progress.setProgress(percent);status.setText(s);}});}
                });
                Uri result=converter.convert(source,dest,crf,name);
                runOnUiThread(()->{if(!destroyed){resultUri=result;openVideo.setVisibility(View.VISIBLE);}});
            }catch(Exception e){
                runOnUiThread(()->{if(!destroyed)status.setText(cancelled.get()?"Conversione annullata.":"Conversione fallita: "+e.getMessage()+"\nSalva il log per i dettagli.");});
            }finally{runOnUiThread(()->{if(!destroyed)setBusy(false);});}
        });
    }
    private void exportLog(){
        File source=VideoConverter.logFile(this);
        if(!source.isFile()){toast("Nessun log di conversione disponibile");return;}
        saveLog.setEnabled(false);
        worker.execute(()->{
            try{
                File snapshot=File.createTempFile("log-export-",".txt",getCacheDir());
                try(InputStream in=new FileInputStream(source);OutputStream out=new FileOutputStream(snapshot)){copy(in,out);}
                runOnUiThread(()->{
                    if(destroyed)return;
                    pendingLog=snapshot.getAbsolutePath();saveLog.setEnabled(true);
                    Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE);
                    i.putExtra(Intent.EXTRA_TITLE,"CodecProbe-log-"+System.currentTimeMillis()+".txt");startActivityForResult(i,SAVE_LOG);
                });
            }catch(Exception e){runOnUiThread(()->{if(!destroyed){saveLog.setEnabled(true);toast("Errore: "+e.getMessage());}});}
        });
    }
    private static void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
    @Override protected void onActivityResult(int request,int code,Intent data){
        super.onActivityResult(request,code,data);
        if(code!=RESULT_OK||data==null||data.getData()==null){
            if(request==SAVE_LOG&&pendingLog!=null){new File(pendingLog).delete();pendingLog=null;}return;
        }
        Uri uri=data.getData();
        if(request==PICK_VIDEO||request==PICK_FOLDER){
            int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            boolean persisted=false;
            try{
                if((flags & Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0 && (flags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION)!=0) {
                    getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    persisted=true;
                } else if((flags & Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0) {
                    getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    persisted=true;
                } else if((flags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION)!=0) {
                    getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    persisted=true;
                }
            }catch(SecurityException ignored){}
            if(request==PICK_VIDEO){
                input=uri;sourceName="video";
                try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
                    if(c!=null&&c.moveToFirst()&&!c.isNull(0))sourceName=c.getString(0);
                }catch(Exception ignored){}
            }else{
                folder=uri;
                if(persisted)getPreferences(0).edit().putString("folder",uri.toString()).apply();
            }
            refreshLabels();
        }else if(request==SAVE_LOG&&pendingLog!=null){
            final File snapshot=new File(pendingLog);pendingLog=null;
            worker.execute(()->{
                try(InputStream in=new FileInputStream(snapshot);OutputStream out=getContentResolver().openOutputStream(uri,"w")){
                    if(out==null)throw new IOException("Destinazione non disponibile");copy(in,out);
                    runOnUiThread(()->{if(!destroyed)toast("Log TXT salvato");});
                }catch(Exception e){runOnUiThread(()->{if(!destroyed)toast("Errore esportazione log: "+e.getMessage());});}
                finally{snapshot.delete();}
            });
        }
    }
    @Override protected void onSaveInstanceState(Bundle b){
        super.onSaveInstanceState(b);
        if(input!=null)b.putString("input",input.toString());
        if(resultUri!=null)b.putString("result",resultUri.toString());
        b.putString("sourceName",sourceName);b.putString("pendingLog",pendingLog);b.putInt("crf",quality.getSelectedItemPosition());
    }
    @Override protected void onDestroy(){destroyed=true;cancelled.set(true);worker.shutdown();super.onDestroy();}
}
