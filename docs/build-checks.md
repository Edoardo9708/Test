# Controlli e tempi della build

**Ambito: CI GitHub Actions.** Questo ordine descrive la pipeline configurata nel workflow, non dipendenze garantite di un generico comando Gradle locale. Verificare workflow e script del branch; per le prove locali usare [local-testing.md](local-testing.md), per la selezione CI [ci-checks.md](ci-checks.md). Non è documentazione da caricare per ogni modifica all'app.

La build esegue prima compilazione Java, test Robolectric e lint. Solo se passano compila e firma l’APK: un errore Java o un test fallito non deve aspettare la compilazione dei codec.

I test Java restano completi. Le anteprime generate sono dichiarate come output dei test, per recuperarle insieme ai risultati quando Gradle riusa una verifica identica. Gradle usa la propria cache delle attività; test o sorgenti diversi invalidano automaticamente i risultati relativi. La selezione confronta il codice con l’ultimo push verificato con successo, così include anche modifiche rimaste senza verifica per un annullamento o un errore precedente. Se il riferimento non è disponibile vengono eseguiti tutti i controlli. Gli smoke test nativi restano selezionati da `scripts/ci-scope.py`, con controllo completo per modifiche native, configurazioni sconosciute e richiesta manuale completa.

La cache nativa conserva `vendor`, `app/.cxx`, `app/build/intermediates/cxx` e la toolchain NDK della versione fissata in Gradle. Il ripristino precede sdkmanager, così preserva anche le date degli header e del compilatore usate da Ninja per decidere cosa ricompilare. Non conserva APK, DEX o chiavi di firma. La chiave deriva da tutti i file nativi tracciati, script di preparazione/patch/compilazione, configurazione Gradle e workflow, oltre ai percorsi del workspace e SDK e al sistema del runner. Non ci sono chiavi di ripiego: sorgenti o strumenti diversi richiedono una nuova compilazione. Le sole righe `versionCode` e `versionName` non invalidano le librerie native.

Dopo il ripristino vengono comunque eseguiti preparazione e controlli delle dipendenze, CMake/Gradle, firma e verifica delle librerie effettivamente incluse nell’APK. La prima build con una chiave nuova resta completa e popola la cache; il vantaggio sulle librerie si misura dalle build successive. Le patch idempotenti non riscrivono sorgenti invariati, così Ninja può riusare gli oggetti compilati.

Le modifiche limitate a documentazione e licenze non avviano una build e non annullano una build del codice già in corso. Gli aggiornamenti del codice dello stesso ramo annullano le esecuzioni precedenti ancora in corso. Le riesecuzioni possono sostituire i propri artifact generati senza conflitti di nome.

Per verificare localmente le regole:

```sh
python3 -m unittest discover -s tests -p test_ci_scope.py -v
python3 -m unittest discover -s tests -p test_ci_native_key.py -v
```
