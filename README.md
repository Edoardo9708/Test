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
