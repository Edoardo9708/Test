package it.edo.codecprobe;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

public final class Mp4MetadataTest {
    static byte[] box(String type,byte[] data){ByteBuffer b=ByteBuffer.allocate(8+data.length);b.putInt(b.capacity());b.put(type.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));b.put(data);return b.array();}
    static byte[] join(byte[]... data)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();for(byte[] p:data)b.write(p);return b.toByteArray();}
    static byte[] dates(String type,int created,int changed,int language){byte[] body=new byte[type.equals("mdhd")?24:100];ByteBuffer b=ByteBuffer.wrap(body);b.putInt(4,created);b.putInt(8,changed);if(type.equals("mdhd"))b.putShort(20,(short)language);return box(type,body);}
    static byte[] track(int time,boolean tags)throws Exception{
        byte[] h=new byte[24];System.arraycopy("vide".getBytes(),0,h,8,4);
        return box("trak",join(dates("tkhd",time,time+1,0),box("mdia",join(dates("mdhd",time,time+1,0x15c7),box("hdlr",h),box("minf",box("stbl",box("stsd",new byte[]{1,2,3,4}))),tags?box("meta",join(new byte[4],box("hdlr",new byte[24]),box("ilst",box("test",new byte[]{7,8,9})))):new byte[0])),tags?box("udta",box("test",new byte[]{44,45,46})):new byte[0]));
    }
    static byte[] movie(int time,boolean tags)throws Exception{return box("moov",join(dates("mvhd",time,time+1,0),track(time,tags),tags?box("udta",box("name","titolo originale".getBytes())):new byte[0]));}
    static byte[] content(boolean tags)throws Exception{return join(box("ftyp",new byte[16]),box("mdat",new byte[]{11,22,33,44,55}),movie(tags?1234:99,tags),tags?box("uuid",new byte[]{(byte)0xbe,0x7a,(byte)0xcf,(byte)0xcb,(byte)0x97,(byte)0xa9,0x42,(byte)0xe8,(byte)0x9c,0x71,(byte)0x99,(byte)0x94,(byte)0x91,(byte)0xe3,(byte)0xaf,(byte)0xac,1,2,3,4}):new byte[0]);}
    public static void main(String[] args)throws Exception{
        Path input=Files.createTempFile("meta-source-",".mp4"),output=Files.createTempFile("meta-target-",".mp4");
        try{
            for(boolean tags:new boolean[]{false,true}){
                Files.write(input,content(tags));byte[] before=content(false);Files.write(output,before);
                try(FileChannel src=FileChannel.open(input)){Mp4Metadata.copy(src,output.toFile(),()->false,s->{});}
                byte[] after=Files.readAllBytes(output);
                for(int i=0;i<37;i++)if(before[i]!=after[i])throw new AssertionError("Encoded mdat bytes moved/changed");
                String text=new String(after,java.nio.charset.StandardCharsets.ISO_8859_1);
                if(tags&&!text.contains("titolo originale"))throw new AssertionError("Opaque tag lost");
                try(FileChannel f=FileChannel.open(output)){Mp4Metadata.check(f,()->false);}
                // The copier itself verifies nested payload hashes, original header dates and language.
            }
            Files.write(input,box("moof",new byte[0]));boolean failed=false;try(FileChannel src=FileChannel.open(input)){Mp4Metadata.check(src,()->false);}catch(IOException e){failed=true;}if(!failed)throw new AssertionError("Fragmented metadata silently accepted");
            Files.write(input,content(true));Files.write(output,content(false));failed=false;try(FileChannel src=FileChannel.open(input)){Mp4Metadata.copy(src,output.toFile(),()->true,s->{});}catch(IOException e){failed=true;}if(!failed)throw new AssertionError("Cancellation ignored");
            Files.write(input,join(content(false),box("uuid",new byte[24])));failed=false;try(FileChannel src=FileChannel.open(input)){Mp4Metadata.check(src,()->false);}catch(IOException e){failed=true;}if(!failed)throw new AssertionError("Unknown vendor UUID silently accepted");
            Files.write(input,join(content(false),box("meta",join(new byte[4],box("iloc",new byte[20])))));failed=false;try(FileChannel src=FileChannel.open(input)){Mp4Metadata.check(src,()->false);}catch(IOException e){failed=true;}if(!failed)throw new AssertionError("Old media item offsets silently accepted");
            System.out.println("MP4 metadata round-trip, no-tag, media offset and cancellation tests passed");
        }finally{Files.deleteIfExists(input);Files.deleteIfExists(output);}
    }
}
