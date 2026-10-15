package it.edo.codecprobe;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;

/** Three direct choices; indexes remain compatible with saved settings and the encoder. */
final class CpuModeButtons extends LinearLayout {
    private static final String[] LABELS={"Limited","Auto","Max"};
    private final Button[] buttons=new Button[3];
    private final int accent,selectedInk,surface,border;
    private int selected;
    CpuModeButtons(Context context,int accent,int selectedInk,int surface,int border){
        super(context);this.accent=accent;this.selectedInk=selectedInk;this.surface=surface;this.border=border;
        setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);
        for(int n=0;n<buttons.length;n++){
            final int index=n;Button button=new Button(context);buttons[n]=button;
            button.setText(LABELS[n]);button.setAllCaps(false);button.setTextSize(16);button.setTypeface(null,Typeface.BOLD);
            button.setSingleLine(false);button.setMinWidth(0);button.setMinimumWidth(0);button.setMinHeight(dp(52));button.setPadding(dp(4),dp(10),dp(4),dp(10));button.setGravity(Gravity.CENTER);
            button.setOnClickListener(v->{if(isEnabled()&&button.isEnabled())setSelection(index);});
            LayoutParams lp=new LayoutParams(0,-2,1);lp.setMargins(dp(3),0,dp(3),0);addView(button,lp);
        }
        setSelection(0);
    }
    int getSelectedItemPosition(){return selected;}
    void setSelection(int value){
        selected=Math.max(0,Math.min(2,value));
        for(int n=0;n<buttons.length;n++){
            boolean active=n==selected;Button button=buttons[n];button.setSelected(active);button.setTextColor(active?selectedInk:accent);
            GradientDrawable shape=new GradientDrawable();shape.setColor(active?accent:surface);shape.setCornerRadius(dp(14));shape.setStroke(dp(1),border);
            button.setBackgroundTintList(null);button.setBackground(new RippleDrawable(ColorStateList.valueOf(border),shape,null));
            button.setContentDescription("Modalità CPU: "+LABELS[n]+(active?", selezionata":""));
            if(Build.VERSION.SDK_INT>=30)button.setStateDescription(active?"Selezionata":"Non selezionata");
        }
    }
    @Override public void setEnabled(boolean enabled){
        super.setEnabled(enabled);setAlpha(enabled?1f:0.5f);
        if(buttons!=null)for(Button button:buttons)if(button!=null)button.setEnabled(enabled);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
