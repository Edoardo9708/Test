#pragma once
#include "../encoder.h"
#include <memory>
class HdrEncoder {
public:
    HdrEncoder(int w,int h,int crf,int primaries,int transfer,int matrix,int flags,ProbeCallbacks& cb);
    ~HdrEncoder();
    void frame(const uint8_t* data,size_t size,int64_t pts,ProbeCallbacks& cb);
    void finish(ProbeCallbacks& cb);
private:
    struct State;
    std::unique_ptr<State> state;
};
