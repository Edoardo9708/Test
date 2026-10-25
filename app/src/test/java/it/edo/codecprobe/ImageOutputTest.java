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
        boolean corrupt;int listings;String lastMime;
        @Override public android.os.Bundle call(String method,String arg,android.os.Bundle extras){if("android:createDocument".equals(method))lastMime=extras.getString(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE);return super.call(method,arg,extras);}
        @Override public android.database.Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){if("children".equals(uri.getLastPathSegment()))listings++;return super.query(uri,projection,selection,args,sort);}
        @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
            if(corrupt&&mode.equals("r"))try(RandomAccessFile out=new RandomAccessFile(files.get(uri.getLastPathSegment()),"rw")){out.seek(0);out.write(99);}catch(IOException e){throw new FileNotFoundException(e.toString());}
            return super.openFile(uri,mode);
        }
    }
    Context context;Documents provider;File encoded;
    @Before public void setup()throws Exception{context=RuntimeEnvironment.getApplication();provider=new Documents();provider.directory=Files.createTempDirectory("image-docs").toFile();android.content.pm.ProviderInfo info=new android.content.pm.ProviderInfo();info.authority="audio.documents";info.exported=true;info.grantUriPermissions=true;provider.attachInfo(context,info);ShadowContentResolver.registerProviderInternal("audio.documents",provider);encoded=new File(provider.directory,"encoded.heic");Files.write(encoded.toPath(),new byte[]{0,0,0,16,'f','t','y','p','h','e','i','c',0,0,0,0});}
    @After public void cleanup(){for(File f:provider.directory.listFiles())f.delete();provider.directory.delete();}
    @Test public void savesReReadsAllBytesAndAddsSuffixInsteadOfOverwriting()throws Exception{
        Uri first=ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.JPG",()->false),second=ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.JPG",()->false);assertNotEquals(first,second);assertEquals(java.util.List.of("Photo.heic","Photo (2).heic"),new java.util.ArrayList<>(provider.names.values()));for(File saved:provider.files.values())assertArrayEquals(Files.readAllBytes(encoded.toPath()),Files.readAllBytes(saved.toPath()));
    }
    @Test public void motionNamesKeepMpSuffixAcrossCollisionsAndHeaderScanSkipsPayload()throws Exception{
        try(DataOutputStream out=new DataOutputStream(new FileOutputStream(encoded))){out.writeInt(16);out.writeBytes("ftypheic");out.writeInt(0);out.writeInt(12);out.writeBytes("mpvd");out.writeInt(123);}
        assertTrue(ImageOutput.hasMotionVideo(encoded));
        ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.JPG",()->false);
        ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.JPG",()->false);
        assertEquals(java.util.List.of("Photo_MP.heic","Photo (2)_MP.heic"),new java.util.ArrayList<>(provider.names.values()));
        assertEquals("AlreadyMP.heic",ImageOutput.name("AlreadyMP.jpg",true));
        assertEquals("Already (2)MP.heic",ImageOutput.unique("AlreadyMP.heic",java.util.Set.of("AlreadyMP.heic")));
        try(RandomAccessFile file=new RandomAccessFile(encoded,"rw")){file.seek(16);file.writeInt(99);}
        assertThrows(IOException.class,()->ImageOutput.hasMotionVideo(encoded));
    }
    @Test public void wholeBatchListsDestinationOnceAndRetainsCaseInsensitiveReservations()throws Exception{
        ImageOutput.Names names=new ImageOutput.Names();
        for(int i=0;i<100;i++)ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,i%2==0?"Photo.JPG":"photo.JPG",()->false,names);
        assertEquals(1,provider.listings);assertEquals(100,provider.files.size());
        java.util.Set<String> unique=new java.util.HashSet<>();for(String name:provider.names.values())assertTrue(unique.add(name.toLowerCase(java.util.Locale.ROOT)));
        ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.JPG",()->false,new ImageOutput.Names());
        assertEquals(2,provider.listings);assertEquals(101,provider.files.size());
    }
    @Test public void sameLengthCorruptionRollsBackTheNewDocument(){provider.corrupt=true;assertThrows(IOException.class,()->ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.jpg",()->false));assertTrue(provider.files.isEmpty());assertTrue(provider.names.isEmpty());}
    @Test public void cancellationPublishesNothing(){assertThrows(IOException.class,()->ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.jpg",()->true));assertTrue(provider.files.isEmpty());}
    private ImageRunState.Result publishOriginal(int size,boolean smaller,boolean copy)throws Exception{
        File original=new File(provider.directory,"original.jpeg");byte[] bytes=new byte[size];for(int i=0;i<size;i++)bytes[i]=(byte)(i+10);Files.write(original.toPath(),bytes);
        ImageOptions options=new ImageOptions(65,"slow","standard",AudioMetadataFlowTest.TREE.toString(),smaller,false,copy,0);
        return ImageConverter.publish(context,original,encoded,"Photo.JPEG",options,"HEIC verified",ImageFingerprint.file(original,()->false),()->false,s->{},new ImageOutput.Names());
    }
    @Test public void largerHeicCopiesExactOriginalWithJpegNameAndZeroSaving()throws Exception{
        var result=publishOriginal(8,true,true);assertEquals(ImageRunState.Outcome.COPIED_ORIGINAL,result.outcome);assertEquals(8,result.outputBytes);assertEquals(result.sourceSha,result.outputSha);assertEquals("image/jpeg",result.mime);assertEquals("image/jpeg",provider.lastMime);
        assertEquals(java.util.List.of("Photo.JPEG"),new java.util.ArrayList<>(provider.names.values()));
        assertArrayEquals(Files.readAllBytes(new File(provider.directory,"original.jpeg").toPath()),Files.readAllBytes(provider.files.values().iterator().next().toPath()));
        var run=new ImageRunState();run.add(result);assertEquals(1,run.copied);assertEquals(0,run.saved);assertEquals(run.inputSavedBytes,run.outputSavedBytes);
        publishOriginal(8,true,true);assertEquals(java.util.List.of("Photo.JPEG","Photo (2).JPEG"),new java.util.ArrayList<>(provider.names.values()));
    }
    @Test public void disabledFallbackSkipsButDisabledSizeFilterSavesHeic()throws Exception{
        assertEquals(ImageRunState.Outcome.NOT_SMALLER,publishOriginal(8,true,false).outcome);assertTrue(provider.files.isEmpty());
        assertEquals(ImageRunState.Outcome.SAVED,publishOriginal(8,false,true).outcome);assertEquals(java.util.List.of("Photo.heic"),new java.util.ArrayList<>(provider.names.values()));
    }
    @Test public void equalSizeCopiesOriginalAndSmallerHeicStillWins()throws Exception{
        assertEquals(ImageRunState.Outcome.COPIED_ORIGINAL,publishOriginal(16,true,true).outcome);
        assertEquals(ImageRunState.Outcome.SAVED,publishOriginal(64,true,true).outcome);assertEquals(java.util.List.of("Photo.JPEG","Photo.heic"),new java.util.ArrayList<>(provider.names.values()));
    }
    @Test public void originalCopyReadbackFailureRollsBackWithoutChangingSource()throws Exception{
        provider.corrupt=true;assertThrows(IOException.class,()->publishOriginal(8,true,true));assertTrue(provider.files.isEmpty());
        assertArrayEquals(new byte[]{10,11,12,13,14,15,16,17},Files.readAllBytes(new File(provider.directory,"original.jpeg").toPath()));
    }
}
