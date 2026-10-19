package it.edo.codecprobe;

import android.graphics.Typeface;
import android.widget.*;

/** Shared report presentation. Media presenters own the data and actions. */
final class WorkReportView {
    final LinearLayout panel, rows;
    final TextView title, summary;

    WorkReportView(AppUi ui, LinearLayout parent) {
        panel = new LinearLayout(ui.activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
        panel.setBackground(ui.shape(ui.bg, 14));
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.setMargins(0, ui.dp(14), 0, 0);
        parent.addView(panel, layout);
        title = ui.text(panel, "REPORT LAVORI", 16);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(ui.accent);
        summary = body(ui, panel, "");
        rows = new LinearLayout(ui.activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        panel.addView(rows, new LinearLayout.LayoutParams(-1, -2));
    }

    static TextView body(AppUi ui, LinearLayout parent, String text) {
        TextView body = ui.text(parent, text, 16);
        body.setTextIsSelectable(true);
        body.setLineSpacing(ui.dp(4), 1);
        return body;
    }

    static LinearLayout row(AppUi ui, LinearLayout parent) {
        LinearLayout row = ui.row(parent);
        row.setPadding(0, ui.dp(4), 0, ui.dp(8));
        return row;
    }

    static Button action(AppUi ui, LinearLayout row, String label, String description, Runnable clicked) {
        Button button = new Button(ui.activity);
        ui.styleButton(button, false);
        button.setText(label);
        button.setContentDescription(description);
        row.addView(button, new LinearLayout.LayoutParams(ui.dp(92), -2));
        button.setOnClickListener(v -> clicked.run());
        return button;
    }
}
