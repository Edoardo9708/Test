package it.edo.codecprobe;

import java.io.IOException;

/** JNI boundary for the bundled JPEG/HEIC engine. No Android codec is used. */
final class ImageNative {
    static {System.loadLibrary("imageheic");}
    interface Callback {void stage(String message)throws IOException;}
    static native String convert(String input,String output,int quality,String preset,String tune,Callback callback)throws IOException;
    static native String inspect(String input)throws IOException;
    private ImageNative(){}
}
