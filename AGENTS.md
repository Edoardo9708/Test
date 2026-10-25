# Regole di sviluppo dell'app

Queste regole valgono per tutta l'app: Video, Audio, Foto e la futura sezione Analisi.

Prima di iniziare il lavoro, leggere `PROJECT_CONTEXT.md`; quando si lavora sulla grafica, leggere anche `docs/design-references/README.md`.

## 1. Efficienza

L'efficienza riguarda il lavoro effettivo eseguito sul SoC e il consumo di risorse, non la brevità del codice. Limitare calcoli, letture, scritture, allocazioni e aggiornamenti grafici inutili. Valutare frequenza di esecuzione e quantità di file elaborati. Non aggiungere elaborazioni inutili soltanto per rendere il codice più elegante.

## 2. Ordine e pulizia

Mantenere codice ordinato, metodi distinti e responsabilità chiare. Evitare catene di condizioni e annidamenti eccessivi. Quando opportuno, preferire una piccola duplicazione con modifiche esplicite a una funzione piena di casi speciali. Scegliere con cognizione di causa, considerando manutenzione e lavoro effettivo sul SoC. Riutilizzare componenti quando semplifica davvero senza introdurre costi inutili.

## 3. Gestione delle eccezioni e degli errori

Gestire esplicitamente ogni errore prevedibile con recupero, segnalazione o interruzione controllata. Non ignorare eccezioni con catch vuoti e non dichiarare riuscita un'operazione fallita. Usare una gestione dedicata nell'interfaccia quando disponibile; altrimenti mostrare un popup comprensibile con dettagli selezionabili o un comando per copiarli. Raggruppare errori ripetitivi evitando raffiche di popup. Conservare i dettagli diagnostici utili, senza esporre dati sensibili. Rispettare la cancellazione delle operazioni e non tentare di proseguire da stati non recuperabili.

## 4. Coerenza grafica

Usare lo stesso linguaggio grafico in tutte le sezioni: componenti, pulsanti, schede, impostazioni, finestre, permessi e animazioni. Azioni equivalenti devono avere presentazione e comportamento coerenti. Le modifiche grafiche devono mantenere l'app uniforme.

## 5. Chiarezza delle funzioni

Usare nomi chiari per metodi, variabili e comandi. Distinguere le responsabilità nel codice. Nell'interfaccia rendere comprensibile cosa fa ogni comando, quali impostazioni usa e quale risultato produce. Rendere disponibili i dettagli tecnici quando utili, senza appesantire l'uso ordinario.

## 6. Divertimento

Curare animazioni piacevoli, messaggi leggeri e qualche Easter egg. Devono accompagnare l'utilizzo senza ostacolarlo o introdurre sprechi significativi sul SoC. Sospendere le animazioni quando non sono visibili e rispettare le preferenze di riduzione delle animazioni. Per errori e rischi sui file usare messaggi seri e chiari.

## 7. Test locali

Leggere `docs/local-testing.md`. Dopo una modifica partire dai test pertinenti: `scripts/test-local.ps1 fast --group files` (anche `savings`, `control`, `errors`); usare `integration --group ...` quando servono componenti Android simulati. `plan --files <percorsi>` mostra la selezione; `changed --files <percorsi>` la esegue. Per componenti condivisi o interventi significativi usare `full`: comprende i test JVM, non codec reali o dispositivi. Senza `--files`, `changed` confronta l'ultima suite completa riuscita, oppure Git HEAD se manca una base locale.

Leggere prima riepiloghi e soli fallimenti; log/XML dettagliati sono in `.tools/test-results/`. Non ripetere prove invariate, non usare `clean`, rebuild o `--rerun-tasks` senza motivo documentato. Affidare il riuso a Gradle e dichiarare nuovi input o variabili d’ambiente usati dai test. Mantenere concorrenza limitata; nei nuovi test usare sincronizzazione su eventi, non `Thread.sleep`. Test nativi e strumentati restano separati: nessuna installazione o modifica ai dati del telefono senza autorizzazione.
