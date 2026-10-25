"""Generate bounded real JPEG fixtures and check rejection/corruption with the native HEIC engine."""
from pathlib import Path
import struct
import subprocess
from fractions import Fraction
from PIL import Image, ImageCms, ImageOps, ImageChops, ImageStat, ImageDraw

directory = Path('image-fixtures')
directory.mkdir(exist_ok=True)
(directory / "metadata-probe-without-pixels.jpg").unlink(missing_ok=True)
for obsolete in directory.glob("reject-orientation-long-*-0.jpg"):
    obsolete.unlink()
image = Image.new('RGB', (192, 128))
image.putdata([((x * 3 + y) % 256, (y * 5) % 256, (x + y * 2) % 256)
               for y in range(128) for x in range(192)])
icc = ImageCms.ImageCmsProfile(ImageCms.createProfile('sRGB')).tobytes()


def segment(marker, payload):
    return bytes([255, marker]) + struct.pack('>H', len(payload) + 2) + payload


def insert(path, marker, payload):
    data = path.read_bytes()
    path.write_bytes(data[:2] + segment(marker, payload) + data[2:])


def sef(records=None, version=105, directory_order=None, data_prefix=b''):
    if records is None:
        records = [(0x0a01, b'Image_UTC_Data', b'1567677602564'), (0x0aa1, b'MCC_Data', b'222')]
    data = data_prefix
    offsets = []
    for kind, name, payload in records:
        block = struct.pack('<HHI', 0, kind, len(name)) + name + payload
        offsets.append((kind, len(data), len(block)))
        data += block
    header = b'SEFH' + struct.pack('<II', version, len(offsets))
    for kind, start, size in (offsets if directory_order is None else [offsets[i] for i in directory_order]):
        header += struct.pack('<HHII', 0, kind, len(data) - start, size)
    return data + header + struct.pack('<I', len(header)) + b'SEFT'


for orientation in range(1, 9):
    exif = Image.Exif()
    exif[274] = orientation
    exif[306] = '2019:09:05 12:00:00'
    exif[34853] = {1: 'N', 2: (Fraction(45), Fraction(1), Fraction(2)),
                   3: 'E', 4: (Fraction(8), Fraction(1), Fraction(2))}
    image.save(directory / f'orientation-{orientation}.jpg', quality=94, subsampling=2, exif=exif)
# A non-standard unsigned LONG orientation is interpreted without rewriting EXIF.
def orientation_exif(value, order='<', field_type=4, count=1):
    slot = struct.pack(order + 'H', value) + b'\0\0' if field_type == 3 else struct.pack(order + 'I', value)
    return (b'Exif\0\0' + (b'II' if order == '<' else b'MM') +
            struct.pack(order + 'HIH', 42, 8, 1) +
            struct.pack(order + 'HHI', 274, field_type, count) + slot + struct.pack(order + 'I', 0))

for order, label in [('<', 'le'), ('>', 'be')]:
    for value in range(1, 9):
        image.save(directory / f'orientation-long-{label}-{value}.jpg', quality=94,
                   subsampling=2, exif=orientation_exif(value, order))
    for value in (9, 65537, 0xffffffff):
        image.save(directory / f'reject-orientation-long-{label}-{value}.jpg',
                   exif=orientation_exif(value, order))
    for field_type, count in [(4, 0), (4, 2), (9, 1)]:
        image.save(directory / f'reject-orientation-schema-{label}-{field_type}-{count}.jpg',
                   exif=orientation_exif(6, order, field_type, count))
for order, label in [('<', 'le'), ('>', 'be')]:
    for kind in (3, 4):
        image.save(directory / f'orientation-undefined-{label}-{kind}.jpg', quality=94,
                   exif=orientation_exif(0, order, kind))
for subsampling in (0, 1):
    image.save(directory / f'chroma-{subsampling}.jpg', quality=94, subsampling=subsampling)
image.save(directory / 'icc.jpg', quality=94, icc_profile=icc)
image.resize((193, 129)).save(directory / 'odd.jpg', quality=94)
image.save(directory / 'sef.jpg', quality=94)
with (directory / 'sef.jpg').open('ab') as out:
    out.write(sef())
