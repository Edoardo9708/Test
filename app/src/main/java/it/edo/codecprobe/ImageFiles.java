package it.edo.codecprobe;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

final class ImageFiles {
    static String name(Context context,Uri uri){try(Cursor rows=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(rows!=null&&rows.moveToFirst()&&!rows.isNull(0))return rows.getString(0);}catch(RuntimeException ignored){}String last=uri.getLastPathSegment();return last==null?"Foto JPEG":last;}
    private ImageFiles(){}
}
