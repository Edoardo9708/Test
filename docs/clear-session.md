# Svuota tutto

Il pulsante accanto a Seleziona video svuota la selezione e la sessione conclusa: video, carosello, metadati in memoria e dettagli aperti, scelte di destinazione della coda, URI dei risultati, avanzamento, parametri attivi, resoconto ed errori per file. Rimuove anche la lista persistente Riprova falliti e la notifica della coda conclusa.

Non elimina originali, risultati salvati, log TXT, autorizzazioni delle cartelle o impostazioni. I controlli di qualità/preset/CPU conservano i valori attuali. La cartella generale/default rimane disponibile per la selezione successiva; le associazioni e i permessi già concessi restano validi. Le misure di sistema continuano ad aggiornarsi.

`HomeReset` coordina la pulizia di modello e viste. `ConversionService.clearSessionIfIdle` rifiuta una sessione in corso, anche in pausa, poi sostituisce lo stato con uno vuoto e invalida il replay persistente. La rimozione del replay aggiorna subito le preferenze in memoria e usa apply per il salvataggio asincrono. I vecchi callback del servizio leggono lo stato nuovo e non ripristinano il report.

Le generazioni di metadati, cache e animazione invalidano i risultati precedenti; il pannello cartelle riceve una selezione vuota e invalida le proprie letture pendenti. Non si chiudono gli executor riutilizzabili: la Home può selezionare e convertire altri video.

Il pulsante è disabilitato a schermata vuota, durante conversione o pausa, lettura metadati, selezione cartelle, esportazione e cancellazione log. Anche la chiamata diretta al controller ricontrolla i blocchi prima di modificare lo stato.

Test: cancellazione UI e stato concluso con errore; originali/risultati/log e preferenze conservati; cache vecchia rifiutata; report non ripristinato da renderSession o ricreazione Activity; selettore nuovamente utilizzabile; rifiuto durante pausa e lettura metadati; rendering della Home svuotata. Nessuna modifica al percorso di codifica.
