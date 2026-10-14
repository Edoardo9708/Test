#include "encoder.h"
#include "stream_encoder.h"
#include <fstream>
#include <iostream>
#include <set>
int main() {
    try {
        for(int preset=0;preset<9;preset++)for(bool b:{false,true})for(bool look:{false,true}){
            auto settings=StreamEncoder::presetSettings(preset,b,look);
            if(settings[3]<0||settings[3]>4||settings[4]<0||settings[4]>3000||settings[0]<0||settings[1]<0||(!b&&settings[0]!=0)||(!look&&(settings[1]!=(b?settings[0]+1:0)||settings[2]!=0)))throw std::runtime_error("Preset settings query disagrees with temporal switches");
            std::cout<<"PRESET_VALUES preset="<<preset<<" B="<<b<<" look="<<look<<" bframes="<<settings[0]<<" lookahead="<<settings[1]<<" cutree="<<settings[2]<<" aq="<<settings[3]<<" strength_milli="<<settings[4]<<std::endl;
        }
        bool rejected=false;try{StreamEncoder::presetSettings(9,true,true);}catch(const std::exception&){rejected=true;}if(!rejected)throw std::runtime_error("Invalid preset query accepted");
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
            StreamEncoder encoder(StreamConfig{320,240,30000,crf,1,3,2,crf==0?0:(crf==23?6:8),crf==0?0:(crf==23?1:2),4,false,false,true,crf==23?6:1,crf==0?1:(crf==23?6:8),crf==23?6:1,crf==23?47:46},cb);
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
        for(int b=0;b<2;b++) for(int look=0;look<2;look++) {
            std::ofstream file("reorder-"+std::to_string(b)+"-"+std::to_string(look)+".hevc",std::ios::binary);
            ProbeCallbacks cb;std::set<int64_t> inputPts,outputPts;int packets=0;
            cb.log=[](const std::string& s){std::cout<<s<<std::endl;};cb.cancelled=[](){return false;};
            cb.headers=[&](const std::vector<uint8_t>& data){file.write(reinterpret_cast<const char*>(data.data()),data.size());};
            cb.packet=[&](const std::vector<uint8_t>& data,int64_t pts,bool){if(!outputPts.insert(pts).second)throw std::runtime_error("Duplicate output PTS");packets++;cb.headers(data);};
            StreamEncoder encoder(StreamConfig{320,240,30000,23,1,3,2,2,2,4,b!=0,look!=0},cb);
            auto settings=encoder.effectiveSettings();
            if((!b&&settings[0]!=0)||(!look&&settings[2]!=0)||settings[1]<0)throw std::runtime_error("Effective encoder settings invalid");
            std::cout<<"EFFECTIVE_VALUES bframes="<<settings[0]<<" lookahead="<<settings[1]<<" cutree="<<settings[2]<<" aq="<<settings[3]<<" strength_milli="<<settings[4]<<std::endl;
            std::vector<uint8_t> pixels(320*240*3/2,128);int64_t pts=10000;
            for(int f=0;f<24;f++) {
                inputPts.insert(pts);
                for(int y=0;y<240;y++)for(int x=0;x<320;x++)pixels[y*320+x]=16+(x+y+f)%210;
                encoder.frame(pixels.data(),pixels.size(),pts,cb);pts+=f%2?40000:26666;
            }
            encoder.finish(cb);
            if(packets!=24||inputPts!=outputPts||!file)throw std::runtime_error("Reordered timestamp mismatch");
        }
    } catch(const std::exception& e) {std::cerr<<e.what()<<std::endl; return 1;}
}