s23_records = [(0x0a01, b'Image_UTC_Data', b'1700000000000'),
               (0x0aa1, b'MCC_Data', b'222'),
               (0x0d01, b'Camera_Scene_Info', b'19'),
               (0x0cc1, b'Color_Display_P3', b'\x0c\x06\x06'),
               (0x0c61, b'Camera_Capture_Mode_Info', b'1')]
image.save(directory / 'sef-s23.jpg', quality=94, icc_profile=icc)
with (directory / 'sef-s23.jpg').open('ab') as out:
    out.write(sef(s23_records, 107))
for suffix, scene, capture in [('short', b'0', b'1'), ('longer', b'123', b'12')]:
    path = directory / f'sef-s23-{suffix}.jpg'
    image.save(path, quality=94)
    with path.open('ab') as out:
        out.write(sef([(0x0d01, b'Camera_Scene_Info', scene),
                       (0x0c61, b'Camera_Capture_Mode_Info', capture)], 107))
# Photo selfie records are opaque: preserve binary variants and text terminators too.
for version in (103, 105, 106, 107):
    for variant, payload in [('text', b'Front_Cam_Selfie_Info'),
                             ('binary', bytes(range(256))),
                             ('terminated', b'Front_Cam_Selfie_Info\0'),
                             ('empty', b'')]:
        path = directory / f'sef-selfie-{version}-{variant}.jpg'
        image.save(path, quality=94)
        with path.open('ab') as out:
            out.write(sef([(0x0910, b'Front_Cam_Selfie_Info', payload),
                           (0x0aa1, b'MCC_Data', b'222')], version))
# Burst identifiers are kept as opaque metadata, including non-text payload variants.
for version in (103, 105, 106, 107):
    for variant, payload in [('text', b'20180415192019'),
                             ('binary', bytes(range(256))),
                             ('terminated', b'20180415192019\0'),
                             ('empty', b'')]:
        path = directory / f'sef-burst-{version}-{variant}.jpg'
        image.save(path, quality=94)
        with path.open('ab') as out:
            out.write(sef([(0x09e0, b'Burst_Shot_Info', payload),
                           (0x0910, b'Front_Cam_Selfie_Info', b'Front_Cam_Selfie_Info'),
                           (0x0aa1, b'MCC_Data', b'222')], version))
# Archival-only records: their exact type/name pair is recognized, their payload
# is never parsed as colour/edit/rotation instructions for the converted picture.
photo_records = [
    (0x0ce1, b'Gallery_DC_Data'), (0x0ba1, b'PhotoEditor_Re_Edit_Data'),
    (0x0ba1, b'Original_Path_Hash_Key'), (0x0ba1, b'deco_doodle_bitmap'),
    (0x0ba1, b'deco_sticker_bitmap'), (0x0ba1, b'deco_text_bitmap'),
    (0x0ba2, b'Copy_Available_Edit_Info'), (0x0b51, b'Intelligent_PhotoEditor_Data'),
    (0x0b60, b'UltraWide_PhotoEditor_Data'), (0x0bf0, b'Remaster_Info'),
    (0x0d91, b'PEg_Info'), (0x09f0, b'Pro_Mode_Info'),
    (0x08e0, b'Panorama_Shot_Info'), (0x0b40, b'SingleShotMeta'),
    (0x0b41, b'SingleShot_DepthMap_1'), (0x0b41, b'SingeShot_DepthMap_1'),
    (0x0b41, b'SingeShot_DepthMap_5'), (0x0b41, b'SingeShot_DepthMap_8'),
    (0x0b41, b'SingeShot_DepthMap_10'), (0x0ab0, b'DualShot_Meta_Info'),
    (0x0ab3, b'DualShot_Extra_Info'), (0x0ab4, b'DualShot_Core_Info'),
    (0x0ab1, b'DualShot_DepthMap_1'), (0x0ab1, b'DualShot_DepthMap_5'),
    (0x09e1, b'BurstShot_Best_Photo_Info'), (0x0bd0, b'Dual_Relighting_Bokeh_Info'),
    (0x0c21, b'Portrait_Effect_Info'), (0x0b90, b'Document_Scan_Info'),
    (0x0d01, b'Camera_Scene_Info'), (0x0d21, b'Camera_Scene_Info'),
    (0x0d01, b'Camera_Scene_Info2'), (0x0d01, b'Camera_Scene_Info3'),
    (0x0001, b'SingleShot'), (0x0001, b'DualShot_1'), (0x0001, b'DualShot_2')]
