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
    static final class Reusable {
        final LinearLayout row;
        private final AppUi ui;
        private final TextView body;
        private Button openButton,errorButton;
        private Boolean informationStyle;
        Reusable(AppUi ui,LinearLayout parent){this.ui=ui;row=WorkReportView.row(ui,parent);body=WorkReportView.body(ui,row,"");body.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));}
        void bind(String text,Runnable open,Runnable error,boolean information){
            UiText.set(body,text);String title=text.split("\n",2)[0];
            if(open!=null&&openButton==null){openButton=ui.button(row,"Apri");openButton.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(88),-2));}
            if(openButton!=null){openButton.setVisibility(open==null?android.view.View.GONE:android.view.View.VISIBLE);openButton.setContentDescription("Apri risultato: "+title);openButton.setOnClickListener(open==null?null:v->open.run());}
            if(error!=null&&errorButton==null){errorButton=new Button(ui.activity);errorButton.setTextSize(22);errorButton.setPadding(0,0,0,0);errorButton.setMinHeight(ui.dp(48));errorButton.setMinimumHeight(ui.dp(48));row.addView(errorButton,new LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)));}
            if(errorButton!=null){
                errorButton.setVisibility(error==null?android.view.View.GONE:android.view.View.VISIBLE);
                if(informationStyle==null||informationStyle!=information){ui.styleButton(errorButton,false);if(!information)ui.danger(errorButton);errorButton.setTextSize(22);errorButton.setPadding(0,0,0,0);errorButton.setMinHeight(ui.dp(48));errorButton.setMinimumHeight(ui.dp(48));informationStyle=information;}
                UiText.set(errorButton,information?"i":"!");errorButton.setContentDescription((information?"Mostra dettagli: ":"Mostra errore: ")+title);errorButton.setOnClickListener(error==null?null:v->error.run());
            }
        }
    }
    private ResultReportRow(){}
}
