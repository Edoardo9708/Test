#include "image_motion.h"
#include <algorithm>
#include <array>
#include <cstring>
#include <fstream>
#include <functional>
#include <stdexcept>
#include <utility>
namespace photo {
namespace {
using Bytes=std::vector<uint8_t>;
[[noreturn]] void bad(const std::string& text){throw std::runtime_error(text);}
uint64_t be(const uint8_t* p,int n){uint64_t value=0;while(n--)value=value<<8|*p++;return value;}
struct Box {size_t start,payload,end;std::string type;};
struct Boxes {
    const uint8_t* data;size_t total,count=0;
    std::vector<Box> read(size_t start,size_t end){
        std::vector<Box> result;
        while(start<end){
            if(end-start<8||++count>65536)bad("Box MP4 troncato o troppi box");
            size_t header=8;uint64_t size=be(data+start,4);
            if(size==1){if(end-start<16)bad("Header MP4 esteso troncato");size=be(data+start+8,8);header=16;}
            else if(size==0)size=end-start;
            if(size<header||size>end-start)bad("Dimensione box MP4 fuori limite");
            result.push_back({start,start+header,start+size,std::string(reinterpret_cast<const char*>(data+start+4),4)});
            start+=size;
        }
        return result;
    }
};
struct FileBox {uint64_t start,payload,end;std::string type;};
std::vector<FileBox> fileBoxes(const std::string& path){
    std::ifstream file(path,std::ios::binary|std::ios::ate);if(!file)bad("HEIC non leggibile per verifica video");
    auto measured=file.tellg();if(measured<=0||uint64_t(measured)>128*1024*1024)bad("Dimensione HEIC fuori limite");
    uint64_t end=uint64_t(measured),at=0;std::vector<FileBox> boxes;
    while(at<end){
        std::array<uint8_t,16> header{};if(end-at<8||boxes.size()>=65536)bad("Box HEIC non valido");
        file.seekg(at);if(!file.read(reinterpret_cast<char*>(header.data()),8))bad("Header HEIC non leggibile");
        uint64_t size=be(header.data(),4),length=8;
        if(size==1){if(end-at<16||!file.read(reinterpret_cast<char*>(header.data()+8),8))bad("Header HEIC esteso troncato");size=be(header.data()+8,8);length=16;}
        // A size-zero box would swallow an appended video. Never append behind it.
        if(size<length||size>end-at)bad("Dimensione box HEIC non valida");
        boxes.push_back({at,at+length,at+size,std::string(reinterpret_cast<char*>(header.data()+4),4)});at+=size;
    }
    return boxes;
}
}
void validateMotionMp4(const std::vector<uint8_t>& bytes,size_t offset,size_t length){
    if(offset>bytes.size()||length>bytes.size()-offset||length<32||length>motionVideoLimit)bad("Video Motion Photo fuori limite: massimo 48 MiB");
    Boxes parser{bytes.data()+offset,length};auto top=parser.read(0,length);
    if(top.empty()||top.front().type!="ftyp"||top.front().end-top.front().payload<8)bad("Video Motion Photo senza ftyp MP4 valido");
    std::vector<std::pair<size_t,size_t>> media;
    size_t movies=0,videoTracks=0;
    for(const auto& box:top)if(box.type=="mdat")media.emplace_back(box.payload,box.end);
    if(media.empty())bad("Video Motion Photo senza dati mdat");
    for(const auto& movie:top)if(movie.type=="moov"){
        movies++;
        for(const auto& track:parser.read(movie.payload,movie.end))if(track.type=="trak"){
            bool video=false;int handlers=0,tables=0;std::vector<Box> samples;
            std::function<void(size_t,size_t,int)> visit=[&](size_t start,size_t end,int depth){
                if(depth>16)bad("MP4 troppo annidato");
                for(const auto& box:parser.read(start,end)){
                    size_t size=box.end-box.payload;const auto* p=parser.data+box.payload;
                    if(box.type=="mdia"||box.type=="minf"||box.type=="stbl")visit(box.payload,box.end,depth+1);
                    else if(box.type=="hdlr"){if(size<12||++handlers!=1)bad("Handler MP4 non valido");video=!memcmp(p+8,"vide",4);}
                    else if(box.type=="stsd"){
                        if(size<8||++tables!=1)bad("Tabella codec MP4 non valida");
                        uint64_t count=be(p+4,4);samples=parser.read(box.payload+8,box.end);
                        if(count!=samples.size()||!count||count>16)bad("Descrizioni codec MP4 non valide");
                    }else if(box.type=="stco"||box.type=="co64"){
                        int width=box.type=="stco"?4:8;
                        if(size<8||be(p+4,4)!=(size-8)/width||(size-8)%width)bad("Tabella offset MP4 non valida");
                        for(size_t at=8;at<size;at+=width){
                            uint64_t target=be(p+at,width);bool found=false;
                            for(const auto& span:media)if(target>=span.first&&target<span.second)found=true;
                            if(!found)bad("Riferimento video fuori dai dati MP4");
                        }
                    }
                }
            };
            visit(track.payload,track.end,0);
            if(handlers!=1)bad("Traccia MP4 senza handler");
            if(video){
                if(samples.size()!=1||samples[0].end-samples[0].start<86)bad("Descrizione video MP4 incompleta");
                const std::string& codec=samples[0].type;
                if(codec!="avc1"&&codec!="avc3"&&codec!="hvc1"&&codec!="hev1"&&codec!="av01")bad("Codec Motion Photo non supportato: "+codec);
                videoTracks++;
            }
        }
    }
    if(movies!=1||!videoTracks||videoTracks>2)bad("Motion Photo richiede un moov e una o due tracce video");
}
void appendMotionVideo(const std::string& path,const std::vector<uint8_t>& tail,const MotionClip& clip){
    if(!clip.length)return;
    if(clip.offset>tail.size()||clip.length>tail.size()-clip.offset||clip.length>motionVideoLimit)bad("Riferimento video da scrivere non valido");
    for(const auto& box:fileBoxes(path))if(box.type=="mpvd")bad("Video HEIC duplicato");
    uint32_t length=uint32_t(clip.length+8);uint8_t header[]={uint8_t(length>>24),uint8_t(length>>16),uint8_t(length>>8),uint8_t(length),'m','p','v','d'};
    std::ofstream out(path,std::ios::binary|std::ios::app);
    out.write(reinterpret_cast<char*>(header),8);out.write(reinterpret_cast<const char*>(tail.data()+clip.offset),clip.length);out.close();
    if(!out)bad("Scrittura video Motion Photo incompleta");
}
namespace {
const uint8_t archiveUuid[16]={'C','P','J','P','E','G','3',0,0x80,0,0,0,0,0,0,1};
bool archiveBox(std::ifstream& file,const FileBox& box){
    if(box.type!="uuid"||box.end-box.payload<16)return false;
    uint8_t uuid[16];file.seekg(box.payload);
    return bool(file.read(reinterpret_cast<char*>(uuid),16))&&!memcmp(uuid,archiveUuid,16);
}
}
void appendArchivedTail(const std::string& path,const std::vector<uint8_t>& tail,const MotionClip& clip){
    if(clip.offset>tail.size()||clip.length>tail.size()-clip.offset||tail.size()>52*1024*1024)bad("Coda CPJPEG3 fuori limite");
    auto boxes=fileBoxes(path);std::ifstream old(path,std::ios::binary);
    for(const auto& box:boxes)if(archiveBox(old,box)||box.type=="mpvd")bad("Archivio CPJPEG3 duplicato o posizione non valida");
    uint32_t size=uint32_t(tail.size()-clip.length+24);
    uint8_t header[]={uint8_t(size>>24),uint8_t(size>>16),uint8_t(size>>8),uint8_t(size),'u','u','i','d'};
    std::ofstream out(path,std::ios::binary|std::ios::app);
    out.write(reinterpret_cast<char*>(header),8);out.write(reinterpret_cast<const char*>(archiveUuid),16);
    out.write(reinterpret_cast<const char*>(tail.data()),clip.offset);
    out.write(reinterpret_cast<const char*>(tail.data()+clip.offset+clip.length),tail.size()-clip.offset-clip.length);
    out.close();if(!out)bad("Scrittura archivio CPJPEG3 incompleta");
}
void verifyArchivedTail(const std::string& path,const std::vector<uint8_t>& tail,const MotionClip& clip,bool expected){
    auto boxes=fileBoxes(path);std::ifstream file(path,std::ios::binary);size_t count=0;
    for(const auto& box:boxes)if(archiveBox(file,box)){
        count++;
        if(!expected||clip.offset>tail.size()||clip.length>tail.size()-clip.offset||box.end-box.payload!=tail.size()-clip.length+16)bad("Dimensione archivio CPJPEG3 diversa");
        uint64_t end=clip.length?boxes.back().start:boxes.back().end;
        if(box.end!=end)bad("Posizione archivio CPJPEG3 diversa");
        file.seekg(box.payload+16);std::array<uint8_t,65536> buffer{};
        auto compare=[&](size_t first,size_t length){
            for(size_t at=0;at<length;){
                size_t n=std::min(buffer.size(),length-at);
                if(!file.read(reinterpret_cast<char*>(buffer.data()),n)||memcmp(buffer.data(),tail.data()+first+at,n))bad("Archivio CPJPEG3 diverso dall’originale");
                at+=n;
            }
        };
        compare(0,clip.offset);compare(clip.offset+clip.length,tail.size()-clip.offset-clip.length);
    }
    if(count!=size_t(expected))bad("Archivio CPJPEG3 mancante o duplicato");
}
void verifyMotionVideo(const std::string& path,const std::vector<uint8_t>& tail,const MotionClip& clip){
    auto boxes=fileBoxes(path);size_t count=0;
    for(const auto& box:boxes)if(box.type=="mpvd"){
        count++;
        if(!clip.length||box.end!=boxes.back().end||box.end-box.payload!=clip.length||box.payload-box.start!=8)bad("Posizione o dimensione video HEIC diversa");
        if(clip.offset>tail.size()||clip.length>tail.size()-clip.offset)bad("Riferimento video originale non valido");
        std::ifstream file(path,std::ios::binary);file.seekg(box.payload);std::array<uint8_t,65536> buffer{};
        for(size_t at=0;at<clip.length;){
            size_t n=std::min(buffer.size(),clip.length-at);
            if(!file.read(reinterpret_cast<char*>(buffer.data()),n)||memcmp(buffer.data(),tail.data()+clip.offset+at,n))bad("Video Motion Photo diverso dall’originale");
            at+=n;
        }
    }
    if(count!=size_t(clip.length!=0))bad("Video Motion Photo mancante o duplicato");
}
}
