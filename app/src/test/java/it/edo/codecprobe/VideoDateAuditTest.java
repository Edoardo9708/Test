package it.edo.codecprobe;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class VideoDateAuditTest {
    private static final long UNIX=Instant.parse("2026-10-16T12:00:00Z").getEpochSecond();
    private static final long MP4=UNIX+CreationDatePolicy.MP4_EPOCH_SECONDS;
    private static VideoDateAudit.Snapshot sample(long header,String raw,Long taken){
        return new VideoDateAudit.Snapshot(header,raw,180000L,new VideoDateAudit.Index(taken,UNIX,UNIX,"indice letto"));
    }
    @Test public void identicalInternalDatesDoNotHideThreeMinuteIndexDifference()throws Exception {
        List<String> log=new ArrayList<>();
        VideoDateAudit.verify(sample(MP4,"20261016T120000.000Z",UNIX*1000-180000),
            sample(MP4,"2026-10-16T14:00:00+02:00",UNIX*1000),0,"FILE SALVATO",log::add);
        assertTrue(log.stream().anyMatch(s->s.startsWith("OK DATE INTERNE")));
        assertTrue(log.stream().anyMatch(s->s.startsWith("AVVISO DATA GALLERIA")&&s.contains("180000 ms")));
        assertFalse(log.stream().anyMatch(s->s.startsWith("OK DATA GALLERIA")));
    }
    @Test public void dateChangeExposedByAndroidFailsEvenIfHeaderMatches()throws Exception {
        for(int seconds:new int[]{120,-180,3600}){
            try{
                VideoDateAudit.verify(sample(MP4,"2026-10-16T12:00:00Z",null),
                    sample(MP4,Instant.ofEpochSecond(UNIX+seconds).toString(),null),0,"FILE SALVATO",s->{});
                fail("Changed reader date accepted");
            }catch(IOException expected){assertTrue(expected.getMessage().contains("DATA LETTA DA ANDROID DIVERSA"));assertTrue(expected.getMessage().contains("Scarto ms="+(seconds*1000)));}
        }
    }
    @Test public void headerChangeFailsAndExplicitFallbackIsVerified()throws Exception {
        try{VideoDateAudit.verify(sample(MP4,"2026-10-16T12:00:00Z",null),sample(MP4+1,"2026-10-16T12:00:00Z",null),0,"MP4 TEMPORANEO",s->{});fail();}
        catch(IOException expected){assertTrue(expected.getMessage().contains("DATA HEADER DIVERSA"));}
        VideoDateAudit.verify(sample(0,null,null),sample(MP4,"2026-10-16T12:00:00Z",null),MP4,"MP4 TEMPORANEO",s->{});
    }
    @Test public void missingOrUnparseableDatesNeverClaimGalleryVerified()throws Exception {
        List<String> log=new ArrayList<>();
        VideoDateAudit.verify(sample(0,null,null),sample(0,null,null),0,"FILE SALVATO",log::add);
        assertTrue(log.stream().anyMatch(s->s.startsWith("DATA GALLERIA NON VERIFICATA")));
        assertFalse(log.stream().anyMatch(s->s.startsWith("OK DATE INTERNE")||s.startsWith("OK DATA GALLERIA")));
        log.clear();VideoDateAudit.verify(sample(MP4,"unknown-original",null),sample(MP4,"unknown-new",null),0,"FILE SALVATO",log::add);
        assertTrue(log.stream().anyMatch(s->s.startsWith("AVVISO DATE")));
    }
    public static class Dates extends ContentProvider {
        boolean denied,empty;int queries;
        public boolean onCreate(){return true;}
        public Cursor query(Uri uri,String[] projection,String where,String[] args,String order){
            queries++;if(denied)throw new SecurityException("No access");
            MatrixCursor rows=new MatrixCursor(new String[]{MediaStore.MediaColumns.DATE_MODIFIED,MediaStore.MediaColumns.DATE_TAKEN,MediaStore.MediaColumns.DATE_ADDED});
            if(!empty)rows.addRow(new Object[]{UNIX-2,UNIX*1000-180000,UNIX});return rows;
        }
        public String getType(Uri uri){return null;}
        public Uri insert(Uri uri,ContentValues values){throw new AssertionError("Dates must not be written");}
        public int update(Uri uri,ContentValues values,String where,String[] args){throw new AssertionError("Dates must not be written");}
        public int delete(Uri uri,String where,String[] args){throw new AssertionError("Dates must not be deleted");}
    }
    @Test public void indexReadsNullableNamedColumnsWithoutWritingAndHandlesUnavailableProviders(){
        Dates provider=new Dates();ShadowContentResolver.registerProviderInternal("media",provider);
        Context context=RuntimeEnvironment.getApplication();Uri uri=Uri.parse("content://media/external/video/media/77?requireOriginal=1");
        VideoDateAudit.Index index=VideoDateAudit.readIndex(context,uri,s->{});
        assertEquals(Long.valueOf(UNIX*1000-180000),index.taken);assertEquals(Long.valueOf(UNIX),index.added);assertEquals(Long.valueOf(UNIX-2),index.modified);
        provider.empty=true;assertNull(VideoDateAudit.readIndex(context,uri,s->{}).taken);
        provider.denied=true;List<String> log=new ArrayList<>();assertNull(VideoDateAudit.readIndex(context,uri,log::add).taken);
        assertTrue(log.stream().anyMatch(s->s.contains("SecurityException")));
        int before=provider.queries;
        assertNull(VideoDateAudit.readIndex(context,Uri.parse("content://cloud/item/77"),s->{}).taken);assertEquals(before,provider.queries);
    }
}
