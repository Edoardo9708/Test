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
    @Test public void reportIsVisibleWhileRunningAndUpdatesWithoutWaitingForQueueEnd()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try{
            
            ConversionService.State state=new ConversionService.State();state.running=true;state.sizeReport="Video 1 · 10 MB → 5 MB";
            activity.home.conversion.renderSizeReport(state);
            assertEquals(View.VISIBLE,((View)activity.home.screen.conversion.sizeReportPanel).getVisibility());
            assertEquals("RESOCONTO PARZIALE",((TextView)activity.home.screen.conversion.sizeReportTitle).getText().toString());
            assertEquals(state.sizeReport,((TextView)activity.home.screen.conversion.sizeReport).getText().toString());
            state.sizeReport+="\nVideo 2 · 20 MB → 8 MB";activity.home.conversion.renderSizeReport(state);
            assertEquals(state.sizeReport,((TextView)activity.home.screen.conversion.sizeReport).getText().toString());
            state.running=false;activity.home.conversion.renderSizeReport(state);
            assertEquals("RESOCONTO CONVERSIONE",((TextView)activity.home.screen.conversion.sizeReportTitle).getText().toString());
            state.running=true;state.sizeReport="";activity.home.conversion.renderSizeReport(state);
            assertEquals(View.GONE,((View)activity.home.screen.conversion.sizeReportPanel).getVisibility());
        }finally{controller.pause().stop().destroy();}
    }
}

