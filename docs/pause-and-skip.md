# Pausa e salto del file attuale

Durante una coda compaiono **Pausa / Riprendi** e **Salta video**. Il salto richiede conferma con nome e numero del file; la conferma vale solo per quella sessione e quel file, mai per il successivo se nel frattempo il primo termina.

Il salto conserva l'originale e i risultati già completati. Interrompe il lavoro, elimina i temporanei e l'eventuale documento di destinazione incompleto. L'esito **Saltato dall'utente** ha un contatore separato; non entra nei fallimenti, nel risparmio o in Riprova falliti. Il salto tardivo è rifiutato dopo il punto atomico di completamento.

Se il provider rifiuta la cancellazione o un temporaneo non può essere rimosso, il file risulta fallito con il dettaglio di pulizia, anziché dichiarare un salto completato. La pulizia tenta anche gli altri temporanei.

## Pausa cooperativa

`ConversionControl` è Java senza Android, separato da servizio e UI. Il worker si ferma ai punti di controllo con wait/notifyAll, senza polling. Una chiamata x265 o un'operazione del provider deve terminare: la UI distingue **Pausa richiesta** da **Conversione in pausa**. I lavori già inviati ai thread interni del codec possono terminare prima che la CPU si stabilizzi.

Il controllo copre analisi, SDR/HDR, svuotamento del codec, audio/metadati, verifiche e copia finale. I watchdog dei decoder usano il tempo attivo: la pausa non attiva il timeout di 60 secondi. Il tempo trascorso della coda include invece le pause.

Il servizio resta foreground, con notifica permanente e azione **Riprendi** o **Pausa**. Durante la pausa riconosciuta rilascia wake lock e mantenimento dello schermo acceso; li ripristina alla ripresa quando previsto. La notifica non garantisce la sopravvivenza del processo né elimina i limiti Android. Non si riprende dopo morte del processo.

**Salta** durante la pausa sveglia il worker per interrompere e pulire; il prossimo file resta in pausa. Annullamento coda, timeout e distruzione del servizio svegliano sempre il worker sospeso.

## Moduli e test

- ConversionControl: sincronizzazione, tempo sospeso, salto legato al file, completamento atomico.
- HomePlaybackControls: presentazione, comandi di sessione e conferma.
- ConversionCleanup: temporanei e tipo esplicito di pulizia fallita.
- ConversionService: coda, notifica, wake lock, contatori e log.

Test concorrenti: pausa/ripresa, tempo sospeso, salto da pausa, prossima pausa, comandi vecchi, annullamento e completamento. Test Android simulati: notifica, wake lock, salto escluso dai falliti/riprova; conferma UI; callback SDR/HDR; pulizia precoce e pulizia fallita. Rendering Home attiva/in pausa.
