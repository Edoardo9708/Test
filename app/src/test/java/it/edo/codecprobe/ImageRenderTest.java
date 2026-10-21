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
    private void render(String card,String filename)throws Exception{
        var context=RuntimeEnvironment.getApplication();context.getSharedPreferences("image-workspace",0).edit().clear().commit();
        context.getSharedPreferences("image-settings",0).edit().clear().commit();var field=ImageConversionService.class.getDeclaredField("state");field.setAccessible(true);Object previous=field.get(null);field.set(null,new ImageRunState());
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=controller.get().images;images.state.inputs.add("content://preview/photo/1");images.state.name="Foto originale con un nome lungo.jpg";images.state.info=JpegInventory.read(new ByteArrayInputStream(ImageWorkspaceTest.fixture()));images.setFolder("content://dirs/tree/primary%3APictures%2FHEIC");
            if(card.equals(ImageScreen.HEIC)){ImageRunState run=new ImageRunState();run.running=true;run.total=3;run.current=2;run.options=images.state.options;run.name="Seconda foto.jpg";run.message="Verifica HEIC, pixel e metadati";run.add(new ImageRunState.Result("Prima foto.jpg",3450000,1800000,"content://saved/1","Salvato",ImageRunState.Outcome.SAVED,"Verifica completata"));field.set(null,run);}images.render();images.screen.sections.focusCard(card.equals(ImageScreen.DETAILS)?ImageScreen.FILES:card);if(card.equals(ImageScreen.DETAILS))images.screen.metadata.performClick();
            View page=images.screen.page;int width=images.screen.ui.dp(360);page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());page.getViewTreeObserver().dispatchOnPreDraw();
            float density=controller.get().getResources().getDisplayMetrics().density;Bitmap bitmap=Bitmap.createBitmap(360,(int)Math.ceil(page.getMeasuredHeight()/density),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(images.screen.ui.bg);canvas.scale(1/density,1/density);page.draw(canvas);
            File dir=new File("build/reports/ui");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,filename))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
        }finally{controller.pause().stop().destroy();field.set(null,previous);context.getSharedPreferences("image-workspace",0).edit().clear().commit();context.getSharedPreferences("image-settings",0).edit().clear().commit();}
    }
    @Test public void files()throws Exception{render(ImageScreen.FILES,"image-files.png");}
    @Test @Config(qualifiers="w360dp-h800dp-night-xhdpi") public void metadataDark()throws Exception{render(ImageScreen.DETAILS,"image-metadata-dark.png");}
    @Test @Config(qualifiers="w360dp-h800dp-night-xhdpi") public void qualityDark()throws Exception{render(ImageScreen.COMPRESSION,"image-quality-dark.png");}
    @Test public void converting()throws Exception{render(ImageScreen.HEIC,"image-conversion.png");}
    @Test public void settings()throws Exception{
        var controller=Robolectric.buildActivity(ImageSettingsActivity.class).setup();try{ImageSettingsScreen screen=controller.get().screen;View page=screen.page;page.measure(View.MeasureSpec.makeMeasureSpec(screen.ui.dp(360),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());page.getViewTreeObserver().dispatchOnPreDraw();float density=controller.get().getResources().getDisplayMetrics().density;Bitmap bitmap=Bitmap.createBitmap(360,(int)Math.ceil(page.getMeasuredHeight()/density),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(screen.ui.bg);canvas.scale(1/density,1/density);page.draw(canvas);File dir=new File("build/reports/ui");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,"image-settings.png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}}finally{controller.pause().stop().destroy();}
    }
}
