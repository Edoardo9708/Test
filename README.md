# v0.94 · scorciatoie qualità foto e icona di notifica

I tre pulsanti rapidi della qualità immagini sono ora 50, 55 e 60, sia nella schermata di conversione sia nelle impostazioni immagini. La notifica del servizio foto usa un’icona monocromatica dedicata con un’immagine, coerente con le icone specifiche già usate da video e audio.

# v0.93 · conserva il riempimento dopo la fine JPEG

Il campione originale rifiutato contiene 172.637 byte dopo il marcatore di fine JPEG: undici byte D9 e 172.626 byte nulli. La conversione accetta ora code di riempimento formate da una sequenza iniziale di D9 seguita esclusivamente da byte zero (anche sequenze contenenti un solo tipo di byte), fino a 4 MiB. Il riconoscimento verifica tutta la coda, non soltanto un prefisso. Altri dati continuano a seguire i controlli SEF o a essere rifiutati come non identificati.

L’intera coda rimane nel blocco proprietario di metadati del file HEIC: nessun byte viene eliminato per risparmiare spazio. Prima della pubblicazione viene riletta e confrontata insieme agli altri metadati originali. Non viene attivata alcuna funzione Motion Photo. Gli originali restano invariati. Questo campione conferma una causa possibile dei precedenti 375 rifiuti, non dimostra che tutti abbiano lo stesso schema.

Test nativi: schema osservato, varianti corte, solo zeri, solo D9 e limite esatto di 4 MiB; rifiuto di byte diversi, D9 dopo lo zero e dimensione oltre limite; conservazione della coda completa e rifiuto di un archivio alterato. La foto privata resta fuori dal repository; i test riproducono soltanto lo schema di riempimento. Prova locale sul campione originale con il motore nativo e le librerie incluse: qualità 65, CRF 17,5, preset slow; 8.506.036 → 6.365.820 byte (−25,2%), verifica completa di HEIC/metadati/coda riuscita, SHA-256 dell’originale invariato.

# v0.92 · raffiche fotografiche SEF e diagnostica degli input rifiutati

Il record foto `Burst_Shot_Info`, tipo SEF `0x09e0`, viene conservato integralmente nell’archivio proprietario JPEG del file HEIC e confrontato byte per byte alla rilettura. Il payload resta opaco, anche binario, vuoto o terminato: nessuna normalizzazione e nessuna ricostruzione della raffica nell’editor Samsung. Il parser SEF dei video resta invariato; Motion Photo e altri contenuti multimediali aggiuntivi non diventano automaticamente supportati.

Per code JPEG non riconosciute, l’errore include lunghezza, primi 32 e ultimi 16 byte in HEX, senza diagnosticare una Motion Photo sulla sola presenza di dati in coda. Gli errori di orientamento riportano il valore fuori intervallo oppure tipo/quantità o duplicazione del tag; riferimenti SEF non validi riportano indice e limiti del record; un marcatore JPEG inatteso riporta offset e byte incontrato. I controlli strutturali restano attivi.

Test nativi: quattro versioni SEF con payload testuali, binari, terminati e vuoti della raffica, insieme a selfie e MCC; conservazione integrale e rifiuto di una copia alterata; tipo/nome sbagliati; diagnostica limitata per code brevi/lunghe e orientamenti 0/9. Il log utente 0.88 contiene 98 rifiuti per Burst_Shot_Info: la correzione gestisce questo record, ma soltanto un nuovo tentativo può escludere ulteriori caratteristiche non supportate nelle stesse foto.

# v0.91 · pausa e ripresa della coda immagini

Il pulsante «Pausa» è nella sezione conversione immagini, sotto «Converti e salva» e «Annulla», ed è disponibile anche nella notifica. La richiesta lascia completare la foto in corso, compresi verifica, salvataggio, registro ed evento nel log; il worker si sospende prima della foto successiva. Durante l’attesa il comando diventa «Riprendi»: continua la stessa coda con gli stessi contatori, senza reimportare file o riavviare le foto completate. Una ripresa richiesta prima dell’effettiva pausa ritira la richiesta.

La pausa mantiene il servizio e la coda, rilascia il wake lock e permette al display di spegnersi. Log e archivio errori rimangono esportabili. «Annulla» risveglia anche un worker già in pausa e termina la coda. I comandi sono associati all’esecuzione corrente: notifiche vecchie non possono controllare una nuova coda. Una pausa in memoria non sopravvive alla terminazione del processo e non estende i limiti Android sul servizio.

Verifiche aggiunte sul servizio reale con sorgenti sintetiche: completamento della foto in corso e arresto prima della successiva, stessa coda e contatori dopo la ripresa, annullamento e distruzione del servizio durante l’attesa, revoca di una richiesta di pausa, rilascio del wake lock, comando in notifica, rifiuto dei comandi di una coda precedente, pulsante dopo ricreazione della schermata ed esportazione del log in pausa. I test non avviano il codec: il punto di pausa è tra due conversioni complete, senza cambiamenti al motore nativo.

# v0.90 · log parziale durante la conversione e contatori nel sommario

«Esporta log» e «Esporta archivio errori» sono disponibili anche con una coda foto in corso. Alla richiesta viene preparata su un worker dedicato una copia locale chiusa e coerente: le scritture attendono soltanto durante questa copia. Appena è pronta, la registrazione riprende sul file originale; la scelta della destinazione e la scrittura del TXT/JSONL esterno non mantengono il blocco. Nessun evento viene scartato, nessuna coda viene riavviata e il log originale non viene svuotato. La copia usa un buffer da 64 KiB e un file temporaneo su disco, senza caricare l’intero log in RAM.

Il nome segnala «parziale» se la coda era attiva alla richiesta. Include gli eventi già registrati, non l’esito di una foto ancora in codifica. Il temporaneo viene eliminato dopo esportazione, annullamento o errore; una scelta di destinazione in corso sopravvive alla ricreazione della schermata. Durante una singola esportazione i pulsanti evitano richieste duplicate; «Cancella log» resta disabilitato durante la conversione.

Il sommario delle foto mostra subito riuscite (nuove foto salvate), fallite, già presenti, senza risparmio e annullate, oltre all’indice corrente e alle completate. I contatori restano cumulativi anche quando i dettagli più vecchi escono dalla finestra di 100 risultati.

Verifiche aggiunte: esportazione durante la conversione alla foto 600 su 8.500, contatori oltre la finestra di 100 dettagli, copia invariata dopo nuove scritture o nuova coda, scritture concorrenti senza eventi parziali o persi, destinazione lenta che non blocca gli eventi, ricreazione della schermata, annullamento, errore e nuovo tentativo, archivio JSONL con record completi.

# v0.89 · conserva il record SEF delle foto frontali

La conversione JPEG → HEIC accetta `Front_Cam_Selfie_Info` con tipo SEF `0x0910`. Il payload viene conservato integralmente come dato opaco nell’archivio proprietario già presente nel file HEIC: non viene decodificato, normalizzato o applicato ai pixel. Non richiede una specifica stringa ASCII: sono conservati anche byte binari, terminatori e payload vuoti strutturalmente validi. Il lettore SEF dei video resta invariato.

