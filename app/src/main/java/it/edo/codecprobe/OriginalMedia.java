package it.edo.codecprobe;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import java.io.File;
import java.io.IOException;

/** Resolves the user's SAF grant to the same media item; never bypasses permissions. */
final class OriginalMedia {
    private OriginalMedia() {}
    static boolean hasLocationPermission(Context c) {
        return c.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    static String[] videoPermissions() {
        if(Build.VERSION.SDK_INT>=34)return new String[]{Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED};
        return new String[]{Build.VERSION.SDK_INT>=33?Manifest.permission.READ_MEDIA_VIDEO:Manifest.permission.READ_EXTERNAL_STORAGE};
    }
    static boolean hasVideoPermission(Context c) {
        for(String p:videoPermissions())if(c.checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED)return true;
        return false;
    }
    static Uri resolve(Context c,Uri selected)throws IOException {
        if(!hasLocationPermission(c))throw new IOException("Permesso posizione dei file multimediali assente: impossibile garantire la copia GPS. Abilitalo e riprova.");
        Uri media=selected;
        String authority=selected.getAuthority();
        if(DocumentsContract.isDocumentUri(c,selected)&&
            ("com.android.providers.media.documents".equals(authority)||"com.android.externalstorage.documents".equals(authority))) {
            media=MediaStore.getMediaUri(c,selected);
            if(media==null)throw new IOException("Il file locale non è indicizzato in MediaStore: selezionalo da Video oppure DCIM e riprova. Apertura senza oscuramento GPS non garantita.");
        }
        // Remote/other document providers have no equivalent MediaStore URI.
        // Their metadata is checked, but Android cannot guarantee unredacted source bytes.
        Uri original=MediaStore.AUTHORITY.equals(media.getAuthority())?MediaStore.setRequireOriginal(media):media;
        // A SAF grant can cover the undecorated URI yet fail for requireOriginal.
        // Validate the actual URI now, before expensive encoding or a success message.
        try(ParcelFileDescriptor fd=c.getContentResolver().openFileDescriptor(original,"r")){
            if(fd==null)throw new IOException("File originale non disponibile");
        }catch(SecurityException e){
            throw new IOException("Android nega la lettura del video originale. Nelle autorizzazioni dell’app consenti Video oppure seleziona anche questo video tra quelli consentiti. Il permesso posizione da solo non concede la lettura del file.",e);
        }
        return original;
    }
    static boolean requiresOriginal(Uri uri) {
        return MediaStore.AUTHORITY.equals(uri.getAuthority())&&
            (Build.VERSION.SDK_INT>=30?MediaStore.getRequireOriginal(uri):"1".equals(uri.getQueryParameter("requireOriginal")));
    }
    static String location(Context c,Uri uri)throws Exception {
        try(ParcelFileDescriptor fd=c.getContentResolver().openFileDescriptor(uri,"r")) {
            if(fd==null)throw new IOException("File non disponibile durante la lettura GPS");
            MediaMetadataRetriever r=new MediaMetadataRetriever();
            try{r.setDataSource(fd.getFileDescriptor());return r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);}
            finally{r.release();}
        }
    }
    static String location(File file)throws Exception {
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        try{r.setDataSource(file.getAbsolutePath());return r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);}
        finally{r.release();}
    }
    static String display(String gps) {
        return gps==null?"NON RILEVATA dal lettore Android · non prova che ogni campo GPS sia assente":gps+" · coordinate ISO 6709 (latitudine, longitudine; eventuale quota)";
    }
}
