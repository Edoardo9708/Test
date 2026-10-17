# Audio input extension — v0.64

MP3 and M4A are **input formats**, not new output encoders. Output remains Ogg Opus,
64–256 kbit/s, free VBR, complexity 10. MP3/AAC to Opus is lossy-to-lossy and does
not guarantee a smaller file. ALAC in M4A requires an available Android decoder.

## Ownership and modules

- `AudioConverter`: route, temporary output, verification, transactional publication.
- `AudioMediaDecoder`: one MediaExtractor, one MediaCodec, one native stream, all
  confined to the existing single audio worker and released in finally blocks.
- `AudioPcm`: explicit little-endian 8/16/24/32-bit integer and float conversion.
- `audio_stream`: bounded PCM blocks to the bundled libopusenc, parameter readback
  after drain, metadata verification before publication.
- `audio_taglib`: TagLib 2.3.2 reads ID3/MP4 fields and embedded pictures. Property
  values and repeated fields are translated into Opus comments. Picture bytes are
  unchanged. Container-specific tags do not have identical binary layouts in Opus.
  Unsupported/complex fields fail explicitly rather than disappearing silently.
  TagLib's unified MP3 property interface prioritizes ID3v2, then APE, then ID3v1;
  this is semantic tag transfer, not a backup of every competing legacy tag block.

FLAC remains on its existing libFLAC path, including CRC/MD5 integrity checks.
The shared conversion lease still prevents simultaneous video/audio conversions.

## Decoder behavior and limits

Mono/stereo, decoded PCM 8–192 kHz. M4A accepts AAC or ALAC only. DRM and multiple
input audio tracks are rejected explicitly. Compressed packets keep original
presentation times. Encoder delay/padding keys are passed unchanged to Android,
which manages gapless trimming; no second manual trim is applied. Container
MP3 duration can be estimated, so it is progress information, not a truncation
boundary. Final output duration is checked against the actual decoded PCM count.
A 15-second inactivity timeout and per-block cancellation protect the pump.
Only a fixed 8192-float Java buffer is used; there is no whole-track PCM cache.

## Validation

Robolectric covers format routing and PCM scale, signedness, offsets, malformed
buffers and non-finite samples. Native smoke fixtures use synthetic MP3/AAC inputs
with UTF-8 tags, lyrics and a PNG cover. FFmpeg independently decodes compressed
input to PCM for the streaming engine, then decodes Opus. Tests compare sample
counts, expected tags and exact picture bytes; the native engine also compares
all emitted tag bytes with the translated metadata. Existing FLAC tests remain.
This native test does not execute Android's input decoder: its device-specific
behavior still requires an MP3 and M4A test on the phone. On-device output
verification decodes every generated Opus file before saving.