Restano i controlli sulla struttura SEF, sulle versioni 103/105/106/107, sugli offset e sulle dimensioni, con limite complessivo di 4 MiB. Prima del salvataggio viene riletto e confrontato l’intero archivio con i byte originali. Altri record sconosciuti e contenuti HDR/Motion Photo non supportati continuano a essere segnalati. La conservazione non garantisce che la galleria Samsung utilizzi il dato archiviato.

Test nativi sintetici: quattro versioni SEF con varianti testuali, binarie, terminate e vuote; copia integrale della coda; rifiuto di tipo/nome errati e di un archivio alterato. La foto originale segnalata non è inclusa nei test perché è disponibile soltanto il messaggio d’errore. Dopo l’aggiornamento si possono riprovare gli ultimi fallimenti con «Carica fallite».

# v0.88 · importa cartella JPEG e registro degli esiti

«Importa cartella JPEG» apre il selettore di cartella Android una sola volta. L’app elenca in background tutti i JPEG direttamente contenuti nella cartella, senza leggere i pixel e senza includere sottocartelle, HEIC o video. Il permesso persistente sulla cartella copre gli originali elencati. La selezione importata può superare 5.000 elementi: non occorre dividerla manualmente in lotti. Il limite manuale resta 5.000; l’importazione ha una protezione a 100.000 riferimenti e il file di passaggio al servizio resta limitato a 32 MiB. UI e storico dettagliato restano limitati a 100 elementi.

Se non è già impostata una destinazione, per i file importati viene proposta la cartella selezionata. Le destinazioni rimangono visibili e modificabili prima di premere «Converti e salva». La scansione elenca nomi e riferimenti: i metadati della singola anteprima restano asincroni e ogni nuovo originale è controllato prima della propria codifica.

Un registro SQLite privato, image-progress.db, conserva un solo esito aggiornato per URI originale: nome, destinazione, impostazioni, stato, dimensioni, impronte SHA-256 di originale e risultato ed eventuale URI del risultato. Nessun pixel, payload EXIF o dettaglio tecnico viene scritto nel registro; i dettagli degli errori restano nel JSONL e nel log separati. Il registro si consulta una voce per volta, senza caricarlo tutto in RAM.

Durante una nuova esecuzione, prima di evitare una ricodifica l’app rilegge originale e HEIC a blocchi da 64 KiB e confronta entrambe le impronte con il registro, oltre alle impostazioni di conversione e alla destinazione. Un originale o risultato modificato, eliminato o non più leggibile non viene considerato già completato. Gli esiti «nessun risparmio» possono essere riutilizzati solo con originale e impostazioni invariati. Una modifica della sola opzione schermo acceso non forza la ricodifica. I risultati già presenti sono conteggiati separatamente e contribuiscono una sola volta ai totali dell’esecuzione corrente.

«Carica fallite» ripristina in selezione tutti gli ultimi fallimenti del registro, con le destinazioni conservate. Le impostazioni di compressione restano quelle attuali, visibili prima dell’avvio. Un nuovo successo sostituisce l’esito fallito nel registro; il JSONL mantiene lo storico tecnico. Le conversioni precedenti all’introduzione del registro non vengono considerate riuscite soltanto perché esiste un HEIC con lo stesso nome.

Verifiche aggiunte: cartella sintetica con 8.500 JPEG e file di altri tipi; un solo risultato del selettore di cartella produce 8.500 riferimenti e 100 pulsanti; salvataggio/ripristino della selezione grande; verifica delle due impronte anche dopo modifiche a parità di dimensione; risultati mancanti; cambi delle impostazioni; cancellazione durante la verifica; sostituzione degli esiti e caricamento dei falliti; dimensione reale del DB per 8.500 record realistici (controllo sotto 20 MiB). Il valore misurato è stampato nel log CI come IMAGE_REGISTRY_8500_BYTES; non include selezione, file temporanei o log tecnici separati.

# v0.87 · coda foto fino a 5.000 elementi

La selezione JPEG è cumulativa: «Aggiungi JPEG» aggiunge i file senza duplicati, fino a 5.000. Il carosello mostra tutte le foto fino a 100; sopra tale soglia mostra le prime 100 e «…», con il conteggio totale. Le liste di audio e video conservano il comportamento precedente.

Il report foto ha al massimo 100 righe. Per le code grandi la finestra segue la foto corrente, al 50º posto quando sono disponibili abbastanza elementi prima e dopo; le righe successive indicano le foto in attesa. Agli estremi la finestra si adatta. I contatori e i byte delle foto salvate sono cumulativi e indipendenti dai risultati conservati: eliminare i vecchi dettagli non riduce il risparmio totale.

Solo gli ultimi 100 risultati hanno dettagli in RAM (massimo 16.384 caratteri di dettaglio per risultato). Il log completo dell’ultima esecuzione è su disco e non ruota durante una coda; la UI conserva soltanto una coda testuale di 24.000 caratteri. Ogni fallimento è inoltre salvato subito in un archivio persistente JSONL con sorgente, nome, destinazione, impostazioni, motivo e dettagli completi. «Esporta archivio errori» permette di conservarlo anche fuori dall’app. L’archivio non viene cancellato svuotando la selezione o avviando una nuova conversione; non implica una ripetizione automatica dei fallimenti.

L’elenco di lavoro viene scritto in un file privato e il servizio riceve solo il suo identificativo, evitando Intent con migliaia di URI. Le letture di anteprima e dei nomi sono asincrone e non bloccano l’avvio; vengono annullate quando si prepara la conversione. Ogni foto viene aperta e controllata prima della propria codifica dal motore esistente. Una lettura fallita produce un errore per quel file e la coda prosegue. I permessi delle destinazioni restano necessari prima dell’avvio. Se Android termina il processo, la conversione non riparte automaticamente.

Verifiche: 5.000 riferimenti salvati e riletti; 5.000 esiti con dettagli grandi e storico limitato a 100; somme a 64 bit dopo l’eliminazione dei dettagli; riga corrente nella finestra; 125 errori consecutivi attraverso il servizio; archivio errori indipendente dalla RAM; log oltre 2 MiB; avvio mentre l’anteprima è ancora in lettura. La prova Java isolata della coda e dei totali passa con heap limitato a 32 MiB: non misura il consumo dell’intera app Android o del codec.

# v0.86 · dizionario EXIF in italiano

L’analisi riconosce e descrive 165 voci: 128 tag del catalogo standard EXIF, 32 GPS e 5 di interoperabilità. Nome italiano, significato e nome tecnico sono collegati sia alle voci di directory sia ai valori esterni. I popup conservano la scomposizione dei byte e aggiungono 42 tabelle di codici con valore presente evidenziato, interpretazione dei bit del flash, unità, tempi e aperture APEX, coordinate e strutture semplici documentate.

Gli ID sono distinti per directory: un identificativo GPS non assume il significato di un tag EXIF o Interop con lo stesso numero. Tipo e quantità vengono controllati prima delle interpretazioni specifiche. Le strutture binarie non decodificate sono indicate esplicitamente: la copertura del dizionario non implica la decodifica completa di MakerNote, tabelle complesse o estensioni TIFF/DNG.

