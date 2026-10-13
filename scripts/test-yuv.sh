#!/usr/bin/env bash
set -euo pipefail
mkdir -p yuv-test
javac -d yuv-test app/src/main/java/it/edo/codecprobe/YuvPacker.java tests/YuvPackerTest.java
java -cp yuv-test it.edo.codecprobe.YuvPackerTest
