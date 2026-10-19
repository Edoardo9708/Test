package it.edo.codecprobe;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.AlphaAnimation;
import android.widget.ScrollView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class WorkspaceNavigationTest {
    @Test public void navigationIsTheFirstPageChildAndScrollsAwayWithContent(){
        var video=Robolectric.buildActivity(MainActivity.class).setup();var audio=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            assertSame(video.get().home.screen.navigation,video.get().home.screen.page.getChildAt(0));assertSame(audio.get().audio.screen.navigation,audio.get().audio.screen.page.getChildAt(0));
            assertTrue(video.get().home.screen.navigation.video.isSelected());assertTrue(audio.get().audio.screen.navigation.audio.isSelected());
            ViewGroup root=audio.get().findViewById(android.R.id.content);assertTrue(root.getChildAt(0) instanceof ScrollView);ScrollView scroll=(ScrollView)root.getChildAt(0);
            audio.get().audio.screen.sections.focusCard(AudioScreen.CONVERSION);
            scroll.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));scroll.layout(0,0,720,900);scroll.scrollTo(0,500);
            int[] tab=new int[2],viewport=new int[2];audio.get().audio.screen.navigation.getLocationOnScreen(tab);scroll.getLocationOnScreen(viewport);assertTrue("Tabs remained fixed at the top",tab[1]+audio.get().audio.screen.navigation.getHeight()<=viewport[1]);
        }finally{audio.pause().stop().destroy();video.pause().stop().destroy();}
    }
    @Test public void workspaceSwitchUsesFadeAndSelectedTabDoesNotLaunchAnotherScreen(){
        var video=Robolectric.buildActivity(MainActivity.class).setup();var audio=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            video.get().home.screen.navigation.video.performClick();assertNull(Shadows.shadowOf(video.get()).getNextStartedActivity());
            video.get().home.screen.navigation.audio.performClick();Intent next=Shadows.shadowOf(video.get()).getNextStartedActivity();assertEquals(AudioActivity.class.getName(),next.getComponent().getClassName());
            assertEquals(R.anim.workspace_fade_in,Shadows.shadowOf(video.get()).getPendingTransitionEnterAnimationResourceId());assertEquals(R.anim.workspace_fade_out,Shadows.shadowOf(video.get()).getPendingTransitionExitAnimationResourceId());
            audio.get().audio.screen.navigation.audio.performClick();assertNull(Shadows.shadowOf(audio.get()).getNextStartedActivity());audio.get().audio.screen.navigation.video.performClick();assertTrue(audio.get().isFinishing());assertEquals(MainActivity.class.getName(),Shadows.shadowOf(audio.get()).getNextStartedActivity().getComponent().getClassName());
            assertEquals(R.anim.workspace_fade_in,Shadows.shadowOf(audio.get()).getPendingTransitionEnterAnimationResourceId());
            assertTrue(AnimationUtils.loadAnimation(video.get(),R.anim.workspace_fade_in) instanceof AlphaAnimation);assertEquals(140,AnimationUtils.loadAnimation(video.get(),R.anim.workspace_fade_out).getDuration());
        }finally{audio.pause().stop().destroy();video.pause().stop().destroy();}
    }
    @Test public void backFromAudioAlsoUsesTheWorkspaceFade(){var audio=Robolectric.buildActivity(AudioActivity.class).setup();try{audio.get().onBackPressed();assertTrue(audio.get().isFinishing());assertEquals(R.anim.workspace_fade_out,Shadows.shadowOf(audio.get()).getPendingTransitionExitAnimationResourceId());}finally{audio.pause().stop().destroy();}}
}