Dettagli, fonti e limiti: [docs/exif-dictionary-0.86.md](docs/exif-dictionary-0.86.md). Aggiunti test con buffer TIFF reali e controllo della copertura del catalogo. Il motore di conversione resta invariato.

# v0.85 · spiegazione dei campi in popup

Il tocco su un elemento non espandibile apre un popup scorrevole; il pulsante «Spiegazione di questo blocco» apre lo stesso dettaglio per il contenitore corrente. Il pannello descrittivo non è più collocato in fondo alla pagina. Il valore trovato compare subito in cima; seguono identificazione, schema dei byte, regole di lunghezza e significato.

Le voci EXIF conservano coordinate e schema TIFF (tag, tipo, quantità, ordine byte, origine degli offset) senza buffer dei file. Il popup rilegge la voce e verifica lo schema prima di spiegare ID a 2 byte, tipo a 2 byte, quantità a 4 byte e spazio valore/offset a 4 byte. Distingue dati inline, byte non usati e dati esterni raggiunti tramite offset relativo al TIFF. Decodifica anteprime limitate di numeri, testo ASCII e frazioni; i byte UNDEFINED restano opachi. L’orientamento include i valori 1–8, quello presente nel file e la distinzione tra trasformazione e forma verticale/orizzontale.

Le intestazioni ISO BMFF mostrano dimensione, ID ASCII, eventuale dimensione estesa e UUID; ftyp e ispe hanno campi specifici. I segmenti JPEG mostrano marcatore, lunghezza e firma (EXIF/XMP/ICC), distinguendo le regole di conteggio dai box. Sono descritti anche intestazione TIFF, blocchi FLAC e intestazione ID3. Payload e valori grandi sono mostrati solo in anteprima; HEX resta a pagine da 256 byte. Nessuna lettura dell’intero video. Chiusura e ricreazione annullano le letture pendenti.

Verifiche aggiunte su byte order, otto orientamenti e valori sconosciuti, ASCII esterno, cambio tag, dimensioni box, lunghezze JPEG, tipi TIFF sconosciuti, troncamenti; prove Android del popup, chiusura/riapertura, HEX e ricreazione, con anteprime chiare/scure.

# v0.84 · gerarchia e spiegazioni nella mappa Analisi

La mappa mantiene visibile il confine del file e disegna il contenitore aperto al suo interno, con i livelli intermedi e i comandi per risalire. Nei percorsi profondi abbrevia soltanto il disegno: il percorso completo resta scritto sopra. I riquadri hanno un nome in italiano, il nome tecnico originale, il ruolo (contenitore, dati o indice/collegamento), la dimensione e una breve spiegazione. La selezione evidenzia il blocco corrispondente.

Glossario di presentazione indipendente dai lettori: box comuni ISO BMFF, segmenti JPEG, struttura EXIF e principali tag, blocchi FLAC e ID3. Le spiegazioni distinguono una relazione tra elementi dalla presenza fisica dei dati: meta elenca e collega; mdat può contenere i payload; thmb è un riferimento alla miniatura. I valori non decodificati non vengono inventati. I dettagli distinguono significato, dati effettivamente letti e posizione in byte spiegata in parole semplici. I valori EXIF esterni mantengono anche l’identificativo del tag nel loro nome tecnico.

# v0.83 · prima bozza Analisi

Quarta scheda indipendente dalle conversioni. L’elenco riusa i riferimenti e i nomi selezionati in Video, Audio e Immagini, senza aprire i file; le informazioni già disponibili nelle cache video/foto vengono mostrate senza ricalcolarle. I riferimenti video sono locali al processo, coerentemente con la selezione video esistente. Le selezioni audio/foto restano nelle rispettive bozze.

Aprendo un file si legge la struttura su worker: segmenti/scansioni/coda JPEG; directory e tag EXIF TIFF; box ISO BMFF di HEIC/MP4/MOV/M4A con contenitori annidati noti; metadati FLAC; tag ID3v2 come blocco. Ogni rettangolo riporta dimensione e percentuale del contenitore corrente, con spazio leggibile non proporzionale ai byte. Percorso navigabile, dettagli, offset assoluti e vista HEX/ASCII opzionale a pagine di 256 byte. Nessun pixel viene decodificato, nessun file modificato. Le scansioni JPEG sono percorse a blocchi di 64 KiB per localizzare i marcatori; mdat e audio compressi vengono saltati.

Lettori puramente Java separati da sorgenti Android, stato/worker e vista. Limiti: 4.096 elementi per livello, 32 livelli nella UI e 32 directory TIFF; rendering progressivo a gruppi di 60 blocchi. Un provider deve consentire accesso seekable: nessuna copia implicita di video interi. Annullamento e generazione impediscono risultati tardivi dopo chiusura/cambio file. Dopo ricreazione si riapre il file dalla radice; nessun buffer viene serializzato. Per i video, con permesso posizione si richiede l’originale usando il flusso esistente; audio e foto usano la stessa URI delle rispettive sezioni. La schermata distingue il caso in cui il GPS originale non è verificato.

Bozza: non interpreta ancora record SEF, singoli item HEIF tramite iloc, frame ID3 o pacchetti audio. Tipi sconosciuti restano opachi con dimensione e HEX; non vengono interpretati per tentativi. La mappa è un inventario strutturale, non una verifica di convertibilità. Il comando Rileggi rinnova la lettura; modifiche esterne durante l’esplorazione richiedono rilettura (il cambio dimensione viene rilevato).

Verifiche dedicate su copertura dei byte JPEG, stuffing/restart, EXIF e cicli, box annidati, file sparse da 5 GiB, FLAC/ID3, troncamenti, annullamento; prove sui campioni reali S23 JPEG e HEIC. Test Android per elenco senza letture, navigazione/HEX, ricreazione, errore provider e integrità della sorgente; anteprime sintetiche chiare/scure.

# v0.77 · conversione JPEG → HEIC

La sezione Immagini converte fino a 100 JPEG in una coda foreground indipendente. Librerie incluse: libheif 1.23.6, x265 già usato dal progetto e FFmpeg 8.1.3, ora compilato anche con decoder MJPEG. Nessun decoder/codificatore Android è usato nel percorso foto. Selezione, bozza e impostazioni immagini sono separate; una sola conversione audio/video/immagini può usare il motore alla volta. La coda continua lasciando la pagina, ma non riprende dopo terminazione del processo. Annullamento al confine delle fasi: una singola codifica x265 non viene interrotta a metà.

Impostazioni immagini dedicate con salvataggio esplicito: cartella predefinita, qualità 40–95, preset fast/medium/slow, tune standard/ssim/psnr, solo se più piccolo e schermo acceso. Predefiniti iniziali: qualità 65, slow, standard, solo se più piccolo. I controlli principali modificano la bozza, non i predefiniti; una nuova revisione salvata si applica a coda ferma. Il servizio riceve uno snapshot immutabile. La qualità libheif usa CRF=(100−qualità)/2; non è una percentuale di qualità né una modalità senza perdita. Patch locale documentata al plugin x265: aggiunta tune none per richiamare il preset senza tune; upstream imposta SSIM come predefinito. Due thread di pool, un frame thread, CU-tree disattivo; formato HEIC Main Still Picture, 4:2:0, 8 bit, full range, nessun ridimensionamento. SharpYUV non è usato: i piani YCbCr sono trasferiti dal decoder JPEG, senza passare per RGB.

