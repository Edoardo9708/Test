# HEIF thumbnails — 0.81

The inspected 0.80-style HEIC contains a 4032 × 3024 main image, a 504 × 376 JPEG thumbnail in EXIF and no HEIF `thmb` reference. The main image displays correctly in Samsung Gallery. This establishes the missing dedicated thumbnail; it does not establish which preview Samsung selected or exclude a stale Gallery cache.

For JPEGs whose longest edge exceeds 1024 pixels, encode a separate HEVC thumbnail with longest edge 1024, preserved aspect ratio (dimensions rounded down to even pixels for 4:2:0), area averaging, quality 80 and preset fast. This size is an application choice, not a mandated HEIF thumbnail size. Smaller images do not receive an enlarged thumbnail.

The thumbnail is linked to the main image with libheif's `heif_context_assign_thumbnail`. The primary image is encoded first with its existing encoder/options; a separate encoder handles the thumbnail. EXIF, XMP, ICC and the proprietary original metadata archive remain preserved. Orientation/mirroring and ICC/NCLX are applied to the thumbnail consistently with the primary image.

The pinned libheif 1.23.6 C wrapper forwards the master/thumbnail arguments in reverse order; the C++ convenience wrapper contains the corresponding reversal. `patch-libheif.py` fixes both to match the public C declaration. The output verification and missing-link regression test exercise the actual relationship after reopening the file.

Before publishing, reopen and verify the thumbnail link, dimensions, colour profiles and full pixel decoding, as well as the existing checks on the main image and metadata. Native fixtures compare decoded thumbnail pixels with independently oriented/resized source images for all eight EXIF orientations, plus portrait/odd dimensions, chroma 420/422/444, ICC, small-image boundary and a missing thumbnail link.

Existing HEICs are not modified by installing this update. Samsung Gallery behaviour requires a new conversion and on-device confirmation; local/native checks cannot prove which thumbnail its proprietary cache selects.
