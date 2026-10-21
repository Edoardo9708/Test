package it.edo.codecprobe;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.util.*;

/** Directory listing only, off the UI thread. Does not open JPEG data or recurse into subfolders. */
final class ImageFolderReader {
    static ImageQueueStore.Queue read(Context context,Uri tree,String destination)throws IOException{
        ImageQueueStore.Queue queue=new ImageQueueStore.Queue();HashSet<String> seen=new HashSet<>();
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
        try(Cursor rows=context.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
            if(rows==null)throw new IOException("Impossibile elencare la cartella");
            while(rows.moveToNext()){
                if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
                String id=rows.getString(0),name=rows.getString(1),mime=rows.getString(2);if(id==null||DocumentsContract.Document.MIME_TYPE_DIR.equals(mime))continue;
                if(name==null)name="Foto JPEG";String lower=name.toLowerCase(Locale.ROOT);
                if(!"image/jpeg".equals(mime)&&!lower.endsWith(".jpg")&&!lower.endsWith(".jpeg"))continue;
                String input=DocumentsContract.buildDocumentUriUsingTree(tree,id).toString();if(!seen.add(input))continue;
                if(queue.inputs.size()>=ImageQueueStore.BULK_LIMIT)throw new IOException("La cartella supera "+ImageQueueStore.BULK_LIMIT+" JPEG");
                queue.inputs.add(input);queue.names.add(name);queue.folders.add(destination);
            }
        }catch(RuntimeException error){throw new IOException("Lettura cartella non riuscita: "+error.getMessage(),error);}
        return queue;
    }
    private ImageFolderReader(){}
}
