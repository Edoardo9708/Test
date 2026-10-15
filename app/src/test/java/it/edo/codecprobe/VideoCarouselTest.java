package it.edo.codecprobe;

import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.robolectric.Shadows.shadowOf;
import static org.junit.Assert.*;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class VideoCarouselTest {
    private Object field(MainActivity activity,String name)throws Exception{
        Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);
    }
    @Test public void scrollingDoesNotSelectAndCachedSelectionDoesNotReadAgain()throws Exception{
        VideoMetadataCache.permissions("false:false");VideoMetadataCache.clear();long epoch=VideoMetadataCache.epoch();
        String first="content://test/video/one",second="content://test/video/two";
        VideoMetadataCache.put(first,new VideoMetadataCache.Entry("Primo video.mp4","Prima sintesi",new InputInspector.Report("primo","dettagli primo")),epoch);
        VideoMetadataCache.put(second,new VideoMetadataCache.Entry("Secondo video.mp4","Seconda sintesi",new InputInspector.Report("secondo","dettagli secondo")),epoch);
        Bundle state=new Bundle();state.putStringArrayList("inputs",new ArrayList<>(Arrays.asList(first,second)));state.putStringArrayList("names",new ArrayList<>(Arrays.asList("Primo video.mp4","Secondo video.mp4")));state.putString("input",first);
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create(state).start().resume().visible();
        MainActivity activity=controller.get();
        try{
            assertFalse((Boolean)field(activity,"metadataLoading"));
            assertEquals("Prima sintesi",((TextView)field(activity,"inputInfo")).getText().toString());
            HorizontalScrollView carousel=(HorizontalScrollView)field(activity,"videoCarousel");carousel.scrollTo(200,0);
            assertEquals(0,field(activity,"selectedVideoIndex"));
            ArrayList<?> chips=(ArrayList<?>)field(activity,"videoChips");assertEquals(2,chips.size());
            ((Button)chips.get(1)).performClick();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals(1,field(activity,"selectedVideoIndex"));
            assertEquals("Seconda sintesi",((TextView)field(activity,"inputInfo")).getText().toString());
            assertFalse((Boolean)field(activity,"metadataLoading"));assertNull(field(activity,"metadataDialog"));
        }finally{controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }
}
