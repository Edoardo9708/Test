package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import java.util.*;

/** Shared destination interactions. Only file lists, draft and notifications come from the host. */
final class MediaDestinationsController {
    static final int FILE=301,ALL=302,AUTHORIZE=303;
    interface Host {
        AppUi ui();
        List<String> inputs();
        List<String> names();
        int selected();
        default List<Integer> selectedIndices(){return Collections.singletonList(selected());}
        DestinationDraft draft();
        String defaultFolder();
        boolean busy();
        void persist();
        void changed();
        void toast(String text);
    }
    private final Host host;
    private final MediaDestinationsView view;
    private final OriginDirectoryPanel panel;
    private boolean choosing,ready,closed;
    private String source,parent,requested;
    private ArrayList<String> targets=new ArrayList<>();
    private boolean origin;
    private ArrayList<String> resolved=new ArrayList<>();
    private AlertDialog dialog;
    private Runnable afterReady;
    private List<String> expectedInputs;
    private List<String> panelInputs=Collections.emptyList(),panelNames=Collections.emptyList();
    private DestinationPlan.Snapshot panelPlan;

    MediaDestinationsController(Host host,MediaDestinationsView view,MediaKind kind,Bundle saved){
        this.host=host;this.view=view;
        if(saved!=null){choosing=saved.getBoolean("destinationChoosing");source=saved.getString("destinationSource");parent=saved.getString("destinationParent");requested=saved.getString("destinationRequested");origin=saved.getBoolean("destinationOrigin");ArrayList<String> restored=saved.getStringArrayList("destinationTargets");if(restored!=null)targets=restored;}
        panel=new OriginDirectoryPanel(host.ui().activity,view.originRows,new OriginDirectoryPanel.Listener(){
            public void authorize(String parent,String representative,boolean origin,String folder){authorizeFolder(parent,representative,origin,folder);}
            public void changed(boolean valid,ArrayList<String> folders,ArrayList<String> notes){
                if(closed)return;ready=valid;resolved=new ArrayList<>(folders);host.changed();
                if(afterReady!=null&&(valid||!notes.isEmpty())){
                    Runnable action=afterReady;afterReady=null;
                    if(valid&&host.inputs().equals(expectedInputs))action.run();
                    else if(!valid)host.toast("Autorizza le cartelle indicate prima di riprovare");
                }
            }
            public void error(String message){host.toast("Cartella: "+message);}
        },host.ui(),kind);
        view.pickFolder.setOnClickListener(v->{if(!host.inputs().isEmpty())pick(FILE,host.inputs().get(host.selected()));});
        view.pickAllFolder.setOnClickListener(v->pick(ALL,null));
        view.videoOrigin.setOnCheckedChangeListener((v,value)->{
            if(view.updating||host.busy()||choosing||host.inputs().isEmpty())return;
            DestinationPlan plan=plan();for(int index:host.selectedIndices())plan.files.get(index).origin=value;edited();
        });
        view.applyAllDestinations.setOnCheckedChangeListener((v,value)->{
            if(view.updating||host.busy()||choosing)return;plan().applyAll=value;edited();
        });
        view.allOrigin.setOnCheckedChangeListener((v,value)->{
            if(view.updating||host.busy()||choosing)return;plan().all.origin=value;edited();
        });
    }
    private DestinationPlan plan(){host.draft().sync(host.inputs(),host.defaultFolder());return host.draft().plan;}
    boolean choosing(){return choosing;}
    boolean ready(){return ready&&!choosing&&!host.inputs().isEmpty()&&resolved.size()==host.inputs().size();}
    ArrayList<String> snapshot(){if(!ready())throw new IllegalStateException("Attendi la verifica di tutte le cartelle");return new ArrayList<>(resolved);}
    void whenReady(Runnable action){
        if(ready()){action.run();return;}afterReady=action;expectedInputs=new ArrayList<>(host.inputs());
    }
    void save(Bundle out){out.putStringArrayList("destinationTargets",targets);out.putBoolean("destinationChoosing",choosing);out.putString("destinationSource",source);out.putString("destinationParent",parent);out.putString("destinationRequested",requested);out.putBoolean("destinationOrigin",origin);}
    void render(){
        if(closed)return;
        DestinationPlan plan=plan();
        view.render(plan,host.inputs().size(),host.selected(),host.busy(),choosing);
        int groupSize=host.selectedIndices().size();
        if(groupSize>1){view.videoDestinationTitle.setText("DESTINAZIONE · "+groupSize+" FILE CORRENTI");view.videoDestinationHelp.setText(plan.applyAll?"È attiva la regola di tutta la coda. Disattivala per gestire il gruppo.":"La cartella scelta verrà applicata a tutti i file selezionati.");}panel.setBusy(host.busy()||choosing);
        if(panelPlan==null||!panelInputs.equals(host.inputs())||!panelNames.equals(host.names())||!panelPlan.matches(plan)){
            panelInputs=new ArrayList<>(host.inputs());panelNames=new ArrayList<>(host.names());panelPlan=new DestinationPlan.Snapshot(plan);
            panel.setInputs(panelInputs,panelNames,plan.effectiveOrigins(),plan.effectiveFolders());
        }
    }
    void refresh(){if(closed)return;render();panel.refresh();}
    private void edited(){ready=false;host.persist();render();host.changed();}
    private void pick(int code,String source){
        if(closed||host.busy()||choosing)return;
        if(code==FILE){targets.clear();for(int index:host.selectedIndices())targets.add(host.inputs().get(index));}
        this.source=source;choosing=true;host.changed();
        try{host.ui().activity.startActivityForResult(new Intent(host.ui().activity,FolderPickerActivity.class),code);}
        catch(RuntimeException error){choosing=false;host.changed();host.toast("Selettore cartelle non avviato: "+error.getMessage());}
    }
    private void authorizeFolder(String parent,String source,boolean origin,String folder){
        if(host.busy()||choosing)return;
        if(!origin&&(folder==null||folder.isEmpty())){pick(plan().applyAll?ALL:FILE,source);return;}
        this.parent=parent;this.source=source;this.requested=folder;this.origin=origin;
        if(origin&&parent==null){
            dialog=new AlertDialog.Builder(host.ui().activity).setTitle("Origine non rilevata")
                .setMessage("Scegli la directory che contiene il file selezionato. Verificherò anche gli altri file presenti nella stessa cartella.")
                .setPositiveButton("Scegli cartella",(d,w)->pick(AUTHORIZE,source)).setNegativeButton("Annulla",null).create();dialog.show();
        }else pick(AUTHORIZE,source);
    }
    boolean result(int code,int result,Intent data){
        if(code!=FILE&&code!=ALL&&code!=AUTHORIZE)return false;
        choosing=false;
        if(result!=android.app.Activity.RESULT_OK||data==null||data.getData()==null){
            String error=data==null?null:data.getStringExtra(FolderPickerActivity.ERROR);if(error!=null)host.toast(error);
            clearPending();refresh();host.changed();return true;
        }
        try{
            int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if(flags!=(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))throw new SecurityException("Servono lettura e scrittura permanenti");
            host.ui().activity.getContentResolver().takePersistableUriPermission(data.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if(code==AUTHORIZE){
                if(origin&&source!=null)panel.accept(parent,source,data.getData());
                else if(requested!=null&&!requested.isEmpty())panel.acceptFixed(parent,requested,data.getData());
            }else if(code==ALL){plan().setAllFolder(data.getData().toString());edited();}
            else{
                int index=host.inputs().indexOf(source);
                if(index<0)throw new IllegalStateException("Il file della richiesta non è più selezionato");
                if(targets.isEmpty())targets.add(source);
                DestinationPlan plan=plan();HashMap<String,Integer> indices=new HashMap<>();for(int n=0;n<host.inputs().size();n++)indices.put(host.inputs().get(n),n);
                for(String input:targets){Integer selected=indices.get(input);if(selected!=null)plan.setFolder(selected,data.getData().toString());}edited();
            }
        }catch(RuntimeException error){host.toast("Cartella non salvata: "+error.getMessage());}
        clearPending();refresh();host.changed();return true;
    }
    private void clearPending(){source=null;parent=null;requested=null;origin=false;targets.clear();}
    void close(){closed=true;afterReady=null;panel.close();if(dialog!=null)dialog.dismiss();}
}
