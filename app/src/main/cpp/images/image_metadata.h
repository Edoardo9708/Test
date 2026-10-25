#pragma once
#include "image_sef.h"
#include "image_gainmap.h"
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
    int width=0,height=0;
    size_t jpegEnd=0;
    bool missingEoi=false,orientationWasZero=false,archivedPanorama=false,externalTail=false,c2pa=false;
    size_t sefZeroPrefix=0;
    GainMap gainMap;
    MotionClip motion;
    JpegTailKind tailKind=JpegTailKind::Empty;
};
// Structural inspection only: no decoding, encoding or HEIF initialization.
Metadata readJpegMetadata(const Bytes& input);
std::string metadataNotes(const Metadata& metadata);
}
