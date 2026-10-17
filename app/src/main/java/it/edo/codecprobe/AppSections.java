package it.edo.codecprobe;

import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;

/**
* Accordion state is independent of Android Activity lifecycle and business actions.
*/
class AppSections {
    private final AppUi ui;
    final java.util.LinkedHashMap<String, LinearLayout> bodies = new java.util.LinkedHashMap<>();
    final java.util.LinkedHashMap<String, Button> headers = new java.util.LinkedHashMap<>();
    private final Bundle restoredCardExpansion;
    private final String initialFocus;
    private final java.util.function.Function<String,String> descriptions;
    boolean closed;
    final java.util.Map<String, TextView> notes = new java.util.LinkedHashMap<>();

    AppSections(AppUi ui, Bundle saved, String initialFocus, java.util.function.Function<String,String> descriptions) {
        this.ui = ui;
        restoredCardExpansion = saved == null ? null : saved.getBundle("cardExpansion");
        this.initialFocus = saved == null ? initialFocus : null;
        this.descriptions = descriptions;
    }

    void save(Bundle out) {
        Bundle expansion = new Bundle();
        for (java.util.Map.Entry<String, LinearLayout> e : bodies.entrySet()) expansion.putBoolean(e.getKey(), e.getValue().getVisibility() == View.VISIBLE);
        out.putBundle("cardExpansion", expansion);
    }

    LinearLayout card(LinearLayout page, String title) {
        LinearLayout wrapper = new LinearLayout(ui.activity);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(ui.dp(18), 0, ui.dp(18), 0);
        wrapper.setBackground(ui.shape(ui.surface, 22));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, ui.dp(16), 0, 0);
        page.addView(wrapper, lp);
        Button header = new Button(ui.activity);
        ui.styleButton(header, false);
        header.setTextSize(15);
        header.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        header.setPadding(0, ui.dp(12), 0, ui.dp(12));
        header.setMinHeight(ui.dp(56));
        header.setSingleLine(false);
        header.setAccessibilityHeading(true);
        header.setBackground(new RippleDrawable(ColorStateList.valueOf(ui.border), null, null));
        wrapper.addView(header, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout body = new LinearLayout(ui.activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, ui.dp(4), 0, ui.dp(18));
        wrapper.addView(body, new LinearLayout.LayoutParams(-1, -2));
        TextView note = ui.text(wrapper, descriptions.apply(title), 13);
        note.setPadding(0, 0, 0, ui.dp(12));
        notes.put(title, note);
        bodies.put(title, body);
        headers.put(title, header);
        boolean initial = initialFocus != null ? title.equals(initialFocus) : bodies.size() == 1;
        setCardExpanded(title, restoredCardExpansion != null ? restoredCardExpansion.getBoolean(title, initial) : initial);
        header.setOnClickListener((v)->setCardExpanded(title, body.getVisibility() != View.VISIBLE));
        return body;
    }

    void setCardExpanded(String title, boolean expanded) {
        LinearLayout body = bodies.get(title);
        Button header = headers.get(title);
        if (body == null || header == null) return;
        if (notes.get(title) != null) notes.get(title).setVisibility(expanded ? View.GONE : View.VISIBLE);
        body.setVisibility(expanded ? View.VISIBLE : View.GONE);
        header.setText((expanded ? "▾  " : "▸  ") + title);
        header.setContentDescription(title + " · " + (expanded ? "aperto, tocca per richiudere" : "chiuso, tocca per espandere"));
        if (Build.VERSION.SDK_INT >= 30) header.setStateDescription(expanded ? "Aperto" : "Chiuso");
    }

    void focusCard(String title) {
        for (String key : bodies.keySet()) setCardExpanded(key, key.equals(title));
        Button header = headers.get(title);
        if (header != null) header.post(()->{
            if (!closed) header.requestRectangleOnScreen(new android.graphics.Rect(0, 0, header.getWidth(), header.getHeight()), false);
        });
    }

}
