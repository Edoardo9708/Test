# Test locali di CodecProbe

**Ambito:** selezione ed esecuzione locale delle suite. Le sezioni di audit e le tabelle di misure sono evidenze storiche, non risultati della sessione corrente; versioni e disponibilità dei task vanno confrontate con i file locali. Per la documentazione dei contratti usare [testing/README.md](testing/README.md). La selezione CI è distinta e descritta in [ci-checks.md](ci-checks.md).

## Audit iniziale e confini osservati — storico

Audit del 25 ottobre 2026 sul worktree `Test-morphe`, branch `ui/morphe-foto-prototype`, HEAD `bd4d78b`. Il worktree conteneva già le modifiche della migrazione Foto 1.05: non sono state annullate. Il worktree `Test` su `main` è separato e non viene modificato.

- **Video:** `HomeController`/azioni preparano le richieste; `ConversionService` possiede coda, recupero, pausa/annullamento e lease. `VideoConverter` pubblica output attraverso SAF; HEVC/HDR e verifiche reali dipendono da Android/JNI.
- **Audio:** `AudioController`, `AudioSelection`, `AudioActions` separano pagina, letture e preparazione. Il servizio mantiene una sessione distinta; il motore condivide la lease con gli altri media. Copia pacchetti, metadati e pubblicazione hanno test dedicati.
- **Foto:** Compose invoca i coordinatori Java; `ImageActions` congela opzioni/coda. `ImageConversionService`, `ImageConverter`, `ImageOutput` separano esecuzione, staging/JNI e pubblicazione con rilettura/hash e rollback. `ImageProgressJournal` governa il riuso tramite contenuto e opzioni.
- Le politiche `OutputName`, `OutputPlan`, `SavingsPolicy`, `CompressionSummary`, `ConversionControl`, `ConversionRunGate` sono effettivamente compilabili senza Android. `ConversionFailure` richiede soltanto il `BuildConfig` Java generato, non widget/framework/JNI.
- Strumenti esistenti: JUnit 4.13.2, Robolectric 4.17, Roborazzi 1.77.0 e test Compose. Prima dell'intervento l'ultima suite completa aveva 310 esiti. Esistono inoltre programmi Java autonomi in `tests/` e smoke test C/C++/FFmpeg, separati dalla suite Gradle.
- Build osservata nell'audit locale: Gradle 9.6, AGP 9.4.1, JDK 17. I codec vengono configurati da CMake e script di preparazione; alcune build esterne usano `--parallel 2`. I test JVM non hanno bisogno di `assembleDebug`, CMake o ricompilazione dei codec.
- Non esistevano `app/src/androidTest` o un runner strumentato configurato. I test Robolectric su provider e lifecycle **non dimostrano** il comportamento di un provider SAF reale, del kernel Android o dell'hardware.

## Tre livelli

| Livello | Implementazione | Contenuto e limiti |
|---|---|---|
| A — rapido | `localFastTest`, JDK + JUnit/Hamcrest soltanto | Nomi e prenotazioni per cartella, confronto dimensioni, overflow dei totali, dettagli errori, gate pausa/skip/cancellazione e lease. Nessuna conversione, rete, risorsa Android o libreria JNI. Compila solo i sorgenti espliciti nel manifest e il BuildConfig reale. |
| B — integrazione JVM | `testDebugUnitTest`, selezionato per gruppo | Provider Robolectric con file reali piccoli, integrità/rollback, servizi, lifecycle, metadati, UI/rendering. I gruppi mirati evitano la maggior parte delle prove costose; `full` include anche rendering e prove esistenti con migliaia di record. Nessun codec reale viene dichiarato verificato. |
| B — nativo esplicito | script già esistenti, fuori dal runner JVM | `scripts/test-images-native.sh`, `test-audio-native.sh`, `test-video-decoder.sh`, smoke CMake e verifiche collegate. Richiedono toolchain/fixture/FFmpeg corretti; gli script host hanno prerequisiti Linux e non sono automaticamente portabili a Windows. Non eseguiti durante questa introduzione perché i codec non cambiano. |
| C — Android reale | non ancora implementato/configurato | Restano da introdurre e approvare prove per SAF di sistema, revoca permessi reali, process death, hardware/JNI e fluidità. Non viene aggiunto un framework solo per avere uno scheletro vuoto. Il runner locale non installa né esegue APK. |

