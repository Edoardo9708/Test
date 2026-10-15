package it.edo.codecprobe;

import android.content.Context;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

/** Resolves local source parents without deriving filesystem paths from display names. */
final class SourceFolder {
    static final String EXTERNAL="com.android.externalstorage.documents";
    static String parentId(Context c,Uri input){
        try{
            if(EXTERNAL.equals(input.getAuthority())){
                String id=DocumentsContract.getDocumentId(input);int slash=id.lastIndexOf('/');
                return slash>=0?id.substring(0,slash):id.substring(0,id.indexOf(':')+1);
            }
            Uri media="media".equals(input.getAuthority())?input:MediaStore.getMediaUri(c,input);
            if(media!=null)try(Cursor row=c.getContentResolver().query(media,new String[]{"relative_path","volume_name"},null,null,null)){
                if(row!=null&&row.moveToFirst()&&!row.isNull(0)&&!row.isNull(1)){
                    String path=row.getString(0),volume=row.getString(1);
                    if("external".equals(volume))return null;
                    if("external_primary".equals(volume))volume="primary";
                    while(path.endsWith("/"))path=path.substring(0,path.length()-1);
                    return volume+":"+path;
                }
            }
        }catch(Exception ignored){}return null;
    }
    static Uri granted(Context c,String parent){
        if(parent==null)return null;
        for(UriPermission permission:c.getContentResolver().getPersistedUriPermissions()){
            Uri tree=permission.getUri();if(!permission.isWritePermission()||!EXTERNAL.equals(tree.getAuthority())||!DocumentsContract.isTreeUri(tree))continue;
            try{String root=DocumentsContract.getTreeDocumentId(tree);
                if(parent.equalsIgnoreCase(root)||parent.regionMatches(true,0,root+(root.endsWith(":")?"":"/"),0,root.length()+(root.endsWith(":")?0:1)))
                    return DocumentsContract.buildDocumentUriUsingTree(tree,parent);
            }catch(Exception ignored){}
        }return null;
    }
    static boolean matches(String expected,Uri chosen){
        if(expected==null)return true;
        try{return EXTERNAL.equals(chosen.getAuthority())&&expected.equalsIgnoreCase(DocumentsContract.getTreeDocumentId(chosen));}
        catch(Exception e){return false;}
    }
}
