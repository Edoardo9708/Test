# Log compatti

Da v0.48 il log di conversione conserva i parametri, le macro procedure, gli avvisi, gli errori e gli stack completi. Solo una lista esplicita di successi ripetitivi viene campionata: chiamate per fotogramma, pacchetti, audio, metadati temporizzati e verifica. Le righe sconosciute vengono conservate.

`CompactLog` conserva i primi due esempi per categoria e poi emette riepiloghi circa ogni 15 secondi, con il numero di messaggi raggruppati e gli ultimi valori per categoria. Mantiene in memoria gli ultimi 24 messaggi ripetitivi omessi, con campioni limitati a 2048 caratteri, e li emette quando arriva un avviso o errore. I risultati negativi del codec non vengono filtrati. I riepiloghi vengono svuotati anche alle macro fasi e alla fine.

Nessun fotogramma, pacchetto, confronto di byte o timestamp viene saltato: il filtro agisce solo sul testo. La UI continua ad aggiornare avanzamento, fotogrammi e secondi indipendentemente dalle righe scritte.

## Log già esistenti

**Log compatto · TXT** rilegge il vecchio log sequenzialmente e prepara una nuova copia compatta per il selettore Android. Non carica l'intero file in memoria e non modifica il log originale. Riconosce i timestamp delle vecchie righe, i riavvii del tempo tra video e conserva gli errori con il nome del file. Anche un log interrotto senza newline finale viene esportato.

Il log batch rimane nei file privati dell'app dopo la morte del processo. Una nuova coda sovrascrive il log batch precedente, come già prima: per recuperare l'errore precedente occorre esportarlo prima di avviarne un'altra. Aggiornare l'APK senza disinstallare conserva questi file. La cancellazione volontaria dei log o dei dati dell'app li elimina.

Il log per file è scritto e flushato a ogni messaggio mantenuto; il batch viene flushato alle macro fasi e alla prima scrittura dopo almeno 5 secondi dall'ultimo flush. Un arresto improvviso può perdere gli ultimi messaggi ancora in buffer; il contesto dettagliato conservato solo in memoria non sopravvive alla morte del processo. La notifica non garantisce che Android mantenga vivo il processo.

Il log HDR diagnostico dei soli sei fotogrammi e l'esportazione dedicata dei metadati restano integrali.

## Verifiche

Test: 300.000 messaggi ripetitivi, negativi del codec, errori e righe nuove conservati; vecchio log di otto video con timestamp che ripartono; HDR multilinea e campioni grandi; fine improvvisa; stream con available=0. Test Android: riapertura con log sul disco, esportazione compatta tramite ACTION_CREATE_DOCUMENT, originale non modificato e snapshot cancellato se si annulla. Prova locale di 2,1 milioni di righe con heap massimo 64 MiB.

Il log aggiunge dimensione del temporaneo e conteggi frame/pacchetti prima della copia metadati, per aiutare la diagnosi del precedente errore moov. Questa versione non dichiara risolta la causa di quell'errore, ancora da analizzare sul TXT completo.

## v0.49: righe HDR mancanti e ZIP

Il file reale allegato mostrava ancora circa 31,4 MB di righe FORMATO P010 e OK VERIFY HDR SEI. Entrambe le categorie vengono ora campionate; i controlli e i confronti SHA-256 restano eseguiti su ogni fotogramma. Il test di regressione include queste due forme.

Esporta log propone ZIP compatto oppure TXT compatto. Lo ZIP contiene un solo TXT UTF-8 con le stesse procedure, diagnostica cartelle ed errori dell'opzione testo; la compressione ZIP non elimina informazioni. Il selettore usa application/zip e nome .zip. Cancellazione, salvataggio e pulizia degli snapshot supportano entrambe le estensioni; i log HDR diagnostici restano TXT. Il menu viene chiuso alla distruzione della schermata.

Il controllo moov ora riporta conteggio effettivo, byte del file e i primi 16 blocchi con offset e dimensioni: evita il vecchio messaggio generico e aiuta la diagnosi di un temporaneo non finalizzato. Il vincolo non viene rimosso e non si dichiara risolta la causa dell'errore del contenitore.
