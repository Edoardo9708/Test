#pragma once
#include "image_sef.h"
#include <cstdint>
#include <string>
#include <vector>
namespace photo {
using Bytes=std::vector<uint8_t>;
struct Block {uint8_t marker;Bytes data;};
struct Metadata {
    std::vector<Block> blocks;
    Bytes exif,xmp,icc,tail,bundle;
    int orientation=1;
    size_t jpegEnd=0;
    bool missingEoi=false;
    JpegTailKind tailKind=JpegTailKind::Empty;
};
// Structural inspection only: no decoding, encoding or HEIF initialization.
Metadata readJpegMetadata(const Bytes& input);
std::string metadataNotes(const Metadata& metadata);
}
