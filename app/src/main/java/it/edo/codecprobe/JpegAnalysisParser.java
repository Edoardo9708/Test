package it.edo.codecprobe;

import java.io.*;
import java.util.*;

final class JpegAnalysisParser {
    static List<AnalysisNode> read(AnalysisReader r,AnalysisNode root)throws IOException{
        List<AnalysisNode> out=new ArrayList<>();out.add(AnalysisParser.leaf("SOI · inizio JPEG","Marcatore FFD8",0,2));long p=2,end=root.end();boolean ended=false;
        while(p<end){r.check();AnalysisParser.limit(out);long start=p;if(r.u8(p++)!=255)throw new IOException("Marcatore JPEG atteso a "+start);
            int code;do{r.within(p,1,end);code=r.u8(p++);}while(code==255);
            if(code==0)throw new IOException("Marcatore JPEG nullo fuori scansione");
            if(code==0xd9){out.add(AnalysisParser.leaf("EOI · fine JPEG","Marcatore FFD9",start,p-start));ended=true;break;}
            if(code==0xd8||code==1||(code>=0xd0&&code<=0xd7)){out.add(AnalysisParser.leaf("Marcatore FF"+Integer.toHexString(code),"Marcatore senza lunghezza",start,p-start));continue;}
            r.within(p,2,end);int length=r.u16(p);if(length<2)throw new IOException("Lunghezza segmento JPEG non valida");r.within(p,length,end);long payload=p+2,next=p+length;
            String title=title(code),detail="Marcatore FF"+String.format(Locale.ROOT,"%02X",code)+" · payload "+(length-2)+" byte",parser="";
            if(code==0xe1&&length>=8&&r.ascii(payload,6).equals("Exif\0\0")){title+=" · EXIF";detail+="\nStruttura TIFF con tag EXIF";parser="exif";payload+=6;}
            else if(code==0xe2&&length>=14&&r.ascii(payload,12).equals("ICC_PROFILE\0")){title+=" · ICC";}
            else if(code==0xe1&&length>=31&&r.ascii(payload,29).equals("http://ns.adobe.com/xap/1.0/\0")){title+=" · XMP";}
            if((code==0xc0||code==0xc1||code==0xc2)&&length>=8)detail+="\n"+r.u16(p+5)+" × "+r.u16(p+3)+" pixel · "+r.u8(p+2)+" bit";
            out.add(new AnalysisNode(title,detail,start,next-start,parser,parser.isEmpty()?start:payload));p=next;
            if(code==0xda){long stop=scanEnd(r,p,end);AnalysisParser.limit(out);out.add(AnalysisParser.leaf("Scansione · immagine compressa","Byte JPEG compressi; nessuna decodifica dei pixel",p,stop-p));p=stop;}
        }
        if(!ended)throw new IOException("JPEG privo di EOI");
        if(p<end){AnalysisParser.limit(out);out.add(AnalysisParser.leaf("Dati dopo EOI","Coda aggiuntiva: può contenere SEF Samsung o altri dati; record non ancora interpretati",p,end-p));}return out;
    }
    // Scans compressed bytes in bounded chunks, respecting byte stuffing and restart markers.
    private static long scanEnd(AnalysisReader r,long p,long end)throws IOException{
        long ff=-1;
        while(p<end){byte[] b=r.bytes(p,(int)Math.min(65536,end-p));for(int i=0;i<b.length;i++){int c=b[i]&255;if(ff>=0){if(c==255)continue;if(c==0||(c>=0xd0&&c<=0xd7)){ff=-1;continue;}return ff;}if(c==255)ff=p+i;}p+=b.length;}
        throw new IOException("Scansione JPEG troncata");
    }
    private static String title(int c){if(c>=0xe0&&c<=0xef)return "APP"+(c-0xe0);switch(c){case 0xda:return "SOS · inizio scansione";case 0xc0:return "SOF0 · immagine";case 0xc2:return "SOF2 · immagine progressiva";case 0xdb:return "DQT · quantizzazione";case 0xc4:return "DHT · Huffman";case 0xfe:return "COM · commento";default:return "Segmento FF"+Integer.toHexString(c);}}
    private JpegAnalysisParser(){}
}
