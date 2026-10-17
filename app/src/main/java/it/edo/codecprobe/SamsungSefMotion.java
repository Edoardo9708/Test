package it.edo.codecprobe;

import java.io.IOException;

/** Relocation-independent Samsung super-slow-motion text schemas. Never retimes media. */
final class SamsungSefMotion {
    static String validate(SamsungSefDirectory.Record record,String schema)throws IOException{
        String value=SamsungSefSchemas.text(record);
        switch(schema){
            case "super-motion":return regions(value);
            case "super-bgm":return music(value);
            case "deflicker-info":
                require(value.equals("true")||value.equals("false"),"Flag deflickering: atteso true/false");break;
            case "deflicker-on":require(value.equals("T")||value.equals("F"),"Flag deflickering: atteso T/F");break;
            default:throw new IOException("Schema super rallentatore sconosciuto");
        }
        return "flag descrittivo verificato · valore="+value+" · byte originali mantenuti";
    }
    // Samsung SEFHelper.superslowSEFParser: start:end:audioEnd:speed[!flag], '*' between regions.
    // Numbers are milliseconds/enum values, never byte offsets. Samsung uses Java int.
    private static String regions(String value)throws IOException{
        String[] regions=value.split("\\*",-1);require(regions.length>=1&&regions.length<=256,"Attesi 1–256 intervalli super rallentatore");
        long previousEnd=-1;StringBuilder detail=new StringBuilder();
        for(int i=0;i<regions.length;i++){
            String[] f=regions[i].split(":",-1);require(f.length==4,"Intervallo super: attesi inizio_ms:fine_ms:fine_audio_ms:modo[!flag]");
            long start=number(f[0]),end=number(f[1]),audioEnd=number(f[2]);String[] mode=f[3].split("!",-1);
            require(mode.length==1||mode.length==2,"Suffisso super rallentatore non valido");
            long speed=number(mode[0]);if(mode.length==2)number(mode[1]);
            require(start<end&&start>=previousEnd,"Intervalli super rallentatore vuoti, sovrapposti o non ordinati");
            require(audioEnd>=start&&audioEnd<=end,"Fine audio fuori dall’intervallo super rallentatore");
            require(speed>=1&&speed<=9,"Modo super rallentatore fuori dall’enum Samsung 1–9");previousEnd=end;
            if(i<8)detail.append("\nIntervallo ").append(i+1).append(": ").append(start).append("–").append(end)
                .append(" ms · fine audio=").append(audioEnd).append(" ms · modo Samsung=").append(speed)
                .append(mode.length==2?" · flag="+mode[1]:"");
        }
        return "schema super rallentatore verificato · intervalli="+regions.length+detail+
            "\nIstruzioni originali conservate senza riscalare tempi o cambiare velocità. Richiedono tutti i fotogrammi e la timeline originale; verifica video completa prima del salvataggio. Riproduzione specifica Samsung da provare nella Galleria.";
    }
    private static String music(String value)throws IOException{
        String[] f=value.split(":",-1);require(f.length==5||f.length==7,"BGM: attesi 5 o 7 campi testuali");
        long mode=number(f[0]);require(mode==1||mode==2,"BGM: tipo tema non verificato");
        require(!f[1].isEmpty(),"BGM: titolo tema vuoto");
        String path=f[2];require(path.startsWith("themedata/")&&path.endsWith(".ogg")&&path.matches("[A-Za-z0-9 _./()\\-]+"),"BGM: attesa risorsa tema relativa themedata/*.ogg");
        for(String segment:path.split("/",-1))require(!segment.isEmpty()&&!segment.equals(".")&&!segment.equals(".."),"BGM: percorso non relativo al tema");
        bool(f[3]);require(number(f[4])<=100,"BGM: volume fuori intervallo 0–100");
        if(f.length==7){bool(f[5]);require(f[6].equals("null"),"BGM: riferimento esterno non verificato");}
        return "configurazione BGM testuale verificata · tema="+f[1]+" · risorsa relativa="+path+" · volume="+f[4]+
            "\nRiferimento al tema Samsung conservato integralmente; non contiene audio incorporato né offset del vecchio MP4. La disponibilità del tema dipende dalla Galleria Samsung.";
    }
    private static void bool(String value)throws IOException{require(value.equals("true")||value.equals("false"),"BGM: campo booleano non valido");}
    private static long number(String value)throws IOException{
        require(value.matches("[0-9]{1,10}"),"Numero super rallentatore non valido");
        try{long n=Long.parseLong(value);require(n<=Integer.MAX_VALUE,"Numero super rallentatore oltre int Samsung");return n;}
        catch(NumberFormatException e){throw new IOException("Numero super rallentatore non rappresentabile",e);}
    }
    private static void require(boolean valid,String message)throws IOException{if(!valid)throw new IOException(message);}
    private SamsungSefMotion(){}
}
