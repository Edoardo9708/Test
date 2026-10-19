package it.edo.codecprobe;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Directory access UI; provider queries stay off the UI thread. */
final class OriginDirectoryPanel {
    interface Listener {
        void authorize(String parent,String representative,boolean origin,String requestedFolder);
        void changed(boolean ready,ArrayList<String> destinations,ArrayList<String> notes);
        void error(String message);
    }
    private static final class Group {
        String parent,label;
        Uri destination;
        boolean manual,origin;
        String requestedFolder;
        final ArrayList<Integer> files=new ArrayList<>();
    }
    private final MediaKind kind;
    private final Activity activity;
    private final AppUi ui;
    private final LinearLayout rows;
    private final Listener listener;
    private final int ink;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ArrayList<Button> buttons=new ArrayList<>();
    private final SharedPreferences associations;
    private ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>(),fixedFolders=new ArrayList<>();
    private ArrayList<Boolean> origins=new ArrayList<>();
    private volatile int generation;
    private volatile boolean closed;
    private boolean busy;
    OriginDirectoryPanel(Activity a,LinearLayout rows,Listener listener,AppUi ui){
        this(a,rows,listener,ui,MediaKind.VIDEO);
    }
    OriginDirectoryPanel(Activity a,LinearLayout rows,Listener listener,AppUi ui,MediaKind kind){
        this.kind=kind;this.ui=ui;int ink=ui.ink;
        activity=a;this.rows=rows;this.listener=listener;this.ink=ink;
        associations=a.getSharedPreferences("origin-associations",Activity.MODE_PRIVATE);
    }
    void setInputs(List<String> uris,List<String> labels,List<Boolean> sourceFolders,List<String> chosenFolders){
        if(uris.size()!=labels.size()||uris.size()!=sourceFolders.size()||uris.size()!=chosenFolders.size())throw new IllegalArgumentException("Piano cartelle incompleto");
        if(inputs.equals(uris)&&names.equals(labels)&&origins.equals(sourceFolders)&&fixedFolders.equals(chosenFolders))return;
        inputs=new ArrayList<>(uris);names=new ArrayList<>(labels);origins=new ArrayList<>(sourceFolders);fixedFolders=new ArrayList<>(chosenFolders);refresh();
    }
    void setBusy(boolean value){busy=value;for(Button b:buttons)b.setEnabled(!value);}
    void close(){closed=true;generation++;worker.shutdown();}
    private TextView label(LinearLayout parent,String message,int color){
        TextView t=ui.text(parent,message,14);t.setTextColor(color);return t;
    }
    void refresh(){
        if(closed)return;
        final int revision=++generation;final ArrayList<String> sources=new ArrayList<>(inputs),labels=new ArrayList<>(names),folders=new ArrayList<>(fixedFolders);final ArrayList<Boolean> useOrigins=new ArrayList<>(origins);
        rows.removeAllViews();buttons.clear();
        label(rows,sources.isEmpty()?"Le destinazioni utilizzate appariranno qui dopo la selezione dei file.":"Controllo delle cartelle e dei permessi…",ink);
        listener.changed(false,new ArrayList<>(),new ArrayList<>());
        if(sources.isEmpty())return;
        worker.execute(()->{
            LinkedHashMap<String,Group> groups=new LinkedHashMap<>();
            ArrayList<String> destinations=new ArrayList<>(Collections.nCopies(sources.size(),null));
            ArrayList<String> notes=new ArrayList<>(Collections.nCopies(sources.size(),""));
            java.util.Map<String,Uri> grants=new java.util.HashMap<>();java.util.Map<Uri,Boolean> permissions=new java.util.HashMap<>();
            for(int i=0;i<sources.size();i++){
                if(closed||revision!=generation)return;
                Uri source=Uri.parse(sources.get(i));boolean origin=useOrigins.get(i),manual=false;
                String parent=null;Uri destination=null;String requested=folders.get(i);
                if(origin){
                    parent=SourceFolder.parentId(activity,source);destination=parent==null?null:cachedGrant(parent,grants);
                    String cached=associations.getString(sources.get(i),null);
                    if(destination==null&&cached!=null){Uri previous=Uri.parse(cached);boolean compatible=parent==null;
                        try{compatible|=SourceFolder.EXTERNAL.equals(previous.getAuthority())&&parent!=null&&FolderPaths.groupKey(parent,null,"").equals(FolderPaths.groupKey(SourceFolder.documentId(activity,previous),null,""));}catch(Exception ignored){}
                        if(compatible&&cachedWritable(previous,permissions)){destination=previous;manual=parent==null;}
                    }
                }else if(requested!=null&&!requested.isEmpty()){
                    Uri chosen=Uri.parse(requested);
                    try{if(SourceFolder.EXTERNAL.equals(chosen.getAuthority()))parent=SourceFolder.documentId(activity,chosen);}catch(Exception ignored){}
                    if(cachedWritable(chosen,permissions))destination=chosen;else if(parent!=null)destination=cachedGrant(parent,grants);
                }
                String displayParent=parent;
                if(displayParent==null&&destination!=null&&SourceFolder.EXTERNAL.equals(destination.getAuthority()))try{displayParent=SourceFolder.documentId(activity,destination);}catch(Exception ignored){}
                String key=FolderPaths.groupKey(parent,displayParent,destination!=null?"uri:"+destination:!origin?(requested==null||requested.isEmpty()?"missing:"+sources.get(i):"chosen:"+requested):"unknown:"+sources.get(i));
                Group group=groups.get(key);
                if(group==null){group=new Group();group.parent=parent;group.destination=destination;group.manual=manual;group.origin=origin;group.requestedFolder=requested;group.label=displayParent!=null?displayParent:destination!=null?Uri.decode(destination.getLastPathSegment()):!origin?(requested==null||requested.isEmpty()?"Cartella da scegliere":Uri.decode(Uri.parse(requested).getLastPathSegment())):"Origine non rilevata · "+labels.get(i);groups.put(key,group);}
                group.manual|=manual;group.files.add(i);destinations.set(i,destination==null?null:destination.toString());
                notes.set(i,kind.title+" "+(i+1)+" · "+labels.get(i)+" · regola="+(origin?"origine":"cartella scelta")+" · destinazione="+group.label+" · "+(destination==null?"ACCESSO MANCANTE":manual?"accesso verificato; origine associata manualmente":origin?"origine rilevata e accesso verificato":"cartella scelta e accesso verificato"));
            }
            activity.runOnUiThread(()->{
                if(closed||revision!=generation)return;
                rows.removeAllViews();buttons.clear();
                int authorized=0;
                for(Group group:groups.values()){
                    boolean allowed=group.destination!=null;if(allowed)authorized++;
                    String numbers="";for(int file:group.files)numbers+=(numbers.isEmpty()?"":", ")+(file+1);
                    Button action=HomeDirectoryRow.add(ui,rows,group.label,numbers,allowed,group.manual,()->listener.authorize(group.parent,sources.get(group.files.get(0)),group.origin,group.requestedFolder),kind);
                    if(!group.origin&&(group.requestedFolder==null||group.requestedFolder.isEmpty()))action.setText("Scegli");
                    buttons.add(action);action.setEnabled(!busy);
                }
                label(rows,"Cartelle autorizzate: "+authorized+" / "+groups.size(),ink);
                listener.changed(!destinations.contains(null),destinations,notes);
            });
        });
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
                    }catch(Exception ignored){/* The manually selected source remains usable; other files stay red. */}
                }
                edit.apply();FolderPickerActivity.record(activity,"OK · associazione cartella origine · video="+representative+" · directory="+destination);
                activity.runOnUiThread(()->{if(!closed){refresh();}});
            }catch(Exception e){activity.runOnUiThread(()->{if(!closed){listener.error(e.getMessage());refresh();}});}
        });
    }
}
