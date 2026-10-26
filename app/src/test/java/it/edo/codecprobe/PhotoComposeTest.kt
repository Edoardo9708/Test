package it.edo.codecprobe

import android.content.DialogInterface
import android.content.Intent
import android.app.Activity
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

    @Test fun pressingAGroupedChoiceWidensItAndCancellationDoesNotChangeQuality() {
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-next").performClick()
        val choice = compose.onNodeWithText("50")
        val resting = choice.fetchSemanticsNode().boundsInRoot.width
        choice.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(400)
        val held = choice.fetchSemanticsNode().boundsInRoot.width
        assertTrue("A held choice should borrow width: $resting / $held", held > resting)
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
        compose.onNodeWithTag("photo-details-loading").assertExists()
        compose.runOnIdle { compose.activity.images.compose.onVisible(true) }
        compose.waitUntil(5_000) { !compose.activity.images.compose.state.previewLoading }
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
        compose.onNodeWithText("File selezionati · 2").assertExists()
        compose.onNodeWithText(files[0].name).assertExists()
        compose.onNodeWithText(files[2].name).assertExists()
        compose.onNodeWithText("Fotocamera").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf(files[0].name, files[2].name).sortedWith(String.CASE_INSENSITIVE_ORDER),
                compose.activity.images.compose.state.selectedNames)
        }
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
            var lastPresentHeight = height()
            var removed = false
            var removalJump = Float.NaN
            repeat(24) {
                compose.mainClock.advanceTimeByFrame()
                val height = height()
                val present = compose.onAllNodesWithTag("photo-details-panel").fetchSemanticsNodes().isNotEmpty()
                if (!removed && !present) {
                    removalJump = lastPresentHeight - height
                    removed = true
                }
                if (present) lastPresentHeight = height
            }
            assertTrue("Details must leave composition after the exit", removed)
            assertEquals(closedHeight, height(), 1f)
            assertTrue("Last-frame height jump: $removalJump dp", kotlin.math.abs(removalJump) <= 2f)
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
        compose.onNodeWithTag("photo-scroll").performScrollToNode(hasTestTag(action))
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
        compose.onNodeWithTag("photo-scroll").performScrollToNode(hasTestTag(action))
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
}
