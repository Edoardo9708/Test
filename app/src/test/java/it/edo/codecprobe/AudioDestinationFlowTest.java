package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class AudioDestinationFlowTest {
    @Before public void reset(){var context=RuntimeEnvironment.getApplication();for(String prefs:new String[]{"audio-workspace","audio-startup-settings"})context.getSharedPreferences(prefs,0).edit().clear().commit();ShadowContentResolver.registerProviderInternal("destinations",new MediaDestinationsTest.Directories());}
    private static void ready(AudioController audio)throws Exception{
        long end=System.currentTimeMillis()+5000;
        while(!audio.destinations.ready()&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}
        Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(audio.destinations.ready());
    }
    @Test public void audioUsesVerifiedPerFileFoldersAndKeepsThemAcrossRecreation()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            AudioController audio=controller.get().audio;assertEquals(View.GONE,audio.screen.destinations.body.getVisibility());assertFalse(audio.screen.start.isEnabled());
            MediaDestinationsTest.grant(controller.get(),MediaDestinationsTest.A);
            audio.state.folder=MediaDestinationsTest.A;
            audio.state.inputs.add("content://songs/one");audio.state.names.add("one.flac");audio.state.inputs.add("content://songs/two");audio.state.names.add("two.flac");audio.render();ready(audio);
            assertTrue(audio.screen.start.isEnabled());audio.state.selectedIndex=1;audio.render();audio.screen.destinations.pickFolder.performClick();
            assertTrue(audio.destinations.choosing());assertFalse(audio.screen.start.isEnabled());
            controller.recreate();audio=controller.get().audio;assertTrue(audio.destinations.choosing());
            controller.get().onActivityResult(MediaDestinationsController.FILE,Activity.RESULT_OK,new Intent().setData(Uri.parse(MediaDestinationsTest.B)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));ready(audio);
            assertEquals(java.util.Arrays.asList(MediaDestinationsTest.A,MediaDestinationsTest.B),audio.destinations.snapshot());
            audio.screen.start.performClick();
            Intent command=null;long end=System.currentTimeMillis()+5000;
            while(command==null&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();command=Shadows.shadowOf(controller.get()).getNextStartedService();Thread.sleep(5);}
            assertNotNull("Audio queue should be launched",command);assertEquals(java.util.Arrays.asList(MediaDestinationsTest.A,MediaDestinationsTest.B),command.getStringArrayListExtra("folders"));
            assertEquals(AudioConversionService.START,command.getAction());assertFalse(audio.screen.destinations.pickFolder.isEnabled());
        }finally{controller.pause().stop().destroy();reset();}
    }
}
