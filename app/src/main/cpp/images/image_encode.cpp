#include "image_core.h"
#include <cstdio>
#include <fstream>
#include <mutex>
#include <stdexcept>
namespace photo {
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
        heif_image_handle* rawHandle=nullptr;check(heif_context_encode_image(context.get(),source.image.get(),encoder.get(),options.get(),&rawHandle),"Codifica HEIC");Handle handle(rawHandle);
        stage("Generazione miniatura HEIF");addThumbnail(context.get(),source,handle.get(),options.get());
        stage("Copia metadati HEIC");attach(context.get(),handle.get(),source.metadata);check(heif_context_write_to_file(context.get(),output.c_str()),"Scrittura HEIC");
        stage("Verifica HEIC, pixel e metadati");verify(output,source,stage);
        return "OK HEIC · libheif "+std::string(heif_get_version())+" · "+heif_encoder_get_name(encoder.get())+" · qualità="+std::to_string(quality)+" · CRF="+std::to_string((100-quality)/2.0)+" · preset="+preset+" · tune="+tune+" · YUV420 8 bit full range · orientamento="+std::to_string(source.metadata.orientation)+" · APP/COM="+std::to_string(source.metadata.blocks.size())+" · SEF="+std::to_string(source.metadata.tail.size())+" byte. "+(source.width>thumbnailEdge||source.height>thumbnailEdge?"Miniatura HEIF dedicata fino a 1024 pixel, collegata e decodificata. ":"Immagine entro 1024 pixel: nessuna miniatura aggiuntiva. ")+"EXIF/XMP/ICC e archivio originali riletti e confrontati byte per byte; immagine decodificata completamente. I dati Samsung archiviati non garantiscono le funzioni dell’editor Samsung.";
    }catch(...){std::remove(output.c_str());throw;}
}
}
