# Test codec video — Android
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

**Limiti:** non invia fotogrammi e non produce un video. OK indica che la chiamata è riuscita; non misura qualità, velocità o bitrate risultante. CQ Android è una scala specifica del codec e non equivale al CRF di x265. Il mancato supporto del profilo di prova non implica che il codec non funzioni con altri parametri.

## APK
Apri **Actions → Build APK → ultima esecuzione riuscita → Artifacts → CodecProbe-APK**.
Scarica lo ZIP, estrai app-debug.apk e installalo sul telefono.
L'APK è una build debug firmata automaticamente, per test personali.
La chiave debug di un runner temporaneo può cambiare tra build: se un aggiornamento non si installa, disinstalla la precedente versione.

## Build locale
JDK 17, Android SDK 35, Gradle 8.11.1:
`gradle :app:assembleDebug :app:lintDebug`
Non è incluso un Gradle wrapper: la CI installa esplicitamente la versione sopra.
