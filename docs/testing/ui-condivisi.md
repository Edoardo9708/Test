# Interfaccia grafica e componenti condivisi

[Guida](README.md) · [Indice completo](index.md) · [Revisioni](review-needed.md) · [Esecuzioni](esecuzioni.md)

Descrizioni statiche trasferite dall’audit: non attestano nuove esecuzioni. Le fixture condivise sono descritte a livello di classe; dati specifici, ordine delle operazioni e assert sono nel corpo integrale di ogni metodo. Gli helper richiamati e gli eventuali `@Before`/`@After` sono consultabili nel sorgente collegato. Lo script di aggiornamento non riscrive questa pagina.

## Classi

- [AnalysisRenderTest](#class-analysisrendertest)
- [AudioRenderTest](#class-audiorendertest)
- [CollapsibleCardsTest](#class-collapsiblecardstest)
- [CompactLogExportTest](#class-compactlogexporttest)
- [CompactLogTest](#class-compactlogtest)
- [CompressionSummaryTest](#class-compressionsummarytest)
- [ConversionDashboardTest](#class-conversiondashboardtest)
- [CpuModeButtonsTest](#class-cpumodebuttonstest)
- [DashboardMetricsTest](#class-dashboardmetricstest)
- [FailureDetailsTest](#class-failuredetailstest)
- [HomeFailureReportTest](#class-homefailurereporttest)
- [HomeLayoutTest](#class-homelayouttest)
- [HomeRenderTest](#class-homerendertest)
- [ImageLiveLogTest](#class-imagelivelogtest)
- [ImageRenderTest](#class-imagerendertest)
- [ImageVirtualReportTest](#class-imagevirtualreporttest)
- [ImageWorkReportTest](#class-imageworkreporttest)
- [IncrementalReportTest](#class-incrementalreporttest)
- [MediaDestinationsRenderTest](#class-mediadestinationsrendertest)
- [MetadataMemoryCacheTest](#class-metadatamemorycachetest)
- [PhotoComposeRenderTest](#class-photocomposerendertest)
- [PhotoComposeTest](#class-photocomposetest)
- [SavingsBoundaryTest](#class-savingsboundarytest)
- [SavingsPolicyTest](#class-savingspolicytest)
- [WorkspaceNavigationTest](#class-workspacenavigationtest)

<a id="class-analysisrendertest"></a>
## AnalysisRenderTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AnalysisRenderTest.java) · 5 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** AnalysisScreen; AnalysisDetailReader.read; AnalysisParser.hex; View.draw.

**Riferimenti produttivi:** [AnalysisScreen](../../app/src/main/java/it/edo/codecprobe/AnalysisScreen.java), [AnalysisDetailReader](../../app/src/main/java/it/edo/codecprobe/AnalysisDetailReader.java), [AnalysisParser](../../app/src/main/java/it/edo/codecprobe/AnalysisParser.java).

**Preparazione, file e simulazioni condivisi:** Alberi JPEG/HEIC costruiti manualmente; popup usa un vero piccolo TIFF temporaneo; schermata e Bitmap Robolectric native graphics; PNG.

**Confini comuni dell’audit:** Solo Bitmap.compress riuscita. Nessun golden. I nodi HEIC/JPEG disegnati non provengono dal parsing dei file visualizzati; non prova foto decodificate.

**Metodi:** [popupLight](#test-5568ac91675eba4f) · [files](#test-21e0ac7cb312e6f0) · [nestedHeic](#test-4956d3ef49873892) · [blocks](#test-baaae0f6069c74ee) · [hexDark](#test-6a7870abdef17c49)

<!-- test: AnalysisRenderTest.popupLight -->
<a id="test-5568ac91675eba4f"></a>
### `AnalysisRenderTest.popupLight`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Alberi JPEG/HEIC costruiti manualmente; popup usa un vero piccolo TIFF temporaneo; schermata e Bitmap Robolectric native graphics; PNG.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `render(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Popup orientamento su TIFF sintetico con hex visibile, tema chiaro.

**Limiti:** Solo Bitmap.compress riuscita. Nessun golden. I nodi HEIC/JPEG disegnati non provengono dal parsing dei file visualizzati; non prova foto decodificate.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void popupLight()throws Exception{render(true,"analysis-popup-light.png");}
```

</details>

<!-- /test -->

<!-- test: AnalysisRenderTest.files -->
<a id="test-21e0ac7cb312e6f0"></a>
### `AnalysisRenderTest.files`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Alberi JPEG/HEIC costruiti manualmente; popup usa un vero piccolo TIFF temporaneo; schermata e Bitmap Robolectric native graphics; PNG.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `render(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Lista riferimenti foto/video preparati manualmente.

**Limiti:** Solo Bitmap.compress riuscita. Nessun golden. I nodi HEIC/JPEG disegnati non provengono dal parsing dei file visualizzati; non prova foto decodificate.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void files()throws Exception{render(false,"analysis-files.png");}
```

</details>

<!-- /test -->

<!-- test: AnalysisRenderTest.nestedHeic -->
<a id="test-4956d3ef49873892"></a>
### `AnalysisRenderTest.nestedHeic`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Alberi JPEG/HEIC costruiti manualmente; popup usa un vero piccolo TIFF temporaneo; schermata e Bitmap Robolectric native graphics; PNG.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `render(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Albero iprp/ipco/ipma HEIC creato a mano.

**Limiti:** Solo Bitmap.compress riuscita. Nessun golden. I nodi HEIC/JPEG disegnati non provengono dal parsing dei file visualizzati; non prova foto decodificate.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void nestedHeic()throws Exception{render(false,"analysis-nested-heic.png");}
```

</details>

<!-- /test -->

<!-- test: AnalysisRenderTest.blocks -->
<a id="test-baaae0f6069c74ee"></a>
### `AnalysisRenderTest.blocks`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Alberi JPEG/HEIC costruiti manualmente; popup usa un vero piccolo TIFF temporaneo; schermata e Bitmap Robolectric native graphics; PNG.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `render(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Blocchi JPEG creati a mano.

**Limiti:** Solo Bitmap.compress riuscita. Nessun golden. I nodi HEIC/JPEG disegnati non provengono dal parsing dei file visualizzati; non prova foto decodificate.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void blocks()throws Exception{render(false,"analysis-blocks.png");}
```

</details>

<!-- /test -->

<!-- test: AnalysisRenderTest.hexDark -->
<a id="test-6a7870abdef17c49"></a>
### `AnalysisRenderTest.hexDark`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Alberi JPEG/HEIC costruiti manualmente; popup usa un vero piccolo TIFF temporaneo; schermata e Bitmap Robolectric native graphics; PNG.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `render(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Stesso popup TIFF in tema scuro.

**Limiti:** Solo Bitmap.compress riuscita. Nessun golden. I nodi HEIC/JPEG disegnati non provengono dal parsing dei file visualizzati; non prova foto decodificate.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test @Config(qualifiers="w360dp-h800dp-night-xhdpi") public void hexDark()throws Exception{render(true,"analysis-hex-dark.png");}
```

</details>

<!-- /test -->


<a id="class-audiorendertest"></a>
## AudioRenderTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AudioRenderTest.java) · 4 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** AudioScreen/AudioController; AudioSettingsScreen; View.draw/Bitmap.compress.

**Riferimenti produttivi:** [AudioScreen](../../app/src/main/java/it/edo/codecprobe/AudioScreen.java), [AudioController](../../app/src/main/java/it/edo/codecprobe/AudioController.java), [AudioSettingsScreen](../../app/src/main/java/it/edo/codecprobe/AudioSettingsScreen.java).

**Preparazione, file e simulazioni condivisi:** Robolectric GraphicsMode.NATIVE (Skia del test, non codec dell'app), stato AudioRunState e descrizione FLAC artificiali; PNG build/reports/ui.

**Confini comuni dell’audit:** Nessun confronto golden/pixel, accessibilità o frame timing. I primi tre controllano solo compressione e dimensione PNG 10–250 kB; settings solo compressione riuscita.

**Metodi:** [files](#test-c7a87d6cbf63ecf8) · [qualityDark](#test-405c50923e4fa6ed) · [verifying](#test-04f0c44e41a0ad12) · [settingsPage](#test-d80e4c1807a1c379)

<!-- test: AudioRenderTest.files -->
<a id="test-c7a87d6cbf63ecf8"></a>
### `AudioRenderTest.files`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric GraphicsMode.NATIVE (Skia del test, non codec dell'app), stato AudioRunState e descrizione FLAC artificiali; PNG build/reports/ui.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `render(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Screenshot card file con due nomi sintetici lunghi.

**Limiti:** Nessun confronto golden/pixel, accessibilità o frame timing. I primi tre controllano solo compressione e dimensione PNG 10–250 kB; settings solo compressione riuscita.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void files()throws Exception{render(AudioScreen.FILES,"audio-files.png");}
```

</details>

<!-- /test -->

<!-- test: AudioRenderTest.qualityDark -->
<a id="test-405c50923e4fa6ed"></a>
### `AudioRenderTest.qualityDark`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric GraphicsMode.NATIVE (Skia del test, non codec dell'app), stato AudioRunState e descrizione FLAC artificiali; PNG build/reports/ui.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `render(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Screenshot card compressione in tema scuro.

**Limiti:** Nessun confronto golden/pixel, accessibilità o frame timing. I primi tre controllano solo compressione e dimensione PNG 10–250 kB; settings solo compressione riuscita.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test @Config(qualifiers="w360dp-h800dp-night-xhdpi") public void qualityDark()throws Exception{render(AudioScreen.COMPRESSION,"audio-quality-dark.png");}
```

</details>

<!-- /test -->

<!-- test: AudioRenderTest.verifying -->
<a id="test-04f0c44e41a0ad12"></a>
### `AudioRenderTest.verifying`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric GraphicsMode.NATIVE (Skia del test, non codec dell'app), stato AudioRunState e descrizione FLAC artificiali; PNG build/reports/ui.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `render(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Screenshot verifica Opus: è soltanto una dicitura/stato preparato dal test.

**Limiti:** Nessun confronto golden/pixel, accessibilità o frame timing. I primi tre controllano solo compressione e dimensione PNG 10–250 kB; settings solo compressione riuscita.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void verifying()throws Exception{render(AudioScreen.CONVERSION,"audio-conversion.png");}
```

</details>

<!-- /test -->

<!-- test: AudioRenderTest.settingsPage -->
<a id="test-d80e4c1807a1c379"></a>
### `AudioRenderTest.settingsPage`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric GraphicsMode.NATIVE (Skia del test, non codec dell'app), stato AudioRunState e descrizione FLAC artificiali; PNG build/reports/ui.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `screen.ui.dp`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Screenshot pagina impostazioni: non verifica corrispondenza visiva.

**Limiti:** Nessun confronto golden/pixel, accessibilità o frame timing. I primi tre controllano solo compressione e dimensione PNG 10–250 kB; settings solo compressione riuscita.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void settingsPage()throws Exception{
        var context=RuntimeEnvironment.getApplication();context.getSharedPreferences("audio-startup-settings",0).edit().clear().commit();
        var activity=Robolectric.buildActivity(SettingsActivity.class,new android.content.Intent(context,SettingsActivity.class).putExtra(AudioSettingsScreen.AUDIO,true)).setup();
        try{
            AudioSettingsScreen screen=activity.get().audioScreen;View page=screen.page;int width=screen.ui.dp(360);page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());page.getViewTreeObserver().dispatchOnPreDraw();
            float density=activity.get().getResources().getDisplayMetrics().density;Bitmap bitmap=Bitmap.createBitmap(360,(int)Math.ceil(page.getMeasuredHeight()/density),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(screen.ui.bg);canvas.scale(1/density,1/density);page.draw(canvas);
            File dir=new File("build/reports/ui");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,"audio-settings.png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
        }finally{activity.pause().stop().destroy();context.getSharedPreferences("audio-startup-settings",0).edit().clear().commit();}
    }
```

</details>

<!-- /test -->


<a id="class-collapsiblecardstest"></a>
## CollapsibleCardsTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/CollapsibleCardsTest.java) · 2 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** CollapsibleCards/HomeScreen; HomeState Bundle.

**Riferimenti produttivi:** [HomeScreen](../../app/src/main/java/it/edo/codecprobe/HomeScreen.java), [HomeState](../../app/src/main/java/it/edo/codecprobe/HomeState.java).

**Preparazione, file e simulazioni condivisi:** Activity Robolectric, accesso diretto mappe dei titoli e SeekBar.

**Confini comuni dell’audit:** Non misura animazioni/fluidità né nuovo percorso Compose; titoli/card-count vincolati.

**Metodi:** [onlyFirstCardOpensAndCollapseKeepsValues](#test-28cb61094bfe261e) · [focusConversionAndRecreationKeepExpansion](#test-6941647b20ae2ef0)

<!-- test: CollapsibleCardsTest.onlyFirstCardOpensAndCollapseKeepsValues -->
<a id="test-28cb61094bfe261e"></a>
### `CollapsibleCardsTest.onlyFirstCardOpensAndCollapseKeepsValues`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity Robolectric, accesso diretto mappe dei titoli e SeekBar.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CollapsibleCardsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `bodies(…)`, `headers(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Solo prima card aperta; collapse conserva stessa SeekBar e CRF28.

**Limiti:** Non misura animazioni/fluidità né nuovo percorso Compose; titoli/card-count vincolati.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void onlyFirstCardOpensAndCollapseKeepsValues()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try{
            Map<String,LinearLayout> cards=bodies(activity);assertEquals(4,cards.size());int n=0;
            for(LinearLayout body:cards.values())assertEquals(n++==0?View.VISIBLE:View.GONE,body.getVisibility());
            SeekBar crf=(SeekBar)activity.home.screen.compression.crfSlider;crf.setProgress(28);
            headers(activity).get("02  COMPRESSIONE").performClick();assertEquals(View.VISIBLE,cards.get("02  COMPRESSIONE").getVisibility());
            headers(activity).get("02  COMPRESSIONE").performClick();assertEquals(View.GONE,cards.get("02  COMPRESSIONE").getVisibility());
            assertSame(crf,activity.home.screen.compression.crfSlider);assertEquals(28,crf.getProgress());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: CollapsibleCardsTest.focusConversionAndRecreationKeepExpansion -->
<a id="test-6941647b20ae2ef0"></a>
### `CollapsibleCardsTest.focusConversionAndRecreationKeepExpansion`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity Robolectric, accesso diretto mappe dei titoli e SeekBar.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CollapsibleCardsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.screen.sections.focusCard`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Focus conversione e Bundle mantengono espansione; prima card può poi aprirsi senza chiudere conversione.

**Limiti:** Non misura animazioni/fluidità né nuovo percorso Compose; titoli/card-count vincolati.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void focusConversionAndRecreationKeepExpansion()throws Exception{
        ActivityController<MainActivity> first=Robolectric.buildActivity(MainActivity.class).setup();Bundle saved=new Bundle();
        try{
            first.get().home.screen.sections.focusCard(MainActivity.CARD_CONVERSION);
            for(Map.Entry<String,LinearLayout> entry:bodies(first.get()).entrySet())assertEquals(entry.getKey().equals(MainActivity.CARD_CONVERSION)?View.VISIBLE:View.GONE,entry.getValue().getVisibility());
            first.saveInstanceState(saved);
        }finally{first.pause().stop().destroy();}
        ActivityController<MainActivity> restored=Robolectric.buildActivity(MainActivity.class).create(saved).start().resume().visible();
        try{
            for(Map.Entry<String,LinearLayout> entry:bodies(restored.get()).entrySet())assertEquals(entry.getKey().equals(MainActivity.CARD_CONVERSION)?View.VISIBLE:View.GONE,entry.getValue().getVisibility());
            headers(restored.get()).get("01  FILE E DESTINAZIONE").performClick();
            assertEquals(View.VISIBLE,bodies(restored.get()).get(MainActivity.CARD_CONVERSION).getVisibility());
            assertEquals(View.VISIBLE,bodies(restored.get()).get("01  FILE E DESTINAZIONE").getVisibility());
        }finally{restored.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-compactlogexporttest"></a>
## CompactLogExportTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/CompactLogExportTest.java) · 2 metodi · area originaria: Componenti condivisi.

**Contesto applicativo della classe:** HomeDiagnostics export; CompactLog; ZipOutputStream; Intent create-document.

**Riferimenti produttivi:** [HomeDiagnostics](../../app/src/main/java/it/edo/codecprobe/HomeDiagnostics.java), [CompactLog](../../app/src/main/java/it/edo/codecprobe/CompactLog.java).

**Preparazione, file e simulazioni condivisi:** File log reale con 30000 righe sintetiche o messaggio errore; Activity/Picker simulati; veri ZIP temporanei.

**Confini comuni dell’audit:** Non completa export verso provider SAF; annulla il picker. Non prova log generato dal codec, encoding di ogni charset o storage failure.

**Metodi:** [reopenedAppExportsOldVerboseLogCompactlyWithoutReplacingIt](#test-4060bc4fc0dd3dde) · [zipChoiceContainsCompleteCompactTextAndUsesZipMime](#test-28e4deb04f3ae21d)

<!-- test: CompactLogExportTest.reopenedAppExportsOldVerboseLogCompactlyWithoutReplacingIt -->
<a id="test-4060bc4fc0dd3dde"></a>
### `CompactLogExportTest.reopenedAppExportsOldVerboseLogCompactlyWithoutReplacingIt`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): File log reale con 30000 righe sintetiche o messaggio errore; Activity/Picker simulati; veri ZIP temporanei.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompactLogExportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.class.getDeclaredField`, `ConversionService.State`, `ConversionService.batchLogFile`, `home.screen.diagnostics.saveLog.performClick`, `home.diagnostics.logFormatDialog.getListView`, `home.diagnostics.worker.submit`, `home.diagnostics.onLogResult`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Export testo da log precedente riduce dimensione sotto un decimo, conserva errore/stack/nome, originale stessa lunghezza e snapshot rimosso su cancel.

**Limiti:** Non completa export verso provider SAF; annulla il picker. Non prova log generato dal codec, encoding di ogni charset o storage failure.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void reopenedAppExportsOldVerboseLogCompactlyWithoutReplacingIt()throws Exception{
        Field field=ConversionService.class.getDeclaredField("state");field.setAccessible(true);Object before=field.get(null);field.set(null,new ConversionService.State());
        File source=ConversionService.batchLogFile(RuntimeEnvironment.getApplication());byte[] previous=source.isFile()?Files.readAllBytes(source.toPath()):null;
        try(Writer writer=new OutputStreamWriter(new FileOutputStream(source),StandardCharsets.UTF_8)){
            writer.write("*** INIZIO VIDEO 8 / 8 ***\nNome: Discorso Mirko_20180622.mp4\n");
            for(int i=0;i<30000;i++)writer.write("["+i+" ms] VERIFY frame="+i+" pts_us="+(i*33333L)+"\n");
            writer.write("*** ERRORE ***\njava.io.IOException: moov missing\n\tat Mp4Metadata.roots(Mp4Metadata.java:86)\n");
        }
        long sourceSize=source.length();var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;home.screen.diagnostics.saveLog.performClick();
            home.diagnostics.logFormatDialog.getListView().performItemClick(null,1,1);
            home.diagnostics.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(home.state.pendingLog);File snapshot=new File(home.state.pendingLog);
            String text=new String(Files.readAllBytes(snapshot.toPath()),StandardCharsets.UTF_8);
            assertTrue(snapshot.length()<sourceSize/10);assertEquals(sourceSize,source.length());assertTrue(text.contains("moov missing"));assertTrue(text.contains("Discorso Mirko_20180622.mp4"));assertTrue(text.contains("Mp4Metadata.java:86"));
            Intent picker=Shadows.shadowOf(controller.get()).getNextStartedActivityForResult().intent;assertEquals(Intent.ACTION_CREATE_DOCUMENT,picker.getAction());assertEquals("text/plain",picker.getType());
            home.diagnostics.onLogResult(HomeRequests.SAVE_LOG,Activity.RESULT_CANCELED,null);assertFalse(snapshot.exists());
        }finally{
            controller.pause().stop().destroy();field.set(null,before);if(previous==null)source.delete();else Files.write(source.toPath(),previous);
        }
    }
```

</details>

<!-- /test -->

<!-- test: CompactLogExportTest.zipChoiceContainsCompleteCompactTextAndUsesZipMime -->
<a id="test-28e4deb04f3ae21d"></a>
### `CompactLogExportTest.zipChoiceContainsCompleteCompactTextAndUsesZipMime`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): File log reale con 30000 righe sintetiche o messaggio errore; Activity/Picker simulati; veri ZIP temporanei.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompactLogExportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.class.getDeclaredField`, `ConversionService.State`, `ConversionService.batchLogFile`, `home.screen.diagnostics.saveLog.performClick`, `home.diagnostics.logFormatDialog.getListView`, `home.diagnostics.worker.submit`, `home.diagnostics.onLogResult`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** ZIP reale contiene unica entry CodecProbe-log.txt con errore integrale; MIME/titolo corretti, sorgente identica e snapshot rimosso.

**Limiti:** Non completa export verso provider SAF; annulla il picker. Non prova log generato dal codec, encoding di ogni charset o storage failure.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void zipChoiceContainsCompleteCompactTextAndUsesZipMime()throws Exception{
        Field field=ConversionService.class.getDeclaredField("state");field.setAccessible(true);Object before=field.get(null);field.set(null,new ConversionService.State());
        File source=ConversionService.batchLogFile(RuntimeEnvironment.getApplication());byte[] previous=source.isFile()?Files.readAllBytes(source.toPath()):null;
        String message="*** ERRORE ***\njava.io.IOException: ZIP preserves the full error\n\tat Example.run(Example.java:10)\n";
        Files.write(source.toPath(),message.getBytes(StandardCharsets.UTF_8));
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            HomeController home=controller.get().home;home.screen.diagnostics.saveLog.performClick();home.diagnostics.logFormatDialog.getListView().performItemClick(null,0,0);
            home.diagnostics.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            File snapshot=new File(home.state.pendingLog);assertTrue(snapshot.getName().endsWith(".zip"));
            try(java.util.zip.ZipInputStream zip=new java.util.zip.ZipInputStream(new FileInputStream(snapshot))){
                assertEquals("CodecProbe-log.txt",zip.getNextEntry().getName());ByteArrayOutputStream decoded=new ByteArrayOutputStream();
                byte[] buffer=new byte[1024];int n;while((n=zip.read(buffer))!=-1)decoded.write(buffer,0,n);
                String exported=decoded.toString("UTF-8");
                assertTrue(exported,exported.replace("\r\n","\n").contains(message));assertNull(zip.getNextEntry());
            }
            Intent picker=Shadows.shadowOf(controller.get()).getNextStartedActivityForResult().intent;
            assertEquals("application/zip",picker.getType());assertTrue(picker.getStringExtra(Intent.EXTRA_TITLE).endsWith(".zip"));
            home.diagnostics.onLogResult(HomeRequests.SAVE_LOG,Activity.RESULT_CANCELED,null);assertFalse(snapshot.exists());
            assertEquals(message,new String(Files.readAllBytes(source.toPath()),StandardCharsets.UTF_8));
        }finally{controller.pause().stop().destroy();field.set(null,before);if(previous==null)source.delete();else Files.write(source.toPath(),previous);}
    }
```

</details>

<!-- /test -->


<a id="class-compactlogtest"></a>
## CompactLogTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/CompactLogTest.java) · 6 metodi · area originaria: Componenti condivisi.

**Contesto applicativo della classe:** CompactLog.accept/finish/export.

**Riferimenti produttivi:** [CompactLog](../../app/src/main/java/it/edo/codecprobe/CompactLog.java).

**Preparazione, file e simulazioni condivisi:** Stringhe di successo/errore artificiali, clock numerico e stream in memoria; fino a 300000 eventi e multilinea da 20k caratteri.

**Confini comuni dell’audit:** Non chiama x265, non produce veri errori HDR o misura I/O del logger del servizio. Stress numerico con allocazioni, non profilo RAM.

**Metodi:** [repeatedSuccessesAreSummarizedButWarningsErrorsAndUnknownMessagesSurvive](#test-513d664161988964) · [negativeCodecResultsAreNeverSampledAway](#test-40271e7cfcc6b712) · [legacyExportKeepsEveryFileAndErrorAndHandlesTimestampRestart](#test-1b117e3c611cc80c) · [hdrMultilineAndAbruptEndAreFlushedAndLargeSamplesAreBounded](#test-a1a655b6d4d6e5cd) · [exportUsesTheRealStreamInsteadOfAssumingAvailableIsItsLength](#test-9eade1f7f2b21ae1) · [repeatedP010FormatsAndHdrVerificationAreCompactedToo](#test-bad77bfeb926b89a)

<!-- test: CompactLogTest.repeatedSuccessesAreSummarizedButWarningsErrorsAndUnknownMessagesSurvive -->
<a id="test-513d664161988964"></a>
### `CompactLogTest.repeatedSuccessesAreSummarizedButWarningsErrorsAndUnknownMessagesSurvive`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Stringhe di successo/errore artificiali, clock numerico e stream in memoria; fino a 300000 eventi e multilinea da 20k caratteri.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompactLogTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `log.accept`, `log.finish`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Campiona ripetizioni ma conserva ultimi frame, contesto, messaggi sconosciuti, stack e fine sessione.

**Limiti:** Non chiama x265, non produce veri errori HDR o misura I/O del logger del servizio. Stress numerico con allocazioni, non profilo RAM.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void repeatedSuccessesAreSummarizedButWarningsErrorsAndUnknownMessagesSurvive(){
        List<String> messages=new ArrayList<>();CompactLog log=new CompactLog(messages::add);
        log.accept("*** STEP 2/3 · CODIFICA ***",0);
        for(int i=0;i<100000;i++){
            log.accept("BEGIN x265_encoder_encode frame="+i+" pts_us="+(i*33333L),i);
            log.accept("END x265_encoder_encode rc=1 nals=1 elapsed_ms=3",i);
            log.accept("VERIFY frame="+i+" pts_us="+(i*33333L),i);
        }
        log.accept("Nuova diagnostica non ancora conosciuta",100001);
        log.accept("*** ERRORE · CONVERSIONE INTERROTTA ***",100002);
        log.accept("java.io.IOException: Example\n\tat example.Source.run(Source.java:12)",100003);
        log.accept("SESSION END cancelled=false",100004);log.finish();
        String result=String.join("\n",messages);
        assertTrue(messages.size()<100);assertTrue(result.length()<30000);
        assertTrue(result.contains("frame=99999"));assertTrue(result.contains("ULTIMI EVENTI DETTAGLIATI"));assertTrue(result.contains("RIEPILOGO EVENTI RIPETITIVI"));
        assertTrue(result.contains("Nuova diagnostica non ancora conosciuta"));assertTrue(result.contains("Source.java:12"));assertTrue(result.contains("SESSION END"));
    }
```

</details>

<!-- /test -->

<!-- test: CompactLogTest.negativeCodecResultsAreNeverSampledAway -->
<a id="test-40271e7cfcc6b712"></a>
### `CompactLogTest.negativeCodecResultsAreNeverSampledAway`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Stringhe di successo/errore artificiali, clock numerico e stream in memoria; fino a 300000 eventi e multilinea da 20k caratteri.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompactLogTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `log.accept`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** rc=-1 resta, benché rc=0 ripetuto sia campionato.

**Limiti:** Non chiama x265, non produce veri errori HDR o misura I/O del logger del servizio. Stress numerico con allocazioni, non profilo RAM.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void negativeCodecResultsAreNeverSampledAway(){
        List<String> messages=new ArrayList<>();CompactLog log=new CompactLog(messages::add);
        for(int i=0;i<100;i++)log.accept("END x265_encoder_encode rc=0 nals=0",0);
        log.accept("END x265_encoder_encode rc=-1 nals=0",0);
        assertTrue(String.join("\n",messages).contains("rc=-1"));
    }
```

</details>

<!-- /test -->

<!-- test: CompactLogTest.legacyExportKeepsEveryFileAndErrorAndHandlesTimestampRestart -->
<a id="test-1b117e3c611cc80c"></a>
### `CompactLogTest.legacyExportKeepsEveryFileAndErrorAndHandlesTimestampRestart`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Stringhe di successo/errore artificiali, clock numerico e stream in memoria; fino a 300000 eventi e multilinea da 20k caratteri.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompactLogTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompactLog.export`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Export di 8 log sintetici conserva intestazioni/errori e gestisce ripartenza timestamp.

**Limiti:** Non chiama x265, non produce veri errori HDR o misura I/O del logger del servizio. Stress numerico con allocazioni, non profilo RAM.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void legacyExportKeepsEveryFileAndErrorAndHandlesTimestampRestart()throws Exception{
        ByteArrayOutputStream old=new ByteArrayOutputStream();
        for(int file=1;file<=8;file++){
            old.write(("*** INIZIO VIDEO "+file+" / 8 ***\nNome: video"+file+".mp4\n").getBytes(StandardCharsets.UTF_8));
            for(int frame=0;frame<10000;frame++)old.write(("["+(frame*10)+" ms] BEGIN conversione YUV frame="+frame+" pts_us="+frame+"\n").getBytes(StandardCharsets.UTF_8));
            old.write("[100001 ms] BEGIN muxer.stop\n[100002 ms] OK muxer.stop\n".getBytes(StandardCharsets.UTF_8));
            if(file==8)old.write("[100003 ms] *** ERRORE ***\njava.io.IOException: moov assente\n\tat Mp4Metadata.roots(Mp4Metadata.java:86)\n".getBytes(StandardCharsets.UTF_8));
        }
        byte[] original=old.toByteArray();ByteArrayOutputStream output=new ByteArrayOutputStream();CompactLog.export(new ByteArrayInputStream(original),output);
        String text=output.toString(StandardCharsets.UTF_8.name());
        assertTrue(output.size()<original.length/20);
        for(int file=1;file<=8;file++){assertTrue(text.contains("Nome: video"+file+".mp4"));assertTrue(text.contains("INIZIO VIDEO "+file+" / 8"));}
        assertTrue(text.contains("moov assente"));assertTrue(text.contains("Mp4Metadata.java:86"));assertTrue(text.contains("BEGIN muxer.stop"));assertTrue(text.contains("OK muxer.stop"));
        assertEquals(8,text.split("frame=0 pts_us=0",-1).length-1);
    }
```

</details>

<!-- /test -->

<!-- test: CompactLogTest.hdrMultilineAndAbruptEndAreFlushedAndLargeSamplesAreBounded -->
<a id="test-a1a655b6d4d6e5cd"></a>
### `CompactLogTest.hdrMultilineAndAbruptEndAreFlushedAndLargeSamplesAreBounded`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Stringhe di successo/errore artificiali, clock numerico e stream in memoria; fino a 300000 eventi e multilinea da 20k caratteri.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompactLogTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `log.accept`, `log.finish`, `CompactLog.export`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** HDR multilinea campionato/troncato; ultimo evento senza newline viene flushato.

**Limiti:** Non chiama x265, non produce veri errori HDR o misura I/O del logger del servizio. Stress numerico con allocazioni, non profilo RAM.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void hdrMultilineAndAbruptEndAreFlushedAndLargeSamplesAreBounded()throws Exception{
        List<String> messages=new ArrayList<>();CompactLog log=new CompactLog(messages::add);
        StringBuilder large=new StringBuilder();for(int i=0;i<20000;i++)large.append('x');
        for(int i=0;i<1000;i++)log.accept("FRAME HDR · numero="+i+"\nMetadati originali: "+large,0);
        log.finish();String text=String.join("\n",messages);
        assertTrue(text.contains("numero=999"));assertTrue(text.contains("campione abbreviato"));assertTrue(text.length()<50000);
        ByteArrayOutputStream out=new ByteArrayOutputStream();CompactLog.export(new ByteArrayInputStream("[1 ms] VERIFY frame=1\n[2 ms] VERIFY frame=2\n[3 ms] VERIFY frame=3".getBytes(StandardCharsets.UTF_8)),out);
        assertTrue(out.toString("UTF-8").contains("frame=3"));
    }
```

</details>

<!-- /test -->

<!-- test: CompactLogTest.exportUsesTheRealStreamInsteadOfAssumingAvailableIsItsLength -->
<a id="test-9eade1f7f2b21ae1"></a>
### `CompactLogTest.exportUsesTheRealStreamInsteadOfAssumingAvailableIsItsLength`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Stringhe di successo/errore artificiali, clock numerico e stream in memoria; fino a 300000 eventi e multilinea da 20k caratteri.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompactLogTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompactLog.export`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** available()==0 non è scambiato per EOF, il contenuto dello stream resta esportato.

**Limiti:** Non chiama x265, non produce veri errori HDR o misura I/O del logger del servizio. Stress numerico con allocazioni, non profilo RAM.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void exportUsesTheRealStreamInsteadOfAssumingAvailableIsItsLength()throws Exception{
        byte[] data="*** PARAMETRI ***\nCRF=23\njava.io.IOException: error".getBytes(StandardCharsets.UTF_8);
        InputStream input=new ByteArrayInputStream(data){public int available(){return 0;}};
        ByteArrayOutputStream output=new ByteArrayOutputStream();CompactLog.export(input,output);
        assertTrue(output.toString("UTF-8").contains("CRF=23"));assertTrue(output.toString("UTF-8").contains("IOException: error"));
    }
```

</details>

<!-- /test -->

<!-- test: CompactLogTest.repeatedP010FormatsAndHdrVerificationAreCompactedToo -->
<a id="test-bad77bfeb926b89a"></a>
### `CompactLogTest.repeatedP010FormatsAndHdrVerificationAreCompactedToo`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Stringhe di successo/errore artificiali, clock numerico e stream in memoria; fino a 300000 eventi e multilinea da 20k caratteri.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompactLogTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompactLog.export`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Righe P010/verify HDR ripetute compattate e ultimo frame/errore mantenuti.

**Limiti:** Non chiama x265, non produce veri errori HDR o misura I/O del logger del servizio. Stress numerico con allocazioni, non profilo RAM.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void repeatedP010FormatsAndHdrVerificationAreCompactedToo()throws Exception{
        ByteArrayOutputStream verbose=new ByteArrayOutputStream();
        for(int i=0;i<10000;i++){
            verbose.write(("["+i+" ms] FORMATO P010 · {width=1920, height=1080, color-format=54, timeUs="+i+"}\n").getBytes(StandardCharsets.UTF_8));
            verbose.write(("["+i+" ms] OK VERIFY HDR SEI · frame="+i+" · pts_us="+i+" · payload originali identici (SHA-256)\n").getBytes(StandardCharsets.UTF_8));
        }
        verbose.write("*** ERRORE ***\njava.io.IOException: retained HDR error\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output=new ByteArrayOutputStream();CompactLog.export(new ByteArrayInputStream(verbose.toByteArray()),output);
        String text=output.toString("UTF-8");assertTrue(output.size()<verbose.size()/20);
        assertTrue(text.contains("FORMATO P010"));assertTrue(text.contains("frame=9999"));assertTrue(text.contains("retained HDR error"));
    }
```

</details>

<!-- /test -->


<a id="class-compressionsummarytest"></a>
## CompressionSummaryTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java) · 9 metodi · area originaria: Componenti condivisi.

**Contesto applicativo della classe:** CompressionSummary.render/change.

**Riferimenti produttivi:** [CompressionSummary](../../app/src/main/java/it/edo/codecprobe/CompressionSummary.java).

**Preparazione, file e simulazioni condivisi:** Item COMPLETE/FAILED/CANCELLED/SKIPPED/IN_PROGRESS già costruiti.

**Confini comuni dell’audit:** Verifica solo report e contabilità; nessun encoder, file salvato o reale stato di servizio.

**Metodi:** [halfSizeIsMinusFifty](#test-68277daef1a89644) · [growthIsPositiveAndUsesAdditionalSpace](#test-f71b34512ddc2217) · [totalsUseBytesNotAverageOfPercentages](#test-8050f86a779be999) · [unknownSizeIsExcludedAndReported](#test-149635478e91ea62) · [zeroInputDoesNotDivideByZero](#test-41d9b8f518fc6713) · [cancelledAndSkippedHaveNoInventedSavings](#test-70dc02454a1956e1) · [runningReportIncludesFinishedVideoAndDistinguishesPendingFiles](#test-5eb6c53d9ee7f42d) · [finalReportDoesNotLeaveFilesRunningOrWaiting](#test-f116f7ff4794ef0f) · [initialReportWaitsForActualCompletedOutput](#test-ec9cb52a6fa2a5c9)

<!-- test: CompressionSummaryTest.halfSizeIsMinusFifty -->
<a id="test-68277daef1a89644"></a>
### `CompressionSummaryTest.halfSizeIsMinusFifty`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Item COMPLETE/FAILED/CANCELLED/SKIPPED/IN_PROGRESS già costruiti.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompressionSummary.render`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** 10→5 MB: -50% e 5 MB risparmiati.

**Limiti:** Verifica solo report e contabilità; nessun encoder, file salvato o reale stato di servizio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void halfSizeIsMinusFifty(){
        String report=CompressionSummary.render(Arrays.asList(ok(1,10000000,5000000)));
        assertTrue(report.contains("10,00 MB → 5,00 MB · -50,0%"));
        assertTrue(report.contains("Spazio risparmiato: 5,00 MB"));
    }
```

</details>

<!-- /test -->

<!-- test: CompressionSummaryTest.growthIsPositiveAndUsesAdditionalSpace -->
<a id="test-f71b34512ddc2217"></a>
### `CompressionSummaryTest.growthIsPositiveAndUsesAdditionalSpace`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Item COMPLETE/FAILED/CANCELLED/SKIPPED/IN_PROGRESS già costruiti.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompressionSummary.render`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** 10→12 MB: +20% e spazio aggiuntivo, senza falso risparmio.

**Limiti:** Verifica solo report e contabilità; nessun encoder, file salvato o reale stato di servizio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void growthIsPositiveAndUsesAdditionalSpace(){
        String report=CompressionSummary.render(Arrays.asList(ok(1,10000000,12000000)));
        assertTrue(report.contains("+20,0%"));assertTrue(report.contains("Spazio aggiuntivo: 2,00 MB"));
        assertFalse(report.contains("Spazio risparmiato:"));
    }
```

</details>

<!-- /test -->

<!-- test: CompressionSummaryTest.totalsUseBytesNotAverageOfPercentages -->
<a id="test-8050f86a779be999"></a>
### `CompressionSummaryTest.totalsUseBytesNotAverageOfPercentages`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Item COMPLETE/FAILED/CANCELLED/SKIPPED/IN_PROGRESS già costruiti.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompressionSummary.render`, `CompressionSummary.Item`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Totali pesati sui byte (110→95 MB), non media percentuali; fallito escluso.

**Limiti:** Verifica solo report e contabilità; nessun encoder, file salvato o reale stato di servizio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void totalsUseBytesNotAverageOfPercentages(){
        String report=CompressionSummary.render(Arrays.asList(ok(1,10000000,5000000),ok(2,100000000,90000000),
            new CompressionSummary.Item(3,"fallito",CompressionSummary.Status.FAILED,900000000,-1)));
        assertTrue(report.contains("110,00 MB → 95,00 MB · -13,6%"));
        assertTrue(report.contains("Conversione fallita"));assertTrue(report.contains("Spazio risparmiato: 15,00 MB"));
    }
```

</details>

<!-- /test -->

<!-- test: CompressionSummaryTest.unknownSizeIsExcludedAndReported -->
<a id="test-149635478e91ea62"></a>
### `CompressionSummaryTest.unknownSizeIsExcludedAndReported`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Item COMPLETE/FAILED/CANCELLED/SKIPPED/IN_PROGRESS già costruiti.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompressionSummary.render`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Dimensioni mancanti escluse e denominatore 1 su 2 esplicito.

**Limiti:** Verifica solo report e contabilità; nessun encoder, file salvato o reale stato di servizio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void unknownSizeIsExcludedAndReported(){
        String report=CompressionSummary.render(Arrays.asList(ok(1,-1,2000000),ok(2,10000000,5000000)));
        assertTrue(report.contains("dimensione non disponibile"));assertTrue(report.contains("variazione non calcolabile"));
        assertTrue(report.contains("Totale calcolato su 1 di 2"));
    }
```

</details>

<!-- /test -->

<!-- test: CompressionSummaryTest.zeroInputDoesNotDivideByZero -->
<a id="test-41d9b8f518fc6713"></a>
### `CompressionSummaryTest.zeroInputDoesNotDivideByZero`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Item COMPLETE/FAILED/CANCELLED/SKIPPED/IN_PROGRESS già costruiti.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompressionSummary.change`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Ingresso zero non divide per zero; uguaglianza resa 0%.

**Limiti:** Verifica solo report e contabilità; nessun encoder, file salvato o reale stato di servizio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void zeroInputDoesNotDivideByZero(){
        assertEquals("variazione non calcolabile",CompressionSummary.change(0,100));
        assertEquals("0,0%",CompressionSummary.change(100,100));
    }
```

</details>

<!-- /test -->

<!-- test: CompressionSummaryTest.cancelledAndSkippedHaveNoInventedSavings -->
<a id="test-70dc02454a1956e1"></a>
### `CompressionSummaryTest.cancelledAndSkippedHaveNoInventedSavings`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Item COMPLETE/FAILED/CANCELLED/SKIPPED/IN_PROGRESS già costruiti.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompressionSummary.render`, `CompressionSummary.Item`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Cancellato/saltato non inventano risparmi né -100%.

**Limiti:** Verifica solo report e contabilità; nessun encoder, file salvato o reale stato di servizio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void cancelledAndSkippedHaveNoInventedSavings(){
        String report=CompressionSummary.render(Arrays.asList(
            new CompressionSummary.Item(1,"a",CompressionSummary.Status.CANCELLED,-1,-1),
            new CompressionSummary.Item(2,"b",CompressionSummary.Status.SKIPPED,-1,-1)));
        assertTrue(report.contains("Conversione annullata"));assertTrue(report.contains("Non convertito"));
        assertTrue(report.contains("Confronto dimensioni non disponibile"));assertFalse(report.contains("-100"));
    }
```

</details>

<!-- /test -->

<!-- test: CompressionSummaryTest.runningReportIncludesFinishedVideoAndDistinguishesPendingFiles -->
<a id="test-5eb6c53d9ee7f42d"></a>
### `CompressionSummaryTest.runningReportIncludesFinishedVideoAndDistinguishesPendingFiles`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Item COMPLETE/FAILED/CANCELLED/SKIPPED/IN_PROGRESS già costruiti.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompressionSummary.render`, `CompressionSummary.Item`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Report parziale include conclusi e distingue file in corso/in attesa.

**Limiti:** Verifica solo report e contabilità; nessun encoder, file salvato o reale stato di servizio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void runningReportIncludesFinishedVideoAndDistinguishesPendingFiles(){
        String report=CompressionSummary.render(Arrays.asList(ok(1,10000000,5000000),
            new CompressionSummary.Item(2,"secondo",CompressionSummary.Status.IN_PROGRESS,-1,-1),
            new CompressionSummary.Item(3,"terzo",CompressionSummary.Status.SKIPPED,-1,-1)),true);
        assertTrue(report.contains("10,00 MB → 5,00 MB · -50,0%"));
        assertTrue(report.contains("Video 2 · secondo\nIn corso"));
        assertTrue(report.contains("Video 3 · terzo\nIn attesa"));
        assertTrue(report.contains("TOTALE PARZIALE"));assertTrue(report.contains("Spazio risparmiato: 5,00 MB"));
        assertFalse(report.contains("Non convertito"));
    }
```

</details>

<!-- /test -->

<!-- test: CompressionSummaryTest.finalReportDoesNotLeaveFilesRunningOrWaiting -->
<a id="test-f116f7ff4794ef0f"></a>
### `CompressionSummaryTest.finalReportDoesNotLeaveFilesRunningOrWaiting`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Item COMPLETE/FAILED/CANCELLED/SKIPPED/IN_PROGRESS già costruiti.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompressionSummary.render`, `CompressionSummary.Item`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Report finale rimuove etichette in corso/in attesa e totale parziale.

**Limiti:** Verifica solo report e contabilità; nessun encoder, file salvato o reale stato di servizio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void finalReportDoesNotLeaveFilesRunningOrWaiting(){
        String report=CompressionSummary.render(Arrays.asList(ok(1,10000000,5000000),
            new CompressionSummary.Item(2,"secondo",CompressionSummary.Status.IN_PROGRESS,-1,-1),
            new CompressionSummary.Item(3,"terzo",CompressionSummary.Status.SKIPPED,-1,-1)));
        assertFalse(report.contains("In corso"));assertFalse(report.contains("In attesa"));assertFalse(report.contains("TOTALE PARZIALE"));
        assertTrue(report.contains("Non completato"));assertTrue(report.contains("Non convertito"));
    }
```

</details>

<!-- /test -->

<!-- test: CompressionSummaryTest.initialReportWaitsForActualCompletedOutput -->
<a id="test-ec9cb52a6fa2a5c9"></a>
### `CompressionSummaryTest.initialReportWaitsForActualCompletedOutput`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Item COMPLETE/FAILED/CANCELLED/SKIPPED/IN_PROGRESS già costruiti.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CompressionSummaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompressionSummary.render`, `CompressionSummary.Item`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Prima del primo completato non si dichiara spazio risparmiato.

**Limiti:** Verifica solo report e contabilità; nessun encoder, file salvato o reale stato di servizio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void initialReportWaitsForActualCompletedOutput(){
        String report=CompressionSummary.render(Arrays.asList(new CompressionSummary.Item(1,"primo",CompressionSummary.Status.IN_PROGRESS,-1,-1)),true);
        assertTrue(report.contains("In attesa del primo video completato"));assertFalse(report.contains("Spazio risparmiato:"));
    }
```

</details>

<!-- /test -->


<a id="class-conversiondashboardtest"></a>
## ConversionDashboardTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ConversionDashboardTest.java) · 1 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** HomeConversion.renderConversionDashboard; ConversionPanel.

**Riferimenti produttivi:** [HomeConversion](../../app/src/main/java/it/edo/codecprobe/HomeConversion.java).

**Preparazione, file e simulazioni condivisi:** State preparato a mano con frame/progress/opzioni arbitrarie, Activity Robolectric.

**Confini comuni dell’audit:** Nessuna misurazione RAM/batteria o avanzamento reale; presenza view non garantisce dato corretto. Gerarchia/weight esplicitamente vincolati.

**Metodi:** [showsFileProgressAndFrozenConfigurationInSeparateRows](#test-6af736e1ceee7335)

<!-- test: ConversionDashboardTest.showsFileProgressAndFrozenConfigurationInSeparateRows -->
<a id="test-6af736e1ceee7335"></a>
### `ConversionDashboardTest.showsFileProgressAndFrozenConfigurationInSeparateRows`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): State preparato a mano con frame/progress/opzioni arbitrarie, Activity Robolectric.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ConversionDashboardTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.State`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Render file43%, frame100/1000, CRF/preset/Bframe/lookahead congelati e layout comandi su stessa riga.

**Limiti:** Nessuna misurazione RAM/batteria o avanzamento reale; presenza view non garantisce dato corretto. Gerarchia/weight esplicitamente vincolati.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void showsFileProgressAndFrozenConfigurationInSeparateRows()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try{
            ((SeekBar)activity.home.screen.compression.crfSlider).setProgress(18);
            ConversionService.State state=new ConversionService.State();state.batchIndex=2;state.batchTotal=3;state.succeeded=1;state.running=true;state.source="Secondo.mp4";state.percent=14;state.filePercent=43;
            state.doneFrames=100;state.totalFrames=1000;state.done=2000000;state.total=10000000;state.requestedCrf=28;state.requestedPreset="slow";state.requestedCpu=2;state.bframeCount=4;state.lookaheadCount=20;state.cuTree=1;state.aqMode=2;state.aqStrength=1000;
            activity.home.conversion.renderConversionDashboard(state);
            assertEquals("43%",((TextView)activity.home.screen.conversion.percentLabel).getText().toString());assertEquals(43,((ProgressBar)activity.home.screen.conversion.progress).getProgress());
            assertEquals("100 / 1000",((TextView)activity.home.screen.conversion.frameLabel).getText().toString());
            assertEquals("28",((TextView)activity.home.screen.conversion.activeCrf).getText().toString());assertEquals("slow",((TextView)activity.home.screen.conversion.activePreset).getText().toString());
            assertEquals("4",((TextView)activity.home.screen.conversion.activeBframes).getText().toString());assertEquals("20",((TextView)activity.home.screen.conversion.activeLookahead).getText().toString());
            assertTrue(((TextView)activity.home.screen.conversion.queueLabel).getText().toString().contains("Video 2 / 3"));
            Button convert=(Button)activity.home.screen.conversion.convert,stop=(Button)activity.home.screen.conversion.stop;assertSame(convert.getParent(),stop.getParent());
            LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)convert.getLayoutParams();assertEquals(0,lp.width);assertEquals(1,lp.weight,0);
            assertNotNull(activity.home.screen.conversion.ramValue);assertNotNull(activity.home.screen.conversion.batteryValue);
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-cpumodebuttonstest"></a>
## CpuModeButtonsTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/CpuModeButtonsTest.java) · 3 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** CpuModeButtons; SettingsActivity; AppSettings.

**Riferimenti produttivi:** [CpuModeButtons](../../app/src/main/java/it/edo/codecprobe/CpuModeButtons.java), [SettingsActivity](../../app/src/main/java/it/edo/codecprobe/SettingsActivity.java), [AppSettings](../../app/src/main/java/it/edo/codecprobe/AppSettings.java).

**Preparazione, file e simulazioni condivisi:** View Robolectric; campo cpu privato letto reflection; SharedPreferences simulate.

**Confini comuni dell’audit:** Scelta indice/testo non verifica limiti CPU, thread C/C++ o prestazioni reali. childAt e label esatte fragili.

**Metodi:** [directChoiceSelectsExactlyOneButtonAndKeepsIndexes](#test-aa022b1787e78852) · [disablingDuringConversionBlocksAllThreeChoices](#test-08215467c4431685) · [settingsReadExistingCpuValueAndSaveNewDirectChoice](#test-4619cca2d7466dbc)

<!-- test: CpuModeButtonsTest.directChoiceSelectsExactlyOneButtonAndKeepsIndexes -->
<a id="test-aa022b1787e78852"></a>
### `CpuModeButtonsTest.directChoiceSelectsExactlyOneButtonAndKeepsIndexes`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): View Robolectric; campo cpu privato letto reflection; SharedPreferences simulate.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CpuModeButtonsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `selector.setSelection`, `selector.getChildAt`, `selector.getSelectedItemPosition`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Unica selezione e mapping tre indici, etichetta Limited.

**Limiti:** Scelta indice/testo non verifica limiti CPU, thread C/C++ o prestazioni reali. childAt e label esatte fragili.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void directChoiceSelectsExactlyOneButtonAndKeepsIndexes(){
        CpuModeButtons selector=selector();selector.setSelection(1);
        ((Button)selector.getChildAt(2)).performClick();assertEquals(2,selector.getSelectedItemPosition());
        for(int i=0;i<3;i++)assertEquals(i==2,selector.getChildAt(i).isSelected());
        selector.setSelection(0);assertEquals(0,selector.getSelectedItemPosition());
        assertEquals("Limited",((Button)selector.getChildAt(0)).getText().toString());
    }
```

</details>

<!-- /test -->

<!-- test: CpuModeButtonsTest.disablingDuringConversionBlocksAllThreeChoices -->
<a id="test-08215467c4431685"></a>
### `CpuModeButtonsTest.disablingDuringConversionBlocksAllThreeChoices`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): View Robolectric; campo cpu privato letto reflection; SharedPreferences simulate.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CpuModeButtonsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `selector.setSelection`, `selector.setEnabled`, `selector.getChildAt`, `selector.getSelectedItemPosition`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Disable impedisce tutti click; riabilita scelta.

**Limiti:** Scelta indice/testo non verifica limiti CPU, thread C/C++ o prestazioni reali. childAt e label esatte fragili.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void disablingDuringConversionBlocksAllThreeChoices(){
        CpuModeButtons selector=selector();selector.setSelection(1);selector.setEnabled(false);
        for(int i=0;i<3;i++){assertFalse(selector.getChildAt(i).isEnabled());selector.getChildAt(i).performClick();}
        assertEquals(1,selector.getSelectedItemPosition());
        selector.setEnabled(true);selector.getChildAt(2).performClick();assertEquals(2,selector.getSelectedItemPosition());
    }
```

</details>

<!-- /test -->

<!-- test: CpuModeButtonsTest.settingsReadExistingCpuValueAndSaveNewDirectChoice -->
<a id="test-4619cca2d7466dbc"></a>
### `CpuModeButtonsTest.settingsReadExistingCpuValueAndSaveNewDirectChoice`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): View Robolectric; campo cpu privato letto reflection; SharedPreferences simulate.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CpuModeButtonsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AppSettings.save`, `SettingsActivity.class.getDeclaredField`, `selector.getSelectedItemPosition`, `selector.getChildAt`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Impostazioni leggono indice 2 e salvano scelta 0 nel modello prefs.

**Limiti:** Scelta indice/testo non verifica limiti CPU, thread C/C++ o prestazioni reali. childAt e label esatte fragili.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void settingsReadExistingCpuValueAndSaveNewDirectChoice()throws Exception{
        AppSettings.save(RuntimeEnvironment.getApplication(),23,6,2,true,true,"",true);
        ActivityController<SettingsActivity> controller=Robolectric.buildActivity(SettingsActivity.class).setup();SettingsActivity activity=controller.get();
        try{
            Field f=SettingsActivity.class.getDeclaredField("cpu");f.setAccessible(true);CpuModeButtons selector=(CpuModeButtons)f.get(activity);
            assertEquals(2,selector.getSelectedItemPosition());selector.getChildAt(0).performClick();
            Button save=findButton(activity.getWindow().getDecorView(),"Salva impostazioni");assertNotNull(save);save.performClick();
            assertEquals(0,new AppSettings(activity).cpu);
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-dashboardmetricstest"></a>
## DashboardMetricsTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/DashboardMetricsTest.java) · 3 metodi · area originaria: Componenti condivisi.

**Contesto applicativo della classe:** DashboardMetrics.elapsed/cpuBand/batteryBand; ConversionService.State costruttore copia.

**Riferimenti produttivi:** [DashboardMetrics](../../app/src/main/java/it/edo/codecprobe/DashboardMetrics.java), [ConversionService](../../app/src/main/java/it/edo/codecprobe/ConversionService.java).

**Preparazione, file e simulazioni condivisi:** Timestamp e percentuali sintetici; stato costruito senza avviare il servizio.

**Confini comuni dell’audit:** Non misura CPU, temperatura o durata reale in background. L'ultimo test verifica campi copiati, non il servizio.

**Metodi:** [clockContinuesInBackgroundAndFreezesAtEnd](#test-24b8e768bda80b6f) · [colorsHaveExplicitBoundariesAndUnknownValues](#test-2227f40cb9c90f5e) · [serviceSnapshotKeepsClockAndStartedConfiguration](#test-5985e1634796dd98)

<!-- test: DashboardMetricsTest.clockContinuesInBackgroundAndFreezesAtEnd -->
<a id="test-24b8e768bda80b6f"></a>
### `DashboardMetricsTest.clockContinuesInBackgroundAndFreezesAtEnd`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Timestamp e percentuali sintetici; stato costruito senza avviare il servizio.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/DashboardMetricsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `DashboardMetrics.elapsed`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Formato clock con inizio/fine espliciti e caso mai avviato.

**Limiti:** Non misura CPU, temperatura o durata reale in background. L'ultimo test verifica campi copiati, non il servizio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void clockContinuesInBackgroundAndFreezesAtEnd(){
        assertEquals("01:01:01",DashboardMetrics.elapsed(1000,0,3662000));
        assertEquals("00:01:05",DashboardMetrics.elapsed(1000,66000,3662000));
        assertEquals("00:00:00",DashboardMetrics.elapsed(0,0,10000));
    }
```

</details>

<!-- /test -->

<!-- test: DashboardMetricsTest.colorsHaveExplicitBoundariesAndUnknownValues -->
<a id="test-2227f40cb9c90f5e"></a>
### `DashboardMetricsTest.colorsHaveExplicitBoundariesAndUnknownValues`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Timestamp e percentuali sintetici; stato costruito senza avviare il servizio.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/DashboardMetricsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `DashboardMetrics.cpuBand`, `DashboardMetrics.batteryBand`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Soglie esatte di bande CPU/batteria e valori ignoti/NaN.

**Limiti:** Non misura CPU, temperatura o durata reale in background. L'ultimo test verifica campi copiati, non il servizio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void colorsHaveExplicitBoundariesAndUnknownValues(){
        assertEquals(0,DashboardMetrics.cpuBand(29.9));assertEquals(1,DashboardMetrics.cpuBand(30));
        assertEquals(2,DashboardMetrics.cpuBand(70));assertEquals(3,DashboardMetrics.cpuBand(90));assertEquals(-1,DashboardMetrics.cpuBand(-1));
        assertEquals(0,DashboardMetrics.batteryBand(34.9));assertEquals(1,DashboardMetrics.batteryBand(35));
        assertEquals(2,DashboardMetrics.batteryBand(40));assertEquals(3,DashboardMetrics.batteryBand(45));assertEquals(-1,DashboardMetrics.batteryBand(Double.NaN));
    }
```

</details>

<!-- /test -->

<!-- test: DashboardMetricsTest.serviceSnapshotKeepsClockAndStartedConfiguration -->
<a id="test-5985e1634796dd98"></a>
### `DashboardMetricsTest.serviceSnapshotKeepsClockAndStartedConfiguration`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Timestamp e percentuali sintetici; stato costruito senza avviare il servizio.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/DashboardMetricsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.State`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Snapshot mantiene clock e parametri iniziali dopo mutazione dell'oggetto sorgente.

**Limiti:** Non misura CPU, temperatura o durata reale in background. L'ultimo test verifica campi copiati, non il servizio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void serviceSnapshotKeepsClockAndStartedConfiguration(){
        ConversionService.State source=new ConversionService.State();source.startedAt=1000;source.endedAt=66000;source.requestedCrf=28;source.requestedCpu=2;source.requestedPreset="slow";
        ConversionService.State copy=new ConversionService.State(source);source.requestedCrf=18;source.startedAt=9000;
        assertEquals(28,copy.requestedCrf);assertEquals(2,copy.requestedCpu);assertEquals("slow",copy.requestedPreset);assertEquals(1000,copy.startedAt);assertEquals(66000,copy.endedAt);
    }
```

</details>

<!-- /test -->


<a id="class-failuredetailstest"></a>
## FailureDetailsTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/FailureDetailsTest.java) · 4 metodi · area originaria: Componenti condivisi.

**Contesto applicativo della classe:** ConversionFailure costruttore/title/copyText; BuildConfig.VERSION_NAME.

**Riferimenti produttivi:** [ConversionFailure](../../app/src/main/java/it/edo/codecprobe/ConversionFailure.java).

**Preparazione, file e simulazioni condivisi:** IOException/InterruptedIOException artificiali con causa/suppressed; BuildConfig reale generato.

**Confini comuni dell’audit:** Non esegue alcun fallimento di conversione, rollback o cancellazione: certifica la presentazione dei dettagli forniti.

**Metodi:** [absentOrBlankMessagesFallBackToTheRealExceptionType](#test-ad013baea97b9e3f) · [rootCauseAndRollbackFailureBothRemainCopyable](#test-45aa2227e5c530ab) · [multilineFilenameCannotInjectAnExtraDiagnosticHeading](#test-6230a6d204d313c9) · [cancellationDetailsAreNotRewrittenAsAnUnrelatedIoError](#test-03e12f26706aa900)

<!-- test: FailureDetailsTest.absentOrBlankMessagesFallBackToTheRealExceptionType -->
<a id="test-ad013baea97b9e3f"></a>
### `FailureDetailsTest.absentOrBlankMessagesFallBackToTheRealExceptionType`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): IOException/InterruptedIOException artificiali con causa/suppressed; BuildConfig reale generato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/FailureDetailsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Messaggi null/vuoti/bianchi ripiegano sul nome IOException.

**Limiti:** Non esegue alcun fallimento di conversione, rollback o cancellazione: certifica la presentazione dei dettagli forniti.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void absentOrBlankMessagesFallBackToTheRealExceptionType() {
        for(String message:new String[]{null,"","  "}) {
            ConversionFailure failure=new ConversionFailure(1,"clip","lettura",new IOException(message));
            assertEquals("IOException",failure.message);assertTrue(failure.details.contains("IOException"));
        }
    }
```

</details>

<!-- /test -->

<!-- test: FailureDetailsTest.rootCauseAndRollbackFailureBothRemainCopyable -->
<a id="test-45aa2227e5c530ab"></a>
### `FailureDetailsTest.rootCauseAndRollbackFailureBothRemainCopyable`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): IOException/InterruptedIOException artificiali con causa/suppressed; BuildConfig reale generato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/FailureDetailsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Causa originale e suppressed rollback restano nel testo copiabile, con ultimo stato e versione.

**Limiti:** Non esegue alcun fallimento di conversione, rollback o cancellazione: certifica la presentazione dei dettagli forniti.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void rootCauseAndRollbackFailureBothRemainCopyable() {
        IOException cause=new IOException("source absent");IOException error=new IOException("publication failed",cause);
        error.addSuppressed(new IOException("rollback denied"));
        String copied=new ConversionFailure(2,"clip","salvataggio",error).copyText();
        assertTrue(copied.contains("publication failed"));assertTrue(copied.contains("source absent"));
        assertTrue(copied.contains("rollback denied"));assertTrue(copied.contains("salvataggio"));
        assertTrue(copied.contains(BuildConfig.VERSION_NAME));
    }
```

</details>

<!-- /test -->

<!-- test: FailureDetailsTest.multilineFilenameCannotInjectAnExtraDiagnosticHeading -->
<a id="test-6230a6d204d313c9"></a>
### `FailureDetailsTest.multilineFilenameCannotInjectAnExtraDiagnosticHeading`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): IOException/InterruptedIOException artificiali con causa/suppressed; BuildConfig reale generato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/FailureDetailsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** CR/LF del nome non iniettano una nuova intestazione.

**Limiti:** Non esegue alcun fallimento di conversione, rollback o cancellazione: certifica la presentazione dei dettagli forniti.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void multilineFilenameCannotInjectAnExtraDiagnosticHeading() {
        ConversionFailure error=new ConversionFailure(3,"clip\nFORGED\rTITLE","lettura",new IOException("unreadable"));
        assertEquals("Video 3 · clip FORGED TITLE",error.title());
    }
```

</details>

<!-- /test -->

<!-- test: FailureDetailsTest.cancellationDetailsAreNotRewrittenAsAnUnrelatedIoError -->
<a id="test-03e12f26706aa900"></a>
### `FailureDetailsTest.cancellationDetailsAreNotRewrittenAsAnUnrelatedIoError`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): IOException/InterruptedIOException artificiali con causa/suppressed; BuildConfig reale generato.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/FailureDetailsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Testo e tipo InterruptedIOException rimangono nei dettagli: non prova conteggi CANCELLED del servizio.

**Limiti:** Non esegue alcun fallimento di conversione, rollback o cancellazione: certifica la presentazione dei dettagli forniti.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void cancellationDetailsAreNotRewrittenAsAnUnrelatedIoError() {
        ConversionFailure failure=new ConversionFailure(1,"clip","lettura",new InterruptedIOException("cancelled"));
        assertEquals("cancelled",failure.message);assertTrue(failure.details.contains("InterruptedIOException"));
    }
```

</details>

<!-- /test -->


<a id="class-homefailurereporttest"></a>
## HomeFailureReportTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/HomeFailureReportTest.java) · 4 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** HomeConversion.renderSizeReport; FailureReport; ConversionFailure; CompressionSummary.

**Riferimenti produttivi:** [HomeConversion](../../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [ConversionFailure](../../app/src/main/java/it/edo/codecprobe/ConversionFailure.java), [CompressionSummary](../../app/src/main/java/it/edo/codecprobe/CompressionSummary.java).

**Preparazione, file e simulazioni condivisi:** Due eccezioni costruite, State e sizeReport manuali; clipboard/dialog shadow, PNG in build/reports/ui.

**Confini comuni dell’audit:** Nessuna eccezione proveniente da conversione. Render screenshot non ha golden; childAt e label italiane vincolano struttura/testo.

**Metodi:** [failedFileOpensItsOwnErrorAndCopiesCollapsedDetailsWithoutClosing](#test-06946e744791ae6a) · [absentExceptionMessageStillHasUsefulText](#test-5b64bbe863dd4ae8) · [renderLightError](#test-f56b17f8824ba19e) · [renderDarkError](#test-9cf8d4e4ab780960)

<!-- test: HomeFailureReportTest.failedFileOpensItsOwnErrorAndCopiesCollapsedDetailsWithoutClosing -->
<a id="test-06946e744791ae6a"></a>
### `HomeFailureReportTest.failedFileOpensItsOwnErrorAndCopiesCollapsedDetailsWithoutClosing`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Due eccezioni costruite, State e sizeReport manuali; clipboard/dialog shadow, PNG in build/reports/ui.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeFailureReportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.State`, `state.failures.add`, `CompressionSummary.render`, `CompressionSummary.Item`, `home.conversion.renderSizeReport`, `home.screen.conversion.sizeReport.getVisibility`, `home.activity.getSystemService`, `state.failures.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Seconda riga apre/copia solo seconda causa senza chiudere popup; refresh non ricrea riga e snapshot copia failures.

**Limiti:** Nessuna eccezione proveniente da conversione. Render screenshot non ha golden; childAt e label italiane vincolano struttura/testo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void failedFileOpensItsOwnErrorAndCopiesCollapsedDetailsWithoutClosing() {
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try {
            HomeController home=controller.get().home;
            ConversionFailure first=new ConversionFailure(1,"first.mp4","Analisi",new IOException("Prima causa"));
            ConversionFailure second=new ConversionFailure(2,"second.mp4","Verifica video",new IOException("Seconda causa",new IllegalStateException("Causa interna")));
            ConversionService.State state=new ConversionService.State();state.failures.add(first);state.failures.add(second);
            state.sizeReport=CompressionSummary.render(List.of(
                new CompressionSummary.Item(1,"first.mp4",CompressionSummary.Status.FAILED,-1,-1),
                new CompressionSummary.Item(2,"second.mp4",CompressionSummary.Status.FAILED,-1,-1),
                new CompressionSummary.Item(3,"saved.mp4",CompressionSummary.Status.COMPLETE,100,50)));
            home.conversion.renderSizeReport(state);
            LinearLayout report=home.screen.conversion.failureReport;
            assertEquals(View.VISIBLE,report.getVisibility());
            assertEquals(View.GONE,home.screen.conversion.sizeReport.getVisibility());
            LinearLayout row=(LinearLayout)report.getChildAt(1);
            Button error=(Button)row.getChildAt(1);assertTrue(error.getContentDescription().toString().contains("second.mp4"));error.performClick();
            AlertDialog dialog=home.conversion.failures.dialog;assertTrue(dialog.isShowing());
            Button toggle=button(dialog.getWindow().getDecorView(),"Mostra dettagli tecnici ▾");assertNotNull(toggle);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            ClipboardManager clipboard=home.activity.getSystemService(ClipboardManager.class);
            String copied=clipboard.getPrimaryClip().getItemAt(0).getText().toString();
            assertEquals(second.copyText(),copied);assertTrue(copied.contains("Causa interna"));assertFalse(copied.contains("Prima causa"));assertTrue(dialog.isShowing());
            toggle.performClick();assertEquals("Nascondi dettagli tecnici ▴",toggle.getText().toString());
            // A dashboard refresh must not rebuild unchanged rows or close the popup.
            home.conversion.renderSizeReport(state);assertSame(row,report.getChildAt(1));assertTrue(dialog.isShowing());
            ConversionService.State snapshot=new ConversionService.State(state);state.failures.clear();assertEquals(2,snapshot.failures.size());
            home.conversion.renderSizeReport(state);assertEquals(View.GONE,report.getVisibility());
        } finally {controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: HomeFailureReportTest.absentExceptionMessageStillHasUsefulText -->
<a id="test-5b64bbe863dd4ae8"></a>
### `HomeFailureReportTest.absentExceptionMessageStillHasUsefulText`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Due eccezioni costruite, State e sizeReport manuali; clipboard/dialog shadow, PNG in build/reports/ui.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeFailureReportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** IOException senza message ha fallback utile e dettagli copiabili.

**Limiti:** Nessuna eccezione proveniente da conversione. Render screenshot non ha golden; childAt e label italiane vincolano struttura/testo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void absentExceptionMessageStillHasUsefulText(){
        ConversionFailure failure=new ConversionFailure(1,"clip.mp4","Analisi",new IOException());
        assertEquals("IOException",failure.message);assertTrue(failure.copyText().contains("DETTAGLI TECNICI"));
    }
```

</details>

<!-- /test -->

<!-- test: HomeFailureReportTest.renderLightError -->
<a id="test-f56b17f8824ba19e"></a>
### `HomeFailureReportTest.renderLightError`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Due eccezioni costruite, State e sizeReport manuali; clipboard/dialog shadow, PNG in build/reports/ui.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeFailureReportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `renderError(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** PNG popup errore chiaro: solo compressione riuscita.

**Limiti:** Nessuna eccezione proveniente da conversione. Render screenshot non ha golden; childAt e label italiane vincolano struttura/testo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void renderLightError()throws Exception{renderError("home-error.png");}
```

</details>

<!-- /test -->

<!-- test: HomeFailureReportTest.renderDarkError -->
<a id="test-9cf8d4e4ab780960"></a>
### `HomeFailureReportTest.renderDarkError`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Due eccezioni costruite, State e sizeReport manuali; clipboard/dialog shadow, PNG in build/reports/ui.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeFailureReportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `renderError(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** PNG popup errore scuro: solo compressione riuscita.

**Limiti:** Nessuna eccezione proveniente da conversione. Render screenshot non ha golden; childAt e label italiane vincolano struttura/testo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test @Config(qualifiers="w360dp-h800dp-night-xhdpi") public void renderDarkError()throws Exception{renderError("home-error-dark.png");}
```

</details>

<!-- /test -->


<a id="class-homelayouttest"></a>
## HomeLayoutTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/HomeLayoutTest.java) · 3 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** HomeScreen/HomeDirectoryRow; HomeCompression; AppUi.

**Riferimenti produttivi:** [HomeScreen](../../app/src/main/java/it/edo/codecprobe/HomeScreen.java), [HomeDirectoryRow](../../app/src/main/java/it/edo/codecprobe/HomeDirectoryRow.java), [HomeCompression](../../app/src/main/java/it/edo/codecprobe/HomeCompression.java), [AppUi](../../app/src/main/java/it/edo/codecprobe/AppUi.java).

**Preparazione, file e simulazioni condivisi:** Robolectric layout320dp/fontScale1.3 e1000dp night; misura View, non screenshot.

**Confini comuni dell’audit:** Copre geometria/touch target specifici, non accessibility service, device OEM o scroll fluido. Label e gerarchia rigide.

**Metodi:** [narrowLayoutWithLargeFontKeepsButtonLabelsAndTouchTargets](#test-6bdefeeb55ba8793) · [wideDarkLayoutLimitsContentWidthAndUsesDarkSystemIconsPolicy](#test-1ef984c5ae467b77) · [qualityShortcutsAndManualSliderKeepOneConsistentSelection](#test-3e0c216611e2c406)

<!-- test: HomeLayoutTest.narrowLayoutWithLargeFontKeepsButtonLabelsAndTouchTargets -->
<a id="test-6bdefeeb55ba8793"></a>
### `HomeLayoutTest.narrowLayoutWithLargeFontKeepsButtonLabelsAndTouchTargets`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric layout320dp/fontScale1.3 e1000dp night; misura View, non screenshot.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeLayoutTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.screen.sections.bodies.keySet`, `home.screen.sections.setCardExpanded`, `home.screen.conversion.convert.getTextSize`, `HomeDirectoryRow.add`, `home.screen.sections.bodies.get`, `home.ui.dp`, `home.screen.conversion.convert.getParent`, `home.screen.conversion.stop.getParent`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Tutte card espanse e cartella lunga: bottoni almeno48dp, testo non tagliato a320dp/font1.3.

**Limiti:** Copre geometria/touch target specifici, non accessibility service, device OEM o scroll fluido. Label e gerarchia rigide.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void narrowLayoutWithLargeFontKeepsButtonLabelsAndTouchTargets(){
        android.app.Application app=org.robolectric.RuntimeEnvironment.getApplication();
        Configuration config=new Configuration(app.getResources().getConfiguration());config.fontScale=1.3f;
        app.getResources().updateConfiguration(config,app.getResources().getDisplayMetrics());
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            MainActivity activity=controller.get();
            HomeController home=activity.home;for(String title:home.screen.sections.bodies.keySet())home.screen.sections.setCardExpanded(title,true);
            assertTrue("Large font configuration not applied",home.screen.conversion.convert.getTextSize()/activity.getResources().getDisplayMetrics().density>=19);
            HomeDirectoryRow.add(home.ui,home.screen.sections.bodies.get("01  FILE E DESTINAZIONE"),"DCIM/Una cartella con un nome molto lungo","1, 2",false,false,()->{});
            measure(home,320);checkButtons(home.screen.page,home.ui.dp(48));
            assertSame(home.screen.conversion.convert.getParent(),home.screen.conversion.stop.getParent());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: HomeLayoutTest.wideDarkLayoutLimitsContentWidthAndUsesDarkSystemIconsPolicy -->
<a id="test-1ef984c5ae467b77"></a>
### `HomeLayoutTest.wideDarkLayoutLimitsContentWidthAndUsesDarkSystemIconsPolicy`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric layout320dp/fontScale1.3 e1000dp night; misura View, non screenshot.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeLayoutTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.ui.dp`, `home.screen.page.getMeasuredWidth`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Contenuto limitato760dp su largo, colori tema scuro e flag systembar coerenti.

**Limiti:** Copre geometria/touch target specifici, non accessibility service, device OEM o scroll fluido. Label e gerarchia rigide.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test @Config(qualifiers="w1000dp-h600dp-night-xhdpi") public void wideDarkLayoutLimitsContentWidthAndUsesDarkSystemIconsPolicy(){
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{HomeController home=controller.get().home;measure(home,1000);assertTrue(home.ui.dark);assertEquals(home.ui.dp(760),home.screen.page.getMeasuredWidth());assertTrue(android.graphics.Color.red(home.ui.bg)<40);
            android.util.TypedValue background=new android.util.TypedValue();assertTrue(controller.get().getTheme().resolveAttribute(android.R.attr.colorBackground,background,true));
            assertTrue("Dialog theme must be dark too",android.graphics.Color.red(background.data)<100);
            int appearance=controller.get().getWindow().getInsetsController().getSystemBarsAppearance();assertEquals(0,appearance&WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: HomeLayoutTest.qualityShortcutsAndManualSliderKeepOneConsistentSelection -->
<a id="test-3e0c216611e2c406"></a>
### `HomeLayoutTest.qualityShortcutsAndManualSliderKeepOneConsistentSelection`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric layout320dp/fontScale1.3 e1000dp night; misura View, non screenshot.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeLayoutTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.screen.compression.crfButtons.get`, `home.compression.selectedCrf`, `home.screen.compression.crfSlider.setProgress`, `home.conversion.setBusy`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Shortcut CRF18, slider19 deseleziona shortcut, busy blocca/riabilita bottoni.

**Limiti:** Copre geometria/touch target specifici, non accessibility service, device OEM o scroll fluido. Label e gerarchia rigide.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void qualityShortcutsAndManualSliderKeepOneConsistentSelection(){
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{HomeController home=controller.get().home;home.screen.compression.crfButtons.get(0).performClick();assertEquals(18,home.compression.selectedCrf());assertTrue(home.screen.compression.crfButtons.get(0).isSelected());
            home.screen.compression.crfSlider.setProgress(19);for(Button b:home.screen.compression.crfButtons)assertFalse(b.isSelected());
            home.conversion.setBusy(true);for(Button b:home.screen.compression.crfButtons)assertFalse(b.isEnabled());
            home.conversion.setBusy(false);for(Button b:home.screen.compression.crfButtons)assertTrue(b.isEnabled());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-homerendertest"></a>
## HomeRenderTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/HomeRenderTest.java) · 5 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** HomeScreen; HomeConversion dashboard/report; HomeReset; View.draw.

**Riferimenti produttivi:** [HomeScreen](../../app/src/main/java/it/edo/codecprobe/HomeScreen.java), [HomeConversion](../../app/src/main/java/it/edo/codecprobe/HomeConversion.java), [HomeReset](../../app/src/main/java/it/edo/codecprobe/HomeReset.java).

**Preparazione, file e simulazioni condivisi:** Cache metadati, directory autorizzazioni, State e CPU/RAM/batteria testi inseriti manualmente; Robolectric native graphics.

**Confini comuni dell’audit:** Ogni capture controlla PNG>10000byte, nessun golden. Percentuali CPU/RAM/temperatura non sono misurate. activeConversion crea2PNG ma vale1metodo.

**Metodi:** [inputPage](#test-8bc7fe657519ffed) · [compressionDark](#test-6a7678a2275a9334) · [activeConversion](#test-eaa96977235623aa) · [batchResults](#test-692df0ca135adc5c) · [clearedHome](#test-343abe8308092fb0)

<!-- test: HomeRenderTest.inputPage -->
<a id="test-8bc7fe657519ffed"></a>
### `HomeRenderTest.inputPage`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Cache metadati, directory autorizzazioni, State e CPU/RAM/batteria testi inseriti manualmente; Robolectric native graphics.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.screen.sections.focusCard`, `home.destinations.originPanel.close`, `home.screen.destinations.originRows.removeAllViews`, `HomeDirectoryRow.add`, `home.ui.text`, `VideoMetadataCache.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Pagina input con directory/cartelle create a mano.

**Limiti:** Ogni capture controlla PNG>10000byte, nessun golden. Percentuali CPU/RAM/temperatura non sono misurate. activeConversion crea2PNG ma vale1metodo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void inputPage()throws Exception{
        ActivityController<MainActivity> controller=screen();try{HomeController home=controller.get().home;home.screen.sections.focusCard("01  FILE E DESTINAZIONE");
            home.destinations.originPanel.close();home.screen.destinations.originRows.removeAllViews();
            HomeDirectoryRow.add(home.ui,home.screen.destinations.originRows,"DCIM/Camera","1, 2",true,false,()->{});
            HomeDirectoryRow.add(home.ui,home.screen.destinations.originRows,"Movies/Screen recordings","3",false,false,()->{});
            home.ui.text(home.screen.destinations.originRows,"Cartelle autorizzate: 1 / 2",14);image(home,"home-input.png");}finally{controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }
```

</details>

<!-- /test -->

<!-- test: HomeRenderTest.compressionDark -->
<a id="test-6a7678a2275a9334"></a>
### `HomeRenderTest.compressionDark`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Cache metadati, directory autorizzazioni, State e CPU/RAM/batteria testi inseriti manualmente; Robolectric native graphics.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.screen.sections.focusCard`, `home.screen.compression.presetSlider.setProgress`, `home.screen.compression.crfSlider.setProgress`, `home.screen.compression.temporalHelp.setVisibility`, `home.screen.compression.bframeSwitch.setText`, `home.screen.compression.lookaheadSwitch.setText`, `home.screen.compression.cuTreeLabel.setText`, `home.screen.compression.aqModeLabel.setText`, `home.screen.compression.aqStrengthLabel.setText`, `VideoMetadataCache.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Compressione scura con Bframe/lookahead/CUtree/AQ label simulate, JNI non caricato.

**Limiti:** Ogni capture controlla PNG>10000byte, nessun golden. Percentuali CPU/RAM/temperatura non sono misurate. activeConversion crea2PNG ma vale1metodo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test @Config(qualifiers="w360dp-h800dp-night-xhdpi") public void compressionDark()throws Exception{
        ActivityController<MainActivity> controller=screen();try{
            HomeController home=controller.get().home;home.screen.sections.focusCard("02  COMPRESSIONE");home.screen.compression.presetSlider.setProgress(5);home.screen.compression.crfSlider.setProgress(23);
            // The JVM has no Android JNI library. Only these preview values are simulated for the image.
            home.screen.compression.temporalHelp.setVisibility(View.GONE);home.screen.compression.bframeSwitch.setText("B-frame · 4");home.screen.compression.lookaheadSwitch.setText("Lookahead · 20");home.screen.compression.cuTreeLabel.setText("CU-tree · ON");home.screen.compression.aqModeLabel.setText("AQ mode · 2");home.screen.compression.aqStrengthLabel.setText("AQ strength · 1,00");
            image(home,"home-compression-dark.png");
        }finally{controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }
```

</details>

<!-- /test -->

<!-- test: HomeRenderTest.activeConversion -->
<a id="test-eaa96977235623aa"></a>
### `HomeRenderTest.activeConversion`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Cache metadati, directory autorizzazioni, State e CPU/RAM/batteria testi inseriti manualmente; Robolectric native graphics.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.screen.sections.focusCard`, `ConversionService.State`, `home.conversion.renderConversionDashboard`, `home.screen.conversion.cpuValue.setText`, `home.screen.conversion.ramValue.setText`, `home.screen.conversion.batteryValue.setText`, `home.conversion.renderSizeReport`, `VideoMetadataCache.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** PNG run attiva+pausa con dati artificiosi, non conversione o prestazioni.

**Limiti:** Ogni capture controlla PNG>10000byte, nessun golden. Percentuali CPU/RAM/temperatura non sono misurate. activeConversion crea2PNG ma vale1metodo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void activeConversion()throws Exception{
        ActivityController<MainActivity> controller=screen();try{
            HomeController home=controller.get().home;home.screen.sections.focusCard(MainActivity.CARD_CONVERSION);
            ConversionService.State state=new ConversionService.State();state.running=true;state.fileActive=true;state.onlySmaller=true;state.batchIndex=2;state.batchTotal=3;state.succeeded=1;state.source="Video_di_prova_2.mp4";state.filePercent=19;state.doneFrames=340;state.totalFrames=1800;state.done=11333333;state.total=60000000;state.requestedPreset="medium";state.requestedCrf=23;state.requestedCpu=2;state.bframeCount=4;state.lookaheadCount=20;state.cuTree=1;state.aqMode=2;state.aqStrength=1000;state.message="Codifica software in corso";
            home.conversion.renderConversionDashboard(state);home.screen.conversion.cpuValue.setText("48,2%");home.screen.conversion.ramValue.setText("320 MiB");home.screen.conversion.batteryValue.setText("39,0 °C");
            state.sizeReport="Video 1 · Video_di_prova_1.mp4\n10 MB → 5 MB · −50%";home.conversion.renderSizeReport(state);image(home,"home-conversion.png");
            state.pauseRequested=true;state.paused=true;home.conversion.renderConversionDashboard(state);image(home,"home-paused.png");
        }finally{controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }
```

</details>

<!-- /test -->

<!-- test: HomeRenderTest.batchResults -->
<a id="test-692df0ca135adc5c"></a>
### `HomeRenderTest.batchResults`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Cache metadati, directory autorizzazioni, State e CPU/RAM/batteria testi inseriti manualmente; Robolectric native graphics.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.screen.sections.focusCard`, `ConversionService.rememberFailures`, `BatchRetry.select`, `ConversionService.State`, `state.failures.add`, `CompressionSummary.render`, `CompressionSummary.Item`, `home.conversion.renderConversionDashboard`, `home.conversion.renderSizeReport`, `VideoMetadataCache.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Report3esiti e errore audio predisposti, nessun file convertito.

**Limiti:** Ogni capture controlla PNG>10000byte, nessun golden. Percentuali CPU/RAM/temperatura non sono misurate. activeConversion crea2PNG ma vale1metodo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void batchResults()throws Exception{
        ActivityController<MainActivity> controller=screen();try{
            HomeController home=controller.get().home;home.screen.sections.focusCard(MainActivity.CARD_CONVERSION);
            android.content.Intent task=new android.content.Intent(home.activity,ConversionService.class).setAction(ConversionService.START)
                .putStringArrayListExtra("inputs",new ArrayList<>(home.state.selectedInputs))
                .putStringArrayListExtra("names",new ArrayList<>(home.state.selectedNames))
                .putStringArrayListExtra("folders",new ArrayList<>(List.of("content://preview/tree/a","content://preview/tree/a","content://preview/tree/a")))
                .putExtra("crf",23).putExtra("preset",5).putExtra("onlySmaller",true);
            ConversionService.rememberFailures(home.activity,BatchRetry.select(task,List.of(1)));
            ConversionService.State state=new ConversionService.State();state.batchIndex=3;state.batchTotal=3;state.succeeded=1;state.failed=1;state.notSaved=1;state.onlySmaller=true;state.filePercent=100;state.source="Video_di_prova_3.mp4";state.requestedCrf=23;state.requestedPreset="medium";state.requestedCpu=1;state.message="Coda terminata · 1 salvato · 1 fallito · 1 senza risparmio";
            state.failures.add(new ConversionFailure(2,"Video_di_prova_2.mp4","Analisi del file",new java.io.IOException("Traccia audio non supportata")));
            state.sizeReport=CompressionSummary.render(List.of(
                new CompressionSummary.Item(1,"Video_di_prova_1.mp4",CompressionSummary.Status.COMPLETE,10000000,5000000),
                new CompressionSummary.Item(2,"Video_di_prova_2.mp4",CompressionSummary.Status.FAILED,-1,-1),
                new CompressionSummary.Item(3,"Video_di_prova_3.mp4",CompressionSummary.Status.NO_SAVING,2000000,2100000)));
            home.conversion.renderConversionDashboard(state);home.conversion.renderSizeReport(state);image(home,"home-results.png");
        }finally{ConversionService.rememberFailures(controller.get(),null);controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }
```

</details>

<!-- /test -->

<!-- test: HomeRenderTest.clearedHome -->
<a id="test-343abe8308092fb0"></a>
### `HomeRenderTest.clearedHome`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Cache metadati, directory autorizzazioni, State e CPU/RAM/batteria testi inseriti manualmente; Robolectric native graphics.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/HomeRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.reset.clear`, `VideoMetadataCache.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Home dopo clear disegnata in PNG, senza confronto atteso.

**Limiti:** Ogni capture controlla PNG>10000byte, nessun golden. Percentuali CPU/RAM/temperatura non sono misurate. activeConversion crea2PNG ma vale1metodo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void clearedHome()throws Exception{
        ActivityController<MainActivity> controller=screen();try{
            HomeController home=controller.get().home;home.reset.clear();image(home,"home-cleared.png");
        }finally{controller.pause().stop().destroy();VideoMetadataCache.clear();}
    }
```

</details>

<!-- /test -->


<a id="class-imagelivelogtest"></a>
## ImageLiveLogTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java) · 6 metodi · area originaria: Componenti condivisi.

**Contesto applicativo della classe:** ImageLogStore; ImageLogExport/Selection; ImageFailureStore; ImageDashboard.

**Riferimenti produttivi:** [ImageLogStore](../../app/src/main/java/it/edo/codecprobe/ImageLogStore.java), [ImageLogExport](../../app/src/main/java/it/edo/codecprobe/ImageLogExport.java), [ImageFailureStore](../../app/src/main/java/it/edo/codecprobe/ImageFailureStore.java).

**Preparazione, file e simulazioni condivisi:** Log/JSONL veri; run/results artificiali via reflection; UI/picker shadow, export verso file://. Writer/executor e latch reali; sink lento fittizio.

**Confini comuni dell’audit:** Nessuna coda che converte davvero; contatori iniettati. Polling5ms/5s, worker deadlines2–10s. Due test C usano Context solo come directory base, non SAF.

**Metodi:** [runningQueueExportsTheClickSnapshotAndShowsAllCumulativeCounts](#test-cd3bba35bce7bb8b) · [pickerRecreationAndCancellationKeepTheSourceAndDeleteTheSnapshot](#test-80b9fcd4bddbd258) · [failedExportCleansUpAndAllowsRetryWhileTheQueueRuns](#test-f01dee5faf09f20f) · [concurrentEventsProduceCompletePrefixSnapshotsWithoutLosingAnyEvent](#test-758c5bfc029441d7) · [slowDestinationDoesNotHoldTheEventWriterLock](#test-3a985f0debfbcdb6) · [runningFailureArchiveExportContainsOnlyWholeJsonRecords](#test-8670833da21420dc)

<!-- test: ImageLiveLogTest.runningQueueExportsTheClickSnapshotAndShowsAllCumulativeCounts -->
<a id="test-cd3bba35bce7bb8b"></a>
### `ImageLiveLogTest.runningQueueExportsTheClickSnapshotAndShowsAllCumulativeCounts`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Log/JSONL veri; run/results artificiali via reflection; UI/picker shadow, export verso file://. Writer/executor e latch reali; sink lento fittizio.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `run.add`, `ImageRunState.Result`, `ImageLogStore.append`, `images.screen.sections.focusCard`, `images.screen.exportLog.isEnabled`, `images.screen.clearLog.isEnabled`, `images.screen.dashboard.summary.getText`, `run.results.size`, `images.screen.exportLog.performClick`, `images.selection.result`, `ImageConversionService.snapshot`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Click snapshot durante run iniettata esporta solo PRIMA, preserva log completo/count599; rimuove snapshot.

**Limiti:** Nessuna coda che converte davvero; contatori iniettati. Polling5ms/5s, worker deadlines2–10s. Due test C usano Context solo come directory base, non SAF.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void runningQueueExportsTheClickSnapshotAndShowsAllCumulativeCounts()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageRunState run=new ImageRunState();run.running=true;run.total=8500;run.current=600;
        for(int i=0;i<599;i++){
            ImageRunState.Outcome outcome=i<400?ImageRunState.Outcome.SAVED:i<590?ImageRunState.Outcome.FAILED:i<598?ImageRunState.Outcome.NOT_SMALLER:ImageRunState.Outcome.ALREADY_DONE;
            run.add(new ImageRunState.Result("Foto "+i,3000,2000,null,"Esito",outcome,""));
        }
        stateField.set(null,run);ImageLogStore.append(context,ImageConversionService.LOG,"PRIMA\n\n");
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();File exported=File.createTempFile("partial-export-",".txt");
        try{
            ImageController images=controller.get().images;images.screen.sections.focusCard(ImageScreen.HEIC);assertTrue(images.screen.exportLog.isEnabled());assertFalse(images.screen.clearLog.isEnabled());
            String counts=images.screen.dashboard.summary.getText().toString();assertTrue(counts,counts.contains("Foto 600 / 8500"));assertTrue(counts,counts.contains("Riuscite: 400 · Fallite: 190"));assertTrue(counts,counts.contains("Già presenti: 1 · Senza risparmio: 8 · Annullate: 0"));assertEquals(100,run.results.size());
            images.screen.exportLog.performClick();ShadowActivity.IntentForResult picker=awaitPicker(controller.get());
            assertEquals(ImageSelection.LOG,picker.requestCode);assertEquals(Intent.ACTION_CREATE_DOCUMENT,picker.intent.getAction());assertTrue(picker.intent.getStringExtra(Intent.EXTRA_TITLE).contains("parziale"));assertFalse(images.screen.exportLog.isEnabled());
            ImageLogStore.append(context,ImageConversionService.LOG,"DOPO IL CLICK\n\n");
            images.selection.result(ImageSelection.LOG,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(exported)));awaitExport(images);
            assertEquals("PRIMA\n\n",read(exported));assertEquals("PRIMA\n\nDOPO IL CLICK\n\n",read(new File(context.getFilesDir(),ImageConversionService.LOG)));
            assertTrue(ImageConversionService.snapshot().running);assertTrue(images.screen.exportLog.isEnabled());assertFalse(images.screen.clearLog.isEnabled());assertEquals(0,snapshots(context).length);
        }finally{controller.pause().stop().destroy();exported.delete();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageLiveLogTest.pickerRecreationAndCancellationKeepTheSourceAndDeleteTheSnapshot -->
<a id="test-80b9fcd4bddbd258"></a>
### `ImageLiveLogTest.pickerRecreationAndCancellationKeepTheSourceAndDeleteTheSnapshot`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Log/JSONL veri; run/results artificiali via reflection; UI/picker shadow, export verso file://. Writer/executor e latch reali; sink lento fittizio.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageLogStore.append`, `images.selection.log`, `ImageLogStore.reset`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Recreate picker conserva snapshot vecchio anche se log cambia; cancel elimina solo snapshot.

**Limiti:** Nessuna coda che converte davvero; contatori iniettati. Polling5ms/5s, worker deadlines2–10s. Due test C usano Context solo come directory base, non SAF.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void pickerRecreationAndCancellationKeepTheSourceAndDeleteTheSnapshot()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageLogStore.append(context,ImageConversionService.LOG,"ORIGINALE\n\n");
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();File output=File.createTempFile("recreated-export-",".txt");
        try{
            controller.get().images.selection.log();awaitPicker(controller.get());assertEquals(1,snapshots(context).length);
            controller.recreate();ImageController restored=controller.get().images;assertTrue(restored.logExport.busy());
            ImageLogStore.reset(context);ImageLogStore.append(context,ImageConversionService.LOG,"NUOVA CODA\n\n");
            restored.selection.result(ImageSelection.LOG,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(output)));awaitExport(restored);assertEquals("ORIGINALE\n\n",read(output));assertEquals(0,snapshots(context).length);
            restored.selection.log();awaitPicker(controller.get());File snapshot=snapshots(context)[0];
            restored.selection.result(ImageSelection.LOG,Activity.RESULT_CANCELED,null);assertFalse(snapshot.exists());assertFalse(restored.logExport.busy());assertEquals("NUOVA CODA\n\n",read(new File(context.getFilesDir(),ImageConversionService.LOG)));
        }finally{controller.pause().stop().destroy();output.delete();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageLiveLogTest.failedExportCleansUpAndAllowsRetryWhileTheQueueRuns -->
<a id="test-f01dee5faf09f20f"></a>
### `ImageLiveLogTest.failedExportCleansUpAndAllowsRetryWhileTheQueueRuns`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Log/JSONL veri; run/results artificiali via reflection; UI/picker shadow, export verso file://. Writer/executor e latch reali; sink lento fittizio.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageLogStore.append`, `images.selection.log`, `images.selection.result`, `images.state.notice.contains`, `images.screen.exportLog.isEnabled`, `ImageConversionService.snapshot`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Export verso directory fallisce, ripulisce e abilita retry senza fermare run iniettata.

**Limiti:** Nessuna coda che converte davvero; contatori iniettati. Polling5ms/5s, worker deadlines2–10s. Due test C usano Context solo come directory base, non SAF.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void failedExportCleansUpAndAllowsRetryWhileTheQueueRuns()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageRunState run=new ImageRunState();run.running=true;run.total=1;stateField.set(null,run);ImageLogStore.append(context,ImageConversionService.LOG,"EVENTO\n\n");
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            ImageController images=controller.get().images;images.selection.log();awaitPicker(controller.get());
            images.selection.result(ImageSelection.LOG,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(context.getCacheDir())));awaitExport(images);
            assertTrue(images.state.notice,images.state.notice.contains("non completata"));assertEquals(0,snapshots(context).length);assertTrue(images.screen.exportLog.isEnabled());assertTrue(ImageConversionService.snapshot().running);
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageLiveLogTest.concurrentEventsProduceCompletePrefixSnapshotsWithoutLosingAnyEvent -->
<a id="test-758c5bfc029441d7"></a>
### `ImageLiveLogTest.concurrentEventsProduceCompletePrefixSnapshotsWithoutLosingAnyEvent`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Log/JSONL veri; run/results artificiali via reflection; UI/picker shadow, export verso file://. Writer/executor e latch reali; sink lento fittizio.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageLogStore.append`, `ImageLogStore.snapshot`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** 80 append concorrenti e8 snapshot reali sono prefissi completi senza righe perse; interleaving non forzato tra ogni coppia.

**Limiti:** Nessuna coda che converte davvero; contatori iniettati. Polling5ms/5s, worker deadlines2–10s. Due test C usano Context solo come directory base, non SAF.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void concurrentEventsProduceCompletePrefixSnapshotsWithoutLosingAnyEvent()throws Exception{
        var context=RuntimeEnvironment.getApplication();ExecutorService writer=Executors.newSingleThreadExecutor();List<File> copies=new ArrayList<>();
        try{
            ImageLogStore.append(context,ImageConversionService.LOG,"INIZIO\n\n");
            Future<?> writing=writer.submit(()->{try{for(int i=0;i<80;i++)ImageLogStore.append(context,ImageConversionService.LOG,"EVENTO "+i+" "+"x".repeat(10000)+"\n\n");}catch(IOException error){throw new UncheckedIOException(error);}});
            for(int i=0;i<8;i++)copies.add(ImageLogStore.snapshot(context,ImageConversionService.LOG));writing.get(10,TimeUnit.SECONDS);
            String complete=read(new File(context.getFilesDir(),ImageConversionService.LOG));
            for(File snapshot:copies){String text=read(snapshot);assertTrue(complete.startsWith(text));assertTrue(text.endsWith("\n\n"));}
            assertEquals(81,complete.split("\n\n").length);assertTrue(complete.contains("EVENTO 79 "));
        }finally{writer.shutdownNow();for(File file:copies)file.delete();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageLiveLogTest.slowDestinationDoesNotHoldTheEventWriterLock -->
<a id="test-3a985f0debfbcdb6"></a>
### `ImageLiveLogTest.slowDestinationDoesNotHoldTheEventWriterLock`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Log/JSONL veri; run/results artificiali via reflection; UI/picker shadow, export verso file://. Writer/executor e latch reali; sink lento fittizio.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageLogStore.append`, `ImageLogStore.snapshot`, `ImageLogStore.copy`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Sink bloccato da latch non trattiene lock append; evento viene scritto mentre export ancora fermo.

**Limiti:** Nessuna coda che converte davvero; contatori iniettati. Polling5ms/5s, worker deadlines2–10s. Due test C usano Context solo come directory base, non SAF.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void slowDestinationDoesNotHoldTheEventWriterLock()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageLogStore.append(context,ImageConversionService.LOG,"INIZIO\n\n");File snapshot=ImageLogStore.snapshot(context,ImageConversionService.LOG);
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);ExecutorService workers=Executors.newFixedThreadPool(2);
        try{
            Future<?> export=workers.submit(()->{try(InputStream in=new FileInputStream(snapshot)){ImageLogStore.copy(in,new OutputStream(){public void write(int value){}public void write(byte[] bytes,int offset,int length)throws IOException{entered.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new IOException("Timeout destinazione");}catch(InterruptedException error){Thread.currentThread().interrupt();throw new InterruptedIOException();}}});}catch(IOException error){throw new UncheckedIOException(error);}});
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            workers.submit(()->{try{ImageLogStore.append(context,ImageConversionService.LOG,"DURANTE EXPORT\n\n");}catch(IOException error){throw new UncheckedIOException(error);}}).get(2,TimeUnit.SECONDS);
            assertFalse(export.isDone());assertTrue(read(new File(context.getFilesDir(),ImageConversionService.LOG)).contains("DURANTE EXPORT"));release.countDown();export.get(5,TimeUnit.SECONDS);
        }finally{release.countDown();workers.shutdownNow();snapshot.delete();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageLiveLogTest.runningFailureArchiveExportContainsOnlyWholeJsonRecords -->
<a id="test-8670833da21420dc"></a>
### `ImageLiveLogTest.runningFailureArchiveExportContainsOnlyWholeJsonRecords`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Log/JSONL veri; run/results artificiali via reflection; UI/picker shadow, export verso file://. Writer/executor e latch reali; sink lento fittizio.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageLiveLogTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageOptions.load`, `ImageRunState.Result`, `ImageFailureStore.append`, `images.screen.exportErrors.isEnabled`, `images.selection.errors`, `images.selection.result`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** JSONL export di errori durante run contiene solo record intero al click, non quello aggiunto dopo.

**Limiti:** Nessuna coda che converte davvero; contatori iniettati. Polling5ms/5s, worker deadlines2–10s. Due test C usano Context solo come directory base, non SAF.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void runningFailureArchiveExportContainsOnlyWholeJsonRecords()throws Exception{
        var context=RuntimeEnvironment.getApplication();ImageRunState run=new ImageRunState();run.running=true;run.total=2;stateField.set(null,run);
        ImageOptions options=ImageOptions.load(context);ImageRunState.Result failure=new ImageRunState.Result("Errore.jpg",0,0,null,"Non supportata",ImageRunState.Outcome.FAILED,"stack\ntrace");
        ImageFailureStore.append(context,1,1,"content://photo/1","content://folder/out",options,failure);
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();File output=File.createTempFile("partial-errors-",".jsonl");
        try{
            ImageController images=controller.get().images;assertTrue(images.screen.exportErrors.isEnabled());images.selection.errors();ShadowActivity.IntentForResult picker=awaitPicker(controller.get());assertEquals(ImageSelection.ERRORS,picker.requestCode);
            ImageFailureStore.append(context,1,2,"content://photo/2","content://folder/out",options,failure);
            images.selection.result(ImageSelection.ERRORS,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(output)));awaitExport(images);
            List<String> lines=Files.readAllLines(output.toPath(),StandardCharsets.UTF_8);assertEquals(1,lines.size());assertEquals("content://photo/1",new JSONObject(lines.get(0)).getString("input"));assertEquals(2,Files.readAllLines(new File(context.getFilesDir(),ImageFailureStore.FILE).toPath()).size());
        }finally{controller.pause().stop().destroy();output.delete();}
    }
```

</details>

<!-- /test -->


<a id="class-imagerendertest"></a>
## ImageRenderTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageRenderTest.java) · 1 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** ImageSettingsScreen; View.draw/Bitmap.compress.

**Riferimenti produttivi:** [ImageSettingsScreen](../../app/src/main/java/it/edo/codecprobe/ImageSettingsScreen.java).

**Preparazione, file e simulazioni condivisi:** Activity Robolectric GraphicsMode.NATIVE; PNG build/reports/ui/image-settings.png.

**Confini comuni dell’audit:** Solo compressione PNG riuscita, nessun confronto visivo o impostazione salvata.

**Metodi:** [settings](#test-060eb73bb8ee1e4c)

<!-- test: ImageRenderTest.settings -->
<a id="test-060eb73bb8ee1e4c"></a>
### `ImageRenderTest.settings`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity Robolectric GraphicsMode.NATIVE; PNG build/reports/ui/image-settings.png.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `screen.ui.dp`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Disegna pagina impostazioni Foto in PNG senza golden.

**Limiti:** Solo compressione PNG riuscita, nessun confronto visivo o impostazione salvata.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void settings()throws Exception{
        var controller=Robolectric.buildActivity(ImageSettingsActivity.class).setup();try{ImageSettingsScreen screen=controller.get().screen;View page=screen.page;page.measure(View.MeasureSpec.makeMeasureSpec(screen.ui.dp(360),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());page.getViewTreeObserver().dispatchOnPreDraw();float density=controller.get().getResources().getDisplayMetrics().density;Bitmap bitmap=Bitmap.createBitmap(360,(int)Math.ceil(page.getMeasuredHeight()/density),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(screen.ui.bg);canvas.scale(1/density,1/density);page.draw(canvas);File dir=new File("build/reports/ui");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,"image-settings.png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}}finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-imagevirtualreporttest"></a>
## ImageVirtualReportTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageVirtualReportTest.java) · 3 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** ImageResultList; ImageController.refresh; ImageLogPreview.text.

**Riferimenti produttivi:** [ImageResultList](../../app/src/main/java/it/edo/codecprobe/ImageResultList.java), [ImageController](../../app/src/main/java/it/edo/codecprobe/ImageController.java), [ImageLogPreview](../../app/src/main/java/it/edo/codecprobe/ImageLogPreview.java).

**Preparazione, file e simulazioni condivisi:** 100 item manuali/callback in memoria e run8500 iniettata; ListView Robolectric640px.

**Confini comuni dell’audit:** Assume meno20 righe visibili a layout fisso, nessun frame timing. Hidden refresh invocato direttamente, non timer reale.

**Metodi:** [oneHundredResultsUseOnlyAVisibleSubsetOfViewsAndRecycleTheirActions](#test-8edfd8a941e7bd4f) · [hiddenProgressAndLogAreNotRewrittenByTheTimer](#test-db0c6a18765d95a4) · [visibleLogPreviewIsBoundedAndContainsTheNewestEvent](#test-7cfb7b648f749c28)

<!-- test: ImageVirtualReportTest.oneHundredResultsUseOnlyAVisibleSubsetOfViewsAndRecycleTheirActions -->
<a id="test-8edfd8a941e7bd4f"></a>
### `ImageVirtualReportTest.oneHundredResultsUseOnlyAVisibleSubsetOfViewsAndRecycleTheirActions`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 100 item manuali/callback in memoria e run8500 iniettata; ListView Robolectric640px.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageVirtualReportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageResultList.Item`, `view.submit`, `view.list.getAdapter`, `view.list.getChildCount`, `view.item`, `view.size`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** 100 risultati con<20 view attaccate, riciclo row0→99 invoca callback99 e conserva Item50 dopo shift.

**Limiti:** Assume meno20 righe visibili a layout fisso, nessun frame timing. Hidden refresh invocato direttamente, non timer reale.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void oneHundredResultsUseOnlyAVisibleSubsetOfViewsAndRecycleTheirActions(){
        var controller=Robolectric.buildActivity(Activity.class).setup();try{
            AppUi ui=new AppUi(controller.get());LinearLayout root=new LinearLayout(controller.get());root.setOrientation(LinearLayout.VERTICAL);controller.get().setContentView(root);
            ImageResultList view=new ImageResultList(ui,root);ArrayList<ImageResultList.Item> items=new ArrayList<>();int[] clicked={-1};
            for(int i=0;i<100;i++){int id=i;items.add(new ImageResultList.Item(i,"Photo "+i,"","Photo "+i+"\nSaved",null,false,()->clicked[0]=id,null,false));}
            view.submit(items,49);layout(view);Shadows.shadowOf(Looper.getMainLooper()).idle();layout(view);
            assertEquals(100,view.list.getAdapter().getCount());assertTrue(view.list.getChildCount()>0);assertTrue("Off-screen rows must not be attached",view.list.getChildCount()<20);
            View recycled=view.list.getAdapter().getView(0,null,view.list);View rebound=view.list.getAdapter().getView(99,recycled,view.list);assertSame(recycled,rebound);
            ((Button)((LinearLayout)rebound).getChildAt(1)).performClick();assertEquals(99,clicked[0]);
            ImageResultList.Item retained=items.get(50);ArrayList<ImageResultList.Item> shifted=new ArrayList<>(items.subList(1,100));shifted.add(new ImageResultList.Item(100,"Last","","Last",null,false,null,null,false));
            view.submit(shifted,50);layout(view);assertSame(retained,view.item(50));assertTrue(view.list.getChildCount()<20);assertEquals(100,view.size());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageVirtualReportTest.hiddenProgressAndLogAreNotRewrittenByTheTimer -->
<a id="test-db0c6a18765d95a4"></a>
### `ImageVirtualReportTest.hiddenProgressAndLogAreNotRewrittenByTheTimer`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 100 item manuali/callback in memoria e run8500 iniettata; ListView Robolectric640px.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageVirtualReportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageConversionService.class.getDeclaredField`, `images.render`, `images.screen.sections.focusCard`, `images.screen.dashboard.summary.getText`, `images.screen.log.setText`, `images.refresh`, `images.screen.log.getText`, `images.screen.toggleLog.performClick`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Refresh non riscrive log/dashboard nascosti ma li aggiorna quando card visibile.

**Limiti:** Assume meno20 righe visibili a layout fisso, nessun frame timing. Hidden refresh invocato direttamente, non timer reale.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void hiddenProgressAndLogAreNotRewrittenByTheTimer()throws Exception{
        var c=RuntimeEnvironment.getApplication();c.getSharedPreferences("image-workspace",0).edit().clear().commit();var field=ImageConversionService.class.getDeclaredField("state");field.setAccessible(true);Object previous=field.get(null);
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{
            var images=controller.get().images;ImageRunState run=new ImageRunState();run.running=true;run.total=8500;run.current=100;run.log="first";field.set(null,run);images.render();
            images.screen.sections.focusCard(ImageScreen.FILES);String original=images.screen.dashboard.summary.getText().toString();images.screen.log.setText("hidden");run.current=500;run.log="new log";images.refresh();
            assertEquals(original,images.screen.dashboard.summary.getText().toString());assertEquals("hidden",images.screen.log.getText().toString());
            images.screen.sections.focusCard(ImageScreen.HEIC);images.refresh();assertTrue(images.screen.dashboard.summary.getText().toString().contains("500 / 8500"));
            images.screen.sections.focusCard(ImageScreen.DETAILS);images.screen.toggleLog.performClick();images.refresh();assertEquals("new log",images.screen.log.getText().toString());
        }finally{field.set(null,previous);controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageVirtualReportTest.visibleLogPreviewIsBoundedAndContainsTheNewestEvent -->
<a id="test-7cfb7b648f749c28"></a>
### `ImageVirtualReportTest.visibleLogPreviewIsBoundedAndContainsTheNewestEvent`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 100 item manuali/callback in memoria e run8500 iniettata; ListView Robolectric640px.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageVirtualReportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `log.append`, `ImageLogPreview.text`, `log.toString`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Preview log include evento99, esclude vecchi, max4000 caratteri, tratta vuoto e stringa100k.

**Limiti:** Assume meno20 righe visibili a layout fisso, nessun frame timing. Hidden refresh invocato direttamente, non timer reale.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void visibleLogPreviewIsBoundedAndContainsTheNewestEvent(){
        StringBuilder log=new StringBuilder();for(int i=0;i<100;i++)log.append("event ").append(i).append('\n');String preview=ImageLogPreview.text(log.toString());assertTrue(preview.contains("event 99"));assertFalse(preview.contains("event 0\n"));assertTrue(preview.length()<=4000);
        assertEquals(4000,ImageLogPreview.text("x".repeat(100000)).length());assertEquals("",ImageLogPreview.text(""));
    }
```

</details>

<!-- /test -->


<a id="class-imageworkreporttest"></a>
## ImageWorkReportTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageWorkReportTest.java) · 1 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** ImageWorkReport.render; WorkReportView; ImageResultList; popup/clipboard.

**Riferimenti produttivi:** [ImageWorkReport](../../app/src/main/java/it/edo/codecprobe/ImageWorkReport.java), [WorkReportView](../../app/src/main/java/it/edo/codecprobe/WorkReportView.java), [ImageResultList](../../app/src/main/java/it/edo/codecprobe/ImageResultList.java).

**Preparazione, file e simulazioni condivisi:** Quattro esiti inventati e URI content://saved/photo senza dati; Activity e clipboard simulate.

**Confini comuni dell’audit:** Nessun salvataggio/cancel/codec produce gli esiti; apertura è solo ACTION_VIEW. Le identità view sono contratti interni.

**Metodi:** [savedOutputOpensWithReadPermissionAndFailuresHaveCopyableDetails](#test-22e330a55a0ddb71)

<!-- test: ImageWorkReportTest.savedOutputOpensWithReadPermissionAndFailuresHaveCopyableDetails -->
<a id="test-22e330a55a0ddb71"></a>
### `ImageWorkReportTest.savedOutputOpensWithReadPermissionAndFailuresHaveCopyableDetails`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Quattro esiti inventati e URI content://saved/photo senza dati; Activity e clipboard simulate.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageWorkReportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `run.add`, `ImageRunState.Result`, `images.report.render`, `images.report.results.size`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Totali/categorie e1MB risparmio, Apri HEIC con grant, errore copiabile senza chiudere, riga riusata e clear report.

**Limiti:** Nessun salvataggio/cancel/codec produce gli esiti; apertura è solo ACTION_VIEW. Le identità view sono contratti interni.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void savedOutputOpensWithReadPermissionAndFailuresHaveCopyableDetails() {
        var controller = Robolectric.buildActivity(ImageActivity.class).setup();
        try {
            ImageController images = controller.get().images;
            ImageRunState run = new ImageRunState();
            run.add(new ImageRunState.Result("Foto.jpg", 3000000, 2000000, "content://saved/photo", "Salvato", ImageRunState.Outcome.SAVED, "Metadati verificati"));
            run.add(new ImageRunState.Result("Errore.jpg", 9000000, 0, null, "JPEG non supportato", ImageRunState.Outcome.FAILED, "Dettaglio tecnico"));
            run.add(new ImageRunState.Result("Grande.jpg", 1000000, 2000000, null, "Nessun risparmio", ImageRunState.Outcome.NOT_SMALLER, "Non salvato"));
            run.add(new ImageRunState.Result("Annullata.jpg", 5000000, 0, null, "Annullata", ImageRunState.Outcome.CANCELLED, ""));
            images.report.render(run);
            WorkReportView report = images.screen.workReport;
            String summary = report.summary.getText().toString();
            assertTrue(summary.contains("Salvate 1 · fallite 1 · senza risparmio 1 · annullate 1"));
            assertTrue(summary.contains("3,00 MB → 2,00 MB"));
            assertTrue(summary.contains("Spazio risparmiato: 1,00 MB"));
            find(ImageVirtualReportTest.row(images,0,null), "Apri risultato:").performClick();
            Intent opened = Shadows.shadowOf(controller.get()).getNextStartedActivity();
            assertEquals("image/heic", opened.getType());
            assertEquals("content://saved/photo", opened.getDataString());
            assertNotEquals(0, opened.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            find(ImageVirtualReportTest.row(images,1,null), "Mostra errore: Foto 2").performClick();
            AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            String copied = controller.get().getSystemService(ClipboardManager.class).getPrimaryClip().getItemAt(0).getText().toString();
            assertTrue(copied.contains("JPEG non supportato"));
            assertTrue(copied.contains("Dettaglio tecnico"));
            assertTrue(dialog.isShowing());
            View firstRow = ImageVirtualReportTest.row(images,0,null);
            images.report.render(new ImageRunState(run));
            assertSame(firstRow, ImageVirtualReportTest.row(images,0,firstRow));
            images.report.render(new ImageRunState());
            assertEquals(0, images.report.results.size());
            assertEquals("Nessuna conversione eseguita.", report.summary.getText().toString());
        } finally { controller.pause().stop().destroy(); }
    }
```

</details>

<!-- /test -->


<a id="class-incrementalreporttest"></a>
## IncrementalReportTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/IncrementalReportTest.java) · 1 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** HomeConversion.renderSizeReport.

**Riferimenti produttivi:** [HomeConversion](../../app/src/main/java/it/edo/codecprobe/HomeConversion.java).

**Preparazione, file e simulazioni condivisi:** State running e sizeReport stringhe preparati manualmente; Activity Robolectric.

**Confini comuni dell’audit:** Non prova produzione incrementale dal servizio o aggiornamenti thread/observer: chiama direttamente render.

**Metodi:** [reportIsVisibleWhileRunningAndUpdatesWithoutWaitingForQueueEnd](#test-a5774ef54e7d48c4)

<!-- test: IncrementalReportTest.reportIsVisibleWhileRunningAndUpdatesWithoutWaitingForQueueEnd -->
<a id="test-a5774ef54e7d48c4"></a>
### `IncrementalReportTest.reportIsVisibleWhileRunningAndUpdatesWithoutWaitingForQueueEnd`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): State running e sizeReport stringhe preparati manualmente; Activity Robolectric.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/IncrementalReportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ConversionService.State`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Report visibile mentre running, nuova stringa aggiorna subito e titolo cambia a fine run; vuoto nasconde pannello.

**Limiti:** Non prova produzione incrementale dal servizio o aggiornamenti thread/observer: chiama direttamente render.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void reportIsVisibleWhileRunningAndUpdatesWithoutWaitingForQueueEnd()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try{
            
            ConversionService.State state=new ConversionService.State();state.running=true;state.sizeReport="Video 1 · 10 MB → 5 MB";
            activity.home.conversion.renderSizeReport(state);
            assertEquals(View.VISIBLE,((View)activity.home.screen.conversion.sizeReportPanel).getVisibility());
            assertEquals("REPORT LAVORI · IN CORSO",((TextView)activity.home.screen.conversion.sizeReportTitle).getText().toString());
            assertEquals(state.sizeReport,((TextView)activity.home.screen.conversion.sizeReport).getText().toString());
            state.sizeReport+="\nVideo 2 · 20 MB → 8 MB";activity.home.conversion.renderSizeReport(state);
            assertEquals(state.sizeReport,((TextView)activity.home.screen.conversion.sizeReport).getText().toString());
            state.running=false;activity.home.conversion.renderSizeReport(state);
            assertEquals("REPORT LAVORI",((TextView)activity.home.screen.conversion.sizeReportTitle).getText().toString());
            state.running=true;state.sizeReport="";activity.home.conversion.renderSizeReport(state);
            assertEquals(View.GONE,((View)activity.home.screen.conversion.sizeReportPanel).getVisibility());
        }finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-mediadestinationsrendertest"></a>
## MediaDestinationsRenderTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/MediaDestinationsRenderTest.java) · 1 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** MediaDestinationsView/Controller; Harness Host; View.draw.

**Riferimenti produttivi:** [MediaDestinationsView](../../app/src/main/java/it/edo/codecprobe/MediaDestinationsView.java).

**Preparazione, file e simulazioni condivisi:** Provider Directories e grant shadow; loop tutti MediaKind × accessfalse/true;6 PNG, unico metodo JUnit.

**Confini comuni dell’audit:** Readiness controllata sul provider simulato; nessun golden, permesso reale o utilizzo sul percorso Compose Foto. Polling5ms/5s.

**Metodi:** [sameDestinationComponentRendersVerifiedAndMissingAccessForEveryMedia](#test-1f579fa2ff4704d3)

<!-- test: MediaDestinationsRenderTest.sameDestinationComponentRendersVerifiedAndMissingAccessForEveryMedia -->
<a id="test-1f579fa2ff4704d3"></a>
### `MediaDestinationsRenderTest.sameDestinationComponentRendersVerifiedAndMissingAccessForEveryMedia`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Directories e grant shadow; loop tutti MediaKind × accessfalse/true;6 PNG, unico metodo JUnit.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/MediaDestinationsRenderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `MediaKind.values`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Pannello comune risulta ready solo con grant inventato per ogni media e produce screenshot dei6stati.

**Limiti:** Readiness controllata sul provider simulato; nessun golden, permesso reale o utilizzo sul percorso Compose Foto. Polling5ms/5s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void sameDestinationComponentRendersVerifiedAndMissingAccessForEveryMedia()throws Exception{
        ShadowContentResolver.registerProviderInternal("destinations",new MediaDestinationsTest.Directories());
        for(MediaKind kind:MediaKind.values())for(boolean allowed:new boolean[]{false,true}){
            var controller=Robolectric.buildActivity(Activity.class).setup();MediaDestinationsTest.Harness host=new MediaDestinationsTest.Harness(controller.get(),kind);
            try{
                controller.get().setContentView(host.root);
                try{controller.get().getContentResolver().releasePersistableUriPermission(Uri.parse(MediaDestinationsTest.A),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(SecurityException ignored){}
                if(allowed)MediaDestinationsTest.grant(controller.get(),MediaDestinationsTest.A);
                host.inputs.addAll(Arrays.asList("content://source/1","content://source/2"));host.names.addAll(Arrays.asList("Primo file","Secondo file"));host.controller.render();
                long end=System.currentTimeMillis()+5000;while(host.changes<2&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}
                assertTrue(host.changes>=2);assertEquals(allowed,host.controller.ready());
                View page=host.root;int width=host.ui.dp(360);page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());page.getViewTreeObserver().dispatchOnPreDraw();
                float density=controller.get().getResources().getDisplayMetrics().density;Bitmap bitmap=Bitmap.createBitmap(360,(int)Math.ceil(page.getMeasuredHeight()/density),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(host.ui.bg);canvas.scale(1/density,1/density);page.draw(canvas);
                File dir=new File("build/reports/ui");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,"destination-"+kind.name().toLowerCase(Locale.ROOT)+"-"+(allowed?"granted":"missing")+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
            }finally{host.controller.close();controller.pause().stop().destroy();}
        }
    }
```

</details>

<!-- /test -->


<a id="class-metadatamemorycachetest"></a>
## MetadataMemoryCacheTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/MetadataMemoryCacheTest.java) · 2 metodi · area originaria: Componenti condivisi.

**Contesto applicativo della classe:** MetadataMemoryCache.put/get/remove/clear/epoch/bytes.

**Riferimenti produttivi:** [MetadataMemoryCache](../../app/src/main/java/it/edo/codecprobe/MetadataMemoryCache.java).

**Preparazione, file e simulazioni condivisi:** Stringhe e peso dichiarato, epoche sincrone; nessun worker reale.

**Confini comuni dell’audit:** Non misura memoria effettiva e non avvia letture concorrenti: simula esito tardivo passando un epoch vecchio.

**Metodi:** [byteBudgetEvictsLeastRecentlyUsedMetadata](#test-81b5c09885ac90f6) · [reloadAndClearRejectInFlightOldReads](#test-2d86b41a37066496)

<!-- test: MetadataMemoryCacheTest.byteBudgetEvictsLeastRecentlyUsedMetadata -->
<a id="test-81b5c09885ac90f6"></a>
### `MetadataMemoryCacheTest.byteBudgetEvictsLeastRecentlyUsedMetadata`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Stringhe e peso dichiarato, epoche sincrone; nessun worker reale.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/MetadataMemoryCacheTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** LRU entro budget 10 e rigetto della singola voce con peso 11.

**Limiti:** Non misura memoria effettiva e non avvia letture concorrenti: simula esito tardivo passando un epoch vecchio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void byteBudgetEvictsLeastRecentlyUsedMetadata() {
        MetadataMemoryCache<String> cache=new MetadataMemoryCache<>(100,10);
        long epoch=cache.epoch();cache.put("a","first",4,epoch);cache.put("b","second",4,epoch);
        cache.get("a");cache.put("c","third",4,epoch);
        assertEquals(8,cache.bytes());assertEquals("first",cache.get("a"));assertNull(cache.get("b"));
        assertFalse(cache.put("huge","oversized",11,epoch));assertEquals(2,cache.size());
    }
```

</details>

<!-- /test -->

<!-- test: MetadataMemoryCacheTest.reloadAndClearRejectInFlightOldReads -->
<a id="test-2d86b41a37066496"></a>
### `MetadataMemoryCacheTest.reloadAndClearRejectInFlightOldReads`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Stringhe e peso dichiarato, epoche sincrone; nessun worker reale.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/MetadataMemoryCacheTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Remove/clear cambiano epoca e rifiutano risultati della generazione precedente.

**Limiti:** Non misura memoria effettiva e non avvia letture concorrenti: simula esito tardivo passando un epoch vecchio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void reloadAndClearRejectInFlightOldReads() {
        MetadataMemoryCache<String> cache=new MetadataMemoryCache<>(2,100);
        long old=cache.epoch();cache.put("photo","before",1,old);cache.remove("photo");
        assertFalse(cache.put("photo","stale",1,old));
        long current=cache.epoch();cache.put("photo","after",1,current);cache.clear();
        assertFalse(cache.put("photo","late",1,current));assertEquals(0,cache.bytes());assertEquals(0,cache.size());
    }
```

</details>

<!-- /test -->


<a id="class-photocomposerendertest"></a>
## PhotoComposeRenderTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt) · 7 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** PhotoWorkspace/PhotoCompose; Roborazzi.captureRoboImage; PhotoBasicDetails.

**Riferimenti produttivi:** [PhotoWorkspace](../../app/src/main/java/it/edo/codecprobe/PhotoWorkspace.kt), [PhotoBasicDetails](../../app/src/main/java/it/edo/codecprobe/PhotoBasicDetails.kt).

**Preparazione, file e simulazioni condivisi:** Robolectric native graphics, Info JPEG iniettata; fixture JPEG valida per basicDetailsCard; proprietà roborazzi.test.record=true forzata e ripristinata.

**Confini comuni dell’audit:** Sette capture in modalità RECORD: scrivono PNG e controllano solo file>3000byte. Nessuna golden o verifica pixel. progressInterop non simula conversione attiva.

**Metodi:** [floatingWorkspaceFrame](#test-8fcf2fb286deb28a) · [floatingDark](#test-958b628869ff48d5) · [savingAndPermissions](#test-1ee3bf0835346c4d) · [metadataDark](#test-14dc6f16500aca2b) · [qualityDark](#test-8b126cd40d0165c1) · [basicDetailsCard](#test-0dcbb4255d6f2755) · [progressInterop](#test-6396c6c661b19dc7)

<!-- test: PhotoComposeRenderTest.floatingWorkspaceFrame -->
<a id="test-8fcf2fb286deb28a"></a>
### `PhotoComposeRenderTest.floatingWorkspaceFrame`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric native graphics, Info JPEG iniettata; fixture JPEG valida per basicDetailsCard; proprietà roborazzi.test.record=true forzata e ripristinata.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `capture(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Screenshot workspace Foto chiaro.

**Limiti:** Sette capture in modalità RECORD: scrivono PNG e controllano solo file>3000byte. Nessuna golden o verifica pixel. progressInterop non simula conversione attiva.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun floatingWorkspaceFrame() = capture(ImageScreen.FILES, "image-floating-workspace.png")
```

</details>

<!-- /test -->

<!-- test: PhotoComposeRenderTest.floatingDark -->
<a id="test-958b628869ff48d5"></a>
### `PhotoComposeRenderTest.floatingDark`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric native graphics, Info JPEG iniettata; fixture JPEG valida per basicDetailsCard; proprietà roborazzi.test.record=true forzata e ripristinata.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `capture(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Screenshot workspace Foto scuro.

**Limiti:** Sette capture in modalità RECORD: scrivono PNG e controllano solo file>3000byte. Nessuna golden o verifica pixel. progressInterop non simula conversione attiva.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test @Config(qualifiers = "w360dp-h800dp-night-xhdpi")
    fun floatingDark() = capture(ImageScreen.FILES, "image-floating-dark.png")
```

</details>

<!-- /test -->

<!-- test: PhotoComposeRenderTest.savingAndPermissions -->
<a id="test-1ee3bf0835346c4d"></a>
### `PhotoComposeRenderTest.savingAndPermissions`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric native graphics, Info JPEG iniettata; fixture JPEG valida per basicDetailsCard; proprietà roborazzi.test.record=true forzata e ripristinata.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `open(…)`, `it(…)`, `save(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Scroll permessi/destinazioni e registra immagine.

**Limiti:** Sette capture in modalità RECORD: scrivono PNG e controllano solo file>3000byte. Nessuna golden o verifica pixel. progressInterop non simula conversione attiva.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun savingAndPermissions() {
        open(ImageScreen.FILES)
        compose.waitUntil(5_000) { compose.activity.images.screen.originRows.childCount > 1 }
        compose.onNodeWithTag("photo-scroll").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        save("image-files.png")
    }
```

</details>

<!-- /test -->

<!-- test: PhotoComposeRenderTest.metadataDark -->
<a id="test-14dc6f16500aca2b"></a>
### `PhotoComposeRenderTest.metadataDark`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric native graphics, Info JPEG iniettata; fixture JPEG valida per basicDetailsCard; proprietà roborazzi.test.record=true forzata e ripristinata.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `open(…)`, `hasTestTag(…)`, `save(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Apre pannello dettagli in tema scuro e registra immagine.

**Limiti:** Sette capture in modalità RECORD: scrivono PNG e controllano solo file>3000byte. Nessuna golden o verifica pixel. progressInterop non simula conversione attiva.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test @Config(qualifiers = "w360dp-h800dp-night-xhdpi")
    fun metadataDark() {
        open(ImageScreen.FILES)
        compose.onNodeWithTag("photo-scroll").performScrollToNode(hasTestTag("photo-details"))
        compose.onNodeWithTag("photo-details").performClick()
        save("image-metadata-dark.png")
    }
```

</details>

<!-- /test -->

<!-- test: PhotoComposeRenderTest.qualityDark -->
<a id="test-8b126cd40d0165c1"></a>
### `PhotoComposeRenderTest.qualityDark`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric native graphics, Info JPEG iniettata; fixture JPEG valida per basicDetailsCard; proprietà roborazzi.test.record=true forzata e ripristinata.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `capture(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Screenshot compressione scura.

**Limiti:** Sette capture in modalità RECORD: scrivono PNG e controllano solo file>3000byte. Nessuna golden o verifica pixel. progressInterop non simula conversione attiva.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test @Config(qualifiers = "w360dp-h800dp-night-xhdpi")
    fun qualityDark() = capture(ImageScreen.COMPRESSION, "image-quality-dark.png")
```

</details>

<!-- /test -->

<!-- test: PhotoComposeRenderTest.basicDetailsCard -->
<a id="test-0dcbb4255d6f2755"></a>
### `PhotoComposeRenderTest.basicDetailsCard`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric native graphics, Info JPEG iniettata; fixture JPEG valida per basicDetailsCard; proprietà roborazzi.test.record=true forzata e ripristinata.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.state.inputs.clear`, `images.state.names.clear`, `images.state.inputs.add`, `images.state.names.add`, `images.open`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** JPEG48x32 valido con EXIF Test Camera: carica dettagli poi registra card, senza asserire valori nel test render.

**Limiti:** Sette capture in modalità RECORD: scrivono PNG e controllano solo file>3000byte. Nessuna golden o verifica pixel. progressInterop non simula conversione attiva.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun basicDetailsCard() {
        val photo = File.createTempFile("preview-card-", ".jpg")
        photo.writeBytes(PhotoBasicDetailsTest.fixture())
        try {
            compose.runOnIdle {
                val images = compose.activity.images
                images.state.inputs.clear(); images.state.names.clear()
                images.state.inputs.add(android.net.Uri.fromFile(photo).toString())
                images.state.names.add("Una foto simpatica.jpg")
                images.open()
            }
            open(ImageScreen.FILES)
            compose.runOnIdle { compose.activity.images.compose.onVisible(true) }
            compose.waitUntil(5_000) { !compose.activity.images.compose.state.previewLoading && !compose.activity.images.state.reading }
            compose.runOnIdle { compose.activity.images.compose.onVisible(false) }
            compose.onNodeWithTag("photo-details").performClick()
            compose.onNodeWithTag("photo-scroll").performScrollToNode(hasTestTag("photo-details-panel"))
            compose.onNodeWithTag("photo-scroll").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { it(0f, 240f) }
            save("image-details-card.png")
        } finally { photo.delete() }
    }
```

</details>

<!-- /test -->

<!-- test: PhotoComposeRenderTest.progressInterop -->
<a id="test-6396c6c661b19dc7"></a>
### `PhotoComposeRenderTest.progressInterop`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric native graphics, Info JPEG iniettata; fixture JPEG valida per basicDetailsCard; proprietà roborazzi.test.record=true forzata e ripristinata.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeRenderTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `capture(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Screenshot step HEIC/interop, nessun avanzamento codec.

**Limiti:** Sette capture in modalità RECORD: scrivono PNG e controllano solo file>3000byte. Nessuna golden o verifica pixel. progressInterop non simula conversione attiva.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun progressInterop() = capture(ImageScreen.HEIC, "image-conversion.png")
```

</details>

<!-- /test -->


<a id="class-photocomposetest"></a>
## PhotoComposeTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt) · 7 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** PhotoWorkspace Compose; PhotoCompose bridge; ImageController; PhotoDetailsLoader; ImageDestinations.

**Riferimenti produttivi:** [PhotoWorkspace](../../app/src/main/java/it/edo/codecprobe/PhotoWorkspace.kt), [ImageController](../../app/src/main/java/it/edo/codecprobe/ImageController.java), [PhotoDetailsLoader](../../app/src/main/java/it/edo/codecprobe/PhotoDetailsLoader.kt).

**Preparazione, file e simulazioni condivisi:** createAndroidComposeRule<ImageActivity> su Robolectric; nodi semantici e gesti touch veri nel renderer host; JPEG sintetici ImageWorkspaceTest.fixture e dialog shadow.

**Confini comuni dell’audit:** Non dispositivo Android. Non prova codec/pixel né jank; mainClock virtuale. waitUntil5s e testi/tag esatti; foto selezionate iniettando state anziché picker.

**Metodi:** [composeIsInstalledAndBackPreservesQuality](#test-e3db067255c4dfe7) · [pressingAGroupedChoiceWidensItAndCancellationDoesNotChangeQuality](#test-532b5189e4a8ddf1) · [detailsKeepFiveFieldsAndASeparateCardWhileLoadingAndAfterSelection](#test-2b79080893f1d59f) · [multiSelectionDetailsListNamesAndDoNotShowThePreviousPhotoMetadata](#test-db7acf6363e5ad44) · [longPressAndSwitchApplyExceptionsToExactlyTheSelectedFiles](#test-37639ad17ed3bd8d) · [clearUsesExistingConfirmationAndNeverDeletesOriginals](#test-fb6f0634b77c7761) · [scrollingToTheEndKeepsPermissionsAboveFloatingButtons](#test-71c93ba1b6d8742e)

<!-- test: PhotoComposeTest.composeIsInstalledAndBackPreservesQuality -->
<a id="test-e3db067255c4dfe7"></a>
### `PhotoComposeTest.composeIsInstalledAndBackPreservesQuality`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): createAndroidComposeRule<ImageActivity> su Robolectric; nodi semantici e gesti touch veri nel renderer host; JPEG sintetici ImageWorkspaceTest.fixture e dialog shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Nodo photo-new reale installato; click qualità55 e back conservano configurazione e unica Compose root.

**Limiti:** Non dispositivo Android. Non prova codec/pixel né jank; mainClock virtuale. waitUntil5s e testi/tag esatti; foto selezionate iniettando state anziché picker.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun composeIsInstalledAndBackPreservesQuality() {
        compose.onNodeWithTag("photo-new").assertIsDisplayed().performClick()
        val tree = compose.onRoot().printToString()
        compose.runOnIdle { assertEquals(tree, ImageScreen.FILES, compose.activity.images.screen.sections.current()) }
        compose.onNodeWithTag("photo-next").performClick()
        compose.onNodeWithText("55", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(55, compose.activity.images.state.options.quality) }
        compose.onNodeWithTag("photo-back").performClick()
        compose.onNodeWithTag("photo-back").performClick()
        compose.onNodeWithTag("photo-new").assertIsDisplayed().performClick()
        val restoredTree = compose.onRoot().printToString()
        compose.runOnIdle { assertEquals(restoredTree, ImageScreen.FILES, compose.activity.images.screen.sections.current()) }
        compose.runOnIdle {
            val images = compose.activity.images
            assertTrue(images.compose.view.isShown)
            assertSame(images.compose.view, compose.activity.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0))
            assertEquals(55, images.state.options.quality)
        }
    }
```

</details>

<!-- /test -->

<!-- test: PhotoComposeTest.pressingAGroupedChoiceWidensItAndCancellationDoesNotChangeQuality -->
<a id="test-532b5189e4a8ddf1"></a>
### `PhotoComposeTest.pressingAGroupedChoiceWidensItAndCancellationDoesNotChangeQuality`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): createAndroidComposeRule<ImageActivity> su Robolectric; nodi semantici e gesti touch veri nel renderer host; JPEG sintetici ImageWorkspaceTest.fixture e dialog shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `down(…)`, `cancel(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Press touch allarga scelta50 senza applicare; cancel ripristina larghezza entro1px e solo click cambia qualità.

**Limiti:** Non dispositivo Android. Non prova codec/pixel né jank; mainClock virtuale. waitUntil5s e testi/tag esatti; foto selezionate iniettando state anziché picker.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun pressingAGroupedChoiceWidensItAndCancellationDoesNotChangeQuality() {
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-next").performClick()
        val choice = compose.onNodeWithText("50")
        val resting = choice.fetchSemanticsNode().boundsInRoot.width
        choice.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(400)
        val held = choice.fetchSemanticsNode().boundsInRoot.width
        assertTrue("A held choice should borrow width: $resting / $held", held > resting)
        compose.runOnIdle { assertEquals(65, compose.activity.images.state.options.quality) }
        choice.performTouchInput { cancel() }
        compose.mainClock.advanceTimeBy(600)
        assertEquals(resting, choice.fetchSemanticsNode().boundsInRoot.width, 1f)
        compose.runOnIdle { assertEquals(65, compose.activity.images.state.options.quality) }
        choice.performClick()
        compose.runOnIdle { assertEquals(50, compose.activity.images.state.options.quality) }
    }
```

</details>

<!-- /test -->

<!-- test: PhotoComposeTest.detailsKeepFiveFieldsAndASeparateCardWhileLoadingAndAfterSelection -->
<a id="test-2b79080893f1d59f"></a>
### `PhotoComposeTest.detailsKeepFiveFieldsAndASeparateCardWhileLoadingAndAfterSelection`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): createAndroidComposeRule<ImageActivity> su Robolectric; nodi semantici e gesti touch veri nel renderer host; JPEG sintetici ImageWorkspaceTest.fixture e dialog shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `addPhotos(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Dettagli mostrano loading poi5 campi e restano aperti selezionando; cache evita reading/previewLoading tornando su0.

**Limiti:** Non dispositivo Android. Non prova codec/pixel né jank; mainClock virtuale. waitUntil5s e testi/tag esatti; foto selezionate iniettando state anziché picker.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun detailsKeepFiveFieldsAndASeparateCardWhileLoadingAndAfterSelection() {
        addPhotos(2)
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-details").performClick()
        compose.onNodeWithTag("photo-details-panel").assertExists()
        compose.onNodeWithTag("photo-details-loading").assertExists()
        compose.runOnIdle { compose.activity.images.compose.onVisible(true) }
        compose.waitUntil(5_000) { !compose.activity.images.compose.state.previewLoading }
        compose.onNodeWithText("Dimensioni").assertExists()
        compose.onNodeWithText("Formato", substring = false).assertExists()
        compose.onNodeWithText("Dimensione file").assertExists()
        compose.onNodeWithText("Fotocamera").assertExists()
        compose.onNodeWithText("Cartella", substring = false).assertExists()
        compose.onNodeWithTag("photo-details-loading").assertDoesNotExist()
        compose.waitUntil(5_000) { !compose.activity.images.state.reading }
        compose.onNodeWithTag("photo-1").performClick()
        compose.onNodeWithTag("photo-details-panel").assertExists()
        compose.waitUntil(5_000) { !compose.activity.images.compose.state.previewLoading && !compose.activity.images.state.reading }
        compose.onNodeWithTag("photo-0").performClick()
        compose.runOnIdle {
            assertFalse("Cached details should appear immediately", compose.activity.images.compose.state.previewLoading)
            assertFalse("Cached inventory must not enter a blank reading state", compose.activity.images.state.reading)
        }
    }
```

</details>

<!-- /test -->

<!-- test: PhotoComposeTest.multiSelectionDetailsListNamesAndDoNotShowThePreviousPhotoMetadata -->
<a id="test-db7acf6363e5ad44"></a>
### `PhotoComposeTest.multiSelectionDetailsListNamesAndDoNotShowThePreviousPhotoMetadata`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): createAndroidComposeRule<ImageActivity> su Robolectric; nodi semantici e gesti touch veri nel renderer host; JPEG sintetici ImageWorkspaceTest.fixture e dialog shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `addPhotos(…)`, `longClick(…)`, `listOf(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Long press2+click0 mostra2 nomi ordinati senza fotocamera della precedente selezione.

**Limiti:** Non dispositivo Android. Non prova codec/pixel né jank; mainClock virtuale. waitUntil5s e testi/tag esatti; foto selezionate iniettando state anziché picker.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun multiSelectionDetailsListNamesAndDoNotShowThePreviousPhotoMetadata() {
        addPhotos(3)
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-2").performTouchInput { longClick() }
        compose.onNodeWithTag("photo-0").performClick()
        compose.onNodeWithTag("photo-details").performClick()
        compose.onNodeWithTag("photo-details-panel").assertExists()
        compose.onNodeWithText("File selezionati · 2").assertExists()
        compose.onNodeWithText(files[0].name).assertExists()
        compose.onNodeWithText(files[2].name).assertExists()
        compose.onNodeWithText("Fotocamera").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf(files[0].name, files[2].name).sortedWith(String.CASE_INSENSITIVE_ORDER),
                compose.activity.images.compose.state.selectedNames)
        }
    }
```

</details>

<!-- /test -->

<!-- test: PhotoComposeTest.longPressAndSwitchApplyExceptionsToExactlyTheSelectedFiles -->
<a id="test-37639ad17ed3bd8d"></a>
### `PhotoComposeTest.longPressAndSwitchApplyExceptionsToExactlyTheSelectedFiles`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): createAndroidComposeRule<ImageActivity> su Robolectric; nodi semantici e gesti touch veri nel renderer host; JPEG sintetici ImageWorkspaceTest.fixture e dialog shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.selectionIndices`, `images.state.destinations.plan.exceptionCount`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Gesti reali selezionano0,2 e switch eccezione applica solo a questi due.

**Limiti:** Non dispositivo Android. Non prova codec/pixel né jank; mainClock virtuale. waitUntil5s e testi/tag esatti; foto selezionate iniettando state anziché picker.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun longPressAndSwitchApplyExceptionsToExactlyTheSelectedFiles() {
        addPhotos(3)
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-0").performTouchInput { longClick() }
        compose.onNodeWithTag("photo-2").performClick()
        compose.onNodeWithTag("photo-scroll").performScrollToNode(hasTestTag("photo-exception"))
        compose.onNodeWithTag("photo-exception").assertIsDisplayed().assertIsOff().performClick()
        compose.onNodeWithTag("photo-exception").assertIsOn()
        compose.runOnIdle {
            val images = compose.activity.images
            assertEquals(listOf(0, 2), images.selectionIndices())
            assertEquals(2, images.state.destinations.plan.exceptionCount())
            assertFalse(images.state.destinations.plan.files[1].exception)
        }
        compose.onNodeWithTag("photo-back").assertIsDisplayed()
    }
```

</details>

<!-- /test -->

<!-- test: PhotoComposeTest.clearUsesExistingConfirmationAndNeverDeletesOriginals -->
<a id="test-fb6f0634b77c7761"></a>
### `PhotoComposeTest.clearUsesExistingConfirmationAndNeverDeletesOriginals`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): createAndroidComposeRule<ImageActivity> su Robolectric; nodi semantici e gesti touch veri nel renderer host; JPEG sintetici ImageWorkspaceTest.fixture e dialog shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `addPhotos(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Clear richiede sì e mantiene esistenza file originali; non confronta tutti i byte.

**Limiti:** Non dispositivo Android. Non prova codec/pixel né jank; mainClock virtuale. waitUntil5s e testi/tag esatti; foto selezionate iniettando state anziché picker.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun clearUsesExistingConfirmationAndNeverDeletesOriginals() {
        addPhotos(2)
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-clear").performClick()
        compose.runOnIdle {
            assertEquals(2, compose.activity.images.state.inputs.size)
            ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        }
        compose.onNodeWithTag("photo-clear").performClick()
        compose.runOnIdle {
            ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            assertTrue(compose.activity.images.state.inputs.isEmpty())
            assertTrue(files.all { it.exists() })
        }
    }
```

</details>

<!-- /test -->

<!-- test: PhotoComposeTest.scrollingToTheEndKeepsPermissionsAboveFloatingButtons -->
<a id="test-71c93ba1b6d8742e"></a>
### `PhotoComposeTest.scrollingToTheEndKeepsPermissionsAboveFloatingButtons`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): createAndroidComposeRule<ImageActivity> su Robolectric; nodi semantici e gesti touch veri nel renderer host; JPEG sintetici ImageWorkspaceTest.fixture e dialog shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoComposeTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `addPhotos(…)`, `it(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Scroll semantico a fine pagina lascia riga permessi sopra bottone Back: controllo geometrico, non prestazioni.

**Limiti:** Non dispositivo Android. Non prova codec/pixel né jank; mainClock virtuale. waitUntil5s e testi/tag esatti; foto selezionate iniettando state anziché picker.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun scrollingToTheEndKeepsPermissionsAboveFloatingButtons() {
        addPhotos(1)
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-permissions").assertDoesNotExist()
        compose.waitUntil(5_000) { compose.activity.images.screen.originRows.childCount > 1 }
        compose.onNodeWithTag("photo-scroll").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) {
            it(0f, 100_000f)
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val permissions = compose.onNodeWithTag("photo-permissions").fetchSemanticsNode().boundsInRoot
        val back = compose.onNodeWithTag("photo-back").fetchSemanticsNode().boundsInRoot
        assertTrue("Last row is covered: $permissions / $back", permissions.bottom <= back.top)
    }
```

</details>

<!-- /test -->


<a id="class-savingsboundarytest"></a>
## SavingsBoundaryTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/SavingsBoundaryTest.java) · 3 metodi · area originaria: Componenti condivisi.

**Contesto applicativo della classe:** SavingsPolicy.decide/NotSaved; CompressionSummary.render.

**Riferimenti produttivi:** [SavingsPolicy](../../app/src/main/java/it/edo/codecprobe/SavingsPolicy.java), [CompressionSummary](../../app/src/main/java/it/edo/codecprobe/CompressionSummary.java).

**Preparazione, file e simulazioni condivisi:** Long.MIN_VALUE, zero, -1, Long.MAX_VALUE ed Item in memoria.

**Confini comuni dell’audit:** NotSaved verifica contenitore e messaggio, non classificazione delle eccezioni prodotte dal motore.

**Metodi:** [unknownAndExtremeSizesCannotBecomeFalseSavings](#test-ace95659f0565ff8) · [notSavedReasonsKeepTheirCategoryAndOriginalMeasurements](#test-8162df7f07c530f4) · [overflowingTotalsAreUnavailableRatherThanNegativeOrInvented](#test-90eb0c3bbeb59c71)

<!-- test: SavingsBoundaryTest.unknownAndExtremeSizesCannotBecomeFalseSavings -->
<a id="test-ace95659f0565ff8"></a>
### `SavingsBoundaryTest.unknownAndExtremeSizesCannotBecomeFalseSavings`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Long.MIN_VALUE, zero, -1, Long.MAX_VALUE ed Item in memoria.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SavingsBoundaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `SavingsPolicy.decide`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Dimensioni non positive restano ignote e i massimi non diventano falso risparmio.

**Limiti:** NotSaved verifica contenitore e messaggio, non classificazione delle eccezioni prodotte dal motore.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void unknownAndExtremeSizesCannotBecomeFalseSavings() {
        for(long unknown:new long[]{Long.MIN_VALUE,-1,0}) {
            assertEquals(SavingsPolicy.Decision.UNKNOWN_SIZE,SavingsPolicy.decide(true,unknown,1));
            assertEquals(SavingsPolicy.Decision.UNKNOWN_SIZE,SavingsPolicy.decide(true,1,unknown));
        }
        assertEquals(SavingsPolicy.Decision.NO_SAVING,SavingsPolicy.decide(true,Long.MAX_VALUE,Long.MAX_VALUE));
        assertEquals(SavingsPolicy.Decision.NO_SAVING,SavingsPolicy.decide(true,1,Long.MAX_VALUE));
    }
```

</details>

<!-- /test -->

<!-- test: SavingsBoundaryTest.notSavedReasonsKeepTheirCategoryAndOriginalMeasurements -->
<a id="test-8162df7f07c530f4"></a>
### `SavingsBoundaryTest.notSavedReasonsKeepTheirCategoryAndOriginalMeasurements`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Long.MIN_VALUE, zero, -1, Long.MAX_VALUE ed Item in memoria.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SavingsBoundaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `SavingsPolicy.NotSaved`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Eccezione NotSaved conserva categoria e misure, con messaggio non vuoto.

**Limiti:** NotSaved verifica contenitore e messaggio, non classificazione delle eccezioni prodotte dal motore.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void notSavedReasonsKeepTheirCategoryAndOriginalMeasurements() {
        for(SavingsPolicy.Decision reason:List.of(SavingsPolicy.Decision.NO_SAVING,SavingsPolicy.Decision.UNKNOWN_SIZE)) {
            SavingsPolicy.NotSaved error=new SavingsPolicy.NotSaved(reason,-1,Long.MAX_VALUE);
            assertSame(reason,error.decision);assertEquals(-1,error.original);assertEquals(Long.MAX_VALUE,error.converted);
            assertFalse(error.getMessage().isBlank());
        }
    }
```

</details>

<!-- /test -->

<!-- test: SavingsBoundaryTest.overflowingTotalsAreUnavailableRatherThanNegativeOrInvented -->
<a id="test-90eb0c3bbeb59c71"></a>
### `SavingsBoundaryTest.overflowingTotalsAreUnavailableRatherThanNegativeOrInvented`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Long.MIN_VALUE, zero, -1, Long.MAX_VALUE ed Item in memoria.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SavingsBoundaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompressionSummary.render`, `CompressionSummary.Item`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Somma in overflow produce totale indisponibile; lista vuota rende stringa vuota.

**Limiti:** NotSaved verifica contenitore e messaggio, non classificazione delle eccezioni prodotte dal motore.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void overflowingTotalsAreUnavailableRatherThanNegativeOrInvented() {
        String report=CompressionSummary.render(List.of(
            new CompressionSummary.Item(1,"one",CompressionSummary.Status.COMPLETE,Long.MAX_VALUE,1),
            new CompressionSummary.Item(2,"two",CompressionSummary.Status.COMPLETE,1,1)));
        String total=report.substring(report.indexOf("TOTALE ·"));
        assertTrue(total.contains("Confronto dimensioni non disponibile"));assertFalse(total.contains("Spazio risparmiato"));
        assertEquals("",CompressionSummary.render(List.of()));
    }
```

</details>

<!-- /test -->


<a id="class-savingspolicytest"></a>
## SavingsPolicyTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/SavingsPolicyTest.java) · 2 metodi · area originaria: Componenti condivisi.

**Contesto applicativo della classe:** SavingsPolicy.decide; CompressionSummary.render.

**Riferimenti produttivi:** [SavingsPolicy](../../app/src/main/java/it/edo/codecprobe/SavingsPolicy.java), [CompressionSummary](../../app/src/main/java/it/edo/codecprobe/CompressionSummary.java).

**Preparazione, file e simulazioni condivisi:** Dimensioni sintetiche long; esiti precostruiti, non output di conversione.

**Confini comuni dell’audit:** Non prova che converter/servizi applichino questa decisione o che una copia originale venga pubblicata.

**Metodi:** [onlyStrictlySmallerKnownFilesPassTheEnabledGate](#test-36aca5a413e23d7c) · [unsavedCandidatesAreExcludedFromActualSavingsTotals](#test-82a06846a0a29bf0)

<!-- test: SavingsPolicyTest.onlyStrictlySmallerKnownFilesPassTheEnabledGate -->
<a id="test-36aca5a413e23d7c"></a>
### `SavingsPolicyTest.onlyStrictlySmallerKnownFilesPassTheEnabledGate`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Dimensioni sintetiche long; esiti precostruiti, non output di conversione.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SavingsPolicyTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `SavingsPolicy.decide`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Salva soltanto riduzione stretta se attivo; uguale/più grande non salvato, ignoto distinto e filtro disattivato bypassato.

**Limiti:** Non prova che converter/servizi applichino questa decisione o che una copia originale venga pubblicata.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void onlyStrictlySmallerKnownFilesPassTheEnabledGate(){
        assertEquals(SavingsPolicy.Decision.SAVE,SavingsPolicy.decide(true,100,99));
        assertEquals(SavingsPolicy.Decision.NO_SAVING,SavingsPolicy.decide(true,100,100));
        assertEquals(SavingsPolicy.Decision.NO_SAVING,SavingsPolicy.decide(true,100,101));
        assertEquals(SavingsPolicy.Decision.UNKNOWN_SIZE,SavingsPolicy.decide(true,-1,99));
        assertEquals(SavingsPolicy.Decision.UNKNOWN_SIZE,SavingsPolicy.decide(true,100,0));
        assertEquals(SavingsPolicy.Decision.SAVE,SavingsPolicy.decide(false,-1,200));
        assertEquals(SavingsPolicy.Decision.SAVE,SavingsPolicy.decide(true,Long.MAX_VALUE,Long.MAX_VALUE-1));
    }
```

</details>

<!-- /test -->

<!-- test: SavingsPolicyTest.unsavedCandidatesAreExcludedFromActualSavingsTotals -->
<a id="test-82a06846a0a29bf0"></a>
### `SavingsPolicyTest.unsavedCandidatesAreExcludedFromActualSavingsTotals`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Dimensioni sintetiche long; esiti precostruiti, non output di conversione.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SavingsPolicyTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompressionSummary.Item`, `CompressionSummary.render`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Totali includono solo il file COMPLETE 10→5 MB; esclude risultati NO_SAVING e SIZE_UNKNOWN.

**Limiti:** Non prova che converter/servizi applichino questa decisione o che una copia originale venga pubblicata.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void unsavedCandidatesAreExcludedFromActualSavingsTotals(){
        List<CompressionSummary.Item> items=List.of(
            new CompressionSummary.Item(1,"saved",CompressionSummary.Status.COMPLETE,10000000,5000000),
            new CompressionSummary.Item(2,"larger",CompressionSummary.Status.NO_SAVING,10000000,11000000),
            new CompressionSummary.Item(3,"unknown",CompressionSummary.Status.SIZE_UNKNOWN,-1,9000000));
        String report=CompressionSummary.render(items);
        assertTrue(report.contains("Nessun risparmio · risultato non salvato"));
        assertTrue(report.contains("Confronto dimensioni non disponibile"));
        String total=report.substring(report.indexOf("TOTALE ·"));
        assertTrue(total.contains("10,00 MB → 5,00 MB"));assertFalse(total.contains("11,00"));assertFalse(total.contains("9,00"));
    }
```

</details>

<!-- /test -->


<a id="class-workspacenavigationtest"></a>
## WorkspaceNavigationTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/WorkspaceNavigationTest.java) · 3 metodi · area originaria: Interfaccia grafica.

**Contesto applicativo della classe:** AppNavigation; MainActivity/AudioActivity transizioni; workspace anim resources.

**Riferimenti produttivi:** [AppNavigation](../../app/src/main/java/it/edo/codecprobe/AppNavigation.java), [MainActivity](../../app/src/main/java/it/edo/codecprobe/MainActivity.java), [AudioActivity](../../app/src/main/java/it/edo/codecprobe/AudioActivity.java).

**Preparazione, file e simulazioni condivisi:** Activity/View/ScrollView Robolectric, Intent shadow e AnimationUtils.

**Confini comuni dell’audit:** Misura posizione statica dopo scrollTo e ID/durata animazione, non jank o effettivo rendering a60/120Hz.

**Metodi:** [navigationIsTheFirstPageChildAndScrollsAwayWithContent](#test-a6b033ea0a026391) · [workspaceSwitchUsesFadeAndSelectedTabDoesNotLaunchAnotherScreen](#test-750618878ae882fd) · [backFromAudioAlsoUsesTheWorkspaceFade](#test-5ef937fdb9b3b585)

<!-- test: WorkspaceNavigationTest.navigationIsTheFirstPageChildAndScrollsAwayWithContent -->
<a id="test-a6b033ea0a026391"></a>
### `WorkspaceNavigationTest.navigationIsTheFirstPageChildAndScrollsAwayWithContent`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/View/ScrollView Robolectric, Intent shadow e AnimationUtils.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/WorkspaceNavigationTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.screen.page.getChildAt`, `audio.get`, `audio.screen.page.getChildAt`, `home.screen.navigation.video.isSelected`, `audio.screen.navigation.audio.isSelected`, `audio.screen.sections.focusCard`, `audio.screen.navigation.getLocationOnScreen`, `audio.screen.navigation.getHeight`, `audio.pause`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Tab primo figlio scorre fuori viewport su Audio con scrollTo500.

**Limiti:** Misura posizione statica dopo scrollTo e ID/durata animazione, non jank o effettivo rendering a60/120Hz.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void navigationIsTheFirstPageChildAndScrollsAwayWithContent(){
        var video=Robolectric.buildActivity(MainActivity.class).setup();var audio=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            assertSame(video.get().home.screen.navigation,video.get().home.screen.page.getChildAt(0));assertSame(audio.get().audio.screen.navigation,audio.get().audio.screen.page.getChildAt(0));
            assertTrue(video.get().home.screen.navigation.video.isSelected());assertTrue(audio.get().audio.screen.navigation.audio.isSelected());
            ViewGroup root=audio.get().findViewById(android.R.id.content);assertTrue(root.getChildAt(0) instanceof ScrollView);ScrollView scroll=(ScrollView)root.getChildAt(0);
            audio.get().audio.screen.sections.focusCard(AudioScreen.CONVERSION);
            scroll.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));scroll.layout(0,0,720,900);scroll.scrollTo(0,500);
            int[] tab=new int[2],viewport=new int[2];audio.get().audio.screen.navigation.getLocationOnScreen(tab);scroll.getLocationOnScreen(viewport);assertTrue("Tabs remained fixed at the top",tab[1]+audio.get().audio.screen.navigation.getHeight()<=viewport[1]);
        }finally{audio.pause().stop().destroy();video.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: WorkspaceNavigationTest.workspaceSwitchUsesFadeAndSelectedTabDoesNotLaunchAnotherScreen -->
<a id="test-750618878ae882fd"></a>
### `WorkspaceNavigationTest.workspaceSwitchUsesFadeAndSelectedTabDoesNotLaunchAnotherScreen`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/View/ScrollView Robolectric, Intent shadow e AnimationUtils.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/WorkspaceNavigationTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.screen.navigation.video.performClick`, `home.screen.navigation.audio.performClick`, `AudioActivity.class.getName`, `audio.get`, `audio.screen.navigation.audio.performClick`, `audio.screen.navigation.video.performClick`, `MainActivity.class.getName`, `audio.pause`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Tab già attiva non rilancia; passaggio usa Intent e resource fade, durata uscita140ms.

**Limiti:** Misura posizione statica dopo scrollTo e ID/durata animazione, non jank o effettivo rendering a60/120Hz.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void workspaceSwitchUsesFadeAndSelectedTabDoesNotLaunchAnotherScreen(){
        var video=Robolectric.buildActivity(MainActivity.class).setup();var audio=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            video.get().home.screen.navigation.video.performClick();assertNull(Shadows.shadowOf(video.get()).getNextStartedActivity());
            video.get().home.screen.navigation.audio.performClick();Intent next=Shadows.shadowOf(video.get()).getNextStartedActivity();assertEquals(AudioActivity.class.getName(),next.getComponent().getClassName());
            assertEquals(R.anim.workspace_fade_in,Shadows.shadowOf(video.get()).getPendingTransitionEnterAnimationResourceId());assertEquals(R.anim.workspace_fade_out,Shadows.shadowOf(video.get()).getPendingTransitionExitAnimationResourceId());
            audio.get().audio.screen.navigation.audio.performClick();assertNull(Shadows.shadowOf(audio.get()).getNextStartedActivity());audio.get().audio.screen.navigation.video.performClick();assertTrue(audio.get().isFinishing());assertEquals(MainActivity.class.getName(),Shadows.shadowOf(audio.get()).getNextStartedActivity().getComponent().getClassName());
            assertEquals(R.anim.workspace_fade_in,Shadows.shadowOf(audio.get()).getPendingTransitionEnterAnimationResourceId());
            assertTrue(AnimationUtils.loadAnimation(video.get(),R.anim.workspace_fade_in) instanceof AlphaAnimation);assertEquals(140,AnimationUtils.loadAnimation(video.get(),R.anim.workspace_fade_out).getDuration());
        }finally{audio.pause().stop().destroy();video.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: WorkspaceNavigationTest.backFromAudioAlsoUsesTheWorkspaceFade -->
<a id="test-5ef937fdb9b3b585"></a>
### `WorkspaceNavigationTest.backFromAudioAlsoUsesTheWorkspaceFade`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Activity/View/ScrollView Robolectric, Intent shadow e AnimationUtils.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/WorkspaceNavigationTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `audio.get`, `audio.pause`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Back da Audio termina usando resource fade uscita.

**Limiti:** Misura posizione statica dopo scrollTo e ID/durata animazione, non jank o effettivo rendering a60/120Hz.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void backFromAudioAlsoUsesTheWorkspaceFade(){var audio=Robolectric.buildActivity(AudioActivity.class).setup();try{audio.get().onBackPressed();assertTrue(audio.get().isFinishing());assertEquals(R.anim.workspace_fade_out,Shadows.shadowOf(audio.get()).getPendingTransitionExitAnimationResourceId());}finally{audio.pause().stop().destroy();}}
```

</details>

<!-- /test -->
