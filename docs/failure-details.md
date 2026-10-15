# Errori per file nel resoconto

Il pulsante rosso `!` accanto a ogni conversione fallita apre l'errore di quel file. È disponibile anche durante la coda, appena il file fallisce. Ignorati, annullati e risultati senza risparmio non mostrano il pulsante.

Il popup mostra nome e numero del video, messaggio dell'eccezione e ultimo stato comunicato dalla conversione. Quest'ultimo è un contesto, non la garanzia della precisa funzione che ha fallito. I dettagli tecnici espandibili contengono lo stack completo, incluse cause e eccezioni soppresse.

**Copia tutto** copia versione app, file, messaggio, ultimo stato e dettagli tecnici, anche se richiusi. Il popup resta aperto. Il medesimo testo è scritto nel TXT della coda.

## Confini dei moduli

- `ConversionFailure`: dato immutabile catturato dal worker e testo condiviso fra log e clipboard.
- `ConversionService`: registra solo errori effettivi per file; snapshot con lista indipendente, azzerata alla nuova coda.
- `HomeFailureReport`: rende il resoconto con azione accanto alla riga fallita, apre il popup e copia il testo. Non cerca errori nel log e non legge file sul thread UI.
- `HomeConversionSection`: crea il contenitore delle viste; `HomeConversion` delega la presentazione.

Il resoconto e i dettagli restano disponibili per la durata del processo, anche ricreando la schermata. Per conservarli dopo la chiusura del processo si può esportare il TXT; la coda Riprova falliti mantiene la propria persistenza già esistente.

Verifiche: due errori distinti aprono/copia quello selezionato; cause incluse nella copia a dettagli chiusi; popup non chiuso dalla copia o dal refresh; lista indipendente negli snapshot; errore senza messaggio; errore reale prima della codifica presente anche nel TXT; rendering chiaro/scuro. La codifica nativa non è modificata.
