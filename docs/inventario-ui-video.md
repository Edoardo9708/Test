# Inventario UI e piano di riuso

Riferimento: CodecProbe 0.79, commit `aa505941a9ed3fa22ce9db63f436eb82bb64126f`.
Data: 19 ottobre 2026. Stato: inventario e proposta da discutere, non implementazione.

## Scopo e metodo

Usare la schermata video come riferimento per struttura, grafica e comportamento di audio e immagini. Condividere i componenti effettivi, non mantenere tre copie simili.

L'inventario seguente deriva dalla lettura dei costruttori delle viste e dei controller che ne aggiornano visibilità, abilitazione e azioni. Non è una nuova prova sul telefono. Le sezioni «Attuale» descrivono il codice; le sezioni «Proposta» definiscono il lavoro successivo.

La schermata video è il riferimento, ma non ogni sua scelta è automaticamente una regola ideale: eventuali difetti o ambiguità vanno risolti nel componente comune, una volta sola.

## 1. Stati da distinguere

| Stato | Significato |
|---|---|
| Nessuna selezione | Non ci sono file nella bozza. Un eventuale resoconto precedente è uno stato distinto. |
| Lettura metadati | Lettura iniziale o esplicita rilettura; può avvenire in primo piano o in background. |
| Cartelle in verifica | Il controllo asincrono dei permessi non ha ancora concluso. |
| Autorizzazione cartella | È aperto il selettore cartelle o una richiesta collegata. |
| Pronto | Selezione presente, destinazioni verificate e nessuna operazione che impedisca l'avvio. |
| Controllo nomi | Verifica dei nomi di output prima della codifica; possibile dialogo per omonimi. |
| Conversione | Sessione attiva; distinguere codifica, verifica e salvataggio. |
| Pausa richiesta / effettiva | Sono stati separati nel video; la risposta al comando non va anticipata dalla grafica. |
| Annullamento / salto | Comando richiesto, pulizia ancora in corso; non equivale a sessione già terminata. |
| Terminato | Risultati disponibili; esiti riusciti, falliti, ignorati, saltati e non salvati distinti. |
| Sessione recuperabile | Video: coda salvata, non attiva, con possibilità di ripresa. |
| Esportazione / cancellazione log | Operazioni separate dalla conversione che bloccano alcune azioni. |

Nel codice video `busy` comprende anche il controllo dei nomi. Non sostituisce tutti gli altri stati: alcune azioni controllano inoltre lettura metadati, selettore cartelle, esportazione e richieste pendenti.

## 2. Inventario attuale della schermata video

### 2.1 Struttura generale

| ID | Oggetto | Azione e comportamento attuali |
|---|---|---|
| G01 | Schede Video / Audio / Immagini | In alto nel contenuto; scorrono fuori schermo con la pagina. Toccare quella attiva non cambia schermata. Cambio con dissolvenza. |
| G02 | Intestazione | Titolo, formato/motore e versione; pulsante Impostazioni a destra. |
| G03 | Impostazioni | Apre una pagina dedicata passando la configurazione corrente. |
| G04 | Quattro riquadri espandibili | File e destinazione; Compressione; Conversione; Diagnostica. Grafica e apertura gestite da `AppSections`. |
| G05 | Apertura manuale dei riquadri | Ogni intestazione apre/chiude il proprio riquadro; possono restarne aperti più di uno. |
| G06 | Apertura guidata | `focusCard` apre soltanto quello richiesto e porta l'intestazione in vista. Alla prima apertura normale è aperto il primo; una sessione video attiva può dare priorità alla conversione. Lo stato ripristinato viene considerato. |
| G07 | Descrizione di riquadro chiuso | Compare sotto l'intestazione soltanto quando il contenuto è chiuso. |
| G08 | Stato della pagina | Aperture dei riquadri salvate; selezione, parametri editabili e sessione di conversione sono responsabilità distinte. |

### 2.2 Riquadro 1: selezione e dettagli

