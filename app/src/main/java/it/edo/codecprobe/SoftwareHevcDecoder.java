package it.edo.codecprobe;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Bundled FFmpeg HEVC decoder. No MediaCodec, P010 or Android Image requirement. */
final class SoftwareHevcDecoder {
    interface Frames { void frame(ByteBuffer pixels,long pts,long precise)throws Exception; }
    interface Packets { void packet(long pts,byte[] bytes)throws Exception; }
    private static native long open(byte[] csd,int w,int h,int p,int t,int m,boolean full)throws IOException;
    private static native void send(long handle,ByteBuffer packet,int size,long pts)throws IOException;
    private static native int receive(long handle,ByteBuffer pixels,long[] result)throws IOException;
    private static native void close(long handle);
    static void decode(MediaExtractor extractor,MediaFormat format,int w,int h,ColorVui color,
                       BooleanSupplier cancelled,Consumer<String> log,Packets packets,Frames frames)throws Exception {
        System.loadLibrary("softwarehevc");
        HdrHevcInfo.requireMain10(HdrCodec.csds(format).toArray(new byte[0][]));
        ByteArrayOutputStream headers=new ByteArrayOutputStream();
        for(byte[] csd:HdrCodec.csds(format))headers.write(csd);
        if(headers.size()==0)throw new IOException("Intestazioni HEVC assenti");
        log.accept("DECODER HDR · software HEVC FFmpeg 8.1.3 incluso nell’APK · planar 10 bit · nessun MediaCodec/P010 · thread=2");
        long handle=open(headers.toByteArray(),w,h,color.primaries,color.transfer,color.matrix,color.fullRange);
        try {
            ByteBuffer input=ByteBuffer.allocateDirect(1024*1024);
            ByteBuffer pixels=ByteBuffer.allocateDirect(Math.multiplyExact(Math.multiplyExact(w,h),3));
            long[] result=new long[2];long lastPts=-1;boolean flushing=false;
            while(true){
                if(cancelled.getAsBoolean())throw new IOException("Decodifica HDR software annullata");
                int status=receive(handle,pixels,result);
                if(status==2)break;
                if(status==1){
                    if(result[0]<=lastPts)throw new IOException("Timestamp HDR software non crescenti");lastPts=result[0];
                    pixels.clear();frames.frame(pixels,result[0],result[1]);continue;
                }
                if(flushing)throw new IOException("Decoder HDR richiede input dopo EOS");
                long size=extractor.getSampleSize();
                if(size<0){send(handle,null,0,0);flushing=true;continue;}
                if(size==0||size>32*1024*1024)throw new IOException("Dimensione campione HDR non valida: "+size);
                if((extractor.getSampleFlags()&(MediaExtractor.SAMPLE_FLAG_ENCRYPTED|MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME))!=0)
                    throw new IOException("Campione HDR protetto/parziale non supportato");
                if(size>input.capacity())input=ByteBuffer.allocateDirect((int)size);
                input.clear();int n=extractor.readSampleData(input,0);
                if(n!=size)throw new IOException("Campione HDR troncato");
                long pts=extractor.getSampleTime();
                if(packets!=null){byte[] original=new byte[n];input.position(0);input.limit(n);input.get(original);packets.packet(pts,original);}
                input.clear();send(handle,input,n,pts);extractor.advance();
            }
        }finally{close(handle);}
    }
}
