package it.edo.codecprobe;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;
import static it.edo.codecprobe.MainActivity.*;

/**
* Composes independent sections; no file access or conversion actions.
*/
final class HomeScreen {
    final HomeSections sections;
    final HomeFilesSection files;
    final HomeDestinationsSection destinations;
    final HomeCompressionSection compression;
    final HomeConversionSection conversion;
    final HomeDiagnosticsSection diagnostics;
    final Button preferences;
    final LinearLayout page;

    HomeScreen(AppUi ui, AppSettings defaults, Bundle saved, boolean active) {
        Activity activity = ui.activity;
        page = ui.newPage();
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(header, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout title = new LinearLayout(activity);
        title.setOrientation(LinearLayout.VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        ui.text(title, "Converti video", 27).setTypeface(null, Typeface.BOLD);
        ui.text(title, "HEVC · x265 · v" + BuildConfig.VERSION_NAME, 13);
        preferences = new Button(activity);
        preferences.setText("Impostazioni");
        ui.styleButton(preferences, false);
        preferences.setTextSize(13);
        preferences.setPadding(ui.dp(10), ui.dp(8), ui.dp(10), ui.dp(8));
        header.addView(preferences, new LinearLayout.LayoutParams(-2, -2));
        sections = new HomeSections(ui, saved, active);
        LinearLayout fileBody = sections.card(page, "01  FILE E DESTINAZIONE");
        files = new HomeFilesSection(ui, fileBody, defaults, saved);
        destinations = new HomeDestinationsSection(ui, fileBody, defaults, saved);
        compression = new HomeCompressionSection(ui, sections.card(page, "02  COMPRESSIONE"), defaults, saved);
        conversion = new HomeConversionSection(ui, sections.card(page, CARD_CONVERSION), defaults, saved);
        diagnostics = new HomeDiagnosticsSection(ui, sections.card(page, "DIAGNOSTICA"), defaults, saved);
        ui.install(page);
    }
}
