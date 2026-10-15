package it.edo.codecprobe;

import java.io.IOException;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

public final class SamsungSefTest {
    // Exact 84-byte sefd box from the uploaded Samsung screen recording. No media or personal data.
    static byte[] screen(){return HexFormat.of().parseHex("00000054736566640000510c1400000053616d73756e675f436170747572655f496e666f53637265656e207265636f7264696e67534546486b000000010000000000510c2c0000002c0000001800000053454654");}
    private static void reject(byte[] b)throws Exception{try{SamsungSef.validate(b);}catch(IOException expected){return;}throw new AssertionError("Invalid SEF accepted");}
    public static void main(String[] args)throws Exception{
        byte[] original=screen(),copy=original.clone();String result=SamsungSef.validate(original);
        if(!result.contains("107")||!result.contains("Samsung_Capture_Info=1")||!Arrays.equals(original,copy))throw new AssertionError(result);
        byte[] corrupt=original.clone();corrupt[56]=108;reject(corrupt);
        corrupt=original.clone();ByteBuffer.wrap(corrupt).order(ByteOrder.LITTLE_ENDIAN).putInt(68,45);reject(corrupt);
        corrupt=original.clone();corrupt[36]=0;reject(corrupt);
        corrupt=original.clone();corrupt[10]=0;reject(corrupt);
        corrupt=original.clone();corrupt[16]='X';reject(corrupt);
        corrupt=original.clone();corrupt[64]=1;reject(corrupt);
        corrupt=original.clone();ByteBuffer.wrap(corrupt).order(ByteOrder.LITTLE_ENDIAN).putInt(72,45);reject(corrupt);
        if(args.length>0){byte[] real=Files.readAllBytes(Path.of(args[0]));if(!Arrays.equals(real,original))throw new AssertionError("Fixture differs from actual file");SamsungSef.validate(real);}
        System.out.println("OK Samsung screen SEF v107: exact original fixture, no mutation; unknown version/type/name/payload and invalid references rejected");
    }
}
