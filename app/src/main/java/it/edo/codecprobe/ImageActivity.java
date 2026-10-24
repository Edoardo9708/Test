package it.edo.codecprobe;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import android.content.Intent;
import android.os.Bundle;

/** Images entry point; lifecycle only. */
public final class ImageActivity extends ComponentActivity {
    ImageController images;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);images=new ImageController(this,saved);images.compose=new PhotoComposeHost(images);getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){public void handleOnBackPressed(){images.screen.layout.back();}});}
    @Override protected void onActivityResult(int request,int code,Intent data){super.onActivityResult(request,code,data);images.selection.result(request,code,data);}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);images.save(out);}
    @Override protected void onResume(){super.onResume();images.resume();}
    @Override protected void onPause(){images.pause();images.persist();super.onPause();}
    @Override protected void onDestroy(){images.destroy();super.onDestroy();}
}
