package it.edo.codecprobe;

import android.app.Activity;
import android.content.Intent;

/** Owns transitions between the app's real media workspaces. */
final class WorkspaceNavigator {
    enum Section { VIDEO, AUDIO, PHOTO, ANALYSIS }

    private WorkspaceNavigator() {}

    static void open(Activity source, Section destination) {
        Section current = sectionOf(source);
        if (current == null || current == destination) return;

        if (source instanceof MainActivity) {
            HomeState state = ((MainActivity) source).home.state;
            AnalysisSources.video(state.selectedInputs, state.selectedNames);
        }

        Class<? extends Activity> target = activityFor(destination);
        Intent intent = new Intent(source, target).addFlags(
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        source.startActivity(intent);

        // Video is the task root; secondary workspaces save through lifecycle and then leave the stack.
        if (current != Section.VIDEO) source.finish();
        AppNavigation.fade(source);
    }

    private static Section sectionOf(Activity activity) {
        if (activity instanceof MainActivity) return Section.VIDEO;
        if (activity instanceof AudioActivity) return Section.AUDIO;
        if (activity instanceof ImageActivity) return Section.PHOTO;
        if (activity instanceof AnalysisActivity) return Section.ANALYSIS;
        return null;
    }

    private static Class<? extends Activity> activityFor(Section section) {
        return switch (section) {
            case VIDEO -> MainActivity.class;
            case AUDIO -> AudioActivity.class;
            case PHOTO -> ImageActivity.class;
            case ANALYSIS -> AnalysisActivity.class;
        };
    }
}
