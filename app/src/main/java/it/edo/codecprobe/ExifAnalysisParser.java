package it.edo.codecprobe;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** TIFF directory entries and bounded tag previews. MakerNote remains opaque. */
final class ExifAnalysisParser {
    static List<AnalysisNode> read(AnalysisReader r,AnalysisNode n)throws IOException{
        long base=n.content,end=n.end();r.within(base,8,end);String order=r.ascii(base,2);boolean le=order.equals("II");
        if(!le&&!order.equals("MM"))throw new IOException("Ordine byte TIFF non valido");if(r.uint(base+2,2,le)!=42)throw new IOException("Intestazione TIFF non supportata");
        List<AnalysisNode> out=new ArrayList<>();out.add(AnalysisParser.leaf("TIFF · intestazione",le?"Little endian (II)":"Big endian (MM)",base,8));
        ArrayDeque<Long> queue=new ArrayDeque<>();ArrayDeque<String> labels=new ArrayDeque<>();queue.add(r.uint(base+4,4,le));labels.add("IFD0");Set<Long> visited=new HashSet<>();
        while(!queue.isEmpty()){
            long rel=queue.remove();String label=labels.remove();if(rel==0)continue;if(rel<8||!visited.add(rel)||visited.size()>32)throw new IOException("Riferimenti TIFF ciclici o eccessivi");
            long p=base+rel;r.within(p,2,end);int count=(int)r.uint(p,2,le);r.within(p+2,count*12L+4,end);
            AnalysisParser.limit(out);out.add(AnalysisParser.leaf(label+" · directory",count+" tag · valori esterni indicati nei dettagli",p,2));
            long thumbnailOffset=0,thumbnailSize=0;
            for(int i=0;i<count;i++){
                r.check();AnalysisParser.limit(out);long e=p+2+i*12L;int tag=(int)r.uint(e,2,le),type=(int)r.uint(e+2,2,le);long amount=r.uint(e+4,4,le);
                int unit=type==1||type==2||type==6||type==7?1:type==3||type==8?2:type==4||type==9||type==11?4:type==5||type==10||type==12?8:0;
                String detail="Tipo TIFF "+type+" · "+amount+" valori";long value=e+8,bytes=amount*unit;
                if(unit>0){if(bytes>4)value=base+r.uint(e+8,4,le);r.within(value,bytes,end);detail+="\nValore a offset "+value+" · "+bytes+" byte";
                    if(type==2){byte[] text=r.bytes(value,(int)Math.min(bytes,128));detail+="\n"+new String(text,StandardCharsets.UTF_8).replace('\0',' ').replaceAll("[\\p{Cntrl}]"," ").trim()+(bytes>128?"…":"");}
                    else if(amount==1&&(type==3||type==4))detail+="\nValore: "+r.uint(value,unit,le);
                    if(type==4&&amount==1){if(tag==0x201)thumbnailOffset=r.uint(value,4,le);if(tag==0x202)thumbnailSize=r.uint(value,4,le);}
                    if((tag==0x8769||tag==0x8825||tag==0xa005)&&type==4&&amount==1){queue.add(r.uint(value,4,le));labels.add(tag==0x8825?"GPS":tag==0x8769?"EXIF":"Interop");}
                }else detail+="\nTipo non interpretato";
                out.add(AnalysisParser.leaf(label+" · "+name(tag)+String.format(Locale.ROOT," [0x%04X]",tag),detail,e,12));
                if(unit>0&&bytes>4){AnalysisParser.limit(out);out.add(AnalysisParser.leaf("Valore · "+label+" · "+name(tag)+String.format(Locale.ROOT," [0x%04X]",tag),"Dati del tag esterni alla voce della directory · "+bytes+" byte",value,bytes));}
            }
            if(thumbnailOffset!=0&&thumbnailSize!=0){long absolute=base+thumbnailOffset;r.within(absolute,thumbnailSize,end);AnalysisParser.limit(out);out.add(AnalysisParser.leaf("Miniatura JPEG negli EXIF","Immagine incorporata · dati indicati da JPEGInterchangeFormat e JPEGInterchangeFormatLength",absolute,thumbnailSize));}
            long nextPos=p+2+count*12L;AnalysisParser.limit(out);long next=r.uint(nextPos,4,le);out.add(AnalysisParser.leaf(label+" · IFD successivo","Offset relativo: "+next,nextPos,4));if(next!=0){queue.add(next);labels.add("IFD successivo / miniatura");}
        }return out;
    }
    private static String name(int tag){switch(tag){case 0x10f:return "Marca";case 0x110:return "Modello";case 0x112:return "Orientamento";case 0x132:return "Data modifica";case 0x9003:return "Data scatto";case 0x927c:return "MakerNote";case 0x8769:return "Puntatore EXIF";case 0x8825:return "Puntatore GPS";case 0x201:return "Offset miniatura JPEG";case 0x202:return "Dimensione miniatura JPEG";default:return "Tag";}}
    private ExifAnalysisParser(){}
}
