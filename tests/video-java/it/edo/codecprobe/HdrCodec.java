package it.edo.codecprobe;
import android.media.MediaFormat;
import java.util.*;
final class HdrCodec {
    static List<byte[]> csds(MediaFormat format){return Collections.singletonList(format.csd);}
}
