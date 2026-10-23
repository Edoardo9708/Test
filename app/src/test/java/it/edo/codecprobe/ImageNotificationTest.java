package it.edo.codecprobe;

import android.app.*;
import android.content.Intent;
import android.os.*;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageNotificationTest {
    @Test public void rapidStagesAreCoalescedButPauseCommandPublishesImmediately()throws Exception{
        var field=ImageConversionService.class.getDeclaredField("state");field.setAccessible(true);Object before=field.get(null);
        ImageRunState run=new ImageRunState();run.running=true;run.startedAt=42;run.total=100;field.set(null,run);
        var controller=Robolectric.buildService(ImageConversionService.class).create();
        try{
            var service=controller.get();var stage=ImageConversionService.class.getDeclaredMethod("stage",String.class);stage.setAccessible(true);
            var looper=Shadows.shadowOf(Looper.getMainLooper());var notifications=Shadows.shadowOf(service.getSystemService(NotificationManager.class));
            for(int i=0;i<500;i++)stage.invoke(service,"stage-"+i);looper.idle();looper.idleFor(Duration.ofMillis(Math.max(0,1000-SystemClock.elapsedRealtime())));
            Notification first=notifications.getNotification(30);assertNotNull(first);assertEquals("stage-499",first.extras.getCharSequence(Notification.EXTRA_TEXT).toString());
            for(int i=0;i<500;i++)stage.invoke(service,"next-"+i);looper.idle();assertSame(first,notifications.getNotification(30));
            looper.idleFor(Duration.ofMillis(999));assertSame(first,notifications.getNotification(30));looper.idleFor(Duration.ofMillis(1));assertNotSame(first,notifications.getNotification(30));
            Notification next=notifications.getNotification(30);service.onStartCommand(new Intent(service,ImageConversionService.class).setAction(ImageConversionService.PAUSE).putExtra("startedAt",42L),0,1);looper.idle();assertNotSame(next,notifications.getNotification(30));assertTrue(notifications.getNotification(30).extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("Pausa richiesta"));
        }finally{controller.destroy();field.set(null,before);}
    }
}
