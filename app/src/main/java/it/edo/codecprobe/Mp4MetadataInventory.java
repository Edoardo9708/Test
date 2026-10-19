package it.edo.codecprobe;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Read-only atom inventory. It does not apply the converter's metadata-copy whitelist. */
final class Mp4MetadataInventory {
    private static final Set<String> CONTAINERS=new HashSet<>(Arrays.asList("moov","trak","mdia","minf","stbl","udta","meta","ilst","meco","mvex","moof","traf","edts","dinf"));
    private static final Set<String> MEDIA=new HashSet<>(Arrays.asList("mdat","idat"));
    private static final Set<String> DESCRIPTIVE=new HashSet<>(Arrays.asList("udta","meta","ilst","meco"));
    private static final long HASH_BUDGET=32L*1024*1024;
    private final FileChannel file;private final Writer out;private final BooleanSupplier cancel;
    private int count;private long hashed,sefExported;
    private Mp4MetadataInventory(FileChannel file,Writer out,BooleanSupplier cancel){this.file=file;this.out=out;this.cancel=cancel;}
    static void write(FileChannel file,Writer out,BooleanSupplier cancel)throws Exception{
        out.write("*** INVENTARIO MP4/MOV · BLOCCHI E CAMPI ORIGINALI ***\nDimensione file="+file.size()+" byte\nOffset e dimensioni sono strutturali: possono cambiare dopo la conversione. Gli SHA-256 riguardano i payload, escluso l’header del blocco.\n");
        Mp4MetadataInventory inventory=new Mp4MetadataInventory(file,out,cancel);
        // Export root SEF first: nested metadata parsing cannot hide the diagnostic bytes.
        inventory.exportRootSef();
        inventory.scan(0,file.size(),"root",0);
    }
    private void check()throws IOException{if(cancel.getAsBoolean())throw new InterruptedIOException("Analisi metadati annullata");}
    private byte[] read(long at,int size)throws IOException{
        check();ByteBuffer bytes=ByteBuffer.allocate(size);
        while(bytes.hasRemaining()){check();if(file.read(bytes,at+bytes.position())<=0)throw new EOFException("Campo incompleto a "+at);}
        return bytes.array();
    }
    private void scan(long from,long end,String parent,int depth)throws Exception{
        if(depth>16)throw new IOException("Limite profondità inventario: 16 · rapporto parziale");
        Map<String,Integer> occurrences=new HashMap<>();
        for(long at=from;at<end;){
            check();if(++count>20000||end-at<8)throw new IOException("Struttura/trailer non interpretabile o limite di 20000 blocchi a offset "+at+" · rapporto parziale");
            ByteBuffer head=ByteBuffer.wrap(read(at,8));long size=Integer.toUnsignedLong(head.getInt());byte[] code=new byte[4];head.get(code);
            String type=new String(code,StandardCharsets.ISO_8859_1);int header=8;
            if(size==1){if(end-at<16)throw new IOException("Header esteso incompleto");size=ByteBuffer.wrap(read(at+8,8)).getLong();header=16;}else if(size==0)size=end-at;
            if(size<header||size>end-at)throw new IOException("Dimensione blocco non valida a "+at+" · rapporto parziale");
            int occurrence=occurrences.merge(type,1,Integer::sum);String path=parent+"/"+escape(type)+"["+occurrence+"]";
            long payload=size-header,start=at+header;
            out.write("\nBLOCCO "+path+" · offset="+at+" · byte="+size+" · payload="+payload+"\n");
            if(MEDIA.contains(type)){out.write("CONTENUTO MULTIMEDIALE SALTATO · nessun fotogramma o pacchetto esportato\n");at+=size;continue;}
            boolean item=parent.endsWith("/ilst[1]")||parent.matches(".*/ilst\\[[0-9]+\\]$");
            if(CONTAINERS.contains(type)||item){
                if(DESCRIPTIVE.contains(type))hash(start,payload);
                long skip=type.equals("meta")?metaSkip(start,payload):0;
                if(payload<skip)throw new IOException("Contenitore meta incompleto");
                scan(start+skip,at+size,path,depth+1);
            }else{
                hash(start,payload);
                byte[] preview=read(start,(int)Math.min(payload,256));
                out.write("ANTEPRIMA · primi "+preview.length+" / "+payload+" byte\nHEX="+hex(preview)+"\nTESTO UTF-8="+text(preview)+"\n");
                if(type.equals("mvhd")||type.equals("tkhd")||type.equals("mdhd"))dates(preview);
                if(type.equals("data")&&preview.length>=8){
                    int format=ByteBuffer.wrap(preview).getInt()&0xffffff;
                    out.write("DATA · tipo="+format+" · locale="+Integer.toUnsignedLong(ByteBuffer.wrap(preview).getInt(4))+"\n");
                    if(format==1)out.write("VALORE UTF-8="+text(Arrays.copyOfRange(preview,8,preview.length))+"\n");
                    if(format==2)out.write("VALORE UTF-16BE="+escape(new String(preview,8,preview.length-8,StandardCharsets.UTF_16BE))+"\n");
                }
                if(type.equals("keys"))keys(start,payload);
                if(type.equals("sefd"))out.write("SEF completo: vedere la sezione iniziale esportata prima dell’inventario annidato.\n");
            }
            at+=size;
        }
    }
    private void exportRootSef()throws Exception{
        long end=file.size();int roots=0;
        for(long at=0;at<end;){
            check();if(end-at<8||++roots>20000)throw new IOException("Struttura principale non valida durante ricerca SEF a "+at);
            ByteBuffer h=ByteBuffer.wrap(read(at,8));long size=Integer.toUnsignedLong(h.getInt());byte[] type=new byte[4];h.get(type);int header=8;
            if(size==1){if(end-at<16)throw new IOException("Header principale esteso incompleto");size=ByteBuffer.wrap(read(at+8,8)).getLong();header=16;}else if(size==0)size=end-at;
            if(size<header||size>end-at)throw new IOException("Dimensione blocco non valida durante ricerca SEF a "+at);
            if(Arrays.equals(type,new byte[]{'s','e','f','d'}))sef(at,size);
            at+=size;
        }
    }
    private long metaSkip(long start,long payload)throws IOException{
        if(payload<4)throw new IOException("Contenitore meta incompleto");
        if(payload>=8){
            ByteBuffer first=ByteBuffer.wrap(read(start,8));long size=Integer.toUnsignedLong(first.getInt());byte[] code=new byte[4];first.get(code);
            boolean type=true;for(byte value:code)if(value<32||value>126)type=false;
            if(type&&((size>=8&&size<=payload)||size==1&&payload>=16)){
                out.write("META QUICKTIME · figli senza prefisso versione/flag\n");return 0;
            }
        }
        out.write("META ISO · prefisso versione/flag di 4 byte\n");return 4;
    }
    private void dates(byte[] bytes)throws IOException{
        if(bytes.length<12)throw new IOException("Header date incompleto");
        int version=bytes[0]&255,length=version==1?8:4;
        if(version>1||bytes.length<4+length*2){out.write("DATE · versione header non interpretabile="+version+"\n");return;}
        ByteBuffer data=ByteBuffer.wrap(bytes);
        long created=length==8?data.getLong(4):Integer.toUnsignedLong(data.getInt(4));
        long changed=length==8?data.getLong(12):Integer.toUnsignedLong(data.getInt(8));
        out.write("DATE HEADER · versione="+version+" · byte per campo="+length+"\nCREAZIONE · secondi dal 1904="+created+" · UTC="+CreationDatePolicy.displayMp4(created)+"\nMODIFICA · secondi dal 1904="+changed+" · UTC="+CreationDatePolicy.displayMp4(changed)+"\n");
    }
    private void keys(long at,long size)throws Exception{
        if(size<8)return;long count=Integer.toUnsignedLong(ByteBuffer.wrap(read(at+4,4)).getInt()),position=at+8;
        out.write("CHIAVI mdta · dichiarate="+count+"\n");
        for(long n=0;n<count;n++){
            check();if(n>=512){out.write("LIMITE · prime 512 chiavi riportate; altre omesse\n");break;}
            if(position>at+size-8)throw new IOException("Tabella keys incompleta");
            ByteBuffer head=ByteBuffer.wrap(read(position,8));long length=Integer.toUnsignedLong(head.getInt());byte[] namespace=new byte[4];head.get(namespace);
            if(length<8||length>at+size-position)throw new IOException("Chiave mdta fuori dal blocco");
            int shown=(int)Math.min(length-8,1024);
            out.write("KEY id="+(n+1)+" · namespace="+text(namespace)+" · nome="+text(read(position+8,shown))+(shown<length-8?" [nome troncato]":"")+"\n");position+=length;
        }
    }
    private void sef(long at,long size)throws Exception{
        if(size>SamsungSef.MAX_BYTES){out.write("SEF · oltre 4 MiB: blocco completo non esportato; vedere hash/anteprima\n");return;}
        if(size>SamsungSef.MAX_BYTES-sefExported){out.write("SEF · budget esportazione completa 4 MiB per file raggiunto; blocco non esportato\n");return;}
        byte[] bytes=read(at,(int)size);sefExported+=size;
        out.write("*** SEF BLOCCO COMPLETO · offset="+at+" · byte="+size+" · SHA256="+digest(ByteBuffer.wrap(bytes))+" ***\n");
        out.write("BASE64: concatenare i valori delle righe SEF BASE64 nell’ordine indicato. L’indice è l’offset in byte nel blocco originale, header sefd incluso.\n");
        Base64.Encoder encoder=Base64.getEncoder();
        for(int offset=0;offset<bytes.length;offset+=768){
            check();out.write("SEF BASE64 "+offset+"="+encoder.encodeToString(Arrays.copyOfRange(bytes,offset,Math.min(bytes.length,offset+768)))+"\n");
        }
        out.write("*** FINE SEF BLOCCO COMPLETO ***\n");
        SamsungSefDirectory directory;
        try{directory=SamsungSefDirectory.parse(bytes);}
        catch(IOException error){out.write("SEF NON INTERPRETABILE · "+escape(error.getMessage())+" · bytes completi disponibili sopra\n");return;}
        out.write("SAMSUNG SEF · versione="+directory.version+" · record="+directory.records.size()+" · sola lettura, non autorizza la copia\n");
        for(SamsungSefDirectory.Record record:directory.records){
            check();out.write("SEF RECORD "+record.number+" · nome="+escape(record.name)+" · tipo="+SamsungSefDiagnostics.type(record.type)+" · payload byte="+record.payloadBytes()+"\n");
            out.write("SEF PAYLOAD OFFSET="+record.payload+" · fine="+record.end+" · relativi al blocco completo\n");
            out.write("SEF SHA256="+digest(ByteBuffer.wrap(record.bytes,record.payload,record.payloadBytes()))+"\n");
            int shown=Math.min(record.payloadBytes(),65536);
            out.write("SEF TESTO "+(shown==record.payloadBytes()?"COMPLETO":"ANTEPRIMA · primi "+shown+" byte · dati completi nel BASE64")+"="+text(Arrays.copyOfRange(record.bytes,record.payload,record.payload+shown))+"\n");
        }
    }
    private void hash(long at,long size)throws Exception{
        if(size>HASH_BUDGET-hashed){out.write("SHA256 NON CALCOLATO · budget lettura payload 32 MiB raggiunto/blocco troppo grande\n");return;}
        MessageDigest digest=MessageDigest.getInstance("SHA-256");ByteBuffer buffer=ByteBuffer.allocate(65536);long left=size;
        while(left>0){check();buffer.clear();buffer.limit((int)Math.min(buffer.capacity(),left));int n=file.read(buffer,at+size-left);if(n<=0)throw new EOFException();digest.update(buffer.array(),0,n);left-=n;}
        hashed+=size;out.write("SHA256 PAYLOAD="+hex(digest.digest())+"\n");
    }
    static String digest(ByteBuffer data)throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");digest.update(data.duplicate());return hex(digest.digest());}
    static String hex(byte[] data){StringBuilder out=new StringBuilder(data.length*2);for(byte value:data)out.append(Character.forDigit((value&255)>>4,16)).append(Character.forDigit(value&15,16));return out.toString();}
    static String text(byte[] data){return escape(new String(data,StandardCharsets.UTF_8));}
    static String escape(String value){if(value==null)return "null";StringBuilder out=new StringBuilder();for(int i=0;i<value.length();i++){char c=value.charAt(i);if(c=='\n')out.append("\\n");else if(c=='\r')out.append("\\r");else if(c=='\t')out.append("\\t");else if(Character.isISOControl(c))out.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}return out.toString();}
}
