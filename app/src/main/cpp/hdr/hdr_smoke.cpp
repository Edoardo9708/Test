#include "hdr_encoder.h"
#include <fstream>
#include <iostream>
int main(){try{
    for(int transfer:{16,18}){
        std::ofstream file("hdr10-"+std::to_string(transfer)+".hevc",std::ios::binary);int packets=0;ProbeCallbacks cb;
        cb.log=[](const std::string& s){std::cout<<s<<std::endl;};cb.cancelled=[](){return false;};
        cb.headers=[&](const std::vector<uint8_t>& b){file.write(reinterpret_cast<const char*>(b.data()),b.size());};
        cb.packet=[&](const std::vector<uint8_t>& b,int64_t pts,bool){if(pts!=packets*33333LL)throw std::runtime_error("PTS HDR alterati");cb.headers(b);packets++;};
        HdrEncoder encoder(320,180,23,9,transfer,9,46,cb);std::vector<uint16_t> pixels(320*180*3/2,512);
        for(int f=0;f<6;f++){for(int y=0;y<180;y++)for(int x=0;x<320;x++)pixels[y*320+x]=64+(x*3+y+f*7)%877;encoder.frame(reinterpret_cast<const uint8_t*>(pixels.data()),pixels.size()*2,f*33333LL,cb);}
        encoder.finish(cb);if(packets!=6||!file)throw std::runtime_error("Prova HDR incompleta");
    }
}catch(const std::exception& e){std::cerr<<e.what()<<std::endl;return 1;}}
