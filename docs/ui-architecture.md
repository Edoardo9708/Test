# Organizzazione dell’interfaccia

La schermata usa componenti Android nativi, senza nuove dipendenze grafiche.
La divisione segue le responsabilità, non il numero di pulsanti.

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

- Un pulsante viene costruito nella sua sezione; il comportamento viene collegato nel gestore della stessa funzione.
- I moduli comunicano attraverso il coordinatore; non conservano riferimenti a una precedente Activity.
- I task annullabili hanno controlli di generazione e di distruzione. Alla chiusura si rimuovono observer e callback e si chiudono gli executor.
- La conversione appartiene a `ConversionService`: chiudere o ricreare la pagina non ferma la coda.
- I parametri della conversione in corso si leggono dallo snapshot del servizio. Gli slider rappresentano la configurazione della schermata.
- Le chiavi Bundle precedenti restano compatibili. La versione visibile deriva da `BuildConfig.VERSION_NAME`, generato da Gradle.
- La grafica non chiama direttamente MediaCodec o x265. Il collegamento JNI per la sola anteprima dei preset è in `EncoderPresetPreview`.

## Verifiche

I test Robolectric coprono riquadri, carosello, conferma annullamento, progresso, resoconto parziale, ripristino dei parametri e dei selettori, esportazione annullata, pulsanti su schermi stretti e limite di larghezza su schermi grandi. I test esistenti di conversione e metadati restano nella CI. `verify-ui-modules.py` controlla che il codice operativo non torni nella Activity o nei costruttori delle sezioni.

Il controllo iniziale risolve tutti i conflitti della coda prima di avviare il servizio. Un video ignorato non viene codificato e compare separatamente dai fallimenti. Il servizio riceve i nomi approvati; se un nome viene occupato in seguito, interrompe quel file senza sovrascriverlo. La ricreazione della pagina durante il controllo annulla solo la preparazione: prima dell’avvio non esiste ancora una conversione attiva.

## Audio

`AudioScreen` costruisce le quattro sezioni, `AudioController` gestisce stato visuale e ciclo di vita, `AudioSelection` possiede selettori, nomi, anteprime asincrone e navigazione dei brani. `AudioInputInfo` legge soltanto un’anteprima: la validazione autorevole del formato resta al motore. `AudioActions` prepara uno snapshot della coda, controlla cartella e testi su worker e gestisce avvio/esportazione/riprova. `AudioRunState` distingue salvataggi, fallimenti, annullamenti e risultati senza risparmio; `AudioReport` presenta gli esiti. La sessione audio ha un’identità distinta per aggiornare correttamente il resoconto. Bozza, preferenze e log rimangono separati dal video; audio e video condividono la stessa esclusione per l’avvio del motore. Lo schermo acceso è limitato alla pagina visibile durante una conversione audio attiva.

`AudioSettingsScreen` compone la pagina Impostazioni dedicata in `SettingsActivity` (modalità audio). `AudioSettings` salva esplicitamente i predefiniti in preferenze separate, con revisione; `AudioDraftStore` continua a conservare selezioni e modifiche della coda. `AudioController` applica una nuova revisione soltanto quando non esiste una conversione/preparazione audio attiva.

## Navigazione tra sezioni

`AppNavigation` costruisce le schede Video/Audio in cima al contenuto di `AppUi`, all’interno dello ScrollView. Le schede scorrono via con la pagina, condividono larghezza e stile e possono ospitare una futura terza sezione. Il cambio usa risorse alpha senza traslazione. Video resta la radice del task; il ritorno da Audio chiude la sola Activity, mentre la coda appartiene al servizio.
