#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
cmake -S app/src/main/cpp/audio -B audio-smoke
cmake --build audio-smoke --parallel 2
mkdir -p audio-fixtures
# Exercise 44.1 kHz resampling/24 bit/stereo, 48 kHz mono and 96 kHz input.
for spec in '44100:2:s32' '48000:1:s16' '96000:2:s32'; do
  IFS=: read -r rate channels sample <<< "$spec"
  stem="audio-fixtures/$rate-$channels"
  ffmpeg -y -v error -f lavfi -i "sine=frequency=997:sample_rate=$rate:duration=1.1" -ac "$channels" -sample_fmt "$sample" -c:a flac "$stem.flac"
  python3 scripts/audio-metadata-fixture.py create "$stem.flac"
  audio-smoke/audio_smoke "$stem.flac" "$stem.opus" encode 2>&1 | tee "$stem.log"
  python3 scripts/audio-metadata-fixture.py verify "$stem.flac" "$stem.opus"
  ffmpeg -y -v error -xerror -i "$stem.opus" -map 0:a:0 -c:a pcm_f32le -f f32le "$stem.pcm"
  python3 - "$stem.pcm" "$channels" <<'PY'
import sys
from pathlib import Path
size=Path(sys.argv[1]).stat().st_size
assert size==52800*int(sys.argv[2])*4, (size, sys.argv)
PY
  grep -q 'PARAMETRI EFFETTIVI DOPO DRAIN: bitrate=128000 bit/s · VBR=1 · constrained=0 · complexity=10' "$stem.log"
  audio-smoke/audio_smoke "$stem.flac" "$stem-cancel.opus" cancel
  audio-smoke/audio_smoke "$stem.flac.application.flac" "$stem-application.opus" encode
  python3 scripts/audio-metadata-fixture.py verify "$stem.flac.application.flac" "$stem-application.opus"
  for invalid in reserved shortapplication invalid; do
    if audio-smoke/audio_smoke "$stem.flac.$invalid.flac" "$stem-$invalid.opus" encode; then exit 1; fi
  done
  # Truncated FLAC must fail; it must never become a successful conversion.
  head -c 100 "$stem.flac" > "$stem-broken.flac"
  if audio-smoke/audio_smoke "$stem-broken.flac" "$stem-broken.opus" encode; then exit 1; fi
done
# Reject multichannel explicitly until mapping and playback have their own coverage.
ffmpeg -y -v error -f lavfi -i 'anullsrc=r=48000:cl=5.1' -t 0.1 -c:a flac audio-fixtures/multichannel.flac
if audio-smoke/audio_smoke audio-fixtures/multichannel.flac audio-fixtures/multichannel.opus encode; then exit 1; fi
printf '%s\n' 'OK: FLAC→Opus VBR, full decode/sample counts, resampling, mono/stereo, cancellation and invalid input'

# MP3 and AAC/M4A: common tags, UTF-8 text and PNG cover, then bounded PCM streaming.
python3 - <<'PY'
import struct,zlib
from pathlib import Path
def chunk(k,d): return struct.pack('>I',len(d))+k+d+struct.pack('>I',zlib.crc32(k+d))
Path('audio-fixtures/cover.png').write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',1,1,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(b'\0\xff\0\0'))+chunk(b'IEND',b''))
PY
for ext in mp3 m4a; do
  codec=libmp3lame; kind=1
  if [ "$ext" = m4a ]; then codec=aac; kind=2; fi
  stem="audio-fixtures/compressed-$ext"
  ffmpeg -y -v error -f lavfi -i 'sine=frequency=997:sample_rate=44100:duration=1.1' -i audio-fixtures/cover.png -map 0:a -map 1:v -c:a "$codec" -ac 2 -c:v copy -disposition:v attached_pic -metadata title='Titolo è 漢字' -metadata artist='Artista' -metadata album='Album' -metadata lyrics='Testo prima riga' -metadata track='2/12' "$stem.$ext"
  if [ "$ext" = mp3 ]; then
    # FFmpeg writes its lyrics option as a TXXX field on some versions.
    # Add a real ID3v2.4 USLT frame to exercise TagLib's standard LYRICS mapping.
    python3 - "$stem.mp3" <<'PYTAG'
from pathlib import Path
import sys
p=Path(sys.argv[1]); data=p.read_bytes(); assert data[:4]==b'ID3\x04'
def syncsafe(n):return bytes((n>>21&127,n>>14&127,n>>7&127,n&127))
size=0
for b in data[6:10]:size=(size<<7)|b
payload=b'\x03eng\0'+b'Testo prima riga'
frame=b'USLT'+syncsafe(len(payload))+b'\0\0'+payload
p.write_bytes(data[:6]+syncsafe(size+len(frame))+frame+data[10:])
PYTAG
  fi
  ffmpeg -y -v error -xerror -i "$stem.$ext" -map 0:a:0 -c:a pcm_f32le -f f32le "$stem-input.pcm"
  audio-smoke/audio_pcm_smoke "$stem.$ext" "$stem.opus" "$kind" 44100 2 "$stem-input.pcm" 2>&1 | tee "$stem.log"
  ffmpeg -y -v error -xerror -i "$stem.opus" -map 0:a:0 -c:a pcm_f32le -f f32le "$stem-output.pcm"
  python3 - "$stem" <<'PY'
import sys,base64,struct
from pathlib import Path
stem=sys.argv[1]
# FFprobe may expose a cover as a stream instead of the METADATA_BLOCK_PICTURE tag.
# Inspect OpusTags directly to compare embedded image bytes without relying on TagLib.
p=Path(stem+'.opus').read_bytes(); at=0; packets=[]; packet=b''
while at<len(p) and len(packets)<2:
    assert p[at:at+4]==b'OggS'; n=p[at+26]; laces=p[at+27:at+27+n]; at+=27+n
    for size in laces:
        packet+=p[at:at+size];at+=size
        if size<255:packets.append(packet);packet=b''
p=packets[1]; at=8
read=lambda offset:struct.unpack_from('<I',p,offset)[0]
at+=4+read(at);count=read(at);at+=4; comments=[]
for _ in range(count):
    size=read(at);at+=4;comments.append(p[at:at+size]);at+=size
tags={k.decode('utf-8').upper():v.decode('utf-8') for k,v in (c.split(b'=',1) for c in comments)}
assert tags['TITLE']=='Titolo è 漢字',tags
assert tags['ARTIST']=='Artista' and tags['ALBUM']=='Album',tags
assert tags['LYRICS']=='Testo prima riga',tags
pictures=[base64.b64decode(v.split(b'=',1)[1]) for v in comments if v.startswith(b'METADATA_BLOCK_PICTURE=')]
assert len(pictures)==1
q=pictures[0];at=4
for _ in range(2):size=struct.unpack_from('>I',q,at)[0];at+=4+size
at+=16;size=struct.unpack_from('>I',q,at)[0];at+=4
assert q[at:at+size]==Path('audio-fixtures/cover.png').read_bytes()
input_samples=Path(stem+'-input.pcm').stat().st_size//8
output_samples=Path(stem+'-output.pcm').stat().st_size//8
assert abs(output_samples-round(input_samples*48000/44100))<=1,(input_samples,output_samples)
print('OK: MP3/M4A translated tags, exact original cover, streaming and decoded sample count')
PY
done
