#pragma once
#include <cstdint>
#include <functional>
#include <string>
#include <vector>
struct ProbeCallbacks {
    std::function<void(const std::string&)> log;
    std::function<bool()> cancelled;
    std::function<void(const std::vector<uint8_t>&)> headers;
    std::function<void(const std::vector<uint8_t>&, int64_t, bool)> packet;
};
void runProbe(ProbeCallbacks& cb, int crf);