EXIF viene trasferito integralmente, comprese data/GPS/MakerNote e orientamento; irot/imir del contenitore rappresentano lo stesso orientamento e vengono verificati separatamente. Il comportamento di lettori che applicano entrambe le dichiarazioni richiede prova sul dispositivo. XMP standard e ICC ricostruito dai segmenti sono mappati nei campi HEIC corrispondenti. Primarie/trasferimento non dichiarati restano non specificati; matrice JPEG BT.601 riconosciuta e range completo vengono esplicitati. Libheif strict decoding controlla il contenitore e il flusso decodificato. Il salvataggio segue la riapertura, verifica di metadati/ICC/NCLX/trasformazioni e decodifica completa dell’immagine finale, incluso clap per dimensioni dispari.

Ogni APP/COM originale, in ordine, e il SEF supportato sono inoltre conservati byte per byte in un item MIME proprietario application/vnd.codecprobe.jpeg-metadata, formato CPJPEG1. I dati proprietari sono archiviati, non reinterpretati né resi automaticamente utilizzabili dall’editor Samsung. Il SEF ammesso contiene soltanto Image_UTC_Data (13 cifre) e MCC_Data (3 cifre), con directory e riferimenti verificati. Non si promette il ripristino delle funzioni proprietarie. Il report JSON diagnostico v1 resta un inventario JPEG, non un certificato di conversione: i byte dopo EOI hanno solo conteggio/hash nel report, mentre il SEF supportato è completo nell’archivio HEIC.

Prima versione limitata a JPEG YCbCr 8 bit, lati 64–16384 pixel, massimo 48 MP e file 64 MiB. Rifiutati: CMYK/RGB, grigi, precisioni superiori, HDR/gain map, Motion Photo, MPF, XMP esteso, APP11/JUMBF non nullo, SEF non supportato, ICC frammentato incoerente e input troncati. Non converte HEIC/PNG. Gli altri APP/COM restano dati opachi nell’archivio, senza semantica HEIF. Nessun originale viene sovrascritto. Il confronto dimensioni include tutti i metadati; il salvataggio crea nomi con suffisso e rilegge l’intero documento confrontando SHA-256, rimuovendo documenti nuovi incompleti o diversi.

Verifiche: JPEG reale Originale.jpg, con quattro APP/EXIF, orientamento 6 e SEF di 98 byte; codifica e decodifica completa riuscite. Suite nativa sintetica con EXIF/data/GPS, otto orientamenti, ICC, XMP, APP opachi, SEF, 420/422/444, dimensioni dispari, rifiuti previsti e alterazione dell’archivio. Suite Android su impostazioni, revisione durante coda, SAF, protezione da corruzione di uguale lunghezza, annullamento, nomi e navigazione; render di qualità, conversione e impostazioni. Restano necessarie prove sul dispositivo per visualizzazione in Galleria e provider reali.

# v0.76 · primo modulo Immagini: inventario JPEG

La terza scheda Immagini permette di selezionare fino a 100 JPEG, navigarli/rimuoverli e analizzarli su worker. Legge dimensioni, precisione, componenti, scansioni, tutti i payload APP0–APP15/COM (anche tra scansioni), EXIF/XMP/ICC riconosciuti per firma, sequenza dei frammenti ICC, dati dopo EOI e SHA-256 dell’intero originale. Nessun pixel viene decodificato e l’analisi non approva una conversione. File troncati o oltre i limiti restituiscono un errore esplicito. Limiti: file 512 MiB, metadati 16 MiB, 65.536 segmenti; cancellazione del worker e generazione proteggono cambio foto/chiusura.

L’export JSON v1 include offset, dimensione, SHA-256 e base64 integrale di ogni APP/COM. I dati dopo EOI hanno solo conteggio e SHA-256: non vengono inclusi come payload. Il report non è una copia della foto. EXIF, XMP e ICC non sono ancora validati semanticamente; data/GPS/orientamento non vengono ancora interpretati. Il report può contenere dati personali dei metadati. Selezione e indice hanno preferenze indipendenti; analisi e report in memoria vengono ricalcolati dopo ricreazione. La sorgente viene letta prima di aprire la destinazione del report e la stessa URI non può essere usata come output.

Questa versione NON converte foto e NON include ancora libheif. x265 è riutilizzabile per la compressione; il contenitore HEIC, il trasferimento verificato dei metadati e il percorso colore richiedono ancora implementazione e prove. I precedenti confronti foto erano HEVC in MP4 a un fotogramma: non dimostravano la produzione di HEIC. Qualità 60/65/70 non è una scala CRF; preset/tune/SharpYUV erano prove, non impostazioni definitive. Video e audio mantengono i rispettivi motori e sessioni.

Verifica reale su Originale.jpg (3.449.651 byte, 4032×3024): quattro blocchi metadati, un EXIF, nessun XMP/ICC, 98 byte aggiunti con record Samsung e footer SEFT. Non è ancora autorizzata la loro copia in HEIC. Test sintetici su scansioni progressive, escape e restart, dati opachi, ICC multiparte, hash, troncamenti, limite metadati, cancellazione, selezione/esportazione/ricreazione, protezione della sorgente e navigazione tra tre sezioni. Render Android in tema chiaro/scuro.

# v0.75 · schede audio/video in alto

La navigazione Audio/Video è composta da schede piatte in cima alla pagina, dentro lo stesso contenuto scorrevole: scendendo scompare insieme all’intestazione. Il passaggio tra sezioni e il ritorno da Audio usano una dissolvenza breve, senza movimento laterale. Le schede hanno larghezza uniforme e il componente consente di aggiungere una futura sezione Immagini; questa versione non aggiunge ancora la conversione immagini. Selezione corrente, sessioni e coda conservano il comportamento precedente.

Test Android su posizione delle schede, scorrimento fuori dalla vista, selezione, navigazione e animazioni. Il motore audio non cambia: gli input restano FLAC/MP3/M4A; i testi incorporati trasferibili come LYRICS sono conservati e verificati negli Opus prodotti. Gli Opus già scaricati non sono input supportati. Nessuna promessa di conversione universale di formati di testo sincronizzato proprietari.

# v0.74 · pagina Impostazioni audio dedicata

Il pulsante Impostazioni della schermata audio apre ora una pagina dedicata, ospitata da SettingsActivity in modalità audio. Contiene cartella predefinita, bitrate con scelte rapide, “Salva solo se più piccolo” e “Mantieni schermo acceso”. Le modifiche richiedono “Salva impostazioni”; Indietro le scarta. Il pulsante “Usa la configurazione della schermata conversione” importa le scelte correnti nel modulo senza salvarle automaticamente. Il selettore cartella usa lo stesso flusso isolato dei video e conserva la scelta precedente in caso di annullamento o permessi insufficienti.

