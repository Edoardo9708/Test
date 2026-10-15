package it.edo.codecprobe;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.lang.reflect.*;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class CollapsibleCardsTest {
    @SuppressWarnings("unchecked") private Map<String,LinearLayout> bodies(MainActivity a)throws Exception{return (Map<String,LinearLayout>)a.home.screen.sections.bodies;}
    @SuppressWarnings("unchecked") private Map<String,Button> headers(MainActivity a)throws Exception{return (Map<String,Button>)a.home.screen.sections.headers;}
    @Test public void onlyFirstCardOpensAndCollapseKeepsValues()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try{
            Map<String,LinearLayout> cards=bodies(activity);assertEquals(4,cards.size());int n=0;
            for(LinearLayout body:cards.values())assertEquals(n++==0?View.VISIBLE:View.GONE,body.getVisibility());
            SeekBar crf=(SeekBar)activity.home.screen.compression.crfSlider;crf.setProgress(28);
            headers(activity).get("02  COMPRESSIONE").performClick();assertEquals(View.VISIBLE,cards.get("02  COMPRESSIONE").getVisibility());
            headers(activity).get("02  COMPRESSIONE").performClick();assertEquals(View.GONE,cards.get("02  COMPRESSIONE").getVisibility());
            assertSame(crf,activity.home.screen.compression.crfSlider);assertEquals(28,crf.getProgress());
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void focusConversionAndRecreationKeepExpansion()throws Exception{
        ActivityController<MainActivity> first=Robolectric.buildActivity(MainActivity.class).setup();Bundle saved=new Bundle();
        try{
            first.get().home.screen.sections.focusCard(MainActivity.CARD_CONVERSION);
            for(Map.Entry<String,LinearLayout> entry:bodies(first.get()).entrySet())assertEquals(entry.getKey().equals(MainActivity.CARD_CONVERSION)?View.VISIBLE:View.GONE,entry.getValue().getVisibility());
            first.saveInstanceState(saved);
        }finally{first.pause().stop().destroy();}
        ActivityController<MainActivity> restored=Robolectric.buildActivity(MainActivity.class).create(saved).start().resume().visible();
        try{
            for(Map.Entry<String,LinearLayout> entry:bodies(restored.get()).entrySet())assertEquals(entry.getKey().equals(MainActivity.CARD_CONVERSION)?View.VISIBLE:View.GONE,entry.getValue().getVisibility());
            headers(restored.get()).get("01  FILE E DESTINAZIONE").performClick();
            assertEquals(View.VISIBLE,bodies(restored.get()).get(MainActivity.CARD_CONVERSION).getVisibility());
            assertEquals(View.VISIBLE,bodies(restored.get()).get("01  FILE E DESTINAZIONE").getVisibility());
        }finally{restored.pause().stop().destroy();}
    }
}

