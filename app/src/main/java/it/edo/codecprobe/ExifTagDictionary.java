package it.edo.codecprobe;

import java.util.*;

/**
 * Italian explanations for the standard EXIF catalog, GPS and Interop.
 * Factual reference and coverage snapshot: docs/exif-dictionary-0.86.md.
 * No file reads here: a tag name never implies that its payload has been decoded.
 */
final class ExifTagDictionary {
    static final class Entry {
        final String name, technical, meaning;
        int choiceType;
        final Map<String,String> choices = new LinkedHashMap<>();
        Entry(String name,String technical,String meaning){this.name=name;this.technical=technical;this.meaning=meaning;}
    }
    private static final Map<Integer,Entry> EXIF=new LinkedHashMap<>(), GPS=new LinkedHashMap<>(), INTEROP=new LinkedHashMap<>();
    static {
        put(EXIF,0x100,"Larghezza dell’immagine","ImageWidth","Numero di pixel lungo l’asse orizzontale dell’immagine descritta da questa directory. L’eventuale rotazione si applica separatamente.");
        put(EXIF,0x101,"Altezza dell’immagine","ImageHeight","Numero di pixel lungo l’asse verticale dell’immagine descritta da questa directory. Non indica da sola l’orientamento di visualizzazione.");
        put(EXIF,0x102,"Bit per componente","BitsPerSample","Precisione di ciascuna componente del pixel, espressa in bit. Una sequenza 8, 8, 8 indica tre componenti da 8 bit.");
        put(EXIF,0x103,"Compressione dei dati","Compression","Codice del metodo di compressione dei dati descritti dalla directory; può riferirsi alla miniatura e non alla foto principale.");
        put(EXIF,0x106,"Modello delle componenti colore","PhotometricInterpretation","Specifica come interpretare le componenti dei pixel, per esempio RGB oppure YCbCr.");
        put(EXIF,0x10e,"Descrizione dell’immagine","ImageDescription","Testo descrittivo associato all’immagine.");
        put(EXIF,0x10f,"Marca della fotocamera","Make","Nome del produttore del dispositivo che ha acquisito l’immagine.");
        put(EXIF,0x110,"Modello della fotocamera","Model","Nome del modello del dispositivo che ha acquisito l’immagine.");
        put(EXIF,0x111,"Posizioni delle strisce di pixel","StripOffsets","Elenco degli offset delle strisce di dati TIFF. Le lunghezze corrispondenti sono dichiarate in StripByteCounts; i riferimenti interni non sono espansi qui.");
        put(EXIF,0x112,"Orientamento della foto","Orientation","Trasformazione da applicare ai pixel per visualizzare l’immagine. Per stabilire se il risultato è verticale o orizzontale servono anche larghezza e altezza.");
        put(EXIF,0x115,"Componenti per pixel","SamplesPerPixel","Numero di componenti che formano un pixel; per esempio tre per RGB.");
        put(EXIF,0x116,"Righe per striscia","RowsPerStrip","Numero di righe dell’immagine contenute in ciascuna striscia TIFF.");
        put(EXIF,0x117,"Dimensioni delle strisce di pixel","StripByteCounts","Lunghezza in byte di ciascuna striscia TIFF. Le posizioni corrispondenti sono dichiarate in StripOffsets.");
        put(EXIF,0x11a,"Risoluzione orizzontale dichiarata","XResolution","Numero di pixel per unità di lunghezza sull’asse orizzontale. L’unità è nel tag ResolutionUnit; questo numero non è la larghezza in pixel.");
        put(EXIF,0x11b,"Risoluzione verticale dichiarata","YResolution","Numero di pixel per unità di lunghezza sull’asse verticale. L’unità è nel tag ResolutionUnit; questo numero non è l’altezza in pixel.");
        put(EXIF,0x11c,"Disposizione delle componenti","PlanarConfiguration","Indica se le componenti dei pixel sono intercalate oppure memorizzate in piani separati.");
        put(EXIF,0x128,"Unità della risoluzione dichiarata","ResolutionUnit","Unità di lunghezza usata da XResolution e YResolution. Il valore TIFF 1 indica assenza di unità assoluta e non è un valore EXIF standard.");
        put(EXIF,0x12d,"Funzione di trasferimento","TransferFunction","Tabella che descrive la relazione tra i valori dei campioni e la risposta tonale. I singoli coefficienti sono mostrati come anteprima numerica.");
        put(EXIF,0x131,"Software che ha scritto la foto","Software","Nome e, se presente, versione del software che ha creato o modificato l’immagine.");
        put(EXIF,0x132,"Data di modifica","ModifyDate","Data e ora dichiarate per la modifica dell’immagine, nel formato AAAA:MM:GG HH:MM:SS. Il fuso orario è separato in OffsetTime.");
        put(EXIF,0x13b,"Autore dell’immagine","Artist","Nome dell’autore riportato nei metadati. È un testo dichiarato, non una verifica dell’identità.");
        put(EXIF,0x13e,"Punto di bianco","WhitePoint","Due coordinate cromatiche x e y che descrivono il punto di bianco.");
        put(EXIF,0x13f,"Cromaticità dei colori primari","PrimaryChromaticities","Sei valori: coordinate x e y del rosso, poi del verde, poi del blu.");
        put(EXIF,0x201,"Posizione della miniatura JPEG","ThumbnailOffset","Offset dell’immagine JPEG incorporata, relativo all’inizio TIFF. La sua dimensione è nel tag JPEGInterchangeFormatLength.");
        put(EXIF,0x202,"Dimensione della miniatura JPEG","ThumbnailLength","Lunghezza in byte del JPEG incorporato, la cui posizione è nel tag JPEGInterchangeFormat.");
        put(EXIF,0x211,"Coefficienti YCbCr","YCbCrCoefficients","Tre coefficienti usati per la conversione tra RGB e YCbCr, nell’ordine rosso, verde e blu.");
        put(EXIF,0x212,"Sottocampionamento YCbCr","YCbCrSubSampling","Due fattori, orizzontale e verticale, del sottocampionamento delle componenti colore rispetto alla luminanza.");
        put(EXIF,0x213,"Posizione dei campioni colore","YCbCrPositioning","Posizione dei campioni Cb e Cr rispetto ai campioni di luminanza Y.");
        put(EXIF,0x214,"Riferimenti del nero e del bianco","ReferenceBlackWhite","Sei valori: riferimento nero e bianco per ciascuna delle tre componenti.");
        put(EXIF,0x8298,"Copyright","Copyright","Testo dei diritti dichiarati. Può contenere informazioni separate per fotografo ed editore, delimitate da byte nulli.");
        put(EXIF,0x829a,"Tempo di esposizione","ExposureTime","Durata dell’esposizione in secondi, memorizzata come frazione. Un valore 1/100 significa un centesimo di secondo.");
        put(EXIF,0x829d,"Apertura del diaframma","FNumber","Numero f del diaframma, senza unità. Per esempio 18/10 corrisponde a f/1,8.");
        put(EXIF,0x8769,"Collegamento ai tag EXIF","ExifOffset","Offset della directory EXIF, relativo all’inizio TIFF. Questo campo contiene un riferimento e non tutti i dati della directory.");
        put(EXIF,0x8822,"Programma di esposizione","ExposureProgram","Programma utilizzato dalla fotocamera per scegliere tempo e apertura.");
        put(EXIF,0x8824,"Sensibilità spettrale","SpectralSensitivity","Testo che descrive la sensibilità del sensore alle diverse lunghezze d’onda.");
        put(EXIF,0x8825,"Collegamento ai tag GPS","GPSInfo","Offset della directory GPS, relativo all’inizio TIFF. La directory GPS usa un proprio insieme di identificativi.");
        put(EXIF,0x8827,"Sensibilità fotografica dichiarata","ISO","Valore di sensibilità fotografica. SensitivityType specifica quale misura rappresenta; per valori estesi possono essere necessari i tag SOS, REI e ISOSpeed.");
        put(EXIF,0x8828,"Funzione di conversione optoelettrica","Opto-ElectricConvFactor","Struttura binaria che descrive la risposta optoelettrica. Il tag è riconosciuto, ma la tabella interna non viene decodificata.");
        put(EXIF,0x8830,"Tipo di sensibilità","SensitivityType","Specifica se il valore di sensibilità rappresenta SOS, REI, velocità ISO oppure una combinazione di queste misure.");
        put(EXIF,0x8831,"Sensibilità standard di uscita","StandardOutputSensitivity","Valore SOS, Standard Output Sensitivity, dichiarato secondo la misura di sensibilità fotografica.");
        put(EXIF,0x8832,"Indice di esposizione raccomandato","RecommendedExposureIndex","Valore REI, Recommended Exposure Index, consigliato dal produttore.");
        put(EXIF,0x8833,"Velocità ISO","ISOSpeed","Valore della velocità ISO; è distinto dai campi SOS e REI.");
        put(EXIF,0x8834,"Limite ISO yyy","ISOSpeedLatitudeyyy","Parametro yyy della latitudine di esposizione ISO, da leggere insieme agli altri campi ISO.");
        put(EXIF,0x8835,"Limite ISO zzz","ISOSpeedLatitudezzz","Parametro zzz della latitudine di esposizione ISO, da leggere insieme agli altri campi ISO.");
        put(EXIF,0x9000,"Versione EXIF","ExifVersion","Quattro byte ASCII che codificano la versione EXIF, per esempio 0232 per la versione 2.32. Il tipo TIFF è UNDEFINED, ma qui il formato dei quattro byte è noto.");
        put(EXIF,0x9003,"Data e ora dello scatto","DateTimeOriginal","Data e ora di acquisizione, nel formato AAAA:MM:GG HH:MM:SS. Fuso e frazione di secondo sono nei tag OffsetTimeOriginal e SubSecTimeOriginal.");
        put(EXIF,0x9004,"Data e ora di digitalizzazione","CreateDate","Data e ora in cui l’immagine è stata digitalizzata. Il fuso è in OffsetTimeDigitized e la frazione di secondo in SubSecTimeDigitized.");
        put(EXIF,0x9010,"Fuso della data di modifica","OffsetTime","Scostamento rispetto a UTC della data di modifica, nel formato +HH:MM oppure -HH:MM.");
        put(EXIF,0x9011,"Fuso della data dello scatto","OffsetTimeOriginal","Scostamento rispetto a UTC di DateTimeOriginal, nel formato +HH:MM oppure -HH:MM.");
        put(EXIF,0x9012,"Fuso della digitalizzazione","OffsetTimeDigitized","Scostamento rispetto a UTC della data di digitalizzazione, nel formato +HH:MM oppure -HH:MM.");
        put(EXIF,0x9101,"Configurazione delle componenti","ComponentsConfiguration","Sequenza di codici delle componenti: 0 assente, 1 Y, 2 Cb, 3 Cr, 4 R, 5 G, 6 B. L’ordine dei byte è l’ordine delle componenti.");
        put(EXIF,0x9102,"Bit compressi per pixel","CompressedBitsPerPixel","Numero medio di bit per pixel dichiarato per l’immagine compressa. Non coincide con la precisione di ciascun canale.");
        put(EXIF,0x9201,"Tempo di posa in scala APEX","ShutterSpeedValue","Valore Tv della scala APEX. Il tempo in secondi si ricava con 2 elevato a -Tv; non è già una durata in secondi.");
        put(EXIF,0x9202,"Apertura in scala APEX","ApertureValue","Valore Av della scala APEX. Il numero f si ricava con 2 elevato ad Av/2.");
        put(EXIF,0x9203,"Luminosità in scala APEX","BrightnessValue","Luminosità della scena espressa come valore Bv della scala APEX; non è una percentuale di luminosità del display.");
        put(EXIF,0x9204,"Compensazione dell’esposizione","ExposureCompensation","Correzione dell’esposizione espressa in EV. Un valore positivo richiede più esposizione, uno negativo meno.");
        put(EXIF,0x9205,"Apertura massima in scala APEX","MaxApertureValue","Valore APEX dell’apertura massima dell’obiettivo. Il corrispondente numero f si ricava con 2 elevato al valore/2.");
        put(EXIF,0x9206,"Distanza del soggetto","SubjectDistance","Distanza dal soggetto in metri. Il valore zero indica distanza sconosciuta; il numeratore 4294967295 rappresenta infinito.");
        put(EXIF,0x9207,"Modalità di misurazione della luce","MeteringMode","Metodo usato dalla fotocamera per misurare la luce della scena.");
        put(EXIF,0x9208,"Sorgente luminosa","LightSource","Tipo di illuminazione dichiarata per lo scatto; può influenzare l’interpretazione del bilanciamento del bianco.");
        put(EXIF,0x9209,"Impostazioni del flash","Flash","Maschera di bit: indica se il flash è scattato, il rilevamento della luce di ritorno, la modalità, la presenza della funzione e la riduzione occhi rossi.");
        put(EXIF,0x920a,"Lunghezza focale","FocalLength","Lunghezza focale effettiva dell’obiettivo in millimetri; è distinta dalla focale equivalente su formato 35 mm.");
        put(EXIF,0x9214,"Area del soggetto","SubjectArea","Coordinate in pixel: due valori descrivono il centro x,y; tre aggiungono il diametro di un cerchio; quattro aggiungono larghezza e altezza di un rettangolo.");
        put(EXIF,0x927c,"Dati del produttore della fotocamera","MakerNote","MakerNote: blocco con formato specifico del produttore. Riconosciamo il campo e mostriamo i byte; le strutture interne proprietarie restano non interpretate.");
        put(EXIF,0x9286,"Commento dell’utente","UserComment","Testo preceduto da un identificativo di codifica di 8 byte. Può usare ASCII, JIS, Unicode o una codifica non dichiarata; il contenuto non viene interpretato per tentativi.");
        put(EXIF,0x9287,"Preferenze per l’uso nell’intelligenza artificiale","LearningOptOutIn","Struttura che dichiara preferenze relative all’uso per addestramento, estrazione di dati e modelli. Il contenuto binario interno non è ancora decodificato.");
        put(EXIF,0x9290,"Frazione di secondo della modifica","SubSecTime","Cifre decimali da aggiungere ai secondi della data di modifica. Il testo 123 rappresenta una frazione di 0,123 secondi.");
        put(EXIF,0x9291,"Frazione di secondo dello scatto","SubSecTimeOriginal","Cifre decimali da aggiungere ai secondi di DateTimeOriginal.");
        put(EXIF,0x9292,"Frazione di secondo della digitalizzazione","SubSecTimeDigitized","Cifre decimali da aggiungere ai secondi della data di digitalizzazione.");
        put(EXIF,0x9400,"Temperatura ambiente","AmbientTemperature","Temperatura ambientale al momento dello scatto, in gradi Celsius.");
        put(EXIF,0x9401,"Umidità relativa","Humidity","Umidità relativa al momento dello scatto, espressa in percentuale.");
        put(EXIF,0x9402,"Pressione atmosferica","Pressure","Pressione atmosferica in hPa, equivalenti ai millibar.");
        put(EXIF,0x9403,"Profondità in acqua","WaterDepth","Profondità in metri al momento dello scatto.");
        put(EXIF,0x9404,"Accelerazione","Acceleration","Modulo dell’accelerazione al momento dello scatto, in mGal. Un mGal equivale a 0,00001 m/s².");
        put(EXIF,0x9405,"Inclinazione della fotocamera","CameraElevationAngle","Angolo di elevazione della fotocamera, in gradi rispetto all’orizzonte.");
        put(EXIF,0xa000,"Versione Flashpix","FlashpixVersion","Quattro byte ASCII che indicano la versione Flashpix supportata.");
        put(EXIF,0xa001,"Spazio colore dichiarato","ColorSpace","Codice dello spazio colore. Un valore non calibrato in questo tag non esclude la presenza di un profilo ICC separato.");
        put(EXIF,0xa002,"Larghezza dell’immagine","ExifImageWidth","Numero di pixel orizzontali dell’immagine descritta dai tag EXIF, prima dell’eventuale trasformazione di orientamento.");
        put(EXIF,0xa003,"Altezza dell’immagine","ExifImageHeight","Numero di pixel verticali dell’immagine descritta dai tag EXIF, prima dell’eventuale trasformazione di orientamento.");
        put(EXIF,0xa004,"File audio associato","RelatedSoundFile","Nome del file audio correlato all’immagine; il tag non contiene i campioni audio.");
        put(EXIF,0xa005,"Collegamento ai tag di interoperabilità","InteropOffset","Offset della directory di interoperabilità, relativo all’inizio TIFF. Questa directory usa identificativi distinti da quelli GPS.");
        put(EXIF,0xa20b,"Energia del flash","FlashEnergy","Energia del flash al momento dello scatto, espressa in BCPS, unità fotometrica di energia luminosa.");
        put(EXIF,0xa20c,"Risposta in frequenza spaziale","SpatialFrequencyResponse","Struttura che descrive la risposta del sistema ai dettagli spaziali. Il contenuto binario interno non viene decodificato.");
        put(EXIF,0xa20e,"Risoluzione orizzontale sul sensore","FocalPlaneXResolution","Numero di pixel per unità di lunghezza sul piano focale lungo l’asse orizzontale. L’unità è in FocalPlaneResolutionUnit.");
        put(EXIF,0xa20f,"Risoluzione verticale sul sensore","FocalPlaneYResolution","Numero di pixel per unità di lunghezza sul piano focale lungo l’asse verticale. L’unità è in FocalPlaneResolutionUnit.");
        put(EXIF,0xa210,"Unità della risoluzione sul sensore","FocalPlaneResolutionUnit","Unità di lunghezza usata da FocalPlaneXResolution e FocalPlaneYResolution.");
        put(EXIF,0xa214,"Posizione del soggetto","SubjectLocation","Due coordinate in pixel: x orizzontale, poi y verticale, riferite all’immagine prima delle trasformazioni di visualizzazione.");
        put(EXIF,0xa215,"Indice di esposizione","ExposureIndex","Indice di esposizione selezionato al momento dello scatto.");
        put(EXIF,0xa217,"Metodo di acquisizione del sensore","SensingMethod","Descrive l’organizzazione del sensore che ha acquisito l’immagine.");
        put(EXIF,0xa300,"Origine del file","FileSource","Codice che identifica l’origine dell’immagine, per esempio una fotocamera digitale. Il codice è memorizzato come byte UNDEFINED.");
        put(EXIF,0xa301,"Tipo di scena acquisita","SceneType","Codice che indica il tipo di acquisizione della scena; il valore 1 indica una scena fotografata direttamente.");
        put(EXIF,0xa302,"Schema dei filtri del sensore","CFAPattern","Struttura binaria della matrice di filtri colore CFA. Il tag è riconosciuto, ma il disegno interno non viene ricostruito.");
        put(EXIF,0xa401,"Elaborazione personalizzata","CustomRendered","Indica se è stata applicata un’elaborazione speciale rispetto alla normale elaborazione dell’immagine.");
        put(EXIF,0xa402,"Modalità di esposizione","ExposureMode","Indica esposizione automatica, manuale oppure acquisizione con variazione automatica dell’esposizione.");
        put(EXIF,0xa403,"Bilanciamento del bianco","WhiteBalance","Indica se il bilanciamento del bianco è stato impostato automaticamente o manualmente.");
        put(EXIF,0xa404,"Rapporto di zoom digitale","DigitalZoomRatio","Fattore dello zoom digitale. Zero indica che lo zoom digitale non è stato usato.");
        put(EXIF,0xa405,"Focale equivalente su 35 mm","FocalLengthIn35mmFormat","Lunghezza focale equivalente sul formato 35 mm, in millimetri. Zero indica un valore sconosciuto.");
        put(EXIF,0xa406,"Categoria della scena","SceneCaptureType","Categoria della scena acquisita, per esempio paesaggio, ritratto o scena notturna.");
        put(EXIF,0xa407,"Regolazione del guadagno","GainControl","Tipo di regolazione del guadagno applicata all’immagine.");
        put(EXIF,0xa408,"Regolazione del contrasto","Contrast","Livello di contrasto applicato dalla fotocamera rispetto al livello normale.");
        put(EXIF,0xa409,"Regolazione della saturazione","Saturation","Livello di saturazione applicato dalla fotocamera rispetto al livello normale.");
        put(EXIF,0xa40a,"Regolazione della nitidezza","Sharpness","Livello di nitidezza applicato dalla fotocamera rispetto al livello normale.");
        put(EXIF,0xa40b,"Descrizione delle impostazioni del dispositivo","DeviceSettingDescription","Struttura binaria delle impostazioni del dispositivo. I campi interni non sono ancora decodificati.");
        put(EXIF,0xa40c,"Intervallo di distanza del soggetto","SubjectDistanceRange","Categoria della distanza del soggetto: macro, vicino, lontano o sconosciuto.");
        put(EXIF,0xa40d,"Tipo di sviluppo dell’immagine","DevelopmentType","Due codici: il primo descrive la fedeltà di riproduzione, il secondo indica se sono state usate impostazioni di fabbrica. I due codici hanno tabelle distinte.");
        put(EXIF,0xa40e,"Descrizione dello sviluppo","DevelopmentTypeDescription","Testo descrittivo del processo di sviluppo dell’immagine.");
        put(EXIF,0xa40f,"Correzione della distorsione","DistortionCorrection","Indica se è stata applicata una correzione della distorsione dell’obiettivo.");
        put(EXIF,0xa410,"Correzione dell’aberrazione cromatica","ChromaticAberrationCorrection","Indica se è stata applicata una correzione dell’aberrazione cromatica dell’obiettivo.");
        put(EXIF,0xa411,"Correzione della vignettatura","ShadingCorrection","Indica se è stata applicata una correzione dell’ombreggiatura ai bordi dell’immagine.");
        put(EXIF,0xa412,"Riduzione del rumore","NoiseReduction","Indica se è stata applicata una riduzione del rumore all’immagine.");
        put(EXIF,0xa420,"Identificativo univoco dell’immagine","ImageUniqueID","Stringa identificativa dell’immagine, generalmente di 32 caratteri esadecimali. Non è una firma crittografica che certifica l’autenticità.");
        put(EXIF,0xa430,"Nome del proprietario della fotocamera","OwnerName","Nome del proprietario dichiarato nei metadati.");
        put(EXIF,0xa431,"Numero di serie della fotocamera","SerialNumber","Identificativo seriale del corpo macchina.");
        put(EXIF,0xa432,"Caratteristiche dell’obiettivo","LensInfo","Quattro frazioni: focale minima e massima in millimetri, poi numero f minimo alle due estremità della focale.");
        put(EXIF,0xa433,"Marca dell’obiettivo","LensMake","Nome del produttore dell’obiettivo.");
        put(EXIF,0xa434,"Modello dell’obiettivo","LensModel","Nome del modello dell’obiettivo.");
        put(EXIF,0xa435,"Numero di serie dell’obiettivo","LensSerialNumber","Identificativo seriale dell’obiettivo.");
        put(EXIF,0xa436,"Titolo dell’immagine","ImageTitle","Titolo testuale dell’immagine.");
        put(EXIF,0xa437,"Fotografo","Photographer","Nome del fotografo dichiarato nei metadati.");
        put(EXIF,0xa438,"Responsabile dell’elaborazione","ImageEditor","Nome della persona che ha elaborato l’immagine.");
        put(EXIF,0xa439,"Firmware della fotocamera","CameraFirmware","Versione del firmware del dispositivo di acquisizione.");
        put(EXIF,0xa43a,"Software di sviluppo RAW","RAWDevelopingSoftware","Nome e versione del software utilizzato per sviluppare i dati RAW.");
        put(EXIF,0xa43b,"Software di elaborazione dell’immagine","ImageEditingSoftware","Nome e versione del software utilizzato per elaborare l’immagine.");
        put(EXIF,0xa43c,"Software di modifica dei metadati","MetadataEditingSoftware","Nome e versione del software utilizzato per modificare i metadati.");
        put(EXIF,0xa460,"Immagine composta","CompositeImage","Indica se l’immagine deriva dalla combinazione di più immagini e come è stata acquisita.");
        put(EXIF,0xa461,"Numero di immagini della composizione","CompositeImageCount","Due numeri: quantità di immagini sorgenti, poi quantità di immagini utilizzate nella composizione.");
        put(EXIF,0xa462,"Tempi delle immagini della composizione","CompositeImageExposureTimes","Struttura con tempi complessivi, estremi e sequenze delle esposizioni sorgenti. Il contenuto binario interno non viene ancora decodificato.");
        put(EXIF,0xa500,"Gamma","Gamma","Esponente gamma dichiarato per la risposta tonale dell’immagine.");
        put(GPS,0x0,"Versione dei dati GPS","GPSVersionID","Quattro numeri che identificano la versione del formato dei metadati GPS.");
        put(GPS,0x1,"Emisfero della latitudine","GPSLatitudeRef","Lettera che determina il segno della latitudine: N per nord, S per sud. La coordinata numerica è nel tag GPSLatitude.");
        put(GPS,0x2,"Latitudine","GPSLatitude","Tre frazioni: gradi, minuti e secondi. Il valore decimale è gradi + minuti/60 + secondi/3600; l’emisfero è nel tag GPSLatitudeRef.");
        put(GPS,0x3,"Emisfero della longitudine","GPSLongitudeRef","Lettera che determina il segno della longitudine: E per est, W per ovest. La coordinata numerica è nel tag GPSLongitude.");
        put(GPS,0x4,"Longitudine","GPSLongitude","Tre frazioni: gradi, minuti e secondi. L’emisfero è nel tag GPSLongitudeRef; la coordinata da sola non ne contiene il segno.");
        put(GPS,0x5,"Riferimento dell’altitudine","GPSAltitudeRef","Specifica come interpretare il segno dell’altitudine rispetto al livello del mare.");
        put(GPS,0x6,"Altitudine","GPSAltitude","Distanza verticale in metri. Per determinarne il segno occorre leggere GPSAltitudeRef.");
        put(GPS,0x7,"Ora GPS","GPSTimeStamp","Tre frazioni: ore, minuti e secondi in UTC. La data è separata nel tag GPSDateStamp.");
        put(GPS,0x8,"Satelliti GPS","GPSSatellites","Testo descrittivo dei satelliti utilizzati per la misura.");
        put(GPS,0x9,"Stato della misura GPS","GPSStatus","Indica se la misura GPS era attiva oppure non valida.");
        put(GPS,0xa,"Dimensioni della misura GPS","GPSMeasureMode","Indica se il posizionamento è bidimensionale o tridimensionale.");
        put(GPS,0xb,"Diluizione della precisione GPS","GPSDOP","Parametro DOP, senza unità, relativo alla geometria dei satelliti. Non è un errore espresso direttamente in metri.");
        put(GPS,0xc,"Unità della velocità GPS","GPSSpeedRef","Specifica se GPSSpeed è espresso in chilometri orari, miglia orarie o nodi.");
        put(GPS,0xd,"Velocità GPS","GPSSpeed","Velocità al momento della misura. L’unità è nel tag GPSSpeedRef.");
        put(GPS,0xe,"Riferimento della direzione di movimento","GPSTrackRef","Indica se la direzione di movimento è riferita al nord geografico o magnetico.");
        put(GPS,0xf,"Direzione di movimento","GPSTrack","Angolo di movimento in gradi, da 0 incluso a 360 escluso. Il riferimento è in GPSTrackRef.");
        put(GPS,0x10,"Riferimento della direzione della foto","GPSImgDirectionRef","Indica se la direzione della fotocamera è riferita al nord geografico o magnetico.");
        put(GPS,0x11,"Direzione della foto","GPSImgDirection","Direzione della fotocamera in gradi. Il riferimento è in GPSImgDirectionRef.");
        put(GPS,0x12,"Sistema geodetico GPS","GPSMapDatum","Nome del riferimento geodetico delle coordinate, per esempio WGS-84.");
        put(GPS,0x13,"Emisfero della latitudine di destinazione","GPSDestLatitudeRef","N per nord, S per sud, riferiti alla coordinata GPSDestLatitude.");
        put(GPS,0x14,"Latitudine di destinazione","GPSDestLatitude","Tre frazioni: gradi, minuti e secondi della destinazione. L’emisfero è separato in GPSDestLatitudeRef.");
        put(GPS,0x15,"Emisfero della longitudine di destinazione","GPSDestLongitudeRef","E per est, W per ovest, riferiti alla coordinata GPSDestLongitude.");
        put(GPS,0x16,"Longitudine di destinazione","GPSDestLongitude","Tre frazioni: gradi, minuti e secondi della destinazione. L’emisfero è separato in GPSDestLongitudeRef.");
        put(GPS,0x17,"Riferimento della direzione di destinazione","GPSDestBearingRef","Indica nord geografico o magnetico per GPSDestBearing.");
        put(GPS,0x18,"Direzione verso la destinazione","GPSDestBearing","Angolo in gradi verso la destinazione. Il riferimento è nel tag GPSDestBearingRef.");
        put(GPS,0x19,"Unità della distanza di destinazione","GPSDestDistanceRef","Specifica chilometri, miglia oppure miglia nautiche per GPSDestDistance.");
        put(GPS,0x1a,"Distanza dalla destinazione","GPSDestDistance","Distanza dalla destinazione; l’unità è dichiarata in GPSDestDistanceRef.");
        put(GPS,0x1b,"Metodo di posizionamento","GPSProcessingMethod","Metodo usato per trovare la posizione, per esempio GPS, rete cellulare, WLAN o inserimento manuale. Il testo ha un prefisso di codifica di 8 byte.");
        put(GPS,0x1c,"Informazioni sull’area GPS","GPSAreaInformation","Descrizione testuale dell’area geografica preceduta da un identificativo di codifica di 8 byte.");
        put(GPS,0x1d,"Data GPS","GPSDateStamp","Data UTC della misura, nel formato AAAA:MM:GG. L’ora è separata in GPSTimeStamp.");
        put(GPS,0x1e,"Correzione differenziale GPS","GPSDifferential","Indica se alla misura è stata applicata una correzione differenziale.");
        put(GPS,0x1f,"Errore orizzontale GPS","GPSHPositioningError","Errore di posizionamento orizzontale dichiarato in metri.");
        put(INTEROP,0x1,"Profilo di interoperabilità","InteropIndex","Identificativo ASCII delle regole di interoperabilità DCF. R98 indica il profilo base sRGB, R03 il profilo opzionale Adobe RGB, THM un file di miniatura.");
        put(INTEROP,0x2,"Versione di interoperabilità","InteropVersion","Quattro byte ASCII che identificano la versione delle regole di interoperabilità.");
        put(INTEROP,0x1000,"Formato dell’immagine correlata","RelatedImageFileFormat","Nome del formato del file immagine correlato.");
        put(INTEROP,0x1001,"Larghezza dell’immagine correlata","RelatedImageWidth","Larghezza in pixel dell’immagine correlata; non è necessariamente la larghezza della foto corrente.");
        put(INTEROP,0x1002,"Altezza dell’immagine correlata","RelatedImageHeight","Altezza in pixel dell’immagine correlata; non è necessariamente l’altezza della foto corrente.");
        choices(EXIF,0x103,3,"1=nessuna compressione|6=JPEG (schema TIFF storico)|7=JPEG");
        choices(EXIF,0x106,3,"0=scala di grigi, zero bianco|1=scala di grigi, zero nero|2=RGB|3=palette RGB|5=CMYK|6=YCbCr|8=CIELab|32803=matrice di filtri colore CFA|34892=RAW lineare");
        choices(EXIF,0x112,3,"1=normale, nessuna trasformazione|2=specchiatura orizzontale (destra e sinistra scambiate)|3=rotazione di 180°|4=specchiatura verticale (alto e basso scambiati)|5=trasposizione lungo la diagonale alto-sinistra / basso-destra|6=rotazione di 90° in senso orario|7=trasposizione lungo la diagonale alto-destra / basso-sinistra|8=rotazione di 90° in senso antiorario");
        choices(EXIF,0x11c,3,"1=componenti intercalate per pixel|2=componenti in piani separati");
        choices(EXIF,0x128,3,"1=nessuna unità assoluta (TIFF)|2=pollici|3=centimetri");
        choices(EXIF,0x213,3,"1=campioni colore centrati|2=campioni colore co-posizionati");
        choices(EXIF,0x8822,3,"0=non definito|1=manuale|2=programma normale|3=priorità al diaframma|4=priorità al tempo|5=programma creativo|6=programma azione|7=ritratto|8=paesaggio");
        choices(EXIF,0x8830,3,"0=sconosciuto|1=SOS|2=REI|3=velocità ISO|4=SOS e REI|5=SOS e velocità ISO|6=REI e velocità ISO|7=SOS, REI e velocità ISO");
        choices(EXIF,0x9207,3,"0=sconosciuta|1=media|2=media pesata al centro|3=spot|4=multi-spot|5=multizona|6=parziale|255=altra");
        choices(EXIF,0x9208,3,"0=sconosciuta|1=luce diurna|2=fluorescente|3=tungsteno, incandescenza|4=flash|9=bel tempo|10=nuvoloso|11=ombra|12=fluorescente diurna|13=fluorescente bianca diurna|14=fluorescente bianca fredda|15=fluorescente bianca|16=fluorescente bianca calda|17=illuminante standard A|18=illuminante standard B|19=illuminante standard C|20=D55|21=D65|22=D75|23=D50|24=illuminazione ISO da studio|255=altra");
        choices(EXIF,0xa001,3,"1=sRGB|2=Adobe RGB (valore non standard)|65533=Wide Gamut RGB (estensione non standard)|65534=profilo ICC (estensione non standard)|65535=non calibrato in questo tag");
        choices(EXIF,0xa210,3,"1=nessuna unità assoluta|2=pollici|3=centimetri|4=millimetri|5=micrometri");
        choices(EXIF,0xa217,3,"1=non definito|2=sensore a un chip a colori|3=sensore a due chip a colori|4=sensore a tre chip a colori|5=sensore ad area a colori sequenziali|7=sensore trilineare|8=sensore lineare a colori sequenziali");
        choices(EXIF,0xa300,7,"1=scanner di pellicola|2=scanner di stampe|3=fotocamera digitale");
        choices(EXIF,0xa301,7,"1=scena fotografata direttamente");
        choices(EXIF,0xa401,3,"0=elaborazione normale|1=elaborazione personalizzata");
        choices(EXIF,0xa402,3,"0=esposizione automatica|1=esposizione manuale|2=bracketing automatico");
        choices(EXIF,0xa403,3,"0=automatico|1=manuale");
        choices(EXIF,0xa406,3,"0=standard|1=paesaggio|2=ritratto|3=scena notturna");
        choices(EXIF,0xa407,3,"0=nessuna regolazione|1=aumento lieve|2=aumento forte|3=riduzione lieve|4=riduzione forte");
        choices(EXIF,0xa408,3,"0=normale|1=morbido|2=duro");
        choices(EXIF,0xa409,3,"0=normale|1=bassa|2=alta");
        choices(EXIF,0xa40a,3,"0=normale|1=morbida|2=dura");
        choices(EXIF,0xa40c,3,"0=sconosciuto|1=macro|2=vicino|3=lontano");
        choices(EXIF,0xa40f,3,"0=non applicata|1=applicata");
        choices(EXIF,0xa410,3,"0=non applicata|1=applicata");
        choices(EXIF,0xa411,3,"0=non applicata|1=applicata");
        choices(EXIF,0xa412,3,"0=non applicata|1=applicata");
        choices(EXIF,0xa460,3,"0=sconosciuto|1=immagine non composta|2=immagine composta|3=immagine composta durante lo scatto");
        choices(GPS,0x1,2,"N=nord|S=sud");
        choices(GPS,0x13,2,"N=nord|S=sud");
        choices(GPS,0x3,2,"E=est|W=ovest");
        choices(GPS,0x15,2,"E=est|W=ovest");
        choices(GPS,0x5,1,"0=sopra il livello del mare|1=sotto il livello del mare|2=positivo rispetto al riferimento del livello del mare|3=negativo rispetto al riferimento del livello del mare");
        choices(GPS,0x9,2,"A=misura attiva|V=misura non valida");
        choices(GPS,0xa,2,"2=misura bidimensionale|3=misura tridimensionale");
        choices(GPS,0xc,2,"K=chilometri orari (km/h)|M=miglia orarie (mph)|N=nodi");
        choices(GPS,0xe,2,"T=nord geografico|M=nord magnetico");
        choices(GPS,0x10,2,"T=nord geografico|M=nord magnetico");
        choices(GPS,0x17,2,"T=nord geografico|M=nord magnetico");
        choices(GPS,0x19,2,"K=chilometri|M=miglia|N=miglia nautiche");
        choices(GPS,0x1e,3,"0=nessuna correzione|1=correzione differenziale applicata");
    }
    private static void put(Map<Integer,Entry> table,int id,String name,String technical,String meaning){
        if(table.put(id,new Entry(name,technical,meaning))!=null)throw new IllegalStateException("Duplicate EXIF dictionary entry");
    }
    private static void choices(Map<Integer,Entry> table,int id,int type,String values){
        Entry e=table.get(id);e.choiceType=type;
        for(String line:values.split("\\|")){int p=line.indexOf('=');e.choices.put(line.substring(0,p),line.substring(p+1));}
    }
    static Entry find(int id,String directory){
        return (directory.equals("GPS")?GPS:directory.equals("Interop")?INTEROP:EXIF).get(id);
    }
    static Entry find(AnalysisField f){return find(f.tag,f.directory);}
    static int size(){return EXIF.size()+GPS.size()+INTEROP.size();}
    private ExifTagDictionary(){}
}
