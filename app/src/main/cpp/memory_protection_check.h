#pragma once
#include "memory_protection.h"
#include <x265.h>
#include <stdexcept>
#include <string>

// Exercise real preset defaults in both separately linked x265 bit depths.
inline void checkMemoryProtection() {
    const char* presets[]={"ultrafast","superfast","veryfast","faster","fast","medium","slow","slower","veryslow"};
    for(const char* preset:presets) {
        x265_param* p=x265_param_alloc();
        if(!p)throw std::runtime_error("Cannot allocate protection test parameters");
        if(x265_param_default_preset(p,preset,nullptr)<0){x265_param_free(p);throw std::runtime_error("Invalid test preset");}
        p->rc.rfConstant=23;
        int aq=p->rc.aqMode;double strength=p->rc.aqStrength;
        p->frameNumThreads=6;p->numaPools="8";p->bframes=0;
        bool ok=MemoryProtection::apply(p,3840,2160)&&p->frameNumThreads==2&&std::string(p->numaPools)=="2"
            &&p->bframes==4&&p->lookaheadDepth==5&&p->bFrameAdaptive==0&&p->rc.cuTree==0
            &&p->rc.rfConstant==23&&p->rc.aqMode==aq&&p->rc.aqStrength==strength;
        x265_param_free(p);
        if(!ok)throw std::runtime_error("UHD protection failed across presets");
    }
    if(!MemoryProtection::required(2160,3840)||!MemoryProtection::required(3840,1600)
       ||MemoryProtection::required(1920,1080)||MemoryProtection::required(1080,1920)
       ||MemoryProtection::required(2560,1440))throw std::runtime_error("UHD detection failed");
    x265_param p{};p.frameNumThreads=6;p.bframes=8;p.lookaheadDepth=40;p.rc.cuTree=1;
    if(MemoryProtection::apply(&p,1920,1080)||p.frameNumThreads!=6||p.bframes!=8||p.lookaheadDepth!=40||p.rc.cuTree!=1)
        throw std::runtime_error("Non-UHD settings were changed");
}
