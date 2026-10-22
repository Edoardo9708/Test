#pragma once
#include <cstdint>
#include <vector>
namespace photo {
// Validate post-JPEG padding/SEF before archiving it verbatim. No payload interpretation.
void validateJpegTail(const std::vector<uint8_t>& bytes);
}
