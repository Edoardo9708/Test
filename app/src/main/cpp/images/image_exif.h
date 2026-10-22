#pragma once
#include <cstdint>
#include <vector>
namespace photo {
// Return 0 for an explicitly undefined orientation; the caller must report its fallback.
int exifOrientation(const std::vector<uint8_t>& exif);
}
