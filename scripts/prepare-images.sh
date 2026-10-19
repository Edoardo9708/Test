#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p vendor app/src/main/assets
archive=vendor/libheif-1.23.6.tar.gz
if [ ! -f "$archive" ]; then
  curl --fail --location --retry 3 https://github.com/strukturag/libheif/releases/download/v1.23.6/libheif-1.23.6.tar.gz -o "$archive"
fi
echo "4484346dc5995319dbc11e3a1c35d0a2ec46511ce370900869337fd2c7033125  $archive" | sha256sum --check
if [ ! -d vendor/libheif-1.23.6 ]; then tar --no-same-owner -xf "$archive" -C vendor; fi
cp vendor/libheif-1.23.6/COPYING app/src/main/assets/libheif-LGPL.txt
python3 scripts/patch-libheif.py
