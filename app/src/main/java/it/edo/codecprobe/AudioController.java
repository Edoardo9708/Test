package it.edo.codecprobe;

import android.os.Bundle;
import android.provider.DocumentsContract;
import android.net.Uri;
import android.widget.*;

/** Audio lifecycle owner. Video state, service and settings are never imported or reset. */
final class AudioController {
    final AudioActivity activity;final AudioState state;final AudioScreen screen;final AudioSelection selection;
    AudioController(AudioActivity activity,Bundle saved){
        this.activity=activity;state=AudioDraftStore.load(activity);
        screen=new AudioScreen(new AppUi(activity),AudioDraftStore.expansion(activity,saved),state);
        selection=new AudioSelection(this);
        screen.pick.setOnClickListener(v->selection.pick());screen.clear.setOnClickListener(v->selection.clear());
        screen.chooseFolder.setOnClickListener(v->selection.folder());screen.settings.setOnClickListener(v->screen.sections.focusCard(AudioScreen.COMPRESSION));
        screen.bitrate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar slider,int progress,boolean fromUser){state.bitrate=64+progress*8;save();render();}
            public void onStartTrackingTouch(SeekBar slider){}public void onStopTrackingTouch(SeekBar slider){}
        });
        render();
    }
    void render(){
        int count=state.inputs.size();screen.selected.setText(count==0?"Nessun brano selezionato":count+" brani selezionati");
        StringBuilder names=new StringBuilder();for(int n=0;n<Math.min(10,count);n++){if(n>0)names.append('\n');names.append(n+1).append(". ").append(state.names.get(n));}
        if(count>10)names.append("\n… e altri ").append(count-10).append(" brani selezionati");screen.names.setText(names);screen.clear.setEnabled(count>0);
        String folder="Nessuna cartella audio scelta";
        if(state.folder!=null)try{folder=DocumentsContract.getTreeDocumentId(Uri.parse(state.folder));}catch(IllegalArgumentException error){folder=state.folder;}
        screen.folder.setText(folder);screen.bitrateLabel.setText("Bitrate audio · "+state.bitrate+" kbit/s · VBR");
    }
    void save(){AudioDraftStore.save(activity,state);}
    void pause(){save();AudioDraftStore.saveExpansion(activity,screen.sections);}
    void destroy(){state.destroyed=true;screen.sections.closed=true;selection.close();}
    void toast(String message){Toast.makeText(activity,message,Toast.LENGTH_LONG).show();}
}
