package it.edo.codecprobe;

import java.io.ByteArrayOutputStream;
import java.util.*;

/** Reads exact H.264/H.265 VUI code points, rather than Android's grouped SDR enum. */
final class ColorVui {
    final int primaries,transfer,matrix,videoFormat;
    final boolean fullRange,signalPresent,descriptionPresent;
    ColorVui(int p,int t,int m,boolean full,boolean signal,boolean description,int vf){
        primaries=p;transfer=t;matrix=m;fullRange=full;signalPresent=signal;descriptionPresent=description;videoFormat=vf;
    }
    static ColorVui unspecified(){return new ColorVui(2,2,2,false,false,false,5);}
    int flags(){return (fullRange?1:0)|(signalPresent?2:0)|(descriptionPresent?4:0)|(videoFormat<<3);}
    boolean same(ColorVui b){return b!=null&&primaries==b.primaries&&transfer==b.transfer&&matrix==b.matrix&&fullRange==b.fullRange&&signalPresent==b.signalPresent&&descriptionPresent==b.descriptionPresent&&videoFormat==b.videoFormat;}
    public String toString(){return "primarie="+primaries+" transfer="+transfer+" matrice="+matrix+" range="+(fullRange?"completo":"limitato")+" signal_present="+signalPresent+" color_description="+descriptionPresent+" video_format="+videoFormat;}
    static ColorVui read(String mime,byte[]... buffers){
        boolean hevc="video/hevc".equals(mime);
        if(!hevc&&!"video/avc".equals(mime))throw new IllegalArgumentException("Lettura colore non supportata per "+mime);
        ColorVui result=null;
        for(byte[] data:buffers)if(data!=null)for(byte[] nal:nals(data)){
            if(nal.length<2)continue;
            int type=hevc?(nal[0]>>1)&63:nal[0]&31;
            if(type!=(hevc?33:7))continue;
            ColorVui c=hevc?hevc(nal):avc(nal);
            if(result!=null&&!result.same(c))throw new IllegalArgumentException("SPS multipli con dichiarazioni colore diverse: conversione non supportata");
            result=c;
        }
        if(result==null)throw new IllegalArgumentException("SPS non trovato: impossibile conservare la dichiarazione colore precisa");
        return result;
    }
    static List<byte[]> nals(byte[] d){
        List<byte[]> out=new ArrayList<>();int start=-1;
        for(int i=0;i+2<d.length;i++)if(d[i]==0&&d[i+1]==0&&d[i+2]==1){
            int end=i;if(i>0&&d[i-1]==0)end--;
            if(start>=0&&end>start)out.add(Arrays.copyOfRange(d,start,end));start=i+3;i+=2;
        }
        if(start>=0){if(start<d.length)out.add(Arrays.copyOfRange(d,start,d.length));return out;}
        // A single raw SPS is also accepted. Android codec CSD uses Annex B.
        out.add(d);return out;
    }
    private static Bits bits(byte[] n,int header){
        ByteArrayOutputStream b=new ByteArrayOutputStream();int zeros=0;
        for(int i=header;i<n.length;i++){
            int v=n[i]&255;
            if(zeros==2&&v==3){if(i+1>=n.length||(n[i+1]&255)>3)throw new IllegalArgumentException("SPS emulation prevention non valida");zeros=0;continue;}
            b.write(v);zeros=v==0?zeros+1:0;
        }return new Bits(b.toByteArray());
    }
    private static final class Bits {
        final byte[] d;int at;
        Bits(byte[] d){this.d=d;}
        int u(int n){if(n<0||n>31||at+n>d.length*8)throw new IllegalArgumentException("SPS troncato o non valido");int v=0;for(int i=0;i<n;i++)v=(v<<1)|((d[at>>3]>>(7-(at++&7)))&1);return v;}
        boolean flag(){return u(1)!=0;}
        void skip(int n){if(n<0||at+n>d.length*8)throw new IllegalArgumentException("SPS troncato");at+=n;}
        int ue(){int z=0;while(!flag())if(++z>24)throw new IllegalArgumentException("SPS Exp-Golomb fuori limite");return ((1<<z)-1)+u(z);}
        int se(){int v=ue();return (v&1)==0?-(v/2):(v+1)/2;}
        int bounded(int max){int v=ue();if(v>max)throw new IllegalArgumentException("SPS oltre i limiti supportati");return v;}
    }
    private static ColorVui vui(Bits b){
        if(b.flag()){int id=b.u(8);if(id==255)b.skip(32);}
        if(b.flag())b.skip(1);
        if(!b.flag())return unspecified();
        int format=b.u(3);boolean full=b.flag(),description=b.flag();
        return description?new ColorVui(b.u(8),b.u(8),b.u(8),full,true,true,format):new ColorVui(2,2,2,full,true,false,format);
    }
    private static ColorVui avc(byte[] nal){
        Bits b=bits(nal,1);int profile=b.u(8);b.skip(16);b.ue();
        if(Arrays.asList(100,110,122,244,44,83,86,118,128,138,139,134,135).contains(profile)){
            int chroma=b.bounded(3);if(chroma==3)b.skip(1);b.ue();b.ue();b.skip(1);
            if(b.flag())for(int i=0;i<(chroma==3?12:8);i++)if(b.flag()){
                int last=8,next=8;for(int j=0;j<(i<6?16:64);j++){if(next!=0)next=(last+b.se()+256)&255;last=next==0?last:next;}
            }
        }
        b.ue();int poc=b.ue();if(poc==0)b.ue();else if(poc==1){b.skip(1);b.se();b.se();int n=b.bounded(256);for(int i=0;i<n;i++)b.se();}else if(poc!=2)throw new IllegalArgumentException("SPS AVC POC non valido");
        b.ue();b.skip(1);b.ue();b.ue();boolean frame=b.flag();if(!frame)b.skip(1);b.skip(1);
        if(b.flag())for(int i=0;i<4;i++)b.ue();return b.flag()?vui(b):unspecified();
    }
    private static ColorVui hevc(byte[] nal){
        Bits b=bits(nal,2);b.skip(4);int layers=b.u(3);b.skip(1);b.skip(96);
        boolean[] profile=new boolean[layers],level=new boolean[layers];for(int i=0;i<layers;i++){profile[i]=b.flag();level[i]=b.flag();}
        if(layers>0)b.skip((8-layers)*2);
        for(int i=0;i<layers;i++){if(profile[i])b.skip(88);if(level[i])b.skip(8);}
        b.ue();int chroma=b.bounded(3);if(chroma==3)b.skip(1);b.ue();b.ue();if(b.flag())for(int i=0;i<4;i++)b.ue();
        b.ue();b.ue();int poc=b.bounded(12)+4;
        boolean ordering=b.flag();for(int i=ordering?0:layers;i<=layers;i++){b.ue();b.ue();b.ue();}
        for(int i=0;i<6;i++)b.ue();
        if(b.flag()&&b.flag())for(int size=0;size<4;size++)for(int matrix=0;matrix<6;matrix+=size==3?3:1){
            if(!b.flag())b.ue();else{if(size>1)b.se();for(int i=0;i<Math.min(64,1<<(4+2*size));i++)b.se();}
        }
        b.skip(2);if(b.flag()){b.skip(8);b.ue();b.ue();b.skip(1);}
        int sets=b.bounded(64);int[] deltas=new int[sets];
        for(int i=0;i<sets;i++){
            if(i>0&&b.flag()){
                b.skip(1);b.ue();int count=0;
                for(int j=0;j<=deltas[i-1];j++){boolean used=b.flag();boolean delta=used||b.flag();if(delta)count++;}deltas[i]=count;
            }else{
                int neg=b.bounded(64),pos=b.bounded(64);if(neg+pos>64)throw new IllegalArgumentException("SPS RPS troppo grande");
                deltas[i]=neg+pos;for(int j=0;j<neg+pos;j++){b.ue();b.skip(1);}
            }
        }
        if(b.flag()){int n=b.bounded(32);for(int i=0;i<n;i++)b.skip(poc+1);}
        b.skip(2);return b.flag()?vui(b):unspecified();
    }
}
