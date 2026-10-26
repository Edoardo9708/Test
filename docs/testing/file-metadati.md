# File, cartelle, metadati e Analisi

[Guida](README.md) · [Indice completo](index.md) · [Revisioni](review-needed.md) · [Esecuzioni](esecuzioni.md)

Descrizioni statiche trasferite dall’audit: non attestano nuove esecuzioni. Le fixture condivise sono descritte a livello di classe; dati specifici, ordine delle operazioni e assert sono nel corpo integrale di ogni metodo. Gli helper richiamati e gli eventuali `@Before`/`@After` sono consultabili nel sorgente collegato. Lo script di aggiornamento non riscrive questa pagina.

## Classi

- [AnalysisDetailReaderTest](#class-analysisdetailreadertest)
- [AnalysisLabelsTest](#class-analysislabelstest)
- [AnalysisParserTest](#class-analysisparsertest)
- [AnalysisWorkspaceTest](#class-analysisworkspacetest)
- [AudioDestinationFlowTest](#class-audiodestinationflowtest)
- [AudioMetadataFlowTest](#class-audiometadataflowtest)
- [CreationDateTest](#class-creationdatetest)
- [ExifDictionaryTest](#class-exifdictionarytest)
- [FolderPickerActivityTest](#class-folderpickeractivitytest)
- [ImageDestinationFlowTest](#class-imagedestinationflowtest)
- [ImageMetadataCheckTest](#class-imagemetadatachecktest)
- [ImageOutputFailureTest](#class-imageoutputfailuretest)
- [ImageOutputTest](#class-imageoutputtest)
- [ImageProgressJournalTest](#class-imageprogressjournaltest)
- [InputCreationDateTest](#class-inputcreationdatetest)
- [MediaDestinationsTest](#class-mediadestinationstest)
- [Mp4MetadataDiagnosticsTest](#class-mp4metadatadiagnosticstest)
- [Mp4MetadataInventoryTest](#class-mp4metadatainventorytest)
- [OutputCollisionTest](#class-outputcollisiontest)
- [OutputNamingSafetyTest](#class-outputnamingsafetytest)
- [OutputPlanTest](#class-outputplantest)
- [PhotoBasicDetailsTest](#class-photobasicdetailstest)
- [PhotoReferenceTest](#class-photoreferencetest)
- [SamsungSefEditorTest](#class-samsungsefeditortest)
- [SamsungSefMotionTest](#class-samsungsefmotiontest)
- [SamsungSefReportTest](#class-samsungsefreporttest)
- [SelectedMetadataReportTest](#class-selectedmetadatareporttest)
- [VideoDateAuditTest](#class-videodateaudittest)

<a id="class-analysisdetailreadertest"></a>
## AnalysisDetailReaderTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java) · 8 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** AnalysisReader; ExifAnalysisParser.read; AnalysisDetailReader.read.

**Riferimenti produttivi:** [AnalysisReader](../../app/src/main/java/it/edo/codecprobe/AnalysisReader.java), [ExifAnalysisParser](../../app/src/main/java/it/edo/codecprobe/ExifAnalysisParser.java), [AnalysisDetailReader](../../app/src/main/java/it/edo/codecprobe/AnalysisDetailReader.java).

**Preparazione, file e simulazioni condivisi:** Byte TIFF/box/JPEG sintetici su veri file temporanei, RandomAccessFile e FileChannel.

**Confini comuni dell’audit:** Parsing e letture reali, ma niente immagine valida decodificata, GUI o file di vendor completi; testi descrittivi verificati per substring.

**Metodi:** [littleAndBigEndianAreExplainedUsingActualBytes](#test-8fa97c6bb203435b) · [everyOrientationHasAConcreteMeaningAndUnknownValuesAreNotGuessed](#test-5af129f14f1076b9) · [externalAsciiFollowsTheOffsetAndPreservesItsTagIdentity](#test-74826c2d6e2d60b8) · [changedDirectoryHeaderIsRejectedInsteadOfShowingCachedClaims](#test-45116630d8262fa6) · [boxAnatomySeparatesAsciiIdAndBigEndianLength](#test-52143a673197cd0d) · [jpegLengthExcludesMarkerAndIncludesLengthField](#test-1559175dde184a50) · [unknownTiffTypeDoesNotInventAnExternalDataSize](#test-bd9ecd20b6c9e92f) · [truncatedSelectedRangeIsRejected](#test-76829604b86854e1)

<!-- test: AnalysisDetailReaderTest.littleAndBigEndianAreExplainedUsingActualBytes -->
<a id="test-8fa97c6bb203435b"></a>
### `AnalysisDetailReaderTest.littleAndBigEndianAreExplainedUsingActualBytes`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Byte TIFF/box/JPEG sintetici su veri file temporanei, RandomAccessFile e FileChannel.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisDetailReader.read`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Byte reali little/big endian: tag, SHORT, count, valore orientamento e sua alternativa selezionata.

**Limiti:** Parsing e letture reali, ma niente immagine valida decodificata, GUI o file di vendor completi; testi descrittivi verificati per substring.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void littleAndBigEndianAreExplainedUsingActualBytes()throws Exception{
        for(boolean little:new boolean[]{true,false})with(orientation(little,6),r->{
            AnalysisDetail d=AnalysisDetailReader.read(r,field(r,false));
            assertEquals(little?"12 01":"01 12",d.parts.get(0).hex);assertEquals(2,d.parts.get(0).size);
            assertTrue(d.parts.get(1).meaning.contains("SHORT"));assertTrue(d.parts.get(2).meaning.contains("1 × 2 = 2"));
            assertEquals(2,d.parts.get(3).size);assertEquals(2,d.parts.get(4).size);
            assertTrue(d.result.contains("6 → rotazione di 90° in senso orario"));assertTrue(d.preview().startsWith("6 →"));assertTrue(d.alternatives.contains("6 = rotazione di 90° in senso orario  [PRESENTE NEL FILE]"));
        });
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisDetailReaderTest.everyOrientationHasAConcreteMeaningAndUnknownValuesAreNotGuessed -->
<a id="test-5af129f14f1076b9"></a>
### `AnalysisDetailReaderTest.everyOrientationHasAConcreteMeaningAndUnknownValuesAreNotGuessed`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Byte TIFF/box/JPEG sintetici su veri file temporanei, RandomAccessFile e FileChannel.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisDetailReader.read`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Orientamenti 1–8 noti; 99 esplicitamente ignoto.

**Limiti:** Parsing e letture reali, ma niente immagine valida decodificata, GUI o file di vendor completi; testi descrittivi verificati per substring.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void everyOrientationHasAConcreteMeaningAndUnknownValuesAreNotGuessed()throws Exception{
        for(int value=1;value<=8;value++){final int v=value;with(orientation(true,value),r->{AnalysisDetail d=AnalysisDetailReader.read(r,field(r,false));assertTrue(d.alternatives.contains(v+" = "));assertFalse(d.result.contains("valore non riconosciuto"));});}
        with(orientation(true,99),r->assertTrue(AnalysisDetailReader.read(r,field(r,false)).result.contains("valore non riconosciuto")));
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisDetailReaderTest.externalAsciiFollowsTheOffsetAndPreservesItsTagIdentity -->
<a id="test-74826c2d6e2d60b8"></a>
### `AnalysisDetailReaderTest.externalAsciiFollowsTheOffsetAndPreservesItsTagIdentity`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Byte TIFF/box/JPEG sintetici su veri file temporanei, RandomAccessFile e FileChannel.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisDetailReader.read`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** ASCII esterno SAMSUNG segue offset 26 e mantiene identità di tag/valore.

**Limiti:** Parsing e letture reali, ma niente immagine valida decodificata, GUI o file di vendor completi; testi descrittivi verificati per substring.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void externalAsciiFollowsTheOffsetAndPreservesItsTagIdentity()throws Exception{
        ByteBuffer b=ByteBuffer.allocate(34).order(ByteOrder.LITTLE_ENDIAN);b.put((byte)'I').put((byte)'I').putShort((short)42).putInt(8).putShort((short)1);
        b.putShort((short)0x10f).putShort((short)2).putInt(8).putInt(26).putInt(0).put(new byte[]{'S','A','M','S','U','N','G',0});
        with(b.array(),r->{AnalysisNode entry=field(r,false),value=field(r,true);assertEquals(entry.field.entry,value.field.entry);assertEquals(entry.field.value,value.field.value);
            AnalysisDetail d=AnalysisDetailReader.read(r,entry);assertTrue(d.parts.get(3).meaning.contains("0 + offset 26 = posizione assoluta 26"));assertEquals(26,d.parts.get(4).offset);assertEquals(8,d.parts.get(4).size);assertTrue(d.result.contains("SAMSUNG\\0"));
            assertTrue(AnalysisDetailReader.read(r,value).result.contains("SAMSUNG\\0"));
        });
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisDetailReaderTest.changedDirectoryHeaderIsRejectedInsteadOfShowingCachedClaims -->
<a id="test-45116630d8262fa6"></a>
### `AnalysisDetailReaderTest.changedDirectoryHeaderIsRejectedInsteadOfShowingCachedClaims`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Byte TIFF/box/JPEG sintetici su veri file temporanei, RandomAccessFile e FileChannel.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisDetailReader.read`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Dopo parse, modifica del tag su disco: la rilettura rifiuta la descrizione obsoleta.

**Limiti:** Parsing e letture reali, ma niente immagine valida decodificata, GUI o file di vendor completi; testi descrittivi verificati per substring.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void changedDirectoryHeaderIsRejectedInsteadOfShowingCachedClaims()throws Exception{
        Path path=Files.createTempFile("changed-field-",".tiff");try{Files.write(path,orientation(true,6));try(RandomAccessFile file=new RandomAccessFile(path.toFile(),"rw")){
            AnalysisReader r=new AnalysisReader(file.getChannel());AnalysisNode n=field(r,false);file.seek(10);file.write(new byte[]{0x10,1});
            try{AnalysisDetailReader.read(r,n);fail("Changed tag accepted");}catch(IOException expected){assertTrue(expected.getMessage().contains("cambiata"));}
        }}finally{Files.delete(path);}
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisDetailReaderTest.boxAnatomySeparatesAsciiIdAndBigEndianLength -->
<a id="test-52143a673197cd0d"></a>
### `AnalysisDetailReaderTest.boxAnatomySeparatesAsciiIdAndBigEndianLength`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Byte TIFF/box/JPEG sintetici su veri file temporanei, RandomAccessFile e FileChannel.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisDetailReader.read`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Box free: lunghezza big endian, ID ASCII e anteprima limitata 64 su 600 byte.

**Limiti:** Parsing e letture reali, ma niente immagine valida decodificata, GUI o file di vendor completi; testi descrittivi verificati per substring.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void boxAnatomySeparatesAsciiIdAndBigEndianLength()throws Exception{
        byte[] bytes=FileAnalysisChecks.box("free",new byte[600]);with(bytes,r->{
            AnalysisNode n=new AnalysisNode("free","",0,bytes.length,"",8);AnalysisDetail d=AnalysisDetailReader.read(r,n);
            assertEquals("00 00 02 60",d.parts.get(0).hex);assertEquals("66 72 65 65",d.parts.get(1).hex);assertTrue(d.result.contains("8 di intestazione e 600 di contenuto"));assertTrue(d.parts.get(2).meaning.contains("Mostrati 64 di 600"));
        });
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisDetailReaderTest.jpegLengthExcludesMarkerAndIncludesLengthField -->
<a id="test-1559175dde184a50"></a>
### `AnalysisDetailReaderTest.jpegLengthExcludesMarkerAndIncludesLengthField`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Byte TIFF/box/JPEG sintetici su veri file temporanei, RandomAccessFile e FileChannel.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisDetailReader.read`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Lunghezza segmento JPEG esclude marker e include campo lunghezza.

**Limiti:** Parsing e letture reali, ma niente immagine valida decodificata, GUI o file di vendor completi; testi descrittivi verificati per substring.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void jpegLengthExcludesMarkerAndIncludesLengthField()throws Exception{
        byte[] segment={(byte)255,(byte)225,0,16,'E','x','i','f',0,0,'I','I',42,0,8,0,0,0};
        with(segment,r->{AnalysisDetail d=AnalysisDetailReader.read(r,new AnalysisNode("APP1 · EXIF","",0,18,"exif",10));
            assertEquals("FF E1",d.parts.get(0).hex);assertEquals("00 10",d.parts.get(1).hex);assertTrue(d.parts.get(1).meaning.contains("Include i 2 byte"));assertTrue(d.parts.get(2).meaning.contains("Exif\\0\\0"));
        });
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisDetailReaderTest.unknownTiffTypeDoesNotInventAnExternalDataSize -->
<a id="test-bd9ecd20b6c9e92f"></a>
### `AnalysisDetailReaderTest.unknownTiffTypeDoesNotInventAnExternalDataSize`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Byte TIFF/box/JPEG sintetici su veri file temporanei, RandomAccessFile e FileChannel.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisDetailReader.read`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Tipo TIFF 99 non inventa dimensione del valore esterno.

**Limiti:** Parsing e letture reali, ma niente immagine valida decodificata, GUI o file di vendor completi; testi descrittivi verificati per substring.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void unknownTiffTypeDoesNotInventAnExternalDataSize()throws Exception{
        byte[] bytes=orientation(true,6);ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putShort(12,(short)99);
        with(bytes,r->{AnalysisDetail d=AnalysisDetailReader.read(r,field(r,false));assertTrue(d.result.contains("Tipo non supportato"));assertEquals(4,d.parts.size());});
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisDetailReaderTest.truncatedSelectedRangeIsRejected -->
<a id="test-76829604b86854e1"></a>
### `AnalysisDetailReaderTest.truncatedSelectedRangeIsRejected`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Byte TIFF/box/JPEG sintetici su veri file temporanei, RandomAccessFile e FileChannel.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisDetailReaderTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisDetailReader.read`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Range selezionato oltre EOF solleva IOException.

**Limiti:** Parsing e letture reali, ma niente immagine valida decodificata, GUI o file di vendor completi; testi descrittivi verificati per substring.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void truncatedSelectedRangeIsRejected()throws Exception{
        with(new byte[4],r->{try{AnalysisDetailReader.read(r,new AnalysisNode("free","",0,12,"",8));fail("Truncated node accepted");}catch(IOException expected){}});
    }
```

</details>

<!-- /test -->


<a id="class-analysislabelstest"></a>
## AnalysisLabelsTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AnalysisLabelsTest.java) · 5 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** AnalysisLabels.of/facts; AnalysisParser.leaf.

**Riferimenti produttivi:** [AnalysisLabels](../../app/src/main/java/it/edo/codecprobe/AnalysisLabels.java), [AnalysisParser](../../app/src/main/java/it/edo/codecprobe/AnalysisParser.java).

**Preparazione, file e simulazioni condivisi:** AnalysisNode costruiti manualmente, comprese descrizioni TIFF testuali già formattate.

**Confini comuni dell’audit:** Non verifica che un parser generi quelle descrizioni né che i pixel o il file sottostante esistano. Accoppiamento a stringhe/etichette.

**Metodi:** [referencesAreNotDescribedAsImagePayloads](#test-58b948fdf3940663) · [technicalIdentifiersHaveReadableNamesWithoutInventingUnknownContent](#test-41e54c854dce4baf) · [exifFactsExplainTypeAndOrientationWithoutChangingRawValue](#test-0ca9cd7d27cebf51) · [malformedOrTextOrientationIsNotInterpretedAsANumber](#test-4c9903a8a36b85bc) · [exifTagAndExternalValueKeepTheirMeaning](#test-7f96af03ef7f6bac)

<!-- test: AnalysisLabelsTest.referencesAreNotDescribedAsImagePayloads -->
<a id="test-58b948fdf3940663"></a>
### `AnalysisLabelsTest.referencesAreNotDescribedAsImagePayloads`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): AnalysisNode costruiti manualmente, comprese descrizioni TIFF testuali già formattate.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisLabelsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `label(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** thmb è collegamento non pixel; meta contenitore e mdat dati.

**Limiti:** Non verifica che un parser generi quelle descrizioni né che i pixel o il file sottostante esistano. Accoppiamento a stringhe/etichette.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void referencesAreNotDescribedAsImagePayloads(){
        assertEquals("Indice / collegamento",label("thmb").kind);assertTrue(label("thmb").meaning.contains("non i pixel"));
        assertTrue(label("meta").meaning.contains("altrove"));assertEquals("Contenitore",label("meta").kind);assertEquals("Dati",label("mdat").kind);
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisLabelsTest.technicalIdentifiersHaveReadableNamesWithoutInventingUnknownContent -->
<a id="test-41e54c854dce4baf"></a>
### `AnalysisLabelsTest.technicalIdentifiersHaveReadableNamesWithoutInventingUnknownContent`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): AnalysisNode costruiti manualmente, comprese descrizioni TIFF testuali già formattate.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisLabelsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `label(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Etichette per ftyp/zzzz/scansione/EXIF senza inventare blocchi sconosciuti.

**Limiti:** Non verifica che un parser generi quelle descrizioni né che i pixel o il file sottostante esistano. Accoppiamento a stringhe/etichette.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void technicalIdentifiersHaveReadableNamesWithoutInventingUnknownContent(){
        assertEquals("Formato e compatibilità",label("ftyp").name);assertEquals("Blocco non riconosciuto",label("zzzz").name);
        assertEquals("Dati dell’immagine compressa",label("Scansione · immagine compressa").name);
        assertEquals("Contenitore",label("APP1 · EXIF").kind);
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisLabelsTest.exifFactsExplainTypeAndOrientationWithoutChangingRawValue -->
<a id="test-0ca9cd7d27cebf51"></a>
### `AnalysisLabelsTest.exifFactsExplainTypeAndOrientationWithoutChangingRawValue`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): AnalysisNode costruiti manualmente, comprese descrizioni TIFF testuali già formattate.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisLabelsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisParser.leaf`, `AnalysisLabels.facts`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Interpretazione orientamento 6 da descrizione SHORT senza cambiare il valore grezzo.

**Limiti:** Non verifica che un parser generi quelle descrizioni né che i pixel o il file sottostante esistano. Accoppiamento a stringhe/etichette.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void exifFactsExplainTypeAndOrientationWithoutChangingRawValue(){
        AnalysisNode n=AnalysisParser.leaf("IFD0 · Orientamento [0x0112]","Tipo TIFF 3 · 1 valori\nValore a offset 30 · 2 byte\nValore: 6",22,12);
        String facts=AnalysisLabels.facts(n);assertTrue(facts.contains("intero senza segno a 16 bit"));assertTrue(facts.contains("Valore: 6"));assertTrue(facts.contains("90° in senso orario"));
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisLabelsTest.malformedOrTextOrientationIsNotInterpretedAsANumber -->
<a id="test-4c9903a8a36b85bc"></a>
### `AnalysisLabelsTest.malformedOrTextOrientationIsNotInterpretedAsANumber`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): AnalysisNode costruiti manualmente, comprese descrizioni TIFF testuali già formattate.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisLabelsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisLabels.facts`, `AnalysisParser.leaf`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Non interpreta tipo TIFF errato, ASCII o orientamento fuori intervallo.

**Limiti:** Non verifica che un parser generi quelle descrizioni né che i pixel o il file sottostante esistano. Accoppiamento a stringhe/etichette.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void malformedOrTextOrientationIsNotInterpretedAsANumber(){
        for(String detail:new String[]{"Tipo TIFF 4 · 1 valori\nValore: 4294967295","Tipo TIFF 2 · 1 valori\nValore: 6","Tipo TIFF 3 · 1 valori\nValore: 99"}){
            assertFalse(AnalysisLabels.facts(AnalysisParser.leaf("IFD0 · Orientamento [0x0112]",detail,0,12)).contains("Interpretazione:"));
        }
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisLabelsTest.exifTagAndExternalValueKeepTheirMeaning -->
<a id="test-7f96af03ef7f6bac"></a>
### `AnalysisLabelsTest.exifTagAndExternalValueKeepTheirMeaning`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): AnalysisNode costruiti manualmente, comprese descrizioni TIFF testuali già formattate.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisLabelsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `label(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Nomi dei tag e dei valori esterni conservano il significato di directory.

**Limiti:** Non verifica che un parser generi quelle descrizioni né che i pixel o il file sottostante esistano. Accoppiamento a stringhe/etichette.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void exifTagAndExternalValueKeepTheirMeaning(){
        assertEquals("Orientamento della foto",label("IFD0 · Orientamento [0x0112]").name);
        assertEquals("Latitudine",label("GPS · Tag [0x0002]").name);
        assertEquals("Valore di Latitudine",label("Valore · GPS · Tag [0x0002]").name);
        assertEquals("Tag EXIF non tradotto",label("IFD0 · Tag [0xFFFF]").name);
    }
```

</details>

<!-- /test -->


<a id="class-analysisparsertest"></a>
## AnalysisParserTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AnalysisParserTest.java) · 1 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** FileAnalysisChecks.run; AnalysisParser.root/children/hex; AnalysisReader.bytes.

**Riferimenti produttivi:** [AnalysisParser](../../app/src/main/java/it/edo/codecprobe/AnalysisParser.java), [AnalysisReader](../../app/src/main/java/it/edo/codecprobe/AnalysisReader.java).

**Preparazione, file e simulazioni condivisi:** Helper reale con JPEG/HEIC/FLAC/MP3/TIFF sintetici, file sparso logico 5 GiB, IFD ciclico e interrupt.

**Confini comuni dell’audit:** Un solo caso aggrega molte asserzioni: individua la prima rottura, non attribuisce a ogni formato un risultato distinto. File sparso non equivale a leggere 5 GiB né a decodificare media.

**Metodi:** [fileStructuresAndBoundedReads](#test-936209107f4e502e)

<!-- test: AnalysisParserTest.fileStructuresAndBoundedReads -->
<a id="test-936209107f4e502e"></a>
### `AnalysisParserTest.fileStructuresAndBoundedReads`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Helper reale con JPEG/HEIC/FLAC/MP3/TIFF sintetici, file sparso logico 5 GiB, IFD ciclico e interrupt.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisParserTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Percorso tramite fixture/helper e componenti indicati sopra; nessun encoder.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Range contigui e completi, JPEG stuffing/restart, nested box, mdat opaco, seek 64 bit, formati troncati, cancel, EXIF/miniatura e ciclo IFD respinto.

**Limiti:** Un solo caso aggrega molte asserzioni: individua la prima rottura, non attribuisce a ogni formato un risultato distinto. File sparso non equivale a leggere 5 GiB né a decodificare media.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void fileStructuresAndBoundedReads()throws Exception{FileAnalysisChecks.run();}
```

</details>

<!-- /test -->


<a id="class-analysisworkspacetest"></a>
## AnalysisWorkspaceTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AnalysisWorkspaceTest.java) · 5 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** AnalysisActivity/Controller/Screen; AnalysisSources; AnalysisParser/DetailReader.

**Riferimenti produttivi:** [AnalysisActivity](../../app/src/main/java/it/edo/codecprobe/AnalysisActivity.java), [AnalysisSources](../../app/src/main/java/it/edo/codecprobe/AnalysisSources.java), [AnalysisParser](../../app/src/main/java/it/edo/codecprobe/AnalysisParser.java).

**Preparazione, file e simulazioni condivisi:** Robolectric, URI mancanti e piccoli veri file MP4/JPEG sintetici con TIFF orientamento; worker reale, polling 10 ms fino a 5 s.

**Confini comuni dell’audit:** Navigazione childAt e testi precisi fragile; nessun file multimediale decodificabile, vero provider SAF o kill del processo.

**Metodi:** [selectionListUsesReferencesWithoutOpeningMissingFiles](#test-62cb13456674b276) · [blockNavigationHexAndRecreationKeepSourceUnchanged](#test-5ef983c268963480) · [exifValueOpensItsExplanationAtTheTopOfAPopup](#test-2d5f67a8f5f7d135) · [missingSourceIsReadableErrorAndAllowsReturn](#test-2a3b3b824bcdb254) · [fourthTabPublishesCurrentVideoSelectionBeforeOpening](#test-006bbda75b920fe9)

<!-- test: AnalysisWorkspaceTest.selectionListUsesReferencesWithoutOpeningMissingFiles -->
<a id="test-62cb13456674b276"></a>
### `AnalysisWorkspaceTest.selectionListUsesReferencesWithoutOpeningMissingFiles`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric, URI mancanti e piccoli veri file MP4/JPEG sintetici con TIFF orientamento; worker reale, polling 10 ms fino a 5 s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `audio.inputs.add`, `audio.names.add`, `AudioDraftStore.save`, `images.inputs.add`, `images.names.add`, `ImageOptions.load`, `ImageDraftStore.save`, `AnalysisSources.video`, `AnalysisSources.list`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Combina riferimenti Audio/Foto/Video senza aprire URI inesistenti.

**Limiti:** Navigazione childAt e testi precisi fragile; nessun file multimediale decodificabile, vero provider SAF o kill del processo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void selectionListUsesReferencesWithoutOpeningMissingFiles(){
        var context=RuntimeEnvironment.getApplication();AudioState audio=new AudioState();audio.inputs.add("content://missing/audio");audio.names.add("Brano.flac");AudioDraftStore.save(context,audio);
        ImageState images=new ImageState();images.inputs.add("content://missing/photo");images.names.add("Foto.jpg");images.options=ImageOptions.load(context);ImageDraftStore.save(context,images);
        AnalysisSources.video(Arrays.asList("content://missing/video"),Arrays.asList("Filmato.mp4"));
        var controller=Robolectric.buildActivity(AnalysisActivity.class).setup();try{AnalysisScreen s=controller.get().analysis.screen;assertEquals(3,AnalysisSources.list(context).size());assertEquals(View.VISIBLE,s.files.getVisibility());assertEquals(View.GONE,s.map.getVisibility());assertEquals("",s.status.getText().toString());}finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisWorkspaceTest.blockNavigationHexAndRecreationKeepSourceUnchanged -->
<a id="test-5ef983c268963480"></a>
### `AnalysisWorkspaceTest.blockNavigationHexAndRecreationKeepSourceUnchanged`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric, URI mancanti e piccoli veri file MP4/JPEG sintetici con TIFF orientamento; worker reale, polling 10 ms fino a 5 s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisSources.video`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Naviga box e hex paginato di MP4 sintetico, ricrea Activity e verifica file sorgente byte-identico.

**Limiti:** Navigazione childAt e testi precisi fragile; nessun file multimediale decodificabile, vero provider SAF o kill del processo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void blockNavigationHexAndRecreationKeepSourceUnchanged()throws Exception{
        Path file=Files.createTempFile("analysis-ui-",".mp4");byte[] data=FileAnalysisChecks.concat(FileAnalysisChecks.box("ftyp",new byte[8]),FileAnalysisChecks.box("moov",FileAnalysisChecks.box("free",new byte[600])));Files.write(file,data);
        AnalysisSources.video(Arrays.asList(Uri.fromFile(file.toFile()).toString()),Arrays.asList("Video.mp4"));var controller=Robolectric.buildActivity(AnalysisActivity.class).setup();
        try{
            AnalysisScreen s=controller.get().analysis.screen;((Button)s.files.getChildAt(2)).performClick();await(s,"Sola lettura");assertEquals(2,s.blocks.getChildCount());assertTrue(((Button)s.blocks.getChildAt(0)).getText().toString().startsWith("Formato e compatibilità"));
            assertEquals(View.GONE,s.hexPanel.getVisibility());((Button)s.blocks.getChildAt(1)).performClick();await(s,"Sola lettura");assertEquals(1,s.blocks.getChildCount());assertTrue(s.frameTitle.getText().toString().contains("moov"));assertTrue(s.frameTitle.getText().toString().contains("Organizzazione del filmato"));assertNotSame(s.fileFrame,s.currentFrame.getParent());
            ((Button)s.blocks.getChildAt(0)).performClick();await(s,"Sola lettura");assertNotNull(s.popup);assertTrue(s.popup.isShowing());assertNotSame(s.map,s.details.getParent());assertTrue(s.anatomy.getChildCount()>0);s.hex.performClick();await(s,"Sola lettura");assertEquals(View.VISIBLE,s.hexPanel.getVisibility());assertTrue(s.next.isEnabled());s.next.performClick();await(s,"Sola lettura");assertTrue(s.pageLabel.getText().toString().contains("256"));
            s.popup.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();assertNull(s.popup);assertEquals(View.GONE,s.details.getVisibility());
            ((Button)s.blocks.getChildAt(0)).performClick();await(s,"Sola lettura");assertNotNull(s.popup);assertEquals(View.GONE,s.hexPanel.getVisibility());
            controller.recreate();s=controller.get().analysis.screen;await(s,"Sola lettura");assertTrue(s.frameTitle.getText().toString().contains("Video.mp4"));assertEquals(2,s.blocks.getChildCount());s.back.performClick();assertEquals(View.VISIBLE,s.files.getVisibility());assertArrayEquals(data,Files.readAllBytes(file));
        }finally{controller.pause().stop().destroy();Files.delete(file);}
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisWorkspaceTest.exifValueOpensItsExplanationAtTheTopOfAPopup -->
<a id="test-2d5f67a8f5f7d135"></a>
### `AnalysisWorkspaceTest.exifValueOpensItsExplanationAtTheTopOfAPopup`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric, URI mancanti e piccoli veri file MP4/JPEG sintetici con TIFF orientamento; worker reale, polling 10 ms fino a 5 s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisSources.video`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** JPEG con EXIF6 porta a popup con spiegazione e anatomia; dismiss/reopen coerenti.

**Limiti:** Navigazione childAt e testi precisi fragile; nessun file multimediale decodificabile, vero provider SAF o kill del processo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void exifValueOpensItsExplanationAtTheTopOfAPopup()throws Exception{
        byte[] tiff=AnalysisDetailReaderTest.orientation(true,6);ByteArrayOutputStream data=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(data);
        out.writeShort(0xffd8);out.writeShort(0xffe1);out.writeShort(tiff.length+8);out.write(new byte[]{'E','x','i','f',0,0});out.write(tiff);out.writeShort(0xffd9);
        Path file=Files.createTempFile("popup-real-",".jpg");Files.write(file,data.toByteArray());AnalysisSources.video(Arrays.asList(Uri.fromFile(file.toFile()).toString()),Arrays.asList("Foto.jpg"));
        var controller=Robolectric.buildActivity(AnalysisActivity.class).setup();try{
            AnalysisScreen s=controller.get().analysis.screen;((Button)s.files.getChildAt(2)).performClick();await(s,"Sola lettura");
            ((Button)s.blocks.getChildAt(1)).performClick();await(s,"Sola lettura");assertEquals(4,s.blocks.getChildCount());
            ((Button)s.blocks.getChildAt(2)).performClick();await(s,"Sola lettura");assertNotNull(s.popup);assertTrue(s.popup.isShowing());
            assertTrue(s.description.getText().toString().startsWith("NEL TUO FILE\n6 → rotazione di 90° in senso orario"));assertEquals(-1,s.map.indexOfChild(s.details));
            assertTrue(s.anatomy.getChildCount()>=7);s.popup.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();assertNull(s.popup);
            assertEquals(4,s.blocks.getChildCount());s.explain.performClick();await(s,"Sola lettura");assertNotNull(s.popup);
        }finally{controller.pause().stop().destroy();Files.delete(file);}
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisWorkspaceTest.missingSourceIsReadableErrorAndAllowsReturn -->
<a id="test-2a3b3b824bcdb254"></a>
### `AnalysisWorkspaceTest.missingSourceIsReadableErrorAndAllowsReturn`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric, URI mancanti e piccoli veri file MP4/JPEG sintetici con TIFF orientamento; worker reale, polling 10 ms fino a 5 s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisSources.video`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** File assente mostra errore leggibile e permette ritorno alla lista.

**Limiti:** Navigazione childAt e testi precisi fragile; nessun file multimediale decodificabile, vero provider SAF o kill del processo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void missingSourceIsReadableErrorAndAllowsReturn()throws Exception{
        AnalysisSources.video(Arrays.asList("file:///does-not-exist-analysis.mp4"),Arrays.asList("Assente.mp4"));var c=Robolectric.buildActivity(AnalysisActivity.class).setup();try{AnalysisScreen s=c.get().analysis.screen;((Button)s.files.getChildAt(2)).performClick();await(s,"Analisi non disponibile");s.back.performClick();assertEquals(View.VISIBLE,s.files.getVisibility());}finally{c.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: AnalysisWorkspaceTest.fourthTabPublishesCurrentVideoSelectionBeforeOpening -->
<a id="test-006bbda75b920fe9"></a>
### `AnalysisWorkspaceTest.fourthTabPublishesCurrentVideoSelectionBeforeOpening`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric, URI mancanti e piccoli veri file MP4/JPEG sintetici con TIFF orientamento; worker reale, polling 10 ms fino a 5 s.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AnalysisWorkspaceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `state.selectedInputs.add`, `state.selectedNames.add`, `home.screen.navigation.analysis.performClick`, `AnalysisActivity.class.getName`, `AnalysisSources.list`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Tab Analisi pubblica selezione corrente Video e apre AnalysisActivity via Intent.

**Limiti:** Navigazione childAt e testi precisi fragile; nessun file multimediale decodificabile, vero provider SAF o kill del processo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void fourthTabPublishesCurrentVideoSelectionBeforeOpening(){
        var c=Robolectric.buildActivity(MainActivity.class).setup();try{HomeState state=c.get().home.state;state.selectedInputs.add("content://preview/video");state.selectedNames.add("Prova.mp4");c.get().home.screen.navigation.analysis.performClick();assertEquals(AnalysisActivity.class.getName(),Shadows.shadowOf(c.get()).getNextStartedActivity().getComponent().getClassName());assertEquals("Prova.mp4",AnalysisSources.list(c.get()).get(0).name);assertFalse(c.get().isFinishing());}finally{c.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-audiodestinationflowtest"></a>
## AudioDestinationFlowTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AudioDestinationFlowTest.java) · 1 metodi · area originaria: File e cartelle.

**Contesto applicativo della classe:** MediaDestinationsController; AudioController; AudioActions.start; AudioActivity.onActivityResult.

**Riferimenti produttivi:** [MediaDestinationsController](../../app/src/main/java/it/edo/codecprobe/MediaDestinationsController.java), [AudioController](../../app/src/main/java/it/edo/codecprobe/AudioController.java), [AudioActions](../../app/src/main/java/it/edo/codecprobe/AudioActions.java), [AudioActivity](../../app/src/main/java/it/edo/codecprobe/AudioActivity.java).

**Preparazione, file e simulazioni condivisi:** MediaDestinationsTest.Directories e permessi ShadowUriPermission; due URI brano senza contenuto, ricreazione Activity simulata.

**Confini comuni dell’audit:** Nessun selettore di sistema o reale concessione SAF. Polling Thread.sleep(5), deadline 5 s. Certifica l'Intent, non uso cartelle nel codec.

**Metodi:** [audioUsesVerifiedPerFileFoldersAndKeepsThemAcrossRecreation](#test-0628d1e7e8602b6b)

<!-- test: AudioDestinationFlowTest.audioUsesVerifiedPerFileFoldersAndKeepsThemAcrossRecreation -->
<a id="test-0628d1e7e8602b6b"></a>
### `AudioDestinationFlowTest.audioUsesVerifiedPerFileFoldersAndKeepsThemAcrossRecreation`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): MediaDestinationsTest.Directories e permessi ShadowUriPermission; due URI brano senza contenuto, ricreazione Activity simulata.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioDestinationFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `audio.screen.destinations.body.getVisibility`, `audio.screen.start.isEnabled`, `audio.state.inputs.add`, `audio.state.names.add`, `audio.render`, `audio.screen.destinations.pickFolder.performClick`, `audio.destinations.choosing`, `audio.destinations.snapshot`, `audio.screen.start.performClick`, `audio.screen.destinations.pickFolder.isEnabled`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Eccezione di destinazione per secondo file sopravvive a recreate e viene congelata nelle folders dell'Intent START.

**Limiti:** Nessun selettore di sistema o reale concessione SAF. Polling Thread.sleep(5), deadline 5 s. Certifica l'Intent, non uso cartelle nel codec.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void audioUsesVerifiedPerFileFoldersAndKeepsThemAcrossRecreation()throws Exception{
        var controller=Robolectric.buildActivity(AudioActivity.class).setup();
        try{
            AudioController audio=controller.get().audio;assertEquals(View.GONE,audio.screen.destinations.body.getVisibility());assertFalse(audio.screen.start.isEnabled());
            MediaDestinationsTest.grant(controller.get(),MediaDestinationsTest.A);
            audio.state.folder=MediaDestinationsTest.A;
            audio.state.inputs.add("content://songs/one");audio.state.names.add("one.flac");audio.state.inputs.add("content://songs/two");audio.state.names.add("two.flac");audio.render();ready(audio);
            assertTrue(audio.screen.start.isEnabled());audio.state.selectedIndex=1;audio.render();audio.screen.destinations.pickFolder.performClick();
            assertTrue(audio.destinations.choosing());assertFalse(audio.screen.start.isEnabled());
            controller.recreate();audio=controller.get().audio;assertTrue(audio.destinations.choosing());
            controller.get().onActivityResult(MediaDestinationsController.FILE,Activity.RESULT_OK,new Intent().setData(Uri.parse(MediaDestinationsTest.B)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));ready(audio);
            assertEquals(java.util.Arrays.asList(MediaDestinationsTest.A,MediaDestinationsTest.B),audio.destinations.snapshot());
            audio.screen.start.performClick();
            Intent command=null;long end=System.currentTimeMillis()+5000;
            while(command==null&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();command=Shadows.shadowOf(controller.get()).getNextStartedService();Thread.sleep(5);}
            assertNotNull("Audio queue should be launched",command);assertEquals(java.util.Arrays.asList(MediaDestinationsTest.A,MediaDestinationsTest.B),command.getStringArrayListExtra("folders"));
            assertEquals(AudioConversionService.START,command.getAction());assertFalse(audio.screen.destinations.pickFolder.isEnabled());
        }finally{controller.pause().stop().destroy();reset();}
    }
```

</details>

<!-- /test -->


<a id="class-audiometadataflowtest"></a>
## AudioMetadataFlowTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/AudioMetadataFlowTest.java) · 5 metodi · area originaria: File e cartelle.

**Contesto applicativo della classe:** AudioLyrics.match/hash; AudioOutput.save; AudioDraftStore.save/load.

**Riferimenti produttivi:** [AudioLyrics](../../app/src/main/java/it/edo/codecprobe/AudioLyrics.java), [AudioOutput](../../app/src/main/java/it/edo/codecprobe/AudioOutput.java), [AudioDraftStore](../../app/src/main/java/it/edo/codecprobe/AudioDraftStore.java).

**Preparazione, file e simulazioni condivisi:** Provider Documents di prova crea/rimuove veri file temporanei e ParcelFileDescriptor; encoded.opus sono soltanto quattro byte, LRC con BOM/CRLF.

**Confini comuni dell’audit:** Non è Opus valido né tagging nel contenitore. Il provider non replica tutti i contratti SAF (delete risponde Bundle vuoto); nessuna galleria/player provati.

**Metodi:** [matchingPrefersSameParentAndRejectsAmbiguousTitles](#test-e0cc852412df6410) · [savesMatchingPairWithOriginalBytesAndHash](#test-61336369ae39f34f) · [sidecarFailureRollsBackNewAudioAndKeepsExistingFiles](#test-9c1c00068579513c) · [failedReadbackRemovesBothNewFiles](#test-91c81ff4bdcf3e14) · [lyricsDraftIsIndependentAndSurvivesReopen](#test-90287e0d5e2a8365)

<!-- test: AudioMetadataFlowTest.matchingPrefersSameParentAndRejectsAmbiguousTitles -->
<a id="test-e0cc852412df6410"></a>
### `AudioMetadataFlowTest.matchingPrefersSameParentAndRejectsAmbiguousTitles`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Documents di prova crea/rimuove veri file temporanei e ParcelFileDescriptor; encoded.opus sono soltanto quattro byte, LRC con BOM/CRLF.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioMetadataFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioLyrics.match`, `AudioOutput.unique`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** LRC abbinato per stesso parent, ambiguità respinta e nome audio evita conflitto con sidecar.

**Limiti:** Non è Opus valido né tagging nel contenitore. Il provider non replica tutti i contratti SAF (delete risponde Bundle vuoto); nessuna galleria/player provati.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void matchingPrefersSameParentAndRejectsAmbiguousTitles()throws Exception{
        String input="content://docs/document/primary%3AMusic%2FAlbumA%2FSong.flac";
        List<String> names=List.of("Song.lrc","Song.lrc");List<String> uris=List.of("content://docs/document/primary%3AMusic%2FAlbumB%2FSong.lrc","content://docs/document/primary%3AMusic%2FAlbumA%2FSong.lrc");
        assertEquals(uris.get(1),AudioLyrics.match(input,"Song.flac",uris,names));
        assertThrows(IOException.class,()->AudioLyrics.match("content://other/song","Song.flac",uris,names));
        assertEquals("",AudioLyrics.match(input,"Other.flac",uris,names));
        assertEquals("Song (2).opus",AudioOutput.unique("Song.opus",Set.of("Song.lrc"),true));
    }
```

</details>

<!-- /test -->

<!-- test: AudioMetadataFlowTest.savesMatchingPairWithOriginalBytesAndHash -->
<a id="test-61336369ae39f34f"></a>
### `AudioMetadataFlowTest.savesMatchingPairWithOriginalBytesAndHash`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Documents di prova crea/rimuove veri file temporanei e ParcelFileDescriptor; encoded.opus sono soltanto quattro byte, LRC con BOM/CRLF.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioMetadataFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioOutput.save`, `log.get`, `AudioLyrics.hash`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Pubblica coppia .opus/.lrc; LRC byte-identico e suo hash nel log. Non valida decodifica audio.

**Limiti:** Non è Opus valido né tagging nel contenitore. Il provider non replica tutti i contratti SAF (delete risponde Bundle vuoto); nessuna galleria/player provati.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void savesMatchingPairWithOriginalBytesAndHash()throws Exception{
        byte[] lyrics=new byte[]{(byte)0xef,(byte)0xbb,(byte)0xbf,'[','0','0',':','0','1',']','x','\r','\n'};
        ArrayList<String> log=new ArrayList<>();Uri output=AudioOutput.save(context,TREE,audio,"Song.flac",()->false,lyrics,log::add);
        assertNotNull(output);assertEquals(List.of("Song.opus","Song.lrc"),new ArrayList<>(provider.names.values()));
        File text=provider.files.entrySet().stream().filter(e->provider.names.get(e.getKey()).endsWith(".lrc")).findFirst().get().getValue();
        assertArrayEquals(lyrics,Files.readAllBytes(text.toPath()));assertTrue(log.get(0).contains(AudioLyrics.hash(lyrics)));
    }
```

</details>

<!-- /test -->

<!-- test: AudioMetadataFlowTest.sidecarFailureRollsBackNewAudioAndKeepsExistingFiles -->
<a id="test-9c1c00068579513c"></a>
### `AudioMetadataFlowTest.sidecarFailureRollsBackNewAudioAndKeepsExistingFiles`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Documents di prova crea/rimuove veri file temporanei e ParcelFileDescriptor; encoded.opus sono soltanto quattro byte, LRC con BOM/CRLF.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioMetadataFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioOutput.save`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Se create LRC fallisce, elimina l'audio appena creato e conserva keep.opus sentinella.

**Limiti:** Non è Opus valido né tagging nel contenitore. Il provider non replica tutti i contratti SAF (delete risponde Bundle vuoto); nessuna galleria/player provati.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void sidecarFailureRollsBackNewAudioAndKeepsExistingFiles()throws Exception{
        File old=new File(provider.directory,"old");Files.write(old.toPath(),new byte[]{9});provider.files.put("old",old);provider.names.put("old","keep.opus");
        provider.failLyrics=true;IOException failure=assertThrows(IOException.class,()->AudioOutput.save(context,TREE,audio,"Song.flac",()->false,new byte[]{4},s->{}));assertTrue(failure.getMessage(),failure.getMessage().contains("Creazione testo .lrc fallita"));
        assertEquals(List.of("keep.opus"),new ArrayList<>(provider.names.values()));assertArrayEquals(new byte[]{9},Files.readAllBytes(old.toPath()));
    }
```

</details>

<!-- /test -->

<!-- test: AudioMetadataFlowTest.failedReadbackRemovesBothNewFiles -->
<a id="test-91c81ff4bdcf3e14"></a>
### `AudioMetadataFlowTest.failedReadbackRemovesBothNewFiles`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Documents di prova crea/rimuove veri file temporanei e ParcelFileDescriptor; encoded.opus sono soltanto quattro byte, LRC con BOM/CRLF.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioMetadataFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AudioOutput.save`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Corrompe LRC alla rilettura: nessuno dei due nuovi file rimane.

**Limiti:** Non è Opus valido né tagging nel contenitore. Il provider non replica tutti i contratti SAF (delete risponde Bundle vuoto); nessuna galleria/player provati.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void failedReadbackRemovesBothNewFiles()throws Exception{
        provider.corruptLyrics=true;IOException failure=assertThrows(IOException.class,()->AudioOutput.save(context,TREE,audio,"Song.flac",()->false,new byte[]{4},s->{}));assertTrue(failure.getMessage(),failure.getMessage().contains("Verifica .lrc fallita"));assertTrue(provider.files.isEmpty());
    }
```

</details>

<!-- /test -->

<!-- test: AudioMetadataFlowTest.lyricsDraftIsIndependentAndSurvivesReopen -->
<a id="test-90287e0d5e2a8365"></a>
### `AudioMetadataFlowTest.lyricsDraftIsIndependentAndSurvivesReopen`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Documents di prova crea/rimuove veri file temporanei e ParcelFileDescriptor; encoded.opus sono soltanto quattro byte, LRC con BOM/CRLF.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/AudioMetadataFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `state.lyricsInputs.add`, `state.lyricsNames.add`, `AudioDraftStore.save`, `AudioDraftStore.load`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** URI e nomi LRC persistono nelle SharedPreferences simulate dopo salvataggio bozza.

**Limiti:** Non è Opus valido né tagging nel contenitore. Il provider non replica tutti i contratti SAF (delete risponde Bundle vuoto); nessuna galleria/player provati.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void lyricsDraftIsIndependentAndSurvivesReopen(){
        context.getSharedPreferences("audio-workspace",0).edit().clear().commit();AudioState state=new AudioState();state.lyricsInputs.add("content://text/1");state.lyricsNames.add("Song.lrc");AudioDraftStore.save(context,state);
        AudioState read=AudioDraftStore.load(context);assertEquals(state.lyricsInputs,read.lyricsInputs);assertEquals(state.lyricsNames,read.lyricsNames);context.getSharedPreferences("audio-workspace",0).edit().clear().commit();
    }
```

</details>

<!-- /test -->


<a id="class-creationdatetest"></a>
## CreationDateTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/CreationDateTest.java) · 3 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** CreationDatePolicy; Mp4Metadata.creationTime/copy.

**Riferimenti produttivi:** [CreationDatePolicy](../../app/src/main/java/it/edo/codecprobe/CreationDatePolicy.java), [Mp4Metadata](../../app/src/main/java/it/edo/codecprobe/Mp4Metadata.java).

**Preparazione, file e simulazioni condivisi:** MP4 sintetici con mvhd/tkhd/mdhd v0/v1; veri file temporanei e FileChannel negli ultimi due test.

**Confini comuni dell’audit:** Nessun MP4 riproducibile/encoder o indice MediaStore reale; il fallback Android è un numero fornito al metodo.

**Metodi:** [onlyMissingAndZero1904DatesUseAndroidFallback](#test-5328922dcfef35fe) · [roundTripFillsOnlyMissingCreationDatesAndPreservesMediaAndOtherMetadata](#test-dd31b61b2f821921) · [absentAndroidDateLeavesZeroUntouched](#test-dc36cc9aed88d59b)

<!-- test: CreationDateTest.onlyMissingAndZero1904DatesUseAndroidFallback -->
<a id="test-5328922dcfef35fe"></a>
### `CreationDateTest.onlyMissingAndZero1904DatesUseAndroidFallback`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): MP4 sintetici con mvhd/tkhd/mdhd v0/v1; veri file temporanei e FileChannel negli ultimi due test.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CreationDateTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CreationDatePolicy.needsFallback`, `CreationDatePolicy.fromAddedSeconds`, `CreationDatePolicy.displayMp4`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Policy su zero/1904/valore storico/ignoto, epoca 1904 e overflow.

**Limiti:** Nessun MP4 riproducibile/encoder o indice MediaStore reale; il fallback Android è un numero fornito al metodo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void onlyMissingAndZero1904DatesUseAndroidFallback()throws Exception {
        assertTrue(CreationDatePolicy.needsFallback(0,null));
        assertTrue(CreationDatePolicy.needsFallback(0,"19040101T000000.000Z"));
        assertFalse(CreationDatePolicy.needsFallback(1234,null));
        assertFalse(CreationDatePolicy.needsFallback(0,"20241012T120000.000Z"));
        assertFalse(CreationDatePolicy.needsFallback(0,"19651012T120000.000Z"));
        assertFalse(CreationDatePolicy.needsFallback(0,"unknown format"));
        assertEquals(0,CreationDatePolicy.fromAddedSeconds(0));
        assertEquals(0,CreationDatePolicy.fromAddedSeconds(-1));
        assertEquals(3782844800L,CreationDatePolicy.fromAddedSeconds(1700000000));
        assertEquals("2023-11-14T22:13:20Z",CreationDatePolicy.displayMp4(3782844800L));
        try{CreationDatePolicy.fromAddedSeconds(Long.MAX_VALUE);fail();}catch(IOException expected){}
    }
```

</details>

<!-- /test -->

<!-- test: CreationDateTest.roundTripFillsOnlyMissingCreationDatesAndPreservesMediaAndOtherMetadata -->
<a id="test-dd31b61b2f821921"></a>
### `CreationDateTest.roundTripFillsOnlyMissingCreationDatesAndPreservesMediaAndOtherMetadata`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): MP4 sintetici con mvhd/tkhd/mdhd v0/v1; veri file temporanei e FileChannel negli ultimi due test.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CreationDateTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CreationDatePolicy.fromAddedSeconds`, `Mp4Metadata.creationTime`, `Mp4Metadata.copy`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Copia date mancanti nelle tre intestazioni, preserva date esistenti/modifica, 36 byte iniziali media e titolo.

**Limiti:** Nessun MP4 riproducibile/encoder o indice MediaStore reale; il fallback Android è un numero fornito al metodo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void roundTripFillsOnlyMissingCreationDatesAndPreservesMediaAndOtherMetadata()throws Exception {
        Path input=Files.createTempFile("date-source-",".mp4"),output=Files.createTempFile("date-result-",".mp4");
        long fallback=CreationDatePolicy.fromAddedSeconds(1700000000);
        try{
            for(int version:new int[]{0,1})for(long original:new long[]{0,1234}){
                Files.write(input,movie(version,original,77));byte[] encoded=movie(version,99,100);Files.write(output,encoded);
                try(FileChannel src=FileChannel.open(input)){
                    assertEquals(original,Mp4Metadata.creationTime(src,()->false));
                    Mp4Metadata.copy(src,output.toFile(),()->false,s->{},fallback);
                }
                byte[] result=Files.readAllBytes(output);
                assertArrayEquals(Arrays.copyOf(encoded,36),Arrays.copyOf(result,36));
                assertTrue(new String(result,StandardCharsets.ISO_8859_1).contains("original title"));
                for(String type:List.of("mvhd","tkhd","mdhd")){
                    assertEquals(List.of(original==0?fallback:original),field(result,type,0));
                    assertEquals(List.of(77L),field(result,type,1));
                }
                try(FileChannel file=FileChannel.open(output)){assertEquals(original==0?fallback:original,Mp4Metadata.creationTime(file,()->false));}
            }
        }finally{Files.deleteIfExists(input);Files.deleteIfExists(output);}
    }
```

</details>

<!-- /test -->

<!-- test: CreationDateTest.absentAndroidDateLeavesZeroUntouched -->
<a id="test-dc36cc9aed88d59b"></a>
### `CreationDateTest.absentAndroidDateLeavesZeroUntouched`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): MP4 sintetici con mvhd/tkhd/mdhd v0/v1; veri file temporanei e FileChannel negli ultimi due test.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/CreationDateTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `Mp4Metadata.copy`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Se il fallback fornito è zero, la creazione mancante rimane zero.

**Limiti:** Nessun MP4 riproducibile/encoder o indice MediaStore reale; il fallback Android è un numero fornito al metodo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void absentAndroidDateLeavesZeroUntouched()throws Exception {
        Path input=Files.createTempFile("date-source-",".mp4"),output=Files.createTempFile("date-result-",".mp4");
        try{
            Files.write(input,movie(0,0,0));Files.write(output,movie(0,99,100));
            try(FileChannel src=FileChannel.open(input)){Mp4Metadata.copy(src,output.toFile(),()->false,s->{},0);}
            assertEquals(List.of(0L),field(Files.readAllBytes(output),"mvhd",0));
        }finally{Files.deleteIfExists(input);Files.deleteIfExists(output);}
    }
```

</details>

<!-- /test -->


<a id="class-exifdictionarytest"></a>
## ExifDictionaryTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java) · 8 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** ExifTagDictionary; ExifAnalysisParser.read; AnalysisDetailReader.read; AnalysisLabels.of.

**Riferimenti produttivi:** [ExifTagDictionary](../../app/src/main/java/it/edo/codecprobe/ExifTagDictionary.java), [ExifAnalysisParser](../../app/src/main/java/it/edo/codecprobe/ExifAnalysisParser.java), [AnalysisDetailReader](../../app/src/main/java/it/edo/codecprobe/AnalysisDetailReader.java), [AnalysisLabels](../../app/src/main/java/it/edo/codecprobe/AnalysisLabels.java).

**Preparazione, file e simulazioni condivisi:** TIFF sintetici little/big endian scritti dal helper AnalysisDetailReaderTest.with; snapshot test resources exif-standard-ids.txt.

**Confini comuni dell’audit:** Non certifica l'intero standard EXIF né immagini di fotocamere reali. size()==165 e stringhe italiane sono sensibili ad estensioni/traduzioni; lo snapshot è interno al repo.

**Metodi:** [standardCoverageAndDirectoryNamespacesAreExplicit](#test-e90240d6784d624c) · [everyIdInTheReferenceSnapshotHasAnItalianExplanation](#test-b870da62fb2323f2) · [sameNumericIdGetsTheMeaningOfItsOwnDirectory](#test-3213aa673f8df9cc) · [exposureAndSignedEvHaveUnitsAndZeroDenominatorsStayVisible](#test-8b33851e97e9d993) · [accelerationUsesMilligalRatherThanTreatingRawValueAsSiUnits](#test-5d5aa95a48dfb0e6) · [enumValuesAreHighlightedButWrongSchemasAndUnknownCodesAreNotGuessed](#test-55c42480aeec9435) · [flashBitsAndCoordinatesDescribeActualComponents](#test-070280b2cce0b79d) · [knownUndefinedLayoutsAreDecodedAndOpaquePayloadsRemainExplicit](#test-45c89d95133ee1d9)

<!-- test: ExifDictionaryTest.standardCoverageAndDirectoryNamespacesAreExplicit -->
<a id="test-e90240d6784d624c"></a>
### `ExifDictionaryTest.standardCoverageAndDirectoryNamespacesAreExplicit`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): TIFF sintetici little/big endian scritti dal helper AnalysisDetailReaderTest.with; snapshot test resources exif-standard-ids.txt.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ExifTagDictionary.size`, `ExifTagDictionary.find`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Numero fisso di 165 voci, namespace GPS/Interop/IFD0 e significati presenti.

**Limiti:** Non certifica l'intero standard EXIF né immagini di fotocamere reali. size()==165 e stringhe italiane sono sensibili ad estensioni/traduzioni; lo snapshot è interno al repo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void standardCoverageAndDirectoryNamespacesAreExplicit(){
        assertEquals(165,ExifTagDictionary.size());
        assertEquals("GPSLatitudeRef",ExifTagDictionary.find(1,"GPS").technical);
        assertEquals("InteropIndex",ExifTagDictionary.find(1,"Interop").technical);
        assertNull(ExifTagDictionary.find(1,"IFD0"));
        assertNull(ExifTagDictionary.find(0x112,"GPS"));
        assertNull(ExifTagDictionary.find(0xffff,"EXIF"));
        for(int id=0;id<32;id++){assertNotNull(ExifTagDictionary.find(id,"GPS"));assertFalse(ExifTagDictionary.find(id,"GPS").meaning.isEmpty());}
        assertTrue(ExifTagDictionary.find(0x9287,"EXIF").meaning.contains("non è ancora decodificato"));
        assertTrue(ExifTagDictionary.find(0xa40d,"EXIF").meaning.contains("Due codici"));
    }
```

</details>

<!-- /test -->

<!-- test: ExifDictionaryTest.everyIdInTheReferenceSnapshotHasAnItalianExplanation -->
<a id="test-b870da62fb2323f2"></a>
### `ExifDictionaryTest.everyIdInTheReferenceSnapshotHasAnItalianExplanation`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): TIFF sintetici little/big endian scritti dal helper AnalysisDetailReaderTest.with; snapshot test resources exif-standard-ids.txt.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `reader.readLine`, `ExifTagDictionary.find`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Ogni ID dello snapshot interno ha nome tecnico e descrizione italiana sufficientemente lunga.

**Limiti:** Non certifica l'intero standard EXIF né immagini di fotocamere reali. size()==165 e stringhe italiane sono sensibili ad estensioni/traduzioni; lo snapshot è interno al repo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void everyIdInTheReferenceSnapshotHasAnItalianExplanation()throws Exception{
        try(java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/exif-standard-ids.txt")),StandardCharsets.UTF_8))){
        for(String line;(line=reader.readLine())!=null;){
            String[] cells=line.split(" ");int id=Integer.parseInt(cells[0],16);
            ExifTagDictionary.Entry e=ExifTagDictionary.find(id,"EXIF");assertNotNull(line,e);
            assertEquals(line,cells[1],e.technical);assertTrue(line,e.meaning.length()>20);assertFalse(line,e.name.isEmpty());
        }}
    }
```

</details>

<!-- /test -->

<!-- test: ExifDictionaryTest.sameNumericIdGetsTheMeaningOfItsOwnDirectory -->
<a id="test-3213aa673f8df9cc"></a>
### `ExifDictionaryTest.sameNumericIdGetsTheMeaningOfItsOwnDirectory`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): TIFF sintetici little/big endian scritti dal helper AnalysisDetailReaderTest.with; snapshot test resources exif-standard-ids.txt.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `AnalysisLabels.of`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Stesso ID interpretato secondo directory; tag fuori namespace non indovinato.

**Limiti:** Non certifica l'intero standard EXIF né immagini di fotocamere reali. size()==165 e stringhe italiane sono sensibili ad estensioni/traduzioni; lo snapshot è interno al repo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void sameNumericIdGetsTheMeaningOfItsOwnDirectory()throws Exception{
        single("GPS",1,2,2,new byte[]{'S',0},true,(n,d)->{assertEquals("Emisfero della latitudine",AnalysisLabels.of(n).name);assertTrue(d.preview().contains("S → sud"));assertTrue(d.alternatives.contains("N = nord"));});
        single("Interop",1,2,4,new byte[]{'R','9','8',0},false,(n,d)->{assertEquals("Profilo di interoperabilità",AnalysisLabels.of(n).name);assertTrue(d.introduction.contains("R98"));assertFalse(d.alternatives.contains("nord"));});
        single("GPS",0x112,3,1,shorts(true,6),true,(n,d)->{assertEquals("Tag EXIF non tradotto",AnalysisLabels.of(n).name);assertFalse(d.result.contains("rotazione"));});
    }
```

</details>

<!-- /test -->

<!-- test: ExifDictionaryTest.exposureAndSignedEvHaveUnitsAndZeroDenominatorsStayVisible -->
<a id="test-8b33851e97e9d993"></a>
### `ExifDictionaryTest.exposureAndSignedEvHaveUnitsAndZeroDenominatorsStayVisible`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): TIFF sintetici little/big endian scritti dal helper AnalysisDetailReaderTest.with; snapshot test resources exif-standard-ids.txt.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `single(…)`, `fractions(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Rational e signed EV con unità, endianness, denominatore zero e conversione APEX.

**Limiti:** Non certifica l'intero standard EXIF né immagini di fotocamere reali. size()==165 e stringhe italiane sono sensibili ad estensioni/traduzioni; lo snapshot è interno al repo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void exposureAndSignedEvHaveUnitsAndZeroDenominatorsStayVisible()throws Exception{
        for(boolean little:new boolean[]{true,false}){
            single("IFD0",0x829a,5,1,fractions(little,1,100),little,(n,d)->{assertTrue(d.result.contains("1/100"));assertTrue(d.preview().endsWith(" s."));assertTrue(d.introduction.contains("Tempo di esposizione"));assertEquals(8,d.parts.get(4).size);});
            single("IFD0",0x9204,10,1,fractions(little,-1,3),little,(n,d)->{assertTrue(d.preview().contains("-0.33333333 EV"));});
        }
        single("IFD0",0x829a,5,1,fractions(true,1,0),true,(n,d)->{assertTrue(d.result.contains("divisione non definita"));assertTrue(d.preview().contains("denominatore zero"));});
        single("IFD0",0x9201,10,1,fractions(true,7,1),true,(n,d)->assertTrue(d.preview().contains("0.0078125000 s")));
    }
```

</details>

<!-- /test -->

<!-- test: ExifDictionaryTest.accelerationUsesMilligalRatherThanTreatingRawValueAsSiUnits -->
<a id="test-5d5aa95a48dfb0e6"></a>
### `ExifDictionaryTest.accelerationUsesMilligalRatherThanTreatingRawValueAsSiUnits`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): TIFF sintetici little/big endian scritti dal helper AnalysisDetailReaderTest.with; snapshot test resources exif-standard-ids.txt.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `single(…)`, `fractions(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Accelerazione mGal convertita in m/s² con fixture 100000.

**Limiti:** Non certifica l'intero standard EXIF né immagini di fotocamere reali. size()==165 e stringhe italiane sono sensibili ad estensioni/traduzioni; lo snapshot è interno al repo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void accelerationUsesMilligalRatherThanTreatingRawValueAsSiUnits()throws Exception{
        single("IFD0",0x9404,5,1,fractions(true,100000,1),true,(n,d)->{assertTrue(d.preview().contains("mGal = 1.0000000 m/s²"));});
    }
```

</details>

<!-- /test -->

<!-- test: ExifDictionaryTest.enumValuesAreHighlightedButWrongSchemasAndUnknownCodesAreNotGuessed -->
<a id="test-55c42480aeec9435"></a>
### `ExifDictionaryTest.enumValuesAreHighlightedButWrongSchemasAndUnknownCodesAreNotGuessed`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): TIFF sintetici little/big endian scritti dal helper AnalysisDetailReaderTest.with; snapshot test resources exif-standard-ids.txt.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `single(…)`, `shorts(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Enum noto evidenziato; codice/tipo/count/terminatore ASCII invalidi non inventano interpretazioni.

**Limiti:** Non certifica l'intero standard EXIF né immagini di fotocamere reali. size()==165 e stringhe italiane sono sensibili ad estensioni/traduzioni; lo snapshot è interno al repo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void enumValuesAreHighlightedButWrongSchemasAndUnknownCodesAreNotGuessed()throws Exception{
        single("IFD0",0xa403,3,1,shorts(true,1),true,(n,d)->{assertTrue(d.preview().contains("1 → manuale"));assertTrue(d.alternatives.contains("1 = manuale  [PRESENTE NEL FILE]"));});
        single("IFD0",0xa403,3,1,shorts(true,99),true,(n,d)->assertTrue(d.preview().contains("valore non riconosciuto")));
        single("IFD0",0xa403,3,2,shorts(true,1,0),true,(n,d)->assertTrue(d.preview().contains("tipo o quantità")));
        single("GPS",1,2,2,new byte[]{'S','X'},true,(n,d)->assertTrue(d.preview().contains("terminatore nullo")));
    }
```

</details>

<!-- /test -->

<!-- test: ExifDictionaryTest.flashBitsAndCoordinatesDescribeActualComponents -->
<a id="test-070280b2cce0b79d"></a>
### `ExifDictionaryTest.flashBitsAndCoordinatesDescribeActualComponents`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): TIFF sintetici little/big endian scritti dal helper AnalysisDetailReaderTest.with; snapshot test resources exif-standard-ids.txt.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `single(…)`, `shorts(…)`, `fractions(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Bit flash, coordinate pixel e GPS DMS descrivono componenti effettivi del payload.

**Limiti:** Non certifica l'intero standard EXIF né immagini di fotocamere reali. size()==165 e stringhe italiane sono sensibili ad estensioni/traduzioni; lo snapshot è interno al repo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void flashBitsAndCoordinatesDescribeActualComponents()throws Exception{
        single("IFD0",0x9209,3,1,shorts(false,0x59),false,(n,d)->{assertTrue(d.preview().contains("flash scattato"));assertTrue(d.preview().contains("automatica"));assertTrue(d.preview().contains("occhi rossi attiva"));assertTrue(d.alternatives.contains("Bit 3–4"));});
        single("IFD0",0xa214,3,2,shorts(true,120,240),true,(n,d)->{assertTrue(d.preview().contains("x orizzontale = 120 px"));assertTrue(d.preview().contains("y verticale = 240 px"));});
        single("IFD0",0x9214,3,4,shorts(true,120,240,50,80),true,(n,d)->assertTrue(d.preview().contains("larghezza = 50 px; altezza = 80 px")));
        single("GPS",2,5,3,fractions(true,45,1,30,1,0,1),true,(n,d)->{assertTrue(d.preview().contains("45.500000°"));assertTrue(d.preview().contains("Segno da leggere"));});
    }
```

</details>

<!-- /test -->

<!-- test: ExifDictionaryTest.knownUndefinedLayoutsAreDecodedAndOpaquePayloadsRemainExplicit -->
<a id="test-45c89d95133ee1d9"></a>
### `ExifDictionaryTest.knownUndefinedLayoutsAreDecodedAndOpaquePayloadsRemainExplicit`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): TIFF sintetici little/big endian scritti dal helper AnalysisDetailReaderTest.with; snapshot test resources exif-standard-ids.txt.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ExifDictionaryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `single(…)`, `shorts(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Layout UNDEFINED noti decodificati; payload non noto esplicitamente opaco.

**Limiti:** Non certifica l'intero standard EXIF né immagini di fotocamere reali. size()==165 e stringhe italiane sono sensibili ad estensioni/traduzioni; lo snapshot è interno al repo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void knownUndefinedLayoutsAreDecodedAndOpaquePayloadsRemainExplicit()throws Exception{
        single("IFD0",0x9000,7,4,new byte[]{'0','2','3','2'},true,(n,d)->assertTrue(d.preview().contains("versione 2.32")));
        single("IFD0",0xa300,7,1,new byte[]{3},true,(n,d)->assertTrue(d.preview().contains("fotocamera digitale")));
        single("IFD0",0x9101,7,4,new byte[]{1,2,3,0},true,(n,d)->assertTrue(d.preview().contains("1 = Y, 2 = Cb, 3 = Cr, 0 = assente")));
        byte[] comment=new byte[]{65,83,67,73,73,0,0,0,'c','i','a','o',0};
        single("IFD0",0x9286,7,comment.length,comment,true,(n,d)->assertTrue(d.preview().contains("ciao\\0")));
        single("IFD0",0x8828,7,4,new byte[]{1,2,3,4},true,(n,d)->{assertTrue(d.introduction.contains("non viene decodificata"));assertTrue(d.result.contains("UNDEFINED"));assertFalse(d.result.contains("VALORE TROVATO"));});
        single("IFD0",0xa40d,3,2,shorts(true,4,2),true,(n,d)->assertTrue(d.preview().contains("differenze estreme; impostazioni: 2 → diverse")));
    }
```

</details>

<!-- /test -->


<a id="class-folderpickeractivitytest"></a>
## FolderPickerActivityTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/FolderPickerActivityTest.java) · 5 metodi · area originaria: File e cartelle.

**Contesto applicativo della classe:** FolderPickerActivity.onActivityResult/onCreate; ContentResolver shadow.

**Riferimenti produttivi:** [FolderPickerActivity](../../app/src/main/java/it/edo/codecprobe/FolderPickerActivity.java).

**Preparazione, file e simulazioni condivisi:** Robolectric Activity/ShadowActivity/URI permission; nessun DocumentsUI di sistema.

**Confini comuni dell’audit:** Il selettore non viene aperto davvero e la persistenza grant è del modello Robolectric; polling Thread.sleep(10) fino a 5 s.

**Metodi:** [opensSystemPickerOnceAndDoesNotReopenOnResume](#test-0af9a890e54fd028) · [restoresPendingSystemPickerWithoutLaunchingAnother](#test-9efeb173982c697a) · [cancellationReturnsToCallerWithoutClaimingAProvidedFolder](#test-5a9c5338e48bb81b) · [successfulGrantIsPersistedAndReturnedToCaller](#test-92802e3767ff192f) · [readOnlyResponseIsReportedAsAnError](#test-a2066d5d7073c69c)

<!-- test: FolderPickerActivityTest.opensSystemPickerOnceAndDoesNotReopenOnResume -->
<a id="test-0af9a890e54fd028"></a>
### `FolderPickerActivityTest.opensSystemPickerOnceAndDoesNotReopenOnResume`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric Activity/ShadowActivity/URI permission; nessun DocumentsUI di sistema.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/FolderPickerActivityTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `shadowOf(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Emette un solo Intent ACTION_OPEN_DOCUMENT_TREE, senza flags/extras, anche dopo resume.

**Limiti:** Il selettore non viene aperto davvero e la persistenza grant è del modello Robolectric; polling Thread.sleep(10) fino a 5 s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void opensSystemPickerOnceAndDoesNotReopenOnResume(){
        try(ActivityController<FolderPickerActivity> c=Robolectric.buildActivity(FolderPickerActivity.class).setup()){
            ShadowActivity.IntentForResult started=shadowOf(c.get()).getNextStartedActivityForResult();assertNotNull(started);
            assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE,started.intent.getAction());assertEquals(0,started.intent.getFlags());assertNull(started.intent.getExtras());
            c.pause().resume();assertNull(shadowOf(c.get()).getNextStartedActivityForResult());
        }
    }
```

</details>

<!-- /test -->

<!-- test: FolderPickerActivityTest.restoresPendingSystemPickerWithoutLaunchingAnother -->
<a id="test-9efeb173982c697a"></a>
### `FolderPickerActivityTest.restoresPendingSystemPickerWithoutLaunchingAnother`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric Activity/ShadowActivity/URI permission; nessun DocumentsUI di sistema.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/FolderPickerActivityTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `saved.getBoolean`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Bundle launched evita nuovo Intent dopo ripristino.

**Limiti:** Il selettore non viene aperto davvero e la persistenza grant è del modello Robolectric; polling Thread.sleep(10) fino a 5 s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void restoresPendingSystemPickerWithoutLaunchingAnother(){
        Bundle saved=new Bundle();
        try(ActivityController<FolderPickerActivity> first=Robolectric.buildActivity(FolderPickerActivity.class).setup()){assertNotNull(shadowOf(first.get()).getNextStartedActivityForResult());first.saveInstanceState(saved);assertTrue(saved.getBoolean("launched"));}
        try(ActivityController<FolderPickerActivity> restored=Robolectric.buildActivity(FolderPickerActivity.class).setup(saved)){assertNull(shadowOf(restored.get()).getNextStartedActivityForResult());}
    }
```

</details>

<!-- /test -->

<!-- test: FolderPickerActivityTest.cancellationReturnsToCallerWithoutClaimingAProvidedFolder -->
<a id="test-5a9c5338e48bb81b"></a>
### `FolderPickerActivityTest.cancellationReturnsToCallerWithoutClaimingAProvidedFolder`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric Activity/ShadowActivity/URI permission; nessun DocumentsUI di sistema.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/FolderPickerActivityTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `shadowOf(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Cancel termina con RESULT_CANCELED e nessun permesso.

**Limiti:** Il selettore non viene aperto davvero e la persistenza grant è del modello Robolectric; polling Thread.sleep(10) fino a 5 s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void cancellationReturnsToCallerWithoutClaimingAProvidedFolder(){
        try(ActivityController<FolderPickerActivity> c=Robolectric.buildActivity(FolderPickerActivity.class).setup()){
            c.get().onActivityResult(1,Activity.RESULT_CANCELED,null);assertTrue(c.get().isFinishing());assertEquals(Activity.RESULT_CANCELED,shadowOf(c.get()).getResultCode());assertTrue(c.get().getContentResolver().getPersistedUriPermissions().isEmpty());
        }
    }
```

</details>

<!-- /test -->

<!-- test: FolderPickerActivityTest.successfulGrantIsPersistedAndReturnedToCaller -->
<a id="test-92802e3767ff192f"></a>
### `FolderPickerActivityTest.successfulGrantIsPersistedAndReturnedToCaller`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric Activity/ShadowActivity/URI permission; nessun DocumentsUI di sistema.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/FolderPickerActivityTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `awaitFinish(…)`, `shadowOf(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Risposta OK con read/write persiste permesso shadow e restituisce URI.

**Limiti:** Il selettore non viene aperto davvero e la persistenza grant è del modello Robolectric; polling Thread.sleep(10) fino a 5 s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void successfulGrantIsPersistedAndReturnedToCaller()throws Exception{
        try(ActivityController<FolderPickerActivity> c=Robolectric.buildActivity(FolderPickerActivity.class).setup()){
            Uri uri=Uri.parse("content://example.documents/tree/test");
            c.get().onActivityResult(1,Activity.RESULT_OK,new Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
            awaitFinish(c.get());java.util.List<UriPermission> permissions=c.get().getContentResolver().getPersistedUriPermissions();assertEquals(1,permissions.size());assertEquals(uri,permissions.get(0).getUri());assertTrue(permissions.get(0).isReadPermission());assertTrue(permissions.get(0).isWritePermission());
            assertEquals(Activity.RESULT_OK,shadowOf(c.get()).getResultCode());assertEquals(uri,shadowOf(c.get()).getResultIntent().getData());
        }
    }
```

</details>

<!-- /test -->

<!-- test: FolderPickerActivityTest.readOnlyResponseIsReportedAsAnError -->
<a id="test-a2066d5d7073c69c"></a>
### `FolderPickerActivityTest.readOnlyResponseIsReportedAsAnError`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric Activity/ShadowActivity/URI permission; nessun DocumentsUI di sistema.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/FolderPickerActivityTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `awaitFinish(…)`, `shadowOf(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Risposta readonly non persiste grant e restituisce errore/cancel.

**Limiti:** Il selettore non viene aperto davvero e la persistenza grant è del modello Robolectric; polling Thread.sleep(10) fino a 5 s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void readOnlyResponseIsReportedAsAnError()throws Exception{
        try(ActivityController<FolderPickerActivity> c=Robolectric.buildActivity(FolderPickerActivity.class).setup()){
            c.get().onActivityResult(1,Activity.RESULT_OK,new Intent().setData(Uri.parse("content://example.documents/tree/test")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            awaitFinish(c.get());assertTrue(c.get().getContentResolver().getPersistedUriPermissions().isEmpty());assertEquals(Activity.RESULT_CANCELED,shadowOf(c.get()).getResultCode());assertNotNull(shadowOf(c.get()).getResultIntent().getStringExtra(FolderPickerActivity.ERROR));
        }
    }
```

</details>

<!-- /test -->


<a id="class-imagedestinationflowtest"></a>
## ImageDestinationFlowTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageDestinationFlowTest.java) · 1 metodi · area originaria: File e cartelle.

**Contesto applicativo della classe:** ImageDestinations/MediaDestinationsController; ImageSelection; ImageActions; ImageQueueStore.

**Riferimenti produttivi:** [MediaDestinationsController](../../app/src/main/java/it/edo/codecprobe/MediaDestinationsController.java), [ImageSelection](../../app/src/main/java/it/edo/codecprobe/ImageSelection.java), [ImageActions](../../app/src/main/java/it/edo/codecprobe/ImageActions.java), [ImageQueueStore](../../app/src/main/java/it/edo/codecprobe/ImageQueueStore.java).

**Preparazione, file e simulazioni condivisi:** Due piccoli JPEG sintetici ImageWorkspaceTest.fixture; provider directory fittizio/grant shadow; Activity recreate.

**Confini comuni dell’audit:** Solo Intent e queue su disco, non output nelle cartelle; polling5ms/5s. Le foto non attraversano JNI.

**Metodi:** [photosUseSameFolderFlowAndFreezeTheCorrectPerFileDestinations](#test-a2b5e30a7b63a85a)

<!-- test: ImageDestinationFlowTest.photosUseSameFolderFlowAndFreezeTheCorrectPerFileDestinations -->
<a id="test-a2b5e30a7b63a85a"></a>
### `ImageDestinationFlowTest.photosUseSameFolderFlowAndFreezeTheCorrectPerFileDestinations`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Due piccoli JPEG sintetici ImageWorkspaceTest.fixture; provider directory fittizio/grant shadow; Activity recreate.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageDestinationFlowTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.screen.convert.isEnabled`, `images.state.inputs.add`, `images.setFolder`, `images.open`, `images.destinations.photoGeneral`, `images.destinations.photoException`, `images.destinations.choosePhotoFolder`, `FolderPickerActivity.class.getName`, `images.destinations.choosing`, `images.destinations.snapshot`, `images.render`, `images.screen.convert.performClick`, `ImageQueueStore.read`, `ImageQueueStore.discard`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Eccezione seconda cartella sopravvive recreate; START passa solo queueId e qualità65, queue contiene due folders congelate e picker bloccato mentre busy.

**Limiti:** Solo Intent e queue su disco, non output nelle cartelle; polling5ms/5s. Le foto non attraversano JNI.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void photosUseSameFolderFlowAndFreezeTheCorrectPerFileDestinations()throws Exception{
        File one=File.createTempFile("photo-one-",".jpg"),two=File.createTempFile("photo-two-",".jpg");Files.write(one.toPath(),ImageWorkspaceTest.fixture());Files.write(two.toPath(),ImageWorkspaceTest.fixture());
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            ImageController images=controller.get().images;assertNotNull(images.destinations.photo);assertFalse(images.screen.convert.isEnabled());
            images.state.inputs.add(Uri.fromFile(one).toString());images.state.inputs.add(Uri.fromFile(two).toString());images.setFolder(MediaDestinationsTest.A);MediaDestinationsTest.grant(controller.get(),MediaDestinationsTest.A);images.open();ready(images);
            assertTrue(images.screen.convert.isEnabled());images.state.selected=1;images.open();ready(images);images.destinations.photoGeneral(false);images.destinations.photoException(true);images.destinations.choosePhotoFolder();assertFalse(images.screen.convert.isEnabled());
            Intent picker=Shadows.shadowOf(controller.get()).getNextStartedActivity();assertNotNull(picker);assertEquals(FolderPickerActivity.class.getName(),picker.getComponent().getClassName());
            controller.recreate();images=controller.get().images;assertTrue(images.destinations.choosing());
            controller.get().onActivityResult(MediaDestinationsController.FILE,Activity.RESULT_OK,new Intent().setData(Uri.parse(MediaDestinationsTest.B)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));ready(images);
            assertEquals(java.util.Arrays.asList(MediaDestinationsTest.A,MediaDestinationsTest.B),images.destinations.snapshot());
            images.state.reading=true;images.state.info=null;images.render();assertTrue(images.screen.convert.isEnabled());
            images.screen.convert.performClick();Intent command=null;long end=System.currentTimeMillis()+5000;
            while(command==null&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();command=Shadows.shadowOf(controller.get()).getNextStartedService();Thread.sleep(5);}
            assertNotNull(command);assertEquals(ImageConversionService.START,command.getAction());assertNull(command.getStringArrayListExtra("inputs"));assertNull(command.getStringArrayListExtra("folders"));
            String queueId=command.getStringExtra("queueId");assertNotNull(queueId);ImageQueueStore.Queue queued=ImageQueueStore.read(controller.get(),queueId);
            assertEquals(java.util.Arrays.asList(MediaDestinationsTest.A,MediaDestinationsTest.B),queued.folders);assertEquals(2,queued.inputs.size());ImageQueueStore.discard(controller.get(),queueId);assertEquals(65,command.getIntExtra("quality",-1));images.destinations.choosePhotoFolder();assertNull(Shadows.shadowOf(controller.get()).getNextStartedActivity());
        }finally{controller.pause().stop().destroy();one.delete();two.delete();reset();}
    }
```

</details>

<!-- /test -->


<a id="class-imagemetadatachecktest"></a>
## ImageMetadataCheckTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageMetadataCheckTest.java) · 4 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** ImageQueueStore.writeInspection; ImageActions/Selection; ImageConversionService metadataOnly; ImageProgressJournal; ImageRunState.

**Riferimenti produttivi:** [ImageQueueStore](../../app/src/main/java/it/edo/codecprobe/ImageQueueStore.java), [ImageActions](../../app/src/main/java/it/edo/codecprobe/ImageActions.java), [ImageConversionService](../../app/src/main/java/it/edo/codecprobe/ImageConversionService.java), [ImageProgressJournal](../../app/src/main/java/it/edo/codecprobe/ImageProgressJournal.java), [ImageRunState](../../app/src/main/java/it/edo/codecprobe/ImageRunState.java).

**Preparazione, file e simulazioni condivisi:** 8500 URI ripetuti o inesistenti; BlockingPhotos restituisce FileNotFoundException; run CHECKED creata a mano e database nel runtime Robolectric.

**Confini comuni dell’audit:** Non esamina metadati di una foto valida. Tutte le vere elaborazioni del servizio falliscono prima di ispezione nativa; CHECKED8500 non prodotti da inspect. Polling5ms fino10s.

**Metodi:** [inspectionQueueNeedsNoDestinationAndKeepsLargeSelection](#test-1feb8981528504cd) · [compatibilityButtonStartsWithoutFolderAndUsesDiskQueue](#test-34ed0dd59c576041) · [checkPausesResumesAndKeepsRetryableProblemsSeparateFromConversionOutcomes](#test-d6c028e0674e0a2e) · [checkedCountersStaySeparateAndSurviveBoundedResultWindow](#test-b48be66972aaa493)

<!-- test: ImageMetadataCheckTest.inspectionQueueNeedsNoDestinationAndKeepsLargeSelection -->
<a id="test-1feb8981528504cd"></a>
### `ImageMetadataCheckTest.inspectionQueueNeedsNoDestinationAndKeepsLargeSelection`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 8500 URI ripetuti o inesistenti; BlockingPhotos restituisce FileNotFoundException; run CHECKED creata a mano e database nel runtime Robolectric.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageMetadataCheckTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageQueueStore.writeInspection`, `ImageQueueStore.read`, `ImageQueueStore.discard`, `ImageQueueStore.write`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Queue inspection8500 permette folder vuota; normale conversione la rifiuta.

**Limiti:** Non esamina metadati di una foto valida. Tutte le vere elaborazioni del servizio falliscono prima di ispezione nativa; CHECKED8500 non prodotti da inspect. Polling5ms fino10s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void inspectionQueueNeedsNoDestinationAndKeepsLargeSelection()throws Exception{
        var c=RuntimeEnvironment.getApplication();List<String> inputs=Collections.nCopies(8500,"content://photos/source");
        String id=ImageQueueStore.writeInspection(c,inputs,Collections.emptyList(),true);
        try{var q=ImageQueueStore.read(c,id);assertTrue(q.metadataOnly);assertEquals(8500,q.inputs.size());assertEquals("",q.folders.get(8499));}
        finally{ImageQueueStore.discard(c,id);}
        try{ImageQueueStore.write(c,Arrays.asList("content://photos/source"),Collections.emptyList(),Arrays.asList(""));fail("Conversion still requires a destination");}catch(IOException expected){}
    }
```

</details>

<!-- /test -->

<!-- test: ImageMetadataCheckTest.compatibilityButtonStartsWithoutFolderAndUsesDiskQueue -->
<a id="test-34ed0dd59c576041"></a>
### `ImageMetadataCheckTest.compatibilityButtonStartsWithoutFolderAndUsesDiskQueue`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 8500 URI ripetuti o inesistenti; BlockingPhotos restituisce FileNotFoundException; run CHECKED creata a mano e database nel runtime Robolectric.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageMetadataCheckTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.state.inputs.clear`, `images.state.names.clear`, `images.state.inputs.add`, `images.state.names.add`, `images.state.options.withFolder`, `images.render`, `images.screen.convert.isEnabled`, `images.screen.checkMetadata.isEnabled`, `images.screen.checkMetadata.performClick`, `ImageQueueStore.read`, `ImageQueueStore.discard`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Pulsante verifica genera Intent metadataOnly con disk queue anche senza destinazione.

**Limiti:** Non esamina metadati di una foto valida. Tutte le vere elaborazioni del servizio falliscono prima di ispezione nativa; CHECKED8500 non prodotti da inspect. Polling5ms fino10s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void compatibilityButtonStartsWithoutFolderAndUsesDiskQueue()throws Exception{
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();String id=null;
        try{
            var images=controller.get().images;images.state.inputs.clear();images.state.names.clear();images.state.inputs.add("content://missing/photo");images.state.names.add("photo.jpg");images.state.options=images.state.options.withFolder(null);images.render();
            assertFalse(images.screen.convert.isEnabled());assertTrue(images.screen.checkMetadata.isEnabled());images.screen.checkMetadata.performClick();
            Intent[] request={null};await(()->{if(request[0]==null)request[0]=Shadows.shadowOf(controller.get()).getNextStartedService();return request[0]!=null;});
            assertEquals(ImageConversionService.START,request[0].getAction());assertTrue(request[0].getBooleanExtra("metadataOnly",false));id=request[0].getStringExtra("queueId");assertTrue(ImageQueueStore.read(controller.get(),id).metadataOnly);
        }finally{ImageQueueStore.discard(controller.get(),id);controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageMetadataCheckTest.checkPausesResumesAndKeepsRetryableProblemsSeparateFromConversionOutcomes -->
<a id="test-d6c028e0674e0a2e"></a>
### `ImageMetadataCheckTest.checkPausesResumesAndKeepsRetryableProblemsSeparateFromConversionOutcomes`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 8500 URI ripetuti o inesistenti; BlockingPhotos restituisce FileNotFoundException; run CHECKED creata a mano e database nel runtime Robolectric.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageMetadataCheckTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageQueueStore.writeInspection`, `ImageOptions.load`, `ImageConversionService.snapshot`, `ImageLogStore.snapshot`, `ImageQueueStore.file`, `ImageQueueStore.discard`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Due foto non apribili: pausa/riprendi conserva errori retryabili nel journal, nessun progresso conversione e nessun temporaneo residuo.

**Limiti:** Non esamina metadati di una foto valida. Tutte le vere elaborazioni del servizio falliscono prima di ispezione nativa; CHECKED8500 non prodotti da inspect. Polling5ms fino10s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void checkPausesResumesAndKeepsRetryableProblemsSeparateFromConversionOutcomes()throws Exception{
        var c=RuntimeEnvironment.getApplication();ImagePauseTest.BlockingPhotos photos=new ImagePauseTest.BlockingPhotos();ShadowContentResolver.registerProviderInternal("pausephotos",photos);
        String id=ImageQueueStore.writeInspection(c,Arrays.asList("content://pausephotos/one","content://pausephotos/two"),Arrays.asList("one.jpg","two.jpg"),false);
        var controller=Robolectric.buildService(ImageConversionService.class).create();
        try{
            Intent start=new Intent().setAction(ImageConversionService.START).putExtra("queueId",id).putExtra("metadataOnly",true);ImageOptions.load(c).withFolder(null).put(start);controller.get().onStartCommand(start,0,1);
            assertTrue(photos.entered.await(5,TimeUnit.SECONDS));long token=ImageConversionService.snapshot().startedAt;
            controller.get().onStartCommand(new Intent().setAction(ImageConversionService.PAUSE).putExtra("startedAt",token),0,2);photos.release.countDown();await(()->ImageConversionService.snapshot().paused);
            assertTrue(ImageConversionService.snapshot().metadataOnly);assertEquals(1,ImageConversionService.snapshot().completed);assertRegistry(c,1);
            File copy=ImageLogStore.snapshot(c,ImageConversionService.LOG);try{assertTrue(read(copy).contains("INIZIO VERIFICA METADATI"));}finally{copy.delete();}
            controller.get().onStartCommand(new Intent().setAction(ImageConversionService.RESUME).putExtra("startedAt",token),0,3);await(()->!ImageConversionService.snapshot().running);
            var run=ImageConversionService.snapshot();assertEquals(2,run.failed);assertEquals(0,run.saved);assertEquals(0,run.checked);assertRegistry(c,2);assertTrue(read(new File(c.getFilesDir(),ImageFailureStore.FILE)).contains("metadata-check"));assertFalse(ImageQueueStore.file(c,id).exists());
            File[] tmp=c.getCacheDir().listFiles((d,n)->n.startsWith("image-source-")||n.startsWith("image-heic-"));assertNotNull(tmp);assertEquals(0,tmp.length);
        }finally{photos.release.countDown();controller.get().onStartCommand(new Intent().setAction(ImageConversionService.CANCEL).putExtra("startedAt",ImageConversionService.snapshot().startedAt),0,4);await(()->!ImageConversionService.snapshot().running);controller.destroy();ImageQueueStore.discard(c,id);}
    }
```

</details>

<!-- /test -->

<!-- test: ImageMetadataCheckTest.checkedCountersStaySeparateAndSurviveBoundedResultWindow -->
<a id="test-b48be66972aaa493"></a>
### `ImageMetadataCheckTest.checkedCountersStaySeparateAndSurviveBoundedResultWindow`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 8500 URI ripetuti o inesistenti; BlockingPhotos restituisce FileNotFoundException; run CHECKED creata a mano e database nel runtime Robolectric.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageMetadataCheckTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `state.add`, `ImageRunState.Result`, `images.screen.sections.focusCard`, `images.screen.dashboard.summary.getText`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** 8500 CHECKED artificiali mantengono conteggio separato/SAVED0 e100 dettagli; dashboard li presenta come Compatibili, non Riuscite.

**Limiti:** Non esamina metadati di una foto valida. Tutte le vere elaborazioni del servizio falliscono prima di ispezione nativa; CHECKED8500 non prodotti da inspect. Polling5ms fino10s.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void checkedCountersStaySeparateAndSurviveBoundedResultWindow()throws Exception{
        ImageRunState state=new ImageRunState();state.metadataOnly=true;state.current=8500;state.total=8500;
        for(int n=0;n<8500;n++)state.add(new ImageRunState.Result("photo",100,0,null,"Metadati compatibili",ImageRunState.Outcome.CHECKED,"Pixel non verificati"));
        ImageRunState copy=new ImageRunState(state);assertTrue(copy.metadataOnly);assertEquals(8500,copy.checked);assertEquals(0,copy.saved);assertEquals(0,copy.cancelledCount);assertEquals(0,copy.inputSavedBytes);assertEquals(100,copy.results.size());
        var controller=Robolectric.buildActivity(ImageActivity.class).setup();try{stateField.set(null,copy);controller.get().images.screen.sections.focusCard(ImageScreen.HEIC);assertTrue(controller.get().images.screen.dashboard.summary.getText().toString().contains("Compatibili: 8500"));assertFalse(controller.get().images.screen.dashboard.summary.getText().toString().contains("Riuscite:"));var row=(android.widget.LinearLayout)ImageVirtualReportTest.row(controller.get().images,0,null);assertTrue(row.getChildAt(1).getContentDescription().toString().startsWith("Mostra dettagli:"));assertEquals("i",((android.widget.Button)row.getChildAt(1)).getText().toString());}finally{controller.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-imageoutputfailuretest"></a>
## ImageOutputFailureTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageOutputFailureTest.java) · 5 metodi · area originaria: File e cartelle.

**Contesto applicativo della classe:** ImageOutput.save; gestione errori e rollback.

**Riferimenti produttivi:** [ImageOutput](../../app/src/main/java/it/edo/codecprobe/ImageOutput.java).

**Preparazione, file e simulazioni condivisi:** TemporaryFolder; provider Documents specializzato inietta create/write/read/delete failure; veri stream e sentinelle di 16 e 4 byte.

**Confini comuni dell’audit:** Nessun encoder è invocato, nonostante il commento 'no encoder or JNI is replaced'. Non prova provider reale, spazio esaurito a metà stream o crash durante commit.

**Metodi:** [missingStagedFileNeverCreatesAnOutputOrDeletesTheExistingName](#test-694c079702c8254d) · [revokedCreatePermissionIsReportedWithoutOverwritingAnExistingFile](#test-dc99a22d584c1bdd) · [writeFailureRollsBackOnlyTheNewDocument](#test-df2c3146d6e1e0a8) · [unreadableOutputIsNotDeclaredSavedAndIsRolledBack](#test-a36d1c09d767f94c) · [failedRollbackRetainsThePrimaryErrorAndItsDiagnosticCause](#test-708f69796e2eb3df)

<!-- test: ImageOutputFailureTest.missingStagedFileNeverCreatesAnOutputOrDeletesTheExistingName -->
<a id="test-694c079702c8254d"></a>
### `ImageOutputFailureTest.missingStagedFileNeverCreatesAnOutputOrDeletesTheExistingName`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): TemporaryFolder; provider Documents specializzato inietta create/write/read/delete failure; veri stream e sentinelle di 16 e 4 byte.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageOutputFailureTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageOutput.save`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** File staged assente non crea output e non elimina sentinella omonima.

**Limiti:** Nessun encoder è invocato, nonostante il commento 'no encoder or JNI is replaced'. Non prova provider reale, spazio esaurito a metà stream o crash durante commit.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void missingStagedFileNeverCreatesAnOutputOrDeletesTheExistingName()throws Exception {
        assertTrue(encoded.delete());assertThrows(FileNotFoundException.class,()->ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.jpg",()->false));
        assertEquals(Set.of("existing"),provider.files.keySet());assertArrayEquals(PREVIOUS,Files.readAllBytes(existing.toPath()));
    }
```

</details>

<!-- /test -->

<!-- test: ImageOutputFailureTest.revokedCreatePermissionIsReportedWithoutOverwritingAnExistingFile -->
<a id="test-dc99a22d584c1bdd"></a>
### `ImageOutputFailureTest.revokedCreatePermissionIsReportedWithoutOverwritingAnExistingFile`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): TemporaryFolder; provider Documents specializzato inietta create/write/read/delete failure; veri stream e sentinelle di 16 e 4 byte.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageOutputFailureTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `publishFailure(…)`, `assertNoNewDocument(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** SecurityException create propagata come causa IOException; sorgente e precedente intatti.

**Limiti:** Nessun encoder è invocato, nonostante il commento 'no encoder or JNI is replaced'. Non prova provider reale, spazio esaurito a metà stream o crash durante commit.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void revokedCreatePermissionIsReportedWithoutOverwritingAnExistingFile()throws Exception {
        provider.createDenied=true;IOException error=publishFailure();
        assertTrue(error.getCause() instanceof SecurityException);assertNoNewDocument();
    }
```

</details>

<!-- /test -->

<!-- test: ImageOutputFailureTest.writeFailureRollsBackOnlyTheNewDocument -->
<a id="test-df2c3146d6e1e0a8"></a>
### `ImageOutputFailureTest.writeFailureRollsBackOnlyTheNewDocument`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): TemporaryFolder; provider Documents specializzato inietta create/write/read/delete failure; veri stream e sentinelle di 16 e 4 byte.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageOutputFailureTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `publishFailure(…)`, `assertNoNewDocument(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Write negato elimina solo nuovo documento e conserva errore senza suppressed.

**Limiti:** Nessun encoder è invocato, nonostante il commento 'no encoder or JNI is replaced'. Non prova provider reale, spazio esaurito a metà stream o crash durante commit.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void writeFailureRollsBackOnlyTheNewDocument()throws Exception {
        provider.writeDenied=true;IOException error=publishFailure();
        assertTrue(error.getMessage().contains("write denied"));assertEquals(0,error.getSuppressed().length);assertNoNewDocument();
    }
```

</details>

<!-- /test -->

<!-- test: ImageOutputFailureTest.unreadableOutputIsNotDeclaredSavedAndIsRolledBack -->
<a id="test-a36d1c09d767f94c"></a>
### `ImageOutputFailureTest.unreadableOutputIsNotDeclaredSavedAndIsRolledBack`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): TemporaryFolder; provider Documents specializzato inietta create/write/read/delete failure; veri stream e sentinelle di 16 e 4 byte.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageOutputFailureTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `publishFailure(…)`, `assertNoNewDocument(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Rilettura negata non dichiara successo e rimuove output.

**Limiti:** Nessun encoder è invocato, nonostante il commento 'no encoder or JNI is replaced'. Non prova provider reale, spazio esaurito a metà stream o crash durante commit.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void unreadableOutputIsNotDeclaredSavedAndIsRolledBack()throws Exception {
        provider.readDenied=true;IOException error=publishFailure();
        assertTrue(error.getMessage().contains("reread denied"));assertNoNewDocument();
    }
```

</details>

<!-- /test -->

<!-- test: ImageOutputFailureTest.failedRollbackRetainsThePrimaryErrorAndItsDiagnosticCause -->
<a id="test-708f69796e2eb3df"></a>
### `ImageOutputFailureTest.failedRollbackRetainsThePrimaryErrorAndItsDiagnosticCause`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): TemporaryFolder; provider Documents specializzato inietta create/write/read/delete failure; veri stream e sentinelle di 16 e 4 byte.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageOutputFailureTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `publishFailure(…)`, `assertOriginalsPreserved(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Write fallita più delete negata: conserva errore primario e suppressed rollback; documento orfano visibile e originali intatti.

**Limiti:** Nessun encoder è invocato, nonostante il commento 'no encoder or JNI is replaced'. Non prova provider reale, spazio esaurito a metà stream o crash durante commit.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void failedRollbackRetainsThePrimaryErrorAndItsDiagnosticCause()throws Exception {
        provider.writeDenied=true;provider.deleteDenied=true;IOException error=publishFailure();
        assertTrue(error.getMessage().contains("write denied"));assertEquals(1,error.getSuppressed().length);
        assertTrue(error.getSuppressed()[0].getMessage().contains("rollback denied"));
        assertEquals(2,provider.files.size());assertOriginalsPreserved();
    }
```

</details>

<!-- /test -->


<a id="class-imageoutputtest"></a>
## ImageOutputTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageOutputTest.java) · 5 metodi · area originaria: File e cartelle.

**Contesto applicativo della classe:** ImageOutput.save/hasMotionVideo/name/unique; ImageOutput.Names.

**Riferimenti produttivi:** [ImageOutput](../../app/src/main/java/it/edo/codecprobe/ImageOutput.java).

**Preparazione, file e simulazioni condivisi:** Provider Documents fittizio e veri file temporanei: pseudo-HEIC contiene solo ftyp16, variante mpvd con quattro byte.

**Confini comuni dell’audit:** Nessuna immagine valida, encoder o JNI. SAF simulato; cancellation è true già all'ingresso, non durante una scrittura lunga.

**Metodi:** [savesReReadsAllBytesAndAddsSuffixInsteadOfOverwriting](#test-77ad007bd60e1c69) · [motionNamesKeepMpSuffixAcrossCollisionsAndHeaderScanSkipsPayload](#test-b494c0f105771f83) · [wholeBatchListsDestinationOnceAndRetainsCaseInsensitiveReservations](#test-70974a6e940c7713) · [sameLengthCorruptionRollsBackTheNewDocument](#test-8df9969d635af9da) · [cancellationPublishesNothing](#test-aa3dc7d8dd124ac2)

<!-- test: ImageOutputTest.savesReReadsAllBytesAndAddsSuffixInsteadOfOverwriting -->
<a id="test-77ad007bd60e1c69"></a>
### `ImageOutputTest.savesReReadsAllBytesAndAddsSuffixInsteadOfOverwriting`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Documents fittizio e veri file temporanei: pseudo-HEIC contiene solo ftyp16, variante mpvd con quattro byte.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageOutputTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageOutput.save`, `saved.toPath`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Pubblica e rilegge tutti i byte, collisione Photo produce Photo (2), senza overwrite.

**Limiti:** Nessuna immagine valida, encoder o JNI. SAF simulato; cancellation è true già all'ingresso, non durante una scrittura lunga.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void savesReReadsAllBytesAndAddsSuffixInsteadOfOverwriting()throws Exception{
        Uri first=ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.JPG",()->false),second=ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.JPG",()->false);assertNotEquals(first,second);assertEquals(java.util.List.of("Photo.heic","Photo (2).heic"),new java.util.ArrayList<>(provider.names.values()));for(File saved:provider.files.values())assertArrayEquals(Files.readAllBytes(encoded.toPath()),Files.readAllBytes(saved.toPath()));
    }
```

</details>

<!-- /test -->

<!-- test: ImageOutputTest.motionNamesKeepMpSuffixAcrossCollisionsAndHeaderScanSkipsPayload -->
<a id="test-b494c0f105771f83"></a>
### `ImageOutputTest.motionNamesKeepMpSuffixAcrossCollisionsAndHeaderScanSkipsPayload`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Documents fittizio e veri file temporanei: pseudo-HEIC contiene solo ftyp16, variante mpvd con quattro byte.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageOutputTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageOutput.hasMotionVideo`, `ImageOutput.save`, `ImageOutput.name`, `ImageOutput.unique`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Marker mpvd determina suffisso MP e lo conserva sulle collisioni; box che supera EOF respinto.

**Limiti:** Nessuna immagine valida, encoder o JNI. SAF simulato; cancellation è true già all'ingresso, non durante una scrittura lunga.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void motionNamesKeepMpSuffixAcrossCollisionsAndHeaderScanSkipsPayload()throws Exception{
        try(DataOutputStream out=new DataOutputStream(new FileOutputStream(encoded))){out.writeInt(16);out.writeBytes("ftypheic");out.writeInt(0);out.writeInt(12);out.writeBytes("mpvd");out.writeInt(123);}
        assertTrue(ImageOutput.hasMotionVideo(encoded));
        ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.JPG",()->false);
        ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.JPG",()->false);
        assertEquals(java.util.List.of("Photo_MP.heic","Photo (2)_MP.heic"),new java.util.ArrayList<>(provider.names.values()));
        assertEquals("AlreadyMP.heic",ImageOutput.name("AlreadyMP.jpg",true));
        assertEquals("Already (2)MP.heic",ImageOutput.unique("AlreadyMP.heic",java.util.Set.of("AlreadyMP.heic")));
        try(RandomAccessFile file=new RandomAccessFile(encoded,"rw")){file.seek(16);file.writeInt(99);}
        assertThrows(IOException.class,()->ImageOutput.hasMotionVideo(encoded));
    }
```

</details>

<!-- /test -->

<!-- test: ImageOutputTest.wholeBatchListsDestinationOnceAndRetainsCaseInsensitiveReservations -->
<a id="test-70974a6e940c7713"></a>
### `ImageOutputTest.wholeBatchListsDestinationOnceAndRetainsCaseInsensitiveReservations`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Documents fittizio e veri file temporanei: pseudo-HEIC contiene solo ftyp16, variante mpvd con quattro byte.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageOutputTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageOutput.Names`, `ImageOutput.save`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** 100 pubblicazioni alternate maiuscolo/minuscolo: listing una volta per Names, un'altra con nuova cache; nessun duplicato case-insensitive.

**Limiti:** Nessuna immagine valida, encoder o JNI. SAF simulato; cancellation è true già all'ingresso, non durante una scrittura lunga.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void wholeBatchListsDestinationOnceAndRetainsCaseInsensitiveReservations()throws Exception{
        ImageOutput.Names names=new ImageOutput.Names();
        for(int i=0;i<100;i++)ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,i%2==0?"Photo.JPG":"photo.JPG",()->false,names);
        assertEquals(1,provider.listings);assertEquals(100,provider.files.size());
        java.util.Set<String> unique=new java.util.HashSet<>();for(String name:provider.names.values())assertTrue(unique.add(name.toLowerCase(java.util.Locale.ROOT)));
        ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.JPG",()->false,new ImageOutput.Names());
        assertEquals(2,provider.listings);assertEquals(101,provider.files.size());
    }
```

</details>

<!-- /test -->

<!-- test: ImageOutputTest.sameLengthCorruptionRollsBackTheNewDocument -->
<a id="test-8df9969d635af9da"></a>
### `ImageOutputTest.sameLengthCorruptionRollsBackTheNewDocument`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Documents fittizio e veri file temporanei: pseudo-HEIC contiene solo ftyp16, variante mpvd con quattro byte.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageOutputTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageOutput.save`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Un byte corrotto senza cambiare lunghezza causa errore e rimozione documento.

**Limiti:** Nessuna immagine valida, encoder o JNI. SAF simulato; cancellation è true già all'ingresso, non durante una scrittura lunga.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void sameLengthCorruptionRollsBackTheNewDocument(){provider.corrupt=true;assertThrows(IOException.class,()->ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.jpg",()->false));assertTrue(provider.files.isEmpty());assertTrue(provider.names.isEmpty());}
```

</details>

<!-- /test -->

<!-- test: ImageOutputTest.cancellationPublishesNothing -->
<a id="test-aa3dc7d8dd124ac2"></a>
### `ImageOutputTest.cancellationPublishesNothing`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Documents fittizio e veri file temporanei: pseudo-HEIC contiene solo ftyp16, variante mpvd con quattro byte.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageOutputTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageOutput.save`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Cancel immediato non pubblica documenti.

**Limiti:** Nessuna immagine valida, encoder o JNI. SAF simulato; cancellation è true già all'ingresso, non durante una scrittura lunga.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void cancellationPublishesNothing(){assertThrows(IOException.class,()->ImageOutput.save(context,AudioMetadataFlowTest.TREE,encoded,"Photo.jpg",()->true));assertTrue(provider.files.isEmpty());}
```

</details>

<!-- /test -->


<a id="class-imageprogressjournaltest"></a>
## ImageProgressJournalTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java) · 7 metodi · area originaria: File e cartelle.

**Contesto applicativo della classe:** ImageProgressJournal.record/reusable/inspection/failures/onUpgrade; ImageFingerprint; ImageQueueStore; ImageRunState.

**Riferimenti produttivi:** [ImageProgressJournal](../../app/src/main/java/it/edo/codecprobe/ImageProgressJournal.java), [ImageFingerprint](../../app/src/main/java/it/edo/codecprobe/ImageFingerprint.java), [ImageQueueStore](../../app/src/main/java/it/edo/codecprobe/ImageQueueStore.java), [ImageRunState](../../app/src/main/java/it/edo/codecprobe/ImageRunState.java).

**Preparazione, file e simulazioni condivisi:** Robolectric Android SQLite e ContentResolver file://; veri file 4 byte e DB di8500 record, hash inventati in alcuni risultati. Versione1 SQL costruita nel test.

**Confini comuni dell’audit:** Riutilizzo verifica veri hash ma non decodifica output. Migrazione copre solo1→2 e schema esplicito, non migrazioni future, corruzione/power loss. Dimensione DB non misura RAM.

**Metodi:** [reuseChecksBothFileContentsAndSettingsEvenWhenSizeDoesNotChange](#test-4df2d92a77e94411) · [latestOutcomeReplacesThePreviousOneAndFailuresCanBeLoadedWithoutDetails](#test-cc4b4af0771cd536) · [inspectionFailuresRemainRetryableWithoutErasingConversionHistory](#test-0a586ed43f988d5b) · [upgradesVersionOneWithoutLosingExistingRecords](#test-c2235211502aa77d) · [eightThousandFiveHundredRecordsOccupyLessThanTwentyMb](#test-ff992d78179ea855) · [completeLargeFolderHandoffKeepsAllEntriesAndDraftSurvivesRecreation](#test-345baa513f6f7a3f) · [reusedOutputsContributeOnceToCurrentBatchSavings](#test-f68b56f724e120e5)

<!-- test: ImageProgressJournalTest.reuseChecksBothFileContentsAndSettingsEvenWhenSizeDoesNotChange -->
<a id="test-4df2d92a77e94411"></a>
### `ImageProgressJournalTest.reuseChecksBothFileContentsAndSettingsEvenWhenSizeDoesNotChange`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric Android SQLite e ContentResolver file://; veri file 4 byte e DB di8500 record, hash inventati in alcuni risultati. Versione1 SQL costruita nel test.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `journal.record`, `journal.reusable`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Stessa lunghezza ma byte sorgente/output mutati, qualità diversa, output mancante o cancel impediscono ALREADY_DONE.

**Limiti:** Riutilizzo verifica veri hash ma non decodifica output. Migrazione copre solo1→2 e schema esplicito, non migrazioni future, corruzione/power loss. Dimensione DB non misura RAM.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void reuseChecksBothFileContentsAndSettingsEvenWhenSizeDoesNotChange()throws Exception{
        File original=File.createTempFile("journal-original-",".jpg"),output=File.createTempFile("journal-output-",".heic");
        try(ImageProgressJournal journal=new ImageProgressJournal(context)){
            Files.write(original.toPath(),new byte[]{1,2,3,4});Files.write(output.toPath(),new byte[]{5,6,7,8});String input=Uri.fromFile(original).toString();ImageRunState.Result result=saved(original,output);
            journal.record(input,options().folder,options(),result);
            assertEquals(ImageRunState.Outcome.ALREADY_DONE,journal.reusable(context,input,"foto.jpg",options(),()->false).outcome);
            Files.write(original.toPath(),new byte[]{9,2,3,4});assertNull(journal.reusable(context,input,"foto.jpg",options(),()->false));Files.write(original.toPath(),new byte[]{1,2,3,4});
            Files.write(output.toPath(),new byte[]{9,6,7,8});assertNull(journal.reusable(context,input,"foto.jpg",options(),()->false));Files.write(output.toPath(),new byte[]{5,6,7,8});
            ImageOptions different=new ImageOptions(70,"slow","standard",options().folder,true,false,0);assertNull(journal.reusable(context,input,"foto.jpg",different,()->false));
            try{journal.reusable(context,input,"foto.jpg",options(),()->true);fail("Cancellation ignored");}catch(InterruptedIOException expected){}
            output.delete();assertNull(journal.reusable(context,input,"foto.jpg",options(),()->false));
        }finally{original.delete();output.delete();}
    }
```

</details>

<!-- /test -->

<!-- test: ImageProgressJournalTest.latestOutcomeReplacesThePreviousOneAndFailuresCanBeLoadedWithoutDetails -->
<a id="test-cc4b4af0771cd536"></a>
### `ImageProgressJournalTest.latestOutcomeReplacesThePreviousOneAndFailuresCanBeLoadedWithoutDetails`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric Android SQLite e ContentResolver file://; veri file 4 byte e DB di8500 record, hash inventati in alcuni risultati. Versione1 SQL costruita nel test.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `journal.record`, `ImageRunState.Result`, `journal.failures`, `journal.getReadableDatabase`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Ultimo esito sostituisce FAILED, retry carica URI/cartella senza100k dettagli e resta una riga.

**Limiti:** Riutilizzo verifica veri hash ma non decodifica output. Migrazione copre solo1→2 e schema esplicito, non migrazioni future, corruzione/power loss. Dimensione DB non misura RAM.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void latestOutcomeReplacesThePreviousOneAndFailuresCanBeLoadedWithoutDetails()throws Exception{
        String input="content://photo/one";
        try(ImageProgressJournal journal=new ImageProgressJournal(context)){
            journal.record(input,options().folder,options(),new ImageRunState.Result("foto.jpg",0,0,null,"Errore",ImageRunState.Outcome.FAILED,"x".repeat(100000)));
            ImageQueueStore.Queue retry=journal.failures();assertEquals(Collections.singletonList(input),retry.inputs);assertEquals(Collections.singletonList(options().folder),retry.folders);
            journal.record(input,options().folder,options(),new ImageRunState.Result("foto.jpg",100,50,"content://output/one","Salvato",ImageRunState.Outcome.SAVED,"","a".repeat(64),"b".repeat(64)));
            assertTrue(journal.failures().inputs.isEmpty());
            try(var rows=journal.getReadableDatabase().rawQuery("SELECT count(*) FROM progress",null)){assertTrue(rows.moveToFirst());assertEquals(1,rows.getInt(0));}
        }
    }
```

</details>

<!-- /test -->

<!-- test: ImageProgressJournalTest.inspectionFailuresRemainRetryableWithoutErasingConversionHistory -->
<a id="test-0a586ed43f988d5b"></a>
### `ImageProgressJournalTest.inspectionFailuresRemainRetryableWithoutErasingConversionHistory`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric Android SQLite e ContentResolver file://; veri file 4 byte e DB di8500 record, hash inventati in alcuni risultati. Versione1 SQL costruita nel test.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageRunState.Result`, `journal.inspection`, `journal.failures`, `journal.record`, `journal.getReadableDatabase`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Inspection fail/CHECKED/CANCELLED mantiene distinta storia conversione e retry; successo metadati non nasconde fallimento conversione.

**Limiti:** Riutilizzo verifica veri hash ma non decodifica output. Migrazione copre solo1→2 e schema esplicito, non migrazioni future, corruzione/power loss. Dimensione DB non misura RAM.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void inspectionFailuresRemainRetryableWithoutErasingConversionHistory(){
        try(ImageProgressJournal journal=new ImageProgressJournal(context)){
            String uri="content://photo/check";
            ImageRunState.Result failure=new ImageRunState.Result("check.jpg",0,0,null,"Metadata",ImageRunState.Outcome.FAILED,"");
            journal.inspection(uri,failure);assertEquals(java.util.List.of(uri),journal.failures().inputs);
            assertEquals("",journal.failures().folders.get(0));
            journal.record(uri,options().folder,options(),failure);assertEquals(1,journal.failures().inputs.size());assertEquals(options().folder,journal.failures().folders.get(0));
            journal.inspection(uri,new ImageRunState.Result("check.jpg",0,0,null,"OK",ImageRunState.Outcome.CHECKED,""));
            assertEquals(1,journal.failures().inputs.size()); // A metadata pass must not hide a conversion failure.
            journal.record(uri,options().folder,options(),new ImageRunState.Result("check.jpg",100,50,"content://output", "OK",ImageRunState.Outcome.SAVED,""));
            assertTrue(journal.failures().inputs.isEmpty());
            journal.inspection(uri,failure);assertEquals(1,journal.failures().inputs.size());
            journal.inspection(uri,new ImageRunState.Result("check.jpg",0,0,null,"Interrupted",ImageRunState.Outcome.CANCELLED,""));assertEquals(1,journal.failures().inputs.size());
            journal.inspection(uri,new ImageRunState.Result("check.jpg",0,0,null,"OK",ImageRunState.Outcome.CHECKED,""));assertTrue(journal.failures().inputs.isEmpty());
            assertNotNull(journal.getReadableDatabase());
        }
    }
```

</details>

<!-- /test -->

<!-- test: ImageProgressJournalTest.upgradesVersionOneWithoutLosingExistingRecords -->
<a id="test-c2235211502aa77d"></a>
### `ImageProgressJournalTest.upgradesVersionOneWithoutLosingExistingRecords`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric Android SQLite e ContentResolver file://; veri file 4 byte e DB di8500 record, hash inventati in alcuni risultati. Versione1 SQL costruita nel test.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `journal.failures`, `journal.getReadableDatabase`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Database SQL versione1 con record FAILED viene aperto a2 senza perdere URI.

**Limiti:** Riutilizzo verifica veri hash ma non decodifica output. Migrazione copre solo1→2 e schema esplicito, non migrazioni future, corruzione/power loss. Dimensione DB non misura RAM.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void upgradesVersionOneWithoutLosingExistingRecords(){
        var db=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(ImageProgressJournal.DATABASE),null);
        db.execSQL("CREATE TABLE progress (uri TEXT PRIMARY KEY, name TEXT NOT NULL, folder TEXT NOT NULL, settings TEXT NOT NULL, status TEXT NOT NULL, source_sha TEXT NOT NULL, output_uri TEXT NOT NULL, output_sha TEXT NOT NULL, input_bytes INTEGER NOT NULL, output_bytes INTEGER NOT NULL, updated INTEGER NOT NULL)");
        db.execSQL("INSERT INTO progress VALUES ('content://old','old.jpg','content://dest','old','FAILED','','','',0,0,1)");db.setVersion(1);db.close();
        try(ImageProgressJournal journal=new ImageProgressJournal(context)){assertEquals(java.util.List.of("content://old"),journal.failures().inputs);assertEquals(2,journal.getReadableDatabase().getVersion());}
    }
```

</details>

<!-- /test -->

<!-- test: ImageProgressJournalTest.eightThousandFiveHundredRecordsOccupyLessThanTwentyMb -->
<a id="test-ff992d78179ea855"></a>
### `ImageProgressJournalTest.eightThousandFiveHundredRecordsOccupyLessThanTwentyMb`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric Android SQLite e ContentResolver file://; veri file 4 byte e DB di8500 record, hash inventati in alcuni risultati. Versione1 SQL costruita nel test.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `journal.getWritableDatabase`, `journal.record`, `ImageRunState.Result`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** 8500 record reali SQLite stanno sotto20MB: test di budget su host, hash/output solo stringhe.

**Limiti:** Riutilizzo verifica veri hash ma non decodifica output. Migrazione copre solo1→2 e schema esplicito, non migrazioni future, corruzione/power loss. Dimensione DB non misura RAM.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void eightThousandFiveHundredRecordsOccupyLessThanTwentyMb()throws Exception{
        try(ImageProgressJournal journal=new ImageProgressJournal(context)){
            var db=journal.getWritableDatabase();db.beginTransaction();try{
                for(int i=0;i<8500;i++){
                    String uri="content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FCamera/document/primary%3ADCIM%2FCamera%2FIMG_20261021_"+i+".jpg";
                    journal.record(uri,options().folder,options(),new ImageRunState.Result("IMG_20261021_"+i+".jpg",3000000,1800000,"content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FHEIC/document/primary%3ADCIM%2FHEIC%2FIMG_"+i+".heic","Salvato",ImageRunState.Outcome.SAVED,"NO TECHNICAL DETAILS IN DB","a".repeat(64),"b".repeat(64)));
                }db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }
        long bytes=context.getDatabasePath(ImageProgressJournal.DATABASE).length();assertTrue(bytes>0);assertTrue("Registry size: "+bytes,bytes<20000000L);System.out.println("IMAGE_REGISTRY_8500_BYTES="+bytes);
    }
```

</details>

<!-- /test -->

<!-- test: ImageProgressJournalTest.completeLargeFolderHandoffKeepsAllEntriesAndDraftSurvivesRecreation -->
<a id="test-345baa513f6f7a3f"></a>
### `ImageProgressJournalTest.completeLargeFolderHandoffKeepsAllEntriesAndDraftSurvivesRecreation`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric Android SQLite e ContentResolver file://; veri file 4 byte e DB di8500 record, hash inventati in alcuni risultati. Versione1 SQL costruita nel test.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `state.inputs.add`, `state.names.add`, `ImageDraftStore.save`, `ImageDraftStore.load`, `ImageQueueStore.write`, `ImageQueueStore.read`, `ImageQueueStore.discard`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Bozza bulk8500 resta completa e handoff disk queue mantiene8500 riferimenti; nessuna Activity ricreata malgrado il nome.

**Limiti:** Riutilizzo verifica veri hash ma non decodifica output. Migrazione copre solo1→2 e schema esplicito, non migrazioni future, corruzione/power loss. Dimensione DB non misura RAM.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void completeLargeFolderHandoffKeepsAllEntriesAndDraftSurvivesRecreation()throws Exception{
        ImageState state=new ImageState();state.bulk=true;state.selectionLabel="8500 JPEG dalla cartella";state.options=options();ArrayList<String> folders=new ArrayList<>();
        for(int i=0;i<8500;i++){state.inputs.add("content://photos/"+i);state.names.add("Foto "+i);folders.add(options().folder);}
        ImageDraftStore.save(context,state);ImageState restored=ImageDraftStore.load(context);assertEquals(state.inputs,restored.inputs);assertEquals(state.names,restored.names);assertTrue(restored.bulk);
        String id=ImageQueueStore.write(context,state.inputs,state.names,folders,true);try{assertEquals(8500,ImageQueueStore.read(context,id).inputs.size());}finally{ImageQueueStore.discard(context,id);}
    }
```

</details>

<!-- /test -->

<!-- test: ImageProgressJournalTest.reusedOutputsContributeOnceToCurrentBatchSavings -->
<a id="test-f68b56f724e120e5"></a>
### `ImageProgressJournalTest.reusedOutputsContributeOnceToCurrentBatchSavings`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Robolectric Android SQLite e ContentResolver file://; veri file 4 byte e DB di8500 record, hash inventati in alcuni risultati. Versione1 SQL costruita nel test.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/ImageProgressJournalTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `run.add`, `ImageRunState.Result`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** ALREADY_DONE e SAVED manuali contribuiscono una volta ciascuno ai4MB risparmiati.

**Limiti:** Riutilizzo verifica veri hash ma non decodifica output. Migrazione copre solo1→2 e schema esplicito, non migrazioni future, corruzione/power loss. Dimensione DB non misura RAM.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void reusedOutputsContributeOnceToCurrentBatchSavings(){
        ImageRunState run=new ImageRunState();run.add(new ImageRunState.Result("existing.jpg",3000000,1000000,"content://saved","Già presente",ImageRunState.Outcome.ALREADY_DONE,""));
        run.add(new ImageRunState.Result("new.jpg",3000000,1000000,"content://saved2","Salvata",ImageRunState.Outcome.SAVED,""));
        assertEquals(1,run.saved);assertEquals(1,run.alreadyDone);assertEquals(4000000,run.inputSavedBytes-run.outputSavedBytes);
    }
```

</details>

<!-- /test -->


<a id="class-inputcreationdatetest"></a>
## InputCreationDateTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/InputCreationDateTest.java) · 1 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** InputCreationDate.fromAndroid; CreationDatePolicy.

**Riferimenti produttivi:** [InputCreationDate](../../app/src/main/java/it/edo/codecprobe/InputCreationDate.java), [CreationDatePolicy](../../app/src/main/java/it/edo/codecprobe/CreationDatePolicy.java).

**Preparazione, file e simulazioni condivisi:** Dates ContentProvider restituisce DATE_ADDED1700000000 o null e vieta scritture; URI media ecloud sintetici.

**Confini comuni dell’audit:** Nessun MediaStore reale, OEM o modifica timestamp video. Asserisce log e query specifica, non data finale galleria.

**Metodi:** [readsOriginalDateAddedInSecondsOnlyWhenInternalDateMissing](#test-8bb28282d4e6480e)

<!-- test: InputCreationDateTest.readsOriginalDateAddedInSecondsOnlyWhenInternalDateMissing -->
<a id="test-8bb28282d4e6480e"></a>
### `InputCreationDateTest.readsOriginalDateAddedInSecondsOnlyWhenInternalDateMissing`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Dates ContentProvider restituisce DATE_ADDED1700000000 o null e vieta scritture; URI media ecloud sintetici.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/InputCreationDateTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `InputCreationDate.fromAndroid`, `log.stream`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** DATE_ADDED in secondi convertito epocaMP4 solo se data interna manca/sentinella; data valida evita query, null/cloud danno0.

**Limiti:** Nessun MediaStore reale, OEM o modifica timestamp video. Asserisce log e query specifica, non data finale galleria.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void readsOriginalDateAddedInSecondsOnlyWhenInternalDateMissing()throws Exception {
        Dates dates=new Dates();ShadowContentResolver.registerProviderInternal("media",dates);
        Context context=RuntimeEnvironment.getApplication();Uri input=Uri.parse("content://media/external/video/media/77?requireOriginal=1");
        List<String> log=new ArrayList<>();
        long fallback=InputCreationDate.fromAndroid(context,input,0,"19040101T000000.000Z",log::add);
        assertEquals(3782844800L,fallback);assertEquals(1,dates.queries);
        assertTrue(log.stream().anyMatch(s->s.contains("provenienza=Android MediaStore.DATE_ADDED")));
        assertEquals(0,InputCreationDate.fromAndroid(context,input,0,"20241012T120000.000Z",log::add));
        assertEquals(0,InputCreationDate.fromAndroid(context,input,1234,null,log::add));assertEquals(1,dates.queries);
        dates.added=null;assertEquals(0,InputCreationDate.fromAndroid(context,input,0,null,log::add));
        assertEquals(0,InputCreationDate.fromAndroid(context,Uri.parse("content://cloud/video/77"),0,null,log::add));
    }
```

</details>

<!-- /test -->


<a id="class-mediadestinationstest"></a>
## MediaDestinationsTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/MediaDestinationsTest.java) · 3 metodi · area originaria: File e cartelle.

**Contesto applicativo della classe:** DestinationDraft; DestinationPlan; QueueDestinations.read; MediaDestinationsController.

**Riferimenti produttivi:** [DestinationDraft](../../app/src/main/java/it/edo/codecprobe/DestinationDraft.java), [DestinationPlan](../../app/src/main/java/it/edo/codecprobe/DestinationPlan.java), [QueueDestinations](../../app/src/main/java/it/edo/codecprobe/QueueDestinations.java), [MediaDestinationsController](../../app/src/main/java/it/edo/codecprobe/MediaDestinationsController.java).

**Preparazione, file e simulazioni condivisi:** Directory ContentProvider inventato, grant Robolectric; Harness Host restituisce busy=false e persist/toast vuoti; prefs simulate.

**Confini comuni dell’audit:** Non verifica filesystem o permessi reali. Polling 5 ms/3–5 s; direct field plan e Harness più ristretto dell'app.

**Metodi:** [choicesFollowSourceUrisAndOverrideDoesNotEraseThem](#test-4e24ed6b6670c45b) · [serviceBoundaryCopiesPerFileFoldersAndRejectsMissingEntries](#test-b7f54085a0214266) · [samePanelHidesEmptySelectionVerifiesAccessAndRestoresPendingPicker](#test-866917d64a095481)

<!-- test: MediaDestinationsTest.choicesFollowSourceUrisAndOverrideDoesNotEraseThem -->
<a id="test-4e24ed6b6670c45b"></a>
### `MediaDestinationsTest.choicesFollowSourceUrisAndOverrideDoesNotEraseThem`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Directory ContentProvider inventato, grant Robolectric; Harness Host restituisce busy=false e persist/toast vuoti; prefs simulate.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/MediaDestinationsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `draft.sync`, `draft.plan.setFolder`, `draft.plan.setAllFolder`, `draft.plan.effective`, `draft.save`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Scelte seguono URI anche riordinando; override globale reversibile, save/load prefs e reset origine.

**Limiti:** Non verifica filesystem o permessi reali. Polling 5 ms/3–5 s; direct field plan e Harness più ristretto dell'app.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void choicesFollowSourceUrisAndOverrideDoesNotEraseThem(){
        DestinationDraft draft=new DestinationDraft();draft.sync(Arrays.asList("one","two"),A);draft.plan.setFolder(1,B);
        draft.plan.applyAll=true;draft.plan.setAllFolder("global");assertEquals("global",draft.plan.effective(1).folder);
        draft.plan.applyAll=false;assertEquals(B,draft.plan.effective(1).folder);draft.sync(Arrays.asList("one","two"),"new-default");assertEquals(A,draft.plan.effective(0).folder);assertEquals(B,draft.plan.effective(1).folder);
        draft.sync(Arrays.asList("two","one"),A);assertEquals(B,draft.plan.effective(0).folder);assertEquals(A,draft.plan.effective(1).folder);
        var prefs=RuntimeEnvironment.getApplication().getSharedPreferences("destination-test",0);var edit=prefs.edit();draft.save(edit,Arrays.asList("two","one"),A);edit.commit();
        DestinationDraft restored=new DestinationDraft();restored.load(prefs,Arrays.asList("two","one"),null);assertEquals(B,restored.plan.effective(0).folder);
        restored.sync(Collections.singletonList("one"),A);assertEquals(A,restored.plan.effective(0).folder);
        restored.reset(Collections.singletonList("new"),null);assertTrue(restored.plan.effective(0).origin);
        prefs.edit().clear().commit();
    }
```

</details>

<!-- /test -->

<!-- test: MediaDestinationsTest.serviceBoundaryCopiesPerFileFoldersAndRejectsMissingEntries -->
<a id="test-b7f54085a0214266"></a>
### `MediaDestinationsTest.serviceBoundaryCopiesPerFileFoldersAndRejectsMissingEntries`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Directory ContentProvider inventato, grant Robolectric; Harness Host restituisce busy=false e persist/toast vuoti; prefs simulate.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/MediaDestinationsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `QueueDestinations.read`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** QueueDestinations copia lista folders e rifiuta cardinalità/fallback mancanti; nessun servizio avviato.

**Limiti:** Non verifica filesystem o permessi reali. Polling 5 ms/3–5 s; direct field plan e Harness più ristretto dell'app.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void serviceBoundaryCopiesPerFileFoldersAndRejectsMissingEntries(){
        ArrayList<String> folders=new ArrayList<>(Arrays.asList(A,B));Intent task=new Intent().putStringArrayListExtra("folders",folders);
        ArrayList<String> snapshot=QueueDestinations.read(task,2,null);folders.set(0,"changed");assertEquals(A,snapshot.get(0));
        assertEquals(Arrays.asList(A,A),QueueDestinations.read(new Intent(),2,A));
        try{QueueDestinations.read(task,1,null);fail();}catch(IllegalArgumentException expected){}
        try{QueueDestinations.read(new Intent(),2,null);fail();}catch(IllegalArgumentException expected){}
    }
```

</details>

<!-- /test -->

<!-- test: MediaDestinationsTest.samePanelHidesEmptySelectionVerifiesAccessAndRestoresPendingPicker -->
<a id="test-866917d64a095481"></a>
### `MediaDestinationsTest.samePanelHidesEmptySelectionVerifiesAccessAndRestoresPendingPicker`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Directory ContentProvider inventato, grant Robolectric; Harness Host restituisce busy=false e persist/toast vuoti; prefs simulate.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/MediaDestinationsTest.java).

**Metodi applicativi e percorso:** Harness View → MediaDestinationsController → OriginDirectoryPanel, che presenta lo snapshot di OriginDirectoryAccess; grant(…) e settle(…) preparano/attendono la verifica. Il contratto View rimane invariato dopo l’estrazione della logica condivisa.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Pannello vuoto nascosto, verifica grant, picker pendente restaurato, applyAll e revoca simulata rendono non ready.

**Limiti:** Non verifica filesystem o permessi reali. Polling 5 ms/3–5 s; direct field plan e Harness più ristretto dell'app.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void samePanelHidesEmptySelectionVerifiesAccessAndRestoresPendingPicker()throws Exception{
        ShadowContentResolver.registerProviderInternal("destinations",new Directories());
        var activity=Robolectric.buildActivity(Activity.class).setup();
        Harness host=new Harness(activity.get());
        try{
            host.controller.render();assertEquals(View.GONE,host.view.body.getVisibility());assertFalse(host.controller.ready());
            host.inputs.addAll(Arrays.asList("content://songs/one","content://songs/two"));host.names.addAll(Arrays.asList("one.flac","two.flac"));grant(activity.get(),A);
            host.controller.render();settle(host);assertEquals(View.VISIBLE,host.view.body.getVisibility());assertEquals(Arrays.asList(A,A),host.controller.snapshot());
            host.view.pickFolder.performClick();assertTrue(host.controller.choosing());assertFalse(host.view.pickFolder.isEnabled());
            Bundle pending=new Bundle();host.controller.save(pending);host.controller.close();
            host.selected=1;host.controller=new MediaDestinationsController(host,host.view,MediaKind.AUDIO,pending);
            host.controller.result(MediaDestinationsController.FILE,Activity.RESULT_OK,new Intent().setData(Uri.parse(B)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            settle(host);assertEquals(Arrays.asList(B,A),host.controller.snapshot());
            host.view.applyAllDestinations.performClick();assertFalse(host.view.pickFolder.isEnabled());assertTrue(host.view.pickAllFolder.isEnabled());
            host.view.applyAllDestinations.performClick();settle(host);assertEquals(B,host.draft.plan.effective(0).folder);
            activity.get().getContentResolver().releasePersistableUriPermission(Uri.parse(B),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);int before=host.changes;host.controller.refresh();
            long end=System.currentTimeMillis()+3000;while(host.changes<before+2&&System.currentTimeMillis()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}
            assertTrue(host.changes>=before+2);assertFalse(host.controller.ready());
        }finally{host.controller.close();activity.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-mp4metadatadiagnosticstest"></a>
## Mp4MetadataDiagnosticsTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/Mp4MetadataDiagnosticsTest.java) · 1 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** Mp4Metadata.check.

**Riferimenti produttivi:** [Mp4Metadata](../../app/src/main/java/it/edo/codecprobe/Mp4Metadata.java).

**Preparazione, file e simulazioni condivisi:** File reali con soli box ftyp/mdat/moov vuoti.

**Confini comuni dell’audit:** Verifica rifiuto/diagnostica strutturale, non correttezza di un moov completo o riproducibilità.

**Metodi:** [missingAndDuplicateMoovHaveCountsSizeAndRootLayout](#test-6fa3ee1d465cc209)

<!-- test: Mp4MetadataDiagnosticsTest.missingAndDuplicateMoovHaveCountsSizeAndRootLayout -->
<a id="test-6fa3ee1d465cc209"></a>
### `Mp4MetadataDiagnosticsTest.missingAndDuplicateMoovHaveCountsSizeAndRootLayout`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): File reali con soli box ftyp/mdat/moov vuoti.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/Mp4MetadataDiagnosticsTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `rejected(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** moov mancante o duplicato segnala numero, dimensione file e layout/offset dei box.

**Limiti:** Verifica rifiuto/diagnostica strutturale, non correttezza di un moov completo o riproducibilità.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void missingAndDuplicateMoovHaveCountsSizeAndRootLayout()throws Exception{
        String missing=rejected("ftyp","mdat");assertTrue(missing.contains("trovati 0"));assertTrue(missing.contains("16 byte"));assertTrue(missing.contains("mdat@offset=8 byte=8"));
        String duplicate=rejected("ftyp","moov","moov");assertTrue(duplicate.contains("trovati 2"));assertTrue(duplicate.contains("24 byte"));assertTrue(duplicate.contains("moov@offset=16"));
    }
```

</details>

<!-- /test -->


<a id="class-mp4metadatainventorytest"></a>
## Mp4MetadataInventoryTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java) · 8 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** Mp4MetadataInventory.write/digest; SamsungSef fixture.

**Riferimenti produttivi:** [Mp4MetadataInventory](../../app/src/main/java/it/edo/codecprobe/Mp4MetadataInventory.java), [SamsungSef](../../app/src/main/java/it/edo/codecprobe/SamsungSef.java).

**Preparazione, file e simulazioni condivisi:** Veri file temporanei con box generati, payload opaco, SEF sintetico, Random a seed fisso e file logico 64 MiB.

**Confini comuni dell’audit:** Nessun video realmente decodificato. Il digest atteso talvolta usa lo stesso metodo del codice sotto test: non è un oracolo indipendente del SHA.

**Metodi:** [datesVersionZeroAndOneAndNestedTextAreReadable](#test-e2241b3e340358e2) · [opaqueMetadataHashSurvivesRelocationAndPreviewIsBounded](#test-90eeea1eb556b475) · [sparseMediaIsSkippedAndMalformedOrCancelledReportsFailClearly](#test-2b4b28148213f4b8) · [unknownEditorRecordsExportEveryByteAndCompleteJson](#test-7f4f4bb2e995134c) · [malformedSefStillExportsExactBytesAndLargeBlocksHaveExplicitLimit](#test-ec64e9383e753b65) · [completeSefExportRemainsCancellableBetweenChunks](#test-6426f03db6d46d15) · [quickTimeMetaWithoutFullBoxFlagsCannotHideFollowingSef](#test-f6eeddf145e95488) · [malformedNestedMetadataStillLeavesCompleteRootSefAvailable](#test-4f3bd3bf78204b2c)

<!-- test: Mp4MetadataInventoryTest.datesVersionZeroAndOneAndNestedTextAreReadable -->
<a id="test-e2241b3e340358e2"></a>
### `Mp4MetadataInventoryTest.datesVersionZeroAndOneAndNestedTextAreReadable`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Veri file temporanei con box generati, payload opaco, SEF sintetico, Random a seed fisso e file logico 64 MiB.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `join(…)`, `box(…)`, `report(…)`, `date(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Date mvhd v0/v1 e testo QuickTime UTF-8 esposti; payload mdat non esportato.

**Limiti:** Nessun video realmente decodificato. Il digest atteso talvolta usa lo stesso metodo del codice sotto test: non è un oracolo indipendente del SHA.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void datesVersionZeroAndOneAndNestedTextAreReadable()throws Exception{
        for(int version:new int[]{0,1}){
            byte[] value=join(ByteBuffer.allocate(8).putInt(1).putInt(0).array(),"2026-10-17T10:00:00+02:00".getBytes(StandardCharsets.UTF_8));
            byte[] metadata=box("meta",join(new byte[4],box("keys",join(ByteBuffer.allocate(8).putInt(0).putInt(1).array(),box("mdta","com.apple.quicktime.creationdate".getBytes(StandardCharsets.UTF_8)))),box("ilst",box("©day",box("data",value)))));
            String text=report(join(box("ftyp",new byte[8]),box("moov",join(date(version,3900000000L),box("udta",metadata))),box("mdat","NOT_VIDEO_EXPORT".getBytes())));
            assertTrue(text.contains("CREAZIONE · secondi dal 1904=3900000000"));assertTrue(text.contains("MODIFICA · secondi dal 1904=3900000005"));
            assertTrue(text.contains("versione="+version));assertTrue(text.contains("com.apple.quicktime.creationdate"));assertTrue(text.contains("VALORE UTF-8=2026-10-17T10:00:00+02:00"));
            assertTrue(text.contains("SHA256 PAYLOAD="));assertFalse(text.contains("NOT_VIDEO_EXPORT"));
        }
    }
```

</details>

<!-- /test -->

<!-- test: Mp4MetadataInventoryTest.opaqueMetadataHashSurvivesRelocationAndPreviewIsBounded -->
<a id="test-90eeea1eb556b475"></a>
### `Mp4MetadataInventoryTest.opaqueMetadataHashSurvivesRelocationAndPreviewIsBounded`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Veri file temporanei con box generati, payload opaco, SEF sintetico, Random a seed fisso e file logico 64 MiB.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `Mp4MetadataInventory.digest`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Hash opaco invariato dopo spostamento e anteprima 256 byte; digest non muove il ByteBuffer.

**Limiti:** Nessun video realmente decodificato. Il digest atteso talvolta usa lo stesso metodo del codice sotto test: non è un oracolo indipendente del SHA.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void opaqueMetadataHashSurvivesRelocationAndPreviewIsBounded()throws Exception{
        byte[] value=new byte[1024];Arrays.fill(value,(byte)'X');
        String first=report(box("uuid",value));String second=report(join(box("free",new byte[32]),box("uuid",value)));
        String expected=Mp4MetadataInventory.digest(ByteBuffer.wrap(value));assertTrue(first.contains(expected));assertTrue(second.contains(expected));
        assertTrue(first.contains("primi 256 / 1024 byte"));assertTrue(first.length()<2500);
        ByteBuffer buffer=ByteBuffer.wrap(value);buffer.position(10);int position=buffer.position();Mp4MetadataInventory.digest(buffer);assertEquals(position,buffer.position());
    }
```

</details>

<!-- /test -->

<!-- test: Mp4MetadataInventoryTest.sparseMediaIsSkippedAndMalformedOrCancelledReportsFailClearly -->
<a id="test-2b4b28148213f4b8"></a>
### `Mp4MetadataInventoryTest.sparseMediaIsSkippedAndMalformedOrCancelledReportsFailClearly`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Veri file temporanei con box generati, payload opaco, SEF sintetico, Random a seed fisso e file logico 64 MiB.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `Mp4MetadataInventory.write`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** mdat 64 MiB saltato con report corto; cancellazione e box con size invalida falliscono.

**Limiti:** Nessun video realmente decodificato. Il digest atteso talvolta usa lo stesso metodo del codice sotto test: non è un oracolo indipendente del SHA.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void sparseMediaIsSkippedAndMalformedOrCancelledReportsFailClearly()throws Exception{
        Path path=Files.createTempFile("metadata-large-mdat-",".mp4");
        try(RandomAccessFile file=new RandomAccessFile(path.toFile(),"rw")){
            file.setLength(64L*1024*1024);file.writeInt(0);file.writeBytes("mdat");
            StringWriter out=new StringWriter();Mp4MetadataInventory.write(file.getChannel(),out,()->false);
            assertTrue(out.toString().contains("CONTENUTO MULTIMEDIALE SALTATO"));assertTrue(out.toString().length()<800);
            try{Mp4MetadataInventory.write(file.getChannel(),new StringWriter(),()->true);fail();}catch(InterruptedIOException expected){}
        }finally{Files.deleteIfExists(path);}
        try{report(ByteBuffer.allocate(8).putInt(1000).put("udta".getBytes()).array());fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("Dimensione blocco non valida"));}
    }
```

</details>

<!-- /test -->

<!-- test: Mp4MetadataInventoryTest.unknownEditorRecordsExportEveryByteAndCompleteJson -->
<a id="test-7f4f4bb2e995134c"></a>
### `Mp4MetadataInventoryTest.unknownEditorRecordsExportEveryByteAndCompleteJson`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Veri file temporanei con box generati, payload opaco, SEF sintetico, Random a seed fisso e file logico 64 MiB.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `Mp4MetadataInventory.digest`, `Mp4MetadataInventory.escape`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Export SEF sconosciuto conserva tutti i byte/base64 e JSON completo, non i media.

**Limiti:** Nessun video realmente decodificato. Il digest atteso talvolta usa lo stesso metodo del codice sotto test: non è un oracolo indipendente del SHA.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void unknownEditorRecordsExportEveryByteAndCompleteJson()throws Exception{
        String json="{\"NDE_VERSION\":\"2.0\",\"data\":\""+"é\\\"".repeat(400)+"END_OF_COMPLETE_JSON\"}";
        byte[] binary=new byte[2501];new Random(8241).nextBytes(binary);
        byte[] sef=SamsungSefMotionFixtures.fixture(107,
            new SamsungSefMotionFixtures.Item(0x0ba1,"VideoEditor_Re_Edit_Data",json.getBytes(StandardCharsets.UTF_8)),
            new SamsungSefMotionFixtures.Item(0x0ba1,"Unknown_Binary_Metadata",binary));
        String report=report(join(box("mdat","DO_NOT_EXPORT_VIDEO".getBytes(StandardCharsets.UTF_8)),sef));
        assertArrayEquals(sef,exportedSef(report));
        assertTrue(report.contains("SHA256="+Mp4MetadataInventory.digest(ByteBuffer.wrap(sef))));
        assertTrue(report.contains("SEF TESTO COMPLETO="+Mp4MetadataInventory.escape(json)));
        assertTrue(report.contains("END_OF_COMPLETE_JSON"));assertFalse(report.contains("DO_NOT_EXPORT_VIDEO"));
        assertTrue(report.contains("*** FINE SEF BLOCCO COMPLETO ***"));
    }
```

</details>

<!-- /test -->

<!-- test: Mp4MetadataInventoryTest.malformedSefStillExportsExactBytesAndLargeBlocksHaveExplicitLimit -->
<a id="test-ec64e9383e753b65"></a>
### `Mp4MetadataInventoryTest.malformedSefStillExportsExactBytesAndLargeBlocksHaveExplicitLimit`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Veri file temporanei con box generati, payload opaco, SEF sintetico, Random a seed fisso e file logico 64 MiB.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `box(…)`, `report(…)`, `exportedSef(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** SEF malformato esportato opaco; blocco oltre limite esplicitamente non esportato.

**Limiti:** Nessun video realmente decodificato. Il digest atteso talvolta usa lo stesso metodo del codice sotto test: non è un oracolo indipendente del SHA.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void malformedSefStillExportsExactBytesAndLargeBlocksHaveExplicitLimit()throws Exception{
        byte[] malformed=box("sefd",new byte[800]);String report=report(malformed);
        assertArrayEquals(malformed,exportedSef(report));assertTrue(report.contains("SEF NON INTERPRETABILE"));
        byte[] large=box("sefd",new byte[SamsungSef.MAX_BYTES]);String limited=report(large);
        assertEquals(0,exportedSef(limited).length);assertTrue(limited.contains("blocco completo non esportato"));
        assertFalse(limited.contains("*** SEF BLOCCO COMPLETO"));
    }
```

</details>

<!-- /test -->

<!-- test: Mp4MetadataInventoryTest.completeSefExportRemainsCancellableBetweenChunks -->
<a id="test-6426f03db6d46d15"></a>
### `Mp4MetadataInventoryTest.completeSefExportRemainsCancellableBetweenChunks`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Veri file temporanei con box generati, payload opaco, SEF sintetico, Random a seed fisso e file logico 64 MiB.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `Mp4MetadataInventory.write`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Cancel dopo primo chunk impedisce marcatore di fine export completo.

**Limiti:** Nessun video realmente decodificato. Il digest atteso talvolta usa lo stesso metodo del codice sotto test: non è un oracolo indipendente del SHA.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void completeSefExportRemainsCancellableBetweenChunks()throws Exception{
        Path path=Files.createTempFile("sef-export-cancel-",".mp4");StringWriter out=new StringWriter();
        try{
            Files.write(path,box("sefd",new byte[3000]));
            try(FileChannel file=FileChannel.open(path)){
                try{Mp4MetadataInventory.write(file,out,()->out.toString().contains("SEF BASE64 0="));fail();}
                catch(InterruptedIOException expected){}
            }
            assertTrue(out.toString().contains("SEF BASE64 0="));assertFalse(out.toString().contains("*** FINE SEF BLOCCO COMPLETO ***"));
        }finally{Files.deleteIfExists(path);}
    }
```

</details>

<!-- /test -->

<!-- test: Mp4MetadataInventoryTest.quickTimeMetaWithoutFullBoxFlagsCannotHideFollowingSef -->
<a id="test-f6eeddf145e95488"></a>
### `Mp4MetadataInventoryTest.quickTimeMetaWithoutFullBoxFlagsCannotHideFollowingSef`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Veri file temporanei con box generati, payload opaco, SEF sintetico, Random a seed fisso e file logico 64 MiB.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `box(…)`, `join(…)`, `report(…)`, `exportedSef(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Meta QuickTime privo di flag FullBox non nasconde SEF successivo.

**Limiti:** Nessun video realmente decodificato. Il digest atteso talvolta usa lo stesso metodo del codice sotto test: non è un oracolo indipendente del SHA.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void quickTimeMetaWithoutFullBoxFlagsCannotHideFollowingSef()throws Exception{
        byte[] editor=SamsungSefMotionFixtures.fixture(107,new SamsungSefMotionFixtures.Item(0x0ba1,"VideoEditor_Re_Edit_Data","{\"NDE_VERSION\":\"2.0\",\"payload\":\""+"A".repeat(1400)+"\"}"));
        byte[] meta=box("meta",join(box("hdlr",new byte[24]),box("keys",new byte[8])));
        String report=report(join(box("ftyp",new byte[8]),box("mdat",new byte[64]),box("moov",meta),editor));
        assertArrayEquals(editor,exportedSef(report));assertTrue(report.contains("META QUICKTIME"));
        assertTrue(report.contains("root/moov[1]/meta[1]/hdlr[1]"));
        assertFalse(report.contains("NON INTERPRETABILE"));
    }
```

</details>

<!-- /test -->

<!-- test: Mp4MetadataInventoryTest.malformedNestedMetadataStillLeavesCompleteRootSefAvailable -->
<a id="test-4f3bd3bf78204b2c"></a>
### `Mp4MetadataInventoryTest.malformedNestedMetadataStillLeavesCompleteRootSefAvailable`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Veri file temporanei con box generati, payload opaco, SEF sintetico, Random a seed fisso e file logico 64 MiB.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/Mp4MetadataInventoryTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `Mp4MetadataInventory.write`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Errore nested dopo il footer non impedisce l'export completo del SEF alla radice.

**Limiti:** Nessun video realmente decodificato. Il digest atteso talvolta usa lo stesso metodo del codice sotto test: non è un oracolo indipendente del SHA.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void malformedNestedMetadataStillLeavesCompleteRootSefAvailable()throws Exception{
        byte[] editor=SamsungSefMotionFixtures.fixture(107,new SamsungSefMotionFixtures.Item(0x0ba1,"VideoEditor_Re_Edit_Data","{\"unknown\":true}"));
        byte[] malformedMeta=box("meta",join(new byte[4],ByteBuffer.allocate(8).putInt(1000).put("hdlr".getBytes(StandardCharsets.US_ASCII)).array()));
        Path path=Files.createTempFile("sef-before-malformed-meta-",".mp4");StringWriter out=new StringWriter();
        try{
            Files.write(path,join(box("moov",malformedMeta),editor));
            try(FileChannel file=FileChannel.open(path)){
                try{Mp4MetadataInventory.write(file,out,()->false);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("Dimensione blocco non valida"));}
            }
            assertArrayEquals(editor,exportedSef(out.toString()));
            assertTrue(out.toString().contains("*** FINE SEF BLOCCO COMPLETO ***"));
        }finally{Files.deleteIfExists(path);}
    }
```

</details>

<!-- /test -->


<a id="class-outputcollisiontest"></a>
## OutputCollisionTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java) · 6 metodi · area originaria: File e cartelle.

**Contesto applicativo della classe:** HomeOutputCheck.begin; OutputName.base; dialog rename/skip/cancel; AppUi.

**Riferimenti produttivi:** [HomeOutputCheck](../../app/src/main/java/it/edo/codecprobe/HomeOutputCheck.java), [OutputName](../../app/src/main/java/it/edo/codecprobe/OutputName.java), [AppUi](../../app/src/main/java/it/edo/codecprobe/AppUi.java).

**Preparazione, file e simulazioni condivisi:** Provider Directory restituisce un nome già esistente, computed con stessa OutputName.base usata in produzione; Activity/dialog/Intent shadow.

**Confini comuni dell’audit:** Non pubblica file né prova collisione comparsa tra preflight e save. Nome skipRecordsDecisionAndCancelDoesNotLaunch verifica solo Skip; Cancel è in altro test. Due PNG senza golden.

**Metodi:** [renameValidatesNameBeforeStartingService](#test-7a69154137cd267d) · [skipRecordsDecisionAndCancelDoesNotLaunch](#test-061006848253d64e) · [backCancelsPreflightWithoutConversion](#test-a9599e3b608f2601) · [sharedButtonsHaveNoPlatformElevationAnimator](#test-f5c197ea72b584e0) · [renderLightCollisionDialog](#test-684bfecb5f017acb) · [renderDarkCollisionDialog](#test-78a16f75a4dd059e)

<!-- test: OutputCollisionTest.renameValidatesNameBeforeStartingService -->
<a id="test-7a69154137cd267d"></a>
### `OutputCollisionTest.renameValidatesNameBeforeStartingService`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Directory restituisce un nome già esistente, computed con stessa OutputName.base usata in produzione; Activity/dialog/Intent shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `OutputName.base`, `home.activity.getApplication`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Nome già occupato mostra errore e conserva dialog; rinominato emette Intent outputNames rinominato.mp4 non omitted.

**Limiti:** Non pubblica file né prova collisione comparsa tra preflight e save. Nome skipRecordsDecisionAndCancelDoesNotLaunch verifica solo Skip; Cancel è in altro test. Due PNG senza golden.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void renameValidatesNameBeforeStartingService() throws Exception {
        begin();AlertDialog dialog=home.conversion.outputCheck.dialog;
        EditText name=input(dialog.getWindow().getDecorView());assertNotNull(name);
        AppSettings settings=new AppSettings(home.activity);name.setText(OutputName.base("clip.mp4",settings.addition,settings.prefix));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertNotNull(name.getError());assertTrue(dialog.isShowing());
        name.setText("rinominato");dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Intent started=Shadows.shadowOf((android.app.Application)home.activity.getApplication()).getNextStartedService();
        assertNotNull(started);assertEquals("rinominato.mp4",started.getStringArrayListExtra("outputNames").get(0));
        assertFalse(started.getBooleanArrayExtra("omitted")[0]);assertFalse(home.conversion.outputCheck.checking);
    }
```

</details>

<!-- /test -->

<!-- test: OutputCollisionTest.skipRecordsDecisionAndCancelDoesNotLaunch -->
<a id="test-061006848253d64e"></a>
### `OutputCollisionTest.skipRecordsDecisionAndCancelDoesNotLaunch`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Directory restituisce un nome già esistente, computed con stessa OutputName.base usata in produzione; Activity/dialog/Intent shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.conversion.outputCheck.dialog.getButton`, `home.activity.getApplication`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Pulsante Skip marca omitted nel servizio lanciato; non esercita Cancel nonostante nome.

**Limiti:** Non pubblica file né prova collisione comparsa tra preflight e save. Nome skipRecordsDecisionAndCancelDoesNotLaunch verifica solo Skip; Cancel è in altro test. Due PNG senza golden.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void skipRecordsDecisionAndCancelDoesNotLaunch() throws Exception {
        begin();home.conversion.outputCheck.dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        Intent started=Shadows.shadowOf((android.app.Application)home.activity.getApplication()).getNextStartedService();
        assertTrue(started.getBooleanArrayExtra("omitted")[0]);
    }
```

</details>

<!-- /test -->

<!-- test: OutputCollisionTest.backCancelsPreflightWithoutConversion -->
<a id="test-a9599e3b608f2601"></a>
### `OutputCollisionTest.backCancelsPreflightWithoutConversion`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Directory restituisce un nome già esistente, computed con stessa OutputName.base usata in produzione; Activity/dialog/Intent shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.conversion.outputCheck.dialog.cancel`, `home.activity.getApplication`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Back/cancel chiude preflight, niente Intent conversione e busy=false.

**Limiti:** Non pubblica file né prova collisione comparsa tra preflight e save. Nome skipRecordsDecisionAndCancelDoesNotLaunch verifica solo Skip; Cancel è in altro test. Due PNG senza golden.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void backCancelsPreflightWithoutConversion() throws Exception {
        begin();home.conversion.outputCheck.dialog.cancel();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertFalse(home.conversion.outputCheck.checking);
        assertNull(Shadows.shadowOf((android.app.Application)home.activity.getApplication()).getNextStartedService());
        assertFalse(home.state.busy);
    }
```

</details>

<!-- /test -->

<!-- test: OutputCollisionTest.sharedButtonsHaveNoPlatformElevationAnimator -->
<a id="test-f5c197ea72b584e0"></a>
### `OutputCollisionTest.sharedButtonsHaveNoPlatformElevationAnimator`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Directory restituisce un nome già esistente, computed con stessa OutputName.base usata in produzione; Activity/dialog/Intent shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.screen.conversion.convert.getStateListAnimator`, `home.screen.conversion.convert.getElevation`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Solo bottone Convert controllato senza elevation/stateListAnimator, non tutti sharedButtons.

**Limiti:** Non pubblica file né prova collisione comparsa tra preflight e save. Nome skipRecordsDecisionAndCancelDoesNotLaunch verifica solo Skip; Cancel è in altro test. Due PNG senza golden.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void sharedButtonsHaveNoPlatformElevationAnimator(){
        assertNull(home.screen.conversion.convert.getStateListAnimator());assertEquals(0f,home.screen.conversion.convert.getElevation(),0f);
    }
```

</details>

<!-- /test -->

<!-- test: OutputCollisionTest.renderLightCollisionDialog -->
<a id="test-684bfecb5f017acb"></a>
### `OutputCollisionTest.renderLightCollisionDialog`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Directory restituisce un nome già esistente, computed con stessa OutputName.base usata in produzione; Activity/dialog/Intent shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `renderDialog(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** PNG dialog collisione chiaro, solo compressione.

**Limiti:** Non pubblica file né prova collisione comparsa tra preflight e save. Nome skipRecordsDecisionAndCancelDoesNotLaunch verifica solo Skip; Cancel è in altro test. Due PNG senza golden.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void renderLightCollisionDialog()throws Exception{renderDialog("home-collision.png");}
```

</details>

<!-- /test -->

<!-- test: OutputCollisionTest.renderDarkCollisionDialog -->
<a id="test-78a16f75a4dd059e"></a>
### `OutputCollisionTest.renderDarkCollisionDialog`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Directory restituisce un nome già esistente, computed con stessa OutputName.base usata in produzione; Activity/dialog/Intent shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputCollisionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `renderDialog(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** PNG dialog collisione scuro, solo compressione.

**Limiti:** Non pubblica file né prova collisione comparsa tra preflight e save. Nome skipRecordsDecisionAndCancelDoesNotLaunch verifica solo Skip; Cancel è in altro test. Due PNG senza golden.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test @Config(qualifiers="w360dp-h800dp-night-xhdpi") public void renderDarkCollisionDialog()throws Exception{renderDialog("home-collision-dark.png");}
```

</details>

<!-- /test -->


<a id="class-outputnamingsafetytest"></a>
## OutputNamingSafetyTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java) · 7 metodi · area originaria: File e cartelle.

**Contesto applicativo della classe:** OutputPlan; OutputName.base/unique.

**Riferimenti produttivi:** [OutputPlan](../../app/src/main/java/it/edo/codecprobe/OutputPlan.java), [OutputName](../../app/src/main/java/it/edo/codecprobe/OutputName.java).

**Preparazione, file e simulazioni condivisi:** Set/array in memoria; locale globale tr-TR ripristinata in finally.

**Confini comuni dell’audit:** Non apre file né prova restrizioni del filesystem/provider. Il test rejectedManualName invoca manual prima di accept: non simula un accept che fallisce.

**Metodi:** [emptyDirectoryAndEmptyQueueDoNotInventCollisions](#test-2b8448aaae4067ce) · [reservationsDoNotMutateTheProviderListingOrAnotherDirectory](#test-a437e29f27678c4a) · [firstAvailableSuffixIsChosenWithoutModifyingReservations](#test-24042c829ec7a758) · [collisionComparisonDoesNotDependOnTurkishDefaultLocale](#test-3c2c56a498119cca) · [manualUtf8LimitAccepts240BytesAndRejects242Bytes](#test-5312690aaae6a4b9) · [generatedNamesKeepWholeUnicodeCodePointsAndRemoveTraversal](#test-d132713c761b2226) · [rejectedManualNameCannotConsumeAQueueSlot](#test-41397aed9d37b511)

<!-- test: OutputNamingSafetyTest.emptyDirectoryAndEmptyQueueDoNotInventCollisions -->
<a id="test-2b8448aaae4067ce"></a>
### `OutputNamingSafetyTest.emptyDirectoryAndEmptyQueueDoNotInventCollisions`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Set/array in memoria; locale globale tr-TR ripristinata in finally.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `plan.directory`, `plan.conflicts`, `plan.suggestion`, `plan.names.isEmpty`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Coda zero e directory vuota non inventano collisioni.

**Limiti:** Non apre file né prova restrizioni del filesystem/provider. Il test rejectedManualName invoca manual prima di accept: non simula un accept che fallisce.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void emptyDirectoryAndEmptyQueueDoNotInventCollisions() {
        OutputPlan plan=new OutputPlan(0);plan.directory("empty",Set.of());
        assertFalse(plan.conflicts("empty","clip.mp4"));assertEquals("clip.mp4",plan.suggestion("empty","clip.mp4"));
        assertTrue(plan.names.isEmpty());assertEquals(0,plan.omitted.length);
    }
```

</details>

<!-- /test -->

<!-- test: OutputNamingSafetyTest.reservationsDoNotMutateTheProviderListingOrAnotherDirectory -->
<a id="test-a437e29f27678c4a"></a>
### `OutputNamingSafetyTest.reservationsDoNotMutateTheProviderListingOrAnotherDirectory`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Set/array in memoria; locale globale tr-TR ripristinata in finally.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `plan.directory`, `plan.accept`, `plan.conflicts`, `plan.suggestion`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Copia del listing non aliasata, cancellare il Set esterno non perde i nomi; prenotazioni isolate per directory.

**Limiti:** Non apre file né prova restrizioni del filesystem/provider. Il test rejectedManualName invoca manual prima di accept: non simula un accept che fallisce.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void reservationsDoNotMutateTheProviderListingOrAnotherDirectory() {
        Set<String> listing=new HashSet<>(Set.of("CLIP.MP4"));OutputPlan plan=new OutputPlan(2);
        plan.directory("one",listing);plan.directory("two",listing);listing.clear();
        plan.accept("one","clip (2).mp4");
        assertTrue(plan.conflicts("one","clip.mp4"));assertTrue(listing.isEmpty());
        assertFalse(plan.conflicts("two","clip (2).mp4"));assertEquals("clip (3).mp4",plan.suggestion("one","clip.mp4"));
    }
```

</details>

<!-- /test -->

<!-- test: OutputNamingSafetyTest.firstAvailableSuffixIsChosenWithoutModifyingReservations -->
<a id="test-24042c829ec7a758"></a>
### `OutputNamingSafetyTest.firstAvailableSuffixIsChosenWithoutModifyingReservations`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Set/array in memoria; locale globale tr-TR ripristinata in finally.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `OutputName.unique`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Usa il primo buco (3), non modifica il Set occupato e ripete la stessa proposta.

**Limiti:** Non apre file né prova restrizioni del filesystem/provider. Il test rejectedManualName invoca manual prima di accept: non simula un accept che fallisce.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void firstAvailableSuffixIsChosenWithoutModifyingReservations() {
        Set<String> occupied=Set.of("film.mp4","film (2).mp4","film (4).mp4");
        assertEquals("film (3).mp4",OutputName.unique("film.mp4",occupied));
        assertEquals(3,occupied.size());assertEquals("film (3).mp4",OutputName.unique("film.mp4",occupied));
    }
```

</details>

<!-- /test -->

<!-- test: OutputNamingSafetyTest.collisionComparisonDoesNotDependOnTurkishDefaultLocale -->
<a id="test-3c2c56a498119cca"></a>
### `OutputNamingSafetyTest.collisionComparisonDoesNotDependOnTurkishDefaultLocale`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Set/array in memoria; locale globale tr-TR ripristinata in finally.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `plan.directory`, `plan.conflicts`, `plan.suggestion`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** INDICE/indice confliggono anche con locale turca.

**Limiti:** Non apre file né prova restrizioni del filesystem/provider. Il test rejectedManualName invoca manual prima di accept: non simula un accept che fallisce.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void collisionComparisonDoesNotDependOnTurkishDefaultLocale() {
        Locale before=Locale.getDefault();
        try { Locale.setDefault(Locale.forLanguageTag("tr-TR"));OutputPlan plan=new OutputPlan(1);
            plan.directory("one",Set.of("INDICE.MP4"));assertTrue(plan.conflicts("one","indice.mp4"));
            assertEquals("indice (2).mp4",plan.suggestion("one","indice.mp4"));
        } finally { Locale.setDefault(before); }
    }
```

</details>

<!-- /test -->

<!-- test: OutputNamingSafetyTest.manualUtf8LimitAccepts240BytesAndRejects242Bytes -->
<a id="test-5312690aaae6a4b9"></a>
### `OutputNamingSafetyTest.manualUtf8LimitAccepts240BytesAndRejects242Bytes`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Set/array in memoria; locale globale tr-TR ripristinata in finally.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `OutputPlan.manual`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** 240 byte UTF-8 accettati, 242 rifiutati.

**Limiti:** Non apre file né prova restrizioni del filesystem/provider. Il test rejectedManualName invoca manual prima di accept: non simula un accept che fallisce.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void manualUtf8LimitAccepts240BytesAndRejects242Bytes() {
        String boundary="à".repeat(118)+".mp4";assertEquals(240,boundary.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(boundary,OutputPlan.manual(boundary));
        assertThrows(IllegalArgumentException.class,()->OutputPlan.manual("à".repeat(119)+".mp4"));
    }
```

</details>

<!-- /test -->

<!-- test: OutputNamingSafetyTest.generatedNamesKeepWholeUnicodeCodePointsAndRemoveTraversal -->
<a id="test-d132713c761b2226"></a>
### `OutputNamingSafetyTest.generatedNamesKeepWholeUnicodeCodePointsAndRemoveTraversal`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Set/array in memoria; locale globale tr-TR ripristinata in finally.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `OutputName.base`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Sanitizzazione traversal e troncamento non spezzano emoji; round-trip UTF-8 e limite 224 byte.

**Limiti:** Non apre file né prova restrizioni del filesystem/provider. Il test rejectedManualName invoca manual prima di accept: non simula un accept che fallisce.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void generatedNamesKeepWholeUnicodeCodePointsAndRemoveTraversal() {
        String result=OutputName.base("🌈".repeat(100)+".mov","../",true);
        assertTrue(result.endsWith(".mp4"));assertTrue(result.getBytes(StandardCharsets.UTF_8).length<=224);
        assertFalse(result.contains("/"));assertFalse(result.contains("\\"));
        assertEquals(result,new String(result.getBytes(StandardCharsets.UTF_8),StandardCharsets.UTF_8));
    }
```

</details>

<!-- /test -->

<!-- test: OutputNamingSafetyTest.rejectedManualNameCannotConsumeAQueueSlot -->
<a id="test-41397aed9d37b511"></a>
### `OutputNamingSafetyTest.rejectedManualNameCannotConsumeAQueueSlot`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Set/array in memoria; locale globale tr-TR ripristinata in finally.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputNamingSafetyTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `plan.directory`, `plan.accept`, `OutputPlan.manual`, `plan.names.isEmpty`, `plan.conflicts`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Nome ../clip respinto prima di accept; nessuno slot o prenotazione consumato.

**Limiti:** Non apre file né prova restrizioni del filesystem/provider. Il test rejectedManualName invoca manual prima di accept: non simula un accept che fallisce.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void rejectedManualNameCannotConsumeAQueueSlot() {
        OutputPlan plan=new OutputPlan(1);plan.directory("one",Set.of());
        assertThrows(IllegalArgumentException.class,()->plan.accept("one",OutputPlan.manual("../clip")));
        assertTrue(plan.names.isEmpty());assertFalse(plan.conflicts("one","clip.mp4"));assertFalse(plan.omitted[0]);
    }
```

</details>

<!-- /test -->


<a id="class-outputplantest"></a>
## OutputPlanTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/OutputPlanTest.java) · 3 metodi · area originaria: File e cartelle.

**Contesto applicativo della classe:** OutputPlan.directory/conflicts/suggestion/accept/skip/manual; OutputName.unique; CompressionSummary.render.

**Riferimenti produttivi:** [OutputPlan](../../app/src/main/java/it/edo/codecprobe/OutputPlan.java), [OutputName](../../app/src/main/java/it/edo/codecprobe/OutputName.java), [CompressionSummary](../../app/src/main/java/it/edo/codecprobe/CompressionSummary.java).

**Preparazione, file e simulazioni condivisi:** Set di nomi e coda in memoria; nessun provider o file reale.

**Confini comuni dell’audit:** Non verifica listing SAF, gare con altri writer né pubblicazione; il rifiuto del nome avviene prima di accept.

**Metodi:** [existingNamesAndQueueReservationsShareNamespace](#test-9a715ed09d68c49d) · [manualNamesKeepMp4AndRejectPathsAndOversizedNames](#test-9cc6c091233a976d) · [omittedFilesAreReportedWithoutSavingsOrFailure](#test-de23f594d43acd69)

<!-- test: OutputPlanTest.existingNamesAndQueueReservationsShareNamespace -->
<a id="test-9a715ed09d68c49d"></a>
### `OutputPlanTest.existingNamesAndQueueReservationsShareNamespace`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Set di nomi e coda in memoria; nessun provider o file reale.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputPlanTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `plan.directory`, `plan.conflicts`, `plan.suggestion`, `plan.accept`, `plan.skip`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Stessa cartella: conflitto senza distinzione maiuscole, suffisso 3 e prenotazione; altra cartella indipendente, skip e refresh conservano la prenotazione.

**Limiti:** Non verifica listing SAF, gare con altri writer né pubblicazione; il rifiuto del nome avviene prima di accept.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void existingNamesAndQueueReservationsShareNamespace() {
        OutputPlan plan=new OutputPlan(4);
        plan.directory("camera",Set.of("Clip.mp4","clip (2).MP4"));
        plan.directory("movies",Set.of());
        assertTrue(plan.conflicts("camera","CLIP.MP4"));
        assertEquals("clip (3).mp4",plan.suggestion("camera","clip.mp4"));
        plan.accept("camera","clip (3).mp4");
        assertTrue(plan.conflicts("camera","CLIP (3).MP4"));
        assertFalse(plan.conflicts("movies","clip (3).mp4"));
        plan.skip();plan.accept("movies","clip.mp4");
        assertTrue(plan.omitted[1]);assertFalse(plan.omitted[0]);
        assertEquals(Arrays.asList("clip (3).mp4","","clip.mp4"),plan.names);
        // A later provider read must not discard earlier queue reservations.
        plan.directory("camera",Set.of());assertTrue(plan.conflicts("camera","clip (3).mp4"));
    }
```

</details>

<!-- /test -->

<!-- test: OutputPlanTest.manualNamesKeepMp4AndRejectPathsAndOversizedNames -->
<a id="test-9cc6c091233a976d"></a>
### `OutputPlanTest.manualNamesKeepMp4AndRejectPathsAndOversizedNames`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Set di nomi e coda in memoria; nessun provider o file reale.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputPlanTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `OutputPlan.manual`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Aggiunge .mp4 e rifiuta vuoto, traversal, controlli e nome oltre 240 byte.

**Limiti:** Non verifica listing SAF, gare con altri writer né pubblicazione; il rifiuto del nome avviene prima di accept.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void manualNamesKeepMp4AndRejectPathsAndOversizedNames() {
        assertEquals("nuovo.mp4",OutputPlan.manual(" nuovo "));
        assertEquals("nuovo.MP4",OutputPlan.manual("nuovo.MP4"));
        for(String name:new String[]{"",".mp4","../video","a\\b","a\nb","a:b","à".repeat(121)}) {
            try {OutputPlan.manual(name);fail(name);}catch(IllegalArgumentException expected){}
        }
    }
```

</details>

<!-- /test -->

<!-- test: OutputPlanTest.omittedFilesAreReportedWithoutSavingsOrFailure -->
<a id="test-de23f594d43acd69"></a>
### `OutputPlanTest.omittedFilesAreReportedWithoutSavingsOrFailure`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Set di nomi e coda in memoria; nessun provider o file reale.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/OutputPlanTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `CompressionSummary.render`, `CompressionSummary.Item`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Voce OMITTED produce testo di omissione, senza percentuale o etichetta di fallimento.

**Limiti:** Non verifica listing SAF, gare con altri writer né pubblicazione; il rifiuto del nome avviene prima di accept.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void omittedFilesAreReportedWithoutSavingsOrFailure() {
        String report=CompressionSummary.render(List.of(new CompressionSummary.Item(1,"clip.mp4",CompressionSummary.Status.OMITTED,100,50)));
        assertTrue(report.contains("Ignorato su richiesta"));assertFalse(report.contains("50,0%"));assertFalse(report.contains("fallita"));
    }
```

</details>

<!-- /test -->


<a id="class-photobasicdetailstest"></a>
## PhotoBasicDetailsTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/PhotoBasicDetailsTest.kt) · 2 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** PhotoBasicDetails.read/rows; ExifInterface; BitmapFactory bounds.

**Riferimenti produttivi:** [PhotoBasicDetails](../../app/src/main/java/it/edo/codecprobe/PhotoBasicDetails.kt).

**Preparazione, file e simulazioni condivisi:** Vero piccolo JPEG48x32 Base64 con EXIF Model Test Camera, temporaryFile; Android parser/bounds nel Robolectric, URI file portabile.

**Confini comuni dell’audit:** Legge dimensioni/camera/bytes/cartella, non decodifica pixel o convertibilità HEIC. 'Copyable error' controlla solo error nonblank, non clipboard.

**Metodi:** [readsTheFivePreviewFieldsWithoutAFullJpegInventory](#test-1426887e87cafcdc) · [unreadableSourceProducesCopyableErrorAndUnavailableValues](#test-7d3532134c1cffb2)

<!-- test: PhotoBasicDetailsTest.readsTheFivePreviewFieldsWithoutAFullJpegInventory -->
<a id="test-1426887e87cafcdc"></a>
### `PhotoBasicDetailsTest.readsTheFivePreviewFieldsWithoutAFullJpegInventory`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Vero piccolo JPEG48x32 Base64 con EXIF Model Test Camera, temporaryFile; Android parser/bounds nel Robolectric, URI file portabile.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoBasicDetailsTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `PhotoBasicDetails.read`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Cinque campi/etichette:48x32, camera, lunghezza reale e directory; nessun JpegInventory completo.

**Limiti:** Legge dimensioni/camera/bytes/cartella, non decodifica pixel o convertibilità HEIC. 'Copyable error' controlla solo error nonblank, non clipboard.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun readsTheFivePreviewFieldsWithoutAFullJpegInventory() {
        val file = File.createTempFile("basic-details-", ".jpg")
        try {
            file.writeBytes(fixture())
            // Use a standard slash-separated file URI on Windows as well as Linux.
            val basic = PhotoBasicDetails.read(RuntimeEnvironment.getApplication(), Uri.parse(file.toURI().toString()).toString())
            assertEquals(basic.toString(), 48, basic.width); assertEquals(32, basic.height)
            assertEquals("Test Camera", basic.camera)
            assertEquals(file.length(), basic.bytes)
            assertEquals(file.parentFile.toURI().path.trimEnd('/'), basic.folder)
            assertEquals(listOf("Dimensioni", "Formato", "Dimensione file", "Fotocamera", "Cartella"), basic.rows().map { it.first })
        } finally { file.delete() }
    }
```

</details>

<!-- /test -->

<!-- test: PhotoBasicDetailsTest.unreadableSourceProducesCopyableErrorAndUnavailableValues -->
<a id="test-7d3532134c1cffb2"></a>
### `PhotoBasicDetailsTest.unreadableSourceProducesCopyableErrorAndUnavailableValues`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Vero piccolo JPEG48x32 Base64 con EXIF Model Test Camera, temporaryFile; Android parser/bounds nel Robolectric, URI file portabile.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoBasicDetailsTest.kt).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `PhotoBasicDetails.read`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Sorgente assente produce errore e valori non disponibili, senza verificare copia UI.

**Limiti:** Legge dimensioni/camera/bytes/cartella, non decodifica pixel o convertibilità HEIC. 'Copyable error' controlla solo error nonblank, non clipboard.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```kotlin
@Test fun unreadableSourceProducesCopyableErrorAndUnavailableValues() {
        val basic = PhotoBasicDetails.read(RuntimeEnvironment.getApplication(), "file:///missing-photo-for-details.jpg")
        assertTrue(basic.error.isNotBlank())
        assertEquals("Non disponibili", basic.rows()[0].second)
        assertEquals("Non indicata nei metadati", basic.rows()[3].second)
    }
```

</details>

<!-- /test -->


<a id="class-photoreferencetest"></a>
## PhotoReferenceTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/PhotoReferenceTest.java) · 4 metodi · area originaria: File e cartelle.

**Contesto applicativo della classe:** DestinationPlan; DestinationDraft; ImageFiles.basename; ImageDestinations.

**Riferimenti produttivi:** [DestinationPlan](../../app/src/main/java/it/edo/codecprobe/DestinationPlan.java), [DestinationDraft](../../app/src/main/java/it/edo/codecprobe/DestinationDraft.java), [ImageFiles](../../app/src/main/java/it/edo/codecprobe/ImageFiles.java).

**Preparazione, file e simulazioni condivisi:** Stringhe URI/nome semplici, SharedPreferences/Activity simulate e3riferimenti artificiali.

**Confini comuni dell’audit:** Nessuna destinazione realmente aperta; eccezioni del modello non dimostrano applicazione nel salvataggio.

**Metodi:** [generalDestinationNeverOverridesExceptions](#test-10febc6b7b69589b) · [draftKeepsExceptionsWithTheirSourceAfterReordering](#test-2a8eb08f773210c3) · [documentIdentifiersAreNeverDisplayedAsFullNames](#test-79453494d24b787a) · [selectingMixedGroupPreservesExistingExceptions](#test-430b47599a0ffaf8)

<!-- test: PhotoReferenceTest.generalDestinationNeverOverridesExceptions -->
<a id="test-10febc6b7b69589b"></a>
### `PhotoReferenceTest.generalDestinationNeverOverridesExceptions`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Stringhe URI/nome semplici, SharedPreferences/Activity simulate e3riferimenti artificiali.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoReferenceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `plan.reset`, `plan.enableExceptions`, `plan.exception`, `plan.setFolder`, `plan.setAllFolder`, `plan.effective`, `plan.exceptionCount`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Nuova cartella generale non sovrascrive own; disabilitare eccezione ritorna al generale.

**Limiti:** Nessuna destinazione realmente aperta; eccezioni del modello non dimostrano applicazione nel salvataggio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void generalDestinationNeverOverridesExceptions(){
        DestinationPlan plan=new DestinationPlan();plan.reset(4,"general");plan.enableExceptions();
        plan.exception(0,true);plan.setFolder(0,"own");plan.setAllFolder("new-general");
        assertEquals("own",plan.effective(0).folder);assertEquals("new-general",plan.effective(1).folder);assertEquals(1,plan.exceptionCount());
        plan.exception(0,true);assertEquals("own",plan.effective(0).folder);
        plan.exception(0,false);assertEquals("new-general",plan.effective(0).folder);assertEquals(0,plan.exceptionCount());
    }
```

</details>

<!-- /test -->

<!-- test: PhotoReferenceTest.draftKeepsExceptionsWithTheirSourceAfterReordering -->
<a id="test-2a8eb08f773210c3"></a>
### `PhotoReferenceTest.draftKeepsExceptionsWithTheirSourceAfterReordering`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Stringhe URI/nome semplici, SharedPreferences/Activity simulate e3riferimenti artificiali.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoReferenceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `draft.sync`, `draft.plan.enableExceptions`, `draft.plan.setFolder`, `draft.save`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Eccezione segue source one dopo persistenza e riordino/nuovo file.

**Limiti:** Nessuna destinazione realmente aperta; eccezioni del modello non dimostrano applicazione nel salvataggio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void draftKeepsExceptionsWithTheirSourceAfterReordering(){
        DestinationDraft draft=new DestinationDraft();draft.sync(Arrays.asList("one","two"),"general");draft.plan.enableExceptions();draft.plan.setFolder(0,"own");
        var prefs=RuntimeEnvironment.getApplication().getSharedPreferences("photo-reference-test",0);var edit=prefs.edit().clear();draft.save(edit,Arrays.asList("one","two"),"general");edit.commit();
        DestinationDraft restored=new DestinationDraft();restored.load(prefs,Arrays.asList("one","two"),"general");restored.sync(Arrays.asList("two","three","one"),"general");
        assertEquals("own",restored.plan.effective(2).folder);assertEquals("general",restored.plan.effective(0).folder);assertEquals(1,restored.plan.exceptionCount());prefs.edit().clear().commit();
    }
```

</details>

<!-- /test -->

<!-- test: PhotoReferenceTest.documentIdentifiersAreNeverDisplayedAsFullNames -->
<a id="test-79453494d24b787a"></a>
### `PhotoReferenceTest.documentIdentifiersAreNeverDisplayedAsFullNames`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Stringhe URI/nome semplici, SharedPreferences/Activity simulate e3riferimenti artificiali.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoReferenceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `ImageFiles.basename`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** basename elimina document parent e null ha fallback Foto JPEG; non testo di tutta UI.

**Limiti:** Nessuna destinazione realmente aperta; eccezioni del modello non dimostrano applicazione nel salvataggio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void documentIdentifiersAreNeverDisplayedAsFullNames(){
        assertEquals("IMG_123.jpg",ImageFiles.basename("primary:DCIM/Test/IMG_123.jpg"));assertEquals("Foto JPEG",ImageFiles.basename(null));
    }
```

</details>

<!-- /test -->

<!-- test: PhotoReferenceTest.selectingMixedGroupPreservesExistingExceptions -->
<a id="test-430b47599a0ffaf8"></a>
### `PhotoReferenceTest.selectingMixedGroupPreservesExistingExceptions`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Stringhe URI/nome semplici, SharedPreferences/Activity simulate e3riferimenti artificiali.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/PhotoReferenceTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `images.state.inputs.clear`, `images.state.names.clear`, `images.state.inputs.addAll`, `images.state.clamp`, `images.setFolder`, `images.state.destinations.plan.setFolder`, `images.toggleSelection`, `images.destinations.photoException`, `images.state.destinations.plan.exceptionCount`, `images.state.destinations.plan.effective`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Gruppo misto0,2 può diventare eccezione senza perdere own già assegnata a0.

**Limiti:** Nessuna destinazione realmente aperta; eccezioni del modello non dimostrano applicazione nel salvataggio.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void selectingMixedGroupPreservesExistingExceptions(){
        var activity=Robolectric.buildActivity(ImageActivity.class).setup();
        try{
            var images=activity.get().images;images.state.inputs.clear();images.state.names.clear();
            images.state.inputs.addAll(Arrays.asList("content://preview/one","content://preview/two","content://preview/three"));images.state.clamp();images.setFolder("general");
            images.state.destinations.plan.setFolder(0,"own");images.toggleSelection(0);images.toggleSelection(2);
            var model=images.destinations.photo;assertFalse(model.general);assertFalse(model.exception);images.destinations.photoException(true);
            assertEquals(2,images.state.destinations.plan.exceptionCount());assertEquals("own",images.state.destinations.plan.effective(0).folder);assertEquals("general",images.state.destinations.plan.effective(2).folder);
            assertTrue(model.allLabel.contains("1"));
        }finally{activity.pause().stop().destroy();RuntimeEnvironment.getApplication().getSharedPreferences("image-workspace",0).edit().clear().commit();}
    }
```

</details>

<!-- /test -->


<a id="class-samsungsefeditortest"></a>
## SamsungSefEditorTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/SamsungSefEditorTest.java) · 3 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** SamsungSef.validate; Mp4Metadata.copy.

**Riferimenti produttivi:** [SamsungSef](../../app/src/main/java/it/edo/codecprobe/SamsungSef.java), [Mp4Metadata](../../app/src/main/java/it/edo/codecprobe/Mp4Metadata.java).

**Preparazione, file e simulazioni condivisi:** JSON NDE sanitizzati e SEF generati da SamsungSefEditorFixtures/MotionFixtures; file reali nel round-trip.

**Confini comuni dell’audit:** Preservare byte/riferimenti non prova che Samsung Editor riapra il progetto o ritrovi il percorso protetto sul dispositivo.

**Metodi:** [observedVersionsCopyWithoutChangingProjectOrReferences](#test-ad6861c561b250f2) · [unexpectedFieldsVersionsReferencesAndInvalidJsonRemainBlocked](#test-b435b24e1d1149c8) · [originalKeyIsAnExternalReferenceNotAnOutputHash](#test-6d2a45205694855d)

<!-- test: SamsungSefEditorTest.observedVersionsCopyWithoutChangingProjectOrReferences -->
<a id="test-ad6861c561b250f2"></a>
### `SamsungSefEditorTest.observedVersionsCopyWithoutChangingProjectOrReferences`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): JSON NDE sanitizzati e SEF generati da SamsungSefEditorFixtures/MotionFixtures; file reali nel round-trip.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SamsungSefEditorTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `SamsungSef.validate`, `Mp4Metadata.copy`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Tre schemi NDE osservati e chiave originale copiati byte per byte dopo relocation; prefisso media invariato.

**Limiti:** Preservare byte/riferimenti non prova che Samsung Editor riapra il progetto o ritrovi il percorso protetto sul dispositivo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void observedVersionsCopyWithoutChangingProjectOrReferences()throws Exception{
        Path source=Files.createTempFile("editor-source-",".mp4"),target=Files.createTempFile("editor-target-",".mp4");
        try{
            for(String project:SamsungSefEditorFixtures.PROJECTS){
                byte[] sef=SamsungSefMotionFixtures.fixture(107,
                    new SamsungSefMotionFixtures.Item(0x0ba1,"Original_Path_Hash_Key","b".repeat(64)+"/12345"),
                    new SamsungSefMotionFixtures.Item(0x0ba1,"VideoEditor_Re_Edit_Data",project.getBytes(StandardCharsets.UTF_8)));
                byte[] original=sef.clone();assertTrue(SamsungSef.validate(sef).contains("2 copiabili · 0 bloccati"));assertArrayEquals(original,sef);
                Files.write(source,SamsungSefMotionFixtures.join(SamsungSefMotionFixtures.content(true),sef));
                byte[] media=SamsungSefMotionFixtures.content(false);Files.write(target,media);
                try(FileChannel input=FileChannel.open(source)){Mp4Metadata.copy(input,target.toFile(),()->false,s->{});}
                byte[] output=Files.readAllBytes(target);
                assertArrayEquals(original,Arrays.copyOfRange(output,output.length-sef.length,output.length));
                assertArrayEquals(Arrays.copyOf(media,37),Arrays.copyOf(output,37));
            }
        }finally{Files.deleteIfExists(source);Files.deleteIfExists(target);}
    }
```

</details>

<!-- /test -->

<!-- test: SamsungSefEditorTest.unexpectedFieldsVersionsReferencesAndInvalidJsonRemainBlocked -->
<a id="test-b435b24e1d1149c8"></a>
### `SamsungSefEditorTest.unexpectedFieldsVersionsReferencesAndInvalidJsonRemainBlocked`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): JSON NDE sanitizzati e SEF generati da SamsungSefEditorFixtures/MotionFixtures; file reali nel round-trip.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SamsungSefEditorTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `reject(…)`, `editor(…)`, `record(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Mutazioni schema/versioni/tipi/path/durata/ID/JSON e UTF-8 invalido restano bloccate.

**Limiti:** Preservare byte/riferimenti non prova che Samsung Editor riapra il progetto o ritrovi il percorso protetto sul dispositivo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void unexpectedFieldsVersionsReferencesAndInvalidJsonRemainBlocked()throws Exception{
        String v2=SamsungSefEditorFixtures.PROJECTS[0],v1=SamsungSefEditorFixtures.PROJECTS[2];
        for(String invalid:new String[]{
            v2.replace("\"NDE_VERSION\":\"2.0\"","\"NDE_VERSION\":\"3.0\""),
            v2.replace("\"ALLOW_REEDIT\":true","\"ALLOW_REEDIT\":1"),
            v2.replace("\"ALLOW_REEDIT\":true","\"ALLOW_REEDIT\":true,\"ALLOW_REEDIT\":false"),
            v2.replace("\"NDE_VERSION\"","\"OFFSET_IN_OUTPUT\":123,\"NDE_VERSION\""),
            v2.replace("_12345.mp4","_54321.mp4"),
            v2.replace("/data/sec/photoeditor/0/","/storage/unknown/"),
            v2.replace("\"BIT_DEPTH\":10","\"BIT_DEPTH\":9"),
            v2.replace("\"DURATION\":30356","\"DURATION\":1"),
            v2.replace("\"LAYERS_INFO\":[","\"LAYERS_INFO\":[{},"),
            v2.replace("\"LAYER_ID\":0","\"LAYER_ID\":1e100"),
            v2+"{}",v2.substring(0,v2.length()-1),"{}","null","[]",
            v1.replace("\"deltaRoi\":1.0012407302856445","\"deltaRoi\":NaN"),
            v1.replace("\"ORIGINAL_FILE_PATH\":\"","\"ORIGINAL_FILE_PATH\":\"!"),
            v1.replace("\"resolution\":[576,1024]","\"resolution\":[0,1024]")
        }){assertFalse("Fixture mutation did not apply",invalid.equals(v1)||invalid.equals(v2));reject(editor(invalid));}
        reject(record("VideoEditor_Re_Edit_Data",new byte[]{(byte)0xc3,0x28}));
        reject(record("VideoEditor_Re_Edit_Data",new byte[65537]));
        reject(SamsungSefMotionFixtures.fixture(107,new SamsungSefMotionFixtures.Item(0x0ba2,"VideoEditor_Re_Edit_Data",v2)));
        reject(record("PhotoEditor_Re_Edit_Data",v2.getBytes(StandardCharsets.UTF_8)));
    }
```

</details>

<!-- /test -->

<!-- test: SamsungSefEditorTest.originalKeyIsAnExternalReferenceNotAnOutputHash -->
<a id="test-6d2a45205694855d"></a>
### `SamsungSefEditorTest.originalKeyIsAnExternalReferenceNotAnOutputHash`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): JSON NDE sanitizzati e SEF generati da SamsungSefEditorFixtures/MotionFixtures; file reali nel round-trip.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SamsungSefEditorTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `SamsungSef.validate`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Original_Path_Hash_Key validato come riferimento esterno: lunghezza, minuscole, ID e suffissi; nessun hash output calcolato.

**Limiti:** Preservare byte/riferimenti non prova che Samsung Editor riapra il progetto o ritrovi il percorso protetto sul dispositivo.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void originalKeyIsAnExternalReferenceNotAnOutputHash()throws Exception{
        String valid="a".repeat(64)+"/12345";assertTrue(SamsungSef.validate(record("Original_Path_Hash_Key",valid.getBytes(StandardCharsets.US_ASCII))).contains("1 copiabili · 0 bloccati"));
        for(String invalid:new String[]{"a".repeat(63)+"/12345","A".repeat(64)+"/12345",valid+"\u0000","a".repeat(64)+"/0","a".repeat(64)+"/9999999999999999999",valid+"/offset"})reject(record("Original_Path_Hash_Key",invalid.getBytes(StandardCharsets.US_ASCII)));
    }
```

</details>

<!-- /test -->


<a id="class-samsungsefmotiontest"></a>
## SamsungSefMotionTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/SamsungSefMotionTest.java) · 1 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** SamsungSef.validate; Mp4Metadata.copy; SamsungSefMotionFixtures.

**Riferimenti produttivi:** [SamsungSef](../../app/src/main/java/it/edo/codecprobe/SamsungSef.java), [Mp4Metadata](../../app/src/main/java/it/edo/codecprobe/Mp4Metadata.java).

**Preparazione, file e simulazioni condivisi:** 15 payload HEX completi con SHA fissati da log v0.65; SEF sintetico e MP4 a box artificiali su file reali.

**Confini comuni dell’audit:** Non prova slow motion/BGM/deflicker in Galleria; non usa video visivo valido. Un unico test aggrega molti formati/errori.

**Metodi:** [completeLogPayloadsAndRelocation](#test-2cc530e49b33de09)

<!-- test: SamsungSefMotionTest.completeLogPayloadsAndRelocation -->
<a id="test-2cc530e49b33de09"></a>
### `SamsungSefMotionTest.completeLogPayloadsAndRelocation`

**Categoria:** C. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): 15 payload HEX completi con SHA fissati da log v0.65; SEF sintetico e MP4 a box artificiali su file reali.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SamsungSefMotionTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: Helper `main(…)`; percorso specificato sopra.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria C. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Hash fixture, copie SEF identiche dopo relocation, 37 byte media invariati, alias super-motion, padding, BGM e varianti malformate.

**Limiti:** Non prova slow motion/BGM/deflicker in Galleria; non usa video visivo valido. Un unico test aggrega molti formati/errori.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@org.junit.Test public void completeLogPayloadsAndRelocation()throws Exception{main(new String[0]);}
```

</details>

<!-- /test -->


<a id="class-samsungsefreporttest"></a>
## SamsungSefReportTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/SamsungSefReportTest.java) · 1 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** SamsungSef.validate; ConversionFailure.copyText; SavedSession.save/load.

**Riferimenti produttivi:** [SamsungSef](../../app/src/main/java/it/edo/codecprobe/SamsungSef.java), [ConversionFailure](../../app/src/main/java/it/edo/codecprobe/ConversionFailure.java), [SavedSession](../../app/src/main/java/it/edo/codecprobe/SavedSession.java).

**Preparazione, file e simulazioni condivisi:** Record Sefd HEX reale modificato type0BA1, offset8000; stato sample con log vuoto, vero journal.

**Confini comuni dell’audit:** Prova diagnosi/persistenza errore, non accettazione Samsung/gallery/editor o conversione reale.

**Metodi:** [fullDiagnosticSurvivesFailureCopyAndSavedSessionWithoutRecentLog](#test-139571f4a34b8bf6)

<!-- test: SamsungSefReportTest.fullDiagnosticSurvivesFailureCopyAndSavedSessionWithoutRecentLog -->
<a id="test-139571f4a34b8bf6"></a>
### `SamsungSefReportTest.fullDiagnosticSurvivesFailureCopyAndSavedSessionWithoutRecentLog`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Record Sefd HEX reale modificato type0BA1, offset8000; stato sample con log vuoto, vero journal.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SamsungSefReportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `SamsungSef.validate`, `state.names.get`, `state.failures.set`, `SavedSession.save`, `SavedSession.load`, `saved.state.failures.get`, `SavedSession.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Errore contiene nome/tipo/offset/hash/preview e viene conservato identico nel checkpoint anche senza recent log.

**Limiti:** Prova diagnosi/persistenza errore, non accettazione Samsung/gallery/editor o conversione reale.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void fullDiagnosticSurvivesFailureCopyAndSavedSessionWithoutRecentLog()throws Exception{
        byte[] bytes=HexFormat.of().parseHex("00000054736566640000510c1400000053616d73756e675f436170747572655f496e666f53637265656e207265636f7264696e67534546486b000000010000000000510c2c0000002c0000001800000053454654");
        // Structurally consistent, but the known capture name has an unexpected 0BA1 type.
        bytes[10]=(byte)0xa1;bytes[11]=0x0b;bytes[66]=(byte)0xa1;bytes[67]=0x0b;
        IOException diagnostic;
        try{SamsungSef.validate(bytes,8000);throw new AssertionError("Unsupported record accepted");}
        catch(IOException expected){diagnostic=expected;}
        Context context=RuntimeEnvironment.getApplication();Intent request=BatchRetryTest.queue(context);
        ConversionService.State state=SavedSessionTest.sample(request);
        ConversionFailure failure=new ConversionFailure(3,state.names.get(2),"Analisi video 3 / 3",diagnostic);
        assertTrue(failure.copyText().contains("Samsung_Capture_Info"));assertTrue(failure.copyText().contains("0x0BA1 (2977)"));
        assertTrue(failure.copyText().contains("record=8008"));assertTrue(failure.copyText().contains("SHA-256 payload"));
        assertTrue(failure.copyText().contains("Primi 16 byte"));assertTrue(failure.copyText().contains("tipo inatteso"));
        state.failures.set(0,failure);state.log="";
        try{
            SavedSession.save(context,request,state);SavedSession saved=SavedSession.load(context);
            assertEquals(failure.copyText(),saved.state.failures.get(0).copyText());
            assertEquals("",saved.state.log);
        }finally{SavedSession.clear(context);}
    }
```

</details>

<!-- /test -->


<a id="class-selectedmetadatareporttest"></a>
## SelectedMetadataReportTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/SelectedMetadataReportTest.java) · 2 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** SelectedMetadataReport.write/section; HomeDiagnostics.exportMetadata.

**Riferimenti produttivi:** [SelectedMetadataReport](../../app/src/main/java/it/edo/codecprobe/SelectedMetadataReport.java), [HomeDiagnostics](../../app/src/main/java/it/edo/codecprobe/HomeDiagnostics.java).

**Preparazione, file e simulazioni condivisi:** Provider Missing registra query/open ma ogni openFile fallisce; writer in memoria o snapshot vero; permessi shadow.

**Confini comuni dell’audit:** Entrambi file sono non leggibili: non prova che report di uno valido sopravviva a un altro invalido, né contenuto metadati reale. Secondo controlla cattura lista/isolamento, non payload riuscito.

**Metodi:** [oneUnreadableFileDoesNotHideOthersAndCancellationDoesNotBecomeSuccess](#test-a36b769fd4a797e3) · [exportCapturesEverySelectedFileAndKeepsConversionSessionUntouched](#test-21b975346dee37aa)

<!-- test: SelectedMetadataReportTest.oneUnreadableFileDoesNotHideOthersAndCancellationDoesNotBecomeSuccess -->
<a id="test-a36b769fd4a797e3"></a>
### `SelectedMetadataReportTest.oneUnreadableFileDoesNotHideOthersAndCancellationDoesNotBecomeSuccess`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Missing registra query/open ma ogni openFile fallisce; writer in memoria o snapshot vero; permessi shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SelectedMetadataReportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `SelectedMetadataReport.write`, `SelectedMetadataReport.section`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Due input inaccessibili compaiono entrambi, progress1/2 e footer; cancel immediato interrompe e section IOException genera NON COMPLETA.

**Limiti:** Entrambi file sono non leggibili: non prova che report di uno valido sopravviva a un altro invalido, né contenuto metadati reale. Secondo controlla cattura lista/isolamento, non payload riuscito.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void oneUnreadableFileDoesNotHideOthersAndCancellationDoesNotBecomeSuccess()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Shadows.shadowOf((android.app.Application)context).grantPermissions(Manifest.permission.ACCESS_MEDIA_LOCATION);
        Missing provider=new Missing();ShadowContentResolver.registerProviderInternal("metadata-test",provider);
        List<Uri> inputs=List.of(Uri.parse("content://metadata-test/one"),Uri.parse("content://metadata-test/two"));
        StringWriter out=new StringWriter();List<Integer> progress=new ArrayList<>();
        SelectedMetadataReport.write(context,inputs,List.of("original.mp4","converted.mp4"),out,()->false,progress::add);
        assertTrue(out.toString(),out.toString().contains("ERRORE ACCESSO ORIGINALE")||out.toString().contains("SEZIONE NON COMPLETA"));
        assertEquals(List.of(1,2),progress);assertTrue(out.toString().contains("FILE 1 / 2 · original.mp4"));assertTrue(out.toString().contains("FILE 2 / 2 · converted.mp4"));assertTrue(out.toString().contains("FINE RAPPORTO METADATI"));assertTrue(provider.queried.contains("content://metadata-test/one"));assertTrue(provider.queried.contains("content://metadata-test/two"));
        try{SelectedMetadataReport.write(context,inputs,List.of("one","two"),new StringWriter(),()->true,n->{});fail();}catch(InterruptedIOException expected){}
        StringWriter partial=new StringWriter();SelectedMetadataReport.section(partial,"unavailable",()->false,w->{throw new IOException("broken metadata");});assertTrue(partial.toString().contains("SEZIONE NON COMPLETA"));
    }
```

</details>

<!-- /test -->

<!-- test: SelectedMetadataReportTest.exportCapturesEverySelectedFileAndKeepsConversionSessionUntouched -->
<a id="test-21b975346dee37aa"></a>
### `SelectedMetadataReportTest.exportCapturesEverySelectedFileAndKeepsConversionSessionUntouched`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Provider Missing registra query/open ma ogni openFile fallisce; writer in memoria o snapshot vero; permessi shadow.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/SelectedMetadataReportTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `home.state.selectedInputs.addAll`, `home.state.selectedNames.addAll`, `home.state.selectedInputs.get`, `ConversionService.snapshot`, `home.diagnostics.exportMetadata`, `home.state.selectedInputs.clear`, `home.state.selectedNames.clear`, `home.diagnostics.worker.submit`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Export congela selezione prima che venga svuotata e conserva sessionId/running/sizeReport del servizio.

**Limiti:** Entrambi file sono non leggibili: non prova che report di uno valido sopravviva a un altro invalido, né contenuto metadati reale. Secondo controlla cattura lista/isolamento, non payload riuscito.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void exportCapturesEverySelectedFileAndKeepsConversionSessionUntouched()throws Exception{
        var activity=Robolectric.buildActivity(MainActivity.class).setup();HomeController home=activity.get().home;
        Missing provider=new Missing();ShadowContentResolver.registerProviderInternal("metadata-test",provider);
        File snapshot=null;
        try{
            Shadows.shadowOf(activity.get().getApplication()).grantPermissions(Manifest.permission.ACCESS_MEDIA_LOCATION,Manifest.permission.READ_MEDIA_VIDEO);
            home.state.selectedInputs.addAll(List.of("content://metadata-test/one","content://metadata-test/two"));home.state.selectedNames.addAll(List.of("original.mp4","converted.mp4"));home.state.input=Uri.parse(home.state.selectedInputs.get(1));home.state.sourceName="converted.mp4";
            ConversionService.State before=ConversionService.snapshot();
            home.diagnostics.exportMetadata();
            // Change the editable selection after dispatch: the export must retain its captured list.
            home.state.selectedInputs.clear();home.state.selectedNames.clear();
            home.diagnostics.worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(home.state.pendingLog);snapshot=new File(home.state.pendingLog);String report=new String(Files.readAllBytes(snapshot.toPath()),java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(report.contains("original.mp4"));assertTrue(report.contains("converted.mp4"));assertTrue(provider.queried.contains("content://metadata-test/one"));assertTrue(provider.queried.contains("content://metadata-test/two"));
            assertEquals(before.sessionId,ConversionService.snapshot().sessionId);assertEquals(before.running,ConversionService.snapshot().running);
            assertEquals(before.sizeReport,ConversionService.snapshot().sizeReport);assertFalse(home.state.exportingDiagnostics);
        }finally{if(snapshot!=null)snapshot.delete();activity.pause().stop().destroy();}
    }
```

</details>

<!-- /test -->


<a id="class-videodateaudittest"></a>
## VideoDateAuditTest

[Sorgente della classe, setup e helper](../../app/src/test/java/it/edo/codecprobe/VideoDateAuditTest.java) · 5 metodi · area originaria: Metadati.

**Contesto applicativo della classe:** VideoDateAudit.verify/readIndex; Snapshot/Index; CreationDatePolicy.

**Riferimenti produttivi:** [VideoDateAudit](../../app/src/main/java/it/edo/codecprobe/VideoDateAudit.java), [CreationDatePolicy](../../app/src/main/java/it/edo/codecprobe/CreationDatePolicy.java).

**Preparazione, file e simulazioni condivisi:** Snapshot numerici/stringhe preparati e Consumer log; ultimo test provider Dates colonne ordinate diversamente, empty/denied e no-write.

**Confini comuni dell’audit:** Prime4 non interrogano Android malgrado messaggi 'letta da Android', nessun video o galleria. Verifica comparatore, non correttezza raccolta Snapshot reale.

**Metodi:** [identicalInternalDatesDoNotHideThreeMinuteIndexDifference](#test-e43c7c6f515ebabe) · [dateChangeExposedByAndroidFailsEvenIfHeaderMatches](#test-c275207e2fe11c01) · [headerChangeFailsAndExplicitFallbackIsVerified](#test-7f4a76574626e933) · [missingOrUnparseableDatesNeverClaimGalleryVerified](#test-79ac95c2982a39e7) · [indexReadsNullableNamedColumnsWithoutWritingAndHandlesUnavailableProviders](#test-68738f801426fb01)

<!-- test: VideoDateAuditTest.identicalInternalDatesDoNotHideThreeMinuteIndexDifference -->
<a id="test-e43c7c6f515ebabe"></a>
### `VideoDateAuditTest.identicalInternalDatesDoNotHideThreeMinuteIndexDifference`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Snapshot numerici/stringhe preparati e Consumer log; ultimo test provider Dates colonne ordinate diversamente, empty/denied e no-write.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoDateAuditTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `VideoDateAudit.verify`, `log.stream`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Header e date interne equivalenti non nascondono delta DATE_TAKEN180000ms: warning, non OK galleria.

**Limiti:** Prime4 non interrogano Android malgrado messaggi 'letta da Android', nessun video o galleria. Verifica comparatore, non correttezza raccolta Snapshot reale.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void identicalInternalDatesDoNotHideThreeMinuteIndexDifference()throws Exception {
        List<String> log=new ArrayList<>();
        VideoDateAudit.verify(sample(MP4,"20261016T120000.000Z",UNIX*1000-180000),
            sample(MP4,"2026-10-16T14:00:00+02:00",UNIX*1000),0,"FILE SALVATO",log::add);
        assertTrue(log.stream().anyMatch(s->s.startsWith("OK DATE INTERNE")));
        assertTrue(log.stream().anyMatch(s->s.startsWith("AVVISO DATA GALLERIA")&&s.contains("180000 ms")));
        assertFalse(log.stream().anyMatch(s->s.startsWith("OK DATA GALLERIA")));
    }
```

</details>

<!-- /test -->

<!-- test: VideoDateAuditTest.dateChangeExposedByAndroidFailsEvenIfHeaderMatches -->
<a id="test-c275207e2fe11c01"></a>
### `VideoDateAuditTest.dateChangeExposedByAndroidFailsEvenIfHeaderMatches`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Snapshot numerici/stringhe preparati e Consumer log; ultimo test provider Dates colonne ordinate diversamente, empty/denied e no-write.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoDateAuditTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `VideoDateAudit.verify`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Reader string diversa ±120/180/3600s causa IOException anche se header uguale.

**Limiti:** Prime4 non interrogano Android malgrado messaggi 'letta da Android', nessun video o galleria. Verifica comparatore, non correttezza raccolta Snapshot reale.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void dateChangeExposedByAndroidFailsEvenIfHeaderMatches()throws Exception {
        for(int seconds:new int[]{120,-180,3600}){
            try{
                VideoDateAudit.verify(sample(MP4,"2026-10-16T12:00:00Z",null),
                    sample(MP4,Instant.ofEpochSecond(UNIX+seconds).toString(),null),0,"FILE SALVATO",s->{});
                fail("Changed reader date accepted");
            }catch(IOException expected){assertTrue(expected.getMessage().contains("DATA LETTA DA ANDROID DIVERSA"));assertTrue(expected.getMessage().contains("Scarto ms="+(seconds*1000)));}
        }
    }
```

</details>

<!-- /test -->

<!-- test: VideoDateAuditTest.headerChangeFailsAndExplicitFallbackIsVerified -->
<a id="test-7f4a76574626e933"></a>
### `VideoDateAuditTest.headerChangeFailsAndExplicitFallbackIsVerified`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Snapshot numerici/stringhe preparati e Consumer log; ultimo test provider Dates colonne ordinate diversamente, empty/denied e no-write.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoDateAuditTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `VideoDateAudit.verify`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Header diverso1secondo fallisce, fallback esplicito MP4 viene accettato.

**Limiti:** Prime4 non interrogano Android malgrado messaggi 'letta da Android', nessun video o galleria. Verifica comparatore, non correttezza raccolta Snapshot reale.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void headerChangeFailsAndExplicitFallbackIsVerified()throws Exception {
        try{VideoDateAudit.verify(sample(MP4,"2026-10-16T12:00:00Z",null),sample(MP4+1,"2026-10-16T12:00:00Z",null),0,"MP4 TEMPORANEO",s->{});fail();}
        catch(IOException expected){assertTrue(expected.getMessage().contains("DATA HEADER DIVERSA"));}
        VideoDateAudit.verify(sample(0,null,null),sample(MP4,"2026-10-16T12:00:00Z",null),MP4,"MP4 TEMPORANEO",s->{});
    }
```

</details>

<!-- /test -->

<!-- test: VideoDateAuditTest.missingOrUnparseableDatesNeverClaimGalleryVerified -->
<a id="test-79ac95c2982a39e7"></a>
### `VideoDateAuditTest.missingOrUnparseableDatesNeverClaimGalleryVerified`

**Categoria:** A. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Snapshot numerici/stringhe preparati e Consumer log; ultimo test provider Dates colonne ordinate diversamente, empty/denied e no-write.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoDateAuditTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `VideoDateAudit.verify`, `log.stream`, `log.clear`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria A. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** Date assenti/non parseabili non producono falso OK e lasciano warning/NON VERIFICATA.

**Limiti:** Prime4 non interrogano Android malgrado messaggi 'letta da Android', nessun video o galleria. Verifica comparatore, non correttezza raccolta Snapshot reale.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void missingOrUnparseableDatesNeverClaimGalleryVerified()throws Exception {
        List<String> log=new ArrayList<>();
        VideoDateAudit.verify(sample(0,null,null),sample(0,null,null),0,"FILE SALVATO",log::add);
        assertTrue(log.stream().anyMatch(s->s.startsWith("DATA GALLERIA NON VERIFICATA")));
        assertFalse(log.stream().anyMatch(s->s.startsWith("OK DATE INTERNE")||s.startsWith("OK DATA GALLERIA")));
        log.clear();VideoDateAudit.verify(sample(MP4,"unknown-original",null),sample(MP4,"unknown-new",null),0,"FILE SALVATO",log::add);
        assertTrue(log.stream().anyMatch(s->s.startsWith("AVVISO DATE")));
    }
```

</details>

<!-- /test -->

<!-- test: VideoDateAuditTest.indexReadsNullableNamedColumnsWithoutWritingAndHandlesUnavailableProviders -->
<a id="test-68738f801426fb01"></a>
### `VideoDateAuditTest.indexReadsNullableNamedColumnsWithoutWritingAndHandlesUnavailableProviders`

**Categoria:** B. Analisi statica; [esecuzioni storiche separate](esecuzioni.md).

**Dati e preparazione:** Fixture di contesto (vedere anche le inizializzazioni nel corpo): Snapshot numerici/stringhe preparati e Consumer log; ultimo test provider Dates colonne ordinate diversamente, empty/denied e no-write.

**Sequenza delle operazioni:** ordine del corpo integrale sotto; setup/teardown e helper nel [sorgente](../../app/src/test/java/it/edo/codecprobe/VideoDateAuditTest.java).

**Metodi applicativi e percorso:** Riferimenti già censiti nell’audit: `VideoDateAudit.readIndex`, `log.stream`.

**File, mock e simulazioni:** fixture di contesto sopra, limitatamente a quanto richiamato dal metodo e dal setup; categoria B. I confini di simulazione sono quelli della classe.

**Asserzioni e risultati attesi:** Valori e condizioni negli assert del corpo sotto e degli helper collegati; la sintesi del contratto è nel campo seguente. Non sono valori runtime misurati.

**Comportamento verificato:** readIndex usa nomi colonne e gestisce null/denied senza write; URIcloud non interrogato.

**Limiti:** Prime4 non interrogano Android malgrado messaggi 'letta da Android', nessun video o galleria. Verifica comparatore, non correttezza raccolta Snapshot reale.

<details>
<summary>Corpo esatto del test: dati, sequenza, chiamate e asserzioni</summary>

```java
@Test public void indexReadsNullableNamedColumnsWithoutWritingAndHandlesUnavailableProviders(){
        Dates provider=new Dates();ShadowContentResolver.registerProviderInternal("media",provider);
        Context context=RuntimeEnvironment.getApplication();Uri uri=Uri.parse("content://media/external/video/media/77?requireOriginal=1");
        VideoDateAudit.Index index=VideoDateAudit.readIndex(context,uri,s->{});
        assertEquals(Long.valueOf(UNIX*1000-180000),index.taken);assertEquals(Long.valueOf(UNIX),index.added);assertEquals(Long.valueOf(UNIX-2),index.modified);
        provider.empty=true;assertNull(VideoDateAudit.readIndex(context,uri,s->{}).taken);
        provider.denied=true;List<String> log=new ArrayList<>();assertNull(VideoDateAudit.readIndex(context,uri,log::add).taken);
        assertTrue(log.stream().anyMatch(s->s.contains("SecurityException")));
        int before=provider.queries;
        assertNull(VideoDateAudit.readIndex(context,Uri.parse("content://cloud/item/77"),s->{}).taken);assertEquals(before,provider.queries);
    }
```

</details>

<!-- /test -->
