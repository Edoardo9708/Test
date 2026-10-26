package it.edo.codecprobe;

import android.view.View;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class PhotoReferenceTest {
    @Test public void generalDestinationNeverOverridesExceptions(){
        DestinationPlan plan=new DestinationPlan();plan.reset(4,"general");plan.enableExceptions();
        plan.exception(0,true);plan.setFolder(0,"own");plan.setAllFolder("new-general");
        assertEquals("own",plan.effective(0).folder);assertEquals("new-general",plan.effective(1).folder);assertEquals(1,plan.exceptionCount());
        plan.exception(0,true);assertEquals("own",plan.effective(0).folder);
        plan.exception(0,false);assertEquals("new-general",plan.effective(0).folder);assertEquals(0,plan.exceptionCount());
    }
    @Test public void draftKeepsExceptionsWithTheirSourceAfterReordering(){
        DestinationDraft draft=new DestinationDraft();draft.sync(Arrays.asList("one","two"),"general");draft.plan.enableExceptions();draft.plan.setFolder(0,"own");
        var prefs=RuntimeEnvironment.getApplication().getSharedPreferences("photo-reference-test",0);var edit=prefs.edit().clear();draft.save(edit,Arrays.asList("one","two"),"general");edit.commit();
        DestinationDraft restored=new DestinationDraft();restored.load(prefs,Arrays.asList("one","two"),"general");restored.sync(Arrays.asList("two","three","one"),"general");
        assertEquals("own",restored.plan.effective(2).folder);assertEquals("general",restored.plan.effective(0).folder);assertEquals(1,restored.plan.exceptionCount());prefs.edit().clear().commit();
    }
    @Test public void documentIdentifiersAreNeverDisplayedAsFullNames(){
        assertEquals("IMG_123.jpg",ImageFiles.basename("primary:DCIM/Test/IMG_123.jpg"));assertEquals("Foto JPEG",ImageFiles.basename(null));
    }
    @Test public void selectingMixedGroupPreservesExistingExceptions(){
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;images.state.inputs.clear();images.state.names.clear();
            images.state.inputs.addAll(Arrays.asList("content://preview/one","content://preview/two","content://preview/three"));images.state.clamp();images.setFolder("general");
            images.state.destinations.plan.setFolder(0,"own");images.toggleSelection(0);images.toggleSelection(2);
            var model=images.destinations.photo;assertFalse(model.general);assertFalse(model.exception);images.destinations.photoException(true);
            assertEquals(2,images.state.destinations.plan.exceptionCount());assertEquals("own",images.state.destinations.plan.effective(0).folder);assertEquals("general",images.state.destinations.plan.effective(2).folder);
            assertTrue(model.allLabel.contains("1"));
        }finally{activity.pause().stop().destroy();RuntimeEnvironment.getApplication().getSharedPreferences("image-workspace",0).edit().clear().commit();}
    }
}
