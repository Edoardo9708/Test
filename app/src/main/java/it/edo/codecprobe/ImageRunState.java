package it.edo.codecprobe;

import java.util.ArrayList;

/** Snapshot of a photo queue. It is never the editable workspace draft. */
final class ImageRunState {
    enum Outcome {SAVED,FAILED,CANCELLED,NOT_SMALLER,ALREADY_DONE}
    static final class Result {
        final String name,uri,message,details,sourceSha,outputSha;final long inputBytes,outputBytes;final Outcome outcome;
        Result(String name,long input,long output,String uri,String message,Outcome outcome,String details){this(name,input,output,uri,message,outcome,details,"","");}
        Result(String name,long input,long output,String uri,String message,Outcome outcome,String details,String sourceSha,String outputSha){this.name=name;inputBytes=input;outputBytes=output;this.uri=uri;this.message=message;this.outcome=outcome;this.details=details;this.sourceSha=sourceSha;this.outputSha=outputSha;}
    }
    static final int DETAIL_LIMIT=16384;
    int completed,saved,failed,skipped,cancelledCount,alreadyDone;
    long inputSavedBytes,outputSavedBytes;
    java.util.List<String> queueNames=java.util.Collections.emptyList();
    int firstResult(){return completed-results.size();}
    int windowStart(){return Math.max(0,Math.min(current-50,Math.max(0,total-ImageQueueStore.VISIBLE)));}
    Result resultAt(int index){int n=index-firstResult();return n>=0&&n<results.size()?results.get(n):null;}
    void add(Result result){
        completed++;
        switch(result.outcome){
            case SAVED:saved++;inputSavedBytes+=result.inputBytes;outputSavedBytes+=result.outputBytes;break;
            case ALREADY_DONE:alreadyDone++;inputSavedBytes+=result.inputBytes;outputSavedBytes+=result.outputBytes;break;
            case FAILED:failed++;break;
            case NOT_SMALLER:skipped++;break;
            default:cancelledCount++;
        }
        String detail=result.details==null?"":result.details;
        if(detail.length()>DETAIL_LIMIT)result=new Result(result.name,result.inputBytes,result.outputBytes,result.uri,result.message,result.outcome,detail.substring(0,DETAIL_LIMIT)+"\nDettagli completi nel log su disco.",result.sourceSha,result.outputSha);
        results.add(result);if(results.size()>ImageQueueStore.VISIBLE)results.remove(0);
    }
    boolean running,cancelling,pauseRequested,paused;long startedAt,endedAt;int current,total;String message="",name="",log="";ImageOptions options;
    final ArrayList<Result> results=new ArrayList<>();
    ImageRunState(){}
    ImageRunState(ImageRunState other){running=other.running;cancelling=other.cancelling;pauseRequested=other.pauseRequested;paused=other.paused;startedAt=other.startedAt;endedAt=other.endedAt;current=other.current;total=other.total;message=other.message;name=other.name;log=other.log;options=other.options;results.addAll(other.results);completed=other.completed;alreadyDone=other.alreadyDone;saved=other.saved;failed=other.failed;skipped=other.skipped;cancelledCount=other.cancelledCount;inputSavedBytes=other.inputSavedBytes;outputSavedBytes=other.outputSavedBytes;queueNames=other.queueNames;}
}
