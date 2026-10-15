#!/usr/bin/env bash
set -euo pipefail
mkdir -p yuv-test
javac -d yuv-test app/src/main/java/it/edo/codecprobe/ColorVui.java app/src/main/java/it/edo/codecprobe/HdrHevcInfo.java app/src/main/java/it/edo/codecprobe/HdrP010Packer.java tests/HdrP010PackerTest.java tests/HdrHeaderCheck.java
java -cp yuv-test it.edo.codecprobe.HdrP010PackerTest
javac -d yuv-test app/src/main/java/it/edo/codecprobe/OutputName.java tests/OutputNameTest.java
java -cp yuv-test it.edo.codecprobe.OutputNameTest
javac -d yuv-test app/src/main/java/it/edo/codecprobe/MetadataDump.java tests/MetadataDumpTest.java
java -cp yuv-test it.edo.codecprobe.MetadataDumpTest
javac -d yuv-test app/src/main/java/it/edo/codecprobe/ColorVui.java app/src/main/java/it/edo/codecprobe/ContainerColor.java tests/ColorVuiTest.java
java -cp yuv-test it.edo.codecprobe.ColorVuiTest
javac -d yuv-test app/src/main/java/it/edo/codecprobe/NumberValues.java tests/NumberValuesTest.java
java -cp yuv-test it.edo.codecprobe.NumberValuesTest
javac -d yuv-test app/src/main/java/it/edo/codecprobe/YuvPacker.java tests/YuvPackerTest.java
java -cp yuv-test it.edo.codecprobe.YuvPackerTest

javac -d yuv-test app/src/main/java/it/edo/codecprobe/Mp4Boxes.java tests/Mp4BoxesTest.java
java -cp yuv-test it.edo.codecprobe.Mp4BoxesTest

javac -d yuv-test app/src/main/java/it/edo/codecprobe/SamsungSef.java app/src/main/java/it/edo/codecprobe/Mp4Metadata.java tests/Mp4MetadataTest.java
java -cp yuv-test it.edo.codecprobe.Mp4MetadataTest




javac -d yuv-test app/src/main/java/it/edo/codecprobe/BatchProgress.java tests/BatchProgressTest.java
java -cp yuv-test it.edo.codecprobe.BatchProgressTest
javac -d yuv-test app/src/main/java/it/edo/codecprobe/FolderPaths.java tests/FolderPathsTest.java
java -cp yuv-test it.edo.codecprobe.FolderPathsTest

javac -d yuv-test app/src/main/java/it/edo/codecprobe/DestinationPlan.java tests/DestinationPlanTest.java
java -cp yuv-test it.edo.codecprobe.DestinationPlanTest
