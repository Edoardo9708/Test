# CodecProbe / Test — contesto del progetto per Codex

**Aggiornato:** 25 ottobre 2026  
**Repository:** https://github.com/Edoardo9708/Test  
**Scopo:** memoria progettuale e criteri di sviluppo condivisi tra chat, Android Studio e Codex locale.  
**Stato di questo documento:** riepilogo di requisiti e decisioni discusse; **non** è una descrizione garantita del comportamento dell'ultimo commit. Prima di modificare codice, leggere le implementazioni, i test, `AGENTS.md` e la documentazione del branch in uso.

## 1. Obiettivo generale

App Android per **analizzare, convertire e gestire file multimediali** in quattro aree, mantenendo sorgenti e metadati quando possibile, con verifica del risultato e particolare attenzione all'efficienza:

- **Video:** informazioni sui video e ricodifica, principalmente verso HEVC/H.265.
- **Audio:** conversione e gestione di brani e metadati, inclusi eventuali testi/testi sincronizzati.
- **Foto:** inventario JPEG/metadata, conversione HEIC/HEIF e strumenti di verifica e gestione di grandi selezioni.
- **Analisi:** visualizzazione della struttura interna dei file (contenitori, segmenti, box, tag, dimensioni e dati); vista esadecimale solo su richiesta. Lo stato concreto di questa sezione va verificato nel branch corrente.

L'utente sviluppa anche sistemi embedded: apprezza motivazioni tecniche verificabili, consumo reale delle risorse, funzioni con responsabilità chiare e spiegazioni causali concise. Non usare analogie superflue. Non cambiare il comportamento senza segnalare i trade-off.

## 2. Repository e ambienti di lavoro

| Ruolo | Branch Git | Cartella Windows | Riferimento osservato il 25/10/2026 |
|---|---|---|---|
| Base/stabile di lavoro | `main` | `C:\Users\edoar\StudioProjects\Test` | `d41f031` |
| Prototipo nuova UI Foto/Morphe | `ui/morphe-foto-prototype` | `C:\Users\edoar\StudioProjects\Test-morphe` | `6ced0d1` |

Le cartelle sono **due Git worktree dello stesso repository**. I commit sopra sono fotografie temporali, non versioni da forzare; prima di operare consultare `git status`, `git branch --show-current`, `git log -1 --oneline` e, quando necessario, `git fetch`.

### Regole di lavoro sui branch

1. **Non modificare `main` mentre si sviluppa la nuova interfaccia**, salvo richiesta esplicita. Usare il worktree `Test-morphe` per gli esperimenti grafici.
2. **Non eseguire automaticamente** `reset --hard`, `clean -fd`, `push --force`, merge, rebase distruttivi o cancellazione di file non tracciati. Prima valutare le modifiche locali e riferire l'effetto dell'operazione.
3. Non presumere che tutti i branch condividano gli stessi file o le stesse dipendenze. Verificare cosa è installato per ogni worktree.
4. Procedere con modifiche circoscritte e verificabili; testare prima di proporre l'integrazione nel ramo stabile.
5. Commit e pubblicazione remota sono operazioni distinte dalle modifiche locali: chiarire quale operazione è stata effettivamente eseguita.
6. `vendor/` è escluso da Git. La mancanza di `vendor/x265/source` dopo clone/worktree non implica codice applicativo difettoso: controllare script di preparazione e documenti di build prima di compilare. Anche `gradle/`, `gradlew` e `gradlew.bat` possono essere file locali non tracciati nel worktree `main`; non eliminarli senza verificarne l'utilità.

## 3. Tecnologie e confini architetturali

**Nucleo dell'app:** Android, classi Java, codice nativo C/C++ via JNI; Gradle/CMake; librerie di codifica/decodifica in base al motore (es. x265, FFmpeg, libheif, Opus/FLAC). Il progetto `main` osservato usa `namespace/applicationId = it.edo.codecprobe`, `compileSdk 35`, `minSdk 29`, `targetSdk 35`, Java 17, NDK `27.2.12479018`, target `arm64-v8a` (controllare le versioni sul branch prima della build).

**Nuova interfaccia nel branch grafico:** **Kotlin + Jetpack Compose + Material 3 + Haze** sono già usati nella schermata Foto. **Non** è corretto affermare che tutta l'app sia stata migrata: i controller e il motore Java/JNI restano; parti dell'interfaccia Foto (permessi, progresso/report/diagnostica nella fase documentata) utilizzano ancora `AndroidView`; Video, Audio e Impostazioni non risultano uniformemente migrati. Riferimento autorevole: `docs/foto-compose-migration.md` su `ui/morphe-foto-prototype`.

