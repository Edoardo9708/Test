# v0.65 — preserve FLAC APPLICATION blocks

FLAC type 2 is APPLICATION. Its body is a four-byte application identifier followed
by opaque binary data. Source files can contain multiple blocks with the same ID.
The official FLAC foreign-metadata scheme uses `riff`, `aiff` and `w64 ` to preserve
source container chunks. Such chunks may include old PCM sizes/rates, not song tags.

`AudioMetadata` stores every APPLICATION body as a repeated custom Opus comment:

    CODECPROBE_FLAC_APPLICATION=<Base64 of the original 4-byte ID + payload>

This is an application-specific archive, **not a standard Opus metadata mapping**.
No player integration or automatic restoration is claimed. Existing players may
ignore it, and later tag editors may remove unknown comments. Header values,
offsets and timing stored inside this archive are never interpreted as properties
of the new Opus audio. Preservation is safe without understanding the application
schema because these bytes are inert text comments, not appended executable or
active container structures. This is not a lossless-audio backup.

Blocks retain their order and repetitions. Base64 is reversible; no image/audio
re-encoding is involved in the archive. The normal metadata budget (32 MiB total,
100,000 entries) also includes the Base64 overhead. Oversized or malformed blocks
still fail explicitly. Logs show bounded identifiers, byte counts and the storage
method, without dumping large payloads. Final OpusTags readback checks every
expected comment byte. CUESHEET and unknown FLAC block types retain explicit
rejection. FLAC PCM CRC/MD5 checks remain unchanged.

Native synthetic fixtures cover a three-block RIFF/WAVE header, an unknown `TEST`
identifier, binary NUL/all-byte values, multi-page metadata, order/repetition,
standalone APPLICATION without song tags, malformed tags and unsupported types.
The independent Python reader decodes Base64 and compares original APPLICATION
bodies byte for byte. No user music is committed as a fixture.

Reference: https://github.com/xiph/flac/blob/1.5.0/doc/foreign_metadata_storage.md
