# Foto

[Guida](README.md) · [Indice completo](index.md) · [Revisioni](review-needed.md) · [Esecuzioni](esecuzioni.md)

Descrizioni statiche trasferite dall’audit: non attestano nuove esecuzioni. Le fixture condivise sono descritte a livello di classe; dati specifici, ordine delle operazioni e assert sono nel corpo integrale di ogni metodo. Gli helper richiamati e gli eventuali `@Before`/`@After` sono consultabili nel sorgente collegato. Lo script di aggiornamento non riscrive questa pagina.

## Classi

- [ImageCarouselCacheTest](#class-imagecarouselcachetest)
- [ImageFolderImportTest](#class-imagefolderimporttest)
- [ImageLargeQueueTest](#class-imagelargequeuetest)
- [ImagePreviewSchedulingTest](#class-imagepreviewschedulingtest)
- [ImageResponsivenessTest](#class-imageresponsivenesstest)
- [ImageSettingsTest](#class-imagesettingstest)
- [ImageUiTrialTest](#class-imageuitrialtest)
- [ImageWorkspaceTest](#class-imageworkspacetest)
- [PhotoDetailsLoaderTest](#class-photodetailsloadertest)

<a id="class-imagecarouselcachetest"></a>
## ImageCarouselCacheTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageCarouselCacheTest.java) · 1 metodi · area originaria: Foto.

**Contesto applicativo della classe:** ImageController.open/read; ImageMetadataCache; PhotoCompose.select/command.

**Riferimenti produttivi:** [ImageController](../../app/src/main/java/it/edo/codecprobe/ImageController.java), [ImageMetadataCache](../../app/src/main/java/it/edo/codecprobe/ImageMetadataCache.java).

**Preparazione, file e simulazioni condivisi:** Due veri JPEG sintetici e poi primo sostituito da3byte; Activity/ContentResolver file:// simulati, cache condivisa.

**Confini comuni dell’audit:** Non garantisce invalidazione automatica su cambi file: prova precisamente che i vecchi dati restano fino REREAD. Polling5ms/5s e tempo virtuale1s; nessun thumbnail.

**Metodi:** [carouselReusesPhotoMetadataAcrossNavigationAndActivityRecreationUntilReload](#test-650ee626dccfc7c8)

<!-- test: ImageCarouselCacheTest.carouselReusesPhotoMetadataAcrossNavigationAndActivityRecreationUntilReload -->
<a id="test-650ee626dccfc7c8"></a>
### `ImageCarouselCacheTest.carouselReusesPhotoMetadataAcrossNavigationAndActivityRecreationUntilReload`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Due veri JPEG sintetici e poi primo sostituito da3byte; Activity/ContentResolver file:// simulati, cache condivisa.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageCarouselCacheTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.selection.result`, `images.compose.command`, `images.compose.getState`, `images.compose.select`, `images.state.notice.contains`, `ImageMetadataCache.memory.size`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Navigazione/recreate riusano stessa Info anche dopo modifica file; REREAD rileva non-JPEG, CLEAR svuota cache ma conserva sorgente.

**Limiti:** Non garantisce invalidazione automatica su cambi file: prova precisamente che i vecchi dati restano fino REREAD. Polling5ms/5s e tempo virtuale1s; nessun thumbnail.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void carouselReusesPhotoMetadataAcrossNavigationAndActivityRecreationUntilReload() throws Exception {
        File first=File.createTempFile("first-photo-",".jpg"),second=File.createTempFile("second-photo-",".jpg");
        Files.write(first.toPath(),ImageWorkspaceTest.fixture());Files.write(second.toPath(),ImageWorkspaceTest.fixture());
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            Intent picked=new Intent();ClipData clips=ClipData.newRawUri("photos",Uri.fromFile(first));clips.addItem(new ClipData.Item(Uri.fromFile(second)));picked.setClipData(clips);
            ImageController images=controller.get().images;images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,picked);settle(images);
            JpegInventory.Info original=images.state.info;assertNotNull(original);
            images.compose.command(PhotoCommand.DETAILS);assertTrue(images.compose.getState().getDetailsOpen());
            images.compose.select(1,false);images.compose.select(0,false);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertEquals(0,images.state.selected);
            Files.write(first.toPath(),new byte[]{1,2,3});
            select(images,1);assertNotNull(images.state.info);
            select(images,0);assertSame(original,images.state.info);
            controller.recreate();images=controller.get().images;settle(images);
            assertSame(original,images.state.info);assertEquals(2,images.compose.getState().getItems().size());
            images.compose.command(PhotoCommand.REREAD);settle(images);
            assertNull(images.state.info);assertTrue(images.state.notice.contains("Non è un JPEG"));
            images.compose.command(PhotoCommand.CLEAR);org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(0,ImageMetadataCache.memory.size());
            assertEquals(0,images.compose.getState().getItems().size());assertTrue(first.exists());
        }finally{controller.pause().stop().destroy();first.delete();second.delete();reset();}
    }
```

</details>

<!-- /test -->


<a id="class-imagefolderimporttest"></a>
## ImageFolderImportTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageFolderImportTest.java) · 2 metodi · area originaria: Foto.

**Contesto applicativo della classe:** ImageFolderReader.read; ImageSelection.result; PhotoCommand.ADD_FOLDER; PhotoCompose state.

**Riferimenti produttivi:** [ImageFolderReader](../../app/src/main/java/it/edo/codecprobe/ImageFolderReader.java), [ImageSelection](../../app/src/main/java/it/edo/codecprobe/ImageSelection.java).

**Preparazione, file e simulazioni condivisi:** Provider Folder genera8500 righe JPEG più HEIC/video/directory; nessun byte immagine fornito.

**Confini comuni dell’audit:** Non prova foto leggibili o ricorsione sottocartelle; import UI polling10ms fino15s. Queue grande non è benchmark né memoria misurata.

**Metodi:** [folderListingImportsAllJpegsWithoutReadingPhotoBytes](#test-7ad0f669dc27cab7) · [oneFolderPickerResultBuildsTheWholeQueueAndOnlyOneHundredChips](#test-85657e06cf591356)

<!-- test: ImageFolderImportTest.folderListingImportsAllJpegsWithoutReadingPhotoBytes -->
<a id="test-7ad0f669dc27cab7"></a>
### `ImageFolderImportTest.folderListingImportsAllJpegsWithoutReadingPhotoBytes`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Folder genera8500 righe JPEG più HEIC/video/directory; nessun byte immagine fornito.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageFolderImportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageFolderReader.read`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Listing importa8500 soli JPEG, filtra HEIC/video/directory e assegna cartella senza leggere pixel.

**Limiti:** Non prova foto leggibili o ricorsione sottocartelle; import UI polling10ms fino15s. Queue grande non è benchmark né memoria misurata.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void folderListingImportsAllJpegsWithoutReadingPhotoBytes()throws Exception{
        var result=ImageFolderReader.read(RuntimeEnvironment.getApplication(),Uri.parse(TREE),TREE);assertEquals(8500,result.inputs.size());assertEquals("IMG_0.JPG",result.names.get(0));assertTrue(result.inputs.get(8499).contains("photo8499"));assertEquals(TREE,result.folders.get(8499));
    }
```

</details>

<!-- /test -->

<!-- test: ImageFolderImportTest.oneFolderPickerResultBuildsTheWholeQueueAndOnlyOneHundredChips -->
<a id="test-85657e06cf591356"></a>
### `ImageFolderImportTest.oneFolderPickerResultBuildsTheWholeQueueAndOnlyOneHundredChips`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Folder genera8500 righe JPEG più HEIC/video/directory; nessun byte immagine fornito.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageFolderImportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.compose.command`, `images.selection.result`, `images.state.inputs.size`, `images.compose.getState`, `images.state.inputs.get`, `images.state.destinations.plan.effective`, `images.state.destinations.plan.exceptionCount`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Risultato folder picker crea8500 riferimenti ma100 item Compose, origine senza eccezioni.

**Limiti:** Non prova foto leggibili o ricorsione sottocartelle; import UI polling10ms fino15s. Queue grande non è benchmark né memoria misurata.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void oneFolderPickerResultBuildsTheWholeQueueAndOnlyOneHundredChips()throws Exception{
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            var images=controller.get().images;images.compose.command(PhotoCommand.ADD_FOLDER);Intent request=Shadows.shadowOf(controller.get()).getNextStartedActivity();assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE,request.getAction());
            images.selection.result(ImageSelection.SOURCE_FOLDER,Activity.RESULT_OK,new Intent().setData(Uri.parse(TREE)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            long end=System.currentTimeMillis()+15000;
            while(images.state.importing&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10);}Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(images.state.importing);assertEquals(8500,images.state.inputs.size());assertTrue(images.state.bulk);assertEquals(100,images.compose.getState().getItems().size());assertEquals(8500,images.compose.getState().getTotal());assertEquals(images.state.inputs.get(99),images.compose.getState().getItems().get(99).getUri());
            assertTrue(images.state.destinations.plan.effective(8499).origin);assertEquals(0,images.state.destinations.plan.exceptionCount());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-imagelargequeuetest"></a>
## ImageLargeQueueTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java) · 8 metodi · area originaria: Foto.

**Contesto applicativo della classe:** ImageRunState; SelectedItemView; ImageWorkReport; ImageQueueStore; ImageFailureStore; ImageConversionService.

**Riferimenti produttivi:** [ImageRunState](../../app/src/main/java/it/edo/codecprobe/ImageRunState.java), [SelectedItemView](../../app/src/main/java/it/edo/codecprobe/SelectedItemView.java), [ImageWorkReport](../../app/src/main/java/it/edo/codecprobe/ImageWorkReport.java), [ImageQueueStore](../../app/src/main/java/it/edo/codecprobe/ImageQueueStore.java), [ImageFailureStore](../../app/src/main/java/it/edo/codecprobe/ImageFailureStore.java), [ImageConversionService](../../app/src/main/java/it/edo/codecprobe/ImageConversionService.java).

**Preparazione, file e simulazioni condivisi:** 5000 risultati/riferimenti manuali; files reali JSON/log nel Context; provider directory falso;125 URI mancanti. Reflection state/write.

**Confini comuni dell’audit:** Nessuna conversione riuscita/codec; dimensioni in byte solo numeri. Timeout15s e polling, conteggio100 hardcoded. Prove cap non sono misure RAM/CPU.

**Metodi:** [fiveThousandOutcomesKeepOnlyOneHundredDetailsAndExactTotals](#test-ecdd094455268991) · [carouselShowsAllSmallQueuesAndOnlyOneHundredPlusEllipsisForLargeQueues](#test-f7a0600a250b04d2) · [reportCentersCurrentPhotoAndStillIncludesTotalsAfterEviction](#test-9bed3cbfe04a2dd2) · [fullQueueIsOnDiskAndTheIntentContainsOnlyItsIdentifier](#test-42f7fec272ed3123) · [failureArchiveSurvivesHistoryEvictionAndContainsRetryInputs](#test-873e8ed1f5db9dad) · [serviceContinuesAfterMoreThanOneHundredFailuresAndPreservesEveryError](#test-3178b157eb2806b6) · [largeDraftRestoresAllFiveThousandReferences](#test-9972958674ea8246) · [diskLogKeepsEarlyErrorsBeyondTheOldTwoMibRotationLimit](#test-ede1a23e58512efc)

<!-- test: ImageLargeQueueTest.fiveThousandOutcomesKeepOnlyOneHundredDetailsAndExactTotals -->
<a id="test-ecdd094455268991"></a>
### `ImageLargeQueueTest.fiveThousandOutcomesKeepOnlyOneHundredDetailsAndExactTotals`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 5000 risultati/riferimenti manuali; files reali JSON/log nel Context; provider directory falso;125 URI mancanti. Reflection state/write.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `run.add`, `ImageRunState.Result`, `run.results.size`, `run.firstResult`, `run.resultAt`, `run.results.get`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** 5000 SAVED fittizi tengono100 dettagli e somme64bit esatte; dettagli errore100k troncati.

**Limiti:** Nessuna conversione riuscita/codec; dimensioni in byte solo numeri. Timeout15s e polling, conteggio100 hardcoded. Prove cap non sono misure RAM/CPU.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void fiveThousandOutcomesKeepOnlyOneHundredDetailsAndExactTotals(){
        ImageRunState run=new ImageRunState();run.total=5000;
        for(int i=0;i<5000;i++)run.add(new ImageRunState.Result("Foto "+i,3000000,2000000,"content://saved/"+i,"Salvato",ImageRunState.Outcome.SAVED,"Dettagli"));
        assertEquals(5000,run.completed);assertEquals(5000,run.saved);assertEquals(100,run.results.size());assertEquals(4900,run.firstResult());
        assertNull(run.resultAt(4899));assertEquals("Foto 4900",run.resultAt(4900).name);
        assertEquals(15000000000L,run.inputSavedBytes);assertEquals(10000000000L,run.outputSavedBytes);
        ImageRunState copy=new ImageRunState(run);assertEquals(100,copy.results.size());assertEquals(run.inputSavedBytes,copy.inputSavedBytes);
        String huge="x".repeat(100000);run.add(new ImageRunState.Result("errore",0,0,null,"Errore",ImageRunState.Outcome.FAILED,huge));
        assertTrue(run.results.get(99).details.length()<17000);assertEquals(1,run.failed);assertEquals(5000,run.saved);
    }
```

</details>

<!-- /test -->

<!-- test: ImageLargeQueueTest.carouselShowsAllSmallQueuesAndOnlyOneHundredPlusEllipsisForLargeQueues -->
<a id="test-f7a0600a250b04d2"></a>
### `ImageLargeQueueTest.carouselShowsAllSmallQueuesAndOnlyOneHundredPlusEllipsisForLargeQueues`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 5000 risultati/riferimenti manuali; files reali JSON/log nel Context; provider directory falso;125 URI mancanti. Reflection state/write.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `view.render`, `view.chips.size`, `view.navigator.getChildCount`, `view.navigator.getChildAt`, `view.hint.getText`, `view.close`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** SelectedItemView crea100 chip più ellissi per5000; non il carosello Compose attuale.

**Limiti:** Nessuna conversione riuscita/codec; dimensioni in byte solo numeri. Timeout15s e polling, conteggio100 hardcoded. Prove cap non sono misure RAM/CPU.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void carouselShowsAllSmallQueuesAndOnlyOneHundredPlusEllipsisForLargeQueues(){
        var controller=Robolectric.buildActivity(Activity.class).setup();try{
            AppUi ui=new AppUi(controller.get());SelectedItemView view=new SelectedItemView(ui,ui.newPage(),"Foto","Nessuna","Dettagli");
            ArrayList<String> names=new ArrayList<>();for(int i=0;i<5000;i++)names.add("Foto "+i);
            view.render(names.subList(0,100),Collections.emptyList(),0,true,n->{},100);assertEquals(100,view.chips.size());assertEquals(100,view.navigator.getChildCount());
            view.render(names,Collections.emptyList(),0,true,n->{},100);assertEquals(100,view.chips.size());assertEquals(101,view.navigator.getChildCount());
            assertEquals("…",((TextView)view.navigator.getChildAt(100)).getText().toString());assertTrue(view.hint.getText().toString().contains("5000"));view.close();
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageLargeQueueTest.reportCentersCurrentPhotoAndStillIncludesTotalsAfterEviction -->
<a id="test-9bed3cbfe04a2dd2"></a>
### `ImageLargeQueueTest.reportCentersCurrentPhotoAndStillIncludesTotalsAfterEviction`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 5000 risultati/riferimenti manuali; files reali JSON/log nel Context; provider directory falso;125 URI mancanti. Reflection state/write.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `run.add`, `ImageRunState.Result`, `images.report.render`, `images.report.results.size`, `images.report.results.item`, `images.screen.workReport.summary.getText`, `run.windowStart`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Report centra foto1000 e mantiene totali999 dopo eviction, confini finestra verificati.

**Limiti:** Nessuna conversione riuscita/codec; dimensioni in byte solo numeri. Timeout15s e polling, conteggio100 hardcoded. Prove cap non sono misure RAM/CPU.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void reportCentersCurrentPhotoAndStillIncludesTotalsAfterEviction(){
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageRunState run=new ImageRunState();run.total=5000;run.current=1000;run.running=true;run.message="Verifica";
            ArrayList<String> names=new ArrayList<>();for(int i=0;i<5000;i++)names.add("Nome "+(i+1));run.queueNames=Collections.unmodifiableList(names);
            for(int i=0;i<999;i++)run.add(new ImageRunState.Result(names.get(i),3000000,2000000,"content://saved/"+i,"Salvato",ImageRunState.Outcome.SAVED,"OK"));
            var images=controller.get().images;images.report.render(run);assertEquals(100,images.report.results.size());
            assertTrue(images.report.results.item(999).text.contains("Foto 1000 · Nome 1000"));assertTrue(images.report.results.item(999).text.contains("IN CORSO"));
            assertTrue(images.report.results.item(1000).text.contains("In attesa"));assertTrue(images.screen.workReport.summary.getText().toString().contains("Salvate 999"));
            assertEquals(950,run.windowStart());run.current=5000;assertEquals(4900,run.windowStart());run.current=1;assertEquals(0,run.windowStart());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageLargeQueueTest.fullQueueIsOnDiskAndTheIntentContainsOnlyItsIdentifier -->
<a id="test-42f7fec272ed3123"></a>
### `ImageLargeQueueTest.fullQueueIsOnDiskAndTheIntentContainsOnlyItsIdentifier`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 5000 risultati/riferimenti manuali; files reali JSON/log nel Context; provider directory falso;125 URI mancanti. Reflection state/write.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageQueueStore.write`, `ImageQueueStore.read`, `ImageQueueStore.discard`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Queue5000 su disco letta identica; Intent con solo id<4096byte; liste disallineate e ../id respinti.

**Limiti:** Nessuna conversione riuscita/codec; dimensioni in byte solo numeri. Timeout15s e polling, conteggio100 hardcoded. Prove cap non sono misure RAM/CPU.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void fullQueueIsOnDiskAndTheIntentContainsOnlyItsIdentifier()throws Exception{
        var context=RuntimeEnvironment.getApplication();ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>(),folders=new ArrayList<>();
        for(int i=0;i<5000;i++){inputs.add("content://photos/"+i+"/"+"a".repeat(120));names.add("Foto "+i);folders.add("content://dirs/tree/out");}
        String id=ImageQueueStore.write(context,inputs,names,folders);
        try{
            ImageQueueStore.Queue read=ImageQueueStore.read(context,id);assertEquals(inputs,read.inputs);assertEquals(names,read.names);assertEquals(folders,read.folders);
            Intent request=new Intent().putExtra("queueId",id);Parcel p=Parcel.obtain();try{request.writeToParcel(p,0);assertTrue(p.dataSize()<4096);}finally{p.recycle();}
            inputs.add("content://too-many");folders.add("content://dirs/tree/out");try{ImageQueueStore.write(context,inputs,names,folders);fail();}catch(IOException expected){}
            try{ImageQueueStore.read(context,"../bad");fail();}catch(IOException expected){}
        }finally{ImageQueueStore.discard(context,id);}
    }
```

</details>

<!-- /test -->

<!-- test: ImageLargeQueueTest.failureArchiveSurvivesHistoryEvictionAndContainsRetryInputs -->
<a id="test-873e8ed1f5db9dad"></a>
### `ImageLargeQueueTest.failureArchiveSurvivesHistoryEvictionAndContainsRetryInputs`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 5000 risultati/riferimenti manuali; files reali JSON/log nel Context; provider directory falso;125 URI mancanti. Reflection state/write.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageOptions.load`, `ImageRunState.Result`, `ImageFailureStore.append`, `run.add`, `run.results.size`, `run.resultAt`, `ImageConversionService.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** 125 errori reali serializzati sopravvivono a finestra100 e clear stato; primo record conserva URI/opzioni/dettagli.

**Limiti:** Nessuna conversione riuscita/codec; dimensioni in byte solo numeri. Timeout15s e polling, conteggio100 hardcoded. Prove cap non sono misure RAM/CPU.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void failureArchiveSurvivesHistoryEvictionAndContainsRetryInputs()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageOptions options=ImageOptions.load(context);ImageRunState run=new ImageRunState();
        for(int i=0;i<125;i++){
            ImageRunState.Result result=new ImageRunState.Result("Fallita "+i,0,0,null,"Errore "+i,ImageRunState.Outcome.FAILED,"Dettaglio "+i);
            ImageFailureStore.append(context,77,i+1,"content://source/"+i,"content://folder/out",options,result);run.add(result);
        }
        assertEquals(100,run.results.size());assertEquals(125,run.failed);assertNull(run.resultAt(0));
        java.util.List<String> lines=Files.readAllLines(new File(context.getFilesDir(),ImageFailureStore.FILE).toPath(),StandardCharsets.UTF_8);assertEquals(125,lines.size());
        JSONObject first=new JSONObject(lines.get(0));assertEquals("content://source/0",first.getString("input"));assertEquals("content://folder/out",first.getString("folder"));assertEquals("Dettaglio 0",first.getString("details"));assertEquals(options.quality,first.getInt("quality"));
        ImageConversionService.clear();assertTrue(new File(context.getFilesDir(),ImageFailureStore.FILE).isFile());
    }
```

</details>

<!-- /test -->

<!-- test: ImageLargeQueueTest.serviceContinuesAfterMoreThanOneHundredFailuresAndPreservesEveryError -->
<a id="test-3178b157eb2806b6"></a>
### `ImageLargeQueueTest.serviceContinuesAfterMoreThanOneHundredFailuresAndPreservesEveryError`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 5000 risultati/riferimenti manuali; files reali JSON/log nel Context; provider directory falso;125 URI mancanti. Reflection state/write.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageConversionService.class.getDeclaredField`, `ImageQueueStore.write`, `ImageOptions.load`, `ImageConversionService.snapshot`, `run.results.size`, `log.contains`, `run.log.length`, `ImageQueueStore.file`, `ImageQueueStore.discard`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Servizio elabora125 file assenti senza fermarsi, conserva tutti errori/log, coda rimossa e tail limitata.

**Limiti:** Nessuna conversione riuscita/codec; dimensioni in byte solo numeri. Timeout15s e polling, conteggio100 hardcoded. Prove cap non sono misure RAM/CPU.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void serviceContinuesAfterMoreThanOneHundredFailuresAndPreservesEveryError()throws Exception{
        var context=RuntimeEnvironment.getApplication();
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("destinations",new MediaDestinationsTest.Directories());
        var stateField=ImageConversionService.class.getDeclaredField("state");stateField.setAccessible(true);Object previous=stateField.get(null);stateField.set(null,new ImageRunState());
        var controller=Robolectric.buildService(ImageConversionService.class).create();
        ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>(),folders=new ArrayList<>();
        for(int i=0;i<125;i++){inputs.add("file:///missing-large-queue-"+i+".jpg");names.add("Foto "+i);folders.add(MediaDestinationsTest.A);}
        String id=ImageQueueStore.write(context,inputs,names,folders);
        try{
            Intent request=new Intent().setAction(ImageConversionService.START).putExtra("queueId",id);ImageOptions.load(context).put(request);
            controller.get().onStartCommand(request,0,1);
            long end=System.currentTimeMillis()+15000;
            while(ImageConversionService.snapshot().running&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10);}
            ImageRunState run=ImageConversionService.snapshot();assertFalse(run.message,run.running);assertEquals(125,run.completed);assertEquals(125,run.failed);assertEquals(100,run.results.size());
            assertEquals(125,Files.readAllLines(new File(context.getFilesDir(),ImageFailureStore.FILE).toPath()).size());
            String log=new String(Files.readAllBytes(new File(context.getFilesDir(),ImageConversionService.LOG).toPath()),StandardCharsets.UTF_8);assertTrue(log.contains("missing-large-queue-0.jpg"));assertTrue(log.contains("missing-large-queue-124.jpg"));assertTrue(run.log.length()<=24000);assertFalse(ImageQueueStore.file(context,id).exists());
        }finally{controller.destroy();stateField.set(null,previous);ImageQueueStore.discard(context,id);}
    }
```

</details>

<!-- /test -->

<!-- test: ImageLargeQueueTest.largeDraftRestoresAllFiveThousandReferences -->
<a id="test-9972958674ea8246"></a>
### `ImageLargeQueueTest.largeDraftRestoresAllFiveThousandReferences`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 5000 risultati/riferimenti manuali; files reali JSON/log nel Context; provider directory falso;125 URI mancanti. Reflection state/write.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageOptions.load`, `state.inputs.add`, `state.names.add`, `ImageDraftStore.save`, `ImageDraftStore.load`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Bozza prefs con5000 riferimenti si rilegge completa.

**Limiti:** Nessuna conversione riuscita/codec; dimensioni in byte solo numeri. Timeout15s e polling, conteggio100 hardcoded. Prove cap non sono misure RAM/CPU.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void largeDraftRestoresAllFiveThousandReferences(){
        var context=RuntimeEnvironment.getApplication();ImageState state=new ImageState();state.options=ImageOptions.load(context);
        for(int i=0;i<5000;i++){state.inputs.add("content://photo/"+i);state.names.add("Foto "+i);}
        ImageDraftStore.save(context,state);ImageState restored=ImageDraftStore.load(context);assertEquals(state.inputs,restored.inputs);assertEquals(state.names,restored.names);
    }
```

</details>

<!-- /test -->

<!-- test: ImageLargeQueueTest.diskLogKeepsEarlyErrorsBeyondTheOldTwoMibRotationLimit -->
<a id="test-ede1a23e58512efc"></a>
### `ImageLargeQueueTest.diskLogKeepsEarlyErrorsBeyondTheOldTwoMibRotationLimit`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 5000 risultati/riferimenti manuali; files reali JSON/log nel Context; provider directory falso;125 URI mancanti. Reflection state/write.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLargeQueueTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageConversionService.class.getDeclaredField`, `ImageConversionService.class.getDeclaredMethod`, `ImageConversionService.snapshot`, `log.length`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Write privato oltre2MiB conserva FIRST/LAST su disco e tail<=24000; non misura rotazione durante conversione.

**Limiti:** Nessuna conversione riuscita/codec; dimensioni in byte solo numeri. Timeout15s e polling, conteggio100 hardcoded. Prove cap non sono misure RAM/CPU.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void diskLogKeepsEarlyErrorsBeyondTheOldTwoMibRotationLimit()throws Exception{
        var field=ImageConversionService.class.getDeclaredField("state");field.setAccessible(true);Object previous=field.get(null);field.set(null,new ImageRunState());
        var controller=Robolectric.buildService(ImageConversionService.class).create();
        try{
            var write=ImageConversionService.class.getDeclaredMethod("write",String.class);write.setAccessible(true);
            write.invoke(controller.get(),"FIRST_ERROR\n"+"x".repeat(2*1024*1024));write.invoke(controller.get(),"LAST_ERROR");
            String disk=new String(Files.readAllBytes(new File(controller.get().getFilesDir(),ImageConversionService.LOG).toPath()),StandardCharsets.UTF_8);assertTrue(disk.contains("FIRST_ERROR"));assertTrue(disk.contains("LAST_ERROR"));assertTrue(ImageConversionService.snapshot().log.length()<=24000);
        }finally{controller.destroy();field.set(null,previous);}
    }
```

</details>

<!-- /test -->


<a id="class-imagepreviewschedulingtest"></a>
## ImagePreviewSchedulingTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImagePreviewSchedulingTest.java) · 1 metodi · area originaria: Foto.

**Contesto applicativo della classe:** ImageController.readNames/open; worker metadati e selection worker.

**Riferimenti produttivi:** [ImageController](../../app/src/main/java/it/edo/codecprobe/ImageController.java).

**Preparazione, file e simulazioni condivisi:** Provider Photos blocca nome slow con latch; fast restituisce nome ma nessun file apribile, executor private via reflection.

**Confini comuni dell’audit:** Il nome parla di analysis: si verifica reading=false/fast.jpg mentre bulk è bloccato, non parsing riuscito o pixel. Timeout3/5s.

**Metodi:** [selectedPhotoAnalysisFinishesWhileBulkNameProviderIsStillBlocked](#test-b2dcbe5b9a17ff17)

<!-- test: ImagePreviewSchedulingTest.selectedPhotoAnalysisFinishesWhileBulkNameProviderIsStillBlocked -->
<a id="test-b2dcbe5b9a17ff17"></a>
### `ImagePreviewSchedulingTest.selectedPhotoAnalysisFinishesWhileBulkNameProviderIsStillBlocked`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Photos blocca nome slow con latch; fast restituisce nome ma nessun file apribile, executor private via reflection.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImagePreviewSchedulingTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageMetadataCache.memory.clear`, `images.state.inputs.add`, `images.state.clamp`, `images.readNames`, `images.open`, `ImageController.class.getDeclaredField`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Analisi/selezione fast termina mentre query nomi slow resta bloccata: protegge separazione executor.

**Limiti:** Il nome parla di analysis: si verifica reading=false/fast.jpg mentre bulk è bloccato, non parsing riuscito o pixel. Timeout3/5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void selectedPhotoAnalysisFinishesWhileBulkNameProviderIsStillBlocked()throws Exception{
        var context=RuntimeEnvironment.getApplication();context.getSharedPreferences("image-workspace",0).edit().clear().commit();ImageMetadataCache.memory.clear();
        Photos photos=new Photos();ShadowContentResolver.registerProviderInternal("preview.photos",photos);
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            ImageController images=activity.get().images;images.state.inputs.add("content://preview.photos/slow");images.state.inputs.add("content://preview.photos/fast");images.state.clamp();images.readNames();assertTrue(photos.entered.await(3,TimeUnit.SECONDS));
            images.state.selected=1;images.open();var worker=ImageController.class.getDeclaredField("worker");worker.setAccessible(true);((ExecutorService)worker.get(images)).submit(()->{}).get(3,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(images.state.reading);assertEquals("fast.jpg",images.state.name);assertEquals(1,photos.release.getCount());
        }finally{photos.release.countDown();activity.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-imageresponsivenesstest"></a>
## ImageResponsivenessTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageResponsivenessTest.java) · 5 metodi · area originaria: Foto.

**Contesto applicativo della classe:** SelectedItemView; ImageController.refresh; ImageDraftStore; ImageSelection; ImageResultList.

**Riferimenti produttivi:** [SelectedItemView](../../app/src/main/java/it/edo/codecprobe/SelectedItemView.java), [ImageController](../../app/src/main/java/it/edo/codecprobe/ImageController.java), [ImageDraftStore](../../app/src/main/java/it/edo/codecprobe/ImageDraftStore.java), [ImageSelection](../../app/src/main/java/it/edo/codecprobe/ImageSelection.java), [ImageResultList](../../app/src/main/java/it/edo/codecprobe/ImageResultList.java).

**Preparazione, file e simulazioni condivisi:** AbstractList8500 con get>=100 vietato; State/report manuali, prefs simulate e125 URI senza provider.

**Confini comuni dell’audit:** Controlli di lavoro evitato/identità oggetti, non jank/CPU/RAM misurati. SelectedItemView è helper View legacy, non carosello Compose corrente. Polling5s.

**Metodi:** [unchangedLargeCarouselDoesNotReadHiddenNamesOrReplaceButtonBackgrounds](#test-ac8ea48bd23dfd6f) · [progressTicksLeaveTheSelectionUntouchedAndReuseReportRows](#test-7428e906378faf85) · [cachedPersistenceKeepsAllFilesAndDetectsIndividualDestinationEdits](#test-d6aad23f2f967c72) · [manualSelectionReturnsWhilePermissionWorkIsPending](#test-accad9e98fd03f64) · [reusedResultButtonsTargetTheNewFileAfterTheWindowMoves](#test-7d75c2db06298fa0)

<!-- test: ImageResponsivenessTest.unchangedLargeCarouselDoesNotReadHiddenNamesOrReplaceButtonBackgrounds -->
<a id="test-ac8ea48bd23dfd6f"></a>
### `ImageResponsivenessTest.unchangedLargeCarouselDoesNotReadHiddenNamesOrReplaceButtonBackgrounds`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): AbstractList8500 con get>=100 vietato; State/report manuali, prefs simulate e125 URI senza provider.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageResponsivenessTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `view.render`, `view.chips.get`, `view.navigator.getChildCount`, `view.number.getText`, `view.update`, `view.close`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Render invariato non legge nomi nascosti e mantiene bottoni/background, update cambia solo selezione.

**Limiti:** Controlli di lavoro evitato/identità oggetti, non jank/CPU/RAM misurati. SelectedItemView è helper View legacy, non carosello Compose corrente. Polling5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void unchangedLargeCarouselDoesNotReadHiddenNamesOrReplaceButtonBackgrounds(){
        var activity=Robolectric.buildActivity(Activity.class).setup();try{
            SelectedItemView view=new SelectedItemView(new AppUi(activity.get()),new LinearLayout(activity.get()),"Foto","Nessuna","Dettagli");
            List<String> names=new AbstractList<String>(){
                public int size(){return 8500;}
                public String get(int index){assertTrue("Hidden names must not be traversed by the carousel",index<100);return "Foto "+index;}
            };
            view.render(names,Collections.emptyList(),0,true,n->{},100);
            Button chip=view.chips.get(0);Drawable background=chip.getBackground();
            view.render(names,Collections.emptyList(),0,true,n->{},100);
            assertSame(chip,view.chips.get(0));assertSame(background,chip.getBackground());
            assertEquals(101,view.navigator.getChildCount());assertTrue(view.number.getText().toString().contains("8500"));
            Drawable untouched=view.chips.get(2).getBackground();view.update(1,true,false);
            assertSame(untouched,view.chips.get(2).getBackground());assertTrue(view.chips.get(1).isSelected());assertFalse(chip.isSelected());view.close();
        }finally{activity.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageResponsivenessTest.progressTicksLeaveTheSelectionUntouchedAndReuseReportRows -->
<a id="test-7428e906378faf85"></a>
### `ImageResponsivenessTest.progressTicksLeaveTheSelectionUntouchedAndReuseReportRows`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): AbstractList8500 con get>=100 vietato; State/report manuali, prefs simulate e125 URI senza provider.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageResponsivenessTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageConversionService.class.getDeclaredField`, `images.state.inputs.add`, `images.state.names.add`, `images.render`, `images.screen.sections.focusCard`, `images.report.results.item`, `images.compose.getState`, `images.refresh`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Tick di progresso non sostituisce PhotoUiState e riusa righe report; aggiorna quella corrente.

**Limiti:** Controlli di lavoro evitato/identità oggetti, non jank/CPU/RAM misurati. SelectedItemView è helper View legacy, non carosello Compose corrente. Polling5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void progressTicksLeaveTheSelectionUntouchedAndReuseReportRows()throws Exception{
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();
        var field=ImageConversionService.class.getDeclaredField("state");field.setAccessible(true);Object old=field.get(null);
        try{
            ImageController images=controller.get().images;
            for(int i=0;i<8500;i++){images.state.inputs.add("content://photos/"+i);images.state.names.add("Foto "+i);}
            ImageRunState run=new ImageRunState();run.running=true;run.total=8500;run.current=200;run.message="Codifica";field.set(null,run);images.render();
            images.screen.sections.focusCard(ImageScreen.HEIC);ImageResultList.Item retained=images.report.results.item(198);PhotoUiState presentation=images.compose.getState();
            images.state.notice="untouched workspace";run.message="Verifica";images.refresh();
            assertEquals("untouched workspace",images.state.notice);assertSame(presentation,images.compose.getState());
            assertSame(retained,images.report.results.item(198));assertTrue(images.report.results.item(199).text.contains("Verifica"));
            run.current=201;images.refresh();assertSame(retained,images.report.results.item(198));assertTrue(images.report.results.item(200).text.contains("Foto 201"));
        }finally{field.set(null,old);controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageResponsivenessTest.cachedPersistenceKeepsAllFilesAndDetectsIndividualDestinationEdits -->
<a id="test-d6aad23f2f967c72"></a>
### `ImageResponsivenessTest.cachedPersistenceKeepsAllFilesAndDetectsIndividualDestinationEdits`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): AbstractList8500 con get>=100 vietato; State/report manuali, prefs simulate e125 URI senza provider.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageResponsivenessTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageOptions.load`, `state.inputs.add`, `state.names.add`, `ImageDraftStore.save`, `ImageDraftStore.load`, `state.destinations.plan.setFolder`, `state.names.set`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Draft8500 riusa Selection cache su indice/opzioni, invalida su eccezione cartella o nome modificato.

**Limiti:** Controlli di lavoro evitato/identità oggetti, non jank/CPU/RAM misurati. SelectedItemView è helper View legacy, non carosello Compose corrente. Polling5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void cachedPersistenceKeepsAllFilesAndDetectsIndividualDestinationEdits(){
        var context=RuntimeEnvironment.getApplication();ImageState state=new ImageState();state.options=ImageOptions.load(context);state.bulk=true;
        for(int i=0;i<8500;i++){state.inputs.add("content://photos/"+i);state.names.add("Foto "+i);}
        ImageDraftStore.save(context,state);ImageDraftStore.Selection previous=state.savedSelection;
        state.selected=123;state.options=new ImageOptions(50,"slow","standard",null,true,false,state.options.revision);ImageDraftStore.save(context,state);
        assertSame(previous,state.savedSelection);ImageState restored=ImageDraftStore.load(context);assertEquals(8500,restored.inputs.size());assertEquals(123,restored.selected);assertEquals(50,restored.options.quality);
        state.destinations.plan.setFolder(5000,"content://folders/output");ImageDraftStore.save(context,state);assertNotSame(previous,state.savedSelection);
        restored=ImageDraftStore.load(context);assertEquals("content://folders/output",restored.destinations.plan.files.get(5000).folder);assertEquals(8500,restored.names.size());
        previous=state.savedSelection;state.names.set(8499,"Renamed.jpg");ImageDraftStore.save(context,state);assertNotSame(previous,state.savedSelection);assertEquals("Renamed.jpg",ImageDraftStore.load(context).names.get(8499));
    }
```

</details>

<!-- /test -->

<!-- test: ImageResponsivenessTest.manualSelectionReturnsWhilePermissionWorkIsPending -->
<a id="test-accad9e98fd03f64"></a>
### `ImageResponsivenessTest.manualSelectionReturnsWhilePermissionWorkIsPending`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): AbstractList8500 con get>=100 vietato; State/report manuali, prefs simulate e125 URI senza provider.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageResponsivenessTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.selection.result`, `images.state.inputs.size`, `images.screen.convert.isEnabled`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Selezione125 ritorna importing e un Handler può eseguire; non impone budget latenza prima callback né blocca davvero provider.

**Limiti:** Controlli di lavoro evitato/identità oggetti, non jank/CPU/RAM misurati. SelectedItemView è helper View legacy, non carosello Compose corrente. Polling5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void manualSelectionReturnsWhilePermissionWorkIsPending()throws Exception{
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=controller.get().images;
            android.content.ClipData clips=android.content.ClipData.newRawUri("photos",android.net.Uri.parse("content://photos/0"));
            for(int i=1;i<125;i++)clips.addItem(new android.content.ClipData.Item(android.net.Uri.parse("content://photos/"+i)));
            android.content.Intent picked=new android.content.Intent();picked.setClipData(clips);
            images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,picked);
            assertTrue(images.state.importing);assertEquals(125,images.state.inputs.size());assertFalse(images.screen.convert.isEnabled());
            java.util.concurrent.atomic.AtomicBoolean responsive=new java.util.concurrent.atomic.AtomicBoolean();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->responsive.set(true));
            long deadline=System.currentTimeMillis()+5000;
            while(images.state.importing&&System.currentTimeMillis()<deadline){Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();Thread.sleep(5);}
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(responsive.get());assertFalse(images.state.importing);assertEquals(125,images.state.inputs.size());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageResponsivenessTest.reusedResultButtonsTargetTheNewFileAfterTheWindowMoves -->
<a id="test-7d75c2db06298fa0"></a>
### `ImageResponsivenessTest.reusedResultButtonsTargetTheNewFileAfterTheWindowMoves`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): AbstractList8500 con get>=100 vietato; State/report manuali, prefs simulate e125 URI senza provider.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageResponsivenessTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `run.add`, `ImageRunState.Result`, `images.report.render`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Finestra report che si sposta ricicla riga ma Apri punta al nuovo URI999, non al vecchio.

**Limiti:** Controlli di lavoro evitato/identità oggetti, non jank/CPU/RAM misurati. SelectedItemView è helper View legacy, non carosello Compose corrente. Polling5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void reusedResultButtonsTargetTheNewFileAfterTheWindowMoves(){
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=controller.get().images;ImageRunState run=new ImageRunState();run.total=5000;run.current=1000;run.running=true;
            for(int i=0;i<1000;i++)run.add(new ImageRunState.Result("Foto "+i,300,200,"content://saved/"+i,"OK",ImageRunState.Outcome.SAVED,""));
            images.report.render(run);View row=ImageVirtualReportTest.row(images,48,null);run.current=1001;images.report.render(run);assertSame(row,ImageVirtualReportTest.row(images,48,row));
            Button button=find(row,"Apri risultato:");assertNotNull(button);button.performClick();assertEquals("content://saved/999",Shadows.shadowOf(controller.get()).getNextStartedActivity().getDataString());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-imagesettingstest"></a>
## ImageSettingsTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageSettingsTest.java) · 6 metodi · area originaria: Foto.

**Contesto applicativo della classe:** ImageController; PhotoCompose state/command; ImageSettingsActivity; ImageOptions; ImageDraftStore; ImageConversionService.

**Riferimenti produttivi:** [ImageController](../../app/src/main/java/it/edo/codecprobe/ImageController.java), [ImageSettingsActivity](../../app/src/main/java/it/edo/codecprobe/ImageSettingsActivity.java), [ImageOptions](../../app/src/main/java/it/edo/codecprobe/ImageOptions.java), [ImageDraftStore](../../app/src/main/java/it/edo/codecprobe/ImageDraftStore.java), [ImageConversionService](../../app/src/main/java/it/edo/codecprobe/ImageConversionService.java).

**Preparazione, file e simulazioni condivisi:** Activity/service/SharedPreferences Robolectric; run iniettate e lease condivisa acquisita a mano. Il setup/teardown svuota anche photo-appearance per isolare le prove sul blur.

**Confini comuni dell’audit:** Nessun encoder né opzioni native applicate; stale cancel ha solo controllo negativo. KeepScreenOn è flag modello finestra, non comportamento device.

**Metodi:** [headerOpensDedicatedSettingsAndUnsavedEditsSurviveRecreation](#test-cfb5a3b1a2fc7678) · [savedDefaultsApplyAfterActiveQueueAndKeepSelection](#test-1e0b1f7256051833) · [imageDraftIsSeparateAndFolderCancellationKeepsChoice](#test-ed89e19b7b58fe80) · [leaseRejectsImagesBeforeNativeAndStaleCancellationCannotStopAnotherRun](#test-fdc1e71c144d0cdb) · [screenAwakeOptionRemainsLiveDuringPhotoConversionAndClearsOnPause](#test-6a180d6faec0ff56) · [glassBlurTogglePersistsOnBackAndAppliesOnResumeWithoutChangingConversion](#test-9a8a5994b873d2f6)

<!-- test: ImageSettingsTest.headerOpensDedicatedSettingsAndUnsavedEditsSurviveRecreation -->
<a id="test-cfb5a3b1a2fc7678"></a>
### `ImageSettingsTest.headerOpensDedicatedSettingsAndUnsavedEditsSurviveRecreation`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/service/SharedPreferences Robolectric; run iniettate e lease condivisa acquisita a mano.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageSettingsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.get`, `images.quality`, `images.setFolder`, `images.compose.command`, `ImageSettingsActivity.class.getName`, `screen.copy.performClick`, `screen.quality.value`, `screen.quality.quality.setProgress`, `screen.smaller.setChecked`, `screen.smaller.isChecked`, `screen.back.performClick`, `ImageOptions.load`, `home.pause`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Header apre settings dedicate con quality70; copy/form non salvato ricreato mantiene80 ma default65/revision0.

**Limiti:** Nessun encoder né opzioni native applicate; stale cancel ha solo controllo negativo. KeepScreenOn è flag modello finestra, non comportamento device.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void headerOpensDedicatedSettingsAndUnsavedEditsSurviveRecreation(){
        var home=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=home.get().images;images.quality(70);images.setFolder("content://dirs/tree/chosen");images.compose.command(PhotoCommand.SETTINGS);Intent open=Shadows.shadowOf(home.get()).getNextStartedActivity();assertEquals(ImageSettingsActivity.class.getName(),open.getComponent().getClassName());assertEquals(70,open.getIntExtra("quality",0));
            var settings=Robolectric.buildActivity(ImageSettingsActivity.class,open).setup();try{
                settings.get().screen.copy.performClick();assertEquals(70,settings.get().screen.quality.value());settings.get().screen.quality.quality.setProgress(40);settings.get().screen.smaller.setChecked(false);settings.recreate();assertEquals(80,settings.get().screen.quality.value());assertFalse(settings.get().screen.smaller.isChecked());settings.get().screen.back.performClick();assertEquals(65,ImageOptions.load(context).quality);assertEquals(0,ImageOptions.load(context).revision);
            }finally{settings.pause().stop().destroy();}
        }finally{home.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageSettingsTest.savedDefaultsApplyAfterActiveQueueAndKeepSelection -->
<a id="test-1e0b1f7256051833"></a>
### `ImageSettingsTest.savedDefaultsApplyAfterActiveQueueAndKeepSelection`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/service/SharedPreferences Robolectric; run iniettate e lease condivisa acquisita a mano.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageSettingsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.get`, `images.state.inputs.add`, `images.refresh`, `ImageConversionService.snapshot`, `images.quality`, `images.compose.getState`, `images.state.inputs.size`, `home.pause`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Default80/ssim salvati differiti durante run60, blocca edit; fine run aggiorna opzioni senza perdere selezione.

**Limiti:** Nessun encoder né opzioni native applicate; stale cancel ha solo controllo negativo. KeepScreenOn è flag modello finestra, non comportamento device.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void savedDefaultsApplyAfterActiveQueueAndKeepSelection()throws Exception{
        ImageRunState run=new ImageRunState();run.running=true;run.startedAt=44;run.options=new ImageOptions(60,"fast","standard",null,true,false,0);field.set(null,run);
        var home=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=home.get().images;images.state.inputs.add("content://photo/original");new ImageOptions(80,"slow","ssim","content://dirs/tree/new",false,true,0).save(context);images.refresh();assertEquals(65,images.state.options.quality);assertEquals(60,ImageConversionService.snapshot().options.quality);images.quality(50);assertEquals(65,images.state.options.quality);assertTrue(images.compose.getState().getBusy());
            run.running=false;images.refresh();assertEquals(80,images.state.options.quality);assertEquals("ssim",images.state.options.tune);assertEquals(1,images.state.inputs.size());assertEquals("content://dirs/tree/new",images.state.options.folder);assertFalse(images.state.options.onlySmaller);assertFalse(images.compose.getState().getBusy());
        }finally{home.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageSettingsTest.imageDraftIsSeparateAndFolderCancellationKeepsChoice -->
<a id="test-ed89e19b7b58fe80"></a>
### `ImageSettingsTest.imageDraftIsSeparateAndFolderCancellationKeepsChoice`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/service/SharedPreferences Robolectric; run iniettate e lease condivisa acquisita a mano.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageSettingsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.get`, `images.setFolder`, `images.selection.result`, `images.quality`, `images.smaller`, `images.persist`, `ImageDraftStore.load`, `ImageOptions.load`, `images.refresh`, `home.pause`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Cancel cartella conserva scelta, draft60 separato default65; rimozione folder nei nuovi default applicata.

**Limiti:** Nessun encoder né opzioni native applicate; stale cancel ha solo controllo negativo. KeepScreenOn è flag modello finestra, non comportamento device.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void imageDraftIsSeparateAndFolderCancellationKeepsChoice(){
        var home=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=home.get().images;images.setFolder("content://dirs/tree/current");images.selection.result(ImageSelection.FOLDER,android.app.Activity.RESULT_CANCELED,null);assertEquals("content://dirs/tree/current",images.state.options.folder);
            images.quality(60);images.smaller(false);images.persist();assertEquals(60,ImageDraftStore.load(context).options.quality);assertEquals(65,ImageOptions.load(context).quality);assertTrue(ImageOptions.load(context).onlySmaller);
            new ImageOptions(75,"medium","psnr",null,true,false,0).save(context);images.refresh();assertNull(images.state.options.folder);assertEquals(75,images.state.options.quality);
        }finally{home.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageSettingsTest.leaseRejectsImagesBeforeNativeAndStaleCancellationCannotStopAnotherRun -->
<a id="test-fdc1e71c144d0cdb"></a>
### `ImageSettingsTest.leaseRejectsImagesBeforeNativeAndStaleCancellationCannotStopAnotherRun`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/service/SharedPreferences Robolectric; run iniettate e lease condivisa acquisita a mano.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageSettingsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `MediaConversionGate.INSTANCE.acquire`, `service.get`, `ImageConversionService.snapshot`, `MediaConversionGate.INSTANCE.owns`, `service.destroy`, `MediaConversionGate.INSTANCE.release`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Lease occupata respinge START prima JNI e token resta posseduto; CANCEL startedAt21 non cambia run22 iniettata.

**Limiti:** Nessun encoder né opzioni native applicate; stale cancel ha solo controllo negativo. KeepScreenOn è flag modello finestra, non comportamento device.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void leaseRejectsImagesBeforeNativeAndStaleCancellationCannotStopAnotherRun()throws Exception{
        var service=Robolectric.buildService(ImageConversionService.class).create();long token=MediaConversionGate.INSTANCE.acquire();assertNotEquals(0,token);
        try{Intent start=new Intent().setAction(ImageConversionService.START).putStringArrayListExtra("inputs",new java.util.ArrayList<>(java.util.List.of("content://photo/1")));new ImageOptions(65,"slow","standard","content://dirs/tree/out",true,false,0).put(start);service.get().onStartCommand(start,0,1);assertFalse(ImageConversionService.snapshot().running);assertTrue(MediaConversionGate.INSTANCE.owns(token));
            ImageRunState run=new ImageRunState();run.running=true;run.startedAt=22;field.set(null,run);service.get().onStartCommand(new Intent().setAction(ImageConversionService.CANCEL).putExtra("startedAt",21L),0,2);assertFalse(ImageConversionService.snapshot().cancelling);
        }finally{service.destroy();MediaConversionGate.INSTANCE.release(token);}
    }
```

</details>

<!-- /test -->

<!-- test: ImageSettingsTest.screenAwakeOptionRemainsLiveDuringPhotoConversionAndClearsOnPause -->
<a id="test-6a180d6faec0ff56"></a>
### `ImageSettingsTest.screenAwakeOptionRemainsLiveDuringPhotoConversionAndClearsOnPause`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/service/SharedPreferences Robolectric; run iniettate e lease condivisa acquisita a mano.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageSettingsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.screen.screenOn.isEnabled`, `images.screen.screenOn.setChecked`, `ImageConversionService.snapshot`, `images.pause`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** KeepScreenOn modificabile a runtime su vista, non muta opzioni run congelate; pause clears flag.

**Limiti:** Nessun encoder né opzioni native applicate; stale cancel ha solo controllo negativo. KeepScreenOn è flag modello finestra, non comportamento device.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void screenAwakeOptionRemainsLiveDuringPhotoConversionAndClearsOnPause()throws Exception{
        ImageRunState run=new ImageRunState();run.running=true;run.options=new ImageOptions(65,"slow","standard",null,true,false,0);field.set(null,run);
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=controller.get().images;assertTrue(images.screen.screenOn.isEnabled());images.screen.screenOn.setChecked(true);
            assertTrue((controller.get().getWindow().getAttributes().flags&android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)!=0);assertFalse(ImageConversionService.snapshot().options.keepScreenOn);
            images.pause();assertEquals(0,controller.get().getWindow().getAttributes().flags&android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<!-- test: ImageSettingsTest.glassBlurTogglePersistsOnBackAndAppliesOnResumeWithoutChangingConversion -->
<a id="test-9a8a5994b873d2f6"></a>
### `ImageSettingsTest.glassBlurTogglePersistsOnBackAndAppliesOnResumeWithoutChangingConversion`

**Categoria:** B. Analisi statica; esiti di esecuzione separati.

**Dati e preparazione:** Activity Foto e impostazioni su Robolectric, preferenze separate svuotate nel setup; selezione sintetica di una foto e snapshot del servizio iniettato solo nella parte finale.

**Sequenza delle operazioni:** Controlla il default attivo, apre le impostazioni dopo la pausa di Foto, spegne il checkbox, ricrea le impostazioni, copia i parametri e torna indietro senza Salva. Riprende e ricrea Foto; poi riattiva la preferenza con una coda simulata attiva.

**Metodi applicativi e percorso:** ImageSettingsScreen.glassBlur → PhotoAppearance → PhotoComposeHost.onVisible; i parametri di ImageOptions e del servizio restano separati.

**File, mock e simulazioni:** Framework, lifecycle, SharedPreferences e servizio simulati; nessun file multimediale, picker, GPU del telefono o codec JNI.

**Asserzioni e risultati attesi:** Default attivo; checkbox e preferenza restano spenti dopo recreate/copy/Indietro; host legge spento al resume e dopo ricreazione; opzioni della bozza, selezione e revisione dei default restano invariate. Riattivazione letta anche con coda attiva senza cambiare il suo snapshot di opzioni.

**Comportamento verificato:** La preferenza grafica è immediata e persistente, indipendente dal salvataggio dei default di compressione e dalla coda.

**Limiti:** Non verifica il blur hardware né la fluidità sul dispositivo. Nessuna golden o misura dei frame; la rimozione dei modifier Haze è verificabile nel ramo del codice, non tramite confronto pixel di questa prova.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void glassBlurTogglePersistsOnBackAndAppliesOnResumeWithoutChangingConversion()throws Exception{
        var home=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            ImageController images=home.get().images;
            assertTrue(images.compose.getBlurEnabled());
            ImageOptions options=images.state.options;
            images.state.inputs.add("content://photo/keep-selection");images.state.clamp();
            home.pause();
            var settings=Robolectric.buildActivity(ImageSettingsActivity.class).setup();
            try{
                assertTrue(settings.get().screen.glassBlur.isChecked());
                settings.get().screen.glassBlur.performClick();
                assertFalse(PhotoAppearance.blurEnabled(context));
                settings.recreate();assertFalse(settings.get().screen.glassBlur.isChecked());
                settings.get().screen.copy.performClick();assertFalse(settings.get().screen.glassBlur.isChecked());
                settings.get().screen.back.performClick();
            }finally{settings.pause().stop().destroy();}
            home.resume();assertFalse(images.compose.getBlurEnabled());
            assertSame(options,images.state.options);assertEquals(1,images.state.inputs.size());
            assertEquals(0,ImageOptions.load(context).revision);
            home.recreate();images=home.get().images;assertFalse(images.compose.getBlurEnabled());
            ImageRunState run=new ImageRunState();run.running=true;run.options=options;field.set(null,run);
            PhotoAppearance.blurEnabled(context,true);images.pause();images.resume();
            assertTrue(images.compose.getBlurEnabled());assertSame(options,ImageConversionService.snapshot().options);
            assertEquals(0,ImageOptions.load(context).revision);
        }finally{home.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<a id="class-imageuitrialtest"></a>
## ImageUiTrialTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java) · 6 metodi · area originaria: Foto.

**Contesto applicativo della classe:** ImageController; PhotoCompose bridge command/select; ImageSelection; ImageDestinations.

**Riferimenti produttivi:** [ImageController](../../app/src/main/java/it/edo/codecprobe/ImageController.java), [ImageSelection](../../app/src/main/java/it/edo/codecprobe/ImageSelection.java).

**Preparazione, file e simulazioni condivisi:** Piccoli file JPEG sintetici; provider directory/folder fittizi, Activity/dialog shadow. Test chiama prevalentemente bridge Java anziché nodo Compose.

**Confini comuni dell’audit:** Non valida gesto touch o rendering nei primi cinque. Import8500 senza byte; polling5ms/10s. Conteggio root/pannelli è vincolo architetturale interno.

**Metodi:** [navigationRetainsDraftAndQualityAcrossRecreation](#test-0440fbba0520d5a2) · [longPressSelectionUsesGroupAndRequiresConfirmationBeforeRemoval](#test-bfe33d98b5fdb42c) · [folderImportIsAdditiveAndKeepsExistingDestinationEvenForDuplicates](#test-3a39be3aef96bf6e) · [addingFilesKeepsFocusNamesAndExistingMetadataCache](#test-8d5de95d250d61cd) · [folderPickerFreezesSelectedGroupAcrossRecreation](#test-4befacdd06a4211f) · [composeIsTheOnlyWorkspaceRootAndNoHiddenPhotoPanelsAreBuilt](#test-5e8d63cb786eddf5)

<!-- test: ImageUiTrialTest.navigationRetainsDraftAndQualityAcrossRecreation -->
<a id="test-0440fbba0520d5a2"></a>
### `ImageUiTrialTest.navigationRetainsDraftAndQualityAcrossRecreation`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Piccoli file JPEG sintetici; provider directory/folder fittizi, Activity/dialog shadow. Test chiama prevalentemente bridge Java anziché nodo Compose.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.screen.sections.current`, `images.screen.sections.focusCard`, `images.selection.result`, `images.next`, `images.quality`, `images.back`, `images.compose.getState`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Avanti/indietro mantiene bozza/quality e restore step compressione.

**Limiti:** Non valida gesto touch o rendering nei primi cinque. Import8500 senza byte; polling5ms/10s. Conteggio root/pannelli è vincolo architetturale interno.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void navigationRetainsDraftAndQualityAcrossRecreation()throws Exception{
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;assertNull(images.screen.sections.current());
            images.screen.sections.focusCard(ImageScreen.FILES);assertEquals(ImageScreen.FILES,images.screen.sections.current());
            String input=photo("draft");images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.parse(input)));settle(images);
            images.next();images.quality(70);
            int quality=images.state.options.quality;images.back();
            assertEquals(ImageScreen.FILES,images.screen.sections.current());images.back();
            assertNull(images.screen.sections.current());assertEquals(Collections.singletonList(input),images.state.inputs);
            images.screen.sections.focusCard(ImageScreen.FILES);images.next();
            assertEquals(quality,images.state.options.quality);
            assertEquals(ImageScreen.COMPRESSION,images.compose.getState().getStep());
            activity.recreate();assertEquals(ImageScreen.COMPRESSION,activity.get().images.screen.sections.current());
        }finally{activity.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageUiTrialTest.longPressSelectionUsesGroupAndRequiresConfirmationBeforeRemoval -->
<a id="test-bfe33d98b5fdb42c"></a>
### `ImageUiTrialTest.longPressSelectionUsesGroupAndRequiresConfirmationBeforeRemoval`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Piccoli file JPEG sintetici; provider directory/folder fittizi, Activity/dialog shadow. Test chiama prevalentemente bridge Java anziché nodo Compose.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.state.inputs.add`, `images.state.clamp`, `images.open`, `images.compose.select`, `images.selectionIndices`, `images.selectedPhotos.size`, `images.compose.getState`, `images.compose.command`, `images.state.inputs.size`, `images.state.inputs.isEmpty`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Selezione gruppo0,2 e Remove richiede conferma; originali esistono ancora dopo rimozione/clear.

**Limiti:** Non valida gesto touch o rendering nei primi cinque. Import8500 senza byte; polling5ms/10s. Conteggio root/pannelli è vincolo architetturale interno.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void longPressSelectionUsesGroupAndRequiresConfirmationBeforeRemoval()throws Exception{
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;for(int n=0;n<3;n++)images.state.inputs.add(photo("selection"+n));images.state.clamp();images.open();settle(images);
            images.compose.select(0,true);images.compose.select(2,false);
            assertEquals(Arrays.asList(0,2),images.selectionIndices());assertEquals(2,images.selectedPhotos.size());
            assertTrue(images.compose.getState().getName().contains("2 foto"));images.compose.command(PhotoCommand.DETAILS);
            assertTrue(images.compose.getState().getDetailsOpen());
            images.compose.command(PhotoCommand.REMOVE);assertEquals(3,images.state.inputs.size());
            ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_NEGATIVE).performClick();assertEquals(3,images.state.inputs.size());
            images.compose.command(PhotoCommand.REMOVE);ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();settle(images);
            assertEquals(1,images.state.inputs.size());for(File file:files)assertTrue(file.exists());
            images.compose.command(PhotoCommand.CLEAR);assertEquals(1,images.state.inputs.size());
            ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(images.state.inputs.isEmpty());
        }finally{activity.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageUiTrialTest.folderImportIsAdditiveAndKeepsExistingDestinationEvenForDuplicates -->
<a id="test-3a39be3aef96bf6e"></a>
### `ImageUiTrialTest.folderImportIsAdditiveAndKeepsExistingDestinationEvenForDuplicates`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Piccoli file JPEG sintetici; provider directory/folder fittizi, Activity/dialog shadow. Test chiama prevalentemente bridge Java anziché nodo Compose.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.state.inputs.add`, `images.state.clamp`, `images.state.destinations.sync`, `images.state.destinations.plan.setFolder`, `images.importFolder`, `images.state.inputs.contains`, `images.state.inputs.size`, `images.state.destinations.plan.files.get`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Folder import additivo deduplica URI e conserva eccezione cartella del vecchio file, anche ripetuto.

**Limiti:** Non valida gesto touch o rendering nei primi cinque. Import8500 senza byte; polling5ms/10s. Conteggio root/pannelli è vincolo architetturale interno.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void folderImportIsAdditiveAndKeepsExistingDestinationEvenForDuplicates()throws Exception{
        ShadowContentResolver.registerProviderInternal("folderphotos",new ImageFolderImportTest.Folder());
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;String old=photo("existing");images.state.inputs.add(old);images.state.inputs.add("content://folderphotos/tree/camera/document/photo0");images.state.clamp();
            images.state.destinations.sync(images.state.inputs,null);images.state.destinations.plan.setFolder(0,"content://saved/tree/custom");
            images.importFolder(Uri.parse(ImageFolderImportTest.TREE));settle(images);
            assertTrue(images.state.inputs.contains(old));assertEquals(new HashSet<>(images.state.inputs).size(),images.state.inputs.size());
            assertEquals("content://saved/tree/custom",images.state.destinations.plan.files.get(0).folder);
            int before=images.state.inputs.size();images.importFolder(Uri.parse(ImageFolderImportTest.TREE));settle(images);assertEquals(before,images.state.inputs.size());
        }finally{activity.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageUiTrialTest.addingFilesKeepsFocusNamesAndExistingMetadataCache -->
<a id="test-8d5de95d250d61cd"></a>
### `ImageUiTrialTest.addingFilesKeepsFocusNamesAndExistingMetadataCache`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Piccoli file JPEG sintetici; provider directory/folder fittizi, Activity/dialog shadow. Test chiama prevalentemente bridge Java anziché nodo Compose.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.selection.result`, `images.open`, `images.state.names.get`, `images.state.inputs.size`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Aggiunta terzo file conserva focus/nome/cache del secondo e stesso URI non duplica.

**Limiti:** Non valida gesto touch o rendering nei primi cinque. Import8500 senza byte; polling5ms/10s. Conteggio root/pannelli è vincolo architetturale interno.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void addingFilesKeepsFocusNamesAndExistingMetadataCache()throws Exception{
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;String one=photo("first"),two=photo("second"),three=photo("third");
            images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.parse(one)));settle(images);
            images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.parse(two)));settle(images);
            images.state.selected=1;images.open();settle(images);var cached=images.state.info;String name=images.state.names.get(1);
            images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.parse(three)));settle(images);
            assertEquals(3,images.state.inputs.size());assertEquals(1,images.state.selected);assertEquals(name,images.state.names.get(1));assertSame(cached,images.state.info);
            images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.parse(two)));assertEquals(3,images.state.inputs.size());assertSame(cached,images.state.info);
        }finally{activity.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageUiTrialTest.folderPickerFreezesSelectedGroupAcrossRecreation -->
<a id="test-4befacdd06a4211f"></a>
### `ImageUiTrialTest.folderPickerFreezesSelectedGroupAcrossRecreation`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Piccoli file JPEG sintetici; provider directory/folder fittizi, Activity/dialog shadow. Test chiama prevalentemente bridge Java anziché nodo Compose.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.state.inputs.add`, `images.state.clamp`, `images.setFolder`, `images.open`, `images.toggleSelection`, `images.destinations.photoException`, `images.destinations.choosePhotoFolder`, `images.selection.result`, `images.state.destinations.plan.files.get`, `images.state.destinations.plan.effective`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Gruppo0,2 congelato nel picker sopravvive recreate e riceve cartellaB lasciando1 inA.

**Limiti:** Non valida gesto touch o rendering nei primi cinque. Import8500 senza byte; polling5ms/10s. Conteggio root/pannelli è vincolo architetturale interno.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void folderPickerFreezesSelectedGroupAcrossRecreation()throws Exception{
        ShadowContentResolver.registerProviderInternal("destinations",new MediaDestinationsTest.Directories());
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;for(int n=0;n<3;n++)images.state.inputs.add(photo("destination"+n));images.state.clamp();images.setFolder(MediaDestinationsTest.A);images.open();settle(images);
            images.toggleSelection(0);images.toggleSelection(2);images.destinations.photoException(true);images.destinations.choosePhotoFolder();activity.recreate();images=activity.get().images;
            images.selection.result(MediaDestinationsController.FILE,Activity.RESULT_OK,new Intent().setData(Uri.parse(MediaDestinationsTest.B)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            assertEquals(MediaDestinationsTest.B,images.state.destinations.plan.files.get(0).folder);
            assertEquals(MediaDestinationsTest.A,images.state.destinations.plan.effective(1).folder);
            assertEquals(MediaDestinationsTest.B,images.state.destinations.plan.files.get(2).folder);
        }finally{activity.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageUiTrialTest.composeIsTheOnlyWorkspaceRootAndNoHiddenPhotoPanelsAreBuilt -->
<a id="test-5e8d63cb786eddf5"></a>
### `ImageUiTrialTest.composeIsTheOnlyWorkspaceRootAndNoHiddenPhotoPanelsAreBuilt`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Piccoli file JPEG sintetici; provider directory/folder fittizi, Activity/dialog shadow. Test chiama prevalentemente bridge Java anziché nodo Compose.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageUiTrialTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.compose.getView`, `images.screen.sections.bodies.size`, `images.screen.sections.bodies.containsKey`, `images.screen.sections.bodies.get`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Compose unica root installata e nessun pannello legacy FILES/COMPRESSION creato; HEIC/DETAILS restano interop.

**Limiti:** Non valida gesto touch o rendering nei primi cinque. Import8500 senza byte; polling5ms/10s. Conteggio root/pannelli è vincolo architetturale interno.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void composeIsTheOnlyWorkspaceRootAndNoHiddenPhotoPanelsAreBuilt(){
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;
            android.view.ViewGroup content=activity.get().findViewById(android.R.id.content);
            assertEquals(1,content.getChildCount());
            assertSame(images.compose.getView(),content.getChildAt(0));
            assertEquals(2,images.screen.sections.bodies.size());
            assertFalse(images.screen.sections.bodies.containsKey(ImageScreen.FILES));
            assertFalse(images.screen.sections.bodies.containsKey(ImageScreen.COMPRESSION));
            assertNotNull(images.screen.sections.bodies.get(ImageScreen.HEIC));
            assertNotNull(images.screen.sections.bodies.get(ImageScreen.DETAILS));
        }finally{activity.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-imageworkspacetest"></a>
## ImageWorkspaceTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageWorkspaceTest.java) · 5 metodi · area originaria: Foto.

**Contesto applicativo della classe:** ImageController/Selection/export; JpegInventory; ImageDraftStore; PhotoCompose.command; AppNavigation.

**Riferimenti produttivi:** [ImageController](../../app/src/main/java/it/edo/codecprobe/ImageController.java), [JpegInventory](../../app/src/main/java/it/edo/codecprobe/JpegInventory.java), [ImageDraftStore](../../app/src/main/java/it/edo/codecprobe/ImageDraftStore.java), [AppNavigation](../../app/src/main/java/it/edo/codecprobe/AppNavigation.java).

**Preparazione, file e simulazioni condivisi:** Vero JPEG minimale sintetico con APP1 'original metadata' (non TIFF EXIF valido), APP7, SOF48x32, finta scansione e3byte trailing. File JSON reale.

**Confini comuni dell’audit:** Non è JPEG con pixel decodificati; export dichiara esplicitamente pixelsDecoded=false/heicConversionApproved=false. Primo nome promette Audio+Video ma sentinella è solo prefs Audio. Polling5ms/5s.

**Metodi:** [jpegAnalysisExportAndDraftAreIndependentFromAudioVideo](#test-8320a3dcae3744b9) · [cancelledSelectionKeepsDraftAndUnsupportedFileCannotExport](#test-20a750678a7f3b10) · [threeTabsSelectImagesAndSwitchWithoutGrowingSecondaryBackStack](#test-c47bddfc45b54f96) · [pendingExportSurvivesRecreationAndSourceCannotBeOverwritten](#test-4fbcbfb5bb7d3804) · [imagesAreReachableFromBothExistingWorkspaces](#test-33b9455ea29f2067)

<!-- test: ImageWorkspaceTest.jpegAnalysisExportAndDraftAreIndependentFromAudioVideo -->
<a id="test-8320a3dcae3744b9"></a>
### `ImageWorkspaceTest.jpegAnalysisExportAndDraftAreIndependentFromAudioVideo`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Vero JPEG minimale sintetico con APP1 'original metadata' (non TIFF EXIF valido), APP7, SOF48x32, finta scansione e3byte trailing. File JSON reale.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.screen.export.isEnabled`, `images.selection.result`, `images.compose.getState`, `images.screen.export.performClick`, `ImageDraftStore.load`, `images.compose.command`, `images.state.inputs.isEmpty`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Seleziona/analizza/esporta inventario con2payload byte-identici e sorgente preservata; CLEAR non elimina fonte o prefs Audio.

**Limiti:** Non è JPEG con pixel decodificati; export dichiara esplicitamente pixelsDecoded=false/heicConversionApproved=false. Primo nome promette Audio+Video ma sentinella è solo prefs Audio. Polling5ms/5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void jpegAnalysisExportAndDraftAreIndependentFromAudioVideo()throws Exception{
        var audioPrefs=RuntimeEnvironment.getApplication().getSharedPreferences("audio-workspace",0);String previous=audioPrefs.getString("selection","[]");audioPrefs.edit().putString("selection","audio-sentinel").commit();
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();File source=File.createTempFile("original-",".jpg"),report=File.createTempFile("inventory-",".json");byte[] original=fixture();Files.write(source.toPath(),original);
        try{
            ImageController images=controller.get().images;assertFalse(images.screen.export.isEnabled());
            Intent picked=new Intent().setData(Uri.fromFile(source));images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,picked);settle(images);
            assertNotNull(images.state.info);assertEquals(48,images.state.info.width);assertTrue(images.screen.export.isEnabled());assertTrue(images.compose.getState().getAllDetails().contains("3 byte dopo"));
            images.screen.export.performClick();Intent create=Shadows.shadowOf(controller.get()).getNextStartedActivity();assertEquals(Intent.ACTION_CREATE_DOCUMENT,create.getAction());assertEquals("application/json",create.getType());
            images.selection.result(ImageSelection.REPORT,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(report)));settle(images);
            JSONObject json=new JSONObject(new String(Files.readAllBytes(report.toPath()),StandardCharsets.UTF_8));assertFalse(json.getBoolean("heicConversionApproved"));assertFalse(json.getBoolean("pixelsDecoded"));assertEquals(2,json.getJSONArray("metadataPayloads").length());
            JSONObject block=json.getJSONArray("metadataPayloads").getJSONObject(0);byte[] payload=Base64.getDecoder().decode(block.getString("base64"));assertArrayEquals(Arrays.copyOfRange(original,block.getInt("payloadOffset"),block.getInt("payloadOffset")+block.getInt("bytes")),payload);
            assertArrayEquals(original,Files.readAllBytes(source.toPath()));assertEquals(1,ImageDraftStore.load(controller.get()).inputs.size());
            assertEquals("audio-sentinel",audioPrefs.getString("selection",""));
            images.compose.command(PhotoCommand.CLEAR);org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(images.state.inputs.isEmpty());assertNull(images.state.info);assertFalse(images.screen.export.isEnabled());assertArrayEquals(original,Files.readAllBytes(source.toPath()));
        }finally{controller.pause().stop().destroy();source.delete();report.delete();audioPrefs.edit().putString("selection",previous).commit();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageWorkspaceTest.cancelledSelectionKeepsDraftAndUnsupportedFileCannotExport -->
<a id="test-20a750678a7f3b10"></a>
### `ImageWorkspaceTest.cancelledSelectionKeepsDraftAndUnsupportedFileCannotExport`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Vero JPEG minimale sintetico con APP1 'original metadata' (non TIFF EXIF valido), APP7, SOF48x32, finta scansione e3byte trailing. File JSON reale.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.selection.result`, `images.screen.export.isEnabled`, `images.state.notice.contains`, `images.state.inputs.size`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** File HEIC che contiene testo non-JPEG disabilita export; cancel mantiene riferimento.

**Limiti:** Non è JPEG con pixel decodificati; export dichiara esplicitamente pixelsDecoded=false/heicConversionApproved=false. Primo nome promette Audio+Video ma sentinella è solo prefs Audio. Polling5ms/5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void cancelledSelectionKeepsDraftAndUnsupportedFileCannotExport()throws Exception{
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();File source=File.createTempFile("unsupported-",".heic");Files.write(source.toPath(),"HEIC not JPEG".getBytes(StandardCharsets.UTF_8));
        try{ImageController images=controller.get().images;images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(source)));settle(images);assertNull(images.state.info);assertFalse(images.screen.export.isEnabled());assertTrue(images.state.notice.contains("Non è un JPEG"));images.selection.result(ImageSelection.FILES,Activity.RESULT_CANCELED,null);assertEquals(1,images.state.inputs.size());}
        finally{controller.pause().stop().destroy();source.delete();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageWorkspaceTest.threeTabsSelectImagesAndSwitchWithoutGrowingSecondaryBackStack -->
<a id="test-c47bddfc45b54f96"></a>
### `ImageWorkspaceTest.threeTabsSelectImagesAndSwitchWithoutGrowingSecondaryBackStack`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Vero JPEG minimale sintetico con APP1 'original metadata' (non TIFF EXIF valido), APP7, SOF48x32, finta scansione e3byte trailing. File JSON reale.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.compose.getView`, `images.compose.command`, `AudioActivity.class.getName`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Pagina Compose Foto visibile e comando Audio emette Intent/termina; non verifica tutti tre tab del nome.

**Limiti:** Non è JPEG con pixel decodificati; export dichiara esplicitamente pixelsDecoded=false/heicConversionApproved=false. Primo nome promette Audio+Video ma sentinella è solo prefs Audio. Polling5ms/5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void threeTabsSelectImagesAndSwitchWithoutGrowingSecondaryBackStack(){
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            ImageController images=controller.get().images;
            assertTrue(images.compose.getView().isShown());
            assertNull(Shadows.shadowOf(controller.get()).getNextStartedActivity());
            images.compose.command(PhotoCommand.AUDIO);
            assertEquals(AudioActivity.class.getName(),Shadows.shadowOf(controller.get()).getNextStartedActivity().getComponent().getClassName());
            assertTrue(controller.get().isFinishing());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageWorkspaceTest.pendingExportSurvivesRecreationAndSourceCannotBeOverwritten -->
<a id="test-4fbcbfb5bb7d3804"></a>
### `ImageWorkspaceTest.pendingExportSurvivesRecreationAndSourceCannotBeOverwritten`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Vero JPEG minimale sintetico con APP1 'original metadata' (non TIFF EXIF valido), APP7, SOF48x32, finta scansione e3byte trailing. File JSON reale.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.selection.result`, `images.screen.export.performClick`, `images.export`, `images.state.notice.contains`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Picker export pendente ricreato produce JSON; destinazione identica sorgente rifiutata senza cambiare byte.

**Limiti:** Non è JPEG con pixel decodificati; export dichiara esplicitamente pixelsDecoded=false/heicConversionApproved=false. Primo nome promette Audio+Video ma sentinella è solo prefs Audio. Polling5ms/5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void pendingExportSurvivesRecreationAndSourceCannotBeOverwritten()throws Exception{
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();File source=File.createTempFile("original-",".jpg"),report=File.createTempFile("inventory-",".json");byte[] original=fixture();Files.write(source.toPath(),original);
        try{
            ImageController images=controller.get().images;images.selection.result(ImageSelection.FILES,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(source)));settle(images);
            images.screen.export.performClick();controller.recreate();images=controller.get().images;settle(images);
            images.selection.result(ImageSelection.REPORT,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(report)));settle(images);assertEquals("JPEG",new JSONObject(new String(Files.readAllBytes(report.toPath()),StandardCharsets.UTF_8)).getString("format"));
            images.export(Uri.fromFile(source),Uri.fromFile(source));settle(images);assertTrue(images.state.notice.contains("coincide"));assertArrayEquals(original,Files.readAllBytes(source.toPath()));
        }finally{controller.pause().stop().destroy();source.delete();report.delete();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageWorkspaceTest.imagesAreReachableFromBothExistingWorkspaces -->
<a id="test-33b9455ea29f2067"></a>
### `ImageWorkspaceTest.imagesAreReachableFromBothExistingWorkspaces`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Vero JPEG minimale sintetico con APP1 'original metadata' (non TIFF EXIF valido), APP7, SOF48x32, finta scansione e3byte trailing. File JSON reale.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.screen.navigation.images.performClick`, `ImageActivity.class.getName`, `audio.get`, `audio.screen.navigation.images.performClick`, `audio.pause`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Foto raggiungibile da Video/Audio con policy back stack coerente.

**Limiti:** Non è JPEG con pixel decodificati; export dichiara esplicitamente pixelsDecoded=false/heicConversionApproved=false. Primo nome promette Audio+Video ma sentinella è solo prefs Audio. Polling5ms/5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void imagesAreReachableFromBothExistingWorkspaces(){
        var video=Robolectric.buildActivity(MainActivity.class).setup();var audio=Robolectric.buildActivity(AudioActivity.class).setup();try{
            video.get().home.screen.navigation.images.performClick();assertEquals(ImageActivity.class.getName(),Shadows.shadowOf(video.get()).getNextStartedActivity().getComponent().getClassName());assertFalse(video.get().isFinishing());
            audio.get().audio.screen.navigation.images.performClick();assertEquals(ImageActivity.class.getName(),Shadows.shadowOf(audio.get()).getNextStartedActivity().getComponent().getClassName());assertTrue(audio.get().isFinishing());
        }finally{audio.pause().stop().destroy();video.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-photodetailsloadertest"></a>
## PhotoDetailsLoaderTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/PhotoDetailsLoaderTest.kt) · 2 metodi · area originaria: Foto.

**Contesto applicativo della classe:** PhotoDetailsLoader.update/get/invalidate/close.

**Riferimenti produttivi:** [PhotoDetailsLoader](../../app/src/main/java/it/edo/codecprobe/PhotoDetailsLoader.kt).

**Preparazione, file e simulazioni condivisi:** read lambda stub produce PhotoBasicDetails(camera=uri); executor reale e callback vuota; nessun file, parser o vista Compose. Android lifecycle handler simulato.

**Confini comuni dell’audit:** Thread.sleep(30) arbitrario e polling5ms/3s; test 'atMost32' controlla solo eviction di0-0, non un conteggio<=32, quindi capacità33/40 potrebbe passare.

**Metodi:** [visibleMetadataIsReusedAndReloadInvalidatesOnlyRequestedSources](#test-56ef47233b547094) · [changingViewportCancelsObsoleteWorkAndKeepsAtMost32SmallEntries](#test-b3fa516003fdd51f)

<!-- test: PhotoDetailsLoaderTest.visibleMetadataIsReusedAndReloadInvalidatesOnlyRequestedSources -->
<a id="test-56ef47233b547094"></a>
### `PhotoDetailsLoaderTest.visibleMetadataIsReusedAndReloadInvalidatesOnlyRequestedSources`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): read lambda stub produce PhotoBasicDetails(camera=uri); executor reale e callback vuota; nessun file, parser o vista Compose. Android lifecycle handler simulato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoDetailsLoaderTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `loader.update`, `loader.get`, `loader.invalidate`, `loader.close`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Richieste a,b,c lette una volta, viewport c,b riusa cache e invalidate solo b la rilegge.

**Limiti:** Thread.sleep(30) arbitrario e polling5ms/3s; test 'atMost32' controlla solo eviction di0-0, non un conteggio<=32, quindi capacità33/40 potrebbe passare.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun visibleMetadataIsReusedAndReloadInvalidatesOnlyRequestedSources() {
        val reads = mutableListOf<String>()
        val loader = PhotoDetailsLoader({ uri -> synchronized(reads) { reads += uri }; PhotoBasicDetails(camera = uri) }, {})
        try {
            loader.update(listOf("a", "b", "c"))
            await { loader.get("c") != null }
            loader.update(listOf("c", "b"))
            Thread.sleep(30)
            assertEquals(listOf("a", "b", "c"), synchronized(reads) { reads.toList() })
            loader.invalidate(listOf("b"))
            await { synchronized(reads) { reads.count { it == "b" } } == 2 && loader.get("b") != null }
            assertEquals(1, synchronized(reads) { reads.count { it == "c" } })
            assertEquals("a", loader.get("a")!!.camera)
        } finally { loader.close() }
    }
```

</details>

<!-- /test -->

<!-- test: PhotoDetailsLoaderTest.changingViewportCancelsObsoleteWorkAndKeepsAtMost32SmallEntries -->
<a id="test-b3fa516003fdd51f"></a>
### `PhotoDetailsLoaderTest.changingViewportCancelsObsoleteWorkAndKeepsAtMost32SmallEntries`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): read lambda stub produce PhotoBasicDetails(camera=uri); executor reale e callback vuota; nessun file, parser o vista Compose. Android lifecycle handler simulato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoDetailsLoaderTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `loader.update`, `loader.get`, `loader.close`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Viewport nuovo interrompe slow ed evita obsolete;60 letture fanno sparire primo elemento e restare ultimo. Non misura esattamente capacità32.

**Limiti:** Thread.sleep(30) arbitrario e polling5ms/3s; test 'atMost32' controlla solo eviction di0-0, non un conteggio<=32, quindi capacità33/40 potrebbe passare.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun changingViewportCancelsObsoleteWorkAndKeepsAtMost32SmallEntries() {
        val entered = CountDownLatch(1)
        val count = AtomicInteger()
        val loader = PhotoDetailsLoader({ uri ->
            if (uri == "slow") {
                entered.countDown()
                try { CountDownLatch(1).await(3, TimeUnit.SECONDS) }
                catch (cancelled: InterruptedException) { throw InterruptedIOException("cancelled") }
            }
            count.incrementAndGet(); PhotoBasicDetails(camera = uri)
        }, {})
        try {
            loader.update(listOf("slow", "obsolete"))
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            loader.update(listOf("current"))
            await { loader.get("current") != null }
            assertNull(loader.get("slow")); assertNull(loader.get("obsolete")); assertEquals(1, count.get())
            for (batch in 0..2) {
                loader.update((0..19).map { "$batch-$it" })
                await { loader.get("$batch-19") != null }
            }
            assertNull(loader.get("0-0")); assertNotNull(loader.get("2-19"))
            loader.update(emptyList())
        } finally { loader.close() }
    }
```

</details>

<!-- /test -->
