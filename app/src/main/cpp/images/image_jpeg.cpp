#include "image_core.h"
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/error.h>
#include <libavutil/pixdesc.h>
}
#include <algorithm>
#include <cstring>
#include <fstream>
#include <stdexcept>
namespace photo {
namespace {
[[noreturn]] void bad(const std::string& message){throw std::runtime_error(message);}
void checkJpegDecoder(int result,const char* action,size_t inputBytes){
    if(result>=0)return;
    char reason[AV_ERROR_MAX_STRING_SIZE]={};
    av_strerror(result,reason,sizeof(reason));
    bad(std::string(action)+": "+reason+"; codice="+std::to_string(result)+
        "; byte JPEG="+std::to_string(inputBytes)+"; FFmpeg="+av_version_info());
}
}
Bytes readFile(const std::string& path,size_t limit){std::ifstream file(path,std::ios::binary|std::ios::ate);if(!file)bad("File non leggibile");auto size=file.tellg();if(size<=0||uint64_t(size)>limit)bad("Dimensione file oltre il limite");Bytes bytes(static_cast<size_t>(size));file.seekg(0);if(!file.read(reinterpret_cast<char*>(bytes.data()),bytes.size()))bad("Lettura file incompleta");return bytes;}
void check(heif_error error,const char* action){if(error.code)bad(std::string(action)+": "+error.message);}
std::string inspectJpeg(const std::string& path){
    // Same metadata parser as conversion, with no FFmpeg, libheif or x265 calls.
    Bytes input=readFile(path,64*1024*1024);Metadata m=readJpegMetadata(input);
    std::string headline="Metadati compatibili";
    if(m.gainMap.width)headline+=" · mappa HDR Apple";
    if(m.c2pa)headline+=" · C2PA originale archiviato";
    if(m.motion.length)headline+=" · Motion Photo con video";
    if(m.orientationWasZero)headline+=" · orientamento non definito";
    if(m.archivedPanorama)headline+=" · panorama archiviato";
    if(m.missingEoi)headline+=" · marcatore finale da ricostruire";
    if(m.tailKind==JpegTailKind::PaddedOpaque)headline+=" · coda non identificata archiviabile";
    return headline+" · pixel non verificati\nBlocchi APP/COM="+std::to_string(m.blocks.size())+
        "; EXIF="+std::to_string(m.exif.size())+" byte; XMP="+std::to_string(m.xmp.size())+
        " byte; ICC="+std::to_string(m.icc.size())+" byte; coda JPEG="+std::to_string(m.tail.size())+
        " byte; orientamento="+std::to_string(m.orientation)+
        "."+metadataNotes(m)+" Pixel compressi non decodificati: integrità e formato colore restano da verificare in conversione.";
}
Source loadJpeg(const std::string& path){
    Bytes input=readFile(path,64*1024*1024);Source source;source.metadata=readJpegMetadata(input);
    input.resize(source.metadata.jpegEnd);input.shrink_to_fit();
    if(source.metadata.missingEoi){input.push_back(255);input.push_back(217);}
    const AVCodec* codec=avcodec_find_decoder(AV_CODEC_ID_MJPEG);if(!codec)bad("Decoder JPEG incluso assente");
    auto ctx=std::unique_ptr<AVCodecContext,void(*)(AVCodecContext*)>(avcodec_alloc_context3(codec),[](AVCodecContext* p){avcodec_free_context(&p);});
    auto frame=std::unique_ptr<AVFrame,void(*)(AVFrame*)>(av_frame_alloc(),[](AVFrame* p){av_frame_free(&p);});
    auto packet=std::unique_ptr<AVPacket,void(*)(AVPacket*)>(av_packet_alloc(),[](AVPacket* p){av_packet_free(&p);});
    if(!ctx||!frame||!packet)throw std::bad_alloc();ctx->thread_count=2;ctx->max_pixels=48000000;ctx->err_recognition=AV_EF_EXPLODE|AV_EF_CAREFUL;
    checkJpegDecoder(avcodec_open2(ctx.get(),codec,nullptr),"Apertura decoder JPEG fallita",input.size());
    checkJpegDecoder(av_new_packet(packet.get(),input.size()),"Allocazione pacchetto JPEG fallita",input.size());
    memcpy(packet->data,input.data(),input.size());
    checkJpegDecoder(avcodec_send_packet(ctx.get(),packet.get()),"Decodifica JPEG fallita (invio dati)",input.size());
    checkJpegDecoder(avcodec_receive_frame(ctx.get(),frame.get()),"Decodifica JPEG fallita (ricezione immagine)",input.size());
    if(frame->decode_error_flags||frame->flags&AV_FRAME_FLAG_CORRUPT)bad("JPEG con errori di decodifica o pixel ricostruiti dal decoder");
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
    if(source.metadata.gainMap.width)source.gainMap=decodeGainMap(source.metadata);
    return source;
}
}
