# Test codec video — Android
## Versione 0.3: prova software x265
Nuovo pulsante per una prova CPU: stesso filmato sintetico I420 1920×1080, 30 fotogrammi a 30 fps, CRF 24 e 32, preset ultrafast / zerolatency.
La libreria x265 è compilata e inclusa nell'APK ARM64: nessun download dal telefono.
Ogni prova crea un MP4 senza audio e lo decodifica interamente per verificare 30/30 fotogrammi.
Il log include chiamate BEGIN/END, parametri effettivi, tempi, NAL, byte, timestamp, conteggi e stack trace.
Il log x265 è salvato incrementalmente in memoria privata e recuperato alla riapertura; Copia risultati permette di condividerlo.
Interrompi richiede cancellazione tra chiamate: una chiamata bloccata nativa non può essere interrotta da questo pulsante.
Esporta MP4 salva l'ultimo file disponibile tramite il selettore Android.
Il test non misura qualità percettiva o prestazioni su riprese reali. Assembly ARM disabilitato per questa prima build diagnostica; la velocità non è il massimo ottenibile con x265 ottimizzato.
La decodifica di verifica può usare l'hardware, mentre la codifica usa esclusivamente x265 sulla CPU.

### Sorgenti e licenza
Questa versione è distribuita sotto GNU GPL v2 o successiva, coerentemente con x265.
La CI include COPYING di x265 negli asset e pubblica il pacchetto di sorgenti completo, con revisione x265 fissata e script di build.
Per la build locale eseguire prima scripts/prepare-x265.sh e installare NDK 27.2.12479018 e CMake 3.22.1.

Prima app di diagnosi per il futuro convertitore video. Java, API Android native, Android 10+.

## Utilizzo
Installa l'APK, premi **Esegui test**, attendi il completamento e premi **Copia risultati** per condividere il rapporto.
Nessun accesso a video, rete o memoria condivisa; nessun permesso richiesto.

Per ogni encoder HEVC/H.265 elencato da Android:
- verifica hardware/software, limiti di risoluzione, allineamento e frequenze con input Surface;
- preferisce 1280×720 a 30 fps; se incompatibile, cerca un profilo dichiarato compatibile tra risoluzioni comuni e dimensioni allineate, entro 1920×1080 e 1–60 fps;
- legge il supporto dichiarato per VBR, CBR e CQ e il range di qualità CQ;
- per ciascuna modalità supportata crea un'istanza indipendente, configura, crea la Surface, avvia, ferma e rilascia l'encoder;
- registra OK, NON SUPPORTATO o ERRORE con fase e dettaglio.

**Limiti del test API Android:** non invia fotogrammi e non produce un video. OK indica che la chiamata è riuscita; non misura qualità, velocità o bitrate risultante. CQ Android è una scala specifica del codec e non equivale al CRF di x265. Il mancato supporto del profilo di prova non implica che il codec non funzioni con altri parametri.

## APK
Apri **Actions → Build APK → ultima esecuzione riuscita → Artifacts → CodecProbe-APK**.
Scarica lo ZIP, estrai app-debug.apk e installalo sul telefono.
L'APK è una build debug firmata automaticamente, per test personali.
La chiave debug di un runner temporaneo può cambiare tra build: se un aggiornamento non si installa, disinstalla la precedente versione.

## Build locale
JDK 17, Android SDK 35, Gradle 8.11.1:
`gradle :app:assembleDebug :app:lintDebug`
Non è incluso un Gradle wrapper: la CI installa esplicitamente la versione sopra.

## v0.5: export and controlled optimization

- Save the complete displayed log (including recovered x265 logs) as UTF-8 TXT through the Android document picker. The export freezes a snapshot before opening the picker.
- Native x265 and JNI compile as RelWithDebInfo with -O3; the APK remains debug-signed for installation. CI checks actual compiler commands and the APK ELF64 AArch64 header.
- Keep the pinned x265 version, 30 synthetic frames, CRF 24/32, two threads and assembly disabled to isolate compiler optimization. ARM assembly is a subsequent experiment.
- Log real pointer size, architecture, optimization macro, x265-only call times and total cycle time. The old upstream build_info labels non-x86 as 32 bit; it is not authoritative on ARM.
- On the SM-S916B, run the software test with the phone cool and idle, save TXT, and compare total times with v0.3. The short benchmark and historic comparison are indicative, not sustained-performance or quality measurements.