opaque_records = [(kind, name, b'\0\xff' + bytes(range(256)) + b'{"rotation":90,"originalPath":"synthetic.jpg"}')
                  for kind, name in photo_records]
for version in (103, 105, 106, 107):
    path = directory / f'sef-photo-records-{version}.jpg'
    image.save(path, quality=94)
    with path.open('ab') as out:
        out.write(sef(opaque_records, version))
for variant, payload in [('empty', b''), ('json', b'{"rotation":90,"scale":0.5}')]:
    path = directory / f'sef-photo-records-{variant}.jpg'
    image.save(path, quality=94)
    with path.open('ab') as out:
        out.write(sef([(kind, name, payload) for kind, name in photo_records], 107))
for variant, tail in [('reverse', sef(opaque_records, 107, list(reversed(range(len(opaque_records)))))),
                      ('permuted', sef(opaque_records, 107, list(range(1, len(opaque_records))) + [0]))]:
    path = directory / f'sef-directory-{variant}.jpg'
    image.save(path, quality=94)
    with path.open('ab') as out:
        out.write(tail)
for kind, name in photo_records:
    path = directory / (f'reject-photo-type-{kind:04x}-' + name.decode() + '.jpg')
    image.save(path)
    with path.open('ab') as out:
        out.write(sef([(kind + 1, name, b'opaque')], 107))
# Actual embedded JPEG payloads are preserved, never promoted to a second HEIF image.
embedded_path = directory / 'sef-embedded-jpeg.jpg'
image.save(embedded_path, quality=94)
embedded_bytes = embedded_path.read_bytes()
with embedded_path.open('ab') as out:
    out.write(sef([(0x0001, b'SingleShot', embedded_bytes),
                   (0x0001, b'DualShot_1', embedded_bytes),
                   (0x0001, b'DualShot_2', embedded_bytes)], 107))
# A correct record header cannot legitimize duplicate directory references or holes.
small_records = opaque_records[:2]
aliased = bytearray(sef(small_records, 107))
directory_at = len(aliased) - 8 - struct.unpack_from('<I', aliased, len(aliased) - 8)[0]
aliased[directory_at + 24:directory_at + 36] = aliased[directory_at + 12:directory_at + 24]
external = bytearray(sef(small_records, 107))
struct.pack_into('<I', external, directory_at + 16, directory_at + 1)
for variant, tail in [('alias', aliased), ('external', external),
                      ('gap', sef(small_records, 107, data_prefix=b'X'))]:
    path = directory / f'reject-sef-directory-{variant}.jpg'
    image.save(path)
    with path.open('ab') as out:
        out.write(tail)
# The new names are not a blanket exemption for malformed records or extra media.
invalid_sef = [
    ('editor-unknown-name', [(0x0ba1, b'Unknown_Editor_Data', b'opaque')]),
    ('depth-unknown-suffix', [(0x0b41, b'SingeShot_DepthMap_99', b'opaque')]),
    ('interactive-panorama', [(0x0202, b'Interactive_Panorama_000', b'video')]),
    ('burst-type', [(0x09e1, b'Burst_Shot_Info', b'opaque')]),
    ('burst-name', [(0x09e0, b'Unknown_Burst_Info', b'opaque')]),
    ('selfie-type', [(0x0911, b'Front_Cam_Selfie_Info', b'opaque')]),
    ('selfie-name', [(0x0910, b'Unknown_Selfie_Info', b'opaque')]),
    ('scene-type', [(0x0d22, b'Camera_Scene_Info', b'19')]),
    ('embedded-unknown-name', [(0x0001, b'SingleShot_Unknown', b'opaque')]),
    ('capture-type', [(0x0c62, b'Camera_Capture_Mode_Info', b'1')]),
    ('capture-length', [(0x0c61, b'Camera_Capture_Mode_Info', b'12345678901234567')]),
    ('capture-empty', [(0x0c61, b'Camera_Capture_Mode_Info', b'')]),
    ('capture-value', [(0x0c61, b'Camera_Capture_Mode_Info', b'x')]),
    ('p3-type', [(0x0cc2, b'Color_Display_P3', b'\x0c\x06\x06')]),
    ('p3-length', [(0x0cc1, b'Color_Display_P3', b'\x0c\x06')]),
    ('hdr', [(0x0cd2, b'Photo_HDR_Info', b'opaque')]),
    ('motion', [(0x0a30, b'MotionPhoto_Data', b'video')]),
]
for name, records in invalid_sef:
    path = directory / f'reject-sef-{name}.jpg'
    image.save(path, quality=94)
    with path.open('ab') as out:
        out.write(sef(records, 107))
