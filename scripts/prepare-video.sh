#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p vendor app/src/main/assets
archive=vendor/ffmpeg-8.1.3.tar.xz
if [ ! -f "$archive" ]; then
  curl --fail --location --retry 3 https://ffmpeg.org/releases/ffmpeg-8.1.3.tar.xz -o "$archive"
fi
echo "7138d28c96d9d3e3af4ee3d8cad72741f8ffb40da90c1112235dea3ecd3178a3  $archive" | sha256sum --check
if [ ! -d vendor/ffmpeg-8.1.3 ]; then tar --no-same-owner -xf "$archive" -C vendor; fi
cp vendor/ffmpeg-8.1.3/COPYING.LGPLv2.1 app/src/main/assets/ffmpeg-LGPLv2.1.txt
