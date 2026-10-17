#pragma once
#include <FLAC/format.h>
#include <opusenc.h>
#include <string>
#include <vector>
#include <functional>

// Container-independent user metadata. Original tag bytes, repeats and picture fields stay intact.
class AudioMetadata {
    std::vector<std::string> entries;
    size_t bytes=0,pictures=0,technical=0,applications=0,applicationBytes=0;
    std::vector<std::string> applicationDetails;
    void readApplication(const FLAC__StreamMetadata& block);
public:
    void add(std::string entry);
    void read(const FLAC__StreamMetadata& block);
    void apply(OggOpusComments* comments,const std::function<void(const std::string&)>& log) const;
    void verify(const char* path,const std::function<void(const std::string&)>& log) const;
};
