package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.view.View;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class AudioWorkspaceTest {
    private Context context;
    private ConversionService.State previous;
    private java.lang.reflect.Field serviceState;
    @Before public void before()throws Exception{previous=ConversionService.snapshot();serviceState=ConversionService.class.getDeclaredField("state");serviceState.setAccessible(true);serviceState.set(null,new ConversionService.State());context=RuntimeEnvironment.getApplication();SavedSession.clear(context);context.getSharedPreferences("audio-workspace",0).edit().clear().commit();}
    @After public void after()throws Exception{serviceState.set(null,previous);SavedSession.clear(context);VideoMetadataCache.clear();context.getSharedPreferences("audio-workspace",0).edit().clear().commit();}
    public static class Names extends ContentProvider {
        public boolean onCreate(){return true;}
        public Cursor query(Uri uri,String[] columns,String s,String[] a,String order){MatrixCursor rows=new MatrixCursor(columns);rows.addRow(new Object[]{uri.getLastPathSegment()+".flac"});return rows;}
        public String getType(Uri uri){return "audio/flac";}
        public Uri insert(Uri uri,ContentValues values){throw new AssertionError("Source must not change");}
        public int update(Uri uri,ContentValues values,String s,String[] a){throw new AssertionError("Source must not change");}
        public int delete(Uri uri,String s,String[] a){throw new AssertionError("Source must not change");}
    }
    @Test public void independentAudioDraftAndSharedViewsSurviveReopeningWithoutChangingSavedVideo()throws Exception{
        Intent queue=BatchRetryTest.queue(context);ConversionService.State video=SavedSessionTest.sample(queue);
        SavedSession.save(context,queue,video);File journal=new File(context.getFilesDir(),"conversion-session.json");byte[] original=Files.readAllBytes(journal.toPath());
        AppSettings settings=new AppSettings(context);int videoCrf=settings.crf;
        var first=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            AudioController audio=first.get().audio;
            assertEquals(View.VISIBLE,audio.screen.sections.bodies.get(AudioScreen.FILES).getVisibility());assertEquals(View.GONE,audio.screen.sections.bodies.get(AudioScreen.COMPRESSION).getVisibility());
            assertTrue(audio.screen.navigation.audio.isSelected());assertFalse(audio.screen.navigation.video.isSelected());
            assertEquals(0f,audio.screen.pick.getElevation(),0f);
            audio.state.inputs.add("content://songs/music");audio.state.names.add("music.flac");audio.state.folder="content://dirs/tree/music";
            audio.screen.bitrate.setProgress(16);audio.render();audio.screen.sections.focusCard(AudioScreen.COMPRESSION);
            first.pause().stop().destroy();
            var second=Robolectric.buildActivity(AudioActivity.class).setup();
            try{
                AudioController restored=second.get().audio;assertEquals(List.of("music.flac"),restored.state.names);assertEquals("content://dirs/tree/music",restored.state.folder);assertEquals(192,restored.state.bitrate);
                assertEquals(View.VISIBLE,restored.screen.sections.bodies.get(AudioScreen.COMPRESSION).getVisibility());
                assertArrayEquals(original,Files.readAllBytes(journal.toPath()));assertEquals(videoCrf,new AppSettings(context).crf);
                restored.selection.clear();assertTrue(restored.state.inputs.isEmpty());assertEquals(192,restored.state.bitrate);assertArrayEquals(original,Files.readAllBytes(journal.toPath()));
            }finally{second.pause().stop().destroy();}
        }finally{if(!first.get().isDestroyed())first.pause().stop().destroy();SavedSession.clear(context);}
    }
    @Test public void multipleAudioFilesUseOnlyAudioPickerAndCancelledSelectionKeepsDraft()throws Exception{
        ShadowContentResolver.registerProviderInternal("songs",new Names());var controller=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            AudioController audio=controller.get().audio;audio.screen.pick.performClick();Intent picker=Shadows.shadowOf(controller.get()).getNextStartedActivity();
            assertEquals("audio/*",picker.getType());assertTrue(picker.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));
            ClipData clips=ClipData.newRawUri("one",Uri.parse("content://songs/one"));clips.addItem(new ClipData.Item(Uri.parse("content://songs/two")));clips.addItem(new ClipData.Item(Uri.parse("content://songs/one")));
            Intent chosen=new Intent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);chosen.setClipData(clips);
            audio.selection.result(AudioSelection.FILES,Activity.RESULT_OK,chosen);
            audio.selection.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(audio.state.readingNames);assertEquals(List.of("one.flac","two.flac"),audio.state.names);assertEquals(2,audio.state.inputs.size());assertTrue(audio.screen.selected.getText().toString().contains("2"));
            audio.selection.result(AudioSelection.FILES,Activity.RESULT_CANCELED,null);assertEquals(2,audio.state.inputs.size());
            audio.screen.chooseFolder.performClick();assertEquals(FolderPickerActivity.class.getName(),Shadows.shadowOf(controller.get()).getNextStartedActivity().getComponent().getClassName());audio.destinations.result(MediaDestinationsController.FILE,Activity.RESULT_CANCELED,null);
            audio.state.folder="content://dirs/tree/previous";
            audio.selection.result(AudioSelection.FOLDER,Activity.RESULT_OK,new Intent().setData(Uri.parse("content://dirs/tree/readonly")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            assertEquals("content://dirs/tree/previous",audio.state.folder);
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void navigationKeepsVideoSelectionAndDoesNotTouchServiceState()throws Exception{
        var video=Robolectric.buildActivity(MainActivity.class).setup();var audio=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            HomeController home=video.get().home;home.state.selectedInputs.add("content://video/kept");home.state.selectedNames.add("kept.mp4");
            ConversionService.State before=ConversionService.snapshot();
            home.screen.navigation.audio.performClick();Intent openAudio=Shadows.shadowOf(video.get()).getNextStartedActivity();assertEquals(AudioActivity.class.getName(),openAudio.getComponent().getClassName());
            assertTrue((openAudio.getFlags()&Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)!=0);
            audio.get().audio.screen.navigation.video.performClick();Intent openVideo=Shadows.shadowOf(audio.get()).getNextStartedActivity();assertEquals(MainActivity.class.getName(),openVideo.getComponent().getClassName());assertTrue(audio.get().isFinishing());
            assertEquals(List.of("kept.mp4"),home.state.selectedNames);assertEquals(before.sessionId,ConversionService.snapshot().sessionId);assertEquals(before.running,ConversionService.snapshot().running);
            assertEquals(before.sizeReport,ConversionService.snapshot().sizeReport);
        }finally{audio.pause().stop().destroy();video.pause().stop().destroy();}
    }
    @Test public void lateFilenameCallbackCannotRepopulateClearedAudioSelection()throws Exception{
        ShadowContentResolver.registerProviderInternal("songs",new Names());var controller=Robolectric.buildActivity(AudioActivity.class).setup();CountDownLatch release=new CountDownLatch(1);
        try{
            AudioController audio=controller.get().audio;
            audio.selection.worker.submit(()->{try{release.await(4,TimeUnit.SECONDS);}catch(InterruptedException error){Thread.currentThread().interrupt();}});
            audio.selection.result(AudioSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.parse("content://songs/late")));
            assertTrue(audio.state.readingNames);assertFalse(audio.screen.start.isEnabled());
            audio.selection.clear();assertFalse(audio.state.readingNames);release.countDown();audio.selection.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertTrue(audio.state.inputs.isEmpty());assertTrue(AudioDraftStore.load(context).inputs.isEmpty());
        }finally{release.countDown();controller.pause().stop().destroy();}
    }
}
