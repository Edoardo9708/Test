package it.edo.codecprobe;
import java.io.IOException;
import java.nio.ByteBuffer;

/** P010 stores ten bits in the upper bits of 16-bit little-endian samples.
 * x265 expects planar 16-bit words with the same ten bits right-aligned. */
final class HdrP010Packer {
    static long pack(ByteBuffer[] planes,int[] rows,int[] pixels,int left,int top,int width,int height,ByteBuffer target)throws IOException{
        if(planes==null||rows==null||pixels==null||(planes.length!=2&&planes.length!=3)||rows.length!=planes.length||pixels.length!=planes.length||left<0||top<0||width<2||height<2||(left|top|width|height)%2!=0||(long)width*height*3>target.capacity())throw new IOException("Layout P010 non supportato");
        target.clear();long lowBits=copy(planes[0],rows[0],pixels[0],left,top,width,height,target);
        if(planes.length==3){lowBits+=copy(planes[1],rows[1],pixels[1],left/2,top/2,width/2,height/2,target);lowBits+=copy(planes[2],rows[2],pixels[2],left/2,top/2,width/2,height/2,target);}
        else{
            if(pixels[1]!=2&&pixels[1]!=4)throw new IOException("Piano CbCr P010 non interlacciato come atteso");
            lowBits+=copy(planes[1],rows[1],4,left/2,top/2,width/2,height/2,target);
            ByteBuffer v=planes[1].duplicate();if(v.remaining()<2)throw new IOException("Piano Cr P010 assente");v.position(v.position()+2);
            lowBits+=copy(v,rows[1],4,left/2,top/2,width/2,height/2,target);
        }
        target.flip();return lowBits;
    }
    private static long copy(ByteBuffer src,int row,int pixel,int left,int top,int width,int height,ByteBuffer out)throws IOException{
        if(src==null||row<=0||pixel<2||pixel%2!=0||row<(long)(left+width-1)*pixel+2)throw new IOException("Stride P010 non valido");
        long base=src.position()+(long)top*row+(long)left*pixel,last=base+(long)(height-1)*row+(long)(width-1)*pixel+1;
        if(base<0||last>=src.limit()||(long)width*height*2>out.remaining())throw new IOException("Buffer P010 insufficiente per crop/stride");
        long lowBits=0;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int at=(int)(base+(long)y*row+(long)x*pixel);int raw=(src.get(at)&255)|((src.get(at+1)&255)<<8);
            if((raw&63)!=0)throw new IOException("P010 non valido: i sei bit bassi non sono zero");
            int value=raw>>>6;if((value&3)!=0)lowBits++;out.put((byte)value).put((byte)(value>>>8));
        }
        return lowBits;
    }
}
