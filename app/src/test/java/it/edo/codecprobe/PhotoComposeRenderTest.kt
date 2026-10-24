package it.edo.codecprobe

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.ByteArrayInputStream
import java.io.File

/** Captures the installed Compose screen, including overlays and the real scroll position. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class PhotoComposeRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ImageActivity>()
    private var oldRecord: String? = null

    @Before fun prepare() {
        oldRecord = System.getProperty("roborazzi.test.record")
        System.setProperty("roborazzi.test.record", "true")
        compose.runOnIdle {
            val images = compose.activity.images
            images.compose.onVisible(false)
            images.stopPreviewReads()
            images.state.inputs.clear(); images.state.names.clear(); images.selectedPhotos.clear()
            images.state.inputs.add("content://preview/photo/1")
            images.state.names.add("Una foto simpatica.jpg"); images.state.name = "Una foto simpatica.jpg"
            images.state.info = JpegInventory.read(ByteArrayInputStream(ImageWorkspaceTest.fixture()))
            images.state.notice = ""; images.state.reading = false
            images.setFolder("content://dirs/tree/primary%3APictures%2FHEIC")
            images.render()
        }
    }

    @After fun cleanup() {
        if (oldRecord == null) System.clearProperty("roborazzi.test.record") else System.setProperty("roborazzi.test.record", oldRecord!!)
        compose.runOnIdle { compose.activity.getSharedPreferences("image-workspace", 0).edit().clear().commit() }
    }

    @Test fun floatingWorkspaceFrame() = capture(ImageScreen.FILES, "image-floating-workspace.png")
    @Test @Config(qualifiers = "w360dp-h800dp-night-xhdpi")
    fun floatingDark() = capture(ImageScreen.FILES, "image-floating-dark.png")
    @Test fun savingAndPermissions() {
        open(ImageScreen.FILES)
        compose.onNodeWithTag("photo-permissions").performScrollTo()
        save("image-files.png")
    }
    @Test @Config(qualifiers = "w360dp-h800dp-night-xhdpi")
    fun metadataDark() {
        open(ImageScreen.FILES)
        compose.onNodeWithTag("photo-details").performScrollTo().performClick()
        save("image-metadata-dark.png")
    }
    @Test @Config(qualifiers = "w360dp-h800dp-night-xhdpi")
    fun qualityDark() = capture(ImageScreen.COMPRESSION, "image-quality-dark.png")
    @Test fun progressInterop() = capture(ImageScreen.HEIC, "image-conversion.png")

    private fun open(step: String) {
        compose.runOnIdle { compose.activity.images.screen.sections.focusCard(step) }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
    }
    private fun capture(step: String, filename: String) { open(step); save(filename) }
    private fun save(filename: String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val dir = File("build/reports/ui"); assertTrue(dir.isDirectory || dir.mkdirs())
        val output = File(dir, filename)
        compose.onRoot().captureRoboImage(output.absolutePath)
        assertTrue("Missing Compose capture: $filename", output.isFile && output.length() > 3000)
    }
}
