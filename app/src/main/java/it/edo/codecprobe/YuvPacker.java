package it.edo.codecprobe;
import java.io.IOException;
import java.nio.ByteBuffer;
final class YuvPacker {
    static void copyPlane(ByteBuffer source,int rowStride,int pixelStride,int left,int top,int width,int height,ByteBuffer target) throws IOException {
        if(rowStride<=0||pixelStride<=0||left<0||top<0||width<=0||height<=0) throw new IOException("Layout YUV non valido");
        long base=source.position()+(long)top*rowStride+(long)left*pixelStride;
        long last=base+(long)(height-1)*rowStride+(long)(width-1)*pixelStride;
        if(base<0||last>=source.limit()||(long)width*height>target.remaining())throw new IOException("Buffer YUV troppo piccolo per stride/crop dichiarati");
        for(int y=0;y<height;y++) {
            int offset=(int)(base+(long)y*rowStride);
            if(pixelStride==1) {
                ByteBuffer row=source.duplicate();row.position(offset);row.limit(offset+width);target.put(row);
            } else for(int x=0;x<width;x++) target.put(source.get(offset+x*pixelStride));
        }
    }
}
