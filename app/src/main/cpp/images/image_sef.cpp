#include "image_sef.h"
#include <algorithm>
#include <cstring>
#include <stdexcept>
#include <string>
#include <utility>
namespace photo {
namespace {
using Bytes=std::vector<uint8_t>;
[[noreturn]] void bad(const std::string& message){throw std::runtime_error(message);}
uint32_t le32(const Bytes& b,size_t p){if(p+4>b.size())bad("SEF troncato");return uint32_t(b[p])|uint32_t(b[p+1])<<8|uint32_t(b[p+2])<<16|uint32_t(b[p+3])<<24;}
// Names and types cross-checked against ExifTool Samsung.pm. These are only
// archived as opaque metadata; never applied as edits, transforms or colour rules.
std::string typeHex(uint32_t type){static const char digits[]="0123456789abcdef";std::string value="0x";for(int shift=28;shift>=16;shift-=4)value+=digits[(type>>shift)&15];return value;}
bool opaquePhotoRecord(uint32_t type,const std::string& name){
    static const std::pair<uint16_t,const char*> records[]={
        {0x0ce1,"Gallery_DC_Data"}, {0x0ba1,"PhotoEditor_Re_Edit_Data"},
        {0x0ba1,"Original_Path_Hash_Key"}, {0x0ba1,"deco_doodle_bitmap"},
        {0x0ba1,"deco_sticker_bitmap"}, {0x0ba1,"deco_text_bitmap"},
        {0x0ba2,"Copy_Available_Edit_Info"}, {0x0b51,"Intelligent_PhotoEditor_Data"},
        {0x0b60,"UltraWide_PhotoEditor_Data"}, {0x0bf0,"Remaster_Info"},
        {0x0d91,"PEg_Info"}, {0x09f0,"Pro_Mode_Info"},
        {0x08e0,"Panorama_Shot_Info"}, {0x0b40,"SingleShotMeta"},
        {0x0b41,"SingleShot_DepthMap_1"}, {0x0b41,"SingeShot_DepthMap_1"},
        {0x0b41,"SingeShot_DepthMap_5"}, {0x0b41,"SingeShot_DepthMap_8"},
        {0x0b41,"SingeShot_DepthMap_10"}, {0x0ab0,"DualShot_Meta_Info"},
        {0x0ab3,"DualShot_Extra_Info"}, {0x0ab4,"DualShot_Core_Info"},
        {0x0ab1,"DualShot_DepthMap_1"}, {0x0ab1,"DualShot_DepthMap_5"},
        {0x09e1,"BurstShot_Best_Photo_Info"}, {0x0bd0,"Dual_Relighting_Bokeh_Info"},
        {0x0c21,"Portrait_Effect_Info"}, {0x0b90,"Document_Scan_Info"},
        {0x0d01,"Camera_Scene_Info"}, {0x0d21,"Camera_Scene_Info"},
        {0x0d01,"Camera_Scene_Info2"}, {0x0d01,"Camera_Scene_Info3"},
        {0x0001,"SingleShot"}, {0x0001,"DualShot_1"}, {0x0001,"DualShot_2"}
    };
    for(const auto& record:records)if(type==(uint32_t(record.first)<<16)&&name==record.second)return true;
    return false;
}
std::string tailSummary(const Bytes& b){
    static const char digits[]="0123456789abcdef";
    auto hex=[&](size_t first,size_t count){std::string text;for(size_t i=first;i<first+count;i++){if(i>first)text+=' ';text+=digits[b[i]>>4];text+=digits[b[i]&15];}return text;};
    size_t head=std::min<size_t>(32,b.size()),last=std::min<size_t>(16,b.size());
    return "byte="+std::to_string(b.size())+"; primi="+hex(0,head)+"; ultimi="+hex(b.size()-last,last);
}
bool paddingTail(const Bytes& b){
    if(b.size()>4*1024*1024)return false;
    size_t at=0;while(at<b.size()&&b[at]==0xd9)at++;
    return std::all_of(b.begin()+at,b.end(),[](uint8_t value){return value==0;});
}
}
void validateJpegTail(const std::vector<uint8_t>& b){
    if(b.empty()||paddingTail(b))return;
    if(b.size()>4*1024*1024&&!memcmp(b.data()+b.size()-4,"SEFT",4))bad("Blocco SEF oltre il limite di 4 MiB: "+tailSummary(b));
    if(b.size()<32||b.size()>4*1024*1024||memcmp(b.data()+b.size()-4,"SEFT",4))bad("Dati dopo JPEG non supportati: tipo da identificare · "+tailSummary(b));
    uint32_t length=le32(b,b.size()-8);if(length<24||length>b.size()-8||(length-12)%12)bad("Directory SEF non valida");
    size_t dir=b.size()-8-length;uint32_t count=le32(b,dir+8),version=le32(b,dir+4);
    if(memcmp(b.data()+dir,"SEFH",4)||count==0||count>4096||count!=(length-12)/12||(version!=103&&version!=105&&version!=106&&version!=107))bad("Versione o directory SEF non supportata");
    std::vector<std::pair<size_t,size_t>> ranges;ranges.reserve(count);
    for(size_t i=0;i<count;i++){
        size_t e=dir+12+i*12;uint32_t type=le32(b,e),offset=le32(b,e+4),size=le32(b,e+8);
        if(offset>dir||size<9||size>offset)bad("Riferimenti SEF non validi: record="+std::to_string(i+1)+"; offset="+std::to_string(offset)+"; dimensione="+std::to_string(size)+"; directory="+std::to_string(dir));
        size_t start=dir-offset;
        uint32_t nameSize=le32(b,start+4);if(nameSize>256||nameSize==0||nameSize>size-8||le32(b,start)!=type)bad("Record SEF non valido");
        std::string name(reinterpret_cast<const char*>(b.data()+start+8),nameSize);size_t payload=start+8+nameSize,bytes=size-8-nameSize;
        ranges.emplace_back(start,start+size);
        bool utc=name=="Image_UTC_Data"&&type==0x0a010000&&bytes==13;
        bool mcc=name=="MCC_Data"&&type==0x0aa10000&&bytes==3;
        bool capture=name=="Camera_Capture_Mode_Info"&&type==0x0c610000&&bytes>0&&bytes<=16;
        bool p3=name=="Color_Display_P3"&&type==0x0cc10000&&bytes==3;
        bool selfie=name=="Front_Cam_Selfie_Info"&&type==0x09100000;
        bool burst=name=="Burst_Shot_Info"&&type==0x09e00000;
        bool opaque=opaquePhotoRecord(type,name);
        if(!utc&&!mcc&&!capture&&!p3&&!selfie&&!burst&&!opaque)bad("Record SEF foto non supportato: "+name+"; tipo="+typeHex(type)+"; payload="+std::to_string(bytes)+" byte");
        // Only UTC, MCC and capture-mode codes are checked as decimal text.
        // Scene/editor/embedded-image records remain archival bytes; their values
        // never supply transforms, colour instructions or additional HEIF images.
        if(!p3&&!selfie&&!burst&&!opaque)for(size_t j=payload;j<payload+bytes;j++)if(b[j]<'0'||b[j]>'9')bad("Valore SEF foto non verificato");
    }
    // Directory order need not match physical order. Every data byte must still
    // belong to exactly one validated record: reject overlaps, holes and aliases.
    std::sort(ranges.begin(),ranges.end());size_t expected=0;
    for(const auto& range:ranges){if(range.first!=expected)bad("Riferimenti SEF non validi: record sovrapposti o byte non indicizzati; atteso="+std::to_string(expected)+"; inizio="+std::to_string(range.first)+"; fine="+std::to_string(range.second));expected=range.second;}
    if(expected!=dir)bad("Byte SEF non indicizzati");
}
}
