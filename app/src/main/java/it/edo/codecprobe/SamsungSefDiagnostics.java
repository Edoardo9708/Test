package it.edo.codecprobe;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** Bounded diagnostic strings; never embeds the full SEF/media payload in a log. */
final class SamsungSefDiagnostics {
    static final int PREVIEW_BYTES=64,MAX_LISTED_RECORDS=64,MAX_DETAIL_RECORDS=4;
    static String type(int type){return String.format(Locale.ROOT,"0x%04X (%d)",type,type);}
    static String hex(byte[] b,int at,int length,int limit){
        StringBuilder s=new StringBuilder();int count=Math.min(length,limit);
        for(int i=0;i<count;i++){if(i>0)s.append(' ');s.append(String.format(Locale.ROOT,"%02X",b[at+i]&255));}
        if(length>count)s.append(" … (+").append(length-count).append(" byte)");return s.toString();
    }
    static String sha(byte[] b,int at,int count){
        try{MessageDigest digest=MessageDigest.getInstance("SHA-256");digest.update(b,at,count);return hex(digest.digest(),0,32,32).replace(" ","");}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    static String ascii(byte[] b,int at,int length){
        StringBuilder s=new StringBuilder();int count=Math.min(length,PREVIEW_BYTES);
        for(int i=0;i<count;i++){int c=b[at+i]&255;if(c>=32&&c<=126)s.append((char)c);else s.append(String.format(Locale.ROOT,"\\x%02X",c));}
        if(length>count)s.append(" …");return s.toString();
    }
    static String format(SamsungSefDirectory.Record r){
        if(r.payloadBytes()==0)return "vuoto";
        if(r.payloadBytes()>=8&&hex(r.bytes,r.payload,8,8).equals("89 50 4E 47 0D 0A 1A 0A"))return "firma PNG (contenuto non validato)";
        if(r.payloadBytes()>=3&&(r.bytes[r.payload]&255)==255&&(r.bytes[r.payload+1]&255)==216&&(r.bytes[r.payload+2]&255)==255)return "firma JPEG (contenuto non validato)";
        int first=r.payload;while(first<r.end&&(r.bytes[first]==32||r.bytes[first]==10||r.bytes[first]==13||r.bytes[first]==9))first++;
        if(first<r.end&&(r.bytes[first]=='{'||r.bytes[first]=='['))return "possibile JSON (contenuto non validato)";
        int sampled=Math.min(r.payloadBytes(),256);boolean text=true;
        for(int i=0;i<sampled;i++){int c=r.bytes[r.payload+i]&255;if(c!=9&&c!=10&&c!=13&&(c<32||c>126)){text=false;break;}}
        return text?"campione ASCII testuale":"campione binario";
    }
    static String record(SamsungSefDirectory.Record r,long base,String reason){
        return "*** DETTAGLIO RECORD SEF "+r.number+" ***\nNome: "+r.name+"\nTipo: "+type(r.type)
            +"\nRecord: "+(r.end-r.start)+" byte · payload: "+r.payloadBytes()+" byte"
            +"\nOffset relativi al blocco sefd: record="+r.start+" · payload="+r.payload+" · fine="+r.end
            +(base>=0?"\nOffset assoluto nel file: record="+(base+r.start)+" · payload="+(base+r.payload):"")
            +"\nMotivo del blocco: "+reason+"\nFormato indicativo: "+format(r)
            +"\nSHA-256 payload: "+sha(r.bytes,r.payload,r.payloadBytes())
            +"\nPrimi "+Math.min(r.payloadBytes(),PREVIEW_BYTES)+" byte · HEX: "+hex(r.bytes,r.payload,r.payloadBytes(),PREVIEW_BYTES)
            +"\nStessi byte · ASCII (\\xNN = byte non stampabile): "+ascii(r.bytes,r.payload,r.payloadBytes())
            +(r.payloadBytes()>PREVIEW_BYTES?"\nUltimi 64 byte · HEX: "+hex(r.bytes,r.end-PREVIEW_BYTES,PREVIEW_BYTES,PREVIEW_BYTES)
                +"\nUltimi 64 byte · ASCII: "+ascii(r.bytes,r.end-PREVIEW_BYTES,PREVIEW_BYTES):"");
    }
    static String structural(byte[] b,long base,String reason){
        if(b==null)return "Metadati Samsung SEF: blocco assente\n"+reason;
        return "Metadati Samsung SEF: struttura non valida\nMotivo: "+reason+"\nBlocco: "+b.length+" byte · offset assoluto="+(base>=0?base:"non disponibile")
            +"\nPrimi byte HEX: "+hex(b,0,b.length,PREVIEW_BYTES)
            +"\nUltimi byte HEX: "+hex(b,Math.max(0,b.length-PREVIEW_BYTES),Math.min(b.length,PREVIEW_BYTES),PREVIEW_BYTES);
    }
    private SamsungSefDiagnostics(){}
}