**Separazione delle responsabilità:**

- Le schermate e i widget presentano dati e inviano azioni; non decodificano file, non fanno letture massive e non comandano direttamente il codec.
- Controller/coordinatori mantengono stato della selezione, permessi e snapshot validi; le conversioni appartengono a servizi/worker, non alla vita della Activity.
- La coda attiva usa parametri immutabili acquisiti all'avvio; modifiche dello slider non alterano una conversione in corso.
- Il codice JNI/nativo opera e verifica i dati; l'interfaccia mostra risultati/diagnostica senza millantare preservazione o compatibilità non dimostrate.
- I file del progetto sono fonte primaria per nomi di classi, API, dipendenze e stato attuale. Si vedano `docs/ui-architecture.md`, `docs/build-checks.md` e i test presenti nel branch.

## 4. Requisiti funzionali e vincoli concordati

### 4.1 Coerenza tra sezioni

La grafica va ripensata **nel suo insieme**: niente sezioni moderne accanto a schermate palesemente incoerenti una volta conclusa la migrazione. Riutilizzare davvero componenti e comportamento di pulsanti, pannelli, popup, impostazioni, caricamento, permessi, conferme, caroselli, avanzamento e report. Uniformità **non** significa inventare azioni inesistenti: ad esempio, Pausa/Riprendi deve controllare il servizio reale, non essere un comando cosmetico.

Flusso auspicato comune: scelta file/cartelle sorgente → anteprima/metadati → destinazioni e permessi → opzioni → avvio/pausa/annulla → verifica/salvataggio/report. I dettagli vanno adattati al motore e agli stati reali. Per confrontare le differenze storiche, leggere `docs/inventario-ui-video.md`.

### 4.2 Selezioni molto grandi e prestazioni

- Possibilità di selezionare cartelle con **migliaia di elementi** (caso pratico: circa **8.500 foto**).
- Il carosello non deve costruire un widget per ciascuna foto. Limite storico: mostrare al massimo le prime **100** voci e indicare chiaramente che ne esistono altre; evoluzioni verso virtualizzazione complete sono lavori separati, da valutare.
- Il report visibile usa una finestra logica massima di **100 risultati**, con elemento corrente vicino alla posizione **50** quando possibile e widget visibili riciclati. Se l'utente scorre manualmente, non riportarlo immediatamente alla riga corrente; ripristino tramite comando «Segui corrente».
- Lettura nomi, miniature e metadati asincrona, cancellabile, prioritaria per elemento corrente e viewport visibile. Limitare cache, richieste concorrenti, serializzazioni, allocazioni e aggiornamenti UI.
- **Non** ricostruire la pagina intera ogni mezzo secondo, né calcolare misure CPU/memoria per pannelli nascosti.
- Misurare gli scatti sul dispositivo prima di attribuirli a una libreria o a un singolo componente.

### 4.3 Foto

- Sorgenti JPEG incluse varianti Samsung (EXIF, XMP, ICC, SEF, Motion Photo, miniature, panorami, gain map HDR e altri dati). **Conservazione di byte originali, archiviazione opaca di un metadato e disponibilità della stessa funzione nella galleria sono tre cose diverse.** Non garantire la terza solo perché è soddisfatta la prima o la seconda.
- Output HEIC/HEIF con controllo che sia leggibile/integro secondo le verifiche implementate; sorgente originale intatta. Documentare esplicitamente ciò che viene convertito, copiato senza interpretazione, o rifiutato.
- Qualità sperimentate: **50, 55 e 60**; slider e preset devono rimanere allineati allo stato di codifica effettivo.
- Opzione «**Salva solo se più piccolo**»; opzione «**Copia l'originale se l'HEIC non è più piccolo**» dove disponibile. I risultati devono distinguere HEIC salvato, originale copiato, saltato, fallito; non contabilizzare risparmio sulla copia originale.
- Attenzione alla **deduplicazione**: se il file è già stato processato e si trova nella cartella di destinazione, non creare automaticamente un nuovo duplicato con suffissi numerici a causa di una regressione. La gestione delle collisioni deve seguire le regole correnti del piano di output e verificare identità/contenuto; non sovrascrivere accidentalmente i file.
- Report riusciti/falliti, log esportabile anche parzialmente, sessioni/coda recuperabili secondo il motore reale, permessi SAF persistenti gestiti correttamente.
- Estendere i formati (es. PNG, DNG) solo come **nuova richiesta esplicita con analisi di fattibilità**, non fingendo supporto perché il file è selezionabile.