I predefiniti audio sono separati sia dalle impostazioni video sia dalla bozza della coda. Fino al primo salvataggio si mantengono le scelte audio esistenti. Il salvataggio aggiorna la schermata audio inattiva; durante una conversione l’applicazione dei nuovi predefiniti viene rimandata alla fine, senza modificare lo snapshot del servizio. Selezioni di brani/testi e resoconti restano conservati. Il modulo non salvato viene ripristinato dopo ricreazione della Activity. Test su navigazione, salvataggio/annullamento, migrazione, separazione dalla bozza/video, cartelle, conversione in corso e render Android della nuova pagina.

# v0.73 · revisione del flusso audio

La schermata audio segue ora il flusso file/destinazione, compressione, conversione e diagnostica. Tutti i brani sono navigabili e rimovibili singolarmente; il brano selezionato ha anteprima di durata, dimensione e formato e dettagli su titolo/artista/album, frequenza e canali quando disponibili nelle API Android. “Qualità audio” apre la sezione corretta senza fingere una pagina di impostazioni separata. Le scelte rapide mostrano il bitrate selezionato. Log e dettagli delle librerie sono in Diagnostica; i testi esterni sono opzionali, con conteggio di abbinamenti, ambiguità e testi inutilizzati.

Prima dell’avvio vengono controllati cartella e abbinamenti su un worker; i parametri della coda sono uno snapshot, bloccato contro doppi avvii. Un abbinamento ambiguo blocca l’avvio; testi non abbinati chiedono una conferma concreta. Le letture dei nomi interrotte dalla ricreazione vengono riprese; una nuova selezione di testi non validi non lascia silenziosamente quelli vecchi. “Svuota tutto” pulisce selezione, testi e resoconto, mantenendo qualità, destinazione, file e log. Il log audio può essere esportato e cancellato separatamente a coda ferma.

Aggiunti “Mantieni schermo acceso” e “Salva solo se più piccolo”, con preferenze audio indipendenti dal video. Il confronto riguarda il file audio verificato, inclusi tag e copertine; il testo .lrc è copiato senza modifiche. Nessun risparmio, annullamento e fallimento sono esiti distinti. La riprova include soltanto i brani falliti, con cartella/qualità attualmente selezionate. Progresso codifica 0–89%, verifica 90%, salvataggio 96%, completamento dopo il ritorno del salvataggio; progresso di coda con peso uguale per brano. Il resoconto include totali e risparmio dei file effettivamente salvati e viene rinnovato anche se due code hanno lo stesso numero di risultati.

Il motore di codifica e la copia dei metadati restano quelli già presenti. La coda continua uscendo dalla schermata, ma non è un checkpoint ripristinabile dopo la terminazione del processo. I nuovi test coprono i flussi e i render Android sintetici; restano necessarie prove sul dispositivo con file e provider reali.

# v0.72 · conservazione del marcatore beam osservato nei video di salsa

Supportato un solo blocco `beam` principale con payload esatto `01000000010000000000000002000000`, identico negli 11 file bloccati del rapporto 0.71. Il payload viene conservato come metadato storico opaco: non assegniamo un significato ai singoli campi e non accettiamo varianti sconosciute. Il marcatore è scritto prima del nuovo moov, senza spostare mdat o modificare stco/co64; viene riletto e confrontato tramite SHA-256. Un eventuale beam del muxer viene sostituito, oppure rimosso se assente nell’originale. SEF resta in fondo al file. Blocchi beam duplicati, annidati o con payload/dimensioni differenti restano rifiutati. La disposizione fisica complessiva del contenitore viene rigenerata: non si promette l’identificazione del risultato come video WhatsApp da parte di servizi esterni.

Test di regressione con i byte/hash osservati, mutazioni di ogni byte, dimensioni non riconosciute, duplicati, annidamento, aggiunta/sostituzione/rimozione, copia con e senza SEF e controllo degli offset media. Le verifiche di colore della 0.71 restano attive. Il nuovo log contiene 3 conversioni riuscite e 11 blocchi beam; non contiene una prova dei due video del 2017 con colore discordante.

# v0.71 · colore del contenitore e del flusso conservati separatamente

I video con un singolo `colr` nclx/nclc discordante dal VUI possono proseguire: in caso di conflitto x265 conserva l’intero VUI originale, mentre la copia metadati sostituisce il `colr` generato dal muxer con il payload originale. Non viene applicata una trasformazione di matrice, gamma o range ai campioni YUV. I campi SPS non specificati continuano a essere completati dal contenitore quando non esiste un conflitto esplicito. Il file finale viene riletto: SHA-256 del colr, VUI HEVC, decodifica completa e timestamp sono verificati prima del salvataggio. Configurazione HEVC, nuove tabelle campioni e offset dei dati restano quelli rigenerati.

La discordanza dell’originale resta rappresentata nel risultato; la precedenza fra contenitore e flusso può dipendere dal lettore. Non si deduce dal solo metadato quale dichiarazione descriva davvero la ripresa. Profili ICC, dichiarazioni colr multiple e descrizioni video multiple restano bloccati. Test: SPS reale dei due video del 2017, copia AVC→HEVC con sostituzione/aggiunta colr nclx/nclc, pacchetti e frame decodificati identici prima/dopo la copia su un MP4 sintetico. La prova dei due originali sul tablet resta necessaria; la codifica CRF resta con perdita come prima.

# v0.70 · progetti editor video Samsung

Aggiunti schemi verificati per `Original_Path_Hash_Key` e `VideoEditor_Re_Edit_Data` NDE 1.0/2.0. Il progetto viene conservato come dato storico, senza applicare nuovamente tagli, zoom, audio o trasformazioni. I riferimenti all’originale esterno rimangono identici; non vengono ricalcolati sul nuovo HEVC. JSON e varianti vengono controllati con un lettore Java incluso, limitato e rigoroso: campi sconosciuti, versioni diverse, chiavi duplicate, numeri/strutture/riferimenti non validi restano bloccati. La copia SEF resta verificata per SHA-256 e i timestamp video vengono verificati come prima. La riapertura del progetto nell’editor Samsung dipende dalla disponibilità dell’originale e richiede una prova sul dispositivo. I due conflitti colore non vengono modificati da questa versione.

# v0.69 · esportazione SEF indipendente dai metadati annidati

L’export cerca ed esporta prima i blocchi SEF principali, saltando i dati audio/video senza attraversare i metadati annidati. Un blocco meta QuickTime non impedisce più di ottenere i dati completi dell’editor Samsung. L’inventario distingue inoltre meta ISO con versione/flag da meta QuickTime senza prefisso. La conversione dei record dell’editor e la gestione dei conflitti colore restano da completare dopo la lettura dei dati.

# v0.68 · esportazione completa metadati Samsung

“Metadati selezionati” include i byte completi dei blocchi SEF, con offset, dimensioni, SHA-256 e righe BASE64 ricostruibili senza perdita. Il testo di ogni record fino a 64 KiB viene riportato integralmente; per record più grandi il BASE64 resta completo. Anche i blocchi SEF con struttura non interpretabile sono esportati. Budget esplicito: 4 MiB di SEF per file; superamenti segnalati senza dichiarare un export completo. I campioni nei blocchi mdat/idat vengono saltati. La modifica serve alla diagnosi dei metadati dell’editor Samsung: non abilita ancora la loro conversione né risolve i conflitti colore.

