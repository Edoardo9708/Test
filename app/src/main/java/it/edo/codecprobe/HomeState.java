package it.edo.codecprobe;

import android.net.Uri;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* Selection and pending-operation state. Views and workers have separate owners.
*/
final class HomeState {
    Uri input;
    Uri folder;
    Uri resultUri;
    String pendingOriginParent;
    String pendingOriginSource;
    String pendingFixedFolder;
    String loadedDefault;
    String pendingLog;
    boolean originsReady;
    boolean updatingDestinationUi;
    boolean pendingAuthorizationOrigin;
    boolean choosingOrigins;
    boolean clearingLogs;
    boolean exportingDiagnostics;
    boolean hdrTesting;
    boolean conversionScreenVisible;
    boolean notificationsRequested;
    volatile boolean destroyed;
    volatile boolean busy;
    volatile boolean metadataLoading;
    volatile int metadataGeneration;
    volatile int transitionGeneration;
    int selectedVideoIndex;
    long settingsRevision;
    String sourceName = "video";
    String selectionSession = "";
    final java.util.ArrayList<String> selectedInputs = new java.util.ArrayList<>();
    final java.util.ArrayList<String> selectedNames = new java.util.ArrayList<>();
    final java.util.ArrayList<String> originFolders = new java.util.ArrayList<>();
    final java.util.ArrayList<String> destinationNotes = new java.util.ArrayList<>();
    final DestinationPlan destinationPlan = new DestinationPlan();
    final AtomicBoolean cancelled = new AtomicBoolean();
}
