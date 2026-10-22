#include "image_core.h"
#include <jni.h>
#include <stdexcept>
extern "C" JNIEXPORT jstring JNICALL Java_it_edo_codecprobe_ImageNative_convert(JNIEnv* env,jclass,jstring input,jstring output,jint quality,jstring preset,jstring tune,jobject callback){
    try{
        auto text=[&](jstring value){const char* p=env->GetStringUTFChars(value,nullptr);if(!p)throw std::runtime_error("Stringa JNI non disponibile");std::string s(p);env->ReleaseStringUTFChars(value,p);return s;};
        std::string in=text(input),out=text(output),speed=text(preset),mode=text(tune);
        jclass type=env->GetObjectClass(callback);jmethodID stage=env->GetMethodID(type,"stage","(Ljava/lang/String;)V");env->DeleteLocalRef(type);if(!stage)return nullptr;
        std::string result=photo::convert(in,out,quality,speed,mode,[&](const std::string& message){jstring value=env->NewStringUTF(message.c_str());if(!value)throw std::runtime_error("Messaggio JNI non disponibile");env->CallVoidMethod(callback,stage,value);env->DeleteLocalRef(value);if(env->ExceptionCheck())throw std::runtime_error("Operazione immagini interrotta");});
        return env->NewStringUTF(result.c_str());
    }catch(const std::exception& error){if(!env->ExceptionCheck()){jclass type=env->FindClass("java/io/IOException");env->ThrowNew(type,error.what());env->DeleteLocalRef(type);}return nullptr;}
}

extern "C" JNIEXPORT jstring JNICALL Java_it_edo_codecprobe_ImageNative_inspect(JNIEnv* env,jclass,jstring input){
    try{
        const char* p=env->GetStringUTFChars(input,nullptr);if(!p)return nullptr;
        std::string path(p);env->ReleaseStringUTFChars(input,p);
        return env->NewStringUTF(photo::inspectJpeg(path).c_str());
    }catch(const std::exception& error){if(!env->ExceptionCheck()){jclass type=env->FindClass("java/io/IOException");env->ThrowNew(type,error.what());env->DeleteLocalRef(type);}return nullptr;}
}
