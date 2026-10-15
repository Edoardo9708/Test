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
    private static final int PICK_VIDEO=1,PICK_FOLDER=2,SAVE_LOG=3,PICK_ORIGIN=7,PICK_ALL_FOLDER=10;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancelled=new AtomicBoolean();
    private final ArrayDeque<String> recent=new ArrayDeque<>();
    private Uri input,folder,resultUri;
    private final java.util.ArrayList<String> selectedInputs=new java.util.ArrayList<>(),selectedNames=new java.util.ArrayList<>(),originFolders=new java.util.ArrayList<>();
    private final java.util.ArrayList<String> destinationNotes=new java.util.ArrayList<>();
    private OriginDirectoryPanel originPanel;
    private Button previousVideo,nextVideo;
    private TextView videoNumber;
    private LinearLayout videoNavigator;
    private int selectedVideoIndex;
    private boolean originsReady;
    private String pendingOriginParent,pendingOriginSource;
    private final DestinationPlan destinationPlan=new DestinationPlan();
    private CheckBox videoOrigin,allOrigin,applyAllDestinations;
    private Button pickAllFolder;
    private TextView videoDestinationTitle,videoDestinationHelp,allFolderLabel;
    private boolean updatingDestinationUi;
    private String pendingFixedFolder;
    private boolean pendingAuthorizationOrigin;
    private TextView queueLabel;
    private String loadedDefault;
    private boolean choosingOrigins;
    private String sourceName="video";
    private Button pickVideo,pickFolder,convert,stop,saveLog,saveMetadata,openVideo,hdrTest,saveHdr;
    private boolean hdrTesting;
    private TextView videoLabel,folderLabel,qualityHelp,status,logView;
    private Choice cpuMode;
    private SeekBar presetSlider,crfSlider;
    private TextView presetValue;
    private final java.util.List<Button> crfButtons=new java.util.ArrayList<>();
    private long settingsRevision;
    private Switch bframeSwitch,lookaheadSwitch;
    private CheckBox keepScreenOn;
    private boolean conversionScreenVisible;
    private TextView temporalHelp,effectiveTemporal,aqModeLabel,aqStrengthLabel,cuTreeLabel;
    private static native int[] nativePresetSettings(int preset,boolean bframes,boolean lookahead);
    private boolean notificationsRequested;
    private final Runnable sessionObserver=this::renderSession;
    private TextView inputInfo;
    private LinearLayout inputSummary,inputDetails,videoDetails;
    private int detailGeneration=-1;
    private volatile int infoGeneration;

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
        AppSettings defaults=new AppSettings(this);settingsRevision=defaults.revision;
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
        Button preferences=button(page,"⚙ Impostazioni");preferences.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class).putExtra("preset",presetSlider.getProgress()).putExtra("crf",selectedCrf()).putExtra("cpu",cpuMode.getSelectedItemPosition()).putExtra("bframes",bframeSwitch.isChecked()).putExtra("lookahead",lookaheadSwitch.isChecked()).putExtra("sourceName",sourceName)));

        LinearLayout files=card(page,"01  FILE E DESTINAZIONE");
        pickVideo=button(files,"Seleziona uno o più video");videoLabel=text(files,"Nessun video selezionato",23);videoLabel.setTypeface(null,Typeface.BOLD);
        videoNavigator=new LinearLayout(this);videoNavigator.setGravity(Gravity.CENTER_VERTICAL);files.addView(videoNavigator);
        previousVideo=new Button(this);previousVideo.setText("‹");previousVideo.setContentDescription("Video precedente");styleButton(previousVideo,false);videoNavigator.addView(previousVideo,new LinearLayout.LayoutParams(dp(56),-2));
        videoNumber=new TextView(this);videoNumber.setTextColor(ink);videoNumber.setTextSize(18);videoNumber.setGravity(Gravity.CENTER);videoNavigator.addView(videoNumber,new LinearLayout.LayoutParams(0,-2,1));
        nextVideo=new Button(this);nextVideo.setText("›");nextVideo.setContentDescription("Video successivo");styleButton(nextVideo,false);videoNavigator.addView(nextVideo,new LinearLayout.LayoutParams(dp(56),-2));
        previousVideo.setOnClickListener(v->selectVideo(selectedVideoIndex-1));nextVideo.setOnClickListener(v->selectVideo(selectedVideoIndex+1));
        videoNumber.setOnClickListener(v->{if(selectedNames.isEmpty())return;String[] entries=new String[selectedNames.size()];for(int n=0;n<entries.length;n++)entries[n]=(n+1)+". "+selectedNames.get(n);new android.app.AlertDialog.Builder(this).setTitle("Mostra dettagli del video").setSingleChoiceItems(entries,selectedVideoIndex,(d,n)->{selectVideo(n);d.dismiss();}).setNegativeButton("Chiudi",null).show();});
        android.view.GestureDetector swipeVideos=new android.view.GestureDetector(this,new android.view.GestureDetector.SimpleOnGestureListener(){@Override public boolean onDown(android.view.MotionEvent e){return true;}@Override public boolean onFling(android.view.MotionEvent first,android.view.MotionEvent last,float x,float y){if(first==null||last==null||Math.abs(last.getX()-first.getX())<dp(48)||Math.abs(x)<=Math.abs(y))return false;selectVideo(selectedVideoIndex+(last.getX()<first.getX()?1:-1));return true;}@Override public boolean onSingleTapUp(android.view.MotionEvent e){videoNumber.performClick();return true;}});
        videoNumber.setOnTouchListener((v,e)->swipeVideos.onTouchEvent(e));
        inputInfo=text(files,"Seleziona un video",15);
        Button details=button(files,"▸ Dettagli video");
        videoDetails=new LinearLayout(this);videoDetails.setOrientation(LinearLayout.VERTICAL);files.addView(videoDetails);videoDetails.setVisibility(View.GONE);
        inputSummary=new LinearLayout(this);inputSummary.setOrientation(LinearLayout.VERTICAL);videoDetails.addView(inputSummary);
        inputDetails=new LinearLayout(this);inputDetails.setOrientation(LinearLayout.VERTICAL);videoDetails.addView(inputDetails);
        details.setOnClickListener(v->{boolean show=videoDetails.getVisibility()!=View.VISIBLE;videoDetails.setVisibility(show?View.VISIBLE:View.GONE);details.setText(show?"▾ Dettagli video":"▸ Dettagli video");if(show)loadInputDetails();});
        divider(files);
        videoDestinationTitle=text(files,"DESTINAZIONE DEL VIDEO",16);videoDestinationTitle.setTypeface(null,Typeface.BOLD);
        videoDestinationHelp=text(files,"Vale solo per il video visualizzato.",14);
        videoOrigin=destinationCheck(files,"Salva nella cartella di origine");
        pickFolder=button(files,"Scegli cartella per questo video…");folderLabel=text(files,"Nessuna cartella scelta",14);
        videoOrigin.setOnCheckedChangeListener((v,checked)->{if(!updatingDestinationUi&&!selectedInputs.isEmpty()){destinationPlan.files.get(selectedVideoIndex).origin=checked;destinationsChanged();}});
        divider(files);
        text(files,"DESTINAZIONE PER TUTTA LA CODA",16).setTypeface(null,Typeface.BOLD);
        applyAllDestinations=destinationCheck(files,"Applica a tutti i video");
        text(files,"Questa regola sostituisce temporaneamente le scelte individuali. Togliendo la spunta, le ritrovi come prima.",14);
        allOrigin=destinationCheck(files,"Salva nella cartella di origine");
        pickAllFolder=button(files,"Scegli una cartella per tutti i video…");allFolderLabel=text(files,"Nessuna cartella scelta",14);
        text(files,"Origine: ciascun video nella propria directory. Cartella scelta: la stessa per tutta la coda.",13);
        applyAllDestinations.setOnCheckedChangeListener((v,checked)->{if(!updatingDestinationUi){destinationPlan.applyAll=checked;destinationsChanged();}});
        allOrigin.setOnCheckedChangeListener((v,checked)->{if(!updatingDestinationUi){destinationPlan.all.origin=checked;destinationsChanged();}});
        pickAllFolder.setOnClickListener(v->launchFolderPicker(PICK_ALL_FOLDER,null));
        divider(files);
        text(files,"CARTELLE UTILIZZATE",16).setTypeface(null,Typeface.BOLD);
        text(files,"Destinazioni attive per tutta la coda. Verde: accesso verificato. Rosso: cartella da scegliere o autorizzare.",14);
        LinearLayout originRows=new LinearLayout(this);originRows.setOrientation(LinearLayout.VERTICAL);files.addView(originRows);
        originPanel=new OriginDirectoryPanel(this,originRows,new OriginDirectoryPanel.Listener(){
            public void authorize(String parent,String representative,boolean origin,String requestedFolder){requestDirectoryAccess(parent,representative,origin,requestedFolder);}
            public void changed(boolean ready,java.util.ArrayList<String> destinations,java.util.ArrayList<String> notes){originsReady=ready;originFolders.clear();originFolders.addAll(destinations);destinationNotes.clear();destinationNotes.addAll(notes);setBusy(busy);}
            public void error(String message){FolderPickerActivity.record(MainActivity.this,"ERRORE · verifica cartella · "+message);toast("Cartella: "+message);}
        },ink,dark);
        LinearLayout settings=card(page,"02  COMPRESSIONE");
        presetValue=parameterRow(settings,"Preset","Il preset regola velocità e compressione. I preset lenti provano più alternative; osserva B-frame, lookahead e AQ mentre sposti lo slider.");
        presetSlider=new SeekBar(this);presetSlider.setMax(8);styleSlider(presetSlider);settings.addView(presetSlider,new LinearLayout.LayoutParams(-1,dp(48)));
        presetSlider.setProgress(state==null?defaults.preset:state.getInt("presetValue",defaults.preset));
        qualityHelp=parameterRow(settings,"CRF","Valori da 0 a 51. Un numero più basso conserva generalmente più dettagli e produce file più grandi. CRF 0 non è lossless. I pulsanti 18, 23 e 28 selezionano i valori classici; ✎ permette di digitare un numero.");
        crfSlider=new SeekBar(this);crfSlider.setMax(51);styleSlider(crfSlider);settings.addView(crfSlider,new LinearLayout.LayoutParams(-1,dp(48)));
        crfSlider.setProgress(state==null?defaults.crf:state.getInt("crfValue",defaults.crf));
        crfSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int n,boolean user){updateQualityHelp();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        LinearLayout crfRow=new LinearLayout(this);settings.addView(crfRow);
        for(int n:new int[]{18,23,28}){Button quick=new Button(this);quick.setText(Integer.toString(n));quick.setContentDescription("CRF "+n);styleButton(quick,false);quick.setOnClickListener(v->crfSlider.setProgress(n));crfRow.addView(quick,new LinearLayout.LayoutParams(0,-2,1));crfButtons.add(quick);}
        Button manual=new Button(this);manual.setText("✎");manual.setContentDescription("Inserisci CRF manualmente");styleButton(manual,false);crfRow.addView(manual,new LinearLayout.LayoutParams(0,-2,1));crfButtons.add(manual);
        manual.setOnClickListener(v->{EditText value=new EditText(this);value.setInputType(InputType.TYPE_CLASS_NUMBER);value.setText(Integer.toString(selectedCrf()));value.setSelectAllOnFocus(true);android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("CRF · 0–51").setView(value).setPositiveButton("Applica",null).setNegativeButton("Annulla",null).create();dialog.setOnShowListener(d->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{try{int n=Integer.parseInt(value.getText().toString());if(n<0||n>51)throw new NumberFormatException();crfSlider.setProgress(n);dialog.dismiss();}catch(NumberFormatException e){value.setError("Inserisci un intero da 0 a 51");}}));dialog.show();});
        updateQualityHelp();
        bframeSwitch=new Switch(this);bframeSwitch.setText("B-frame");bframeSwitch.setTextColor(ink);bframeSwitch.setTextSize(20);bframeSwitch.setTypeface(null,Typeface.BOLD);bframeSwitch.setPadding(0,dp(12),0,dp(12));switchRow(settings,bframeSwitch,"B-frame","Fotogrammi che possono usare riferimenti precedenti e successivi. Il numero visualizzato è il massimo consecutivo; x265 può usarne meno in base alla scena. OFF imposta il massimo a zero.");bframeSwitch.setChecked(state==null?defaults.bframes:state.getBoolean("bframes",defaults.bframes));
        lookaheadSwitch=new Switch(this);lookaheadSwitch.setText("Lookahead esteso");lookaheadSwitch.setTextColor(ink);lookaheadSwitch.setTextSize(20);lookaheadSwitch.setTypeface(null,Typeface.BOLD);lookaheadSwitch.setPadding(0,dp(12),0,dp(12));switchRow(settings,lookaheadSwitch,"Lookahead","Numero di fotogrammi futuri analizzati per pianificare la codifica. ON usa il preset; OFF disattiva l’analisi estesa e CU-tree, mantenendo l’eventuale buffer minimo dei B-frame.");lookaheadSwitch.setChecked(state==null?defaults.lookahead:state.getBoolean("lookahead",defaults.lookahead));
        temporalHelp=text(settings,"",13);temporalHelp.setVisibility(View.GONE);
        cuTreeLabel=parameterRow(settings,"CU-tree","Usa il lookahead per valutare quanto una zona verrà riutilizzata nei fotogrammi successivi e distribuire i bit. Segue il preset; viene disattivato con Lookahead OFF.");
        aqModeLabel=parameterRow(settings,"AQ mode","Adaptive Quantization: distribuisce la compressione tra le zone dello stesso fotogramma. 0: disattivata. 1: AQ basata sulla variabilità. 2: auto-variance. 3: auto-variance con attenzione alle zone scure. 4: auto-variance con informazioni sui bordi. Qui segue il preset x265.");
        aqStrengthLabel=parameterRow(settings,"AQ strength","Intensità dell’Adaptive Quantization. Segue il valore del preset; 0 disattiva AQ. Un valore maggiore non garantisce qualità migliore o file più piccoli.");
        presetSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int n,boolean user){updateTemporalHelp();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        bframeSwitch.setOnCheckedChangeListener((v,checked)->updateTemporalHelp());
        lookaheadSwitch.setOnCheckedChangeListener((v,checked)->updateTemporalHelp());
        updateTemporalHelp();

        parameterRow(settings,"CPU","Limited limita il numero di thread. Auto lascia decidere x265. Max aumenta il parallelismo in base ai core disponibili. Non garantisce il 100% di utilizzo: dipende dal video, dal preset e dai limiti del telefono.");
        cpuMode=spinner(settings,new String[]{"Limited","Auto","Max"});
        cpuMode.setSelection(state==null?defaults.cpu:state.getInt("cpu",defaults.cpu));

        LinearLayout work=card(page,"03  CONVERSIONE");
        convert=button(work,"Converti e salva MP4");styleButton(convert,true);
        stop=button(work,"Annulla conversione");stop.setVisibility(View.GONE);
        keepScreenOn=new CheckBox(this);keepScreenOn.setText("Mantieni schermo acceso");keepScreenOn.setTextColor(ink);keepScreenOn.setTextSize(17);keepScreenOn.setButtonTintList(ColorStateList.valueOf(accent));keepScreenOn.setMinHeight(dp(48));LinearLayout screenRow=new LinearLayout(this);screenRow.setGravity(Gravity.CENTER_VERTICAL);work.addView(screenRow);screenRow.addView(keepScreenOn,new LinearLayout.LayoutParams(0,-2,1));
        keepScreenOn.setChecked(defaults.keepScreenOn);
        keepScreenOn.setOnCheckedChangeListener((v,checked)->{AppSettings.setKeepScreenOn(this,checked);updateScreenAwake();});
        infoButton(screenRow,"Mantieni schermo acceso","Evita lo spegnimento automatico mentre la conversione è in corso e questa schermata è visibile. Si disattiva al termine o uscendo dalla schermata. Puoi spegnere lo schermo con il tasto del telefono. La scelta viene salvata per gli avvii successivi; non garantisce una velocità di codifica maggiore.");
        queueLabel=text(work,"Nessuna coda avviata",16);
        percentLabel=text(work,"0%",28);percentLabel.setTypeface(null,Typeface.BOLD);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);
        progress.setProgressTintList(ColorStateList.valueOf(accent));progress.setProgressBackgroundTintList(ColorStateList.valueOf(border));
        LinearLayout.LayoutParams bar=new LinearLayout.LayoutParams(-1,dp(12));bar.setMargins(0,dp(8),0,dp(12));work.addView(progress,bar);
        frameLabel=text(work,"Fotogrammi: 0 / —",18);
        effectiveTemporal=text(work,"Parametri effettivi: disponibili all’avvio dell’encoder.",14);
        timeline=text(work,"Video convertito: — s\nVideo rimanente: — s",16);
        status=text(work,"Scegli video, cartella e qualità.",14);
        telemetry=text(work,"CPU APP   …     •     BATTERIA   …",14);
        parameterRow(work,"ⓘ Prestazioni","CPU app: 100% indica tutti i core disponibili occupati. La temperatura è della batteria, non della CPU. La conversione usa un servizio con notifica e un wake lock, ma Android può applicare limiti energetici. Supportati video SDR a 8 bit e audio AAC o assente.");
        openVideo=button(work,"Apri video convertito");openVideo.setVisibility(View.GONE);
        LinearLayout logs=card(page,"DIAGNOSTICA");saveLog=button(logs,"Esporta log completo · TXT");
        saveMetadata=button(logs,"Esporta metadati originali · TXT");
        hdrTest=button(logs,"Test HDR · 10 bit (sperimentale)");hdrTest.setOnClickListener(v->startHdrTest());
        saveHdr=button(logs,"Esporta log HDR · TXT");saveHdr.setOnClickListener(v->exportLog(HdrProbe.logFile(this),"CodecProbe-HDR-log-"));
        Button toggle=button(logs,"Mostra ultimi messaggi");
        logView=text(logs,"",12);logView.setTypeface(Typeface.MONOSPACE);logView.setTextIsSelectable(true);logView.setMaxLines(16);logView.setVisibility(View.GONE);
        toggle.setOnClickListener(v->{boolean show=logView.getVisibility()!=View.VISIBLE;logView.setVisibility(show?View.VISIBLE:View.GONE);toggle.setText(show?"Nascondi ultimi messaggi":"Mostra ultimi messaggi");});
        setContentView(scroll);page.requestApplyInsets();
        if(Build.VERSION.SDK_INT>=30)getWindow().getInsetsController().setSystemBarsAppearance(dark?0:WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        else getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        loadedDefault=defaults.defaultFolder;
        String savedFolder=loadedDefault!=null?loadedDefault:getPreferences(0).getString("folder",null);
        if(savedFolder!=null)folder=Uri.parse(savedFolder);
        if(state!=null){
            if(state.getString("input")!=null)input=Uri.parse(state.getString("input"));
            sourceName=state.getString("sourceName","video");
            if(state.getStringArrayList("inputs")!=null){selectedInputs.addAll(state.getStringArrayList("inputs"));selectedNames.addAll(state.getStringArrayList("names"));}
            if(state.getString("folder")!=null)folder=Uri.parse(state.getString("folder"));
            if(state.getStringArrayList("origins")!=null)originFolders.addAll(state.getStringArrayList("origins"));
            selectedVideoIndex=state.getInt("selectedVideoIndex",0);pendingOriginParent=state.getString("pendingOriginParent");pendingOriginSource=state.getString("pendingOriginSource");choosingOrigins=state.getBoolean("choosingOrigins",false);pendingFixedFolder=state.getString("pendingFixedFolder");pendingAuthorizationOrigin=state.getBoolean("pendingAuthorizationOrigin",false);
            pendingLog=state.getString("pendingLog");
            if(state.getString("result")!=null)resultUri=Uri.parse(state.getString("result"));
        }
        if(selectedInputs.isEmpty()&&input!=null){selectedInputs.add(input.toString());selectedNames.add(sourceName);}
        if(!selectedInputs.isEmpty()){int restored=selectedInputs.indexOf(input==null?"":input.toString());selectedVideoIndex=restored>=0?restored:Math.min(selectedVideoIndex,selectedInputs.size()-1);input=Uri.parse(selectedInputs.get(selectedVideoIndex));sourceName=selectedNames.get(selectedVideoIndex);}
        destinationPlan.reset(selectedInputs.size(),savedFolder);
        if(state!=null)destinationPlan.restore(state.getBooleanArray("individualOrigins"),state.getStringArrayList("individualFolders"),state.getBoolean("applyAllDestinations"),state.getBoolean("allOrigin",true),state.getString("allFolder",""));
        updateDestinationPanel();refreshLabels();inspectInput();
        if(resultUri!=null)openVideo.setVisibility(View.VISIBLE);
        File old=ConversionService.logFile(this);
        if(old.isFile())worker.execute(()->{
            try(RandomAccessFile f=new RandomAccessFile(old,"r")){
                int size=(int)Math.min(6000,f.length());byte[] bytes=new byte[size];f.seek(f.length()-size);f.readFully(bytes);
                String tail=new String(bytes,StandardCharsets.UTF_8);
                runOnUiThread(()->{if(!destroyed&&!busy)logView.setText(tail);});
            }catch(IOException ignored){}
        });
        pickVideo.setOnClickListener(v->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("video/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i,PICK_VIDEO);
        });
        pickFolder.setOnClickListener(v->{if(selectedInputs.isEmpty()){toast("Seleziona prima i video");return;}launchFolderPicker(PICK_FOLDER,selectedInputs.get(selectedVideoIndex));});
        convert.setOnClickListener(v->startConversion());
        stop.setOnClickListener(v->{if(hdrTesting){cancelled.set(true);status.setText("Annullamento prova HDR…");}else startService(new Intent(this,ConversionService.class).setAction(ConversionService.CANCEL));stop.setEnabled(false);});
        openVideo.setOnClickListener(v->{
            ConversionService.State current=ConversionService.snapshot();
            if(current.results.size()>1){String[] labels=new String[current.results.size()];for(int n=0;n<labels.length;n++)labels[n]=displayName(current.results.get(n));new android.app.AlertDialog.Builder(this).setTitle("Video completati").setItems(labels,(d,n)->openResult(current.results.get(n))).show();}
            else if(resultUri!=null)openResult(resultUri);
        });
        saveLog.setOnClickListener(v->exportLog());
        saveMetadata.setOnClickListener(v->exportMetadata());
    }
    private TextView parameterRow(LinearLayout parent,String title,String help){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);parent.addView(row);
        TextView label=text(row,title,20);label.setTypeface(null,Typeface.BOLD);label.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        infoButton(row,title,help);return label;
    }
    private void switchRow(LinearLayout parent,Switch toggle,String title,String help){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);parent.addView(row);
        row.addView(toggle,new LinearLayout.LayoutParams(0,-2,1));infoButton(row,title,help);
    }
    private void infoButton(LinearLayout row,String title,String help){
        Button info=new Button(this);info.setText("ⓘ");info.setTextSize(21);info.setTextColor(accent);info.setContentDescription("Spiegazione: "+title);
        info.setMinWidth(0);info.setMinimumWidth(0);info.setPadding(0,0,0,0);
        row.addView(info,new LinearLayout.LayoutParams(dp(48),dp(48)));
        info.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle(title).setMessage(help).setPositiveButton("OK",null).show());
    }
    private void updateTemporalHelp(){
        if(temporalHelp==null)return;
        presetValue.setText("Preset · "+AppSettings.PRESETS[presetSlider.getProgress()]);
        try{
            System.loadLibrary("x265probe");
            int[] values=nativePresetSettings(presetSlider.getProgress(),bframeSwitch.isChecked(),lookaheadSwitch.isChecked());
            bframeSwitch.setText("B-frame · "+values[0]);
            lookaheadSwitch.setText("Lookahead · "+values[1]);
            cuTreeLabel.setText("CU-tree · "+(values[2]!=0?"ON":"OFF"));
            aqModeLabel.setText("AQ mode · "+values[3]);
            aqStrengthLabel.setText(String.format(Locale.ITALY,"AQ strength · %.2f",values[4]/1000.0));
            temporalHelp.setVisibility(View.GONE);
        }catch(RuntimeException|LinkageError e){temporalHelp.setVisibility(View.VISIBLE);temporalHelp.setText("Parametri x265 non disponibili: "+e.getMessage());}
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
        final int generation=++infoGeneration;detailGeneration=-1;inputSummary.removeAllViews();inputDetails.removeAllViews();
        if(input==null){inputInfo.setText("Seleziona uno o più video");return;}
        final Uri selected=input;inputInfo.setText("Leggo dimensione e tracce…");
        worker.execute(()->{String quick=InputInspector.quickSummary(this,selected);runOnUiThread(()->{if(!destroyed&&generation==infoGeneration){inputInfo.setText((selectedInputs.size()>1?"Coda: "+selectedInputs.size()+" file\nDettagli del video: "+sourceName+"\n":"")+quick);if(videoDetails.getVisibility()==View.VISIBLE)loadInputDetails();}});});
    }
    private void loadInputDetails(){
        if(input==null||detailGeneration==infoGeneration)return;
        final int generation=infoGeneration;detailGeneration=generation;final Uri selected=input;
        inputSummary.removeAllViews();text(inputSummary,"Analisi dei dettagli…",14);
        worker.execute(()->{InputInspector.Report info=InputInspector.inspect(this,selected,()->destroyed||generation!=infoGeneration);runOnUiThread(()->{if(!destroyed&&generation==infoGeneration){renderInfoSections(inputSummary,info.summarySections);renderInfoSections(inputDetails,info.detailSections);}});});
    }
    private void styleSlider(SeekBar slider){slider.setProgressTintList(ColorStateList.valueOf(accent));slider.setThumbTintList(ColorStateList.valueOf(accent));slider.setProgressBackgroundTintList(ColorStateList.valueOf(border));}
    private void applyDefaults(AppSettings defaults){
        presetSlider.setProgress(defaults.preset);crfSlider.setProgress(defaults.crf);cpuMode.setSelection(defaults.cpu);bframeSwitch.setChecked(defaults.bframes);lookaheadSwitch.setChecked(defaults.lookahead);updateTemporalHelp();updateQualityHelp();settingsRevision=defaults.revision;
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
    private int selectedCrf(){return crfSlider.getProgress();}
    private void updateQualityHelp(){if(qualityHelp==null)return;int n=selectedCrf();qualityHelp.setText(n<0||n>51?"Inserisci un numero intero da 0 a 51.":"CRF · "+n);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private String displayName(Uri uri){
        try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst()&&!c.isNull(0))return c.getString(0);}catch(Exception ignored){}return "video";
    }
    private void openResult(Uri uri){try{startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}catch(Exception e){toast("Nessun lettore video disponibile");}}
    private void selectVideo(int index){
        if(index<0||index>=selectedInputs.size())return;
        selectedVideoIndex=index;input=Uri.parse(selectedInputs.get(index));sourceName=selectedNames.get(index);refreshLabels();inspectInput();
    }
    private void divider(LinearLayout parent){View line=new View(this);line.setBackgroundColor(border);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.setMargins(0,dp(16),0,dp(12));parent.addView(line,lp);}
    private CheckBox destinationCheck(LinearLayout parent,String title){CheckBox c=new CheckBox(this);c.setText(title);c.setTextSize(17);c.setTextColor(ink);c.setButtonTintList(ColorStateList.valueOf(accent));c.setMinHeight(dp(48));parent.addView(c,new LinearLayout.LayoutParams(-1,-2));return c;}
    private String folderPath(String value){if(value==null||value.isEmpty())return "Nessuna cartella scelta";Uri uri=Uri.parse(value);return Uri.decode(uri.getLastPathSegment());}
    private void updateDestinationPanel(){originPanel.setInputs(selectedInputs,selectedNames,destinationPlan.effectiveOrigins(),destinationPlan.effectiveFolders());}
    private void destinationsChanged(){
        originsReady=false;refreshLabels();updateDestinationPanel();originPanel.refresh();setBusy(busy);
        FolderPickerActivity.record(this,"PIANO DESTINAZIONI · override_coda="+destinationPlan.applyAll+" · video_visualizzato="+(selectedVideoIndex+1));
    }
    private void launchFolderPicker(int request,String source){
        if(busy||choosingOrigins)return;
        pendingOriginSource=source;choosingOrigins=true;setBusy(busy);
        FolderPickerActivity.record(this,"RICHIESTA CARTELLA · tipo="+request+" · video="+source);
        try{startActivityForResult(new Intent(this,FolderPickerActivity.class),request);}
        catch(RuntimeException e){cancelOrigins();toast("Selettore cartelle non avviato: "+e.getMessage());FolderPickerActivity.record(this,"ERRORE apertura · "+e);}
    }
    private void requestDirectoryAccess(String parent,String representative,boolean origin,String requestedFolder){
        if(busy||choosingOrigins)return;
        if(!origin&&(requestedFolder==null||requestedFolder.isEmpty())){launchFolderPicker(destinationPlan.applyAll?PICK_ALL_FOLDER:PICK_FOLDER,representative);return;}
        pendingOriginParent=parent;pendingFixedFolder=requestedFolder;pendingAuthorizationOrigin=origin;
        if(origin&&parent==null){
            new android.app.AlertDialog.Builder(this).setTitle("Origine non rilevata")
                .setMessage("Scegli la directory che contiene "+displayName(Uri.parse(representative))+". Verificherò anche gli altri video presenti nella stessa cartella.")
                .setPositiveButton("Scegli cartella",(d,w)->launchFolderPicker(PICK_ORIGIN,representative)).setNegativeButton("Annulla",(d,w)->cancelOrigins()).show();
        }else launchFolderPicker(PICK_ORIGIN,representative);
    }
    private void cancelOrigins(){choosingOrigins=false;pendingOriginParent=null;pendingOriginSource=null;pendingFixedFolder=null;pendingAuthorizationOrigin=false;setBusy(busy);}
    private void refreshLabels(){
        videoLabel.setText(input==null?"Nessun video selezionato":sourceName);
        videoNavigator.setVisibility(selectedInputs.size()>1?View.VISIBLE:View.GONE);videoNumber.setText("Video "+(selectedVideoIndex+1)+" di "+selectedInputs.size()+"  ▾");previousVideo.setEnabled(selectedVideoIndex>0);nextVideo.setEnabled(selectedVideoIndex+1<selectedInputs.size());
        updatingDestinationUi=true;
        applyAllDestinations.setChecked(destinationPlan.applyAll);allOrigin.setChecked(destinationPlan.all.origin);
        allFolderLabel.setText(destinationPlan.all.origin?"Ogni video nella propria directory di origine":folderPath(destinationPlan.all.folder));
        boolean selected=!selectedInputs.isEmpty()&&selectedVideoIndex<destinationPlan.files.size();
        videoDestinationTitle.setText(selected?"DESTINAZIONE · VIDEO "+(selectedVideoIndex+1)+" DI "+selectedInputs.size():"DESTINAZIONE DEL VIDEO");
        videoDestinationHelp.setText(destinationPlan.applyAll?"È attiva la regola della coda. La scelta individuale qui sotto viene conservata.":"Questa scelta riguarda solo il video visualizzato.");
        videoOrigin.setChecked(selected&&destinationPlan.files.get(selectedVideoIndex).origin);
        folderLabel.setText(selected?(destinationPlan.files.get(selectedVideoIndex).origin?"Directory di origine del video selezionato":folderPath(destinationPlan.files.get(selectedVideoIndex).folder)):"Seleziona prima un video");
        updatingDestinationUi=false;
    }
    private void setBusy(boolean value){
        busy=value;updateScreenAwake();if(originPanel!=null)originPanel.setBusy(value||choosingOrigins);boolean destinationEditable=!value&&!choosingOrigins;videoOrigin.setEnabled(destinationEditable&&!selectedInputs.isEmpty()&&!destinationPlan.applyAll);pickAllFolder.setEnabled(destinationEditable&&destinationPlan.applyAll);allOrigin.setEnabled(destinationEditable&&destinationPlan.applyAll);applyAllDestinations.setEnabled(destinationEditable&&!selectedInputs.isEmpty());pickVideo.setEnabled(!value&&!choosingOrigins);pickFolder.setEnabled(destinationEditable&&!selectedInputs.isEmpty()&&!destinationPlan.applyAll);crfSlider.setEnabled(!value);presetSlider.setEnabled(!value);for(Button b:crfButtons)b.setEnabled(!value);cpuMode.setEnabled(!value);bframeSwitch.setEnabled(!value);lookaheadSwitch.setEnabled(!value);
        hdrTest.setEnabled(!value);saveHdr.setEnabled(!value);convert.setEnabled(!value&&!choosingOrigins&&originsReady&&!selectedInputs.isEmpty());saveLog.setEnabled(!value);saveMetadata.setEnabled(!value);stop.setVisibility(value?View.VISIBLE:View.GONE);stop.setEnabled(value);

    }
    private void updateScreenAwake(){
        boolean enabled=conversionScreenVisible&&busy&&keepScreenOn!=null&&keepScreenOn.isChecked();
        if(enabled)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void startConversion(){
        if(input==null){toast("Seleziona uno o più video");return;}
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
        if(choosingOrigins){toast("Completa prima la scelta delle cartelle di origine");return;}
        if(!originsReady||originFolders.size()!=selectedInputs.size()||originFolders.contains(null)){toast("Completa le scelte e autorizza tutte le cartelle rosse prima di avviare la coda");return;}
        java.util.ArrayList<String> destinations=new java.util.ArrayList<>();
        destinations.addAll(originFolders);
        final int crf=selectedCrf();

        final int preset=presetSlider.getProgress(),cpu=cpuMode.getSelectedItemPosition();
        if(Build.VERSION.SDK_INT>=33&&!notificationsRequested&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){notificationsRequested=true;requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},4);return;}
        Intent task=new Intent(this,ConversionService.class).setAction(ConversionService.START)
            .putStringArrayListExtra("inputs",new java.util.ArrayList<>(selectedInputs)).putStringArrayListExtra("names",new java.util.ArrayList<>(selectedNames)).putStringArrayListExtra("folders",destinations).putStringArrayListExtra("destinationNotes",new java.util.ArrayList<>(destinationNotes))
            .putExtra("individualOrigins",destinationPlan.individualOrigins()).putStringArrayListExtra("individualFolders",destinationPlan.individualFolders()).putExtra("applyAllDestinations",destinationPlan.applyAll).putExtra("allOrigin",destinationPlan.all.origin).putExtra("allFolder",destinationPlan.all.folder).putExtra("crf",crf).putExtra("preset",preset).putExtra("cpu",cpu)
            .putExtra("bframes",bframeSwitch.isChecked()).putExtra("lookahead",lookaheadSwitch.isChecked());
        try{startForegroundService(task);setBusy(true);status.setText("Avvio conversione…");}
        catch(RuntimeException e){setBusy(false);status.setText("Avvio non riuscito: "+e.getMessage());}
    }
    private void renderSession(){
        if(destroyed||hdrTesting)return;
        ConversionService.State s=ConversionService.snapshot();if(!s.hasSession)return;
        frameLabel.setText("Fotogrammi: "+s.doneFrames+" / "+(s.totalFrames>0?s.totalFrames:"— (totale non dichiarato)"));
        queueLabel.setText("Video "+s.batchIndex+" / "+s.batchTotal+" · "+s.source+"\nRiusciti: "+s.succeeded+" · Falliti: "+s.failed);setBusy(s.running);progress.setProgress(s.percent);percentLabel.setText("Coda "+s.percent+"% · File "+s.filePercent+"%");status.setText(s.message);logView.setText(s.log);
        timeline.setText(String.format(Locale.ITALY,"Video convertito: %.1f s\nVideo rimanente: %s",s.done/1000000.0,s.total>0?String.format(Locale.ITALY,"%.1f s",Math.max(0,s.total-s.done)/1000000.0):"durata non disponibile"));
        effectiveTemporal.setText(s.bframeCount>=0?String.format(Locale.ITALY,"Encoder: B-frame %d · Lookahead %d\nCU-tree %s · AQ %d · Strength %.2f",s.bframeCount,s.lookaheadCount,s.cuTree!=0?"ON":"OFF",s.aqMode,s.aqStrength/1000.0):"Encoder: in attesa");
        resultUri=s.result;openVideo.setVisibility(resultUri==null?View.GONE:View.VISIBLE);
        if(input==null){selectedInputs.addAll(s.inputs);selectedNames.addAll(s.names);input=Uri.parse(selectedInputs.get(0));folder=s.folder;destinationPlan.reset(selectedInputs.size(),null);destinationPlan.restore(s.individualOrigins,s.individualFolders,s.applyAllDestinations,s.allOrigin,s.allFolder);sourceName=selectedNames.get(0);selectedVideoIndex=0;updateDestinationPanel();refreshLabels();inspectInput();}
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==9){updateDestinationPanel();originPanel.refresh();if(!OriginalMedia.hasVideoPermission(this))toast("Android può nascondere i percorsi: usa Autorizza per associare le cartelle manualmente.");return;}
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
    private void startHdrTest(){
        if(input==null){toast("Seleziona un video HDR HEVC");return;}if(busy||ConversionService.snapshot().running)return;
        final Uri selected=input;final int crf=selectedCrf();hdrTesting=true;if(selectedInputs.size()>1)toast("Prova HDR sul video visualizzato: "+sourceName);cancelled.set(false);setBusy(true);status.setText("Test HDR: resta su questa schermata…");recent.clear();logView.setText("");logView.setVisibility(View.VISIBLE);
        worker.execute(()->{String result;
            try{result=new HdrProbe(getApplicationContext(),()->destroyed||cancelled.get(),line->{synchronized(recent){recent.addLast(line);while(recent.size()>14)recent.removeFirst();}runOnUiThread(()->{if(!destroyed){synchronized(recent){logView.setText(String.join("\n",recent));}}});}).run(selected,crf);}
            catch(Exception|LinkageError e){result="Test HDR non completato: "+e.getMessage()+"\nEsporta il log HDR.";}
            final String message=result;runOnUiThread(()->{hdrTesting=false;if(!destroyed){setBusy(false);status.setText(message);}});
        });
    }
    private void exportLog(){File conversion=ConversionService.logFile(this);exportLog(conversion.isFile()?conversion:FolderPickerActivity.logFile(this),"CodecProbe-log-");}
    private void exportLog(File source,String prefix){
        if(!source.isFile()){toast("Nessun log di conversione disponibile");return;}
        saveLog.setEnabled(false);
        worker.execute(()->{
            try{
                File snapshot=File.createTempFile("log-export-",".txt",getCacheDir());
                try(InputStream in=new FileInputStream(source);OutputStream out=new FileOutputStream(snapshot)){copy(in,out);
                    File access=FolderPickerActivity.logFile(this);
                    if(prefix.equals("CodecProbe-log-")&&access.isFile()&&!access.equals(source)){out.write("\n\n*** DIAGNOSTICA CARTELLE E SELETTORE ANDROID ***\n".getBytes(StandardCharsets.UTF_8));try(InputStream extra=new FileInputStream(access)){copy(extra,out);}}
                }
                runOnUiThread(()->{
                    if(destroyed)return;
                    pendingLog=snapshot.getAbsolutePath();saveLog.setEnabled(true);
                    Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE);
                    i.putExtra(Intent.EXTRA_TITLE,prefix+System.currentTimeMillis()+".txt");startActivityForResult(i,SAVE_LOG);
                });
            }catch(Exception e){runOnUiThread(()->{if(!destroyed){saveLog.setEnabled(true);toast("Errore: "+e.getMessage());}});}
        });
    }
    private static void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
    @Override protected void onActivityResult(int request,int code,Intent data){
        super.onActivityResult(request,code,data);
        if(request==PICK_FOLDER||request==PICK_ALL_FOLDER||request==PICK_ORIGIN){
            String source=pendingOriginSource,parent=pendingOriginParent,requested=pendingFixedFolder;boolean sourceFolder=pendingAuthorizationOrigin;
            FolderPickerActivity.record(this,"RISPOSTA ALLA HOME · tipo="+request+" result="+code+" uri="+(data==null?null:data.getData()));
            cancelOrigins();
            String error=data==null?null:data.getStringExtra(FolderPickerActivity.ERROR);
            if(code!=RESULT_OK||data==null||data.getData()==null){toast(error!=null?error:"Il selettore Android non ha restituito una cartella. Ricontrollo i permessi già presenti.");originPanel.refresh();return;}
            Uri chosen=data.getData();
            if(request==PICK_ORIGIN){if(sourceFolder&&source!=null)originPanel.accept(parent,source,chosen);else if(requested!=null&&!requested.isEmpty())originPanel.acceptFixed(parent,requested,chosen);else originPanel.refresh();return;}
            if(request==PICK_ALL_FOLDER)destinationPlan.setAllFolder(chosen.toString());
            else {int index=selectedInputs.indexOf(source);if(index<0){toast("Il video della richiesta non è più nella coda");return;}destinationPlan.setFolder(index,chosen.toString());}
            folder=chosen;getPreferences(0).edit().putString("folder",chosen.toString()).apply();destinationsChanged();return;
        }
        if(code!=RESULT_OK||data==null){
            if(request==SAVE_LOG&&pendingLog!=null){new File(pendingLog).delete();pendingLog=null;}return;
        }
        if(request==PICK_VIDEO){
            java.util.LinkedHashSet<Uri> chosen=new java.util.LinkedHashSet<>();
            if(data.getClipData()!=null)for(int n=0;n<data.getClipData().getItemCount();n++)chosen.add(data.getClipData().getItemAt(n).getUri());
            if(data.getData()!=null)chosen.add(data.getData());
            if(chosen.isEmpty())return;
            if(chosen.size()>BatchProgress.MAX_FILES){toast("Seleziona al massimo "+BatchProgress.MAX_FILES+" video per coda");return;}
            selectedInputs.clear();selectedNames.clear();originFolders.clear();cancelOrigins();selectedVideoIndex=0;
            for(Uri chosenUri:chosen){try{getContentResolver().takePersistableUriPermission(chosenUri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}selectedInputs.add(chosenUri.toString());selectedNames.add(displayName(chosenUri));}
            input=Uri.parse(selectedInputs.get(0));sourceName=selectedNames.get(0);String defaultFolder=new AppSettings(this).defaultFolder;destinationPlan.reset(selectedInputs.size(),defaultFolder!=null?defaultFolder:folder==null?null:folder.toString());destinationsChanged();inspectInput();return;
        }
        if(data.getData()==null){if(request==PICK_ORIGIN)cancelOrigins();if(request==SAVE_LOG&&pendingLog!=null){new File(pendingLog).delete();pendingLog=null;}return;}Uri uri=data.getData();
        if(request==SAVE_LOG&&pendingLog!=null){
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
        if(folder!=null)b.putString("folder",folder.toString());
        b.putStringArrayList("inputs",new java.util.ArrayList<>(selectedInputs));b.putStringArrayList("names",new java.util.ArrayList<>(selectedNames));b.putStringArrayList("origins",new java.util.ArrayList<>(originFolders));b.putBooleanArray("individualOrigins",destinationPlan.individualOrigins());b.putStringArrayList("individualFolders",destinationPlan.individualFolders());b.putBoolean("applyAllDestinations",destinationPlan.applyAll);b.putBoolean("allOrigin",destinationPlan.all.origin);b.putString("allFolder",destinationPlan.all.folder);
        b.putInt("selectedVideoIndex",selectedVideoIndex);b.putString("pendingOriginParent",pendingOriginParent);b.putString("pendingOriginSource",pendingOriginSource);b.putBoolean("choosingOrigins",choosingOrigins);b.putString("pendingFixedFolder",pendingFixedFolder);b.putBoolean("pendingAuthorizationOrigin",pendingAuthorizationOrigin);
        if(resultUri!=null)b.putString("result",resultUri.toString());
        b.putString("sourceName",sourceName);b.putString("pendingLog",pendingLog);b.putInt("crfValue",selectedCrf());b.putInt("presetValue",presetSlider.getProgress());b.putInt("cpu",cpuMode.getSelectedItemPosition());b.putBoolean("bframes",bframeSwitch.isChecked());b.putBoolean("lookahead",lookaheadSwitch.isChecked());
    }
    @Override protected void onResume(){super.onResume();conversionScreenVisible=true;AppSettings saved=new AppSettings(this);keepScreenOn.setChecked(saved.keepScreenOn);if(!java.util.Objects.equals(loadedDefault,saved.defaultFolder)&&!ConversionService.snapshot().running){loadedDefault=saved.defaultFolder;if(loadedDefault!=null){folder=Uri.parse(loadedDefault);}refreshLabels();}if(saved.revision!=settingsRevision&&!ConversionService.snapshot().running)applyDefaults(saved);ConversionService.uiVisible=true;previousWall=0;monitor.post(sample);ConversionService.observe(sessionObserver);renderSession();if(originPanel!=null&&!ConversionService.snapshot().running)originPanel.refresh();updateScreenAwake();}
    @Override protected void onPause(){conversionScreenVisible=false;updateScreenAwake();ConversionService.uiVisible=false;monitor.removeCallbacks(sample);ConversionService.detach(sessionObserver);super.onPause();}
    @Override protected void onDestroy(){cancelled.set(true);conversionScreenVisible=false;updateScreenAwake();destroyed=true;if(originPanel!=null)originPanel.close();ConversionService.detach(sessionObserver);worker.shutdown();super.onDestroy();}
}
