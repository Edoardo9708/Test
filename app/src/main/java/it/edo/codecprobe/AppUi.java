package it.edo.codecprobe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.*;
import android.widget.*;
import java.util.Map;

/**
* Shared visual language. No selection, permission, conversion or persistence logic.
*/
final class AppUi {
    final Activity activity;
    final boolean dark;
    final int bg;
    final int surface;
    final int ink;
    final int muted;
    final int accent;
    final int border;

    final boolean photos;
    PhotoBackdrop backdrop;
    AppUi(Activity activity) { this(activity,false); }
    AppUi(Activity activity,boolean photos) {
        this.photos=photos;
        this.activity = activity;
        dark = (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        bg=photos?(dark?0xFF151820:0xFFF8F8FB):activity.getColor(R.color.ui_background);
        surface=photos?(dark?0xFF232D42:0xFFE8EEFC):activity.getColor(R.color.ui_surface);
        ink=photos?(dark?0xFFE7ECF7:0xFF292C32):activity.getColor(R.color.ui_ink);
        muted=photos?(dark?0xFFB5BFD1:0xFF565B66):activity.getColor(R.color.ui_muted);
        accent=photos?(dark?0xFFA8C5FF:0xFF3977E9):activity.getColor(R.color.ui_accent);
        border=photos?(dark?0xFF465064:0xFFC8CBD3):activity.getColor(R.color.ui_border);
    }

    int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    GradientDrawable shape(int fill, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), border);
        return d;
    }

    LinearLayout newPage() {
        LinearLayout page = new Page(activity, dp(760));
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(12), dp(16), dp(24));
        return page;
    }

    void install(LinearLayout page) { install(page,null); }

    void install(LinearLayout page, View navigation) {
        if(navigation!=null){
            LinearLayout.LayoutParams tabs=new LinearLayout.LayoutParams(-1,-2);tabs.setMargins(0,0,0,dp(14));
            page.addView(navigation,0,tabs);
        }
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);
        FrameLayout frame = new FrameLayout(activity);
        frame.setBackgroundColor(bg);
        scroll.addView(frame, new ScrollView.LayoutParams(-1, -2));
        frame.addView(page, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        View insetHost=frame;
        View content=scroll;
        insetHost.setOnApplyWindowInsetsListener((view,insets)->{
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets b = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(b.left, b.top, b.right, b.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        activity.setContentView(content);
        insetHost.requestApplyInsets();
        applySystemBars();
    }

    void applySystemBars() {
        activity.getWindow().setStatusBarColor(bg);
        activity.getWindow().setNavigationBarColor(bg);
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = activity.getWindow().getInsetsController();
            int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            if (controller != null) controller.setSystemBarsAppearance(dark ? 0 : mask, mask);
        } else activity.getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    TextView text(LinearLayout parent, String value, int size) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(size >= 16 ? ink : muted);
        view.setPadding(0, dp(6), 0, dp(6));
        view.setIncludeFontPadding(true);
        parent.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }

    void styleButton(Button button, boolean primary) {
        button.setStateListAnimator(null); button.setElevation(0); button.setTranslationZ(0);
        button.setAllCaps(false);
        button.setTextSize(photos?13:15);
        button.setTypeface(null, photos?Typeface.NORMAL:Typeface.BOLD);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(52));
        button.setMinimumHeight(dp(52));
        button.setSingleLine(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setIncludeFontPadding(true);
        button.setTextColor(new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {android.R.attr.state_selected}, {}}, new int[]{muted, bg, primary ? bg : accent}));
        StateListDrawable fills = new StateListDrawable();
        fills.addState(new int[]{-android.R.attr.state_enabled}, shape(bg, 14));
        fills.addState(new int[]{android.R.attr.state_selected}, shape(accent, 14));
        fills.addState(new int[]{}, shape(primary ? accent : surface, 14));
        button.setBackgroundTintList(null);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(border), fills, null));
    }

    Button button(LinearLayout parent, String title) {
        Button b = new Button(activity);
        b.setText(title);
        styleButton(b, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(8), 0, dp(6));
        parent.addView(b, lp);
        return b;
    }

    Button halfButton(LinearLayout row, String title, boolean primary) {
        Button b = new Button(activity);
        b.setText(title);
        styleButton(b, primary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(dp(3), dp(6), dp(3), dp(6));
        row.addView(b, lp);
        return b;
    }

    void danger(Button b) {
        int color = Color.parseColor(dark ? "#FFB4AB" : "#AD2921");
        b.setTextColor(new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}}, new int[]{muted, color}));
    }

    void styleSlider(SeekBar slider) {
        slider.setProgressTintList(ColorStateList.valueOf(accent));
        slider.setThumbTintList(ColorStateList.valueOf(accent));
        slider.setProgressBackgroundTintList(ColorStateList.valueOf(border));
        slider.setMinHeight(dp(48));
    }

    LinearLayout row(LinearLayout parent) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    LinearLayout panel(LinearLayout parent) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(10), dp(12), dp(10));
        panel.setBackground(shape(bg, 14));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(10), 0, dp(6));
        parent.addView(panel, p);
        return panel;
    }

    TextView parameterRow(LinearLayout parent, String title, String help) {
        LinearLayout row = row(parent);
        TextView label = text(row, title, 18);
        label.setTypeface(null, Typeface.BOLD);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        infoButton(row, title, help);
        return label;
    }

    void switchRow(LinearLayout parent, Switch toggle, String title, String help) {
        LinearLayout row = row(parent);
        toggle.setTextSize(17);
        toggle.setMinHeight(dp(56));
        styleSwitch(toggle);
        row.addView(toggle, new LinearLayout.LayoutParams(0, -2, 1));
        infoButton(row, title, help);
    }

    void styleSwitch(Switch toggle) {
        int[][] states = {{-android.R.attr.state_enabled}, {android.R.attr.state_checked}, {}};
        toggle.setThumbTintList(new ColorStateList(states, new int[]{border, accent, muted}));
        toggle.setTrackTintList(new ColorStateList(states, new int[]{border, android.graphics.Color.argb(110, Color.red(accent), Color.green(accent), Color.blue(accent)), border}));
        toggle.setTextColor(new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}}, new int[]{muted, ink}));
    }

    void infoButton(LinearLayout row, String title, String help) {
        Button info = new Button(activity);
        info.setText("i");
        styleButton(info, false);
        info.setTextSize(17);
        info.setMinHeight(dp(48));
        info.setMinimumHeight(dp(48));
        info.setPadding(0, 0, 0, 0);
        info.setContentDescription("Spiegazione: " + title);
        info.setBackground(new RippleDrawable(ColorStateList.valueOf(border), null, null));
        row.addView(info, new LinearLayout.LayoutParams(dp(48), dp(48)));
        info.setOnClickListener((v)->new AlertDialog.Builder(activity).setTitle(title).setMessage(help).setPositiveButton("OK", null).show());
    }

    void divider(LinearLayout parent) {
        View line = new View(activity);
        line.setBackgroundColor(border);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1));
        p.setMargins(0, dp(18), 0, dp(12));
        parent.addView(line, p);
    }

    CheckBox destinationCheck(LinearLayout parent, String title) {
        CheckBox b = photos?new PhotoSwitch(this):new CheckBox(activity);
        b.setText(title);
        b.setTextSize(photos?13:16);
        b.setTextColor(ink);
        b.setButtonTintList(new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}}, new int[]{border, accent}));
        b.setTextColor(new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}}, new int[]{muted, ink}));
        b.setMinHeight(dp(48));
        parent.addView(b, new LinearLayout.LayoutParams(-1, -2));
        return b;
    }

    void sectionHeading(LinearLayout parent, String title) {
        divider(parent);
        TextView label = text(parent, title, 12);
        label.setTextColor(accent);
        label.setLetterSpacing(0.06F);
        label.setTypeface(null, Typeface.BOLD);
        label.setAccessibilityHeading(true);
    }

    TextView dashboardRow(LinearLayout parent, String title, String initial) {
        LinearLayout row = row(parent);
        row.setGravity(Gravity.TOP);
        row.setPadding(0, dp(3), 0, dp(3));
        TextView label = text(row, title, 14);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.15F));
        label.setPadding(0, dp(5), dp(8), dp(5));
        TextView value = text(row, initial, 16);
        value.setTypeface(null, Typeface.BOLD);
        value.setGravity(Gravity.END);
        value.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        return value;
    }

    TextView metric(LinearLayout row, String title) {
        LinearLayout tile = new LinearLayout(activity);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(8), dp(8), dp(8), dp(8));
        tile.setBackground(shape(bg, 12));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        p.setMargins(dp(2), dp(8), dp(2), dp(8));
        row.addView(tile, p);
        TextView label = text(tile, title, 12);
        label.setGravity(Gravity.CENTER);
        TextView value = text(tile, "—", 17);
        value.setTypeface(null, Typeface.BOLD);
        value.setGravity(Gravity.CENTER);
        return value;
    }

    void renderInfoSections(LinearLayout parent, Map<String, String> sections) {
        parent.removeAllViews();
        for (Map.Entry<String, String> section : sections.entrySet()) {
            LinearLayout group = photos?plainGroup(parent):panel(parent);
            TextView title = text(group, section.getKey(), 13);
            title.setTextColor(accent);
            title.setTypeface(null, Typeface.BOLD);
            TextView body = text(group, section.getValue(), 15);
            body.setTextColor(ink);
            body.setLineSpacing(dp(3), 1);
            body.setTextIsSelectable(true);
        }
    }

    private LinearLayout plainGroup(LinearLayout parent){
        LinearLayout row=new LinearLayout(activity);row.setOrientation(LinearLayout.VERTICAL);parent.addView(row,new LinearLayout.LayoutParams(-1,-2));return row;
    }

    private static final class Page extends LinearLayout {
        private final int maxWidth;

        Page(Activity activity, int maxWidth) {
            super(activity);
            this.maxWidth = maxWidth;
        }

        @Override
        protected void onMeasure(int width, int height) {
            int available = MeasureSpec.getSize(width);
            if (available > maxWidth) width = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.getMode(width));
            super.onMeasure(width, height);
        }
    }
}
