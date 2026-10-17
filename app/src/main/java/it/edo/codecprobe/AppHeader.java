package it.edo.codecprobe;

import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;

/** Shared page header. Each workspace owns its settings action. */
final class AppHeader {
    static Button add(AppUi ui,LinearLayout page,String heading,String subtitle){
        LinearLayout header=new LinearLayout(ui.activity);header.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(header,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout title=new LinearLayout(ui.activity);title.setOrientation(LinearLayout.VERTICAL);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        ui.text(title,heading,27).setTypeface(null,Typeface.BOLD);ui.text(title,subtitle,13);
        Button settings=new Button(ui.activity);settings.setText("Impostazioni");ui.styleButton(settings,false);
        settings.setTextSize(13);settings.setPadding(ui.dp(10),ui.dp(8),ui.dp(10),ui.dp(8));
        header.addView(settings,new LinearLayout.LayoutParams(-2,-2));return settings;
    }
    private AppHeader(){}
}
