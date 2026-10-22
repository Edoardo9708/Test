#pragma once
#include <cstdint>
#include <cstddef>
#include <vector>
namespace photo {
inline constexpr const char* appleGainMapType="urn:com:apple:photo:2020:aux:hdrgainmap";
struct GainMap {int width=0,height=0;std::vector<uint8_t> xmp;};
// Accept only a two-image MPF whose complete second JPEG is an Apple HDR map.
GainMap inspectAppleGainMap(const std::vector<uint8_t>& mpf,size_t tiffBase,size_t jpegEnd,const std::vector<uint8_t>& tail,int width,int height);
}
