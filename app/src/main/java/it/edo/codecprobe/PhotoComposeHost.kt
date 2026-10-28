package it.edo.codecprobe

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.graphics.toArgb
import coil.ImageLoader
import coil.memory.MemoryCache

/** UI-facing contract so the real workspace can render from static preview data too. */
internal interface PhotoWorkspaceHost {
    val state: PhotoUiState
    val active: Boolean
    val blurEnabled: Boolean
    val imageLoader: ImageLoader?
    fun command(command: PhotoCommand)
    fun visiblePhotos(uris: List<String>)
    fun select(index: Int, multiple: Boolean)
    fun exception(value: Boolean)
    fun smaller(value: Boolean)
    fun quality(value: Int)
    fun preset(index: Int)
    fun tune(index: Int)
    fun nameAddition(value: String)
    fun namePrefix(value: Boolean)
    fun authorize(row: OriginDirectoryAccess.Row)
    fun changeFolder(row: OriginDirectoryAccess.Row)
    fun setSystemBarsAppearance(dark: Boolean, opaque: Boolean)
    @Composable fun progressContent()
    @Composable fun diagnosticsContent()
}

/** Compose consumes the model and invokes coordinators directly; no hidden command widgets. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class PhotoComposeHost(private val images: ImageController) : PhotoWorkspaceHost {
    val window get() = images.activity.window
    private val screen get() = images.screen
    private val destinations get() = images.destinations.photo
    private val thumbnailRevisions = mutableMapOf<String, Int>()
    private var visibleUris = emptyList<String>()
    private var inventory: JpegInventory.Info? = null
    private var inventoryDetails = ""
    private val previewDetails = PhotoDetailsLoader({ PhotoBasicDetails.read(images.activity, it) }) { uri ->
        if (images.state.inputs.getOrNull(images.state.selected) == uri) refresh()
    }
    override var state by mutableStateOf(snapshot())
        private set
    override var active by mutableStateOf(false)
        private set
    override var blurEnabled by mutableStateOf(PhotoAppearance.blurEnabled(images.activity))
        private set
    override val imageLoader: ImageLoader? = ImageLoader.Builder(images.activity)
        .dispatcher(kotlinx.coroutines.Dispatchers.IO.limitedParallelism(1))
        .bitmapFactoryMaxParallelism(1)
        .memoryCache { MemoryCache.Builder(images.activity).maxSizeBytes(2 * 1024 * 1024).build() }
        .diskCache(null)
        .build()
    val view = ComposeView(images.activity).apply {
        id = R.id.photo_compose
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { PhotoWorkspace(this@PhotoComposeHost) }
    }

    init {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(images.activity.window, false)
        val initialOpaque = state.step == ImageScreen.FILES
        val dark = (images.activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val initialBarColor = if (initialOpaque) appSurfaceColor(dark).toArgb() else android.graphics.Color.TRANSPARENT
        images.activity.window.statusBarColor = initialBarColor
        images.activity.window.navigationBarColor = initialBarColor
        images.activity.setContentView(view)
    }

    fun onVisible(value: Boolean) {
        if (value) blurEnabled = PhotoAppearance.blurEnabled(images.activity)
        active = value
        scheduleDetails()
    }
    fun refresh() { state = snapshot(); scheduleDetails() }
    override fun visiblePhotos(uris: List<String>) { visibleUris = uris; scheduleDetails() }
    private fun scheduleDetails() {
        val ready = active && !state.busy && state.step == ImageScreen.FILES
        val selected = images.state.inputs.getOrNull(images.state.selected)
        val first = images.state.inputs.take(ImageQueueStore.VISIBLE)
        val visible = visibleUris.filter { it in first }
        previewDetails.update(if (ready) listOfNotNull(selected) + visible else emptyList())
    }

    override fun command(command: PhotoCommand) {
        when (command) {
            PhotoCommand.NEW -> screen.sections.focusCard(ImageScreen.FILES)
            PhotoCommand.RESUME -> if (screen.sections.previousEnabled) screen.sections.focusCard(ImageScreen.HEIC)
            PhotoCommand.SETTINGS -> images.settings()
            PhotoCommand.ADD_FILES -> images.selection.pick()
            PhotoCommand.ADD_FOLDER -> images.selection.sourceFolder()
            PhotoCommand.CLEAR -> images.confirmRemoval(true)
            PhotoCommand.REMOVE -> images.confirmRemoval(false)
            PhotoCommand.REREAD -> reread()
            PhotoCommand.DETAILS -> images.toggleDetails()
            PhotoCommand.END_SELECTION -> images.finishSelection()
            PhotoCommand.CURRENT -> images.destinations.photoGeneral(false)
            PhotoCommand.ALL -> images.destinations.photoGeneral(true)
            PhotoCommand.ORIGIN -> images.destinations.photoOrigin(true)
            PhotoCommand.DESTINATION -> images.destinations.photoOrigin(false)
            PhotoCommand.CHOOSE_FOLDER -> images.destinations.choosePhotoFolder()
            PhotoCommand.BACK -> images.back()
            PhotoCommand.NEXT -> images.next()
            PhotoCommand.VIDEO -> images.navigate(WorkspaceNavigator.Section.VIDEO)
            PhotoCommand.AUDIO -> images.navigate(WorkspaceNavigator.Section.AUDIO)
            PhotoCommand.ANALYSIS -> images.navigate(WorkspaceNavigator.Section.ANALYSIS)
        }
    }

    private fun reread() {
        if (images.busy() || images.state.reading || images.state.inputs.isEmpty()) return
        val targets = images.selectionIndices().map { images.state.inputs[it] }.toSet()
        previewDetails.invalidate(targets)
        targets.forEach { uri -> thumbnailRevisions[uri] = (thumbnailRevisions[uri] ?: 0) + 1 }
        val cache = imageLoader?.memoryCache
        cache?.keys?.filter { key -> targets.any { uri -> key.key.startsWith(uri + ":") } }?.forEach { cache.remove(it) }
        images.reloadSelection()
    }

    override fun select(index: Int, multiple: Boolean) {
        if (multiple) images.toggleSelection(index) else images.select(index)
    }
    override fun exception(value: Boolean) = images.destinations.photoException(value)
    override fun smaller(value: Boolean) = images.smaller(value)
    override fun quality(value: Int) = images.quality(value)
    override fun preset(index: Int) = images.preset(index)
    override fun tune(index: Int) = images.tune(index)
    override fun nameAddition(value: String) = images.nameAddition(value)
    override fun namePrefix(value: Boolean) = images.namePrefix(value)
    override fun authorize(row: OriginDirectoryAccess.Row) = images.destinations.authorizePhoto(row)
    override fun changeFolder(row: OriginDirectoryAccess.Row) = images.destinations.changePhotoFolder(row)

    override fun setSystemBarsAppearance(dark: Boolean, opaque: Boolean) {
        val color = if (opaque) appSurfaceColor(dark).toArgb() else android.graphics.Color.TRANSPARENT
        window.statusBarColor = color
        window.navigationBarColor = color
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }

    @Composable override fun progressContent() {
        androidx.compose.ui.viewinterop.AndroidView(factory = { legacyProgress() }, modifier = Modifier.fillMaxWidth())
    }

    @Composable override fun diagnosticsContent() {
        androidx.compose.ui.viewinterop.AndroidView(factory = { legacyDiagnostics() }, modifier = Modifier.fillMaxWidth())
    }

    /** Each of these is a real visible control, embedded once rather than mirrored in Compose. */
    fun legacyProgress(): View = detach(screen.sections.bodies[ImageScreen.HEIC]!!)
    fun legacyDiagnostics(): View = detach(screen.sections.bodies[ImageScreen.DETAILS]!!)
    private fun detach(view: View): View { (view.parent as? ViewGroup)?.removeView(view); return view }
    fun close() { previewDetails.close(); imageLoader?.shutdown(); view.disposeComposition() }

    private fun snapshot(): PhotoUiState {
        val data = images.state
        val selected = images.selectedPhotos
        val first = data.inputs.take(ImageQueueStore.VISIBLE)
        thumbnailRevisions.keys.retainAll(first.toSet())
        val items = first.mapIndexed { index, uri ->
            PhotoItem(uri, data.names.getOrElse(index) { "Foto " + (index + 1) },
                if (selected.isEmpty()) index == data.selected else uri in selected, thumbnailRevisions[uri] ?: 0)
        }
        val uri = data.inputs.getOrNull(data.selected).orEmpty()
        val preview = previewDetails.get(uri)
        val names = if (selected.size > 1) images.selectionIndices().map { data.names[it] }.sortedWith(String.CASE_INSENSITIVE_ORDER) else emptyList()
        val info = data.info?.takeIf { images.infoUri == uri }
        if (info !== inventory) {
            inventory = info
            inventoryDetails = if (info == null) "" else PhotoInventoryText.format(info)
        }
        val name = if (names.isNotEmpty()) names.size.toString() + " foto selezionate"
            else data.name.ifEmpty { data.names.getOrNull(data.selected) ?: "Nessuna foto selezionata" }
        val summary = when {
            names.isNotEmpty() -> "Rimuovi, rileggi e destinazione agiscono sulla selezione."
            preview == null -> ""
            preview.error.isNotBlank() && info == null -> ""
            else -> photoMetadataSummary(preview, info)
        }
        val notice = when {
            data.importing -> data.notice
            data.reading -> "Analisi della foto…"
            data.notice.isNotEmpty() -> data.notice
            info != null -> "JPEG analizzato. La compatibilità sarà verificata prima della conversione."
            else -> "Scegli le foto. Gli originali restano invariati."
        }
        val metadata = PhotoMetadataUi(
            uri = uri,
            name = name,
            summary = summary,
            loading = uri.isNotEmpty() && names.isEmpty() && preview == null,
            analyzing = data.reading && names.isEmpty(),
            status = if (names.isEmpty()) notice else "",
            // Basic dimensions and size stay bound to the URI-keyed preview result.
            rows = (preview ?: PhotoBasicDetails()).rows(),
            error = preview?.error.orEmpty()
        )
        val allDetails = when {
            names.isNotEmpty() || data.reading -> ""
            info != null -> inventoryDetails
            data.notice.isEmpty() -> "Seleziona una foto per leggere i metadati."
            else -> "Analisi non disponibile: " + data.notice
        }
        return PhotoUiState(
            step = screen.sections.current(), busy = images.busy(), total = data.inputs.size, items = items,
            metadata = metadata, notice = notice,
            detailsEnabled = data.inputs.isNotEmpty(), detailsOpen = images.detailsOpen,
            reading = data.reading, selectedNames = names, allDetails = allDetails,
            previousEnabled = screen.sections.previousEnabled, previousHint = screen.sections.previousHint,
            general = destinations.general, currentLabel = destinations.currentLabel,
            allLabel = destinations.allLabel, allocation = destinations.allocation,
            exception = destinations.exception, exceptionLabel = destinations.exceptionLabel,
            editorVisible = destinations.editorVisible, origin = destinations.origin, destination = destinations.destination,
            folder = destinations.folder, chooseVisible = destinations.chooseVisible,
            smaller = data.options.onlySmaller, quality = data.options.quality, preset = data.options.preset, tune = data.options.tune,
            nameAddition = data.options.nameAddition, namePrefix = data.options.namePrefix,
            permissions = images.destinations.permissions(), selectionCount = selected.size
        )
    }

    private fun photoMetadataSummary(preview: PhotoBasicDetails, info: JpegInventory.Info?): String {
        val width = preview.width
        val height = preview.height
        val bytes = preview.bytes
        val parts = mutableListOf<String>()
        if (width > 0 && height > 0) parts += "$width × $height"
        parts += CompressionSummary.size(bytes)
        parts += "JPEG"
        if (info != null) {
            val component = if (info.components == 1) "componente" else "componenti"
            parts += "${info.precision} bit"
            parts += "${info.components} $component"
        }
        return parts.joinToString(" · ")
    }

}
