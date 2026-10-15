package it.edo.codecprobe;
import android.widget.*;
import java.lang.reflect.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ConversionDashboardTest {
    private Object field(MainActivity a,String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(a);}
    @Test public void showsFileProgressAndFrozenConfigurationInSeparateRows()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try{
            ((SeekBar)field(activity,"crfSlider")).setProgress(18);
            ConversionService.State state=new ConversionService.State();state.batchIndex=2;state.batchTotal=3;state.succeeded=1;state.running=true;state.source="Secondo.mp4";state.percent=14;state.filePercent=43;
            state.doneFrames=100;state.totalFrames=1000;state.done=2000000;state.total=10000000;state.requestedCrf=28;state.requestedPreset="slow";state.requestedCpu=2;state.bframeCount=4;state.lookaheadCount=20;state.cuTree=1;state.aqMode=2;state.aqStrength=1000;
            Method render=MainActivity.class.getDeclaredMethod("renderConversionDashboard",ConversionService.State.class);render.setAccessible(true);render.invoke(activity,state);
            assertEquals("43%",((TextView)field(activity,"percentLabel")).getText().toString());assertEquals(43,((ProgressBar)field(activity,"progress")).getProgress());
            assertEquals("100 / 1000",((TextView)field(activity,"frameLabel")).getText().toString());
            assertEquals("28",((TextView)field(activity,"activeCrf")).getText().toString());assertEquals("slow",((TextView)field(activity,"activePreset")).getText().toString());
            assertEquals("4",((TextView)field(activity,"activeBframes")).getText().toString());assertEquals("20",((TextView)field(activity,"activeLookahead")).getText().toString());
            assertTrue(((TextView)field(activity,"queueLabel")).getText().toString().contains("Video 2 / 3"));
            Button convert=(Button)field(activity,"convert"),stop=(Button)field(activity,"stop");assertSame(convert.getParent(),stop.getParent());
            LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)convert.getLayoutParams();assertEquals(0,lp.width);assertEquals(1,lp.weight,0);
            assertNotNull(field(activity,"ramValue"));assertNotNull(field(activity,"batteryValue"));
        }finally{controller.pause().stop().destroy();}
    }
}
