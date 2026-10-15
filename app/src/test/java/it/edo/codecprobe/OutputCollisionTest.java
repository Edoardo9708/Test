package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.widget.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class OutputCollisionTest {
    public static class Directory extends ContentProvider {
        public boolean onCreate(){return true;}
        public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
            MatrixCursor cursor=new MatrixCursor(new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME});
            cursor.addRow(new Object[]{"clip.mp4"});return cursor;
        }
        public String getType(Uri uri){return null;}public Uri insert(Uri uri,ContentValues values){return null;}
        public int update(Uri uri,ContentValues values,String s,String[] a){return 0;}public int delete(Uri uri,String s,String[] a){return 0;}
    }
    private ActivityController<MainActivity> controller;
    private HomeController home;
    @Before public void setup(){
        ShadowContentResolver.registerProviderInternal("test.documents",new Directory());
        controller=Robolectric.buildActivity(MainActivity.class).setup();home=controller.get().home;
    }
    @After public void teardown(){controller.pause().stop().destroy();}
    private void begin() throws Exception {
        // Read the real default naming configuration rather than relying on a preference key.
        String source="clip.mp4";AppSettings settings=new AppSettings(home.activity);
        String expected=OutputName.base(source,settings.addition,settings.prefix);
        ContentProvider provider=new Directory(){public Cursor query(Uri u,String[] p,String s,String[] a,String o){
            MatrixCursor cursor=new MatrixCursor(new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME});cursor.addRow(new Object[]{expected});return cursor;}};
        ShadowContentResolver.registerProviderInternal("test.documents",provider);
        Intent task=new Intent(home.activity,ConversionService.class).setAction(ConversionService.START)
            .putStringArrayListExtra("names",new ArrayList<>(List.of(source)))
            .putStringArrayListExtra("inputs",new ArrayList<>(List.of("content://input/1")))
            .putStringArrayListExtra("folders",new ArrayList<>(List.of("content://test.documents/tree/primary%3AMovies")));
        home.conversion.outputCheck.begin(task);
        home.conversion.outputCheck.worker.submit(()->{}).get(5,TimeUnit.SECONDS);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertNotNull(home.conversion.outputCheck.dialog);
    }
    private EditText input(android.view.View view){
        if(view instanceof EditText)return (EditText)view;
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int n=0;n<group.getChildCount();n++){EditText found=input(group.getChildAt(n));if(found!=null)return found;}}
        return null;
    }
    @Test public void renameValidatesNameBeforeStartingService() throws Exception {
        begin();AlertDialog dialog=home.conversion.outputCheck.dialog;
        EditText name=input(dialog.getWindow().getDecorView());assertNotNull(name);
        AppSettings settings=new AppSettings(home.activity);name.setText(OutputName.base("clip.mp4",settings.addition,settings.prefix));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertNotNull(name.getError());assertTrue(dialog.isShowing());
        name.setText("rinominato");dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Intent started=Shadows.shadowOf((android.app.Application)home.activity.getApplication()).getNextStartedService();
        assertNotNull(started);assertEquals("rinominato.mp4",started.getStringArrayListExtra("outputNames").get(0));
        assertFalse(started.getBooleanArrayExtra("omitted")[0]);assertFalse(home.conversion.outputCheck.checking);
    }
    @Test public void skipRecordsDecisionAndCancelDoesNotLaunch() throws Exception {
        begin();home.conversion.outputCheck.dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        Intent started=Shadows.shadowOf((android.app.Application)home.activity.getApplication()).getNextStartedService();
        assertTrue(started.getBooleanArrayExtra("omitted")[0]);
    }
    @Test public void backCancelsPreflightWithoutConversion() throws Exception {
        begin();home.conversion.outputCheck.dialog.cancel();assertFalse(home.conversion.outputCheck.checking);
        assertNull(Shadows.shadowOf((android.app.Application)home.activity.getApplication()).getNextStartedService());
        assertFalse(home.state.busy);
    }
    @Test public void sharedButtonsHaveNoPlatformElevationAnimator(){
        assertNull(home.screen.conversion.convert.getStateListAnimator());assertEquals(0f,home.screen.conversion.convert.getElevation(),0f);
    }
    private void renderDialog(String filename)throws Exception{
        begin();android.view.View view=home.conversion.outputCheck.dialog.getWindow().getDecorView();
        int width=home.ui.dp(340);view.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(home.ui.dp(760),android.view.View.MeasureSpec.AT_MOST));
        view.layout(0,0,view.getMeasuredWidth(),view.getMeasuredHeight());
        float density=home.activity.getResources().getDisplayMetrics().density;
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(340,(int)Math.ceil(view.getHeight()/density),android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);canvas.drawColor(home.ui.bg);canvas.scale(1/density,1/density);view.draw(canvas);
        java.io.File directory=new java.io.File("build/reports/ui");directory.mkdirs();
        try(java.io.OutputStream out=new java.io.FileOutputStream(new java.io.File(directory,filename))){assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
    }
    @Test public void renderLightCollisionDialog()throws Exception{renderDialog("home-collision.png");}
    @Test @Config(qualifiers="w360dp-h800dp-night-xhdpi") public void renderDarkCollisionDialog()throws Exception{renderDialog("home-collision-dark.png");}

}
