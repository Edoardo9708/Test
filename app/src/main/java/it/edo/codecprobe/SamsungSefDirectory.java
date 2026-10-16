package it.edo.codecprobe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Bounds and directory references only. Does not approve payload relocation. */
final class SamsungSefDirectory {
    static final int MAX_RECORDS=4096;
    static final class Record {
        final byte[] bytes;
        final int number,type,start,end,payload;
        final String name;
        Record(byte[] b,int n,int t,int s,int e,int p,String name){bytes=b;number=n;type=t;start=s;end=e;payload=p;this.name=name;}
        int payloadBytes(){return end-payload;}
    }
    final int version,start;
    final List<Record> records;
    private SamsungSefDirectory(int version,int start,List<Record> records){this.version=version;this.start=start;this.records=records;}
    static IOException bad(String detail){return new IOException(detail);}
    static long u32(byte[] b,int p){return (b[p]&255L)|((b[p+1]&255L)<<8)|((b[p+2]&255L)<<16)|((b[p+3]&255L)<<24);}
    private static int u16(byte[] b,int p){return (b[p]&255)|((b[p+1]&255)<<8);}
    static boolean signature(byte[] b,int p,String s){return p>=0&&p<=b.length-s.length()&&new String(b,p,s.length(),StandardCharsets.US_ASCII).equals(s);}

    static SamsungSefDirectory parse(byte[] b)throws IOException {
        if(b==null||b.length<40||b.length>SamsungSef.MAX_BYTES)throw bad("Dimensione SEF non supportata (40 byte–4 MiB)");
        long box=((b[0]&255L)<<24)|((b[1]&255L)<<16)|((b[2]&255L)<<8)|(b[3]&255L);
        if(box!=b.length||!signature(b,4,"sefd"))throw bad("Header sefd o dimensione del blocco non validi");
        if(!signature(b,b.length-4,"SEFT"))throw bad("Firma finale SEFT mancante");
        long tail=u32(b,b.length-8);
        if(tail<12||tail>b.length-16||(tail-12)%12!=0)throw bad("Dimensione directory SEFT non valida: "+tail);
        int dir=b.length-8-(int)tail;
        if(!signature(b,dir,"SEFH"))throw bad("Firma directory SEFH mancante a offset relativo "+dir);
        long version=u32(b,dir+4),count=u32(b,dir+8);
        if(version!=103&&version!=105&&version!=106&&version!=107)throw bad("Versione directory non verificata: "+version+" (riconosciute: 103, 105, 106, 107)");
        if(count==0||count!=(tail-12)/12||count>MAX_RECORDS)throw bad("Conteggio record incoerente o superiore al limite "+MAX_RECORDS+": "+count);
        List<Record> records=new ArrayList<>();int expected=8;
        for(int i=0;i<count;i++){
            int entry=dir+12+i*12,type=u16(b,entry+2);long start=dir-u32(b,entry+4),size=u32(b,entry+8);
            String context="Record "+(i+1)+" · tipo="+SamsungSefDiagnostics.type(type)+" · entry="+entry+" · inizio="+start+" · byte="+size;
            if(start!=expected||size<9||start<8||size>dir-start)throw bad(context+"\nRiferimento esterno, record sovrapposti, ordine non verificato o dati non indicizzati");
            int at=(int)start,end=(int)(start+size);long names=u32(b,at+4);
            if(u16(b,entry)!=0||u16(b,at)!=0||type!=u16(b,at+2)||names<1||names>256||names>size-8)
                throw bad(context+"\nHeader record non valido: nome_byte="+names+", tipo directory e tipo payload devono coincidere");
            String name=new String(b,at+8,(int)names,StandardCharsets.US_ASCII);
            for(int n=at+8;n<at+8+names;n++)if(b[n]<32||b[n]>126)
                throw bad(context+"\nNome record non ASCII leggibile: "+SamsungSefDiagnostics.hex(b,at+8,(int)names,64));
            records.add(new Record(b,i+1,type,at,end,at+8+(int)names,name));expected=end;
        }
        if(expected!=dir)throw bad("Byte non indicizzati prima della directory: "+(dir-expected));
        return new SamsungSefDirectory((int)version,dir,records);
    }
    private SamsungSefDirectory(){throw new AssertionError();}
}
