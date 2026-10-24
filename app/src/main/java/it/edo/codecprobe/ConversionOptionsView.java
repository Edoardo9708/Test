package it.edo.codecprobe;

import android.widget.*;

/** Common conversion options, in the same position and with the same help in every workspace. */
final class ConversionOptionsView {
    final CheckBox screenOn,smaller;
    ConversionOptionsView(AppUi ui,LinearLayout parent,boolean screen,boolean onlySmaller){
        screenOn=option(ui,parent,"Mantieni schermo acceso",screen,"Evita lo spegnimento automatico durante la conversione mentre questa schermata è visibile. Puoi comunque spegnere lo schermo con il tasto del telefono. La scelta viene ricordata e non garantisce una velocità maggiore.");
        smaller=option(ui,parent,"Salva solo se più piccolo",onlySmaller,"Confronta il file completo, inclusi tutti i metadati conservati, dopo codifica e verifica. Se è uguale o più grande non viene salvato. Gli originali restano conservati.");
    }
    private static CheckBox option(AppUi ui,LinearLayout parent,String title,boolean value,String help){
        LinearLayout row=ui.row(parent);CheckBox check;if(ui.photos){check=new PhotoSwitch(ui);check.setText(title);check.setChecked(value);row.addView(check);}else check=SettingsPage.check(ui,row,title,value);check.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));ui.infoButton(row,title,help);return check;
    }
}
