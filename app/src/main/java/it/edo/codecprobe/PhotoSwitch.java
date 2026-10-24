package it.edo.codecprobe;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.widget.CheckBox;

/** Retains CompoundButton semantics and listeners with the reference's trailing switch. */
final class PhotoSwitch extends CheckBox {
    private final AppUi ui;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track=new RectF();
    private float position;
    private ValueAnimator motion;
    PhotoSwitch(AppUi ui){
        super(ui.activity);this.ui=ui;setButtonDrawable(null);setTextSize(13);setTextColor(ui.ink);
        setPadding(0,ui.dp(8),ui.dp(62),ui.dp(8));setMinHeight(ui.dp(60));setGravity(android.view.Gravity.CENTER_VERTICAL);
    }
    @Override public void setChecked(boolean checked){
        boolean changed=isChecked()!=checked;super.setChecked(checked);
        if(!changed||ui==null)return;if(motion!=null)motion.cancel();float target=checked?1:0;
        if(!isLaidOut()||!ValueAnimator.areAnimatorsEnabled()){position=target;invalidate();return;}
        motion=ValueAnimator.ofFloat(position,target);motion.setDuration(160);motion.addUpdateListener(animation->{position=(float)animation.getAnimatedValue();invalidate();});motion.start();
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float right=getWidth(),top=(getHeight()-ui.dp(28))/2f;
        track.set(right-ui.dp(46),top,right,top+ui.dp(28));
        paint.setColor(isChecked()?ui.accent:ui.border);paint.setAlpha(isEnabled()?255:110);canvas.drawRoundRect(track,ui.dp(14),ui.dp(14),paint);
        paint.setColor(isChecked()?ui.bg:ui.surface);paint.setAlpha(isEnabled()?255:150);
        canvas.drawCircle(track.left+ui.dp(14)+position*ui.dp(18),track.centerY(),ui.dp(10),paint);
    }
    @Override public CharSequence getAccessibilityClassName(){return "android.widget.Switch";}
    @Override protected void onDetachedFromWindow(){if(motion!=null)motion.cancel();super.onDetachedFromWindow();}
}
