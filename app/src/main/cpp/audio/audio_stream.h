#pragma once
#include "audio_metadata.h"
#include <cstdint>
#include <memory>

// One worker owns one streaming encoder. No whole-track PCM allocation.
class AudioStream {
    AudioMetadata tags;
    OggOpusEnc* encoder=nullptr;
    std::string destination;
    unsigned rate,channels;
    int bitrate;
    uint64_t samples=0;
public:
    AudioStream(int fd,int kind,const char* path,int rate,int channels,int bitrate,const std::function<void(const std::string&)>& log);
    ~AudioStream();
    AudioStream(const AudioStream&)=delete;
    AudioStream& operator=(const AudioStream&)=delete;
    void write(const float* pcm,size_t count);
    uint64_t finish(const std::function<void(const std::string&)>& log);
};
AudioMetadata readCompressedMetadata(int fd,int kind);
