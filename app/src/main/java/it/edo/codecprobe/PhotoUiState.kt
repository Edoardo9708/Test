package it.edo.codecprobe

import androidx.compose.runtime.Immutable

/** Bounded presentation snapshot. No photo bytes, provider calls or codec handles. */
@Immutable
internal data class PhotoItem(val uri: String, val name: String, val selected: Boolean, val revision: Int)

@Immutable
internal data class PhotoUiState(
    val step: String?,
    val busy: Boolean,
    val total: Int,
    val items: List<PhotoItem>,
    val name: String,
    val summary: String,
    val notice: String,
    val detailsEnabled: Boolean,
    val detailsOpen: Boolean,
    val basicDetails: String,
    val allDetails: String,
    val previousEnabled: Boolean,
    val previousHint: String,
    val general: Boolean,
    val currentLabel: String,
    val allLabel: String,
    val allocation: String,
    val exception: Boolean,
    val exceptionLabel: String,
    val editorVisible: Boolean,
    val origin: Boolean,
    val destination: Boolean,
    val folder: String,
    val chooseVisible: Boolean,
    val smaller: Boolean,
    val quality: Int,
    val preset: String,
    val tune: String
)

internal enum class PhotoCommand {
    NEW, RESUME, SETTINGS, ADD_FILES, ADD_FOLDER, CLEAR, REMOVE, REREAD, DETAILS,
    CURRENT, ALL, ORIGIN, DESTINATION, CHOOSE_FOLDER, BACK, NEXT, VIDEO, AUDIO, ANALYSIS
}
