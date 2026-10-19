#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
cmake -S app/src/main/cpp/images -B image-host-build
cmake --build image-host-build --parallel 2
/usr/bin/python3 scripts/image-fixtures.py
