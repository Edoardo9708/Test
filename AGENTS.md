# Regole di sviluppo dell'app

Queste regole valgono per tutta l'app: Video, Audio, Foto e Analisi.

All'avvio leggere questo file e [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md), poi solo i documenti pertinenti indicati nella sua mappa. Non leggere integralmente il README/cronologia né il catalogo dei test a ogni sessione. Per la grafica leggere [docs/design-references/README.md](docs/design-references/README.md).

Verificare `git status --short`, `git branch --show-current` e `git worktree list` prima delle modifiche: sorgenti e documenti locali, compresi file non tracciati e modifiche non committate, possono differire da GitHub. Preservarli; non modificare altri branch/worktree o eseguire reset, clean distruttivi, commit, push o merge senza richiesta. Controllare dipendenze e toolchain nei file del branch, senza importare versioni da una cronologia. `vendor/` e cache locali possono essere necessari alla build: non eliminarli per ripulire il repository.

La richiesta della sessione e queste regole guidano il lavoro; i requisiti descrivono il comportamento desiderato. Per affermazioni sull'implementazione leggere i sorgenti locali e le prove pertinenti: una descrizione storica o un requisito non certifica una funzione. In caso di divergenza segnalarla senza allineare forzatamente il worktree al remoto.

Prima del codice individuare classi, dipendenze e flussi coinvolti e scegliere un intervento circoscritto; distinguere cause riscontrate, ipotesi e proposte. Nel resoconto indicare file e comportamento cambiati, verifiche realmente eseguite e limiti ancora aperti.

## 1. Efficienza

L'efficienza riguarda il lavoro effettivo eseguito sul SoC e il consumo di risorse, non la brevità del codice. Limitare calcoli, letture, scritture, allocazioni e aggiornamenti grafici inutili. Valutare frequenza di esecuzione e quantità di file elaborati. Non aggiungere elaborazioni inutili soltanto per rendere il codice più elegante.

## 2. Ordine e pulizia

Mantenere codice ordinato, metodi distinti e responsabilità chiare. Evitare catene di condizioni e annidamenti eccessivi. Quando opportuno, preferire una piccola duplicazione con modifiche esplicite a una funzione piena di casi speciali. Scegliere con cognizione di causa, considerando manutenzione e lavoro effettivo sul SoC. Riutilizzare componenti quando semplifica davvero senza introdurre costi inutili.

## 3. Gestione delle eccezioni e degli errori

Gestire esplicitamente ogni errore prevedibile con recupero, segnalazione o interruzione controllata. Non ignorare eccezioni con catch vuoti e non dichiarare riuscita un'operazione fallita. Usare una gestione dedicata nell'interfaccia quando disponibile; altrimenti mostrare un popup comprensibile con dettagli selezionabili o un comando per copiarli. Raggruppare errori ripetitivi evitando raffiche di popup. Conservare i dettagli diagnostici utili, senza esporre dati sensibili. Rispettare la cancellazione delle operazioni e non tentare di proseguire da stati non recuperabili.

## 4. Coerenza grafica

Usare lo stesso linguaggio grafico in tutte le sezioni: componenti, pulsanti, schede, impostazioni, finestre, permessi e animazioni. Azioni equivalenti devono avere presentazione e comportamento coerenti. Le modifiche grafiche devono mantenere l'app uniforme.

Durante la migrazione non introdurre nuove UI legacy in View Java o nuovi wrapper `AndroidView` per mantenerle; seguire l'[architettura finale dell'interfaccia](docs/project-requirements.md#architettura-finale-dellinterfaccia).

## 5. Chiarezza delle funzioni

Usare nomi chiari per metodi, variabili e comandi. Distinguere le responsabilità nel codice. Nell'interfaccia rendere comprensibile cosa fa ogni comando, quali impostazioni usa e quale risultato produce. Rendere disponibili i dettagli tecnici quando utili, senza appesantire l'uso ordinario.

## 6. Divertimento

Curare animazioni piacevoli, messaggi leggeri e qualche Easter egg. Devono accompagnare l'utilizzo senza ostacolarlo o introdurre sprechi significativi sul SoC. Sospendere le animazioni quando non sono visibili e rispettare le preferenze di riduzione delle animazioni. Per errori e rischi sui file usare messaggi seri e chiari.

## 7. Test locali

Quando serve selezionare o eseguire prove, consultare [docs/local-testing.md](docs/local-testing.md), fonte dei comandi e del funzionamento del runner. Partire dai test pertinenti e ampliare per componenti condivisi o interventi significativi; `full` comprende i test JVM, non codec reali o dispositivi.

Leggere prima riepiloghi e soli fallimenti; log/XML dettagliati sono in `.tools/test-results/`. Non ripetere prove invariate, non usare `clean`, rebuild o `--rerun-tasks` senza motivo documentato. Affidare il riuso a Gradle e dichiarare nuovi input o variabili d’ambiente usati dai test. Mantenere concorrenza limitata; nei nuovi test usare sincronizzazione su eventi, non `Thread.sleep`. Test nativi e strumentati restano separati: nessuna installazione o modifica ai dati del telefono senza autorizzazione.

## 8. Documentazione dei test

Quando si interviene sui test o sui contratti documentati, leggere [docs/testing/README.md](docs/testing/README.md) e le sole schede interessate. Quando si aggiunge, modifica o elimina un test, aggiornare le relative schede e l'indice; se cambia il comportamento applicativo verificato, revisionare le schede interessate. Mantenere collegati sorgente, preparazione, operazioni, asserzioni, risultato atteso e limiti; distinguere analisi statica da esecuzioni effettive.

Usare `python scripts/test-docs.py update` per individuare le revisioni pendenti; leggere il codice e aggiornare manualmente le spiegazioni, quindi registrare solo le revisioni realmente effettuate con `review` secondo la guida. Prima di concludere interventi sui test eseguire `python scripts/test-docs.py check`. Non approvare impronte soltanto per eliminare avvisi e non eseguire una suite completa per il solo aggiornamento documentale.
