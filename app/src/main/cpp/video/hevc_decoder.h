#pragma once
#include <cstddef>
#include <cstdint>
struct AVCodecContext;
struct AVFrame;
class HevcDecoder {
    AVCodecContext* ctx=nullptr;
    AVFrame* frame=nullptr;
    int width,height,primaries,transfer,matrix,range;
public:
    HevcDecoder(const uint8_t* csd,size_t size,int w,int h,int p,int t,int m,bool full);
    ~HevcDecoder();
    HevcDecoder(const HevcDecoder&)=delete;
    HevcDecoder& operator=(const HevcDecoder&)=delete;
    void send(const uint8_t* bytes,size_t size,int64_t pts);
    // 0: drain needs input, 1: frame, 2: EOF. Output: PTS and precise sample count.
    int receive(uint8_t* pixels,size_t capacity,int64_t& pts,int64_t& precise);
};
