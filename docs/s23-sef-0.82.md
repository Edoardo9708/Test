# S23 JPEG SEF metadata - 0.82

The supplied S23 JPEG fails in 0.81 before encoding on `Camera_Scene_Info`. Its SEF v107 directory contains the already supported UTC/MCC records plus three short camera records.

Accept only the observed type/name/layout combinations:

| Record | SEF type | Value layout |
| --- | --- | --- |
| Camera_Scene_Info | 0x0d01 | Short decimal ASCII code, 1-16 bytes |
| Camera_Capture_Mode_Info | 0x0c61 | Short decimal ASCII code, 1-16 bytes |
| Color_Display_P3 | 0x0cc1 | 3 opaque binary bytes |

Preserve the complete original SEF byte sequence in the existing proprietary JPEG metadata archive, which is re-read and compared before publishing. The P3 marker is not itself interpreted as a colour conversion instruction. The actual ICC profile continues to be attached as a standard HEIF colour profile and checked byte for byte. These archived camera records do not imply Samsung editor feature support.

The 16-byte bound is an application limit for these opaque decimal codes, not a claimed Samsung format requirement. Their length is not fixed to the particular scene/mode value seen in one photo. Synthetic short/longer code fixtures prevent that restriction from returning.

Directory bounds, offsets, record lengths and version checks remain enforced. Unknown records, including HDR/Motion Photo payloads, are still rejected. This fixes the supplied layout, not every possible Samsung SEF variant.

Native synthetic regression fixtures cover the S23 layout, wrong record types/lengths/decimal values, truncated SEF, unknown HDR/Motion Photo records and alteration of each new archived value. No private photo or private GPS metadata is added to the repository.
