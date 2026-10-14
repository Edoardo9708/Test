package it.edo.codecprobe;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class OutputNameTest {
    public static void main(String[] args){
        if(!OutputName.base("Vacanze.MP4","HEVC-",true).equals("HEVC-Vacanze.mp4"))throw new AssertionError("Prefix or extension changed incorrectly");
        if(!OutputName.base("Vacanze.mov","_compresso",false).equals("Vacanze_compresso.mp4"))throw new AssertionError("Suffix applied after extension");
        if(!OutputName.base("Video originale.mp4","",true).equals("Video originale.mp4"))throw new AssertionError("Empty addition changed the stem");
        Set<String> names=new HashSet<>(Arrays.asList("video originale.mp4","video originale (2).mp4"));
        if(!OutputName.unique("Video originale.mp4",names).equals("Video originale (3).mp4"))throw new AssertionError("Collision could overwrite source");
        String unsafe=OutputName.base("Video.mp4","../cartella/",true);
        if(unsafe.contains("/")||unsafe.contains("\\"))throw new AssertionError("Filename allowed path traversal");
        String longName=OutputName.base("🌈".repeat(100)+".mp4","✨".repeat(100),true);
        if(longName.getBytes(StandardCharsets.UTF_8).length>224||longName.indexOf('\ufffd')>=0)throw new AssertionError("Unicode filename exceeds safe byte limit");
        if(!OutputName.base("","",true).equals("video.mp4"))throw new AssertionError("Empty filename accepted");
        System.out.println("Output naming: prefix, suffix, no overwrite, traversal and Unicode checks passed");
    }
}
