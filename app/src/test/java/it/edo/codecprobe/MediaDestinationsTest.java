package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.view.View;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class MediaDestinationsTest {
    static final String A="content://destinations/tree/a",B="content://destinations/tree/b";
    public static class Directories extends ContentProvider {
        public boolean onCreate(){return true;}
        public Cursor query(Uri uri,String[] columns,String selection,String[] args,String sort){
            MatrixCursor rows=new MatrixCursor(columns);Object[] row=new Object[columns.length];
            for(int n=0;n<columns.length;n++)row[n]=DocumentsContract.Document.COLUMN_MIME_TYPE.equals(columns[n])?DocumentsContract.Document.MIME_TYPE_DIR:DocumentsContract.Document.COLUMN_FLAGS.equals(columns[n])?DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE:uri.getLastPathSegment();
            rows.addRow(row);return rows;
        }
        public String getType(Uri uri){return DocumentsContract.Document.MIME_TYPE_DIR;}
        public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
        public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
        public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
    }
    static void grant(Activity activity,String folder){activity.getContentResolver().takePersistableUriPermission(Uri.parse(folder),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}
    @Test public void choicesFollowSourceUrisAndOverrideDoesNotEraseThem(){
        DestinationDraft draft=new DestinationDraft();draft.sync(Arrays.asList("one","two"),A);draft.plan.setFolder(1,B);
        draft.plan.applyAll=true;draft.plan.setAllFolder("global");assertEquals("global",draft.plan.effective(1).folder);
        draft.plan.applyAll=false;assertEquals(B,draft.plan.effective(1).folder);draft.sync(Arrays.asList("one","two"),"new-default");assertEquals(A,draft.plan.effective(0).folder);assertEquals(B,draft.plan.effective(1).folder);
        draft.sync(Arrays.asList("two","one"),A);assertEquals(B,draft.plan.effective(0).folder);assertEquals(A,draft.plan.effective(1).folder);
        var prefs=RuntimeEnvironment.getApplication().getSharedPreferences("destination-test",0);var edit=prefs.edit();draft.save(edit,Arrays.asList("two","one"),A);edit.commit();
        DestinationDraft restored=new DestinationDraft();restored.load(prefs,Arrays.asList("two","one"),null);assertEquals(B,restored.plan.effective(0).folder);
        restored.sync(Collections.singletonList("one"),A);assertEquals(A,restored.plan.effective(0).folder);
        restored.reset(Collections.singletonList("new"),null);assertTrue(restored.plan.effective(0).origin);
        prefs.edit().clear().commit();
    }
    @Test public void serviceBoundaryCopiesPerFileFoldersAndRejectsMissingEntries(){
        ArrayList<String> folders=new ArrayList<>(Arrays.asList(A,B));Intent task=new Intent().putStringArrayListExtra("folders",folders);
        ArrayList<String> snapshot=QueueDestinations.read(task,2,null);folders.set(0,"changed");assertEquals(A,snapshot.get(0));
        assertEquals(Arrays.asList(A,A),QueueDestinations.read(new Intent(),2,A));
        try{QueueDestinations.read(task,1,null);fail();}catch(IllegalArgumentException expected){}
        try{QueueDestinations.read(new Intent(),2,null);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void samePanelHidesEmptySelectionVerifiesAccessAndRestoresPendingPicker()throws Exception{
        ShadowContentResolver.registerProviderInternal("destinations",new Directories());
        var activity=Robolectric.buildActivity(Activity.class).setup();
        Harness host=new Harness(activity.get());
        try{
            host.controller.render();assertEquals(View.GONE,host.view.body.getVisibility());assertFalse(host.controller.ready());
            host.inputs.addAll(Arrays.asList("content://songs/one","content://songs/two"));host.names.addAll(Arrays.asList("one.flac","two.flac"));grant(activity.get(),A);
            host.controller.render();settle(host);assertEquals(View.VISIBLE,host.view.body.getVisibility());assertEquals(Arrays.asList(A,A),host.controller.snapshot());
            host.view.pickFolder.performClick();assertTrue(host.controller.choosing());assertFalse(host.view.pickFolder.isEnabled());
            Bundle pending=new Bundle();host.controller.save(pending);host.controller.close();
            host.selected=1;host.controller=new MediaDestinationsController(host,host.view,MediaKind.AUDIO,pending);
            host.controller.result(MediaDestinationsController.FILE,Activity.RESULT_OK,new Intent().setData(Uri.parse(B)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            settle(host);assertEquals(Arrays.asList(B,A),host.controller.snapshot());
            host.view.applyAllDestinations.performClick();assertFalse(host.view.pickFolder.isEnabled());assertTrue(host.view.pickAllFolder.isEnabled());
            host.view.applyAllDestinations.performClick();settle(host);assertEquals(B,host.draft.plan.effective(0).folder);
            activity.get().getContentResolver().releasePersistableUriPermission(Uri.parse(B),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);int before=host.changes;host.controller.refresh();
            long end=System.currentTimeMillis()+3000;while(host.changes<before+2&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}
            assertTrue(host.changes>=before+2);assertFalse(host.controller.ready());
        }finally{host.controller.close();activity.pause().stop().destroy();}
    }
    static void settle(Harness host)throws Exception{
        long end=System.currentTimeMillis()+5000;
        while(!host.controller.ready()&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}
        Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue("Destinations were not verified",host.controller.ready());
    }
    static final class Harness implements MediaDestinationsController.Host {
        final AppUi ui;final ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>();final DestinationDraft draft=new DestinationDraft();final MediaDestinationsView view;final android.widget.LinearLayout root;
        MediaDestinationsController controller;int selected,changes;
        Harness(Activity activity){this(activity,MediaKind.AUDIO);}
        Harness(Activity activity,MediaKind kind){ui=new AppUi(activity);root=ui.newPage();view=new MediaDestinationsView(ui,root,kind);controller=new MediaDestinationsController(this,view,kind,null);}
        public AppUi ui(){return ui;}public List<String> inputs(){return inputs;}public List<String> names(){return names;}public int selected(){return selected;}public DestinationDraft draft(){return draft;}public String defaultFolder(){return A;}public boolean busy(){return false;}public void persist(){}public void changed(){changes++;controller.render();}public void toast(String text){}
    }
}
