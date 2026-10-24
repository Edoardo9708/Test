package it.edo.codecprobe;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

final class ImageFiles {
    static String name(Context context,Uri uri){try(Cursor rows=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(rows!=null&&rows.moveToFirst()&&!rows.isNull(0))return rows.getString(0);}catch(RuntimeException ignored){}return basename(uri.getLastPathSegment());}
    static String basename(String value){
        if(value==null||value.isEmpty())return "Foto JPEG";
        int split=Math.max(value.lastIndexOf('/'),value.lastIndexOf('\\'));
        String name=value.substring(split+1);return name.isEmpty()?"Foto JPEG":name;
    }
    private ImageFiles(){}
}