### 4.4 Video

- Preferenza per conversione efficiente verso **HEVC/H.265**, con x265 nativo e dipendenze confezionate dove previsto, riducendo la dipendenza da capacità variabili dei dispositivi; non è però lecito promettere che ogni HDR/codec/input sia gestibile senza verifiche.
- Controlli di qualità, preset, modalità CPU, pausa/ripresa/annulla e stato effettivo del motore.
- Distinguere verifiche di formato e metadata da decodifica completa; attenzione alle differenze fra risultato del codec e riproduzione sul dispositivo.

### 4.5 Audio

- Conversione per archivi locali (fra gli esempi: FLAC e Opus VBR), con attenzione a tag, copertine e **testi locali/integrati/sincronizzati**.
- Non promettere che un lettore mostri testi solo perché un formato supporta la memorizzazione; distinguere metadati incorporati da file di testi esterni e testare compatibilità dei player.
- Selezione/cartelle, impostazioni, gestione errori e report coerenti con Video/Foto, senza sostituire l'effettiva logica dei motori.

### 4.6 Analisi

- Obiettivo: navigazione della **struttura** dei file (JPEG: marker/segmenti; MP4/HEIF: box/atomi; tag, offset, size, spiegazioni), con esadecimale caricato su richiesta.
- Il parser non deve trattenere in RAM l'intero contenuto di file grandi per mostrare una lista; interpretare soltanto i dati necessari e distinguere chiaramente campi sconosciuti, opachi o non verificati.

## 5. Direzione estetica e UX — leggere prima di qualsiasi lavoro UI

**Fonte dettagliata: [`docs/design-references/README.md`](docs/design-references/README.md)**.

