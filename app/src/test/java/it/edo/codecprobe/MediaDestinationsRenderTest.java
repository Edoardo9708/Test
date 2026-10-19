package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class MediaDestinationsRenderTest {
    @Test public void sameDestinationComponentRendersVerifiedAndMissingAccessForEveryMedia()throws Exception{
        ShadowContentResolver.registerProviderInternal("destinations",new MediaDestinationsTest.Directories());
        for(MediaKind kind:MediaKind.values())for(boolean allowed:new boolean[]{false,true}){
            var controller=Robolectric.buildActivity(Activity.class).setup();MediaDestinationsTest.Harness host=new MediaDestinationsTest.Harness(controller.get(),kind);
            try{
                controller.get().setContentView(host.root);
                try{controller.get().getContentResolver().releasePersistableUriPermission(Uri.parse(MediaDestinationsTest.A),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(SecurityException ignored){}
                if(allowed)MediaDestinationsTest.grant(controller.get(),MediaDestinationsTest.A);
                host.inputs.addAll(Arrays.asList("content://source/1","content://source/2"));host.names.addAll(Arrays.asList("Primo file","Secondo file"));host.controller.render();
                long end=System.currentTimeMillis()+5000;while(host.changes<2&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}
                assertTrue(host.changes>=2);assertEquals(allowed,host.controller.ready());
                View page=host.root;int width=host.ui.dp(360);page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());page.getViewTreeObserver().dispatchOnPreDraw();
                float density=controller.get().getResources().getDisplayMetrics().density;Bitmap bitmap=Bitmap.createBitmap(360,(int)Math.ceil(page.getMeasuredHeight()/density),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(host.ui.bg);canvas.scale(1/density,1/density);page.draw(canvas);
                File dir=new File("build/reports/ui");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,"destination-"+kind.name().toLowerCase(Locale.ROOT)+"-"+(allowed?"granted":"missing")+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
            }finally{host.controller.close();controller.pause().stop().destroy();}
        }
    }
}
