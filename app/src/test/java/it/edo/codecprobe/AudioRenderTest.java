package it.edo.codecprobe;

import android.graphics.*;
import android.view.View;
import java.io.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Actual Android view renders with synthetic audio data; no native codec is invoked. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AudioRenderTest {
    private void render(String card,String name)throws Exception{
        var context=RuntimeEnvironment.getApplication();context.getSharedPreferences("audio-workspace",0).edit().clear().commit();
        var field=AudioConversionService.class.getDeclaredField("state");field.setAccessible(true);Object previous=field.get(null);field.set(null,new AudioRunState());
        var activity=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=activity.get().audio;audio.state.inputs.add("content://preview/music/1");audio.state.names.add("Artista - Un brano dal nome molto lungo.flac");audio.state.inputs.add("content://preview/music/2");audio.state.names.add("Secondo brano.flac");audio.state.folder="content://dirs/tree/primary%3AMusic%2FOpus";audio.render();
            audio.screen.inputInfo.setText("32.50 MB · 4:12 · FLAC");audio.screen.sections.focusCard(card);
            if(card.equals(AudioScreen.CONVERSION)){
                AudioRunState run=new AudioRunState();run.total=2;run.current=2;run.running=true;run.bitrate=128;run.percent=90;run.rate=44100;run.doneSamples=11113200;run.totalSamples=11113200;run.name="Secondo brano.flac";run.message="Verifica decodifica Opus…";run.onlySmaller=true;
                run.results.add(new AudioRunState.Result("Primo brano.flac","content://saved/1",32500000,4050000,null));field.set(null,run);audio.renderRun();
            }
            View page=audio.screen.page;int width=audio.screen.ui.dp(360);page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());
            float density=activity.get().getResources().getDisplayMetrics().density;Bitmap bitmap=Bitmap.createBitmap(360,(int)Math.ceil(page.getMeasuredHeight()/density),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(audio.screen.ui.bg);canvas.scale(1/density,1/density);page.draw(canvas);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,bytes));bitmap.recycle();assertTrue(bytes.size()>10000);assertTrue(bytes.size()<250000);
            File dir=new File("build/reports/ui");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,name))){out.write(bytes.toByteArray());}
        }finally{activity.pause().stop().destroy();field.set(null,previous);context.getSharedPreferences("audio-workspace",0).edit().clear().commit();}
    }
    @Test public void files()throws Exception{render(AudioScreen.FILES,"audio-files.png");}
    @Test @Config(qualifiers="w360dp-h800dp-night-xhdpi") public void qualityDark()throws Exception{render(AudioScreen.COMPRESSION,"audio-quality-dark.png");}
    @Test public void verifying()throws Exception{render(AudioScreen.CONVERSION,"audio-conversion.png");}
    @Test public void settingsPage()throws Exception{
        var context=RuntimeEnvironment.getApplication();context.getSharedPreferences("audio-startup-settings",0).edit().clear().commit();
        var activity=Robolectric.buildActivity(SettingsActivity.class,new android.content.Intent(context,SettingsActivity.class).putExtra(AudioSettingsScreen.AUDIO,true)).setup();
        try{
            AudioSettingsScreen screen=activity.get().audioScreen;View page=screen.page;int width=screen.ui.dp(360);page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());
            float density=activity.get().getResources().getDisplayMetrics().density;Bitmap bitmap=Bitmap.createBitmap(360,(int)Math.ceil(page.getMeasuredHeight()/density),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(screen.ui.bg);canvas.scale(1/density,1/density);page.draw(canvas);
            File dir=new File("build/reports/ui");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,"audio-settings.png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
        }finally{activity.pause().stop().destroy();context.getSharedPreferences("audio-startup-settings",0).edit().clear().commit();}
    }
}
