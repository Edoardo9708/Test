#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p vendor app/src/main/assets
fetch() {
  local name="$1" archive="$2" digest="$3" directory="$4"
  local cache="vendor/$archive"
  if [ ! -f "$cache" ]; then
    curl --fail --location --retry 3 "https://downloads.xiph.org/releases/$name/$archive" -o "$cache"
  fi
  printf '%s  %s\n' "$digest" "$cache" | sha256sum --check
  if [ ! -d "vendor/$directory" ]; then tar -xf "$cache" -C vendor; fi
}
fetch opus opus-1.6.1.tar.gz 6ffcb593207be92584df15b32466ed64bbec99109f007c82205f0194572411a1 opus-1.6.1
fetch opus libopusenc-0.3.tar.gz f616d3aff9b2034547894ccb8ab56c36cf1a4acb0d922c5d7119f97bbe58642c libopusenc-0.3
fetch flac flac-1.5.0.tar.xz f2c1c76592a82ffff8413ba3c4a1299b6c7ab06c734dee03fd88630485c2b920 flac-1.5.0
cp vendor/opus-1.6.1/COPYING app/src/main/assets/opus-COPYING.txt
cp vendor/libopusenc-0.3/COPYING app/src/main/assets/libopusenc-COPYING.txt
cp vendor/flac-1.5.0/COPYING.Xiph app/src/main/assets/flac-COPYING.txt
