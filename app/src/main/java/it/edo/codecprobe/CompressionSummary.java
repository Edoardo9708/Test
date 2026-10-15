package it.edo.codecprobe;

import java.util.List;
import java.util.Locale;

/** Whole-file byte sizes, including audio and metadata. Failed outputs never count as savings. */
final class CompressionSummary {
    enum Status { COMPLETE, FAILED, CANCELLED, IN_PROGRESS, SKIPPED }
    static final class Item {
        final int number;final String name;final Status status;final long before,after;
        Item(int number,String name,Status status,long before,long after){this.number=number;this.name=name;this.status=status;this.before=before;this.after=after;}
    }
    static String size(long bytes){
        if(bytes<0)return "dimensione non disponibile";
        return bytes>=1000000?String.format(Locale.ITALY,"%.2f MB",bytes/1000000.0):String.format(Locale.ITALY,"%.2f kB",bytes/1000.0);
    }
    static String change(long before,long after){
        if(before<=0||after<0)return "variazione non calcolabile";
        double percent=(after/(double)before-1)*100;
        if(Math.abs(percent)<0.05)return "0,0%";
        return String.format(Locale.ITALY,"%+.1f%%",percent);
    }
    static String row(Item item){return row(item,false);}
    private static String row(Item item,boolean ongoing){
        String title="Video "+item.number+" · "+item.name.replace('\n',' ').replace('\r',' ');
        switch(item.status){
            case COMPLETE:return title+"\n"+size(item.before)+" → "+size(item.after)+" · "+change(item.before,item.after);
            case IN_PROGRESS:return title+"\n"+(ongoing?"In corso":"Non completato");
            case FAILED:return title+"\nConversione fallita · nessun nuovo file completato";
            case CANCELLED:return title+"\nConversione annullata";
            default:return title+"\n"+(ongoing?"In attesa":"Non convertito");
        }
    }
    static String render(List<Item> items){return render(items,false);}
    static String render(List<Item> items,boolean ongoing){
        if(items.isEmpty())return "";
        StringBuilder text=new StringBuilder();long before=0,after=0;int completed=0,comparable=0;
        boolean overflow=false;
        for(Item item:items){
            if(text.length()>0)text.append("\n\n");text.append(row(item,ongoing));
            if(item.status==Status.COMPLETE){completed++;if(item.before>=0&&item.after>=0){
                comparable++;try{before=Math.addExact(before,item.before);after=Math.addExact(after,item.after);}catch(ArithmeticException e){overflow=true;}
            }}
        }
        text.append(ongoing?"\n\nTOTALE PARZIALE · VIDEO RIUSCITI":"\n\nTOTALE · VIDEO RIUSCITI");
        if(ongoing&&completed==0){text.append("\nIn attesa del primo video completato");return text.toString();}
        if(comparable==0||overflow){text.append("\nConfronto dimensioni non disponibile");return text.toString();}
        text.append("\n").append(size(before)).append(" → ").append(size(after)).append(" · ").append(change(before,after));
        if(before>after)text.append("\nSpazio risparmiato: ").append(size(before-after));
        else if(after>before)text.append("\nSpazio aggiuntivo: ").append(size(after-before));
        else text.append("\nDimensione totale invariata");
        if(comparable<completed)text.append("\nTotale calcolato su ").append(comparable).append(" di ").append(completed).append(" video riusciti: dimensioni mancanti escluse.");
        text.append("\nDimensioni del file completo, inclusi audio e metadati. MB = 1.000.000 byte.");
        return text.toString();
    }
    private CompressionSummary(){}
}
