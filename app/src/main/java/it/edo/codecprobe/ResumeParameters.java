package it.edo.codecprobe;

import android.content.Intent;

/** Only tab 2 encoder options may change on resume; file plans and reports are untouched. */
final class ResumeParameters {
    static Intent current(HomeController home){
        return new Intent().putExtra("crf",home.compression.selectedCrf())
            .putExtra("preset",home.screen.compression.presetSlider.getProgress())
            .putExtra("cpu",home.screen.compression.cpuMode.getSelectedItemPosition())
            .putExtra("bframes",home.screen.compression.bframeSwitch.isChecked())
            .putExtra("lookahead",home.screen.compression.lookaheadSwitch.isChecked());
    }

    static Intent choose(Intent saved,Intent current,boolean useCurrent){
        Intent result=new Intent(saved);
        if(useCurrent){
            for(String key:new String[]{"crf","preset","cpu"})result.putExtra(key,current.getIntExtra(key,0));
            for(String key:new String[]{"bframes","lookahead"})result.putExtra(key,current.getBooleanExtra(key,true));
        }
        result.putExtra("resumeParameterChoice",useCurrent?"attuali della scheda 2":"salvati nella sessione");
        return result;
    }

    static String describe(Intent request){
        String[] cpu={"Limited","Auto","Max"};
        return "Speed: "+AppSettings.PRESETS[Math.max(0,Math.min(8,request.getIntExtra("preset",6)))]+
            " · CRF: "+request.getIntExtra("crf",23)+
            "\nCPU: "+cpu[Math.max(0,Math.min(2,request.getIntExtra("cpu",1)))]+
            "\nB-frame: "+(request.getBooleanExtra("bframes",true)?"ON":"OFF")+
            " · Lookahead: "+(request.getBooleanExtra("lookahead",true)?"ON":"OFF");
    }
    private ResumeParameters(){}
}
