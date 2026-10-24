package it.edo.codecprobe

import android.content.DialogInterface
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
            assertFalse(images.screen.layout.root.isShown)
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

    @Test fun longPressAndSwitchApplyExceptionsToExactlyTheSelectedFiles() {
        addPhotos(3)
        compose.onNodeWithTag("photo-new").performClick()
        compose.onNodeWithTag("photo-0").performTouchInput { longClick() }
        compose.onNodeWithTag("photo-2").performClick()
        compose.onNodeWithTag("photo-exception").performScrollTo()
        // ScrollTo only knows the scroll viewport, so clear the floating controls before tapping.
        compose.onNodeWithTag("photo-scroll").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) {
            it(0f, compose.activity.images.screen.ui.dp(160).toFloat())
        }
        compose.onNodeWithTag("photo-exception").assertIsDisplayed().performClick()
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
        compose.waitUntil(5_000) { compose.activity.images.screen.destinations.originRows.childCount > 1 }
        compose.onNodeWithTag("photo-scroll").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) {
            it(0f, 100_000f)
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val permissions = compose.onNodeWithTag("photo-permissions").fetchSemanticsNode().boundsInRoot
        val back = compose.onNodeWithTag("photo-back").fetchSemanticsNode().boundsInRoot
        assertTrue("Last row is covered: $permissions / $back", permissions.bottom <= back.top)
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
