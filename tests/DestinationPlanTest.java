package it.edo.codecprobe;

import java.util.*;
public final class DestinationPlanTest {
    private static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        DestinationPlan p=new DestinationPlan();p.reset(3,null);
        p.setFolder(0,"content://a/tree/first");p.setFolder(2,"content://a/tree/third");
        check(!p.effective(0).origin&&p.effective(1).origin&&!p.effective(2).origin);
        check(p.effective(0).folder.equals("content://a/tree/first"));
        p.setAllFolder("content://a/tree/all");p.applyAll=true;
        check(p.effectiveFolders().equals(List.of("content://a/tree/all","content://a/tree/all","content://a/tree/all")));
        p.all.origin=true;check(p.effectiveOrigins().equals(List.of(true,true,true)));
        p.applyAll=false;check(p.effectiveFolders().equals(List.of("content://a/tree/first","","content://a/tree/third")));
        check(p.effectiveOrigins().equals(List.of(false,true,false)));
        p.files.get(0).origin=true;p.files.get(0).origin=false;check(p.files.get(0).folder.equals("content://a/tree/first"));
        ArrayList<String> snapshot=p.effectiveFolders();p.setFolder(0,"content://a/tree/changed");check(snapshot.get(0).equals("content://a/tree/first"));
        DestinationPlan restored=new DestinationPlan();restored.reset(3,null);restored.restore(p.individualOrigins(),p.individualFolders(),true,false,"content://a/tree/global");
        check(restored.effectiveFolders().equals(List.of("content://a/tree/global","content://a/tree/global","content://a/tree/global")));
        restored.applyAll=false;check(restored.effectiveFolders().equals(p.effectiveFolders()));
        p.reset(2,"content://a/tree/default");check(!p.applyAll&&p.effectiveFolders().equals(List.of("content://a/tree/default","content://a/tree/default")));
        System.out.println("Destination plan: mixed individual choices, queue override, restore, defaults and immutable snapshots OK");
    }
}
