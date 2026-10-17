#include "audio_encoder.h"
#include <fcntl.h>
#include <iostream>
#include <unistd.h>
int main(int argc,char** argv){
    if(argc!=4)return 2;
    int fd=open(argv[1],O_RDONLY);if(fd<0)return 3;
    bool cancel=std::string(argv[3])=="cancel";
    try{
        auto duration=encodeFlacOpus(fd,argv[2],128000,{[&](uint64_t n,uint64_t,unsigned,unsigned){return !cancel||n==0;},[](const std::string& text){std::cout<<text<<std::endl;}});
        close(fd);std::cout<<"DURATION_US="<<duration<<std::endl;return cancel?4:0;
    }catch(const std::exception& error){close(fd);std::cerr<<error.what()<<std::endl;return cancel&&std::string(error.what())=="Annullato"?0:1;}
}
