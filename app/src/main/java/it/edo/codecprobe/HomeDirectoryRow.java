package it.edo.codecprobe;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;

/**
 * A directory permission row. Provider access and permission decisions belong to its controller. 
 */
final class HomeDirectoryRow {
    
    static Button add(AppUi ui, LinearLayout parent, String directory, String videos, boolean allowed, boolean manual, Runnable authorize) {
        return add(ui,parent,directory,videos,allowed,manual,authorize,MediaKind.VIDEO);
    }
    static Button add(AppUi ui, LinearLayout parent, String directory, String numbers, boolean allowed, boolean manual, Runnable authorize, MediaKind kind) {
        int color = android.graphics.Color.parseColor(allowed ? (ui.dark ? "#83D9A2" : "#176D37") : (ui.dark ? "#FF9B98" : "#AF2424"));
        LinearLayout tile = ui.panel(parent);
        GradientDrawable background = ui.shape(ui.bg, 14);
        background.setStroke(ui.dp(1), color);
        tile.setBackground(ui.photos?PhotoStyle.glass(ui,tile,24,false,true):background);
        if(ui.photos){LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)tile.getLayoutParams();lp.setMargins(0,ui.dp(4),0,0);tile.setLayoutParams(lp);tile.setPadding(ui.dp(16),ui.dp(12),ui.dp(16),ui.dp(12));}
        TextView status = ui.text(tile, allowed ? "\u2713 Accesso verificato" : "! Da autorizzare", 13);
        status.setTextColor(color);
        status.setTypeface(null, Typeface.BOLD);
        LinearLayout row = ui.row(tile);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout description = new LinearLayout(ui.activity);
        description.setOrientation(LinearLayout.VERTICAL);
        row.addView(description, new LinearLayout.LayoutParams(0, -2, 1));
        TextView path = ui.text(description, directory, 14);
        path.setTextColor(ui.ink);
        path.setTextIsSelectable(true);
        ui.text(description, kind.title+": " + numbers + (manual ? " \u00b7 origine associata manualmente" : ""), 12);
        Button action = new Button(ui.activity);
        action.setText(allowed ? "Gestisci" : "Autorizza");
        ui.styleButton(action, false);
        action.setTextSize(13);
        action.setMinHeight(ui.dp(48));
        action.setMinimumHeight(ui.dp(48));
        LinearLayout.LayoutParams button = new LinearLayout.LayoutParams(ui.dp(92), -2);
        button.setMargins(ui.dp(8), 0, 0, 0);
        row.addView(action, button);
        action.setOnClickListener((v)->authorize.run());
        return action;
    }
    
    private HomeDirectoryRow() {
    }
}