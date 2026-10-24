package it.edo.codecprobe

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.util.Locale

/** Small preview values only; never keeps EXIF payloads, pixels or an open file. */
internal data class PhotoBasicDetails(
    val width: Int = 0,
    val height: Int = 0,
    val bytes: Long = -1,
    val camera: String = "",
    val folder: String = "",
    val error: String = ""
) {
    fun rows(info: JpegInventory.Info? = null): List<Pair<String, String>> {
        val w = info?.width ?: width
        val h = info?.height ?: height
        val length = info?.bytes ?: bytes
        return listOf(
            "Dimensioni" to if (w > 0 && h > 0) "$w × $h px" else "Non disponibili",
            "Formato" to "JPEG",
            "Dimensione file" to if (length >= 0) String.format(Locale.ITALY, "%.2f MB", length / 1_000_000.0) else "Non disponibile",
            "Fotocamera" to camera.ifBlank { "Non indicata nei metadati" },
            "Cartella" to folder.ifBlank { "Non fornita dal gestore file" }
        )
    }

    fun weight(): Long = 256L + 2L * (camera.length + folder.length + error.length)

    companion object {
        fun read(context: Context, value: String): PhotoBasicDetails {
            val uri = Uri.parse(value)
            var bytes = -1L
            val problems = mutableListOf<String>()
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst() && !cursor.isNull(0)) bytes = cursor.getLong(0)
                }
            } catch (error: IOException) { problems += "Dimensione file: ${message(error)}" }
            catch (error: RuntimeException) { problems += "Dimensione file: ${message(error)}" }
            if (bytes < 0) try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { bytes = it.length }
            } catch (error: IOException) { problems += "Dimensione file: ${message(error)}" }
            catch (error: RuntimeException) { problems += "Dimensione file: ${message(error)}" }
            var width = 0
            var height = 0
            var camera = ""
            try {
                val source = context.contentResolver.openInputStream(uri) ?: throw IOException("Foto non accessibile")
                HeaderStream(source).use { input ->
                    val exif = ExifInterface(input)
                    if (input.exceeded) throw IOException("Intestazione oltre 1 MiB; consulta l’analisi completa")
                    width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                    height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                    val make = exif.getAttribute(ExifInterface.TAG_MAKE).orEmpty().trim().take(128)
                    val model = exif.getAttribute(ExifInterface.TAG_MODEL).orEmpty().trim().take(128)
                    camera = if (model.startsWith(make, ignoreCase = true)) model else listOf(make, model).filter { it.isNotBlank() }.joinToString(" ")
                }
            } catch (cancelled: InterruptedIOException) { throw cancelled }
            catch (error: IOException) { problems += "Metadati anteprima: ${message(error)}" }
            catch (error: RuntimeException) { problems += "Metadati anteprima: ${message(error)}" }
            if (Thread.currentThread().isInterrupted) throw InterruptedIOException("Lettura annullata")
            return PhotoBasicDetails(width, height, bytes, camera, folder(uri), problems.joinToString("\n"))
        }

        private fun folder(uri: Uri): String {
            if (uri.scheme == "file") return uri.path?.substringBeforeLast('/', "").orEmpty()
            // Only document IDs with an actual relative path can provide a folder. Opaque IDs cannot.
            val id = if (uri.pathSegments.contains("document")) {
                try { DocumentsContract.getDocumentId(uri) } catch (error: IllegalArgumentException) { return "" }
            } else return ""
            if ('/' !in id) return ""
            return id.substringAfter(':', id).substringBeforeLast('/').take(512)
        }
        private fun message(error: Exception): String = (error.message ?: error.javaClass.simpleName).take(512)
    }
}

/** Cap metadata preview IO. A large/malformed header never causes an entire photo scan. */
private class HeaderStream(source: InputStream) : FilterInputStream(source) {
    private var remaining = 1024L * 1024L
    var exceeded = false
        private set
    private fun check() {
        if (Thread.currentThread().isInterrupted) throw InterruptedIOException("Lettura annullata")
        if (remaining <= 0) { exceeded = true; throw IOException("Intestazione oltre 1 MiB; usa Mostra tutto per l’analisi completa") }
    }
    override fun read(): Int { check(); val value = `in`.read(); if (value >= 0) remaining--; return value }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        check(); val count = `in`.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
        if (count > 0) remaining -= count
        return count
    }
    override fun skip(count: Long): Long {
        if (count <= 0) return 0
        check(); val skipped = `in`.skip(minOf(count, remaining)); remaining -= skipped; return skipped
    }
}
