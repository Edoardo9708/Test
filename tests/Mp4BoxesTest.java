package it.edo.codecprobe;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.io.*;

public final class Mp4BoxesTest {
    static byte[] box(String type,byte[] payload){ByteBuffer b=ByteBuffer.allocate(8+payload.length);b.putInt(b.capacity());b.put(type.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));b.put(payload);return b.array();}
    static byte[] join(byte[]... a)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();for(byte[] x:a)b.write(x);return b.toByteArray();}
    static Mp4Boxes.Sizes parse(byte[] data)throws Exception{Path p=Files.createTempFile("mp4-boxes-",".mp4");try{Files.write(p,data);try(SeekableByteChannel f=Files.newByteChannel(p)){return Mp4Boxes.read(f);}}finally{Files.delete(p);}}
    public static void main(String[] args)throws Exception{
        byte[] tags=box("udta",box("meta",new byte[32])),technical=box("trak",box("mdia",box("minf",box("stbl",box("stsz",new byte[12])))));
        byte[] moov=box("moov",join(tags,technical));
        Mp4Boxes.Sizes s=parse(join(box("ftyp",new byte[16]),box("mdat",new byte[1024]),moov));
        if(s.moov!=moov.length||s.tags!=tags.length)throw new AssertionError("Metadata counted twice or media payload counted");
        ByteBuffer extended=ByteBuffer.allocate(24).putInt(1).put("free".getBytes()).putLong(24).putLong(0);
        s=parse(join(extended.array(),box("moov",new byte[0])));if(s.moov!=8)throw new AssertionError("Extended size");
        byte[] zero=box("mdat",new byte[20]);ByteBuffer.wrap(zero).putInt(0);s=parse(zero);if(s.moov!=0)throw new AssertionError("Size zero box");
        boolean rejected=false;try{parse(ByteBuffer.allocate(8).putInt(1000).put("moov".getBytes()).array());}catch(IOException e){rejected=true;}if(!rejected)throw new AssertionError("Truncated box accepted");
        System.out.println("MP4 metadata sizing tests passed");
    }
}
