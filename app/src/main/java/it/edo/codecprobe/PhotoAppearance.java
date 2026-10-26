package it.edo.codecprobe;

import android.content.Context;

/** UI preferences, independent of image encoding defaults and active queue options. */
final class PhotoAppearance {
    private static final String STORE="photo-appearance",BLUR="glassBlur";
    static boolean blurEnabled(Context context){return context.getSharedPreferences(STORE,Context.MODE_PRIVATE).getBoolean(BLUR,true);}
    static void blurEnabled(Context context,boolean enabled){context.getSharedPreferences(STORE,Context.MODE_PRIVATE).edit().putBoolean(BLUR,enabled).apply();}
    private PhotoAppearance(){}
}
