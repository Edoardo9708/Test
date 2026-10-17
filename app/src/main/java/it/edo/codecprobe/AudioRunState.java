package it.edo.codecprobe;

import java.util.ArrayList;

/** Audio runtime snapshot only. Never serialized into the video queue journal. */
final class AudioRunState {
    static final class Result {
        final String name,uri,error;
        final long inputBytes,outputBytes;
        Result(String name,String uri,long inputBytes,long outputBytes,String error){this.name=name;this.uri=uri;this.inputBytes=inputBytes;this.outputBytes=outputBytes;this.error=error;}
    }
    boolean running,cancelling;
    int current,total,bitrate,channels,rate,percent;
    long doneSamples,totalSamples,startedAt,endedAt;
    String message="Pronto per una prova FLAC → Opus VBR",name="—",log="";
    final ArrayList<Result> results=new ArrayList<>();
    AudioRunState(){}
    AudioRunState(AudioRunState s){
        running=s.running;cancelling=s.cancelling;current=s.current;total=s.total;bitrate=s.bitrate;channels=s.channels;rate=s.rate;percent=s.percent;
        doneSamples=s.doneSamples;totalSamples=s.totalSamples;startedAt=s.startedAt;endedAt=s.endedAt;message=s.message;name=s.name;log=s.log;results.addAll(s.results);
    }
    static String sizes(long before,long after){
        if(before<=0)return String.format(java.util.Locale.ROOT,"Output %.2f MiB",after/1048576.0);
        return String.format(java.util.Locale.ROOT,"%.2f → %.2f MiB · %+.1f%%",before/1048576.0,after/1048576.0,(after-before)*100.0/before);
    }
}
