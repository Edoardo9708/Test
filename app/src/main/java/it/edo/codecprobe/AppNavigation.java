package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.widget.*;

/** Scrollable page-top workspace tabs. Switching never changes either conversion session. */
final class AppNavigation extends LinearLayout {
    final Button video,audio;
    AppNavigation(AppUi ui,boolean audioSelected){
        super(ui.activity);setOrientation(HORIZONTAL);setBackgroundColor(ui.bg);
        video=tab(ui,"Video",!audioSelected,()->{
            if(!audioSelected)return;
            ui.activity.startActivity(new Intent(ui.activity,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP));
            // Keep Video as the task root: repeated switching must not build a Back-stack loop.
            ui.activity.finish();fade(ui.activity);
        });
        audio=tab(ui,"Audio",audioSelected,()->{
            if(audioSelected)return;
            ui.activity.startActivity(new Intent(ui.activity,AudioActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP));fade(ui.activity);
        });
    }
    /** Equal-width tabs allow another workspace to be added without changing the host layout. */
    private Button tab(AppUi ui,String label,boolean selected,Runnable action){
        LinearLayout column=new LinearLayout(ui.activity);column.setOrientation(VERTICAL);addView(column,new LinearLayout.LayoutParams(0,-2,1));
        Button button=new Button(ui.activity);ui.styleButton(button,false);button.setText(label);button.setTextSize(17);button.setSelected(selected);
        button.setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);button.setTextColor(selected?ui.accent:ui.muted);button.setPadding(ui.dp(8),ui.dp(8),ui.dp(8),ui.dp(8));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(ui.border),null,ui.shape(android.graphics.Color.WHITE,8)));
        button.setContentDescription(label+(selected?" · selezionato":""));column.addView(button,new LinearLayout.LayoutParams(-1,-2));
        android.view.View underline=new android.view.View(ui.activity);underline.setBackgroundColor(selected?ui.accent:ui.border);column.addView(underline,new LinearLayout.LayoutParams(-1,ui.dp(2)));
        button.setOnClickListener(v->action.run());return button;
    }
    static void fade(Activity activity){activity.overridePendingTransition(R.anim.workspace_fade_in,R.anim.workspace_fade_out);}
}
