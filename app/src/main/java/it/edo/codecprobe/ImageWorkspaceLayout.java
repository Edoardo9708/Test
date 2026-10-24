package it.edo.codecprobe;

import android.os.Build;
import android.view.*;
import android.widget.*;

/** One scrolling surface with inset-aware floating navigation. No nested vertical scroller. */
final class ImageWorkspaceLayout {
    final FrameLayout root;
    final ScrollView scroll;
    final LinearLayout footer,actions;
    final Button back,forward;
    private final ImageSteps steps;

    ImageWorkspaceLayout(AppUi ui,LinearLayout page,AppNavigation navigation,ImageSteps steps){
        this.steps=steps;
        root=new FrameLayout(ui.activity);root.setBackgroundColor(ui.bg);
        // A static gradient gives the translucent panels a backdrop without idle animation work.
        root.setBackground(new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            ui.dark?new int[]{0xFF172135,0xFF26384D,0xFF172135}:new int[]{0xFFF7F8FC,0xFFD9E7FD,0xFFFFF2DC}));
        View title=page.getChildAt(0);page.removeView(title);
        title.setBackground(ui.shape(ui.dark?0xED172135:0xEDF4F7FC,22));
        title.setPadding(ui.dp(16),ui.dp(8),ui.dp(16),ui.dp(8));
        scroll=new ScrollView(ui.activity);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        FrameLayout widthHost=new FrameLayout(ui.activity);scroll.addView(widthHost,new ScrollView.LayoutParams(-1,-2));
        widthHost.addView(page,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL));
        root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        FrameLayout.LayoutParams top=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);top.setMargins(ui.dp(12),0,ui.dp(12),0);root.addView(title,top);
        footer=new LinearLayout(ui.activity);footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(ui.dp(16),ui.dp(6),ui.dp(16),ui.dp(8));
        actions=new LinearLayout(ui.activity);
        back=ui.halfButton(actions,"Indietro",false);forward=ui.halfButton(actions,"Procedi al punto 2",true);
        for(Button b:new Button[]{back,forward}){b.setMinHeight(ui.dp(48));b.setMinimumHeight(ui.dp(48));b.setPadding(ui.dp(12),ui.dp(8),ui.dp(12),ui.dp(8));}
        footer.addView(actions,new LinearLayout.LayoutParams(-1,-2));
        navigation.setPadding(ui.dp(5),ui.dp(5),ui.dp(5),ui.dp(5));
        navigation.setBackground(ui.shape(ui.dark?0xF0263549:0xF0E9EEF8,28));
        footer.addView(navigation,new LinearLayout.LayoutParams(-1,ui.dp(56)));
        root.addView(footer,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        Runnable padding=()->scroll.setPadding(0,title.getHeight()+ui.dp(4),0,footer.getHeight()+ui.dp(8));
        title.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->padding.run());
        footer.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->padding.run());
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(i.left,i.top,i.right,i.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        TrialIcons.set(ui,back,R.drawable.trial_back,false);TrialIcons.set(ui,forward,R.drawable.trial_next,true);
        back.setOnClickListener(v->back());
        forward.setOnClickListener(v->steps.focusCard(ImageScreen.FILES.equals(steps.current())?ImageScreen.COMPRESSION:ImageScreen.HEIC));
        steps.changed=()->{refresh();scroll.scrollTo(0,0);};refresh();
        ui.activity.setContentView(root);root.requestApplyInsets();ui.applySystemBars();
    }
    boolean back(){
        String current=steps.current();
        if(current==null){((android.app.Activity)root.getContext()).finish();return false;}
        if(ImageScreen.COMPRESSION.equals(current))steps.focusCard(ImageScreen.FILES);
        else if(ImageScreen.HEIC.equals(current))steps.focusCard(ImageScreen.COMPRESSION);
        else if(ImageScreen.DETAILS.equals(current))steps.focusCard(ImageScreen.HEIC);
        else steps.home();
        return true;
    }
    void refresh(){
        String current=steps.current();boolean next=ImageScreen.FILES.equals(current)||ImageScreen.COMPRESSION.equals(current);
        forward.setVisibility(next?View.VISIBLE:View.GONE);
        forward.setText(ImageScreen.FILES.equals(current)?"Procedi al punto 2":"Procedi al punto 3");
        back.setText(current==null?"Indietro":ImageScreen.FILES.equals(current)?"Indietro · inizio":"Indietro");
    }
}
