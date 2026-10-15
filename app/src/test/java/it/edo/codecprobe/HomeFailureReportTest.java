package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.view.*;
import android.widget.*;
import java.io.IOException;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class HomeFailureReportTest {
    private Button button(View view, String text) {
        if(view instanceof Button && ((Button)view).getText().toString().equals(text))return (Button)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button found=button(group.getChildAt(i),text);if(found!=null)return found;}}
        return null;
    }
    @Test public void failedFileOpensItsOwnErrorAndCopiesCollapsedDetailsWithoutClosing() {
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try {
            HomeController home=controller.get().home;
            ConversionFailure first=new ConversionFailure(1,"first.mp4","Analisi",new IOException("Prima causa"));
            ConversionFailure second=new ConversionFailure(2,"second.mp4","Verifica video",new IOException("Seconda causa",new IllegalStateException("Causa interna")));
            ConversionService.State state=new ConversionService.State();state.failures.add(first);state.failures.add(second);
            state.sizeReport=CompressionSummary.render(List.of(
                new CompressionSummary.Item(1,"first.mp4",CompressionSummary.Status.FAILED,-1,-1),
                new CompressionSummary.Item(2,"second.mp4",CompressionSummary.Status.FAILED,-1,-1),
                new CompressionSummary.Item(3,"saved.mp4",CompressionSummary.Status.COMPLETE,100,50)));
            home.conversion.renderSizeReport(state);
            LinearLayout report=home.screen.conversion.failureReport;
            assertEquals(View.VISIBLE,report.getVisibility());
            assertEquals(View.GONE,home.screen.conversion.sizeReport.getVisibility());
            LinearLayout row=(LinearLayout)report.getChildAt(1);
            Button error=(Button)row.getChildAt(1);assertTrue(error.getContentDescription().toString().contains("second.mp4"));error.performClick();
            AlertDialog dialog=home.conversion.failures.dialog;assertTrue(dialog.isShowing());
            Button toggle=button(dialog.getWindow().getDecorView(),"Mostra dettagli tecnici ▾");assertNotNull(toggle);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            ClipboardManager clipboard=home.activity.getSystemService(ClipboardManager.class);
            String copied=clipboard.getPrimaryClip().getItemAt(0).getText().toString();
            assertEquals(second.copyText(),copied);assertTrue(copied.contains("Causa interna"));assertFalse(copied.contains("Prima causa"));assertTrue(dialog.isShowing());
            toggle.performClick();assertEquals("Nascondi dettagli tecnici ▴",toggle.getText().toString());
            // A dashboard refresh must not rebuild unchanged rows or close the popup.
            home.conversion.renderSizeReport(state);assertSame(row,report.getChildAt(1));assertTrue(dialog.isShowing());
            ConversionService.State snapshot=new ConversionService.State(state);state.failures.clear();assertEquals(2,snapshot.failures.size());
            home.conversion.renderSizeReport(state);assertEquals(View.GONE,report.getVisibility());
        } finally {controller.pause().stop().destroy();}
    }
    @Test public void absentExceptionMessageStillHasUsefulText(){
        ConversionFailure failure=new ConversionFailure(1,"clip.mp4","Analisi",new IOException());
        assertEquals("IOException",failure.message);assertTrue(failure.copyText().contains("DETTAGLI TECNICI"));
    }
    private void renderError(String filename)throws Exception {
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try {
            HomeController home=controller.get().home;
            home.conversion.failures.show(new ConversionFailure(2,"Video_di_prova_2.mp4","Verifica della traccia audio",new IOException("Traccia audio non supportata: formato non riconosciuto.")));
            View view=home.conversion.failures.dialog.getWindow().getDecorView();
            view.measure(View.MeasureSpec.makeMeasureSpec(home.ui.dp(340),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(home.ui.dp(760),View.MeasureSpec.AT_MOST));
            view.layout(0,0,view.getMeasuredWidth(),view.getMeasuredHeight());
            float density=home.activity.getResources().getDisplayMetrics().density;
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(340,(int)Math.ceil(view.getHeight()/density),android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);canvas.drawColor(home.ui.bg);canvas.scale(1/density,1/density);view.draw(canvas);
            java.io.File directory=new java.io.File("build/reports/ui");directory.mkdirs();
            try(java.io.OutputStream out=new java.io.FileOutputStream(new java.io.File(directory,filename))){assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void renderLightError()throws Exception{renderError("home-error.png");}
    @Test @Config(qualifiers="w360dp-h800dp-night-xhdpi") public void renderDarkError()throws Exception{renderError("home-error-dark.png");}
}
