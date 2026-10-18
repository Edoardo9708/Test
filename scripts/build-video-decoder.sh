#!/usr/bin/env bash
# Build only the software HEVC decoder and avutil, without external codecs or network I/O.
set -euo pipefail
source_dir="$1"
build_dir="$2"
install_dir="$3"
mkdir -p "$build_dir"
cd "$build_dir"
args=(--prefix="$install_dir" --disable-everything --disable-autodetect --disable-programs --disable-doc --disable-network
      --disable-avdevice --disable-avformat --disable-avfilter --disable-swscale --disable-swresample
      --enable-avcodec --enable-avutil --enable-decoder=hevc --enable-static --disable-shared --enable-pic
      --disable-debug --disable-x86asm --extra-cflags=-O3)
if [ "$#" -ge 4 ]; then
  compiler="$4"
  args+=(--enable-cross-compile --target-os=android --arch=aarch64 --cc="$compiler" --cxx="${compiler/clang/clang++}"
         --nm="$(dirname "$compiler")/llvm-nm" --ar="$(dirname "$compiler")/llvm-ar" --ranlib="$(dirname "$compiler")/llvm-ranlib" --strip="$(dirname "$compiler")/llvm-strip")
fi
bash "$source_dir/configure" "${args[@]}"
make -j2
make install
