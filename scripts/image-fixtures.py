"""Generate bounded real JPEG fixtures and check rejection/corruption with the native HEIC engine."""
from pathlib import Path
import struct
import subprocess
from fractions import Fraction
from PIL import Image, ImageCms, ImageOps, ImageChops, ImageStat, ImageDraw

directory = Path('image-fixtures')
directory.mkdir(exist_ok=True)
image = Image.new('RGB', (192, 128))
image.putdata([((x * 3 + y) % 256, (y * 5) % 256, (x + y * 2) % 256)
               for y in range(128) for x in range(192)])
icc = ImageCms.ImageCmsProfile(ImageCms.createProfile('sRGB')).tobytes()


def segment(marker, payload):
    return bytes([255, marker]) + struct.pack('>H', len(payload) + 2) + payload


def insert(path, marker, payload):
    data = path.read_bytes()
    path.write_bytes(data[:2] + segment(marker, payload) + data[2:])


def sef():
    records = [(0x0a01, b'Image_UTC_Data', b'1567677602564'), (0x0aa1, b'MCC_Data', b'222')]
    data = b''
    offsets = []
    for kind, name, payload in records:
        block = struct.pack('<HHI', 0, kind, len(name)) + name + payload
        offsets.append((kind, len(data), len(block)))
        data += block
    header = b'SEFH' + struct.pack('<II', 105, len(offsets))
    for kind, start, size in offsets:
        header += struct.pack('<HHII', 0, kind, len(data) - start, size)
    return data + header + struct.pack('<I', len(header)) + b'SEFT'


for orientation in range(1, 9):
    exif = Image.Exif()
    exif[274] = orientation
    exif[306] = '2019:09:05 12:00:00'
    exif[34853] = {1: 'N', 2: (Fraction(45), Fraction(1), Fraction(2)),
                   3: 'E', 4: (Fraction(8), Fraction(1), Fraction(2))}
    image.save(directory / f'orientation-{orientation}.jpg', quality=94, subsampling=2, exif=exif)
for subsampling in (0, 1):
    image.save(directory / f'chroma-{subsampling}.jpg', quality=94, subsampling=subsampling)
image.save(directory / 'icc.jpg', quality=94, icc_profile=icc)
image.resize((193, 129)).save(directory / 'odd.jpg', quality=94)
image.save(directory / 'sef.jpg', quality=94)
with (directory / 'sef.jpg').open('ab') as out:
    out.write(sef())
image.save(directory / 'xmp.jpg', quality=94)
insert(directory / 'xmp.jpg', 0xe1, b'http://ns.adobe.com/xap/1.0/\0<x:xmpmeta xmlns:x="adobe:ns:meta/">original</x:xmpmeta>')
insert(directory / 'xmp.jpg', 0xe4, b'opaque Samsung APP test\0\1\2')
# Asymmetric large pictures exercise real thumbnails, rotations/mirrors and odd aspect ratios.
large = Image.new('RGB', (1281, 961), (50, 90, 130))
draw = ImageDraw.Draw(large)
draw.rectangle((0, 0, 510, 370), fill=(230, 40, 30))
draw.rectangle((900, 0, 1280, 580), fill=(20, 210, 70))
draw.rectangle((200, 720, 1280, 960), fill=(30, 40, 230))
for orientation in range(1, 9):
    exif = Image.Exif()
    exif[274] = orientation
    large.save(directory / f'thumbnail-{orientation}.jpg', quality=94, exif=exif)
for subsampling in (0, 1):
    large.save(directory / f'thumbnail-chroma-{subsampling}.jpg', quality=94, subsampling=subsampling)