- Ispirazione: **Morphe Manager/Patcher**, stile Material 3/Material You, ma **identità propria** e nessuna copia indiscriminata di codice o asset.
- Barra di navigazione inferiore compatta, flottante, con trasparenza/blur controllati; intestazione superiore in vetro sopra il contenuto; schermo leggibile su sfondo dinamico e temi chiaro/scuro.
- Superfici principali **opache** durante espansioni/riduzioni: nel prototipo si è già sperimentato che applicare Haze ai pannelli che cambiano altezza non è lo stesso approccio di Morphe e potrebbe contribuire ai problemi di fluidità. Haze riservato a superfici stabili quali titolo, Indietro e bottom bar.
- Scelte/pulsanti collegati: angoli, riempimento, contorno, testo e feedback alla pressione animati coerentemente; chiusura dei riquadri con **espansione/collasso e dissolvenza**, senza salti.
- «Indietro» sempre accessibile in sovrimpressione, con rispetto degli inset Android; anteprime foto vere, 64 dp nel prototipo, scorrimento senza frecce, long-press per selezione multipla.
- Slider e animazione di progresso ondulata coerenti con lo stile; pannelli risorse CPU/memoria e metriche I/O **da specificare in modo coerente con quanto il motore misura** (nell'interfaccia video storica esistono CPU app, PSS e temperatura batteria).
- Movimenti decorativi discreti, stoppati fuori schermata, durante conversione e con la preferenza di sistema per ridurre le animazioni. Non consumare CPU/GPU inutilmente.

### Stato del prototipo grafico documentato

Nel ramo UI la schermata Foto impiega Compose + Material 3 + Haze 1.5.2 + Coil 2.7.0, con Kotlin/Compose 2.1.20 secondo `docs/foto-compose-migration.md`. Sono descritti:

- Animazioni dei pannelli con espansione + dissolvenza, **220 ms** e `LinearOutSlowInEasing` nel prototipo.
- Barra inferiore **56 dp** nella prima prova, navigazione fra Activity ancora non equivalente al pager di Morphe.
- Miniature **64 dp** con fetch a 192 px, cache RAM 2 MiB, richieste serializzate/cancellabili secondo lo stato della pagina.
- Contenuto animato solo per un periodo limitato e sospeso durante lavorazioni; non aggiungere loop di animazione continui senza motivo.
- La prova sul telefono ha evidenziato problemi di resa/fluidità; le soluzioni non sono da dare per confermate finché misurate.

**Non confondere un requisito futuro con una funzionalità già implementata.** La bozza interattiva grafica esistente è `docs/foto-ui-reference.html` nel ramo `ui/morphe-foto-prototype`.

## 6. Robustezza, file e diagnostica

- **Mai modificare i sorgenti degli utenti** se la funzione non lo prevede espressamente. Scrivere output in modo sicuro, poi verificare/rileggere prima di dichiarare successo; rimuovere artefatti parziali secondo le regole correnti.
- Permessi Android/SAF e URI vanno gestiti senza assumere path filesystem tradizionali. Non attribuire a URI opachi una cartella che non è nota.
- Errori chiari e copiabili, dettagli tecnici accessibili, niente catch vuoti o eccezioni inghiottite; export completo e/o parziale del log quando supportato. Raggruppare errori ripetitivi.
- Sessioni recuperabili e log persistenti con scritture controllate, non aggiornamenti su disco a ogni frame/tick. Non dichiarare uno stato recuperabile se il motore non lo ha persistito.
- Prestare particolare attenzione a output già esistenti, skip coerente, permessi, annullamento, numero di file falliti/riusciti, stato durante rotazione e ricreazione della Activity.

## 7. Metodo di lavoro richiesto a Codex

1. Leggere **prima** `AGENTS.md`, questo documento, il README e i documenti relativi alla funzione da modificare. Per la grafica leggere anche `docs/design-references/README.md`, `docs/foto-compose-migration.md` e la bozza HTML se presenti.
2. Prima di toccare il codice, individuare il branch/worktree e fare un breve inventario di classi, dipendenze e flussi coinvolti. Non trasferire assunzioni da un branch all'altro.
3. Riassumere in poche righe: problema, causa riscontrata vs ipotesi, intervento minimo, test previsti. Se una decisione non è stata concordata, segnalarla come proposta.
4. Modificare componenti riusabili quando riduce realmente duplicazione/bug e non introduce lavoro inutile su SoC o migrazioni premature.
5. Fare test pertinenti (Robolectric/unit, test nativi, Gradle/build, prova su dispositivo per gesti/blur/prestazioni). Dichiarare esattamente test eseguiti e non eseguiti.
6. Evitare grandi riscritture in una singola modifica; preservare ciò che funziona, commit piccoli e branch separati; niente pubblicazioni/merge distruttivi non richiesti.
7. Report finale breve: file cambiati, comportamento cambiato, test, limiti, come verificare a mano.

## 8. Riferimenti nel repository

- [`AGENTS.md`](AGENTS.md) — regole vincolanti su efficienza, pulizia, errori, UI e animazioni.
- [`README.md`](README.md) — cronologia tecnica e dettagli/versioni, da verificare nel ramo corrente.
- [`docs/ui-architecture.md`](docs/ui-architecture.md) — confini e moduli UI.
- [`docs/inventario-ui-video.md`](docs/inventario-ui-video.md) — oggetti dell'UI Video e confronto con Audio/Foto.
- [`docs/build-checks.md`](docs/build-checks.md) — build e verifiche.
- [`docs/foto-compose-migration.md`](docs/foto-compose-migration.md) — stato e limiti della migrazione Compose nel branch grafico.
- [`docs/foto-ui-trial.md`](docs/foto-ui-trial.md) — prima prova UI e limiti di implementazione nel branch grafico.
- [`docs/foto-ui-reference.html`](docs/foto-ui-reference.html) — **bozza visiva interattiva**, presente nel branch grafico (potrebbe mancare in `main`).
- [`docs/design-references/README.md`](docs/design-references/README.md) — riferimenti e specifiche visive concordate.

Se un link relativo non esiste sul branch attivo, **non inventare il file**: aprire il riferimento nell'altro worktree oppure consultare Git prima di decidere di portarlo qui.

## 9. Provenienza e livello di certezza

- **Verificato nel repository (25/10/2026):** esistenza/linee guida di `AGENTS.md`; principali tecnologie e limiti del prototipo UI da `docs/foto-compose-migration.md`; bozza HTML e inventario UI nel branch grafico; `app/build.gradle` di `main`.
- **Richieste e decisioni discusse con l'utente:** obiettivo globale, uniformità UI, referenze Morphe, gestione file e performance, preferenze di codifica e presentazione dei dati.
- **Da ricontrollare sul codice prima di asserirlo:** stato effettivo della singola funzione/versione, test passati, compatibilità codec/metadati, completamento della sezione Analisi, prestazioni visive e funzionamento su telefono/tablet.

Questo file è una guida di continuità, non sostituisce né i sorgenti né i test.
