package it.edo.codecprobe;

import java.io.*;
import java.util.*;

/** Read-only container dispatch, independent of Android and conversion acceptance rules. */
final class AnalysisParser {
    static final int MAX_NODES=4096;
    static AnalysisNode root(AnalysisReader r,String name)throws IOException{
        String kind="",label="Formato non interpretato · byte consultabili";
        if(r.size>=2&&r.u16(0)==0xffd8){kind="jpeg";label="JPEG · segmenti e dati compressi";}
        else if(r.size>=8&&Arrays.asList("ftyp","moov","free","wide","mdat").contains(r.ascii(4,4))){kind="boxes";label="ISO BMFF · HEIC / MP4 / MOV / M4A";}
        else if(r.size>=4&&r.ascii(0,4).equals("fLaC")){kind="flac";label="FLAC · metadati e audio";}
        else if(r.size>=3&&r.ascii(0,3).equals("ID3")){kind="id3";label="ID3 · tag e audio";}
        return new AnalysisNode(name,label,0,r.size,kind,0);
    }
    static List<AnalysisNode> children(AnalysisReader r,AnalysisNode n)throws IOException{
        r.within(n.offset,n.size,r.size);
        switch(n.parser){
            case "boxes":return IsoAnalysisParser.read(r,n);
            case "jpeg":return JpegAnalysisParser.read(r,n);
            case "exif":return ExifAnalysisParser.read(r,n);
            case "flac":case "id3":return AudioAnalysisParser.read(r,n);
            default:return Collections.emptyList();
        }
    }
    static AnalysisNode leaf(String title,String detail,long offset,long size){return new AnalysisNode(title,detail,offset,size,"",offset);}
    static void limit(List<AnalysisNode> out)throws IOException{if(out.size()>=MAX_NODES)throw new IOException("Troppi elementi in questo livello (limite "+MAX_NODES+"). Nessuna struttura parziale mostrata.");}
    static String hex(AnalysisReader r,AnalysisNode n,long page)throws IOException{
        if(page<0||page>n.size||page%256!=0)throw new IOException("Pagina HEX non valida");
        r.within(n.offset,n.size,r.size);byte[] data=r.bytes(n.offset+page,(int)Math.min(256,n.size-page));StringBuilder out=new StringBuilder();
        for(int i=0;i<data.length;i+=16){out.append(String.format(Locale.ROOT,"%012X  ",n.offset+page+i));for(int j=0;j<16;j++)out.append(i+j<data.length?String.format(Locale.ROOT,"%02X ",data[i+j]&255):"   ");out.append(" ");for(int j=i;j<Math.min(i+16,data.length);j++){int c=data[j]&255;out.append(c>=32&&c<127?(char)c:'.');}out.append('\n');}return out.toString();
    }
    private AnalysisParser(){}
}
