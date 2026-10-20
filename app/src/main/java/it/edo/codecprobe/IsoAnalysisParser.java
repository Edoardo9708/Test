package it.edo.codecprobe;

import java.io.*;
import java.util.*;

/** Descends only into known box containers; opaque payloads are never guessed as boxes. */
final class IsoAnalysisParser {
    private static final Set<String> CONTAINERS=new HashSet<>(Arrays.asList("moov","trak","mdia","minf","stbl","edts","dinf","udta","iprp","ipco","meco","moof","traf","mvex","tref","sinf","schi"));
    static List<AnalysisNode> read(AnalysisReader r,AnalysisNode parent)throws IOException{
        List<AnalysisNode> out=new ArrayList<>();long p=parent.content,end=parent.end();
        while(p<end){r.check();AnalysisParser.limit(out);r.within(p,8,end);long length=r.uint(p,4,false);String type=r.ascii(p+4,4);int header=8;
            if(length==1){r.within(p,16,end);length=r.uint(p+8,8,false);header=16;}else if(length==0)length=end-p;
            if(type.equals("uuid"))header+=16;
            if(length<header)throw new IOException("Box "+type+" con dimensione non valida a "+p);r.within(p,length,end);
            String parser="";long content=p+header;String detail=description(type)+"\nIntestazione: "+header+" byte";
            if(CONTAINERS.contains(type))parser="boxes";
            if(type.equals("meta")||type.equals("iref")){r.within(content,4,p+length);content+=4;parser="boxes";}
            if(type.equals("iinf")){r.within(content,6,p+length);int version=r.u8(content);int skip=version==0?6:8;r.within(content,skip,p+length);content+=skip;parser="boxes";}
            if(type.equals("ftyp")){r.within(content,8,p+length);detail+="\nMarca principale: "+r.ascii(content,4);}
            out.add(new AnalysisNode(type,detail,p,length,parser,content));p+=length;
        }return out;
    }
    private static String description(String t){switch(t){
        case "ftyp":return "Identifica formato e compatibilità";case "meta":return "Indice degli elementi e delle proprietà";case "mdat":return "Dati effettivi: media e possibili metadati. Per identificare ogni item servono i riferimenti di iloc";
        case "moov":return "Descrizione del filmato e delle tracce";case "iinf":return "Elenco degli elementi";case "iloc":return "Posizioni e lunghezze degli elementi (non ancora interpretate)";
        case "iref":return "Collegamenti tra elementi";case "iprp":return "Proprietà delle immagini";case "ipco":return "Contenitore delle proprietà";case "thmb":return "Riferimento miniatura → immagine principale";case "colr":return "Descrizione del colore / profilo ICC";case "Exif":return "Elemento EXIF";
        default:return "Box del contenitore · contenuto "+(CONTAINERS.contains(t)?"esplorabile":"non interpretato in questa bozza");}}
    private IsoAnalysisParser(){}
}
