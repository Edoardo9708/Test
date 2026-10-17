package it.edo.codecprobe;

import android.content.*;
import android.os.Looper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class AudioConversionTest {
    @Test public void outputNamesNeverOverwriteAndKeepOriginalBase(){
        assertEquals("Song.opus",AudioOutput.name("Song.FLAC"));
        assertEquals("a_b.opus",AudioOutput.name("a/b.flac"));
        assertEquals("Song (3).opus",AudioOutput.unique("Song.opus",new HashSet<>(Arrays.asList("Song.opus","Song (2).opus"))));
        assertEquals("Song.opus",AudioOutput.unique("Song.opus",Collections.emptySet()));
    }
    @Test public void audioAndVideoUseTheSameLeaseUntilCleanup()throws Exception{
        java.lang.reflect.Field field=ConversionService.class.getDeclaredField("RUNS");field.setAccessible(true);
        assertSame(MediaConversionGate.INSTANCE,field.get(null));
        long audio=MediaConversionGate.INSTANCE.acquire();assertNotEquals(0,audio);
        try{assertEquals(0,MediaConversionGate.INSTANCE.acquire());MediaConversionGate.INSTANCE.release(audio+1);assertTrue(MediaConversionGate.INSTANCE.owns(audio));}
        finally{MediaConversionGate.INSTANCE.release(audio);}
        long video=MediaConversionGate.INSTANCE.acquire();assertNotEquals(0,video);MediaConversionGate.INSTANCE.release(video);
    }
    @Test public void audioLogIsBoundedAndPreservesReadableSteps()throws Exception{
        Context c=RuntimeEnvironment.getApplication();
        try(AudioLog log=new AudioLog(c)){
            log.write("*** INIZIO CODIFICA ***\n1. Leggo FLAC\n2. Codifico Opus");
            assertTrue(log.tail().contains("\n1. Leggo FLAC\n2. Codifico Opus"));
            char[] fill=new char[10000];Arrays.fill(fill,'x');for(int n=0;n<300;n++)log.write(new String(fill));
            assertTrue(log.tail().length()<130000);
        }
        assertTrue(AudioLog.file(c).length()<AudioLog.LIMIT+200);
        assertTrue(new String(Files.readAllBytes(AudioLog.file(c).toPath()),StandardCharsets.UTF_8).contains("LIMITE LOG RAGGIUNTO"));
        assertTrue(AudioLog.file(c).delete());
    }
    @Test public void exportOnlyUsesAudioLogAndSelectionSurvivesReportClear(){
        Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("audio-workspace",0).edit().clear().commit();AudioConversionService.clear();
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            AudioController audio=controller.get().audio;audio.state.inputs.add("content://songs/a");audio.state.names.add("a.flac");audio.state.folder="content://music/tree/chosen";audio.render();
            assertTrue(audio.screen.start.isEnabled());assertFalse(audio.screen.cancel.isEnabled());
            audio.screen.clearReport.performClick();assertEquals(List.of("a.flac"),audio.state.names);
            assertTrue(audio.screen.parameters.getText().toString().equals("—"));
            assertTrue(audio.screen.status.getText().toString().contains("Opus"));
        }finally{controller.pause().stop().destroy();c.getSharedPreferences("audio-workspace",0).edit().clear().commit();}
    }
    @Test public void staleCancelCannotChangeAnotherAudioRun()throws Exception{
        java.lang.reflect.Field field=AudioConversionService.class.getDeclaredField("state");field.setAccessible(true);
        AudioRunState previous=(AudioRunState)field.get(null);AudioRunState current=new AudioRunState();current.running=true;current.startedAt=22;field.set(null,current);
        var service=Robolectric.buildService(AudioConversionService.class).create();
        try{
            service.get().onStartCommand(new Intent().setAction(AudioConversionService.CANCEL).putExtra("startedAt",21L),0,1);
            assertFalse(AudioConversionService.snapshot().cancelling);
        }finally{service.destroy();field.set(null,previous);}
    }
    @Test public void busySharedLeaseRejectsAudioBeforeLoadingNativeCode(){
        var service=Robolectric.buildService(AudioConversionService.class).create();Context c=RuntimeEnvironment.getApplication();
        long lease=MediaConversionGate.INSTANCE.acquire();assertNotEquals(0,lease);
        try{
            Intent request=new Intent(c,AudioConversionService.class).setAction(AudioConversionService.START)
                .putStringArrayListExtra("inputs",new ArrayList<>(List.of("content://songs/a")))
                .putStringArrayListExtra("names",new ArrayList<>(List.of("a.flac")))
                .putExtra("folder","content://music/tree/chosen").putExtra("bitrate",128);
            service.get().onStartCommand(request,0,1);
            assertFalse(AudioConversionService.snapshot().running);assertTrue(AudioConversionService.snapshot().message.contains("già attiva"));assertTrue(MediaConversionGate.INSTANCE.owns(lease));
        }finally{MediaConversionGate.INSTANCE.release(lease);service.destroy();AudioConversionService.clear();Shadows.shadowOf(Looper.getMainLooper()).idle();}
    }
}
