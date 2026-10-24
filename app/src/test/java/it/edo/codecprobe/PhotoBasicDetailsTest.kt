package it.edo.codecprobe

import android.net.Uri
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhotoBasicDetailsTest {
    @Test fun readsTheFivePreviewFieldsWithoutAFullJpegInventory() {
        val file = File.createTempFile("basic-details-", ".jpg")
        try {
            val jpeg = ImageWorkspaceTest.fixture()
            val model = "Test Camera\u0000".toByteArray()
            val tiff = ByteBuffer.allocate(26 + model.size).order(ByteOrder.LITTLE_ENDIAN)
            tiff.put('I'.code.toByte()).put('I'.code.toByte()).putShort(42).putInt(8)
            tiff.putShort(1).putShort(0x110).putShort(2).putInt(model.size).putInt(26).putInt(0).put(model)
            val exif = "Exif\u0000\u0000".toByteArray() + tiff.array()
            file.writeBytes(jpeg.take(2).toByteArray() + byteArrayOf(0xff.toByte(), 0xe1.toByte(), ((exif.size + 2) shr 8).toByte(), (exif.size + 2).toByte()) + exif + jpeg.drop(2).toByteArray())
            val basic = PhotoBasicDetails.read(RuntimeEnvironment.getApplication(), Uri.fromFile(file).toString())
            assertEquals(48, basic.width); assertEquals(32, basic.height)
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
}
