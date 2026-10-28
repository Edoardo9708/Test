package it.edo.codecprobe;

import android.graphics.Typeface;
import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.widget.*;
import java.util.*;
import java.util.function.Consumer;

/** Views only: readable block sizes, physical byte ranges, drill-down and optional HEX. */
final class AnalysisScreen {
    final AppUi ui;
    final LinearLayout files,map,trail,blocks,details,hexPanel,anatomy;
    final LinearLayout fileFrame,currentFrame;
    final TextView contains,detailHeading;
    private final Map<AnalysisNode,Button> blockButtons=new IdentityHashMap<>();
    final TextView status,description,hexText,frameTitle,pageLabel;
    final Button back,reload,hex,previous,next,more,explain;
    AlertDialog popup;
    Runnable onDetailClosed;
    private AnalysisNode popupNode;
    AnalysisScreen(AppUi ui){
        this.ui=ui;LinearLayout page=ui.newPage();
        ui.text(page,"Analisi",27);ui.text(page,"Struttura dei file · v"+BuildConfig.VERSION_NAME,14);
        status=ui.text(page,"",15);files=ui.panel(page);map=ui.panel(page);map.setVisibility(View.GONE);
        LinearLayout actions=ui.row(map);back=ui.halfButton(actions,"Elenco file",false);reload=ui.halfButton(actions,"Rileggi file",false);
        trail=new LinearLayout(ui.activity);trail.setOrientation(LinearLayout.VERTICAL);map.addView(trail);
        fileFrame=ui.panel(map);fileFrame.setBackground(ui.shape(ui.surface,14));currentFrame=new LinearLayout(ui.activity);currentFrame.setOrientation(LinearLayout.VERTICAL);fileFrame.addView(currentFrame);
        frameTitle=ui.text(currentFrame,"",18);frameTitle.setTypeface(null,Typeface.BOLD);explain=ui.button(currentFrame,"Spiegazione di questo blocco");contains=ui.text(currentFrame,"",14);
        blocks=new LinearLayout(ui.activity);blocks.setOrientation(LinearLayout.VERTICAL);currentFrame.addView(blocks);more=ui.button(currentFrame,"Mostra altri elementi");
        details=ui.panel(map);detailHeading=ui.text(details,"ELEMENTO SELEZIONATO",12);detailHeading.setTextColor(ui.accent);description=ui.text(details,"",15);description.setTextIsSelectable(true);anatomy=new LinearLayout(ui.activity);anatomy.setOrientation(LinearLayout.VERTICAL);details.addView(anatomy);hex=ui.button(details,"Mostra byte HEX");
        hexPanel=ui.panel(details);hexPanel.setVisibility(View.GONE);ui.text(hexPanel,"Offset assoluti nel file · HEX e ASCII",13);
        HorizontalScrollView scroll=new HorizontalScrollView(ui.activity);hexText=new TextView(ui.activity);hexText.setTypeface(Typeface.MONOSPACE);hexText.setTextSize(12);hexText.setTextColor(ui.ink);hexText.setTextIsSelectable(true);scroll.addView(hexText);hexPanel.addView(scroll);
        pageLabel=ui.text(hexPanel,"",13);LinearLayout paging=ui.row(hexPanel);previous=ui.halfButton(paging,"← 256 byte",false);next=ui.halfButton(paging,"256 byte →",false);
        map.removeView(details);details.setVisibility(View.GONE);
        ui.install(page,new AppNavigation(ui,WorkspaceNavigator.Section.ANALYSIS));
    }
    void sources(List<AnalysisSources.Entry> entries,Consumer<AnalysisSources.Entry> select){
        files.removeAllViews();files.setVisibility(View.VISIBLE);map.setVisibility(View.GONE);
        ui.text(files,"FILE GIÀ SELEZIONATI",13);ui.text(files,"L’elenco riusa le selezioni. La struttura viene letta solo aprendo un file.",15);
        if(entries.isEmpty())ui.text(files,"Seleziona prima un file in Video, Audio o Immagini.",17);
        for(AnalysisSources.Entry e:entries){Button b=ui.button(files,e.name+"\n"+e.workspace+(e.known.isEmpty()?"":" · "+e.known));b.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);b.setOnClickListener(v->select.accept(e));}
    }
    void tree(AnalysisSources.Entry source,List<AnalysisNode> path,List<AnalysisNode> children,int shown,Consumer<Integer> navigate,Consumer<AnalysisNode> select){
        files.setVisibility(View.GONE);map.setVisibility(View.VISIBLE);trail.removeAllViews();blocks.removeAllViews();blockButtons.clear();
        AnalysisNode node=path.get(path.size()-1);AnalysisLabels.Label label=AnalysisLabels.of(node);
        if(path.size()>1){
            ui.text(trail,"PERCORSO NEL FILE",12);StringBuilder route=new StringBuilder("File completo");
            for(int i=1;i<path.size();i++)route.append(" › ").append(AnalysisLabels.of(path.get(i)).name);
            ui.text(trail,route.toString(),14);
            LinearLayout controls=ui.row(trail);Button up=ui.halfButton(controls,"← Livello superiore",false),root=ui.halfButton(controls,"File completo",false);
            up.setOnClickListener(v->navigate.accept(path.size()-2));root.setOnClickListener(v->navigate.accept(0));
        }
        // The file boundary stays visible while the selected container is shown inside it.
        if(currentFrame.getParent()!=null)((ViewGroup)currentFrame.getParent()).removeView(currentFrame);fileFrame.removeAllViews();
        LinearLayout host=fileFrame;
        if(path.size()>1){
            TextView file=ui.text(host,"FILE COMPLETO\n"+source.name+"\n"+bytes(path.get(0).size),16);file.setTypeface(null,Typeface.BOLD);
            if(path.size()>4)ui.text(host,"Percorso intermedio abbreviato nel disegno; il percorso completo è indicato sopra.",13);
            for(int i=Math.max(1,path.size()-3);i<path.size()-1;i++){
                AnalysisNode ancestor=path.get(i);ui.text(host,"CONTIENE",11);host=nested(host);
                ui.text(host,AnalysisLabels.of(ancestor).name+" · "+ancestor.title,15);
            }
            ui.text(host,"CONTIENE",11);host=nested(host);
        }
        host.addView(currentFrame);frameTitle.setText(path.size()==1?node.title+"\n"+bytes(node.size):label.name+"\n"+node.title+" · "+bytes(node.size));
        contains.setText(children.isEmpty()?(node.expandable()?"Nessun elemento interno trovato.":"Contenuto non suddiviso in questa versione."):"CONTIENE "+children.size()+" ELEMENTI");
        for(int i=0;i<Math.min(shown,children.size());i++){
            AnalysisNode child=children.get(i);AnalysisLabels.Label translated=AnalysisLabels.of(child);
            String size=bytes(child.size)+String.format(Locale.ITALY," · %.2f%% del contenitore",node.size==0?0:100.0*child.size/node.size);
            Button button=ui.button(blocks,"");button.setText(blockText(translated,child,size));button.setTypeface(null,Typeface.NORMAL);button.setTextColor(new android.content.res.ColorStateList(new int[][]{{android.R.attr.state_selected},{}},new int[]{ui.bg,ui.ink}));button.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);
            button.setContentDescription(translated.name+", "+child.title+", "+child.size+" byte"+(child.expandable()?", apri gli elementi interni":", mostra spiegazione"));
            button.setOnClickListener(v->select.accept(child));blockButtons.put(child,button);
        }
        more.setVisibility(children.size()>shown?View.VISIBLE:View.GONE);showDetails(node);
    }
    private LinearLayout nested(LinearLayout parent){LinearLayout panel=ui.panel(parent);panel.setPadding(ui.dp(6),ui.dp(4),ui.dp(6),ui.dp(4));panel.setBackground(ui.shape(ui.bg,12));return panel;}
    private CharSequence blockText(AnalysisLabels.Label label,AnalysisNode node,String size){
        SpannableStringBuilder text=new SpannableStringBuilder(label.name+(node.expandable()?"  ›":""));text.setSpan(new StyleSpan(Typeface.BOLD),0,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int secondary=text.length();text.append("\n").append(node.title).append(" · ").append(label.kind).append("\n").append(size);
        if(!label.meaning.isEmpty())text.append("\n").append(firstSentence(label.meaning));
        text.append(node.expandable()?"\nTocca per vedere cosa contiene":"\nTocca per la spiegazione e i byte");text.setSpan(new RelativeSizeSpan(0.88f),secondary,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);return text;
    }
    private static String firstSentence(String text){int end=text.indexOf(". ");return end<0?text:text.substring(0,end+1);}
    void showDetails(AnalysisNode node){
        AnalysisLabels.Label label=AnalysisLabels.of(node);for(Map.Entry<AnalysisNode,Button> e:blockButtons.entrySet())e.getValue().setSelected(e.getKey()==node);
        detailHeading.setText("ELEMENTO SELEZIONATO · "+label.kind.toUpperCase(Locale.ITALY));
        description.setText(label.name+(label.name.equals(node.title)?"":"\nNome tecnico: "+node.title)+(label.meaning.isEmpty()?"":"\n\nCOSA SIGNIFICA\n"+label.meaning)+"\n\nDATI LETTI DAL FILE\n"+AnalysisLabels.facts(node)+"\n\nDOVE SI TROVA\nInizia al byte "+node.offset+" del file (0x"+Long.toHexString(node.offset).toUpperCase(Locale.ROOT)+").\nOccupa "+bytes(node.size)+".\nIl primo byte dopo questo elemento è "+node.end()+".");
        hexPanel.setVisibility(View.GONE);hex.setText("Mostra byte HEX");hex.setEnabled(node.size>0);
    }
    void openPopup(AnalysisNode node){
        closePopup();popupNode=node;showDetails(node);anatomy.removeAllViews();description.setText("Leggo l’intestazione e i valori del campo…");hex.setEnabled(false);details.setVisibility(View.VISIBLE);
        ScrollView scroll=new ScrollView(ui.activity);scroll.addView(details,new ScrollView.LayoutParams(-1,-2));
        AlertDialog dialog=new AlertDialog.Builder(ui.activity).setTitle(AnalysisLabels.of(node).name).setView(scroll).setPositiveButton("Chiudi",null).create();
        popup=dialog;dialog.setOnDismissListener(d->{
            if(details.getParent()==scroll)scroll.removeView(details);
            if(popup==dialog){popup=null;popupNode=null;details.setVisibility(View.GONE);if(onDetailClosed!=null)onDetailClosed.run();}
        });
        dialog.show();android.util.DisplayMetrics metrics=ui.activity.getResources().getDisplayMetrics();
        dialog.getWindow().setLayout(Math.max(1,metrics.widthPixels-ui.dp(24)),Math.max(1,(int)(metrics.heightPixels*0.85f)));
    }
    void showDetail(AnalysisDetail detail){
        if(popup==null)return;description.setTextColor(ui.ink);description.setText((detail.preview().isEmpty()?"":"NEL TUO FILE\n"+detail.preview()+"\n\n")+detail.introduction);anatomy.removeAllViews();
        TextView heading=ui.text(anatomy,"COME SONO FATTI I BYTE",13);heading.setTypeface(null,Typeface.BOLD);heading.setTextColor(ui.accent);
        for(AnalysisDetail.Part part:detail.parts){
            LinearLayout panel=ui.panel(anatomy);TextView name=ui.text(panel,part.name,16);name.setTypeface(null,Typeface.BOLD);
            ui.text(panel,part.size+" byte · inizio al byte "+part.offset+" del file",13);
            TextView bytes=ui.text(panel,part.hex,14);bytes.setTypeface(Typeface.MONOSPACE);bytes.setTextIsSelectable(true);
            TextView meaning=ui.text(panel,part.meaning,15);meaning.setTextColor(ui.ink);meaning.setTextIsSelectable(true);
        }
        TextView result=ui.text(anatomy,detail.result,16);result.setTextIsSelectable(true);
        if(!detail.alternatives.isEmpty()){LinearLayout choices=ui.panel(anatomy);TextView values=ui.text(choices,detail.alternatives,16);values.setTextIsSelectable(true);}
        hex.setEnabled(popupNode!=null&&popupNode.size>0);
    }
    void detailError(String message){
        if(popup==null)return;hexText.setText("");pageLabel.setText(message);hexPanel.setVisibility(View.VISIBLE);previous.setEnabled(false);next.setEnabled(false);
    }
    void closePopup(){
        AlertDialog previous=popup;popup=null;popupNode=null;
        if(details.getParent()!=null)((ViewGroup)details.getParent()).removeView(details);
        details.setVisibility(View.GONE);if(previous!=null)previous.dismiss();
    }
    static String bytes(long size){return String.format(Locale.ITALY,"%,d byte",size);}
}