## v0.6: ARM64 assembly experiment

Enable pinned x265 ARM64/NEON routines while retaining O3, two threads and the same 30 frames/CRFs. The prepare script applies only target-flag and assembler-directive portability fixes. CI runs an ARM64 Linux build under QEMU, independently decodes its output, and checks linked Android assembly symbols. QEMU does not measure phone speed or validate Android runtime behavior. On-device logs compare with v0.5 (1333.327448 ms / 1985.072707 ms).


## v0.7: real-video conversion UI
Select a video using Android's document picker, select an output folder, and choose any integer CRF from 0 to 51 (default 23). Lower numbers generally retain more detail and produce larger files; descriptions are approximate. CRF 0 is not x265 lossless mode. Synthetic/API diagnostic buttons are removed from the main UI; diagnostic source remains for automated smoke tests.

The pipeline reads decoded YUV_420_888 planes with stride/crop handling, feeds the stateful x265 encoder on the CPU, preserves video timestamps/orientation and copies one AAC audio track. It verifies all output video frames, then creates a uniquely named MP4 in the selected folder. Originals are read-only. Failed/cancelled exports attempt to remove the partial document. The full step-by-step UTF-8 log remains exportable; the UI displays only recent entries to avoid rendering an ever-growing log.

Initial supported scope: SDR 8-bit, even dimensions up to 3840x2160 including portrait, one video and zero/one AAC track. HDR, non-YUV420 decoder output, multiple audio/video tracks and non-AAC audio produce an explicit error. Other track types are excluded with a log warning. Missing color metadata uses logged SDR defaults. Keep the app open; leaving/destroying the activity cancels the worker. Temporary MP4 storage is needed before export. No resizing, audio transcoding or HDR tone mapping yet.

Validation: host and emulated ARM64 streaming tests cover CRF 0/23/51, variable input timestamps and independent FFmpeg full-frame decode; Java plane packing tests cover crop/padding/interleaved chroma. Android build/lint and native architecture/symbol checks remain required. Android picker and real-device decoder integration need on-device verification.

## v0.8: controls and monitoring

- System-aware dark/light palette, matching status/navigation icon contrast, rounded cards and buttons.
- Encoder preset menu ultrafast through veryslow, slow default. The streaming pipeline still applies zerolatency and disables B-frames/lookahead to preserve its PTS-only muxing contract; these are not unmodified offline presets.
- Quality choices CRF 18/23/28 plus integer entry 0–51, checked before conversion.
- Application process CPU time sampled each second, normalized by available processor count (100% = all available cores); not total device utilization or CPU frequency. Battery temperature and Android thermal status are explicitly labeled, unavailable values have a fallback.
- Video seconds encoded and remaining use last emitted packet PTS, then full duration only after successful encoder flush. Unknown duration remains unknown. Overall progress reserves final steps for audio, validation and saving.
- Native CI tests exercise ultrafast, slow and veryslow with CRF 0/23/51 respectively, including independent full decoding. Phone UI and end-to-end playback still require device verification.

## v0.9: input information and CPU policies

- Replaces fixed single-line Spinner rows with wrapping selection buttons and dynamically measured dialog rows.
- Shows source size in decimal MB and exact bytes (provider query, then descriptor stat fallback), declared container MIME, video/audio codecs, resolution, duration, rotation, declared fps, color transfer/HDR and known HEVC bit depth. Missing values remain unknown; no claim of playback validation from metadata. Reads run on the worker and ignore obsolete selection results. Same summary is included in the full conversion log.
- CPU policies: Limited retains frame threads=2/pool=2; Automatic delegates frame threads and pool size to x265; Maximum assigns workers for available cores (pool cap 64) and up to six frame threads. The maximum policy is a parallelism configuration, not a guarantee of 100% CPU utilization. Automatic is the default.
- Native tests cover all three CPU policies with variable input PTS and verify that every frame decodes. Logs include requested CPU policy and effective frame thread count.

