package it.edo.codecprobe;

import android.content.Intent;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class VideoSettingsParityTest {
    @Before @After public void clear(){RuntimeEnvironment.getApplication().getSharedPreferences("startup-settings",0).edit().clear().commit();}
    @SuppressWarnings("unchecked") private static <T>T field(SettingsActivity activity,String name)throws Exception{var field=SettingsActivity.class.getDeclaredField(name);field.setAccessible(true);return (T)field.get(activity);}
    private static Button button(View root,String label){if(root instanceof Button&&label.contentEquals(((Button)root).getText()))return (Button)root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int n=0;n<group.getChildCount();n++){Button found=button(group.getChildAt(n),label);if(found!=null)return found;}}return null;}
    @Test public void unsavedVideoSettingsSurviveRecreationAndBackDiscardsThem()throws Exception{
        var controller=Robolectric.buildActivity(SettingsActivity.class).setup();try{
            ((SeekBar)field(controller.get(),"crf")).setProgress(31);((CheckBox)field(controller.get(),"onlySmaller")).setChecked(false);((EditText)field(controller.get(),"addition")).setText("custom-");
            controller.recreate();assertEquals(31,((SeekBar)field(controller.get(),"crf")).getProgress());assertFalse(((CheckBox)field(controller.get(),"onlySmaller")).isChecked());assertEquals("custom-",((EditText)field(controller.get(),"addition")).getText().toString());
            button(controller.get().getWindow().getDecorView(),"← Torna alla conversione").performClick();assertEquals(23,new AppSettings(controller.get()).crf);assertTrue(new AppSettings(controller.get()).onlySmaller);
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void copyingCurrentIncludesFolderAndCommonOptionsAndOnlySaveWritesDefaults()throws Exception{
        Intent current=new Intent().putExtra("crf",29).putExtra("preset",2).putExtra("cpu",0).putExtra("bframes",false).putExtra("lookahead",false).putExtra("folder",MediaDestinationsTest.B).putExtra("onlySmaller",false).putExtra("keepScreenOn",true);
        var controller=Robolectric.buildActivity(SettingsActivity.class,current).setup();try{
            button(controller.get().getWindow().getDecorView(),"Usa la configurazione della schermata conversione").performClick();
            assertEquals(29,((SeekBar)field(controller.get(),"crf")).getProgress());assertEquals(MediaDestinationsTest.B,field(controller.get(),"defaultFolder"));assertTrue(((CheckBox)field(controller.get(),"keepScreenOn")).isChecked());assertEquals(23,new AppSettings(controller.get()).crf);
            button(controller.get().getWindow().getDecorView(),"Salva impostazioni").performClick();AppSettings saved=new AppSettings(controller.get());assertEquals(29,saved.crf);assertEquals(MediaDestinationsTest.B,saved.defaultFolder);assertFalse(saved.onlySmaller);assertTrue(saved.keepScreenOn);
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void settingsFrameRender()throws Exception{
        var controller=Robolectric.buildActivity(SettingsActivity.class).setup();try{
            View page=field(controller.get(),"page");AppUi ui=new AppUi(controller.get());int width=ui.dp(360);page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());page.getViewTreeObserver().dispatchOnPreDraw();float density=controller.get().getResources().getDisplayMetrics().density;
            Bitmap bitmap=Bitmap.createBitmap(360,(int)Math.ceil(page.getMeasuredHeight()/density),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(ui.bg);canvas.scale(1/density,1/density);page.draw(canvas);File dir=new File("build/reports/ui");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,"settings-video.png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
        }finally{controller.pause().stop().destroy();}
    }
}
