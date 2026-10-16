# Automatic UHD memory protection

Version 0.54 applies a per-file override when either actual encoder dimension is
at least 3840 pixels. This covers supported UHD landscape, portrait and cropped
3840-wide files. Lower resolutions retain the user's parameters.

Both isolated encoder implementations share `memory_protection.h`. After the
selected preset and temporal/CPU settings, before encoder opening, it forces:

- two frame threads and a pool of two workers;
- four B-frames, five lookahead frames, fixed B-frame placement;
- CU-tree disabled.

CRF, preset, AQ and color/HDR metadata remain as selected. The override is a
memory-saving profile, not a hard allocation limit. Actual RAM varies by input.

The converter enables reordered packet handling even if the user disabled
B-frames. The service/dashboard expose the protection status and report the
actual encoder's B-frame/lookahead/CU-tree/AQ values. Protection is cleared at
each file boundary and recalculated on every conversion, including recovered
sessions. Queue settings, defaults and the original recovery request are not
overwritten; a later smaller video uses those choices again. The optional
checkpoint field defaults to false for sessions saved by earlier releases.
