package it.edo.codecprobe;

import android.app.*;
import android.content.*;
import android.os.Looper;
import android.view.*;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class HomePlaybackControlsTest {
    @Test public void pauseAndResumeSendSessionCommandsAndSkipRequiresCurrentFileConfirmation()throws Exception{
        ConversionService.State before=ConversionService.snapshot();
        Field field=ConversionService.class.getDeclaredField("state");field.setAccessible(true);ConversionService.State actual=(ConversionService.State)field.get(null);
        actual.hasSession=false;actual.running=true;actual.startedAt=1337;actual.batchIndex=2;actual.batchTotal=3;actual.source="clip.mp4";actual.fileActive=true;
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;home.conversion.renderConversionDashboard(ConversionService.snapshot());
            assertEquals(View.VISIBLE,home.screen.conversion.playbackActions.getVisibility());home.screen.conversion.pause.performClick();
            Intent pause=Shadows.shadowOf(home.activity.getApplication()).getNextStartedService();assertEquals(ConversionService.PAUSE,pause.getAction());assertEquals(1337,pause.getLongExtra(MainActivity.CANCEL_SESSION,-1));
            actual.pauseRequested=true;actual.paused=true;home.conversion.renderConversionDashboard(ConversionService.snapshot());
            assertEquals("Riprendi",home.screen.conversion.pause.getText().toString());home.screen.conversion.pause.performClick();
            assertEquals(ConversionService.RESUME,Shadows.shadowOf(home.activity.getApplication()).getNextStartedService().getAction());
            home.screen.conversion.skip.performClick();assertNotNull(home.conversion.playback.confirmation);
            home.conversion.playback.confirmation.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNull(Shadows.shadowOf(home.activity.getApplication()).getNextStartedService());
            home.screen.conversion.skip.performClick();home.conversion.playback.confirmation.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            Intent skip=Shadows.shadowOf(home.activity.getApplication()).getNextStartedService();assertEquals(ConversionService.SKIP,skip.getAction());assertEquals(2,skip.getIntExtra("fileNumber",-1));
            home.screen.conversion.skip.performClick();actual.batchIndex=3;
            home.conversion.playback.confirmation.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertNull(Shadows.shadowOf(home.activity.getApplication()).getNextStartedService());
            actual.running=false;home.conversion.renderConversionDashboard(ConversionService.snapshot());assertEquals(View.GONE,home.screen.conversion.playbackActions.getVisibility());
        }finally{
            controller.pause().stop().destroy();
            for(Field value:ConversionService.State.class.getDeclaredFields()){value.setAccessible(true);value.set(actual,value.get(before));}
        }
    }
}
