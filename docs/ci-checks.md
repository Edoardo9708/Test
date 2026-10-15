# Selezione dei controlli automatici

La versione dell’app non cambia quando si modifica soltanto la CI.

| Modifica | Controlli |
| --- | --- |
| Solo README, licenza o `docs/` | Test del selettore; nessuna compilazione APK |
| Interfaccia, risorse, nomi, cartelle, codice Java dell’app o test Java | Compilazione APK, tutti i test Java rapidi, lint, anteprime UI, verifica firma/versione/architettura |
| Codice C/C++, x265, classi JNI/encoder, toolchain, configurazione di build, CI o dipendenze sconosciute | Anche prove native SDR, ARM64 e HDR |

Le prove native aggiuntive ricompilano x265 per host e ARM64, codificano campioni e li decodificano con FFmpeg. Non eseguono il percorso Java `VideoConverter`: per questo una modifica alla sola copia audio non richiede automaticamente queste ricompilazioni. Restano i test pertinenti al codice Java modificato e la verifica sul telefono per il comportamento delle API Android.

Anche una compilazione rapida contiene e compila le librerie native necessarie all’APK. Quello che viene saltato è la loro ricompilazione aggiuntiva per le prove host/QEMU. I test Java vengono mantenuti tutti perché rapidi e utili a verificare i collegamenti tra sezioni dell’app.

## Regole

- `scripts/ci-scope.py` confronta `before` e `after` dell’intero push, non soltanto l’ultimo commit.
- Lo storico completo viene recuperato dal job `scope`. Le rinomine considerano sia il vecchio sia il nuovo percorso; anche le cancellazioni entrano nel confronto.
- Un percorso non riconosciuto, un diff non disponibile, un primo push o un evento non valido attivano tutti i controlli.
- Le classi che caricano le librerie o definiscono il confine JNI (`EncoderPresetPreview`, `X265Probe`, `HdrProbe`, `HdrTranscoder`) attivano le prove complete.
- Per gli altri file Java si controllano anche le righe modificate: dichiarazioni JNI, chiamate `nativeXxx(...)` e caricamenti di librerie attivano le prove complete. Se non si può leggere il diff si esegue tutto.
- Una modifica al selettore o al workflow attiva le prove complete per verificare la nuova procedura.
- Il riepilogo GitHub Actions e il log del job di selezione indicano i file modificati, la scelta e il motivo. I test non eseguiti risultano saltati, non superati.

## Esecuzione manuale

In Actions → Build APK → Run workflow, `full_tests` è attivo per impostazione predefinita. Disattivandolo si forza una compilazione rapida con i controlli Java/APK; lasciandolo attivo si eseguono anche tutte le prove native.

## Test della selezione

`python3 -m unittest discover -s tests -p test_ci_scope.py -v`

Comprendono modifiche UI/audio, codice nativo, dipendenze sconosciute, documentazione, modifiche miste, primo push, diff fallito e un repository temporaneo con più commit e una rinomina.
