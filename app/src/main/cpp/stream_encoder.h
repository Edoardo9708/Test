#pragma once
#include "encoder.h"
#include <memory>
struct StreamConfig {
    int width, height, fpsMilli, crf, standard, transfer, range;
    int preset=0;
    int cpuMode=0, cores=2;
    bool bframes=false, lookahead=false;
};
class StreamEncoder {
public:
    StreamEncoder(const StreamConfig&, ProbeCallbacks&);
    ~StreamEncoder();
    void frame(const uint8_t*, size_t, int64_t, ProbeCallbacks&);
    void finish(ProbeCallbacks&);
private:
    struct State;
    std::unique_ptr<State> state;
};
