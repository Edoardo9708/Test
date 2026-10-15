package it.edo.codecprobe;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;

/** Actual Android View rendering with synthetic content for visual review, not codec validation. */
@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class HomeRenderTest {
    private ActivityController<MainActivity> screen(){
        VideoMetadataCache.permissions("false:false");VideoMetadataCache.clear();long epoch=VideoMetadataCache.epoch();
        ArrayList<String> uris=new ArrayList<>(),names=new ArrayList<>();
        for(int n=1;n<=3;n++){
            String uri="content://preview/video/"+n,name="Video_di_prova_"+n+".mp4";uris.add(uri);names.add(name);
            VideoMetadataCache.put(uri,new VideoMetadataCache.Entry(name,"1 traccia video · 1 traccia audio · 148 MB",new InputInspector.Report("HEVC · 1920 × 1080 · 30 fps","Metadati del video di prova")),epoch);
        }
        Bundle state=new Bundle();state.putStringArrayList("inputs",uris);state.putStringArrayList("names",names);state.putString("input",uris.get(0));
        return Robolectric.buildActivity(MainActivity.class).create(state).start().resume().visible();
    }
    private void image(HomeController home,String name)throws Exception{
        View page=home.screen.page;int width=home.ui.dp(360);page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());
        float density=home.activity.getResources().getDisplayMetrics().density;int height=(int)Math.ceil(page.getMeasuredHeight()/density);
        Bitmap bitmap=Bitmap.createBitmap(360,height,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(home.ui.bg);canvas.scale(1/density,1/density);page.draw(canvas);
        File directory=new File("build/reports/ui");assertTrue(directory.isDirectory()||directory.mkdirs());File output=new File(directory,name);
        try(OutputStream out=new FileOutputStream(output)){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
        assertTrue("Empty UI rendering",output.length()>10000);
    }
    @Test public void inputPage()throws Exception{
        ActivityController<MainActivity> controller=screen();try{HomeController home=controller.get().home;home.screen.sections.focusCard("01  FILE E DESTINAZIONE");
            home.destinations.originPanel.close();home.screen.destinations.originRows.removeAllViews();
            HomeDirectoryRow.add(home.ui,home.screen.destinations.originRows,"DCIM/Camera","1, 2",true,false,()->{});
            HomeDirectoryRow.add(home.ui,home.screen.destinations.originRows,"Movies/Screen recordings","3",false,false,()->{});
            home.ui.text(home.screen.destinations.originRows,"Cartelle autorizzate: 1 / 2",14);image(home,"home-input.png");}finally{controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }
    @Test @Config(qualifiers="w360dp-h800dp-night-xhdpi") public void compressionDark()throws Exception{
        ActivityController<MainActivity> controller=screen();try{
            HomeController home=controller.get().home;home.screen.sections.focusCard("02  COMPRESSIONE");home.screen.compression.presetSlider.setProgress(5);home.screen.compression.crfSlider.setProgress(23);
            // The JVM has no Android JNI library. Only these preview values are simulated for the image.
            home.screen.compression.temporalHelp.setVisibility(View.GONE);home.screen.compression.bframeSwitch.setText("B-frame · 4");home.screen.compression.lookaheadSwitch.setText("Lookahead · 20");home.screen.compression.cuTreeLabel.setText("CU-tree · ON");home.screen.compression.aqModeLabel.setText("AQ mode · 2");home.screen.compression.aqStrengthLabel.setText("AQ strength · 1,00");
            image(home,"home-compression-dark.png");
        }finally{controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }
    @Test public void activeConversion()throws Exception{
        ActivityController<MainActivity> controller=screen();try{
            HomeController home=controller.get().home;home.screen.sections.focusCard(MainActivity.CARD_CONVERSION);
            ConversionService.State state=new ConversionService.State();state.running=true;state.onlySmaller=true;state.batchIndex=2;state.batchTotal=3;state.succeeded=1;state.source="Video_di_prova_2.mp4";state.filePercent=19;state.doneFrames=340;state.totalFrames=1800;state.done=11333333;state.total=60000000;state.requestedPreset="medium";state.requestedCrf=23;state.requestedCpu=2;state.bframeCount=4;state.lookaheadCount=20;state.cuTree=1;state.aqMode=2;state.aqStrength=1000;state.message="Codifica software in corso";
            home.conversion.renderConversionDashboard(state);home.screen.conversion.cpuValue.setText("48,2%");home.screen.conversion.ramValue.setText("320 MiB");home.screen.conversion.batteryValue.setText("39,0 °C");
            state.sizeReport="Video 1 · Video_di_prova_1.mp4\n10 MB → 5 MB · −50%";home.conversion.renderSizeReport(state);image(home,"home-conversion.png");
        }finally{controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }
    @Test public void batchResults()throws Exception{
        ActivityController<MainActivity> controller=screen();try{
            HomeController home=controller.get().home;home.screen.sections.focusCard(MainActivity.CARD_CONVERSION);
            android.content.Intent task=new android.content.Intent(home.activity,ConversionService.class).setAction(ConversionService.START)
                .putStringArrayListExtra("inputs",new ArrayList<>(home.state.selectedInputs))
                .putStringArrayListExtra("names",new ArrayList<>(home.state.selectedNames))
                .putStringArrayListExtra("folders",new ArrayList<>(List.of("content://preview/tree/a","content://preview/tree/a","content://preview/tree/a")))
                .putExtra("crf",23).putExtra("preset",5).putExtra("onlySmaller",true);
            ConversionService.rememberFailures(home.activity,BatchRetry.select(task,List.of(1)));
            ConversionService.State state=new ConversionService.State();state.batchIndex=3;state.batchTotal=3;state.succeeded=1;state.failed=1;state.notSaved=1;state.onlySmaller=true;state.filePercent=100;state.source="Video_di_prova_3.mp4";state.requestedCrf=23;state.requestedPreset="medium";state.requestedCpu=1;state.message="Coda terminata · 1 salvato · 1 fallito · 1 senza risparmio";
            state.failures.add(new ConversionFailure(2,"Video_di_prova_2.mp4","Analisi del file",new java.io.IOException("Traccia audio non supportata")));
            state.sizeReport=CompressionSummary.render(List.of(
                new CompressionSummary.Item(1,"Video_di_prova_1.mp4",CompressionSummary.Status.COMPLETE,10000000,5000000),
                new CompressionSummary.Item(2,"Video_di_prova_2.mp4",CompressionSummary.Status.FAILED,-1,-1),
                new CompressionSummary.Item(3,"Video_di_prova_3.mp4",CompressionSummary.Status.NO_SAVING,2000000,2100000)));
            home.conversion.renderConversionDashboard(state);home.conversion.renderSizeReport(state);image(home,"home-results.png");
        }finally{ConversionService.rememberFailures(controller.get(),null);controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }

}
