package it.edo.codecprobe;

import java.util.*;

/** Immutable explanation of actual source fields; no Android objects or full-file buffers. */
final class AnalysisDetail {
    static final class Part {
        final String name,hex,meaning;
        final long offset,size;
        Part(String name,long offset,long size,String hex,String meaning){this.name=name;this.offset=offset;this.size=size;this.hex=hex;this.meaning=meaning;}
    }
    final String introduction,result,alternatives;
    String preview(){
        for(String marker:new String[]{"VALORE TROVATO: ","TESTO LETTO\n","VALORI LETTI\n"}){
            int at=result.indexOf(marker);if(at>=0){int start=at+marker.length(),end=result.indexOf('\n',start);return result.substring(start,end<0?result.length():end);}
        }
        return "";
    }
    final List<Part> parts;
    AnalysisDetail(String introduction,List<Part> parts,String result,String alternatives){
        this.introduction=introduction;this.parts=Collections.unmodifiableList(new ArrayList<>(parts));this.result=result;this.alternatives=alternatives;
    }
}
