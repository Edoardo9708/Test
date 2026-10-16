package it.edo.codecprobe;

import android.content.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class MemoryProtectionTest {
    @Test public void detectsUhdOrientationAndCropWithoutChangingSmallerVideos(){
        assertTrue(MemoryProtection.required(3840,2160));assertTrue(MemoryProtection.required(2160,3840));
        assertTrue(MemoryProtection.required(3840,1600));
        assertFalse(MemoryProtection.required(1920,1080));assertFalse(MemoryProtection.required(1080,1920));
        assertFalse(MemoryProtection.required(2560,1440));
    }

    @Test public void effectiveLabelSurvivesCheckpointThenResetsForNextFileAndResume()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Intent request=BatchRetryTest.queue(context);
        ConversionService.State state=SavedSessionTest.sample(request);state.memoryProtection=true;
        state.bframeCount=4;state.lookaheadCount=5;state.cuTree=0;
        try{
            SavedSession.save(context,request,state);SavedSession saved=SavedSession.load(context);
            assertTrue(saved.state.memoryProtection);assertEquals(2,saved.state.requestedCpu);
            assertEquals(2,saved.resumeRequest().getIntExtra("cpu",-1));
            var activity=Robolectric.buildActivity(MainActivity.class).setup();
            try{
                HomeController home=activity.get().home;
                saved.state.running=true;home.conversion.renderConversionDashboard(saved.state);
                assertEquals("Limited · protezione memoria 4K",home.screen.conversion.activeCpu.getText().toString());
                assertEquals("4",home.screen.conversion.activeBframes.getText().toString());
                assertEquals("5",home.screen.conversion.activeLookahead.getText().toString());
                assertEquals("OFF",home.screen.conversion.activeTree.getText().toString());
                ConversionService.State next=new ConversionService.State(saved.state);ConversionSessionState.resetFile(next);
                home.conversion.renderConversionDashboard(next);
                assertFalse(next.memoryProtection);assertEquals("Max",home.screen.conversion.activeCpu.getText().toString());
                assertEquals("in attesa",home.screen.conversion.activeLookahead.getText().toString());
                ConversionService.State resumed=ConversionSessionState.begin(saved.resumeRequest(),saved,saved.state.items,1000);
                assertFalse(resumed.memoryProtection);assertEquals(2,resumed.requestedCpu);
                assertEquals(saved.state.outputUris,resumed.outputUris);assertEquals(saved.state.elapsedBefore,resumed.elapsedBefore);
            }finally{activity.pause().stop().destroy();}
        }finally{SavedSession.clear(context);}
    }
}
