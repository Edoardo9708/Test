package it.edo.codecprobe;

import java.io.IOException;

/** JNI only; callbacks run synchronously on the audio conversion worker. */
final class AudioCodec {
    interface Listener {
        boolean progress(long samples,long total,int rate,int channels);
        void log(String text);
    }
    static void load(){System.loadLibrary("audioopus");}
    static native long encode(int inputFd,String outputPath,int bitrate,Listener listener)throws IOException;
    private AudioCodec(){}
}
