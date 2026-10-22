#include "image_jumbf.h"
#include <cstring>
#include <functional>
#include <stdexcept>
#include <string>
namespace photo {
namespace {
using Bytes=std::vector<uint8_t>;
[[noreturn]] void bad(const char* message){throw std::runtime_error(message);}
uint32_t be(const uint8_t* p){return uint32_t(p[0])<<24|uint32_t(p[1])<<16|uint32_t(p[2])<<8|p[3];}
}
void validateC2pa(const std::vector<std::vector<uint8_t>>& packets){
    if(packets.empty())return;
    Bytes data;uint16_t instance=0;uint32_t sequence=1;
    for(const auto& p:packets){
        if(p.size()<16||p[0]!='J'||p[1]!='P'||be(p.data()+4)!=sequence)bad("APP11/JUMBF: sequenza di pacchetti non valida");
        uint16_t id=uint16_t(p[2])<<8|p[3];
        if(sequence==1){instance=id;data.assign(p.begin()+8,p.end());}
        else{
            if(id!=instance||memcmp(p.data()+8,data.data(),8))bad("APP11/JUMBF: istanze o header discordanti");
            data.insert(data.end(),p.begin()+16,p.end());
        }
        if(data.size()>16*1024*1024)bad("JUMBF oltre 16 MiB");
        sequence++;
    }
    if(data.size()<38||be(data.data())!=data.size()||memcmp(data.data()+4,"jumb",4))bad("APP11/JUMBF: contenitore incompleto o non supportato");
    size_t descriptor=8;uint32_t size=be(data.data()+descriptor);
    const uint8_t uuid[16]={'c','2','p','a',0,0x11,0,0x10,0x80,0,0,0xaa,0,0x38,0x9b,0x71};
    if(size<30||size>data.size()-descriptor||memcmp(data.data()+descriptor+4,"jumd",4)||
       memcmp(data.data()+descriptor+8,uuid,16)||(data[descriptor+24]&2)==0||
       memcmp(data.data()+descriptor+25,"c2pa",5))bad("APP11/JUMBF diverso da un archivio C2PA riconosciuto");
    size_t boxes=0;
    std::function<void(size_t,size_t,int)> validate=[&](size_t first,size_t end,int depth){
        if(depth>32)bad("JUMBF troppo annidato");
        size_t at=first;
        while(at<end){
            if(end-at<8||++boxes>65536)bad("JUMBF troncato o troppi box");
            uint32_t n=be(data.data()+at);
            if(n<8||n>end-at)bad("Dimensione box JUMBF non valida");
            if(!memcmp(data.data()+at+4,"jumb",4)){
                if(n<33||memcmp(data.data()+at+12,"jumd",4))bad("Superbox JUMBF senza descrittore");
                validate(at+8,at+n,depth+1);
            }
            at+=n;
        }
    };
    validate(8,data.size(),0);
}
}
