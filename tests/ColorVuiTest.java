package it.edo.codecprobe;
public final class ColorVuiTest {
 static byte[] hex(String h){byte[] b=new byte[h.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(h.substring(i*2,i*2+2),16);return b;}
 static void check(String mime,String h,int p,int t,int m,boolean full){ColorVui c=ColorVui.read(mime,hex(h));if(c.primaries!=p||c.transfer!=t||c.matrix!=m||c.fullRange!=full||!c.signalPresent||!c.descriptionPresent)throw new AssertionError(c);}
 static void container()throws Exception{
  java.nio.file.Path path=java.nio.file.Files.createTempFile("color-colr-",".mp4");
  try{
   for(int transfer:new int[]{1,6}){
    java.nio.ByteBuffer b=java.nio.ByteBuffer.allocate(19);b.putInt(19).put("colr".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).put("nclx".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putShort((short)1).putShort((short)transfer).putShort((short)1).put((byte)0);
    java.nio.file.Files.write(path,b.array());boolean failed=false;java.util.List<String> logs=new java.util.ArrayList<>();
    try(java.nio.channels.FileChannel f=java.nio.channels.FileChannel.open(path)){ContainerColor.check(f,new ColorVui(1,1,1,false,true,true,5),logs::add);}catch(java.io.IOException e){failed=true;}
    String diagnostic=String.join("\n",logs);
    if(!diagnostic.contains("HEX payload colr:")||!diagnostic.contains("Tipo dichiarazione: nclx"))throw new AssertionError("Original colr diagnostic missing");
    if(transfer==6&&!diagnostic.contains("Trasferimento: MP4=6 · SPS=1"))throw new AssertionError("Mismatch values missing before rejection");
    if(failed)throw new AssertionError("Single colr override must be preserved separately");
   }
   // Exact SPS and colr from both failing 2017 recordings.
   ColorVui contradictory=ColorVui.read("video/avc",hex("000000016742801fda014016e958303030368509a8"));
   if(contradictory.primaries!=6||contradictory.transfer!=6||contradictory.matrix!=6||!contradictory.fullRange)throw new AssertionError("2017 fixture differs");
   byte[] override=hex("00000013636f6c726e636c7800010001000100");
   java.nio.file.Files.write(path,override);
   try(java.nio.channels.FileChannel f=java.nio.channels.FileChannel.open(path)){
    if(!ContainerColor.check(f,contradictory).same(contradictory))throw new AssertionError("Conflicting colr overwrote original SPS");
   }
   java.nio.file.Files.write(path,java.nio.ByteBuffer.allocate(override.length*2).put(override).put(override).array());
   try(java.nio.channels.FileChannel f=java.nio.channels.FileChannel.open(path)){
    ContainerColor.check(f,contradictory);throw new AssertionError("Duplicate colr accepted");
   }catch(java.io.IOException expected){}
   ColorVui original=ColorVui.read("video/avc",hex("0000000167640028acb403c0113f2a"));
   if(original.descriptionPresent||original.signalPresent||original.primaries!=2)throw new AssertionError("Actual Samsung AVC fixture must have no color declaration");
   for(boolean full:new boolean[]{false,true}){
    java.nio.ByteBuffer b=java.nio.ByteBuffer.allocate(19);b.putInt(19).put("colr".getBytes()).put("nclx".getBytes()).putShort((short)1).putShort((short)1).putShort((short)1).put((byte)(full?128:0));
    java.nio.file.Files.write(path,b.array());
    try(java.nio.channels.FileChannel f=java.nio.channels.FileChannel.open(path)){
     ColorVui resolved=ContainerColor.check(f,original);
     if(resolved.primaries!=1||resolved.transfer!=1||resolved.matrix!=1||resolved.fullRange!=full||!resolved.signalPresent||!resolved.descriptionPresent)throw new AssertionError("Missing SPS fields not filled from colr");
    }
   }
   // Unspecified container fields must not erase explicit SPS declarations.
   java.nio.ByteBuffer b=java.nio.ByteBuffer.allocate(18);b.putInt(18).put("colr".getBytes()).put("nclc".getBytes()).putShort((short)2).putShort((short)2).putShort((short)2);
   java.nio.file.Files.write(path,b.array());ColorVui declared=new ColorVui(6,6,6,true,true,true,5);
   try(java.nio.channels.FileChannel f=java.nio.channels.FileChannel.open(path)){if(!ContainerColor.check(f,declared).same(declared))throw new AssertionError("Explicit SPS erased by unspecified container");}
  }finally{java.nio.file.Files.deleteIfExists(path);}
 }
 public static void main(String[] args)throws Exception{
  container();
  check("video/hevc","420101216000000300b0000003000003007ba003c0801107cb96bb9324ba928080808176850940",1,1,1,false);
  check("video/avc","6742c00ada0811b016a020202800000300080000030010789135",1,1,1,false);
  check("video/hevc","42010101600000030090000003000003001ea01020205965654a4c2f016e0c0c0c080000030008000003000840",6,6,6,true);
  try{ColorVui.read("video/hevc",new byte[]{0x42,1,0});throw new AssertionError("Truncated SPS accepted");}catch(IllegalArgumentException expected){}
  System.out.println("Samsung HEVC BT709, AVC BT709, HEVC SMPTE170M full-range and truncated SPS checks passed");
 }
}


