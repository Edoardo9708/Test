#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/prepare-video.sh
bash scripts/build-video-decoder.sh "$PWD/vendor/ffmpeg-8.1.3" "$PWD/video-host-build" "$PWD/video-host-install" > video-host-build.log 2>&1
c++ -std=c++17 -O3 -Iapp/src/main/cpp/video -Ivideo-host-install/include tests/software_hevc_smoke.cpp app/src/main/cpp/video/hevc_decoder.cpp -Lvideo-host-install/lib -lavcodec -lavutil -pthread -lm -o video-host-build/smoke
python3 scripts/test-video-decoder.py
# Compile the same JNI bridge too, including direct-buffer and long-array signatures.
c++ -std=c++17 -O3 -shared -fPIC -I"${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}/include" -I"${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}/include/linux" -Ivideo-host-install/include app/src/main/cpp/video/decoder_jni.cpp app/src/main/cpp/video/hevc_decoder.cpp -Lvideo-host-install/lib -lavcodec -lavutil -pthread -lm -Wl,--exclude-libs,ALL -Wl,-Bsymbolic -o video-host-build/libsoftwarehevc.so
javac -d video-host-build/java tests/video-java/android/media/*.java tests/video-java/it/edo/codecprobe/*.java app/src/main/java/it/edo/codecprobe/ColorVui.java app/src/main/java/it/edo/codecprobe/HdrHevcInfo.java app/src/main/java/it/edo/codecprobe/SoftwareHevcDecoder.java
java -Djava.library.path=video-host-build -cp video-host-build/java it.edo.codecprobe.SoftwareHevcDecoderTest "$PWD/video-fixtures"
