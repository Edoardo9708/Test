# Migrazione Foto a Compose

**Tipo di fonte:** descrizione del prototipo nel worktree locale, non requisito universale né garanzia di fluidità. Include modifiche che possono non essere committate/pubblicate: verificare sorgenti e stato Git. Per la specifica desiderata usare [design-references/README.md](design-references/README.md); le iterazioni precedenti sono raccolte nella sezione storica in fondo.

La schermata Foto installata nell'Activity usa Kotlin, Jetpack Compose, Material 3 e Haze. La bozza approvata resta [foto-ui-reference.html](foto-ui-reference.html). Il motore JNI/C++ e i coordinatori Java restano gli stessi.

## Parti migrate

- Scelta Nuovo lavoro / Riprendi sessione, intestazioni e navigazione sovrapposta con inset di sistema.
- Importazione, carosello con pressione lunga, rimozione con conferma e dettagli espandibili.
- Destinazione generale, eccezioni della selezione, contatori e opzione solo se più piccolo.
- Qualità con slider Material 3, preset e opzioni avanzate.
- Schede Compose opache; Haze 2 riservato ai controlli fissi, con una sola sorgente contenente sfondo e contenuto scorrevole. Non ci sono acquisizioni bitmap artigianali nell'interfaccia Compose.

Avanzamento, report e diagnostica sono ancora controlli Java reali inseriti con `AndroidView`; impostazioni e le altre sezioni restano da migrare. L'Activity è ora `ComponentActivity` per fornire lifecycle e stato salvato a Compose. La navigazione indietro passa dal suo dispatcher.

## Confine temporaneo

