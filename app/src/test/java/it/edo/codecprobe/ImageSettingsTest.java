package it.edo.codecprobe;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageSettingsTest {
    private Context context;private java.lang.reflect.Field field;private Object previous;
    @Before public void setup()throws Exception{context=RuntimeEnvironment.getApplication();for(String name:new String[]{"image-workspace","image-settings","photo-appearance"})context.getSharedPreferences(name,0).edit().clear().commit();field=ImageConversionService.class.getDeclaredField("state");field.setAccessible(true);previous=field.get(null);field.set(null,new ImageRunState());}
    @After public void cleanup()throws Exception{field.set(null,previous);for(String name:new String[]{"image-workspace","image-settings","photo-appearance"})context.getSharedPreferences(name,0).edit().clear().commit();}
    @Test public void headerOpensDedicatedSettingsAndUnsavedEditsSurviveRecreation(){
        var home=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=home.get().images;images.quality(70);images.setFolder("content://dirs/tree/chosen");images.compose.command(PhotoCommand.SETTINGS);Intent open=Shadows.shadowOf(home.get()).getNextStartedActivity();assertEquals(ImageSettingsActivity.class.getName(),open.getComponent().getClassName());assertEquals(70,open.getIntExtra("quality",0));
            var settings=Robolectric.buildActivity(ImageSettingsActivity.class,open).setup();try{
                settings.get().screen.copy.performClick();assertEquals(70,settings.get().screen.quality.value());settings.get().screen.quality.quality.setProgress(40);settings.get().screen.smaller.setChecked(false);settings.recreate();assertEquals(80,settings.get().screen.quality.value());assertFalse(settings.get().screen.smaller.isChecked());settings.get().screen.back.performClick();assertEquals(65,ImageOptions.load(context).quality);assertEquals(0,ImageOptions.load(context).revision);
            }finally{settings.pause().stop().destroy();}
        }finally{home.pause().stop().destroy();}
    }
    @Test public void savedDefaultsApplyAfterActiveQueueAndKeepSelection()throws Exception{
        ImageRunState run=new ImageRunState();run.running=true;run.startedAt=44;run.options=new ImageOptions(60,"fast","standard",null,true,false,0);field.set(null,run);
        var home=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=home.get().images;images.state.inputs.add("content://photo/original");new ImageOptions(80,"slow","ssim","content://dirs/tree/new",false,true,0).save(context);images.refresh();assertEquals(65,images.state.options.quality);assertEquals(60,ImageConversionService.snapshot().options.quality);images.quality(50);assertEquals(65,images.state.options.quality);assertTrue(images.compose.getState().getBusy());
            run.running=false;images.refresh();assertEquals(80,images.state.options.quality);assertEquals("ssim",images.state.options.tune);assertEquals(1,images.state.inputs.size());assertEquals("content://dirs/tree/new",images.state.options.folder);assertFalse(images.state.options.onlySmaller);assertFalse(images.compose.getState().getBusy());
        }finally{home.pause().stop().destroy();}
    }
    @Test public void imageDraftIsSeparateAndFolderCancellationKeepsChoice(){
        var home=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=home.get().images;images.setFolder("content://dirs/tree/current");images.selection.result(ImageSelection.FOLDER,android.app.Activity.RESULT_CANCELED,null);assertEquals("content://dirs/tree/current",images.state.options.folder);
            images.quality(60);images.smaller(false);images.persist();assertEquals(60,ImageDraftStore.load(context).options.quality);assertEquals(65,ImageOptions.load(context).quality);assertTrue(ImageOptions.load(context).onlySmaller);
            new ImageOptions(75,"medium","psnr",null,true,false,0).save(context);images.refresh();assertNull(images.state.options.folder);assertEquals(75,images.state.options.quality);
        }finally{home.pause().stop().destroy();}
    }
    @Test public void leaseRejectsImagesBeforeNativeAndStaleCancellationCannotStopAnotherRun()throws Exception{
        var service=Robolectric.buildService(ImageConversionService.class).create();long token=MediaConversionGate.INSTANCE.acquire();assertNotEquals(0,token);
        try{Intent start=new Intent().setAction(ImageConversionService.START).putStringArrayListExtra("inputs",new java.util.ArrayList<>(java.util.List.of("content://photo/1")));new ImageOptions(65,"slow","standard","content://dirs/tree/out",true,false,0).put(start);service.get().onStartCommand(start,0,1);assertFalse(ImageConversionService.snapshot().running);assertTrue(MediaConversionGate.INSTANCE.owns(token));
            ImageRunState run=new ImageRunState();run.running=true;run.startedAt=22;field.set(null,run);service.get().onStartCommand(new Intent().setAction(ImageConversionService.CANCEL).putExtra("startedAt",21L),0,2);assertFalse(ImageConversionService.snapshot().cancelling);
        }finally{service.destroy();MediaConversionGate.INSTANCE.release(token);}
    }
    @Test public void screenAwakeOptionRemainsLiveDuringPhotoConversionAndClearsOnPause()throws Exception{
        ImageRunState run=new ImageRunState();run.running=true;run.options=new ImageOptions(65,"slow","standard",null,true,false,0);field.set(null,run);
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=controller.get().images;assertTrue(images.screen.screenOn.isEnabled());images.screen.screenOn.setChecked(true);
            assertTrue((controller.get().getWindow().getAttributes().flags&android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)!=0);assertFalse(ImageConversionService.snapshot().options.keepScreenOn);
            images.pause();assertEquals(0,controller.get().getWindow().getAttributes().flags&android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }finally{controller.pause().stop().destroy();}
    }

    @Test public void glassBlurTogglePersistsOnBackAndAppliesOnResumeWithoutChangingConversion()throws Exception{
        var home=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            ImageController images=home.get().images;
            assertTrue(images.compose.getBlurEnabled());
            ImageOptions options=images.state.options;
            images.state.inputs.add("content://photo/keep-selection");images.state.clamp();
            home.pause();
            var settings=Robolectric.buildActivity(ImageSettingsActivity.class).setup();
            try{
                assertTrue(settings.get().screen.glassBlur.isChecked());
                settings.get().screen.glassBlur.performClick();
                assertFalse(PhotoAppearance.blurEnabled(context));
                settings.recreate();assertFalse(settings.get().screen.glassBlur.isChecked());
                settings.get().screen.copy.performClick();assertFalse(settings.get().screen.glassBlur.isChecked());
                settings.get().screen.back.performClick();
            }finally{settings.pause().stop().destroy();}
            home.resume();assertFalse(images.compose.getBlurEnabled());
            assertSame(options,images.state.options);assertEquals(1,images.state.inputs.size());
            assertEquals(0,ImageOptions.load(context).revision);
            home.recreate();images=home.get().images;assertFalse(images.compose.getBlurEnabled());
            ImageRunState run=new ImageRunState();run.running=true;run.options=options;field.set(null,run);
            PhotoAppearance.blurEnabled(context,true);images.pause();images.resume();
            assertTrue(images.compose.getBlurEnabled());assertSame(options,ImageConversionService.snapshot().options);
            assertEquals(0,ImageOptions.load(context).revision);
        }finally{home.pause().stop().destroy();}
    }

}
