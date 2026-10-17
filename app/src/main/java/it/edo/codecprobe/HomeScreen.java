package it.edo.codecprobe;

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
    final AppNavigation navigation;

    HomeScreen(AppUi ui, AppSettings defaults, Bundle saved, boolean active) {
        page = ui.newPage();
        preferences=AppHeader.add(ui,page,"Converti video","HEVC · x265 · v"+BuildConfig.VERSION_NAME);
        sections = new HomeSections(ui, saved, active);
        LinearLayout fileBody = sections.card(page, "01  FILE E DESTINAZIONE");
        files = new HomeFilesSection(ui, fileBody, defaults, saved);
        destinations = new HomeDestinationsSection(ui, fileBody, defaults, saved);
        compression = new HomeCompressionSection(ui, sections.card(page, "02  COMPRESSIONE"), defaults, saved);
        conversion = new HomeConversionSection(ui, sections.card(page, CARD_CONVERSION), defaults, saved);
        diagnostics = new HomeDiagnosticsSection(ui, sections.card(page, "DIAGNOSTICA"), defaults, saved);
        navigation=new AppNavigation(ui,false);
        ui.install(page,navigation);
    }
}
