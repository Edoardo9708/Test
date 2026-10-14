package it.edo.codecprobe;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Read-only diagnostic export; encoded video/audio samples are skipped by seeking. */
final class MetadataDump {
    private static final Set<String> CONTAINERS=new HashSet<>(Arrays.asList("moov","trak","mdia","minf","stbl"));
    private static final Set<String> COPY=new HashSet<>(Arrays.asList("ftyp","udta","meta","uuid","meco","sefd"));
    private final FileChannel file;private final Writer out;private final BooleanSupplier cancel;
    private long exported;private int count;
    private MetadataDump(FileChannel f,Writer w,BooleanSupplier c){file=f;out=w;cancel=c;}
    static void write(FileChannel file,Writer out,BooleanSupplier cancel)throws Exception{
        MetadataDump d=new MetadataDump(file,out,cancel);
        out.write("*** ESPORTAZIONE DIAGNOSTICA METADATI ORIGINALI ***\n");
        out.write("Dimensione originale: "+file.size()+" byte\n");
        out.write("I campioni audio/video non sono inclusi. I blocchi esportati sono originali; HEX include l’header MP4. Nessuna modifica al video.\n\n");
        d.scan(0,file.size(),"root",0);out.write("\nRIEPILOGO: "+d.exported+" byte originali esportati; "+d.count+" blocchi ispezionati.\n");
    }
    private void check()throws IOException{if(cancel.getAsBoolean())throw new IOException("Esportazione metadati annullata");}
    private byte[] read(long at,int n)throws IOException{
        check();ByteBuffer b=ByteBuffer.allocate(n);
        while(b.hasRemaining()){check();int got=file.read(b,at+b.position());if(got<=0)throw new IOException("Metadati incompleti");}return b.array();
    }
    private void scan(long from,long end,String path,int depth)throws Exception{
        if(depth>12)throw new IOException("Metadati troppo profondi");
        for(long at=from;at<end;){
            check();if(end-at<8||++count>1000000)throw new IOException("Struttura MP4 o trailer non riconosciuto a "+at);
            ByteBuffer b=ByteBuffer.wrap(read(at,8));long size=Integer.toUnsignedLong(b.getInt());byte[] t=new byte[4];b.get(t);int header=8;
            if(size==1){size=ByteBuffer.wrap(read(at+8,8)).getLong();header=16;}else if(size==0)size=end-at;
            if(size<header||size>end-at)throw new IOException("Box MP4 incompleto a "+at);
            String type=new String(t,StandardCharsets.ISO_8859_1);String where=path+"/"+type;
            out.write("BLOCCO "+where+" · offset="+at+" · dimensione="+size+" byte\n");
            if(COPY.contains(type)){
                if(size>4*1024*1024L||exported+size>8*1024*1024L)throw new IOException("Blocchi diagnostici oltre il limite di 8 MB: esportazione interrotta");
                byte[] data=read(at,(int)size);exported+=size;
                out.write("SHA256="+hex(MessageDigest.getInstance("SHA-256").digest(data))+"\n");
                out.write("HEX_BEGIN\n");for(int i=0;i<data.length;i+=32){check();out.write(hex(Arrays.copyOfRange(data,i,Math.min(data.length,i+32)))+"\n");}out.write("HEX_END\n\n");
            }else if(CONTAINERS.contains(type))scan(at+header,at+size,where,depth+1);
            else if(type.equals("mdat"))out.write("Campioni audio/video saltati: non esportati.\n\n");
            at+=size;
        }
    }
    private static String hex(byte[] a){char[] h="0123456789abcdef".toCharArray(),out=new char[a.length*2];for(int i=0;i<a.length;i++){out[i*2]=h[(a[i]&255)>>4];out[i*2+1]=h[a[i]&15];}return new String(out);}
}
