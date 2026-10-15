package it.edo.codecprobe;
import java.nio.file.*;
public final class HdrHeaderCheck {
    public static void main(String[] args)throws Exception{
        byte[] bytes=Files.readAllBytes(Path.of(args[0]));
        System.out.println("Java HDR SPS verification: "+HdrHevcInfo.requireMain10(bytes));
        ColorVui color=ColorVui.read("video/hevc",bytes);
        if(color.primaries!=9||color.transfer!=Integer.parseInt(args[1])||color.matrix!=9||color.fullRange)throw new AssertionError("HDR CICP changed: "+color);
    }
}
