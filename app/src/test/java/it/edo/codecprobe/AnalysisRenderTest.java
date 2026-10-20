package it.edo.codecprobe;

import android.app.Activity;
import android.graphics.*;
import android.view.*;
import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AnalysisRenderTest {
    private void render(boolean hex,String name)throws Exception{
        var host=Robolectric.buildActivity(Activity.class).setup();try{
            AnalysisScreen screen=new AnalysisScreen(new AppUi(host.get()));AnalysisSources.Entry source=new AnalysisSources.Entry("content://preview/photo","Foto originale S23.jpg","Immagini","4000 × 3000 pixel");
            if(name.contains("files"))screen.sources(Arrays.asList(source,new AnalysisSources.Entry("content://preview/video","Video vacanza.mp4","Video","148 MB")),e->{});
            else{
                AnalysisNode root=new AnalysisNode(source.name,"JPEG · segmenti e dati compressi",0,4714303,"jpeg",0);
                screen.tree(source,Arrays.asList(root),Arrays.asList(AnalysisParser.leaf("SOI · inizio JPEG","",0,2),new AnalysisNode("APP1 · EXIF","Metadati della foto e miniatura JPEG",2,33267,"exif",12),AnalysisParser.leaf("Scansione · immagine compressa","Dati compressi",33269,4681034)),60,i->{},n->{});
                if(hex){screen.showDetails(new AnalysisNode("APP1 · EXIF","Metadati della foto e miniatura JPEG",2,33267,"exif",12));screen.hexPanel.setVisibility(View.VISIBLE);screen.hexText.setText("000000000002  FF E1 81 F1 45 78 69 66  .. ..Exif\n00000000000A  00 00 49 49 2A 00 08 00  ..II*...");screen.pageLabel.setText("Byte 0–255 del blocco (33.267 totali)");}
            }
            ViewGroup content=host.get().findViewById(android.R.id.content);View page=((ViewGroup)((ViewGroup)content.getChildAt(0)).getChildAt(0)).getChildAt(0);int width=screen.ui.dp(360);page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());
            float density=host.get().getResources().getDisplayMetrics().density;Bitmap bitmap=Bitmap.createBitmap(360,(int)Math.ceil(page.getMeasuredHeight()/density),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(screen.ui.bg);canvas.scale(1/density,1/density);page.draw(canvas);File dir=new File("build/reports/ui");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,name))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
        }finally{host.pause().stop().destroy();}
    }
    @Test public void files()throws Exception{render(false,"analysis-files.png");}
    @Test public void blocks()throws Exception{render(false,"analysis-blocks.png");}
    @Test @Config(qualifiers="w360dp-h800dp-night-xhdpi") public void hexDark()throws Exception{render(true,"analysis-hex-dark.png");}
}
