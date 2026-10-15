#include "hdr_encoder.h"
#include <x265.h>
#include <stdexcept>
#include <chrono>
struct HdrEncoder::State {
    x265_param* p=nullptr;x265_encoder* encoder=nullptr;x265_picture in{},out{};
    int w=0,h=0,submitted=0,produced=0;int64_t last=-1;bool finished=false;
    ~State(){if(encoder)x265_encoder_close(encoder);if(p)x265_param_free(p);}
    void emit(x265_nal* nals,uint32_t count,ProbeCallbacks& cb){
        std::vector<uint8_t> packet;bool key=false;
        for(uint32_t i=0;i<count;i++){packet.insert(packet.end(),nals[i].payload,nals[i].payload+nals[i].sizeBytes);if(nals[i].type>=16&&nals[i].type<=21)key=true;}
        cb.packet(packet,out.pts,key);produced++;
    }
};
HdrEncoder::HdrEncoder(int w,int h,int crf,int primaries,int transfer,int matrix,int flags,ProbeCallbacks& cb):state(new State){
    if(w<16||h<16||w>3840||h>3840||int64_t(w)*h>3840LL*2160||w%2||h%2||crf<0||crf>51||(transfer!=16&&transfer!=18))throw std::runtime_error("Parametri della prova HDR non supportati");
    cb.log("LIBRERIA HDR SEPARATA · x265="+std::string(x265_version_str)+" max_bit_depth="+std::to_string(x265_max_bit_depth)+" build="+x265_build_info_str);
    if(x265_max_bit_depth!=10)throw std::runtime_error("Isolamento librerie fallito: richiesta x265 a 10 bit");
    auto& s=*state;s.w=w;s.h=h;s.p=x265_param_alloc();
    if(!s.p||x265_param_default_preset(s.p,"ultrafast","zerolatency")<0)throw std::runtime_error("Configurazione x265 HDR fallita");
    auto p=s.p;p->sourceWidth=w;p->sourceHeight=h;p->fpsNum=30;p->fpsDenom=1;
    p->internalBitDepth=10;p->internalCsp=X265_CSP_I420;p->rc.rateControlMode=X265_RC_CRF;p->rc.rfConstant=crf;
    p->frameNumThreads=2;p->numaPools="2";p->bframes=0;p->lookaheadDepth=0;p->rc.cuTree=0;
    p->bAnnexB=1;p->bRepeatHeaders=0;p->bEmitInfoSEI=0;p->logLevel=X265_LOG_WARNING;
    p->vui.colorPrimaries=primaries;p->vui.transferCharacteristics=transfer;p->vui.matrixCoeffs=matrix;
    p->vui.bEnableVideoSignalTypePresentFlag=(flags&2)!=0;p->vui.bEnableColorDescriptionPresentFlag=(flags&4)!=0;p->vui.bEnableVideoFullRangeFlag=(flags&1)!=0;p->vui.videoFormat=(flags>>3)&7;
    if(x265_param_apply_profile(p,"main10")<0)throw std::runtime_error("Profilo Main10 rifiutato");
    s.encoder=x265_encoder_open(p);if(!s.encoder)throw std::runtime_error("Apertura encoder HDR fallita");
    x265_encoder_parameters(s.encoder,p);
    if(p->internalBitDepth!=10||p->rc.rateControlMode!=X265_RC_CRF)throw std::runtime_error("Encoder HDR non è a 10 bit CRF");
    cb.log("OK ENCODER HDR · Main10 · CPU · I420 16 bit per campione con valori 0–1023 · CRF="+std::to_string(p->rc.rfConstant)+" · ultrafast/zerolatency · fps nominali=30 · assembly disabilitato in questa prova");
    x265_picture_init(p,&s.in);x265_picture_init(p,&s.out);s.in.bitDepth=10;
    x265_nal* nals=nullptr;uint32_t count=0;if(x265_encoder_headers(s.encoder,&nals,&count)<0)throw std::runtime_error("Header HDR fallito");
    std::vector<uint8_t> headers;for(uint32_t i=0;i<count;i++)headers.insert(headers.end(),nals[i].payload,nals[i].payload+nals[i].sizeBytes);cb.headers(headers);
}
HdrEncoder::~HdrEncoder()=default;
void HdrEncoder::frame(const uint8_t* data,size_t size,int64_t pts,ProbeCallbacks& cb){
    auto& s=*state;if(s.finished||!data||size!=size_t(s.w)*s.h*3||pts<0||pts<=s.last)throw std::runtime_error("Buffer/timestamp HDR non valido");
    if(cb.cancelled())throw std::runtime_error("Prova HDR annullata");
    const uint16_t* pixels=reinterpret_cast<const uint16_t*>(data);for(size_t i=0;i<size/2;i++)if(pixels[i]>1023)throw std::runtime_error("Campione HDR oltre 10 bit: P010 non convertito correttamente");
    s.in.planes[0]=const_cast<uint8_t*>(data);s.in.planes[1]=const_cast<uint8_t*>(data)+s.w*s.h*2;s.in.planes[2]=const_cast<uint8_t*>(data)+s.w*s.h*5/2;
    s.in.stride[0]=s.w*2;s.in.stride[1]=s.w;s.in.stride[2]=s.w;s.in.pts=pts;s.last=pts;
    x265_nal* nals=nullptr;uint32_t count=0;auto start=std::chrono::steady_clock::now();
    int result=x265_encoder_encode(s.encoder,&nals,&count,&s.in,&s.out);
    cb.log("CODIFICA HDR · frame="+std::to_string(s.submitted+1)+" pts_us="+std::to_string(pts)+" risultato="+std::to_string(result)+" tempo_ms="+std::to_string(std::chrono::duration<double,std::milli>(std::chrono::steady_clock::now()-start).count()));
    if(result<0)throw std::runtime_error("Codifica HDR fallita");s.submitted++;if(result>0)s.emit(nals,count,cb);
}
void HdrEncoder::finish(ProbeCallbacks& cb){
    auto& s=*state;if(s.finished)throw std::runtime_error("Encoder HDR già finalizzato");
    for(int i=0;i<s.submitted+16;i++){
        if(cb.cancelled())throw std::runtime_error("Prova HDR annullata");x265_nal* nals=nullptr;uint32_t count=0;int result=x265_encoder_encode(s.encoder,&nals,&count,nullptr,&s.out);
        if(result<0)throw std::runtime_error("Flush HDR fallito");if(result==0){s.finished=true;break;}s.emit(nals,count,cb);
    }
    if(!s.finished||s.submitted==0||s.produced!=s.submitted)throw std::runtime_error("Conteggio HDR incoerente");
    cb.log("OK HDR · frames_in="+std::to_string(s.submitted)+" frames_out="+std::to_string(s.produced));
}
