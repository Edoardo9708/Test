# CodecProbe — Istruzioni di sviluppo

- Scrivi codice ordinato, modulare, efficiente e manutenibile. Riutilizza i componenti quando utile, evitando complicazioni e astrazioni superflue.
- Concentrati sulla richiesta, scegliendo la soluzione più semplice. Non aggiungere attività non richieste.
- Consulta solo i sorgenti necessari. La documentazione del repository è disponibile come riferimento, ma non deve essere letta preventivamente.
- L'interfaccia finale deve essere realizzata in Kotlin/Jetpack Compose, senza introdurre nuove interfacce Java View o wrapper AndroidView. La logica Java e i motori C/C++ possono rimanere.
- Per gli esperimenti, modifica il minimo indispensabile e produci il risultato da provare. Esegui test e verifiche in misura proporzionata alla modifica; evita documentazione e refactoring non richiesti.
- Preserva sempre le modifiche locali e gli altri branch/worktree. Non eseguire reset, clean distruttivi, commit, push o merge senza richiesta esplicita.
- Al termine, comunica brevemente cosa hai modificato ed eventuali problemi rimasti.
- Per compilare e installare APK locali, utilizza lo script di firma già esistente in C:\Users\edoar\.codecprobe-signing, che riutilizza la chiave permanente locale e la password protetta con DPAPI. Non utilizzare APK con firma debug predefinita, non generare nuove chiavi e non disinstallare l'app per aggiornarla.