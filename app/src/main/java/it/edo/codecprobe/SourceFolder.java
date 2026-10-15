package it.edo.codecprobe;

import android.content.Context;
import android.content.ContentUris;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;

/** Provider-aware parent resolution; never assumes equal file names mean equal files. */
final class SourceFolder {
    static final String EXTERNAL="com.android.externalstorage.documents";
    static Uri mediaUri(Context c,Uri input){
        if("media".equals(input.getAuthority()))return input;
        try{Uri converted=MediaStore.getMediaUri(c,input);if(converted!=null)return converted;}catch(Exception ignored){}
        if("com.android.providers.media.documents".equals(input.getAuthority()))try{
            String id=DocumentsContract.getDocumentId(input);
            if(id.startsWith("video:"))return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,Long.parseLong(id.substring(6)));
        }catch(Exception ignored){}
        return null;
    }
    static String parentId(Context c,Uri input){
        if(EXTERNAL.equals(input.getAuthority()))try{return FolderPaths.parent(DocumentsContract.getDocumentId(input));}catch(Exception ignored){}
        String result=queryParent(c,input);if(result!=null)return result;
        Uri media=mediaUri(c,input);return media==null?null:queryParent(c,media);
    }
    private static String queryParent(Context c,Uri uri){
        try(Cursor row=c.getContentResolver().query(uri,new String[]{"relative_path","volume_name"},null,null,null)){
            if(row!=null&&row.moveToFirst()){
                int pathColumn=row.getColumnIndexOrThrow("relative_path"),volumeColumn=row.getColumnIndexOrThrow("volume_name");
                String parent=FolderPaths.fromRelative(row.isNull(volumeColumn)?null:row.getString(volumeColumn),row.isNull(pathColumn)?null:row.getString(pathColumn));
                if(parent!=null)return parent;
            }
        }catch(Exception ignored){}
        // Metadata-only fallback. Opening files still uses SAF/MediaStore URIs.
        try(Cursor row=c.getContentResolver().query(uri,new String[]{"_data"},null,null,null)){
            if(row!=null&&row.moveToFirst()){
                int dataColumn=row.getColumnIndexOrThrow("_data");if(row.isNull(dataColumn))return null;
                String path=row.getString(dataColumn),primary=Environment.getExternalStorageDirectory().getAbsolutePath()+"/";
                if(path.startsWith(primary))return FolderPaths.parent("primary:"+path.substring(primary.length()));
                if(path.startsWith("/storage/")){String relative=path.substring(9);int slash=relative.indexOf('/');if(slash>0&&relative.substring(0,slash).matches("[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"))return FolderPaths.parent(relative.substring(0,slash)+":"+relative.substring(slash+1));}
            }
        }catch(Exception ignored){}return null;
    }
    static String documentId(Context c,Uri uri){return DocumentsContract.isDocumentUri(c,uri)?DocumentsContract.getDocumentId(uri):DocumentsContract.getTreeDocumentId(uri);}
    static Uri underTree(Uri tree,String parent){
        String root=DocumentsContract.getTreeDocumentId(tree);
        String id=root.substring(0,root.indexOf(':')+1)+parent.substring(parent.indexOf(':')+1);
        return DocumentsContract.buildDocumentUriUsingTree(tree,id);
    }
    static Uri granted(Context c,String parent){
        if(parent==null)return null;
        for(UriPermission permission:c.getContentResolver().getPersistedUriPermissions()){
            Uri tree=permission.getUri();if(!permission.isReadPermission()||!permission.isWritePermission()||!EXTERNAL.equals(tree.getAuthority())||!DocumentsContract.isTreeUri(tree))continue;
            try{if(FolderPaths.covers(DocumentsContract.getTreeDocumentId(tree),parent)){
                Uri document=underTree(tree,parent);if(writable(c,document))return document;
            }}catch(Exception ignored){}
        }return null;
    }
    static boolean writable(Context c,Uri folder){
        if(folder==null)return false;
        try{
            boolean granted=false;
            for(UriPermission p:c.getContentResolver().getPersistedUriPermissions())if(p.isReadPermission()&&p.isWritePermission()&&DocumentsContract.isTreeUri(p.getUri())&&p.getUri().getAuthority().equals(folder.getAuthority())&&DocumentsContract.getTreeDocumentId(p.getUri()).equals(DocumentsContract.getTreeDocumentId(folder))){granted=true;break;}
            if(!granted)return false;
            Uri document=DocumentsContract.buildDocumentUriUsingTree(folder,documentId(c,folder));
            try(Cursor row=c.getContentResolver().query(document,new String[]{DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_FLAGS},null,null,null)){
                return row!=null&&row.moveToFirst()&&DocumentsContract.Document.MIME_TYPE_DIR.equals(row.getString(0))&&(row.getInt(1)&DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE)!=0;
            }
        }catch(Exception ignored){return false;}
    }
    static boolean sameFile(Context c,Uri a,Uri b){
        if(a.equals(b))return true;
        if(EXTERNAL.equals(a.getAuthority())&&EXTERNAL.equals(b.getAuthority()))try{if(DocumentsContract.getDocumentId(a).equals(DocumentsContract.getDocumentId(b)))return true;}catch(Exception ignored){}
        Uri ma=mediaUri(c,a),mb=mediaUri(c,b);
        if(ma!=null&&mb!=null)try{
            java.util.List<String> pa=ma.getPathSegments(),pb=mb.getPathSegments();String va=pa.get(0),vb=pb.get(0);
            boolean collections=(pa.contains("video")||pa.contains("file"))&&(pb.contains("video")||pb.contains("file"));
            boolean sameVolume=va.equalsIgnoreCase(vb)||(va.equals("external")&&!vb.equals("internal"))||(vb.equals("external")&&!va.equals("internal"));
            if(collections&&sameVolume&&ContentUris.parseId(ma)>0&&ContentUris.parseId(ma)==ContentUris.parseId(mb))return true;
        }catch(Exception ignored){}
        // Some providers expose no MediaStore ID. A regular-file identity can still prove membership.
        try(ParcelFileDescriptor fa=c.getContentResolver().openFileDescriptor(a,"r");ParcelFileDescriptor fb=c.getContentResolver().openFileDescriptor(b,"r")){
            if(fa==null||fb==null)return false;
            StructStat sa=Os.fstat(fa.getFileDescriptor()),sb=Os.fstat(fb.getFileDescriptor());
            return OsConstants.S_ISREG(sa.st_mode)&&OsConstants.S_ISREG(sb.st_mode)&&sa.st_ino!=0&&sa.st_ino==sb.st_ino&&sa.st_dev==sb.st_dev;
        }catch(Exception ignored){return false;}
    }
}
