package it.edo.codecprobe;

import java.util.*;

/** Individual choices survive temporary queue-wide overrides. No Android dependencies. */
final class DestinationPlan {
    static final class Choice {
        boolean origin;
        String folder;
        Choice(boolean origin,String folder){this.origin=origin;this.folder=folder==null?"":folder;}
    }
    final ArrayList<Choice> files=new ArrayList<>();
    final Choice all=new Choice(true,"");
    boolean applyAll;
    void reset(int count,String defaultFolder){
        files.clear();for(int i=0;i<count;i++)files.add(new Choice(defaultFolder==null||defaultFolder.isEmpty(),defaultFolder));
        all.origin=defaultFolder==null||defaultFolder.isEmpty();all.folder=defaultFolder==null?"":defaultFolder;applyAll=false;
    }
    Choice effective(int index){return applyAll?all:files.get(index);}
    void setFolder(int index,String folder){Choice c=files.get(index);c.folder=folder==null?"":folder;c.origin=false;}
    void setAllFolder(String folder){all.folder=folder==null?"":folder;all.origin=false;}
    ArrayList<Boolean> effectiveOrigins(){ArrayList<Boolean> r=new ArrayList<>();for(int i=0;i<files.size();i++)r.add(effective(i).origin);return r;}
    ArrayList<String> effectiveFolders(){ArrayList<String> r=new ArrayList<>();for(int i=0;i<files.size();i++)r.add(effective(i).folder);return r;}
    boolean[] individualOrigins(){boolean[] r=new boolean[files.size()];for(int i=0;i<r.length;i++)r[i]=files.get(i).origin;return r;}
    ArrayList<String> individualFolders(){ArrayList<String> r=new ArrayList<>();for(Choice c:files)r.add(c.folder);return r;}
    static final class Snapshot {
        private final boolean[] origins;
        private final ArrayList<String> folders;
        private final boolean applyAll,allOrigin;
        private final String allFolder;
        Snapshot(DestinationPlan plan){origins=plan.individualOrigins();folders=plan.individualFolders();applyAll=plan.applyAll;allOrigin=plan.all.origin;allFolder=plan.all.folder;}
        boolean matches(DestinationPlan plan){
            if(origins.length!=plan.files.size()||applyAll!=plan.applyAll||allOrigin!=plan.all.origin||!Objects.equals(allFolder,plan.all.folder))return false;
            for(int i=0;i<origins.length;i++){Choice choice=plan.files.get(i);if(origins[i]!=choice.origin||!Objects.equals(folders.get(i),choice.folder))return false;}
            return true;
        }
    }
    void restore(boolean[] origins,List<String> folders,boolean apply,boolean allOrigin,String allFolder){
        if(origins==null||folders==null||origins.length!=files.size()||folders.size()!=files.size())return;
        for(int i=0;i<origins.length;i++){files.get(i).origin=origins[i];files.get(i).folder=folders.get(i)==null?"":folders.get(i);}
        applyAll=apply;all.origin=allOrigin;all.folder=allFolder==null?"":allFolder;
    }
}
