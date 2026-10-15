package it.edo.codecprobe;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.provider.MediaStore;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class InputCreationDateTest {
    static class Dates extends ContentProvider {
        int queries;Long added=1700000000L;
        public boolean onCreate(){return true;}
        public Cursor query(Uri uri,String[] projection,String s,String[] a,String o){
            queries++;assertArrayEquals(new String[]{MediaStore.MediaColumns.DATE_ADDED},projection);
            MatrixCursor row=new MatrixCursor(projection);row.addRow(new Object[]{added});return row;
        }
        public String getType(Uri uri){return null;}public Uri insert(Uri uri,ContentValues values){throw new AssertionError("Original must not change");}
        public int update(Uri uri,ContentValues values,String s,String[] a){throw new AssertionError("Original must not change");}
        public int delete(Uri uri,String s,String[] a){throw new AssertionError("Original must not change");}
    }
    @Test public void readsOriginalDateAddedInSecondsOnlyWhenInternalDateMissing()throws Exception {
        Dates dates=new Dates();ShadowContentResolver.registerProviderInternal("media",dates);
        Context context=RuntimeEnvironment.getApplication();Uri input=Uri.parse("content://media/external/video/media/77?requireOriginal=1");
        List<String> log=new ArrayList<>();
        long fallback=InputCreationDate.fromAndroid(context,input,0,"19040101T000000.000Z",log::add);
        assertEquals(3782844800L,fallback);assertEquals(1,dates.queries);
        assertTrue(log.stream().anyMatch(s->s.contains("provenienza=Android MediaStore.DATE_ADDED")));
        assertEquals(0,InputCreationDate.fromAndroid(context,input,0,"20241012T120000.000Z",log::add));
        assertEquals(0,InputCreationDate.fromAndroid(context,input,1234,null,log::add));assertEquals(1,dates.queries);
        dates.added=null;assertEquals(0,InputCreationDate.fromAndroid(context,input,0,null,log::add));
        assertEquals(0,InputCreationDate.fromAndroid(context,Uri.parse("content://cloud/video/77"),0,null,log::add));
    }
}
