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
