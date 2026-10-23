package it.edo.codecprobe;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.widget.*;
import java.util.*;

/** Common dashboard geometry. Media provide values and only their applicable progress/parameter rows. */
final class ConversionDashboard {
    final TextView summary,totalProgress,elapsed,current,percent,status,cpu,ram,battery;
    final ProgressBar progress;
    final Map<String,TextView> measures=new LinkedHashMap<>(),parameters=new LinkedHashMap<>();
    final ProcessSystemMonitor monitor;
    ConversionDashboard(AppUi ui,LinearLayout parent,String[] progressRows,String[] parameterRows){
        ui.sectionHeading(parent,"SOMMARIO");LinearLayout panel=ui.panel(parent);
        summary=ui.text(panel,"Nessuna coda avviata",16);totalProgress=ui.dashboardRow(panel,"Avanzamento coda","—");elapsed=ui.dashboardRow(panel,"Tempo trascorso","00:00:00");
        ui.sectionHeading(parent,"PROGRESSO · FILE ATTUALE");current=ui.text(parent,"Nessun file in conversione",16);current.setTypeface(null,Typeface.BOLD);current.setMaxLines(2);current.setEllipsize(android.text.TextUtils.TruncateAt.END);
        percent=ui.text(parent,"0%",28);percent.setTypeface(null,Typeface.BOLD);
        progress=new ProgressBar(ui.activity,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setProgressTintList(ColorStateList.valueOf(ui.accent));progress.setProgressBackgroundTintList(ColorStateList.valueOf(ui.border));
        LinearLayout.LayoutParams bar=new LinearLayout.LayoutParams(-1,ui.dp(12));bar.setMargins(0,ui.dp(8),0,ui.dp(12));parent.addView(progress,bar);
        for(String name:progressRows)measures.put(name,ui.dashboardRow(parent,name,"—"));
        status=ui.text(parent,"Scegli file, cartelle e qualità.",14);
        ui.sectionHeading(parent,"PARAMETRI IN USO");for(String name:parameterRows)parameters.put(name,ui.dashboardRow(parent,name,"—"));
        ui.sectionHeading(parent,"SISTEMA");LinearLayout metrics=ui.row(parent);cpu=ui.metric(metrics,"CPU app");ram=ui.metric(metrics,"RAM · PSS");battery=ui.metric(metrics,"Batteria °C");
        ui.parameterRow(parent,"Info misure","CPU: percentuale del processo rispetto a tutti i core disponibili. RAM: PSS del processo, inclusi Java, codice nativo e quota delle pagine condivise; MiB = 1.048.576 byte. La temperatura è quella della batteria. I colori indicano intensità di utilizzo e temperatura, non errori. Il tempo trascorso include analisi, codifica, verifica e salvataggio; non è una stima del tempo rimanente.");
        monitor=new ProcessSystemMonitor(ui,cpu,ram,battery);
    }
    void parameter(String name,String value){UiText.set(parameters.get(name),value);}
}
