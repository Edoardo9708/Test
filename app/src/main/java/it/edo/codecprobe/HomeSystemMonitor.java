package it.edo.codecprobe;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import java.util.Locale;
import android.os.*;
import java.util.concurrent.*;

/**
* Process metrics polling; no encoding or queue ownership.
*/
final class HomeSystemMonitor {
    private final HomeController home;
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    boolean memorySamplePending;
    long lastMemorySample;
    long previousCpu;
    long previousWall;
    final Runnable sample = new Runnable(){

        public void run() {
            if (home.state.destroyed || !home.state.conversionScreenVisible) return;
            long now = SystemClock.elapsedRealtime();
            long cpu = android.os.Process.getElapsedCpuTime();
            int cores = Runtime.getRuntime().availableProcessors();
            double usage = previousWall == 0 ? -1 : Math.min(100, Math.max(0, 100.0 * (cpu - previousCpu) / Math.max(1, now - previousWall) / Math.max(1, cores)));
            previousCpu = cpu;
            previousWall = now;
            home.screen.conversion.cpuValue.setText(usage < 0 ? "—" : String.format(Locale.ITALY, "%.1f%%", usage));
            home.screen.conversion.cpuValue.setTextColor(systemColor(DashboardMetrics.cpuBand(usage), false));
            double temperature = Double.NaN;
            try {
                Intent battery = home.activity.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (battery != null && battery.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) temperature = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0;
            } catch (RuntimeException ignored) {
            }
            home.screen.conversion.batteryValue.setText(Double.isFinite(temperature) ? String.format(Locale.ITALY, "%.1f °C", temperature) : "non disponibile");
            home.screen.conversion.batteryValue.setTextColor(systemColor(DashboardMetrics.batteryBand(temperature), true));
            if (!memorySamplePending && (lastMemorySample == 0 || now - lastMemorySample >= 3000)) {
                memorySamplePending = true;
                lastMemorySample = now;
                worker.execute(()->{
                    String ram;
                    try {
                        Debug.MemoryInfo memory = new Debug.MemoryInfo();
                        Debug.getMemoryInfo(memory);
                        ram = String.format(Locale.ITALY, "%.1f MiB", memory.getTotalPss() / 1024.0);
                    } catch (RuntimeException | LinkageError e) {
                        ram = "non disponibile";
                    }
                    final String measured = ram;
                    home.activity.runOnUiThread(()->{
                        memorySamplePending = false;
                        if (!home.state.destroyed) home.screen.conversion.ramValue.setText(measured);
                    });
                });
            }
            if (!home.state.hdrTesting) {
                ConversionService.State session = ConversionService.snapshot();
                home.conversion.renderElapsed(session,now);
            }
            home.handler.postDelayed(this, 1000);
        }
    };

    HomeSystemMonitor(HomeController home) {
        this.home = home;
    }

    int systemColor(int band, boolean battery) {
        if (band < 0) return home.ui.muted;
        String[] light = battery ? new String[]{"#18713E", "#8A6000", "#B54A00", "#B51F35"} : new String[]{"#215CBD", "#18713E", "#8A6000", "#B54A00"};
        String[] dark = battery ? new String[]{"#80D5A0", "#F2C66D", "#FFAF75", "#FF909F"} : new String[]{"#99C1FF", "#80D5A0", "#F2C66D", "#FFAF75"};
        return Color.parseColor((home.ui.dark ? dark : light)[Math.min(3, band)]);
    }
}
