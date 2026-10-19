package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.*;
import android.view.View;
import android.widget.*;

/** One result popup for all formats. Copy includes collapsed details and keeps the popup open. */
final class ResultDialog {
    private final AppUi ui;
    private final java.util.function.Consumer<String> toast;
    private AlertDialog dialog;
    ResultDialog(AppUi ui,java.util.function.Consumer<String> toast){this.ui=ui;this.toast=toast;}
    AlertDialog show(String title,String name,String message,String lastState,String technical,String copy){
        close();LinearLayout content=new LinearLayout(ui.activity);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(ui.dp(20),ui.dp(8),ui.dp(20),ui.dp(12));
        ui.text(content,name,17).setTypeface(null,android.graphics.Typeface.BOLD);
        ui.text(content,"MESSAGGIO",12);TextView body=ui.text(content,message==null?"Dettagli non disponibili":message,16);body.setTextIsSelectable(true);
        if(lastState!=null&&!lastState.isEmpty()){ui.text(content,"ULTIMO STATO COMUNICATO",12);ui.text(content,lastState,15);}
        Button toggle=ui.button(content,"Mostra dettagli tecnici ▾");TextView details=ui.text(content,technical==null||technical.isEmpty()?"Nessun dettaglio tecnico aggiuntivo":technical,13);
        details.setTextColor(ui.ink);details.setTypeface(android.graphics.Typeface.MONOSPACE);details.setTextIsSelectable(true);details.setVisibility(View.GONE);
        toggle.setOnClickListener(v->{boolean expanded=details.getVisibility()!=View.VISIBLE;details.setVisibility(expanded?View.VISIBLE:View.GONE);toggle.setText(expanded?"Nascondi dettagli tecnici ▴":"Mostra dettagli tecnici ▾");});
        ScrollView scroll=new ScrollView(ui.activity);scroll.addView(content);
        dialog=new AlertDialog.Builder(ui.activity).setTitle(title).setView(scroll).setNegativeButton("Chiudi",null).setPositiveButton("Copia tutto",null).create();dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{ui.activity.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Esito conversione",copy));toast.accept("Errore e dettagli copiati");});
        return dialog;
    }
    void close(){if(dialog!=null){dialog.dismiss();dialog=null;}}
}
