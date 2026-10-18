#include "hevc_decoder.h"
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/error.h>
#include <libavutil/pixfmt.h>
}
#include <algorithm>
#include <cstring>
#include <stdexcept>
#include <string>
#include <vector>
namespace {
void checked(int code,const char* operation){if(code<0){char error[AV_ERROR_MAX_STRING_SIZE];av_strerror(code,error,sizeof(error));throw std::runtime_error(std::string(operation)+": "+error);}}
std::vector<uint8_t> annexB(const uint8_t* data,size_t size){
    if(!data||!size||size>32*1024*1024)throw std::runtime_error("Invalid HEVC packet size");
    if(size>=4&&data[0]==0&&data[1]==0&&(data[2]==1||(data[2]==0&&data[3]==1)))return {data,data+size};
    std::vector<uint8_t> result;size_t offset=0;
    while(offset<size){
        if(size-offset<4)throw std::runtime_error("Truncated HEVC NAL length");
        uint32_t n=(uint32_t(data[offset])<<24)|(uint32_t(data[offset+1])<<16)|(uint32_t(data[offset+2])<<8)|data[offset+3];offset+=4;
        if(n<2||n>size-offset)throw std::runtime_error("Invalid HEVC NAL length");
        result.insert(result.end(),{0,0,0,1});result.insert(result.end(),data+offset,data+offset+n);offset+=n;
    }return result;
}
}
HevcDecoder::HevcDecoder(const uint8_t* csd,size_t size,int w,int h,int p,int t,int m,bool full):width(w),height(h),primaries(p),transfer(t),matrix(m),range(full?AVCOL_RANGE_JPEG:AVCOL_RANGE_MPEG){
    if(w<16||h<16||w>3840||h>3840||int64_t(w)*h>3840LL*2160||(w%2)||(h%2))throw std::runtime_error("Invalid HDR dimensions");
    try{
        const AVCodec* codec=avcodec_find_decoder_by_name("hevc");if(!codec)throw std::runtime_error("Bundled HEVC decoder absent");
        ctx=avcodec_alloc_context3(codec);frame=av_frame_alloc();if(!ctx||!frame)throw std::bad_alloc();
        auto headers=annexB(csd,size);ctx->extradata=static_cast<uint8_t*>(av_mallocz(headers.size()+AV_INPUT_BUFFER_PADDING_SIZE));if(!ctx->extradata)throw std::bad_alloc();
        memcpy(ctx->extradata,headers.data(),headers.size());ctx->extradata_size=static_cast<int>(headers.size());
        ctx->pkt_timebase={1,1000000};ctx->thread_count=2;ctx->thread_type=FF_THREAD_FRAME|FF_THREAD_SLICE;
        ctx->max_pixels=3840LL*2160;ctx->err_recognition=AV_EF_CAREFUL|AV_EF_EXPLODE;
        checked(avcodec_open2(ctx,codec,nullptr),"Open software HEVC");
    }catch(...){av_frame_free(&frame);avcodec_free_context(&ctx);throw;}
}
HevcDecoder::~HevcDecoder(){av_frame_free(&frame);avcodec_free_context(&ctx);}
void HevcDecoder::send(const uint8_t* bytes,size_t size,int64_t pts){
    if(!bytes){checked(avcodec_send_packet(ctx,nullptr),"Flush software HEVC");return;}
    auto data=annexB(bytes,size);AVPacket* packet=av_packet_alloc();if(!packet)throw std::bad_alloc();
    int result=av_new_packet(packet,static_cast<int>(data.size()));
    if(result>=0){memcpy(packet->data,data.data(),data.size());packet->pts=pts;packet->dts=AV_NOPTS_VALUE;result=avcodec_send_packet(ctx,packet);}
    av_packet_free(&packet);checked(result,"Send software HEVC");
}
int HevcDecoder::receive(uint8_t* pixels,size_t capacity,int64_t& pts,int64_t& precise){
    av_frame_unref(frame);int result=avcodec_receive_frame(ctx,frame);
    if(result==AVERROR(EAGAIN))return 0;if(result==AVERROR_EOF)return 2;checked(result,"Receive software HEVC");
    if(frame->flags&AV_FRAME_FLAG_CORRUPT)throw std::runtime_error("Corrupt decoded HDR frame");
    if(frame->format!=AV_PIX_FMT_YUV420P10LE||frame->width!=width||frame->height!=height)throw std::runtime_error("Expected original dimensions and planar 10-bit HEVC");
    if(frame->color_primaries!=primaries||frame->color_trc!=transfer||frame->colorspace!=matrix||frame->color_range!=range)throw std::runtime_error("Software HEVC color does not match source VUI");
    if(frame->pts==AV_NOPTS_VALUE)throw std::runtime_error("Missing decoded HEVC timestamp");
    if(!pixels||capacity<size_t(width)*height*3)throw std::runtime_error("HDR output buffer too small");
    precise=0;size_t offset=0;
    for(int plane=0;plane<3;plane++){
        int w=plane?width/2:width,h=plane?height/2:height;
        if(!frame->data[plane]||frame->linesize[plane]<w*2)throw std::runtime_error("Invalid decoded HDR stride");
        for(int y=0;y<h;y++){
            const uint8_t* row=frame->data[plane]+ptrdiff_t(y)*frame->linesize[plane];
            for(int x=0;x<w;x++){unsigned value=row[x*2]|(unsigned(row[x*2+1])<<8);if(value>1023)throw std::runtime_error("HDR sample out of range");if(value&3)precise++;}
            memcpy(pixels+offset,row,size_t(w)*2);offset+=size_t(w)*2;
        }
    }pts=frame->pts;return 1;
}