| ID | Oggetto | Visibilità, abilitazione e azione attuali |
|---|---|---|
| F01 | Seleziona video | Visibile; apre selezione multipla Android. Disabilitato durante operazione occupata o scelta cartelle. |
| F02 | Svuota tutto | Visibile; abilitato se esistono selezione/sessione/risultati recuperabili e non ci sono operazioni o richieste pendenti che lo impediscano. |
| F03 | Effetto di Svuota tutto | Svuota bozza, cache video, avanzamento e resoconto; conserva originali, log e impostazioni; riporta al riquadro 1. |
| F04 | Contatore Video N di M | Visibile con selezione, nascosto senza file. |
| F05 | Carosello | Un pulsante per file, numero e nome abbreviato; nome completo nell'accessibilità; selezionato evidenziato; eventuale errore di lettura indicato. Scorrere non cambia selezione. |
| F06 | Cambio elemento | Dissolvenza breve; aggiornamento nome, metadati e destinazione del file; il pulsante selezionato viene portato in vista. Le richieste precedenti vengono invalidate. |
| F07 | Nome e sintesi | Nome completo fino a tre righe; riepilogo dei dati del file o stato della lettura. Nessun file: messaggio iniziale. |
| F08 | Dettagli video | Compare con selezione; apre/chiude i dettagli sotto la sintesi; freccia e testo seguono lo stato. |
| F09 | Contenuto dettagli | Gruppi di dati con titolo e corpo selezionabile, tramite `renderInfoSections`. Distinzione tra sintesi e dettagli tecnici. |
| F10 | Rileggi metadati | Compare con selezione; disabilitato durante lettura, operazione occupata o scelta cartelle. Invalida la cache video e rilegge la selezione. |
| F11 | Lettura iniziale | Precarica i metadati mancanti della selezione. Primo piano: dialogo di avanzamento con Interrompi. Background: stato sulla pagina. I risultati già letti restano utilizzabili. |
| F12 | Memoria dei metadati | Cache locale al processo, fino a 100 voci; invalida anche in caso di cambiamento dei permessi rilevanti per gli originali. Nessun buffer video conservato nella cache. |
| F13 | Rimuovi singolo elemento | Nella schermata video non esiste attualmente questo pulsante; esiste invece in audio e immagini. È una decisione di prodotto da esplicitare. |

### 2.3 Riquadro 1: destinazioni e permessi

**Regola osservata:** tutto il blocco seguente è nascosto finché la selezione video è vuota. Non è soltanto disabilitato.

| ID | Oggetto | Visibilità, abilitazione e azione attuali |
|---|---|---|
| D01 | Destinazione del file visualizzato | Titolo con numero del video e spiegazione della portata della scelta. |
| D02 | Salva nella cartella di origine, individuale | Modifica la regola del file visualizzato. Disabilitato se occupato, in scelta cartelle o se la regola globale è attiva. |
| D03 | Scegli cartella per questo video | Selettore cartelle dell'app/Android; risultato associato al file della richiesta, non semplicemente all'indice attuale. Stessi blocchi di D02. |
| D04 | Percorso individuale | Mostra cartella scelta o origine. La scelta individuale rimane memorizzata anche con override globale. |
| D05 | Applica a tutti i video | Attiva/disattiva la regola globale. Disabilitato se occupato o in scelta cartelle. |
| D06 | Salva nella cartella di origine, globale | Attivo solo con override globale e pagina modificabile; ciascun file resta nella propria origine. |
| D07 | Scegli una cartella per tutti | Attivo solo con override globale e pagina modificabile; applica una cartella alla coda. |
| D08 | Percorso e spiegazioni globali | Esplicitano differenza tra stessa cartella per tutti e origine di ciascun file. |
| D09 | Cartelle utilizzate | Raggruppa le destinazioni effettive della coda; verifica asincrona, non soltanto presenza di un URI salvato. |
| D10 | Riquadro permesso verificato | Bordo/stato verde, percorso, numeri dei file interessati, pulsante Gestisci. |
| D11 | Riquadro da autorizzare | Bordo/stato rosso, percorso/origine da risolvere, pulsante Autorizza oppure Scegli se manca la cartella. |
| D12 | Associazione manuale | Se l'origine non è rilevata, il dialogo chiede la directory del file; verifica gli altri abbinamenti senza presumere che tutti siano autorizzati. |
| D13 | Contatore autorizzazioni | Cartelle autorizzate N / M. Durante verifica mostra lo stato di controllo. |
| D14 | Effetto dei permessi sull'avvio | La conversione diventa disponibile solo quando tutte le destinazioni effettive sono pronte. Gestisci/Autorizza disabilitati durante operazione occupata o scelta cartelle. |

