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

#include "audio_stream.h"
namespace {
auto logger(JNIEnv* env,jobject callback){
    jclass type=env->GetObjectClass(callback);auto method=env->GetMethodID(type,"log","(Ljava/lang/String;)V");env->DeleteLocalRef(type);
    if(!method)throw std::runtime_error("Callback log non disponibile");
    return [=](const std::string& text){jstring value=env->NewStringUTF(text.c_str());if(!value)throw std::runtime_error("Memoria log JNI esaurita");env->CallVoidMethod(callback,method,value);env->DeleteLocalRef(value);if(env->ExceptionCheck())throw std::runtime_error("Callback log fallita");};
}
void ioError(JNIEnv* env,const std::exception& e){if(!env->ExceptionCheck())env->ThrowNew(env->FindClass("java/io/IOException"),e.what());}
}
extern "C" JNIEXPORT jlong JNICALL Java_it_edo_codecprobe_AudioCodec_openPcm(JNIEnv* env,jclass,jint fd,jint kind,jstring path,jint rate,jint channels,jint bitrate,jobject callback){
    const char* raw=env->GetStringUTFChars(path,nullptr);if(!raw)return 0;
    std::string destination(raw);env->ReleaseStringUTFChars(path,raw);
    try{return reinterpret_cast<jlong>(new AudioStream(fd,kind,destination.c_str(),rate,channels,bitrate,logger(env,callback)));}
    catch(const std::exception& e){ioError(env,e);return 0;}
}
extern "C" JNIEXPORT void JNICALL Java_it_edo_codecprobe_AudioCodec_writePcm(JNIEnv* env,jclass,jlong handle,jfloatArray pcm,jint count){
    if(!handle||!pcm||count<=0||count>262144||count>env->GetArrayLength(pcm)){env->ThrowNew(env->FindClass("java/io/IOException"),"Blocco PCM JNI non valido");return;}
    jfloat* values=env->GetFloatArrayElements(pcm,nullptr);if(!values)return;
    try{reinterpret_cast<AudioStream*>(handle)->write(values,count);}
    catch(const std::exception& e){ioError(env,e);}
    env->ReleaseFloatArrayElements(pcm,values,JNI_ABORT);
}
extern "C" JNIEXPORT jlong JNICALL Java_it_edo_codecprobe_AudioCodec_finishPcm(JNIEnv* env,jclass,jlong handle,jobject callback){
    try{if(!handle)throw std::runtime_error("Encoder PCM chiuso");return reinterpret_cast<AudioStream*>(handle)->finish(logger(env,callback));}
    catch(const std::exception& e){ioError(env,e);return 0;}
}
extern "C" JNIEXPORT void JNICALL Java_it_edo_codecprobe_AudioCodec_closePcm(JNIEnv*,jclass,jlong handle){delete reinterpret_cast<AudioStream*>(handle);}
