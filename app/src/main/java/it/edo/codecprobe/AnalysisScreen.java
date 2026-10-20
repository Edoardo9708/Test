package it.edo.codecprobe;

import android.graphics.Typeface;
import android.view.View;
import android.widget.*;
import java.util.*;
import java.util.function.Consumer;

/** Views only: readable block sizes, physical byte ranges, drill-down and optional HEX. */
final class AnalysisScreen {
    final AppUi ui;
    final LinearLayout files,map,trail,blocks,details,hexPanel;
    final TextView status,description,hexText,frameTitle,pageLabel;
    final Button back,reload,hex,previous,next,more;
    AnalysisScreen(AppUi ui){
        this.ui=ui;LinearLayout page=ui.newPage();
        ui.text(page,"Analisi",27);ui.text(page,"Struttura dei file · v"+BuildConfig.VERSION_NAME,14);
        status=ui.text(page,"",15);files=ui.panel(page);map=ui.panel(page);map.setVisibility(View.GONE);
        LinearLayout actions=ui.row(map);back=ui.halfButton(actions,"Elenco file",false);reload=ui.halfButton(actions,"Rileggi file",false);
        trail=new LinearLayout(ui.activity);trail.setOrientation(LinearLayout.VERTICAL);map.addView(trail);
        LinearLayout frame=ui.panel(map);frame.setBackground(ui.shape(ui.surface,14));frameTitle=ui.text(frame,"",18);frameTitle.setTypeface(null,Typeface.BOLD);
        blocks=new LinearLayout(ui.activity);blocks.setOrientation(LinearLayout.VERTICAL);frame.addView(blocks);more=ui.button(frame,"Mostra altri blocchi");
        details=ui.panel(map);description=ui.text(details,"",15);description.setTextIsSelectable(true);hex=ui.button(details,"Mostra byte HEX");
        hexPanel=ui.panel(details);hexPanel.setVisibility(View.GONE);ui.text(hexPanel,"Offset assoluti nel file · HEX e ASCII",13);
        HorizontalScrollView scroll=new HorizontalScrollView(ui.activity);hexText=new TextView(ui.activity);hexText.setTypeface(Typeface.MONOSPACE);hexText.setTextSize(12);hexText.setTextColor(ui.ink);hexText.setTextIsSelectable(true);scroll.addView(hexText);hexPanel.addView(scroll);
        pageLabel=ui.text(hexPanel,"",13);LinearLayout paging=ui.row(hexPanel);previous=ui.halfButton(paging,"← 256 byte",false);next=ui.halfButton(paging,"256 byte →",false);
        ui.install(page,new AppNavigation(ui,AppNavigation.Workspace.ANALYSIS));
    }
    void sources(List<AnalysisSources.Entry> entries,Consumer<AnalysisSources.Entry> select){
        files.removeAllViews();files.setVisibility(View.VISIBLE);map.setVisibility(View.GONE);
        ui.text(files,"FILE GIÀ SELEZIONATI",13);ui.text(files,"L’elenco riusa le selezioni. La struttura viene letta solo aprendo un file.",15);
        if(entries.isEmpty())ui.text(files,"Seleziona prima un file in Video, Audio o Immagini.",17);
        for(AnalysisSources.Entry e:entries){Button b=ui.button(files,e.name+"\n"+e.workspace+(e.known.isEmpty()?"":" · "+e.known));b.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);b.setOnClickListener(v->select.accept(e));}
    }
    void tree(AnalysisSources.Entry source,List<AnalysisNode> path,List<AnalysisNode> children,int shown,Consumer<Integer> navigate,Consumer<AnalysisNode> select){
        files.setVisibility(View.GONE);map.setVisibility(View.VISIBLE);trail.removeAllViews();blocks.removeAllViews();
        for(int i=0;i<path.size();i++){final int index=i;Button crumb=ui.button(trail,(i==0?"":"↳ ")+path.get(i).title);crumb.setOnClickListener(v->navigate.accept(index));}
        AnalysisNode node=path.get(path.size()-1);frameTitle.setText(node.title+"\n"+bytes(node.size));
        for(int i=0;i<Math.min(shown,children.size());i++){
            AnalysisNode child=children.get(i);Button b=ui.button(blocks,child.title+(child.expandable()?"  ›":"")+"\n"+bytes(child.size)+String.format(Locale.ITALY," · %.2f%% del blocco",node.size==0?0:100.0*child.size/node.size));
            b.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);b.setContentDescription(child.title+", "+child.size+" byte"+(child.expandable()?", esplora":""));b.setOnClickListener(v->select.accept(child));
        }
        more.setVisibility(children.size()>shown?View.VISIBLE:View.GONE);showDetails(node);
    }
    void showDetails(AnalysisNode node){
        description.setText(node.title+"\n"+node.description+"\n\nOffset: "+node.offset+" (0x"+Long.toHexString(node.offset).toUpperCase(Locale.ROOT)+")\nDimensione: "+bytes(node.size)+"\nFine esclusa: "+node.end());
        hexPanel.setVisibility(View.GONE);hex.setText("Mostra byte HEX");hex.setEnabled(node.size>0);
    }
    static String bytes(long size){return String.format(Locale.ITALY,"%,d byte",size);}
}
