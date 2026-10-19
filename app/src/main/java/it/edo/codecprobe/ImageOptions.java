package it.edo.codecprobe;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;

/** Immutable options snapshot, separate from file selection and other media settings. */
final class ImageOptions {
    final int quality;final String preset,tune,folder;final boolean onlySmaller,keepScreenOn;final long revision;
    ImageOptions(int quality,String preset,String tune,String folder,boolean onlySmaller,boolean keepScreenOn,long revision){
        if(quality<40||quality>95||!validPreset(preset)||!validTune(tune))throw new IllegalArgumentException("Impostazioni immagini non valide");
        this.quality=quality;this.preset=preset;this.tune=tune;this.folder=folder;this.onlySmaller=onlySmaller;this.keepScreenOn=keepScreenOn;this.revision=revision;
    }
    static boolean validPreset(String value){return "fast".equals(value)||"medium".equals(value)||"slow".equals(value);}
    static boolean validTune(String value){return "standard".equals(value)||"ssim".equals(value)||"psnr".equals(value);}
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("image-settings",0);}
    static ImageOptions load(Context c){var p=prefs(c);int quality=Math.max(40,Math.min(95,p.getInt("quality",65)));String preset=p.getString("preset","slow"),tune=p.getString("tune","standard");return new ImageOptions(quality,validPreset(preset)?preset:"slow",validTune(tune)?tune:"standard",p.getString("folder",null),p.getBoolean("onlySmaller",true),p.getBoolean("keepScreenOn",false),p.getLong("revision",0));}
    void save(Context c){prefs(c).edit().putInt("quality",quality).putString("preset",preset).putString("tune",tune).putString("folder",folder).putBoolean("onlySmaller",onlySmaller).putBoolean("keepScreenOn",keepScreenOn).putLong("revision",Math.max(System.currentTimeMillis(),load(c).revision+1)).apply();}
    void put(Intent intent){intent.putExtra("quality",quality).putExtra("preset",preset).putExtra("tune",tune).putExtra("folder",folder).putExtra("onlySmaller",onlySmaller).putExtra("keepScreenOn",keepScreenOn);}
    static ImageOptions from(Intent intent){return new ImageOptions(intent.getIntExtra("quality",65),intent.getStringExtra("preset"),intent.getStringExtra("tune"),intent.getStringExtra("folder"),intent.getBooleanExtra("onlySmaller",true),intent.getBooleanExtra("keepScreenOn",false),0);}
    ImageOptions withFolder(String folder){return new ImageOptions(quality,preset,tune,folder,onlySmaller,keepScreenOn,revision);}
    String description(){return "HEIC · qualità "+quality+"/100 · CRF "+((100-quality)/2.0)+" · preset "+preset+" · tune "+tune+"\nYCbCr 4:2:0 · 8 bit · range completo · dimensioni originali";}
}
