package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.widget.*;

/** Scrollable page-top workspace tabs. Switching never changes either conversion session. */
final class AppNavigation extends LinearLayout {
    enum Workspace { VIDEO,AUDIO,IMAGES,ANALYSIS }
    final Button video,audio,images,analysis;
    AppNavigation(AppUi ui,boolean audioSelected){
        this(ui,audioSelected?Workspace.AUDIO:Workspace.VIDEO);
    }
    AppNavigation(AppUi ui,Workspace selected){this(ui,selected,false);}
    AppNavigation(AppUi ui,Workspace selected,boolean compact){
        super(ui.activity);setOrientation(HORIZONTAL);setBackgroundColor(ui.bg);
        video=tab(ui,"Video",selected==Workspace.VIDEO,()->open(ui,selected,Workspace.VIDEO,MainActivity.class));
        audio=tab(ui,"Audio",selected==Workspace.AUDIO,()->open(ui,selected,Workspace.AUDIO,AudioActivity.class));
        images=tab(ui,"Immagini",selected==Workspace.IMAGES,()->open(ui,selected,Workspace.IMAGES,ImageActivity.class));
        analysis=tab(ui,"Analisi",selected==Workspace.ANALYSIS,()->open(ui,selected,Workspace.ANALYSIS,AnalysisActivity.class));
        if(compact)images.setText("Foto");
        if(compact)for(Button b:new Button[]{video,audio,images,analysis}){
            LinearLayout column=(LinearLayout)b.getParent();column.removeViewAt(1);
            b.setTextSize(11);b.setTypeface(null,Typeface.NORMAL);
            int resource=b==video?R.drawable.trial_video:b==audio?R.drawable.trial_music:b==images?R.drawable.trial_photo:R.drawable.trial_grid;
            android.graphics.drawable.Drawable icon=ui.activity.getDrawable(resource).mutate();icon.setTint(b.isSelected()?ui.accent:ui.muted);icon.setBounds(0,0,ui.dp(20),ui.dp(20));b.setCompoundDrawables(null,icon,null,null);b.setCompoundDrawablePadding(0);b.setPadding(ui.dp(2),0,ui.dp(2),0);b.setMinHeight(ui.dp(44));b.setMinimumHeight(ui.dp(44));
            android.graphics.drawable.GradientDrawable pill=ui.shape(b.isSelected()?ui.dark?0xFF344D76:0xFFD5E3FF:android.graphics.Color.TRANSPARENT,24);pill.setStroke(ui.dp(1),b.isSelected()?ui.accent:android.graphics.Color.TRANSPARENT);
            b.setBackground(new RippleDrawable(ColorStateList.valueOf(ui.border),pill,null));
            column.setLayoutParams(new LinearLayout.LayoutParams(0,-1,1));
        }
    }
    private static void open(AppUi ui,Workspace selected,Workspace target,Class<? extends Activity> screen){
        if(selected==target)return;
        if(ui.activity instanceof MainActivity){HomeState state=((MainActivity)ui.activity).home.state;AnalysisSources.video(state.selectedInputs,state.selectedNames);}
        ui.activity.startActivity(new Intent(ui.activity,screen).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        // Video is the task root. Secondary pages persist their draft, then leave the stack.
        if(selected!=Workspace.VIDEO)ui.activity.finish();
        fade(ui.activity);
    }
    /** Equal-width tabs allow another workspace to be added without changing the host layout. */
    private Button tab(AppUi ui,String label,boolean selected,Runnable action){
        LinearLayout column=new LinearLayout(ui.activity);column.setOrientation(VERTICAL);addView(column,new LinearLayout.LayoutParams(0,-2,1));
        Button button=new Button(ui.activity);ui.styleButton(button,false);button.setText(label);button.setTextSize(14);button.setSelected(selected);
        button.setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);button.setTextColor(selected?ui.accent:ui.muted);button.setPadding(ui.dp(2),ui.dp(8),ui.dp(2),ui.dp(8));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(ui.border),null,ui.shape(android.graphics.Color.WHITE,8)));
        button.setContentDescription(label+(selected?" · selezionato":""));column.addView(button,new LinearLayout.LayoutParams(-1,-2));
        android.view.View underline=new android.view.View(ui.activity);underline.setBackgroundColor(selected?ui.accent:ui.border);column.addView(underline,new LinearLayout.LayoutParams(-1,ui.dp(2)));
        button.setOnClickListener(v->action.run());return button;
    }
    static void fade(Activity activity){activity.overridePendingTransition(R.anim.workspace_fade_in,R.anim.workspace_fade_out);}
}
