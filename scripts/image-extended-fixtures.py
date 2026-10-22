"""Public synthetic regression cases for CPJPEG3, C2PA and Apple HDR MPF."""
from pathlib import Path
from io import BytesIO
import base64
import hashlib
import struct
import subprocess
from PIL import Image

root = Path('image-fixtures/extended')
root.mkdir(parents=True, exist_ok=True)
engine = str(Path('image-host-build/image_smoke').resolve())
u32 = lambda n: struct.pack('>I', n)
box = lambda name, data: u32(len(data) + 8) + name + data
segment = lambda marker, data: bytes([255, marker]) + struct.pack('>H', len(data) + 2) + data

def jpeg(image, **kwargs):
    out = BytesIO()
    image.save(out, format='JPEG', quality=94, **kwargs)
    return out.getvalue()

plain = jpeg(Image.new('RGB', (192, 128), (37, 82, 149)))

def sef(records, prefix=b''):
    data = prefix
    entries = []
    for kind, name, value in records:
        record = struct.pack('<HHI', 0, kind, len(name)) + name + value
        entries.append((kind, len(data), len(record)))
        data += record
    directory = b'SEFH' + struct.pack('<II', 107, len(entries))
    directory += b''.join(struct.pack('<HHII', 0, kind, len(data)-start, size) for kind, start, size in entries)
    return data + directory + struct.pack('<I', len(directory)) + b'SEFT'

utc = (0x0a01, b'Image_UTC_Data', b'1663352834748')

def run(*args):
    return subprocess.run([engine, *map(str, args)], capture_output=True, text=True)

def source(name, data):
    path = root / (name + '.jpg')
    path.write_bytes(data)
    path.with_suffix('.heic').unlink(missing_ok=True)
    return path

def accept(name, data):
    path = source(name, data)
    digest = hashlib.sha256(data).digest()
    inspected = run('inspect', path)
    assert inspected.returncode == 0, (name, inspected.stderr)
    result = run(path, path.with_suffix('.heic'), '60')
    path.with_suffix('.log').write_text(result.stdout + result.stderr)
    assert result.returncode == 0, (name, result.stdout, result.stderr)
    assert hashlib.sha256(path.read_bytes()).digest() == digest
    checked = run('verify', path, path.with_suffix('.heic'))
    assert checked.returncode == 0, (name, checked.stderr)
    return path, path.with_suffix('.heic').read_bytes()

def reject(name, data):
    path = source(name, data)
    assert run('inspect', path).returncode != 0, name
    result = run(path, path.with_suffix('.heic'), '60')
    assert result.returncode != 0 and not path.with_suffix('.heic').exists(), name

def tamper(path, data, at, label):
    changed = bytearray(data)
    changed[at] ^= 1
    output = root / (path.stem + '-changed-' + label + '.heic')
    output.write_bytes(changed)
    assert run('verify', path, output).returncode != 0, (path, label)

path, data = accept('zero-sef-prefix', plain + sef([utc], bytes(55968)))
assert bytes(55968) + sef([utc]) in data
reject('nonzero-sef-prefix', plain + sef([utc], bytes(55967) + b'X'))
reject('zero-prefix-over-limit', plain + sef([utc], bytes(65537)))
# A valid first record does not permit an internal hole or overlapping directory entries.
broken = bytearray(sef([utc, utc]))
dir_at = broken.index(b'SEFH')
struct.pack_into('<I', broken, dir_at + 12 + 12 + 4, 1)
reject('overlapping-sef', plain + broken)
for kind, name in [(0x0ac0, b'ZoomInOut_Info'), (0x0b41, b'SingeShot_DepthMap_9'),
                   (0x0001, b'DualShot_5'), (0x0b41, b'SingleShot_DepthMap_7')]:
    accept(name.decode(), plain + sef([(kind, name, b'archival data\x00\xff')]))
reject('unknown-sef', plain + sef([(0x0ac0, b'Unknown_Info', b'opaque')]))

# CPJPEG3 archive: a 34 MiB validated record, plus the primary Motion Photo hole.
uuid = b'CPJPEG3\0\x80' + bytes(6) + b'\x01'
large_record = (0x0001, b'DualShot_5', b'large-source-' + bytes(34*1024*1024))
video = base64.decodebytes(Path('tests/fixtures/motion-photo-video.mp4.b64').read_bytes())
for motion in (False, True):
    tail = sef([utc, large_record] + ([(0x0a30, b'MotionPhoto_Data', video)] if motion else []))
    path, data = accept('large-sef-' + str(motion), plain + tail)
    at = data.index(b'CPJPEG3\0') + 8
    count = struct.unpack_from('>I', data, at)[0]
    at += 4
    for _ in range(count):
        length = struct.unpack_from('>I', data, at+1)[0]
        at += 5 + length
    length, offset, size = struct.unpack_from('>III', data, at)
    archive = data.index(uuid)
    start = archive - 8
    assert data[start+4:start+8] == b'uuid'
    archived_size = struct.unpack_from('>I', data, start)[0] - 24
    archived = data[archive+16:archive+16+archived_size]
    movie = b''
    if motion:
        movie_start = archive+16+archived_size
        assert data[movie_start+4:movie_start+8] == b'mpvd'
        movie = data[movie_start+8:]
        assert movie == video and data.count(video) == 1
    assert length == len(tail) and size == len(movie)
    assert archived[:offset] + movie + archived[offset:] == tail
    tamper(path, data, archive+16+100, 'large-archive')

