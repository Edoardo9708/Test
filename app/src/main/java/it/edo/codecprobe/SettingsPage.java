package it.edo.codecprobe;

import android.graphics.Typeface;
import android.widget.*;

/** Common settings frame. Headings never insert a separator into an empty card. */
final class SettingsPage {
    final AppUi ui;
    final LinearLayout page;
    final Button back;
    SettingsPage(AppUi ui,String title,String subtitle){
        this.ui=ui;page=ui.newPage();ui.text(page,title,28).setTypeface(null,Typeface.BOLD);ui.text(page,subtitle,13);
        back=ui.button(page,"← Torna alla conversione");back.setOnClickListener(v->ui.activity.finish());
    }
    LinearLayout section(String title){
        LinearLayout section=ui.panel(page);TextView heading=ui.text(section,title,16);heading.setTypeface(null,Typeface.BOLD);heading.setAccessibilityHeading(true);return section;
    }
    Actions actions(){return new Actions(ui,page);}
    static CheckBox check(AppUi ui,LinearLayout parent,String label,boolean value){
        CheckBox check=new CheckBox(ui.activity);check.setText(label);check.setTextColor(ui.ink);check.setTextSize(17);check.setMinHeight(ui.dp(48));check.setButtonTintList(android.content.res.ColorStateList.valueOf(ui.accent));check.setChecked(value);parent.addView(check);return check;
    }
    static final class Actions {
        final Button copy,save;
        Actions(AppUi ui,LinearLayout page){ui.divider(page);copy=ui.button(page,"Usa la configurazione della schermata conversione");save=ui.button(page,"Salva impostazioni");ui.styleButton(save,true);}
    }
}
