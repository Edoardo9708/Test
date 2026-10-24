# Migrazione Foto a Compose

La schermata Foto installata nell'Activity usa Kotlin, Jetpack Compose, Material 3 e Haze. La bozza approvata resta [foto-ui-reference.html](foto-ui-reference.html). Il motore JNI/C++ e i coordinatori Java restano gli stessi.

## Parti migrate

- Scelta Nuovo lavoro / Riprendi sessione, intestazioni e navigazione sovrapposta con inset di sistema.
- Importazione, carosello con pressione lunga, rimozione con conferma e dettagli espandibili.
- Destinazione generale, eccezioni della selezione, contatori e opzione solo se più piccolo.
- Qualità con slider Material 3, preset e opzioni avanzate.
- Superfici Haze con sorgenti distinte: sfondo per i pannelli, sfondo e contenuto scorrevole per titolo e barra. Non ci sono acquisizioni bitmap artigianali nell'interfaccia Compose.

Permessi cartelle, avanzamento, report e diagnostica sono ancora controlli Java reali inseriti con `AndroidView`; impostazioni e le altre sezioni restano da migrare. L'Activity è ora `ComponentActivity` per fornire lifecycle e stato salvato a Compose. La navigazione indietro passa dal suo dispatcher.

## Confine temporaneo

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

La migrazione non aggiunge ancora prefisso/suffisso, sovrascrittura, copia originale senza risparmio o carosello oltre 100 elementi. Dopo la verifica di Foto, i componenti condivisi possono sostituire quelli delle altre sezioni.

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
