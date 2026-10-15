package it.edo.codecprobe;

import android.view.*;
import android.widget.*;
import android.content.res.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class HomeLayoutTest {
    private void checkButtons(View view,int minimum){
        if(view.getVisibility()!=View.VISIBLE)return;
        if(view instanceof Button){Button b=(Button)view;assertTrue(b.getText().toString(),b.getMeasuredHeight()>=minimum);
            if(b.getLayout()!=null)assertTrue("Clipped button: "+b.getText(),b.getLayout().getHeight()<=b.getMeasuredHeight()-b.getCompoundPaddingTop()-b.getCompoundPaddingBottom());
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)checkButtons(((ViewGroup)view).getChildAt(i),minimum);
    }
    private void measure(HomeController home,int width){View page=home.screen.page;page.measure(View.MeasureSpec.makeMeasureSpec(home.ui.dp(width),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());}
    @Test public void narrowLayoutWithLargeFontKeepsButtonLabelsAndTouchTargets(){
        android.app.Application app=org.robolectric.RuntimeEnvironment.getApplication();
        Configuration config=new Configuration(app.getResources().getConfiguration());config.fontScale=1.3f;
        app.getResources().updateConfiguration(config,app.getResources().getDisplayMetrics());
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            MainActivity activity=controller.get();
            HomeController home=activity.home;for(String title:home.screen.sections.bodies.keySet())home.screen.sections.setCardExpanded(title,true);
            assertTrue("Large font configuration not applied",home.screen.conversion.convert.getTextSize()/activity.getResources().getDisplayMetrics().density>=19);
            HomeDirectoryRow.add(home.ui,home.screen.sections.bodies.get("01  FILE E DESTINAZIONE"),"DCIM/Una cartella con un nome molto lungo","1, 2",false,false,()->{});
            measure(home,320);checkButtons(home.screen.page,home.ui.dp(48));
            assertSame(home.screen.conversion.convert.getParent(),home.screen.conversion.stop.getParent());
        }finally{controller.pause().stop().destroy();}
    }
    @Test @Config(qualifiers="w1000dp-h600dp-night-xhdpi") public void wideDarkLayoutLimitsContentWidthAndUsesDarkSystemIconsPolicy(){
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{HomeController home=controller.get().home;measure(home,1000);assertTrue(home.ui.dark);assertEquals(home.ui.dp(760),home.screen.page.getMeasuredWidth());assertTrue(android.graphics.Color.red(home.ui.bg)<40);
            int appearance=controller.get().getWindow().getInsetsController().getSystemBarsAppearance();assertEquals(0,appearance&WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void qualityShortcutsAndManualSliderKeepOneConsistentSelection(){
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{HomeController home=controller.get().home;home.screen.compression.crfButtons.get(0).performClick();assertEquals(18,home.compression.selectedCrf());assertTrue(home.screen.compression.crfButtons.get(0).isSelected());
            home.screen.compression.crfSlider.setProgress(19);for(Button b:home.screen.compression.crfButtons)assertFalse(b.isSelected());
            home.conversion.setBusy(true);for(Button b:home.screen.compression.crfButtons)assertFalse(b.isEnabled());
            home.conversion.setBusy(false);for(Button b:home.screen.compression.crfButtons)assertTrue(b.isEnabled());
        }finally{controller.pause().stop().destroy();}
    }
}
