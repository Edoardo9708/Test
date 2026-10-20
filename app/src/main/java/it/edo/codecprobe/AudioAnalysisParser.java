package it.edo.codecprobe;

import java.io.*;
import java.util.*;

final class AudioAnalysisParser {
    static List<AnalysisNode> read(AnalysisReader r,AnalysisNode root)throws IOException{
        List<AnalysisNode> out=new ArrayList<>();long p;
        if(root.parser.equals("flac")){
            out.add(AnalysisParser.leaf("fLaC · firma","Identifica il formato FLAC",0,4));p=4;boolean last=false;
            String[] names={"STREAMINFO","PADDING","APPLICATION","SEEKTABLE","VORBIS_COMMENT · tag e testi","CUESHEET","PICTURE · copertina"};
            while(!last){r.check();AnalysisParser.limit(out);r.within(p,4,r.size);int h=r.u8(p),type=h&127;last=(h&128)!=0;long size=r.uint(p+1,3,false)+4;r.within(p,size,r.size);out.add(AnalysisParser.leaf(type<names.length?names[type]:"Blocco FLAC "+type,"Metadati FLAC · intestazione 4 byte",p,size));p+=size;}
        }else{
            r.within(0,10,r.size);int version=r.u8(3),flags=r.u8(5);if(version<2||version>4)throw new IOException("Versione ID3 non supportata");long size=0;for(byte b:r.bytes(6,4)){if((b&128)!=0)throw new IOException("Dimensione ID3 non valida");size=(size<<7)|(b&127);}p=10+size+(version==4&&(flags&16)!=0?10:0);r.within(0,p,r.size);out.add(AnalysisParser.leaf("ID3v2."+version,"Tag audio (frame interni non ancora interpretati)",0,p));
        }
        if(p<r.size)out.add(AnalysisParser.leaf("Audio e dati rimanenti","Dati compressi non decodificati; eventuali tag finali non ancora separati",p,r.size-p));return out;
    }
    private AudioAnalysisParser(){}
}
