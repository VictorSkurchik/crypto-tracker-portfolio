package by.vsdev.cpt.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

private const val DIAMOND_ROTATION_DEGREES = 45f

/** Small geometric glyphs for the app's 5 top-level destinations. Tint follows [LocalContentColor]. */
object CptNavIcons {
    @Composable
    fun Portfolio(modifier: Modifier = Modifier) {
        Box(modifier.size(9.dp).background(LocalContentColor.current, CircleShape))
    }

    @Composable
    fun Wallets(modifier: Modifier = Modifier) {
        Box(
            modifier
                .size(width = 16.dp, height = 11.dp)
                .border(1.5.dp, LocalContentColor.current, RoundedCornerShape(3.dp)),
        )
    }

    @Composable
    fun Exchanges(modifier: Modifier = Modifier) {
        val color = LocalContentColor.current
        Canvas(modifier.size(width = 14.dp, height = 9.dp)) {
            val strokeWidth = 1.6.dp.toPx()
            drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth)
            drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth)
        }
    }

    @Composable
    fun CustomAssets(modifier: Modifier = Modifier) {
        Box(
            modifier
                .size(9.dp)
                .rotate(DIAMOND_ROTATION_DEGREES)
                .border(1.5.dp, LocalContentColor.current, RoundedCornerShape(1.dp)),
        )
    }

    @Composable
    fun Settings(modifier: Modifier = Modifier) {
        Box(modifier.size(13.dp).border(1.5.dp, LocalContentColor.current, CircleShape))
    }
}
