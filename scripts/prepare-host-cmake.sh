#!/usr/bin/env bash
# Pinned x265 still requires policies removed by CMake 4. Android keeps its SDK CMake.
set -euo pipefail
host_cmake_dir="${RUNNER_TEMP:?}/codecprobe-host-cmake"
python3 -m pip install --disable-pip-version-check --no-input --target "$host_cmake_dir" 'cmake==3.31.6'
"$host_cmake_dir/cmake/data/bin/cmake" --version
echo "$host_cmake_dir/cmake/data/bin" >> "${GITHUB_PATH:?}"
