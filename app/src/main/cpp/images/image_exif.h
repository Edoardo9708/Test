#pragma once
#include <cstdint>
#include <vector>
namespace photo {
int exifOrientation(const std::vector<uint8_t>& exif);
}
