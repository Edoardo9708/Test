package it.edo.codecprobe

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.blur.*

/** Shared Compose surfaces. Uses Material 3 interactions and Haze, not custom Android drawing. */
internal data class AppGlassState(val content: HazeState, val enabled: Boolean, val blurEnabled: Boolean)
internal val LocalAppGlassState = staticCompositionLocalOf<AppGlassState> { error("App glass host missing") }
internal fun appSurfaceColor(dark: Boolean): Color = if (dark) Color(0xFF1A1F27) else Color(0xFFF9FAFC)

@Composable
/** Glass belongs to fixed floating chrome; resizing cards use an opaque fill. */
internal fun Modifier.appGlass(shape: Shape): Modifier {
    val glass = LocalAppGlassState.current
    val colors = MaterialTheme.colorScheme
    // Bypass both Haze consumers and its source when the user disables blur.
    if (!glass.blurEnabled) return clip(shape).background(colors.surface.copy(alpha = .58f))
    val style = remember(colors, glass.enabled) {
        HazeBlurStyle {
            backgroundColor(colors.background)
            blurRadius(10.dp)
            noiseFactor(0f)
            blurEnabled(glass.enabled && HazeBlurDefaults.isBlurEnabledByDefault())
            colorEffects(listOf(HazeColorEffect.tint(colors.surface.copy(alpha = AppDesignTokens.Color.glassAlpha))))
            fallbackColorEffect(HazeColorEffect.tint(colors.surface.copy(alpha = AppDesignTokens.Color.glassFallbackAlpha)))
        }
    }
    return clip(shape).hazeBlur(input = HazeInput.Sources(glass.content), style = style)
}

@Composable
internal fun AppTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    edgeVisible: Boolean = true,
    showSurface: Boolean = true,
    action: @Composable RowScope.() -> Unit
) {
    val glass = LocalAppGlassState.current
    val scrim = when {
        glass.blurEnabled && glass.enabled -> AppDesignTokens.Color.glassActiveScrim
        glass.blurEnabled -> AppDesignTokens.Color.glassInactiveScrim
        else -> AppDesignTokens.Color.glassClearScrim
    }
    val edge = MaterialTheme.colorScheme.outline.copy(alpha = AppDesignTokens.Color.headerEdgeAlpha)
    Row(modifier.fillMaxWidth()
        .then(if (showSurface) {
            Modifier
                .then(if (edgeVisible) Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = scrim)) else Modifier)
                .appGlass(RoundedCornerShape(0.dp))
                .then(if (edgeVisible) Modifier.drawBehind {
                    drawLine(edge, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                } else Modifier)
        } else Modifier)
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = AppDesignTokens.Spacing.topBarHorizontal, vertical = AppDesignTokens.Spacing.topBarVertical),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action()
    }
}

internal enum class AppNavigationSurfaceStyle { GlassOutlined, Glass, OpaqueVariant }

