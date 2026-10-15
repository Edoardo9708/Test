#pragma once
#include "../stream_encoder.h"
#include <memory>
class HdrEncoder {
public:
    HdrEncoder(int w,int h,int crf,int primaries,int transfer,int matrix,int flags,ProbeCallbacks& cb);
    HdrEncoder(const StreamConfig& config,ProbeCallbacks& cb);
    std::array<int,5> effectiveSettings() const;
    ~HdrEncoder();
    void frame(const uint8_t* data,size_t size,int64_t pts,ProbeCallbacks& cb);
    void finish(ProbeCallbacks& cb);
private:
    struct State;
    std::unique_ptr<State> state;
};

