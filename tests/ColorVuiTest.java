package it.edo.codecprobe;
public final class ColorVuiTest {
 static byte[] hex(String h){byte[] b=new byte[h.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(h.substring(i*2,i*2+2),16);return b;}
 static void check(String mime,String h,int p,int t,int m,boolean full){ColorVui c=ColorVui.read(mime,hex(h));if(c.primaries!=p||c.transfer!=t||c.matrix!=m||c.fullRange!=full||!c.signalPresent||!c.descriptionPresent)throw new AssertionError(c);}
 static void container()throws Exception{
  java.nio.file.Path path=java.nio.file.Files.createTempFile("color-colr-",".mp4");
  try{
   for(int transfer:new int[]{1,6}){
    java.nio.ByteBuffer b=java.nio.ByteBuffer.allocate(19);b.putInt(19).put("colr".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).put("nclx".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putShort((short)1).putShort((short)transfer).putShort((short)1).put((byte)0);
    java.nio.file.Files.write(path,b.array());boolean failed=false;
    try(java.nio.channels.FileChannel f=java.nio.channels.FileChannel.open(path)){ContainerColor.check(f,new ColorVui(1,1,1,false,true,true,5));}catch(java.io.IOException e){failed=true;}
    if(failed!=(transfer==6))throw new AssertionError("Conflicting container transfer not detected");
   }
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
