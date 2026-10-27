package it.edo.codecprobe

import android.content.DialogInterface
import android.content.Intent
import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.io.ByteArrayOutputStream

/** Interactions on actual Compose nodes, not the retained Java command widgets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoComposeTest {
    @get:Rule val compose = createAndroidComposeRule<ImageActivity>()
    private val files = mutableListOf<File>()

    @Before fun reset() {
        compose.runOnIdle {
            val images = compose.activity.images
            images.compose.onVisible(false)
            images.stopPreviewReads()
            images.state.inputs.clear(); images.state.names.clear(); images.selectedPhotos.clear()
            images.state.options = ImageOptions(65, "slow", "standard", null, true, false, 0)
            images.state.destinations.reset(emptyList(), null)
            images.showOptions(); images.render(); images.screen.sections.home()
        }
    }

    @After fun cleanup() {
        files.forEach { it.delete() }
        compose.runOnIdle { compose.activity.getSharedPreferences("image-workspace", 0).edit().clear().commit() }
    }

    @Test fun composeIsInstalledAndBackPreservesQuality() {
        compose.onNodeWithTag("photo-new").assertIsDisplayed().performClick()
        val tree = compose.onRoot().printToString()
        compose.runOnIdle { assertEquals(tree, ImageScreen.FILES, compose.activity.images.screen.sections.current()) }
        compose.onNodeWithTag("photo-next").performClick()
        compose.onNodeWithText("55", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(55, compose.activity.images.state.options.quality) }
        compose.onNodeWithTag("photo-back").performClick()
        compose.onNodeWithTag("photo-back").performClick()
        compose.onNodeWithTag("photo-new").assertIsDisplayed().performClick()
        val restoredTree = compose.onRoot().printToString()
        compose.runOnIdle { assertEquals(restoredTree, ImageScreen.FILES, compose.activity.images.screen.sections.current()) }
        compose.runOnIdle {
            val images = compose.activity.images
            assertTrue(images.compose.view.isShown)
            assertSame(images.compose.view, compose.activity.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0))
            assertEquals(55, images.state.options.quality)
        }
    }

    @Test fun compactFirstPageNavigationKeepsItsCenterAndOnlyShowsAvailableArrows() {
        compose.onNodeWithTag("photo-back").assertDoesNotExist()
        compose.onNodeWithTag("photo-next").assertDoesNotExist()
        val chooserTab = compose.onNodeWithTag("photo-nav-foto").fetchSemanticsNode().boundsInRoot

        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-back").assertIsDisplayed()
        compose.onNodeWithTag("photo-next").assertIsDisplayed()
        val filesTab = compose.onNodeWithTag("photo-nav-foto").fetchSemanticsNode().boundsInRoot
        assertEquals("Hiding arrows must not shift the central navigation", chooserTab.left, filesTab.left, 1f)
        assertEquals(chooserTab.width, filesTab.width, 1f)
        listOf("video", "audio", "foto", "analisi").forEach { compose.onNodeWithTag("photo-nav-$it").assertIsDisplayed() }

        compose.onNodeWithTag("photo-next").performClick()
        compose.onNodeWithTag("photo-next").performClick()
        compose.onNodeWithTag("photo-next").assertDoesNotExist()
    }

    @Test fun pressingAGroupedChoiceKeepsItsWidthAndCancellationDoesNotChangeQuality() {
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-next").performClick()
        val choice = compose.onNodeWithText("50")
        val resting = choice.fetchSemanticsNode().boundsInRoot.width
        choice.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(400)
        val held = choice.fetchSemanticsNode().boundsInRoot.width
        assertEquals("Press feedback must not change the button layout", resting, held, 1f)
        compose.runOnIdle { assertEquals(65, compose.activity.images.state.options.quality) }
        choice.performTouchInput { cancel() }
        compose.mainClock.advanceTimeBy(600)
        assertEquals(resting, choice.fetchSemanticsNode().boundsInRoot.width, 1f)
        compose.runOnIdle { assertEquals(65, compose.activity.images.state.options.quality) }
        choice.performClick()
        compose.runOnIdle { assertEquals(50, compose.activity.images.state.options.quality) }
    }

    @Test fun detailsKeepFiveFieldsAndASeparateCardWhileLoadingAndAfterSelection() {
        addPhotos(2)
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-details").performClick()
        compose.onNodeWithTag("photo-details-panel").assertExists()
        compose.onNodeWithTag("photo-details-card").assertExists()
        compose.onNodeWithTag("photo-details-loading").assertExists()
        compose.onNodeWithTag("photo-details-skeleton").assertExists()
        val cardHeightWhileLoading = compose.onNodeWithTag("photo-details-card").fetchSemanticsNode().boundsInRoot.height
        compose.runOnIdle { compose.activity.images.compose.onVisible(true) }
        compose.waitUntil(5_000) { !compose.activity.images.compose.state.previewLoading }
        val cardHeightAfterLoading = compose.onNodeWithTag("photo-details-card").fetchSemanticsNode().boundsInRoot.height
        assertEquals("Metadata loading must not collapse or resize the details card", cardHeightWhileLoading, cardHeightAfterLoading, 1f)
        compose.onNodeWithText("Dimensioni").assertExists()
        compose.onNodeWithText("Formato", substring = false).assertExists()
        compose.onNodeWithText("Dimensione file").assertExists()
        compose.onNodeWithText("Fotocamera").assertExists()
        compose.onNodeWithText("Cartella", substring = false).assertExists()
        compose.onNodeWithTag("photo-details-loading").assertDoesNotExist()
        compose.waitUntil(5_000) { !compose.activity.images.state.reading }
        compose.onNodeWithTag("photo-1").performClick()
        compose.onNodeWithTag("photo-details-panel").assertExists()
        compose.waitUntil(5_000) { !compose.activity.images.compose.state.previewLoading && !compose.activity.images.state.reading }
        compose.onNodeWithTag("photo-0").performClick()
        compose.runOnIdle {
            assertFalse("Cached details should appear immediately", compose.activity.images.compose.state.previewLoading)
            assertFalse("Cached inventory must not enter a blank reading state", compose.activity.images.state.reading)
        }
    }

    @Test fun multiSelectionDetailsListNamesAndDoNotShowThePreviousPhotoMetadata() {
        addPhotos(3)
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-2").performTouchInput { longClick() }
        compose.onNodeWithTag("photo-0").performClick()
        compose.onNodeWithTag("photo-details").performClick()
        compose.onNodeWithTag("photo-details-panel").assertExists()
        compose.onNodeWithTag("photo-details-card").assertExists()
        compose.onNodeWithText("Dettagli della selezione").assertExists()
        compose.onNodeWithText(files[0].name).assertExists()
        compose.onNodeWithText(files[2].name).assertExists()
        compose.onNodeWithText("Fotocamera").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf(files[0].name, files[2].name).sortedWith(String.CASE_INSENSITIVE_ORDER),
                compose.activity.images.compose.state.selectedNames)
        }
    }

    @Test fun metadataSnapshotCannotPairNewSelectionWithPreviousInventory() {
        addPhotos(2)
        compose.runOnIdle {
            val images = compose.activity.images
            images.compose.onVisible(false)
            val oldUri = images.state.inputs[0]
            images.state.selected = 0
            images.state.info = JpegInventory.read(ImageWorkspaceTest.fixture().inputStream())
            images.infoUri = oldUri
            images.state.name = files[0].name
            images.render()

            images.select(1)

            val metadata = images.compose.state.metadata
            assertEquals(images.state.inputs[1], metadata.uri)
            assertEquals(files[1].name, metadata.name)
            assertTrue("Uncached selected photo must immediately enter its own loading state", metadata.loading)
            assertEquals("No previous-photo summary may survive the selection", "", metadata.summary)
            assertEquals("Non disponibili", metadata.rows.first().second)
            assertNull("Inventory from another URI must be discarded", images.state.info)
        }
    }

    @Test fun cachedPhotoMetadataRunsAnEntranceTransitionOnEachUriChange() {
        addPhotos(2)
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-details").performClick()
        compose.runOnIdle { compose.activity.images.compose.onVisible(true) }
        compose.waitUntil(5_000) { !compose.activity.images.compose.state.previewLoading }

        compose.onNodeWithTag("photo-1").performClick()
        compose.waitUntil(5_000) { !compose.activity.images.compose.state.previewLoading }
        compose.onNodeWithTag("photo-0").performClick()
        compose.runOnIdle {
            assertFalse("First photo metadata should already be cached", compose.activity.images.compose.state.previewLoading)
        }

        val settledCardHeight = compose.onNodeWithTag("photo-details-card").fetchSemanticsNode().boundsInRoot.height
        val oldUri = compose.activity.images.compose.state.metadata.uri
        val oldContent = "photo-metadata-content-${oldUri.hashCode()}"
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithTag("photo-1").performClick()
            val newMetadata = compose.activity.images.compose.state.metadata
            assertEquals(compose.activity.images.state.inputs[1], newMetadata.uri)
            assertEquals(files[1].name, newMetadata.name)
            val newContent = "photo-metadata-content-${newMetadata.uri.hashCode()}"
            compose.mainClock.advanceTimeBy(50)
            compose.onNodeWithTag(oldContent).assertExists()
            compose.onNodeWithTag(newContent).assertExists()
            assertEquals(settledCardHeight,
                compose.onNodeWithTag("photo-details-card").fetchSemanticsNode().boundsInRoot.height, 1f)
            compose.mainClock.advanceTimeBy(100)
            compose.onNodeWithTag(newContent).assertExists()
            compose.mainClock.advanceTimeBy(250)
            compose.onNodeWithTag(newContent).assertExists()
            compose.onNodeWithTag(oldContent).assertDoesNotExist()
            assertEquals(settledCardHeight,
                compose.onNodeWithTag("photo-details-card").fetchSemanticsNode().boundsInRoot.height, 1f)
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test fun rapidPhotoChangesKeepMetadataBoundToTheSelectedUriWithDetailsOpenAndClosed() {
        val dimensions = listOf(64 to 48, 96 to 72, 128 to 80, 160 to 120)
        addPhotosWithDimensions(dimensions)
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-details").performClick()
        compose.runOnIdle { compose.activity.images.compose.onVisible(true) }

        // Warm all per-URI quick metadata entries before testing the cached A → B → C → A path.
        dimensions.indices.forEach { index ->
            if (compose.activity.images.state.selected != index) {
                compose.runOnIdle { compose.activity.images.select(index) }
            }
            compose.waitUntil(5_000) { !compose.activity.images.compose.state.previewLoading }
            compose.waitUntil(5_000) { !compose.activity.images.state.reading }
        }

        fun assertRapidSequence() {
            compose.runOnIdle {
                val images = compose.activity.images
                listOf(1, 2, 3, 0).forEach { index ->
                    images.select(index)
                    val metadata = images.compose.state.metadata
                    val uri = images.state.inputs[index]
                    val (width, height) = dimensions[index]
                    assertEquals(uri, metadata.uri)
                    assertEquals(files[index].name, metadata.name)
                    assertFalse("Cached URI must not enter loading while switching", metadata.loading)
                    assertEquals("$width × $height px", metadata.rows.first().second)
                    assertEquals("$width × $height", metadata.summary.substringBefore(" · "))
                    assertEquals(String.format(java.util.Locale.ITALY, "%.2f MB", files[index].length() / 1_000_000.0),
                        metadata.rows[2].second)
                }
            }
            compose.waitForIdle()
            compose.waitUntil(5_000) {
                !compose.activity.images.compose.state.previewLoading && compose.activity.images.compose.state.metadata.uri ==
                    compose.activity.images.state.inputs[0]
            }
        }

        assertRapidSequence()
        compose.onNodeWithTag("photo-details").performClick()
        compose.mainClock.advanceTimeBy(500)
        assertRapidSequence()
    }

    @Test fun namingFieldCanFinishEditingAndShowsTheTransformedExampleName() {
        compose.onNodeWithTag("photo-new").performClick()
        compose.runOnIdle { compose.activity.images.namePrefix(true) }
        compose.onNodeWithTag("photo-scroll").performScrollToNode(hasTestTag("photo-name-addition"))
        val nameField = compose.onNodeWithTag("photo-name-addition")
        nameField.performClick().assertIsFocused()
        nameField.performTextInput("titolo_")
        compose.onNodeWithTag("photo-name-preview")
            .assertTextContains("la mia magnifica foto.jpeg → titolo_la mia magnifica foto.heic", substring = true)
        compose.onNodeWithTag("photo-name-finish").assertIsDisplayed().performClick()
        compose.onNodeWithTag("photo-name-finish").assertDoesNotExist()
    }

    @Test fun savingOptionsArePresentedInsideOneGroupedSurface() {
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-scroll").performScrollToNode(hasTestTag("photo-saving-group"))
        val group = compose.onNodeWithTag("photo-saving-group")
        group.assertExists()
        group.assert(hasAnyDescendant(hasText("Cosa elaborare")))
        group.assert(hasAnyDescendant(hasText("Dove salvare")))
        group.assert(hasAnyDescendant(hasText("Nome dei file")))
        group.assert(hasAnyDescendant(hasText("Conserva solo se più piccolo")))
    }

    @Test fun detailsCollapseDoesNotDropAnExternalGapAtDisposal() {
        addPhotos(1)
        compose.onNodeWithTag("photo-new").performClick()
        compose.waitUntil(5_000) { !compose.activity.images.state.reading }
        val panel = compose.onNodeWithTag("photo-selected-panel")
        fun height() = panel.getUnclippedBoundsInRoot().let { (it.bottom - it.top).value }
        val closedHeight = height()
        compose.onNodeWithTag("photo-details").performClick()
        compose.mainClock.advanceTimeBy(500)
        assertTrue(height() > closedHeight + 100f)
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithTag("photo-details").performClick()
            val expandedHeight = height()
            var previousHeight = expandedHeight
            var largestFrameChange = 0f
            var sawIntermediateHeight = false
            repeat(24) {
                compose.mainClock.advanceTimeByFrame()
                val currentHeight = height()
                largestFrameChange = maxOf(largestFrameChange, kotlin.math.abs(previousHeight - currentHeight))
                if (currentHeight > closedHeight + 1f && currentHeight < expandedHeight - 1f) sawIntermediateHeight = true
                previousHeight = currentHeight
            }
            assertTrue("Closing details should pass through intermediate heights", sawIntermediateHeight)
            assertTrue("Collapse should not remove the panel in one frame: $largestFrameChange dp",
                largestFrameChange < (expandedHeight - closedHeight) * .4f)
            assertEquals(closedHeight, height(), 1f)
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test fun longPressAndSwitchApplyExceptionsToExactlyTheSelectedFiles() {
        addPhotos(3)
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-0").performTouchInput { longClick() }
        compose.onNodeWithTag("photo-2").performClick()
        compose.onNodeWithTag("photo-scroll").performScrollToNode(hasTestTag("photo-exception"))
        compose.onNodeWithTag("photo-exception").assertIsDisplayed().assertIsOff().performClick()
        compose.onNodeWithTag("photo-exception").assertIsOn()
        compose.runOnIdle {
            val images = compose.activity.images
            assertEquals(listOf(0, 2), images.selectionIndices())
            assertEquals(2, images.state.destinations.plan.exceptionCount())
            assertFalse(images.state.destinations.plan.files[1].exception)
        }
        compose.onNodeWithTag("photo-back").assertIsDisplayed()
    }

    @Test fun clearUsesExistingConfirmationAndNeverDeletesOriginals() {
        addPhotos(2)
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-clear").performClick()
        compose.runOnIdle {
            assertEquals(2, compose.activity.images.state.inputs.size)
            ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        }
        compose.onNodeWithTag("photo-clear").performClick()
        compose.runOnIdle {
            ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            assertTrue(compose.activity.images.state.inputs.isEmpty())
            assertTrue(files.all { it.exists() })
        }
    }

    @Test fun scrollingToTheEndKeepsPermissionsAboveFloatingButtons() {
        addPhotos(1)
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-permissions").assertDoesNotExist()
        compose.waitUntil(5_000) { !compose.activity.images.compose.state.permissions.checking }
        compose.onNodeWithTag("photo-scroll").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) {
            it(0f, 100_000f)
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val permissions = compose.onNodeWithTag("photo-permissions").fetchSemanticsNode().boundsInRoot
        val back = compose.onNodeWithTag("photo-back").fetchSemanticsNode().boundsInRoot
        assertTrue("Last row is covered: $permissions / $back", permissions.bottom <= back.top)
    }

    @Test fun permissionCancellationKeepsFolderAndBlocksConversion() {
        addPhotos(1)
        compose.runOnIdle {
            ShadowContentResolver.registerProviderInternal("destinations", MediaDestinationsTest.Directories())
            compose.activity.images.setFolder(MediaDestinationsTest.A)
        }
        compose.onNodeWithTag("photo-new").performClick()
        compose.waitUntil(5_000) { !compose.activity.images.compose.state.permissions.checking }
        val row = compose.activity.images.compose.state.permissions.rows.single()
        assertFalse(row.allowed)
        compose.runOnIdle { assertFalse(compose.activity.images.screen.convert.isEnabled) }
        val action = "photo-authorize:${row.key}"
        revealAboveDock(action)
        compose.onNodeWithTag(action).assertIsEnabled().performClick()
        compose.onNodeWithTag(action).assertIsNotEnabled()
        compose.runOnIdle {
            val request = shadowOf(compose.activity).nextStartedActivityForResult
            assertEquals(MediaDestinationsController.AUTHORIZE, request.requestCode)
            assertEquals(FolderPickerActivity::class.java.name, request.intent.component!!.className)
            compose.activity.images.selection.result(request.requestCode, Activity.RESULT_CANCELED, null)
        }
        compose.waitUntil(5_000) { !compose.activity.images.compose.state.permissions.checking }
        compose.onNodeWithTag(action).assertIsEnabled()
        compose.runOnIdle {
            val images = compose.activity.images
            assertEquals(MediaDestinationsTest.A, images.state.destinations.plan.effective(0).folder)
            assertFalse(images.destinations.ready())
            assertFalse(images.screen.convert.isEnabled)
        }
    }

    @Test fun permissionGrantSurvivesRecreationAndRevocationBlocksConversion() {
        addPhotos(1)
        compose.runOnIdle {
            ShadowContentResolver.registerProviderInternal("destinations", MediaDestinationsTest.Directories())
            compose.activity.images.setFolder(MediaDestinationsTest.A)
        }
        compose.onNodeWithTag("photo-new").performClick()
        compose.waitUntil(5_000) { !compose.activity.images.compose.state.permissions.checking }
        val row = compose.activity.images.compose.state.permissions.rows.single()
        val action = "photo-authorize:${row.key}"
        revealAboveDock(action)
        compose.onNodeWithTag(action).performClick()
        compose.runOnIdle { assertEquals(MediaDestinationsController.AUTHORIZE, shadowOf(compose.activity).nextStartedActivityForResult.requestCode) }
        compose.activityRule.scenario.recreate()
        compose.runOnIdle {
            assertTrue(compose.activity.images.destinations.choosing())
            compose.activity.images.selection.result(MediaDestinationsController.AUTHORIZE, Activity.RESULT_OK,
                Intent().setData(Uri.parse(MediaDestinationsTest.A)).addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
        }
        compose.waitUntil(5_000) { compose.activity.images.destinations.ready() }
        compose.onNodeWithTag("photo-scroll").performScrollToNode(hasText("✓ Accesso verificato"))
        compose.onNodeWithText("✓ Accesso verificato").assertExists()
        compose.runOnIdle {
            val images = compose.activity.images
            assertEquals(listOf(MediaDestinationsTest.A), images.destinations.snapshot())
            assertTrue(images.screen.convert.isEnabled)
            compose.activity.contentResolver.releasePersistableUriPermission(Uri.parse(MediaDestinationsTest.A),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            images.destinations.refresh()
        }
        compose.waitUntil(5_000) { !compose.activity.images.compose.state.permissions.checking }
        compose.onNodeWithTag("photo-scroll").performScrollToNode(hasText("! Da autorizzare"))
        compose.onNodeWithText("! Da autorizzare").assertExists()
        compose.runOnIdle {
            assertFalse(compose.activity.images.destinations.ready())
            assertFalse(compose.activity.images.screen.convert.isEnabled)
        }
    }

    @Test fun permissionSnapshotsBoundRowsKeepAllDestinationsAndRejectStaleActions() {
        compose.runOnIdle {
            val images = compose.activity.images
            repeat(105) {
                images.state.inputs.add("content://unknown-photo/$it")
                images.state.names.add("Foto $it.jpg")
            }
            images.state.clamp(); images.render()
        }
        compose.waitUntil(5_000) { !compose.activity.images.compose.state.permissions.checking }
        val previous = compose.activity.images.compose.state.permissions
        assertEquals(105, previous.total)
        assertEquals(100, previous.rows.size)
        assertEquals(105, previous.destinations.size)
        assertFalse(previous.ready)
        compose.runOnIdle {
            ShadowContentResolver.registerProviderInternal("destinations", MediaDestinationsTest.Directories())
            MediaDestinationsTest.grant(compose.activity, MediaDestinationsTest.A)
            compose.activity.images.setFolder(MediaDestinationsTest.A)
            compose.activity.images.compose.authorize(previous.rows.first())
            assertNull(shadowOf(compose.activity).nextStartedActivity)
        }
        compose.waitUntil(5_000) { compose.activity.images.destinations.ready() }
        compose.runOnIdle {
            val current = compose.activity.images.compose.state.permissions
            assertEquals(1, current.total)
            assertEquals(1, current.authorized)
            assertEquals(1, current.rows.size)
            assertEquals(List(105) { MediaDestinationsTest.A }, current.destinations)
            assertTrue(current.rows.single().numbers.endsWith("… (105 foto)"))
            assertEquals(100, previous.rows.size)
            assertEquals(105, previous.total)
            compose.activity.images.compose.authorize(previous.rows.first())
            assertNull(shadowOf(compose.activity).nextStartedActivity)
        }
    }

    private fun addPhotos(count: Int) {
        repeat(count) {
            val file = File.createTempFile("compose-photo-", ".jpg")
            file.writeBytes(ImageWorkspaceTest.fixture()); files += file
        }
        compose.runOnIdle {
            val images = compose.activity.images
            images.state.inputs.addAll(files.map { Uri.fromFile(it).toString() })
            images.state.names.addAll(files.map { it.name })
            images.state.clamp(); images.open()
        }
    }

    private fun addPhotosWithDimensions(dimensions: List<Pair<Int, Int>>) {
        dimensions.forEachIndexed { index, (width, height) ->
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.rgb(24 + index * 35, 72 + index * 25, 150 - index * 20))
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            bitmap.recycle()
            val file = File.createTempFile("compose-photo-$index-", ".jpg")
            file.writeBytes(output.toByteArray())
            files += file
        }
        compose.runOnIdle {
            val images = compose.activity.images
            images.state.inputs.addAll(files.map { Uri.fromFile(it).toString() })
            images.state.names.addAll(files.map { it.name })
            images.state.clamp(); images.open()
        }
    }

    private fun revealAboveDock(tag: String) {
        val scroll = compose.onNodeWithTag("photo-scroll")
        scroll.performScrollToNode(hasTestTag(tag))
        scroll.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        compose.onNodeWithTag(tag).assertIsDisplayed()
    }
}
