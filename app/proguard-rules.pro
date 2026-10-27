# Native bridges resolve these callback members by their literal JNI names.
-keepclassmembers class * {
    boolean isCancelled();
    void onLog(java.lang.String);
    void onHeaders(byte[]);
    void onPacket(byte[], long, boolean);
    boolean progress(long, long, int, int);
    void log(java.lang.String);
    void stage(java.lang.String);
}
