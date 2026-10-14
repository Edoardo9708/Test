package it.edo.codecprobe;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Copies opaque descriptive MP4 atoms without changing encoded media offsets. */
final class Mp4Metadata {
    private static final Set<String> CONTAINERS=new HashSet<>(Arrays.asList("moov","trak","mdia","minf","stbl"));
    private static final Set<String> TAGS=new HashSet<>(Arrays.asList("udta","meta","uuid","meco"));
    private static final Set<String> ROOT_ALLOWED=new HashSet<>(Arrays.asList("ftyp","mdat","moov","free","skip","wide","udta","meta","uuid","meco"));
    private static final class Box {
        FileChannel file;long at,size;int header;String type;List<Box> children;byte[] patched;
        long payload(){if(patched!=null)return patched.length;if(children==null)return size-header;long n=0;for(Box b:children)n=Math.addExact(n,b.outputSize());return n;}
        int outputHeader(){return payload()>0xffffffffL-8?16:8;}
        long outputSize(){return payload()+outputHeader();}
    }
    private static final class Match {Box source,target;String path;Match(Box a,Box b,String p){source=a;target=b;path=p;}}
    private final BooleanSupplier cancel;private final Consumer<String> log;private int atoms;private long copied;private final List<Match> matches=new ArrayList<>();
    private Mp4Metadata(BooleanSupplier cancel,Consumer<String> log){this.cancel=cancel;this.log=log;}
    static void check(FileChannel input,BooleanSupplier cancel)throws IOException{
        Mp4Metadata p=new Mp4Metadata(cancel,s->{});p.roots(input);
    }
    static void copy(FileChannel source,File target,BooleanSupplier cancel,Consumer<String> log)throws Exception {
        new Mp4Metadata(cancel,log).run(source,target);
    }
    private void cancelled()throws IOException{if(cancel.getAsBoolean())throw new IOException("Copia metadati annullata");}
    private List<Box> parse(FileChannel file,long from,long end,int depth)throws IOException{
        if(depth>12)throw new IOException("Metadati MP4 troppo profondi");
        List<Box> result=new ArrayList<>();
        for(long at=from;at<end;){
            cancelled();if(end-at<8||++atoms>1000000)throw new IOException("Struttura MP4 non riconosciuta: copia integrale dei metadati non garantita");
            byte[] h=read(file,at,8);ByteBuffer b=ByteBuffer.wrap(h);long size=Integer.toUnsignedLong(b.getInt());byte[] type=new byte[4];b.get(type);int header=8;
            if(size==1){size=ByteBuffer.wrap(read(file,at+8,8)).getLong();header=16;}else if(size==0)size=end-at;
            if(size<header||size>end-at)throw new IOException("Box MP4 incompleto o trailer proprietario non supportato");
            Box box=new Box();box.file=file;box.at=at;box.size=size;box.header=header;box.type=new String(type,StandardCharsets.ISO_8859_1);
            if(CONTAINERS.contains(box.type))box.children=parse(file,at+header,at+size,depth+1);
            result.add(box);at+=size;
        }return result;
    }
    private void validateTags(Box b)throws IOException{
        if(b.type.equals("uuid")){
            byte[] known={(byte)0xbe,0x7a,(byte)0xcf,(byte)0xcb,(byte)0x97,(byte)0xa9,0x42,(byte)0xe8,(byte)0x9c,0x71,(byte)0x99,(byte)0x94,(byte)0x91,(byte)0xe3,(byte)0xaf,(byte)0xac};
            if(b.size-b.header<16||!Arrays.equals(read(b.file,b.at+b.header,16),known))throw new IOException("UUID proprietario non riconosciuto: impossibile garantire che non contenga riferimenti ai vecchi dati video");
        }
        if(b.type.equals("meta")||b.type.equals("udta")||b.type.equals("meco")){
            long begin=b.at+b.header;
            if(b.type.equals("meta")){
                if(b.size-b.header<4)throw new IOException("Meta box incompleto");
                int prefix=ByteBuffer.wrap(read(b.file,begin,4)).getInt();if(prefix==0||prefix==0x01000000)begin+=4;
            }
            List<Box> children=parse(b.file,begin,b.at+b.size,0);
            for(Box c:children){
                if(c.type.equals("iloc")||c.type.equals("idat")||c.type.equals("iref")||c.type.equals("pitm"))throw new IOException("Metadati a oggetti con riferimenti ai dati originali non ancora supportati ("+c.type+")");
                if(TAGS.contains(c.type))validateTags(c);
            }
        }
        if(b.children!=null)for(Box c:b.children)validateTags(c);
    }
    private List<Box> roots(FileChannel f)throws IOException{
        List<Box> roots=parse(f,0,f.size(),0);int movies=0;
        for(Box b:roots){if(!ROOT_ALLOWED.contains(b.type))throw new IOException("Blocco "+b.type+" non supportato per la conservazione integrale dei metadati");if(b.type.equals("moov"))movies++;}
        if(movies!=1)throw new IOException("La copia dei metadati richiede un MP4/MOV non frammentato con un solo moov");
        for(Box b:roots)validateTags(b);
        return roots;
    }
    private static Box first(List<Box> boxes,String type){for(Box b:boxes)if(b.type.equals(type))return b;return null;}
    private String handler(Box track)throws IOException{
        Box mdia=first(track.children,"mdia");Box h=mdia==null?null:first(mdia.children,"hdlr");
        if(h==null||h.size-h.header<12)throw new IOException("Tipo traccia MP4 non leggibile");
        return new String(read(h.file,h.at+h.header+8,4),StandardCharsets.ISO_8859_1);
    }
    private void merge(Box source,Box target,String path)throws IOException{
        if(source.children==null){if(source.type.equals("mvhd")||source.type.equals("tkhd")||source.type.equals("mdhd")||source.type.equals("hdlr"))patchHeader(source,target,path);return;}
        List<Box> tags=new ArrayList<>();for(Box b:source.children)if(TAGS.contains(b.type))tags.add(b);
        target.children.removeIf(b->TAGS.contains(b.type));
        if(!tags.isEmpty()){
            for(Box tag:tags){target.children.add(tag);matches.add(new Match(tag,tag,path+"/"+tag.type));copied+=tag.size;log.accept("COPIA BLOCCO "+path+"/"+tag.type+" · "+tag.size+" byte (contenuto originale)");}
        }
        Map<String,Integer> occurrence=new HashMap<>();
        for(Box b:source.children){
            if(TAGS.contains(b.type))continue;
            Box paired=null;
            if(b.type.equals("trak")){
                String kind=handler(b);int n=occurrence.getOrDefault(kind,0);occurrence.put(kind,n+1);int seen=0;
                for(Box t:target.children)if(t.type.equals("trak")&&handler(t).equals(kind)){if(seen++==n){paired=t;break;}}
                if(paired==null)throw new IOException("Metadati della traccia "+kind+" non conservabili: manca la traccia nel risultato");
            }else paired=first(target.children,b.type);
            if(paired!=null&&(CONTAINERS.contains(b.type)||b.type.equals("mvhd")||b.type.equals("tkhd")||b.type.equals("mdhd")||b.type.equals("hdlr")))merge(b,paired,path+"/"+b.type);
            else if(CONTAINERS.contains(b.type)&&hasTags(b))throw new IOException("Metadati in "+path+"/"+b.type+" non conservabili");
        }
    }
    private boolean hasTags(Box b){if(b.children==null)return false;for(Box c:b.children)if(TAGS.contains(c.type)||hasTags(c))return true;return false;}
    private void patchHeader(Box a,Box b,String path)throws IOException{
        if(a.type.equals("hdlr")){
            if(a.size-a.header>1024*1024)throw new IOException("Nome/handler della traccia troppo grande");
            b.patched=read(a.file,a.at+a.header,(int)(a.size-a.header));log.accept("COPIA NOME/HANDLER "+path);return;
        }
        if(a.size-a.header>256||b.size-b.header>256)throw new IOException("Header MP4 non standard");
        byte[] in=read(a.file,a.at+a.header,(int)(a.size-a.header)),out=read(b.file,b.at+b.header,(int)(b.size-b.header));
        int av=in[0]&255,bv=out[0]&255;if(av>1||bv>1)throw new IOException("Versione header MP4 non supportata");
        int alen=av==1?8:4,blen=bv==1?8:4;
        for(int i=0;i<2;i++){
            long value=alen==8?ByteBuffer.wrap(in,4+i*alen,alen).getLong():Integer.toUnsignedLong(ByteBuffer.wrap(in,4+i*alen,alen).getInt());
            if(value<0||(blen==4&&value>0xffffffffL))throw new IOException("Data MP4 non rappresentabile nel nuovo header");
            ByteBuffer dst=ByteBuffer.wrap(out);if(blen==8)dst.putLong(4+i*blen,value);else dst.putInt(4+i*blen,(int)value);
        }
        if(a.type.equals("mdhd")){int ai=av==1?32:20,bi=bv==1?32:20;if(in.length<ai+2||out.length<bi+2)throw new IOException("Lingua MP4 non leggibile");out[bi]=in[ai];out[bi+1]=in[ai+1];}
        // Preserve playback flags/transforms while retaining new IDs, timing and dimensions.
        if(a.type.equals("tkhd")||a.type.equals("mvhd")){
            for(int i=1;i<4;i++)out[i]=in[i];
            int aa=(a.type.equals("tkhd")?32:20)+(av==1?12:0),bb=(b.type.equals("tkhd")?32:20)+(bv==1?12:0);
            int am=(a.type.equals("tkhd")?40:36)+(av==1?12:0),bm=(b.type.equals("tkhd")?40:36)+(bv==1?12:0);
            if(in.length<am+36||out.length<bm+36)throw new IOException("Trasformazione MP4 incompleta");
            System.arraycopy(in,aa,out,bb,6);System.arraycopy(in,am,out,bm,36);
        }
        b.patched=out;log.accept("COPIA DATE "+path+(a.type.equals("mdhd")?" + lingua":""));
    }
    private void run(FileChannel source,File target)throws Exception{
        log.accept("*** CONSERVAZIONE METADATI MP4 ORIGINALI ***");
        List<Box> input=roots(source);
        try(RandomAccessFile destination=new RandomAccessFile(target,"rw")){
            FileChannel file=destination.getChannel();List<Box> output=roots(file);Box old=first(output,"moov"),original=first(input,"moov");
            merge(original,old,"moov");long start=file.size();file.position(start);write(old,file);
            for(Box b:input)if(TAGS.contains(b.type)){write(b,file);matches.add(new Match(b,b,"root/"+b.type));copied+=b.size;log.accept("COPIA BLOCCO root/"+b.type+" · "+b.size+" byte");}
            // Old movie atom becomes padding: all mdat/stco/co64 media offsets stay fixed.
            free(old,file);for(Box b:output)if(TAGS.contains(b.type))free(b,file);
            file.force(true);
            List<Box> result=roots(file);Box movie=first(result,"moov");verifyTree(original,movie,"moov");
            for(String type:TAGS){List<Box> src=of(input,type),dst=of(result,type);verifyOpaque(src,dst,"root/"+type);}
            log.accept("OK METADATI verificati byte per byte: "+copied+" byte descrittivi; date e lingue originali conservate. Indici e codec rigenerati.");
        }
    }
    private void verifyTree(Box a,Box b,String path)throws Exception{
        for(String type:TAGS)verifyOpaque(of(a.children,type),of(b.children,type),path+"/"+type);
        Map<String,Integer> used=new HashMap<>();
        for(Box x:a.children){
            if(TAGS.contains(x.type))continue;Box y=null;
            if(x.type.equals("trak")){String kind=handler(x);int n=used.getOrDefault(kind,0);used.put(kind,n+1);int seen=0;for(Box t:b.children)if(t.type.equals("trak")&&handler(t).equals(kind)&&seen++==n){y=t;break;}}
            else y=first(b.children,x.type);
            if(y!=null&&CONTAINERS.contains(x.type))verifyTree(x,y,path+"/"+x.type);
            if(y!=null&&(x.type.equals("mvhd")||x.type.equals("tkhd")||x.type.equals("mdhd")||x.type.equals("hdlr"))){byte[] expected=read(y.file,y.at+y.header,(int)(y.size-y.header));Box tmp=new Box();tmp.file=y.file;tmp.at=y.at;tmp.size=y.size;tmp.header=y.header;tmp.type=y.type;patchHeader(x,tmp,path);if(!Arrays.equals(expected,tmp.patched))throw new IOException("Verifica date/lingua fallita");}
        }
    }
    private static List<Box> of(List<Box> boxes,String type){List<Box> out=new ArrayList<>();for(Box b:boxes)if(b.type.equals(type))out.add(b);return out;}
    private void verifyOpaque(List<Box> a,List<Box> b,String path)throws Exception{
        if(a.size()!=b.size())throw new IOException("Conteggio metadati diverso: "+path);
        for(int i=0;i<a.size();i++)if(a.get(i).size-a.get(i).header!=b.get(i).size-b.get(i).header||!Arrays.equals(hash(a.get(i)),hash(b.get(i))))throw new IOException("Contenuto metadati diverso: "+path);
    }
    private byte[] hash(Box b)throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");ByteBuffer buffer=ByteBuffer.allocate(65536);long at=b.at+b.header,left=b.size-b.header;while(left>0){cancelled();buffer.clear();buffer.limit((int)Math.min(buffer.capacity(),left));b.file.position(at);int n=b.file.read(buffer);if(n<=0)throw new EOFException();digest.update(buffer.array(),0,n);at+=n;left-=n;}return digest.digest();}
    private void free(Box b,FileChannel out)throws IOException{ByteBuffer h=ByteBuffer.allocate(b.header);if(b.header==16)h.putInt(1);else h.putInt((int)b.size);h.put("free".getBytes(StandardCharsets.ISO_8859_1));if(b.header==16)h.putLong(b.size);h.flip();out.position(b.at);put(out,h);}
    private void write(Box b,FileChannel out)throws IOException{
        cancelled();int head=b.outputHeader();long size=b.outputSize();ByteBuffer h=ByteBuffer.allocate(head);h.putInt(head==16?1:(int)size);h.put(b.type.getBytes(StandardCharsets.ISO_8859_1));if(head==16)h.putLong(size);h.flip();put(out,h);
        if(b.children!=null){for(Box c:b.children)write(c,out);}
        else if(b.patched!=null)put(out,ByteBuffer.wrap(b.patched));
        else{ByteBuffer buffer=ByteBuffer.allocate(65536);long at=b.at+b.header,left=b.size-b.header;while(left>0){cancelled();buffer.clear();buffer.limit((int)Math.min(buffer.capacity(),left));b.file.position(at);int n=b.file.read(buffer);if(n<=0)throw new EOFException();buffer.flip();if(b.file==out)out.position(out.size());put(out,buffer);at+=n;left-=n;}}
    }
    private static void put(FileChannel out,ByteBuffer b)throws IOException{while(b.hasRemaining())out.write(b);}
    private static byte[] read(FileChannel f,long at,int size)throws IOException{ByteBuffer b=ByteBuffer.allocate(size);f.position(at);while(b.hasRemaining())if(f.read(b)<=0)throw new EOFException();return b.array();}
}
