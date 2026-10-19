package it.edo.codecprobe;

import android.util.JsonWriter;
import java.io.*;

/** Versioned diagnostic export. Payloads are original JPEG metadata, never reconstructed tags. */
final class ImageReport {
    static void write(Writer output,String name,JpegInventory.Info info)throws IOException{
        JsonWriter json=new JsonWriter(output);json.setIndent("  ");json.beginObject();
        json.name("schema").value("codecprobe.jpeg-inventory.v1");json.name("appVersion").value(BuildConfig.VERSION_NAME);
        json.name("fileName").value(name);json.name("format").value("JPEG");json.name("bytes").value(info.bytes);json.name("sha256").value(info.sha256);
        json.name("width").value(info.width);json.name("height").value(info.height);json.name("precision").value(info.precision);json.name("components").value(info.components);json.name("scans").value(info.scans);
        json.name("pixelsDecoded").value(false);json.name("heicConversionApproved").value(false);json.name("iccChunkSequenceComplete").value(info.iccComplete);
        json.name("metadataPayloads").beginArray();
        for(JpegInventory.Block block:info.blocks){
            json.beginObject();json.name("marker").value(String.format(java.util.Locale.ROOT,"FF%02X",block.marker));json.name("kind").value(block.kind);json.name("payloadOffset").value(block.offset);json.name("bytes").value(block.size());json.name("sha256").value(block.sha256);json.name("base64").value(block.base64());json.endObject();
        }
        json.endArray();json.name("trailingBytes").value(info.trailingBytes);json.name("trailingSha256").value(info.trailingSha256);
        json.name("scope").value("Inventario strutturale: APP0–APP15 e commenti completi in base64, anche dopo la prima scansione. I dati dopo EOI sono conteggiati e identificati con SHA-256, non inclusi in base64. Nessuna decodifica dei pixel o validazione semantica di EXIF/XMP/ICC. Non è una copia della foto né un file HEIC.");
        json.endObject();json.flush();
    }
    private ImageReport(){}
}
