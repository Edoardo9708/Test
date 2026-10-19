#include "image_core.h"
#include <libheif/heif_properties.h>
#include <cstring>
#include <stdexcept>
namespace photo {
namespace {
void require(bool value,const char* message){if(!value)throw std::runtime_error(message);}
Bytes metadata(const heif_image_handle* handle,heif_item_id id){size_t size=heif_image_handle_get_metadata_size(handle,id);require(size<=24*1024*1024,"Metadato HEIC oltre limite");Bytes b(size);check(heif_image_handle_get_metadata(handle,id,b.data()),"Rilettura metadati");return b;}
}
void verify(const std::string& path,const Source& source,const Stage& stage){
    Context context(heif_context_alloc());if(!context)throw std::bad_alloc();heif_context_set_maximum_image_size_limit(context.get(),16384);
    check(heif_context_read_from_file(context.get(),path.c_str(),nullptr),"Riapertura HEIC");
    require(heif_context_get_number_of_top_level_images(context.get())==1,"Numero immagini HEIC diverso da uno");
    heif_image_handle* raw=nullptr;check(heif_context_get_primary_image_handle(context.get(),&raw),"Immagine principale");Handle handle(raw);
    int w=source.metadata.orientation>=5?source.height:source.width,h=source.metadata.orientation>=5?source.width:source.height;
    require(heif_image_handle_get_width(raw)==w&&heif_image_handle_get_height(raw)==h,"Dimensioni o orientamento HEIC diversi");
    require(heif_image_handle_get_luma_bits_per_pixel(raw)==8,"Precisione HEIC diversa da 8 bit");
    heif_item_id primary;check(heif_context_get_primary_image_ID(context.get(),&primary),"ID immagine principale");
    const int rotations[]={0,0,0,180,0,270,270,270,90},mirrors[]={-1,-1,1,-1,0,1,-1,0,-1};int rotation=rotations[source.metadata.orientation],mirror=mirrors[source.metadata.orientation];
    require(heif_item_get_properties_of_type(context.get(),primary,heif_item_property_type_transform_rotation,nullptr,0)==int(rotation!=0),"Rotazione HEIC mancante o duplicata");
    require(heif_item_get_properties_of_type(context.get(),primary,heif_item_property_type_transform_mirror,nullptr,0)==int(mirror>=0),"Specchiatura HEIC mancante o duplicata");
    if(rotation)require(heif_item_get_property_transform_rotation_ccw(context.get(),primary,0)==rotation,"Rotazione HEIC diversa da EXIF");
    if(mirror>=0)require(int(heif_item_get_property_transform_mirror(context.get(),primary,0))==mirror,"Specchiatura HEIC diversa da EXIF");
    std::vector<heif_item_id> ids(heif_image_handle_get_number_of_metadata_blocks(raw,nullptr));require(ids.size()<=8,"Metadati HEIC inattesi");
    heif_image_handle_get_list_of_metadata_block_IDs(raw,nullptr,ids.data(),ids.size());int exif=0,xmp=0,bundle=0;
    for(auto id:ids){
        const char* type=heif_image_handle_get_metadata_type(raw,id);const char* content=heif_image_handle_get_metadata_content_type(raw,id);Bytes b=metadata(raw,id);
        if(type&&!strcmp(type,"Exif")){require(b.size()==source.metadata.exif.size()+4&&b.size()>=4,"Dimensione EXIF HEIC diversa");require(b[0]==0&&b[1]==0&&b[2]==0&&b[3]==6,"Offset TIFF HEIC diverso");require(Bytes(b.begin()+4,b.end())==source.metadata.exif,"EXIF HEIC diversi");exif++;}
        else if(content&&!strcmp(content,"application/rdf+xml")){require(b==source.metadata.xmp,"XMP HEIC diversi");xmp++;}
        else if(content&&!strcmp(content,"application/vnd.codecprobe.jpeg-metadata")){require(b==source.metadata.bundle,"Archivio APP/COM/SEF diverso");bundle++;}
        else throw std::runtime_error("Metadato HEIC inatteso");
    }
    require(exif==int(!source.metadata.exif.empty())&&xmp==int(!source.metadata.xmp.empty())&&bundle==1,"Metadati HEIC mancanti o duplicati");
    Bytes icc(heif_image_handle_get_raw_color_profile_size(raw));require(icc.size()<=16*1024*1024,"ICC HEIC troppo grande");if(!icc.empty())check(heif_image_handle_get_raw_color_profile(raw,icc.data()),"Rilettura ICC");require(icc==source.metadata.icc,"Profilo ICC diverso");
    heif_color_profile_nclx* rawProfile=nullptr;check(heif_image_handle_get_nclx_color_profile(raw,&rawProfile),"Rilettura NCLX");
    auto profile=std::unique_ptr<heif_color_profile_nclx,void(*)(heif_color_profile_nclx*)>(rawProfile,heif_nclx_color_profile_free);
    require(rawProfile&&rawProfile->full_range_flag&&int(rawProfile->matrix_coefficients)==source.matrix&&int(rawProfile->color_primaries)==source.primaries&&int(rawProfile->transfer_characteristics)==source.transfer,"Dichiarazioni colore HEIC diverse");
    auto options=std::unique_ptr<heif_decoding_options,void(*)(heif_decoding_options*)>(heif_decoding_options_alloc(),heif_decoding_options_free);if(!options)throw std::bad_alloc();options->ignore_transformations=0;options->strict_decoding=1;
    heif_image* decoded=nullptr;check(heif_decode_image(raw,&decoded,heif_colorspace_YCbCr,heif_chroma_420,options.get()),"Decodifica completa HEIC con FFmpeg");Image image(decoded);
    require(heif_image_get_primary_width(decoded)==w&&heif_image_get_primary_height(decoded)==h,"Dimensioni pixel HEIC diverse");
    require(heif_image_get_bits_per_pixel_range(decoded,heif_channel_Y)==8,"Precisione pixel decodificati diversa");
    for(auto channel:{heif_channel_Y,heif_channel_Cb,heif_channel_Cr}){int stride;const uint8_t* plane=heif_image_get_plane_readonly(decoded,channel,&stride);int width=heif_image_get_width(decoded,channel),height=heif_image_get_height(decoded,channel);require(plane&&width>0&&height>0&&stride>=width,"Piano HEIC decodificato non valido");}
    stage("HEIC verificato");
}
}
