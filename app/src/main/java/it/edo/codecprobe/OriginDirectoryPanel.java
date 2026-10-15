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
        void authorize(String parent,String representative);
        void changed(boolean ready,ArrayList<String> destinations,ArrayList<String> notes);
        void error(String message);
    }
    private static final class Group {
        String parent,label;
        Uri destination;
        boolean manual;
        final ArrayList<Integer> files=new ArrayList<>();
    }
    private final Activity activity;
    private final LinearLayout rows;
    private final Listener listener;
    private final int ink,green,red;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ArrayList<Button> buttons=new ArrayList<>();
    private final SharedPreferences associations;
    private ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>();
    private volatile int generation;
    private volatile boolean closed;
    private boolean busy;
    OriginDirectoryPanel(Activity a,LinearLayout rows,Listener listener,int ink,boolean dark){
        activity=a;this.rows=rows;this.listener=listener;this.ink=ink;
        green=android.graphics.Color.parseColor(dark?"#83D9A2":"#176D37");red=android.graphics.Color.parseColor(dark?"#FF9B98":"#AF2424");
        associations=a.getSharedPreferences("origin-associations",Activity.MODE_PRIVATE);
    }
    void setInputs(List<String> uris,List<String> labels){
        if(inputs.equals(uris)&&names.equals(labels))return;
        inputs=new ArrayList<>(uris);names=new ArrayList<>(labels);refresh();
    }
    void setBusy(boolean value){busy=value;for(Button b:buttons)b.setEnabled(!value);}
    void close(){closed=true;generation++;worker.shutdown();}
    private TextView label(LinearLayout parent,String message,int color){
        TextView t=new TextView(activity);t.setText(message);t.setTextSize(14);t.setTextColor(color);t.setPadding(0,8,0,8);t.setSingleLine(false);parent.addView(t);return t;
    }
    void refresh(){
        if(closed)return;
        final int revision=++generation;final ArrayList<String> sources=new ArrayList<>(inputs),labels=new ArrayList<>(names);
        rows.removeAllViews();buttons.clear();
        label(rows,sources.isEmpty()?"Le cartelle di origine appariranno qui dopo la selezione dei video.":"Controllo delle cartelle e dei permessi…",ink);
        listener.changed(false,new ArrayList<>(),new ArrayList<>());
        if(sources.isEmpty())return;
        worker.execute(()->{
            LinkedHashMap<String,Group> groups=new LinkedHashMap<>();
            ArrayList<String> destinations=new ArrayList<>(Collections.nCopies(sources.size(),null));
            ArrayList<String> notes=new ArrayList<>(Collections.nCopies(sources.size(),""));
            for(int i=0;i<sources.size();i++){
                if(closed||revision!=generation)return;
                Uri source=Uri.parse(sources.get(i));String parent=SourceFolder.parentId(activity,source);
                String cached=associations.getString(sources.get(i),null);Uri destination=parent==null?null:SourceFolder.granted(activity,parent);boolean manual=false;
                if(destination==null&&cached!=null){Uri previous=Uri.parse(cached);boolean compatible=parent==null;
                    try{compatible|=SourceFolder.EXTERNAL.equals(previous.getAuthority())&&parent!=null&&parent.equals(SourceFolder.documentId(activity,previous));}catch(Exception ignored){}
                    if(compatible&&SourceFolder.writable(activity,previous)){destination=previous;manual=parent==null;}
                }
                String key=parent!=null?"parent:"+parent:destination!=null?"uri:"+destination:"unknown:"+sources.get(i);
                Group group=groups.get(key);
                if(group==null){group=new Group();group.parent=parent;group.destination=destination;group.manual=manual;group.label=parent!=null?parent:destination!=null?Uri.decode(destination.getLastPathSegment()):"Cartella non rilevata · "+labels.get(i);groups.put(key,group);}
                group.files.add(i);destinations.set(i,destination==null?null:destination.toString());
                notes.set(i,"Video "+(i+1)+" · "+labels.get(i)+" · origine="+group.label+" · "+(destination==null?"ACCESSO MANCANTE":manual?"accesso verificato; origine associata manualmente":"origine rilevata e accesso verificato"));
            }
            activity.runOnUiThread(()->{
                if(closed||revision!=generation)return;
                rows.removeAllViews();buttons.clear();
                int authorized=0;
                for(Group group:groups.values()){
                    boolean allowed=group.destination!=null;if(allowed)authorized++;
                    LinearLayout row=new LinearLayout(activity);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(android.view.Gravity.CENTER_VERTICAL);rows.addView(row,new LinearLayout.LayoutParams(-1,-2));
                    LinearLayout description=new LinearLayout(activity);description.setOrientation(LinearLayout.VERTICAL);row.addView(description,new LinearLayout.LayoutParams(0,-2,1));
                    String numbers="";for(int file:group.files)numbers+=(numbers.isEmpty()?"":", ")+(file+1);
                    label(description,(allowed?"✓ AUTORIZZATA":"✕ DA AUTORIZZARE")+"\n"+group.label+"\nVideo: "+numbers+(group.manual?"\nOrigine associata manualmente":""),allowed?green:red);
                    Button action=new Button(activity);action.setText(allowed?"Gestisci":"Autorizza");action.setAllCaps(false);action.setTextColor(allowed?green:red);action.setSingleLine(false);action.setMinHeight(Math.round(48*activity.getResources().getDisplayMetrics().density));row.addView(action,new LinearLayout.LayoutParams(-2,-2));buttons.add(action);action.setEnabled(!busy);
                    action.setOnClickListener(v->listener.authorize(group.parent,sources.get(group.files.get(0))));
                }
                label(rows,"Cartelle autorizzate: "+authorized+" / "+groups.size(),ink);
                listener.changed(!destinations.contains(null),destinations,notes);
            });
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
                edit.apply();
                activity.runOnUiThread(()->{if(!closed){refresh();}});
            }catch(Exception e){activity.runOnUiThread(()->{if(!closed){listener.error(e.getMessage());refresh();}});}
        });
    }
}
