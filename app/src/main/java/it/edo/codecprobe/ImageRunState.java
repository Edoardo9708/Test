package it.edo.codecprobe;

import java.util.ArrayList;

/** Snapshot of a photo queue. It is never the editable workspace draft. */
final class ImageRunState {
    enum Outcome {SAVED,FAILED,CANCELLED,NOT_SMALLER}
    static final class Result {
        final String name,uri,message,details;final long inputBytes,outputBytes;final Outcome outcome;
        Result(String name,long input,long output,String uri,String message,Outcome outcome,String details){this.name=name;inputBytes=input;outputBytes=output;this.uri=uri;this.message=message;this.outcome=outcome;this.details=details;}
    }
    boolean running,cancelling;long startedAt,endedAt;int current,total;String message="",name="",log="";ImageOptions options;
    final ArrayList<Result> results=new ArrayList<>();
    ImageRunState(){}
    ImageRunState(ImageRunState other){running=other.running;cancelling=other.cancelling;startedAt=other.startedAt;endedAt=other.endedAt;current=other.current;total=other.total;message=other.message;name=other.name;log=other.log;options=other.options;results.addAll(other.results);}
}
