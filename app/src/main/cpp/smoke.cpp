#include "encoder.h"
#include "stream_encoder.h"
#include <fstream>
#include <iostream>
int main() {
    try {
        for(int crf : {24,32}) {
            std::ofstream file("smoke-"+std::to_string(crf)+".hevc",std::ios::binary);
            ProbeCallbacks cb;
            cb.log=[](const std::string& s){std::cout<<s<<std::endl;};
            cb.cancelled=[](){return false;};
            cb.headers=[&](const std::vector<uint8_t>& data){file.write(reinterpret_cast<const char*>(data.data()),data.size());};
            cb.packet=[&](const std::vector<uint8_t>& data,int64_t,bool){cb.headers(data);};
            runProbe(cb,crf);
            if(!file) throw std::runtime_error("write failure");
        }
        // The real-video encoder API: non-synthetic caller-owned I420 buffers,
        // variable timestamps and both CRF endpoints exposed in the menu.
        for(int crf : {0,23,51}) {
            std::ofstream file("stream-"+std::to_string(crf)+".hevc",std::ios::binary);
            ProbeCallbacks cb;
            cb.log=[](const std::string& s){std::cout<<s<<std::endl;};
            cb.cancelled=[](){return false;};
            cb.headers=[&](const std::vector<uint8_t>& data){file.write(reinterpret_cast<const char*>(data.data()),data.size());};
            int packets=0;int64_t last=-1;
            cb.packet=[&](const std::vector<uint8_t>& data,int64_t pts,bool){
                if(pts<=last)throw std::runtime_error("Streaming PTS order");
                last=pts;packets++;cb.headers(data);
            };
            StreamEncoder encoder(StreamConfig{320,240,30000,crf,1,3,2,crf==0?0:(crf==23?6:8)},cb);
            std::vector<uint8_t> pixels(320*240*3/2,128);
            int64_t pts=10000;
            for(int f=0;f<12;f++) {
                for(int y=0;y<240;y++) for(int x=0;x<320;x++) pixels[y*320+x]=16+(x+y+f*7)%210;
                encoder.frame(pixels.data(),pixels.size(),pts,cb);
                pts+=f%2?40000:26666;
            }
            encoder.finish(cb);
            if(packets!=12||!file)throw std::runtime_error("Streaming output mismatch");
        }
    } catch(const std::exception& e) {std::cerr<<e.what()<<std::endl; return 1;}
}