path = directory / 'reject-sef-over-limit.jpg'
image.save(path)
with path.open('ab') as out:
    out.write(sef([(0x09e1, b'BurstShot_Best_Photo_Info', bytes(52 * 1024 * 1024))], 107))
path = directory / 'reject-sef-truncated.jpg'
image.save(path)
with path.open('ab') as out:
    out.write(sef(s23_records, 107)[:-1])
# Observed post-EOI padding: eleven D9 bytes then 172626 zeros. Keep every byte.
for name, tail in [('observed', b'\xd9' * 11 + bytes(172626)),
                   ('zero-only', bytes(48)), ('d9-only', b'\xd9' * 11),
                   ('short', b'\xd9\0'), ('zero-short', b'\0'),
                   ('max', b'\xd9' * 11 + bytes(4 * 1024 * 1024 - 11))]:
    path = directory / f'padding-{name}.jpg'
    image.save(path, quality=94)
    with path.open('ab') as out:
        out.write(tail)
for name, tail in [('padding-mixed', b'\xd9' * 11 + bytes(64) + b'X'),
                   ('padding-d9-after-zero', b'\xd9\0\xd9'),
                   ('padding-over-limit', bytes(4 * 1024 * 1024 + 1))]:
    path = directory / f'reject-{name}.jpg'
    image.save(path, quality=94)
    with path.open('ab') as out:
        out.write(tail)
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

# Actual observed shapes, generated without including private originals.
base = directory / 'eoi-reference.jpg'
image.save(base, quality=94)
plain = base.read_bytes()
assert plain.endswith(b'\xff\xd9')
base.write_bytes(plain + sef(version=106))
(directory / 'missing-eoi-sef.jpg').write_bytes(plain[:-2] + sef(version=106))
for name, data in [
    ('plain-eof', plain[:-2]),
    ('unindexed-tail', plain[:-2] + b'opaque'),
    ('incomplete-marker', plain[:-2] + b'\xff' + sef()),
    ('unknown-sef', plain[:-2] + sef([(0x1234, b'Unknown_Record', b'0')])),
    ('broken-sef', plain[:-2] + sef()[:-8] + b'\0\0\0\0SEFT'),
    ('damaged-scan', plain[:-202] + sef()),
]:
    (directory / ('reject-missing-eoi-' + name + '.jpg')).write_bytes(data)
opaque = b'\xd9\xd9\xd9\xe7\xdb\xd8\xfe\xb5' + bytes(range(256)) * 15
(directory / 'opaque-padded-tail.jpg').write_bytes(plain + opaque + bytes(173259))
for name, tail in [
    ('short-zero-area', opaque + bytes(65535)),
    ('large-prefix', bytes([1]) * 65537 + bytes(65536)),
    ('embedded-jpeg', b'\xff\xd8' + opaque + bytes(65536)),
    ('embedded-video', b'ftypisom' + opaque + bytes(65536)),
    ('embedded-sef', b'SEFH' + opaque + bytes(65536)),
    ('nonzero-end', opaque + bytes(65536) + b'X'),
    ('over-limit', opaque + bytes(4 * 1024 * 1024)),
]:
    (directory / ('reject-opaque-padded-' + name + '.jpg')).write_bytes(plain + tail)

for kind, name in [(0x0a31, b'MotionPhoto_Version'), (0x08d0, b'Interactive_Panorama_Info'),
                   (0x08e1, b'Motion_Panorama_Info'), (0x0bc0, b'Single_Relighting_Bokeh_Info'), (0x0b41, b'SingeShot_DepthMap_6'),
                   (0x0201, b'Motion_Panorama_MP4_000'), (0x0201, b'Interactive_Panorama_000')]:
    path = directory / (name.decode() + '.jpg')
    path.write_bytes(plain + sef([(kind, name, b'\0\xffopaque archival data')]))

