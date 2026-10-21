package it.edo.codecprobe;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.IntConsumer;

/** Shared selection carousel and preview frame; metadata readers remain media-specific. */
final class SelectedItemView {
    final TextView number, hint, name, summary;
    final HorizontalScrollView carousel;
    final LinearLayout navigator, preview, detailsBody, metadataSummary, metadataDetails;
    private Map<String,String> lastSummary=Collections.emptyMap(),lastDetails=Collections.emptyMap();
    final Button details,remove;
    private final String detailsLabel;
    final ArrayList<Button> chips = new ArrayList<>();
    private final AppUi ui;
    private final String item, empty;
    private List<String> names = Collections.emptyList();
    private List<Boolean> errors = Collections.emptyList();
    private IntConsumer selected;
    private int current, transition;
    private boolean closed;
    private int renderedLimit=Integer.MAX_VALUE;

    SelectedItemView(AppUi ui, LinearLayout parent, String item, String empty, String detailsLabel) {
        this.ui = ui; this.item = item; this.empty = empty; this.detailsLabel = detailsLabel;
        number = ui.text(parent, empty, 15);
        carousel = new HorizontalScrollView(ui.activity);
        carousel.setHorizontalScrollBarEnabled(true);
        navigator = new LinearLayout(ui.activity);
        navigator.setGravity(Gravity.CENTER_VERTICAL);
        carousel.addView(navigator, new HorizontalScrollView.LayoutParams(-2, -2));
        parent.addView(carousel, new LinearLayout.LayoutParams(-1, -2));
        hint = ui.text(parent, "Scorri la barra e tocca un elemento per mostrarne i dettagli.", 13);
        preview = new LinearLayout(ui.activity);
        preview.setOrientation(LinearLayout.VERTICAL);
        parent.addView(preview, new LinearLayout.LayoutParams(-1, -2));
        name = ui.text(preview, empty, 23);
        name.setTypeface(null, Typeface.BOLD);
        name.setMaxLines(3);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        summary = ui.text(preview, "", 15);
        summary.setLineSpacing(ui.dp(3), 1);
        details = ui.button(preview, "▸ " + detailsLabel);
        detailsBody = new LinearLayout(ui.activity);
        detailsBody.setOrientation(LinearLayout.VERTICAL);
        preview.addView(detailsBody, new LinearLayout.LayoutParams(-1, -2));
        metadataSummary=new LinearLayout(ui.activity);metadataSummary.setOrientation(LinearLayout.VERTICAL);detailsBody.addView(metadataSummary);
        metadataDetails=new LinearLayout(ui.activity);metadataDetails.setOrientation(LinearLayout.VERTICAL);detailsBody.addView(metadataDetails);
        remove=ui.button(preview,"Rimuovi "+item.toLowerCase(java.util.Locale.ITALY));
        setExpanded(false);
        details.setOnClickListener(v -> setExpanded(detailsBody.getVisibility() != View.VISIBLE));
    }

    void render(List<String> names, List<Boolean> errors, int current, boolean enabled, IntConsumer selected) {
        render(names,errors,current,enabled,selected,Integer.MAX_VALUE);
    }
    void render(List<String> names,List<Boolean> errors,int current,boolean enabled,IntConsumer selected,int limit){
        this.selected=selected;
        if (!this.names.equals(names) || !this.errors.equals(errors) || renderedLimit!=limit) {
            renderedLimit=limit;
            this.names = new ArrayList<>(names); this.errors = new ArrayList<>(errors);
            navigator.removeAllViews(); chips.clear();
            for (int n = 0; n < Math.min(names.size(),limit); n++) {
                final int index = n;
                String label = names.get(n);
                int length = label.codePointCount(0, label.length());
                String shortName = length > 10 ? label.substring(0, label.offsetByCodePoints(0, 10)) + "…" : label;
                boolean error = n < errors.size() && errors.get(n);
                Button chip = new Button(ui.activity); ui.styleButton(chip, false);
                chip.setSingleLine(true); chip.setTextSize(14);
                chip.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
                chip.setText((n + 1) + " · " + shortName + (error ? " !" : ""));
                chip.setContentDescription(item + " " + (n + 1) + " di " + names.size() + ": " + label + (error ? ", lettura incompleta" : ""));
                chip.setOnClickListener(v -> this.selected.accept(index));
                LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-2, -2);
                layout.setMargins(0, ui.dp(6), ui.dp(8), ui.dp(6));
                navigator.addView(chip, layout); chips.add(chip);
            }
            if(names.size()>limit){TextView more=ui.text(navigator,"…",23);more.setPadding(ui.dp(12),0,ui.dp(12),0);more.setContentDescription("Altre "+(names.size()-limit)+" foto accodate; visualizzate solo le prime "+limit);}
        }
        update(current, enabled, false);
    }

    void update(int current, boolean enabled, boolean reveal) {
        this.current = current;
        number.setText(names.isEmpty() ? empty : item + " " + (current + 1) + " di " + names.size());
        number.setVisibility(names.isEmpty() ? View.GONE : View.VISIBLE);
        carousel.setVisibility(names.isEmpty() ? View.GONE : View.VISIBLE);
        details.setVisibility(names.isEmpty() ? View.GONE : View.VISIBLE);remove.setVisibility(names.isEmpty()?View.GONE:View.VISIBLE);remove.setEnabled(enabled);
        hint.setVisibility(names.size() > 1 ? View.VISIBLE : View.GONE);
        hint.setText(names.size()>renderedLimit?"Mostrate le prime "+renderedLimit+" foto; tutte le "+names.size()+" foto sono in coda.":"Scorri la barra e tocca un elemento per mostrarne i dettagli.");
        for (int n = 0; n < chips.size(); n++) {
            Button chip = chips.get(n); boolean active = n == current;
            chip.setEnabled(enabled); chip.setSelected(active);
            chip.setTextColor(active ? ui.bg : ui.accent);
            chip.setBackground(new RippleDrawable(ColorStateList.valueOf(ui.border), ui.shape(active ? ui.accent : ui.surface, 14), null));
        }
        if (reveal) carousel.post(() -> {
            if (closed || this.current < 0 || this.current >= chips.size()) return;
            View chip = chips.get(this.current);
            int left = carousel.getScrollX(), right = left + carousel.getWidth();
            if (chip.getLeft() < left) carousel.smoothScrollTo(chip.getLeft(), 0);
            else if (chip.getRight() > right) carousel.smoothScrollTo(chip.getRight() - carousel.getWidth(), 0);
        });
    }

    void showMetadata(Map<String,String> summary,Map<String,String> details) {
        if(lastSummary.equals(summary)&&lastDetails.equals(details))return;
        lastSummary=new LinkedHashMap<>(summary);lastDetails=new LinkedHashMap<>(details);
        metadataSummary.removeAllViews();metadataDetails.removeAllViews();
        ui.renderInfoSections(metadataSummary,summary);ui.renderInfoSections(metadataDetails,details);
    }

    void setExpanded(boolean expanded) {
        detailsBody.setVisibility(expanded ? View.VISIBLE : View.GONE);
        details.setText((expanded ? "▾ " : "▸ ") + detailsLabel);
    }

    void transition(Runnable apply) {
        final int generation = ++transition;
        preview.animate().cancel();
        preview.animate().alpha(0).setDuration(90).withEndAction(() -> {
            if (closed || generation != transition) return;
            apply.run();
            preview.animate().alpha(1).setDuration(140).withEndAction(null).start();
        }).start();
    }

    void cancelTransition() { ++transition; preview.animate().cancel(); preview.setAlpha(1); }
    void close() { closed = true; cancelTransition(); }
}
