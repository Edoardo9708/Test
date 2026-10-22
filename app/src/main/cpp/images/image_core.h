#pragma once
#include <libheif/heif.h>
#include <cstdint>
#include <functional>
#include <memory>
#include <string>
#include <vector>
namespace photo {
using Bytes=std::vector<uint8_t>;
using Stage=std::function<void(const std::string&)>;
struct Block { uint8_t marker;Bytes data; };
struct Metadata { std::vector<Block> blocks;Bytes exif,xmp,icc,tail,bundle;int orientation=1; };
struct ImageDelete {void operator()(heif_image* p)const{if(p)heif_image_release(p);}};
struct ContextDelete {void operator()(heif_context* p)const{if(p)heif_context_free(p);}};
struct HandleDelete {void operator()(heif_image_handle* p)const{if(p)heif_image_handle_release(p);}};
using Image=std::unique_ptr<heif_image,ImageDelete>;
using Context=std::unique_ptr<heif_context,ContextDelete>;
using Handle=std::unique_ptr<heif_image_handle,HandleDelete>;
struct Source {Metadata metadata;Image image;int width=0,height=0,matrix=6,primaries=2,transfer=2;};
constexpr int thumbnailEdge=1024;
void addThumbnail(heif_context* context,const Source& source,const heif_image_handle* primary,const heif_encoding_options* options);
void check(heif_error error,const char* action);
Bytes readFile(const std::string& path,size_t limit);
std::string inspectJpeg(const std::string& path);
Source loadJpeg(const std::string& path);
void attach(heif_context* context,heif_image_handle* handle,const Metadata& data);
void verify(const std::string& path,const Source& source,const Stage& stage);
std::string convert(const std::string& input,const std::string& output,int quality,const std::string& preset,const std::string& tune,const Stage& stage);
}
