package it.edo.codecprobe;

import java.util.regex.*;

/** Presentation glossary only: explains the structure without guessing unparsed values. */
final class AnalysisLabels {
    static final class Label {
        final String name,meaning,kind;
        Label(String name,String meaning,String kind){this.name=name;this.meaning=meaning;this.kind=kind;}
    }
    private static Label data(String n,String m){return new Label(n,m,"Dati");}
    private static Label container(String n,String m){return new Label(n,m,"Contenitore");}
    private static Label reference(String n,String m){return new Label(n,m,"Indice / collegamento");}
    static Label of(AnalysisNode node){
        String t=node.title;
        switch(t){
            case "ftyp":return data("Formato e compatibilità","Dichiara il tipo di file e i formati che il lettore deve conoscere per aprirlo.");
            case "meta":return container("Indice di immagini e metadati","Contiene elenchi, proprietà e collegamenti. I byte delle immagini e degli EXIF possono trovarsi altrove, per esempio nell’area mdat.");
            case "mdat":return data("Area dei dati effettivi","Può contenere immagini, video, audio e metadati. Gli indici del file indicano a quale elemento appartiene ciascuna porzione. Questa bozza non separa ancora le singole porzioni.");
            case "moov":return container("Organizzazione del filmato","Descrive le tracce e come riprodurle. I dati compressi sono generalmente nell’area mdat.");
            case "trak":return container("Traccia del filmato","Raccoglie le informazioni di una traccia: per esempio video, audio o metadati.");
            case "mdia":return container("Informazioni sui media della traccia","Raccoglie tipo, tempi e organizzazione dei dati della traccia.");
            case "minf":return container("Organizzazione dei dati della traccia","Contiene informazioni e tabelle necessarie per trovare e leggere i campioni.");
            case "stbl":return container("Tabelle dei campioni","Indicano come individuare e riprodurre le porzioni di audio o video, chiamate campioni.");
            case "stsd":return data("Formato dei campioni","Descrive codec e parametri necessari per decodificare la traccia. Le descrizioni interne non sono ancora esplorabili.");
            case "stts":return reference("Tempi di decodifica","Tabella dei tempi con cui i campioni devono essere decodificati.");
            case "ctts":return reference("Tempi di presentazione","Indica gli scostamenti usati per ricostruire l’ordine di visualizzazione dei campioni.");
            case "stss":return reference("Punti di accesso al video","Elenca i campioni di sincronizzazione utilizzati per iniziare o riprendere la decodifica.");
            case "stsc":return reference("Campioni raggruppati in blocchi","Indica come sono distribuiti i campioni nei gruppi di dati della traccia.");
            case "stsz":case "stz2":return reference("Dimensioni dei campioni","Specifica quanto spazio occupano i singoli campioni.");
            case "stco":case "co64":return reference("Posizione dei gruppi di dati","Contiene gli offset necessari per trovare i gruppi di campioni nel file.");
            case "mvhd":return data("Dati generali del filmato","Intestazione con informazioni generali come scala temporale e durata dichiarata.");
            case "tkhd":return data("Dati generali della traccia","Intestazione della traccia: identificazione, durata e parametri di presentazione.");
            case "mdhd":return data("Tempi e lingua dei media","Intestazione con scala temporale, durata e lingua dichiarata della traccia.");
            case "hdlr":return data("Tipo di contenuto","Identifica il gestore del contenuto, per esempio video, audio o immagini.");
            case "iinf":return container("Elenco degli elementi","Contiene le descrizioni degli elementi identificati da un ID: immagini, miniature o metadati. È un elenco, non una copia dei loro dati.");
            case "infe":return data("Descrizione di un elemento","Descrive un elemento e il suo tipo. I byte dell’elemento si trovano usando l’indice delle posizioni.");
            case "iloc":return reference("Posizione e dimensione degli elementi","Collega gli ID degli elementi alle posizioni e alle lunghezze dei loro dati. I valori di questa tabella non sono ancora interpretati dalla bozza.");
            case "pitm":return reference("Scelta dell’immagine principale","Indica quale ID corrisponde all’immagine principale del file.");
            case "iref":return container("Collegamenti tra gli elementi","Contiene relazioni tra ID. Un collegamento non significa che i byte di un elemento siano contenuti nell’altro.");
            case "thmb":return reference("Collegamento della miniatura","Indica quale elemento è la miniatura di un altro. Questo blocco contiene il riferimento, non i pixel della miniatura.");
            case "cdsc":return reference("Metadati associati a un elemento","Collega un elemento descrittivo, per esempio EXIF, all’elemento che descrive. Non contiene necessariamente i suoi metadati.");
            case "dimg":return reference("Immagini usate per costruirne un’altra","Collega un’immagine derivata agli elementi da cui viene ricavata.");
            case "iprp":return container("Proprietà delle immagini","Contiene le proprietà disponibili e le associazioni con le immagini.");
            case "ipco":return container("Elenco delle proprietà","Raccoglie proprietà come dimensioni, colore e orientamento, poi assegnate agli elementi tramite un indice.");
            case "ipma":return reference("Proprietà assegnate agli elementi","Associa gli ID delle immagini alle proprietà presenti nell’elenco ipco.");
            case "ispe":return data("Dimensioni dell’immagine","Proprietà che dichiara larghezza e altezza in pixel.");
            case "pixi":return data("Precisione dei canali","Proprietà che descrive il numero di canali e i bit usati per ciascuno.");
            case "colr":return data("Descrizione del colore","Dichiara come interpretare i colori, tramite parametri numerici o un profilo colore ICC.");
            case "hvcC":return data("Configurazione del decoder HEVC","Contiene i parametri necessari al decoder HEVC; non è l’immagine compressa completa.");
            case "avcC":return data("Configurazione del decoder H.264","Contiene i parametri necessari per decodificare i campioni H.264.");
            case "av1C":return data("Configurazione del decoder AV1","Contiene i parametri di configurazione del decoder AV1.");
            case "irot":return data("Rotazione da applicare","Proprietà che indica al lettore come ruotare l’immagine per visualizzarla.");
            case "imir":return data("Specchiatura da applicare","Proprietà che indica una riflessione dell’immagine.");
            case "clap":return data("Area visibile dell’immagine","Descrive il ritaglio da usare per la visualizzazione.");
            case "pasp":return data("Proporzioni dei pixel","Dichiara il rapporto tra larghezza e altezza di un pixel.");
            case "udta":return container("Dati aggiuntivi del filmato","Raccoglie metadati aggiuntivi definiti dal formato o dall’applicazione.");
            case "dinf":return container("Riferimenti alle sorgenti","Raccoglie i riferimenti alle sorgenti dei dati della traccia.");
            case "dref":return reference("Elenco delle sorgenti dei dati","Indica dove risiedono i dati a cui la traccia fa riferimento.");
            case "edts":return container("Montaggio e intervalli della traccia","Raccoglie le regole che collegano la sequenza della traccia alla sequenza del filmato.");
            case "elst":return reference("Intervalli da riprodurre","Elenca gli intervalli della traccia utilizzati nella presentazione del filmato.");
            case "moof":return container("Organizzazione di un frammento","Descrive un frammento di un filmato organizzato in parti.");
            case "traf":return container("Traccia dentro un frammento","Raccoglie i dati organizzativi di una traccia per il frammento corrente.");
            case "mvex":return container("Impostazioni dei frammenti","Raccoglie impostazioni del filmato utilizzate dai frammenti.");
            case "tref":return container("Relazioni tra tracce","Raccoglie collegamenti tra gli identificativi delle tracce.");
            case "meco":return container("Metadati aggiuntivi","Raccoglie ulteriori contenitori di metadati.");
            case "sinf":return container("Informazioni di protezione","Raccoglie informazioni sulla protezione del contenuto.");
            case "schi":return container("Dettagli della protezione","Contiene dati specifici del sistema di protezione dichiarato.");
            case "uuid":return data("Dati definiti da un’applicazione","Blocco identificato da un UUID. Il significato dipende dall’applicazione che lo ha scritto; qui il contenuto resta opaco.");
            case "free":case "skip":case "wide":return data("Spazio di servizio del contenitore","Spazio di riempimento o di servizio. Non va confuso con i dati dell’immagine o del video.");
        }
        if(t.startsWith("APP")){
            if(t.contains("EXIF"))return container("Metadati della foto","EXIF contiene informazioni come data, fotocamera e orientamento. Può includere anche GPS, dati del produttore e una miniatura JPEG.");
            if(t.contains("ICC"))return data("Profilo colore","Questo segmento contiene una parte del profilo ICC, usato per interpretare i colori della foto.");
            if(t.contains("XMP"))return data("Metadati descrittivi","Contiene metadati XMP, in genere rappresentati come testo XML.");
            return data("Dati aggiuntivi del JPEG","Segmento applicativo. Il numero APP identifica la famiglia; una firma nel contenuto può specificarne il formato. Il contenuto qui non è interpretato.");
        }
        if(t.startsWith("SOI"))return data("Inizio della foto JPEG","Marcatore che indica dove inizia la struttura JPEG.");
        if(t.startsWith("EOI"))return data("Fine della foto JPEG","Marcatore che chiude il JPEG. Eventuali byte successivi sono mostrati separatamente.");
        if(t.startsWith("SOF"))return data("Parametri dell’immagine","Dichiara dimensioni, precisione e componenti dell’immagine JPEG.");
        if(t.startsWith("SOS"))return data("Intestazione della scansione","Descrive la scansione che segue. I byte compressi sono nel blocco separato dei dati dell’immagine.");
        if(t.startsWith("DQT"))return data("Tabelle di quantizzazione","Tabelle usate dal decoder per ricostruire i valori dell’immagine compressa.");
        if(t.startsWith("DHT"))return data("Tabelle di codifica Huffman","Tabelle che permettono al decoder di interpretare i simboli compressi del JPEG.");
        if(t.startsWith("Scansione"))return data("Dati dell’immagine compressa","Questi byte rappresentano l’immagine JPEG compressa. Non vengono decodificati in pixel dall’analisi.");
        if(t.startsWith("COM"))return data("Commento della foto","Segmento di commento JPEG. Il contenuto non viene interpretato automaticamente.");
        if(t.startsWith("Dati dopo EOI"))return data("Dati aggiunti dopo la foto","Byte aggiunti dopo la fine JPEG. Possono contenere dati proprietari, come Samsung SEF; questa bozza non ne separa ancora i record.");
        if(t.startsWith("TIFF"))return data("Intestazione degli EXIF","Indica l’ordine dei byte e dove trovare la prima directory dei tag EXIF.");
        if(t.contains(" · directory"))return data("Numero di tag della directory","Questa voce contiene il numero di tag della directory. Le voci dei tag e i loro valori sono mostrati separatamente qui sotto.");
        if(t.endsWith(" · IFD successivo"))return reference("Collegamento alla directory successiva","Contiene un offset relativo all’inizio TIFF; zero indica che non c’è un’altra directory in questa catena.");
        if(t.startsWith("Miniatura JPEG"))return data("Anteprima JPEG incorporata","I byte dell’immagine di anteprima presente negli EXIF, distinta dalla foto principale.");
        Matcher tag=Pattern.compile("\\[0x([0-9A-Fa-f]{4})\\]").matcher(t);
        if(tag.find()){
            int id=Integer.parseInt(tag.group(1),16);
            String directory=node.field!=null?node.field.directory:t.startsWith("GPS")||t.startsWith("Valore · GPS")?"GPS":t.startsWith("Interop")||t.startsWith("Valore · Interop")?"Interop":"IFD0";
            ExifTagDictionary.Entry entry=ExifTagDictionary.find(id,directory);
            String field=entry==null?"Tag EXIF non tradotto":entry.name;
            String meaning=entry==null?"Significato non presente nel dizionario; i byte restano consultabili.":entry.meaning+"\nNome del tag: "+entry.technical+".";
            if(t.startsWith("Valore"))return data("Valore di "+field,meaning+"\nQuesti sono i dati del valore, conservati fuori dalla voce di directory.");
            return new Label(field,meaning+"\nLa voce EXIF dichiara tipo, quantità e valore oppure posizione dei dati.","Voce EXIF");
        }
        if(t.equals("fLaC · firma"))return data("Inizio del file FLAC","Firma che identifica un file audio FLAC.");
        if(t.startsWith("STREAMINFO"))return data("Parametri dell’audio FLAC","Blocco con parametri fondamentali dell’audio, come frequenza, canali e precisione.");
        if(t.startsWith("PADDING"))return data("Spazio riservato ai metadati","Riempimento che consente di modificare i metadati senza riscrivere tutti i dati audio.");
        if(t.startsWith("VORBIS_COMMENT"))return data("Tag e testi del brano","Blocco che può contenere campi come titolo, artista, album e testi. I singoli campi non sono ancora separati.");
        if(t.startsWith("PICTURE"))return data("Immagine del brano","Blocco FLAC che descrive e contiene un’immagine, per esempio la copertina.");
        if(t.startsWith("SEEKTABLE"))return reference("Indice per spostarsi nell’audio","Tabella che aiuta il lettore a raggiungere punti specifici del brano.");
        if(t.startsWith("CUESHEET"))return data("Indice delle tracce audio","Informazioni di suddivisione del contenuto in tracce e punti di indice.");
        if(t.startsWith("APPLICATION"))return data("Dati audio definiti da un’applicazione","Metadati FLAC il cui contenuto dipende dall’applicazione che li ha scritti.");
        if(t.startsWith("ID3"))return data("Metadati del brano","Tag ID3 con possibili titolo, artista, album, copertina e testi. I singoli campi non sono ancora esplorabili.");
        if(t.startsWith("Audio e dati"))return data("Audio e dati rimanenti","Parte restante del file: dati audio compressi ed eventuali tag finali non ancora separati.");
        if(t.length()==4)return new Label("Blocco non riconosciuto","Il codice tecnico è visibile, ma questa bozza non ne interpreta il contenuto. I byte restano consultabili.","Contenuto non interpretato");
        return new Label(t,"",node.expandable()?"Contenitore":"Dati");
    }
    static String facts(AnalysisNode node){
        String text=node.description;Matcher type=Pattern.compile("Tipo TIFF (\\d+) · (\\d+) valori").matcher(text);
        if(type.find()){
            int id=Integer.parseInt(type.group(1));String kind;
            switch(id){case 1:kind="byte senza segno";break;case 2:kind="testo ASCII";break;case 3:kind="intero senza segno a 16 bit";break;case 4:kind="intero senza segno a 32 bit";break;case 5:kind="frazione di due interi senza segno";break;case 6:kind="byte con segno";break;case 7:kind="byte dal significato specifico del tag";break;case 8:kind="intero con segno a 16 bit";break;case 9:kind="intero con segno a 32 bit";break;case 10:kind="frazione di due interi con segno";break;case 11:kind="numero in virgola mobile a 32 bit";break;case 12:kind="numero in virgola mobile a 64 bit";break;default:kind="tipo non interpretato";}
            text=text.substring(0,type.start())+"Tipo del valore: "+kind+" (TIFF "+id+")\nNumero di valori: "+type.group(2)+text.substring(type.end());
        }
        text=text.replace("Valore a offset ","Dati del valore al byte ");
        Matcher value=Pattern.compile("(?:^|\\n)Valore: ([1-8])(?:$|\\n)").matcher(node.description);
        if(node.title.contains("[0x0112]")&&node.description.startsWith("Tipo TIFF 3 · 1 valori\n")&&value.find()){
            int v=Integer.parseInt(value.group(1));String[] meanings={"","Orientamento normale","Specchiatura orizzontale","Rotazione di 180°","Specchiatura verticale","Specchiatura e rotazione (orientamento EXIF 5)","Rotazione di 90° in senso orario","Specchiatura e rotazione (orientamento EXIF 7)","Rotazione di 90° in senso antiorario"};
            if(v>0&&v<meanings.length)text+="\nInterpretazione: "+meanings[v]+".";
        }
        return text;
    }
    private AnalysisLabels(){}
}
