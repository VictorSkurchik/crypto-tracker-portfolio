package by.vsdev.cpt.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Design's standard width breakpoints (compact/medium/expanded), computed from
 * [LocalWindowInfo] rather than the platform-specific `androidx.window`/`WindowSizeClass`
 * libraries so the same code runs on Android, Desktop, and iOS without per-platform wiring.
 */
enum class CptWindowWidthSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

private val MEDIUM_BREAKPOINT: Dp = 600.dp
private val EXPANDED_BREAKPOINT: Dp = 840.dp

@Composable
fun rememberWindowWidthSizeClass(): CptWindowWidthSizeClass {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val widthDp = with(density) { containerSize.width.toDp() }
    return remember(widthDp) {
        when {
            widthDp < MEDIUM_BREAKPOINT -> CptWindowWidthSizeClass.COMPACT
            widthDp < EXPANDED_BREAKPOINT -> CptWindowWidthSizeClass.MEDIUM
            else -> CptWindowWidthSizeClass.EXPANDED
        }
    }
}