`full` significa **tutta la suite Gradle JVM**, con A e B senza duplicazioni, non l'intero parco di smoke nativi/strumentati. I programmi Java autonomi storici e le verifiche Python CI restano comandi distinti: non vengono contati come test Gradle eseguiti.

## Comandi

Da PowerShell nella radice del worktree:

```powershell
.\scripts\test-local.ps1 fast
.\scripts\test-local.ps1 fast --group files
.\scripts\test-local.ps1 integration --group files
.\scripts\test-local.ps1 integration --group control
.\scripts\test-local.ps1 full
.\scripts\test-local.ps1 plan --files app/src/main/java/it/edo/codecprobe/OutputName.java
.\scripts\test-local.ps1 changed --files app/src/main/java/it/edo/codecprobe/OutputName.java
```

Equivalente multipiattaforma: `python3 scripts/test-local.py <suite> [opzioni]`, con JDK 17 esistente in `JAVA_HOME` o `vendor/jdk17`. PowerShell usa Python già disponibile, incluso quello del runtime Codex se presente; non installa strumenti. Il wrapper Gradle è quello del repository.

Gruppi rapidi: `files`, `savings`, `control`, `errors`. Integrazioni: gli stessi gruppi più `photo`, `audio`, `video`, `analysis`. Più `--group` combinano i gruppi. Senza gruppo, `integration` esegue tutto B. `--files` accetta percorsi relativi al repository, anche cancellati; rifiuta percorsi esterni.

Per verificare il runner, senza Gradle:

```powershell
python -m unittest discover -s tests -p test_local_tests.py
```

Su questo PC, se Python non è nel PATH, usare l'eseguibile Python del runtime Codex oppure quello già configurato: non è necessario installarne un altro.

## Selezione incrementale e cache

- Modifica piccola: selezionare manualmente un gruppo mirato. `changed --files` combina i percorsi indicati con tutte le modifiche non ancora verificate del worktree; per sorgenti di produzione non mappati sceglie tutta la suite. Soltanto le cinque foglie UI Compose Foto già revisionate restano nel gruppo Foto. I prefissi Audio/Image non dimostrano ownership: AudioCopy e JpegInventory sono condivisi. Il manifest `scripts/local-test-suites.json` contiene le politiche/test rapidi e le classi di integrazione associate.
- Controlli condivisi, lease, destinazioni comuni o fixture riusate fra sezioni ampliano alla suite JVM completa. File sconosciuti non vengono ignorati: la selezione è conservativa. Modifiche native segnalano la necessità di verifiche native separate.
- `changed` senza file usa la fotografia salvata dall'ultima `full` riuscita; prima della prima `full` usa differenze Git HEAD, compresi file non tracciati. Non aggiorna una base completa dopo una sola prova parziale. È utile in questo worktree con modifiche preesistenti non committate.
- Il runner non salta mai Gradle sulla base di una propria impronta o di vecchi JSON. Il riuso è affidato a `UP-TO-DATE`/build cache di Gradle: sorgenti/test, configurazioni Gradle e cataloghi TOML, init script, lockfile, classpath delle dipendenze ed ambiente del processo sono input dichiarati. Il digest dell’ambiente non salva in chiaro i valori: copre JDK/SDK, locale/fuso, PATH, directory temporanee, opzioni JVM/Gradle e prefissi CODECPROBE_/ROBOLECTRIC_. Gli ID transitori di Codex/launcher non sono input dei test e sono esclusi. Se un nuovo test usa un’altra variabile, dichiararla nella policy degli input Gradle.
- Anche senza diff eseguibili, `changed` chiede a Gradle di ricontrollare gli input; `plan` non esegue nulla. Nessun commit è necessario per rilevare una modifica: lo snapshot legge contenuti correnti, file non tracciati e tutti i sorgenti effettivi sotto app/src, comprese cancellazioni rispetto alla base.
- Ogni task registra separatamente numero di casi realmente eseguiti, stato skipped/noSource/failure e riuso Gradle. Un task saltato senza una valida decisione di cache, un marker mancante o un conteggio incoerente non certificano gli XML rimasti su disco.
- Gradle conserva inoltre output e cache normali. Il runner non usa `clean`, `--rerun-tasks`, rebuild o `--refresh-dependencies`. Non modifica la configurazione Gradle della distribuzione: usa un init script opt-in, senza cambiare le chiavi della cache nativa del progetto.
- Limiti predefiniti: due worker Gradle, niente parallelismo fra progetti, un fork JUnit per task, heap Gradle 1024 MiB, heap test A 256 MiB/B 512 MiB, due processori visibili al fork. Sono limiti configurati, non misure di picco RAM/CPU. Il compilatore Kotlin opera nel processo Gradle per evitare un daemon aggiuntivo.
- Gradle è offline. Robolectric risolve le immagini Android solo dal repository Maven locale (`~/.m2/repository`): se mancano, fallisce senza scaricarle. Prima esecuzione su un altro PC: predisporre esplicitamente le dipendenze già previste dal progetto; il runner non tenta installazioni automatiche.

