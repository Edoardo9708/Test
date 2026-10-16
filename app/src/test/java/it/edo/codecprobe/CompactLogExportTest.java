package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.os.Looper;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class CompactLogExportTest {
    @Test public void reopenedAppExportsOldVerboseLogCompactlyWithoutReplacingIt()throws Exception{
        Field field=ConversionService.class.getDeclaredField("state");field.setAccessible(true);Object before=field.get(null);field.set(null,new ConversionService.State());
        File source=ConversionService.batchLogFile(RuntimeEnvironment.getApplication());byte[] previous=source.isFile()?Files.readAllBytes(source.toPath()):null;
        try(Writer writer=new OutputStreamWriter(new FileOutputStream(source),StandardCharsets.UTF_8)){
            writer.write("*** INIZIO VIDEO 8 / 8 ***\nNome: Discorso Mirko_20180622.mp4\n");
            for(int i=0;i<30000;i++)writer.write("["+i+" ms] VERIFY frame="+i+" pts_us="+(i*33333L)+"\n");
            writer.write("*** ERRORE ***\njava.io.IOException: moov missing\n\tat Mp4Metadata.roots(Mp4Metadata.java:86)\n");
        }
        long sourceSize=source.length();var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;home.screen.diagnostics.saveLog.performClick();
            home.diagnostics.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(home.state.pendingLog);File snapshot=new File(home.state.pendingLog);
            String text=new String(Files.readAllBytes(snapshot.toPath()),StandardCharsets.UTF_8);
            assertTrue(snapshot.length()<sourceSize/10);assertEquals(sourceSize,source.length());assertTrue(text.contains("moov missing"));assertTrue(text.contains("Discorso Mirko_20180622.mp4"));assertTrue(text.contains("Mp4Metadata.java:86"));
            Intent picker=Shadows.shadowOf(controller.get()).getNextStartedActivityForResult().intent;assertEquals(Intent.ACTION_CREATE_DOCUMENT,picker.getAction());assertEquals("text/plain",picker.getType());
            home.diagnostics.onLogResult(HomeRequests.SAVE_LOG,Activity.RESULT_CANCELED,null);assertFalse(snapshot.exists());
        }finally{
            controller.pause().stop().destroy();field.set(null,before);if(previous==null)source.delete();else Files.write(source.toPath(),previous);
        }
    }
}
