"""Independent stdlib FLAC/Ogg readers: exact tag/picture equality, including repeats and multi-page packets."""
import base64
import struct
import sys
import zlib
from pathlib import Path

def flac(path):
    data=Path(path).read_bytes();assert data[:4]==b'fLaC';at=4;blocks=[]
    while True:
        kind=data[at];size=int.from_bytes(data[at+1:at+4],'big');at+=4
        blocks.append((kind&127,data[at:at+size]));at+=size
        if kind&128:break
    return blocks,data[at:]

def write(path,blocks,frames):
    Path(path).write_bytes(b'fLaC'+b''.join(bytes([kind|(128 if n==len(blocks)-1 else 0)])+len(body).to_bytes(3,'big')+body for n,(kind,body) in enumerate(blocks))+frames)

def comments(payload):
    at=0
    def field():
        nonlocal at
        length=struct.unpack_from('<I',payload,at)[0];at+=4;value=payload[at:at+length];assert len(value)==length;at+=length;return value
    field();count=struct.unpack_from('<I',payload,at)[0];at+=4
    return [field() for _ in range(count)]

def expected(path):
    entries=[]
    for kind,payload in flac(path)[0]:
        if kind==4:entries+=comments(payload)
        if kind==2:entries.append(b'CODECPROBE_FLAC_APPLICATION='+base64.b64encode(payload))
        if kind==6:entries.append(b'METADATA_BLOCK_PICTURE='+base64.b64encode(payload))
    return entries

def opus(path):
    data=Path(path).read_bytes();at=0;packet=bytearray();count=0
    while at<len(data):
        assert data[at:at+4]==b'OggS';segments=data[at+26];laces=data[at+27:at+27+segments];at+=27+segments
        for size in laces:
            packet+=data[at:at+size];at+=size
            if size<255:
                count+=1
                if count==2:
                    assert packet[:8]==b'OpusTags';return comments(packet[8:])
                packet.clear()
    raise AssertionError('No OpusTags')

mode=sys.argv[1]
if mode=='create':
    path=sys.argv[2];blocks,frames=flac(path)
    tags=['TITLE=Prova è 音楽','ARTIST=Primo artista','ARTIST=Secondo artista','ALBUM=Album test','TRACKNUMBER=01/10','DISCNUMBER=2','DATE=2007','REPLAYGAIN_TRACK_GAIN=-4.25 dB','X-PERSONALIZZATO=valore originale','LYRICS=[00:01.25]Testo interno\n[00:02.50]Seconda riga','COMMENT='+('x'*70000)]
    encoded=[s.encode('utf-8') for s in tags];vendor=b'Original FLAC encoder'
    vc=struct.pack('<I',len(vendor))+vendor+struct.pack('<I',len(encoded))+b''.join(struct.pack('<I',len(e))+e for e in encoded)
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',1,1,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(b'\x00\xff\x00\x00'))+chunk(b'IEND',b'')
    pictures=[]
    for kind,description in [(3,'Copertina è'),(4,'Retro')]:
        mime=b'image/png';desc=description.encode('utf-8')
        picture=struct.pack('>II',kind,len(mime))+mime+struct.pack('>I',len(desc))+desc+struct.pack('>IIIII',1,1,24,0,len(png))+png
        pictures.append((6,picture))
    # Standard source-WAV headers plus arbitrary binary APPLICATION, including NUL.
    info=int.from_bytes(blocks[0][1][10:18],'big');rate=info>>44;channels=((info>>41)&7)+1;bits=((info>>36)&31)+1;total=info&((1<<36)-1)
    audio_bytes=total*channels*((bits+7)//8);align=channels*((bits+7)//8)
    riff=[b'riffRIFF'+struct.pack('<I',audio_bytes+36)+b'WAVE',b'rifffmt '+struct.pack('<IHHIIHH',16,1,channels,rate,rate*align,align,bits),b'riffdata'+struct.pack('<I',audio_bytes)]
    applications=[(2,p) for p in riff]+[(2,b'TEST'+bytes(range(256))*300),(2,b'ZERO'),(2,b'\0\x01\xfe\xffpayload')]
    write(path,[b for b in blocks if b[0] not in (2,4,6)]+applications+[(4,vc)]+pictures,frames)
    write(path+'.application.flac',[(0,blocks[0][1]),(2,b'TESTprivate')],frames)
    write(path+'.shortapplication.flac',[(0,blocks[0][1]),(2,b'BAD')],frames)
    write(path+'.reserved.flac',[(0,blocks[0][1]),(7,b'unsupported')],frames)
    bad=b'TITLE=bad\x00value';write(path+'.invalid.flac',[(0,blocks[0][1]),(4,struct.pack('<I',0)+struct.pack('<I',1)+struct.pack('<I',len(bad))+bad)],frames)
elif mode=='verify':
    original=expected(sys.argv[2]);result=opus(sys.argv[3]);assert result==original,'Comments/pictures differ'
    # Archive must decode back to exact identifiers and binary payloads, in source order.
    archived=[base64.b64decode(e.split(b'=',1)[1],validate=True) for e in result if e.startswith(b'CODECPROBE_FLAC_APPLICATION=')]
    assert archived==[p for k,p in flac(sys.argv[2])[0] if k==2]
    if not any(e.startswith(b'ARTIST=') for e in original):
        print('OK: exact APPLICATION archive without song tags');sys.exit(0)
    assert sum(e.startswith(b'ARTIST=') for e in result)==2
    assert sum(e.startswith(b'METADATA_BLOCK_PICTURE=') for e in result)==2
    print('OK: exact original tag bytes/order/repeats/Unicode/lyrics both picture blocks and binary APPLICATION archives; multi-page OpusTags')
