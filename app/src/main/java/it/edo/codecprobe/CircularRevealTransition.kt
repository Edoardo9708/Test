package it.edo.codecprobe

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.hypot

internal enum class CircularRevealPhase { IDLE, EXPANDING, COVERED, REVEALING, COMPLETE }

/** Owns one reveal request; navigation remains supplied by the calling screen. */
internal class CircularRevealState {
    var phase by mutableStateOf(CircularRevealPhase.IDLE)
        internal set
    var origin by mutableStateOf(Offset.Zero)
        private set
    var color by mutableStateOf(Color.Transparent)
        private set
    var requestId by mutableIntStateOf(0)
        private set
    internal var renderedContentKey by mutableStateOf<Any?>(null)
        private set
    private var coveredAction: (() -> Unit)? = null

    fun start(origin: Offset, color: Color, onCovered: () -> Unit): Boolean {
        if (phase != CircularRevealPhase.IDLE) return false
        this.origin = origin
        this.color = color.copy(alpha = 1f)
        coveredAction = onCovered
        phase = CircularRevealPhase.EXPANDING
        requestId++
        return true
    }

    internal fun contentComposed(key: Any?) {
        renderedContentKey = key
    }

    internal fun navigateWhileCovered() {
        val action = coveredAction
        coveredAction = null
        action?.invoke()
    }

    internal fun finish() {
        phase = CircularRevealPhase.COMPLETE
        phase = CircularRevealPhase.IDLE
        coveredAction = null
    }
}

@Composable
internal fun rememberCircularRevealState(): CircularRevealState = remember { CircularRevealState() }

/** Keeps its Canvas above the screen content while the route beneath it changes. */
@Composable
internal fun CircularRevealTransition(
    state: CircularRevealState,
    contentKey: Any?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val radius = remember { Animatable(0f) }
    val opacity = remember { Animatable(1f) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val active = state.phase != CircularRevealPhase.IDLE

    SideEffect { state.contentComposed(contentKey) }
    DisposableEffect(state) {
        onDispose { state.finish() }
    }

    LaunchedEffect(state.requestId) {
        if (state.requestId == 0 || viewport == IntSize.Zero) return@LaunchedEffect
        val sourceKey = state.renderedContentKey
        val maxRadius = maximumRevealRadius(state.origin, viewport)
        try {
            opacity.snapTo(1f)
            if (!ValueAnimator.areAnimatorsEnabled()) {
                radius.snapTo(maxRadius)
            } else {
                radius.snapTo(0f)
                radius.animateTo(maxRadius, tween(
                    AppDesignTokens.Motion.circularRevealExpandMs,
                    easing = FastOutLinearInEasing
                ))
            }

            state.phase = CircularRevealPhase.COVERED
            state.navigateWhileCovered()

            // The route key is published by SideEffect only after Compose applied its new content.
            val destinationReady = withTimeoutOrNull(8_000L) {
                snapshotFlow { state.renderedContentKey }.first { it != sourceKey }
            }
            if (destinationReady == null) {
                // A failed/no-op route must never leave input blocked behind a permanent overlay.
                if (ValueAnimator.areAnimatorsEnabled()) {
                    opacity.animateTo(0f, tween(
                        AppDesignTokens.Motion.circularRevealFadeMs,
                        easing = FastOutSlowInEasing
                    ))
                } else {
                    opacity.snapTo(0f)
                }
                state.finish()
                return@LaunchedEffect
            }
            withFrameNanos { }

            state.phase = CircularRevealPhase.REVEALING
            if (ValueAnimator.areAnimatorsEnabled()) {
                opacity.animateTo(0f, tween(
                    AppDesignTokens.Motion.circularRevealFadeMs,
                    easing = FastOutSlowInEasing
                ))
            } else {
                opacity.snapTo(0f)
            }
            state.phase = CircularRevealPhase.COMPLETE
        } finally {
            // Cancellation by disposal/lifecycle cannot strand a covered screen.
            state.finish()
        }
    }

    Box(modifier.fillMaxSize().onSizeChanged { viewport = it }) {
        content()
        if (active) {
            Canvas(
                Modifier.fillMaxSize()
                    .zIndex(1f)
                    .clearAndSetSemantics { }
                    .pointerInput(active) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
            ) {
                drawCircle(color = state.color.copy(alpha = opacity.value), radius = radius.value, center = state.origin)
            }
        }
    }
}

internal fun maximumRevealRadius(origin: Offset, viewport: IntSize): Float {
    val right = viewport.width.toFloat()
    val bottom = viewport.height.toFloat()
    val x = origin.x.coerceIn(0f, right)
    val y = origin.y.coerceIn(0f, bottom)
    return maxOf(
        hypot(x, y),
        hypot(right - x, y),
        hypot(x, bottom - y),
        hypot(right - x, bottom - y)
    ) + 2f
}
