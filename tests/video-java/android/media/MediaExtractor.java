package android.media;
import java.io.*;
import java.nio.*;
import java.util.*;
/** Host transport fixture only: the production decoder and JNI are used unchanged. */
public final class MediaExtractor {
    public static final int SAMPLE_FLAG_ENCRYPTED=2,SAMPLE_FLAG_PARTIAL_FRAME=4;
    public final MediaFormat format;
    private final List<byte[]> packets=new ArrayList<>();
    private final List<Long> timestamps=new ArrayList<>();
    private int at;
    public MediaExtractor(String path)throws IOException{
        try(DataInputStream in=new DataInputStream(new FileInputStream(path))){
            int n=Integer.reverseBytes(in.readInt());format=new MediaFormat(in.readNBytes(n));
            while(in.available()>0){timestamps.add(Long.reverseBytes(in.readLong()));n=Integer.reverseBytes(in.readInt());packets.add(in.readNBytes(n));}
        }
    }
    public long getSampleSize(){return at<packets.size()?packets.get(at).length:-1;}
    public int getSampleFlags(){return 0;}
    public int readSampleData(ByteBuffer buffer,int offset){buffer.position(offset);buffer.put(packets.get(at));return packets.get(at).length;}
    public long getSampleTime(){return timestamps.get(at);}
    public boolean advance(){return ++at<packets.size();}
}
