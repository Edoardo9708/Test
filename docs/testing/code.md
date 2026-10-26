# Code e operazioni asincrone

[Guida](README.md) · [Indice completo](index.md) · [Revisioni](review-needed.md) · [Esecuzioni](esecuzioni.md)

Descrizioni statiche trasferite dall’audit: non attestano nuove esecuzioni. Le fixture condivise sono descritte a livello di classe; dati specifici, ordine delle operazioni e assert sono nel corpo integrale di ogni metodo. Gli helper richiamati e gli eventuali `@Before`/`@After` sono consultabili nel sorgente collegato. Lo script di aggiornamento non riscrive questa pagina.

## Classi

- [BatchRetryTest](#class-batchretrytest)
- [CancellationConfirmationTest](#class-cancellationconfirmationtest)
- [ConversionControlBoundaryTest](#class-conversioncontrolboundarytest)
- [ConversionControlTest](#class-conversioncontroltest)
- [ConversionRunGateTest](#class-conversionrungatetest)
- [HomePlaybackControlsTest](#class-homeplaybackcontrolstest)
- [HomeSavedSessionTest](#class-homesavedsessiontest)
- [ImageNotificationTest](#class-imagenotificationtest)
- [ImagePauseTest](#class-imagepausetest)
- [ImageSessionServiceTest](#class-imagesessionservicetest)
- [ImageSessionStoreTest](#class-imagesessionstoretest)
- [QueuePlaybackServiceTest](#class-queueplaybackservicetest)
- [RecoveryLifecycleTest](#class-recoverylifecycletest)
- [RecoveryPresentationTest](#class-recoverypresentationtest)
- [ResumeParametersTest](#class-resumeparameterstest)
- [SavedSessionServiceTest](#class-savedsessionservicetest)
- [SavedSessionTest](#class-savedsessiontest)

<a id="class-batchretrytest"></a>
## BatchRetryTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/BatchRetryTest.java) · 2 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** BatchRetry.select/save/load.

**Riferimenti produttivi:** [BatchRetry](../../app/src/main/java/it/edo/codecprobe/BatchRetry.java).

**Preparazione, file e simulazioni condivisi:** Intent con tre URI e opzioni arbitrarie; SharedPreferences simulate nel secondo test.

**Confini comuni dell’audit:** Non avvia conversione/riprova né verifica tutti gli indici invalidi. Il clone controllato protegge liste specifiche, non ogni extra futuro.

**Metodi:** [selectedFailuresKeepTheMatchingDestinationsApprovedNamesAndSettings](#test-64331cfdbe1390f8) · [privateStoreSurvivesReloadAndRejectsCorruptData](#test-1510269c66e4a541)

<!-- test: BatchRetryTest.selectedFailuresKeepTheMatchingDestinationsApprovedNamesAndSettings -->
<a id="test-64331cfdbe1390f8"></a>
### `BatchRetryTest.selectedFailuresKeepTheMatchingDestinationsApprovedNamesAndSettings`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Intent con tre URI e opzioni arbitrarie; SharedPreferences simulate nel secondo test.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/BatchRetryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `BatchRetry.select`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Sottoselezione 2,0 conserva allineamento nomi/cartelle/origini e parametri; copia difensiva, lista vuota e indice duplicato.

**Limiti:** Non avvia conversione/riprova né verifica tutti gli indici invalidi. Il clone controllato protegge liste specifiche, non ogni extra futuro.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void selectedFailuresKeepTheMatchingDestinationsApprovedNamesAndSettings(){
        Context context=RuntimeEnvironment.getApplication();Intent original=queue(context);
        Intent retry=BatchRetry.select(original,List.of(2,0));
        assertEquals(List.of("content://input/2","content://input/0"),retry.getStringArrayListExtra("inputs"));
        assertEquals(List.of("content://dirs/tree/two","content://dirs/tree/zero"),retry.getStringArrayListExtra("folders"));
        assertEquals(List.of("custom-two.mp4","custom-zero.mp4"),retry.getStringArrayListExtra("requestedOutputNames"));
        assertArrayEquals(new boolean[]{true,true},retry.getBooleanArrayExtra("individualOrigins"));
        assertFalse(retry.hasExtra("omitted"));assertFalse(retry.hasExtra("outputNames"));
        assertEquals(17,retry.getIntExtra("crf",0));assertEquals(8,retry.getIntExtra("preset",0));assertTrue(retry.getBooleanExtra("onlySmaller",false));
        original.getStringArrayListExtra("inputs").set(2,"changed");assertEquals("content://input/2",retry.getStringArrayListExtra("inputs").get(0));
        assertNull(BatchRetry.select(original,List.of()));
        try{BatchRetry.select(original,List.of(1,1));fail();}catch(IllegalArgumentException expected){}
    }
```

</details>

<!-- /test -->

<!-- test: BatchRetryTest.privateStoreSurvivesReloadAndRejectsCorruptData -->
<a id="test-1510269c66e4a541"></a>
### `BatchRetryTest.privateStoreSurvivesReloadAndRejectsCorruptData`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Intent con tre URI e opzioni arbitrarie; SharedPreferences simulate nel secondo test.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/BatchRetryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `BatchRetry.select`, `BatchRetry.save`, `BatchRetry.load`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Persistenza privata ricarica retry e opzioni, clear restituisce null e JSON invalido viene respinto.

**Limiti:** Non avvia conversione/riprova né verifica tutti gli indici invalidi. Il clone controllato protegge liste specifiche, non ogni extra futuro.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void privateStoreSurvivesReloadAndRejectsCorruptData(){
        Context context=RuntimeEnvironment.getApplication();Intent expected=BatchRetry.select(queue(context),List.of(1));
        assertTrue(BatchRetry.save(context,expected));Intent restored=BatchRetry.load(context);assertNotNull(restored);
        assertEquals(expected.getStringArrayListExtra("inputs"),restored.getStringArrayListExtra("inputs"));
        assertEquals(expected.getStringArrayListExtra("folders"),restored.getStringArrayListExtra("folders"));
        assertEquals(List.of("custom-one.mp4"),restored.getStringArrayListExtra("requestedOutputNames"));
        assertEquals(17,restored.getIntExtra("crf",0));assertEquals(8,restored.getIntExtra("preset",0));assertEquals(2,restored.getIntExtra("cpu",0));
        assertFalse(restored.getBooleanExtra("bframes",true));assertTrue(restored.getBooleanExtra("lookahead",false));assertTrue(restored.getBooleanExtra("onlySmaller",false));
        assertTrue(BatchRetry.save(context,null));assertNull(BatchRetry.load(context));
        context.getSharedPreferences("failed-conversion-retry",0).edit().putString("request","invalid json").commit();assertNull(BatchRetry.load(context));
    }
```

</details>

<!-- /test -->


<a id="class-cancellationconfirmationtest"></a>
## CancellationConfirmationTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/CancellationConfirmationTest.java) · 4 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** HomeConversion conferma/handleCancellationIntent; ConversionService.notification.

**Riferimenti produttivi:** [HomeConversion](../../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [ConversionService](../../app/src/main/java/it/edo/codecprobe/ConversionService.java).

**Preparazione, file e simulazioni condivisi:** hdrTesting=true impostato dal test, nessun HDR; Notification privata invocata via reflection, Intent e dialog shadow.

**Confini comuni dell’audit:** Conferma imposta AtomicBoolean ma non interrompe codec né verifica file rimossi; stale Intent provato con nessuna vera run.

**Metodi:** [continueOrDismissDoesNotCancel](#test-ccadc6eef9b13a7c) · [onlyPositiveConfirmationCancels](#test-cba350b50f06958d) · [notificationOpensConfirmationActivityInsteadOfStoppingService](#test-6fd37986f0f29d88) · [staleNotificationDoesNotShowCancelDialog](#test-871793ae53a9cffe)

<!-- test: CancellationConfirmationTest.continueOrDismissDoesNotCancel -->
<a id="test-ccadc6eef9b13a7c"></a>
### `CancellationConfirmationTest.continueOrDismissDoesNotCancel`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): hdrTesting=true impostato dal test, nessun HDR; Notification privata invocata via reflection, Intent e dialog shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CancellationConfirmationTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `startFakeHdr(…)`, `shadowOf(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Continua e dismiss non impostano cancel.

**Limiti:** Conferma imposta AtomicBoolean ma non interrompe codec né verifica file rimossi; stale Intent provato con nessuna vera run.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void continueOrDismissDoesNotCancel()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try{
            startFakeHdr(activity);AtomicBoolean cancelled=(AtomicBoolean)activity.home.state.cancelled;
            AlertDialog dialog=(AlertDialog)activity.home.conversion.cancelConfirmation;assertNotNull(dialog);assertFalse(cancelled.get());
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertFalse(cancelled.get());
            ((Button)activity.home.screen.conversion.stop).performClick();((AlertDialog)activity.home.conversion.cancelConfirmation).cancel();shadowOf(Looper.getMainLooper()).idle();assertFalse(cancelled.get());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: CancellationConfirmationTest.onlyPositiveConfirmationCancels -->
<a id="test-cba350b50f06958d"></a>
### `CancellationConfirmationTest.onlyPositiveConfirmationCancels`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): hdrTesting=true impostato dal test, nessun HDR; Notification privata invocata via reflection, Intent e dialog shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CancellationConfirmationTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `startFakeHdr(…)`, `shadowOf(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Solo positivo imposta cancel e disabilita Stop.

**Limiti:** Conferma imposta AtomicBoolean ma non interrompe codec né verifica file rimossi; stale Intent provato con nessuna vera run.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void onlyPositiveConfirmationCancels()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try{
            startFakeHdr(activity);AtomicBoolean cancelled=(AtomicBoolean)activity.home.state.cancelled;assertFalse(cancelled.get());
            ((AlertDialog)activity.home.conversion.cancelConfirmation).getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();
            assertTrue(cancelled.get());assertFalse(((Button)activity.home.screen.conversion.stop).isEnabled());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: CancellationConfirmationTest.notificationOpensConfirmationActivityInsteadOfStoppingService -->
<a id="test-6fd37986f0f29d88"></a>
### `CancellationConfirmationTest.notificationOpensConfirmationActivityInsteadOfStoppingService`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): hdrTesting=true impostato dal test, nessun HDR; Notification privata invocata via reflection, Intent e dialog shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CancellationConfirmationTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.State`, `ConversionService.class.getDeclaredMethod`, `MainActivity.class.getName`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Notifica usa PendingIntent Activity di conferma con startedAt, non stop diretto.

**Limiti:** Conferma imposta AtomicBoolean ma non interrompe codec né verifica file rimossi; stale Intent provato con nessuna vera run.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void notificationOpensConfirmationActivityInsteadOfStoppingService()throws Exception{
        ServiceController<ConversionService> controller=Robolectric.buildService(ConversionService.class).create();
        try{
            ConversionService.State state=new ConversionService.State();state.running=true;state.startedAt=123;state.batchTotal=2;state.batchIndex=1;
            Method make=ConversionService.class.getDeclaredMethod("notification",ConversionService.State.class);make.setAccessible(true);
            Notification notification=(Notification)make.invoke(controller.get(),state);
            assertTrue(notification.actions[0].actionIntent.isActivity());
            Intent request=shadowOf(notification.actions[0].actionIntent).getSavedIntent();
            assertEquals(MainActivity.CONFIRM_CANCEL,request.getAction());assertEquals(MainActivity.class.getName(),request.getComponent().getClassName());
            assertEquals(123,request.getLongExtra(MainActivity.CANCEL_SESSION,-1));
        }finally{controller.destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: CancellationConfirmationTest.staleNotificationDoesNotShowCancelDialog -->
<a id="test-871793ae53a9cffe"></a>
### `CancellationConfirmationTest.staleNotificationDoesNotShowCancelDialog`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): hdrTesting=true impostato dal test, nessun HDR; Notification privata invocata via reflection, Intent e dialog shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CancellationConfirmationTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Richiesta cancellazione obsoleta non apre dialog e consuma action.

**Limiti:** Conferma imposta AtomicBoolean ma non interrompe codec né verifica file rimossi; stale Intent provato con nessuna vera run.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void staleNotificationDoesNotShowCancelDialog()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try{
            
            Intent request=new Intent(MainActivity.CONFIRM_CANCEL).putExtra(MainActivity.CANCEL_SESSION,999L);activity.home.conversion.handleCancellationIntent(request);
            assertNull(activity.home.conversion.cancelConfirmation);assertNull(request.getAction());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-conversioncontrolboundarytest"></a>
## ConversionControlBoundaryTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ConversionControlBoundaryTest.java) · 3 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** ConversionControl.checkpoint/finishFile/cancel/skip.

**Riferimenti produttivi:** [ConversionControl](../../app/src/main/java/it/edo/codecprobe/ConversionControl.java).

**Preparazione, file e simulazioni condivisi:** Clock costante e worker reale; latch/future/join con deadline 3 s.

**Confini comuni dell’audit:** Non verifica teardown di un servizio Android né cancellazione dentro il codec.

**Metodi:** [cancellationBeforeTheFirstFileNeverEntersPauseOrAcceptsSkip](#test-136f83b25c60a543) · [completedFileRejectsLateSkipWithoutCancellingTheNextFile](#test-b77f0138a88e50fa) · [interruptingAPausedWorkerCancelsAndRestoresItsInterruptStatus](#test-d4960a9c4c6e5b2a)

<!-- test: ConversionControlBoundaryTest.cancellationBeforeTheFirstFileNeverEntersPauseOrAcceptsSkip -->
<a id="test-136f83b25c60a543"></a>
### `ConversionControlBoundaryTest.cancellationBeforeTheFirstFileNeverEntersPauseOrAcceptsSkip`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Clock costante e worker reale; latch/future/join con deadline 3 s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ConversionControlBoundaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `gate.pause`, `gate.cancel`, `gate.checkpoint`, `gate.finishFile`, `gate.skip`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Annullamento prima del primo file non entra in pausa o accetta skip.

**Limiti:** Non verifica teardown di un servizio Android né cancellazione dentro il codec.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void cancellationBeforeTheFirstFileNeverEntersPauseOrAcceptsSkip() {
        AtomicBoolean cancelled=new AtomicBoolean();ConversionControl gate=new ConversionControl(cancelled,()->0L,p->fail("No worker was paused"));
        gate.pause(true);gate.cancel();assertTrue(gate.checkpoint());assertFalse(gate.finishFile());assertFalse(gate.skip(1));
    }
```

</details>

<!-- /test -->

<!-- test: ConversionControlBoundaryTest.completedFileRejectsLateSkipWithoutCancellingTheNextFile -->
<a id="test-b77f0138a88e50fa"></a>
### `ConversionControlBoundaryTest.completedFileRejectsLateSkipWithoutCancellingTheNextFile`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Clock costante e worker reale; latch/future/join con deadline 3 s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ConversionControlBoundaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `gate.skip`, `gate.beginFile`, `gate.finishFile`, `gate.endFile`, `gate.stopping`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Skip tardivo sul file già concluso non annulla il successivo.

**Limiti:** Non verifica teardown di un servizio Android né cancellazione dentro il codec.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void completedFileRejectsLateSkipWithoutCancellingTheNextFile() {
        ConversionControl gate=new ConversionControl(new AtomicBoolean(),()->0L,p->{});
        assertFalse(gate.skip(1));gate.beginFile(1);assertTrue(gate.finishFile());assertFalse(gate.skip(1));
        gate.endFile();gate.beginFile(2);assertFalse(gate.skip(1));assertFalse(gate.stopping());assertTrue(gate.finishFile());
    }
```

</details>

<!-- /test -->

<!-- test: ConversionControlBoundaryTest.interruptingAPausedWorkerCancelsAndRestoresItsInterruptStatus -->
<a id="test-d4960a9c4c6e5b2a"></a>
### `ConversionControlBoundaryTest.interruptingAPausedWorkerCancelsAndRestoresItsInterruptStatus`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Clock costante e worker reale; latch/future/join con deadline 3 s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ConversionControlBoundaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `gate.checkpoint`, `gate.beginFile`, `gate.pause`, `gate.cancel`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Interrupt del thread in pausa diventa arresto e conserva il flag interrupt; worker non rimane vivo.

**Limiti:** Non verifica teardown di un servizio Android né cancellazione dentro il codec.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void interruptingAPausedWorkerCancelsAndRestoresItsInterruptStatus()throws Exception {
        CountDownLatch paused=new CountDownLatch(1);AtomicBoolean cancellation=new AtomicBoolean();
        ConversionControl gate=new ConversionControl(cancellation,()->0L,p->{if(p)paused.countDown();});
        CompletableFuture<Boolean> outcome=new CompletableFuture<>();
        Thread worker=new Thread(()->{try{boolean stopped=gate.checkpoint();outcome.complete(stopped&&Thread.currentThread().isInterrupted());}catch(Throwable error){outcome.completeExceptionally(error);}},"test-paused-worker");
        gate.beginFile(1);gate.pause(true);
        try { worker.start();assertTrue(paused.await(3,TimeUnit.SECONDS));worker.interrupt();assertTrue(outcome.get(3,TimeUnit.SECONDS));assertTrue(cancellation.get()); }
        finally { gate.cancel();worker.interrupt();worker.join(3000);assertFalse("Worker leaked",worker.isAlive()); }
    }
```

</details>

<!-- /test -->


<a id="class-conversioncontroltest"></a>
## ConversionControlTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ConversionControlTest.java) · 4 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** ConversionControl.beginFile/checkpoint/pause/skip/endFile/finishFile/cancel/pausedMillis.

**Riferimenti produttivi:** [ConversionControl](../../app/src/main/java/it/edo/codecprobe/ConversionControl.java).

**Preparazione, file e simulazioni condivisi:** Thread/latch/future reali e clock AtomicLong iniettato; timeout 3 s.

**Confini comuni dell’audit:** Non ferma un decoder o watchdog reale: confronta il clock calcolato e l'API del gate. Nessun MediaCodec/JNI.

**Metodi:** [pauseWaitsAndExcludesTimeFromDecoderWatchdog](#test-b5fa19f24826c060) · [skippingWhilePausedKeepsNextFilePausedAndRejectsOldFileCommands](#test-6e511bcc243de8a2) · [cancellationWakesPausedWorkerAndCannotCommitAFile](#test-3f2905f30ca70920) · [resumeCommandDoesNotWaitForWorkerAcknowledgementLogging](#test-9699510ed61e614d)

<!-- test: ConversionControlTest.pauseWaitsAndExcludesTimeFromDecoderWatchdog -->
<a id="test-b5fa19f24826c060"></a>
### `ConversionControlTest.pauseWaitsAndExcludesTimeFromDecoderWatchdog`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Thread/latch/future reali e clock AtomicLong iniettato; timeout 3 s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ConversionControlTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `gate.beginFile`, `gate.pause`, `gate.pausedMillis`, `gate.cancel`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Pausa blocca il worker e 120 s simulati sono esclusi dal tempo utile, poi riprende.

**Limiti:** Non ferma un decoder o watchdog reale: confronta il clock calcolato e l'API del gate. Nessun MediaCodec/JNI.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void pauseWaitsAndExcludesTimeFromDecoderWatchdog() throws Exception {
        AtomicLong clock=new AtomicLong(1000);
        CountDownLatch entered=new CountDownLatch(1);
        List<Boolean> events=Collections.synchronizedList(new ArrayList<>());
        ConversionControl gate=new ConversionControl(new AtomicBoolean(),clock::get,p->{events.add(p);if(p)entered.countDown();});
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            gate.beginFile(1);gate.pause(true);
            Future<Boolean> result=worker.submit(gate::checkpoint);
            assertTrue(entered.await(3,TimeUnit.SECONDS));assertFalse(result.isDone());
            clock.set(121000);assertEquals(120000,gate.pausedMillis());
            assertEquals(1000,clock.get()-gate.pausedMillis());
            gate.pause(false);assertFalse(result.get(3,TimeUnit.SECONDS));
            assertEquals(List.of(true,false),events);assertEquals(120000,gate.pausedMillis());
            clock.set(122000);assertEquals(2000,clock.get()-gate.pausedMillis());
        }finally{gate.cancel();worker.shutdownNow();}
    }
```

</details>

<!-- /test -->

<!-- test: ConversionControlTest.skippingWhilePausedKeepsNextFilePausedAndRejectsOldFileCommands -->
<a id="test-6e511bcc243de8a2"></a>
### `ConversionControlTest.skippingWhilePausedKeepsNextFilePausedAndRejectsOldFileCommands`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Thread/latch/future reali e clock AtomicLong iniettato; timeout 3 s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ConversionControlTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `gate.beginFile`, `gate.pause`, `gate.skip`, `gate.pauseRequested`, `gate.endFile`, `gate.finishFile`, `gate.cancel`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Skip in pausa interrompe solo il file attuale, conserva pausa e rifiuta comando con numero vecchio.

**Limiti:** Non ferma un decoder o watchdog reale: confronta il clock calcolato e l'API del gate. Nessun MediaCodec/JNI.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void skippingWhilePausedKeepsNextFilePausedAndRejectsOldFileCommands() throws Exception {
        BlockingQueue<Boolean> events=new LinkedBlockingQueue<>();
        ConversionControl gate=new ConversionControl(new AtomicBoolean(),()->0L,events::add);
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            gate.beginFile(1);gate.pause(true);Future<Boolean> first=worker.submit(gate::checkpoint);
            assertEquals(Boolean.TRUE,events.poll(3,TimeUnit.SECONDS));assertTrue(gate.skip(1));
            assertTrue(first.get(3,TimeUnit.SECONDS));assertEquals(Boolean.FALSE,events.poll(3,TimeUnit.SECONDS));
            assertTrue(gate.pauseRequested());gate.endFile();gate.beginFile(2);
            Future<Boolean> next=worker.submit(gate::checkpoint);assertEquals(Boolean.TRUE,events.poll(3,TimeUnit.SECONDS));
            assertFalse(gate.skip(1));assertFalse(next.isDone());gate.pause(false);
            assertFalse(next.get(3,TimeUnit.SECONDS));assertEquals(Boolean.FALSE,events.poll(3,TimeUnit.SECONDS));
            assertTrue(gate.finishFile());assertFalse(gate.skip(2));
        }finally{gate.cancel();worker.shutdownNow();}
    }
```

</details>

<!-- /test -->

<!-- test: ConversionControlTest.cancellationWakesPausedWorkerAndCannotCommitAFile -->
<a id="test-3f2905f30ca70920"></a>
### `ConversionControlTest.cancellationWakesPausedWorkerAndCannotCommitAFile`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Thread/latch/future reali e clock AtomicLong iniettato; timeout 3 s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ConversionControlTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `gate.beginFile`, `gate.pause`, `gate.cancel`, `gate.skip`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Cancel sveglia il worker in pausa e impedisce finishFile.

**Limiti:** Non ferma un decoder o watchdog reale: confronta il clock calcolato e l'API del gate. Nessun MediaCodec/JNI.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void cancellationWakesPausedWorkerAndCannotCommitAFile() throws Exception {
        CountDownLatch entered=new CountDownLatch(1);AtomicBoolean cancel=new AtomicBoolean();
        ConversionControl gate=new ConversionControl(cancel,()->0L,p->{if(p)entered.countDown();});
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            gate.beginFile(1);gate.pause(true);Future<Boolean> done=worker.submit(gate::finishFile);
            assertTrue(entered.await(3,TimeUnit.SECONDS));gate.cancel();
            assertFalse(done.get(3,TimeUnit.SECONDS));assertTrue(cancel.get());assertFalse(gate.skip(1));
        }finally{gate.cancel();worker.shutdownNow();}
    }
```

</details>

<!-- /test -->

<!-- test: ConversionControlTest.resumeCommandDoesNotWaitForWorkerAcknowledgementLogging -->
<a id="test-9699510ed61e614d"></a>
### `ConversionControlTest.resumeCommandDoesNotWaitForWorkerAcknowledgementLogging`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Thread/latch/future reali e clock AtomicLong iniettato; timeout 3 s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ConversionControlTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `gate.beginFile`, `gate.pause`, `gate.cancel`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Resume torna anche se la callback di riconoscimento della pausa è deliberatamente bloccata.

**Limiti:** Non ferma un decoder o watchdog reale: confronta il clock calcolato e l'API del gate. Nessun MediaCodec/JNI.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void resumeCommandDoesNotWaitForWorkerAcknowledgementLogging() throws Exception {
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        ConversionControl gate=new ConversionControl(new AtomicBoolean(),()->0L,p->{
            if(p){entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}}
        });
        ExecutorService worker=Executors.newSingleThreadExecutor(),commands=Executors.newSingleThreadExecutor();
        try {
            gate.beginFile(1);gate.pause(true);Future<Boolean> done=worker.submit(gate::checkpoint);
            assertTrue(entered.await(3,TimeUnit.SECONDS));
            commands.submit(()->gate.pause(false)).get(3,TimeUnit.SECONDS);
            assertFalse(done.isDone());release.countDown();assertFalse(done.get(3,TimeUnit.SECONDS));
        }finally{release.countDown();gate.cancel();worker.shutdownNow();commands.shutdownNow();}
    }
```

</details>

<!-- /test -->


<a id="class-conversionrungatetest"></a>
## ConversionRunGateTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ConversionRunGateTest.java) · 1 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** ConversionRunGate.acquire/owns/release.

**Riferimenti produttivi:** [ConversionRunGate](../../app/src/main/java/it/edo/codecprobe/ConversionRunGate.java).

**Preparazione, file e simulazioni condivisi:** Otto worker reali, latch; deadline 3 s e token di sessione.

**Confini comuni dell’audit:** Non avvia servizi/codec o due processi Android. Sensibile al carico macchina per i timeout; una sola schedulazione concorrente.

**Metodi:** [concurrentStartsHaveOneOwnerAndOldOwnerCannotReleaseItsSuccessor](#test-dac2586da87c8b15)

<!-- test: ConversionRunGateTest.concurrentStartsHaveOneOwnerAndOldOwnerCannotReleaseItsSuccessor -->
<a id="test-dac2586da87c8b15"></a>
### `ConversionRunGateTest.concurrentStartsHaveOneOwnerAndOldOwnerCannotReleaseItsSuccessor`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Otto worker reali, latch; deadline 3 s e token di sessione.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ConversionRunGateTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `gate.acquire`, `gate.release`, `gate.owns`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Un solo proprietario su otto avvii, token crescente; un vecchio token non rilascia il successore.

**Limiti:** Non avvia servizi/codec o due processi Android. Sensibile al carico macchina per i timeout; una sola schedulazione concorrente.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void concurrentStartsHaveOneOwnerAndOldOwnerCannotReleaseItsSuccessor()throws Exception{
        ConversionRunGate gate=new ConversionRunGate();ExecutorService workers=Executors.newFixedThreadPool(8);
        CountDownLatch ready=new CountDownLatch(8),start=new CountDownLatch(1),attempted=new CountDownLatch(8),release=new CountDownLatch(1);
        AtomicInteger entered=new AtomicInteger();
        try{
            for(int n=0;n<8;n++)workers.submit(()->{
                ready.countDown();try{start.await();long token=gate.acquire();if(token!=0)entered.incrementAndGet();attempted.countDown();
                    if(token!=0){release.await();gate.release(token);}}
                catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
            });
            assertTrue(ready.await(3,TimeUnit.SECONDS));start.countDown();assertTrue(attempted.await(3,TimeUnit.SECONDS));assertEquals(1,entered.get());assertEquals(0,gate.acquire());
            release.countDown();workers.shutdown();assertTrue(workers.awaitTermination(3,TimeUnit.SECONDS));
            long first=gate.acquire();assertTrue(first>0);gate.release(first);long next=gate.acquire();assertTrue(next>first);
            gate.release(first);assertTrue(gate.owns(next));assertFalse(gate.owns(first));gate.release(next);assertTrue(gate.acquire()>next);
        }finally{start.countDown();release.countDown();workers.shutdownNow();}
    }
```

</details>

<!-- /test -->


<a id="class-homeplaybackcontrolstest"></a>
## HomePlaybackControlsTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/HomePlaybackControlsTest.java) · 1 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** HomePlayback/HomeConversion; ConversionService command Intents.

**Riferimenti produttivi:** [HomeConversion](../../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [ConversionService](../../app/src/main/java/it/edo/codecprobe/ConversionService.java).

**Preparazione, file e simulazioni condivisi:** State running/startedAt/batchIndex modificato via reflection; servizio non esegue i comandi.

**Confini comuni dell’audit:** Pause/resume/skip provati solo come Intent e conferma, non loro effetto su encoder o file.

**Metodi:** [pauseAndResumeSendSessionCommandsAndSkipRequiresCurrentFileConfirmation](#test-0a5682fd66ea8aeb)

<!-- test: HomePlaybackControlsTest.pauseAndResumeSendSessionCommandsAndSkipRequiresCurrentFileConfirmation -->
<a id="test-0a5682fd66ea8aeb"></a>
### `HomePlaybackControlsTest.pauseAndResumeSendSessionCommandsAndSkipRequiresCurrentFileConfirmation`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): State running/startedAt/batchIndex modificato via reflection; servizio non esegue i comandi.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomePlaybackControlsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.snapshot`, `ConversionService.class.getDeclaredField`, `home.conversion.renderConversionDashboard`, `home.screen.conversion.playbackActions.getVisibility`, `home.screen.conversion.pause.performClick`, `home.activity.getApplication`, `home.screen.conversion.pause.getText`, `home.screen.conversion.skip.performClick`, `home.conversion.playback.confirmation.getButton`, `ConversionService.State.class.getDeclaredFields`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Pause/Resume portano sessione1337; Skip richiede sì sul file corrente e conferma diventata obsoleta non emette Intent.

**Limiti:** Pause/resume/skip provati solo come Intent e conferma, non loro effetto su encoder o file.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void pauseAndResumeSendSessionCommandsAndSkipRequiresCurrentFileConfirmation()throws Exception{
        ConversionService.State before=ConversionService.snapshot();
        Field field=ConversionService.class.getDeclaredField("state");field.setAccessible(true);ConversionService.State actual=(ConversionService.State)field.get(null);
        actual.hasSession=false;actual.running=true;actual.startedAt=1337;actual.batchIndex=2;actual.batchTotal=3;actual.source="clip.mp4";actual.fileActive=true;
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;home.conversion.renderConversionDashboard(ConversionService.snapshot());
            assertEquals(View.VISIBLE,home.screen.conversion.playbackActions.getVisibility());home.screen.conversion.pause.performClick();
            Intent pause=Shadows.shadowOf(home.activity.getApplication()).getNextStartedService();assertEquals(ConversionService.PAUSE,pause.getAction());assertEquals(1337,pause.getLongExtra(MainActivity.CANCEL_SESSION,-1));
            actual.pauseRequested=true;actual.paused=true;home.conversion.renderConversionDashboard(ConversionService.snapshot());
            assertEquals("Riprendi",home.screen.conversion.pause.getText().toString());home.screen.conversion.pause.performClick();
            assertEquals(ConversionService.RESUME,Shadows.shadowOf(home.activity.getApplication()).getNextStartedService().getAction());
            home.screen.conversion.skip.performClick();assertNotNull(home.conversion.playback.confirmation);
            home.conversion.playback.confirmation.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNull(Shadows.shadowOf(home.activity.getApplication()).getNextStartedService());
            home.screen.conversion.skip.performClick();home.conversion.playback.confirmation.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            Intent skip=Shadows.shadowOf(home.activity.getApplication()).getNextStartedService();assertEquals(ConversionService.SKIP,skip.getAction());assertEquals(2,skip.getIntExtra("fileNumber",-1));
            home.screen.conversion.skip.performClick();actual.batchIndex=3;
            home.conversion.playback.confirmation.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertNull(Shadows.shadowOf(home.activity.getApplication()).getNextStartedService());
            actual.running=false;home.conversion.renderConversionDashboard(ConversionService.snapshot());assertEquals(View.GONE,home.screen.conversion.playbackActions.getVisibility());
        }finally{
            controller.pause().stop().destroy();
            for(Field value:ConversionService.State.class.getDeclaredFields()){value.setAccessible(true);value.set(actual,value.get(before));}
        }
    }
```

</details>

<!-- /test -->


<a id="class-homesavedsessiontest"></a>
## HomeSavedSessionTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/HomeSavedSessionTest.java) · 2 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** HomeConversion report/SavedSession UI; SavedSession.load/save; BatchRetry.select.

**Riferimenti produttivi:** [HomeConversion](../../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [SavedSession](../../app/src/main/java/it/edo/codecprobe/SavedSession.java), [BatchRetry](../../app/src/main/java/it/edo/codecprobe/BatchRetry.java).

**Preparazione, file e simulazioni condivisi:** Due risultati e URI inesistenti preparati; vero journal generato dal sample; ShadowActivity/Bundle.

**Confini comuni dell’audit:** Apri verifica Intent non player o esistenza file; recovery dialog non avvia alcun ripristino effettivo.

**Metodi:** [eachSuccessfulReportButtonOpensExactlyItsOwnFile](#test-3cbb1d9f4685191c) · [freshActivityShowsRecoveryWithoutStartingAndNewQueueRequiresReplacementConfirmation](#test-066277d1add2b289)

<!-- test: HomeSavedSessionTest.eachSuccessfulReportButtonOpensExactlyItsOwnFile -->
<a id="test-3cbb1d9f4685191c"></a>
### `HomeSavedSessionTest.eachSuccessfulReportButtonOpensExactlyItsOwnFile`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Due risultati e URI inesistenti preparati; vero journal generato dal sample; ShadowActivity/Bundle.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeSavedSessionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.State`, `state.items.add`, `CompressionSummary.Item`, `state.outputUris.add`, `state.results.add`, `state.outputUris.get`, `CompressionSummary.render`, `home.conversion.renderSizeReport`, `home.screen.conversion.failureReport.getChildAt`, `home.screen.conversion.failureReport.getChildCount`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Ogni riga successo apre il proprio URI con read grant, report vuoto pulisce righe.

**Limiti:** Apri verifica Intent non player o esistenza file; recovery dialog non avvia alcun ripristino effettivo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void eachSuccessfulReportButtonOpensExactlyItsOwnFile(){
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;ConversionService.State state=new ConversionService.State();
            for(int n=0;n<2;n++){
                state.items.add(new CompressionSummary.Item(n+1,"clip"+n+".mp4",CompressionSummary.Status.COMPLETE,1000,500));
                state.outputUris.add("content://results/"+n);state.results.add(Uri.parse(state.outputUris.get(n)));
            }
            state.sizeReport=CompressionSummary.render(state.items);home.conversion.renderSizeReport(state);
            for(int n=0;n<2;n++){
                LinearLayout row=(LinearLayout)home.screen.conversion.failureReport.getChildAt(n);
                Button button=(Button)row.getChildAt(1);assertEquals("Apri",button.getText().toString());assertTrue(button.getContentDescription().toString().contains("clip"+n));
                button.performClick();Intent opened=Shadows.shadowOf(home.activity).getNextStartedActivity();
                assertEquals(Intent.ACTION_VIEW,opened.getAction());assertEquals(Uri.parse("content://results/"+n),opened.getData());assertTrue((opened.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
            }
            home.conversion.renderSizeReport(new ConversionService.State());assertEquals(0,home.screen.conversion.failureReport.getChildCount());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: HomeSavedSessionTest.freshActivityShowsRecoveryWithoutStartingAndNewQueueRequiresReplacementConfirmation -->
<a id="test-066277d1add2b289"></a>
### `HomeSavedSessionTest.freshActivityShowsRecoveryWithoutStartingAndNewQueueRequiresReplacementConfirmation`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Due risultati e URI inesistenti preparati; vero journal generato dal sample; ShadowActivity/Bundle.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeSavedSessionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.snapshot`, `ConversionService.class.getDeclaredField`, `SavedSession.save`, `ConversionService.State`, `home.screen.conversion.savedSession.getVisibility`, `home.screen.conversion.savedSession.getText`, `home.screen.conversion.sizeReport.getText`, `home.screen.conversion.savedSession.performClick`, `home.conversion.savedSession.dialog.isShowing`, `home.conversion.savedSession.dialog.getButton`, `home.conversion.outputCheck.begin`, `SavedSession.load`, `BatchRetry.select`, `SavedSession.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Activity fresca propone recovery senza partire; nuova coda richiede sostituzione, No conserva journal; retry rimuove extra restore.

**Limiti:** Apri verifica Intent non player o esistenza file; recovery dialog non avvia alcun ripristino effettivo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void freshActivityShowsRecoveryWithoutStartingAndNewQueueRequiresReplacementConfirmation()throws Exception{
        var context=RuntimeEnvironment.getApplication();Intent request=BatchRetryTest.queue(context);
        ConversionService.State before=ConversionService.snapshot();Field field=ConversionService.class.getDeclaredField("state");field.setAccessible(true);
        SavedSession.save(context,request,SavedSessionTest.sample(request));field.set(null,new ConversionService.State());
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;
            assertFalse(ConversionService.snapshot().running);assertTrue(ConversionService.snapshot().recoverable);
            assertEquals(View.VISIBLE,home.screen.conversion.savedSession.getVisibility());assertEquals("Riprendi sessione salvata",home.screen.conversion.savedSession.getText().toString());
            assertTrue(home.screen.conversion.sizeReport.getText().toString().contains("0,50 kB"));
            home.screen.conversion.savedSession.performClick();assertTrue(home.conversion.savedSession.dialog.isShowing());
            home.conversion.savedSession.dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertFalse(ConversionService.snapshot().running);
            home.conversion.outputCheck.begin(BatchRetryTest.queue(context));assertFalse(home.conversion.outputCheck.checking);
            assertTrue(home.conversion.savedSession.dialog.isShowing());home.conversion.savedSession.dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            assertEquals("saved-test-session",SavedSession.load(context).state.sessionId);
            Intent retry=BatchRetry.select(SavedSession.load(context).resumeRequest(),List.of(2));assertFalse(retry.hasExtra(SavedSession.RESTORE));assertFalse(retry.hasExtra("finishedFiles"));
        }finally{controller.pause().stop().destroy();field.set(null,before);SavedSession.clear(context);}
    }
```

</details>

<!-- /test -->


<a id="class-imagenotificationtest"></a>
## ImageNotificationTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageNotificationTest.java) · 1 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** ImageConversionService.stage/notification/onStartCommand.

**Riferimenti produttivi:** [ImageConversionService](../../app/src/main/java/it/edo/codecprobe/ImageConversionService.java).

**Preparazione, file e simulazioni condivisi:** State running iniettato e metodo privato stage via reflection; ShadowLooper/SystemClock/NotificationManager.

**Confini comuni dell’audit:** Tempo virtuale, non schedulazione Android reale né consumo batteria; verifica stessa istanza Notification a1000ms e id30.

**Metodi:** [rapidStagesAreCoalescedButPauseCommandPublishesImmediately](#test-0b6a883a9435c126)

<!-- test: ImageNotificationTest.rapidStagesAreCoalescedButPauseCommandPublishesImmediately -->
<a id="test-0b6a883a9435c126"></a>
### `ImageNotificationTest.rapidStagesAreCoalescedButPauseCommandPublishesImmediately`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): State running iniettato e metodo privato stage via reflection; ShadowLooper/SystemClock/NotificationManager.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageNotificationTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageConversionService.class.getDeclaredField`, `ImageConversionService.class.getDeclaredMethod`, `service.getSystemService`, `service.onStartCommand`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** 500 stage coalesciati nel testo stage499; successivi attendono1000ms virtuali, PAUSE notifica immediatamente.

**Limiti:** Tempo virtuale, non schedulazione Android reale né consumo batteria; verifica stessa istanza Notification a1000ms e id30.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void rapidStagesAreCoalescedButPauseCommandPublishesImmediately()throws Exception{
        var field=ImageConversionService.class.getDeclaredField("state");field.setAccessible(true);Object before=field.get(null);
        ImageRunState run=new ImageRunState();run.running=true;run.startedAt=42;run.total=100;field.set(null,run);
        var controller=Robolectric.buildService(ImageConversionService.class).create();
        try{
            var service=controller.get();var stage=ImageConversionService.class.getDeclaredMethod("stage",String.class);stage.setAccessible(true);
            var looper=Shadows.shadowOf(Looper.getMainLooper());var notifications=Shadows.shadowOf(service.getSystemService(NotificationManager.class));
            for(int i=0;i<500;i++)stage.invoke(service,"stage-"+i);looper.idle();looper.idleFor(Duration.ofMillis(Math.max(0,1000-SystemClock.elapsedRealtime())));
            Notification first=notifications.getNotification(30);assertNotNull(first);assertEquals("stage-499",first.extras.getCharSequence(Notification.EXTRA_TEXT).toString());
            for(int i=0;i<500;i++)stage.invoke(service,"next-"+i);looper.idle();assertSame(first,notifications.getNotification(30));
            looper.idleFor(Duration.ofMillis(999));assertSame(first,notifications.getNotification(30));looper.idleFor(Duration.ofMillis(1));assertNotSame(first,notifications.getNotification(30));
            Notification next=notifications.getNotification(30);service.onStartCommand(new Intent(service,ImageConversionService.class).setAction(ImageConversionService.PAUSE).putExtra("startedAt",42L),0,1);looper.idle();assertNotSame(next,notifications.getNotification(30));assertTrue(notifications.getNotification(30).extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("Pausa richiesta"));
        }finally{controller.destroy();field.set(null,before);}
    }
```

</details>

<!-- /test -->


<a id="class-imagepausetest"></a>
## ImagePauseTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImagePauseTest.java) · 5 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** ImageConversionService ciclo/PAUSE/RESUME/CANCEL/onDestroy; ImageQueueStore; ImageLogStore; ImageController.

**Riferimenti produttivi:** [ImageConversionService](../../app/src/main/java/it/edo/codecprobe/ImageConversionService.java), [ImageQueueStore](../../app/src/main/java/it/edo/codecprobe/ImageQueueStore.java), [ImageLogStore](../../app/src/main/java/it/edo/codecprobe/ImageLogStore.java), [ImageController](../../app/src/main/java/it/edo/codecprobe/ImageController.java).

**Preparazione, file e simulazioni condivisi:** Provider BlockingPhotos blocca query con latch poi openFile sempre fallisce; servizio worker reale nel Robolectric; queue/log su disco.

**Confini comuni dell’audit:** Pausa verificata tra tentativi falliti, mai durante/completando una vera conversione JPEG→HEIC. WakeLock shadow, polling5ms/deadline10s.

**Metodi:** [pauseFinishesTheCurrentPhotoAndResumeKeepsQueueAndCounters](#test-5106ecbbfddb8b4a) · [cancellingWhilePausedWakesTheWorkerAndDoesNotStartAnotherPhoto](#test-1b68bae91201fabc) · [resumeCanWithdrawAPauseBeforeTheCurrentPhotoFinishes](#test-d42c50077f6f1de1) · [destroyingAPausedServiceDoesNotLeaveItsWorkerWaiting](#test-8cf9f21deeb523ff) · [screenAndNotificationCommandsAreBoundToTheActiveQueue](#test-3c686667954a99d2)

<!-- test: ImagePauseTest.pauseFinishesTheCurrentPhotoAndResumeKeepsQueueAndCounters -->
<a id="test-5106ecbbfddb8b4a"></a>
### `ImagePauseTest.pauseFinishesTheCurrentPhotoAndResumeKeepsQueueAndCounters`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider BlockingPhotos blocca query con latch poi openFile sempre fallisce; servizio worker reale nel Robolectric; queue/log su disco.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImagePauseTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageConversionService.snapshot`, `ImageQueueStore.file`, `ImageConversionService.class.getDeclaredMethod`, `log.contains`, `ImageLogStore.snapshot`, `ImageQueueStore.discard`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Pausa termina il primo tentativo fallito e non passa al secondo, conserva queue/counters; Resume vecchio rifiutato, corrente completa3 falliti e pulisce.

**Limiti:** Pausa verificata tra tentativi falliti, mai durante/completando una vera conversione JPEG→HEIC. WakeLock shadow, polling5ms/deadline10s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void pauseFinishesTheCurrentPhotoAndResumeKeepsQueueAndCounters()throws Exception{
        BlockingPhotos photos=new BlockingPhotos();ShadowContentResolver.registerProviderInternal("pausephotos",photos);
        var controller=Robolectric.buildService(ImageConversionService.class).create();String id=queue();
        try{
            start(controller.get(),id);assertTrue(photos.entered.await(5,TimeUnit.SECONDS));long run=ImageConversionService.snapshot().startedAt;
            command(controller.get(),ImageConversionService.PAUSE,run);assertTrue(ImageConversionService.snapshot().pauseRequested);assertFalse(ImageConversionService.snapshot().paused);
            command(controller.get(),ImageConversionService.RESUME,run-1);assertTrue("Old notification must not resume a new run",ImageConversionService.snapshot().pauseRequested);
            photos.release.countDown();await(()->ImageConversionService.snapshot().paused);
            ImageRunState paused=ImageConversionService.snapshot();assertTrue(paused.running);assertEquals(1,paused.completed);assertEquals(1,paused.failed);assertEquals(1,photos.queries);assertTrue(ImageQueueStore.file(controller.get(),id).isFile());assertNull(field(controller.get(),"wake"));
            var notification=ImageConversionService.class.getDeclaredMethod("notification");notification.setAccessible(true);Notification notice=(Notification)notification.invoke(controller.get());assertEquals("Riprendi",notice.actions[0].title.toString());
            await(()->ImageConversionService.snapshot().log.contains("PAUSA IMMAGINI"));File copy=ImageLogStore.snapshot(controller.get(),ImageConversionService.LOG);try{assertTrue(read(copy).contains("PAUSA IMMAGINI"));}finally{copy.delete();}
            command(controller.get(),ImageConversionService.RESUME,run);await(()->!ImageConversionService.snapshot().running);
            ImageRunState ended=ImageConversionService.snapshot();assertEquals(3,ended.completed);assertEquals(3,ended.failed);assertEquals(3,photos.queries);assertFalse(ended.paused);assertFalse(ended.pauseRequested);assertFalse(ImageQueueStore.file(controller.get(),id).exists());assertNull(field(controller.get(),"wake"));
            String log=read(new File(controller.get().getFilesDir(),ImageConversionService.LOG));assertTrue(log.contains("FINE PAUSA IMMAGINI"));assertTrue(log.contains("ripresa della stessa coda"));
        }finally{photos.release.countDown();command(controller.get(),ImageConversionService.CANCEL,ImageConversionService.snapshot().startedAt);await(()->!ImageConversionService.snapshot().running);controller.destroy();ImageQueueStore.discard(RuntimeEnvironment.getApplication(),id);}
    }
```

</details>

<!-- /test -->

<!-- test: ImagePauseTest.cancellingWhilePausedWakesTheWorkerAndDoesNotStartAnotherPhoto -->
<a id="test-1b68bae91201fabc"></a>
### `ImagePauseTest.cancellingWhilePausedWakesTheWorkerAndDoesNotStartAnotherPhoto`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider BlockingPhotos blocca query con latch poi openFile sempre fallisce; servizio worker reale nel Robolectric; queue/log su disco.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImagePauseTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageConversionService.snapshot`, `ImageQueueStore.file`, `ImageQueueStore.discard`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Cancel in pausa sveglia worker senza iniziare altra foto e rimuove queue/wake.

**Limiti:** Pausa verificata tra tentativi falliti, mai durante/completando una vera conversione JPEG→HEIC. WakeLock shadow, polling5ms/deadline10s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void cancellingWhilePausedWakesTheWorkerAndDoesNotStartAnotherPhoto()throws Exception{
        BlockingPhotos photos=new BlockingPhotos();ShadowContentResolver.registerProviderInternal("pausephotos",photos);
        var controller=Robolectric.buildService(ImageConversionService.class).create();String id=queue();
        try{
            start(controller.get(),id);assertTrue(photos.entered.await(5,TimeUnit.SECONDS));long run=ImageConversionService.snapshot().startedAt;
            command(controller.get(),ImageConversionService.PAUSE,run);photos.release.countDown();await(()->ImageConversionService.snapshot().paused);
            command(controller.get(),ImageConversionService.CANCEL,run);await(()->!ImageConversionService.snapshot().running);
            ImageRunState ended=ImageConversionService.snapshot();assertEquals(1,ended.completed);assertEquals(1,photos.queries);assertFalse(ended.paused);assertFalse(ended.pauseRequested);assertFalse(ImageQueueStore.file(controller.get(),id).exists());assertNull(field(controller.get(),"wake"));
        }finally{photos.release.countDown();command(controller.get(),ImageConversionService.CANCEL,ImageConversionService.snapshot().startedAt);await(()->!ImageConversionService.snapshot().running);controller.destroy();ImageQueueStore.discard(RuntimeEnvironment.getApplication(),id);}
    }
```

</details>

<!-- /test -->

<!-- test: ImagePauseTest.resumeCanWithdrawAPauseBeforeTheCurrentPhotoFinishes -->
<a id="test-d42c50077f6f1de1"></a>
### `ImagePauseTest.resumeCanWithdrawAPauseBeforeTheCurrentPhotoFinishes`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider BlockingPhotos blocca query con latch poi openFile sempre fallisce; servizio worker reale nel Robolectric; queue/log su disco.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImagePauseTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageConversionService.snapshot`, `ImageQueueStore.discard`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Resume anticipato ritira richiesta pausa prima del termine primo tentativo:3 falliti senza evento PAUSA.

**Limiti:** Pausa verificata tra tentativi falliti, mai durante/completando una vera conversione JPEG→HEIC. WakeLock shadow, polling5ms/deadline10s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void resumeCanWithdrawAPauseBeforeTheCurrentPhotoFinishes()throws Exception{
        BlockingPhotos photos=new BlockingPhotos();ShadowContentResolver.registerProviderInternal("pausephotos",photos);
        var controller=Robolectric.buildService(ImageConversionService.class).create();String id=queue();
        try{
            start(controller.get(),id);assertTrue(photos.entered.await(5,TimeUnit.SECONDS));long run=ImageConversionService.snapshot().startedAt;
            command(controller.get(),ImageConversionService.PAUSE,run);command(controller.get(),ImageConversionService.RESUME,run);photos.release.countDown();await(()->!ImageConversionService.snapshot().running);
            assertEquals(3,ImageConversionService.snapshot().completed);assertFalse(read(new File(controller.get().getFilesDir(),ImageConversionService.LOG)).contains("PAUSA IMMAGINI"));
        }finally{photos.release.countDown();command(controller.get(),ImageConversionService.CANCEL,ImageConversionService.snapshot().startedAt);await(()->!ImageConversionService.snapshot().running);controller.destroy();ImageQueueStore.discard(RuntimeEnvironment.getApplication(),id);}
    }
```

</details>

<!-- /test -->

<!-- test: ImagePauseTest.destroyingAPausedServiceDoesNotLeaveItsWorkerWaiting -->
<a id="test-8cf9f21deeb523ff"></a>
### `ImagePauseTest.destroyingAPausedServiceDoesNotLeaveItsWorkerWaiting`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider BlockingPhotos blocca query con latch poi openFile sempre fallisce; servizio worker reale nel Robolectric; queue/log su disco.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImagePauseTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageConversionService.snapshot`, `ImageQueueStore.discard`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** onDestroy in pausa sblocca worker e lascia un solo tentativo completato.

**Limiti:** Pausa verificata tra tentativi falliti, mai durante/completando una vera conversione JPEG→HEIC. WakeLock shadow, polling5ms/deadline10s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void destroyingAPausedServiceDoesNotLeaveItsWorkerWaiting()throws Exception{
        BlockingPhotos photos=new BlockingPhotos();ShadowContentResolver.registerProviderInternal("pausephotos",photos);
        var controller=Robolectric.buildService(ImageConversionService.class).create();String id=queue();boolean destroyed=false;
        try{
            start(controller.get(),id);assertTrue(photos.entered.await(5,TimeUnit.SECONDS));command(controller.get(),ImageConversionService.PAUSE,ImageConversionService.snapshot().startedAt);photos.release.countDown();await(()->ImageConversionService.snapshot().paused);
            controller.destroy();destroyed=true;await(()->!ImageConversionService.snapshot().running);assertEquals(1,ImageConversionService.snapshot().completed);assertNull(field(controller.get(),"wake"));
        }finally{photos.release.countDown();if(!destroyed){command(controller.get(),ImageConversionService.CANCEL,ImageConversionService.snapshot().startedAt);await(()->!ImageConversionService.snapshot().running);controller.destroy();}ImageQueueStore.discard(RuntimeEnvironment.getApplication(),id);}
    }
```

</details>

<!-- /test -->

<!-- test: ImagePauseTest.screenAndNotificationCommandsAreBoundToTheActiveQueue -->
<a id="test-3c686667954a99d2"></a>
### `ImagePauseTest.screenAndNotificationCommandsAreBoundToTheActiveQueue`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider BlockingPhotos blocca query con latch poi openFile sempre fallisce; servizio worker reale nel Robolectric; queue/log su disco.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImagePauseTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageLogStore.reset`, `ImageLogStore.append`, `images.screen.sections.focusCard`, `images.screen.pause.isEnabled`, `ImageOptions.load`, `images.render`, `images.screen.pause.getText`, `images.screen.pause.performClick`, `images.screen.runStatus.getText`, `images.screen.progress.isIndeterminate`, `images.screen.exportLog.isEnabled`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** UI/notifica sessione12345: Intent Pause/Resume, restore etichette e comandi bloccati in cancelling; nessun comando eseguito nel test UI.

**Limiti:** Pausa verificata tra tentativi falliti, mai durante/completando una vera conversione JPEG→HEIC. WakeLock shadow, polling5ms/deadline10s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void screenAndNotificationCommandsAreBoundToTheActiveQueue()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageLogStore.reset(context);ImageLogStore.append(context,ImageConversionService.LOG,"EVENTO\n\n");
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            ImageController images=controller.get().images;images.screen.sections.focusCard(ImageScreen.HEIC);assertFalse(images.screen.pause.isEnabled());
            ImageRunState run=new ImageRunState();run.running=true;run.total=10;run.startedAt=12345;run.options=ImageOptions.load(context);stateField.set(null,run);images.render();
            assertTrue(images.screen.pause.isEnabled());assertEquals("Pausa",images.screen.pause.getText().toString());images.screen.pause.performClick();Intent pause=Shadows.shadowOf(controller.get()).getNextStartedService();assertEquals(ImageConversionService.PAUSE,pause.getAction());assertEquals(12345,pause.getLongExtra("startedAt",-1));
            run.pauseRequested=true;images.render();assertEquals("Riprendi",images.screen.pause.getText().toString());assertTrue(images.screen.runStatus.getText().toString().contains("Pausa richiesta"));
            run.paused=true;images.render();assertTrue(images.screen.runStatus.getText().toString().contains("in pausa"));assertFalse(images.screen.progress.isIndeterminate());assertTrue(images.screen.exportLog.isEnabled());assertEquals(0,controller.get().getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            controller.recreate();images=controller.get().images;assertEquals("Riprendi",images.screen.pause.getText().toString());images.screen.pause.performClick();Intent resume=Shadows.shadowOf(controller.get()).getNextStartedService();assertEquals(ImageConversionService.RESUME,resume.getAction());assertEquals(12345,resume.getLongExtra("startedAt",-1));
            run.cancelling=true;images.render();assertFalse(images.screen.pause.isEnabled());assertTrue(images.screen.runStatus.getText().toString().contains("Annullamento"));
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-imagesessionservicetest"></a>
## ImageSessionServiceTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageSessionServiceTest.java) · 2 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** ImageConversionService.onStartCommand ripristino; ImageSessionStore; ImageProgressJournal; ImageLogStore.

**Riferimenti produttivi:** [ImageConversionService](../../app/src/main/java/it/edo/codecprobe/ImageConversionService.java), [ImageSessionStore](../../app/src/main/java/it/edo/codecprobe/ImageSessionStore.java), [ImageProgressJournal](../../app/src/main/java/it/edo/codecprobe/ImageProgressJournal.java), [ImageLogStore](../../app/src/main/java/it/edo/codecprobe/ImageLogStore.java).

**Preparazione, file e simulazioni condivisi:** Servizio Robolectric e stati via reflection; due veri file3/2byte, primo risultato già completato e secondo preregistrato con hash reali.

**Confini comuni dell’audit:** Ripresa segue percorso ALREADY_DONE, non converte file rimasto. Nessuna ricreazione processo o crash reale; executor get5s.

**Metodi:** [freshServiceResumesAfterCursorPreservesTotalsAndAppendsLog](#test-59c66b285caec79c) · [fatalLogFailureKeepsQueueAndPreviousCursorForRetry](#test-13bf6103eb75c877)

<!-- test: ImageSessionServiceTest.freshServiceResumesAfterCursorPreservesTotalsAndAppendsLog -->
<a id="test-59c66b285caec79c"></a>
### `ImageSessionServiceTest.freshServiceResumesAfterCursorPreservesTotalsAndAppendsLog`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Servizio Robolectric e stati via reflection; due veri file3/2byte, primo risultato già completato e secondo preregistrato con hash reali.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageSessionServiceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageLogStore.append`, `ImageSessionStore.load`, `ImageConversionService.snapshot`, `ImageSessionStore.exists`, `ImageQueueStore.file`, `log.startsWith`, `log.contains`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Servizio fresco parte dopo cursore1, riusa secondo output e preserva risparmio201/log precedente; non riapre primo URI e pulisce sessione/coda.

**Limiti:** Ripresa segue percorso ALREADY_DONE, non converte file rimasto. Nessuna ricreazione processo o crash reale; executor get5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void freshServiceResumesAfterCursorPreservesTotalsAndAppendsLog()throws Exception{
        prepare();ImageLogStore.append(context,ImageConversionService.LOG,"previous-details\n");install(new ImageRunState());
        var controller=Robolectric.buildService(ImageConversionService.class).create();try{
            controller.get().onStartCommand(ImageSessionStore.load(context).request(context),0,1);drain(controller.get());
            ImageRunState run=ImageConversionService.snapshot();assertFalse(run.running);assertEquals(2,run.completed);assertEquals(1,run.saved);assertEquals(1,run.alreadyDone);assertEquals(201,run.inputSavedBytes-run.outputSavedBytes);assertFalse(ImageSessionStore.exists(context));assertFalse(ImageQueueStore.file(context,id).exists());
            String log=new String(Files.readAllBytes(new File(context.getFilesDir(),ImageConversionService.LOG).toPath()),java.nio.charset.StandardCharsets.UTF_8);assertTrue(log.startsWith("previous-details"));assertTrue(log.contains("RIPRESA CODA"));assertFalse(log.contains("must-not-be-reopened"));
        }finally{controller.destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageSessionServiceTest.fatalLogFailureKeepsQueueAndPreviousCursorForRetry -->
<a id="test-13bf6103eb75c877"></a>
### `ImageSessionServiceTest.fatalLogFailureKeepsQueueAndPreviousCursorForRetry`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Servizio Robolectric e stati via reflection; due veri file3/2byte, primo risultato già completato e secondo preregistrato con hash reali.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageSessionServiceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `log.delete`, `log.mkdir`, `ImageSessionStore.load`, `ImageConversionService.snapshot`, `ImageQueueStore.file`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Log sostituito da directory rende avvio fatale: conserva queue e cursore1 per retry.

**Limiti:** Ripresa segue percorso ALREADY_DONE, non converte file rimasto. Nessuna ricreazione processo o crash reale; executor get5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void fatalLogFailureKeepsQueueAndPreviousCursorForRetry()throws Exception{
        prepare();File log=new File(context.getFilesDir(),ImageConversionService.LOG);log.delete();assertTrue(log.mkdir());
        var controller=Robolectric.buildService(ImageConversionService.class).create();try{
            controller.get().onStartCommand(ImageSessionStore.load(context).request(context),0,1);drain(controller.get());assertFalse(ImageConversionService.snapshot().running);assertTrue(ImageConversionService.snapshot().message.contains("Coda conservata"));assertTrue(ImageQueueStore.file(context,id).exists());assertEquals(1,ImageSessionStore.load(context).run.completed);
        }finally{controller.destroy();log.delete();}
    }
```

</details>

<!-- /test -->


<a id="class-imagesessionstoretest"></a>
## ImageSessionStoreTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageSessionStoreTest.java) · 3 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** ImageSessionStore.save/load/exists/discard; ImageQueueStore; AtomicFile.

**Riferimenti produttivi:** [ImageSessionStore](../../app/src/main/java/it/edo/codecprobe/ImageSessionStore.java), [ImageQueueStore](../../app/src/main/java/it/edo/codecprobe/ImageQueueStore.java).

**Preparazione, file e simulazioni condivisi:** Vero JSON/checkpoint e file queue125 nel Context host; risultati manuali con dettagli20k; scrittura AtomicFile interrotta chiudendo stream senza finishWrite.

**Confini comuni dell’audit:** Nessun processo ucciso o filesystem Android. Load non verifica media. Budget2kB riguarda checkpoint privo dettagli, non memoria runtime.

**Metodi:** [freshLoadKeepsQueueOptionsCursorAndCumulativeTotalsWithoutGrowingWithDetails](#test-7b8ce77e329ca9d0) · [interruptedAtomicWriteKeepsPreviousCheckpointAndExplicitDiscardRemovesQueue](#test-96c1b3fcbb1056a6) · [missingQueueIsReportedWithoutSilentlyDroppingTheSession](#test-0a4bfcf3a4fd813d)

<!-- test: ImageSessionStoreTest.freshLoadKeepsQueueOptionsCursorAndCumulativeTotalsWithoutGrowingWithDetails -->
<a id="test-7b8ce77e329ca9d0"></a>
### `ImageSessionStoreTest.freshLoadKeepsQueueOptionsCursorAndCumulativeTotalsWithoutGrowingWithDetails`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Vero JSON/checkpoint e file queue125 nel Context host; risultati manuali con dettagli20k; scrittura AtomicFile interrotta chiudendo stream senza finishWrite.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageSessionStoreTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `run.add`, `ImageRunState.Result`, `ImageSessionStore.save`, `ImageSessionStore.load`, `ImageQueueStore.read`, `ImageOptions.from`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Reload conserva cursore121, totali/risparmio/elapsed/opzioni e125 riferimenti; risultati dettagliati non persistiti, file<2048byte.

**Limiti:** Nessun processo ucciso o filesystem Android. Load non verifica media. Budget2kB riguarda checkpoint privo dettagli, non memoria runtime.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void freshLoadKeepsQueueOptionsCursorAndCumulativeTotalsWithoutGrowingWithDetails()throws Exception{
        ImageRunState run=sample();for(int i=0;i<120;i++)run.add(new ImageRunState.Result("photo"+i,3000,1000,"content://out/"+i,"OK",ImageRunState.Outcome.SAVED,"x".repeat(20000)));
        run.add(new ImageRunState.Result("failed",0,0,null,"err",ImageRunState.Outcome.FAILED,""));ImageSessionStore.save(context,id,run);
        ImageSessionStore.Session restored=ImageSessionStore.load(context);assertEquals(id,restored.queueId);assertEquals(121,restored.run.completed);assertEquals(120,restored.run.saved);assertEquals(1,restored.run.failed);assertEquals(240000,restored.run.inputSavedBytes-restored.run.outputSavedBytes);assertEquals(1234,restored.run.elapsedBefore);
        assertEquals(125,ImageQueueStore.read(context,id).inputs.size());assertTrue(restored.run.results.isEmpty());assertTrue(new File(context.getFilesDir(),"image-session.json").length()<2048);
        Intent request=restored.request(context);ImageOptions options=ImageOptions.from(request);assertEquals(50,options.quality);assertEquals("medium",options.preset);assertEquals("ssim",options.tune);assertFalse(options.onlySmaller);assertTrue(options.keepScreenOn);assertTrue(request.getBooleanExtra("restore",false));assertEquals(id,request.getStringExtra("queueId"));
    }
```

</details>

<!-- /test -->

<!-- test: ImageSessionStoreTest.interruptedAtomicWriteKeepsPreviousCheckpointAndExplicitDiscardRemovesQueue -->
<a id="test-96c1b3fcbb1056a6"></a>
### `ImageSessionStoreTest.interruptedAtomicWriteKeepsPreviousCheckpointAndExplicitDiscardRemovesQueue`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Vero JSON/checkpoint e file queue125 nel Context host; risultati manuali con dettagli20k; scrittura AtomicFile interrotta chiudendo stream senza finishWrite.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageSessionStoreTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageSessionStore.save`, `ImageSessionStore.load`, `ImageSessionStore.exists`, `ImageSessionStore.discard`, `ImageQueueStore.file`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Scrittura incompleta recupera checkpoint precedente; discard rimuove sessione e queue.

**Limiti:** Nessun processo ucciso o filesystem Android. Load non verifica media. Budget2kB riguarda checkpoint privo dettagli, non memoria runtime.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void interruptedAtomicWriteKeepsPreviousCheckpointAndExplicitDiscardRemovesQueue()throws Exception{
        ImageRunState run=sample();ImageSessionStore.save(context,id,run);
        AtomicFile file=new AtomicFile(new File(context.getFilesDir(),"image-session.json"));FileOutputStream incomplete=file.startWrite();incomplete.write("broken".getBytes(java.nio.charset.StandardCharsets.UTF_8));incomplete.close();
        assertEquals(0,ImageSessionStore.load(context).run.completed);assertTrue(ImageSessionStore.exists(context));
        ImageSessionStore.discard(context);assertFalse(ImageSessionStore.exists(context));assertFalse(ImageQueueStore.file(context,id).exists());
    }
```

</details>

<!-- /test -->

<!-- test: ImageSessionStoreTest.missingQueueIsReportedWithoutSilentlyDroppingTheSession -->
<a id="test-0a4bfcf3a4fd813d"></a>
### `ImageSessionStoreTest.missingQueueIsReportedWithoutSilentlyDroppingTheSession`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Vero JSON/checkpoint e file queue125 nel Context host; risultati manuali con dettagli20k; scrittura AtomicFile interrotta chiudendo stream senza finishWrite.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageSessionStoreTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageSessionStore.save`, `ImageQueueStore.discard`, `ImageSessionStore.load`, `ImageSessionStore.exists`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Queue assente produce IOException ma lascia sessione per diagnosi, senza scarto silenzioso.

**Limiti:** Nessun processo ucciso o filesystem Android. Load non verifica media. Budget2kB riguarda checkpoint privo dettagli, non memoria runtime.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void missingQueueIsReportedWithoutSilentlyDroppingTheSession()throws Exception{
        ImageRunState run=sample();ImageSessionStore.save(context,id,run);ImageQueueStore.discard(context,id);
        assertThrows(IOException.class,()->ImageSessionStore.load(context));assertTrue(ImageSessionStore.exists(context));
    }
```

</details>

<!-- /test -->


<a id="class-queueplaybackservicetest"></a>
## QueuePlaybackServiceTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/QueuePlaybackServiceTest.java) · 1 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** ConversionService PAUSE/SKIP/RESUME; ConversionControl; BatchRetry; notification.

**Riferimenti produttivi:** [ConversionService](../../app/src/main/java/it/edo/codecprobe/ConversionService.java), [ConversionControl](../../app/src/main/java/it/edo/codecprobe/ConversionControl.java), [BatchRetry](../../app/src/main/java/it/edo/codecprobe/BatchRetry.java).

**Preparazione, file e simulazioni condivisi:** Provider directory bloccato con CountDownLatch, poi URI sorgenti assenti; worker reale Robolectric; wake/notifiche shadow e reflection.

**Confini comuni dell’audit:** Nessuna conversione reale. Pausa al confine preflight; secondo input fallisce prima codec. Polling10ms/4s, latch3s e worker5s.

**Metodi:** [pauseSkipResumeKeepsNotificationAndExcludesSkippedFileFromRetry](#test-c5cac3a61ae53acb)

<!-- test: QueuePlaybackServiceTest.pauseSkipResumeKeepsNotificationAndExcludesSkippedFileFromRetry -->
<a id="test-c5cac3a61ae53acb"></a>
### `QueuePlaybackServiceTest.pauseSkipResumeKeepsNotificationAndExcludesSkippedFileFromRetry`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider directory bloccato con CountDownLatch, poi URI sorgenti assenti; worker reale Robolectric; wake/notifiche shadow e reflection.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/QueuePlaybackServiceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.snapshot`, `ConversionService.retryRequest`, `BatchRetry.select`, `service.onStartCommand`, `ConversionService.class.getDeclaredField`, `ConversionService.class.getDeclaredMethod`, `BatchRetry.load`, `ConversionService.State.class.getDeclaredFields`, `ConversionService.rememberFailures`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Token obsoleto respinto; Pause rilascia wake e notifica Riprendi; Skip solo file corrente escluso retry, Resume prosegue al secondo che fallisce.

**Limiti:** Nessuna conversione reale. Pausa al confine preflight; secondo input fallisce prima codec. Polling10ms/4s, latch3s e worker5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void pauseSkipResumeKeepsNotificationAndExcludesSkippedFileFromRetry()throws Exception{
        Directory directory=new Directory();ShadowContentResolver.registerProviderInternal("dirs",directory);
        var controller=Robolectric.buildService(ConversionService.class).create();ConversionService service=controller.get();
        ConversionService.State before=ConversionService.snapshot();Intent beforeRetry=ConversionService.retryRequest(service);
        try{
            Intent task=BatchRetry.select(BatchRetryTest.queue(service),java.util.List.of(0,1));
            service.onStartCommand(task,0,1);assertTrue(directory.entered.await(3,TimeUnit.SECONDS));
            long session=ConversionService.snapshot().startedAt;
            service.onStartCommand(command(service,ConversionService.PAUSE,session+1,1),0,2);assertFalse(ConversionService.snapshot().pauseRequested);
            service.onStartCommand(command(service,ConversionService.PAUSE,session,1),0,3);directory.release.countDown();
            await(()->ConversionService.snapshot().paused);
            Field wake=ConversionService.class.getDeclaredField("wake");wake.setAccessible(true);assertFalse(((PowerManager.WakeLock)wake.get(service)).isHeld());
            Method make=ConversionService.class.getDeclaredMethod("notification",ConversionService.State.class);make.setAccessible(true);
            Notification notification=(Notification)make.invoke(service,ConversionService.snapshot());
            assertTrue((notification.flags&Notification.FLAG_ONGOING_EVENT)!=0);assertEquals("Conversione in pausa",notification.extras.getCharSequence(Notification.EXTRA_TITLE));
            assertEquals("Riprendi",notification.actions[1].title);assertTrue(notification.actions[1].actionIntent.isService());
            Intent resume=Shadows.shadowOf(notification.actions[1].actionIntent).getSavedIntent();assertEquals(ConversionService.RESUME,resume.getAction());
            service.onStartCommand(command(service,ConversionService.SKIP,session,2),0,4);assertFalse(ConversionService.snapshot().skipping);
            service.onStartCommand(command(service,ConversionService.SKIP,session,1),0,5);
            await(()->{var state=ConversionService.snapshot();return state.batchIndex==2&&state.paused;});
            assertEquals(1,ConversionService.snapshot().skipped);assertEquals(0,ConversionService.snapshot().failed);assertTrue(ConversionService.snapshot().failures.isEmpty());assertNull(BatchRetry.load(service));
            service.onStartCommand(command(service,ConversionService.SKIP,session,1),0,6);assertFalse(ConversionService.snapshot().skipping);
            service.onStartCommand(resume,0,7);
            Field worker=ConversionService.class.getDeclaredField("worker");worker.setAccessible(true);((ExecutorService)worker.get(service)).submit(()->{}).get(5,TimeUnit.SECONDS);
            Shadows.shadowOf(Looper.getMainLooper()).idle();ConversionService.State done=ConversionService.snapshot();
            assertFalse(done.running);assertEquals(1,done.skipped);assertEquals(1,done.failed);assertTrue(done.sizeReport.contains("Saltato dall’utente"));
            assertEquals(java.util.List.of("content://input/1"),BatchRetry.load(service).getStringArrayListExtra("inputs"));
            assertFalse(((PowerManager.WakeLock)wake.get(service)).isHeld());
        }finally{
            directory.release.countDown();
            Field workerField=ConversionService.class.getDeclaredField("worker");workerField.setAccessible(true);
            ExecutorService executor=(ExecutorService)workerField.get(service);controller.destroy();
            assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));Shadows.shadowOf(Looper.getMainLooper()).idle();
            Field stored=ConversionService.class.getDeclaredField("state");stored.setAccessible(true);Object actual=stored.get(null);
            for(Field field:ConversionService.State.class.getDeclaredFields()){field.setAccessible(true);field.set(actual,field.get(before));}
            ConversionService.rememberFailures(service,beforeRetry);
        }
    }
```

</details>

<!-- /test -->


<a id="class-recoverylifecycletest"></a>
## RecoveryLifecycleTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/RecoveryLifecycleTest.java) · 2 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** HomeSavedState; HomeSystem timer; ConversionService; ConversionSessionState; SavedSession; ConversionRunGate.

**Riferimenti produttivi:** [HomeSavedState](../../app/src/main/java/it/edo/codecprobe/HomeSavedState.java), [ConversionService](../../app/src/main/java/it/edo/codecprobe/ConversionService.java), [ConversionSessionState](../../app/src/main/java/it/edo/codecprobe/ConversionSessionState.java), [SavedSession](../../app/src/main/java/it/edo/codecprobe/SavedSession.java), [ConversionRunGate](../../app/src/main/java/it/edo/codecprobe/ConversionRunGate.java).

**Preparazione, file e simulazioni condivisi:** Bundle 'another-process', journal sample, SystemClock virtuale; provider directory bloccato/poi fail; due istanze servizio simulato.

**Confini comuni dell’audit:** Process death simulata tramite stringa/reset static, non processo Android terminato. Lease protegge worker senza codec; reflection e timeout2–5s.

**Metodi:** [restoredBundleStartsEmptyWithoutMetadataScanAndBothTimersUseAccumulatedTime](#test-ed8a31bcea635ef3) · [destroyedWorkerRetainsLeaseUntilCleanupAndRepeatedStartsCannotOverlap](#test-38490cab0199f0bd)

<!-- test: RecoveryLifecycleTest.restoredBundleStartsEmptyWithoutMetadataScanAndBothTimersUseAccumulatedTime -->
<a id="test-ed8a31bcea635ef3"></a>
### `RecoveryLifecycleTest.restoredBundleStartsEmptyWithoutMetadataScanAndBothTimersUseAccumulatedTime`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Bundle 'another-process', journal sample, SystemClock virtuale; provider directory bloccato/poi fail; due istanze servizio simulato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/RecoveryLifecycleTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.snapshot`, `SavedSession.save`, `ConversionService.State`, `home.state.selectedInputs.isEmpty`, `home.screen.conversion.sizeReport.getText`, `home.handler.removeCallbacks`, `home.conversion.renderConversionDashboard`, `home.screen.conversion.elapsedLabel.getText`, `home.system.sample.run`, `SavedSession.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Bundle da processo diverso non ripristina selezione né avvia scan; timer mantiene45s, poi50s run e50s finita anche dopo2h virtuali.

**Limiti:** Process death simulata tramite stringa/reset static, non processo Android terminato. Lease protegge worker senza codec; reflection e timeout2–5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void restoredBundleStartsEmptyWithoutMetadataScanAndBothTimersUseAccumulatedTime()throws Exception{
        Context context=RuntimeEnvironment.getApplication();ConversionService.State before=ConversionService.snapshot();
        Intent request=BatchRetryTest.queue(context);SavedSession.save(context,request,SavedSessionTest.sample(request));stateField().set(null,new ConversionService.State());
        Bundle fromDeadProcess=new Bundle();fromDeadProcess.putString("homeProcess","another-process");fromDeadProcess.putString("input","content://input/0");
        fromDeadProcess.putStringArrayList("inputs",request.getStringArrayListExtra("inputs"));fromDeadProcess.putStringArrayList("names",request.getStringArrayListExtra("names"));
        var activity=Robolectric.buildActivity(MainActivity.class).create(fromDeadProcess).start().resume().visible();
        try{
            HomeController home=activity.get().home;
            assertNull(home.state.input);assertTrue(home.state.selectedInputs.isEmpty());assertFalse(home.state.metadataLoading);assertNull(home.selection.metadataDialog);
            assertTrue(home.screen.conversion.sizeReport.getText().toString().contains("0,50 kB"));assertTrue(ConversionService.snapshot().recoverable);
            home.handler.removeCallbacks(home.system.sample);
            ConversionService.State saved=ConversionService.snapshot();home.conversion.renderConversionDashboard(saved);String initial=home.screen.conversion.elapsedLabel.getText().toString();
            home.system.sample.run();assertEquals(initial,home.screen.conversion.elapsedLabel.getText().toString());assertEquals("00:00:45",initial);
            ShadowSystemClock.advanceBy(Duration.ofSeconds(10));
            ConversionService.State running=new ConversionService.State(saved);running.startedAt=SystemClock.elapsedRealtime()-5000;running.running=true;stateField().set(null,running);
            home.conversion.renderConversionDashboard(running);assertEquals("00:00:50",home.screen.conversion.elapsedLabel.getText().toString());
            home.system.sample.run();assertEquals("00:00:50",home.screen.conversion.elapsedLabel.getText().toString());
            running.endedAt=SystemClock.elapsedRealtime();running.running=false;ShadowSystemClock.advanceBy(Duration.ofHours(2));
            home.system.sample.run();assertEquals("00:00:50",home.screen.conversion.elapsedLabel.getText().toString());
            assertNull(home.selection.metadataDialog);assertTrue(home.state.selectedInputs.isEmpty());
        }finally{activity.pause().stop().destroy();stateField().set(null,before);SavedSession.clear(context);}
    }
```

</details>

<!-- /test -->

<!-- test: RecoveryLifecycleTest.destroyedWorkerRetainsLeaseUntilCleanupAndRepeatedStartsCannotOverlap -->
<a id="test-38490cab0199f0bd"></a>
### `RecoveryLifecycleTest.destroyedWorkerRetainsLeaseUntilCleanupAndRepeatedStartsCannotOverlap`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Bundle 'another-process', journal sample, SystemClock virtuale; provider directory bloccato/poi fail; due istanze servizio simulato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/RecoveryLifecycleTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.snapshot`, `BatchRetry.select`, `SavedSession.save`, `ConversionService.State`, `SavedSession.load`, `ConversionService.batchLogFile`, `log.contains`, `SavedSession.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Servizio distrutto mantiene lease fino cleanup; START ripetuti/altro servizio non sovrapposti; metriche vecchie azzerate, resume successivo fallisce controllato e logga risorse.

**Limiti:** Process death simulata tramite stringa/reset static, non processo Android terminato. Lease protegge worker senza codec; reflection e timeout2–5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void destroyedWorkerRetainsLeaseUntilCleanupAndRepeatedStartsCannotOverlap()throws Exception{
        Context context=RuntimeEnvironment.getApplication();ConversionService.State before=ConversionService.snapshot();
        Intent request=BatchRetry.select(BatchRetryTest.queue(context),List.of(0,1));
        request.putStringArrayListExtra("outputNames",new ArrayList<>(List.of("first.mp4","second.mp4"))).putExtra("omitted",new boolean[2]);
        ConversionService.State original=SavedSessionTest.sample(request);original.bframeCount=8;original.lookaheadCount=40;original.aqMode=3;original.aqStrength=1000;
        SavedSession.save(context,request,original);stateField().set(null,new ConversionService.State());
        SavedSessionServiceTest.Directory directory=new SavedSessionServiceTest.Directory();ShadowContentResolver.registerProviderInternal("dirs",directory);
        var old=Robolectric.buildService(ConversionService.class).create();var other=Robolectric.buildService(ConversionService.class).create();
        try{
            old.get().onStartCommand(SavedSession.load(context).resumeRequest(),0,1);assertTrue(directory.entered.await(3,TimeUnit.SECONDS));
            ConversionService.State active=ConversionService.snapshot();assertEquals(0,active.doneFrames);assertEquals(0,active.filePercent);assertEquals(-1,active.lookaheadCount);assertEquals(-1,active.aqMode);assertEquals(-1,active.aqStrength);
            assertEquals(1,active.succeeded);assertEquals(45000,active.elapsedBefore);
            old.get().onStartCommand(SavedSession.load(context).resumeRequest(),0,2);assertEquals(1,directory.requested.size());
            old.destroy();other.get().onStartCommand(SavedSession.load(context).resumeRequest(),0,3);
            assertEquals(active.sessionId,ConversionService.snapshot().sessionId);assertEquals(1,directory.requested.size());assertTrue(ConversionService.snapshot().running);
            directory.release.countDown();drain(old.get());assertFalse(ConversionService.snapshot().running);assertTrue(SavedSession.load(context).state.recoverable);
            other.destroy();directory.fail=true;
            var next=Robolectric.buildService(ConversionService.class).create();
            try{
                next.get().onStartCommand(SavedSession.load(context).resumeRequest(),0,1);drain(next.get());
                assertEquals(1,ConversionService.snapshot().succeeded);assertEquals(1,ConversionService.snapshot().failed);assertFalse(ConversionService.snapshot().running);
                String log=new String(java.nio.file.Files.readAllBytes(ConversionService.batchLogFile(context).toPath()),java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(log.contains("RISORSE ·"));assertTrue(log.contains("dopo rilascio risorse"));
            }finally{next.destroy();}
        }finally{directory.release.countDown();drain(old.get());old.destroy();other.destroy();stateField().set(null,before);SavedSession.clear(context);}
    }
```

</details>

<!-- /test -->


<a id="class-recoverypresentationtest"></a>
## RecoveryPresentationTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/RecoveryPresentationTest.java) · 2 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** HomeConversion recovery/dashboard; HomeSavedSession; HomeSelection; HomeSavedState; VideoMetadataCache.

**Riferimenti produttivi:** [HomeConversion](../../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [HomeSavedSession](../../app/src/main/java/it/edo/codecprobe/HomeSavedSession.java), [HomeSelection](../../app/src/main/java/it/edo/codecprobe/HomeSelection.java), [HomeSavedState](../../app/src/main/java/it/edo/codecprobe/HomeSavedState.java), [VideoMetadataCache](../../app/src/main/java/it/edo/codecprobe/VideoMetadataCache.java).

**Preparazione, file e simulazioni condivisi:** Journal sample/stato iniettati; worker bloccato con latch; nessun provider video valido, cache può contenere report d'errore.

**Confini comuni dell’audit:** Non esegue il servizio conversione lanciato né metadati video validi. 'One reader across recreation' controlla generation/loading/cache, non conta aperture effettive.

**Metodi:** [pendingRecoveryHidesFileMetricsWithoutLosingReportOrJournal](#test-b9932420f95211db) · [explicitResumeRestoresNamesImmediatelyAndUsesOneBackgroundReaderAcrossRecreation](#test-52ead93df8715929)

<!-- test: RecoveryPresentationTest.pendingRecoveryHidesFileMetricsWithoutLosingReportOrJournal -->
<a id="test-b9932420f95211db"></a>
### `RecoveryPresentationTest.pendingRecoveryHidesFileMetricsWithoutLosingReportOrJournal`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Journal sample/stato iniettati; worker bloccato con latch; nessun provider video valido, cache può contenere report d'errore.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/RecoveryPresentationTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.snapshot`, `SavedSession.save`, `ConversionService.State`, `home.state.selectedInputs.isEmpty`, `home.screen.conversion.percentLabel.getText`, `home.screen.conversion.progress.getProgress`, `home.screen.conversion.frameLabel.getText`, `home.screen.conversion.timeline.getText`, `home.screen.conversion.activeCpu.getText`, `home.screen.conversion.activeCrf.getText`, `home.screen.conversion.activeBframes.getText`, `home.screen.conversion.activeLookahead.getText`, `home.screen.conversion.currentFile.getText`, `home.screen.conversion.elapsedLabel.getText`, `home.screen.conversion.sizeReport.getText`, `SavedSession.load`, `home.conversion.renderConversionDashboard`, `SavedSession.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Recovery in attesa nasconde vecchi frame/opzioni ma mantiene report45s e journal intatto; render attivo torna7%.

**Limiti:** Non esegue il servizio conversione lanciato né metadati video validi. 'One reader across recreation' controlla generation/loading/cache, non conta aperture effettive.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void pendingRecoveryHidesFileMetricsWithoutLosingReportOrJournal()throws Exception{
        Context context=RuntimeEnvironment.getApplication();ConversionService.State before=ConversionService.snapshot();
        Intent request=BatchRetryTest.queue(context);ConversionService.State saved=SavedSessionTest.sample(request);
        saved.bframeCount=4;saved.lookaheadCount=5;saved.cuTree=0;saved.aqMode=2;saved.aqStrength=1000;saved.memoryProtection=true;
        SavedSession.save(context,request,saved);field().set(null,new ConversionService.State());
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;assertTrue(home.state.selectedInputs.isEmpty());assertNull(home.selection.metadataDialog);
            assertEquals("0%",home.screen.conversion.percentLabel.getText().toString());assertEquals(0,home.screen.conversion.progress.getProgress());
            assertEquals("—",home.screen.conversion.frameLabel.getText().toString());assertEquals("—",home.screen.conversion.timeline.getText().toString());
            assertEquals("—",home.screen.conversion.activeCpu.getText().toString());assertEquals("—",home.screen.conversion.activeCrf.getText().toString());
            assertEquals("—",home.screen.conversion.activeBframes.getText().toString());assertEquals("—",home.screen.conversion.activeLookahead.getText().toString());
            assertEquals("Nessun file in conversione",home.screen.conversion.currentFile.getText().toString());
            assertEquals("00:00:45",home.screen.conversion.elapsedLabel.getText().toString());assertTrue(home.screen.conversion.sizeReport.getText().toString().contains("0,50 kB"));
            SavedSession untouched=SavedSession.load(context);assertEquals(42,untouched.state.filePercent);assertEquals(100,untouched.state.doneFrames);assertEquals(saved.outputUris,untouched.state.outputUris);
            ConversionService.State active=new ConversionService.State(untouched.state);active.running=true;active.filePercent=7;active.doneFrames=17;
            home.conversion.renderConversionDashboard(active);assertEquals("7%",home.screen.conversion.percentLabel.getText().toString());
            assertTrue(home.screen.conversion.frameLabel.getText().toString().startsWith("17 /"));
        }finally{controller.pause().stop().destroy();field().set(null,before);SavedSession.clear(context);}
    }
```

</details>

<!-- /test -->

<!-- test: RecoveryPresentationTest.explicitResumeRestoresNamesImmediatelyAndUsesOneBackgroundReaderAcrossRecreation -->
<a id="test-52ead93df8715929"></a>
### `RecoveryPresentationTest.explicitResumeRestoresNamesImmediatelyAndUsesOneBackgroundReaderAcrossRecreation`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Journal sample/stato iniettati; worker bloccato con latch; nessun provider video valido, cache può contenere report d'errore.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/RecoveryPresentationTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.snapshot`, `SavedSession.save`, `ConversionService.State`, `VideoMetadataCache.clear`, `home.state.selectedInputs.isEmpty`, `home.selection.worker.submit`, `SavedSession.load`, `home.conversion.launchPrepared`, `home.state.destinationPlan.individualFolders`, `home.state.destinationPlan.individualOrigins`, `home.screen.files.videoChips.size`, `home.screen.conversion.savedSession.isEnabled`, `home.selection.inspectInput`, `home.screen.files.videoChips.get`, `home.state.selectedInputs.size`, `VideoMetadataCache.size`, `HomeSavedState.inThisProcess`, `SavedSession.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Resume esplicito ripristina subito URI/nomi/cartelle, background senza dialog/generation duplicata; recreate conserva sessione, Bundle processo morto ripulito.

**Limiti:** Non esegue il servizio conversione lanciato né metadati video validi. 'One reader across recreation' controlla generation/loading/cache, non conta aperture effettive.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void explicitResumeRestoresNamesImmediatelyAndUsesOneBackgroundReaderAcrossRecreation()throws Exception{
        Context context=RuntimeEnvironment.getApplication();ConversionService.State before=ConversionService.snapshot();
        Intent request=BatchRetryTest.queue(context);SavedSession.save(context,request,SavedSessionTest.sample(request));field().set(null,new ConversionService.State());
        VideoMetadataCache.clear();var controller=Robolectric.buildActivity(MainActivity.class).setup();
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);Bundle bundle=new Bundle();
        try{
            HomeController home=controller.get().home;assertTrue(home.state.selectedInputs.isEmpty());assertFalse(home.state.metadataLoading);
            home.selection.worker.submit(()->{entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});
            assertTrue(entered.await(2,TimeUnit.SECONDS));Intent resume=SavedSession.load(context).resumeRequest();
            home.conversion.launchPrepared(resume);
            assertEquals(request.getStringArrayListExtra("inputs"),home.state.selectedInputs);assertEquals(request.getStringArrayListExtra("names"),home.state.selectedNames);
            assertEquals(request.getStringArrayListExtra("folders"),home.state.destinationPlan.individualFolders());
            assertArrayEquals(request.getBooleanArrayExtra("individualOrigins"),home.state.destinationPlan.individualOrigins());
            assertEquals("saved-test-session",home.state.selectionSession);
            assertTrue(home.state.metadataLoading);assertTrue(home.selection.metadataBackground);assertNull(home.selection.metadataDialog);
            assertEquals("Rileggo i metadati in background",ShadowToast.getTextOfLatestToast());assertEquals(3,home.screen.files.videoChips.size());
            assertTrue(home.screen.conversion.savedSession.isEnabled());
            int generation=home.state.metadataGeneration;home.selection.inspectInput();assertEquals(generation,home.state.metadataGeneration);
            home.screen.files.videoChips.get(1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertEquals(1,home.state.selectedVideoIndex);
            controller.saveInstanceState(bundle);
        }finally{release.countDown();controller.pause().stop().destroy();}
        var recreated=Robolectric.buildActivity(MainActivity.class).create(bundle).start().resume();
        try{
            HomeController home=recreated.get().home;assertEquals(3,home.state.selectedInputs.size());assertEquals("saved-test-session",home.state.selectionSession);
            assertNull(home.selection.metadataDialog);
            home.selection.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(home.state.metadataLoading);assertTrue(VideoMetadataCache.size()>0);assertNull(home.selection.metadataDialog);
            int generation=home.state.metadataGeneration;home.selection.inspectInput();assertEquals(generation,home.state.metadataGeneration);
            Bundle dead=new Bundle(bundle);dead.putString("homeProcess","dead-process");Bundle clean=HomeSavedState.inThisProcess(dead);
            assertNull(clean.getStringArrayList("inputs"));assertNull(clean.getString("selectionSession"));
        }finally{recreated.pause().stop().destroy();field().set(null,before);SavedSession.clear(context);VideoMetadataCache.clear();}
    }
```

</details>

<!-- /test -->


<a id="class-resumeparameterstest"></a>
## ResumeParametersTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ResumeParametersTest.java) · 2 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** ResumeParameters.choose; ConversionSessionState.begin; SavedSession; HomeSavedSession.

**Riferimenti produttivi:** [ResumeParameters](../../app/src/main/java/it/edo/codecprobe/ResumeParameters.java), [ConversionSessionState](../../app/src/main/java/it/edo/codecprobe/ConversionSessionState.java), [SavedSession](../../app/src/main/java/it/edo/codecprobe/SavedSession.java), [HomeSavedSession](../../app/src/main/java/it/edo/codecprobe/HomeSavedSession.java).

**Preparazione, file e simulazioni condivisi:** Intent/sample in memoria e vero round-trip journal nel primo; secondo Activity/permission shadow e pendingResume/Bundle.

**Confini comuni dell’audit:** Nessun encoder applica parametri; primo modifica solo CPU/CRF/preset/temporal e preserva onlySmaller/naming, non test generico tutte opzioni.

**Metodi:** [currentChoiceChangesOnlyTabTwoAndPersistsWithRecoveredReport](#test-6254967d3d8c55c3) · [popupChoiceSurvivesRecreationAndPermissionDenialLeavesDiskUnchanged](#test-a70003e9082f4240)

<!-- test: ResumeParametersTest.currentChoiceChangesOnlyTabTwoAndPersistsWithRecoveredReport -->
<a id="test-6254967d3d8c55c3"></a>
### `ResumeParametersTest.currentChoiceChangesOnlyTabTwoAndPersistsWithRecoveredReport`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Intent/sample in memoria e vero round-trip journal nel primo; secondo Activity/permission shadow e pendingResume/Bundle.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ResumeParametersTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ResumeParameters.choose`, `ConversionSessionState.begin`, `SavedSession.save`, `SavedSession.load`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Scelta corrente modifica parametri tab2, conserva elenco/destinazioni/report/elapsed e sopravvive save/load senza mutare request originale.

**Limiti:** Nessun encoder applica parametri; primo modifica solo CPU/CRF/preset/temporal e preserva onlySmaller/naming, non test generico tutte opzioni.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void currentChoiceChangesOnlyTabTwoAndPersistsWithRecoveredReport()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Intent original=BatchRetryTest.queue(context);
        SavedSession initial=new SavedSession(original,SavedSessionTest.sample(original));
        Intent current=new Intent().putExtra("crf",23).putExtra("preset",5).putExtra("cpu",0).putExtra("bframes",true).putExtra("lookahead",false)
            .putExtra("onlySmaller",false).putExtra("namingAddition","DO NOT COPY");
        Intent keep=ResumeParameters.choose(initial.resumeRequest(),current,false);
        assertEquals(2,keep.getIntExtra("cpu",-1));assertEquals(17,keep.getIntExtra("crf",-1));assertEquals(8,keep.getIntExtra("preset",-1));
        Intent changed=ResumeParameters.choose(initial.resumeRequest(),current,true);
        assertEquals(0,changed.getIntExtra("cpu",-1));assertEquals(23,changed.getIntExtra("crf",-1));assertEquals(5,changed.getIntExtra("preset",-1));
        assertTrue(changed.getBooleanExtra("bframes",false));assertFalse(changed.getBooleanExtra("lookahead",true));assertTrue(changed.getBooleanExtra("onlySmaller",false));
        assertFalse(changed.hasExtra("namingAddition"));assertEquals(original.getStringArrayListExtra("inputs"),changed.getStringArrayListExtra("inputs"));
        assertEquals(original.getStringArrayListExtra("folders"),changed.getStringArrayListExtra("folders"));assertEquals(original.getStringArrayListExtra("outputNames"),changed.getStringArrayListExtra("outputNames"));
        ConversionService.State state=ConversionSessionState.begin(changed,initial,initial.state.items,1000);
        assertEquals(0,state.requestedCpu);assertEquals(23,state.requestedCrf);assertEquals("medium",state.requestedPreset);assertEquals(45000,state.elapsedBefore);
        assertEquals(initial.state.outputUris,state.outputUris);assertEquals(initial.state.failures,state.failures);assertEquals(1,state.succeeded);
        state.startedAt=0;SavedSession.save(context,changed,state);SavedSession loaded=SavedSession.load(context);
        assertEquals(0,loaded.state.requestedCpu);assertEquals(0,loaded.resumeRequest().getIntExtra("cpu",-1));assertEquals("medium",loaded.state.requestedPreset);
        assertEquals(initial.state.sizeReport,loaded.state.sizeReport);assertEquals(initial.state.outputUris,loaded.state.outputUris);
        assertEquals(2,original.getIntExtra("cpu",-1));assertEquals(17,original.getIntExtra("crf",-1));
    }
```

</details>

<!-- /test -->

<!-- test: ResumeParametersTest.popupChoiceSurvivesRecreationAndPermissionDenialLeavesDiskUnchanged -->
<a id="test-a70003e9082f4240"></a>
### `ResumeParametersTest.popupChoiceSurvivesRecreationAndPermissionDenialLeavesDiskUnchanged`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Intent/sample in memoria e vero round-trip journal nel primo; secondo Activity/permission shadow e pendingResume/Bundle.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ResumeParametersTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.class.getDeclaredField`, `ConversionService.snapshot`, `SavedSession.save`, `ConversionService.State`, `home.screen.compression.crfSlider.setProgress`, `home.screen.compression.presetSlider.setProgress`, `home.screen.compression.cpuMode.setSelection`, `home.conversion.savedSession.confirm`, `saved.isChecked`, `saved.getText`, `home.conversion.savedSession.pendingResume.getIntExtra`, `SavedSession.load`, `SavedSession.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Popup sceglie CPU0 congelato attraverso recreate; diniego permesso scarta pendingResume e lascia discoCPU2 senza avvio.

**Limiti:** Nessun encoder applica parametri; primo modifica solo CPU/CRF/preset/temporal e preserva onlySmaller/naming, non test generico tutte opzioni.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void popupChoiceSurvivesRecreationAndPermissionDenialLeavesDiskUnchanged()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Field field=ConversionService.class.getDeclaredField("state");field.setAccessible(true);ConversionService.State before=ConversionService.snapshot();
        Intent original=BatchRetryTest.queue(context);SavedSession.save(context,original,SavedSessionTest.sample(original));field.set(null,new ConversionService.State());
        var activity=Robolectric.buildActivity(MainActivity.class).setup();Bundle bundle=new Bundle();
        try{
            HomeController home=activity.get().home;
            Shadows.shadowOf(home.activity).denyPermissions(android.Manifest.permission.ACCESS_MEDIA_LOCATION);
            home.screen.compression.crfSlider.setProgress(23);home.screen.compression.presetSlider.setProgress(5);home.screen.compression.cpuMode.setSelection(0);
            home.conversion.savedSession.confirm();AlertDialog dialog=home.conversion.savedSession.dialog;
            RadioButton saved=option(dialog.getWindow().getDecorView(),"Usa i parametri salvati"),current=option(dialog.getWindow().getDecorView(),"Usa quelli attuali");
            assertNotNull(saved);assertNotNull(current);assertTrue(saved.isChecked());assertTrue(saved.getText().toString().contains("CPU: Max"));assertTrue(current.getText().toString().contains("CPU: Limited"));
            // Hold continuation so the Activity can be recreated before a permission result.
            home.state.busy=true;
            current.performClick();dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertNotNull(home.conversion.savedSession.pendingResume);assertEquals(0,home.conversion.savedSession.pendingResume.getIntExtra("cpu",-1));
            home.screen.compression.cpuMode.setSelection(2);assertEquals(0,home.conversion.savedSession.pendingResume.getIntExtra("cpu",-1));
            activity.saveInstanceState(bundle);assertEquals(2,SavedSession.load(context).request.getIntExtra("cpu",-1));
        }finally{activity.pause().stop().destroy();}
        var recreated=Robolectric.buildActivity(MainActivity.class).create(bundle).start().resume();
        try{
            HomeSavedSession recovery=recreated.get().home.conversion.savedSession;
            assertNotNull(recovery.pendingResume);assertEquals(0,recovery.pendingResume.getIntExtra("cpu",-1));
            recovery.onPermissions(new int[]{android.content.pm.PackageManager.PERMISSION_DENIED});assertNull(recovery.pendingResume);
            assertEquals(2,SavedSession.load(context).request.getIntExtra("cpu",-1));assertFalse(ConversionService.snapshot().running);
        }finally{recreated.pause().stop().destroy();field.set(null,before);SavedSession.clear(context);}
    }
```

</details>

<!-- /test -->


<a id="class-savedsessionservicetest"></a>
## SavedSessionServiceTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/SavedSessionServiceTest.java) · 1 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** ConversionService.restoreSession/onStartCommand SAVE_STOP; SavedSession; ConversionSessionState.

**Riferimenti produttivi:** [ConversionService](../../app/src/main/java/it/edo/codecprobe/ConversionService.java), [SavedSession](../../app/src/main/java/it/edo/codecprobe/SavedSession.java), [ConversionSessionState](../../app/src/main/java/it/edo/codecprobe/ConversionSessionState.java).

**Preparazione, file e simulazioni condivisi:** Provider Directory bloccante/poi querynull, State e metriche encoder preparati a mano. Shadow custom AndroidAtomicFile.finishWrite aggiunge replace atomic sul Windows host.

**Confini comuni dell’audit:** 'FreshProcess' è reset state + nuova istanza servizio, nessun vero processo. Si ferma prima codec; custom shadow corregge semantica Windows e non prova atomicità Android reale.

**Metodi:** [durableStopAndFreshProcessRecoverySkipCompletedFileAndKeepWholeReport](#test-9e7c2f1ea6bcc9f5)

<!-- test: SavedSessionServiceTest.durableStopAndFreshProcessRecoverySkipCompletedFileAndKeepWholeReport -->
<a id="test-9e7c2f1ea6bcc9f5"></a>
### `SavedSessionServiceTest.durableStopAndFreshProcessRecoverySkipCompletedFileAndKeepWholeReport`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Directory bloccante/poi querynull, State e metriche encoder preparati a mano. Shadow custom AndroidAtomicFile.finishWrite aggiunge replace atomic sul Windows host.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SavedSessionServiceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `BatchRetry.select`, `ConversionService.snapshot`, `SavedSession.save`, `ConversionService.State`, `ConversionService.restoreSession`, `service.onStartCommand`, `SavedSession.load`, `ConversionService.class.getDeclaredField`, `ConversionService.batchLogFile`, `log.split`, `log.contains`, `ConversionService.clearSessionIfIdle`, `SavedSession.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** SAVE_STOP token giusto conserva sessione/cursore/report e ripulisce metriche; restart salta primo completato, secondo fallisce, preserva1 successo/1fallimento e poi clear elimina journal.

**Limiti:** 'FreshProcess' è reset state + nuova istanza servizio, nessun vero processo. Si ferma prima codec; custom shadow corregge semantica Windows e non prova atomicità Android reale.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void durableStopAndFreshProcessRecoverySkipCompletedFileAndKeepWholeReport()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Intent request=BatchRetry.select(BatchRetryTest.queue(context),List.of(0,1));
        request.putStringArrayListExtra("outputNames",new ArrayList<>(List.of("custom-zero.mp4","custom-one.mp4"))).putExtra("omitted",new boolean[2]);
        ConversionService.State original=SavedSessionTest.sample(request),before=ConversionService.snapshot();
        SavedSession.save(context,request,original);install(new ConversionService.State());
        Directory directory=new Directory();ShadowContentResolver.registerProviderInternal("dirs",directory);
        var controller=Robolectric.buildService(ConversionService.class).create();ConversionService service=controller.get();
        try{
            assertNull(ConversionService.restoreSession(context));assertTrue(ConversionService.snapshot().recoverable);assertEquals(42,ConversionService.snapshot().filePercent);
            service.onStartCommand(SavedSession.load(context).resumeRequest(),0,1);assertTrue(directory.entered.await(4,TimeUnit.SECONDS));
            long session=ConversionService.snapshot().startedAt;
            // Simulate current encoder metrics while the worker is held in the provider.
            Field stateField=ConversionService.class.getDeclaredField("state");stateField.setAccessible(true);
            ConversionService.State live=(ConversionService.State)stateField.get(null);
            live.filePercent=42;live.doneFrames=100;live.totalFrames=238;live.done=3333333;live.total=7933333;
            live.bframeCount=4;live.lookaheadCount=5;live.cuTree=0;live.aqMode=2;live.aqStrength=1;live.memoryProtection=true;
            service.onStartCommand(new Intent(context,ConversionService.class).setAction(ConversionService.PAUSE).putExtra(MainActivity.CANCEL_SESSION,session),0,2);
            assertEquals(42,ConversionService.snapshot().filePercent);assertEquals(100,ConversionService.snapshot().doneFrames);
            assertEquals(4,ConversionService.snapshot().bframeCount);

            service.onStartCommand(new Intent(context,ConversionService.class).setAction(ConversionService.SAVE_STOP).putExtra(MainActivity.CANCEL_SESSION,session+1),0,2);assertFalse(ConversionService.snapshot().savingSession);
            service.onStartCommand(new Intent(context,ConversionService.class).setAction(ConversionService.SAVE_STOP).putExtra(MainActivity.CANCEL_SESSION,session),0,3);
            // A last callback before cleanup must also be cleared by finalization.
            live.filePercent=43;live.doneFrames=101;
            directory.release.countDown();drain(service);
            SavedSession stopped=SavedSession.load(context);assertTrue(stopped.state.recoverable);assertFalse(ConversionService.snapshot().running);
            assertEquals(ConversionService.snapshot().message,0,stopped.state.filePercent);assertEquals(0,stopped.state.doneFrames);assertEquals(-1,stopped.state.totalFrames);
            assertEquals(0,stopped.state.done);assertEquals(0,stopped.state.total);assertEquals(0,stopped.state.percent);
            assertEquals(-1,stopped.state.bframeCount);assertEquals(-1,stopped.state.lookaheadCount);assertEquals(-1,stopped.state.cuTree);
            assertEquals(-1,stopped.state.aqMode);assertEquals(-1d,stopped.state.aqStrength,0);assertFalse(stopped.state.memoryProtection);
            assertEquals(0,ConversionService.snapshot().filePercent);assertEquals(-1,ConversionService.snapshot().bframeCount);
            assertTrue(stopped.state.elapsedBefore>=original.elapsedBefore);
            assertEquals(original.sessionId,stopped.state.sessionId);
            assertEquals(1,stopped.state.succeeded);assertEquals(0,stopped.state.failed);assertEquals("content://saved/first",stopped.state.outputUris.get(0));
            assertTrue(directory.requested.stream().noneMatch(uri->uri.contains("zero")));
            controller.destroy();install(new ConversionService.State());
            assertNull(ConversionService.restoreSession(context));assertEquals(stopped.state.sizeReport,ConversionService.snapshot().sizeReport);
            directory.fail=true;
            var next=Robolectric.buildService(ConversionService.class).create();
            try{
                next.get().onStartCommand(SavedSession.load(context).resumeRequest(),0,1);drain(next.get());
                SavedSession finished=SavedSession.load(context);assertFalse(finished.state.recoverable);assertEquals(1,finished.state.succeeded);assertEquals(1,finished.state.failed);
                assertTrue(finished.state.sizeReport.contains("1,00 kB → 0,50 kB"));assertEquals(2,finished.state.failures.get(0).number);
                String log=new String(java.nio.file.Files.readAllBytes(ConversionService.batchLogFile(context).toPath()),java.nio.charset.StandardCharsets.UTF_8);
                assertEquals(2,log.split("RIPRESA SESSIONE SALVATA",-1).length-1);
                assertFalse(log.contains("*** INIZIO VIDEO 1 / 2 ***"));
            }finally{next.destroy();}
            install(new ConversionService.State());ConversionService.restoreSession(context);assertFalse(ConversionService.snapshot().recoverable);assertEquals(1,ConversionService.snapshot().succeeded);
            assertTrue(ConversionService.clearSessionIfIdle(context));assertNull(SavedSession.load(context));
        }finally{directory.release.countDown();controller.destroy();install(before);SavedSession.clear(context);}
    }
```

</details>

<!-- /test -->


<a id="class-savedsessiontest"></a>
## SavedSessionTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/SavedSessionTest.java) · 3 metodi · area originaria: Code e operazioni asincrone.

**Contesto applicativo della classe:** SavedSession.save/load/resumeRequest/clear; AtomicFile; CompressionSummary.

**Riferimenti produttivi:** [SavedSession](../../app/src/main/java/it/edo/codecprobe/SavedSession.java), [CompressionSummary](../../app/src/main/java/it/edo/codecprobe/CompressionSummary.java).

**Preparazione, file e simulazioni condivisi:** Journal JSON reale da sample manuale di3file, risultati/output URI inesistenti; Context solo directory e Intent dati.

**Confini comuni dell’audit:** Non riapre media, non verifica permessi né kill/power loss. Secondo usa failWrite esplicito (rollback ordinato), non crash improvviso.

**Metodi:** [roundTripPreservesReportErrorsProgressOptionsAndPerFileUris](#test-5a7f0cd5304e2c47) · [interruptedAtomicWriteKeepsPreviousSessionAndCompletedQueueOnlyRestoresReport](#test-48ee0fde7a2d9083) · [invalidSessionIsReportedAndPreservedForDiagnosis](#test-83fca3417ff76103)

<!-- test: SavedSessionTest.roundTripPreservesReportErrorsProgressOptionsAndPerFileUris -->
<a id="test-5a7f0cd5304e2c47"></a>
### `SavedSessionTest.roundTripPreservesReportErrorsProgressOptionsAndPerFileUris`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Journal JSON reale da sample manuale di3file, risultati/output URI inesistenti; Context solo directory e Intent dati.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SavedSessionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `SavedSession.save`, `SavedSession.load`, `saved.resumeRequest`, `SavedSession.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Round-trip conserva report/errori/frame/tempo/opzioni/URI e resume finishedFiles; clear rimuove sessione.

**Limiti:** Non riapre media, non verifica permessi né kill/power loss. Secondo usa failWrite esplicito (rollback ordinato), non crash improvviso.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void roundTripPreservesReportErrorsProgressOptionsAndPerFileUris()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Intent request=BatchRetryTest.queue(context);
        request.putExtra("namingAddition","converted-").putExtra("namingPrefix",false);
        ConversionService.State original=sample(request);SavedSession.save(context,request,original);
        SavedSession saved=SavedSession.load(context);assertNotNull(saved);ConversionService.State s=saved.state;
        assertTrue(s.recoverable);assertFalse(s.running);assertFalse(s.paused);assertEquals(original.sessionId,s.sessionId);
        assertEquals(original.sizeReport,s.sizeReport);assertEquals(original.outputUris,s.outputUris);assertEquals(original.failures.get(0).copyText(),s.failures.get(0).copyText());
        assertEquals(42,s.filePercent);assertEquals(100,s.doneFrames);assertEquals(3333333,s.done);assertEquals(45000,s.elapsedBefore);assertEquals(0,s.startedAt);
        assertEquals(17,s.requestedCrf);assertEquals("veryslow",s.requestedPreset);assertEquals(2,s.requestedCpu);
        Intent resume=saved.resumeRequest();assertArrayEquals(new boolean[]{true,false,true},resume.getBooleanArrayExtra("finishedFiles"));
        assertEquals(original.sessionId,resume.getStringExtra(SavedSession.RESTORE));assertEquals(request.getStringArrayListExtra("outputNames"),resume.getStringArrayListExtra("requestedOutputNames"));
        assertEquals("converted-",resume.getStringExtra("namingAddition"));assertFalse(resume.getBooleanExtra("namingPrefix",true));assertFalse(resume.getBooleanExtra("bframes",true));
        SavedSession.clear(context);assertNull(SavedSession.load(context));
    }
```

</details>

<!-- /test -->

<!-- test: SavedSessionTest.interruptedAtomicWriteKeepsPreviousSessionAndCompletedQueueOnlyRestoresReport -->
<a id="test-48ee0fde7a2d9083"></a>
### `SavedSessionTest.interruptedAtomicWriteKeepsPreviousSessionAndCompletedQueueOnlyRestoresReport`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Journal JSON reale da sample manuale di3file, risultati/output URI inesistenti; Context solo directory e Intent dati.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SavedSessionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompressionSummary.Item`, `SavedSession.save`, `SavedSession.load`, `saved.hasPending`, `saved.state.sizeReport.contains`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** AtomicFile.failWrite mantiene checkpoint precedente; coda finita resta solo report e conteggi NO_SAVING.

**Limiti:** Non riapre media, non verifica permessi né kill/power loss. Secondo usa failWrite esplicito (rollback ordinato), non crash improvviso.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void interruptedAtomicWriteKeepsPreviousSessionAndCompletedQueueOnlyRestoresReport()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Intent request=BatchRetryTest.queue(context);ConversionService.State s=sample(request);
        s.items.set(1,new CompressionSummary.Item(2,s.names.get(1),CompressionSummary.Status.NO_SAVING,100,150));s.recoverable=false;
        SavedSession.save(context,request,s);
        AtomicFile file=new AtomicFile(new File(context.getFilesDir(),"conversion-session.json"));
        FileOutputStream interrupted=file.startWrite();interrupted.write("partial".getBytes(java.nio.charset.StandardCharsets.UTF_8));file.failWrite(interrupted);
        SavedSession saved=SavedSession.load(context);assertFalse(saved.state.recoverable);assertFalse(saved.hasPending());assertEquals(1,saved.state.succeeded);assertEquals(1,saved.state.failed);assertEquals(1,saved.state.notSaved);
        assertTrue(saved.state.sizeReport.contains("Nessun risparmio"));assertEquals(s.outputUris,saved.state.outputUris);
    }
```

</details>

<!-- /test -->

<!-- test: SavedSessionTest.invalidSessionIsReportedAndPreservedForDiagnosis -->
<a id="test-83fca3417ff76103"></a>
### `SavedSessionTest.invalidSessionIsReportedAndPreservedForDiagnosis`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Journal JSON reale da sample manuale di3file, risultati/output URI inesistenti; Context solo directory e Intent dati.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SavedSessionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `SavedSession.load`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** JSON invalido produce IOException e resta su disco per diagnosi.

**Limiti:** Non riapre media, non verifica permessi né kill/power loss. Secondo usa failWrite esplicito (rollback ordinato), non crash improvviso.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void invalidSessionIsReportedAndPreservedForDiagnosis()throws Exception{
        Context context=RuntimeEnvironment.getApplication();File file=new File(context.getFilesDir(),"conversion-session.json");
        try(OutputStream out=new FileOutputStream(file)){out.write("invalid json".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        try{SavedSession.load(context);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("non leggibile"));}
        assertTrue(file.exists());
    }
```

</details>

<!-- /test -->
