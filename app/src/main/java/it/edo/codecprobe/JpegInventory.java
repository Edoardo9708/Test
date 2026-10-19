package it.edo.codecprobe;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Bounded, streaming JPEG inventory. Does not decode pixels or approve a conversion. */
final class JpegInventory {
    static final long MAX_FILE=512L*1024*1024;
    static final int MAX_METADATA=16*1024*1024,MAX_SEGMENTS=65536;
    static final class Block {
        final int marker;final long offset;final String kind,sha256;private final byte[] payload;
        Block(int marker,long offset,byte[] payload){this.marker=marker;this.offset=offset;this.payload=payload;kind=kind(marker,payload);sha256=hex(digest().digest(payload));}
        int size(){return payload.length;}
        String base64(){return Base64.getEncoder().encodeToString(payload);}
    }
    static final class Info {
        final int width,height,precision,components,scans;final long bytes,trailingBytes;final String sha256,trailingSha256;
        final List<Block> blocks;final boolean iccComplete;
        Info(int w,int h,int p,int c,int scans,long bytes,long trailing,String hash,String tail,List<Block> blocks){
            width=w;height=h;precision=p;components=c;this.scans=scans;this.bytes=bytes;trailingBytes=trailing;sha256=hash;trailingSha256=tail;
            this.blocks=Collections.unmodifiableList(new ArrayList<>(blocks));iccComplete=iccComplete(blocks);
        }
        int count(String kind){int count=0;for(Block block:blocks)if(block.kind.equals(kind))count++;return count;}
        int metadataBytes(){int bytes=0;for(Block block:blocks)bytes+=block.size();return bytes;}
    }
    static Info read(InputStream source)throws IOException {
        MessageDigest hash=digest();Reader r=new Reader(new DigestInputStream(new BufferedInputStream(source,65536),hash));
        if(r.next()!=0xff||r.next()!=0xd8)throw new IOException("Non è un JPEG: questa prima analisi supporta soltanto file JPEG, non HEIC o PNG.");
        List<Block> blocks=new ArrayList<>();int width=0,height=0,precision=0,components=0,scans=0,segments=0,metadata=0,pending=-1;
        while(true){
            int marker=pending<0?r.marker():pending;pending=-1;
            if(++segments>MAX_SEGMENTS)throw new IOException("Troppi segmenti JPEG: analisi interrotta.");
            if(marker==0xd9)break;
            if(marker==0xd8||marker==0||marker>=0xd0&&marker<=0xd7)throw new IOException("Sequenza di marcatori JPEG non valida.");
            if(marker==1)continue;
            int size=(r.next()<<8|r.next())-2;if(size<0)throw new IOException("Lunghezza segmento JPEG non valida.");
            long offset=r.position;boolean keep=marker>=0xe0&&marker<=0xef||marker==0xfe;
            if(keep&&(metadata+=size)>MAX_METADATA)throw new IOException("Metadati JPEG oltre il limite di 16 MiB: nessun inventario parziale esportato.");
            byte[] payload=r.bytes(size);
            if(keep)blocks.add(new Block(marker,offset,payload));
            if(isFrame(marker)){
                if(width!=0||payload.length<6)throw new IOException("Descrizione immagine JPEG multipla o incompleta.");
                precision=payload[0]&255;height=u16(payload,1);width=u16(payload,3);components=payload[5]&255;
                if(width==0||height==0||components==0||payload.length!=6+3*components)throw new IOException("Dimensioni o componenti JPEG non validi.");
            }
            if(marker==0xda){
                if(width==0||payload.length<4||payload.length!=4+2*(payload[0]&255))throw new IOException("Intestazione scansione JPEG non valida.");
                scans++;pending=r.entropyMarker();
            }
        }
        if(width==0||scans==0)throw new IOException("JPEG senza immagine o dati di scansione.");
        long start=r.position;MessageDigest trailing=digest();byte[] buffer=new byte[65536];int n;
        while((n=r.remaining(buffer))!=-1)trailing.update(buffer,0,n);
        return new Info(width,height,precision,components,scans,r.position,r.position-start,hex(hash.digest()),hex(trailing.digest()),blocks);
    }
    private static final class Reader {
        final InputStream in;long position;
        Reader(InputStream in){this.in=in;}
        void check()throws IOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Analisi annullata");if(position>MAX_FILE)throw new IOException("JPEG oltre il limite di 512 MiB.");}
        int next()throws IOException{if((position&65535)==0)check();int value=in.read();if(value<0)throw new EOFException("JPEG troncato: analisi incompleta.");position++;return value;}
        byte[] bytes(int size)throws IOException{byte[] data=new byte[size];for(int i=0;i<size;i++)data[i]=(byte)next();return data;}
        int marker()throws IOException{if(next()!=255)throw new IOException("Marcatore JPEG atteso.");int value;do{value=next();}while(value==255);return value;}
        int entropyMarker()throws IOException{while(true){if(next()!=255)continue;int value;do{value=next();}while(value==255);if(value==0||value>=0xd0&&value<=0xd7)continue;return value;}}
        int remaining(byte[] buffer)throws IOException{check();int size=in.read(buffer);if(size>0){position+=size;check();}return size;}
    }
    private static boolean isFrame(int marker){return marker>=0xc0&&marker<=0xcf&&marker!=0xc4&&marker!=0xc8&&marker!=0xcc;}
    private static int u16(byte[] data,int at){return (data[at]&255)<<8|(data[at+1]&255);}
    private static boolean starts(byte[] data,String text){byte[] prefix=text.getBytes(StandardCharsets.US_ASCII);if(data.length<prefix.length)return false;for(int n=0;n<prefix.length;n++)if(data[n]!=prefix[n])return false;return true;}
    private static String kind(int marker,byte[] data){
        if(marker==0xe1&&starts(data,"Exif\0\0"))return "EXIF";
        if(marker==0xe1&&starts(data,"http://ns.adobe.com/xap/1.0/\0"))return "XMP";
        if(marker==0xe1&&starts(data,"http://ns.adobe.com/xmp/extension/\0"))return "XMP esteso";
        if(marker==0xe2&&starts(data,"ICC_PROFILE\0"))return "ICC";
        if(marker==0xe2&&starts(data,"MPF\0"))return "MPF";
        if(marker==0xe0&&starts(data,"JFIF\0"))return "JFIF";
        if(marker==0xed&&starts(data,"Photoshop 3.0\0"))return "Photoshop";
        if(marker==0xee&&starts(data,"Adobe"))return "Adobe";
        return marker==0xfe?"Commento":"APP"+(marker-0xe0)+" non interpretato";
    }
    private static boolean iccComplete(List<Block> blocks){
        int count=0,total=0;Set<Integer> seen=new HashSet<>();
        for(Block b:blocks)if(b.kind.equals("ICC")){
            byte[] data=b.payload;if(data.length<14)return false;int index=data[12]&255,parts=data[13]&255;
            if(index==0||parts==0||index>parts||total!=0&&total!=parts||!seen.add(index))return false;
            total=parts;count++;
        }
        return count==0||count==total;
    }
    static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    static String hex(byte[] data){char[] hex="0123456789abcdef".toCharArray();StringBuilder out=new StringBuilder(data.length*2);for(byte b:data)out.append(hex[(b&255)>>>4]).append(hex[b&15]);return out.toString();}
    private JpegInventory(){}
}
