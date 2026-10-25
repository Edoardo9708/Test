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
    @Test public void copyingOptionInvalidatesSkippedResultAndCopiedFileMustStillMatch()throws Exception{
        File original=File.createTempFile("journal-original-",".jpg"),output=File.createTempFile("journal-copy-",".jpg");
        try(ImageProgressJournal journal=new ImageProgressJournal(context)){
            Files.write(original.toPath(),new byte[]{1,2,3,4});Files.copy(original.toPath(),output.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String input=Uri.fromFile(original).toString(),sha=ImageFingerprint.file(original,()->false);
            ImageOptions copy=new ImageOptions(65,"slow","standard",options().folder,true,false,true,0);
            journal.record(input,options().folder,options(),new ImageRunState.Result("foto.jpg",4,8,null,"Skip",ImageRunState.Outcome.NOT_SMALLER,"",sha,""));
            assertNull(journal.reusable(context,input,"foto.jpg",copy,()->false));
            journal.record(input,copy.folder,copy,new ImageRunState.Result("foto.jpg",4,4,Uri.fromFile(output).toString(),"Copied",ImageRunState.Outcome.COPIED_ORIGINAL,"",sha,sha));
            assertEquals("Originale copiato già verificato",journal.reusable(context,input,"foto.jpg",copy,()->false).message);
            Files.write(output.toPath(),new byte[]{9,2,3,4});assertNull(journal.reusable(context,input,"foto.jpg",copy,()->false));
        }finally{original.delete();output.delete();}
    }
    @Test public void savedHeicSurvivesFallbackToggleAndLegacyKeys()throws Exception{
        File original=File.createTempFile("journal-original-",".jpg"),output=File.createTempFile("journal-output-",".heic");
        try(ImageProgressJournal journal=new ImageProgressJournal(context)){
            Files.write(original.toPath(),new byte[]{1,2,3,4});Files.write(output.toPath(),new byte[]{5,6});
            String input=Uri.fromFile(original).toString();
            ImageOptions copy=new ImageOptions(65,"slow","standard",options().folder,true,false,true,0);
            journal.record(input,options().folder,options(),saved(original,output));
            assertEquals(ImageRunState.Outcome.ALREADY_DONE,journal.reusable(context,input,"foto.jpg",copy,()->false).outcome);
            // v1.06 and v1.07 wrote this suffix when the checkbox was checked.
            android.content.ContentValues legacy=new android.content.ContentValues();legacy.put("settings",ImageProgressJournal.settings(options())+"|copy-original");
            journal.getWritableDatabase().update("progress",legacy,"uri=?",new String[]{input});
            assertEquals(ImageRunState.Outcome.ALREADY_DONE,journal.reusable(context,input,"foto.jpg",copy,()->false).outcome);
            assertEquals(ImageRunState.Outcome.ALREADY_DONE,journal.reusable(context,input,"foto.jpg",options(),()->false).outcome);
            ImageOptions otherQuality=new ImageOptions(70,"slow","standard",copy.folder,true,false,true,0);
            assertNull(journal.reusable(context,input,"foto.jpg",otherQuality,()->false));
            assertNull(journal.reusable(context,input,"foto.jpg",copy.withFolder("content://another"),()->false));
            Files.write(output.toPath(),new byte[]{9,6});
            assertNull(journal.reusable(context,input,"foto.jpg",copy,()->false));
        }finally{original.delete();output.delete();}
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
    @Test public void inspectionFailuresRemainRetryableWithoutErasingConversionHistory(){
        try(ImageProgressJournal journal=new ImageProgressJournal(context)){
            String uri="content://photo/check";
            ImageRunState.Result failure=new ImageRunState.Result("check.jpg",0,0,null,"Metadata",ImageRunState.Outcome.FAILED,"");
            journal.inspection(uri,failure);assertEquals(java.util.List.of(uri),journal.failures().inputs);
            assertEquals("",journal.failures().folders.get(0));
            journal.record(uri,options().folder,options(),failure);assertEquals(1,journal.failures().inputs.size());assertEquals(options().folder,journal.failures().folders.get(0));
            journal.inspection(uri,new ImageRunState.Result("check.jpg",0,0,null,"OK",ImageRunState.Outcome.CHECKED,""));
            assertEquals(1,journal.failures().inputs.size()); // A metadata pass must not hide a conversion failure.
            journal.record(uri,options().folder,options(),new ImageRunState.Result("check.jpg",100,50,"content://output", "OK",ImageRunState.Outcome.SAVED,""));
            assertTrue(journal.failures().inputs.isEmpty());
            journal.inspection(uri,failure);assertEquals(1,journal.failures().inputs.size());
            journal.inspection(uri,new ImageRunState.Result("check.jpg",0,0,null,"Interrupted",ImageRunState.Outcome.CANCELLED,""));assertEquals(1,journal.failures().inputs.size());
            journal.inspection(uri,new ImageRunState.Result("check.jpg",0,0,null,"OK",ImageRunState.Outcome.CHECKED,""));assertTrue(journal.failures().inputs.isEmpty());
            assertNotNull(journal.getReadableDatabase());
        }
    }
    @Test public void upgradesVersionOneWithoutLosingExistingRecords(){
        var db=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(ImageProgressJournal.DATABASE),null);
        db.execSQL("CREATE TABLE progress (uri TEXT PRIMARY KEY, name TEXT NOT NULL, folder TEXT NOT NULL, settings TEXT NOT NULL, status TEXT NOT NULL, source_sha TEXT NOT NULL, output_uri TEXT NOT NULL, output_sha TEXT NOT NULL, input_bytes INTEGER NOT NULL, output_bytes INTEGER NOT NULL, updated INTEGER NOT NULL)");
        db.execSQL("INSERT INTO progress VALUES ('content://old','old.jpg','content://dest','old','FAILED','','','',0,0,1)");db.setVersion(1);db.close();
        try(ImageProgressJournal journal=new ImageProgressJournal(context)){assertEquals(java.util.List.of("content://old"),journal.failures().inputs);assertEquals(2,journal.getReadableDatabase().getVersion());}
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
