"""Synthetic Motion Photo integration cases. No private originals are included."""
from pathlib import Path
from io import BytesIO
import base64
import hashlib
import struct
import subprocess
import xml.etree.ElementTree as ET
from PIL import Image

root = Path("image-fixtures/motion")
root.mkdir(parents=True, exist_ok=True)
engine = str(Path("image-host-build/image_smoke").resolve())
video = base64.decodebytes(Path("tests/fixtures/motion-photo-video.mp4.b64").read_bytes())
buffer = BytesIO()
Image.new("RGB", (192, 128), (31, 91, 153)).save(buffer, format="JPEG", quality=93)
jpeg = buffer.getvalue()
camera = "http://ns.google.com/photos/1.0/camera/"
container = "http://ns.google.com/photos/1.0/container/"
item = "http://ns.google.com/photos/1.0/container/item/"
rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"


def sef(movie, duplicate=False):
    records = [(0x0a01, b"Image_UTC_Data", b"1663352834748"),
               (0x0aa1, b"MCC_Data", b"222"),
               (0x0a30, b"MotionPhoto_Data", movie)]
    if duplicate:
        records.append((0x0a30, b"MotionPhoto_Data", movie))
    payload = b""
    entries = []
    for kind, name, value in records:
        block = struct.pack("<HHI", 0, kind, len(name)) + name + value
        entries.append((kind, len(payload), len(block)))
        payload += block
    directory = b"SEFH" + struct.pack("<II", 106, len(entries))
    directory += b"".join(struct.pack("<HHII", 0, kind, len(payload) - start, size)
                          for kind, start, size in reversed(entries))
    return payload + directory + struct.pack("<I", len(directory)) + b"SEFT"


def packet(properties="", directory=""):
    return (f'<x:xmpmeta xmlns:x="adobe:ns:meta/"><r:RDF xmlns:r="{rdf}">'
            f'<r:Description xmlns:C="{camera}" xmlns:dc="http://purl.org/dc/elements/1.1/" '
            f'{properties}><dc:description>Test &amp; original metadata</dc:description>'
            f'{directory}</r:Description></r:RDF></x:xmpmeta>').encode()


def source(name, movie=video, xmp=None, duplicate=False):
    data = jpeg
    if xmp is not None:
        payload = b"http://ns.adobe.com/xap/1.0/\0" + xmp
        data = data[:2] + b"\xff\xe1" + struct.pack(">H", len(payload) + 2) + payload + data[2:]
    path = root / (name + ".jpg")
    path.with_suffix(".heic").unlink(missing_ok=True)
    path.write_bytes(data + sef(movie, duplicate))
    return path


def run(*args):
    return subprocess.run([engine, *map(str, args)], capture_output=True, text=True)


def boxes(data):
    at = 0
    while at < len(data):
        size, kind = struct.unpack_from(">I4s", data, at)
        header = 8
        if size == 1:
            size = struct.unpack_from(">Q", data, at + 8)[0]
            header = 16
        assert size >= header and at + size <= len(data)
        yield kind, at, at + header, at + size
        at += size
    assert at == len(data)


