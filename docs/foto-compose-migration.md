# Migrazione Foto a Compose

**Tipo di fonte:** descrizione del prototipo nel worktree locale, non requisito universale né garanzia di fluidità. Include modifiche che possono non essere committate/pubblicate: verificare sorgenti e stato Git. Per la specifica desiderata usare [design-references/README.md](design-references/README.md); le iterazioni precedenti sono raccolte nella sezione storica in fondo.

La schermata Foto installata nell'Activity usa Kotlin, Jetpack Compose, Material 3 e Haze. La bozza approvata resta [foto-ui-reference.html](foto-ui-reference.html). Il motore JNI/C++ e i coordinatori Java restano gli stessi.

## Parti migrate

- Scelta Nuovo lavoro / Riprendi sessione, intestazioni e navigazione sovrapposta con inset di sistema.
- Importazione, carosello con pressione lunga, rimozione con conferma e dettagli espandibili.
- Destinazione generale, eccezioni della selezione, contatori e opzione solo se più piccolo.
- Qualità con slider Material 3, preset e opzioni avanzate.
- Schede Compose opache; Haze riservato al titolo e alla barra flottante, con sfondo e contenuto scorrevole come sorgenti. Non ci sono acquisizioni bitmap artigianali nell'interfaccia Compose.

Permessi cartelle, avanzamento, report e diagnostica sono ancora controlli Java reali inseriti con `AndroidView`; impostazioni e le altre sezioni restano da migrare. L'Activity è ora `ComponentActivity` per fornire lifecycle e stato salvato a Compose. La navigazione indietro passa dal suo dispatcher.

## Confine temporaneo

