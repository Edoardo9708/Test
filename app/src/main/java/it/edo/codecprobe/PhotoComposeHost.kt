package it.edo.codecprobe

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import coil.ImageLoader
import coil.memory.MemoryCache

/** Temporary boundary: Compose commands use the existing Java coordinators and confirmations. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class PhotoComposeHost(private val images: ImageController) {
    private val screen get() = images.screen
    private val destinations get() = screen.destinations as PhotoDestinationsView
    private val thumbnailRevisions = mutableMapOf<String, Int>()
    var state by mutableStateOf(snapshot())
        private set
    var active by mutableStateOf(false)
        private set
    val imageLoader = ImageLoader.Builder(images.activity)
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
        // Only Compose is installed. Legacy bindings do not form another visible or scrolling page.
        screen.layout.backdrop.active(false)
        screen.selectedItem.thumbnails.active(false)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(images.activity.window, false)
        images.activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
        images.activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
        images.activity.setContentView(view)
        val previous = screen.sections.changed
        screen.sections.changed = Runnable {
            previous.run()
            screen.selectedItem.thumbnails.active(false)
            refresh()
        }
    }

    fun onVisible(value: Boolean) { active = value }
    fun refresh() { state = snapshot() }

    fun command(command: PhotoCommand) {
        if (command == PhotoCommand.REREAD && screen.reload.isEnabled) {
            val targets = images.selectionIndices().map { images.state.inputs[it] }.toSet()
            targets.forEach { uri -> thumbnailRevisions[uri] = (thumbnailRevisions[uri] ?: 0) + 1 }
            // Other visible images keep their cached bitmap; only the requested sources are invalidated.
            val cache = imageLoader.memoryCache
            cache?.keys?.filter { key -> targets.any { uri -> key.key.startsWith("$uri:") } }?.forEach { cache.remove(it) }
        }
        val button: Button = when (command) {
            PhotoCommand.NEW -> screen.sections.newWork
            PhotoCommand.RESUME -> screen.sections.previousWork
            PhotoCommand.SETTINGS -> screen.settings
            PhotoCommand.ADD_FILES -> screen.select
            PhotoCommand.ADD_FOLDER -> screen.importFolder
            PhotoCommand.CLEAR -> screen.clear
            PhotoCommand.REMOVE -> screen.remove
            PhotoCommand.REREAD -> screen.reload
            PhotoCommand.DETAILS -> screen.metadata
            PhotoCommand.CURRENT -> destinations.currentScope
            PhotoCommand.ALL -> destinations.allScope
            PhotoCommand.ORIGIN -> destinations.originChoice
            PhotoCommand.DESTINATION -> destinations.targetChoice
            PhotoCommand.CHOOSE_FOLDER -> destinations.pickFolder
            PhotoCommand.BACK -> screen.layout.back
            PhotoCommand.NEXT -> screen.layout.forward
            PhotoCommand.VIDEO -> screen.navigation.video
            PhotoCommand.AUDIO -> screen.navigation.audio
            PhotoCommand.ANALYSIS -> screen.navigation.analysis
        }
        if (button.isEnabled) button.performClick()
        refresh()
    }

    fun select(index: Int, multiple: Boolean) {
        if (multiple) images.toggleSelection(index) else {
            // The detached legacy preview does not need a ViewPropertyAnimator to change selection.
            if (images.busy() || index !in images.state.inputs.indices) return
            if (images.selectedPhotos.isNotEmpty()) images.toggleSelection(index) else {
                images.state.selected = index
                images.open()
            }
        }
        refresh()
    }

    fun exception(value: Boolean) { if (!images.busy()) destinations.exception.isChecked = value; refresh() }
    fun smaller(value: Boolean) { if (!images.busy()) screen.smaller.isChecked = value; refresh() }
    fun quality(value: Int) { if (!images.busy()) screen.quality.quality.progress = value.coerceIn(40, 95) - 40; refresh() }
    fun preset(index: Int) { if (!images.busy()) screen.quality.preset.slider.progress = index; refresh() }
    fun tune(index: Int) { if (!images.busy()) screen.quality.tune.slider.progress = index; refresh() }

    /** These sections are real existing controls; the same Java workers continue to update them. */
    fun legacyPermissions(): View = detach(destinations.originRows)
    fun legacyProgress(): View = detach(screen.sections.bodies[ImageScreen.HEIC]!!)
    fun legacyDiagnostics(): View = detach(screen.sections.bodies[ImageScreen.DETAILS]!!)
    private fun detach(view: View): View { (view.parent as? ViewGroup)?.removeView(view); return view }
    fun close() { imageLoader.shutdown(); view.disposeComposition() }

    private fun snapshot(): PhotoUiState {
        val data = images.state
        val selected = images.selectedPhotos
        val first = data.inputs.take(ImageQueueStore.VISIBLE)
        thumbnailRevisions.keys.retainAll(first.toSet())
        val items = first.mapIndexed { index, uri ->
            PhotoItem(uri, data.names.getOrElse(index) { "Foto ${index + 1}" },
                if (selected.isEmpty()) index == data.selected else uri in selected, thumbnailRevisions[uri] ?: 0)
        }
        return PhotoUiState(
            step = screen.sections.current(), busy = images.busy(), total = data.inputs.size, items = items,
            name = screen.name.text.toString(), summary = screen.summary.text.toString(), notice = screen.status.text.toString(),
            detailsEnabled = screen.metadata.isEnabled, detailsOpen = screen.selectedItem.detailsBody.visibility == View.VISIBLE,
            basicDetails = textOf(screen.selectedItem.metadataSummary), allDetails = if (selected.size > 1) "" else screen.details.text.toString(),
            previousEnabled = screen.sections.previousWork.isEnabled, previousHint = screen.sections.previousHint.text.toString(),
            general = destinations.general, currentLabel = destinations.currentScope.text.toString(),
            allLabel = destinations.allScope.text.toString(), allocation = destinations.allocation.text.toString(),
            exception = destinations.exception.isChecked, exceptionLabel = destinations.exception.text.toString(),
            editorVisible = destinations.editor.visibility == View.VISIBLE,
            origin = destinations.originChoice.isSelected, destination = destinations.targetChoice.isSelected,
            folder = destinations.folderLabel.text.toString(), chooseVisible = destinations.pickFolder.visibility == View.VISIBLE,
            smaller = data.options.onlySmaller, quality = data.options.quality, preset = data.options.preset, tune = data.options.tune
        )
    }

    private fun textOf(view: View): String = when (view) {
        is TextView -> view.text.toString()
        is ViewGroup -> (0 until view.childCount).map { textOf(view.getChildAt(it)) }.filter { it.isNotEmpty() }.joinToString("\n")
        else -> ""
    }
}
