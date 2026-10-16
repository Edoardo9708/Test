package it.edo.codecprobe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class Mp4MetadataDiagnosticsTest {
    private byte[] boxes(String... types){ByteBuffer data=ByteBuffer.allocate(types.length*8);for(String type:types){data.putInt(8);data.put(type.getBytes(java.nio.charset.StandardCharsets.US_ASCII));}return data.array();}
    private String rejected(String... types)throws Exception{
        Path file=Files.createTempFile("root-layout-",".mp4");
        try{Files.write(file,boxes(types));try(FileChannel in=FileChannel.open(file)){Mp4Metadata.check(in,()->false);fail("Invalid movie count accepted");}return "";}
        catch(IOException error){return error.getMessage();}finally{Files.delete(file);}
    }
    @Test public void missingAndDuplicateMoovHaveCountsSizeAndRootLayout()throws Exception{
        String missing=rejected("ftyp","mdat");assertTrue(missing.contains("trovati 0"));assertTrue(missing.contains("16 byte"));assertTrue(missing.contains("mdat@offset=8 byte=8"));
        String duplicate=rejected("ftyp","moov","moov");assertTrue(duplicate.contains("trovati 2"));assertTrue(duplicate.contains("24 byte"));assertTrue(duplicate.contains("moov@offset=16"));
    }
}
