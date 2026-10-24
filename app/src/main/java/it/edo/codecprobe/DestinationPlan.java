package it.edo.codecprobe;

import java.util.*;

/** Individual choices survive temporary queue-wide overrides. No Android dependencies. */
final class DestinationPlan {
    static final class Choice {
        boolean origin,exception;
        String folder;
        Choice(boolean origin,String folder){this.origin=origin;this.folder=folder==null?"":folder;}
    }
    final ArrayList<Choice> files=new ArrayList<>();
    final Choice all=new Choice(true,"");
    boolean applyAll,exceptionsMode;
    void reset(int count,String defaultFolder){
        exceptionsMode=false;files.clear();for(int i=0;i<count;i++)files.add(new Choice(defaultFolder==null||defaultFolder.isEmpty(),defaultFolder));
        all.origin=defaultFolder==null||defaultFolder.isEmpty();all.folder=defaultFolder==null?"":defaultFolder;applyAll=false;
    }
    Choice effective(int index){return exceptionsMode?(files.get(index).exception?files.get(index):all):applyAll?all:files.get(index);}
    void enableExceptions(){
        if(exceptionsMode)return;
        for(Choice choice:files)choice.exception=!applyAll&&(choice.origin!=all.origin||!Objects.equals(choice.folder,all.folder));
        exceptionsMode=true;applyAll=false;
    }
    void exception(int index,boolean enabled){Choice choice=files.get(index);if(enabled&&!choice.exception){choice.origin=all.origin;choice.folder=all.folder;}choice.exception=enabled;}
    int exceptionCount(){int count=0;for(Choice choice:files)if(choice.exception)count++;return count;}
    void setFolder(int index,String folder){Choice c=files.get(index);c.folder=folder==null?"":folder;c.origin=false;if(exceptionsMode)c.exception=true;}
    void setAllFolder(String folder){all.folder=folder==null?"":folder;all.origin=false;}
    ArrayList<Boolean> effectiveOrigins(){ArrayList<Boolean> r=new ArrayList<>();for(int i=0;i<files.size();i++)r.add(effective(i).origin);return r;}
    ArrayList<String> effectiveFolders(){ArrayList<String> r=new ArrayList<>();for(int i=0;i<files.size();i++)r.add(effective(i).folder);return r;}
    boolean[] individualOrigins(){boolean[] r=new boolean[files.size()];for(int i=0;i<r.length;i++)r[i]=files.get(i).origin;return r;}
    ArrayList<String> individualFolders(){ArrayList<String> r=new ArrayList<>();for(Choice c:files)r.add(c.folder);return r;}
    static final class Snapshot {
        private final boolean[] origins;
        private final ArrayList<String> folders;
        private final boolean applyAll,allOrigin,mode;private final boolean[] exceptions;
        private final String allFolder;
        Snapshot(DestinationPlan plan){mode=plan.exceptionsMode;exceptions=new boolean[plan.files.size()];for(int n=0;n<exceptions.length;n++)exceptions[n]=plan.files.get(n).exception;origins=plan.individualOrigins();folders=plan.individualFolders();applyAll=plan.applyAll;allOrigin=plan.all.origin;allFolder=plan.all.folder;}
        boolean matches(DestinationPlan plan){
            if(mode!=plan.exceptionsMode||origins.length!=plan.files.size()||applyAll!=plan.applyAll||allOrigin!=plan.all.origin||!Objects.equals(allFolder,plan.all.folder))return false;
            for(int i=0;i<origins.length;i++){Choice choice=plan.files.get(i);if(exceptions[i]!=choice.exception||origins[i]!=choice.origin||!Objects.equals(folders.get(i),choice.folder))return false;}
            return true;
        }
    }
    void restore(boolean[] origins,List<String> folders,boolean apply,boolean allOrigin,String allFolder){
        if(origins==null||folders==null||origins.length!=files.size()||folders.size()!=files.size())return;
        for(int i=0;i<origins.length;i++){files.get(i).origin=origins[i];files.get(i).folder=folders.get(i)==null?"":folders.get(i);}
        applyAll=apply;all.origin=allOrigin;all.folder=allFolder==null?"":allFolder;
    }
}
