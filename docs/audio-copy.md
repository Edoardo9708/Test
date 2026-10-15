# Copia AAC e timestamp prima dello zero

`AudioPacketCopier` è separato dalla codifica video. Legge finché MediaExtractor restituisce un indice di traccia valido: un timestamp negativo, compreso -1, non è usato come segnale di fine.

Il modulo conserva ogni pacchetto e il suo timestamp originale. Non scarta una percentuale iniziale, non azzera i tempi e non sposta soltanto l’audio. Il controllo di ordine usa la presenza del pacchetto precedente, anziché inizializzare il precedente a -1.

Il lettore Android e MediaMuxer vengono collegati da `VideoConverter.copyAudio`. Un errore del muxer riporta numero del pacchetto e timestamp originale; non viene nascosto eliminando l’audio. Al termine la verifica esistente rilegge tutti i pacchetti originali e salvati, confrontando byte, numero di campioni e timestamp (tolleranza già esistente: 1000 µs per arrotondamenti del contenitore).

## Diagnostica

- `CONTROLLO INIZIO AUDIO`: indice, primo timestamp e dimensione del pacchetto.
- `DIAGNOSTICA PRIMI TIMESTAMP AUDIO`: primi 12 pacchetti, tempo con segno e numero di byte.
- `OK copia audio`: conteggio totale, conteggio dei tempi negativi, primo/ultimo tempo e byte copiati.
- `VERIFY TIMESTAMP TRACCIA`: primi 12 confronti tra originale e MP4 finale; il confronto del contenuto continua su tutti i pacchetti.

## Verifiche e limiti

I test Java usano sorgenti e destinazioni simulate per verificare copia dei byte, timestamp negativi/-1/zero/positivi, nessun riallineamento, ordine, cancellazione, letture incomplete, tracce vuote, pacchetti eccessivi, audio protetto ed errori della destinazione. Non simulano il comportamento del muxer Samsung: serve la prova sul telefono con i file che hanno fallito. Il log precedente dimostra un errore nella fase audio, ma non espone il primo timestamp; questa versione rende verificabile l’ipotesi iniziale.

Riferimento al framework consultato: AOSP `frameworks/base/media/java/android/media/MediaMuxer.java` e `frameworks/av/media/libstagefright/MPEG4Writer.cpp` (gestione dei tempi iniziali negativi tramite offset ed edit list). La copia conserva i timestamp e richiede la verifica del risultato, senza presumere che ogni versione/provider si comporti allo stesso modo.