def verify_archive(path, movie, original_xmp=None):
    data = path.with_suffix(".heic").read_bytes()
    spans = [box for box in boxes(data) if box[0] == b"mpvd"]
    assert len(spans) == 1 and spans[0][3] == len(data)
    _, start, first, end = spans[0]
    extracted = data[first:end]
    assert extracted == movie
    # CPJPEG2 layout: blocks, original-tail length, video offset, video length,
    # followed by the original tail with exactly that video's byte range removed.
    at = data.index(b"CPJPEG2\0") + 8
    count = struct.unpack_from(">I", data, at)[0]
    at += 4
    originals = []
    for _ in range(count):
        marker, length = struct.unpack_from(">BI", data, at)
        at += 5
        originals.append((marker, data[at:at + length]))
        at += length
    length, offset, size = struct.unpack_from(">III", data, at)
    at += 12
    assert size == len(movie)
    archived = data[at:at + length - size]
    assert archived[:offset] + extracted + archived[offset:] == sef(movie)
    assert data.count(movie) == 1, "The original video must not be duplicated"
    if original_xmp is not None:
        assert any(marker == 0xe1 and payload == b"http://ns.adobe.com/xap/1.0/\0" + original_xmp
                   for marker, payload in originals)
    # Locate the active packet independently and validate URI-resolved properties.
    packets = []
    at = 0
    while True:
        at = data.find(b"<x:xmpmeta", at)
        if at < 0:
            break
        finish = data.find(b"</x:xmpmeta>", at)
        if finish >= 0:
            try:
                doc = ET.fromstring(data[at:finish + len(b"</x:xmpmeta>")])
                if any(entry.get("{" + item + "}Mime") == "image/heic" for entry in doc.findall(".//{" + container + "}Item")):
                    packets.append(doc)
            except ET.ParseError:
                pass
        at += 1
    assert len(packets) == 1
    entries = packets[0].findall(".//{" + container + "}Item")
    assert len(entries) == 2
    assert entries[0].get("{" + item + "}Mime") == "image/heic"
    assert entries[0].get("{" + item + "}Padding") == "8"
    assert entries[1].get("{" + item + "}Length") == str(len(movie))
    for element in packets[0].iter():
        assert all("MicroVideo" not in key for key in element.attrib)
    if original_xmp is not None:
        assert packets[0].find(".//{http://purl.org/dc/elements/1.1/}description").text == "Test & original metadata"
    for label, position in [("video", first + len(movie) // 2), ("archive", at_archive(data))]:
        changed = bytearray(data)
        changed[position] ^= 1
        bad = root / (path.stem + "-changed-" + label + ".heic")
        bad.write_bytes(changed)
        checked = run("verify", path, bad)
        assert checked.returncode != 0, (path, label, checked.stdout)


def at_archive(data):
    return data.index(b"CPJPEG2\0") + 12


large = video + struct.pack(">I4s", 5 * 1024 * 1024 + 8, b"free") + bytes(5 * 1024 * 1024)
legacy = packet(f'C:MicroVideo="1" C:MicroVideoVersion="1" C:MicroVideoOffset="{len(video) + 56}" C:MicroVideoPresentationTimestampUs="-1"')
directory = (f'<K:Directory xmlns:K="{container}" xmlns:I="{item}"><r:Seq>'
             '<r:li r:parseType="Resource"><K:Item I:Mime="image/jpeg" I:Semantic="Primary" I:Padding="78"/></r:li>'
             f'<r:li r:parseType="Resource"><K:Item I:Mime="video/mp4" I:Semantic="MotionPhoto" I:Length="{len(video)}"/></r:li>'
             '</r:Seq></K:Directory>')
modern = packet('C:MotionPhoto="1" C:MotionPhotoVersion="1" C:MotionPhotoPresentationTimestampUs="0"', directory)
element_properties = packet(directory='<C:MicroVideo>1</C:MicroVideo><C:MicroVideoVersion>1</C:MicroVideoVersion>'
                           f'<C:MicroVideoOffset>{len(video) + 56}</C:MicroVideoOffset>')
cases = [("sef-only", video, None), ("legacy", video, legacy), ("modern", video, modern),
         ("element-properties", video, element_properties),
         ("disabled", video, packet('C:MotionPhoto="0"')),
         ("over-4-MiB", large, None)]
for name, movie, xmp in cases:
    path = source(name, movie, xmp)
    digest = hashlib.sha256(path.read_bytes()).digest()
    inspected = run("inspect", path)
    assert inspected.returncode == 0 and "Motion Photo con video" in inspected.stdout, inspected.stderr
    result = run(path, path.with_suffix(".heic"), "60")
    path.with_suffix(".log").write_text(result.stdout + result.stderr)
    assert result.returncode == 0, (name, result.stderr)
    assert hashlib.sha256(path.read_bytes()).digest() == digest
    assert run("verify", path, path.with_suffix(".heic")).returncode == 0
    verify_archive(path, movie, xmp)

invalid = [
    ("wrong-offset", video, legacy.replace(str(len(video) + 56).encode(), b"1"), False),
    ("wrong-version", video, packet('C:MicroVideo="1" C:MicroVideoVersion="2"'), False),
    ("duplicate-property", video, packet('C:MicroVideo="1"', '<C:MicroVideo>1</C:MicroVideo>'), False),
    ("bad-xml", video, b"<broken", False),
    ("dtd", video, b'<!DOCTYPE x [<!ENTITY a SYSTEM "file:///etc/passwd">]>' + legacy, False),
    ("wrong-item-length", video, modern.replace(f'I:Length="{len(video)}"'.encode(), b'I:Length="1"'), False),
    ("missing-mp4", b"not a video" * 9, None, False),
    ("duplicate-video", video, None, True),
    ("truncated-mp4", video[:-9], None, False),
    ("video-over-48-MiB", large + bytes(44 * 1024 * 1024), None, False),
]
for name, movie, xmp, duplicate in invalid:
    path = source(name, movie, xmp, duplicate)
    output = path.with_suffix(".heic")
    result = run(path, output, "60")
    path.with_suffix(".log").write_text(result.stdout + result.stderr)
    assert result.returncode != 0 and not output.exists(), (name, result.stdout)
    assert run("inspect", path).returncode != 0
print("OK Motion Photo: SEF-only/legacy/modern/element XMP, disabled flag, 5 MiB video, exact MP4 and original XMP/SEF recovery, no duplicate video; corrupt video/archive, XML/offset/version/directory errors and limits rejected")
