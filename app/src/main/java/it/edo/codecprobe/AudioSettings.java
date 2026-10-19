package it.edo.codecprobe;

import android.content.Context;
import android.content.SharedPreferences;

/** Explicitly saved audio defaults, separate from the editable queue and video preferences. */
final class AudioSettings {
    final int bitrate;final String folder;final boolean onlySmaller,keepScreenOn;final long revision;
    private static SharedPreferences preferences(Context c){return c.getSharedPreferences("audio-startup-settings",Context.MODE_PRIVATE);}
    AudioSettings(Context c){
        SharedPreferences p=preferences(c);revision=p.getLong("revision",0);
        // Retain the user's existing audio choices until the first explicit settings save.
        if(revision==0)p=c.getSharedPreferences("audio-workspace",Context.MODE_PRIVATE);
        bitrate=normalize(p.getInt("bitrate",128));folder=p.getString("folder",null);
        onlySmaller=p.getBoolean("onlySmaller",true);keepScreenOn=p.getBoolean("keepScreenOn",false);
    }
    void apply(AudioState state){state.bitrate=bitrate;state.folder=folder;state.onlySmaller=onlySmaller;state.keepScreenOn=keepScreenOn;state.settingsRevision=revision;}
    static int normalize(int bitrate){return 64+(Math.max(64,Math.min(256,bitrate))-64)/8*8;}
    static void save(Context c,int bitrate,String folder,boolean smaller,boolean screen){
        SharedPreferences p=preferences(c);p.edit().putInt("bitrate",normalize(bitrate)).putString("folder",folder).putBoolean("onlySmaller",smaller).putBoolean("keepScreenOn",screen).putLong("revision",p.getLong("revision",0)+1).apply();
    }
}
