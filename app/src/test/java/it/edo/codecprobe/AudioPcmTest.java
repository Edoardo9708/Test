package it.edo.codecprobe;

import android.media.AudioFormat;
import java.nio.*;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
public class AudioPcmTest {
    @Test public void detectsSupportedInputsWithoutChangingFlacRoute()throws Exception{
        assertEquals(0,AudioMediaDecoder.kind("song.FLAC"));assertEquals(1,AudioMediaDecoder.kind("song.MP3"));assertEquals(2,AudioMediaDecoder.kind("song.m4a"));
        assertThrows(IOException.class,()->AudioMediaDecoder.kind("song.opus"));
    }
    @Test public void decodesSignedAndUnsignedSamplesWithCorrectScale()throws Exception{
        float[] out=new float[3];
        AudioPcm.read(ByteBuffer.wrap(new byte[]{0,(byte)128,(byte)255}),AudioFormat.ENCODING_PCM_8BIT,out,3);
        assertArrayEquals(new float[]{-1,0,127/128f},out,0);
        ByteBuffer b=ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).putShort(Short.MIN_VALUE).putShort((short)0).putShort(Short.MAX_VALUE);b.flip();
        AudioPcm.read(b,AudioFormat.ENCODING_PCM_16BIT,out,3);assertArrayEquals(new float[]{-1,0,32767/32768f},out,0);
        AudioPcm.read(ByteBuffer.wrap(new byte[]{0,0,(byte)128,0,0,0,(byte)255,(byte)255,127}),AudioFormat.ENCODING_PCM_24BIT_PACKED,out,3);
        assertArrayEquals(new float[]{-1,0,8388607/8388608f},out,0);
    }
    @Test public void handlesFloatOffsetsAndRejectsCorruption()throws Exception{
        float[] out=new float[2];ByteBuffer b=ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);b.putFloat(99).putFloat(-.5f).putFloat(.75f).putFloat(99);b.position(4);b.limit(12);
        AudioPcm.read(b,AudioFormat.ENCODING_PCM_FLOAT,out,2);assertArrayEquals(new float[]{-.5f,.75f},out,0);
        ByteBuffer invalid=ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(Float.NaN);invalid.flip();
        assertThrows(IOException.class,()->AudioPcm.read(invalid,AudioFormat.ENCODING_PCM_FLOAT,out,1));
        assertThrows(IOException.class,()->AudioPcm.read(ByteBuffer.allocate(1),AudioFormat.ENCODING_PCM_16BIT,out,1));
        assertThrows(IOException.class,()->AudioPcm.bytes(9999));
    }
}