### 2.4 Riquadro 2: compressione

| ID | Oggetto | Comportamento attuale |
|---|---|---|
| C01 | Preset | Slider a nove posizioni; nome e valori derivati aggiornati. Informazione associata al parametro. |
| C02 | CRF | Slider intero 0–51 con valore e spiegazione. |
| C03 | Qualità predefinita | Pulsanti Alta / Media / Ridotta: CRF 18 / 23 / 28. Evidenziazione coerente con lo slider. |
| C04 | B-frame | Interruttore con informazione; modifica la configurazione temporale. |
| C05 | Lookahead esteso | Interruttore con informazione; influenza anche CU-tree. |
| C06 | CU-tree | Riga informativa con stato derivato. |
| C07 | AQ mode | Riga informativa con valore derivato dal preset. |
| C08 | AQ strength | Riga informativa con valore derivato dal preset. |
| C09 | Modalità CPU | Pulsanti Limited / Auto / Max; informazione associata. |
| C10 | Blocco modifiche | Slider, interruttori, scorciatoie e modalità CPU disabilitati quando il video è occupato. I valori della coda attiva restano distinti dai futuri valori modificabili. |

L'elenco dei parametri non è da copiare ciecamente su audio e immagini: il componente grafico di slider, scelta rapida, interruttore e informazione può essere identico; parametro, intervallo e azione dipendono dal motore.

### 2.5 Riquadro 3: conversione e sistema

| ID | Oggetto | Visibilità, abilitazione e azione attuali |
|---|---|---|
| R01 | Converti e salva | Visibile; abilitato con selezione, destinazioni pronte e senza occupazione, cancellazione log o scelta cartelle. Seguono richieste di accesso agli originali e controllo dei nomi. |
| R02 | Annulla | Visibile; abilitato quando occupato, escluso controllo iniziale dei nomi. Apre conferma Continua / Sì, annulla; i completati restano salvati. |
| R03 | Pausa / Riprendi | Riga visibile soltanto con sessione attiva; disabilitato durante annullamento o salto. Testo segue la richiesta di pausa. |
| R04 | Salta video | Stessa riga; abilitato solo con file attivo, senza annullamento/salto. Conferma legata a sessione e file correnti. |
| R05 | Salva e interrompi | Visibile durante sessione o se recuperabile; durante attività conserva la coda e interrompe il file in corso. |
| R06 | Riprendi sessione salvata | Lo stesso pulsante cambia funzione con sessione recuperabile; scelta tra parametri salvati e attuali, poi verifica accessi e nomi. |
| R07 | Aiuto sulla sessione | Visibile insieme al pulsante di salvataggio/ripresa; specifica che il file interrotto riparte dall'inizio. |
| R08 | Mantieni schermo acceso | Checkbox con pulsante informativo; scelta ricordata. Effetto solo sulla pagina video visibile durante attività, senza pausa effettiva. |
| R09 | Salva solo se più piccolo | Checkbox con pulsante informativo; disabilitato durante occupazione. Confronto del file completo. |
| R10 | Sommario | Riquadro con posizione nella coda e conteggi distinti degli esiti; tempo trascorso. |
| R11 | File attuale | Nome, percentuale, barra orizzontale; stati iniziali e di sessione recuperabile espliciti. |
| R12 | Avanzamento specifico | Fotogrammi e secondi elaborati/rimanenti; valori mancanti indicati come tali. |
| R13 | Messaggio di stato | Fase comunicata dal servizio, incluso controllo, verifica, salvataggio o esito. |
| R14 | Parametri in uso | Righe etichetta/valore: solo se più piccolo, preset, CRF, CPU, B-frame, lookahead, CU-tree, AQ mode e strength. Non semplice descrizione della bozza corrente. |
| R15 | Sistema | Tre misure: CPU app, RAM PSS, temperatura batteria; spiegazione delle unità e dei colori. |
| R16 | Riprova i falliti | Visibile soltanto se esiste un sottoinsieme riprovabile. Conferma con parametri/destinazioni precedenti; esclusi altri esiti. Disabilitato durante operazioni incompatibili. |
| R17 | File già presente | Prima della codifica: dialogo con nome suggerito, Annulla / Ignora / Converti. Nome modificabile, conflitti ricontrollati; nessuna sovrascrittura. |
| R18 | Accesso originale | Dialoghi specifici per accesso ai video e posizione incorporata prima dell'avvio. Non corrispondono ai permessi della cartella di destinazione. |

