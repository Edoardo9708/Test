#!/usr/bin/env bash
set -euo pipefail
mkdir -p yuv-test
javac -d yuv-test app/src/main/java/it/edo/codecprobe/ColorVui.java app/src/main/java/it/edo/codecprobe/ContainerColor.java tests/ColorVuiTest.java
java -cp yuv-test it.edo.codecprobe.ColorVuiTest
javac -d yuv-test app/src/main/java/it/edo/codecprobe/NumberValues.java tests/NumberValuesTest.java
java -cp yuv-test it.edo.codecprobe.NumberValuesTest
javac -d yuv-test app/src/main/java/it/edo/codecprobe/YuvPacker.java tests/YuvPackerTest.java
java -cp yuv-test it.edo.codecprobe.YuvPackerTest

javac -d yuv-test app/src/main/java/it/edo/codecprobe/Mp4Boxes.java tests/Mp4BoxesTest.java
java -cp yuv-test it.edo.codecprobe.Mp4BoxesTest

javac -d yuv-test app/src/main/java/it/edo/codecprobe/Mp4Metadata.java tests/Mp4MetadataTest.java
java -cp yuv-test it.edo.codecprobe.Mp4MetadataTest
