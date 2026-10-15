package it.edo.codecprobe;
import java.nio.*;
import java.io.IOException;
import java.util.Arrays;
public final class HdrP010PackerTest {
    private static void sample(ByteBuffer b,int offset,int value){b.put(offset,(byte)(value<<6));b.put(offset+1,(byte)(value>>>2));}
    public static void main(String[] args)throws Exception{
        ByteBuffer y=ByteBuffer.allocate(36),uv=ByteBuffer.allocate(20);y.position(2);uv.position(2);
        int[] values={0,1,2,3,255,256,511,512};for(int row=0;row<2;row++)for(int x=0;x<4;x++)sample(y,2+row*12+x*2,values[row*4+x]);
        sample(uv,2,513);sample(uv,4,1023);sample(uv,6,514);sample(uv,8,1022);
        ByteBuffer target=ByteBuffer.allocateDirect(24).order(ByteOrder.LITTLE_ENDIAN);
        long precise=HdrP010Packer.pack(new ByteBuffer[]{y,uv},new int[]{12,12},new int[]{2,4},0,0,4,2,target);
        int[] expected={0,1,2,3,255,256,511,512,513,514,1023,1022};for(int v:expected)if(Short.toUnsignedInt(target.getShort())!=v)throw new AssertionError("P010 sample or U/V order changed");
        if(precise==0||y.position()!=2||uv.position()!=2)throw new AssertionError("Precision or source position lost");
        target.clear();HdrP010Packer.pack(new ByteBuffer[]{y,uv},new int[]{12,12},new int[]{2,2},0,0,4,2,target);
        for(int value:expected)if(Short.toUnsignedInt(target.getShort())!=value)throw new AssertionError("Two-plane scalar stride not interpreted as P010 CbCr");
        ByteBuffer u=uv.duplicate(),v=uv.duplicate();v.position(v.position()+2);target.clear();
        HdrP010Packer.pack(new ByteBuffer[]{y,u,v},new int[]{12,12,12},new int[]{2,4,4},0,0,4,2,target);
        for(int value:expected)if(Short.toUnsignedInt(target.getShort())!=value)throw new AssertionError("Three-plane P010 layout changed");
        ByteBuffer cropY=ByteBuffer.allocate(48),cropUV=ByteBuffer.allocate(24);sample(cropY,28,1);sample(cropY,30,1023);sample(cropY,40,2);sample(cropY,42,1022);sample(cropUV,16,513);sample(cropUV,18,514);
        target=ByteBuffer.allocateDirect(12).order(ByteOrder.LITTLE_ENDIAN);HdrP010Packer.pack(new ByteBuffer[]{cropY,cropUV},new int[]{12,12},new int[]{2,4},2,2,2,2,target);
        for(int value:new int[]{1,1023,2,1022,513,514})if(Short.toUnsignedInt(target.getShort())!=value)throw new AssertionError("P010 crop wrong");
        y.put(2,(byte)1);boolean rejected=false;try{HdrP010Packer.pack(new ByteBuffer[]{y,uv},new int[]{12,12},new int[]{2,4},0,0,4,2,ByteBuffer.allocate(24));}catch(IOException e){rejected=true;}if(!rejected)throw new AssertionError("Malformed P010 accepted");
        rejected=false;try{HdrP010Packer.pack(new ByteBuffer[]{cropY,cropUV},new int[]{12,12},new int[]{2,4},1,0,2,2,ByteBuffer.allocate(12));}catch(IOException e){rejected=true;}if(!rejected)throw new AssertionError("Odd crop accepted");
        rejected=false;try{HdrHevcInfo.requireMain10(new byte[]{0,0,1,0x42,1,0});}catch(IllegalArgumentException e){rejected=true;}if(!rejected)throw new AssertionError("Truncated HEVC accepted");
        System.out.println("HDR P010: ten-bit values, two/three planes, padding, crop, malformed layouts and SPS truncation passed");
    }
}