@Composable
internal fun AppNavigationSurface(
    modifier: Modifier = Modifier,
    style: AppNavigationSurfaceStyle,
    height: Dp = AppDesignTokens.Size.navigationBar,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(AppDesignTokens.Shape.navigation)
    val glass = LocalAppGlassState.current
    val scrim = when {
        glass.blurEnabled && glass.enabled -> AppDesignTokens.Color.glassActiveScrim
        glass.blurEnabled -> AppDesignTokens.Color.glassInactiveScrim
        else -> AppDesignTokens.Color.glassClearScrim
    }
    val treatment = when (style) {
        AppNavigationSurfaceStyle.GlassOutlined -> Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = scrim), shape)
            .appGlass(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = AppDesignTokens.Color.navigationOutlineAlpha), shape)
            .padding(2.dp)
        AppNavigationSurfaceStyle.Glass -> Modifier.appGlass(shape)
        AppNavigationSurfaceStyle.OpaqueVariant -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant, shape)
    }
    Row(modifier.height(height).then(treatment), verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
internal fun AppNavigationArrow(
    visible: Boolean,
    enabled: Boolean,
    forward: Boolean,
    tag: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.fillMaxSize().then(if (!visible) Modifier.clearAndSetSemantics {} else Modifier),
            enter = fadeIn(tween(AppDesignTokens.Motion.navigationEnterMs, easing = FastOutSlowInEasing)) +
                scaleIn(tween(AppDesignTokens.Motion.navigationEnterMs, easing = FastOutSlowInEasing), initialScale = .86f),
            exit = fadeOut(tween(AppDesignTokens.Motion.navigationExitMs, easing = FastOutSlowInEasing)) +
                scaleOut(tween(AppDesignTokens.Motion.navigationExitMs, easing = FastOutSlowInEasing), targetScale = .86f),
            label = tag
        ) {
            Surface(
                onClick = { if (visible && enabled) onClick() }, enabled = visible && enabled,
                modifier = Modifier.fillMaxSize().semantics { testTag = tag; if (!visible || !enabled) hideFromAccessibility() },
                shape = CircleShape,
                color = (if (forward) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .copy(alpha = .74f),
                contentColor = if (forward) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = AppDesignTokens.Color.dockOutlineAlpha))
            ) {
                Box(Modifier.fillMaxSize().appGlass(CircleShape), contentAlignment = Alignment.Center) {
                    Icon(if (forward) Icons.AutoMirrored.Rounded.ArrowForward else Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = description, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
internal fun AppDialogSurface(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppDesignTokens.Shape.groupOuter),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier.fillMaxWidth().widthIn(max = 440.dp),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = AppDesignTokens.Color.panelOutlineAlpha)),
            shadowElevation = 8.dp
        ) {
            Column(Modifier.fillMaxWidth().padding(AppDesignTokens.Spacing.dialogPadding),
                verticalArrangement = Arrangement.spacedBy(AppDesignTokens.Spacing.dialogContent), content = content)
        }
    }
}

@Composable
internal fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Slider(value, onValueChange, modifier = modifier, enabled = enabled, valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished)
}

@Composable
internal fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Checkbox(checked, onCheckedChange, modifier = modifier, enabled = enabled)
}

internal enum class StartActionVariant { Primary, Secondary }

internal data class StartAction(
    val icon: Int,
    val text: String,
    val variant: StartActionVariant = StartActionVariant.Primary,
    val enabled: Boolean = true,
    val tag: String = "",
    val onClick: () -> Unit
)

/** Responsive, reusable action layout for the first screen of each media workspace. */
@Composable
internal fun StartActionLayout(
    actions: List<StartAction>,
    modifier: Modifier = Modifier,
    onAction: ((StartAction, Offset, Color) -> Unit)? = null
) {
    if (actions.isEmpty()) return
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val fontScale = LocalDensity.current.fontScale
        val gap = AppDesignTokens.Spacing.startActionGap
        val horizontal = actions.size == 1 ||
            (fontScale < 1.4f && maxWidth >= AppDesignTokens.Size.startActionMin * actions.size + gap * (actions.size - 1))
        val maximumSize = if (actions.size == 1) AppDesignTokens.Size.startActionSingleMax else AppDesignTokens.Size.startActionMax
        val availableDiameter = if (horizontal) {
            minOf((maxWidth - gap * (actions.size - 1)) / actions.size, maxHeight * .42f)
        } else {
            minOf(maxWidth * .68f, (maxHeight - gap * (actions.size - 1)) / actions.size)
        }.coerceAtMost(maximumSize).coerceAtLeast(0.dp)
        // Keep the preferred touch size where possible, but never overflow a short or narrow viewport.
        val diameter = minOf(availableDiameter, maximumSize)
            .coerceAtLeast(minOf(AppDesignTokens.Size.startActionMin, availableDiameter))

        if (horizontal) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                actions.forEach { action -> StartActionButton(action, diameter, onAction = onAction) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
                actions.forEach { action -> StartActionButton(action, diameter, onAction = onAction) }
            }
        }
    }
}

