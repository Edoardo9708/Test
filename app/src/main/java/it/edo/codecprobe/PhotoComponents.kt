package it.edo.codecprobe

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.blur.*

/** Shared Compose surfaces. Uses Material 3 interactions and Haze, not custom Android drawing. */
internal data class PhotoGlass(val content: HazeState, val enabled: Boolean, val blurEnabled: Boolean)
internal val LocalPhotoGlass = staticCompositionLocalOf<PhotoGlass> { error("Photo glass host missing") }
internal fun photoSurfaceColor(dark: Boolean): Color = if (dark) Color(0xFF232D42) else Color(0xFFE8EEFC)

@Composable
/** Glass belongs to fixed floating chrome; resizing cards use an opaque fill. */
internal fun Modifier.photoGlass(shape: Shape): Modifier {
    val glass = LocalPhotoGlass.current
    val colors = MaterialTheme.colorScheme
    // Bypass both Haze consumers and its source when the user disables blur.
    if (!glass.blurEnabled) return clip(shape).background(colors.surface.copy(alpha = .58f))
    val style = remember(colors, glass.enabled) {
        HazeBlurStyle {
            backgroundColor(colors.background)
            blurRadius(10.dp)
            noiseFactor(0f)
            blurEnabled(glass.enabled && HazeBlurDefaults.isBlurEnabledByDefault())
            colorEffects(listOf(HazeColorEffect.tint(colors.surface.copy(alpha = .58f))))
            fallbackColorEffect(HazeColorEffect.tint(colors.surface.copy(alpha = .94f)))
        }
    }
    return clip(shape).hazeBlur(input = HazeInput.Sources(glass.content), style = style)
}

internal fun photoJoined(position: Int, vertical: Boolean = true): RoundedCornerShape =
    if (vertical) RoundedCornerShape(
        topStart = if (position == 0) 24.dp else 5.dp, topEnd = if (position == 0) 24.dp else 5.dp,
        bottomStart = if (position == 2) 24.dp else 5.dp, bottomEnd = if (position == 2) 24.dp else 5.dp
    ) else RoundedCornerShape(
        topStart = if (position == 0) 22.dp else 5.dp, bottomStart = if (position == 0) 22.dp else 5.dp,
        topEnd = if (position == 2) 22.dp else 5.dp, bottomEnd = if (position == 2) 22.dp else 5.dp
    )

@Composable
internal fun PhotoPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    contentSpacing: Dp = 12.dp,
    animateSizeChanges: Boolean = false,
    outlined: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth()
            .then(if (animateSizeChanges) Modifier.animateContentSize(tween(220, easing = LinearOutSlowInEasing)) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (outlined) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f)) else null
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(contentSpacing), content = content)
    }
}

/** Shared expanding/fading motion, with the explicit 220 ms curve used by Morphe's folds. */
@Composable
internal fun PhotoFold(visible: Boolean, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(220, easing = LinearOutSlowInEasing)) +
            fadeIn(tween(220, easing = LinearOutSlowInEasing)),
        exit = shrinkVertically(tween(220, easing = LinearOutSlowInEasing)) +
            fadeOut(tween(220, easing = LinearOutSlowInEasing)),
        content = content
    )
}

/** Immediate visibility for the Foto-page layout-animation experiment. */
@Composable
internal fun PhotoPopupFold(visible: Boolean, content: @Composable () -> Unit) {
    if (visible) content()
}

@Composable
internal fun PhotoHeading(text: String, icon: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(start = 8.dp, top = 28.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() })
    }
}

@Composable
internal fun PhotoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    selected: Boolean = false,
    primary: Boolean = false,
    enabled: Boolean = true,
    vertical: Boolean = false,
    shape: Shape? = null,
    tag: String = "",
    compact: Boolean = false,
    destructive: Boolean = false,
    chrome: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val targetFill = when {
        primary -> colors.primary
        selected -> colors.primaryContainer
        else -> colors.surfaceVariant
    }
    val targetInk = when {
        destructive -> colors.error
        primary -> colors.onPrimary
        selected -> colors.onPrimaryContainer
        else -> colors.onSurfaceVariant
    }
    val targetEdge = when {
        destructive -> colors.error
        selected || primary -> colors.primary
        else -> colors.outline
    }
    val base = shape ?: RoundedCornerShape(50)
    Surface(
        onClick = onClick, enabled = enabled, shape = base,
        color = targetFill,
        contentColor = if (enabled) targetInk else colors.outline,
        border = BorderStroke(if (selected) 2.dp else 1.dp, targetEdge),
        modifier = modifier.defaultMinSize(minHeight = 48.dp)
            .then(if (chrome) Modifier.photoGlass(base) else Modifier).semantics {
            role = Role.Button
            if (tag.isNotEmpty()) testTag = tag
            this.selected = selected
        }
    ) {
        if (vertical) Column(Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(painterResource(icon), null, Modifier.size(23.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        } else Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(painterResource(icon), null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

/** Equal-width connected choices using the standard Row layout. */
@Composable
internal fun PhotoButtonGroup(count: Int, modifier: Modifier = Modifier, content: @Composable (Int, Modifier) -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { index -> content(index, Modifier.weight(1f)) }
    }
}

@Composable
internal fun PhotoToggle(text: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit, tag: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked, change, enabled = enabled, modifier = Modifier.semantics { testTag = tag })
    }
}

@Composable
internal fun PhotoTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = remember(dark) { if (dark) darkColorScheme(
        primary = Color(0xFFA8C5FF), onPrimary = Color(0xFF112C58), primaryContainer = Color(0xFF344D76), onPrimaryContainer = Color(0xFFE7ECF7),
        background = Color(0xFF151820), surface = photoSurfaceColor(true), surfaceVariant = Color(0xFF2C3A54),
        onSurface = Color(0xFFE7ECF7), onSurfaceVariant = Color(0xFFB5BFD1), outline = Color(0xFF788599)
    ) else lightColorScheme(
        primary = Color(0xFF3977E9), onPrimary = Color.White, primaryContainer = Color(0xFFD5E3FF), onPrimaryContainer = Color(0xFF33588C),
        background = Color(0xFFF8F8FB), surface = photoSurfaceColor(false), surfaceVariant = Color(0xFFDCE7FC),
        onSurface = Color(0xFF292C32), onSurfaceVariant = Color(0xFF565B66), outline = Color(0xFF8D919B)
    )
    }
    MaterialTheme(colorScheme = scheme, typography = Typography()) {
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface, content = content)
    }
}
