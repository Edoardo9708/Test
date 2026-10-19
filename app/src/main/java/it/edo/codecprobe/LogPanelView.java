package it.edo.codecprobe;

import android.view.View;
import android.widget.*;

/** Shared log controls; the host owns export and deletion of its own files. */
final class LogPanelView {
    final Button clear,toggle;
    final TextView log;
    LogPanelView(AppUi ui,LinearLayout parent,String clearLabel,String help){
        ui.divider(parent);clear=ui.button(parent,clearLabel);ui.danger(clear);ui.text(parent,help,13);
        toggle=ui.button(parent,"Mostra ultimi messaggi");log=ui.text(parent,"",12);log.setTypeface(android.graphics.Typeface.MONOSPACE);log.setTextIsSelectable(true);log.setMaxLines(16);log.setVisibility(View.GONE);
        toggle.setOnClickListener(v->{boolean expanded=log.getVisibility()!=View.VISIBLE;log.setVisibility(expanded?View.VISIBLE:View.GONE);toggle.setText(expanded?"Nascondi ultimi messaggi":"Mostra ultimi messaggi");});
    }
}
