# Video

[Guida](README.md) · [Indice completo](index.md) · [Revisioni](review-needed.md) · [Esecuzioni](esecuzioni.md)

Descrizioni statiche trasferite dall’audit: non attestano nuove esecuzioni. Le fixture condivise sono descritte a livello di classe; dati specifici, ordine delle operazioni e assert sono nel corpo integrale di ogni metodo. Gli helper richiamati e gli eventuali `@Before`/`@After` sono consultabili nel sorgente collegato. Lo script di aggiornamento non riscrive questa pagina.

## Classi

- [ConverterControlTest](#class-convertercontroltest)
- [FailedQueueServiceTest](#class-failedqueueservicetest)
- [HomeLifecycleTest](#class-homelifecycletest)
- [HomeResetTest](#class-homeresettest)
- [HomeSavingsRetryTest](#class-homesavingsretrytest)
- [MemoryProtectionTest](#class-memoryprotectiontest)
- [VideoCarouselTest](#class-videocarouseltest)
- [VideoMetadataCacheTest](#class-videometadatacachetest)
- [VideoSettingsParityTest](#class-videosettingsparitytest)

<a id="class-convertercontroltest"></a>
## ConverterControlTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ConverterControlTest.java) · 3 metodi · area originaria: Video.

**Contesto applicativo della classe:** VideoConverter.isCancelled/convert; HdrTranscoder.isCancelled; ConversionControl; ConversionCleanup.temporaryFiles.

**Riferimenti produttivi:** [VideoConverter](../../app/src/main/java/it/edo/codecprobe/VideoConverter.java), [HdrTranscoder](../../app/src/main/java/it/edo/codecprobe/HdrTranscoder.java), [ConversionControl](../../app/src/main/java/it/edo/codecprobe/ConversionControl.java), [ConversionCleanup](../../app/src/main/java/it/edo/codecprobe/ConversionCleanup.java).

**Preparazione, file e simulazioni condivisi:** Listener no-op; HdrTranscoder costruito ma non avviato; latch/executor; working MP4 tre byte e directory temporanea non vuota.

**Confini comuni dell’audit:** Non esegue SDR/HDR decode/encode. Il primo verifica callback comune, il secondo solo early abort; terzo IO reale dipende da semantica cancellazione directory non vuota.

**Metodi:** [sdrAndHdrCancellationCallbacksUseTheSamePauseGate](#test-a64ea6deab9a64b4) · [skipBeforeOpeningInputDeletesWorkingFileAndNeverCreatesOutput](#test-375ddb2fa376792a) · [cleanupReportsAnUndeletableTemporaryAndStillDeletesOtherFiles](#test-bd9d19658065f641)

<!-- test: ConverterControlTest.sdrAndHdrCancellationCallbacksUseTheSamePauseGate -->
<a id="test-a64ea6deab9a64b4"></a>
### `ConverterControlTest.sdrAndHdrCancellationCallbacksUseTheSamePauseGate`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Listener no-op; HdrTranscoder costruito ma non avviato; latch/executor; working MP4 tre byte e directory temporanea non vuota.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ConverterControlTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `gate.beginFile`, `gate.pause`, `gate.skip`, `converter.isCancelled`, `gate.cancel`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Callback HdrTranscoder entra nella gate di pausa del VideoConverter e si sblocca con skip, senza transcoding.

**Limiti:** Non esegue SDR/HDR decode/encode. Il primo verifica callback comune, il secondo solo early abort; terzo IO reale dipende da semantica cancellazione directory non vuota.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void sdrAndHdrCancellationCallbacksUseTheSamePauseGate()throws Exception{
        AtomicBoolean cancel=new AtomicBoolean();CountDownLatch paused=new CountDownLatch(1);
        ConversionControl gate=new ConversionControl(cancel,android.os.SystemClock::elapsedRealtime,p->{if(p)paused.countDown();});
        VideoConverter converter=converter(cancel);converter.control=gate;
        HdrTranscoder hdr=new HdrTranscoder(converter::isCancelled,null,null);
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            gate.beginFile(1);gate.pause(true);Future<Boolean> check=worker.submit(hdr::isCancelled);
            assertTrue(paused.await(3,TimeUnit.SECONDS));assertFalse(check.isDone());
            assertTrue(gate.skip(1));assertTrue(check.get(3,TimeUnit.SECONDS));assertTrue(converter.isCancelled());
        }finally{gate.cancel();worker.shutdownNow();}
    }
```

</details>

<!-- /test -->

<!-- test: ConverterControlTest.skipBeforeOpeningInputDeletesWorkingFileAndNeverCreatesOutput -->
<a id="test-375ddb2fa376792a"></a>
### `ConverterControlTest.skipBeforeOpeningInputDeletesWorkingFileAndNeverCreatesOutput`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Listener no-op; HdrTranscoder costruito ma non avviato; latch/executor; working MP4 tre byte e directory temporanea non vuota.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ConverterControlTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `gate.beginFile`, `gate.skip`, `converter.convert`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Skip prima apertura URI inesistente cancella file working e non cancella l'intera coda.

**Limiti:** Non esegue SDR/HDR decode/encode. Il primo verifica callback comune, il secondo solo early abort; terzo IO reale dipende da semantica cancellazione directory non vuota.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void skipBeforeOpeningInputDeletesWorkingFileAndNeverCreatesOutput()throws Exception{
        AtomicBoolean cancel=new AtomicBoolean();ConversionControl gate=new ConversionControl(cancel,()->0L,p->{});
        VideoConverter converter=converter(cancel);converter.control=gate;gate.beginFile(1);assertTrue(gate.skip(1));
        File working=new File(RuntimeEnvironment.getApplication().getCacheDir(),"conversion-working.mp4");
        Files.write(working.toPath(),new byte[]{1,2,3});
        try {
            converter.convert(Uri.parse("content://missing/input"),Uri.parse("content://missing/folder"),23,"clip.mp4",5,1,true,true);
            fail("Must stop before opening the provider");
        }catch(InterruptedException expected){assertFalse(working.exists());}
        assertFalse(cancel.get()); // File skip is independent of queue cancellation.
    }
```

</details>

<!-- /test -->

<!-- test: ConverterControlTest.cleanupReportsAnUndeletableTemporaryAndStillDeletesOtherFiles -->
<a id="test-bd9d19658065f641"></a>
### `ConverterControlTest.cleanupReportsAnUndeletableTemporaryAndStillDeletesOtherFiles`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Listener no-op; HdrTranscoder costruito ma non avviato; latch/executor; working MP4 tre byte e directory temporanea non vuota.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ConverterControlTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionCleanup.temporaryFiles`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Cleanup fallisce su directory non vuota ma cancella altro file e conserva diagnosi.

**Limiti:** Non esegue SDR/HDR decode/encode. Il primo verifica callback comune, il secondo solo early abort; terzo IO reale dipende da semantica cancellazione directory non vuota.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void cleanupReportsAnUndeletableTemporaryAndStillDeletesOtherFiles()throws Exception{
        File root=Files.createTempDirectory("cleanup-test").toFile();
        File blocked=new File(root,"nonempty");assertTrue(blocked.mkdir());File child=new File(blocked,"child");Files.write(child.toPath(),new byte[]{1});
        File removable=new File(root,"removable");Files.write(removable.toPath(),new byte[]{1});
        try{ConversionCleanup.temporaryFiles(blocked,removable);fail("Must report failed cleanup");}
        catch(ConversionCleanup.Failed expected){assertTrue(expected.getMessage().contains("nonempty"));assertFalse(removable.exists());}
        finally{child.delete();blocked.delete();root.delete();}
    }
```

</details>

<!-- /test -->


<a id="class-failedqueueservicetest"></a>
## FailedQueueServiceTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/FailedQueueServiceTest.java) · 1 metodi · area originaria: Video.

**Contesto applicativo della classe:** ConversionService.onStartCommand/worker; ConversionFailure; BatchRetry; batch log.

**Riferimenti produttivi:** [ConversionService](../../app/src/main/java/it/edo/codecprobe/ConversionService.java), [ConversionFailure](../../app/src/main/java/it/edo/codecprobe/ConversionFailure.java), [BatchRetry](../../app/src/main/java/it/edo/codecprobe/BatchRetry.java).

**Preparazione, file e simulazioni condivisi:** START reale del servizio Robolectric con URI di cartella senza provider e primo elemento omitted; executor vero, file log vero.

**Confini comuni dell’audit:** Fallisce intenzionalmente prima dei codec; non prova classificazione errore decoder né successo. Accesso worker/campi State via reflection.

**Metodi:** [actualFailureIsPersistedButExplicitlyIgnoredVideoIsExcluded](#test-77a7ace9f5020511)

<!-- test: FailedQueueServiceTest.actualFailureIsPersistedButExplicitlyIgnoredVideoIsExcluded -->
<a id="test-77a7ace9f5020511"></a>
### `FailedQueueServiceTest.actualFailureIsPersistedButExplicitlyIgnoredVideoIsExcluded`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): START reale del servizio Robolectric con URI di cartella senza provider e primo elemento omitted; executor vero, file log vero.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/FailedQueueServiceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.snapshot`, `ConversionService.retryRequest`, `BatchRetry.select`, `service.onStartCommand`, `ConversionService.class.getDeclaredField`, `state.failures.size`, `state.failures.get`, `ConversionService.batchLogFile`, `BatchRetry.load`, `ConversionService.State.class.getDeclaredFields`, `ConversionService.rememberFailures`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Il ciclo batch ignora zero, registra fallimento IOException di one nel log, conta 1 ignored/1 failed e persiste retry allineato.

**Limiti:** Fallisce intenzionalmente prima dei codec; non prova classificazione errore decoder né successo. Accesso worker/campi State via reflection.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void actualFailureIsPersistedButExplicitlyIgnoredVideoIsExcluded()throws Exception {
        ServiceController<ConversionService> controller=Robolectric.buildService(ConversionService.class).create();
        ConversionService service=controller.get();
        ConversionService.State before=ConversionService.snapshot();
        Intent beforeRetry=ConversionService.retryRequest(service);
        try{
            Intent task=BatchRetry.select(BatchRetryTest.queue(service),List.of(0,1));
            task.putExtra("omitted",new boolean[]{true,false});
            task.putStringArrayListExtra("outputNames",new ArrayList<>(List.of("zero.mp4","one.mp4")));
            // This URI has no writable directory provider, so the second file fails before any codec runs.
            service.onStartCommand(task,0,1);
            Field field=ConversionService.class.getDeclaredField("worker");field.setAccessible(true);
            ((ExecutorService)field.get(service)).submit(()->{}).get(5,TimeUnit.SECONDS);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            ConversionService.State state=ConversionService.snapshot();assertFalse(state.running);assertEquals(1,state.ignored);assertEquals(1,state.failed);assertEquals(0,state.notSaved);
            assertEquals(1,state.failures.size());assertEquals(2,state.failures.get(0).number);
            assertTrue(state.failures.get(0).details.contains("IOException"));assertFalse(state.failures.get(0).message.isEmpty());
            assertTrue(new String(java.nio.file.Files.readAllBytes(ConversionService.batchLogFile(service).toPath()),java.nio.charset.StandardCharsets.UTF_8).contains(state.failures.get(0).copyText()));
            Intent retry=BatchRetry.load(service);assertNotNull(retry);assertEquals(List.of("content://input/1"),retry.getStringArrayListExtra("inputs"));
            assertEquals(List.of("content://dirs/tree/one"),retry.getStringArrayListExtra("folders"));
            assertEquals(List.of("one.mp4"),retry.getStringArrayListExtra("requestedOutputNames"));
        }finally{
            controller.destroy();
            Field stored=ConversionService.class.getDeclaredField("state");stored.setAccessible(true);Object actual=stored.get(null);
            for(Field value:ConversionService.State.class.getDeclaredFields()){value.setAccessible(true);value.set(actual,value.get(before));}
            ConversionService.rememberFailures(service,beforeRetry);
        }
    }
```

</details>

<!-- /test -->


<a id="class-homelifecycletest"></a>
## HomeLifecycleTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/HomeLifecycleTest.java) · 4 metodi · area originaria: Video.

**Contesto applicativo della classe:** HomeController/State/Selection/System; HomeDiagnostics; HomeConversion.

**Riferimenti produttivi:** [HomeController](../../app/src/main/java/it/edo/codecprobe/HomeController.java), [HomeDiagnostics](../../app/src/main/java/it/edo/codecprobe/HomeDiagnostics.java), [HomeConversion](../../app/src/main/java/it/edo/codecprobe/HomeConversion.java).

**Preparazione, file e simulazioni condivisi:** Bundle/cache metadati e ConversionService.State artificiali; Activity e Intent Robolectric, file snapshot/sentinella reali.

**Confini comuni dell’audit:** Recreate/saveInstanceState non è kill del processo. Nessun metadata extractor o codec; lifecycle reale Android e lavoro foreground non provati.

**Metodi:** [recreationKeepsParametersDetailsAndPendingFolderRequest](#test-ff14739b09c691fb) · [pickVideosAndOpenSettingsReachTheCorrectAndroidActions](#test-dec6d474abcb4d1d) · [cancellingTxtPickerDeletesOnlyItsTemporarySnapshot](#test-98ccd8156dcf3578) · [reopeningAnActiveSessionShowsConversionAndClosingDoesNotCancelIt](#test-a02cba3b0cb00da7)

<!-- test: HomeLifecycleTest.recreationKeepsParametersDetailsAndPendingFolderRequest -->
<a id="test-ff14739b09c691fb"></a>
### `HomeLifecycleTest.recreationKeepsParametersDetailsAndPendingFolderRequest`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Bundle/cache metadati e ConversionService.State artificiali; Activity e Intent Robolectric, file snapshot/sentinella reali.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeLifecycleTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.compression.selectedCrf`, `home.screen.compression.presetSlider.getProgress`, `home.screen.compression.cpuMode.getSelectedItemPosition`, `home.screen.compression.bframeSwitch.isChecked`, `home.screen.compression.lookaheadSwitch.isChecked`, `home.screen.files.videoDetails.getVisibility`, `home.screen.sections.bodies.get`, `VideoMetadataCache.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Ricrea parametri, dettagli, card e picker origine pendente; controller vecchio destroyed ed executor chiusi.

**Limiti:** Recreate/saveInstanceState non è kill del processo. Nessun metadata extractor o codec; lifecycle reale Android e lavoro foreground non provati.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void recreationKeepsParametersDetailsAndPendingFolderRequest(){
        ActivityController<MainActivity> first=Robolectric.buildActivity(MainActivity.class).create(oneCachedVideo()).start().resume().visible();Bundle saved=new Bundle();HomeController old=first.get().home;
        try{
            old.screen.compression.crfSlider.setProgress(11);old.screen.compression.presetSlider.setProgress(8);old.screen.compression.cpuMode.setSelection(2);
            old.screen.compression.bframeSwitch.setChecked(true);old.screen.compression.lookaheadSwitch.setChecked(false);
            old.screen.files.details.performClick();old.state.choosingOrigins=true;old.state.pendingOriginSource=old.state.input.toString();old.state.pendingOriginParent="primary:DCIM";
            old.screen.sections.focusCard(MainActivity.CARD_CONVERSION);first.saveInstanceState(saved);
        }finally{first.pause().stop().destroy();}
        assertTrue(old.state.destroyed);assertTrue(old.selection.worker.isShutdown());assertTrue(old.system.worker.isShutdown());
        ActivityController<MainActivity> second=Robolectric.buildActivity(MainActivity.class).create(saved).start().resume().visible();
        try{
            HomeController home=second.get().home;assertNotSame(old,home);
            assertEquals(11,home.compression.selectedCrf());assertEquals(8,home.screen.compression.presetSlider.getProgress());assertEquals(2,home.screen.compression.cpuMode.getSelectedItemPosition());
            assertTrue(home.screen.compression.bframeSwitch.isChecked());assertFalse(home.screen.compression.lookaheadSwitch.isChecked());
            assertEquals(View.VISIBLE,home.screen.files.videoDetails.getVisibility());assertEquals("primary:DCIM",home.state.pendingOriginParent);
            assertTrue(home.state.choosingOrigins);assertFalse(home.state.metadataLoading);
            assertEquals(View.VISIBLE,home.screen.sections.bodies.get(MainActivity.CARD_CONVERSION).getVisibility());
            second.get().onActivityResult(HomeRequests.PICK_ORIGIN,Activity.RESULT_CANCELED,null);
            assertFalse(home.state.choosingOrigins);assertNull(home.state.pendingOriginSource);
        }finally{second.pause().stop().destroy();VideoMetadataCache.clear();}
    }
```

</details>

<!-- /test -->

<!-- test: HomeLifecycleTest.pickVideosAndOpenSettingsReachTheCorrectAndroidActions -->
<a id="test-dec6d474abcb4d1d"></a>
### `HomeLifecycleTest.pickVideosAndOpenSettingsReachTheCorrectAndroidActions`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Bundle/cache metadati e ConversionService.State artificiali; Activity e Intent Robolectric, file snapshot/sentinella reali.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeLifecycleTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.screen.files.pickVideo.performClick`, `home.screen.compression.crfSlider.setProgress`, `home.screen.preferences.performClick`, `SettingsActivity.class.getName`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Picker video emette OPEN_DOCUMENT video/* multiplo e impostazioni ricevono CRF18.

**Limiti:** Recreate/saveInstanceState non è kill del processo. Nessun metadata extractor o codec; lifecycle reale Android e lavoro foreground non provati.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void pickVideosAndOpenSettingsReachTheCorrectAndroidActions(){
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            MainActivity activity=controller.get();HomeController home=activity.home;
            home.screen.files.pickVideo.performClick();Intent picker=shadowOf(activity).getNextStartedActivityForResult().intent;
            assertEquals(Intent.ACTION_OPEN_DOCUMENT,picker.getAction());assertEquals("video/*",picker.getType());assertTrue(picker.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));
            home.screen.compression.crfSlider.setProgress(18);home.screen.preferences.performClick();Intent settings=shadowOf(activity).getNextStartedActivity();
            assertEquals(SettingsActivity.class.getName(),settings.getComponent().getClassName());assertEquals(18,settings.getIntExtra("crf",-1));
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: HomeLifecycleTest.cancellingTxtPickerDeletesOnlyItsTemporarySnapshot -->
<a id="test-98ccd8156dcf3578"></a>
### `HomeLifecycleTest.cancellingTxtPickerDeletesOnlyItsTemporarySnapshot`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Bundle/cache metadati e ConversionService.State artificiali; Activity e Intent Robolectric, file snapshot/sentinella reali.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeLifecycleTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Cancel export elimina solo snapshot, non un altro file temporaneo.

**Limiti:** Recreate/saveInstanceState non è kill del processo. Nessun metadata extractor o codec; lifecycle reale Android e lavoro foreground non provati.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void cancellingTxtPickerDeletesOnlyItsTemporarySnapshot()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            MainActivity activity=controller.get();File snapshot=File.createTempFile("log-export-",".txt",activity.getCacheDir()),other=File.createTempFile("original-",".mp4",activity.getCacheDir());
            activity.home.state.pendingLog=snapshot.getAbsolutePath();activity.onActivityResult(HomeRequests.SAVE_LOG,Activity.RESULT_CANCELED,null);
            assertFalse(snapshot.exists());assertTrue(other.exists());assertNull(activity.home.state.pendingLog);other.delete();
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: HomeLifecycleTest.reopeningAnActiveSessionShowsConversionAndClosingDoesNotCancelIt -->
<a id="test-a02cba3b0cb00da7"></a>
### `HomeLifecycleTest.reopeningAnActiveSessionShowsConversionAndClosingDoesNotCancelIt`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Bundle/cache metadati e ConversionService.State artificiali; Activity e Intent Robolectric, file snapshot/sentinella reali.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeLifecycleTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.class.getDeclaredField`, `ConversionService.State`, `home.screen.sections.bodies.get`, `home.screen.conversion.percentLabel.getText`, `home.screen.compression.crfSlider.isEnabled`, `home.screen.conversion.stop.isEnabled`, `ConversionService.snapshot`, `ConversionService.State.class.getDeclaredFields`, `VideoMetadataCache.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Run iniettata all'apertura mostra conversione22%; chiusura Activity non cambia running del modello.

**Limiti:** Recreate/saveInstanceState non è kill del processo. Nessun metadata extractor o codec; lifecycle reale Android e lavoro foreground non provati.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void reopeningAnActiveSessionShowsConversionAndClosingDoesNotCancelIt()throws Exception{
        java.lang.reflect.Field field=ConversionService.class.getDeclaredField("state");field.setAccessible(true);
        ConversionService.State live=(ConversionService.State)field.get(null),before=new ConversionService.State(live);
        ActivityController<MainActivity> controller=null;
        try{
            Bundle cached=oneCachedVideo();live.hasSession=true;live.running=true;live.inputs=cached.getStringArrayList("inputs");live.names=cached.getStringArrayList("names");
            live.source="Originale.mp4";live.batchTotal=1;live.batchIndex=1;live.doneFrames=22;live.totalFrames=100;live.filePercent=22;live.requestedCrf=18;
            controller=Robolectric.buildActivity(MainActivity.class).setup();HomeController home=controller.get().home;
            assertEquals(View.VISIBLE,home.screen.sections.bodies.get(MainActivity.CARD_CONVERSION).getVisibility());assertEquals("22%",home.screen.conversion.percentLabel.getText().toString());
            assertFalse(home.screen.compression.crfSlider.isEnabled());assertTrue(home.screen.conversion.stop.isEnabled());
            controller.pause().stop().destroy();controller=null;assertTrue(ConversionService.snapshot().running);
        }finally{
            if(controller!=null)controller.pause().stop().destroy();
            for(java.lang.reflect.Field f:ConversionService.State.class.getDeclaredFields())f.set(live,f.get(before));
            VideoMetadataCache.clear();
        }
    }
```

</details>

<!-- /test -->


<a id="class-homeresettest"></a>
## HomeResetTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/HomeResetTest.java) · 3 metodi · area originaria: Video.

**Contesto applicativo della classe:** HomeReset.clear/update; ConversionService.clearSessionIfIdle; VideoMetadataCache; BatchRetry.

**Riferimenti produttivi:** [HomeReset](../../app/src/main/java/it/edo/codecprobe/HomeReset.java), [ConversionService](../../app/src/main/java/it/edo/codecprobe/ConversionService.java), [VideoMetadataCache](../../app/src/main/java/it/edo/codecprobe/VideoMetadataCache.java), [BatchRetry](../../app/src/main/java/it/edo/codecprobe/BatchRetry.java).

**Preparazione, file e simulazioni condivisi:** Activity Robolectric, stato/report iniettati; due veri file sorgente/output e log sentinella; cache metadati costruita.

**Confini comuni dell’audit:** File multimediali sono 3/2 byte. Nessun reset durante lavoro reale/kill, molte asserzioni dipendono dalle view interne.

**Metodi:** [clearRemovesSelectionAndReportWithoutDeletingMediaSettingsOrLogAndSurvivesRecreation](#test-c85b910fcae077f2) · [activeOrPausedQueueCannotBeClearedEvenByCallingTheActionDirectly](#test-ad12077e63e43a9b) · [metadataReadCannotBeClearedUntilItFinishes](#test-63e7e49e8bf16dfc)

<!-- test: HomeResetTest.clearRemovesSelectionAndReportWithoutDeletingMediaSettingsOrLogAndSurvivesRecreation -->
<a id="test-c85b910fcae077f2"></a>
### `HomeResetTest.clearRemovesSelectionAndReportWithoutDeletingMediaSettingsOrLogAndSurvivesRecreation`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity Robolectric, stato/report iniettati; due veri file sorgente/output e log sentinella; cache metadati costruita.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeResetTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.batchLogFile`, `log.exists`, `log.toPath`, `ConversionService.State`, `CompressionSummary.render`, `CompressionSummary.Item`, `ConversionService.rememberFailures`, `BatchRetry.select`, `VideoMetadataCache.permissions`, `VideoMetadataCache.clear`, `VideoMetadataCache.Entry`, `InputInspector.Report`, `VideoMetadataCache.put`, `VideoMetadataCache.epoch`, `saved.putString`, `saved.putStringArrayList`, `home.conversion.renderSession`, `home.screen.compression.crfSlider.setProgress`; altri passaggi nel sorgente collegato.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Clear svuota selezione/report/cache/retry e sopravvive recreate, preservando byte sorgente/output/log e slider/prefs.

**Limiti:** File multimediali sono 3/2 byte. Nessun reset durante lavoro reale/kill, molte asserzioni dipendono dalle view interne.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void clearRemovesSelectionAndReportWithoutDeletingMediaSettingsOrLogAndSurvivesRecreation()throws Exception{
        Context context=RuntimeEnvironment.getApplication();
        File original=File.createTempFile("keep-original-",".mp4",context.getCacheDir());
        File converted=File.createTempFile("keep-output-",".mp4",context.getCacheDir());
        Files.write(original.toPath(),new byte[]{1,2,3});Files.write(converted.toPath(),new byte[]{4,5});
        File log=ConversionService.batchLogFile(context);byte[] oldLog=log.exists()?Files.readAllBytes(log.toPath()):null;
        Files.write(log.toPath(),"Detailed log kept for export".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String input=Uri.fromFile(original).toString();
        ConversionService.State completed=new ConversionService.State();completed.hasSession=true;completed.batchIndex=2;completed.batchTotal=2;completed.filePercent=100;completed.succeeded=1;completed.failed=1;
        completed.inputs.add(input);completed.names.add("clip.mp4");completed.folders.add("content://dirs/tree/zero");completed.result=Uri.fromFile(converted);completed.results.add(completed.result);
        completed.failures.add(new ConversionFailure(2,"failed.mp4","Analisi",new IOException("Example error")));
        completed.sizeReport=CompressionSummary.render(List.of(new CompressionSummary.Item(1,"clip.mp4",CompressionSummary.Status.COMPLETE,3,2),new CompressionSummary.Item(2,"failed.mp4",CompressionSummary.Status.FAILED,-1,-1)));
        stored.set(null,completed);ConversionService.rememberFailures(context,BatchRetry.select(BatchRetryTest.queue(context),List.of(1)));
        VideoMetadataCache.permissions("false:false");VideoMetadataCache.clear();
        VideoMetadataCache.Entry entry=new VideoMetadataCache.Entry("clip.mp4","Video · 3 byte",new InputInspector.Report("video","metadata"));
        VideoMetadataCache.put(input,entry,VideoMetadataCache.epoch());
        Bundle saved=new Bundle();saved.putString("input",input);saved.putStringArrayList("inputs",new ArrayList<>(List.of(input)));saved.putStringArrayList("names",new ArrayList<>(List.of("clip.mp4")));
        var controller=Robolectric.buildActivity(MainActivity.class).create(saved).start().resume();
        try{
            HomeController home=controller.get().home;home.conversion.renderSession();
            home.screen.compression.crfSlider.setProgress(27);home.screen.compression.presetSlider.setProgress(3);
            Map<String,?> settings=new HashMap<>(context.getSharedPreferences("startup-settings",0).getAll());
            long epoch=VideoMetadataCache.epoch();int generation=home.state.metadataGeneration;
            home.screen.files.videoPreview.setAlpha(0.3f);
            assertTrue(home.screen.files.clearSelection.isEnabled());assertEquals(View.VISIBLE,home.screen.conversion.sizeReportPanel.getVisibility());
            home.screen.files.clearSelection.performClick();
            assertTrue(home.state.selectedInputs.isEmpty());assertTrue(home.state.selectedNames.isEmpty());assertNull(home.state.input);assertNull(home.state.resultUri);
            assertTrue(home.state.originFolders.isEmpty());assertTrue(home.state.destinationPlan.files.isEmpty());assertFalse(home.state.originsReady);
            assertTrue(home.state.metadataGeneration>generation);assertEquals(0,VideoMetadataCache.size());assertFalse(VideoMetadataCache.put(input,entry,epoch));
            assertEquals(1f,home.screen.files.videoPreview.getAlpha(),0f);assertEquals(0,home.screen.files.videoNavigator.getChildCount());assertEquals(0,home.screen.files.inputDetails.getChildCount());
            assertEquals("Nessun video selezionato",home.screen.files.videoLabel.getText().toString());assertEquals(View.GONE,home.screen.files.videoDetails.getVisibility());
            assertEquals(0,home.screen.conversion.progress.getProgress());assertEquals("Nessuna coda avviata",home.screen.conversion.queueLabel.getText().toString());
            assertEquals(View.GONE,home.screen.conversion.sizeReportPanel.getVisibility());assertEquals("",home.screen.conversion.sizeReport.getText().toString());assertEquals(0,home.screen.conversion.failureReport.getChildCount());
            assertEquals(View.GONE,home.screen.conversion.retryFailed.getVisibility());assertFalse(home.screen.files.clearSelection.isEnabled());
            assertEquals(27,home.screen.compression.crfSlider.getProgress());assertEquals(3,home.screen.compression.presetSlider.getProgress());assertEquals(settings,context.getSharedPreferences("startup-settings",0).getAll());
            assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(original.toPath()));assertArrayEquals(new byte[]{4,5},Files.readAllBytes(converted.toPath()));
            assertEquals("Detailed log kept for export",new String(Files.readAllBytes(log.toPath()),java.nio.charset.StandardCharsets.UTF_8));
            assertFalse(ConversionService.snapshot().hasSession);assertTrue(ConversionService.snapshot().failures.isEmpty());assertNull(BatchRetry.load(context));assertNull(ConversionService.retryRequest(context));
            home.conversion.renderSession();assertNull(home.state.input);
            Bundle empty=new Bundle();controller.saveInstanceState(empty).pause().stop().destroy();
            controller=Robolectric.buildActivity(MainActivity.class).create(empty).start().resume();
            home=controller.get().home;Shadows.shadowOf(Looper.getMainLooper()).idle();home.conversion.renderSession();
            assertTrue(home.state.selectedInputs.isEmpty());assertNull(home.state.resultUri);assertEquals(View.GONE,home.screen.conversion.sizeReportPanel.getVisibility());
            home.screen.files.pickVideo.performClick();assertEquals(Intent.ACTION_OPEN_DOCUMENT,Shadows.shadowOf(controller.get()).getNextStartedActivityForResult().intent.getAction());
        }finally{
            controller.pause().stop().destroy();original.delete();converted.delete();if(oldLog==null)log.delete();else Files.write(log.toPath(),oldLog);
        }
    }
```

</details>

<!-- /test -->

<!-- test: HomeResetTest.activeOrPausedQueueCannotBeClearedEvenByCallingTheActionDirectly -->
<a id="test-ad12077e63e43a9b"></a>
### `HomeResetTest.activeOrPausedQueueCannotBeClearedEvenByCallingTheActionDirectly`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity Robolectric, stato/report iniettati; due veri file sorgente/output e log sentinella; cache metadati costruita.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeResetTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.State`, `home.conversion.setBusy`, `home.screen.files.clearSelection.isEnabled`, `home.reset.clear`, `ConversionService.clearSessionIfIdle`, `ConversionService.snapshot`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Stato running+paused impedisce clear anche chiamando direttamente action.

**Limiti:** File multimediali sono 3/2 byte. Nessun reset durante lavoro reale/kill, molte asserzioni dipendono dalle view interne.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void activeOrPausedQueueCannotBeClearedEvenByCallingTheActionDirectly()throws Exception{
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;ConversionService.State active=new ConversionService.State();active.running=true;active.hasSession=true;active.paused=true;active.pauseRequested=true;active.sizeReport="Report to keep";stored.set(null,active);
            home.conversion.setBusy(true);assertFalse(home.screen.files.clearSelection.isEnabled());home.reset.clear();
            assertFalse(ConversionService.clearSessionIfIdle(controller.get()));assertTrue(ConversionService.snapshot().running);assertEquals("Report to keep",ConversionService.snapshot().sizeReport);
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: HomeResetTest.metadataReadCannotBeClearedUntilItFinishes -->
<a id="test-63e7e49e8bf16dfc"></a>
### `HomeResetTest.metadataReadCannotBeClearedUntilItFinishes`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity Robolectric, stato/report iniettati; due veri file sorgente/output e log sentinella; cache metadati costruita.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeResetTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.state.selectedInputs.add`, `home.reset.update`, `home.screen.files.clearSelection.isEnabled`, `home.reset.clear`, `home.state.selectedInputs.size`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** metadataLoading iniettato impedisce Clear fino a quando il flag termina; nessuna lettura effettiva.

**Limiti:** File multimediali sono 3/2 byte. Nessun reset durante lavoro reale/kill, molte asserzioni dipendono dalle view interne.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void metadataReadCannotBeClearedUntilItFinishes(){
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;home.state.selectedInputs.add("content://input/pending");home.state.metadataLoading=true;
            home.reset.update(false);assertFalse(home.screen.files.clearSelection.isEnabled());home.reset.clear();assertEquals(1,home.state.selectedInputs.size());
            home.state.metadataLoading=false;
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-homesavingsretrytest"></a>
## HomeSavingsRetryTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/HomeSavingsRetryTest.java) · 2 metodi · area originaria: Video.

**Contesto applicativo della classe:** HomeConversion; HomeRetry; HomeOutputCheck; BatchRetry.

**Riferimenti produttivi:** [HomeConversion](../../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [HomeRetry](../../app/src/main/java/it/edo/codecprobe/HomeRetry.java), [HomeOutputCheck](../../app/src/main/java/it/edo/codecprobe/HomeOutputCheck.java), [BatchRetry](../../app/src/main/java/it/edo/codecprobe/BatchRetry.java).

**Preparazione, file e simulazioni condivisi:** Provider directory sempre vuota e permessi shadow; richiesta fallimenti persista a mano, worker reale.

**Confini comuni dell’audit:** Nessuna collisione effettiva o conversione; solo Intent e stato checkbox. Timeout executor 5 s.

**Metodi:** [savingsCheckboxIsRememberedAndFrozenDuringConversion](#test-d6fddf1ddfbb9ce9) · [retryUsesOnlySavedFailuresAndRunsFilenamePreflightAgain](#test-668882c9597d0627)

<!-- test: HomeSavingsRetryTest.savingsCheckboxIsRememberedAndFrozenDuringConversion -->
<a id="test-d6fddf1ddfbb9ce9"></a>
### `HomeSavingsRetryTest.savingsCheckboxIsRememberedAndFrozenDuringConversion`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider directory sempre vuota e permessi shadow; richiesta fallimenti persista a mano, worker reale.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeSavingsRetryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.screen.conversion.onlySmaller.isChecked`, `home.screen.conversion.onlySmaller.setChecked`, `home.conversion.setBusy`, `home.screen.conversion.onlySmaller.isEnabled`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** onlySmaller ricordato fra Activity e disabilitato durante busy artificiale.

**Limiti:** Nessuna collisione effettiva o conversione; solo Intent e stato checkbox. Timeout executor 5 s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void savingsCheckboxIsRememberedAndFrozenDuringConversion(){
        ActivityController<MainActivity> first=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            assertTrue(first.get().home.screen.conversion.onlySmaller.isChecked());
            first.get().home.screen.conversion.onlySmaller.setChecked(false);
            first.get().home.conversion.setBusy(true);assertFalse(first.get().home.screen.conversion.onlySmaller.isEnabled());
        }finally{first.pause().stop().destroy();}
        ActivityController<MainActivity> second=Robolectric.buildActivity(MainActivity.class).setup();
        try{assertFalse(second.get().home.screen.conversion.onlySmaller.isChecked());}finally{second.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: HomeSavingsRetryTest.retryUsesOnlySavedFailuresAndRunsFilenamePreflightAgain -->
<a id="test-668882c9597d0627"></a>
### `HomeSavingsRetryTest.retryUsesOnlySavedFailuresAndRunsFilenamePreflightAgain`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider directory sempre vuota e permessi shadow; richiesta fallimenti persista a mano, worker reale.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeSavingsRetryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.rememberFailures`, `BatchRetry.select`, `home.conversion.setBusy`, `home.screen.conversion.retryFailed.getVisibility`, `home.screen.conversion.retryFailed.getText`, `home.screen.conversion.retryFailed.performClick`, `home.conversion.retry.confirmation.getButton`, `home.conversion.outputCheck.worker.submit`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Retry confermato ripassa preflight e lancia solo file fallito con cartella/nome/CRF/preset originali.

**Limiti:** Nessuna collisione effettiva o conversione; solo Intent e stato checkbox. Timeout executor 5 s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void retryUsesOnlySavedFailuresAndRunsFilenamePreflightAgain()throws Exception {
        Application app=RuntimeEnvironment.getApplication();
        Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.ACCESS_MEDIA_LOCATION,android.Manifest.permission.READ_MEDIA_VIDEO);
        ShadowContentResolver.registerProviderInternal("dirs",new EmptyDirectory());
        ConversionService.rememberFailures(app,BatchRetry.select(BatchRetryTest.queue(app),List.of(1)));
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;home.conversion.setBusy(false);
            assertEquals(View.VISIBLE,home.screen.conversion.retryFailed.getVisibility());assertTrue(home.screen.conversion.retryFailed.getText().toString().contains("1"));
            home.screen.conversion.retryFailed.performClick();assertNotNull(home.conversion.retry.confirmation);
            home.conversion.retry.confirmation.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            home.conversion.outputCheck.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            Intent started=Shadows.shadowOf(app).getNextStartedService();assertNotNull(started);
            assertEquals(List.of("content://input/1"),started.getStringArrayListExtra("inputs"));
            assertEquals(List.of("content://dirs/tree/one"),started.getStringArrayListExtra("folders"));
            assertEquals(List.of("custom-one.mp4"),started.getStringArrayListExtra("outputNames"));
            assertEquals(17,started.getIntExtra("crf",0));assertEquals(8,started.getIntExtra("preset",0));assertTrue(started.getBooleanExtra("onlySmaller",false));
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-memoryprotectiontest"></a>
## MemoryProtectionTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/MemoryProtectionTest.java) · 2 metodi · area originaria: Video.

**Contesto applicativo della classe:** MemoryProtection.required; SavedSession; ConversionSessionState.resetFile/begin; HomeConversion dashboard.

**Riferimenti produttivi:** [MemoryProtection](../../app/src/main/java/it/edo/codecprobe/MemoryProtection.java), [SavedSession](../../app/src/main/java/it/edo/codecprobe/SavedSession.java), [ConversionSessionState](../../app/src/main/java/it/edo/codecprobe/ConversionSessionState.java), [HomeConversion](../../app/src/main/java/it/edo/codecprobe/HomeConversion.java).

**Preparazione, file e simulazioni condivisi:** Dimensioni numeriche 4K/FHD/QHD, State e opzioni manuali, journal reale e UI simulata.

**Confini comuni dell’audit:** Non alloca UHD né misura memoria/OOM e non dimostra restrizioni applicate nel codec. 'Resume' è begin diretto, non servizio in lavoro.

**Metodi:** [detectsUhdOrientationAndCropWithoutChangingSmallerVideos](#test-f3e905d5fd492a9b) · [effectiveLabelSurvivesCheckpointThenResetsForNextFileAndResume](#test-45c15d0a20bce4d6)

<!-- test: MemoryProtectionTest.detectsUhdOrientationAndCropWithoutChangingSmallerVideos -->
<a id="test-f3e905d5fd492a9b"></a>
### `MemoryProtectionTest.detectsUhdOrientationAndCropWithoutChangingSmallerVideos`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Dimensioni numeriche 4K/FHD/QHD, State e opzioni manuali, journal reale e UI simulata.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/MemoryProtectionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `MemoryProtection.required`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Soglia riconosce3840x2160 e crop/orientamento, non1920/2560.

**Limiti:** Non alloca UHD né misura memoria/OOM e non dimostra restrizioni applicate nel codec. 'Resume' è begin diretto, non servizio in lavoro.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void detectsUhdOrientationAndCropWithoutChangingSmallerVideos(){
        assertTrue(MemoryProtection.required(3840,2160));assertTrue(MemoryProtection.required(2160,3840));
        assertTrue(MemoryProtection.required(3840,1600));
        assertFalse(MemoryProtection.required(1920,1080));assertFalse(MemoryProtection.required(1080,1920));
        assertFalse(MemoryProtection.required(2560,1440));
    }
```

</details>

<!-- /test -->

<!-- test: MemoryProtectionTest.effectiveLabelSurvivesCheckpointThenResetsForNextFileAndResume -->
<a id="test-45c15d0a20bce4d6"></a>
### `MemoryProtectionTest.effectiveLabelSurvivesCheckpointThenResetsForNextFileAndResume`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Dimensioni numeriche 4K/FHD/QHD, State e opzioni manuali, journal reale e UI simulata.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/MemoryProtectionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `SavedSession.save`, `SavedSession.load`, `saved.resumeRequest`, `home.conversion.renderConversionDashboard`, `home.screen.conversion.activeCpu.getText`, `home.screen.conversion.activeBframes.getText`, `home.screen.conversion.activeLookahead.getText`, `home.screen.conversion.activeTree.getText`, `ConversionService.State`, `ConversionSessionState.resetFile`, `ConversionSessionState.begin`, `SavedSession.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Flag protezione sopravvive checkpoint e label Limited, reset prossimo file/ripresa lo azzera senza perdere CPU richiesta/URI/elapsed.

**Limiti:** Non alloca UHD né misura memoria/OOM e non dimostra restrizioni applicate nel codec. 'Resume' è begin diretto, non servizio in lavoro.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void effectiveLabelSurvivesCheckpointThenResetsForNextFileAndResume()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Intent request=BatchRetryTest.queue(context);
        ConversionService.State state=SavedSessionTest.sample(request);state.memoryProtection=true;
        state.bframeCount=4;state.lookaheadCount=5;state.cuTree=0;
        try{
            SavedSession.save(context,request,state);SavedSession saved=SavedSession.load(context);
            assertTrue(saved.state.memoryProtection);assertEquals(2,saved.state.requestedCpu);
            assertEquals(2,saved.resumeRequest().getIntExtra("cpu",-1));
            var activity=Robolectric.buildActivity(MainActivity.class).setup();
            try{
                HomeController home=activity.get().home;
                saved.state.running=true;home.conversion.renderConversionDashboard(saved.state);
                assertEquals("Limited · protezione memoria 4K",home.screen.conversion.activeCpu.getText().toString());
                assertEquals("4",home.screen.conversion.activeBframes.getText().toString());
                assertEquals("5",home.screen.conversion.activeLookahead.getText().toString());
                assertEquals("OFF",home.screen.conversion.activeTree.getText().toString());
                ConversionService.State next=new ConversionService.State(saved.state);ConversionSessionState.resetFile(next);
                home.conversion.renderConversionDashboard(next);
                assertFalse(next.memoryProtection);assertEquals("Max",home.screen.conversion.activeCpu.getText().toString());
                assertEquals("in attesa",home.screen.conversion.activeLookahead.getText().toString());
                ConversionService.State resumed=ConversionSessionState.begin(saved.resumeRequest(),saved,saved.state.items,1000);
                assertFalse(resumed.memoryProtection);assertEquals(2,resumed.requestedCpu);
                assertEquals(saved.state.outputUris,resumed.outputUris);assertEquals(saved.state.elapsedBefore,resumed.elapsedBefore);
            }finally{activity.pause().stop().destroy();}
        }finally{SavedSession.clear(context);}
    }
```

</details>

<!-- /test -->


<a id="class-videocarouseltest"></a>
## VideoCarouselTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/VideoCarouselTest.java) · 2 metodi · area originaria: Video.

**Contesto applicativo della classe:** HomeSelection; VideoMetadataCache; DestinationPlan; HomeScreen.

**Riferimenti produttivi:** [HomeSelection](../../app/src/main/java/it/edo/codecprobe/HomeSelection.java), [VideoMetadataCache](../../app/src/main/java/it/edo/codecprobe/VideoMetadataCache.java), [DestinationPlan](../../app/src/main/java/it/edo/codecprobe/DestinationPlan.java), [HomeScreen](../../app/src/main/java/it/edo/codecprobe/HomeScreen.java).

**Preparazione, file e simulazioni condivisi:** Due cache Entry preinserite, Bundle URI fittizi; HorizontalScrollView/click Robolectric, virtual idle1s.

**Confini comuni dell’audit:** Nessun video letto, la promessa 'doesNotReadAgain' dedotta da loading=false/cache non contatore extractor. Nessun jank.

**Metodi:** [scrollingDoesNotSelectAndCachedSelectionDoesNotReadAgain](#test-4ca1c1aa3f8360b7) · [removingSelectedVideoKeepsTheRemainingFileAndItsDestination](#test-715002e9310c637b)

<!-- test: VideoCarouselTest.scrollingDoesNotSelectAndCachedSelectionDoesNotReadAgain -->
<a id="test-4ca1c1aa3f8360b7"></a>
### `VideoCarouselTest.scrollingDoesNotSelectAndCachedSelectionDoesNotReadAgain`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Due cache Entry preinserite, Bundle URI fittizi; HorizontalScrollView/click Robolectric, virtual idle1s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoCarouselTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `VideoMetadataCache.permissions`, `VideoMetadataCache.clear`, `VideoMetadataCache.epoch`, `VideoMetadataCache.put`, `VideoMetadataCache.Entry`, `InputInspector.Report`, `state.putStringArrayList`, `state.putString`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** scrollTo non seleziona; click secondo usa sintesi cache senza loading/dialog.

**Limiti:** Nessun video letto, la promessa 'doesNotReadAgain' dedotta da loading=false/cache non contatore extractor. Nessun jank.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void scrollingDoesNotSelectAndCachedSelectionDoesNotReadAgain()throws Exception{
        VideoMetadataCache.permissions("false:false");VideoMetadataCache.clear();long epoch=VideoMetadataCache.epoch();
        String first="content://test/video/one",second="content://test/video/two";
        VideoMetadataCache.put(first,new VideoMetadataCache.Entry("Primo video.mp4","Prima sintesi",new InputInspector.Report("primo","dettagli primo")),epoch);
        VideoMetadataCache.put(second,new VideoMetadataCache.Entry("Secondo video.mp4","Seconda sintesi",new InputInspector.Report("secondo","dettagli secondo")),epoch);
        Bundle state=new Bundle();state.putStringArrayList("inputs",new ArrayList<>(Arrays.asList(first,second)));state.putStringArrayList("names",new ArrayList<>(Arrays.asList("Primo video.mp4","Secondo video.mp4")));state.putString("input",first);
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create(state).start().resume().visible();
        MainActivity activity=controller.get();
        try{
            assertFalse((Boolean)activity.home.state.metadataLoading);
            assertEquals("Prima sintesi",((TextView)activity.home.screen.files.inputInfo).getText().toString());
            HorizontalScrollView carousel=(HorizontalScrollView)activity.home.screen.files.videoCarousel;carousel.scrollTo(200,0);
            assertEquals(0,activity.home.state.selectedVideoIndex);
            ArrayList<?> chips=(ArrayList<?>)activity.home.screen.files.videoChips;assertEquals(2,chips.size());
            ((Button)chips.get(1)).performClick();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals(1,activity.home.state.selectedVideoIndex);
            assertEquals("Seconda sintesi",((TextView)activity.home.screen.files.inputInfo).getText().toString());
            assertFalse((Boolean)activity.home.state.metadataLoading);assertNull(activity.home.selection.metadataDialog);
        }finally{controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }
```

</details>

<!-- /test -->

<!-- test: VideoCarouselTest.removingSelectedVideoKeepsTheRemainingFileAndItsDestination -->
<a id="test-715002e9310c637b"></a>
### `VideoCarouselTest.removingSelectedVideoKeepsTheRemainingFileAndItsDestination`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Due cache Entry preinserite, Bundle URI fittizi; HorizontalScrollView/click Robolectric, virtual idle1s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoCarouselTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `VideoMetadataCache.permissions`, `VideoMetadataCache.clear`, `VideoMetadataCache.epoch`, `VideoMetadataCache.put`, `VideoMetadataCache.Entry`, `InputInspector.Report`, `saved.putStringArrayList`, `saved.putString`, `home.state.destinationPlan.setFolder`, `home.screen.files.videoChips.get`, `home.screen.files.remove.performClick`, `home.state.destinationPlan.effective`, `home.state.input.toString`, `home.screen.files.videoChips.size`, `home.selection.removeVideo`, `home.state.selectedInputs.size`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Rimuovere secondo mantiene primo e relativa cartella, busy impedisce rimozione.

**Limiti:** Nessun video letto, la promessa 'doesNotReadAgain' dedotta da loading=false/cache non contatore extractor. Nessun jank.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void removingSelectedVideoKeepsTheRemainingFileAndItsDestination()throws Exception{
        VideoMetadataCache.permissions("false:false");VideoMetadataCache.clear();long epoch=VideoMetadataCache.epoch();
        ArrayList<String> inputs=new ArrayList<>(Arrays.asList("content://test/video/one","content://test/video/two"));ArrayList<String> names=new ArrayList<>(Arrays.asList("one.mp4","two.mp4"));
        for(int n=0;n<2;n++)VideoMetadataCache.put(inputs.get(n),new VideoMetadataCache.Entry(names.get(n),"Sintesi",new InputInspector.Report("info","dettagli")),epoch);
        Bundle saved=new Bundle();saved.putStringArrayList("inputs",inputs);saved.putStringArrayList("names",names);saved.putString("input",inputs.get(0));
        var controller=Robolectric.buildActivity(MainActivity.class).create(saved).start().resume().visible();try{
            HomeController home=controller.get().home;home.state.destinationPlan.setFolder(0,"content://dirs/tree/first");home.state.destinationPlan.setFolder(1,"content://dirs/tree/second");
            home.screen.files.videoChips.get(1).performClick();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));home.screen.files.remove.performClick();
            assertEquals(Arrays.asList(inputs.get(0)),home.state.selectedInputs);assertEquals("content://dirs/tree/first",home.state.destinationPlan.effective(0).folder);assertEquals(inputs.get(0),home.state.input.toString());assertEquals(1,home.screen.files.videoChips.size());
            home.state.busy=true;home.selection.removeVideo();assertEquals(1,home.state.selectedInputs.size());
        }finally{controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }
```

</details>

<!-- /test -->


<a id="class-videometadatacachetest"></a>
## VideoMetadataCacheTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/VideoMetadataCacheTest.java) · 5 metodi · area originaria: Video.

**Contesto applicativo della classe:** VideoMetadataCache; InputInspector.Report costruttori.

**Riferimenti produttivi:** [VideoMetadataCache](../../app/src/main/java/it/edo/codecprobe/VideoMetadataCache.java), [InputInspector](../../app/src/main/java/it/edo/codecprobe/InputInspector.java).

**Preparazione, file e simulazioni condivisi:** Entry/report sintetici in cache statica; token di permesso è una stringa.

**Confini comuni dell’audit:** Nessun provider o lettura media; il cambio permesso non è una revoca Android e non prova oscuramento GPS sul file.

**Metodi:** [reusedEntryDoesNotLoseDetails](#test-aaf7b57589af7402) · [permissionChangeInvalidatesGpsAndRejectsLateResult](#test-a29bdfa6e0fc88a9) · [explicitReloadRejectsOlderWorker](#test-970af288e44de57a) · [boundedCacheRetainsRecentlyViewedEntry](#test-63dd9b063cd9f71c) · [reportCopiesMapsAndPreservesErrorStatus](#test-ce88b6220e64bda0)

<!-- test: VideoMetadataCacheTest.reusedEntryDoesNotLoseDetails -->
<a id="test-aaf7b57589af7402"></a>
### `VideoMetadataCacheTest.reusedEntryDoesNotLoseDetails`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Entry/report sintetici in cache statica; token di permesso è una stringa.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoMetadataCacheTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `VideoMetadataCache.clear`, `VideoMetadataCache.permissions`, `VideoMetadataCache.put`, `VideoMetadataCache.epoch`, `VideoMetadataCache.get`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Get riusa la stessa Entry e conserva i dettagli.

**Limiti:** Nessun provider o lettura media; il cambio permesso non è una revoca Android e non prova oscuramento GPS sul file.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void reusedEntryDoesNotLoseDetails(){
        VideoMetadataCache.clear();VideoMetadataCache.permissions("read");
        VideoMetadataCache.Entry original=entry("primo.mp4");
        assertTrue(VideoMetadataCache.put("one",original,VideoMetadataCache.epoch()));
        assertSame(original,VideoMetadataCache.get("one"));
        assertTrue(VideoMetadataCache.get("one").report.details.contains("dettagli"));
    }
```

</details>

<!-- /test -->

<!-- test: VideoMetadataCacheTest.permissionChangeInvalidatesGpsAndRejectsLateResult -->
<a id="test-a29bdfa6e0fc88a9"></a>
### `VideoMetadataCacheTest.permissionChangeInvalidatesGpsAndRejectsLateResult`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Entry/report sintetici in cache statica; token di permesso è una stringa.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoMetadataCacheTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `VideoMetadataCache.clear`, `VideoMetadataCache.permissions`, `VideoMetadataCache.epoch`, `VideoMetadataCache.put`, `VideoMetadataCache.get`, `VideoMetadataCache.size`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Cambio token permessi invalida cache e rigetta put con vecchia epoca.

**Limiti:** Nessun provider o lettura media; il cambio permesso non è una revoca Android e non prova oscuramento GPS sul file.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void permissionChangeInvalidatesGpsAndRejectsLateResult(){
        VideoMetadataCache.clear();VideoMetadataCache.permissions("redacted");long old=VideoMetadataCache.epoch();
        VideoMetadataCache.put("one",entry("one"),old);VideoMetadataCache.permissions("original");
        assertNull(VideoMetadataCache.get("one"));
        assertFalse(VideoMetadataCache.put("one",entry("stale"),old));
        assertEquals(0,VideoMetadataCache.size());
    }
```

</details>

<!-- /test -->

<!-- test: VideoMetadataCacheTest.explicitReloadRejectsOlderWorker -->
<a id="test-970af288e44de57a"></a>
### `VideoMetadataCacheTest.explicitReloadRejectsOlderWorker`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Entry/report sintetici in cache statica; token di permesso è una stringa.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoMetadataCacheTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `VideoMetadataCache.clear`, `VideoMetadataCache.epoch`, `VideoMetadataCache.put`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Clear/rilettura esplicita respinge un risultato vecchio.

**Limiti:** Nessun provider o lettura media; il cambio permesso non è una revoca Android e non prova oscuramento GPS sul file.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void explicitReloadRejectsOlderWorker(){
        VideoMetadataCache.clear();long old=VideoMetadataCache.epoch();VideoMetadataCache.clear();
        assertFalse(VideoMetadataCache.put("one",entry("stale"),old));
    }
```

</details>

<!-- /test -->

<!-- test: VideoMetadataCacheTest.boundedCacheRetainsRecentlyViewedEntry -->
<a id="test-63dd9b063cd9f71c"></a>
### `VideoMetadataCacheTest.boundedCacheRetainsRecentlyViewedEntry`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Entry/report sintetici in cache statica; token di permesso è una stringa.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoMetadataCacheTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `VideoMetadataCache.clear`, `VideoMetadataCache.epoch`, `VideoMetadataCache.put`, `VideoMetadataCache.get`, `VideoMetadataCache.size`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Capacità 100 e promozione LRU: v0 resta, v1 esce.

**Limiti:** Nessun provider o lettura media; il cambio permesso non è una revoca Android e non prova oscuramento GPS sul file.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void boundedCacheRetainsRecentlyViewedEntry(){
        VideoMetadataCache.clear();long epoch=VideoMetadataCache.epoch();
        for(int n=0;n<100;n++)VideoMetadataCache.put("v"+n,entry("name"),epoch);
        VideoMetadataCache.get("v0");VideoMetadataCache.put("new",entry("name"),epoch);
        assertEquals(100,VideoMetadataCache.size());assertNotNull(VideoMetadataCache.get("v0"));assertNull(VideoMetadataCache.get("v1"));
    }
```

</details>

<!-- /test -->

<!-- test: VideoMetadataCacheTest.reportCopiesMapsAndPreservesErrorStatus -->
<a id="test-ce88b6220e64bda0"></a>
### `VideoMetadataCacheTest.reportCopiesMapsAndPreservesErrorStatus`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Entry/report sintetici in cache statica; token di permesso è una stringa.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoMetadataCacheTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `InputInspector.Report`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Report copia le mappe di errore ed evita aliasing con sorgente.

**Limiti:** Nessun provider o lettura media; il cambio permesso non è una revoca Android e non prova oscuramento GPS sul file.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void reportCopiesMapsAndPreservesErrorStatus(){
        LinkedHashMap<String,String> source=new LinkedHashMap<>();source.put("VIDEO","errore");
        InputInspector.Report report=new InputInspector.Report(source,Collections.emptyMap(),true);
        source.clear();assertEquals("errore",report.summarySections.get("VIDEO"));assertTrue(report.readError);
    }
```

</details>

<!-- /test -->


<a id="class-videosettingsparitytest"></a>
## VideoSettingsParityTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/VideoSettingsParityTest.java) · 3 metodi · area originaria: Video.

**Contesto applicativo della classe:** SettingsActivity; AppSettings.

**Riferimenti produttivi:** [SettingsActivity](../../app/src/main/java/it/edo/codecprobe/SettingsActivity.java), [AppSettings](../../app/src/main/java/it/edo/codecprobe/AppSettings.java).

**Preparazione, file e simulazioni condivisi:** Activity/SharedPreferences Robolectric, campi privati reflection, Intent opzioni ePNG.

**Confini comuni dell’audit:** Non prova opzioni nell'encoder, picker cartelle reale o parità completa Audio/Foto; screenshot no golden.

**Metodi:** [unsavedVideoSettingsSurviveRecreationAndBackDiscardsThem](#test-6b46cfec5f208989) · [copyingCurrentIncludesFolderAndCommonOptionsAndOnlySaveWritesDefaults](#test-36789ab8e9dff430) · [settingsFrameRender](#test-1cb96ce702960e2d)

<!-- test: VideoSettingsParityTest.unsavedVideoSettingsSurviveRecreationAndBackDiscardsThem -->
<a id="test-6b46cfec5f208989"></a>
### `VideoSettingsParityTest.unsavedVideoSettingsSurviveRecreationAndBackDiscardsThem`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/SharedPreferences Robolectric, campi privati reflection, Intent opzioni ePNG.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoSettingsParityTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `field(…)`, `button(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Form31/addition custom e onlySmallerfalse sopravvive recreate; Back lascia default23/true.

**Limiti:** Non prova opzioni nell'encoder, picker cartelle reale o parità completa Audio/Foto; screenshot no golden.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void unsavedVideoSettingsSurviveRecreationAndBackDiscardsThem()throws Exception{
        var controller=Robolectric.buildActivity(SettingsActivity.class).setup();try{
            ((SeekBar)field(controller.get(),"crf")).setProgress(31);((CheckBox)field(controller.get(),"onlySmaller")).setChecked(false);((EditText)field(controller.get(),"addition")).setText("custom-");
            controller.recreate();assertEquals(31,((SeekBar)field(controller.get(),"crf")).getProgress());assertFalse(((CheckBox)field(controller.get(),"onlySmaller")).isChecked());assertEquals("custom-",((EditText)field(controller.get(),"addition")).getText().toString());
            button(controller.get().getWindow().getDecorView(),"← Torna alla conversione").performClick();assertEquals(23,new AppSettings(controller.get()).crf);assertTrue(new AppSettings(controller.get()).onlySmaller);
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: VideoSettingsParityTest.copyingCurrentIncludesFolderAndCommonOptionsAndOnlySaveWritesDefaults -->
<a id="test-36789ab8e9dff430"></a>
### `VideoSettingsParityTest.copyingCurrentIncludesFolderAndCommonOptionsAndOnlySaveWritesDefaults`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/SharedPreferences Robolectric, campi privati reflection, Intent opzioni ePNG.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoSettingsParityTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `button(…)`, `field(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Copia corrente include folder e flag ma solo Save persiste CRF29/cartellaB/onlySmallerfalse/keepScreenOn.

**Limiti:** Non prova opzioni nell'encoder, picker cartelle reale o parità completa Audio/Foto; screenshot no golden.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void copyingCurrentIncludesFolderAndCommonOptionsAndOnlySaveWritesDefaults()throws Exception{
        Intent current=new Intent().putExtra("crf",29).putExtra("preset",2).putExtra("cpu",0).putExtra("bframes",false).putExtra("lookahead",false).putExtra("folder",MediaDestinationsTest.B).putExtra("onlySmaller",false).putExtra("keepScreenOn",true);
        var controller=Robolectric.buildActivity(SettingsActivity.class,current).setup();try{
            button(controller.get().getWindow().getDecorView(),"Usa la configurazione della schermata conversione").performClick();
            assertEquals(29,((SeekBar)field(controller.get(),"crf")).getProgress());assertEquals(MediaDestinationsTest.B,field(controller.get(),"defaultFolder"));assertTrue(((CheckBox)field(controller.get(),"keepScreenOn")).isChecked());assertEquals(23,new AppSettings(controller.get()).crf);
            button(controller.get().getWindow().getDecorView(),"Salva impostazioni").performClick();AppSettings saved=new AppSettings(controller.get());assertEquals(29,saved.crf);assertEquals(MediaDestinationsTest.B,saved.defaultFolder);assertFalse(saved.onlySmaller);assertTrue(saved.keepScreenOn);
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: VideoSettingsParityTest.settingsFrameRender -->
<a id="test-1cb96ce702960e2d"></a>
### `VideoSettingsParityTest.settingsFrameRender`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/SharedPreferences Robolectric, campi privati reflection, Intent opzioni ePNG.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoSettingsParityTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `field(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** PNG pagina settings Video, solo compressione riuscita.

**Limiti:** Non prova opzioni nell'encoder, picker cartelle reale o parità completa Audio/Foto; screenshot no golden.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void settingsFrameRender()throws Exception{
        var controller=Robolectric.buildActivity(SettingsActivity.class).setup();try{
            View page=field(controller.get(),"page");AppUi ui=new AppUi(controller.get());int width=ui.dp(360);page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());page.getViewTreeObserver().dispatchOnPreDraw();float density=controller.get().getResources().getDisplayMetrics().density;
            Bitmap bitmap=Bitmap.createBitmap(360,(int)Math.ceil(page.getMeasuredHeight()/density),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(ui.bg);canvas.scale(1/density,1/density);page.draw(canvas);File dir=new File("build/reports/ui");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,"settings-video.png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->
