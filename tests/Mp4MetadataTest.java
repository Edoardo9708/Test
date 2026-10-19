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
        return box("trak",join(dates("tkhd",time,time+1,0),box("mdia",join(dates("mdhd",time,time+1,0x15c7),box("hdlr",h),box("minf",box("stbl",box("stsd",new byte[8]))),tags?box("meta",join(new byte[4],box("hdlr",new byte[24]),box("ilst",box("test",new byte[]{7,8,9})))):new byte[0])),tags?box("udta",box("test",new byte[]{44,45,46})):new byte[0]));
    }
    static byte[] movie(int time,boolean tags)throws Exception{return box("moov",join(dates("mvhd",time,time+1,0),track(time,tags),tags?box("udta",join(box("name","titolo originale".getBytes()),box("©xyz",join(new byte[]{0,18,0,0},"+45.1234+008.1234/".getBytes(java.nio.charset.StandardCharsets.US_ASCII))))):new byte[0]));}
    static byte[] content(boolean tags)throws Exception{return join(box("ftyp",new byte[16]),box("mdat",new byte[]{11,22,33,44,55}),movie(tags?1234:99,tags),tags?box("uuid",new byte[]{(byte)0xbe,0x7a,(byte)0xcf,(byte)0xcb,(byte)0x97,(byte)0xa9,0x42,(byte)0xe8,(byte)0x9c,0x71,(byte)0x99,(byte)0x94,(byte)0x91,(byte)0xe3,(byte)0xaf,(byte)0xac,1,2,3,4}):new byte[0]);}
    static byte[] colorMovie(String codec,byte[] color,byte[] codecData)throws Exception{
        byte[] h=new byte[24];System.arraycopy("vide".getBytes(),0,h,8,4);
        byte[] entry=box(codec,join(new byte[78],box(codec.equals("avc1")?"avcC":"hvcC",codecData),color));
        byte[] stsd=box("stsd",join(new byte[]{0,0,0,0,0,0,0,1},entry));
        return join(box("ftyp",new byte[16]),box("mdat",new byte[]{11,22,33,44,55}),box("moov",join(dates("mvhd",1234,1235,0),box("trak",join(dates("tkhd",1234,1235,0),box("mdia",join(dates("mdhd",1234,1235,0),box("hdlr",h),box("minf",box("stbl",stsd)))))))));
    }
    static void colors(Path input,Path output)throws Exception{
        byte[] sourceCodec={10,20,30},targetCodec={40,50,60,70};
        for(String kind:new String[]{"nclx","nclc"})for(boolean targetColor:new boolean[]{false,true}){
            byte[] body=ByteBuffer.allocate(kind.equals("nclx")?11:10).put(kind.getBytes()).putShort((short)1).putShort((short)1).putShort((short)1).array();
            byte[] colr=box("colr",body),other=box("colr",new byte[]{'n','c','l','x',0,6,0,6,0,6,(byte)128});
            Files.write(input,colorMovie("avc1",colr,sourceCodec));
            byte[] before=colorMovie("hvc1",targetColor?other:new byte[0],targetCodec);Files.write(output,before);
            List<String> logs=new ArrayList<>();
            try(FileChannel src=FileChannel.open(input)){Mp4Metadata.copy(src,output.toFile(),()->false,logs::add);}
            byte[] after=Files.readAllBytes(output);
            for(int i=0;i<37;i++)if(before[i]!=after[i])throw new AssertionError("Media offsets changed copying colr");
            // Ignore the old freed moov: check the active movie at EOF, including regenerated entry lengths.
            byte[] active=Arrays.copyOfRange(after,before.length,after.length);
            String text=new String(active,java.nio.charset.StandardCharsets.ISO_8859_1);
            if(!text.contains(new String(colr,java.nio.charset.StandardCharsets.ISO_8859_1)))throw new AssertionError("Original colr payload lost");
            if(text.indexOf("colr")!=text.lastIndexOf("colr"))throw new AssertionError("Muxer colr not replaced");
            if(!text.contains(new String(box("hvcC",targetCodec),java.nio.charset.StandardCharsets.ISO_8859_1))||text.contains("avcC"))throw new AssertionError("New codec configuration overwritten");
            if(!String.join("\n",logs).contains("colr originale identico"))throw new AssertionError("Output colr not verified");
            try(FileChannel f=FileChannel.open(output)){Mp4Metadata.check(f,()->false);}
        }
    }
    static byte[] sef()throws Exception{
        byte[] name="HighlightVideo_Data".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] data="   AutoSum 10\n TMDI\n 1\n 21\n 1234567890\n SMDI\n 2\n 81\n 0 525 3525 3000 0 0 66 0\n 1 4000 7000 3000 0 0 533 0\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        ByteBuffer record=ByteBuffer.allocate(8+name.length+data.length).order(ByteOrder.LITTLE_ENDIAN);
        record.putShort((short)0).putShort((short)0x08b0).putInt(name.length).put(name).put(data);
        ByteBuffer directory=ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
        directory.put("SEFH".getBytes()).putInt(105).putInt(1).putShort((short)0).putShort((short)0x08b0).putInt(record.capacity()).putInt(record.capacity()).putInt(24).put("SEFT".getBytes());
        return box("sefd",join(record.array(),directory.array()));
    }
    static void rejected(Path input,byte[] bytes,String detail)throws Exception{
        Files.write(input,bytes);try(FileChannel src=FileChannel.open(input)){Mp4Metadata.check(src,()->false);throw new AssertionError(detail);}catch(IOException expected){}
    }
    public static void main(String[] args)throws Exception{
        Path input=Files.createTempFile("meta-source-",".mp4"),output=Files.createTempFile("meta-target-",".mp4");
        try{
            colors(input,output);
            for(boolean tags:new boolean[]{false,true}){
                Files.write(input,content(tags));byte[] before=content(false);Files.write(output,before);
                try(FileChannel src=FileChannel.open(input)){Mp4Metadata.copy(src,output.toFile(),()->false,s->{});}
                byte[] after=Files.readAllBytes(output);
                for(int i=0;i<37;i++)if(before[i]!=after[i])throw new AssertionError("Encoded mdat bytes moved/changed");
                String text=new String(after,java.nio.charset.StandardCharsets.ISO_8859_1);
                if(tags&&!text.contains("titolo originale"))throw new AssertionError("Opaque tag lost");
                if(tags&&!text.contains("©xyz\u0000\u0012\u0000\u0000+45.1234+008.1234/"))throw new AssertionError("GPS atom type or coordinates lost");
                try(FileChannel f=FileChannel.open(output)){Mp4Metadata.check(f,()->false);}
                // The copier itself verifies nested payload hashes, original header dates and language.
            }
            Files.write(input,box("moof",new byte[0]));boolean failed=false;try(FileChannel src=FileChannel.open(input)){Mp4Metadata.check(src,()->false);}catch(IOException e){failed=true;}if(!failed)throw new AssertionError("Fragmented metadata silently accepted");
            Files.write(input,content(true));Files.write(output,content(false));failed=false;try(FileChannel src=FileChannel.open(input)){Mp4Metadata.copy(src,output.toFile(),()->true,s->{});}catch(IOException e){failed=true;}if(!failed)throw new AssertionError("Cancellation ignored");
            Files.write(input,join(content(false),box("uuid",new byte[24])));failed=false;try(FileChannel src=FileChannel.open(input)){Mp4Metadata.check(src,()->false);}catch(IOException e){failed=true;}if(!failed)throw new AssertionError("Unknown vendor UUID silently accepted");
            Files.write(input,join(content(false),box("meta",join(new byte[4],box("iloc",new byte[20])))));failed=false;try(FileChannel src=FileChannel.open(input)){Mp4Metadata.check(src,()->false);}catch(IOException e){failed=true;}if(!failed)throw new AssertionError("Old media item offsets silently accepted");
            for(byte[] sef:new byte[][]{sef(),SamsungSefTest.screen(),SamsungSefTest.descriptive(),SamsungSefTest.slowMotion()}){
            SamsungSef.validate(sef);
            Files.write(input,join(content(true),sef));byte[] before=content(false);Files.write(output,before);
            try(FileChannel src=FileChannel.open(input)){Mp4Metadata.copy(src,output.toFile(),()->false,System.out::println);}
            byte[] after=Files.readAllBytes(output);byte[] relocated=Arrays.copyOfRange(after,after.length-sef.length,after.length);
            if(!Arrays.equals(sef,relocated))throw new AssertionError("Samsung SEF changed or not at EOF");
            for(int i=0;i<37;i++)if(before[i]!=after[i])throw new AssertionError("Media moved during SEF copy");
            int originalStart=content(true).length,newStart=after.length-sef.length;
            if(originalStart==newStart)throw new AssertionError("Test did not relocate SEF");
            SamsungSef.validate(relocated);
            }
            byte[] sef=sef();
            byte[] corrupt=sef.clone();ByteBuffer.wrap(corrupt).order(ByteOrder.LITTLE_ENDIAN).putInt(corrupt.length-16,Integer.MAX_VALUE);
            rejected(input,join(content(false),corrupt),"External SEF reference accepted");
            corrupt=sef.clone();corrupt[corrupt.length-2]='X';rejected(input,join(content(false),corrupt),"Broken SEFT accepted");
            corrupt=sef.clone();corrupt[10]=0;rejected(input,join(content(false),corrupt),"Unknown SEF record accepted");
            corrupt=sef.clone();corrupt[35]=0;rejected(input,join(content(false),corrupt),"Binary highlight payload accepted");
            corrupt=sef.clone();ByteBuffer.wrap(corrupt).order(ByteOrder.LITTLE_ENDIAN).putInt(corrupt.length-8,12);
            rejected(input,join(content(false),corrupt),"Invalid directory count accepted");
            rejected(input,join(content(false),sef,box("free",new byte[0])),"Non-final SEF accepted");
            rejected(input,join(content(false),sef,sef),"Duplicate SEF accepted");
            System.out.println("MP4 metadata round-trip, no-tag, media offset and cancellation tests passed");
        }finally{Files.deleteIfExists(input);Files.deleteIfExists(output);}
    }
}


