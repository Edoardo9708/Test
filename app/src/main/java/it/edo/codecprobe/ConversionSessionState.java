package it.edo.codecprobe;

import android.content.Intent;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Explicit reset boundaries: queue totals survive recovery; per-file codec metrics never do. */
final class ConversionSessionState {
    static ConversionService.State begin(Intent request,SavedSession restored,List<CompressionSummary.Item> items,long now){
        ConversionService.State value=restored==null?new ConversionService.State():new ConversionService.State(restored.state);
        if(restored==null){
            value.sessionId=UUID.randomUUID().toString();
            value.inputs=new ArrayList<>(request.getStringArrayListExtra("inputs"));
            value.names=new ArrayList<>(request.getStringArrayListExtra("names"));
            value.folders=new ArrayList<>(request.getStringArrayListExtra("folders"));
            value.batchTotal=value.inputs.size();
            value.individualOrigins=request.getBooleanArrayExtra("individualOrigins");
            if(value.individualOrigins==null)value.individualOrigins=new boolean[value.batchTotal];
            ArrayList<String> choices=request.getStringArrayListExtra("individualFolders");
            value.individualFolders=new ArrayList<>(choices==null?value.folders:choices);
            value.applyAllDestinations=request.getBooleanExtra("applyAllDestinations",false);
            value.allOrigin=request.getBooleanExtra("allOrigin",true);value.allFolder=request.getStringExtra("allFolder");
            value.originMode=request.getBooleanExtra("originMode",false);
            value.requestedCrf=request.getIntExtra("crf",23);value.requestedCpu=request.getIntExtra("cpu",1);
            value.requestedPreset=AppSettings.PRESETS[Math.max(0,Math.min(8,request.getIntExtra("preset",6)))];
            value.onlySmaller=request.getBooleanExtra("onlySmaller",false);
            value.source=value.names.get(0);value.input=Uri.parse(value.inputs.get(0));value.folder=Uri.parse(value.folders.get(0));
            value.message="Analisi video…";
            for(int n=0;n<value.batchTotal;n++)value.outputUris.add("");
        }
        value.items=new ArrayList<>(items);value.sizeReport=CompressionSummary.render(items,true);
        value.hasSession=true;value.running=true;value.recoverable=true;
        value.startedAt=now;value.endedAt=0;
        value.pauseRequested=false;value.paused=false;value.cancelling=false;value.savingSession=false;
        resetFile(value);
        return value;
    }

    static void resetFile(ConversionService.State value){
        value.filePercent=0;value.doneFrames=0;value.totalFrames=-1;value.done=0;value.total=0;
        value.bframeCount=-1;value.lookaheadCount=-1;value.cuTree=-1;value.aqMode=-1;value.aqStrength=-1;
        value.fileActive=false;value.skipping=false;
    }
    private ConversionSessionState(){}
}