# C2PA identification is structural; original signatures are only archived.
c2pa_uuid = bytes.fromhex('6332706100110010800000aa00389b71')
desc = box(b'jumd', c2pa_uuid + b'\x03c2pa\0')
jumbf = box(b'jumb', desc + box(b'cbor', b'original-signed-content' * 4000))
packets = []
for i, begin in enumerate(range(8, len(jumbf), 60000), 1):
    packets.append(b'JP\x02\x11' + u32(i) + jumbf[:8] + jumbf[begin:begin+60000])
source_data = plain[:2] + b''.join(segment(0xeb, x) for x in packets) + plain[2:]
path, data = accept('c2pa-fragmented', source_data)
for packet in packets:
    assert packet in data
assert b'original-signed-content' in data
tamper(path, data, data.index(packets[0])+100, 'c2pa-archive')
reject('c2pa-missing-fragment', plain[:2] + segment(0xeb, packets[0]) + plain[2:])
reject('c2pa-duplicate-sequence', plain[:2] + segment(0xeb, packets[0]) * 2 + plain[2:])
unknown = jumbf.replace(c2pa_uuid, b'unknown-jumbf-id!')
reject('unknown-jumbf', plain[:2] + segment(0xeb, b'JP\x02\x11'+u32(1)+unknown[:100]) + plain[2:])

# Apple v1 monochrome gain map with an MPF attribute IFD, no private fixtures.
aux_xmp = (b'<x:xmpmeta xmlns:x="adobe:ns:meta/"><r:RDF xmlns:r="http://www.w3.org/1999/02/22-rdf-syntax-ns#"><r:Description '
           b'xmlns:a="http://ns.apple.com/pixeldatainfo/1.0/" xmlns:g="http://ns.apple.com/HDRGainMap/1.0/">'
           b'<a:AuxiliaryImageType>urn:com:apple:photo:2020:aux:hdrgainmap</a:AuxiliaryImageType>'
           b'<g:HDRGainMapVersion>65536</g:HDRGainMapVersion></r:Description></r:RDF></x:xmpmeta>')
xmp_header = b'http://ns.adobe.com/xap/1.0/\0'
gray = Image.new('L', (96,64))
gray.putdata([(x*3+y*7)%256 for y in range(64) for x in range(96)])
attribute_mpf = b'MPF\0MM\0*'+u32(8)+struct.pack('>H',2)+struct.pack('>HHI4s',0xb000,7,4,b'0100')+struct.pack('>HHII',0xb001,4,1,2)+bytes(4)
aux_plain = jpeg(gray)
aux = aux_plain[:2]+segment(0xe1,xmp_header+aux_xmp)+segment(0xe2,attribute_mpf)+aux_plain[2:]

def mpf_source(auxiliary, orientation=1):
    exif = Image.Exif()
    exif[274] = orientation
    primary = jpeg(Image.new('RGB',(192,128),(51,83,127)),exif=exif)
    primary_size = len(primary) + 90  # APP2 4-byte framing + 86-byte payload
    mpentries = struct.pack('>IIII',0x00030000,primary_size,0,0)+struct.pack('>IIII',0,len(auxiliary),primary_size-10,0)
    mpf = (b'MPF\0MM\0*'+u32(8)+struct.pack('>H',3)+struct.pack('>HHI4s',0xb000,7,4,b'0100')+
           struct.pack('>HHII',0xb001,4,1,2)+struct.pack('>HHII',0xb002,7,32,50)+bytes(4)+mpentries)
    assert len(mpf)==86
    return primary[:2]+segment(0xe2,mpf)+primary[2:]+auxiliary

for orientation in (1,2,6,7):
    path, data = accept('apple-gain-map-'+str(orientation),mpf_source(aux,orientation))
    assert aux in data and aux_xmp in data and b'auxl' in data and b'auxC' in data
    tamper(path,data,data.index(b'auxl'),'gain-map-link')
    tamper(path,data,data.index(b'urn:com:apple:photo:2020:aux:hdrgainmap'),'gain-map-type')
    tamper(path,data,data.index(aux)+100,'original-aux-jpeg')
reject('unknown-mpf-image',mpf_source(aux_plain))
reject('gain-map-wrong-version',mpf_source(aux.replace(b'65536',b'99999')))
reject('gain-map-wrong-urn',mpf_source(aux.replace(b'2020:aux:hdrgainmap',b'9999:aux:hdrgainmap')))
reject('mpf-extra-tail',mpf_source(aux)+b'X')
reject('mpf-truncated-tail',mpf_source(aux)[:-1])
invalid = bytearray(mpf_source(aux))
invalid[87] ^= 1  # secondary MP entry offset
reject('mpf-wrong-offset',invalid)
print('OK extended: zero SEF prefix, new Samsung records, 34 MiB CPJPEG3 recovery with/without Motion Photo, fragmented C2PA archive, Apple HDR lossless auxiliary pixels/orientations; corrupt data and unknown MPF/JUMBF rejected')
