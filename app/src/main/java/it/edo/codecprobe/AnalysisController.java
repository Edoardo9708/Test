package it.edo.codecprobe;

import android.app.Activity;
import android.os.*;
import android.view.View;
import java.util.*;
import java.util.concurrent.*;

/** Lifecycle/worker ownership. No source reads on the main thread, no whole-file buffers. */
final class AnalysisController {
    final Activity activity;
    final AnalysisScreen screen;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private Future<?> pending;
    private long generation;
    private boolean closed,active,busy;
    private AnalysisSources.Entry source;
    private final List<AnalysisNode> path=new ArrayList<>();
    private List<AnalysisNode> children=Collections.emptyList();
    private AnalysisNode selected;
    private int shown=60;
    private long hexPage;
    private String restoreUri;
    AnalysisController(Activity activity,Bundle saved){
        this.activity=activity;screen=new AnalysisScreen(new AppUi(activity));restoreUri=saved==null?null:saved.getString("analysisUri");
        screen.back.setOnClickListener(v->list());screen.reload.setOnClickListener(v->{if(source!=null)open(source);});
        screen.explain.setOnClickListener(v->{if(!path.isEmpty())inspect(path.get(path.size()-1));});
        screen.onDetailClosed=()->{cancel();ready();};
        screen.more.setOnClickListener(v->{shown+=60;render();});screen.hex.setOnClickListener(v->{if(busy||selected==null)return;if(screen.hexPanel.getVisibility()==View.VISIBLE){screen.hexPanel.setVisibility(View.GONE);screen.hex.setText("Mostra byte HEX");}else{hexPage=0;hex();}});
        screen.previous.setOnClickListener(v->{if(!busy&&hexPage>=256){hexPage-=256;hex();}});screen.next.setOnClickListener(v->{if(!busy&&selected!=null&&selected.size-hexPage>256){hexPage+=256;hex();}});
    }
    void resume(){active=true;if(source!=null){open(source);return;}List<AnalysisSources.Entry> entries=AnalysisSources.list(activity);if(restoreUri!=null){String value=restoreUri;restoreUri=null;for(AnalysisSources.Entry e:entries)if(e.uri.equals(value)){open(e);return;}}list();}
    void pause(){active=false;screen.closePopup();cancel();}
    void save(Bundle out){if(source!=null)out.putString("analysisUri",source.uri);}
    void destroy(){closed=true;screen.closePopup();cancel();worker.shutdownNow();main.removeCallbacksAndMessages(null);}
    private void cancel(){generation++;if(pending!=null)pending.cancel(true);pending=null;busy=false;}
    void list(){screen.closePopup();cancel();source=null;selected=null;path.clear();children=Collections.emptyList();screen.status.setText("");screen.sources(AnalysisSources.list(activity),this::open);}
    void open(AnalysisSources.Entry entry){
        screen.closePopup();source=entry;path.clear();children=Collections.emptyList();selected=null;screen.files.setVisibility(View.GONE);screen.map.setVisibility(View.GONE);screen.blocks.removeAllViews();screen.trail.removeAllViews();screen.frameTitle.setText(entry.name);screen.description.setText("");screen.hexPanel.setVisibility(View.GONE);screen.hex.setEnabled(false);screen.more.setVisibility(View.GONE);
        run("Lettura della struttura…",r->{AnalysisNode root=AnalysisParser.root(r,entry.name);return new Result(root,AnalysisParser.children(r,root));},result->{path.add(result.node);children=result.nodes;selected=result.node;shown=60;render();});
    }
    private void enter(AnalysisNode node){
        if(busy)return;selected=node;screen.showDetails(node);
        if(!node.expandable()){inspect(node);return;}if(path.size()>=32){screen.status.setText("Profondità massima raggiunta (32 livelli).");return;}
        run("Lettura di "+node.title+"…",r->new Result(node,AnalysisParser.children(r,node)),result->{path.add(node);children=result.nodes;shown=60;render();});
    }
    private void inspect(AnalysisNode node){
        if(busy)return;selected=node;screen.openPopup(node);
        run("Lettura del campo…",r->AnalysisDetailReader.read(r,node),screen::showDetail);
    }
    private void ready(){screen.status.setText("Sola lettura · blocchi non proporzionali ai byte"+(source!=null&&source.workspace.equals("Video")&&OriginalMedia.hasLocationPermission(activity)?"":"\nGPS originale non verificato nell’accesso del provider."));}
    private void navigate(int index){
        if(busy)return;AnalysisNode target=path.get(index);
        run("Lettura di "+target.title+"…",r->new Result(target,AnalysisParser.children(r,target)),result->{while(path.size()>index+1)path.remove(path.size()-1);children=result.nodes;selected=target;shown=60;render();});
    }
    private void render(){screen.tree(source,path,children,shown,this::navigate,this::enter);selected=path.get(path.size()-1);}
    private void hex(){
        AnalysisNode node=selected;long page=hexPage;
        run("Lettura di 256 byte…",r->AnalysisParser.hex(r,node,page),text->{screen.hexText.setText(text);screen.hexPanel.setVisibility(View.VISIBLE);screen.hex.setText("Nascondi byte HEX");screen.pageLabel.setText("Byte "+page+"–"+(page+Math.min(256,node.size-page)-1)+" del blocco ("+node.size+" totali)");screen.previous.setEnabled(page>=256);screen.next.setEnabled(node.size-page>256);});
    }
    private interface Read<T>{T read(AnalysisReader r)throws Exception;}
    private <T> void run(String message,Read<T> read,java.util.function.Consumer<T> done){
        cancel();busy=true;long ticket=generation;AnalysisSources.Entry input=source;long expectedSize=path.isEmpty()?-1:path.get(0).size;screen.status.setText(message);
        pending=worker.submit(()->{try(AnalysisInput file=new AnalysisInput(activity.getApplicationContext(),input)){
            if(expectedSize>=0&&file.reader.size!=expectedSize)throw new java.io.IOException("La dimensione del file è cambiata. Premi Rileggi file.");
            T result=read.read(file.reader);main.post(()->{if(closed||!active||ticket!=generation)return;busy=false;ready();done.accept(result);});
        }catch(Exception e){main.post(()->{if(closed||!active||ticket!=generation)return;busy=false;screen.status.setText("Analisi non disponibile: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));screen.map.setVisibility(View.VISIBLE);screen.detailError("Lettura non disponibile: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));});}});
    }
    boolean back(){if(source==null)return false;if(!busy&&path.size()>1)navigate(path.size()-2);else list();return true;}
    private static final class Result{final AnalysisNode node;final List<AnalysisNode> nodes;Result(AnalysisNode node,List<AnalysisNode> nodes){this.node=node;this.nodes=nodes;}}
}
