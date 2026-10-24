package it.edo.codecprobe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*

/** Shared Compose surfaces. Uses Material 3 interactions and Haze, not custom Android drawing. */
internal data class PhotoGlass(val background: HazeState, val content: HazeState, val enabled: Boolean)
internal val LocalPhotoGlass = staticCompositionLocalOf<PhotoGlass> { error("Photo glass host missing") }

@Composable
internal fun Modifier.photoGlass(shape: Shape, chrome: Boolean = false): Modifier {
    val glass = LocalPhotoGlass.current
    val colors = MaterialTheme.colorScheme
    val style = remember(colors, chrome) {
        HazeStyle(
            backgroundColor = colors.background,
            tint = HazeTint(colors.surface.copy(alpha = if (chrome) .58f else .68f)),
            blurRadius = if (chrome) 10.dp else 5.dp,
            noiseFactor = 0f,
            fallbackTint = HazeTint(colors.surface.copy(alpha = .88f))
        )
    }
    return clip(shape).hazeEffect(if (chrome) glass.content else glass.background, style) {
        blurEnabled = glass.enabled && HazeDefaults.blurEnabled()
    }
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
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth().photoGlass(shape)
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f), shape)
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
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
    compact: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) 18.dp else 28.dp, label = "button-shape")
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        when { primary -> colors.primary; selected -> colors.primaryContainer.copy(alpha = .8f); else -> colors.surfaceVariant.copy(alpha = .7f) },
        label = "button-fill"
    )
    val ink by animateColorAsState(if (primary) colors.onPrimary else if (selected) colors.onPrimaryContainer else colors.primary, label = "button-ink")
    val actualShape = shape ?: RoundedCornerShape(corner)
    Surface(
        onClick = onClick, enabled = enabled, interactionSource = interaction, shape = actualShape,
        color = fill, contentColor = if (enabled) ink else colors.onSurface.copy(alpha = .38f),
        border = BorderStroke(1.dp, if (selected) colors.primary.copy(alpha = .5f) else colors.outline.copy(alpha = .55f)),
        modifier = modifier.defaultMinSize(minHeight = 48.dp).semantics {
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
        background = Color(0xFF151820), surface = Color(0xFF232D42), surfaceVariant = Color(0xFF2C3A54),
        onSurface = Color(0xFFE7ECF7), onSurfaceVariant = Color(0xFFB5BFD1), outline = Color(0xFF788599)
    ) else lightColorScheme(
        primary = Color(0xFF3977E9), onPrimary = Color.White, primaryContainer = Color(0xFFD5E3FF), onPrimaryContainer = Color(0xFF33588C),
        background = Color(0xFFF8F8FB), surface = Color(0xFFE8EEFC), surfaceVariant = Color(0xFFDCE7FC),
        onSurface = Color(0xFF292C32), onSurfaceVariant = Color(0xFF565B66), outline = Color(0xFF8D919B)
    )
    }
    MaterialTheme(colorScheme = scheme, typography = Typography()) {
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface, content = content)
    }
}