engine = str(Path('image-host-build/image_smoke').resolve())
for path in sorted(directory.glob('*.jpg')):
    if path.name.startswith('original-'):
        continue
    output = path.with_suffix('.heic')
    output.unlink(missing_ok=True)
    result = subprocess.run([engine, str(path), str(output), '60'], capture_output=True, text=True)
    path.with_suffix('.log').write_text(result.stdout + result.stderr)
    inspected = subprocess.run([engine, 'inspect', str(path)], capture_output=True, text=True)
    if result.returncode == 0:
        assert inspected.returncode == 0 and 'Pixel compressi non decodificati' in inspected.stdout, (path, inspected.stderr)
    if inspected.returncode != 0:
        assert result.returncode != 0 and inspected.stderr == result.stderr, (path, inspected.stderr, result.stderr)
    if path.name.startswith('reject-'):
        assert result.returncode != 0 and not output.exists(), (path, result.stdout, result.stderr)
    else:
        assert result.returncode == 0 and output.stat().st_size > 0, (path, result.stdout, result.stderr)
        checked = subprocess.run([engine, 'verify', str(path), str(output)], capture_output=True, text=True)
        assert checked.returncode == 0, (path, checked.stderr)
        if path.stem.startswith('orientation-long-'):
            ppm = path.with_suffix('.ppm')
            decoded = subprocess.run([engine, 'decode-oriented', str(output), str(ppm)], capture_output=True, text=True)
            assert decoded.returncode == 0, (path, decoded.stderr)
            with Image.open(ppm) as actual, Image.open(path) as original:
                reference = ImageOps.exif_transpose(original)
                assert actual.size == reference.size, (path, actual.size, reference.size)
                error = sum(ImageStat.Stat(ImageChops.difference(reference, actual)).mean) / 3
                assert error < 8, (path, 'LONG orientation differs from independently oriented pixels', error)
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
# Each new proprietary value must survive unchanged in the original-data archive.
source = directory / 'sef-s23.jpg'
original = source.with_suffix('.heic').read_bytes()
archive_start = original.index(b'CPJPEG1\0')
for name in (b'Camera_Scene_Info', b'Color_Display_P3', b'Camera_Capture_Mode_Info'):
    data = bytearray(original)
    position = data.index(name, archive_start) + len(name)
    data[position] ^= 1
    corrupted = directory / ('corrupt-' + name.decode() + '.heic')
    corrupted.write_bytes(data)
    result = subprocess.run([engine, 'verify', str(source), str(corrupted)], capture_output=True, text=True)
    assert result.returncode != 0 and 'Archivio APP/COM/SEF diverso' in result.stderr, (name, result.stderr)
# The complete selfie SEF tail must occur in the proprietary archive unchanged.
# Tampering with its payload must fail verification before an output is accepted.
for source in sorted(list(directory.glob('sef-selfie-*.jpg')) + list(directory.glob('sef-burst-*.jpg'))):
    source_data = source.read_bytes()
    name = b'Burst_Shot_Info' if source.stem.startswith('sef-burst-') else b'Front_Cam_Selfie_Info'
    start = source_data.index(name) - 8
    tail = source_data[start:]
    original = source.with_suffix('.heic').read_bytes()
    archive_start = original.index(b'CPJPEG1\0')
    position = original.index(tail, archive_start)
    if source.stem.endswith('-empty'):
        continue
    data = bytearray(original)
    data[position + 8 + len(name)] ^= 1
    corrupted = directory / (source.stem + '-corrupt.heic')
    corrupted.write_bytes(data)
    result = subprocess.run([engine, 'verify', str(source), str(corrupted)], capture_output=True, text=True)
    assert result.returncode != 0 and 'Archivio APP/COM/SEF diverso' in result.stderr, (source, result.stderr)
# Report the real out-of-range orientation and bounded raw tail information.
for value in (9,):
    path = directory / f'original-invalid-orientation-{value}.jpg'
    exif = Image.Exif()
    exif[274] = value
    image.save(path, exif=exif)
    result = subprocess.run([engine, str(path), str(path.with_suffix('.heic')), '60'], capture_output=True, text=True)
    assert result.returncode != 0 and f'valore={value}' in result.stderr, result.stderr
