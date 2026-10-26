# Documentazione tecnica dei test di CodecProbe

[Indice di tutti i metodi](index.md) · [Schede da revisionare](review-needed.md) · [Risultati storici](esecuzioni.md) · [Audit originale](../test-coverage-audit.md) · [Comandi di esecuzione](../local-testing.md)

Questa documentazione riusa l'audit esistente dei **332 metodi in 94 classi**. Descrive il codice del worktree `ui/morphe-foto-prototype`, incluse le modifiche locali presenti al momento del trasferimento. Non è una nuova esecuzione dei test, né una misura di copertura del codice.

Il catalogo corrente contiene **338 metodi in 94 classi**: alla base dell’audit sono stati aggiunti tre test di `PhotoComposeTest` per autorizzazioni Compose, annullamento, ricreazione/revoca e snapshot limitati con rifiuto di azioni obsolete. Altri due test verificano preferenza della sfocatura, ritorno dalle impostazioni e rendering senza vetro sfocato. Un ulteriore test campiona il collasso dei dettagli per verificare lo scalino finale alla rimozione. Le nuove schede descrivono i contratti; gli esiti di esecuzione restano separati.

**Contesto della pubblicazione:** il commit documentale esclude le modifiche applicative/UI e i relativi aggiornamenti dei test rimasti locali. Alcuni sorgenti consultabili su GitHub possono quindi differire dalla fotografia auditata: per leggere esattamente il metodo documentato usare il corpo integrale nella scheda. In particolare, nello snapshot locale `ImageUiTrialTest` contiene `navigationRetainsDraftAndQualityAcrossRecreation` e `composeIsTheOnlyWorkspaceRootAndNoHiddenPhotoPanelsAreBuilt`; nel sorgente precedentemente committato occupavano quei posti due metodi con nomi e verifiche diversi. Il totale resta 332, ma non si dichiara identità fra i due inventari. Su un clone privo delle modifiche locali il verificatore deve segnalare le differenze, non certificare falsamente l'allineamento. I link dell'indice aprono i file, senza numeri di riga che potrebbero riferirsi alla sola versione locale.

| Area | Schede |
|---|---|
| Video | [Video](video.md) |
| Audio | [Audio](audio.md) |
| Foto | [Foto](foto.md) |
| File, cartelle, metadati e Analisi | [File e metadati](file-metadati.md) |
| Code, controlli e operazioni asincrone | [Code](code.md) |
| UI e componenti condivisi | [UI e componenti condivisi](ui-condivisi.md) |

L'indice assegna ogni metodo a una sola pagina, anche quando riguarda più aree. Le schede mantengono la sintesi e i limiti già revisionati nell'audit. I dati puntuali, la sequenza e gli assert sono consultabili nel corpo del metodo trascritto integralmente, con collegamento al sorgente. I setup/helper della classe sono indicati separatamente: non tutte le fixture di una classe si applicano a ogni metodo. Il corpo trascritto non è un esempio nuovo o uno pseudotest: è una fotografia del test esistente, controllata dal verificatore.

Le descrizioni sono **ricavate dal codice**, non da una traccia runtime. Le liste di chiamate riprendono i riferimenti statici dell'audit: non sono grafi completi di esecuzione, e una chiamata a un helper va letta insieme al relativo codice. Quando un test delega a un helper, il collegamento al sorgente e il contesto della classe ne esplicitano il ruolo. Nuove spiegazioni semantiche richiedono lettura del codice da parte di Codex; lo script non le inventa.

## Categorie e confini

All'inizio del catalogo: **A 88, B 201, C 43, D 0, E 0**. Il conteggio attuale dei metodi è nell'[indice](index.md); eventuali variazioni richiedono revisione. Le categorie descrivono il contenuto delle prove, non il nome del task Gradle:

