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
