package it.edo.codecprobe;

import android.media.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Bounded PCM conversion. No normalization, clipping or guessing an unknown encoding. */
final class AudioPcm {
    static int bytes(int encoding)throws IOException{
        switch(encoding){
            case AudioFormat.ENCODING_PCM_8BIT:return 1;
            case AudioFormat.ENCODING_PCM_16BIT:return 2;
            case AudioFormat.ENCODING_PCM_24BIT_PACKED:return 3;
            case AudioFormat.ENCODING_PCM_FLOAT:case AudioFormat.ENCODING_PCM_32BIT:return 4;
            default:throw new IOException("Formato PCM Android non supportato: "+encoding);
        }
    }
    static void read(ByteBuffer source,int encoding,float[] target,int count)throws IOException{
        int width=bytes(encoding);if(count<0||count>target.length||(long)count*width>source.remaining())throw new IOException("Blocco PCM troncato");
        source.order(ByteOrder.LITTLE_ENDIAN);
        for(int n=0;n<count;n++){
            float value;
            switch(encoding){
                case AudioFormat.ENCODING_PCM_8BIT:value=((source.get()&255)-128)/128f;break;
                case AudioFormat.ENCODING_PCM_16BIT:value=source.getShort()/32768f;break;
                case AudioFormat.ENCODING_PCM_24BIT_PACKED:{int v=(source.get()&255)|((source.get()&255)<<8)|(source.get()<<16);value=v/8388608f;break;}
                case AudioFormat.ENCODING_PCM_32BIT:value=(float)(source.getInt()/2147483648.0);break;
                case AudioFormat.ENCODING_PCM_FLOAT:value=source.getFloat();break;
                default:throw new IOException("Formato PCM non supportato");
            }
            if(!Float.isFinite(value)||value < -1f||value > 1f)throw new IOException("Campione PCM non finito o fuori intervallo");
            target[n]=value;
        }
    }
    private AudioPcm(){}
}
