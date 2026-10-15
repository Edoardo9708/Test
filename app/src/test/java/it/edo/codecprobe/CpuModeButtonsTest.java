package it.edo.codecprobe;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class CpuModeButtonsTest {
    private CpuModeButtons selector(){return new CpuModeButtons(RuntimeEnvironment.getApplication(),Color.BLUE,Color.WHITE,Color.WHITE,Color.GRAY);}
    @Test public void directChoiceSelectsExactlyOneButtonAndKeepsIndexes(){
        CpuModeButtons selector=selector();selector.setSelection(1);
        ((Button)selector.getChildAt(2)).performClick();assertEquals(2,selector.getSelectedItemPosition());
        for(int i=0;i<3;i++)assertEquals(i==2,selector.getChildAt(i).isSelected());
        selector.setSelection(0);assertEquals(0,selector.getSelectedItemPosition());
        assertEquals("Limited",((Button)selector.getChildAt(0)).getText().toString());
    }
    @Test public void disablingDuringConversionBlocksAllThreeChoices(){
        CpuModeButtons selector=selector();selector.setSelection(1);selector.setEnabled(false);
        for(int i=0;i<3;i++){assertFalse(selector.getChildAt(i).isEnabled());selector.getChildAt(i).performClick();}
        assertEquals(1,selector.getSelectedItemPosition());
        selector.setEnabled(true);selector.getChildAt(2).performClick();assertEquals(2,selector.getSelectedItemPosition());
    }
    private Button findButton(View view,String title){
        if(view instanceof Button&&title.contentEquals(((Button)view).getText()))return (Button)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int n=0;n<group.getChildCount();n++){Button found=findButton(group.getChildAt(n),title);if(found!=null)return found;}}
        return null;
    }
    @Test public void settingsReadExistingCpuValueAndSaveNewDirectChoice()throws Exception{
        AppSettings.save(RuntimeEnvironment.getApplication(),23,6,2,true,true,"",true);
        ActivityController<SettingsActivity> controller=Robolectric.buildActivity(SettingsActivity.class).setup();SettingsActivity activity=controller.get();
        try{
            Field f=SettingsActivity.class.getDeclaredField("cpu");f.setAccessible(true);CpuModeButtons selector=(CpuModeButtons)f.get(activity);
            assertEquals(2,selector.getSelectedItemPosition());selector.getChildAt(0).performClick();
            Button save=findButton(activity.getWindow().getDecorView(),"Salva impostazioni");assertNotNull(save);save.performClick();
            assertEquals(0,new AppSettings(activity).cpu);
        }finally{controller.pause().stop().destroy();}
    }
}