## v0.10: file compression and background conversion

- B-frame and extended lookahead switches default ON. Preset settings are used without zerolatency. With B-frames ON / extended lookahead OFF, x265 requires bframes+1 lookahead frames: adaptive B choice and CU-tree are disabled in that minimal mode. Both OFF means no B-frames or lookahead. Effective values and output PTS/DTS/slice type are logged.
- Android MediaMuxer supports MP4 B-frames since API 25; packets are delivered in encoder output order with original presentation timestamps. Every decoded output PTS is compared with the original decoded input PTS (1 ms container rounding tolerance), including VFR; failed validation prevents publication.
- Conversion runs in a foreground service, with progress/cancel notification and partial wake lock for screen-off processing. API35+ uses mediaProcessing, older supported releases use dataSync. Notification permission is requested on API33+. Service timeout cancels and reports the reason; this does not bypass Android stop/force-stop or memory/resource decisions. No automatic restart of interrupted conversions.
- Inspector sums compressed sample sizes per track without decoding; non-sample bytes are explicitly described as overhead, tags and padding. A bounded MP4 box reader measures moov and descriptive udta/meta blocks without double counting nested tags. Other formats/nonseekable providers report unsupported box measurement.
- Expandable metadata shows all retriever metadata fields and all MediaFormat keys exposed by Android, including binary field lengths. Proprietary metadata is not guaranteed readable. Descriptive tags/location/date are displayed but not automatically copied; output preserves technical video format/orientation/timestamps.
- CI tests MP4 box size accounting, all four B/lookahead combinations and actual B-picture presence, original PTS set preservation and independent full frame decoding on host and ARM64. Foreground lifecycle and Android B-frame MP4 muxing require phone testing; full app verification checks the final MP4 before saving.

## v0.11: metadata preservation and readable input sections

- Input properties are grouped into File, Video, Audio, Track sizes and Metadata/container sections. Expandable sections separate Date/GPS, Title/author, other fields and technical properties per track, with clear labels and spacing. Android-readable values are not an exhaustive decoder of vendor tags.
- MP4/MOV descriptive udta/meta/uuid/meco blocks are copied as opaque payloads at root, movie, track and media levels, preserving proprietary tags within them. Creation/modification dates in mvhd/tkhd/mdhd and track language are patched from the original. New codec/sample tables and media offsets are retained. Copied payloads are verified using SHA-256; dates and language are read back and checked.
- The rewritten moov is appended and the old moov becomes free padding, keeping all encoded media chunk offsets unchanged. This adds a second movie-table-sized overhead and can put the active moov at the end; generated files remain ordinary seekable MP4s.
- A strict source preflight rejects fragmented/non-MP4 formats, malformed/trailing vendor data and unsupported top-level blocks, rather than claiming complete metadata retention. An opaque vendor UUID/tag may itself contain private references whose interpretation is not guaranteed by raw copying. Filesystem timestamps controlled by the Android document provider are not set to the source times.
- Application metadata tracks supported by Android MediaMuxer are copied with original packet contents and PTS. Unsupported tracks abort conversion. Audio/metadata copied packets are read back and verified in addition to full video decode and original PTS checks.
- CI round trips no-tag and tagged fixtures with nested metadata, dates/languages and root UUIDs, asserts media data positions unchanged, and tests cancellation and unsupported containers. Device testing remains required for real camera metadata and vendor playback interpretation.

- Preservation preflight additionally rejects item-location metadata (iloc/idat/iref/pitm) and unrecognized UUIDs, since opaque copying could leave pointers to old media. The known Adobe XMP UUID is supported. Unsupported vendor formats need an explicit parser before the app can guarantee preservation.

- Track handler/name payloads and playback flags, layer/group/volume and transformation matrices are preserved and read back. IDs, codec descriptions, durations and media chunk indexes remain those of the new file.

## Versioned downloads

APK artifacts now start with the actual version from the built APK metadata, for example `v0.11-CodecProbe-APK.zip`. The APK inside is `v0.11-CodecProbe.apk`. The corresponding source artifact also starts with the version. App version is unchanged when only packaging changes.
