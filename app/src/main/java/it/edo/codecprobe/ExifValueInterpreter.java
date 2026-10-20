package it.edo.codecprobe;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Tag-specific meaning, kept separate from TIFF layout and bounded raw-value previews. */
final class ExifValueInterpreter {
    static final class Result {
        final String value, alternatives;
        Result(String value,String alternatives){this.value=value;this.alternatives=alternatives;}
    }
    static Result read(AnalysisReader r,AnalysisField f,long value)throws IOException{
        ExifTagDictionary.Entry e=ExifTagDictionary.find(f);
        if(e==null)return result("","");
        if(!e.choices.isEmpty()){
            boolean valid=f.type==e.choiceType&&(f.type==2?f.count==2:f.count==1);
            if(!valid)return result("Interpretazione specifica non applicata: tipo o quantità non corrispondono al formato previsto per questo codice.",choices(e,null));
            String actual=f.type==2?new String(r.bytes(value,2),StandardCharsets.US_ASCII):Long.toString(r.uint(value,f.unit,f.little));
            if(f.type==2){
                if(actual.charAt(1)!=0)return result("Codice testuale non valido: manca il terminatore nullo atteso.",choices(e,null));
                actual=actual.substring(0,1);
            }
            String meaning=e.choices.get(actual);
            return result(actual+" → "+(meaning==null?"valore non riconosciuto; nessun significato viene dedotto":meaning),choices(e,actual));
        }
        if(f.gps)return gps(r,f,value);
        if(f.directory.equals("Interop")){
            if(f.tag==2)return version(r,f,value);
            return result("","");
        }
        if(f.tag==0x9000||f.tag==0xa000)return version(r,f,value);
        if(f.tag==0x9286)return encodedText(r,f,value);
        if(f.tag==0x9209&&f.type==3&&f.count==1)return flash(r.uint(value,2,f.little));
        if(f.tag==0x9101&&f.type==7&&f.count==4){
            String[] names={"assente","Y","Cb","Cr","R","G","B"};StringBuilder out=new StringBuilder();
            for(byte b:r.bytes(value,4)){int code=b&255;if(out.length()>0)out.append(", ");out.append(code).append(" = ").append(code<names.length?names[code]:"componente non riconosciuta");}
            return result(out.toString(),"");
        }
        if(f.tag==0xa40d&&f.type==3&&f.count==2){
            long a=r.uint(value,2,f.little),b=r.uint(value+2,2,f.little);
            return result("riproduzione: "+a+" → "+(a==1?"fedele":a==2?"piccole differenze":a==4?"differenze estreme":"codice non riconosciuto")+
                "; impostazioni: "+b+" → "+(b==1?"di fabbrica":b==2?"diverse da quelle di fabbrica":b==4?"sconosciute":"codice non riconosciuto"),
                "PRIMO CODICE\n1 = riproduzione fedele\n2 = piccole differenze\n4 = differenze estreme\nSECONDO CODICE\n1 = impostazioni di fabbrica\n2 = impostazioni non di fabbrica\n4 = impostazioni sconosciute");
        }
        if(f.tag==0xa461&&f.type==3&&f.count==2)return result(r.uint(value,2,f.little)+" immagini sorgenti; "+r.uint(value+2,2,f.little)+" immagini utilizzate","");
        if((f.tag==0xa214||f.tag==0x9214)&&f.type==3){
            if(f.count<2||f.count>4||f.tag==0xa214&&f.count!=2)return result("Quantità non prevista per le coordinate del soggetto; nessuna geometria viene dedotta.","");
            String text="x orizzontale = "+r.uint(value,2,f.little)+" px; y verticale = "+r.uint(value+2,2,f.little)+" px";
            if(f.count==3)text+="; diametro = "+r.uint(value+4,2,f.little)+" px";
            if(f.count==4)text+="; larghezza = "+r.uint(value+4,2,f.little)+" px; altezza = "+r.uint(value+6,2,f.little)+" px";
            return result(text,"");
        }
        if(f.tag==0xa432&&f.type==5&&f.count==4){
            double[] a=fractions(r,f,value,4);
            if(a==null)return result("Una frazione ha denominatore zero; caratteristiche dell’obiettivo non calcolabili.","");
            return result("focale "+number(a[0])+"–"+number(a[1])+" mm; apertura minima f/"+number(a[2])+" alla focale minima, f/"+number(a[3])+" alla focale massima","");
        }
        if(f.count!=1)return result("","");
        if(f.tag==0xa405&&(f.type==3||f.type==4)){long v=r.uint(value,f.unit,f.little);return result(v==0?"0 → focale equivalente sconosciuta":v+" mm equivalenti su formato 35 mm","");}
        if(f.tag==0x9206&&f.type==5){
            long a=r.uint(value,4,f.little);
            if(a==0)return result("0 → distanza sconosciuta","");
            if(a==0xffffffffL)return result("4294967295 nel numeratore → distanza infinita","");
        }
        boolean signed=f.tag==0x9201||f.tag==0x9203||f.tag==0x9204||f.tag==0x9400||f.tag==0x9403||f.tag==0x9405;
        if(f.type!=(signed?10:5))return result("","");
        double[] a=fractions(r,f,value,1);if(a==null)return result("Frazione con denominatore zero: il valore fisico non è calcolabile.","");
        double x=a[0];String unit="";
        switch(f.tag){
            case 0x829a:unit=" s";break;
            case 0x829d:return result("f/"+number(x),"");
            case 0x9201:double seconds=Math.pow(2,-x);return result(number(x)+" APEX; tempo = 2^(-Tv) = "+number(seconds)+" s","");
            case 0x9202:case 0x9205:return result(number(x)+" APEX; apertura = 2^(Av/2) = f/"+number(Math.pow(2,x/2)),"");
            case 0x9203:unit=" Bv (APEX)";break;
            case 0x9204:unit=" EV";break;
            case 0x9206:case 0x9403:unit=" m";break;
            case 0x920a:unit=" mm";break;
            case 0x9400:unit=" °C";break;
            case 0x9401:unit=" %";break;
            case 0x9402:unit=" hPa";break;
            case 0x9404:return result(number(x)+" mGal = "+number(x*0.00001)+" m/s²","");
            case 0x9405:unit="°";break;
            case 0xa404:return result(x==0?"0 → zoom digitale non utilizzato":number(x)+"× di zoom digitale","");
            default:return result("","");
        }
        return result(number(x)+unit,"");
    }
    private static Result gps(AnalysisReader r,AnalysisField f,long value)throws IOException{
        if(f.tag==0x1b||f.tag==0x1c)return encodedText(r,f,value);
        if(f.tag==0&&f.type==1&&f.count==4){
            byte[] v=r.bytes(value,4);return result((v[0]&255)+"."+(v[1]&255)+"."+(v[2]&255)+"."+(v[3]&255),"");
        }
        if(f.type!=5)return result("","");
        if((f.tag==2||f.tag==4||f.tag==20||f.tag==22)&&f.count==3){
            double[] a=fractions(r,f,value,3);if(a==null)return result("Coordinata non calcolabile: denominatore zero.","");
            double max=f.tag==2||f.tag==20?90:180;
            if(a[0]<0||a[0]>max||a[1]<0||a[1]>=60||a[2]<0||a[2]>=60||a[0]+a[1]/60+a[2]/3600>max)
                return result("Coordinate fuori intervallo; nessuna posizione viene dedotta.","");
            return result(number(a[0])+"° "+number(a[1])+"′ "+number(a[2])+"″ = "+number(a[0]+a[1]/60+a[2]/3600)+"° in valore assoluto. Segno da leggere nel relativo tag Ref","");
        }
        if(f.tag==7&&f.count==3){
            double[] a=fractions(r,f,value,3);if(a==null)return result("Ora non calcolabile: denominatore zero.","");
            return result(number(a[0])+" ore, "+number(a[1])+" minuti, "+number(a[2])+" secondi UTC","");
        }
        if(f.count==1&&(f.tag==6||f.tag==31||f.tag==15||f.tag==17||f.tag==24)){
            double[] a=fractions(r,f,value,1);if(a==null)return result("Frazione con denominatore zero: valore non calcolabile.","");
            return result(number(a[0])+(f.tag==6?" m; segno definito da GPSAltitudeRef":f.tag==31?" m":"°; riferimento nel relativo tag Ref"),"");
        }
        return result("","");
    }
    private static Result version(AnalysisReader r,AnalysisField f,long value)throws IOException{
        if(f.type!=7||f.count!=4)return result("Versione non interpretata: sono attesi quattro byte UNDEFINED.","");
        String text=new String(r.bytes(value,4),StandardCharsets.US_ASCII);
        if(!text.matches("[0-9]{4}"))return result("Codice versione non riconosciuto: i quattro byte non sono tutte cifre ASCII.","");
        return result(text+" → versione "+Integer.parseInt(text.substring(0,2))+"."+text.substring(2),"");
    }
    private static Result encodedText(AnalysisReader r,AnalysisField f,long value)throws IOException{
        if(f.type!=7||f.bytes<8)return result("Testo non decodificato: è richiesto un prefisso di codifica di 8 byte nel formato UNDEFINED.","");
        byte[] prefix=r.bytes(value,8);
        if(!Arrays.equals(prefix,new byte[]{65,83,67,73,73,0,0,0}))
            return result("Prefisso di codifica presente; testo non decodificato per questa codifica. I byte restano consultabili.","");
        int n=(int)Math.min(f.bytes-8,128);byte[] text=r.bytes(value+8,n);StringBuilder out=new StringBuilder();
        for(byte b:text){int c=b&255;if(c==0)out.append("\\0");else if(c>=32&&c<127)out.append((char)c);else out.append(String.format(Locale.ROOT,"\\x%02X",c));}
        return result("testo ASCII: \""+out+"\""+(f.bytes-8>n?" (anteprima parziale)":""),"");
    }
    private static Result flash(long v){
        if((v&~127L)!=0)return result(v+" → contiene bit riservati; interpretazione completa non disponibile","");
        int mode=(int)((v>>3)&3),ret=(int)((v>>1)&3);
        String text=v+" → "+((v&1)!=0?"flash scattato":"flash non scattato")+
            "; modalità "+new String[]{"non specificata","scatto obbligatorio","soppressione obbligatoria","automatica"}[mode]+
            "; ritorno "+new String[]{"non rilevato dal sistema","codice riservato","non rilevato","rilevato"}[ret]+
            "; funzione flash "+((v&32)!=0?"assente":"presente")+
            "; riduzione occhi rossi "+((v&64)!=0?"attiva":"non attiva");
        return result(text,"SIGNIFICATO DEI BIT DEL FLASH\nBit 0: 1 = scattato, 0 = non scattato\nBit 1–2: 0 = nessun rilevamento del ritorno; 1 = riservato; 2 = ritorno non rilevato; 3 = rilevato\nBit 3–4: 0 = modalità non specificata; 1 = obbligatorio; 2 = soppresso; 3 = automatico\nBit 5: 1 = funzione flash assente; 0 = presente\nBit 6: 1 = riduzione occhi rossi attiva; 0 = non attiva");
    }
    private static double[] fractions(AnalysisReader r,AnalysisField f,long value,int count)throws IOException{
        double[] out=new double[count];
        for(int i=0;i<count;i++){long p=value+i*8L,a=r.uint(p,4,f.little),b=r.uint(p+4,4,f.little);if(f.type==10){a=(int)a;b=(int)b;}if(b==0)return null;out[i]=(double)a/b;}
        return out;
    }
    private static String choices(ExifTagDictionary.Entry e,String actual){
        StringBuilder out=new StringBuilder("VALORI DOCUMENTATI PER ").append(e.technical).append('\n');
        for(Map.Entry<String,String> c:e.choices.entrySet())out.append(c.getKey().equals(actual)?"→ ":"").append(c.getKey()).append(" = ").append(c.getValue()).append(c.getKey().equals(actual)?"  [PRESENTE NEL FILE]":"").append('\n');
        return out.append("Un codice assente da questa tabella resta non interpretato.").toString();
    }
    private static String number(double value){return Double.isFinite(value)?String.format(Locale.ROOT,"%.8g",value):"fuori intervallo numerico";}
    private static Result result(String value,String alternatives){return new Result(value,alternatives);}
    private ExifValueInterpreter(){}
}
