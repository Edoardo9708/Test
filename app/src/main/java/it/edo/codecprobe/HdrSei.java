package it.edo.codecprobe;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Bitstream metadata only: never interprets or rewrites the original HDR payload. */
final class HdrSei {
    static final int MAX_BYTES=1024*1024;
    static final class Message {
        final int type;final byte[] payload;
        Message(int type,byte[] payload){this.type=type;this.payload=payload.clone();}
    }
    static List<byte[]> nals(byte[] data){
        if(data.length==0)return Collections.emptyList();
        if(data.length>32*1024*1024)throw bad("campione oltre 32 MiB");
        List<byte[]> out=new ArrayList<>();int first=start(data,0);
        // A four-byte MP4 length of 256..511 begins like a three-byte start code.
        // Refuse that ambiguity instead of silently interpreting an HDR SEI as a slice.
        if(first==0&&prefix(data,0)==3&&lengthNals(data,4)!=null)throw bad("framing NAL ambiguo: start code o lunghezza MP4");
        if(first==0){
            int at=0;
            while(at<data.length){int prefix=prefix(data,at),begin=at+prefix,next=start(data,begin);int end=next<0?data.length:next;
                while(end>begin&&data[end-1]==0)end--;addNal(out,data,begin,end);if(next<0)break;at=next;
            }
            return out;
        }
        // Android usually exposes Annex B; some extractors return MP4 length-prefixed NALs.
        for(int lengthBytes=4;lengthBytes>=1;lengthBytes--){
            List<byte[]> parsed=lengthNals(data,lengthBytes);if(parsed!=null)return parsed;
        }
        throw bad("struttura NAL non riconosciuta");
    }
    private static List<byte[]> lengthNals(byte[] data,int lengthBytes){
        List<byte[]> out=new ArrayList<>();int at=0;
        while(at<data.length){if(data.length-at<lengthBytes)return null;long n=0;for(int j=0;j<lengthBytes;j++)n=n*256+(data[at++]&255);
            if(n<2||n>data.length-at)return null;
            try{addNal(out,data,at,at+(int)n);}catch(IllegalArgumentException e){return null;}at+=(int)n;
        }
        return out.isEmpty()?null:out;
    }
    private static void addNal(List<byte[]> out,byte[] d,int from,int to){
        if(to-from<2||(d[from]&128)!=0||(d[from+1]&7)==0)throw bad("header NAL non valido");
        if(out.size()>=65536)throw bad("troppi NAL");out.add(Arrays.copyOfRange(d,from,to));
    }
    private static int prefix(byte[] d,int p){if(p+3<=d.length&&d[p]==0&&d[p+1]==0){if(d[p+2]==1)return 3;if(p+4<=d.length&&d[p+2]==0&&d[p+3]==1)return 4;}return 0;}
    private static int start(byte[] d,int from){for(int i=from;i+3<=d.length;i++)if(prefix(d,i)>0)return i;return -1;}
    static int type(byte[] nal){return (nal[0]&126)>>1;}
    private static byte[] rbsp(byte[] n){ByteArrayOutputStream out=new ByteArrayOutputStream();int zeros=0;for(int i=2;i<n.length;i++){int b=n[i]&255;if(zeros>=2&&b==3){if(i+1>=n.length||(n[i+1]&255)>3)throw bad("emulation prevention non valido");zeros=0;continue;}out.write(b);zeros=b==0?zeros+1:0;}return out.toByteArray();}
    private static List<Message> messages(byte[] n){
        byte[] d=rbsp(n);List<Message> result=new ArrayList<>();int at=0;
        while(at<d.length){if((d[at]&255)==128){boolean end=true;for(int j=at+1;j<d.length;j++)if(d[j]!=0)end=false;if(end)return result;}
            int t=0,size=0,b;do{if(at>=d.length||t>MAX_BYTES)throw bad("tipo SEI troncato");b=d[at++]&255;t+=b;}while(b==255);
            do{if(at>=d.length||size>MAX_BYTES)throw bad("lunghezza SEI troncata");b=d[at++]&255;size+=b;}while(b==255);
            if(size>MAX_BYTES||size>d.length-at)throw bad("payload SEI troncato/eccessivo");
            result.add(new Message(t,Arrays.copyOfRange(d,at,at+size)));at+=size;
        }
        throw bad("terminatore SEI assente");
    }
    private static boolean hdr(Message m){
        if(m.type==137){if(m.payload.length!=24)throw bad("mastering display non valido");return true;}
        if(m.type==144){if(m.payload.length!=4)throw bad("content light level non valido");return true;}
        if(m.type==147){if(m.payload.length!=1)throw bad("alternative transfer non valido");return true;}
        if(m.type==4){byte[] p=m.payload;if(p.length>=7&&(p[0]&255)==181&&p[1]==0&&p[2]==60&&p[3]==0&&p[4]==1&&p[5]==4)return true;throw bad("SEI T.35 diverso da HDR10+: copia non verificabile");}
        return false;
    }
    static List<Message> read(byte[] bytes){
        List<Message> result=new ArrayList<>();int total=0;
        for(byte[] n:nals(bytes)){
            int t=type(n),layer=((n[0]&1)<<5)|((n[1]&248)>>3);
            if(layer!=0||t==62||t==63)throw bad("livelli aggiuntivi/Dolby Vision non supportati");
            if(t!=39&&t!=40)continue;
            for(Message m:messages(n)){
                if(hdr(m)){if(t!=39)throw bad("metadato HDR in suffix SEI non supportato");result.add(m);total+=m.payload.length;if(total>MAX_BYTES)throw bad("metadati oltre 1 MiB");}
                // Timing, recovery and decoded-picture hashes describe the old encoding, not its HDR appearance.
                else if(m.type!=0&&m.type!=1&&m.type!=3&&m.type!=6&&m.type!=129&&m.type!=130&&m.type!=132)
                    throw bad("SEI originale tipo="+m.type+" non supportato: nessun metadato scartato silenziosamente");
            }
        }
        return result;
    }
    static List<Message> merge(List<Message> a,List<Message> b){
        List<Message> out=new ArrayList<>(a);for(Message m:b){boolean found=false;for(Message v:out)if(v.type==m.type){if(!Arrays.equals(v.payload,m.payload))throw bad("SEI HDR contrastanti tipo="+m.type);found=true;}if(!found)out.add(m);}return out;
    }
    static byte[] annexB(List<byte[]> nals){ByteArrayOutputStream out=new ByteArrayOutputStream();for(byte[] n:nals){out.write(0);out.write(0);out.write(0);out.write(1);out.write(n,0,n.length);}return out.toByteArray();}
    private static void extended(ByteArrayOutputStream out,int value){while(value>=255){out.write(255);value-=255;}out.write(value);}
    static byte[] encode(List<Message> messages){
        ByteArrayOutputStream raw=new ByteArrayOutputStream();for(Message m:messages){extended(raw,m.type);extended(raw,m.payload.length);raw.write(m.payload,0,m.payload.length);}raw.write(128);
        ByteArrayOutputStream nal=new ByteArrayOutputStream();nal.write(78);nal.write(1);int zeros=0;for(byte b:raw.toByteArray()){if(zeros>=2&&(b&255)<=3){nal.write(3);zeros=0;}nal.write(b&255);zeros=b==0?zeros+1:0;}return nal.toByteArray();
    }
    /** Drop any encoder-generated HDR SEI, then insert the exact original messages before the VCL. */
    static byte[] replace(byte[] generated,List<Message> original){
        List<byte[]> out=new ArrayList<>();boolean inserted=false;
        for(byte[] n:nals(generated)){
            int t=type(n);if(t<=31&&!inserted){if(!original.isEmpty())out.add(encode(original));inserted=true;}
            if(t==39||t==40){List<Message> kept=new ArrayList<>();for(Message m:messages(n))if(!hdr(m))kept.add(m);if(!kept.isEmpty())out.add(encode(kept));}
            else out.add(n);
        }
        if(!inserted&&!original.isEmpty())throw bad("metadati senza fotogramma VCL");return annexB(out);
    }
    static byte[] digest(List<Message> messages){
        try{MessageDigest hash=MessageDigest.getInstance("SHA-256");List<Message> sorted=new ArrayList<>(messages);sorted.sort(Comparator.comparingInt(m->m.type));for(Message m:sorted){hash.update(ByteBuffer.allocate(8).putInt(m.type).putInt(m.payload.length).array());hash.update(m.payload);}return hash.digest();}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}
    }
    static String describe(List<Message> messages){StringBuilder s=new StringBuilder();for(Message m:messages){if(s.length()>0)s.append(", ");s.append(m.type==4?"HDR10+ T.35":m.type==137?"Mastering display":m.type==144?"Content light level":"Alternative transfer").append(" (").append(m.payload.length).append(" byte)");}return s.length()==0?"nessun SEI HDR in questo campione":s.toString();}
    private static IllegalArgumentException bad(String s){return new IllegalArgumentException("METADATI HDR · "+s);}
}
