#include "audio_stream.h"
#include <fcntl.h>
#include <fstream>
#include <iostream>
#include <unistd.h>
// Compressed fixtures are decoded independently by FFmpeg; this tests the exact new
// streaming JNI engine and TagLib path without pretending to test Android's decoder.
int main(int argc,char** argv){
    if(argc!=7)return 2;int fd=open(argv[1],O_RDONLY);if(fd<0)return 3;
    try{
        AudioStream stream(fd,std::stoi(argv[3]),argv[2],std::stoi(argv[4]),std::stoi(argv[5]),128000,[](const std::string& s){std::cout<<s<<std::endl;});
        std::ifstream pcm(argv[6],std::ios::binary);if(!pcm)throw std::runtime_error("PCM missing");
        float block[8192];
        while(pcm.read(reinterpret_cast<char*>(block),sizeof(block))||pcm.gcount()){
            auto bytes=pcm.gcount();if(bytes%sizeof(float))throw std::runtime_error("PCM truncated");stream.write(block,bytes/sizeof(float));
        }
        auto duration=stream.finish([](const std::string& s){std::cout<<s<<std::endl;});close(fd);std::cout<<"PCM_STREAM_OK duration_us="<<duration<<std::endl;return 0;
    }catch(const std::exception& e){close(fd);std::cerr<<e.what()<<std::endl;return 1;}
}
