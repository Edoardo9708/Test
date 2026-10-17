package it.edo.codecprobe;

import android.Manifest;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class SelectedMetadataReportTest {
    public static class Missing extends ContentProvider {
        final List<String> opened=new ArrayList<>();
        public boolean onCreate(){return true;}
        public Cursor query(Uri uri,String[] projection,String s,String[] a,String o){return new MatrixCursor(new String[]{"_display_name"});}
        public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{opened.add(uri.toString());throw new FileNotFoundException("Test missing input");}
        public String getType(Uri uri){return "video/mp4";}
        public Uri insert(Uri u,ContentValues v){throw new AssertionError("Read-only");}
        public int update(Uri u,ContentValues v,String s,String[] a){throw new AssertionError("Read-only");}
        public int delete(Uri u,String s,String[] a){throw new AssertionError("Read-only");}
    }
    @Test public void oneUnreadableFileDoesNotHideOthersAndCancellationDoesNotBecomeSuccess()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Shadows.shadowOf((android.app.Application)context).grantPermissions(Manifest.permission.ACCESS_MEDIA_LOCATION);
        Missing provider=new Missing();ShadowContentResolver.registerProviderInternal("metadata-test",provider);
        List<Uri> inputs=List.of(Uri.parse("content://metadata-test/one"),Uri.parse("content://metadata-test/two"));
        StringWriter out=new StringWriter();List<Integer> progress=new ArrayList<>();
        SelectedMetadataReport.write(context,inputs,List.of("original.mp4","converted.mp4"),out,()->false,progress::add);
        assertEquals(List.of(1,2),progress);assertTrue(out.toString().contains("FILE 1 / 2 · original.mp4"));assertTrue(out.toString().contains("FILE 2 / 2 · converted.mp4"));assertTrue(out.toString().contains("FINE RAPPORTO METADATI"));assertEquals(2,provider.opened.size());
        try{SelectedMetadataReport.write(context,inputs,List.of("one","two"),new StringWriter(),()->true,n->{});fail();}catch(InterruptedIOException expected){}
        StringWriter partial=new StringWriter();SelectedMetadataReport.section(partial,"unavailable",()->false,w->{throw new IOException("broken metadata");});assertTrue(partial.toString().contains("SEZIONE NON COMPLETA"));
    }
    @Test public void exportCapturesEverySelectedFileAndKeepsConversionSessionUntouched()throws Exception{
        var activity=Robolectric.buildActivity(MainActivity.class).setup();HomeController home=activity.get().home;
        Missing provider=new Missing();ShadowContentResolver.registerProviderInternal("metadata-test",provider);
        File snapshot=null;
        try{
            Shadows.shadowOf(activity.get().getApplication()).grantPermissions(Manifest.permission.ACCESS_MEDIA_LOCATION,Manifest.permission.READ_MEDIA_VIDEO);
            home.state.selectedInputs.addAll(List.of("content://metadata-test/one","content://metadata-test/two"));home.state.selectedNames.addAll(List.of("original.mp4","converted.mp4"));home.state.input=Uri.parse(home.state.selectedInputs.get(1));home.state.sourceName="converted.mp4";
            ConversionService.State before=ConversionService.snapshot();
            home.diagnostics.exportMetadata();
            // Change the editable selection after dispatch: the export must retain its captured list.
            home.state.selectedInputs.clear();home.state.selectedNames.clear();
            home.diagnostics.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(home.state.pendingLog);snapshot=new File(home.state.pendingLog);String report=new String(Files.readAllBytes(snapshot.toPath()),java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(report.contains("original.mp4"));assertTrue(report.contains("converted.mp4"));assertEquals(2,provider.opened.size());
            assertEquals(before.sessionId,ConversionService.snapshot().sessionId);assertEquals(before.running,ConversionService.snapshot().running);
            assertEquals(before.sizeReport,ConversionService.snapshot().sizeReport);assertFalse(home.state.exportingDiagnostics);
        }finally{if(snapshot!=null)snapshot.delete();activity.pause().stop().destroy();}
    }
}
