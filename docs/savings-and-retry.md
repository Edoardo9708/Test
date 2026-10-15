# Salva solo se più piccolo e riprova i falliti

## Confronto prima della pubblicazione

La checkbox nella sezione Conversione è attiva per impostazione predefinita e viene ricordata. La scelta viene fotografata all’avvio della coda e non può cambiare i file in corso.

`SavingsPolicy` confronta i byte dell’originale aperto senza oscuramento con la dimensione dell’MP4 temporaneo completo, dopo copia dei metadati e verifica di video/audio. Il nuovo file viene creato nella cartella scelta solo se è strettamente più piccolo. Anche un risultato di dimensione identica viene escluso. Se la dimensione originale non si può recuperare, con l’opzione attiva non si pubblica il risultato; disattivandola si conserva il comportamento precedente.

Il temporaneo viene eliminato con la pulizia già prevista. Il risultato non salvato ha un esito separato dai fallimenti: `NO_SAVING` oppure `SIZE_UNKNOWN`. Il resoconto mostra originale e candidato; questi candidati non entrano nel totale dei file salvati né nel risparmio effettivo. L’originale non viene eliminato.

## Ripetizione dei fallimenti

`BatchRetry` seleziona esclusivamente gli indici che il servizio ha registrato come falliti. Conserva l’associazione di ciascun URI con nome sorgente, cartella, nome output approvato, scelte di destinazione e parametri della coda. Ignorati, senza risparmio, annullati e ancora in attesa non vengono aggiunti.

L’elenco dell’ultima coda viene salvato nelle preferenze private dell’app, così resta disponibile dopo la chiusura del processo. All’avvio di una nuova coda viene sostituito dai fallimenti di quella nuova coda; un tentativo che riesce o non produce risparmio non resta tra i falliti.

`HomeRetry` mostra il conteggio e la conferma. Riprova con i parametri e le cartelle della coda originale, non con eventuali nuovi valori lasciati nei controlli della Home. La selezione modificabile della Home resta disponibile per la prossima coda manuale; il riquadro Parametri in uso mostra i valori effettivi del servizio. Prima del rilancio controlla i permessi video/GPS e ripete il controllo dei nomi nella destinazione. I conflitti richiedono ancora la scelta tra rinomina e ignora. Una cartella che ha perso i permessi deve essere nuovamente autorizzata; non viene sostituita con un’altra destinazione.

I permessi richiesti durante una ripetizione ripristinano la richiesta pendente tramite Bundle se la schermata viene ricreata.

## Moduli e verifiche

- `SavingsPolicy`: decisione di salvataggio e risultato distinto da errore.
- `MediaFileSizes`: lettura della dimensione dal descrittore o dal provider.
- `BatchRetry`: sottoinsieme coerente, nomi approvati e salvataggio privato con schema controllato.
- `HomeRetry`: conferma, permessi e avvio tramite il controllo nomi esistente.

Test: file più piccolo/uguale/più grande e dimensioni ignote; esclusione dei candidati dai totali; selezione e ripristino dei falliti con cartelle e nomi corretti; stato della checkbox; avvio dalla Home con nuovo controllo nomi; servizio reale simulato che distingue un file ignorato da un errore prima della codifica; immagini sintetiche della pagina e del resoconto.
