package it.edo.codecprobe;

import android.os.SystemClock;
import java.util.concurrent.ExecutorService;

/** Video timer adapter; metric sampling itself is common to all workspaces. */
final class HomeSystemMonitor {
    private final HomeController home;
    final ExecutorService worker;
    long previousWall;
    final Runnable sample=new Runnable(){public void run(){
        if(home.state.destroyed||!home.state.conversionScreenVisible)return;
        long now=SystemClock.elapsedRealtime();if(previousWall==0)home.screen.conversion.dashboard.monitor.reset();previousWall=now;
        home.screen.conversion.dashboard.monitor.sample(now);
        if(!home.state.hdrTesting)home.conversion.renderElapsed(ConversionService.snapshot(),now);
        home.handler.postDelayed(this,1000);
    }};
    HomeSystemMonitor(HomeController home){this.home=home;worker=home.screen.conversion.dashboard.monitor.worker;}
    void close(){home.screen.conversion.dashboard.monitor.close();}
    int systemColor(int band,boolean battery){return home.screen.conversion.dashboard.monitor.color(band,battery);}
}
