package it.edo.codecprobe;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Pure Java regression cases, also executable locally without an Android runtime. */
public final class FileAnalysisChecks {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static byte[] box(String type,byte[] payload)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(bytes);d.writeInt(payload.length+8);d.writeBytes(type);d.write(payload);return bytes.toByteArray();}
    static byte[] concat(byte[]... parts)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();for(byte[] b:parts)out.write(b);return out.toByteArray();}
    static void rejects(byte[] bytes)throws Exception{Path p=Files.createTempFile("bad-analysis-",".bin");try{Files.write(p,bytes);try(RandomAccessFile file=new RandomAccessFile(p.toFile(),"r")){AnalysisReader r=new AnalysisReader(file.getChannel());try{AnalysisParser.children(r,AnalysisParser.root(r,"test"));throw new AssertionError("Malformed structure accepted");}catch(IOException expected){}}}finally{Files.delete(p);}}
    static void coverage(List<AnalysisNode> nodes,long size){long p=0;for(AnalysisNode n:nodes){check(n.offset==p,"Physical ranges contiguous");p=n.end();}check(p==size,"All bytes accounted for");}
    public static void run()throws Exception{
        Path p=Files.createTempFile("file-analysis-",".bin");
        try{
            byte[] jpeg={-1,-40,-1,-31,0,8,69,120,105,102,0,0,-1,-38,0,2,42,-1,0,9,-1,-48,8,-1,-39,7};Files.write(p,jpeg);
            try(RandomAccessFile f=new RandomAccessFile(p.toFile(),"r")){AnalysisReader r=new AnalysisReader(f.getChannel());AnalysisNode root=AnalysisParser.root(r,"JPEG");List<AnalysisNode> nodes=AnalysisParser.children(r,root);coverage(nodes,jpeg.length);check(nodes.get(1).parser.equals("exif"),"EXIF signature");check(nodes.get(3).size==7,"Stuffing/restart remain in scan");check(nodes.get(5).size==1,"Trailing data represented");check(AnalysisParser.hex(r,nodes.get(3),0).contains("2A FF 00 09 FF D0 08"),"HEX reads selected range");}
            byte[] data=concat(box("ftyp",new byte[]{'h','e','i','c',0,0,0,0}),box("meta",concat(new byte[4],box("iinf",new byte[6]))),box("mdat",new byte[100]));Files.write(p,data);
            try(RandomAccessFile f=new RandomAccessFile(p.toFile(),"r")){AnalysisReader r=new AnalysisReader(f.getChannel());List<AnalysisNode> nodes=AnalysisParser.children(r,AnalysisParser.root(r,"HEIC"));coverage(nodes,data.length);check(AnalysisParser.children(r,nodes.get(1)).get(0).title.equals("iinf"),"FullBox prefix skipped");check(!nodes.get(2).expandable(),"mdat is opaque, never parsed as boxes");}
            // 64-bit sparse payload: parsing must seek over 5 GiB, never read/allocate it.
            try(RandomAccessFile f=new RandomAccessFile(p.toFile(),"rw")){f.setLength(0);f.write(box("ftyp",new byte[8]));f.writeInt(1);f.writeBytes("mdat");f.writeLong(5L*1024*1024*1024);f.setLength(16+5L*1024*1024*1024);AnalysisReader r=new AnalysisReader(f.getChannel());List<AnalysisNode> nodes=AnalysisParser.children(r,AnalysisParser.root(r,"large"));check(nodes.size()==2&&nodes.get(1).size==5L*1024*1024*1024,"64-bit payload skipped");}
            rejects(concat(box("ftyp",new byte[8]),new byte[]{0,0,0,7,'m','d','a','t'}));rejects(new byte[]{-1,-40,-1,-31,0,16,0});rejects(Arrays.copyOf(jpeg,jpeg.length-2));
            Files.write(p,new byte[]{'f','L','a','C',-128,0,0,2,1,2,42});
            try(RandomAccessFile f=new RandomAccessFile(p.toFile(),"r")){AnalysisReader r=new AnalysisReader(f.getChannel());coverage(AnalysisParser.children(r,AnalysisParser.root(r,"FLAC")),r.size);}
            Files.write(p,new byte[]{'I','D','3',4,0,0,0,0,0,2,1,2,42});
            try(RandomAccessFile f=new RandomAccessFile(p.toFile(),"r")){AnalysisReader r=new AnalysisReader(f.getChannel());coverage(AnalysisParser.children(r,AnalysisParser.root(r,"MP3")),r.size);Thread.currentThread().interrupt();try{r.bytes(0,1);throw new AssertionError("Cancellation ignored");}catch(InterruptedIOException expected){}finally{Thread.interrupted();}}
            // EXIF IFD0 little endian: orientation stored inline, no TIFF payload copied.
            byte[] tiff={73,73,42,0,8,0,0,0,1,0,18,1,3,0,1,0,0,0,6,0,0,0,0,0,0,0};Files.write(p,tiff);
            try(RandomAccessFile f=new RandomAccessFile(p.toFile(),"r")){AnalysisReader r=new AnalysisReader(f.getChannel());List<AnalysisNode> nodes=AnalysisParser.children(r,new AnalysisNode("EXIF","",0,tiff.length,"exif",0));check(nodes.get(2).description.contains("Valore: 6"),"Orientation interpreted");}
            byte[] thumb=new byte[42];java.nio.ByteBuffer tb=java.nio.ByteBuffer.wrap(thumb).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            tb.put((byte)'I').put((byte)'I').putShort((short)42).putInt(8).putShort((short)2);
            tb.putShort((short)0x201).putShort((short)4).putInt(1).putInt(38);
            tb.putShort((short)0x202).putShort((short)4).putInt(1).putInt(4).putInt(0);tb.put(new byte[]{-1,-40,-1,-39});Files.write(p,thumb);
            try(RandomAccessFile f=new RandomAccessFile(p.toFile(),"r")){AnalysisReader r=new AnalysisReader(f.getChannel());List<AnalysisNode> nodes=AnalysisParser.children(r,new AnalysisNode("EXIF","",0,thumb.length,"exif",0));AnalysisNode miniature=nodes.stream().filter(n->n.title.startsWith("Miniatura")).findFirst().orElseThrow();check(miniature.offset==38&&miniature.size==4,"Thumbnail exact range");check(AnalysisParser.hex(r,miniature,0).contains("FF D8 FF D9"),"Thumbnail payload HEX");}
            // Cycle through next IFD must terminate with an error.
            tiff[tiff.length-4]=8;Files.write(p,tiff);try(RandomAccessFile f=new RandomAccessFile(p.toFile(),"r")){AnalysisReader r=new AnalysisReader(f.getChannel());try{AnalysisParser.children(r,new AnalysisNode("EXIF","",0,tiff.length,"exif",0));throw new AssertionError("Cycle accepted");}catch(IOException expected){}}
        }finally{Files.deleteIfExists(p);}
    }
    public static void main(String[] args)throws Exception{run();for(String name:args){try(RandomAccessFile f=new RandomAccessFile(name,"r")){AnalysisReader r=new AnalysisReader(f.getChannel());AnalysisNode root=AnalysisParser.root(r,name);List<AnalysisNode> nodes=AnalysisParser.children(r,root);coverage(nodes,r.size);for(AnalysisNode n:nodes)if(n.expandable())AnalysisParser.children(r,n);System.out.println(root.description+": "+r.size+" bytes, "+nodes.size()+" blocks");}}System.out.println("OK analysis: ranges, JPEG scans, nested boxes, EXIF, audio, sparse 5 GiB, truncation and cancellation");}
}
