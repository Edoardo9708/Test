package it.edo.codecprobe;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
public final class MetadataDumpTest {
 static byte[] box(String t,byte[] p){ByteBuffer b=ByteBuffer.allocate(p.length+8);b.putInt(b.capacity()).put(t.getBytes(java.nio.charset.StandardCharsets.US_ASCII)).put(p);return b.array();}
 public static void main(String[] args)throws Exception{
  Path p=Files.createTempFile("dump-test-",".mp4");
  try{
   // A sparse 2.4 GB mdat reproduces the user's size without reading/copying samples.
   long media=2400000000L;byte[] sef=box("sefd",new byte[]{83,69,70,84,1,2,3,4});
   try(RandomAccessFile f=new RandomAccessFile(p.toFile(),"rw")){f.write(box("ftyp",new byte[]{1,2,3,4}));f.write(ByteBuffer.allocate(8).putInt((int)media).put("mdat".getBytes()).array());f.seek(12+media);f.write(sef);}
   long before=Files.size(p);StringWriter text=new StringWriter();
   try(FileChannel f=FileChannel.open(p)){MetadataDump.write(f,text,()->false);}
   if(Files.size(p)!=before||!text.toString().contains("736566645345465401020304")||text.toString().length()>4000)throw new AssertionError("SEF export or sample skipping failed");
   boolean cancelled=false;try(FileChannel f=FileChannel.open(p)){MetadataDump.write(f,new StringWriter(),()->true);}catch(IOException expected){cancelled=true;}if(!cancelled)throw new AssertionError("Cancellation ignored");
   Files.write(p,new byte[]{0,0,0,20,115,101,102,100});boolean truncated=false;try(FileChannel f=FileChannel.open(p)){MetadataDump.write(f,new StringWriter(),()->false);}catch(IOException expected){truncated=true;}if(!truncated)throw new AssertionError("Truncated box accepted");
   System.out.println("SEF diagnostic export, sparse 2.4GB media skipping, cancellation and truncation checks passed");
  }finally{Files.deleteIfExists(p);}
 }
}
