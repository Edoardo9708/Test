package it.edo.codecprobe;

import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class ImageProgressJournalTest {
    Context context;
    @Before public void reset(){context=RuntimeEnvironment.getApplication();context.deleteDatabase(ImageProgressJournal.DATABASE);}
    private ImageOptions options(){return new ImageOptions(65,"slow","standard","content://destinations/tree/out",true,false,0);}
    private ImageRunState.Result saved(File original,File output)throws Exception{
        return new ImageRunState.Result("foto.jpg",original.length(),output.length(),Uri.fromFile(output).toString(),"Salvato",ImageRunState.Outcome.SAVED,"OK",ImageFingerprint.file(original,()->false),ImageFingerprint.file(output,()->false));
    }
    @Test public void reuseChecksBothFileContentsAndSettingsEvenWhenSizeDoesNotChange()throws Exception{
        File original=File.createTempFile("journal-original-",".jpg"),output=File.createTempFile("journal-output-",".heic");
        try(ImageProgressJournal journal=new ImageProgressJournal(context)){
            Files.write(original.toPath(),new byte[]{1,2,3,4});Files.write(output.toPath(),new byte[]{5,6,7,8});String input=Uri.fromFile(original).toString();ImageRunState.Result result=saved(original,output);
            journal.record(input,options().folder,options(),result);
            assertEquals(ImageRunState.Outcome.ALREADY_DONE,journal.reusable(context,input,"foto.jpg",options(),()->false).outcome);
            Files.write(original.toPath(),new byte[]{9,2,3,4});assertNull(journal.reusable(context,input,"foto.jpg",options(),()->false));Files.write(original.toPath(),new byte[]{1,2,3,4});
            Files.write(output.toPath(),new byte[]{9,6,7,8});assertNull(journal.reusable(context,input,"foto.jpg",options(),()->false));Files.write(output.toPath(),new byte[]{5,6,7,8});
            ImageOptions different=new ImageOptions(70,"slow","standard",options().folder,true,false,0);assertNull(journal.reusable(context,input,"foto.jpg",different,()->false));
            try{journal.reusable(context,input,"foto.jpg",options(),()->true);fail("Cancellation ignored");}catch(InterruptedIOException expected){}
            output.delete();assertNull(journal.reusable(context,input,"foto.jpg",options(),()->false));
        }finally{original.delete();output.delete();}
    }
    @Test public void latestOutcomeReplacesThePreviousOneAndFailuresCanBeLoadedWithoutDetails()throws Exception{
        String input="content://photo/one";
        try(ImageProgressJournal journal=new ImageProgressJournal(context)){
            journal.record(input,options().folder,options(),new ImageRunState.Result("foto.jpg",0,0,null,"Errore",ImageRunState.Outcome.FAILED,"x".repeat(100000)));
            ImageQueueStore.Queue retry=journal.failures();assertEquals(Collections.singletonList(input),retry.inputs);assertEquals(Collections.singletonList(options().folder),retry.folders);
            journal.record(input,options().folder,options(),new ImageRunState.Result("foto.jpg",100,50,"content://output/one","Salvato",ImageRunState.Outcome.SAVED,"","a".repeat(64),"b".repeat(64)));
            assertTrue(journal.failures().inputs.isEmpty());
            try(var rows=journal.getReadableDatabase().rawQuery("SELECT count(*) FROM progress",null)){assertTrue(rows.moveToFirst());assertEquals(1,rows.getInt(0));}
        }
    }
    @Test public void eightThousandFiveHundredRecordsOccupyLessThanTwentyMb()throws Exception{
        try(ImageProgressJournal journal=new ImageProgressJournal(context)){
            var db=journal.getWritableDatabase();db.beginTransaction();try{
                for(int i=0;i<8500;i++){
                    String uri="content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FCamera/document/primary%3ADCIM%2FCamera%2FIMG_20261021_"+i+".jpg";
                    journal.record(uri,options().folder,options(),new ImageRunState.Result("IMG_20261021_"+i+".jpg",3000000,1800000,"content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FHEIC/document/primary%3ADCIM%2FHEIC%2FIMG_"+i+".heic","Salvato",ImageRunState.Outcome.SAVED,"NO TECHNICAL DETAILS IN DB","a".repeat(64),"b".repeat(64)));
                }db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }
        long bytes=context.getDatabasePath(ImageProgressJournal.DATABASE).length();assertTrue(bytes>0);assertTrue("Registry size: "+bytes,bytes<20000000L);System.out.println("IMAGE_REGISTRY_8500_BYTES="+bytes);
    }
    @Test public void completeLargeFolderHandoffKeepsAllEntriesAndDraftSurvivesRecreation()throws Exception{
        ImageState state=new ImageState();state.bulk=true;state.selectionLabel="8500 JPEG dalla cartella";state.options=options();ArrayList<String> folders=new ArrayList<>();
        for(int i=0;i<8500;i++){state.inputs.add("content://photos/"+i);state.names.add("Foto "+i);folders.add(options().folder);}
        ImageDraftStore.save(context,state);ImageState restored=ImageDraftStore.load(context);assertEquals(state.inputs,restored.inputs);assertEquals(state.names,restored.names);assertTrue(restored.bulk);
        String id=ImageQueueStore.write(context,state.inputs,state.names,folders,true);try{assertEquals(8500,ImageQueueStore.read(context,id).inputs.size());}finally{ImageQueueStore.discard(context,id);}
    }
    @Test public void reusedOutputsContributeOnceToCurrentBatchSavings(){
        ImageRunState run=new ImageRunState();run.add(new ImageRunState.Result("existing.jpg",3000000,1000000,"content://saved","Già presente",ImageRunState.Outcome.ALREADY_DONE,""));
        run.add(new ImageRunState.Result("new.jpg",3000000,1000000,"content://saved2","Salvata",ImageRunState.Outcome.SAVED,""));
        assertEquals(1,run.saved);assertEquals(1,run.alreadyDone);assertEquals(4000000,run.inputSavedBytes-run.outputSavedBytes);
    }
}
