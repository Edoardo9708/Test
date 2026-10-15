package it.edo.codecprobe;

import java.util.*;
public final class FolderPathsTest {
    private static void equal(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
    public static void main(String[] args){
        equal("primary:DCIM/Camera",FolderPaths.parent("primary:DCIM/Camera/first.mp4"));
        equal(FolderPaths.parent("primary:DCIM/Camera/first.mp4"),FolderPaths.parent("primary:DCIM/Camera/second.mp4"));
        equal("primary:",FolderPaths.parent("primary:video.mp4"));
        equal(null,FolderPaths.parent("video:123".substring(6)));
        equal("primary:DCIM/Camera",FolderPaths.fromRelative("external_primary","DCIM/Camera/"));
        equal("A1B2-C3D4:Movies",FolderPaths.fromRelative("A1B2-C3D4","Movies/"));
        equal(null,FolderPaths.fromRelative("external","DCIM/"));
        equal(true,FolderPaths.covers("primary:DCIM","primary:DCIM/Camera"));
        equal(true,FolderPaths.covers("primary:DCIM/Camera","primary:DCIM/Camera"));
        equal(false,FolderPaths.covers("primary:DCIM/Camera","primary:DCIM/Camera2"));
        equal(false,FolderPaths.covers("primary:DCIM/Camera","primary:DCIM/camera"));
        equal(false,FolderPaths.covers("primary:DCIM","A1B2-C3D4:DCIM/Camera"));
        equal(true,FolderPaths.covers("a1b2-c3d4:Movies","A1B2-C3D4:Movies"));
        equal(FolderPaths.groupKey("primary:DCIM/Camera",null,"a"),FolderPaths.groupKey(null,"primary:DCIM/Camera","b"));
        equal(FolderPaths.groupKey("a1b2-c3d4:Movies",null,"a"),FolderPaths.groupKey(null,"A1B2-C3D4:Movies","b"));
        equal(false,FolderPaths.groupKey(null,null,"unknown:a").equals(FolderPaths.groupKey(null,null,"unknown:b")));
        List<String> inputs=List.of("primary:DCIM/Camera/a.mp4","primary:DCIM/Camera/b.mp4","primary:Movies/a.mp4");
        Map<String,List<Integer>> grouped=new LinkedHashMap<>();
        for(int i=0;i<inputs.size();i++)grouped.computeIfAbsent(FolderPaths.parent(inputs.get(i)),k->new ArrayList<>()).add(i+1);
        equal(List.of(1,2),grouped.get("primary:DCIM/Camera"));equal(List.of(3),grouped.get("primary:Movies"));
        for(String input:inputs)equal(FolderPaths.parent(input).startsWith("primary:DCIM/"),FolderPaths.covers("primary:DCIM",FolderPaths.parent(input)));
        System.out.println("Folder resolution: shared parents, multiple folders, ancestor grants, boundaries and volumes OK");
    }
}
