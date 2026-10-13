package it.edo.codecprobe;
import java.nio.ByteBuffer;
import java.util.Arrays;
public final class YuvPackerTest {
    static void test(byte[] source,int position,int row,int pixel,int left,int top,int width,int height,byte[] expected)throws Exception {
        ByteBuffer b=ByteBuffer.wrap(source);b.position(position);
        ByteBuffer out=ByteBuffer.allocate(expected.length);
        YuvPacker.copyPlane(b,row,pixel,left,top,width,height,out);
        if(!Arrays.equals(expected,out.array()))throw new AssertionError(Arrays.toString(out.array()));
        if(b.position()!=position)throw new AssertionError("Source position changed");
    }
    public static void main(String[] args)throws Exception {
        test(new byte[]{1,2,3,99,4,5,6,99},0,4,1,0,0,3,2,new byte[]{1,2,3,4,5,6});
        test(new byte[]{9,9,9,9,1,2,3,9,4,5,6,9},0,4,1,1,1,2,2,new byte[]{2,3,5,6});
        test(new byte[]{1,90,2,91,3,92,4,93},0,4,2,0,0,2,2,new byte[]{1,2,3,4});
        test(new byte[]{9,1,90,2,91,3,92,4},1,4,2,0,0,2,2,new byte[]{1,2,3,4});
        try {
            YuvPacker.copyPlane(ByteBuffer.wrap(new byte[]{1}),4,2,0,0,2,2,ByteBuffer.allocate(4));
            throw new AssertionError("Invalid plane accepted");
        } catch(java.io.IOException expected){}
        System.out.println("OK YUV: planar padding, crop, interleaved chroma, buffer offset, short final row and invalid bounds");
    }
}