- **A:** logica, calcoli, nomi e stati; callback/input sintetici possono alimentare un singolo algoritmo.
- **B:** integrazione con dipendenze Android o esterne simulate. Può includere vere scritture di file attraverso un provider di prova.
- **C:** IO/parsing/serializzazione reali sul PC, senza sostituire il componente verificato.
- **D:** codec/librerie native dell'app realmente eseguiti.
- **E:** test su dispositivo o emulatore Android.

Robolectric, Compose testing e `GraphicsMode.NATIVE` non certificano codec dell'app, provider SAF reali o fluidità del Samsung. Le prove native, i programmi Java autonomi e i test Python dell'infrastruttura sono censiti nell'[appendice dell'audit](../test-coverage-audit.md#prove-esistenti-fuori-dai-332), **fuori dai 332** e fuori dal discovery di questo catalogo. `full` indica la suite JVM, non tutte queste prove separate.

## Aggiornamento locale incrementale

Richiede soltanto Python 3.10+ già disponibile. Non installa dipendenze, non invoca Gradle/Git/ADB, non usa rete e non genera report di esecuzione. Da PowerShell, nella radice del repository:

```powershell
python scripts/test-docs.py check
python scripts/test-docs.py update
python scripts/verify-test-docs.py
```

Se `python` non è nel PATH, usare l'eseguibile Python già configurato sul PC, come per `test-local.py`.

`check` è in sola lettura. Confronta i metodi presenti con le impronte dell'ultima revisione documentale, verifica schede, corpi trascritti, marcatori, collegamenti e indice; esce con **0** se coerente, **1** per revisioni/errori di coerenza, **2** per input/parser non validi. Non dice che i test passano. `verify-test-docs.py` esegue separatamente i controlli del verificatore su piccoli repository fittizi in cartelle temporanee sotto `.tools/`; non modifica o esegue i test dell'app. Il suo riepilogo resta locale e ignorato.

`update` riscrive **soltanto `index.md` e `review-needed.md`**, e solo se il loro contenuto cambia. Mostra aggiunte, rimozioni, modifiche e richieste di revisione; restituisce 1 finché rimangono questioni aperte. Non riscrive le sei pagine, non aggiorna le impronte approvate e non considera automaticamente corrette le vecchie descrizioni.

### Modifica o aggiunta di un test

1. Eseguire `update` e leggere le schede segnalate. Un nuovo metodo compare nell'indice anche prima che esista la sua scheda.
2. Codex legge metodo, setup/helper e comportamento applicativo interessato. Aggiorna manualmente dati, sequenza, chiamate, simulazioni, assert, comportamento e limiti, usando il formato delle schede esistenti. Per i nuovi test scegliere una delle sei pagine, aggiungere una voce nell'indice locale della classe e una scheda con `<!-- test: Classe.metodo -->` / `<!-- /test -->` e ancora stabile. L'ancora richiesta è `test-` più i primi 16 caratteri SHA-256 UTF-8 di `Classe.metodo`; può essere ottenuta con `anchor()` in `scripts/test-docs.py`.
3. Trascrivere il corpo attuale nel solo blocco `java`/`kotlin` della scheda. I sorgenti restano la fonte autorevole; la trascrizione deve coincidere esattamente, ignorando soltanto lo spazio esterno al corpo.
4. Registrare **dopo la lettura e revisione umana**, per ogni metodo interessato:

```powershell
python scripts/test-docs.py review --test NomeClasse.nomeMetodo
python scripts/test-docs.py check
```

`--test` è ripetibile. `review` registra le impronte dei soli metodi indicati e della relativa scheda; non riscrive descrizioni e non equivale a una prova eseguita. Una descrizione ancora corretta può restare uguale dopo la lettura; non modificarla artificialmente per cambiare un hash. I link ai file restano stabili quando cambiano le righe; modifiche al contesto del file possono richiedere una revisione precauzionale anche quando il corpo del metodo è invariato.

### Rimozione di un test

`update` conserva un avviso di rimozione finché Codex non elimina la scheda e la voce dell'indice locale della classe (o le trasferisce in un documento storico esterno alle sei pagine). Poi `review --test Classe.metodo` elimina la vecchia voce dal catalogo. Lo script rifiuta la registrazione della rimozione finché rimane una scheda attiva obsoleta.

### Cambiamenti applicativi, helper o configurazione

- Modifiche al corpo interessano il metodo. Modifiche a import, annotazioni di classe, setup o helper nello stesso file interessano la classe. Le dipendenze fra file di test vengono seguite con riferimenti lessicali transitivi: è una selezione conservativa, non una comprensione semantica. Un cambiamento a una classe-fixture condivisa può ampliare le revisioni a più classi.
- Le risorse sotto `app/src/test` e i file helper sono input della revisione. Una risorsa cambiata amplia prudenzialmente la selezione. Il controllo non dipende da commit, timestamp o risultati XML: legge i contenuti presenti, compresi nuovi file non committati.
- Sorgenti/risorse/native sotto `app/src/main`, configurazioni Gradle/TOML/lockfile nei percorsi standard, wrapper e `buildSrc`/`build-logic` hanno una fotografia separata. **Ogni cambiamento applicativo richiede valutazione d'impatto di Codex**: fino alla valutazione compare un avviso conservativo su tutte le schede. Nessuna pagina descrittiva viene riscritta. Questo evita di saltare un legame indiretto che un indice statico semplice non conosce.

Dopo avere individuato e revisionato manualmente i test interessati, motivando perché gli altri non richiedono cambiamenti:

```powershell
python scripts/test-docs.py review --application --test NomeClasse.nomeMetodo --reason "Contratto interessato e motivazione della selezione verificata nel codice"
```

Se, dopo la lettura, nessun contratto documentato è interessato:

```powershell
python scripts/test-docs.py review --application --unaffected --reason "Motivo concreto per cui i contratti documentati restano invariati"
```

Questi comandi sono **attestazioni esplicite di una revisione di Codex**, non deduzioni automatiche. Una modifica ai test ancora pendente non viene approvata da `--unaffected`. Non usarli soltanto per far sparire un avviso. Nuovi percorsi di configurazione non standard o nuove forme sintattiche di test richiedono revisione del verificatore; una firma non riconosciuta viene segnalata, non ignorata.

## File permanenti e file locali

`catalog.json` conserva soltanto schema, impronte SHA-256, percorsi relativi e l'ultima motivazione di revisione applicativa. È una **base documentale versionata**, non una cache dei test o un log. Non contiene credenziali, ambiente, percorsi di media personali o risultati runtime. `index.md` e `review-needed.md` sono viste documentali intenzionalmente versionate, aggiornate senza riscrivere i file identici.

Il discovery legge solo `app/src/test`; gli input applicativi hanno radici esplicite. `.tools`, `.gradle`, `.git`, `vendor`, `build`, `.cxx`, `__pycache__`, file `.class`/`.pyc`, log e risultati generati non entrano nell'inventario. XML/PNG conservati intenzionalmente in `src/test/resources` sono invece fixture, non report, e devono poter invalidare una revisione. I report Gradle restano nelle cartelle locali già ignorate. Le prove del verificatore usano directory temporanee isolate, non modifiche ai test dell'app.

## Limiti del controllo automatico

Il verificatore riconosce la sintassi Java/Kotlin `@Test` attualmente usata, anche corpi Kotlin a espressione. Conta **metodi**, non eventuali combinazioni parametrizzate o invocazioni runtime future. Classi omonime, metodi in classi annidate o con nome diverso dal file e test JUnit 3 vengono rifiutati finché non viene aggiunto supporto esplicito. Le impronte rilevano cambiamenti, non provano che una spiegazione sia semanticamente giusta: questa responsabilità resta a Codex. Collegamenti e trascrizioni coerenti non sostituiscono una prova di comportamento.
