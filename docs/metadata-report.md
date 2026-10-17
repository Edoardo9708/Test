# Selected-file metadata report (v0.59)

The existing Diagnostics metadata export now snapshots all selected URIs and names
on the UI thread and writes one fresh TXT on its existing single worker. It is
independent of conversion and the saved conversion session. No new executor,
codec instance, decoded frames or copied video is involved. The visible carousel
item does not restrict the export. The immutable selection snapshot prevents
later selection edits from changing files/names in an export already dispatched.

Modules:
- `HomeDiagnostics`: existing permissions, progress and Android save-document flow.
- `SelectedMetadataReport`: per-file report, provider/MediaStore columns, all
  public retriever metadata keys and track-format keys. Binary codec fields have
  size, SHA-256 and a short HEX preview. No sample traversal is performed.
- `Mp4MetadataInventory`: bounded structural read-only inspection of MP4 atoms,
  independent of the stricter metadata-copy whitelist. Reports paths with sibling
  occurrence indexes, offsets/sizes, header creation/modification values (v0/v1),
  descriptive payload hashes, UTF-8/UTF-16 data fields, mdta key names and Samsung
  SEF record identities/hashes/previews. Audio/video payload atoms are skipped by
  offset. Unknown binary fields are not assigned invented meanings.

Dates in Android provider columns include units and UTC/local representations;
retriever dates retain their raw strings and include the parsed instant. Internal
header dates use the original seconds-since-1904 value and its UTC representation.
DATE_ADDED is not asserted to be the capture time. Database columns are queried
only for the exact selected/resolved item; no other media are enumerated or edited.

The report is deliberately not a claim to decode every proprietary metadata
schema. Limits are explicit: 256 provider columns, 4096 text characters per API
value, 256 bytes per atom preview, 64-byte codec-buffer previews, 4 MiB per codec
buffer hash, 32 MiB of atom payload hashing per file, 16 nested levels, 20000
inspected atoms, 512 mdta key names and the existing bounded SEF directory parser.
Provider BLOB values are identified without exporting their contents. MP4/MOV
structural errors and unavailable API sections are retained as incomplete sections
instead of deleting the whole report or hiding later files. Non-MP4 containers
can still expose Android metadata/tracks; the MP4 section reports its own failure.
In-band SEI across all frames and media samples are not traversed.

Cancellation/Activity destruction aborts the export and removes its temporary TXT.
Normal completion uses the existing version-prefixed TXT save picker and cache
cleanup. The conversion log, saved session, report and original/output media are
not overwritten. The older raw `MetadataDump` remains unchanged for its callers.

Tests exercise readable nested metadata, signed/unsigned date representations,
MDTA names, offset-independent payload hashes, binary preview limits, a sparse
64 MiB mdat skipped without media export, malformed structure, cancellation,
per-file failures and stable multi-selection export through the UI worker.
