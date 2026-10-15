package it.edo.codecprobe;
import android.view.View;
import android.widget.TextView;
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
public class IncrementalReportTest {
    private Object field(MainActivity a,String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(a);}
    @Test public void reportIsVisibleWhileRunningAndUpdatesWithoutWaitingForQueueEnd()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try{
            Method render=MainActivity.class.getDeclaredMethod("renderSizeReport",ConversionService.State.class);render.setAccessible(true);
            ConversionService.State state=new ConversionService.State();state.running=true;state.sizeReport="Video 1 · 10 MB → 5 MB";
            render.invoke(activity,state);
            assertEquals(View.VISIBLE,((View)field(activity,"sizeReportPanel")).getVisibility());
            assertEquals("RESOCONTO PARZIALE",((TextView)field(activity,"sizeReportTitle")).getText().toString());
            assertEquals(state.sizeReport,((TextView)field(activity,"sizeReport")).getText().toString());
            state.sizeReport+="\nVideo 2 · 20 MB → 8 MB";render.invoke(activity,state);
            assertEquals(state.sizeReport,((TextView)field(activity,"sizeReport")).getText().toString());
            state.running=false;render.invoke(activity,state);
            assertEquals("RESOCONTO CONVERSIONE",((TextView)field(activity,"sizeReportTitle")).getText().toString());
            state.running=true;state.sizeReport="";render.invoke(activity,state);
            assertEquals(View.GONE,((View)field(activity,"sizeReportPanel")).getVisibility());
        }finally{controller.pause().stop().destroy();}
    }
}
