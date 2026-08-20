package by.vsdev.cpt.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Original monogram-badge icon system (real exchange/coin logos are trademarked): square badges
 * for exchanges, circular badges for chains/tokens, each tinted with a loose color association and
 * marked with one of three ultra-minimal glyphs instead of letters.
 */
enum class CptBadgeShape { SQUARE, CIRCLE }

private enum class CptGlyphMark { DOT, RING, DIAMOND }

private data class CptCoinStyle(
    val background: Color,
    val mark: Color,
    val glyph: CptGlyphMark,
)

private val CptCoinFallback = CptCoinStyle(Color(0xFFF1EFEA), Color(0xFF6E6B63), CptGlyphMark.DOT)

// Ticker -> style, derived from the design's per-hue OKLCH background/mark colors.
private val CptCoinStyles: Map<String, CptCoinStyle> =
    mapOf(
        "BN" to CptCoinStyle(Color(0xFFF6E6C7), Color(0xFF604200), CptGlyphMark.DIAMOND),
        "OK" to CptCoinStyle(Color(0xFFE7E5E1), Color(0xFF2A2926), CptGlyphMark.RING),
        "BY" to CptCoinStyle(Color(0xFFFFDFD2), Color(0xFF733119), CptGlyphMark.DOT),
        "BG" to CptCoinStyle(Color(0xFFCDF2E0), Color(0xFF00583A), CptGlyphMark.RING),
        "BTC" to CptCoinStyle(Color(0xFFFFDFD0), Color(0xFF723311), CptGlyphMark.DOT),
        "ETH" to CptCoinStyle(Color(0xFFD2EBFF), Color(0xFF124A7B), CptGlyphMark.DIAMOND),
        "OP" to CptCoinStyle(Color(0xFFFFDDD9), Color(0xFF742E2B), CptGlyphMark.RING),
        "APT" to CptCoinStyle(Color(0xFFCAF2E6), Color(0xFF005845), CptGlyphMark.DOT),
        "ARB" to CptCoinStyle(Color(0xFFCAEEFF), Color(0xFF005073), CptGlyphMark.DIAMOND),
        "TON" to CptCoinStyle(Color(0xFFC6F1F9), Color(0xFF005565), CptGlyphMark.RING),
        "TRX" to CptCoinStyle(Color(0xFFFFDCDE), Color(0xFF742D36), CptGlyphMark.DOT),
        "SOL" to CptCoinStyle(Color(0xFFECE2FF), Color(0xFF503975), CptGlyphMark.DIAMOND),
        "USDT" to CptCoinStyle(Color(0xFFD4F1D8), Color(0xFF115629), CptGlyphMark.RING),
        "USDC" to CptCoinStyle(Color(0xFFCCEEFF), Color(0xFF004E75), CptGlyphMark.DOT),
    )

/**
 * Icon-set badge for an exchange/chain/token ticker (e.g. "BN", "ETH", "BTC"). Unknown tickers
 * fall back to a neutral badge with a plain dot, matching the design's fallback for untinted marks.
 */
@Composable
fun CptCoinBadge(
    symbol: String,
    shape: CptBadgeShape,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val style = CptCoinStyles[symbol.uppercase()] ?: CptCoinFallback
    val containerShape = if (shape == CptBadgeShape.SQUARE) RoundedCornerShape(size * 0.28f) else CircleShape
    Box(
        modifier = modifier.size(size).background(style.background, containerShape),
        contentAlignment = Alignment.Center,
    ) {
        CptGlyph(style.glyph, style.mark, size * 0.4f)
    }
}

@Composable
private fun CptGlyph(
    mark: CptGlyphMark,
    color: Color,
    size: Dp,
) {
    when (mark) {
        CptGlyphMark.DOT -> Box(Modifier.size(size).background(color, CircleShape))
        CptGlyphMark.RING -> Box(Modifier.size(size).border(2.dp, color, CircleShape))
        CptGlyphMark.DIAMOND ->
            Box(Modifier.size(size * 0.82f).rotate(45f).border(2.dp, color, RectangleShape))
    }
}
