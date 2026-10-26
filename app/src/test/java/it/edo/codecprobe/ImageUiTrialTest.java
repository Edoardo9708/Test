package it.edo.codecprobe;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class ImageUiTrialTest {
    final ArrayList<File> files=new ArrayList<>();
    @Before public void reset(){
        var c=RuntimeEnvironment.getApplication();
        for(String pref:new String[]{"image-workspace","image-settings"})c.getSharedPreferences(pref,0).edit().clear().commit();
        ImageMetadataCache.memory.clear();
    }
    @After public void cleanup(){for(File file:files)file.delete();reset();}
    private String photo(String name)throws Exception{
        File file=File.createTempFile(name,".jpg");Files.write(file.toPath(),ImageWorkspaceTest.fixture());files.add(file);return Uri.fromFile(file).toString();
    }
    static void settle(ImageController images)throws Exception{
        long end=System.currentTimeMillis()+10000;
        while((images.state.reading||images.state.importing)&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}
        Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(images.state.reading);assertFalse(images.state.importing);
    }
    @Test public void navigationRetainsDraftAndQualityAcrossRecreation()throws Exception{
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;assertNull(images.screen.sections.current());
            images.screen.sections.focusCard(ImageScreen.FILES);assertEquals(ImageScreen.FILES,images.screen.sections.current());
            String input=photo("draft");images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.parse(input)));settle(images);
            images.next();images.quality(70);
            int quality=images.state.options.quality;images.back();
            assertEquals(ImageScreen.FILES,images.screen.sections.current());images.back();
            assertNull(images.screen.sections.current());assertEquals(Collections.singletonList(input),images.state.inputs);
            images.screen.sections.focusCard(ImageScreen.FILES);images.next();
            assertEquals(quality,images.state.options.quality);
            assertEquals(ImageScreen.COMPRESSION,images.compose.getState().getStep());
            activity.recreate();assertEquals(ImageScreen.COMPRESSION,activity.get().images.screen.sections.current());
        }finally{activity.pause().stop().destroy();}
    }
    @Test public void longPressSelectionUsesGroupAndRequiresConfirmationBeforeRemoval()throws Exception{
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;for(int n=0;n<3;n++)images.state.inputs.add(photo("selection"+n));images.state.clamp();images.open();settle(images);
            images.compose.select(0,true);images.compose.select(2,false);
            assertEquals(Arrays.asList(0,2),images.selectionIndices());assertEquals(2,images.selectedPhotos.size());
            assertTrue(images.compose.getState().getName().contains("2 foto"));images.compose.command(PhotoCommand.DETAILS);
            assertTrue(images.compose.getState().getDetailsOpen());
            images.compose.command(PhotoCommand.REMOVE);assertEquals(3,images.state.inputs.size());
            ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_NEGATIVE).performClick();assertEquals(3,images.state.inputs.size());
            images.compose.command(PhotoCommand.REMOVE);ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();settle(images);
            assertEquals(1,images.state.inputs.size());for(File file:files)assertTrue(file.exists());
            images.compose.command(PhotoCommand.CLEAR);assertEquals(1,images.state.inputs.size());
            ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(images.state.inputs.isEmpty());
        }finally{activity.pause().stop().destroy();}
    }
    @Test public void folderImportIsAdditiveAndKeepsExistingDestinationEvenForDuplicates()throws Exception{
        ShadowContentResolver.registerProviderInternal("folderphotos",new ImageFolderImportTest.Folder());
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;String old=photo("existing");images.state.inputs.add(old);images.state.inputs.add("content://folderphotos/tree/camera/document/photo0");images.state.clamp();
            images.state.destinations.sync(images.state.inputs,null);images.state.destinations.plan.setFolder(0,"content://saved/tree/custom");
            images.importFolder(Uri.parse(ImageFolderImportTest.TREE));settle(images);
            assertTrue(images.state.inputs.contains(old));assertEquals(new HashSet<>(images.state.inputs).size(),images.state.inputs.size());
            assertEquals("content://saved/tree/custom",images.state.destinations.plan.files.get(0).folder);
            int before=images.state.inputs.size();images.importFolder(Uri.parse(ImageFolderImportTest.TREE));settle(images);assertEquals(before,images.state.inputs.size());
        }finally{activity.pause().stop().destroy();}
    }
    @Test public void addingFilesKeepsFocusNamesAndExistingMetadataCache()throws Exception{
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;String one=photo("first"),two=photo("second"),three=photo("third");
            images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.parse(one)));settle(images);
            images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.parse(two)));settle(images);
            images.state.selected=1;images.open();settle(images);var cached=images.state.info;String name=images.state.names.get(1);
            images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.parse(three)));settle(images);
            assertEquals(3,images.state.inputs.size());assertEquals(1,images.state.selected);assertEquals(name,images.state.names.get(1));assertSame(cached,images.state.info);
            images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.parse(two)));assertEquals(3,images.state.inputs.size());assertSame(cached,images.state.info);
        }finally{activity.pause().stop().destroy();}
    }
    @Test public void folderPickerFreezesSelectedGroupAcrossRecreation()throws Exception{
        ShadowContentResolver.registerProviderInternal("destinations",new MediaDestinationsTest.Directories());
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;for(int n=0;n<3;n++)images.state.inputs.add(photo("destination"+n));images.state.clamp();images.setFolder(MediaDestinationsTest.A);images.open();settle(images);
            images.toggleSelection(0);images.toggleSelection(2);images.destinations.photoException(true);images.destinations.choosePhotoFolder();activity.recreate();images=activity.get().images;
            images.selection.result(MediaDestinationsController.FILE,Activity.RESULT_OK,new Intent().setData(Uri.parse(MediaDestinationsTest.B)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            assertEquals(MediaDestinationsTest.B,images.state.destinations.plan.files.get(0).folder);
            assertEquals(MediaDestinationsTest.A,images.state.destinations.plan.effective(1).folder);
            assertEquals(MediaDestinationsTest.B,images.state.destinations.plan.files.get(2).folder);
        }finally{activity.pause().stop().destroy();}
    }
    @Test public void composeIsTheOnlyWorkspaceRootAndNoHiddenPhotoPanelsAreBuilt(){
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;
            android.view.ViewGroup content=activity.get().findViewById(android.R.id.content);
            assertEquals(1,content.getChildCount());
            assertSame(images.compose.getView(),content.getChildAt(0));
            assertEquals(2,images.screen.sections.bodies.size());
            assertFalse(images.screen.sections.bodies.containsKey(ImageScreen.FILES));
            assertFalse(images.screen.sections.bodies.containsKey(ImageScreen.COMPRESSION));
            assertNotNull(images.screen.sections.bodies.get(ImageScreen.HEIC));
            assertNotNull(images.screen.sections.bodies.get(ImageScreen.DETAILS));
        }finally{activity.pause().stop().destroy();}
    }
}
