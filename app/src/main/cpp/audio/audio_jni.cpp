#include "audio_encoder.h"
#include <jni.h>
#include <stdexcept>
extern "C" JNIEXPORT jlong JNICALL Java_it_edo_codecprobe_AudioCodec_encode(JNIEnv* env,jclass,jint fd,jstring path,jint bitrate,jobject callback){
    const char* raw=env->GetStringUTFChars(path,nullptr);if(!raw)return 0;
    std::string destination(raw);env->ReleaseStringUTFChars(path,raw);
    jclass type=env->GetObjectClass(callback);
    auto progress=env->GetMethodID(type,"progress","(JJII)Z");auto log=env->GetMethodID(type,"log","(Ljava/lang/String;)V");
    if(!progress||!log)return 0;
    try{
        AudioCallbacks cb;
        cb.progress=[&](uint64_t done,uint64_t total,unsigned rate,unsigned channels){
            bool keep=env->CallBooleanMethod(callback,progress,static_cast<jlong>(done),static_cast<jlong>(total),static_cast<jint>(rate),static_cast<jint>(channels));
            if(env->ExceptionCheck())throw std::runtime_error("Callback avanzamento fallita");return keep;
        };
        cb.log=[&](const std::string& message){jstring value=env->NewStringUTF(message.c_str());if(!value)throw std::runtime_error("Memoria log JNI esaurita");env->CallVoidMethod(callback,log,value);env->DeleteLocalRef(value);if(env->ExceptionCheck())throw std::runtime_error("Callback log fallita");};
        return static_cast<jlong>(encodeFlacOpus(fd,destination.c_str(),bitrate,cb));
    }catch(const std::exception& error){if(!env->ExceptionCheck())env->ThrowNew(env->FindClass("java/io/IOException"),error.what());return 0;}
}
