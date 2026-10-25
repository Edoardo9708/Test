# CodecProbe — riferimenti visivi e specifica UX

**Aggiornato:** 25 ottobre 2026  
**Destinatari:** Codex, sviluppatori UI, revisione sul dispositivo.  
**Priorità:** comportamento reale, coerenza e fluidità; imitare i principi visivi desiderati, **non** copiare ciecamente l'interfaccia di un'altra app.

## 1. Riferimenti principali (in ordine d'uso)

| Riferimento | Dove trovarlo | Cosa osservare | Stato |
|---|---|---|---|
| **Bozza interattiva Foto approvata** | `../foto-ui-reference.html` **nel branch** `ui/morphe-foto-prototype` | Gerarchia della pagina Foto, gruppi, colori, posizioni delle azioni e flusso a passaggi | **Già nel repository** |
| **Morphe Manager / Patcher** | https://github.com/MorpheApp/morphe-manager | Design system: barra flottante, pulsanti collegati, forme, vetro, animazioni, slider, progresso | **Riferimento esterno** |
| **Screenshot Morphe inviato in chat (20 ottobre 2026)** | Da recuperare dalla conversazione «Ramo · Continuare sviluppo app» | Barra di navigazione inferiore, trasparenza, gerarchia visiva | **File originale non disponibile qui** |
| **Video di riferimento del 24 ottobre 2026** | Allegato della chat «Sviluppo grafica concettuale», `1000220682.mp4` | Ritmo e forma delle animazioni di menu/pannelli, transizioni e comportamento reale | **File originale non disponibile qui** |
| **Screenshot/video delle prove sul nostro telefono** | Da recuperare nelle chat sul prototipo | Difetti rispetto a Morphe, scatti del blur su superfici mobili | **Da allegare quando recuperati** |

> Non creare immagini finte attribuendole a Morphe o alla conversazione. Se gli allegati vengono recuperati, collocarli in questa cartella con nomi descrittivi e inserirne link in questa tabella. Non considerare la bozza HTML identica al comportamento nativo.

### Codice di riferimento grafico Morphe

Il prototipo ha già studiato componenti come `FloatingPillBar`, `GlassButton`, `GlassButtonDefaults`, `ConnectedShapes`, `PressFeedback`, `ButtonGroupRow`, `FoldingVisibility`, `SurfaceCard`, `SliderComponents` e `WavyProgress`. Consultare, quando disponibili, i file del repository esterno nella directory `app/src/main/java/app/morphe/manager/ui/screen/shared/`. I nomi sono **punti da studiare**, non classi che devono necessariamente esistere in CodecProbe.

**Licenze:** il riferimento Morphe è segnalato nel documento di migrazione come progetto GPLv3. La nostra implementazione deve rimanere originale usando API pubbliche di Compose/Material3/Haze, evitando copia di sorgenti o asset incompatibili senza esplicita revisione delle licenze.

## 2. Principi non negoziabili

1. **Uniformità:** il restyling definitivo coinvolge Video, Audio, Foto, Analisi, impostazioni, permessi, finestre, navigazione, report e notifiche. La migrazione incrementale è ammessa in un branch sperimentale, ma la UI finale non deve rimanere un mosaico di stili.
2. **Gerarchia:** contenuti e azioni primarie immediatamente leggibili; elementi tecnici dettagliati espandibili, senza schermate affollate o separatori ornamentali superflui.
3. **Movimento come feedback:** l'animazione spiega quale elemento cambia stato, non nasconde un aggiornamento lento o una perdita di focus.
4. **Efficienza:** nessun effetto decorativo deve interferire con conversioni, anteprime o scorrimento; rispettare lo stato visibile e la scala animazioni Android.
5. **Parità funzionale onesta:** controlli coerenti, ma non esporre pulsanti che non hanno ancora una implementazione operativa nei servizi.

## 3. Esempi dei componenti concordati

### A. Bottom bar flottante, ispirata a Morphe

