package it.edo.codecprobe;

import android.app.Activity;
import android.net.Uri;
import android.widget.*;
import java.util.*;

/** Existing View renderer for sections that have not migrated to Compose. */
final class OriginDirectoryPanel {
    interface Listener {
        void authorize(String parent,String representative,boolean origin,String requestedFolder);
        void changed(boolean ready,ArrayList<String> destinations,ArrayList<String> notes);
        void error(String message);
    }
    final OriginDirectoryAccess access;
    private final AppUi ui;
    private final LinearLayout rows;
    private final Listener listener;
    private final MediaKind kind;
    private final ArrayList<Button> buttons=new ArrayList<>();
    private boolean busy;
    OriginDirectoryPanel(Activity activity,LinearLayout rows,Listener listener,AppUi ui){
        this(activity,rows,listener,ui,MediaKind.VIDEO);
    }
    OriginDirectoryPanel(Activity activity,LinearLayout rows,Listener listener,AppUi ui,MediaKind kind){
        this.ui=ui;this.rows=rows;this.listener=listener;this.kind=kind;
        access=new OriginDirectoryAccess(activity,kind,new OriginDirectoryAccess.Listener(){
            public void changed(OriginDirectoryAccess.State state){
                render(state);
                listener.changed(state.ready,new ArrayList<>(state.destinations),new ArrayList<>(state.notes));
            }
            public void error(String message){listener.error(message);}
        });
    }
    private void render(OriginDirectoryAccess.State state){
        rows.removeAllViews();buttons.clear();
        if(!state.message.isEmpty()){ui.text(rows,state.message,14);return;}
        for(OriginDirectoryAccess.Row row:state.rows){
            Button action=HomeDirectoryRow.add(ui,rows,row.label,row.numbers,row.allowed,row.manual,
                ()->listener.authorize(row.parent,row.representative,row.origin,row.requestedFolder),kind);
            if(row.choose)action.setText("Scegli");
            buttons.add(action);action.setEnabled(!busy);
        }
        if(state.total>state.rows.size())ui.text(rows,"… Altre "+(state.total-state.rows.size())+" cartelle. Usa la destinazione per tutte le foto per gestirle insieme.",14);
        ui.text(rows,"Cartelle autorizzate: "+state.authorized+" / "+state.total,14);
    }
    void setInputs(List<String> uris,List<String> labels,List<Boolean> origins,List<String> folders){access.setInputs(uris,labels,origins,folders);}
    void setBusy(boolean value){busy=value;for(Button button:buttons)button.setEnabled(!value);}
    void refresh(){access.refresh();}
    void accept(String parent,String representative,Uri tree){access.accept(parent,representative,tree);}
    void acceptFixed(String parent,String requested,Uri tree){access.acceptFixed(parent,requested,tree);}
    void close(){access.close();}
}
