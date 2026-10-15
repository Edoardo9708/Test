#include "hdr_encoder.h"
#include <fstream>
#include <iostream>
#include <set>
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
    {
        std::ofstream file("hdr10-bframes.hevc",std::ios::binary),index("hdr10-bframes.index");std::set<int64_t> ptsSeen;int packets=0;bool reordered=false;int64_t previous=-1;
        ProbeCallbacks cb;cb.log=[](const std::string& s){std::cout<<s<<std::endl;};cb.cancelled=[](){return false;};
        cb.headers=[&](const std::vector<uint8_t>& b){index<<"H "<<b.size()<<"\n";file.write(reinterpret_cast<const char*>(b.data()),b.size());};
        cb.packet=[&](const std::vector<uint8_t>& b,int64_t pts,bool){if(!ptsSeen.insert(pts).second||pts%41708||pts<0||pts>=72*41708LL)throw std::runtime_error("PTS Main10 ripetuti/alterati");if(pts<previous)reordered=true;previous=pts;index<<pts<<" "<<b.size()<<"\n";file.write(reinterpret_cast<const char*>(b.data()),b.size());packets++;};
        StreamConfig config{320,180,23976,23,6,6,2};config.preset=2;config.cpuMode=0;config.bframes=true;config.lookahead=true;config.colorPrimaries=9;config.colorTransfer=16;config.colorMatrix=9;config.colorFlags=46;
        HdrEncoder encoder(config,cb);auto settings=encoder.effectiveSettings();if(settings[0]<1||settings[1]<1)throw std::runtime_error("B-frame/lookahead Main10 non attivi");
        std::vector<uint16_t> pixels(320*180*3/2,512);for(int f=0;f<72;f++){for(int y=0;y<180;y++)for(int x=0;x<320;x++)pixels[y*320+x]=64+(x*3+y+f*7)%877;encoder.frame(reinterpret_cast<const uint8_t*>(pixels.data()),pixels.size()*2,f*41708LL,cb);}encoder.finish(cb);
        if(packets!=72||!reordered||!file||!index)throw std::runtime_error("Prova riordinamento Main10 incompleta");
        std::cout<<"OK HDR full settings: 72 frames, 23.976 fps, B-frame reordering, lookahead, effective AQ/CU-tree"<<std::endl;
    }
}catch(const std::exception& e){std::cerr<<e.what()<<std::endl;return 1;}}

