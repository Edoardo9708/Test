package it.edo.codecprobe;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Explains bytes using the actual container schema. All reads remain bounded and cancellable. */
final class AnalysisDetailReader {
    static AnalysisDetail read(AnalysisReader r,AnalysisNode n)throws IOException{
        r.within(n.offset,n.size,r.size);r.check();
        if(n.field!=null)return tiffField(r,n,n.field);
        if(n.title.length()==4&&n.size>=8&&r.ascii(n.offset+4,4).equals(n.title))return box(r,n);
        if(jpegTitle(n.title))return jpeg(r,n);
        if(n.title.startsWith("TIFF ·"))return tiffHeader(r,n);
        if(flacTitle(n.title)&&n.size>=4)return flac(r,n);
        if(n.title.startsWith("ID3v2."))return id3(r,n);
        List<AnalysisDetail.Part> parts=new ArrayList<>();parts.add(part(r,"Byte dell’elemento",n.offset,n.size,64,"Intervallo selezionato nel file. Qui non è ancora disponibile una scomposizione specifica del suo formato."));
        return detail(n,parts,"Questo elemento occupa "+n.size+" byte. La bozza ne mostra posizione e byte, ma non attribuisce un significato ai dati che non sa interpretare.","");
    }
    private static AnalysisDetail detail(AnalysisNode n,List<AnalysisDetail.Part> parts,String result,String alternatives){
        AnalysisLabels.Label label=AnalysisLabels.of(n);
        return new AnalysisDetail(label.name+"\nNome tecnico: "+n.title+"\n\n"+label.meaning+"\n\nPosizione nel file: byte "+n.offset+". Dimensione dell’elemento selezionato: "+n.size+" byte.",parts,result,alternatives);
    }
    private static AnalysisDetail.Part part(AnalysisReader r,String name,long offset,long size,int preview,String meaning)throws IOException{
        r.within(offset,size,r.size);int count=(int)Math.min(size,preview);String hex=hex(r.bytes(offset,count));
        if(size>count)meaning+="\nMostrati "+count+" di "+size+" byte; il resto è consultabile con Mostra byte HEX.";
        return new AnalysisDetail.Part(name,offset,size,hex,meaning);
    }
    static String hex(byte[] bytes){
        StringBuilder out=new StringBuilder();for(int i=0;i<bytes.length;i++){if(i>0)out.append(i%16==0?'\n':' ');out.append(String.format(Locale.ROOT,"%02X",bytes[i]&255));}return out.length()==0?"(nessun byte)":out.toString();
    }
    private static String ascii(byte[] bytes){
        StringBuilder out=new StringBuilder();for(byte b:bytes){int c=b&255;if(c==0)out.append("\\0");else if(c>=32&&c<127)out.append((char)c);else out.append(String.format(Locale.ROOT,"\\x%02X",c));}return out.toString();
    }
    private static AnalysisDetail tiffField(AnalysisReader r,AnalysisNode n,AnalysisField f)throws IOException{
        r.within(f.base,8,f.end);if(!r.ascii(f.base,2).equals(f.little?"II":"MM")||r.uint(f.base+2,2,f.little)!=42)throw new IOException("L’intestazione TIFF è cambiata. Rileggi il file.");
        r.within(f.entry,12,f.end);
        int tag=(int)r.uint(f.entry,2,f.little),type=(int)r.uint(f.entry+2,2,f.little);long count=r.uint(f.entry+4,4,f.little);
        if(tag!=f.tag||type!=f.type||count!=f.count)throw new IOException("La voce EXIF è cambiata. Rileggi il file.");
        long value=f.unit==0?f.entry+8:f.bytes>4?f.base+r.uint(f.entry+8,4,f.little):f.entry+8;
        if(f.unit>0){r.within(value,f.bytes,f.end);if(value!=f.value)throw new IOException("La posizione del valore EXIF è cambiata. Rileggi il file.");}
        List<AnalysisDetail.Part> parts=new ArrayList<>();
        String order=f.little?"little endian: il byte meno significativo viene prima":"big endian: il byte più significativo viene prima";
        String intro=(n.offset==f.entry?"Questa è una voce EXIF/TIFF classica di 12 byte.":"Hai selezionato i dati esterni del valore. Per spiegare come vengono trovati mostro anche la voce EXIF/TIFF di 12 byte al byte "+f.entry+".")+" I numeri sono memorizzati in ordine "+order+". L’ordine è dichiarato nell’intestazione TIFF del segmento, non in ogni tag.";
        parts.add(part(r,"1 · Identificativo del campo (tag)",f.entry,2,2,"Questi 2 byte, letti con l’ordine indicato sopra, valgono "+String.format(Locale.ROOT,"0x%04X",tag)+". Questo è l’identificativo del tag a cui appartiene il valore."));
        parts.add(part(r,"2 · Tipo del valore",f.entry+2,2,2,"Codice "+type+" = "+typeName(type)+(f.unit>0?". Ciascun valore occupa "+f.unit+" byte.":". La dimensione di questo tipo non è interpretata dalla bozza.")));
        parts.add(part(r,"3 · Quantità di valori",f.entry+4,4,4,"Il numero letto è "+count+". Non è direttamente la lunghezza in byte."+(f.unit>0?" Lunghezza dei dati = "+count+" × "+f.unit+" = "+f.bytes+" byte.":" La lunghezza dei dati non viene indovinata.")));
        if(f.unit==0){
            parts.add(part(r,"4 · Spazio per valore o indirizzo",f.entry+8,4,4,"Sono i 4 byte finali della voce. Senza conoscere la dimensione del tipo non possiamo stabilire se siano il valore o un indirizzo."));
            return tiffDetail(n,parts,intro,"Tipo non supportato: il contenuto di questi 4 byte resta non interpretato.","");
        }
        if(f.bytes<=4){
            parts.add(part(r,"4 · Valore dentro la voce",f.entry+8,f.bytes,4,"I dati occupano "+f.bytes+" byte, quindi entrano nello spazio di 4 byte previsto dalla voce. Sono memorizzati qui: questi byte non sono un indirizzo."));
            if(f.bytes<4)parts.add(part(r,"Spazio non usato della voce",f.entry+8+f.bytes,4-f.bytes,4,"Questi byte completano lo spazio di 4 byte. Non fanno parte del valore; non è necessario che siano tutti zero."));
        }else{
            long relative=r.uint(f.entry+8,4,f.little);
            parts.add(part(r,"4 · Indirizzo relativo dei dati",f.entry+8,4,4,"I dati richiedono "+f.bytes+" byte e non entrano nello spazio di 4 byte. Questi 4 byte contengono invece l’offset "+relative+", relativo all’inizio TIFF.\nInizio TIFF al byte "+f.base+" + offset "+relative+" = posizione assoluta "+value+" nel file."));
            parts.add(part(r,"5 · Dati raggiunti tramite l’indirizzo",value,f.bytes,64,"Questi sono i byte del valore effettivo, esterni alla voce di directory ma dentro lo stesso segmento EXIF."));
        }
        String result=valueText(r,f,value);
        String alternatives="";
        if(tag==0x112&&type==3&&count==1){
            long actual=r.uint(value,2,f.little);String meaning=orientation(actual);
            result+="\n\nVALORE TROVATO: "+actual+" → "+meaning+".";
            result+="\nIl tag descrive la trasformazione da applicare ai pixel. Da solo non dice se la foto è verticale o orizzontale: servono anche larghezza e altezza.";
            StringBuilder choices=new StringBuilder("VALORI POSSIBILI DELL’ORIENTAMENTO\n");for(int i=1;i<=8;i++)choices.append(i==actual?"→ ":"").append(i).append(" = ").append(orientation(i)).append(i==actual?"  [PRESENTE NEL FILE]":"").append('\n');alternatives=choices.toString();
        }else if(tag==0x128&&type==3&&count==1){
            long actual=r.uint(value,2,f.little);alternatives="VALORI POSSIBILI DELL’UNITÀ DI RISOLUZIONE\n1 = nessuna unità assoluta\n2 = pollici\n3 = centimetri\nNel file: "+actual+".";
        }else if(tag==0xa001&&type==3&&count==1){
            long actual=r.uint(value,2,f.little);alternatives="VALORI DEL TAG SPAZIO COLORE\n1 = sRGB\n65535 = non calibrato in questo tag\nNel file: "+actual+". Un eventuale profilo colore separato va controllato a parte.";
        }else if(f.gps&&type==2&&(tag==1||tag==3)){
            alternatives=tag==1?"VALORI POSSIBILI\nN = latitudine nord\nS = latitudine sud":"VALORI POSSIBILI\nE = longitudine est\nW = longitudine ovest";
        }
        return tiffDetail(n,parts,intro,result,alternatives);
    }
    private static AnalysisDetail tiffDetail(AnalysisNode node,List<AnalysisDetail.Part> parts,String intro,String result,String alternatives){
        AnalysisDetail base=detail(node,parts,result,alternatives);
        return new AnalysisDetail(base.introduction+"\n\n"+intro,parts,result,alternatives);
    }
    private static String typeName(int type){
        switch(type){case 1:return "BYTE (intero senza segno a 8 bit)";case 2:return "ASCII (testo, 1 byte per carattere)";case 3:return "SHORT (intero senza segno a 16 bit)";case 4:return "LONG (intero senza segno a 32 bit)";case 5:return "RATIONAL (frazione di due interi senza segno a 32 bit)";case 6:return "SBYTE (intero con segno a 8 bit)";case 7:return "UNDEFINED (byte dal significato specifico del tag)";case 8:return "SSHORT (intero con segno a 16 bit)";case 9:return "SLONG (intero con segno a 32 bit)";case 10:return "SRATIONAL (frazione di due interi con segno a 32 bit)";case 11:return "FLOAT (numero a virgola mobile a 32 bit)";case 12:return "DOUBLE (numero a virgola mobile a 64 bit)";default:return "tipo non interpretato";}
    }
    private static String valueText(AnalysisReader r,AnalysisField f,long value)throws IOException{
        if(f.type==2){int n=(int)Math.min(f.bytes,128);return "TESTO LETTO\nASCII: \""+ascii(r.bytes(value,n))+"\""+(f.bytes>n?" (anteprima parziale)":"")+". Il byte \\0 indica una terminazione del testo; anche quel byte rientra nella quantità dichiarata.";}
        if(f.type==7)return "VALORE\nIl tipo UNDEFINED non specifica il significato dei byte. Non vengono interpretati come testo o numeri per tentativi.";
        StringBuilder text=new StringBuilder("VALORI LETTI\n");long limit=Math.min(f.count,8);
        for(int i=0;i<limit;i++){if(i>0)text.append(", ");long p=value+(long)i*f.unit;switch(f.type){
            case 1:case 3:case 4:text.append(r.uint(p,f.unit,f.little));break;
            case 6:case 8:case 9:{long v=r.uint(p,f.unit,f.little),sign=1L<<(f.unit*8-1);text.append((v&sign)!=0?v-(1L<<(f.unit*8)):v);break;}
            case 5:case 10:{long a=r.uint(p,4,f.little),b=r.uint(p+4,4,f.little);if(f.type==10){a=(int)a;b=(int)b;}text.append(a).append('/').append(b);if(b!=0)text.append(" = ").append(String.format(Locale.ROOT,"%.6g",(double)a/b));else text.append(" (divisione non definita)");break;}
            case 11:case 12:{ByteBuffer b=ByteBuffer.wrap(r.bytes(p,f.unit)).order(f.little?ByteOrder.LITTLE_ENDIAN:ByteOrder.BIG_ENDIAN);text.append(f.type==11?Float.toString(b.getFloat()):Double.toString(b.getDouble()));break;}
            default:text.append("tipo non interpretato");
        }}if(f.count==0)text.append("(nessun valore)");if(f.count>limit)text.append("\nMostrati i primi ").append(limit).append(" di ").append(f.count).append(" valori.");return text.toString();
    }
    private static String orientation(long value){
        switch((int)value){case 1:return "normale, nessuna trasformazione";case 2:return "specchiatura orizzontale (destra e sinistra scambiate)";case 3:return "rotazione di 180°";case 4:return "specchiatura verticale (alto e basso scambiati)";case 5:return "trasposizione lungo la diagonale alto-sinistra / basso-destra";case 6:return "rotazione di 90° in senso orario";case 7:return "trasposizione lungo la diagonale alto-destra / basso-sinistra";case 8:return "rotazione di 90° in senso antiorario";default:return "valore non riconosciuto; nessuna trasformazione viene dedotta";}
    }
    private static AnalysisDetail box(AnalysisReader r,AnalysisNode n)throws IOException{
        List<AnalysisDetail.Part> parts=new ArrayList<>();long declared=r.uint(n.offset,4,false),header=8;String sizeRule="La dimensione di un box comprende intestazione e contenuto.";
        parts.add(part(r,"1 · Dimensione del box",n.offset,4,4,"Numero a 32 bit in big endian: "+declared+". "+(declared==1?"Il valore 1 annuncia una dimensione estesa a 64 bit.":declared==0?"Il valore 0 estende il box fino alla fine del contenitore che lo racchiude.":sizeRule)));
        parts.add(part(r,"2 · Identificativo ASCII",n.offset+4,4,4,"Questi 4 byte letti come ASCII formano \""+ascii(r.bytes(n.offset+4,4))+"\". Il codice identifica il tipo di box, non un valore numerico."));
        long size=declared;
        if(declared==1){r.within(n.offset,16,n.end());size=r.uint(n.offset+8,8,false);header=16;parts.add(part(r,"3 · Dimensione estesa",n.offset+8,8,8,"Numero a 64 bit in big endian: "+size+" byte. "+sizeRule));}
        else if(declared==0)size=n.size;
        if(n.title.equals("uuid")){r.within(n.offset+header,16,n.end());parts.add(part(r,"Identificativo dell’applicazione (UUID)",n.offset+header,16,16,"Identificativo binario di 16 byte che specifica il tipo di contenuto definito dall’applicazione."));header+=16;}
        if(size!=n.size||size<header)throw new IOException("La dimensione del box è cambiata. Rileggi il file.");
        long p=n.offset+header,payload=n.size-header;
        if(n.title.equals("ispe")&&payload>=12){
            parts.add(part(r,"Versione e flag",p,4,4,"Intestazione della proprietà delle dimensioni. Le coordinate successive sono interpretate qui solo per la versione 0."));
            if(r.u8(p)==0){parts.add(part(r,"Larghezza in pixel",p+4,4,4,"Intero a 32 bit big endian: "+r.uint(p+4,4,false)+" pixel."));parts.add(part(r,"Altezza in pixel",p+8,4,4,"Intero a 32 bit big endian: "+r.uint(p+8,4,false)+" pixel. Rotazione e ritaglio possono essere dichiarati in altre proprietà."));}
        }else if(n.title.equals("ftyp")&&payload>=8){
            parts.add(part(r,"Formato principale (major brand)",p,4,4,"Identificativo ASCII \""+ascii(r.bytes(p,4))+"\"."));
            parts.add(part(r,"Versione del formato",p+4,4,4,"Numero big endian: "+r.uint(p+4,4,false)+"."));
            if(payload>8)parts.add(part(r,"Formati compatibili",p+8,payload-8,64,"Sequenza di identificativi ASCII da 4 byte ciascuno. Non sono testo libero."));
        }else parts.add(part(r,"3 · Contenuto del box",p,payload,64,"Dimensione del contenuto = "+n.size+" − "+header+" = "+payload+" byte."+(n.expandable()?" Qui possono essere contenuti altri box, mostrati dalla mappa.":" Il significato dipende dal tipo del box; una scomposizione ulteriore non viene indovinata.")));
        return detail(n,parts,"NEL FILE\nIl box \""+n.title+"\" occupa "+n.size+" byte: "+header+" di intestazione e "+payload+" di contenuto. I numeri dell’intestazione sono in big endian, cioè il byte più significativo viene prima.","");
    }
    private static boolean jpegTitle(String t){return t.startsWith("APP")||t.startsWith("SOI")||t.startsWith("EOI")||t.startsWith("SOS")||t.startsWith("SOF")||t.startsWith("DQT")||t.startsWith("DHT")||t.startsWith("COM")||t.startsWith("Segmento FF")||t.startsWith("Marcatore FF");}
    private static AnalysisDetail jpeg(AnalysisReader r,AnalysisNode n)throws IOException{
        List<AnalysisDetail.Part> parts=new ArrayList<>();long p=n.offset;if(r.u8(p++)!=255)throw new IOException("Marcatore JPEG cambiato.");
        int code;int fill=0;do{r.within(p,1,n.end());code=r.u8(p++);if(++fill>256)throw new IOException("Troppi byte di riempimento nell’intestazione JPEG.");}while(code==255);
        parts.add(part(r,"1 · Marcatore del segmento",n.offset,p-n.offset,64,"Il codice FF"+String.format(Locale.ROOT,"%02X",code)+" identifica il tipo generale di segmento JPEG. Per un APP non basta a distinguere EXIF, XMP o altri dati: occorre leggere anche la firma nel contenuto."));
        if(code==0xd8||code==0xd9||code==1||(code>=0xd0&&code<=0xd7))return detail(n,parts,"Questo marcatore non ha un campo di lunghezza né un payload.","");
        r.within(p,2,n.end());long declared=r.u16(p);if(declared<2||p+declared!=n.end())throw new IOException("Lunghezza JPEG cambiata. Rileggi il file.");
        parts.add(part(r,"2 · Lunghezza dichiarata",p,2,2,"Numero big endian: "+declared+" byte. Include i 2 byte del campo lunghezza, ma esclude il marcatore precedente."));
        long payload=p+2,body=declared-2,signature=0;
        if(code==0xe1&&body>=6&&r.ascii(payload,6).equals("Exif\0\0"))signature=6;
        else if(code==0xe2&&body>=12&&r.ascii(payload,12).equals("ICC_PROFILE\0"))signature=12;
        else if(code==0xe1&&body>=29&&r.ascii(payload,29).equals("http://ns.adobe.com/xap/1.0/\0"))signature=29;
        if(signature>0)parts.add(part(r,"3 · Firma che identifica il contenuto",payload,signature,64,"ASCII: \""+ascii(r.bytes(payload,(int)signature))+"\". Questa firma specifica il formato dei dati presenti nel segmento."));
        parts.add(part(r,"Dati dopo l’intestazione"+(signature>0?" e la firma":""),payload+signature,body-signature,64,"Payload del segmento: "+declared+" − 2 = "+body+" byte"+(signature>0?", dei quali "+signature+" di firma e "+(body-signature)+" di dati successivi":".")+".")); 
        return detail(n,parts,"NEL FILE\nIl segmento occupa "+n.size+" byte. Per leggerlo: riconosci il marcatore, leggi la lunghezza e applica il formato indicato dall’eventuale firma. Non si cerca un segnale di fine dentro ai metadati.","");
    }
    private static AnalysisDetail tiffHeader(AnalysisReader r,AnalysisNode n)throws IOException{
        r.within(n.offset,8,n.end());String order=r.ascii(n.offset,2);if(!order.equals("II")&&!order.equals("MM"))throw new IOException("Intestazione TIFF cambiata.");boolean little=order.equals("II");List<AnalysisDetail.Part> parts=new ArrayList<>();
        parts.add(part(r,"Ordine dei byte",n.offset,2,2,"ASCII \""+order+"\": "+(little?"little endian, byte meno significativo prima.":"big endian, byte più significativo prima.")));
        parts.add(part(r,"Identificativo TIFF",n.offset+2,2,2,"Numero letto: "+r.uint(n.offset+2,2,little)+". Nel TIFF classico il valore atteso è 42."));
        long relative=r.uint(n.offset+4,4,little);parts.add(part(r,"Indirizzo della prima directory",n.offset+4,4,4,"Offset relativo "+relative+". Inizio TIFF "+n.offset+" + "+relative+" = byte "+(n.offset+relative)+" del file."));
        return detail(n,parts,"La prima directory raccoglie le voci dei tag. Ogni voce classica ha 12 byte e descrive identificativo, tipo, quantità e valore oppure indirizzo.","");
    }
    private static boolean flacTitle(String t){return t.startsWith("STREAMINFO")||t.startsWith("PADDING")||t.startsWith("APPLICATION")||t.startsWith("SEEKTABLE")||t.startsWith("VORBIS_COMMENT")||t.startsWith("CUESHEET")||t.startsWith("PICTURE")||t.startsWith("Blocco FLAC");}
    private static AnalysisDetail flac(AnalysisReader r,AnalysisNode n)throws IOException{
        List<AnalysisDetail.Part> parts=new ArrayList<>();int first=r.u8(n.offset);long length=r.uint(n.offset+1,3,false);if(length+4!=n.size)throw new IOException("Blocco FLAC cambiato.");
        parts.add(part(r,"Tipo e flag nello stesso byte",n.offset,1,1,"Bit più alto: "+((first>>>7)&1)+" → "+((first&128)!=0?"ultimo blocco di metadati":"seguono altri blocchi di metadati")+". I 7 bit rimanenti valgono "+(first&127)+" e identificano il tipo del blocco."));
        parts.add(part(r,"Lunghezza dei dati",n.offset+1,3,3,"Intero big endian a 24 bit: "+length+" byte. Esclude i 4 byte dell’intestazione."));
        parts.add(part(r,"Dati del blocco",n.offset+4,length,64,"Contenuto del blocco di metadati. I campi interni non sono ancora separati."));
        return detail(n,parts,"NEL FILE\nIl blocco occupa 4 + "+length+" = "+n.size+" byte.","");
    }
    private static AnalysisDetail id3(AnalysisReader r,AnalysisNode n)throws IOException{
        r.within(n.offset,10,n.end());if(!r.ascii(n.offset,3).equals("ID3"))throw new IOException("Firma ID3 cambiata.");List<AnalysisDetail.Part> parts=new ArrayList<>();long length=0;for(byte b:r.bytes(n.offset+6,4)){if((b&128)!=0)throw new IOException("Dimensione ID3 non valida.");length=(length<<7)|(b&127);}
        parts.add(part(r,"Firma ASCII",n.offset,3,3,"Identificativo \""+ascii(r.bytes(n.offset,3))+"\": indica un tag ID3."));
        parts.add(part(r,"Versione e revisione",n.offset+3,2,2,"Versione 2."+r.u8(n.offset+3)+", revisione "+r.u8(n.offset+4)+"."));
        parts.add(part(r,"Flag",n.offset+5,1,1,"Bit di opzioni del tag; qui sono mostrati senza attribuire significati non interpretati."));
        parts.add(part(r,"Lunghezza synchsafe",n.offset+6,4,4,"Ogni byte contribuisce solo con i suoi 7 bit inferiori. Il numero ricostruito è "+length+" byte: non si legge come un normale intero a 32 bit."));
        parts.add(part(r,"Dati dopo l’intestazione",n.offset+10,n.size-10,64,"Contiene i frame ID3 ed eventualmente un footer. La bozza non separa ancora i singoli frame."));
        return detail(n,parts,"La lunghezza ID3 esclude l’intestazione iniziale di 10 byte; un eventuale footer della versione 2.4 è conteggiato separatamente dal lettore.","");
    }
    private AnalysisDetailReader(){}
}