- Navigation inferiore per **Video / Audio / Foto / Analisi**; compatta, con margini rispetto ai bordi e protezione degli inset del sistema.
- Sfondo traslucido e, dove supportato e conveniente, sfocatura **solo della parte dietro la barra**; il contenuto dell'app deve poter scorrere visivamente sotto la barra senza diventare illeggibile.
- Voce attiva chiaramente identificabile; selettore con forma/riempimento/contorno che si trasformano gradualmente, evitando salti o lampeggi.
- Nella prima prova Foto è stata adottata una barra di **56 dp**. Non trattare la misura come universale senza valutare contenuti e accessibilità.
- Nel prototipo il passaggio fra sezioni usa ancora Activity separate: non dichiarare implementato il pager e l'indicatore scorrevole continuo di Morphe.

**Verifica:** selezionare più tab, premere Indietro, ruotare il telefono, aprire tastiera/dialog, navigare con gesti Android. Assenza di sovrapposizione delle azioni importanti.

### B. Header superiore e pulsante Indietro

- Titolo e navigazione superiore con effetto vetro **stabile**, sopra i contenuti sottostanti; niente blocchi opachi inutili.
- Pulsante **Indietro** sempre reperibile in sovrimpressione quando il flusso lo richiede, con dimensioni tocco adeguate e insets corretti.
- Le intestazioni devono restare comprensibili anche su sfondi chiari/scuri e quando il blur non è disponibile.

### C. Schede e riquadri espandibili

- **Riquadri principali opachi**, bordi sottili e raggi coerenti. Haze deve concentrarsi sulle superfici stabili (titolo, barra, Indietro), non sui pannelli che variano continuamente dimensione.
- Alla chiusura, le opzioni spariscono tramite una transizione **collasso/espansione + dissolvenza** anziché sparire istantaneamente o causare un salto di layout.
- Nella versione documentata del prototipo è usata un'animazione condivisa di **220 ms** e `LinearOutSlowInEasing`. Considerarla come baseline da verificare sul telefono, non come unica impostazione corretta.
- Non aggiornare l'intera schermata a ogni frame per eseguire un'animazione locale. Evitare blur applicato ai nodi ridimensionati.

**Lezione dalla prova precedente:** un primo tentativo con trasparenza/Haze sui riquadri espandibili non riproduceva il comportamento di Morphe ed era visivamente insoddisfacente; l'iterazione successiva ha provato superfici opache e la stessa tipologia di animazione (`AnimatedVisibility` / `FoldingVisibility`). Non cambiare arbitrariamente tipo di animazione senza confronto diretto.

### D. Gruppi di pulsanti e selezione

- Pulsanti contigui, con angoli esterni più arrotondati e angoli interni coerenti; selezione visibile senza cambiare improvvisamente dimensione degli elementi.
- Stato selezionato: transizione congiunta di riempimento, colore del testo, bordo e geometria.
- Stato premuto: feedback temporaneo di forma/spazio; cancellare il gesto deve ripristinare la disposizione senza attivare il comando.
- Comandi distruttivi distinti chiaramente e protetti da conferma; i comandi disabilitati non devono sembrare attivi.
- Il pulsante **Avanti** rimane visivamente prioritario nei flussi guidati.

### E. Slider e preset

- Stessa famiglia visuale per qualità Foto, CRF/preset Video e opzioni Audio; scala e azioni differiscono per motore.
- Il cursore si muove fluidamente; non invocare ricodifica, persistenza o calcoli pesanti per ogni evento di trascinamento.
- Nel comportamento storico Foto i preset **50 / 55 / 60** sono immediati, mentre lo slider applica il valore alla fine del trascinamento. Non cambiare senza motivazione.
- Etichette e valore numerico sempre comprensibili e sincronizzati con la configurazione che sarà **davvero** usata.

### F. Anteprime e carosello Foto

- Miniature **reali**, compatte e scorribili orizzontalmente, senza frecce laterali inutili. **64 dp** è la dimensione del prototipo documentato.
- Segnaposto visivo coerente durante le letture; non sostituire la miniatura di una foto con quella della precedente.
- **Pressione lunga** per entrare in multiselezione; tocco successivo per aggiungere/togliere un elemento; indicatore della selezione inequivocabile.
- Lista lazy: leggere esclusivamente gli elementi sullo schermo e un piccolo margine; cache con dimensione esplicita e cancellazione delle richieste non più utili.
- Nel prototipo Compose Coil richiede circa **192 px** per miniature da 64 dp, con cache RAM limitata a **2 MiB**; lo stato di questi valori va verificato nel commit attuale.
- Foto selezionata prima nella lettura dettagli; no decoding massivo o scansione completa per ogni movimento del carosello.