# v0.67 · decoder HDR incluso nell’APK

La conversione completa HEVC HDR PQ/HLG e la verifica finale usano il decoder software HEVC di FFmpeg 8.1.3, compilato e incluso nell’APK ARM64. Non richiedono MediaCodec, uscita P010 o Android 13: resta Android 10+ come minimo dell’app. I fotogrammi sono I420 a 10 bit, senza riduzione a 8 bit; dimensioni, colore e timestamp vengono controllati. x265 Main10 e la conservazione/verifica dei payload HDR originali restano attivi.

Questa integrazione riguarda la conversione HDR completa. Il test breve diagnostico P010 resta una prova separata del decoder Android. SDR, MP3/M4A, estrazione/rimux MP4 e lettura metadati continuano a usare le API Android dove già previsto. Non è ancora una pipeline completamente indipendente dal dispositivo.

Build: eseguire anche `bash scripts/prepare-video.sh`. Il sorgente FFmpeg è fissato con SHA-256; si compilano soltanto avcodec/avutil e il decoder HEVC software, senza rete, programmi esterni o codec hardware. La CI confronta tutti i campioni decodificati con FFmpeg indipendente su PQ/HLG, B-frame e timestamp variabili. La prova sul tablet con il video originale resta necessaria.

Interfaccia modulare: pagina principale separata in sezioni e gestori delle azioni, tema e controlli condivisi con Impostazioni. Pulsanti uniformi, selezione qualità evidenziata, metriche a riquadri, larghezza massima sugli schermi grandi e stato ripristinato dopo ricreazione. Architettura e regole di manutenzione: [docs/ui-architecture.md](docs/ui-architecture.md).

Samsung screen recordings: supporto del record SEF `Samsung_Capture_Info` con etichetta `Screen recording` e directory versione 107. Il blocco resta identico e viene verificato con SHA-256 dopo la copia; versioni o payload non verificati restano bloccati. Test di regressione con gli 84 byte originali del campione, riferimenti corrotti e copia insieme ai precedenti record HighlightVideo_Data.

# Conversione HDR e struttura del codice

La coda riconosce HEVC Main10 PQ/HLG BT.2020 e usa il percorso separato a 10 bit con decoder software incluso. CRF, preset, CPU, B-frame e lookahead sono quelli selezionati; il log riporta i valori effettivi. Il percorso HDR è sperimentale: la libreria 10 bit non usa ancora assembly ARM. SDR continua a usare la propria libreria ottimizzata.

- `HdrTranscoder`: decodifica P010 e codifica CPU Main10 dell'intero video.
- `HdrCodec`: scelta decoder, controlli colore e accesso alle immagini Android.
- `HdrP010Packer`: conversione campioni P010 in I420 a 10 bit, senza passare a 8 bit.
- `HdrSei`: lettura SEI, copia esatta dei payload mastering display, content light, alternative transfer e HDR10+ T.35. Timing/hash del vecchio bitstream non vengono riutilizzati. SEI semantici sconosciuti e livelli Dolby Vision bloccano la conversione con dettaglio nel log.
- `HdrMetadataLedger`: associazione per timestamp anche con B-frame e indice SHA-256 su disco; memoria limitata. Verifica tutti i payload nell'MP4 prima del salvataggio.
- `cpp/hdr`: x265 Main10 e JNI in una libreria distinta dalla versione SDR.
- `VideoConverter`: coordina i percorsi; riusa copia audio, metadati MP4/Samsung/GPS, verifica e salvataggio esistenti.

Il test breve HDR conserva e verifica gli stessi payload su sei fotogrammi. La CI verifica PQ/HLG, campioni 10 bit, riordinamento con B-frame/lookahead, payload Samsung HDR10+, decodifica FFmpeg e conservazione dei byte dopo remux MP4. La verifica sul telefono resta necessaria per il decoder specifico e il video reale; non viene dichiarata una validazione percettiva della qualità.

# Novità v0.26 · cartelle e conversione multipla

- Impostazioni: cartella predefinita, salvata con un permesso SAF permanente. La scelta nella home vale per la coda corrente.
- Home: «Salva nella cartella di origine», con autorizzazione per ogni cartella necessaria. Se il provider non comunica il percorso, la cartella viene scelta manualmente e dichiarata non verificabile.
- Selezione multipla fino a 100 video, con elenco selezionati e analisi del singolo elemento scelto.
- Coda sequenziale nel servizio foreground: percentuale per file e globale (peso uguale per file, non ETA), fotogrammi e secondi del file corrente, esiti separati. Un errore passa al successivo; annullare ferma il resto e conserva i file già completati.
- Nome, CRF, preset e parametri restano quelli acquisiti all’avvio della coda. Nessuna sovrascrittura degli originali.
- Esporta log TXT raccoglie tutti i file, le destinazioni e gli errori della coda. I risultati si possono aprire dall’elenco dei video completati.
- Il test HDR resta separato e opera sul video visualizzato; questa modifica non abilita la conversione HDR completa.
- Collaudo sul telefono: due video con nomi uguali da cartelle diverse, destinazione predefinita e origine, coda con file non supportato, annullamento durante il secondo file, riapertura dalla notifica, esportazione TXT e revoca del permesso cartella.

# Test codec video — Android
## Versione 0.3: prova software x265
Nuovo pulsante per una prova CPU: stesso filmato sintetico I420 1920×1080, 30 fotogrammi a 30 fps, CRF 24 e 32, preset ultrafast / zerolatency.
La libreria x265 è compilata e inclusa nell'APK ARM64: nessun download dal telefono.
Ogni prova crea un MP4 senza audio e lo decodifica interamente per verificare 30/30 fotogrammi.
Il log include chiamate BEGIN/END, parametri effettivi, tempi, NAL, byte, timestamp, conteggi e stack trace.
Il log x265 è salvato incrementalmente in memoria privata e recuperato alla riapertura; Copia risultati permette di condividerlo.
Interrompi richiede cancellazione tra chiamate: una chiamata bloccata nativa non può essere interrotta da questo pulsante.
Esporta MP4 salva l'ultimo file disponibile tramite il selettore Android.
Il test non misura qualità percettiva o prestazioni su riprese reali. Assembly ARM disabilitato per questa prima build diagnostica; la velocità non è il massimo ottenibile con x265 ottimizzato.
La decodifica di verifica può usare l'hardware, mentre la codifica usa esclusivamente x265 sulla CPU.

### Sorgenti e licenza
Questa versione è distribuita sotto GNU GPL v2 o successiva, coerentemente con x265.
La CI include COPYING di x265 negli asset e pubblica il pacchetto di sorgenti completo, con revisione x265 fissata e script di build.
Per la build locale eseguire prima scripts/prepare-x265.sh e installare NDK 27.2.12479018 e CMake 3.22.1.

Prima app di diagnosi per il futuro convertitore video. Java, API Android native, Android 10+.

