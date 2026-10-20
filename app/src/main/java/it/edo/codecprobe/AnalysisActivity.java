package it.edo.codecprobe;

import android.app.Activity;
import android.os.Bundle;

/** Analysis entry point: lifecycle only. */
public final class AnalysisActivity extends Activity {
    AnalysisController analysis;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);analysis=new AnalysisController(this,saved);}
    @Override protected void onResume(){super.onResume();analysis.resume();}
    @Override protected void onPause(){analysis.pause();super.onPause();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);analysis.save(out);}
    @Override protected void onDestroy(){analysis.destroy();super.onDestroy();}
    @Override public void onBackPressed(){if(!analysis.back()){super.onBackPressed();AppNavigation.fade(this);}}
}
