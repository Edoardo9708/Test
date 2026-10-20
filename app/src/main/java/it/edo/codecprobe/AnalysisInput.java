package it.edo.codecprobe;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;

/** Owns a seekable provider descriptor for one worker operation; never copies a whole file. */
final class AnalysisInput implements AutoCloseable {
    private final ParcelFileDescriptor descriptor;
    private final FileInputStream stream;
    final AnalysisReader reader;
    AnalysisInput(Context context,AnalysisSources.Entry source)throws IOException{
        Uri uri=Uri.parse(source.uri);
        if(source.workspace.equals("Video")&&OriginalMedia.hasLocationPermission(context))uri=OriginalMedia.resolve(context,uri);
        descriptor=context.getContentResolver().openFileDescriptor(uri,"r");
        if(descriptor==null)throw new IOException("File non disponibile");
        FileInputStream opened=new ParcelFileDescriptor.AutoCloseInputStream(descriptor);
        try{reader=new AnalysisReader(opened.getChannel());stream=opened;}
        catch(IOException|RuntimeException e){opened.close();throw new IOException("Il provider non consente la lettura a posizioni. Usa una copia locale del file.",e);}
    }
    @Override public void close()throws IOException{stream.close();}
}
