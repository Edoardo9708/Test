#pragma once
#include <cstdint>
#include <cstddef>
#include <string>
#include <vector>
namespace photo {
struct MotionClip {size_t offset=0,length=0;int64_t timestamp=-1;bool enabled=true;};
constexpr size_t motionVideoLimit=48*1024*1024;
void validateMotionMp4(const std::vector<uint8_t>& bytes,size_t offset,size_t length);
std::vector<uint8_t> motionXmp(const std::vector<uint8_t>& original,size_t eofOffset,MotionClip& clip);
void appendMotionVideo(const std::string& path,const std::vector<uint8_t>& tail,const MotionClip& clip);
void verifyMotionVideo(const std::string& path,const std::vector<uint8_t>& tail,const MotionClip& clip);
}