## Utilizzo
Installa l'APK, premi **Esegui test**, attendi il completamento e premi **Copia risultati** per condividere il rapporto.
Nessun accesso a video, rete o memoria condivisa; nessun permesso richiesto.

Per ogni encoder HEVC/H.265 elencato da Android:
- verifica hardware/software, limiti di risoluzione, allineamento e frequenze con input Surface;
- preferisce 1280×720 a 30 fps; se incompatibile, cerca un profilo dichiarato compatibile tra risoluzioni comuni e dimensioni allineate, entro 1920×1080 e 1–60 fps;
- legge il supporto dichiarato per VBR, CBR e CQ e il range di qualità CQ;
- per ciascuna modalità supportata crea un'istanza indipendente, configura, crea la Surface, avvia, ferma e rilascia l'encoder;
- registra OK, NON SUPPORTATO o ERRORE con fase e dettaglio.

**Limiti del test API Android:** non invia fotogrammi e non produce un video. OK indica che la chiamata è riuscita; non misura qualità, velocità o bitrate risultante. CQ Android è una scala specifica del codec e non equivale al CRF di x265. Il mancato supporto del profilo di prova non implica che il codec non funzioni con altri parametri.

## APK
Apri **Actions → Build APK → ultima esecuzione riuscita → Artifacts → CodecProbe-APK**.
Scarica lo ZIP, estrai app-debug.apk e installalo sul telefono.
L'APK è una build debug firmata automaticamente, per test personali.
La chiave debug di un runner temporaneo può cambiare tra build: se un aggiornamento non si installa, disinstalla la precedente versione.

## Build locale
JDK 17, Android SDK 35, Gradle 8.11.1:
`gradle :app:assembleDebug :app:lintDebug`
Non è incluso un Gradle wrapper: la CI installa esplicitamente la versione sopra.

## v0.5: export and controlled optimization

- Save the complete displayed log (including recovered x265 logs) as UTF-8 TXT through the Android document picker. The export freezes a snapshot before opening the picker.
- Native x265 and JNI compile as RelWithDebInfo with -O3; the APK remains debug-signed for installation. CI checks actual compiler commands and the APK ELF64 AArch64 header.
- Keep the pinned x265 version, 30 synthetic frames, CRF 24/32, two threads and assembly disabled to isolate compiler optimization. ARM assembly is a subsequent experiment.
- Log real pointer size, architecture, optimization macro, x265-only call times and total cycle time. The old upstream build_info labels non-x86 as 32 bit; it is not authoritative on ARM.
- On the SM-S916B, run the software test with the phone cool and idle, save TXT, and compare total times with v0.3. The short benchmark and historic comparison are indicative, not sustained-performance or quality measurements.

## v0.6: ARM64 assembly experiment

Enable pinned x265 ARM64/NEON routines while retaining O3, two threads and the same 30 frames/CRFs. The prepare script applies only target-flag and assembler-directive portability fixes. CI runs an ARM64 Linux build under QEMU, independently decodes its output, and checks linked Android assembly symbols. QEMU does not measure phone speed or validate Android runtime behavior. On-device logs compare with v0.5 (1333.327448 ms / 1985.072707 ms).


## v0.7: real-video conversion UI
Select a video using Android's document picker, select an output folder, and choose any integer CRF from 0 to 51 (default 23). Lower numbers generally retain more detail and produce larger files; descriptions are approximate. CRF 0 is not x265 lossless mode. Synthetic/API diagnostic buttons are removed from the main UI; diagnostic source remains for automated smoke tests.

The pipeline reads decoded YUV_420_888 planes with stride/crop handling, feeds the stateful x265 encoder on the CPU, preserves video timestamps/orientation and copies one AAC audio track. It verifies all output video frames, then creates a uniquely named MP4 in the selected folder. Originals are read-only. Failed/cancelled exports attempt to remove the partial document. The full step-by-step UTF-8 log remains exportable; the UI displays only recent entries to avoid rendering an ever-growing log.

Initial supported scope: SDR 8-bit, even dimensions up to 3840x2160 including portrait, one video and zero/one AAC track. HDR, non-YUV420 decoder output, multiple audio/video tracks and non-AAC audio produce an explicit error. Other track types are excluded with a log warning. Missing color metadata uses logged SDR defaults. Keep the app open; leaving/destroying the activity cancels the worker. Temporary MP4 storage is needed before export. No resizing, audio transcoding or HDR tone mapping yet.

Validation: host and emulated ARM64 streaming tests cover CRF 0/23/51, variable input timestamps and independent FFmpeg full-frame decode; Java plane packing tests cover crop/padding/interleaved chroma. Android build/lint and native architecture/symbol checks remain required. Android picker and real-device decoder integration need on-device verification.

## v0.8: controls and monitoring

- System-aware dark/light palette, matching status/navigation icon contrast, rounded cards and buttons.
- Encoder preset menu ultrafast through veryslow, slow default. The streaming pipeline still applies zerolatency and disables B-frames/lookahead to preserve its PTS-only muxing contract; these are not unmodified offline presets.
- Quality choices CRF 18/23/28 plus integer entry 0–51, checked before conversion.
- Application process CPU time sampled each second, normalized by available processor count (100% = all available cores); not total device utilization or CPU frequency. Battery temperature and Android thermal status are explicitly labeled, unavailable values have a fallback.
- Video seconds encoded and remaining use last emitted packet PTS, then full duration only after successful encoder flush. Unknown duration remains unknown. Overall progress reserves final steps for audio, validation and saving.
- Native CI tests exercise ultrafast, slow and veryslow with CRF 0/23/51 respectively, including independent full decoding. Phone UI and end-to-end playback still require device verification.

## v0.9: input information and CPU policies

- Replaces fixed single-line Spinner rows with wrapping selection buttons and dynamically measured dialog rows.
- Shows source size in decimal MB and exact bytes (provider query, then descriptor stat fallback), declared container MIME, video/audio codecs, resolution, duration, rotation, declared fps, color transfer/HDR and known HEVC bit depth. Missing values remain unknown; no claim of playback validation from metadata. Reads run on the worker and ignore obsolete selection results. Same summary is included in the full conversion log.
- CPU policies: Limited retains frame threads=2/pool=2; Automatic delegates frame threads and pool size to x265; Maximum assigns workers for available cores (pool cap 64) and up to six frame threads. The maximum policy is a parallelism configuration, not a guarantee of 100% CPU utilization. Automatic is the default.
- Native tests cover all three CPU policies with variable input PTS and verify that every frame decodes. Logs include requested CPU policy and effective frame thread count.

## v0.10: file compression and background conversion