/** Circular entry action. Its modifier stays exposed so a later route transition can wrap it. */
@Composable
internal fun StartActionButton(
    action: StartAction,
    size: Dp,
    modifier: Modifier = Modifier,
    onAction: ((StartAction, Offset, Color) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    var center by remember { mutableStateOf(Offset.Zero) }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && action.enabled) .96f else 1f,
        animationSpec = if (pressed) tween(AppDesignTokens.Motion.startActionPressMs, easing = FastOutSlowInEasing)
            else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "start-action-scale"
    )
    val containerTarget = when {
        !action.enabled -> colors.surfaceVariant.copy(alpha = .55f)
        action.variant == StartActionVariant.Primary -> colors.primary
        else -> colors.surfaceVariant.copy(alpha = .82f)
    }
    val contentTarget = when {
        !action.enabled -> colors.onSurface.copy(alpha = .38f)
        action.variant == StartActionVariant.Primary -> colors.onPrimary
        else -> colors.onSurfaceVariant
    }
    val container by animateColorAsState(containerTarget, tween(AppDesignTokens.Motion.controlMs, easing = FastOutSlowInEasing),
        label = "start-action-container")
    val content by animateColorAsState(contentTarget, tween(AppDesignTokens.Motion.controlMs, easing = FastOutSlowInEasing),
        label = "start-action-content")
    val edge = if (action.variant == StartActionVariant.Primary) colors.primary.copy(alpha = .42f)
        else colors.outline.copy(alpha = .48f)
    Surface(
        onClick = {
            if (onAction == null) action.onClick() else onAction(action, center, container)
        },
        enabled = action.enabled,
        interactionSource = interactionSource,
        modifier = modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale }
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                center = position + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
            }
            .semantics { if (action.tag.isNotEmpty()) testTag = action.tag },
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, edge)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(painterResource(action.icon), contentDescription = null, modifier = Modifier.size(minOf(size * .34f, 52.dp)))
            Spacer(Modifier.height(8.dp))
            Text(action.text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

internal fun appJoinedShape(position: Int, vertical: Boolean = true): RoundedCornerShape =
    if (vertical) RoundedCornerShape(
        topStart = if (position == 0) AppDesignTokens.Shape.panel else AppDesignTokens.Shape.groupJoin + 1.dp,
        topEnd = if (position == 0) AppDesignTokens.Shape.panel else AppDesignTokens.Shape.groupJoin + 1.dp,
        bottomStart = if (position == 2) AppDesignTokens.Shape.panel else AppDesignTokens.Shape.groupJoin + 1.dp,
        bottomEnd = if (position == 2) AppDesignTokens.Shape.panel else AppDesignTokens.Shape.groupJoin + 1.dp
    ) else RoundedCornerShape(
        topStart = if (position == 0) 22.dp else AppDesignTokens.Shape.groupJoin + 1.dp,
        bottomStart = if (position == 0) 22.dp else AppDesignTokens.Shape.groupJoin + 1.dp,
        topEnd = if (position == 2) 22.dp else AppDesignTokens.Shape.groupJoin + 1.dp,
        bottomEnd = if (position == 2) 22.dp else AppDesignTokens.Shape.groupJoin + 1.dp
    )

internal fun appGroupShape(index: Int, count: Int): RoundedCornerShape {
    val outer = AppDesignTokens.Shape.groupOuter
    val joined = AppDesignTokens.Shape.groupJoin
    return RoundedCornerShape(
        topStart = if (index == 0) outer else joined,
        topEnd = if (index == 0) outer else joined,
        bottomStart = if (index == count - 1) outer else joined,
        bottomEnd = if (index == count - 1) outer else joined
    )
}

@Composable
internal fun AppPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppDesignTokens.Shape.panel),
    contentSpacing: Dp = AppDesignTokens.Spacing.panelContent,
    contentPadding: PaddingValues = PaddingValues(AppDesignTokens.Spacing.panelPadding),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = AppDesignTokens.Color.panelOutlineAlpha),
    outlined: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (outlined) BorderStroke(1.dp, borderColor) else null
    ) {
        Column(Modifier.fillMaxWidth().padding(contentPadding), verticalArrangement = Arrangement.spacedBy(contentSpacing), content = content)
    }
}

