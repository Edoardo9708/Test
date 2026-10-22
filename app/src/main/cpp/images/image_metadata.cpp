#include "image_metadata.h"
#include "image_exif.h"
#include "image_jumbf.h"
#include <algorithm>
#include <cctype>
#include <cstring>
#include <map>
#include <stdexcept>
namespace photo {
namespace {
[[noreturn]] void bad(const std::string& message){throw std::runtime_error(message);}
bool starts(const Bytes& b,const char* text,size_t n){return b.size()>=n&&!memcmp(b.data(),text,n);}
void put32(Bytes& b,size_t value){for(int shift=24;shift>=0;shift-=8)b.push_back((value>>shift)&255);}
}
Metadata readJpegMetadata(const Bytes& input){
    Metadata m;size_t at=2,total=0;int pending=-1,segments=0;bool motionHint=false,directoryHint=false;std::map<int,Bytes> icc;std::vector<Bytes> app11;int iccParts=0;Bytes mpf;size_t tiffBase=0;int width=0,height=0;
    if(input.size()<4||input[0]!=255||input[1]!=216)bad("Input non JPEG");
    size_t sefStart=jpegSefTailStart(input);
    size_t jpegLimit=sefStart<input.size()?sefStart:input.size();
    auto next=[&](){if(at>=jpegLimit)bad("JPEG troncato: offset="+std::to_string(at)+"; byte totali="+std::to_string(input.size()));return int(input[at++]);};
    while(true){
        int marker=pending;pending=-1;if(marker<0){size_t markerAt=at;int prefix=next();if(prefix!=255)bad("Marcatore JPEG atteso: offset="+std::to_string(markerAt)+"; byte="+std::to_string(prefix));do{marker=next();}while(marker==255);}
        if(++segments>65536)bad("Troppi segmenti JPEG");if(marker==217)break;
        if(marker==216||marker==0||(marker>=208&&marker<=215))bad("Marcatori JPEG non validi");if(marker==1)continue;
        int length=(next()<<8);length+=next();length-=2;if(length<0||size_t(length)>jpegLimit-at)bad("Segmento JPEG troncato");
        size_t offset=at;at+=length;
        if((marker>=224&&marker<=239)||marker==254){
            total+=length;if(total>16*1024*1024)bad("Metadati JPEG oltre 16 MiB");Bytes b(input.begin()+offset,input.begin()+at);m.blocks.push_back({uint8_t(marker),b});
            if(marker==225&&starts(b,"Exif\0\0",6)){if(!m.exif.empty())bad("EXIF multipli non supportati");m.exif=b;}
            else if(marker==225&&starts(b,"http://ns.adobe.com/xap/1.0/\0",29)){
                if(!m.xmp.empty())bad("XMP multipli non supportati");m.xmp.assign(b.begin()+29,b.end());std::string text(m.xmp.begin(),m.xmp.end());
                std::transform(text.begin(),text.end(),text.begin(),[](unsigned char c){return std::tolower(c);});
                for(const char* word:{"gainmap","hdrgm"})if(text.find(word)!=std::string::npos)bad("XMP con HDR/gain map non supportato");
                for(const char* word:{"motionphoto","microvideo"})if(text.find(word)!=std::string::npos)motionHint=true;
                directoryHint=text.find("container:directory")!=std::string::npos;
            }else if(marker==226&&starts(b,"ICC_PROFILE\0",12)){
                if(b.size()<14||!b[12]||!b[13]||b[12]>b[13]||(iccParts&&iccParts!=b[13])||icc.count(b[12]))bad("Sequenza ICC non valida");iccParts=b[13];icc[b[12]]=Bytes(b.begin()+14,b.end());
            }else if(marker==224&&starts(b,"JFIF\0",5)){}
            else if(marker==238&&starts(b,"Adobe",5)){}
            else if(marker==254){}
            else if(marker==226&&starts(b,"MPF\0",4)){if(!mpf.empty())bad("MPF multipli");mpf=b;tiffBase=offset+4;}
            else if(marker==225&&starts(b,"http://ns.adobe.com/xmp/extension/\0",35))bad("XMP esteso non ancora supportato");
            else if(marker==235&&!std::all_of(b.begin(),b.end(),[](uint8_t v){return v==0;}))app11.push_back(b);
            // Other JPEG APP/COM payloads have no HEIF semantics. Preserve their complete
            // original bytes/order in the explicitly proprietary archive; never activate them.
        }
        if(marker==192||marker==194){if(length<6)bad("Dimensioni JPEG non valide");height=input[offset+1]*256+input[offset+2];width=input[offset+3]*256+input[offset+4];}
        if(marker==218){while(true){
            if(at==jpegLimit&&sefStart<input.size()){
                // Only a fully indexed and supported SEF suffix supplies a trusted
                // boundary. A plain EOF or arbitrary trailer never permits repair.
                Bytes suffix(input.begin()+sefStart,input.end());
                if(validateJpegTail(suffix)!=JpegTailKind::Sef)bad("Fine JPEG senza SEF verificato");
                size_t zeros=sefStart;while(zeros>0&&input[zeros-1]==0)zeros--;
                if(sefStart-zeros>=32)bad("Confine JPEG/SEF ambiguo: zeri prima del SEF senza marcatore finale");
                m.missingEoi=true;pending=217;break;
            }
            if(next()!=255)continue;int value;do{value=next();}while(value==255);if(value==0||(value>=208&&value<=215))continue;pending=value;break;}}
    }
    validateC2pa(app11);m.c2pa=!app11.empty();
    if(icc.size()!=size_t(iccParts))bad("Profilo ICC incompleto");for(const auto& part:icc)m.icc.insert(m.icc.end(),part.second.begin(),part.second.end());
    if(!m.icc.empty()&&(m.icc.size()<128||memcmp(m.icc.data()+36,"acsp",4)))bad("Header profilo ICC non valido");
    m.orientation=exifOrientation(m.exif);if(m.orientation==0){m.orientationWasZero=true;m.orientation=1;}
    m.jpegEnd=at;m.tail.assign(input.begin()+at,input.end());JpegTailInfo tailInfo;
    if(!mpf.empty())m.gainMap=inspectAppleGainMap(mpf,tiffBase,at,m.tail,width,height);else tailInfo=inspectJpegTail(m.tail);
    m.tailKind=tailInfo.kind;m.motion=tailInfo.motion;m.archivedPanorama=tailInfo.archivedPanorama;m.sefZeroPrefix=tailInfo.zeroPrefix;
    m.externalTail=m.tail.size()-m.motion.length>4*1024*1024;
    if(m.motion.length)m.xmp=motionXmp(m.xmp,input.size()-at-m.motion.offset,at,m.motion);
    else if(motionHint||directoryHint)bad("XMP di contenuti aggiuntivi senza video Motion Photo SEF verificato");
    m.bundle={'C','P','J','P','E','G',uint8_t(m.externalTail?'3':m.motion.length?'2':'1'),0};put32(m.bundle,m.blocks.size());
    for(const Block& b:m.blocks){m.bundle.push_back(b.marker);put32(m.bundle,b.data.size());m.bundle.insert(m.bundle.end(),b.data.begin(),b.data.end());}
    put32(m.bundle,m.tail.size());
    if(m.externalTail){put32(m.bundle,m.motion.offset);put32(m.bundle,m.motion.length);}
    else if(m.motion.length){
        // CPJPEG2 stores the exact original tail minus the video. Its two extra
        // u32 values locate the hole; the final mpvd payload fills it on recovery.
        put32(m.bundle,m.motion.offset);put32(m.bundle,m.motion.length);
        m.bundle.insert(m.bundle.end(),m.tail.begin(),m.tail.begin()+m.motion.offset);
        m.bundle.insert(m.bundle.end(),m.tail.begin()+m.motion.offset+m.motion.length,m.tail.end());
    }else m.bundle.insert(m.bundle.end(),m.tail.begin(),m.tail.end());
    return m;
}
std::string metadataNotes(const Metadata& m){
    std::string notes;
    if(m.missingEoi)notes+=" Marcatore finale JPEG assente prima di un SEF valido: aggiunta alla sola copia privata per il decoder; verifica rigorosa dei pixel obbligatoria prima di accettare l’HEIC. Originale invariato.";
    if(m.tailKind==JpegTailKind::PaddedOpaque)notes+=" Coda con contenuto non identificato e area finale di zeri: conservazione integrale nell’archivio proprietario HEIC; nessuna funzione aggiuntiva è attivata.";
    if(m.orientationWasZero)notes+=" EXIF orientamento 0 non definito: nessuna rotazione applicata, pixel nel loro ordine originale; tag EXIF conservato senza modifiche.";
    if(m.motion.length)notes+=" Motion Photo: video originale di "+std::to_string(m.motion.length)+" byte copiato senza ricodifica nel box HEIC mpvd finale; XMP attivo adattato al nuovo contenitore. XMP e SEF originali ricostruibili esattamente dall’archivio CPJPEG"+std::string(m.externalTail?"3":"2")+" e dal video, senza duplicare il filmato. Riproduzione da verificare nella galleria del dispositivo.";
    if(m.archivedPanorama)notes+=" Contenuto panorama Samsung conservato integralmente nell’archivio proprietario; funzione interattiva Samsung non attivata nell’HEIC.";
    if(m.sefZeroPrefix)notes+=" Prefisso SEF di "+std::to_string(m.sefZeroPrefix)+" byte tutti zero conservato integralmente, senza ammettere buchi o sovrapposizioni tra record.";
    if(m.externalTail)notes+=" Coda JPEG voluminosa conservata nel box UUID privato CPJPEG3, verificato integralmente e senza duplicarlo nell’item metadati HEIF. Nessuna ricodifica dei contenuti Samsung archiviati.";
    if(m.motion.primaryLengthRewritten)notes+=" Length primaria XMP non nulla ricostruita per l’HEIC dal contenitore effettivo; pacchetto originale conservato.";
    if(m.c2pa)notes+=" Manifesto C2PA del JPEG conservato nell’archivio proprietario. Non è una certificazione C2PA del nuovo HEIC e non viene presentato come tale; firma originale non verificata e nessuna nuova firma creata.";
    if(m.gainMap.width)notes+=" Mappa HDR Apple v1 conservata come immagine ausiliaria HEVC monocromatica lossless, collegata con auxl/auxC e XMP originale; la conversione confronta esattamente i pixel della mappa dopo la codifica. JPEG ausiliario originale archiviato integralmente. Resa HDR da verificare nella galleria del dispositivo.";
    return notes;
}
}
