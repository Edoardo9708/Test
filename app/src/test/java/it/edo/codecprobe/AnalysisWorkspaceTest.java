package it.edo.codecprobe;

import android.app.Activity;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class AnalysisWorkspaceTest {
    @Before public void clear(){AnalysisSources.video(Collections.emptyList(),Collections.emptyList());var c=RuntimeEnvironment.getApplication();c.getSharedPreferences("audio-workspace",0).edit().clear().commit();c.getSharedPreferences("image-workspace",0).edit().clear().commit();}
    @After public void cleanup(){clear();}
    private static void await(AnalysisScreen screen,String text)throws Exception{
        long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while(!screen.status.getText().toString().contains(text)&&System.nanoTime()<until){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10);}Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(screen.status.getText().toString(),screen.status.getText().toString().contains(text));
    }
    @Test public void selectionListUsesReferencesWithoutOpeningMissingFiles(){
        var context=RuntimeEnvironment.getApplication();AudioState audio=new AudioState();audio.inputs.add("content://missing/audio");audio.names.add("Brano.flac");AudioDraftStore.save(context,audio);
        ImageState images=new ImageState();images.inputs.add("content://missing/photo");images.names.add("Foto.jpg");images.options=ImageOptions.load(context);ImageDraftStore.save(context,images);
        AnalysisSources.video(Arrays.asList("content://missing/video"),Arrays.asList("Filmato.mp4"));
        var controller=Robolectric.buildActivity(AnalysisActivity.class).setup();try{AnalysisScreen s=controller.get().analysis.screen;assertEquals(3,AnalysisSources.list(context).size());assertEquals(View.VISIBLE,s.files.getVisibility());assertEquals(View.GONE,s.map.getVisibility());assertEquals("",s.status.getText().toString());}finally{controller.pause().stop().destroy();}
    }
    @Test public void blockNavigationHexAndRecreationKeepSourceUnchanged()throws Exception{
        Path file=Files.createTempFile("analysis-ui-",".mp4");byte[] data=FileAnalysisChecks.concat(FileAnalysisChecks.box("ftyp",new byte[8]),FileAnalysisChecks.box("moov",FileAnalysisChecks.box("free",new byte[600])));Files.write(file,data);
        AnalysisSources.video(Arrays.asList(Uri.fromFile(file.toFile()).toString()),Arrays.asList("Video.mp4"));var controller=Robolectric.buildActivity(AnalysisActivity.class).setup();
        try{
            AnalysisScreen s=controller.get().analysis.screen;((Button)s.files.getChildAt(2)).performClick();await(s,"Sola lettura");assertEquals(2,s.blocks.getChildCount());assertTrue(((Button)s.blocks.getChildAt(0)).getText().toString().startsWith("Formato e compatibilità"));
            assertEquals(View.GONE,s.hexPanel.getVisibility());((Button)s.blocks.getChildAt(1)).performClick();await(s,"Sola lettura");assertEquals(1,s.blocks.getChildCount());assertTrue(s.frameTitle.getText().toString().contains("moov"));assertTrue(s.frameTitle.getText().toString().contains("Organizzazione del filmato"));assertNotSame(s.fileFrame,s.currentFrame.getParent());
            ((Button)s.blocks.getChildAt(0)).performClick();s.hex.performClick();await(s,"Sola lettura");assertEquals(View.VISIBLE,s.hexPanel.getVisibility());assertTrue(s.next.isEnabled());s.next.performClick();await(s,"Sola lettura");assertTrue(s.pageLabel.getText().toString().contains("256"));
            controller.recreate();s=controller.get().analysis.screen;await(s,"Sola lettura");assertTrue(s.frameTitle.getText().toString().contains("Video.mp4"));assertEquals(2,s.blocks.getChildCount());s.back.performClick();assertEquals(View.VISIBLE,s.files.getVisibility());assertArrayEquals(data,Files.readAllBytes(file));
        }finally{controller.pause().stop().destroy();Files.delete(file);}
    }
    @Test public void missingSourceIsReadableErrorAndAllowsReturn()throws Exception{
        AnalysisSources.video(Arrays.asList("file:///does-not-exist-analysis.mp4"),Arrays.asList("Assente.mp4"));var c=Robolectric.buildActivity(AnalysisActivity.class).setup();try{AnalysisScreen s=c.get().analysis.screen;((Button)s.files.getChildAt(2)).performClick();await(s,"Analisi non disponibile");s.back.performClick();assertEquals(View.VISIBLE,s.files.getVisibility());}finally{c.pause().stop().destroy();}
    }
    @Test public void fourthTabPublishesCurrentVideoSelectionBeforeOpening(){
        var c=Robolectric.buildActivity(MainActivity.class).setup();try{HomeState state=c.get().home.state;state.selectedInputs.add("content://preview/video");state.selectedNames.add("Prova.mp4");c.get().home.screen.navigation.analysis.performClick();assertEquals(AnalysisActivity.class.getName(),Shadows.shadowOf(c.get()).getNextStartedActivity().getComponent().getClassName());assertEquals("Prova.mp4",AnalysisSources.list(c.get()).get(0).name);assertFalse(c.get().isFinishing());}finally{c.pause().stop().destroy();}
    }
}
