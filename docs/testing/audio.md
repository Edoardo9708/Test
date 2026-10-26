# Audio

[Guida](README.md) · [Indice completo](index.md) · [Revisioni](review-needed.md) · [Esecuzioni](esecuzioni.md)

Descrizioni statiche trasferite dall’audit: non attestano nuove esecuzioni. Le fixture condivise sono descritte a livello di classe; dati specifici, ordine delle operazioni e assert sono nel corpo integrale di ogni metodo. Gli helper richiamati e gli eventuali `@Before`/`@After` sono consultabili nel sorgente collegato. Lo script di aggiornamento non riscrive questa pagina.

## Classi

- [AudioConversionTest](#class-audioconversiontest)
- [AudioFlowTest](#class-audioflowtest)
- [AudioPacketCopierTest](#class-audiopacketcopiertest)
- [AudioPcmTest](#class-audiopcmtest)
- [AudioSettingsTest](#class-audiosettingstest)
- [AudioWorkspaceTest](#class-audioworkspacetest)

<a id="class-audioconversiontest"></a>
## AudioConversionTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java) · 6 metodi · area originaria: Audio.

**Contesto applicativo della classe:** AudioOutput.name/unique; MediaConversionGate; AudioLog; AudioController; AudioConversionService.onStartCommand.

**Riferimenti produttivi:** [AudioOutput](../../app/src/main/java/it/edo/codecprobe/AudioOutput.java), [MediaConversionGate](../../app/src/main/java/it/edo/codecprobe/MediaConversionGate.java), [AudioLog](../../app/src/main/java/it/edo/codecprobe/AudioLog.java), [AudioController](../../app/src/main/java/it/edo/codecprobe/AudioController.java), [AudioConversionService](../../app/src/main/java/it/edo/codecprobe/AudioConversionService.java).

**Preparazione, file e simulazioni condivisi:** Robolectric; nei test del servizio stato statico sostituito con reflection, URI fittizi; nel log vero file privato del Context simulato.

**Confini comuni dell’audit:** Nessun FLAC letto o Opus codificato. Lease confrontata direttamente, non due conversioni concorrenti.

**Metodi:** [outputNamesNeverOverwriteAndKeepOriginalBase](#test-0647585be2ea6c97) · [audioAndVideoUseTheSameLeaseUntilCleanup](#test-76ba276495303ab0) · [audioLogIsBoundedAndPreservesReadableSteps](#test-695d9ec7f96c1f14) · [resetClearsAudioDraftWithoutChangingVideoOrSettings](#test-94eeacc3820cbe96) · [staleCancelCannotChangeAnotherAudioRun](#test-e7cc410bfcea8c84) · [busySharedLeaseRejectsAudioBeforeLoadingNativeCode](#test-db2ab978290f5e9e)

<!-- test: AudioConversionTest.outputNamesNeverOverwriteAndKeepOriginalBase -->
<a id="test-0647585be2ea6c97"></a>
### `AudioConversionTest.outputNamesNeverOverwriteAndKeepOriginalBase`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric; nei test del servizio stato statico sostituito con reflection, URI fittizi; nel log vero file privato del Context simulato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioOutput.name`, `AudioOutput.unique`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Nome .opus e suffissi tramite Set, non vera assenza di overwrite sul provider.

**Limiti:** Nessun FLAC letto o Opus codificato. Lease confrontata direttamente, non due conversioni concorrenti.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void outputNamesNeverOverwriteAndKeepOriginalBase(){
        assertEquals("Song.opus",AudioOutput.name("Song.FLAC"));
        assertEquals("a_b.opus",AudioOutput.name("a/b.flac"));
        assertEquals("Song (3).opus",AudioOutput.unique("Song.opus",new HashSet<>(Arrays.asList("Song.opus","Song (2).opus"))));
        assertEquals("Song.opus",AudioOutput.unique("Song.opus",Collections.emptySet()));
    }
```

</details>

<!-- /test -->

<!-- test: AudioConversionTest.audioAndVideoUseTheSameLeaseUntilCleanup -->
<a id="test-76ba276495303ab0"></a>
### `AudioConversionTest.audioAndVideoUseTheSameLeaseUntilCleanup`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric; nei test del servizio stato statico sostituito con reflection, URI fittizi; nel log vero file privato del Context simulato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.class.getDeclaredField`, `MediaConversionGate.INSTANCE.acquire`, `MediaConversionGate.INSTANCE.release`, `MediaConversionGate.INSTANCE.owns`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Identità del campo privato RUNS e acquisizione/rilascio della lease; forte accoppiamento interno.

**Limiti:** Nessun FLAC letto o Opus codificato. Lease confrontata direttamente, non due conversioni concorrenti.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void audioAndVideoUseTheSameLeaseUntilCleanup()throws Exception{
        java.lang.reflect.Field field=ConversionService.class.getDeclaredField("RUNS");field.setAccessible(true);
        assertSame(MediaConversionGate.INSTANCE,field.get(null));
        long audio=MediaConversionGate.INSTANCE.acquire();assertNotEquals(0,audio);
        try{assertEquals(0,MediaConversionGate.INSTANCE.acquire());MediaConversionGate.INSTANCE.release(audio+1);assertTrue(MediaConversionGate.INSTANCE.owns(audio));}
        finally{MediaConversionGate.INSTANCE.release(audio);}
        long video=MediaConversionGate.INSTANCE.acquire();assertNotEquals(0,video);MediaConversionGate.INSTANCE.release(video);
    }
```

</details>

<!-- /test -->

<!-- test: AudioConversionTest.audioLogIsBoundedAndPreservesReadableSteps -->
<a id="test-695d9ec7f96c1f14"></a>
### `AudioConversionTest.audioLogIsBoundedAndPreservesReadableSteps`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric; nei test del servizio stato statico sostituito con reflection, URI fittizi; nel log vero file privato del Context simulato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `log.write`, `log.tail`, `AudioLog.file`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Scrive circa 3 MB di testo nel log reale e controlla limite/tail/marcatore.

**Limiti:** Nessun FLAC letto o Opus codificato. Lease confrontata direttamente, non due conversioni concorrenti.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void audioLogIsBoundedAndPreservesReadableSteps()throws Exception{
        Context c=RuntimeEnvironment.getApplication();
        try(AudioLog log=new AudioLog(c)){
            log.write("*** INIZIO CODIFICA ***\n1. Leggo FLAC\n2. Codifico Opus");
            assertTrue(log.tail().contains("\n1. Leggo FLAC\n2. Codifico Opus"));
            char[] fill=new char[10000];Arrays.fill(fill,'x');for(int n=0;n<300;n++)log.write(new String(fill));
            assertTrue(log.tail().length()<130000);
        }
        assertTrue(AudioLog.file(c).length()<AudioLog.LIMIT+200);
        assertTrue(new String(Files.readAllBytes(AudioLog.file(c).toPath()),StandardCharsets.UTF_8).contains("LIMITE LOG RAGGIUNTO"));
        assertTrue(AudioLog.file(c).delete());
    }
```

</details>

<!-- /test -->

<!-- test: AudioConversionTest.resetClearsAudioDraftWithoutChangingVideoOrSettings -->
<a id="test-94eeacc3820cbe96"></a>
### `AudioConversionTest.resetClearsAudioDraftWithoutChangingVideoOrSettings`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric; nei test del servizio stato statico sostituito con reflection, URI fittizi; nel log vero file privato del Context simulato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioConversionService.clear`, `audio.state.inputs.add`, `audio.state.names.add`, `audio.render`, `audio.screen.start.isEnabled`, `audio.screen.cancel.isEnabled`, `audio.screen.clear.performClick`, `audio.state.names.isEmpty`, `audio.screen.parameters.getText`, `audio.screen.status.getText`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Clear pulisce bozza/pulsanti/etichette audio e conserva cartella; malgrado il nome, non confronta stato Video o impostazioni salvate.

**Limiti:** Nessun FLAC letto o Opus codificato. Lease confrontata direttamente, non due conversioni concorrenti.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void resetClearsAudioDraftWithoutChangingVideoOrSettings(){
        Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("audio-workspace",0).edit().clear().commit();AudioConversionService.clear();
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            AudioController audio=controller.get().audio;audio.state.inputs.add("content://songs/a");audio.state.names.add("a.flac");audio.state.folder="content://music/tree/chosen";audio.render();
            assertFalse(audio.screen.start.isEnabled());assertFalse(audio.screen.cancel.isEnabled());
            audio.screen.clear.performClick();assertTrue(audio.state.names.isEmpty());assertEquals("content://music/tree/chosen",audio.state.folder);
            assertTrue(audio.screen.parameters.getText().toString().equals("—"));
            assertTrue(audio.screen.status.getText().toString().contains("Opus"));
        }finally{controller.pause().stop().destroy();c.getSharedPreferences("audio-workspace",0).edit().clear().commit();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioConversionTest.staleCancelCannotChangeAnotherAudioRun -->
<a id="test-e7cc410bfcea8c84"></a>
### `AudioConversionTest.staleCancelCannotChangeAnotherAudioRun`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric; nei test del servizio stato statico sostituito con reflection, URI fittizi; nel log vero file privato del Context simulato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioConversionService.class.getDeclaredField`, `service.get`, `AudioConversionService.snapshot`, `service.destroy`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Intent CANCEL con startedAt vecchio non porta cancelling su uno stato iniettato: manca il controllo positivo del comando corrente.

**Limiti:** Nessun FLAC letto o Opus codificato. Lease confrontata direttamente, non due conversioni concorrenti.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void staleCancelCannotChangeAnotherAudioRun()throws Exception{
        java.lang.reflect.Field field=AudioConversionService.class.getDeclaredField("state");field.setAccessible(true);
        AudioRunState previous=(AudioRunState)field.get(null);AudioRunState current=new AudioRunState();current.running=true;current.startedAt=22;field.set(null,current);
        var service=Robolectric.buildService(AudioConversionService.class).create();
        try{
            service.get().onStartCommand(new Intent().setAction(AudioConversionService.CANCEL).putExtra("startedAt",21L),0,1);
            assertFalse(AudioConversionService.snapshot().cancelling);
        }finally{service.destroy();field.set(null,previous);}
    }
```

</details>

<!-- /test -->

<!-- test: AudioConversionTest.busySharedLeaseRejectsAudioBeforeLoadingNativeCode -->
<a id="test-db2ab978290f5e9e"></a>
### `AudioConversionTest.busySharedLeaseRejectsAudioBeforeLoadingNativeCode`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric; nei test del servizio stato statico sostituito con reflection, URI fittizi; nel log vero file privato del Context simulato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioConversionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `MediaConversionGate.INSTANCE.acquire`, `service.get`, `AudioConversionService.snapshot`, `MediaConversionGate.INSTANCE.owns`, `MediaConversionGate.INSTANCE.release`, `service.destroy`, `AudioConversionService.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Lease già occupata: START non avvia il servizio audio né carica JNI e conserva il proprietario.

**Limiti:** Nessun FLAC letto o Opus codificato. Lease confrontata direttamente, non due conversioni concorrenti.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void busySharedLeaseRejectsAudioBeforeLoadingNativeCode(){
        var service=Robolectric.buildService(AudioConversionService.class).create();Context c=RuntimeEnvironment.getApplication();
        long lease=MediaConversionGate.INSTANCE.acquire();assertNotEquals(0,lease);
        try{
            Intent request=new Intent(c,AudioConversionService.class).setAction(AudioConversionService.START)
                .putStringArrayListExtra("inputs",new ArrayList<>(List.of("content://songs/a")))
                .putStringArrayListExtra("names",new ArrayList<>(List.of("a.flac")))
                .putExtra("folder","content://music/tree/chosen").putExtra("bitrate",128);
            service.get().onStartCommand(request,0,1);
            assertFalse(AudioConversionService.snapshot().running);assertTrue(AudioConversionService.snapshot().message.contains("già attiva"));assertTrue(MediaConversionGate.INSTANCE.owns(lease));
        }finally{MediaConversionGate.INSTANCE.release(lease);service.destroy();AudioConversionService.clear();Shadows.shadowOf(Looper.getMainLooper()).idle();}
    }
```

</details>

<!-- /test -->


<a id="class-audioflowtest"></a>
## AudioFlowTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java) · 9 metodi · area originaria: Audio.

**Contesto applicativo della classe:** AudioController/Selection/Actions; AudioDraftStore; AudioRunState; AudioOutput.unique.

**Riferimenti produttivi:** [AudioController](../../app/src/main/java/it/edo/codecprobe/AudioController.java), [AudioDraftStore](../../app/src/main/java/it/edo/codecprobe/AudioDraftStore.java), [AudioRunState](../../app/src/main/java/it/edo/codecprobe/AudioRunState.java), [AudioOutput](../../app/src/main/java/it/edo/codecprobe/AudioOutput.java).

**Preparazione, file e simulazioni condivisi:** Provider Names fittizio restituisce nomi/12345 byte/directory scrivibile, senza audio. Stato servizi e risultati iniettati via reflection; MainLooper pilotato.

**Confini comuni dell’audit:** Intent intercettati, non servizio/encoder avviato. Helper ready usa polling con Thread.sleep(5) e timeout 5 s; vista dei report legata a childAt.

**Metodi:** [everySelectedTrackCanBeInspectedAndRemoved](#test-a9a1b0b19c8eca24) · [invalidNewLyricsSelectionCannotLeaveOldLyricsSilentlySelected](#test-c94db269b199ccb6) · [filenameReadInterruptedByRecreationIsRestarted](#test-83e74e1254ef6dbb) · [preflightFreezesParametersAndPreventsDoubleStart](#test-b768715436a74cf8) · [ambiguousLyricsAreReportedBeforeStartingTheEncoder](#test-1d7e82668ee85eae) · [newRunWithSameResultCountReplacesThePreviousReport](#test-4006d083f239e0c2) · [cancelledAndUnsavedTracksAreNotPresentedAsFailuresAndDoNotRetry](#test-3d5f8252261b869e) · [screenFlagIsScopedToVisibleRunningAudioAndResetKeepsSettings](#test-0b4bd754c0b68935) · [progressReservesVerificationAndReportsIncompleteQueue](#test-92d4ce4da3ab27f8)

<!-- test: AudioFlowTest.everySelectedTrackCanBeInspectedAndRemoved -->
<a id="test-a9a1b0b19c8eca24"></a>
### `AudioFlowTest.everySelectedTrackCanBeInspectedAndRemoved`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Names fittizio restituisce nomi/12345 byte/directory scrivibile, senza audio. Stato servizi e risultati iniettati via reflection; MainLooper pilotato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `audio.state.inputs.add`, `audio.state.names.add`, `audio.render`, `audio.screen.navigator.getChildCount`, `audio.screen.navigator.getChildAt`, `audio.screen.names.getText`, `audio.screen.remove.performClick`, `audio.state.inputs.size`, `AudioDraftStore.load`, `audio.state.names.contains`, `audio.screen.log.getVisibility`, `audio.screen.sections.bodies.containsKey`, `audio.screen.settings.performClick`, `SettingsActivity.class.getName`, `audio.screen.settings.getText`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** 13 brani artificiali selezionabili/rimovibili, indice salvato e apertura impostazioni via Intent.

**Limiti:** Intent intercettati, non servizio/encoder avviato. Helper ready usa polling con Thread.sleep(5) e timeout 5 s; vista dei report legata a childAt.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void everySelectedTrackCanBeInspectedAndRemoved(){
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;for(int n=0;n<13;n++){audio.state.inputs.add("content://songs/"+n+".flac");audio.state.names.add(n+".flac");}audio.render();
            assertEquals(13,audio.screen.navigator.getChildCount());audio.screen.navigator.getChildAt(12).performClick();Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));assertEquals("12.flac",audio.screen.names.getText().toString());
            audio.screen.remove.performClick();assertEquals(12,audio.state.inputs.size());assertEquals("11.flac",audio.screen.names.getText().toString());
            assertEquals(11,AudioDraftStore.load(context).selectedIndex);assertFalse(audio.state.names.contains("12.flac"));
            assertEquals(View.GONE,audio.screen.log.getVisibility());assertTrue(audio.screen.sections.bodies.containsKey(AudioScreen.DIAGNOSTICS));
            audio.screen.settings.performClick();Intent settings=Shadows.shadowOf(controller.get()).getNextStartedActivity();assertEquals(SettingsActivity.class.getName(),settings.getComponent().getClassName());assertTrue(settings.getBooleanExtra(AudioSettingsScreen.AUDIO,false));assertEquals("Impostazioni",audio.screen.settings.getText().toString());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioFlowTest.invalidNewLyricsSelectionCannotLeaveOldLyricsSilentlySelected -->
<a id="test-c94db269b199ccb6"></a>
### `AudioFlowTest.invalidNewLyricsSelectionCannotLeaveOldLyricsSilentlySelected`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Names fittizio restituisce nomi/12345 byte/directory scrivibile, senza audio. Stato servizi e risultati iniettati via reflection; MainLooper pilotato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `audio.state.lyricsInputs.add`, `audio.state.lyricsNames.add`, `audio.selection.result`, `audio.state.lyricsInputs.isEmpty`, `AudioDraftStore.load`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Selezione .txt invalida azzera vecchi testi LRC anche nella bozza.

**Limiti:** Intent intercettati, non servizio/encoder avviato. Helper ready usa polling con Thread.sleep(5) e timeout 5 s; vista dei report legata a childAt.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void invalidNewLyricsSelectionCannotLeaveOldLyricsSilentlySelected()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;audio.state.lyricsInputs.add("content://songs/old.lrc");audio.state.lyricsNames.add("old.lrc");
            audio.selection.result(AudioSelection.LYRICS,Activity.RESULT_OK,new Intent().setData(Uri.parse("content://songs/not-lyrics.txt")));await(audio);
            assertFalse(audio.state.readingLyrics);assertTrue(audio.state.lyricsInputs.isEmpty());assertTrue(AudioDraftStore.load(context).lyricsInputs.isEmpty());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioFlowTest.filenameReadInterruptedByRecreationIsRestarted -->
<a id="test-83e74e1254ef6dbb"></a>
### `AudioFlowTest.filenameReadInterruptedByRecreationIsRestarted`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Names fittizio restituisce nomi/12345 byte/directory scrivibile, senza audio. Stato servizi e risultati iniettati via reflection; MainLooper pilotato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `draft.inputs.add`, `draft.names.add`, `AudioDraftStore.save`, `AudioDraftStore.load`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Bozza interrotta durante lettura nomi viene ripresa e aggiorna real.flac.

**Limiti:** Intent intercettati, non servizio/encoder avviato. Helper ready usa polling con Thread.sleep(5) e timeout 5 s; vista dei report legata a childAt.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void filenameReadInterruptedByRecreationIsRestarted()throws Exception{
        AudioState draft=new AudioState();draft.inputs.add("content://songs/real.flac");draft.names.add("Brano 1");draft.readingNames=true;AudioDraftStore.save(context,draft);
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;await(audio);assertEquals(List.of("real.flac"),audio.state.names);assertFalse(audio.state.readingNames);assertFalse(AudioDraftStore.load(context).readingNames);
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioFlowTest.preflightFreezesParametersAndPreventsDoubleStart -->
<a id="test-b768715436a74cf8"></a>
### `AudioFlowTest.preflightFreezesParametersAndPreventsDoubleStart`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Names fittizio restituisce nomi/12345 byte/directory scrivibile, senza audio. Stato servizi e risultati iniettati via reflection; MainLooper pilotato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `audio.actions.start`, `audio.screen.start.isEnabled`, `audio.screen.pick.isEnabled`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Preflight congela bitrate 160 e onlySmaller, disabilita comandi e impedisce doppio lancio anche dopo modifica UI a 96.

**Limiti:** Intent intercettati, non servizio/encoder avviato. Helper ready usa polling con Thread.sleep(5) e timeout 5 s; vista dei report legata a childAt.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void preflightFreezesParametersAndPreventsDoubleStart()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;chosen(audio,"music.flac");audio.state.bitrate=160;audio.state.onlySmaller=true;
            audio.actions.start();assertTrue(audio.state.preparing);assertFalse(audio.screen.start.isEnabled());assertFalse(audio.screen.pick.isEnabled());
            audio.state.bitrate=96;audio.actions.start();await(audio);
            Intent launch=Shadows.shadowOf(controller.get()).getNextStartedService();assertNotNull(launch);assertEquals(160,launch.getIntExtra("bitrate",0));assertTrue(launch.getBooleanExtra("onlySmaller",false));
            assertEquals(List.of("music.flac"),launch.getStringArrayListExtra("names"));audio.actions.start();assertNull(Shadows.shadowOf(controller.get()).getNextStartedService());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioFlowTest.ambiguousLyricsAreReportedBeforeStartingTheEncoder -->
<a id="test-1d7e82668ee85eae"></a>
### `AudioFlowTest.ambiguousLyricsAreReportedBeforeStartingTheEncoder`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Names fittizio restituisce nomi/12345 byte/directory scrivibile, senza audio. Stato servizi e risultati iniettati via reflection; MainLooper pilotato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `audio.state.lyricsInputs.addAll`, `audio.state.lyricsNames.addAll`, `audio.render`, `audio.screen.lyrics.getText`, `audio.actions.start`, `audio.screen.status.getText`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Due LRC omonimi ambigui impediscono START e spiegano l'errore prima dell'encoder.

**Limiti:** Intent intercettati, non servizio/encoder avviato. Helper ready usa polling con Thread.sleep(5) e timeout 5 s; vista dei report legata a childAt.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void ambiguousLyricsAreReportedBeforeStartingTheEncoder()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;chosen(audio,"music.flac");audio.state.lyricsInputs.addAll(List.of("content://one/a","content://two/b"));audio.state.lyricsNames.addAll(List.of("music.lrc","music.lrc"));audio.render();
            assertTrue(audio.screen.lyrics.getText().toString().contains("ambigui"));audio.actions.start();await(audio);
            assertFalse(audio.state.preparing);assertNull(Shadows.shadowOf(controller.get()).getNextStartedService());assertTrue(audio.screen.status.getText().toString().contains("Più testi"));
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioFlowTest.newRunWithSameResultCountReplacesThePreviousReport -->
<a id="test-4006d083f239e0c2"></a>
### `AudioFlowTest.newRunWithSameResultCountReplacesThePreviousReport`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Names fittizio restituisce nomi/12345 byte/directory scrivibile, senza audio. Stato servizi e risultati iniettati via reflection; MainLooper pilotato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioRunState.Result`, `audio.renderRun`, `audio.screen.report.getChildAt`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Nuova sessione con stesso numero risultati sostituisce la riga old con new.

**Limiti:** Intent intercettati, non servizio/encoder avviato. Helper ready usa polling con Thread.sleep(5) e timeout 5 s; vista dei report legata a childAt.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void newRunWithSameResultCountReplacesThePreviousReport()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;AudioRunState first=new AudioRunState();first.total=1;first.results.add(new AudioRunState.Result("old.flac","content://out/old",100,50,null));audioState.set(null,first);audio.renderRun();
            AudioRunState next=new AudioRunState();next.total=1;next.results.add(new AudioRunState.Result("new.flac","content://out/new",200,50,null));audioState.set(null,next);audio.renderRun();
            android.widget.LinearLayout row=(android.widget.LinearLayout)audio.screen.report.getChildAt(0);assertTrue(((android.widget.TextView)row.getChildAt(0)).getText().toString().contains("new.flac"));
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioFlowTest.cancelledAndUnsavedTracksAreNotPresentedAsFailuresAndDoNotRetry -->
<a id="test-3d5f8252261b869e"></a>
### `AudioFlowTest.cancelledAndUnsavedTracksAreNotPresentedAsFailuresAndDoNotRetry`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Names fittizio restituisce nomi/12345 byte/directory scrivibile, senza audio. Stato servizi e risultati iniettati via reflection; MainLooper pilotato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `run.results.add`, `AudioRunState.Result`, `audio.renderRun`, `audio.screen.summary.getText`, `audio.screen.totalProgress.getText`, `audio.actions.retry`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Esiti SAVED/CANCELLED/NOT_SAVED/FAILED preinseriti: solo FAILED entra nell'Intent retry; non produce quegli esiti nel motore.

**Limiti:** Intent intercettati, non servizio/encoder avviato. Helper ready usa polling con Thread.sleep(5) e timeout 5 s; vista dei report legata a childAt.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void cancelledAndUnsavedTracksAreNotPresentedAsFailuresAndDoNotRetry()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioRunState run=new AudioRunState();run.total=4;
            run.results.add(new AudioRunState.Result("ok.flac","content://songs/ok.flac","content://out/ok",1000,400,null,AudioRunState.Outcome.SAVED));
            run.results.add(new AudioRunState.Result("cancel.flac","content://songs/cancel.flac",null,-1,0,"Annullato",AudioRunState.Outcome.CANCELLED));
            run.results.add(new AudioRunState.Result("large.flac","content://songs/large.flac",null,100,120,"Nessun risparmio",AudioRunState.Outcome.NOT_SAVED));
            run.results.add(new AudioRunState.Result("fail.flac","content://songs/fail.flac",null,-1,0,"Errore",AudioRunState.Outcome.FAILED));audioState.set(null,run);
            AudioController audio=controller.get().audio;audio.state.folder="content://dirs/tree/music";MediaDestinationsTest.grant(audio.activity,audio.state.folder);audio.renderRun();assertTrue(audio.screen.summary.getText().toString().contains("falliti 1"));assertEquals("100%",audio.screen.totalProgress.getText().toString());
            audio.actions.retry();await(audio);Intent launch=Shadows.shadowOf(controller.get()).getNextStartedService();assertNotNull(launch);assertEquals(List.of("content://songs/fail.flac"),launch.getStringArrayListExtra("inputs"));
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioFlowTest.screenFlagIsScopedToVisibleRunningAudioAndResetKeepsSettings -->
<a id="test-0b4bd754c0b68935"></a>
### `AudioFlowTest.screenFlagIsScopedToVisibleRunningAudioAndResetKeepsSettings`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Names fittizio restituisce nomi/12345 byte/directory scrivibile, senza audio. Stato servizi e risultati iniettati via reflection; MainLooper pilotato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `audio.screen.keepScreenOn.setChecked`, `audio.screen.bitrate.setProgress`, `audio.renderRun`, `audio.screen.clear.performClick`, `audio.state.inputs.isEmpty`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Flag schermo attivo solo con pagina visibile e run iniettata; reset conserva impostazioni correnti.

**Limiti:** Intent intercettati, non servizio/encoder avviato. Helper ready usa polling con Thread.sleep(5) e timeout 5 s; vista dei report legata a childAt.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void screenFlagIsScopedToVisibleRunningAudioAndResetKeepsSettings()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=controller.get().audio;chosen(audio,"music.flac");audio.screen.keepScreenOn.setChecked(true);audio.screen.bitrate.setProgress(12);
            AudioRunState run=new AudioRunState();run.running=true;run.total=1;audioState.set(null,run);audio.renderRun();assertTrue((controller.get().getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)!=0);
            controller.pause();assertEquals(0,controller.get().getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            run.running=false;audioState.set(null,run);controller.resume();audio.renderRun();audio.screen.clear.performClick();assertTrue(audio.state.inputs.isEmpty());assertEquals(160,audio.state.bitrate);assertEquals("content://dirs/tree/music",audio.state.folder);assertTrue(audio.state.keepScreenOn);
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioFlowTest.progressReservesVerificationAndReportsIncompleteQueue -->
<a id="test-92d4ce4da3ab27f8"></a>
### `AudioFlowTest.progressReservesVerificationAndReportsIncompleteQueue`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Names fittizio restituisce nomi/12345 byte/directory scrivibile, senza audio. Stato servizi e risultati iniettati via reflection; MainLooper pilotato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `run.queuePercent`, `run.results.add`, `AudioRunState.Result`, `run.summary`, `run.savings`, `AudioOutput.unique`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Percentuale coda e categorie su AudioRunState costruito; nessuna fase di verifica effettivamente eseguita.

**Limiti:** Intent intercettati, non servizio/encoder avviato. Helper ready usa polling con Thread.sleep(5) e timeout 5 s; vista dei report legata a childAt.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void progressReservesVerificationAndReportsIncompleteQueue(){
        AudioRunState run=new AudioRunState();run.running=true;run.total=2;run.current=1;run.percent=89;assertEquals(44,run.queuePercent());run.percent=96;assertEquals(48,run.queuePercent());
        run.results.add(new AudioRunState.Result("cancel.flac","content://songs/cancel.flac",null,-1,0,"Annullato",AudioRunState.Outcome.CANCELLED));run.running=false;assertEquals(50,run.queuePercent());assertTrue(run.summary().contains("non elaborati 1"));assertEquals("Nessun file salvato",run.savings());
        assertEquals("Song (2).opus",AudioOutput.unique("Song.opus",Set.of("SONG.OPUS")));
    }
```

</details>

<!-- /test -->


<a id="class-audiopacketcopiertest"></a>
## AudioPacketCopierTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java) · 7 metodi · area originaria: Audio.

**Contesto applicativo della classe:** AudioPacketCopier.copy.

**Riferimenti produttivi:** [AudioPacketCopier](../../app/src/main/java/it/edo/codecprobe/AudioPacketCopier.java).

**Preparazione, file e simulazioni condivisi:** Packets implementa Source con array di byte/timestamp; sink, check e log sono lambda di prova. ByteBuffer reali, non MediaExtractor/MediaMuxer.

**Confini comuni dell’audit:** Può passare con extractor/muxer AAC Android guasti: verifica il ciclo di copia su un'interfaccia simulata, non un file audio.

**Metodi:** [negativePrerollZeroAndPositivePacketsKeepEveryByteAndTimestamp](#test-a53e4511d65aab85) · [minusOneIsAValidTimestampWhenSampleExists](#test-9ca4d22f6fc54d34) · [positiveStartingTimeIsNotRebased](#test-ab8b5fb1a6f56e83) · [diagnosticsAreBoundedButCopyContinues](#test-73fa6da2741f5cff) · [emptyAndMalformedSourcesAreErrorsInsteadOfSilentAudioLoss](#test-760a711b6b38fbde) · [cancellationStopsBeforeReadingNextPacket](#test-21cdbdb92c93360e) · [unsupportedMuxerReportsTheOriginalNegativeTimestamp](#test-2822cb24424f468d)

<!-- test: AudioPacketCopierTest.negativePrerollZeroAndPositivePacketsKeepEveryByteAndTimestamp -->
<a id="test-a53e4511d65aab85"></a>
### `AudioPacketCopierTest.negativePrerollZeroAndPositivePacketsKeepEveryByteAndTimestamp`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Packets implementa Source con array di byte/timestamp; sink, check e log sono lambda di prova. ByteBuffer reali, non MediaExtractor/MediaMuxer.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioPacketCopier.copy`, `log.stream`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Preroll negativo: conserva tutti i byte e sei timestamp, logga quattro PTS negativi. Il nome menziona zero ma questa fixture ha quattro PTS negativi e due positivi; zero è esercitato dal test successivo.

**Limiti:** Può passare con extractor/muxer AAC Android guasti: verifica il ciclo di copia su un'interfaccia simulata, non un file audio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void negativePrerollZeroAndPositivePacketsKeepEveryByteAndTimestamp()throws Exception {
        Packets source=new Packets(-161133,-114694,-68254,-21814,24625,71065);
        List<Long> written=new ArrayList<>();List<byte[]> output=new ArrayList<>();List<String> log=new ArrayList<>();
        AudioPacketCopier.copy(source,2,(buffer,size,time)->{
            written.add(time);byte[] bytes=new byte[size];buffer.get(bytes);output.add(bytes);
        },()->{},log::add);
        assertEquals(Arrays.asList(-161133L,-114694L,-68254L,-21814L,24625L,71065L),written);
        for(int n=0;n<source.bytes.length;n++)assertArrayEquals(source.bytes[n],output.get(n));
        assertTrue(log.stream().anyMatch(line->line.contains("negative_pts=4")));
        assertEquals(6,log.stream().filter(line->line.startsWith("DIAGNOSTICA PRIMI TIMESTAMP")).count());
    }
```

</details>

<!-- /test -->

<!-- test: AudioPacketCopierTest.minusOneIsAValidTimestampWhenSampleExists -->
<a id="test-9ca4d22f6fc54d34"></a>
### `AudioPacketCopierTest.minusOneIsAValidTimestampWhenSampleExists`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Packets implementa Source con array di byte/timestamp; sink, check e log sono lambda di prova. ByteBuffer reali, non MediaExtractor/MediaMuxer.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioPacketCopier.copy`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** PTS -1 non è EOF se esiste un campione.

**Limiti:** Può passare con extractor/muxer AAC Android guasti: verifica il ciclo di copia su un'interfaccia simulata, non un file audio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void minusOneIsAValidTimestampWhenSampleExists()throws Exception {
        Packets source=new Packets(-40000,-1,0,20000);List<Long> written=new ArrayList<>();
        AudioPacketCopier.copy(source,2,(b,n,t)->written.add(t),()->{},s->{});
        assertEquals(Arrays.asList(-40000L,-1L,0L,20000L),written);
    }
```

</details>

<!-- /test -->

<!-- test: AudioPacketCopierTest.positiveStartingTimeIsNotRebased -->
<a id="test-ab8b5fb1a6f56e83"></a>
### `AudioPacketCopierTest.positiveStartingTimeIsNotRebased`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Packets implementa Source con array di byte/timestamp; sink, check e log sono lambda di prova. ByteBuffer reali, non MediaExtractor/MediaMuxer.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioPacketCopier.copy`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** PTS iniziale positivo non viene ribasato.

**Limiti:** Può passare con extractor/muxer AAC Android guasti: verifica il ciclo di copia su un'interfaccia simulata, non un file audio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void positiveStartingTimeIsNotRebased()throws Exception {
        List<Long> written=new ArrayList<>();
        AudioPacketCopier.copy(new Packets(80000,120000),2,(b,n,t)->written.add(t),()->{},s->{});
        assertEquals(Arrays.asList(80000L,120000L),written);
    }
```

</details>

<!-- /test -->

<!-- test: AudioPacketCopierTest.diagnosticsAreBoundedButCopyContinues -->
<a id="test-73fa6da2741f5cff"></a>
### `AudioPacketCopierTest.diagnosticsAreBoundedButCopyContinues`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Packets implementa Source con array di byte/timestamp; sink, check e log sono lambda di prova. ByteBuffer reali, non MediaExtractor/MediaMuxer.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioPacketCopier.copy`, `log.stream`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Copia 100 pacchetti ma limita a 12 la diagnostica iniziale.

**Limiti:** Può passare con extractor/muxer AAC Android guasti: verifica il ciclo di copia su un'interfaccia simulata, non un file audio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void diagnosticsAreBoundedButCopyContinues()throws Exception {
        long[] times=new long[100];for(int n=0;n<times.length;n++)times[n]=n*20000L-40000;
        List<String> log=new ArrayList<>();List<Long> written=new ArrayList<>();
        AudioPacketCopier.copy(new Packets(times),2,(b,n,t)->written.add(t),()->{},log::add);
        assertEquals(100,written.size());
        assertEquals(12,log.stream().filter(line->line.startsWith("DIAGNOSTICA PRIMI TIMESTAMP")).count());
    }
```

</details>

<!-- /test -->

<!-- test: AudioPacketCopierTest.emptyAndMalformedSourcesAreErrorsInsteadOfSilentAudioLoss -->
<a id="test-760a711b6b38fbde"></a>
### `AudioPacketCopierTest.emptyAndMalformedSourcesAreErrorsInsteadOfSilentAudioLoss`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Packets implementa Source con array di byte/timestamp; sink, check e log sono lambda di prova. ByteBuffer reali, non MediaExtractor/MediaMuxer.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `assertFailure(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Errori per zero pacchetti, PTS duplicati/decrescenti, protezione, lettura corta, traccia errata e pacchetto oltre 16 MiB.

**Limiti:** Può passare con extractor/muxer AAC Android guasti: verifica il ciclo di copia su un'interfaccia simulata, non un file audio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void emptyAndMalformedSourcesAreErrorsInsteadOfSilentAudioLoss()throws Exception {
        assertFailure(new Packets(),"realmente senza pacchetti");
        assertFailure(new Packets(-10,-10),"non crescente");
        assertFailure(new Packets(10,0),"non crescente");
        Packets protectedAudio=new Packets(0);protectedAudio.protectedAudio=true;assertFailure(protectedAudio,"protetto");
        Packets incomplete=new Packets(-20);incomplete.readAdjustment=-1;assertFailure(incomplete,"incompleta");
        Packets wrongTrack=new Packets(0);wrongTrack.track=3;assertFailure(wrongTrack,"traccia diversa");
        Packets excessive=new Packets(0);excessive.bytes[0]=new byte[AudioPacketCopier.MAX_PACKET_BYTES+1];assertFailure(excessive,"Dimensione");
    }
```

</details>

<!-- /test -->

<!-- test: AudioPacketCopierTest.cancellationStopsBeforeReadingNextPacket -->
<a id="test-21cdbdb92c93360e"></a>
### `AudioPacketCopierTest.cancellationStopsBeforeReadingNextPacket`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Packets implementa Source con array di byte/timestamp; sink, check e log sono lambda di prova. ByteBuffer reali, non MediaExtractor/MediaMuxer.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioPacketCopier.copy`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Check annullamento interrompe prima del secondo pacchetto.

**Limiti:** Può passare con extractor/muxer AAC Android guasti: verifica il ciclo di copia su un'interfaccia simulata, non un file audio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void cancellationStopsBeforeReadingNextPacket()throws Exception {
        Packets source=new Packets(-20,-10,0);List<Long> written=new ArrayList<>();
        try{
            AudioPacketCopier.copy(source,2,(b,n,t)->written.add(t),()->{
                if(!written.isEmpty())throw new InterruptedException("cancelled");
            },s->{});fail();
        }catch(InterruptedException expected){}
        assertEquals(List.of(-20L),written);assertEquals(1,source.index);
    }
```

</details>

<!-- /test -->

<!-- test: AudioPacketCopierTest.unsupportedMuxerReportsTheOriginalNegativeTimestamp -->
<a id="test-2822cb24424f468d"></a>
### `AudioPacketCopierTest.unsupportedMuxerReportsTheOriginalNegativeTimestamp`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Packets implementa Source con array di byte/timestamp; sink, check e log sono lambda di prova. ByteBuffer reali, non MediaExtractor/MediaMuxer.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioPacketCopierTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioPacketCopier.copy`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Sink che rifiuta il PTS negativo produce IOException con il PTS originario, non lo corregge silenziosamente.

**Limiti:** Può passare con extractor/muxer AAC Android guasti: verifica il ciclo di copia su un'interfaccia simulata, non un file audio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void unsupportedMuxerReportsTheOriginalNegativeTimestamp()throws Exception {
        try{
            AudioPacketCopier.copy(new Packets(-12345,0),2,(b,n,t)->{throw new IllegalArgumentException("unsupported");},()->{},s->{});fail();
        }catch(IOException error){assertTrue(error.getMessage().contains("pts_us=-12345"));assertTrue(error.getMessage().contains("nessun timestamp modificato"));}
    }
```

</details>

<!-- /test -->


<a id="class-audiopcmtest"></a>
## AudioPcmTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AudioPcmTest.java) · 3 metodi · area originaria: Audio.

**Contesto applicativo della classe:** AudioMediaDecoder.kind; AudioPcm.read/bytes.

**Riferimenti produttivi:** [AudioMediaDecoder](../../app/src/main/java/it/edo/codecprobe/AudioMediaDecoder.java), [AudioPcm](../../app/src/main/java/it/edo/codecprobe/AudioPcm.java).

**Preparazione, file e simulazioni condivisi:** ByteBuffer sintetici; Robolectric è presente ma vengono usate costanti AudioFormat, nessun decoder avviato.

**Confini comuni dell’audit:** Non decodifica MP3/M4A/FLAC reali, né verifica formato negoziato da MediaCodec.

**Metodi:** [detectsSupportedInputsWithoutChangingFlacRoute](#test-e3afc28fac938c20) · [decodesSignedAndUnsignedSamplesWithCorrectScale](#test-91c7c9347e565ea6) · [handlesFloatOffsetsAndRejectsCorruption](#test-0dcaa688be67f170)

<!-- test: AudioPcmTest.detectsSupportedInputsWithoutChangingFlacRoute -->
<a id="test-e3afc28fac938c20"></a>
### `AudioPcmTest.detectsSupportedInputsWithoutChangingFlacRoute`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): ByteBuffer sintetici; Robolectric è presente ma vengono usate costanti AudioFormat, nessun decoder avviato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioPcmTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioMediaDecoder.kind`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Routing da estensione FLAC/MP3/M4A e rifiuto Opus; non apertura del contenuto.

**Limiti:** Non decodifica MP3/M4A/FLAC reali, né verifica formato negoziato da MediaCodec.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void detectsSupportedInputsWithoutChangingFlacRoute()throws Exception{
        assertEquals(0,AudioMediaDecoder.kind("song.FLAC"));assertEquals(1,AudioMediaDecoder.kind("song.MP3"));assertEquals(2,AudioMediaDecoder.kind("song.m4a"));
        assertThrows(IOException.class,()->AudioMediaDecoder.kind("song.opus"));
    }
```

</details>

<!-- /test -->

<!-- test: AudioPcmTest.decodesSignedAndUnsignedSamplesWithCorrectScale -->
<a id="test-91c7c9347e565ea6"></a>
### `AudioPcmTest.decodesSignedAndUnsignedSamplesWithCorrectScale`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): ByteBuffer sintetici; Robolectric è presente ma vengono usate costanti AudioFormat, nessun decoder avviato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioPcmTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioPcm.read`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Conversione numerica 8/16/24 bit dei campioni estremi verso float con scala prevista.

**Limiti:** Non decodifica MP3/M4A/FLAC reali, né verifica formato negoziato da MediaCodec.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void decodesSignedAndUnsignedSamplesWithCorrectScale()throws Exception{
        float[] out=new float[3];
        AudioPcm.read(ByteBuffer.wrap(new byte[]{0,(byte)128,(byte)255}),AudioFormat.ENCODING_PCM_8BIT,out,3);
        assertArrayEquals(new float[]{-1,0,127/128f},out,0);
        ByteBuffer b=ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).putShort(Short.MIN_VALUE).putShort((short)0).putShort(Short.MAX_VALUE);b.flip();
        AudioPcm.read(b,AudioFormat.ENCODING_PCM_16BIT,out,3);assertArrayEquals(new float[]{-1,0,32767/32768f},out,0);
        AudioPcm.read(ByteBuffer.wrap(new byte[]{0,0,(byte)128,0,0,0,(byte)255,(byte)255,127}),AudioFormat.ENCODING_PCM_24BIT_PACKED,out,3);
        assertArrayEquals(new float[]{-1,0,8388607/8388608f},out,0);
    }
```

</details>

<!-- /test -->

<!-- test: AudioPcmTest.handlesFloatOffsetsAndRejectsCorruption -->
<a id="test-0dcaa688be67f170"></a>
### `AudioPcmTest.handlesFloatOffsetsAndRejectsCorruption`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): ByteBuffer sintetici; Robolectric è presente ma vengono usate costanti AudioFormat, nessun decoder avviato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioPcmTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioPcm.read`, `AudioPcm.bytes`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Rispetta offset/limit per float e rifiuta NaN, byte insufficienti e encoding sconosciuto.

**Limiti:** Non decodifica MP3/M4A/FLAC reali, né verifica formato negoziato da MediaCodec.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void handlesFloatOffsetsAndRejectsCorruption()throws Exception{
        float[] out=new float[2];ByteBuffer b=ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);b.putFloat(99).putFloat(-.5f).putFloat(.75f).putFloat(99);b.position(4);b.limit(12);
        AudioPcm.read(b,AudioFormat.ENCODING_PCM_FLOAT,out,2);assertArrayEquals(new float[]{-.5f,.75f},out,0);
        ByteBuffer invalid=ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(Float.NaN);invalid.flip();
        assertThrows(IOException.class,()->AudioPcm.read(invalid,AudioFormat.ENCODING_PCM_FLOAT,out,1));
        assertThrows(IOException.class,()->AudioPcm.read(ByteBuffer.allocate(1),AudioFormat.ENCODING_PCM_16BIT,out,1));
        assertThrows(IOException.class,()->AudioPcm.bytes(9999));
    }
```

</details>

<!-- /test -->


<a id="class-audiosettingstest"></a>
## AudioSettingsTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java) · 6 metodi · area originaria: Audio.

**Contesto applicativo della classe:** AudioSettingsScreen/AudioSettings; AudioController; AudioDraftStore.

**Riferimenti produttivi:** [AudioSettingsScreen](../../app/src/main/java/it/edo/codecprobe/AudioSettingsScreen.java), [AudioSettings](../../app/src/main/java/it/edo/codecprobe/AudioSettings.java), [AudioController](../../app/src/main/java/it/edo/codecprobe/AudioController.java), [AudioDraftStore](../../app/src/main/java/it/edo/codecprobe/AudioDraftStore.java).

**Preparazione, file e simulazioni condivisi:** Activity/SharedPreferences simulate; AudioRunState iniettato tramite campo privato e nessun brano aperto.

**Confini comuni dell’audit:** Non prova salvataggio su storage persistente di un processo reale, configurazione nativa o lifecycle imposto dal sistema.

**Metodi:** [headerOpensDedicatedAudioPageWithCurrentConfiguration](#test-3c2feb0df0dffa83) · [unsavedEditsSurviveRecreationButBackDoesNotSave](#test-9a2c59a5f9e6ef75) · [savedDefaultsUpdateAudioHomeWithoutChangingSelectedFilesOrVideo](#test-6faf922c365c0e31) · [defaultsSavedDuringConversionApplyOnlyAfterItsEnd](#test-dd65cf0ef35fef5a) · [explicitFolderRemovalDoesNotFallBackToOldDraftAndDefaultsStaySeparate](#test-376aa0b628d6d4f6) · [copyCurrentAndFolderCancelOnlyChangeUnsavedForm](#test-c0c41669885b1086)

<!-- test: AudioSettingsTest.headerOpensDedicatedAudioPageWithCurrentConfiguration -->
<a id="test-3c2feb0df0dffa83"></a>
### `AudioSettingsTest.headerOpensDedicatedAudioPageWithCurrentConfiguration`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/SharedPreferences simulate; AudioRunState iniettato tramite campo privato e nessun brano aperto.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.get`, `audio.screen.bitrate.setProgress`, `audio.screen.settings.performClick`, `SettingsActivity.class.getName`, `home.pause`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Header passa configurazione corrente 160 all'Activity impostazioni audio dedicata.

**Limiti:** Non prova salvataggio su storage persistente di un processo reale, configurazione nativa o lifecycle imposto dal sistema.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void headerOpensDedicatedAudioPageWithCurrentConfiguration(){
        var home=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=home.get().audio;audio.screen.bitrate.setProgress(12);audio.state.folder="content://dirs/tree/current";audio.screen.settings.performClick();
            Intent open=Shadows.shadowOf(home.get()).getNextStartedActivity();assertEquals(SettingsActivity.class.getName(),open.getComponent().getClassName());assertTrue(open.getBooleanExtra(AudioSettingsScreen.AUDIO,false));assertEquals(160,open.getIntExtra("audioBitrate",0));
            var settings=Robolectric.buildActivity(SettingsActivity.class,open).setup();try{assertNotNull(settings.get().audioScreen);assertEquals("Salva impostazioni",settings.get().audioScreen.save.getText().toString());}finally{settings.pause().stop().destroy();}
        }finally{home.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioSettingsTest.unsavedEditsSurviveRecreationButBackDoesNotSave -->
<a id="test-9a2c59a5f9e6ef75"></a>
### `AudioSettingsTest.unsavedEditsSurviveRecreationButBackDoesNotSave`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/SharedPreferences simulate; AudioRunState iniettato tramite campo privato e nessun brano aperto.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `intent(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Modifiche non salvate sopravvivono al Bundle, Back non cambia revisione/default.

**Limiti:** Non prova salvataggio su storage persistente di un processo reale, configurazione nativa o lifecycle imposto dal sistema.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void unsavedEditsSurviveRecreationButBackDoesNotSave(){
        var page=Robolectric.buildActivity(SettingsActivity.class,intent()).setup();Bundle bundle=new Bundle();
        try{AudioSettingsScreen s=page.get().audioScreen;s.bitrate.setProgress(16);s.onlySmaller.setChecked(false);s.keepScreenOn.setChecked(true);s.folder="content://dirs/tree/edited";page.saveInstanceState(bundle);assertEquals(0,new AudioSettings(context).revision);}finally{page.pause().stop().destroy();}
        var restored=Robolectric.buildActivity(SettingsActivity.class,intent()).create(bundle).start().resume();try{
            AudioSettingsScreen s=restored.get().audioScreen;assertEquals(192,s.value());assertFalse(s.onlySmaller.isChecked());assertTrue(s.keepScreenOn.isChecked());assertEquals("content://dirs/tree/edited",s.folder);s.back.performClick();
            assertEquals(0,new AudioSettings(context).revision);assertEquals(128,new AudioSettings(context).bitrate);assertNull(new AudioSettings(context).folder);
        }finally{restored.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioSettingsTest.savedDefaultsUpdateAudioHomeWithoutChangingSelectedFilesOrVideo -->
<a id="test-6faf922c365c0e31"></a>
### `AudioSettingsTest.savedDefaultsUpdateAudioHomeWithoutChangingSelectedFilesOrVideo`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/SharedPreferences simulate; AudioRunState iniettato tramite campo privato e nessun brano aperto.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.get`, `audio.state.inputs.add`, `audio.state.names.add`, `audio.state.lyricsInputs.add`, `audio.state.lyricsNames.add`, `audio.save`, `audio.renderRun`, `audio.screen.bitrate.getProgress`, `audio.screen.onlySmaller.isChecked`, `audio.screen.keepScreenOn.isChecked`, `home.pause`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Salva default 192 e flag/cartella senza perdere file/LRC o modificare prefs Video.

**Limiti:** Non prova salvataggio su storage persistente di un processo reale, configurazione nativa o lifecycle imposto dal sistema.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void savedDefaultsUpdateAudioHomeWithoutChangingSelectedFilesOrVideo(){
        Map<String,?> video=new HashMap<>(context.getSharedPreferences("startup-settings",0).getAll());
        var home=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=home.get().audio;audio.state.inputs.add("content://songs/kept");audio.state.names.add("kept.flac");audio.state.lyricsInputs.add("content://songs/kept-lrc");audio.state.lyricsNames.add("kept.lrc");audio.save();
            var settings=Robolectric.buildActivity(SettingsActivity.class,intent()).setup();try{
                AudioSettingsScreen s=settings.get().audioScreen;s.bitrate.setProgress(16);s.onlySmaller.setChecked(false);s.keepScreenOn.setChecked(true);s.folder="content://dirs/tree/saved";s.save.performClick();
            }finally{settings.pause().stop().destroy();}
            audio.renderRun();assertEquals(192,audio.state.bitrate);assertEquals(16,audio.screen.bitrate.getProgress());assertFalse(audio.screen.onlySmaller.isChecked());assertTrue(audio.screen.keepScreenOn.isChecked());assertEquals("content://dirs/tree/saved",audio.state.folder);
            assertEquals(List.of("kept.flac"),audio.state.names);assertEquals(List.of("kept.lrc"),audio.state.lyricsNames);assertEquals(video,context.getSharedPreferences("startup-settings",0).getAll());
        }finally{home.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioSettingsTest.defaultsSavedDuringConversionApplyOnlyAfterItsEnd -->
<a id="test-dd65cf0ef35fef5a"></a>
### `AudioSettingsTest.defaultsSavedDuringConversionApplyOnlyAfterItsEnd`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/SharedPreferences simulate; AudioRunState iniettato tramite campo privato e nessun brano aperto.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.get`, `audio.renderRun`, `audio.screen.settings.isEnabled`, `audio.save`, `AudioSettings.save`, `AudioConversionService.snapshot`, `home.pause`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Nuovi default durante run iniettata restano differiti fino a run terminata; stato del servizio rimane 128.

**Limiti:** Non prova salvataggio su storage persistente di un processo reale, configurazione nativa o lifecycle imposto dal sistema.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void defaultsSavedDuringConversionApplyOnlyAfterItsEnd()throws Exception{
        var home=Robolectric.buildActivity(AudioActivity.class).setup();try{
            AudioController audio=home.get().audio;AudioRunState run=new AudioRunState();run.running=true;run.total=1;run.bitrate=128;run.onlySmaller=true;serviceState.set(null,run);audio.renderRun();assertTrue(audio.screen.settings.isEnabled());
            audio.save();AudioSettings.save(context,192,"content://dirs/tree/next",false,false);audio.renderRun();assertEquals(128,audio.state.bitrate);assertEquals(128,AudioConversionService.snapshot().bitrate);assertTrue(AudioConversionService.snapshot().onlySmaller);
            var recreated=Robolectric.buildActivity(AudioActivity.class).setup();try{assertEquals(128,recreated.get().audio.state.bitrate);}finally{recreated.pause().stop().destroy();}
            run.running=false;audio.renderRun();assertEquals(192,audio.state.bitrate);assertFalse(audio.state.onlySmaller);assertEquals(128,AudioConversionService.snapshot().bitrate);
        }finally{home.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioSettingsTest.explicitFolderRemovalDoesNotFallBackToOldDraftAndDefaultsStaySeparate -->
<a id="test-376aa0b628d6d4f6"></a>
### `AudioSettingsTest.explicitFolderRemovalDoesNotFallBackToOldDraftAndDefaultsStaySeparate`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/SharedPreferences simulate; AudioRunState iniettato tramite campo privato e nessun brano aperto.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioDraftStore.save`, `AudioSettings.save`, `AudioDraftStore.load`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Rimozione esplicita cartella non riporta una vecchia bozza; default e bozza restano separati.

**Limiti:** Non prova salvataggio su storage persistente di un processo reale, configurazione nativa o lifecycle imposto dal sistema.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void explicitFolderRemovalDoesNotFallBackToOldDraftAndDefaultsStaySeparate(){
        AudioState legacy=new AudioState();legacy.bitrate=160;legacy.folder="content://dirs/tree/old";AudioDraftStore.save(context,legacy);assertEquals(160,new AudioSettings(context).bitrate);assertEquals(legacy.folder,new AudioSettings(context).folder);
        AudioSettings.save(context,128,null,true,false);assertNull(new AudioSettings(context).folder);AudioState first=AudioDraftStore.load(context);assertEquals(128,first.bitrate);assertNull(first.folder);
        first.bitrate=192;first.folder="content://dirs/tree/temporary";AudioDraftStore.save(context,first);assertEquals(128,new AudioSettings(context).bitrate);assertNull(new AudioSettings(context).folder);assertEquals(192,AudioDraftStore.load(context).bitrate);
    }
```

</details>

<!-- /test -->

<!-- test: AudioSettingsTest.copyCurrentAndFolderCancelOnlyChangeUnsavedForm -->
<a id="test-c0c41669885b1086"></a>
### `AudioSettingsTest.copyCurrentAndFolderCancelOnlyChangeUnsavedForm`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/SharedPreferences simulate; AudioRunState iniettato tramite campo privato e nessun brano aperto.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioSettingsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioSettings.save`, `FolderPickerActivity.class.getName`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Copia corrente modifica solo form; cancel o concessione readonly non sostituiscono cartella né default.

**Limiti:** Non prova salvataggio su storage persistente di un processo reale, configurazione nativa o lifecycle imposto dal sistema.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void copyCurrentAndFolderCancelOnlyChangeUnsavedForm(){
        AudioSettings.save(context,128,"content://dirs/tree/default",true,false);
        Intent open=intent().putExtra("audioBitrate",160).putExtra("audioFolder","content://dirs/tree/current").putExtra("audioSmaller",false).putExtra("audioScreen",true);
        var settings=Robolectric.buildActivity(SettingsActivity.class,open).setup();try{
            AudioSettingsScreen s=settings.get().audioScreen;assertEquals(128,s.value());s.copyCurrent.performClick();assertEquals(160,s.value());assertEquals("content://dirs/tree/current",s.folder);assertFalse(s.onlySmaller.isChecked());
            s.chooseFolder.performClick();assertEquals(FolderPickerActivity.class.getName(),Shadows.shadowOf(settings.get()).getNextStartedActivity().getComponent().getClassName());
            s.result(10,Activity.RESULT_CANCELED,null);assertEquals("content://dirs/tree/current",s.folder);
            s.result(10,Activity.RESULT_OK,new Intent().setData(Uri.parse("content://dirs/tree/readonly")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));assertEquals("content://dirs/tree/current",s.folder);
            assertEquals(128,new AudioSettings(context).bitrate);assertEquals("content://dirs/tree/default",new AudioSettings(context).folder);
        }finally{settings.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-audioworkspacetest"></a>
## AudioWorkspaceTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AudioWorkspaceTest.java) · 4 metodi · area originaria: Audio.

**Contesto applicativo della classe:** AudioController/Selection; AudioDraftStore; SavedSession; AppNavigation.

**Riferimenti produttivi:** [AudioController](../../app/src/main/java/it/edo/codecprobe/AudioController.java), [AudioDraftStore](../../app/src/main/java/it/edo/codecprobe/AudioDraftStore.java), [SavedSession](../../app/src/main/java/it/edo/codecprobe/SavedSession.java), [AppNavigation](../../app/src/main/java/it/edo/codecprobe/AppNavigation.java).

**Preparazione, file e simulazioni condivisi:** Activity Robolectric, provider Names sintetico; un vero journal Video usato come sentinella e confrontato byte per byte; executor reale/latch.

**Confini comuni dell’audit:** Nessun file audio o conversione. Latch con timeout e worker get(5 s); navigazione osservata come Intent.

**Metodi:** [independentAudioDraftAndSharedViewsSurviveReopeningWithoutChangingSavedVideo](#test-65b18141843d0b44) · [multipleAudioFilesUseOnlyAudioPickerAndCancelledSelectionKeepsDraft](#test-9fe1514ec029ecd3) · [navigationKeepsVideoSelectionAndDoesNotTouchServiceState](#test-256a2d4838181964) · [lateFilenameCallbackCannotRepopulateClearedAudioSelection](#test-2dc254c0a4f308f9)

<!-- test: AudioWorkspaceTest.independentAudioDraftAndSharedViewsSurviveReopeningWithoutChangingSavedVideo -->
<a id="test-65b18141843d0b44"></a>
### `AudioWorkspaceTest.independentAudioDraftAndSharedViewsSurviveReopeningWithoutChangingSavedVideo`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity Robolectric, provider Names sintetico; un vero journal Video usato come sentinella e confrontato byte per byte; executor reale/latch.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `SavedSession.save`, `journal.toPath`, `audio.screen.sections.bodies.get`, `audio.screen.navigation.audio.isSelected`, `audio.screen.navigation.video.isSelected`, `audio.screen.pick.getElevation`, `audio.state.inputs.add`, `audio.state.names.add`, `audio.screen.bitrate.setProgress`, `audio.render`, `audio.screen.sections.focusCard`, `SavedSession.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Bozza audio e card riaperte conservano configurazione mentre journal Video e CRF rimangono identici.

**Limiti:** Nessun file audio o conversione. Latch con timeout e worker get(5 s); navigazione osservata come Intent.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void independentAudioDraftAndSharedViewsSurviveReopeningWithoutChangingSavedVideo()throws Exception{
        Intent queue=BatchRetryTest.queue(context);ConversionService.State video=SavedSessionTest.sample(queue);
        SavedSession.save(context,queue,video);File journal=new File(context.getFilesDir(),"conversion-session.json");byte[] original=Files.readAllBytes(journal.toPath());
        AppSettings settings=new AppSettings(context);int videoCrf=settings.crf;
        var first=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            AudioController audio=first.get().audio;
            assertEquals(View.VISIBLE,audio.screen.sections.bodies.get(AudioScreen.FILES).getVisibility());assertEquals(View.GONE,audio.screen.sections.bodies.get(AudioScreen.COMPRESSION).getVisibility());
            assertTrue(audio.screen.navigation.audio.isSelected());assertFalse(audio.screen.navigation.video.isSelected());
            assertEquals(0f,audio.screen.pick.getElevation(),0f);
            audio.state.inputs.add("content://songs/music");audio.state.names.add("music.flac");audio.state.folder="content://dirs/tree/music";
            audio.screen.bitrate.setProgress(16);audio.render();audio.screen.sections.focusCard(AudioScreen.COMPRESSION);
            first.pause().stop().destroy();
            var second=Robolectric.buildActivity(AudioActivity.class).setup();
            try{
                AudioController restored=second.get().audio;assertEquals(List.of("music.flac"),restored.state.names);assertEquals("content://dirs/tree/music",restored.state.folder);assertEquals(192,restored.state.bitrate);
                assertEquals(View.VISIBLE,restored.screen.sections.bodies.get(AudioScreen.COMPRESSION).getVisibility());
                assertArrayEquals(original,Files.readAllBytes(journal.toPath()));assertEquals(videoCrf,new AppSettings(context).crf);
                restored.selection.clear();assertTrue(restored.state.inputs.isEmpty());assertEquals(192,restored.state.bitrate);assertArrayEquals(original,Files.readAllBytes(journal.toPath()));
            }finally{second.pause().stop().destroy();}
        }finally{if(!first.get().isDestroyed())first.pause().stop().destroy();SavedSession.clear(context);}
    }
```

</details>

<!-- /test -->

<!-- test: AudioWorkspaceTest.multipleAudioFilesUseOnlyAudioPickerAndCancelledSelectionKeepsDraft -->
<a id="test-9fe1514ec029ecd3"></a>
### `AudioWorkspaceTest.multipleAudioFilesUseOnlyAudioPickerAndCancelledSelectionKeepsDraft`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity Robolectric, provider Names sintetico; un vero journal Video usato come sentinella e confrontato byte per byte; executor reale/latch.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `audio.screen.pick.performClick`, `audio.selection.result`, `audio.selection.worker.submit`, `audio.state.inputs.size`, `audio.screen.selected.getText`, `audio.screen.chooseFolder.performClick`, `FolderPickerActivity.class.getName`, `audio.destinations.result`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Picker audio multiselezione deduplica lo stesso URI e ignora annullamento/cartella readonly.

**Limiti:** Nessun file audio o conversione. Latch con timeout e worker get(5 s); navigazione osservata come Intent.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void multipleAudioFilesUseOnlyAudioPickerAndCancelledSelectionKeepsDraft()throws Exception{
        ShadowContentResolver.registerProviderInternal("songs",new Names());var controller=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            AudioController audio=controller.get().audio;audio.screen.pick.performClick();Intent picker=Shadows.shadowOf(controller.get()).getNextStartedActivity();
            assertEquals("audio/*",picker.getType());assertTrue(picker.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));
            ClipData clips=ClipData.newRawUri("one",Uri.parse("content://songs/one"));clips.addItem(new ClipData.Item(Uri.parse("content://songs/two")));clips.addItem(new ClipData.Item(Uri.parse("content://songs/one")));
            Intent chosen=new Intent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);chosen.setClipData(clips);
            audio.selection.result(AudioSelection.FILES,Activity.RESULT_OK,chosen);
            audio.selection.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(audio.state.readingNames);assertEquals(List.of("one.flac","two.flac"),audio.state.names);assertEquals(2,audio.state.inputs.size());assertTrue(audio.screen.selected.getText().toString().contains("2"));
            audio.selection.result(AudioSelection.FILES,Activity.RESULT_CANCELED,null);assertEquals(2,audio.state.inputs.size());
            audio.screen.chooseFolder.performClick();assertEquals(FolderPickerActivity.class.getName(),Shadows.shadowOf(controller.get()).getNextStartedActivity().getComponent().getClassName());audio.destinations.result(MediaDestinationsController.FILE,Activity.RESULT_CANCELED,null);
            audio.state.folder="content://dirs/tree/previous";
            audio.selection.result(AudioSelection.FOLDER,Activity.RESULT_OK,new Intent().setData(Uri.parse("content://dirs/tree/readonly")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            assertEquals("content://dirs/tree/previous",audio.state.folder);
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioWorkspaceTest.navigationKeepsVideoSelectionAndDoesNotTouchServiceState -->
<a id="test-256a2d4838181964"></a>
### `AudioWorkspaceTest.navigationKeepsVideoSelectionAndDoesNotTouchServiceState`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity Robolectric, provider Names sintetico; un vero journal Video usato come sentinella e confrontato byte per byte; executor reale/latch.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.state.selectedInputs.add`, `home.state.selectedNames.add`, `ConversionService.snapshot`, `home.screen.navigation.audio.performClick`, `AudioActivity.class.getName`, `audio.get`, `audio.screen.navigation.video.performClick`, `MainActivity.class.getName`, `audio.pause`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Navigare Video↔Audio mantiene selezione, sessionId/running/report del servizio iniettato.

**Limiti:** Nessun file audio o conversione. Latch con timeout e worker get(5 s); navigazione osservata come Intent.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void navigationKeepsVideoSelectionAndDoesNotTouchServiceState()throws Exception{
        var video=Robolectric.buildActivity(MainActivity.class).setup();var audio=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            HomeController home=video.get().home;home.state.selectedInputs.add("content://video/kept");home.state.selectedNames.add("kept.mp4");
            ConversionService.State before=ConversionService.snapshot();
            home.screen.navigation.audio.performClick();Intent openAudio=Shadows.shadowOf(video.get()).getNextStartedActivity();assertEquals(AudioActivity.class.getName(),openAudio.getComponent().getClassName());
            assertTrue((openAudio.getFlags()&Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)!=0);
            audio.get().audio.screen.navigation.video.performClick();Intent openVideo=Shadows.shadowOf(audio.get()).getNextStartedActivity();assertEquals(MainActivity.class.getName(),openVideo.getComponent().getClassName());assertTrue(audio.get().isFinishing());
            assertEquals(List.of("kept.mp4"),home.state.selectedNames);assertEquals(before.sessionId,ConversionService.snapshot().sessionId);assertEquals(before.running,ConversionService.snapshot().running);
            assertEquals(before.sizeReport,ConversionService.snapshot().sizeReport);
        }finally{audio.pause().stop().destroy();video.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AudioWorkspaceTest.lateFilenameCallbackCannotRepopulateClearedAudioSelection -->
<a id="test-2dc254c0a4f308f9"></a>
### `AudioWorkspaceTest.lateFilenameCallbackCannotRepopulateClearedAudioSelection`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity Robolectric, provider Names sintetico; un vero journal Video usato come sentinella e confrontato byte per byte; executor reale/latch.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `audio.selection.worker.submit`, `audio.selection.result`, `audio.screen.start.isEnabled`, `audio.selection.clear`, `audio.state.inputs.isEmpty`, `AudioDraftStore.load`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Callback nomi bloccata con latch e poi rilasciata non ripopola una selezione già cancellata.

**Limiti:** Nessun file audio o conversione. Latch con timeout e worker get(5 s); navigazione osservata come Intent.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void lateFilenameCallbackCannotRepopulateClearedAudioSelection()throws Exception{
        ShadowContentResolver.registerProviderInternal("songs",new Names());var controller=Robolectric.buildActivity(AudioActivity.class).setup();CountDownLatch release=new CountDownLatch(1);
        try{
            AudioController audio=controller.get().audio;
            audio.selection.worker.submit(()->{try{release.await(4,TimeUnit.SECONDS);}catch(InterruptedException error){Thread.currentThread().interrupt();}});
            audio.selection.result(AudioSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.parse("content://songs/late")));
            assertTrue(audio.state.readingNames);assertFalse(audio.screen.start.isEnabled());
            audio.selection.clear();assertFalse(audio.state.readingNames);release.countDown();audio.selection.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertTrue(audio.state.inputs.isEmpty());assertTrue(AudioDraftStore.load(context).inputs.isEmpty());
        }finally{release.countDown();controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->
