package it.edo.codecprobe;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/** Audio runtime snapshot only. Never serialized into the video queue journal. */
final class AudioRunState {
    enum Outcome { SAVED, FAILED, CANCELLED, NOT_SAVED }
    private static final AtomicLong NEXT=new AtomicLong();
    static final class Result {
        final String name,input,uri,error;final Outcome outcome;
        final long inputBytes,outputBytes;
        Result(String name,String uri,long inputBytes,long outputBytes,String error){this(name,null,uri,inputBytes,outputBytes,error,uri!=null?Outcome.SAVED:Outcome.FAILED);}
        Result(String name,String input,String uri,long before,long after,String error,Outcome outcome){this.name=name;this.input=input;this.uri=uri;inputBytes=before;outputBytes=after;this.error=error;this.outcome=outcome;}
    }
    boolean running,cancelling,onlySmaller;
    int current,total,bitrate,channels,rate,percent;
    long doneSamples,totalSamples,startedAt,endedAt,sessionId;
    String message="Scegli audio, cartella e qualità Opus.",name="Nessun file in conversione",log="";
    final ArrayList<Result> results=new ArrayList<>();
    AudioRunState(){sessionId=NEXT.incrementAndGet();}
    AudioRunState(AudioRunState s){
        sessionId=s.sessionId;running=s.running;cancelling=s.cancelling;onlySmaller=s.onlySmaller;current=s.current;total=s.total;bitrate=s.bitrate;channels=s.channels;rate=s.rate;percent=s.percent;
        doneSamples=s.doneSamples;totalSamples=s.totalSamples;startedAt=s.startedAt;endedAt=s.endedAt;message=s.message;name=s.name;log=s.log;results.addAll(s.results);
    }
    int count(Outcome outcome){int n=0;for(Result result:results)if(result.outcome==outcome)n++;return n;}
    int queuePercent(){return BatchProgress.percent(results.size(),running&&current>results.size()?percent:0,total);}
    String summary(){return total==0?"Nessuna coda avviata":results.size()+" / "+total+" elaborati · riusciti "+count(Outcome.SAVED)+" · falliti "+count(Outcome.FAILED)+" · non salvati "+count(Outcome.NOT_SAVED)+" · interrotti "+count(Outcome.CANCELLED)+( !running&&results.size()<total?" · non elaborati "+(total-results.size()):"");}
    String savings(){long before=0,after=0;int saved=0;boolean allKnown=true;
        for(Result r:results)if(r.outcome==Outcome.SAVED){saved++;if(r.inputBytes<=0)allKnown=false;else before+=r.inputBytes;after+=r.outputBytes;}
        if(saved==0)return "Nessun file salvato";
        return "Totale audio salvati · "+(allKnown?sizes(before,after):String.format(Locale.ROOT,"Output %.2f MB · dimensioni originali incomplete",after/1000000.0))+(allKnown?String.format(Locale.ROOT,"\nSpazio %s: %.2f MB",before>=after?"risparmiato":"aggiuntivo",Math.abs(before-after)/1000000.0):"");
    }
    static String sizes(long before,long after){
        if(before<=0)return String.format(Locale.ROOT,"Output %.2f MB",after/1000000.0);
        return String.format(Locale.ROOT,"%.2f → %.2f MB · %+.1f%%",before/1000000.0,after/1000000.0,(after-before)*100.0/before);
    }
}
