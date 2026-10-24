package it.edo.codecprobe;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.os.*;
import android.view.View;
import android.widget.ScrollView;

/** Shared scene: one cached display list, no full-screen bitmap copies per panel. */
final class PhotoBackdrop extends View {
    private final AppUi ui;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final int[] sourceLocation=new int[2],targetLocation=new int[2];
    private final RenderNode scene=new RenderNode("photo-background"),overlay=new RenderNode("photo-chrome");
    private final java.util.WeakHashMap<View,Boolean> owners=new java.util.WeakHashMap<>();
    void register(View owner){owners.put(owner,Boolean.TRUE);}
    private void redrawOwners(){for(View owner:owners.keySet())if(owner.isShown())owner.invalidate();}
    private ScrollView content;private boolean active,working,capturing,overlayDirty=true;private int revision,recorded=-1;
    private long animatedMs,lastFrame;private float drift;
    private final Runnable frame=new Runnable(){public void run(){
        if(!canAnimate())return;long now=SystemClock.uptimeMillis();animatedMs+=Math.min(100,now-lastFrame);lastFrame=now;
        drift=(float)Math.sin(animatedMs*Math.PI/18000);revision++;overlayDirty=true;invalidate();redrawOwners();
        if(getParent() instanceof View)((View)getParent()).invalidate();
        if(canAnimate())handler.postDelayed(this,67);
    }};
    PhotoBackdrop(AppUi ui){super(ui.activity);this.ui=ui;ui.backdrop=this;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void content(ScrollView view){content=view;view.setOnScrollChangeListener((v,x,y,oldX,oldY)->contentChanged());}
    void active(boolean value){active=value;schedule();}
    void working(boolean value){if(working==value)return;working=value;contentChanged();schedule();}
    boolean isWorking(){return working;}
    void contentChanged(){overlayDirty=true;redrawOwners();if(getParent() instanceof View)((View)getParent()).invalidate();}
    private boolean canAnimate(){return active&&!working&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE&&animatedMs<36000&&ValueAnimator.areAnimatorsEnabled();}
    private void schedule(){handler.removeCallbacks(frame);lastFrame=SystemClock.uptimeMillis();if(canAnimate())handler.postDelayed(frame,67);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();schedule();}
    @Override protected void onDetachedFromWindow(){handler.removeCallbacks(frame);scene.discardDisplayList();overlay.discardDisplayList();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);schedule();}
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){recorded=-1;overlayDirty=true;}
    @Override protected void onDraw(Canvas canvas){paintScene(canvas);}
    private void paintScene(Canvas canvas){
        canvas.drawColor(ui.bg);float d=ui.dp(1),x=drift*8*d,y=drift*14*d;
        paint.setStyle(Paint.Style.FILL);paint.setColor(ui.dark?0x338C703B:0x99F9D99B);canvas.drawCircle(getWidth()*.89f+x,getHeight()*.08f+y,58*d,paint);
        paint.setColor(ui.dark?0x3342618C:0x88C9DAFB);canvas.drawCircle(getWidth()*.05f-x,getHeight()*.34f-y,80*d,paint);
        paint.setColor(ui.dark?0x338C703B:0x88F9D99B);canvas.drawCircle(getWidth()*.96f+x,getHeight()*.64f+y,75*d,paint);
        paint.setColor(ui.dark?0x3342618C:0x88C9DAFB);canvas.drawCircle(getWidth()*.06f-x,getHeight()*.91f-y,100*d,paint);
        paint.setColor(ui.dark?0x4D7F9EC5:0x7A7891B7);paint.setStrokeWidth(1.8f*d);
        sparkle(canvas,getWidth()*.94f-x,getHeight()*.29f+y,10*d);sparkle(canvas,getWidth()*.08f+x,getHeight()*.74f-y,8*d);
    }
    private void sparkle(Canvas c,float x,float y,float r){c.drawLine(x-r,y,x+r,y,paint);c.drawLine(x,y-r,x,y+r,paint);}
    void drawBehind(Canvas canvas,View target,boolean chrome){
        if(getWidth()==0||getHeight()==0)return;
        getLocationInWindow(sourceLocation);target.getLocationInWindow(targetLocation);
        int saved=canvas.save();canvas.translate(sourceLocation[0]-targetLocation[0],sourceLocation[1]-targetLocation[1]);
        if(Build.VERSION.SDK_INT<31||!canvas.isHardwareAccelerated()||working||capturing){paintScene(canvas);canvas.restoreToCount(saved);return;}
        if(recorded!=revision||!scene.hasDisplayList()){
            scene.setPosition(0,0,getWidth(),getHeight());Canvas recording=scene.beginRecording();paintScene(recording);scene.endRecording();
            scene.setRenderEffect(RenderEffect.createBlurEffect(ui.dp(5),ui.dp(5),Shader.TileMode.CLAMP));recorded=revision;
        }
        if(chrome&&content!=null){
            if(overlayDirty||!overlay.hasDisplayList()){
                overlay.setPosition(0,0,getWidth(),getHeight());Canvas recording=overlay.beginRecording();paintScene(recording);
                int save=recording.save();recording.translate(content.getLeft()-getLeft(),content.getTop()-getTop());capturing=true;
                try{content.draw(recording);}finally{capturing=false;recording.restoreToCount(save);overlay.endRecording();}
                overlay.setRenderEffect(RenderEffect.createBlurEffect(ui.dp(9),ui.dp(9),Shader.TileMode.CLAMP));overlayDirty=false;
            }
            canvas.drawRenderNode(overlay);
        }else canvas.drawRenderNode(scene);
        canvas.restoreToCount(saved);
    }
}