for name, tail in [('short', b'XYZ'), ('mixed-padding', bytes(48) + b'X'),
                   ('long', bytes(range(256)) * 32)]:
    path = directory / f'original-unknown-tail-{name}.jpg'
    image.save(path)
    with path.open('ab') as out:
        out.write(tail)
    result = subprocess.run([engine, str(path), str(path.with_suffix('.heic')), '60'], capture_output=True, text=True)
    assert result.returncode != 0 and f'byte={len(tail)};' in result.stderr, result.stderr
    assert 'primi=' in result.stderr and 'ultimi=' in result.stderr and len(result.stderr) < 512, result.stderr
# Complete padding survives in the archive; changing or removing it is detected.
for source in sorted(directory.glob('padding-*.jpg')):
    source_data = source.read_bytes()
    end = source_data.index(b'\xff\xd9') + 2
    tail = source_data[end:]
    original = source.with_suffix('.heic').read_bytes()
    archive_start = original.index(b'CPJPEG1\0')
    position = archive_start + 8
    blocks = struct.unpack_from('>I', original, position)[0]
    position += 4
    for _ in range(blocks):
        length = struct.unpack_from('>I', original, position + 1)[0]
        position += 5 + length
    length = struct.unpack_from('>I', original, position)[0]
    position += 4
    assert length == len(tail) and original[position:position + length] == tail
    changed = bytearray(original)
    changed[position + len(tail) - 1] ^= 1
    corrupted = directory / (source.stem + '-changed.heic')
    corrupted.write_bytes(changed)
    result = subprocess.run([engine, 'verify', str(source), str(corrupted)], capture_output=True, text=True)
    assert result.returncode != 0 and 'Archivio APP/COM/SEF diverso' in result.stderr, (source, result.stderr)
# Every archival field is preserved in the HEIC; altering any payload is detected.
source = directory / 'sef-photo-records-107.jpg'
original = source.with_suffix('.heic').read_bytes()
archive_start = original.index(b'CPJPEG1\0')
for kind, name in photo_records:
    data = bytearray(original)
    position = data.index(struct.pack('<HHI', 0, kind, len(name)) + name, archive_start) + 8 + len(name)
    data[position] ^= 1
    corrupted = directory / (f'corrupt-photo-record-{kind:04x}-' + name.decode() + '.heic')
    corrupted.write_bytes(data)
    result = subprocess.run([engine, 'verify', str(source), str(corrupted)], capture_output=True, text=True)
    assert result.returncode != 0 and 'Archivio APP/COM/SEF diverso' in result.stderr, (name, result.stderr)
print('OK photo SEF catalog: 35 archival record types/names, binary/empty/JSON payloads, four SEF versions, every altered payload and wrong type rejected; unordered directories accepted; alias/external/gap references rejected')
print('OK JPEG padding: observed D9+zero tail and bounded variants preserved byte for byte; mixed/reordered/oversized tails and archive tampering rejected')
print('OK burst SEF: versions 103/105/106/107, text/binary/terminated/empty payloads preserved byte for byte alongside selfie and MCC; tampering and wrong type/name rejected; bounded tail and explicit EXIF diagnostics')
print('OK selfie SEF: versions 103/105/106/107, text/binary/terminated/empty payloads preserved byte for byte; altered archive and wrong type/name rejected')
print('OK HEIC: EXIF/GPS/date, eight orientations, ICC, XMP, opaque APP, SEF including S23 v107 records, wrong SEF type/length/value and HDR and malformed Motion Photo rejection, altered SEF archive rejection, 420/422/444, odd dimensions, rejected inputs, altered metadata; dedicated 1024 px thumbnails, oriented pixels, colour profiles, small-image boundary and missing-thumbnail rejection')

# A structurally traversable JPEG with no usable image data proves inspection
# does not ask the decoder to validate pixels (and must never claim that it does).
probe = directory / 'metadata-probe-without-pixels.jpg'
probe.write_bytes(b'\xff\xd8\xff\xda\x00\x08\x01\x01\x00\x00\x3f\x00\xff\xd9')
inspected = subprocess.run([engine, 'inspect', str(probe)], capture_output=True, text=True)
assert inspected.returncode == 0 and 'Pixel compressi non decodificati' in inspected.stdout, inspected.stderr
converted = subprocess.run([engine, str(probe), str(probe.with_suffix('.heic'))], capture_output=True, text=True)
assert converted.returncode != 0 and not probe.with_suffix('.heic').exists(), converted.stdout
print('OK metadata preflight: same parser and rejections as conversion; no decode for traversable JPEG without pixels; no HEIC produced')

