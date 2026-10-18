#include "hevc_decoder.h"
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <vector>
#include <algorithm>
#include <cstring>
#include <string>
template<class T>T read(std::istream& in){T value;in.read(reinterpret_cast<char*>(&value),sizeof(value));if(!in)throw std::runtime_error("Truncated fixture");return value;}
int main(int argc,char** argv){
 try{
    if(argc!=4)throw std::runtime_error("fixture output transfer");
    std::ifstream input(argv[1],std::ios::binary);auto size=read<uint32_t>(input);std::vector<uint8_t> csd(size);input.read(reinterpret_cast<char*>(csd.data()),size);
    int transfer=std::stoi(argv[3]);HevcDecoder decoder(csd.data(),csd.size(),64,48,9,transfer,9,false);
    std::vector<uint8_t> pixels(64*48*3);std::vector<int64_t> expected,actual;int64_t totalPrecise=0;std::ofstream output(argv[2],std::ios::binary);
    auto drain=[&](){int status;int64_t pts,precise;while((status=decoder.receive(pixels.data(),pixels.size(),pts,precise))==1){actual.push_back(pts);totalPrecise+=precise;output.write(reinterpret_cast<char*>(pixels.data()),pixels.size());}return status;};
    while(input.peek()!=EOF){int64_t pts=read<int64_t>(input);size=read<uint32_t>(input);std::vector<uint8_t> packet(size);input.read(reinterpret_cast<char*>(packet.data()),size);expected.push_back(pts);decoder.send(packet.data(),size,pts);if(drain()!=0)throw std::runtime_error("Premature EOF");}
    decoder.send(nullptr,0,0);if(drain()!=2)throw std::runtime_error("Missing flush EOF");
    std::sort(expected.begin(),expected.end());if(actual!=expected||actual.size()!=18||!totalPrecise)throw std::runtime_error("Timestamp/frame/10-bit mismatch");
    if(std::is_sorted(expected.begin(),expected.end())&&actual.back()!=885000)throw std::runtime_error("VFR timestamps changed");
    // Invalid dimensions and mismatched color must fail, never silently convert/downcast.
    bool rejected=false;try{HevcDecoder invalid(csd.data(),csd.size(),63,48,9,transfer,9,false);}catch(const std::exception&){rejected=true;}if(!rejected)throw std::runtime_error("Odd dimensions accepted");
    std::cout<<"OK HEVC software: 18 VFR/B-frames, exact PTS, flush, "<<totalPrecise<<" precise 10-bit samples\n";
 }catch(const std::exception& e){std::cerr<<e.what()<<'\n';return 1;}
}
