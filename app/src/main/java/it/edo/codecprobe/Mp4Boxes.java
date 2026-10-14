package it.edo.codecprobe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;

/** Measures MP4 structural boxes without loading media payload into memory. */
final class Mp4Boxes {
    static final class Sizes {long moov,tags;}
    private final SeekableByteChannel file;
    private final Sizes sizes=new Sizes();
    private int boxes;
    private Mp4Boxes(SeekableByteChannel file){this.file=file;}
    static Sizes read(SeekableByteChannel file)throws IOException{
        Mp4Boxes reader=new Mp4Boxes(file);reader.walk(0,file.size(),0);return reader.sizes;
    }
    private void walk(long from,long end,int depth)throws IOException{
        if(depth>12)throw new IOException("Struttura MP4 troppo profonda");
        long position=from;
        while(position<end){
            if(end-position<8||++boxes>1000000)throw new IOException("Struttura MP4 non valida");
            ByteBuffer b=ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);b.limit(8);read(position,b);b.flip();
            long length=Integer.toUnsignedLong(b.getInt());byte[] name=new byte[4];b.get(name);String type=new String(name,StandardCharsets.ISO_8859_1);int header=8;
            if(length==1){b.clear();b.limit(8);read(position+8,b);b.flip();length=b.getLong();header=16;}
            else if(length==0)length=end-position;
            if(length<header||length>end-position)throw new IOException("Box MP4 incompleto: "+type);
            if(type.equals("moov"))sizes.moov+=length;
            if(type.equals("udta")||type.equals("meta"))sizes.tags+=length;
            else if(type.equals("moov")||type.equals("trak")||type.equals("mdia")||type.equals("minf")||type.equals("stbl")||type.equals("mvex"))walk(position+header,position+length,depth+1);
            position+=length;
        }
    }
    private void read(long position,ByteBuffer b)throws IOException{file.position(position);while(b.hasRemaining())if(file.read(b)<=0)throw new IOException("Lettura box MP4 incompleta");}
}
