#include "encoder.h"
#include <x265.h>
#include <chrono>
#include <memory>
#include <stdexcept>
#include <sstream>
#include <algorithm>
using Clock = std::chrono::steady_clock;
static double millis(Clock::time_point start) {
    return std::chrono::duration<double, std::milli>(Clock::now()-start).count();
}
static std::vector<uint8_t> collect(x265_nal* nals, uint32_t count) {
    std::vector<uint8_t> data;
    for (uint32_t i=0; i<count; ++i)
        data.insert(data.end(), nals[i].payload, nals[i].payload+nals[i].sizeBytes);
    return data;
}
void runProbe(ProbeCallbacks& cb, int crf) {
    constexpr int W=1920, H=1080, FPS=30, FRAMES=30;
    cb.log("*** VERIFICA COMPILAZIONE NATIVA ***");
#if defined(__aarch64__)
    const char* arch="aarch64";
#elif defined(__x86_64__)
    const char* arch="x86_64 (test su server)";
#else
    const char* arch="other";
#endif
#if defined(__OPTIMIZE__)
    const bool optimized=true;
#else
    const bool optimized=false;
#endif
    cb.log(std::string("BUILD arch=")+arch+"\n• pointer_bits="+std::to_string(sizeof(void*)*8)
        +"\n• configurazione="+PROBE_BUILD_TYPE+"\n• ottimizzazione="+PROBE_OPT_LEVEL
        +"\n• macro compilatore __OPTIMIZE__="+std::to_string(optimized));
    if(!optimized) throw std::runtime_error("Native optimization disabled");
#if defined(__ANDROID__)
    static_assert(sizeof(void*)==8, "Android probe must be 64 bit");
#if !defined(__aarch64__)
#error Android probe requires AArch64
#endif
#endif
    cb.log("NOTA: il vecchio build_info di x265 mostra 32 bit anche su ARM64 perché controlla solo X86_64. Fare riferimento a BUILD e PROCESSO.");
    cb.log("*** STEP 3 · CONFIGURAZIONE ENCODER X265 ***");
    cb.log("Preparo 1920x1080, 30 fps, colore I420 a 8 bit. Imposto CRF e controllo i parametri accettati.");
    cb.log("LIBRARY = versione/build\n• EFFECTIVE = impostazioni effettive\n• rcMode=2 indica CRF, rfConstant è il CRF, bframes indica i fotogrammi B.");
    cb.log(std::string("LIBRARY version=")+x265_version_str+" build="+x265_build_info_str);
    cb.log("CONFIG CPU software only; ARM assembly disabled in this diagnostic build");
    std::unique_ptr<x265_param, decltype(&x265_param_free)> param(x265_param_alloc(), x265_param_free);
    if (!param) throw std::runtime_error("param_alloc returned null");
    cb.log("BEGIN param_default_preset ultrafast / zerolatency");
    int rc=x265_param_default_preset(param.get(), "ultrafast", "zerolatency");
    if(rc<0) throw std::runtime_error("param_default_preset rc="+std::to_string(rc));
    param->sourceWidth=W; param->sourceHeight=H;
    param->fpsNum=FPS; param->fpsDenom=1; param->internalCsp=X265_CSP_I420;
    param->internalBitDepth=8; param->rc.rateControlMode=X265_RC_CRF;
    param->rc.rfConstant=crf; param->bAnnexB=1; param->bRepeatHeaders=0;
    param->bEmitInfoSEI=0; param->bframes=0; param->lookaheadDepth=0;
    param->frameNumThreads=2; param->numaPools="2";
    param->logLevel=X265_LOG_WARNING;
    cb.log("BEGIN apply_profile main");
    if(x265_param_apply_profile(param.get(),"main")<0) throw std::runtime_error("apply_profile failed");
    cb.log("BEGIN encoder_open 1920x1080 30fps I420/8bit CRF="+std::to_string(crf)+" threads=2 pool=2");
    auto openStart=Clock::now();
    std::unique_ptr<x265_encoder, decltype(&x265_encoder_close)> enc(x265_encoder_open(param.get()),x265_encoder_close);
    if(!enc) throw std::runtime_error("encoder_open returned null");
    cb.log("OK encoder_open elapsed_ms="+std::to_string(millis(openStart)));
    x265_encoder_parameters(enc.get(),param.get());
    cb.log("EFFECTIVE rcMode="+std::to_string(param->rc.rateControlMode)
        +" (CRF="+std::to_string(X265_RC_CRF)+") rfConstant="+std::to_string(param->rc.rfConstant)
        +" bframes="+std::to_string(param->bframes));
    if(param->rc.rateControlMode!=X265_RC_CRF) throw std::runtime_error("effective mode is not CRF");
    std::unique_ptr<x265_picture, decltype(&x265_picture_free)> in(x265_picture_alloc(),x265_picture_free);
    std::unique_ptr<x265_picture, decltype(&x265_picture_free)> out(x265_picture_alloc(),x265_picture_free);
    if(!in || !out) throw std::runtime_error("picture_alloc returned null");
    x265_picture_init(param.get(),in.get()); x265_picture_init(param.get(),out.get());
    std::vector<uint8_t> pixels(W*H*3/2,128);
    in->planes[0]=pixels.data(); in->planes[1]=pixels.data()+W*H;
    in->planes[2]=pixels.data()+W*H*5/4;
    in->stride[0]=W; in->stride[1]=W/2; in->stride[2]=W/2;
    x265_nal* nals=nullptr; uint32_t count=0;
    cb.log("BEGIN encoder_headers");
    if(x265_encoder_headers(enc.get(),&nals,&count)<0) throw std::runtime_error("encoder_headers failed");
    auto headers=collect(nals,count);
    cb.log("OK encoder_headers nals="+std::to_string(count)+" bytes="+std::to_string(headers.size()));
    cb.headers(headers);
    int encoded=0; uint64_t bytes=0;
    auto emit=[&]() {
        auto data=collect(nals,count);
        bool key=false;
        for(uint32_t j=0;j<count;++j) if(nals[j].type>=16 && nals[j].type<=21) key=true;
        cb.packet(data,out->pts,key);
        bytes+=data.size(); ++encoded;
        cb.log("PACKET index="+std::to_string(encoded)+" pts_us="+std::to_string(out->pts)
            +" bytes="+std::to_string(data.size())+" key="+std::to_string(key));
    };
    cb.log("*** STEP 5 · INIZIO SEQUENZA DI CODIFICA ***");
    cb.log("Genero e invio 30 fotogrammi sintetici. Scrivo i pacchetti prodotti nel file MP4.");
    cb.log("LEGENDA: frame = indice in ingresso da 0 a 29\n• pts_us = posizione nel video in microsecondi (1000000 = 1 secondo).");
    cb.log("LEGENDA: BEGIN = prima della chiamata\n• END = chiamata terminata\n• elapsed_ms = durata della sola chiamata x265 in millisecondi (escluso log).");
    cb.log("LEGENDA: rc < 0 errore\n• rc = 0 nessun fotogramma ancora pronto\n• rc > 0 fotogrammi prodotti\n• nals = blocchi HEVC restituiti.");
    cb.log("LEGENDA PACKET: index/packet = progressivo da 1\n• bytes = dimensione compressa\n• key=1 = fotogramma chiave\n• key=0 = altro fotogramma.");
    cb.log("NOTA: ingresso e uscita possono essere sfalsati. rc=0 iniziale non indica un errore.");
    double codecMs=0;
    auto encodingStart=Clock::now();
    for(int frame=0;frame<FRAMES;++frame) {
        if(cb.cancelled()) throw std::runtime_error("CANCELLED before frame "+std::to_string(frame));
        // Deterministic gradient/checker pattern with a moving square, identical for both CRFs.
        for(int y=0;y<H;++y) for(int x=0;x<W;++x) {
            int value=16+(x*150/W)+(((x/16+y/16)%2)*35);
            int left=frame*35;
            if(x>=left && x<left+220 && y>H/3 && y<H/3+220) value=230;
            pixels[y*W+x]=static_cast<uint8_t>(value);
        }
        in->pts=static_cast<int64_t>(frame)*1000000/FPS;
        cb.log("BEGIN encode frame="+std::to_string(frame)+" pts_us="+std::to_string(in->pts));
        auto tick=Clock::now();
        rc=x265_encoder_encode(enc.get(),&nals,&count,in.get(),out.get());
        double callMs=millis(tick);
        codecMs+=callMs;
        cb.log("END encode frame="+std::to_string(frame)+" rc="+std::to_string(rc)
            +" nals="+std::to_string(count)+" elapsed_ms="+std::to_string(callMs));
        if(rc<0) throw std::runtime_error("encoder_encode failed frame="+std::to_string(frame));
        if(rc>0) emit();
    }
    cb.log("*** STEP 6 · RECUPERO FOTOGRAMMI IN ATTESA ***");
    cb.log("Invio un ingresso vuoto a x265 finché restituisce rc=0. FLUSH usa gli stessi rc e nals della codifica.");
    cb.log("BEGIN flush");
    for(int tries=0;tries<FRAMES+10;++tries) {
        if(cb.cancelled()) throw std::runtime_error("CANCELLED during flush");
        cb.log("BEGIN flush iteration="+std::to_string(tries));
        auto tick=Clock::now();
        rc=x265_encoder_encode(enc.get(),&nals,&count,nullptr,out.get());
        codecMs+=millis(tick);
        cb.log("FLUSH rc="+std::to_string(rc)+" nals="+std::to_string(count));
        if(rc<0) throw std::runtime_error("flush failed");
        if(rc==0) break;
        emit();
    }
    if(encoded!=FRAMES) throw std::runtime_error("frame count mismatch encoded="+std::to_string(encoded));
    double duration=millis(encodingStart);
    cb.log("*** RIEPILOGO CODIFICA ***");
    cb.log("SUMMARY: frames = totale prodotto\n• bytes = dati compressi senza contenitore\n• encode_ms = durata ciclo inclusi generazione, scrittura e log\n• fps = velocità di elaborazione, non fps del video.");
    cb.log("SUMMARY frames="+std::to_string(encoded)+" bytes="+std::to_string(bytes)
        +" encode_ms="+std::to_string(duration)+" fps="+std::to_string(encoded*1000.0/duration));
    cb.log("TEMPI: codec_ms = somma delle chiamate x265, incluso flush\n• altro_ms = tempo restante del ciclo (generazione, scrittura e log)\n• confronto_v03 = vecchio tempo / tempo attuale; maggiore di 1 indica un test più veloce.");
    cb.log("TIMING codec_ms="+std::to_string(codecMs)+" altro_ms="+std::to_string(std::max(0.0,duration-codecMs)));
#if defined(__ANDROID__)
    double previousMs=crf==24 ? 11129.941871 : 13663.755568;
    cb.log("CONFRONTO v0.3 SM-S916B crf="+std::to_string(crf)+" precedente_ms="+std::to_string(previousMs)
        +" attuale_ms="+std::to_string(duration)+" rapporto="+std::to_string(previousMs/duration));
    cb.log("Confronto indicativo con il log precedente: temperatura, carico del telefono e log possono influenzarlo. Non è una misura di qualità.");
#endif
    cb.log("BEGIN encoder_close");
    enc.reset(); cb.log("OK encoder_close");
}
