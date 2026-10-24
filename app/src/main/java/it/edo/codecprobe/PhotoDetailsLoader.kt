package it.edo.codecprobe

import android.os.Handler
import android.os.Looper
import java.io.InterruptedIOException
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** One cancellable reader for the selected photo and the actual visible carousel window. */
internal class PhotoDetailsLoader(
    private val read: (String) -> PhotoBasicDetails,
    private val publish: (String) -> Unit
) {
    private val cache = MetadataMemoryCache<PhotoBasicDetails>(32, 128L * 1024)
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var task: Future<*>? = null
    private var targets = emptyList<String>()
    @Volatile private var generation = 0
    @Volatile private var closed = false

    fun get(uri: String): PhotoBasicDetails? = cache.get(uri)

    fun update(requested: List<String>) {
        val next = requested.distinct().take(25)
        if (closed || next == targets) return
        targets = next
        val current = ++generation
        task?.cancel(true)
        if (next.isEmpty()) return
        val epoch = cache.epoch()
        task = worker.submit {
            for (uri in next) {
                if (closed || generation != current || Thread.currentThread().isInterrupted) return@submit
                if (cache.get(uri) != null) continue
                val result = try { read(uri) }
                catch (cancelled: InterruptedIOException) { return@submit }
                catch (error: Exception) { PhotoBasicDetails(error = (error.message ?: error.javaClass.simpleName).take(512)) }
                if (closed || generation != current || Thread.currentThread().isInterrupted) return@submit
                if (!cache.put(uri, result, result.weight(), epoch)) continue
                main.post { if (!closed && generation == current) publish(uri) }
            }
        }
    }

    fun invalidate(uris: Collection<String>) {
        uris.forEach { cache.remove(it) }
        val previous = targets
        targets = emptyList()
        update(previous)
    }
    fun close() {
        closed = true; ++generation; task?.cancel(true); worker.shutdownNow(); cache.clear()
        main.removeCallbacksAndMessages(null)
    }
}
