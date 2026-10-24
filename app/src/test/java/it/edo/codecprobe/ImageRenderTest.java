package it.edo.codecprobe;

import android.graphics.*;
import android.view.View;
import java.io.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ImageRenderTest {
    @Test public void settings()throws Exception{
        var controller=Robolectric.buildActivity(ImageSettingsActivity.class).setup();try{ImageSettingsScreen screen=controller.get().screen;View page=screen.page;page.measure(View.MeasureSpec.makeMeasureSpec(screen.ui.dp(360),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());page.getViewTreeObserver().dispatchOnPreDraw();float density=controller.get().getResources().getDisplayMetrics().density;Bitmap bitmap=Bitmap.createBitmap(360,(int)Math.ceil(page.getMeasuredHeight()/density),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(screen.ui.bg);canvas.scale(1/density,1/density);page.draw(canvas);File dir=new File("build/reports/ui");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,"image-settings.png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}}finally{controller.pause().stop().destroy();}
    }
}
