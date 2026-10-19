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
    @Test public void scrollingDoesNotSelectAndCachedSelectionDoesNotReadAgain()throws Exception{
        VideoMetadataCache.permissions("false:false");VideoMetadataCache.clear();long epoch=VideoMetadataCache.epoch();
        String first="content://test/video/one",second="content://test/video/two";
        VideoMetadataCache.put(first,new VideoMetadataCache.Entry("Primo video.mp4","Prima sintesi",new InputInspector.Report("primo","dettagli primo")),epoch);
        VideoMetadataCache.put(second,new VideoMetadataCache.Entry("Secondo video.mp4","Seconda sintesi",new InputInspector.Report("secondo","dettagli secondo")),epoch);
        Bundle state=new Bundle();state.putStringArrayList("inputs",new ArrayList<>(Arrays.asList(first,second)));state.putStringArrayList("names",new ArrayList<>(Arrays.asList("Primo video.mp4","Secondo video.mp4")));state.putString("input",first);
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create(state).start().resume().visible();
        MainActivity activity=controller.get();
        try{
            assertFalse((Boolean)activity.home.state.metadataLoading);
            assertEquals("Prima sintesi",((TextView)activity.home.screen.files.inputInfo).getText().toString());
            HorizontalScrollView carousel=(HorizontalScrollView)activity.home.screen.files.videoCarousel;carousel.scrollTo(200,0);
            assertEquals(0,activity.home.state.selectedVideoIndex);
            ArrayList<?> chips=(ArrayList<?>)activity.home.screen.files.videoChips;assertEquals(2,chips.size());
            ((Button)chips.get(1)).performClick();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals(1,activity.home.state.selectedVideoIndex);
            assertEquals("Seconda sintesi",((TextView)activity.home.screen.files.inputInfo).getText().toString());
            assertFalse((Boolean)activity.home.state.metadataLoading);assertNull(activity.home.selection.metadataDialog);
        }finally{controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }
    @Test public void removingSelectedVideoKeepsTheRemainingFileAndItsDestination()throws Exception{
        VideoMetadataCache.permissions("false:false");VideoMetadataCache.clear();long epoch=VideoMetadataCache.epoch();
        ArrayList<String> inputs=new ArrayList<>(Arrays.asList("content://test/video/one","content://test/video/two"));ArrayList<String> names=new ArrayList<>(Arrays.asList("one.mp4","two.mp4"));
        for(int n=0;n<2;n++)VideoMetadataCache.put(inputs.get(n),new VideoMetadataCache.Entry(names.get(n),"Sintesi",new InputInspector.Report("info","dettagli")),epoch);
        Bundle saved=new Bundle();saved.putStringArrayList("inputs",inputs);saved.putStringArrayList("names",names);saved.putString("input",inputs.get(0));
        var controller=Robolectric.buildActivity(MainActivity.class).create(saved).start().resume().visible();try{
            HomeController home=controller.get().home;home.state.destinationPlan.setFolder(0,"content://dirs/tree/first");home.state.destinationPlan.setFolder(1,"content://dirs/tree/second");
            home.screen.files.videoChips.get(1).performClick();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));home.screen.files.remove.performClick();
            assertEquals(Arrays.asList(inputs.get(0)),home.state.selectedInputs);assertEquals("content://dirs/tree/first",home.state.destinationPlan.effective(0).folder);assertEquals(inputs.get(0),home.state.input.toString());assertEquals(1,home.screen.files.videoChips.size());
            home.state.busy=true;home.selection.removeVideo();assertEquals(1,home.state.selectedInputs.size());
        }finally{controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }

}

