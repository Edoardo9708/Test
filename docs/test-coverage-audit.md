# Audit della copertura dei test esistenti

Audit statico del 25 ottobre 2026, worktree `Test-morphe`, branch `ui/morphe-foto-prototype`, HEAD `3c19c69`. Sono esaminati i file **effettivamente presenti nel worktree, comprese le modifiche non committate**, non soltanto quelli del commit. Le 40 variazioni preesistenti del worktree non sono state modificate da questa attività.

## Come interpretare questo audit

Sono state lette le implementazioni delle 94 classi JUnit, i relativi helper e fixture, i confini di produzione interessati, il runner locale e le prove autonome elencate in appendice. Ogni metodo della suite è riconciliato con i casi negli XML della precedente esecuzione completa. Un metodo con molti assert o molte iterazioni conta sempre come **un test**; due screenshot dello stesso metodo non diventano due test.

**Non sono stati eseguiti test, Gradle, build native o comandi ADB durante l'audit.** L'unico documento aggiunto al progetto è questo report; i materiali intermedi sono nell'area locale ignorata `.tools/`. Non sono stati modificati codice, test, configurazioni o dipendenze; nessun commit, push o merge.

Il report storico `.tools/test-results/audited-full.json` registra **332 casi effettivamente eseguiti**, 0 fallimenti/errori e 0 saltati, in **106,650 s wall**: 36 nel task rapido e 296 nel task JVM esteso; `gradle_reused=false` per entrambi, contatori di esecuzione 36 e 296. Questi sono risultati della prova precedente, **non una nuova certificazione dinamica di questo audit**. Il tempo JUnit registrato è 0,089 + 89,449 s, distinto dall'intera durata Gradle.

Non è stata misurata la copertura di istruzioni, righe o rami: **nessuna percentuale di code coverage è attribuita**. Il numero dei test non quantifica da solo la protezione dalle regressioni.

## Distribuzione dei 332 casi

La classificazione seguente descrive il comportamento realmente verificato e non coincide con i livelli di esecuzione A/B/C del documento `local-testing.md`.

| Categoria richiesta | Casi | Significato in questo audit |
|---|---:|---|
| A — logica pura | **88** | Calcoli, nomi, validazioni, strutture dati e gate in memoria; eventuali callback/source di prova forniscono dati al singolo algoritmo. |
| B — integrazione simulata | **201** | Coordinatori, servizi, UI o persistenza dipendenti da Activity, resolver, permessi, prefs, SQLite o scheduling Android modellati da Robolectric/stub. |
| C — integrazione reale | **43** | File, stream, parsing, serializzazione o concorrenza su IO effettivi; niente sostituzione del parser/storage sottoposto alla verifica. |
| D — nativi/codec dell'app | **0** | Nessuna esecuzione delle librerie multimediali C/C++ dell'app nei 332. |
| E — strumentati Android | **0** | Nessun dispositivo/emulatore e nessuna suite `app/src/androidTest`. |
| **Totale** | **332** | **94 classi; 332 metodi/casi distinti, senza duplicazione fra task.** |

Le categorie sono **primarie ed esclusive**, ma i meccanismi possono sovrapporsi: un test B può scrivere veri file. Per esempio, `ImageOutputFailureTest` esercita stream e rollback reali, ma il comportamento SAF determinante è fornito da un provider inventato: rimane B, con IO reale esplicitato. Per C, un `Context` usato soltanto per individuare una directory temporanea non trasforma il file in uno storage simulato. I test del journal SQLite rimangono B: passano attraverso il modello delle API Android di Robolectric, pur eseguendo SQL e producendo un database host. `GraphicsMode.NATIVE` indica rendering host/Skia e **non** l'esecuzione dei codec dell'app, né Android reale.

Un confronto numerico sotto Robolectric può essere A: per esempio i tre `AudioPcmTest` verificano campioni PCM e scelta del formato, senza un decoder. Viceversa, soltanto **36** degli 88 A appartengono al task isolato JDK+JUnit; gli altri non sono automaticamente disponibili nella suite rapida. La separazione tecnica storica è 36 isolati + 61 ulteriori casi JUnit senza runner Robolectric + 235 casi Robolectric; non va confusa con la classificazione per valore qui usata.

## Copertura per funzionalità

| Area | Copertura effettiva | Copertura parziale | Non dimostrato dai 332 |
|---|---|---|---|
| Video | Nomi/preflight, retry, errori prima codec, controllo coda, checkpoint, calcoli risparmio, parser MP4/SEF/date | Servizio eseguito con provider bloccanti o sorgenti mancanti; callback di cancellazione SDR/HDR | Un video convertito con successo, MediaCodec/MediaMuxer reali, HEVC/HDR end-to-end, audio sincronizzato nel file pubblicato |
| Audio | Naming Opus, log, bozza/default, abbinamento LRC, pubblicazione e rollback della coppia, algoritmo copia pacchetti/PCM | START osservato come Intent, stati preparati, source/sink AAC stub | FLAC/MP3/AAC→Opus locale completo, AudioVerifier/decoder Android, tag e cover realmente prodotti dal motore |
| Foto | Deduplica riferimenti, import additivo, cache/invalidation esplicita, stato Compose, pubblicazione/hash/rollback, sessione e riuso journal | Metadati JPEG sintetici e un piccolo JPEG valido, grandi code di riferimenti, servizio su fallimenti o ALREADY_DONE | JPEG→HEIC riuscito tramite JNI, qualità/pixel/orientamento output, applicazione reale onlySmaller, limite input durante conversione valida |
| File/cartelle | Logica nomi e prenotazioni, file mancanti, byte corrotti alla rilettura, errori create/write/read/delete, JSON e checkpoint | SAF tramite provider e permessi simulati; file locali reali con payload minuscoli | DocumentProvider/OEM/cloud reali, spazio esaurito a metà copia, race con altro writer, affidabilità in crash/power loss |
| Code/asincronia | Lease concorrente, gate pausa/skip/cancel, callback obsoleti, snapshot log, prosecuzione dopo errori, ripresa cursore | Lifecycle e process death rappresentati con Bundle/reset static; tempi virtuali e timeout host | Processo Android realmente ucciso, foreground restrictions, ANR, pausa/stop mentre il codec è al lavoro, stress prolungato |
| Metadati/Analisi | Parser binari reali EXIF/TIFF/MP4/SEF; offset, endian, troncamenti, export byte/Base64, date e diagnosi | Molti contenitori costruiti a mano; date galleria e permessi simulati; compatibilità Foto iniettata | Valore semantico completo su media reali, Samsung Gallery/editor, validità firme C2PA, preservazione end-to-end dopo codec |
| UI | Interazioni vere con nodi Compose Foto, conferme, selezione, alcuni vincoli geometrici/font; UI View e Intent | Screenshot generati, stato conversione/telemetria inventato, bridge Java chiamato direttamente in molti test | Confronto golden delle immagini, fluidità/scroll FPS/GPU, TalkBack, tutte densità/OEM, interazione sul Samsung |
| Condivisi | SavingsPolicy, CompressionSummary, ConversionFailure, OutputName/Plan, ConversionControl/RunGate, cache, CompactLog | Politiche invocate isolatamente, non tutti i call site; selezione/cache runner verificate anche con subprocess stub | Classificatore universale errori, correttezza automatica di ogni futuro input Gradle o dipendenza tra moduli |

Le ultime due colonne sono limiti dei test, non affermazioni che le funzionalità applicative siano guaste o assenti. Per esempio, `ImageConverter.convert` chiama davvero `ImageNative.convert`, applica un proprio confronto dimensionale, esegue hash e pubblica; i 332 non percorrono una conversione riuscita fino a quel punto. `SavingsPolicy` è chiamata da Video e Audio, ma non sostituisce la decisione autonoma di Foto. L'opzione universale «copia originale quando più grande» non è provata né va dedotta dai test della politica di risparmio.

## Dieci test ad alto valore

Questa è una scelta motivata, non una graduatoria numerica assoluta; le schede successive spiegano dati, codice, assert e limiti dei medesimi dieci casi.

1. `ImageOutputFailureTest.failedRollbackRetainsThePrimaryErrorAndItsDiagnosticCause`: protegge diagnostica primaria e originali quando fallisce anche il recupero.
2. `ImageOutputTest.sameLengthCorruptionRollsBackTheNewDocument`: distingue integrità del contenuto da semplice uguaglianza della lunghezza.
3. `ImageProgressJournalTest.reuseChecksBothFileContentsAndSettingsEvenWhenSizeDoesNotChange`: impedisce il riuso di un output cambiato o non più pertinente.
4. `OutputNamingSafetyTest.reservationsDoNotMutateTheProviderListingOrAnotherDirectory`: evita alias delle prenotazioni e collisioni fra destinazioni indipendenti.
5. `SavingsBoundaryTest.overflowingTotalsAreUnavailableRatherThanNegativeOrInvented`: protegge report e risparmi da overflow dei `long`.
6. `ConversionRunGateTest.concurrentStartsHaveOneOwnerAndOldOwnerCannotReleaseItsSuccessor`: verifica una vera competizione fra worker e lease obsolete.
7. `QueuePlaybackServiceTest.pauseSkipResumeKeepsNotificationAndExcludesSkippedFileFromRetry`: attraversa il coordinamento del servizio e la distinzione skip/fallimento.
8. `Mp4MetadataInventoryTest.unknownEditorRecordsExportEveryByteAndCompleteJson`: protegge l'export completo di payload sconosciuti, senza confonderlo con una preview.
9. `AnalysisParserTest.fileStructuresAndBoundedReads`: usa i veri parser con strutture, limiti e input malformati in più formati.
10. `PhotoComposeTest.clearUsesExistingConfirmationAndNeverDeletesOriginals`: attraversa un controllo realmente esposto nella UI Compose e una conferma distruttiva sulla selezione.

## Dieci limiti principali

1. **Nessun successo codec nei 332.** Possono passare anche se JNI non si carica, il codificatore non produce output o il risultato non è riproducibile. Le prove native separate non sono implicitamente incluse in `full`.
2. **Contratti Android simulati.** Grant persistenti, DocumentsUI, MediaStore, WakeLock, notifiche e process death non sono verificati nel sistema reale. Un Bundle con process id diverso non equivale a uccidere l'app.
3. **Screenshot senza un risultato visivo atteso.** I test render possono passare con colori/layout/contenuti sbagliati purché producano PNG; `PhotoComposeRenderTest` forza esplicitamente `roborazzi.test.record=true`.
4. **Prestazioni non misurate.** Contare 100 item, preservare identità di un oggetto e far avanzare un clock virtuale non certifica frame time, consumo RAM, CPU o fluidità del telefono.
5. **Stati e risultati spesso inseriti a mano.** SAVED, CHECKED, percentuali, frame, durata e telemetria possono essere visualizzati correttamente anche se nessun produttore reale li emette. La verifica metadati Foto del servizio usa solo file non apribili; le 8.500 compatibilità sono costruite dal test.
6. **Oracoli talvolta condivisi con la produzione.** Round-trip serializer/parser, hash atteso calcolato con lo stesso helper e nomi attesi generati con `OutputName.base` possono non rilevare uno stesso errore presente su entrambi i lati. Restano utili contro perdita/corruzione, ma non sono sempre conferme indipendenti dello standard.
7. **Failure injection limitata.** Negare subito `openFile` non equivale a errore di scrittura dopo metà stream, ENOSPC, timeout cloud o collisione tra listing e create. Il cancel in `ImageOutputTest` è già true all'ingresso.
8. **Fragilità temporale e ambientale.** Polling con `Thread.sleep`, deadline 2–15 s, font/densità/PNG-size e filesystem host; `PhotoDetailsLoaderTest` ha anche una pausa fissa di 30 ms. Un run verde non esplora tutti gli interleaving.
9. **Accoppiamento agli interni e nomi troppo ampi.** Reflection su `state`, `worker`, `notification`, `write`; `childAt`, titoli e stessa istanza View. Il test loader “at most 32” dimostra eviction, non misura esattamente il limite 32. `rejectedManualName...` respinge prima di entrare in `accept`.
10. **Copertura duplicata e percorsi parziali.** Molti test ripetono reset/default/draft/label; i bridge Java e `SelectedItemView` non equivalgono alla UI Compose installata. I test Python del runner simulano Gradle: provano gestione degli esiti, non l'invalidazione reale del grafo Gradle per ogni possibile dipendenza.

## Ridondanza, fragilità e qualità

Non ho individuato una ragione per scartare automaticamente le famiglie con verifiche simili: stesso nome/default/clear in media diversi può proteggere call site realmente diversi. Sono però **sovrapposizioni da consolidare con criterio**, non 332 rischi indipendenti: `SavingsPolicyTest`/`SavingsBoundaryTest`/`CompressionSummaryTest`; naming in `OutputPlanTest`, `OutputNamingSafetyTest`, `AudioConversionTest`, `AudioFlowTest`, `ImageOutputTest`; clear/report in `HomeResetTest`, `ImageUiTrialTest`, `PhotoComposeTest`, `ImageWorkReportTest`; riordino destinazioni in `MediaDestinationsTest`/`PhotoReferenceTest`; molti rendering chiaro/scuro senza golden. I programmi Java storici ripetono inoltre parti di naming, destinazioni e metadati, ma restano fuori dal conteggio.

Gli assert di identità hanno valore se l'obiettivo è evitare ricostruzioni inutili; diventano fragili se impediscono un'implementazione altrettanto efficiente. Lo schema SQL scritto a mano nel test di migrazione è necessario come fixture della vecchia versione: va mantenuto indipendente dallo schema nuovo. Il controllo `ExifDictionary.size()==165` e lo snapshot degli ID possono rilevare cambi involontari, ma vincolano anche estensioni legittime e non provano da soli l'aderenza allo standard.

**Giudizio:** una suite utile, con buona profondità su protezione dei file, parsing, contabilità e orchestrazione; contiene prove concrete di rollback, cancellazione e concorrenza, non soltanto mock banali. L'affidabilità è però molto disomogenea. È ragionevole usarla per individuare regressioni nei contratti descritti; non è una certificazione della conversione multimediale, della preservazione funzionale Samsung o della fluidità. La presenza di un nucleo di 36 test isolati è un buon punto di partenza, ma il numero totale è dominato dal runtime simulato e include prove render con oracoli deboli e stress di grandi modelli.

### Cinque priorità di miglioramento — soltanto proposte

1. Eseguire separatamente, quando cambia il motore, pochi smoke nativi già esistenti con file piccoli e verifica indipendente dell'output; rendere esplicito nel report cosa è JVM e cosa è codec. Non partire da migliaia di conversioni.
2. Aggiungere in futuro una sottile verifica autorizzata su Android reale: pubblicazione SAF con grant/revoca e successo end-to-end per media, preservando fonti e applicando policy di risparmio. Nessuna installazione autorizzata da questo audit.
3. Rafforzare gli oracoli dei casi deboli: limite esatto cache32, controllo positivo accanto ai comandi obsoleti, metadati validi accanto ai fallimenti, valori dell'output verificati con strumenti/fixture indipendenti.
4. Per la UI, mantenere le interazioni Compose significative, introdurre pochi confronti visivi deterministici e misurazioni mirate sul dispositivo; separare catture per revisione umana dalle regressioni automatiche.
5. Eliminare gradualmente sleep arbitrari/reflection non necessari nei test toccati e alleggerire ripetizioni/stress nella suite quotidiana, conservando stress e native come esecuzioni esplicite. Nessun refactoring esteso o nuova dipendenza è stato effettuato qui.



## Riepilogo numerico per area

L’assegnazione a un’area è primaria: per esempio una prova file Foto compare in File e cartelle e non viene ricontata in Foto.

| Area | A | B | C | Totale |
|---|---:|---:|---:|---:|
| Video | 6 | 18 | 1 | 25 |
| Audio | 13 | 21 | 1 | 35 |
| Foto | 1 | 32 | 2 | 35 |
| File e cartelle | 15 | 37 | 0 | 52 |
| Code e operazioni asincrone | 9 | 23 | 7 | 39 |
| Metadati | 13 | 15 | 30 | 58 |
| Interfaccia grafica | 2 | 49 | 0 | 51 |
| Componenti condivisi | 29 | 6 | 2 | 37 |
| **Totale** | **88** | **201** | **43** | **332** |

Nei sorgenti JUnit esaminati: **23 occorrenze di `Thread.sleep`** in 18 classi; reflection esplicita su campi/metodi privati in 29 classi. Questi sono indicatori statici, non numero di test falliti o misure di flakiness.

Classi con sleep: `AnalysisWorkspaceTest`, `AudioDestinationFlowTest`, `AudioFlowTest`, `FolderPickerActivityTest`, `ImageCarouselCacheTest`, `ImageDestinationFlowTest`, `ImageFolderImportTest`, `ImageLargeQueueTest`, `ImageLiveLogTest`, `ImageMetadataCheckTest`, `ImagePauseTest`, `ImageResponsivenessTest`, `ImageUiTrialTest`, `ImageWorkspaceTest`, `MediaDestinationsRenderTest`, `MediaDestinationsTest`, `PhotoDetailsLoaderTest`, `QueuePlaybackServiceTest`.

## Dieci esempi spiegati nel dettaglio

### 1. Fallisce anche il rollback

`ImageOutputFailureTest.failedRollbackRetainsThePrimaryErrorAndItsDiagnosticCause` — B.

1. Prepara staging pseudo-HEIC di 16 byte e un file precedente di 4 byte, con nome `Photo.heic`. Il provider di prova nega sia l'apertura in scrittura del nuovo documento sia la sua eliminazione.
2. Chiama il vero `ImageOutput.save`, attraverso il resolver Robolectric e il provider `Documents` specializzato del test.
3. Richiede una `IOException` primaria contenente `write denied`, una sola eccezione suppressed contenente `rollback denied`, due documenti residui nel provider e originali byte-identici.
4. Intercetta una regressione che nasconde l'errore iniziale, cancella il file precedente o dichiara un successo dopo una scrittura fallita.
5. Non dimostra che il provider Samsung autorizzi/nega le operazioni allo stesso modo, né prova scrittura parziale, ENOSPC o recupero successivo del documento orfano. L'orfano è precisamente il risultato atteso quando la cancellazione fallisce.

### 2. Output corrotto ma della stessa lunghezza

`ImageOutputTest.sameLengthCorruptionRollsBackTheNewDocument` — B.

1. Prepara 16 byte con header `ftyp/heic`. Il provider modifica il primo byte quando il risultato viene riaperto in lettura, mantenendo la lunghezza.
2. Chiama `ImageOutput.save`; copia e verifica del contenuto appartengono al codice di produzione.
3. Si aspetta `IOException` e nessun documento/nome rimasto nel provider.
4. Intercetta una pubblicazione che controlla solo la dimensione o ignora un hash diverso alla rilettura.
5. Non rileva un HEIC non decodificabile se i byte sono copiati fedelmente: il file di prova non contiene affatto un'immagine HEIC completa.

### 3. Riuso delle foto basato sul contenuto

`ImageProgressJournalTest.reuseChecksBothFileContentsAndSettingsEvenWhenSizeDoesNotChange` — B, con IO e hash reali.

1. Crea sorgente e output di 4 byte, registra nel journal un SAVED con hash reali e qualità 65. Non crea una conversione.
2. Chiama `ImageProgressJournal.record/reusable`, che legge i file tramite il resolver `file://` e calcola l'impronta con `ImageFingerprint`.
3. All'inizio ottiene ALREADY_DONE. Dopo una modifica a lunghezza invariata della sorgente o dell'output, oppure qualità 70/output eliminato, pretende `null`; cancellazione immediata deve lanciare `InterruptedIOException`.
4. Intercetta riuso basato solo su nome/lunghezza e mancata invalidazione quando cambiano i parametri controllati.
5. Non prova validità HEIC, tutte le opzioni possibili, provider remoto o modifiche concorrenti dopo il controllo ma prima dell'uso.

### 4. Prenotazioni indipendenti per destinazione

`OutputNamingSafetyTest.reservationsDoNotMutateTheProviderListingOrAnotherDirectory` — A.

1. Prepara un insieme di nomi esistenti e un `OutputPlan` con destinazioni differenti, tutto in memoria.
2. Chiama le operazioni directory/conflitto/prenotazione del vero `OutputPlan`, con la politica `OutputName`.
3. Verifica che la copia del listing non sia aliasata al Set del chiamante e che una prenotazione non contamini l'altra directory; lo svuotamento del Set esterno non cancella la conoscenza dei nomi già caricati.
4. Intercetta una regressione in cui modificare il Set esterno libera un nome occupato, o una cartella blocca inutilmente un'altra.
5. Non esegue il listing di un provider, non crea file e non protegge da altri programmi che creano il nome dopo il preflight.

### 5. Nessun risparmio inventato quando una somma va in overflow

`SavingsBoundaryTest.overflowingTotalsAreUnavailableRatherThanNegativeOrInvented` — A.

1. Prepara due risultati COMPLETE: input `Long.MAX_VALUE` e 1, output 1 e 1. Sommare gli input supera il massimo intero `long`.
2. Chiama `CompressionSummary.render`, poi esamina la sezione del totale; controlla anche una lista vuota.
3. Il totale deve dire «Confronto dimensioni non disponibile», senza «Spazio risparmiato»; la lista vuota produce stringa vuota.
4. Intercetta overflow silenzioso che mostra un totale negativo o un falso enorme risparmio.
5. Non dimostra che dimensioni reali vengano lette correttamente, che un convertitore applichi la policy, né ogni possibile combinazione di overflow fra input e output.

### 6. Due sessioni non devono possedere insieme il motore

`ConversionRunGateTest.concurrentStartsHaveOneOwnerAndOldOwnerCannotReleaseItsSuccessor` — A, con thread reali.

1. Crea una gate e 8 worker; latch coordinano partenza, tentativi e rilascio del vincitore.
2. Tutti invocano `ConversionRunGate.acquire`; il proprietario resta in attesa mentre gli altri tentano. Poi si acquisiscono lease successive e si tenta di rilasciarle con un vecchio token.
3. Esattamente un worker entra; un'ulteriore acquire fallisce mentre occupata; i token crescono e il vecchio release non toglie ownership al nuovo.
4. Intercetta race di acquisizione e callback vecchie che liberano una sessione nuova.
5. Non avvia servizi o JNI, non prova esclusione fra processi e non esaurisce gli interleaving possibili con una sola esecuzione; i timeout di 3 s dipendono dal carico host.

### 7. Pausa, salta e riprendi nel servizio Video

`QueuePlaybackServiceTest.pauseSkipResumeKeepsNotificationAndExcludesSkippedFileFromRetry` — B.

1. Crea una coda di due URI fittizi e un provider directory bloccato da latch. Nessuna sorgente video valida è disponibile.
2. Invoca `ConversionService.onStartCommand` per START, PAUSE, SKIP e RESUME; il vero worker percorre il ciclo e la gate. Il test apre anche la notifica privata via reflection.
3. Un token errato non mette in pausa; quello giusto rilascia WakeLock shadow e offre Riprendi. Skip accetta solo l'elemento corrente, conta uno skipped senza fallimento/retry. Dopo resume il secondo file fallisce e diventa l'unico retry.
4. Intercetta comandi obsoleti, avanzamento errato della coda e confusione tra skip volontario e fallimento.
5. Non prova la pausa dentro un encoder, l'effettivo risparmio energetico del telefono o una conversione successiva riuscita.

### 8. Metadati sconosciuti esportati integralmente

`Mp4MetadataInventoryTest.unknownEditorRecordsExportEveryByteAndCompleteJson` — C.

1. Costruisce un box SEF con JSON lungo contenente caratteri accentati/escape e marcatore finale, più 2.501 byte binari pseudocasuali a seed fisso 8241. Aggiunge un `mdat` sentinella.
2. Scrive un file temporaneo e chiama il vero writer/parser `Mp4MetadataInventory`, usando i costruttori binari helper.
3. Decodifica il SEF esportato e lo confronta byte per byte, controlla JSON completo/footer e assenza del contenuto `mdat`; confronta anche l'hash, calcolato con lo stesso helper produttivo.
4. Intercetta troncamento alla preview, perdita di dati ignoti, export accidentale dei pixel/video e testo JSON tagliato.
5. Non dimostra conformità dello schema proprietario, riapertura nell'editor Samsung o preservazione dopo una conversione; il confronto dell'hash non è un oracolo indipendente dell'algoritmo, mentre il confronto dei byte lo è per il contenuto.

### 9. Parser su file e confini di lettura

`AnalysisParserTest.fileStructuresAndBoundedReads` — C.

1. Delega a `FileAnalysisChecks.run`, che prepara file binari JPEG/HEIC/FLAC/MP3, input troncati, IFD con ciclo e un file sparse di circa 5 GiB. La dimensione logica non implica lettura/allocazione di 5 GiB.
2. Esegue i veri `AnalysisReader`, `AnalysisParser` e parser EXIF attraverso canali e file temporanei, senza mock del parser.
3. Verifica intervalli/nodi e limiti, trattamento di `mdat` opaco, marker JPEG, miniature, errore su strutture invalide e risposta all'interruzione del thread.
4. Intercetta offset fuori file, overflow/troncamenti e parsing che interpreta payload opachi o si blocca su un ciclo.
5. Non decodifica alcun media, non è un fuzzer esaustivo e aggrega molti scenari in un caso: il primo fallimento impedisce di conoscere subito lo stato dei successivi formati.

### 10. Clear dalla UI Compose realmente installata

`PhotoComposeTest.clearUsesExistingConfirmationAndNeverDeletesOriginals` — B.

1. Crea due JPEG sintetici e inserisce i loro riferimenti nello stato Foto; la rule installa `ImageActivity` in Robolectric.
2. Usa i veri nodi Compose `photo-new` e `photo-clear` con `performClick`; risponde al dialog Android tramite `ShadowAlertDialog`.
3. Prima del sì rimangono due input; No li conserva; Sì svuota la selezione e tutti i file originali esistono ancora.
4. Intercetta un bottone non collegato, clear senza conferma, No distruttivo o eliminazione fisica delle fonti.
5. Non confronta i byte delle fonti, non seleziona attraverso DocumentsUI e non prova la stessa interazione su dispositivo. Un file modificato ma ancora esistente non sarebbe rilevato da questo metodo.


## Inventario completo dei 332 metodi

Ogni riga rappresenta un metodo JUnit esistente. Per ciascuna classe sono esplicitati componenti produttivi, preparazione/file/simulazioni e limiti comuni: **si applicano a tutte le righe pertinenti della classe**, insieme alle precisazioni specifiche della riga. Le chiamate elencate sono riferimenti leggibili estratti dai corpi già esaminati; non sono una traccia di esecuzione dinamica né un grafo esaustivo. I metodi che delegano a helper sono da leggere con la descrizione dell’helper. Nessuna riga di questa sezione è D o E.

Origine: **310 casi preesistenti + 22 introdotti nel commit di infrastruttura**, in cinque classi: OutputNamingSafetyTest (7), SavingsBoundaryTest (3), FailureDetailsTest (4), ConversionControlBoundaryTest (3), ImageOutputFailureTest (5). Sono inclusi anche i test preesistenti modificati nel worktree UI; non è stato modificato alcun test durante l’audit.


### Video


#### ConverterControlTest — 3 test

