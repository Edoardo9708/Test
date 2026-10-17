#pragma once
#include <cstdint>
#include <functional>
#include <string>
struct AudioCallbacks {
    std::function<bool(uint64_t,uint64_t,unsigned,unsigned)> progress;
    std::function<void(const std::string&)> log;
};
// Owns a duplicate of fd, not the caller's descriptor. No global encoder state.
uint64_t encodeFlacOpus(int fd,const char* destination,int bitrate,const AudioCallbacks& callbacks);
