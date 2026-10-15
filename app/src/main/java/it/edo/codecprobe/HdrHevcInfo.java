package it.edo.codecprobe;
import java.io.ByteArrayOutputStream;

/** Independent check of the profile and sample depth actually signalled in HEVC SPS. */
final class HdrHevcInfo {
    static String requireMain10(byte[]... csds){
        boolean found=false;String result="";
        for(byte[] data:csds)for(byte[] nal:ColorVui.nals(data)){
            if(nal.length<3||((nal[0]>>>1)&63)!=33)continue;
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();int zeros=0;
            for(int i=2;i<nal.length;i++){int value=nal[i]&255;if(zeros==2&&value==3){if(i+1>=nal.length||(nal[i+1]&255)>3)throw new IllegalArgumentException("SPS HEVC escape non valido");zeros=0;continue;}bytes.write(value);zeros=value==0?zeros+1:0;}
            Bits b=new Bits(bytes.toByteArray());b.skip(4);int layers=b.u(3);b.skip(1);b.skip(3);int profile=b.u(5);b.skip(88);
            boolean[] p=new boolean[layers],l=new boolean[layers];for(int i=0;i<layers;i++){p[i]=b.u(1)!=0;l[i]=b.u(1)!=0;}if(layers>0)b.skip((8-layers)*2);
            for(int i=0;i<layers;i++){if(p[i])b.skip(88);if(l[i])b.skip(8);}
            b.ue();int chroma=b.ue();if(chroma==3)b.skip(1);int w=b.ue(),h=b.ue();if(b.u(1)!=0)for(int i=0;i<4;i++)b.ue();int y=b.ue()+8,c=b.ue()+8;
            if(profile!=2||chroma!=1||y!=10||c!=10)throw new IllegalArgumentException("Richiesto HEVC Main10 4:2:0: profile="+profile+" chroma="+chroma+" bit_Y="+y+" bit_C="+c);
            result="profile="+profile+" · 4:2:0 · bit_Y="+y+" · bit_C="+c+" · dimensioni codificate="+w+"×"+h;found=true;
        }
        if(!found)throw new IllegalArgumentException("SPS HEVC assente");return result;
    }
    private static final class Bits {
        final byte[] bytes;int at;Bits(byte[] b){bytes=b;}
        int u(int n){if(n<0||n>31||at+n>bytes.length*8)throw new IllegalArgumentException("SPS HEVC troncato");int v=0;for(int i=0;i<n;i++)v=(v<<1)|((bytes[at>>3]>>(7-(at++&7)))&1);return v;}
        void skip(int n){if(n<0||at+n>bytes.length*8)throw new IllegalArgumentException("SPS HEVC troncato");at+=n;}
        int ue(){int zero=0;while(u(1)==0)if(++zero>24)throw new IllegalArgumentException("SPS HEVC fuori limite");return ((1<<zero)-1)+u(zero);}
    }
}
