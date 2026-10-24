package it.edo.codecprobe;

import android.graphics.drawable.Drawable;
import android.widget.TextView;

/** Fixed icon box prevents changing glyph metrics from shifting the text baseline. */
final class TrialIcons {
    static void set(AppUi ui,TextView text,int resource,boolean primary){
        Drawable icon=ui.activity.getDrawable(resource).mutate();icon.setTint(primary?ui.bg:ui.accent);icon.setBounds(0,0,ui.dp(20),ui.dp(20));
        text.setCompoundDrawablePadding(ui.dp(8));text.setCompoundDrawablesRelative(icon,null,null,null);
    }
    private TrialIcons(){}
}