Il controllo del grafo rifiuta task CMake/native/connected/install/uninstall prima che possano eseguire operazioni. Nessun comando di questa pagina avvia un emulatore o accede al Samsung.

## Report e diagnosi

Output normale: esito, conteggi, durata wall e tempo JUnit, percorso dei report. I log Gradle, XML e `summary.json` sono archiviati in `.tools/test-results/runs/<id>/`; `latest.json` riepiloga l'ultima selezione. Le immagini restano nel normale `app/build/reports/ui/`, senza copiarle a ogni prova.

In caso di errore vengono mostrati soltanto i primi fallimenti o gli errori di build pertinenti. Gli stack completi restano su disco. Un errore di compilazione non può essere trasformato in successo leggendo XML vecchi; zero report corrispondenti è una verifica incompleta. Prima analizzare il singolo errore, poi ampliare se il problema coinvolge un contratto condiviso.

I report in `.tools` possono essere archiviati separatamente e non decidono se saltare una prova. I vecchi file `verified/*.json`, eventualmente presenti dalle prime prove, non vengono più letti. Per una ripetizione deliberata di un task con input invariati documentare il motivo e usare il solo task Gradle pertinente con una ragione esplicita. Non svuotare cache native o fare `clean` per risolvere un errore di test.

## Primo incremento di copertura

- `OutputNamingSafetyTest`: namespace indipendenti, copia delle prenotazioni, buchi nei suffissi, locale turco, confine UTF-8, Unicode e traversal, rifiuto di un nome senza consumare la coda.
- `SavingsBoundaryTest`: dimensioni sconosciute/estreme, categorie `NO_SAVING`/`UNKNOWN_SIZE`, overflow che non inventa risparmi.
- `FailureDetailsTest`: messaggi assenti, causa originale e fallimento del rollback copiabili, nome multilinea, conservazione dell'eccezione di cancellazione.
- `ConversionControlBoundaryTest`: annullamento prima del primo file, skip tardivo e interruzione di un worker in pausa. Latch/future e join con timeout verificano condizioni reali; nessun `Thread.sleep`.
- `ImageOutputFailureTest`: 16 byte di staging e un file preesistente sentinella; sorgente assente, creazione negata, errore in scrittura, impossibilità di rilettura e rollback negato. Usa stream/provider reali nel sistema Android simulato, senza sostituire il convertitore con un mock.
- `tests/test_local_tests.py`: selezione, ampliamento dei gruppi, input esterni rifiutati, conteggi/report, cache e protezione dagli XML obsoleti.

## Ambiguità e debito preesistente

