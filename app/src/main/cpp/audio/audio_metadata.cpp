#include "audio_metadata.h"
#include <cstring>
#include <cstdint>
#include <fstream>
#include <stdexcept>
#include <algorithm>
namespace {
constexpr size_t LIMIT=32*1024*1024;
void check(bool condition,const char* message){if(!condition)throw std::runtime_error(message);}
void be32(std::string& out,uint32_t n){for(int shift=24;shift>=0;shift-=8)out.push_back(static_cast<char>(n>>shift));}
std::string base64(const std::string& data){
    static const char table[]="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    std::string out;out.reserve((data.size()+2)/3*4);
    for(size_t i=0;i<data.size();i+=3){uint32_t n=static_cast<unsigned char>(data[i])<<16;
        if(i+1<data.size())n|=static_cast<unsigned char>(data[i+1])<<8;
        if(i+2<data.size())n|=static_cast<unsigned char>(data[i+2]);
        out+=table[n>>18];out+=table[(n>>12)&63];out+=i+1<data.size()?table[(n>>6)&63]:'=';out+=i+2<data.size()?table[n&63]:'=';
    }return out;
}
uint32_t le32(const std::string& data,size_t& at){
    check(at+4<=data.size(),"OpusTags troncato");uint32_t n=0;for(int i=0;i<4;i++)n|=static_cast<uint32_t>(static_cast<unsigned char>(data[at++]))<<(i*8);return n;
}
std::vector<std::string> readTags(const char* path){
    std::ifstream in(path,std::ios::binary);check(bool(in),"Riapertura Opus per verifica metadati fallita");
    std::string packet;int packets=0;size_t scanned=0;
    while(packets<2){
        unsigned char header[27];in.read(reinterpret_cast<char*>(header),27);
        check(in.gcount()==27&&std::memcmp(header,"OggS",4)==0&&header[4]==0,"Pagina Ogg non valida durante verifica tag");
        unsigned char laces[255];in.read(reinterpret_cast<char*>(laces),header[26]);check(in.gcount()==header[26],"Segmenti Ogg troncati");
        for(unsigned i=0;i<header[26];i++){
            char data[255];in.read(data,laces[i]);check(in.gcount()==laces[i],"Dati Ogg troncati");
            scanned+=laces[i];check(scanned<=LIMIT+65536,"OpusTags oltre il limite di verifica");packet.append(data,laces[i]);
            if(laces[i]<255){
                if(++packets==2){
                    check(packet.size()>=16&&packet.compare(0,8,"OpusTags")==0,"Pacchetto OpusTags assente");
                    size_t at=8;uint32_t vendor=le32(packet,at);check(vendor<=packet.size()-at,"Vendor OpusTags troncato");at+=vendor;
                    uint32_t count=le32(packet,at);check(count<=100000,"Troppi tag nell’output Opus");std::vector<std::string> result;
                    for(uint32_t n=0;n<count;n++){uint32_t length=le32(packet,at);check(length<=packet.size()-at,"Valore OpusTags troncato");result.push_back(packet.substr(at,length));at+=length;}
                    return result;
                }
                check(packet.size()==19&&packet.compare(0,8,"OpusHead")==0,"Header Opus mono/stereo inatteso");packet.clear();
            }
        }
    }throw std::runtime_error("OpusTags non trovato");
}
}
void AudioMetadata::add(std::string entry){
    check(entry.size()+4<=LIMIT-bytes&&entries.size()<100000,"Metadati audio oltre 32 MiB o 100000 voci: nessun dato scartato, conversione interrotta");
    check(entry.find('\0')==std::string::npos,"Tag FLAC con NUL interno: impossibile copiarlo integralmente in libopusenc");
    check(FLAC__format_vorbiscomment_entry_is_legal(reinterpret_cast<const FLAC__byte*>(entry.data()),static_cast<uint32_t>(entry.size())),"Tag FLAC non conforme (chiave o UTF-8): nessun dato scartato");
    bytes+=entry.size()+4;entries.push_back(std::move(entry));
}
// APPLICATION blocks belong to the source format. Archive them as inert binary
// data encoded in a custom comment, never as active RIFF/AIFF/Opus structures.
void AudioMetadata::readApplication(const FLAC__StreamMetadata& block){
    check(block.length>=4&&block.length<=0xFFFFFF,"Blocco FLAC APPLICATION con lunghezza non valida");
    const auto& a=block.data.application;size_t payload=block.length-4;
    check(payload==0||a.data!=nullptr,"Blocco FLAC APPLICATION senza payload");
    std::string raw(reinterpret_cast<const char*>(a.id),4);
    if(payload)raw.append(reinterpret_cast<const char*>(a.data),payload);
    add("CODECPROBE_FLAC_APPLICATION="+base64(raw));
    applications++;applicationBytes+=raw.size();
    if(applicationDetails.size()<80){
        static const char hex[]="0123456789ABCDEF";std::string id,encoded;
        for(unsigned char c:a.id){id+=c>=32&&c<=126?static_cast<char>(c):'.';encoded+=hex[c>>4];encoded+=hex[c&15];}
        applicationDetails.push_back("APPLICATION #"+std::to_string(applications)+" · ID="+id+" · HEX="+encoded+" · payload="+std::to_string(payload)+" byte · ID+payload archiviati="+std::to_string(raw.size())+" byte");
    }
}
void AudioMetadata::read(const FLAC__StreamMetadata& block){
    if(block.type==FLAC__METADATA_TYPE_APPLICATION){readApplication(block);}
    else if(block.type==FLAC__METADATA_TYPE_VORBIS_COMMENT){
        const auto& comments=block.data.vorbis_comment;
        for(uint32_t n=0;n<comments.num_comments;n++){const auto& e=comments.comments[n];add(std::string(reinterpret_cast<const char*>(e.entry),e.length));}
    }else if(block.type==FLAC__METADATA_TYPE_PICTURE){
        const auto& p=block.data.picture;std::string picture;
        size_t mime=std::strlen(p.mime_type),description=std::strlen(reinterpret_cast<const char*>(p.description));
        check(p.data_length<=16*1024*1024&&mime+description<=65536,"Copertina FLAC oltre i limiti: nessuna immagine scartata");
        be32(picture,p.type);be32(picture,static_cast<uint32_t>(mime));picture.append(p.mime_type,mime);
        be32(picture,static_cast<uint32_t>(description));picture.append(reinterpret_cast<const char*>(p.description),description);
        be32(picture,p.width);be32(picture,p.height);be32(picture,p.depth);be32(picture,p.colors);be32(picture,p.data_length);
        picture.append(reinterpret_cast<const char*>(p.data),p.data_length);
        add("METADATA_BLOCK_PICTURE="+base64(picture));pictures++;
    }else if(block.type==FLAC__METADATA_TYPE_PADDING||block.type==FLAC__METADATA_TYPE_SEEKTABLE){technical++;}
    else if(block.type!=FLAC__METADATA_TYPE_STREAMINFO){
        throw std::runtime_error("Blocco FLAC tipo "+std::to_string(block.type)+" (CUESHEET/proprietario) non trasferibile automaticamente in Opus; nessun metadato scartato");
    }
}
void AudioMetadata::apply(OggOpusComments* comments,const std::function<void(const std::string&)>& log) const{
    log("*** COPIA METADATI AUDIO ***\nTag e immagini: "+std::to_string(entries.size())+" voci · copertine PICTURE="+std::to_string(pictures)+" · byte nei commenti="+std::to_string(bytes)+"\nValori originali, campi personalizzati e ripetizioni conservati. Copertine trasferite in METADATA_BLOCK_PICTURE senza ricomprimere.\nBlocchi tecnici FLAC (padding/seektable) non trasferiti: "+std::to_string(technical)+". Il vendor del contenitore identifica le librerie del nuovo encoder.");
    if(applications){
        log("*** ARCHIVIO DATI APPLICATION DEL FLAC ***\nBlocchi="+std::to_string(applications)+" · ID+payload originali="+std::to_string(applicationBytes)+" byte.\nOgni blocco e conservato in un commento CODECPROBE_FLAC_APPLICATION: Base64 di ID (4 byte) + payload, nello stesso ordine della sorgente.\nSono dati di archivio: i lettori Opus non li applicano all’audio. Intestazioni WAV/RIFF originali non vengono usate per descrivere la nuova codifica.\nLa verifica finale confronta tutti i commenti, compresi questi dati, byte per byte.");
        for(const auto& detail:applicationDetails)log(detail);
        if(applications>80)log("Elenco APPLICATION abbreviato a 80 righe; tutti i blocchi sono archiviati e verificati.");
    }
    size_t n=0;for(const auto& entry:entries){
        int rc=ope_comments_add_string(comments,entry.c_str());if(rc!=OPE_OK)throw std::runtime_error(std::string("Copia tag: ")+ope_strerror(rc));
        if(n++<80){std::string key=entry.substr(0,std::min(entry.find('='),size_t(100)));log("COPIATO: "+key+" · "+std::to_string(entry.size())+" byte");}
    }
    if(entries.size()>80)log("Elenco tag abbreviato a 80 righe; tutte le "+std::to_string(entries.size())+" voci vengono comunque copiate e verificate.");
}
void AudioMetadata::verify(const char* path,const std::function<void(const std::string&)>& log) const{
    auto actual=readTags(path);check(actual==entries,"Verifica metadati Opus fallita: numero, ordine o byte dei tag/copertine diversi dall’originale");
    log("OK VERIFICA METADATI: "+std::to_string(entries.size())+" voci rilette dall’Opus e confrontate byte per byte, incluse immagini e campi ripetuti.");
}
