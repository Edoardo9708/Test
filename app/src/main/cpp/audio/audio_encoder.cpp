#include "audio_encoder.h"
#include <FLAC/stream_decoder.h>
#include <opusenc.h>
#include <opus.h>
#include <cmath>
#include <cstdio>
#include <memory>
#include <stdexcept>
#include <vector>
#include <unistd.h>
namespace {
void require(bool ok,const std::string& text){if(!ok)throw std::runtime_error(text);}
struct Job {
    const AudioCallbacks& cb;
    OggOpusEnc* encoder=nullptr;
    uint64_t done=0,total=0;
    unsigned rate=0,channels=0,bits=0;
    std::string error;
    std::vector<float> pcm;
    ~Job(){if(encoder)ope_encoder_destroy(encoder);}
};
void metadata(const FLAC__StreamDecoder*,const FLAC__StreamMetadata* block,void* opaque){
    auto& j=*static_cast<Job*>(opaque);
    if(block->type==FLAC__METADATA_TYPE_STREAMINFO){
        auto& s=block->data.stream_info;j.total=s.total_samples;j.rate=s.sample_rate;j.channels=s.channels;j.bits=s.bits_per_sample;
    }
}
void error(const FLAC__StreamDecoder*,FLAC__StreamDecoderErrorStatus status,void* opaque){
    static_cast<Job*>(opaque)->error=std::string("FLAC: ")+FLAC__StreamDecoderErrorStatusString[status];
}
FLAC__StreamDecoderWriteStatus write(const FLAC__StreamDecoder*,const FLAC__Frame* f,const FLAC__int32* const input[],void* opaque){
    auto& j=*static_cast<Job*>(opaque);
    // C callbacks must not propagate a C++ exception through the library.
    try{
        require(f->header.channels==j.channels&&f->header.sample_rate==j.rate&&f->header.bits_per_sample==j.bits,"Formato FLAC cambiato durante la lettura");
        require(j.cb.progress(j.done,j.total,j.rate,j.channels),"Annullato");
        const unsigned count=f->header.blocksize;
        require(count>0&&count<=65535,"Blocco FLAC non valido");
        j.pcm.resize(static_cast<size_t>(count)*j.channels);
        double scale=std::ldexp(1.0,1-static_cast<int>(j.bits));
        for(unsigned n=0;n<count;n++)for(unsigned c=0;c<j.channels;c++)j.pcm[n*j.channels+c]=static_cast<float>(input[c][n]*scale);
        int rc=ope_encoder_write_float(j.encoder,j.pcm.data(),count);
        require(rc==OPE_OK,std::string("Scrittura Opus: ")+ope_strerror(rc));
        j.done+=count;
        require(j.cb.progress(j.done,j.total,j.rate,j.channels),"Annullato");
        return FLAC__STREAM_DECODER_WRITE_STATUS_CONTINUE;
    }catch(const std::exception& e){j.error=e.what();return FLAC__STREAM_DECODER_WRITE_STATUS_ABORT;}
}
}
uint64_t encodeFlacOpus(int fd,const char* destination,int bitrate,const AudioCallbacks& cb){
    require(bitrate>=64000&&bitrate<=256000,"Bitrate fuori intervallo");
    cb.log(std::string("*** 1. LETTURA FLAC ***\nLibrerie CPU: ")+opus_get_version_string()+" · "+ope_get_version_string()+"\nDecodifica FLAC senza MediaCodec; PCM float, nessuna normalizzazione.");
    int owned=dup(fd);require(owned>=0,"Duplicazione descrittore input fallita");
    FILE* file=fdopen(owned,"rb");if(!file){close(owned);throw std::runtime_error("Apertura input FLAC fallita");}
    auto closeFile=[](FILE* p){fclose(p);};std::unique_ptr<FILE,decltype(closeFile)> guard(file,closeFile);
    auto freeDecoder=[](FLAC__StreamDecoder* d){FLAC__stream_decoder_delete(d);};
    std::unique_ptr<FLAC__StreamDecoder,decltype(freeDecoder)> decoder(FLAC__stream_decoder_new(),freeDecoder);
    require(bool(decoder),"Memoria decoder FLAC non disponibile");
    Job j{cb};
    FLAC__stream_decoder_set_md5_checking(decoder.get(),true);
    auto init=FLAC__stream_decoder_init_FILE(decoder.get(),file,write,metadata,error,&j);
    require(init==FLAC__STREAM_DECODER_INIT_STATUS_OK,"Input non leggibile come FLAC nativo");
    // init_FILE owns fclose after successful initialization.
    guard.release();
    require(FLAC__stream_decoder_process_until_end_of_metadata(decoder.get()),"Metadati FLAC non leggibili: "+j.error);
    require((j.channels==1||j.channels==2)&&j.rate>=8000&&j.rate<=192000&&j.bits>=4&&j.bits<=32,"Prova preliminare: solo FLAC mono/stereo, 8–192 kHz, 4–32 bit");
    require(cb.progress(0,j.total,j.rate,j.channels),"Annullato");
    cb.log("INPUT: "+std::to_string(j.rate)+" Hz · "+std::to_string(j.channels)+" canali · "+std::to_string(j.bits)+" bit · campioni per canale="+std::to_string(j.total));
    std::unique_ptr<OggOpusComments,decltype(&ope_comments_destroy)> comments(ope_comments_create(),ope_comments_destroy);
    require(bool(comments),"Memoria commenti Opus non disponibile");
    int rc=0;j.encoder=ope_encoder_create_file(destination,comments.get(),j.rate,j.channels,0,&rc);
    require(j.encoder!=nullptr,std::string("Apertura encoder: ")+ope_strerror(rc));
#define CTL(call) require((rc=ope_encoder_ctl(j.encoder,call))==OPE_OK,std::string("Parametri Opus: ")+ope_strerror(rc))
    CTL(OPUS_SET_APPLICATION(OPUS_APPLICATION_AUDIO));
    CTL(OPUS_SET_BITRATE(bitrate));CTL(OPUS_SET_VBR(1));CTL(OPUS_SET_VBR_CONSTRAINT(0));CTL(OPUS_SET_COMPLEXITY(10));
    int actualRate=0,vbr=0,constrained=0,complexity=0;
    CTL(OPUS_GET_BITRATE(&actualRate));CTL(OPUS_GET_VBR(&vbr));CTL(OPUS_GET_VBR_CONSTRAINT(&constrained));CTL(OPUS_GET_COMPLEXITY(&complexity));
    require(actualRate==bitrate&&vbr==1&&constrained==0&&complexity==10,"Parametri effettivi Opus diversi dai richiesti");
    cb.log("*** 2. CODIFICA OPUS VBR ***\nParametri riletti dall’encoder: bitrate="+std::to_string(actualRate)+" bit/s · VBR="+std::to_string(vbr)+" · constrained="+std::to_string(constrained)+" · complexity="+std::to_string(complexity)+"\nOgg Opus standard a 48 kHz; libopusenc ricampiona quando necessario.\nDa qui: campioni elaborati/totali (per canale), secondi e percentuale. Aggiornamenti log limitati a uno ogni 5 secondi.");
    bool processed=FLAC__stream_decoder_process_until_end_of_stream(decoder.get());
    require(processed&&j.error.empty(),j.error.empty()?"Decodifica FLAC incompleta":j.error);
    require(j.done>0&&(j.total==0||j.done==j.total),"Numero campioni FLAC incompleto");
    require(FLAC__stream_decoder_finish(decoder.get()),"Controllo MD5 del PCM FLAC fallito");
    require(cb.progress(j.done,j.total,j.rate,j.channels),"Annullato");
    rc=ope_encoder_drain(j.encoder);require(rc==OPE_OK,std::string("Chiusura stream Opus: ")+ope_strerror(rc));
    ope_encoder_destroy(j.encoder);j.encoder=nullptr;
    cb.log("OK: drain e rilascio encoder · campioni="+std::to_string(j.done));
    return j.done*1000000ULL/j.rate;
}
