# Organizzazione dell’interfaccia

**Tipo di fonte:** mappa descrittiva dei confini, verificata staticamente sul worktree locale; non specifica grafica né risultato di una prova sul dispositivo. I sorgenti del branch prevalgono sulle descrizioni di implementazione. Regole generali in [AGENTS.md](../AGENTS.md), requisiti in [project-requirements.md](project-requirements.md).

**Stato attuale, temporaneo:** Video, Audio, Analisi e Impostazioni usano View Android; il workspace Foto usa Compose con interop `AndroidView`. La migrazione è parziale e sperimentale: dettagli in [foto-compose-migration.md](foto-compose-migration.md). Le sezioni seguenti descrivono questi confini attuali, non l'architettura finale.

**Destinazione concordata:** definita in [Architettura finale dell'interfaccia](project-requirements.md#architettura-finale-dellinterfaccia). La sostituzione della presentazione legacy non implica riscrivere coordinatori, servizi o motori.

## Video — componenti View

| Modulo | Responsabilità |
| --- | --- |
| `MainActivity` | Riceve il ciclo di vita e i risultati di Android, poi delega. |
| `HomeController` | Collega i moduli, gestisce apertura impostazioni e notifiche del ciclo di vita. |
| `HomeState` / `HomeSavedState` | Dati della selezione e ripristino di scelte, riquadri e selettori pendenti. Nessuna View nello stato. |
| `HomeScreen` / `HomeSections` | Compongono la pagina e gestiscono i riquadri richiudibili. |
| `Home…Section` | Costruiscono i controlli della rispettiva sezione. Nessuna lettura di file o avvio di conversioni. |
| `HomeDirectoryRow` | Aspetto dei permessi di ogni cartella, con stato colorato e pulsante uniforme. |
| `HomeSelection` | Selezione multipla, cache dei metadati, carosello e transizioni. |
| `HomeDestinations` | Regole individuali e globali, cartelle e autorizzazioni. Riusa `DestinationPlan` e `OriginDirectoryPanel`. |
| `HomeCompression` | Slider, scelte rapide e lettura dei parametri del preset tramite `EncoderPresetPreview`. |
| `HomeConversion` | Avvio della coda, conferma annullamento e visualizzazione dello stato del servizio. |
| `HomeOutputCheck` | Controllo asincrono dei nomi prima della codifica, popup per rinominare o ignorare e annullamento del controllo alla chiusura della pagina. |
| `OutputPlan` / `OutputDocuments` | Prenotazioni dei nomi per cartella e lettura SAF; controllo ripetuto prima della codifica e del salvataggio, senza sovrascritture. |
| `HomeDiagnostics` | Esportazione TXT, pulizia dei log e prova HDR. |
| `HomeSystemMonitor` | Campionamento CPU, PSS e temperatura batteria; non gestisce la codifica. |
| `AppUi` | Colori chiari/scuri, spaziature, pulsanti, righe informative, insets e larghezza massima. Condiviso con Impostazioni; palette e tema dei dialoghi nelle risorse `values` e `values-night`. |
| `QualityButtons` / `CpuModeButtons` | Controlli riusabili, identici in Home e Impostazioni. |

## Regole per le prossime modifiche

- Nella manutenzione delle View ancora presenti conservare i confini delle sezioni; per la nuova UI seguire la regola di migrazione in AGENTS.md. Le azioni restano collegate al coordinatore della funzione, senza duplicare una pagina nascosta.
- I moduli comunicano attraverso il coordinatore; non conservano riferimenti a una precedente Activity.
- I task annullabili hanno controlli di generazione e di distruzione. Alla chiusura si rimuovono observer e callback e si chiudono gli executor.
- La conversione appartiene al servizio del relativo media (`ConversionService`, `AudioConversionService`, `ImageConversionService`), non alla pagina.
- I parametri della conversione in corso si leggono dallo snapshot del servizio. Gli slider rappresentano la configurazione della schermata.
- Le chiavi Bundle precedenti restano compatibili. La versione visibile deriva da `BuildConfig.VERSION_NAME`, generato da Gradle.
- La grafica non chiama direttamente MediaCodec o x265. Il collegamento JNI per la sola anteprima dei preset è in `EncoderPresetPreview`.

## Verifiche

Il [catalogo dei test](testing/README.md) documenta i contratti effettivamente verificati da JUnit/Robolectric e dalle interazioni Compose. Esiti storici e limiti sono separati dalle descrizioni; uno screenshot generato non dimostra fluidità o correttezza visiva completa. `verify-ui-modules.py` controlla alcuni confini lessicali della UI View Video, non tutta l'architettura Compose. I controlli CI dipendono dal workflow e dalla selezione, non da questa mappa.

Il controllo iniziale risolve tutti i conflitti della coda prima di avviare il servizio. Un video ignorato non viene codificato e compare separatamente dai fallimenti. Il servizio riceve i nomi approvati; se un nome viene occupato in seguito, interrompe quel file senza sovrascriverlo. La ricreazione della pagina durante il controllo annulla solo la preparazione: prima dell’avvio non esiste ancora una conversione attiva.

## Audio

`AudioScreen` costruisce le quattro sezioni, `AudioController` gestisce stato visuale e ciclo di vita, `AudioSelection` possiede selettori, nomi, anteprime asincrone e navigazione dei brani. `AudioInputInfo` legge soltanto un’anteprima: la validazione autorevole del formato resta al motore. `AudioActions` prepara uno snapshot della coda, controlla cartella e testi su worker e gestisce avvio/esportazione/riprova. `AudioRunState` distingue salvataggi, fallimenti, annullamenti e risultati senza risparmio; `AudioReport` presenta gli esiti. La sessione audio ha un’identità distinta per aggiornare correttamente il resoconto. Bozza, preferenze e log rimangono separati dal video; audio e video condividono la stessa esclusione per l’avvio del motore. Lo schermo acceso è limitato alla pagina visibile durante una conversione audio attiva.

`AudioSettingsScreen` compone la pagina Impostazioni dedicata in `SettingsActivity` (modalità audio). `AudioSettings` salva esplicitamente i predefiniti in preferenze separate, con revisione; `AudioDraftStore` continua a conservare selezioni e modifiche della coda. `AudioController` applica una nuova revisione soltanto quando non esiste una conversione/preparazione audio attiva.

## Navigazione tra sezioni

`AppNavigation` offre Video, Audio, Immagini e Analisi nelle pagine View. Il workspace Foto ha la propria navigazione Compose in `PhotoWorkspace`; non tutte le sezioni usano la stessa barra. Il cambio resta fra Activity distinte, con Video come radice del task; non è un pager unico. Le code appartengono ai rispettivi servizi.

## Immagini

`ImageActivity` è una `ComponentActivity`: delega lifecycle/risultati e installa `PhotoComposeHost`. `PhotoWorkspace`, `PhotoFilesPane` e `PhotoCompressionPane` presentano lo stato e inviano azioni ai coordinatori Java. `ImageScreen` costruisce soltanto i controlli View ancora visibili tramite `AndroidView` (permessi, avanzamento/report e diagnostica), non una seconda pagina Foto/Compressione nascosta.

`ImageController` gestisce worker, generazioni, render ed esportazione; `ImageSelection` gestisce SAF e lo snapshot della sorgente del report, salvato nel Bundle anche durante il selettore. `ImageState` e `ImageDraftStore` conservano la selezione indipendente. `JpegInventory` legge in streaming senza decodificare pixel; `ImageReport` esporta l'inventario JSON con i payload originali. Il motore HEIC separato esiste già, come descritto sotto: l'inventario non equivale a una conversione o a una verifica dei pixel.

## Analisi

`AnalysisActivity`, `AnalysisController` e `AnalysisScreen` collegano la navigazione View alla lettura strutturale dei file tramite i parser. Offset, box, tag e hex sono analisi dei dati, non una certificazione di decodifica o di compatibilità con la galleria.

## Motore immagini e coda

`ImageOptions` è immutabile e salva i predefiniti separati dalla bozza. `ImageQualityControls` serve la pagina View `ImageSettingsScreen`; il workspace presenta le opzioni con Compose. `ImageSettingsActivity` contiene solo lifecycle. `ImageActions` fa lo snapshot e il controllo cartella, `ImageConversionService` possiede foreground, wake lock, lease condivisa e coda. `ImageConverter` gestisce staging privato, JNI e confronto delle dimensioni; `ImageOutput` pubblica SAF con rilettura/hash e rollback. `ImageRunState` è lo snapshot del servizio, `ImageRunPresenter` si occupa soltanto della visualizzazione. La pagina non possiede il motore e non può cambiare una coda attiva.

Il nativo è in `app/src/main/cpp/images`: `image_jpeg` legge e classifica JPEG/metadati e decodifica con FFmpeg; `image_encode` configura x265 tramite libheif e scrive HEIC; `image_verify` riapre e controlla metadati, profili, trasformazioni e decodifica; `image_jni` è il confine Java. Librerie e patch dipendono dagli script di preparazione del branch: non dedurre le opzioni effettive dai soli default upstream. CPJPEG1 è uno dei formati di archivio: il codice contiene anche CPJPEG2/CPJPEG3 per altri casi, descritti nei documenti delle rispettive funzioni. Le funzioni proprietarie non vengono dedotte dalla conservazione dei byte.
