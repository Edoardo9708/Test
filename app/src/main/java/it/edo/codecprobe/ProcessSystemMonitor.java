package it.edo.codecprobe;

import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.widget.TextView;
import java.util.Locale;
import java.util.concurrent.*;

/** One process-metrics sampler for all workspaces; memory reads stay off the UI thread. */
final class ProcessSystemMonitor {
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AppUi ui;private final TextView cpuValue,ramValue,batteryValue;
    private long previousCpu,previousWall,lastSample,lastMemory;private boolean memoryPending,closed;
    ProcessSystemMonitor(AppUi ui,TextView cpu,TextView ram,TextView battery){this.ui=ui;cpuValue=cpu;ramValue=ram;batteryValue=battery;}
    void reset(){previousWall=0;lastSample=0;}
    void sample(long now){
        if(closed||lastSample>0&&now-lastSample<1000)return;lastSample=now;
        long cpu=android.os.Process.getElapsedCpuTime();int cores=Runtime.getRuntime().availableProcessors();double usage=previousWall==0?-1:Math.min(100,Math.max(0,100.0*(cpu-previousCpu)/Math.max(1,now-previousWall)/Math.max(1,cores)));previousCpu=cpu;previousWall=now;
        cpuValue.setText(usage<0?"—":String.format(Locale.ITALY,"%.1f%%",usage));cpuValue.setTextColor(color(DashboardMetrics.cpuBand(usage),false));
        double temperature=Double.NaN;try{Intent battery=ui.activity.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(battery!=null&&battery.hasExtra(BatteryManager.EXTRA_TEMPERATURE))temperature=battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10.0;}catch(RuntimeException ignored){}
        batteryValue.setText(Double.isFinite(temperature)?String.format(Locale.ITALY,"%.1f °C",temperature):"non disponibile");batteryValue.setTextColor(color(DashboardMetrics.batteryBand(temperature),true));
        if(!memoryPending&&(lastMemory==0||now-lastMemory>=3000)){memoryPending=true;lastMemory=now;worker.execute(()->{
            String value;try{Debug.MemoryInfo memory=new Debug.MemoryInfo();Debug.getMemoryInfo(memory);value=String.format(Locale.ITALY,"%.1f MiB",memory.getTotalPss()/1024.0);}catch(RuntimeException|LinkageError error){value="non disponibile";}
            String measured=value;ui.activity.runOnUiThread(()->{memoryPending=false;if(!closed)ramValue.setText(measured);});
        });}
    }
    int color(int band,boolean battery){if(band<0)return ui.muted;String[] light=battery?new String[]{"#18713E","#8A6000","#B54A00","#B51F35"}:new String[]{"#215CBD","#18713E","#8A6000","#B54A00"};String[] dark=battery?new String[]{"#80D5A0","#F2C66D","#FFAF75","#FF909F"}:new String[]{"#99C1FF","#80D5A0","#F2C66D","#FFAF75"};return Color.parseColor((ui.dark?dark:light)[Math.min(3,band)]);}
    void close(){closed=true;worker.shutdownNow();}
}
