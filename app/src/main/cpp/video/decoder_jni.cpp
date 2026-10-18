#include "hevc_decoder.h"
#include <jni.h>
#include <memory>
#include <stdexcept>
#include <vector>
namespace {
void fail(JNIEnv* e,const std::exception& ex){if(!e->ExceptionCheck())e->ThrowNew(e->FindClass("java/io/IOException"),ex.what());}
HevcDecoder& decoder(jlong handle){if(!handle)throw std::runtime_error("HEVC decoder closed");return *reinterpret_cast<HevcDecoder*>(handle);}
}
extern "C" JNIEXPORT jlong JNICALL Java_it_edo_codecprobe_SoftwareHevcDecoder_open(JNIEnv* e,jclass,jbyteArray headers,jint w,jint h,jint p,jint t,jint m,jboolean full){
    try{if(!headers)throw std::runtime_error("HEVC headers missing");jsize size=e->GetArrayLength(headers);if(size<=0||size>1024*1024)throw std::runtime_error("Invalid HEVC headers");std::vector<uint8_t> bytes(size);e->GetByteArrayRegion(headers,0,size,reinterpret_cast<jbyte*>(bytes.data()));if(e->ExceptionCheck())return 0;return reinterpret_cast<jlong>(new HevcDecoder(bytes.data(),bytes.size(),w,h,p,t,m,full));}catch(const std::exception& ex){fail(e,ex);return 0;}
}
extern "C" JNIEXPORT void JNICALL Java_it_edo_codecprobe_SoftwareHevcDecoder_send(JNIEnv* e,jclass,jlong handle,jobject buffer,jint size,jlong pts){
    try{if(!buffer){decoder(handle).send(nullptr,0,0);return;}auto data=static_cast<const uint8_t*>(e->GetDirectBufferAddress(buffer));auto capacity=e->GetDirectBufferCapacity(buffer);if(!data||size<=0||capacity<size)throw std::runtime_error("Invalid direct HEVC input");decoder(handle).send(data,size,pts);}catch(const std::exception& ex){fail(e,ex);}
}
extern "C" JNIEXPORT jint JNICALL Java_it_edo_codecprobe_SoftwareHevcDecoder_receive(JNIEnv* e,jclass,jlong handle,jobject buffer,jlongArray values){
    try{if(!buffer||!values||e->GetArrayLength(values)<2)throw std::runtime_error("Invalid HEVC output");auto data=static_cast<uint8_t*>(e->GetDirectBufferAddress(buffer));auto capacity=e->GetDirectBufferCapacity(buffer);if(!data||capacity<0)throw std::runtime_error("Invalid direct HEVC output");int64_t pts=0,precise=0;int result=decoder(handle).receive(data,static_cast<size_t>(capacity),pts,precise);if(result==1){jlong out[]={pts,precise};e->SetLongArrayRegion(values,0,2,out);}return result;}catch(const std::exception& ex){fail(e,ex);return -1;}
}
extern "C" JNIEXPORT void JNICALL Java_it_edo_codecprobe_SoftwareHevcDecoder_close(JNIEnv*,jclass,jlong handle){delete reinterpret_cast<HevcDecoder*>(handle);}
