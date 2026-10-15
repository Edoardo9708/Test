package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class FolderPickerActivityTest {
    private void awaitFinish(FolderPickerActivity activity)throws Exception{
        long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while(!activity.isFinishing()&&System.nanoTime()<end){shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10);}
        shadowOf(Looper.getMainLooper()).idle();assertTrue("Picker result must finish the intermediate activity",activity.isFinishing());
    }
    @Test public void opensSystemPickerOnceAndDoesNotReopenOnResume(){
        try(ActivityController<FolderPickerActivity> c=Robolectric.buildActivity(FolderPickerActivity.class).setup()){
            ShadowActivity.IntentForResult started=shadowOf(c.get()).getNextStartedActivityForResult();assertNotNull(started);
            assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE,started.intent.getAction());assertEquals(0,started.intent.getFlags());assertNull(started.intent.getExtras());
            c.pause().resume();assertNull(shadowOf(c.get()).getNextStartedActivityForResult());
        }
    }
    @Test public void restoresPendingSystemPickerWithoutLaunchingAnother(){
        Bundle saved=new Bundle();
        try(ActivityController<FolderPickerActivity> first=Robolectric.buildActivity(FolderPickerActivity.class).setup()){first.saveInstanceState(saved);}
        try(ActivityController<FolderPickerActivity> restored=Robolectric.buildActivity(FolderPickerActivity.class).setup(saved)){assertNull(shadowOf(restored.get()).getNextStartedActivityForResult());}
    }
    @Test public void cancellationReturnsToCallerWithoutClaimingAProvidedFolder(){
        try(ActivityController<FolderPickerActivity> c=Robolectric.buildActivity(FolderPickerActivity.class).setup()){
            c.get().onActivityResult(1,Activity.RESULT_CANCELED,null);assertTrue(c.get().isFinishing());assertEquals(Activity.RESULT_CANCELED,shadowOf(c.get()).getResultCode());assertTrue(c.get().getContentResolver().getPersistedUriPermissions().isEmpty());
        }
    }
    @Test public void successfulGrantIsPersistedAndReturnedToCaller()throws Exception{
        try(ActivityController<FolderPickerActivity> c=Robolectric.buildActivity(FolderPickerActivity.class).setup()){
            Uri uri=Uri.parse("content://example.documents/tree/test");
            c.get().onActivityResult(1,Activity.RESULT_OK,new Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
            awaitFinish(c.get());java.util.List<UriPermission> permissions=c.get().getContentResolver().getPersistedUriPermissions();assertEquals(1,permissions.size());assertEquals(uri,permissions.get(0).getUri());assertTrue(permissions.get(0).isReadPermission());assertTrue(permissions.get(0).isWritePermission());
            assertEquals(Activity.RESULT_OK,shadowOf(c.get()).getResultCode());assertEquals(uri,shadowOf(c.get()).getResultIntent().getData());
        }
    }
    @Test public void readOnlyResponseIsReportedAsAnError()throws Exception{
        try(ActivityController<FolderPickerActivity> c=Robolectric.buildActivity(FolderPickerActivity.class).setup()){
            c.get().onActivityResult(1,Activity.RESULT_OK,new Intent().setData(Uri.parse("content://example.documents/tree/test")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            awaitFinish(c.get());assertTrue(c.get().getContentResolver().getPersistedUriPermissions().isEmpty());assertEquals(Activity.RESULT_CANCELED,shadowOf(c.get()).getResultCode());assertNotNull(shadowOf(c.get()).getResultIntent().getStringExtra(FolderPickerActivity.ERROR));
        }
    }
}
