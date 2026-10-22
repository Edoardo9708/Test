#include "image_core.h"
#include <libheif/heif_properties.h>
extern "C" {
#include <libavcodec/avcodec.h>
heif_error codecprobe_assign_apple_gainmap(heif_context*,const heif_image_handle*,const heif_image_handle*);
}
#include <cstring>
#include <stdexcept>
namespace photo {
namespace {
void require(bool ok,const char* message){if(!ok)throw std::runtime_error(message);}
}
Image decodeGainMap(const Metadata& m){
    const auto& map=m.gainMap;const AVCodec* codec=avcodec_find_decoder(AV_CODEC_ID_MJPEG);require(codec,"Decoder gain map assente");
    auto ctx=std::unique_ptr<AVCodecContext,void(*)(AVCodecContext*)>(avcodec_alloc_context3(codec),[](AVCodecContext* p){avcodec_free_context(&p);});
    auto frame=std::unique_ptr<AVFrame,void(*)(AVFrame*)>(av_frame_alloc(),[](AVFrame* p){av_frame_free(&p);});
    auto packet=std::unique_ptr<AVPacket,void(*)(AVPacket*)>(av_packet_alloc(),[](AVPacket* p){av_packet_free(&p);});
    if(!ctx||!frame||!packet)throw std::bad_alloc();ctx->max_pixels=16000000;ctx->thread_count=1;ctx->err_recognition=AV_EF_EXPLODE|AV_EF_CAREFUL;
    require(avcodec_open2(ctx.get(),codec,nullptr)>=0&&av_new_packet(packet.get(),m.tail.size())>=0,"Apertura decoder gain map fallita");
    memcpy(packet->data,m.tail.data(),m.tail.size());require(avcodec_send_packet(ctx.get(),packet.get())>=0&&avcodec_receive_frame(ctx.get(),frame.get())>=0,"Decodifica gain map fallita");
    require(!frame->decode_error_flags&&!(frame->flags&AV_FRAME_FLAG_CORRUPT)&&frame->format==AV_PIX_FMT_GRAY8&&frame->width==map.width&&frame->height==map.height,"Pixel gain map non validi");
    heif_image* raw=nullptr;check(heif_image_create(map.width,map.height,heif_colorspace_monochrome,heif_chroma_monochrome,&raw),"Crea gain map");Image image(raw);
    check(heif_image_add_plane(raw,heif_channel_Y,map.width,map.height,8),"Piano gain map");int stride=0;auto dest=heif_image_get_plane(raw,heif_channel_Y,&stride);require(dest&&stride>=map.width,"Piano gain map assente");
    for(int y=0;y<map.height;y++)memcpy(dest+y*stride,frame->data[0]+y*frame->linesize[0],map.width);
    auto profile=std::unique_ptr<heif_color_profile_nclx,void(*)(heif_color_profile_nclx*)>(heif_nclx_color_profile_alloc(),heif_nclx_color_profile_free);if(!profile)throw std::bad_alloc();
    check(heif_nclx_color_profile_set_color_primaries(profile.get(),2),"Primarie gain map");check(heif_nclx_color_profile_set_transfer_characteristics(profile.get(),2),"Trasferimento gain map");check(heif_nclx_color_profile_set_matrix_coefficients(profile.get(),2),"Matrice gain map");profile->full_range_flag=1;
    check(heif_image_set_nclx_color_profile(raw,profile.get()),"Profilo gain map");return image;
}
void encodeGainMap(heif_context* context,const heif_image_handle* primary,const Source& source,const heif_encoding_options* options){
    heif_encoder* raw=nullptr;check(heif_context_get_encoder_for_format(context,heif_compression_HEVC,&raw),"Encoder gain map");
    auto encoder=std::unique_ptr<heif_encoder,void(*)(heif_encoder*)>(raw,heif_encoder_release);
    check(heif_encoder_set_lossless(raw,1),"Gain map lossless");check(heif_encoder_set_parameter_string(raw,"preset","fast"),"Preset gain map");
    check(heif_encoder_set_parameter_string(raw,"x265:pools","2"),"Thread gain map");check(heif_encoder_set_parameter_string(raw,"x265:frame-threads","1"),"Frame gain map");check(heif_encoder_set_parameter_string(raw,"x265:cutree","0"),"CU-tree gain map");check(heif_encoder_set_logging_level(raw,0),"Log gain map");
    heif_image_handle* rawHandle=nullptr;check(heif_context_encode_image(context,source.gainMap.get(),raw,options,&rawHandle),"Codifica gain map");Handle auxiliary(rawHandle);
    const auto& xmp=source.metadata.gainMap.xmp;check(heif_context_add_XMP_metadata(context,rawHandle,xmp.data(),xmp.size()),"XMP gain map");
    check(codecprobe_assign_apple_gainmap(context,primary,rawHandle),"Collegamento gain map");
}
void verifyGainMap(heif_context* context,const heif_image_handle* primary,const Source& source){
    bool expected=source.metadata.gainMap.width!=0;require(heif_image_handle_get_number_of_auxiliary_images(primary,0)==int(expected),"Gain map mancante o immagini ausiliarie inattese");if(!expected)return;
    heif_item_id id;require(heif_image_handle_get_list_of_auxiliary_image_IDs(primary,0,&id,1)==1,"Riferimento gain map mancante");heif_image_handle* raw=nullptr;check(heif_image_handle_get_auxiliary_image_handle(primary,id,&raw),"Rilettura gain map");Handle handle(raw);
    const char* type=nullptr;check(heif_image_handle_get_auxiliary_type(raw,&type),"Tipo gain map");bool correct=type&&!strcmp(type,appleGainMapType);heif_image_handle_release_auxiliary_type(raw,&type);require(correct,"Tipo gain map diverso");
    const int rotations[]={0,0,0,180,0,270,270,270,90},mirrors[]={-1,-1,1,-1,0,1,-1,0,-1};int rotation=rotations[source.metadata.orientation],mirror=mirrors[source.metadata.orientation];
    require(heif_item_get_properties_of_type(context,id,heif_item_property_type_transform_rotation,nullptr,0)==int(rotation!=0),"Rotazione gain map mancante o duplicata");
    require(heif_item_get_properties_of_type(context,id,heif_item_property_type_transform_mirror,nullptr,0)==int(mirror>=0),"Specchiatura gain map mancante o duplicata");
    if(rotation)require(heif_item_get_property_transform_rotation_ccw(context,id,0)==rotation,"Rotazione gain map diversa");
    if(mirror>=0)require(int(heif_item_get_property_transform_mirror(context,id,0))==mirror,"Specchiatura gain map diversa");
    const auto& map=source.metadata.gainMap;int w=map.width,h=map.height;if(source.metadata.orientation>=5)std::swap(w,h);require(heif_image_handle_get_width(raw)==w&&heif_image_handle_get_height(raw)==h,"Dimensioni gain map diverse");
    require(heif_image_handle_get_number_of_metadata_blocks(raw,nullptr)==1,"Metadati gain map inattesi");heif_item_id meta;require(heif_image_handle_get_list_of_metadata_block_IDs(raw,nullptr,&meta,1)==1,"XMP gain map assente");
    const char* content=heif_image_handle_get_metadata_content_type(raw,meta);require(content&&!strcmp(content,"application/rdf+xml")&&heif_image_handle_get_metadata_size(raw,meta)==map.xmp.size(),"XMP gain map diverso");Bytes xmp(map.xmp.size());check(heif_image_handle_get_metadata(raw,meta,xmp.data()),"Rilettura XMP gain map");require(xmp==map.xmp,"XMP gain map modificato");
    auto options=std::unique_ptr<heif_decoding_options,void(*)(heif_decoding_options*)>(heif_decoding_options_alloc(),heif_decoding_options_free);if(!options)throw std::bad_alloc();options->ignore_transformations=1;options->strict_decoding=1;
    heif_image* decoded=nullptr;check(heif_decode_image(raw,&decoded,heif_colorspace_monochrome,heif_chroma_monochrome,options.get()),"Decodifica gain map");Image image(decoded);
    require(heif_image_get_primary_width(decoded)==map.width&&heif_image_get_primary_height(decoded)==map.height&&heif_image_get_bits_per_pixel_range(decoded,heif_channel_Y)==8,"Formato gain map diverso");
    int srcStride=0,dstStride=0;auto src=heif_image_get_plane_readonly(source.gainMap.get(),heif_channel_Y,&srcStride);auto dst=heif_image_get_plane_readonly(decoded,heif_channel_Y,&dstStride);require(src&&dst&&srcStride>=map.width&&dstStride>=map.width,"Piani gain map assenti");
    for(int y=0;y<map.height;y++)require(!memcmp(src+y*srcStride,dst+y*dstStride,map.width),"Pixel gain map modificati dalla codifica");
}
}
