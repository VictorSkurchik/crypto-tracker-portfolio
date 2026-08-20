package by.vsdev.cpt.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Each hex literal below is already self-documenting via the token name it's assigned to.
@Suppress("MagicNumber")
private fun cptLightColorScheme() =
    lightColorScheme(
        background = Color(0xFFFAFAF8),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF1EFEA),
        // Deliberately darker than the design mockup's literal token — chips/text-field borders are
        // interactive-component boundaries, so WCAG 1.4.11 wants >=3:1 against the background; the
        // mockup's #E1DED6 only clears 1.29:1.
        outline = Color(0xFF8A867B),
        onSurface = Color(0xFF1B1A18),
        onSurfaceVariant = Color(0xFF6E6B63),
        primary = Color(0xFF1171A0),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6EBFA),
        onPrimaryContainer = Color(0xFF003255),
        error = Color(0xFFA05C58),
        onError = Color(0xFFFFFFFF),
        surfaceTint = Color.Transparent,
    )

@Suppress("MagicNumber")
private fun cptDarkColorScheme() =
    darkColorScheme(
        background = Color(0xFF141312),
        surface = Color(0xFF1C1B19),
        surfaceVariant = Color(0xFF242220),
        // See the light-scheme comment above — same >=3:1 non-text-contrast fix for dark mode.
        outline = Color(0xFF6D695C),
        onSurface = Color(0xFFECEAE4),
        onSurfaceVariant = Color(0xFFA7A199),
        primary = Color(0xFF79BAE4),
        onPrimary = Color(0xFF141312),
        primaryContainer = Color(0xFF083248),
        onPrimaryContainer = Color(0xFFC0E4FC),
        error = Color(0xFFCA827D),
        onError = Color(0xFF141312),
        surfaceTint = Color.Transparent,
    )

@Composable
fun CptTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) cptDarkColorScheme() else cptLightColorScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CptTypography.default,
        content = content,
    )
}