print('OK EXIF LONG orientation: both byte orders, all eight transforms and actual pixels, raw EXIF preserved; undefined zero retained without rotation; out-of-range/full-width values, wrong count and signed type rejected')

# Repair changes only the decoder input; the same JPEG with EOI gives identical
# converted pixels. The complete original trailer remains in the CPJPEG1 archive.
for name in ('eoi-reference', 'missing-eoi-sef'):
    result = subprocess.run([engine, 'decode-oriented', str(directory / (name + '.heic')),
                             str(directory / (name + '.ppm'))], capture_output=True, text=True)
    assert result.returncode == 0, result.stderr
assert (directory / 'eoi-reference.ppm').read_bytes() == (directory / 'missing-eoi-sef.ppm').read_bytes()
for name, note in [('missing-eoi-sef', 'marcatore finale da ricostruire'),
                   ('opaque-padded-tail', 'coda non identificata archiviabile')]:
    source = directory / (name + '.jpg')
    result = subprocess.run([engine, 'inspect', str(source)], capture_output=True, text=True)
    assert result.returncode == 0 and note in result.stdout.splitlines()[0], result.stdout
    tail = sef(version=106) if name == 'missing-eoi-sef' else opaque + bytes(173259)
    data = source.with_suffix('.heic').read_bytes()
    position = data.index(tail, data.index(b'CPJPEG1\0'))
    for offset in (0, len(tail) - 1):
        changed = bytearray(data)
        changed[position + offset] ^= 1
        bad = directory / (name + '-archive-altered.heic')
        bad.write_bytes(changed)
        checked = subprocess.run([engine, 'verify', str(source), str(bad)], capture_output=True, text=True)
        assert checked.returncode != 0 and 'Archivio APP/COM/SEF diverso' in checked.stderr, checked.stderr
print('OK bounded opaque padded tails: all original bytes and zero area preserved, altered archive rejected; short/unbounded/embedded-image/video/SEF patterns rejected')
print('OK missing EOI before fully indexed supported SEF: exact same converted pixels as EOI reference, full tail archived; ordinary EOF, partial markers, malformed/unknown SEF and damaged entropy rejected')

# Header-only boundary checks do not allocate hundreds of MB or claim that
# changing SOF dimensions makes the small entropy payload a valid large image.
small = (directory / 'orientation-1.jpg').read_bytes()
sof = small.index(b'\xff\xc0')
for name, width, height, accepted in [
        ('50mp', 8160, 6120, True),
        ('200mp-camera', 16320, 12240, True),
        ('200mp-exact', 16000, 12500, True),
        ('over-200mp', 16000, 12501, False),
        ('panorama', 23728, 3744, True),
        ('panorama-16416', 16416, 3520, True),
        ('panorama-19840', 19840, 3744, True),
        ('panorama-17296', 17296, 3760, True),
        ('panorama-22480', 22480, 3600, True),
        ('portrait-panorama', 3744, 23728, True),
        ('jpeg-side-max', 65535, 64, True),
        ('long-over-200mp', 65535, 3052, False),
        ('too-small-side', 63, 128, False)]:
    data = bytearray(small)
    data[sof + 5:sof + 9] = struct.pack('>HH', height, width)
    path = directory / ('dimension-boundary-' + name + '.bin')
    path.write_bytes(data)
    checked = subprocess.run([engine, 'inspect', str(path)], capture_output=True, text=True)
    assert (checked.returncode == 0) == accepted, (name, checked.stderr)
    if not accepted:
        assert 'massimo 200 megapixel' in checked.stderr, checked.stderr
        converted = subprocess.run([engine, str(path), str(path.with_suffix('.heic'))], capture_output=True, text=True)
        assert converted.returncode != 0 and converted.stderr == checked.stderr, converted.stderr
        assert not path.with_suffix('.heic').exists()
print('OK JPEG dimensions: 50/200 MP, five reported panoramas and long-axis headers accepted; area/minimum-side violations rejected before pixel decoding')
