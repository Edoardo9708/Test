package it.edo.codecprobe;

import org.junit.Test;
import static org.junit.Assert.*;

public class AnalysisLabelsTest {
    private AnalysisLabels.Label label(String title){return AnalysisLabels.of(AnalysisParser.leaf(title,"",0,8));}
    @Test public void referencesAreNotDescribedAsImagePayloads(){
        assertEquals("Indice / collegamento",label("thmb").kind);assertTrue(label("thmb").meaning.contains("non i pixel"));
        assertTrue(label("meta").meaning.contains("altrove"));assertEquals("Contenitore",label("meta").kind);assertEquals("Dati",label("mdat").kind);
    }
    @Test public void technicalIdentifiersHaveReadableNamesWithoutInventingUnknownContent(){
        assertEquals("Formato e compatibilità",label("ftyp").name);assertEquals("Blocco non riconosciuto",label("zzzz").name);
        assertEquals("Dati dell’immagine compressa",label("Scansione · immagine compressa").name);
        assertEquals("Contenitore",label("APP1 · EXIF").kind);
    }
    @Test public void exifFactsExplainTypeAndOrientationWithoutChangingRawValue(){
        AnalysisNode n=AnalysisParser.leaf("IFD0 · Orientamento [0x0112]","Tipo TIFF 3 · 1 valori\nValore a offset 30 · 2 byte\nValore: 6",22,12);
        String facts=AnalysisLabels.facts(n);assertTrue(facts.contains("intero senza segno a 16 bit"));assertTrue(facts.contains("Valore: 6"));assertTrue(facts.contains("90° in senso orario"));
    }
    @Test public void malformedOrTextOrientationIsNotInterpretedAsANumber(){
        for(String detail:new String[]{"Tipo TIFF 4 · 1 valori\nValore: 4294967295","Tipo TIFF 2 · 1 valori\nValore: 6","Tipo TIFF 3 · 1 valori\nValore: 99"}){
            assertFalse(AnalysisLabels.facts(AnalysisParser.leaf("IFD0 · Orientamento [0x0112]",detail,0,12)).contains("Interpretazione:"));
        }
    }
    @Test public void exifTagAndExternalValueKeepTheirMeaning(){
        assertEquals("Orientamento della foto",label("IFD0 · Orientamento [0x0112]").name);
        assertEquals("Latitudine",label("GPS · Tag [0x0002]").name);
        assertEquals("Valore di Latitudine",label("Valore · GPS · Tag [0x0002]").name);
        assertEquals("Tag EXIF non tradotto",label("IFD0 · Tag [0xFFFF]").name);
    }
}
