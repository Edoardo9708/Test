#include "stream_encoder.h"
#include <x265.h>
#include <stdexcept>
#include <chrono>
#include <algorithm>
struct StreamEncoder::State {
    x265_param* p=nullptr;
    x265_encoder* encoder=nullptr;
    x265_picture input{}, output{};
    int width=0,height=0,submitted=0,produced=0;
    int64_t lastPts=-1;
    bool finished=false;
    std::string pool;
    ~State() { if(encoder) x265_encoder_close(encoder); if(p) x265_param_free(p); }
    void emit(x265_nal* nals,uint32_t count,ProbeCallbacks& cb) {
        std::vector<uint8_t> data;
        bool key=false;
        for(uint32_t i=0;i<count;i++) {
            data.insert(data.end(),nals[i].payload,nals[i].payload+nals[i].sizeBytes);
            if(nals[i].type>=16 && nals[i].type<=21) key=true;
        }
        cb.log("PACCHETTO: pts_us="+std::to_string(output.pts)+" dts_us="+std::to_string(output.dts)+" slice_type="+std::to_string(output.sliceType)+" (B="+std::to_string(X265_TYPE_B)+")");
        cb.packet(data,output.pts,key);
        produced++;
    }
};
static const char* presetNames[]={"ultrafast","superfast","veryfast","faster","fast","medium","slow","slower","veryslow"};
static void temporalSettings(x265_param* p,bool bframes,bool lookahead) {
    if(!bframes)p->bframes=0;
    if(!lookahead){p->lookaheadDepth=bframes?p->bframes+1:0;p->rc.cuTree=0;if(bframes)p->bFrameAdaptive=0;}
}
std::array<int,3> StreamEncoder::presetSettings(int preset,bool bframes,bool lookahead) {
    if(preset<0||preset>8)throw std::runtime_error("Preset non valido");
    x265_param* p=x265_param_alloc();if(!p)throw std::runtime_error("Memoria x265 insufficiente");
    if(x265_param_default_preset(p,presetNames[preset],nullptr)<0){x265_param_free(p);throw std::runtime_error("Preset x265 non disponibile");}
    temporalSettings(p,bframes,lookahead);
    std::array<int,3> result={p->bframes,p->lookaheadDepth,p->rc.cuTree};x265_param_free(p);return result;
}
std::array<int,3> StreamEncoder::effectiveSettings() const {
    auto p=state->p;return {p->bframes,p->lookaheadDepth,p->rc.cuTree};
}
StreamEncoder::StreamEncoder(const StreamConfig& c,ProbeCallbacks& cb):state(new State) {
    if(c.width<16 || c.height<16 || c.width>3840 || c.height>3840 ||
       int64_t(c.width)*c.height>3840LL*2160 || c.width%2 || c.height%2 ||
       c.fpsMilli<1000 || c.fpsMilli>240000 || c.crf<0 || c.crf>51)
        throw std::runtime_error("Formato o CRF fuori dai limiti supportati");
    const auto& presets=presetNames;
    if(c.preset<0 || c.preset>8) throw std::runtime_error("Preset non valido");
    auto& s=*state;
    s.width=c.width; s.height=c.height;
    cb.log(std::string("BEGIN x265_param_alloc / preset ")+presets[c.preset]+" / preset per conversione file");
    s.p=x265_param_alloc();
    if(!s.p || x265_param_default_preset(s.p,presets[c.preset],nullptr)<0)
        throw std::runtime_error("Configurazione x265 fallita");
    auto p=s.p;
    p->sourceWidth=c.width; p->sourceHeight=c.height;
    p->fpsNum=c.fpsMilli; p->fpsDenom=1000;
    p->internalCsp=X265_CSP_I420; p->internalBitDepth=8;
    p->rc.rateControlMode=X265_RC_CRF; p->rc.rfConstant=c.crf;
    temporalSettings(p,c.bframes,c.lookahead);
    cb.log("OTTIMIZZAZIONI richieste: B-frame="+std::to_string(c.bframes)+" lookahead_esteso="+std::to_string(c.lookahead));
    if(c.cpuMode<0 || c.cpuMode>2 || c.cores<1 || c.cores>256) throw std::runtime_error("Configurazione CPU non valida");
    if(c.cpuMode==0) {p->frameNumThreads=2; s.pool="2";p->numaPools=s.pool.c_str();}
    else if(c.cpuMode==1) {p->frameNumThreads=0;p->numaPools=nullptr;}
    else {p->frameNumThreads=std::min(6,c.cores);s.pool=std::to_string(std::min(64,c.cores));p->numaPools=s.pool.c_str();}
    cb.log("CPU richiesta: modalita="+std::to_string(c.cpuMode)+" core_disponibili="+std::to_string(c.cores)+" frame_threads="+std::to_string(p->frameNumThreads)+" pool="+(p->numaPools?std::string(p->numaPools):"automatico"));
    p->bAnnexB=1; p->bRepeatHeaders=0; p->bEmitInfoSEI=0; p->logLevel=X265_LOG_WARNING;
    p->vui.bEnableVideoSignalTypePresentFlag=1;
    p->vui.bEnableColorDescriptionPresentFlag=1;
    p->vui.bEnableVideoFullRangeFlag=c.range==1;
    p->vui.colorPrimaries=c.standard==6?9:c.standard==2?5:c.standard==4?6:1;
    p->vui.matrixCoeffs=c.standard==6?9:c.standard==2?5:c.standard==4?6:1;
    p->vui.transferCharacteristics=c.transfer==1?8:(c.standard==1?1:6);
    if(c.exactColor){
        p->vui.bEnableVideoSignalTypePresentFlag=(c.colorFlags&2)!=0;
        p->vui.bEnableColorDescriptionPresentFlag=(c.colorFlags&4)!=0;
        p->vui.bEnableVideoFullRangeFlag=(c.colorFlags&1)!=0;
        p->vui.videoFormat=(c.colorFlags>>3)&7;
        p->vui.colorPrimaries=c.colorPrimaries;
        p->vui.transferCharacteristics=c.colorTransfer;
        p->vui.matrixCoeffs=c.colorMatrix;
    }
    cb.log("COLORE x265 richiesto: primarie="+std::to_string(p->vui.colorPrimaries)+" transfer="+std::to_string(p->vui.transferCharacteristics)+" matrice="+std::to_string(p->vui.matrixCoeffs)+" full_range="+std::to_string(p->vui.bEnableVideoFullRangeFlag));
    if(x265_param_apply_profile(p,"main")<0) throw std::runtime_error("Profilo Main rifiutato");
    cb.log("BEGIN x265_encoder_open · CPU, I420/8bit, CRF="+std::to_string(c.crf));
    s.encoder=x265_encoder_open(p);
    if(!s.encoder) throw std::runtime_error("Apertura x265 fallita");
    x265_encoder_parameters(s.encoder,p);
    cb.log("OK x265_encoder_open · pointer_bits="+std::to_string(sizeof(void*)*8)
           +" cpuid="+std::to_string(p->cpuid)+" CRF="+std::to_string(p->rc.rfConstant)
           +" rcMode="+std::to_string(p->rc.rateControlMode)+" (CRF="+std::to_string(X265_RC_CRF)+") preset="+presets[c.preset]+" frame_threads_effettivi="+std::to_string(p->frameNumThreads)
           +" bframes="+std::to_string(p->bframes)+" lookahead="+std::to_string(p->lookaheadDepth)+" cutree="+std::to_string(p->rc.cuTree));
    if(p->rc.rateControlMode!=X265_RC_CRF) throw std::runtime_error("CRF non attivo");
    x265_picture_init(p,&s.input); x265_picture_init(p,&s.output);
    x265_nal* nals=nullptr; uint32_t count=0;
    if(x265_encoder_headers(s.encoder,&nals,&count)<0) throw std::runtime_error("Intestazioni HEVC fallite");
    std::vector<uint8_t> data;
    for(uint32_t i=0;i<count;i++) data.insert(data.end(),nals[i].payload,nals[i].payload+nals[i].sizeBytes);
    cb.headers(data);
}
StreamEncoder::~StreamEncoder()=default;
void StreamEncoder::frame(const uint8_t* pixels,size_t length,int64_t pts,ProbeCallbacks& cb) {
    auto& s=*state;
    if(s.finished || !pixels || length!=size_t(s.width)*s.height*3/2 || pts<0 || pts<=s.lastPts)
        throw std::runtime_error("Fotogramma o timestamp non valido");
    if(cb.cancelled()) throw std::runtime_error("Conversione annullata");
    s.input.planes[0]=const_cast<uint8_t*>(pixels);
    s.input.planes[1]=const_cast<uint8_t*>(pixels)+s.width*s.height;
    s.input.planes[2]=const_cast<uint8_t*>(pixels)+s.width*s.height*5/4;
    s.input.stride[0]=s.width; s.input.stride[1]=s.width/2; s.input.stride[2]=s.width/2;
    s.input.pts=pts; s.lastPts=pts;
    x265_nal* nals=nullptr; uint32_t count=0;
    cb.log("BEGIN x265_encoder_encode frame="+std::to_string(s.submitted)+" pts_us="+std::to_string(pts));
    auto start=std::chrono::steady_clock::now();
    int rc=x265_encoder_encode(s.encoder,&nals,&count,&s.input,&s.output);
    auto ms=std::chrono::duration<double,std::milli>(std::chrono::steady_clock::now()-start).count();
    cb.log("END x265_encoder_encode rc="+std::to_string(rc)+" nals="+std::to_string(count)+" elapsed_ms="+std::to_string(ms));
    if(rc<0) throw std::runtime_error("Errore codifica x265");
    s.submitted++;
    if(rc>0) s.emit(nals,count,cb);
}
void StreamEncoder::finish(ProbeCallbacks& cb) {
    auto& s=*state;
    if(s.finished) throw std::runtime_error("Encoder già finalizzato");
    cb.log("*** RECUPERO FOTOGRAMMI IN ATTESA (FLUSH) ***");
    for(int i=0;i<s.submitted+16;i++) {
        if(cb.cancelled()) throw std::runtime_error("Conversione annullata");
        x265_nal* nals=nullptr; uint32_t count=0;
        int rc=x265_encoder_encode(s.encoder,&nals,&count,nullptr,&s.output);
        cb.log("FLUSH rc="+std::to_string(rc)+" nals="+std::to_string(count));
        if(rc<0) throw std::runtime_error("Errore flush x265");
        if(rc==0) { s.finished=true; break; }
        s.emit(nals,count,cb);
    }
    if(!s.finished || s.produced!=s.submitted || !s.produced) throw std::runtime_error("Conteggio fotogrammi x265 incoerente");
    cb.log("OK x265 frames_in="+std::to_string(s.submitted)+" frames_out="+std::to_string(s.produced));
}

