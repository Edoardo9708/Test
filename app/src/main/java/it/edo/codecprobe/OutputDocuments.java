package it.edo.codecprobe;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** SAF directory reads shared by preflight and the last no-overwrite check. */
final class OutputDocuments {
    static String id(Context context, Uri folder) {
        return DocumentsContract.isDocumentUri(context,folder)
            ? DocumentsContract.getDocumentId(folder) : DocumentsContract.getTreeDocumentId(folder);
    }
    static String key(Context context, Uri folder) {
        return folder.getAuthority()+":"+id(context,folder);
    }
    static Set<String> names(Context context, Uri folder) throws IOException {
        Set<String> result=new HashSet<>();
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(folder,id(context,folder));
        try(Cursor cursor=context.getContentResolver().query(children,
                new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)) {
            if(cursor==null)throw new IOException("Impossibile leggere i nomi nella cartella di destinazione");
            while(cursor.moveToNext()) {
                if(Thread.currentThread().isInterrupted())throw new IOException("Controllo nomi interrotto");
                if(!cursor.isNull(0))result.add(cursor.getString(0).toLowerCase(Locale.ROOT));
            }
        } catch(RuntimeException error) { throw new IOException("Controllo cartella non riuscito: "+error.getMessage(),error); }
        return result;
    }
    static void requireFree(Context context, Uri folder, String name) throws IOException {
        if(names(context,folder).contains(name.toLowerCase(Locale.ROOT)))
            throw new IOException("Il nome scelto è già presente: "+name+". Nessun file sovrascritto; scegli un altro nome e riprova.");
    }
    private OutputDocuments() {}
}
