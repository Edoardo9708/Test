package it.edo.codecprobe;

import java.util.*;

/** Inventory text formatted once per parsed inventory, without a hidden TextView. */
final class PhotoInventoryText {
    static String format(JpegInventory.Info info){
        LinkedHashMap<String,Integer> counts=new LinkedHashMap<>();for(JpegInventory.Block b:info.blocks)counts.put(b.kind,counts.getOrDefault(b.kind,0)+1);
        StringBuilder detail=new StringBuilder();for(Map.Entry<String,Integer> e:counts.entrySet())detail.append(e.getKey()).append(": ").append(e.getValue()).append(" blocchi\n");
        if(counts.isEmpty())detail.append("Nessun blocco APP o commento presente.\n");
        detail.append("\nMetadati inventariati: ").append(info.metadataBytes()).append(" byte");
        if(info.count("EXIF")>0)detail.append("\nEXIF presente: i dati originali sono inclusi nel report. In conversione l’orientamento viene applicato al contenitore; EXIF viene conservato integralmente.");
        if(info.count("ICC")>0)detail.append(info.iccComplete?"\nSequenza dei blocchi ICC completa (profilo non validato).":"\nAttenzione: sequenza ICC incompleta o duplicata.");
        if(info.trailingBytes>0)detail.append("\nAttenzione: ").append(info.trailingBytes).append(" byte dopo la fine JPEG. Potrebbero contenere altre immagini o dati Motion Photo: tipo da verificare. Il report contiene conteggio e hash, non questi byte.");
        detail.append("\n\nSHA-256 originale\n").append(info.sha256);return detail.toString();
    }
    private PhotoInventoryText(){}
}
