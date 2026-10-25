#pragma once
#include <cstdint>
#include <stdexcept>
#include <string>

namespace photo {
constexpr int64_t maxPrimaryPixels=200000000;
// JPEG SOF dimensions use unsigned 16-bit fields. The total area remains bounded.
constexpr int maxPrimarySide=65535;
constexpr int maxHevcTileSide=16384;
// libheif's pinned x265 plugin refuses a single coded image above this area.
constexpr int64_t maxSingleHevcPixels=142606336;
inline void validatePrimaryDimensions(int width,int height){
    if(width<64||height<64||width>maxPrimarySide||height>maxPrimarySide||
       int64_t(width)*height>maxPrimaryPixels)
        throw std::runtime_error("Dimensioni JPEG non supportate: "+std::to_string(width)+" × "+
            std::to_string(height)+" pixel; massimo 200 megapixel, lati da 64 a 65535 pixel");
}
}
