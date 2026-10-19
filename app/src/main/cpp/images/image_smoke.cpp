#include "image_core.h"
#include <iostream>
#include <fstream>
int main(int argc,char** argv){try{
    if(argc==4&&std::string(argv[1])=="verify"){photo::check(heif_init(nullptr),"Init");auto source=photo::loadJpeg(argv[2]);photo::verify(argv[3],source,[](const std::string&){});std::cout<<"OK existing HEIC"<<std::endl;return 0;}
    if(argc==4&&std::string(argv[1])=="decode"){
        photo::check(heif_init(nullptr),"Init");photo::Context context(heif_context_alloc());photo::check(heif_context_read_from_file(context.get(),argv[2],nullptr),"Read");heif_image_handle* rawHandle;photo::check(heif_context_get_primary_image_handle(context.get(),&rawHandle),"Handle");photo::Handle handle(rawHandle);
        auto options=std::unique_ptr<heif_decoding_options,void(*)(heif_decoding_options*)>(heif_decoding_options_alloc(),heif_decoding_options_free);options->ignore_transformations=1;heif_image* raw;photo::check(heif_decode_image(rawHandle,&raw,heif_colorspace_RGB,heif_chroma_interleaved_RGB,options.get()),"Decode RGB");photo::Image image(raw);int w=heif_image_get_primary_width(raw),h=heif_image_get_primary_height(raw),stride;auto plane=heif_image_get_plane_readonly(raw,heif_channel_interleaved,&stride);std::ofstream out(argv[3],std::ios::binary);out<<"P6\n"<<w<<" "<<h<<"\n255\n";for(int y=0;y<h;y++)out.write(reinterpret_cast<const char*>(plane+y*stride),w*3);if(!out)throw std::runtime_error("Write PPM");return 0;
    }
    if(argc<3||argc>5)throw std::runtime_error("image_smoke input.jpg output.heic [quality] [tune]");std::cout<<photo::convert(argv[1],argv[2],argc>=4?std::stoi(argv[3]):65,"slow",argc==5?argv[4]:"standard",[](const std::string& text){std::cout<<text<<std::endl;})<<std::endl;return 0;
}catch(const std::exception& e){std::cerr<<e.what()<<std::endl;return 1;}}
