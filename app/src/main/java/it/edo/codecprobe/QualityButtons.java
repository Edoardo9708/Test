package it.edo.codecprobe;

import android.widget.Button;
import android.widget.LinearLayout;
import android.view.Gravity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
* The same three quality choices in Home and Settings. Numeric values have one owner.
*/
final class QualityButtons extends LinearLayout {
    private static final int[] DEFAULT_VALUES = {18, 23, 28};
    private final int[] values;
    private static final String[] LABELS = {"Alta", "Media", "Ridotta"};
    final List<Button> buttons = new ArrayList<>();

    QualityButtons(AppUi ui, IntConsumer selected) {
        this(ui,DEFAULT_VALUES,LABELS,"CRF",selected);
    }
    QualityButtons(AppUi ui,int[] values,String[] labels,String unit,IntConsumer selected) {
        super(ui.activity);if(values.length!=labels.length)throw new IllegalArgumentException("Scelte qualità incomplete");this.values=values.clone();
        setGravity(Gravity.CENTER);
        for (int i = 0; i < values.length; i++) {
            final int value = values[i];
            Button b = new Button(ui.activity);
            b.setText(labels[i]);
            b.setContentDescription(labels[i] + ", "+unit+" " + value);
            ui.styleButton(b, false);
            b.setPadding(ui.dp(4), ui.dp(12), ui.dp(4), ui.dp(12));
            b.setOnClickListener((v)->selected.accept(value));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
            p.setMargins(ui.dp(3), 0, ui.dp(3), 0);
            addView(b, p);
            buttons.add(b);
        }
    }

    void selectFromCrf(int crf) {
        for (int i = 0; i < buttons.size(); i++) buttons.get(i).setSelected(crf == values[i]);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (buttons != null) for (Button b : buttons) b.setEnabled(enabled);
    }
}
