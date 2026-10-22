# Photo SEF archival records, v0.95

Reference: ExifTool’s primary [Samsung.pm implementation](https://github.com/exiftool/exiftool/blob/master/lib/Image/ExifTool/Samsung.pm), Trailer table and ProcessSamsung directory reader, consulted 2026-10-22. Type IDs and names describe recognition, not a guarantee that Samsung software can use the archived data after a change of container/codec. The SingeShot suffixes 5/8/10 also occur in the supplied failure log; their payloads remain opaque under the known 0x0b41 depth-map family.

The JPEG pixel dimensions are preserved. None of these payloads is used to apply edits, regenerate a portrait effect, rotate the image or construct an HEIF auxiliary image. The original complete SEF tail is kept in the existing application/vnd.codecprobe.jpeg-metadata archive and verified as a byte sequence before output publication. Standard EXIF/XMP/ICC copying is separate.

| SEF type | Exact record name |
| --- | --- |
| `0x0ce1` | `Gallery_DC_Data` |
| `0x0ba1` | `PhotoEditor_Re_Edit_Data` |
| `0x0ba1` | `Original_Path_Hash_Key` |
| `0x0ba1` | `deco_doodle_bitmap` |
| `0x0ba1` | `deco_sticker_bitmap` |
| `0x0ba1` | `deco_text_bitmap` |
| `0x0ba2` | `Copy_Available_Edit_Info` |
| `0x0b51` | `Intelligent_PhotoEditor_Data` |
| `0x0b60` | `UltraWide_PhotoEditor_Data` |
| `0x0bf0` | `Remaster_Info` |
| `0x0d91` | `PEg_Info` |
| `0x09f0` | `Pro_Mode_Info` |
| `0x08e0` | `Panorama_Shot_Info` |
| `0x0b40` | `SingleShotMeta` |
| `0x0b41` | `SingleShot_DepthMap_1` |
| `0x0b41` | `SingeShot_DepthMap_1` |
| `0x0b41` | `SingeShot_DepthMap_5` |
| `0x0b41` | `SingeShot_DepthMap_8` |
| `0x0b41` | `SingeShot_DepthMap_10` |
| `0x0ab0` | `DualShot_Meta_Info` |
| `0x0ab3` | `DualShot_Extra_Info` |
| `0x0ab4` | `DualShot_Core_Info` |
| `0x0ab1` | `DualShot_DepthMap_1` |
| `0x0ab1` | `DualShot_DepthMap_5` |

A matching type alone is insufficient: the exact record name must match too. Unknown names, unknown suffixes, Photo_HDR_Info, MotionPhoto_Data and Interactive_Panorama_000 remain rejected. The whole tail retains the 4 MiB bound, directory versions 103/105/106/107 and count limit 4096. Payload bytes may be binary, JSON, text or empty because they are archived without evaluation.

Directory order is not assumed to equal data order. Each record header/type/name length is checked at its own offset. Sorted record ranges must tile [0, directoryStart) exactly. No file offsets are rewritten, and no unindexed media data is admitted. Negative fixtures cover aliases, a reference before the trailer and an unindexed leading byte.

Local standalone validator checks use AddressSanitizer and UndefinedBehaviorSanitizer. LeakSanitizer cannot inspect process threads in the execution environment, so only that component is disabled locally. Full JPEG-to-HEIC preservation/corruption tests run separately in the native image suite.
