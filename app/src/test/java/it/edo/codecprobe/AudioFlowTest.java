package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class AudioFlowTest {
    private Context context;private Field audioState,videoState;private Object oldAudio,oldVideo;
    @Before public void before()throws Exception{
        context=RuntimeEnvironment.getApplication();context.getSharedPreferences("audio-workspace",0).edit().clear().commit();
        audioState=AudioConversionService.class.getDeclaredField("state");audioState.setAccessible(true);oldAudio=audioState.get(null);audioState.set(null,new AudioRunState());
        videoState=ConversionService.class.getDeclaredField("state");videoState.setAccessible(true);oldVideo=videoState.get(null);videoState.set(null,new ConversionService.State());
        ShadowContentResolver.registerProviderInternal("songs",new Names());ShadowContentResolver.registerProviderInternal("dirs",new Names());
    }
    @After public void after()throws Exception{audioState.set(null,oldAudio);videoState.set(null,oldVideo);context.getSharedPreferences("audio-workspace",0).edit().clear().commit();}
    public static class Names extends ContentProvider {
        public boolean onCreate(){return true;}
        public Cursor query(Uri uri,String[] columns,String s,String[] a,String sort){MatrixCursor cursor=new MatrixCursor(columns);if(!"children".equals(uri.getLastPathSegment())){Object[] row=new Object[columns.length];for(int n=0;n<columns.length;n++)row[n]="_size".equals(columns[n])?12345L:uri.getLastPathSegment();cursor.addRow(row);}return cursor;}
        public String getType(Uri uri){return "audio/flac";}
        public Uri insert(Uri u,ContentValues v){throw new AssertionError();}public int delete(Uri u,String s,String[] a){throw new AssertionError();}public int update(Uri u,ContentValues v,String s,String[] a){throw new AssertionError();}
    }
    private void chosen(AudioController audio,String... names){for(String name:names){audio.state.inputs.add("content://songs/"+name);audio.state.names.add(name);}audio.state.folder="content://dirs/tree/music";audio.render();}
    private void await(AudioController audio)throws Exception{audio.selection.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();}
    @Test public void everySelectedTrackCanBeInspectedAndRemoved(){
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;for(int n=0;n<13;n++){audio.state.inputs.add("content://songs/"+n+".flac");audio.state.names.add(n+".flac");}audio.render();
            assertEquals(13,audio.screen.navigator.getChildCount());audio.screen.navigator.getChildAt(12).performClick();Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));assertEquals("12.flac",audio.screen.names.getText().toString());
            audio.screen.remove.performClick();assertEquals(12,audio.state.inputs.size());assertEquals("11.flac",audio.screen.names.getText().toString());
            assertEquals(11,AudioDraftStore.load(context).selectedIndex);assertFalse(audio.state.names.contains("12.flac"));
            assertEquals(View.GONE,audio.screen.log.getVisibility());assertTrue(audio.screen.sections.bodies.containsKey(AudioScreen.DIAGNOSTICS));
            audio.screen.settings.performClick();Intent settings=Shadows.shadowOf(controller.get()).getNextStartedActivity();assertEquals(SettingsActivity.class.getName(),settings.getComponent().getClassName());assertTrue(settings.getBooleanExtra(AudioSettingsScreen.AUDIO,false));assertEquals("Impostazioni",audio.screen.settings.getText().toString());
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void invalidNewLyricsSelectionCannotLeaveOldLyricsSilentlySelected()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;audio.state.lyricsInputs.add("content://songs/old.lrc");audio.state.lyricsNames.add("old.lrc");
            audio.selection.result(AudioSelection.LYRICS,Activity.RESULT_OK,new Intent().setData(Uri.parse("content://songs/not-lyrics.txt")));await(audio);
            assertFalse(audio.state.readingLyrics);assertTrue(audio.state.lyricsInputs.isEmpty());assertTrue(AudioDraftStore.load(context).lyricsInputs.isEmpty());
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void filenameReadInterruptedByRecreationIsRestarted()throws Exception{
        AudioState draft=new AudioState();draft.inputs.add("content://songs/real.flac");draft.names.add("Brano 1");draft.readingNames=true;AudioDraftStore.save(context,draft);
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;await(audio);assertEquals(List.of("real.flac"),audio.state.names);assertFalse(audio.state.readingNames);assertFalse(AudioDraftStore.load(context).readingNames);
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void preflightFreezesParametersAndPreventsDoubleStart()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;chosen(audio,"music.flac");audio.state.bitrate=160;audio.state.onlySmaller=true;
            audio.actions.start();assertTrue(audio.state.preparing);assertFalse(audio.screen.start.isEnabled());assertFalse(audio.screen.pick.isEnabled());
            audio.state.bitrate=96;audio.actions.start();await(audio);
            Intent launch=Shadows.shadowOf(controller.get()).getNextStartedService();assertNotNull(launch);assertEquals(160,launch.getIntExtra("bitrate",0));assertTrue(launch.getBooleanExtra("onlySmaller",false));
            assertEquals(List.of("music.flac"),launch.getStringArrayListExtra("names"));audio.actions.start();assertNull(Shadows.shadowOf(controller.get()).getNextStartedService());
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void ambiguousLyricsAreReportedBeforeStartingTheEncoder()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;chosen(audio,"music.flac");audio.state.lyricsInputs.addAll(List.of("content://one/a","content://two/b"));audio.state.lyricsNames.addAll(List.of("music.lrc","music.lrc"));audio.render();
            assertTrue(audio.screen.lyrics.getText().toString().contains("ambigui"));audio.actions.start();await(audio);
            assertFalse(audio.state.preparing);assertNull(Shadows.shadowOf(controller.get()).getNextStartedService());assertTrue(audio.screen.status.getText().toString().contains("Più testi"));
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void newRunWithSameResultCountReplacesThePreviousReport()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;AudioRunState first=new AudioRunState();first.total=1;first.results.add(new AudioRunState.Result("old.flac","content://out/old",100,50,null));audioState.set(null,first);audio.renderRun();
            AudioRunState next=new AudioRunState();next.total=1;next.results.add(new AudioRunState.Result("new.flac","content://out/new",200,50,null));audioState.set(null,next);audio.renderRun();
            android.widget.LinearLayout row=(android.widget.LinearLayout)audio.screen.report.getChildAt(0);assertTrue(((android.widget.TextView)row.getChildAt(0)).getText().toString().contains("new.flac"));
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void cancelledAndUnsavedTracksAreNotPresentedAsFailuresAndDoNotRetry()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioRunState run=new AudioRunState();run.total=4;
            run.results.add(new AudioRunState.Result("ok.flac","content://songs/ok.flac","content://out/ok",1000,400,null,AudioRunState.Outcome.SAVED));
            run.results.add(new AudioRunState.Result("cancel.flac","content://songs/cancel.flac",null,-1,0,"Annullato",AudioRunState.Outcome.CANCELLED));
            run.results.add(new AudioRunState.Result("large.flac","content://songs/large.flac",null,100,120,"Nessun risparmio",AudioRunState.Outcome.NOT_SAVED));
            run.results.add(new AudioRunState.Result("fail.flac","content://songs/fail.flac",null,-1,0,"Errore",AudioRunState.Outcome.FAILED));audioState.set(null,run);
            AudioController audio=controller.get().audio;audio.state.folder="content://dirs/tree/music";audio.renderRun();assertTrue(audio.screen.summary.getText().toString().contains("falliti 1"));assertEquals("100%",audio.screen.totalProgress.getText().toString());
            audio.actions.retry();await(audio);Intent launch=Shadows.shadowOf(controller.get()).getNextStartedService();assertNotNull(launch);assertEquals(List.of("content://songs/fail.flac"),launch.getStringArrayListExtra("inputs"));
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void screenFlagIsScopedToVisibleRunningAudioAndResetKeepsSettings()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;chosen(audio,"music.flac");audio.screen.keepScreenOn.setChecked(true);audio.screen.bitrate.setProgress(12);
            AudioRunState run=new AudioRunState();run.running=true;run.total=1;audioState.set(null,run);audio.renderRun();assertTrue((controller.get().getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)!=0);
            controller.pause();assertEquals(0,controller.get().getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            run.running=false;audioState.set(null,run);controller.resume();audio.renderRun();audio.screen.clear.performClick();assertTrue(audio.state.inputs.isEmpty());assertEquals(160,audio.state.bitrate);assertEquals("content://dirs/tree/music",audio.state.folder);assertTrue(audio.state.keepScreenOn);
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void progressReservesVerificationAndReportsIncompleteQueue(){
        AudioRunState run=new AudioRunState();run.running=true;run.total=2;run.current=1;run.percent=89;assertEquals(44,run.queuePercent());run.percent=96;assertEquals(48,run.queuePercent());
        run.results.add(new AudioRunState.Result("cancel.flac","content://songs/cancel.flac",null,-1,0,"Annullato",AudioRunState.Outcome.CANCELLED));run.running=false;assertEquals(50,run.queuePercent());assertTrue(run.summary().contains("non elaborati 1"));assertEquals("Nessun file salvato",run.savings());
        assertEquals("Song (2).opus",AudioOutput.unique("Song.opus",Set.of("SONG.OPUS")));
    }
}
