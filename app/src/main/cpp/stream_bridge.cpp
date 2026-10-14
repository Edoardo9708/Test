#include "stream_encoder.h"
#include <jni.h>
#include <stdexcept>
static void checkJava(JNIEnv* e) { if(e->ExceptionCheck()) throw std::runtime_error("Errore callback Java"); }
static void raise(JNIEnv* e,const std::exception& ex) {
    if(!e->ExceptionCheck()) { jclass c=e->FindClass("java/lang/RuntimeException"); e->ThrowNew(c,ex.what()); e->DeleteLocalRef(c); }
}
struct Callbacks {
    JNIEnv* e; jobject target; jclass cls; ProbeCallbacks cb;
    Callbacks(JNIEnv* env,jobject obj):e(env),target(obj),cls(env->GetObjectClass(obj)) {
        jmethodID log=e->GetMethodID(cls,"onLog","(Ljava/lang/String;)V");
        jmethodID cancel=e->GetMethodID(cls,"isCancelled","()Z");
        jmethodID headers=e->GetMethodID(cls,"onHeaders","([B)V");
        jmethodID packet=e->GetMethodID(cls,"onPacket","([BJZ)V");
        checkJava(e);
        cb.log=[this,log](const std::string& s) {
            jstring text=e->NewStringUTF(s.c_str()); checkJava(e);
            e->CallVoidMethod(target,log,text); e->DeleteLocalRef(text); checkJava(e);
        };
        cb.cancelled=[this,cancel]() { bool v=e->CallBooleanMethod(target,cancel); checkJava(e); return v; };
        cb.headers=[this,headers](const std::vector<uint8_t>& d) {
            auto a=array(d); e->CallVoidMethod(target,headers,a); e->DeleteLocalRef(a); checkJava(e);
        };
        cb.packet=[this,packet](const std::vector<uint8_t>& d,int64_t pts,bool key) {
            auto a=array(d); e->CallVoidMethod(target,packet,a,static_cast<jlong>(pts),static_cast<jboolean>(key)); e->DeleteLocalRef(a); checkJava(e);
        };
    }
    ~Callbacks(){e->DeleteLocalRef(cls);}
    jbyteArray array(const std::vector<uint8_t>& d) {
        auto a=e->NewByteArray(static_cast<jsize>(d.size())); checkJava(e);
        if(!a) throw std::runtime_error("Memoria JNI insufficiente");
        e->SetByteArrayRegion(a,0,static_cast<jsize>(d.size()),reinterpret_cast<const jbyte*>(d.data())); checkJava(e); return a;
    }
};
extern "C" JNIEXPORT jlong JNICALL Java_it_edo_codecprobe_VideoConverter_nativeOpen(
    JNIEnv* e,jobject target,jint w,jint h,jint fps,jint crf,jint standard,jint transfer,jint range,jint preset,jint cpuMode,jint cores,jboolean bframes,jboolean lookahead,jint primaries,jint colorTransfer,jint matrix,jint colorFlags) {
    try {
        Callbacks c(e,target);
        auto s=std::make_unique<StreamEncoder>(StreamConfig{w,h,fps,crf,standard,transfer,range,preset,cpuMode,cores,bframes!=0,lookahead!=0,true,primaries,colorTransfer,matrix,colorFlags},c.cb);
        return reinterpret_cast<jlong>(s.release());
    } catch(const std::exception& ex) {raise(e,ex);return 0;}
}
extern "C" JNIEXPORT void JNICALL Java_it_edo_codecprobe_VideoConverter_nativeFrame(
    JNIEnv* e,jobject target,jlong handle,jobject buffer,jlong pts) {
    try {
        if(!handle) throw std::runtime_error("Encoder non aperto");
        Callbacks c(e,target);
        auto size=e->GetDirectBufferCapacity(buffer);
        auto ptr=static_cast<const uint8_t*>(e->GetDirectBufferAddress(buffer));
        if(!ptr || size<0) throw std::runtime_error("Buffer I420 non diretto");
        reinterpret_cast<StreamEncoder*>(handle)->frame(ptr,static_cast<size_t>(size),pts,c.cb);
    } catch(const std::exception& ex) {raise(e,ex);}
}
extern "C" JNIEXPORT void JNICALL Java_it_edo_codecprobe_VideoConverter_nativeFinish(JNIEnv* e,jobject target,jlong handle) {
    try { if(!handle) throw std::runtime_error("Encoder non aperto"); Callbacks c(e,target); reinterpret_cast<StreamEncoder*>(handle)->finish(c.cb); }
    catch(const std::exception& ex) {raise(e,ex);}
}
extern "C" JNIEXPORT void JNICALL Java_it_edo_codecprobe_VideoConverter_nativeClose(JNIEnv*,jobject,jlong handle) {
    delete reinterpret_cast<StreamEncoder*>(handle);
}


static jintArray temporalArray(JNIEnv* e,const std::array<int,5>& values) {
    jint data[]={values[0],values[1],values[2],values[3],values[4]};jintArray a=e->NewIntArray(5);if(a)e->SetIntArrayRegion(a,0,5,data);return a;
}
extern "C" JNIEXPORT jintArray JNICALL Java_it_edo_codecprobe_MainActivity_nativePresetSettings(JNIEnv* e,jclass,jint preset,jboolean bframes,jboolean lookahead){
    try{return temporalArray(e,StreamEncoder::presetSettings(preset,bframes!=0,lookahead!=0));}catch(const std::exception& ex){raise(e,ex);return nullptr;}
}
extern "C" JNIEXPORT jintArray JNICALL Java_it_edo_codecprobe_VideoConverter_nativeEffectiveSettings(JNIEnv* e,jobject,jlong handle){
    try{if(!handle)throw std::runtime_error("Encoder non aperto");return temporalArray(e,reinterpret_cast<StreamEncoder*>(handle)->effectiveSettings());}catch(const std::exception& ex){raise(e,ex);return nullptr;}
}