L'interoperabilità con le vecchie View descritte qui è esclusivamente un ponte di migrazione. Avanzamento/report e diagnostica ancora incorporati saranno sostituiti secondo l'[architettura finale dell'interfaccia](project-requirements.md#architettura-finale-dellinterfaccia); il mantenimento dei coordinatori Java non richiede di mantenere la loro vecchia UI.

`PhotoComposeHost` pubblica un modello immutabile dopo le modifiche dei coordinatori Java. Non interroga provider o codec e non usa un timer per aggiornare la nuova interfaccia. Il modello conserva solo le prime 100 voci del carosello, come nella prova precedente; contatori e destinazioni riguardano l'intera coda.

I comandi Compose richiamano direttamente i coordinatori: selezione, conferme, navigazione, destinazioni e opzioni non passano più attraverso pulsanti o slider Java nascosti. `ImageSteps` conserva soltanto la navigazione; `PhotoDestinationState` presenta valori del piano di destinazione senza View. `PhotoComposeHost` deriva lo snapshot da questi dati e da `ImageState`, senza leggere etichette o stati dei vecchi widget. Il testo dell'inventario completo è formattato una volta per inventario, anziché a ogni modifica della destinazione.

La pagina Java scollegata, il suo carosello, i suoi pannelli Foto/Compressione, il selettore iniziale e la barra flottante duplicata non vengono più costruiti. `ImageScreen` conserva esclusivamente i controlli Java realmente incorporati con `AndroidView`: avanzamento e diagnostica. Sono un'unica rappresentazione visibile delle rispettive funzioni, non una copia sotto Compose. L'avanzamento aggiorna il report solo nella pagina dedicata. Conferme, URI delle richieste pendenti, permessi e recupero restano nei coordinatori esistenti; il motore JNI non cambia.

Le autorizzazioni cartelle sono presentate da `PhotoPermissionsPane` come righe lazy Compose con chiavi stabili, stato testuale, percorso selezionabile e comandi Scegli/Autorizza/Gestisci. Foto non costruisce più `originRows` né incorpora i permessi con `AndroidView`. `OriginDirectoryAccess` conserva in Java risoluzione SAF, associazioni, cache per verifica e worker singolo cancellabile; pubblica snapshot immutabili. `OriginDirectoryPanel` è il renderer View preesistente delle sezioni ancora legacy e delega allo stesso componente. Il limite di 100 cartelle visibili e di 100 numeri per gruppo riguarda la presentazione; conteggi, destinazioni e disponibilità dell’avvio considerano l’intera coda. Le azioni di righe non più correnti sono rifiutate. Richieste pendenti e selettore restano nel coordinatore esistente.

Coil carica solo le miniature composte dalla `LazyRow`, con richiesta da 192 px, cache RAM limitata a 2 MiB e nessuna cache disco. Fetch e decodifica passano da un dispatcher con parallelismo uno; uscendo dalla pagina le richieste delle miniature vengono cancellate. Le tessere restano da 64 dp. La rilettura invalida solo le immagini richieste, conservando le altre miniature in cache. Nella nuova base 1.05 lo sfondo è statico: non esiste più il timer decorativo di 36 secondi.

La pagina usa sezioni distinte di `LazyColumn`, con chiavi stabili e stato salvato per ogni passaggio. Non mantiene due pagine sovrapposte durante un `AnimatedContent`; l'ingresso aggiorna soltanto l'alpha del layer. I pesi dei pulsanti sono letti nella misura del `Layout`: l'animazione non ricompone la riga per leggere quei valori. Colori e forme restano animazioni locali del singolo pulsante; è stato tolto il clip duplicato esterno alla Surface. La misura intrinseca dell'altezza rimane necessaria per allineare pulsanti con etichette diverse; non viene dichiarato un costo nullo. Queste scelte riducono lavoro evitabile, ma non costituiscono una verifica degli scatti sul Samsung.

## Confronto della sfocatura sul telefono

In Impostazioni immagini → Aspetto Foto, «Sfocatura effetto vetro» è attiva per impostazione predefinita e si salva subito. Tornando a Foto, anche durante una coda attiva, `PhotoComposeHost` rilegge `PhotoAppearance` e aggiorna la composizione. La preferenza è separata dai default di compressione: non richiede Salva, non viene sovrascritta da Usa configurazione e non cambia le opzioni della coda.

Con l’interruttore spento `PhotoWorkspace` non installa `hazeSource` e i controlli fissi non installano `hazeBlur`: resta un normale sfondo traslucido. Riattivandolo torna il percorso Haze, con le consuete limitazioni di supporto hardware, visibilità e conversione in corso. La sfocatura dei segnaposto dei dettagli è un effetto separato e non viene cambiata. Il confronto avviene nello stesso APK; il risultato sulla fluidità resta da misurare sul telefono.

## Prova circoscritta: chiusura dei dettagli

Solo il riquadro aperto dal pulsante **Dettagli** nella scheda File selezionati usa ora il margine superiore di 12 dp dentro `PhotoFold`: pulsanti e pannello animato stanno in una Column senza `spacedBy` esterno. Il margine si richiude insieme alla scheda, senza sparire separatamente quando `AnimatedVisibility` rimuove il contenuto. Il test frame per frame sul layout precedente riproduceva uno scalino finale di 12,5 dp nella fixture Robolectric.

Durata 220 ms, curva, superfici e helper `PhotoFold` restano invariati. **Mostra tutto**, destinazioni e opzioni avanzate mantengono il layout precedente, per consentire un confronto circoscritto sul telefono. La prova geometrica non è una misura della fluidità sul dispositivo.

## Dipendenze

Versioni aggiornate nella 1.05, confrontate con il catalogo Morphe il 25 ottobre 2026:

- AGP 9.4.1, Gradle 9.6.0 con checksum ufficiale e compileSdk 37. JDK 17, targetSdk 35 e NDK 27.2.12479018 restano invariati.
- Kotlin e compilatore Compose 2.4.21, usando il supporto Kotlin integrato di AGP.
- Compose BOM 2026.09.00, Material 3 1.5.0-beta02, Activity Compose 1.13.0. La beta Material 3 è intenzionale nel branch sperimentale, non una versione stabile.
- Haze e modulo Haze Blur 2.0.1, API tipizzata `hazeBlur`, e Coil Compose 2.7.0.
- Robolectric 4.17 e Roborazzi 1.77.0 solo nei test, aggiornati per SDK 37 e il nuovo Compose, per verificare e fotografare l'interfaccia installata.

Morphe usa componenti propri sulle stesse librerie. Non viene importato il suo intero progetto né copiato codice GPL: i componenti di questa app usano le API pubbliche delle librerie Apache 2.0. Riferimenti: [catalogo Morphe](https://github.com/MorpheApp/morphe-manager/blob/main/gradle/libs.versions.toml), [migrazione Haze 2](https://github.com/chrisbanes/haze/blob/2.0.1/docs/migrating-2.0.md), [compatibilità AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes), [Compose](https://developer.android.com/compose), [Coil](https://github.com/coil-kt/coil/tree/2.7.0).

## Verifica e lavoro successivo

I test della logica Java continuano a verificare importazioni, destinazioni, code e conservazione dei file. I nuovi test Compose interagiscono con i controlli realmente visibili. Le immagini di CI devono provenire dalla schermata Compose, non dalla pagina Java scollegata. Il blur hardware richiede anche una prova sul telefono.

La migrazione non aggiunge ancora prefisso/suffisso, sovrascrittura, copia originale senza risparmio o carosello oltre 100 elementi. Dopo la verifica di Foto, il riuso dei componenti condivisi accompagna la migrazione delle altre sezioni verso la destinazione concordata.

## Iterazioni precedenti alla ricostruzione 1.05

Le sezioni seguenti documentano le prove precedenti; le dipendenze e la struttura attuali sono quelle descritte sopra.

### Rifinitura dei pulsanti dopo la prova sul telefono

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