L'interoperabilità con le vecchie View descritte qui è esclusivamente un ponte di migrazione. Permessi, avanzamento/report e diagnostica ancora incorporati saranno sostituiti secondo l'[architettura finale dell'interfaccia](project-requirements.md#architettura-finale-dellinterfaccia); il mantenimento dei coordinatori Java non richiede di mantenere la loro vecchia UI.

`PhotoComposeHost` pubblica un modello immutabile dopo le modifiche dei coordinatori Java. Non interroga provider o codec e non usa un timer per aggiornare la nuova interfaccia. Il modello conserva solo le prime 100 voci del carosello, come nella prova precedente; contatori e destinazioni riguardano l'intera coda.

I comandi richiamano i binding esistenti, mantenendo conferme, URI delle richieste pendenti, permessi e recupero. Durante questa fase vengono ancora costruiti i widget Java di binding, ma la loro vecchia pagina è scollegata e non viene disegnata. La loro rimozione è un passaggio successivo, dopo aver separato completamente lo stato di presentazione dai widget. Questo compromesso non viene presentato come un'ottimizzazione delle allocazioni iniziali.

Coil carica solo le miniature composte dalla `LazyRow`, con richiesta da 192 px, cache RAM limitata a 2 MiB e nessuna cache disco. Fetch e decodifica passano da un dispatcher con parallelismo uno; uscendo dalla pagina le richieste delle miniature vengono cancellate. Le tessere restano da 64 dp. La rilettura invalida solo le immagini richieste, conservando le altre miniature in cache. Il movimento decorativo si ferma dopo 36 secondi, si sospende fuori pagina/durante elaborazioni e rispetta le animazioni di sistema. Nessuna animazione decorativa deve competere con la conversione.

## Dipendenze

Versioni fissate compatibili con AGP 8.9, Gradle 8.11.1 e compileSdk 35:

- Kotlin e compilatore Compose 2.1.20.
- Compose BOM 2025.04.01, Material 3, Activity Compose 1.10.1.
- Haze 1.5.2 e Coil Compose 2.7.0.
- Roborazzi 1.43.1 solo nei test, come nel progetto Haze, per fotografare l'interfaccia installata.

Morphe usa la stessa famiglia di librerie con versioni più recenti e componenti propri. Non viene importato il suo intero progetto né copiato codice GPL: i componenti di questa app usano le API pubbliche delle librerie Apache 2.0. Riferimenti: [Morphe Manager](https://github.com/MorpheApp/morphe-manager), [Haze 1.5.2](https://github.com/chrisbanes/haze/tree/1.5.2), [Compose](https://developer.android.com/compose), [Coil](https://github.com/coil-kt/coil/tree/2.7.0).

## Verifica e lavoro successivo

I test della logica Java continuano a verificare importazioni, destinazioni, code e conservazione dei file. I nuovi test Compose interagiscono con i controlli realmente visibili. Le immagini di CI devono provenire dalla schermata Compose, non dalla pagina Java scollegata. Il blur hardware richiede anche una prova sul telefono.

La migrazione non aggiunge ancora prefisso/suffisso, sovrascrittura, copia originale senza risparmio o carosello oltre 100 elementi. Dopo la verifica di Foto, il riuso dei componenti condivisi accompagna la migrazione delle altre sezioni verso la destinazione concordata.

## Rifinitura dei pulsanti dopo la prova sul telefono

Confrontati direttamente i componenti Morphe `GlassButton`, `GlassButtonDefaults`,
`ConnectedShapes`, `PressFeedback`, `ButtonGroupRow` e `FloatingPillBar`.
Riferimento: https://github.com/MorpheApp/morphe-manager/tree/main/app/src/main/java/app/morphe/manager/ui/screen/shared.
Il codice dell'app resta un'implementazione propria sulle API Compose già adottate.

I pulsanti collegati ora animano anche gli angoli definiti esplicitamente: la
selezione arrotonda il segmento, la pressione riduce temporaneamente la curvatura.
Le righe di scelte e azioni condividono una piccola parte della larghezza durante
la pressione; una cancellazione del gesto restituisce lo spazio senza attivare
il comando. Riempimento, testo e bordo cambiano insieme, compreso lo stato
disabilitato. I comandi distruttivi hanno testo e bordo rossi e mantengono la
conferma esistente. I colori neutri sono più traslucidi, soprattutto in modalità
scura. Il pulsante Avanti resta evidenziato.

La barra inferiore mantiene l'altezza compatta concordata; i suoi elementi animano
colore e pressione. Il passaggio fra Video/Audio/Foto usa ancora Activity distinte:
non è quindi ancora il pager con indicatore scorrevole condiviso di Morphe.
Le animazioni sono finite e guidate dagli eventi Compose; non aggiungono timer e
seguono la scala delle animazioni del sistema. Le anteprime restano a 64 dp.

## Dettagli delle foto e cache della finestra visibile

Dettagli apre una scheda separata con i cinque campi della specifica: dimensioni,
formato, dimensione file, fotocamera e cartella. I valori assenti sono indicati
esplicitamente; con più foto viene mostrato l'elenco ordinato dei nomi. Mostra
tutto conserva la diagnostica integrale esistente. La scheda resta disponibile
anche durante la lettura, con righe segnaposto sfocate e un messaggio leggibile.
Nome, riepilogo e stato riservano due righe per evitare salti al cambio foto;
non si mostrano metadati della foto precedente come se appartenessero alla nuova.

`PhotoDetailsLoader` legge una foto alla volta, in un worker cancellabile:
foto selezionata prima, poi solo gli elementi realmente visibili della LazyRow,
con 140 ms di attesa per evitare letture attraversando rapidamente il carosello.
Una LRU di massimo 32 schede / 128 KiB conserva solo numeri e stringhe. Pausa,
cambio pagina, elaborazione e finestra non più visibile interrompono il prefetch.
Rileggi invalida le schede delle sorgenti richieste. Il risultato tardivo di una
lettura annullata non può aggiornare la scheda corrente.

Per le anteprime si riusa AndroidX ExifInterface 1.3.7 (la stessa famiglia usata
da Coil), con un massimo di 1 MiB letto/saltato nell'intestazione e senza decodifica
pixel, hash o scansione completa del JPEG. Le dimensioni del file arrivano dal
provider. Gli errori sono leggibili e copiabili nella scheda. Un URI opaco non
viene presentato come percorso di cartella.

La cache completa degli inventari JPEG resta separata; il controller ora la
consulta prima di svuotare lo stato visibile, eliminando il passaggio temporaneo
vuoto quando si torna su una foto già analizzata.

## Prova con schede opache e stessa espansione

Dopo il confronto con `SurfaceCard`, `FoldingVisibility` e `glassFill` di Morphe,
le schede Compose usano un riempimento opaco: il grande riquadro Foto e la scheda
Dettagli non applicano più Haze mentre cambiano altezza. Il titolo, la barra di
navigazione e Indietro conservano il vetro sui contenuti sottostanti. È stata
rimossa anche la sorgente Haze separata dello sfondo, che serviva soltanto ai
pannelli ora opachi.

L'effetto resta espansione + dissolvenza, con durata esplicita 220 ms e curva
`LinearOutSlowInEasing`, condivise da Dettagli, Mostra tutto, destinazione e opzioni
avanzate. Si usano le normali API Compose; non viene introdotta un'animazione
alternativa né aggiornata la versione delle librerie in questa prova.

La sfocatura dei segnaposto durante la lettura resta un effetto distinto dal
vetro dello sfondo. Il confronto sul telefono con la build precedente deve
stabilire se le superfici Haze ridimensionate contribuivano agli scatti: i test
funzionali e i rendering non sono misure della durata dei fotogrammi sul telefono.
