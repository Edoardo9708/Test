#include "audio_stream.h"
#include <fileref.h>
#include <tfilestream.h>
#include <tpropertymap.h>
#include <tvariant.h>
#include <tpicturetype.h>
#include <mpegfile.h>
#include <mp4file.h>
#include <stdexcept>
#include <unistd.h>

// TagLib translates ID3/MP4 fields into the same generic property vocabulary as Opus.
// Unknown fields fail explicitly; they are never silently discarded.
AudioMetadata readCompressedMetadata(int fd,int kind){
    int owned=dup(fd);if(owned<0)throw std::runtime_error("Duplicazione input metadati fallita");
    TagLib::FileStream stream(owned,true); // FileStream owns fclose of this duplicate.
    if(!stream.isOpen()){close(owned);throw std::runtime_error("Input metadati non seekable o non leggibile");}
    std::unique_ptr<TagLib::File> file;
    if(kind==1)file=std::make_unique<TagLib::MPEG::File>(&stream,false);
    else if(kind==2)file=std::make_unique<TagLib::MP4::File>(&stream,false);
    else throw std::runtime_error("Contenitore metadati sconosciuto");
    TagLib::FileRef ref(file.release());
    if(ref.isNull()||!ref.file()->isValid())throw std::runtime_error("Metadati ID3/MP4 non leggibili");
    auto properties=ref.properties();
    for(const auto& key:properties.unsupportedData()){
        // PICTURE is handled through the complex-property API below.
        if(key!="APIC"&&key!="covr")throw std::runtime_error("Metadato non traducibile in Opus: "+key.to8Bit(true)+"; nessun dato scartato");
    }
    AudioMetadata out;
    for(auto it=properties.begin();it!=properties.end();++it)
        for(const auto& value:it->second)out.add(it->first.to8Bit(true)+"="+value.to8Bit(true));
    for(const auto& key:ref.complexPropertyKeys()){
        if(key!="PICTURE")throw std::runtime_error("Metadato complesso non traducibile in Opus: "+key.to8Bit(true));
        for(const auto& picture:ref.complexProperties(key)){
            auto data=picture.value("data").toByteVector();
            std::string mime=picture.value("mimeType").toString().to8Bit(true),description=picture.value("description").toString().to8Bit(true);
            if(data.isEmpty()||data.size()>16*1024*1024||mime.size()+description.size()>65536)throw std::runtime_error("Copertina non valida o oltre 16 MiB");
            FLAC__StreamMetadata block{};block.type=FLAC__METADATA_TYPE_PICTURE;auto& p=block.data.picture;
            // MP4 covr has no picture type: use front cover. ID3 preserves its original type.
            int type=picture.contains("pictureType")?TagLib::Utils::pictureTypeFromString(picture.value("pictureType").toString()):3;
            if(type<0||type>20)throw std::runtime_error("Tipo copertina ID3 non valido");
            p.type=static_cast<FLAC__StreamMetadata_Picture_Type>(type);p.mime_type=mime.data();p.description=reinterpret_cast<FLAC__byte*>(description.data());
            p.width=picture.value("width").toUInt();p.height=picture.value("height").toUInt();p.depth=picture.value("colorDepth").toUInt();p.colors=picture.value("numColors").toUInt();
            p.data_length=data.size();p.data=reinterpret_cast<FLAC__byte*>(data.data());out.read(block);
        }
    }
    return out;
}
