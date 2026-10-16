package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class HomeSavedSessionTest {
    @Test public void eachSuccessfulReportButtonOpensExactlyItsOwnFile(){
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;ConversionService.State state=new ConversionService.State();
            for(int n=0;n<2;n++){
                state.items.add(new CompressionSummary.Item(n+1,"clip"+n+".mp4",CompressionSummary.Status.COMPLETE,1000,500));
                state.outputUris.add("content://results/"+n);state.results.add(Uri.parse(state.outputUris.get(n)));
            }
            state.sizeReport=CompressionSummary.render(state.items);home.conversion.renderSizeReport(state);
            for(int n=0;n<2;n++){
                LinearLayout row=(LinearLayout)home.screen.conversion.failureReport.getChildAt(n);
                Button button=(Button)row.getChildAt(1);assertEquals("Apri",button.getText().toString());assertTrue(button.getContentDescription().toString().contains("clip"+n));
                button.performClick();Intent opened=Shadows.shadowOf(home.activity).getNextStartedActivity();
                assertEquals(Intent.ACTION_VIEW,opened.getAction());assertEquals(Uri.parse("content://results/"+n),opened.getData());assertTrue((opened.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
            }
            home.conversion.renderSizeReport(new ConversionService.State());assertEquals(0,home.screen.conversion.failureReport.getChildCount());
        }finally{controller.pause().stop().destroy();}
    }

    @Test public void freshActivityShowsRecoveryWithoutStartingAndNewQueueRequiresReplacementConfirmation()throws Exception{
        var context=RuntimeEnvironment.getApplication();Intent request=BatchRetryTest.queue(context);
        ConversionService.State before=ConversionService.snapshot();Field field=ConversionService.class.getDeclaredField("state");field.setAccessible(true);
        SavedSession.save(context,request,SavedSessionTest.sample(request));field.set(null,new ConversionService.State());
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;
            assertFalse(ConversionService.snapshot().running);assertTrue(ConversionService.snapshot().recoverable);
            assertEquals(View.VISIBLE,home.screen.conversion.savedSession.getVisibility());assertEquals("Riprendi sessione salvata",home.screen.conversion.savedSession.getText().toString());
            assertTrue(home.screen.conversion.sizeReport.getText().toString().contains("0,50 kB"));
            home.screen.conversion.savedSession.performClick();assertTrue(home.conversion.savedSession.dialog.isShowing());
            home.conversion.savedSession.dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertFalse(ConversionService.snapshot().running);
            home.conversion.outputCheck.begin(BatchRetryTest.queue(context));assertFalse(home.conversion.outputCheck.checking);
            assertTrue(home.conversion.savedSession.dialog.isShowing());home.conversion.savedSession.dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            assertEquals("saved-test-session",SavedSession.load(context).state.sessionId);
            Intent retry=BatchRetry.select(SavedSession.load(context).resumeRequest(),List.of(2));assertFalse(retry.hasExtra(SavedSession.RESTORE));assertFalse(retry.hasExtra("finishedFiles"));
        }finally{controller.pause().stop().destroy();field.set(null,before);SavedSession.clear(context);}
    }
}
