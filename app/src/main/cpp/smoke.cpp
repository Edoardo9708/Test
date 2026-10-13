#include "encoder.h"
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
    } catch(const std::exception& e) {std::cerr<<e.what()<<std::endl; return 1;}
}
