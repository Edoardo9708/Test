package it.edo.codecprobe;

import java.io.IOException;
import java.nio.ByteBuffer;

/** Copies complete AAC packets without interpreting a negative timestamp as end of input. */
final class AudioPacketCopier {
    static final int MAX_PACKET_BYTES=16*1024*1024;
    interface Source {
        int trackIndex();
        long size();
        long timeUs();
        boolean encrypted();
        int read(ByteBuffer buffer);
        void advance();
    }
    interface Sink { void write(ByteBuffer buffer,int size,long timeUs)throws Exception; }
    interface Check { void run()throws Exception; }
    interface Log { void write(String message); }

    static void copy(Source source,int track,Sink sink,Check check,Log log)throws Exception {
        ByteBuffer buffer=ByteBuffer.allocateDirect(1024*1024);
        int count=0,negative=0;long first=0,last=0,bytes=0;
        log.write("CONTROLLO INIZIO AUDIO · indice_traccia="+source.trackIndex()+
            " · primo_pts_us="+source.timeUs()+" · dimensione_pacchetto="+source.size());
        while(source.trackIndex()>=0) {
            check.run();
            if(source.trackIndex()!=track)throw new IOException("Il lettore audio ha restituito una traccia diversa: "+source.trackIndex());
            long size=source.size(),pts=source.timeUs();
            if(size<=0||size>MAX_PACKET_BYTES)throw new IOException("Dimensione pacchetto AAC non valida: "+size+" · pts_us="+pts);
            if(pts==Long.MIN_VALUE)throw new IOException("Timestamp audio fuori intervallo: "+pts);
            if(count>0&&pts<=last)throw new IOException("Timestamp audio non crescente: precedente="+last+" attuale="+pts);
            if(source.encrypted())throw new IOException("Audio protetto non supportato");
            if(size>buffer.capacity())buffer=ByteBuffer.allocateDirect((int)size);
            buffer.clear();int read=source.read(buffer);
            if(read!=size)throw new IOException("Lettura AAC incompleta: attesi="+size+" letti="+read+" · pts_us="+pts);
            buffer.position(0);buffer.limit(read);
            if(count<12)log.write("DIAGNOSTICA PRIMI TIMESTAMP AUDIO · campione="+(count+1)+" · pts_us="+pts+
                " · negativo="+(pts<0)+" · byte="+read);
            try { sink.write(buffer,read,pts); }
            catch(Exception error) { throw new IOException("Scrittura pacchetto AAC non riuscita · campione="+(count+1)+
                " · pts_us="+pts+" · nessun timestamp modificato: "+error,error); }
            if(count==0)first=pts;
            count++;if(pts<0)negative++;last=pts;bytes+=read;
            log.write("OK audio sample="+count+" pts_us="+pts+" bytes="+read);
            source.advance();
        }
        if(count==0)throw new IOException("Traccia audio realmente senza pacchetti: indice="+source.trackIndex()+
            " · pts_us="+source.timeUs()+" · dimensione="+source.size());
        log.write("OK copia audio samples="+count+" negative_pts="+negative+" first_pts_us="+first+" last_pts_us="+last+" bytes="+bytes+
            "\nTutti i pacchetti copiati senza tagli; timestamp originali conservati. Segue verifica di contenuto e sincronizzazione.");
    }
    private AudioPacketCopier() {}
}
