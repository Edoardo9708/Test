package it.edo.codecprobe;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import java.io.*;
import java.nio.file.Files;
import java.util.Set;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

/** Exercises real byte streams and SAF publication rollback; no encoder or JNI is replaced. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageOutputFailureTest {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private Context context;
    private Documents provider;
    private File encoded,existing;
    private static final byte[] SOURCE={0,0,0,16,'f','t','y','p','h','e','i','c',0,0,0,0};
    private static final byte[] PREVIOUS={9,8,7,6};

    public static class Documents extends AudioMetadataFlowTest.Documents {
        boolean createDenied,writeDenied,readDenied,deleteDenied;
        @Override public Bundle call(String method,String arg,Bundle extras) {
            if(method.equals("android:createDocument")&&createDenied)throw new SecurityException("creation denied");
            if(method.equals("android:deleteDocument")&&deleteDenied)throw new SecurityException("rollback denied");
            Bundle result=super.call(method,arg,extras);
            if(method.equals("android:deleteDocument"))result.putBoolean("result",true);
            return result;
        }
        @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException {
            if(mode.equals("w")&&writeDenied)throw new FileNotFoundException("write denied");
            if(mode.equals("r")&&readDenied)throw new FileNotFoundException("reread denied");
            return super.openFile(uri,mode);
        }
    }
    @Before public void prepare()throws Exception {
        context=RuntimeEnvironment.getApplication();provider=new Documents();provider.directory=temporary.newFolder("documents");
        ProviderInfo info=new ProviderInfo();info.authority="audio.documents";info.exported=true;info.grantUriPermissions=true;
        provider.attachInfo(context,info);ShadowContentResolver.registerProviderInternal(info.authority,provider);
        encoded=temporary.newFile("encoded.heic");Files.write(encoded.toPath(),SOURCE);
        existing=new File(provider.directory,"previous.heic");Files.write(existing.toPath(),PREVIOUS);
        provider.files.put("existing",existing);provider.names.put("existing","Photo.heic");
    }
    private IOException publishFailure() {
        return assertThrows(IOException.class,()->ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.jpg",()->false));
    }
    private void assertOriginalsPreserved()throws IOException {
        assertArrayEquals(SOURCE,Files.readAllBytes(encoded.toPath()));assertArrayEquals(PREVIOUS,Files.readAllBytes(existing.toPath()));
    }
    private void assertNoNewDocument()throws IOException {
        assertEquals(Set.of("existing"),provider.files.keySet());assertEquals(Set.of("existing"),provider.names.keySet());assertOriginalsPreserved();
    }
    @Test public void missingStagedFileNeverCreatesAnOutputOrDeletesTheExistingName()throws Exception {
        assertTrue(encoded.delete());assertThrows(FileNotFoundException.class,()->ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.jpg",()->false));
        assertEquals(Set.of("existing"),provider.files.keySet());assertArrayEquals(PREVIOUS,Files.readAllBytes(existing.toPath()));
    }
    @Test public void revokedCreatePermissionIsReportedWithoutOverwritingAnExistingFile()throws Exception {
        provider.createDenied=true;IOException error=publishFailure();
        assertTrue(error.getCause() instanceof SecurityException);assertNoNewDocument();
    }
    @Test public void writeFailureRollsBackOnlyTheNewDocument()throws Exception {
        provider.writeDenied=true;IOException error=publishFailure();
        assertTrue(error.getMessage().contains("write denied"));assertEquals(0,error.getSuppressed().length);assertNoNewDocument();
    }
    @Test public void unreadableOutputIsNotDeclaredSavedAndIsRolledBack()throws Exception {
        provider.readDenied=true;IOException error=publishFailure();
        assertTrue(error.getMessage().contains("reread denied"));assertNoNewDocument();
    }
    @Test public void failedRollbackRetainsThePrimaryErrorAndItsDiagnosticCause()throws Exception {
        provider.writeDenied=true;provider.deleteDenied=true;IOException error=publishFailure();
        assertTrue(error.getMessage().contains("write denied"));assertEquals(1,error.getSuppressed().length);
        assertTrue(error.getSuppressed()[0].getMessage().contains("rollback denied"));
        assertEquals(2,provider.files.size());assertOriginalsPreserved();
    }
}
