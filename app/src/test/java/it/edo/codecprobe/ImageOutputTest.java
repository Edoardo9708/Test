package it.edo.codecprobe;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;
import java.nio.file.Files;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageOutputTest {
    public static class Documents extends AudioMetadataFlowTest.Documents {
        boolean corrupt;
        @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
            if(corrupt&&mode.equals("r"))try(RandomAccessFile out=new RandomAccessFile(files.get(uri.getLastPathSegment()),"rw")){out.seek(0);out.write(99);}catch(IOException e){throw new FileNotFoundException(e.toString());}
            return super.openFile(uri,mode);
        }
    }
    Context context;Documents provider;File encoded;
    @Before public void setup()throws Exception{context=RuntimeEnvironment.getApplication();provider=new Documents();provider.directory=Files.createTempDirectory("image-docs").toFile();android.content.pm.ProviderInfo info=new android.content.pm.ProviderInfo();info.authority="audio.documents";info.exported=true;info.grantUriPermissions=true;provider.attachInfo(context,info);ShadowContentResolver.registerProviderInternal("audio.documents",provider);encoded=new File(provider.directory,"encoded.heic");Files.write(encoded.toPath(),new byte[]{1,2,3,4,5});}
    @After public void cleanup(){for(File f:provider.directory.listFiles())f.delete();provider.directory.delete();}
    @Test public void savesReReadsAllBytesAndAddsSuffixInsteadOfOverwriting()throws Exception{
        Uri first=ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.JPG",()->false),second=ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.JPG",()->false);assertNotEquals(first,second);assertEquals(java.util.List.of("Photo.heic","Photo (2).heic"),new java.util.ArrayList<>(provider.names.values()));for(File saved:provider.files.values())assertArrayEquals(Files.readAllBytes(encoded.toPath()),Files.readAllBytes(saved.toPath()));
    }
    @Test public void sameLengthCorruptionRollsBackTheNewDocument(){provider.corrupt=true;assertThrows(IOException.class,()->ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.jpg",()->false));assertTrue(provider.files.isEmpty());assertTrue(provider.names.isEmpty());}
    @Test public void cancellationPublishesNothing(){assertThrows(IOException.class,()->ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.jpg",()->true));assertTrue(provider.files.isEmpty());}
}
