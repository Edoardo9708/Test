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
  audio-smoke/audio_smoke "$stem.flac" "$stem.opus" encode > "$stem.log" 2>&1
  ffmpeg -y -v error -xerror -i "$stem.opus" -c:a pcm_f32le -f f32le "$stem.pcm"
  python3 - "$stem.pcm" "$channels" <<'PY'
import sys
from pathlib import Path
size=Path(sys.argv[1]).stat().st_size
assert size==52800*int(sys.argv[2])*4, (size, sys.argv)
PY
  grep -q 'VBR=1 · constrained=0 · complexity=10' "$stem.log"
  audio-smoke/audio_smoke "$stem.flac" "$stem-cancel.opus" cancel
  # Truncated FLAC must fail; it must never become a successful conversion.
  head -c 100 "$stem.flac" > "$stem-broken.flac"
  if audio-smoke/audio_smoke "$stem-broken.flac" "$stem-broken.opus" encode; then exit 1; fi
done
# Reject multichannel explicitly until mapping and playback have their own coverage.
ffmpeg -y -v error -f lavfi -i 'anullsrc=r=48000:cl=5.1' -t 0.1 -c:a flac audio-fixtures/multichannel.flac
if audio-smoke/audio_smoke audio-fixtures/multichannel.flac audio-fixtures/multichannel.opus encode; then exit 1; fi
printf '%s\n' 'OK: FLAC→Opus VBR, full decode/sample counts, resampling, mono/stereo, cancellation and invalid input'