Pausa, salto e ripresa duratura sono funzioni del servizio video. Renderle realmente disponibili anche negli altri motori richiede lavoro funzionale; non basta mostrare i medesimi pulsanti.

### 2.6 Report video e popup

| ID | Oggetto | Comportamento attuale |
|---|---|---|
| W01 | Contenitore report | Nascosto senza resoconto; titolo REPORT LAVORI, con IN CORSO durante attività. |
| W02 | Riga risultato | Numero e nome del file; esito e dimensioni pertinenti; testo selezionabile. |
| W03 | Apri | Per un risultato completato con URI disponibile; pulsante laterale, larghezza 88 dp; apre il file con permesso di lettura. |
| W04 | Errore | Per fallimento con dettaglio disponibile; pulsante rosso «!» di 48 × 48 dp, con descrizione accessibile. |
| W05 | Popup errore | Nome, messaggio, ultimo stato comunicato, dettagli tecnici inizialmente chiusi; contenuto scorrevole. |
| W06 | Copia tutto / Chiudi | Copia anche i dettagli tecnici chiusi; la copia lascia aperto il popup. Chiudi lo chiude. |
| W07 | Totale | In fondo al report: file riusciti, confronto dimensioni, variazione e spazio risparmiato/aggiuntivo; unità dichiarate. |
| W08 | Dettagli di un successo | Il video non ha attualmente un secondo pulsante Dettagli sul successo. Le immagini lo hanno: va deciso un accesso comune o una collocazione in diagnostica, evitando una variante grafica isolata. |

### 2.7 Diagnostica

| ID | Oggetto | Comportamento attuale |
|---|---|---|
| L01 | Esporta log | Scelta ZIP / TXT; include log compattati. Richiesta Android di destinazione. |
| L02 | Metadati selezionati | Analizza la selezione e produce un TXT; non è una conversione. |
| L03 | Test HDR Android | Prova diagnostica specifica del video, distinta dal decoder incluso usato in conversione. |
| L04 | Log HDR TXT | Esporta il log della prova HDR. |
| L05 | Cancella tutti i log | Cancella log interni, non i file già esportati. |
| L06 | Mostra / Nascondi ultimi messaggi | Testo monospaziato selezionabile, massimo 16 righe nella pagina. |
| L07 | Blocchi delle azioni | Esportazioni/prova/cancellazione disabilitate durante occupazione, altre operazioni diagnostiche o scelta cartelle; controlli supplementari nei rispettivi controller. |

### 2.8 Impostazioni video

| ID | Oggetto | Comportamento attuale |
|---|---|---|
| S01 | Titolo e versione | Pagina dedicata, con pulsante Torna alla conversione. |
| S02 | Cartella predefinita | Riquadro con percorso, Scegli, Rimuovi e spiegazione. È una preferenza generale: non richiede una selezione nella home. |
| S03 | Scelta cartella | Stesso selettore dedicato; richiede lettura/scrittura persistenti. La scelta aggiorna il modulo, non viene salvata come default finché non si preme Salva. |
| S04 | Nome file | Riquadro con testo aggiuntivo, posizione all'inizio/alla fine e anteprima. |
| S05 | Preset predefinito | Slider e nome. |
| S06 | CRF e scorciatoie | Slider e pulsanti qualità. |
| S07 | Modalità CPU | Stessa famiglia di pulsanti della home. |
| S08 | B-frame e lookahead | Interruttori e spiegazione dei parametri derivati. |
| S09 | Schermo acceso | Checkbox del valore predefinito. |
| S10 | Copia configurazione corrente | Attualmente copia preset, CRF, CPU, B-frame e lookahead. Non copia tutti i campi della pagina: differisce dalle altre sezioni. |
| S11 | Salva impostazioni | Pulsante primario; salva e torna alla conversione. Nuovi valori di compressione applicati a riposo, senza cambiare la sessione video già avviata. |
| S12 | Torna / Indietro | Esce senza eseguire il salvataggio del modulo. |
| S13 | Ricreazione della pagina | Il codice video salva esplicitamente nel Bundle la cartella; per gli altri campi la conservazione delle modifiche non salvate va verificata e resa esplicita nel componente comune. Audio/immagini hanno salvataggi espliciti più completi. |
| S14 | Solo se più piccolo | Esiste nella home e nelle preferenze video, ma non come campo di questo modulo. Audio/immagini lo espongono nelle impostazioni: scelta da uniformare. |
| S15 | Titoli e separatori | Il video usa etichette proprie; non usa gli stessi titoli con linea automatica di audio/immagini. |

