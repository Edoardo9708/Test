package it.edo.codecprobe;

import android.app.*;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class HomeSavingsRetryTest {
    public static class EmptyDirectory extends ContentProvider {
        public boolean onCreate(){return true;}
        public Cursor query(Uri u,String[] p,String s,String[] a,String o){return new MatrixCursor(p);}
        public String getType(Uri u){return null;}public Uri insert(Uri u,ContentValues v){return null;}
        public int update(Uri u,ContentValues v,String s,String[] a){return 0;}public int delete(Uri u,String s,String[] a){return 0;}
    }
    @Before public void before(){ConversionService.rememberFailures(RuntimeEnvironment.getApplication(),null);RuntimeEnvironment.getApplication().getSharedPreferences("startup-settings",0).edit().remove("onlySmaller").commit();}
    @After public void after(){ConversionService.rememberFailures(RuntimeEnvironment.getApplication(),null);}
    @Test public void savingsCheckboxIsRememberedAndFrozenDuringConversion(){
        ActivityController<MainActivity> first=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            assertTrue(first.get().home.screen.conversion.onlySmaller.isChecked());
            first.get().home.screen.conversion.onlySmaller.setChecked(false);
            first.get().home.conversion.setBusy(true);assertFalse(first.get().home.screen.conversion.onlySmaller.isEnabled());
        }finally{first.pause().stop().destroy();}
        ActivityController<MainActivity> second=Robolectric.buildActivity(MainActivity.class).setup();
        try{assertFalse(second.get().home.screen.conversion.onlySmaller.isChecked());}finally{second.pause().stop().destroy();}
    }
    @Test public void retryUsesOnlySavedFailuresAndRunsFilenamePreflightAgain()throws Exception {
        Application app=RuntimeEnvironment.getApplication();
        Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.ACCESS_MEDIA_LOCATION,android.Manifest.permission.READ_MEDIA_VIDEO);
        ShadowContentResolver.registerProviderInternal("dirs",new EmptyDirectory());
        ConversionService.rememberFailures(app,BatchRetry.select(BatchRetryTest.queue(app),List.of(1)));
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;home.conversion.setBusy(false);
            assertEquals(View.VISIBLE,home.screen.conversion.retryFailed.getVisibility());assertTrue(home.screen.conversion.retryFailed.getText().toString().contains("1"));
            home.screen.conversion.retryFailed.performClick();assertNotNull(home.conversion.retry.confirmation);
            home.conversion.retry.confirmation.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            home.conversion.outputCheck.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            Intent started=Shadows.shadowOf(app).getNextStartedService();assertNotNull(started);
            assertEquals(List.of("content://input/1"),started.getStringArrayListExtra("inputs"));
            assertEquals(List.of("content://dirs/tree/one"),started.getStringArrayListExtra("folders"));
            assertEquals(List.of("custom-one.mp4"),started.getStringArrayListExtra("outputNames"));
            assertEquals(17,started.getIntExtra("crf",0));assertEquals(8,started.getIntExtra("preset",0));assertTrue(started.getBooleanExtra("onlySmaller",false));
        }finally{controller.pause().stop().destroy();}
    }
}
