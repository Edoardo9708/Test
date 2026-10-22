#pragma once
#include <cstdint>
#include <cstddef>
#include <vector>
namespace photo {
// Validate post-JPEG padding/SEF before archiving it verbatim. No payload interpretation.
enum class JpegTailKind { Empty, Padding, PaddedOpaque, Sef };
JpegTailKind validateJpegTail(const std::vector<uint8_t>& bytes);
// Locate a possible SEF boundary from the footer and directory, without decoding.
// The caller must validate the complete suffix before accepting a missing EOI.
size_t jpegSefTailStart(const std::vector<uint8_t>& bytes);
}