## 3. Differenze attuali confermate

| Parte | Video | Audio | Immagini | Direzione proposta |
|---|---|---|---|---|
| Destinazioni prima della selezione | Intero blocco nascosto | Pulsante/percorso già visibili | Pulsante/percorso già visibili | Identica regola del video nella home. |
| Piano destinazioni | Individuale + override coda + origine | Una cartella per coda | Una cartella per coda | Estendere il modello dove si vuole parità funzionale; non limitarsi alla grafica. |
| Verifica cartelle | Schede verdi/rosse, percorso, file coinvolti, azione | Controllo soprattutto durante preparazione avvio, senza schede | Controllo durante preparazione avvio, senza schede | Un pannello comune, alimentato da stato verificato. |
| Selettore cartella nella home | Selettore dedicato | Apertura diretta del selettore Android | Selettore dedicato | Stesso percorso di selezione e stesse regole di ritorno. |
| Carosello e cornice anteprima | Componente comune | Componente comune | Componente comune | Consolidare la condivisione già presente; verificare anche stati vuoti e letture. |
| Contenuto dettagli | Gruppi titolati | Testo semplice | Testo semplice | Stesso renderer di gruppi; dati e gruppi pertinenti al formato. |
| Rilettura | Rilegge selezione video | Rilegge brano corrente | Rilegge foto corrente | Stabilire esplicitamente una sola portata; proposta: selezione, come video. |
| Lettura metadati | Precaricamento della selezione, dialogo/background | Lettura dell'elemento visualizzato | Lettura dell'elemento visualizzato | Decidere comportamento comune senza confondere cache e precaricamento. |
| Rimozione singolo file | Assente | Presente | Presente | Decidere se aggiungerla in tutte e tre; non eliminarla implicitamente. |
| Checkbox comuni | Nel riquadro 3, con informazioni | Nel riquadro 3, senza stessi aiuti | Nel riquadro 2, senza stessi aiuti | Stessa posizione, stesso componente e stessi aiuti. |
| Sommario e parametri attivi | Riquadro e righe etichetta/valore | Struttura più semplice | Struttura molto ridotta | Layout comune; righe definite dai dati realmente disponibili. |
| Progresso | Percentuale e barra, fotogrammi/tempo | Percentuale/barra e campioni/tempo | Fase e indicatore indeterminato | Cornice comune; non inventare una percentuale foto che il motore non comunica. |
| Sistema CPU/RAM/batteria | Presente | Assente | Assente | Componente comune riutilizzabile; alimentazione delle misure da completare. |
| Report senza risultati | Nascosto | Contenitore e testo vuoto presenti | Contenitore con messaggio presente | Stessa regola di comparsa del video. |
| Apri nel report | Pulsante 88 dp | Pulsante 92 dp | Pulsante 92 dp, colonna di azioni | Stesso componente e misure del video. |
| Errore nel report | «!» rosso, 48 dp | Pulsante Errore, popup semplice | Pulsante Errore, popup semplice diverso | Stesso pulsante e stesso popup video, con dati adattati. |
| Copia dal popup | Non chiude il popup | Il listener del dialogo chiude dopo la copia | Non chiude il popup | Stessa semantica del video. |
| Totale report | In fondo | Prima delle righe | Prima delle righe | Stessa struttura e posizione del video. |
| Riprova falliti | Presente | Presente | Assente | UI comune; per immagini serve anche conservare i dati necessari alla nuova richiesta. |
| Pausa/salto/ripresa duratura | Disponibili | Non disponibili | Non disponibili | Funzioni separate da progettare, se richieste; non simularle con pulsanti inattivi. |
| Riquadri impostazioni | Costruzione video propria | Costruzione separata | Costruzione separata | Un solo telaio e componenti di modulo. |
| Linee nei riquadri impostazioni | Titoli senza linea automatica | Titoli con linea anche a inizio riquadro | Alcuni titoli con linea anche a inizio riquadro | Separare titolo e separatore; linea solo tra contenuti reali. |
| Copia configurazione corrente | Solo alcuni campi | Include più campi | Include più campi | Definire una semantica comune e dichiarare cosa copia. |
| Diagnostica | Log, metadati selezione, HDR, cancellazione | Log e dettagli motore | Inventario JSON e log, senza stesso menu | Telaio e azioni comuni; formati e prove specifiche espliciti. |

