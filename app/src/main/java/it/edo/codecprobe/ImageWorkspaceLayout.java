package it.edo.codecprobe;

import android.os.Build;
import android.view.*;
import android.widget.*;

/** One scrolling surface with inset-aware floating navigation. No nested vertical scroller. */
final class ImageWorkspaceLayout {
    final FrameLayout root;
    final PhotoBackdrop backdrop;
    final ScrollView scroll;
    final LinearLayout footer,actions;
    final Button back,forward;
    private final ImageSteps steps;

    ImageWorkspaceLayout(AppUi ui,LinearLayout page,AppNavigation navigation,ImageSteps steps){
        this.steps=steps;
        root=new FrameLayout(ui.activity);root.setBackgroundColor(ui.bg);
        backdrop=new PhotoBackdrop(ui);root.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));
        View title=page.getChildAt(0);page.removeView(title);
        title.setBackground(PhotoStyle.glass(ui,title,0,true,false));
        title.setPadding(ui.dp(22),ui.dp(14),ui.dp(22),ui.dp(14));
        scroll=new ScrollView(ui.activity);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        FrameLayout widthHost=new FrameLayout(ui.activity);scroll.addView(widthHost,new ScrollView.LayoutParams(-1,-2));
        widthHost.addView(page,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL));
        root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));backdrop.content(scroll);
        FrameLayout.LayoutParams top=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);top.setMargins(0,0,0,0);root.addView(title,top);
        footer=new LinearLayout(ui.activity);footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(ui.dp(8),ui.dp(6),ui.dp(8),ui.dp(6));
        footer.setBackground(PhotoStyle.glass(ui,footer,28,true,false));footer.setClickable(true);
        actions=new LinearLayout(ui.activity);actions.setBaselineAligned(false);actions.setGravity(Gravity.CENTER_VERTICAL);
        back=ui.halfButton(actions,"Indietro",false);forward=ui.halfButton(actions,"Procedi al punto 2",true);
        for(Button b:new Button[]{back,forward}){b.setTextSize(12);b.setMinHeight(ui.dp(50));b.setMinimumHeight(ui.dp(50));b.setPadding(ui.dp(8),ui.dp(8),ui.dp(8),ui.dp(8));}
        footer.addView(actions,new LinearLayout.LayoutParams(-1,-2));
        navigation.setPadding(ui.dp(5),ui.dp(5),ui.dp(5),ui.dp(5));
        navigation.setBackground(PhotoStyle.glass(ui,navigation,28,true,true));
        footer.addView(navigation,new LinearLayout.LayoutParams(-1,ui.dp(56)));
        FrameLayout.LayoutParams dock=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);dock.setMargins(ui.dp(10),0,ui.dp(10),ui.dp(6));root.addView(footer,dock);
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{int width=Math.min(ui.dp(440),r-l-ui.dp(20));if(footer.getLayoutParams().width!=width){footer.getLayoutParams().width=width;footer.requestLayout();}});
        Runnable padding=()->scroll.setPadding(0,title.getHeight()+ui.dp(4),0,footer.getHeight()+ui.dp(24));
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
        steps.changed=()->{refresh();scroll.scrollTo(0,0);backdrop.contentChanged();};refresh();
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
        back.setText("Indietro");
    }
}
