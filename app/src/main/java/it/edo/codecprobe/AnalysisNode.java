package it.edo.codecprobe;

/** Immutable physical byte range. No source buffers, Android objects or decoded pixels. */
final class AnalysisNode {
    final String title, description, parser;
    final long offset, size, content;
    final AnalysisField field;
    AnalysisNode(String title,String description,long offset,long size,String parser,long content){this(title,description,offset,size,parser,content,null);}
    AnalysisNode(String title,String description,long offset,long size,String parser,long content,AnalysisField field){
        if(offset<0||size<0||content<offset||content-offset>size)throw new IllegalArgumentException("Intervallo non valido");
        this.title=title;this.description=description;this.offset=offset;this.size=size;this.parser=parser;this.content=content;this.field=field;
    }
    boolean expandable(){return !parser.isEmpty();}
    long end(){return Math.addExact(offset,size);}
}
