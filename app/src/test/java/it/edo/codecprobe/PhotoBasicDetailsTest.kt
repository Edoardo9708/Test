package it.edo.codecprobe

import android.net.Uri
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhotoBasicDetailsTest {
    @Test fun readsTheFivePreviewFieldsWithoutAFullJpegInventory() {
        val file = File.createTempFile("basic-details-", ".jpg")
        try {
            file.writeBytes(fixture())
            val basic = PhotoBasicDetails.read(RuntimeEnvironment.getApplication(), Uri.fromFile(file).toString())
            assertEquals(basic.toString(), 48, basic.width); assertEquals(32, basic.height)
            assertEquals("Test Camera", basic.camera)
            assertEquals(file.length(), basic.bytes)
            assertEquals(file.parent, basic.folder)
            assertEquals(listOf("Dimensioni", "Formato", "Dimensione file", "Fotocamera", "Cartella"), basic.rows().map { it.first })
        } finally { file.delete() }
    }

    @Test fun unreadableSourceProducesCopyableErrorAndUnavailableValues() {
        val basic = PhotoBasicDetails.read(RuntimeEnvironment.getApplication(), "file:///missing-photo-for-details.jpg")
        assertTrue(basic.error.isNotBlank())
        assertEquals("Non disponibili", basic.rows()[0].second)
        assertEquals("Non indicata nei metadati", basic.rows()[3].second)
    }
    companion object {
        // Real 48 × 32 JPEG with a valid EXIF Model tag; no deliberately malformed APP blocks.
        fun fixture(): ByteArray = java.util.Base64.getDecoder().decode(
            "/9j/4AAQSkZJRgABAQAAAQABAAD/4QAuRXhpZgAATU0AKgAAAAgAAQEQAAIAAAAMAAAAGgAAAABUZXN0IENhbWVyYQD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAAgADADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwCnRRRX0x88FFFFABRRRQAUUUUAFFFFABRRRQB//9k="
        )
    }

}
