package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Image defaults have an independent page and explicit save/back behavior. */
public final class ImageSettingsActivity extends Activity {
    ImageSettingsScreen screen;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);screen=new ImageSettingsScreen(this,saved);}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);screen.saveState(out);}
    @Override protected void onActivityResult(int request,int code,Intent data){super.onActivityResult(request,code,data);screen.result(request,code,data);}
}
