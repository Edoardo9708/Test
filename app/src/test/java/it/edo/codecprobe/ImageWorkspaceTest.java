package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageWorkspaceTest {
    @Before public void clear(){RuntimeEnvironment.getApplication().getSharedPreferences("image-workspace",0).edit().clear().commit();}
    static byte[] fixture()throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(new byte[]{-1,-40});
        segment(out,0xe1,"Exif\0\0original metadata".getBytes(StandardCharsets.US_ASCII));segment(out,0xe7,new byte[]{0,1,2,3});
        segment(out,0xc0,new byte[]{8,0,32,0,48,1,1,0x11,0});segment(out,0xda,new byte[]{1,1,0,0,63,0});out.write(new byte[]{1,2,3,-1,-39,9,8,7});return out.toByteArray();
    }
    private static void segment(ByteArrayOutputStream out,int marker,byte[] bytes)throws Exception{out.write(255);out.write(marker);out.write((bytes.length+2)>>>8);out.write(bytes.length+2);out.write(bytes);}
    private static void settle(ImageController images)throws Exception{long end=System.currentTimeMillis()+5000;while(images.state.reading||images.state.exporting){Shadows.shadowOf(Looper.getMainLooper()).idle();if(System.currentTimeMillis()>end)fail("Image worker did not complete");Thread.sleep(5);}Shadows.shadowOf(Looper.getMainLooper()).idle();}
    @Test public void jpegAnalysisExportAndDraftAreIndependentFromAudioVideo()throws Exception{
        var audioPrefs=RuntimeEnvironment.getApplication().getSharedPreferences("audio-workspace",0);String previous=audioPrefs.getString("selection","[]");audioPrefs.edit().putString("selection","audio-sentinel").commit();
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();File source=File.createTempFile("original-",".jpg"),report=File.createTempFile("inventory-",".json");byte[] original=fixture();Files.write(source.toPath(),original);
        try{
            ImageController images=controller.get().images;assertFalse(images.screen.export.isEnabled());
            Intent picked=new Intent().setData(Uri.fromFile(source));images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,picked);settle(images);
            assertNotNull(images.state.info);assertEquals(48,images.state.info.width);assertTrue(images.screen.export.isEnabled());assertTrue(images.compose.getState().getAllDetails().contains("3 byte dopo"));
            images.screen.export.performClick();Intent create=Shadows.shadowOf(controller.get()).getNextStartedActivity();assertEquals(Intent.ACTION_CREATE_DOCUMENT,create.getAction());assertEquals("application/json",create.getType());
            images.selection.result(ImageSelection.REPORT,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(report)));settle(images);
            JSONObject json=new JSONObject(new String(Files.readAllBytes(report.toPath()),StandardCharsets.UTF_8));assertFalse(json.getBoolean("heicConversionApproved"));assertFalse(json.getBoolean("pixelsDecoded"));assertEquals(2,json.getJSONArray("metadataPayloads").length());
            JSONObject block=json.getJSONArray("metadataPayloads").getJSONObject(0);byte[] payload=Base64.getDecoder().decode(block.getString("base64"));assertArrayEquals(Arrays.copyOfRange(original,block.getInt("payloadOffset"),block.getInt("payloadOffset")+block.getInt("bytes")),payload);
            assertArrayEquals(original,Files.readAllBytes(source.toPath()));assertEquals(1,ImageDraftStore.load(controller.get()).inputs.size());
            assertEquals("audio-sentinel",audioPrefs.getString("selection",""));
            images.compose.command(PhotoCommand.CLEAR);org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(images.state.inputs.isEmpty());assertNull(images.state.info);assertFalse(images.screen.export.isEnabled());assertArrayEquals(original,Files.readAllBytes(source.toPath()));
        }finally{controller.pause().stop().destroy();source.delete();report.delete();audioPrefs.edit().putString("selection",previous).commit();}
    }
    @Test public void cancelledSelectionKeepsDraftAndUnsupportedFileCannotExport()throws Exception{
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();File source=File.createTempFile("unsupported-",".heic");Files.write(source.toPath(),"HEIC not JPEG".getBytes(StandardCharsets.UTF_8));
        try{ImageController images=controller.get().images;images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(source)));settle(images);assertNull(images.state.info);assertFalse(images.screen.export.isEnabled());assertTrue(images.state.notice.contains("Non è un JPEG"));images.selection.result(ImageSelection.FILES,Activity.RESULT_CANCELED,null);assertEquals(1,images.state.inputs.size());}
        finally{controller.pause().stop().destroy();source.delete();}
    }
    @Test public void threeTabsSelectImagesAndSwitchWithoutGrowingSecondaryBackStack(){
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=controller.get().images;
            assertTrue(images.compose.getView().isShown());
            assertNull(Shadows.shadowOf(controller.get()).getNextStartedActivity());
            images.compose.command(PhotoCommand.AUDIO);
            assertEquals(AudioActivity.class.getName(),Shadows.shadowOf(controller.get()).getNextStartedActivity().getComponent().getClassName());
            assertTrue(controller.get().isFinishing());
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void pendingExportSurvivesRecreationAndSourceCannotBeOverwritten()throws Exception{
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();File source=File.createTempFile("original-",".jpg"),report=File.createTempFile("inventory-",".json");byte[] original=fixture();Files.write(source.toPath(),original);
        try{
            ImageController images=controller.get().images;images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(source)));settle(images);
            images.screen.export.performClick();controller.recreate();images=controller.get().images;settle(images);
            images.selection.result(ImageSelection.REPORT,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(report)));settle(images);assertEquals("JPEG",new JSONObject(new String(Files.readAllBytes(report.toPath()),StandardCharsets.UTF_8)).getString("format"));
            images.export(Uri.fromFile(source),Uri.fromFile(source));settle(images);assertTrue(images.state.notice.contains("coincide"));assertArrayEquals(original,Files.readAllBytes(source.toPath()));
        }finally{controller.pause().stop().destroy();source.delete();report.delete();}
    }
    @Test public void imagesAreReachableFromBothExistingWorkspaces(){
        var video=Robolectric.buildActivity(MainActivity.class).setup();var audio=Robolectric.buildActivity(AudioActivity.class).setup();try{
            video.get().home.screen.navigation.images.performClick();assertEquals(ImageActivity.class.getName(),Shadows.shadowOf(video.get()).getNextStartedActivity().getComponent().getClassName());assertFalse(video.get().isFinishing());
            audio.get().audio.screen.navigation.images.performClick();assertEquals(ImageActivity.class.getName(),Shadows.shadowOf(audio.get()).getNextStartedActivity().getComponent().getClassName());assertTrue(audio.get().isFinishing());
        }finally{audio.pause().stop().destroy();video.pause().stop().destroy();}
    }
}
