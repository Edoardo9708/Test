package it.edo.codecprobe;

import android.app.AlertDialog;
import android.content.Intent;

/** Explicit replay of the previous queue's failed subset; the editable selection stays available. */
final class HomeRetry {
    static final int PERMISSION=12;
    private final HomeController home;
    AlertDialog confirmation;
    Intent pending;
    HomeRetry(HomeController home){this.home=home;}
    void bind(){home.screen.conversion.retryFailed.setOnClickListener(v->confirm());}
    void update(boolean busy){
        Intent task=ConversionService.retryRequest(home.activity);
        int count=task==null?0:task.getStringArrayListExtra("inputs").size();
        home.screen.conversion.retryFailed.setVisibility(count==0?android.view.View.GONE:android.view.View.VISIBLE);
        home.screen.conversion.retryFailed.setText("Riprova i falliti ("+count+")");
        home.screen.conversion.retryFailed.setEnabled(!busy&&!home.state.clearingLogs&&!home.state.choosingOrigins&&!home.state.metadataLoading&&!home.state.exportingDiagnostics);
    }
    void confirm(){
        if(confirmation!=null&&confirmation.isShowing())return;
        if(home.state.busy||home.state.clearingLogs||home.state.choosingOrigins||home.state.metadataLoading||home.state.exportingDiagnostics)return;
        Intent task=ConversionService.retryRequest(home.activity);if(task==null)return;
        int count=task.getStringArrayListExtra("inputs").size();
        confirmation=new AlertDialog.Builder(home.activity).setTitle("Riprova "+count+" video falliti")
            .setMessage("Rilancio solo i video falliti dell’ultima coda, con gli stessi parametri, cartelle e nomi di destinazione.\n\nCRF="+task.getIntExtra("crf",23)+
                " · preset="+AppSettings.PRESETS[Math.max(0,Math.min(8,task.getIntExtra("preset",6)))]+
                "\nSalva solo se più piccolo: "+(task.getBooleanExtra("onlySmaller",false)?"ON":"OFF")+
                "\n\nI video riusciti, ignorati, annullati o senza risparmio restano esclusi. I nomi saranno ricontrollati prima di iniziare.")
            .setNegativeButton("Annulla",null).setPositiveButton("Riprova",(d,w)->{pending=task;resume();}).create();
        confirmation.setOnDismissListener(d->confirmation=null);confirmation.show();
    }
    void resume(){
        if(pending==null||home.state.destroyed||home.state.busy||ConversionService.snapshot().running)return;
        if(!OriginalMedia.hasLocationPermission(home.activity)){
            home.activity.requestPermissions(new String[]{android.Manifest.permission.ACCESS_MEDIA_LOCATION},PERMISSION);return;
        }
        if(!OriginalMedia.hasVideoPermission(home.activity)){
            home.activity.requestPermissions(OriginalMedia.videoPermissions(),PERMISSION);return;
        }
        Intent task=pending;pending=null;
        FolderPickerActivity.record(home.activity,"*** RIPROVA SOLO I FALLITI ***\nFile: "+task.getStringArrayListExtra("inputs").size()+"\nParametri e destinazioni originali ripristinati; controllo nuovamente i nomi.");
        home.conversion.outputCheck.begin(task);
    }
    void onPermissions(int[] results){
        boolean granted=results.length>0;for(int result:results)granted &= result==android.content.pm.PackageManager.PERMISSION_GRANTED;
        if(!granted){
            pending=null;home.toast("Per riprovare servono accesso ai video e posizione dei file originali.");return;
        }
        resume();
    }
    void close(){if(confirmation!=null){confirmation.dismiss();confirmation=null;}}
}
