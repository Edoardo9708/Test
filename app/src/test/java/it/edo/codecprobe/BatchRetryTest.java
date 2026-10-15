package it.edo.codecprobe;

import android.content.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class BatchRetryTest {
    static Intent queue(Context context){
        Intent task=new Intent(context,ConversionService.class).setAction(ConversionService.START);
        task.putStringArrayListExtra("inputs",new ArrayList<>(List.of("content://input/0","content://input/1","content://input/2")));
        task.putStringArrayListExtra("names",new ArrayList<>(List.of("zero.mp4","one.mp4","two.mp4")));
        task.putStringArrayListExtra("folders",new ArrayList<>(List.of("content://dirs/tree/zero","content://dirs/tree/one","content://dirs/tree/two")));
        task.putStringArrayListExtra("outputNames",new ArrayList<>(List.of("custom-zero.mp4","custom-one.mp4","custom-two.mp4")));
        task.putExtra("individualOrigins",new boolean[]{true,false,true});task.putExtra("omitted",new boolean[]{false,false,false});
        task.putExtra("crf",17).putExtra("preset",8).putExtra("cpu",2).putExtra("bframes",false).putExtra("lookahead",true).putExtra("onlySmaller",true);
        return task;
    }
    @Test public void selectedFailuresKeepTheMatchingDestinationsApprovedNamesAndSettings(){
        Context context=RuntimeEnvironment.getApplication();Intent original=queue(context);
        Intent retry=BatchRetry.select(original,List.of(2,0));
        assertEquals(List.of("content://input/2","content://input/0"),retry.getStringArrayListExtra("inputs"));
        assertEquals(List.of("content://dirs/tree/two","content://dirs/tree/zero"),retry.getStringArrayListExtra("folders"));
        assertEquals(List.of("custom-two.mp4","custom-zero.mp4"),retry.getStringArrayListExtra("requestedOutputNames"));
        assertArrayEquals(new boolean[]{true,true},retry.getBooleanArrayExtra("individualOrigins"));
        assertFalse(retry.hasExtra("omitted"));assertFalse(retry.hasExtra("outputNames"));
        assertEquals(17,retry.getIntExtra("crf",0));assertEquals(8,retry.getIntExtra("preset",0));assertTrue(retry.getBooleanExtra("onlySmaller",false));
        original.getStringArrayListExtra("inputs").set(2,"changed");assertEquals("content://input/2",retry.getStringArrayListExtra("inputs").get(0));
        assertNull(BatchRetry.select(original,List.of()));
        try{BatchRetry.select(original,List.of(1,1));fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void privateStoreSurvivesReloadAndRejectsCorruptData(){
        Context context=RuntimeEnvironment.getApplication();Intent expected=BatchRetry.select(queue(context),List.of(1));
        assertTrue(BatchRetry.save(context,expected));Intent restored=BatchRetry.load(context);assertNotNull(restored);
        assertEquals(expected.getStringArrayListExtra("inputs"),restored.getStringArrayListExtra("inputs"));
        assertEquals(expected.getStringArrayListExtra("folders"),restored.getStringArrayListExtra("folders"));
        assertEquals(List.of("custom-one.mp4"),restored.getStringArrayListExtra("requestedOutputNames"));
        assertEquals(17,restored.getIntExtra("crf",0));assertEquals(8,restored.getIntExtra("preset",0));assertEquals(2,restored.getIntExtra("cpu",0));
        assertFalse(restored.getBooleanExtra("bframes",true));assertTrue(restored.getBooleanExtra("lookahead",false));assertTrue(restored.getBooleanExtra("onlySmaller",false));
        assertTrue(BatchRetry.save(context,null));assertNull(BatchRetry.load(context));
        context.getSharedPreferences("failed-conversion-retry",0).edit().putString("request","invalid json").commit();assertNull(BatchRetry.load(context));
    }
}
