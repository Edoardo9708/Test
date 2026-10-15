package it.edo.codecprobe;

import android.net.Uri;
import android.os.*;
import android.view.*;

/**
* One stable Bundle schema for screen recreation and pending Android pickers.
*/
final class HomeSavedState {

    static void restore(HomeController home, Bundle state, AppSettings defaults) {
        home.state.loadedDefault = defaults.defaultFolder;
        String savedFolder = home.state.loadedDefault != null ? home.state.loadedDefault : home.activity.getPreferences(0).getString("folder", null);
        if (savedFolder != null) home.state.folder = Uri.parse(savedFolder);
        if (state != null) {
            home.conversion.retry.pending=state.getParcelable("pendingRetry");
            if (state.getString("input") != null) home.state.input = Uri.parse(state.getString("input"));
            home.state.sourceName = state.getString("sourceName", "video");
            if (state.getStringArrayList("inputs") != null) {
                home.state.selectedInputs.addAll(state.getStringArrayList("inputs"));
                home.state.selectedNames.addAll(state.getStringArrayList("names"));
            }
            if (state.getString("folder") != null) home.state.folder = Uri.parse(state.getString("folder"));
            if (state.getStringArrayList("origins") != null) home.state.originFolders.addAll(state.getStringArrayList("origins"));
            home.state.selectedVideoIndex = state.getInt("selectedVideoIndex", 0);
            home.state.pendingOriginParent = state.getString("pendingOriginParent");
            home.state.pendingOriginSource = state.getString("pendingOriginSource");
            home.state.choosingOrigins = state.getBoolean("choosingOrigins", false);
            home.state.pendingFixedFolder = state.getString("pendingFixedFolder");
            home.state.pendingAuthorizationOrigin = state.getBoolean("pendingAuthorizationOrigin", false);
            home.state.pendingLog = state.getString("pendingLog");
            if (state.getString("result") != null) home.state.resultUri = Uri.parse(state.getString("result"));
        }
        if (home.state.selectedInputs.isEmpty() && home.state.input != null) {
            home.state.selectedInputs.add(home.state.input.toString());
            home.state.selectedNames.add(home.state.sourceName);
        }
        if (!home.state.selectedInputs.isEmpty()) {
            int restored = home.state.selectedInputs.indexOf(home.state.input == null ? "" : home.state.input.toString());
            home.state.selectedVideoIndex = restored >= 0 ? restored : Math.max(0, Math.min(home.state.selectedVideoIndex, home.state.selectedInputs.size() - 1));
            home.state.input = Uri.parse(home.state.selectedInputs.get(home.state.selectedVideoIndex));
            home.state.sourceName = home.state.selectedNames.get(home.state.selectedVideoIndex);
        }
        home.state.destinationPlan.reset(home.state.selectedInputs.size(), savedFolder);
        if (state != null) home.state.destinationPlan.restore(state.getBooleanArray("individualOrigins"), state.getStringArrayList("individualFolders"), state.getBoolean("applyAllDestinations"), state.getBoolean("allOrigin", true), state.getString("allFolder", ""));
    }

    static void save(HomeController home, Bundle b) {
        home.screen.sections.save(b);
        if(home.conversion.retry.pending!=null)b.putParcelable("pendingRetry",home.conversion.retry.pending);
        if (home.state.input != null) b.putString("input", home.state.input.toString());
        if (home.state.folder != null) b.putString("folder", home.state.folder.toString());
        b.putStringArrayList("inputs", new java.util.ArrayList<>(home.state.selectedInputs));
        b.putStringArrayList("names", new java.util.ArrayList<>(home.state.selectedNames));
        b.putStringArrayList("origins", new java.util.ArrayList<>(home.state.originFolders));
        b.putBooleanArray("individualOrigins", home.state.destinationPlan.individualOrigins());
        b.putStringArrayList("individualFolders", home.state.destinationPlan.individualFolders());
        b.putBoolean("applyAllDestinations", home.state.destinationPlan.applyAll);
        b.putBoolean("allOrigin", home.state.destinationPlan.all.origin);
        b.putString("allFolder", home.state.destinationPlan.all.folder);
        b.putInt("selectedVideoIndex", home.state.selectedVideoIndex);
        b.putString("pendingOriginParent", home.state.pendingOriginParent);
        b.putString("pendingOriginSource", home.state.pendingOriginSource);
        b.putBoolean("choosingOrigins", home.state.choosingOrigins);
        b.putString("pendingFixedFolder", home.state.pendingFixedFolder);
        b.putBoolean("pendingAuthorizationOrigin", home.state.pendingAuthorizationOrigin);
        if (home.state.resultUri != null) b.putString("result", home.state.resultUri.toString());
        b.putString("sourceName", home.state.sourceName);
        b.putString("pendingLog", home.state.pendingLog);
        home.compression.save(b);
        b.putBoolean("logExpanded", home.screen.diagnostics.logView.getVisibility() == View.VISIBLE);
        b.putBoolean("videoDetailsExpanded", home.screen.files.videoDetails.getVisibility() == View.VISIBLE);
    }
}
