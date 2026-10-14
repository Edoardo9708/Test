package it.edo.codecprobe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Reject conflicting MP4 color overrides rather than silently discarding them. */
final class ContainerColor {
    private final FileChannel file;private final ColorVui color;private int count;
    private ContainerColor(FileChannel f,ColorVui c){file=f;color=c;}
    static void check(FileChannel f,ColorVui c)throws IOException{new ContainerColor(f,c).scan(0,f.size(),0);}
    private byte[] read(long at,int n)throws IOException{
        ByteBuffer b=ByteBuffer.allocate(n);while(b.hasRemaining()){int got=file.read(b,at+b.position());if(got<=0)throw new IOException("Contenitore colore troncato");}return b.array();
    }
    private void scan(long from,long end,int depth)throws IOException{
        if(depth>12)throw new IOException("Contenitore colore troppo profondo");
        for(long at=from;at<end;){
            if(end-at<8||++count>1000000)throw new IOException("Struttura MP4 non valida durante il controllo colore");
            ByteBuffer h=ByteBuffer.wrap(read(at,8));long n=Integer.toUnsignedLong(h.getInt());byte[] tb=new byte[4];h.get(tb);String t=new String(tb,StandardCharsets.ISO_8859_1);int header=8;
            if(n==1){n=ByteBuffer.wrap(read(at+8,8)).getLong();header=16;}else if(n==0)n=end-at;
            if(n<header||n>end-at)throw new IOException("Box MP4 incompleto durante il controllo colore");
            long begin=at+header;
            if(Arrays.asList("moov","trak","mdia","minf","stbl").contains(t))scan(begin,at+n,depth+1);
            else if(t.equals("stsd"))scan(begin+8,at+n,depth+1);
            else if(Arrays.asList("avc1","avc3","hvc1","hev1").contains(t))scan(begin+78,at+n,depth+1);
            else if(t.equals("colr")){
                if(n-header<10)throw new IOException("Box colr incompleto");
                byte[] data=read(begin,(int)Math.min(16,n-header));String kind=new String(data,0,4,StandardCharsets.US_ASCII);
                if(!kind.equals("nclx")&&!kind.equals("nclc"))throw new IOException("Profilo colore "+kind+" non supportato: impossibile conservarlo senza conversione colore");
                ByteBuffer b=ByteBuffer.wrap(data);int p=Short.toUnsignedInt(b.getShort(4)),tr=Short.toUnsignedInt(b.getShort(6)),m=Short.toUnsignedInt(b.getShort(8));
                if(p!=color.primaries||tr!=color.transfer||m!=color.matrix||
                    (kind.equals("nclx")&&(data.length<11||((data[10]&128)!=0)!=color.fullRange)))
                    throw new IOException("Il colore dichiarato nel contenitore colr differisce dall’SPS: conversione rifiutata per evitare modifiche colore");
            }
            at+=n;
        }
    }
}
