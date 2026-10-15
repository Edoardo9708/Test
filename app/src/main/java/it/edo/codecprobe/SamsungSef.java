package it.edo.codecprobe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Relocatable Samsung SEF directory with verified text highlight and screen-capture schemas.
 * Directory offsets are backwards from SEFH (Android SefReader); arbitrary SEF payloads
 * can contain other references and must not be accepted based on that fact alone. */
final class SamsungSef {
    static final int MAX_BYTES=4*1024*1024;
    private static IOException bad(String detail){return new IOException("Metadati Samsung SEF: "+detail);}
    private static long u32(byte[] b,int p){return Integer.toUnsignedLong(ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt(p));}
    private static int u16(byte[] b,int p){return (b[p]&255)|((b[p+1]&255)<<8);}
    private static boolean signature(byte[] b,int p,String s){return p>=0&&p+s.length()<=b.length&&new String(b,p,s.length(),StandardCharsets.US_ASCII).equals(s);}
    static String validate(byte[] b)throws IOException {
        if(b.length<40||b.length>MAX_BYTES||Integer.toUnsignedLong(ByteBuffer.wrap(b).getInt())!=b.length||!signature(b,4,"sefd"))throw bad("header/dimensione non supportati");
        if(!signature(b,b.length-4,"SEFT"))throw bad("firma finale SEFT mancante");
        long tail=u32(b,b.length-8);
        if(tail<12||tail>b.length-16||(tail-12)%12!=0)throw bad("directory incompleta");
        int dir=b.length-8-(int)tail;
        if(!signature(b,dir,"SEFH"))throw bad("firma directory SEFH non riconosciuta");
        long version=u32(b,dir+4);
        if(version!=105&&version!=107)throw bad("versione directory non supportata: "+version+" (supportate: 105, 107)");
        long count=u32(b,dir+8);
        if(count==0||count!=(tail-12)/12)throw bad("conteggio record incoerente");
        int expected=8,totalRows=0,captures=0,highlights=0;
        for(int i=0;i<count;i++){
            int entry=dir+12+i*12;long start=dir-u32(b,entry+4),size=u32(b,entry+8);
            if(start!=expected||size<9||start<8||size>dir-start)throw bad("riferimento esterno, record sovrapposti o dati non indicizzati");
            int at=(int)start,end=(int)(start+size);long names=u32(b,at+4);
            int type=u16(b,entry+2);
            if(u16(b,entry)!=0||u16(b,at)!=0||type!=u16(b,at+2)||names<1||names>256||names>size-8)throw bad("header record/nome non valido");
            if(type==0x08b0&&names==19&&signature(b,at+8,"HighlightVideo_Data")){
                totalRows+=highlight(b,at+27,end);highlights++;
            }else if(type==0x0c51&&names==20&&signature(b,at+8,"Samsung_Capture_Info")){
                // Verified screen-recorder schema: descriptive text, no offsets or timing fields.
                // Directory back-offsets are validated above; preserve the entire box unchanged.
                if(end-(at+28)!=16||!signature(b,at+28,"Screen recording"))throw bad("Samsung_Capture_Info: contenuto non ancora verificato");
                captures++;
            }else throw bad("tipo record non ancora verificato: 0x"+Integer.toHexString(type)+" (non viene copiato alla cieca)");
            expected=end;
        }
        if(expected!=dir)throw bad("dati fuori dalla directory");
        return "SEF versione="+version+" · record="+count+" · HighlightVideo_Data="+highlights+" · righe="+totalRows+" · Samsung_Capture_Info="+captures+(captures>0?" (Screen recording)":"")+" · riferimenti interni verificati";
    }
    private static int highlight(byte[] b,int start,int end)throws IOException {
        for(int p=start;p<end;p++){int c=b[p]&255;if(c!=10&&c!=13&&c!=9&&(c<32||c>126))throw bad("HighlightVideo_Data contiene dati non testuali");}
        String[] t=new String(b,start,end-start,StandardCharsets.US_ASCII).trim().split("\\s+");
        // Observed AutoSum v10: text header followed by fixed eight-number rows.
        if(t.length<10||!t[0].equals("AutoSum")||!t[1].equals("10")||!t[2].equals("TMDI")||!t[3].equals("1")||!t[4].equals("21")||!t[6].equals("SMDI")||!t[8].equals("81"))throw bad("schema HighlightVideo_Data non riconosciuto");
        try{
            nonnegative(t[5]);long rows=nonnegative(t[7]);
            if(rows<1||rows>50000||t.length!=9+rows*8)throw bad("righe HighlightVideo_Data incomplete");
            for(int i=0;i<rows;i++){
                int k=9+i*8;long[] v=new long[8];for(int j=0;j<8;j++)v[j]=nonnegative(t[k+j]);
                if(v[0]!=i||v[2]<v[1]||v[2]-v[1]!=v[3]||v[4]!=0||v[5]!=0||v[7]!=0)throw bad("schema della riga "+i+" non verificato");
            }
            return (int)rows;
        }catch(NumberFormatException e){throw bad("numero HighlightVideo_Data non valido");}
    }
    private static long nonnegative(String s){if(!s.matches("[0-9]{1,19}"))throw new NumberFormatException();return Long.parseLong(s);}
}

