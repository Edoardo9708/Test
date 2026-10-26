# Risultati di esecuzione: evidenze storiche

[Guida](README.md) · [Indice dei metodi](index.md) · [Audit originale](../test-coverage-audit.md)

Questa pagina è separata dalle schede statiche. Aggiornare una scheda o la sua impronta **non produce un nuovo risultato di esecuzione**.

| Evidenza | Superati | Falliti/errori | Saltati | Wall time | Tempo JUnit |
|---|---:|---:|---:|---:|---:|
| Ultima prova completa registrata nell'audit del 25 ottobre 2026 | 332 | 0 | 0 | 106,650 s | 89,538 s |
| Task isolato rapido della stessa prova | 36 | 0 | 0 | 5,359 s | 0,089 s |
| Task JVM esteso della stessa prova | 296 | 0 | 0 | 100,369 s | 89,449 s |

Fonte: valori già riportati dall'audit e dal riepilogo locale `.tools/test-results/audited-full.json`. I due task registravano `gradle_reused=false` e rispettivamente 36/296 casi eseguiti. Gli artefatti locali XML/log non sono pubblicati in questa documentazione: questa tabella conserva l'evidenza storica riassunta, non un badge verificabile da GitHub Actions.

Il trasferimento in `docs/testing/` non ha rieseguito le suite JVM, i programmi Java/Python dell'app, le prove native o i test sul dispositivo. Le verifiche del nuovo strumento documentale sono controlli separati e non vanno aggiunte ai 332.

Per una futura esecuzione registrare comando/selezione, data, identità degli input o revisione e stato del worktree, conteggi pass/fail/skip, durata misurata e posizione degli artefatti disponibili. Distinguere risultati eseguiti da riuso Gradle e da semplici verifiche statiche; non inventare tempi per schede mai eseguite singolarmente. Nessuna percentuale di copertura del codice è stata misurata.

## 26 ottobre 2026 — autorizzazioni Foto in Compose

Worktree `ui/morphe-foto-prototype`, HEAD `0939603`, con le 39 variazioni locali preesistenti conservate e la nuova estrazione `OriginDirectoryAccess` / presentazione `PhotoPermissionsPane` non committata. Comandi eseguiti con JDK 17 e cache Gradle locali, offline, attraverso il runner del repository:

| Comando | Eseguiti | Falliti/errori | Saltati | Wall time | Tempo JUnit |
|---|---:|---:|---:|---:|---:|
| `scripts/test-local.ps1 integration --group photo --group files`, prima delle ultime rifiniture | 115 | 0 | 0 | 138,953 s | 63,948 s |
| `scripts/test-local.ps1 full`, stato finale dei sorgenti | 335 | 0 | 0 | 119,739 s complessivi | 84,696 s |
| Task rapido della prova completa | 36 | 0 | 0 | 5,726 s | 0,105 s |
| Task integrazione della prova completa | 299 | 0 | 0 | 113,041 s | 84,591 s |

I 115 mirati sono un sottoinsieme e non si sommano ai 335 finali. Entrambi i task della prova completa riportano `gradle_reused=false`: 36 e 299 casi realmente eseguiti. XML, marker e log sono in `.tools/test-results/runs/20261026-100005-186000-fast/` e `.tools/test-results/runs/20261026-100011-170800-integration/`; la prima selezione è in `20261026-094608-106200-integration/`. La fotografia degli input finali è in `.tools/test-results/last-full.json`.

Le nuove prove verificano nodi Compose reali nel renderer Robolectric, annullamento, autorizzazione pendente durante ricreazione, revoca, limite di 100 righe su 105 cartelle, piano completo e rifiuto delle azioni obsolete. Esaminato anche il PNG generato `app/build/reports/ui/image-files.png`: la scheda dei permessi e il riepilogo sono leggibili sopra il dock nella configurazione della fixture. Nessuna golden e nessuna misura di fluidità. Non sono stati eseguiti codec JNI, installazioni o test sul telefono.

Un primo tentativo nella sandbox non ha avviato Gradle perché la cache predefinita risolveva a `C:\.gradle`, non scrivibile. Il test è stato poi eseguito con autorizzazione nelle cache locali esistenti; quel tentativo fallito non viene contato come esecuzione dei casi.

Eseguito anche `:app:lintDebug` offline, con due worker e JDK 17: **0 errori, 135 avvisi**, build riuscita in 2 min 11 s; dettaglio in `app/build/reports/lint-results-debug.xml` e log locale `.tools/permissions-lint.log`. Il primo comando lint aveva una quotatura PowerShell errata e non aveva selezionato il task; è stato corretto senza cambiare i sorgenti. Il controllo finale `scripts/test-docs.py check` conferma 335 schede, zero revisioni pendenti e zero errori; `git diff --check` non segnala errori di spaziatura. Questi controlli non aggiungono casi al totale JVM.

## 26 ottobre 2026 — interruttore della sfocatura Foto

