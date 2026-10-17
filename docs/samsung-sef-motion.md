# v0.66 — Samsung motion metadata

The v0.65 diagnostic contains 14 files blocked by SEF metadata checks, covering
15 distinct complete blocked name/type/payload combinations. Those payloads are
small text/zero records, not embedded media. Regression fixtures reconstruct the
complete bytes from the log (including overlapping first/last hex previews) and
check the logged SHA-256 before exercising validators and MP4 relocation.
No source videos, GPS, original filenames or complete user logs are committed.

## Modules and accepted schemas

`SamsungSefDirectory` retains structural bounds, record/directory consistency and
relative-offset checks. `SamsungSefCatalog` still checks both record name and type.
`SamsungSefMotion` separately validates:

- `Super_SlowMotion_Data`: `startMs:endMs:audioEndMs:speed[!flag]`, `*` between
  ordered non-overlapping regions. Nonnegative Samsung int values, start < end,
  audio end within the region, speed enum 1–9. Optional numeric flag is retained
  without interpreting it. Edit_Data uses the same observed text grammar.
- Known legacy type 0x0890 is accepted for Super_SlowMotion_Data and BGM, alongside
  0x0B00/0x0B01. This is name-specific, not a wildcard for unrelated records.
- Deflickering_Info: true/false; Deflickering_On: T/F.
- BGM: observed five/seven-field text layouts with theme mode 1/2, title, a relative
  `themedata/*.ogg` path, boolean, volume 0–100; optional boolean and literal null.
  External/absolute/traversing paths, binary music and other layouts stay blocked.
  This preserves a reference to Samsung's theme resources, not an embedded audio
  track. Availability remains dependent on the Gallery/theme installation.

`SamsungSefSchemas` now accepts Invalid_Data containing 1–4096 **all-zero** bytes,
covering observed lengths 13/14/26 without a length-specific exception each time.
Nonzero data and unrelated unknown record names still fail explicitly.

## Preservation and verification

No encoder parameters, timestamps or frames are changed by this patch. The SEF
box is copied unchanged at EOF; structural parsing and SHA-256 readback continue.
The conversion pipeline already checks output frame counts and original PTS with
1 ms tolerance, before publication. Playback instructions are not flattened into
frames. This validates byte/timeline preservation; Samsung Gallery interpretation
after changing AVC to HEVC still requires a device playback test.

Tests cover all 15 complete diagnostic payloads, exact SHA-256 matches, relocation
to different MP4 offsets, unchanged SEF and mdat bytes, type aliases, malformed
numbers/intervals/flags/BGM references, zero-padding bounds and mixed directories.
Only Java metadata handling changed; native encoders are unchanged.

## Evidence and limits

Android Media3 SefReader explicitly documents inconsistent numeric types and
uses names for classification:
https://github.com/androidx/media/blob/release/libraries/extractor/src/main/java/androidx/media3/extractor/mp4/SefReader.java

A public decompiled Samsung framework implementation exposes
`SEFHelper.superslowSEFParser` and the Samsung speed enum. It is implementation
evidence, **not an official published specification**:
https://github.com/488315/samsung_framework/blob/main/framework/sources/com/samsung/android/transcode/util/SEFHelper.java

BGM/deflicker layout restrictions also rely on complete observed diagnostic
payloads. They are not a claim to implement every proprietary Samsung format.