- La collisione di nome genera un suffisso, non una deduplicazione per contenuto. Il riuso Foto nel journal è un contratto diverso, già verificato dai test dedicati; restano prove reali con provider che cambiano documenti durante la pubblicazione.
- `SavingsPolicy` decide se pubblicare il convertito; non implementa universalmente «copia originale nella destinazione se più grande». Non viene inventato quel requisito nei nuovi test. Foto applica anche una propria decisione in `ImageConverter`, oltre alla politica condivisa Video/Audio.
- Non esiste un classificatore unico degli errori per tutta l'app. I nuovi test verificano categorie esistenti e dettagli diagnostici, non aggiungono arbitrariamente enum o nuove politiche di recupero.
- Nei test preesistenti sono presenti 23 occorrenze di `Thread.sleep`. Non sono state riscritte in massa: spostarle verso eventi è debito da affrontare nei moduli interessati. Esistono inoltre fixture di stress intenzionali da 5.000/8.500 record e logging da 100.000 eventi: non fanno parte di A.
- I rendering Robolectric non misurano frame time/GPU; i provider simulati non certificano SAF o permessi reali. JNI, corruzione durante conversioni effettive, spazio esaurito reale, kill del processo e compatibilità galleria richiedono B nativo/C.
- Lint preesistente ha 135 avvisi e non è incluso automaticamente in ogni prova rapida; per modifiche significative alle API/UI eseguire anche `gradlew :app:lintDebug`. Non viene dichiarato nuovamente eseguito se non lo è.

La matrice di selezione non è un grafo automatico perfetto delle dipendenze Java: quando si introduce un nuovo legame fra sezioni, aggiornare manifest/selettore e i relativi test, oppure scegliere `full`.

## Verifica iniziale, prima dell’audit finale

| Suite | Passati | Falliti | Saltati | Durata misurata |
|---|---:|---:|---:|---|
| A, tutti i gruppi | 36 | 0 | 0 | 3,863 s wall con Gradle caldo; 0,083 s JUnit |
| B, gruppo file mirato | 24 | 0 | 0 | 45,127 s wall; 22,436 s JUnit |
| B completa | 296 | 0 | 0 | 98,185 s wall; 87,542 s JUnit |
| Selezione A+B: 36 riusati + 296 eseguiti | 332 | 0 | 0 | 102,708 s totali |
| Infrastruttura Python | 13 | 0 | 0 | 0,090 s |

I 24 test mirati sono un sottoinsieme di B: non sommarli al totale. Dei 332 test app, 310 erano preesistenti e 22 sono nuovi (17 in A, 5 in B); i 13 Python sono aggiuntivi e separati. La prima A con avvio Gradle ha richiesto 22,752 s. Il riuso diretto della selezione completa è stato verificato senza avviare Gradle; inoltre, dopo una correzione del solo selettore, Gradle ha correttamente mantenuto entrambi i task test `UP-TO-DATE`, senza rieseguire i casi app. Il report storico è in `.tools/test-results/measured-full.json`: il marker A dimostra che i 36 rapidi erano UP-TO-DATE, non nuove esecuzioni in quella stessa prova. Le prove successive di riuso diretto non descrivono più il sistema attuale: la scorciatoia del runner è stata eliminata durante l’audit.

Nessun test nativo o strumentato, lint, installazione APK o accesso al Samsung è stato eseguito per questo incremento. Nessun cambiamento a sorgenti applicativi/UI, firma, applicationId, versioni o distribuzione. Le limitazioni della sezione precedente restano tali.

File introdotti: `scripts/test-local.py`, `scripts/test-local.ps1`, `scripts/local-tests.gradle`, `scripts/local-test-suites.json`, `tests/test_local_tests.py`, questo documento e le cinque classi test elencate sopra in `app/src/test/java/it/edo/codecprobe/`. L'unico altro file modificato per questo incremento è `AGENTS.md`, con l'aggiunta delle istruzioni sintetiche. Le modifiche pregresse del worktree sono separate da questo elenco.


## Audit finale prima del commit

