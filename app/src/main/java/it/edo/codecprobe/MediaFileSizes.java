package it.edo.codecprobe;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

final class MediaFileSizes {
    static long read(Context context,Uri uri){
        try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(uri,"r")){
            if(fd!=null&&fd.getStatSize()>0)return fd.getStatSize();
        }catch(Exception ignored){}
        try(Cursor row=context.getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){
            if(row!=null&&row.moveToFirst()&&!row.isNull(0)&&row.getLong(0)>0)return row.getLong(0);
        }catch(Exception ignored){}
        return -1;
    }
    private MediaFileSizes(){}
}
