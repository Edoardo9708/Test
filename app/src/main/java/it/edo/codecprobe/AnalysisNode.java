package it.edo.codecprobe;

/** Immutable physical byte range. No source buffers, Android objects or decoded pixels. */
final class AnalysisNode {
    final String title, description, parser;
    final long offset, size, content;
    AnalysisNode(String title,String description,long offset,long size,String parser,long content){
        if(offset<0||size<0||content<offset||content-offset>size)throw new IllegalArgumentException("Intervallo non valido");
        this.title=title;this.description=description;this.offset=offset;this.size=size;this.parser=parser;this.content=content;
    }
    boolean expandable(){return !parser.isEmpty();}
    long end(){return Math.addExact(offset,size);}
}