### Causa verificata delle linee isolate

`AppUi.sectionHeading()` chiama sempre `divider()` prima di disegnare il titolo. `AudioSettingsScreen` e `ImageSettingsScreen` lo chiamano anche come primo contenuto di pannelli appena creati. `SettingsActivity` video usa invece `label()` per quelle intestazioni. La linea iniziale, quindi, non separa due contenuti: deriva dall'uso di un componente non adatto a quel punto della pagina.

## 4. Componenti da condividere realmente — proposta

| Componente comune | Dati forniti dalle sezioni | Responsabilità da lasciare fuori dalla vista |
|---|---|---|
| Telaio pagina e riquadri | Titoli, descrizioni, sezione attiva | Stato della sessione e navigazione del servizio. |
| Selezione e dettagli | Nomi, indice, gruppi di metadati, stato lettura | Accesso ai file e lettori dei metadati. |
| Destinazioni | Piano effettivo, percorsi, file interessati, stato accesso | Autorizzazioni Android, verifiche del provider, assegnazione della destinazione alla conversione. |
| Riga permesso cartella | Verificato/da autorizzare/in verifica, percorso, azione | Decisione se la cartella sia davvero utilizzabile. |
| Campo impostazione | Etichetta, valore, intervallo, scelte, aiuto | Parametri del codec e loro validazione. |
| Modulo impostazioni | Cartella, gruppi di parametri, valori correnti e predefiniti | Salvataggio delle preferenze specifiche e applicazione alla prossima coda. |
| Cruscotto conversione | Stato, file, progresso disponibile, parametri effettivi, misure | Codifica e gestione della coda. |
| Riga risultato e totale | Esito, nome, byte, URI, messaggio/stato/dettagli | Calcolo degli esiti e apertura del formato appropriato. |
| Popup risultato/errore | Titolo, messaggio, ultimo stato opzionale, dettagli tecnici | Nessuna mutazione della coda. |
| Conferme comuni | Operazione, oggetto interessato, effetto e callback | Verifica finale che sessione/file siano ancora quelli della richiesta. |
| Diagnostica | Azioni disponibili e loro descrizioni | Lettura/esportazione dei log e prove specifiche. |

«Condiviso» significa che dimensioni, colori, ordine, apertura/chiusura e posizione dei pulsanti vengono definiti in un solo posto. Le sezioni passano dati e callback, non ricostruiscono la stessa vista con varianti locali.

## 5. Primo intervento proposto: riquadro 1

### Comportamento di arrivo

| Situazione | Comportamento comune atteso |
|---|---|
| Nessun file | Seleziona e Svuota secondo stato; messaggio iniziale. Nessun controllo di destinazione, percorso o scheda permesso della coda. |
| File selezionati | Contatore, carosello, dati del selezionato, Dettagli e Rileggi. Compare il blocco destinazioni. |
| Lettura metadati | Stato di lettura esplicito; dati di un altro file non devono apparire come dati del selezionato. |
| Controllo cartelle | Stato In verifica; nessun verde anticipato, avvio non disponibile. |
| Permesso mancante | Scheda rossa e azione appropriata; avvio non disponibile. |
| Tutto verificato | Schede verdi; avvio disponibile solo se anche gli altri requisiti della coda sono soddisfatti. |
| Override globale attivo | Controlli individuali non modificabili; scelte individuali conservate. |
| Conversione o scelta cartella in corso | Modifiche della destinazione bloccate secondo le stesse regole; consultazione dei dati trattata esplicitamente. |
| Svuota | Stato iniziale, destinazioni della coda nascoste; originali e impostazioni conservati. |
| Cambio sezione / ricreazione | Bozza coerente; metadati già letti riusati dove ancora in cache; callback precedenti non devono sovrascrivere dati più recenti. |