large.save(directory / 'thumbnail-icc.jpg', quality=94, icc_profile=icc)
large.resize((961, 1281)).save(directory / 'thumbnail-portrait.jpg', quality=94)
large.resize((1024, 768)).save(directory / 'thumbnail-boundary.jpg', quality=94)
# At exactly 2:1, Pillow BOX is an independent area-average reference. A point sampler
# aliases this fine checkerboard into a flat dark/light patch instead of the correct grey.
detail = Image.new('RGB', (2048, 1536), (50, 90, 130))
checker = Image.new('L', (1024, 1024))
checker.putdata([230 if (x + y) % 2 else 20 for y in range(1024) for x in range(1024)])
detail.paste(checker.convert('RGB'), (0, 0))
detail.save(directory / 'thumbnail-detail.jpg', quality=94)
image.convert('CMYK').save(directory / 'reject-cmyk.jpg')
image.convert('L').save(directory / 'reject-gray.jpg')
for name, marker, payload in [
    ('mpf', 0xe2, b'MPF\0opaque'),
    ('extended-xmp', 0xe1, b'http://ns.adobe.com/xmp/extension/\0opaque'),
    ('gainmap', 0xe1, b'http://ns.adobe.com/xap/1.0/\0hdrgm:Version="1.0"'),
    ('jumbf', 0xeb, b'JPopaque'),
    ('icc', 0xe2, b'ICC_PROFILE\0\1\2missing part'),
]:
    path = directory / f'reject-{name}.jpg'
    image.save(path)
    insert(path, marker, payload)
image.save(directory / 'reject-tail.jpg')
with (directory / 'reject-tail.jpg').open('ab') as out:
    out.write(b'Motion Photo payload')

engine = str(Path('image-host-build/image_smoke').resolve())
for path in sorted(directory.glob('*.jpg')):
    if path.name.startswith('original-'):
        continue
    output = path.with_suffix('.heic')
    output.unlink(missing_ok=True)
    result = subprocess.run([engine, str(path), str(output), '60'], capture_output=True, text=True)
    path.with_suffix('.log').write_text(result.stdout + result.stderr)
    if path.name.startswith('reject-'):
        assert result.returncode != 0 and not output.exists(), (path, result.stdout, result.stderr)
    else:
        assert result.returncode == 0 and output.stat().st_size > 0, (path, result.stdout, result.stderr)
        checked = subprocess.run([engine, 'verify', str(path), str(output)], capture_output=True, text=True)
        assert checked.returncode == 0, (path, checked.stderr)
        if path.stem.startswith('thumbnail-'):
            ppm = path.with_suffix('.ppm')
            decoded = subprocess.run([engine, 'decode-thumbnail', str(output), str(ppm)], capture_output=True, text=True)
            if path.stem == 'thumbnail-boundary':
                assert decoded.returncode != 0, 'Do not upscale small pictures to generate thumbnails'
                continue
            assert decoded.returncode == 0, (path, decoded.stderr)
            with Image.open(ppm) as actual, Image.open(path) as original:
                assert max(actual.size) == 1024 and min(actual.size) > 0, (path, actual.size)
                reference = ImageOps.exif_transpose(original).resize(actual.size, Image.Resampling.BOX)
                # Compare pixels, not just dimensions: catch inverted/rotated thumbnails and colour errors.
                error = sum(ImageStat.Stat(ImageChops.difference(reference, actual)).mean) / 3
                assert error < 5, (path, 'Thumbnail differs from oriented source', error)
            # Removing the thumbnail link must fail the same verification used before publishing.
            if path.stem == 'thumbnail-1':
                data = bytearray(output.read_bytes())
                position = data.index(b'thmb')
                data[position:position + 4] = b'cdsc'
                corrupt = directory / 'missing-thumbnail.heic'
                corrupt.write_bytes(data)
                result = subprocess.run([engine, 'verify', str(path), str(corrupt)], capture_output=True, text=True)
                assert result.returncode != 0, 'Missing thumbnail accepted'

# Unknown APP data and SEF must survive; any alteration in the preserved archive must fail verification.
for name in ('xmp', 'sef'):
    source = directory / f'{name}.jpg'
    data = bytearray(source.with_suffix('.heic').read_bytes())
    position = data.index(b'CPJPEG1\0')
    data[position + 12] ^= 1
    corrupted = directory / f'{name}-corrupt.heic'
    corrupted.write_bytes(data)
    result = subprocess.run([engine, 'verify', str(source), str(corrupted)], capture_output=True, text=True)
    assert result.returncode != 0, ('Corrupted archive accepted', name)
    corrupted.with_suffix('.log').write_text(result.stdout + result.stderr)
print('OK HEIC: EXIF/GPS/date, eight orientations, ICC, XMP, opaque APP, SEF, 420/422/444, odd dimensions, rejected inputs, altered metadata; dedicated 1024 px thumbnails, oriented pixels, colour profiles, small-image boundary and missing-thumbnail rejection')
