# Data di creazione quando il video originale non la dichiara

La regola si applica automaticamente durante la copia dei metadati.

1. Si legge la creazione dal box `mvhd` dell’originale. Un valore diverso da zero viene conservato.
2. Se il valore è zero (epoca MP4: 1 gennaio 1904), si controlla anche la data esposta da MediaMetadataRetriever. Una data valida già presente in altri tag viene conservata; un formato sconosciuto non viene sostituito automaticamente.
3. Solo quando la data manca o è il segnaposto 1904 si legge `MediaStore.MediaColumns.DATE_ADDED` dell’elemento originale risolto in MediaStore.
4. `DATE_ADDED` è in secondi dal 1970: viene convertito nell’epoca MP4 aggiungendo 2.082.844.800 secondi. Non è una data di ripresa certa: spesso indica il download, ma può essere l’aggiunta dopo un trasferimento o ripristino.
5. Nella copia si riempiono esclusivamente i campi di creazione a zero di `mvhd`, `tkhd` e `mdhd`. Date valide, date di modifica, lingua e blocchi descrittivi originali restano conservati. Il file originale e il suo database Android non vengono modificati.
6. La verifica dei metadati usa la stessa regola esplicita e confronta i campi attesi. Inoltre si rilegge la creazione dal nuovo MP4 e dal lettore Android: devono concordare con la data recuperata prima che il file venga pubblicato.

Se DATE_ADDED non è disponibile o il provider non espone un elemento MediaStore, il log lo segnala e non inventa una data.

## Moduli

- `CreationDatePolicy`: classificazione della data e conversione delle epoche, senza API Android.
- `InputCreationDate`: lettura della data originale dal framework e da MediaStore; verifica della data esposta nel risultato.
- `Mp4Metadata`: applicazione circoscritta ai campi di creazione mancanti e verifica dei byte; l’API precedente mantiene il comportamento originale.

## Log

Cercare `DATA DI CREAZIONE`, `DATA HEADER ORIGINALE`, `DATA METADATI ORIGINALE`, `DATA SCELTA` e `OK VERIFICA DATA`. Sono riportati valore originale, provenienza del recupero, UTC e risultato del controllo.

## Limiti

Questa regola scrive la data all’interno del nuovo file, così è disponibile dopo una nuova scansione o un trasferimento. Non cambia DATE_ADDED del nuovo elemento Android, che è gestito dal sistema. Una vista della galleria ordinata per aggiunta recente può comunque considerare il risultato un nuovo file; l’ordinamento per data del video può usare il metadato corretto. Va verificato sul telefono Samsung.
