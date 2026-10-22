#include "image_core.h"
#include "image_sef.h"
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/pixdesc.h>
}
#include <algorithm>
#include <cstring>
#include <fstream>
#include <map>
#include <stdexcept>
namespace photo {
namespace {
[[noreturn]] void bad(const std::string& message){throw std::runtime_error(message);}
bool starts(const Bytes& b,const char* text,size_t n){return b.size()>=n&&!memcmp(b.data(),text,n);}
void put32(Bytes& b,size_t value){for(int shift=24;shift>=0;shift-=8)b.push_back((value>>shift)&255);}

int orientation(const Bytes& exif){
    if(exif.empty())return 1;
    if(exif.size()<14)bad("EXIF incompleto");size_t base=6;bool little=exif[6]=='I'&&exif[7]=='I';
    if(!little&&!(exif[6]=='M'&&exif[7]=='M'))bad("Ordine byte EXIF non valido");
    auto word=[&](size_t at,int count)->uint32_t{if(at>exif.size()||size_t(count)>exif.size()-at)bad("Riferimento EXIF fuori limite");uint32_t v=0;for(int n=0;n<count;n++)v|=uint32_t(exif[at+n])<<(8*(little?n:count-n-1));return v;};
    if(word(8,2)!=42)bad("Header TIFF EXIF non valido");size_t ifd=base+word(10,4);size_t count=word(ifd,2);if(count>(exif.size()-ifd-2)/12)bad("IFD EXIF troncato");
    int result=1;bool seen=false;
    for(size_t i=0;i<count;i++){size_t p=ifd+2+i*12;if(word(p,2)==0x112){if(seen)bad("Orientamento EXIF non valido: tag duplicato");if(word(p+2,2)!=3||word(p+4,4)!=1)bad("Orientamento EXIF non valido: tipo="+std::to_string(word(p+2,2))+"; quantita="+std::to_string(word(p+4,4)));result=word(p+8,2);seen=true;if(result<1||result>8)bad("Orientamento EXIF fuori intervallo: valore="+std::to_string(result)+"; atteso 1..8");}}
    return result;
}
Metadata metadata(const Bytes& input){
    Metadata m;size_t at=2,total=0;int pending=-1,segments=0;std::map<int,Bytes> icc;int iccParts=0;
    if(input.size()<4||input[0]!=255||input[1]!=216)bad("Input non JPEG");
    auto next=[&](){if(at>=input.size())bad("JPEG troncato: offset="+std::to_string(at)+"; byte totali="+std::to_string(input.size()));return int(input[at++]);};
    while(true){
        int marker=pending;pending=-1;if(marker<0){size_t markerAt=at;int prefix=next();if(prefix!=255)bad("Marcatore JPEG atteso: offset="+std::to_string(markerAt)+"; byte="+std::to_string(prefix));do{marker=next();}while(marker==255);}
        if(++segments>65536)bad("Troppi segmenti JPEG");if(marker==217)break;
        if(marker==216||marker==0||(marker>=208&&marker<=215))bad("Marcatori JPEG non validi");if(marker==1)continue;
        int length=(next()<<8);length+=next();length-=2;if(length<0||size_t(length)>input.size()-at)bad("Segmento JPEG troncato");
        size_t offset=at;at+=length;
        if((marker>=224&&marker<=239)||marker==254){
            total+=length;if(total>16*1024*1024)bad("Metadati JPEG oltre 16 MiB");Bytes b(input.begin()+offset,input.begin()+at);m.blocks.push_back({uint8_t(marker),b});
            if(marker==225&&starts(b,"Exif\0\0",6)){if(!m.exif.empty())bad("EXIF multipli non supportati");m.exif=b;}
            else if(marker==225&&starts(b,"http://ns.adobe.com/xap/1.0/\0",29)){
                if(!m.xmp.empty())bad("XMP multipli non supportati");m.xmp.assign(b.begin()+29,b.end());std::string text(m.xmp.begin(),m.xmp.end());
                std::transform(text.begin(),text.end(),text.begin(),[](unsigned char c){return std::tolower(c);});
                for(const char* word:{"gainmap","hdrgm","motionphoto","microvideo","container:directory"})if(text.find(word)!=std::string::npos)bad("XMP con HDR/gain map/Motion Photo non supportato");
            }else if(marker==226&&starts(b,"ICC_PROFILE\0",12)){
                if(b.size()<14||!b[12]||!b[13]||b[12]>b[13]||(iccParts&&iccParts!=b[13])||icc.count(b[12]))bad("Sequenza ICC non valida");iccParts=b[13];icc[b[12]]=Bytes(b.begin()+14,b.end());
            }else if(marker==224&&starts(b,"JFIF\0",5)){}
            else if(marker==238&&starts(b,"Adobe",5)){}
            else if(marker==254){}
            else if(marker==226&&starts(b,"MPF\0",4))bad("JPEG multipicture/MPF non supportato");
            else if(marker==225&&starts(b,"http://ns.adobe.com/xmp/extension/\0",35))bad("XMP esteso non ancora supportato");
            else if(marker==235&&!std::all_of(b.begin(),b.end(),[](uint8_t v){return v==0;}))bad("APP11/JUMBF non supportato: possibile contenuto HDR aggiuntivo");
            // Other JPEG APP/COM payloads have no HEIF semantics. Preserve their complete
            // original bytes/order in the explicitly proprietary archive; never activate them.
        }
        if(marker==218){while(true){if(next()!=255)continue;int value;do{value=next();}while(value==255);if(value==0||(value>=208&&value<=215))continue;pending=value;break;}}
    }
    if(icc.size()!=size_t(iccParts))bad("Profilo ICC incompleto");for(const auto& part:icc)m.icc.insert(m.icc.end(),part.second.begin(),part.second.end());
    if(!m.icc.empty()&&(m.icc.size()<128||memcmp(m.icc.data()+36,"acsp",4)))bad("Header profilo ICC non valido");
    m.orientation=orientation(m.exif);m.tail.assign(input.begin()+at,input.end());validateJpegTail(m.tail);
    m.bundle={'C','P','J','P','E','G','1',0};put32(m.bundle,m.blocks.size());
    for(const Block& b:m.blocks){m.bundle.push_back(b.marker);put32(m.bundle,b.data.size());m.bundle.insert(m.bundle.end(),b.data.begin(),b.data.end());}
    put32(m.bundle,m.tail.size());m.bundle.insert(m.bundle.end(),m.tail.begin(),m.tail.end());return m;
}
}
Bytes readFile(const std::string& path,size_t limit){std::ifstream file(path,std::ios::binary|std::ios::ate);if(!file)bad("File non leggibile");auto size=file.tellg();if(size<=0||uint64_t(size)>limit)bad("Dimensione file oltre il limite");Bytes bytes(static_cast<size_t>(size));file.seekg(0);if(!file.read(reinterpret_cast<char*>(bytes.data()),bytes.size()))bad("Lettura file incompleta");return bytes;}
void check(heif_error error,const char* action){if(error.code)bad(std::string(action)+": "+error.message);}
std::string inspectJpeg(const std::string& path){
    // Same metadata parser as conversion, with no FFmpeg, libheif or x265 calls.
    Bytes input=readFile(path,64*1024*1024);Metadata m=metadata(input);
    return "Metadati compatibili · blocchi APP/COM="+std::to_string(m.blocks.size())+
        "; EXIF="+std::to_string(m.exif.size())+" byte; XMP="+std::to_string(m.xmp.size())+
        " byte; ICC="+std::to_string(m.icc.size())+" byte; coda JPEG="+std::to_string(m.tail.size())+
        " byte; orientamento="+std::to_string(m.orientation)+
        ". Pixel compressi non decodificati: integrità e formato colore restano da verificare in conversione.";
}
Source loadJpeg(const std::string& path){
    Bytes input=readFile(path,64*1024*1024);Source source;source.metadata=metadata(input);
    const AVCodec* codec=avcodec_find_decoder(AV_CODEC_ID_MJPEG);if(!codec)bad("Decoder JPEG incluso assente");
    auto ctx=std::unique_ptr<AVCodecContext,void(*)(AVCodecContext*)>(avcodec_alloc_context3(codec),[](AVCodecContext* p){avcodec_free_context(&p);});
    auto frame=std::unique_ptr<AVFrame,void(*)(AVFrame*)>(av_frame_alloc(),[](AVFrame* p){av_frame_free(&p);});
    auto packet=std::unique_ptr<AVPacket,void(*)(AVPacket*)>(av_packet_alloc(),[](AVPacket* p){av_packet_free(&p);});
    if(!ctx||!frame||!packet)throw std::bad_alloc();ctx->thread_count=2;ctx->max_pixels=48000000;ctx->err_recognition=AV_EF_EXPLODE|AV_EF_CAREFUL;
    if(avcodec_open2(ctx.get(),codec,nullptr)<0||av_new_packet(packet.get(),input.size())<0)bad("Apertura decoder JPEG fallita");
    memcpy(packet->data,input.data(),input.size());if(avcodec_send_packet(ctx.get(),packet.get())<0||avcodec_receive_frame(ctx.get(),frame.get())<0)bad("Decodifica JPEG fallita");
    source.width=frame->width;source.height=frame->height;if(source.width<64||source.height<64||source.width>16384||source.height>16384||int64_t(source.width)*source.height>48000000)bad("JPEG supportati: lati da 64 a 16384 pixel, massimo 48 megapixel");
    int sx=0,sy=0;heif_chroma chroma;
    switch(frame->format){case AV_PIX_FMT_YUVJ420P:case AV_PIX_FMT_YUV420P:chroma=heif_chroma_420;sx=sy=1;break;
        case AV_PIX_FMT_YUVJ422P:case AV_PIX_FMT_YUV422P:chroma=heif_chroma_422;sx=1;break;
        case AV_PIX_FMT_YUVJ444P:case AV_PIX_FMT_YUV444P:chroma=heif_chroma_444;break;
        default:bad("JPEG non YCbCr a 8 bit: CMYK, RGB, scala di grigi e precisioni superiori non ancora supportati");}
    if(frame->color_range!=AVCOL_RANGE_JPEG)bad("Range JPEG non riconosciuto come completo");
    if(frame->colorspace!=AVCOL_SPC_UNSPECIFIED&&frame->colorspace!=AVCOL_SPC_BT470BG&&frame->colorspace!=AVCOL_SPC_SMPTE170M)bad("Matrice JPEG non supportata");
    source.matrix=frame->colorspace==AVCOL_SPC_BT470BG?5:6;source.primaries=frame->color_primaries;source.transfer=frame->color_trc;
    heif_image* raw=nullptr;check(heif_image_create(source.width,source.height,heif_colorspace_YCbCr,chroma,&raw),"Crea immagine");source.image.reset(raw);
    for(int n=0;n<3;n++){auto channel=n==0?heif_channel_Y:n==1?heif_channel_Cb:heif_channel_Cr;int w=n?(source.width+(1<<sx)-1)>>sx:source.width,h=n?(source.height+(1<<sy)-1)>>sy:source.height;
        check(heif_image_add_plane(raw,channel,w,h,8),"Crea piano");int stride=0;uint8_t* dest=heif_image_get_plane(raw,channel,&stride);if(!dest)bad("Piano immagine assente");
        for(int y=0;y<h;y++)memcpy(dest+y*stride,frame->data[n]+y*frame->linesize[n],w);
    }
    auto profile=std::unique_ptr<heif_color_profile_nclx,void(*)(heif_color_profile_nclx*)>(heif_nclx_color_profile_alloc(),heif_nclx_color_profile_free);if(!profile)throw std::bad_alloc();
    check(heif_nclx_color_profile_set_color_primaries(profile.get(),source.primaries),"Primarie");check(heif_nclx_color_profile_set_transfer_characteristics(profile.get(),source.transfer),"Trasferimento");check(heif_nclx_color_profile_set_matrix_coefficients(profile.get(),source.matrix),"Matrice");profile->full_range_flag=1;
    check(heif_image_set_nclx_color_profile(raw,profile.get()),"Profilo NCLX");if(!source.metadata.icc.empty())check(heif_image_set_raw_color_profile(raw,"prof",source.metadata.icc.data(),source.metadata.icc.size()),"Profilo ICC");
    return source;
}
}
