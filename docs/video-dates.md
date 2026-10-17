# Video dates: separate file preservation from gallery indexing (v0.58)

A matching metadata hash cannot establish which date Samsung Gallery displays.
The existing copier verifies opaque payloads and header date values, but the
Android retriever's date was only checked when the missing-date fallback ran.

`VideoDateAudit` now snapshots the source header creation value, retriever date,
video duration and (when accessible) MediaStore DATE_TAKEN, DATE_ADDED and
DATE_MODIFIED. Original internal dates and index dates are never overwritten.
Each logged value specifies units; parsed dates include UTC, local time, offset
and device time zone. This is a bounded per-file report, not per-frame logging.

After metadata copying the temporary MP4's actual header and retriever date are
compared against the source (or the explicitly approved missing-date fallback).
The saved document is then reopened and checked again before finishFile/success.
A changed header or a different/missing parsed retriever date is a conversion
error and follows the existing cleanup path. Equivalent date representations
with different explicit offsets compare as instants. Missing/unrecognized source
dates are reported honestly, never called a verified gallery date.

Source and output DATE_TAKEN are compared independently. A difference is a warning
with signed milliseconds and seconds; it does not prove the embedded metadata
changed. Missing index rows and denied/unsupported providers are explicitly
unverified. A document is resolved only through MediaStore.getMediaUri under its
existing SAF grant. There is no broad filename lookup, polling, added executor,
manual SQL update, MediaStore mutation or arbitrary offset/duration correction.
The output index may not exist yet when the final document is checked; the log
states this limitation. Samsung's private Gallery database is not read.

The report provides evidence for the next fix; it is not a proven correction for
the reported two/three-minute discrepancy without a device reproduction. A short
new conversion of a camera video is sufficient to collect the original and output
date sections in one log. The successful conversion report remains unchanged.

Tests cover equal instants with different timezone representations; same headers
with reader dates changed by +120/-180/+3600 seconds; changed header values;
explicit fallback; missing/unrecognized dates; a separate three-minute index
mismatch; column-name lookup and unavailable/read-denied providers. No index writes.

Primary references:
- https://developer.android.com/reference/android/provider/MediaStore.MediaColumns#DATE_TAKEN
  DATE_TAKEN is a read-only indexed value in milliseconds. DATE_ADDED and
  DATE_MODIFIED use seconds; these are separate concepts.
- https://developer.android.com/reference/android/provider/MediaStore#getMediaUri(android.content.Context,%20android.net.Uri)
  Conversion to the equivalent MediaStore URI relies on the existing document grant.