- B-frame and extended lookahead switches default ON. Preset settings are used without zerolatency. With B-frames ON / extended lookahead OFF, x265 requires bframes+1 lookahead frames: adaptive B choice and CU-tree are disabled in that minimal mode. Both OFF means no B-frames or lookahead. Effective values and output PTS/DTS/slice type are logged.
- Android MediaMuxer supports MP4 B-frames since API 25; packets are delivered in encoder output order with original presentation timestamps. Every decoded output PTS is compared with the original decoded input PTS (1 ms container rounding tolerance), including VFR; failed validation prevents publication.
- Conversion runs in a foreground service, with progress/cancel notification and partial wake lock for screen-off processing. API35+ uses mediaProcessing, older supported releases use dataSync. Notification permission is requested on API33+. Service timeout cancels and reports the reason; this does not bypass Android stop/force-stop or memory/resource decisions. No automatic restart of interrupted conversions.
- Inspector sums compressed sample sizes per track without decoding; non-sample bytes are explicitly described as overhead, tags and padding. A bounded MP4 box reader measures moov and descriptive udta/meta blocks without double counting nested tags. Other formats/nonseekable providers report unsupported box measurement.
- Expandable metadata shows all retriever metadata fields and all MediaFormat keys exposed by Android, including binary field lengths. Proprietary metadata is not guaranteed readable. Descriptive tags/location/date are displayed but not automatically copied; output preserves technical video format/orientation/timestamps.
- CI tests MP4 box size accounting, all four B/lookahead combinations and actual B-picture presence, original PTS set preservation and independent full frame decoding on host and ARM64. Foreground lifecycle and Android B-frame MP4 muxing require phone testing; full app verification checks the final MP4 before saving.

## v0.11: metadata preservation and readable input sections

- Input properties are grouped into File, Video, Audio, Track sizes and Metadata/container sections. Expandable sections separate Date/GPS, Title/author, other fields and technical properties per track, with clear labels and spacing. Android-readable values are not an exhaustive decoder of vendor tags.
- MP4/MOV descriptive udta/meta/uuid/meco blocks are copied as opaque payloads at root, movie, track and media levels, preserving proprietary tags within them. Creation/modification dates in mvhd/tkhd/mdhd and track language are patched from the original. New codec/sample tables and media offsets are retained. Copied payloads are verified using SHA-256; dates and language are read back and checked.
- The rewritten moov is appended and the old moov becomes free padding, keeping all encoded media chunk offsets unchanged. This adds a second movie-table-sized overhead and can put the active moov at the end; generated files remain ordinary seekable MP4s.
- A strict source preflight rejects fragmented/non-MP4 formats, malformed/trailing vendor data and unsupported top-level blocks, rather than claiming complete metadata retention. An opaque vendor UUID/tag may itself contain private references whose interpretation is not guaranteed by raw copying. Filesystem timestamps controlled by the Android document provider are not set to the source times.
- Application metadata tracks supported by Android MediaMuxer are copied with original packet contents and PTS. Unsupported tracks abort conversion. Audio/metadata copied packets are read back and verified in addition to full video decode and original PTS checks.
- CI round trips no-tag and tagged fixtures with nested metadata, dates/languages and root UUIDs, asserts media data positions unchanged, and tests cancellation and unsupported containers. Device testing remains required for real camera metadata and vendor playback interpretation.

- Preservation preflight additionally rejects item-location metadata (iloc/idat/iref/pitm) and unrecognized UUIDs, since opaque copying could leave pointers to old media. The known Adobe XMP UUID is supported. Unsupported vendor formats need an explicit parser before the app can guarantee preservation.

- Track handler/name payloads and playback flags, layer/group/volume and transformation matrices are preserved and read back. IDs, codec descriptions, durations and media chunk indexes remain those of the new file.

## Versioned downloads

APK artifacts now start with the actual version from the built APK metadata, for example `v0.11-CodecProbe-APK.zip`. The APK inside is `v0.11-CodecProbe.apk`. The corresponding source artifact also starts with the version. App version is unchanged when only packaging changes.

## v0.13: original media access and explicit GPS checks

Before conversion, request ACCESS_MEDIA_LOCATION to preserve embedded coordinates (no live location access). Convert supported SAF document URIs to the equivalent MediaStore item using the existing document grant and setRequireOriginal. Fail instead of silently falling back to redacted local media; other providers are explicitly labelled as unverified. Reopen source bytes after permission changes and refresh input inspection. Compare Android-exposed ISO 6709 location against both the temporary MP4 and the final saved file; delete a failed output. Null source location is labelled non-verifiable, never successful GPS preservation. Opaque metadata blocks still receive byte-level verification.

Device check: select the camera original that displays a location in Samsung Gallery, grant media location permission, convert, and export the log. Look for GPS ACCESSO requireOriginal=true, GPS ORIGINALE, GPS CONVERTITO and GPS ESITO. Previously redacted copies cannot recover coordinates.

## v0.14: separate video-read authorization

The v0.13 device log exposed a SecurityException opening a MediaStore URI with requireOriginal despite ACCESS_MEDIA_LOCATION being granted. Request READ_MEDIA_VIDEO separately; Android 14+ also offers READ_MEDIA_VISUAL_USER_SELECTED so the user can restrict access to selected videos. Android 10–12 use READ_EXTERNAL_STORAGE (maxSdkVersion 32). Keep strict original access and validate opening the actual URI before reporting requireOriginal success. Restricted selection must include the conversion input. Permission denial blocks conversion with a readable reason. GPS comparisons and original metadata copying remain intact.

## v0.15: Android provider numeric compatibility

The v0.14 Samsung Android 16 log confirms original GPS is readable, then exposes Long accurate-framecount where conversion assumed Integer. Read MediaFormat numeric fields using getNumber; convert integer fields exactly with explicit finite/integral/range checks. Apply this to conversion properties, duration, frame rate, verification dimensions and input inspection. Regression tests cover Long(52), integer and float representations, duration, overflow, fractional/non-finite values and missing numbers. GPS preservation still needs an end-to-end device conversion.

## v0.16: one runtime version and exact source color

BuildConfig.VERSION_NAME generated from app/build.gradle now supplies the screen and conversion log. APK and ZIP names already derive from the built output metadata. Only versionName/versionCode in Gradle are updated for a release; UI and log have no literal versions.

Read H.264/H.265 SPS VUI directly from codec CSD, including exact primaries, transfer, matrix, full range and presence flags/video format. Pass those values to x265 instead of collapsing Android SDR to SMPTE170M. Verify emitted HEVC headers against the original before muxing. Unsupported/truncated SPS and inconsistent multiple SPS fail explicitly, with no color guess. Tests include the Samsung SPS from the sample, generated AVC BT709 and HEVC SMPTE170M full-range, plus truncated input. MP4 nclx/nclc color declarations are checked against SPS; conflicts and ICC profiles are explicitly rejected rather than guessed. Native host and ARM64 tests independently verify BT709, SMPTE170M full-range and linear output with ffprobe.

## v0.17: small read-only metadata diagnostics

The diagnostic screen adds Export original metadata TXT. It opens the selected original with the same media/location permissions and exports ftyp, udta/meta/uuid/meco and sefd blocks with original offsets, sizes, full HEX and SHA256. It skips mdat by seeking, so a multi-GB video yields a small metadata-only file. Limits: 4 MB per copied block and 8 MB total metadata; no partial-success report. Failed or cancelled exports delete their temporary file. Conversion still rejects unverified sefd; the diagnostic export does not enable blind copying. Attach the exported TXT to identify actual SEF entries and assess relocation.

