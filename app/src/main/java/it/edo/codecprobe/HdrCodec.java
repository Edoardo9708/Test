package it.edo.codecprobe;

import android.graphics.Rect;
import android.media.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Android-specific P010 access. All sample conversion is in the independent HdrP010Packer. */
@android.annotation.TargetApi(33)
final class HdrCodec {
    static MediaCodec open(MediaFormat format,BooleanSupplier cancelled,Consumer<String> log)throws IOException{
        if(android.os.Build.VERSION.SDK_INT<33)throw new IOException("HDR richiede Android 13/API 33 e un decoder con uscita P010");
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,54);format.setInteger(MediaFormat.KEY_ROTATION,0);
        for(MediaCodecInfo info:new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos()){
            if(info.isEncoder()||!Arrays.asList(info.getSupportedTypes()).contains("video/hevc"))continue;
            if(cancelled.getAsBoolean())throw new IOException("Conversione HDR annullata");
            MediaCodec decoder=null;
            try{
                MediaCodecInfo.CodecCapabilities caps=info.getCapabilitiesForType("video/hevc");boolean supported=false;for(int c:caps.colorFormats)if(c==54)supported=true;
                log.accept("DECODER HDR · "+info.getName()+" · hardware="+info.isHardwareAccelerated()+" · P010="+supported);
                if(!supported||!caps.isFormatSupported(format))continue;
                decoder=MediaCodec.createByCodecName(info.getName());decoder.configure(format,null,null,0);decoder.start();
                log.accept("OK decoder P010 · "+info.getName());return decoder;
            }catch(IOException|RuntimeException e){log.accept("DECODER HDR non disponibile · "+info.getName()+" · "+e);if(decoder!=null)decoder.release();}
        }
        throw new IOException("Nessun decoder P010 accessibile alla CPU: conversione HDR interrotta senza riduzione a 8 bit");
    }
    static void check(MediaFormat f,ColorVui color)throws IOException{
        int transfer=color.transfer==16?6:7,range=color.fullRange?1:2;
        if(number(f,"color-standard")!=6||number(f,"color-transfer")!=transfer||number(f,"color-range")!=range)
            throw new IOException("Colore del buffer HDR modificato o non verificabile: "+f);
    }
    private static int number(MediaFormat f,String key){return f.containsKey(key)?NumberValues.intExact(f.getNumber(key),key):-1;}
    static long pack(Image image,ByteBuffer target,int w,int h)throws IOException{
        if(image==null||image.getFormat()!=54)throw new IOException("Decoder HDR: richiesta Image P010, ricevuta "+(image==null?"null":image.getFormat()));
        Rect crop=image.getCropRect();if(crop.width()!=w||crop.height()!=h)throw new IOException("Dimensioni HDR diverse dall’originale/cambiate: "+crop);
        Image.Plane[] p=image.getPlanes();ByteBuffer[] planes=new ByteBuffer[p.length];int[] rows=new int[p.length],strides=new int[p.length];
        for(int i=0;i<p.length;i++){planes[i]=p[i].getBuffer();rows[i]=p[i].getRowStride();strides[i]=p[i].getPixelStride();}
        return HdrP010Packer.pack(planes,rows,strides,crop.left,crop.top,w,h,target);
    }
    static List<byte[]> csds(MediaFormat format){List<byte[]> values=new ArrayList<>();for(String key:format.getKeys())if(key.startsWith("csd-")){ByteBuffer b=format.getByteBuffer(key).duplicate();byte[] data=new byte[b.remaining()];b.get(data);values.add(data);}return values;}
    static List<HdrSei.Message> metadata(MediaFormat format){List<HdrSei.Message> values=new ArrayList<>();for(byte[] bytes:csds(format))values=HdrSei.merge(values,HdrSei.read(bytes));return values;}
}
