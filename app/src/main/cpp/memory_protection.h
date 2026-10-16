#pragma once

// Shared by the isolated 8-bit and 10-bit encoders. Also covers portrait
// and cropped UHD inputs whose long side remains 3840 pixels.
namespace MemoryProtection {
constexpr bool required(int width, int height) {
    return width >= 3840 || height >= 3840;
}

template<class Param> bool apply(Param* p, int width, int height) {
    if (!required(width, height)) return false;
    p->frameNumThreads = 2;
    p->numaPools = "2";
    p->bframes = 4;
    p->lookaheadDepth = 5;
    p->bFrameAdaptive = 0;
    p->rc.cuTree = 0;
    return true;
}
}
