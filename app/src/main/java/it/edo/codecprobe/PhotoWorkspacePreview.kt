package it.edo.codecprobe

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import coil.ImageLoader

/** Static sample data: the preview renders the production workspace without an Activity or providers. */
private class PhotoWorkspacePreviewHost : PhotoWorkspaceHost {
    override val state = PhotoUiState(
        step = ImageScreen.FILES,
        busy = false,
        total = 4,
        items = listOf(
            PhotoItem("preview://photo/1", "IMG_20261026_1420.jpg", true, 0),
            PhotoItem("preview://photo/2", "vacanza.jpg", false, 0),
            PhotoItem("preview://photo/3", "famiglia.jpg", false, 0),
            PhotoItem("preview://photo/4", "documento.jpg", false, 0)
        ),
        metadata = PhotoMetadataUi(
            uri = "preview://photo/1",
            name = "IMG_20261026_1420.jpg",
            summary = "4032 × 3024 · 3,82 MB · JPEG · 8 bit · 3 componenti",
            loading = false,
            analyzing = false,
            status = "JPEG analizzato. La compatibilità verrà verificata prima della codifica.",
            rows = listOf("Dimensioni" to "4032 × 3024", "Dimensione" to "3,82 MB"),
            error = ""
        ),
        notice = "JPEG analizzato. La compatibilità completa verrà controllata prima della codifica.",
        detailsEnabled = true,
        detailsOpen = false,
        reading = false,
        selectedNames = emptyList(),
        allDetails = "",
        previousEnabled = false,
        previousHint = "Non ci sono sessioni da riprendere.",
        general = true,
        currentLabel = "File corrente",
        allLabel = "Tutti · 4",
        allocation = "4 file con destinazione generale",
        exception = false,
        exceptionLabel = "Destinazione diversa per il file corrente",
        editorVisible = false,
        origin = true,
        destination = false,
        folder = "Cartella originale",
        chooseVisible = false,
        smaller = true,
        quality = 65,
        preset = "slow",
        tune = "standard",
        nameAddition = "_convertita",
        namePrefix = false,
        permissions = OriginDirectoryAccess.State(emptyList(), emptyList(), emptyList(), "", false, 0, 0),
        selectionCount = 0
    )
    override val active = false
    override val blurEnabled = false
    override val imageLoader: ImageLoader? = null

    override fun command(command: PhotoCommand) = Unit
    override fun visiblePhotos(uris: List<String>) = Unit
    override fun select(index: Int, multiple: Boolean) = Unit
    override fun exception(value: Boolean) = Unit
    override fun smaller(value: Boolean) = Unit
    override fun quality(value: Int) = Unit
    override fun preset(index: Int) = Unit
    override fun tune(index: Int) = Unit
    override fun nameAddition(value: String) = Unit
    override fun namePrefix(value: Boolean) = Unit
    override fun authorize(row: OriginDirectoryAccess.Row) = Unit
    override fun changeFolder(row: OriginDirectoryAccess.Row) = Unit
    override fun setSystemBarsAppearance(dark: Boolean, opaque: Boolean) = Unit
    @Composable override fun progressContent() = Unit
    @Composable override fun diagnosticsContent() = Unit
}

@Preview(name = "Foto · Prepara il lavoro", widthDp = 420, heightDp = 1800, showBackground = true)
@Composable
private fun PhotoWorkspacePreview() {
    PhotoWorkspace(PhotoWorkspacePreviewHost())
}
