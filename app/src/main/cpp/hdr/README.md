# HDR diagnostic module, phase 1

This directory is an independent x265 Main10 build. `VideoConverter.java` and
the SDR encoder implementation remain unchanged. Android packages a new
`libx265hdrprobe.so`, with its static x265 symbols hidden and locally bound so
they cannot resolve to the existing eight-bit library.

The Android `HdrProbe` checks at most six frames from selected HEVC Main10
BT.2020 PQ/HLG input. It requests a CPU-readable P010 decoder output, converts
the sample packing to planar ten-bit I420 without quantization, encodes with
Main10 CRF and verifies SPS depth/color, frame count, timestamps and P010 decode.
The diagnostic elementary stream exists only in cache and is deleted afterwards.

This is **not full HDR conversion**: audio, MP4 export, static mastering metadata
copy and HDR10+ dynamic metadata preservation are not implemented in this module.
Their presence is logged when the platform exposes them. Never infer absence
from missing platform metadata. Dolby Vision and other profiles are rejected.

Initial native settings are ultrafast/zerolatency, two threads, nominal 30 fps,
no ARM assembly; they establish sample-depth correctness before optimization.
Source timestamps are retained. The regular SDR pipeline keeps its existing
assembly and preset configuration. The user must keep the test activity open;
the bounded probe does not run in the SDR foreground service.

Host tests independently decode PQ/HLG output with FFmpeg and assert Main10,
yuv420p10le, exact CICP and range, six frames and retained low sample bits.
Android CI checks that both AArch64 DSOs are packaged and the HDR x265 API is
not exported. Real device P010 support must still be verified with the log.