### G. Progresso ondulato e risorse

- Desiderato un **indicatore di avanzamento ondulato** simile a Morphe, dove esiste un progresso numerico affidabile; altrimenti mostrare uno stato indeterminato, senza inventare percentuali.
- Tre pannelli compatti di risorse desiderati dall'utente: **memoria, letture/scritture e CPU**. Non inventare il conteggio I/O finché il sistema non espone misure attendibili.
- Il vecchio sistema di monitor Video invece misura **CPU app, RAM PSS, temperatura batteria**: decidere esplicitamente cosa mantenere o estendere, distinguendo desiderio grafico da metrica implementata.
- Aggiornare le misure **solo quando il pannello è visibile** e a frequenza ragionevole. Non mettere monitor ad alto costo in un ciclo grafico.

### H. Sfondo decorativo animato

- Riferimento al fondo Morphe con movimento lento e gradevole, anche evocativo di luna/pipistrelli, ma reinterpretato in modo originale e adatto a un convertitore multimediale.
- Elementi decorativi **dietro** la pagina, a basso contrasto, adattivi al tema chiaro/scuro; non interferiscono con leggibilità o selezione file.
- Nel prototipo il movimento decorativo è **limitato a 36 secondi**, si sospende quando la pagina non è visibile, durante la conversione e quando Android riduce/disattiva le animazioni. Non sostituirlo con un ciclo infinito pesante.
- Evitare rendering/bitmap aggiuntivi per ogni pannello; preferire composizione efficiente.

## 4. Problemi di UX da NON reintrodurre

- Stessa azione con grafica e popup diversi tra sezioni; permessi cartella gestiti in modo incoerente.
- Riquadri Foto con animazione a scatti, chiusura delle opzioni istantanea o blur costoso mentre cambia la dimensione.
- Carosello che carica migliaia di immagini o report che ricrea 100 righe a ogni tick.
- Nomi file, metadati o anteprime «vecchi» mostrati mentre l'utente passa al file successivo.
- Progress bar percentuale inventata, pulsante Pausa inattivo, doppio salvataggio con suffisso senza la corretta verifica dei file già presenti.
- Sfondo animato continuamente durante conversioni o fuori schermata.
- Copiare codice/asset esterni ignorando licenze o dipendenze.

## 5. Come usare i riferimenti nella prossima sessione Codex

1. Aprire **`docs/foto-ui-reference.html`** nel branch grafico per capire *come deve apparire la pagina*; leggere `docs/foto-compose-migration.md` per capire *cosa è realmente implementato*.
2. Confrontare una schermata o un video del telefono con il riferimento originale Morphe. Se l'originale non è presente nei file di progetto, **chiederne l'allegato quando è indispensabile per replicare un dettaglio**, non ipotizzare che il mock HTML dimostri esattamente animazioni e blur.
3. Individuare il componente piccolo da correggere (es. pannello/indietro/bottom bar); leggere la sua implementazione e test, proporre un intervento locale.
4. Valutare la fluidità sul dispositivo reale. Distinguere test Robolectric, screenshot CI e misurazioni di frame sul telefono.
5. Solo dopo validazione, generalizzare il componente alle altre sezioni senza ricopiare schermate intere.

## 6. Inventario degli allegati originali mancanti

I file **non** sono inclusi in questo pacchetto: gli allegati delle chat precedenti non sono stati trasferiti automaticamente nel filesystem del progetto. Le denominazioni seguenti sono **destinazioni proposte**, non file già presenti:

```text
docs/design-references/
  README.md                        # questo documento
  morphe-bottom-bar-original.png   # screenshot del 20 ottobre, da recuperare
  morphe-menu-animation-original.mp4 # video 1000220682.mp4 del 24 ottobre, da recuperare
  codecprobe-foto-prototype.png    # screenshot della nostra prova sul telefono, da recuperare
```

Appena disponibili gli originali, controllare licenza/provenienza e aggiornare la tabella iniziale con i nomi effettivi. In assenza di immagini, i riferimenti utilizzabili **subito** sono il mock `../foto-ui-reference.html` e il codice del ramo UI.
