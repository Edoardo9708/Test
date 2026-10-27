package it.edo.codecprobe

import android.animation.ValueAnimator
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/** Available ambient themes. Geometric is the only implemented theme for now. */
internal enum class AppBackgroundTheme { Geometric }

/** Draw-only animated backdrop, independent of the screen content placed above it. */
@Composable
internal fun AppAnimatedBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    theme: AppBackgroundTheme = AppBackgroundTheme.Geometric
) {
    if (!enabled || !ValueAnimator.areAnimatorsEnabled()) return
    val timeMs = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var previousFrame = 0L
        var elapsedSinceDraw = 0L
        while (true) {
            withFrameNanos { frame ->
                if (previousFrame != 0L) {
                    elapsedSinceDraw += ((frame - previousFrame) / 1_000_000L).coerceIn(0L, 64L)
                    if (elapsedSinceDraw >= 16L) {
                        timeMs.longValue += elapsedSinceDraw
                        elapsedSinceDraw = 0L
                    }
                }
                previousFrame = frame
            }
        }
    }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Canvas(modifier) {
        when (theme) {
            AppBackgroundTheme.Geometric -> {
                val width = size.width
                val height = size.height
                val time = timeMs.longValue
                val twoPi = 6.2831855f
                GEOMETRIC_AMBIENT_SHAPES.forEach { shape ->
                    val color = when (shape.palette) {
                        0 -> primary
                        1 -> secondary
                        else -> tertiary
                    }
                    val xPhase = (time % shape.xPeriodMs).toFloat() * twoPi / shape.xPeriodMs.toFloat()
                    val yPhase = (time % shape.yPeriodMs).toFloat() * twoPi / shape.yPeriodMs.toFloat()
                    val x = shape.x + shape.xTravel * sin(xPhase)
                    val y = shape.y + shape.yTravel * sin(yPhase)
                    drawCircle(
                        color = color.copy(alpha = shape.alpha),
                        radius = shape.radiusDp.dp.toPx(),
                        center = Offset(width * x, height * y)
                    )
                }
            }
        }
    }
}

private data class AmbientShape(
    val x: Float,
    val y: Float,
    val xTravel: Float,
    val yTravel: Float,
    val radiusDp: Float,
    val alpha: Float,
    val xPeriodMs: Long,
    val yPeriodMs: Long,
    val palette: Int
)

private val GEOMETRIC_AMBIENT_SHAPES = listOf(
    AmbientShape(.20f, .225f, .05f, .025f, 190f, .055f, 8_000, 7_000, 0),
    AmbientShape(.85f, .185f, .03f, .035f, 138f, .045f, 9_000, 6_500, 2),
    AmbientShape(.715f, .44f, .035f, .04f, 98f, .05f, 7_500, 8_500, 2),
    AmbientShape(.815f, .785f, .035f, .035f, 154f, .045f, 9_500, 7_200, 1),
    AmbientShape(.24f, .765f, .04f, .035f, 90f, .05f, 8_200, 6_800, 0),
    AmbientShape(.525f, .895f, .025f, .025f, 112f, .05f, 8_800, 7_800, 1)
)
