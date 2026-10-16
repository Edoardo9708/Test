package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.RadioButton;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ResumeParametersTest {
    @Test public void currentChoiceChangesOnlyTabTwoAndPersistsWithRecoveredReport()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Intent original=BatchRetryTest.queue(context);
        SavedSession initial=new SavedSession(original,SavedSessionTest.sample(original));
        Intent current=new Intent().putExtra("crf",23).putExtra("preset",5).putExtra("cpu",0).putExtra("bframes",true).putExtra("lookahead",false)
            .putExtra("onlySmaller",false).putExtra("namingAddition","DO NOT COPY");
        Intent keep=ResumeParameters.choose(initial.resumeRequest(),current,false);
        assertEquals(2,keep.getIntExtra("cpu",-1));assertEquals(17,keep.getIntExtra("crf",-1));assertEquals(8,keep.getIntExtra("preset",-1));
        Intent changed=ResumeParameters.choose(initial.resumeRequest(),current,true);
        assertEquals(0,changed.getIntExtra("cpu",-1));assertEquals(23,changed.getIntExtra("crf",-1));assertEquals(5,changed.getIntExtra("preset",-1));
        assertTrue(changed.getBooleanExtra("bframes",false));assertFalse(changed.getBooleanExtra("lookahead",true));assertTrue(changed.getBooleanExtra("onlySmaller",false));
        assertFalse(changed.hasExtra("namingAddition"));assertEquals(original.getStringArrayListExtra("inputs"),changed.getStringArrayListExtra("inputs"));
        assertEquals(original.getStringArrayListExtra("folders"),changed.getStringArrayListExtra("folders"));assertEquals(original.getStringArrayListExtra("outputNames"),changed.getStringArrayListExtra("outputNames"));
        ConversionService.State state=ConversionSessionState.begin(changed,initial,initial.state.items,1000);
        assertEquals(0,state.requestedCpu);assertEquals(23,state.requestedCrf);assertEquals("medium",state.requestedPreset);assertEquals(45000,state.elapsedBefore);
        assertEquals(initial.state.outputUris,state.outputUris);assertEquals(initial.state.failures,state.failures);assertEquals(1,state.succeeded);
        state.startedAt=0;SavedSession.save(context,changed,state);SavedSession loaded=SavedSession.load(context);
        assertEquals(0,loaded.state.requestedCpu);assertEquals(0,loaded.resumeRequest().getIntExtra("cpu",-1));assertEquals("medium",loaded.state.requestedPreset);
        assertEquals(initial.state.sizeReport,loaded.state.sizeReport);assertEquals(initial.state.outputUris,loaded.state.outputUris);
        assertEquals(2,original.getIntExtra("cpu",-1));assertEquals(17,original.getIntExtra("crf",-1));
    }

    private RadioButton option(View view,String prefix){
        if(view instanceof RadioButton&&((RadioButton)view).getText().toString().startsWith(prefix))return (RadioButton)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int n=0;n<group.getChildCount();n++){RadioButton found=option(group.getChildAt(n),prefix);if(found!=null)return found;}}
        return null;
    }

    @Test public void popupDefaultsToSavedAndCurrentChoiceRemainsFrozenAcrossPermissionRecreation()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Field field=ConversionService.class.getDeclaredField("state");field.setAccessible(true);ConversionService.State before=ConversionService.snapshot();
        Intent original=BatchRetryTest.queue(context);SavedSession.save(context,original,SavedSessionTest.sample(original));field.set(null,new ConversionService.State());
        var activity=Robolectric.buildActivity(MainActivity.class).setup();Bundle bundle=new Bundle();
        try{
            HomeController home=activity.get().home;
            Shadows.shadowOf(home.activity).denyPermissions(android.Manifest.permission.ACCESS_MEDIA_LOCATION);
            home.screen.compression.crfSlider.setProgress(23);home.screen.compression.presetSlider.setProgress(5);home.screen.compression.cpuMode.setSelection(0);
            home.conversion.savedSession.confirm();AlertDialog dialog=home.conversion.savedSession.dialog;
            RadioButton saved=option(dialog.getWindow().getDecorView(),"Usa i parametri salvati"),current=option(dialog.getWindow().getDecorView(),"Usa quelli attuali");
            assertNotNull(saved);assertNotNull(current);assertTrue(saved.isChecked());assertTrue(saved.getText().toString().contains("CPU: Max"));assertTrue(current.getText().toString().contains("CPU: Limited"));
            current.performClick();dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            assertNotNull(home.conversion.savedSession.pendingResume);assertEquals(0,home.conversion.savedSession.pendingResume.getIntExtra("cpu",-1));
            home.screen.compression.cpuMode.setSelection(2);assertEquals(0,home.conversion.savedSession.pendingResume.getIntExtra("cpu",-1));
            activity.saveInstanceState(bundle);assertEquals(2,SavedSession.load(context).request.getIntExtra("cpu",-1));
        }finally{activity.pause().stop().destroy();}
        var recreated=Robolectric.buildActivity(MainActivity.class).create(bundle).start().resume();
        try{
            HomeSavedSession recovery=recreated.get().home.conversion.savedSession;
            assertNotNull(recovery.pendingResume);assertEquals(0,recovery.pendingResume.getIntExtra("cpu",-1));
            recovery.onPermissions(new int[]{android.content.pm.PackageManager.PERMISSION_DENIED});assertNull(recovery.pendingResume);
            assertEquals(2,SavedSession.load(context).request.getIntExtra("cpu",-1));assertFalse(ConversionService.snapshot().running);
        }finally{recreated.pause().stop().destroy();field.set(null,before);SavedSession.clear(context);}
    }
}
