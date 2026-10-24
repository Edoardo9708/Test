# Prima prova nativa Foto

Branch `ui/morphe-foto-prototype`, base `1394f12`. Motore JPEG/HEIC invariato.

## Riferimenti approvati

La bozza interattiva è in [foto-ui-reference.html](foto-ui-reference.html); è una specifica visiva, non il codice Android. Riferimento grafico: [Morphe Manager](https://github.com/MorpheApp/morphe-manager), in particolare `FloatingPillBar.kt`, `CardBorder.kt`, `GlassButtonDefaults.kt`, `GlassBlur.kt`, `Animations.kt`, `SliderComponents.kt` e `WavyProgress.kt`, nella cartella `ui/screen/shared`. Concetti adattati con View Android Java; nessun componente Compose copiato o nuova dipendenza runtime.

## Questa prima prova

- Scelta iniziale Nuovo lavoro / Riprendi sessione; la seconda porta ai controlli di recupero esistenti e richiede ancora il comando esplicito di ripresa. Il recupero non modifica la bozza nuova.
- Tre passaggi senza intestazioni richiudibili; indietro e avanti sempre sovrapposti alla pagina, barra inferiore di 56dp, spazio di scorrimento calcolato dall’altezza reale dei controlli e dagli inset di sistema.
- Pannelli azzurri con bordo 1dp e colore semitrasparente; titolo e barra semitrasparenti su sfondo statico. Il motore rimane collegato ai controlli reali.
- Importazione file e cartella cumulativa, deduplicata per URI; nessuna sostituzione delle destinazioni preesistenti.
- Svuota e Rimuovi richiedono conferma, senza cancellare le foto originali.
- Anteprime 64dp con immagini richieste a 192px, decodifica seriale fuori dal thread UI, cache 2 MiB, richieste limitate alla zona visibile con un piccolo margine. Nessuna lettura di tutte le immagini per riempire le anteprime. Resta il limite preesistente di 100 elementi nel carosello.
- Pressione lunga per selezionare più foto, tocco per modificarne la selezione; Rimuovi e Rileggi agiscono sul gruppo. I dettagli multipli elencano i nomi ordinati, uno per riga. La scelta della cartella individuale agisce sul gruppo selezionato e conserva gli URI richiesti durante la rotazione.
- Dettagli di base e Mostra tutto per l’inventario già disponibile.
- 20 segnaposto WebP Minions statici su bianco, complessivamente circa 60 KiB; scelta stabile per URI per evitare cambi casuali a ogni aggiornamento. Provenienza in `photo-placeholder-sources.json`: asset dimostrativi da riesaminare prima della distribuzione pubblica.

## Passaggi ancora da implementare

Questa prova non implementa ancora tutta la specifica: editor unificato della destinazione generale con eccezioni e contatori, prefisso/suffisso, sovrascrittura verificata, copia dell’originale se non conviene convertire, virtualizzazione dell’intero carosello oltre 100 foto, blur reale del contenuto sottostante, animazioni di trasformazione Morphe, indicatore ondulato e tre grafici risorse. La destinazione usa ancora la regola esistente: quando è attiva “Applica a tutta la coda”, essa prevale sulle scelte individuali, che restano memorizzate. Evitare di presentarla come il modello definitivo delle eccezioni.

## Verifica

I test Robolectric verificano selezione, navigazione, destinazioni e rendering sintetico. GitHub Actions esegue test Java, lint e compilazione APK anche sui branch `ui/**`. Runner fissato a Ubuntu 24.04: Ubuntu 26.04 include CMake 4, che rifiuta le policy obsolete della versione fissata di x265.

La prova su telefono resta necessaria per gesti, compatibilità dei provider SAF, leggibilità, prestazioni e animazioni.