Stesso branch e HEAD, con le modifiche locali precedenti conservate e l’aggiunta non committata di `PhotoAppearance`, checkbox nelle impostazioni Foto e rami Compose senza sorgente/consumer Haze. La preferenza è immediata, predefinita attiva, separata da opzioni e revisione della compressione.

`scripts/test-local.ps1 integration --group photo --group files`: **118 casi realmente eseguiti, zero fallimenti/errori e zero saltati**, `gradle_reused=false`, 88,804 s wall / 62,153 s JUnit. Artefatti locali in `.tools/test-results/runs/20261026-101930-342000-integration/`. È una selezione mirata, non una nuova esecuzione completa dei 337 metodi ora catalogati.

Le due nuove prove verificano default attivo, salvataggio immediato con Indietro, ricreazione delle impostazioni e di Foto, rilettura al resume, indipendenza da Usa configurazione/default/coda e rendering senza blur con navigazione visibile. Esaminato `app/build/reports/ui/image-floating-no-blur.png`: superfici traslucide e comandi visibili nella fixture. Non verifica blur hardware, fluidità o metriche GPU sul telefono. `scripts/test-docs.py check` conferma 337 schede senza revisioni pendenti o errori.

Eseguiti `:app:lintDebug :app:assembleRelease` offline, senza clean e con due worker Gradle: build finale riuscita in 18 min 31 s, **lint 0 errori / 135 avvisi**. Primo tentativo interrotto perché il comando bash risolveva al launcher WSL senza distribuzione: corretto soltanto il PATH della build locale verso MSYS, poi ripresa la build con cache e oggetti conservati. Log in `.tools/blur-apk-build.log` e `.tools/blur-apk-build-first.log`. Questa compilazione nativa non è una prova di conversione.

APK release locale `.tools/apks/CodecProbe-1.05-blur-toggle.apk`, versione 1.05 (105), applicationId originale, firma verificata contro il certificato permanente locale; verificate sei librerie ELF64 ARM64, licenze e stringhe/classe della nuova preferenza nei DEX. SHA-256 `c410c4e008e7b99ae490ab36a2676fea6ca20795518b3e0aff37d915d82ac169`. Nessuna installazione o prova sul telefono.

## 26 ottobre 2026 — prova del solo collasso esterno dei dettagli

Stesso branch/HEAD e modifiche locali precedenti conservate. Nuovo test `PhotoComposeTest.detailsCollapseDoesNotDropAnExternalGapAtDisposal`: misura l’altezza non ritagliata del pannello File selezionati, campiona 24 frame con clock Compose manuale e controlla lo scarto nel primo frame dopo la rimozione dei dettagli.

| Stato del layout, comando `scripts/test-local.ps1 integration --group photo` | Superati | Falliti | Saltati | Wall time | Tempo JUnit |
|---|---:|---:|---:|---:|---:|
| Layout precedente, con nuovo test diagnostico | 104 | 1 | 0 | 71,736 s | 59,077 s |
| Margine incluso nel collasso esterno dei dettagli | 105 | 0 | 0 | 73,000 s | 59,191 s |

Il fallimento iniziale è intenzionale e riproduce un salto finale di **12,5 dp**; la stessa asserzione passa dopo la correzione, con soglia massima **2 dp**. In entrambi i casi i 105 test sono realmente eseguiti (`gradle_reused=false`). Report rispettivamente in `.tools/test-results/runs/20261026-111939-564200-integration/` e `20261026-112145-647200-integration/`. Un tentativo preliminare aveva soltanto un errore nell’accesso alle dimensioni nell’API del test, corretto prima delle misure: zero casi eseguiti, non incluso nella tabella.

Produzione modificata solo in `PhotoFilesPane`: il gruppo di pulsanti e il fold dei dettagli condividono una Column senza spaziatura esterna; il margine superiore di 12 dp è interno al fold. Nessuna modifica a `PhotoFold`, durata, curva, Mostra tutto, destinazioni o opzioni avanzate. La prova certifica la geometria nella fixture Robolectric, non la durata dei frame o la fluidità sul Samsung. Il catalogo contiene ora 338 metodi; questa è una selezione Foto e non una nuova suite JVM completa.

Lint/build release offline riusciti in 2 min 44 s, senza clean: **0 errori lint / 135 avvisi**, librerie native già costruite riutilizzate. Log `.tools/details-apk-build.log`. APK `.tools/apks/CodecProbe-1.05-details-fold.apk`, versione 1.05 (105), certificato permanente verificato, sei ELF64 ARM64 e marcatori dell’interfaccia controllati; SHA-256 `baa11d199394b30db6458205e4ebd0fcfe01dc422d8c94106bd148199489d2e1`.

Installato come aggiornamento sul Samsung SM-S916B con `adb install -r`, esito `Success`, senza disinstallare o cancellare dati. Nessuna interazione automatica con la pagina Foto né misura della fluidità sul telefono: resta il confronto visivo dell’utente, limitato al riquadro esterno aperto da Dettagli. Controlli documentali finali: 338 schede, zero revisioni pendenti/errori; `git diff --check` senza errori di spaziatura.
