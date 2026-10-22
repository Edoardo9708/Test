#include "image_motion.h"
#include "xml/tinyxml2.h"
#include <algorithm>
#include <cctype>
#include <map>
#include <stdexcept>
#include <utility>
namespace photo {
namespace {
using namespace tinyxml2;
const std::string camera="http://ns.google.com/photos/1.0/camera/";
const std::string container="http://ns.google.com/photos/1.0/container/";
const std::string item="http://ns.google.com/photos/1.0/container/item/";
const std::string rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#";
[[noreturn]] void bad(const std::string& message){throw std::runtime_error("XMP Motion Photo: "+message);}
std::string local(const char* name){std::string s=name;auto colon=s.find(':');return colon==std::string::npos?s:s.substr(colon+1);}
std::string space(const XMLElement* element,const char* name,bool attribute=false){
    std::string s=name;auto colon=s.find(':');
    if(colon==std::string::npos&&attribute)return "";
    std::string key=colon==std::string::npos?"xmlns":"xmlns:"+s.substr(0,colon);
    for(auto p=element;p;p=p->Parent()?p->Parent()->ToElement():nullptr){const char* value=p->Attribute(key.c_str());if(value)return value;}
    return "";
}
bool named(const XMLElement* e,const std::string& uri,const std::string& name){return space(e,e->Name())==uri&&local(e->Name())==name;}
std::string trim(const char* value){
    if(!value)return "";std::string s=value;
    auto blank=[](unsigned char c){return std::isspace(c);};
    s.erase(s.begin(),std::find_if_not(s.begin(),s.end(),blank));s.erase(std::find_if_not(s.rbegin(),s.rend(),blank).base(),s.end());return s;
}
int64_t number(const std::string& value,bool timestamp=false){
    if(timestamp&&value=="-1")return -1;
    if(value.empty()||value.size()>19)bad("intero non valido");
    uint64_t n=0;for(char c:value){if(c<'0'||c>'9'||n>(uint64_t(INT64_MAX)-(c-'0'))/10)bad("intero fuori limite");n=n*10+c-'0';}
    return int64_t(n);
}
std::string property(XMLElement* element,const std::string& uri,const std::string& name,bool required=true){
    std::string result;bool found=false;
    auto set=[&](const char* text){if(found)bad("proprietà duplicata: "+name);found=true;result=trim(text);};
    for(auto a=element->FirstAttribute();a;a=a->Next())if(space(element,a->Name(),true)==uri&&local(a->Name())==name)set(a->Value());
    for(auto e=element->FirstChildElement();e;e=e->NextSiblingElement())if(named(e,uri,name)){if(e->FirstChildElement())bad("proprietà strutturata inattesa");set(e->GetText());}
    if(required&&!found)bad("proprietà mancante: "+name);
    return result;
}
void validateDirectory(XMLElement* directory,size_t eofOffset,size_t jpegSize,MotionClip& clip){
    auto seq=directory->FirstChildElement();
    if(!seq||!named(seq,rdf,"Seq")||seq->NextSiblingElement())bad("Directory deve essere una sequenza RDF");
    size_t index=0;
    for(auto li=seq->FirstChildElement();li;li=li->NextSiblingElement(),index++){
        if(index>=2||!named(li,rdf,"li"))bad("Directory con contenuti aggiuntivi non gestiti");
        auto entry=li->FirstChildElement();
        if(entry&&named(entry,container,"Item")){if(entry->NextSiblingElement())bad("Item duplicato");}
        else entry=li;
        auto mime=property(entry,item,"Mime"),semantic=property(entry,item,"Semantic");
        if(index==0){
            if(mime!="image/jpeg"||semantic!="Primary")bad("immagine primaria diversa dal JPEG semplice");
            auto length=property(entry,item,"Length",false),padding=property(entry,item,"Padding",false);
            if(!length.empty()){auto n=uint64_t(number(length));if(n>jpegSize+clip.offset)bad("Length dell’immagine primaria fuori dal JPEG");clip.primaryLengthRewritten=n!=0;}
            if(!padding.empty()&&uint64_t(number(padding))>eofOffset)bad("Padding fuori limite");
        }else{
            if(mime!="video/mp4"||semantic!="MotionPhoto")bad("elemento secondario diverso dal video Motion Photo");
            auto length=uint64_t(number(property(entry,item,"Length")));
            // Samsung JPEGs may count the following SEF directory in this value.
            if(length!=clip.length&&length!=eofOffset)bad("Length non corrisponde al video SEF");
        }
    }
    if(index!=2)bad("Directory incompleta");
}
}
std::vector<uint8_t> motionXmp(const std::vector<uint8_t>& original,size_t eofOffset,size_t jpegSize,MotionClip& clip){
    XMLDocument document;
    const char* empty="<x:xmpmeta xmlns:x=\"adobe:ns:meta/\"><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/></x:xmpmeta>";
    if((original.empty()?document.Parse(empty):document.Parse(reinterpret_cast<const char*>(original.data()),original.size()))!=XML_SUCCESS)bad("XML non valido");
    if(!document.RootElement()||document.RootElement()->NextSiblingElement())bad("radice XML non valida");
    XMLElement* graph=nullptr;std::vector<XMLElement*> directories,properties;
    std::vector<std::pair<XMLElement*,std::string>> attributes;
    std::map<std::string,std::string> values;
    const std::vector<std::string> keys={"MicroVideo","MicroVideoVersion","MicroVideoOffset","MicroVideoPresentationTimestampUs","MotionPhoto","MotionPhotoVersion","MotionPhotoPresentationTimestampUs"};
    auto save=[&](const std::string& key,const char* text){if(!values.emplace(key,trim(text)).second)bad("proprietà duplicata: "+key);};
    std::vector<XMLNode*> pending{&document};size_t nodes=0;
    while(!pending.empty()){
        auto node=pending.back();pending.pop_back();if(++nodes>16384)bad("troppi nodi XML");
        if(node->ToUnknown())bad("DTD o dichiarazione XML esterna non ammessa");
        auto e=node->ToElement();
        if(e){
            if(named(e,rdf,"RDF")){if(graph)bad("grafi RDF multipli");graph=e;}
            if(named(e,container,"Directory"))directories.push_back(e);
            std::string key=local(e->Name());
            if(space(e,e->Name())==camera&&(key.find("MotionPhoto")==0||key.find("MicroVideo")==0)){
                if(std::find(keys.begin(),keys.end(),key)==keys.end()||e->FirstChildElement())bad("proprietà camera non gestita: "+key);
                save(key,e->GetText());properties.push_back(e);
            }
            for(auto a=e->FirstAttribute();a;a=a->Next()){
                key=local(a->Name());
                if(space(e,a->Name(),true)==camera&&(key.find("MotionPhoto")==0||key.find("MicroVideo")==0)){
                    if(std::find(keys.begin(),keys.end(),key)==keys.end())bad("proprietà camera non gestita: "+key);
                    save(key,a->Value());attributes.emplace_back(e,a->Name());
                }
            }
        }
        for(auto c=node->FirstChild();c;c=c->NextSibling())pending.push_back(c);
    }
    if(!graph||directories.size()>1)bad("grafo RDF assente o directory duplicate");
    for(const char* key:{"MicroVideoVersion","MotionPhotoVersion"})if(values.count(key)&&number(values[key])!=1)bad("versione non supportata");
    bool seenFlag=false;int64_t flag=1;int64_t timestamp=-1;bool seenTimestamp=false;
    for(const char* key:{"MicroVideo","MotionPhoto"})if(values.count(key)){auto n=number(values[key]);if(n>1||(seenFlag&&flag!=n))bad("flag camera incoerenti");seenFlag=true;flag=n;}
    for(const char* key:{"MicroVideoPresentationTimestampUs","MotionPhotoPresentationTimestampUs"})if(values.count(key)){
        auto n=number(values[key],true);if(seenTimestamp&&timestamp!=n)bad("timestamp camera incoerenti");seenTimestamp=true;timestamp=n;
    }
    if(values.count("MicroVideoOffset")&&uint64_t(number(values["MicroVideoOffset"]))!=eofOffset)bad("MicroVideoOffset non corrisponde al video SEF");
    for(auto directory:directories)validateDirectory(directory,eofOffset,jpegSize,clip);
    // Retain all non-motion properties in the active XMP. The unmodified packet
    // also remains in the APP archive, including its original offsets and layout.
    for(const auto& a:attributes)a.first->DeleteAttribute(a.second.c_str());
    for(auto property:properties)property->Parent()->DeleteChild(property);
    for(auto directory:directories)directory->Parent()->DeleteChild(directory);
    clip.enabled=flag==1;clip.timestamp=timestamp;
    auto description=document.NewElement("rdf:Description");
    description->SetAttribute("xmlns:rdf",rdf.c_str());description->SetAttribute("rdf:about","");
    description->SetAttribute("xmlns:Camera",camera.c_str());description->SetAttribute("xmlns:Container",container.c_str());description->SetAttribute("xmlns:Item",item.c_str());
    description->SetAttribute("Camera:MotionPhoto",clip.enabled?"1":"0");description->SetAttribute("Camera:MotionPhotoVersion","1");
    description->SetAttribute("Camera:MotionPhotoPresentationTimestampUs",std::to_string(timestamp).c_str());
    auto directory=document.NewElement("Container:Directory");auto sequence=document.NewElement("rdf:Seq");
    for(bool video:{false,true}){
        auto li=document.NewElement("rdf:li");li->SetAttribute("rdf:parseType","Resource");auto entry=document.NewElement("Container:Item");
        entry->SetAttribute("Item:Mime",video?"video/mp4":"image/heic");entry->SetAttribute("Item:Semantic",video?"MotionPhoto":"Primary");
        if(video)entry->SetAttribute("Item:Length",std::to_string(clip.length).c_str());else entry->SetAttribute("Item:Padding","8");
        li->InsertEndChild(entry);sequence->InsertEndChild(li);
    }
    directory->InsertEndChild(sequence);description->InsertEndChild(directory);graph->InsertEndChild(description);
    XMLPrinter printer(nullptr,true);document.Print(&printer);
    return std::vector<uint8_t>(printer.CStr(),printer.CStr()+printer.CStrSize()-1);
}
}
