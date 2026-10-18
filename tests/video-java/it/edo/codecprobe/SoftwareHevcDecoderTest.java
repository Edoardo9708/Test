package it.edo.codecprobe;
import android.media.MediaExtractor;
import java.nio.file.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class SoftwareHevcDecoderTest {
    public static void main(String[] args)throws Exception {
        for(int transfer:new int[]{16,18}){
            String base=args[0]+"/"+transfer;
            MediaExtractor e=new MediaExtractor(base+".bin");ColorVui color=ColorVui.read("video/hevc",e.format.csd);
            ByteArrayOutputStream output=new ByteArrayOutputStream();int[] frames={0},packets={0};long[] last={-1};
            SoftwareHevcDecoder.decode(e,e.format,64,48,color,()->false,s->{},(pts,b)->packets[0]++,(pixels,pts,precise)->{
                if(pts<=last[0]||precise==0)throw new AssertionError("Timestamp or 10-bit loss");last[0]=pts;
                byte[] bytes=new byte[pixels.remaining()];pixels.get(bytes);output.write(bytes);frames[0]++;
            });
            if(frames[0]!=18||packets[0]!=18||!java.util.Arrays.equals(output.toByteArray(),Files.readAllBytes(Path.of(base+".yuv"))))throw new AssertionError("JNI frame mismatch");
            MediaExtractor cancelled=new MediaExtractor(base+".bin");AtomicInteger seen=new AtomicInteger();
            try{
                SoftwareHevcDecoder.decode(cancelled,cancelled.format,64,48,color,()->seen.get()>=3,s->{},null,(b,p,n)->seen.incrementAndGet());
                throw new AssertionError("Cancellation ignored");
            }catch(IOException expected){if(!expected.getMessage().contains("annullata"))throw expected;}
            MediaExtractor wrong=new MediaExtractor(base+".bin");ColorVui wrongColor=new ColorVui(9,transfer==16?18:16,9,false,true,true,5);
            try{
                SoftwareHevcDecoder.decode(wrong,wrong.format,64,48,wrongColor,()->false,s->{},null,(b,p,n)->{});
                throw new AssertionError("Changed HDR color accepted");
            }catch(IOException expected){if(!expected.getMessage().contains("color"))throw expected;}
            System.out.println("OK Java/JNI HDR transfer="+transfer+": all samples, reordered PTS, metadata packet callback, cancellation and color rejection");
        }
    }
}