/** Transparent layout for individually surfaced panels in one visual group. */
@Composable
internal fun AppPanelGroup(
    modifier: Modifier = Modifier,
    spacing: Dp = AppDesignTokens.Spacing.groupedPanels,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
}

@Composable
internal fun ColumnScope.AppGroupPanel(
    position: Int,
    count: Int = 4,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = AppDesignTokens.Color.panelOutlineAlpha),
    content: @Composable ColumnScope.() -> Unit
) {
    val verticalPadding = if (position == 0 || position == count - 1) 14.dp else AppDesignTokens.Spacing.panelContent
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = appGroupShape(position, count),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = AppDesignTokens.Spacing.panelPadding, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(AppDesignTokens.Spacing.groupContent),
            content = content
        )
    }
}

private fun appFoldEnter() = expandVertically(
    animationSpec = tween(AppDesignTokens.Motion.foldEnterMs, easing = FastOutSlowInEasing), expandFrom = Alignment.Top
) + fadeIn(tween(AppDesignTokens.Motion.foldEnterFadeMs, easing = FastOutSlowInEasing))

private fun appFoldExit(synchronized: Boolean = false) = shrinkVertically(
    animationSpec = tween(AppDesignTokens.Motion.foldExitMs, easing = FastOutLinearInEasing), shrinkTowards = Alignment.Top
) + fadeOut(tween(if (synchronized) AppDesignTokens.Motion.foldSynchronizedExitFadeMs else AppDesignTokens.Motion.foldExitFadeMs,
    easing = FastOutLinearInEasing))

/** A short height-and-fade transition for expandable content. */
@Composable
internal fun AppFold(visible: Boolean, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    AnimatedVisibility(visible = visible, enter = appFoldEnter(), exit = appFoldExit(), content = content)
}

/** Same fold motion for inline details, destination controls, and other short panels. */
@Composable
internal fun AppPopupTransition(
    visible: Boolean,
    synchronizedExit: Boolean = false,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(visible = visible, enter = appFoldEnter(), exit = appFoldExit(synchronizedExit)) { content() }
}

@Composable
internal fun AppSectionHeading(text: String, icon: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(start = AppDesignTokens.Spacing.headingStart,
        top = AppDesignTokens.Spacing.headingTop, bottom = AppDesignTokens.Spacing.headingBottom),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(icon), null, Modifier.size(AppDesignTokens.Size.iconSmall), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() })
    }
}

