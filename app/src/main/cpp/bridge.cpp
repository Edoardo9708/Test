#include "encoder.h"
#include <jni.h>
#include <stdexcept>
static void check(JNIEnv* e) {
    if(e->ExceptionCheck()) throw std::runtime_error("Java callback failed (see Java stack trace)");
}
extern "C" JNIEXPORT void JNICALL
Java_it_edo_codecprobe_X265Probe_encode(JNIEnv* e,jclass,jobject target,jint crf) {
    jclass cls=e->GetObjectClass(target);
    jmethodID log=e->GetMethodID(cls,"onLog","(Ljava/lang/String;)V");
    jmethodID cancel=e->GetMethodID(cls,"isCancelled","()Z");
    jmethodID headers=e->GetMethodID(cls,"onHeaders","([B)V");
    jmethodID packet=e->GetMethodID(cls,"onPacket","([BJZ)V");
    if(e->ExceptionCheck()) return;
    auto array=[&](const std::vector<uint8_t>& data) {
        jbyteArray a=e->NewByteArray(static_cast<jsize>(data.size()));
        if(!a) throw std::runtime_error("JNI byte array allocation failed");
        e->SetByteArrayRegion(a,0,static_cast<jsize>(data.size()),reinterpret_cast<const jbyte*>(data.data()));
        check(e); return a;
    };
    ProbeCallbacks cb;
    cb.log=[&](const std::string& s) {
        jstring text=e->NewStringUTF(s.c_str()); check(e);
        e->CallVoidMethod(target,log,text); e->DeleteLocalRef(text); check(e);
    };
    cb.cancelled=[&]() { bool yes=e->CallBooleanMethod(target,cancel); check(e); return yes; };
    cb.headers=[&](const std::vector<uint8_t>& data) {
        jbyteArray a=array(data); e->CallVoidMethod(target,headers,a); e->DeleteLocalRef(a); check(e);
    };
    cb.packet=[&](const std::vector<uint8_t>& data,int64_t pts,bool key) {
        jbyteArray a=array(data); e->CallVoidMethod(target,packet,a,static_cast<jlong>(pts),static_cast<jboolean>(key));
        e->DeleteLocalRef(a); check(e);
    };
    try { runProbe(cb,crf); }
    catch(const std::exception& ex) {
        if(!e->ExceptionCheck()) {
            jclass error=e->FindClass("java/lang/RuntimeException");
            e->ThrowNew(error,ex.what());
        }
    }
    e->DeleteLocalRef(cls);
}
