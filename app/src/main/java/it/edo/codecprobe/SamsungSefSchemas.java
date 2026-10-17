package it.edo.codecprobe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Bounded relocation-independent schemas. Playback instructions require an unchanged timeline. */
final class SamsungSefSchemas {
    static String text(SamsungSefDirectory.Record r)throws IOException{
        if(r.payloadBytes()>4096)throw new IOException("Campo descrittivo oltre 4096 byte");
        for(int p=r.payload;p<r.end;p++)if(r.bytes[p]<32||r.bytes[p]>126)throw new IOException("Atteso testo ASCII senza terminatori o byte binari");
        return new String(r.bytes,r.payload,r.payloadBytes(),StandardCharsets.US_ASCII);
    }
    static String validate(SamsungSefDirectory.Record r,String schema)throws IOException{
        if(schema.equals("highlight"))return highlight(r);
        if(schema.equals("super-motion")||schema.equals("super-bgm")||schema.startsWith("deflicker-"))return SamsungSefMotion.validate(r,schema);
        if(schema.equals("invalid-zero")){
            if(r.payloadBytes()<1||r.payloadBytes()>4096)throw new IOException("Invalid_Data: attesi 1–4096 byte interamente nulli");
            for(int p=r.payload;p<r.end;p++)if(r.bytes[p]!=0)throw new IOException("Invalid_Data: variante con byte non nullo non verificata");
            return "contenuto interamente nullo verificato · "+r.payloadBytes()+" byte mantenuti senza interpretare il significato proprietario";
        }
        if(schema.equals("slow-motion"))return slowMotion(r);
        String value=text(r);boolean valid;
        switch(schema){
            case "capture":valid=value.equals("Screen recording");break;
            case "utc":valid=value.matches("[0-9]{1,19}");if(valid)try{Long.parseLong(value);}catch(NumberFormatException e){valid=false;}break;
            case "mcc":valid=value.matches("[0-9]{3}");break;
            case "burst":valid=value.matches("[0-9]{1,19}");break;
            case "mode":valid=value.matches("[0-9]{1,3}");break;
            case "selfie":valid=value.equals("Front_Cam_Selfie_Info");break;
            case "panorama":valid=value.equals("PanoramaShot");break;
            case "jpeg360":valid=value.equals("Jpeg3602D");break;
            case "timezone":
                valid=value.matches("[+-][0-9]{4}");
                if(valid){int h=Integer.parseInt(value.substring(1,3)),m=Integer.parseInt(value.substring(3,5));valid=h<=14&&m<60&&(h!=14||m==0);}break;
            default:throw new IOException("Schema non implementato");
        }
        if(!valid)throw new IOException("Contenuto diverso dallo schema descrittivo verificato: "+schema);
        return "testo descrittivo verificato; valore="+value;
    }
    /** Media3 SefReader: startMs:endMs:speedMode, segments separated by '*'. */
    private static String slowMotion(SamsungSefDirectory.Record r)throws IOException{
        String value=text(r);String[] segments=value.split("\\*",-1);long previousEnd=-1;
        if(segments.length<1||segments.length>256)throw new IOException("Numero intervalli rallentatore non supportato (1–256)");
        StringBuilder detail=new StringBuilder();
        try{
            for(int i=0;i<segments.length;i++){
                String[] fields=segments[i].split(":",-1);
                if(fields.length!=3)throw new IOException("Intervallo rallentatore: attesi inizio_ms:fine_ms:modo");
                long start=nonnegative(fields[0]),end=nonnegative(fields[1]),mode=nonnegative(fields[2]);
                if(start>=end||start<previousEnd||end>Long.MAX_VALUE/1000)throw new IOException("Intervalli rallentatore non ordinati, sovrapposti o fuori timeline rappresentabile");
                // Avoid Java's modulo-32 shift semantics and negative divisors for unknown modes.
                if(mode<1||mode>31)throw new IOException("Modo rallentatore fuori dall’intervallo rappresentabile 1–31");
                long divisor=1L<<(mode-1);previousEnd=end;
                if(i<8)detail.append("\nIntervallo ").append(i+1).append(": ").append(start).append("–").append(end)
                    .append(" ms · modo=").append(mode).append(" · velocità=1/").append(divisor);
            }
        }catch(NumberFormatException error){throw new IOException("Numero o separatori SlowMotion_Data non validi",error);}
        return "schema SlowMotion_Data verificato · intervalli="+segments.length+detail+
            (segments.length>8?"\nAltri intervalli controllati, non elencati per contenere il log":"")+
            "\nIstruzioni di riproduzione conservate; richiedono fotogrammi e timestamp invariati. Compatibilità del lettore Samsung da verificare sul dispositivo dopo il cambio codec.";
    }
    private static String highlight(SamsungSefDirectory.Record r)throws IOException{
        for(int p=r.payload;p<r.end;p++){int c=r.bytes[p]&255;if(c!=10&&c!=13&&c!=9&&(c<32||c>126))throw new IOException("HighlightVideo_Data contiene dati non testuali");}
        String[] t=new String(r.bytes,r.payload,r.payloadBytes(),StandardCharsets.US_ASCII).trim().split("\\s+");
        if(t.length<10||!t[0].equals("AutoSum")||!t[1].equals("10")||!t[2].equals("TMDI")||!t[3].equals("1")||!t[4].equals("21")||!t[6].equals("SMDI")||!t[8].equals("81"))throw new IOException("Schema HighlightVideo_Data non riconosciuto");
        try{
            nonnegative(t[5]);long rows=nonnegative(t[7]);
            if(rows<1||rows>50000||t.length!=9+rows*8)throw new IOException("Righe HighlightVideo_Data incomplete");
            for(int i=0;i<rows;i++){
                int k=9+i*8;long[] v=new long[8];for(int j=0;j<8;j++)v[j]=nonnegative(t[k+j]);
                if(v[0]!=i||v[2]<v[1]||v[2]-v[1]!=v[3]||v[4]!=0||v[5]!=0||v[7]!=0)throw new IOException("Schema highlight riga "+i+" non verificato");
            }
            return "AutoSum v10; righe="+rows+"; timestamp mantenuti";
        }catch(NumberFormatException e){throw new IOException("Numero HighlightVideo_Data non valido",e);}
    }
    private static long nonnegative(String s){if(!s.matches("[0-9]{1,19}"))throw new NumberFormatException();return Long.parseLong(s);}
    private SamsungSefSchemas(){}
}