Nuova esecuzione completa: **332 casi effettivamente eseguiti, zero fallimenti e zero saltati**, 106,650 s wall. I marker afterTest confermano 36 eseguiti in A e 296 in B; gli XML hanno 332 identità distinte senza sovrapposizioni fra A/B. Il report è `.tools/test-results/audited-full.json` e rimanda a log, XML e task-status archiviati.

Classificazione effettiva: 36 test nella JVM isolata; altri 61 test JVM di logica/parser/file e 235 con Robolectric. I file, gli stream, gli hash e la sincronizzazione Java utilizzati dai test sono reali, mentre framework Android, provider e lifecycle sono simulati. Alcuni rendering usano Skia nativo sul PC: questo non è un test dei codec multimediali JNI né dell'hardware Android. Zero prove su telefono o SAF di sistema.

Prove controllate di invalidazione, conservate in `.tools/test-results/audit-probes.json`:

- errore temporaneo in ConversionFailure.java non committato: 4 casi eseguiti, regressione rilevata;
- asserzione temporaneamente errata in FailureDetailsTest.java non committato: 4 casi eseguiti, regressione rilevata;
- ripristino byte per byte di entrambi: nuovamente esito positivo, con riuso Gradle correttamente indicato come zero nuove esecuzioni;
- catalogo TOML temporaneo non tracciato, poi modificato: ogni cambiamento ha eseguito nuovamente 4 casi;
- JAR di prova sul classpath, poi modificato allo stesso percorso: entrambi i passaggi hanno eseguito 4 casi. Nessun JAR della cache Maven è stato alterato.

Le due regressioni erano intenzionali e non restano nei sorgenti. Il TOML temporaneo è stato rimosso; JAR, init script di prova e report restano soltanto sotto `.tools`, esclusa da Git. La suite Python del selettore/report ha ora 18 test passati e include input non committati, classi condivise, lista esplicita che non può nascondere altre modifiche e ricontrollo Gradle senza diff.

### Trasferibilità verso main

Trasferibili senza sorgenti UI: i quattro file scripts/test-local.py, scripts/test-local.ps1, scripts/local-tests.gradle e scripts/local-test-suites.json; tests/test_local_tests.py; le cinque nuove classi Java in app/src/test; questo documento; la sola sezione Test locali aggiunta ad AGENTS.md. Non sostituire le altre istruzioni di main.

Verifica statica su main: le sette politiche pure usate da A sono identiche, JUnit 4.13.2 è già presente, BuildConfig è abilitato, la fixture AudioMetadataFlowTest è identica e `.tools/` è già ignorata. Non occorrono Compose, Haze, AGP 9.4 o SDK 37 per introdurre questa infrastruttura: main usa SDK 35/Robolectric 4.14.1 e mantiene i propri strumenti. Il runner non contiene più il percorso SDK 37 prima usato per la propria cache.

ImageOutput su main è più evoluto (include copyOriginal): i cinque nuovi test usano il contratto save comune, ma non coprono la funzione aggiuntiva. La trasferibilità è stata confrontata sui sorgenti/API; non è stata eseguita una build di main e nessun file del suo worktree è stato modificato. Prima di dichiarare validata l’integrazione su main occorre eseguire lì le suite, mantenendo le dipendenze e la logica di quel branch.

Il commit dell’infrastruttura non comprende app/build.gradle, build.gradle, workflow, sorgenti dell’app o le modifiche pregresse della UI sperimentale. Non comprende report, cache, APK o script di prova temporanei.

L’audit ha corretto anche una causa di riesecuzioni inutili: il primo digest includeva variabili transitorie del launcher. La policy finale dichiara esplicitamente l’ambiente pertinente e lascia a Gradle gli input di sorgenti, configurazioni e dipendenze. Un test JUnit ignorato è riportato come saltato e non come caso effettivamente eseguito.

Con la policy finale, una ripetizione completa a input invariati ha richiesto 7,712 s: entrambi i task erano UP-TO-DATE/CACHED e il contatore delle nuove esecuzioni era zero. I 18 test Python sono passati in 0,299 s. Tutti i report e le prove temporanee sono ignorati da Git; il commit contiene soltanto i 12 file sorgente/documentali dell’infrastruttura.
