package it.edo.codecprobe;

import android.content.Intent;
import android.widget.*;

/** Workspace navigation only. It never resets, starts or cancels a conversion. */
final class AppNavigation extends LinearLayout {
    final Button video,audio;
    AppNavigation(AppUi ui,boolean audioSelected){
        super(ui.activity);setOrientation(VERTICAL);setBackgroundColor(ui.surface);setPadding(ui.dp(10),ui.dp(2),ui.dp(10),ui.dp(2));
        LinearLayout row=ui.row(this);video=ui.halfButton(row,"Video",false);audio=ui.halfButton(row,"Audio",false);
        video.setSelected(!audioSelected);audio.setSelected(audioSelected);
        video.setContentDescription("Video"+(!audioSelected?" · selezionato":""));audio.setContentDescription("Audio"+(audioSelected?" · selezionato":""));
        video.setOnClickListener(v->{
            if(!audioSelected)return;
            ui.activity.startActivity(new Intent(ui.activity,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP));
            ui.activity.finish();
        });
        audio.setOnClickListener(v->{if(!audioSelected)ui.activity.startActivity(new Intent(ui.activity,AudioActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP));});
    }
}
