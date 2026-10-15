package it.edo.codecprobe;

/** Document IDs are volume:path pairs. Paths remain case sensitive. */
final class FolderPaths {
    static String parent(String documentId){
        if(documentId==null||documentId.indexOf(':')<1)return null;
        int slash=documentId.lastIndexOf('/');
        return slash<0?documentId.substring(0,documentId.indexOf(':')+1):documentId.substring(0,slash);
    }
    static String fromRelative(String volume,String path){
        if(volume==null||path==null||volume.equals("external")||volume.equals("internal"))return null;
        if(volume.equals("external_primary"))volume="primary";
        while(path.endsWith("/"))path=path.substring(0,path.length()-1);
        return volume+":"+path;
    }
    static boolean covers(String root,String parent){
        if(root==null||parent==null)return false;
        int colon=root.indexOf(':'),other=parent.indexOf(':');
        if(colon<1||other<1||!root.substring(0,colon).equalsIgnoreCase(parent.substring(0,other)))return false;
        String base=root.substring(colon+1),path=parent.substring(other+1);
        return path.equals(base)||base.isEmpty()||path.startsWith(base+"/");
    }
}
