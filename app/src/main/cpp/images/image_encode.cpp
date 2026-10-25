#include "image_core.h"
#include "image_limits.h"
#include <libheif/heif_tiling.h>
#include <cstdio>
#include <fstream>
#include <mutex>
#include <stdexcept>
namespace photo {
namespace {
Handle encodePrimary(heif_context* context,const Source& source,heif_encoder* encoder,
                     const heif_encoding_options* options,const Stage& stage){
    heif_image_handle* raw=nullptr;
    if(int64_t((source.width+1)&~1)*((source.height+1)&~1)<=maxSingleHevcPixels){
        check(heif_context_encode_image(context,source.image.get(),encoder,options,&raw),"Codifica HEIC");
        return Handle(raw);
    }
    // Equal, even tile sizes preserve chroma alignment. The grid stores the exact
    // original extent, so padding on the right/bottom is never displayed.
    int tileWidth=((source.width+3)/4)*2,tileHeight=((source.height+3)/4)*2;
    // Unlike encode_image(), add_grid_image() does not infer the output NCLX
    // profile from the source. Supply it explicitly for the grid and its tiles.
    heif_color_profile_nclx* rawProfile=nullptr;
    check(heif_image_get_nclx_color_profile(source.image.get(),&rawProfile),"Colore griglia HEIC");
    auto profile=std::unique_ptr<heif_color_profile_nclx,void(*)(heif_color_profile_nclx*)>(rawProfile,heif_nclx_color_profile_free);
    heif_encoding_options gridOptions=*options;gridOptions.output_nclx_profile=profile.get();
    check(heif_context_add_grid_image(context,source.width,source.height,2,2,&gridOptions,&raw),"Crea griglia HEIC");
    Handle grid(raw);
    for(int y=0;y<2;y++)for(int x=0;x<2;x++){
        stage("Codifica HEIC · riquadro "+std::to_string(y*2+x+1)+"/4");
        heif_image* rawTile=nullptr;
        check(heif_image_extract_area(source.image.get(),x*tileWidth,y*tileHeight,tileWidth,tileHeight,
                                     heif_get_global_security_limits(),&rawTile),"Estrai riquadro HEIC");
        Image tile(rawTile);
        check(heif_context_add_image_tile(context,grid.get(),x,y,tile.get(),encoder),"Codifica riquadro HEIC");
    }
    return grid;
}
}
void attach(heif_context* context,heif_image_handle* handle,const Metadata& m){
    if(!m.exif.empty())check(heif_context_add_exif_metadata(context,handle,m.exif.data(),m.exif.size()),"Copia EXIF");
    if(!m.xmp.empty())check(heif_context_add_XMP_metadata(context,handle,m.xmp.data(),m.xmp.size()),"Copia XMP");
    check(heif_context_add_generic_metadata(context,handle,m.bundle.data(),m.bundle.size(),"mime","application/vnd.codecprobe.jpeg-metadata"),"Archivio metadati JPEG");
}
std::string convert(const std::string& input,const std::string& output,int quality,const std::string& preset,const std::string& tune,const Stage& stage){
    if(input==output||quality<40||quality>95||(preset!="fast"&&preset!="medium"&&preset!="slow")||(tune!="standard"&&tune!="ssim"&&tune!="psnr"))throw std::runtime_error("Parametri foto non validi");
    {std::ifstream old(output,std::ios::binary|std::ios::ate);if(old&&old.tellg()>0)throw std::runtime_error("Destinazione temporanea non vuota");}
    try{
        static std::once_flag initialized;std::call_once(initialized,[]{check(heif_init(nullptr),"Inizializzazione libheif");});
        stage("Analisi e decodifica JPEG");Source source=loadJpeg(input);stage("Codifica HEIC con x265");
        Context context(heif_context_alloc());if(!context)throw std::bad_alloc();heif_encoder* rawEncoder=nullptr;
        check(heif_context_get_encoder_for_format(context.get(),heif_compression_HEVC,&rawEncoder),"Encoder HEVC");
        auto encoder=std::unique_ptr<heif_encoder,void(*)(heif_encoder*)>(rawEncoder,heif_encoder_release);
        check(heif_encoder_set_lossy_quality(encoder.get(),quality),"Qualità");check(heif_encoder_set_parameter_string(encoder.get(),"preset",preset.c_str()),"Preset");
        check(heif_encoder_set_parameter_string(encoder.get(),"tune",tune=="standard"?"none":tune.c_str()),"Tune");
        check(heif_encoder_set_parameter_string(encoder.get(),"chroma","420"),"Sottocampionamento");
        check(heif_encoder_set_parameter_string(encoder.get(),"x265:pools","2"),"Thread pool");
        check(heif_encoder_set_parameter_string(encoder.get(),"x265:frame-threads","1"),"Thread frame");
        check(heif_encoder_set_parameter_string(encoder.get(),"x265:cutree","0"),"CU-tree foto");
        check(heif_encoder_set_logging_level(encoder.get(),0),"Log encoder");
        auto options=std::unique_ptr<heif_encoding_options,void(*)(heif_encoding_options*)>(heif_encoding_options_alloc(),heif_encoding_options_free);if(!options)throw std::bad_alloc();
        options->image_orientation=static_cast<heif_orientation>(source.metadata.orientation);options->save_two_colr_boxes_when_ICC_and_nclx_available=1;
        Handle handle=encodePrimary(context.get(),source,encoder.get(),options.get(),stage);
        if(source.gainMap){stage("Copia lossless mappa HDR Apple");encodeGainMap(context.get(),handle.get(),source,options.get());}
        stage("Generazione miniatura HEIF");addThumbnail(context.get(),source,handle.get(),options.get());
        stage("Copia metadati HEIC");attach(context.get(),handle.get(),source.metadata);check(heif_context_write_to_file(context.get(),output.c_str()),"Scrittura HEIC");
        if(source.metadata.externalTail)appendArchivedTail(output,source.metadata.tail,source.metadata.motion);
        appendMotionVideo(output,source.metadata.tail,source.metadata.motion);
        stage("Verifica HEIC, pixel e metadati");verify(output,source,stage);
        return "OK HEIC · libheif "+std::string(heif_get_version())+" · "+heif_encoder_get_name(encoder.get())+" · qualità="+std::to_string(quality)+" · CRF="+std::to_string((100-quality)/2.0)+" · preset="+preset+" · tune="+tune+" · YUV420 8 bit full range · orientamento="+std::to_string(source.metadata.orientation)+" · APP/COM="+std::to_string(source.metadata.blocks.size())+" · coda JPEG="+std::to_string(source.metadata.tail.size())+" byte. "+(source.width>thumbnailEdge||source.height>thumbnailEdge?"Miniatura HEIF dedicata fino a 1024 pixel, collegata e decodificata. ":"Immagine entro 1024 pixel: nessuna miniatura aggiuntiva. ")+metadataNotes(source.metadata)+" EXIF/ICC, XMP attivo e archivio originali riletti e confrontati byte per byte; immagine decodificata completamente. I dati Samsung archiviati non garantiscono le funzioni dell’editor Samsung.";
    }catch(...){std::remove(output.c_str());throw;}
}
}
