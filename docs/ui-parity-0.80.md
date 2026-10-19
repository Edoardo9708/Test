# Unificazione dei componenti UI — 0.80

Implementazione successiva all'inventario della 0.79. L'inventario resta una fotografia della versione precedente.

## Componenti effettivamente condivisi

- `SelectedItemView`: carosello, selezione, pulsante di rimozione, apertura dettagli e gruppi dei metadati.
- `MediaDestinationsView`: stesso layout e stesse regole di comparsa/abilitazione delle destinazioni per i tre formati.
- `OriginDirectoryPanel` e `HomeDirectoryRow`: verifica asincrona, schede verdi/rosse e azioni sui permessi, con etichette parametrizzate.
- `DestinationDraft` e `MediaDestinationsController`: persistenza per URI, scelte individuali, override della coda e ritorno dal selettore per audio/immagini. Il video mantiene il proprio adattatore allo stesso pannello e verificatore.
- `QueueDestinations`: snapshot delle cartelle per file al confine del servizio; supporto delle richieste precedenti con una sola cartella.
- `ResultReportRow` e `ResultDialog`: stessa geometria dei risultati, Apri da 88 dp, errore da 48 dp, popup con dettagli espandibili, copia senza chiusura.
- `SettingsPage`: telaio delle impostazioni, titoli senza divisori iniziali, checkbox e comandi finali comuni.
- `QualityButtons`: stesse scelte rapide grafiche, con valori ed etichette forniti dal formato.
- `ChoiceSlider`: scelte discrete dei preset immagini con il cursore standard; rimossi i dropdown locali.
- `ConversionOptionsView` e `ConversionDashboard`: stesse opzioni comuni, sommario, avanzamento, righe dei parametri e misure di sistema.
- `ProcessSystemMonitor`: un solo campionatore per CPU, PSS e temperatura batteria.
- `LogPanelView`: stessi controlli di cancellazione/espansione e stessa visualizzazione del log.

## Comportamento

- Senza selezione, le destinazioni della coda sono nascoste.
- Scelte individuali e override globale conservati separatamente; rimozione/riordino seguono gli URI, non gli indici precedenti.
- Avvio audio/immagini subordinato alla verifica delle destinazioni effettive.
- Ogni file raggiunge la cartella catturata per quel file prima dell'avvio; modifica della bozza non cambia la richiesta già preparata.
- Annullare il selettore non elimina le scelte; la richiesta pendente sopravvive alla ricreazione e resta associata al proprio file.
- Modifiche non salvate alle impostazioni video conservate alla ricreazione; Indietro non salva. Copia include cartella e opzioni comuni, quando fornite dalla home.
- Report nascosto senza risultati; totali dopo le righe, con indicazione parziale durante l'attività.
- Schermo acceso modificabile durante l'attività senza cambiare i parametri di codifica della sessione.
- Le immagini mostrano la fase e un avanzamento indeterminato del file; non viene inventata una percentuale nativa.

## Differenze funzionali esplicite

Parametri dei codec, testi LRC, diagnostica HDR e formato degli inventari restano specifici. Pausa, salto e ripresa duratura sono ancora capacità del servizio video; non sono simulati negli altri motori.

## Verifica

Eseguiti controlli mirati dopo estrazione video, collegamento audio, collegamento immagini, report/popup, impostazioni e cruscotto. Le prove coprono anche snapshot delle destinazioni, cache, ricreazione, copia dei dettagli e salvataggio esplicito.

Anteprime Android native a larghezza telefono per le tre sezioni, impostazioni video e sei combinazioni formato/accesso del pannello destinazioni. Lint usato come controllo statico. La build firmata esegue inoltre il controllo finale previsto dal progetto; gli smoke test nativi sono selezionati dal diff.
