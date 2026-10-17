package it.edo.codecprobe;

import java.nio.*;
import java.io.ByteArrayOutputStream;

// Complete blocked payloads reconstructed from the v0.65 log; no video, GPS or filenames.
final class SamsungSefMotionFixtures {
    static final String[][] RECORDS={
        {"7ffe","Invalid_Data","0000000000000000000000000000","e7ecebbc590bc88b3761fa6cd03d749f87463dabb67021a5c6768c25ec68b3f2"},
        {"0b02","Super_SlowMotion_Deflickering_Info","66616c7365","fcbcf165908dd18a9e49f7ff27810176db8e9f63b4352213741664245224f8aa"},
        {"0890","Super_SlowMotion_BGM","323a536572656e653a7468656d65646174612f62676d2f352d536572656e652e6f67673a747275653a313030","f24f37faf40adfdb7fac272c2f94406eee8327c84e4320dcfa91d600d954d69c"},
        {"0890","Super_SlowMotion_Data","3936363a373239393a313136363a312131","e1dcb0935ca7a70239e681c93564b8348bf8cbfbfad800d129ba49ff1416b19d"},
        {"0b04","Super_SlowMotion_Deflickering_On","46","f67ab10ad4e4c53121b6a5fe4da9c10ddee905b978d3788d2723d7bfacbe28a9"},
        {"0b03","Super_SlowMotion_Edit_Data","3936363a373336363a313136363a312131","c6ceb397709135e382b7298af40bebb84dd12d8499e9e0d2de99548cb845f08b"},
        {"0890","Super_SlowMotion_Data","3936363a373336363a313136363a312131","c6ceb397709135e382b7298af40bebb84dd12d8499e9e0d2de99548cb845f08b"},
        {"0890","Super_SlowMotion_BGM","313a54726176656c3a7468656d65646174612f7468656d656d757369632f48617070792f54726176656c2e6f67673a747275653a3130303a66616c73653a6e756c6c","033e8a50aff97622df5eed01c845a2f3d5bcd95489689a04adcb921e41e116bc"},
        {"0890","Super_SlowMotion_BGM","313a426c7565204a6f6e65733a7468656d65646174612f7468656d656d757369632f4c6f756e67652f426c7565204a6f6e65732e6f67673a747275653a3130303a66616c73653a6e756c6c","ffb301f963f24a416a220f0f9212804be0c13c1f9289dd464e7b38f6ed878f8a"},
        {"0b03","Super_SlowMotion_Edit_Data","3936363a373239393a313136363a312131","e1dcb0935ca7a70239e681c93564b8348bf8cbfbfad800d129ba49ff1416b19d"},
        {"0890","Super_SlowMotion_BGM","313a57686973746c6520596f75722043617265733a7468656d65646174612f7468656d656d757369632f48617070792f57686973746c6520596f75722043617265732e6f67673a747275653a3130303a66616c73653a6e756c6c","81f610a27e95d2aa41d8a9575e5fdddab5d91b062def1fa61611c61c51cf31b8"},
        {"0b00","Super_SlowMotion_Data","3936363a323733333a323733333a312131","b9be8fa2581163c14b065bdfd6b2c1f16ff0c28e4f29c1fa6c47b047f3f17d6d"},
        {"0890","Super_SlowMotion_BGM","313a52756e3a7468656d65646174612f7468656d656d757369632f5570626561742f52756e2e6f67673a747275653a3130303a66616c73653a6e756c6c","36860ce9887cc0235efa88a3fbd11fee72668a8062b14f4bc1c8372c3df3bcad"},
        {"7ffe","Invalid_Data","0000000000000000000000000000000000000000000000000000","659d36ca563ba4622daabb36a71dafaf6060cdcbf89bb12e75426198496d272c"},
        {"0b00","Super_SlowMotion_Data","313030303a31363836363a31363836363a312131","1cfaf36efbd2a7e8ad228bfb943647079342e214e29709a1597d3c4400b9fdf6"},
    };
    static final class Item {
        final int type;final String name;final byte[] payload;
        Item(int type,String name,String payload){this(type,name,payload.getBytes(java.nio.charset.StandardCharsets.US_ASCII));}
        Item(int type,String name,byte[] payload){this.type=type;this.name=name;this.payload=payload;}
    }
    static byte[] fixture(int version,Item... items){
        int length=8+20+12*items.length;for(Item item:items)length+=8+item.name.length()+item.payload.length;
        ByteBuffer out=ByteBuffer.allocate(length);out.putInt(length).put("sefd".getBytes(java.nio.charset.StandardCharsets.US_ASCII));out.order(ByteOrder.LITTLE_ENDIAN);
        int[] offsets=new int[items.length],sizes=new int[items.length];
        for(int n=0;n<items.length;n++){
            Item item=items[n];offsets[n]=out.position();byte[] name=item.name.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            out.putShort((short)0).putShort((short)item.type).putInt(name.length).put(name).put(item.payload);sizes[n]=out.position()-offsets[n];
        }
        int directory=out.position();out.put("SEFH".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(version).putInt(items.length);
        for(int n=0;n<items.length;n++)out.putShort((short)0).putShort((short)items[n].type).putInt(directory-offsets[n]).putInt(sizes[n]);
        out.putInt(12+12*items.length).put("SEFT".getBytes(java.nio.charset.StandardCharsets.US_ASCII));return out.array();
    }
    static byte[] box(String type,byte[] data){ByteBuffer b=ByteBuffer.allocate(8+data.length);b.putInt(b.capacity()).put(type.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)).put(data);return b.array();}
    static byte[] join(byte[]... data)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();for(byte[] p:data)b.write(p);return b.toByteArray();}
    static byte[] content(boolean tags)throws Exception{return join(box("ftyp",new byte[16]),box("mdat",new byte[]{11,22,33,44,55}),box("moov",tags?box("udta",box("name",new byte[]{7,8,9})):new byte[0]));}
    private SamsungSefMotionFixtures(){}
}
