package it.edo.codecprobe;

import java.util.Locale;
import java.util.Set;
import java.nio.charset.StandardCharsets;

/** Naming policy only; all outputs are new MP4 documents, never overwritten. */
final class OutputName {
    private static String clean(String value){return value.replaceAll("[\\p{Cntrl}/\\\\:*?\"<>|]","_");}
    static String base(String original,String addition,boolean prefix){
        String stem=original==null?"video":original;int dot=stem.lastIndexOf('.');if(dot>0)stem=stem.substring(0,dot);
        stem=clean(stem);if(stem.trim().isEmpty())stem="video";
        String tag=clean(addition==null?"":addition);
        String result=prefix?tag+stem:stem+tag;
        result=result.trim();if(result.isEmpty()||result.equals(".")||result.equals(".."))result="video";
        while(result.getBytes(StandardCharsets.UTF_8).length>220){int end=result.offsetByCodePoints(result.length(),-1);result=result.substring(0,end);}
        return result+".mp4";
    }
    static String unique(String requested,Set<String> occupied){
        String candidate=requested;int n=2;String stem=requested.substring(0,requested.length()-4);
        while(occupied.contains(candidate.toLowerCase(Locale.ROOT))){candidate=stem+" ("+(n++)+").mp4";}
        return candidate;
    }
}
