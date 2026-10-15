package it.edo.codecprobe;

import android.app.AlertDialog;
import android.app.Notification;
import android.content.Intent;
import android.widget.Button;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class CancellationConfirmationTest {
    private Object field(MainActivity a,String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(a);}
    private void startFakeHdr(MainActivity activity)throws Exception{
        Field hdr=MainActivity.class.getDeclaredField("hdrTesting");hdr.setAccessible(true);hdr.setBoolean(activity,true);
        Button stop=(Button)field(activity,"stop");stop.setEnabled(true);stop.performClick();
    }
    @Test public void continueOrDismissDoesNotCancel()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try{
            startFakeHdr(activity);AtomicBoolean cancelled=(AtomicBoolean)field(activity,"cancelled");
            AlertDialog dialog=(AlertDialog)field(activity,"cancelConfirmation");assertNotNull(dialog);assertFalse(cancelled.get());
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertFalse(cancelled.get());
            ((Button)field(activity,"stop")).performClick();((AlertDialog)field(activity,"cancelConfirmation")).cancel();assertFalse(cancelled.get());
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void onlyPositiveConfirmationCancels()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try{
            startFakeHdr(activity);AtomicBoolean cancelled=(AtomicBoolean)field(activity,"cancelled");assertFalse(cancelled.get());
            ((AlertDialog)field(activity,"cancelConfirmation")).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            assertTrue(cancelled.get());assertFalse(((Button)field(activity,"stop")).isEnabled());
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void notificationOpensConfirmationActivityInsteadOfStoppingService()throws Exception{
        ServiceController<ConversionService> controller=Robolectric.buildService(ConversionService.class).create();
        try{
            ConversionService.State state=new ConversionService.State();state.running=true;state.startedAt=123;state.batchTotal=2;state.batchIndex=1;
            Method make=ConversionService.class.getDeclaredMethod("notification",ConversionService.State.class);make.setAccessible(true);
            Notification notification=(Notification)make.invoke(controller.get(),state);
            assertTrue(notification.actions[0].actionIntent.isActivity());
            Intent request=shadowOf(notification.actions[0].actionIntent).getSavedIntent();
            assertEquals(MainActivity.CONFIRM_CANCEL,request.getAction());assertEquals(MainActivity.class.getName(),request.getComponent().getClassName());
            assertEquals(123,request.getLongExtra(MainActivity.CANCEL_SESSION,-1));
        }finally{controller.destroy();}
    }
    @Test public void staleNotificationDoesNotShowCancelDialog()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try{
            Method handle=MainActivity.class.getDeclaredMethod("handleCancellationIntent",Intent.class);handle.setAccessible(true);
            Intent request=new Intent(MainActivity.CONFIRM_CANCEL).putExtra(MainActivity.CANCEL_SESSION,999L);handle.invoke(activity,request);
            assertNull(field(activity,"cancelConfirmation"));assertNull(request.getAction());
        }finally{controller.pause().stop().destroy();}
    }
}
