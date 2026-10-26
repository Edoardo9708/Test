# CodecProbe — orientamento per gli agenti

Leggere all'avvio insieme a [AGENTS.md](AGENTS.md). Questa è una mappa, non una cronologia, una copia dei requisiti o una certificazione del codice.

## Gerarchia delle fonti

- **Istruzioni:** richiesta della sessione e AGENTS.md. I [requisiti funzionali](docs/project-requirements.md) e la [specifica visiva](docs/design-references/README.md) descrivono gli obiettivi; non attestano che siano realizzati.
- **Fatti sull'implementazione:** sorgenti, configurazioni e test del worktree locale, compresi file non tracciati e modifiche non committate. Le mappe tecniche aiutano a trovare il codice; in caso di divergenza verificare e segnalare il limite.
- **Evidenze:** un risultato vale per gli input e il tipo di prova dichiarati. Descrizione statica, mock/Robolectric, esecuzione nativa e prova su dispositivo sono distinti.
- **Storia:** README, audit datati e documenti esplicitamente storici conservano il contesto delle decisioni; non definiscono automaticamente lo stato attuale.

## Perimetro

CodecProbe è un'app Android per Video, Audio, Foto e Analisi strutturale dei file. I motori includono Java e C/C++/JNI; il prototipo Foto usa Kotlin/Compose con interop View. Non presumere la migrazione di tutte le sezioni o la disponibilità di ogni formato. Versioni e dipendenze vanno lette nei file del branch, non copiate da questo documento.

Motivare le scelte con comportamento e risorse reali; distinguere causa riscontrata, ipotesi e proposta. Preservare il lavoro locale secondo AGENTS.md: GitHub non è una fotografia completa del worktree.

## Letture solo quando pertinenti

| Attività | Fonte da aprire | Natura |
|---|---|---|
| Modifica di una funzione | [Requisiti](docs/project-requirements.md), solo l'area coinvolta, poi sorgenti/test | Obiettivi e contratti da verificare |
| Confini fra UI, coordinatori e servizi | [Architettura UI](docs/ui-architecture.md) | Mappa descrittiva del codice locale |
| Grafica/UX | [Riferimenti visivi](docs/design-references/README.md); per Foto [migrazione Compose](docs/foto-compose-migration.md) | Specifica desiderata e prototipo, separati |
| Scelta/esecuzione dei test | [Test locali](docs/local-testing.md) | Comandi e perimetro delle suite |
| Copertura o modifica dei test | [Guida del catalogo](docs/testing/README.md), poi sole schede interessate | Contratti statici e corrispondenza ai sorgenti |
| Risultati precedenti | [Esecuzioni documentate](docs/testing/esecuzioni.md) | Evidenze storiche, non nuovi esiti |
| Build/CI | Configurazioni locali; [controlli CI](docs/build-checks.md), [selettore CI](docs/ci-checks.md) solo se coinvolti | Implementazione e procedure del branch |
| Code, file e recupero | [Sessioni](docs/saved-session.md), [pausa/salto](docs/pause-and-skip.md), [risparmio/riprova](docs/savings-and-retry.md) | Note delle singole funzioni, da confrontare con motore e test coinvolti |
| Metadati e formati | [Report](docs/metadata-report.md), [SEF](docs/samsung-sef.md), [date Video](docs/video-dates.md), [archivio Audio](docs/audio-application-archive.md) | Note per formato/versione, non garanzie universali o per tutti i media |
| Origine di una decisione | [Cronologia README](README.md#cronologia-tecnica), documenti della funzione o Git | Storia/proposte, non lettura iniziale obbligatoria |

Il mock HTML descrive una direzione visiva, non il comportamento Android; gli allegati mancanti sono elencati nella specifica grafica. Il catalogo può descrivere uno snapshot locale diverso dai sorgenti pubblicati: controllare la guida e la coerenza locale, senza alterare codice o impronte soltanto per eliminare la differenza.
