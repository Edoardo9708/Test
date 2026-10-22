package it.edo.codecprobe;

import android.widget.*;

/** Exact report row geometry shared by video, audio and photos. */
final class ResultReportRow {
    static void add(AppUi ui,LinearLayout parent,String text,Runnable open,Runnable error){
        add(ui,parent,text,open,error,false);
    }
    static void add(AppUi ui,LinearLayout parent,String text,Runnable open,Runnable error,boolean information){
        LinearLayout row=WorkReportView.row(ui,parent);TextView body=WorkReportView.body(ui,row,text);
        body.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));String title=text.split("\n",2)[0];
        if(open!=null){Button button=ui.button(row,"Apri");button.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(88),-2));button.setContentDescription("Apri risultato: "+title);button.setOnClickListener(v->open.run());}
        if(error!=null){Button button=new Button(ui.activity);ui.styleButton(button,false);if(!information)ui.danger(button);button.setText(information?"i":"!");button.setTextSize(22);button.setPadding(0,0,0,0);button.setMinHeight(ui.dp(48));button.setMinimumHeight(ui.dp(48));button.setContentDescription((information?"Mostra dettagli: ":"Mostra errore: ")+title);row.addView(button,new LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)));button.setOnClickListener(v->error.run());}
    }
    private ResultReportRow(){}
}
