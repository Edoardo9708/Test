# Samsung SEF preservation and diagnostics (v0.55)

## Responsibilities

- `SamsungSef`: coordination and combined result; does not rewrite records.
- `SamsungSefDirectory`: bounds, directory/header consistency and internal references.
- `SamsungSefCatalog`: name/type pairs, descriptions and explicit copy policy.
- `SamsungSefSchemas`: validation of supported descriptive payloads.
- `SamsungSefDiagnostics`: bounded binary/text excerpts, offsets and hashes.
- `Mp4Metadata`: copies the original complete `sefd` box to EOF and verifies the
  copied bytes with SHA-256, including a second directory/schema validation.

Recognition is deliberately separate from permission to relocate. A name in the
catalog may have a proprietary payload that remains unsupported. No record is
silently dropped and an unknown payload is not accepted solely because its
directory uses relative offsets. No editing instruction is applied to the video.

## Sources and scope

Primary implementation references inspected on 2026-10-16:

- ExifTool `Samsung.pm`, commit
  `2200871d9cef988051d2a99d67df3bda6cbb30a8`:
  https://github.com/exiftool/exiftool/blob/2200871d9cef988051d2a99d67df3bda6cbb30a8/lib/Image/ExifTool/Samsung.pm
- Neal Krawetz's original record research and follow-up by ExifTool author Phil Harvey:
  https://exiftool.org/forum/index.php?topic=16086.0
- Android Media3's SEF directory/slow-motion reader:
  https://github.com/androidx/media/blob/release/libraries/extractor/src/main/java/androidx/media3/extractor/mp4/SefReader.java

These are partial descriptions and implementations, not a complete Samsung
specification. Catalog depth-map name families include indexed variants; their
payloads are recognized for diagnosis only, not approved for copying. Versions
103, 105, 106 and 107 are recognized only when every structural and payload check
also passes. Unknown versions and malformed structures remain rejected.

## Copyable payload schemas

| Name | Type | Accepted payload |
| --- | --- | --- |
| HighlightVideo_Data | 08B0 | Existing AutoSum 10/TMDI/SMDI schema, complete fixed numeric rows |
| Samsung_Capture_Info | 0C51 | Exactly `Screen recording` |
| Image_UTC_Data | 0A01 | ASCII nonnegative Unix milliseconds, up to signed 64-bit maximum |
| MCC_Data | 0AA1 | Exactly three ASCII digits; descriptive mobile network country code, not GPS |
| Burst_Shot_Info | 09E0 | ASCII numeric identifier, 1–19 digits |
| Camera_Capture_Mode_Info | 0C61 | ASCII numeric mode identifier, 1–3 digits |
| Front_Cam_Selfie_Info | 0910 | Exactly `Front_Cam_Selfie_Info` |
| Panorama_Shot_Info | 08E0 | Exactly `PanoramaShot` |
| Jpeg360_2D_Info | A050 | Exactly `Jpeg3602D` |
| Video_Edited_UTC_Offset | 0E41 | ASCII `+HHMM`/`-HHMM`, valid minutes, at most 14:00 |

Text terminators, binary variants and unexpected type/name combinations are not
implicitly normalized. Original bytes remain unchanged. UTC/MCC fields are not
used to overwrite the existing video creation date or GPS metadata.

The 0BA1 family is recognized by name (Original_Path_Hash_Key,
PhotoEditor_Re_Edit_Data and decoration layers) but remains blocked until its
actual schema is verified. In particular, editor JSON can contain original paths,
crop dimensions, rotation and motion-video intervals. Merely recognizing JSON
does not make its contents safe or semantically correct after transcoding.

## Diagnostics

For a structurally valid block, every record is checked before deciding whether
to reject it. The report gives version, block size, directory position, total
copyable/blocked counts, and up to 64 records with name, numeric/hex type, payload
size, description and exact validation result. The first four blocked records
add record/payload start/end offsets relative to sefd, absolute offsets when read
from an MP4, payload SHA-256 and the first/last 64 bytes in hex and escaped ASCII.
PNG/JPEG/JSON detection is a signature hint, not payload approval. Truncation and
omitted record counts are explicit. Names cannot inject multiline log entries.

Structural failures report the failing directory entry and available bounds,
plus bounded header/tail bytes. No full payload/video is embedded in the log and
no extra diagnostic file or worker is created. Existing error popups, copy buttons,
TXT/ZIP exports and saved reports retain this exception message without depending
on a truncated recent-log tail. Excerpts may include original file paths or other
metadata from the source, just as the existing metadata report does.

Limits: 4 MiB per sefd block, 4096 records, 256 bytes per record name, 4096 bytes
per simple scalar payload. These limits also bound metadata-related allocations.

## Validation

Pure Java tests cover unchanged real screen-recording bytes, all additional
descriptive schemas, directory versions, unsupported 0BA1 subtypes in one block,
wrong type/name combinations, invalid scalar variants, timestamp overflow,
timezone bounds, embedded control bytes, bounded large-payload/many-record
diagnostics and 500 deterministic byte mutations. MP4 round trips verify that
the newly supported multi-record SEF stays byte-identical at its relocated EOF,
while encoded media bytes and existing dates/GPS metadata remain intact.
