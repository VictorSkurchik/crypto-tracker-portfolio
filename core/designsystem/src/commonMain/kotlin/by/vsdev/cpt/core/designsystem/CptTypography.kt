package by.vsdev.cpt.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object CptTypography {
    private val base = Typography()

    val default =
        base.copy(
            displayLarge =
                base.displayLarge.copy(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 52.sp,
                    lineHeight = 56.sp,
                    letterSpacing = (-1.5).sp,
                ),
            titleMedium =
                base.titleMedium.copy(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.4.sp,
                ),
            bodyLarge =
                base.bodyLarge.copy(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                ),
            labelSmall =
                base.labelSmall.copy(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                ),
        )
}
