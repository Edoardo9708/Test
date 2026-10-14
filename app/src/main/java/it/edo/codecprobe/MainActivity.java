package it.edo.codecprobe;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import java.util.Locale;
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
    private Button pickVideo,pickFolder,convert,stop,saveLog,saveMetadata,openVideo;
    private TextView videoLabel,folderLabel,qualityHelp,status,logView;
    private Choice quality,mode,cpuMode;
    private Switch bframeSwitch,lookaheadSwitch;
    private boolean notificationsRequested;
    private final Runnable sessionObserver=this::renderSession;
    private TextView inputInfo;
    private LinearLayout inputSummary,inputDetails;
    private volatile int infoGeneration;
    private EditText customCrf;
    private TextView timeline,telemetry,percentLabel,frameLabel;
    private int bg,surface,ink,muted,accent,border;
    
    private final Handler monitor=new Handler(Looper.getMainLooper());
    private long previousCpu,previousWall;
    private final Runnable sample=new Runnable(){public void run(){
        if(destroyed)return;
        long now=SystemClock.elapsedRealtime(),cpu=android.os.Process.getElapsedCpuTime();
        int cores=Runtime.getRuntime().availableProcessors();
        String usage=previousWall==0?"…":String.format(Locale.ITALY,"%.1f%%",Math.min(100,Math.max(0,100.0*(cpu-previousCpu)/Math.max(1,now-previousWall)/cores)));
        previousCpu=cpu;previousWall=now;
        String temp="non disponibile",thermal="non disponibile";
        try {
            Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(battery!=null&&battery.hasExtra(BatteryManager.EXTRA_TEMPERATURE))temp=String.format(Locale.ITALY,"%.1f °C",battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10.0);
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            String[] levels={"regolare","lieve","moderato","elevato","critico","emergenza","spegnimento"};
            int level=pm.getCurrentThermalStatus();if(level>=0&&level<levels.length)thermal=levels[level];
        }catch(RuntimeException ignored){}
        telemetry.setText("CPU APP   "+usage+"     •     BATTERIA   "+temp+"\nStato termico: "+thermal);
        monitor.postDelayed(this,1000);
    }};
    private ProgressBar progress;
    private volatile boolean destroyed,busy;
    private long lastUi;
    private String pendingLog;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        boolean dark=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        bg=Color.parseColor(dark?"#10151D":"#F2F5FA");surface=Color.parseColor(dark?"#1B2330":"#FFFFFF");
        ink=Color.parseColor(dark?"#EDF3FC":"#172236");muted=Color.parseColor(dark?"#A7B6CA":"#53647A");
        accent=Color.parseColor(dark?"#99C1FF":"#215CBD");border=Color.parseColor(dark?"#34445B":"#D7E1EF");
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        ScrollView scroll=new ScrollView(this);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setBackgroundColor(bg);scroll.setBackgroundColor(bg);
        int pad=(int)(16*getResources().getDisplayMetrics().density);
        page.setPadding(pad,pad,pad,pad);scroll.addView(page);
        page.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(pad+b.left,pad+b.top,pad+b.right,pad+b.bottom);
            } else v.setPadding(pad+insets.getSystemWindowInsetLeft(),pad+insets.getSystemWindowInsetTop(),pad+insets.getSystemWindowInsetRight(),pad+insets.getSystemWindowInsetBottom());
            return insets;
        });
        TextView title=text(page,"Converti video",30);title.setTypeface(null,Typeface.BOLD);
        text(page,"HEVC / x265   •   v"+BuildConfig.VERSION_NAME,13);
        text(page,"Meno spazio, qualità sotto controllo.",16);
        LinearLayout files=card(page,"01  FILE E DESTINAZIONE");
        pickVideo=button(files,"Seleziona video");videoLabel=text(files,"Nessun video selezionato",14);
        inputInfo=text(files,"Seleziona un video per vedere le informazioni del file.",15);
        inputSummary=new LinearLayout(this);inputSummary.setOrientation(LinearLayout.VERTICAL);files.addView(inputSummary);
        Button details=button(files,"Mostra metadati e dettagli");inputDetails=new LinearLayout(this);inputDetails.setOrientation(LinearLayout.VERTICAL);files.addView(inputDetails);inputDetails.setVisibility(View.GONE);
        details.setOnClickListener(v->{boolean show=inputDetails.getVisibility()!=View.VISIBLE;inputDetails.setVisibility(show?View.VISIBLE:View.GONE);details.setText(show?"Nascondi metadati":"Mostra metadati e dettagli");});
        pickFolder=button(files,"Scegli cartella");folderLabel=text(files,"Nessuna cartella selezionata",14);
        LinearLayout settings=card(page,"02  COMPRESSIONE");
        text(settings,"Modalità · velocità dell’encoder",17);
        mode=spinner(settings,new String[]{"Ultraveloce · ultrafast","Superveloce · superfast","Molto veloce · veryfast","Più veloce · faster","Veloce · fast","Media · medium","Lenta · slow","Più lenta · slower","Molto lenta · veryslow"});
        mode.setSelection(state==null?6:state.getInt("mode",6));
        text(settings,"Più lenta: cerca una compressione più efficiente, ma richiede più tempo.",13);
        text(settings,"Qualità del video",17);
        quality=spinner(settings,new String[]{"Alta qualità · CRF 18","Bilanciata · CRF 23","Risparmio spazio · CRF 28","Personalizzata · scrivi CRF 0–51"});
        customCrf=new EditText(this);customCrf.setSingleLine(true);customCrf.setInputType(InputType.TYPE_CLASS_NUMBER);
        customCrf.setTextColor(ink);customCrf.setHintTextColor(muted);customCrf.setTextSize(18);customCrf.setHint("Valore CRF da 0 a 51");
        customCrf.setBackgroundTintList(ColorStateList.valueOf(accent));settings.addView(customCrf);
        customCrf.setText(state==null?"23":state.getString("manual","23"));
        qualityHelp=text(settings,"",13);
        quality.onSelection=()->{customCrf.setVisibility(quality.getSelectedItemPosition()==3?View.VISIBLE:View.GONE);updateQualityHelp();};
        quality.setSelection(state==null?1:state.getInt("quality",1));
        customCrf.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){updateQualityHelp();}public void afterTextChanged(Editable e){}});
        bframeSwitch=new Switch(this);bframeSwitch.setText("B-frame");bframeSwitch.setTextColor(ink);bframeSwitch.setTextSize(17);bframeSwitch.setPadding(0,dp(12),0,dp(12));settings.addView(bframeSwitch);bframeSwitch.setChecked(state==null||state.getBoolean("bframes",true));
        lookaheadSwitch=new Switch(this);lookaheadSwitch.setText("Lookahead esteso");lookaheadSwitch.setTextColor(ink);lookaheadSwitch.setTextSize(17);lookaheadSwitch.setPadding(0,dp(12),0,dp(12));settings.addView(lookaheadSwitch);lookaheadSwitch.setChecked(state==null||state.getBoolean("lookahead",true));
        text(settings,"ON usa i valori del preset. Lookahead OFF con B-frame ON conserva solo il buffer minimo richiesto da x265.",13);
        text(settings,"Utilizzo CPU",17);
        cpuMode=spinner(settings,new String[]{"Limitato · configurazione precedente","Automatico · decide x265","Massimo · tutti i core disponibili"});
        cpuMode.setSelection(state==null?1:state.getInt("cpu",1));
        text(settings,"Massimo aumenta il parallelismo; l’uso al 100% dipende dal video e dal telefono.",13);
        LinearLayout work=card(page,"03  CONVERSIONE");
        convert=button(work,"Converti e salva MP4");styleButton(convert,true);
        stop=button(work,"Annulla conversione");stop.setVisibility(View.GONE);
        percentLabel=text(work,"0%",28);percentLabel.setTypeface(null,Typeface.BOLD);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);
        progress.setProgressTintList(ColorStateList.valueOf(accent));progress.setProgressBackgroundTintList(ColorStateList.valueOf(border));
        LinearLayout.LayoutParams bar=new LinearLayout.LayoutParams(-1,dp(12));bar.setMargins(0,dp(8),0,dp(12));work.addView(progress,bar);
        frameLabel=text(work,"Fotogrammi: 0 / —",18);
        timeline=text(work,"Video convertito: — s\nVideo rimanente: — s",16);
        status=text(work,"Scegli video, cartella e qualità.",14);
        telemetry=text(work,"CPU APP   …     •     BATTERIA   …",14);
        text(work,"CPU dell’app: 100% = tutti i core disponibili occupati. Temperatura della batteria, non del processore.",12);
        text(work,"Video SDR a 8 bit, audio AAC o assente. La conversione continua con notifica anche a schermo spento.",13);
        openVideo=button(work,"Apri video convertito");openVideo.setVisibility(View.GONE);
        LinearLayout logs=card(page,"DIAGNOSTICA");saveLog=button(logs,"Esporta log completo · TXT");
        saveMetadata=button(logs,"Esporta metadati originali · TXT");
        Button toggle=button(logs,"Mostra ultimi messaggi");
        logView=text(logs,"",12);logView.setTypeface(Typeface.MONOSPACE);logView.setTextIsSelectable(true);logView.setMaxLines(16);logView.setVisibility(View.GONE);
        toggle.setOnClickListener(v->{boolean show=logView.getVisibility()!=View.VISIBLE;logView.setVisibility(show?View.VISIBLE:View.GONE);toggle.setText(show?"Nascondi ultimi messaggi":"Mostra ultimi messaggi");});
        setContentView(scroll);page.requestApplyInsets();
        if(Build.VERSION.SDK_INT>=30)getWindow().getInsetsController().setSystemBarsAppearance(dark?0:WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        else getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        String savedFolder=getPreferences(0).getString("folder",null);
        if(savedFolder!=null)folder=Uri.parse(savedFolder);
        if(state!=null){
            if(state.getString("input")!=null)input=Uri.parse(state.getString("input"));
            sourceName=state.getString("sourceName","video");
            pendingLog=state.getString("pendingLog");
            if(state.getString("result")!=null)resultUri=Uri.parse(state.getString("result"));
        }
        refreshLabels();inspectInput();
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
        stop.setOnClickListener(v->{startService(new Intent(this,ConversionService.class).setAction(ConversionService.CANCEL));stop.setEnabled(false);});
        openVideo.setOnClickListener(v->{
            if(resultUri==null)return;
            try{startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(resultUri,"video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}
            catch(Exception e){toast("Nessun lettore video disponibile");}
        });
        saveLog.setOnClickListener(v->exportLog());
        saveMetadata.setOnClickListener(v->exportMetadata());
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
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),border);return d;}
    private LinearLayout card(LinearLayout page,String title){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(18));c.setBackground(shape(surface,22));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(16),0,0);page.addView(c,lp);
        TextView t=text(c,title,12);t.setTextColor(accent);t.setTypeface(null,Typeface.BOLD);return c;
    }
    private TextView text(LinearLayout p,String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextColor(size>=16?ink:muted);t.setTextSize(size);t.setPadding(0,dp(6),0,dp(6));p.addView(t);return t;}
    private void styleButton(Button b,boolean primary){
        int fill=primary?accent:surface;int fg=primary?bg:accent;
        b.setAllCaps(false);b.setTextSize(16);b.setTypeface(null,Typeface.BOLD);b.setMinHeight(dp(52));b.setPadding(dp(16),dp(10),dp(16),dp(10));
        b.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{muted,fg}));
        b.setBackgroundTintList(null);b.setBackground(new RippleDrawable(ColorStateList.valueOf(border),shape(fill,14),null));
    }
    private Button button(LinearLayout p,String s){Button b=new Button(this);b.setText(s);styleButton(b,false);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(4));p.addView(b,lp);return b;}
    private final class Choice extends Button {
        private final String[] entries;
        private int selected;
        private Runnable onSelection;
        Choice(String[] entries){
            super(MainActivity.this);this.entries=entries;styleButton(this,false);
            setGravity(Gravity.START|Gravity.CENTER_VERTICAL);setSingleLine(false);setMaxLines(Integer.MAX_VALUE);setEllipsize(null);setIncludeFontPadding(true);
            setOnClickListener(v->{
                ArrayAdapter<String> adapter=new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1,entries){
                    @Override public View getView(int position,View recycled,ViewGroup parent){
                        TextView t=new TextView(MainActivity.this);t.setText((position==selected?"✓  ":"")+entries[position]);t.setTextSize(17);t.setTextColor(ink);t.setBackgroundColor(surface);
                        t.setSingleLine(false);t.setEllipsize(null);t.setIncludeFontPadding(true);t.setMinHeight(dp(60));t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(20),dp(16),dp(20),dp(16));
                        t.setLayoutParams(new AbsListView.LayoutParams(-1,-2));return t;
                    }
                };
                android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(MainActivity.this).setAdapter(adapter,(d,position)->setSelection(position)).create();
                dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(shape(surface,18));
            });
            setSelection(0);
        }
        int getSelectedItemPosition(){return selected;}
        void setSelection(int n){selected=Math.max(0,Math.min(entries.length-1,n));setText(entries[selected]+"  ▾");if(onSelection!=null)onSelection.run();}
    }
    private Choice spinner(LinearLayout parent,String[] entries){Choice c=new Choice(entries);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(6));parent.addView(c,lp);return c;}
    private void inspectInput(){
        final Uri uri=input;final int generation=++infoGeneration;
        if(uri==null)return;
        inputInfo.setVisibility(View.VISIBLE);inputInfo.setText("Lettura informazioni del file…");inputSummary.removeAllViews();inputDetails.removeAllViews();
        worker.execute(()->{
            InputInspector.Report info=InputInspector.inspect(getApplicationContext(),uri,()->destroyed||generation!=infoGeneration);
            runOnUiThread(()->{if(!destroyed&&generation==infoGeneration){inputInfo.setVisibility(View.GONE);renderInfoSections(inputSummary,info.summarySections);renderInfoSections(inputDetails,info.detailSections);}});
        });
    }
    private void renderInfoSections(LinearLayout parent,java.util.Map<String,String> sections){
        parent.removeAllViews();
        for(java.util.Map.Entry<String,String> section:sections.entrySet()){
            LinearLayout group=new LinearLayout(this);group.setOrientation(LinearLayout.VERTICAL);group.setPadding(dp(14),dp(12),dp(14),dp(12));group.setBackground(shape(bg,14));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(10),0,0);parent.addView(group,lp);
            TextView title=text(group,section.getKey(),14);title.setTextColor(accent);title.setTypeface(null,Typeface.BOLD);
            TextView body=text(group,section.getValue(),15);body.setTextColor(ink);body.setLineSpacing(dp(3),1);body.setTextIsSelectable(true);
        }
    }
    private int selectedCrf(){int i=quality.getSelectedItemPosition();if(i<3)return new int[]{18,23,28}[Math.max(0,i)];try{return Integer.parseInt(customCrf.getText().toString());}catch(NumberFormatException e){return -1;}}
    private void updateQualityHelp(){if(qualityHelp==null)return;int n=selectedCrf();qualityHelp.setText(n<0||n>51?"Inserisci un numero intero da 0 a 51.":"CRF "+n+" · "+qualityName(n)+".\nNumero più basso = più qualità e generalmente più spazio. Il CRF non è una percentuale.");}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void refreshLabels(){
        videoLabel.setText(input==null?"Nessun video selezionato":sourceName);
        folderLabel.setText(folder==null?"Nessuna cartella selezionata":Uri.decode(folder.getLastPathSegment()));
    }
    private void setBusy(boolean value){
        busy=value;pickVideo.setEnabled(!value);pickFolder.setEnabled(!value);quality.setEnabled(!value);mode.setEnabled(!value);cpuMode.setEnabled(!value);customCrf.setEnabled(!value);bframeSwitch.setEnabled(!value);lookaheadSwitch.setEnabled(!value);
        convert.setEnabled(!value);saveLog.setEnabled(!value);saveMetadata.setEnabled(!value);stop.setVisibility(value?View.VISIBLE:View.GONE);stop.setEnabled(value);

    }
    private void startConversion(){
        if(input==null||folder==null){toast("Seleziona il video e la cartella di destinazione");return;}
        if(!OriginalMedia.hasLocationPermission(this)){
            new android.app.AlertDialog.Builder(this).setTitle("Conserva la posizione del video")
                .setMessage("Android può nascondere le coordinate salvate nei video. Il permesso serve a leggerle e copiarle: non rileva la tua posizione attuale. Senza il permesso la conversione non parte, per evitare di perdere il GPS.")
                .setPositiveButton("Continua",(d,w)->requestPermissions(new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION},5))
                .setNegativeButton("Annulla",null).show();return;
        }
        if(!OriginalMedia.hasVideoPermission(this)){
            new android.app.AlertDialog.Builder(this).setTitle("Accesso al video originale")
                .setMessage("Per aprire il video originale senza oscurare il GPS, Android richiede anche l’accesso ai video. Se scegli solo alcuni video, includi quello che vuoi convertire. Il permesso per la posizione, già concesso, non basta per leggere il file tramite MediaStore.")
                .setPositiveButton("Continua",(d,w)->requestPermissions(OriginalMedia.videoPermissions(),6))
                .setNegativeButton("Annulla",null).show();return;
        }
        final Uri source=input,dest=folder;final String name=sourceName;final int crf=selectedCrf();
        if(crf<0||crf>51){customCrf.setError("Inserisci un intero da 0 a 51");customCrf.requestFocus();return;}
        final int preset=mode.getSelectedItemPosition(),cpu=cpuMode.getSelectedItemPosition();
        if(Build.VERSION.SDK_INT>=33&&!notificationsRequested&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){notificationsRequested=true;requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},4);return;}
        Intent task=new Intent(this,ConversionService.class).setAction(ConversionService.START)
            .putExtra("input",source.toString()).putExtra("folder",dest.toString()).putExtra("name",name)
            .putExtra("crf",crf).putExtra("preset",preset).putExtra("cpu",cpu)
            .putExtra("bframes",bframeSwitch.isChecked()).putExtra("lookahead",lookaheadSwitch.isChecked());
        try{startForegroundService(task);setBusy(true);status.setText("Avvio conversione…");}
        catch(RuntimeException e){setBusy(false);status.setText("Avvio non riuscito: "+e.getMessage());}
    }
    private void renderSession(){
        if(destroyed)return;
        ConversionService.State s=ConversionService.snapshot();if(!s.hasSession)return;
        frameLabel.setText("Fotogrammi: "+s.doneFrames+" / "+(s.totalFrames>0?s.totalFrames:"— (totale non dichiarato)"));
        setBusy(s.running);progress.setProgress(s.percent);percentLabel.setText(s.percent+"%");status.setText(s.message);logView.setText(s.log);
        timeline.setText(String.format(Locale.ITALY,"Video convertito: %.1f s\nVideo rimanente: %s",s.done/1000000.0,s.total>0?String.format(Locale.ITALY,"%.1f s",Math.max(0,s.total-s.done)/1000000.0):"durata non disponibile"));
        resultUri=s.result;openVideo.setVisibility(resultUri==null?View.GONE:View.VISIBLE);
        if(input==null){input=s.input;folder=s.folder;sourceName=s.source;refreshLabels();inspectInput();}
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==8){
            if(OriginalMedia.hasLocationPermission(this)&&OriginalMedia.hasVideoPermission(this))exportMetadata();
            else toast("Per esportare i metadati originali, consenti posizione dei file e lettura del video selezionato.");
        }
        if(request==5){
            if(OriginalMedia.hasLocationPermission(this)){inspectInput();startConversion();}
            else{status.setText("Conversione non avviata: permesso posizione dei file negato.");toast("Per conservare il GPS, abilita il permesso nelle impostazioni dell’app e riprova.");}
        }
        if(request==6){
            if(OriginalMedia.hasVideoPermission(this)){inspectInput();startConversion();}
            else{status.setText("Conversione non avviata: accesso ai video negato.");toast("Consenti la lettura del video originale nelle impostazioni dell’app.");}
        }
        if(request==4){if(results.length==0||results[0]!=PackageManager.PERMISSION_GRANTED)toast("Notifiche disattivate: puoi abilitarle nelle impostazioni Android.");startConversion();}
    }
    private void exportMetadata(){
        if(input==null){toast("Seleziona prima il video da analizzare");return;}
        if(!OriginalMedia.hasLocationPermission(this)||!OriginalMedia.hasVideoPermission(this)){
            java.util.ArrayList<String> permissions=new java.util.ArrayList<>();
            permissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION);
            java.util.Collections.addAll(permissions,OriginalMedia.videoPermissions());
            requestPermissions(permissions.toArray(new String[0]),8);return;
        }
        final Uri selected=input;final String name=sourceName;saveMetadata.setEnabled(false);saveLog.setEnabled(false);
        toast("Leggo solo i blocchi dei metadati, senza copiare il video…");
        worker.execute(()->{
            File snapshot=null;
            try{
                Uri original=OriginalMedia.resolve(this,selected);
                snapshot=File.createTempFile("metadata-export-",".txt",getCacheDir());
                try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(original,"r")){
                    if(fd==null)throw new IOException("Video originale non disponibile");
                    try(ParcelFileDescriptor.AutoCloseInputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd);Writer out=new OutputStreamWriter(new FileOutputStream(snapshot),StandardCharsets.UTF_8)){
                        out.write("Versione app: "+BuildConfig.VERSION_NAME+"\nFile: "+name+"\n\n");
                        MetadataDump.write(in.getChannel(),out,()->destroyed);
                    }
                }
                final File result=snapshot;runOnUiThread(()->{
                    if(destroyed){result.delete();return;}
                    pendingLog=result.getAbsolutePath();saveMetadata.setEnabled(!busy);saveLog.setEnabled(!busy);
                    Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE);
                    i.putExtra(Intent.EXTRA_TITLE,"v"+BuildConfig.VERSION_NAME+"-CodecProbe-metadati-"+System.currentTimeMillis()+".txt");startActivityForResult(i,SAVE_LOG);
                });
            }catch(Exception e){if(snapshot!=null)snapshot.delete();runOnUiThread(()->{if(!destroyed){saveMetadata.setEnabled(!busy);saveLog.setEnabled(!busy);toast("Esportazione metadati non riuscita: "+e.getMessage());}});}
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
            refreshLabels();if(request==PICK_VIDEO)inspectInput();
        }else if(request==SAVE_LOG&&pendingLog!=null){
            final File snapshot=new File(pendingLog);pendingLog=null;
            worker.execute(()->{
                try(InputStream in=new FileInputStream(snapshot);OutputStream out=getContentResolver().openOutputStream(uri,"w")){
                    if(out==null)throw new IOException("Destinazione non disponibile");copy(in,out);
                    runOnUiThread(()->{if(!destroyed)toast("File TXT salvato");});
                }catch(Exception e){runOnUiThread(()->{if(!destroyed)toast("Errore esportazione log: "+e.getMessage());});}
                finally{snapshot.delete();}
            });
        }
    }
    @Override protected void onSaveInstanceState(Bundle b){
        super.onSaveInstanceState(b);
        if(input!=null)b.putString("input",input.toString());
        if(resultUri!=null)b.putString("result",resultUri.toString());
        b.putString("sourceName",sourceName);b.putString("pendingLog",pendingLog);b.putInt("quality",quality.getSelectedItemPosition());b.putInt("mode",mode.getSelectedItemPosition());b.putInt("cpu",cpuMode.getSelectedItemPosition());b.putBoolean("bframes",bframeSwitch.isChecked());b.putBoolean("lookahead",lookaheadSwitch.isChecked());b.putString("manual",customCrf.getText().toString());
    }
    @Override protected void onResume(){super.onResume();previousWall=0;monitor.post(sample);ConversionService.observe(sessionObserver);renderSession();}
    @Override protected void onPause(){monitor.removeCallbacks(sample);ConversionService.detach(sessionObserver);super.onPause();}
    @Override protected void onDestroy(){destroyed=true;ConversionService.detach(sessionObserver);worker.shutdown();super.onDestroy();}
}