@Composable
internal fun AppButton(
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
        !enabled -> colors.onSurface.copy(alpha = .08f)
        primary -> colors.primary
        selected -> colors.primaryContainer
        else -> colors.surfaceVariant
    }
    val targetInk = when {
        !enabled -> colors.onSurface.copy(alpha = .38f)
        destructive -> colors.error
        primary -> colors.onPrimary
        selected -> colors.onPrimaryContainer
        else -> colors.onSurfaceVariant
    }
    val targetEdge = when {
        !enabled -> colors.onSurface.copy(alpha = .12f)
        destructive -> colors.error
        selected || primary -> colors.primary
        else -> colors.outline
    }
    val fill by animateColorAsState(targetFill, tween(AppDesignTokens.Motion.controlMs, easing = FastOutSlowInEasing), label = "app-button-fill")
    val ink by animateColorAsState(targetInk, tween(AppDesignTokens.Motion.controlMs, easing = FastOutSlowInEasing), label = "app-button-content")
    val edge by animateColorAsState(targetEdge, tween(AppDesignTokens.Motion.controlMs, easing = FastOutSlowInEasing), label = "app-button-border")
    val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp, tween(AppDesignTokens.Motion.controlMs, easing = FastOutSlowInEasing), label = "app-button-border-width")
    val base = shape ?: RoundedCornerShape(AppDesignTokens.Shape.control)
    Surface(
        onClick = onClick, enabled = enabled, shape = base,
        color = fill,
        contentColor = ink,
        border = BorderStroke(borderWidth, edge),
        modifier = modifier.defaultMinSize(minHeight = AppDesignTokens.Size.touchTarget)
            .then(if (chrome) Modifier.appGlass(base) else Modifier).semantics {
            role = Role.Button
            if (tag.isNotEmpty()) testTag = tag
            this.selected = selected
        }
    ) {
        if (vertical) Column(Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) Icon(painterResource(icon), null, Modifier.size(AppDesignTokens.Size.groupIcon))
            Text(text, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        } else Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(painterResource(icon), null, Modifier.size(AppDesignTokens.Size.iconSmall)); Spacer(Modifier.width(AppDesignTokens.Spacing.controlGroup * 2)) }
            Text(text, style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

/** Equal-width connected choices using the standard Row layout. */
@Composable
internal fun AppButtonGroup(count: Int, modifier: Modifier = Modifier, content: @Composable (Int, Modifier) -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.Spacing.controlGroup)) {
        repeat(count) { index -> content(index, Modifier.weight(1f)) }
    }
}

@Composable
internal fun AppSwitchRow(text: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit, tag: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked, change, enabled = enabled, modifier = Modifier.semantics { testTag = tag })
    }
}

@Composable
internal fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = remember(dark) {
        if (dark) darkColorScheme(
            primary = Color(0xFFB4CCF5), onPrimary = Color(0xFF193459),
            primaryContainer = Color(0xFF344A6A), onPrimaryContainer = Color(0xFFE0EAF8),
            secondary = Color(0xFFB9C8DF), onSecondary = Color(0xFF26344A),
            secondaryContainer = Color(0xFF344154), onSecondaryContainer = Color(0xFFDCE5F2),
            background = Color(0xFF11151A), surface = appSurfaceColor(true), surfaceVariant = Color(0xFF29323F),
            onSurface = Color(0xFFE5E8ED), onSurfaceVariant = Color(0xFFBCC5D2),
            outline = Color(0xFF778294), error = Color(0xFFF2B8B5)
        ) else lightColorScheme(
            primary = Color(0xFF315FA8), onPrimary = Color.White,
            primaryContainer = Color(0xFFD9E5F7), onPrimaryContainer = Color(0xFF243F66),
            secondary = Color(0xFF53657D), onSecondary = Color.White,
            secondaryContainer = Color(0xFFE5EAF2), onSecondaryContainer = Color(0xFF26354B),
            background = Color(0xFFF3F5F8), surface = appSurfaceColor(false), surfaceVariant = Color(0xFFE9EEF5),
            onSurface = Color(0xFF1B1D22), onSurfaceVariant = Color(0xFF4B5563),
            outline = Color(0xFF9BA6B5), error = Color(0xFFB3261E)
        )
    }
    val typography = remember {
        val defaults = Typography()
        defaults.copy(
            headlineSmall = defaults.headlineSmall.copy(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
            titleLarge = defaults.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = defaults.titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
            titleSmall = defaults.titleSmall.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = defaults.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = defaults.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = defaults.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
            labelLarge = defaults.labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
            labelMedium = defaults.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
            labelSmall = defaults.labelSmall.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
        )
    }
    MaterialTheme(colorScheme = scheme, typography = typography) {
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface, content = content)
    }
}
