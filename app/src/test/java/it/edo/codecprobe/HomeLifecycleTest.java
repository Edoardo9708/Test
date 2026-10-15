package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class HomeLifecycleTest {
    private Bundle oneCachedVideo(){
        VideoMetadataCache.permissions("false:false");VideoMetadataCache.clear();
        String uri="content://test/video/cached";
        VideoMetadataCache.put(uri,new VideoMetadataCache.Entry("Originale.mp4","1 video · 0 audio · 5 MB",new InputInspector.Report("sintesi","dettagli")),VideoMetadataCache.epoch());
        Bundle saved=new Bundle();saved.putString("input",uri);saved.putStringArrayList("inputs",new ArrayList<>(Arrays.asList(uri)));saved.putStringArrayList("names",new ArrayList<>(Arrays.asList("Originale.mp4")));return saved;
    }
    @Test public void recreationKeepsParametersDetailsAndPendingFolderRequest(){
        ActivityController<MainActivity> first=Robolectric.buildActivity(MainActivity.class).create(oneCachedVideo()).start().resume().visible();Bundle saved=new Bundle();HomeController old=first.get().home;
        try{
            old.screen.compression.crfSlider.setProgress(11);old.screen.compression.presetSlider.setProgress(8);old.screen.compression.cpuMode.setSelection(2);
            old.screen.compression.bframeSwitch.setChecked(true);old.screen.compression.lookaheadSwitch.setChecked(false);
            old.screen.files.details.performClick();old.state.choosingOrigins=true;old.state.pendingOriginSource=old.state.input.toString();old.state.pendingOriginParent="primary:DCIM";
            old.screen.sections.focusCard(MainActivity.CARD_CONVERSION);first.saveInstanceState(saved);
        }finally{first.pause().stop().destroy();}
        assertTrue(old.state.destroyed);assertTrue(old.selection.worker.isShutdown());assertTrue(old.system.worker.isShutdown());
        ActivityController<MainActivity> second=Robolectric.buildActivity(MainActivity.class).create(saved).start().resume().visible();
        try{
            HomeController home=second.get().home;assertNotSame(old,home);
            assertEquals(11,home.compression.selectedCrf());assertEquals(8,home.screen.compression.presetSlider.getProgress());assertEquals(2,home.screen.compression.cpuMode.getSelectedItemPosition());
            assertTrue(home.screen.compression.bframeSwitch.isChecked());assertFalse(home.screen.compression.lookaheadSwitch.isChecked());
            assertEquals(View.VISIBLE,home.screen.files.videoDetails.getVisibility());assertEquals("primary:DCIM",home.state.pendingOriginParent);
            assertTrue(home.state.choosingOrigins);assertFalse(home.state.metadataLoading);
            assertEquals(View.VISIBLE,home.screen.sections.bodies.get(MainActivity.CARD_CONVERSION).getVisibility());
            second.get().onActivityResult(HomeRequests.PICK_ORIGIN,Activity.RESULT_CANCELED,null);
            assertFalse(home.state.choosingOrigins);assertNull(home.state.pendingOriginSource);
        }finally{second.pause().stop().destroy();VideoMetadataCache.clear();}
    }
    @Test public void pickVideosAndOpenSettingsReachTheCorrectAndroidActions(){
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            MainActivity activity=controller.get();HomeController home=activity.home;
            home.screen.files.pickVideo.performClick();Intent picker=shadowOf(activity).getNextStartedActivityForResult().intent;
            assertEquals(Intent.ACTION_OPEN_DOCUMENT,picker.getAction());assertEquals("video/*",picker.getType());assertTrue(picker.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));
            home.screen.compression.crfSlider.setProgress(18);home.screen.preferences.performClick();Intent settings=shadowOf(activity).getNextStartedActivity();
            assertEquals(SettingsActivity.class.getName(),settings.getComponent().getClassName());assertEquals(18,settings.getIntExtra("crf",-1));
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void cancellingTxtPickerDeletesOnlyItsTemporarySnapshot()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            MainActivity activity=controller.get();File snapshot=File.createTempFile("log-export-",".txt",activity.getCacheDir()),other=File.createTempFile("original-",".mp4",activity.getCacheDir());
            activity.home.state.pendingLog=snapshot.getAbsolutePath();activity.onActivityResult(HomeRequests.SAVE_LOG,Activity.RESULT_CANCELED,null);
            assertFalse(snapshot.exists());assertTrue(other.exists());assertNull(activity.home.state.pendingLog);other.delete();
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void reopeningAnActiveSessionShowsConversionAndClosingDoesNotCancelIt()throws Exception{
        java.lang.reflect.Field field=ConversionService.class.getDeclaredField("state");field.setAccessible(true);
        ConversionService.State live=(ConversionService.State)field.get(null),before=new ConversionService.State(live);
        ActivityController<MainActivity> controller=null;
        try{
            Bundle cached=oneCachedVideo();live.hasSession=true;live.running=true;live.inputs=cached.getStringArrayList("inputs");live.names=cached.getStringArrayList("names");
            live.source="Originale.mp4";live.batchTotal=1;live.batchIndex=1;live.doneFrames=22;live.totalFrames=100;live.filePercent=22;live.requestedCrf=18;
            controller=Robolectric.buildActivity(MainActivity.class).setup();HomeController home=controller.get().home;
            assertEquals(View.VISIBLE,home.screen.sections.bodies.get(MainActivity.CARD_CONVERSION).getVisibility());assertEquals("22%",home.screen.conversion.percentLabel.getText().toString());
            assertFalse(home.screen.compression.crfSlider.isEnabled());assertTrue(home.screen.conversion.stop.isEnabled());
            controller.pause().stop().destroy();controller=null;assertTrue(ConversionService.snapshot().running);
        }finally{
            if(controller!=null)controller.pause().stop().destroy();
            for(java.lang.reflect.Field f:ConversionService.State.class.getDeclaredFields())f.set(live,f.get(before));
            VideoMetadataCache.clear();
        }
    }
}
