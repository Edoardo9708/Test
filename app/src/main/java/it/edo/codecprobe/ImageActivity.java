package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Images entry point; lifecycle only. */
public final class ImageActivity extends Activity {
    ImageController images;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);images=new ImageController(this,saved);}
    @Override protected void onActivityResult(int request,int code,Intent data){super.onActivityResult(request,code,data);images.selection.result(request,code,data);}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);images.save(out);}
    @Override protected void onPause(){images.persist();super.onPause();}
    @Override protected void onDestroy(){images.destroy();super.onDestroy();}
    @Override public void onBackPressed(){super.onBackPressed();AppNavigation.fade(this);}
}
