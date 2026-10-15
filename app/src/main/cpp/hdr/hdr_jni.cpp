#include "hdr_encoder.h"
#include <jni.h>
#include <stdexcept>
namespace {
void checked(JNIEnv* e){if(e->ExceptionCheck())throw std::runtime_error("Callback HDR Java fallita");}
void fail(JNIEnv* e,const std::exception& ex){if(!e->ExceptionCheck()){jclass c=e->FindClass("java/lang/RuntimeException");e->ThrowNew(c,ex.what());e->DeleteLocalRef(c);}}
struct Callbacks {
    JNIEnv* e;jobject target;jclass cls;ProbeCallbacks cb;
    Callbacks(JNIEnv* env,jobject obj):e(env),target(obj),cls(env->GetObjectClass(obj)){
        auto log=e->GetMethodID(cls,"onLog","(Ljava/lang/String;)V"),cancel=e->GetMethodID(cls,"isCancelled","()Z"),headers=e->GetMethodID(cls,"onHeaders","([B)V"),packet=e->GetMethodID(cls,"onPacket","([BJZ)V");checked(e);
        cb.log=[this,log](const std::string& s){jstring value=e->NewStringUTF(s.c_str());checked(e);e->CallVoidMethod(target,log,value);e->DeleteLocalRef(value);checked(e);};
        cb.cancelled=[this,cancel](){bool value=e->CallBooleanMethod(target,cancel);checked(e);return value;};
        cb.headers=[this,headers](const std::vector<uint8_t>& b){auto a=array(b);e->CallVoidMethod(target,headers,a);e->DeleteLocalRef(a);checked(e);};
        cb.packet=[this,packet](const std::vector<uint8_t>& b,int64_t pts,bool key){auto a=array(b);e->CallVoidMethod(target,packet,a,static_cast<jlong>(pts),static_cast<jboolean>(key));e->DeleteLocalRef(a);checked(e);};
    }
    ~Callbacks(){e->DeleteLocalRef(cls);}
    jbyteArray array(const std::vector<uint8_t>& b){auto a=e->NewByteArray(static_cast<jsize>(b.size()));checked(e);if(!a)throw std::runtime_error("Memoria HDR JNI insufficiente");e->SetByteArrayRegion(a,0,static_cast<jsize>(b.size()),reinterpret_cast<const jbyte*>(b.data()));checked(e);return a;}
};
}
extern "C" JNIEXPORT jlong JNICALL Java_it_edo_codecprobe_HdrProbe_nativeOpen(JNIEnv* e,jobject obj,jint w,jint h,jint crf,jint p,jint t,jint m,jint flags){try{Callbacks c(e,obj);auto encoder=std::make_unique<HdrEncoder>(w,h,crf,p,t,m,flags,c.cb);return reinterpret_cast<jlong>(encoder.release());}catch(const std::exception& ex){fail(e,ex);return 0;}}
extern "C" JNIEXPORT void JNICALL Java_it_edo_codecprobe_HdrProbe_nativeFrame(JNIEnv* e,jobject obj,jlong handle,jobject pixels,jlong pts){try{if(!handle)throw std::runtime_error("Encoder HDR assente");Callbacks c(e,obj);auto size=e->GetDirectBufferCapacity(pixels);auto data=e->GetDirectBufferAddress(pixels);if(!data||size<0)throw std::runtime_error("Buffer HDR non diretto");reinterpret_cast<HdrEncoder*>(handle)->frame(static_cast<const uint8_t*>(data),static_cast<size_t>(size),pts,c.cb);}catch(const std::exception& ex){fail(e,ex);}}
extern "C" JNIEXPORT void JNICALL Java_it_edo_codecprobe_HdrProbe_nativeFinish(JNIEnv* e,jobject obj,jlong handle){try{if(!handle)throw std::runtime_error("Encoder HDR assente");Callbacks c(e,obj);reinterpret_cast<HdrEncoder*>(handle)->finish(c.cb);}catch(const std::exception& ex){fail(e,ex);}}
extern "C" JNIEXPORT void JNICALL Java_it_edo_codecprobe_HdrProbe_nativeClose(JNIEnv*,jobject,jlong handle){delete reinterpret_cast<HdrEncoder*>(handle);}

extern "C" JNIEXPORT jlong JNICALL Java_it_edo_codecprobe_HdrTranscoder_nativeOpen(JNIEnv* e,jobject obj,jint w,jint h,jint fps,jint crf,jint preset,jint cpu,jint cores,jboolean b,jboolean look,jint p,jint t,jint m,jint flags){
    try{Callbacks cb(e,obj);StreamConfig c{w,h,fps,crf,6,t==16?6:7,(flags&1)?1:2};c.preset=preset;c.cpuMode=cpu;c.cores=cores;c.bframes=b;c.lookahead=look;c.colorPrimaries=p;c.colorTransfer=t;c.colorMatrix=m;c.colorFlags=flags;
        auto encoder=std::make_unique<HdrEncoder>(c,cb.cb);return reinterpret_cast<jlong>(encoder.release());
    }catch(const std::exception& ex){fail(e,ex);return 0;}
}
extern "C" JNIEXPORT void JNICALL Java_it_edo_codecprobe_HdrTranscoder_nativeFrame(JNIEnv* e,jobject obj,jlong handle,jobject pixels,jlong pts){Java_it_edo_codecprobe_HdrProbe_nativeFrame(e,obj,handle,pixels,pts);}
extern "C" JNIEXPORT void JNICALL Java_it_edo_codecprobe_HdrTranscoder_nativeFinish(JNIEnv* e,jobject obj,jlong handle){Java_it_edo_codecprobe_HdrProbe_nativeFinish(e,obj,handle);}
extern "C" JNIEXPORT void JNICALL Java_it_edo_codecprobe_HdrTranscoder_nativeClose(JNIEnv* e,jobject obj,jlong handle){Java_it_edo_codecprobe_HdrProbe_nativeClose(e,obj,handle);}
extern "C" JNIEXPORT jintArray JNICALL Java_it_edo_codecprobe_HdrTranscoder_nativeEffectiveSettings(JNIEnv* e,jobject,jlong handle){
    try{if(!handle)throw std::runtime_error("Encoder HDR assente");auto values=reinterpret_cast<HdrEncoder*>(handle)->effectiveSettings();auto a=e->NewIntArray(5);checked(e);if(!a)throw std::runtime_error("Memoria HDR insufficiente");e->SetIntArrayRegion(a,0,5,values.data());return a;}catch(const std::exception& ex){fail(e,ex);return nullptr;}
}

