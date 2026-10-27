package it.edo.codecprobe

import androidx.compose.runtime.Immutable

/** Bounded presentation snapshot. No photo bytes, provider calls or codec handles. */
@Immutable
internal data class PhotoItem(val uri: String, val name: String, val selected: Boolean, val revision: Int)

/** One presentation snapshot whose identity, status, summary and rows all belong to the same URI. */
@Immutable
internal data class PhotoMetadataUi(
    val uri: String,
    val name: String,
    val summary: String,
    val loading: Boolean,
    val analyzing: Boolean,
    val status: String,
    val rows: List<Pair<String, String>>,
    val error: String
)

@Immutable
internal data class PhotoUiState(
    val step: String?,
    val busy: Boolean,
    val total: Int,
    val items: List<PhotoItem>,
    val metadata: PhotoMetadataUi,
    val notice: String,
    val detailsEnabled: Boolean,
    val detailsOpen: Boolean,
    val reading: Boolean,
    val selectedNames: List<String>,
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
    val tune: String,
    val nameAddition: String,
    val namePrefix: Boolean,
    val permissions: OriginDirectoryAccess.State,
    val selectionCount: Int
) {
    // Compatibility accessors for focused UI assertions; values are sourced from one snapshot.
    val name get() = metadata.name
    val summary get() = metadata.summary
    val currentUri get() = metadata.uri
    val previewLoading get() = metadata.loading
    val detailRows get() = metadata.rows
    val detailError get() = metadata.error
}

internal enum class PhotoCommand {
    NEW, RESUME, SETTINGS, ADD_FILES, ADD_FOLDER, CLEAR, REMOVE, REREAD, DETAILS, END_SELECTION,
    CURRENT, ALL, ORIGIN, DESTINATION, CHOOSE_FOLDER, BACK, NEXT, VIDEO, AUDIO, ANALYSIS
}
