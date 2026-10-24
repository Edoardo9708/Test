package it.edo.codecprobe;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.util.LruCache;
import android.util.Size;
import android.widget.Button;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/** At most one decode in flight plus the visible strip; no full-size bitmap retained. */
final class PhotoThumbnails {
    private final AppUi ui;
    private final SelectedItemView view;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ArrayDeque<String> pending=new ArrayDeque<>();
    private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(2*1024*1024){
        @Override protected int sizeOf(String key,Bitmap value){return value.getAllocationByteCount();}
    };
    private List<String> sources=Collections.emptyList();
    private boolean running,closed,paused;
    private final int[] placeholders={R.drawable.minion_01,R.drawable.minion_02,R.drawable.minion_03,R.drawable.minion_04,R.drawable.minion_05,R.drawable.minion_06,R.drawable.minion_07,R.drawable.minion_08,R.drawable.minion_09,R.drawable.minion_10,R.drawable.minion_11,R.drawable.minion_12,R.drawable.minion_13,R.drawable.minion_14,R.drawable.minion_15,R.drawable.minion_16,R.drawable.minion_17,R.drawable.minion_18,R.drawable.minion_19,R.drawable.minion_20};
    PhotoThumbnails(AppUi ui,SelectedItemView view){
        this.ui=ui;this.view=view;
        view.carousel.setOnScrollChangeListener((v,x,y,ox,oy)->request());
        view.carousel.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->request());
    }
    void bind(List<String> inputs){
        int count=Math.min(inputs.size(),view.chips.size());
        List<String> visible=inputs.subList(0,count);
        if(!sources.equals(visible))sources=new ArrayList<>(visible);
        request();
    }
    void active(boolean active){paused=!active;if(active)request();else synchronized(pending){pending.clear();}}
    void invalidate(Collection<String> inputs){for(String input:inputs)cache.remove(input);request();}
    void request(){
        if(closed||paused||!view.carousel.isShown()||view.carousel.getWidth()==0)return;
        int left=view.carousel.getScrollX()-ui.dp(72),right=left+view.carousel.getWidth()+ui.dp(144);
        ArrayList<String> wanted=new ArrayList<>();
        for(int n=0;n<Math.min(sources.size(),view.chips.size());n++){
            Button chip=view.chips.get(n);if(chip.getRight()<left||chip.getLeft()>right){Drawable bg=chip.getBackground();if(bg instanceof Tile)((Tile)bg).bitmap=null;continue;}
            String input=sources.get(n);Bitmap bitmap=cache.get(input);
            if(bitmap!=null)apply(n,input,bitmap);else wanted.add(input);
        }
        synchronized(pending){
            pending.clear();pending.addAll(wanted);
            if(running||pending.isEmpty())return;running=true;
        }
        worker.execute(this::drain);
    }
    private void drain(){
        while(true){
            String input;synchronized(pending){if(closed||paused||pending.isEmpty()){running=false;return;}input=pending.removeFirst();}
            Bitmap bitmap=cache.get(input);if(bitmap==null){bitmap=decode(input);if(bitmap!=null)cache.put(input,bitmap);}
            Bitmap result=bitmap;
            main.post(()->{if(closed||paused)return;int n=sources.indexOf(input);if(n>=0&&result!=null)apply(n,input,result);});
        }
    }
    private Bitmap decode(String value){
        Uri uri=Uri.parse(value);
        try{
            if(!"file".equals(uri.getScheme())){
                try{Bitmap thumbnail=ui.activity.getContentResolver().loadThumbnail(uri,new Size(192,192),null);if(thumbnail!=null)return thumbnail;}
                catch(IOException|RuntimeException unavailable){/* Providers without thumbnail support use a bounded direct decode below. */}
            }
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(ui.activity.getContentResolver(),uri),(decoder,info,source)->{
                Size size=info.getSize();float scale=Math.min(1f,192f/Math.max(size.getWidth(),size.getHeight()));
                decoder.setTargetSize(Math.max(1,Math.round(size.getWidth()*scale)),Math.max(1,Math.round(size.getHeight()*scale)));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
        }catch(IOException|RuntimeException error){
            // A preview failure does not reject a source. The metadata and conversion paths report it independently.
            return BitmapFactory.decodeResource(ui.activity.getResources(),placeholders[Math.floorMod(value.hashCode(),placeholders.length)]);
        }
    }
    private void apply(int index,String input,Bitmap bitmap){
        if(index>=view.chips.size()||!input.equals(sources.get(index)))return;
        Drawable background=view.chips.get(index).getBackground();
        if(background instanceof Tile){Tile tile=(Tile)background;if(tile.bitmap!=bitmap){tile.bitmap=bitmap;tile.invalidateSelf();}}
    }
    void close(){closed=true;synchronized(pending){pending.clear();}worker.shutdownNow();main.removeCallbacksAndMessages(null);cache.evictAll();sources=Collections.emptyList();}
    static final class Tile extends Drawable {
        Bitmap bitmap;private final AppUi ui;private final Button chip;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final RectF rect=new RectF();
        Tile(AppUi ui,Button chip){this.ui=ui;this.chip=chip;}
        @Override public void draw(Canvas canvas){
            rect.set(getBounds());float radius=ui.dp(14);paint.setColor(ui.surface);paint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(rect,radius,radius,paint);
            if(bitmap!=null){int save=canvas.save();Path clip=new Path();clip.addRoundRect(rect,radius,radius,Path.Direction.CW);canvas.clipPath(clip);
                float scale=Math.max(rect.width()/bitmap.getWidth(),rect.height()/bitmap.getHeight());float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;
                canvas.drawBitmap(bitmap,null,new RectF(rect.centerX()-w/2,rect.centerY()-h/2,rect.centerX()+w/2,rect.centerY()+h/2),paint);canvas.restoreToCount(save);}
            paint.setColor(chip.isSelected()?ui.accent:ui.border);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(ui.dp(chip.isSelected()?3:1));rect.inset(ui.dp(2),ui.dp(2));canvas.drawRoundRect(rect,radius,radius,paint);paint.setStyle(Paint.Style.FILL);
        }
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] state){invalidateSelf();return true;}
    }
}