- **Sorgente:** [ConverterControlTest.java](../app/src/test/java/it/edo/codecprobe/ConverterControlTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** VideoConverter.isCancelled/convert; HdrTranscoder.isCancelled; ConversionControl; ConversionCleanup.temporaryFiles.
- **Riferimenti produttivi:** [VideoConverter](../app/src/main/java/it/edo/codecprobe/VideoConverter.java), [HdrTranscoder](../app/src/main/java/it/edo/codecprobe/HdrTranscoder.java), [ConversionControl](../app/src/main/java/it/edo/codecprobe/ConversionControl.java), [ConversionCleanup](../app/src/main/java/it/edo/codecprobe/ConversionCleanup.java).
- **Dati, file e simulazioni:** Listener no-op; HdrTranscoder costruito ma non avviato; latch/executor; working MP4 tre byte e directory temporanea non vuota.
- **Non verificato / fragilità:** Non esegue SDR/HDR decode/encode. Il primo verifica callback comune, il secondo solo early abort; terzo IO reale dipende da semantica cancellazione directory non vuota.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`sdrAndHdrCancellationCallbacksUseTheSamePauseGate`](../app/src/test/java/it/edo/codecprobe/ConverterControlTest.java#L23) | B | Callback HdrTranscoder entra nella gate di pausa del VideoConverter e si sblocca con skip, senza transcoding. | `gate.beginFile`, `gate.pause`, `gate.skip`, `converter.isCancelled`, `gate.cancel` |
| [`skipBeforeOpeningInputDeletesWorkingFileAndNeverCreatesOutput`](../app/src/test/java/it/edo/codecprobe/ConverterControlTest.java#L35) | B | Skip prima apertura URI inesistente cancella file working e non cancella l'intera coda. | `gate.beginFile`, `gate.skip`, `converter.convert` |
| [`cleanupReportsAnUndeletableTemporaryAndStillDeletesOtherFiles`](../app/src/test/java/it/edo/codecprobe/ConverterControlTest.java#L46) | C | Cleanup fallisce su directory non vuota ma cancella altro file e conserva diagnosi. | `ConversionCleanup.temporaryFiles` |


#### FailedQueueServiceTest — 1 test

- **Sorgente:** [FailedQueueServiceTest.java](../app/src/test/java/it/edo/codecprobe/FailedQueueServiceTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ConversionService.onStartCommand/worker; ConversionFailure; BatchRetry; batch log.
- **Riferimenti produttivi:** [ConversionService](../app/src/main/java/it/edo/codecprobe/ConversionService.java), [ConversionFailure](../app/src/main/java/it/edo/codecprobe/ConversionFailure.java), [BatchRetry](../app/src/main/java/it/edo/codecprobe/BatchRetry.java).
- **Dati, file e simulazioni:** START reale del servizio Robolectric con URI di cartella senza provider e primo elemento omitted; executor vero, file log vero.
- **Non verificato / fragilità:** Fallisce intenzionalmente prima dei codec; non prova classificazione errore decoder né successo. Accesso worker/campi State via reflection.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`actualFailureIsPersistedButExplicitlyIgnoredVideoIsExcluded`](../app/src/test/java/it/edo/codecprobe/FailedQueueServiceTest.java#L19) | B | Il ciclo batch ignora zero, registra fallimento IOException di one nel log, conta 1 ignored/1 failed e persiste retry allineato. | `ConversionService.snapshot`, `ConversionService.retryRequest`, `BatchRetry.select`, `service.onStartCommand`, `ConversionService.class.getDeclaredField`, `state.failures.size`, `state.failures.get`, `ConversionService.batchLogFile`, `BatchRetry.load`, `ConversionService.State.class.getDeclaredFields`, `ConversionService.rememberFailures` |


#### HomeLifecycleTest — 4 test

- **Sorgente:** [HomeLifecycleTest.java](../app/src/test/java/it/edo/codecprobe/HomeLifecycleTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeController/State/Selection/System; HomeDiagnostics; HomeConversion.
- **Riferimenti produttivi:** [HomeController](../app/src/main/java/it/edo/codecprobe/HomeController.java), [HomeDiagnostics](../app/src/main/java/it/edo/codecprobe/HomeDiagnostics.java), [HomeConversion](../app/src/main/java/it/edo/codecprobe/HomeConversion.java).
- **Dati, file e simulazioni:** Bundle/cache metadati e ConversionService.State artificiali; Activity e Intent Robolectric, file snapshot/sentinella reali.
- **Non verificato / fragilità:** Recreate/saveInstanceState non è kill del processo. Nessun metadata extractor o codec; lifecycle reale Android e lavoro foreground non provati.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`recreationKeepsParametersDetailsAndPendingFolderRequest`](../app/src/test/java/it/edo/codecprobe/HomeLifecycleTest.java#L30) | B | Ricrea parametri, dettagli, card e picker origine pendente; controller vecchio destroyed ed executor chiusi. | `home.compression.selectedCrf`, `home.screen.compression.presetSlider.getProgress`, `home.screen.compression.cpuMode.getSelectedItemPosition`, `home.screen.compression.bframeSwitch.isChecked`, `home.screen.compression.lookaheadSwitch.isChecked`, `home.screen.files.videoDetails.getVisibility`, `home.screen.sections.bodies.get`, `VideoMetadataCache.clear` |
| [`pickVideosAndOpenSettingsReachTheCorrectAndroidActions`](../app/src/test/java/it/edo/codecprobe/HomeLifecycleTest.java#L51) | B | Picker video emette OPEN_DOCUMENT video/* multiplo e impostazioni ricevono CRF18. | `home.screen.files.pickVideo.performClick`, `home.screen.compression.crfSlider.setProgress`, `home.screen.preferences.performClick`, `SettingsActivity.class.getName` |
| [`cancellingTxtPickerDeletesOnlyItsTemporarySnapshot`](../app/src/test/java/it/edo/codecprobe/HomeLifecycleTest.java#L61) | B | Cancel export elimina solo snapshot, non un altro file temporaneo. | Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder |
| [`reopeningAnActiveSessionShowsConversionAndClosingDoesNotCancelIt`](../app/src/test/java/it/edo/codecprobe/HomeLifecycleTest.java#L69) | B | Run iniettata all'apertura mostra conversione22%; chiusura Activity non cambia running del modello. | `ConversionService.class.getDeclaredField`, `ConversionService.State`, `home.screen.sections.bodies.get`, `home.screen.conversion.percentLabel.getText`, `home.screen.compression.crfSlider.isEnabled`, `home.screen.conversion.stop.isEnabled`, `ConversionService.snapshot`, `ConversionService.State.class.getDeclaredFields`, `VideoMetadataCache.clear` |


#### HomeResetTest — 3 test

- **Sorgente:** [HomeResetTest.java](../app/src/test/java/it/edo/codecprobe/HomeResetTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeReset.clear/update; ConversionService.clearSessionIfIdle; VideoMetadataCache; BatchRetry.
- **Riferimenti produttivi:** [HomeReset](../app/src/main/java/it/edo/codecprobe/HomeReset.java), [ConversionService](../app/src/main/java/it/edo/codecprobe/ConversionService.java), [VideoMetadataCache](../app/src/main/java/it/edo/codecprobe/VideoMetadataCache.java), [BatchRetry](../app/src/main/java/it/edo/codecprobe/BatchRetry.java).
- **Dati, file e simulazioni:** Activity Robolectric, stato/report iniettati; due veri file sorgente/output e log sentinella; cache metadati costruita.
- **Non verificato / fragilità:** File multimediali sono 3/2 byte. Nessun reset durante lavoro reale/kill, molte asserzioni dipendono dalle view interne.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`clearRemovesSelectionAndReportWithoutDeletingMediaSettingsOrLogAndSurvivesRecreation`](../app/src/test/java/it/edo/codecprobe/HomeResetTest.java#L32) | B | Clear svuota selezione/report/cache/retry e sopravvive recreate, preservando byte sorgente/output/log e slider/prefs. | `ConversionService.batchLogFile`, `log.exists`, `log.toPath`, `ConversionService.State`, `CompressionSummary.render`, `CompressionSummary.Item`, `ConversionService.rememberFailures`, `BatchRetry.select`, `VideoMetadataCache.permissions`, `VideoMetadataCache.clear`, `VideoMetadataCache.Entry`, `InputInspector.Report`, `VideoMetadataCache.put`, `VideoMetadataCache.epoch`, `saved.putString`, `saved.putStringArrayList`, `home.conversion.renderSession`, `home.screen.compression.crfSlider.setProgress`; altri passaggi nel sorgente collegato |
| [`activeOrPausedQueueCannotBeClearedEvenByCallingTheActionDirectly`](../app/src/test/java/it/edo/codecprobe/HomeResetTest.java#L80) | B | Stato running+paused impedisce clear anche chiamando direttamente action. | `ConversionService.State`, `home.conversion.setBusy`, `home.screen.files.clearSelection.isEnabled`, `home.reset.clear`, `ConversionService.clearSessionIfIdle`, `ConversionService.snapshot` |
| [`metadataReadCannotBeClearedUntilItFinishes`](../app/src/test/java/it/edo/codecprobe/HomeResetTest.java#L88) | B | metadataLoading iniettato impedisce Clear fino a quando il flag termina; nessuna lettura effettiva. | `home.state.selectedInputs.add`, `home.reset.update`, `home.screen.files.clearSelection.isEnabled`, `home.reset.clear`, `home.state.selectedInputs.size` |


#### HomeSavingsRetryTest — 2 test

- **Sorgente:** [HomeSavingsRetryTest.java](../app/src/test/java/it/edo/codecprobe/HomeSavingsRetryTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeConversion; HomeRetry; HomeOutputCheck; BatchRetry.
- **Riferimenti produttivi:** [HomeConversion](../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [HomeRetry](../app/src/main/java/it/edo/codecprobe/HomeRetry.java), [HomeOutputCheck](../app/src/main/java/it/edo/codecprobe/HomeOutputCheck.java), [BatchRetry](../app/src/main/java/it/edo/codecprobe/BatchRetry.java).
- **Dati, file e simulazioni:** Provider directory sempre vuota e permessi shadow; richiesta fallimenti persista a mano, worker reale.
- **Non verificato / fragilità:** Nessuna collisione effettiva o conversione; solo Intent e stato checkbox. Timeout executor 5 s.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`savingsCheckboxIsRememberedAndFrozenDuringConversion`](../app/src/test/java/it/edo/codecprobe/HomeSavingsRetryTest.java#L31) | B | onlySmaller ricordato fra Activity e disabilitato durante busy artificiale. | `home.screen.conversion.onlySmaller.isChecked`, `home.screen.conversion.onlySmaller.setChecked`, `home.conversion.setBusy`, `home.screen.conversion.onlySmaller.isEnabled` |
| [`retryUsesOnlySavedFailuresAndRunsFilenamePreflightAgain`](../app/src/test/java/it/edo/codecprobe/HomeSavingsRetryTest.java#L41) | B | Retry confermato ripassa preflight e lancia solo file fallito con cartella/nome/CRF/preset originali. | `ConversionService.rememberFailures`, `BatchRetry.select`, `home.conversion.setBusy`, `home.screen.conversion.retryFailed.getVisibility`, `home.screen.conversion.retryFailed.getText`, `home.screen.conversion.retryFailed.performClick`, `home.conversion.retry.confirmation.getButton`, `home.conversion.outputCheck.worker.submit` |


#### MemoryProtectionTest — 2 test

- **Sorgente:** [MemoryProtectionTest.java](../app/src/test/java/it/edo/codecprobe/MemoryProtectionTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** MemoryProtection.required; SavedSession; ConversionSessionState.resetFile/begin; HomeConversion dashboard.
- **Riferimenti produttivi:** [MemoryProtection](../app/src/main/java/it/edo/codecprobe/MemoryProtection.java), [SavedSession](../app/src/main/java/it/edo/codecprobe/SavedSession.java), [ConversionSessionState](../app/src/main/java/it/edo/codecprobe/ConversionSessionState.java), [HomeConversion](../app/src/main/java/it/edo/codecprobe/HomeConversion.java).
- **Dati, file e simulazioni:** Dimensioni numeriche 4K/FHD/QHD, State e opzioni manuali, journal reale e UI simulata.
- **Non verificato / fragilità:** Non alloca UHD né misura memoria/OOM e non dimostra restrizioni applicate nel codec. 'Resume' è begin diretto, non servizio in lavoro.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`detectsUhdOrientationAndCropWithoutChangingSmallerVideos`](../app/src/test/java/it/edo/codecprobe/MemoryProtectionTest.java#L14) | A | Soglia riconosce3840x2160 e crop/orientamento, non1920/2560. | `MemoryProtection.required` |
| [`effectiveLabelSurvivesCheckpointThenResetsForNextFileAndResume`](../app/src/test/java/it/edo/codecprobe/MemoryProtectionTest.java#L21) | B | Flag protezione sopravvive checkpoint e label Limited, reset prossimo file/ripresa lo azzera senza perdere CPU richiesta/URI/elapsed. | `SavedSession.save`, `SavedSession.load`, `saved.resumeRequest`, `home.conversion.renderConversionDashboard`, `home.screen.conversion.activeCpu.getText`, `home.screen.conversion.activeBframes.getText`, `home.screen.conversion.activeLookahead.getText`, `home.screen.conversion.activeTree.getText`, `ConversionService.State`, `ConversionSessionState.resetFile`, `ConversionSessionState.begin`, `SavedSession.clear` |


#### VideoCarouselTest — 2 test

- **Sorgente:** [VideoCarouselTest.java](../app/src/test/java/it/edo/codecprobe/VideoCarouselTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeSelection; VideoMetadataCache; DestinationPlan; HomeScreen.
- **Riferimenti produttivi:** [HomeSelection](../app/src/main/java/it/edo/codecprobe/HomeSelection.java), [VideoMetadataCache](../app/src/main/java/it/edo/codecprobe/VideoMetadataCache.java), [DestinationPlan](../app/src/main/java/it/edo/codecprobe/DestinationPlan.java), [HomeScreen](../app/src/main/java/it/edo/codecprobe/HomeScreen.java).
- **Dati, file e simulazioni:** Due cache Entry preinserite, Bundle URI fittizi; HorizontalScrollView/click Robolectric, virtual idle1s.
- **Non verificato / fragilità:** Nessun video letto, la promessa 'doesNotReadAgain' dedotta da loading=false/cache non contatore extractor. Nessun jank.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`scrollingDoesNotSelectAndCachedSelectionDoesNotReadAgain`](../app/src/test/java/it/edo/codecprobe/VideoCarouselTest.java#L25) | B | scrollTo non seleziona; click secondo usa sintesi cache senza loading/dialog. | `VideoMetadataCache.permissions`, `VideoMetadataCache.clear`, `VideoMetadataCache.epoch`, `VideoMetadataCache.put`, `VideoMetadataCache.Entry`, `InputInspector.Report`, `state.putStringArrayList`, `state.putString` |
| [`removingSelectedVideoKeepsTheRemainingFileAndItsDestination`](../app/src/test/java/it/edo/codecprobe/VideoCarouselTest.java#L45) | B | Rimuovere secondo mantiene primo e relativa cartella, busy impedisce rimozione. | `VideoMetadataCache.permissions`, `VideoMetadataCache.clear`, `VideoMetadataCache.epoch`, `VideoMetadataCache.put`, `VideoMetadataCache.Entry`, `InputInspector.Report`, `saved.putStringArrayList`, `saved.putString`, `home.state.destinationPlan.setFolder`, `home.screen.files.videoChips.get`, `home.screen.files.remove.performClick`, `home.state.destinationPlan.effective`, `home.state.input.toString`, `home.screen.files.videoChips.size`, `home.selection.removeVideo`, `home.state.selectedInputs.size` |


#### VideoMetadataCacheTest — 5 test

- **Sorgente:** [VideoMetadataCacheTest.java](../app/src/test/java/it/edo/codecprobe/VideoMetadataCacheTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** VideoMetadataCache; InputInspector.Report costruttori.
- **Riferimenti produttivi:** [VideoMetadataCache](../app/src/main/java/it/edo/codecprobe/VideoMetadataCache.java), [InputInspector](../app/src/main/java/it/edo/codecprobe/InputInspector.java).
- **Dati, file e simulazioni:** Entry/report sintetici in cache statica; token di permesso è una stringa.
- **Non verificato / fragilità:** Nessun provider o lettura media; il cambio permesso non è una revoca Android e non prova oscuramento GPS sul file.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`reusedEntryDoesNotLoseDetails`](../app/src/test/java/it/edo/codecprobe/VideoMetadataCacheTest.java#L12) | A | Get riusa la stessa Entry e conserva i dettagli. | `VideoMetadataCache.clear`, `VideoMetadataCache.permissions`, `VideoMetadataCache.put`, `VideoMetadataCache.epoch`, `VideoMetadataCache.get` |
| [`permissionChangeInvalidatesGpsAndRejectsLateResult`](../app/src/test/java/it/edo/codecprobe/VideoMetadataCacheTest.java#L19) | A | Cambio token permessi invalida cache e rigetta put con vecchia epoca. | `VideoMetadataCache.clear`, `VideoMetadataCache.permissions`, `VideoMetadataCache.epoch`, `VideoMetadataCache.put`, `VideoMetadataCache.get`, `VideoMetadataCache.size` |
| [`explicitReloadRejectsOlderWorker`](../app/src/test/java/it/edo/codecprobe/VideoMetadataCacheTest.java#L26) | A | Clear/rilettura esplicita respinge un risultato vecchio. | `VideoMetadataCache.clear`, `VideoMetadataCache.epoch`, `VideoMetadataCache.put` |
| [`boundedCacheRetainsRecentlyViewedEntry`](../app/src/test/java/it/edo/codecprobe/VideoMetadataCacheTest.java#L30) | A | Capacità 100 e promozione LRU: v0 resta, v1 esce. | `VideoMetadataCache.clear`, `VideoMetadataCache.epoch`, `VideoMetadataCache.put`, `VideoMetadataCache.get`, `VideoMetadataCache.size` |
| [`reportCopiesMapsAndPreservesErrorStatus`](../app/src/test/java/it/edo/codecprobe/VideoMetadataCacheTest.java#L36) | A | Report copia le mappe di errore ed evita aliasing con sorgente. | `InputInspector.Report` |


#### VideoSettingsParityTest — 3 test

- **Sorgente:** [VideoSettingsParityTest.java](../app/src/test/java/it/edo/codecprobe/VideoSettingsParityTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** SettingsActivity; AppSettings.
- **Riferimenti produttivi:** [SettingsActivity](../app/src/main/java/it/edo/codecprobe/SettingsActivity.java), [AppSettings](../app/src/main/java/it/edo/codecprobe/AppSettings.java).
- **Dati, file e simulazioni:** Activity/SharedPreferences Robolectric, campi privati reflection, Intent opzioni ePNG.
- **Non verificato / fragilità:** Non prova opzioni nell'encoder, picker cartelle reale o parità completa Audio/Foto; screenshot no golden.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`unsavedVideoSettingsSurviveRecreationAndBackDiscardsThem`](../app/src/test/java/it/edo/codecprobe/VideoSettingsParityTest.java#L22) | B | Form31/addition custom e onlySmallerfalse sopravvive recreate; Back lascia default23/true. | Helper `field(…)`, `button(…)`; percorso specificato sopra |
| [`copyingCurrentIncludesFolderAndCommonOptionsAndOnlySaveWritesDefaults`](../app/src/test/java/it/edo/codecprobe/VideoSettingsParityTest.java#L29) | B | Copia corrente include folder e flag ma solo Save persiste CRF29/cartellaB/onlySmallerfalse/keepScreenOn. | Helper `button(…)`, `field(…)`; percorso specificato sopra |
| [`settingsFrameRender`](../app/src/test/java/it/edo/codecprobe/VideoSettingsParityTest.java#L37) | B | PNG pagina settings Video, solo compressione riuscita. | Helper `field(…)`; percorso specificato sopra |


### Audio


#### AudioConversionTest — 6 test

- **Sorgente:** [AudioConversionTest.java](../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** AudioOutput.name/unique; MediaConversionGate; AudioLog; AudioController; AudioConversionService.onStartCommand.
- **Riferimenti produttivi:** [AudioOutput](../app/src/main/java/it/edo/codecprobe/AudioOutput.java), [MediaConversionGate](../app/src/main/java/it/edo/codecprobe/MediaConversionGate.java), [AudioLog](../app/src/main/java/it/edo/codecprobe/AudioLog.java), [AudioController](../app/src/main/java/it/edo/codecprobe/AudioController.java), [AudioConversionService](../app/src/main/java/it/edo/codecprobe/AudioConversionService.java).
- **Dati, file e simulazioni:** Robolectric; nei test del servizio stato statico sostituito con reflection, URI fittizi; nel log vero file privato del Context simulato.
- **Non verificato / fragilità:** Nessun FLAC letto o Opus codificato. Lease confrontata direttamente, non due conversioni concorrenti.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`outputNamesNeverOverwriteAndKeepOriginalBase`](../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java#L19) | A | Nome .opus e suffissi tramite Set, non vera assenza di overwrite sul provider. | `AudioOutput.name`, `AudioOutput.unique` |
| [`audioAndVideoUseTheSameLeaseUntilCleanup`](../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java#L25) | A | Identità del campo privato RUNS e acquisizione/rilascio della lease; forte accoppiamento interno. | `ConversionService.class.getDeclaredField`, `MediaConversionGate.INSTANCE.acquire`, `MediaConversionGate.INSTANCE.release`, `MediaConversionGate.INSTANCE.owns` |
| [`audioLogIsBoundedAndPreservesReadableSteps`](../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java#L33) | C | Scrive circa 3 MB di testo nel log reale e controlla limite/tail/marcatore. | `log.write`, `log.tail`, `AudioLog.file` |
| [`resetClearsAudioDraftWithoutChangingVideoOrSettings`](../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java#L45) | B | Clear pulisce bozza/pulsanti/etichette audio e conserva cartella; malgrado il nome, non confronta stato Video o impostazioni salvate. | `AudioConversionService.clear`, `audio.state.inputs.add`, `audio.state.names.add`, `audio.render`, `audio.screen.start.isEnabled`, `audio.screen.cancel.isEnabled`, `audio.screen.clear.performClick`, `audio.state.names.isEmpty`, `audio.screen.parameters.getText`, `audio.screen.status.getText` |
| [`staleCancelCannotChangeAnotherAudioRun`](../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java#L56) | B | Intent CANCEL con startedAt vecchio non porta cancelling su uno stato iniettato: manca il controllo positivo del comando corrente. | `AudioConversionService.class.getDeclaredField`, `service.get`, `AudioConversionService.snapshot`, `service.destroy` |
| [`busySharedLeaseRejectsAudioBeforeLoadingNativeCode`](../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java#L65) | B | Lease già occupata: START non avvia il servizio audio né carica JNI e conserva il proprietario. | `MediaConversionGate.INSTANCE.acquire`, `service.get`, `AudioConversionService.snapshot`, `MediaConversionGate.INSTANCE.owns`, `MediaConversionGate.INSTANCE.release`, `service.destroy`, `AudioConversionService.clear` |


#### AudioFlowTest — 9 test

- **Sorgente:** [AudioFlowTest.java](../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** AudioController/Selection/Actions; AudioDraftStore; AudioRunState; AudioOutput.unique.
- **Riferimenti produttivi:** [AudioController](../app/src/main/java/it/edo/codecprobe/AudioController.java), [AudioDraftStore](../app/src/main/java/it/edo/codecprobe/AudioDraftStore.java), [AudioRunState](../app/src/main/java/it/edo/codecprobe/AudioRunState.java), [AudioOutput](../app/src/main/java/it/edo/codecprobe/AudioOutput.java).
- **Dati, file e simulazioni:** Provider Names fittizio restituisce nomi/12345 byte/directory scrivibile, senza audio. Stato servizi e risultati iniettati via reflection; MainLooper pilotato.
- **Non verificato / fragilità:** Intent intercettati, non servizio/encoder avviato. Helper ready usa polling con Thread.sleep(5) e timeout 5 s; vista dei report legata a childAt.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`everySelectedTrackCanBeInspectedAndRemoved`](../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java#L41) | B | 13 brani artificiali selezionabili/rimovibili, indice salvato e apertura impostazioni via Intent. | `audio.state.inputs.add`, `audio.state.names.add`, `audio.render`, `audio.screen.navigator.getChildCount`, `audio.screen.navigator.getChildAt`, `audio.screen.names.getText`, `audio.screen.remove.performClick`, `audio.state.inputs.size`, `AudioDraftStore.load`, `audio.state.names.contains`, `audio.screen.log.getVisibility`, `audio.screen.sections.bodies.containsKey`, `audio.screen.settings.performClick`, `SettingsActivity.class.getName`, `audio.screen.settings.getText` |
| [`invalidNewLyricsSelectionCannotLeaveOldLyricsSilentlySelected`](../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java#L51) | B | Selezione .txt invalida azzera vecchi testi LRC anche nella bozza. | `audio.state.lyricsInputs.add`, `audio.state.lyricsNames.add`, `audio.selection.result`, `audio.state.lyricsInputs.isEmpty`, `AudioDraftStore.load` |
| [`filenameReadInterruptedByRecreationIsRestarted`](../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java#L58) | B | Bozza interrotta durante lettura nomi viene ripresa e aggiorna real.flac. | `draft.inputs.add`, `draft.names.add`, `AudioDraftStore.save`, `AudioDraftStore.load` |
| [`preflightFreezesParametersAndPreventsDoubleStart`](../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java#L64) | B | Preflight congela bitrate 160 e onlySmaller, disabilita comandi e impedisce doppio lancio anche dopo modifica UI a 96. | `audio.actions.start`, `audio.screen.start.isEnabled`, `audio.screen.pick.isEnabled` |
| [`ambiguousLyricsAreReportedBeforeStartingTheEncoder`](../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java#L73) | B | Due LRC omonimi ambigui impediscono START e spiegano l'errore prima dell'encoder. | `audio.state.lyricsInputs.addAll`, `audio.state.lyricsNames.addAll`, `audio.render`, `audio.screen.lyrics.getText`, `audio.actions.start`, `audio.screen.status.getText` |
| [`newRunWithSameResultCountReplacesThePreviousReport`](../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java#L80) | B | Nuova sessione con stesso numero risultati sostituisce la riga old con new. | `AudioRunState.Result`, `audio.renderRun`, `audio.screen.report.getChildAt` |
| [`cancelledAndUnsavedTracksAreNotPresentedAsFailuresAndDoNotRetry`](../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java#L87) | B | Esiti SAVED/CANCELLED/NOT_SAVED/FAILED preinseriti: solo FAILED entra nell'Intent retry; non produce quegli esiti nel motore. | `run.results.add`, `AudioRunState.Result`, `audio.renderRun`, `audio.screen.summary.getText`, `audio.screen.totalProgress.getText`, `audio.actions.retry` |
| [`screenFlagIsScopedToVisibleRunningAudioAndResetKeepsSettings`](../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java#L98) | B | Flag schermo attivo solo con pagina visibile e run iniettata; reset conserva impostazioni correnti. | `audio.screen.keepScreenOn.setChecked`, `audio.screen.bitrate.setProgress`, `audio.renderRun`, `audio.screen.clear.performClick`, `audio.state.inputs.isEmpty` |
| [`progressReservesVerificationAndReportsIncompleteQueue`](../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java#L106) | A | Percentuale coda e categorie su AudioRunState costruito; nessuna fase di verifica effettivamente eseguita. | `run.queuePercent`, `run.results.add`, `AudioRunState.Result`, `run.summary`, `run.savings`, `AudioOutput.unique` |


#### AudioPacketCopierTest — 7 test

- **Sorgente:** [AudioPacketCopierTest.java](../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** AudioPacketCopier.copy.
- **Riferimenti produttivi:** [AudioPacketCopier](../app/src/main/java/it/edo/codecprobe/AudioPacketCopier.java).
- **Dati, file e simulazioni:** Packets implementa Source con array di byte/timestamp; sink, check e log sono lambda di prova. ByteBuffer reali, non MediaExtractor/MediaMuxer.
- **Non verificato / fragilità:** Può passare con extractor/muxer AAC Android guasti: verifica il ciclo di copia su un'interfaccia simulata, non un file audio.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`negativePrerollZeroAndPositivePacketsKeepEveryByteAndTimestamp`](../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java#L22) | A | Preroll negativo: conserva tutti i byte e sei timestamp, logga quattro PTS negativi. Il nome menziona zero ma questa fixture ha quattro PTS negativi e due positivi; zero è esercitato dal test successivo. | `AudioPacketCopier.copy`, `log.stream` |
| [`minusOneIsAValidTimestampWhenSampleExists`](../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java#L33) | A | PTS -1 non è EOF se esiste un campione. | `AudioPacketCopier.copy` |
| [`positiveStartingTimeIsNotRebased`](../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java#L38) | A | PTS iniziale positivo non viene ribasato. | `AudioPacketCopier.copy` |
| [`diagnosticsAreBoundedButCopyContinues`](../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java#L43) | A | Copia 100 pacchetti ma limita a 12 la diagnostica iniziale. | `AudioPacketCopier.copy`, `log.stream` |
| [`emptyAndMalformedSourcesAreErrorsInsteadOfSilentAudioLoss`](../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java#L50) | A | Errori per zero pacchetti, PTS duplicati/decrescenti, protezione, lettura corta, traccia errata e pacchetto oltre 16 MiB. | Helper `assertFailure(…)`; percorso specificato sopra |
| [`cancellationStopsBeforeReadingNextPacket`](../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java#L63) | A | Check annullamento interrompe prima del secondo pacchetto. | `AudioPacketCopier.copy` |
| [`unsupportedMuxerReportsTheOriginalNegativeTimestamp`](../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java#L72) | A | Sink che rifiuta il PTS negativo produce IOException con il PTS originario, non lo corregge silenziosamente. | `AudioPacketCopier.copy` |


#### AudioPcmTest — 3 test

- **Sorgente:** [AudioPcmTest.java](../app/src/test/java/it/edo/codecprobe/AudioPcmTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** AudioMediaDecoder.kind; AudioPcm.read/bytes.
- **Riferimenti produttivi:** [AudioMediaDecoder](../app/src/main/java/it/edo/codecprobe/AudioMediaDecoder.java), [AudioPcm](../app/src/main/java/it/edo/codecprobe/AudioPcm.java).
- **Dati, file e simulazioni:** ByteBuffer sintetici; Robolectric è presente ma vengono usate costanti AudioFormat, nessun decoder avviato.
- **Non verificato / fragilità:** Non decodifica MP3/M4A/FLAC reali, né verifica formato negoziato da MediaCodec.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`detectsSupportedInputsWithoutChangingFlacRoute`](../app/src/test/java/it/edo/codecprobe/AudioPcmTest.java#L14) | A | Routing da estensione FLAC/MP3/M4A e rifiuto Opus; non apertura del contenuto. | `AudioMediaDecoder.kind` |
| [`decodesSignedAndUnsignedSamplesWithCorrectScale`](../app/src/test/java/it/edo/codecprobe/AudioPcmTest.java#L18) | A | Conversione numerica 8/16/24 bit dei campioni estremi verso float con scala prevista. | `AudioPcm.read` |
| [`handlesFloatOffsetsAndRejectsCorruption`](../app/src/test/java/it/edo/codecprobe/AudioPcmTest.java#L27) | A | Rispetta offset/limit per float e rifiuta NaN, byte insufficienti e encoding sconosciuto. | `AudioPcm.read`, `AudioPcm.bytes` |


#### AudioSettingsTest — 6 test

- **Sorgente:** [AudioSettingsTest.java](../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** AudioSettingsScreen/AudioSettings; AudioController; AudioDraftStore.
- **Riferimenti produttivi:** [AudioSettingsScreen](../app/src/main/java/it/edo/codecprobe/AudioSettingsScreen.java), [AudioSettings](../app/src/main/java/it/edo/codecprobe/AudioSettings.java), [AudioController](../app/src/main/java/it/edo/codecprobe/AudioController.java), [AudioDraftStore](../app/src/main/java/it/edo/codecprobe/AudioDraftStore.java).
- **Dati, file e simulazioni:** Activity/SharedPreferences simulate; AudioRunState iniettato tramite campo privato e nessun brano aperto.
- **Non verificato / fragilità:** Non prova salvataggio su storage persistente di un processo reale, configurazione nativa o lifecycle imposto dal sistema.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`headerOpensDedicatedAudioPageWithCurrentConfiguration`](../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java#L25) | B | Header passa configurazione corrente 160 all'Activity impostazioni audio dedicata. | `home.get`, `audio.screen.bitrate.setProgress`, `audio.screen.settings.performClick`, `SettingsActivity.class.getName`, `home.pause` |
| [`unsavedEditsSurviveRecreationButBackDoesNotSave`](../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java#L32) | B | Modifiche non salvate sopravvivono al Bundle, Back non cambia revisione/default. | Helper `intent(…)`; percorso specificato sopra |
| [`savedDefaultsUpdateAudioHomeWithoutChangingSelectedFilesOrVideo`](../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java#L40) | B | Salva default 192 e flag/cartella senza perdere file/LRC o modificare prefs Video. | `home.get`, `audio.state.inputs.add`, `audio.state.names.add`, `audio.state.lyricsInputs.add`, `audio.state.lyricsNames.add`, `audio.save`, `audio.renderRun`, `audio.screen.bitrate.getProgress`, `audio.screen.onlySmaller.isChecked`, `audio.screen.keepScreenOn.isChecked`, `home.pause` |
| [`defaultsSavedDuringConversionApplyOnlyAfterItsEnd`](../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java#L51) | B | Nuovi default durante run iniettata restano differiti fino a run terminata; stato del servizio rimane 128. | `home.get`, `audio.renderRun`, `audio.screen.settings.isEnabled`, `audio.save`, `AudioSettings.save`, `AudioConversionService.snapshot`, `home.pause` |
| [`explicitFolderRemovalDoesNotFallBackToOldDraftAndDefaultsStaySeparate`](../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java#L59) | B | Rimozione esplicita cartella non riporta una vecchia bozza; default e bozza restano separati. | `AudioDraftStore.save`, `AudioSettings.save`, `AudioDraftStore.load` |
| [`copyCurrentAndFolderCancelOnlyChangeUnsavedForm`](../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java#L64) | B | Copia corrente modifica solo form; cancel o concessione readonly non sostituiscono cartella né default. | `AudioSettings.save`, `FolderPickerActivity.class.getName` |


#### AudioWorkspaceTest — 4 test

- **Sorgente:** [AudioWorkspaceTest.java](../app/src/test/java/it/edo/codecprobe/AudioWorkspaceTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** AudioController/Selection; AudioDraftStore; SavedSession; AppNavigation.
- **Riferimenti produttivi:** [AudioController](../app/src/main/java/it/edo/codecprobe/AudioController.java), [AudioDraftStore](../app/src/main/java/it/edo/codecprobe/AudioDraftStore.java), [SavedSession](../app/src/main/java/it/edo/codecprobe/SavedSession.java), [AppNavigation](../app/src/main/java/it/edo/codecprobe/AppNavigation.java).
- **Dati, file e simulazioni:** Activity Robolectric, provider Names sintetico; un vero journal Video usato come sentinella e confrontato byte per byte; executor reale/latch.
- **Non verificato / fragilità:** Nessun file audio o conversione. Latch con timeout e worker get(5 s); navigazione osservata come Intent.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`independentAudioDraftAndSharedViewsSurviveReopeningWithoutChangingSavedVideo`](../app/src/test/java/it/edo/codecprobe/AudioWorkspaceTest.java#L37) | B | Bozza audio e card riaperte conservano configurazione mentre journal Video e CRF rimangono identici. | `SavedSession.save`, `journal.toPath`, `audio.screen.sections.bodies.get`, `audio.screen.navigation.audio.isSelected`, `audio.screen.navigation.video.isSelected`, `audio.screen.pick.getElevation`, `audio.state.inputs.add`, `audio.state.names.add`, `audio.screen.bitrate.setProgress`, `audio.render`, `audio.screen.sections.focusCard`, `SavedSession.clear` |
| [`multipleAudioFilesUseOnlyAudioPickerAndCancelledSelectionKeepsDraft`](../app/src/test/java/it/edo/codecprobe/AudioWorkspaceTest.java#L59) | B | Picker audio multiselezione deduplica lo stesso URI e ignora annullamento/cartella readonly. | `audio.screen.pick.performClick`, `audio.selection.result`, `audio.selection.worker.submit`, `audio.state.inputs.size`, `audio.screen.selected.getText`, `audio.screen.chooseFolder.performClick`, `FolderPickerActivity.class.getName`, `audio.destinations.result` |
| [`navigationKeepsVideoSelectionAndDoesNotTouchServiceState`](../app/src/test/java/it/edo/codecprobe/AudioWorkspaceTest.java#L76) | B | Navigare Video↔Audio mantiene selezione, sessionId/running/report del servizio iniettato. | `home.state.selectedInputs.add`, `home.state.selectedNames.add`, `ConversionService.snapshot`, `home.screen.navigation.audio.performClick`, `AudioActivity.class.getName`, `audio.get`, `audio.screen.navigation.video.performClick`, `MainActivity.class.getName`, `audio.pause` |
| [`lateFilenameCallbackCannotRepopulateClearedAudioSelection`](../app/src/test/java/it/edo/codecprobe/AudioWorkspaceTest.java#L88) | B | Callback nomi bloccata con latch e poi rilasciata non ripopola una selezione già cancellata. | `audio.selection.worker.submit`, `audio.selection.result`, `audio.screen.start.isEnabled`, `audio.selection.clear`, `audio.state.inputs.isEmpty`, `AudioDraftStore.load` |


### Foto


#### ImageCarouselCacheTest — 1 test

- **Sorgente:** [ImageCarouselCacheTest.java](../app/src/test/java/it/edo/codecprobe/ImageCarouselCacheTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageController.open/read; ImageMetadataCache; PhotoCompose.select/command.
- **Riferimenti produttivi:** [ImageController](../app/src/main/java/it/edo/codecprobe/ImageController.java), [ImageMetadataCache](../app/src/main/java/it/edo/codecprobe/ImageMetadataCache.java).
- **Dati, file e simulazioni:** Due veri JPEG sintetici e poi primo sostituito da3byte; Activity/ContentResolver file:// simulati, cache condivisa.
- **Non verificato / fragilità:** Non garantisce invalidazione automatica su cambi file: prova precisamente che i vecchi dati restano fino REREAD. Polling5ms/5s e tempo virtuale1s; nessun thumbnail.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`carouselReusesPhotoMetadataAcrossNavigationAndActivityRecreationUntilReload`](../app/src/test/java/it/edo/codecprobe/ImageCarouselCacheTest.java#L35) | B | Navigazione/recreate riusano stessa Info anche dopo modifica file; REREAD rileva non-JPEG, CLEAR svuota cache ma conserva sorgente. | `images.selection.result`, `images.compose.command`, `images.compose.getState`, `images.compose.select`, `images.state.notice.contains`, `ImageMetadataCache.memory.size` |


#### ImageFolderImportTest — 2 test

- **Sorgente:** [ImageFolderImportTest.java](../app/src/test/java/it/edo/codecprobe/ImageFolderImportTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageFolderReader.read; ImageSelection.result; PhotoCommand.ADD_FOLDER; PhotoCompose state.
- **Riferimenti produttivi:** [ImageFolderReader](../app/src/main/java/it/edo/codecprobe/ImageFolderReader.java), [ImageSelection](../app/src/main/java/it/edo/codecprobe/ImageSelection.java).
- **Dati, file e simulazioni:** Provider Folder genera8500 righe JPEG più HEIC/video/directory; nessun byte immagine fornito.
- **Non verificato / fragilità:** Non prova foto leggibili o ricorsione sottocartelle; import UI polling10ms fino15s. Queue grande non è benchmark né memoria misurata.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`folderListingImportsAllJpegsWithoutReadingPhotoBytes`](../app/src/test/java/it/edo/codecprobe/ImageFolderImportTest.java#L37) | B | Listing importa8500 soli JPEG, filtra HEIC/video/directory e assegna cartella senza leggere pixel. | `ImageFolderReader.read` |
| [`oneFolderPickerResultBuildsTheWholeQueueAndOnlyOneHundredChips`](../app/src/test/java/it/edo/codecprobe/ImageFolderImportTest.java#L40) | B | Risultato folder picker crea8500 riferimenti ma100 item Compose, origine senza eccezioni. | `images.compose.command`, `images.selection.result`, `images.state.inputs.size`, `images.compose.getState`, `images.state.inputs.get`, `images.state.destinations.plan.effective`, `images.state.destinations.plan.exceptionCount` |


#### ImageLargeQueueTest — 8 test

- **Sorgente:** [ImageLargeQueueTest.java](../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageRunState; SelectedItemView; ImageWorkReport; ImageQueueStore; ImageFailureStore; ImageConversionService.
- **Riferimenti produttivi:** [ImageRunState](../app/src/main/java/it/edo/codecprobe/ImageRunState.java), [SelectedItemView](../app/src/main/java/it/edo/codecprobe/SelectedItemView.java), [ImageWorkReport](../app/src/main/java/it/edo/codecprobe/ImageWorkReport.java), [ImageQueueStore](../app/src/main/java/it/edo/codecprobe/ImageQueueStore.java), [ImageFailureStore](../app/src/main/java/it/edo/codecprobe/ImageFailureStore.java), [ImageConversionService](../app/src/main/java/it/edo/codecprobe/ImageConversionService.java).
- **Dati, file e simulazioni:** 5000 risultati/riferimenti manuali; files reali JSON/log nel Context; provider directory falso;125 URI mancanti. Reflection state/write.
- **Non verificato / fragilità:** Nessuna conversione riuscita/codec; dimensioni in byte solo numeri. Timeout15s e polling, conteggio100 hardcoded. Prove cap non sono misure RAM/CPU.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`fiveThousandOutcomesKeepOnlyOneHundredDetailsAndExactTotals`](../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java#L25) | A | 5000 SAVED fittizi tengono100 dettagli e somme64bit esatte; dettagli errore100k troncati. | `run.add`, `ImageRunState.Result`, `run.results.size`, `run.firstResult`, `run.resultAt`, `run.results.get` |
| [`carouselShowsAllSmallQueuesAndOnlyOneHundredPlusEllipsisForLargeQueues`](../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java#L35) | B | SelectedItemView crea100 chip più ellissi per5000; non il carosello Compose attuale. | `view.render`, `view.chips.size`, `view.navigator.getChildCount`, `view.navigator.getChildAt`, `view.hint.getText`, `view.close` |
| [`reportCentersCurrentPhotoAndStillIncludesTotalsAfterEviction`](../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java#L44) | B | Report centra foto1000 e mantiene totali999 dopo eviction, confini finestra verificati. | `run.add`, `ImageRunState.Result`, `images.report.render`, `images.report.results.size`, `images.report.results.item`, `images.screen.workReport.summary.getText`, `run.windowStart` |
| [`fullQueueIsOnDiskAndTheIntentContainsOnlyItsIdentifier`](../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java#L55) | C | Queue5000 su disco letta identica; Intent con solo id<4096byte; liste disallineate e ../id respinti. | `ImageQueueStore.write`, `ImageQueueStore.read`, `ImageQueueStore.discard` |
| [`failureArchiveSurvivesHistoryEvictionAndContainsRetryInputs`](../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java#L66) | C | 125 errori reali serializzati sopravvivono a finestra100 e clear stato; primo record conserva URI/opzioni/dettagli. | `ImageOptions.load`, `ImageRunState.Result`, `ImageFailureStore.append`, `run.add`, `run.results.size`, `run.resultAt`, `ImageConversionService.clear` |
| [`serviceContinuesAfterMoreThanOneHundredFailuresAndPreservesEveryError`](../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java#L77) | B | Servizio elabora125 file assenti senza fermarsi, conserva tutti errori/log, coda rimossa e tail limitata. | `ImageConversionService.class.getDeclaredField`, `ImageQueueStore.write`, `ImageOptions.load`, `ImageConversionService.snapshot`, `run.results.size`, `log.contains`, `run.log.length`, `ImageQueueStore.file`, `ImageQueueStore.discard` |
| [`largeDraftRestoresAllFiveThousandReferences`](../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java#L95) | B | Bozza prefs con5000 riferimenti si rilegge completa. | `ImageOptions.load`, `state.inputs.add`, `state.names.add`, `ImageDraftStore.save`, `ImageDraftStore.load` |
| [`diskLogKeepsEarlyErrorsBeyondTheOldTwoMibRotationLimit`](../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java#L100) | B | Write privato oltre2MiB conserva FIRST/LAST su disco e tail<=24000; non misura rotazione durante conversione. | `ImageConversionService.class.getDeclaredField`, `ImageConversionService.class.getDeclaredMethod`, `ImageConversionService.snapshot`, `log.length` |


#### ImagePreviewSchedulingTest — 1 test

- **Sorgente:** [ImagePreviewSchedulingTest.java](../app/src/test/java/it/edo/codecprobe/ImagePreviewSchedulingTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageController.readNames/open; worker metadati e selection worker.
- **Riferimenti produttivi:** [ImageController](../app/src/main/java/it/edo/codecprobe/ImageController.java).
- **Dati, file e simulazioni:** Provider Photos blocca nome slow con latch; fast restituisce nome ma nessun file apribile, executor private via reflection.
- **Non verificato / fragilità:** Il nome parla di analysis: si verifica reading=false/fast.jpg mentre bulk è bloccato, non parsing riuscito o pixel. Timeout3/5s.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`selectedPhotoAnalysisFinishesWhileBulkNameProviderIsStillBlocked`](../app/src/test/java/it/edo/codecprobe/ImagePreviewSchedulingTest.java#L29) | B | Analisi/selezione fast termina mentre query nomi slow resta bloccata: protegge separazione executor. | `ImageMetadataCache.memory.clear`, `images.state.inputs.add`, `images.state.clamp`, `images.readNames`, `images.open`, `ImageController.class.getDeclaredField` |


#### ImageResponsivenessTest — 5 test

- **Sorgente:** [ImageResponsivenessTest.java](../app/src/test/java/it/edo/codecprobe/ImageResponsivenessTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** SelectedItemView; ImageController.refresh; ImageDraftStore; ImageSelection; ImageResultList.
- **Riferimenti produttivi:** [SelectedItemView](../app/src/main/java/it/edo/codecprobe/SelectedItemView.java), [ImageController](../app/src/main/java/it/edo/codecprobe/ImageController.java), [ImageDraftStore](../app/src/main/java/it/edo/codecprobe/ImageDraftStore.java), [ImageSelection](../app/src/main/java/it/edo/codecprobe/ImageSelection.java), [ImageResultList](../app/src/main/java/it/edo/codecprobe/ImageResultList.java).
- **Dati, file e simulazioni:** AbstractList8500 con get>=100 vietato; State/report manuali, prefs simulate e125 URI senza provider.
- **Non verificato / fragilità:** Controlli di lavoro evitato/identità oggetti, non jank/CPU/RAM misurati. SelectedItemView è helper View legacy, non carosello Compose corrente. Polling5s.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`unchangedLargeCarouselDoesNotReadHiddenNamesOrReplaceButtonBackgrounds`](../app/src/test/java/it/edo/codecprobe/ImageResponsivenessTest.java#L19) | B | Render invariato non legge nomi nascosti e mantiene bottoni/background, update cambia solo selezione. | `view.render`, `view.chips.get`, `view.navigator.getChildCount`, `view.number.getText`, `view.update`, `view.close` |
| [`progressTicksLeaveTheSelectionUntouchedAndReuseReportRows`](../app/src/test/java/it/edo/codecprobe/ImageResponsivenessTest.java#L35) | B | Tick di progresso non sostituisce PhotoUiState e riusa righe report; aggiorna quella corrente. | `ImageConversionService.class.getDeclaredField`, `images.state.inputs.add`, `images.state.names.add`, `images.render`, `images.screen.sections.focusCard`, `images.report.results.item`, `images.compose.getState`, `images.refresh` |
| [`cachedPersistenceKeepsAllFilesAndDetectsIndividualDestinationEdits`](../app/src/test/java/it/edo/codecprobe/ImageResponsivenessTest.java#L49) | B | Draft8500 riusa Selection cache su indice/opzioni, invalida su eccezione cartella o nome modificato. | `ImageOptions.load`, `state.inputs.add`, `state.names.add`, `ImageDraftStore.save`, `ImageDraftStore.load`, `state.destinations.plan.setFolder`, `state.names.set` |
| [`manualSelectionReturnsWhilePermissionWorkIsPending`](../app/src/test/java/it/edo/codecprobe/ImageResponsivenessTest.java#L59) | B | Selezione125 ritorna importing e un Handler può eseguire; non impone budget latenza prima callback né blocca davvero provider. | `images.selection.result`, `images.state.inputs.size`, `images.screen.convert.isEnabled` |
| [`reusedResultButtonsTargetTheNewFileAfterTheWindowMoves`](../app/src/test/java/it/edo/codecprobe/ImageResponsivenessTest.java#L73) | B | Finestra report che si sposta ricicla riga ma Apri punta al nuovo URI999, non al vecchio. | `run.add`, `ImageRunState.Result`, `images.report.render` |


#### ImageSettingsTest — 5 test

- **Sorgente:** [ImageSettingsTest.java](../app/src/test/java/it/edo/codecprobe/ImageSettingsTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageController; PhotoCompose state/command; ImageSettingsActivity; ImageOptions; ImageDraftStore; ImageConversionService.
- **Riferimenti produttivi:** [ImageController](../app/src/main/java/it/edo/codecprobe/ImageController.java), [ImageSettingsActivity](../app/src/main/java/it/edo/codecprobe/ImageSettingsActivity.java), [ImageOptions](../app/src/main/java/it/edo/codecprobe/ImageOptions.java), [ImageDraftStore](../app/src/main/java/it/edo/codecprobe/ImageDraftStore.java), [ImageConversionService](../app/src/main/java/it/edo/codecprobe/ImageConversionService.java).
- **Dati, file e simulazioni:** Activity/service/SharedPreferences Robolectric; run iniettate e lease condivisa acquisita a mano.
- **Non verificato / fragilità:** Nessun encoder né opzioni native applicate; stale cancel ha solo controllo negativo. KeepScreenOn è flag modello finestra, non comportamento device.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`headerOpensDedicatedSettingsAndUnsavedEditsSurviveRecreation`](../app/src/test/java/it/edo/codecprobe/ImageSettingsTest.java#L19) | B | Header apre settings dedicate con quality70; copy/form non salvato ricreato mantiene80 ma default65/revision0. | `home.get`, `images.quality`, `images.setFolder`, `images.compose.command`, `ImageSettingsActivity.class.getName`, `screen.copy.performClick`, `screen.quality.value`, `screen.quality.quality.setProgress`, `screen.smaller.setChecked`, `screen.smaller.isChecked`, `screen.back.performClick`, `ImageOptions.load`, `home.pause` |
| [`savedDefaultsApplyAfterActiveQueueAndKeepSelection`](../app/src/test/java/it/edo/codecprobe/ImageSettingsTest.java#L27) | B | Default80/ssim salvati differiti durante run60, blocca edit; fine run aggiorna opzioni senza perdere selezione. | `home.get`, `images.state.inputs.add`, `images.refresh`, `ImageConversionService.snapshot`, `images.quality`, `images.compose.getState`, `images.state.inputs.size`, `home.pause` |
| [`imageDraftIsSeparateAndFolderCancellationKeepsChoice`](../app/src/test/java/it/edo/codecprobe/ImageSettingsTest.java#L34) | B | Cancel cartella conserva scelta, draft60 separato default65; rimozione folder nei nuovi default applicata. | `home.get`, `images.setFolder`, `images.selection.result`, `images.quality`, `images.smaller`, `images.persist`, `ImageDraftStore.load`, `ImageOptions.load`, `images.refresh`, `home.pause` |
| [`leaseRejectsImagesBeforeNativeAndStaleCancellationCannotStopAnotherRun`](../app/src/test/java/it/edo/codecprobe/ImageSettingsTest.java#L41) | B | Lease occupata respinge START prima JNI e token resta posseduto; CANCEL startedAt21 non cambia run22 iniettata. | `MediaConversionGate.INSTANCE.acquire`, `service.get`, `ImageConversionService.snapshot`, `MediaConversionGate.INSTANCE.owns`, `service.destroy`, `MediaConversionGate.INSTANCE.release` |
| [`screenAwakeOptionRemainsLiveDuringPhotoConversionAndClearsOnPause`](../app/src/test/java/it/edo/codecprobe/ImageSettingsTest.java#L47) | B | KeepScreenOn modificabile a runtime su vista, non muta opzioni run congelate; pause clears flag. | `images.screen.screenOn.isEnabled`, `images.screen.screenOn.setChecked`, `ImageConversionService.snapshot`, `images.pause` |


#### ImageUiTrialTest — 6 test

- **Sorgente:** [ImageUiTrialTest.java](../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageController; PhotoCompose bridge command/select; ImageSelection; ImageDestinations.
- **Riferimenti produttivi:** [ImageController](../app/src/main/java/it/edo/codecprobe/ImageController.java), [ImageSelection](../app/src/main/java/it/edo/codecprobe/ImageSelection.java).
- **Dati, file e simulazioni:** Piccoli file JPEG sintetici; provider directory/folder fittizi, Activity/dialog shadow. Test chiama prevalentemente bridge Java anziché nodo Compose.
- **Non verificato / fragilità:** Non valida gesto touch o rendering nei primi cinque. Import8500 senza byte; polling5ms/10s. Conteggio root/pannelli è vincolo architetturale interno.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`navigationRetainsDraftAndQualityAcrossRecreation`](../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java#L38) | B | Avanti/indietro mantiene bozza/quality e restore step compressione. | `images.screen.sections.current`, `images.screen.sections.focusCard`, `images.selection.result`, `images.next`, `images.quality`, `images.back`, `images.compose.getState` |
| [`longPressSelectionUsesGroupAndRequiresConfirmationBeforeRemoval`](../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java#L54) | B | Selezione gruppo0,2 e Remove richiede conferma; originali esistono ancora dopo rimozione/clear. | `images.state.inputs.add`, `images.state.clamp`, `images.open`, `images.compose.select`, `images.selectionIndices`, `images.selectedPhotos.size`, `images.compose.getState`, `images.compose.command`, `images.state.inputs.size`, `images.state.inputs.isEmpty` |
| [`folderImportIsAdditiveAndKeepsExistingDestinationEvenForDuplicates`](../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java#L70) | B | Folder import additivo deduplica URI e conserva eccezione cartella del vecchio file, anche ripetuto. | `images.state.inputs.add`, `images.state.clamp`, `images.state.destinations.sync`, `images.state.destinations.plan.setFolder`, `images.importFolder`, `images.state.inputs.contains`, `images.state.inputs.size`, `images.state.destinations.plan.files.get` |
| [`addingFilesKeepsFocusNamesAndExistingMetadataCache`](../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java#L82) | B | Aggiunta terzo file conserva focus/nome/cache del secondo e stesso URI non duplica. | `images.selection.result`, `images.open`, `images.state.names.get`, `images.state.inputs.size` |
| [`folderPickerFreezesSelectedGroupAcrossRecreation`](../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java#L94) | B | Gruppo0,2 congelato nel picker sopravvive recreate e riceve cartellaB lasciando1 inA. | `images.state.inputs.add`, `images.state.clamp`, `images.setFolder`, `images.open`, `images.toggleSelection`, `images.destinations.photoException`, `images.destinations.choosePhotoFolder`, `images.selection.result`, `images.state.destinations.plan.files.get`, `images.state.destinations.plan.effective` |
| [`composeIsTheOnlyWorkspaceRootAndNoHiddenPhotoPanelsAreBuilt`](../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java#L106) | B | Compose unica root installata e nessun pannello legacy FILES/COMPRESSION creato; HEIC/DETAILS restano interop. | `images.compose.getView`, `images.screen.sections.bodies.size`, `images.screen.sections.bodies.containsKey`, `images.screen.sections.bodies.get` |


#### ImageWorkspaceTest — 5 test

- **Sorgente:** [ImageWorkspaceTest.java](../app/src/test/java/it/edo/codecprobe/ImageWorkspaceTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageController/Selection/export; JpegInventory; ImageDraftStore; PhotoCompose.command; AppNavigation.
- **Riferimenti produttivi:** [ImageController](../app/src/main/java/it/edo/codecprobe/ImageController.java), [JpegInventory](../app/src/main/java/it/edo/codecprobe/JpegInventory.java), [ImageDraftStore](../app/src/main/java/it/edo/codecprobe/ImageDraftStore.java), [AppNavigation](../app/src/main/java/it/edo/codecprobe/AppNavigation.java).
- **Dati, file e simulazioni:** Vero JPEG minimale sintetico con APP1 'original metadata' (non TIFF EXIF valido), APP7, SOF48x32, finta scansione e3byte trailing. File JSON reale.
- **Non verificato / fragilità:** Non è JPEG con pixel decodificati; export dichiara esplicitamente pixelsDecoded=false/heicConversionApproved=false. Primo nome promette Audio+Video ma sentinella è solo prefs Audio. Polling5ms/5s.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`jpegAnalysisExportAndDraftAreIndependentFromAudioVideo`](../app/src/test/java/it/edo/codecprobe/ImageWorkspaceTest.java#L31) | B | Seleziona/analizza/esporta inventario con2payload byte-identici e sorgente preservata; CLEAR non elimina fonte o prefs Audio. | `images.screen.export.isEnabled`, `images.selection.result`, `images.compose.getState`, `images.screen.export.performClick`, `ImageDraftStore.load`, `images.compose.command`, `images.state.inputs.isEmpty` |
| [`cancelledSelectionKeepsDraftAndUnsupportedFileCannotExport`](../app/src/test/java/it/edo/codecprobe/ImageWorkspaceTest.java#L47) | B | File HEIC che contiene testo non-JPEG disabilita export; cancel mantiene riferimento. | `images.selection.result`, `images.screen.export.isEnabled`, `images.state.notice.contains`, `images.state.inputs.size` |
| [`threeTabsSelectImagesAndSwitchWithoutGrowingSecondaryBackStack`](../app/src/test/java/it/edo/codecprobe/ImageWorkspaceTest.java#L52) | B | Pagina Compose Foto visibile e comando Audio emette Intent/termina; non verifica tutti tre tab del nome. | `images.compose.getView`, `images.compose.command`, `AudioActivity.class.getName` |
| [`pendingExportSurvivesRecreationAndSourceCannotBeOverwritten`](../app/src/test/java/it/edo/codecprobe/ImageWorkspaceTest.java#L62) | B | Picker export pendente ricreato produce JSON; destinazione identica sorgente rifiutata senza cambiare byte. | `images.selection.result`, `images.screen.export.performClick`, `images.export`, `images.state.notice.contains` |
| [`imagesAreReachableFromBothExistingWorkspaces`](../app/src/test/java/it/edo/codecprobe/ImageWorkspaceTest.java#L71) | B | Foto raggiungibile da Video/Audio con policy back stack coerente. | `home.screen.navigation.images.performClick`, `ImageActivity.class.getName`, `audio.get`, `audio.screen.navigation.images.performClick`, `audio.pause` |


#### PhotoDetailsLoaderTest — 2 test

- **Sorgente:** [PhotoDetailsLoaderTest.kt](../app/src/test/java/it/edo/codecprobe/PhotoDetailsLoaderTest.kt). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** PhotoDetailsLoader.update/get/invalidate/close.
- **Riferimenti produttivi:** [PhotoDetailsLoader](../app/src/main/java/it/edo/codecprobe/PhotoDetailsLoader.kt).
- **Dati, file e simulazioni:** read lambda stub produce PhotoBasicDetails(camera=uri); executor reale e callback vuota; nessun file, parser o vista Compose. Android lifecycle handler simulato.
- **Non verificato / fragilità:** Thread.sleep(30) arbitrario e polling5ms/3s; test 'atMost32' controlla solo eviction di0-0, non un conteggio<=32, quindi capacità33/40 potrebbe passare.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`visibleMetadataIsReusedAndReloadInvalidatesOnlyRequestedSources`](../app/src/test/java/it/edo/codecprobe/PhotoDetailsLoaderTest.kt#L18) | B | Richieste a,b,c lette una volta, viewport c,b riusa cache e invalidate solo b la rilegge. | `loader.update`, `loader.get`, `loader.invalidate`, `loader.close` |
| [`changingViewportCancelsObsoleteWorkAndKeepsAtMost32SmallEntries`](../app/src/test/java/it/edo/codecprobe/PhotoDetailsLoaderTest.kt#L34) | B | Viewport nuovo interrompe slow ed evita obsolete;60 letture fanno sparire primo elemento e restare ultimo. Non misura esattamente capacità32. | `loader.update`, `loader.get`, `loader.close` |


### File e cartelle


#### AudioDestinationFlowTest — 1 test

- **Sorgente:** [AudioDestinationFlowTest.java](../app/src/test/java/it/edo/codecprobe/AudioDestinationFlowTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** MediaDestinationsController; AudioController; AudioActions.start; AudioActivity.onActivityResult.
- **Riferimenti produttivi:** [MediaDestinationsController](../app/src/main/java/it/edo/codecprobe/MediaDestinationsController.java), [AudioController](../app/src/main/java/it/edo/codecprobe/AudioController.java), [AudioActions](../app/src/main/java/it/edo/codecprobe/AudioActions.java), [AudioActivity](../app/src/main/java/it/edo/codecprobe/AudioActivity.java).
- **Dati, file e simulazioni:** MediaDestinationsTest.Directories e permessi ShadowUriPermission; due URI brano senza contenuto, ricreazione Activity simulata.
- **Non verificato / fragilità:** Nessun selettore di sistema o reale concessione SAF. Polling Thread.sleep(5), deadline 5 s. Certifica l'Intent, non uso cartelle nel codec.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`audioUsesVerifiedPerFileFoldersAndKeepsThemAcrossRecreation`](../app/src/test/java/it/edo/codecprobe/AudioDestinationFlowTest.java#L25) | B | Eccezione di destinazione per secondo file sopravvive a recreate e viene congelata nelle folders dell'Intent START. | `audio.screen.destinations.body.getVisibility`, `audio.screen.start.isEnabled`, `audio.state.inputs.add`, `audio.state.names.add`, `audio.render`, `audio.screen.destinations.pickFolder.performClick`, `audio.destinations.choosing`, `audio.destinations.snapshot`, `audio.screen.start.performClick`, `audio.screen.destinations.pickFolder.isEnabled` |


#### AudioMetadataFlowTest — 5 test

- **Sorgente:** [AudioMetadataFlowTest.java](../app/src/test/java/it/edo/codecprobe/AudioMetadataFlowTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** AudioLyrics.match/hash; AudioOutput.save; AudioDraftStore.save/load.
- **Riferimenti produttivi:** [AudioLyrics](../app/src/main/java/it/edo/codecprobe/AudioLyrics.java), [AudioOutput](../app/src/main/java/it/edo/codecprobe/AudioOutput.java), [AudioDraftStore](../app/src/main/java/it/edo/codecprobe/AudioDraftStore.java).
- **Dati, file e simulazioni:** Provider Documents di prova crea/rimuove veri file temporanei e ParcelFileDescriptor; encoded.opus sono soltanto quattro byte, LRC con BOM/CRLF.
- **Non verificato / fragilità:** Non è Opus valido né tagging nel contenitore. Il provider non replica tutti i contratti SAF (delete risponde Bundle vuoto); nessuna galleria/player provati.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`matchingPrefersSameParentAndRejectsAmbiguousTitles`](../app/src/test/java/it/edo/codecprobe/AudioMetadataFlowTest.java#L58) | A | LRC abbinato per stesso parent, ambiguità respinta e nome audio evita conflitto con sidecar. | `AudioLyrics.match`, `AudioOutput.unique` |
| [`savesMatchingPairWithOriginalBytesAndHash`](../app/src/test/java/it/edo/codecprobe/AudioMetadataFlowTest.java#L66) | B | Pubblica coppia .opus/.lrc; LRC byte-identico e suo hash nel log. Non valida decodifica audio. | `AudioOutput.save`, `log.get`, `AudioLyrics.hash` |
| [`sidecarFailureRollsBackNewAudioAndKeepsExistingFiles`](../app/src/test/java/it/edo/codecprobe/AudioMetadataFlowTest.java#L73) | B | Se create LRC fallisce, elimina l'audio appena creato e conserva keep.opus sentinella. | `AudioOutput.save` |
| [`failedReadbackRemovesBothNewFiles`](../app/src/test/java/it/edo/codecprobe/AudioMetadataFlowTest.java#L78) | B | Corrompe LRC alla rilettura: nessuno dei due nuovi file rimane. | `AudioOutput.save` |
| [`lyricsDraftIsIndependentAndSurvivesReopen`](../app/src/test/java/it/edo/codecprobe/AudioMetadataFlowTest.java#L81) | B | URI e nomi LRC persistono nelle SharedPreferences simulate dopo salvataggio bozza. | `state.lyricsInputs.add`, `state.lyricsNames.add`, `AudioDraftStore.save`, `AudioDraftStore.load` |


#### FolderPickerActivityTest — 5 test

- **Sorgente:** [FolderPickerActivityTest.java](../app/src/test/java/it/edo/codecprobe/FolderPickerActivityTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** FolderPickerActivity.onActivityResult/onCreate; ContentResolver shadow.
- **Riferimenti produttivi:** [FolderPickerActivity](../app/src/main/java/it/edo/codecprobe/FolderPickerActivity.java).
- **Dati, file e simulazioni:** Robolectric Activity/ShadowActivity/URI permission; nessun DocumentsUI di sistema.
- **Non verificato / fragilità:** Il selettore non viene aperto davvero e la persistenza grant è del modello Robolectric; polling Thread.sleep(10) fino a 5 s.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`opensSystemPickerOnceAndDoesNotReopenOnResume`](../app/src/test/java/it/edo/codecprobe/FolderPickerActivityTest.java#L25) | B | Emette un solo Intent ACTION_OPEN_DOCUMENT_TREE, senza flags/extras, anche dopo resume. | Helper `shadowOf(…)`; percorso specificato sopra |
| [`restoresPendingSystemPickerWithoutLaunchingAnother`](../app/src/test/java/it/edo/codecprobe/FolderPickerActivityTest.java#L32) | B | Bundle launched evita nuovo Intent dopo ripristino. | `saved.getBoolean` |
| [`cancellationReturnsToCallerWithoutClaimingAProvidedFolder`](../app/src/test/java/it/edo/codecprobe/FolderPickerActivityTest.java#L37) | B | Cancel termina con RESULT_CANCELED e nessun permesso. | Helper `shadowOf(…)`; percorso specificato sopra |
| [`successfulGrantIsPersistedAndReturnedToCaller`](../app/src/test/java/it/edo/codecprobe/FolderPickerActivityTest.java#L42) | B | Risposta OK con read/write persiste permesso shadow e restituisce URI. | Helper `awaitFinish(…)`, `shadowOf(…)`; percorso specificato sopra |
| [`readOnlyResponseIsReportedAsAnError`](../app/src/test/java/it/edo/codecprobe/FolderPickerActivityTest.java#L50) | B | Risposta readonly non persiste grant e restituisce errore/cancel. | Helper `awaitFinish(…)`, `shadowOf(…)`; percorso specificato sopra |


#### ImageDestinationFlowTest — 1 test

- **Sorgente:** [ImageDestinationFlowTest.java](../app/src/test/java/it/edo/codecprobe/ImageDestinationFlowTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageDestinations/MediaDestinationsController; ImageSelection; ImageActions; ImageQueueStore.
- **Riferimenti produttivi:** [MediaDestinationsController](../app/src/main/java/it/edo/codecprobe/MediaDestinationsController.java), [ImageSelection](../app/src/main/java/it/edo/codecprobe/ImageSelection.java), [ImageActions](../app/src/main/java/it/edo/codecprobe/ImageActions.java), [ImageQueueStore](../app/src/main/java/it/edo/codecprobe/ImageQueueStore.java).
- **Dati, file e simulazioni:** Due piccoli JPEG sintetici ImageWorkspaceTest.fixture; provider directory fittizio/grant shadow; Activity recreate.
- **Non verificato / fragilità:** Solo Intent e queue su disco, non output nelle cartelle; polling5ms/5s. Le foto non attraversano JNI.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`photosUseSameFolderFlowAndFreezeTheCorrectPerFileDestinations`](../app/src/test/java/it/edo/codecprobe/ImageDestinationFlowTest.java#L27) | B | Eccezione seconda cartella sopravvive recreate; START passa solo queueId e qualità65, queue contiene due folders congelate e picker bloccato mentre busy. | `images.screen.convert.isEnabled`, `images.state.inputs.add`, `images.setFolder`, `images.open`, `images.destinations.photoGeneral`, `images.destinations.photoException`, `images.destinations.choosePhotoFolder`, `FolderPickerActivity.class.getName`, `images.destinations.choosing`, `images.destinations.snapshot`, `images.render`, `images.screen.convert.performClick`, `ImageQueueStore.read`, `ImageQueueStore.discard` |


#### ImageOutputFailureTest — 5 test

- **Sorgente:** [ImageOutputFailureTest.java](../app/src/test/java/it/edo/codecprobe/ImageOutputFailureTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageOutput.save; gestione errori e rollback.
- **Riferimenti produttivi:** [ImageOutput](../app/src/main/java/it/edo/codecprobe/ImageOutput.java).
- **Dati, file e simulazioni:** TemporaryFolder; provider Documents specializzato inietta create/write/read/delete failure; veri stream e sentinelle di 16 e 4 byte.
- **Non verificato / fragilità:** Nessun encoder è invocato, nonostante il commento 'no encoder or JNI is replaced'. Non prova provider reale, spazio esaurito a metà stream o crash durante commit.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`missingStagedFileNeverCreatesAnOutputOrDeletesTheExistingName`](../app/src/test/java/it/edo/codecprobe/ImageOutputFailureTest.java#L63) | B | File staged assente non crea output e non elimina sentinella omonima. | `ImageOutput.save` |
| [`revokedCreatePermissionIsReportedWithoutOverwritingAnExistingFile`](../app/src/test/java/it/edo/codecprobe/ImageOutputFailureTest.java#L67) | B | SecurityException create propagata come causa IOException; sorgente e precedente intatti. | Helper `publishFailure(…)`, `assertNoNewDocument(…)`; percorso specificato sopra |
| [`writeFailureRollsBackOnlyTheNewDocument`](../app/src/test/java/it/edo/codecprobe/ImageOutputFailureTest.java#L71) | B | Write negato elimina solo nuovo documento e conserva errore senza suppressed. | Helper `publishFailure(…)`, `assertNoNewDocument(…)`; percorso specificato sopra |
| [`unreadableOutputIsNotDeclaredSavedAndIsRolledBack`](../app/src/test/java/it/edo/codecprobe/ImageOutputFailureTest.java#L75) | B | Rilettura negata non dichiara successo e rimuove output. | Helper `publishFailure(…)`, `assertNoNewDocument(…)`; percorso specificato sopra |
| [`failedRollbackRetainsThePrimaryErrorAndItsDiagnosticCause`](../app/src/test/java/it/edo/codecprobe/ImageOutputFailureTest.java#L79) | B | Write fallita più delete negata: conserva errore primario e suppressed rollback; documento orfano visibile e originali intatti. | Helper `publishFailure(…)`, `assertOriginalsPreserved(…)`; percorso specificato sopra |


#### ImageOutputTest — 5 test

- **Sorgente:** [ImageOutputTest.java](../app/src/test/java/it/edo/codecprobe/ImageOutputTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageOutput.save/hasMotionVideo/name/unique; ImageOutput.Names.
- **Riferimenti produttivi:** [ImageOutput](../app/src/main/java/it/edo/codecprobe/ImageOutput.java).
- **Dati, file e simulazioni:** Provider Documents fittizio e veri file temporanei: pseudo-HEIC contiene solo ftyp16, variante mpvd con quattro byte.
- **Non verificato / fragilità:** Nessuna immagine valida, encoder o JNI. SAF simulato; cancellation è true già all'ingresso, non durante una scrittura lunga.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`savesReReadsAllBytesAndAddsSuffixInsteadOfOverwriting`](../app/src/test/java/it/edo/codecprobe/ImageOutputTest.java#L30) | B | Pubblica e rilegge tutti i byte, collisione Photo produce Photo (2), senza overwrite. | `ImageOutput.save`, `saved.toPath` |
| [`motionNamesKeepMpSuffixAcrossCollisionsAndHeaderScanSkipsPayload`](../app/src/test/java/it/edo/codecprobe/ImageOutputTest.java#L33) | B | Marker mpvd determina suffisso MP e lo conserva sulle collisioni; box che supera EOF respinto. | `ImageOutput.hasMotionVideo`, `ImageOutput.save`, `ImageOutput.name`, `ImageOutput.unique` |
| [`wholeBatchListsDestinationOnceAndRetainsCaseInsensitiveReservations`](../app/src/test/java/it/edo/codecprobe/ImageOutputTest.java#L44) | B | 100 pubblicazioni alternate maiuscolo/minuscolo: listing una volta per Names, un'altra con nuova cache; nessun duplicato case-insensitive. | `ImageOutput.Names`, `ImageOutput.save` |
| [`sameLengthCorruptionRollsBackTheNewDocument`](../app/src/test/java/it/edo/codecprobe/ImageOutputTest.java#L52) | B | Un byte corrotto senza cambiare lunghezza causa errore e rimozione documento. | `ImageOutput.save` |
| [`cancellationPublishesNothing`](../app/src/test/java/it/edo/codecprobe/ImageOutputTest.java#L53) | B | Cancel immediato non pubblica documenti. | `ImageOutput.save` |


#### ImageProgressJournalTest — 7 test

- **Sorgente:** [ImageProgressJournalTest.java](../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageProgressJournal.record/reusable/inspection/failures/onUpgrade; ImageFingerprint; ImageQueueStore; ImageRunState.
- **Riferimenti produttivi:** [ImageProgressJournal](../app/src/main/java/it/edo/codecprobe/ImageProgressJournal.java), [ImageFingerprint](../app/src/main/java/it/edo/codecprobe/ImageFingerprint.java), [ImageQueueStore](../app/src/main/java/it/edo/codecprobe/ImageQueueStore.java), [ImageRunState](../app/src/main/java/it/edo/codecprobe/ImageRunState.java).
- **Dati, file e simulazioni:** Robolectric Android SQLite e ContentResolver file://; veri file 4 byte e DB di8500 record, hash inventati in alcuni risultati. Versione1 SQL costruita nel test.
- **Non verificato / fragilità:** Riutilizzo verifica veri hash ma non decodifica output. Migrazione copre solo1→2 e schema esplicito, non migrazioni future, corruzione/power loss. Dimensione DB non misura RAM.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`reuseChecksBothFileContentsAndSettingsEvenWhenSizeDoesNotChange`](../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java#L23) | B | Stessa lunghezza ma byte sorgente/output mutati, qualità diversa, output mancante o cancel impediscono ALREADY_DONE. | `journal.record`, `journal.reusable` |
| [`latestOutcomeReplacesThePreviousOneAndFailuresCanBeLoadedWithoutDetails`](../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java#L36) | B | Ultimo esito sostituisce FAILED, retry carica URI/cartella senza100k dettagli e resta una riga. | `journal.record`, `ImageRunState.Result`, `journal.failures`, `journal.getReadableDatabase` |
| [`inspectionFailuresRemainRetryableWithoutErasingConversionHistory`](../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java#L46) | B | Inspection fail/CHECKED/CANCELLED mantiene distinta storia conversione e retry; successo metadati non nasconde fallimento conversione. | `ImageRunState.Result`, `journal.inspection`, `journal.failures`, `journal.record`, `journal.getReadableDatabase` |
| [`upgradesVersionOneWithoutLosingExistingRecords`](../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java#L63) | B | Database SQL versione1 con record FAILED viene aperto a2 senza perdere URI. | `journal.failures`, `journal.getReadableDatabase` |
| [`eightThousandFiveHundredRecordsOccupyLessThanTwentyMb`](../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java#L69) | B | 8500 record reali SQLite stanno sotto20MB: test di budget su host, hash/output solo stringhe. | `journal.getWritableDatabase`, `journal.record`, `ImageRunState.Result` |
| [`completeLargeFolderHandoffKeepsAllEntriesAndDraftSurvivesRecreation`](../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java#L80) | B | Bozza bulk8500 resta completa e handoff disk queue mantiene8500 riferimenti; nessuna Activity ricreata malgrado il nome. | `state.inputs.add`, `state.names.add`, `ImageDraftStore.save`, `ImageDraftStore.load`, `ImageQueueStore.write`, `ImageQueueStore.read`, `ImageQueueStore.discard` |
| [`reusedOutputsContributeOnceToCurrentBatchSavings`](../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java#L86) | A | ALREADY_DONE e SAVED manuali contribuiscono una volta ciascuno ai4MB risparmiati. | `run.add`, `ImageRunState.Result` |


#### MediaDestinationsTest — 3 test

- **Sorgente:** [MediaDestinationsTest.java](../app/src/test/java/it/edo/codecprobe/MediaDestinationsTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** DestinationDraft; DestinationPlan; QueueDestinations.read; MediaDestinationsController.
- **Riferimenti produttivi:** [DestinationDraft](../app/src/main/java/it/edo/codecprobe/DestinationDraft.java), [DestinationPlan](../app/src/main/java/it/edo/codecprobe/DestinationPlan.java), [QueueDestinations](../app/src/main/java/it/edo/codecprobe/QueueDestinations.java), [MediaDestinationsController](../app/src/main/java/it/edo/codecprobe/MediaDestinationsController.java).
- **Dati, file e simulazioni:** Directory ContentProvider inventato, grant Robolectric; Harness Host restituisce busy=false e persist/toast vuoti; prefs simulate.
- **Non verificato / fragilità:** Non verifica filesystem o permessi reali. Polling 5 ms/3–5 s; direct field plan e Harness più ristretto dell'app.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`choicesFollowSourceUrisAndOverrideDoesNotEraseThem`](../app/src/test/java/it/edo/codecprobe/MediaDestinationsTest.java#L36) | B | Scelte seguono URI anche riordinando; override globale reversibile, save/load prefs e reset origine. | `draft.sync`, `draft.plan.setFolder`, `draft.plan.setAllFolder`, `draft.plan.effective`, `draft.save` |
| [`serviceBoundaryCopiesPerFileFoldersAndRejectsMissingEntries`](../app/src/test/java/it/edo/codecprobe/MediaDestinationsTest.java#L47) | A | QueueDestinations copia lista folders e rifiuta cardinalità/fallback mancanti; nessun servizio avviato. | `QueueDestinations.read` |
| [`samePanelHidesEmptySelectionVerifiesAccessAndRestoresPendingPicker`](../app/src/test/java/it/edo/codecprobe/MediaDestinationsTest.java#L54) | B | Pannello vuoto nascosto, verifica grant, picker pendente restaurato, applyAll e revoca simulata rendono non ready. | Helper `grant(…)`, `settle(…)`; percorso specificato sopra |


#### OutputCollisionTest — 6 test

- **Sorgente:** [OutputCollisionTest.java](../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeOutputCheck.begin; OutputName.base; dialog rename/skip/cancel; AppUi.
- **Riferimenti produttivi:** [HomeOutputCheck](../app/src/main/java/it/edo/codecprobe/HomeOutputCheck.java), [OutputName](../app/src/main/java/it/edo/codecprobe/OutputName.java), [AppUi](../app/src/main/java/it/edo/codecprobe/AppUi.java).
- **Dati, file e simulazioni:** Provider Directory restituisce un nome già esistente, computed con stessa OutputName.base usata in produzione; Activity/dialog/Intent shadow.
- **Non verificato / fragilità:** Non pubblica file né prova collisione comparsa tra preflight e save. Nome skipRecordsDecisionAndCancelDoesNotLaunch verifica solo Skip; Cancel è in altro test. Due PNG senza golden.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`renameValidatesNameBeforeStartingService`](../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java#L61) | B | Nome già occupato mostra errore e conserva dialog; rinominato emette Intent outputNames rinominato.mp4 non omitted. | `OutputName.base`, `home.activity.getApplication` |
| [`skipRecordsDecisionAndCancelDoesNotLaunch`](../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java#L71) | B | Pulsante Skip marca omitted nel servizio lanciato; non esercita Cancel nonostante nome. | `home.conversion.outputCheck.dialog.getButton`, `home.activity.getApplication` |
| [`backCancelsPreflightWithoutConversion`](../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java#L77) | B | Back/cancel chiude preflight, niente Intent conversione e busy=false. | `home.conversion.outputCheck.dialog.cancel`, `home.activity.getApplication` |
| [`sharedButtonsHaveNoPlatformElevationAnimator`](../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java#L82) | B | Solo bottone Convert controllato senza elevation/stateListAnimator, non tutti sharedButtons. | `home.screen.conversion.convert.getStateListAnimator`, `home.screen.conversion.convert.getElevation` |
| [`renderLightCollisionDialog`](../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java#L95) | B | PNG dialog collisione chiaro, solo compressione. | Helper `renderDialog(…)`; percorso specificato sopra |
| [`renderDarkCollisionDialog`](../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java#L96) | B | PNG dialog collisione scuro, solo compressione. | Helper `renderDialog(…)`; percorso specificato sopra |


#### OutputNamingSafetyTest — 7 test

- **Sorgente:** [OutputNamingSafetyTest.java](../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `fast`.
- **Componenti/metodi:** OutputPlan; OutputName.base/unique.
- **Riferimenti produttivi:** [OutputPlan](../app/src/main/java/it/edo/codecprobe/OutputPlan.java), [OutputName](../app/src/main/java/it/edo/codecprobe/OutputName.java).
- **Dati, file e simulazioni:** Set/array in memoria; locale globale tr-TR ripristinata in finally.
- **Non verificato / fragilità:** Non apre file né prova restrizioni del filesystem/provider. Il test rejectedManualName invoca manual prima di accept: non simula un accept che fallisce.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`emptyDirectoryAndEmptyQueueDoNotInventCollisions`](../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java#L9) | A | Coda zero e directory vuota non inventano collisioni. | `plan.directory`, `plan.conflicts`, `plan.suggestion`, `plan.names.isEmpty` |
| [`reservationsDoNotMutateTheProviderListingOrAnotherDirectory`](../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java#L14) | A | Copia del listing non aliasata, cancellare il Set esterno non perde i nomi; prenotazioni isolate per directory. | `plan.directory`, `plan.accept`, `plan.conflicts`, `plan.suggestion` |
| [`firstAvailableSuffixIsChosenWithoutModifyingReservations`](../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java#L21) | A | Usa il primo buco (3), non modifica il Set occupato e ripete la stessa proposta. | `OutputName.unique` |
| [`collisionComparisonDoesNotDependOnTurkishDefaultLocale`](../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java#L26) | A | INDICE/indice confliggono anche con locale turca. | `plan.directory`, `plan.conflicts`, `plan.suggestion` |
| [`manualUtf8LimitAccepts240BytesAndRejects242Bytes`](../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java#L33) | A | 240 byte UTF-8 accettati, 242 rifiutati. | `OutputPlan.manual` |
| [`generatedNamesKeepWholeUnicodeCodePointsAndRemoveTraversal`](../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java#L38) | A | Sanitizzazione traversal e troncamento non spezzano emoji; round-trip UTF-8 e limite 224 byte. | `OutputName.base` |
| [`rejectedManualNameCannotConsumeAQueueSlot`](../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java#L44) | A | Nome ../clip respinto prima di accept; nessuno slot o prenotazione consumato. | `plan.directory`, `plan.accept`, `OutputPlan.manual`, `plan.names.isEmpty`, `plan.conflicts` |


#### OutputPlanTest — 3 test

- **Sorgente:** [OutputPlanTest.java](../app/src/test/java/it/edo/codecprobe/OutputPlanTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `fast`.
- **Componenti/metodi:** OutputPlan.directory/conflicts/suggestion/accept/skip/manual; OutputName.unique; CompressionSummary.render.
- **Riferimenti produttivi:** [OutputPlan](../app/src/main/java/it/edo/codecprobe/OutputPlan.java), [OutputName](../app/src/main/java/it/edo/codecprobe/OutputName.java), [CompressionSummary](../app/src/main/java/it/edo/codecprobe/CompressionSummary.java).
- **Dati, file e simulazioni:** Set di nomi e coda in memoria; nessun provider o file reale.
- **Non verificato / fragilità:** Non verifica listing SAF, gare con altri writer né pubblicazione; il rifiuto del nome avviene prima di accept.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`existingNamesAndQueueReservationsShareNamespace`](../app/src/test/java/it/edo/codecprobe/OutputPlanTest.java#L7) | A | Stessa cartella: conflitto senza distinzione maiuscole, suffisso 3 e prenotazione; altra cartella indipendente, skip e refresh conservano la prenotazione. | `plan.directory`, `plan.conflicts`, `plan.suggestion`, `plan.accept`, `plan.skip` |
| [`manualNamesKeepMp4AndRejectPathsAndOversizedNames`](../app/src/test/java/it/edo/codecprobe/OutputPlanTest.java#L22) | A | Aggiunge .mp4 e rifiuta vuoto, traversal, controlli e nome oltre 240 byte. | `OutputPlan.manual` |
| [`omittedFilesAreReportedWithoutSavingsOrFailure`](../app/src/test/java/it/edo/codecprobe/OutputPlanTest.java#L29) | A | Voce OMITTED produce testo di omissione, senza percentuale o etichetta di fallimento. | `CompressionSummary.render`, `CompressionSummary.Item` |


#### PhotoReferenceTest — 4 test

- **Sorgente:** [PhotoReferenceTest.java](../app/src/test/java/it/edo/codecprobe/PhotoReferenceTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** DestinationPlan; DestinationDraft; ImageFiles.basename; ImageDestinations.
- **Riferimenti produttivi:** [DestinationPlan](../app/src/main/java/it/edo/codecprobe/DestinationPlan.java), [DestinationDraft](../app/src/main/java/it/edo/codecprobe/DestinationDraft.java), [ImageFiles](../app/src/main/java/it/edo/codecprobe/ImageFiles.java).
- **Dati, file e simulazioni:** Stringhe URI/nome semplici, SharedPreferences/Activity simulate e3riferimenti artificiali.
- **Non verificato / fragilità:** Nessuna destinazione realmente aperta; eccezioni del modello non dimostrano applicazione nel salvataggio.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`generalDestinationNeverOverridesExceptions`](../app/src/test/java/it/edo/codecprobe/PhotoReferenceTest.java#L15) | A | Nuova cartella generale non sovrascrive own; disabilitare eccezione ritorna al generale. | `plan.reset`, `plan.enableExceptions`, `plan.exception`, `plan.setFolder`, `plan.setAllFolder`, `plan.effective`, `plan.exceptionCount` |
| [`draftKeepsExceptionsWithTheirSourceAfterReordering`](../app/src/test/java/it/edo/codecprobe/PhotoReferenceTest.java#L22) | B | Eccezione segue source one dopo persistenza e riordino/nuovo file. | `draft.sync`, `draft.plan.enableExceptions`, `draft.plan.setFolder`, `draft.save` |
| [`documentIdentifiersAreNeverDisplayedAsFullNames`](../app/src/test/java/it/edo/codecprobe/PhotoReferenceTest.java#L28) | A | basename elimina document parent e null ha fallback Foto JPEG; non testo di tutta UI. | `ImageFiles.basename` |
| [`selectingMixedGroupPreservesExistingExceptions`](../app/src/test/java/it/edo/codecprobe/PhotoReferenceTest.java#L31) | B | Gruppo misto0,2 può diventare eccezione senza perdere own già assegnata a0. | `images.state.inputs.clear`, `images.state.names.clear`, `images.state.inputs.addAll`, `images.state.clamp`, `images.setFolder`, `images.state.destinations.plan.setFolder`, `images.toggleSelection`, `images.destinations.photoException`, `images.state.destinations.plan.exceptionCount`, `images.state.destinations.plan.effective` |


### Code e operazioni asincrone


#### BatchRetryTest — 2 test

- **Sorgente:** [BatchRetryTest.java](../app/src/test/java/it/edo/codecprobe/BatchRetryTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** BatchRetry.select/save/load.
- **Riferimenti produttivi:** [BatchRetry](../app/src/main/java/it/edo/codecprobe/BatchRetry.java).
- **Dati, file e simulazioni:** Intent con tre URI e opzioni arbitrarie; SharedPreferences simulate nel secondo test.
- **Non verificato / fragilità:** Non avvia conversione/riprova né verifica tutti gli indici invalidi. Il clone controllato protegge liste specifiche, non ogni extra futuro.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`selectedFailuresKeepTheMatchingDestinationsApprovedNamesAndSettings`](../app/src/test/java/it/edo/codecprobe/BatchRetryTest.java#L24) | A | Sottoselezione 2,0 conserva allineamento nomi/cartelle/origini e parametri; copia difensiva, lista vuota e indice duplicato. | `BatchRetry.select` |
| [`privateStoreSurvivesReloadAndRejectsCorruptData`](../app/src/test/java/it/edo/codecprobe/BatchRetryTest.java#L37) | B | Persistenza privata ricarica retry e opzioni, clear restituisce null e JSON invalido viene respinto. | `BatchRetry.select`, `BatchRetry.save`, `BatchRetry.load` |


#### CancellationConfirmationTest — 4 test

- **Sorgente:** [CancellationConfirmationTest.java](../app/src/test/java/it/edo/codecprobe/CancellationConfirmationTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeConversion conferma/handleCancellationIntent; ConversionService.notification.
- **Riferimenti produttivi:** [HomeConversion](../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [ConversionService](../app/src/main/java/it/edo/codecprobe/ConversionService.java).
- **Dati, file e simulazioni:** hdrTesting=true impostato dal test, nessun HDR; Notification privata invocata via reflection, Intent e dialog shadow.
- **Non verificato / fragilità:** Conferma imposta AtomicBoolean ma non interrompe codec né verifica file rimossi; stale Intent provato con nessuna vera run.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`continueOrDismissDoesNotCancel`](../app/src/test/java/it/edo/codecprobe/CancellationConfirmationTest.java#L29) | B | Continua e dismiss non impostano cancel. | Helper `startFakeHdr(…)`, `shadowOf(…)`; percorso specificato sopra |
| [`onlyPositiveConfirmationCancels`](../app/src/test/java/it/edo/codecprobe/CancellationConfirmationTest.java#L38) | B | Solo positivo imposta cancel e disabilita Stop. | Helper `startFakeHdr(…)`, `shadowOf(…)`; percorso specificato sopra |
| [`notificationOpensConfirmationActivityInsteadOfStoppingService`](../app/src/test/java/it/edo/codecprobe/CancellationConfirmationTest.java#L46) | B | Notifica usa PendingIntent Activity di conferma con startedAt, non stop diretto. | `ConversionService.State`, `ConversionService.class.getDeclaredMethod`, `MainActivity.class.getName` |
| [`staleNotificationDoesNotShowCancelDialog`](../app/src/test/java/it/edo/codecprobe/CancellationConfirmationTest.java#L58) | B | Richiesta cancellazione obsoleta non apre dialog e consuma action. | Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder |


#### ConversionControlBoundaryTest — 3 test

- **Sorgente:** [ConversionControlBoundaryTest.java](../app/src/test/java/it/edo/codecprobe/ConversionControlBoundaryTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `fast`.
- **Componenti/metodi:** ConversionControl.checkpoint/finishFile/cancel/skip.
- **Riferimenti produttivi:** [ConversionControl](../app/src/main/java/it/edo/codecprobe/ConversionControl.java).
- **Dati, file e simulazioni:** Clock costante e worker reale; latch/future/join con deadline 3 s.
- **Non verificato / fragilità:** Non verifica teardown di un servizio Android né cancellazione dentro il codec.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`cancellationBeforeTheFirstFileNeverEntersPauseOrAcceptsSkip`](../app/src/test/java/it/edo/codecprobe/ConversionControlBoundaryTest.java#L9) | A | Annullamento prima del primo file non entra in pausa o accetta skip. | `gate.pause`, `gate.cancel`, `gate.checkpoint`, `gate.finishFile`, `gate.skip` |
| [`completedFileRejectsLateSkipWithoutCancellingTheNextFile`](../app/src/test/java/it/edo/codecprobe/ConversionControlBoundaryTest.java#L13) | A | Skip tardivo sul file già concluso non annulla il successivo. | `gate.skip`, `gate.beginFile`, `gate.finishFile`, `gate.endFile`, `gate.stopping` |
| [`interruptingAPausedWorkerCancelsAndRestoresItsInterruptStatus`](../app/src/test/java/it/edo/codecprobe/ConversionControlBoundaryTest.java#L18) | A | Interrupt del thread in pausa diventa arresto e conserva il flag interrupt; worker non rimane vivo. | `gate.checkpoint`, `gate.beginFile`, `gate.pause`, `gate.cancel` |


#### ConversionControlTest — 4 test

- **Sorgente:** [ConversionControlTest.java](../app/src/test/java/it/edo/codecprobe/ConversionControlTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `fast`.
- **Componenti/metodi:** ConversionControl.beginFile/checkpoint/pause/skip/endFile/finishFile/cancel/pausedMillis.
- **Riferimenti produttivi:** [ConversionControl](../app/src/main/java/it/edo/codecprobe/ConversionControl.java).
- **Dati, file e simulazioni:** Thread/latch/future reali e clock AtomicLong iniettato; timeout 3 s.
- **Non verificato / fragilità:** Non ferma un decoder o watchdog reale: confronta il clock calcolato e l'API del gate. Nessun MediaCodec/JNI.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`pauseWaitsAndExcludesTimeFromDecoderWatchdog`](../app/src/test/java/it/edo/codecprobe/ConversionControlTest.java#L10) | A | Pausa blocca il worker e 120 s simulati sono esclusi dal tempo utile, poi riprende. | `gate.beginFile`, `gate.pause`, `gate.pausedMillis`, `gate.cancel` |
| [`skippingWhilePausedKeepsNextFilePausedAndRejectsOldFileCommands`](../app/src/test/java/it/edo/codecprobe/ConversionControlTest.java#L27) | A | Skip in pausa interrompe solo il file attuale, conserva pausa e rifiuta comando con numero vecchio. | `gate.beginFile`, `gate.pause`, `gate.skip`, `gate.pauseRequested`, `gate.endFile`, `gate.finishFile`, `gate.cancel` |
| [`cancellationWakesPausedWorkerAndCannotCommitAFile`](../app/src/test/java/it/edo/codecprobe/ConversionControlTest.java#L42) | A | Cancel sveglia il worker in pausa e impedisce finishFile. | `gate.beginFile`, `gate.pause`, `gate.cancel`, `gate.skip` |
| [`resumeCommandDoesNotWaitForWorkerAcknowledgementLogging`](../app/src/test/java/it/edo/codecprobe/ConversionControlTest.java#L52) | A | Resume torna anche se la callback di riconoscimento della pausa è deliberatamente bloccata. | `gate.beginFile`, `gate.pause`, `gate.cancel` |


#### ConversionRunGateTest — 1 test

- **Sorgente:** [ConversionRunGateTest.java](../app/src/test/java/it/edo/codecprobe/ConversionRunGateTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `fast`.
- **Componenti/metodi:** ConversionRunGate.acquire/owns/release.
- **Riferimenti produttivi:** [ConversionRunGate](../app/src/main/java/it/edo/codecprobe/ConversionRunGate.java).
- **Dati, file e simulazioni:** Otto worker reali, latch; deadline 3 s e token di sessione.
- **Non verificato / fragilità:** Non avvia servizi/codec o due processi Android. Sensibile al carico macchina per i timeout; una sola schedulazione concorrente.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`concurrentStartsHaveOneOwnerAndOldOwnerCannotReleaseItsSuccessor`](../app/src/test/java/it/edo/codecprobe/ConversionRunGateTest.java#L9) | A | Un solo proprietario su otto avvii, token crescente; un vecchio token non rilascia il successore. | `gate.acquire`, `gate.release`, `gate.owns` |


#### HomePlaybackControlsTest — 1 test

- **Sorgente:** [HomePlaybackControlsTest.java](../app/src/test/java/it/edo/codecprobe/HomePlaybackControlsTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomePlayback/HomeConversion; ConversionService command Intents.
- **Riferimenti produttivi:** [HomeConversion](../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [ConversionService](../app/src/main/java/it/edo/codecprobe/ConversionService.java).
- **Dati, file e simulazioni:** State running/startedAt/batchIndex modificato via reflection; servizio non esegue i comandi.
- **Non verificato / fragilità:** Pause/resume/skip provati solo come Intent e conferma, non loro effetto su encoder o file.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`pauseAndResumeSendSessionCommandsAndSkipRequiresCurrentFileConfirmation`](../app/src/test/java/it/edo/codecprobe/HomePlaybackControlsTest.java#L18) | B | Pause/Resume portano sessione1337; Skip richiede sì sul file corrente e conferma diventata obsoleta non emette Intent. | `ConversionService.snapshot`, `ConversionService.class.getDeclaredField`, `home.conversion.renderConversionDashboard`, `home.screen.conversion.playbackActions.getVisibility`, `home.screen.conversion.pause.performClick`, `home.activity.getApplication`, `home.screen.conversion.pause.getText`, `home.screen.conversion.skip.performClick`, `home.conversion.playback.confirmation.getButton`, `ConversionService.State.class.getDeclaredFields` |


#### HomeSavedSessionTest — 2 test

- **Sorgente:** [HomeSavedSessionTest.java](../app/src/test/java/it/edo/codecprobe/HomeSavedSessionTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeConversion report/SavedSession UI; SavedSession.load/save; BatchRetry.select.
- **Riferimenti produttivi:** [HomeConversion](../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [SavedSession](../app/src/main/java/it/edo/codecprobe/SavedSession.java), [BatchRetry](../app/src/main/java/it/edo/codecprobe/BatchRetry.java).
- **Dati, file e simulazioni:** Due risultati e URI inesistenti preparati; vero journal generato dal sample; ShadowActivity/Bundle.
- **Non verificato / fragilità:** Apri verifica Intent non player o esistenza file; recovery dialog non avvia alcun ripristino effettivo.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`eachSuccessfulReportButtonOpensExactlyItsOwnFile`](../app/src/test/java/it/edo/codecprobe/HomeSavedSessionTest.java#L21) | B | Ogni riga successo apre il proprio URI con read grant, report vuoto pulisce righe. | `ConversionService.State`, `state.items.add`, `CompressionSummary.Item`, `state.outputUris.add`, `state.results.add`, `state.outputUris.get`, `CompressionSummary.render`, `home.conversion.renderSizeReport`, `home.screen.conversion.failureReport.getChildAt`, `home.screen.conversion.failureReport.getChildCount` |
| [`freshActivityShowsRecoveryWithoutStartingAndNewQueueRequiresReplacementConfirmation`](../app/src/test/java/it/edo/codecprobe/HomeSavedSessionTest.java#L40) | B | Activity fresca propone recovery senza partire; nuova coda richiede sostituzione, No conserva journal; retry rimuove extra restore. | `ConversionService.snapshot`, `ConversionService.class.getDeclaredField`, `SavedSession.save`, `ConversionService.State`, `home.screen.conversion.savedSession.getVisibility`, `home.screen.conversion.savedSession.getText`, `home.screen.conversion.sizeReport.getText`, `home.screen.conversion.savedSession.performClick`, `home.conversion.savedSession.dialog.isShowing`, `home.conversion.savedSession.dialog.getButton`, `home.conversion.outputCheck.begin`, `SavedSession.load`, `BatchRetry.select`, `SavedSession.clear` |


#### ImageNotificationTest — 1 test

- **Sorgente:** [ImageNotificationTest.java](../app/src/test/java/it/edo/codecprobe/ImageNotificationTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageConversionService.stage/notification/onStartCommand.
- **Riferimenti produttivi:** [ImageConversionService](../app/src/main/java/it/edo/codecprobe/ImageConversionService.java).
- **Dati, file e simulazioni:** State running iniettato e metodo privato stage via reflection; ShadowLooper/SystemClock/NotificationManager.
- **Non verificato / fragilità:** Tempo virtuale, non schedulazione Android reale né consumo batteria; verifica stessa istanza Notification a1000ms e id30.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`rapidStagesAreCoalescedButPauseCommandPublishesImmediately`](../app/src/test/java/it/edo/codecprobe/ImageNotificationTest.java#L17) | B | 500 stage coalesciati nel testo stage499; successivi attendono1000ms virtuali, PAUSE notifica immediatamente. | `ImageConversionService.class.getDeclaredField`, `ImageConversionService.class.getDeclaredMethod`, `service.getSystemService`, `service.onStartCommand` |


#### ImagePauseTest — 5 test

- **Sorgente:** [ImagePauseTest.java](../app/src/test/java/it/edo/codecprobe/ImagePauseTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageConversionService ciclo/PAUSE/RESUME/CANCEL/onDestroy; ImageQueueStore; ImageLogStore; ImageController.
- **Riferimenti produttivi:** [ImageConversionService](../app/src/main/java/it/edo/codecprobe/ImageConversionService.java), [ImageQueueStore](../app/src/main/java/it/edo/codecprobe/ImageQueueStore.java), [ImageLogStore](../app/src/main/java/it/edo/codecprobe/ImageLogStore.java), [ImageController](../app/src/main/java/it/edo/codecprobe/ImageController.java).
- **Dati, file e simulazioni:** Provider BlockingPhotos blocca query con latch poi openFile sempre fallisce; servizio worker reale nel Robolectric; queue/log su disco.
- **Non verificato / fragilità:** Pausa verificata tra tentativi falliti, mai durante/completando una vera conversione JPEG→HEIC. WakeLock shadow, polling5ms/deadline10s.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`pauseFinishesTheCurrentPhotoAndResumeKeepsQueueAndCounters`](../app/src/test/java/it/edo/codecprobe/ImagePauseTest.java#L44) | B | Pausa termina il primo tentativo fallito e non passa al secondo, conserva queue/counters; Resume vecchio rifiutato, corrente completa3 falliti e pulisce. | `ImageConversionService.snapshot`, `ImageQueueStore.file`, `ImageConversionService.class.getDeclaredMethod`, `log.contains`, `ImageLogStore.snapshot`, `ImageQueueStore.discard` |
| [`cancellingWhilePausedWakesTheWorkerAndDoesNotStartAnotherPhoto`](../app/src/test/java/it/edo/codecprobe/ImagePauseTest.java#L60) | B | Cancel in pausa sveglia worker senza iniziare altra foto e rimuove queue/wake. | `ImageConversionService.snapshot`, `ImageQueueStore.file`, `ImageQueueStore.discard` |
| [`resumeCanWithdrawAPauseBeforeTheCurrentPhotoFinishes`](../app/src/test/java/it/edo/codecprobe/ImagePauseTest.java#L70) | B | Resume anticipato ritira richiesta pausa prima del termine primo tentativo:3 falliti senza evento PAUSA. | `ImageConversionService.snapshot`, `ImageQueueStore.discard` |
| [`destroyingAPausedServiceDoesNotLeaveItsWorkerWaiting`](../app/src/test/java/it/edo/codecprobe/ImagePauseTest.java#L79) | B | onDestroy in pausa sblocca worker e lascia un solo tentativo completato. | `ImageConversionService.snapshot`, `ImageQueueStore.discard` |
| [`screenAndNotificationCommandsAreBoundToTheActiveQueue`](../app/src/test/java/it/edo/codecprobe/ImagePauseTest.java#L87) | B | UI/notifica sessione12345: Intent Pause/Resume, restore etichette e comandi bloccati in cancelling; nessun comando eseguito nel test UI. | `ImageLogStore.reset`, `ImageLogStore.append`, `images.screen.sections.focusCard`, `images.screen.pause.isEnabled`, `ImageOptions.load`, `images.render`, `images.screen.pause.getText`, `images.screen.pause.performClick`, `images.screen.runStatus.getText`, `images.screen.progress.isIndeterminate`, `images.screen.exportLog.isEnabled` |


#### ImageSessionServiceTest — 2 test

- **Sorgente:** [ImageSessionServiceTest.java](../app/src/test/java/it/edo/codecprobe/ImageSessionServiceTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageConversionService.onStartCommand ripristino; ImageSessionStore; ImageProgressJournal; ImageLogStore.
- **Riferimenti produttivi:** [ImageConversionService](../app/src/main/java/it/edo/codecprobe/ImageConversionService.java), [ImageSessionStore](../app/src/main/java/it/edo/codecprobe/ImageSessionStore.java), [ImageProgressJournal](../app/src/main/java/it/edo/codecprobe/ImageProgressJournal.java), [ImageLogStore](../app/src/main/java/it/edo/codecprobe/ImageLogStore.java).
- **Dati, file e simulazioni:** Servizio Robolectric e stati via reflection; due veri file3/2byte, primo risultato già completato e secondo preregistrato con hash reali.
- **Non verificato / fragilità:** Ripresa segue percorso ALREADY_DONE, non converte file rimasto. Nessuna ricreazione processo o crash reale; executor get5s.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`freshServiceResumesAfterCursorPreservesTotalsAndAppendsLog`](../app/src/test/java/it/edo/codecprobe/ImageSessionServiceTest.java#L35) | B | Servizio fresco parte dopo cursore1, riusa secondo output e preserva risparmio201/log precedente; non riapre primo URI e pulisce sessione/coda. | `ImageLogStore.append`, `ImageSessionStore.load`, `ImageConversionService.snapshot`, `ImageSessionStore.exists`, `ImageQueueStore.file`, `log.startsWith`, `log.contains` |
| [`fatalLogFailureKeepsQueueAndPreviousCursorForRetry`](../app/src/test/java/it/edo/codecprobe/ImageSessionServiceTest.java#L43) | B | Log sostituito da directory rende avvio fatale: conserva queue e cursore1 per retry. | `log.delete`, `log.mkdir`, `ImageSessionStore.load`, `ImageConversionService.snapshot`, `ImageQueueStore.file` |


#### ImageSessionStoreTest — 3 test

- **Sorgente:** [ImageSessionStoreTest.java](../app/src/test/java/it/edo/codecprobe/ImageSessionStoreTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageSessionStore.save/load/exists/discard; ImageQueueStore; AtomicFile.
- **Riferimenti produttivi:** [ImageSessionStore](../app/src/main/java/it/edo/codecprobe/ImageSessionStore.java), [ImageQueueStore](../app/src/main/java/it/edo/codecprobe/ImageQueueStore.java).
- **Dati, file e simulazioni:** Vero JSON/checkpoint e file queue125 nel Context host; risultati manuali con dettagli20k; scrittura AtomicFile interrotta chiudendo stream senza finishWrite.
- **Non verificato / fragilità:** Nessun processo ucciso o filesystem Android. Load non verifica media. Budget2kB riguarda checkpoint privo dettagli, non memoria runtime.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`freshLoadKeepsQueueOptionsCursorAndCumulativeTotalsWithoutGrowingWithDetails`](../app/src/test/java/it/edo/codecprobe/ImageSessionStoreTest.java#L24) | C | Reload conserva cursore121, totali/risparmio/elapsed/opzioni e125 riferimenti; risultati dettagliati non persistiti, file<2048byte. | `run.add`, `ImageRunState.Result`, `ImageSessionStore.save`, `ImageSessionStore.load`, `ImageQueueStore.read`, `ImageOptions.from` |
| [`interruptedAtomicWriteKeepsPreviousCheckpointAndExplicitDiscardRemovesQueue`](../app/src/test/java/it/edo/codecprobe/ImageSessionStoreTest.java#L31) | C | Scrittura incompleta recupera checkpoint precedente; discard rimuove sessione e queue. | `ImageSessionStore.save`, `ImageSessionStore.load`, `ImageSessionStore.exists`, `ImageSessionStore.discard`, `ImageQueueStore.file` |
| [`missingQueueIsReportedWithoutSilentlyDroppingTheSession`](../app/src/test/java/it/edo/codecprobe/ImageSessionStoreTest.java#L37) | C | Queue assente produce IOException ma lascia sessione per diagnosi, senza scarto silenzioso. | `ImageSessionStore.save`, `ImageQueueStore.discard`, `ImageSessionStore.load`, `ImageSessionStore.exists` |


#### QueuePlaybackServiceTest — 1 test

- **Sorgente:** [QueuePlaybackServiceTest.java](../app/src/test/java/it/edo/codecprobe/QueuePlaybackServiceTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ConversionService PAUSE/SKIP/RESUME; ConversionControl; BatchRetry; notification.
- **Riferimenti produttivi:** [ConversionService](../app/src/main/java/it/edo/codecprobe/ConversionService.java), [ConversionControl](../app/src/main/java/it/edo/codecprobe/ConversionControl.java), [BatchRetry](../app/src/main/java/it/edo/codecprobe/BatchRetry.java).
- **Dati, file e simulazioni:** Provider directory bloccato con CountDownLatch, poi URI sorgenti assenti; worker reale Robolectric; wake/notifiche shadow e reflection.
- **Non verificato / fragilità:** Nessuna conversione reale. Pausa al confine preflight; secondo input fallisce prima codec. Polling10ms/4s, latch3s e worker5s.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`pauseSkipResumeKeepsNotificationAndExcludesSkippedFileFromRetry`](../app/src/test/java/it/edo/codecprobe/QueuePlaybackServiceTest.java#L44) | B | Token obsoleto respinto; Pause rilascia wake e notifica Riprendi; Skip solo file corrente escluso retry, Resume prosegue al secondo che fallisce. | `ConversionService.snapshot`, `ConversionService.retryRequest`, `BatchRetry.select`, `service.onStartCommand`, `ConversionService.class.getDeclaredField`, `ConversionService.class.getDeclaredMethod`, `BatchRetry.load`, `ConversionService.State.class.getDeclaredFields`, `ConversionService.rememberFailures` |


#### RecoveryLifecycleTest — 2 test

- **Sorgente:** [RecoveryLifecycleTest.java](../app/src/test/java/it/edo/codecprobe/RecoveryLifecycleTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeSavedState; HomeSystem timer; ConversionService; ConversionSessionState; SavedSession; ConversionRunGate.
- **Riferimenti produttivi:** [HomeSavedState](../app/src/main/java/it/edo/codecprobe/HomeSavedState.java), [ConversionService](../app/src/main/java/it/edo/codecprobe/ConversionService.java), [ConversionSessionState](../app/src/main/java/it/edo/codecprobe/ConversionSessionState.java), [SavedSession](../app/src/main/java/it/edo/codecprobe/SavedSession.java), [ConversionRunGate](../app/src/main/java/it/edo/codecprobe/ConversionRunGate.java).
- **Dati, file e simulazioni:** Bundle 'another-process', journal sample, SystemClock virtuale; provider directory bloccato/poi fail; due istanze servizio simulato.
- **Non verificato / fragilità:** Process death simulata tramite stringa/reset static, non processo Android terminato. Lease protegge worker senza codec; reflection e timeout2–5s.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`restoredBundleStartsEmptyWithoutMetadataScanAndBothTimersUseAccumulatedTime`](../app/src/test/java/it/edo/codecprobe/RecoveryLifecycleTest.java#L26) | B | Bundle da processo diverso non ripristina selezione né avvia scan; timer mantiene45s, poi50s run e50s finita anche dopo2h virtuali. | `ConversionService.snapshot`, `SavedSession.save`, `ConversionService.State`, `home.state.selectedInputs.isEmpty`, `home.screen.conversion.sizeReport.getText`, `home.handler.removeCallbacks`, `home.conversion.renderConversionDashboard`, `home.screen.conversion.elapsedLabel.getText`, `home.system.sample.run`, `SavedSession.clear` |
| [`destroyedWorkerRetainsLeaseUntilCleanupAndRepeatedStartsCannotOverlap`](../app/src/test/java/it/edo/codecprobe/RecoveryLifecycleTest.java#L49) | B | Servizio distrutto mantiene lease fino cleanup; START ripetuti/altro servizio non sovrapposti; metriche vecchie azzerate, resume successivo fallisce controllato e logga risorse. | `ConversionService.snapshot`, `BatchRetry.select`, `SavedSession.save`, `ConversionService.State`, `SavedSession.load`, `ConversionService.batchLogFile`, `log.contains`, `SavedSession.clear` |


#### RecoveryPresentationTest — 2 test

- **Sorgente:** [RecoveryPresentationTest.java](../app/src/test/java/it/edo/codecprobe/RecoveryPresentationTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeConversion recovery/dashboard; HomeSavedSession; HomeSelection; HomeSavedState; VideoMetadataCache.
- **Riferimenti produttivi:** [HomeConversion](../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [HomeSavedSession](../app/src/main/java/it/edo/codecprobe/HomeSavedSession.java), [HomeSelection](../app/src/main/java/it/edo/codecprobe/HomeSelection.java), [HomeSavedState](../app/src/main/java/it/edo/codecprobe/HomeSavedState.java), [VideoMetadataCache](../app/src/main/java/it/edo/codecprobe/VideoMetadataCache.java).
- **Dati, file e simulazioni:** Journal sample/stato iniettati; worker bloccato con latch; nessun provider video valido, cache può contenere report d'errore.
- **Non verificato / fragilità:** Non esegue il servizio conversione lanciato né metadati video validi. 'One reader across recreation' controlla generation/loading/cache, non conta aperture effettive.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`pendingRecoveryHidesFileMetricsWithoutLosingReportOrJournal`](../app/src/test/java/it/edo/codecprobe/RecoveryPresentationTest.java#L21) | B | Recovery in attesa nasconde vecchi frame/opzioni ma mantiene report45s e journal intatto; render attivo torna7%. | `ConversionService.snapshot`, `SavedSession.save`, `ConversionService.State`, `home.state.selectedInputs.isEmpty`, `home.screen.conversion.percentLabel.getText`, `home.screen.conversion.progress.getProgress`, `home.screen.conversion.frameLabel.getText`, `home.screen.conversion.timeline.getText`, `home.screen.conversion.activeCpu.getText`, `home.screen.conversion.activeCrf.getText`, `home.screen.conversion.activeBframes.getText`, `home.screen.conversion.activeLookahead.getText`, `home.screen.conversion.currentFile.getText`, `home.screen.conversion.elapsedLabel.getText`, `home.screen.conversion.sizeReport.getText`, `SavedSession.load`, `home.conversion.renderConversionDashboard`, `SavedSession.clear` |
| [`explicitResumeRestoresNamesImmediatelyAndUsesOneBackgroundReaderAcrossRecreation`](../app/src/test/java/it/edo/codecprobe/RecoveryPresentationTest.java#L42) | B | Resume esplicito ripristina subito URI/nomi/cartelle, background senza dialog/generation duplicata; recreate conserva sessione, Bundle processo morto ripulito. | `ConversionService.snapshot`, `SavedSession.save`, `ConversionService.State`, `VideoMetadataCache.clear`, `home.state.selectedInputs.isEmpty`, `home.selection.worker.submit`, `SavedSession.load`, `home.conversion.launchPrepared`, `home.state.destinationPlan.individualFolders`, `home.state.destinationPlan.individualOrigins`, `home.screen.files.videoChips.size`, `home.screen.conversion.savedSession.isEnabled`, `home.selection.inspectInput`, `home.screen.files.videoChips.get`, `home.state.selectedInputs.size`, `VideoMetadataCache.size`, `HomeSavedState.inThisProcess`, `SavedSession.clear` |


#### ResumeParametersTest — 2 test

- **Sorgente:** [ResumeParametersTest.java](../app/src/test/java/it/edo/codecprobe/ResumeParametersTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ResumeParameters.choose; ConversionSessionState.begin; SavedSession; HomeSavedSession.
- **Riferimenti produttivi:** [ResumeParameters](../app/src/main/java/it/edo/codecprobe/ResumeParameters.java), [ConversionSessionState](../app/src/main/java/it/edo/codecprobe/ConversionSessionState.java), [SavedSession](../app/src/main/java/it/edo/codecprobe/SavedSession.java), [HomeSavedSession](../app/src/main/java/it/edo/codecprobe/HomeSavedSession.java).
- **Dati, file e simulazioni:** Intent/sample in memoria e vero round-trip journal nel primo; secondo Activity/permission shadow e pendingResume/Bundle.
- **Non verificato / fragilità:** Nessun encoder applica parametri; primo modifica solo CPU/CRF/preset/temporal e preserva onlySmaller/naming, non test generico tutte opzioni.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`currentChoiceChangesOnlyTabTwoAndPersistsWithRecoveredReport`](../app/src/test/java/it/edo/codecprobe/ResumeParametersTest.java#L19) | C | Scelta corrente modifica parametri tab2, conserva elenco/destinazioni/report/elapsed e sopravvive save/load senza mutare request originale. | `ResumeParameters.choose`, `ConversionSessionState.begin`, `SavedSession.save`, `SavedSession.load` |
| [`popupChoiceSurvivesRecreationAndPermissionDenialLeavesDiskUnchanged`](../app/src/test/java/it/edo/codecprobe/ResumeParametersTest.java#L46) | B | Popup sceglie CPU0 congelato attraverso recreate; diniego permesso scarta pendingResume e lascia discoCPU2 senza avvio. | `ConversionService.class.getDeclaredField`, `ConversionService.snapshot`, `SavedSession.save`, `ConversionService.State`, `home.screen.compression.crfSlider.setProgress`, `home.screen.compression.presetSlider.setProgress`, `home.screen.compression.cpuMode.setSelection`, `home.conversion.savedSession.confirm`, `saved.isChecked`, `saved.getText`, `home.conversion.savedSession.pendingResume.getIntExtra`, `SavedSession.load`, `SavedSession.clear` |


#### SavedSessionServiceTest — 1 test

- **Sorgente:** [SavedSessionServiceTest.java](../app/src/test/java/it/edo/codecprobe/SavedSessionServiceTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ConversionService.restoreSession/onStartCommand SAVE_STOP; SavedSession; ConversionSessionState.
- **Riferimenti produttivi:** [ConversionService](../app/src/main/java/it/edo/codecprobe/ConversionService.java), [SavedSession](../app/src/main/java/it/edo/codecprobe/SavedSession.java), [ConversionSessionState](../app/src/main/java/it/edo/codecprobe/ConversionSessionState.java).
- **Dati, file e simulazioni:** Provider Directory bloccante/poi querynull, State e metriche encoder preparati a mano. Shadow custom AndroidAtomicFile.finishWrite aggiunge replace atomic sul Windows host.
- **Non verificato / fragilità:** 'FreshProcess' è reset state + nuova istanza servizio, nessun vero processo. Si ferma prima codec; custom shadow corregge semantica Windows e non prova atomicità Android reale.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`durableStopAndFreshProcessRecoverySkipCompletedFileAndKeepWholeReport`](../app/src/test/java/it/edo/codecprobe/SavedSessionServiceTest.java#L56) | B | SAVE_STOP token giusto conserva sessione/cursore/report e ripulisce metriche; restart salta primo completato, secondo fallisce, preserva1 successo/1fallimento e poi clear elimina journal. | `BatchRetry.select`, `ConversionService.snapshot`, `SavedSession.save`, `ConversionService.State`, `ConversionService.restoreSession`, `service.onStartCommand`, `SavedSession.load`, `ConversionService.class.getDeclaredField`, `ConversionService.batchLogFile`, `log.split`, `log.contains`, `ConversionService.clearSessionIfIdle`, `SavedSession.clear` |


#### SavedSessionTest — 3 test

- **Sorgente:** [SavedSessionTest.java](../app/src/test/java/it/edo/codecprobe/SavedSessionTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** SavedSession.save/load/resumeRequest/clear; AtomicFile; CompressionSummary.
- **Riferimenti produttivi:** [SavedSession](../app/src/main/java/it/edo/codecprobe/SavedSession.java), [CompressionSummary](../app/src/main/java/it/edo/codecprobe/CompressionSummary.java).
- **Dati, file e simulazioni:** Journal JSON reale da sample manuale di3file, risultati/output URI inesistenti; Context solo directory e Intent dati.
- **Non verificato / fragilità:** Non riapre media, non verifica permessi né kill/power loss. Secondo usa failWrite esplicito (rollback ordinato), non crash improvviso.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`roundTripPreservesReportErrorsProgressOptionsAndPerFileUris`](../app/src/test/java/it/edo/codecprobe/SavedSessionTest.java#L33) | C | Round-trip conserva report/errori/frame/tempo/opzioni/URI e resume finishedFiles; clear rimuove sessione. | `SavedSession.save`, `SavedSession.load`, `saved.resumeRequest`, `SavedSession.clear` |
| [`interruptedAtomicWriteKeepsPreviousSessionAndCompletedQueueOnlyRestoresReport`](../app/src/test/java/it/edo/codecprobe/SavedSessionTest.java#L48) | C | AtomicFile.failWrite mantiene checkpoint precedente; coda finita resta solo report e conteggi NO_SAVING. | `CompressionSummary.Item`, `SavedSession.save`, `SavedSession.load`, `saved.hasPending`, `saved.state.sizeReport.contains` |
| [`invalidSessionIsReportedAndPreservedForDiagnosis`](../app/src/test/java/it/edo/codecprobe/SavedSessionTest.java#L58) | C | JSON invalido produce IOException e resta su disco per diagnosi. | `SavedSession.load` |


### Metadati


#### AnalysisDetailReaderTest — 8 test

- **Sorgente:** [AnalysisDetailReaderTest.java](../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** AnalysisReader; ExifAnalysisParser.read; AnalysisDetailReader.read.
- **Riferimenti produttivi:** [AnalysisReader](../app/src/main/java/it/edo/codecprobe/AnalysisReader.java), [ExifAnalysisParser](../app/src/main/java/it/edo/codecprobe/ExifAnalysisParser.java), [AnalysisDetailReader](../app/src/main/java/it/edo/codecprobe/AnalysisDetailReader.java).
- **Dati, file e simulazioni:** Byte TIFF/box/JPEG sintetici su veri file temporanei, RandomAccessFile e FileChannel.
- **Non verificato / fragilità:** Parsing e letture reali, ma niente immagine valida decodificata, GUI o file di vendor completi; testi descrittivi verificati per substring.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`littleAndBigEndianAreExplainedUsingActualBytes`](../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java#L24) | C | Byte reali little/big endian: tag, SHORT, count, valore orientamento e sua alternativa selezionata. | `AnalysisDetailReader.read` |
| [`everyOrientationHasAConcreteMeaningAndUnknownValuesAreNotGuessed`](../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java#L33) | C | Orientamenti 1–8 noti; 99 esplicitamente ignoto. | `AnalysisDetailReader.read` |
| [`externalAsciiFollowsTheOffsetAndPreservesItsTagIdentity`](../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java#L37) | C | ASCII esterno SAMSUNG segue offset 26 e mantiene identità di tag/valore. | `AnalysisDetailReader.read` |
| [`changedDirectoryHeaderIsRejectedInsteadOfShowingCachedClaims`](../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java#L45) | C | Dopo parse, modifica del tag su disco: la rilettura rifiuta la descrizione obsoleta. | `AnalysisDetailReader.read` |
| [`boxAnatomySeparatesAsciiIdAndBigEndianLength`](../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java#L51) | C | Box free: lunghezza big endian, ID ASCII e anteprima limitata 64 su 600 byte. | `AnalysisDetailReader.read` |
| [`jpegLengthExcludesMarkerAndIncludesLengthField`](../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java#L57) | C | Lunghezza segmento JPEG esclude marker e include campo lunghezza. | `AnalysisDetailReader.read` |
| [`unknownTiffTypeDoesNotInventAnExternalDataSize`](../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java#L63) | C | Tipo TIFF 99 non inventa dimensione del valore esterno. | `AnalysisDetailReader.read` |
| [`truncatedSelectedRangeIsRejected`](../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java#L67) | C | Range selezionato oltre EOF solleva IOException. | `AnalysisDetailReader.read` |


#### AnalysisLabelsTest — 5 test

- **Sorgente:** [AnalysisLabelsTest.java](../app/src/test/java/it/edo/codecprobe/AnalysisLabelsTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** AnalysisLabels.of/facts; AnalysisParser.leaf.
- **Riferimenti produttivi:** [AnalysisLabels](../app/src/main/java/it/edo/codecprobe/AnalysisLabels.java), [AnalysisParser](../app/src/main/java/it/edo/codecprobe/AnalysisParser.java).
- **Dati, file e simulazioni:** AnalysisNode costruiti manualmente, comprese descrizioni TIFF testuali già formattate.
- **Non verificato / fragilità:** Non verifica che un parser generi quelle descrizioni né che i pixel o il file sottostante esistano. Accoppiamento a stringhe/etichette.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`referencesAreNotDescribedAsImagePayloads`](../app/src/test/java/it/edo/codecprobe/AnalysisLabelsTest.java#L8) | A | thmb è collegamento non pixel; meta contenitore e mdat dati. | Helper `label(…)`; percorso specificato sopra |
| [`technicalIdentifiersHaveReadableNamesWithoutInventingUnknownContent`](../app/src/test/java/it/edo/codecprobe/AnalysisLabelsTest.java#L12) | A | Etichette per ftyp/zzzz/scansione/EXIF senza inventare blocchi sconosciuti. | Helper `label(…)`; percorso specificato sopra |
| [`exifFactsExplainTypeAndOrientationWithoutChangingRawValue`](../app/src/test/java/it/edo/codecprobe/AnalysisLabelsTest.java#L17) | A | Interpretazione orientamento 6 da descrizione SHORT senza cambiare il valore grezzo. | `AnalysisParser.leaf`, `AnalysisLabels.facts` |
| [`malformedOrTextOrientationIsNotInterpretedAsANumber`](../app/src/test/java/it/edo/codecprobe/AnalysisLabelsTest.java#L21) | A | Non interpreta tipo TIFF errato, ASCII o orientamento fuori intervallo. | `AnalysisLabels.facts`, `AnalysisParser.leaf` |
| [`exifTagAndExternalValueKeepTheirMeaning`](../app/src/test/java/it/edo/codecprobe/AnalysisLabelsTest.java#L26) | A | Nomi dei tag e dei valori esterni conservano il significato di directory. | Helper `label(…)`; percorso specificato sopra |


#### AnalysisParserTest — 1 test

- **Sorgente:** [AnalysisParserTest.java](../app/src/test/java/it/edo/codecprobe/AnalysisParserTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** FileAnalysisChecks.run; AnalysisParser.root/children/hex; AnalysisReader.bytes.
- **Riferimenti produttivi:** [AnalysisParser](../app/src/main/java/it/edo/codecprobe/AnalysisParser.java), [AnalysisReader](../app/src/main/java/it/edo/codecprobe/AnalysisReader.java).
- **Dati, file e simulazioni:** Helper reale con JPEG/HEIC/FLAC/MP3/TIFF sintetici, file sparso logico 5 GiB, IFD ciclico e interrupt.
- **Non verificato / fragilità:** Un solo caso aggrega molte asserzioni: individua la prima rottura, non attribuisce a ogni formato un risultato distinto. File sparso non equivale a leggere 5 GiB né a decodificare media.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`fileStructuresAndBoundedReads`](../app/src/test/java/it/edo/codecprobe/AnalysisParserTest.java#L5) | C | Range contigui e completi, JPEG stuffing/restart, nested box, mdat opaco, seek 64 bit, formati troncati, cancel, EXIF/miniatura e ciclo IFD respinto. | Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder |


#### AnalysisWorkspaceTest — 5 test

- **Sorgente:** [AnalysisWorkspaceTest.java](../app/src/test/java/it/edo/codecprobe/AnalysisWorkspaceTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** AnalysisActivity/Controller/Screen; AnalysisSources; AnalysisParser/DetailReader.
- **Riferimenti produttivi:** [AnalysisActivity](../app/src/main/java/it/edo/codecprobe/AnalysisActivity.java), [AnalysisSources](../app/src/main/java/it/edo/codecprobe/AnalysisSources.java), [AnalysisParser](../app/src/main/java/it/edo/codecprobe/AnalysisParser.java).
- **Dati, file e simulazioni:** Robolectric, URI mancanti e piccoli veri file MP4/JPEG sintetici con TIFF orientamento; worker reale, polling 10 ms fino a 5 s.
- **Non verificato / fragilità:** Navigazione childAt e testi precisi fragile; nessun file multimediale decodificabile, vero provider SAF o kill del processo.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`selectionListUsesReferencesWithoutOpeningMissingFiles`](../app/src/test/java/it/edo/codecprobe/AnalysisWorkspaceTest.java#L27) | B | Combina riferimenti Audio/Foto/Video senza aprire URI inesistenti. | `audio.inputs.add`, `audio.names.add`, `AudioDraftStore.save`, `images.inputs.add`, `images.names.add`, `ImageOptions.load`, `ImageDraftStore.save`, `AnalysisSources.video`, `AnalysisSources.list` |
| [`blockNavigationHexAndRecreationKeepSourceUnchanged`](../app/src/test/java/it/edo/codecprobe/AnalysisWorkspaceTest.java#L33) | B | Naviga box e hex paginato di MP4 sintetico, ricrea Activity e verifica file sorgente byte-identico. | `AnalysisSources.video` |
| [`exifValueOpensItsExplanationAtTheTopOfAPopup`](../app/src/test/java/it/edo/codecprobe/AnalysisWorkspaceTest.java#L45) | B | JPEG con EXIF6 porta a popup con spiegazione e anatomia; dismiss/reopen coerenti. | `AnalysisSources.video` |
| [`missingSourceIsReadableErrorAndAllowsReturn`](../app/src/test/java/it/edo/codecprobe/AnalysisWorkspaceTest.java#L58) | B | File assente mostra errore leggibile e permette ritorno alla lista. | `AnalysisSources.video` |
| [`fourthTabPublishesCurrentVideoSelectionBeforeOpening`](../app/src/test/java/it/edo/codecprobe/AnalysisWorkspaceTest.java#L61) | B | Tab Analisi pubblica selezione corrente Video e apre AnalysisActivity via Intent. | `state.selectedInputs.add`, `state.selectedNames.add`, `home.screen.navigation.analysis.performClick`, `AnalysisActivity.class.getName`, `AnalysisSources.list` |


#### CreationDateTest — 3 test

- **Sorgente:** [CreationDateTest.java](../app/src/test/java/it/edo/codecprobe/CreationDateTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** CreationDatePolicy; Mp4Metadata.creationTime/copy.
- **Riferimenti produttivi:** [CreationDatePolicy](../app/src/main/java/it/edo/codecprobe/CreationDatePolicy.java), [Mp4Metadata](../app/src/main/java/it/edo/codecprobe/Mp4Metadata.java).
- **Dati, file e simulazioni:** MP4 sintetici con mvhd/tkhd/mdhd v0/v1; veri file temporanei e FileChannel negli ultimi due test.
- **Non verificato / fragilità:** Nessun MP4 riproducibile/encoder o indice MediaStore reale; il fallback Android è un numero fornito al metodo.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`onlyMissingAndZero1904DatesUseAndroidFallback`](../app/src/test/java/it/edo/codecprobe/CreationDateTest.java#L13) | A | Policy su zero/1904/valore storico/ignoto, epoca 1904 e overflow. | `CreationDatePolicy.needsFallback`, `CreationDatePolicy.fromAddedSeconds`, `CreationDatePolicy.displayMp4` |
| [`roundTripFillsOnlyMissingCreationDatesAndPreservesMediaAndOtherMetadata`](../app/src/test/java/it/edo/codecprobe/CreationDateTest.java#L52) | C | Copia date mancanti nelle tre intestazioni, preserva date esistenti/modifica, 36 byte iniziali media e titolo. | `CreationDatePolicy.fromAddedSeconds`, `Mp4Metadata.creationTime`, `Mp4Metadata.copy` |
| [`absentAndroidDateLeavesZeroUntouched`](../app/src/test/java/it/edo/codecprobe/CreationDateTest.java#L73) | C | Se il fallback fornito è zero, la creazione mancante rimane zero. | `Mp4Metadata.copy` |


#### ExifDictionaryTest — 8 test

- **Sorgente:** [ExifDictionaryTest.java](../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** ExifTagDictionary; ExifAnalysisParser.read; AnalysisDetailReader.read; AnalysisLabels.of.
- **Riferimenti produttivi:** [ExifTagDictionary](../app/src/main/java/it/edo/codecprobe/ExifTagDictionary.java), [ExifAnalysisParser](../app/src/main/java/it/edo/codecprobe/ExifAnalysisParser.java), [AnalysisDetailReader](../app/src/main/java/it/edo/codecprobe/AnalysisDetailReader.java), [AnalysisLabels](../app/src/main/java/it/edo/codecprobe/AnalysisLabels.java).
- **Dati, file e simulazioni:** TIFF sintetici little/big endian scritti dal helper AnalysisDetailReaderTest.with; snapshot test resources exif-standard-ids.txt.
- **Non verificato / fragilità:** Non certifica l'intero standard EXIF né immagini di fotocamere reali. size()==165 e stringhe italiane sono sensibili ad estensioni/traduzioni; lo snapshot è interno al repo.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`standardCoverageAndDirectoryNamespacesAreExplicit`](../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java#L30) | A | Numero fisso di 165 voci, namespace GPS/Interop/IFD0 e significati presenti. | `ExifTagDictionary.size`, `ExifTagDictionary.find` |
| [`everyIdInTheReferenceSnapshotHasAnItalianExplanation`](../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java#L41) | C | Ogni ID dello snapshot interno ha nome tecnico e descrizione italiana sufficientemente lunga. | `reader.readLine`, `ExifTagDictionary.find` |
| [`sameNumericIdGetsTheMeaningOfItsOwnDirectory`](../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java#L50) | C | Stesso ID interpretato secondo directory; tag fuori namespace non indovinato. | `AnalysisLabels.of` |
| [`exposureAndSignedEvHaveUnitsAndZeroDenominatorsStayVisible`](../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java#L55) | C | Rational e signed EV con unità, endianness, denominatore zero e conversione APEX. | Helper `single(…)`, `fractions(…)`; percorso specificato sopra |
| [`accelerationUsesMilligalRatherThanTreatingRawValueAsSiUnits`](../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java#L63) | C | Accelerazione mGal convertita in m/s² con fixture 100000. | Helper `single(…)`, `fractions(…)`; percorso specificato sopra |
| [`enumValuesAreHighlightedButWrongSchemasAndUnknownCodesAreNotGuessed`](../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java#L66) | C | Enum noto evidenziato; codice/tipo/count/terminatore ASCII invalidi non inventano interpretazioni. | Helper `single(…)`, `shorts(…)`; percorso specificato sopra |
| [`flashBitsAndCoordinatesDescribeActualComponents`](../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java#L72) | C | Bit flash, coordinate pixel e GPS DMS descrivono componenti effettivi del payload. | Helper `single(…)`, `shorts(…)`, `fractions(…)`; percorso specificato sopra |
| [`knownUndefinedLayoutsAreDecodedAndOpaquePayloadsRemainExplicit`](../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java#L78) | C | Layout UNDEFINED noti decodificati; payload non noto esplicitamente opaco. | Helper `single(…)`, `shorts(…)`; percorso specificato sopra |


#### ImageMetadataCheckTest — 4 test

- **Sorgente:** [ImageMetadataCheckTest.java](../app/src/test/java/it/edo/codecprobe/ImageMetadataCheckTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageQueueStore.writeInspection; ImageActions/Selection; ImageConversionService metadataOnly; ImageProgressJournal; ImageRunState.
- **Riferimenti produttivi:** [ImageQueueStore](../app/src/main/java/it/edo/codecprobe/ImageQueueStore.java), [ImageActions](../app/src/main/java/it/edo/codecprobe/ImageActions.java), [ImageConversionService](../app/src/main/java/it/edo/codecprobe/ImageConversionService.java), [ImageProgressJournal](../app/src/main/java/it/edo/codecprobe/ImageProgressJournal.java), [ImageRunState](../app/src/main/java/it/edo/codecprobe/ImageRunState.java).
- **Dati, file e simulazioni:** 8500 URI ripetuti o inesistenti; BlockingPhotos restituisce FileNotFoundException; run CHECKED creata a mano e database nel runtime Robolectric.
- **Non verificato / fragilità:** Non esamina metadati di una foto valida. Tutte le vere elaborazioni del servizio falliscono prima di ispezione nativa; CHECKED8500 non prodotti da inspect. Polling5ms fino10s.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`inspectionQueueNeedsNoDestinationAndKeepsLargeSelection`](../app/src/test/java/it/edo/codecprobe/ImageMetadataCheckTest.java#L30) | C | Queue inspection8500 permette folder vuota; normale conversione la rifiuta. | `ImageQueueStore.writeInspection`, `ImageQueueStore.read`, `ImageQueueStore.discard`, `ImageQueueStore.write` |
| [`compatibilityButtonStartsWithoutFolderAndUsesDiskQueue`](../app/src/test/java/it/edo/codecprobe/ImageMetadataCheckTest.java#L37) | B | Pulsante verifica genera Intent metadataOnly con disk queue anche senza destinazione. | `images.state.inputs.clear`, `images.state.names.clear`, `images.state.inputs.add`, `images.state.names.add`, `images.state.options.withFolder`, `images.render`, `images.screen.convert.isEnabled`, `images.screen.checkMetadata.isEnabled`, `images.screen.checkMetadata.performClick`, `ImageQueueStore.read`, `ImageQueueStore.discard` |
| [`checkPausesResumesAndKeepsRetryableProblemsSeparateFromConversionOutcomes`](../app/src/test/java/it/edo/codecprobe/ImageMetadataCheckTest.java#L46) | B | Due foto non apribili: pausa/riprendi conserva errori retryabili nel journal, nessun progresso conversione e nessun temporaneo residuo. | `ImageQueueStore.writeInspection`, `ImageOptions.load`, `ImageConversionService.snapshot`, `ImageLogStore.snapshot`, `ImageQueueStore.file`, `ImageQueueStore.discard` |
| [`checkedCountersStaySeparateAndSurviveBoundedResultWindow`](../app/src/test/java/it/edo/codecprobe/ImageMetadataCheckTest.java#L61) | B | 8500 CHECKED artificiali mantengono conteggio separato/SAVED0 e100 dettagli; dashboard li presenta come Compatibili, non Riuscite. | `state.add`, `ImageRunState.Result`, `images.screen.sections.focusCard`, `images.screen.dashboard.summary.getText` |


#### InputCreationDateTest — 1 test

- **Sorgente:** [InputCreationDateTest.java](../app/src/test/java/it/edo/codecprobe/InputCreationDateTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** InputCreationDate.fromAndroid; CreationDatePolicy.
- **Riferimenti produttivi:** [InputCreationDate](../app/src/main/java/it/edo/codecprobe/InputCreationDate.java), [CreationDatePolicy](../app/src/main/java/it/edo/codecprobe/CreationDatePolicy.java).
- **Dati, file e simulazioni:** Dates ContentProvider restituisce DATE_ADDED1700000000 o null e vieta scritture; URI media ecloud sintetici.
- **Non verificato / fragilità:** Nessun MediaStore reale, OEM o modifica timestamp video. Asserisce log e query specifica, non data finale galleria.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`readsOriginalDateAddedInSecondsOnlyWhenInternalDateMissing`](../app/src/test/java/it/edo/codecprobe/InputCreationDateTest.java#L29) | B | DATE_ADDED in secondi convertito epocaMP4 solo se data interna manca/sentinella; data valida evita query, null/cloud danno0. | `InputCreationDate.fromAndroid`, `log.stream` |


#### Mp4MetadataDiagnosticsTest — 1 test

- **Sorgente:** [Mp4MetadataDiagnosticsTest.java](../app/src/test/java/it/edo/codecprobe/Mp4MetadataDiagnosticsTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** Mp4Metadata.check.
- **Riferimenti produttivi:** [Mp4Metadata](../app/src/main/java/it/edo/codecprobe/Mp4Metadata.java).
- **Dati, file e simulazioni:** File reali con soli box ftyp/mdat/moov vuoti.
- **Non verificato / fragilità:** Verifica rifiuto/diagnostica strutturale, non correttezza di un moov completo o riproducibilità.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`missingAndDuplicateMoovHaveCountsSizeAndRootLayout`](../app/src/test/java/it/edo/codecprobe/Mp4MetadataDiagnosticsTest.java#L17) | C | moov mancante o duplicato segnala numero, dimensione file e layout/offset dei box. | Helper `rejected(…)`; percorso specificato sopra |


#### Mp4MetadataInventoryTest — 8 test

- **Sorgente:** [Mp4MetadataInventoryTest.java](../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** Mp4MetadataInventory.write/digest; SamsungSef fixture.
- **Riferimenti produttivi:** [Mp4MetadataInventory](../app/src/main/java/it/edo/codecprobe/Mp4MetadataInventory.java), [SamsungSef](../app/src/main/java/it/edo/codecprobe/SamsungSef.java).
- **Dati, file e simulazioni:** Veri file temporanei con box generati, payload opaco, SEF sintetico, Random a seed fisso e file logico 64 MiB.
- **Non verificato / fragilità:** Nessun video realmente decodificato. Il digest atteso talvolta usa lo stesso metodo del codice sotto test: non è un oracolo indipendente del SHA.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`datesVersionZeroAndOneAndNestedTextAreReadable`](../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java#L21) | C | Date mvhd v0/v1 e testo QuickTime UTF-8 esposti; payload mdat non esportato. | Helper `join(…)`, `box(…)`, `report(…)`, `date(…)`; percorso specificato sopra |
| [`opaqueMetadataHashSurvivesRelocationAndPreviewIsBounded`](../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java#L31) | C | Hash opaco invariato dopo spostamento e anteprima 256 byte; digest non muove il ByteBuffer. | `Mp4MetadataInventory.digest` |
| [`sparseMediaIsSkippedAndMalformedOrCancelledReportsFailClearly`](../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java#L38) | C | mdat 64 MiB saltato con report corto; cancellazione e box con size invalida falliscono. | `Mp4MetadataInventory.write` |
| [`unknownEditorRecordsExportEveryByteAndCompleteJson`](../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java#L58) | C | Export SEF sconosciuto conserva tutti i byte/base64 e JSON completo, non i media. | `Mp4MetadataInventory.digest`, `Mp4MetadataInventory.escape` |
| [`malformedSefStillExportsExactBytesAndLargeBlocksHaveExplicitLimit`](../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java#L71) | C | SEF malformato esportato opaco; blocco oltre limite esplicitamente non esportato. | Helper `box(…)`, `report(…)`, `exportedSef(…)`; percorso specificato sopra |
| [`completeSefExportRemainsCancellableBetweenChunks`](../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java#L78) | C | Cancel dopo primo chunk impedisce marcatore di fine export completo. | `Mp4MetadataInventory.write` |
| [`quickTimeMetaWithoutFullBoxFlagsCannotHideFollowingSef`](../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java#L90) | C | Meta QuickTime privo di flag FullBox non nasconde SEF successivo. | Helper `box(…)`, `join(…)`, `report(…)`, `exportedSef(…)`; percorso specificato sopra |
| [`malformedNestedMetadataStillLeavesCompleteRootSefAvailable`](../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java#L98) | C | Errore nested dopo il footer non impedisce l'export completo del SEF alla radice. | `Mp4MetadataInventory.write` |


#### PhotoBasicDetailsTest — 2 test

- **Sorgente:** [PhotoBasicDetailsTest.kt](../app/src/test/java/it/edo/codecprobe/PhotoBasicDetailsTest.kt). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** PhotoBasicDetails.read/rows; ExifInterface; BitmapFactory bounds.
- **Riferimenti produttivi:** [PhotoBasicDetails](../app/src/main/java/it/edo/codecprobe/PhotoBasicDetails.kt).
- **Dati, file e simulazioni:** Vero piccolo JPEG48x32 Base64 con EXIF Model Test Camera, temporaryFile; Android parser/bounds nel Robolectric, URI file portabile.
- **Non verificato / fragilità:** Legge dimensioni/camera/bytes/cartella, non decodifica pixel o convertibilità HEIC. 'Copyable error' controlla solo error nonblank, non clipboard.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`readsTheFivePreviewFieldsWithoutAFullJpegInventory`](../app/src/test/java/it/edo/codecprobe/PhotoBasicDetailsTest.kt#L15) | B | Cinque campi/etichette:48x32, camera, lunghezza reale e directory; nessun JpegInventory completo. | `PhotoBasicDetails.read` |
| [`unreadableSourceProducesCopyableErrorAndUnavailableValues`](../app/src/test/java/it/edo/codecprobe/PhotoBasicDetailsTest.kt#L29) | B | Sorgente assente produce errore e valori non disponibili, senza verificare copia UI. | `PhotoBasicDetails.read` |


#### SamsungSefEditorTest — 3 test

- **Sorgente:** [SamsungSefEditorTest.java](../app/src/test/java/it/edo/codecprobe/SamsungSefEditorTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** SamsungSef.validate; Mp4Metadata.copy.
- **Riferimenti produttivi:** [SamsungSef](../app/src/main/java/it/edo/codecprobe/SamsungSef.java), [Mp4Metadata](../app/src/main/java/it/edo/codecprobe/Mp4Metadata.java).
- **Dati, file e simulazioni:** JSON NDE sanitizzati e SEF generati da SamsungSefEditorFixtures/MotionFixtures; file reali nel round-trip.
- **Non verificato / fragilità:** Preservare byte/riferimenti non prova che Samsung Editor riapra il progetto o ritrovi il percorso protetto sul dispositivo.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`observedVersionsCopyWithoutChangingProjectOrReferences`](../app/src/test/java/it/edo/codecprobe/SamsungSefEditorTest.java#L15) | C | Tre schemi NDE osservati e chiave originale copiati byte per byte dopo relocation; prefisso media invariato. | `SamsungSef.validate`, `Mp4Metadata.copy` |
| [`unexpectedFieldsVersionsReferencesAndInvalidJsonRemainBlocked`](../app/src/test/java/it/edo/codecprobe/SamsungSefEditorTest.java#L32) | A | Mutazioni schema/versioni/tipi/path/durata/ID/JSON e UTF-8 invalido restano bloccate. | Helper `reject(…)`, `editor(…)`, `record(…)`; percorso specificato sopra |
| [`originalKeyIsAnExternalReferenceNotAnOutputHash`](../app/src/test/java/it/edo/codecprobe/SamsungSefEditorTest.java#L55) | A | Original_Path_Hash_Key validato come riferimento esterno: lunghezza, minuscole, ID e suffissi; nessun hash output calcolato. | `SamsungSef.validate` |


#### SamsungSefMotionTest — 1 test

- **Sorgente:** [SamsungSefMotionTest.java](../app/src/test/java/it/edo/codecprobe/SamsungSefMotionTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** SamsungSef.validate; Mp4Metadata.copy; SamsungSefMotionFixtures.
- **Riferimenti produttivi:** [SamsungSef](../app/src/main/java/it/edo/codecprobe/SamsungSef.java), [Mp4Metadata](../app/src/main/java/it/edo/codecprobe/Mp4Metadata.java).
- **Dati, file e simulazioni:** 15 payload HEX completi con SHA fissati da log v0.65; SEF sintetico e MP4 a box artificiali su file reali.
- **Non verificato / fragilità:** Non prova slow motion/BGM/deflicker in Galleria; non usa video visivo valido. Un unico test aggrega molti formati/errori.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`completeLogPayloadsAndRelocation`](../app/src/test/java/it/edo/codecprobe/SamsungSefMotionTest.java#L11) | C | Hash fixture, copie SEF identiche dopo relocation, 37 byte media invariati, alias super-motion, padding, BGM e varianti malformate. | Helper `main(…)`; percorso specificato sopra |


#### SamsungSefReportTest — 1 test

- **Sorgente:** [SamsungSefReportTest.java](../app/src/test/java/it/edo/codecprobe/SamsungSefReportTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** SamsungSef.validate; ConversionFailure.copyText; SavedSession.save/load.
- **Riferimenti produttivi:** [SamsungSef](../app/src/main/java/it/edo/codecprobe/SamsungSef.java), [ConversionFailure](../app/src/main/java/it/edo/codecprobe/ConversionFailure.java), [SavedSession](../app/src/main/java/it/edo/codecprobe/SavedSession.java).
- **Dati, file e simulazioni:** Record Sefd HEX reale modificato type0BA1, offset8000; stato sample con log vuoto, vero journal.
- **Non verificato / fragilità:** Prova diagnosi/persistenza errore, non accettazione Samsung/gallery/editor o conversione reale.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`fullDiagnosticSurvivesFailureCopyAndSavedSessionWithoutRecentLog`](../app/src/test/java/it/edo/codecprobe/SamsungSefReportTest.java#L17) | B | Errore contiene nome/tipo/offset/hash/preview e viene conservato identico nel checkpoint anche senza recent log. | `SamsungSef.validate`, `state.names.get`, `state.failures.set`, `SavedSession.save`, `SavedSession.load`, `saved.state.failures.get`, `SavedSession.clear` |


#### SelectedMetadataReportTest — 2 test

- **Sorgente:** [SelectedMetadataReportTest.java](../app/src/test/java/it/edo/codecprobe/SelectedMetadataReportTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** SelectedMetadataReport.write/section; HomeDiagnostics.exportMetadata.
- **Riferimenti produttivi:** [SelectedMetadataReport](../app/src/main/java/it/edo/codecprobe/SelectedMetadataReport.java), [HomeDiagnostics](../app/src/main/java/it/edo/codecprobe/HomeDiagnostics.java).
- **Dati, file e simulazioni:** Provider Missing registra query/open ma ogni openFile fallisce; writer in memoria o snapshot vero; permessi shadow.
- **Non verificato / fragilità:** Entrambi file sono non leggibili: non prova che report di uno valido sopravviva a un altro invalido, né contenuto metadati reale. Secondo controlla cattura lista/isolamento, non payload riuscito.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`oneUnreadableFileDoesNotHideOthersAndCancellationDoesNotBecomeSuccess`](../app/src/test/java/it/edo/codecprobe/SelectedMetadataReportTest.java#L33) | B | Due input inaccessibili compaiono entrambi, progress1/2 e footer; cancel immediato interrompe e section IOException genera NON COMPLETA. | `SelectedMetadataReport.write`, `SelectedMetadataReport.section` |
| [`exportCapturesEverySelectedFileAndKeepsConversionSessionUntouched`](../app/src/test/java/it/edo/codecprobe/SelectedMetadataReportTest.java#L44) | B | Export congela selezione prima che venga svuotata e conserva sessionId/running/sizeReport del servizio. | `home.state.selectedInputs.addAll`, `home.state.selectedNames.addAll`, `home.state.selectedInputs.get`, `ConversionService.snapshot`, `home.diagnostics.exportMetadata`, `home.state.selectedInputs.clear`, `home.state.selectedNames.clear`, `home.diagnostics.worker.submit` |


#### VideoDateAuditTest — 5 test

- **Sorgente:** [VideoDateAuditTest.java](../app/src/test/java/it/edo/codecprobe/VideoDateAuditTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** VideoDateAudit.verify/readIndex; Snapshot/Index; CreationDatePolicy.
- **Riferimenti produttivi:** [VideoDateAudit](../app/src/main/java/it/edo/codecprobe/VideoDateAudit.java), [CreationDatePolicy](../app/src/main/java/it/edo/codecprobe/CreationDatePolicy.java).
- **Dati, file e simulazioni:** Snapshot numerici/stringhe preparati e Consumer log; ultimo test provider Dates colonne ordinate diversamente, empty/denied e no-write.
- **Non verificato / fragilità:** Prime4 non interrogano Android malgrado messaggi 'letta da Android', nessun video o galleria. Verifica comparatore, non correttezza raccolta Snapshot reale.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`identicalInternalDatesDoNotHideThreeMinuteIndexDifference`](../app/src/test/java/it/edo/codecprobe/VideoDateAuditTest.java#L25) | A | Header e date interne equivalenti non nascondono delta DATE_TAKEN180000ms: warning, non OK galleria. | `VideoDateAudit.verify`, `log.stream` |
| [`dateChangeExposedByAndroidFailsEvenIfHeaderMatches`](../app/src/test/java/it/edo/codecprobe/VideoDateAuditTest.java#L33) | A | Reader string diversa ±120/180/3600s causa IOException anche se header uguale. | `VideoDateAudit.verify` |
| [`headerChangeFailsAndExplicitFallbackIsVerified`](../app/src/test/java/it/edo/codecprobe/VideoDateAuditTest.java#L42) | A | Header diverso1secondo fallisce, fallback esplicito MP4 viene accettato. | `VideoDateAudit.verify` |
| [`missingOrUnparseableDatesNeverClaimGalleryVerified`](../app/src/test/java/it/edo/codecprobe/VideoDateAuditTest.java#L47) | A | Date assenti/non parseabili non producono falso OK e lasciano warning/NON VERIFICATA. | `VideoDateAudit.verify`, `log.stream`, `log.clear` |
| [`indexReadsNullableNamedColumnsWithoutWritingAndHandlesUnavailableProviders`](../app/src/test/java/it/edo/codecprobe/VideoDateAuditTest.java#L68) | B | readIndex usa nomi colonne e gestisce null/denied senza write; URIcloud non interrogato. | `VideoDateAudit.readIndex`, `log.stream` |


### Interfaccia grafica


#### AnalysisRenderTest — 5 test

- **Sorgente:** [AnalysisRenderTest.java](../app/src/test/java/it/edo/codecprobe/AnalysisRenderTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** AnalysisScreen; AnalysisDetailReader.read; AnalysisParser.hex; View.draw.
- **Riferimenti produttivi:** [AnalysisScreen](../app/src/main/java/it/edo/codecprobe/AnalysisScreen.java), [AnalysisDetailReader](../app/src/main/java/it/edo/codecprobe/AnalysisDetailReader.java), [AnalysisParser](../app/src/main/java/it/edo/codecprobe/AnalysisParser.java).
- **Dati, file e simulazioni:** Alberi JPEG/HEIC costruiti manualmente; popup usa un vero piccolo TIFF temporaneo; schermata e Bitmap Robolectric native graphics; PNG.
- **Non verificato / fragilità:** Solo Bitmap.compress riuscita. Nessun golden. I nodi HEIC/JPEG disegnati non provengono dal parsing dei file visualizzati; non prova foto decodificate.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`popupLight`](../app/src/test/java/it/edo/codecprobe/AnalysisRenderTest.java#L50) | B | Popup orientamento su TIFF sintetico con hex visibile, tema chiaro. | Helper `render(…)`; percorso specificato sopra |
| [`files`](../app/src/test/java/it/edo/codecprobe/AnalysisRenderTest.java#L51) | B | Lista riferimenti foto/video preparati manualmente. | Helper `render(…)`; percorso specificato sopra |
| [`nestedHeic`](../app/src/test/java/it/edo/codecprobe/AnalysisRenderTest.java#L52) | B | Albero iprp/ipco/ipma HEIC creato a mano. | Helper `render(…)`; percorso specificato sopra |
| [`blocks`](../app/src/test/java/it/edo/codecprobe/AnalysisRenderTest.java#L53) | B | Blocchi JPEG creati a mano. | Helper `render(…)`; percorso specificato sopra |
| [`hexDark`](../app/src/test/java/it/edo/codecprobe/AnalysisRenderTest.java#L54) | B | Stesso popup TIFF in tema scuro. | Helper `render(…)`; percorso specificato sopra |


#### AudioRenderTest — 4 test

- **Sorgente:** [AudioRenderTest.java](../app/src/test/java/it/edo/codecprobe/AudioRenderTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** AudioScreen/AudioController; AudioSettingsScreen; View.draw/Bitmap.compress.
- **Riferimenti produttivi:** [AudioScreen](../app/src/main/java/it/edo/codecprobe/AudioScreen.java), [AudioController](../app/src/main/java/it/edo/codecprobe/AudioController.java), [AudioSettingsScreen](../app/src/main/java/it/edo/codecprobe/AudioSettingsScreen.java).
- **Dati, file e simulazioni:** Robolectric GraphicsMode.NATIVE (Skia del test, non codec dell'app), stato AudioRunState e descrizione FLAC artificiali; PNG build/reports/ui.
- **Non verificato / fragilità:** Nessun confronto golden/pixel, accessibilità o frame timing. I primi tre controllano solo compressione e dimensione PNG 10–250 kB; settings solo compressione riuscita.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`files`](../app/src/test/java/it/edo/codecprobe/AudioRenderTest.java#L34) | B | Screenshot card file con due nomi sintetici lunghi. | Helper `render(…)`; percorso specificato sopra |
| [`qualityDark`](../app/src/test/java/it/edo/codecprobe/AudioRenderTest.java#L35) | B | Screenshot card compressione in tema scuro. | Helper `render(…)`; percorso specificato sopra |
| [`verifying`](../app/src/test/java/it/edo/codecprobe/AudioRenderTest.java#L36) | B | Screenshot verifica Opus: è soltanto una dicitura/stato preparato dal test. | Helper `render(…)`; percorso specificato sopra |
| [`settingsPage`](../app/src/test/java/it/edo/codecprobe/AudioRenderTest.java#L37) | B | Screenshot pagina impostazioni: non verifica corrispondenza visiva. | `screen.ui.dp` |


#### CollapsibleCardsTest — 2 test

- **Sorgente:** [CollapsibleCardsTest.java](../app/src/test/java/it/edo/codecprobe/CollapsibleCardsTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** CollapsibleCards/HomeScreen; HomeState Bundle.
- **Riferimenti produttivi:** [HomeScreen](../app/src/main/java/it/edo/codecprobe/HomeScreen.java), [HomeState](../app/src/main/java/it/edo/codecprobe/HomeState.java).
- **Dati, file e simulazioni:** Activity Robolectric, accesso diretto mappe dei titoli e SeekBar.
- **Non verificato / fragilità:** Non misura animazioni/fluidità né nuovo percorso Compose; titoli/card-count vincolati.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`onlyFirstCardOpensAndCollapseKeepsValues`](../app/src/test/java/it/edo/codecprobe/CollapsibleCardsTest.java#L22) | B | Solo prima card aperta; collapse conserva stessa SeekBar e CRF28. | Helper `bodies(…)`, `headers(…)`; percorso specificato sopra |
| [`focusConversionAndRecreationKeepExpansion`](../app/src/test/java/it/edo/codecprobe/CollapsibleCardsTest.java#L33) | B | Focus conversione e Bundle mantengono espansione; prima card può poi aprirsi senza chiudere conversione. | `home.screen.sections.focusCard` |


#### ConversionDashboardTest — 1 test

- **Sorgente:** [ConversionDashboardTest.java](../app/src/test/java/it/edo/codecprobe/ConversionDashboardTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeConversion.renderConversionDashboard; ConversionPanel.
- **Riferimenti produttivi:** [HomeConversion](../app/src/main/java/it/edo/codecprobe/HomeConversion.java).
- **Dati, file e simulazioni:** State preparato a mano con frame/progress/opzioni arbitrarie, Activity Robolectric.
- **Non verificato / fragilità:** Nessuna misurazione RAM/batteria o avanzamento reale; presenza view non garantisce dato corretto. Gerarchia/weight esplicitamente vincolati.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`showsFileProgressAndFrozenConfigurationInSeparateRows`](../app/src/test/java/it/edo/codecprobe/ConversionDashboardTest.java#L16) | B | Render file43%, frame100/1000, CRF/preset/Bframe/lookahead congelati e layout comandi su stessa riga. | `ConversionService.State` |


#### CpuModeButtonsTest — 3 test

- **Sorgente:** [CpuModeButtonsTest.java](../app/src/test/java/it/edo/codecprobe/CpuModeButtonsTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** CpuModeButtons; SettingsActivity; AppSettings.
- **Riferimenti produttivi:** [CpuModeButtons](../app/src/main/java/it/edo/codecprobe/CpuModeButtons.java), [SettingsActivity](../app/src/main/java/it/edo/codecprobe/SettingsActivity.java), [AppSettings](../app/src/main/java/it/edo/codecprobe/AppSettings.java).
- **Dati, file e simulazioni:** View Robolectric; campo cpu privato letto reflection; SharedPreferences simulate.
- **Non verificato / fragilità:** Scelta indice/testo non verifica limiti CPU, thread C/C++ o prestazioni reali. childAt e label esatte fragili.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`directChoiceSelectsExactlyOneButtonAndKeepsIndexes`](../app/src/test/java/it/edo/codecprobe/CpuModeButtonsTest.java#L22) | B | Unica selezione e mapping tre indici, etichetta Limited. | `selector.setSelection`, `selector.getChildAt`, `selector.getSelectedItemPosition` |
| [`disablingDuringConversionBlocksAllThreeChoices`](../app/src/test/java/it/edo/codecprobe/CpuModeButtonsTest.java#L29) | B | Disable impedisce tutti click; riabilita scelta. | `selector.setSelection`, `selector.setEnabled`, `selector.getChildAt`, `selector.getSelectedItemPosition` |
| [`settingsReadExistingCpuValueAndSaveNewDirectChoice`](../app/src/test/java/it/edo/codecprobe/CpuModeButtonsTest.java#L40) | B | Impostazioni leggono indice 2 e salvano scelta 0 nel modello prefs. | `AppSettings.save`, `SettingsActivity.class.getDeclaredField`, `selector.getSelectedItemPosition`, `selector.getChildAt` |


#### HomeFailureReportTest — 4 test

- **Sorgente:** [HomeFailureReportTest.java](../app/src/test/java/it/edo/codecprobe/HomeFailureReportTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeConversion.renderSizeReport; FailureReport; ConversionFailure; CompressionSummary.
- **Riferimenti produttivi:** [HomeConversion](../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [ConversionFailure](../app/src/main/java/it/edo/codecprobe/ConversionFailure.java), [CompressionSummary](../app/src/main/java/it/edo/codecprobe/CompressionSummary.java).
- **Dati, file e simulazioni:** Due eccezioni costruite, State e sizeReport manuali; clipboard/dialog shadow, PNG in build/reports/ui.
- **Non verificato / fragilità:** Nessuna eccezione proveniente da conversione. Render screenshot non ha golden; childAt e label italiane vincolano struttura/testo.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`failedFileOpensItsOwnErrorAndCopiesCollapsedDetailsWithoutClosing`](../app/src/test/java/it/edo/codecprobe/HomeFailureReportTest.java#L25) | B | Seconda riga apre/copia solo seconda causa senza chiudere popup; refresh non ricrea riga e snapshot copia failures. | `ConversionService.State`, `state.failures.add`, `CompressionSummary.render`, `CompressionSummary.Item`, `home.conversion.renderSizeReport`, `home.screen.conversion.sizeReport.getVisibility`, `home.activity.getSystemService`, `state.failures.clear` |
| [`absentExceptionMessageStillHasUsefulText`](../app/src/test/java/it/edo/codecprobe/HomeFailureReportTest.java#L55) | A | IOException senza message ha fallback utile e dettagli copiabili. | Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder |
| [`renderLightError`](../app/src/test/java/it/edo/codecprobe/HomeFailureReportTest.java#L74) | B | PNG popup errore chiaro: solo compressione riuscita. | Helper `renderError(…)`; percorso specificato sopra |
| [`renderDarkError`](../app/src/test/java/it/edo/codecprobe/HomeFailureReportTest.java#L75) | B | PNG popup errore scuro: solo compressione riuscita. | Helper `renderError(…)`; percorso specificato sopra |


#### HomeLayoutTest — 3 test

- **Sorgente:** [HomeLayoutTest.java](../app/src/test/java/it/edo/codecprobe/HomeLayoutTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeScreen/HomeDirectoryRow; HomeCompression; AppUi.
- **Riferimenti produttivi:** [HomeScreen](../app/src/main/java/it/edo/codecprobe/HomeScreen.java), [HomeDirectoryRow](../app/src/main/java/it/edo/codecprobe/HomeDirectoryRow.java), [HomeCompression](../app/src/main/java/it/edo/codecprobe/HomeCompression.java), [AppUi](../app/src/main/java/it/edo/codecprobe/AppUi.java).
- **Dati, file e simulazioni:** Robolectric layout320dp/fontScale1.3 e1000dp night; misura View, non screenshot.
- **Non verificato / fragilità:** Copre geometria/touch target specifici, non accessibility service, device OEM o scroll fluido. Label e gerarchia rigide.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`narrowLayoutWithLargeFontKeepsButtonLabelsAndTouchTargets`](../app/src/test/java/it/edo/codecprobe/HomeLayoutTest.java#L26) | B | Tutte card espanse e cartella lunga: bottoni almeno48dp, testo non tagliato a320dp/font1.3. | `home.screen.sections.bodies.keySet`, `home.screen.sections.setCardExpanded`, `home.screen.conversion.convert.getTextSize`, `HomeDirectoryRow.add`, `home.screen.sections.bodies.get`, `home.ui.dp`, `home.screen.conversion.convert.getParent`, `home.screen.conversion.stop.getParent` |
| [`wideDarkLayoutLimitsContentWidthAndUsesDarkSystemIconsPolicy`](../app/src/test/java/it/edo/codecprobe/HomeLayoutTest.java#L40) | B | Contenuto limitato760dp su largo, colori tema scuro e flag systembar coerenti. | `home.ui.dp`, `home.screen.page.getMeasuredWidth` |
| [`qualityShortcutsAndManualSliderKeepOneConsistentSelection`](../app/src/test/java/it/edo/codecprobe/HomeLayoutTest.java#L48) | B | Shortcut CRF18, slider19 deseleziona shortcut, busy blocca/riabilita bottoni. | `home.screen.compression.crfButtons.get`, `home.compression.selectedCrf`, `home.screen.compression.crfSlider.setProgress`, `home.conversion.setBusy` |


#### HomeRenderTest — 5 test

- **Sorgente:** [HomeRenderTest.java](../app/src/test/java/it/edo/codecprobe/HomeRenderTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeScreen; HomeConversion dashboard/report; HomeReset; View.draw.
- **Riferimenti produttivi:** [HomeScreen](../app/src/main/java/it/edo/codecprobe/HomeScreen.java), [HomeConversion](../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [HomeReset](../app/src/main/java/it/edo/codecprobe/HomeReset.java).
- **Dati, file e simulazioni:** Cache metadati, directory autorizzazioni, State e CPU/RAM/batteria testi inseriti manualmente; Robolectric native graphics.
- **Non verificato / fragilità:** Ogni capture controlla PNG>10000byte, nessun golden. Percentuali CPU/RAM/temperatura non sono misurate. activeConversion crea2PNG ma vale1metodo.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`inputPage`](../app/src/test/java/it/edo/codecprobe/HomeRenderTest.java#L42) | B | Pagina input con directory/cartelle create a mano. | `home.screen.sections.focusCard`, `home.destinations.originPanel.close`, `home.screen.destinations.originRows.removeAllViews`, `HomeDirectoryRow.add`, `home.ui.text`, `VideoMetadataCache.clear` |
| [`compressionDark`](../app/src/test/java/it/edo/codecprobe/HomeRenderTest.java#L49) | B | Compressione scura con Bframe/lookahead/CUtree/AQ label simulate, JNI non caricato. | `home.screen.sections.focusCard`, `home.screen.compression.presetSlider.setProgress`, `home.screen.compression.crfSlider.setProgress`, `home.screen.compression.temporalHelp.setVisibility`, `home.screen.compression.bframeSwitch.setText`, `home.screen.compression.lookaheadSwitch.setText`, `home.screen.compression.cuTreeLabel.setText`, `home.screen.compression.aqModeLabel.setText`, `home.screen.compression.aqStrengthLabel.setText`, `VideoMetadataCache.clear` |
| [`activeConversion`](../app/src/test/java/it/edo/codecprobe/HomeRenderTest.java#L57) | B | PNG run attiva+pausa con dati artificiosi, non conversione o prestazioni. | `home.screen.sections.focusCard`, `ConversionService.State`, `home.conversion.renderConversionDashboard`, `home.screen.conversion.cpuValue.setText`, `home.screen.conversion.ramValue.setText`, `home.screen.conversion.batteryValue.setText`, `home.conversion.renderSizeReport`, `VideoMetadataCache.clear` |
| [`batchResults`](../app/src/test/java/it/edo/codecprobe/HomeRenderTest.java#L66) | B | Report3esiti e errore audio predisposti, nessun file convertito. | `home.screen.sections.focusCard`, `ConversionService.rememberFailures`, `BatchRetry.select`, `ConversionService.State`, `state.failures.add`, `CompressionSummary.render`, `CompressionSummary.Item`, `home.conversion.renderConversionDashboard`, `home.conversion.renderSizeReport`, `VideoMetadataCache.clear` |
| [`clearedHome`](../app/src/test/java/it/edo/codecprobe/HomeRenderTest.java#L85) | B | Home dopo clear disegnata in PNG, senza confronto atteso. | `home.reset.clear`, `VideoMetadataCache.clear` |


#### ImageRenderTest — 1 test

- **Sorgente:** [ImageRenderTest.java](../app/src/test/java/it/edo/codecprobe/ImageRenderTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageSettingsScreen; View.draw/Bitmap.compress.
- **Riferimenti produttivi:** [ImageSettingsScreen](../app/src/main/java/it/edo/codecprobe/ImageSettingsScreen.java).
- **Dati, file e simulazioni:** Activity Robolectric GraphicsMode.NATIVE; PNG build/reports/ui/image-settings.png.
- **Non verificato / fragilità:** Solo compressione PNG riuscita, nessun confronto visivo o impostazione salvata.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`settings`](../app/src/test/java/it/edo/codecprobe/ImageRenderTest.java#L17) | B | Disegna pagina impostazioni Foto in PNG senza golden. | `screen.ui.dp` |


#### ImageVirtualReportTest — 3 test

- **Sorgente:** [ImageVirtualReportTest.java](../app/src/test/java/it/edo/codecprobe/ImageVirtualReportTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageResultList; ImageController.refresh; ImageLogPreview.text.
- **Riferimenti produttivi:** [ImageResultList](../app/src/main/java/it/edo/codecprobe/ImageResultList.java), [ImageController](../app/src/main/java/it/edo/codecprobe/ImageController.java), [ImageLogPreview](../app/src/main/java/it/edo/codecprobe/ImageLogPreview.java).
- **Dati, file e simulazioni:** 100 item manuali/callback in memoria e run8500 iniettata; ListView Robolectric640px.
- **Non verificato / fragilità:** Assume meno20 righe visibili a layout fisso, nessun frame timing. Hidden refresh invocato direttamente, non timer reale.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`oneHundredResultsUseOnlyAVisibleSubsetOfViewsAndRecycleTheirActions`](../app/src/test/java/it/edo/codecprobe/ImageVirtualReportTest.java#L20) | B | 100 risultati con<20 view attaccate, riciclo row0→99 invoca callback99 e conserva Item50 dopo shift. | `ImageResultList.Item`, `view.submit`, `view.list.getAdapter`, `view.list.getChildCount`, `view.item`, `view.size` |
| [`hiddenProgressAndLogAreNotRewrittenByTheTimer`](../app/src/test/java/it/edo/codecprobe/ImageVirtualReportTest.java#L33) | B | Refresh non riscrive log/dashboard nascosti ma li aggiorna quando card visibile. | `ImageConversionService.class.getDeclaredField`, `images.render`, `images.screen.sections.focusCard`, `images.screen.dashboard.summary.getText`, `images.screen.log.setText`, `images.refresh`, `images.screen.log.getText`, `images.screen.toggleLog.performClick` |
| [`visibleLogPreviewIsBoundedAndContainsTheNewestEvent`](../app/src/test/java/it/edo/codecprobe/ImageVirtualReportTest.java#L43) | A | Preview log include evento99, esclude vecchi, max4000 caratteri, tratta vuoto e stringa100k. | `log.append`, `ImageLogPreview.text`, `log.toString` |


#### ImageWorkReportTest — 1 test

- **Sorgente:** [ImageWorkReportTest.java](../app/src/test/java/it/edo/codecprobe/ImageWorkReportTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageWorkReport.render; WorkReportView; ImageResultList; popup/clipboard.
- **Riferimenti produttivi:** [ImageWorkReport](../app/src/main/java/it/edo/codecprobe/ImageWorkReport.java), [WorkReportView](../app/src/main/java/it/edo/codecprobe/WorkReportView.java), [ImageResultList](../app/src/main/java/it/edo/codecprobe/ImageResultList.java).
- **Dati, file e simulazioni:** Quattro esiti inventati e URI content://saved/photo senza dati; Activity e clipboard simulate.
- **Non verificato / fragilità:** Nessun salvataggio/cancel/codec produce gli esiti; apertura è solo ACTION_VIEW. Le identità view sono contratti interni.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`savedOutputOpensWithReadPermissionAndFailuresHaveCopyableDetails`](../app/src/test/java/it/edo/codecprobe/ImageWorkReportTest.java#L17) | B | Totali/categorie e1MB risparmio, Apri HEIC con grant, errore copiabile senza chiudere, riga riusata e clear report. | `run.add`, `ImageRunState.Result`, `images.report.render`, `images.report.results.size` |


#### IncrementalReportTest — 1 test

- **Sorgente:** [IncrementalReportTest.java](../app/src/test/java/it/edo/codecprobe/IncrementalReportTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeConversion.renderSizeReport.
- **Riferimenti produttivi:** [HomeConversion](../app/src/main/java/it/edo/codecprobe/HomeConversion.java).
- **Dati, file e simulazioni:** State running e sizeReport stringhe preparati manualmente; Activity Robolectric.
- **Non verificato / fragilità:** Non prova produzione incrementale dal servizio o aggiornamenti thread/observer: chiama direttamente render.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`reportIsVisibleWhileRunningAndUpdatesWithoutWaitingForQueueEnd`](../app/src/test/java/it/edo/codecprobe/IncrementalReportTest.java#L17) | B | Report visibile mentre running, nuova stringa aggiorna subito e titolo cambia a fine run; vuoto nasconde pannello. | `ConversionService.State` |


#### MediaDestinationsRenderTest — 1 test

- **Sorgente:** [MediaDestinationsRenderTest.java](../app/src/test/java/it/edo/codecprobe/MediaDestinationsRenderTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** MediaDestinationsView/Controller; Harness Host; View.draw.
- **Riferimenti produttivi:** [MediaDestinationsView](../app/src/main/java/it/edo/codecprobe/MediaDestinationsView.java).
- **Dati, file e simulazioni:** Provider Directories e grant shadow; loop tutti MediaKind × accessfalse/true;6 PNG, unico metodo JUnit.
- **Non verificato / fragilità:** Readiness controllata sul provider simulato; nessun golden, permesso reale o utilizzo sul percorso Compose Foto. Polling5ms/5s.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`sameDestinationComponentRendersVerifiedAndMissingAccessForEveryMedia`](../app/src/test/java/it/edo/codecprobe/MediaDestinationsRenderTest.java#L23) | B | Pannello comune risulta ready solo con grant inventato per ogni media e produce screenshot dei6stati. | `MediaKind.values` |


#### PhotoComposeRenderTest — 7 test

- **Sorgente:** [PhotoComposeRenderTest.kt](../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** PhotoWorkspace/PhotoCompose; Roborazzi.captureRoboImage; PhotoBasicDetails.
- **Riferimenti produttivi:** [PhotoWorkspace](../app/src/main/java/it/edo/codecprobe/PhotoWorkspace.kt), [PhotoBasicDetails](../app/src/main/java/it/edo/codecprobe/PhotoBasicDetails.kt).
- **Dati, file e simulazioni:** Robolectric native graphics, Info JPEG iniettata; fixture JPEG valida per basicDetailsCard; proprietà roborazzi.test.record=true forzata e ripristinata.
- **Non verificato / fragilità:** Sette capture in modalità RECORD: scrivono PNG e controllano solo file>3000byte. Nessuna golden o verifica pixel. progressInterop non simula conversione attiva.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`floatingWorkspaceFrame`](../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt#L45) | B | Screenshot workspace Foto chiaro. | Helper `capture(…)`; percorso specificato sopra |
| [`floatingDark`](../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt#L46) | B | Screenshot workspace Foto scuro. | Helper `capture(…)`; percorso specificato sopra |
| [`savingAndPermissions`](../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt#L48) | B | Scroll permessi/destinazioni e registra immagine. | Helper `open(…)`, `it(…)`, `save(…)`; percorso specificato sopra |
| [`metadataDark`](../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt#L54) | B | Apre pannello dettagli in tema scuro e registra immagine. | Helper `open(…)`, `hasTestTag(…)`, `save(…)`; percorso specificato sopra |
| [`qualityDark`](../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt#L61) | B | Screenshot compressione scura. | Helper `capture(…)`; percorso specificato sopra |
| [`basicDetailsCard`](../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt#L63) | B | JPEG48x32 valido con EXIF Test Camera: carica dettagli poi registra card, senza asserire valori nel test render. | `images.state.inputs.clear`, `images.state.names.clear`, `images.state.inputs.add`, `images.state.names.add`, `images.open` |
| [`progressInterop`](../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt#L84) | B | Screenshot step HEIC/interop, nessun avanzamento codec. | Helper `capture(…)`; percorso specificato sopra |


#### PhotoComposeTest — 7 test

- **Sorgente:** [PhotoComposeTest.kt](../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** PhotoWorkspace Compose; PhotoCompose bridge; ImageController; PhotoDetailsLoader; ImageDestinations.
- **Riferimenti produttivi:** [PhotoWorkspace](../app/src/main/java/it/edo/codecprobe/PhotoWorkspace.kt), [ImageController](../app/src/main/java/it/edo/codecprobe/ImageController.java), [PhotoDetailsLoader](../app/src/main/java/it/edo/codecprobe/PhotoDetailsLoader.kt).
- **Dati, file e simulazioni:** createAndroidComposeRule<ImageActivity> su Robolectric; nodi semantici e gesti touch veri nel renderer host; JPEG sintetici ImageWorkspaceTest.fixture e dialog shadow.
- **Non verificato / fragilità:** Non dispositivo Android. Non prova codec/pixel né jank; mainClock virtuale. waitUntil5s e testi/tag esatti; foto selezionate iniettando state anziché picker.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`composeIsInstalledAndBackPreservesQuality`](../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt#L43) | B | Nodo photo-new reale installato; click qualità55 e back conservano configurazione e unica Compose root. | Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder |
| [`pressingAGroupedChoiceWidensItAndCancellationDoesNotChangeQuality`](../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt#L63) | B | Press touch allarga scelta50 senza applicare; cancel ripristina larghezza entro1px e solo click cambia qualità. | Helper `down(…)`, `cancel(…)`; percorso specificato sopra |
| [`detailsKeepFiveFieldsAndASeparateCardWhileLoadingAndAfterSelection`](../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt#L81) | B | Dettagli mostrano loading poi5 campi e restano aperti selezionando; cache evita reading/previewLoading tornando su0. | Helper `addPhotos(…)`; percorso specificato sopra |
| [`multiSelectionDetailsListNamesAndDoNotShowThePreviousPhotoMetadata`](../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt#L106) | B | Long press2+click0 mostra2 nomi ordinati senza fotocamera della precedente selezione. | Helper `addPhotos(…)`, `longClick(…)`, `listOf(…)`; percorso specificato sopra |
| [`longPressAndSwitchApplyExceptionsToExactlyTheSelectedFiles`](../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt#L123) | B | Gesti reali selezionano0,2 e switch eccezione applica solo a questi due. | `images.selectionIndices`, `images.state.destinations.plan.exceptionCount` |
| [`clearUsesExistingConfirmationAndNeverDeletesOriginals`](../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt#L140) | B | Clear richiede sì e mantiene esistenza file originali; non confronta tutti i byte. | Helper `addPhotos(…)`; percorso specificato sopra |
| [`scrollingToTheEndKeepsPermissionsAboveFloatingButtons`](../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt#L156) | B | Scroll semantico a fine pagina lascia riga permessi sopra bottone Back: controllo geometrico, non prestazioni. | Helper `addPhotos(…)`, `it(…)`; percorso specificato sopra |


#### WorkspaceNavigationTest — 3 test

- **Sorgente:** [WorkspaceNavigationTest.java](../app/src/test/java/it/edo/codecprobe/WorkspaceNavigationTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** AppNavigation; MainActivity/AudioActivity transizioni; workspace anim resources.
- **Riferimenti produttivi:** [AppNavigation](../app/src/main/java/it/edo/codecprobe/AppNavigation.java), [MainActivity](../app/src/main/java/it/edo/codecprobe/MainActivity.java), [AudioActivity](../app/src/main/java/it/edo/codecprobe/AudioActivity.java).
- **Dati, file e simulazioni:** Activity/View/ScrollView Robolectric, Intent shadow e AnimationUtils.
- **Non verificato / fragilità:** Misura posizione statica dopo scrollTo e ID/durata animazione, non jank o effettivo rendering a60/120Hz.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`navigationIsTheFirstPageChildAndScrollsAwayWithContent`](../app/src/test/java/it/edo/codecprobe/WorkspaceNavigationTest.java#L19) | B | Tab primo figlio scorre fuori viewport su Audio con scrollTo500. | `home.screen.page.getChildAt`, `audio.get`, `audio.screen.page.getChildAt`, `home.screen.navigation.video.isSelected`, `audio.screen.navigation.audio.isSelected`, `audio.screen.sections.focusCard`, `audio.screen.navigation.getLocationOnScreen`, `audio.screen.navigation.getHeight`, `audio.pause` |
| [`workspaceSwitchUsesFadeAndSelectedTabDoesNotLaunchAnotherScreen`](../app/src/test/java/it/edo/codecprobe/WorkspaceNavigationTest.java#L30) | B | Tab già attiva non rilancia; passaggio usa Intent e resource fade, durata uscita140ms. | `home.screen.navigation.video.performClick`, `home.screen.navigation.audio.performClick`, `AudioActivity.class.getName`, `audio.get`, `audio.screen.navigation.audio.performClick`, `audio.screen.navigation.video.performClick`, `MainActivity.class.getName`, `audio.pause` |
| [`backFromAudioAlsoUsesTheWorkspaceFade`](../app/src/test/java/it/edo/codecprobe/WorkspaceNavigationTest.java#L41) | B | Back da Audio termina usando resource fade uscita. | `audio.get`, `audio.pause` |


### Componenti condivisi


#### CompactLogExportTest — 2 test

- **Sorgente:** [CompactLogExportTest.java](../app/src/test/java/it/edo/codecprobe/CompactLogExportTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** HomeDiagnostics export; CompactLog; ZipOutputStream; Intent create-document.
- **Riferimenti produttivi:** [HomeDiagnostics](../app/src/main/java/it/edo/codecprobe/HomeDiagnostics.java), [CompactLog](../app/src/main/java/it/edo/codecprobe/CompactLog.java).
- **Dati, file e simulazioni:** File log reale con 30000 righe sintetiche o messaggio errore; Activity/Picker simulati; veri ZIP temporanei.
- **Non verificato / fragilità:** Non completa export verso provider SAF; annulla il picker. Non prova log generato dal codec, encoding di ogni charset o storage failure.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`reopenedAppExportsOldVerboseLogCompactlyWithoutReplacingIt`](../app/src/test/java/it/edo/codecprobe/CompactLogExportTest.java#L21) | B | Export testo da log precedente riduce dimensione sotto un decimo, conserva errore/stack/nome, originale stessa lunghezza e snapshot rimosso su cancel. | `ConversionService.class.getDeclaredField`, `ConversionService.State`, `ConversionService.batchLogFile`, `home.screen.diagnostics.saveLog.performClick`, `home.diagnostics.logFormatDialog.getListView`, `home.diagnostics.worker.submit`, `home.diagnostics.onLogResult` |
| [`zipChoiceContainsCompleteCompactTextAndUsesZipMime`](../app/src/test/java/it/edo/codecprobe/CompactLogExportTest.java#L43) | B | ZIP reale contiene unica entry CodecProbe-log.txt con errore integrale; MIME/titolo corretti, sorgente identica e snapshot rimosso. | `ConversionService.class.getDeclaredField`, `ConversionService.State`, `ConversionService.batchLogFile`, `home.screen.diagnostics.saveLog.performClick`, `home.diagnostics.logFormatDialog.getListView`, `home.diagnostics.worker.submit`, `home.diagnostics.onLogResult` |


#### CompactLogTest — 6 test

- **Sorgente:** [CompactLogTest.java](../app/src/test/java/it/edo/codecprobe/CompactLogTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** CompactLog.accept/finish/export.
- **Riferimenti produttivi:** [CompactLog](../app/src/main/java/it/edo/codecprobe/CompactLog.java).
- **Dati, file e simulazioni:** Stringhe di successo/errore artificiali, clock numerico e stream in memoria; fino a 300000 eventi e multilinea da 20k caratteri.
- **Non verificato / fragilità:** Non chiama x265, non produce veri errori HDR o misura I/O del logger del servizio. Stress numerico con allocazioni, non profilo RAM.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`repeatedSuccessesAreSummarizedButWarningsErrorsAndUnknownMessagesSurvive`](../app/src/test/java/it/edo/codecprobe/CompactLogTest.java#L10) | A | Campiona ripetizioni ma conserva ultimi frame, contesto, messaggi sconosciuti, stack e fine sessione. | `log.accept`, `log.finish` |
| [`negativeCodecResultsAreNeverSampledAway`](../app/src/test/java/it/edo/codecprobe/CompactLogTest.java#L27) | A | rc=-1 resta, benché rc=0 ripetuto sia campionato. | `log.accept` |
| [`legacyExportKeepsEveryFileAndErrorAndHandlesTimestampRestart`](../app/src/test/java/it/edo/codecprobe/CompactLogTest.java#L33) | A | Export di 8 log sintetici conserva intestazioni/errori e gestisce ripartenza timestamp. | `CompactLog.export` |
| [`hdrMultilineAndAbruptEndAreFlushedAndLargeSamplesAreBounded`](../app/src/test/java/it/edo/codecprobe/CompactLogTest.java#L48) | A | HDR multilinea campionato/troncato; ultimo evento senza newline viene flushato. | `log.accept`, `log.finish`, `CompactLog.export` |
| [`exportUsesTheRealStreamInsteadOfAssumingAvailableIsItsLength`](../app/src/test/java/it/edo/codecprobe/CompactLogTest.java#L57) | A | available()==0 non è scambiato per EOF, il contenuto dello stream resta esportato. | `CompactLog.export` |
| [`repeatedP010FormatsAndHdrVerificationAreCompactedToo`](../app/src/test/java/it/edo/codecprobe/CompactLogTest.java#L63) | A | Righe P010/verify HDR ripetute compattate e ultimo frame/errore mantenuti. | `CompactLog.export` |


#### CompressionSummaryTest — 9 test

- **Sorgente:** [CompressionSummaryTest.java](../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `fast`.
- **Componenti/metodi:** CompressionSummary.render/change.
- **Riferimenti produttivi:** [CompressionSummary](../app/src/main/java/it/edo/codecprobe/CompressionSummary.java).
- **Dati, file e simulazioni:** Item COMPLETE/FAILED/CANCELLED/SKIPPED/IN_PROGRESS già costruiti.
- **Non verificato / fragilità:** Verifica solo report e contabilità; nessun encoder, file salvato o reale stato di servizio.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`halfSizeIsMinusFifty`](../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java#L11) | A | 10→5 MB: -50% e 5 MB risparmiati. | `CompressionSummary.render` |
| [`growthIsPositiveAndUsesAdditionalSpace`](../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java#L16) | A | 10→12 MB: +20% e spazio aggiuntivo, senza falso risparmio. | `CompressionSummary.render` |
| [`totalsUseBytesNotAverageOfPercentages`](../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java#L21) | A | Totali pesati sui byte (110→95 MB), non media percentuali; fallito escluso. | `CompressionSummary.render`, `CompressionSummary.Item` |
| [`unknownSizeIsExcludedAndReported`](../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java#L27) | A | Dimensioni mancanti escluse e denominatore 1 su 2 esplicito. | `CompressionSummary.render` |
| [`zeroInputDoesNotDivideByZero`](../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java#L32) | A | Ingresso zero non divide per zero; uguaglianza resa 0%. | `CompressionSummary.change` |
| [`cancelledAndSkippedHaveNoInventedSavings`](../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java#L36) | A | Cancellato/saltato non inventano risparmi né -100%. | `CompressionSummary.render`, `CompressionSummary.Item` |
| [`runningReportIncludesFinishedVideoAndDistinguishesPendingFiles`](../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java#L43) | A | Report parziale include conclusi e distingue file in corso/in attesa. | `CompressionSummary.render`, `CompressionSummary.Item` |
| [`finalReportDoesNotLeaveFilesRunningOrWaiting`](../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java#L53) | A | Report finale rimuove etichette in corso/in attesa e totale parziale. | `CompressionSummary.render`, `CompressionSummary.Item` |
| [`initialReportWaitsForActualCompletedOutput`](../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java#L60) | A | Prima del primo completato non si dichiara spazio risparmiato. | `CompressionSummary.render`, `CompressionSummary.Item` |


#### DashboardMetricsTest — 3 test

- **Sorgente:** [DashboardMetricsTest.java](../app/src/test/java/it/edo/codecprobe/DashboardMetricsTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** DashboardMetrics.elapsed/cpuBand/batteryBand; ConversionService.State costruttore copia.
- **Riferimenti produttivi:** [DashboardMetrics](../app/src/main/java/it/edo/codecprobe/DashboardMetrics.java), [ConversionService](../app/src/main/java/it/edo/codecprobe/ConversionService.java).
- **Dati, file e simulazioni:** Timestamp e percentuali sintetici; stato costruito senza avviare il servizio.
- **Non verificato / fragilità:** Non misura CPU, temperatura o durata reale in background. L'ultimo test verifica campi copiati, non il servizio.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`clockContinuesInBackgroundAndFreezesAtEnd`](../app/src/test/java/it/edo/codecprobe/DashboardMetricsTest.java#L5) | A | Formato clock con inizio/fine espliciti e caso mai avviato. | `DashboardMetrics.elapsed` |
| [`colorsHaveExplicitBoundariesAndUnknownValues`](../app/src/test/java/it/edo/codecprobe/DashboardMetricsTest.java#L10) | A | Soglie esatte di bande CPU/batteria e valori ignoti/NaN. | `DashboardMetrics.cpuBand`, `DashboardMetrics.batteryBand` |
| [`serviceSnapshotKeepsClockAndStartedConfiguration`](../app/src/test/java/it/edo/codecprobe/DashboardMetricsTest.java#L16) | A | Snapshot mantiene clock e parametri iniziali dopo mutazione dell'oggetto sorgente. | `ConversionService.State` |


#### FailureDetailsTest — 4 test

- **Sorgente:** [FailureDetailsTest.java](../app/src/test/java/it/edo/codecprobe/FailureDetailsTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `fast`.
- **Componenti/metodi:** ConversionFailure costruttore/title/copyText; BuildConfig.VERSION_NAME.
- **Riferimenti produttivi:** [ConversionFailure](../app/src/main/java/it/edo/codecprobe/ConversionFailure.java).
- **Dati, file e simulazioni:** IOException/InterruptedIOException artificiali con causa/suppressed; BuildConfig reale generato.
- **Non verificato / fragilità:** Non esegue alcun fallimento di conversione, rollback o cancellazione: certifica la presentazione dei dettagli forniti.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`absentOrBlankMessagesFallBackToTheRealExceptionType`](../app/src/test/java/it/edo/codecprobe/FailureDetailsTest.java#L8) | A | Messaggi null/vuoti/bianchi ripiegano sul nome IOException. | Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder |
| [`rootCauseAndRollbackFailureBothRemainCopyable`](../app/src/test/java/it/edo/codecprobe/FailureDetailsTest.java#L14) | A | Causa originale e suppressed rollback restano nel testo copiabile, con ultimo stato e versione. | Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder |
| [`multilineFilenameCannotInjectAnExtraDiagnosticHeading`](../app/src/test/java/it/edo/codecprobe/FailureDetailsTest.java#L22) | A | CR/LF del nome non iniettano una nuova intestazione. | Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder |
| [`cancellationDetailsAreNotRewrittenAsAnUnrelatedIoError`](../app/src/test/java/it/edo/codecprobe/FailureDetailsTest.java#L26) | A | Testo e tipo InterruptedIOException rimangono nei dettagli: non prova conteggi CANCELLED del servizio. | Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder |


#### ImageLiveLogTest — 6 test

- **Sorgente:** [ImageLiveLogTest.java](../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java). **Runtime:** Robolectric su JVM host; task storico `integration`.
- **Componenti/metodi:** ImageLogStore; ImageLogExport/Selection; ImageFailureStore; ImageDashboard.
- **Riferimenti produttivi:** [ImageLogStore](../app/src/main/java/it/edo/codecprobe/ImageLogStore.java), [ImageLogExport](../app/src/main/java/it/edo/codecprobe/ImageLogExport.java), [ImageFailureStore](../app/src/main/java/it/edo/codecprobe/ImageFailureStore.java).
- **Dati, file e simulazioni:** Log/JSONL veri; run/results artificiali via reflection; UI/picker shadow, export verso file://. Writer/executor e latch reali; sink lento fittizio.
- **Non verificato / fragilità:** Nessuna coda che converte davvero; contatori iniettati. Polling5ms/5s, worker deadlines2–10s. Due test C usano Context solo come directory base, non SAF.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`runningQueueExportsTheClickSnapshotAndShowsAllCumulativeCounts`](../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java#L33) | B | Click snapshot durante run iniettata esporta solo PRIMA, preserva log completo/count599; rimuove snapshot. | `run.add`, `ImageRunState.Result`, `ImageLogStore.append`, `images.screen.sections.focusCard`, `images.screen.exportLog.isEnabled`, `images.screen.clearLog.isEnabled`, `images.screen.dashboard.summary.getText`, `run.results.size`, `images.screen.exportLog.performClick`, `images.selection.result`, `ImageConversionService.snapshot` |
| [`pickerRecreationAndCancellationKeepTheSourceAndDeleteTheSnapshot`](../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java#L53) | B | Recreate picker conserva snapshot vecchio anche se log cambia; cancel elimina solo snapshot. | `ImageLogStore.append`, `images.selection.log`, `ImageLogStore.reset` |
| [`failedExportCleansUpAndAllowsRetryWhileTheQueueRuns`](../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java#L66) | B | Export verso directory fallisce, ripulisce e abilita retry senza fermare run iniettata. | `ImageLogStore.append`, `images.selection.log`, `images.selection.result`, `images.state.notice.contains`, `images.screen.exportLog.isEnabled`, `ImageConversionService.snapshot` |
| [`concurrentEventsProduceCompletePrefixSnapshotsWithoutLosingAnyEvent`](../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java#L76) | C | 80 append concorrenti e8 snapshot reali sono prefissi completi senza righe perse; interleaving non forzato tra ogni coppia. | `ImageLogStore.append`, `ImageLogStore.snapshot` |
| [`slowDestinationDoesNotHoldTheEventWriterLock`](../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java#L88) | C | Sink bloccato da latch non trattiene lock append; evento viene scritto mentre export ancora fermo. | `ImageLogStore.append`, `ImageLogStore.snapshot`, `ImageLogStore.copy` |
| [`runningFailureArchiveExportContainsOnlyWholeJsonRecords`](../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java#L99) | B | JSONL export di errori durante run contiene solo record intero al click, non quello aggiunto dopo. | `ImageOptions.load`, `ImageRunState.Result`, `ImageFailureStore.append`, `images.screen.exportErrors.isEnabled`, `images.selection.errors`, `images.selection.result` |


#### MetadataMemoryCacheTest — 2 test

- **Sorgente:** [MetadataMemoryCacheTest.java](../app/src/test/java/it/edo/codecprobe/MetadataMemoryCacheTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `integration`.
- **Componenti/metodi:** MetadataMemoryCache.put/get/remove/clear/epoch/bytes.
- **Riferimenti produttivi:** [MetadataMemoryCache](../app/src/main/java/it/edo/codecprobe/MetadataMemoryCache.java).
- **Dati, file e simulazioni:** Stringhe e peso dichiarato, epoche sincrone; nessun worker reale.
- **Non verificato / fragilità:** Non misura memoria effettiva e non avvia letture concorrenti: simula esito tardivo passando un epoch vecchio.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`byteBudgetEvictsLeastRecentlyUsedMetadata`](../app/src/test/java/it/edo/codecprobe/MetadataMemoryCacheTest.java#L7) | A | LRU entro budget 10 e rigetto della singola voce con peso 11. | Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder |
| [`reloadAndClearRejectInFlightOldReads`](../app/src/test/java/it/edo/codecprobe/MetadataMemoryCacheTest.java#L14) | A | Remove/clear cambiano epoca e rifiutano risultati della generazione precedente. | Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder |


#### SavingsBoundaryTest — 3 test

- **Sorgente:** [SavingsBoundaryTest.java](../app/src/test/java/it/edo/codecprobe/SavingsBoundaryTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `fast`.
- **Componenti/metodi:** SavingsPolicy.decide/NotSaved; CompressionSummary.render.
- **Riferimenti produttivi:** [SavingsPolicy](../app/src/main/java/it/edo/codecprobe/SavingsPolicy.java), [CompressionSummary](../app/src/main/java/it/edo/codecprobe/CompressionSummary.java).
- **Dati, file e simulazioni:** Long.MIN_VALUE, zero, -1, Long.MAX_VALUE ed Item in memoria.
- **Non verificato / fragilità:** NotSaved verifica contenitore e messaggio, non classificazione delle eccezioni prodotte dal motore.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`unknownAndExtremeSizesCannotBecomeFalseSavings`](../app/src/test/java/it/edo/codecprobe/SavingsBoundaryTest.java#L8) | A | Dimensioni non positive restano ignote e i massimi non diventano falso risparmio. | `SavingsPolicy.decide` |
| [`notSavedReasonsKeepTheirCategoryAndOriginalMeasurements`](../app/src/test/java/it/edo/codecprobe/SavingsBoundaryTest.java#L16) | A | Eccezione NotSaved conserva categoria e misure, con messaggio non vuoto. | `SavingsPolicy.NotSaved` |
| [`overflowingTotalsAreUnavailableRatherThanNegativeOrInvented`](../app/src/test/java/it/edo/codecprobe/SavingsBoundaryTest.java#L23) | A | Somma in overflow produce totale indisponibile; lista vuota rende stringa vuota. | `CompressionSummary.render`, `CompressionSummary.Item` |


#### SavingsPolicyTest — 2 test

- **Sorgente:** [SavingsPolicyTest.java](../app/src/test/java/it/edo/codecprobe/SavingsPolicyTest.java). **Runtime:** JUnit JVM senza runner Robolectric; task storico `fast`.
- **Componenti/metodi:** SavingsPolicy.decide; CompressionSummary.render.
- **Riferimenti produttivi:** [SavingsPolicy](../app/src/main/java/it/edo/codecprobe/SavingsPolicy.java), [CompressionSummary](../app/src/main/java/it/edo/codecprobe/CompressionSummary.java).
- **Dati, file e simulazioni:** Dimensioni sintetiche long; esiti precostruiti, non output di conversione.
- **Non verificato / fragilità:** Non prova che converter/servizi applichino questa decisione o che una copia originale venga pubblicata.

| Metodo e riferimento | Cat. | Condizioni e risultato atteso realmente asserito | Chiamate nel corpo / helper |
|---|:---:|---|---|
| [`onlyStrictlySmallerKnownFilesPassTheEnabledGate`](../app/src/test/java/it/edo/codecprobe/SavingsPolicyTest.java#L7) | A | Salva soltanto riduzione stretta se attivo; uguale/più grande non salvato, ignoto distinto e filtro disattivato bypassato. | `SavingsPolicy.decide` |
| [`unsavedCandidatesAreExcludedFromActualSavingsTotals`](../app/src/test/java/it/edo/codecprobe/SavingsPolicyTest.java#L16) | A | Totali includono solo il file COMPLETE 10→5 MB; esclude risultati NO_SAVING e SIZE_UNKNOWN. | `CompressionSummary.Item`, `CompressionSummary.render` |

### Helper e risorse non contati come test

- [FileAnalysisChecks.java](../app/src/test/java/it/edo/codecprobe/FileAnalysisChecks.java): `run`, builder di box/JPEG/TIFF e controlli di intervallo/cancellazione, invocati da `AnalysisParserTest`; file sparse e piccoli file temporanei reali. La fixture non contiene filmati decodificabili.
- [SamsungSefEditorFixtures.java](../app/src/test/java/it/edo/codecprobe/SamsungSefEditorFixtures.java): tre JSON NDE sanitizzati costanti (non accesso a file personali o editor), usati dai test dello schema.
- [SamsungSefMotionFixtures.java](../app/src/test/java/it/edo/codecprobe/SamsungSefMotionFixtures.java): 15 record HEX/SHA da diagnostica e builder `Item`, `fixture`, `content`; struttura reale di record, media sintetici.
- [exif-standard-ids.txt](../app/src/test/resources/exif-standard-ids.txt): elenco di ID confrontato dal test del dizionario; è una snapshot locale, non una validazione automatica dello standard esterno.
- Gli helper di provider `Documents`, `Directories`, `Names`, `BlockingPhotos`, `Harness`, `Packets`, i metodi `ready/settle/await/drain`, i builder fixture e `@Before/@After` sono inclusi nelle descrizioni delle classi; non sono test distinti. I provider non sono mock Mockito: sono implementazioni di prova esplicite.

## Prove esistenti fuori dai 332

Questa appendice evita due errori: dire che il repository non abbia prove codec, oppure contarle come già eseguite da `full`. I programmi seguenti sono stati esaminati staticamente, **non eseguiti in questo audit**. Non sono metodi JUnit aggiuntivi dei 332. Le varianti interne ai loop non hanno un conteggio uniforme e non vengono sommate arbitrariamente ai casi Gradle.

### Programmi Java autonomi

`scripts/test-yuv.sh` compila/invoca 14 programmi `main`; compila inoltre due checker invocati nelle prove HDR. Usano assert espliciti/`AssertionError`, non discovery JUnit. Fixture costruite nel sorgente, salvo input opzionali/artefatti nativi indicati. I metodi helper elencati preparano dati o raccolgono assert del `main`, non costituiscono casi separati.

| File e metodi | Categoria | Verifica concreta e limiti |
|---|---|---|
| [BatchProgressTest.java](../tests/BatchProgressTest.java), `main/expect` | A | Percentuali vuoto/negative/overflow e monotonia di tutte le transizioni fino a MAX_FILES; solo numeri, non avanzamento di un servizio. |
| [DestinationPlanTest.java](../tests/DestinationPlanTest.java), `main/check` | A | Destinazioni individuali/globali, origini miste, snapshot difensivi, restore/default; nessun URI realmente aperto. |
| [FolderPathsTest.java](../tests/FolderPathsTest.java), `main/equal` | A | Parent/relative, confini cartella, maiuscole del volume, chiavi/gruppi; ultimo raggruppamento è scritto nel test, non attraversa il coordinatore UI. |
| [NumberValuesTest.java](../tests/NumberValuesTest.java), `main/rejected` | A | Cast esatti Number, Long framecount52, overflow, frazioni, NaN/Infinity/null; nessun MediaFormat ottenuto dal dispositivo. |
| [OutputNameTest.java](../tests/OutputNameTest.java), `main` | A | Prefissi/suffissi, collisioni, traversal, Unicode/224byte/vuoto; nessun file. Sovrapposizione con nuovi test naming JUnit. |
| [YuvPackerTest.java](../tests/YuvPackerTest.java), `main/test` | A | Vero `YuvPacker.copyPlane` su ByteBuffer: padding, crop, pixel stride, offset, bounds e source position; nessun Image Android/decoder. |
| [HdrP010PackerTest.java](../tests/HdrP010PackerTest.java), `main/sample` | A | Campioni10bit esatti, 2/3plane, crop/stride, input malformato e SPS troncato; nessun HDR realmente codificato. |
| [HdrMetadataTest.java](../tests/HdrMetadataTest.java), `main/fixture/au/fails/require/hex` | C | Veri SEI/ledger su file: payload Samsung diagnostici, NAL length-prefix1–4, escape, sostituzione, riordino PTS, dati alterati/frame mancanti e static CSD. I round-trip usano parser/encoder produttivi e video sintetico, non codec. |
| [ColorVuiTest.java](../tests/ColorVuiTest.java), `main/container/check/hex` | C | SPS HEX e box `colr` reali su file, override e conflitti/duplicati/campi non specificati; nessun confronto dei colori renderizzati. |
| [JpegInventoryTest.java](../tests/JpegInventoryTest.java), `main/fixture/read/rejects` e builder `start/frame/scan/segment/bytes/check` | C | Vero parser su stream progressivo sintetico con5blocchi, ICC inter-scan, appendice, hash/payload/offset; prefissi troncati, limite metadata e interruzione. `args[0]` facoltativo legge un vero JPEG ma si limita a stampare inventario, non confronta un oracle. |
| [MetadataDumpTest.java](../tests/MetadataDumpTest.java), `main/box` | C | Sparse `mdat`2,4GB, export SEF limitato, cancel e box troncato; non legge né decodifica2,4GB e l'immutabilità controllata qui è la dimensione del file. |
| [Mp4BoxesTest.java](../tests/Mp4BoxesTest.java), `main/parse/box/join` | C | Conteggio moov/tag senza doppioni, box extended/zero-size e troncato su file veri; non valida video decodificabile. |
| [Mp4MetadataTest.java](../tests/Mp4MetadataTest.java), `main/colors/beam/rejected` e builder `box/join/dates/track/movie/content/colorMovie/sef` | C | Copia vera di date/tag/GPS/colr/beam/SEF su contenitori piccoli costruiti; mantiene primi37byte e configurazione hvcC, SEF finale, blocca UUID/offset/media frammentato/corruzioni/cancel. Non contiene una transcodifica; parte dell'oracle è la verifica interna del copier. |
| [SamsungSefTest.java](../tests/SamsungSefTest.java), `main/failure/reject` e builder `screen/fixture/descriptive/slowMotion/Item` | A | Record HEX osservati e schemi sintetici, versioni103/105/106/107, tipi/nomi/range, diagnosi limitata e500mutazioni seed55; parser reale in memoria, non compatibilità funzionale editor. Parametro opzionale confronta file con fixture84byte. |
| [HdrHeaderCheck.java](../tests/HdrHeaderCheck.java), `main` | C | Legge HEVC generato esternamente, richiede SPS Main10 e CICP BT2020/transfer atteso/limited; checker di parsing non esegue codec da solo. |
| [HdrNativeMetadataCheck.java](../tests/HdrNativeMetadataCheck.java), `main` (due modalità) | C; parte di pipeline D | Legge72pacchetti x265 nativi già prodotti, associa HDR10+ per PTS via ledger, verifica digest; altra modalità confronta SEI prima/dopo remux. I dati dinamici sono fixture Samsung inserite dal test, non cattura Android. |
| [SoftwareHevcDecoderTest.java](../tests/video-java/it/edo/codecprobe/SoftwareHevcDecoderTest.java), `main` | D con frontiera Android simulata | Chiama vero decoder Java/JNI/C++ su fixture PQ/HLG64x48,18frame; byte pixel contro output precedente, PTS crescenti/precisione, cancel dopo3 e colore errato rifiutato. `MediaExtractor`, `MediaFormat` e `HdrCodec` sono classi stub host nei file adiacenti: non prova extractor/hardware Android. |

### Test Python dell'infrastruttura: 29 metodi, separati

Le tabelle seguenti sono un inventario per metodo, non un nuovo risultato di esecuzione. `subTest`, iterazioni e scenari non aumentano il numero dei metodi. Nessuna prova qui interroga GitHub o usa credenziali.

`tests/test_local_tests.py`: 18 metodi. I test di classificazione sono A; il coordinamento con Gradle stub è B; report/snapshot leggono e scrivono veri file temporanei (C). La funzione `write_report` produce XML artificiali e `setUp` sostituisce ROOT/RESULTS con una directory temporanea.

| Classe.metodo | Categoria | Cosa chiama/confronta; simulazioni e limite |
|---|---|---|
| `SelectionTest.test_documentation_needs_no_execution` | A | `select_changes` su due documenti → lista vuota e nativefalse; non verifica esecuzione. |
| `SelectionTest.test_output_changes_select_only_file_contracts` | A | `select_changes` → full conservativa per4path, malgrado nome “only”; il Test.java è passato sotto JAVA produzione, non TEST. Non dimostra selezione mirata di quel vero file test. |
| `SelectionTest.test_shared_control_and_cross_section_fixtures_expand_to_full` | A | Shared Control/MediaDestinations/AudioMetadataFlow → full; ultimo nome è sotto JAVA, non reale cartella test: la conservatività resta verificata per path sconosciuto. |
| `SelectionTest.test_multiple_domains_are_combined_without_duplicates` | A | AudioCopy/PhotoWorkspace/ImageActions → full; non conta classi eseguite né deduplicazione runtime. `AudioCopy.java` è un path di policy, non un file produttivo presente. |
| `SelectionTest.test_unknown_code_is_never_treated_as_documentation` | A | Nuovo path Java → full, non skip. |
| `SelectionTest.test_native_changes_require_separate_native_verification` | A | C++ e build script → full e native required; non avvia native. |
| `SelectionTest.test_native_requirement_survives_unknown_paths_in_either_order` | A | Ordine di path Gradle/C++ non perde flag native. |
| `SelectionTest.test_paths_outside_repository_are_rejected` | A | `normalized_paths('../outside.java')` → ValueError, nessun traversal effettivo eseguito. |
| `SelectionTest.test_explicit_files_cannot_hide_another_uncommitted_shared_change` | B | `main(plan --files)` combina path esplicito con changed_files stub → stampa full; snapshot/Git simulati. |
| `SelectionTest.test_no_source_diff_still_consults_gradle_for_external_input_changes` | B | `main(changed)` con snapshot/diff/env stub deve raggiungere execute sentinella; non esegue Gradle né invalida una dipendenza reale. |
| `ReportAndCacheTest.test_counts_skips_and_failure_without_printing_stack_trace` | C | `reports` interpreta XML artificiale su disco:3test/1fail/1skip, message e dettagli distinti; non testa direttamente stdout. |
| `ReportAndCacheTest.test_compilation_failure_cannot_reuse_stale_successful_xml` | B | subprocess stub exit1 più XML vecchio99successi → build_failed e0test; nessuna vera compilazione fallita. |
| `ReportAndCacheTest.test_each_invocation_asks_gradle_to_validate_real_inputs` | B | Due execute chiamano subprocess stub due volte, controllano offline/maxworkers/no clean e marker1; non verifica davvero gli input Gradle. |
| `ReportAndCacheTest.test_success_with_no_reports_is_an_incomplete_verification` | B | subprocess stub exit0 senza report/marker → exit nonzero. |
| `ReportAndCacheTest.test_snapshot_tracks_uncommitted_sources_tests_catalogs_and_configuration` | C | Veri5file temporanei modificati/eliminati cambiano snapshot; listing Git stub, nessun build reale. |
| `ReportAndCacheTest.test_skipped_or_missing_task_cannot_validate_stale_reports` | B | XML vecchio e marker assente/SKIPPED artificiale non certificano successo. |
| `ReportAndCacheTest.test_jpeg_and_audio_helpers_used_across_sections_expand_to_full` | A |4nomi shared → full; non è un grafo statico completo dei call site. |
| `ReportAndCacheTest.test_ignored_junit_case_is_reported_as_skipped_not_executed` | B | XML e marker costruiti rappresentano un ignored:1skip/0eseguiti; non usa @Ignore realmente scoperto da JUnit. |

`tests/test_ci_scope.py`: 9 metodi `ScopeTest`; patchano subprocess/eventi/env dove indicato. `flags` delega a `scope.classify`, non è un test autonomo.

| Metodo | Categoria | Verifica e limite |
|---|---|---|
| `test_ui_naming_audio_and_java_tests_skip_native` | A | Classificazione path UI/Java → buildtrue/nativefalse; nessuna build. |
| `test_native_build_jni_and_unknown_changes_run_full` | A | Path nativi/config/sconosciuti → build+native; non valida toolchain. |
| `test_changed_jni_lines_trigger_full_even_in_otherwise_fast_java` | B | Diff subprocess stub con native/loadLibrary/external → boundarytrue, normale while → false; lessicale, non risoluzione semantica completa. |
| `test_version_bump_is_quick_but_toolchain_changes_are_full` | B | Diff version-only distinto da NDK/error diff; subprocess stub. |
| `test_documentation_only_and_mixed_changes` | A | Doc, mix, vuoto e forcefull producono flag attesi. |
| `test_initial_commit_invalid_diff_and_manual_run` | B | Eventi iniziali/manuali/base assente e OSError stub → diff vuoto. |
| `test_entry_point_writes_outputs_for_manual_quick_full_and_documentation` | B | Quattro eventi finti e changes stub producono veri file output/summary con flag corretti; non GitHub Actions. |
| `test_cancelled_push_changes_are_compared_to_last_success_not_previous_push` | B | Baseline/event/env e changes stub: usa CI_BASE_SHA e conserva cambi native pendenti; non un push cancellato realmente. |
| `test_real_git_diff_covers_multiple_commits_and_both_sides_of_rename` | C | In una directory temporanea, il test è predisposto a creare davvero repo/commit/rename e leggere vero Git diff; wrapper instrada cwd. Non eseguito ora: questo audit non ha creato commit, neppure temporanei. |

`tests/test_ci_native_key.py`: 2 metodi `NativeKeyTest`, categoria C (vere letture/filehash temporanei), nessun compilatore/cache nativa reale:

- `test_ui_and_version_reuse_native_but_sources_flags_dependencies_and_paths_invalidate`: `fingerprint` non cambia per UI/versione, cambia per file nativi/build/config e SDK/workspace diversi; input build Gradle sintetico, non parser Groovy completo.
- `test_added_removed_and_reordered_files`: ordine dei path irrilevante, rimozione di path o file cambia fingerprint; non prova una restore cache reale.

### Driver nativi e verificatori

| Entrata / componenti chiamati | Tipo | Assert reali e limiti |
|---|---|---|
| [test-audio-native.sh](../scripts/test-audio-native.sh), `audio_smoke.cpp::main`, `audio_pcm_smoke.cpp::main` | D | FLAC→Opus44.1/48/96kHz, mono/stereo, resampling, cancel/troncamento/multicanale rifiutato; MP3/M4A decodificati da FFmpeg alimentano vero AudioStream. Verifica numero campioni decodificati e tag/cover. Non prova decoder Android o servizio/publish SAF. |
| [audio-metadata-fixture.py](../scripts/audio-metadata-fixture.py), `flac/write/comments/expected/opus`, modalità create/verify | C in pipeline D | Lettori FLAC/Ogg indipendenti stdlib confrontano byte/ordine/ripetizioni/Unicode/LRC/picture e APPLICATION Base64; creano input invalidi e comment70k/multi-page. Non è una misura qualità audio o riproduzione OEM. |
| [test-images-native.sh](../scripts/test-images-native.sh), `image_smoke.cpp::main` | D | Compila e chiama motore reale: convert/inspect/verify/decode/decode-oriented/decode-thumbnail. Non transita da UI/JNI Android o SAF. |
| [image-fixtures.py](../scripts/image-fixtures.py), builder `segment/insert/sef/orientation_exif` e scenari top-level | D con oracle C | Genera JPEG reali Pillow; orientamenti1–8/SHORT/LONG/endian, ICC, SOF/chroma/dimensioni, thumbnail1024, SEF/APP/XMP/trailer, limiti e corruzioni. Alcuni pixel confrontati con Pillow orientato/BOX (errore medio<8/<5), trailer/metadata alterati devono fallire. Il verificatore spesso è lo stesso `photo::verify` della produzione; archivio opaco non prova galleria/editor. Alcuni scenari scrivono decine di MiB: non suite rapida. |
| [image-motion-fixtures.py](../scripts/image-motion-fixtures.py), `sef/packet/source/run/boxes/verify_archive/at_archive` | D |7varianti valide e11invalide; vero MP4 pubblico Base64, XMP legacy/modern/element, >4MiB, archivio CPJPEG2/video mpvd unici e ricostruibili byte per byte; reject XML/DTD/offset/versione/troncamento/limite. Non prova Samsung Gallery o playback automatico Motion Photo. |
| [image-extended-fixtures.py](../scripts/image-extended-fixtures.py), `jpeg/sef/source/accept/reject/tamper/mpf_source/run` | D | Prefisso SEF zero, CPJPEG3/record34MiB, frammenti C2PA e MPF gainmap Apple, tamper e input ignoti rifiutati. C2PA è conservazione di firma originale opaca, **non verifica crittografica o nuova firma valida**. Nessun Android HDR display. |
| [smoke.cpp](../app/src/main/cpp/smoke.cpp), `main`; [memory_protection_check.h](../app/src/main/cpp/memory_protection_check.h), `checkMemoryProtection` | D | Vero x2658bit: preset/opzioni, apertura UHD protetta, probe1080p30frame, stream320x24012frame CRF0/23/51,24frame con4combinazioni B/lookahead e PTS; check protezione su9preset. Non end-to-end app. |
| [verify-smoke.py](../scripts/verify-smoke.py), scenari top-level | D/C | FFmpeg decodifica stream e FFprobe controlla frame/dimensioni/colore/B-frame; i due probe1080p leggono JSON precedentemente prodotto dal workflow, non li generano qui. |
| [hdr_smoke.cpp](../app/src/main/cpp/hdr/hdr_smoke.cpp), `main`; [verify-hdr-smoke.py](../scripts/verify-hdr-smoke.py) | D | Vero x26510bit,6frame PQ/HLG e72frame B-reordered, UHD portrait protetto; FFmpeg/FFprobe richiedono Main10, CICP e campioni con precisione oltre8bit. Non hardware Android. |
| [verify-hdr-metadata.py](../scripts/verify-hdr-metadata.py), scenari top-level + HdrNativeMetadataCheck | D/C |72frame con HDR10+ e mastering visibili a FFprobe in elementary e MP4, remux e confronto SEI; dipende da artefatti già generati dalla pipeline. |
| [test-video-decoder.sh](../scripts/test-video-decoder.sh), [test-video-decoder.py](../scripts/test-video-decoder.py), `software_hevc_smoke.cpp::main/drain/read` e Java/JNI test sopra | D | HEVC lossless64x48 VFR/B/PQ/HLG18frame, input packet AnnexB/lengthprefix, confronto pixel bit-exact contro FFmpeg indipendente, flush/PTS/bounds/cancel/color. Extractor Java è stub e build host Linux, non MediaExtractor Android. |
| [verify-ui-modules.py](../scripts/verify-ui-modules.py), scenari top-level | A, controllo statico | Cerca stringhe proibite in Activity/section/state e versione hardcoded; può fallire su commenti o aggirarsi con un alias. Non prova comportamento né architettura completa. |

Il workflow `.github/workflows/android.yml` separa i job native in base alla selezione CI; alcuni richiedono compilazione ARM64/QEMU oltre all'host. La presenza di questi driver è un valore reale del progetto, ma questo audit non ne certifica l'ultima esecuzione o il funzionamento su Windows. Nessuno va installato o lanciato automaticamente su Samsung per questa attività.

## Tracciabilità e ripetibilità dell'audit

Fonti: `AGENTS.md`, `PROJECT_CONTEXT.md`, `docs/local-testing.md`, `scripts/local-test-suites.json`, `scripts/local-tests.gradle`, `scripts/test-local.py`, configurazione Gradle e workflow, implementazioni dei test/helper e componenti collegati sopra. Gli XML sono quelli dei due run indicati in `audited-full.json`. La mappa a seguire include ogni metodo una volta e usa l'ordine delle dichiarazioni sorgente, non l'ordine variabile degli XML.

Le impronte dei sorgenti, test e script esistenti confrontate con `.tools/test-results/last-full.json` coincidono al momento dell'audit. È una verifica statica di corrispondenza degli input, non una ripetizione dei test o una promessa di copertura esaustiva. Documenti/report intermedi non entrano nel totale dei test né determinano un esito verde.
