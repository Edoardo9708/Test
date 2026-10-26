package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.Looper;
import android.provider.DocumentsContract;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageFolderImportTest {
    static final String TREE="content://folderphotos/tree/camera";
    public static class Folder extends ContentProvider {
        public boolean onCreate(){return true;}
        public Cursor query(Uri uri,String[] columns,String selection,String[] args,String sort){
            MatrixCursor rows=new MatrixCursor(columns);
            if(columns.length==3){
                for(int i=0;i<8500;i++)rows.addRow(new Object[]{"photo"+i,"IMG_"+i+(i%2==0?".JPG":".jpeg"),"image/jpeg"});
                rows.addRow(new Object[]{"heic","photo.heic","image/heic"});rows.addRow(new Object[]{"video","video.mp4","video/mp4"});rows.addRow(new Object[]{"nested","subfolder.jpg",DocumentsContract.Document.MIME_TYPE_DIR});
            }else if(columns.length==1&&columns[0].equals(DocumentsContract.Document.COLUMN_DISPLAY_NAME))rows.addRow(new Object[]{"IMG_0.JPG"});
            else{Object[] row=new Object[columns.length];for(int i=0;i<columns.length;i++)row[i]=columns[i].equals(DocumentsContract.Document.COLUMN_MIME_TYPE)?DocumentsContract.Document.MIME_TYPE_DIR:columns[i].equals(DocumentsContract.Document.COLUMN_FLAGS)?DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE:"camera";rows.addRow(row);}
            return rows;
        }
        public String getType(Uri uri){return "image/jpeg";}
        public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}public int update(Uri uri,ContentValues values,String s,String[] a){return 0;}public int delete(Uri uri,String s,String[] a){return 0;}
    }
    @Before public void reset(){var c=RuntimeEnvironment.getApplication();for(String pref:new String[]{"image-workspace","image-settings"})c.getSharedPreferences(pref,0).edit().clear().commit();ShadowContentResolver.registerProviderInternal("folderphotos",new Folder());ImageMetadataCache.memory.clear();}
    @Test public void folderListingImportsAllJpegsWithoutReadingPhotoBytes()throws Exception{
        var result=ImageFolderReader.read(RuntimeEnvironment.getApplication(),Uri.parse(TREE),TREE);assertEquals(8500,result.inputs.size());assertEquals("IMG_0.JPG",result.names.get(0));assertTrue(result.inputs.get(8499).contains("photo8499"));assertEquals(TREE,result.folders.get(8499));
    }
    @Test public void oneFolderPickerResultBuildsTheWholeQueueAndOnlyOneHundredChips()throws Exception{
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            var images=controller.get().images;images.compose.command(PhotoCommand.ADD_FOLDER);Intent request=Shadows.shadowOf(controller.get()).getNextStartedActivity();assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE,request.getAction());
            images.selection.result(ImageSelection.SOURCE_FOLDER,Activity.RESULT_OK,new Intent().setData(Uri.parse(TREE)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            long end=System.currentTimeMillis()+15000;
            while(images.state.importing&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10);}Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(images.state.importing);assertEquals(8500,images.state.inputs.size());assertTrue(images.state.bulk);assertEquals(100,images.compose.getState().getItems().size());assertEquals(8500,images.compose.getState().getTotal());assertEquals(images.state.inputs.get(99),images.compose.getState().getItems().get(99).getUri());
            assertTrue(images.state.destinations.plan.effective(8499).origin);assertEquals(0,images.state.destinations.plan.exceptionCount());
        }finally{controller.pause().stop().destroy();}
    }
}
