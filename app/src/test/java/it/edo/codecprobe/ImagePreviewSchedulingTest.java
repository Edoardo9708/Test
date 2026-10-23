package it.edo.codecprobe;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.Looper;
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
public class ImagePreviewSchedulingTest {
    public static class Photos extends ContentProvider {
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        public boolean onCreate(){return true;}
        public Cursor query(Uri uri,String[] columns,String selection,String[] args,String sort){
            if(uri.getLastPathSegment().equals("slow")){entered.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Provider timeout");}catch(InterruptedException error){Thread.currentThread().interrupt();}}
            MatrixCursor cursor=new MatrixCursor(columns);Object[] row=new Object[columns.length];for(int i=0;i<row.length;i++)row[i]="_display_name".equals(columns[i])?uri.getLastPathSegment()+".jpg":0L;cursor.addRow(row);return cursor;
        }
        public String getType(Uri uri){return "image/jpeg";}public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
        public int update(Uri uri,ContentValues values,String where,String[] args){return 0;}public int delete(Uri uri,String where,String[] args){return 0;}
    }
    @Test public void selectedPhotoAnalysisFinishesWhileBulkNameProviderIsStillBlocked()throws Exception{
        var context=RuntimeEnvironment.getApplication();context.getSharedPreferences("image-workspace",0).edit().clear().commit();ImageMetadataCache.memory.clear();
        Photos photos=new Photos();ShadowContentResolver.registerProviderInternal("preview.photos",photos);
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            ImageController images=activity.get().images;images.state.inputs.add("content://preview.photos/slow");images.state.inputs.add("content://preview.photos/fast");images.state.clamp();images.readNames();assertTrue(photos.entered.await(3,TimeUnit.SECONDS));
            images.state.selected=1;images.open();var worker=ImageController.class.getDeclaredField("worker");worker.setAccessible(true);((ExecutorService)worker.get(images)).submit(()->{}).get(3,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(images.state.reading);assertEquals("fast.jpg",images.state.name);assertEquals(1,photos.release.getCount());
        }finally{photos.release.countDown();activity.pause().stop().destroy();}
    }
}
