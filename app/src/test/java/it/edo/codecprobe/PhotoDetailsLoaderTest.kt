package it.edo.codecprobe

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.InterruptedIOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class PhotoDetailsLoaderTest {
    @Test fun visibleMetadataIsReusedAndReloadInvalidatesOnlyRequestedSources() {
        val reads = mutableListOf<String>()
        val loader = PhotoDetailsLoader({ uri -> synchronized(reads) { reads += uri }; PhotoBasicDetails(camera = uri) }, {})
        try {
            loader.update(listOf("a", "b", "c"))
            await { loader.get("c") != null }
            loader.update(listOf("c", "b"))
            Thread.sleep(30)
            assertEquals(listOf("a", "b", "c"), synchronized(reads) { reads.toList() })
            loader.invalidate(listOf("b"))
            await { synchronized(reads) { reads.count { it == "b" } } == 2 && loader.get("b") != null }
            assertEquals(1, synchronized(reads) { reads.count { it == "c" } })
            assertEquals("a", loader.get("a")!!.camera)
        } finally { loader.close() }
    }

    @Test fun changingViewportCancelsObsoleteWorkAndKeepsAtMost32SmallEntries() {
        val entered = CountDownLatch(1)
        val count = AtomicInteger()
        val loader = PhotoDetailsLoader({ uri ->
            if (uri == "slow") {
                entered.countDown()
                try { CountDownLatch(1).await(3, TimeUnit.SECONDS) }
                catch (cancelled: InterruptedException) { throw InterruptedIOException("cancelled") }
            }
            count.incrementAndGet(); PhotoBasicDetails(camera = uri)
        }, {})
        try {
            loader.update(listOf("slow", "obsolete"))
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            loader.update(listOf("current"))
            await { loader.get("current") != null }
            assertNull(loader.get("slow")); assertNull(loader.get("obsolete")); assertEquals(1, count.get())
            for (batch in 0..2) {
                loader.update((0..19).map { "$batch-$it" })
                await { loader.get("$batch-19") != null }
            }
            assertNull(loader.get("0-0")); assertNotNull(loader.get("2-19"))
            loader.update(emptyList())
        } finally { loader.close() }
    }

    private fun await(condition: () -> Boolean) {
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (!condition() && System.nanoTime() < end) Thread.sleep(5)
        assertTrue("Preview worker did not finish", condition())
    }
}
