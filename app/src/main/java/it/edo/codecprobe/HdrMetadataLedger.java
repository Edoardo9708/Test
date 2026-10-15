package it.edo.codecprobe;

import java.io.*;
import java.util.*;

/** Bounded timestamp joins for decoder/encoder reordering; verification index lives on disk. */
final class HdrMetadataLedger implements AutoCloseable {
    private final Map<Long,List<HdrSei.Message>> decoding=new HashMap<>(),encoding=new HashMap<>();
    private final DataOutputStream expected;
    private List<HdrSei.Message> initial;
    private long pendingBytes;
    private int submitted,produced,metadataFrames,dynamicFrames;
    HdrMetadataLedger(File file,List<HdrSei.Message> headers)throws IOException{
        initial=headers;for(HdrSei.Message m:headers)if(m.type==4)throw new IOException("HDR10+ nel CSD senza timestamp: associazione non verificabile");
        expected=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
    }
    void queued(long pts,byte[] compressed)throws IOException{
        if(pts<0||decoding.containsKey(pts))throw new IOException("Timestamp originale HDR duplicato/non valido: "+pts);
        List<HdrSei.Message> values=HdrSei.read(compressed);
        if(initial!=null){values=HdrSei.merge(initial,values);initial=null;}
        long size=bytes(values);if(decoding.size()+encoding.size()>=4096||pendingBytes+size>16*1024*1024)throw new IOException("Riordinamento metadati HDR oltre il limite di memoria");
        decoding.put(pts,values);pendingBytes+=size;
    }
    String beforeEncode(long pts)throws IOException{
        List<HdrSei.Message> values=decoding.remove(pts);if(values==null||encoding.containsKey(pts))throw new IOException("Metadati originali HDR non associabili al frame pts_us="+pts);
        encoding.put(pts,values);expected.writeLong(pts);expected.write(HdrSei.digest(values));submitted++;
        if(!values.isEmpty())metadataFrames++;for(HdrSei.Message m:values)if(m.type==4){dynamicFrames++;break;}
        return HdrSei.describe(values);
    }
    byte[] packet(byte[] data,long pts)throws IOException{
        List<HdrSei.Message> values=encoding.remove(pts);if(values==null)throw new IOException("Pacchetto HDR senza originale pts_us="+pts);
        byte[] result=HdrSei.replace(data,values);pendingBytes-=bytes(values);produced++;return result;
    }
    void finish()throws IOException{
        if(!decoding.isEmpty()||!encoding.isEmpty()||submitted!=produced||submitted==0)throw new IOException("Metadati HDR non completamente consumati: frame="+submitted+" pacchetti="+produced+" in attesa="+(decoding.size()+encoding.size()));
        expected.flush();
    }
    String summary(){return "frame="+submitted+" · frame con SEI HDR="+metadataFrames+" · frame con HDR10+="+dynamicFrames+" · pacchetti="+produced;}
    @Override public void close()throws IOException{expected.close();}
    private static long bytes(List<HdrSei.Message> values){long n=0;for(HdrSei.Message m:values)n+=m.payload.length;return n;}

    static final class Verifier implements AutoCloseable {
        private final DataInputStream input;private final TreeMap<Long,byte[]> ahead=new TreeMap<>();
        private boolean eof;private int count;private List<HdrSei.Message> initial;
        Verifier(File file,List<HdrSei.Message> headers)throws IOException{input=new DataInputStream(new BufferedInputStream(new FileInputStream(file)));initial=headers;}
        void sample(long pts,byte[] data)throws IOException{
            Map.Entry<Long,byte[]> match=find(pts);
            while(match==null&&!eof){
                if(ahead.size()>=4096)throw new IOException("Riordinamento verifica HDR eccessivo");
                int first=input.read();if(first<0){eof=true;break;}
                long p=(long)first<<56;for(int i=6;i>=0;i--)p|=(long)input.readUnsignedByte()<<(i*8);
                byte[] digest=new byte[32];input.readFully(digest);if(ahead.put(p,digest)!=null)throw new IOException("Indice HDR duplicato");match=find(pts);
            }
            if(match==null)throw new IOException("Frame extra/timestamp HDR modificato: "+pts);
            List<HdrSei.Message> actual=HdrSei.read(data);if(initial!=null){actual=HdrSei.merge(initial,actual);initial=null;}
            if(!Arrays.equals(match.getValue(),HdrSei.digest(actual)))throw new IOException("METADATI HDR PERSI O MODIFICATI · pts_us="+pts+" · ottenuti: "+HdrSei.describe(actual));
            ahead.remove(match.getKey());count++;
        }
        private Map.Entry<Long,byte[]> find(long pts){Map.Entry<Long,byte[]> a=ahead.floorEntry(pts),b=ahead.ceilingEntry(pts);long da=a==null?Long.MAX_VALUE:pts-a.getKey(),db=b==null?Long.MAX_VALUE:b.getKey()-pts;return da<=db&&da<=1000?a:db<=1000?b:null;}
        int finish()throws IOException{if(!ahead.isEmpty()||input.read()!=-1)throw new IOException("Fotogrammi/metadati HDR mancanti nel risultato");if(count==0)throw new IOException("Verifica HDR vuota");return count;}
        @Override public void close()throws IOException{input.close();}
    }
}
