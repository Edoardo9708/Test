#include "image_exif.h"
#include <stdexcept>
#include <string>
namespace photo {
namespace {
[[noreturn]] void bad(const std::string& message){throw std::runtime_error(message);}
}
// SHORT is the usual EXIF orientation representation. Also tolerate a single
// unsigned LONG, reading all four bytes and retaining the source EXIF verbatim.
int exifOrientation(const std::vector<uint8_t>& exif){
    if(exif.empty())return 1;
    if(exif.size()<14)bad("EXIF incompleto");
    const size_t base=6;
    bool little=exif[6]=='I'&&exif[7]=='I';
    if(!little&&!(exif[6]=='M'&&exif[7]=='M'))bad("Ordine byte EXIF non valido");
    auto word=[&](size_t at,int count)->uint32_t{
        if(at>exif.size()||size_t(count)>exif.size()-at)bad("Riferimento EXIF fuori limite");
        uint32_t value=0;
        for(int n=0;n<count;n++)value|=uint32_t(exif[at+n])<<(8*(little?n:count-n-1));
        return value;
    };
    if(word(8,2)!=42)bad("Header TIFF EXIF non valido");
    size_t ifd=base+word(10,4),count=word(ifd,2);
    if(count>(exif.size()-ifd-2)/12)bad("IFD EXIF troncato");
    int result=1;
    bool seen=false;
    for(size_t i=0;i<count;i++){
        size_t p=ifd+2+i*12;
        if(word(p,2)!=0x112)continue;
        if(seen)bad("Orientamento EXIF non valido: tag duplicato");
        uint32_t type=word(p+2,2),quantity=word(p+4,4);
        if((type!=3&&type!=4)||quantity!=1)
            bad("Orientamento EXIF non valido: tipo="+std::to_string(type)+"; quantita="+std::to_string(quantity));
        uint32_t value=word(p+8,type==4?4:2);
        if(value>8)
            bad("Orientamento EXIF fuori intervallo: valore="+std::to_string(value)+"; atteso 1..8");
        result=int(value);
        seen=true;
    }
    return result;
}
}
