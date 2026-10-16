package it.edo.codecprobe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** SEF coordinator: structural checks, name-centric catalog, payload policy and diagnostics. */
final class SamsungSef {
    static final int MAX_BYTES=4*1024*1024;
    static String validate(byte[] bytes)throws IOException{return validate(bytes,-1);}
    static String validate(byte[] bytes,long absoluteOffset)throws IOException{
        SamsungSefDirectory directory;
        try{directory=SamsungSefDirectory.parse(bytes);}
        catch(IOException error){throw new IOException(SamsungSefDiagnostics.structural(bytes,absoluteOffset,error.getMessage()),error);}
        List<String> reasons=new ArrayList<>();int blocked=0,highlights=0,captures=0;
        StringBuilder inventory=new StringBuilder();
        for(SamsungSefDirectory.Record record:directory.records){
            SamsungSefCatalog.Entry entry=SamsungSefCatalog.find(record.name);String reason=null,detail="";
            if(entry==null)reason="Nome e schema non presenti nel catalogo";
            else if(!entry.matches(record.type))reason="Nome conosciuto ma tipo inatteso: previsto "+SamsungSefDiagnostics.type(entry.type)
                +(entry.alternate!=0?" oppure "+SamsungSefDiagnostics.type(entry.alternate):"");
            else if(entry.schema==null)reason=entry.reason;
            else try{detail=SamsungSefSchemas.validate(record,entry.schema);}
                catch(IOException error){reason="Nome/tipo riconosciuti, contenuto diverso dalla variante supportata: "+error.getMessage();}
            reasons.add(reason);if(reason!=null)blocked++;
            else {if(record.name.equals("HighlightVideo_Data"))highlights++;if(record.name.equals("Samsung_Capture_Info"))captures++;}
            if(record.number<=SamsungSefDiagnostics.MAX_LISTED_RECORDS){
                inventory.append("\n").append(record.number).append(". ").append(record.name).append(" · ").append(SamsungSefDiagnostics.type(record.type))
                    .append(" · payload=").append(record.payloadBytes()).append(" byte\n   ")
                    .append(entry==null?"Descrizione non disponibile":entry.description).append("\n   ")
                    .append(reason==null?"COPIABILE · "+detail:"BLOCCATO · "+reason);
            }
        }
        String header="SEF versione="+directory.version+" · record="+directory.records.size()+" · blocco="+bytes.length+" byte"
            +" · HighlightVideo_Data="+highlights+" · Samsung_Capture_Info="+captures
            +"\nDirectory SEFH a offset relativo="+directory.start+" · riferimenti interni verificati"
            +"\nCatalogo: "+SamsungSefCatalog.size()+" nomi riconosciuti · "+SamsungSefCatalog.supported()+" schemi con copia verificata"
            +"\nEsito: "+(directory.records.size()-blocked)+" copiabili · "+blocked+" bloccati";
        if(directory.records.size()>SamsungSefDiagnostics.MAX_LISTED_RECORDS)inventory.append("\nElenco limitato ai primi ").append(SamsungSefDiagnostics.MAX_LISTED_RECORDS)
            .append(" record; tutti i ").append(directory.records.size()).append(" record sono stati controllati.");
        String report=header+"\n*** ELENCO RECORD SAMSUNG SEF ***"+inventory;
        if(blocked>0){
            StringBuilder details=new StringBuilder();int shown=0;
            for(int n=0;n<reasons.size()&&shown<SamsungSefDiagnostics.MAX_DETAIL_RECORDS;n++)if(reasons.get(n)!=null){
                details.append("\n\n").append(SamsungSefDiagnostics.record(directory.records.get(n),absoluteOffset,reasons.get(n)));shown++;
            }
            if(blocked>shown)details.append("\nAltri ").append(blocked-shown).append(" record bloccati: estratti omessi per contenere il log.");
            throw new IOException("Metadati Samsung SEF: "+blocked+" record non copiabili con verifica completa. Nessun metadato è stato scartato.\n"
                +"*** DIAGNOSTICA SAMSUNG SEF ***\n"+report+details);
        }
        return report;
    }
    private SamsungSef(){}
}
