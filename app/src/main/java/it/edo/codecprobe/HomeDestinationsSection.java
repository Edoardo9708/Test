package it.edo.codecprobe;

import android.os.Bundle;
import android.widget.LinearLayout;

/** Video adapter; construction belongs to the common destination component. */
final class HomeDestinationsSection extends MediaDestinationsView {
    HomeDestinationsSection(AppUi ui, LinearLayout parent, AppSettings defaults, Bundle state) {
        super(ui, parent, MediaKind.VIDEO);
    }
}
