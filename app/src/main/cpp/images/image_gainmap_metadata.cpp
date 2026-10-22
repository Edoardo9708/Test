#include "image_gainmap.h"
#include "xml/tinyxml2.h"
#include <cstring>
#include <string>
#include <stdexcept>
#include <functional>
namespace photo {
namespace {
using Bytes=std::vector<uint8_t>;
[[noreturn]] void bad(){throw std::runtime_error("MPF non supportato o mappa HDR Apple non valida");}
struct Index {uint32_t size=0,offset=0;};
Index mpfIndex(const Bytes& b,bool index,size_t jpegEnd){
    if(b.size()<12||memcmp(b.data(),"MPF\0",4))bad();
    bool le=b[4]=='I'&&b[5]=='I';if(!le&&!(b[4]=='M'&&b[5]=='M'))bad();
    auto u16=[&](size_t at)->uint16_t{at+=4;if(at+2>b.size())bad();return le?b[at]|b[at+1]<<8:b[at]<<8|b[at+1];};
    auto u32=[&](size_t at)->uint32_t{at+=4;if(at+4>b.size())bad();uint32_t v=0;for(int i=0;i<4;i++)v=(v<<8)|b[at+(le?3-i:i)];return v;};
    if(u16(2)!=42)bad();size_t at=u32(4);unsigned n=u16(at);if(n!=(index?3u:2u)||at+2+12*n+4>b.size()-4)bad();
    bool version=false,count=false,entries=false;size_t entry=0;
    for(unsigned i=0;i<n;i++){size_t t=at+2+12*i;unsigned tag=u16(t),type=u16(t+2);uint32_t num=u32(t+4),value=u32(t+8);
        if(tag==0xb000&&!version&&type==7&&num==4){if(memcmp(b.data()+4+t+8,"0100",4))bad();version=true;}
        else if(tag==0xb001&&!count&&type==4&&num==1&&value==2)count=true;
        else if(index&&tag==0xb002&&!entries&&type==7&&num==32){entry=value;entries=true;}
        else bad();
    }
    if(!version||!count||u32(at+2+12*n)!=0)bad();if(!index)return {};
    if(!entries||entry>b.size()-4||b.size()-4-entry<32)bad();
    // Simple primary + independent JPEG gain map. No other MP types/dependencies.
    if(u32(entry)!=0x00030000||u32(entry+4)!=jpegEnd||u32(entry+8)!=0||u32(entry+12)!=0||u32(entry+16)!=0||u32(entry+28)!=0)bad();
    return {u32(entry+20),u32(entry+24)};
}
std::string uri(const tinyxml2::XMLElement* e,const char* name){std::string s=name;auto colon=s.find(':');std::string key=colon==std::string::npos?"xmlns":"xmlns:"+s.substr(0,colon);for(auto p=e;p;p=p->Parent()?p->Parent()->ToElement():nullptr){auto value=p->Attribute(key.c_str());if(value)return value;}return {};}
void validateXmp(const Bytes& b){
    if(b.empty()||b.size()>65536||memchr(b.data(),0,b.size()))bad();std::string s(b.begin(),b.end());if(s.find("<!DOCTYPE")!=std::string::npos||s.find("<!ENTITY")!=std::string::npos)bad();
    tinyxml2::XMLDocument doc;if(doc.Parse(s.data(),s.size())!=tinyxml2::XML_SUCCESS||!doc.RootElement())bad();int aux=0,version=0,nodes=0;
    std::function<void(const tinyxml2::XMLElement*,int)> visit=[&](const tinyxml2::XMLElement* e,int depth){if(depth>32||++nodes>2048)bad();
        auto field=[&](const char* name,const char* value){std::string local=name;auto c=local.find(':');if(c!=std::string::npos)local=local.substr(c+1);auto ns=uri(e,name);
            if(local=="AuxiliaryImageType"&&ns=="http://ns.apple.com/pixeldatainfo/1.0/"){if(++aux!=1||!value||strcmp(value,appleGainMapType))bad();}
            if(local=="HDRGainMapVersion"&&ns=="http://ns.apple.com/HDRGainMap/1.0/"){if(++version!=1||!value||strcmp(value,"65536"))bad();}
        };field(e->Name(),e->GetText());for(auto a=e->FirstAttribute();a;a=a->Next())field(a->Name(),a->Value());for(auto child=e->FirstChildElement();child;child=child->NextSiblingElement())visit(child,depth+1);
    };visit(doc.RootElement(),0);if(aux!=1||version!=1)bad();
}
}
GainMap inspectAppleGainMap(const Bytes& mpf,size_t tiffBase,size_t jpegEnd,const Bytes& tail,int width,int height){
    Index index=mpfIndex(mpf,true,jpegEnd);if(uint64_t(tiffBase)+index.offset!=jpegEnd||index.size!=tail.size()||tail.size()<4||tail.size()>16*1024*1024||tail[0]!=255||tail[1]!=216)bad();
    GainMap map;size_t at=2;int pending=-1,segments=0,scans=0;bool auxMpf=false;
    auto next=[&](){if(at>=tail.size())bad();return int(tail[at++]);};
    while(true){int marker=pending;pending=-1;if(marker<0){if(next()!=255)bad();do{marker=next();}while(marker==255);}if(++segments>65536)bad();if(marker==217)break;
        if(marker==0||marker==216||(marker>=208&&marker<=215))bad();int len=next()<<8;len+=next();if(len<2||size_t(len-2)>tail.size()-at)bad();len-=2;size_t start=at;at+=len;const uint8_t* data=tail.data()+start;
        if(marker==192){if(map.width||len!=9||data[0]!=8||data[5]!=1||data[6]!=1||data[7]!=0x11)bad();map.height=data[1]*256+data[2];map.width=data[3]*256+data[4];}
        else if(marker==225){if(len<29||memcmp(data,"http://ns.adobe.com/xap/1.0/\0",29)||!map.xmp.empty())bad();map.xmp.assign(data+29,data+len);}
        else if(marker==226){if(auxMpf)bad();mpfIndex(Bytes(data,data+len),false,0);auxMpf=true;}
        else if(marker==224){if(len!=14||memcmp(data,"JFIF\0",5)||data[12]||data[13])bad();}
        else if(marker!=219&&marker!=196&&marker!=221&&marker!=218)bad();
        if(marker==218){if(!map.width||++scans!=1||len!=6||data[0]!=1)bad();while(true){if(next()!=255)continue;int v;do{v=next();}while(v==255);if(v==0||(v>=208&&v<=215))continue;pending=v;break;}}
    }
    if(at!=tail.size()||scans!=1||map.width<64||map.height<64||map.width>16384||map.height>16384||int64_t(map.width)*map.height>16000000||int64_t(map.width)*height!=int64_t(map.height)*width)bad();
    validateXmp(map.xmp);return map;
}
}
