package it.edo.codecprobe;

import android.content.Context;
import java.io.*;
import java.util.*;

/** Private queue handoff: only a random identifier crosses Binder, never thousands of URIs. */
final class ImageQueueStore {
    static final int LIMIT=5000, VISIBLE=100;
    private static final int MAGIC=0x43504951;
    static final class Queue {
        final ArrayList<String> inputs=new ArrayList<>(),names=new ArrayList<>(),folders=new ArrayList<>();
    }
    static File file(Context context,String id)throws IOException{
        if(id==null||!id.matches("[0-9a-f-]{36}"))throw new IOException("Identificativo coda non valido");
        return new File(context.getFilesDir(),"image-queue-"+id+".bin");
    }
    static String write(Context context,List<String> inputs,List<String> names,List<String> folders)throws IOException{
        if(inputs.isEmpty()||inputs.size()>LIMIT||folders.size()!=inputs.size())throw new IOException("Coda immagini non valida");
        String id=UUID.randomUUID().toString();File file=file(context,id);boolean complete=false;
        try(FileOutputStream stream=new FileOutputStream(file);DataOutputStream out=new DataOutputStream(new BufferedOutputStream(stream))){
            out.writeInt(MAGIC);out.writeInt(inputs.size());
            for(int i=0;i<inputs.size();i++){
                if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
                String input=inputs.get(i),folder=folders.get(i);
                if(input==null||input.isEmpty()||folder==null||folder.isEmpty())throw new IOException("Sorgente o destinazione mancante");
                out.writeUTF(input);out.writeUTF(i<names.size()?names.get(i):"Foto "+(i+1));out.writeUTF(folder);
            }
            out.flush();if(stream.getChannel().size()>32L*1024*1024)throw new IOException("Coda immagini troppo grande");stream.getFD().sync();complete=true;
        }finally{if(!complete)file.delete();}
        return id;
    }
    static Queue read(Context context,String id)throws IOException{
        File file=file(context,id);if(file.length()>32L*1024*1024)throw new IOException("Coda immagini troppo grande");
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(file)))){
            if(in.readInt()!=MAGIC)throw new IOException("Formato coda immagini non valido");
            int count=in.readInt();if(count<1||count>LIMIT)throw new IOException("Numero immagini non valido");Queue queue=new Queue();
            for(int i=0;i<count;i++){
                if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
                String input=in.readUTF(),name=in.readUTF(),folder=in.readUTF();
                if(input.isEmpty()||folder.isEmpty())throw new IOException("Coda immagini incompleta");
                queue.inputs.add(input);queue.names.add(name);queue.folders.add(folder);
            }
            if(in.read()!=-1)throw new IOException("Dati inattesi nella coda immagini");return queue;
        }
    }
    static void discard(Context context,String id){try{file(context,id).delete();}catch(IOException ignored){}}
    private ImageQueueStore(){}
}
