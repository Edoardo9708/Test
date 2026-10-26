# Requisiti funzionali di CodecProbe

**Tipo di fonte: comportamento desiderato e vincoli di progetto.** Contenuto trasferito dal precedente `PROJECT_CONTEXT.md`, da consultare solo per la funzione interessata. Non è un elenco di funzionalità già completate o una prova di compatibilità. Per le istruzioni operative usare [AGENTS.md](../AGENTS.md); per lo stato dell'implementazione leggere codice/test locali e [ui-architecture.md](ui-architecture.md).

La direzione grafica è mantenuta in [design-references/README.md](design-references/README.md), senza duplicarla qui; i limiti del prototipo sono in [foto-compose-migration.md](foto-compose-migration.md). Una funzionalità richiesta ma assente resta da discutere/implementare: non esporla come disponibile e non ampliare implicitamente l'intervento.

## Architettura finale dell'interfaccia

L'obiettivo concordato è migrare l'intera UI a **Kotlin + Jetpack Compose + Material 3**, usando **Haze e Coil dove appropriato**. Il perimetro comprende Video, Audio, Foto, Analisi, Impostazioni, navigazione, finestre, UI dei permessi, report e ogni altro componente grafico dell'app. Le finestre e i selettori forniti dal sistema Android restano responsabilità del sistema.

L'attuale interfaccia ibrida Java View / Compose è una fase **temporanea** della migrazione. A migrazione completata non devono rimanere schermate legacy costruite con View Java né `AndroidView` usati per mantenere le vecchie interfacce.

Coordinatori, servizi e logica applicativa Java possono rimanere, così come motori C/C++ e JNI: il cambio della UI non richiede di riscriverli. Una loro eventuale migrazione a Kotlin è una decisione separata.

## Coerenza tra sezioni

La grafica va ripensata **nel suo insieme**: niente sezioni moderne accanto a schermate palesemente incoerenti una volta conclusa la migrazione. Riutilizzare davvero componenti e comportamento di pulsanti, pannelli, popup, impostazioni, caricamento, permessi, conferme, caroselli, avanzamento e report. Uniformità **non** significa inventare azioni inesistenti: ad esempio, Pausa/Riprendi deve controllare il servizio reale, non essere un comando cosmetico.

Flusso auspicato comune: scelta file/cartelle sorgente → anteprima/metadati → destinazioni e permessi → opzioni → avvio/pausa/annulla → verifica/salvataggio/report. I dettagli vanno adattati al motore e agli stati reali. Per confrontare le differenze storiche, leggere [inventario-ui-video.md](inventario-ui-video.md).

## Selezioni molto grandi e prestazioni

- Possibilità di selezionare cartelle con **migliaia di elementi** (caso pratico: circa **8.500 foto**).
- Il carosello non deve costruire un widget per ciascuna foto. Limite storico: mostrare al massimo le prime **100** voci e indicare chiaramente che ne esistono altre; evoluzioni verso virtualizzazione complete sono lavori separati, da valutare.
- Il report visibile usa una finestra logica massima di **100 risultati**, con elemento corrente vicino alla posizione **50** quando possibile e widget visibili riciclati. Se l'utente scorre manualmente, non riportarlo immediatamente alla riga corrente; ripristino tramite comando «Segui corrente».
- Lettura nomi, miniature e metadati asincrona, cancellabile, prioritaria per elemento corrente e viewport visibile. Limitare cache, richieste concorrenti, serializzazioni, allocazioni e aggiornamenti UI.
- **Non** ricostruire la pagina intera ogni mezzo secondo, né calcolare misure CPU/memoria per pannelli nascosti.
- Misurare gli scatti sul dispositivo prima di attribuirli a una libreria o a un singolo componente.

## Foto

- Sorgenti JPEG incluse varianti Samsung (EXIF, XMP, ICC, SEF, Motion Photo, miniature, panorami, gain map HDR e altri dati). **Conservazione di byte originali, archiviazione opaca di un metadato e disponibilità della stessa funzione nella galleria sono tre cose diverse.** Non garantire la terza solo perché è soddisfatta la prima o la seconda.
- Output HEIC/HEIF con controllo che sia leggibile/integro secondo le verifiche implementate; sorgente originale intatta. Documentare esplicitamente ciò che viene convertito, copiato senza interpretazione, o rifiutato.
- Preferenza UX discussa: scorciatoie **50, 55 e 60**; non è un inventario dei preset attualmente esposti. Slider e preset devono rimanere allineati allo stato di codifica effettivo.
- Opzione «**Salva solo se più piccolo**»; opzione «**Copia l'originale se l'HEIC non è più piccolo**» dove disponibile. I risultati devono distinguere HEIC salvato, originale copiato, saltato, fallito; non contabilizzare risparmio sulla copia originale.
- Attenzione alla **deduplicazione**: se il file è già stato processato e si trova nella cartella di destinazione, non creare automaticamente un nuovo duplicato con suffissi numerici a causa di una regressione. La gestione delle collisioni deve seguire le regole correnti del piano di output e verificare identità/contenuto; non sovrascrivere accidentalmente i file.
- Report riusciti/falliti, log esportabile anche parzialmente, sessioni/coda recuperabili secondo il motore reale, permessi SAF persistenti gestiti correttamente.
- Estendere i formati (es. PNG, DNG) solo come **nuova richiesta esplicita con analisi di fattibilità**, non fingendo supporto perché il file è selezionabile.

## Video

- Preferenza per conversione efficiente verso **HEVC/H.265**, con x265 nativo e dipendenze confezionate dove previsto, riducendo la dipendenza da capacità variabili dei dispositivi; non è però lecito promettere che ogni HDR/codec/input sia gestibile senza verifiche.
- Controlli di qualità, preset, modalità CPU, pausa/ripresa/annulla e stato effettivo del motore.
- Distinguere verifiche di formato e metadata da decodifica completa; attenzione alle differenze fra risultato del codec e riproduzione sul dispositivo.

## Audio

- Conversione per archivi locali (fra gli esempi: FLAC e Opus VBR), con attenzione a tag, copertine e **testi locali/integrati/sincronizzati**.
- Non promettere che un lettore mostri testi solo perché un formato supporta la memorizzazione; distinguere metadati incorporati da file di testi esterni e testare compatibilità dei player.
- Selezione/cartelle, impostazioni, gestione errori e report coerenti con Video/Foto, senza sostituire l'effettiva logica dei motori.

## Analisi

- Obiettivo: navigazione della **struttura** dei file (JPEG: marker/segmenti; MP4/HEIF: box/atomi; tag, offset, size, spiegazioni), con esadecimale caricato su richiesta.
- Il parser non deve trattenere in RAM l'intero contenuto di file grandi per mostrare una lista; interpretare soltanto i dati necessari e distinguere chiaramente campi sconosciuti, opachi o non verificati.

## Robustezza dei file e delle sessioni

- Non modificare i sorgenti multimediali dell'utente salvo funzione e richiesta esplicite. Verificare/rileggere gli output prima di dichiarare successo; gestire gli artefatti parziali secondo il contratto del motore.
- SAF/URI non garantiscono un percorso filesystem noto. Non inventare cartelle per URI opachi.
- Dichiarare una sessione recuperabile soltanto se persistita; limitare scritture di log/checkpoint evitando aggiornamenti a ogni frame.
- Considerare output già esistenti, skip, annullamento, permessi, conteggi e ricreazione della pagina. Le regole generali sugli errori sono in AGENTS.md.
