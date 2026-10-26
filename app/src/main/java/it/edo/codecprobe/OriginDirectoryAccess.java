package it.edo.codecprobe;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.util.*;
import java.util.concurrent.*;

/** Resolves directory access on one worker and publishes immutable values, never Views. */
final class OriginDirectoryAccess {
    interface Listener {
        void changed(State state);
        void error(String message);
    }
    private static final class Group {
        String parent,label;
        Uri destination;
        boolean manual,origin;
        String requestedFolder;
        final ArrayList<Integer> files=new ArrayList<>();
    }
    static final class Row {
        final String key,label,numbers,parent,representative,requestedFolder;
        final boolean allowed,manual,origin,choose;
        Row(String key,Group group,String representative,String numbers){
            this.key=key;label=group.label;this.numbers=numbers;parent=group.parent;
            this.representative=representative;requestedFolder=group.requestedFolder;
            allowed=group.destination!=null;manual=group.manual;origin=group.origin;
            choose=!origin&&(requestedFolder==null||requestedFolder.isEmpty());
        }
    }
    static final class State {
        final List<Row> rows;
        final List<String> destinations,notes;
        final String message;
        final boolean checking,ready;
        final int authorized,total;
        State(List<Row> rows,List<String> destinations,List<String> notes,String message,boolean checking,int authorized,int total){
            this.rows=Collections.unmodifiableList(new ArrayList<>(rows));
            this.destinations=Collections.unmodifiableList(new ArrayList<>(destinations));
            this.notes=Collections.unmodifiableList(new ArrayList<>(notes));
            this.message=message;this.checking=checking;this.authorized=authorized;this.total=total;
            ready=!checking&&!destinations.isEmpty()&&!destinations.contains(null);
        }
        static State waiting(boolean hasInputs){
            return new State(Collections.emptyList(),Collections.emptyList(),Collections.emptyList(),
                hasInputs?"Controllo delle cartelle e dei permessi…":"Le destinazioni utilizzate appariranno qui dopo la selezione dei file.",hasInputs,0,0);
        }
    }
    private final MediaKind kind;
    private final Activity activity;
    private final Listener listener;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final SharedPreferences associations;
    private Future<?> scan;
    private ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>(),fixedFolders=new ArrayList<>();
    private ArrayList<Boolean> origins=new ArrayList<>();
    private volatile int generation;
    private volatile boolean closed;
    OriginDirectoryAccess(Activity activity,MediaKind kind,Listener listener){
        this.activity=activity;this.kind=kind;this.listener=listener;
        associations=activity.getSharedPreferences("origin-associations",Activity.MODE_PRIVATE);
    }
    void setInputs(List<String> uris,List<String> labels,List<Boolean> sourceFolders,List<String> chosenFolders){
        if(uris.size()!=labels.size()||uris.size()!=sourceFolders.size()||uris.size()!=chosenFolders.size())throw new IllegalArgumentException("Piano cartelle incompleto");
        if(inputs.equals(uris)&&names.equals(labels)&&origins.equals(sourceFolders)&&fixedFolders.equals(chosenFolders))return;
        inputs=new ArrayList<>(uris);names=new ArrayList<>(labels);origins=new ArrayList<>(sourceFolders);fixedFolders=new ArrayList<>(chosenFolders);refresh();
    }
    void close(){closed=true;generation++;if(scan!=null)scan.cancel(true);worker.shutdownNow();}
    void refresh(){
        if(closed)return;
        final int revision=++generation;final ArrayList<String> sources=new ArrayList<>(inputs),labels=new ArrayList<>(names),folders=new ArrayList<>(fixedFolders);final ArrayList<Boolean> useOrigins=new ArrayList<>(origins);
        if(scan!=null)scan.cancel(true);
        listener.changed(State.waiting(!sources.isEmpty()));
        if(sources.isEmpty())return;
        scan=worker.submit(()->{
            try {
                State result=resolve(revision,sources,labels,folders,useOrigins);
                if(result!=null)activity.runOnUiThread(()->{if(!closed&&revision==generation)listener.changed(result);});
            }catch(RuntimeException error){
                String message=error.getMessage()==null?error.getClass().getSimpleName():error.getMessage();
                activity.runOnUiThread(()->{
                    if(closed||revision!=generation)return;
                    listener.changed(new State(Collections.emptyList(),Collections.emptyList(),Collections.emptyList(),
                        "Verifica delle cartelle non riuscita: "+message,false,0,0));
                    listener.error(message);
                });
            }
        });
    }
    private State resolve(int revision,List<String> sources,List<String> labels,List<String> folders,List<Boolean> useOrigins){
        LinkedHashMap<String,Group> groups=new LinkedHashMap<>();
        ArrayList<String> destinations=new ArrayList<>(Collections.nCopies(sources.size(),null));
        ArrayList<String> notes=new ArrayList<>(Collections.nCopies(sources.size(),""));
        java.util.Map<String,Uri> grants=new java.util.HashMap<>();java.util.Map<Uri,Boolean> permissions=new java.util.HashMap<>();
        for(int i=0;i<sources.size();i++){
            if(closed||revision!=generation||Thread.currentThread().isInterrupted())return null;
            Uri source=Uri.parse(sources.get(i));boolean origin=useOrigins.get(i),manual=false;
            String parent=null;Uri destination=null;String requested=folders.get(i);
            if(origin){
                parent=SourceFolder.parentId(activity,source);destination=parent==null?null:cachedGrant(parent,grants);
                String cached=associations.getString(sources.get(i),null);
                if(destination==null&&cached!=null){Uri previous=Uri.parse(cached);boolean compatible=parent==null;
                    try{compatible|=SourceFolder.EXTERNAL.equals(previous.getAuthority())&&parent!=null&&FolderPaths.groupKey(parent,null,"").equals(FolderPaths.groupKey(SourceFolder.documentId(activity,previous),null,""));}catch(RuntimeException error){compatible=parent==null;}
                    if(compatible&&cachedWritable(previous,permissions)){destination=previous;manual=parent==null;}
                }
            }else if(requested!=null&&!requested.isEmpty()){
                Uri chosen=Uri.parse(requested);
                try{if(SourceFolder.EXTERNAL.equals(chosen.getAuthority()))parent=SourceFolder.documentId(activity,chosen);}catch(RuntimeException error){parent=null;}
                if(cachedWritable(chosen,permissions))destination=chosen;else if(parent!=null)destination=cachedGrant(parent,grants);
            }
            String displayParent=parent;
            if(displayParent==null&&destination!=null&&SourceFolder.EXTERNAL.equals(destination.getAuthority()))try{displayParent=SourceFolder.documentId(activity,destination);}catch(RuntimeException error){displayParent=null;}
            String key=FolderPaths.groupKey(parent,displayParent,destination!=null?"uri:"+destination:!origin?(requested==null||requested.isEmpty()?"missing:"+sources.get(i):"chosen:"+requested):"unknown:"+sources.get(i));
            Group group=groups.get(key);
            if(group==null){group=new Group();group.parent=parent;group.destination=destination;group.manual=manual;group.origin=origin;group.requestedFolder=requested;group.label=displayParent!=null?displayParent:destination!=null?Uri.decode(destination.getLastPathSegment()):!origin?(requested==null||requested.isEmpty()?"Cartella da scegliere":Uri.decode(Uri.parse(requested).getLastPathSegment())):"Origine non rilevata · "+labels.get(i);groups.put(key,group);}
            group.manual|=manual;group.files.add(i);destinations.set(i,destination==null?null:destination.toString());
            notes.set(i,kind.title+" "+(i+1)+" · "+labels.get(i)+" · regola="+(origin?"origine":"cartella scelta")+" · destinazione="+group.label+" · "+(destination==null?"ACCESSO MANCANTE":manual?"accesso verificato; origine associata manualmente":origin?"origine rilevata e accesso verificato":"cartella scelta e accesso verificato"));
        }
        ArrayList<Row> rows=new ArrayList<>();
        int authorized=0;
        for(Map.Entry<String,Group> entry:groups.entrySet()){
            if(closed||revision!=generation||Thread.currentThread().isInterrupted())return null;
            Group group=entry.getValue();
            if(group.destination!=null)authorized++;
            if(kind==MediaKind.IMAGE&&rows.size()>=ImageQueueStore.VISIBLE)continue;
            StringBuilder list=new StringBuilder();int numberLimit=kind==MediaKind.IMAGE?100:Integer.MAX_VALUE;
            for(int n=0;n<Math.min(group.files.size(),numberLimit);n++){if(n>0)list.append(", ");list.append(group.files.get(n)+1);}
            if(group.files.size()>numberLimit)list.append(", … (").append(group.files.size()).append(" foto)");
            rows.add(new Row(entry.getKey(),group,sources.get(group.files.get(0)),list.toString()));
        }
        State result=new State(rows,destinations,notes,"",false,authorized,groups.size());
        return result;
    }
    private Uri cachedGrant(String parent,java.util.Map<String,Uri> grants){if(!grants.containsKey(parent))grants.put(parent,SourceFolder.granted(activity,parent));return grants.get(parent);}
    private boolean cachedWritable(Uri uri,java.util.Map<Uri,Boolean> permissions){if(!permissions.containsKey(uri))permissions.put(uri,SourceFolder.writable(activity,uri));return permissions.get(uri);}
    void acceptFixed(String parent,String requested,Uri tree){
        worker.execute(()->{
            try{
                Uri intended=Uri.parse(requested);
                if(parent!=null){
                    if(!SourceFolder.EXTERNAL.equals(tree.getAuthority())||!FolderPaths.covers(DocumentsContract.getTreeDocumentId(tree),parent))throw new IllegalArgumentException("Autorizza "+parent+" oppure una cartella superiore.");
                    intended=SourceFolder.underTree(tree,parent);
                }else if(!java.util.Objects.equals(intended.getAuthority(),tree.getAuthority())||!SourceFolder.documentId(activity,intended).equals(SourceFolder.documentId(activity,tree)))throw new IllegalArgumentException("La cartella autorizzata non corrisponde a quella scelta.");
                if(!SourceFolder.writable(activity,intended))throw new IllegalArgumentException("Permesso di scrittura non disponibile per la cartella scelta.");
                FolderPickerActivity.record(activity,"OK · accesso verificato per destinazione scelta · "+intended);
                activity.runOnUiThread(()->{if(!closed)refresh();});
            }catch(Exception e){activity.runOnUiThread(()->{if(!closed){listener.error(e.getMessage());refresh();}});}
        });
    }
    void accept(String parent,String representative,Uri tree){
        final ArrayList<String> sources=new ArrayList<>(inputs),labels=new ArrayList<>(names);
        worker.execute(()->{
            try{
                Uri destination=tree;
                if(parent!=null){
                    if(!SourceFolder.EXTERNAL.equals(tree.getAuthority())||!FolderPaths.covers(DocumentsContract.getTreeDocumentId(tree),parent))throw new IllegalArgumentException("Scegli la cartella "+parent+" oppure una sua cartella superiore.");
                    destination=SourceFolder.underTree(tree,parent);
                }
                if(!SourceFolder.writable(activity,destination))throw new IllegalArgumentException("La cartella non consente la creazione di file. Autorizza lettura e scrittura.");
                SharedPreferences.Editor edit=associations.edit();
                if(parent!=null){
                    for(String source:sources)if(parent.equals(SourceFolder.parentId(activity,Uri.parse(source))))edit.putString(source,destination.toString());
                }else{
                    // Associate the explicitly chosen source; then prove membership for the others.
                    edit.putString(representative,destination.toString());
                    Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,SourceFolder.documentId(activity,tree));
                    try(Cursor cursor=activity.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
                        if(cursor!=null)while(cursor.moveToNext()){
                            if(closed)return;
                            String name=cursor.getString(1);Uri child=DocumentsContract.buildDocumentUriUsingTree(tree,cursor.getString(0));
                            for(int i=0;i<sources.size();i++)if(labels.get(i).equals(name)&&SourceFolder.sameFile(activity,Uri.parse(sources.get(i)),child))edit.putString(sources.get(i),destination.toString());
                        }
                    }catch(Exception error){
                        // Keep the proven manual association; unverified siblings remain unauthorized.
                        FolderPickerActivity.record(activity,"Verifica degli altri file non completata: "+error.getMessage());
                    }
                }
                edit.apply();FolderPickerActivity.record(activity,"OK · associazione cartella origine · video="+representative+" · directory="+destination);
                activity.runOnUiThread(()->{if(!closed){refresh();}});
            }catch(Exception e){activity.runOnUiThread(()->{if(!closed){listener.error(e.getMessage());refresh();}});}
        });
    }
}
