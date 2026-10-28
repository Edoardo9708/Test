package it.edo.codecprobe

import androidx.compose.ui.unit.dp

/** Shared visual measurements for Compose surfaces, controls, and motion. */
internal object AppDesignTokens {
    object Shape {
        val panel = 24.dp
        val groupOuter = 28.dp
        val groupJoin = 4.dp
        val control = 50.dp
        val field = 18.dp
        val navigation = 28.dp
    }

    object Spacing {
        val panelContent = 12.dp
        val panelPadding = 16.dp
        val groupedPanels = 4.dp
        val groupContent = 10.dp
        val navigationHorizontalInset = 12.dp
        val headingStart = 8.dp
        val headingTop = 20.dp
        val headingBottom = 10.dp
        val controlGroup = 4.dp
        val topBarHorizontal = 22.dp
        val topBarVertical = 18.dp
        val dialogPadding = 20.dp
        val dialogContent = 14.dp
        val startActionGap = 24.dp
    }

    object Size {
        val touchTarget = 48.dp
        val iconSmall = 20.dp
        val iconButton = 22.dp
        val groupIcon = 22.dp
        val navigationBar = 56.dp
        val navigationArrow = 56.dp
        val navigationIcon = 26.dp
        val thumbnail = 64.dp
        val startActionMin = 112.dp
        val startActionMax = 176.dp
        val startActionSingleMax = 184.dp
    }

    object Motion {
        const val controlMs = 180
        const val foldEnterMs = 280
        const val foldEnterFadeMs = 190
        const val foldExitMs = 240
        const val foldExitFadeMs = 150
        const val foldSynchronizedExitFadeMs = 240
        const val navigationEnterMs = 170
        const val navigationExitMs = 130
        const val startActionPressMs = 90
        const val circularRevealExpandMs = 400
        const val circularRevealFadeMs = 230
    }

    object Color {
        const val panelOutlineAlpha = .22f
        const val glassAlpha = .58f
        const val glassFallbackAlpha = .94f
        const val navigationOutlineAlpha = .30f
        const val dockOutlineAlpha = .42f
        const val headerEdgeAlpha = .18f
        const val glassActiveScrim = .34f
        const val glassInactiveScrim = 1f
        const val glassClearScrim = .64f
    }
}
