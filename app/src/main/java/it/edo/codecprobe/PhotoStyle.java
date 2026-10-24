package it.edo.codecprobe;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.transition.*;
import android.view.*;
import android.widget.*;

/** Photo reference tokens and grouped surfaces; no file or selection state. */
final class PhotoStyle {
    static LinearLayout group(AppUi ui,LinearLayout parent,int top){
        LinearLayout group=new LinearLayout(ui.activity);group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(ui.dp(14),ui.dp(12),ui.dp(14),ui.dp(14));
        group.setBackground(glass(ui,group,24,false,true));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=ui.dp(top);parent.addView(group,lp);return group;
    }
    static TextView heading(AppUi ui,LinearLayout parent,String text,int icon){
        TextView title=ui.text(parent,text,14);title.setTextColor(ui.muted);title.setAccessibilityHeading(true);
        title.setPadding(ui.dp(6),ui.dp(28),ui.dp(6),ui.dp(12));
        if(icon!=0)TrialIcons.set(ui,title,icon,false);return title;
    }
    static Drawable glass(AppUi ui,View owner,int radius,boolean chrome,boolean border){
        float r=ui.dp(radius);return new Glass(ui,owner,new float[]{r,r,r,r,r,r,r,r},chrome,border);
    }
    /** Positions 0/1/2 preserve large external corners and small internal corners. */
    static void joined(AppUi ui,View view,int position,boolean vertical){
        float big=ui.dp(22),small=ui.dp(5);
        float[] radii=vertical?new float[]{position==0?big:small,position==0?big:small,position==0?big:small,position==0?big:small,position==2?big:small,position==2?big:small,position==2?big:small,position==2?big:small}:
            new float[]{position==0?big:small,position==0?big:small,position==2?big:small,position==2?big:small,position==2?big:small,position==2?big:small,position==0?big:small,position==0?big:small};
        view.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(ui.border),new Glass(ui,view,radii,false,true),null));
        if(view.getLayoutParams() instanceof LinearLayout.LayoutParams){LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)view.getLayoutParams();lp.setMargins(0,vertical?ui.dp(4):0,vertical||position==2?0:ui.dp(4),0);view.setLayoutParams(lp);}
    }
    static void visibility(View view,boolean visible){
        int state=visible?View.VISIBLE:View.GONE;if(view.getVisibility()==state)return;
        if(view.isLaidOut()&&ValueAnimator.areAnimatorsEnabled()&&view.getParent() instanceof ViewGroup){
            TransitionManager.beginDelayedTransition((ViewGroup)view.getParent(),new TransitionSet().addTransition(new ChangeBounds()).addTransition(new Fade()).setDuration(180));
        }
        view.setVisibility(state);
    }
    private static final class Glass extends Drawable {
        private final AppUi ui;private final View owner;private final float[] radii;private final boolean chrome,border;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path=new Path();private final RectF rect=new RectF();
        Glass(AppUi ui,View owner,float[] radii,boolean chrome,boolean border){this.ui=ui;this.owner=owner;this.radii=radii;this.chrome=chrome;this.border=border;}
        @Override public void draw(Canvas canvas){
            rect.set(getBounds());rect.inset(.5f,.5f);path.reset();path.addRoundRect(rect,radii,Path.Direction.CW);
            int saved=canvas.save();canvas.clipPath(path);
            if(ui.backdrop!=null){ui.backdrop.register(owner);ui.backdrop.drawBehind(canvas,owner,chrome);}
            int alpha=ui.backdrop!=null&&ui.backdrop.isWorking()?240:chrome?185:ui.dark?189:173;
            paint.setStyle(Paint.Style.FILL);paint.setColor((ui.surface&0xFFFFFF)|(alpha<<24));canvas.drawPath(path,paint);canvas.restoreToCount(saved);
            if(border){paint.setColor(ui.border);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(ui.dp(1));canvas.drawPath(path,paint);}
        }
        @Override public void setAlpha(int alpha){}
        @Override public void setColorFilter(ColorFilter filter){}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private PhotoStyle(){}
}