La cartella predefinita nelle **impostazioni** può essere scelta anche senza file: è una preferenza, non il blocco destinazioni della coda nella **home**.

### Pezzi di lavoro, nell'ordine

1. Concordare la portata delle destinazioni: proposta di parità col video anche per file singolo, origine e override della coda. Precisare rimozione individuale e portata di Rileggi.
2. Estrarre/adattare il pannello destinazioni video, eliminando etichette e dipendenze codificate solo per i video. Verificare prima che il video conservi il comportamento di riferimento.
3. Collegare audio: stesso pannello e stati; adattare bozza e richiesta al servizio se si approva il piano per file. Verificare ritorno dal selettore, annullamento e permessi mancanti.
4. Collegare immagini con le stesse regole e verifiche. Nessun rifacimento del pannello nella sezione immagini.
5. Controllare affiancati gli stati iniziale, selezionato, non autorizzato, verificato, occupato e svuotato, in chiaro e scuro, a larghezza telefono.
6. Solo quando il riquadro 1 è coerente, passare a righe/popup del report; poi impostazioni e separatori; poi cruscotto e diagnostica.

## 6. Verifica proporzionata a ciascun pezzo

- Documentazione e inventario: controllo dei riferimenti e del diff; nessuna compilazione APK e nessuna suite da 194 test.
- Riquadro 1: test mirati di selezione/carosello/cache, piano destinazioni, permessi, ritorno dal selettore e reset; anteprime dei soli stati coinvolti.
- Report: test mirati di risultato, apertura URI, popup, copia e totale; anteprime di successo, errore e nessun risparmio.
- Impostazioni: test mirati di Salva/Indietro, copia, ricreazione e applicazione differita; anteprime dei riquadri.
- Controlli condivisi che possono incidere su tutte le sezioni: ampliare la verifica alle sezioni collegate, indicando perché.
- Test nativi dei codec: soltanto se il pezzo modifica davvero la conversione o il confine col motore, non per un separatore o un pulsante.

Riferimenti di test esistenti: `VideoCarouselTest`, `ImageCarouselCacheTest`, `FolderPickerActivityTest`, `HomeResetTest`, `CollapsibleCardsTest`, `WorkspaceNavigationTest`, `HomeFailureReportTest`, `ImageWorkReportTest`, `AudioSettingsTest`, `ImageSettingsTest` e i render test delle tre sezioni.

## 7. Riferimenti al codice esaminato

- Struttura: `HomeScreen`, `HomeSections`, `AppSections`, `AppHeader`, `AppNavigation`, `AppUi`.
- Selezione: `HomeFilesSection`, `HomeSelection`, `SelectedItemView`, `VideoMetadataCache`, `HomeReset`.
- Destinazioni: `HomeDestinationsSection`, `HomeDestinations`, `DestinationPlan`, `OriginDirectoryPanel`, `HomeDirectoryRow`.
- Parametri: `HomeCompressionSection`, `HomeCompression`, `AppSettings`.
- Conversione: `HomeConversionSection`, `HomeConversion`, `HomePlaybackControls`, `HomeSavedSession`, `HomeRetry`, `HomeOutputCheck`.
- Report: `WorkReportView`, `HomeFailureReport`, `AudioReport`, `ImageWorkReport`.
- Diagnostica: `HomeDiagnosticsSection`, `HomeDiagnostics`.
- Confronto: `AudioScreen`, `AudioController`, `AudioSelection`, `ImageScreen`, `ImageController`, `ImageRunPresenter`, `SettingsActivity`, `AudioSettingsScreen`, `ImageSettingsScreen`, `ImageQualityControls`.
