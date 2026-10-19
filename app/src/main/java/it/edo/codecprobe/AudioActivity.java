package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Independent audio workspace. MainActivity remains the video and notification entry point. */
public final class AudioActivity extends Activity {
    AudioController audio;
    @Override public void onCreate(Bundle state){super.onCreate(state);audio=new AudioController(this,state);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);audio.selection.result(request,result,data);audio.actions.result(request,result,data);}
    @Override public void onBackPressed(){super.onBackPressed();AppNavigation.fade(this);}
    @Override protected void onResume(){super.onResume();audio.resume();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);audio.save();audio.screen.sections.save(state);}
    @Override protected void onPause(){audio.pause();super.onPause();}
    @Override protected void onDestroy(){audio.destroy();super.onDestroy();}
}
