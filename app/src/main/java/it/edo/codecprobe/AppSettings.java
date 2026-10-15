package it.edo.codecprobe;

import android.content.Context;
import android.content.SharedPreferences;

final class AppSettings {
    static final String[] PRESETS={"ultrafast","superfast","veryfast","faster","fast","medium","slow","slower","veryslow"};
    final int crf,preset,cpu;
    final boolean bframes,lookahead,prefix,keepScreenOn;
    final String addition,defaultFolder;
    final long revision;
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("startup-settings",Context.MODE_PRIVATE);}
    AppSettings(Context c){
        SharedPreferences p=prefs(c);
        crf=clamp(p.getInt("crf",23),51);preset=clamp(p.getInt("preset",6),8);cpu=clamp(p.getInt("cpu",1),2);
        defaultFolder=p.getString("defaultFolder",null);
        keepScreenOn=p.getBoolean("keepScreenOn",false);
        bframes=p.getBoolean("bframes",true);lookahead=p.getBoolean("lookahead",true);
        prefix=p.getBoolean("prefix",true);addition=p.getString("addition","HEVC-");revision=p.getLong("revision",0);
    }
    static void setDefaultFolder(Context c,String uri){prefs(c).edit().putString("defaultFolder",uri).apply();}
    static void setKeepScreenOn(Context c,boolean enabled){prefs(c).edit().putBoolean("keepScreenOn",enabled).apply();}
    static void save(Context c,int crf,int preset,int cpu,boolean b,boolean look,String addition,boolean prefix){
        SharedPreferences p=prefs(c);
        p.edit().putInt("crf",clamp(crf,51)).putInt("preset",clamp(preset,8)).putInt("cpu",clamp(cpu,2))
            .putBoolean("bframes",b).putBoolean("lookahead",look).putString("addition",addition).putBoolean("prefix",prefix)
            .putLong("revision",p.getLong("revision",0)+1).apply();
    }
    private static int clamp(int v,int max){return Math.max(0,Math.min(max,v));}
}


