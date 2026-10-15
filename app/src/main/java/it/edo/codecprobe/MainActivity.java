package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
* Android entry point. UI, state and actions live in the Home modules.
*/
public final class MainActivity extends Activity {
    static final String CARD_CONVERSION = "03  CONVERSIONE";
    static final String CONFIRM_CANCEL = "it.edo.codecprobe.CONFIRM_CANCEL";
    static final String CANCEL_SESSION = "cancelSessionStartedAt";
    HomeController home;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        home = new HomeController(this, state);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        home.conversion.handleCancellationIntent(intent);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        home.onActivityResult(request, result, data);
    }

    @Override
    public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        home.onPermissions(request, permissions, results);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        HomeSavedState.save(home, state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        home.resume();
    }

    @Override
    protected void onPause() {
        home.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        home.destroy();
        super.onDestroy();
    }
}
