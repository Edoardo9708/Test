package it.edo.codecprobe;

import java.util.Locale;

final class DashboardMetrics {
    static String elapsed(long started,long ended,long now){
        if(started<=0)return "00:00:00";
        long seconds=Math.max(0,(ended>0?ended:now)-started)/1000;
        return String.format(Locale.ITALY,"%02d:%02d:%02d",seconds/3600,(seconds/60)%60,seconds%60);
    }
    static int cpuBand(double value){
        if(!Double.isFinite(value)||value<0)return -1;
        return value<30?0:value<70?1:value<90?2:3;
    }
    static int batteryBand(double value){
        if(!Double.isFinite(value))return -1;
        return value<35?0:value<40?1:value<45?2:3;
    }
    private DashboardMetrics(){}
}
