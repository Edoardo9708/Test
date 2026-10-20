package it.edo.codecprobe;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/** Small positional reads. Large payloads are represented by ranges and skipped. */
final class AnalysisReader {
    final FileChannel channel;
    final long size;
    AnalysisReader(FileChannel channel)throws IOException{this.channel=channel;size=channel.size();channel.position(0);}
    void check()throws IOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Analisi annullata");}
    byte[] bytes(long offset,int count)throws IOException{
        check();if(count<0||count>65536||offset<0||offset>size||count>size-offset)throw new IOException("Dati troncati a "+offset);
        ByteBuffer b=ByteBuffer.allocate(count);while(b.hasRemaining()){check();int n=channel.read(b,offset+b.position());if(n<=0)throw new EOFException("Lettura incompleta a "+offset);}return b.array();
    }
    int u8(long p)throws IOException{return bytes(p,1)[0]&255;}
    int u16(long p)throws IOException{byte[] b=bytes(p,2);return (b[0]&255)*256+(b[1]&255);}
    long uint(long p,int n,boolean little)throws IOException{byte[] b=bytes(p,n);long v=0;for(int i=0;i<n;i++)v=(v<<8)|(b[little?n-1-i:i]&255L);if(v<0)throw new IOException("Dimensione oltre 63 bit");return v;}
    String ascii(long p,int n)throws IOException{return new String(bytes(p,n),StandardCharsets.ISO_8859_1);}
    void within(long p,long n,long end)throws IOException{if(p<0||n<0||p>end||n>end-p)throw new IOException("Struttura troncata a "+p);}
}
