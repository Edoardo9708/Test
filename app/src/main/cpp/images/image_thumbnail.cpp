#include "image_core.h"
#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <utility>

namespace photo {
namespace {
// Average the source area covered by each output pixel, avoiding nearest-neighbour aliasing.
void reducePlane(const heif_image* input,heif_image* output,heif_channel channel){
    int inStride=0,outStride=0;
    const auto* in=heif_image_get_plane_readonly(input,channel,&inStride);
    auto* out=heif_image_get_plane(output,channel,&outStride);
    int iw=heif_image_get_width(input,channel),ih=heif_image_get_height(input,channel);
    int ow=heif_image_get_width(output,channel),oh=heif_image_get_height(output,channel);
    if(!in||!out||iw<=0||ih<=0||ow<=0||oh<=0)throw std::runtime_error("Piano miniatura non valido");
    double sx=double(iw)/ow,sy=double(ih)/oh;
    for(int y=0;y<oh;y++)for(int x=0;x<ow;x++){
        double left=x*sx,right=(x+1)*sx,top=y*sy,bottom=(y+1)*sy,sum=0;
        for(int iy=int(top);iy<std::min(ih,int(std::ceil(bottom)));iy++){
            double wy=std::min(bottom,double(iy+1))-std::max(top,double(iy));
            for(int ix=int(left);ix<std::min(iw,int(std::ceil(right)));ix++){
                double wx=std::min(right,double(ix+1))-std::max(left,double(ix));
                sum+=in[iy*inStride+ix]*wx*wy;
            }
        }
        out[y*outStride+x]=uint8_t(std::clamp(int(std::lround(sum/(sx*sy))),0,255));
    }
}
}
void addThumbnail(heif_context* context,const Source& source,const heif_image_handle* primary,const heif_encoding_options* options){
    int edge=std::max(source.width,source.height);if(edge<=thumbnailEdge)return;
    int w=(source.width*thumbnailEdge/edge)&~1,h=(source.height*thumbnailEdge/edge)&~1;
    heif_image* rawImage=nullptr;check(heif_image_create(w,h,heif_colorspace_YCbCr,heif_chroma_420,&rawImage),"Crea miniatura HEIF");Image image(rawImage);
    for(auto channel:{heif_channel_Y,heif_channel_Cb,heif_channel_Cr}){
        check(heif_image_add_plane(rawImage,channel,channel==heif_channel_Y?w:w/2,channel==heif_channel_Y?h:h/2,8),"Piano miniatura HEIF");
        reducePlane(source.image.get(),rawImage,channel);
    }
    heif_color_profile_nclx* rawProfile=nullptr;check(heif_image_get_nclx_color_profile(source.image.get(),&rawProfile),"Colore miniatura");
    auto profile=std::unique_ptr<heif_color_profile_nclx,void(*)(heif_color_profile_nclx*)>(rawProfile,heif_nclx_color_profile_free);
    check(heif_image_set_nclx_color_profile(rawImage,rawProfile),"Profilo miniatura");
    if(!source.metadata.icc.empty())check(heif_image_set_raw_color_profile(rawImage,"prof",source.metadata.icc.data(),source.metadata.icc.size()),"ICC miniatura");
    heif_encoder* rawEncoder=nullptr;check(heif_context_get_encoder_for_format(context,heif_compression_HEVC,&rawEncoder),"Encoder miniatura");
    auto encoder=std::unique_ptr<heif_encoder,void(*)(heif_encoder*)>(rawEncoder,heif_encoder_release);
    check(heif_encoder_set_lossy_quality(rawEncoder,80),"Qualità miniatura");
    for(auto parameter:{std::pair{"preset","fast"},{"tune","none"},{"chroma","420"},{"x265:pools","2"},{"x265:frame-threads","1"},{"x265:cutree","0"}})
        check(heif_encoder_set_parameter_string(rawEncoder,parameter.first,parameter.second),"Parametro miniatura");
    check(heif_encoder_set_logging_level(rawEncoder,0),"Log miniatura");
    heif_image_handle* rawHandle=nullptr;check(heif_context_encode_image(context,rawImage,rawEncoder,options,&rawHandle),"Codifica miniatura HEIF");Handle handle(rawHandle);
    check(heif_context_assign_thumbnail(context,primary,rawHandle),"Collega miniatura HEIF");
}
}
