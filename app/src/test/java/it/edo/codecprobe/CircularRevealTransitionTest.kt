package it.edo.codecprobe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class CircularRevealTransitionTest {
    @Test fun radiusReachesTheFarthestViewportCornerFromTheActualOrigin() {
        val origin = Offset(30f, 70f)
        val viewport = IntSize(400, 800)
        val expected = hypot(370f, 730f) + 2f

        assertEquals(expected, maximumRevealRadius(origin, viewport), .01f)
    }

    @Test fun oneTapAcceptsOnlyOneNavigationCallback() {
        val state = CircularRevealState()
        var navigationCount = 0

        assertTrue(state.start(Offset(20f, 40f), Color.Blue) { navigationCount++ })
        assertFalse(state.start(Offset(80f, 90f), Color.Red) { navigationCount++ })
        assertEquals(0, navigationCount)
        state.navigateWhileCovered()
        state.navigateWhileCovered()

        assertEquals(1, navigationCount)
    }
}
