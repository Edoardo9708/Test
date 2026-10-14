#pragma once
#include "encoder.h"
#include <memory>
#include <array>
struct StreamConfig {
    int width, height, fpsMilli, crf, standard, transfer, range;
    int preset=0;
    int cpuMode=0, cores=2;
    bool bframes=false, lookahead=false;
    bool exactColor=false;
    int colorPrimaries=2,colorTransfer=2,colorMatrix=2,colorFlags=40;
};
class StreamEncoder {
public:
    StreamEncoder(const StreamConfig&, ProbeCallbacks&);
    ~StreamEncoder();
    void frame(const uint8_t*, size_t, int64_t, ProbeCallbacks&);
    void finish(ProbeCallbacks&);
    static std::array<int,5> presetSettings(int preset,bool bframes,bool lookahead);
    std::array<int,5> effectiveSettings() const;
private:
    struct State;
    std::unique_ptr<State> state;
};


