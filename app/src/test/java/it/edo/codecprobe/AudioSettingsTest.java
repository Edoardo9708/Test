package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class AudioSettingsTest {
    private Context context;private java.lang.reflect.Field serviceState;private Object previousRun;
    @Before public void before()throws Exception{
        context=RuntimeEnvironment.getApplication();context.getSharedPreferences("audio-workspace",0).edit().clear().commit();context.getSharedPreferences("audio-startup-settings",0).edit().clear().commit();
        serviceState=AudioConversionService.class.getDeclaredField("state");serviceState.setAccessible(true);previousRun=serviceState.get(null);serviceState.set(null,new AudioRunState());
    }
    @After public void after()throws Exception{serviceState.set(null,previousRun);context.getSharedPreferences("audio-workspace",0).edit().clear().commit();context.getSharedPreferences("audio-startup-settings",0).edit().clear().commit();}
    private Intent intent(){return new Intent(context,SettingsActivity.class).putExtra(AudioSettingsScreen.AUDIO,true);}
    @Test public void headerOpensDedicatedAudioPageWithCurrentConfiguration(){
        var home=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=home.get().audio;audio.screen.bitrate.setProgress(12);audio.state.folder="content://dirs/tree/current";audio.screen.settings.performClick();
            Intent open=Shadows.shadowOf(home.get()).getNextStartedActivity();assertEquals(SettingsActivity.class.getName(),open.getComponent().getClassName());assertTrue(open.getBooleanExtra(AudioSettingsScreen.AUDIO,false));assertEquals(160,open.getIntExtra("audioBitrate",0));
            var settings=Robolectric.buildActivity(SettingsActivity.class,open).setup();try{assertNotNull(settings.get().audioScreen);assertEquals("Salva impostazioni",settings.get().audioScreen.save.getText().toString());}finally{settings.pause().stop().destroy();}
        }finally{home.pause().stop().destroy();}
    }
    @Test public void unsavedEditsSurviveRecreationButBackDoesNotSave(){
        var page=Robolectric.buildActivity(SettingsActivity.class,intent()).setup();Bundle bundle=new Bundle();
        try{AudioSettingsScreen s=page.get().audioScreen;s.bitrate.setProgress(16);s.onlySmaller.setChecked(false);s.keepScreenOn.setChecked(true);s.folder="content://dirs/tree/edited";page.saveInstanceState(bundle);assertEquals(0,new AudioSettings(context).revision);}finally{page.pause().stop().destroy();}
        var restored=Robolectric.buildActivity(SettingsActivity.class,intent()).create(bundle).start().resume();try{
            AudioSettingsScreen s=restored.get().audioScreen;assertEquals(192,s.value());assertFalse(s.onlySmaller.isChecked());assertTrue(s.keepScreenOn.isChecked());assertEquals("content://dirs/tree/edited",s.folder);s.back.performClick();
            assertEquals(0,new AudioSettings(context).revision);assertEquals(128,new AudioSettings(context).bitrate);assertNull(new AudioSettings(context).folder);
        }finally{restored.pause().stop().destroy();}
    }
    @Test public void savedDefaultsUpdateAudioHomeWithoutChangingSelectedFilesOrVideo(){
        Map<String,?> video=new HashMap<>(context.getSharedPreferences("startup-settings",0).getAll());
        var home=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=home.get().audio;audio.state.inputs.add("content://songs/kept");audio.state.names.add("kept.flac");audio.state.lyricsInputs.add("content://songs/kept-lrc");audio.state.lyricsNames.add("kept.lrc");audio.save();
            var settings=Robolectric.buildActivity(SettingsActivity.class,intent()).setup();try{
                AudioSettingsScreen s=settings.get().audioScreen;s.bitrate.setProgress(16);s.onlySmaller.setChecked(false);s.keepScreenOn.setChecked(true);s.folder="content://dirs/tree/saved";s.save.performClick();
            }finally{settings.pause().stop().destroy();}
            audio.renderRun();assertEquals(192,audio.state.bitrate);assertEquals(16,audio.screen.bitrate.getProgress());assertFalse(audio.screen.onlySmaller.isChecked());assertTrue(audio.screen.keepScreenOn.isChecked());assertEquals("content://dirs/tree/saved",audio.state.folder);
            assertEquals(List.of("kept.flac"),audio.state.names);assertEquals(List.of("kept.lrc"),audio.state.lyricsNames);assertEquals(video,context.getSharedPreferences("startup-settings",0).getAll());
        }finally{home.pause().stop().destroy();}
    }
    @Test public void defaultsSavedDuringConversionApplyOnlyAfterItsEnd()throws Exception{
        var home=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=home.get().audio;AudioRunState run=new AudioRunState();run.running=true;run.total=1;run.bitrate=128;run.onlySmaller=true;serviceState.set(null,run);audio.renderRun();assertTrue(audio.screen.settings.isEnabled());
            audio.save();AudioSettings.save(context,192,"content://dirs/tree/next",false,false);audio.renderRun();assertEquals(128,audio.state.bitrate);assertEquals(128,AudioConversionService.snapshot().bitrate);assertTrue(AudioConversionService.snapshot().onlySmaller);
            var recreated=Robolectric.buildActivity(AudioActivity.class).setup();try{assertEquals(128,recreated.get().audio.state.bitrate);}finally{recreated.pause().stop().destroy();}
            run.running=false;audio.renderRun();assertEquals(192,audio.state.bitrate);assertFalse(audio.state.onlySmaller);assertEquals(128,AudioConversionService.snapshot().bitrate);
        }finally{home.pause().stop().destroy();}
    }
    @Test public void explicitFolderRemovalDoesNotFallBackToOldDraftAndDefaultsStaySeparate(){
        AudioState legacy=new AudioState();legacy.bitrate=160;legacy.folder="content://dirs/tree/old";AudioDraftStore.save(context,legacy);assertEquals(160,new AudioSettings(context).bitrate);assertEquals(legacy.folder,new AudioSettings(context).folder);
        AudioSettings.save(context,128,null,true,false);assertNull(new AudioSettings(context).folder);AudioState first=AudioDraftStore.load(context);assertEquals(128,first.bitrate);assertNull(first.folder);
        first.bitrate=192;first.folder="content://dirs/tree/temporary";AudioDraftStore.save(context,first);assertEquals(128,new AudioSettings(context).bitrate);assertNull(new AudioSettings(context).folder);assertEquals(192,AudioDraftStore.load(context).bitrate);
    }
    @Test public void copyCurrentAndFolderCancelOnlyChangeUnsavedForm(){
        AudioSettings.save(context,128,"content://dirs/tree/default",true,false);
        Intent open=intent().putExtra("audioBitrate",160).putExtra("audioFolder","content://dirs/tree/current").putExtra("audioSmaller",false).putExtra("audioScreen",true);
        var settings=Robolectric.buildActivity(SettingsActivity.class,open).setup();try{
            AudioSettingsScreen s=settings.get().audioScreen;assertEquals(128,s.value());s.copyCurrent.performClick();assertEquals(160,s.value());assertEquals("content://dirs/tree/current",s.folder);assertFalse(s.onlySmaller.isChecked());
            s.chooseFolder.performClick();assertEquals(FolderPickerActivity.class.getName(),Shadows.shadowOf(settings.get()).getNextStartedActivity().getComponent().getClassName());
            s.result(10,Activity.RESULT_CANCELED,null);assertEquals("content://dirs/tree/current",s.folder);
            s.result(10,Activity.RESULT_OK,new Intent().setData(Uri.parse("content://dirs/tree/readonly")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));assertEquals("content://dirs/tree/current",s.folder);
            assertEquals(128,new AudioSettings(context).bitrate);assertEquals("content://dirs/tree/default",new AudioSettings(context).folder);
        }finally{settings.pause().stop().destroy();}
    }
}
