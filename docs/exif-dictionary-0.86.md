# Dizionario EXIF 0.86

## Copertura e fonti

165 voci: 128 identificativi della tabella EXIF evidenziati come standard (escluso InteropIndex, conteggiato in Interop), tutti i 32 tag GPS e 5 tag di interoperabilità (InteropIndex, InteropVersion, RelatedImageFileFormat, RelatedImageWidth, RelatedImageHeight).

Riferimenti fattuali consultati il 20 ottobre 2026:
- https://exiftool.org/TagNames/EXIF.html
- https://exiftool.org/TagNames/GPS.html
- Copia ufficiale della documentazione nel repository ExifTool: https://github.com/exiftool/exiftool/blob/master/html/TagNames/EXIF.html
- Snapshot EXIF: blob b59c1e49557349949cb9f3e600871393d59cd18d.
- Snapshot GPS: blob ff2bf21bff51176d9231872e9b3c34e912486bda.

Il catalogo EXIF consultato identifica i nomi sottolineati come appartenenti a EXIF 3.1. La lista di ID e nomi tecnici è fissata in app/src/test/resources/exif-standard-ids.txt: il test controlla che ogni voce abbia nome e spiegazione italiana. Le descrizioni italiane sono originali; nessun testo descrittivo inglese è incorporato. Il manifest contiene solo identificativi e nomi tecnici, con MakerNote normalizzato al nome generico.

La pagina ExifTool include anche estensioni TIFF, DNG e proprietarie: queste non sono incluse nella dichiarazione di copertura. Riconoscere tutti gli ID del catalogo standard non equivale a decodificare tutte le strutture binarie previste dallo standard, né a supportare tutti i formati TIFF.

## Interfaccia

Nome italiano nella mappa e nel popup. Significato specifico e nome tecnico nella descrizione, compresi i nodi dei valori esterni. Restano visibili ID, tipo, quantità, byte reali e calcolo della posizione.

42 tabelle di codici con evidenziazione del codice presente. La tabella di Compression e alcune estensioni TIFF/colore sono intenzionalmente parziali e la UI le chiama “valori documentati”, senza affermare che siano esaustive. Interpretazione applicata solo se tipo e quantità corrispondono allo schema atteso; i codici sconosciuti restano espliciti.

Interpretazioni dedicate: bit del flash, versioni ASCII in UNDEFINED, componenti YCbCr/RGB, origine file e scena, commenti con prefisso ASCII, tempi/aperture APEX, unità fisiche, coordinate del soggetto, quattro parametri dell’obiettivo, conteggi delle immagini composte, due codici dello sviluppo, coordinate GPS in gradi e ora UTC. Le coordinate GPS non vengono combinate con Ref letto altrove: il popup mostra il valore assoluto e il tag necessario al segno. Analogamente velocità e distanze con unità in un altro tag non ricevono un’unità indovinata.

MakerNote, OECF, SFR, CFA, impostazioni dispositivo, preferenze AI, esposizioni della composizione e testi con codifiche non implementate hanno spiegazione della funzione e stato di decodifica esplicito; nessuna lettura arbitraria del payload. Il lettore continua a gestire i tipi TIFF classici 1–12; altri tipi restano non interpretati.

## Organizzazione e verifica

- ExifTagDictionary: definizioni, nomi e tabelle, senza I/O.
- ExifValueInterpreter: interpretazioni specifiche, letture limitate dal reader.
- ExifAnalysisParser: struttura e contesto della directory; evita di seguire falsi puntatori EXIF dentro GPS/Interop.
- AnalysisDetailReader: anatomia TIFF e verifica dello schema prima di leggere.
- AnalysisLabels: presentazione collegata allo stesso dizionario.

Test con veri buffer TIFF in entrambi gli ordini byte, directory GPS e Interop, valori esterni, tipi/quantità incoerenti, codici sconosciuti, denominatori nulli, flash, coordinate, versioni e payload opachi. Restano attivi i test precedenti di popup e sola lettura. Nessuna modifica al motore di conversione.
