#include "audio_stream.h"
#include <opus.h>
#include <cmath>
#include <stdexcept>
namespace {void check(bool ok,const std::string& msg){if(!ok)throw std::runtime_error(msg);}}
AudioStream::AudioStream(int fd,int kind,const char* path,int r,int c,int b,const std::function<void(const std::string&)>& log):tags(readCompressedMetadata(fd,kind)),destination(path),rate(r),channels(c),bitrate(b){
    check((c==1||c==2)&&r>=8000&&r<=192000&&b>=64000&&b<=256000,"Formato PCM o bitrate non supportato");
    std::unique_ptr<OggOpusComments,decltype(&ope_comments_destroy)> comments(ope_comments_create(),ope_comments_destroy);
    check(bool(comments),"Memoria commenti Opus non disponibile");
    tags.apply(comments.get(),log);
    int rc=0;encoder=ope_encoder_create_file(path,comments.get(),rate,channels,0,&rc);
    check(encoder!=nullptr,std::string("Apertura Opus: ")+ope_strerror(rc));
    try {
#define SET(call) check(ope_encoder_ctl(encoder,call)==OPE_OK,"Configurazione Opus fallita: " #call)
        SET(OPUS_SET_APPLICATION(OPUS_APPLICATION_AUDIO));SET(OPUS_SET_BITRATE(bitrate));SET(OPUS_SET_VBR(1));SET(OPUS_SET_VBR_CONSTRAINT(0));SET(OPUS_SET_COMPLEXITY(10));
#undef SET
    }catch(...){ope_encoder_destroy(encoder);encoder=nullptr;throw;}
}
AudioStream::~AudioStream(){if(encoder)ope_encoder_destroy(encoder);}
void AudioStream::write(const float* pcm,size_t count){
    check(encoder&&count>0&&count<=262144&&count%channels==0,"Blocco PCM non valido o encoder chiuso");
    for(size_t i=0;i<count;i++)check(std::isfinite(pcm[i])&&pcm[i]>=-1.0f&&pcm[i]<=1.0f,"PCM non finito o fuori intervallo");
    int rc=ope_encoder_write_float(encoder,pcm,static_cast<int>(count/channels));check(rc==OPE_OK,std::string("Scrittura Opus: ")+ope_strerror(rc));samples+=count/channels;
}
uint64_t AudioStream::finish(const std::function<void(const std::string&)>& log){
    check(encoder&&samples>0,"Nessun campione PCM da salvare");
    int rc=ope_encoder_drain(encoder);check(rc==OPE_OK,std::string("Drain Opus: ")+ope_strerror(rc));
    int b=0,v=0,c=0,x=0;
    check(ope_encoder_ctl(encoder,OPUS_GET_BITRATE(&b))==OPE_OK&&ope_encoder_ctl(encoder,OPUS_GET_VBR(&v))==OPE_OK&&ope_encoder_ctl(encoder,OPUS_GET_VBR_CONSTRAINT(&c))==OPE_OK&&ope_encoder_ctl(encoder,OPUS_GET_COMPLEXITY(&x))==OPE_OK,"Rilettura parametri Opus fallita");
    check(b==bitrate&&v==1&&c==0&&x==10,"Parametri effettivi Opus diversi dai richiesti");
    ope_encoder_destroy(encoder);encoder=nullptr;
    tags.verify(destination.c_str(),log);
    return samples*1000000ULL/rate;
}
